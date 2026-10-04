package com.dsmg11.nexstock.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.CatalogPermissions
import com.dsmg11.nexstock.domain.usecase.catalog.LoadSampleProductsUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.ObserveProductsUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val isLoadingSamples: Boolean = false,
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
    private val loadSampleProducts: LoadSampleProductsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeProducts()
                .catch {
                    _uiState.update { it.copy(isLoading = false, message = "No se pudo cargar el catálogo") }
                }
                .collect { products ->
                    _uiState.update { it.copy(products = products, isLoading = false) }
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

    fun loadSamples() {
        if (_uiState.value.isLoadingSamples) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSamples = true) }
            loadSampleProducts()
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(isLoadingSamples = false, message = "Se cargaron $count productos de ejemplo")
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoadingSamples = false, message = e.message ?: "No se pudieron cargar")
                    }
                }
        }
    }

    fun messageShown() {
        _uiState.update { it.copy(message = null) }
    }
}