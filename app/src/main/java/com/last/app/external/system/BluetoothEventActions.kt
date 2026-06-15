package com.last.app.external.system

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile

object BluetoothEventActions {
    val ACL_CONNECTED = BluetoothDevice.ACTION_ACL_CONNECTED
    val ACL_DISCONNECTED = BluetoothDevice.ACTION_ACL_DISCONNECTED
    val A2DP_CONNECTION_STATE_CHANGED = BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED
    val HEADSET_CONNECTION_STATE_CHANGED = BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED
    val HID_CONNECTION_STATE_CHANGED = BluetoothHidDevice.ACTION_CONNECTION_STATE_CHANGED
    const val INPUT_DEVICE_CONNECTION_STATE_CHANGED =
        "android.bluetooth.InputDevice.ACTION_CONNECTION_STATE_CHANGED"

    val ALL = listOf(
        ACL_CONNECTED,
        ACL_DISCONNECTED,
        A2DP_CONNECTION_STATE_CHANGED,
        HEADSET_CONNECTION_STATE_CHANGED,
        HID_CONNECTION_STATE_CHANGED,
        INPUT_DEVICE_CONNECTION_STATE_CHANGED,
    )

    fun isBluetoothAction(action: String): Boolean = action in ALL
}
