package com.example.bairesessence.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun ResenaDialog(
    reservaId: String,
    servicioId: String,
    servicioTitle: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser
    var rating by remember { mutableStateOf(0) }
    var comentario by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var yaRevisado by remember { mutableStateOf(false) }
    var verificando by remember { mutableStateOf(true) }

    LaunchedEffect(reservaId) {
        if (user != null) {
            yaRevisado = runCatching {
                FirestoreRepository.hasUserReviewedReserva(user.uid, reservaId)
            }.getOrDefault(false)
        }
        verificando = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BESurface,
        title = {
            Column {
                Text("Calificá tu experiencia", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = BETextPrimary)
                Text(servicioTitle, style = MaterialTheme.typography.bodySmall, color = BETextSecond)
            }
        },
        text = {
            if (verificando) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BEPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            } else if (yaRevisado) {
                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), color = BEPrimary.copy(0.08f)) {
                    Text(
                        "Ya dejaste una reseña para esta reserva.",
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BEPrimaryDark
                    )
                }
            } else Column(modifier = Modifier.fillMaxWidth()) {
                // Star picker
                Text("Tu puntuación", style = MaterialTheme.typography.labelMedium,
                    color = BETextSecond, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { i ->
                        TextButton(
                            onClick = { rating = i + 1 },
                            contentPadding = PaddingValues(4.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text(
                                "★",
                                fontSize = 28.sp,
                                color = if (i < rating) BEStarColor else BEBorder
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = comentario,
                    onValueChange = { if (it.length <= 140) comentario = it },
                    label = { Text("Contá tu experiencia (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 3,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder
                    )
                )
                Text(
                    "${comentario.length}/140",
                    style = MaterialTheme.typography.labelSmall,
                    color = BETextMuted,
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                )

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = BEError, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (!verificando && !yaRevisado) {
                Button(
                    onClick = {
                        if (rating == 0) { error = "Elegí una puntuación."; return@Button }
                        if (user == null) { error = "Necesitás sesión activa."; return@Button }
                        enviando = true
                        scope.launch {
                            runCatching {
                                FirestoreRepository.addResena(
                                    userId = user.uid,
                                    userEmail = user.email ?: "",
                                    reservaId = reservaId,
                                    servicioId = servicioId,
                                    rating = rating,
                                    comentario = comentario.trim()
                                )
                            }.onSuccess { onSuccess() }
                             .onFailure { enviando = false; error = "Error al enviar. Intentá de nuevo." }
                        }
                    },
                    enabled = !enviando,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)
                ) {
                    if (enviando) CircularProgressIndicator(color = Color.White,
                        modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Enviar reseña", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else if (yaRevisado) {
                Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BEPrimary)) {
                    Text("Cerrar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = BETextSecond) }
        }
    )
}
