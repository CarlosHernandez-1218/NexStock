package com.dsmg11.nexstock.presentation.reception

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.isPendingReception
import com.dsmg11.nexstock.domain.model.pendingOf
import com.dsmg11.nexstock.domain.usecase.reception.FindProductCodeByBarcodeUseCase
import com.dsmg11.nexstock.domain.usecase.reception.ObserveOrderUseCase
import com.dsmg11.nexstock.domain.usecase.reception.RegisterReceptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceptionRegisterUiState(
    val order: PurchaseOrder? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val quantities: Map<String, Int> = emptyMap(),   // código de producto -> cantidad recibida ahora
    val highlightedCode: String? = null,            // último producto escaneado
    val scanCount: Int = 0,                         // sube en cada escaneo válido (para desplazar la lista)
    val message: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false                      // true al guardar: la pantalla se cierra
) {
    val totalUnits: Int get() = quantities.values.sum()
    val canReview: Boolean get() = totalUnits > 0 && !isSaving && order?.isPendingReception == true

    // ¿Con lo que se recibe ahora ya no falta nada de la orden?
    val completesOrder: Boolean
        get() {
            val o = order ?: return false
            return o.items.all { item -> o.pendingOf(item) - (quantities[item.code] ?: 0) <= 0 }
        }
}

@HiltViewModel
class ReceptionRegisterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeOrder: ObserveOrderUseCase,
    private val findProductCode: FindProductCodeByBarcodeUseCase,
    private val registerReception: RegisterReceptionUseCase
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _uiState = MutableStateFlow(ReceptionRegisterUiState())
    val uiState: StateFlow<ReceptionRegisterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeOrder(orderId)
                .catch {
                    _uiState.update { state -> state.copy(isLoading = false, error = "No se pudo cargar la orden") }
                }
                .collect { order ->
                    _uiState.update { state ->
                        // Si mientras tanto llegó otra recepción, ninguna cantidad puede pasar de lo que falta
                        val clamped = if (order == null) emptyMap() else state.quantities.mapValues { (code, qty) ->
                            val item = order.items.firstOrNull { it.code == code }
                            if (item == null) 0 else qty.coerceAtMost(order.pendingOf(item))
                        }
                        state.copy(order = order, isLoading = false, error = null, quantities = clamped)
                    }
                }
        }
    }

    // MOV-48: llega un código de barras desde la cámara
    fun onBarcodeScanned(barcode: String) {
        val order = _uiState.value.order ?: return
        viewModelScope.launch {
            val code = findProductCode(barcode)
            val item = code?.let { c -> order.items.firstOrNull { it.code == c } }
            when {
                code == null -> showMessage("El código $barcode no está registrado en el catálogo")
                item == null -> showMessage("Ese producto no está en esta orden de compra")
                else -> {
                    val current = _uiState.value.quantities[item.code] ?: 0
                    val pending = order.pendingOf(item)
                    if (current >= pending) {
                        _uiState.update {
                            it.copy(
                                highlightedCode = item.code,
                                scanCount = it.scanCount + 1,
                                message = "Ya completaste lo que falta de ${item.name}"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                quantities = it.quantities + (item.code to current + 1),
                                highlightedCode = item.code,
                                scanCount = it.scanCount + 1,
                                message = "${item.name}: ${current + 1} de $pending"
                            )
                        }
                    }
                }
            }
        }
    }

    fun onScannerError(message: String) = showMessage(message)

    fun onIncrement(code: String) = setQuantity(code, (_uiState.value.quantities[code] ?: 0) + 1)

    fun onDecrement(code: String) = setQuantity(code, (_uiState.value.quantities[code] ?: 0) - 1)

    // Lo que el almacenero escribe a mano en el campo de cantidad
    fun onQuantityTyped(code: String, text: String) {
        val digits = text.filter { it.isDigit() }.take(5)
        setQuantity(code, digits.toIntOrNull() ?: 0)
    }

    private fun setQuantity(code: String, requested: Int) {
        val order = _uiState.value.order ?: return
        val item = order.items.firstOrNull { it.code == code } ?: return
        val max = order.pendingOf(item)
        val value = requested.coerceIn(0, max)
        _uiState.update {
            it.copy(
                quantities = it.quantities + (code to value),
                message = if (requested > max) "Solo faltan $max ${item.unit} de ${item.name}" else it.message
            )
        }
    }

    // MOV-50: guarda la recepción; el stock y la orden se actualizan al instante en el celular
    fun onSave() {
        val state = _uiState.value
        val order = state.order ?: return
        if (!state.canReview) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            registerReception(order, state.quantities)
                .onSuccess { reception ->
                    val units = reception.items.sumOf { it.received }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saved = true,
                            message = "Recepción registrada: $units unidades. Se sincroniza automáticamente."
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, message = e.message ?: "No se pudo registrar la recepción")
                    }
                }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun showMessage(text: String) = _uiState.update { it.copy(message = text) }
}
