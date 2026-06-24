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
import com.example.bairesessence.core.navigation.Screen
import com.example.bairesessence.core.ui.components.BottomBar
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.example.bairesessence.data.model.Paquete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaquetesScreen(navController: NavController) {
    var paquetes by remember { mutableStateOf(listOf<Paquete>()) }
    var cargando by remember { mutableStateOf(true) }
    var fetchError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            paquetes = FirestoreRepository.fetchPaquetes()
        }.onFailure { fetchError = true }
        cargando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Paquetes especiales", fontWeight = FontWeight.SemiBold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
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
                    Text("Error al cargar paquetes", color = BEError, style = MaterialTheme.typography.titleSmall)
                }
            }

            paquetes.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("✨", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(12.dp))
                    Text("No hay paquetes disponibles", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = BETextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("Pronto tendremos paquetes especiales para vos.",
                        style = MaterialTheme.typography.bodyMedium, color = BETextSecond)
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(paquetes, key = { it.id }) { paquete ->
                    PaqueteCard(paquete = paquete, onClick = {
                        navController.navigate("paquete_detalle/${paquete.id}")
                    })
                }
            }
        }
    }
}

@Composable
private fun PaqueteCard(paquete: Paquete, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BESurface),
        elevation = CardDefaults.cardElevation(3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (paquete.imagen.isNotBlank()) {
                AsyncImage(
                    model = paquete.imagen,
                    contentDescription = paquete.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
            Column(Modifier.padding(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            paquete.nombre,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BETextPrimary
                        )
                        if (paquete.agencia.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "por ${paquete.agencia}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BETextMuted
                            )
                        }
                    }
                    if (paquete.descuento > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BEPrimary
                        ) {
                            Text(
                                paquete.descuentoFormateado,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (paquete.precioTotal > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        paquete.precioFormateado,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BEPrimary
                    )
                }
            }
        }
    }
}
