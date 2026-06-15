package com.last.app.domain.index

import com.last.app.domain.model.location.ResolvedDeviceLocation
import com.last.app.data.entity.Device
import com.last.app.data.entity.DeviceConnectionEvent
import com.last.app.data.entity.DeviceLastKnownLocation

class DeviceLocationIndex(
    locatedEvents: List<DeviceConnectionEvent>,
    periodicLocations: List<DeviceLastKnownLocation>,
) {
    private val eventsByDeviceId = locatedEvents
        .asSequence()
        .filter { it.deviceId != null }
        .associateBy { it.deviceId!! }
    private val periodicByDeviceId = periodicLocations.associateBy { it.deviceId }

    fun resolveForDevice(device: Device): ResolvedDeviceLocation? {
        return resolveForDeviceId(device.id, device.deviceType)
    }

    fun resolveForDeviceId(deviceId: Long, deviceType: String): ResolvedDeviceLocation? {
        val event = eventsByDeviceId[deviceId]
        val periodic = periodicByDeviceId[deviceId]
        val eventHasCoords = event?.latitude != null && event.longitude != null

        return when {
            eventHasCoords && periodic != null -> {
                if (event.eventTime >= periodic.recordedAt) {
                    event.toResolvedDeviceLocation(isPeriodicBackup = false)
                } else {
                    periodic.toResolvedDeviceLocation(deviceType)
                }
            }
            eventHasCoords -> event.toResolvedDeviceLocation(isPeriodicBackup = false)
            periodic != null -> periodic.toResolvedDeviceLocation(deviceType)
            else -> null
        }
    }

    fun eventForDevice(deviceId: Long): DeviceConnectionEvent? = eventsByDeviceId[deviceId]

    fun periodicForDevice(deviceId: Long): DeviceLastKnownLocation? = periodicByDeviceId[deviceId]

    private fun DeviceConnectionEvent.toResolvedDeviceLocation(
        isPeriodicBackup: Boolean,
    ): ResolvedDeviceLocation? {
        val lat = latitude ?: return null
        val lng = longitude ?: return null
        return ResolvedDeviceLocation(
            deviceId = deviceId,
            deviceType = deviceType,
            latitude = lat,
            longitude = lng,
            recordedAt = eventTime,
            isPeriodicBackup = isPeriodicBackup,
        )
    }

    private fun DeviceLastKnownLocation.toResolvedDeviceLocation(
        deviceType: String,
    ): ResolvedDeviceLocation {
        return ResolvedDeviceLocation(
            deviceId = deviceId,
            deviceType = deviceType,
            latitude = latitude,
            longitude = longitude,
            recordedAt = recordedAt,
            isPeriodicBackup = true,
        )
    }
}
