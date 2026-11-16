package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.AppLanguage
import com.example.data.model.VpnConnectionState
import com.example.ui.components.VpnPermissionExplanationDialog
import com.example.ui.localization.LocalAppStrings
import com.example.ui.localization.stringsForLanguage
import com.example.ui.navigation.NovaRoute
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LocationsScreen
import com.example.ui.screens.ServerDetailsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeedTestScreen
import com.example.ui.theme.NovaVpnTheme
import com.example.ui.viewmodel.NovaViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      NovaVpnRoot()
    }
  }
}

private data class TopNavItem(
  val route: NovaRoute,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

@Composable
fun NovaVpnRoot(viewModel: NovaViewModel = viewModel()) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val countries by viewModel.countries.collectAsStateWithLifecycle()
  val allServers by viewModel.allServers.collectAsStateWithLifecycle()
  val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
  val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
  val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
  val syncStatus by viewModel.backendSyncStatus.collectAsStateWithLifecycle()
  val speedTestState by viewModel.speedTestState.collectAsStateWithLifecycle()
  val history by viewModel.connectionHistory.collectAsStateWithLifecycle()
  val privacyDashboard by viewModel.privacyDashboard.collectAsStateWithLifecycle()
  val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedRegion by viewModel.selectedRegion.collectAsStateWithLifecycle()
  val devNoticeDismissed by viewModel.devNoticeDismissed.collectAsStateWithLifecycle()
  val authMessage by viewModel.authMessage.collectAsStateWithLifecycle()

  val strings = stringsForLanguage(settings.language)
  val layoutDirection = if (settings.language == AppLanguage.ARABIC) {
    LayoutDirection.Rtl
  } else {
    LayoutDirection.Ltr
  }

  val context = LocalContext.current
  var showPermissionRationale by remember { mutableStateOf(false) }

  val vpnConsentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      viewModel.onVpnPermissionGranted()
    } else {
      viewModel.onVpnPermissionDenied()
    }
  }

  val notificationPermLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { /* Optional notification permission handled gracefully */ }

  LaunchedEffect(connectionState) {
    if (connectionState is VpnConnectionState.PreparingPermission) {
      showPermissionRationale = true
    } else if (connectionState is VpnConnectionState.Connecting &&
      settings.notificationsEnabled &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  CompositionLocalProvider(
    LocalAppStrings provides strings,
    LocalLayoutDirection provides layoutDirection,
  ) {
    NovaVpnTheme(themeMode = settings.themeMode) {
      val navController = rememberNavController()
      val backStackEntry by navController.currentBackStackEntryAsState()
      val currentDest = backStackEntry?.destination

      val navItems = listOf(
        TopNavItem(
          route = NovaRoute.HomeRoute,
          label = strings.navHome,
          selectedIcon = Icons.Rounded.Home,
          unselectedIcon = Icons.Outlined.Home,
          testTag = "nav_home",
        ),
        TopNavItem(
          route = NovaRoute.LocationsRoute,
          label = strings.navLocations,
          selectedIcon = Icons.Rounded.Public,
          unselectedIcon = Icons.Outlined.Public,
          testTag = "nav_locations",
        ),
        TopNavItem(
          route = NovaRoute.SpeedTestRoute,
          label = strings.navSpeedTest,
          selectedIcon = Icons.Rounded.Speed,
          unselectedIcon = Icons.Outlined.Speed,
          testTag = "nav_speed",
        ),
        TopNavItem(
          route = NovaRoute.HistoryRoute,
          label = strings.navHistory,
          selectedIcon = Icons.Rounded.History,
          unselectedIcon = Icons.Outlined.History,
          testTag = "nav_history",
        ),
        TopNavItem(
          route = NovaRoute.SettingsRoute,
          label = strings.navSettings,
          selectedIcon = Icons.Rounded.Settings,
          unselectedIcon = Icons.Outlined.Settings,
          testTag = "nav_settings",
        ),
      )

      val showTopLevelNav = currentDest?.hasRoute<NovaRoute.ServerDetailsRoute>() != true

      fun navigateToTopLevel(route: NovaRoute) {
        navController.navigate(route) {
          popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
          }
          launchSingleTop = true
          restoreState = true
        }
      }

      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          bottomBar = {
            if (showTopLevelNav && !useRail) {
              NavigationBar(windowInsets = WindowInsets.navigationBars) {
                navItems.forEach { item ->
                  val selected = currentDest?.hasRoute(item.route::class) == true
                  NavigationBarItem(
                    selected = selected,
                    onClick = { navigateToTopLevel(item.route) },
                    icon = {
                      Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                      )
                    },
                    label = {
                      Text(
                        text = item.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    },
                    modifier = Modifier.testTag(item.testTag),
                  )
                }
              }
            }
          },
        ) { innerPadding ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
          ) {
            if (showTopLevelNav && useRail) {
              NavigationRail {
                navItems.forEach { item ->
                  val selected = currentDest?.hasRoute(item.route::class) == true
                  NavigationRailItem(
                    selected = selected,
                    onClick = { navigateToTopLevel(item.route) },
                    icon = {
                      Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                      )
                    },
                    label = { Text(item.label, maxLines = 1) },
                    modifier = Modifier.testTag(item.testTag),
                  )
                }
              }
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
              NavHost(
                navController = navController,
                startDestination = NovaRoute.HomeRoute,
              ) {
                composable<NovaRoute.HomeRoute> {
                  HomeScreen(
                    connectionState = connectionState,
                    selectedCountry = selectedCountry,
                    selectedServer = selectedServer,
                    settings = settings,
                    privacyDashboard = privacyDashboard,
                    devNoticeDismissed = devNoticeDismissed,
                    onConnectToggle = { viewModel.connectOrDisconnectPrimary() },
                    onSmartLocationClick = { viewModel.smartConnectGlobal() },
                    onSelectCountryClick = { navigateToTopLevel(NovaRoute.LocationsRoute) },
                    onOpenSettingsClick = { navigateToTopLevel(NovaRoute.SettingsRoute) },
                    onDismissDevNotice = { viewModel.dismissDevNotice() },
                    onClearError = { viewModel.clearVpnError() },
                  )
                }

                composable<NovaRoute.LocationsRoute> {
                  LocationsScreen(
                    countries = countries,
                    searchQuery = searchQuery,
                    selectedRegion = selectedRegion,
                    syncStatus = syncStatus,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onRegionSelected = viewModel::selectRegion,
                    onRefreshClick = viewModel::refreshLocations,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onConnectSmartInCountry = { country ->
                      viewModel.connectToCountrySmart(country)
                      navigateToTopLevel(NovaRoute.HomeRoute)
                    },
                    onOpenServerDetails = { server ->
                      navController.navigate(NovaRoute.ServerDetailsRoute(server.id))
                    },
                  )
                }

                composable<NovaRoute.ServerDetailsRoute> { backStack ->
                  val route: NovaRoute.ServerDetailsRoute = backStack.toRoute()
                  val server = allServers.firstOrNull { it.id == route.serverId }
                  ServerDetailsScreen(
                    server = server,
                    connectionState = connectionState,
                    onBack = { navController.popBackStack() },
                    onMeasureLatency = viewModel::measureLatencyForServer,
                    onConnect = { target ->
                      viewModel.connectToSpecificServer(target)
                      navigateToTopLevel(NovaRoute.HomeRoute)
                    },
                    onDisconnect = viewModel::connectOrDisconnectPrimary,
                  )
                }

                composable<NovaRoute.SpeedTestRoute> {
                  SpeedTestScreen(
                    state = speedTestState,
                    onStartTest = viewModel::startSpeedTest,
                  )
                }

                composable<NovaRoute.HistoryRoute> {
                  HistoryScreen(
                    history = history,
                    onClearHistory = viewModel::clearHistory,
                  )
                }

                composable<NovaRoute.SettingsRoute> {
                  SettingsScreen(
                    settings = settings,
                    installedApps = installedApps,
                    authMessage = authMessage,
                    onLoadInstalledApps = viewModel::loadInstalledApps,
                    onSetLanguage = viewModel::setLanguage,
                    onSetThemeMode = viewModel::setThemeMode,
                    onSetProtocol = viewModel::setProtocol,
                    onSetSmartSelection = viewModel::setSmartSelection,
                    onSetKillSwitch = viewModel::setKillSwitch,
                    onSetDnsProtection = viewModel::setDnsProtection,
                    onSetSecureDns = viewModel::setSecureDns,
                    onSetIpv6Protection = viewModel::setIpv6Protection,
                    onSetAutoConnectBoot = viewModel::setAutoConnectBoot,
                    onSetAutoConnectWifi = viewModel::setAutoConnectWifi,
                    onSetSplitTunnel = viewModel::setSplitTunnel,
                    onSetNotifications = viewModel::setNotificationsEnabled,
                    onSetConnectionTimeout = viewModel::setConnectionTimeout,
                    onSaveBackendUrl = viewModel::saveBackendUrlAndSync,
                    onAuthenticate = viewModel::authenticate,
                    onSignOut = viewModel::signOut,
                  )
                }
              }
            }
          }
        }
      }

      if (showPermissionRationale) {
        VpnPermissionExplanationDialog(
          onAccept = {
            showPermissionRationale = false
            val intent = viewModel.prepareVpnPermissionIntent()
            if (intent != null) {
              vpnConsentLauncher.launch(intent)
            } else {
              viewModel.onVpnPermissionGranted()
            }
          },
          onDismiss = {
            showPermissionRationale = false
            viewModel.onVpnPermissionDenied()
          },
        )
      }
    }
  }
}
