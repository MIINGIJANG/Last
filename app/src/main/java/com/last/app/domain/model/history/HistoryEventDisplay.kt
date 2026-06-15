package com.last.app.domain.model.history

data class HistoryEventDisplay(
    val deviceName: String,
    val deviceType: String,
    val eventStatusLabel: String,
    val isDisconnect: Boolean,
    val timeLabel: String,
    val locationLabel: String,
)
