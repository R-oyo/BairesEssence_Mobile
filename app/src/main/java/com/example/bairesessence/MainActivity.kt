package com.example.bairesessence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bairesessence.core.ui.navigation.Screen
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

                    // Home (placeholder por ahora)
                    composable(Screen.Home.route) {
                        HomePlaceholder()
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Home Baires Essence (después agregamos bottom bar y cards)",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
