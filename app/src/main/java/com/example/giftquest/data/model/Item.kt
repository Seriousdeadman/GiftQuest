package com.example.giftquest.data.model

data class Item(
    val remoteId: String,
    val title: String,
    val notes: String,
    val createdByUid: String,
    val createdAtMillis: Long,
    val position: Double,
    val coupleId: String
)

