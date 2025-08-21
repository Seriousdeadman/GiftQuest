package com.example.giftquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.giftquest.ui.heritems.HerItemsViewModel

@Composable
fun HerItemsScreen(
    onBack: (() -> Unit)? = null
) {
    val vm: HerItemsViewModel = viewModel()
    val ui by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        if (ui.myShareCode.isBlank()) vm.ensureShareCode()
    }

    if (ui.coupleId == null) {
        // Not linked yet -> show share & link UI
        NotLinkedContent(
            loading = ui.loading,
            myCode = ui.myShareCode,
            error = ui.error,
            onRefreshCode = { vm.ensureShareCode() },
            onLink = { code -> vm.linkWith(code) }
        )
    } else {
        // Linked -> show partner's items
        PartnerItemsList(
            loading = ui.loading,
            items = ui.partnerItems,
            error = ui.error
        )
    }
}

@Composable
private fun NotLinkedContent(
    loading: Boolean,
    myCode: String,
    error: String?,
    onRefreshCode: () -> Unit,
    onLink: (String) -> Unit
) {
    var partnerCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxSize()
    ) {
        Text("Her Items", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(
            "You haven't linked a partner yet. Share your code below with your partner, " +
                    "or paste your partner's code to link and view their items."
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = myCode,
            onValueChange = {},
            readOnly = true,
            label = { Text("Your share code") },
            trailingIcon = {
                IconButton(onClick = onRefreshCode, enabled = !loading) {
                    Icon(Icons.Default.Link, contentDescription = "Refresh code")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = partnerCode,
            onValueChange = { partnerCode = it },
            label = { Text("Enter partner's code") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { onLink(partnerCode) },
            enabled = !loading && partnerCode.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(if (loading) "Linking..." else "Link Partner")
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PartnerItemsList(
    loading: Boolean,
    items: List<com.example.giftquest.data.model.Item>,
    error: String?
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Her Items",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 8.dp)
        )
        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        if (loading && items.isEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { item ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        if (item.notes.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(item.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
