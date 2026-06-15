package com.last.app.infrastructure.monitoring

enum class ConnectionState {
    IDLE,
    MONITORING,
    CONNECTED,
    DISCONNECTED,
    LOCATION_SAVED,
    EVENT_RECORDED,
    STATUS_UPDATED,
    FINAL,
}
