package com.dsmg11.nexstock.presentation.reception

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.receivedPercent
import com.dsmg11.nexstock.domain.model.todayUtcMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// MOV-47: el almacenero elige la orden de compra que va a recibir
@Composable
fun ReceptionListScreen(
    onBack: () -> Unit,
    onSelectOrder: (String) -> Unit,
    viewModel: ReceptionListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Column {
                Text("Recepción", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Órdenes pendientes de recibir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Proveedor, RUC, orden o producto") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        // MOV-51: lo registrado sin internet espera aquí hasta que vuelva la conexión
        if (uiState.pendingSyncCount > 0) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = contentColorFor(MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Text(
                        if (uiState.pendingSyncCount == 1) "1 recepción pendiente de sincronizar. Se subirá sola al volver la conexión."
                        else "${uiState.pendingSyncCount} recepciones pendientes de sincronizar. Se subirán solas al volver la conexión.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> CenteredMessage(uiState.error!!)
            uiState.orders.isEmpty() -> CenteredMessage(
                "No hay órdenes por recibir.\nAparecen aquí cuando el administrador las marca como Enviadas."
            )
            uiState.filteredOrders.isEmpty() -> CenteredMessage("Ninguna orden coincide con la búsqueda")
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(uiState.filteredOrders, key = { it.id }) { order ->
                    PendingOrderCard(order = order, onClick = { onSelectOrder(order.id) })
                }
            }
        }
    }
}

@Composable
private fun PendingOrderCard(order: PurchaseOrder, onClick: () -> Unit) {
    val overdue = order.deliveryDate in 1 until todayUtcMillis()
    val percent = order.receivedPercent

    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "OC-${order.id.takeLast(6).uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(order.status)
                }
                Text(order.supplierName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "RUC ${order.supplierRuc} · ${order.items.size} productos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Entrega máxima: ${formatUtcDate(order.deliveryDate)}" + if (overdue) " · Atrasada" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (order.status == OrderStatus.PARCIALMENTE_RECIBIDA) {
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                    Text(
                        "$percent% recibido",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Recibir",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun StatusChip(status: OrderStatus) {
    val container = if (status == OrderStatus.PARCIALMENTE_RECIBIDA)
        MaterialTheme.colorScheme.tertiaryContainer
    else MaterialTheme.colorScheme.secondaryContainer
    Surface(color = container, contentColor = contentColorFor(container), shape = RoundedCornerShape(50)) {
        Text(
            status.displayName,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// La fecha de entrega se guarda como medianoche UTC (DatePicker)
private fun formatUtcDate(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(millis))
