package com.example.bairesessence.core.ui.screens.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.bairesessence.R
import com.example.bairesessence.core.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.net.URLEncoder

private const val WHATSAPP_PHONE = "5491171284739"

data class ItineraryActivity(
    val id: String = "",
    val time: String = "",
    val title: String = "",
    val description: String = "",
    val isPreset: Boolean = false
)

private data class ExperienceItem(
    val title: String,
    val subtitle: String
)

// =======================================================
// Scaffold reutilizable con bottom bar + FAB
// =======================================================
@Composable
fun AppScaffold(
    navController: NavHostController,
    currentRoute: String,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val items = listOf(
        Screen.Home,
        Screen.Itinerary,
        Screen.Payments,
        Screen.Profile,
        Screen.Guests
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val route = navBackStackEntry?.destination?.route ?: currentRoute

                items.forEach { screen ->
                    val selected = route == screen.route

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            when (screen) {
                                Screen.Home -> Icon(Icons.Default.Home, contentDescription = "Home")
                                Screen.Itinerary -> Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Itinerario"
                                )

                                Screen.Payments -> Icon(
                                    Icons.Default.CreditCard,
                                    contentDescription = "Pagos"
                                )

                                Screen.Profile -> Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Perfil"
                                )

                                Screen.Guests -> Icon(
                                    Icons.Default.GroupAdd,
                                    contentDescription = "Invitados"
                                )

                                else -> {}
                            }
                        },
                        label = {
                            Text(
                                text = when (screen) {
                                    Screen.Home -> "Home"
                                    Screen.Itinerary -> "Itinerario"
                                    Screen.Payments -> "Pagos"
                                    Screen.Profile -> "Perfil"
                                    Screen.Guests -> "Invitados"
                                    else -> ""
                                }
                            )
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            floatingActionButton?.invoke()
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

// =======================================================
// HOME
// =======================================================
@Composable
fun HomeScreen(navController: NavHostController) {
    AppScaffold(
        navController = navController,
        currentRoute = Screen.Home.route
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            HomeTopBar()

            Spacer(modifier = Modifier.height(16.dp))

            FeaturedCityTourCard()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "¿Por qué CABA?",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Un city tour por Buenos Aires concentra historia, arquitectura y la esencia porteña: Obelisco, Plaza de Mayo, Caminito, Puerto Madero y más.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Otras experiencias para tu viaje",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExperiencesRow()
        }
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "BE", style = MaterialTheme.typography.labelMedium)
        }

        Text(
            text = "BAIRES ESSENCE",
            style = MaterialTheme.typography.titleMedium
        )

        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Ubicación",
            tint = Color.Black
        )
    }
}

@Composable
private fun FeaturedCityTourCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
    ) {
        Image(
            painter = painterResource(id = R.drawable.baires_background),
            contentDescription = "City Tour",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Buenos Aires • 20°C",   // por ahora fijo
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Text(
                    text = "City Tour",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Desde USD 70",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF9CFF3C))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ver detalles",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// -------------------------
// Otras experiencias
// -------------------------
@Composable
private fun ExperiencesRow() {
    val items = listOf(
        ExperienceItem("Traslado desde aeropuerto", "Pick-up Ezeiza o Aeroparque"),
        ExperienceItem("Delta del Tigre + Navegación", "Día completo"),
        ExperienceItem("Noche de tango con cena", "Show + cena incluida")
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            ExperienceCard(item)
        }
    }
}

@Composable
private fun ExperienceCard(item: ExperienceItem) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.baires_background),
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

// =======================================================
// ITINERARIO – sincronizado con Firestore + acompañantes
// =======================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid

    // Lista que viene de Firestore (SnapshotStateList)
    val activities = remember { mutableStateListOf<ItineraryActivity>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBooking by remember { mutableStateOf(false) }

    // Presets que el usuario puede agregar rápido
    val presetOptions = remember {
        listOf(
            ItineraryActivity(
                time = "10:00",
                title = "Paseo por Recoleta",
                description = "Cementerio de la Recoleta + cafés y librerías.",
                isPreset = true
            ),
            ItineraryActivity(
                time = "14:00",
                title = "Delta del Tigre + Navegación",
                description = "Salida desde CABA, paseo en lancha por el Delta.",
                isPreset = true
            ),
            ItineraryActivity(
                time = "20:30",
                title = "Noche de tango con cena",
                description = "Show de tango con cena incluida.",
                isPreset = true
            )
        )
    }

    // Seed inicial (las 2 actividades base)
    val initialSeed = remember {
        listOf(
            ItineraryActivity(
                time = "08:30",
                title = "Llegada a Ezeiza",
                description = "Traslado privado al hotel en CABA.",
                isPreset = true
            ),
            ItineraryActivity(
                time = "16:00",
                title = "City Tour clásico",
                description = "Obelisco, Plaza de Mayo, Caminito y Puerto Madero.",
                isPreset = true
            )
        )
    }
    var hasSeeded by remember { mutableStateOf(false) }

    // Escucha en tiempo real de Firestore
    DisposableEffect(userId) {
        if (userId == null) {
            isLoading = false
            errorMessage = "Iniciá sesión para guardar tu itinerario."
            onDispose { }
        } else {
            val ref = firestore
                .collection("users")
                .document(userId)
                .collection("itinerary")

            val registration = ref.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage = "Error al cargar itinerario."
                    isLoading = false
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents ?: emptyList()

                if (docs.isEmpty() && !hasSeeded) {
                    // Primer ingreso del usuario: sembramos actividades iniciales
                    hasSeeded = true
                    initialSeed.forEach { act ->
                        ref.add(act)
                    }
                } else {
                    val newList = docs.mapNotNull { doc ->
                        doc.toObject(ItineraryActivity::class.java)?.copy(id = doc.id)
                    }.sortedBy { it.time }

                    activities.clear()
                    activities.addAll(newList)

                    isLoading = false
                    errorMessage = null
                }
            }

            onDispose { registration.remove() }
        }
    }

    // Estados para el diálogo agregar/editar actividad
    var showDialog by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<ItineraryActivity?>(null) }

    var time by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedPresetLabel by remember { mutableStateOf("Actividad personalizada") }

    // -------- Reserva: acompañantes + popup confirmación --------
    var showPassengersDialog by remember { mutableStateOf(false) }
    var companionsInput by remember { mutableStateOf("0") }
    var showReservationConfirmation by remember { mutableStateOf(false) }

    fun resetForm() {
        time = ""
        title = ""
        description = ""
        editingActivity = null
        selectedPresetLabel = "Actividad personalizada"
        dropdownExpanded = false
    }

    fun startCreate() {
        resetForm()
        showDialog = true
    }

    fun startEdit(activity: ItineraryActivity) {
        editingActivity = activity
        time = activity.time
        title = activity.title
        description = activity.description

        val presetMatch = presetOptions.firstOrNull { it.title == activity.title }
        selectedPresetLabel = presetMatch?.title ?: "Actividad personalizada"

        showDialog = true
    }

    fun saveActivity() {
        if (userId == null) return

        val ref = firestore
            .collection("users")
            .document(userId)
            .collection("itinerary")

        // Si viene de preset y el usuario no tocó el título, rellenamos
        if (selectedPresetLabel != "Actividad personalizada" && title.isBlank()) {
            val preset = presetOptions.firstOrNull { it.title == selectedPresetLabel }
            if (preset != null) {
                title = preset.title
                description = preset.description
            }
        }

        val finalTitle = if (title.isBlank()) "Actividad" else title
        val finalDescription = description
        val finalIsPreset = selectedPresetLabel != "Actividad personalizada"
        val finalTime = if (time.isBlank()) "--:--" else time

        val currentId = editingActivity?.id
        val activity = ItineraryActivity(
            id = currentId ?: "",
            time = finalTime,
            title = finalTitle,
            description = finalDescription,
            isPreset = finalIsPreset
        )

        if (currentId.isNullOrBlank()) {
            // Nueva actividad
            ref.add(activity)
        } else {
            // Actualizar existente
            ref.document(currentId).set(activity)
        }
    }

    fun deleteActivity(activity: ItineraryActivity) {
        if (userId == null || activity.id.isBlank()) return
        firestore
            .collection("users")
            .document(userId)
            .collection("itinerary")
            .document(activity.id)
            .delete()
    }

    fun openWhatsAppWithItinerary(
        reservationId: String,
        companions: Int,
        totalPassengers: Int
    ) {
        val userEmail = auth.currentUser?.email ?: "sin-email"

        val message = buildString {
            append("Nueva reserva desde la app Baires Essence.\n\n")
            append("Reserva ID: $reservationId\n")
            append("Email del titular: $userEmail\n")
            append("Pasajeros: $totalPassengers (titular + $companions acompañantes)\n\n")
            append("Itinerario propuesto:\n")
            activities.forEach { act ->
                append("• ${act.time} - ${act.title}\n")
                if (act.description.isNotBlank()) {
                    append("  ${act.description}\n")
                }
            }
        }

        val encoded = URLEncoder.encode(message, "UTF-8")
        val url = "https://wa.me/$WHATSAPP_PHONE?text=$encoded"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        try {
            intent.setPackage("com.whatsapp")
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }
    }

    fun createReservation(companions: Int) {
        if (userId == null) {
            errorMessage = "Iniciá sesión para poder reservar."
            return
        }
        if (activities.isEmpty()) {
            errorMessage = "Agregá al menos una actividad antes de reservar."
            return
        }

        isBooking = true
        errorMessage = null

        val userEmail = auth.currentUser?.email ?: ""
        val totalPassengers = 1 + companions

        val reservationData = hashMapOf(
            "userId" to userId,
            "userEmail" to userEmail,
            "timestamp" to FieldValue.serverTimestamp(),
            "estado" to "pendiente",
            "companions" to companions,
            "totalPassengers" to totalPassengers,
            "actividades" to activities.map { act ->
                mapOf(
                    "time" to act.time,
                    "title" to act.title,
                    "description" to act.description,
                    "isPreset" to act.isPreset
                )
            }
        )

        firestore.collection("reservas")
            .add(reservationData)
            .addOnSuccessListener { doc ->
                isBooking = false
                showReservationConfirmation = true
                openWhatsAppWithItinerary(
                    reservationId = doc.id,
                    companions = companions,
                    totalPassengers = totalPassengers
                )
            }
            .addOnFailureListener {
                isBooking = false
                errorMessage = "No se pudo crear la reserva. Intentá de nuevo."
            }
    }

    fun onReserveClick() {
        if (userId == null) {
            errorMessage = "Iniciá sesión para poder reservar."
            return
        }
        if (activities.isEmpty()) {
            errorMessage = "Agregá al menos una actividad antes de reservar."
            return
        }
        errorMessage = null
        companionsInput = "0"
        showPassengersDialog = true
    }

    AppScaffold(
        navController = navController,
        currentRoute = Screen.Itinerary.route,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { startCreate() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar actividad")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Itinerario de tu día",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Armá tu día en Buenos Aires combinando nuestras sugerencias con tus propios planes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                } else if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cargando itinerario...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Agregar rápido (presets Baires Essence)",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetOptions) { preset ->
                        PresetChip(
                            label = preset.title,
                            onClick = {
                                if (userId != null) {
                                    firestore
                                        .collection("users")
                                        .document(userId)
                                        .collection("itinerary")
                                        .add(
                                            preset.copy(
                                                id = "",
                                                time = preset.time.ifBlank { "--:--" }
                                            )
                                        )
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (!isLoading && activities.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Todavía no agregaste actividades. Usá el botón + para empezar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 96.dp) // espacio para el botón
                    ) {
                        items(activities.size) { index ->
                            val activity = activities[index]
                            ActivityTimelineItem(
                                activity = activity,
                                isLast = index == activities.lastIndex,
                                onEdit = { startEdit(activity) },
                                onDelete = { deleteActivity(activity) }
                            )
                        }
                    }
                }
            }

            // Botón "Reservar ahora" fijo abajo
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = { if (!isBooking) onReserveClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9CFF3C),
                        contentColor = Color.Black
                    ),
                    enabled = !isBooking
                ) {
                    Text(
                        text = if (isBooking) "Enviando reserva..." else "Reservar ahora",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    // Diálogo para cantidad de acompañantes
    if (showPassengersDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isBooking) {
                    showPassengersDialog = false
                }
            },
            title = {
                Text("¿Cuántos acompañantes viajan con vos?")
            },
            text = {
                Column {
                    Text(
                        text = "Podés poner 0 si viajás solo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = companionsInput,
                        onValueChange = { new ->
                            if (new.all { it.isDigit() } && new.length <= 2) {
                                companionsInput = new
                            }
                        },
                        label = { Text("Cantidad de acompañantes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val companions = companionsInput.toIntOrNull() ?: 0
                        showPassengersDialog = false
                        createReservation(companions)
                    },
                    enabled = companionsInput.toIntOrNull() != null && !isBooking
                ) {
                    Text("Confirmar reserva")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isBooking) {
                            showPassengersDialog = false
                        }
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Popup de confirmación de reserva
    if (showReservationConfirmation) {
        AlertDialog(
            onDismissRequest = { showReservationConfirmation = false },
            title = {
                Text("Reserva enviada")
            },
            text = {
                Text(
                    "Tu reserva fue creada correctamente y se envió un mensaje por WhatsApp a Baires Essence. " +
                            "Nos vamos a contactar con vos para confirmar los detalles."
                )
            },
            confirmButton = {
                TextButton(onClick = { showReservationConfirmation = false }) {
                    Text("Aceptar")
                }
            }
        )
    }

    // Diálogo agregar / editar actividad
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                resetForm()
            },
            title = {
                Text(
                    text = if (editingActivity == null)
                        "Agregar actividad"
                    else
                        "Editar actividad"
                )
            },
            text = {
                Column {
                    // Campo "Servicio sugerido" con dropdown
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Servicio sugerido",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedPresetLabel,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Actividad personalizada") },
                                onClick = {
                                    selectedPresetLabel = "Actividad personalizada"
                                    dropdownExpanded = false
                                }
                            )

                            presetOptions.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.title) },
                                    onClick = {
                                        selectedPresetLabel = preset.title
                                        title = preset.title
                                        description = preset.description
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Hora (ej: 10:30)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nombre de la actividad") },
                        enabled = selectedPresetLabel == "Actividad personalizada",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detalle / punto de encuentro") },
                        enabled = selectedPresetLabel == "Actividad personalizada",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveActivity()
                        showDialog = false
                        resetForm()
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        resetForm()
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun ActivityTimelineItem(
    activity: ItineraryActivity,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (activity.isPreset) Color(0xFF9CFF3C) else Color(0xFF4CAF50)
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                        .background(Color.LightGray)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(12.dp)
        ) {
            Text(
                text = "${activity.time} • ${activity.title}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = activity.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (activity.isPreset)
                    "Recomendado por Baires Essence"
                else
                    "Actividad personalizada",
                style = MaterialTheme.typography.labelSmall,
                color = if (activity.isPreset) Color(0xFF9C27B0) else Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onEdit) {
                    Text("Editar")
                }
                TextButton(onClick = onDelete) {
                    Text("Eliminar")
                }
            }
        }
    }
}

// =======================================================
// Resto de pestañas (placeholder simples)
// =======================================================
@Composable
fun PaymentMethodsScreen(navController: NavHostController) {
    AppScaffold(
        navController = navController,
        currentRoute = Screen.Payments.route
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Métodos de pago (tarjeta, efectivo, transferencias)",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun GuestsScreen(navController: NavHostController) {
    AppScaffold(
        navController = navController,
        currentRoute = Screen.Guests.route
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Invitados al viaje (agregar acompañantes)",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
