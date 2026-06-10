package com.example.bairesessence.core.ui.screens.itinerary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bairesessence.core.ui.components.BottomBar
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Actividad(val id: String = "", val time: String = "", val title: String = "", val description: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    val userId = user?.uid
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val scope = rememberCoroutineScope()

    val actividades = remember { mutableStateListOf<Actividad>() }
    var reservas by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var cargando by remember { mutableStateOf(true) }
    var mostrarAgregarActividad by remember { mutableStateOf(false) }

    DisposableEffect(userId) {
        var reg: ListenerRegistration? = null
        if (userId != null) {
            reg = db.collection("users").document(userId).collection("itinerary")
                .addSnapshotListener { snap, _ ->
                    val list = snap?.documents?.mapNotNull { doc ->
                        Actividad(
                            id = doc.id,
                            time = doc.getString("time") ?: "",
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: ""
                        )
                    }?.sortedBy { it.time } ?: emptyList()
                    actividades.clear()
                    actividades.addAll(list)
                    cargando = false
                }
        } else { cargando = false }
        onDispose { reg?.remove() }
    }

    LaunchedEffect(user?.email) {
        if (user?.email != null) {
            runCatching {
                reservas = FirestoreRepository.fetchReservasByUser(user.uid)
                    .filter { (it["checkin"] as? String ?: "") >= today }
                    .filter { (it["estado"] as? String) !in listOf("cancelada") }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Itinerario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BEDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarAgregarActividad = true },
                containerColor = BEPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar actividad", tint = Color.White)
            }
        },
        bottomBar = { BottomBar(navController) },
        containerColor = BEBackground
    ) { innerPadding ->
        if (cargando) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BEPrimary)
            }
        } else if (actividades.isEmpty() && reservas.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📅", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Aún no tenés actividades", style = MaterialTheme.typography.titleMedium, color = BETextSecondary)
                    Text("Tocá + para agregar una actividad", style = MaterialTheme.typography.bodyMedium, color = BETextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Próximas reservas
                if (reservas.isNotEmpty()) {
                    item {
                        Text("Próximas reservas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BETextPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(reservas) { r ->
                        val checkin = r["checkin"] as? String ?: ""
                        val checkout = r["checkout"] as? String ?: ""
                        val estado = r["estado"] as? String ?: "pendiente"
                        val svcs = (r["servicios"] as? List<*>)?.filterIsInstance<Map<*, *>>()
                        val estadoColor = when (estado) {
                            "confirmada" -> BEPrimary
                            "pagada"     -> Color(0xFF6366F1)
                            else         -> BEWarning
                        }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BESurface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Box(Modifier.fillMaxWidth().height(3.dp).background(estadoColor))
                                Column(Modifier.padding(12.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("$checkin → $checkout",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BETextPrimary)
                                        Surface(shape = RoundedCornerShape(99.dp), color = estadoColor.copy(0.13f)) {
                                            Text(estado,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = estadoColor, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (!svcs.isNullOrEmpty()) {
                                        Spacer(Modifier.height(6.dp))
                                        svcs.forEach { s ->
                                            Text("• ${s["title"] ?: "Experiencia"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = BETextSecond)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                // ── Actividades manuales
                if (actividades.isNotEmpty()) {
                    item {
                        Text("Mis actividades",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BETextPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(actividades, key = { it.id }) { act ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(BEPrimary))
                                Box(Modifier.width(2.dp).height(64.dp).background(BEBorder))
                            }
                            Spacer(Modifier.width(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = BESurface),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (act.time.isNotBlank()) Text(act.time, style = MaterialTheme.typography.labelSmall, color = BEPrimary, fontWeight = FontWeight.Bold)
                                        Text(act.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        if (act.description.isNotBlank()) Text(act.description, style = MaterialTheme.typography.bodySmall, color = BETextSecondary)
                                    }
                                    IconButton(
                                        onClick = {
                                            if (userId != null) {
                                                scope.launch {
                                                    runCatching {
                                                        db.collection("users").document(userId)
                                                            .collection("itinerary").document(act.id).delete()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = BETextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarAgregarActividad && userId != null) {
        AgregarActividadDialog(
            onDismiss = { mostrarAgregarActividad = false },
            onConfirm = { time, title, description ->
                scope.launch {
                    runCatching {
                        db.collection("users").document(userId).collection("itinerary")
                            .add(mapOf("time" to time, "title" to title, "description" to description))
                    }
                    mostrarAgregarActividad = false
                }
            }
        )
    }
}

@Composable
private fun AgregarActividadDialog(
    onDismiss: () -> Unit,
    onConfirm: (time: String, title: String, description: String) -> Unit
) {
    var time by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BESurface,
        title = { Text("Nueva actividad", fontWeight = FontWeight.Bold, color = BETextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error?.let { Text(it, color = BEError, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("Título *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder)
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Horario (ej: 10:00 hs)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) { error = "El título es obligatorio."; return@Button }
                    onConfirm(time.trim(), title.trim(), description.trim())
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
            ) { Text("Agregar", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = BETextSecond) }
        }
    )
}
