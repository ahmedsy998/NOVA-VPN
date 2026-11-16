package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.InstalledAppInfo
import com.example.data.model.SecureDnsProvider
import com.example.data.model.SplitTunnelMode
import com.example.data.model.VpnProtocol
import com.example.data.model.VpnSettings
import com.example.ui.components.NovaGlassCard
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
  settings: VpnSettings,
  installedApps: List<InstalledAppInfo>,
  authMessage: String?,
  onLoadInstalledApps: () -> Unit,
  onSetLanguage: (AppLanguage) -> Unit,
  onSetThemeMode: (AppThemeMode) -> Unit,
  onSetProtocol: (VpnProtocol) -> Unit,
  onSetSmartSelection: (Boolean) -> Unit,
  onSetKillSwitch: (Boolean) -> Unit,
  onSetDnsProtection: (Boolean) -> Unit,
  onSetSecureDns: (SecureDnsProvider) -> Unit,
  onSetIpv6Protection: (Boolean) -> Unit,
  onSetAutoConnectBoot: (Boolean) -> Unit,
  onSetAutoConnectWifi: (Boolean) -> Unit,
  onSetSplitTunnel: (SplitTunnelMode, Set<String>) -> Unit,
  onSetNotifications: (Boolean) -> Unit,
  onSetConnectionTimeout: (Int) -> Unit,
  onSaveBackendUrl: (String) -> Unit,
  onAuthenticate: (String, String, Boolean) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current

  var backendUrlInput by remember(settings.backendBaseUrl) { mutableStateOf(settings.backendBaseUrl) }
  var emailInput by remember { mutableStateOf(settings.authEmail) }
  var passwordInput by remember { mutableStateOf("") }
  var showSplitTunnelDialog by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 680.dp)
        .align(Alignment.TopCenter)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = strings.settingsTitle,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )

      // 1. Appearance & Language
      SectionTitle(strings.sectionAppearance)
      NovaGlassCard {
        Text(text = strings.languageLabel, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = settings.language == AppLanguage.ARABIC,
            onClick = { onSetLanguage(AppLanguage.ARABIC) },
            label = { Text("العربية (RTL)") },
            modifier = Modifier.testTag("lang_arabic_chip"),
          )
          FilterChip(
            selected = settings.language == AppLanguage.ENGLISH,
            onClick = { onSetLanguage(AppLanguage.ENGLISH) },
            label = { Text("English (LTR)") },
            modifier = Modifier.testTag("lang_english_chip"),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = strings.themeLabel, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = settings.themeMode == AppThemeMode.DARK,
            onClick = { onSetThemeMode(AppThemeMode.DARK) },
            label = { Text(strings.themeDark) },
          )
          FilterChip(
            selected = settings.themeMode == AppThemeMode.LIGHT,
            onClick = { onSetThemeMode(AppThemeMode.LIGHT) },
            label = { Text(strings.themeLight) },
          )
          FilterChip(
            selected = settings.themeMode == AppThemeMode.SYSTEM,
            onClick = { onSetThemeMode(AppThemeMode.SYSTEM) },
            label = { Text(strings.themeSystem) },
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingSwitchRow(
          title = strings.notificationsLabel,
          checked = settings.notificationsEnabled,
          onCheckedChange = onSetNotifications,
        )
      }

      // 2. VPN Protocol & Smart Engine
      SectionTitle(strings.sectionConnection)
      NovaGlassCard {
        Text(text = strings.vpnProtocolLabel, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          VpnProtocol.entries.forEach { protocol ->
            FilterChip(
              selected = settings.protocol == protocol,
              onClick = { onSetProtocol(protocol) },
              label = { Text(protocol.displayName) },
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SettingSwitchRow(
          title = strings.smartSelectionLabel,
          subtitle = strings.smartSelectionDesc,
          checked = settings.smartSelectionEnabled,
          onCheckedChange = onSetSmartSelection,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "${strings.connectionTimeoutLabel}: ${settings.connectionTimeoutSeconds}s",
          style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
          value = settings.connectionTimeoutSeconds.toFloat(),
          onValueChange = { onSetConnectionTimeout(it.roundToInt()) },
          valueRange = 5f..60f,
          steps = 10,
        )
      }

      // 3. Advanced Privacy & Security
      SectionTitle(strings.sectionPrivacy)
      NovaGlassCard {
        SettingSwitchRow(
          title = strings.killSwitchLabel,
          subtitle = strings.killSwitchDesc,
          checked = settings.killSwitchEnabled,
          onCheckedChange = onSetKillSwitch,
        )
        OutlinedButton(
          onClick = {
            runCatching {
              context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(strings.openSystemVpnSettings)
        }
        Spacer(modifier = Modifier.height(8.dp))
        SettingSwitchRow(
          title = strings.dnsProtectionLabel,
          subtitle = strings.dnsProtectionDesc,
          checked = settings.dnsLeakProtectionEnabled,
          onCheckedChange = onSetDnsProtection,
        )
        if (settings.dnsLeakProtectionEnabled) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = strings.secureDnsProviderLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SecureDnsProvider.entries.chunked(2).forEach { rowItems ->
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { provider ->
                  FilterChip(
                    selected = settings.secureDnsProvider == provider,
                    onClick = { onSetSecureDns(provider) },
                    label = { Text(provider.label) },
                  )
                }
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SettingSwitchRow(
          title = strings.ipv6ProtectionLabel,
          subtitle = strings.ipv6ProtectionDesc,
          checked = settings.ipv6LeakProtectionEnabled,
          onCheckedChange = onSetIpv6Protection,
        )
        SettingSwitchRow(
          title = strings.autoConnectBootLabel,
          checked = settings.autoConnectOnBoot,
          onCheckedChange = onSetAutoConnectBoot,
        )
        SettingSwitchRow(
          title = strings.autoConnectWifiLabel,
          checked = settings.autoConnectOnWifi,
          onCheckedChange = onSetAutoConnectWifi,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = strings.splitTunnelingLabel, style = MaterialTheme.typography.titleMedium)
        Text(
          text = strings.splitTunnelingDesc,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SplitTunnelMode.entries.forEach { mode ->
            FilterChip(
              selected = settings.splitTunnelMode == mode,
              onClick = { onSetSplitTunnel(mode, settings.splitTunnelPackages) },
              label = { Text(mode.name.lowercase().replace('_', ' ')) },
            )
          }
        }
        if (settings.splitTunnelMode != SplitTunnelMode.DISABLED) {
          OutlinedButton(
            onClick = {
              onLoadInstalledApps()
              showSplitTunnelDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("${strings.configureApps} (${settings.splitTunnelPackages.size})")
          }
        }
      }

      // 4. Backend Infrastructure & Auth
      SectionTitle(strings.sectionBackend)
      NovaGlassCard {
        OutlinedTextField(
          value = backendUrlInput,
          onValueChange = { backendUrlInput = it },
          label = { Text(strings.backendUrlLabel) },
          placeholder = { Text(strings.backendUrlPlaceholder) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("backend_url_input"),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = { onSaveBackendUrl(backendUrlInput) },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("save_backend_url_button"),
        ) {
          Text(strings.saveBackendUrl)
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(text = strings.accountSessionLabel, style = MaterialTheme.typography.titleMedium)
        if (settings.isAuthenticated) {
          Text(
            text = settings.authEmail,
            style = MaterialTheme.typography.bodyLarge,
            color = NovaEmerald,
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text(strings.signOutButton)
          }
        } else {
          OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          if (!authMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = authMessage, style = MaterialTheme.typography.bodyMedium, color = NovaCyan)
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { onAuthenticate(emailInput, passwordInput, false) },
              modifier = Modifier.weight(1f),
            ) {
              Text(strings.signInButton)
            }
            OutlinedButton(
              onClick = { onAuthenticate(emailInput, passwordInput, true) },
              modifier = Modifier.weight(1f),
            ) {
              Text(strings.registerButton)
            }
          }
        }
      }

      // 5. Privacy & About
      SectionTitle(strings.sectionAbout)
      NovaGlassCard {
        Text(text = strings.privacyPolicyTitle, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = strings.privacyPolicyBody,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "${strings.aboutVersionLabel}: ${BuildConfig.VERSION_NAME}",
          style = MaterialTheme.typography.labelLarge,
          color = NovaCyan,
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  if (showSplitTunnelDialog) {
    AlertDialog(
      onDismissRequest = { showSplitTunnelDialog = false },
      title = { Text(strings.splitTunnelingLabel) },
      text = {
        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
          items(installedApps, key = { it.packageName }) { appInfo ->
            val selected = appInfo.packageName in settings.splitTunnelPackages
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  val next = settings.splitTunnelPackages.toMutableSet()
                  if (selected) next.remove(appInfo.packageName) else next.add(appInfo.packageName)
                  onSetSplitTunnel(settings.splitTunnelMode, next)
                }
                .padding(vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(
                checked = selected,
                onCheckedChange = null,
              )
              Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(text = appInfo.appName, style = MaterialTheme.typography.bodyLarge)
                Text(
                  text = appInfo.packageName,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSplitTunnelDialog = false }) {
          Text(strings.confirm)
        }
      },
    )
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
  )
}

@Composable
private fun SettingSwitchRow(
  title: String,
  subtitle: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}
