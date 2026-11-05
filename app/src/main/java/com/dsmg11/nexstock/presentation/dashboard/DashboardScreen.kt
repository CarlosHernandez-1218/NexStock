package com.dsmg11.nexstock.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsmg11.nexstock.ui.theme.StockAgotado
import com.dsmg11.nexstock.ui.theme.StockBajo

// HU10 - MOV-66: indicadores clave para el Administrador de Tienda
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    onOpenStock: () -> Unit,
    onOpenPendingOrders: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = uiState.summary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Dashboard", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Indicadores logísticos en tiempo real",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxWidth().padding(top = 64.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.error != null -> Text(
                uiState.error!!,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 64.dp)
            )

            else -> {
                SectionTitle("Inventario · ${summary.activeProducts} productos activos")
                KpiRow {
                    KpiCard(
                        icon = Icons.Filled.WarningAmber,
                        value = summary.criticalStock,
                        label = "Stock crítico",
                        caption = "En o bajo el stock mínimo",
                        accent = if (summary.criticalStock > 0) StockBajo else null,
                        onClick = onOpenStock
                    )
                    KpiCard(
                        icon = Icons.Filled.RemoveShoppingCart,
                        value = summary.outOfStock,
                        label = "Agotados",
                        caption = "Sin existencias",
                        accent = if (summary.outOfStock > 0) StockAgotado else null,
                        onClick = onOpenStock
                    )
                }

                SectionTitle("Abastecimiento")
                KpiRow {
                    KpiCard(
                        icon = Icons.Filled.PendingActions,
                        value = summary.pendingOrders,
                        label = "Órdenes pendientes",
                        caption = when (summary.overdueOrders) {
                            0 -> "Ninguna vencida"
                            1 -> "1 vencida"
                            else -> "${summary.overdueOrders} vencidas"
                        },
                        accent = if (summary.overdueOrders > 0) StockAgotado else null,
                        onClick = onOpenPendingOrders
                    )
                    KpiCard(
                        icon = Icons.AutoMirrored.Filled.AssignmentReturn,
                        value = summary.returnsToPickup,
                        label = "Devoluciones por recoger",
                        caption = "Pendientes de recojo",
                        accent = if (summary.returnsToPickup > 0) StockBajo else null,
                        onClick = null
                    )
                }

                SectionTitle("Gráficos")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChartCard(
                        title = "Órdenes por estado",
                        subtitle = "Todas las órdenes de compra registradas"
                    ) { OrdersByStatusChart(summary.ordersByStatus) }

                    val weekTotal = summary.receptionsByDay.sumOf { it.count }
                    ChartCard(
                        title = "Recepciones de los últimos 7 días",
                        subtitle = if (weekTotal == 1) "1 recepción esta semana" else "$weekTotal recepciones esta semana"
                    ) { ReceptionsByDayChart(summary.receptionsByDay) }

                    ChartCard(
                        title = "Productos por nivel de rotación",
                        subtitle = "Productos activos según su rotación"
                    ) { ProductsByRotationChart(summary.productsByRotation) }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

// Las dos tarjetas de cada fila quedan del mismo alto
@Composable
private fun KpiRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        content = content
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.KpiCard(
    icon: ImageVector,
    value: Int,
    label: String,
    caption: String,
    accent: Color?,
    onClick: (() -> Unit)?
) {
    val modifier = Modifier.weight(1f).fillMaxHeight()
    // Con alerta, el borde y el número toman el color del problema (amarillo o rojo)
    val border = if (accent != null) BorderStroke(1.5.dp, accent) else CardDefaults.outlinedCardBorder()
    val body: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = accent ?: MaterialTheme.colorScheme.primary)
            Text(
                value.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = accent ?: MaterialTheme.colorScheme.onSurface
            )
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (onClick != null) OutlinedCard(onClick = onClick, modifier = modifier, border = border, content = body)
    else OutlinedCard(modifier = modifier, border = border, content = body)
}
