package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage(val code: String) {
  ARABIC("ar"),
  ENGLISH("en"),
}

@Serializable
enum class AppThemeMode {
  DARK,
  LIGHT,
  SYSTEM,
}

@Serializable
enum class VpnProtocol(val displayName: String) {
  WIREGUARD("WireGuard"),
  OPENVPN("OpenVPN"),
}

@Serializable
enum class ServerStatus {
  ONLINE,
  BUSY,
  MAINTENANCE,
  OFFLINE,
}

@Serializable
enum class ConnectionQuality {
  EXCELLENT,
  GOOD,
  FAIR,
  POOR,
  UNKNOWN,
}

@Serializable
enum class GeographicalRegion(val code: String) {
  ALL("ALL"),
  MENA("MENA"),
  EUROPE("EUROPE"),
  NORTH_AMERICA("NORTH_AMERICA"),
  SOUTH_AMERICA("SOUTH_AMERICA"),
  ASIA_PACIFIC("ASIA_PACIFIC"),
  SUB_SAHARAN_AFRICA("SUB_SAHARAN_AFRICA"),
  CENTRAL_ASIA("CENTRAL_ASIA"),
  CARIBBEAN_OCEANIA("CARIBBEAN_OCEANIA"),
}

@Serializable
enum class SecureDnsProvider(val dns1: String, val dns2: String, val label: String) {
  CLOUDFLARE("1.1.1.1", "1.0.0.1", "Cloudflare (1.1.1.1)"),
  QUAD9("9.9.9.9", "149.112.112.112", "Quad9 (9.9.9.9)"),
  ADGUARD("94.140.14.14", "94.140.15.15", "AdGuard DNS"),
  MULLVAD("194.242.2.2", "194.242.2.3", "Mullvad DoH/DNS"),
}

@Serializable
enum class SplitTunnelMode {
  DISABLED,
  EXCLUDE_SELECTED,
  INCLUDE_ONLY_SELECTED,
}

@Serializable
data class VpnServer(
  val id: String,
  val countryIso: String,
  val cityId: String,
  val cityNameEn: String,
  val cityNameAr: String,
  val hostname: String,
  val endpointHost: String,
  val endpointPort: Int,
  val publicKey: String,
  val supportedProtocols: List<VpnProtocol> = listOf(VpnProtocol.WIREGUARD),
  val status: ServerStatus = ServerStatus.OFFLINE,
  val loadPercent: Int? = null,
  val availableCapacityPercent: Int? = null,
  val measuredLatencyMs: Int? = null,
  val jitterMs: Int? = null,
  val bandwidthCapacityMbps: Int? = null,
  val isAvailable: Boolean = false,
  val lastHealthCheckEpochMs: Long? = null,
) {
  val connectionQuality: ConnectionQuality
    get() {
      if (status != ServerStatus.ONLINE || !isAvailable) return ConnectionQuality.UNKNOWN
      val latency = measuredLatencyMs ?: return ConnectionQuality.UNKNOWN
      val load = loadPercent ?: 50
      return when {
        latency < 65 && load < 55 -> ConnectionQuality.EXCELLENT
        latency < 130 && load < 75 -> ConnectionQuality.GOOD
        latency < 220 && load < 90 -> ConnectionQuality.FAIR
        else -> ConnectionQuality.POOR
      }
    }
}

@Serializable
data class CityLocation(
  val id: String,
  val countryIso: String,
  val nameEn: String,
  val nameAr: String,
  val servers: List<VpnServer> = emptyList(),
)

@Serializable
data class CountryLocation(
  val isoCode: String,
  val nameEn: String,
  val nameAr: String,
  val flagEmoji: String,
  val region: GeographicalRegion,
  val cities: List<CityLocation> = emptyList(),
  val isFavorite: Boolean = false,
  val lastConnectedEpochMs: Long? = null,
) {
  val allServers: List<VpnServer>
    get() = cities.flatMap { it.servers }

  val availableServerCount: Int
    get() = allServers.count { it.isAvailable && it.status == ServerStatus.ONLINE }

  val totalServerCount: Int
    get() = allServers.size

  val hasDeployedServers: Boolean
    get() = allServers.isNotEmpty()

  val bestLatencyMs: Int?
    get() = allServers.filter { it.isAvailable }.mapNotNull { it.measuredLatencyMs }.minOrNull()

  val averageLoadPercent: Int?
    get() {
      val loads = allServers.filter { it.isAvailable }.mapNotNull { it.loadPercent }
      return if (loads.isEmpty()) null else loads.average().toInt()
    }
}

sealed interface VpnConnectionState {
  data object Disconnected : VpnConnectionState
  data class PreparingPermission(val targetServer: VpnServer) : VpnConnectionState
  data class ProvisioningConfig(val targetServer: VpnServer) : VpnConnectionState
  data class Connecting(
    val targetServer: VpnServer,
    val attempt: Int,
    val timeoutSeconds: Int,
  ) : VpnConnectionState

  data class VerifyingTunnel(val targetServer: VpnServer) : VpnConnectionState

  data class Connected(
    val server: VpnServer,
    val sessionId: String,
    val verifiedTunnelIp: String,
    val connectedAtEpochMs: Long,
    val rxBytesPerSec: Long = 0L,
    val txBytesPerSec: Long = 0L,
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L,
    val measuredLatencyMs: Int? = null,
    val handshakeConfirmed: Boolean = true,
  ) : VpnConnectionState

  data class Reconnecting(
    val server: VpnServer,
    val reason: String,
  ) : VpnConnectionState

  data object Disconnecting : VpnConnectionState

  data class Error(
    val errorCode: VpnErrorCode,
    val messageEn: String,
    val messageAr: String,
    val server: VpnServer? = null,
  ) : VpnConnectionState
}

enum class VpnErrorCode {
  BACKEND_NOT_CONFIGURED,
  NO_SERVERS_AVAILABLE,
  PERMISSION_DENIED,
  PERMISSION_REVOKED,
  PROVISIONING_FAILED,
  TIMEOUT,
  TUNNEL_VERIFICATION_FAILED,
  NETWORK_UNAVAILABLE,
  PROTOCOL_UNAVAILABLE,
  UNKNOWN,
}

@Serializable
enum class SpeedTestPhase {
  IDLE,
  PING,
  DOWNLOAD,
  UPLOAD,
  COMPLETED,
  FAILED,
}

@Serializable
data class SpeedTestMeasurement(
  val downloadMbps: Double? = null,
  val uploadMbps: Double? = null,
  val pingMs: Int? = null,
  val jitterMs: Int? = null,
  val overVpn: Boolean = false,
  val serverLabel: String? = null,
  val measuredAtEpochMs: Long = System.currentTimeMillis(),
)

data class SpeedTestUiState(
  val phase: SpeedTestPhase = SpeedTestPhase.IDLE,
  val progress: Float = 0f,
  val liveDownloadMbps: Double? = null,
  val liveUploadMbps: Double? = null,
  val livePingMs: Int? = null,
  val liveJitterMs: Int? = null,
  val beforeVpn: SpeedTestMeasurement? = null,
  val afterVpn: SpeedTestMeasurement? = null,
  val statusMessageEn: String? = null,
  val statusMessageAr: String? = null,
)

@Serializable
enum class ConnectionResult {
  CONNECTED,
  DISCONNECTED,
  FAILED,
  PERMISSION_REVOKED,
}

data class ConnectionHistoryRecord(
  val id: Long = 0,
  val countryIso: String,
  val countryNameEn: String,
  val countryNameAr: String,
  val cityNameEn: String,
  val cityNameAr: String,
  val serverId: String,
  val protocol: VpnProtocol,
  val startedAtEpochMs: Long,
  val durationSeconds: Long,
  val result: ConnectionResult,
)

data class InstalledAppInfo(
  val packageName: String,
  val appName: String,
)

data class VpnSettings(
  val language: AppLanguage = AppLanguage.ARABIC,
  val themeMode: AppThemeMode = AppThemeMode.DARK,
  val protocol: VpnProtocol = VpnProtocol.WIREGUARD,
  val smartSelectionEnabled: Boolean = true,
  val killSwitchEnabled: Boolean = true,
  val dnsLeakProtectionEnabled: Boolean = true,
  val secureDnsProvider: SecureDnsProvider = SecureDnsProvider.CLOUDFLARE,
  val ipv6LeakProtectionEnabled: Boolean = true,
  val autoConnectOnBoot: Boolean = false,
  val autoConnectOnWifi: Boolean = false,
  val splitTunnelMode: SplitTunnelMode = SplitTunnelMode.DISABLED,
  val splitTunnelPackages: Set<String> = emptySet(),
  val notificationsEnabled: Boolean = true,
  val connectionTimeoutSeconds: Int = 15,
  val backendBaseUrl: String = "",
  val authEmail: String = "",
  val isAuthenticated: Boolean = false,
)

data class PrivacyDashboardState(
  val tunnelEncrypted: Boolean,
  val dnsProtected: Boolean,
  val ipv6Protected: Boolean,
  val killSwitchConfigured: Boolean,
  val systemAlwaysOnActive: Boolean,
  val systemLockdownActive: Boolean,
  val noLogsVerified: Boolean = true,
)
