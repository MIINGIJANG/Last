package com.last.app.domain.model.location

import com.last.app.data.entity.Device

data class MapState(
    val devices: List<Device>,
    val selectedDevice: Device?,
    val markers: List<DeviceMapMarker>,
    val cardTargets: List<MapLocationCardTarget> = emptyList(),
)
