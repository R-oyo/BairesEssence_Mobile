package com.example.bairesessence.data.firebase

import com.example.bairesessence.data.model.Servicio
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // ── Servicios ──────────────────────────────────────────
    private fun com.google.firebase.firestore.DocumentSnapshot.parsePrice(): Double =
        when (val raw = get("price")) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    private fun com.google.firebase.firestore.DocumentSnapshot.toServicio(): Servicio? {
        val price = parsePrice()
        // Try normal deserialization first; fall back to manual mapping if types mismatch
        return try {
            toObject(Servicio::class.java)?.copy(id = id, precio = price)
        } catch (e: RuntimeException) {
            val d = data ?: return null
            Servicio(
                id = id,
                title       = d["title"]       as? String ?: "",
                description = d["description"] as? String ?: "",
                image       = d["image"]       as? String ?: "",
                precio      = price,
                categoria   = d["categoria"]   as? String ?: "",
                duracion    = d["duracion"]    as? String ?: "",
                idioma      = d["idioma"]      as? String ?: "",
                ubicacion   = d["ubicacion"]   as? String ?: "",
                lat         = (d["lat"]    as? Number)?.toDouble() ?: 0.0,
                lng         = (d["lng"]    as? Number)?.toDouble() ?: 0.0,
                incluye     = d["incluye"]     as? String ?: "",
                activo      = d["activo"]      as? Boolean ?: true,
                whatsapp    = d["whatsapp"]    as? String ?: "",
                rating      = (d["rating"] as? Number)?.toDouble() ?: 4.5
            )
        }
    }

    suspend fun fetchServicios(): List<Servicio> =
        db.collection("servicios")
            .whereEqualTo("activo", true)
            .get().await()
            .documents.mapNotNull { it.toServicio() }

    suspend fun fetchServicioById(id: String): Servicio? {
        val doc = db.collection("servicios").document(id).get().await()
        return doc.toServicio()
    }

    // ── Reservas ───────────────────────────────────────────
    /**
     * Crea reserva con estructura idéntica a la web:
     * servicios[] con id/title/image/price/personas/lat/lng
     * estado: "pendiente"
     */
    suspend fun crearReserva(
        userId: String,
        userEmail: String,
        fullname: String,
        checkin: String,
        checkout: String,
        servicios: List<Map<String, Any>>,
        personas: Int,
        total: Double,
        acompanantes: List<Map<String, Any>> = emptyList()
    ): String? = try {
        // "pasajeros" (mismo esquema que la web: titular + acompañantes,
        // cada uno con nombre/dni/fechaNacimiento) en vez de "acompanantes"
        // — ese campo nunca lo leía nadie, ni el móvil ni el seller web.
        val titular = mapOf("nombre" to fullname, "dni" to "", "fechaNacimiento" to "")
        val pasajeros = listOf(titular) + acompanantes.map {
            mapOf(
                "nombre" to (it["nombre"] ?: ""),
                "dni" to (it["dni"] ?: ""),
                "fechaNacimiento" to (it["fechaNacimiento"] ?: "")
            )
        }
        val data = hashMapOf(
            "userId"     to userId,
            "email"      to userEmail,
            "fullname"   to fullname,
            "checkin"    to checkin,
            "checkout"   to checkout,
            "servicios"  to servicios,
            "personas"   to personas,
            "total"      to total,
            "pasajeros"  to pasajeros,
            "estado"     to "pendiente",
            "timestamp"  to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db.collection("reservas").add(data).await().id
    } catch (e: Exception) { e.printStackTrace(); null }

    suspend fun cancelarReserva(reservaId: String) {
        db.collection("reservas").document(reservaId)
            .update(mapOf(
                "estado" to "cancelada",
                "motivoCancelacion" to "Cancelada por el usuario"
            )).await()
    }

    // Mismo contrato que la web (confirmarReservaPorUsuario en firestore.js):
    // el turista confirma su parte -> el seller recién ahí puede confirmar
    // la suya y habilitar el pago. Sin este paso la reserva queda trabada
    // en "pendiente" para siempre del lado del seller.
    suspend fun confirmarReservaPorUsuario(reservaId: String) {
        db.collection("reservas").document(reservaId)
            .update(mapOf(
                "confirmadoPorUsuario" to true,
                "estado" to "confirmada_usuario",
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )).await()
    }

    suspend fun actualizarFechasReserva(reservaId: String, checkin: String, checkout: String) {
        db.collection("reservas").document(reservaId)
            .update(mapOf("checkin" to checkin, "checkout" to checkout))
            .await()
    }

    // ── Favoritos ──────────────────────────────────────────
    // Colección: favoritos/{userId}/items/{servicioId} (misma estructura que la web)
    private fun favoritosRef(userId: String) =
        db.collection("favoritos").document(userId).collection("items")

    suspend fun fetchFavoritoIds(userId: String): Set<String> =
        favoritosRef(userId).get().await().documents.map { it.id }.toSet()

    suspend fun toggleFavorito(userId: String, servicioId: String): Boolean {
        val ref = favoritosRef(userId).document(servicioId)
        return if (ref.get().await().exists()) {
            ref.delete().await()
            false
        } else {
            val servicio = fetchServicioById(servicioId)
            ref.set(
                mapOf(
                    "id" to servicioId,
                    "title" to (servicio?.title ?: ""),
                    "image" to (servicio?.image ?: ""),
                    "price" to (servicio?.precio?.toString() ?: ""),
                    "categoria" to (servicio?.categoria ?: ""),
                    "ubicacion" to (servicio?.ubicacion ?: ""),
                    "duracion" to (servicio?.duracion ?: ""),
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
            true
        }
    }

    suspend fun fetchFavoritos(userId: String): List<Servicio> {
        val ids = fetchFavoritoIds(userId)
        if (ids.isEmpty()) return emptyList()
        return ids.mapNotNull { id ->
            runCatching { fetchServicioById(id) }.getOrNull()
        }
    }

    // ── Reseñas ────────────────────────────────────────────
    // Misma colección que la web ("resenas"): el móvil siempre crea
    // reseñas de tipo "experiencia" (no tiene UI para calificar empresa
    // o plataforma, a diferencia de la web).
    suspend fun addResena(
        userId: String,
        userEmail: String,
        userName: String,
        reservaId: String,
        servicioId: String,
        servicioTitle: String,
        estrellas: Int,
        texto: String
    ) {
        db.collection("resenas").add(
            mapOf(
                "tipo"             to "experiencia",
                "referenciaId"     to servicioId,
                "referenciaNombre" to servicioTitle,
                "reservaId"        to reservaId,
                "userId"           to userId,
                "userName"         to userName,
                "userEmail"        to userEmail,
                "estrellas"        to estrellas,
                "texto"            to texto,
                "timestamp"        to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }

    // Mismo esquema que la web (reservas/{id}/mensajes): autorId/autorNombre/autorRol.
    // El móvil solo representa turistas, así que autorRol siempre es "usuario".
    suspend fun sendMensaje(reservaId: String, autorId: String, autorNombre: String, texto: String) {
        db.collection("reservas").document(reservaId)
            .collection("mensajes")
            .add(mapOf(
                "texto"       to texto,
                "autorId"     to autorId,
                "autorNombre" to autorNombre,
                "autorRol"    to "usuario",
                "servicioId"  to null,
                "leido"       to false,
                "timestamp"   to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )).await()
    }

    suspend fun actualizarPasajeros(reservaId: String, servicios: List<Map<String, Any>>, nuevoTotal: Double) {
        db.collection("reservas").document(reservaId)
            .update(mapOf("servicios" to servicios, "total" to nuevoTotal)).await()
    }

    // Mismo contrato que updatePasajeros() de la web: reemplaza la lista
    // completa de pasajeros nombrados (titular en el índice 0 + acompañantes).
    suspend fun actualizarListaPasajeros(reservaId: String, pasajeros: List<Map<String, Any>>) {
        db.collection("reservas").document(reservaId).update("pasajeros", pasajeros).await()
    }

    // ── Familia ────────────────────────────────────────────
    // Mismo esquema que la web: array "grupofamiliar" en users/{userId}
    // (antes era una subcolección propia del móvil, invisible para la web).
    @Suppress("UNCHECKED_CAST")
    suspend fun fetchFamilia(userId: String): List<Map<String, Any>> {
        val snap = db.collection("users").document(userId).get().await()
        val lista = snap.get("grupofamiliar") as? List<*> ?: return emptyList()
        return lista.mapNotNull { it as? Map<String, Any> }
    }

    suspend fun agregarFamiliar(userId: String, data: Map<String, Any>): String? = try {
        val id = System.currentTimeMillis().toString()
        val actual = fetchFamilia(userId)
        val nuevo = actual + (data + ("id" to id))
        db.collection("users").document(userId).update("grupofamiliar", nuevo).await()
        id
    } catch (e: Exception) { null }

    suspend fun eliminarFamiliar(userId: String, familiarId: String) {
        val actual = fetchFamilia(userId)
        val nuevo = actual.filter { it["id"] != familiarId }
        db.collection("users").document(userId).update("grupofamiliar", nuevo).await()
    }

    // Filtra "oculto" del lado del cliente (igual que hace la web) para no
    // requerir un índice compuesto y para respetar la moderación del admin
    // sin importar desde qué plataforma se creó la reseña.
    suspend fun fetchReviewsByServicio(servicioId: String): List<Map<String, Any>> =
        db.collection("resenas")
            .whereEqualTo("tipo", "experiencia")
            .whereEqualTo("referenciaId", servicioId)
            .get().await()
            .documents.mapNotNull { doc ->
                val d = doc.data?.toMutableMap() ?: return@mapNotNull null
                if (d["oculto"] == true) return@mapNotNull null
                d["id"] = doc.id
                d
            }
            .sortedByDescending { (it["timestamp"] as? com.google.firebase.Timestamp)?.seconds }

    suspend fun hasUserReviewedReserva(userId: String, reservaId: String): Boolean =
        !db.collection("resenas")
            .whereEqualTo("tipo", "experiencia")
            .whereEqualTo("userId", userId)
            .whereEqualTo("reservaId", reservaId)
            .get().await().isEmpty

    suspend fun fetchReservasByUser(userId: String): List<Map<String, Any>> =
        db.collection("reservas")
            .whereEqualTo("userId", userId)
            .get().await()
            .documents.mapNotNull { doc ->
                val d = doc.data?.toMutableMap() ?: return@mapNotNull null
                if (!d.containsKey("estado")) d["estado"] = "pendiente"
                d["id"] = doc.id
                d
            }
            .sortedByDescending { (it["timestamp"] as? com.google.firebase.Timestamp)?.seconds }

    // ── Mapa ───────────────────────────────────────────────
    suspend fun fetchServiciosDeReserva(reservaId: String): List<Servicio> {
        val doc = db.collection("reservas").document(reservaId).get().await()
        val svcs = doc.data?.get("servicios") as? List<*> ?: return emptyList()
        return svcs.filterIsInstance<Map<*, *>>().mapNotNull { s ->
            val id  = s["id"]  as? String ?: return@mapNotNull null
            val lat = (s["lat"] as? Number)?.toDouble() ?: 0.0
            val lng = (s["lng"] as? Number)?.toDouble() ?: 0.0
            if (lat == 0.0 && lng == 0.0) return@mapNotNull null
            Servicio(
                id       = id,
                title    = s["title"] as? String ?: "",
                image    = s["image"] as? String ?: "",
                precio   = (s["price"] as? Number)?.toDouble() ?: 0.0,
                lat      = lat,
                lng      = lng
            )
        }
    }

    suspend fun fetchServiciosDeItinerario(userId: String): List<Servicio> {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        return fetchReservasByUser(userId)
            .filter { (it["checkin"] as? String ?: "") >= today }
            .filter { (it["estado"] as? String) !in listOf("cancelada") }
            .flatMap { r ->
                val svcs = r["servicios"] as? List<*> ?: return@flatMap emptyList()
                svcs.filterIsInstance<Map<*, *>>().mapNotNull { s ->
                    val id  = s["id"]  as? String ?: return@mapNotNull null
                    val lat = (s["lat"] as? Number)?.toDouble() ?: 0.0
                    val lng = (s["lng"] as? Number)?.toDouble() ?: 0.0
                    if (lat == 0.0 && lng == 0.0) return@mapNotNull null
                    Servicio(
                        id     = id,
                        title  = s["title"] as? String ?: "",
                        image  = s["image"] as? String ?: "",
                        precio = (s["price"] as? Number)?.toDouble() ?: 0.0,
                        lat    = lat,
                        lng    = lng
                    )
                }
            }
            .distinctBy { it.id }
    }

    // ── Paquetes ───────────────────────────────────────────
    suspend fun fetchPaquetes(): List<com.example.bairesessence.data.model.Paquete> =
        db.collection("paquetes")
            .whereEqualTo("activo", true)
            .get().await()
            .documents.mapNotNull { doc ->
                val d = doc.data ?: return@mapNotNull null
                com.example.bairesessence.data.model.Paquete(
                    id         = doc.id,
                    agencia    = d["agencia"]    as? String ?: "",
                    nombre     = d["nombre"]     as? String ?: "",
                    descripcion= d["descripcion"] as? String ?: "",
                    servicios  = (d["servicios"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    precioTotal= (d["precioTotal"] as? Number)?.toDouble() ?: 0.0,
                    descuento  = (d["descuento"]  as? Number)?.toDouble() ?: 0.0,
                    imagen     = d["imagen"]     as? String ?: "",
                    activo     = d["activo"]     as? Boolean ?: true
                )
            }

    suspend fun fetchPaqueteById(id: String): com.example.bairesessence.data.model.Paquete? {
        val doc = db.collection("paquetes").document(id).get().await()
        val d = doc.data ?: return null
        return com.example.bairesessence.data.model.Paquete(
            id         = doc.id,
            agencia    = d["agencia"]    as? String ?: "",
            nombre     = d["nombre"]     as? String ?: "",
            descripcion= d["descripcion"] as? String ?: "",
            servicios  = (d["servicios"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            precioTotal= (d["precioTotal"] as? Number)?.toDouble() ?: 0.0,
            descuento  = (d["descuento"]  as? Number)?.toDouble() ?: 0.0,
            imagen     = d["imagen"]     as? String ?: "",
            activo     = d["activo"]     as? Boolean ?: true
        )
    }

    suspend fun fetchServiciosByIds(ids: List<String>): List<Servicio> =
        ids.mapNotNull { id ->
            runCatching { fetchServicioById(id) }.getOrNull()
        }
}
