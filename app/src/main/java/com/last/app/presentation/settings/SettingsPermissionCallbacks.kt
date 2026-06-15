package com.last.app.presentation.settings

interface SettingsPermissionCallbacks {
    fun requestLocationPermission(onGranted: () -> Unit)
    fun requestBluetoothPermission(onGranted: () -> Unit)
}
