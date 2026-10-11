package com.dsmg11.nexstock.presentation.replenishment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.Supplier
import com.dsmg11.nexstock.domain.model.UserRole
import com.dsmg11.nexstock.domain.model.suggestedQuantity
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.ObserveProductsUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import com.dsmg11.nexstock.domain.usecase.replenishment.GetReplenishmentCandidatesUseCase
import com.dsmg11.nexstock.domain.usecase.supplier.ObserveSuppliersUseCase
import com.dsmg11.nexstock.domain.model.PurchaseOrderItem
import com.dsmg11.nexstock.domain.usecase.replenishment.CreatePurchaseOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReplenishmentStep { SELECCION, DETALLE }

data class ReplenishmentUiState(
    val candidates: List<Product> = emptyList(),
    val selected: Map<String, Int> = emptyMap(),   // código del producto -> cantidad
    val myLines: List<ProductLine>? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val step: ReplenishmentStep = ReplenishmentStep.SELECCION,
    val suppliers: List<Supplier> = emptyList(),
    val supplier: Supplier? = null,
    val deliveryDate: Long? = null,                 // fecha máxima pactada (medianoche UTC, en milisegundos)
    val isSaving: Boolean = false,
    val message: String? = null                    // aviso de éxito o error para mostrar una vez
) {
    val selectedCount: Int get() = selected.size
    // Se puede continuar con al menos un producto y todas las cantidades mayores que 0
    val canContinue: Boolean get() = selected.isNotEmpty() && selected.values.all { it > 0 }
    // Se puede generar la orden con productos válidos, proveedor y fecha elegidos
    val canGenerate: Boolean get() = canContinue && supplier != null && deliveryDate != null && !isSaving
}

@HiltViewModel
class ReplenishmentViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    observeProducts: ObserveProductsUseCase,
    observeSuppliers: ObserveSuppliersUseCase,
    private val getCandidates: GetReplenishmentCandidatesUseCase,
    private val createPurchaseOrder: CreatePurchaseOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReplenishmentUiState())
    val uiState: StateFlow<ReplenishmentUiState> = _uiState.asStateFlow()

    private val profile = (getCurrentUser()?.let { observeUserProfile(it.uid) } ?: flowOf(null))
        .catch { emit(null) }

    init {
        viewModelScope.launch {
            combine(observeProducts(), profile) { all, p ->
                getCandidates(p, all) to
                        (if (p?.role == UserRole.JEFE_LINEA) p.assignedLines else null)
            }
                .catch {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudieron cargar los candidatos") }
                }
                .collect { (candidates, lines) ->
                    _uiState.update { state ->
                        val codes = candidates.map { it.code }.toSet()
                        val selected = state.selected.filterKeys { it in codes }
                        state.copy(
                            candidates = candidates,
                            // si un producto ya no es candidato, se quita de la selección
                            selected = selected,
                            // si no queda nada seleccionado, se vuelve al primer paso
                            step = if (selected.isEmpty()) ReplenishmentStep.SELECCION else state.step,
                            myLines = lines,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
        // Proveedores registrados (MOV-37). Si falla, la lista queda vacía.
        viewModelScope.launch {
            observeSuppliers()
                .catch { emit(emptyList()) }
                .collect { list -> _uiState.update { it.copy(suppliers = list) } }
        }
    }

    fun onToggleProduct(product: Product) = _uiState.update { state ->
        val selected = if (product.code in state.selected) {
            state.selected - product.code
        } else {
            state.selected + (product.code to product.suggestedQuantity)
        }
        state.copy(selected = selected)
    }

    fun onQuantityChange(code: String, text: String) = _uiState.update { state ->
        if (code !in state.selected) return@update state
        val quantity = text.filter { it.isDigit() }.take(5).toIntOrNull() ?: 0
        state.copy(selected = state.selected + (code to quantity))
    }

    fun onSelectAll() = _uiState.update { state ->
        state.copy(selected = state.candidates.associate { it.code to (state.selected[it.code] ?: it.suggestedQuantity) })
    }

    fun onClearSelection() = _uiState.update { it.copy(selected = emptyMap()) }

    // Paso 1 -> paso 2
    fun onContinue() = _uiState.update {
        if (it.canContinue) it.copy(step = ReplenishmentStep.DETALLE) else it
    }

    // Paso 2 -> paso 1 (conserva la selección, el proveedor y la fecha)
    fun onBackToSelection() = _uiState.update { it.copy(step = ReplenishmentStep.SELECCION) }

    fun onSupplierSelected(supplier: Supplier) = _uiState.update { it.copy(supplier = supplier) }

    fun onDeliveryDateSelected(dateMillis: Long) = _uiState.update { it.copy(deliveryDate = dateMillis) }

    fun onGenerate() {
        val state = _uiState.value
        val supplier = state.supplier ?: return
        val deliveryDate = state.deliveryDate ?: return
        if (!state.canGenerate) return

        val items = state.candidates
            .filter { it.code in state.selected }
            .map { p ->
                PurchaseOrderItem(
                    code = p.code,
                    name = p.name,
                    unit = p.unit,
                    line = p.line.name,
                    quantity = state.selected.getValue(p.code)
                )
            }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            createPurchaseOrder(items, supplier, deliveryDate)
                .onSuccess {
                    // Orden guardada como "Generada": se limpia el formulario
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            selected = emptyMap(),
                            supplier = null,
                            deliveryDate = null,
                            step = ReplenishmentStep.SELECCION,
                            message = "Orden de compra generada"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            message = e.message ?: "No se pudo generar la orden"
                        )
                    }
                }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }
}