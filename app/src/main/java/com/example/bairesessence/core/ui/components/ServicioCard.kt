package com.example.bairesessence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import coil.compose.SubcomposeAsyncImage
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.model.Servicio

@Composable
fun RatingStars(rating: Double, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { i ->
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = if (i < rating.toInt()) BEStarColor else BEBorder,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "$rating",
            style = MaterialTheme.typography.labelSmall,
            color = BETextSecond,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ServicioCard(
    servicio: Servicio,
    enCarrito: Boolean = false,
    esFavorito: Boolean = false,
    onFavoritoClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BESurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = if (enCarrito) androidx.compose.foundation.BorderStroke(2.dp, BEPrimary) else null
    ) {
        Box {
            // Hero image
            SubcomposeAsyncImage(
                model = servicio.image.ifBlank { null },
                contentDescription = servicio.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop,
                loading = { ImageShimmer() },
                error = { ImagePlaceholder(servicio.categoria) }
            )

            // Categoria badge — top left
            if (servicio.categoria.isNotBlank()) {
                Surface(
                    modifier = Modifier.padding(10.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = BEDark.copy(alpha = 0.85f)
                ) {
                    Text(
                        servicio.categoria.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = BEPrimaryLight,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Heart button — top right
            if (onFavoritoClick != null) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.92f)
                ) {
                    IconButton(
                        onClick = onFavoritoClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (esFavorito) "Quitar favorito" else "Agregar a favoritos",
                            tint = if (esFavorito) BEError else BETextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // En carrito badge — bottom right
            if (enCarrito) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = BEPrimary
                ) {
                    Text(
                        "En carrito",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(12.dp)) {
            // Title
            Text(
                servicio.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = BETextPrimary
            )

            Spacer(Modifier.height(4.dp))

            // Stars + rating
            RatingStars(servicio.rating)

            Spacer(Modifier.height(6.dp))

            // Location row
            if (servicio.ubicacion.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = BETextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        servicio.ubicacion,
                        style = MaterialTheme.typography.bodySmall,
                        color = BETextSecond,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            // Duration + price row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (servicio.duracion.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = BETextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            servicio.duracion,
                            style = MaterialTheme.typography.bodySmall,
                            color = BETextMuted
                        )
                    }
                }
                if (servicio.tienePrecio) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Desde",
                            style = MaterialTheme.typography.labelSmall,
                            color = BETextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            servicio.precioFormateado,
                            style = MaterialTheme.typography.labelMedium,
                            color = BEPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageShimmer() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offsetState = transition.animateFloat(
        initialValue = -1000f, targetValue = 1000f, label = "shimmer_offset",
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing))
    )
    Box(
        Modifier.fillMaxWidth().height(180.dp).background(
            Brush.linearGradient(
                colors = listOf(BESurface, BEBorder, BESurface),
                start = Offset(offsetState.value, 0f),
                end   = Offset(offsetState.value + 600f, 300f)
            )
        )
    )
}

@Composable
private fun ImagePlaceholder(categoria: String) {
    val icon = when (categoria.lowercase()) {
        "gastronomia", "gastronomía" -> Icons.Default.Restaurant
        "traslados"  -> Icons.Default.DirectionsBus
        "experiencias" -> Icons.Default.TheaterComedy
        else -> Icons.Default.Map
    }
    Box(
        Modifier.fillMaxWidth().height(180.dp).background(BESurface),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = BETextMuted, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(4.dp))
            Text("Sin imagen", style = MaterialTheme.typography.labelSmall, color = BETextMuted)
        }
    }
}
