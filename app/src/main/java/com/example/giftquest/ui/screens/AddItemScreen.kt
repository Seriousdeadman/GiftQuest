package com.example.giftquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.giftquest.ui.components.AppTopBar

@Composable
fun AddItemScreen(
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var hints by remember { mutableStateOf("") }

    Scaffold(topBar = { AppTopBar(title = "Add Item", onBack = onBack) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize()
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title (secret)") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = hints, onValueChange = { hints = it },
                label = { Text("Hints (comma separated)") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { if (title.isNotBlank()) onSave(title) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Save") }
        }
    }
}
