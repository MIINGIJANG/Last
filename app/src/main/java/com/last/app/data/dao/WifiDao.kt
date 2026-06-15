package com.last.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.last.app.data.entity.WifiInformation

@Dao
interface WifiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWifiInfo(info: WifiInformation): Long

    @Query("SELECT * FROM wifi_information WHERE locationId = :locationId LIMIT 1")
    suspend fun getWifiByLocationId(locationId: Long): WifiInformation?

    @Query("DELETE FROM wifi_information WHERE locationId IN (:locationIds)")
    suspend fun deleteByLocationIds(locationIds: List<Long>)

    @Query(
        """
        DELETE FROM wifi_information
        WHERE NOT EXISTS (
            SELECT 1 FROM device_connection_events e
            WHERE e.locationId = wifi_information.locationId
        )
        """,
    )
    suspend fun deleteOrphanWifi(): Int
}
