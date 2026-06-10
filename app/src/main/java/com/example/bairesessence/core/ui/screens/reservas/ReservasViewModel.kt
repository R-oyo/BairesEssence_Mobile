package com.example.bairesessence.core.ui.screens.reservas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bairesessence.data.firebase.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReservasViewModel : ViewModel() {

    private val _reservas = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val reservas: StateFlow<List<Map<String, Any>>> = _reservas.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _fetchError = MutableStateFlow(false)
    val fetchError: StateFlow<Boolean> = _fetchError.asStateFlow()

    private val _userRole = MutableStateFlow("turista")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _mutacionError = MutableStateFlow<String?>(null)
    val mutacionError: StateFlow<String?> = _mutacionError.asStateFlow()

    fun clearMutacionError() { _mutacionError.value = null }

    private var currentUserId: String? = null

    fun cargar(uid: String) {
        currentUserId = uid
        viewModelScope.launch {
            _cargando.value = true
            _fetchError.value = false
            try {
                val role = FirestoreRepository.fetchUserRole(uid)
                _userRole.value = role
                _reservas.value = if (role in listOf("admin", "seller")) {
                    FirestoreRepository.fetchAllReservas()
                } else {
                    FirestoreRepository.fetchReservasByUser(uid)
                }
            } catch (e: Exception) {
                _fetchError.value = true
            }
            _cargando.value = false
        }
    }

    fun actualizarPasajeros(reservaId: String, serviciosActualizados: List<Map<String, Any>>, nuevoTotal: Double) {
        viewModelScope.launch {
            runCatching {
                FirestoreRepository.actualizarPasajeros(reservaId, serviciosActualizados, nuevoTotal)
            }.onSuccess {
                _reservas.update { lista ->
                    lista.map { r ->
                        if (r["id"] == reservaId) {
                            r.toMutableMap().apply {
                                put("servicios", serviciosActualizados)
                                put("total", nuevoTotal)
                            }
                        } else r
                    }
                }
            }.onFailure { _mutacionError.value = "No se pudieron actualizar los pasajeros. Intentá de nuevo." }
        }
    }

    fun cancelarReserva(reservaId: String) {
        viewModelScope.launch {
            runCatching { FirestoreRepository.cancelarReserva(reservaId) }
                .onSuccess {
                    _reservas.update { lista ->
                        lista.map { r ->
                            if (r["id"] == reservaId) r.toMutableMap().apply { put("estado", "cancelada") } else r
                        }
                    }
                }
                .onFailure { _mutacionError.value = "No se pudo cancelar la reserva. Intentá de nuevo." }
        }
    }

    fun actualizarFechas(reservaId: String, checkin: String, checkout: String) {
        viewModelScope.launch {
            runCatching { FirestoreRepository.actualizarFechasReserva(reservaId, checkin, checkout) }
                .onSuccess {
                    _reservas.update { lista ->
                        lista.map { r ->
                            if (r["id"] == reservaId) r.toMutableMap().apply {
                                put("checkin", checkin); put("checkout", checkout)
                            } else r
                        }
                    }
                }
                .onFailure { _mutacionError.value = "No se pudieron actualizar las fechas. Intentá de nuevo." }
        }
    }
}
