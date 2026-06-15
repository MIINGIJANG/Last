package com.last.app.domain.model.location

fun ResolvedDeviceLocation.toMapMarker(deviceName: String): DeviceMapMarker {
    return DeviceMapMarker(
        latitude = latitude,
        longitude = longitude,
        deviceName = deviceName,
        deviceType = deviceType,
    )
}
