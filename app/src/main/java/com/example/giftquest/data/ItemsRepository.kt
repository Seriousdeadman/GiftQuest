package com.example.giftquest.data

import com.example.giftquest.data.local.ItemDao
import com.example.giftquest.data.local.ItemEntity
import kotlinx.coroutines.flow.Flow

class ItemsRepository(private val dao: ItemDao) {

    fun itemsFlow(coupleId: String): Flow<List<ItemEntity>> =
        dao.itemsFlow(coupleId)

    suspend fun addItem(title: String, uid: String, coupleId: String) {
        // place new item at end
        val nextPos = (dao.maxPosition() ?: -1.0) + 1.0
        dao.insert(
            ItemEntity(
                title = title,
                notes = "",
                createdByUid = uid,
                createdAtMillis = System.currentTimeMillis(),
                position = nextPos,
                coupleId = coupleId
            )
        )
    }

    suspend fun moveItemUp(id: Long) {
        val item = dao.getById(id) ?: return
        val prev = dao.getPrevious(item.position) ?: return
        dao.swapPositions(item.id, item.position, prev.id, prev.position)
    }

    suspend fun moveItemDown(id: Long) {
        val item = dao.getById(id) ?: return
        val next = dao.getNext(item.position) ?: return
        dao.swapPositions(item.id, item.position, next.id, next.position)
    }

    suspend fun reorder(idsInOrder: List<Long>) {
        dao.applyOrder(idsInOrder)
    }
}
