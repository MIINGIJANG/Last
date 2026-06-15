package com.last.app.external.location

import com.last.app.data.entity.DeviceLocation
import com.last.app.data.entity.WifiInformation

interface LocationService {
    fun hasLocationPermission(): Boolean
    suspend fun requestPermission(): Boolean
    suspend fun getCurrentLocation(): DeviceLocation?
    suspend fun collectWifiInformation(locationId: Long): WifiInformation?
}
