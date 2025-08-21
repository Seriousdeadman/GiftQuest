package com.example.giftquest.ui.heritems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giftquest.data.model.Item
import com.example.giftquest.data.remote.CloudItemsRepository
import com.example.giftquest.data.remote.PairingRepository
import com.example.giftquest.data.user.UserDoc
import com.example.giftquest.data.user.UserDocRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HerItemsUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val me: UserDoc? = null,
    val myShareCode: String = "",
    val coupleId: String? = null,
    val partnerItems: List<Item> = emptyList()
)

class HerItemsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepo: UserDocRepository = UserDocRepository(),
    private val pairRepo: PairingRepository = PairingRepository(),
    private val cloudItems: CloudItemsRepository = CloudItemsRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(HerItemsUiState(loading = true))
    val state: StateFlow<HerItemsUiState> = _state.asStateFlow()

    private var itemsJobActiveCid: String? = null

    init {
        // Observe my user doc (to get coupleId & myShareCode)
        viewModelScope.launch {
            userRepo.meFlow().collect { me ->
                val uid = auth.currentUser?.uid
                val cid = me?.coupleId
                _state.update {
                    it.copy(
                        me = me,
                        coupleId = cid,
                        loading = false,
                        error = null,
                        myShareCode = me?.myShareCode ?: it.myShareCode
                    )
                }
                if (uid != null && cid != null) {
                    startPartnerItems(uid, cid)
                }
            }
        }
    }

    private fun startPartnerItems(myUid: String, coupleId: String) {
        if (itemsJobActiveCid == coupleId) return
        itemsJobActiveCid = coupleId
        viewModelScope.launch {
            cloudItems.itemsFlow(coupleId).collect { list ->
                val partnerOnly = list.filter { it.createdByUid != myUid }
                _state.update { it.copy(partnerItems = partnerOnly) }
            }
        }
    }

    fun ensureShareCode() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _state.update { it.copy(loading = true, error = null) }
                val code = pairRepo.ensureMyShareCode(uid)
                _state.update { it.copy(loading = false, myShareCode = code) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Error") }
            }
        }
    }

    fun linkWith(partnerCode: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _state.update { it.copy(loading = true, error = null) }
                val cid = pairRepo.linkWithPartnerCode(uid, partnerCode.trim())
                _state.update { it.copy(loading = false, coupleId = cid) }
                // Streaming starts automatically via init observer
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Link failed") }
            }
        }
    }
}
