package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface NovaRoute {
  @Serializable data object HomeRoute : NovaRoute
  @Serializable data object LocationsRoute : NovaRoute
  @Serializable data class ServerDetailsRoute(val serverId: String) : NovaRoute
  @Serializable data object SpeedTestRoute : NovaRoute
  @Serializable data object HistoryRoute : NovaRoute
  @Serializable data object SettingsRoute : NovaRoute
}
