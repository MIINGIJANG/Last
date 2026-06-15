package com.last.app.external.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.last.app.data.entity.DeviceLocation
import com.last.app.data.entity.WifiInformation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FusedLocationService(
    private val context: Context,
) : LocationService {

    override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun requestPermission(): Boolean = hasLocationPermission()

    override suspend fun getCurrentLocation(): DeviceLocation? {
        if (!hasLocationPermission()) return null

        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = requestFreshLocation(client) ?: requestLastLocation(client) ?: return null
        val address = AddressResolver.resolveRoadAddress(context, location.latitude, location.longitude).orEmpty()
        return location.copy(address = address)
    }

    private suspend fun requestFreshLocation(
        client: com.google.android.gms.location.FusedLocationProviderClient,
    ): DeviceLocation? = suspendCancellableCoroutine { continuation ->
        try {
            val token = CancellationTokenSource().token
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(location.toDeviceLocation())
                    } else {
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { continuation.resume(null) }
        } catch (_: SecurityException) {
            continuation.resume(null)
        }
    }

    private suspend fun requestLastLocation(
        client: com.google.android.gms.location.FusedLocationProviderClient,
    ): DeviceLocation? = suspendCancellableCoroutine { continuation ->
        try {
            client.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location?.toDeviceLocation())
                }
                .addOnFailureListener { continuation.resume(null) }
        } catch (_: SecurityException) {
            continuation.resume(null)
        }
    }

    private fun android.location.Location.toDeviceLocation(): DeviceLocation {
        return DeviceLocation(
            latitude = latitude,
            longitude = longitude,
            address = "",
            timestamp = System.currentTimeMillis(),
        )
    }

    override suspend fun collectWifiInformation(locationId: Long): WifiInformation? {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo ?: return null
            WifiInformation(
                locationId = locationId,
                ssid = info.ssid?.replace("\"", "") ?: "Unknown",
                bssid = info.bssid ?: "Unknown",
                signalStrength = info.rssi,
                timestamp = System.currentTimeMillis(),
            )
        } catch (_: Exception) {
            null
        }
    }
}
