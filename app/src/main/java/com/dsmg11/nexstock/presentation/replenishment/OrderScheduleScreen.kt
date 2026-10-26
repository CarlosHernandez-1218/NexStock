package com.dsmg11.nexstock.presentation.replenishment

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberDatePickerState
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
import com.dsmg11.nexstock.domain.model.OrderSchedule
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.ScheduleFrequency
import com.dsmg11.nexstock.domain.model.ScheduleStatus
import com.dsmg11.nexstock.domain.model.statusOn
import com.dsmg11.nexstock.domain.model.todayUtcMillis
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun OrderScheduleScreen(
    onBack: () -> Unit,
    viewModel: OrderScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<ProductLine?>(null) }

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
            Text("Cronograma de órdenes", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            "Define cada cuánto y desde cuándo se generan las órdenes de compra de cada línea.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> CenteredMessage(uiState.error!!)
            uiState.lines.isEmpty() -> CenteredMessage("No tienes líneas para programar")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.lines, key = { it.name }) { line ->
                    ScheduleCard(
                        line = line,
                        schedule = uiState.schedules[line.name],
                        onEdit = { editing = line }
                    )
                }
            }
        }
    }

    editing?.let { line ->
        ScheduleDialog(
            line = line,
            current = uiState.schedules[line.name],
            isSaving = uiState.isSaving,
            onDismiss = { editing = null },
            onSave = { frequency, date ->
                viewModel.onSave(line, frequency, date)
                editing = null
            }
        )
    }
}

@Composable
private fun ScheduleCard(
    line: ProductLine,
    schedule: OrderSchedule?,
    onEdit: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(line.displayName, style = MaterialTheme.typography.titleMedium)
                if (schedule == null) {
                    Text(
                        "Sin cronograma definido",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "${schedule.frequency.displayName} · próxima: ${formatScheduleDate(schedule.nextDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (schedule != null) {
                ScheduleStatusChip(schedule.statusOn(todayUtcMillis()))
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.padding(start = 8.dp)) {
                Text(if (schedule == null) "Definir" else "Editar")
            }
        }
    }
}

@Composable
private fun ScheduleStatusChip(status: ScheduleStatus) {
    val container = when (status) {
        ScheduleStatus.VENCIDO -> MaterialTheme.colorScheme.errorContainer
        ScheduleStatus.HOY -> MaterialTheme.colorScheme.primaryContainer
        ScheduleStatus.PROXIMO -> MaterialTheme.colorScheme.secondaryContainer
    }
    Surface(
        color = container,
        contentColor = contentColorFor(container),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            scheduleStatusLabel(status),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDialog(
    line: ProductLine,
    current: OrderSchedule?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ScheduleFrequency, Long) -> Unit
) {
    var frequency by remember { mutableStateOf(current?.frequency ?: ScheduleFrequency.SEMANAL) }
    // Si la fecha guardada ya pasó, se pide una nueva
    var date by remember { mutableStateOf(current?.nextDate?.takeIf { it >= todayUtcMillis() }) }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cronograma · ${line.displayName}") },
        text = {
            Column {
                Text("Frecuencia", style = MaterialTheme.typography.labelLarge)
                ScheduleFrequency.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { frequency = option }
                    ) {
                        RadioButton(selected = frequency == option, onClick = { frequency = option })
                        Text("${option.displayName} (cada ${option.days} días)")
                    }
                }
                Text(
                    "Próxima generación",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                OutlinedCard(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Text(
                            date?.let { formatScheduleDate(it) } ?: "Selecciona una fecha",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Filled.DateRange, contentDescription = null)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { date?.let { onSave(frequency, it) } },
                enabled = date != null && !isSaving
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (showPicker) {
        val today = todayUtcMillis()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date,
            // Solo fechas de hoy en adelante
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

// Estos dos también los usa la pantalla de reposición
fun scheduleStatusLabel(status: ScheduleStatus): String = when (status) {
    ScheduleStatus.VENCIDO -> "Vencido"
    ScheduleStatus.HOY -> "Toca hoy"
    ScheduleStatus.PROXIMO -> "Programado"
}

// El DatePicker trabaja en UTC: se formatea en UTC para que no cambie el día
fun formatScheduleDate(utcMillis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(utcMillis)

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}