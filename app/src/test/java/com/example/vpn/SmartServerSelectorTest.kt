package com.example.vpn

import com.example.data.model.ConnectionHistoryRecord
import com.example.data.model.ConnectionResult
import com.example.data.model.ServerStatus
import com.example.data.model.VpnProtocol
import com.example.data.model.VpnServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartServerSelectorTest {

  private val selector = SmartServerSelector()

  private fun sampleServer(
    id: String,
    countryIso: String,
    status: ServerStatus = ServerStatus.ONLINE,
    isAvailable: Boolean = true,
    latencyMs: Int? = 40,
    loadPercent: Int? = 25,
    capacityPercent: Int? = 75,
    jitterMs: Int? = 4,
  ) = VpnServer(
    id = id,
    countryIso = countryIso,
    cityId = "${countryIso.lowercase()}-main",
    cityNameEn = "City",
    cityNameAr = "مدينة",
    hostname = "$id.example.net",
    endpointHost = "198.51.100.1",
    endpointPort = 51820,
    publicKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    supportedProtocols = listOf(VpnProtocol.WIREGUARD),
    status = status,
    loadPercent = loadPercent,
    availableCapacityPercent = capacityPercent,
    measuredLatencyMs = latencyMs,
    jitterMs = jitterMs,
    isAvailable = isAvailable,
  )

  @Test
  fun `never routes outside selected country`() {
    val servers = listOf(
      sampleServer("de-1", "DE", latencyMs = 12, loadPercent = 5),
      sampleServer("us-1", "US", latencyMs = 150, loadPercent = 60),
    )

    val selectedForFr = selector.selectBestServer(servers = servers, countryIso = "FR")
    assertNull(selectedForFr)

    val selectedForUs = selector.selectBestServer(servers = servers, countryIso = "US")
    assertEquals("us-1", selectedForUs?.id)
  }

  @Test
  fun `selects lowest latency and lowest load among online servers`() {
    val servers = listOf(
      sampleServer("de-slow", "DE", latencyMs = 140, loadPercent = 70, capacityPercent = 30),
      sampleServer("de-fast", "DE", latencyMs = 24, loadPercent = 18, capacityPercent = 82),
      sampleServer("de-offline", "DE", status = ServerStatus.OFFLINE, isAvailable = false, latencyMs = 10),
    )

    val best = selector.selectBestServer(servers = servers, countryIso = "DE")
    assertEquals("de-fast", best?.id)
  }

  @Test
  fun `penalizes servers with recent failed connections`() {
    val servers = listOf(
      sampleServer("nl-flaky", "NL", latencyMs = 25, loadPercent = 20),
      sampleServer("nl-steady", "NL", latencyMs = 32, loadPercent = 22),
    )
    val history = listOf(
      ConnectionHistoryRecord(
        countryIso = "NL",
        countryNameEn = "Netherlands",
        countryNameAr = "هولندا",
        cityNameEn = "Amsterdam",
        cityNameAr = "أمستردام",
        serverId = "nl-flaky",
        protocol = VpnProtocol.WIREGUARD,
        startedAtEpochMs = 1000L,
        durationSeconds = 0L,
        result = ConnectionResult.FAILED,
      ),
      ConnectionHistoryRecord(
        countryIso = "NL",
        countryNameEn = "Netherlands",
        countryNameAr = "هولندا",
        cityNameEn = "Amsterdam",
        cityNameAr = "أمستردام",
        serverId = "nl-flaky",
        protocol = VpnProtocol.WIREGUARD,
        startedAtEpochMs = 2000L,
        durationSeconds = 0L,
        result = ConnectionResult.FAILED,
      ),
    )

    val best = selector.selectBestServer(servers = servers, countryIso = "NL", recentHistory = history)
    assertEquals("nl-steady", best?.id)
  }
}
