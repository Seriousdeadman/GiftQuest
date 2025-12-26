package com.example.giftquest.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PairingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Generate and save a unique 8-character share code for the user
     */
    suspend fun ensureMyShareCode(uid: String): String {
        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()
        val existing = snap.getString("myShareCode")

        if (!existing.isNullOrBlank()) return existing

        // Generate new code
        val newCode = UUID.randomUUID().toString().take(8).uppercase()
        userRef.set(mapOf("myShareCode" to newCode), SetOptions.merge()).await()
        return newCode
    }

    /**
     * Instantly link with partner using their share code
     * NO collection group query - simple search in users/ collection
     */
    suspend fun linkWithPartnerCode(myUid: String, partnerCode: String): String {
        // ✅ Simple query - no collection group, no index needed!
        val usersQuery = db.collection("users")
            .whereEqualTo("myShareCode", partnerCode)
            .get()
            .await()

        require(!usersQuery.isEmpty) { "Invalid code - partner not found" }

        val partnerDoc = usersQuery.documents.first()
        val partnerUid = partnerDoc.id

        require(myUid != partnerUid) { "Cannot link with yourself" }

        // Check if either user is already linked
        val myDoc = db.collection("users").document(myUid).get().await()
        val myLinkedWith = myDoc.getString("linkedWith")

        val partnerLinkedWith = partnerDoc.getString("linkedWith")

        require(myLinkedWith.isNullOrBlank()) { "You are already linked with someone" }
        require(partnerLinkedWith.isNullOrBlank()) { "Partner is already linked with someone" }

        // Atomic link: set linkedWith on both users
        val batch = db.batch()

        batch.set(
            db.collection("users").document(myUid),
            mapOf(
                "linkedWith" to partnerUid,
                "linkedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )

        batch.set(
            db.collection("users").document(partnerUid),
            mapOf(
                "linkedWith" to myUid,
                "linkedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )

        batch.commit().await()

        return partnerUid // Return partner's UID
    }

    /**
     * Unlink from current partner
     * Removes linkedWith field from both users
     */
    suspend fun unlinkMe(myUid: String) {
        // Get my partner's UID
        val myDoc = db.collection("users").document(myUid).get().await()
        val partnerUid = myDoc.getString("linkedWith")

        if (partnerUid.isNullOrBlank()) {
            throw IllegalStateException("You are not linked with anyone")
        }

        // Atomic unlink: remove linkedWith from both users
        val batch = db.batch()

        batch.update(
            db.collection("users").document(myUid),
            mapOf("linkedWith" to FieldValue.delete())
        )

        batch.update(
            db.collection("users").document(partnerUid),
            mapOf("linkedWith" to FieldValue.delete())
        )

        batch.commit().await()
    }

    /**
     * Listen for changes to my linkedWith status
     * Triggers callback when I get linked or unlinked
     */
    fun observeMyLinkStatus(myUid: String, onChange: (String?) -> Unit): ListenerRegistration {
        return db.collection("users").document(myUid)
            .addSnapshotListener { snap, _ ->
                val linkedWith = snap?.getString("linkedWith")
                onChange(linkedWith)
            }
    }

    /**
     * Get partner's profile info
     */
    suspend fun getPartnerProfile(partnerUid: String): Map<String, Any>? {
        val snap = db.collection("users").document(partnerUid).get().await()
        return snap.data
    }
}