package com.last.app.external.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AddressResolver {

    private const val MAX_CACHE_SIZE = 256
    private val addressCache = object : LinkedHashMap<String, CachedAddress>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedAddress>): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }
    private val cacheLock = Any()

    private sealed class CachedAddress {
        data class Resolved(val value: String) : CachedAddress()
        data object Unresolved : CachedAddress()
    }

    fun isUsableRoadAddress(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val trimmed = value.trim()
        if (trimmed.length <= 2) return false
        if (trimmed.all { it.isDigit() }) return false
        return true
    }

    suspend fun resolveRoadAddress(
        context: Context,
        latitude: Double,
        longitude: Double,
    ): String? {
        val cacheKey = coordinateKey(latitude, longitude)
        synchronized(cacheLock) {
            addressCache[cacheKey]?.let { cached ->
                return when (cached) {
                    is CachedAddress.Resolved -> cached.value
                    CachedAddress.Unresolved -> null
                }
            }
        }

        if (!Geocoder.isPresent()) return null
        val resolved = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCoroutine { continuation ->
                    Geocoder(context, Locale.KOREAN).getFromLocation(
                        latitude,
                        longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                continuation.resume(formatKoreanRoadAddress(addresses.firstOrNull()))
                            }

                            override fun onError(errorMessage: String?) {
                                continuation.resume(null)
                            }
                        },
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale.KOREAN).getFromLocation(latitude, longitude, 1)
                formatKoreanRoadAddress(addresses?.firstOrNull())
            }
        } catch (_: Exception) {
            null
        }

        synchronized(cacheLock) {
            addressCache[cacheKey] = if (isUsableRoadAddress(resolved)) {
                CachedAddress.Resolved(resolved!!)
            } else {
                CachedAddress.Unresolved
            }
        }
        return resolved?.takeIf { isUsableRoadAddress(it) }
    }

    fun formatKoreanRoadAddress(address: Address?): String? {
        if (address == null) return null

        val roadName = address.thoroughfare?.trim()?.takeIf { it.isNotBlank() }
        val buildingNumber = address.subThoroughfare?.trim()?.takeIf { it.isNotBlank() }
        val road = when {
            roadName != null && buildingNumber != null -> "$roadName $buildingNumber"
            roadName != null -> roadName
            else -> null
        }
        if (isUsableRoadAddress(road)) return road

        val featureName = address.featureName?.trim()?.takeIf { it.isNotBlank() }
        if (isUsableRoadAddress(featureName)) return featureName

        val addressLine = address.getAddressLine(0)?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return simplifyKoreanAddressLine(addressLine).takeIf { isUsableRoadAddress(it) }
    }

    private fun simplifyKoreanAddressLine(line: String): String {
        var result = line
        for (prefix in listOf("대한민국 ", "South Korea ")) {
            if (result.startsWith(prefix)) {
                result = result.removePrefix(prefix)
            }
        }
        result = result.replace(Regex("^\\d{5}\\s+"), "")
        return result.trim()
    }

    private fun coordinateKey(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "%.4f,%.4f", latitude, longitude)
    }
}
