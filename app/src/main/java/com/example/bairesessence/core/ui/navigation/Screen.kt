package com.example.bairesessence.core.ui.navigation

sealed class Screen(val route: String) {

    // Pantallas de autenticación
    object Landing : Screen("landing")
    object Login : Screen("login")
    object Register : Screen("register")

    // Home principal (primer pantalla post-login)
    object Home : Screen("home")

    // Pestañas internas / secciones dentro del home
    object Itinerary : Screen("itinerary")
    object Payments : Screen("payments")
    object Profile : Screen("profile")
    object Guests : Screen("guests")
}
