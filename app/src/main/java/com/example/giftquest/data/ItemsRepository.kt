package com.example.giftquest.data

import com.example.giftquest.data.local.ItemDao
import com.example.giftquest.data.local.ItemEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class ItemsRepository(private val dao: ItemDao) {

    private val fs = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null
    @Volatile private var activeCoupleId: String? = null

    fun itemsFlow(coupleId: String): Flow<List<ItemEntity>> = dao.itemsFlow(coupleId)

    suspend fun startSync(coupleId: String) {
        if (activeCoupleId == coupleId) return
        stopSync()
        activeCoupleId = coupleId

        listener = fs.collection("couples").document(coupleId).collection("items")
            .orderBy("position", Query.Direction.ASCENDING)
            .addSnapshotListener { snaps, _ ->
                if (snaps == null) return@addSnapshotListener
                val list = snaps.documents.map { d ->
                    ItemEntity(
                        id = 0L, // autogen
                        remoteId = d.id,
                        title = d.getString("title") ?: "",
                        notes = d.getString("notes") ?: "",
                        createdByUid = d.getString("createdByUid") ?: "",
                        createdAtMillis = (d.getLong("createdAtMillis") ?: System.currentTimeMillis()),
                        position = (d.getDouble("position") ?: 0.0),
                        coupleId = coupleId
                    )
                }
                // Replace local cache for that couple
                CoroutineScope(Dispatchers.IO).launch {
                    dao.clearForCouple(coupleId)
                    list.forEach { dao.insert(it) }
                }
            }
    }

    fun stopSync() {
        listener?.remove()
        listener = null
        activeCoupleId = null
    }

    suspend fun addItem(title: String, coupleId: String, uid: String) {
        val col = fs.collection("couples").document(coupleId).collection("items")
        val now = System.currentTimeMillis()

        // Try to read the current max position. If rules block reads, fall back to 0.0
        val nextPos = try {
            val last = col.orderBy("position", Query.Direction.DESCENDING).limit(1).get().await()
            (last.documents.firstOrNull()?.getDouble("position") ?: -1.0) + 1.0
        } catch (_: Exception) {
            0.0
        }

        val data = mapOf(
            "title" to title,
            "notes" to "",
            "createdByUid" to uid,
            "createdAtMillis" to now,
            "position" to nextPos,
            "coupleId" to coupleId
        )

        // This write will succeed as long as the user is a member of couples/{coupleId}
        col.add(data).await()
    }

    suspend fun updateItem(itemId: Long, title: String, notes: String, coupleId: String) {
        val item = dao.getById(itemId) ?: return
        val col = fs.collection("couples").document(coupleId).collection("items")
        
        val data = mapOf(
            "title" to title,
            "notes" to notes
        )

        col.document(item.remoteId).update(data).await()
    }


    suspend fun reorder(idsInOrder: List<Long>, coupleId: String) {
        val locals = dao.getByIds(idsInOrder)
        val byLocalId = locals.associateBy { it.id }
        val col = fs.collection("couples").document(coupleId).collection("items")
        val batch = fs.batch()
        idsInOrder.forEachIndexed { index, localId ->
            val remoteId = byLocalId[localId]?.remoteId ?: return@forEachIndexed
            batch.update(col.document(remoteId), mapOf("position" to index.toDouble()))
        }
        batch.commit().await()
    }
}
