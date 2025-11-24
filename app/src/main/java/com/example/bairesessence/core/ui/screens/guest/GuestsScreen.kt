package com.example.bairesessence.core.ui.screens.guests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.bairesessence.core.ui.navigation.Screen
import com.example.bairesessence.core.ui.screens.home.AppScaffold
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Guest(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val notes: String = ""
)

@Composable
fun GuestsScreen(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    val guests = remember { mutableStateListOf<Guest>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // diálogo agregar / editar
    var showDialog by remember { mutableStateOf(false) }
    var editingGuest by remember { mutableStateOf<Guest?>(null) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    fun resetForm() {
        name = ""
        email = ""
        notes = ""
        editingGuest = null
    }

    fun startCreate() {
        resetForm()
        showDialog = true
    }

    fun startEdit(guest: Guest) {
        editingGuest = guest
        name = guest.name
        email = guest.email
        notes = guest.notes
        showDialog = true
    }

    fun saveGuest() {
        if (userId == null) return
        val ref = firestore
            .collection("users")
            .document(userId)
            .collection("guests")

        val currentId = editingGuest?.id
        val guest = Guest(
            id = currentId ?: "",
            name = name.trim(),
            email = email.trim(),
            notes = notes.trim()
        )

        if (currentId.isNullOrBlank()) {
            ref.add(guest)
        } else {
            ref.document(currentId).set(guest)
        }
    }

    fun deleteGuest(guest: Guest) {
        if (userId == null || guest.id.isBlank()) return
        firestore
            .collection("users")
            .document(userId)
            .collection("guests")
            .document(guest.id)
            .delete()
    }

    // Escucha en tiempo real
    DisposableEffect(userId) {
        if (userId == null) {
            isLoading = false
            errorMessage = "Iniciá sesión para gestionar tus invitados."
            onDispose { }
        } else {
            val ref = firestore
                .collection("users")
                .document(userId)
                .collection("guests")

            val registration = ref.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage = "Error al cargar invitados."
                    isLoading = false
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents ?: emptyList()
                val newList = docs.mapNotNull { doc ->
                    doc.toObject(Guest::class.java)?.copy(id = doc.id)
                }

                guests.clear()
                guests.addAll(newList)
                isLoading = false
                errorMessage = null
            }

            onDispose { registration.remove() }
        }
    }

    AppScaffold(
        navController = navController,
        currentRoute = Screen.Guests.route,
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = { startCreate() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar invitado")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Invitados al viaje",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Agregá acompañantes para tener registrado quién viaja con vos. " +
                            "Por ahora es solo informativo para Baires Essence.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                } else if (isLoading) {
                    Text(
                        text = "Cargando invitados...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } else if (guests.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Todavía no agregaste invitados. Usá el botón + para sumar acompañantes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        guests.forEach { guest ->
                            GuestItem(
                                guest = guest,
                                onEdit = { startEdit(guest) },
                                onDelete = { deleteGuest(guest) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                resetForm()
            },
            title = {
                Text(
                    text = if (editingGuest == null)
                        "Agregar invitado"
                    else
                        "Editar invitado"
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveGuest()
                        showDialog = false
                        resetForm()
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        resetForm()
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun GuestItem(
    guest: Guest,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onEdit() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = guest.name,
                style = MaterialTheme.typography.titleMedium
            )

            if (guest.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = guest.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }

            if (guest.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = guest.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Text("Editar")
                }
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = Color.Red)
                }
            }
        }
    }
}
