package com.example.bairesessence.core.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.bairesessence.core.navigation.Screen
import com.example.bairesessence.core.ui.components.BottomBar
import com.example.bairesessence.core.ui.components.DateRangePickerDialog
import com.example.bairesessence.core.ui.components.ServicioCard
import com.example.bairesessence.core.ui.screens.carrito.CarritoViewModel
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.example.bairesessence.data.model.Servicio
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// (clave Firestore, título con emoji para el carrusel)
private val CATEGORIAS = listOf(
    "Tours"         to "🗺️  Tours",
    "Gastronomia"   to "🍽️  Gastronomía",
    "Traslados"     to "🚌  Traslados",
    "Experiencias"  to "🎭  Experiencias"
)

@Composable
fun CatalogoScreen(
    navController: NavController,
    carritoVm: CarritoViewModel,
    homeVm: HomeViewModel
) {
    val carritoState by carritoVm.state.collectAsState()
    val servicios by homeVm.servicios.collectAsState()
    val cargando by homeVm.cargando.collectAsState()
    val fetchError by homeVm.fetchError.collectAsState()
    val favoritoIds by homeVm.favoritoIds.collectAsState()
    var busqueda by remember { mutableStateOf("") }
    var mostrarCarrito by remember { mutableStateOf(false) }

    var mostrarFechasDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val user = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(user?.uid) {
        homeVm.cargar(user?.uid)
    }

    // Filtra por fechas (se aplica siempre, tanto al carrusel como a búsqueda)
    val porFecha = servicios.filter { s ->
        val ci = carritoState.checkin; val co = carritoState.checkout
        if (ci.isBlank() || co.isBlank() || (s.from == null && s.until == null)) return@filter true
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val userIn  = runCatching { fmt.parse(ci)  }.getOrNull() ?: return@filter true
        val userOut = runCatching { fmt.parse(co) }.getOrNull() ?: return@filter true
        val svcFrom = s.from?.toDate(); val svcUntil = s.until?.toDate()
        when {
            svcFrom != null && svcUntil != null -> !userOut.before(svcFrom) && !userIn.after(svcUntil)
            svcFrom != null -> !userOut.before(svcFrom)
            svcUntil != null -> !userIn.after(svcUntil)
            else -> true
        }
    }
    // Resultados de búsqueda textual
    val resultados = if (busqueda.isNotBlank())
        porFecha.filter { it.title.contains(busqueda, true) || it.description.contains(busqueda, true) }
    else emptyList()
    // Secciones del carrusel agrupadas por categoría
    val seccionesCarrusel = CATEGORIAS.mapNotNull { (key, titulo) ->
        val svcs = porFecha.filter { it.categoria.equals(key, true) }
        if (svcs.isNotEmpty()) Pair(titulo, svcs) else null
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
                                IconButton(onClick = { navController.navigate(Screen.Mapa.route) }) {
                                    Icon(Icons.Default.Map, "Mapa", tint = Color.White)
                                }
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
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
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
                            onClick = { mostrarFechasDialog = true },
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

                // ── Skeleton mientras carga
                if (cargando) {
                    items(3) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BESurface)
                        ) {
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
                } else if (fetchError) {
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
                } else if (busqueda.isNotBlank()) {
                    // ── Resultados de búsqueda (lista vertical)
                    item {
                        Text(
                            "${resultados.size} resultado${if (resultados.size != 1) "s" else ""} para \"$busqueda\"",
                            style = MaterialTheme.typography.bodySmall, color = BETextMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    if (resultados.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔍", style = MaterialTheme.typography.displayMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Sin resultados", style = MaterialTheme.typography.titleSmall, color = BETextSecond)
                                    TextButton(onClick = { busqueda = "" }) {
                                        Text("Limpiar búsqueda", color = BEPrimary)
                                    }
                                }
                            }
                        }
                    } else {
                        items(resultados) { s ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                ServicioCard(
                                    servicio = s,
                                    enCarrito = carritoVm.estaEnCarrito(s.id),
                                    esFavorito = s.id in favoritoIds,
                                    onFavoritoClick = { if (user != null) homeVm.toggleFavorito(user.uid, s.id) },
                                    onClick = { navController.navigate("detalle/${s.id}") }
                                )
                            }
                        }
                    }
                } else {
                    // ── Carrusel por categoría (modo descubrimiento)
                    if (seccionesCarrusel.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📅", style = MaterialTheme.typography.displayMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text("No hay experiencias en esas fechas",
                                        style = MaterialTheme.typography.titleSmall, color = BETextSecond)
                                    TextButton(onClick = { carritoVm.setFechas("", "") }) {
                                        Text("Quitar filtro de fechas", color = BEPrimary)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "✨  Paquetes especiales",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BETextPrimary
                                )
                                TextButton(onClick = { navController.navigate(Screen.Paquetes.route) }) {
                                    Text("Ver todos →", color = BEPrimary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        items(seccionesCarrusel) { (titulo, svcs) ->
                            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                Text(
                                    titulo,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BETextPrimary,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(svcs) { s ->
                                        ServicioCard(
                                            servicio = s,
                                            enCarrito = carritoVm.estaEnCarrito(s.id),
                                            esFavorito = s.id in favoritoIds,
                                            onFavoritoClick = { if (user != null) homeVm.toggleFavorito(user.uid, s.id) },
                                            onClick = { navController.navigate("detalle/${s.id}") },
                                            modifier = Modifier.width(220.dp)
                                        )
                                    }
                                }
                            }
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
        DateRangePickerDialog(
            initialCheckin = carritoState.checkin,
            initialCheckout = carritoState.checkout,
            onConfirm = { ci, co ->
                carritoVm.setFechas(ci, co)
                mostrarFechasDialog = false
            },
            onDismiss = { mostrarFechasDialog = false },
            onClearDates = if (carritoState.checkin.isNotBlank()) {
                { carritoVm.setFechas("", ""); mostrarFechasDialog = false }
            } else null
        )
    }
}

// ── Carrito panel (2 pasos: carrito → grupo de viaje)
@Composable
fun CarritoPanel(carritoVm: CarritoViewModel, navController: NavController, onClose: () -> Unit) {
    val state by carritoVm.state.collectAsState()
    val scope = rememberCoroutineScope()
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val user = FirebaseAuth.getInstance().currentUser

    var paso by remember { mutableStateOf(0) }
    var viajaSolo by remember { mutableStateOf(true) }
    var compNombres by remember { mutableStateOf(listOf("")) }
    var compDnis    by remember { mutableStateOf(listOf("")) }

    AlertDialog(
        onDismissRequest = onClose,
        containerColor = BESurface,
        title = {
            if (paso == 0) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Mi carrito", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = BETextPrimary)
                    if (state.items.isNotEmpty())
                        TextButton(onClick = { carritoVm.limpiar() }) { Text("Vaciar", color = BEError) }
                }
            } else {
                Text("Grupo de viaje", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = BETextPrimary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (paso == 0) {
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
                } else {
                    // ── Paso 1: grupo de viaje
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()) {
                            RadioButton(selected = viajaSolo, onClick = { viajaSolo = true },
                                colors = RadioButtonDefaults.colors(selectedColor = BEPrimary))
                            Text("Viajo solo/a", style = MaterialTheme.typography.bodyMedium, color = BETextPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()) {
                            RadioButton(selected = !viajaSolo, onClick = { viajaSolo = false },
                                colors = RadioButtonDefaults.colors(selectedColor = BEPrimary))
                            Text("Viajo con acompañantes", style = MaterialTheme.typography.bodyMedium, color = BETextPrimary)
                        }
                        if (!viajaSolo) {
                            HorizontalDivider(color = BEBorder)
                            compNombres.forEachIndexed { i, nombre ->
                                Text("Acompañante ${i + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BETextMuted, fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = nombre,
                                    onValueChange = { v ->
                                        compNombres = compNombres.toMutableList().also { it[i] = v }
                                    },
                                    label = { Text("Nombre completo") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder)
                                )
                                OutlinedTextField(
                                    value = compDnis.getOrElse(i) { "" },
                                    onValueChange = { v ->
                                        compDnis = compDnis.toMutableList().also { it[i] = v }
                                    },
                                    label = { Text("DNI / Pasaporte") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder)
                                )
                                if (compNombres.size > 1) {
                                    TextButton(
                                        onClick = {
                                            compNombres = compNombres.toMutableList().also { it.removeAt(i) }
                                            compDnis    = compDnis.toMutableList().also { it.removeAt(i) }
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = BEError)
                                    ) { Text("Quitar", style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                            TextButton(
                                onClick = { compNombres = compNombres + ""; compDnis = compDnis + "" },
                                colors = ButtonDefaults.textButtonColors(contentColor = BEPrimary)
                            ) { Text("+ Agregar otro acompañante") }
                        }
                        error?.let {
                            Text(it, color = BEError, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (paso == 0) {
                if (state.items.isNotEmpty()) {
                    Button(
                        onClick = {
                            when {
                                user == null -> error = "Iniciá sesión para continuar."
                                state.checkin.isBlank() -> error = "Agregá las fechas de viaje antes de continuar."
                                else -> { error = null; paso = 1 }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                    ) { Text("Continuar →", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            } else {
                Button(
                    onClick = {
                        guardando = true; error = null
                        val acompMap = if (viajaSolo) emptyList() else
                            compNombres.mapIndexed { i, nombre ->
                                mapOf("nombre" to nombre.trim(), "dni" to compDnis.getOrElse(i) { "" }.trim())
                            }.filter { (it["nombre"] as? String).orEmpty().isNotBlank() }
                        scope.launch {
                            val serviciosMap = state.items.map { it2 ->
                                mapOf("id" to it2.servicioId, "title" to it2.title, "image" to it2.image,
                                    "price" to it2.precio, "personas" to it2.personas,
                                    "lat" to it2.lat, "lng" to it2.lng)
                            }
                            val id = FirestoreRepository.crearReserva(
                                userId = user!!.uid, userEmail = user.email ?: "",
                                fullname = user.displayName ?: user.email ?: "",
                                checkin = state.checkin, checkout = state.checkout,
                                servicios = serviciosMap,
                                personas = state.items.sumOf { it2 -> it2.personas },
                                total = carritoVm.total,
                                acompanantes = acompMap
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
        dismissButton = {
            if (paso == 0) {
                TextButton(onClick = onClose) { Text("Cerrar", color = BETextSecond) }
            } else {
                TextButton(onClick = { paso = 0; error = null }) { Text("← Volver", color = BETextSecond) }
            }
        }
    )
}

// ── MainScreen
@Composable
fun MainScreen(navController: NavController, carritoVm: CarritoViewModel = viewModel()) {
    val homeVm: HomeViewModel = viewModel()
    CatalogoScreen(navController = navController, carritoVm = carritoVm, homeVm = homeVm)
}
