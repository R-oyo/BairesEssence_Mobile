package com.example.bairesessence.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.bairesessence.data.model.Servicio

@Composable
fun ServicioCard(servicio: Servicio, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .width(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rememberAsyncImagePainter(servicio.image),
                contentDescription = servicio.title,
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = servicio.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = servicio.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 2,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
