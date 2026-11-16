package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class NovaVpnMonitorService : Service() {

  companion object {
    const val CHANNEL_ID = "nova_vpn_tunnel_channel"
    const val NOTIFICATION_ID = 4091
    const val ACTION_START = "com.example.vpn.ACTION_START_MONITOR"
    const val ACTION_UPDATE = "com.example.vpn.ACTION_UPDATE_MONITOR"
    const val ACTION_STOP = "com.example.vpn.ACTION_STOP_MONITOR"
    const val ACTION_USER_DISCONNECT = "com.example.vpn.ACTION_USER_DISCONNECT"

    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_SUBTITLE = "extra_subtitle"

    fun startOrUpdate(context: Context, title: String, subtitle: String) {
      val intent = Intent(context, NovaVpnMonitorService::class.java).apply {
        action = ACTION_START
        putExtra(EXTRA_TITLE, title)
        putExtra(EXTRA_SUBTITLE, subtitle)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, NovaVpnMonitorService::class.java).apply {
        action = ACTION_STOP
      }
      context.stopService(intent)
    }
  }

  override fun onCreate() {
    super.onCreate()
    ensureChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_USER_DISCONNECT -> {
        val app = applicationContext as? com.example.NovaVpnApp
        app?.container?.tunnelManager?.disconnect(userInitiated = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      else -> {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)
        val subtitle = intent?.getStringExtra(EXTRA_SUBTITLE) ?: getString(R.string.vpn_notification_connecting)
        val notification = buildNotification(title, subtitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
          startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
          )
        } else {
          startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
      }
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun ensureChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(NotificationManager::class.java) ?: return
      val existing = nm.getNotificationChannel(CHANNEL_ID)
      if (existing == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          getString(R.string.vpn_notification_channel_name),
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = getString(R.string.vpn_notification_channel_desc)
          setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
      }
    }
  }

  private fun buildNotification(title: String, subtitle: String): Notification {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openPending = PendingIntent.getActivity(
      this,
      1,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val disconnectIntent = Intent(this, NovaVpnMonitorService::class.java).apply {
      action = ACTION_USER_DISCONNECT
    }
    val disconnectPending = PendingIntent.getService(
      this,
      2,
      disconnectIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_lock_lock)
      .setContentTitle(title)
      .setContentText(subtitle)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setContentIntent(openPending)
      .addAction(0, getString(R.string.vpn_notification_disconnect), disconnectPending)
      .build()
  }
}
