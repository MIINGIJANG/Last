package com.last.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.last.app.data.entity.DeviceLatestLocatedEvent

@Dao
interface LatestLocatedEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DeviceLatestLocatedEvent)

    @Query(
        """
        INSERT INTO device_latest_located_events (deviceId, eventId, eventTime)
        VALUES (:deviceId, :eventId, :eventTime)
        ON CONFLICT(deviceId) DO UPDATE SET
            eventId = excluded.eventId,
            eventTime = excluded.eventTime
        WHERE excluded.eventTime > device_latest_located_events.eventTime
           OR (
               excluded.eventTime = device_latest_located_events.eventTime
               AND excluded.eventId > device_latest_located_events.eventId
           )
        """,
    )
    suspend fun upsertIfNewer(deviceId: Long, eventId: Long, eventTime: Long)

    @Query("SELECT * FROM device_latest_located_events WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getByDeviceId(deviceId: Long): DeviceLatestLocatedEvent?

    @Query("DELETE FROM device_latest_located_events WHERE deviceId = :deviceId")
    suspend fun deleteByDeviceId(deviceId: Long)

    @Query("DELETE FROM device_latest_located_events WHERE eventTime < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int

    @Query(
        """
        DELETE FROM device_latest_located_events
        WHERE NOT EXISTS (
            SELECT 1 FROM device_connection_events e
            WHERE e.eventId = device_latest_located_events.eventId
        )
        """,
    )
    suspend fun deleteStaleEntries(): Int
}
