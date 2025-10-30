// MainActivity.kt
package com.example.bairesessence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.bairesessence.core.ui.screens.landing.LandingScreen
import com.example.bairesessence.core.ui.theme.BairesEssenceTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this) // Inicializa Firebase

        setContent {
            BairesEssenceTheme {
                LandingScreen() // Tu pantalla principal
            }
        }
    }
}
