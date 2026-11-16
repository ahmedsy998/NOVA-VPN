package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IsoCountryCatalog
import com.example.data.model.VpnConnectionState
import com.example.data.model.VpnServer
import com.example.ui.components.MetricTile
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.ServerStatusChip
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaCoral
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald

@Composable
fun ServerDetailsScreen(
  server: VpnServer?,
  connectionState: VpnConnectionState,
  onBack: () -> Unit,
  onMeasureLatency: (VpnServer) -> Unit,
  onConnect: (VpnServer) -> Unit,
  onDisconnect: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 640.dp)
        .align(Alignment.TopCenter)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = strings.cancel,
          )
        }
        Text(
          text = strings.serverDetailsTitle,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
      }

      if (server == null) {
        NovaGlassCard {
          Text(
            text = strings.serverNotAvailable,
            style = MaterialTheme.typography.titleMedium,
          )
        }
        return@Column
      }

      val isCurrentlyConnected = (connectionState as? VpnConnectionState.Connected)?.server?.id == server.id

      NovaGlassCard {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(text = IsoCountryCatalog.flagEmojiForIso(server.countryIso), fontSize = 36.sp)
            Column {
              Text(
                text = if (strings.isArabic) server.cityNameAr else server.cityNameEn,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = server.hostname.ifBlank { server.id },
                fontFamily = JetBrainsMonoFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          ServerStatusChip(status = server.status)
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        MetricTile(
          icon = Icons.Rounded.Speed,
          label = strings.latencyField,
          value = server.measuredLatencyMs?.let { "$it ms" } ?: strings.notMeasured,
          accentColor = NovaCyan,
          modifier = Modifier.weight(1f),
        )
        MetricTile(
          icon = Icons.Rounded.Dns,
          label = strings.loadField,
          value = server.loadPercent?.let { "$it%" } ?: strings.notMeasured,
          accentColor = NovaAmber,
          modifier = Modifier.weight(1f),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        MetricTile(
          icon = Icons.Rounded.NetworkCheck,
          label = strings.capacityField,
          value = server.availableCapacityPercent?.let { "$it%" } ?: strings.notMeasured,
          accentColor = NovaEmerald,
          modifier = Modifier.weight(1f),
        )
        MetricTile(
          icon = Icons.Rounded.VerifiedUser,
          label = strings.qualityField,
          value = strings.qualityLabelText(server.connectionQuality),
          accentColor = NovaCyan,
          modifier = Modifier.weight(1f),
        )
      }

      NovaGlassCard {
        DetailRow(label = strings.countryField, value = server.countryIso)
        DetailRow(label = strings.cityField, value = if (strings.isArabic) server.cityNameAr else server.cityNameEn)
        DetailRow(label = strings.serverIdField, value = server.id, mono = true)
        DetailRow(
          label = strings.protocolsField,
          value = server.supportedProtocols.joinToString(", ") { it.displayName },
        )
        DetailRow(label = strings.availabilityField, value = strings.serverStatusLabel(server.status))
      }

      if (!server.isAvailable) {
        NovaGlassCard(accentBorder = NovaAmber.copy(alpha = 0.5f)) {
          Text(
            text = strings.serverUnavailableHint,
            style = MaterialTheme.typography.bodyMedium,
            color = NovaAmber,
          )
        }
      }

      OutlinedButton(
        onClick = { onMeasureLatency(server) },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("measure_ping_button"),
      ) {
        Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(strings.measurePingButton)
      }

      if (isCurrentlyConnected) {
        Button(
          onClick = onDisconnect,
          colors = ButtonDefaults.buttonColors(containerColor = NovaCoral),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("server_disconnect_button"),
        ) {
          Text(strings.disconnectButton, fontWeight = FontWeight.Bold)
        }
      } else {
        Button(
          onClick = { onConnect(server) },
          enabled = server.isAvailable,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("server_connect_button"),
        ) {
          Text(strings.connectButton, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun DetailRow(
  label: String,
  value: String,
  mono: Boolean = false,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      fontFamily = if (mono) JetBrainsMonoFamily else null,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
