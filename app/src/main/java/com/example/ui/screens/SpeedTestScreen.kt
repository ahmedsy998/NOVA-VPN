package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SpeedTestMeasurement
import com.example.data.model.SpeedTestPhase
import com.example.data.model.SpeedTestUiState
import com.example.ui.components.MetricTile
import com.example.ui.components.NovaGlassCard
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaElectricBlue
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaViolet

@Composable
fun SpeedTestScreen(
  state: SpeedTestUiState,
  onStartTest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val animatedProgress by animateFloatAsState(
    targetValue = state.progress.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 320),
    label = "speed_progress",
  )

  val isRunning = state.phase in setOf(
    SpeedTestPhase.PING,
    SpeedTestPhase.DOWNLOAD,
    SpeedTestPhase.UPLOAD,
  )

  val primaryGaugeValue = when (state.phase) {
    SpeedTestPhase.UPLOAD -> state.liveUploadMbps
    else -> state.liveDownloadMbps
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 640.dp)
        .align(Alignment.TopCenter)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = strings.speedTestTitle,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = strings.speedTestSubtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Futuristic Radial Speed Gauge
      Box(
        modifier = Modifier.size(210.dp),
        contentAlignment = Alignment.Center,
      ) {
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        Canvas(modifier = Modifier.size(210.dp)) {
          val stroke = 14.dp.toPx()
          val arcSize = Size(size.width - stroke, size.height - stroke)
          val topLeft = Offset(stroke / 2f, stroke / 2f)

          drawArc(
            color = trackColor,
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
          )

          drawArc(
            brush = Brush.sweepGradient(
              colors = listOf(NovaElectricBlue, NovaCyan, NovaEmerald, NovaViolet),
            ),
            startAngle = 140f,
            sweepAngle = 260f * animatedProgress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = primaryGaugeValue?.let { String.format("%.1f", it) } ?: "--",
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = strings.mbpsUnit,
            style = MaterialTheme.typography.labelLarge,
            color = NovaCyan,
          )
        }
      }

      // 4 Real Metrics Grid
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        MetricTile(
          icon = Icons.Rounded.ArrowDownward,
          label = strings.downloadLabel,
          value = state.liveDownloadMbps?.let { String.format("%.1f %s", it, strings.mbpsUnit) }
            ?: strings.notMeasured,
          accentColor = NovaCyan,
          modifier = Modifier.weight(1f),
        )
        MetricTile(
          icon = Icons.Rounded.ArrowUpward,
          label = strings.uploadLabel,
          value = state.liveUploadMbps?.let { String.format("%.1f %s", it, strings.mbpsUnit) }
            ?: strings.notMeasured,
          accentColor = NovaViolet,
          modifier = Modifier.weight(1f),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        MetricTile(
          icon = Icons.Rounded.Speed,
          label = strings.latencyLabel,
          value = state.livePingMs?.let { "$it ${strings.pingMsUnit}" } ?: strings.notMeasured,
          accentColor = NovaEmerald,
          modifier = Modifier.weight(1f),
        )
        MetricTile(
          icon = Icons.Rounded.GraphicEq,
          label = strings.jitterLabel,
          value = state.liveJitterMs?.let { "$it ${strings.pingMsUnit}" } ?: strings.notMeasured,
          accentColor = NovaAmber,
          modifier = Modifier.weight(1f),
        )
      }

      // Honest status / error message
      val msg = if (strings.isArabic) state.statusMessageAr else state.statusMessageEn
      if (!msg.isNullOrBlank()) {
        NovaGlassCard(accentBorder = NovaAmber.copy(alpha = 0.45f)) {
          Text(
            text = msg,
            style = MaterialTheme.typography.bodyMedium,
            color = NovaAmber,
          )
        }
      }

      Button(
        onClick = onStartTest,
        enabled = !isRunning,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("start_speed_test_button"),
      ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(
          text = if (isRunning) strings.runningTest else strings.startSpeedTest,
          fontWeight = FontWeight.Bold,
        )
      }

      // Before vs After VPN Comparison
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        ComparisonCard(
          title = strings.beforeVpnTitle,
          measurement = state.beforeVpn,
          accent = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.weight(1f),
        )
        ComparisonCard(
          title = strings.afterVpnTitle,
          measurement = state.afterVpn,
          accent = NovaCyan,
          modifier = Modifier.weight(1f),
        )
      }

      NovaGlassCard {
        Row(
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = strings.speedTestDisclaimer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun ComparisonCard(
  title: String,
  measurement: SpeedTestMeasurement?,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  NovaGlassCard(
    modifier = modifier,
    accentBorder = accent.copy(alpha = 0.4f),
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = accent,
    )
    Spacer(modifier = Modifier.height(6.dp))
    if (measurement == null) {
      Text(
        text = strings.notMeasured,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    } else {
      Text(
        text = "↓ ${measurement.downloadMbps?.let { String.format("%.1f", it) } ?: "--"} ${strings.mbpsUnit}",
        fontFamily = JetBrainsMonoFamily,
        fontSize = 13.sp,
      )
      Text(
        text = "↑ ${measurement.uploadMbps?.let { String.format("%.1f", it) } ?: "--"} ${strings.mbpsUnit}",
        fontFamily = JetBrainsMonoFamily,
        fontSize = 13.sp,
      )
      Text(
        text = "${measurement.pingMs ?: "--"} ms",
        fontFamily = JetBrainsMonoFamily,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
