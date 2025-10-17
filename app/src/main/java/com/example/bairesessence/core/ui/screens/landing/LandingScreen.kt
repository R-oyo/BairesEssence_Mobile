// LandingScreen.kt
package com.example.bairesessence.core.ui.screens.landing // Asegúrate de usar el paquete correcto

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bairesessence.R // Necesitas importar tu R
import com.example.bairesessence.core.ui.theme.BairesEssenceTheme // Necesitas importar tu Theme

// Renombrada de WelcomeScreen a LandingScreen
@Composable
fun LandingScreen() {

    // Definición del TextStyle con Sombra Blanca
    val whiteShadowStyle = TextStyle(
        shadow = Shadow(
            color = Color.White,
            offset = Offset(x = 2f, y = 2f),
            blurRadius = 4f
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Fondo de imagen
        Image(
            painter = painterResource(id = R.drawable.baires_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Contenido
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            // Espacio inicial para empujar el contenido desde el borde superior
            Spacer(modifier = Modifier.height(80.dp))

            // Ícono de ubicación
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Location Icon",
                tint = Color.Black,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Título con Sombra
            Text(
                text = "BAIRES ESSENCE",
                style = whiteShadowStyle.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Texto de bienvenida con Sombra
            Text(
                text = "Welcome-bienvenido...",
                style = whiteShadowStyle.copy(
                    fontSize = 16.sp,
                    color = Color.Black
                )
            )

            // Espaciador flexible que empuja los botones
            Spacer(modifier = Modifier.weight(1f))

            // Botón Sign In
            Button(
                onClick = { /* Acción Sign In */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8BC34A)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Sign In", fontSize = 18.sp, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(64.dp))

            // Botón Register
            Button(
                onClick = { /* Acción Register */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8BC34A)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Register", fontSize = 18.sp, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(128.dp))
        }
    }
}

@Preview
@Composable
fun LandingScreenPreview() {
    BairesEssenceTheme {
        LandingScreen()
    }
}