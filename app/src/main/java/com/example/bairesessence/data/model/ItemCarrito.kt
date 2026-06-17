package com.example.bairesessence.data.model

data class ItemCarrito(
    val servicioId: String,
    val title: String,
    val image: String,
    val precio: Double,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val ubicacion: String = "",
    val personas: Int = 1
) {
    val subtotal: Double get() = precio * personas
    val precioFormateado: String get() = "$${"%,.0f".format(precio).replace(",", ".")}"
    val subtotalFormateado: String get() = "$${"%,.0f".format(subtotal).replace(",", ".")}"
}

