package com.example.bairesessence.core.ui.screens.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.bairesessence.core.ui.navigation.Screen
import com.example.bairesessence.core.ui.screens.home.AppScaffold
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun ProfileScreen(navController: NavHostController) {

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var preferredLanguage by remember { mutableStateOf("es") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ===============================
    // CARGA DEL PERFIL DESDE FIRESTORE
    // ===============================
    LaunchedEffect(userId) {
        if (userId == null) {
            isLoading = false
            errorMessage = "Iniciá sesión para ver tu perfil."
        } else {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    fullName = doc.getString("fullName").orEmpty()
                    email = doc.getString("email") ?: auth.currentUser?.email.orEmpty()
                    preferredLanguage = doc.getString("preferredLanguage") ?: "es"

                    isLoading = false
                }
                .addOnFailureListener {
                    errorMessage = "Error al cargar el perfil."
                    isLoading = false
                }
        }
    }

    // ===============================
    // GUARDAR PERFIL
    // ===============================
    fun saveProfile() {
        if (userId == null) {
            errorMessage = "Iniciá sesión para editar tu perfil."
            return
        }
        if (fullName.isBlank()) {
            errorMessage = "El nombre no puede estar vacío."
            return
        }

        isSaving = true
        saveSuccess = false
        errorMessage = null

        val data = mapOf(
            "fullName" to fullName,
            "email" to email.ifBlank { auth.currentUser?.email.orEmpty() },
            "preferredLanguage" to preferredLanguage
        )

        firestore.collection("users").document(userId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                isSaving = false
                saveSuccess = true
            }
            .addOnFailureListener {
                isSaving = false
                errorMessage = "Error al guardar el perfil."
            }
    }

    // ===============================
    // UI
    // ===============================
    AppScaffold(
        navController = navController,
        currentRoute = Screen.Profile.route
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(innerPadding)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Text(
                    text = "Tu perfil",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Actualizá tus datos para personalizar tu experiencia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ============= Mensajes ==============
                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (isLoading) {
                    Text("Cargando...", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ============= Campos ==============
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        saveSuccess = false
                    },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    label = { Text("Email") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Idioma preferido",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            preferredLanguage = "es"
                            saveSuccess = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (preferredLanguage == "es") Color(0xFF9CFF3C) else Color.LightGray
                        )
                    ) { Text("Español") }

                    Button(
                        onClick = {
                            preferredLanguage = "en"
                            saveSuccess = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (preferredLanguage == "en") Color(0xFF9CFF3C) else Color.LightGray
                        )
                    ) { Text("English") }
                }

                if (saveSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Perfil actualizado correctamente.", color = Color(0xFF2E7D32))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { if (!isSaving) saveProfile() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9CFF3C),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = if (isSaving) "Guardando..." else "Guardar cambios",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
