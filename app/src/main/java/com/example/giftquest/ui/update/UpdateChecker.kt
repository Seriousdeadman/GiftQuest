package com.example.giftquest.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.giftquest.data.remote.AppConfig
import com.example.giftquest.data.remote.RemoteConfigRepository
import kotlinx.coroutines.flow.catch

private const val TAG = "GiftQuest"

/** Drop this anywhere in your NavHost / GiftQuestApp to get automatic update prompts */
@Composable
fun UpdateChecker(currentVersion: Int) {
    val context = LocalContext.current
    var config by remember { mutableStateOf<AppConfig?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        RemoteConfigRepository().appConfigFlow()
            .catch { Log.w(TAG, "UpdateChecker flow error: $it") }
            .collect { config = it }
    }

    val cfg = config ?: return
    if (!dismissed && cfg.latestVersion > currentVersion && cfg.downloadUrl.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { dismissed = true },
            title = { Text("Update Available 🎁") },
            text = {
                Text(
                    "A new version of GiftQuest is available (v${cfg.latestVersion}). " +
                            "Update now for the latest features and fixes."
                )
            },
            confirmButton = {
                Button(onClick = {
                    openUrl(context, cfg.downloadUrl)
                    dismissed = true
                }) { Text("Update Now") }
            },
            dismissButton = {
                TextButton(onClick = { dismissed = true }) { Text("Later") }
            }
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open URL: ${e.message}")
    }
}