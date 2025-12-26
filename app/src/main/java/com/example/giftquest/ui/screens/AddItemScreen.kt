package com.example.giftquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.giftquest.ui.components.AppTopBar
import com.example.giftquest.ui.home.HomeViewModel
import android.app.Application
import androidx.compose.ui.platform.LocalContext

@Composable
fun AddItemScreen(
    itemId: String? = null,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))

    var title by remember { mutableStateOf("") }
    var hints by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    // Load existing item data if editing
    LaunchedEffect(itemId) {
        if (!itemId.isNullOrBlank()) {
            val allItems = vm.myItems.value
            val itemToEdit = allItems.find { it.remoteId == itemId.toString() }
            if (itemToEdit != null) {
                title = itemToEdit.title
                hints = itemToEdit.notes
                isEditing = true
            }
        }
    }

    Scaffold(topBar = {
        AppTopBar(
            title = if (isEditing) "Edit Item" else "Add Item",
            onBack = onBack
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()  // ← Keyboard padding
                .verticalScroll(rememberScrollState())  // ← Scrollable
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title (secret)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = hints,
                onValueChange = { hints = it },
                label = { Text("Hints (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        if (isEditing && itemId != null) {
                            vm.updateItem(itemId, title, hints)
                        } else {
                            onSave(title)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (isEditing) "Update" else "Save")
            }

            // Add delete button if editing
            if (isEditing && itemId != null) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        vm.deleteItem(itemId)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Item")
                }
            }
        }
    }
}