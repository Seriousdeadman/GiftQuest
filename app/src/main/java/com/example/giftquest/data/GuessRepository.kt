package com.example.giftquest.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.min

data class Guess(
    val remoteId: String,
    val itemId: String,  // remote item ID
    val guessedByUid: String,
    val guessText: String
)

class GuessRepository() {  // No constructor parameters

    private val fs = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    fun guesses(itemId: String): Flow<List<Guess>> = callbackFlow {
        val listener = fs.collection("guesses")
            .whereEqualTo("itemId", itemId)
            .orderBy("createdAtMillis", Query.Direction.ASCENDING)
            .addSnapshotListener { snaps, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val guesses = snaps?.documents?.map { d ->
                    Guess(
                        remoteId = d.id,
                        itemId = d.getString("itemId") ?: "",
                        guessedByUid = d.getString("guessedByUid") ?: "",
                        guessText = d.getString("guessText") ?: ""
                    )
                } ?: emptyList()

                trySend(guesses)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendUserGuess(itemId: String, text: String, uid: String) {
        val now = System.currentTimeMillis()
        fs.collection("guesses").add(mapOf(
            "itemId" to itemId,
            "guessedByUid" to uid,
            "guessText" to text,
            "createdAtMillis" to now
        )).await()
    }

    suspend fun sendAiReply(itemId: String, itemTitle: String, guessText: String) {
        val overlap = commonChars(itemTitle.lowercase(), guessText.lowercase())
        val score = (overlap.toFloat() / maxOf(1, itemTitle.length)).coerceIn(0f, 1f)
        val reply = when {
            score > 0.6f -> "Hot! 🔥 You're very close!"
            score > 0.25f -> "Warm 🙂 Getting there!"
            else -> "Cold ❄️"
        }
        val now = System.currentTimeMillis()
        fs.collection("guesses").add(mapOf(
            "itemId" to itemId,
            "guessedByUid" to "ai",
            "guessText" to reply,
            "createdAtMillis" to now
        )).await()
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
