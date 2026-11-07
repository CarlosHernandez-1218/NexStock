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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.graphics.Color
import com.dsmg11.nexstock.domain.model.ReturnStatus
import androidx.compose.foundation.BorderStroke
import com.dsmg11.nexstock.domain.model.WarehouseAlert
import com.dsmg11.nexstock.domain.model.warehouseAlert
import com.dsmg11.nexstock.domain.model.warehouseText
import com.dsmg11.nexstock.ui.theme.StockAgotado
import com.dsmg11.nexstock.ui.theme.StockBajo

@Composable
fun ReturnListScreen(
    onBack: () -> Unit,
    viewModel: ReturnListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showForm by remember { mutableStateOf(false) }
    var authorizing by remember { mutableStateOf<SupplierReturn?>(null) }
    var pickingUp by remember { mutableStateOf<SupplierReturn?>(null) }
    var confirmingPickup by remember { mutableStateOf<SupplierReturn?>(null) }

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

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.statusFilter == null,
                    onClick = { viewModel.onFilterChange(null) },
                    label = { Text("Todas (${uiState.returns.size})") }
                )
            }
            items(ReturnStatus.entries) { status ->
                FilterChip(
                    selected = uiState.statusFilter == status,
                    onClick = { viewModel.onFilterChange(status) },
                    label = { Text("${status.displayName} (${uiState.returns.count { it.status == status }})") }
                )
            }
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> MessageBox(uiState.error!!)
            uiState.filteredReturns.isEmpty() -> MessageBox(
                if (uiState.statusFilter == null) "No hay devoluciones registradas"
                else "No hay devoluciones en estado ${uiState.statusFilter!!.displayName}"
            )
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(uiState.filteredReturns, key = { it.id }) { item ->
                    ReturnCard(
                        item = item,
                        canAuthorize = ReturnPermissions.canAuthorize(uiState.profile, item),
                        onAuthorize = { authorizing = item },
                        canRequestPickup = ReturnPermissions.canRequestPickup(uiState.profile, item),
                        onRequestPickup = { pickingUp = item },
                        canConfirmPickup = ReturnPermissions.canConfirmPickup(uiState.profile, item),
                        onConfirmPickup = { confirmingPickup = item }
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
    pickingUp?.let { item ->
        AlertDialog(
            onDismissRequest = { pickingUp = null },
            title = { Text("Coordinar recojo") },
            text = {
                Text(
                    "¿Marcar como pendiente de recojo la devolución de ${item.quantity} ${item.unit} " +
                            "de ${item.productName}? Confirma que ya coordinaste el recojo con ${item.supplierName}."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onRequestPickup(item)
                        pickingUp = null
                    },
                    enabled = !uiState.isSaving
                ) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { pickingUp = null }) { Text("Cancelar") } }
        )
    }
    confirmingPickup?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmingPickup = null },
            title = { Text("Confirmar recojo") },
            text = {
                Text(
                    "¿${item.supplierName} ya recogió ${item.quantity} ${item.unit} de ${item.productName}? " +
                            "Se descontarán del stock y esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onConfirmPickup(item)
                        confirmingPickup = null
                    },
                    enabled = !uiState.isSaving
                ) { Text("Confirmar recojo") }
            },
            dismissButton = { TextButton(onClick = { confirmingPickup = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ReturnCard(
    item: SupplierReturn,
    canAuthorize: Boolean,
    onAuthorize: () -> Unit,
    canRequestPickup: Boolean,
    onRequestPickup: () -> Unit,
    canConfirmPickup: Boolean,
    onConfirmPickup: () -> Unit
) {
    val (container, content) = statusColors(item.status)
    val alert = item.warehouseAlert()
    val accent: Color? = when (alert) {
        WarehouseAlert.OVER_LIMIT -> StockAgotado
        WarehouseAlert.NEAR_LIMIT -> StockBajo
        null -> null
    }
    OutlinedCard(
        border = if (accent != null) BorderStroke(2.dp, accent) else CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
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
                "Tipo: ${item.type.displayName} · Registrada ${formatDateTime(item.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = container,
                contentColor = content,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    item.status.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            if (accent != null) {
                val textColor = if (alert == WarehouseAlert.NEAR_LIMIT) Color.Black else Color.White
                Surface(
                    color = accent,
                    contentColor = textColor,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        item.warehouseText(),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            if (item.authorizedByName.isNotBlank()) {
                Text(
                    "Autorizada por ${item.authorizedByName} · ${formatDateTime(item.authorizedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (item.pickupRequestedAt > 0) {
                Text(
                    "Pendiente de recojo desde ${formatDateTime(item.pickupRequestedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.pickedUpAt > 0) {
                Text(
                    "Recogida el ${formatDateTime(item.pickedUpAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canAuthorize) {
                TextButton(onClick = onAuthorize, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Autorizar")
                }
            }
            if (canRequestPickup) {
                TextButton(onClick = onRequestPickup, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Coordinar recojo")
                }
            }
            if (canConfirmPickup) {
                TextButton(onClick = onConfirmPickup, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Confirmar recojo")
                }
            }
        }
    }
}

// Color de la etiqueta según el estado (fondo, texto)
@Composable
private fun statusColors(status: ReturnStatus): Pair<Color, Color> = when (status) {
    ReturnStatus.REGISTRADA ->
        MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    ReturnStatus.AUTORIZADA ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    ReturnStatus.PENDIENTE_RECOJO ->
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    ReturnStatus.RECOGIDA ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
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