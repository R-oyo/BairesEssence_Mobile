package com.example.bairesessence.core.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.example.bairesessence.data.model.Servicio
import com.example.bairesessence.core.ui.components.BottomBar
import com.example.bairesessence.core.ui.components.ServicioCard
import com.example.bairesessence.core.ui.components.TopBar
import kotlinx.coroutines.launch

@Composable
fun MainScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var servicios by remember { mutableStateOf(listOf<Servicio>()) }

    // 🔹 Cargar servicios desde Firestore
    LaunchedEffect(Unit) {
        scope.launch {
            servicios = FirestoreRepository.fetchServicios()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 🔹 Top Bar
        TopBar(title = "BAIRES ESSENCE")

        // 🔹 Contenido principal
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFF9F9F9)),
            contentAlignment = Alignment.Center
        ) {
            if (servicios.isEmpty()) {
                Text("Cargando servicios...", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(servicios) { servicio ->
                        ServicioCard(servicio = servicio) {
                            // 🔹 Acción al hacer clic (por ahora vacía)
                            // navController.navigate("detalle/${servicio.id}")
                        }
                    }
                }
            }
        }

        // 🔹 Bottom Bar
        BottomBar(navController = navController)
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()

    // 🔹 Datos de ejemplo para el preview
    val fakeServicios = listOf(
        Servicio(id = "1", title = "Tour Caminata Palermo", description = "Recorrido a pie por Palermo", image = ""),
        Servicio(id = "1", title = "Tour Caminata Palermo", description = "Recorrido a pie por Palermo", image = ""),
        Servicio(id = "1", title = "Tour Caminata Palermo", description = "Recorrido a pie por Palermo", image = ""),
        Servicio(id = "1", title = "Tour Caminata Palermo", description = "Recorrido a pie por Palermo", image = ""),
        Servicio(id = "1", title = "Tour Caminata Palermo", description = "Recorrido a pie por Palermo", image = ""),
        Servicio(id = "1", title = "Tour Caminata Palermo", description = "Recorrido a pie por Palermo", image = ""),
        Servicio(id = "1", title = "Tour Caminata Palermo", description = "Recorrido a pie por Palermo", image = ""),
        Servicio(id = "2", title = "Cena Tango Show", description = "Cena con espectáculo de tango", image = "")
    )

    // 🔹 Versión mock de la pantalla
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TopBar(title = "BAIRES ESSENCE")

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(fakeServicios) { servicio ->
                ServicioCard(servicio = servicio, onClick = {})
            }
        }

        BottomBar(navController = navController)
    }
}
