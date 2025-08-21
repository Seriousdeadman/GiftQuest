package com.example.giftquest.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// GuessEntity.kt
@Entity(tableName = "guesses")
data class GuessEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val itemId: Long,
    val guessedByUid: String,
    val guessText: String,
    @ColumnInfo(name = "createdAt") val createdAtMillis: Long = System.currentTimeMillis()
)

