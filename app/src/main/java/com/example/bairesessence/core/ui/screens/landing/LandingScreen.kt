package com.example.bairesessence.core.ui.screens.landing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.bairesessence.R
import com.example.bairesessence.core.navigation.Screen
import com.example.bairesessence.core.ui.theme.*

@Composable
fun LandingScreen(navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image — FillWidth + TopCenter muestra el Obelisco sin cortarlo
        Image(
            painter = painterResource(id = R.drawable.baires_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter
        )

        // Dark gradient overlay — stronger at bottom
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.35f),
                        Color.Black.copy(alpha = 0.75f)
                    )
                )
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // Brand badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BEPrimary
            ) {
                Text(
                    "BAIRES ESSENCE",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Descubrí Buenos Aires\ncomo nunca antes",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Tours, gastronomía, cultura y experiencias únicas curadas para vos.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            // TripAdvisor-style trust badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrustBadge("★ 4.9", "Valoración media")
                TrustBadge("500+", "Experiencias")
                TrustBadge("10k+", "Viajeros")
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate(Screen.Login.route) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
            ) {
                Text(
                    "Iniciar sesión",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { navController.navigate(Screen.Register.route) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(0.7f))
            ) {
                Text(
                    "Crear cuenta gratis",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun TrustBadge(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BEPrimaryLight)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
    }
}

@Preview
@Composable
fun LandingScreenPreview() {
    BairesEssenceTheme { LandingScreen(navController = rememberNavController()) }
}
