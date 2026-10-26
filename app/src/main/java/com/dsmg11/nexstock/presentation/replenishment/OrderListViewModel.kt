package com.dsmg11.nexstock.presentation.replenishment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.OrderStatus
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import com.dsmg11.nexstock.domain.usecase.replenishment.ChangeOrderStatusUseCase
import com.dsmg11.nexstock.domain.usecase.replenishment.ObserveOrdersUseCase
import com.dsmg11.nexstock.domain.usecase.replenishment.OrderPermissions
import com.dsmg11.nexstock.domain.usecase.replenishment.ConsolidateOrdersUseCase
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

data class OrderListUiState(
    val orders: List<PurchaseOrder> = emptyList(),
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val updatingId: String? = null,
    val message: String? = null,
    val selectedIds: Set<String> = emptySet(),     // órdenes marcadas para consolidar
    val isConsolidating: Boolean = false
) {
    val selectedOrders: List<PurchaseOrder> get() = orders.filter { it.id in selectedIds }
    // Con una orden marcada, solo se pueden marcar las del mismo proveedor
    val selectedSupplierRuc: String? get() = selectedOrders.firstOrNull()?.supplierRuc
    val canConsolidate: Boolean get() = selectedOrders.size >= 2 && !isConsolidating
}

@HiltViewModel
class OrderListViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    observeOrders: ObserveOrdersUseCase,
    private val changeOrderStatus: ChangeOrderStatusUseCase,
    private val consolidateOrders: ConsolidateOrdersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderListUiState())
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    private val profile = (getCurrentUser()?.let { observeUserProfile(it.uid) } ?: flowOf(null))
        .catch { emit(null) }

    init {
        viewModelScope.launch {
            combine(observeOrders(), profile) { orders, p ->
                OrderPermissions.visibleOrders(p, orders) to p
            }
                .catch {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudieron cargar las órdenes") }
                }
                .collect { (orders, p) ->
                    _uiState.update { state ->
                        val stillValid = state.selectedIds.filter { id ->
                            orders.any { it.id == id && it.status == OrderStatus.GENERADA }
                        }.toSet()
                        state.copy(
                            orders = orders, profile = p, isLoading = false, error = null,
                            selectedIds = stillValid
                        )
                    }
                }
        }
    }

    fun onChangeStatus(order: PurchaseOrder, next: OrderStatus) {
        if (_uiState.value.updatingId != null) return
        _uiState.update { it.copy(updatingId = order.id) }
        viewModelScope.launch {
            changeOrderStatus(order, next)
                .onSuccess {
                    _uiState.update {
                        it.copy(updatingId = null, message = "La orden pasó a ${next.displayName}")
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(updatingId = null, message = e.message ?: "No se pudo cambiar el estado")
                    }
                }
        }
    }

    fun onToggleSelect(order: PurchaseOrder) = _uiState.update { state ->
        val ids = if (order.id in state.selectedIds) state.selectedIds - order.id
        else state.selectedIds + order.id
        state.copy(selectedIds = ids)
    }

    fun onClearSelection() = _uiState.update { it.copy(selectedIds = emptySet()) }

    fun onConsolidate() {
        val state = _uiState.value
        if (!state.canConsolidate) return
        _uiState.update { it.copy(isConsolidating = true) }
        viewModelScope.launch {
            consolidateOrders(state.selectedOrders)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isConsolidating = false,
                            selectedIds = emptySet(),
                            message = "Órdenes consolidadas. Ya puedes aprobar la orden nueva"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isConsolidating = false, message = e.message ?: "No se pudo consolidar")
                    }
                }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }
}