package com.example.vpn

import com.example.data.model.SplitTunnelMode
import com.example.data.model.VpnSettings
import com.example.data.remote.CreateVpnSessionResponseDto
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.KeyPair

/**
 * Builds a verified WireGuard [Config] from a backend-provisioned session,
 * enforcing DNS leak protection, IPv6 leak protection, and Android per-app split tunneling.
 */
object WireGuardConfigBuilder {

  fun buildConfig(
    session: CreateVpnSessionResponseDto,
    clientKeyPair: KeyPair,
    settings: VpnSettings,
  ): Config {
    val interfaceBuilder = Interface.Builder()
      .setKeyPair(clientKeyPair)
      .setMtu(session.mtu.coerceIn(1280, 1420))

    val addresses = buildList {
      add(session.assignedIpv4)
      if (!session.assignedIpv6.isNullOrBlank()) {
        add(session.assignedIpv6)
      } else if (settings.ipv6LeakProtectionEnabled) {
        // Assign a ULA address to the tun interface when IPv6 leak protection is enabled
        // so ::/0 routes into the encrypted interface instead of leaking on native IPv6.
        add("fd4e:6f76:61::2/128")
      }
    }
    interfaceBuilder.parseAddresses(addresses.joinToString(","))

    if (settings.dnsLeakProtectionEnabled) {
      val dnsList = if (session.dnsServers.isNotEmpty()) {
        session.dnsServers
      } else {
        listOf(settings.secureDnsProvider.dns1, settings.secureDnsProvider.dns2)
      }
      interfaceBuilder.parseDnsServers(dnsList.joinToString(","))
    }

    when (settings.splitTunnelMode) {
      SplitTunnelMode.DISABLED -> Unit
      SplitTunnelMode.EXCLUDE_SELECTED -> {
        if (settings.splitTunnelPackages.isNotEmpty()) {
          interfaceBuilder.excludeApplications(settings.splitTunnelPackages)
        }
      }
      SplitTunnelMode.INCLUDE_ONLY_SELECTED -> {
        if (settings.splitTunnelPackages.isNotEmpty()) {
          interfaceBuilder.includeApplications(settings.splitTunnelPackages)
        }
      }
    }

    val allowedIps = buildSet {
      addAll(session.allowedIps.ifEmpty { listOf("0.0.0.0/0") })
      if (settings.ipv6LeakProtectionEnabled) {
        add("::/0")
      }
    }.let { set ->
      if (!settings.ipv6LeakProtectionEnabled && session.assignedIpv6.isNullOrBlank()) {
        set.filterNot { it.contains(":") }
      } else {
        set.toList()
      }
    }

    val peerBuilder = Peer.Builder()
      .parsePublicKey(session.serverPublicKey)
      .parseEndpoint(session.endpoint)
      .parseAllowedIPs(allowedIps.joinToString(","))
      .setPersistentKeepalive(session.persistentKeepaliveSec.coerceIn(15, 60))

    if (!session.presharedKey.isNullOrBlank()) {
      peerBuilder.parsePreSharedKey(session.presharedKey)
    }

    return Config.Builder()
      .setInterface(interfaceBuilder.build())
      .addPeer(peerBuilder.build())
      .build()
  }
}
