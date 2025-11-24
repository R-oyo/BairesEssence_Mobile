package com.example.bairesessence.core.ui.screens.pagos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.bairesessence.core.ui.navigation.Screen
import com.example.bairesessence.core.ui.screens.home.AppScaffold

@Composable
fun PaymentMethodsScreen(navController: NavHostController) {
    AppScaffold(
        navController = navController,
        currentRoute = Screen.Payments.route
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2))
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Cómo se paga tu experiencia",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Trabajamos con transferencia bancaria, tarjetas y efectivo. " +
                            "Siempre vas a coordinar el pago final directamente con Baires Essence " +
                            "luego de confirmar tu reserva por WhatsApp.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ========== TRANSFERENCIA ==========
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Transferencia bancaria",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Es el método recomendado. Una vez confirmada la reserva " +
                                    "te enviamos los datos de la cuenta y el monto final en pesos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ========== TARJETA ==========
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Tarjeta de crédito / débito",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Podés pagar con tarjeta a través de un link de pago. " +
                                    "Aplican las condiciones e impuestos vigentes del proveedor de pagos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ========== EFECTIVO ==========
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Efectivo",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "En algunos servicios podés abonar una parte en efectivo el día de la experiencia. " +
                                    "Lo coordinamos caso por caso al momento de la reserva.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Recordá que por ahora la confirmación final siempre se coordina por WhatsApp " +
                            "luego de que envíes tu itinerario desde la app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
