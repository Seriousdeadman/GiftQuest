package com.example.giftquest.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.giftquest.GiftQuestApplication
import com.example.giftquest.data.ItemsRepository
import com.example.giftquest.data.local.ItemEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val db = (app as GiftQuestApplication).database
    private val repo = ItemsRepository(db.itemDao())

    private val uid: String? = FirebaseAuth.getInstance().currentUser?.uid
    private val coupleId: String? = uid   // TEMP: couple == yourself

    // If not logged in we expose an empty stream; HomeScreen should only reach here when logged-in
    val items: StateFlow<List<ItemEntity>> =
        if (coupleId != null)
            repo.itemsFlow(coupleId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        else
            kotlinx.coroutines.flow.flowOf(emptyList<ItemEntity>())
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Start Firestore -> Room mirroring when we have an authenticated user
        coupleId?.let { repo.startSync(it) }
    }

    override fun onCleared() {
        // Stop listener to avoid leaks
        repo.stopSync()
        super.onCleared()
    }

    fun addItem(title: String) {
        val u = uid ?: return    // not logged in; ignore
        val c = coupleId ?: return
        viewModelScope.launch {
            repo.addItem(title = title, uid = u, coupleId = c)
        }
    }

    fun moveUp(id: Long) {
        val c = coupleId ?: return
        viewModelScope.launch { repo.moveItemUp(id, c) }
    }

    fun moveDown(id: Long) {
        val c = coupleId ?: return
        viewModelScope.launch { repo.moveItemDown(id, c) }
    }

    fun reorder(idsInOrder: List<Long>) {
        val c = coupleId ?: return
        viewModelScope.launch { repo.reorder(idsInOrder, c) }
    }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(app) as T
        }
    }
}
