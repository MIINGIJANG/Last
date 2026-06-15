package com.last.app.domain.model.device

import com.last.app.data.entity.Device

data class ScannedDevice(
    val device: Device,
    val isConnected: Boolean,
)
