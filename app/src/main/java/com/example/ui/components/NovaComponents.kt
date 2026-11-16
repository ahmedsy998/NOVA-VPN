package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServerStatus
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaCoral
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaElectricBlue
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaViolet
import com.example.ui.theme.SpaceGroteskFamily

enum class ConnectOrbState {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
}

@Composable
fun NovaBrandLogo(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(RoundedCornerShape(11.dp))
        .background(
          Brush.linearGradient(
            colors = listOf(NovaCyan.copy(alpha = 0.22f), NovaViolet.copy(alpha = 0.25f)),
          ),
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.Shield,
        contentDescription = null,
        tint = NovaCyan,
        modifier = Modifier.size(21.dp),
      )
    }
    Column {
      Text(
        text = "NOVA VPN",
        fontFamily = SpaceGroteskFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }
  }
}

@Composable
fun NovaGlassCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  accentBorder: Color? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = RoundedCornerShape(22.dp)
  val borderColor = accentBorder ?: MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    shape = shape,
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, borderColor),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      content = content,
    )
  }
}

@Composable
fun AnimatedConnectOrb(
  orbState: ConnectOrbState,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val infiniteTransition = rememberInfiniteTransition(label = "nova_orb_anim")

  val sweepRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1900, easing = LinearEasing),
    ),
    label = "orb_rotation",
  )

  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.06f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "orb_pulse",
  )

  val primaryRingColor by animateColorAsState(
    targetValue = when (orbState) {
      ConnectOrbState.DISCONNECTED -> NovaElectricBlue
      ConnectOrbState.CONNECTING -> NovaAmber
      ConnectOrbState.CONNECTED -> NovaCyan
    },
    animationSpec = tween(350),
    label = "orb_color",
  )

  val secondaryRingColor by animateColorAsState(
    targetValue = when (orbState) {
      ConnectOrbState.DISCONNECTED -> NovaViolet
      ConnectOrbState.CONNECTING -> NovaCyan
      ConnectOrbState.CONNECTED -> NovaEmerald
    },
    animationSpec = tween(350),
    label = "orb_secondary_color",
  )

  Box(
    modifier = modifier
      .size(216.dp)
      .testTag("connect_orb_button"),
    contentAlignment = Alignment.Center,
  ) {
    // Outer atmospheric glow & animated orbital arc
    Canvas(modifier = Modifier.size(216.dp)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val outerRadius = (size.minDimension / 2f) * (if (orbState == ConnectOrbState.CONNECTED) pulseScale * 0.94f else 0.90f)

      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            primaryRingColor.copy(alpha = if (orbState == ConnectOrbState.CONNECTED) 0.26f else 0.14f),
            Color.Transparent,
          ),
          center = center,
          radius = outerRadius + 18f,
        ),
        radius = outerRadius + 18f,
        center = center,
      )

      drawCircle(
        color = primaryRingColor.copy(alpha = 0.24f),
        radius = outerRadius - 6f,
        center = center,
        style = Stroke(width = 2.dp.toPx()),
      )

      if (orbState == ConnectOrbState.CONNECTING) {
        rotate(degrees = sweepRotation, pivot = center) {
          drawArc(
            brush = Brush.sweepGradient(
              colors = listOf(Color.Transparent, primaryRingColor, secondaryRingColor, Color.Transparent),
              center = center,
            ),
            startAngle = 0f,
            sweepAngle = 240f,
            useCenter = false,
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
          )
        }
      } else if (orbState == ConnectOrbState.CONNECTED) {
        drawArc(
          brush = Brush.sweepGradient(
            colors = listOf(NovaCyan, NovaEmerald, NovaElectricBlue, NovaCyan),
            center = center,
          ),
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
      }
    }

    // Main circular button core
    Surface(
      modifier = Modifier
        .size(162.dp)
        .shadow(
          elevation = if (orbState == ConnectOrbState.CONNECTED) 20.dp else 10.dp,
          shape = CircleShape,
          ambientColor = primaryRingColor,
          spotColor = primaryRingColor,
        )
        .clip(CircleShape)
        .clickable(onClick = onClick),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(
        width = 1.5.dp,
        brush = Brush.linearGradient(listOf(primaryRingColor, secondaryRingColor)),
      ),
    ) {
      Box(
        modifier = Modifier
          .background(
            Brush.radialGradient(
              colors = listOf(
                primaryRingColor.copy(alpha = 0.22f),
                MaterialTheme.colorScheme.surface,
              ),
            ),
          ),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(12.dp),
        ) {
          Icon(
            imageVector = when (orbState) {
              ConnectOrbState.CONNECTED -> Icons.Rounded.VerifiedUser
              ConnectOrbState.CONNECTING -> Icons.Rounded.Bolt
              ConnectOrbState.DISCONNECTED -> Icons.Rounded.PowerSettingsNew
            },
            contentDescription = label,
            tint = primaryRingColor,
            modifier = Modifier.size(42.dp),
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = when (orbState) {
              ConnectOrbState.CONNECTED -> strings.tapToDisconnect
              ConnectOrbState.CONNECTING -> strings.statusConnecting
              ConnectOrbState.DISCONNECTED -> strings.tapToConnect
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun MetricTile(
  icon: ImageVector,
  label: String,
  value: String,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = value,
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ServerStatusChip(status: ServerStatus) {
  val strings = LocalAppStrings.current
  val color = when (status) {
    ServerStatus.ONLINE -> NovaEmerald
    ServerStatus.BUSY -> NovaAmber
    ServerStatus.MAINTENANCE -> NovaViolet
    ServerStatus.OFFLINE -> NovaCoral
  }
  Surface(
    shape = RoundedCornerShape(50),
    color = color.copy(alpha = 0.14f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Box(
        modifier = Modifier
          .size(7.dp)
          .clip(CircleShape)
          .background(color),
      )
      Text(
        text = strings.serverStatusLabel(status),
        style = MaterialTheme.typography.labelMedium,
        color = color,
      )
    }
  }
}

@Composable
fun VpnPermissionExplanationDialog(
  onAccept: () -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Rounded.Lock,
        contentDescription = null,
        tint = NovaCyan,
        modifier = Modifier.size(32.dp),
      )
    },
    title = {
      Text(
        text = strings.vpnPermissionTitle,
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Text(
        text = strings.vpnPermissionBody,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    confirmButton = {
      Button(
        onClick = onAccept,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.testTag("grant_vpn_permission_button"),
      ) {
        Text(strings.vpnPermissionAccept)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.vpnPermissionCancel)
      }
    },
  )
}

fun formatDuration(seconds: Long): String {
  val h = seconds / 3600
  val m = (seconds % 3600) / 60
  val s = seconds % 60
  return String.format("%02d:%02d:%02d", h, m, s)
}

fun formatByteRate(bytesPerSec: Long): String {
  return when {
    bytesPerSec >= 1_048_576L -> String.format("%.1f MB/s", bytesPerSec / 1_048_576.0)
    bytesPerSec >= 1024L -> String.format("%.0f KB/s", bytesPerSec / 1024.0)
    else -> "$bytesPerSec B/s"
  }
}
