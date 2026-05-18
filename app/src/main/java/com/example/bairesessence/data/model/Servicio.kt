package com.example.bairesessence.data.model

import com.google.firebase.Timestamp

data class Servicio(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val image: String = "",
    @get:com.google.firebase.firestore.Exclude
    @set:com.google.firebase.firestore.Exclude
    var precio: Double = 0.0,
    val categoria: String = "",
    val duracion: String = "",
    val idioma: String = "",
    val ubicacion: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val incluye: String = "",
    val activo: Boolean = true,
    val from: Timestamp? = null,
    val until: Timestamp? = null,
    val companyId: String = "",
    val rating: Double = 4.5,
    val timestamp: Timestamp? = null
) {
    val tieneMapa: Boolean get() = lat != 0.0 && lng != 0.0
    val tienePrecio: Boolean get() = precio > 0.0
    val precioFormateado: String get() =
        if (tienePrecio) "$${"%,.0f".format(precio).replace(",", ".")}" else "A consultar"
    val fechaDesde: String? get() = from?.toDate()?.let {
        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("es", "AR")).format(it)
    }
    val fechaHasta: String? get() = until?.toDate()?.let {
        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("es", "AR")).format(it)
    }
}
