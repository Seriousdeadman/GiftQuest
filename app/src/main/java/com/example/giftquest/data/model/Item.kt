package com.example.giftquest.data.model

data class Item(
    val id: String = "",
    val title: String = "",
    val notes: String = "",
    val createdByUid: String = "",
    val createdAt: com.google.firebase.Timestamp? = null,
    val position: Double = 0.0
)
