import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bairesessence.R
import com.example.bairesessence.ui.theme.BairesEssenceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BairesEssenceTheme {
                BairesEssenceRegister()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BairesEssenceRegister() {
    // Nuevos estados para los 4 campos
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Definición del TextStyle con Sombra Blanca
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
        // 1. FONDO DE IMAGEN
        Image(
            painter = painterResource(id = R.drawable.baires_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. RECUIADRO NEGRO (Layer de fondo del formulario)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Color.Black)
        )

        // 3. CAPA DE CONTENIDO
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // CONTENIDO SUPERIOR (Título e Ícono)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Espacio inicial para empujar el contenido desde el borde superior
                Spacer(modifier = Modifier.height(30.dp))

                // Ícono de ubicación
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Location Icon",
                    tint = Color.Black,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Título
                Text(
                    text = "BAIRES ESSENCE",
                    style = whiteShadowStyle.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )

                // Espaciador para empezar el recuadro negro
                Spacer(modifier = Modifier.height(90.dp))

                // --- CAMPOS DE REGISTRO ---

                // Input Name
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Name", color = Color.LightGray) },
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.LightGray,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp)) // Espacio reducido entre campos

                // Input Email
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Mail", color = Color.LightGray) },
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.LightGray,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input Password
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password", color = Color.LightGray) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.LightGray,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input Confirm Password
                TextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text("Confirm Password", color = Color.LightGray) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.LightGray,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )


                Spacer(modifier = Modifier.height(30.dp)) // Espacio antes del botón

                // Botón Register
                Button(
                    onClick = { /* TODO: acción registro */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9CFF3C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(text = "Register", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            } // Fin Column Superior


            // CONTENIDO INFERIOR (Login)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "¿Ya estás registrado? ", color = Color.White, fontSize = 14.sp)
                TextButton(onClick = { /* TODO: acción login */ }) {
                    Text(
                        text = "Login", // Texto modificado
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// PREVIEW para la nueva pantalla
@Preview(showBackground = true)
@Composable
fun BairesEssenceRegisterPreview() {
    BairesEssenceTheme {
        BairesEssenceRegister()
    }
}