package com.last.app.infrastructure.platform

import android.Manifest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionHelperTest {

    @Test
    fun requiredPermissions_includeLocationOnAllApiLevels() {
        val permissions = PermissionHelper.requiredRuntimePermissions(sdkInt = 26)
        assertTrue(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    @Test
    fun requiredPermissions_includeBluetoothOnApi31() {
        val permissions = PermissionHelper.requiredRuntimePermissions(sdkInt = 31)
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
        assertTrue(permissions.contains(Manifest.permission.BLUETOOTH_SCAN))
    }

    @Test
    fun requiredPermissions_excludeBluetoothOnApi26() {
        val permissions = PermissionHelper.requiredRuntimePermissions(sdkInt = 26)
        assertFalse(permissions.contains(Manifest.permission.BLUETOOTH_CONNECT))
    }
}
