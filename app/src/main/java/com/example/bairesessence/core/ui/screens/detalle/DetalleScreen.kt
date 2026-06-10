package com.example.bairesessence.core.ui.screens.detalle

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.bairesessence.core.ui.components.RatingStars
import com.example.bairesessence.core.ui.screens.carrito.CarritoViewModel
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.example.bairesessence.data.model.ItemCarrito
import com.example.bairesessence.data.model.Servicio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    navController: NavController,
    servicioId: String,
    carritoVm: CarritoViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser
    var servicio by remember { mutableStateOf<Servicio?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var personas by remember { mutableStateOf(1) }
    var esFavorito by remember { mutableStateOf(false) }
    var reviews by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    val carritoState by carritoVm.state.collectAsState()
    val enCarrito = carritoVm.estaEnCarrito(servicioId)

    LaunchedEffect(servicioId) {
        servicio = try { FirestoreRepository.fetchServicioById(servicioId) } catch (e: Exception) { null }
        if (user != null) {
            esFavorito = try {
                servicioId in FirestoreRepository.fetchFavoritoIds(user.uid)
            } catch (e: Exception) { false }
        }
        reviews = try { FirestoreRepository.fetchReviewsByServicio(servicioId) } catch (_: Exception) { emptyList() }
        cargando = false
    }

    if (cargando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BEPrimary)
        }
        return
    }

    val s = servicio ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No se encontró la experiencia", color = BETextSecond)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // Hero image
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                AsyncImage(
                    model = s.image.ifBlank { null },
                    contentDescription = s.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(0.3f), Color.Transparent, Color.Black.copy(0.5f)))
                ))
                // Back button
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(8.dp)) {
                    Surface(shape = RoundedCornerShape(99.dp), color = Color.White.copy(0.9f)) {
                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.padding(8.dp), tint = BETextPrimary)
                    }
                }
                // Favorite button — top right of hero
                if (user != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        shape = RoundedCornerShape(99.dp),
                        color = Color.White.copy(0.9f)
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    esFavorito = FirestoreRepository.toggleFavorito(user.uid, servicioId)
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (esFavorito) BEError else BETextMuted
                            )
                        }
                    }
                }
                if (s.categoria.isNotBlank()) {
                    Surface(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                        shape = RoundedCornerShape(99.dp), color = BEPrimary) {
                        Text(s.categoria.uppercase(), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.background(BEBackground).padding(20.dp)) {
                Text(s.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                RatingStars(s.rating)
                Spacer(Modifier.height(16.dp))

                // Info rápida
                if (s.duracion.isNotBlank() || s.idioma.isNotBlank() || s.ubicacion.isNotBlank()) {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BESurface)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (s.duracion.isNotBlank()) InfoRow("⏱", "Duración", s.duracion)
                            if (s.idioma.isNotBlank()) InfoRow("🌐", "Idioma", s.idioma)
                            if (s.ubicacion.isNotBlank()) InfoRow("📍", "Ubicación", s.ubicacion)
                            if (s.fechaDesde != null) InfoRow("📅", "Disponible", "${s.fechaDesde} → ${s.fechaHasta ?: "..."}")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (s.tieneMapa) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("geo:${s.lat},${s.lng}?q=${s.lat},${s.lng}(${Uri.encode(s.title)})")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BEPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BEPrimary.copy(0.4f))
                    ) { Text("📍 Ver ubicación en Maps", fontWeight = FontWeight.SemiBold) }
                }

                Spacer(Modifier.height(16.dp))
                Text("Descripción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(s.description, style = MaterialTheme.typography.bodyMedium, color = BETextSecond)

                if (s.incluye.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))) {
                        Column(Modifier.padding(16.dp)) {
                            Text("✅ ¿Qué incluye?", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold, color = BEPrimaryDark)
                            Spacer(Modifier.height(4.dp))
                            Text(s.incluye, style = MaterialTheme.typography.bodyMedium, color = BEPrimary)
                        }
                    }
                }

                if (s.whatsapp.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("https://wa.me/${s.whatsapp}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(0.5f))
                    ) { Text("💬 Consultar por WhatsApp", fontWeight = FontWeight.SemiBold) }
                }

                // Selector de personas
                Spacer(Modifier.height(16.dp))
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BESurface)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Personas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (personas > 1) personas-- }) { Text("−", color = BEPrimary, style = MaterialTheme.typography.titleLarge) }
                            Text("$personas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                            IconButton(onClick = { personas++ }) { Text("+", color = BEPrimary, style = MaterialTheme.typography.titleLarge) }
                        }
                    }
                }

                // Total estimado
                if (s.tienePrecio) {
                    Spacer(Modifier.height(8.dp))
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = BEPrimaryDark)
                            Text("$${"%,.0f".format(s.precio * personas).replace(",", ".")}",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BEPrimary)
                        }
                    }
                }

                // Reseñas
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = BEBorder)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reseñas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (reviews.isNotEmpty()) {
                        val avg = reviews.mapNotNull { (it["rating"] as? Number)?.toDouble() }.average()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("★", color = BEStarColor, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(3.dp))
                            Text("${"%.1f".format(avg)} (${reviews.size})",
                                style = MaterialTheme.typography.bodySmall, color = BETextMuted)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (reviews.isEmpty()) {
                    Text("Todavía no hay reseñas para esta experiencia.",
                        style = MaterialTheme.typography.bodyMedium, color = BETextMuted)
                } else {
                    reviews.take(5).forEach { r ->
                        val rVal = (r["rating"] as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
                        val comment = r["comentario"] as? String
                        val author = (r["email"] as? String)?.substringBefore("@") ?: "Usuario"
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = BESurfaceVar),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(author, style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold, color = BETextPrimary)
                                    Text("★".repeat(rVal) + "☆".repeat(5 - rVal),
                                        color = BEStarColor, style = MaterialTheme.typography.bodySmall)
                                }
                                if (!comment.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(comment, style = MaterialTheme.typography.bodySmall, color = BETextSecond)
                                }
                            }
                        }
                    }
                }

                // Espacio para botón flotante
                Spacer(Modifier.height(120.dp))
            }
        }

        // Botón flotante agregar al carrito
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(BESurface).padding(16.dp)) {
            if (s.tienePrecio) {
                Text("Desde", style = MaterialTheme.typography.labelSmall, color = BETextSecond)
                Text(s.precioFormateado, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = BETextPrimary)
                Text("por persona", style = MaterialTheme.typography.labelSmall, color = BETextSecond)
                Spacer(Modifier.height(8.dp))
            }

            if (!enCarrito) {
                Button(
                    onClick = {
                        carritoVm.agregarItem(ItemCarrito(
                            servicioId = s.id, title = s.title, image = s.image,
                            precio = s.precio, lat = s.lat, lng = s.lng,
                            ubicacion = s.ubicacion, personas = personas
                        ))
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                ) {
                    Text("+ Agregar al carrito", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = { carritoVm.quitarItem(s.id); navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BEError)
                ) {
                    Text("✅ En carrito — Quitar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(emoji: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = BETextMuted)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = BETextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}
