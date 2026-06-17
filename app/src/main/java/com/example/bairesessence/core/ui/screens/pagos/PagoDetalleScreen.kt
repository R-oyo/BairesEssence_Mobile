package com.example.bairesessence.core.ui.screens.pagos

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bairesessence.core.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val RAILWAY_BASE_URL = "https://tu-app.railway.app"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagoDetalleScreen(navController: NavController, reservaId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    var reserva by remember { mutableStateOf<Map<String, Any>?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var procesando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Snapshot listener: detecta el cambio de estado cuando el webhook de MP actualiza Firestore
    DisposableEffect(reservaId) {
        val reg = db.collection("reservas").document(reservaId)
            .addSnapshotListener { snap, _ ->
                reserva = snap?.data
                cargando = false
            }
        onDispose { reg.remove() }
    }

    val estado = reserva?.get("estado") as? String ?: ""
    val servicios = reserva?.get("servicios") as? List<*>
    val total = when (val t = reserva?.get("total")) {
        is Double -> t
        is Long   -> t.toDouble()
        else      -> null
    }

    fun iniciarPago() {
        scope.launch {
            procesando = true
            error = null
            try {
                val user = FirebaseAuth.getInstance().currentUser
                    ?: throw Exception("Sesión no activa.")
                val idToken = user.getIdToken(false).await().token
                    ?: throw Exception("No se pudo obtener el token.")

                val initPoint = withContext(Dispatchers.IO) {
                    val url = URL("$RAILWAY_BASE_URL/createMercadoPagoPreference")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $idToken")
                    conn.doOutput = true
                    conn.outputStream.use { out ->
                        out.write("""{"reservaId":"$reservaId"}""".toByteArray())
                    }
                    val code = conn.responseCode
                    val body = (if (code == 200) conn.inputStream else conn.errorStream)
                        .bufferedReader().readText()
                    if (code != 200) throw Exception("Error $code: $body")
                    JSONObject(body).getString("initPoint")
                }

                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(context, Uri.parse(initPoint))
            } catch (e: Exception) {
                error = e.message ?: "Error al conectar con el sistema de pagos."
            } finally {
                procesando = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Confirmar pago",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BEDark)
            )
        },
        containerColor = BEBackground
    ) { innerPadding ->
        when {
            cargando -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BEPrimary)
            }

            estado == "pagada" -> PagoExitosoContent(
                modifier = Modifier.padding(innerPadding),
                onVerReservas = {
                    navController.navigate("mis_reservas") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Resumen de la reserva
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BESurface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Resumen de la reserva",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BETextPrimary
                        )
                        Spacer(Modifier.height(12.dp))

                        val checkin  = reserva?.get("checkin")  as? String
                        val checkout = reserva?.get("checkout") as? String
                        if (!checkin.isNullOrBlank() && !checkout.isNullOrBlank()) {
                            InfoRow(label = "Período", value = "$checkin → $checkout")
                            Spacer(Modifier.height(6.dp))
                        }

                        val email = reserva?.get("email") as? String
                        if (!email.isNullOrBlank()) {
                            InfoRow(label = "Email", value = email)
                        }

                        if (!servicios.isNullOrEmpty()) {
                            HorizontalDivider(color = BEBorder, modifier = Modifier.padding(vertical = 10.dp))
                            Text(
                                "Experiencias incluidas",
                                style = MaterialTheme.typography.labelMedium,
                                color = BETextMuted
                            )
                            Spacer(Modifier.height(6.dp))
                            servicios.filterIsInstance<Map<*, *>>().forEach { s ->
                                val personas = (s["personas"] as? Long)?.toInt() ?: 1
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "· ${s["title"] ?: "Experiencia"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BETextSecond,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "×$personas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BETextMuted
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }

                        if (total != null && total > 0) {
                            HorizontalDivider(color = BEBorder, modifier = Modifier.padding(vertical = 10.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total a pagar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${"$"}${"%,.0f".format(total).replace(",", ".")}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BEPrimary
                                )
                            }
                        }
                    }
                }

                // Aviso si la reserva todavía está pendiente de confirmación
                if (estado == "pendiente") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BEWarning.copy(alpha = 0.1f)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⏳", style = MaterialTheme.typography.bodyLarge)
                            Column {
                                Text(
                                    "Reserva pendiente de confirmación",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BEWarning
                                )
                                Text(
                                    "El pago se habilitará cuando la reserva sea confirmada.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BETextMuted
                                )
                            }
                        }
                    }
                }

                // Error de pago
                AnimatedVisibility(visible = error != null) {
                    Surface(shape = RoundedCornerShape(10.dp), color = BEError.copy(alpha = 0.1f)) {
                        Text(
                            "⚠️ ${error.orEmpty()}",
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = BEError
                        )
                    }
                }

                // Botón pagar — solo visible para reservas confirmadas
                if (estado == "confirmada") {
                    Button(
                        onClick = ::iniciarPago,
                        enabled = !procesando,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009EE3))
                    ) {
                        if (procesando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Iniciando pago...", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                "💳  Pagar con MercadoPago",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Text(
                        "Serás redirigido al checkout seguro de MercadoPago. " +
                        "Tu reserva se actualizará automáticamente al completar el pago.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BETextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = BETextMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = BETextPrimary)
    }
}

@Composable
private fun PagoExitosoContent(modifier: Modifier = Modifier, onVerReservas: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = BEPrimary,
                modifier = Modifier.size(80.dp)
            )
            Text(
                "¡Pago confirmado!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BETextPrimary
            )
            Text(
                "Tu reserva fue pagada exitosamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = BETextSecond
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onVerReservas,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
            ) {
                Text("Ver mis reservas", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
