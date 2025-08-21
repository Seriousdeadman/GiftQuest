package com.example.giftquest.data.remote

import com.example.giftquest.data.model.Item
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CloudItemsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun itemsCollection(coupleId: String) =
        db.collection("couples").document(coupleId).collection("items")

    suspend fun addItem(coupleId: String, title: String, notes: String, createdByUid: String) {
        val doc = itemsCollection(coupleId).document()
        val data = mapOf(
            "title" to title,
            "notes" to notes,
            "createdByUid" to createdByUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "position" to System.currentTimeMillis().toDouble()
        )
        doc.set(data).await()
    }

    fun itemsFlow(coupleId: String): Flow<List<Item>> = callbackFlow {
        val reg = itemsCollection(coupleId)
            .orderBy("position", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.documents?.map { d ->
                    (d.toObject(Item::class.java) ?: Item()).copy(id = d.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun updatePositions(coupleId: String, itemsInNewOrder: List<Item>) {
        val batch = db.batch()
        var pos = 0.0
        for (it in itemsInNewOrder) {
            pos += 1.0
            batch.update(itemsCollection(coupleId).document(it.id), "position", pos)
        }
        batch.commit().await()
    }
}
