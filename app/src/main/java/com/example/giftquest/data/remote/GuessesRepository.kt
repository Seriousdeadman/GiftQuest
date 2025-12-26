/*
package com.example.giftquest.data.remote

import com.example.giftquest.data.model.Guess
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GuessesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun guessesCollection(coupleId: String, itemId: String) =
        db.collection("couples").document(coupleId)
            .collection("items").document(itemId)
            .collection("guesses")

    */
/*suspend fun addGuess(coupleId: String, itemId: String, byUid: String, text: String, score: Int?) {
        val doc = guessesCollection(coupleId, itemId).document()
        val data = mapOf(
            "text" to text,
            "byUid" to byUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "closenessScore" to (score ?: 0)
        )
        doc.set(data).await()
    }*//*


    fun guessesFlow(coupleId: String, itemId: String): Flow<List<Guess>> = callbackFlow {
        val reg = guessesCollection(coupleId, itemId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList()); return@addSnapshotListener
                }
                val list = snap?.documents?.map { doc ->
                    val g = doc.toObject(Guess::class.java) ?: Guess()
                    g.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }
}
*/
