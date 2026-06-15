package com.last.app.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_device_connection_events_deviceId_eventTime " +
                    "ON device_connection_events(deviceId, eventTime)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_device_connection_events_eventTime " +
                    "ON device_connection_events(eventTime)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_devices_isRegistered_id ON devices(isRegistered, id)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_devices_isRegistered_eventSource_macAddress " +
                    "ON devices(isRegistered, eventSource, macAddress)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_devices_isRegistered_isConnected " +
                    "ON devices(isRegistered, isConnected)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_system_logs_deviceId_createdAt " +
                    "ON system_logs(deviceId, createdAt)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_system_logs_createdAt ON system_logs(createdAt)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_wifi_information_locationId ON wifi_information(locationId)",
            )
            db.execSQL(
                "UPDATE devices SET macAddress = UPPER(macAddress) " +
                    "WHERE eventSource = 'BLUETOOTH' AND macAddress != UPPER(macAddress)",
            )
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_devices_isRegistered_eventSource " +
                    "ON devices(isRegistered, eventSource)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_device_connection_events_deviceId_eventId " +
                    "ON device_connection_events(deviceId, eventId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_device_connection_events_located_deviceId_eventId " +
                    "ON device_connection_events(deviceId, eventId) WHERE latitude IS NOT NULL",
            )
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_device_connection_events_locationId " +
                    "ON device_connection_events(locationId) WHERE locationId IS NOT NULL",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_device_connection_events_located_deviceId_eventTime " +
                    "ON device_connection_events(deviceId, eventTime) WHERE latitude IS NOT NULL",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_devices_registered_source_name " +
                    "ON devices(isRegistered, eventSource, deviceName) WHERE isRegistered = 1",
            )
            db.execSQL(
                "DROP INDEX IF EXISTS index_device_connection_events_located_deviceId_eventId",
            )
            DatabaseOpenCallback.optimize(db)
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS device_latest_located_events (
                    deviceId INTEGER NOT NULL PRIMARY KEY,
                    eventId INTEGER NOT NULL,
                    eventTime INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_device_latest_located_events_eventId
                ON device_latest_located_events(eventId)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT OR REPLACE INTO device_latest_located_events (deviceId, eventId, eventTime)
                SELECT deviceId, eventId, eventTime
                FROM (
                    SELECT deviceId, eventId, eventTime,
                        ROW_NUMBER() OVER (
                            PARTITION BY deviceId
                            ORDER BY eventTime DESC, eventId DESC
                        ) AS rn
                    FROM device_connection_events
                    WHERE deviceId IS NOT NULL AND latitude IS NOT NULL
                )
                WHERE rn = 1
                """.trimIndent(),
            )
            DatabaseIndexes.ensureMacLookupIndex(db)
            DatabaseIndexes.ensurePartialIndexes(db)
            DatabaseOpenCallback.optimize(db)
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            DatabaseIndexes.ensureLatestLocatedEventIndex(db)
            DatabaseOpenCallback.optimize(db)
        }
    }
}
