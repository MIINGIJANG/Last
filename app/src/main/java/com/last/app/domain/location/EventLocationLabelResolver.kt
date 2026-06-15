package com.last.app.domain.location

import android.content.Context
import com.last.app.data.entity.DeviceConnectionEvent
import com.last.app.external.location.AddressResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object EventLocationLabelResolver {

    private const val GEOCODE_CONCURRENCY = 4

    suspend fun resolveLabels(
        context: Context,
        events: List<DeviceConnectionEvent>,
        storedAddresses: Map<Long, String>,
    ): Map<Long, String> {
        if (events.isEmpty()) return emptyMap()

        val labels = HashMap<Long, String>(events.size)
        val pendingCoordinates = LinkedHashMap<CoordinateKey, MutableList<Long>>()

        for (event in events) {
            val lat = event.latitude
            val lng = event.longitude
            if (lat == null || lng == null) {
                labels[event.eventId] = "위치 정보 없음"
                continue
            }

            var resolved = false
            event.locationId?.let { locationId ->
                storedAddresses[locationId]
                    ?.takeIf { AddressResolver.isUsableRoadAddress(it) }
                    ?.let {
                        labels[event.eventId] = it
                        resolved = true
                    }
            }
            if (resolved) continue

            val coordinateKey = CoordinateKey.from(lat, lng)
            pendingCoordinates.getOrPut(coordinateKey) { mutableListOf() }.add(event.eventId)
        }

        if (pendingCoordinates.isEmpty()) return labels

        val semaphore = Semaphore(GEOCODE_CONCURRENCY)
        coroutineScope {
            pendingCoordinates.map { (coordinateKey, eventIds) ->
                async(Dispatchers.IO) {
                    val label = semaphore.withPermit {
                        AddressResolver.resolveRoadAddress(
                            context,
                            coordinateKey.latitude,
                            coordinateKey.longitude,
                        )
                    } ?: "위치 정보 없음"
                    eventIds to label
                }
            }.awaitAll().forEach { (eventIds, label) ->
                for (eventId in eventIds) {
                    labels[eventId] = label
                }
            }
        }

        return labels
    }

    private data class CoordinateKey(
        val latitude: Double,
        val longitude: Double,
    ) {
        companion object {
            fun from(latitude: Double, longitude: Double): CoordinateKey {
                val roundedLat = (latitude * 10_000.0).toLong() / 10_000.0
                val roundedLng = (longitude * 10_000.0).toLong() / 10_000.0
                return CoordinateKey(roundedLat, roundedLng)
            }
        }
    }
}
