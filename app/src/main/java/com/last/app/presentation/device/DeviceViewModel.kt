package com.last.app.presentation.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.last.app.data.repository.LastRepository
import com.last.app.data.entity.Device
import com.last.app.domain.util.deviceListSignature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DeviceUiState(
    val isLoading: Boolean = true,
    val registered: List<Device> = emptyList(),
    val available: List<Device> = emptyList(),
    val deviceScanEnabled: Boolean = true,
    val isScanning: Boolean = false,
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceViewModel(
    private val repository: LastRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceUiState())
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    private val lastScanAt = MutableStateFlow(0L)
    private val isScreenActive = MutableStateFlow(false)
    private var scanExpiryJob: Job? = null
    private var clearedAvailableOnStart = false

    init {
        viewModelScope.launch {
            isScreenActive.flatMapLatest { active ->
                if (!active) {
                    flowOf()
                } else {
                    combine(
                        repository.observeSortedRegisteredDevices(),
                        repository.observeAvailableDevices()
                            .distinctUntilChanged { previous, current ->
                                previous.deviceListSignature() == current.deviceListSignature()
                            },
                        repository.observeSettings(),
                        lastScanAt,
                    ) { registered, available, settings, scanAt ->
                        val scanFresh = isScanFresh(scanAt)
                        DeviceUiState(
                            isLoading = false,
                            registered = registered,
                            available = if (scanFresh) available else emptyList(),
                            deviceScanEnabled = settings.deviceScanEnabled,
                            isScanning = _uiState.value.isScanning,
                            message = _uiState.value.message,
                        )
                    }
                }
            }.collectLatest { state -> _uiState.value = state }
        }
    }

    fun setScreenActive(active: Boolean) {
        isScreenActive.value = active
        if (active && !clearedAvailableOnStart) {
            clearedAvailableOnStart = true
            viewModelScope.launch(Dispatchers.IO) {
                repository.clearAvailableDevices()
            }
        }
    }

    fun registerDevice(deviceId: Long) {
        viewModelScope.launch {
            if (!isScanFresh(lastScanAt.value)) return@launch
            runCatching { repository.registerDevice(deviceId) }
                .onSuccess { _uiState.update { it.copy(message = "success") } }
                .onFailure { _uiState.update { it.copy(message = "failed") } }
        }
    }

    fun unregisterDevice(deviceId: Long) {
        viewModelScope.launch { repository.unregisterDevice(deviceId) }
    }

    fun scanDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            runCatching { repository.scanNearbyDevices() }
                .onSuccess {
                    lastScanAt.value = System.currentTimeMillis()
                    _uiState.update { it.copy(isScanning = false) }
                    scheduleScanExpiry()
                }
                .onFailure {
                    _uiState.update { it.copy(isScanning = false, message = "scan_failed") }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun scheduleScanExpiry() {
        scanExpiryJob?.cancel()
        scanExpiryJob = viewModelScope.launch {
            delay(SCAN_VISIBLE_MS)
            if (!isScanFresh(lastScanAt.value)) return@launch
            lastScanAt.value = 0
            withContext(Dispatchers.IO) {
                repository.clearAvailableDevices()
            }
        }
    }

    private fun isScanFresh(scanAt: Long): Boolean {
        return scanAt > 0 && System.currentTimeMillis() - scanAt <= SCAN_VISIBLE_MS
    }

    companion object {
        private const val SCAN_VISIBLE_MS = 30_000L
    }
}
