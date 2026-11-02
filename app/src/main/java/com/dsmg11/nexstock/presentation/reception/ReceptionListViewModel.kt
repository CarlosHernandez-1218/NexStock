package com.dsmg11.nexstock.presentation.reception

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.usecase.reception.ObserveOrdersToReceiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceptionListUiState(
    val orders: List<PurchaseOrder> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
) {
    // Búsqueda por proveedor, RUC, número de orden o producto
    val filteredOrders: List<PurchaseOrder>
        get() {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return orders
            return orders.filter { order ->
                order.supplierName.lowercase().contains(q) ||
                        order.supplierRuc.contains(q) ||
                        order.id.takeLast(6).lowercase().contains(q) ||
                        order.items.any { it.name.lowercase().contains(q) || it.code.lowercase().contains(q) }
            }
        }
}

@HiltViewModel
class ReceptionListViewModel @Inject constructor(
    observeOrdersToReceive: ObserveOrdersToReceiveUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceptionListUiState())
    val uiState: StateFlow<ReceptionListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeOrdersToReceive()
                .catch {
                    _uiState.update { state ->
                        state.copy(isLoading = false, error = "No se pudieron cargar las órdenes por recibir")
                    }
                }
                .collect { orders ->
                    _uiState.update { state -> state.copy(orders = orders, isLoading = false, error = null) }
                }
        }
    }

    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query) }
}
