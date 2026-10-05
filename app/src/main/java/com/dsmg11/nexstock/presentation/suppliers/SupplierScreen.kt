package com.dsmg11.nexstock.presentation.suppliers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierScreen(
    onBack: () -> Unit,
    viewModel: SupplierViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.mensajeUsuario) {
        uiState.mensajeUsuario?.let { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text("Registrar proveedor")
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack
                    ) {
                        Text("Volver")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Información SUNAT",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedTextField(
                    value = uiState.ruc,
                    onValueChange = viewModel::onRucChange,
                    label = {
                        Text("RUC")
                    },
                    placeholder = {
                        Text("Ingrese 11 dígitos")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    enabled = !uiState.isConsultandoRuc &&
                            !uiState.isRegistrando,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = viewModel::consultarRuc,
                    enabled = uiState.ruc.length == 11 &&
                            !uiState.isConsultandoRuc &&
                            !uiState.isRegistrando
                ) {
                    if (uiState.isConsultandoRuc) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Consultar")
                    }
                }
            }

            OutlinedTextField(
                value = uiState.razonSocial,
                onValueChange = {},
                label = {
                    Text("Razón social")
                },
                readOnly = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.rucValidado) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    InformacionSunat(
                        titulo = "Estado",
                        valor = uiState.estado,
                        modifier = Modifier.weight(1f)
                    )

                    InformacionSunat(
                        titulo = "Condición",
                        valor = uiState.condicion,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = uiState.direccion,
                    onValueChange = {},
                    label = {
                        Text("Dirección fiscal")
                    },
                    readOnly = true,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Información de contacto",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = uiState.contacto,
                onValueChange = viewModel::onContactoChange,
                label = {
                    Text("Nombre del contacto")
                },
                singleLine = true,
                enabled = !uiState.isRegistrando,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.telefono,
                onValueChange = viewModel::onTelefonoChange,
                label = {
                    Text("Teléfono")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                singleLine = true,
                enabled = !uiState.isRegistrando,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.correo,
                onValueChange = viewModel::onCorreoChange,
                label = {
                    Text("Correo electrónico")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                singleLine = true,
                enabled = !uiState.isRegistrando,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = viewModel::registrarProveedor,
                enabled = uiState.rucValidado &&
                        uiState.contacto.isNotBlank() &&
                        !uiState.isRegistrando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {

                if (uiState.isRegistrando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Registrar proveedor")
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun InformacionSunat(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = valor.ifBlank { "-" },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}