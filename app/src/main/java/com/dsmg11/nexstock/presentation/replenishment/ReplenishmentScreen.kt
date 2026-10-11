package com.dsmg11.nexstock.presentation.replenishment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.stockLevel
import com.dsmg11.nexstock.presentation.catalog.StockBadge
import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun ReplenishmentScreen(
    onBack: () -> Unit,
    viewModel: ReplenishmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.step == ReplenishmentStep.DETALLE) {
        viewModel.onBackToSelection()
    }

    val context = LocalContext.current
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = {
                if (uiState.step == ReplenishmentStep.DETALLE) viewModel.onBackToSelection() else onBack()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Text(
                if (uiState.step == ReplenishmentStep.DETALLE) "Orden de compra" else "Candidatos de reposición",
                style = MaterialTheme.typography.titleLarge
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
            uiState.step == ReplenishmentStep.DETALLE -> ReplenishmentDetailStep(
                state = uiState,
                onSupplierSelected = viewModel::onSupplierSelected,
                onDeliveryDateSelected = viewModel::onDeliveryDateSelected,
                onGenerate = viewModel::onGenerate
            )
            uiState.candidates.isEmpty() -> CenteredMessage("No hay productos por debajo del stock mínimo")
            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${uiState.candidates.size} candidatos · ${uiState.selectedCount} seleccionados",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.selectedCount < uiState.candidates.size) {
                        TextButton(onClick = viewModel::onSelectAll) { Text("Seleccionar todos") }
                    } else {
                        TextButton(onClick = viewModel::onClearSelection) { Text("Quitar todos") }
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.candidates, key = { it.code }) { product ->
                        val quantity = uiState.selected[product.code]
                        CandidateItem(
                            product = product,
                            isSelected = quantity != null,
                            quantity = quantity ?: 0,
                            onToggle = { viewModel.onToggleProduct(product) },
                            onQuantityChange = { viewModel.onQuantityChange(product.code, it) }
                        )
                    }
                }
                Button(
                    onClick = viewModel::onContinue,
                    enabled = uiState.canContinue,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Continuar (${uiState.selectedCount})")
                }
            }
        }
    }
}

@Composable
private fun CandidateItem(
    product: Product,
    isSelected: Boolean,
    quantity: Int,
    onToggle: () -> Unit,
    onQuantityChange: (String) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${product.code} · ${product.brand} · ${product.line.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Stock: ${product.stock} ${product.unit}  ·  Mínimo: ${product.minStock}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                StockBadge(product.stockLevel)
            }
            if (isSelected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 48.dp, top = 4.dp)
                ) {
                    OutlinedTextField(
                        value = if (quantity == 0) "" else quantity.toString(),
                        onValueChange = onQuantityChange,
                        label = { Text("Cantidad") },
                        singleLine = true,
                        isError = quantity <= 0,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(140.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(product.unit, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}