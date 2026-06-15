package com.last.app.domain.index

import com.last.app.data.entity.Device
import com.last.app.data.entity.EventSources
import com.last.app.external.device.DeviceIdentifier

class RegisteredDeviceIndex(
    devices: List<Device>,
) {
    private val byStableId = HashMap<String, Device>(devices.size)
    private val byId = HashMap<Long, Device>(devices.size)
    private val bySourceAndMac = HashMap<Pair<String, String>, Device>(devices.size)
    private val bySourceAndName = HashMap<Pair<String, String>, Device>(devices.size)

    init {
        for (device in devices) {
            byId[device.id] = device
            byStableId.putIfAbsent(DeviceIdentifier.stableId(device), device)
            if (device.macAddress.isNotBlank()) {
                val macKey = device.eventSource to normalizedMac(device)
                bySourceAndMac.putIfAbsent(macKey, device)
            }
            bySourceAndName.putIfAbsent(device.eventSource to device.deviceName, device)
        }
    }

    fun findById(deviceId: Long): Device? = byId[deviceId]

    fun findByIdentifier(source: String, identifier: String): Device? {
        val normalized = normalizeIdentifier(source, identifier)
        bySourceAndMac[source to normalized]?.let { return it }
        bySourceAndName[source to normalized]?.let { return it }
        return null
    }

    fun findMatch(candidate: Device, excludeId: Long? = null): Device? {
        byStableId[DeviceIdentifier.stableId(candidate)]?.let { device ->
            if (device.id != excludeId) return device
        }
        if (candidate.macAddress.isNotBlank()) {
            bySourceAndMac[candidate.eventSource to normalizedMac(candidate)]?.let { device ->
                if (device.id != excludeId && DeviceIdentifier.matches(candidate, device)) return device
            }
        }
        bySourceAndName[candidate.eventSource to candidate.deviceName]?.let { device ->
            if (device.id != excludeId && DeviceIdentifier.matches(candidate, device)) return device
        }
        return null
    }

    private fun normalizedMac(device: Device): String {
        return if (device.eventSource == EventSources.BLUETOOTH) {
            device.macAddress.uppercase()
        } else {
            device.macAddress
        }
    }

    private fun normalizeIdentifier(source: String, identifier: String): String {
        return if (source == EventSources.BLUETOOTH) identifier.uppercase() else identifier
    }
}
