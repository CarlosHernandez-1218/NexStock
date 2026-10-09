package com.dsmg11.nexstock.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.ProductLine
import com.dsmg11.nexstock.domain.model.UserProfile
import com.dsmg11.nexstock.domain.model.UserRole
import com.dsmg11.nexstock.domain.model.sameAs
import com.dsmg11.nexstock.domain.usecase.GetCurrentUserUseCase
import com.dsmg11.nexstock.domain.usecase.master.ObserveProductLinesUseCase
import com.dsmg11.nexstock.domain.usecase.users.ObserveUsersUseCase
import com.dsmg11.nexstock.domain.usecase.users.UpdateUserRoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UsersUiState(
    val users: List<UserProfile> = emptyList(),
    val isLoading: Boolean = true,
    val editing: UserProfile? = null,
    val selectedRole: UserRole = UserRole.ALMACENERO,
    val selectedLines: Set<ProductLine> = emptySet(),
    val availableLines: List<ProductLine> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class UsersViewModel @Inject constructor(
    getCurrentUser: GetCurrentUserUseCase,
    observeUsers: ObserveUsersUseCase,
    observeProductLines: ObserveProductLinesUseCase,
    private val updateUserRole: UpdateUserRoleUseCase
) : ViewModel() {

    private val currentUid = getCurrentUser()?.uid

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeUsers()
                .catch {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "No se pudo cargar la lista de usuarios")
                    }
                }
                .collect { users ->
                    _uiState.update { it.copy(users = users, isLoading = false) }
                }
        }
        viewModelScope.launch {
            observeProductLines(onlyActive = true)
                .catch { }
                .collect { lines -> _uiState.update { it.copy(availableLines = lines) } }
        }
    }

    fun startEditing(user: UserProfile) {
        _uiState.update { state ->
            state.copy(
                editing = user,
                selectedRole = user.role,
                // Se emparejan por código con las líneas disponibles para marcar bien los checkboxes
                selectedLines = user.assignedLines.map { assigned ->
                    state.availableLines.firstOrNull { it.sameAs(assigned) } ?: assigned
                }.toSet(),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(editing = null) }
    }

    fun onRoleSelected(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun onLineToggled(line: ProductLine) {
        _uiState.update { state ->
            val lines = if (line in state.selectedLines) state.selectedLines - line
            else state.selectedLines + line
            state.copy(selectedLines = lines)
        }
    }

    fun save() {
        val state = _uiState.value
        val user = state.editing ?: return
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            updateUserRole(currentUid, user.uid, state.selectedRole, state.selectedLines.toList())
                .onSuccess {
                    _uiState.update {
                        it.copy(isSaving = false, editing = null, successMessage = "Usuario actualizado")
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSaving = false, editing = null, errorMessage = e.message)
                    }
                }
        }
    }
}