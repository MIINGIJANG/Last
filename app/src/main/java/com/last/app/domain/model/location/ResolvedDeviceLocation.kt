package com.last.app.domain.model.location

data class ResolvedDeviceLocation(
    val deviceId: Long?,
    val deviceType: String,
    val latitude: Double,
    val longitude: Double,
    val recordedAt: Long,
    val isPeriodicBackup: Boolean,
)
