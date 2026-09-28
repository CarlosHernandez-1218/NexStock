package com.dsmg11.nexstock.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.SignOutUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val currentUser = getCurrentUser()

    val userEmail: String = currentUser?.email ?: "Invitado"

    val profile: StateFlow<UserProfile?> =
        (currentUser?.let { observeUserProfile(it.uid) } ?: flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun signOut() = signOutUseCase()
}