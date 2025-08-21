package com.example.giftquest.data.mapper

import com.example.giftquest.data.local.ItemEntity
import com.example.giftquest.data.model.Item

fun Item.toEntity(coupleId: String): ItemEntity =
    ItemEntity(
        // id is auto-generate in Room; if you want stable ids, add a separate String key in entity
        title = title,
        notes = notes,
        createdByUid = createdByUid,
        createdAtMillis = createdAt?.toDate()?.time ?: 0L,
        position = position,
        coupleId = coupleId
    )
