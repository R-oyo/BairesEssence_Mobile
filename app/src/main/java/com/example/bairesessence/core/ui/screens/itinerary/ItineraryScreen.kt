package com.example.bairesessence.core.ui.screens.itinerary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class Actividad(val id: String = "", val time: String = "", val title: String = "", val description: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    val actividades = remember { mutableStateListOf<Actividad>() }
    var cargando by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var time by remember { mutableStateOf("") }
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Itinerario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BEDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = BEPrimary) {
                Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White)
            }
        },
        bottomBar = { BottomBar(navController) },
        containerColor = BEBackground
    ) { innerPadding ->
        if (cargando) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BEPrimary)
            }
        } else if (actividades.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📅", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Aún no tenés actividades", style = MaterialTheme.typography.titleMedium, color = BETextSecondary)
                    Text("Tocá + para agregar una", style = MaterialTheme.typography.bodyMedium, color = BETextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(actividades) { act ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Timeline
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
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (act.time.isNotBlank()) Text(act.time, style = MaterialTheme.typography.labelSmall, color = BEPrimary, fontWeight = FontWeight.Bold)
                                Text(act.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                if (act.description.isNotBlank()) Text(act.description, style = MaterialTheme.typography.bodySmall, color = BETextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Agregar actividad") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Hora (ej: 10:30)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (userId != null && titulo.isNotBlank()) {
                        db.collection("users").document(userId).collection("itinerary")
                            .add(mapOf("time" to time, "title" to titulo, "description" to descripcion))
                        time = ""; titulo = ""; descripcion = ""
                        showDialog = false
                    }
                }) { Text("Guardar", color = BEPrimary) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}
