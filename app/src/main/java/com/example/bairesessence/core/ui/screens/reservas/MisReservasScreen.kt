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
import androidx.navigation.NavController
import com.example.bairesessence.core.ui.components.BottomBar
import com.example.bairesessence.core.ui.components.ResenaDialog
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisReservasScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    var reservas by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var cargando by remember { mutableStateOf(true) }
    var fetchError by remember { mutableStateOf(false) }

    // Review dialog state
    var resenaTarget by remember { mutableStateOf<Triple<String, String, String>?>(null) } // reservaId, servicioId, title

    LaunchedEffect(user?.uid) {
        if (user?.uid != null) {
            try {
                reservas = FirestoreRepository.fetchReservasByUser(user.uid)
            } catch (e: Exception) {
                fetchError = true
            }
        }
        cargando = false
    }

    val estadoColor = { e: String ->
        when (e) {
            "confirmada" -> BEPrimary
            "pagada"     -> BESuccess
            "cancelada"  -> BEError
            "finalizada" -> BETextMuted
            else         -> BEWarning
        }
    }
    val estadoLabel = { e: String ->
        when (e) {
            "confirmada" -> "Confirmada"
            "pagada"     -> "Pagada"
            "cancelada"  -> "Cancelada"
            "finalizada" -> "Finalizada"
            else         -> "Pendiente"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Reservas", fontWeight = FontWeight.SemiBold, color = Color.White) },
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
                    Text("No tenés reservas todavía", style = MaterialTheme.typography.titleMedium,
                        color = BETextSecond)
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
                    val canReview = estado in listOf("confirmada", "pagada", "finalizada")

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BESurface),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column {
                            // Colored top bar (TripAdvisor-style status indicator)
                            Box(Modifier.fillMaxWidth().height(4.dp).background(color))

                            Column(Modifier.padding(16.dp)) {
                                // Header row: dates + status badge
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
                                        Text(
                                            r["email"] as? String ?: "",
                                            style = MaterialTheme.typography.bodySmall, color = BETextMuted
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(99.dp),
                                        color = color.copy(alpha = 0.13f)
                                    ) {
                                        Text(
                                            estadoLabel(estado),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = color, fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Services list
                                if (svcs != null) {
                                    Spacer(Modifier.height(10.dp))
                                    svcs.filterIsInstance<Map<*, *>>().forEach { s ->
                                        val personas = (s["personas"] as? Long)?.toInt() ?: 1
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                Modifier.size(6.dp).background(BEPrimary, RoundedCornerShape(99.dp))
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "${s["title"] ?: "Experiencia"} · $personas persona${if (personas != 1) "s" else ""}",
                                                style = MaterialTheme.typography.bodySmall, color = BETextSecond
                                            )
                                        }
                                        Spacer(Modifier.height(2.dp))
                                    }
                                }

                                // Cancellation reason
                                val motivo = r["motivoCancelacion"] as? String
                                if (!motivo.isNullOrBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(shape = RoundedCornerShape(8.dp), color = BEError.copy(0.08f)) {
                                        Text("❌ $motivo", modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.bodySmall, color = BEError)
                                    }
                                }

                                // Total
                                val total = when (val t = r["total"]) {
                                    is Double -> t
                                    is Long   -> t.toDouble()
                                    else      -> null
                                }
                                if (total != null && total > 0) {
                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(color = BEBorder)
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Total", style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold, color = BETextSecond)
                                        Text(
                                            "${"$"}${"%,.0f".format(total).replace(",", ".")}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold, color = BEPrimary
                                        )
                                    }
                                }

                                // Pay button — only for confirmed reservations
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

                                // Review button — only for eligible states
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
                                    ) {
                                        Text("★ Dejar reseña", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Review dialog
    resenaTarget?.let { (rId, sId, title) ->
        ResenaDialog(
            reservaId = rId,
            servicioId = sId,
            servicioTitle = title,
            onDismiss = { resenaTarget = null },
            onSuccess = { resenaTarget = null }
        )
    }
}
