package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NovaVpnApp
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.ConnectionHistoryRecord
import com.example.data.model.CountryLocation
import com.example.data.model.GeographicalRegion
import com.example.data.model.InstalledAppInfo
import com.example.data.model.PrivacyDashboardState
import com.example.data.model.SecureDnsProvider
import com.example.data.model.SpeedTestPhase
import com.example.data.model.SpeedTestUiState
import com.example.data.model.SplitTunnelMode
import com.example.data.model.VpnConnectionState
import com.example.data.model.VpnErrorCode
import com.example.data.model.VpnProtocol
import com.example.data.model.VpnServer
import com.example.data.model.VpnSettings
import com.example.data.repository.BackendSyncStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NovaViewModel(application: Application) : AndroidViewModel(application) {

  private val container = (application as NovaVpnApp).container
  private val repository = container.repository
  private val tunnelManager = container.tunnelManager
  private val speedTestEngine = container.speedTestEngine

  val settings: StateFlow<VpnSettings> = repository.settingsFlow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = VpnSettings(),
  )

  val countries: StateFlow<List<CountryLocation>> = repository.countriesFlow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList(),
  )

  val allServers: StateFlow<List<VpnServer>> = repository.allServersFlow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList(),
  )

  val connectionState: StateFlow<VpnConnectionState> = tunnelManager.connectionState

  val backendSyncStatus: StateFlow<BackendSyncStatus> = repository.backendSyncStatus

  val connectionHistory: StateFlow<List<ConnectionHistoryRecord>> = repository.connectionHistoryFlow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList(),
  )

  val selectedCountry: StateFlow<CountryLocation?> = combine(
    countries,
    repository.selectedCountryIsoFlow,
  ) { list, iso ->
    if (iso == null) null else list.firstOrNull { it.isoCode.equals(iso, ignoreCase = true) }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val selectedServer: StateFlow<VpnServer?> = combine(
    allServers,
    repository.selectedServerIdFlow,
    selectedCountry,
  ) { servers, serverId, country ->
    servers.firstOrNull { it.id == serverId }
      ?: country?.allServers?.firstOrNull { it.isAvailable }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val privacyDashboard: StateFlow<PrivacyDashboardState> = combine(
    settings,
    connectionState,
  ) { currentSettings, _ ->
    tunnelManager.getPrivacyDashboardState(currentSettings)
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    PrivacyDashboardState(
      tunnelEncrypted = false,
      dnsProtected = true,
      ipv6Protected = true,
      killSwitchConfigured = true,
      systemAlwaysOnActive = false,
      systemLockdownActive = false,
    ),
  )

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedRegion = MutableStateFlow(GeographicalRegion.ALL)
  val selectedRegion: StateFlow<GeographicalRegion> = _selectedRegion.asStateFlow()

  private val _speedTestState = MutableStateFlow(SpeedTestUiState())
  val speedTestState: StateFlow<SpeedTestUiState> = _speedTestState.asStateFlow()

  private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
  val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

  private val _devNoticeDismissed = MutableStateFlow(false)
  val devNoticeDismissed: StateFlow<Boolean> = _devNoticeDismissed.asStateFlow()

  private val _authMessage = MutableStateFlow<String?>(null)
  val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

  private var speedTestJob: Job? = null

  init {
    viewModelScope.launch {
      val currentSettings = repository.settingsFlow.first()
      if (currentSettings.backendBaseUrl.isNotBlank()) {
        repository.refreshLocationsFromBackend()
      }
    }
  }

  fun dismissDevNotice() {
    _devNoticeDismissed.value = true
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun selectRegion(region: GeographicalRegion) {
    _selectedRegion.value = region
  }

  fun refreshLocations() {
    viewModelScope.launch {
      repository.refreshLocationsFromBackend()
      repository.probeAllAvailableServers()
    }
  }

  fun toggleFavorite(country: CountryLocation) {
    viewModelScope.launch {
      repository.toggleFavorite(country.isoCode, country.isFavorite)
    }
  }

  fun selectCountryOnly(country: CountryLocation) {
    viewModelScope.launch {
      val best = repository.selectBestServerForCountry(country.isoCode)
      repository.setSelection(country.isoCode, best?.id)
    }
  }

  fun connectOrDisconnectPrimary() {
    when (connectionState.value) {
      is VpnConnectionState.Connected,
      is VpnConnectionState.Connecting,
      is VpnConnectionState.ProvisioningConfig,
      is VpnConnectionState.VerifyingTunnel,
      is VpnConnectionState.Reconnecting -> {
        tunnelManager.disconnect(userInitiated = true)
      }
      else -> {
        viewModelScope.launch {
          val currentSettings = repository.settingsFlow.first()
          if (currentSettings.backendBaseUrl.isBlank()) {
            connectWithCheck(null)
            return@launch
          }
          val country = selectedCountry.value
          val targetServer = if (country != null) {
            if (currentSettings.smartSelectionEnabled) {
              repository.selectBestServerForCountry(country.isoCode)
            } else {
              selectedServer.value?.takeIf { it.countryIso == country.isoCode }
                ?: repository.selectBestServerForCountry(country.isoCode)
            }
          } else {
            selectedServer.value ?: repository.selectBestServerForCountry(null)
          }
          connectWithCheck(targetServer)
        }
      }
    }
  }

  fun smartConnectGlobal() {
    viewModelScope.launch {
      val best = repository.selectBestServerForCountry(null)
      if (best != null) {
        repository.setSelection(best.countryIso, best.id)
      }
      connectWithCheck(best)
    }
  }

  fun connectToCountrySmart(country: CountryLocation) {
    viewModelScope.launch {
      val best = repository.selectBestServerForCountry(country.isoCode)
      repository.setSelection(country.isoCode, best?.id)
      connectWithCheck(best)
    }
  }

  fun connectToSpecificServer(server: VpnServer) {
    viewModelScope.launch {
      repository.setSelection(server.countryIso, server.id)
      connectWithCheck(server)
    }
  }

  private fun connectWithCheck(server: VpnServer?) {
    if (server == null) {
      val currentSettings = settings.value
      val code = if (currentSettings.backendBaseUrl.isBlank()) {
        VpnErrorCode.BACKEND_NOT_CONFIGURED
      } else {
        VpnErrorCode.NO_SERVERS_AVAILABLE
      }
      // Route through tunnelManager with synthetic error check or clear feedback
      if (code == VpnErrorCode.BACKEND_NOT_CONFIGURED) {
        tunnelManager.connect(
          VpnServer(
            id = "unconfigured",
            countryIso = selectedCountry.value?.isoCode ?: "UN",
            cityId = "none",
            cityNameEn = "Unavailable",
            cityNameAr = "غير متاح",
            hostname = "",
            endpointHost = "",
            endpointPort = 0,
            publicKey = "",
            isAvailable = false,
          ),
        )
      } else {
        tunnelManager.connect(
          VpnServer(
            id = "unavailable",
            countryIso = selectedCountry.value?.isoCode ?: "UN",
            cityId = "none",
            cityNameEn = "Unavailable",
            cityNameAr = "غير متاح",
            hostname = "",
            endpointHost = "",
            endpointPort = 0,
            publicKey = "",
            isAvailable = false,
          ),
        )
      }
      return
    }
    tunnelManager.connect(server)
  }

  fun prepareVpnPermissionIntent(): Intent? = tunnelManager.prepareVpnIntent()
  fun onVpnPermissionGranted() = tunnelManager.onVpnPermissionGranted()
  fun onVpnPermissionDenied() = tunnelManager.onVpnPermissionDenied()
  fun clearVpnError() = tunnelManager.clearError()

  fun measureLatencyForServer(server: VpnServer) {
    viewModelScope.launch {
      repository.measureServerLatency(server)
    }
  }

  fun startSpeedTest() {
    if (_speedTestState.value.phase in setOf(SpeedTestPhase.PING, SpeedTestPhase.DOWNLOAD, SpeedTestPhase.UPLOAD)) {
      return
    }
    speedTestJob?.cancel()
    speedTestJob = viewModelScope.launch {
      val currentSettings = repository.settingsFlow.first()
      val activeConn = connectionState.value as? VpnConnectionState.Connected
      val overVpn = activeConn != null
      val label = activeConn?.server?.let { "${it.countryIso} • ${it.cityNameEn}" }

      speedTestEngine.runSpeedTest(
        baseUrl = currentSettings.backendBaseUrl,
        overVpn = overVpn,
        serverLabel = label,
      ).collect { update ->
        val prev = _speedTestState.value
        _speedTestState.value = prev.copy(
          phase = update.phase,
          progress = update.progress,
          livePingMs = update.pingMs ?: prev.livePingMs,
          liveJitterMs = update.jitterMs ?: prev.liveJitterMs,
          liveDownloadMbps = update.downloadMbps ?: prev.liveDownloadMbps,
          liveUploadMbps = update.uploadMbps ?: prev.liveUploadMbps,
          beforeVpn = if (!overVpn && update.completedMeasurement != null) update.completedMeasurement else prev.beforeVpn,
          afterVpn = if (overVpn && update.completedMeasurement != null) update.completedMeasurement else prev.afterVpn,
          statusMessageEn = update.errorMessageEn,
          statusMessageAr = update.errorMessageAr,
        )
      }
    }
  }

  fun clearHistory() {
    viewModelScope.launch { repository.clearConnectionHistory() }
  }

  fun loadInstalledApps() {
    viewModelScope.launch {
      _installedApps.value = repository.getInstalledLauncherApps()
    }
  }

  // Settings Actions
  fun setLanguage(language: AppLanguage) = viewModelScope.launch { repository.setLanguage(language) }
  fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
  fun setProtocol(protocol: VpnProtocol) = viewModelScope.launch { repository.setProtocol(protocol) }
  fun setSmartSelection(enabled: Boolean) = viewModelScope.launch { repository.setSmartSelection(enabled) }
  fun setKillSwitch(enabled: Boolean) = viewModelScope.launch { repository.setKillSwitch(enabled) }
  fun setDnsProtection(enabled: Boolean) = viewModelScope.launch { repository.setDnsProtection(enabled) }
  fun setSecureDns(provider: SecureDnsProvider) = viewModelScope.launch { repository.setSecureDns(provider) }
  fun setIpv6Protection(enabled: Boolean) = viewModelScope.launch { repository.setIpv6Protection(enabled) }
  fun setAutoConnectBoot(enabled: Boolean) = viewModelScope.launch { repository.setAutoConnectBoot(enabled) }
  fun setAutoConnectWifi(enabled: Boolean) = viewModelScope.launch { repository.setAutoConnectWifi(enabled) }
  fun setSplitTunnel(mode: SplitTunnelMode, packages: Set<String>) =
    viewModelScope.launch { repository.setSplitTunnel(mode, packages) }
  fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { repository.setNotificationsEnabled(enabled) }
  fun setConnectionTimeout(seconds: Int) = viewModelScope.launch { repository.setConnectionTimeout(seconds) }

  fun saveBackendUrlAndSync(url: String) {
    viewModelScope.launch {
      repository.setBackendBaseUrl(url)
      repository.refreshLocationsFromBackend()
    }
  }

  fun authenticate(email: String, password: String, isRegister: Boolean) {
    viewModelScope.launch {
      _authMessage.value = null
      val res = repository.loginOrRegister(email, password, isRegister)
      _authMessage.value = res.fold(
        onSuccess = { "OK" },
        onFailure = { it.localizedMessage ?: "Authentication error" },
      )
    }
  }

  fun signOut() {
    viewModelScope.launch {
      repository.logout()
      _authMessage.value = null
    }
  }
}
