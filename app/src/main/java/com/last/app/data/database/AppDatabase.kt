package com.last.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.last.app.data.dao.DeviceDao
import com.last.app.data.dao.EventDao
import com.last.app.data.dao.LatestLocatedEventDao
import com.last.app.data.dao.LocationDao
import com.last.app.data.dao.LastKnownLocationDao
import com.last.app.data.dao.SettingsDao
import com.last.app.data.dao.SystemLogDao
import com.last.app.data.dao.UserDao
import com.last.app.data.dao.WifiDao
import com.last.app.data.entity.AppSettings
import com.last.app.data.entity.Device
import com.last.app.data.entity.DeviceConnectionEvent
import com.last.app.data.entity.DeviceLatestLocatedEvent
import com.last.app.data.entity.SystemLog
import com.last.app.data.entity.User
import com.last.app.data.entity.WifiInformation
import com.last.app.data.entity.DeviceLastKnownLocation
import com.last.app.data.entity.DeviceLocation

@Database(
    entities = [
        User::class,
        Device::class,
        DeviceConnectionEvent::class,
        DeviceLocation::class,
        DeviceLastKnownLocation::class,
        DeviceLatestLocatedEvent::class,
        SystemLog::class,
        WifiInformation::class,
        AppSettings::class,
    ],
    version = 14,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun eventDao(): EventDao
    abstract fun latestLocatedEventDao(): LatestLocatedEventDao
    abstract fun locationDao(): LocationDao
    abstract fun lastKnownLocationDao(): LastKnownLocationDao
    abstract fun systemLogDao(): SystemLogDao
    abstract fun wifiDao(): WifiDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "last_app_database",
                )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .addCallback(DatabaseOpenCallback)
                    .addMigrations(
                        DatabaseMigrations.MIGRATION_9_10,
                        DatabaseMigrations.MIGRATION_10_11,
                        DatabaseMigrations.MIGRATION_11_12,
                        DatabaseMigrations.MIGRATION_12_13,
                        DatabaseMigrations.MIGRATION_13_14,
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
