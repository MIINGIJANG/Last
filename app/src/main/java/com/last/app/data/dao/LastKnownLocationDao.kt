package com.last.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.last.app.data.entity.DeviceLastKnownLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface LastKnownLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(location: DeviceLastKnownLocation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(locations: List<DeviceLastKnownLocation>)

    @Query("SELECT * FROM device_last_known_locations WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getByDeviceId(deviceId: Long): DeviceLastKnownLocation?

    @Query("SELECT * FROM device_last_known_locations")
    fun observeAll(): Flow<List<DeviceLastKnownLocation>>

    @Query("DELETE FROM device_last_known_locations WHERE deviceId = :deviceId")
    suspend fun deleteByDeviceId(deviceId: Long)
}
