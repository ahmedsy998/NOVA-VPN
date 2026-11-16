package com.example.vpn

import com.example.data.model.ConnectionHistoryRecord
import com.example.data.model.ConnectionResult
import com.example.data.model.ServerStatus
import com.example.data.model.VpnProtocol
import com.example.data.model.VpnServer
import kotlin.math.max

data class ScoredServer(
  val server: VpnServer,
  val score: Double,
)

/**
 * Smart Server Selection Engine.
 * Evaluates eligible servers using:
 * 1. Lowest measured latency
 * 2. Lowest server load
 * 3. Actual server availability & capacity
 * 4. Network stability (jitter)
 * 5. Recent connection reliability from local history
 *
 * Strictly preserves the user's selected exit country when countryIso is specified.
 */
class SmartServerSelector {

  fun selectBestServer(
    servers: List<VpnServer>,
    countryIso: String? = null,
    preferredProtocol: VpnProtocol = VpnProtocol.WIREGUARD,
    recentHistory: List<ConnectionHistoryRecord> = emptyList(),
    excludeServerIds: Set<String> = emptySet(),
  ): VpnServer? {
    return rankServers(
      servers = servers,
      countryIso = countryIso,
      preferredProtocol = preferredProtocol,
      recentHistory = recentHistory,
      excludeServerIds = excludeServerIds,
    ).firstOrNull()?.server
  }

  fun rankServers(
    servers: List<VpnServer>,
    countryIso: String? = null,
    preferredProtocol: VpnProtocol = VpnProtocol.WIREGUARD,
    recentHistory: List<ConnectionHistoryRecord> = emptyList(),
    excludeServerIds: Set<String> = emptySet(),
  ): List<ScoredServer> {
    val candidates = servers.filter { server ->
      val matchesCountry = countryIso == null || server.countryIso.equals(countryIso, ignoreCase = true)
      val notExcluded = server.id !in excludeServerIds
      val operational = server.isAvailable &&
        (server.status == ServerStatus.ONLINE || server.status == ServerStatus.BUSY)
      val supportsProtocol = preferredProtocol in server.supportedProtocols
      matchesCountry && notExcluded && operational && supportsProtocol
    }

    if (candidates.isEmpty()) return emptyList()

    val historyByServer = recentHistory.groupBy { it.serverId }

    return candidates.map { server ->
      val latencyPenalty = (server.measuredLatencyMs ?: 250).coerceIn(1, 1000) * 0.45
      val loadPenalty = (server.loadPercent ?: 60).coerceIn(0, 100) * 0.30
      val capacityBonus = (server.availableCapacityPercent ?: 40).coerceIn(0, 100) * 0.15
      val jitterPenalty = (server.jitterMs ?: 20).coerceIn(0, 300) * 0.15
      val busyPenalty = if (server.status == ServerStatus.BUSY) 25.0 else 0.0

      val records = historyByServer[server.id].orEmpty()
      val recentBonus = if (records.isEmpty()) {
        0.0
      } else {
        val successes = records.count {
          it.result == ConnectionResult.CONNECTED || it.result == ConnectionResult.DISCONNECTED
        }
        val failures = records.count { it.result == ConnectionResult.FAILED }
        (successes * 3.0) - (failures * 12.0)
      }

      // Higher score = better candidate
      val rawScore = 1000.0 - latencyPenalty - loadPenalty - jitterPenalty - busyPenalty +
        capacityBonus + recentBonus
      ScoredServer(server = server, score = max(0.0, rawScore))
    }.sortedByDescending { it.score }
  }
}
