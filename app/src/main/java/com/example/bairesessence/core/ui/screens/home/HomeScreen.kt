package com.example.bairesessence.core.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bairesessence.core.navigation.Screen
import com.example.bairesessence.core.ui.components.BottomBar
import com.example.bairesessence.core.ui.components.ServicioCard
import com.example.bairesessence.core.ui.screens.carrito.CarritoViewModel
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.example.bairesessence.data.model.Servicio
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private val CATEGORIAS = listOf("Todas", "Tours", "Gastronomia", "Traslados", "Experiencias")

@Composable
fun CatalogoScreen(
    navController: NavController,
    carritoVm: CarritoViewModel
) {
    val carritoState by carritoVm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var servicios by remember { mutableStateOf(listOf<Servicio>()) }
    var cargando by remember { mutableStateOf(true) }
    var fetchError by remember { mutableStateOf(false) }
    var catActiva by remember { mutableStateOf("Todas") }
    var busqueda by remember { mutableStateOf("") }
    var mostrarCarrito by remember { mutableStateOf(false) }
    var favoritoIds by remember { mutableStateOf(setOf<String>()) }

    var mostrarFechasDialog by remember { mutableStateOf(false) }
    var checkinTemp by remember { mutableStateOf("") }
    var checkoutTemp by remember { mutableStateOf("") }
    var errorFechas by remember { mutableStateOf<String?>(null) }

    val user = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(Unit) {
        try {
            servicios = FirestoreRepository.fetchServicios()
        } catch (e: Exception) {
            fetchError = true
        }
        if (user != null) {
            try {
                favoritoIds = FirestoreRepository.fetchFavoritoIds(user.uid)
            } catch (_: Exception) {
                // Favoritos no disponibles — no es fatal
            }
        }
        cargando = false
    }

    val filtrados = servicios.filter { s ->
        val matchCat = catActiva == "Todas" || s.categoria.equals(catActiva, true)
        val matchQ = busqueda.isBlank() || s.title.contains(busqueda, true) || s.description.contains(busqueda, true)
        matchCat && matchQ
    }

    Scaffold(
        containerColor = BEBackground,
        bottomBar = { BottomBar(navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {

                // ── Dark header
                item {
                    Column(modifier = Modifier.background(BEDark).padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Baires Essence", style = MaterialTheme.typography.titleMedium,
                                    color = BEPrimaryLight, fontWeight = FontWeight.Bold)
                                Text("Buenos Aires, Argentina",
                                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                BadgedBox(badge = {
                                    if (carritoState.items.isNotEmpty())
                                        Badge(containerColor = BEPrimary) { Text("${carritoState.items.size}") }
                                }) {
                                    IconButton(onClick = { mostrarCarrito = true }) {
                                        Icon(Icons.Default.ShoppingCart, "Carrito", tint = Color.White)
                                    }
                                }
                                IconButton(onClick = { navController.navigate(Screen.Perfil.route) }) {
                                    Icon(Icons.Default.Person, "Perfil", tint = Color.White)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Search bar
                        OutlinedTextField(
                            value = busqueda, onValueChange = { busqueda = it },
                            placeholder = { Text("Buscar experiencias...", color = Color.White.copy(0.45f)) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BEPrimaryLight,
                                unfocusedBorderColor = Color.White.copy(0.25f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        // Date chip — optional, opens inline dialog
                        TextButton(
                            onClick = {
                                checkinTemp = carritoState.checkin
                                checkoutTemp = carritoState.checkout
                                errorFechas = null
                                mostrarFechasDialog = true
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = BEPrimaryLight)
                        ) {
                            if (carritoState.checkin.isNotBlank()) {
                                Text("📅 ${carritoState.checkin}  →  ${carritoState.checkout}  ✏️",
                                    style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("📅 Agregar fechas de viaje  →",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // ── Category pills
                item {
                    LazyRow(
                        modifier = Modifier.background(BESurface).padding(vertical = 10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(CATEGORIAS) { cat ->
                            val sel = cat == catActiva
                            FilterChip(
                                selected = sel,
                                onClick = { catActiva = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(99.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BEPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = BESurfaceVar,
                                    labelColor = BETextSecond
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true, selected = sel,
                                    borderColor = BEBorder, selectedBorderColor = BEPrimary
                                )
                            )
                        }
                    }
                    HorizontalDivider(color = BEBorder)
                }

                // ── Result count
                item {
                    if (!cargando && !fetchError)
                        Text(
                            "${filtrados.size} experiencia${if (filtrados.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall, color = BETextMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                }

                // ── Error state
                if (fetchError) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚠️", style = MaterialTheme.typography.displayMedium)
                                Spacer(Modifier.height(8.dp))
                                Text("Error al cargar experiencias", color = BEError,
                                    style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }

                // ── Skeleton / results
                if (cargando) {
                    items(3) {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BESurface)) {
                            Column {
                                Box(Modifier.fillMaxWidth().height(180.dp).background(BESurfaceVar))
                                Column(Modifier.padding(12.dp)) {
                                    Box(Modifier.fillMaxWidth(0.7f).height(14.dp).background(BESurfaceVar, RoundedCornerShape(4.dp)))
                                    Spacer(Modifier.height(8.dp))
                                    Box(Modifier.fillMaxWidth(0.45f).height(12.dp).background(BESurfaceVar, RoundedCornerShape(4.dp)))
                                }
                            }
                        }
                    }
                } else if (!fetchError && filtrados.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔍", style = MaterialTheme.typography.displayMedium)
                                Spacer(Modifier.height(8.dp))
                                Text("Sin resultados", style = MaterialTheme.typography.titleSmall,
                                    color = BETextSecond)
                                TextButton(onClick = { busqueda = ""; catActiva = "Todas" }) {
                                    Text("Limpiar filtros", color = BEPrimary)
                                }
                            }
                        }
                    }
                } else {
                    items(filtrados) { s ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            ServicioCard(
                                servicio = s,
                                enCarrito = carritoVm.estaEnCarrito(s.id),
                                esFavorito = s.id in favoritoIds,
                                onFavoritoClick = {
                                    if (user != null) {
                                        scope.launch {
                                            val esAhora = FirestoreRepository.toggleFavorito(user.uid, s.id)
                                            favoritoIds = if (esAhora) favoritoIds + s.id else favoritoIds - s.id
                                        }
                                    }
                                },
                                onClick = { navController.navigate("detalle/${s.id}") }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarCarrito) {
        CarritoPanel(carritoVm = carritoVm, navController = navController, onClose = { mostrarCarrito = false })
    }

    // ── Date picker dialog
    if (mostrarFechasDialog) {
        AlertDialog(
            onDismissRequest = { mostrarFechasDialog = false },
            containerColor = BESurface,
            title = { Text("¿Cuándo viajás?", fontWeight = FontWeight.Bold, color = BETextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = checkinTemp, onValueChange = { checkinTemp = it; errorFechas = null },
                        label = { Text("Llegada (yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder)
                    )
                    OutlinedTextField(
                        value = checkoutTemp, onValueChange = { checkoutTemp = it; errorFechas = null },
                        label = { Text("Salida (yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder)
                    )
                    errorFechas?.let {
                        Text(it, color = BEError, style = MaterialTheme.typography.bodySmall)
                    }
                    if (carritoState.checkin.isNotBlank()) {
                        TextButton(
                            onClick = { carritoVm.setFechas("", ""); mostrarFechasDialog = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = BETextMuted)
                        ) { Text("Quitar fechas", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when {
                            checkinTemp.isBlank() -> errorFechas = "Ingresá la fecha de llegada."
                            checkoutTemp.isBlank() -> errorFechas = "Ingresá la fecha de salida."
                            checkoutTemp <= checkinTemp -> errorFechas = "La salida debe ser posterior a la llegada."
                            else -> { carritoVm.setFechas(checkinTemp, checkoutTemp); mostrarFechasDialog = false }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                ) { Text("Aplicar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarFechasDialog = false }) {
                    Text("Cancelar", color = BETextSecond)
                }
            }
        )
    }
}

// ── Carrito panel
@Composable
fun CarritoPanel(carritoVm: CarritoViewModel, navController: NavController, onClose: () -> Unit) {
    val state by carritoVm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val user = FirebaseAuth.getInstance().currentUser

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = BESurface,
        title = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Mi carrito", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = BETextPrimary)
                if (state.items.isNotEmpty())
                    TextButton(onClick = { carritoVm.limpiar() }) { Text("Vaciar", color = BEError) }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.items.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🧳", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Tu carrito está vacío", color = BETextSecond,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    if (state.checkin.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = BEPrimary.copy(0.08f)) {
                            Text("📅 ${state.checkin}  →  ${state.checkout}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall, color = BEPrimaryDark,
                                fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    state.items.forEach { item ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = BESurfaceVar)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold, maxLines = 1, color = BETextPrimary)
                                    if (item.precio > 0) {
                                        Text("${item.precioFormateado} × ${item.personas} = ${item.subtotalFormateado}",
                                            style = MaterialTheme.typography.bodySmall, color = BEPrimary)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { carritoVm.actualizarPersonas(item.servicioId, item.personas - 1) },
                                        modifier = Modifier.size(28.dp)) {
                                        Text("−", color = BEPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Text("${item.personas}", style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                    IconButton(onClick = { carritoVm.actualizarPersonas(item.servicioId, item.personas + 1) },
                                        modifier = Modifier.size(28.dp)) {
                                        Text("+", color = BEPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(onClick = { carritoVm.quitarItem(item.servicioId) },
                                        modifier = Modifier.size(28.dp)) {
                                        Text("✕", color = BEError, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = BEBorder)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total estimado", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = BETextPrimary)
                        Text("${"$"}${"%,.0f".format(carritoVm.total).replace(",", ".")}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = BEPrimary)
                    }
                    error?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = BEError, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (state.items.isNotEmpty()) {
                Button(
                    onClick = {
                        if (user == null) { error = "Iniciá sesión para confirmar."; return@Button }
                        if (state.checkin.isBlank()) { error = "Agregá las fechas de viaje antes de confirmar."; return@Button }
                        guardando = true; error = null
                        scope.launch {
                            val serviciosMap = state.items.map { i ->
                                mapOf("id" to i.servicioId, "title" to i.title, "image" to i.image,
                                    "price" to i.precio, "personas" to i.personas,
                                    "lat" to i.lat, "lng" to i.lng)
                            }
                            val id = FirestoreRepository.crearReserva(
                                userId = user.uid, userEmail = user.email ?: "",
                                fullname = user.displayName ?: user.email ?: "",
                                checkin = state.checkin, checkout = state.checkout,
                                servicios = serviciosMap,
                                personas = state.items.sumOf { it.personas },
                                total = carritoVm.total
                            )
                            guardando = false
                            if (id != null) {
                                carritoVm.limpiar(); onClose()
                                navController.navigate(Screen.ReservaExitosa.route)
                            } else error = "Error al confirmar. Intentá de nuevo."
                        }
                    },
                    enabled = !guardando,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                ) {
                    if (guardando) CircularProgressIndicator(color = Color.White,
                        modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Confirmar reserva →", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cerrar", color = BETextSecond) } }
    )
}

// ── MainScreen
@Composable
fun MainScreen(navController: NavController, carritoVm: CarritoViewModel = viewModel()) {
    CatalogoScreen(navController = navController, carritoVm = carritoVm)
}
