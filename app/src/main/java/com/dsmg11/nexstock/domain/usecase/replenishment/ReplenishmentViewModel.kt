package com.dsmg11.nexstock.presentation.replenishment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.UserRole
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
    val myLines: List<ProductLine>? = null,   // null = ve todas las líneas
    val isLoading: Boolean = true,
    val error: String? = null
)

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
                    _uiState.update {
                        it.copy(candidates = candidates, myLines = lines, isLoading = false, error = null)
                    }
                }
        }
    }
}