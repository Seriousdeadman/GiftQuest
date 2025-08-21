package com.example.giftquest.data

import com.example.giftquest.data.local.ItemDao
import com.example.giftquest.data.local.ItemEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext


/**
 * Firestore is the source of truth.
 * A realtime listener mirrors data into Room (cache).
 * UI reads from Room via [itemsFlow].
 */
class ItemsRepository(
    private val dao: ItemDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var registration: ListenerRegistration? = null

    /** Stream from Room cache (ordered). */
    fun itemsFlow(coupleId: String): Flow<List<ItemEntity>> = dao.itemsFlow(coupleId)

    /** Start Firestore -> Room mirroring for the given couple. */
    fun startSync(coupleId: String) {
        stopSync()

        registration = firestore.collection("couples")
            .document(coupleId)
            .collection("items")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                val items = snap.documents.mapNotNull { doc ->
                    try {
                        val title = doc.getString("title") ?: ""
                        val notes = doc.getString("notes") ?: ""
                        val createdByUid = doc.getString("createdByUid") ?: ""
                        val createdAtMillis = (doc.get("createdAtMillis") as? Number)?.toLong()
                            ?: System.currentTimeMillis()
                        val position = (doc.get("position") as? Number)?.toDouble() ?: 0.0

                        // NOTE: no remoteId column locally; Room assigns id
                        ItemEntity(
                            id = 0L,
                            title = title,
                            notes = notes,
                            createdByUid = createdByUid,
                            createdAtMillis = createdAtMillis,
                            position = position,
                            coupleId = coupleId
                        )
                    } catch (_: Throwable) { null }
                }

                // Mirror snapshot into Room cache
                ioScope.launch {
                    try {
                        dao.clearAll()                 // dev-simple: replace local cache
                        items.forEach { dao.insert(it) }
                    } catch (_: Throwable) { /* swallow in dev */ }
                }
            }
    }



    /** Stop Firestore listener. */
    fun stopSync() {
        registration?.remove()
        registration = null
    }

    /** Add an item: write to Firestore; snapshot will update Room. */
    suspend fun addItem(title: String, uid: String, coupleId: String) {
        val nextPos: Double = (dao.maxPosition() ?: -1.0) + 1.0
        val data = hashMapOf(
            "title" to title,
            "notes" to "",
            "createdByUid" to uid,
            "createdAtMillis" to System.currentTimeMillis(),
            "position" to nextPos,
            "coupleId" to coupleId
        )

        try {
            firestore.collection("couples")
                .document(coupleId)
                .collection("items")
                .add(data)
                .await()
            // listener will mirror to Room
        } catch (e: Exception) {
            // log or surface to UI (e.g., send a callback/flow)
            e.printStackTrace()
        }
    }


    /** Reorder locally for instant UI, then push new positions to Firestore. */
    suspend fun reorder(idsInOrder: List<Long>, coupleId: String) {
        dao.applyOrder(idsInOrder)

        // One-shot read of current items from Room
        val current: List<ItemEntity> = withContext(Dispatchers.IO) { dao.itemsFlow(coupleId).first() }

        // Push positions by remoteId
        val batch = firestore.batch()
        current.forEach { item: ItemEntity ->
            val docId: String = item.remoteId ?: return@forEach
            val ref = firestore.collection("couples")
                .document(coupleId)
                .collection("items")
                .document(docId)
            batch.update(ref, "position", item.position)
        }
        try {
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /** Move item up locally, then push both changed items to Firestore. */
    suspend fun moveItemUp(id: Long, coupleId: String) {
        val item: ItemEntity = dao.getById(id) ?: return
        val prev: ItemEntity = dao.getPrevious(item.position) ?: return
        dao.swapPositions(item.id, item.position, prev.id, prev.position)
        pushPositionsToFirestore(coupleId, listOf(item.id, prev.id))
    }

    /** Move item down locally, then push both changed items to Firestore. */
    suspend fun moveItemDown(id: Long, coupleId: String) {
        val item: ItemEntity = dao.getById(id) ?: return
        val next: ItemEntity = dao.getNext(item.position) ?: return
        dao.swapPositions(item.id, item.position, next.id, next.position)
        pushPositionsToFirestore(coupleId, listOf(item.id, next.id))
    }

    /** Helper: push a small set of items' positions to Firestore using remoteId. */
    private suspend fun pushPositionsToFirestore(coupleId: String, localIds: List<Long>) {
        val roomItems: List<ItemEntity> = dao.itemsFlow(coupleId).first()
        val toPush: List<ItemEntity> = roomItems.filter { item ->
            localIds.contains(item.id) && item.remoteId != null
        }

        if (toPush.isEmpty()) return

        val batch = firestore.batch()
        toPush.forEach { item: ItemEntity ->
            val ref = firestore.collection("couples")
                .document(coupleId)
                .collection("items")
                .document(item.remoteId!!)
            batch.update(ref, "position", item.position)
        }
        batch.commit().await()
    }
}
