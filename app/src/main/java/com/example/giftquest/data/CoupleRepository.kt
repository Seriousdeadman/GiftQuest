package com.example.giftquest.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CoupleRepository {
    private val db = FirebaseFirestore.getInstance()

    fun deterministicCoupleId(a: String, b: String): String {
        val (x, y) = if (a < b) a to b else b to a
        return "${x}_${y}"
    }

    // Observe my users/{uid}.coupleId
    fun observeMyCoupleId(myUid: String, onChange: (String?) -> Unit): ListenerRegistration {
        val ref = db.collection("users").document(myUid)
        return ref.addSnapshotListener { snap, _ ->
            onChange(snap?.getString("coupleId"))
        }
    }

    suspend fun isMemberOf(coupleId: String, uid: String): Boolean {
        val snap = db.collection("couples").document(coupleId).get().await()
        val members = (snap.get("members") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        return uid in members
    }

    suspend fun clearMyCouple(uid: String) {
        db.collection("users").document(uid)
            .update("coupleId", com.google.firebase.firestore.FieldValue.delete())
            .await()
    }

    // Ensure couples/{id} exists; used for the pre-link “personal bucket” (id == uid)
    suspend fun ensureCoupleDoc(coupleId: String, uid: String) {
        val ref = db.collection("couples").document(coupleId)
        val snap = ref.get().await()
        if (!snap.exists()) {
            ref.set(mapOf("members" to listOf(uid)), SetOptions.merge()).await()
        }
    }

    // When I receive an invite at invites/{myUid}, accept it, set users/{myUid}.coupleId, and delete invite
    fun listenForInvites(myUid: String, onLinked: (String) -> Unit): ListenerRegistration {
        val ref = db.collection("invites").document(myUid)
        return ref.addSnapshotListener { snap, _ ->
            val data = snap?.data ?: return@addSnapshotListener
            val coupleId = data["coupleId"] as? String ?: return@addSnapshotListener
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    db.collection("users").document(myUid)
                        .set(mapOf("coupleId" to coupleId), SetOptions.merge())
                        .await()
                    ref.delete().await()
                    onLinked(coupleId)
                } catch (_: Exception) { /* ignore */ }
            }
        }
    }

    // Your existing link method (from earlier message) should still create couples doc + invite

    suspend fun unlinkMe(currentUid: String) {
        val meRef = db.collection("users").document(currentUid)
        val coupleId = meRef.get().await().getString("coupleId") ?: return
        val coupleRef = db.collection("couples").document(coupleId)

        db.runTransaction { txn ->
            val c = txn.get(coupleRef)
            if (c.exists()) {
                val members = (c.get("members") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val newMembers = members.filterNot { it == currentUid }
                if (newMembers.isEmpty()) {
                    txn.delete(coupleRef)
                } else {
                    txn.update(coupleRef, mapOf("members" to newMembers))
                }
            }
            txn.update(meRef, mapOf("coupleId" to com.google.firebase.firestore.FieldValue.delete()))
            null
        }.await()
    }

    // Optional migration of old personal lists into the new shared couple
    suspend fun migrateOldListsToCouple(myOldId: String, partnerOldId: String, coupleId: String) {
        val src1 = db.collection("couples").document(myOldId).collection("items").get().await()
        val src2 = db.collection("couples").document(partnerOldId).collection("items").get().await()
        val target = db.collection("couples").document(coupleId).collection("items")
        val batch = db.batch()
        for (doc in (src1.documents + src2.documents)) {
            val data = doc.data ?: continue
            batch.set(target.document(), data, SetOptions.merge())
        }
        for (doc in src1.documents) batch.delete(doc.reference)
        for (doc in src2.documents) batch.delete(doc.reference)
        batch.commit().await()
    }

    suspend fun linkAndInvite(myUid: String, partnerUid: String) {
        val coupleId = deterministicCoupleId(myUid, partnerUid)

        // Ensure couple doc exists
        db.collection("couples").document(coupleId)
            .set(mapOf("members" to listOf(myUid, partnerUid)), SetOptions.merge())
            .await()

        // Update *my* user profile
        db.collection("users").document(myUid)
            .set(mapOf("coupleId" to coupleId), SetOptions.merge())
            .await()

        // Create an invite for partner (they’ll pick it up via listenForInvites)
        db.collection("invites").document(partnerUid)
            .set(mapOf("coupleId" to coupleId), SetOptions.merge())
            .await()
    }


}
