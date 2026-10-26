package com.dsmg11.nexstock.presentation.replenishment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.OrderSchedule
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.ScheduleFrequency
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.CatalogPermissions
import com.dsmg11.nexstock.domain.usecase.master.ObserveProductLinesUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import com.dsmg11.nexstock.domain.usecase.replenishment.ObserveOrderSchedulesUseCase
import com.dsmg11.nexstock.domain.usecase.replenishment.SaveOrderScheduleUseCase
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

data class OrderScheduleUiState(
    val lines: List<ProductLine> = emptyList(),               // líneas que el usuario puede programar
    val schedules: Map<String, OrderSchedule> = emptyMap(),   // código de línea -> cronograma
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSaving: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class OrderScheduleViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    observeProductLines: ObserveProductLinesUseCase,
    observeSchedules: ObserveOrderSchedulesUseCase,
    private val saveSchedule: SaveOrderScheduleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderScheduleUiState())
    val uiState: StateFlow<OrderScheduleUiState> = _uiState.asStateFlow()

    private val profile = (getCurrentUser()?.let { observeUserProfile(it.uid) } ?: flowOf(null))
        .catch { emit(null) }

    init {
        viewModelScope.launch {
            combine(observeProductLines(), profile, observeSchedules()) { allLines, p, schedules ->
                CatalogPermissions.allowedLines(p, allLines) to schedules.associateBy { it.lineCode }
            }
                .catch {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar el cronograma") }
                }
                .collect { (lines, schedules) ->
                    _uiState.update {
                        it.copy(lines = lines, schedules = schedules, isLoading = false, error = null)
                    }
                }
        }
    }

    fun onSave(line: ProductLine, frequency: ScheduleFrequency, nextDate: Long) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            saveSchedule(line.name, frequency, nextDate)
                .onSuccess {
                    _uiState.update {
                        it.copy(isSaving = false, message = "Cronograma de ${line.displayName} guardado")
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, message = e.message ?: "No se pudo guardar")
                    }
                }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }
}