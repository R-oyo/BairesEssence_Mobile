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
        val data = hashMapOf(
            "userId"       to userId,
            "email"        to userEmail,
            "fullname"     to fullname,
            "checkin"      to checkin,
            "checkout"     to checkout,
            "servicios"    to servicios,
            "personas"     to personas,
            "total"        to total,
            "acompanantes" to acompanantes,
            "estado"       to "pendiente",
            "timestamp"    to com.google.firebase.firestore.FieldValue.serverTimestamp()
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

    suspend fun actualizarFechasReserva(reservaId: String, checkin: String, checkout: String) {
        db.collection("reservas").document(reservaId)
            .update(mapOf("checkin" to checkin, "checkout" to checkout))
            .await()
    }

    // ── Favoritos ──────────────────────────────────────────
    suspend fun fetchFavoritoIds(userId: String): Set<String> =
        db.collection("favoritos")
            .whereEqualTo("userId", userId)
            .get().await()
            .documents.mapNotNull { it.getString("servicioId") }
            .toSet()

    suspend fun toggleFavorito(userId: String, servicioId: String): Boolean {
        val query = db.collection("favoritos")
            .whereEqualTo("userId", userId)
            .whereEqualTo("servicioId", servicioId)
            .get().await()
        return if (query.isEmpty) {
            db.collection("favoritos").add(
                mapOf("userId" to userId, "servicioId" to servicioId)
            ).await()
            true
        } else {
            query.documents.forEach { it.reference.delete().await() }
            false
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
    suspend fun addResena(
        userId: String,
        userEmail: String,
        reservaId: String,
        servicioId: String,
        rating: Int,
        comentario: String
    ) {
        db.collection("reviews").add(
            mapOf(
                "userId"     to userId,
                "email"      to userEmail,
                "reservaId"  to reservaId,
                "servicioId" to servicioId,
                "rating"     to rating,
                "comentario" to comentario,
                "timestamp"  to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun sendMensaje(reservaId: String, senderId: String, senderEmail: String, texto: String) {
        db.collection("reservas").document(reservaId)
            .collection("mensajes")
            .add(mapOf(
                "texto"       to texto,
                "senderId"    to senderId,
                "senderEmail" to senderEmail,
                "timestamp"   to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )).await()
    }

    suspend fun actualizarPasajeros(reservaId: String, servicios: List<Map<String, Any>>, nuevoTotal: Double) {
        db.collection("reservas").document(reservaId)
            .update(mapOf("servicios" to servicios, "total" to nuevoTotal)).await()
    }

    // ── Familia ────────────────────────────────────────────
    suspend fun fetchFamilia(userId: String): List<Map<String, Any>> =
        db.collection("users").document(userId).collection("familia")
            .get().await()
            .documents.mapNotNull { doc ->
                val d = doc.data?.toMutableMap() ?: return@mapNotNull null
                d["id"] = doc.id
                d
            }

    suspend fun agregarFamiliar(userId: String, data: Map<String, Any>): String? =
        try { db.collection("users").document(userId).collection("familia").add(data).await().id }
        catch (e: Exception) { null }

    suspend fun eliminarFamiliar(userId: String, familiarId: String) {
        db.collection("users").document(userId).collection("familia")
            .document(familiarId).delete().await()
    }

    suspend fun fetchReviewsByServicio(servicioId: String): List<Map<String, Any>> =
        db.collection("reviews")
            .whereEqualTo("servicioId", servicioId)
            .get().await()
            .documents.mapNotNull { doc ->
                val d = doc.data?.toMutableMap() ?: return@mapNotNull null
                d["id"] = doc.id
                d
            }
            .sortedByDescending { (it["timestamp"] as? com.google.firebase.Timestamp)?.seconds }

    suspend fun hasUserReviewedReserva(userId: String, reservaId: String): Boolean =
        !db.collection("reviews")
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
}
