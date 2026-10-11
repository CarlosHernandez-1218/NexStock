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
    val updatingId: String? = null,    // orden que se está actualizando
    val message: String? = null        // aviso para mostrar una vez
)

@HiltViewModel
class OrderListViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    observeOrders: ObserveOrdersUseCase,
    private val changeOrderStatus: ChangeOrderStatusUseCase
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
                    _uiState.update {
                        it.copy(orders = orders, profile = p, isLoading = false, error = null)
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

    fun onMessageShown() = _uiState.update { it.copy(message = null) }
}