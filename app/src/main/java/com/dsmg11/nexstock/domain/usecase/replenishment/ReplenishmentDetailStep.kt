package com.dsmg11.nexstock.presentation.replenishment

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dsmg11.nexstock.domain.model.Supplier
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun ReplenishmentDetailStep(
    state: ReplenishmentUiState,
    onSupplierSelected: (Supplier) -> Unit,
    onDeliveryDateSelected: (Long) -> Unit,
    onGenerate: () -> Unit
) {
    val items = state.candidates.filter { it.code in state.selected }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Productos a reponer (${items.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items, key = { it.code }) { product ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                product.code,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${state.selected[product.code]} ${product.unit}",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        SupplierSelector(
            suppliers = state.suppliers,
            selected = state.supplier,
            onSelected = onSupplierSelected
        )
        DeliveryDateSelector(
            dateMillis = state.deliveryDate,
            onDateSelected = onDeliveryDateSelected
        )

        Button(
            onClick = onGenerate,
            enabled = state.canGenerate,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text(if (state.isSaving) "Guardando…" else "Generar orden de compra")
        }
    }
}

@Composable
private fun SupplierSelector(
    suppliers: List<Supplier>,
    selected: Supplier?,
    onSelected: (Supplier) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Text(
        "Proveedor",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
    if (suppliers.isEmpty()) {
        Text(
            "No hay proveedores registrados. Regístralos primero en Proveedores.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        return
    }
    Box {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        selected?.razonSocial ?: "Selecciona un proveedor",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    selected?.let {
                        Text(
                            "RUC ${it.ruc}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            suppliers.forEach { supplier ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(supplier.razonSocial)
                            Text(
                                "RUC ${supplier.ruc}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelected(supplier)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryDateSelector(
    dateMillis: Long?,
    onDateSelected: (Long) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Text(
        "Fecha máxima de entrega",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
    OutlinedCard(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Text(
                dateMillis?.let { formatDate(it) } ?: "Selecciona una fecha",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.DateRange, contentDescription = null)
        }
    }

    if (showPicker) {
        val today = todayUtcStart()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateMillis,
            // Solo se pueden elegir fechas de hoy en adelante
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= today
            }
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(onDateSelected)
                        showPicker = false
                    },
                    enabled = pickerState.selectedDateMillis != null
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// El DatePicker trabaja en UTC: se formatea en UTC para que no cambie el día
private fun formatDate(utcMillis: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(utcMillis)

// Medianoche UTC del día de hoy (según la fecha local del celular)
private fun todayUtcStart(): Long {
    val local = Calendar.getInstance()
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local[Calendar.YEAR], local[Calendar.MONTH], local[Calendar.DAY_OF_MONTH])
    }.timeInMillis
}