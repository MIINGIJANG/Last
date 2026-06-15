package com.last.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.last.app.data.repository.LastRepository
import com.last.app.domain.model.dashboard.DashboardRecentEventItem
import com.last.app.domain.model.location.DeviceMapMarker
import com.last.app.data.entity.Device
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val devices: List<Device> = emptyList(),
    val selectedDevice: Device? = null,
    val recentEvents: List<DashboardRecentEventItem> = emptyList(),
    val mapMarkers: List<DeviceMapMarker> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: LastRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val selectedDevice = MutableStateFlow<Device?>(null)
    private val isScreenActive = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            isScreenActive.flatMapLatest { active ->
                if (active) {
                    repository.observeDashboardState(selectedDevice)
                } else {
                    flowOf()
                }
            }.collectLatest { state ->
                _uiState.update {
                    DashboardUiState(
                        isLoading = false,
                        devices = state.devices,
                        selectedDevice = state.selectedDevice,
                        recentEvents = state.recentEvents,
                        mapMarkers = state.mapMarkers,
                    )
                }
            }
        }
    }

    fun setScreenActive(active: Boolean) {
        isScreenActive.value = active
    }

    fun toggleDeviceSelection(device: Device) {
        selectedDevice.update { current ->
            if (current?.id == device.id) null else device
        }
    }
}
