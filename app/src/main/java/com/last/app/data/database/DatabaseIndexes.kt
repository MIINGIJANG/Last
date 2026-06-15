package com.last.app.data.database

import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseIndexes {

    fun ensureAll(db: SupportSQLiteDatabase) {
        ensurePartialIndexes(db)
        ensureMacLookupIndex(db)
        ensureLatestLocatedEventIndex(db)
    }

    fun ensurePartialIndexes(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_device_connection_events_locationId
            ON device_connection_events(locationId) WHERE locationId IS NOT NULL
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_device_connection_events_located_deviceId_eventTime
            ON device_connection_events(deviceId, eventTime) WHERE latitude IS NOT NULL
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_devices_registered_source_name
            ON devices(isRegistered, eventSource, deviceName) WHERE isRegistered = 1
            """.trimIndent(),
        )
    }

    fun ensureMacLookupIndex(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_devices_isRegistered_macAddress
            ON devices(isRegistered, macAddress)
            """.trimIndent(),
        )
    }

    fun ensureLatestLocatedEventIndex(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_device_latest_located_events_eventTime_eventId
            ON device_latest_located_events(eventTime, eventId)
            """.trimIndent(),
        )
    }
}
