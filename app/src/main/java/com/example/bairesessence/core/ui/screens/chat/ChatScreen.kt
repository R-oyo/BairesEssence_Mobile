package com.example.bairesessence.core.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bairesessence.core.ui.theme.*
import com.example.bairesessence.data.firebase.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, reservaId: String) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val mensajes = remember { mutableStateListOf<Map<String, Any>>() }
    var texto by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }

    DisposableEffect(reservaId) {
        val reg = db.collection("reservas").document(reservaId)
            .collection("mensajes")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                mensajes.clear()
                snap?.documents?.forEach { doc ->
                    val d = doc.data?.toMutableMap() ?: return@forEach
                    d["id"] = doc.id
                    mensajes.add(d)
                }
            }
        onDispose { reg.remove() }
    }

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) listState.animateScrollToItem(mensajes.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat con equipo", fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BEDark)
            )
        },
        containerColor = BEBackground
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (mensajes.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💬", style = MaterialTheme.typography.displayMedium)
                                Spacer(Modifier.height(8.dp))
                                Text("Todavía no hay mensajes",
                                    style = MaterialTheme.typography.bodyMedium, color = BETextMuted)
                                Text("Escribí tu consulta y el equipo te responderá.",
                                    style = MaterialTheme.typography.bodySmall, color = BETextMuted)
                            }
                        }
                    }
                } else {
                    items(mensajes, key = { it["id"] as? String ?: it.hashCode().toString() }) { msg ->
                        val esMio = msg["senderId"] == user?.uid
                        val msgTexto = msg["texto"] as? String ?: ""
                        val senderEmail = (msg["senderEmail"] as? String)?.substringBefore("@") ?: "Equipo"
                        val ts = msg["timestamp"] as? com.google.firebase.Timestamp
                        val hora = ts?.toDate()?.let {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
                        } ?: ""

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                horizontalAlignment = if (esMio) Alignment.End else Alignment.Start,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                if (!esMio) {
                                    Text(senderEmail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BETextMuted,
                                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp, topEnd = 16.dp,
                                        bottomStart = if (esMio) 16.dp else 4.dp,
                                        bottomEnd = if (esMio) 4.dp else 16.dp
                                    ),
                                    color = if (esMio) BEPrimary else BESurface,
                                    shadowElevation = 1.dp
                                ) {
                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(msgTexto,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (esMio) Color.White else BETextPrimary)
                                        if (hora.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(hora,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (esMio) Color.White.copy(0.7f) else BETextMuted,
                                                modifier = Modifier.align(Alignment.End))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input
            Surface(shadowElevation = 8.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(BESurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        placeholder = { Text("Escribí un mensaje...", color = BETextMuted) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        enabled = !enviando,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BEPrimary,
                            unfocusedBorderColor = BEBorder
                        )
                    )
                    IconButton(
                        onClick = {
                            val msg = texto.trim()
                            if (msg.isBlank() || user == null) return@IconButton
                            enviando = true
                            val temp = msg
                            texto = ""
                            scope.launch {
                                runCatching {
                                    FirestoreRepository.sendMensaje(
                                        reservaId = reservaId,
                                        senderId = user.uid,
                                        senderEmail = user.email ?: "",
                                        texto = temp
                                    )
                                }
                                enviando = false
                            }
                        },
                        enabled = texto.isNotBlank() && !enviando,
                        modifier = Modifier
                            .size(48.dp)
                            .background(if (texto.isNotBlank()) BEPrimary else BEBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, "Enviar", tint = Color.White)
                    }
                }
            }
        }
    }
}
