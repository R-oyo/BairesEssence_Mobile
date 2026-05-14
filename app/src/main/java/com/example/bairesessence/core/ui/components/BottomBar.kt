package com.example.bairesessence.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.bairesessence.core.ui.theme.*

data class NavItem(val route: String, val icon: ImageVector, val label: String)

@Composable
fun BottomBar(navController: NavController) {
    val items = listOf(
        NavItem("home",       Icons.Default.Home,      "Inicio"),
        NavItem("favoritos",  Icons.Default.Favorite,  "Favoritos"),
        NavItem("itinerary",  Icons.Default.DateRange, "Itinerario"),
        NavItem("pagos",      Icons.Default.ListAlt,   "Reservas"),
        NavItem("perfil",     Icons.Default.Person,    "Perfil"),
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(containerColor = BESurface, tonalElevation = 8.dp) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = BEPrimary,
                    unselectedIconColor = BETextMuted,
                    selectedTextColor   = BEPrimary,
                    unselectedTextColor = BETextMuted,
                    indicatorColor      = BEPrimary.copy(alpha = 0.12f)
                ),
                alwaysShowLabel = true
            )
        }
    }
}
