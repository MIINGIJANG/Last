package com.last.app.domain.model.device

data class BluetoothConnectionChange(
    val deviceId: Long,
    val deviceType: String,
    val connected: Boolean,
)
