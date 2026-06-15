package com.last.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "device_latest_located_events",
    indices = [
        Index(value = ["eventId"], unique = true),
        Index(value = ["eventTime", "eventId"]),
    ],
)
data class DeviceLatestLocatedEvent(
    @PrimaryKey
    val deviceId: Long,
    val eventId: Long,
    val eventTime: Long,
)
