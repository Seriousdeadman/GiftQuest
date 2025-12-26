package com.example.giftquest.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.giftquest.data.ItemsRepository
import com.example.giftquest.data.model.Item
import com.example.giftquest.data.remote.PairingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val itemsRepo = ItemsRepository()
    private val pairingRepo = PairingRepository()

    private val uid: String = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun consumeMessage() { _message.value = null }

    // Partner's UID (null if not linked)
    private val _partnerUid = MutableStateFlow<String?>(null)
    val partnerUid: StateFlow<String?> = _partnerUid

    // My items (always from users/{myUid}/items/)
    val myItems: StateFlow<List<Item>> = itemsRepo.itemsFlow(uid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Partner's items (from users/{partnerUid}/items/ if linked)
    val partnerItems: StateFlow<List<Item>> = _partnerUid
        .flatMapLatest { pUid ->
            if (pUid == null) flowOf(emptyList())
            else itemsRepo.itemsFlow(pUid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var linkListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        if (uid != "anon") {
            // Listen for link status changes
            linkListener = pairingRepo.observeMyLinkStatus(uid) { linkedWithUid ->
                viewModelScope.launch {
                    _partnerUid.value = linkedWithUid
                    if (linkedWithUid != null) {
                        _message.value = "Linked with partner!"
                    } else {
                        _message.value = "Unlinked from partner"
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        linkListener?.remove()
    }

    fun addItem(title: String) {
        android.util.Log.d("GiftQuest", "=== ADD ITEM START ===")
        android.util.Log.d("GiftQuest", "Title: $title")
        android.util.Log.d("GiftQuest", "UID: $uid")

        if (uid == "anon") {
            android.util.Log.e("GiftQuest", "ERROR: Not signed in!")
            _message.value = "Please sign in first"
            return
        }

        viewModelScope.launch {
            try {
                android.util.Log.d("GiftQuest", "Calling itemsRepo.addItem...")

                itemsRepo.addItem(
                    title = title,
                    uid = uid,
                    coupleId = uid  // Not used but required by signature
                )

                android.util.Log.d("GiftQuest", "✅ Item added successfully!")
                _message.value = "Item added!"

            } catch (e: Exception) {
                android.util.Log.e("GiftQuest", "❌ Add failed: ${e.message}", e)
                _message.value = "Add failed: ${e.message}"
            }
        }



    }

    fun updateItem(remoteId: String, title: String, notes: String) {
        viewModelScope.launch {
            try {
                itemsRepo.updateItem(remoteId, title, notes, uid)
                _message.value = "Item updated!"
            } catch (e: Exception) {
                _message.value = "Update failed: ${e.message}"
            }
        }
    }

    fun deleteItem(remoteId: String) {
        viewModelScope.launch {
            try {
                itemsRepo.deleteItem(remoteId, uid)
                _message.value = "Item deleted!"
            } catch (e: Exception) {
                _message.value = "Delete failed: ${e.message}"
            }
        }
    }

    fun reorder(remoteIdsInOrder: List<String>) {
        viewModelScope.launch {
            try {
                itemsRepo.reorder(remoteIdsInOrder, uid)
            } catch (e: Exception) {
                _message.value = "Reorder failed: ${e.message}"
            }
        }
    }

    fun unlinkPartner() {
        if (uid == "anon") {
            _message.value = "Please sign in first"
            return
        }

        viewModelScope.launch {
            try {
                pairingRepo.unlinkMe(uid)
                _message.value = "Unlinked successfully"
            } catch (e: Exception) {
                _message.value = "Unlink failed: ${e.message}"
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