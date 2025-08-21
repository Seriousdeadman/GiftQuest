package com.example.giftquest

import android.app.Application
import androidx.room.Room
import com.example.giftquest.data.local.AppDatabase
import com.example.giftquest.data.local.MIGRATION_1_2

class GiftQuestApplication : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "giftquest.db"
        )
            // while developing, this guarantees no crash on version mismatch
            .fallbackToDestructiveMigration()
            // keep your explicit migrations too (fine to have both)
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
