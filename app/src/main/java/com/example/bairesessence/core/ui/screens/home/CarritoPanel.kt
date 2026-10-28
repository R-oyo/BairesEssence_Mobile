package com.example.bairesessence.core.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bairesessence.core.navigation.Screen
import com.example.bairesessence.core.ui.screens.carrito.CarritoViewModel
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun CarritoPanel(carritoVm: CarritoViewModel, navController: NavController, onClose: () -> Unit) {
    val state by carritoVm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val user = FirebaseAuth.getInstance().currentUser

    var paso by remember { mutableStateOf(0) }
    var viajaSolo by remember { mutableStateOf(true) }
    var compNombres by remember { mutableStateOf(listOf("")) }
    var compDnis    by remember { mutableStateOf(listOf("")) }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = BESurface,
        title = {
            if (paso == 0) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Mi carrito", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = BETextPrimary)
                    if (state.items.isNotEmpty())
                        TextButton(onClick = { carritoVm.limpiar() }) { Text("Vaciar", color = BEError) }
                }
            } else {
                Text("Grupo de viaje", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = BETextPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (paso == 0) {
                    if (state.items.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Luggage, contentDescription = null, tint = BETextMuted, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Tu carrito está vacío", color = BETextSecond,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        if (state.checkin.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(8.dp), color = BEPrimary.copy(0.08f)) {
                                Text("${state.checkin} → ${state.checkout}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall, color = BEPrimaryDark,
                                    fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        state.items.forEach { item ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = BESurfaceVar)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.title, style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold, maxLines = 1, color = BETextPrimary)
                                        if (item.precio > 0) {
                                            Text("${item.precioFormateado} × ${item.personas} = ${item.subtotalFormateado}",
                                                style = MaterialTheme.typography.bodySmall, color = BEPrimary)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { carritoVm.actualizarPersonas(item.servicioId, item.personas - 1) },
                                            modifier = Modifier.size(28.dp)) {
                                            Text("−", color = BEPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Text("${item.personas}", style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                        IconButton(onClick = { carritoVm.actualizarPersonas(item.servicioId, item.personas + 1) },
                                            modifier = Modifier.size(28.dp)) {
                                            Text("+", color = BEPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(onClick = { carritoVm.quitarItem(item.servicioId) },
                                            modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Quitar del carrito", tint = BEError, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = BEBorder)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total estimado", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = BETextPrimary)
                            Text("${"$"}${"%,.0f".format(carritoVm.total).replace(",", ".")}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = BEPrimary)
                        }
                        error?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, color = BEError, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    // ── Paso 1: grupo de viaje
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()) {
                            RadioButton(selected = viajaSolo, onClick = { viajaSolo = true },
                                colors = RadioButtonDefaults.colors(selectedColor = BEPrimary))
                            Text("Viajo solo/a", style = MaterialTheme.typography.bodyMedium, color = BETextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()) {
                            RadioButton(selected = !viajaSolo, onClick = { viajaSolo = false },
                                colors = RadioButtonDefaults.colors(selectedColor = BEPrimary))
                            Text("Viajo con acompañantes", style = MaterialTheme.typography.bodyMedium, color = BETextPrimary)
                        }
                        if (!viajaSolo) {
                            HorizontalDivider(color = BEBorder)
                            compNombres.forEachIndexed { i, nombre ->
                                Text("Acompañante ${i + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BETextMuted, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = nombre,
                                    onValueChange = { v ->
                                        compNombres = compNombres.toMutableList().also { it[i] = v }
                                    },
                                    label = { Text("Nombre completo") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder)
                                )
                                OutlinedTextField(
                                    value = compDnis.getOrElse(i) { "" },
                                    onValueChange = { v ->
                                        compDnis = compDnis.toMutableList().also { it[i] = v }
                                    },
                                    label = { Text("DNI / Pasaporte") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder)
                                )
                                if (compNombres.size > 1) {
                                    TextButton(
                                        onClick = {
                                            compNombres = compNombres.toMutableList().also { it.removeAt(i) }
                                            compDnis    = compDnis.toMutableList().also { it.removeAt(i) }
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = BEError)
                                    ) { Text("Quitar", style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                            TextButton(
                                onClick = { compNombres = compNombres + ""; compDnis = compDnis + "" },
                                colors = ButtonDefaults.textButtonColors(contentColor = BEPrimary)
                            ) { Text("+ Agregar otro acompañante") }
                        }
                        error?.let {
                            Text(it, color = BEError, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (paso == 0) {
                if (state.items.isNotEmpty()) {
                    Button(
                        onClick = {
                            when {
                                user == null -> error = "Iniciá sesión para continuar."
                                state.checkin.isBlank() -> error = "Agregá las fechas de viaje antes de continuar."
                                else -> { error = null; paso = 1 }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                    ) { Text("Continuar", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            } else {
                Button(
                    onClick = {
                        guardando = true; error = null
                        val acompMap = if (viajaSolo) emptyList() else
                            compNombres.mapIndexed { i, nombre ->
                                mapOf("nombre" to nombre.trim(), "dni" to compDnis.getOrElse(i) { "" }.trim())
                            }.filter { (it["nombre"] as? String).orEmpty().isNotBlank() }
                        scope.launch {
                            val yaReservados = FirestoreRepository.checkServiciosYaReservados(
                                user!!.uid, state.items.map { it.servicioId }
                            )
                            if (yaReservados.isNotEmpty()) {
                                val nombres = state.items.filter { it.servicioId in yaReservados }
                                    .joinToString(", ") { it.title }
                                error = "Ya tenés una reserva activa para: $nombres. Cancelala o esperá a que termine antes de reservarla de nuevo."
                                guardando = false
                                return@launch
                            }
                            val serviciosMap = state.items.map { it2 ->
                                mapOf("id" to it2.servicioId, "title" to it2.title, "image" to it2.image,
                                    "price" to it2.precio, "personas" to it2.personas,
                                    "lat" to it2.lat, "lng" to it2.lng)
                            }
                            runCatching {
                                FirestoreRepository.crearReserva(
                                    userId = user!!.uid, userEmail = user.email ?: "",
                                    fullname = user.displayName ?: user.email ?: "",
                                    checkin = state.checkin, checkout = state.checkout,
                                    servicios = serviciosMap,
                                    personas = state.items.sumOf { it2 -> it2.personas },
                                    total = carritoVm.total,
                                    acompanantes = acompMap
                                )
                            }.onSuccess { id ->
                                guardando = false
                                if (id != null) {
                                    carritoVm.limpiar(); onClose()
                                    navController.navigate(Screen.ReservaExitosa.route)
                                } else error = "Error al confirmar. Intentá de nuevo."
                            }.onFailure { e ->
                                guardando = false
                                error = e.message ?: "Error al confirmar. Intentá de nuevo."
                            }
                        }
                    },
                    enabled = !guardando,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                ) {
                    if (guardando) CircularProgressIndicator(color = Color.White,
                        modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Confirmar reserva", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (paso == 0) {
                TextButton(onClick = onClose) { Text("Cerrar", color = BETextSecond) }
            } else {
                TextButton(onClick = { paso = 0; error = null }) { Text("Volver", color = BETextSecond) }
            }
        }
    )
}
