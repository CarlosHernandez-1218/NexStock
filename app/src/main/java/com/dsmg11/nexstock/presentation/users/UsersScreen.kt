package com.dsmg11.nexstock.presentation.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.UserRole

@Composable
fun UsersScreen(
    onBack: () -> Unit,
    viewModel: UsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text("Usuarios", style = MaterialTheme.typography.titleLarge)
        }

        uiState.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        }
        uiState.successMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(8.dp))
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.users, key = { it.uid }) { user ->
                    UserCard(user = user, onClick = { viewModel.startEditing(user) })
                }
            }
        }
    }

    uiState.editing?.let { user ->
        EditUserDialog(
            user = user,
            selectedRole = uiState.selectedRole,
            selectedLines = uiState.selectedLines,
            isSaving = uiState.isSaving,
            onRoleSelected = viewModel::onRoleSelected,
            onLineToggled = viewModel::onLineToggled,
            onSave = viewModel::save,
            onDismiss = viewModel::cancelEditing
        )
    }
}

@Composable
private fun UserCard(user: UserProfile, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(user.fullName.ifBlank { "(Sin nombre)" }, style = MaterialTheme.typography.titleMedium)
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = user.role.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            if (user.role == UserRole.JEFE_LINEA) {
                Text(
                    text = user.assignedLines.joinToString(", ") { it.displayName }
                        .ifBlank { "Sin líneas asignadas" },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun EditUserDialog(
    user: UserProfile,
    selectedRole: UserRole,
    selectedLines: Set<ProductLine>,
    isSaving: Boolean,
    onRoleSelected: (UserRole) -> Unit,
    onLineToggled: (ProductLine) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(user.fullName.ifBlank { user.email }) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Rol", style = MaterialTheme.typography.titleSmall)
                UserRole.entries.forEach { role ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = role == selectedRole,
                                onClick = { onRoleSelected(role) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = role == selectedRole, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(role.displayName)
                    }
                }

                // Las líneas solo aplican al Jefe de Línea
                if (selectedRole == UserRole.JEFE_LINEA) {
                    Spacer(Modifier.height(12.dp))
                    Text("Líneas a cargo", style = MaterialTheme.typography.titleSmall)
                    ProductLine.entries.forEach { line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = line in selectedLines,
                                    onValueChange = { onLineToggled(line) },
                                    role = Role.Checkbox
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = line in selectedLines, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(line.displayName)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !isSaving) {
                Text(if (isSaving) "Guardando..." else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancelar")
            }
        }
    )
}