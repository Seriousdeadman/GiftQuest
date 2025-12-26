/*
package com.example.giftquest.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CoupleRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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

    */
/**
     * Uploads a user profile photo to Firebase Storage under users/{uid}/profile.jpg
     * and returns the public download URL.
     *//*

    suspend fun uploadUserPhoto(uid: String, fileUri: android.net.Uri): String {
        val path = "users/$uid/profile_${System.currentTimeMillis()}.jpg"
        val ref = storage.reference.child(path)
        ref.putFile(fileUri).await()
        return ref.downloadUrl.await().toString()
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

    suspend fun updateUserProfile(uid: String, nickname: String?, photoUrl: String?) {
        val updates = mutableMapOf<String, Any>()
        if (nickname != null) updates["nickname"] = nickname
        if (photoUrl != null) updates["photoUrl"] = photoUrl
        
        if (updates.isNotEmpty()) {
            db.collection("users").document(uid)
                .update(updates)
                .await()
        }
    }

    suspend fun getUserProfile(uid: String): Map<String, Any>? {
        val doc = db.collection("users").document(uid).get().await()
        return doc.data
    }

    // Ensure user document exists
    suspend fun ensureUserDoc(uid: String) {
        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()
        if (!snap.exists()) {
            ref.set(mapOf(
                "uid" to uid,
                "nickname" to null,
                "photoUrl" to null,
                "createdAt" to com.google.firebase.Timestamp.now()
            ), SetOptions.merge()).await()
        }
    }

    // Ensure couples/{id} exists; used for the pre-link "personal bucket" (id == uid)
    suspend fun ensureCoupleDoc(coupleId: String, uid: String) {
        val ref = db.collection("couples").document(coupleId)
        val snap = ref.get().await()
        if (!snap.exists()) {
            ref.set(mapOf(
                "members" to listOf(uid),
                "createdAt" to com.google.firebase.Timestamp.now(),
                "createdBy" to uid
            ), SetOptions.merge()).await()
        }
    }

    // When I receive an invite, accept it, set users/{myUid}.coupleId, and delete invite
    fun listenForInvites(myUid: String, onLinked: (String) -> Unit): ListenerRegistration {
        // Listen to all invites where I'm the recipient (toUid)
        val ref = db.collection("invites")
        return ref.whereEqualTo("toUid", myUid).addSnapshotListener { snaps, _ ->
            if (snaps == null || snaps.isEmpty) return@addSnapshotListener
            
            // Process each invite for this user
            for (doc in snaps.documents) {
                val data = doc.data ?: continue
                val coupleId = data["coupleId"] as? String ?: continue
                val fromUid = data["fromUid"] as? String ?: continue
                
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Ensure my user document exists
                        ensureUserDoc(myUid)
                        
                        // Validate the couple document exists and I'm a member
                        val coupleDoc = db.collection("couples").document(coupleId).get().await()
                        if (!coupleDoc.exists()) {
                            doc.reference.delete().await() // Clean up invalid invite
                            return@launch
                        }
                        
                        val members = (coupleDoc.get("members") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        if (myUid !in members) {
                            doc.reference.delete().await() // Clean up invalid invite
                            return@launch
                        }

                        // Check if I'm already linked
                        val myDoc = db.collection("users").document(myUid).get().await()
                        val myCoupleId = myDoc.getString("coupleId")
                        if (myCoupleId != null) {
                            doc.reference.delete().await() // Already linked, clean up invite
                            return@launch
                        }

                        // Accept the invite
                        db.collection("users").document(myUid)
                            .set(mapOf(
                                "coupleId" to coupleId,
                                "linkedAt" to com.google.firebase.Timestamp.now()
                            ), SetOptions.merge())
                            .await()
                        
                        // Delete the invite
                        doc.reference.delete().await()
                        
                        onLinked(coupleId)
                    } catch (e: Exception) {
                        // On error, try to clean up the invite
                        try {
                            doc.reference.delete().await()
                        } catch (_: Exception) {
                            // Ignore cleanup errors
                        }
                    }
                }
            }
        }
    }

    // Your existing link method (from earlier message) should still create couples doc + invite

    suspend fun unlinkMe(currentUid: String) {
        println("DEBUG: unlinkMe called for user: $currentUid")
        val meRef = db.collection("users").document(currentUid)
        val coupleId = meRef.get().await().getString("coupleId") ?: return
        println("DEBUG: Found coupleId: $coupleId")
        
        val coupleRef = db.collection("couples").document(coupleId)

        try {
            println("DEBUG: Starting transaction to unlink")
            db.runTransaction { txn ->
                val c = txn.get(coupleRef)
                if (c.exists()) {
                    val members = (c.get("members") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    println("DEBUG: Current members: $members")
                    val newMembers = members.filterNot { it == currentUid }
                    println("DEBUG: New members after removal: $newMembers")
                    
                    if (newMembers.isEmpty()) {
                        // No members left, delete the couple document
                        println("DEBUG: No members left, deleting couple document")
                        txn.delete(coupleRef)
                    } else {
                        // Update members list - the other user will be notified via the couple document listener
                        println("DEBUG: Updating members list to: $newMembers")
                        txn.update(coupleRef, mapOf("members" to newMembers))
                    }
                }
                
                // Remove my coupleId
                println("DEBUG: Removing coupleId from user document")
                txn.update(meRef, mapOf("coupleId" to com.google.firebase.firestore.FieldValue.delete()))
                null
            }.await()
            println("DEBUG: Transaction completed successfully")
            
            // Clean up any pending invites for me (both as sender and recipient)
            try {
                println("DEBUG: Cleaning up invites")
                // Delete invites where I'm the recipient
                val myInvites = db.collection("invites").whereEqualTo("toUid", currentUid).get().await()
                for (doc in myInvites.documents) {
                    doc.reference.delete().await()
                }
                
                // Delete invites where I'm the sender
                val sentInvites = db.collection("invites").whereEqualTo("fromUid", currentUid).get().await()
                for (doc in sentInvites.documents) {
                    doc.reference.delete().await()
                }
                println("DEBUG: Invite cleanup completed")
            } catch (_: Exception) {
                // Ignore if invites don't exist
                println("DEBUG: No invites to clean up")
            }
            
        } catch (e: Exception) {
            println("DEBUG: Transaction failed with error: ${e.message}")
            throw IllegalStateException("Failed to unlink: ${e.message}")
        }
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
        // Validate inputs
        if (myUid.isBlank() || partnerUid.isBlank()) {
            throw IllegalArgumentException("UIDs cannot be blank")
        }
        if (myUid == partnerUid) {
            throw IllegalArgumentException("Cannot link with yourself")
        }

        val coupleId = deterministicCoupleId(myUid, partnerUid)

        try {
            // Ensure my user document exists
            ensureUserDoc(myUid)
            
            // Check if I'm already linked
            val myDoc = db.collection("users").document(myUid).get().await()
            val myCoupleId = myDoc.getString("coupleId")
            if (myCoupleId != null) {
                throw IllegalStateException("You are already linked with a partner")
            }

            // Create couple document with both members
            db.collection("couples").document(coupleId)
                .set(mapOf(
                    "members" to listOf(myUid, partnerUid),
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "createdBy" to myUid
                ), SetOptions.merge())
                .await()

            // Update my user profile
            db.collection("users").document(myUid)
                .set(mapOf(
                    "coupleId" to coupleId,
                    "linkedAt" to com.google.firebase.Timestamp.now()
                ), SetOptions.merge())
                .await()

            // Create an invite for partner - using the structure expected by security rules
            val inviteId = "${myUid}_${partnerUid}_${System.currentTimeMillis()}"
            db.collection("invites").document(inviteId)
                .set(mapOf(
                    "fromUid" to myUid,
                    "toUid" to partnerUid,
                    "coupleId" to coupleId,
                    "invitedAt" to com.google.firebase.Timestamp.now()
                ), SetOptions.merge())
                .await()
            migrateOldListsToCouple(myUid, partnerUid, coupleId)


        } catch (e: Exception) {
            // Clean up on failure
            try {
                db.collection("couples").document(coupleId).delete().await()
                db.collection("users").document(myUid).update("coupleId", com.google.firebase.firestore.FieldValue.delete()).await()
                // Clean up any invites we created
                val inviteId = "${myUid}_${partnerUid}_${System.currentTimeMillis()}"
                db.collection("invites").document(inviteId).delete().await()
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
            throw e
        }
    }

    fun observeMembers(coupleId: String, onChange: (List<String>) -> Unit): ListenerRegistration {
        return db.collection("couples").document(coupleId)
            .addSnapshotListener { snap, _ ->
                val members = (snap?.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                onChange(members)
            }
    }
}
*/
