package com.last.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_locations")
data class DeviceLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val timestamp: Long,
)
