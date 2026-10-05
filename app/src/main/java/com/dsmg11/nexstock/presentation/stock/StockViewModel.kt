package com.dsmg11.nexstock.presentation.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.RotationLevel
import com.dsmg11.nexstock.domain.model.StockFilters
import com.dsmg11.nexstock.domain.model.StockGrouping
import com.dsmg11.nexstock.domain.model.StockLevel
import com.dsmg11.nexstock.domain.model.stockLevel
import com.dsmg11.nexstock.domain.usecase.catalog.FilterProductsUseCase
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
    val filters: StockFilters = StockFilters(),
    val availableBrands: List<String> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val isFiltering: Boolean get() = query.isNotBlank() || !filters.isEmpty
}

private data class StockResult(
    val total: Int,
    val count: Int,
    val sections: List<StockSection>,
    val brands: List<String>,
    val categories: List<String>
)

@HiltViewModel
class StockViewModel @Inject constructor(
    observeProducts: ObserveProductsUseCase,
    private val searchProducts: SearchProductsUseCase,
    private val filterProducts: FilterProductsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockUiState())
    val uiState: StateFlow<StockUiState> = _uiState.asStateFlow()

    private val query = MutableStateFlow("")
    private val grouping = MutableStateFlow(StockGrouping.PRODUCTO)
    private val filters = MutableStateFlow(StockFilters())

    init {
        viewModelScope.launch {
            combine(observeProducts(), query, grouping, filters) { all, q, g, f ->
                val result = filterProducts(searchProducts(all, q), f)
                StockResult(
                    total = all.size,
                    count = result.size,
                    sections = buildSections(result, g),
                    brands = all.map { it.brand }.distinct().sorted(),
                    categories = all.map { it.category }.distinct().sorted()
                )
            }
                .catch {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar el stock") }
                }
                .collect { r ->
                    _uiState.update {
                        it.copy(
                            totalCount = r.total,
                            resultCount = r.count,
                            sections = r.sections,
                            availableBrands = r.brands,
                            availableCategories = r.categories,
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

    fun onToggleBrand(brand: String) = updateFilters { copy(brands = brands.toggle(brand)) }
    fun onToggleCategory(category: String) = updateFilters { copy(categories = categories.toggle(category)) }
    fun onToggleRotation(rotation: RotationLevel) = updateFilters { copy(rotations = rotations.toggle(rotation)) }
    fun onToggleLevel(level: StockLevel) = updateFilters { copy(levels = levels.toggle(level)) }
    fun onClearFilters() = updateFilters { StockFilters() }

    private fun updateFilters(change: StockFilters.() -> StockFilters) {
        val updated = filters.value.change()
        filters.value = updated
        _uiState.update { it.copy(filters = updated) }
    }

    private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

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