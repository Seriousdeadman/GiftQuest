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

    // Local Room -> UI stream (uses activeId = coupleId if linked, else my uid personal bucket)
    val items: StateFlow<List<ItemEntity>> =
        coupleIdProfile
            .map { it ?: uid }
            .flatMapLatest { activeId -> repo.itemsFlow(activeId) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var syncJob: Job? = null
    private var coupleListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var inviteListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        // Only start listeners if we have a signed-in user
        if (uid != "anon") {
            try {
                // 1) Listen to my users/{uid}.coupleId changes
                coupleListener = coupleRepo.observeMyCoupleId(uid) { newCoupleId ->
                    viewModelScope.launch {
                        _coupleIdProfile.value = newCoupleId

                        try {
                            // If linked, make sure I'm actually a member; if not, clear bad state
                            if (newCoupleId != null) {
                                val amMember = coupleRepo.isMemberOf(newCoupleId, uid)
                                if (!amMember) {
                                    coupleRepo.clearMyCouple(uid)
                                    _coupleIdProfile.value = null
                                }
                            }

                            // Ensure personal or couple bucket exists before we start listening/writing
                            val activeId = newCoupleId ?: uid
                            try {
                                coupleRepo.ensureCoupleDoc(activeId, uid)
                            } catch (_: Exception) {
                                // best-effort; listener below is error-safe
                            }

                            // Restart Firestore -> Room sync for the activeId
                            syncJob?.cancel()
                            syncJob = launch { repo.startSync(activeId) }
                        } catch (e: Exception) {
                            _message.value = "Startup sync error: ${e.message}"
                        }
                    }
                }

                // 2) Also listen for partner invites so the OTHER device auto-links
                inviteListener = coupleRepo.listenForInvites(uid) { linkedId ->
                    viewModelScope.launch {
                        _coupleIdProfile.value = linkedId
                        _message.value = "Paired!"
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
        repo.stopSync()
    }

    fun addItem(title: String) {
        val activeId = coupleIdProfile.value ?: uid
        viewModelScope.launch {
            try {
                // Guarantee couples/{activeId} exists & includes me (helps avoid rules races)
                try {
                    coupleRepo.ensureCoupleDoc(activeId, uid)
                } catch (_: Exception) { /* ignore */ }

                repo.addItem(title = title, coupleId = activeId, uid = uid)
            } catch (e: Exception) {
                _message.value = "Add failed: ${e.message}"
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
                coupleRepo.linkAndInvite(uid, partnerUid)
                val coupleId = coupleRepo.deterministicCoupleId(uid, partnerUid)

                // Move any old personal lists into the new couple bucket (idempotent)
                try {
                    coupleRepo.migrateOldListsToCouple(uid, partnerUid, coupleId)
                } catch (_: Exception) { /* non-fatal */ }

                _message.value = "Linked!"
            } catch (e: Exception) {
                _message.value = "Failed to link: ${e.message}"
            }
        }
    }

    fun unlinkPartner() {
        viewModelScope.launch {
            try {
                _message.value = "Unlinking…"
                coupleRepo.unlinkMe(uid)
                _message.value = "Unlinked."
            } catch (e: Exception) {
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
