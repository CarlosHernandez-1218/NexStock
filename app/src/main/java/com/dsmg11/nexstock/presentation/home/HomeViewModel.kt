package com.dsmg11.nexstock.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.AppFeature
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.SignOutUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.CatalogPermissions
import com.dsmg11.nexstock.domain.usecase.catalog.CheckStockAlertsUseCase
import com.dsmg11.nexstock.domain.usecase.catalog.ObserveProductsUseCase
import com.dsmg11.nexstock.domain.usecase.profile.GetAvailableFeaturesUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    getAvailableFeatures: GetAvailableFeaturesUseCase,
    observeProducts: ObserveProductsUseCase,
    private val checkStockAlerts: CheckStockAlertsUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val currentUser = getCurrentUser()

    val userEmail: String = currentUser?.email ?: "Invitado"

    val profile: StateFlow<UserProfile?> =
        (currentUser?.let { observeUserProfile(it.uid) } ?: flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // El menú se recalcula solo si el Administrador cambia el rol
    val features: StateFlow<List<AppFeature>> =
        profile.map { it?.let { p -> getAvailableFeatures(p.role) } ?: emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // MOV-31: mientras la sesión está abierta, avisa cuando un producto llega al stock mínimo
        // (el Jefe de Línea solo recibe avisos de sus líneas)
        viewModelScope.launch {
            combine(observeProducts(), profile.filterNotNull()) { products, p ->
                CatalogPermissions.visibleProducts(p, products)
            }
                .catch { }
                .collect { visible -> checkStockAlerts(visible) }
        }
    }

    fun signOut() = signOutUseCase()
}