package com.last.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.last.app.data.entity.DeviceConnectionEvent
import com.last.app.data.database.DatabaseRetention
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    companion object {
        const val HISTORY_EVENT_LIMIT = DatabaseRetention.HISTORY_EVENT_LIMIT
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DeviceConnectionEvent): Long

    @Query(
        "UPDATE device_connection_events SET locationId = :locationId, latitude = :latitude, longitude = :longitude WHERE eventId = :eventId",
    )
    suspend fun updateEventLocation(eventId: Long, locationId: Long, latitude: Double, longitude: Double)

    @Query(
        """
        SELECT * FROM device_connection_events
        WHERE deviceId = :deviceId
        ORDER BY eventTime DESC, eventId DESC
        LIMIT :limit
        """,
    )
    fun observeEventsForDeviceId(deviceId: Long, limit: Int = HISTORY_EVENT_LIMIT): Flow<List<DeviceConnectionEvent>>

    @Query(
        """
        SELECT * FROM device_connection_events
        ORDER BY eventTime DESC, eventId DESC
        LIMIT :limit
        """,
    )
    fun observeRecentEvents(limit: Int): Flow<List<DeviceConnectionEvent>>

    @Query(
        """
        SELECT * FROM device_connection_events
        WHERE deviceId IN (:deviceIds)
        ORDER BY eventTime DESC, eventId DESC
        LIMIT :limit
        """,
    )
    fun observeRecentEventsForDevices(deviceIds: List<Long>, limit: Int): Flow<List<DeviceConnectionEvent>>

    @Query(
        """
        SELECT * FROM device_connection_events
        WHERE deviceId = :deviceId
        ORDER BY eventTime DESC, eventId DESC
        LIMIT :limit
        """,
    )
    fun observeRecentEventsForDevice(deviceId: Long, limit: Int): Flow<List<DeviceConnectionEvent>>

    @Query(
        """
        SELECT e.eventId, e.deviceType, e.eventType, e.deviceId, e.locationId, e.eventTime, e.latitude, e.longitude
        FROM device_connection_events e
        INNER JOIN devices d ON e.deviceId = d.id
        WHERE d.isRegistered = 1
        ORDER BY e.eventTime DESC, e.eventId DESC
        LIMIT :limit
        """,
    )
    fun observeEventsForRegisteredDevices(limit: Int = HISTORY_EVENT_LIMIT): Flow<List<DeviceConnectionEvent>>

    @Query(
        "SELECT DISTINCT locationId FROM device_connection_events WHERE deviceId = :deviceId AND locationId IS NOT NULL",
    )
    suspend fun getLocationIdsForDevice(deviceId: Long): List<Long>

    @Query("DELETE FROM device_connection_events WHERE eventTime < :cutoffTime")
    suspend fun deleteEventsOlderThan(cutoffTime: Long): Int

    @Query("DELETE FROM device_connection_events WHERE deviceId = :deviceId")
    suspend fun deleteEventsForDevice(deviceId: Long)

    @Query(
        """
        SELECT e.eventId, e.deviceType, e.eventType, e.deviceId, e.locationId, e.eventTime, e.latitude, e.longitude
        FROM device_connection_events e
        INNER JOIN device_latest_located_events latest ON e.eventId = latest.eventId
        ORDER BY latest.eventTime DESC, latest.eventId DESC
        LIMIT 1
        """,
    )
    fun observeLatestLocatedEvent(): Flow<DeviceConnectionEvent?>

    @Query(
        """
        SELECT e.eventId, e.deviceType, e.eventType, e.deviceId, e.locationId, e.eventTime, e.latitude, e.longitude
        FROM device_connection_events e
        INNER JOIN device_latest_located_events latest ON e.eventId = latest.eventId
        WHERE latest.deviceId = :deviceId
        LIMIT 1
        """,
    )
    fun observeLatestLocatedEventByDeviceId(deviceId: Long): Flow<DeviceConnectionEvent?>

    @Query(
        """
        SELECT e.eventId, e.deviceType, e.eventType, e.deviceId, e.locationId, e.eventTime, e.latitude, e.longitude
        FROM device_connection_events e
        INNER JOIN device_latest_located_events latest ON e.eventId = latest.eventId
        """,
    )
    fun observeLatestLocatedEventsPerDevice(): Flow<List<DeviceConnectionEvent>>
}
