package com.dsmg11.nexstock.presentation.replenishment

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.usecase.replenishment.OrderPermissions
import androidx.compose.material3.Checkbox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun OrderListScreen(
    onBack: () -> Unit,
    onNewRequest: () -> Unit,
    viewModel: OrderListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var expandedId by remember { mutableStateOf<String?>(null) }
    // Cambio pendiente de confirmar: la orden y el estado al que se quiere pasar
    var pending by remember { mutableStateOf<Pair<PurchaseOrder, OrderStatus>?>(null) }
    var showConsolidate by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text("Órdenes de compra", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Button(onClick = onNewRequest) { Text("Nueva solicitud") }
        }

        if (OrderPermissions.canConsolidate(uiState.profile) && uiState.selectedIds.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    "${uiState.selectedIds.size} seleccionadas",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = viewModel::onClearSelection) { Text("Limpiar") }
                Button(
                    onClick = { showConsolidate = true },
                    enabled = uiState.canConsolidate
                ) { Text("Consolidar") }
            }
        }
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> CenteredMessage(uiState.error!!)
            uiState.orders.isEmpty() -> CenteredMessage("Aún no hay órdenes de compra")
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(uiState.orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        expanded = expandedId == order.id,
                        transitions = OrderPermissions.availableTransitions(uiState.profile, order),
                        isUpdating = uiState.updatingId == order.id,
                        showCheckbox = OrderPermissions.canConsolidate(uiState.profile) &&
                                OrderPermissions.isConsolidable(order),
                        checked = order.id in uiState.selectedIds,
                        checkEnabled = uiState.selectedSupplierRuc == null ||
                                uiState.selectedSupplierRuc == order.supplierRuc,
                        onCheckedChange = { viewModel.onToggleSelect(order) },
                        onToggle = { expandedId = if (expandedId == order.id) null else order.id },
                        onTransition = { next -> pending = order to next }
                    )
                }
            }
        }
    }

    pending?.let { (order, next) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Cambiar estado") },
            text = {
                Text(
                    "¿Pasar la orden ${shortId(order)} de ${order.status.displayName} " +
                            "a ${next.displayName}?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onChangeStatus(order, next)
                    pending = null
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Cancelar") }
            }
        )
    }

    if (showConsolidate) {
        val selected = uiState.selectedOrders
        AlertDialog(
            onDismissRequest = { showConsolidate = false },
            title = { Text("Consolidar órdenes") },
            text = {
                Text(
                    "Se juntarán ${selected.size} órdenes de " +
                            "${selected.firstOrNull()?.supplierName.orEmpty()} en una sola orden nueva. " +
                            "Las originales pasarán a Cancelada."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onConsolidate()
                    showConsolidate = false
                }) { Text("Consolidar") }
            },
            dismissButton = {
                TextButton(onClick = { showConsolidate = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun OrderCard(
    order: PurchaseOrder,
    expanded: Boolean,
    transitions: List<OrderStatus>,
    isUpdating: Boolean,
    showCheckbox: Boolean,
    checked: Boolean,
    checkEnabled: Boolean,
    onCheckedChange: () -> Unit,
    onToggle: () -> Unit,
    onTransition: (OrderStatus) -> Unit
){
    OutlinedCard(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCheckbox) {
                    Checkbox(checked = checked, onCheckedChange = { onCheckedChange() }, enabled = checkEnabled)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${shortId(order)} · ${order.supplierName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Entrega máxima: ${formatDate(order.deliveryDate)} · ${order.items.size} productos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(order.status)
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                order.items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(
                            "${item.name} (${item.code})",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${item.quantity} ${item.unit}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    "Generada el ${formatDate(order.createdAt, utc = false)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (order.consolidatedInto.isNotEmpty()) {
                    Text(
                        "Consolidada en ${shortId(order.consolidatedInto)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (order.consolidatedFrom.isNotEmpty()) {
                    Text(
                        "Consolidación de ${order.consolidatedFrom.size} órdenes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (transitions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        transitions.forEach { next ->
                            if (next == OrderStatus.CANCELADA) {
                                OutlinedButton(
                                    onClick = { onTransition(next) },
                                    enabled = !isUpdating,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) { Text("Cancelar orden") }
                            } else {
                                Button(onClick = { onTransition(next) }, enabled = !isUpdating) {
                                    Text(actionLabel(next))
                                }
                            }
                        }
                    }
                } else if (order.status.isFinal) {
                    Text(
                        "Estado final: no admite más cambios",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val container = when (status) {
        OrderStatus.CANCELADA -> MaterialTheme.colorScheme.errorContainer
        OrderStatus.RECIBIDA -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    Surface(
        color = container,
        contentColor = contentColorFor(container),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            status.displayName,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// Texto del botón según el estado de destino
private fun actionLabel(next: OrderStatus): String = when (next) {
    OrderStatus.APROBADA -> "Aprobar"
    OrderStatus.ENVIADA -> "Marcar como enviada"
    else -> next.displayName
}

private fun shortId(order: PurchaseOrder): String = "OC-${order.id.takeLast(6).uppercase()}"
private fun shortId(id: String): String = "OC-${id.takeLast(6).uppercase()}"

// La fecha de entrega se guarda en UTC (DatePicker); la de creación es hora local
private fun formatDate(millis: Long, utc: Boolean = true): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).apply {
        if (utc) timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(millis))

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}