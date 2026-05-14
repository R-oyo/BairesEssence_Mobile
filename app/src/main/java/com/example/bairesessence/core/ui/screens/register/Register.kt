package com.example.bairesessence.core.ui.screens.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bairesessence.R
import com.example.bairesessence.core.navigation.Screen
import com.example.bairesessence.core.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest

@Composable
fun BairesEssenceRegister(navController: NavHostController) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val isPreview = LocalInspectionMode.current
    val auth: FirebaseAuth? = if (!isPreview) FirebaseAuth.getInstance() else null

    RegisterScreenUI(
        name = name, onNameChange = { name = it; errorMessage = null },
        email = email, onEmailChange = { email = it; errorMessage = null },
        password = password, onPasswordChange = { password = it; errorMessage = null },
        confirmPassword = confirmPassword, onConfirmPasswordChange = { confirmPassword = it; errorMessage = null },
        onRegisterClick = {
            when {
                name.isBlank() || email.isBlank() || password.isBlank() ->
                    errorMessage = "Completá todos los campos."
                password != confirmPassword ->
                    errorMessage = "Las contraseñas no coinciden."
                password.length < 6 ->
                    errorMessage = "La contraseña debe tener al menos 6 caracteres."
                else -> {
                    isLoading = true
                    auth?.createUserWithEmailAndPassword(email, password)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Save display name
                                task.result?.user?.updateProfile(
                                    userProfileChangeRequest { displayName = name.trim() }
                                )
                                isLoading = false
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Landing.route) { inclusive = true }
                                }
                            } else {
                                isLoading = false
                                errorMessage = task.exception?.message ?: "Error al crear la cuenta."
                            }
                        }
                }
            }
        },
        onLoginClick = { navController.navigate(Screen.Login.route) },
        errorMessage = errorMessage,
        isLoading = isLoading
    )
}

@Composable
fun RegisterScreenUI(
    name: String, onNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    confirmPassword: String, onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit,
    errorMessage: String?,
    isLoading: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.baires_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(0.4f), Color.Black.copy(0.85f)))
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            Surface(shape = RoundedCornerShape(8.dp), color = BEPrimary) {
                Text(
                    "BAIRES ESSENCE",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Creá tu cuenta", style = MaterialTheme.typography.headlineSmall,
                color = Color.White, fontWeight = FontWeight.Bold)
            Text("Gratis y sin compromisos", style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.7f))

            Spacer(Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(20.dp),
                color = BESurface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = BEPrimary, trackColor = BEBorder
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    errorMessage?.let {
                        Surface(shape = RoundedCornerShape(8.dp), color = BEError.copy(0.1f)) {
                            Text(it, modifier = Modifier.fillMaxWidth().padding(10.dp),
                                color = BEError, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    listOf(
                        Triple("Nombre completo", name, onNameChange to KeyboardType.Text),
                        Triple("Email", email, onEmailChange to KeyboardType.Email),
                        Triple("Contraseña", password, onPasswordChange to KeyboardType.Password),
                        Triple("Confirmar contraseña", confirmPassword, onConfirmPasswordChange to KeyboardType.Password)
                    ).forEachIndexed { idx, (label, value, pair) ->
                        val (onChange, kbType) = pair
                        OutlinedTextField(
                            value = value,
                            onValueChange = onChange,
                            label = { Text(label) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = kbType),
                            visualTransformation = if (kbType == KeyboardType.Password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder
                            ),
                            enabled = !isLoading
                        )
                        if (idx < 3) Spacer(Modifier.height(12.dp))
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = onRegisterClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BEPrimary),
                        enabled = !isLoading
                    ) {
                        Text("Crear cuenta", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("¿Ya tenés cuenta? ", color = BETextSecond, fontSize = 14.sp)
                        TextButton(onClick = onLoginClick, enabled = !isLoading) {
                            Text("Iniciá sesión", color = BEPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BairesEssenceRegisterPreview() {
    BairesEssenceTheme {
        RegisterScreenUI(
            name = "", onNameChange = {}, email = "", onEmailChange = {},
            password = "", onPasswordChange = {}, confirmPassword = "", onConfirmPasswordChange = {},
            onRegisterClick = {}, onLoginClick = {}, errorMessage = null, isLoading = false
        )
    }
}
