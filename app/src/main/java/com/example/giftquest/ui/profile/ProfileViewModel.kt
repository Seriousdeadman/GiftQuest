package com.example.giftquest.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val nickname: String = "",
    val photoUrl: String = "",
    val uid: String = ""
)

class ProfileViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _state.value = ProfileUiState(loading = false, error = "Not signed in")
            return
        }

        _state.value = ProfileUiState(loading = true, uid = uid)

        viewModelScope.launch {
            try {
                android.util.Log.d("GiftQuest", "Loading profile for UID: $uid")
                val userDoc = db.collection("users").document(uid).get().await()

                val nickname = userDoc.getString("nickname") ?: ""
                val photoUrl = userDoc.getString("photoUrl") ?: ""

                android.util.Log.d("GiftQuest", "Profile loaded - nickname: $nickname, photoUrl: $photoUrl")

                _state.value = ProfileUiState(
                    loading = false,
                    nickname = nickname,
                    photoUrl = photoUrl,
                    uid = uid
                )
            } catch (e: Exception) {
                android.util.Log.e("GiftQuest", "Failed to load profile", e)
                _state.value = ProfileUiState(
                    loading = false,
                    error = "Failed to load profile: ${e.message}",
                    uid = uid
                )
            }
        }
    }

    fun updateNickname(newNickname: String) {
        _state.value = _state.value.copy(nickname = newNickname)
    }

    fun updatePhotoUrl(newPhotoUrl: String) {
        _state.value = _state.value.copy(photoUrl = newPhotoUrl)
    }

    /**
     * Upload photo and automatically save to Firestore
     * Fixed version with proper Storage reference handling
     */
    suspend fun uploadPhotoAndSave(uri: Uri): Boolean {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _state.value = _state.value.copy(error = "Not signed in")
            return false
        }

        _state.value = _state.value.copy(loading = true, error = null)

        return try {
            android.util.Log.d("GiftQuest", "Starting photo upload for UID: $uid")
            android.util.Log.d("GiftQuest", "Photo URI: $uri")

            // Create Storage reference with proper path
            val timestamp = System.currentTimeMillis()
            val filename = "profile_$timestamp.jpg"
            val storagePath = "users/$uid/$filename"

            android.util.Log.d("GiftQuest", "Storage path: $storagePath")

            // Get storage reference
            val storageRef = storage.reference.child(storagePath)

            // Upload file using putFile (better for content URIs)
            android.util.Log.d("GiftQuest", "Starting upload...")
            val uploadTask = storageRef.putFile(uri).await()

            android.util.Log.d("GiftQuest", "Upload complete, getting download URL...")

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()

            android.util.Log.d("GiftQuest", "Photo uploaded successfully: $downloadUrl")

            // Update local state
            _state.value = _state.value.copy(photoUrl = downloadUrl)

            // Save to Firestore immediately
            val updates = hashMapOf<String, Any>(
                "photoUrl" to downloadUrl,
                "photoUpdatedAt" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(uid)
                .set(updates, SetOptions.merge())
                .await()

            android.util.Log.d("GiftQuest", "Photo URL saved to Firestore")

            // Also update Firebase Auth profile
            try {
                auth.currentUser?.updateProfile(
                    userProfileChangeRequest {
                        photoUri = Uri.parse(downloadUrl)
                    }
                )?.await()

                android.util.Log.d("GiftQuest", "Auth profile updated")
            } catch (e: Exception) {
                // Auth update is not critical, just log
                android.util.Log.w("GiftQuest", "Auth profile update failed (non-critical)", e)
            }

            _state.value = _state.value.copy(loading = false)
            true

        } catch (e: Exception) {
            android.util.Log.e("GiftQuest", "Photo upload failed", e)
            android.util.Log.e("GiftQuest", "Error details: ${e.message}")
            e.printStackTrace()

            _state.value = _state.value.copy(
                loading = false,
                error = "Photo upload failed: ${e.message}"
            )
            false
        }
    }

    fun saveProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _state.value = _state.value.copy(error = "Not signed in")
            return
        }

        _state.value = _state.value.copy(loading = true, error = null)

        viewModelScope.launch {
            try {
                android.util.Log.d("GiftQuest", "Saving profile for UID: $uid")

                val updates = mutableMapOf<String, Any>()

                if (_state.value.nickname.isNotBlank()) {
                    updates["nickname"] = _state.value.nickname
                }

                if (_state.value.photoUrl.isNotBlank()) {
                    updates["photoUrl"] = _state.value.photoUrl
                }

                if (updates.isNotEmpty()) {
                    db.collection("users")
                        .document(uid)
                        .set(updates, SetOptions.merge())
                        .await()

                    android.util.Log.d("GiftQuest", "Profile saved: $updates")
                }

                // Update Firebase Auth displayName
                if (_state.value.nickname.isNotBlank()) {
                    try {
                        auth.currentUser?.updateProfile(
                            userProfileChangeRequest {
                                displayName = _state.value.nickname
                            }
                        )?.await()

                        android.util.Log.d("GiftQuest", "Auth displayName updated")
                    } catch (e: Exception) {
                        android.util.Log.w("GiftQuest", "Auth displayName update failed (non-critical)", e)
                    }
                }

                _state.value = _state.value.copy(loading = false)

            } catch (e: Exception) {
                android.util.Log.e("GiftQuest", "Save profile failed", e)
                _state.value = _state.value.copy(
                    loading = false,
                    error = "Save failed: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}