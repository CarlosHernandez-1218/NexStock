package com.dsmg11.nexstock.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.DashboardSummary
import com.dsmg11.nexstock.domain.usecase.dashboard.ObserveDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val summary: DashboardSummary = DashboardSummary(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeSummary: ObserveDashboardSummaryUseCase
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = observeSummary()
        .map { DashboardUiState(summary = it, isLoading = false) }
        .catch { emit(DashboardUiState(isLoading = false, error = "No se pudieron cargar los indicadores")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
