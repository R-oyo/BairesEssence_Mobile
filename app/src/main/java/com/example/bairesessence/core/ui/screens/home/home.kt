package com.example.bairesessence.core.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.bairesessence.R
import com.example.bairesessence.core.ui.navigation.Screen
import androidx.compose.material3.ExperimentalMaterial3Api

// -------------------------
// Scaffold reutilizable con bottom bar + FAB opcional
// -------------------------
@Composable
private fun AppScaffold(
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

// -------------------------
// HOME
// -------------------------
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
private data class ExperienceItem(
    val title: String,
    val subtitle: String
)

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
// ITINERARIO – timeline interactivo con presets + editar/eliminar
// =======================================================
private data class ItineraryActivity(
    val time: String,
    val title: String,
    val description: String,
    val isPreset: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(navController: NavHostController) {
    val activities = remember {
        mutableStateListOf(
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

    val presetOptions = listOf(
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

    var showDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    var time by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Estado del dropdown
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedPresetLabel by remember { mutableStateOf("Actividad personalizada") }

    fun resetForm() {
        time = ""
        title = ""
        description = ""
        editingIndex = null
        selectedPresetLabel = "Actividad personalizada"
        dropdownExpanded = false
    }

    fun startCreate() {
        resetForm()
        showDialog = true
    }

    fun startEdit(index: Int) {
        val act = activities[index]
        time = act.time
        title = act.title
        description = act.description
        editingIndex = index

        val presetMatch = presetOptions.firstOrNull { it.title == act.title }
        selectedPresetLabel = presetMatch?.title ?: "Actividad personalizada"

        showDialog = true
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(innerPadding)
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
                        onClick = { activities.add(preset) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val sortedActivities = activities.sortedBy { it.time }

            if (sortedActivities.isEmpty()) {
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    sortedActivities.forEachIndexed { visualIndex, activity ->
                        val realIndex = activities.indexOf(activity)
                        ActivityTimelineItem(
                            activity = activity,
                            isLast = visualIndex == sortedActivities.lastIndex,
                            onEdit = { if (realIndex >= 0) startEdit(realIndex) },
                            onDelete = { if (realIndex >= 0) activities.removeAt(realIndex) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                resetForm()
            },
            title = {
                Text(
                    text = if (editingIndex == null)
                        "Agregar actividad"
                    else
                        "Editar actividad"
                )
            },
            text = {
                Column {
                    // ---------- Campo "Servicio sugerido" con dropdown propio ----------
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
                        if (title.isNotBlank() || selectedPresetLabel != "Actividad personalizada") {
                            if (selectedPresetLabel != "Actividad personalizada" &&
                                title.isBlank()
                            ) {
                                val preset = presetOptions.firstOrNull {
                                    it.title == selectedPresetLabel
                                }
                                if (preset != null) {
                                    title = preset.title
                                    description = preset.description
                                }
                            }

                            val finalTitle = if (title.isBlank()) "Actividad" else title
                            val finalDescription = description
                            val finalIsPreset = selectedPresetLabel != "Actividad personalizada"

                            if (editingIndex == null) {
                                activities.add(
                                    ItineraryActivity(
                                        time = if (time.isBlank()) "--:--" else time,
                                        title = finalTitle,
                                        description = finalDescription,
                                        isPreset = finalIsPreset
                                    )
                                )
                            } else {
                                val idx = editingIndex!!
                                activities[idx] = activities[idx].copy(
                                    time = if (time.isBlank()) "--:--" else time,
                                    title = finalTitle,
                                    description = finalDescription,
                                    isPreset = finalIsPreset
                                )
                            }
                        }
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

// -------------------------
// Resto de pestañas (simple)
// -------------------------
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
fun ProfileScreen(navController: NavHostController) {
    AppScaffold(
        navController = navController,
        currentRoute = Screen.Profile.route
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Perfil del usuario (nombre, preferencias, idioma, etc.)",
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
