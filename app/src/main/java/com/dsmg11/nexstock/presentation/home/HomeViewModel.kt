package com.dsmg11.nexstock.presentation.home

import androidx.lifecycle.ViewModel
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    val userEmail: String = getCurrentUser()?.email ?: "Invitado"

    fun signOut() = signOutUseCase()
}