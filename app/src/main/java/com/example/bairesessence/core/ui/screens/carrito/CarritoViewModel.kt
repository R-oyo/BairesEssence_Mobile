package com.example.bairesessence.core.ui.screens.carrito

import androidx.lifecycle.ViewModel
import com.example.bairesessence.data.model.ItemCarrito
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CarritoState(
    val items: List<ItemCarrito> = emptyList(),
    val checkin: String = "",
    val checkout: String = ""
)

class CarritoViewModel : ViewModel() {

    private val _state = MutableStateFlow(CarritoState())
    val state: StateFlow<CarritoState> = _state.asStateFlow()

    fun setFechas(checkin: String, checkout: String) {
        _state.update { it.copy(checkin = checkin, checkout = checkout) }
    }

    fun agregarItem(item: ItemCarrito) {
        _state.update { s ->
            if (s.items.any { it.servicioId == item.servicioId }) s
            else s.copy(items = s.items + item)
        }
    }

    fun quitarItem(servicioId: String) {
        _state.update { it.copy(items = it.items.filter { i -> i.servicioId != servicioId }) }
    }

    fun actualizarPersonas(servicioId: String, personas: Int) {
        _state.update { s ->
            s.copy(items = s.items.map { i ->
                if (i.servicioId == servicioId) i.copy(personas = personas.coerceAtLeast(1)) else i
            })
        }
    }

    fun estaEnCarrito(servicioId: String) = _state.value.items.any { it.servicioId == servicioId }

    fun limpiar() { _state.value = CarritoState() }

    val total: Double get() = _state.value.items.sumOf { it.subtotal }
}
