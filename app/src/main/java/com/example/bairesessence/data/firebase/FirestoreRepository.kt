package com.example.bairesessence.data.firebase

import com.example.bairesessence.data.model.Servicio
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // ── Servicios ──────────────────────────────────────────
    suspend fun fetchServicios(): List<Servicio> =
        db.collection("servicios")
            .whereEqualTo("activo", true)
            .get().await()
            .documents.mapNotNull { it.toObject(Servicio::class.java)?.copy(id = it.id) }

    suspend fun fetchServiciosByCategoria(cat: String): List<Servicio> =
        db.collection("servicios")
            .whereEqualTo("activo", true)
            .whereEqualTo("categoria", cat)
            .get().await()
            .documents.mapNotNull { it.toObject(Servicio::class.java)?.copy(id = it.id) }

    suspend fun fetchServicioById(id: String): Servicio? {
        val doc = db.collection("servicios").document(id).get().await()
        return doc.toObject(Servicio::class.java)?.copy(id = doc.id)
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
        total: Double
    ): String? = try {
        val data = hashMapOf(
            "userId"    to userId,
            "email"     to userEmail,
            "fullname"  to fullname,
            "checkin"   to checkin,
            "checkout"  to checkout,
            "servicios" to servicios,
            "personas"  to personas,
            "total"     to total,
            "estado"    to "pendiente",
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db.collection("reservas").add(data).await().id
    } catch (e: Exception) { e.printStackTrace(); null }

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

    suspend fun fetchReservasByUser(userEmail: String): List<Map<String, Any>> =
        db.collection("reservas")
            .whereEqualTo("email", userEmail)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get().await()
            .documents.mapNotNull { doc ->
                val d = doc.data?.toMutableMap() ?: return@mapNotNull null
                if (!d.containsKey("estado")) d["estado"] = "pendiente"
                d["id"] = doc.id
                d
            }
}
