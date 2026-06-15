package com.last.app.external.system

import com.last.app.data.entity.EventType

fun interface SystemEventListener {
    fun onSystemEvent(deviceType: String, eventType: EventType, timestamp: Long, deviceId: String?)
}
