package com.giftquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerLinkScreen(onBack: () -> Unit) {
    val token = remember { UUID.randomUUID().toString().take(8) } // mock token

    Scaffold(
        topBar = { TopAppBar(title = { Text("Partner Link") }) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Share this code with your partner:")
            Spacer(Modifier.height(12.dp))
            ElevatedCard { Text(token, modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.headlineMedium) }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Back") }
        }
    }
}
