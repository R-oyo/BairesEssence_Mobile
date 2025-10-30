package com.example.bairesessence.data.firebase

import com.example.bairesessence.data.model.Servicio
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun fetchServicios(): List<Servicio> {
        return try {
            val snapshot = db.collection("servicios").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Servicio::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
