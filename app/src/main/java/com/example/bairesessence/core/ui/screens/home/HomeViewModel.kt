package com.example.bairesessence.core.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.example.bairesessence.data.model.Servicio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _servicios = MutableStateFlow<List<Servicio>>(emptyList())
    val servicios: StateFlow<List<Servicio>> = _servicios.asStateFlow()

    private val _favoritoIds = MutableStateFlow<Set<String>>(emptySet())
    val favoritoIds: StateFlow<Set<String>> = _favoritoIds.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _fetchError = MutableStateFlow(false)
    val fetchError: StateFlow<Boolean> = _fetchError.asStateFlow()

    fun cargar(userId: String?) {
        viewModelScope.launch {
            _cargando.value = true
            _fetchError.value = false
            try {
                _servicios.value = FirestoreRepository.fetchServicios()
            } catch (e: Exception) {
                _fetchError.value = true
            }
            if (userId != null) {
                try {
                    _favoritoIds.value = FirestoreRepository.fetchFavoritoIds(userId)
                } catch (_: Exception) {}
            }
            _cargando.value = false
        }
    }

    fun toggleFavorito(userId: String, servicioId: String) {
        viewModelScope.launch {
            val esAhora = runCatching {
                FirestoreRepository.toggleFavorito(userId, servicioId)
            }.getOrDefault(!_favoritoIds.value.contains(servicioId))
            _favoritoIds.update { if (esAhora) it + servicioId else it - servicioId }
        }
    }
}
