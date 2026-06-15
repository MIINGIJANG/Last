package com.last.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.last.app.data.database.AppDatabase
import com.last.app.data.database.DatabaseOpenCallback
import com.last.app.data.database.DatabaseQueryLimits
import com.last.app.data.database.DatabaseRetention
import com.last.app.data.dao.EventDao
import com.last.app.data.entity.AppSettings
import com.last.app.data.entity.Device
import com.last.app.data.entity.DeviceConnectionEvent
import com.last.app.data.entity.DeviceLastKnownLocation
import com.last.app.data.entity.DeviceLocation
import com.last.app.data.entity.EventSources
import com.last.app.data.entity.SystemLog
import com.last.app.data.entity.WifiInformation
import com.last.app.external.location.AddressResolver
import com.last.app.external.system.EventIntentParser
import com.last.app.external.location.FusedLocationService
import com.last.app.external.device.DeviceIdentifier
import com.last.app.external.device.DeviceScanner
import com.last.app.domain.util.DateFormats
import com.last.app.domain.util.deviceListSignature
import com.last.app.domain.util.stateSignature
import com.last.app.domain.index.DeviceLocationIndex
import com.last.app.domain.index.RegisteredDeviceIndex
import com.last.app.domain.index.ScannedDeviceIndex
import com.last.app.domain.index.RegisteredDeviceOrdering
import com.last.app.domain.history.HistoryTimelineBuilder
import com.last.app.domain.history.HistoryTimelineCache
import com.last.app.domain.model.history.HistoryTimelineSection
import com.last.app.domain.location.EventLocationLabelResolver
import com.last.app.domain.history.historyEventStatusLabel
import com.last.app.domain.history.isDisconnectEvent
import com.last.app.domain.model.location.toMapMarker
import com.last.app.domain.model.device.BluetoothConnectionChange
import com.last.app.domain.model.device.ScannedDevice
import com.last.app.domain.model.dashboard.DashboardRecentEventItem
import com.last.app.domain.model.dashboard.DashboardState
import com.last.app.domain.model.location.MapLocationCardDisplay
import com.last.app.domain.model.location.MapLocationCardTarget
import com.last.app.domain.model.location.MapState
import com.last.app.domain.model.location.DeviceMapMarker
import com.last.app.domain.model.location.ResolvedDeviceLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import java.util.Date
import java.util.Locale

class LastRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val deviceDao = db.deviceDao()
    private val eventDao = db.eventDao()
    private val latestLocatedEventDao = db.latestLocatedEventDao()
    private val locationDao = db.locationDao()
    private val lastKnownLocationDao = db.lastKnownLocationDao()
    private val systemLogDao = db.systemLogDao()
    private val wifiDao = db.wifiDao()
    private val settingsDao = db.settingsDao()
    private val locationService = FusedLocationService(appContext)
    private val deviceScanner = DeviceScanner(appContext)

    @Volatile
    private var cachedRegisteredDeviceIndex: RegisteredDeviceIndex? = null

    @Volatile
    private var cachedBluetoothScan: List<ScannedDevice>? = null

    @Volatile
    private var bluetoothScanAtMs: Long = 0L

    @Volatile
    private var lastPruneAtMs: Long = 0L

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val sharedLatestLocatedEvents = eventDao.observeLatestLocatedEventsPerDevice()
        .distinctUntilChanged()
        .shareIn(repositoryScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), replay = 1)

    private val sharedPeriodicLocations = lastKnownLocationDao.observeAll()
        .distinctUntilChanged()
        .shareIn(repositoryScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), replay = 1)

    private val sharedSortedRegisteredDevices = observeRegisteredDevices()
        .distinctUntilChanged { previous, current ->
            previous.deviceListSignature() == current.deviceListSignature()
        }
        .map { RegisteredDeviceOrdering.sort(it) }
        .shareIn(repositoryScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), replay = 1)

    private suspend fun registeredDeviceIndex(): RegisteredDeviceIndex {
        cachedRegisteredDeviceIndex?.let { return it }
        return RegisteredDeviceIndex(deviceDao.getRegisteredDevices()).also {
            cachedRegisteredDeviceIndex = it
        }
    }

    private fun invalidateDeviceCaches() {
        invalidateRegisteredDeviceCaches()
        invalidateBluetoothScanCache()
    }

    private fun invalidateRegisteredDeviceCaches() {
        cachedRegisteredDeviceIndex = null
        HistoryTimelineCache.invalidate()
    }

    private fun cacheBluetoothScanResult(scanned: List<ScannedDevice>) {
        cachedBluetoothScan = scanned.filter { it.device.eventSource == EventSources.BLUETOOTH }
        bluetoothScanAtMs = System.currentTimeMillis()
    }

    private fun invalidateBluetoothScanCache() {
        cachedBluetoothScan = null
        bluetoothScanAtMs = 0L
    }

    private suspend fun cachedBluetoothScan(forceRefresh: Boolean = false): List<ScannedDevice> {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val cached = cachedBluetoothScan
            if (cached != null && now - bluetoothScanAtMs < BLUETOOTH_SCAN_TTL_MS) {
                return cached
            }
        }
        return deviceScanner.scanBluetoothDevices().also { scanned ->
            cachedBluetoothScan = scanned
            bluetoothScanAtMs = now
        }
    }

    private suspend fun applyConnectionStatusBatch(devices: List<Device>, statusById: Map<Long, Boolean>) {
        if (statusById.isEmpty()) return
        val connectIds = ArrayList<Long>()
        val disconnectIds = ArrayList<Long>()
        for (device in devices) {
            val connected = statusById[device.id] ?: continue
            if (device.isConnected == connected) continue
            if (connected) {
                connectIds.add(device.id)
            } else {
                disconnectIds.add(device.id)
            }
        }
        for (chunk in connectIds.chunked(DatabaseQueryLimits.IN_CLAUSE_CHUNK)) {
            deviceDao.updateConnectionStatusForIds(chunk, connected = true)
        }
        for (chunk in disconnectIds.chunked(DatabaseQueryLimits.IN_CLAUSE_CHUNK)) {
            deviceDao.updateConnectionStatusForIds(chunk, connected = false)
        }
    }

    private suspend fun upsertLocatedEventCache(deviceId: Long, eventId: Long, eventTime: Long) {
        latestLocatedEventDao.upsertIfNewer(deviceId, eventId, eventTime)
    }

    fun observeSortedRegisteredDevices(): Flow<List<Device>> = sharedSortedRegisteredDevices

    fun observeRegisteredDevices(): Flow<List<Device>> = deviceDao.observeRegisteredDevices()

    fun observeAvailableDevices(): Flow<List<Device>> = deviceDao.observeAvailableDevices()

    fun observeRecentEventsForRegisteredDevices(
        limit: Int,
        devices: List<Device>,
    ): Flow<List<DeviceConnectionEvent>> {
        val deviceIds = devices.map { it.id }
        if (deviceIds.isEmpty()) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }
        return eventDao.observeRecentEventsForDevices(deviceIds, limit)
    }

    fun observeConnectionEvents(device: Device?, registeredDeviceIds: List<Long> = emptyList()): Flow<List<DeviceConnectionEvent>> {
        return when {
            device != null -> eventDao.observeEventsForDeviceId(device.id, DatabaseRetention.HISTORY_EVENT_LIMIT)
            registeredDeviceIds.isEmpty() -> kotlinx.coroutines.flow.flowOf(emptyList())
            else -> eventDao.observeEventsForRegisteredDevices(DatabaseRetention.HISTORY_EVENT_LIMIT)
        }
    }

    fun observeSettings(): Flow<AppSettings> {
        return settingsDao.observeSettings().map { it ?: AppSettings() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDashboardState(selectedDevice: Flow<Device?>): Flow<DashboardState> {
        return combine(
            observeSortedRegisteredDevices(),
            selectedDevice,
            sharedLatestLocatedEvents,
            sharedPeriodicLocations,
        ) { devices, selected, locatedEvents, periodicLocations ->
            val deviceIds = devices.asSequence().map { it.id }.toSet()
            DashboardFlowInput(
                devices = devices,
                selectedDevice = selected?.takeIf { it.id in deviceIds },
                locationIndex = DeviceLocationIndex(locatedEvents, periodicLocations),
            )
        }.flatMapLatest { input ->
            val eventsFlow = if (input.selectedDevice == null) {
                observeRecentEventsForRegisteredDevices(3, input.devices)
            } else {
                eventDao.observeRecentEventsForDevice(input.selectedDevice.id, 3)
            }
            eventsFlow
                .distinctUntilChanged()
                .map { events ->
                val markers = if (input.selectedDevice == null) {
                    input.devices.mapNotNull { device ->
                        input.locationIndex.resolveForDevice(device)?.toMapMarker(device.deviceName)
                    }
                } else {
                    listOfNotNull(
                        input.locationIndex.resolveForDevice(input.selectedDevice)?.toMapMarker(
                            input.selectedDevice.deviceName,
                        ),
                    )
                }
                DashboardState(
                    devices = input.devices,
                    selectedDevice = input.selectedDevice,
                    recentEvents = buildDashboardRecentEvents(events, input.devices),
                    mapMarkers = markers,
                )
            }
        }
        .distinctUntilChanged { previous, current -> previous.stateSignature() == current.stateSignature() }
        .flowOn(Dispatchers.Default)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMapState(selectedDevice: Flow<Device?>): Flow<MapState> {
        return combine(
            observeSortedRegisteredDevices(),
            selectedDevice,
            sharedLatestLocatedEvents,
            sharedPeriodicLocations,
        ) { devices, selected, locatedEvents, periodicLocations ->
            val sortedDevices = devices
            val locationIndex = DeviceLocationIndex(locatedEvents, periodicLocations)
            sortedDevices to (selected to locationIndex)
        }.map { (devices, data) ->
            val (selected, locationIndex) = data
            val targetDevices = if (selected == null) devices else listOf(selected)
            val markers = ArrayList<DeviceMapMarker>(targetDevices.size)
            val cardTargets = ArrayList<MapLocationCardTarget>(targetDevices.size)
            for (device in targetDevices) {
                val resolved = locationIndex.resolveForDevice(device) ?: continue
                markers.add(resolved.toMapMarker(device.deviceName))
                cardTargets.add(buildMapLocationCardTarget(device, locationIndex, resolved))
            }
            MapState(
                devices = devices,
                selectedDevice = selected,
                markers = markers,
                cardTargets = cardTargets,
            )
        }
        .distinctUntilChanged { previous, current -> previous.stateSignature() == current.stateSignature() }
        .flowOn(Dispatchers.Default)
    }

    suspend fun buildMapLocationCardDisplays(
        targets: List<MapLocationCardTarget>,
        geocodeMissing: Boolean = true,
    ): List<MapLocationCardDisplay> {
        if (targets.isEmpty()) return emptyList()

        val storedAddresses = loadStoredAddresses(targets)
        return targets.map { target ->
            val showEventStatus = !target.isPeriodicBackup && !target.eventType.isNullOrBlank()
            MapLocationCardDisplay(
                deviceName = target.deviceName,
                deviceType = target.deviceType,
                eventStatusLabel = if (showEventStatus) {
                    historyEventStatusLabel(target.eventType!!)
                } else {
                    null
                },
                isDisconnect = showEventStatus && isDisconnectEvent(target.eventType!!),
                timeLabel = DateFormats.koreanTime.format(Date(target.recordedAt)),
                locationLabel = resolveCardLocationLabel(target, geocodeMissing, storedAddresses),
            )
        }
    }

    private suspend fun loadStoredAddresses(targets: List<MapLocationCardTarget>): Map<Long, String> {
        val locationIds = targets.mapNotNull { it.locationId }.distinct()
        return loadStoredAddressesByIds(locationIds)
    }

    private suspend fun resolveCardLocationLabel(
        target: MapLocationCardTarget,
        geocodeMissing: Boolean = true,
        storedAddresses: Map<Long, String> = emptyMap(),
    ): String {
        if (AddressResolver.isUsableRoadAddress(target.storedAddress)) {
            return target.storedAddress
        }
        target.locationId?.let { locationId ->
            storedAddresses[locationId]
                ?.takeIf { AddressResolver.isUsableRoadAddress(it) }
                ?.let { return it }
        }
        if (!geocodeMissing) {
            return formatCoordinateLabel(target.latitude, target.longitude)
        }
        return AddressResolver.resolveRoadAddress(appContext, target.latitude, target.longitude)
            ?: formatCoordinateLabel(target.latitude, target.longitude)
    }

    private fun formatCoordinateLabel(latitude: Double, longitude: Double): String {
        return String.format(Locale.KOREAN, "%.5f, %.5f", latitude, longitude)
    }

    private fun buildMapLocationCardTarget(
        device: Device,
        locationIndex: DeviceLocationIndex,
        resolved: ResolvedDeviceLocation,
    ): MapLocationCardTarget {
        val event = locationIndex.eventForDevice(device.id)
        val periodic = locationIndex.periodicForDevice(device.id)
        return MapLocationCardTarget(
            deviceName = device.deviceName,
            deviceType = device.deviceType,
            recordedAt = resolved.recordedAt,
            latitude = resolved.latitude,
            longitude = resolved.longitude,
            isPeriodicBackup = resolved.isPeriodicBackup,
            eventType = if (resolved.isPeriodicBackup) null else event?.eventType,
            locationId = if (resolved.isPeriodicBackup) null else event?.locationId,
            storedAddress = if (resolved.isPeriodicBackup) periodic?.address.orEmpty() else "",
        )
    }

    suspend fun getConnectedRegisteredDevices(): List<Device> {
        return deviceDao.getConnectedRegisteredDevices()
    }

    suspend fun savePeriodicLocations(devices: List<Device>, location: DeviceLocation) {
        if (devices.isEmpty()) return
        val recordedAt = System.currentTimeMillis()
        val locations = devices.map { device ->
            DeviceLastKnownLocation(
                deviceId = device.id,
                deviceType = device.deviceType,
                latitude = location.latitude,
                longitude = location.longitude,
                address = location.address,
                recordedAt = recordedAt,
            )
        }
        db.withTransaction {
            lastKnownLocationDao.upsertAll(locations)
        }
    }

    suspend fun savePeriodicLocationForDevice(
        deviceId: Long,
        deviceType: String,
        location: DeviceLocation,
    ) {
        lastKnownLocationDao.upsert(
            DeviceLastKnownLocation(
                deviceId = deviceId,
                deviceType = deviceType,
                latitude = location.latitude,
                longitude = location.longitude,
                address = location.address,
                recordedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getSettings(): AppSettings {
        return settingsDao.getSettings() ?: AppSettings().also { settingsDao.insertSettings(it) }
    }

    suspend fun pruneStaleData() {
        val now = System.currentTimeMillis()
        if (now - lastPruneAtMs < DatabaseRetention.PRUNE_INTERVAL_MS) return
        lastPruneAtMs = now
        db.withTransaction {
            val eventCutoff = now - DatabaseRetention.EVENT_RETENTION_MS
            eventDao.deleteEventsOlderThan(eventCutoff)
            latestLocatedEventDao.deleteOlderThan(eventCutoff)
            systemLogDao.deleteLogsOlderThan(now - DatabaseRetention.LOG_RETENTION_MS)
            latestLocatedEventDao.deleteStaleEntries()
            wifiDao.deleteOrphanWifi()
            locationDao.deleteOrphanLocations()
            DatabaseOpenCallback.optimize(db.openHelper.writableDatabase)
        }
    }

    suspend fun getCurrentLocation(): DeviceLocation? {
        return locationService.getCurrentLocation()
    }

    suspend fun saveSettings(settings: AppSettings) {
        settingsDao.insertSettings(settings)
    }

    suspend fun registerDevice(deviceId: Long) {
        val registeredId = db.withTransaction {
            val device = deviceDao.getDeviceById(deviceId) ?: return@withTransaction null
            val duplicate = registeredDeviceIndex().findMatch(device, excludeId = deviceId)
            if (duplicate != null) {
                deviceDao.deleteAvailableDevice(deviceId)
                deviceDao.updateConnectionStatus(duplicate.id, device.isConnected)
                return@withTransaction duplicate.id
            }
            deviceDao.registerDevice(deviceId)
            deviceId
        } ?: return

        invalidateDeviceCaches()
        captureInitialLocationForDevice(registeredId)
    }

    private suspend fun captureInitialLocationForDevice(deviceId: Long) {
        val settings = getSettings()
        if (!settings.locationTrackingEnabled) return

        val device = deviceDao.getDeviceById(deviceId) ?: return
        val location = getCurrentLocation() ?: return
        savePeriodicLocationForDevice(
            deviceId = device.id,
            deviceType = device.deviceType,
            location = location,
        )
    }

    suspend fun unregisterDevice(deviceId: Long) {
        db.withTransaction {
            val locationIds = eventDao.getLocationIdsForDevice(deviceId)
            eventDao.deleteEventsForDevice(deviceId)
            if (locationIds.isNotEmpty()) {
                for (chunk in locationIds.chunked(DatabaseQueryLimits.IN_CLAUSE_CHUNK)) {
                    wifiDao.deleteByLocationIds(chunk)
                    locationDao.deleteLocationsByIds(chunk)
                }
            }
            lastKnownLocationDao.deleteByDeviceId(deviceId)
            latestLocatedEventDao.deleteByDeviceId(deviceId)
            systemLogDao.deleteLogsForDeviceId(deviceId)
            deviceDao.deleteDevice(deviceId)
        }
        invalidateDeviceCaches()
    }

    suspend fun clearAvailableDevices() = deviceDao.deleteAllAvailableDevices()

    suspend fun hasRegisteredDevicesForSource(source: String): Boolean {
        return deviceDao.hasRegisteredDevicesForSource(source)
    }

    suspend fun resolveDeviceId(identifier: String?, source: String): Long? {
        return findRegisteredDevice(source, identifier)?.id
    }

    suspend fun getDeviceById(deviceId: Long): Device? = deviceDao.getDeviceById(deviceId)

    suspend fun getRegisteredDeviceType(deviceId: Long): String? {
        return registeredDeviceIndex().findById(deviceId)?.deviceType
    }

    suspend fun syncRegisteredBluetoothConnectionStatuses(): Int {
        return collectBluetoothConnectionChanges().size
    }

    suspend fun detectBluetoothConnectionStatusChanges(): List<BluetoothConnectionChange> {
        return collectBluetoothConnectionChanges()
    }

    private suspend fun collectBluetoothConnectionChanges(): List<BluetoothConnectionChange> {
        val scanned = cachedBluetoothScan(forceRefresh = true)
        val scannedIndex = ScannedDeviceIndex(scanned)
        val registeredDevices = deviceDao.getRegisteredDevicesBySource(EventSources.BLUETOOTH)
        val changes = ArrayList<BluetoothConnectionChange>()
        val statusById = HashMap<Long, Boolean>()
        for (registered in registeredDevices) {
            val connected = scannedIndex.connectionStateFor(registered)
            if (registered.isConnected != connected) {
                statusById[registered.id] = connected
                changes.add(
                    BluetoothConnectionChange(
                        deviceId = registered.id,
                        deviceType = registered.deviceType,
                        connected = connected,
                    ),
                )
            }
        }
        if (statusById.isNotEmpty()) {
            db.withTransaction {
                applyConnectionStatusBatch(registeredDevices, statusById)
            }
            invalidateRegisteredDeviceCaches()
        }
        return changes
    }

    suspend fun scanNearbyDevices(): Int {
        val scanned = deviceScanner.scanAll()
        cacheBluetoothScanResult(scanned)
        return db.withTransaction {
        val registeredDevices = deviceDao.getRegisteredDevices()
        val availableDevices = deviceDao.getAvailableDevices()
        val deviceLookupIndex = RegisteredDeviceIndex(registeredDevices + availableDevices)
        val scannedIndex = ScannedDeviceIndex(scanned)
        val devicesToUpsert = ArrayList<Device>(scanned.size)

        for (scannedDevice in scanned) {
            val normalized = normalizeScannedDevice(scannedDevice.device)
            val existing = deviceLookupIndex.findMatch(normalized)

            val deviceToSave = when {
                existing == null -> normalized.copy(
                    isConnected = scannedDevice.isConnected,
                    isRegistered = false,
                )
                existing.isRegistered -> existing.copy(
                    macAddress = normalized.macAddress,
                    deviceName = normalized.deviceName,
                    deviceType = normalized.deviceType,
                    isConnected = scannedDevice.isConnected,
                    description = normalized.description,
                    isRegistered = true,
                )
                else -> existing.copy(
                    macAddress = normalized.macAddress,
                    deviceName = normalized.deviceName,
                    deviceType = normalized.deviceType,
                    isConnected = scannedDevice.isConnected,
                    description = normalized.description,
                    isRegistered = false,
                )
            }
            devicesToUpsert.add(deviceToSave)
        }

        if (devicesToUpsert.isNotEmpty()) {
            deviceDao.insertDevices(devicesToUpsert)
        }

        val staleAvailableIds = availableDevices
            .asSequence()
            .filter { !scannedIndex.contains(it) }
            .map { it.id }
            .toList()
        if (staleAvailableIds.isNotEmpty()) {
            deviceDao.deleteAvailableDevicesByIds(staleAvailableIds)
        }

        val connectionStatusById = HashMap<Long, Boolean>()
        for (registered in registeredDevices) {
            val connected = scannedIndex.connectionStateFor(registered)
            if (registered.isConnected != connected) {
                connectionStatusById[registered.id] = connected
            }
        }
        applyConnectionStatusBatch(registeredDevices, connectionStatusById)

        scanned.size
        }.also { invalidateRegisteredDeviceCaches() }
    }

    private fun normalizeScannedDevice(device: Device): Device {
        return device.copy(
            macAddress = when (device.eventSource) {
                EventSources.BLUETOOTH -> device.macAddress.uppercase()
                EventSources.USB -> DeviceIdentifier.stableId(device)
                else -> device.macAddress
            },
        )
    }

    private suspend fun findRegisteredDevice(source: String, identifier: String?): Device? {
        val normalized = EventIntentParser.normalizeIdentifier(source, identifier) ?: return null
        deviceDao.findRegisteredDeviceByIdentifier(source, normalized)?.let { return it }
        if (source == EventSources.BLUETOOTH) {
            deviceDao.findRegisteredDeviceByMac(normalized)?.let { return it }
        }
        return registeredDeviceIndex().findByIdentifier(source, normalized)
    }

    suspend fun recordConnectEvent(
        event: DeviceConnectionEvent,
        location: DeviceLocation?,
        logMessage: String,
    ): Long = db.withTransaction {
        val eventId = eventDao.insertEvent(event)
        if (location != null) {
            val locationId = locationDao.insertLocation(location)
            eventDao.updateEventLocation(
                eventId = eventId,
                locationId = locationId,
                latitude = location.latitude,
                longitude = location.longitude,
            )
            event.deviceId?.let { deviceId ->
                upsertLocatedEventCache(deviceId, eventId, event.eventTime)
            }
        }
        event.deviceId?.let { deviceDao.updateConnectionStatus(it, connected = true) }
        systemLogDao.insertLog(
            SystemLog.saveLog(
                message = logMessage,
                eventId = eventId,
                deviceId = event.deviceId,
                logType = "CONNECT",
            ),
        )
        eventId
    }

    suspend fun recordDisconnectEvent(
        event: DeviceConnectionEvent,
        location: DeviceLocation?,
        wifiProvider: (suspend (Long) -> WifiInformation?)?,
        logMessage: String,
        logType: String,
        errorLog: String? = null,
    ): Long = db.withTransaction {
        val eventId = eventDao.insertEvent(event)
        if (location != null) {
            val locationId = locationDao.insertLocation(location)
            eventDao.updateEventLocation(
                eventId = eventId,
                locationId = locationId,
                latitude = location.latitude,
                longitude = location.longitude,
            )
            event.deviceId?.let { deviceId ->
                upsertLocatedEventCache(deviceId, eventId, event.eventTime)
            }
            wifiProvider?.invoke(locationId)?.let { wifiDao.insertWifiInfo(it) }
        }
        errorLog?.let {
            systemLogDao.insertLog(
                SystemLog.saveLog(message = it, eventId = eventId, deviceId = event.deviceId, logType = "ERROR"),
            )
        }
        systemLogDao.insertLog(
            SystemLog.saveLog(
                message = logMessage,
                eventId = eventId,
                deviceId = event.deviceId,
                logType = logType,
            ),
        )
        event.deviceId?.let { deviceDao.updateConnectionStatus(it, connected = false) }
        eventId
    }

    suspend fun saveLog(
        message: String,
        eventId: Long? = null,
        deviceId: Long? = null,
        logType: String = "EVENT",
    ): Long {
        return systemLogDao.insertLog(
            SystemLog.saveLog(message = message, eventId = eventId, deviceId = deviceId, logType = logType),
        )
    }

    fun mapEventSource(deviceType: String): String = EventSources.fromDeviceType(deviceType)

    suspend fun buildHistoryTimeline(
        events: List<DeviceConnectionEvent>,
        devices: List<Device>,
    ): List<HistoryTimelineSection> {
        return HistoryTimelineCache.getOrBuild(events, devices) {
            val storedAddresses = loadStoredAddressesForEvents(events)
            val locationLabels = EventLocationLabelResolver.resolveLabels(appContext, events, storedAddresses)
            HistoryTimelineBuilder.buildSections(events, devices, locationLabels)
        }
    }

    private suspend fun loadStoredAddressesForEvents(
        events: List<DeviceConnectionEvent>,
    ): Map<Long, String> {
        val locationIds = events.mapNotNull { it.locationId }.distinct()
        return loadStoredAddressesByIds(locationIds)
    }

    private suspend fun loadStoredAddressesByIds(locationIds: List<Long>): Map<Long, String> {
        if (locationIds.isEmpty()) return emptyMap()
        val addresses = HashMap<Long, String>(locationIds.size)
        for (chunk in locationIds.chunked(DatabaseQueryLimits.IN_CLAUSE_CHUNK)) {
            for (location in locationDao.getLocationsByIds(chunk)) {
                addresses[location.id] = location.address
            }
        }
        return addresses
    }

    fun buildDashboardRecentEvents(
        events: List<DeviceConnectionEvent>,
        devices: List<Device>,
    ): List<DashboardRecentEventItem> {
        val deviceById = devices.associateBy { it.id }
        return events.map { event ->
            val device = event.deviceId?.let { deviceById[it] }
            DashboardRecentEventItem(
                timeLabel = DateFormats.koreanTime.format(Date(event.eventTime)),
                deviceName = device?.deviceName ?: event.deviceType,
                deviceType = device?.deviceType ?: event.deviceType,
                eventStatusLabel = historyEventStatusLabel(event.eventType),
                isDisconnect = isDisconnectEvent(event.eventType),
            )
        }
    }

    private data class DashboardFlowInput(
        val devices: List<Device>,
        val selectedDevice: Device?,
        val locationIndex: DeviceLocationIndex,
    )

    companion object {
        private const val BLUETOOTH_SCAN_TTL_MS = 2_000L
    }
}
