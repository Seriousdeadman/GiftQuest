package com.example.giftquest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val remoteId: String = "",               // <— NEW: Firestore doc id
    val title: String,
    val notes: String = "",
    val createdByUid: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val position: Double = 0.0,
    val coupleId: String? = null
)
