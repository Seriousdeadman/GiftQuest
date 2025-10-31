package com.example.giftquest.ui.partner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giftquest.data.remote.PairingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PartnerLinkUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val myShareCode: String = "",
    val coupleId: String? = null
)

class PartnerLinkViewModel(
    private val repo: PairingRepository = PairingRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(PartnerLinkUiState())
    val state: StateFlow<PartnerLinkUiState> = _state

    fun loadShareCode() {
        val uid = auth.currentUser?.uid ?: return
        if (uid.isNullOrBlank()) {
            _state.value = _state.value.copy(error = "Please sign in first")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val code = repo.ensureMyShareCode(uid)
                _state.value = _state.value.copy(loading = false, myShareCode = code)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    fun linkWith(code: String, onLinked: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _state.value = _state.value.copy(error = "Please sign in first")
            return
        }

        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val cid = repo.linkWithPartnerCode(uid, code.trim())
                _state.value = _state.value.copy(loading = false, coupleId = cid)
                onLinked(cid)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Link failed")
            }
        }
    }
}
