package com.example.giftquest.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.giftquest.data.CoupleRepository
import com.example.giftquest.data.model.Item
import com.example.giftquest.data.remote.CloudItemsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    // Firestore-only repositories
    private val repo = CloudItemsRepository()
    private val coupleRepo = CoupleRepository()

    private val uid: String = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun consumeMessage() { _message.value = null }

    private val _coupleIdProfile = MutableStateFlow<String?>(null)
    val coupleIdProfile: StateFlow<String?> = _coupleIdProfile

    private val _userProfile = MutableStateFlow<Map<String, Any>?>(null)
    val userProfile: StateFlow<Map<String, Any>?> = _userProfile

    // Firestore item stream (no Room)
    private val allItems: StateFlow<List<Item>> =
        coupleIdProfile
            .map { it ?: uid }
            .flatMapLatest { activeId -> repo.itemsFlow(activeId) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _partnerUid = MutableStateFlow<String?>(null)
    val partnerUid: StateFlow<String?> = _partnerUid

    val myItems: StateFlow<List<Item>> =
        allItems.map { list -> list.filter { it.createdByUid == uid } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val partnerItems: StateFlow<List<Item>> =
        combine(allItems, partnerUid) { list, pUid ->
            if (pUid == null) emptyList() else list.filter { it.createdByUid == pUid }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var coupleListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var inviteListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var membersListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        if (uid != "anon") {
            viewModelScope.launch {
                try { coupleRepo.ensureUserDoc(uid) }
                catch (e: Exception) { _message.value = "Profile init error: ${e.message}" }
            }

            // Watch user's coupleId field
            coupleListener = coupleRepo.observeMyCoupleId(uid) { newCoupleId ->
                viewModelScope.launch {
                    _coupleIdProfile.value = newCoupleId
                    handleCoupleChange(newCoupleId)
                }
            }

            // Load user profile
            viewModelScope.launch {
                try { _userProfile.value = coupleRepo.getUserProfile(uid) }
                catch (_: Exception) { }
            }

            // Also listen for invites
            inviteListener = coupleRepo.listenForInvites(uid) { linkedId ->
                viewModelScope.launch {
                    _coupleIdProfile.value = linkedId
                    _message.value = "Paired successfully!"
                }
            }
        }
    }

    private suspend fun handleCoupleChange(newCoupleId: String?) {
        try {
            if (newCoupleId != null) {
                val amMember = coupleRepo.isMemberOf(newCoupleId, uid)
                if (!amMember) {
                    coupleRepo.clearMyCouple(uid)
                    _coupleIdProfile.value = null
                } else {
                    coupleRepo.ensureCoupleDoc(newCoupleId, uid)
                    membersListener?.remove()
                    _partnerUid.value = null
                    membersListener = coupleRepo.observeMembers(newCoupleId) { members ->
                        _partnerUid.value = members.firstOrNull { it != uid }
                    }
                }
            }
        } catch (e: Exception) {
            _message.value = "Couple sync error: ${e.message}"
        }
    }

    override fun onCleared() {
        super.onCleared()
        coupleListener?.remove()
        inviteListener?.remove()
        membersListener?.remove()
    }

    fun addItem(title: String) {
        val activeId = coupleIdProfile.value ?: uid
        viewModelScope.launch {
            try {
                coupleRepo.ensureCoupleDoc(activeId, uid)
                repo.addItem(
                    title = title,
                    notes = "",
                    createdByUid = uid,
                    coupleId = activeId
                )

            } catch (e: Exception) {
                _message.value = "Add failed: ${e.message}"
            }
        }
    }

    // Temporarily disabled until Firestore update is implemented
    fun updateItem(itemId: Long, title: String, notes: String) {
        _message.value = "Update not yet implemented for Firestore mode"
    }

    // Temporarily disabled reorder
    fun reorder(idsInOrder: List<Long>) {
        _message.value = "Reorder not yet implemented for Firestore mode"
    }

    fun linkWithPartner(partnerUid: String) {
        if (uid == "anon") { _message.value = "Please sign in first"; return }
        viewModelScope.launch {
            try {
                coupleRepo.linkAndInvite(uid, partnerUid)
                _message.value = "Invitation sent!"
            } catch (e: Exception) {
                _message.value = "Failed to link: ${e.message}"
            }
        }
    }

    fun unlinkPartner() {
        if (uid == "anon") { _message.value = "Please sign in first"; return }
        viewModelScope.launch {
            try {
                coupleRepo.unlinkMe(uid)
                _message.value = "Unlinked successfully."
            } catch (e: Exception) {
                _message.value = "Unlink failed: ${e.message}"
            }
        }
    }

    fun getUserProfile(): Map<String, Any>? = _userProfile.value

    fun updateProfile(nickname: String?, photoUrl: String?) {
        viewModelScope.launch {
            try {
                val resolvedPhotoUrl = when {
                    photoUrl.isNullOrBlank() -> null
                    photoUrl.startsWith("http", true) -> photoUrl
                    else -> photoUrl
                }
                coupleRepo.updateUserProfile(uid, nickname, resolvedPhotoUrl)
                _userProfile.value = coupleRepo.getUserProfile(uid)
                _message.value = "Profile updated successfully"
            } catch (e: Exception) {
                _message.value = "Profile update failed: ${e.message}"
            }
        }
    }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(app) as T
        }
    }
}
