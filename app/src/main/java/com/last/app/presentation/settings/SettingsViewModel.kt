package com.last.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.last.app.data.repository.LastRepository
import com.last.app.data.entity.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
)

class SettingsViewModel(
    private val repository: LastRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                _uiState.update {
                    SettingsUiState(isLoading = false, settings = settings)
                }
            }
        }
    }

    fun setLocationTrackingEnabled(enabled: Boolean) {
        updateSetting { it.copy(locationTrackingEnabled = enabled) }
    }

    fun setDeviceScanEnabled(enabled: Boolean) {
        updateSetting { it.copy(deviceScanEnabled = enabled) }
    }

    private fun updateSetting(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(transform(current))
        }
    }
}
