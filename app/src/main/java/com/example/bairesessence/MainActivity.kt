// MainActivity.kt
package com.example.bairesessence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.bairesessence.core.ui.screens.landing.LandingScreen // IMPORTACIÓN CLAVE
import com.example.bairesessence.core.ui.theme.BairesEssenceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BairesEssenceTheme {
                LandingScreen()
            }
        }
    }
}