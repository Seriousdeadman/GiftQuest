package com.example.giftquest.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.giftquest.ui.signup.SignUpViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignedUp: () -> Unit
) {
    val vm: SignUpViewModel = viewModel()
    val ui by vm.state.collectAsState()

    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // DOB
    val dateFormatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    var dob by remember { mutableStateOf("") } // "YYYY-MM-DD"
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Photo (local only for now)
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) photoUri = uri }

    // Simple validators
    fun isValidEmail(x: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(x).matches()
    fun canSubmit(): Boolean =
        name.isNotBlank() &&
                nickname.isNotBlank() &&
                isValidEmail(email.trim()) &&
                password.length >= 6 &&
                dob.isNotBlank()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val local = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        dob = local.format(dateFormatter)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Create account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Photo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center
            ) {
                if (photoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Add\nphoto", color = Color.Gray)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { pickPhoto.launch("image/*") }) {
                Text("Choose Photo")
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Nickname") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (min 6)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { showDatePicker = true }) { Text("Pick") }
            }

            // Error from ViewModel or local validation hint
            if (ui.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(ui.error ?: "", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!canSubmit()) {
                        // show a friendly message by setting a “fake” error in the VM state:
                        // easiest is just a local Snackbar; but to keep things simple:
                        // You can also hook this into VM if you like.
                        // For now, do nothing and rely on visible fields; or add basic checks:
                        return@Button
                    }
                    // Photo upload comes later; pass null for now
                    vm.signUp(
                        name = name.trim(),
                        nickname = nickname.trim(),
                        email = email.trim(),
                        password = password,
                        dateOfBirth = dob.trim(),
                        photoUrl = null,
                        onSuccess = onSignedUp
                    )
                },
                enabled = !ui.loading && canSubmit(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (ui.loading) "Creating..." else "Create Account")
            }
        }
    }
}
