package com.example.bairesessence.core.ui.screens.reservas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bairesessence.core.ui.components.BottomBar
import com.example.bairesessence.core.ui.components.DateRangePickerDialog
import com.example.bairesessence.core.ui.components.ResenaDialog
import com.example.bairesessence.core.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisReservasScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()
    val vm: ReservasViewModel = viewModel()
    val reservas by vm.reservas.collectAsState()
    val cargando by vm.cargando.collectAsState()
    val fetchError by vm.fetchError.collectAsState()
    val mutacionError by vm.mutacionError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mutacionError) {
        mutacionError?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearMutacionError()
        }
    }

    var resenaTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var pasajerosTarget by remember { mutableStateOf<Pair<String, List<Map<String, Any>>>?>(null) }
    var cancelarTarget by remember { mutableStateOf<String?>(null) }
    var editFechasTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var confirmarTarget by remember { mutableStateOf<String?>(null) }
    var pasajerosNombradosTarget by remember { mutableStateOf<Pair<String, List<Map<String, Any>>>?>(null) }

    LaunchedEffect(user?.uid) {
        if (user?.uid != null) vm.cargar(user.uid)
    }

    val estadoColor = { e: String ->
        when (e) {
            "confirmada_usuario" -> BEPrimary
            "confirmada" -> BEPrimary
            "pagada"     -> BESuccess
            "cancelada"  -> BEError
            "finalizada" -> BETextMuted
            else         -> BEWarning
        }
    }
    val estadoLabel = { e: String ->
        when (e) {
            "confirmada_usuario" -> "Esperando seller"
            "confirmada" -> "Confirmada"
            "pagada"     -> "Pagada"
            "cancelada"  -> "Cancelada"
            "finalizada" -> "Finalizada"
            else         -> "Pendiente"
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Mis Reservas", fontWeight = FontWeight.SemiBold, color = Color.White)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BEDark)
            )
        },
        bottomBar = { BottomBar(navController) },
        containerColor = BEBackground
    ) { innerPadding ->
        when {
            cargando -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BEPrimary)
            }

            fetchError -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Error al cargar reservas", color = BEError, style = MaterialTheme.typography.titleSmall)
                }
            }

            reservas.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧳", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(8.dp))
                    Text("No tenés reservas todavía", style = MaterialTheme.typography.titleMedium, color = BETextSecond)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { navController.navigate("home") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                    ) { Text("Explorar experiencias →", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(reservas) { r ->
                    val reservaId = r["id"] as? String ?: ""
                    val estado = r["estado"] as? String ?: "pendiente"
                    val svcs = r["servicios"] as? List<*>
                    val color = estadoColor(estado)
                    val canReview = estado in listOf("pagada", "finalizada")

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BESurface),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column {
                            Box(Modifier.fillMaxWidth().height(4.dp).background(color))

                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "${r["checkin"]} → ${r["checkout"]}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold, color = BETextPrimary
                                        )
                                        Text(r["email"] as? String ?: "",
                                            style = MaterialTheme.typography.bodySmall, color = BETextMuted)
                                    }
                                    Surface(shape = RoundedCornerShape(99.dp), color = color.copy(alpha = 0.13f)) {
                                        Text(estadoLabel(estado),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (svcs != null) {
                                    Spacer(Modifier.height(10.dp))
                                    svcs.filterIsInstance<Map<*, *>>().forEach { s ->
                                        val personas = (s["personas"] as? Long)?.toInt() ?: 1
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(6.dp).background(BEPrimary, RoundedCornerShape(99.dp)))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "${s["title"] ?: "Experiencia"} · $personas persona${if (personas != 1) "s" else ""}",
                                                style = MaterialTheme.typography.bodySmall, color = BETextSecond
                                            )
                                        }
                                        Spacer(Modifier.height(2.dp))
                                    }
                                }

                                val motivo = r["motivoCancelacion"] as? String
                                if (!motivo.isNullOrBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(shape = RoundedCornerShape(8.dp), color = BEError.copy(0.08f)) {
                                        Text("❌ $motivo", modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.bodySmall, color = BEError)
                                    }
                                }

                                val total = when (val t = r["total"]) {
                                    is Double -> t
                                    is Long   -> t.toDouble()
                                    else      -> null
                                }
                                if (total != null && total > 0) {
                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(color = BEBorder)
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("Total", style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold, color = BETextSecond)
                                        Text("${"$"}${"%,.0f".format(total).replace(",", ".")}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold, color = BEPrimary)
                                    }
                                }

                                // Pagar — solo confirmadas
                                if (estado == "confirmada") {
                                    Spacer(Modifier.height(10.dp))
                                    Button(
                                        onClick = { navController.navigate("pago_detalle/$reservaId") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF009EE3))
                                    ) {
                                        Text("💳 Pagar con MercadoPago", color = Color.White,
                                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                // Esperando al seller — ya confirmaste tu parte
                                if (estado == "confirmada_usuario") {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(shape = RoundedCornerShape(8.dp), color = BEPrimary.copy(0.08f)) {
                                        Text("🕐 Confirmaste tu parte. Estamos esperando que el seller confirme para habilitar el pago.",
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.bodySmall, color = BEPrimary)
                                    }
                                }

                                // Editar pasajeros y fechas — solo pendientes
                                if (estado == "pendiente" && svcs != null) {
                                    Spacer(Modifier.height(10.dp))
                                    val serviciosList = svcs.filterIsInstance<Map<*, *>>().map { s ->
                                        mapOf(
                                            "id"       to (s["id"] as? String ?: ""),
                                            "title"    to (s["title"] as? String ?: ""),
                                            "price"    to (s["price"] ?: 0.0),
                                            "personas" to (s["personas"] ?: 1L),
                                            "image"    to (s["image"] as? String ?: ""),
                                            "lat"      to (s["lat"] ?: 0.0),
                                            "lng"      to (s["lng"] ?: 0.0)
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            @Suppress("UNCHECKED_CAST")
                                            val pasajerosActuales = (r["pasajeros"] as? List<*>)
                                                ?.mapNotNull { it as? Map<String, Any> }
                                                ?.takeIf { it.isNotEmpty() }
                                                ?: listOf(mapOf(
                                                    "nombre" to (r["fullname"] as? String ?: ""),
                                                    "dni" to "", "fechaNacimiento" to ""
                                                ))
                                            pasajerosNombradosTarget = Pair(reservaId, pasajerosActuales)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BEWarning),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BEWarning.copy(0.5f))
                                    ) { Text("🧑‍💼 Pasajeros (nombre y DNI)", fontWeight = FontWeight.SemiBold) }
                                    Spacer(Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { pasajerosTarget = Pair(reservaId, serviciosList) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BEWarning),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BEWarning.copy(0.5f))
                                    ) { Text("👥 Cantidad por actividad", fontWeight = FontWeight.SemiBold) }
                                    Spacer(Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = {
                                            editFechasTarget = Triple(
                                                reservaId,
                                                r["checkin"] as? String ?: "",
                                                r["checkout"] as? String ?: ""
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BEPrimary),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BEPrimary.copy(0.4f))
                                    ) { Text("📅 Modificar fechas", fontWeight = FontWeight.SemiBold) }
                                    Spacer(Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            @Suppress("UNCHECKED_CAST")
                                            val titular = (r["pasajeros"] as? List<*>)
                                                ?.firstOrNull() as? Map<String, Any>
                                            val nombreOk = (titular?.get("nombre") as? String)?.isNotBlank() == true
                                            val dniOk = (titular?.get("dni") as? String)?.isNotBlank() == true
                                            if (nombreOk && dniOk) {
                                                confirmarTarget = reservaId
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Completá nombre y DNI del titular en \"Pasajeros\" antes de confirmar.")
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                                    ) { Text("✅ Confirmar reserva", color = Color.White, fontWeight = FontWeight.SemiBold) }
                                    Spacer(Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { cancelarTarget = reservaId },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BEError),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BEError.copy(0.4f))
                                    ) { Text("✕ Cancelar reserva", fontWeight = FontWeight.SemiBold) }
                                }

                                // Reseña
                                if (canReview && svcs != null) {
                                    Spacer(Modifier.height(10.dp))
                                    val firstSvc = svcs.filterIsInstance<Map<*, *>>().firstOrNull()
                                    val svcId = firstSvc?.get("id") as? String ?: ""
                                    val svcTitle = firstSvc?.get("title") as? String ?: "la experiencia"
                                    OutlinedButton(
                                        onClick = { resenaTarget = Triple(reservaId, svcId, svcTitle) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BEStarColor),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BEStarColor.copy(0.5f))
                                    ) { Text("★ Dejar reseña", fontWeight = FontWeight.SemiBold) }
                                }

                                // Chat
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { navController.navigate("chat/$reservaId") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BEPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BEPrimary.copy(0.4f))
                                ) { Text("💬 Chat con equipo", fontWeight = FontWeight.SemiBold) }

                                // Ver en mapa
                                Spacer(Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { navController.navigate("mapa_filtrado/reserva:$reservaId") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BEPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BEPrimary.copy(0.4f))
                                ) { Text("🗺️ Ver en mapa", fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }
        }
    }

    pasajerosNombradosTarget?.let { (rId, pasajeros) ->
        PasajerosNombradosDialog(
            pasajeros = pasajeros,
            onDismiss = { pasajerosNombradosTarget = null },
            onConfirm = { actualizados ->
                vm.actualizarPasajerosNombrados(rId, actualizados)
                pasajerosNombradosTarget = null
            }
        )
    }

    resenaTarget?.let { (rId, sId, title) ->
        ResenaDialog(reservaId = rId, servicioId = sId, servicioTitle = title,
            onDismiss = { resenaTarget = null }, onSuccess = { resenaTarget = null })
    }

    pasajerosTarget?.let { (rId, servicios) ->
        PasajerosDialog(
            servicios = servicios,
            onDismiss = { pasajerosTarget = null },
            onConfirm = { actualizados ->
                val nuevoTotal = actualizados.sumOf {
                    val price = (it["price"] as? Number)?.toDouble() ?: 0.0
                    val personas = (it["personas"] as? Number)?.toInt() ?: 1
                    price * personas
                }
                vm.actualizarPasajeros(rId, actualizados, nuevoTotal)
                pasajerosTarget = null
            }
        )
    }

    cancelarTarget?.let { rId ->
        AlertDialog(
            onDismissRequest = { cancelarTarget = null },
            containerColor = BESurface,
            title = { Text("¿Cancelar reserva?", fontWeight = FontWeight.Bold, color = BETextPrimary) },
            text = {
                Text("Esta acción no se puede deshacer. ¿Estás seguro de que querés cancelar esta reserva?",
                    style = MaterialTheme.typography.bodyMedium, color = BETextSecond)
            },
            confirmButton = {
                Button(
                    onClick = { vm.cancelarReserva(rId); cancelarTarget = null },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BEError)
                ) { Text("Sí, cancelar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { cancelarTarget = null }) { Text("No, volver", color = BETextSecond) }
            }
        )
    }

    confirmarTarget?.let { rId ->
        AlertDialog(
            onDismissRequest = { confirmarTarget = null },
            containerColor = BESurface,
            title = { Text("¿Confirmar reserva?", fontWeight = FontWeight.Bold, color = BETextPrimary) },
            text = {
                Text("Una vez confirmada, ya no vas a poder editar pasajeros ni fechas. El equipo la revisará para habilitar el pago.",
                    style = MaterialTheme.typography.bodyMedium, color = BETextSecond)
            },
            confirmButton = {
                Button(
                    onClick = { vm.confirmarReserva(rId); confirmarTarget = null },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                ) { Text("Sí, confirmar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarTarget = null }) { Text("Todavía no", color = BETextSecond) }
            }
        )
    }

    editFechasTarget?.let { (rId, ci, co) ->
        DateRangePickerDialog(
            initialCheckin = ci,
            initialCheckout = co,
            onConfirm = { newCi, newCo ->
                vm.actualizarFechas(rId, newCi, newCo)
                editFechasTarget = null
            },
            onDismiss = { editFechasTarget = null }
        )
    }
}

// Mismo esquema que el "pasajeros[]" de la web (titular en el índice 0 +
// acompañantes, cada uno con nombre/dni/fechaNacimiento).
@Composable
private fun PasajerosNombradosDialog(
    pasajeros: List<Map<String, Any>>,
    onDismiss: () -> Unit,
    onConfirm: (List<Map<String, Any>>) -> Unit
) {
    var nombres by remember { mutableStateOf(pasajeros.map { it["nombre"] as? String ?: "" }) }
    var dnis by remember { mutableStateOf(pasajeros.map { it["dni"] as? String ?: "" }) }
    var fechas by remember { mutableStateOf(pasajeros.map { it["fechaNacimiento"] as? String ?: "" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BESurface,
        title = { Text("Pasajeros", fontWeight = FontWeight.Bold, color = BETextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                nombres.indices.forEach { i ->
                    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = BESurfaceVar),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(if (i == 0) "Titular" else "Acompañante $i",
                                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = BETextMuted)
                                if (i != 0) {
                                    TextButton(
                                        onClick = {
                                            nombres = nombres.toMutableList().also { it.removeAt(i) }
                                            dnis = dnis.toMutableList().also { it.removeAt(i) }
                                            fechas = fechas.toMutableList().also { it.removeAt(i) }
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = BEError)
                                    ) { Text("Quitar", style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                            OutlinedTextField(
                                value = nombres[i],
                                onValueChange = { v -> nombres = nombres.toMutableList().also { it[i] = v } },
                                label = { Text("Nombre completo") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder)
                            )
                            OutlinedTextField(
                                value = dnis[i],
                                onValueChange = { v -> dnis = dnis.toMutableList().also { it[i] = v } },
                                label = { Text("DNI / Pasaporte") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder)
                            )
                            OutlinedTextField(
                                value = fechas[i],
                                onValueChange = { v -> fechas = fechas.toMutableList().also { it[i] = v } },
                                label = { Text("Fecha nacimiento (dd/MM/yyyy)") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder)
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { nombres = nombres + ""; dnis = dnis + ""; fechas = fechas + "" },
                    colors = ButtonDefaults.textButtonColors(contentColor = BEPrimary)
                ) { Text("+ Agregar pasajero") }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val actualizados = nombres.indices.map { i ->
                        mapOf(
                            "nombre" to nombres[i].trim(),
                            "dni" to dnis[i].trim(),
                            "fechaNacimiento" to fechas[i].trim()
                        )
                    }
                    onConfirm(actualizados)
                },
                enabled = nombres[0].isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
            ) { Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = BETextSecond) }
        }
    )
}

@Composable
private fun PasajerosDialog(
    servicios: List<Map<String, Any>>,
    onDismiss: () -> Unit,
    onConfirm: (List<Map<String, Any>>) -> Unit
) {
    var personasPorServicio by remember {
        mutableStateOf(servicios.map { (it["personas"] as? Number)?.toInt() ?: 1 })
    }
    var eliminados by remember { mutableStateOf(setOf<Int>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BESurface,
        title = { Text("Editar reserva", fontWeight = FontWeight.Bold, color = BETextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (servicios.indices.all { it in eliminados }) {
                    Text("No podés quitar todas las experiencias.",
                        color = BEError, style = MaterialTheme.typography.bodySmall)
                }
                servicios.forEachIndexed { i, svc ->
                    if (i in eliminados) return@forEachIndexed
                    val personas = personasPorServicio.getOrElse(i) { 1 }
                    Card(shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = BESurfaceVar),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(svc["title"] as? String ?: "Experiencia",
                                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                                    color = BETextPrimary, modifier = Modifier.weight(1f))
                                TextButton(
                                    onClick = { eliminados = eliminados + i },
                                    colors = ButtonDefaults.textButtonColors(contentColor = BEError)
                                ) { Text("Quitar", style = MaterialTheme.typography.labelSmall) }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pasajeros:", style = MaterialTheme.typography.bodySmall, color = BETextMuted)
                                IconButton(onClick = {
                                    if (personas > 1)
                                        personasPorServicio = personasPorServicio.toMutableList().also { it[i] = personas - 1 }
                                }, modifier = Modifier.size(32.dp)) {
                                    Text("−", color = BEPrimary, fontWeight = FontWeight.Bold)
                                }
                                Text("$personas", fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp))
                                IconButton(onClick = {
                                    personasPorServicio = personasPorServicio.toMutableList().also { it[i] = personas + 1 }
                                }, modifier = Modifier.size(32.dp)) {
                                    Text("+", color = BEPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val indicesValidos = servicios.indices.filter { i -> i !in eliminados }
                    val actualizados = indicesValidos.map { originalIdx ->
                        servicios[originalIdx].toMutableMap().apply {
                            put("personas", personasPorServicio.getOrElse(originalIdx) { 1 }.toLong())
                        }
                    }
                    onConfirm(actualizados)
                },
                enabled = servicios.indices.any { it !in eliminados },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
            ) { Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = BETextSecond) }
        }
    )
}

