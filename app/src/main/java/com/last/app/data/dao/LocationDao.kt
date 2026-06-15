package com.last.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.last.app.data.entity.DeviceLocation

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: DeviceLocation): Long

    @Query("SELECT * FROM device_locations WHERE id = :locationId LIMIT 1")
    suspend fun getLocationById(locationId: Long): DeviceLocation?

    @Query("SELECT * FROM device_locations WHERE id IN (:locationIds)")
    suspend fun getLocationsByIds(locationIds: List<Long>): List<DeviceLocation>

    @Query("DELETE FROM device_locations WHERE id IN (:locationIds)")
    suspend fun deleteLocationsByIds(locationIds: List<Long>)

    @Query(
        """
        DELETE FROM device_locations
        WHERE NOT EXISTS (
            SELECT 1 FROM device_connection_events e
            WHERE e.locationId = device_locations.id
        )
        """,
    )
    suspend fun deleteOrphanLocations(): Int
}
