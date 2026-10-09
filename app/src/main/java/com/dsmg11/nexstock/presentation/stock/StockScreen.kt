package com.dsmg11.nexstock.presentation.stock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.StockGrouping
import com.dsmg11.nexstock.domain.model.stockLevel
import com.dsmg11.nexstock.presentation.catalog.StockBadge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dsmg11.nexstock.domain.model.RotationLevel
import com.dsmg11.nexstock.domain.model.StockLevel

@Composable
fun StockScreen(
    onBack: () -> Unit,
    viewModel: StockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text("Stock", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            Text(
                if (!uiState.isFiltering) "${uiState.totalCount} productos"
                else "${uiState.resultCount} de ${uiState.totalCount}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        uiState.myLines?.let { lines ->
            Text(
                if (lines.isEmpty()) "No tienes líneas asignadas. Pide al Administrador que te asigne una."
                else "Tus líneas: ${lines.joinToString { it.displayName }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            )
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.error != null -> CenteredMessage(uiState.error!!)
            uiState.totalCount == 0 -> CenteredMessage("Aún no hay productos en el catálogo")
            else -> {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    singleLine = true,
                    placeholder = { Text("Buscar por código, nombre, marca…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                            }
                        }
                    }
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 4.dp)
                ) {
                    MultiSelectChip(
                        label = "Marca",
                        options = uiState.availableBrands,
                        selected = uiState.filters.brands,
                        optionLabel = { it },
                        onToggle = viewModel::onToggleBrand
                    )
                    MultiSelectChip(
                        label = "Categoría",
                        options = uiState.availableCategories,
                        selected = uiState.filters.categories,
                        optionLabel = { it },
                        onToggle = viewModel::onToggleCategory
                    )
                    MultiSelectChip(
                        label = "Rotación",
                        options = RotationLevel.entries,
                        selected = uiState.filters.rotations,
                        optionLabel = { it.displayName },
                        onToggle = viewModel::onToggleRotation
                    )
                    MultiSelectChip(
                        label = "Nivel de stock",
                        options = StockLevel.entries,
                        selected = uiState.filters.levels,
                        optionLabel = { it.displayName },
                        onToggle = viewModel::onToggleLevel
                    )
                    if (!uiState.filters.isEmpty) {
                        TextButton(onClick = viewModel::onClearFilters) { Text("Limpiar") }
                    }
                }
                Text(
                    "Agrupar por",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    StockGrouping.entries.forEach { option ->
                        FilterChip(
                            selected = uiState.grouping == option,
                            onClick = { viewModel.onGroupingChange(option) },
                            label = { Text(option.displayName) }
                        )
                    }
                }

                if (uiState.sections.isEmpty()) {
                    CenteredMessage(
                        if (uiState.query.isBlank()) "Ningún producto cumple los filtros"
                        else "Sin resultados para \"${uiState.query}\""
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        uiState.sections.forEach { section ->
                            if (section.title != null) {
                                item(key = "header-${section.title}") { SectionHeader(section) }
                            }
                            items(section.products, key = { it.code }) { product ->
                                StockItem(product)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(section: StockSection) {
    val count = section.products.size
    val countText = if (count == 1) "1 producto" else "$count productos"
    val alertText = if (section.alertCount > 0) " · ${section.alertCount} con alerta" else ""
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Text(
            section.title.orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Text(
            countText + alertText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StockItem(product: Product) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${product.code} · ${product.brand}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${product.line.displayName} · ${product.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${product.stock}", style = MaterialTheme.typography.titleLarge)
                Text(
                    product.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                StockBadge(product.stockLevel)
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun <T> MultiSelectChip(
    label: String,
    options: List<T>,
    selected: Set<T>,
    optionLabel: (T) -> String,
    onToggle: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected.isNotEmpty(),
            onClick = { expanded = true },
            label = { Text(if (selected.isEmpty()) label else "$label (${selected.size})") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    leadingIcon = { Checkbox(checked = option in selected, onCheckedChange = null) },
                    onClick = { onToggle(option) }
                )
            }
        }
    }
}