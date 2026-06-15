package com.last.app.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.last.app.data.repository.LastRepository
import com.last.app.presentation.dashboard.DashboardViewModel
import com.last.app.presentation.device.DeviceViewModel
import com.last.app.presentation.history.HistoryViewModel
import com.last.app.presentation.location.LocationViewModel
import com.last.app.presentation.settings.SettingsViewModel

class ViewModelFactory(
    private val repository: LastRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(DeviceViewModel::class.java) -> DeviceViewModel(repository) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(repository) as T
            modelClass.isAssignableFrom(LocationViewModel::class.java) -> LocationViewModel(repository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
