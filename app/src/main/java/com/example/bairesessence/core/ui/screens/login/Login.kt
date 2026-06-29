package com.example.bairesessence.core.ui.screens.login

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bairesessence.R
import com.example.bairesessence.core.navigation.Screen
import com.example.bairesessence.core.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun BairesEssenceLogin(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val auth: FirebaseAuth? = if (!isPreview) FirebaseAuth.getInstance() else null

    val googleSignInClient = remember {
        if (!isPreview) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, gso)
        } else null
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    isLoading = true
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account?.idToken
                    if (idToken == null) {
                        isLoading = false
                        errorMessage = "No se pudo obtener el token de Google. Intentá de nuevo."
                        return@rememberLauncherForActivityResult
                    }
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth?.signInWithCredential(credential)?.addOnCompleteListener { firebaseTask ->
                        if (firebaseTask.isSuccessful) {
                            val uid = firebaseTask.result?.user?.uid ?: return@addOnCompleteListener
                            val firebaseUser = firebaseTask.result?.user
                            val ref = FirebaseFirestore.getInstance().collection("users").document(uid)
                            ref.get()
                                .addOnSuccessListener { doc ->
                                    if (!doc.exists()) {
                                        ref.set(mapOf(
                                            "userId"   to uid,
                                            "email"    to (firebaseUser?.email ?: ""),
                                            "fullname" to (firebaseUser?.displayName ?: ""),
                                            "fullName" to (firebaseUser?.displayName ?: ""),
                                            "role"     to "user",
                                            "activo"   to true
                                        )).addOnCompleteListener {
                                            isLoading = false
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(Screen.Landing.route) { inclusive = true }
                                            }
                                        }
                                    } else {
                                        val activo = doc.getBoolean("activo") ?: true
                                        isLoading = false
                                        if (!activo) {
                                            auth?.signOut()
                                            errorMessage = "Tu cuenta está desactivada."
                                        } else {
                                            navController.navigate(Screen.Home.route) { popUpTo(Screen.Landing.route) { inclusive = true } }
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Landing.route) { inclusive = true } }
                                }
                        } else {
                            isLoading = false
                            errorMessage = firebaseTask.exception?.message ?: "Error en la autenticación."
                        }
                    }
                } catch (e: ApiException) {
                    isLoading = false
                    errorMessage = when (e.statusCode) {
                        12500 -> "Google Sign-In no está configurado. Verificá el SHA-1 en Firebase Console."
                        12501 -> null  // user cancelled — no error
                        7     -> "Sin conexión a internet."
                        else  -> "Google Sign-In falló (código ${e.statusCode})."
                    }
                    Log.e("GoogleSignIn", "ApiException code=${e.statusCode}", e)
                }
            }
            Activity.RESULT_CANCELED -> { /* user cancelled — no error */ }
        }
    }

    LoginScreenUI(
        email = email,
        onEmailChange = { email = it; errorMessage = null },
        password = password,
        onPasswordChange = { password = it; errorMessage = null },
        onLoginClick = {
            if (email.isBlank() || password.isBlank()) {
                errorMessage = "Completá email y contraseña."
                return@LoginScreenUI
            }
            isLoading = true
            auth?.signInWithEmailAndPassword(email, password)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = task.result?.user?.uid ?: return@addOnCompleteListener
                        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val activo = doc.getBoolean("activo") ?: true
                                isLoading = false
                                if (!activo) {
                                    auth.signOut()
                                    errorMessage = "Tu cuenta está desactivada."
                                } else {
                                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Landing.route) { inclusive = true } }
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Landing.route) { inclusive = true } }
                            }
                    } else {
                        isLoading = false
                        errorMessage = task.exception?.message ?: "Email o contraseña incorrectos."
                    }
                }
        },
        onGoogleLoginClick = {
            errorMessage = null
            googleSignInClient?.signInIntent?.let { launcher.launch(it) }
        },
        onRegisterClick = { navController.navigate(Screen.Register.route) },
        onForgotPasswordClick = {
            if (email.isBlank()) { errorMessage = "Ingresá tu email para recuperar la contraseña."; return@LoginScreenUI }
            errorMessage = null; resetMessage = null
            FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim())
                .addOnSuccessListener { resetMessage = "Te enviamos un mail para restablecer tu contraseña." }
                .addOnFailureListener { resetMessage = null; errorMessage = it.localizedMessage ?: "No se pudo enviar el mail." }
        },
        errorMessage = errorMessage,
        resetMessage = resetMessage,
        isLoading = isLoading
    )
}

@Composable
fun LoginScreenUI(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit = {},
    errorMessage: String?,
    resetMessage: String? = null,
    isLoading: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.baires_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Gradient overlay
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(0.4f), Color.Black.copy(0.85f)))
            )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            // Brand
            Surface(shape = RoundedCornerShape(8.dp), color = BEPrimary) {
                Text(
                    "BAIRES ESSENCE",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Bienvenido de vuelta", style = MaterialTheme.typography.headlineSmall,
                color = Color.White, fontWeight = FontWeight.Bold)
            Text("Iniciá sesión para continuar", style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.7f))

            Spacer(Modifier.weight(1f))

            // Form card
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                color = BESurface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = BEPrimary,
                            trackColor = BEBorder
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

                    OutlinedTextField(
                        value = email, onValueChange = onEmailChange,
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder
                        ),
                        enabled = !isLoading
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password, onValueChange = onPasswordChange,
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BEPrimary, unfocusedBorderColor = BEBorder
                        ),
                        enabled = !isLoading
                    )

                    if (resetMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(resetMessage, color = BEPrimary, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(4.dp))

                    TextButton(
                        onClick = onForgotPasswordClick,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("¿Olvidaste tu contraseña?", color = BETextSecond, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BEPrimary),
                        enabled = !isLoading
                    ) {
                        Text("Iniciar sesión", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BEBorder)
                        Text("  o  ", color = BETextMuted, style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BEBorder)
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onGoogleLoginClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BETextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BEBorder),
                        enabled = !isLoading
                    ) {
                        Text("Continuar con Google", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("¿No tenés cuenta? ", color = BETextSecond, fontSize = 14.sp)
                        TextButton(onClick = onRegisterClick, enabled = !isLoading) {
                            Text("Registrate", color = BEPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BairesEssenceLoginPreview() {
    BairesEssenceTheme {
        LoginScreenUI(
            email = "", onEmailChange = {}, password = "", onPasswordChange = {},
            onLoginClick = {}, onGoogleLoginClick = {}, onRegisterClick = {},
            errorMessage = null, isLoading = false
        )
    }
}
