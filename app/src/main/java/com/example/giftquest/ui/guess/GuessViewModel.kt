package com.example.giftquest.ui.guess

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.giftquest.GiftQuestApplication
import com.example.giftquest.data.local.GuessEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GuessViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as GiftQuestApplication).database
    private val guessDao = db.guessDao()

    val guessesFlow = guessDao.getAllGuesses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addGuess(itemId: Long, text: String, uid: String) {
        viewModelScope.launch {
            guessDao.insert(
                GuessEntity(
                    itemId = itemId,
                    guessedByUid = uid,
                    guessText = text
                )
            )
        }
    }
}
