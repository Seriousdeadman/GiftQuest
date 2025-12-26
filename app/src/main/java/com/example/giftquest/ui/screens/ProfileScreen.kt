package com.example.giftquest.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.giftquest.ui.components.AppTopBar
import com.example.giftquest.ui.profile.ProfileViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    var showPhotoPickerDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Photo picker launcher for gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val success = viewModel.uploadPhotoAndSave(it)
                if (success) {
                    Toast.makeText(context, "Photo uploaded and saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Photo upload failed. Check logs.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Camera launcher - MUST be declared before cameraPermissionLauncher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            scope.launch {
                val uploaded = viewModel.uploadPhotoAndSave(tempCameraUri!!)
                if (uploaded) {
                    Toast.makeText(context, "Photo uploaded and saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Photo upload failed. Check logs.", Toast.LENGTH_LONG).show()
                }
            }
        } else if (!success) {
            Toast.makeText(context, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera permission launcher - uses cameraLauncher, so must come after
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed with camera
            val cameraUri = createCameraFile(context)
            if (cameraUri != null) {
                tempCameraUri = cameraUri
                cameraLauncher.launch(cameraUri)
            }
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Edit Profile",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()  // ← Keyboard padding
                .verticalScroll(rememberScrollState())  // ← Scrollable
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Show loading indicator
            if (state.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Loading...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                return@Column
            }

            // Show error message
            state.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Text("✕")
                        }
                    }
                }
            }

            // Profile Photo Section
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (state.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = state.photoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        onError = {
                            android.util.Log.e("GiftQuest", "Failed to load image: ${state.photoUrl}")
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Default Profile",
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Camera button
                FloatingActionButton(
                    onClick = { showPhotoPickerDialog = true },
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Photo",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Helper text
            Text(
                text = if (state.photoUrl.isNotEmpty()) "Tap camera to change photo" else "Tap camera to add photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nickname Field
            OutlinedTextField(
                value = state.nickname,
                onValueChange = { viewModel.updateNickname(it) },
                label = { Text("Nickname") },
                placeholder = { Text("Enter your nickname") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    viewModel.saveProfile()
                    Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = state.nickname.isNotBlank() && !state.loading
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Profile")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "ℹ️ Note",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Photos are uploaded and saved immediately. Your nickname is saved when you tap 'Save Profile'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current UID (read-only) - for debugging
            OutlinedTextField(
                value = state.uid,
                onValueChange = { },
                label = { Text("User ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                enabled = false
            )
        }

        // Photo Picker Dialog
        if (showPhotoPickerDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoPickerDialog = false },
                title = { Text("Choose Photo") },
                text = { Text("Select how you want to add a profile photo") },
                confirmButton = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                showPhotoPickerDialog = false
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📷 Choose from Gallery")
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                showPhotoPickerDialog = false
                                // Check camera permission first
                                when (PackageManager.PERMISSION_GRANTED) {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) -> {
                                        // Permission already granted
                                        val cameraUri = createCameraFile(context)
                                        if (cameraUri != null) {
                                            tempCameraUri = cameraUri
                                            cameraLauncher.launch(cameraUri)
                                        }
                                    }
                                    else -> {
                                        // Request permission
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📸 Take Photo")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPhotoPickerDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Helper function to create a file URI for camera photos
 */
private fun createCameraFile(context: android.content.Context): Uri? {
    return try {
        // Create temp file in cache directory
        val photoFile = File.createTempFile(
            "profile_photo_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        ).apply {
            createNewFile()
            deleteOnExit()
        }

        android.util.Log.d("GiftQuest", "Created camera file: ${photoFile.absolutePath}")

        // Get URI using FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )

        android.util.Log.d("GiftQuest", "Camera file URI: $uri")
        uri

    } catch (e: Exception) {
        android.util.Log.e("GiftQuest", "Failed to create camera file", e)
        Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
        null
    }
}