package com.last.app.external.device

import android.hardware.usb.UsbDevice
import com.last.app.data.entity.Device
import com.last.app.data.entity.EventSources

object DeviceIdentifier {

    fun usbStableId(usb: UsbDevice): String = "${usb.vendorId}:${usb.productId}"

    fun stableId(device: Device): String = when (device.eventSource) {
        EventSources.BLUETOOTH -> device.macAddress.uppercase()
        EventSources.USB -> usbStableIdFromMac(device.macAddress)
        else -> device.macAddress
    }

    fun usbStableIdFromMac(macAddress: String): String {
        val parts = macAddress.split(":")
        return if (parts.size >= 2) "${parts[0]}:${parts[1]}" else macAddress
    }

    fun matches(candidate: Device, existing: Device): Boolean {
        if (candidate.eventSource != existing.eventSource) return false
        return when (candidate.eventSource) {
            EventSources.BLUETOOTH -> {
                candidate.macAddress.equals(existing.macAddress, ignoreCase = true) ||
                    candidate.deviceName == existing.deviceName
            }
            EventSources.USB -> {
                usbStableIdFromMac(candidate.macAddress) == usbStableIdFromMac(existing.macAddress) ||
                    candidate.macAddress == existing.macAddress ||
                    candidate.deviceName == existing.deviceName
            }
            EventSources.POWER -> candidate.macAddress == existing.macAddress
            else -> candidate.macAddress == existing.macAddress || candidate.deviceName == existing.deviceName
        }
    }

    fun matchesIdentifier(source: String, identifier: String?, device: Device): Boolean {
        if (identifier.isNullOrBlank()) return false
        return when (source) {
            EventSources.BLUETOOTH -> {
                device.macAddress.equals(identifier, ignoreCase = true) ||
                    device.deviceName == identifier
            }
            EventSources.USB -> {
                usbStableIdFromMac(identifier) == stableId(device) ||
                    device.macAddress == identifier ||
                    device.deviceName == identifier
            }
            else -> device.macAddress == identifier || device.deviceName == identifier
        }
    }
}
