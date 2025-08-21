package com.example.giftquest.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.giftquest.data.auth.AuthRepository
import com.example.giftquest.data.user.UserProfile
import com.example.giftquest.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SignUpUiState(
    val loading: Boolean = false,
    val error: String? = null
)

class SignUpViewModel(
    private val auth: AuthRepository = AuthRepository(),
    private val users: UserRepository = UserRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state

    fun signUp(
        name: String,
        nickname: String,
        email: String,
        password: String,
        dateOfBirth: String,
        photoUrl: String?,
        onSuccess: () -> Unit
    ) {
        _state.value = SignUpUiState(loading = true, error = null)
        viewModelScope.launch {
            try {
                // 1) Create Auth user FIRST
                auth.signUp(email, password)
                val uid = auth.uid ?: throw IllegalStateException("Signed up, but no UID")

                // 2) Immediately navigate forward (don’t block UI)
                _state.value = SignUpUiState(loading = false, error = null)
                onSuccess()

                // 3) Fire-and-forget profile save (won’t block the UI)
                viewModelScope.launch {
                    try {
                        users.saveUser(
                            UserProfile(
                                uid = uid,
                                name = name,
                                nickname = nickname,
                                email = email,
                                photoUrl = photoUrl,
                                dateOfBirth = dateOfBirth
                            )
                        )
                    } catch (e: Exception) {
                        // Optional: log or surface somewhere non-blocking
                        // e.g., Log.e("SignUp", "Profile save failed", e)
                    }
                }
            } catch (e: Exception) {
                _state.value = SignUpUiState(loading = false, error = e.message ?: "Sign up failed")
            }
        }
    }
}
