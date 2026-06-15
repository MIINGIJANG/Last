package com.last.app.domain.model.location

data class MapLocationCardDisplay(
    val deviceName: String,
    val deviceType: String,
    val eventStatusLabel: String? = null,
    val isDisconnect: Boolean = false,
    val timeLabel: String,
    val locationLabel: String,
)
