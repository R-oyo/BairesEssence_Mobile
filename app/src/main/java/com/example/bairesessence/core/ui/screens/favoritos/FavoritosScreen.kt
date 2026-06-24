package com.example.bairesessence.core.ui.screens.favoritos

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bairesessence.core.ui.components.BottomBar
import com.example.bairesessence.core.ui.components.ServicioCard
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.example.bairesessence.data.model.Servicio
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritosScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()
    var favoritos by remember { mutableStateOf(listOf<Servicio>()) }
    var favoritoIds by remember { mutableStateOf(setOf<String>()) }
    var cargando by remember { mutableStateOf(true) }
    var fetchError by remember { mutableStateOf(false) }

    LaunchedEffect(user?.uid) {
        if (user != null) {
            try {
                favoritos = FirestoreRepository.fetchFavoritos(user.uid)
                favoritoIds = favoritos.map { it.id }.toSet()
            } catch (e: Exception) {
                fetchError = true
            }
        }
        cargando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis favoritos", fontWeight = FontWeight.SemiBold, color = Color.White) },
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
                    Text("Error al cargar favoritos", color = BEError, style = MaterialTheme.typography.titleSmall)
                }
            }

            favoritos.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("🤍", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(12.dp))
                    Text("Todavía no guardaste nada", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = BETextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("Presioná el ❤ en cualquier experiencia para guardarla aquí.",
                        style = MaterialTheme.typography.bodyMedium, color = BETextSecond)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { navController.navigate("home") },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                    ) { Text("Explorar experiencias", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${favoritos.size} guardada${if (favoritos.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall, color = BETextMuted
                        )
                        TextButton(onClick = { navController.navigate("mapa_filtrado/favoritos") }) {
                            Text("Ver en mapa 🗺️", color = BEPrimary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                items(favoritos, key = { it.id }) { servicio ->
                    ServicioCard(
                        servicio = servicio,
                        esFavorito = servicio.id in favoritoIds,
                        onFavoritoClick = {
                            scope.launch {
                                val esFav = FirestoreRepository.toggleFavorito(user!!.uid, servicio.id)
                                if (!esFav) {
                                    favoritos = favoritos.filter { it.id != servicio.id }
                                    favoritoIds = favoritoIds - servicio.id
                                }
                            }
                        },
                        onClick = { navController.navigate("detalle/${servicio.id}") }
                    )
                }
            }
        }
    }
}
