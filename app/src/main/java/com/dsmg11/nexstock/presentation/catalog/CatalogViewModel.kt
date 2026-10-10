package com.dsmg11.nexstock.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.CatalogPermissions
import com.dsmg11.nexstock.domain.usecase.catalog.ObserveProductsUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.SearchProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val totalCount: Int = 0,
    val query: String = "",
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val message: String? = null
) {
    val canCreate: Boolean get() = CatalogPermissions.canCreate(profile)
    fun canEdit(product: Product): Boolean = CatalogPermissions.canEdit(profile, product)
}

@HiltViewModel
class CatalogViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    observeProducts: ObserveProductsUseCase,
    private val searchProducts: SearchProductsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()
    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            combine(observeProducts(), query) { all, q -> all to searchProducts(all, q) }
                .catch {
                    _uiState.update { it.copy(isLoading = false, message = "No se pudo cargar el catálogo") }
                }
                .collect { (all, filtered) ->
                    _uiState.update {
                        it.copy(products = filtered, totalCount = all.size, isLoading = false)
                    }
                }
        }
        getCurrentUser()?.let { user ->
            viewModelScope.launch {
                observeUserProfile(user.uid)
                    .catch { }
                    .collect { profile -> _uiState.update { it.copy(profile = profile) } }
            }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
        _uiState.update { it.copy(query = value) }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }
    // MOV-21: si el escáner falla, se muestra en el snackbar
    fun onScanError(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}