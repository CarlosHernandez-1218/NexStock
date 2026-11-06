package com.dsmg11.nexstock.presentation.returns

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ReturnType
import com.dsmg11.nexstock.domain.model.Supplier
import com.dsmg11.nexstock.domain.model.SupplierReturn
import com.dsmg11.nexstock.domain.usecase.returns.ReturnPermissions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReturnListScreen(
    onBack: () -> Unit,
    viewModel: ReturnListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showForm by remember { mutableStateOf(false) }
    var authorizing by remember { mutableStateOf<SupplierReturn?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(
                "Devoluciones",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { showForm = true }) { Text("Nueva") }
        }
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> MessageBox(uiState.error!!)
            uiState.returns.isEmpty() -> MessageBox("No hay devoluciones registradas")
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(uiState.returns, key = { it.id }) { item ->
                    ReturnCard(
                        item = item,
                        canAuthorize = ReturnPermissions.canAuthorize(uiState.profile, item),
                        onAuthorize = { authorizing = item }
                    )
                }
            }
        }
    }

    if (showForm) {
        NewReturnDialog(
            products = uiState.products,
            suppliers = uiState.suppliers,
            isSaving = uiState.isSaving,
            onDismiss = { showForm = false },
            onConfirm = { product, supplier, quantity, type ->
                viewModel.onRegister(product, supplier, quantity, type) { showForm = false }
            }
        )
    }
    authorizing?.let { item ->
        AlertDialog(
            onDismissRequest = { authorizing = null },
            title = { Text("Autorizar devolución") },
            text = {
                Text(
                    "¿Autorizar la devolución de ${item.quantity} ${item.unit} de ${item.productName} " +
                            "a ${item.supplierName}? Se avisará al Administrador de Tienda."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAuthorize(item)
                        authorizing = null
                    },
                    enabled = !uiState.isSaving
                ) { Text("Autorizar") }
            },
            dismissButton = { TextButton(onClick = { authorizing = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ReturnCard(item: SupplierReturn, canAuthorize: Boolean, onAuthorize: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "${item.productName} · ${item.quantity} ${item.unit}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Proveedor: ${item.supplierName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Tipo: ${item.type.displayName} · ${formatDateTime(item.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    item.status.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            if (item.authorizedByName.isNotBlank()) {
                Text(
                    "Autorizada por ${item.authorizedByName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (canAuthorize) {
                TextButton(onClick = onAuthorize, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Autorizar")
                }
            }
        }
    }
}

@Composable
private fun NewReturnDialog(
    products: List<Product>,
    suppliers: List<Supplier>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Product, Supplier, Int, ReturnType) -> Unit
) {
    var product by remember { mutableStateOf<Product?>(null) }
    var supplier by remember { mutableStateOf<Supplier?>(null) }
    var quantityText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ReturnType.FALLADOS) }
    val quantity = quantityText.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva devolución") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerField(
                    label = "Producto",
                    selected = product,
                    options = products,
                    text = { "${it.code} · ${it.name}" },
                    onSelect = { product = it }
                )
                PickerField(
                    label = "Proveedor",
                    selected = supplier,
                    options = suppliers,
                    text = { "${it.razonSocial} (${it.ruc})" },
                    onSelect = { supplier = it }
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { text -> quantityText = text.filter { it.isDigit() }.take(6) },
                    label = { Text("Cantidad") },
                    supportingText = {
                        product?.let { Text("Stock disponible: ${it.stock} ${it.unit}") }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Tipo de devolución", style = MaterialTheme.typography.labelMedium)
                ReturnType.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(selected = type == option, onClick = { type = option })
                        Text(option.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(product!!, supplier!!, quantity, type) },
                enabled = !isSaving && product != null && supplier != null && quantity > 0
            ) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun <T> PickerField(
    label: String,
    selected: T?,
    options: List<T>,
    text: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    selected?.let(text) ?: "Selecciona",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBox(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// createdAt es un momento real (no medianoche UTC), por eso se formatea en la hora del celular
private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date(millis))