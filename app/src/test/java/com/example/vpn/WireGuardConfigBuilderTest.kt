package com.example.vpn

import com.example.data.model.SecureDnsProvider
import com.example.data.model.SplitTunnelMode
import com.example.data.model.VpnSettings
import com.example.data.remote.CreateVpnSessionResponseDto
import com.wireguard.config.InetNetwork
import com.wireguard.crypto.KeyPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WireGuardConfigBuilderTest {

  @Test
  fun `enforces DNS leak protection, IPv6 leak protection, and split tunneling`() {
    val clientKeyPair = KeyPair()
    val serverKeyPair = KeyPair()
    val session = CreateVpnSessionResponseDto(
      sessionId = "sess-123",
      serverId = "de-fra-01",
      assignedIpv4 = "10.66.0.5/32",
      assignedIpv6 = null,
      dnsServers = emptyList(),
      serverPublicKey = serverKeyPair.publicKey.toBase64(),
      presharedKey = null,
      endpoint = "203.0.113.10:51820",
      allowedIps = listOf("0.0.0.0/0"),
      persistentKeepaliveSec = 25,
      mtu = 1280,
      expiresAt = "2026-12-01T00:00:00Z",
    )
    val settings = VpnSettings(
      dnsLeakProtectionEnabled = true,
      secureDnsProvider = SecureDnsProvider.QUAD9,
      ipv6LeakProtectionEnabled = true,
      splitTunnelMode = SplitTunnelMode.EXCLUDE_SELECTED,
      splitTunnelPackages = setOf("com.example.excluded"),
    )

    val config = WireGuardConfigBuilder.buildConfig(session, clientKeyPair, settings)

    assertEquals(clientKeyPair.privateKey, config.`interface`.keyPair.privateKey)
    assertTrue(config.`interface`.dnsServers.any { it.hostAddress == "9.9.9.9" })
    assertTrue(config.`interface`.addresses.contains(InetNetwork.parse("10.66.0.5/32")))
    assertTrue(config.`interface`.addresses.contains(InetNetwork.parse("fd4e:6f76:61::2/128")))
    assertTrue(config.`interface`.excludedApplications.contains("com.example.excluded"))

    val peer = config.peers.first()
    assertTrue(peer.allowedIps.contains(InetNetwork.parse("::/0")))
    assertTrue(peer.allowedIps.contains(InetNetwork.parse("0.0.0.0/0")))
  }
}
