package com.last.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wifi_information",
    indices = [Index(value = ["locationId"])],
)
data class WifiInformation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val locationId: Long,
    val ssid: String,
    val bssid: String,
    val signalStrength: Int = 0,
    val timestamp: Long,
) {
    fun getWifiInfo(): String = "$ssid ($bssid) · $signalStrength dBm"
}
