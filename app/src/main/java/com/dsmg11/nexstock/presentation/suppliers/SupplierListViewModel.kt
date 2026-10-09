package com.dsmg11.nexstock.presentation.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.Supplier
import com.dsmg11.nexstock.domain.usecase.supplier.ObserveSuppliersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierListUiState(
    val suppliers: List<Supplier> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
) {
    // Búsqueda por RUC o razón social
    val filtered: List<Supplier>
        get() = query.trim().let { q ->
            if (q.isEmpty()) suppliers
            else suppliers.filter { it.ruc.contains(q) || it.razonSocial.contains(q, ignoreCase = true) }
        }
}

@HiltViewModel
class SupplierListViewModel @Inject constructor(
    observeSuppliers: ObserveSuppliersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierListUiState())
    val uiState: StateFlow<SupplierListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSuppliers()
                .catch {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar los proveedores") }
                }
                .collect { list -> _uiState.update { it.copy(suppliers = list, isLoading = false) } }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }
}