package com.last.app.domain.index

import com.last.app.domain.model.device.ScannedDevice
import com.last.app.data.entity.Device
import com.last.app.data.entity.EventSources
import com.last.app.external.device.DeviceIdentifier

class ScannedDeviceIndex(
    scanned: List<ScannedDevice>,
) {
    private val byStableId = HashMap<String, ScannedDevice>(scanned.size)
    private val bySourceAndMac = HashMap<Pair<String, String>, ScannedDevice>(scanned.size)
    private val bySourceAndName = HashMap<Pair<String, String>, ScannedDevice>(scanned.size)

    init {
        for (item in scanned) {
            val device = item.device
            byStableId.putIfAbsent(DeviceIdentifier.stableId(device), item)
            if (device.macAddress.isNotBlank()) {
                bySourceAndMac.putIfAbsent(device.eventSource to normalizedMac(device), item)
            }
            bySourceAndName.putIfAbsent(device.eventSource to device.deviceName, item)
        }
    }

    fun connectionStateFor(registered: Device): Boolean {
        findScanned(registered)?.let { return it.isConnected }
        return false
    }

    fun contains(device: Device): Boolean = findScanned(device) != null

    private fun findScanned(device: Device): ScannedDevice? {
        byStableId[DeviceIdentifier.stableId(device)]?.let { return it }
        if (device.macAddress.isNotBlank()) {
            bySourceAndMac[device.eventSource to normalizedMac(device)]?.let { return it }
        }
        bySourceAndName[device.eventSource to device.deviceName]?.let { item ->
            if (DeviceIdentifier.matches(device, item.device)) return item
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
}
