package com.example.giftquest.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GuessDao {

    // All guesses (if you still want this stream)
    @Query("SELECT * FROM guesses ORDER BY createdAt DESC")
    fun getAllGuesses(): Flow<List<GuessEntity>>

    // Guesses for one item
    @Query("SELECT * FROM guesses WHERE itemId = :itemId ORDER BY createdAt DESC")
    fun guessesForItemFlow(itemId: Long): Flow<List<GuessEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(guess: GuessEntity)
}
