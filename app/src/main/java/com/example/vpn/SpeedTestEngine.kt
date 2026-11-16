package com.example.vpn

import com.example.data.model.SpeedTestMeasurement
import com.example.data.model.SpeedTestPhase
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink

data class SpeedTestProgressUpdate(
  val phase: SpeedTestPhase,
  val progress: Float,
  val pingMs: Int? = null,
  val jitterMs: Int? = null,
  val downloadMbps: Double? = null,
  val uploadMbps: Double? = null,
  val completedMeasurement: SpeedTestMeasurement? = null,
  val errorMessageEn: String? = null,
  val errorMessageAr: String? = null,
)

/**
 * Real HTTP throughput and latency measurement engine.
 * Measures actual ping, jitter, download Mbps, and upload Mbps against a configured
 * NOVA VPN speedtest endpoint. Never generates simulated speeds.
 */
class SpeedTestEngine(private val httpClient: OkHttpClient) {

  fun runSpeedTest(
    baseUrl: String,
    overVpn: Boolean,
    serverLabel: String?,
  ): Flow<SpeedTestProgressUpdate> = flow {
    val cleanBase = baseUrl.trim().trimEnd('/')
    if (cleanBase.isEmpty()) {
      emit(
        SpeedTestProgressUpdate(
          phase = SpeedTestPhase.FAILED,
          progress = 0f,
          errorMessageEn = "Backend endpoint is not configured. Configure your NOVA VPN backend URL in Settings to run real speed measurements.",
          errorMessageAr = "نقطة نهاية الخادم غير مُعدة. يرجى ضبط رابط خادم NOVA VPN في الإعدادات لإجراء قياس سرعة حقيقي.",
        ),
      )
      return@flow
    }

    // 1. Ping & Jitter (5 probes to /api/v1/speedtest/ping)
    emit(SpeedTestProgressUpdate(phase = SpeedTestPhase.PING, progress = 0.05f))
    val pingTimings = mutableListOf<Int>()
    for (i in 0 until 5) {
      currentCoroutineContext().ensureActive()
      val elapsed = measureHttpPing("$cleanBase/api/v1/speedtest/ping")
      if (elapsed != null) {
        pingTimings.add(elapsed)
      }
      emit(
        SpeedTestProgressUpdate(
          phase = SpeedTestPhase.PING,
          progress = 0.05f + ((i + 1) / 5f) * 0.20f,
          pingMs = pingTimings.minOrNull(),
        ),
      )
    }

    if (pingTimings.isEmpty()) {
      emit(
        SpeedTestProgressUpdate(
          phase = SpeedTestPhase.FAILED,
          progress = 0f,
          errorMessageEn = "Speed test endpoint unreachable. Check internet connectivity or backend URL.",
          errorMessageAr = "تعذّر الوصول إلى خادم قياس السرعة. تحقق من الاتصال بالإنترنت أو رابط الخادم.",
        ),
      )
      return@flow
    }

    val sortedPings = pingTimings.sorted()
    val medianPing = sortedPings[sortedPings.size / 2]
    val jitter = if (pingTimings.size >= 2) {
      pingTimings.zipWithNext { a, b -> abs(a - b) }.average().roundToInt()
    } else {
      0
    }

    // 2. Download test (/api/v1/speedtest/download?bytes=8388608)
    emit(
      SpeedTestProgressUpdate(
        phase = SpeedTestPhase.DOWNLOAD,
        progress = 0.28f,
        pingMs = medianPing,
        jitterMs = jitter,
      ),
    )

    val downloadMbps = runCatching {
      measureDownload("$cleanBase/api/v1/speedtest/download?bytes=8388608") { frac, currentMbps ->
        // Callback captured below via local state
      }
    }

    val measuredDown = runDownloadWithProgress(
      url = "$cleanBase/api/v1/speedtest/download?bytes=8388608",
      onProgress = { frac, mbps ->
        emit(
          SpeedTestProgressUpdate(
            phase = SpeedTestPhase.DOWNLOAD,
            progress = 0.28f + frac * 0.36f,
            pingMs = medianPing,
            jitterMs = jitter,
            downloadMbps = mbps,
          ),
        )
      },
    )

    if (measuredDown == null) {
      emit(
        SpeedTestProgressUpdate(
          phase = SpeedTestPhase.FAILED,
          progress = 0.30f,
          pingMs = medianPing,
          jitterMs = jitter,
          errorMessageEn = "Download measurement failed or timed out.",
          errorMessageAr = "فشل قياس سرعة التنزيل أو انتهت مهلة الاتصال.",
        ),
      )
      return@flow
    }

    // 3. Upload test (/api/v1/speedtest/upload)
    emit(
      SpeedTestProgressUpdate(
        phase = SpeedTestPhase.UPLOAD,
        progress = 0.66f,
        pingMs = medianPing,
        jitterMs = jitter,
        downloadMbps = measuredDown,
      ),
    )

    val measuredUp = runUploadWithProgress(
      url = "$cleanBase/api/v1/speedtest/upload",
      totalBytes = 4L * 1024L * 1024L,
      onProgress = { frac, mbps ->
        emit(
          SpeedTestProgressUpdate(
            phase = SpeedTestPhase.UPLOAD,
            progress = 0.66f + frac * 0.34f,
            pingMs = medianPing,
            jitterMs = jitter,
            downloadMbps = measuredDown,
            uploadMbps = mbps,
          ),
        )
      },
    )

    val finalMeasurement = SpeedTestMeasurement(
      downloadMbps = measuredDown,
      uploadMbps = measuredUp,
      pingMs = medianPing,
      jitterMs = jitter,
      overVpn = overVpn,
      serverLabel = serverLabel,
      measuredAtEpochMs = System.currentTimeMillis(),
    )

    emit(
      SpeedTestProgressUpdate(
        phase = SpeedTestPhase.COMPLETED,
        progress = 1f,
        pingMs = medianPing,
        jitterMs = jitter,
        downloadMbps = measuredDown,
        uploadMbps = measuredUp,
        completedMeasurement = finalMeasurement,
      ),
    )
  }.flowOn(Dispatchers.IO)

  private fun measureHttpPing(url: String): Int? = runCatching {
    val request = Request.Builder().url(url).get().build()
    val startNs = System.nanoTime()
    httpClient.newCall(request).execute().use { response ->
      if (!response.isSuccessful) return null
      ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceAtLeast(1)
    }
  }.getOrNull()

  private fun measureDownload(url: String, onSample: (Float, Double) -> Unit): Double? {
    val request = Request.Builder().url(url).get().build()
    httpClient.newCall(request).execute().use { response ->
      if (!response.isSuccessful) return null
      val body = response.body ?: return null
      val contentLength = body.contentLength().takeIf { it > 0 } ?: (8L * 1024L * 1024L)
      val source = body.source()
      val buffer = ByteArray(64 * 1024)
      var totalRead = 0L
      val startNs = System.nanoTime()
      while (true) {
        val read = source.read(buffer)
        if (read == -1) break
        totalRead += read
        val elapsedSec = ((System.nanoTime() - startNs) / 1_000_000_000.0).coerceAtLeast(0.05)
        val mbps = (totalRead * 8.0) / (elapsedSec * 1_000_000.0)
        onSample((totalRead.toFloat() / contentLength).coerceIn(0f, 1f), mbps)
      }
      val totalSec = ((System.nanoTime() - startNs) / 1_000_000_000.0).coerceAtLeast(0.01)
      return (totalRead * 8.0) / (totalSec * 1_000_000.0)
    }
  }

  private suspend fun runDownloadWithProgress(
    url: String,
    onProgress: suspend (Float, Double) -> Unit,
  ): Double? {
    val request = Request.Builder().url(url).get().build()
    return runCatching {
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@use null
        val body = response.body ?: return@use null
        val expected = body.contentLength().takeIf { it > 0 } ?: (8L * 1024L * 1024L)
        val stream = body.byteStream()
        val buf = ByteArray(64 * 1024)
        var total = 0L
        val startNs = System.nanoTime()
        var lastEmitNs = startNs
        while (true) {
          currentCoroutineContext().ensureActive()
          val count = stream.read(buf)
          if (count <= 0) break
          total += count
          val now = System.nanoTime()
          if (now - lastEmitNs >= 180_000_000L) {
            val sec = ((now - startNs) / 1_000_000_000.0).coerceAtLeast(0.05)
            val mbps = (total * 8.0) / (sec * 1_000_000.0)
            onProgress((total.toFloat() / expected).coerceIn(0f, 1f), mbps)
            lastEmitNs = now
          }
        }
        val totalSec = ((System.nanoTime() - startNs) / 1_000_000_000.0).coerceAtLeast(0.01)
        (total * 8.0) / (totalSec * 1_000_000.0)
      }
    }.getOrNull()
  }

  private suspend fun runUploadWithProgress(
    url: String,
    totalBytes: Long,
    onProgress: suspend (Float, Double) -> Unit,
  ): Double? {
    val startNs = System.nanoTime()
    var lastMbps = 0.0
    val chunk = ByteArray(32 * 1024) { 0x4E }
    val body = object : RequestBody() {
      override fun contentType() = "application/octet-stream".toMediaType()
      override fun contentLength() = totalBytes
      override fun writeTo(sink: BufferedSink) {
        var sent = 0L
        while (sent < totalBytes) {
          val toWrite = minOf(chunk.size.toLong(), totalBytes - sent).toInt()
          sink.write(chunk, 0, toWrite)
          sent += toWrite
          val elapsed = ((System.nanoTime() - startNs) / 1_000_000_000.0).coerceAtLeast(0.05)
          lastMbps = (sent * 8.0) / (elapsed * 1_000_000.0)
        }
      }
    }
    return runCatching {
      val request = Request.Builder().url(url).post(body).build()
      onProgress(0.35f, 0.0)
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
      }
      val totalSec = ((System.nanoTime() - startNs) / 1_000_000_000.0).coerceAtLeast(0.01)
      val finalMbps = (totalBytes * 8.0) / (totalSec * 1_000_000.0)
      onProgress(1f, finalMbps)
      finalMbps
    }.getOrNull() ?: lastMbps.takeIf { it > 0.0 }
  }
}
