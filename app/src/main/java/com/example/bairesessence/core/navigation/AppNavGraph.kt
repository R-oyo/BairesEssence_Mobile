package com.example.bairesessence.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bairesessence.core.ui.screens.carrito.CarritoViewModel
import com.example.bairesessence.core.ui.screens.detalle.DetalleScreen
import com.example.bairesessence.core.ui.screens.favoritos.FavoritosScreen
import com.example.bairesessence.core.ui.screens.home.MainScreen
import com.example.bairesessence.core.ui.screens.itinerary.ItineraryScreen
import com.example.bairesessence.core.ui.screens.landing.LandingScreen
import com.example.bairesessence.core.ui.screens.login.BairesEssenceLogin
import com.example.bairesessence.core.ui.screens.pagos.PagoDetalleScreen
import com.example.bairesessence.core.ui.screens.pagos.PagosScreen
import com.example.bairesessence.core.ui.screens.perfil.PerfilScreen
import com.example.bairesessence.core.ui.screens.register.BairesEssenceRegister
import com.example.bairesessence.core.ui.screens.reservas.MisReservasScreen
import com.example.bairesessence.core.ui.screens.reservas.ReservaExitosaScreen
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String) {
    object Landing        : Screen("landing")
    object Login          : Screen("login")
    object Register       : Screen("register")
    object Home           : Screen("home")
    object Favoritos      : Screen("favoritos")
    object Perfil         : Screen("perfil")
    object MisReservas    : Screen("mis_reservas")
    object ReservaExitosa : Screen("reserva_exitosa")
    object Detalle        : Screen("detalle/{servicioId}")
    object Itinerary      : Screen("itinerary")
    object Pagos          : Screen("pagos")
    object PagoDetalle    : Screen("pago_detalle/{reservaId}")
}

private val AUTH_ROUTES = setOf(Screen.Landing.route, Screen.Login.route, Screen.Register.route)
private val PROTECTED_ROUTES = setOf(
    Screen.Home.route, Screen.Favoritos.route, Screen.Perfil.route,
    Screen.MisReservas.route, Screen.ReservaExitosa.route,
    Screen.Itinerary.route, Screen.Pagos.route, "pago_detalle/{reservaId}"
)

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val carritoVm: CarritoViewModel = viewModel()

    val auth = FirebaseAuth.getInstance()
    val start = if (auth.currentUser != null) Screen.Home.route else Screen.Landing.route

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null &&
                navController.currentDestination?.route in PROTECTED_ROUTES
            ) {
                navController.navigate(Screen.Landing.route) { popUpTo(0) { inclusive = true } }
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    NavHost(navController = navController, startDestination = start) {

        // ── Auth
        composable(Screen.Landing.route)  { LandingScreen(navController) }
        composable(Screen.Login.route)    { BairesEssenceLogin(navController) }
        composable(Screen.Register.route) { BairesEssenceRegister(navController) }

        // ── App principal
        composable(Screen.Home.route) {
            MainScreen(navController = navController, carritoVm = carritoVm)
        }
        composable(Screen.Favoritos.route)   { FavoritosScreen(navController) }
        composable(Screen.Perfil.route)      { PerfilScreen(navController) }
        composable(Screen.MisReservas.route) { MisReservasScreen(navController) }
        composable(Screen.ReservaExitosa.route) { ReservaExitosaScreen(navController) }
        composable(Screen.Itinerary.route) { ItineraryScreen(navController) }
        composable(Screen.Pagos.route)    { PagosScreen(navController) }

        // ── Pago de una reserva (MercadoPago)
        composable(
            route = Screen.PagoDetalle.route,
            arguments = listOf(navArgument("reservaId") { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString("reservaId") ?: return@composable
            PagoDetalleScreen(navController = navController, reservaId = id)
        }

        // ── Detalle de servicio (con carrito compartido)
        composable(
            route = Screen.Detalle.route,
            arguments = listOf(navArgument("servicioId") { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString("servicioId") ?: return@composable
            DetalleScreen(navController = navController, servicioId = id, carritoVm = carritoVm)
        }
    }
}
