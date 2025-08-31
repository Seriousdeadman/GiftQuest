package com.example.giftquest.ui.screens

import androidx.compose.foundation.layout.*
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
    itemId: Long = -1L,
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
        if (itemId != -1L) {
            val allItems = vm.myItems.value
            val itemToEdit = allItems.find { it.id == itemId }
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
                onClick = { 
                    if (title.isNotBlank()) {
                        if (isEditing) {
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
        }
    }
}
