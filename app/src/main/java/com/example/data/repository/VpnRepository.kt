package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.BuildConfig
import com.example.data.local.CachedServerEntity
import com.example.data.local.ConnectionHistoryEntity
import com.example.data.local.FavoriteLocationEntity
import com.example.data.local.HistoryDao
import com.example.data.local.LocationDao
import com.example.data.local.RecentLocationEntity
import com.example.data.local.SettingsDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.CityLocation
import com.example.data.model.ConnectionHistoryRecord
import com.example.data.model.ConnectionResult
import com.example.data.model.CountryLocation
import com.example.data.model.InstalledAppInfo
import com.example.data.model.IsoCountryCatalog
import com.example.data.model.SecureDnsProvider
import com.example.data.model.ServerStatus
import com.example.data.model.SplitTunnelMode
import com.example.data.model.VpnProtocol
import com.example.data.model.VpnServer
import com.example.data.model.VpnSettings
import com.example.data.remote.AuthRequestDto
import com.example.data.remote.CreateVpnSessionRequestDto
import com.example.data.remote.CreateVpnSessionResponseDto
import com.example.data.remote.DiagnosticsRequestDto
import com.example.data.remote.NetworkModule
import com.example.data.remote.ServerDto
import com.example.vpn.RealLatencyProbe
import com.example.vpn.SmartServerSelector
import com.example.vpn.WireGuardKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface BackendSyncStatus {
  data object NotConfigured : BackendSyncStatus
  data object Syncing : BackendSyncStatus
  data class Ready(val serverCount: Int, val lastSyncedEpochMs: Long) : BackendSyncStatus
  data class Unreachable(val reasonEn: String, val reasonAr: String) : BackendSyncStatus
}

class VpnRepository(
  private val context: Context,
  private val locationDao: LocationDao,
  private val historyDao: HistoryDao,
  private val settingsDataStore: SettingsDataStore,
  private val networkModule: NetworkModule,
  private val keyManager: WireGuardKeyManager,
  private val latencyProbe: RealLatencyProbe,
  val smartServerSelector: SmartServerSelector,
) {
  private val baseCountries: List<CountryLocation> = IsoCountryCatalog.buildAllCountries()

  private val _backendSyncStatus = MutableStateFlow<BackendSyncStatus>(BackendSyncStatus.NotConfigured)
  val backendSyncStatus: StateFlow<BackendSyncStatus> = _backendSyncStatus.asStateFlow()

  val settingsFlow: Flow<VpnSettings> = settingsDataStore.settingsFlow
  val selectedCountryIsoFlow: Flow<String?> = settingsDataStore.selectedCountryIsoFlow
  val selectedServerIdFlow: Flow<String?> = settingsDataStore.selectedServerIdFlow

  val countriesFlow: Flow<List<CountryLocation>> = combine(
    locationDao.observeFavoriteIsos(),
    locationDao.observeRecentLocations(),
    locationDao.observeCachedServers(),
  ) { favorites, recents, cachedServers ->
    val favSet = favorites.toSet()
    val recentMap = recents.associate { it.countryIso to it.lastConnectedEpochMs }
    val serversByCountry = cachedServers.map { it.toDomain() }.groupBy { it.countryIso.uppercase() }

    baseCountries.map { country ->
      val countryServers = serversByCountry[country.isoCode].orEmpty()
      val cities = countryServers
        .groupBy { it.cityId }
        .map { (cityId, servers) ->
          val sample = servers.first()
          CityLocation(
            id = cityId,
            countryIso = country.isoCode,
            nameEn = sample.cityNameEn,
            nameAr = sample.cityNameAr,
            servers = servers,
          )
        }
        .sortedBy { it.nameEn }

      country.copy(
        cities = cities,
        isFavorite = country.isoCode in favSet,
        lastConnectedEpochMs = recentMap[country.isoCode],
      )
    }
  }

  val allServersFlow: Flow<List<VpnServer>> = locationDao.observeCachedServers()
    .map { list -> list.map { it.toDomain() } }

  val connectionHistoryFlow: Flow<List<ConnectionHistoryRecord>> = historyDao.observeHistory()
    .map { list -> list.map { it.toDomain() } }

  suspend fun refreshLocationsFromBackend(): Result<Int> = withContext(Dispatchers.IO) {
    val settings = settingsFlow.first()
    val api = networkModule.createApiService(settings.backendBaseUrl)
    if (api == null) {
      _backendSyncStatus.value = BackendSyncStatus.NotConfigured
      return@withContext Result.failure(IllegalStateException("Backend URL not configured"))
    }

    _backendSyncStatus.value = BackendSyncStatus.Syncing
    return@withContext try {
      val response = api.getLocations()
      if (!response.isSuccessful || response.body() == null) {
        _backendSyncStatus.value = BackendSyncStatus.Unreachable(
          reasonEn = "Backend returned HTTP ${response.code()}",
          reasonAr = "أعاد الخادم رمز الحالة ${response.code()}",
        )
        return@withContext Result.failure(IllegalStateException("HTTP ${response.code()}"))
      }

      val body = response.body()!!
      val fetchedServers = body.countries.flatMap { country ->
        country.cities.flatMap { city ->
          city.servers.map { it.toEntity() }
        }
      }

      locationDao.clearCachedServers()
      if (fetchedServers.isNotEmpty()) {
        locationDao.upsertServers(fetchedServers)
      }

      _backendSyncStatus.value = BackendSyncStatus.Ready(
        serverCount = fetchedServers.size,
        lastSyncedEpochMs = System.currentTimeMillis(),
      )
      Result.success(fetchedServers.size)
    } catch (t: Throwable) {
      _backendSyncStatus.value = BackendSyncStatus.Unreachable(
        reasonEn = t.localizedMessage ?: "Network error contacting backend",
        reasonAr = "تعذّر الاتصال بالخادم: ${t.localizedMessage ?: "خطأ شبكة"}",
      )
      Result.failure(t)
    }
  }

  suspend fun measureServerLatency(server: VpnServer): VpnServer? = withContext(Dispatchers.IO) {
    val sample = latencyProbe.measureTcpLatency(server.endpointHost, server.endpointPort)
      ?: return@withContext null
    val updated = server.copy(
      measuredLatencyMs = sample.medianLatencyMs,
      jitterMs = sample.jitterMs,
      lastHealthCheckEpochMs = System.currentTimeMillis(),
    )
    locationDao.upsertServers(listOf(updated.toEntity()))
    updated
  }

  suspend fun probeAllAvailableServers() = withContext(Dispatchers.IO) {
    val servers = allServersFlow.first().filter { it.isAvailable }
    val updated = servers.mapNotNull { server ->
      val sample = latencyProbe.measureTcpLatency(server.endpointHost, server.endpointPort)
      sample?.let {
        server.copy(
          measuredLatencyMs = it.medianLatencyMs,
          jitterMs = it.jitterMs,
          lastHealthCheckEpochMs = System.currentTimeMillis(),
        ).toEntity()
      }
    }
    if (updated.isNotEmpty()) {
      locationDao.upsertServers(updated)
    }
  }

  suspend fun loginOrRegister(email: String, password: String, isRegister: Boolean): Result<Unit> =
    withContext(Dispatchers.IO) {
      val settings = settingsFlow.first()
      val api = networkModule.createApiService(settings.backendBaseUrl)
        ?: return@withContext Result.failure(IllegalStateException("Backend URL not configured"))
      return@withContext try {
        val request = AuthRequestDto(email = email.trim(), password = password)
        val response = if (isRegister) api.register(request) else api.login(request)
        if (response.isSuccessful && response.body() != null) {
          val auth = response.body()!!
          networkModule.saveAuthToken(auth.token)
          settingsDataStore.updateAuthState(auth.user.email, true)
          Result.success(Unit)
        } else {
          Result.failure(IllegalStateException("Auth failed (${response.code()})"))
        }
      } catch (t: Throwable) {
        Result.failure(t)
      }
    }

  suspend fun logout() {
    networkModule.clearAuthToken()
    settingsDataStore.updateAuthState("", false)
  }

  suspend fun provisionWireGuardSession(
    server: VpnServer,
    settings: VpnSettings,
  ): Result<CreateVpnSessionResponseDto> = withContext(Dispatchers.IO) {
    val api = networkModule.createApiService(settings.backendBaseUrl)
      ?: return@withContext Result.failure(IllegalStateException("Backend URL not configured"))
    return@withContext try {
      val publicKey = keyManager.getPublicKeyBase64()
      val dns = if (settings.dnsLeakProtectionEnabled) settings.secureDnsProvider.dns1 else null
      val response = api.createVpnSession(
        CreateVpnSessionRequestDto(
          serverId = server.id,
          clientPublicKey = publicKey,
          protocol = settings.protocol.name,
          preferredDns = dns,
        ),
      )
      if (response.isSuccessful && response.body() != null) {
        Result.success(response.body()!!)
      } else {
        Result.failure(IllegalStateException("Session provisioning failed (HTTP ${response.code()})"))
      }
    } catch (t: Throwable) {
      Result.failure(t)
    }
  }

  suspend fun revokeVpnSession(sessionId: String, settings: VpnSettings) {
    withContext(Dispatchers.IO) {
      val api = networkModule.createApiService(settings.backendBaseUrl) ?: return@withContext
      runCatching { api.revokeVpnSession(sessionId) }
    }
  }

  suspend fun sendDiagnostics(
    serverId: String?,
    event: String,
    latencyMs: Int? = null,
    errorCode: String? = null,
  ) {
    withContext(Dispatchers.IO) {
      val settings = settingsFlow.first()
      val api = networkModule.createApiService(settings.backendBaseUrl) ?: return@withContext
      runCatching {
        api.reportDiagnostics(
          DiagnosticsRequestDto(
            serverId = serverId,
            event = event,
            measuredLatencyMs = latencyMs,
            errorCode = errorCode,
            appVersion = BuildConfig.VERSION_NAME,
          ),
        )
      }
    }
  }

  suspend fun selectBestServerForCountry(countryIso: String?): VpnServer? {
    val settings = settingsFlow.first()
    val servers = allServersFlow.first()
    val history = historyDao.getRecentHistorySnapshot().map { it.toDomain() }
    return smartServerSelector.selectBestServer(
      servers = servers,
      countryIso = countryIso,
      preferredProtocol = settings.protocol,
      recentHistory = history,
    )
  }

  suspend fun toggleFavorite(countryIso: String, currentlyFavorite: Boolean) {
    if (currentlyFavorite) {
      locationDao.removeFavorite(countryIso)
    } else {
      locationDao.addFavorite(FavoriteLocationEntity(countryIso = countryIso))
    }
  }

  suspend fun recordConnectionHistory(
    server: VpnServer,
    startedAtEpochMs: Long,
    durationSeconds: Long,
    result: ConnectionResult,
  ) {
    val country = baseCountries.firstOrNull { it.isoCode.equals(server.countryIso, ignoreCase = true) }
    historyDao.insertRecord(
      ConnectionHistoryEntity(
        countryIso = server.countryIso,
        countryNameEn = country?.nameEn ?: server.countryIso,
        countryNameAr = country?.nameAr ?: server.countryIso,
        cityNameEn = server.cityNameEn,
        cityNameAr = server.cityNameAr,
        serverId = server.id,
        protocol = VpnProtocol.WIREGUARD.name,
        startedAtEpochMs = startedAtEpochMs,
        durationSeconds = durationSeconds,
        result = result.name,
      ),
    )
    if (result == ConnectionResult.CONNECTED || result == ConnectionResult.DISCONNECTED) {
      locationDao.upsertRecentLocation(
        RecentLocationEntity(
          countryIso = server.countryIso,
          lastConnectedEpochMs = System.currentTimeMillis(),
        ),
      )
    }
  }

  suspend fun clearConnectionHistory() = historyDao.clearHistory()

  suspend fun setSelection(countryIso: String?, serverId: String?) {
    settingsDataStore.updateSelection(countryIso, serverId)
  }

  suspend fun getInstalledLauncherApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
      addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
      @Suppress("DEPRECATION")
      pm.queryIntentActivities(launcherIntent, 0)
    }
    resolveInfos
      .mapNotNull { info ->
        val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
        if (pkg == context.packageName) return@mapNotNull null
        val label = info.loadLabel(pm)?.toString()?.ifBlank { pkg } ?: pkg
        InstalledAppInfo(packageName = pkg, appName = label)
      }
      .distinctBy { it.packageName }
      .sortedBy { it.appName.lowercase() }
  }

  // Settings writers
  suspend fun setLanguage(language: AppLanguage) = settingsDataStore.updateLanguage(language)
  suspend fun setThemeMode(mode: AppThemeMode) = settingsDataStore.updateThemeMode(mode)
  suspend fun setProtocol(protocol: VpnProtocol) = settingsDataStore.updateProtocol(protocol)
  suspend fun setSmartSelection(enabled: Boolean) = settingsDataStore.updateSmartSelection(enabled)
  suspend fun setKillSwitch(enabled: Boolean) = settingsDataStore.updateKillSwitch(enabled)
  suspend fun setDnsProtection(enabled: Boolean) = settingsDataStore.updateDnsLeakProtection(enabled)
  suspend fun setSecureDns(provider: SecureDnsProvider) = settingsDataStore.updateSecureDnsProvider(provider)
  suspend fun setIpv6Protection(enabled: Boolean) = settingsDataStore.updateIpv6LeakProtection(enabled)
  suspend fun setAutoConnectBoot(enabled: Boolean) = settingsDataStore.updateAutoConnectBoot(enabled)
  suspend fun setAutoConnectWifi(enabled: Boolean) = settingsDataStore.updateAutoConnectWifi(enabled)
  suspend fun setSplitTunnel(mode: SplitTunnelMode, packages: Set<String>) =
    settingsDataStore.updateSplitTunnel(mode, packages)
  suspend fun setNotificationsEnabled(enabled: Boolean) = settingsDataStore.updateNotificationsEnabled(enabled)
  suspend fun setConnectionTimeout(seconds: Int) = settingsDataStore.updateConnectionTimeout(seconds)
  suspend fun setBackendBaseUrl(url: String) = settingsDataStore.updateBackendBaseUrl(url)

  suspend fun autoConnectOnBootOrWifi(): VpnServer? {
    val countryIso = selectedCountryIsoFlow.first()
    return selectBestServerForCountry(countryIso)
  }
}

private fun ServerDto.toEntity(): CachedServerEntity = CachedServerEntity(
  id = id,
  countryIso = countryIso.uppercase(),
  cityId = cityId,
  cityNameEn = cityNameEn,
  cityNameAr = cityNameAr,
  hostname = hostname,
  endpointHost = endpointHost,
  endpointPort = endpointPort,
  publicKey = publicKey,
  supportedProtocolsCsv = supportedProtocols.joinToString(","),
  status = status.uppercase(),
  loadPercent = loadPercent,
  availableCapacityPercent = availableCapacityPercent,
  measuredLatencyMs = latencyMs,
  jitterMs = jitterMs,
  bandwidthCapacityMbps = bandwidthCapacityMbps,
  isAvailable = isAvailable,
  lastHealthCheckEpochMs = null,
)

private fun CachedServerEntity.toDomain(): VpnServer = VpnServer(
  id = id,
  countryIso = countryIso,
  cityId = cityId,
  cityNameEn = cityNameEn,
  cityNameAr = cityNameAr,
  hostname = hostname,
  endpointHost = endpointHost,
  endpointPort = endpointPort,
  publicKey = publicKey,
  supportedProtocols = supportedProtocolsCsv.split(",")
    .mapNotNull { runCatching { VpnProtocol.valueOf(it.trim().uppercase()) }.getOrNull() }
    .ifEmpty { listOf(VpnProtocol.WIREGUARD) },
  status = runCatching { ServerStatus.valueOf(status) }.getOrDefault(ServerStatus.OFFLINE),
  loadPercent = loadPercent,
  availableCapacityPercent = availableCapacityPercent,
  measuredLatencyMs = measuredLatencyMs,
  jitterMs = jitterMs,
  bandwidthCapacityMbps = bandwidthCapacityMbps,
  isAvailable = isAvailable,
  lastHealthCheckEpochMs = lastHealthCheckEpochMs,
)

private fun VpnServer.toEntity(): CachedServerEntity = CachedServerEntity(
  id = id,
  countryIso = countryIso,
  cityId = cityId,
  cityNameEn = cityNameEn,
  cityNameAr = cityNameAr,
  hostname = hostname,
  endpointHost = endpointHost,
  endpointPort = endpointPort,
  publicKey = publicKey,
  supportedProtocolsCsv = supportedProtocols.joinToString(",") { it.name },
  status = status.name,
  loadPercent = loadPercent,
  availableCapacityPercent = availableCapacityPercent,
  measuredLatencyMs = measuredLatencyMs,
  jitterMs = jitterMs,
  bandwidthCapacityMbps = bandwidthCapacityMbps,
  isAvailable = isAvailable,
  lastHealthCheckEpochMs = lastHealthCheckEpochMs,
)
