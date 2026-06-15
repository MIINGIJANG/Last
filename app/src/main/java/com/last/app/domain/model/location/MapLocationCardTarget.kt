package com.last.app.domain.model.location

data class MapLocationCardTarget(
    val deviceName: String,
    val deviceType: String,
    val recordedAt: Long,
    val latitude: Double,
    val longitude: Double,
    val isPeriodicBackup: Boolean,
    val eventType: String? = null,
    val locationId: Long? = null,
    val storedAddress: String = "",
)
