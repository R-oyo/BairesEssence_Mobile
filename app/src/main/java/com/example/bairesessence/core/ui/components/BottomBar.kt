package com.example.bairesessence.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

// Clase para definir cada ítem del menú inferior
data class NavItem(val route: String, val icon: ImageVector, val label: String)

@Composable
fun BottomBar(navController: NavController) {
    val items = listOf(
        NavItem("home", Icons.Default.Home, "Inicio"),
        NavItem("itinerary", Icons.Default.DateRange, "Itinerario"),
        NavItem("payment", Icons.Default.CreditCard, "Pagos"),
        NavItem("profile", Icons.Default.Person, "Perfil"),
        NavItem("settings", Icons.Default.Settings, "Ajustes")
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar(
        containerColor = Color.Black,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Evita apilar la misma pantalla varias veces
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = Color.White
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = Color.White
                    )
                },
                alwaysShowLabel = false
            )
        }
    }
}
