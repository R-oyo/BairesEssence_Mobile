package com.example.bairesessence.core.ui.screens.pagos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.bairesessence.core.ui.navigation.Screen
import com.example.bairesessence.core.ui.screens.home.AppScaffold

@Composable
fun PaymentMethodsScreen(navController: NavHostController) {
    AppScaffold(
        navController = navController,
        currentRoute = Screen.Payments.route
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(Color(0xFFF2F2F2)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Métodos de Pago",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Elegí el método de pago que prefieras. Todos los pagos son confirmados vía WhatsApp y se emite comprobante digital.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )

            // ✔ Transferencia Bancaria
            PaymentCard(
                title = "Transferencia bancaria",
                icon = Icons.Default.AccountBalance,
                content = {
                    Text("CBU: 0000007900204638154221")
                    Text("Alias: bairesessence")
                    Text("Titular: Baires Essence Viajes")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Una vez realizada la transferencia, enviá el comprobante por WhatsApp para confirmar tu reserva.",
                        color = Color.DarkGray
                    )
                }
            )

            // ✔ Tarjeta de crédito
            PaymentCard(
                title = "Tarjeta de crédito / débito",
                icon = Icons.Default.CreditCard,
                content = {
                    Text("Aceptamos todas las tarjetas principales.")
                    Text("El link de pago se envía vía WhatsApp para tu seguridad.")
                }
            )

            // ✔ Efectivo
            PaymentCard(
                title = "Efectivo",
                icon = Icons.Default.AttachMoney,
                content = {
                    Text("Disponible solo para servicios dentro de CABA.")
                    Text("El pago se realiza al iniciar el servicio.")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Confirmación",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Después de pagar, recibirás la confirmación de tu reserva y los detalles del servicio por WhatsApp.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
private fun PaymentCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Black
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}
