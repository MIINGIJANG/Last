package com.last.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "devices",
    indices = [
        Index(value = ["isRegistered", "id"]),
        Index(value = ["isRegistered", "eventSource"]),
        Index(value = ["isRegistered", "eventSource", "macAddress"]),
        Index(value = ["isRegistered", "eventSource", "deviceName"]),
        Index(value = ["isRegistered", "isConnected"]),
        Index(value = ["isRegistered", "macAddress"]),
    ],
)
data class Device(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceName: String,
    val deviceType: String,
    val macAddress: String = "",
    val isConnected: Boolean = false,
    val description: String = "",
    val eventSource: String,
    val isRegistered: Boolean = true,
) {
    val deviceId: String
        get() = id.toString()

    fun connect(): Device = copy(isConnected = true)

    fun disconnect(): Device = copy(isConnected = false)

    fun updateStatus(connected: Boolean): Device = if (connected) connect() else disconnect()
}
