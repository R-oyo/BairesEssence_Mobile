package com.example.bairesessence.data.model

data class Paquete(
    val id: String = "",
    val agencia: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val servicios: List<String> = emptyList(),
    val precioTotal: Double = 0.0,
    val descuento: Double = 0.0,
    val imagen: String = "",
    val activo: Boolean = true
) {
    val precioFormateado: String get() = "$${"%,.0f".format(precioTotal).replace(",", ".")}"
    val descuentoFormateado: String get() = if (descuento > 0) "-${descuento.toInt()}%" else ""
}
