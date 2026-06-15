package com.last.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.last.app.data.entity.Device
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<Device>): List<Long>

    @Query("SELECT COUNT(*) FROM devices")
    suspend fun countDevices(): Int

    @Query("SELECT * FROM devices WHERE isRegistered = 1 ORDER BY id ASC")
    suspend fun getRegisteredDevices(): List<Device>

    @Query("SELECT * FROM devices WHERE isRegistered = 1 ORDER BY id ASC")
    fun observeRegisteredDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE isRegistered = 0 ORDER BY id ASC")
    fun observeAvailableDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE isRegistered = 0 ORDER BY id ASC")
    suspend fun getAvailableDevices(): List<Device>

    @Query("DELETE FROM devices WHERE id = :deviceId AND isRegistered = 0")
    suspend fun deleteAvailableDevice(deviceId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM devices WHERE isRegistered = 1 AND eventSource = :source)")
    suspend fun hasRegisteredDevicesForSource(source: String): Boolean

    @Query("SELECT * FROM devices WHERE isRegistered = 1 AND macAddress = :macAddress LIMIT 1")
    suspend fun findRegisteredDeviceByMac(macAddress: String): Device?

    @Query(
        """
        SELECT * FROM devices
        WHERE isRegistered = 1 AND eventSource = :source
          AND (macAddress = :identifier OR deviceName = :identifier)
        LIMIT 1
        """,
    )
    suspend fun findRegisteredDeviceByIdentifier(source: String, identifier: String): Device?

    @Query(
        """
        SELECT * FROM devices
        WHERE eventSource = :source
          AND (macAddress = :identifier OR deviceName = :identifier)
        LIMIT 1
        """,
    )
    suspend fun findDeviceByIdentifier(source: String, identifier: String): Device?

    @Query("SELECT * FROM devices WHERE isRegistered = 1 AND eventSource = :source ORDER BY id ASC")
    suspend fun getRegisteredDevicesBySource(source: String): List<Device>

    @Query("DELETE FROM devices WHERE isRegistered = 0 AND id IN (:deviceIds)")
    suspend fun deleteAvailableDevicesByIds(deviceIds: List<Long>)

    @Query("UPDATE devices SET isConnected = :connected WHERE id = :deviceId")
    suspend fun updateConnectionStatus(deviceId: Long, connected: Boolean)

    @Query("UPDATE devices SET isConnected = :connected WHERE id IN (:deviceIds)")
    suspend fun updateConnectionStatusForIds(deviceIds: List<Long>, connected: Boolean)

    @Query("SELECT * FROM devices WHERE id = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: Long): Device?

    @Query("DELETE FROM devices WHERE id = :deviceId")
    suspend fun deleteDevice(deviceId: Long)

    @Query("DELETE FROM devices WHERE isRegistered = 0")
    suspend fun deleteAllAvailableDevices()

    @Query("UPDATE devices SET isRegistered = 1 WHERE id = :deviceId")
    suspend fun registerDevice(deviceId: Long)

    @Query("SELECT * FROM devices WHERE isRegistered = 1 AND isConnected = 1 ORDER BY id ASC")
    suspend fun getConnectedRegisteredDevices(): List<Device>
}
