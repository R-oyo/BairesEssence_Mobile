package com.example.bairesessence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            BairesEssenceTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
private fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Landing.route
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
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Landing.route) { inclusive = true }
                    }
                },
                onGoToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        // Register
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Landing.route) { inclusive = true }
                    }
                },
                onGoToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Landing.route) { saveState = true }
                    }
                }
            )
        }

        // Pantallas internas con bottom bar
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }

        composable(Screen.Itinerary.route) {
            ItineraryScreen(navController)
        }

        composable(Screen.Payments.route) {
            PaymentMethodsScreen(navController)
        }

        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }

        composable(Screen.Guests.route) {
            GuestsScreen(navController)
        }
    }
}
