package com.last.app.external.system

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.last.app.data.entity.EventType
import com.last.app.external.system.BluetoothEventActions

class AndroidSystemEventListener(
    private val listener: SystemEventListener,
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action ?: return
        val timestamp = System.currentTimeMillis()
        val deviceId = EventIntentParser.extractDeviceIdentifier(context, intent, action)

        val eventType = when {
            EventIntentParser.isBluetoothProfileAction(action) -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                EventIntentParser.parseBluetoothConnectionEvent(action, state)
            }
            else -> parseLegacyEventType(action)
        } ?: return

        val deviceType = when (action) {
            BluetoothEventActions.ACL_CONNECTED,
            BluetoothEventActions.ACL_DISCONNECTED,
            BluetoothEventActions.A2DP_CONNECTION_STATE_CHANGED,
            BluetoothEventActions.HEADSET_CONNECTION_STATE_CHANGED,
            BluetoothEventActions.HID_CONNECTION_STATE_CHANGED,
            BluetoothEventActions.INPUT_DEVICE_CONNECTION_STATE_CHANGED,
            -> "BLUETOOTH"

            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            -> "POWER"

            else -> "USB"
        }

        listener.onSystemEvent(deviceType, eventType, timestamp, deviceId)
    }

    private fun parseLegacyEventType(action: String): EventType? {
        return when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED,
            Intent.ACTION_POWER_CONNECTED,
            "android.hardware.usb.action.USB_DEVICE_ATTACHED",
            -> EventType.CONNECT

            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            "android.hardware.usb.action.USB_DEVICE_DETACHED",
            -> EventType.DISCONNECT

            else -> null
        }
    }
}
