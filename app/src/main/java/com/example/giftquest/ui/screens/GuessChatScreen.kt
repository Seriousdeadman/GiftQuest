package com.example.giftquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.giftquest.ui.components.AppTopBar
import com.example.giftquest.ui.guess.GuessViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun GuessChatScreen(
    itemId: String?,  // ✅ Changed from Long to String?
    onBack: () -> Unit
) {
    val vm: GuessViewModel = viewModel()

    // Handle null itemId gracefully
    if (itemId.isNullOrBlank()) {
        // Show error state or navigate back
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("Invalid item", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    // Collect the flow of all guesses and filter by itemId for this screen
    val allGuesses by vm.guessesFlow.collectAsState()
    val guessesForItem = remember(allGuesses, itemId) {
        allGuesses.filter { it.itemId == itemId }  // ✅ Now comparing String to String
    }

    var input by remember { mutableStateOf("") }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "anon"

    Scaffold(
        topBar = { AppTopBar(title = "Guess Chat", onBack = onBack) },
        bottomBar = {
            Row(
                Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type your guess…") }
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val text = input.trim()
                        if (text.isNotEmpty()) {
                            vm.addGuess(itemId = itemId, text = text, uid = currentUid)
                            input = ""
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (guessesForItem.isEmpty()) {
                item {
                    Text(
                        "No guesses yet. Start the conversation!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(guessesForItem) { g ->
                    val label = if (g.guessedByUid == currentUid) "You" else "Partner"
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (g.guessedByUid == currentUid)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                g.guessText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}