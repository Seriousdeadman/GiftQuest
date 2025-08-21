package com.example.giftquest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    // Stream for a couple’s list, ordered by position then recency
    @Query("SELECT * FROM items WHERE coupleId = :coupleId ORDER BY position ASC, createdAtMillis DESC")
    fun itemsFlow(coupleId: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Query("UPDATE items SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Double)

    @Query("DELETE FROM items")
    suspend fun clearAll()

    // ---- helpers ported from the other DAO ----

    @Query("SELECT MAX(position) FROM items")
    suspend fun maxPosition(): Double?

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE position < :pos ORDER BY position DESC LIMIT 1")
    suspend fun getPrevious(pos: Double): ItemEntity?

    @Query("SELECT * FROM items WHERE position > :pos ORDER BY position ASC LIMIT 1")
    suspend fun getNext(pos: Double): ItemEntity?

    @Query("UPDATE items SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Double)

    @Transaction
    suspend fun swapPositions(aId: Long, aPos: Double, bId: Long, bPos: Double) {
        setPosition(aId, bPos)
        setPosition(bId, aPos)
    }

    @Transaction
    suspend fun applyOrder(idsInOrder: List<Long>) {
        idsInOrder.forEachIndexed { index, id ->
            setPosition(id, index.toDouble())
        }
    }
}
