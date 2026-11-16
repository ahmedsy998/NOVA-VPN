package com.example

import android.app.Application
import android.content.Context
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.NovaDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.remote.NetworkModule
import com.example.data.repository.VpnRepository
import com.example.vpn.NovaTunnelManager
import com.example.vpn.RealLatencyProbe
import com.example.vpn.SmartServerSelector
import com.example.vpn.SpeedTestEngine
import com.example.vpn.WireGuardKeyManager

class AppContainer(context: Context) {
  private val appContext = context.applicationContext

  val database: NovaDatabase by lazy { NovaDatabase.getInstance(appContext) }
  val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(appContext) }
  val cryptoManager: KeystoreCryptoManager by lazy { KeystoreCryptoManager(appContext) }
  val networkModule: NetworkModule by lazy { NetworkModule(cryptoManager) }
  val keyManager: WireGuardKeyManager by lazy { WireGuardKeyManager(cryptoManager) }
  val latencyProbe: RealLatencyProbe by lazy { RealLatencyProbe() }
  val smartServerSelector: SmartServerSelector by lazy { SmartServerSelector() }
  val speedTestEngine: SpeedTestEngine by lazy { SpeedTestEngine(networkModule.httpClient) }

  val repository: VpnRepository by lazy {
    VpnRepository(
      context = appContext,
      locationDao = database.locationDao(),
      historyDao = database.historyDao(),
      settingsDataStore = settingsDataStore,
      networkModule = networkModule,
      keyManager = keyManager,
      latencyProbe = latencyProbe,
      smartServerSelector = smartServerSelector,
    )
  }

  val tunnelManager: NovaTunnelManager by lazy {
    NovaTunnelManager(
      context = appContext,
      repository = repository,
      keyManager = keyManager,
      latencyProbe = latencyProbe,
    )
  }
}

class NovaVpnApp : Application() {
  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
  }
}
