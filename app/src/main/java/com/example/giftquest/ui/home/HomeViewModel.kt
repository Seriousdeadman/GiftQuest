package com.example.giftquest.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.giftquest.GiftQuestApplication
import com.example.giftquest.data.CoupleRepository
import com.example.giftquest.data.ItemsRepository
import com.example.giftquest.data.local.ItemEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as GiftQuestApplication).database
    private val repo = ItemsRepository(db.itemDao())
    private val coupleRepo = CoupleRepository()

    private val uid: String = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun consumeMessage() { _message.value = null }

    private val _coupleIdProfile = MutableStateFlow<String?>(null)
    val coupleIdProfile: StateFlow<String?> = _coupleIdProfile

    // All items mirrored for the active bucket (coupleId if linked, else my uid personal bucket)
    private val allItems: StateFlow<List<ItemEntity>> =
        coupleIdProfile
            .map { it ?: uid }
            .flatMapLatest { activeId -> repo.itemsFlow(activeId) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Partner UID (null until linked)
    private val _partnerUid = MutableStateFlow<String?>(null)
    val partnerUid: StateFlow<String?> = _partnerUid

    // Public flows for the UI
    val myItems: StateFlow<List<ItemEntity>> =
        allItems.map { list -> list.filter { it.createdByUid == uid } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val partnerItems: StateFlow<List<ItemEntity>> =
        combine(allItems, partnerUid) { list, pUid ->
            if (pUid == null) emptyList() else list.filter { it.createdByUid == pUid }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var syncJob: Job? = null
    private var coupleListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var inviteListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var membersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var coupleDocListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        // Only start listeners if we have a signed-in user
        if (uid != "anon") {
            try {
                // Ensure user document exists
                viewModelScope.launch {
                    try {
                        coupleRepo.ensureUserDoc(uid)
                    } catch (e: Exception) {
                        _message.value = "Failed to create user profile: ${e.message}"
                    }
                }

                // Watch my users/{uid}.coupleId
                coupleListener = coupleRepo.observeMyCoupleId(uid) { newCoupleId ->
                    viewModelScope.launch {
                        _coupleIdProfile.value = newCoupleId

                        try {
                            // If linked, validate membership; otherwise clear bogus coupleId
                            if (newCoupleId != null) {
                                val amMember = coupleRepo.isMemberOf(newCoupleId, uid)
                                if (!amMember) {
                                    coupleRepo.clearMyCouple(uid)
                                    _coupleIdProfile.value = null
                                }
                            }

                            // Ensure active bucket (personal or couple) exists
                            val activeId = newCoupleId ?: uid
                            try { coupleRepo.ensureCoupleDoc(activeId, uid) } catch (_: Exception) {}

                            // If linked, watch members and derive partner UID
                            membersListener?.remove()
                            coupleDocListener?.remove()
                            _partnerUid.value = null
                            if (newCoupleId != null) {
                                membersListener = coupleRepo.observeMembers(newCoupleId) { members ->
                                    _partnerUid.value = members.firstOrNull { it != uid }
                                    
                                    // Check if I'm still a member, if not clear my coupleId
                                    if (uid !in members) {
                                        viewModelScope.launch {
                                            coupleRepo.clearMyCouple(uid)
                                            _coupleIdProfile.value = null
                                            _message.value = "Partner unlinked"
                                        }
                                    }
                                }
                            }

                            // Restart Firestore -> Room sync
                            syncJob?.cancel()
                            syncJob = launch { repo.startSync(activeId) }
                        } catch (e: Exception) {
                            _message.value = "Startup sync error: ${e.message}"
                        }
                    }
                }

                // Also listen for invites so the OTHER device auto-links
                inviteListener = coupleRepo.listenForInvites(uid) { linkedId ->
                    viewModelScope.launch {
                        _coupleIdProfile.value = linkedId
                        _message.value = "Paired successfully!"
                    }
                }
            } catch (e: Exception) {
                _message.value = "Init error: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        coupleListener?.remove()
        inviteListener?.remove()
        membersListener?.remove()
        coupleDocListener?.remove()
        repo.stopSync()
    }

    fun addItem(title: String) {
        val activeId = coupleIdProfile.value ?: uid
        viewModelScope.launch {
            try {
                // Make sure couples/{activeId} exists & includes me
                try { coupleRepo.ensureCoupleDoc(activeId, uid) } catch (_: Exception) {}
                repo.addItem(title = title, coupleId = activeId, uid = uid)
            } catch (e: Exception) {
                _message.value = "Add failed: ${e.message}"
            }
        }
    }

    fun updateItem(itemId: Long, title: String, notes: String) {
        val activeId = coupleIdProfile.value ?: uid
        viewModelScope.launch {
            try {
                repo.updateItem(itemId = itemId, title = title, notes = notes, coupleId = activeId)
            } catch (e: Exception) {
                _message.value = "Update failed: ${e.message}"
            }
        }
    }

    fun reorder(idsInOrder: List<Long>) {
        val activeId = coupleIdProfile.value ?: uid
        viewModelScope.launch { repo.reorder(idsInOrder, activeId) }
    }

    fun linkWithPartner(partnerUid: String) {
        viewModelScope.launch {
            try {
                _message.value = "Linking…"
                
                // Validate partner UID format (basic check)
                if (partnerUid.length < 20) {
                    _message.value = "Invalid partner code format"
                    return@launch
                }
                
                coupleRepo.linkAndInvite(uid, partnerUid)
                
                // Migration will happen automatically when the coupleId changes
                _message.value = "Invitation sent! Your partner will be notified automatically."
            } catch (e: IllegalArgumentException) {
                _message.value = e.message ?: "Invalid partner code"
            } catch (e: IllegalStateException) {
                _message.value = e.message ?: "Linking failed"
            } catch (e: Exception) {
                if (e.message?.contains("PERMISSION_DENIED") == true) {
                    _message.value = "Permission denied. Please check your internet connection and try again."
                } else {
                    _message.value = "Failed to link: ${e.message}"
                }
            }
        }
    }


    fun unlinkPartner() {
        viewModelScope.launch {
            try {
                _message.value = "Unlinking…"
                println("DEBUG: Starting unlink process for user: $uid")
                coupleRepo.unlinkMe(uid)
                println("DEBUG: Unlink process completed successfully")
                _message.value = "Unlinked successfully."
            } catch (e: Exception) {
                println("DEBUG: Unlink failed with error: ${e.message}")
                _message.value = "Failed to unlink: ${e.message}"
            }
        }
    }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(app) as T
        }
    }
}
