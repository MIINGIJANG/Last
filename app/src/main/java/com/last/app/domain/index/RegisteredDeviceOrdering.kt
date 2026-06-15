package com.last.app.domain.index

import com.last.app.data.entity.Device
import com.last.app.data.entity.EventSources

object RegisteredDeviceOrdering {

    data class GroupedDevices(
        val bluetooth: List<Device>,
        val usb: List<Device>,
        val power: List<Device>,
    )

    fun sort(devices: List<Device>): List<Device> {
        return devices.sortedWith(deviceComparator)
    }

    fun groupByCategory(devices: List<Device>): GroupedDevices {
        val bluetooth = ArrayList<Device>()
        val usb = ArrayList<Device>()
        val power = ArrayList<Device>()
        for (device in devices) {
            when (categoryOrder(device.eventSource)) {
                0 -> bluetooth.add(device)
                1 -> usb.add(device)
                2 -> power.add(device)
            }
        }
        bluetooth.sortWith(nameComparator)
        usb.sortWith(nameComparator)
        power.sortWith(nameComparator)
        return GroupedDevices(
            bluetooth = bluetooth,
            usb = usb,
            power = power,
        )
    }

    private val deviceComparator = compareBy<Device>(
        { categoryOrder(it.eventSource) },
        { it.deviceName },
    )

    private val nameComparator = compareBy<Device> { it.deviceName }

    private fun categoryOrder(source: String): Int {
        return when (source.uppercase()) {
            EventSources.BLUETOOTH -> 0
            EventSources.USB -> 1
            EventSources.POWER -> 2
            else -> 3
        }
    }
}
