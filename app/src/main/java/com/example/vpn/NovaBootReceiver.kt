package com.example.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.example.NovaVpnApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles BOOT_COMPLETED to trigger auto-connect if enabled by the user
 * and Android VPN permission has already been granted.
 */
class NovaBootReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
    val pendingResult = goAsync()
    val app = context.applicationContext as? NovaVpnApp ?: run {
      pendingResult.finish()
      return
    }

    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val settings = app.container.settingsDataStore.settingsFlow.first()
        if (settings.autoConnectOnBoot && VpnService.prepare(context) == null) {
          app.container.repository.autoConnectOnBootOrWifi()
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}
