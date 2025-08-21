package com.example.giftquest.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PairingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun ensureMyShareCode(uid: String): String {
        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()
        val existing = snap.getString("myShareCode")
        if (!existing.isNullOrBlank()) return existing

        val newCode = UUID.randomUUID().toString().take(8).uppercase()
        userRef.set(mapOf("myShareCode" to newCode), SetOptions.merge()).await()
        return newCode
    }

    suspend fun linkWithPartnerCode(myUid: String, partnerCode: String): String {
        val users = db.collection("users").whereEqualTo("myShareCode", partnerCode).get().await()
        require(!users.isEmpty) { "Invalid code" }
        val partnerDoc = users.documents.first()
        val partnerUid = partnerDoc.id

        // (optional) sanity: avoid linking if already paired
        val myCouple = db.collection("users").document(myUid).get().await().getString("coupleId")
        val partnerCouple = partnerDoc.getString("coupleId")
        require(myCouple.isNullOrBlank() && partnerCouple.isNullOrBlank()) { "Already linked" }

        val coupleId = UUID.randomUUID().toString()
        val coupleRef = db.collection("couples").document(coupleId)

        val batch = db.batch()
        batch.set(coupleRef, mapOf(
            "createdAt" to FieldValue.serverTimestamp(),
            "memberUids" to listOf(myUid, partnerUid)
        ))
        batch.set(db.collection("users").document(myUid), mapOf("coupleId" to coupleId), SetOptions.merge())
        batch.set(db.collection("users").document(partnerUid), mapOf("coupleId" to coupleId), SetOptions.merge())
        batch.commit().await()

        return coupleId
    }
}
