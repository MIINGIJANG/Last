package com.last.app.domain.history

import com.last.app.data.entity.EventType

fun historyEventStatusLabel(eventType: String): String {
    val type = runCatching { EventType.valueOf(eventType) }.getOrNull() ?: return eventType
    return when (type) {
        EventType.CONNECT -> "연결됨"
        EventType.DISCONNECT -> "연결 해제"
    }
}

fun isDisconnectEvent(eventType: String): Boolean {
    val type = runCatching { EventType.valueOf(eventType) }.getOrNull() ?: return false
    return type.isDisconnect
}
