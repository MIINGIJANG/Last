package com.last.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_last_known_locations")
data class DeviceLastKnownLocation(
    @PrimaryKey
    val deviceId: Long,
    val deviceType: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val recordedAt: Long,
)
