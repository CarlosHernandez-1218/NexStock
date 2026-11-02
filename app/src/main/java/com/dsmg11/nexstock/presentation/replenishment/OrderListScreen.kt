package com.dsmg11.nexstock.presentation.replenishment

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.usecase.replenishment.OrderPermissions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun OrderListScreen(
    onBack: () -> Unit,
    onNewRequest: () -> Unit,
    onSchedule: () -> Unit,
    onPendingReport: () -> Unit,
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            // Encabezado: el título se recorta con "…" si no entra, así los íconos nunca se salen
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Órdenes de compra",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!uiState.isLoading) {
                        Text(
                            orderCountLabel(uiState.orders.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onPendingReport) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Reporte de órdenes pendientes")
                }
                IconButton(onClick = onSchedule) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Cronograma")
                }
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
                uiState.orders.isEmpty() -> CenteredMessage(
                    "Aún no hay órdenes de compra.\nToca \"Nueva solicitud\" para crear la primera."
                )
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // Espacio abajo para que el botón flotante no tape la última orden
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
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

        // Nueva solicitud como botón flotante: siempre visible, en cualquier ancho de pantalla
        ExtendedFloatingActionButton(
            onClick = onNewRequest,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nueva solicitud") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
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
) {
    OutlinedCard(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (showCheckbox) {
                    Checkbox(checked = checked, onCheckedChange = { onCheckedChange() }, enabled = checkEnabled)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Fila 1: número de orden y estado
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            shortId(order),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        StatusChip(order.status)
                    }
                    // Fila 2: proveedor (hasta dos líneas)
                    Text(
                        order.supplierName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Fila 3: fecha y cantidad de productos, con la flecha para desplegar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Entrega máx. ${formatDate(order.deliveryDate)} · ${order.items.size} productos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded) "Ocultar detalle" else "Ver detalle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                    // Botones uno debajo del otro y a lo ancho: entran en cualquier pantalla
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        transitions.filter { it != OrderStatus.CANCELADA }.forEach { next ->
                            Button(
                                onClick = { onTransition(next) },
                                enabled = !isUpdating,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(actionLabel(next)) }
                        }
                        if (OrderStatus.CANCELADA in transitions) {
                            OutlinedButton(
                                onClick = { onTransition(OrderStatus.CANCELADA) },
                                enabled = !isUpdating,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Cancelar orden") }
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
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            status.displayName,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun orderCountLabel(count: Int): String = when (count) {
    0 -> "Sin órdenes"
    1 -> "1 orden"
    else -> "$count órdenes"
}

// Texto del botón según el estado de destino
private fun actionLabel(next: OrderStatus): String = when (next) {
    OrderStatus.APROBADA -> "Aprobar"
    OrderStatus.ENVIADA -> "Marcar como enviada"
    else -> next.displayName
}

fun shortId(order: PurchaseOrder): String = "OC-${order.id.takeLast(6).uppercase()}"
fun shortId(id: String): String = "OC-${id.takeLast(6).uppercase()}"

// La fecha de entrega se guarda en UTC (DatePicker); la de creación es hora local
fun formatDate(millis: Long, utc: Boolean = true): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).apply {
        if (utc) timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(millis))

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
