package com.last.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.last.app.data.repository.LastRepository
import com.last.app.data.entity.Device
import com.last.app.domain.util.eventTimelineSignature
import com.last.app.domain.model.history.HistoryTimelineSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HistoryUiState(
    val isLoading: Boolean = true,
    val isResolvingLocations: Boolean = false,
    val sections: List<HistoryTimelineSection> = emptyList(),
    val devices: List<Device> = emptyList(),
    val selectedDevice: Device? = null,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HistoryViewModel(
    private val repository: LastRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    private val selectedDevice = MutableStateFlow<Device?>(null)
    private val isScreenActive = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            isScreenActive.flatMapLatest { active ->
                if (!active) {
                    flowOf()
                } else {
                    combine(
                        repository.observeSortedRegisteredDevices(),
                        selectedDevice,
                    ) { devices, selected ->
                        devices to selected
                    }
                        .flatMapLatest { (devices, selected) ->
                            val registeredIds = devices.map { it.id }
                            repository.observeConnectionEvents(selected, registeredIds)
                                .distinctUntilChanged { previous, current ->
                                    previous.eventTimelineSignature() == current.eventTimelineSignature()
                                }
                                .map { events ->
                                    Triple(devices, selected, events)
                                }
                        }
                        .debounce(HISTORY_REBUILD_DEBOUNCE_MS)
                }
            }.collectLatest { (devices, selected, events) ->
                    val previous = _uiState.value
                    _uiState.value = previous.copy(
                        isLoading = false,
                        isResolvingLocations = true,
                        devices = devices,
                        selectedDevice = selected,
                    )
                    val sections = withContext(Dispatchers.IO) {
                        repository.buildHistoryTimeline(events, devices)
                    }
                    _uiState.value = HistoryUiState(
                        isLoading = false,
                        isResolvingLocations = false,
                        sections = sections,
                        devices = devices,
                        selectedDevice = selected,
                    )
                }
        }
    }

    fun selectDevice(device: Device?) {
        selectedDevice.value = device
    }

    fun setScreenActive(active: Boolean) {
        isScreenActive.value = active
        if (!active) {
            _uiState.value = _uiState.value.copy(isResolvingLocations = false)
        }
    }

    companion object {
        private const val HISTORY_REBUILD_DEBOUNCE_MS = 250L
    }
}
