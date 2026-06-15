package com.last.app.domain.model.dashboard

import com.last.app.domain.model.location.DeviceMapMarker
import com.last.app.data.entity.Device

data class DashboardState(
    val devices: List<Device>,
    val selectedDevice: Device?,
    val recentEvents: List<DashboardRecentEventItem>,
    val mapMarkers: List<DeviceMapMarker>,
)
