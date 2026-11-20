package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.domain.model.WeatherCondition
import com.example.ui.WeatherViewModel
import com.example.ui.components.AnimatedSkyBackground
import com.example.ui.screens.LocationsScreen
import com.example.ui.screens.WeatherHomeScreen
import com.example.ui.screens.WidgetStudioScreen
import com.example.ui.theme.BrisaTheme
import com.example.ui.theme.IceCyan
import kotlinx.serialization.Serializable

@Serializable
data object WeatherRoute

@Serializable
data object LocationsRoute

@Serializable
data object WidgetsRoute

private data class BrisaNavItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        setContent {
            BrisaTheme(darkTheme = true, dynamicColor = false) {
                BrisaApp()
            }
        }
    }
}

@Composable
fun BrisaApp(
    viewModel: WeatherViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchCurrentGpsLocation()
        } else {
            Toast.makeText(
                context,
                "Permiso de ubicación denegado. Puedes buscar cualquier ciudad manualmente.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val requestGpsWithPermission: () -> Unit = {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            viewModel.fetchCurrentGpsLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }

    val navItems = listOf(
        BrisaNavItem(
            route = WeatherRoute,
            label = "Clima",
            selectedIcon = Icons.Rounded.WbSunny,
            unselectedIcon = Icons.Outlined.WbSunny,
            testTag = "nav_weather",
        ),
        BrisaNavItem(
            route = LocationsRoute,
            label = "Ciudades",
            selectedIcon = Icons.Rounded.LocationCity,
            unselectedIcon = Icons.Outlined.LocationCity,
            testTag = "nav_locations",
        ),
        BrisaNavItem(
            route = WidgetsRoute,
            label = "Widgets",
            selectedIcon = Icons.Rounded.Widgets,
            unselectedIcon = Icons.Outlined.Widgets,
            testTag = "nav_widgets",
        ),
    )

    val navigateTo: (Any) -> Unit = { destination ->
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val activeCondition = uiState.activeSnapshot?.condition ?: WeatherCondition.PARTLY_CLOUDY
    val isDay = uiState.activeSnapshot?.isDay ?: true

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedSkyBackground(
            condition = activeCondition,
            isDay = isDay,
            modifier = Modifier.fillMaxSize(),
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = Color(0xCC091326),
                            contentColor = Color.White,
                            windowInsets = WindowInsets.navigationBars,
                        ) {
                            navItems.forEach { item ->
                                val selected = currentDestination?.hasRoute(item.route::class) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                        )
                                    },
                                    label = { Text(item.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF091326),
                                        selectedTextColor = IceCyan,
                                        indicatorColor = IceCyan,
                                        unselectedIconColor = Color.White.copy(alpha = 0.72f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.72f),
                                    ),
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
                        .padding(innerPadding)
                        .windowInsetsPadding(WindowInsets.statusBars),
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = Color(0x88091326),
                            contentColor = Color.White,
                        ) {
                            navItems.forEach { item ->
                                val selected = currentDestination?.hasRoute(item.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                        )
                                    },
                                    label = { Text(item.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = Color(0xFF091326),
                                        selectedTextColor = IceCyan,
                                        indicatorColor = IceCyan,
                                        unselectedIconColor = Color.White.copy(alpha = 0.72f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.72f),
                                    ),
                                    modifier = Modifier.testTag(item.testTag),
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = WeatherRoute,
                        modifier = Modifier.weight(1f),
                    ) {
                        composable<WeatherRoute> {
                            WeatherHomeScreen(
                                uiState = uiState,
                                onRefresh = viewModel::refreshActiveWeather,
                                onCycleCity = viewModel::cycleLocation,
                                onRequestGps = requestGpsWithPermission,
                                onToggleUnit = viewModel::toggleTemperatureUnit,
                                onOpenLocations = { navigateTo(LocationsRoute) },
                                onDismissBanner = viewModel::clearBanner,
                            )
                        }
                        composable<LocationsRoute> {
                            LocationsScreen(
                                uiState = uiState,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onAddCity = { city ->
                                    viewModel.addAndSelectCity(city)
                                    navigateTo(WeatherRoute)
                                },
                                onSelectLocation = { id ->
                                    viewModel.selectLocation(id)
                                    navigateTo(WeatherRoute)
                                },
                                onRemoveLocation = viewModel::removeLocation,
                                onRequestGps = requestGpsWithPermission,
                            )
                        }
                        composable<WidgetsRoute> {
                            WidgetStudioScreen(
                                uiState = uiState,
                                onCycleCity = viewModel::cycleLocation,
                                onToggleUnit = viewModel::toggleTemperatureUnit,
                                onToggleClockFormat = viewModel::toggleClockFormat,
                                onToggleForecastMode = viewModel::toggleForecastWidgetMode,
                                onRefresh = viewModel::refreshActiveWeather,
                            )
                        }
                    }
                }
            }
        }
    }
}
