package com.last.app.external.system

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.last.app.data.entity.EventSources
import com.last.app.data.entity.EventType
import com.last.app.external.system.BluetoothEventActions
import com.last.app.external.device.DeviceIdentifier
import com.last.app.external.device.DeviceScanner

object EventIntentParser {

    fun extractDeviceIdentifier(context: Context?, intent: Intent, action: String): String? {
        return when {
            BluetoothEventActions.isBluetoothAction(action) -> {
                context?.let { extractBluetoothMac(it, intent) }
            }
            action == Intent.ACTION_POWER_CONNECTED ||
                action == Intent.ACTION_POWER_DISCONNECTED -> {
                DeviceScanner.POWER_DEVICE_ID
            }
            action == "android.hardware.usb.action.USB_DEVICE_ATTACHED" ||
                action == "android.hardware.usb.action.USB_DEVICE_DETACHED" -> {
                extractUsbIdentifier(intent)
            }
            else -> null
        }
    }

    fun parseBluetoothConnectionEvent(action: String, state: Int): EventType? {
        return when (action) {
            BluetoothEventActions.ACL_CONNECTED -> EventType.CONNECT
            BluetoothEventActions.ACL_DISCONNECTED -> EventType.DISCONNECT
            BluetoothEventActions.A2DP_CONNECTION_STATE_CHANGED,
            BluetoothEventActions.HEADSET_CONNECTION_STATE_CHANGED,
            BluetoothEventActions.HID_CONNECTION_STATE_CHANGED,
            BluetoothEventActions.INPUT_DEVICE_CONNECTION_STATE_CHANGED,
            -> when (state) {
                BluetoothProfile.STATE_CONNECTED -> EventType.CONNECT
                BluetoothProfile.STATE_DISCONNECTED -> EventType.DISCONNECT
                else -> null
            }
            else -> null
        }
    }

    fun isBluetoothProfileAction(action: String): Boolean {
        return action == BluetoothEventActions.A2DP_CONNECTION_STATE_CHANGED ||
            action == BluetoothEventActions.HEADSET_CONNECTION_STATE_CHANGED ||
            action == BluetoothEventActions.HID_CONNECTION_STATE_CHANGED ||
            action == BluetoothEventActions.INPUT_DEVICE_CONNECTION_STATE_CHANGED
    }

    private fun extractUsbIdentifier(intent: Intent): String? {
        @Suppress("DEPRECATION")
        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
        return device?.let { usb -> DeviceIdentifier.usbStableId(usb) }
    }

    private fun extractBluetoothMac(context: Context, intent: Intent): String? {
        if (!hasBluetoothConnectPermission(context)) return null

        @Suppress("DEPRECATION")
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        return normalizeBluetoothMac(device?.address)
    }

    fun normalizeBluetoothMac(address: String?): String? {
        return address?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
    }

    fun normalizeIdentifier(source: String, identifier: String?): String? {
        if (identifier.isNullOrBlank()) return null
        return if (source == EventSources.BLUETOOTH) {
            normalizeBluetoothMac(identifier) ?: identifier.trim()
        } else {
            identifier.trim()
        }
    }

    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
