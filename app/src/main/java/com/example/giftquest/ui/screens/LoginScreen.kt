package com.example.giftquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.giftquest.ui.login.LoginViewModel
import com.example.giftquest.utils.DevMode

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var autoLoginAttempted by remember { mutableStateOf(false) }

    // 🔧 DEV MODE: Auto-login for whitelisted devices only
    LaunchedEffect(Unit) {
        if (DevMode.DEV_MODE_ENABLED && !autoLoginAttempted) {
            autoLoginAttempted = true

            val deviceConfig = DevMode.getDeviceConfig()
            if (deviceConfig != null) {
                android.util.Log.d("DevMode", "Auto-login enabled for this device")
                viewModel.signIn(
                    email = deviceConfig.email,
                    password = deviceConfig.password,
                    onSuccess = onLoggedIn
                )
            } else {
                android.util.Log.d("DevMode", "Device not whitelisted - showing login screen")
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()  // ← Keyboard padding
                .verticalScroll(rememberScrollState())  // ← Scrollable
                .padding(horizontal = 24.dp, vertical = 24.dp),  // More top padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)  // Fixed spacing instead of Center
        ) {

            // Show loading or login form
            if (state.loading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(
                    if (DevMode.isDeviceWhitelisted()) "Auto-logging in..." else "Logging in...",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                // App branding
                Text(
                    "GiftQuest",
                    style = MaterialTheme.typography.displayMedium
                )

                Text(
                    "Gift Discovery Made Easy",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(48.dp))

                // Error message
                state.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Login form (shown for non-whitelisted devices or if auto-login fails)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { /* Focus moves to password automatically */ }
                    )
                )

                Spacer(Modifier.height(2.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                viewModel.signIn(
                                    email = email.trim(),
                                    password = password,
                                    onSuccess = onLoggedIn
                                )
                            }
                        }
                    )
                )

                Spacer(Modifier.height(1.dp))

                // Forgot password
                TextButton(
                    onClick = onForgotPassword,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot Password?")
                }

                Spacer(Modifier.height(16.dp))

                // Login button
                Button(
                    onClick = {
                        viewModel.signIn(
                            email = email.trim(),
                            password = password,
                            onSuccess = onLoggedIn
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = email.isNotBlank() && password.isNotBlank()
                ) {
                    Text("Log In", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(1.dp))

                // Sign up
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Don't have an account?")
                    TextButton(onClick = onCreateAccount) {
                        Text("Sign Up")
                    }
                }
            }
        }
    }
}