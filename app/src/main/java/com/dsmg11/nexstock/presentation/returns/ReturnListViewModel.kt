package com.dsmg11.nexstock.presentation.returns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.Product
import com.dsmg11.nexstock.domain.model.ReturnStatus
import com.dsmg11.nexstock.domain.model.ReturnType
import com.dsmg11.nexstock.domain.model.Supplier
import com.dsmg11.nexstock.domain.model.SupplierReturn
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.ObserveProductsUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import com.dsmg11.nexstock.domain.usecase.returns.ObserveReturnsUseCase
import com.dsmg11.nexstock.domain.usecase.returns.RegisterReturnUseCase
import com.dsmg11.nexstock.domain.usecase.returns.ReturnPermissions
import com.dsmg11.nexstock.domain.usecase.supplier.ObserveSuppliersUseCase
import com.dsmg11.nexstock.domain.usecase.returns.AuthorizeReturnUseCase
import com.dsmg11.nexstock.domain.usecase.returns.ConfirmPickupUseCase
import com.dsmg11.nexstock.domain.usecase.returns.RequestPickupUseCase
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

data class ReturnListUiState(
    val returns: List<SupplierReturn> = emptyList(),
    val products: List<Product> = emptyList(),    // los que se pueden devolver
    val suppliers: List<Supplier> = emptyList(),
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSaving: Boolean = false,
    val message: String? = null,
    val statusFilter: ReturnStatus? = null
){
    val filteredReturns: List<SupplierReturn>
        get() = statusFilter?.let { filter -> returns.filter { it.status == filter } } ?: returns
}
@HiltViewModel
class ReturnListViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    observeReturns: ObserveReturnsUseCase,
    observeProducts: ObserveProductsUseCase,
    observeSuppliers: ObserveSuppliersUseCase,
    private val registerReturn: RegisterReturnUseCase,
    private val authorizeReturn: AuthorizeReturnUseCase,
    private val requestPickup: RequestPickupUseCase,
    private val confirmPickup: ConfirmPickupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReturnListUiState())
    val uiState: StateFlow<ReturnListUiState> = _uiState.asStateFlow()

    private val profile = (getCurrentUser()?.let { observeUserProfile(it.uid) } ?: flowOf(null))
        .catch { emit(null) }

    init {
        viewModelScope.launch {
            combine(
                observeReturns(),
                observeProducts().catch { emit(emptyList()) },
                observeSuppliers().catch { emit(emptyList()) },
                profile
            ) { returns, products, suppliers, p ->
                ReturnListUiState(
                    returns = ReturnPermissions.visibleReturns(p, returns),
                    products = ReturnPermissions.registrableProducts(p, products),
                    suppliers = suppliers.sortedBy { it.razonSocial },
                    profile = p,
                    isLoading = false
                )
            }
                .catch {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudieron cargar las devoluciones") }
                }
                .collect { fresh ->
                    _uiState.update {
                        fresh.copy(isSaving = it.isSaving, message = it.message, statusFilter = it.statusFilter)
                    }
                }
        }
    }

    fun onRegister(
        product: Product,
        supplier: Supplier,
        quantity: Int,
        type: ReturnType,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            registerReturn(_uiState.value.profile, product, supplier, quantity, type)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, message = "Devolución registrada") }
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, message = e.message ?: "No se pudo registrar la devolución")
                    }
                }
        }
    }

    fun onAuthorize(item: SupplierReturn) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            authorizeReturn(_uiState.value.profile, item)
                .onSuccess {
                    _uiState.update {
                        it.copy(isSaving = false, message = "Devolución autorizada. Se avisó al Administrador")
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, message = e.message ?: "No se pudo autorizar la devolución")
                    }
                }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onFilterChange(status: ReturnStatus?) = _uiState.update { it.copy(statusFilter = status) }

    fun onRequestPickup(item: SupplierReturn) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            requestPickup(_uiState.value.profile, item)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, message = "Devolución pendiente de recojo") }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, message = e.message ?: "No se pudo coordinar el recojo")
                    }
                }
        }
    }

    fun onConfirmPickup(item: SupplierReturn) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            confirmPickup(_uiState.value.profile, item)
                .onSuccess {
                    _uiState.update {
                        it.copy(isSaving = false, message = "Recojo confirmado. Stock descontado")
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, message = e.message ?: "No se pudo confirmar el recojo")
                    }
                }
        }
    }
}