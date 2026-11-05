package com.dsmg11.nexstock.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dsmg11.nexstock.domain.model.DayCount
import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.RotationLevel
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData
import com.github.tehras.charts.bar.renderer.label.SimpleValueDrawer
import com.github.tehras.charts.bar.renderer.xaxis.SimpleXAxisDrawer
import com.github.tehras.charts.bar.renderer.yaxis.YAxisDrawer
import com.github.tehras.charts.piechart.PieChart
import com.github.tehras.charts.piechart.PieChartData
import com.github.tehras.charts.piechart.renderer.SimpleSliceDrawer

// Colores fijos por estado de la orden (se leen bien en tema claro y oscuro)
private val statusColors = mapOf(
    OrderStatus.GENERADA to Color(0xFF90A4AE),
    OrderStatus.APROBADA to Color(0xFF42A5F5),
    OrderStatus.ENVIADA to Color(0xFF7E57C2),
    OrderStatus.PARCIALMENTE_RECIBIDA to Color(0xFFFFB300),
    OrderStatus.RECIBIDA to Color(0xFF43A047),
    OrderStatus.CANCELADA to Color(0xFFE53935)
)

private val rotationColors = mapOf(
    RotationLevel.ALTA to Color(0xFF43A047),
    RotationLevel.MEDIA to Color(0xFFFFB300),
    RotationLevel.BAJA to Color(0xFF90A4AE)
)

private val receptionColor = Color(0xFF26A69A)

@Composable
fun ChartCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

// MOV-67: gráfico de torta con la cantidad de órdenes en cada estado
@Composable
fun OrdersByStatusChart(ordersByStatus: Map<OrderStatus, Int>) {
    val total = ordersByStatus.values.sum()
    if (total == 0) {
        EmptyChart("Aún no hay órdenes de compra")
        return
    }
    val present = OrderStatus.entries.filter { (ordersByStatus[it] ?: 0) > 0 }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.size(130.dp)) {
            PieChart(
                pieChartData = PieChartData(
                    present.map { PieChartData.Slice(value = ordersByStatus.getValue(it).toFloat(), color = statusColors.getValue(it)) }
                ),
                sliceDrawer = SimpleSliceDrawer(sliceThickness = 40f)
            )
            Text(
                "$total",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            present.forEach { status ->
                val count = ordersByStatus.getValue(status)
                LegendRow(statusColors.getValue(status), "${status.displayName}: $count (${count * 100 / total}%)")
            }
        }
    }
}

// MOV-67: gráfico de barras con las recepciones de cada día
@Composable
fun ReceptionsByDayChart(receptionsByDay: List<DayCount>) {
    if (receptionsByDay.all { it.count == 0 }) {
        EmptyChart("Sin recepciones en los últimos 7 días")
        return
    }
    LabeledBars(receptionsByDay.map { BarItem(it.label, it.count, receptionColor) })
}

// MOV-68: productos activos por nivel de rotación
@Composable
fun ProductsByRotationChart(productsByRotation: Map<RotationLevel, Int>) {
    if (productsByRotation.values.all { it == 0 }) {
        EmptyChart("No hay productos activos")
        return
    }
    LabeledBars(
        RotationLevel.entries.map { level ->
            BarItem(level.shortName, productsByRotation[level] ?: 0, rotationColors.getValue(level))
        }
    )
}

private data class BarItem(val label: String, val value: Int, val color: Color)

// Barras de tehras/charts con el valor encima de cada barra y la etiqueta debajo (en Compose).
// La librería no reserva espacio para textos bajo el eje (se cortaban), por eso las etiquetas van aparte.
@Composable
private fun LabeledBars(items: List<BarItem>) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val axisColor = MaterialTheme.colorScheme.outline
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // La librería deja a la izquierda el 10 % del ancho (máx. 50 dp) para el eje Y
        val yAxisWidth = minOf(50.dp, maxWidth * 0.1f)
        Column {
            BarChart(
                barChartData = BarChartData(
                    bars = items.map {
                        BarChartData.Bar(
                            value = it.value.toFloat(),
                            color = it.color,
                            label = if (it.value > 0) it.value.toString() else ""
                        )
                    }
                ),
                modifier = Modifier.fillMaxWidth().height(160.dp),
                xAxisDrawer = SimpleXAxisDrawer(axisLineColor = axisColor),
                yAxisDrawer = NoYAxisDrawer,
                labelDrawer = SimpleValueDrawer(
                    drawLocation = SimpleValueDrawer.DrawLocation.Outside,
                    labelTextColor = textColor
                )
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Spacer(Modifier.width(yAxisWidth))
                items.forEach { item ->
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Eje Y sin números: con el valor sobre cada barra no hacen falta
// (y los de la librería no coinciden con la altura de las barras)
private object NoYAxisDrawer : YAxisDrawer {
    override fun drawAxisLine(drawScope: DrawScope, canvas: Canvas, drawableArea: Rect) = Unit
    override fun drawAxisLabels(
        drawScope: DrawScope,
        canvas: Canvas,
        drawableArea: Rect,
        minValue: Float,
        maxValue: Float
    ) = Unit
}

@Composable
private fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyChart(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
    )
}
