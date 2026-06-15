package com.last.app.presentation.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.last.app.data.repository.LastRepository
import com.last.app.domain.util.cardTargetsSignature
import com.last.app.domain.util.deviceListSignature
import com.last.app.domain.model.location.DeviceMapMarker
import com.last.app.domain.model.location.MapLocationCardDisplay
import com.last.app.data.entity.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LocationUiState(
    val isLoading: Boolean = true,
    val isResolvingCards: Boolean = false,
    val devices: List<Device> = emptyList(),
    val selectedDevice: Device? = null,
    val markers: List<DeviceMapMarker> = emptyList(),
    val locationCards: List<MapLocationCardDisplay> = emptyList(),
    val centerLatitude: Double? = null,
    val centerLongitude: Double? = null,
    val mapRenderKey: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModel(
    private val repository: LastRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()
    private val selectedDevice = MutableStateFlow<Device?>(null)
    private val isScreenActive = MutableStateFlow(false)
    private var cachedCardTargetsSignature: Long? = null
    private var cachedLocationCards: List<MapLocationCardDisplay> = emptyList()

    init {
        viewModelScope.launch {
            isScreenActive.flatMapLatest { active ->
                if (active) {
                    repository.observeMapState(selectedDevice)
                } else {
                    flowOf()
                }
            }.collectLatest { state ->
                val previous = _uiState.value
                val cardSignature = state.cardTargets.cardTargetsSignature()
                val needsCards = state.cardTargets.isNotEmpty() && cardSignature != cachedCardTargetsSignature
                val needsCenter = state.markers.isEmpty() &&
                    state.devices.isNotEmpty() &&
                    previous.centerLatitude == null

                val cards = if (needsCards) {
                    withContext(Dispatchers.IO) {
                        repository.buildMapLocationCardDisplays(
                            targets = state.cardTargets,
                            geocodeMissing = false,
                        ).also {
                            cachedCardTargetsSignature = cardSignature
                            cachedLocationCards = it
                        }
                    }
                } else if (state.cardTargets.isEmpty()) {
                    emptyList()
                } else {
                    cachedLocationCards
                }

                val currentLocation = if (needsCenter) {
                    withContext(Dispatchers.IO) { repository.getCurrentLocation() }
                } else {
                    null
                }

                _uiState.value = LocationUiState(
                    isLoading = false,
                    isResolvingCards = false,
                    devices = state.devices,
                    selectedDevice = state.selectedDevice,
                    markers = state.markers,
                    locationCards = if (cards.isNotEmpty()) cards else previous.locationCards,
                    centerLatitude = currentLocation?.latitude ?: previous.centerLatitude,
                    centerLongitude = currentLocation?.longitude ?: previous.centerLongitude,
                    mapRenderKey = previous.mapRenderKey,
                )
            }
        }
    }

    fun setScreenActive(active: Boolean) {
        isScreenActive.value = active
        if (!active) {
            _uiState.value = _uiState.value.copy(isResolvingCards = false)
        }
    }

    fun selectDevice(device: Device?) {
        selectedDevice.value = device
    }

    fun recenterMap() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.markers.isEmpty() && state.devices.isNotEmpty()) {
                val current = withContext(Dispatchers.IO) { repository.getCurrentLocation() }
                _uiState.value = state.copy(
                    centerLatitude = current?.latitude,
                    centerLongitude = current?.longitude,
                    mapRenderKey = state.mapRenderKey + 1,
                )
            } else {
                _uiState.value = state.copy(mapRenderKey = state.mapRenderKey + 1)
            }
        }
    }
}
