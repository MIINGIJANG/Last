package com.last.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "system_logs",
    indices = [
        Index(value = ["deviceId", "createdAt"]),
        Index(value = ["createdAt"]),
    ],
)
data class SystemLog(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0,
    val eventId: Long? = null,
    val deviceId: Long? = null,
    val logType: String = "EVENT",
    val message: String,
    val createdAt: Long,
) {
    companion object {
        fun saveLog(
            message: String,
            eventId: Long? = null,
            deviceId: Long? = null,
            logType: String = "EVENT",
        ): SystemLog {
            return SystemLog(
                eventId = eventId,
                deviceId = deviceId,
                logType = logType,
                message = message,
                createdAt = System.currentTimeMillis(),
            )
        }
    }

    fun loadLogInfo(): String = "[$logType] $message"
}
