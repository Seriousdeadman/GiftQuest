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

    // temporary until Firebase is wired
    val user = FirebaseAuth.getInstance().currentUser

    private val UID = user?.uid ?: "anon"
    private val COUPLE_ID = UID

    val items: StateFlow<List<ItemEntity>> =
        repo.itemsFlow(COUPLE_ID)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addItem(title: String) {
        viewModelScope.launch { repo.addItem(title = title, uid = UID, coupleId = COUPLE_ID) }
    }

    fun moveUp(id: Long) { viewModelScope.launch { repo.moveItemUp(id) } }
    fun moveDown(id: Long) { viewModelScope.launch { repo.moveItemDown(id) } }
    fun reorder(idsInOrder: List<Long>) { viewModelScope.launch { repo.reorder(idsInOrder) } }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(app) as T
        }
    }
}
