package com.last.app.presentation.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.last.app.data.repository.LastRepository
import com.last.app.presentation.dashboard.DashboardView
import com.last.app.presentation.dashboard.DashboardViewModel
import com.last.app.presentation.device.DeviceView
import com.last.app.presentation.device.DeviceViewModel
import com.last.app.presentation.history.HistoryView
import com.last.app.presentation.history.HistoryViewModel
import com.last.app.presentation.location.LocationView
import com.last.app.presentation.location.LocationViewModel
import com.last.app.presentation.navigation.AppDestination
import com.last.app.presentation.navigation.BottomNavBar
import com.last.app.presentation.settings.SettingsPermissionCallbacks
import com.last.app.presentation.settings.SettingsView
import com.last.app.presentation.settings.SettingsViewModel
import com.last.app.presentation.theme.LastBackground

@Composable
fun LastApp(
    repository: LastRepository,
    settingsPermissionCallbacks: SettingsPermissionCallbacks,
    modifier: Modifier = Modifier,
) {
    var selectedDestination by rememberSaveable { mutableStateOf(AppDestination.Dashboard) }
    var initializedTabNames by rememberSaveable {
        mutableStateOf(listOf(AppDestination.Dashboard.name))
    }
    val factory = ViewModelFactory(repository)

    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val deviceViewModel: DeviceViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)
    val locationViewModel: LocationViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    LaunchedEffect(selectedDestination) {
        if (selectedDestination.name !in initializedTabNames) {
            initializedTabNames = initializedTabNames + selectedDestination.name
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LastBackground,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            BottomNavBar(
                selected = selectedDestination,
                onSelect = { selectedDestination = it },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()

        Box(contentModifier) {
            if (AppDestination.Dashboard.name in initializedTabNames) {
                KeepAliveTab(visible = selectedDestination == AppDestination.Dashboard) {
                    DashboardView(
                        viewModel = dashboardViewModel,
                        isTabVisible = selectedDestination == AppDestination.Dashboard,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (AppDestination.Device.name in initializedTabNames) {
                KeepAliveTab(visible = selectedDestination == AppDestination.Device) {
                    DeviceView(
                        viewModel = deviceViewModel,
                        isTabVisible = selectedDestination == AppDestination.Device,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (AppDestination.History.name in initializedTabNames) {
                KeepAliveTab(visible = selectedDestination == AppDestination.History) {
                    HistoryView(
                        viewModel = historyViewModel,
                        isTabVisible = selectedDestination == AppDestination.History,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (AppDestination.Settings.name in initializedTabNames) {
                KeepAliveTab(visible = selectedDestination == AppDestination.Settings) {
                    SettingsView(
                        viewModel = settingsViewModel,
                        permissionCallbacks = settingsPermissionCallbacks,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            if (AppDestination.Location.name in initializedTabNames) {
                KeepAliveTab(visible = selectedDestination == AppDestination.Location) {
                    LocationView(
                        viewModel = locationViewModel,
                        isTabVisible = selectedDestination == AppDestination.Location,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
