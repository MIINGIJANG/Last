package com.last.app.domain.model.dashboard

data class DashboardRecentEventItem(
    val timeLabel: String,
    val deviceName: String,
    val deviceType: String,
    val eventStatusLabel: String,
    val isDisconnect: Boolean,
)
