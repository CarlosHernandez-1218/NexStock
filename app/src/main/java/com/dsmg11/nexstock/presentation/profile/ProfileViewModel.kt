package com.dsmg11.nexstock.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.profile.ObserveUserProfileUseCase
import com.dsmg11.nexstock.domain.usecase.profile.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: UserProfile? = null,
    val fullName: String = "",
    val phone: String = "",
    val fieldsLoaded: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    private val updateProfile: UpdateProfileUseCase
) : ViewModel() {

    private val uid = getCurrentUser()?.uid

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        uid?.let { id ->
            viewModelScope.launch {
                observeUserProfile(id).collect { profile ->
                    _uiState.update { state ->
                        // Los campos se llenan solo la primera vez, para no borrar lo que el usuario está escribiendo
                        if (profile != null && !state.fieldsLoaded) {
                            state.copy(
                                profile = profile,
                                fullName = profile.fullName,
                                phone = profile.phone,
                                fieldsLoaded = true
                            )
                        } else {
                            state.copy(profile = profile)
                        }
                    }
                }
            }
        }
    }

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, errorMessage = null, successMessage = null) }
    }

    fun onPhoneChange(value: String) {
        val digits = value.filter(Char::isDigit).take(9)
        _uiState.update { it.copy(phone = digits, errorMessage = null, successMessage = null) }
    }

    fun save() {
        val id = uid ?: return
        val state = _uiState.value
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            updateProfile(id, state.fullName, state.phone)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, successMessage = "Perfil actualizado") }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = e.message ?: "No se pudo guardar")
                    }
                }
        }
    }
}