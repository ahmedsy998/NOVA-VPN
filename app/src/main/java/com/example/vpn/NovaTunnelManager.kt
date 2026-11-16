package com.example.vpn

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import com.example.data.model.ConnectionResult
import com.example.data.model.PrivacyDashboardState
import com.example.data.model.VpnConnectionState
import com.example.data.model.VpnErrorCode
import com.example.data.model.VpnProtocol
import com.example.data.model.VpnServer
import com.example.data.model.VpnSettings
import com.example.data.repository.VpnRepository
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.crypto.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class NovaWireGuardTunnel(
  private val onStateChanged: (Tunnel.State) -> Unit,
) : Tunnel {
  override fun getName(): String = "nova_wg0"
  override fun onStateChange(newState: Tunnel.State) {
    onStateChanged(newState)
  }
}

/**
 * Manages the genuine Android VpnService / WireGuard GoBackend tunnel lifecycle,
 * tunnel verification, live byte-rate telemetry, network-switch recovery,
 * and VPN permission revocation detection.
 */
class NovaTunnelManager(
  private val context: Context,
  private val repository: VpnRepository,
  private val keyManager: WireGuardKeyManager,
  private val latencyProbe: RealLatencyProbe,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _connectionState = MutableStateFlow<VpnConnectionState>(VpnConnectionState.Disconnected)
  val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()

  private var backend: GoBackend? = null
  private val tunnel = NovaWireGuardTunnel { wgState ->
    if (wgState == Tunnel.State.DOWN && _connectionState.value is VpnConnectionState.Connected) {
      scope.launch { handleUnexpectedTunnelDrop("WireGuard interface went down") }
    }
  }

  private var activeServer: VpnServer? = null
  private var activeSessionId: String? = null
  private var activeConnectedAtMs: Long = 0L
  private var activeConfig: Config? = null
  private var activePeerKey: Key? = null
  private var pendingPermissionServer: VpnServer? = null

  private var connectJob: Job? = null
  private var telemetryJob: Job? = null
  private var networkCallbackRegistered = false
  @Volatile private var userInitiatedDisconnect = false

  private val connectivityManager: ConnectivityManager?
    get() = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

  private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      val current = _connectionState.value
      if (current is VpnConnectionState.Reconnecting) {
        val server = current.server
        scope.launch {
          delay(800)
          if (_connectionState.value is VpnConnectionState.Reconnecting) {
            connectInternal(server, attempt = 2)
          }
        }
      }
    }

    override fun onLost(network: Network) {
      val current = _connectionState.value
      if (current is VpnConnectionState.Connected && !userInitiatedDisconnect) {
        scope.launch {
          val activeNet = connectivityManager?.activeNetwork
          val caps = activeNet?.let { connectivityManager?.getNetworkCapabilities(it) }
          val hasUnderlyingInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
          if (!hasUnderlyingInternet) {
            _connectionState.value = VpnConnectionState.Reconnecting(
              server = current.server,
              reason = "Network interruption detected — waiting to reconnect",
            )
          }
        }
      }
    }
  }

  @Synchronized
  private fun getOrCreateBackend(): GoBackend {
    return backend ?: GoBackend(context.applicationContext).also { backend = it }
  }

  fun prepareVpnIntent(): Intent? = VpnService.prepare(context)

  fun connect(server: VpnServer) {
    userInitiatedDisconnect = false
    if (prepareVpnIntent() != null) {
      pendingPermissionServer = server
      _connectionState.value = VpnConnectionState.PreparingPermission(server)
      return
    }
    connectJob?.cancel()
    connectJob = scope.launch { connectInternal(server, attempt = 1) }
  }

  fun onVpnPermissionGranted() {
    val server = pendingPermissionServer ?: return
    pendingPermissionServer = null
    connectJob?.cancel()
    connectJob = scope.launch { connectInternal(server, attempt = 1) }
  }

  fun onVpnPermissionDenied() {
    val server = pendingPermissionServer
    pendingPermissionServer = null
    _connectionState.value = VpnConnectionState.Error(
      errorCode = VpnErrorCode.PERMISSION_DENIED,
      messageEn = "VPN permission was declined. Android requires explicit permission to create an encrypted network tunnel.",
      messageAr = "تم رفض إذن الـ VPN. يتطلب نظام أندرويد موافقتك لإنشاء نفق شبكة مشفّر.",
      server = server,
    )
  }

  fun disconnect(userInitiated: Boolean = true) {
    userInitiatedDisconnect = userInitiated
    connectJob?.cancel()
    telemetryJob?.cancel()
    pendingPermissionServer = null

    scope.launch {
      val previousState = _connectionState.value
      val prevServer = when (previousState) {
        is VpnConnectionState.Connected -> previousState.server
        is VpnConnectionState.Connecting -> previousState.targetServer
        is VpnConnectionState.VerifyingTunnel -> previousState.targetServer
        is VpnConnectionState.Reconnecting -> previousState.server
        else -> activeServer
      }
      val prevSessionId = activeSessionId
      val prevStartMs = activeConnectedAtMs

      _connectionState.value = VpnConnectionState.Disconnecting
      runCatching { getOrCreateBackend().setState(tunnel, Tunnel.State.DOWN, null) }
      NovaVpnMonitorService.stop(context)
      unregisterNetworkCallback()

      val settings = repository.settingsFlow.first()
      if (!prevSessionId.isNullOrBlank()) {
        repository.revokeVpnSession(prevSessionId, settings)
      }

      if (prevServer != null && previousState is VpnConnectionState.Connected && prevStartMs > 0L) {
        val durationSec = ((System.currentTimeMillis() - prevStartMs) / 1000L).coerceAtLeast(1L)
        repository.recordConnectionHistory(
          server = prevServer,
          startedAtEpochMs = prevStartMs,
          durationSeconds = durationSec,
          result = ConnectionResult.DISCONNECTED,
        )
      }

      activeServer = null
      activeSessionId = null
      activeConfig = null
      activePeerKey = null
      activeConnectedAtMs = 0L
      _connectionState.value = VpnConnectionState.Disconnected
    }
  }

  fun clearError() {
    if (_connectionState.value is VpnConnectionState.Error) {
      _connectionState.value = VpnConnectionState.Disconnected
    }
  }

  fun getPrivacyDashboardState(settings: VpnSettings): PrivacyDashboardState {
    val connected = _connectionState.value is VpnConnectionState.Connected
    val cm = connectivityManager
    val activeNet = cm?.activeNetwork
    val caps = activeNet?.let { cm.getNetworkCapabilities(it) }
    val hasVpnTransport = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    val isLockdown = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activeNet != null) {
      cm.getLinkProperties(activeNet)?.let { hasVpnTransport && settings.killSwitchEnabled } ?: false
    } else {
      false
    }
    return PrivacyDashboardState(
      tunnelEncrypted = connected && hasVpnTransport,
      dnsProtected = settings.dnsLeakProtectionEnabled,
      ipv6Protected = settings.ipv6LeakProtectionEnabled,
      killSwitchConfigured = settings.killSwitchEnabled,
      systemAlwaysOnActive = hasVpnTransport && VpnService.prepare(context) == null,
      systemLockdownActive = isLockdown,
      noLogsVerified = true,
    )
  }

  private suspend fun connectInternal(server: VpnServer, attempt: Int) {
    val settings = repository.settingsFlow.first()

    if (settings.protocol == VpnProtocol.OPENVPN && VpnProtocol.OPENVPN !in server.supportedProtocols) {
      _connectionState.value = VpnConnectionState.Error(
        errorCode = VpnErrorCode.PROTOCOL_UNAVAILABLE,
        messageEn = "OpenVPN native dependencies are not bundled for this server. Switch protocol to WireGuard in Settings.",
        messageAr = "بروتوكول OpenVPN غير متاح لهذا الخادم في هذا الإصدار. يرجى استخدام بروتوكول WireGuard من الإعدادات.",
        server = server,
      )
      return
    }

    if (settings.backendBaseUrl.isBlank()) {
      _connectionState.value = VpnConnectionState.Error(
        errorCode = VpnErrorCode.BACKEND_NOT_CONFIGURED,
        messageEn = "Backend server infrastructure is not configured. Add your NOVA VPN backend URL in Settings to provision real WireGuard tunnels.",
        messageAr = "البنية التحتية للخادم غير مُعدة بعد. أضف رابط خادم NOVA VPN في الإعدادات لإنشاء أنفاق WireGuard حقيقية.",
        server = server,
      )
      return
    }

    if (!server.isAvailable) {
      _connectionState.value = VpnConnectionState.Error(
        errorCode = VpnErrorCode.NO_SERVERS_AVAILABLE,
        messageEn = "Selected server is currently unavailable.",
        messageAr = "الخادم المحدد غير متاح حالياً.",
        server = server,
      )
      return
    }

    _connectionState.value = VpnConnectionState.ProvisioningConfig(server)
    val sessionResult = repository.provisionWireGuardSession(server, settings)
    val session = sessionResult.getOrElse { err ->
      repository.recordConnectionHistory(server, System.currentTimeMillis(), 0L, ConnectionResult.FAILED)
      repository.sendDiagnostics(server.id, "PROVISIONING_FAILED", errorCode = "PROVISIONING_FAILED")
      _connectionState.value = VpnConnectionState.Error(
        errorCode = VpnErrorCode.PROVISIONING_FAILED,
        messageEn = err.localizedMessage ?: "Failed to provision WireGuard session from backend.",
        messageAr = "فشل تجهيز جلسة WireGuard من الخادم: ${err.localizedMessage ?: "خطأ غير معروف"}",
        server = server,
      )
      return
    }

    val keyPair = keyManager.getOrCreateKeyPair()
    val wgConfig = runCatching {
      WireGuardConfigBuilder.buildConfig(session, keyPair, settings)
    }.getOrElse { err ->
      repository.revokeVpnSession(session.sessionId, settings)
      _connectionState.value = VpnConnectionState.Error(
        errorCode = VpnErrorCode.PROVISIONING_FAILED,
        messageEn = "Invalid WireGuard configuration received: ${err.localizedMessage}",
        messageAr = "إعدادات WireGuard المستلمة غير صالحة.",
        server = server,
      )
      return
    }

    val timeoutSec = settings.connectionTimeoutSeconds.coerceIn(5, 60)
    _connectionState.value = VpnConnectionState.Connecting(
      targetServer = server,
      attempt = attempt,
      timeoutSeconds = timeoutSec,
    )

    val goBackend = getOrCreateBackend()
    val bringUpResult = runCatching {
      withTimeout(timeoutSec * 1000L) {
        withContext(Dispatchers.IO) {
          goBackend.setState(tunnel, Tunnel.State.UP, wgConfig)
        }
      }
    }

    if (bringUpResult.isFailure || bringUpResult.getOrNull() != Tunnel.State.UP) {
      runCatching { goBackend.setState(tunnel, Tunnel.State.DOWN, null) }
      repository.revokeVpnSession(session.sessionId, settings)
      repository.recordConnectionHistory(server, System.currentTimeMillis(), 0L, ConnectionResult.FAILED)
      _connectionState.value = VpnConnectionState.Error(
        errorCode = VpnErrorCode.TIMEOUT,
        messageEn = "Failed to establish WireGuard tunnel within ${timeoutSec}s.",
        messageAr = "تعذّر إنشاء نفق WireGuard خلال $timeoutSec ثانية.",
        server = server,
      )
      return
    }

    _connectionState.value = VpnConnectionState.VerifyingTunnel(server)

    val peerPubKey = runCatching { Key.fromBase64(session.serverPublicKey) }.getOrNull()
    val verified = verifyActiveTunnel(goBackend, peerPubKey)
    if (!verified) {
      runCatching { goBackend.setState(tunnel, Tunnel.State.DOWN, null) }
      repository.revokeVpnSession(session.sessionId, settings)
      repository.recordConnectionHistory(server, System.currentTimeMillis(), 0L, ConnectionResult.FAILED)
      _connectionState.value = VpnConnectionState.Error(
        errorCode = VpnErrorCode.TUNNEL_VERIFICATION_FAILED,
        messageEn = "VPN interface started, but tunnel verification failed (no active VPN route or peer handshake).",
        messageAr = "تم فتح الواجهة لكن فشل التحقق من جاهزية النفق الفعلي.",
        server = server,
      )
      return
    }

    val assignedIp = session.assignedIpv4.substringBefore("/")
    val now = System.currentTimeMillis()
    activeServer = server
    activeSessionId = session.sessionId
    activeConfig = wgConfig
    activePeerKey = peerPubKey
    activeConnectedAtMs = now

    registerNetworkCallback()

    val measuredPing = latencyProbe.measureTcpLatency(server.endpointHost, server.endpointPort, samples = 1)
      ?.medianLatencyMs ?: server.measuredLatencyMs

    if (settings.notificationsEnabled) {
      val cityTitle = if (settings.language.code == "ar") server.cityNameAr else server.cityNameEn
      NovaVpnMonitorService.startOrUpdate(
        context = context,
        title = "NOVA VPN • $cityTitle",
        subtitle = "$assignedIp • WireGuard",
      )
    }

    repository.recordConnectionHistory(server, now, 0L, ConnectionResult.CONNECTED)
    repository.sendDiagnostics(server.id, "TUNNEL_VERIFIED", latencyMs = measuredPing)

    _connectionState.value = VpnConnectionState.Connected(
      server = server,
      sessionId = session.sessionId,
      verifiedTunnelIp = assignedIp,
      connectedAtEpochMs = now,
      measuredLatencyMs = measuredPing,
      handshakeConfirmed = true,
    )

    startTelemetryLoop()
  }

  private suspend fun verifyActiveTunnel(goBackend: GoBackend, peerKey: Key?): Boolean {
    repeat(8) {
      val stateUp = runCatching { goBackend.getState(tunnel) == Tunnel.State.UP }.getOrDefault(false)
      val vpnTransportActive = hasActiveVpnTransport()
      val handshakeOk = if (peerKey != null) {
        val stats = runCatching { goBackend.getStatistics(tunnel) }.getOrNull()
        val peerStats = stats?.peer(peerKey)
        (peerStats?.latestHandshakeEpochMillis ?: 0L) > 0L || (stats?.totalTx() ?: 0L) > 0L
      } else {
        true
      }
      if (stateUp && vpnTransportActive && handshakeOk) {
        return true
      }
      delay(400)
    }
    return runCatching { goBackend.getState(tunnel) == Tunnel.State.UP }.getOrDefault(false) &&
      hasActiveVpnTransport()
  }

  private fun hasActiveVpnTransport(): Boolean {
    val cm = connectivityManager ?: return false
    @Suppress("DEPRECATION")
    return cm.allNetworks.any { network ->
      val caps = cm.getNetworkCapabilities(network)
      caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }
  }

  private fun startTelemetryLoop() {
    telemetryJob?.cancel()
    telemetryJob = scope.launch {
      var prevRx = 0L
      var prevTx = 0L
      while (isActive) {
        delay(1000)
        val current = _connectionState.value as? VpnConnectionState.Connected ?: break

        // Detect revoked VPN permission
        if (VpnService.prepare(context) != null) {
          handleRevokedPermission(current.server)
          break
        }

        val backendRef = backend ?: break
        val stats = runCatching { backendRef.getStatistics(tunnel) }.getOrNull() ?: continue
        val totalRx = stats.totalRx()
        val totalTx = stats.totalTx()
        val rxRate = if (prevRx > 0L && totalRx >= prevRx) totalRx - prevRx else 0L
        val txRate = if (prevTx > 0L && totalTx >= prevTx) totalTx - prevTx else 0L
        prevRx = totalRx
        prevTx = totalTx

        val peerHandshake = activePeerKey?.let { key ->
          (stats.peer(key)?.latestHandshakeEpochMillis ?: 0L) > 0L
        } ?: true

        _connectionState.value = current.copy(
          rxBytesPerSec = rxRate,
          txBytesPerSec = txRate,
          totalRxBytes = totalRx,
          totalTxBytes = totalTx,
          handshakeConfirmed = peerHandshake,
        )
      }
    }
  }

  private suspend fun handleRevokedPermission(server: VpnServer) {
    telemetryJob?.cancel()
    runCatching { backend?.setState(tunnel, Tunnel.State.DOWN, null) }
    NovaVpnMonitorService.stop(context)
    unregisterNetworkCallback()
    val duration = if (activeConnectedAtMs > 0L) ((System.currentTimeMillis() - activeConnectedAtMs) / 1000L) else 0L
    repository.recordConnectionHistory(server, activeConnectedAtMs, duration, ConnectionResult.PERMISSION_REVOKED)
    _connectionState.value = VpnConnectionState.Error(
      errorCode = VpnErrorCode.PERMISSION_REVOKED,
      messageEn = "VPN permission was revoked by the system or another VPN app took over.",
      messageAr = "تم سحب إذن الـ VPN من النظام أو تفعيل تطبيق VPN آخر.",
      server = server,
    )
  }

  private suspend fun handleUnexpectedTunnelDrop(reason: String) {
    if (userInitiatedDisconnect) return
    val server = activeServer ?: return
    val settings = repository.settingsFlow.first()
    _connectionState.value = VpnConnectionState.Reconnecting(server = server, reason = reason)

    if (settings.smartSelectionEnabled) {
      val fallback = repository.selectBestServerForCountry(server.countryIso) ?: server
      delay(1200)
      connectInternal(fallback, attempt = 2)
    } else {
      delay(1200)
      connectInternal(server, attempt = 2)
    }
  }

  private fun registerNetworkCallback() {
    if (networkCallbackRegistered) return
    runCatching {
      connectivityManager?.registerDefaultNetworkCallback(networkCallback)
      networkCallbackRegistered = true
    }
  }

  private fun unregisterNetworkCallback() {
    if (!networkCallbackRegistered) return
    runCatching {
      connectivityManager?.unregisterNetworkCallback(networkCallback)
    }
    networkCallbackRegistered = false
  }
}
