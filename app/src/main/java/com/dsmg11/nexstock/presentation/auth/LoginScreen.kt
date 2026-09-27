package com.dsmg11.nexstock.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// TODO MOV-4 (Carlos): reemplazar por el formulario real de inicio de sesión
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit,
    onGoToForgotPassword: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NexStock",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Iniciar sesión (en construcción)")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onLoginSuccess, modifier = Modifier.fillMaxWidth()) {
            Text("Entrar (temporal)")
        }
        OutlinedButton(onClick = onGoToRegister, modifier = Modifier.fillMaxWidth()) {
            Text("Crear cuenta")
        }
        TextButton(onClick = onGoToForgotPassword) {
            Text("¿Olvidaste tu contraseña?")
        }
    }
}