package com.example.giftquest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GuessEntity::class, ItemEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun guessDao(): GuessDao
    abstract fun itemDao(): ItemDao
}

// Migration from v1 -> v2
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE guesses ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0"
        )
    }
}
