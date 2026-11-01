package com.rvishal.tvcalendar.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE events ADD COLUMN kind TEXT NOT NULL DEFAULT 'EVENT'")
        db.execSQL("ALTER TABLE events ADD COLUMN completed INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(entities = [EventEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
