package com.last.app.external.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.BatteryManager
import android.os.Build
import com.last.app.domain.model.device.ScannedDevice
import com.last.app.data.entity.Device
import com.last.app.data.entity.EventSources
import com.last.app.infrastructure.platform.PermissionHelper

class DeviceScanner(
    private val context: Context,
) {

    companion object {
        const val POWER_DEVICE_ID = "power:charging"
        private const val BLUETOOTH_INPUT_DEVICE_PROFILE = 4
    }

    fun scanAll(): List<ScannedDevice> {
        return scanBluetoothDevices() + scanUsbDevices() + scanPowerDevices()
    }

    @SuppressLint("MissingPermission")
    fun scanBluetoothDevices(): List<ScannedDevice> {
        if (!PermissionHelper.hasBluetoothPermissions(context)) return emptyList()

        val manager = context.getSystemService(BluetoothManager::class.java) ?: return emptyList()
        val adapter = manager.adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        val connectedAddresses = connectedBluetoothAddresses(manager)
        val discovered = linkedMapOf<String, BluetoothDevice>()

        runCatching {
            adapter.bondedDevices.orEmpty().forEach { device ->
                discovered[device.address.uppercase()] = device
            }
        }

        connectedAddresses.forEach { address ->
            runCatching {
                adapter.getRemoteDevice(address)?.let { device ->
                    discovered[device.address.uppercase()] = device
                }
            }
        }

        return discovered.values.map { btDevice ->
            val connected = isBluetoothDeviceConnected(btDevice, connectedAddresses)
            val macAddress = btDevice.address.uppercase()
            val name = btDevice.name?.takeIf { it.isNotBlank() } ?: "Bluetooth ${macAddress.takeLast(5)}"
            ScannedDevice(
                device = Device(
                    deviceName = name,
                    deviceType = inferBluetoothDeviceType(name),
                    macAddress = macAddress,
                    isConnected = connected,
                    description = "",
                    eventSource = EventSources.BLUETOOTH,
                    isRegistered = false,
                ),
                isConnected = connected,
            )
        }
    }

    fun scanUsbDevices(): List<ScannedDevice> {
        val usbManager = context.getSystemService(UsbManager::class.java) ?: return emptyList()
        return usbManager.deviceList.values.map { usb ->
            val identifier = DeviceIdentifier.usbStableId(usb)
            val name = usb.productName?.takeIf { it.isNotBlank() }
                ?: usb.deviceName?.takeIf { it.isNotBlank() }
                ?: "USB ${usb.vendorId}:${usb.productId}"
            ScannedDevice(
                device = Device(
                    deviceName = name,
                    deviceType = "USB",
                    macAddress = identifier,
                    isConnected = true,
                    description = "",
                    eventSource = EventSources.USB,
                    isRegistered = false,
                ),
                isConnected = true,
            )
        }
    }

    fun scanPowerDevices(): List<ScannedDevice> {
        if (!isPowerConnected()) return emptyList()
        return listOf(
            ScannedDevice(
                device = Device(
                    deviceName = "충전기",
                    deviceType = EventSources.POWER,
                    macAddress = POWER_DEVICE_ID,
                    isConnected = true,
                    description = "",
                    eventSource = EventSources.POWER,
                    isRegistered = false,
                ),
                isConnected = true,
            ),
        )
    }

    private fun isPowerConnected(): Boolean {
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        if (batteryManager?.isCharging == true) return true

        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        return plugged != 0
    }

    @SuppressLint("MissingPermission")
    private fun connectedBluetoothAddresses(manager: BluetoothManager): Set<String> {
        val addresses = mutableSetOf<String>()
        val profiles = buildList {
            add(BluetoothProfile.A2DP)
            add(BluetoothProfile.HEADSET)
            add(BluetoothProfile.GATT)
            add(BLUETOOTH_INPUT_DEVICE_PROFILE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(BluetoothProfile.HID_DEVICE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(BluetoothProfile.LE_AUDIO)
            }
        }
        for (profile in profiles) {
            runCatching {
                manager.getConnectedDevices(profile)?.forEach { device ->
                    addresses.add(device.address.uppercase())
                }
            }
        }
        return addresses
    }

    private fun isBluetoothDeviceConnected(device: BluetoothDevice, connectedAddresses: Set<String>): Boolean {
        val address = device.address.uppercase()
        if (connectedAddresses.contains(address)) return true
        return runCatching {
            val method = device.javaClass.getMethod("isConnected")
            method.invoke(device) as Boolean
        }.getOrDefault(false)
    }

    private fun inferBluetoothDeviceType(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("keyboard") || lower.contains("키보드") -> "KEYBOARD"
            lower.contains("mouse") || lower.contains("마우스") -> "MOUSE"
            lower.contains("bud") || lower.contains("headphone") || lower.contains("이어폰") -> "HEADPHONES"
            else -> "BLUETOOTH"
        }
    }
}
