package com.dsmg11.nexstock.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.AppFeature
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.SignOutUseCase
import com.dsmg11.nexstock.domain.usecase.profile.GetAvailableFeaturesUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    getAvailableFeatures: GetAvailableFeaturesUseCase,
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

    fun signOut() = signOutUseCase()
}