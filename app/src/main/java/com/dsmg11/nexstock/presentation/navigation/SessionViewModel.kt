package com.dsmg11.nexstock.presentation.navigation

import androidx.lifecycle.ViewModel
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase
) : ViewModel() {

    // Firebase guarda la sesión en el dispositivo: si hay usuario, vamos directo a Home
    val startDestination: String =
        if (getCurrentUser() != null) Screen.Home.route else Screen.Login.route
}