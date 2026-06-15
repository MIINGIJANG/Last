package com.last.app.infrastructure.monitoring

import com.last.app.data.repository.LastRepository
import com.last.app.infrastructure.monitoring.ConnectionState
import com.last.app.data.entity.AppSettings
import com.last.app.data.entity.DeviceConnectionEvent
import com.last.app.data.entity.EventSources
import com.last.app.data.entity.EventType
import com.last.app.external.location.LocationService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ConnectionMonitor(
    private val repository: LastRepository,
    private val locationService: LocationService,
) {
    private val eventMutex = Mutex()
    private var cachedSettings: AppSettings? = null
    private val recentEventTimestamps = mutableMapOf<EventDedupKey, Long>()

    var state: ConnectionState = ConnectionState.IDLE
        private set

    val monitoring: Boolean
        get() = state == ConnectionState.MONITORING

    suspend fun refreshSettings() {
        cachedSettings = repository.getSettings()
    }

    private suspend fun settings(): AppSettings {
        return cachedSettings ?: repository.getSettings().also { cachedSettings = it }
    }

    suspend fun startMonitoring() {
        val settings = settings()
        if (!settings.autoMonitoringEnabled) {
            state = ConnectionState.IDLE
            return
        }
        state = ConnectionState.MONITORING
        repository.pruneStaleData()
        repository.syncRegisteredBluetoothConnectionStatuses()
        repository.saveLog(
            message = "등록된 주변기기 자동 모니터링을 시작했습니다",
            logType = "SYSTEM",
        )
    }

    suspend fun stopMonitoring() {
        state = ConnectionState.FINAL
        cachedSettings = null
        recentEventTimestamps.clear()
        repository.saveLog(message = "모니터링이 중지되었습니다", logType = "SYSTEM")
    }

    suspend fun syncBluetoothConnectionStatuses() = eventMutex.withLock {
        if (state == ConnectionState.FINAL) return@withLock
        val changes = repository.detectBluetoothConnectionStatusChanges()
        val eventTime = System.currentTimeMillis()
        for (change in changes) {
            val eventType = if (change.connected) EventType.CONNECT else EventType.DISCONNECT
            if (shouldSkipDuplicate(change.deviceId, eventType, eventTime)) continue
            if (eventType.isDisconnect) {
                handleDisconnect(
                    deviceType = change.deviceType,
                    eventType = eventType,
                    eventTime = eventTime,
                    settings = settings(),
                    deviceId = change.deviceId,
                )
            } else {
                handleConnect(
                    deviceType = change.deviceType,
                    eventType = eventType,
                    eventTime = eventTime,
                    deviceId = change.deviceId,
                )
            }
        }
    }

    suspend fun detectConnectionEvent(
        deviceType: String,
        eventType: EventType,
        eventTime: Long,
        deviceIdentifier: String? = null,
    ) = eventMutex.withLock {
        val settings = settings()
        if (!settings.autoMonitoringEnabled || state == ConnectionState.FINAL) return@withLock

        val source = repository.mapEventSource(deviceType)
        if (!repository.hasRegisteredDevicesForSource(source)) return@withLock

        val resolvedDeviceId = repository.resolveDeviceId(deviceIdentifier, source)
        if (resolvedDeviceId == null) {
            if (source == EventSources.BLUETOOTH) {
                recordBluetoothChangesFromSync(eventTime)
            }
            return@withLock
        }

        if (shouldSkipDuplicate(resolvedDeviceId, eventType, eventTime)) return@withLock

        if (eventType.isDisconnect) {
            handleDisconnect(
                deviceType = deviceType,
                eventType = eventType,
                eventTime = eventTime,
                settings = settings,
                deviceId = resolvedDeviceId,
            )
        } else {
            handleConnect(
                deviceType = deviceType,
                eventType = eventType,
                eventTime = eventTime,
                deviceId = resolvedDeviceId,
            )
        }
    }

    private suspend fun recordBluetoothChangesFromSync(referenceTime: Long) {
        val changes = repository.detectBluetoothConnectionStatusChanges()
        for (change in changes) {
            val eventType = if (change.connected) EventType.CONNECT else EventType.DISCONNECT
            if (shouldSkipDuplicate(change.deviceId, eventType, referenceTime)) continue
            if (eventType.isDisconnect) {
                handleDisconnect(
                    deviceType = change.deviceType,
                    eventType = eventType,
                    eventTime = referenceTime,
                    settings = settings(),
                    deviceId = change.deviceId,
                )
            } else {
                handleConnect(
                    deviceType = change.deviceType,
                    eventType = eventType,
                    eventTime = referenceTime,
                    deviceId = change.deviceId,
                )
            }
        }
    }

    private fun shouldSkipDuplicate(deviceId: Long, eventType: EventType, eventTime: Long): Boolean {
        val key = EventDedupKey(deviceId, eventType)
        val previous = recentEventTimestamps[key]
        if (previous != null && eventTime - previous < DEDUP_WINDOW_MS) {
            return true
        }
        recentEventTimestamps[key] = eventTime
        pruneExpiredDedupKeys(eventTime)
        return false
    }

    private fun pruneExpiredDedupKeys(now: Long) {
        if (recentEventTimestamps.size < DEDUP_PRUNE_THRESHOLD) return
        val iterator = recentEventTimestamps.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > DEDUP_WINDOW_MS) {
                iterator.remove()
            }
        }
    }

    private suspend fun handleConnect(
        deviceType: String,
        eventType: EventType,
        eventTime: Long,
        deviceId: Long,
    ) {
        state = ConnectionState.CONNECTED
        val eventDeviceType = repository.getRegisteredDeviceType(deviceId) ?: deviceType
        val event = DeviceConnectionEvent.createEvent(eventDeviceType, eventType, eventTime, deviceId)
        val settings = settings()
        val location = if (settings.locationTrackingEnabled && locationService.hasLocationPermission()) {
            locationService.getCurrentLocation()
        } else {
            null
        }
        repository.recordConnectEvent(
            event = event,
            location = location,
            logMessage = "Connection event recorded: ${event.getEventInfo()}",
        )
        if (settings.locationTrackingEnabled && location != null) {
            repository.savePeriodicLocationForDevice(
                deviceId = deviceId,
                deviceType = eventDeviceType,
                location = location,
            )
        }
        state = ConnectionState.STATUS_UPDATED
        state = ConnectionState.MONITORING
    }

    private suspend fun handleDisconnect(
        deviceType: String,
        eventType: EventType,
        eventTime: Long,
        settings: AppSettings,
        deviceId: Long,
    ) {
        state = ConnectionState.DISCONNECTED
        val eventDeviceType = repository.getRegisteredDeviceType(deviceId) ?: deviceType
        val event = DeviceConnectionEvent.createEvent(eventDeviceType, eventType, eventTime, deviceId)
        var locationSaved = false
        val location = if (settings.locationTrackingEnabled && locationService.hasLocationPermission()) {
            locationService.getCurrentLocation()
        } else {
            null
        }
        if (location != null) locationSaved = true

        if (settings.locationTrackingEnabled && location == null) {
            repository.saveLog(message = "Location retrieval failed", logType = "ERROR", deviceId = deviceId)
        }

        repository.recordDisconnectEvent(
            event = event,
            location = location,
            wifiProvider = if (location != null) {
                { locationId -> locationService.collectWifiInformation(locationId) }
            } else {
                null
            },
            logMessage = "Disconnect event recorded: ${event.getEventInfo()}",
            logType = if (locationSaved) "DISCONNECT" else "DISCONNECT_NO_LOCATION",
        )

        state = if (locationSaved) ConnectionState.LOCATION_SAVED else ConnectionState.EVENT_RECORDED
        state = ConnectionState.STATUS_UPDATED
        state = ConnectionState.MONITORING
    }

    private data class EventDedupKey(
        val deviceId: Long,
        val eventType: EventType,
    )

    companion object {
        private const val DEDUP_WINDOW_MS = 1_500L
        private const val DEDUP_PRUNE_THRESHOLD = 64
    }
}
