package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CountryLocation
import com.example.data.model.PrivacyDashboardState
import com.example.data.model.VpnConnectionState
import com.example.data.model.VpnErrorCode
import com.example.data.model.VpnServer
import com.example.data.model.VpnSettings
import com.example.ui.components.AnimatedConnectOrb
import com.example.ui.components.ConnectOrbState
import com.example.ui.components.MetricTile
import com.example.ui.components.NovaBrandLogo
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.formatByteRate
import com.example.ui.components.formatDuration
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaCoral
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaElectricBlue
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaViolet
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
  connectionState: VpnConnectionState,
  selectedCountry: CountryLocation?,
  selectedServer: VpnServer?,
  settings: VpnSettings,
  privacyDashboard: PrivacyDashboardState,
  devNoticeDismissed: Boolean,
  onConnectToggle: () -> Unit,
  onSmartLocationClick: () -> Unit,
  onSelectCountryClick: () -> Unit,
  onOpenSettingsClick: () -> Unit,
  onDismissDevNotice: () -> Unit,
  onClearError: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val scrollState = rememberScrollState()

  var elapsedSeconds by remember { mutableLongStateOf(0L) }
  LaunchedEffect(connectionState) {
    if (connectionState is VpnConnectionState.Connected) {
      while (true) {
        elapsedSeconds = ((System.currentTimeMillis() - connectionState.connectedAtEpochMs) / 1000L)
          .coerceAtLeast(0L)
        delay(1000L)
      }
    } else {
      elapsedSeconds = 0L
    }
  }

  val orbState = when (connectionState) {
    is VpnConnectionState.Connected -> ConnectOrbState.CONNECTED
    is VpnConnectionState.Connecting,
    is VpnConnectionState.ProvisioningConfig,
    is VpnConnectionState.VerifyingTunnel,
    is VpnConnectionState.Reconnecting,
    is VpnConnectionState.PreparingPermission,
    is VpnConnectionState.Disconnecting -> ConnectOrbState.CONNECTING
    else -> ConnectOrbState.DISCONNECTED
  }

  val statusText = when (connectionState) {
    is VpnConnectionState.Disconnected -> strings.statusDisconnected
    is VpnConnectionState.PreparingPermission -> strings.statusPreparingPermission
    is VpnConnectionState.ProvisioningConfig -> strings.statusProvisioning
    is VpnConnectionState.Connecting -> strings.statusConnecting
    is VpnConnectionState.VerifyingTunnel -> strings.statusVerifying
    is VpnConnectionState.Connected -> strings.statusConnected
    is VpnConnectionState.Reconnecting -> strings.statusReconnecting
    is VpnConnectionState.Disconnecting -> strings.statusDisconnecting
    is VpnConnectionState.Error -> strings.statusError
  }

  val statusColor = when (orbState) {
    ConnectOrbState.CONNECTED -> NovaEmerald
    ConnectOrbState.CONNECTING -> NovaAmber
    ConnectOrbState.DISCONNECTED -> if (connectionState is VpnConnectionState.Error) NovaCoral else MaterialTheme.colorScheme.onSurfaceVariant
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.background,
          ),
        ),
      ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 640.dp)
        .align(Alignment.TopCenter)
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Header bar with NOVA VPN Logo & Protocol Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        NovaBrandLogo()
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.VpnKey,
              contentDescription = null,
              tint = NovaCyan,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = settings.protocol.displayName,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      // Honest Development / Unconfigured Backend Banner
      AnimatedVisibility(visible = settings.backendBaseUrl.isBlank() && !devNoticeDismissed) {
        NovaGlassCard(
          accentBorder = NovaAmber.copy(alpha = 0.5f),
        ) {
          Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Info,
              contentDescription = null,
              tint = NovaAmber,
              modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = strings.devModeTitle,
                style = MaterialTheme.typography.titleMedium,
                color = NovaAmber,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = strings.devModeDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                  onClick = onOpenSettingsClick,
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                  Text(strings.configureBackendCta)
                }
                TextButton(onClick = onDismissDevNotice) {
                  Text(strings.dismiss)
                }
              }
            }
          }
        }
      }

      // Prominent Connection Status Pill
      Surface(
        shape = RoundedCornerShape(50),
        color = statusColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
        modifier = Modifier.testTag("connection_status_badge"),
      ) {
        Text(
          text = statusText,
          style = MaterialTheme.typography.titleMedium,
          color = statusColor,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
      }

      // Signature Animated Connect Button
      AnimatedConnectOrb(
        orbState = orbState,
        label = statusText,
        onClick = onConnectToggle,
      )

      // Error Alert Card (when applicable)
      if (connectionState is VpnConnectionState.Error) {
        NovaGlassCard(accentBorder = NovaCoral.copy(alpha = 0.6f)) {
          Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.ErrorOutline,
              contentDescription = null,
              tint = NovaCoral,
              modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (strings.isArabic) connectionState.messageAr else connectionState.messageEn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (connectionState.errorCode == VpnErrorCode.BACKEND_NOT_CONFIGURED) {
                  Button(onClick = onOpenSettingsClick) {
                    Text(strings.configureBackendCta)
                  }
                }
                TextButton(onClick = onClearError) {
                  Text(strings.dismiss)
                }
              }
            }
          }
        }
      }

      // Active Location & Quick Access Row
      val activeServer = (connectionState as? VpnConnectionState.Connected)?.server ?: selectedServer
      val displayFlag = selectedCountry?.flagEmoji ?: "🌐"
      val displayCountry = selectedCountry?.let {
        if (strings.isArabic) it.nameAr else it.nameEn
      } ?: strings.selectCountryPrompt
      val displayCity = activeServer?.let {
        if (strings.isArabic) it.cityNameAr else it.cityNameEn
      } ?: strings.serverNotAvailable

      NovaGlassCard(onClick = onSelectCountryClick) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f),
          ) {
            Text(
              text = displayFlag,
              fontSize = 34.sp,
            )
            Column {
              Text(
                text = displayCountry,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = displayCity,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          Icon(
            imageVector = Icons.Rounded.Public,
            contentDescription = strings.countrySelection,
            tint = MaterialTheme.colorScheme.primary,
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FilledTonalButton(
            onClick = onSmartLocationClick,
            modifier = Modifier
              .weight(1f)
              .testTag("smart_location_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
              text = strings.smartLocation,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          OutlinedButton(
            onClick = onSelectCountryClick,
            modifier = Modifier
              .weight(1f)
              .testTag("select_country_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Public,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
              text = strings.countrySelection,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      // Live Verified Tunnel Metrics Grid
      val connectedState = connectionState as? VpnConnectionState.Connected
      val verifiedIpText = connectedState?.verifiedTunnelIp ?: strings.notVerifiedYet
      val durationText = if (connectedState != null) formatDuration(elapsedSeconds) else "00:00:00"
      val downRateText = if (connectedState != null) formatByteRate(connectedState.rxBytesPerSec) else "--"
      val upRateText = if (connectedState != null) formatByteRate(connectedState.txBytesPerSec) else "--"
      val latencyText = (connectedState?.measuredLatencyMs ?: activeServer?.measuredLatencyMs)
        ?.let { "$it ms" } ?: "--"
      val loadText = activeServer?.loadPercent?.let { "$it%" } ?: "--"
      val qualityText = activeServer?.let { strings.qualityLabelText(it.connectionQuality) }
        ?: strings.notMeasured

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricTile(
            icon = Icons.Rounded.VpnKey,
            label = strings.vpnIpLabel,
            value = verifiedIpText,
            accentColor = if (connectedState != null) NovaEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            icon = Icons.Rounded.Timer,
            label = strings.durationLabel,
            value = durationText,
            accentColor = NovaCyan,
            modifier = Modifier.weight(1f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          MetricTile(
            icon = Icons.Rounded.ArrowDownward,
            label = strings.downloadLabel,
            value = downRateText,
            accentColor = NovaCyan,
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            icon = Icons.Rounded.ArrowUpward,
            label = strings.uploadLabel,
            value = upRateText,
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
            value = latencyText,
            accentColor = NovaElectricBlue,
            modifier = Modifier.weight(1f),
          )
          MetricTile(
            icon = Icons.Rounded.Dns,
            label = strings.serverLoadLabel,
            value = "$loadText • $qualityText",
            accentColor = NovaEmerald,
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Active Privacy Protections Dashboard Strip
      NovaGlassCard {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.GppGood,
            contentDescription = null,
            tint = NovaEmerald,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = strings.privacyActiveTitle,
            style = MaterialTheme.typography.titleMedium,
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PrivacyBadge(
            label = strings.dnsProtectedBadge,
            active = privacyDashboard.dnsProtected,
            modifier = Modifier.weight(1f),
          )
          PrivacyBadge(
            label = strings.ipv6ProtectedBadge,
            active = privacyDashboard.ipv6Protected,
            modifier = Modifier.weight(1f),
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          PrivacyBadge(
            label = strings.killSwitchBadge,
            active = privacyDashboard.killSwitchConfigured,
            modifier = Modifier.weight(1f),
          )
          PrivacyBadge(
            label = strings.noLogsBadge,
            active = privacyDashboard.noLogsVerified,
            modifier = Modifier.weight(1f),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun PrivacyBadge(
  label: String,
  active: Boolean,
  modifier: Modifier = Modifier,
) {
  val color = if (active) NovaEmerald else Color.Gray
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = color.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Security,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(15.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
