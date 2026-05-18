package com.example.bairesessence.core.ui.screens.pagos

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
import com.example.bairesessence.core.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagosScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser
    val scope = rememberCoroutineScope()

    var reservas by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(user?.uid) {
        if (user?.uid != null) {
            scope.launch {
                try {
                    val snap = db.collection("reservas").whereEqualTo("userId", user.uid).get().await()
                    reservas = snap.documents.mapNotNull { doc ->
                        val d = doc.data?.toMutableMap() ?: return@mapNotNull null
                        if (!d.containsKey("estado")) d["estado"] = "pendiente"
                        d["id"] = doc.id
                        d
                    }
                } catch (e: Exception) { e.printStackTrace() }
                cargando = false
            }
        } else { cargando = false }
    }

    val estadoColor = { estado: String -> when (estado) {
        "confirmada" -> Color(0xFF10B981)
        "pagada"     -> Color(0xFF6366F1)
        "cancelada"  -> Color(0xFFEF4444)
        else         -> Color(0xFFF59E0B)
    }}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Reservas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BEDark)
            )
        },
        bottomBar = { BottomBar(navController) },
        containerColor = BEBackground
    ) { innerPadding ->
        if (cargando) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BEPrimary)
            }
        } else if (reservas.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧳", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("No tenés reservas", style = MaterialTheme.typography.titleMedium, color = BETextSecondary)
                    TextButton(onClick = { navController.navigate("home") }) {
                        Text("Ver experiencias", color = BEPrimary)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reservas) { r ->
                    val estado = r["estado"] as? String ?: "pendiente"
                    val servicios = r["servicios"] as? List<*>
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BESurface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Barra de color por estado
                            Box(Modifier.fillMaxWidth().height(4.dp).fillMaxWidth()) {
                                Surface(modifier = Modifier.fillMaxSize(), color = estadoColor(estado)) {}
                            }
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("${r["checkin"]} → ${r["checkout"]}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text(r["email"] as? String ?: "", style = MaterialTheme.typography.bodySmall, color = BETextMuted)
                                    }
                                    Surface(shape = RoundedCornerShape(99.dp), color = estadoColor(estado).copy(alpha = 0.15f)) {
                                        Text(estado, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall, color = estadoColor(estado), fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (servicios != null) {
                                    Spacer(Modifier.height(8.dp))
                                    servicios.filterIsInstance<Map<*, *>>().forEach { s ->
                                        Text("• ${s["title"] ?: "Experiencia"}", style = MaterialTheme.typography.bodySmall, color = BETextSecondary)
                                    }
                                }
                                if (estado == "confirmada") {
                                    Spacer(Modifier.height(10.dp))
                                    Button(
                                        onClick = { navController.navigate("pago_detalle/${r["id"]}") },
                                        modifier = Modifier.fillMaxWidth().height(40.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009EE3))
                                    ) {
                                        Text("💳 Pagar con MercadoPago", color = Color.White, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
