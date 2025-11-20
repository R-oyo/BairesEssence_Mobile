package com.example.bairesessence.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bairesessence.core.ui.screens.home.MainScreen
import com.example.bairesessence.core.ui.screens.landing.LandingScreen
import com.example.bairesessence.core.ui.screens.login.BairesEssenceLogin
import com.example.bairesessence.core.ui.screens.register.BairesEssenceRegister

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Landing.route
    ) {
        composable(Screen.Landing.route) {
            LandingScreen(navController)
        }
        composable(Screen.Login.route) {
            BairesEssenceLogin(navController)
        }
        composable(Screen.Register.route) {
            BairesEssenceRegister(navController)
        }
        composable(Screen.Home.route) {
            MainScreen(navController)
        }
    }
}