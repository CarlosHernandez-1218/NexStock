package com.dsmg11.nexstock.domain.usecase.auth

import javax.inject.Inject

data class RegisterValidation(
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
) {
    val isValid: Boolean
        get() = emailError == null && passwordError == null && confirmPasswordError == null
}

class ValidateRegisterUseCase @Inject constructor() {

    operator fun invoke(
        email: String,
        password: String,
        confirmPassword: String
    ): RegisterValidation {
        val emailError = when {
            email.isBlank() -> "El correo es obligatorio"
            !EMAIL_REGEX.matches(email.trim()) -> "Ingresa un correo válido"
            else -> null
        }
        val passwordError = when {
            password.isBlank() -> "La contraseña es obligatoria"
            password.length < MIN_PASSWORD_LENGTH -> "Debe tener al menos $MIN_PASSWORD_LENGTH caracteres"
            else -> null
        }
        val confirmPasswordError = when {
            confirmPassword.isBlank() -> "Confirma tu contraseña"
            confirmPassword != password -> "Las contraseñas no coinciden"
            else -> null
        }
        return RegisterValidation(emailError, passwordError, confirmPasswordError)
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}