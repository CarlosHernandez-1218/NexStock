package com.dsmg11.nexstock.presentation.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.StockGrouping
import com.dsmg11.nexstock.domain.model.StockLevel
import com.dsmg11.nexstock.domain.model.stockLevel
import com.dsmg11.nexstock.domain.usecase.catalog.ObserveProductsUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.SearchProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Un bloque de la lista. En la vista "Producto" hay un solo bloque sin título.
data class StockSection(
    val title: String?,
    val products: List<Product>
) {
    val alertCount: Int get() = products.count { it.stockLevel != StockLevel.NORMAL }
}

data class StockUiState(
    val sections: List<StockSection> = emptyList(),
    val resultCount: Int = 0,
    val totalCount: Int = 0,
    val query: String = "",
    val grouping: StockGrouping = StockGrouping.PRODUCTO,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StockViewModel @Inject constructor(
    observeProducts: ObserveProductsUseCase,
    private val searchProducts: SearchProductsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockUiState())
    val uiState: StateFlow<StockUiState> = _uiState.asStateFlow()

    private val query = MutableStateFlow("")
    private val grouping = MutableStateFlow(StockGrouping.PRODUCTO)

    init {
        viewModelScope.launch {
            combine(observeProducts(), query, grouping) { all, q, g ->
                val filtered = searchProducts(all, q)
                Triple(all.size, filtered.size, buildSections(filtered, g))
            }
                .catch {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar el stock") }
                }
                .collect { (total, count, sections) ->
                    _uiState.update {
                        it.copy(
                            totalCount = total,
                            resultCount = count,
                            sections = sections,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
        _uiState.update { it.copy(query = value) }
    }

    fun onGroupingChange(value: StockGrouping) {
        grouping.value = value
        _uiState.update { it.copy(grouping = value) }
    }

    private fun buildSections(products: List<Product>, grouping: StockGrouping): List<StockSection> =
        when (grouping) {
            StockGrouping.PRODUCTO ->
                if (products.isEmpty()) emptyList() else listOf(StockSection(null, products))
            StockGrouping.LINEA -> products.groupBy { it.line.displayName }.toSections()
            StockGrouping.CATEGORIA -> products.groupBy { it.category }.toSections()
        }

    private fun Map<String, List<Product>>.toSections(): List<StockSection> =
        entries.sortedBy { it.key }.map { StockSection(it.key, it.value) }
}