package com.example.giftquest.data

import com.example.giftquest.data.model.Item
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ItemsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Get real-time stream of items for a specific user
     */
    fun itemsFlow(ownerUid: String): Flow<List<Item>> = callbackFlow {
        val listener = db.collection("users")
            .document(ownerUid)
            .collection("items")
            .orderBy("position", Query.Direction.ASCENDING)
            .addSnapshotListener { snaps, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val items = snaps?.documents?.map { doc ->
                    Item(
                        remoteId = doc.id,
                        title = doc.getString("title") ?: "",
                        notes = doc.getString("notes") ?: "",
                        createdByUid = ownerUid,
                        createdAtMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis(),
                        position = doc.getDouble("position") ?: 0.0,
                        coupleId = ownerUid // Using ownerUid as identifier
                    )
                } ?: emptyList()

                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Add a new item to user's list
     */
    suspend fun addItem(title: String, uid: String, coupleId: String) {
        android.util.Log.d("GiftQuest", "=== ITEMS REPO ADD START ===")
        android.util.Log.d("GiftQuest", "Collection path: users/$uid/items")

        val itemsRef = db.collection("users").document(uid).collection("items")
        val now = System.currentTimeMillis()

        // Calculate next position
        val nextPos = try {
            val last = itemsRef
                .orderBy("position", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            (last.documents.firstOrNull()?.getDouble("position") ?: -1.0) + 1.0
        } catch (e: Exception) {
            android.util.Log.w("GiftQuest", "Could not get last position: ${e.message}")
            0.0
        }

        android.util.Log.d("GiftQuest", "Next position: $nextPos")

        val data = mapOf(
            "title" to title,
            "notes" to "",
            "createdByUid" to uid,
            "createdAtMillis" to now,
            "position" to nextPos
        )

        android.util.Log.d("GiftQuest", "Writing document with data: $data")

        itemsRef.add(data).await()

        android.util.Log.d("GiftQuest", "✅ Document written to Firestore")
    }

    /**
     * Update an existing item
     */
    suspend fun updateItem(remoteId: String, title: String, notes: String, ownerUid: String) {
        val itemRef = db.collection("users")
            .document(ownerUid)
            .collection("items")
            .document(remoteId)

        val data = mapOf(
            "title" to title,
            "notes" to notes
        )

        itemRef.update(data).await()
    }

    /**
     * Delete an item
     */
    suspend fun deleteItem(remoteId: String, ownerUid: String) {
        db.collection("users")
            .document(ownerUid)
            .collection("items")
            .document(remoteId)
            .delete()
            .await()
    }

    /**
     * Reorder items by updating their positions
     */
    suspend fun reorder(remoteIdsInOrder: List<String>, ownerUid: String) {
        val itemsRef = db.collection("users").document(ownerUid).collection("items")
        val batch = db.batch()

        remoteIdsInOrder.forEachIndexed { index, remoteId ->
            batch.update(itemsRef.document(remoteId), mapOf("position" to index.toDouble()))
        }

        batch.commit().await()
    }
}