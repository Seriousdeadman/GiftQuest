package com.example.giftquest.ui.screens

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.giftquest.ui.components.AppTopBar
import com.example.giftquest.ui.home.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun ProfileScreen(
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val context = LocalContext.current
    val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    
    var nickname by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoPickerDialog by remember { mutableStateOf(false) }
    
    val currentUser = FirebaseAuth.getInstance().currentUser
    val uid = currentUser?.uid ?: ""

    // Photo picker launcher for gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            photoUrl = it.toString()
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri?.let { uri ->
                photoUrl = uri.toString()
            }
        }
    }

    // Function to create camera file
    fun createCameraFile(): Uri? {
        val photoFile = File.createTempFile(
            "profile_photo_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        )
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }

    // Load current profile data
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            try {
                val profile = vm.getUserProfile()
                nickname = profile?.get("nickname") as? String ?: ""
                photoUrl = profile?.get("photoUrl") as? String ?: ""
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(topBar = { 
        AppTopBar(
            title = "Edit Profile", 
            onBack = onBack
        ) 
    }) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo Section
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    if (photoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Default Profile",
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Camera button - now functional
                    FloatingActionButton(
                        onClick = { 
                            showPhotoPickerDialog = true
                        },
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Nickname Field
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname") },
                    placeholder = { Text("Enter your nickname") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Save Button
                Button(
                    onClick = {
                        vm.updateProfile(nickname.takeIf { it.isNotBlank() }, photoUrl.takeIf { it.isNotBlank() })
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = nickname.isNotBlank()
                ) {
                    Text("Save Profile")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Current UID (read-only)
                OutlinedTextField(
                    value = uid,
                    onValueChange = { },
                    label = { Text("User ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    readOnly = true
                )
            }
        }
        
        // Photo Picker Dialog
        if (showPhotoPickerDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoPickerDialog = false },
                title = { Text("Choose Photo") },
                text = { Text("Select how you want to add a profile photo") },
                confirmButton = {
                    Column {
                        TextButton(
                            onClick = {
                                showPhotoPickerDialog = false
                                galleryLauncher.launch("image/*")
                            }
                        ) {
                            Text("Gallery")
                        }
                        TextButton(
                            onClick = {
                                showPhotoPickerDialog = false
                                try {
                                    val cameraUri = createCameraFile()
                                    if (cameraUri != null) {
                                        selectedImageUri = cameraUri
                                        cameraLauncher.launch(cameraUri)
                                    } else {
                                        Toast.makeText(context, "Camera URI creation failed", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        ) {
                            Text("Camera")
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
