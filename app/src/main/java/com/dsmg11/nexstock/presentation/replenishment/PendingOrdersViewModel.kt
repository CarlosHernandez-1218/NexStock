package com.dsmg11.nexstock.presentation.replenishment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.PurchaseOrder
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import com.dsmg11.nexstock.domain.usecase.replenishment.ObserveOrdersUseCase
import com.dsmg11.nexstock.domain.usecase.replenishment.PendingOrdersReport
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

data class PendingOrdersUiState(
    val orders: List<PurchaseOrder> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PendingOrdersViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    observeOrders: ObserveOrdersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendingOrdersUiState())
    val uiState: StateFlow<PendingOrdersUiState> = _uiState.asStateFlow()

    private val profile = (getCurrentUser()?.let { observeUserProfile(it.uid) } ?: flowOf(null))
        .catch { emit(null) }

    init {
        viewModelScope.launch {
            combine(observeOrders(), profile) { orders, p ->
                PendingOrdersReport.build(p, orders)
            }
                .catch {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar el reporte") }
                }
                .collect { orders ->
                    _uiState.update { it.copy(orders = orders, isLoading = false, error = null) }
                }
        }
    }
}