package com.dsmg11.nexstock.presentation.replenishment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.dsmg11.nexstock.domain.model.DeadlineStatus
import com.dsmg11.nexstock.domain.model.deadlineStatus
import com.dsmg11.nexstock.domain.model.deadlineText
import com.dsmg11.nexstock.ui.theme.StockAgotado
import com.dsmg11.nexstock.ui.theme.StockBajo
import com.dsmg11.nexstock.domain.model.receivedPercent
import android.widget.Toast
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.dsmg11.nexstock.domain.model.todayUtcMillis
import com.dsmg11.nexstock.domain.usecase.replenishment.PendingOrdersReport

@Composable
fun PendingOrdersScreen(
    onBack: () -> Unit,
    viewModel: PendingOrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var extending by remember { mutableStateOf<PurchaseOrder?>(null) }

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
            Text("Órdenes pendientes", style = MaterialTheme.typography.titleLarge)
        }
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> MessageBox(uiState.error!!)
            uiState.orders.isEmpty() -> MessageBox("No tienes órdenes pendientes")
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(uiState.orders, key = { it.id }) { order ->
                    PendingOrderCard(
                        order = order,
                        canExtend = PendingOrdersReport.canExtend(uiState.profile, order),
                        onExtend = { extending = order }
                    )
                }
            }
        }
    }
    extending?.let { order ->
        ExtendDialog(
            order = order,
            onDismiss = { extending = null },
            onConfirm = { date, reason ->
                viewModel.onExtend(order, date, reason)
                extending = null
            }
        )
    }
}

@Composable
private fun PendingOrderCard(order: PurchaseOrder, canExtend: Boolean, onExtend: () -> Unit) {
    val deadline = order.deadlineStatus()
    // El borde y la etiqueta toman el color del estado de vencimiento
    val accent: Color? = when (deadline) {
        DeadlineStatus.VENCIDA -> StockAgotado
        DeadlineStatus.POR_VENCER -> StockBajo
        DeadlineStatus.EN_PLAZO -> null
    }

    OutlinedCard(
        border = if (accent != null) BorderStroke(2.dp, accent) else CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                if (accent != null) {
                    // El ámbar necesita texto oscuro para leerse bien (igual que en StockBadge)
                    val textColor = if (deadline == DeadlineStatus.POR_VENCER) Color.Black else Color.White
                    Surface(color = accent, contentColor = textColor, shape = RoundedCornerShape(50)) {
                        Text(
                            order.deadlineText(),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            val percent = order.receivedPercent
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Text(
                    order.status.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$percent% recibido",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
        if (canExtend) {
            TextButton(onClick = onExtend, modifier = Modifier.padding(top = 4.dp)) {
                Text("Registrar prórroga")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtendDialog(
    order: PurchaseOrder,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit
) {
    var date by remember { mutableStateOf<Long?>(null) }
    var reason by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Prórroga · ${shortId(order)}") },
        text = {
            Column {
                Text(
                    "Fecha actual: ${formatDate(order.deliveryDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Nueva fecha máxima",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                OutlinedCard(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Text(
                            date?.let { formatDate(it) } ?: "Selecciona una fecha",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Filled.DateRange, contentDescription = null)
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo") },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { date?.let { onConfirm(it, reason) } },
                enabled = date != null && reason.isNotBlank()
            ) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (showPicker) {
        val today = todayUtcMillis()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date ?: order.deliveryDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= today
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        date = pickerState.selectedDateMillis
                        showPicker = false
                    },
                    enabled = pickerState.selectedDateMillis != null
                ) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun MessageBox(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}