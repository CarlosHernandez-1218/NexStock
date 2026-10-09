package com.dsmg11.nexstock.presentation.master

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.MasterItem
import com.dsmg11.nexstock.domain.model.MasterType
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.usecase.master.AddMasterItemUseCase
import com.dsmg11.nexstock.domain.usecase.master.ObserveMasterItemsUseCase
import com.dsmg11.nexstock.domain.usecase.master.SetMasterItemActiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MasterDataUiState(
    val selectedType: MasterType = MasterType.UNIDADES,
    val items: Map<MasterType, List<MasterItem>> = emptyMap(),
    // Paso 4: las líneas vendrán de la colección product_lines
    val lines: List<MasterItem> = ProductLine.entries.map { MasterItem(it.name, it.displayName) },
    val selectedLineId: String? = ProductLine.entries.first().name,
    val newName: String = "",
    val message: String? = null
) {
    // Las categorías se muestran solo las de la línea elegida
    val visibleItems: List<MasterItem>
        get() = items[selectedType].orEmpty().let { list ->
            if (selectedType == MasterType.CATEGORIAS) list.filter { it.parentId == selectedLineId }
            else list
        }
}

@HiltViewModel
class MasterDataViewModel @Inject constructor(
    observeMasterItems: ObserveMasterItemsUseCase,
    private val addMasterItem: AddMasterItemUseCase,
    private val setMasterItemActive: SetMasterItemActiveUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MasterDataUiState())
    val uiState: StateFlow<MasterDataUiState> = _uiState.asStateFlow()

    init {
        // Escucha en tiempo real cada colección de datos maestros
        MasterType.entries.forEach { type ->
            viewModelScope.launch {
                observeMasterItems(type)
                    .catch {
                        _uiState.update { it.copy(message = "No se pudo cargar ${type.title.lowercase()}") }
                    }
                    .collect { list ->
                        _uiState.update { it.copy(items = it.items + (type to list)) }
                    }
            }
        }
    }

    fun onTypeSelected(type: MasterType) = _uiState.update { it.copy(selectedType = type, newName = "") }
    fun onLineSelected(lineId: String) = _uiState.update { it.copy(selectedLineId = lineId) }
    fun onNewNameChange(value: String) = _uiState.update { it.copy(newName = value) }

    fun add() {
        val state = _uiState.value
        val parentId = if (state.selectedType == MasterType.CATEGORIAS) state.selectedLineId else null
        viewModelScope.launch {
            addMasterItem(state.selectedType, state.newName, state.items[state.selectedType].orEmpty(), parentId)
                .onSuccess { _uiState.update { it.copy(newName = "", message = "Registrado correctamente") } }
                .onFailure { e -> _uiState.update { it.copy(message = e.message) } }
        }
    }

    fun toggleActive(item: MasterItem) {
        val type = _uiState.value.selectedType
        viewModelScope.launch {
            setMasterItemActive(type, item.id, !item.active)
                .onFailure { e -> _uiState.update { it.copy(message = e.message) } }
        }
    }

    fun messageShown() = _uiState.update { it.copy(message = null) }
}