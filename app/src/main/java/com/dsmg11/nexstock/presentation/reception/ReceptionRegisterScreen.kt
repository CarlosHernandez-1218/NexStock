package com.dsmg11.nexstock.presentation.reception

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsmg11.nexstock.domain.model.PurchaseOrderItem
import com.dsmg11.nexstock.domain.model.isPendingReception
import com.dsmg11.nexstock.domain.model.pendingOf
import com.dsmg11.nexstock.presentation.catalog.rememberBarcodeScanner
import com.dsmg11.nexstock.presentation.replenishment.shortId

// MOV-48 y MOV-49: registrar lo que llegó de una orden, escaneando o digitando cantidades
@Composable
fun ReceptionRegisterScreen(
    onBack: () -> Unit,
    viewModel: ReceptionRegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var showReview by remember { mutableStateOf(false) }

    val openScanner = rememberBarcodeScanner(
        onScanned = viewModel::onBarcodeScanned,
        onError = viewModel::onScannerError
    )

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }
    }

    // Al guardar, vuelve a la lista de órdenes por recibir
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }

    // Después de cada escaneo la lista se desplaza hasta el producto leído
    LaunchedEffect(uiState.scanCount) {
        val order = uiState.order ?: return@LaunchedEffect
        val index = order.items.indexOfFirst { it.code == uiState.highlightedCode }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    val order = uiState.order

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Registrar recepción",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (order != null) {
                    Text(
                        "${shortId(order)} · ${order.supplierName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> CenteredMessage(uiState.error!!)
            order == null -> CenteredMessage("No se encontró la orden de compra")
            !order.isPendingReception -> CenteredMessage(
                "Esta orden está en estado ${order.status.displayName} y ya no admite recepciones"
            )
            else -> {
                FilledTonalButton(
                    onClick = openScanner,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Escanear código de barras")
                }
                Text(
                    "Escanea cada unidad que llegó o escribe la cantidad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(order.items, key = { it.code }) { item ->
                        val now = uiState.quantities[item.code] ?: 0
                        ReceptionItemCard(
                            item = item,
                            alreadyReceived = order.received[item.code] ?: 0,
                            pending = order.pendingOf(item),
                            now = now,
                            highlighted = item.code == uiState.highlightedCode,
                            onIncrement = { viewModel.onIncrement(item.code) },
                            onDecrement = { viewModel.onDecrement(item.code) },
                            onTyped = { viewModel.onQuantityTyped(item.code, it) }
                        )
                    }
                }

                Button(
                    onClick = { showReview = true },
                    enabled = uiState.canReview,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            uiState.isSaving -> "Guardando…"
                            uiState.totalUnits == 0 -> "Ingresa al menos una cantidad"
                            else -> "Revisar recepción (${uiState.totalUnits} unid.)"
                        }
                    )
                }
            }
        }
    }

    if (showReview && order != null) {
        AlertDialog(
            onDismissRequest = { showReview = false },
            title = { Text("Confirmar recepción") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${shortId(order)} · ${order.supplierName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    order.items.forEach { item ->
                        val qty = uiState.quantities[item.code] ?: 0
                        if (qty > 0) {
                            Text("• ${item.name}: $qty ${item.unit}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(
                        if (uiState.completesOrder) "La orden quedará como Recibida."
                        else "Aún faltarán productos: la orden quedará como Parcialmente recibida.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showReview = false
                    viewModel.onSave()
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showReview = false }) { Text("Seguir editando") }
            }
        )
    }
}

@Composable
private fun ReceptionItemCard(
    item: PurchaseOrderItem,
    alreadyReceived: Int,
    pending: Int,
    now: Int,
    highlighted: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onTyped: (String) -> Unit
) {
    // El último producto escaneado se marca con un borde de color
    val border = if (highlighted) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    else CardDefaults.outlinedCardBorder()

    OutlinedCard(modifier = Modifier.fillMaxWidth(), border = border) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${item.code} · ${item.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // MOV-49: lo pedido frente a lo recibido
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QuantityTag("Pedido", item.quantity, MaterialTheme.colorScheme.surfaceVariant)
                QuantityTag("Recibido", alreadyReceived, MaterialTheme.colorScheme.surfaceVariant)
                QuantityTag(
                    "Falta", pending,
                    if (pending > 0) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (pending == 0) {
                Text(
                    "Completo: ya se recibió todo lo pedido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Recibido ahora",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDecrement, enabled = now > 0) {
                        Icon(Icons.Filled.Remove, contentDescription = "Restar uno")
                    }
                    OutlinedTextField(
                        value = now.toString(),
                        onValueChange = onTyped,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.width(76.dp)
                    )
                    IconButton(onClick = onIncrement, enabled = now < pending) {
                        Icon(Icons.Filled.Add, contentDescription = "Sumar uno")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityTag(label: String, value: Int, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(8.dp)) {
        Text(
            "$label $value",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
