package com.dsmg11.nexstock.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.ProductStatus
import com.dsmg11.nexstock.domain.model.RotationLevel
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.CatalogPermissions
import com.dsmg11.nexstock.domain.usecase.catalog.ObserveProductUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.SaveProductUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.dsmg11.nexstock.domain.model.MasterItem
import com.dsmg11.nexstock.domain.model.MasterType
import com.dsmg11.nexstock.domain.usecase.master.ObserveMasterItemsUseCase
import com.dsmg11.nexstock.domain.usecase.master.ObserveProductLinesUseCase
import kotlinx.coroutines.flow.catch

data class ProductFormUiState(
    val isNew: Boolean = true,
    val code: String = "",
    val name: String = "",
    val brand: String = "",
    val line: ProductLine? = null,
    val category: String = "",
    val unit: String = "",
    val price: String = "",
    val minStock: String = "",
    val status: ProductStatus = ProductStatus.ACTIVO,
    val rotation: RotationLevel = RotationLevel.MEDIA,
    val barcode: String = "",
    val allowedLines: List<ProductLine> = emptyList(),
    val units: List<String> = emptyList(),              // De la colección units
    val allCategories: List<MasterItem> = emptyList(),  // De la colección categories
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false
) {
    // Solo las categorías de la línea elegida
    val categoriesForLine: List<String>
        get() = allCategories.filter { it.parentId == line?.name }.map { it.name }
}

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    observeProduct: ObserveProductUseCase,
    observeMasterItems: ObserveMasterItemsUseCase,
    observeProductLines: ObserveProductLinesUseCase,
    private val saveProduct: SaveProductUseCase
) : ViewModel() {

    private val editingCode: String? = savedStateHandle["code"]
    private var original: Product? = null

    private val _uiState = MutableStateFlow(ProductFormUiState(isNew = editingCode == null))
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    init {
        // Unidades y categorías activas desde la base de datos (se actualizan en tiempo real)
        viewModelScope.launch {
            observeMasterItems(MasterType.UNIDADES, onlyActive = true)
                .catch { }
                .collect { list -> _uiState.update { it.copy(units = list.map { u -> u.name }) } }
        }
        viewModelScope.launch {
            observeMasterItems(MasterType.CATEGORIAS, onlyActive = true)
                .catch { }
                .collect { list -> _uiState.update { it.copy(allCategories = list) } }
        }
        viewModelScope.launch {
            val profile = getCurrentUser()?.let { user ->
                observeUserProfile(user.uid).filterNotNull().first()
            }
            // Líneas activas desde product_lines
            val activeLines = observeProductLines(onlyActive = true).first()
            val lines = CatalogPermissions.allowedLines(profile, activeLines)
            _uiState.update { it.copy(allowedLines = lines, line = lines.firstOrNull()) }

            editingCode?.let { code ->
                val product = observeProduct(code).filterNotNull().first()
                original = product
                _uiState.update {
                    it.copy(
                        code = product.code,
                        name = product.name,
                        brand = product.brand,
                        line = lines.firstOrNull { it == product.line } ?: product.line,
                        category = product.category,
                        unit = product.unit,
                        price = product.price.toString(),
                        minStock = product.minStock.toString(),
                        status = product.status,
                        rotation = product.rotation,
                        barcode = product.barcode.orEmpty()
                    )
                }
            }
        }
    }

    fun onCodeChange(value: String) = _uiState.update { it.copy(code = value.uppercase()) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onBrandChange(value: String) = _uiState.update { it.copy(brand = value) }
    fun onLineChange(value: ProductLine) = _uiState.update {
        if (it.line == value) it else it.copy(line = value, category = "")
    }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(category = value) }
    fun onUnitChange(value: String) = _uiState.update { it.copy(unit = value) }
    fun onPriceChange(value: String) =
        _uiState.update { it.copy(price = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
    fun onMinStockChange(value: String) = _uiState.update { it.copy(minStock = value.filter(Char::isDigit)) }
    fun onStatusChange(value: ProductStatus) = _uiState.update { it.copy(status = value) }
    fun onRotationChange(value: RotationLevel) = _uiState.update { it.copy(rotation = value) }
    fun onBarcodeChange(value: String) = _uiState.update { it.copy(barcode = value.filter(Char::isDigit)) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return

        val line = state.line
        val price = state.price.replace(',', '.').toDoubleOrNull()
        val minStock = state.minStock.toIntOrNull()
        val error = when {
            line == null -> "Selecciona la línea"
            price == null -> "Ingresa un precio válido"
            minStock == null -> "Ingresa el stock mínimo"
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        val product = Product(
            code = state.code,
            name = state.name,
            brand = state.brand,
            line = line!!,
            category = state.category,
            unit = state.unit,
            price = price!!,
            minStock = minStock!!,
            stock = original?.stock ?: 0,      // El stock no se edita aquí (MOV-26)
            status = state.status,
            rotation = state.rotation,
            barcode = state.barcode
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            saveProduct(product, isNew = state.isNew)
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.message) } }
        }
    }
}