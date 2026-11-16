package com.example.vpn

import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LatencySampleResult(
  val medianLatencyMs: Int,
  val jitterMs: Int,
)

/**
 * Measures genuine TCP handshake round-trip latency and jitter against reachable server hosts.
 * Never fabricates or synthesizes ping values when a host is unreachable or unconfigured.
 */
class RealLatencyProbe {

  suspend fun measureTcpLatency(
    host: String,
    port: Int = 443,
    samples: Int = 3,
    timeoutMs: Int = 2500,
  ): LatencySampleResult? = withContext(Dispatchers.IO) {
    if (host.isBlank() || port <= 0) return@withContext null
    val timings = mutableListOf<Int>()
    repeat(samples.coerceIn(1, 5)) {
      val elapsed = runCatching {
        val startNs = System.nanoTime()
        Socket().use { socket ->
          socket.connect(InetSocketAddress(host, port), timeoutMs)
        }
        ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceAtLeast(1)
      }.getOrNull()
      if (elapsed != null) {
        timings.add(elapsed)
      }
    }
    if (timings.isEmpty()) return@withContext null
    val sorted = timings.sorted()
    val median = sorted[sorted.size / 2]
    val jitter = if (timings.size >= 2) {
      timings.zipWithNext { a, b -> abs(a - b) }.average().roundToInt()
    } else {
      0
    }
    LatencySampleResult(medianLatencyMs = median, jitterMs = jitter)
  }
}
