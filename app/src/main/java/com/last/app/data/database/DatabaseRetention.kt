package com.last.app.data.database

object DatabaseRetention {
    const val EVENT_RETENTION_DAYS = 180L
    const val LOG_RETENTION_DAYS = 90L
    const val OBSERVE_ALL_LOGS_LIMIT = 500
    const val HISTORY_EVENT_LIMIT = 300

    val EVENT_RETENTION_MS: Long = EVENT_RETENTION_DAYS * 24 * 60 * 60 * 1000
    val LOG_RETENTION_MS: Long = LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000
    const val PRUNE_INTERVAL_MS = 24L * 60 * 60 * 1000
}
