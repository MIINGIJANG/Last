package com.last.app.data.entity

enum class EventType {
    CONNECT,
    DISCONNECT,
    ;

    val isDisconnect: Boolean
        get() = this == DISCONNECT
}
