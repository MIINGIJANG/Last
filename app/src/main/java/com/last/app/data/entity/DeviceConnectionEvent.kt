package com.last.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "device_connection_events",
    indices = [
        Index(value = ["deviceId", "eventTime"]),
        Index(value = ["deviceId", "eventId"]),
        Index(value = ["eventTime"]),
        Index(value = ["locationId"]),
    ],
)
data class DeviceConnectionEvent(
    @PrimaryKey(autoGenerate = true)
    val eventId: Long = 0,
    val deviceType: String,
    val eventType: String,
    val deviceId: Long? = null,
    val locationId: Long? = null,
    val eventTime: Long,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    companion object {
        fun createEvent(
            deviceType: String,
            eventType: EventType,
            eventTime: Long,
            deviceId: Long? = null,
        ): DeviceConnectionEvent {
            return DeviceConnectionEvent(
                deviceType = deviceType,
                eventType = eventType.name,
                deviceId = deviceId,
                eventTime = eventTime,
            )
        }
    }

    fun getEventInfo(): String {
        return "$deviceType · $eventType · $eventTime"
    }
}
