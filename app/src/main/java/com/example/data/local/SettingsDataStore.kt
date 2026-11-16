package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.SecureDnsProvider
import com.example.data.model.SplitTunnelMode
import com.example.data.model.VpnProtocol
import com.example.data.model.VpnSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.novaDataStore by preferencesDataStore(name = "nova_vpn_settings")

class SettingsDataStore(private val context: Context) {

  private object Keys {
    val LANGUAGE = stringPreferencesKey("language")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val PROTOCOL = stringPreferencesKey("protocol")
    val SMART_SELECTION = booleanPreferencesKey("smart_selection")
    val KILL_SWITCH = booleanPreferencesKey("kill_switch")
    val DNS_LEAK_PROTECTION = booleanPreferencesKey("dns_leak_protection")
    val SECURE_DNS_PROVIDER = stringPreferencesKey("secure_dns_provider")
    val IPV6_LEAK_PROTECTION = booleanPreferencesKey("ipv6_leak_protection")
    val AUTO_CONNECT_BOOT = booleanPreferencesKey("auto_connect_boot")
    val AUTO_CONNECT_WIFI = booleanPreferencesKey("auto_connect_wifi")
    val SPLIT_TUNNEL_MODE = stringPreferencesKey("split_tunnel_mode")
    val SPLIT_TUNNEL_PACKAGES = stringSetPreferencesKey("split_tunnel_packages")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val CONNECTION_TIMEOUT = intPreferencesKey("connection_timeout_sec")
    val BACKEND_BASE_URL = stringPreferencesKey("backend_base_url")
    val AUTH_EMAIL = stringPreferencesKey("auth_email")
    val IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")
    val SELECTED_COUNTRY_ISO = stringPreferencesKey("selected_country_iso")
    val SELECTED_SERVER_ID = stringPreferencesKey("selected_server_id")
  }

  private val defaultBaseUrl: String
    get() = runCatching {
      BuildConfig::class.java.getField("NOVA_API_BASE_URL").get(null) as? String
    }.getOrNull().orEmpty()

  val settingsFlow: Flow<VpnSettings> = context.novaDataStore.data.map { prefs ->
    VpnSettings(
      language = prefs[Keys.LANGUAGE]?.let { code ->
        AppLanguage.entries.firstOrNull { it.code == code }
      } ?: AppLanguage.ARABIC,
      themeMode = prefs[Keys.THEME_MODE]?.let { name ->
        runCatching { AppThemeMode.valueOf(name) }.getOrNull()
      } ?: AppThemeMode.DARK,
      protocol = prefs[Keys.PROTOCOL]?.let { name ->
        runCatching { VpnProtocol.valueOf(name) }.getOrNull()
      } ?: VpnProtocol.WIREGUARD,
      smartSelectionEnabled = prefs[Keys.SMART_SELECTION] ?: true,
      killSwitchEnabled = prefs[Keys.KILL_SWITCH] ?: true,
      dnsLeakProtectionEnabled = prefs[Keys.DNS_LEAK_PROTECTION] ?: true,
      secureDnsProvider = prefs[Keys.SECURE_DNS_PROVIDER]?.let { name ->
        runCatching { SecureDnsProvider.valueOf(name) }.getOrNull()
      } ?: SecureDnsProvider.CLOUDFLARE,
      ipv6LeakProtectionEnabled = prefs[Keys.IPV6_LEAK_PROTECTION] ?: true,
      autoConnectOnBoot = prefs[Keys.AUTO_CONNECT_BOOT] ?: false,
      autoConnectOnWifi = prefs[Keys.AUTO_CONNECT_WIFI] ?: false,
      splitTunnelMode = prefs[Keys.SPLIT_TUNNEL_MODE]?.let { name ->
        runCatching { SplitTunnelMode.valueOf(name) }.getOrNull()
      } ?: SplitTunnelMode.DISABLED,
      splitTunnelPackages = prefs[Keys.SPLIT_TUNNEL_PACKAGES] ?: emptySet(),
      notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
      connectionTimeoutSeconds = prefs[Keys.CONNECTION_TIMEOUT] ?: 15,
      backendBaseUrl = prefs[Keys.BACKEND_BASE_URL] ?: defaultBaseUrl,
      authEmail = prefs[Keys.AUTH_EMAIL] ?: "",
      isAuthenticated = prefs[Keys.IS_AUTHENTICATED] ?: false,
    )
  }

  val selectedCountryIsoFlow: Flow<String?> = context.novaDataStore.data.map { it[Keys.SELECTED_COUNTRY_ISO] }
  val selectedServerIdFlow: Flow<String?> = context.novaDataStore.data.map { it[Keys.SELECTED_SERVER_ID] }

  suspend fun updateLanguage(language: AppLanguage) {
    context.novaDataStore.edit { it[Keys.LANGUAGE] = language.code }
  }

  suspend fun updateThemeMode(themeMode: AppThemeMode) {
    context.novaDataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
  }

  suspend fun updateProtocol(protocol: VpnProtocol) {
    context.novaDataStore.edit { it[Keys.PROTOCOL] = protocol.name }
  }

  suspend fun updateSmartSelection(enabled: Boolean) {
    context.novaDataStore.edit { it[Keys.SMART_SELECTION] = enabled }
  }

  suspend fun updateKillSwitch(enabled: Boolean) {
    context.novaDataStore.edit { it[Keys.KILL_SWITCH] = enabled }
  }

  suspend fun updateDnsLeakProtection(enabled: Boolean) {
    context.novaDataStore.edit { it[Keys.DNS_LEAK_PROTECTION] = enabled }
  }

  suspend fun updateSecureDnsProvider(provider: SecureDnsProvider) {
    context.novaDataStore.edit { it[Keys.SECURE_DNS_PROVIDER] = provider.name }
  }

  suspend fun updateIpv6LeakProtection(enabled: Boolean) {
    context.novaDataStore.edit { it[Keys.IPV6_LEAK_PROTECTION] = enabled }
  }

  suspend fun updateAutoConnectBoot(enabled: Boolean) {
    context.novaDataStore.edit { it[Keys.AUTO_CONNECT_BOOT] = enabled }
  }

  suspend fun updateAutoConnectWifi(enabled: Boolean) {
    context.novaDataStore.edit { it[Keys.AUTO_CONNECT_WIFI] = enabled }
  }

  suspend fun updateSplitTunnel(mode: SplitTunnelMode, packages: Set<String>) {
    context.novaDataStore.edit {
      it[Keys.SPLIT_TUNNEL_MODE] = mode.name
      it[Keys.SPLIT_TUNNEL_PACKAGES] = packages
    }
  }

  suspend fun updateNotificationsEnabled(enabled: Boolean) {
    context.novaDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
  }

  suspend fun updateConnectionTimeout(seconds: Int) {
    context.novaDataStore.edit { it[Keys.CONNECTION_TIMEOUT] = seconds.coerceIn(5, 60) }
  }

  suspend fun updateBackendBaseUrl(url: String) {
    context.novaDataStore.edit { it[Keys.BACKEND_BASE_URL] = url.trim().trimEnd('/') }
  }

  suspend fun updateAuthState(email: String, authenticated: Boolean) {
    context.novaDataStore.edit {
      it[Keys.AUTH_EMAIL] = email
      it[Keys.IS_AUTHENTICATED] = authenticated
    }
  }

  suspend fun updateSelection(countryIso: String?, serverId: String?) {
    context.novaDataStore.edit { prefs ->
      if (countryIso != null) prefs[Keys.SELECTED_COUNTRY_ISO] = countryIso else prefs.remove(Keys.SELECTED_COUNTRY_ISO)
      if (serverId != null) prefs[Keys.SELECTED_SERVER_ID] = serverId else prefs.remove(Keys.SELECTED_SERVER_ID)
    }
  }
}
