package com.example.bairesessence.core.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bairesessence.R
import com.example.bairesessence.core.navigation.Screen
import com.example.bairesessence.core.ui.theme.BEDark
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    val alpha = remember { Animatable(0f) }
    val auth = FirebaseAuth.getInstance()

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(durationMillis = 700))
        delay(800L)
        val dest = if (auth.currentUser != null) Screen.Home.route else Screen.Landing.route
        navController.navigate(dest) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BEDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_baires),
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .alpha(alpha.value)
            )
            Spacer(Modifier.height(28.dp))
            Text(
                "BAIRES ESSENCE",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = alpha.value),
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Buenos Aires, Argentina",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = alpha.value * 0.55f),
                letterSpacing = 1.sp
            )
        }
    }
}
