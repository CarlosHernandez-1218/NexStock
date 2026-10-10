package com.dsmg11.nexstock.presentation.replenishment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.UserRole
import com.dsmg11.nexstock.domain.model.suggestedQuantity
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.ObserveProductsUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import com.dsmg11.nexstock.domain.usecase.replenishment.GetReplenishmentCandidatesUseCase
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

data class ReplenishmentUiState(
    val candidates: List<Product> = emptyList(),
    val selected: Map<String, Int> = emptyMap(),   // código del producto -> cantidad
    val myLines: List<ProductLine>? = null,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val selectedCount: Int get() = selected.size
    // Se puede continuar con al menos un producto y todas las cantidades mayores que 0
    val canContinue: Boolean get() = selected.isNotEmpty() && selected.values.all { it > 0 }
}

@HiltViewModel
class ReplenishmentViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    observeProducts: ObserveProductsUseCase,
    private val getCandidates: GetReplenishmentCandidatesUseCase
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
                        state.copy(
                            candidates = candidates,
                            // si un producto ya no es candidato, se quita de la selección
                            selected = state.selected.filterKeys { it in codes },
                            myLines = lines,
                            isLoading = false,
                            error = null
                        )
                    }
                }
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
}