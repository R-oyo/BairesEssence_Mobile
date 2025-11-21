package com.example.bairesessence.core.ui.screens.landing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.example.bairesessence.R
import com.example.bairesessence.core.ui.theme.BairesEssenceTheme

@Composable
fun LandingScreen(
    onSignInClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val whiteShadowStyle = TextStyle(
        shadow = Shadow(
            color = Color.White,
            offset = Offset(x = 2f, y = 2f),
            blurRadius = 4f
        )
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.baires_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Location Icon",
                tint = Color.Black,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BAIRES ESSENCE",
                style = whiteShadowStyle.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Welcome-bienvenido...",
                style = whiteShadowStyle.copy(
                    fontSize = 16.sp,
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onSignInClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8BC34A)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Sign In", fontSize = 18.sp, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onRegisterClick,
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
        LandingScreen(
            onSignInClick = {},
            onRegisterClick = {}
        )
    }
}
