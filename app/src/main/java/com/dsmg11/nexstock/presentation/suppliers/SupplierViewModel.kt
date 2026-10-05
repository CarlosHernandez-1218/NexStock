package com.dsmg11.nexstock.presentation.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsmg11.nexstock.domain.model.Supplier
import com.dsmg11.nexstock.domain.usecase.supplier.ConsultarRucUseCase
import com.dsmg11.nexstock.domain.usecase.supplier.RegistrarProveedorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupplierViewModel @Inject constructor(
    private val consultarRucUseCase: ConsultarRucUseCase,
    private val registrarProveedorUseCase: RegistrarProveedorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierUiState())
    val uiState: StateFlow<SupplierUiState> = _uiState.asStateFlow()

    fun onRucChange(value: String) {
        val nuevoRuc = value
            .filter { it.isDigit() }
            .take(11)

        _uiState.update {
            it.copy(
                ruc = nuevoRuc,
                razonSocial = "",
                estado = "",
                condicion = "",
                direccion = "",
                rucValidado = false,
                mensajeUsuario = null
            )
        }
    }

    fun onContactoChange(value: String) {
        _uiState.update {
            it.copy(
                contacto = value,
                mensajeUsuario = null
            )
        }
    }

    fun onTelefonoChange(value: String) {
        val telefono = value
            .filter { it.isDigit() }
            .take(15)

        _uiState.update {
            it.copy(
                telefono = telefono,
                mensajeUsuario = null
            )
        }
    }

    fun onCorreoChange(value: String) {
        _uiState.update {
            it.copy(
                correo = value,
                mensajeUsuario = null
            )
        }
    }

    fun consultarRuc() {
        val ruc = _uiState.value.ruc

        if (_uiState.value.isConsultandoRuc) {
            return
        }

        _uiState.update {
            it.copy(
                isConsultandoRuc = true,
                mensajeUsuario = null
            )
        }

        viewModelScope.launch {
            consultarRucUseCase(ruc)
                .onSuccess { informacionRuc ->
                    _uiState.update {
                        it.copy(
                            razonSocial = informacionRuc.razonSocial,
                            estado = informacionRuc.estado,
                            condicion = informacionRuc.condicion,
                            direccion = informacionRuc.direccion,
                            rucValidado = true,
                            isConsultandoRuc = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            razonSocial = "",
                            estado = "",
                            condicion = "",
                            direccion = "",
                            rucValidado = false,
                            isConsultandoRuc = false,
                            mensajeUsuario = error.message
                                ?: "No se pudo consultar el RUC",
                            mensajeEsError = true
                        )
                    }
                }
        }
    }

    fun registrarProveedor() {
        val estadoActual = _uiState.value

        if (estadoActual.isRegistrando) {
            return
        }

        if (!estadoActual.rucValidado) {
            mostrarError(
                "Debe consultar y validar el RUC antes de registrar el proveedor"
            )
            return
        }

        val proveedor = Supplier(
            ruc = estadoActual.ruc,
            razonSocial = estadoActual.razonSocial,
            estadoSunat = estadoActual.estado,
            condicionSunat = estadoActual.condicion,
            direccion = estadoActual.direccion,
            contacto = estadoActual.contacto.trim(),
            telefono = estadoActual.telefono.trim(),
            correo = estadoActual.correo.trim()
        )

        _uiState.update {
            it.copy(
                isRegistrando = true,
                mensajeUsuario = null
            )
        }

        viewModelScope.launch {
            registrarProveedorUseCase(proveedor)
                .onSuccess {
                    _uiState.value = SupplierUiState(
                        mensajeUsuario = "Proveedor registrado correctamente",
                        mensajeEsError = false
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isRegistrando = false,
                            mensajeUsuario = error.message
                                ?: "No se pudo registrar el proveedor",
                            mensajeEsError = true
                        )
                    }
                }
        }
    }

    fun limpiarMensaje() {
        _uiState.update {
            it.copy(
                mensajeUsuario = null,
                mensajeEsError = false
            )
        }
    }

    private fun mostrarError(mensaje: String) {
        _uiState.update {
            it.copy(
                mensajeUsuario = mensaje,
                mensajeEsError = true
            )
        }
    }
}