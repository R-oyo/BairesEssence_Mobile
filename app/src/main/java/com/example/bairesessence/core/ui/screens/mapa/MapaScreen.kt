package com.example.bairesessence.core.ui.screens.mapa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.example.bairesessence.data.model.Servicio
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(navController: NavController, filter: String = "all") {
    val user = FirebaseAuth.getInstance().currentUser

    var servicios by remember { mutableStateOf(listOf<Servicio>()) }
    var cargando by remember { mutableStateOf(true) }
    var seleccionado by remember { mutableStateOf<Servicio?>(null) }

    val buenosAires = LatLng(-34.6037, -58.3816)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(buenosAires, 12f)
    }

    val titulo = when {
        filter == "all"               -> "Experiencias en el mapa"
        filter == "favoritos"         -> "Mis favoritos"
        filter.startsWith("reserva:") -> "Experiencias reservadas"
        filter == "itinerario"        -> "Ruta del día"
        else                          -> "Mapa"
    }

    LaunchedEffect(filter) {
        cargando = true
        runCatching {
            servicios = when {
                filter == "all" ->
                    FirestoreRepository.fetchServicios().filter { it.tieneMapa }
                filter == "favoritos" && user != null ->
                    FirestoreRepository.fetchFavoritos(user.uid).filter { it.tieneMapa }
                filter.startsWith("reserva:") -> {
                    val reservaId = filter.removePrefix("reserva:")
                    FirestoreRepository.fetchServiciosDeReserva(reservaId)
                }
                filter == "itinerario" && user != null ->
                    FirestoreRepository.fetchServiciosDeItinerario(user.uid)
                else -> emptyList()
            }
        }.onFailure { servicios = emptyList() }
        cargando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo, fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BEDark)
            )
        }
    ) { innerPadding ->
        if (cargando) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BEPrimary)
            }
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                cameraPositionState = cameraPositionState
            ) {
                servicios.forEach { servicio ->
                    Marker(
                        state = MarkerState(position = LatLng(servicio.lat, servicio.lng)),
                        title = servicio.title,
                        snippet = servicio.precioFormateado,
                        onClick = { _ ->
                            seleccionado = servicio
                            true
                        }
                    )
                }
            }
        }
    }

    seleccionado?.let { s ->
        ModalBottomSheet(
            onDismissRequest = { seleccionado = null },
            containerColor = BESurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    s.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BETextPrimary
                )
                if (s.ubicacion.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(s.ubicacion, style = MaterialTheme.typography.bodySmall, color = BETextMuted)
                }
                if (s.tienePrecio) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        s.precioFormateado,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BEPrimary
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        seleccionado = null
                        navController.navigate("detalle/${s.id}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                ) {
                    Text("Ver detalle →", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
