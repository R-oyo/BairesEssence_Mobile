package com.example.bairesessence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bairesessence.core.ui.navigation.Screen
import com.example.bairesessence.core.ui.screens.home.GuestsScreen
import com.example.bairesessence.core.ui.screens.home.HomeScreen
import com.example.bairesessence.core.ui.screens.home.ItineraryScreen
import com.example.bairesessence.core.ui.screens.home.PaymentMethodsScreen
import com.example.bairesessence.core.ui.screens.home.ProfileScreen
import com.example.bairesessence.core.ui.screens.landing.LandingScreen
import com.example.bairesessence.core.ui.screens.login.LoginScreen
import com.example.bairesessence.core.ui.screens.register.RegisterScreen
import com.example.bairesessence.core.ui.theme.BairesEssenceTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase (por si no se hizo aún)
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        setContent {
            BairesEssenceTheme {
                val navController = rememberNavController()

                // Si hay usuario logueado -> arrancamos en Home, sino en Landing
                val startDestination = remember {
                    if (auth.currentUser != null) {
                        Screen.Home.route
                    } else {
                        Screen.Landing.route
                    }
                }

                BairesEssenceNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    auth = auth
                )
            }
        }
    }
}

@Composable
private fun BairesEssenceNavHost(
    navController: NavHostController,
    startDestination: String,
    auth: FirebaseAuth
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Landing
        composable(Screen.Landing.route) {
            LandingScreen(
                onSignInClick = {
                    navController.navigate(Screen.Login.route)
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        // Login
        composable(Screen.Login.route) {
            LoginScreen(
                auth = auth,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Landing.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        // Register
        composable(Screen.Register.route) {
            RegisterScreen(
                auth = auth,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Landing.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Landing.route) { saveState = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Home + tabs internas
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Itinerary.route) {
            ItineraryScreen(navController = navController)
        }
        composable(Screen.Payments.route) {
            PaymentMethodsScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable(Screen.Guests.route) {
            GuestsScreen(navController = navController)
        }
    }
}
