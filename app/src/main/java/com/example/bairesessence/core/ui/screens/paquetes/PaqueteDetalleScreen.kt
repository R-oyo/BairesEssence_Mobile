package com.example.bairesessence.core.ui.screens.paquetes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bairesessence.core.ui.components.ServicioCard
import com.example.bairesessence.core.ui.screens.carrito.CarritoViewModel
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.example.bairesessence.data.model.ItemCarrito
import com.example.bairesessence.data.model.Paquete
import com.example.bairesessence.data.model.Servicio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaqueteDetalleScreen(
    navController: NavController,
    paqueteId: String,
    carritoVm: CarritoViewModel
) {
    var paquete by remember { mutableStateOf<Paquete?>(null) }
    var servicios by remember { mutableStateOf(listOf<Servicio>()) }
    var cargando by remember { mutableStateOf(true) }
    var agregado by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(paqueteId) {
        runCatching {
            val p = FirestoreRepository.fetchPaqueteById(paqueteId)
            paquete = p
            if (p != null && p.servicios.isNotEmpty()) {
                servicios = FirestoreRepository.fetchServiciosByIds(p.servicios)
            }
        }
        cargando = false
    }

    LaunchedEffect(agregado) {
        if (agregado) {
            snackbarHostState.showSnackbar("Paquete agregado al carrito")
            agregado = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        paquete?.nombre ?: "Paquete",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BEDark)
            )
        },
        containerColor = BEBackground
    ) { innerPadding ->
        if (cargando) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BEPrimary)
            }
        } else if (paquete == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Paquete no encontrado", color = BEError)
            }
        } else {
            val p = paquete!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Imagen de portada
                if (p.imagen.isNotBlank()) {
                    item {
                        AsyncImage(
                            model = p.imagen,
                            contentDescription = p.nombre,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                    }
                }

                // Info del paquete
                item {
                    Card(
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = BESurface),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        p.nombre,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = BETextPrimary
                                    )
                                    if (p.agencia.isNotBlank()) {
                                        Text(
                                            "por ${p.agencia}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BETextMuted
                                        )
                                    }
                                }
                                if (p.descuento > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BEPrimary
                                    ) {
                                        Text(
                                            p.descuentoFormateado,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (p.descripcion.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    p.descripcion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BETextSecond
                                )
                            }

                            if (p.precioTotal > 0) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = BEBorder)
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Total del paquete",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BETextSecond
                                    )
                                    Text(
                                        p.precioFormateado,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = BEPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Sección de servicios
                if (servicios.isNotEmpty()) {
                    item {
                        Text(
                            "Experiencias incluidas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BETextPrimary,
                            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
                        )
                    }
                    items(servicios, key = { it.id }) { s ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            ServicioCard(
                                servicio = s,
                                enCarrito = carritoVm.estaEnCarrito(s.id),
                                esFavorito = false,
                                onFavoritoClick = {},
                                onClick = { navController.navigate("detalle/${s.id}") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Botón agregar al carrito
                item {
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        Button(
                            onClick = {
                                servicios.forEach { s ->
                                    carritoVm.agregarItem(
                                        ItemCarrito(
                                            servicioId = s.id,
                                            title = s.title,
                                            image = s.image,
                                            precio = s.precio,
                                            lat = s.lat,
                                            lng = s.lng,
                                            ubicacion = s.ubicacion
                                        )
                                    )
                                }
                                agregado = true
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                        ) {
                            Text(
                                "✨  Agregar paquete al carrito",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
