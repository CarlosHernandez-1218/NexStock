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

@Composable
fun PendingOrdersScreen(
    onBack: () -> Unit,
    viewModel: PendingOrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                items(uiState.orders, key = { it.id }) { order -> PendingOrderCard(order) }
            }
        }
    }
}

@Composable
private fun PendingOrderCard(order: PurchaseOrder) {
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
            Text(
                order.status.displayName,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun MessageBox(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}