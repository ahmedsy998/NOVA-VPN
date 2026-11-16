package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRequestDto(
  val email: String,
  val password: String,
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
  val token: String,
  @Json(name = "expires_at") val expiresAt: String,
  val user: UserDto,
)

@JsonClass(generateAdapter = true)
data class UserDto(
  val id: String,
  val email: String,
  val tier: String = "standard",
)

@JsonClass(generateAdapter = true)
data class LocationsResponseDto(
  val regions: List<RegionDto> = emptyList(),
  val countries: List<CountryDeploymentDto> = emptyList(),
  @Json(name = "updated_at") val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class RegionDto(
  val code: String,
  @Json(name = "name_en") val nameEn: String,
  @Json(name = "name_ar") val nameAr: String,
)

@JsonClass(generateAdapter = true)
data class CountryDeploymentDto(
  @Json(name = "iso_code") val isoCode: String,
  @Json(name = "name_en") val nameEn: String,
  @Json(name = "name_ar") val nameAr: String,
  val region: String,
  val cities: List<CityDeploymentDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CityDeploymentDto(
  val id: String,
  @Json(name = "country_iso") val countryIso: String,
  @Json(name = "name_en") val nameEn: String,
  @Json(name = "name_ar") val nameAr: String,
  val servers: List<ServerDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ServersResponseDto(
  val servers: List<ServerDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ServerDto(
  val id: String,
  @Json(name = "country_iso") val countryIso: String,
  @Json(name = "city_id") val cityId: String,
  @Json(name = "city_name_en") val cityNameEn: String,
  @Json(name = "city_name_ar") val cityNameAr: String,
  val hostname: String,
  @Json(name = "endpoint_host") val endpointHost: String,
  @Json(name = "endpoint_port") val endpointPort: Int,
  @Json(name = "public_key") val publicKey: String,
  @Json(name = "supported_protocols") val supportedProtocols: List<String> = listOf("WIREGUARD"),
  val status: String,
  @Json(name = "load_percent") val loadPercent: Int? = null,
  @Json(name = "available_capacity_percent") val availableCapacityPercent: Int? = null,
  @Json(name = "latency_ms") val latencyMs: Int? = null,
  @Json(name = "jitter_ms") val jitterMs: Int? = null,
  @Json(name = "bandwidth_capacity_mbps") val bandwidthCapacityMbps: Int? = null,
  @Json(name = "is_available") val isAvailable: Boolean = false,
  @Json(name = "last_health_check") val lastHealthCheck: String? = null,
)

@JsonClass(generateAdapter = true)
data class ServerHealthDto(
  @Json(name = "server_id") val serverId: String,
  val status: String,
  @Json(name = "load_percent") val loadPercent: Int,
  @Json(name = "available_capacity_percent") val availableCapacityPercent: Int,
  @Json(name = "active_sessions") val activeSessions: Int,
  @Json(name = "packet_loss_percent") val packetLossPercent: Double,
  @Json(name = "checked_at") val checkedAt: String,
)

@JsonClass(generateAdapter = true)
data class CreateVpnSessionRequestDto(
  @Json(name = "server_id") val serverId: String,
  @Json(name = "client_public_key") val clientPublicKey: String,
  val protocol: String = "WIREGUARD",
  @Json(name = "preferred_dns") val preferredDns: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateVpnSessionResponseDto(
  @Json(name = "session_id") val sessionId: String,
  @Json(name = "server_id") val serverId: String,
  @Json(name = "assigned_ipv4") val assignedIpv4: String,
  @Json(name = "assigned_ipv6") val assignedIpv6: String? = null,
  @Json(name = "dns_servers") val dnsServers: List<String> = emptyList(),
  @Json(name = "server_public_key") val serverPublicKey: String,
  @Json(name = "preshared_key") val presharedKey: String? = null,
  val endpoint: String,
  @Json(name = "allowed_ips") val allowedIps: List<String> = listOf("0.0.0.0/0", "::/0"),
  @Json(name = "persistent_keepalive_sec") val persistentKeepaliveSec: Int = 25,
  val mtu: Int = 1280,
  @Json(name = "expires_at") val expiresAt: String,
)

@JsonClass(generateAdapter = true)
data class DiagnosticsRequestDto(
  @Json(name = "server_id") val serverId: String?,
  val event: String,
  @Json(name = "measured_latency_ms") val measuredLatencyMs: Int? = null,
  @Json(name = "error_code") val errorCode: String? = null,
  @Json(name = "app_version") val appVersion: String,
)
