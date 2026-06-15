package com.last.app.domain.util

import com.last.app.data.entity.Device
import com.last.app.data.entity.DeviceConnectionEvent
import com.last.app.domain.model.dashboard.DashboardRecentEventItem
import com.last.app.domain.model.dashboard.DashboardState
import com.last.app.domain.model.location.DeviceMapMarker
import com.last.app.domain.model.location.MapLocationCardTarget
import com.last.app.domain.model.location.MapState

fun List<DeviceConnectionEvent>.eventTimelineSignature(): Long {
    var hash = 1L
    for (event in this) {
        hash = 31 * hash + event.eventId
        hash = 31 * hash + event.eventTime
        hash = 31 * hash + (event.deviceId ?: -1L)
        hash = 31 * hash + (event.locationId ?: -1L)
        hash = 31 * hash + (event.latitude?.toBits() ?: 0)
        hash = 31 * hash + (event.longitude?.toBits() ?: 0)
    }
    return hash
}

fun List<Device>.deviceListSignature(): Long {
    var hash = 1L
    for (device in this) {
        hash = 31 * hash + device.id
        hash = 31 * hash + device.deviceName.hashCode()
        hash = 31 * hash + device.deviceType.hashCode()
        hash = 31 * hash + device.isConnected.hashCode()
        hash = 31 * hash + device.eventSource.hashCode()
        hash = 31 * hash + device.macAddress.hashCode()
    }
    return hash
}

fun List<MapLocationCardTarget>.cardTargetsSignature(): Long {
    var hash = 1L
    for (target in this) {
        hash = 31 * hash + target.deviceName.hashCode()
        hash = 31 * hash + target.recordedAt
        hash = 31 * hash + target.latitude.toBits()
        hash = 31 * hash + target.longitude.toBits()
        hash = 31 * hash + (target.eventType?.hashCode() ?: 0)
        hash = 31 * hash + (target.locationId ?: -1L)
    }
    return hash
}

fun List<DeviceMapMarker>.markersSignature(): Long {
    var hash = 1L
    for (marker in this) {
        hash = 31 * hash + marker.latitude.toBits()
        hash = 31 * hash + marker.longitude.toBits()
        hash = 31 * hash + marker.deviceName.hashCode()
        hash = 31 * hash + marker.deviceType.hashCode()
    }
    return hash
}

fun List<DashboardRecentEventItem>.dashboardRecentEventsSignature(): Long {
    var hash = 1L
    for (item in this) {
        hash = 31 * hash + item.timeLabel.hashCode()
        hash = 31 * hash + item.deviceName.hashCode()
        hash = 31 * hash + item.deviceType.hashCode()
        hash = 31 * hash + item.eventStatusLabel.hashCode()
        hash = 31 * hash + item.isDisconnect.hashCode()
    }
    return hash
}

fun MapState.stateSignature(): Long {
    var hash = devices.deviceListSignature()
    hash = 31 * hash + (selectedDevice?.id ?: -1L)
    hash = 31 * hash + markers.markersSignature()
    hash = 31 * hash + cardTargets.cardTargetsSignature()
    return hash
}

fun DashboardState.stateSignature(): Long {
    var hash = devices.deviceListSignature()
    hash = 31 * hash + (selectedDevice?.id ?: -1L)
    hash = 31 * hash + mapMarkers.markersSignature()
    hash = 31 * hash + recentEvents.dashboardRecentEventsSignature()
    return hash
}
