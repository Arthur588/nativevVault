package com.example.vault.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.vault.ui.login.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onUnlock: () -> Unit
) {
    val loginState by viewModel.loginState.collectAsState()
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(loginState) {
        if (loginState is LoginViewModel.LoginState.Success) {
            onUnlock()
        }
    }

    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Enter Password")
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
        when (loginState) {
            is LoginViewModel.LoginState.Error -> {
                Text(text = (loginState as LoginViewModel.LoginState.Error).message, color = androidx.compose.ui.graphics.Color.Red)
            }
            is LoginViewModel.LoginState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            }
            else -> {}
        }
        Button(
            onClick = { viewModel.login(password) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(text = "Unlock")
        }
    }
}