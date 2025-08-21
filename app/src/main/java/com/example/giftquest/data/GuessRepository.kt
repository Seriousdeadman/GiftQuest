package com.example.giftquest.data

import com.example.giftquest.data.local.GuessDao
import com.example.giftquest.data.local.GuessEntity
import kotlinx.coroutines.flow.Flow
import kotlin.math.min

class GuessRepository(private val dao: GuessDao) {

    fun guesses(itemId: Long): Flow<List<GuessEntity>> =
        dao.guessesForItemFlow(itemId)

    // now takes uid and uses GuessEntity fields that actually exist
    suspend fun sendUserGuess(itemId: Long, text: String, uid: String) {
        dao.insert(
            GuessEntity(
                itemId = itemId,
                guessedByUid = uid,
                guessText = text
            )
        )
    }

    // If you want a simple AI reply without adding new DB columns,
    // store it as a normal GuessEntity with guessedByUid="ai"
    suspend fun sendAiReply(itemId: Long, itemTitle: String, guessText: String) {
        val overlap = commonChars(itemTitle.lowercase(), guessText.lowercase())
        val score = (overlap.toFloat() / maxOf(1, itemTitle.length)).coerceIn(0f, 1f)
        val reply = when {
            score > 0.6f -> "Hot! 🔥 You're very close!"
            score > 0.25f -> "Warm 🙂 Getting there!"
            else -> "Cold ❄️"
        }
        dao.insert(
            GuessEntity(
                itemId = itemId,
                guessedByUid = "ai",
                guessText = reply
            )
        )
    }

    private fun commonChars(a: String, b: String): Int {
        val freqA = IntArray(26)
        val freqB = IntArray(26)
        fun fill(s: String, arr: IntArray) {
            s.forEach { ch ->
                val i = ch - 'a'
                if (i in 0..25) arr[i]++
            }
        }
        fill(a, freqA); fill(b, freqB)
        var sum = 0
        for (i in 0..25) sum += min(freqA[i], freqB[i])
        return sum
    }
}
