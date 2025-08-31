package com.example.giftquest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

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

    // NEW:
    @Query("DELETE FROM items WHERE coupleId = :coupleId")
    suspend fun clearForCouple(coupleId: String)

    @Query("SELECT * FROM items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?
}
