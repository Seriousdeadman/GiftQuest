package com.example.giftquest

import android.app.Application
import androidx.room.Room
import com.example.giftquest.data.local.AppDatabase

class GiftQuestApplication : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        // Use a brand-new file name for dev so no stale DB can be reused
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            /* NEW NAME */ "giftquest_dev_v5.db"
        )
            // Dev-only: drop & recreate on any mismatch
            .fallbackToDestructiveMigration()
            // DO NOT add any .addMigrations(...) while iterating
            .build()
    }
}
