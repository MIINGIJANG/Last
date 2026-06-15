package com.last.app.infrastructure.monitoring

import com.last.app.data.repository.LastRepository
import com.last.app.external.location.LocationService
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class PeriodicLocationRecorder(
    private val repository: LastRepository,
    private val locationService: LocationService,
) {

    companion object {
        private const val INTERVAL_MS = 15 * 60 * 1000L
    }

    suspend fun captureSnapshotIfNeeded() {
        val settings = repository.getSettings()
        if (!settings.autoMonitoringEnabled || !settings.locationTrackingEnabled) return
        if (!locationService.hasLocationPermission()) return

        val connectedDevices = repository.getConnectedRegisteredDevices()
        if (connectedDevices.isEmpty()) return

        val location = locationService.getCurrentLocation() ?: return
        repository.savePeriodicLocations(connectedDevices, location)
    }

    suspend fun runLoop() {
        captureSnapshotIfNeeded()
        while (currentCoroutineContext().isActive) {
            delay(INTERVAL_MS)
            captureSnapshotIfNeeded()
        }
    }
}
