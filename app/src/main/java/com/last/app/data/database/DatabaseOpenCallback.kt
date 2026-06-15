package com.last.app.data.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseOpenCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DatabaseIndexes.ensureAll(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA synchronous = NORMAL")
        db.execSQL("PRAGMA temp_store = MEMORY")
        DatabaseIndexes.ensureAll(db)
        optimize(db)
    }

    fun optimize(db: SupportSQLiteDatabase) {
        db.query("PRAGMA optimize").close()
    }
}
