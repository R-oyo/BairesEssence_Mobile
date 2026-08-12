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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.tasks.await
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

// (clave Firestore, tÃ­tulo con emoji para el carrusel)
private val CATEGORIAS = listOf(
    "Tours"         to "ðŸ—ºï¸  Tours",
    "Gastronomia"   to "ðŸ½ï¸  GastronomÃ­a",
    "Traslados"     to "ðŸšŒ  Traslados",
    "Experiencias"  to "ðŸŽ­  Experiencias"
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var emailVerificado by remember { mutableStateOf(user?.isEmailVerified == true) }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    runCatching { FirebaseAuth.getInstance().currentUser?.reload()?.await() }
                    emailVerificado = FirebaseAuth.getInstance().currentUser?.isEmailVerified == true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(user?.uid) {
        homeVm.cargar(user?.uid)
    }

    // Filtra por fechas (se aplica siempre, tanto al carrusel como a bÃºsqueda)
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
    // Resultados de bÃºsqueda textual
    val resultados = if (busqueda.isNotBlank())
        porFecha.filter { it.title.contains(busqueda, true) || it.description.contains(busqueda, true) }
    else emptyList()
    // Secciones del carrusel agrupadas por categorÃ­a
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

                // â”€â”€ Banner verificaciÃ³n de email
                val authUser = FirebaseAuth.getInstance().currentUser
                val isGoogleUser = authUser?.providerData?.any { it.providerId == "google.com" } == true
                if (authUser != null && !emailVerificado && !isGoogleUser) {
                    item {
                        var dismissed by remember { mutableStateOf(false) }
                        var sent by remember { mutableStateOf(false) }
                        if (!dismissed) {
                            Surface(
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = androidx.compose.ui.graphics.Color(0xFFFFF3CD)
                            ) {
                                Row(
                                    modifier = androidx.compose.ui.Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                                        Text("VerificÃ¡ tu email", fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = androidx.compose.ui.graphics.Color(0xFF7D5700))
                                        Text(if (sent) "Mail enviado. RevisÃ¡ tu bandeja." else "Tu cuenta no estÃ¡ verificada.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = androidx.compose.ui.graphics.Color(0xFF7D5700))
                                    }
                                    if (!sent) {
                                        TextButton(onClick = { authUser.sendEmailVerification(); sent = true }) {
                                            Text("Reenviar", style = MaterialTheme.typography.bodySmall,
                                                color = androidx.compose.ui.graphics.Color(0xFF7D5700), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    TextButton(onClick = { dismissed = true }) {
                                        Text("âœ•", style = MaterialTheme.typography.bodySmall,
                                            color = androidx.compose.ui.graphics.Color(0xFF7D5700))
                                    }
                                }
                            }
                        }
                    }
                }

                // â”€â”€ Dark header
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

                        // Date chip â€” optional, opens inline dialog
                        TextButton(
                            onClick = { mostrarFechasDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = BEPrimaryLight)
                        ) {
                            if (carritoState.checkin.isNotBlank()) {
                                Text("ðŸ“… ${carritoState.checkin}  â†’  ${carritoState.checkout}  âœï¸",
                                    style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("ðŸ“… Agregar fechas de viaje  â†’",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // â”€â”€ Skeleton mientras carga
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
                                Text("âš ï¸", style = MaterialTheme.typography.displayMedium)
                                Spacer(Modifier.height(8.dp))
                                Text("Error al cargar experiencias", color = BEError,
                                    style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                } else if (busqueda.isNotBlank()) {
                    // â”€â”€ Resultados de bÃºsqueda (lista vertical)
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
                                    Text("ðŸ”", style = MaterialTheme.typography.displayMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Sin resultados", style = MaterialTheme.typography.titleSmall, color = BETextSecond)
                                    TextButton(onClick = { busqueda = "" }) {
                                        Text("Limpiar bÃºsqueda", color = BEPrimary)
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
                    // â”€â”€ Carrusel por categorÃ­a (modo descubrimiento)
                    if (seccionesCarrusel.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ðŸ“…", style = MaterialTheme.typography.displayMedium)
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
                                    "âœ¨  Paquetes especiales",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BETextPrimary
                                )
                                TextButton(onClick = { navController.navigate(Screen.Paquetes.route) }) {
                                    Text("Ver todos â†’", color = BEPrimary, style = MaterialTheme.typography.bodySmall)
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

    // â”€â”€ Date picker dialog
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

// â”€â”€ MainScreen
@Composable
fun MainScreen(navController: NavController, carritoVm: CarritoViewModel = viewModel()) {
    val homeVm: HomeViewModel = viewModel()
    CatalogoScreen(navController = navController, carritoVm = carritoVm, homeVm = homeVm)
}
