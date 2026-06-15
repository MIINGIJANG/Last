package com.last.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val notificationEnabled: Boolean = false,
    val locationTrackingEnabled: Boolean = true,
    val autoMonitoringEnabled: Boolean = true,
    val deviceScanEnabled: Boolean = true,
)
