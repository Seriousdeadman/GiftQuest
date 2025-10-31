package com.example.giftquest.ui.guess

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Temporary Firestore-ready GuessViewModel.
 * Local Room references are removed.
 * Later, you'll connect this to a Firestore "guesses" collection.
 */
class GuessViewModel(app: Application) : AndroidViewModel(app) {

    // Placeholder until Firestore version implemented
    private val _guesses = MutableStateFlow<List<Guess>>(emptyList())
    val guessesFlow: StateFlow<List<Guess>> =
        _guesses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addGuess(itemId: Long, text: String, uid: String) {
        if (uid == "anon") return
        viewModelScope.launch {
            // TODO: connect this with Firestore (e.g. add to /guesses collection)
            val newGuess = Guess(itemId, uid, text)
            _guesses.value = _guesses.value + newGuess
        }
    }

    data class Guess(
        val itemId: Long,
        val guessedByUid: String,
        val guessText: String
    )
}
