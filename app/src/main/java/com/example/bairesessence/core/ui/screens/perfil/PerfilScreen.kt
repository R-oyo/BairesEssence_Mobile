package com.example.bairesessence.core.ui.screens.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bairesessence.core.navigation.Screen
import com.example.bairesessence.data.firebase.FirestoreRepository
import kotlinx.coroutines.launch
import com.example.bairesessence.core.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    val userId = user?.uid

    var fullName by remember { mutableStateOf(user?.displayName ?: "") }
    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var guardado by remember { mutableStateOf(false) }

    // Stats
    var totalReservas by remember { mutableStateOf(0) }
    var confirmadas by remember { mutableStateOf(0) }
    var pendientes by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    var familia by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var cargandoFamilia by remember { mutableStateOf(true) }
    var mostrarAgregarFamiliar by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId != null) {
            familia = runCatching { FirestoreRepository.fetchFamilia(userId) }.getOrDefault(emptyList())
        }
        cargandoFamilia = false
    }

    LaunchedEffect(userId) {
        if (userId != null) {
            // Load profile
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    fullName = doc.getString("fullName") ?: doc.getString("fullname") ?: user?.displayName ?: ""
                    cargando = false
                }
                .addOnFailureListener { cargando = false }

            // Load stats
            db.collection("reservas")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { snap ->
                    totalReservas = snap.size()
                    confirmadas = snap.documents.count { (it.getString("estado") ?: "") in listOf("confirmada", "pagada", "finalizada") }
                    pendientes = snap.documents.count { it.getString("estado") == "pendiente" }
                }
        } else { cargando = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.MisReservas.route) }) {
                        Icon(Icons.Default.ListAlt, contentDescription = "Mis Reservas", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BEDark)
            )
        },
        containerColor = BEBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(88.dp).clip(CircleShape).background(BEPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (user?.displayName?.firstOrNull() ?: user?.email?.firstOrNull() ?: 'U')
                            .uppercaseChar().toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                }
            }

            // Stats row (TripAdvisor profile style)
            if (totalReservas > 0 || !cargando) {
                Card(shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BESurface),
                    elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(value = totalReservas.toString(), label = "Reservas")
                        VerticalDivider(modifier = Modifier.height(40.dp), color = BEBorder)
                        StatItem(value = confirmadas.toString(), label = "Confirmadas")
                        VerticalDivider(modifier = Modifier.height(40.dp), color = BEBorder)
                        StatItem(value = pendientes.toString(), label = "Pendientes")
                    }
                }
            }

            // Email (read-only)
            OutlinedTextField(
                value = user?.email ?: "",
                onValueChange = {},
                readOnly = true, enabled = false,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = BEBorder,
                    disabledTextColor = BETextSecond,
                    disabledLabelColor = BETextMuted
                )
            )

            // Name (editable)
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; guardado = false },
                label = { Text("Nombre completo") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder
                )
            )

            if (guardado) {
                Surface(shape = RoundedCornerShape(8.dp), color = BEPrimary.copy(0.1f)) {
                    Text("✅ Perfil actualizado correctamente",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        color = BEPrimaryDark, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Button(
                onClick = {
                    if (userId != null && fullName.isNotBlank()) {
                        guardando = true
                        db.collection("users").document(userId)
                            .set(mapOf("fullName" to fullName.trim(), "fullname" to fullName.trim(),
                                "email" to (user?.email ?: "")), SetOptions.merge())
                            .addOnSuccessListener { guardando = false; guardado = true }
                            .addOnFailureListener { guardando = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !guardando && !cargando && fullName.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
            ) {
                if (guardando) CircularProgressIndicator(color = Color.White,
                    modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Guardar cambios", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = { navController.navigate(Screen.MisReservas.route) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BEPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, BEPrimary.copy(0.5f))
            ) { Text("📋 Ver mis reservas", fontWeight = FontWeight.SemiBold) }

            HorizontalDivider(color = BEBorder)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Grupo Familiar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { mostrarAgregarFamiliar = true }) { Text("+ Agregar", color = BEPrimary) }
            }
            if (cargandoFamilia) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BEPrimary, strokeWidth = 2.dp)
            } else if (familia.isEmpty()) {
                Text("Todavía no agregaste familiares.", style = MaterialTheme.typography.bodyMedium, color = BETextMuted)
            } else {
                familia.forEach { f ->
                    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = BESurfaceVar),
                        elevation = CardDefaults.cardElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(f["nombre"] as? String ?: "", fontWeight = FontWeight.SemiBold,
                                    color = BETextPrimary, style = MaterialTheme.typography.bodyMedium)
                                val rel = f["relacion"] as? String
                                if (!rel.isNullOrBlank()) Text(rel, style = MaterialTheme.typography.bodySmall, color = BETextSecond)
                                val dni = f["dni"] as? String
                                if (!dni.isNullOrBlank()) Text("DNI: $dni", style = MaterialTheme.typography.bodySmall, color = BETextMuted)
                            }
                            IconButton(onClick = {
                                val fId = f["id"] as? String ?: return@IconButton
                                scope.launch {
                                    runCatching { FirestoreRepository.eliminarFamiliar(userId!!, fId) }
                                        .onSuccess { familia = familia.filter { it["id"] != fId } }
                                }
                            }) { Icon(Icons.Default.Delete, null, tint = BEError, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }

            HorizontalDivider(color = BEBorder)

            OutlinedButton(
                onClick = {
                    auth.signOut()
                    navController.navigate(Screen.Landing.route) { popUpTo(0) { inclusive = true } }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BEError),
                border = androidx.compose.foundation.BorderStroke(1.dp, BEError.copy(0.4f))
            ) { Text("Cerrar sesión", fontWeight = FontWeight.SemiBold) }
        }
    }

    if (mostrarAgregarFamiliar) {
        AgregarFamiliarDialog(
            onDismiss = { mostrarAgregarFamiliar = false },
            onConfirm = { data ->
                scope.launch {
                    val id = FirestoreRepository.agregarFamiliar(userId!!, data)
                    if (id != null) familia = familia + (data.toMutableMap().apply { put("id", id) })
                    mostrarAgregarFamiliar = false
                }
            }
        )
    }
}


@Composable
private fun AgregarFamiliarDialog(onDismiss: () -> Unit, onConfirm: (Map<String, Any>) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf("") }
    var relacion by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = BESurface,
        title = { Text("Agregar familiar", fontWeight = FontWeight.Bold, color = BETextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error?.let { Text(it, color = BEError, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(value = nombre, onValueChange = { nombre = it; error = null },
                    label = { Text("Nombre completo") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder))
                OutlinedTextField(value = dni, onValueChange = { dni = it },
                    label = { Text("DNI / Pasaporte") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder))
                OutlinedTextField(value = fechaNacimiento, onValueChange = { fechaNacimiento = it },
                    label = { Text("Fecha nacimiento (dd/MM/yyyy)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder))
                OutlinedTextField(value = relacion, onValueChange = { relacion = it },
                    label = { Text("Relación (Cónyuge, Hijo/a…)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder))
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nombre.isBlank()) { error = "El nombre es obligatorio."; return@Button }
                onConfirm(mapOf("nombre" to nombre.trim(), "dni" to dni.trim(), "fechaNacimiento" to fechaNacimiento.trim(), "relacion" to relacion.trim()))
            }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)) {
                Text("Agregar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = BETextSecond) } }
    )
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = BEPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BETextMuted)
    }
}
