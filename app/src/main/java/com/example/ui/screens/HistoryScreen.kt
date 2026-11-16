package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionHistoryRecord
import com.example.data.model.ConnectionResult
import com.example.data.model.IsoCountryCatalog
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.formatDuration
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaCoral
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
  history: List<ConnectionHistoryRecord>,
  onClearHistory: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  var showClearDialog by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 680.dp)
        .align(Alignment.TopCenter)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = strings.historyTitle,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        if (history.isNotEmpty()) {
          IconButton(
            onClick = { showClearDialog = true },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("clear_history_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteSweep,
              contentDescription = strings.clearHistory,
              tint = NovaCoral,
            )
          }
        }
      }

      NovaGlassCard(accentBorder = NovaEmerald.copy(alpha = 0.35f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Shield,
            contentDescription = null,
            tint = NovaEmerald,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = strings.privacyHistoryNote,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (history.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.History,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(54.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = strings.emptyHistoryTitle,
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = strings.emptyHistorySubtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 24.dp),
        ) {
          items(history, key = { it.id }) { record ->
            HistoryRecordCard(record = record)
          }
        }
      }
    }
  }

  if (showClearDialog) {
    AlertDialog(
      onDismissRequest = { showClearDialog = false },
      title = { Text(strings.clearHistoryConfirmTitle) },
      text = { Text(strings.clearHistoryConfirmBody) },
      confirmButton = {
        Button(
          onClick = {
            showClearDialog = false
            onClearHistory()
          },
          colors = ButtonDefaults.buttonColors(containerColor = NovaCoral),
        ) {
          Text(strings.confirm)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDialog = false }) {
          Text(strings.cancel)
        }
      },
    )
  }
}

@Composable
private fun HistoryRecordCard(record: ConnectionHistoryRecord) {
  val strings = LocalAppStrings.current
  val resultColor = when (record.result) {
    ConnectionResult.CONNECTED, ConnectionResult.DISCONNECTED -> NovaEmerald
    ConnectionResult.PERMISSION_REVOKED -> NovaAmber
    ConnectionResult.FAILED -> NovaCoral
  }
  val dateLabel = remember(record.startedAtEpochMs) {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.startedAtEpochMs))
  }

  NovaGlassCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f),
      ) {
        Text(text = IsoCountryCatalog.flagEmojiForIso(record.countryIso), fontSize = 26.sp)
        Column {
          Text(
            text = if (strings.isArabic) "${record.countryNameAr} • ${record.cityNameAr}" else "${record.countryNameEn} • ${record.cityNameEn}",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${strings.durationLabel}: ${formatDuration(record.durationSeconds)}",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 12.sp,
            color = NovaCyan,
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(50),
        color = resultColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, resultColor.copy(alpha = 0.45f)),
      ) {
        Text(
          text = strings.historyResultLabel(record.result),
          style = MaterialTheme.typography.labelMedium,
          color = resultColor,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }
  }
}
