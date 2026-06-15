package com.last.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.last.app.data.database.DatabaseRetention
import com.last.app.data.entity.SystemLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SystemLog): Long

    @Query(
        "SELECT * FROM system_logs ORDER BY createdAt DESC LIMIT ${DatabaseRetention.OBSERVE_ALL_LOGS_LIMIT}",
    )
    fun observeAllLogs(): Flow<List<SystemLog>>

    @Query("SELECT * FROM system_logs WHERE deviceId = :deviceId ORDER BY createdAt DESC LIMIT ${DatabaseRetention.OBSERVE_ALL_LOGS_LIMIT}")
    fun observeLogsForDeviceId(deviceId: Long): Flow<List<SystemLog>>

    @Query("DELETE FROM system_logs WHERE createdAt < :cutoffTime")
    suspend fun deleteLogsOlderThan(cutoffTime: Long): Int

    @Query("DELETE FROM system_logs WHERE deviceId = :deviceId")
    suspend fun deleteLogsForDeviceId(deviceId: Long)
}
