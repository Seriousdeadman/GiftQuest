package com.example.giftquest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.giftquest.ui.login.LoginViewModel

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onSkip: (() -> Unit)? = null, // optional "skip" you had before
    onCreateAccount: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val vm: LoginViewModel = viewModel()
    val ui by vm.state.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to GiftQuest", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            if (ui.error != null) {
                Text(ui.error ?: "", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = { vm.signIn(email.trim(), password, onLoggedIn) },
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (ui.loading) "Signing in..." else "Sign In")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCreateAccount,   // 👈 navigate to Sign Up screen
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Create Account")
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { if (email.isNotBlank()) vm.reset(email.trim()) }) {
                Text("Forgot password?")
            }

            if (onSkip != null) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onSkip) { Text("Skip for now") }
            }
        }
    }
}
