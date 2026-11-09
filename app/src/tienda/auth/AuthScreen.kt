package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.example.data.remote.EleganApiClient
import com.example.data.remote.MobileSession
import com.example.data.remote.RegisterRequest
import com.example.data.remote.LoginRequest
import com.example.data.remote.GoogleAuthRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

const val APP_PACKAGE_NAME = "com.elegan"
const val APP_DEBUG_SHA1 = "1D:3F:73:38:83:E9:8B:0A:00:95:26:0D:08:A9:1D:CE:4E:51:89:72"
const val DEFAULT_SUPER_ADMIN_EMAIL = "cg0220037@gmail.com"

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val credentialManager = remember { CredentialManager.create(context) }
    val activity = context as? Activity

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Google, 1: Correo, 2: Acceso Rápido
    var isRegisterMode by remember { mutableStateOf(false) }

    // Form states for Email / Password
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showNoAccountDialog by remember { mutableStateOf(false) }

    val submitEmailAuth: () -> Unit = {
        val email = emailInput.trim()
        val password = passwordInput
        if (email.isBlank() || !email.contains("@")) {
            errorMessage = "Por favor ingresa un correo electrónico válido."
        } else if (password.length < 6) {
            errorMessage = "La contraseña debe tener al menos 6 caracteres."
        } else if (isRegisterMode && password != confirmPasswordInput) {
            errorMessage = "Las contraseñas no coinciden."
        } else {
            isLoading = true
            errorMessage = null

            coroutineScope.launch {
                try {
                    val response = if (isRegisterMode) {
                        val displayName = nameInput.trim().ifBlank {
                            if (email.equals(DEFAULT_SUPER_ADMIN_EMAIL, ignoreCase = true)) "Carlos García (Administrador)" else "Caballero Elegan"
                        }
                        try {
                            EleganApiClient.auth.register(
                                RegisterRequest(displayName, email, password)
                            )
                        } catch (netEx: Exception) {
                            Log.w("Auth", "Servidor no disponible para registro remoto (${netEx.message}), registrando en almacenamiento seguro local.")
                            MobileSession.registerLocalUser(context, displayName, email, password)
                        }
                    } else {
                        try {
                            EleganApiClient.auth.login(LoginRequest(email, password))
                        } catch (netEx: Exception) {
                            Log.w("Auth", "Servidor no disponible para login remoto (${netEx.message}), autenticando con credenciales locales.")
                            MobileSession.loginLocalUser(context, email, password)
                        }
                    }
                    MobileSession.save(context, response)
                    isLoading = false
                    onAuthSuccess()
                } catch (e: Exception) {
                    isLoading = false
                    Log.e("Auth", "Error al procesar correo/contraseña", e)
                    errorMessage = e.message?.takeIf {
                        it.isNotBlank() &&
                        !it.contains("JsonDataException") &&
                        !it.contains("BEGIN_OBJECT") &&
                        !it.contains("HTTP 200")
                    } ?: mapAuthApiError(e)
                }
            }
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            EleganBurgundyDark,
            EleganBurgundy,
            Color(0xFF2B050B)
        )
    )

    DisposableEffect(activity) {
        activity?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.WHITE
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = true
        }
        onDispose {
            activity?.window?.let { window ->
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ==============================================================
            // CONTENEDOR SUPERIOR (ROJO VIVO / BURGUNDY)
            // Icono, Logo y Presentación de ELEGAN
            // ==============================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 28.dp, bottom = 26.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo Elegan
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, EleganGold.copy(alpha = 0.7f), CircleShape)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.icono),
                        contentDescription = "Logo Elegan",
                        modifier = Modifier.size(58.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "ELEGAN",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Serif
                    )
                )

                Text(
                    text = "ALTA SASTRERÍA & MODA CABALLERO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        color = EleganGold,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // ==============================================================
            // CONTENEDOR BLANCO INFERIOR (Cubre laterales y fondo completo)
            // ==============================================================
            val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val isImeVisible = imeBottomPadding > 0.dp

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ÁREA DESPLAZABLE DE CONTENIDO
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // SELECTOR HORIZONTAL DE 2 MÉTODOS (Google y Correo)
                        // Fondo 100% blanco puro
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.White,
                            contentColor = EleganBurgundy,
                            divider = {
                                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                            },
                            indicator = { tabPositions ->
                                if (selectedTab < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                        color = EleganBurgundy,
                                        height = 3.dp
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = {
                                    selectedTab = 0
                                    errorMessage = null
                                },
                                text = {
                                    Text(
                                        "Google",
                                        fontSize = 14.sp,
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                    errorMessage = null
                                },
                                text = {
                                    Text(
                                        "Correo",
                                        fontSize = 14.sp,
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // CONTENIDO SEGÚN LA PESTAÑA SELECCIONADA
                        when (selectedTab) {
                            0 -> {
                                // PESTAÑA GOOGLE
                                Text(
                                    text = "Inicia sesión con tu cuenta de Google para sincronizar tus pedidos, catálogo guardado y compras en la tienda.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF555555),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = EleganBurgundy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Acceso seguro y verificado",
                                        fontSize = 13.sp,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }

                            1 -> {
                                // PESTAÑA CORREO Y CONTRASEÑA
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    TextButton(
                                        onClick = {
                                            isRegisterMode = false
                                            errorMessage = null
                                        }
                                    ) {
                                        Text(
                                            "Iniciar Sesión",
                                            fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                                            color = if (!isRegisterMode) EleganBurgundy else Color(0xFF777777),
                                            fontSize = 13.5.sp
                                        )
                                    }
                                    Text(
                                        " | ",
                                        color = Color(0xFFCCCCCC),
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                    TextButton(
                                        onClick = {
                                            isRegisterMode = true
                                            errorMessage = null
                                        }
                                    ) {
                                        Text(
                                            "Crear Cuenta",
                                            fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isRegisterMode) EleganBurgundy else Color(0xFF777777),
                                            fontSize = 13.5.sp
                                        )
                                    }
                                }

                                if (isRegisterMode) {
                                    OutlinedTextField(
                                        value = nameInput,
                                        onValueChange = { nameInput = it },
                                        label = { Text("Nombre y Apellido") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = EleganBurgundy
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("auth_name_input"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EleganBurgundy,
                                            unfocusedBorderColor = Color(0xFFDDD5CE)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Correo Electrónico") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Email,
                                            contentDescription = null,
                                            tint = EleganBurgundy
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_email_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EleganBurgundy,
                                        unfocusedBorderColor = Color(0xFFDDD5CE)
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("Contraseña (mínimo 6 caracteres)") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = EleganBurgundy
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Mostrar u ocultar contraseña",
                                                tint = Color(0xFF777777)
                                            )
                                        }
                                    },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { if (!isRegisterMode) submitEmailAuth() }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_password_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EleganBurgundy,
                                        unfocusedBorderColor = Color(0xFFDDD5CE)
                                    )
                                )

                                if (isRegisterMode) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = confirmPasswordInput,
                                        onValueChange = { confirmPasswordInput = it },
                                        label = { Text("Confirmar Contraseña") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = EleganBurgundy
                                            )
                                        },
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { submitEmailAuth() }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("auth_confirm_password_input"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EleganBurgundy,
                                            unfocusedBorderColor = Color(0xFFDDD5CE)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFEBEE)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = null,
                                tint = EleganGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Exclusividad, Calidad & Confianza",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF777777)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(5.dp))
                    }

                    if (!isImeVisible) {
                        // ==============================================================
                        // BARRA FIJA EN EL PIE DE PÁGINA (Altura 60dp - Unida en fondo blanco)
                        // ==============================================================
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .navigationBarsPadding()
                                    .height(60.dp)
                                    .padding(horizontal = 20.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedTab == 0) {
                                    Button(
                                        onClick = {
                                            isLoading = true
                                            errorMessage = null
                                            onGoogleSignInClicked(
                                                context = context,
                                                credentialManager = credentialManager,
                                                onAuthSuccess = {
                                                    isLoading = false
                                                    onAuthSuccess()
                                                },
                                                onAuthError = { err ->
                                                    isLoading = false
                                                    errorMessage = err
                                                },
                                                onNoCredentials = {
                                                    isLoading = false
                                                    showNoAccountDialog = true
                                                },
                                                scope = coroutineScope,
                                                onAuthCancelled = {
                                                    isLoading = false
                                                }
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                            .testTag("google_sign_in_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EleganBurgundy,
                                            contentColor = Color.White
                                        ),
                                        enabled = !isLoading
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Iniciar sesión con Google",
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { submitEmailAuth() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                            .testTag("email_submit_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EleganBurgundy,
                                            contentColor = Color.White
                                        ),
                                        enabled = !isLoading
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = if (isRegisterMode) "Crear Mi Cuenta Elegan" else "Iniciar Sesión",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }



    // NO GOOGLE ACCOUNT ON DEVICE / EMULATOR DIALOG
    if (showNoAccountDialog) {
        AlertDialog(
            onDismissRequest = { showNoAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = EleganBurgundy,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Sin Cuenta Google en el Dispositivo",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganBurgundyDark
                        )
                    )
                }
            },
            text = {
                Text(
                    text = "No fue posible iniciar sesión con Google en este dispositivo. Para acceder a Elegan, selecciona la pestaña Correo e inicia sesión con tu correo electrónico y contraseña.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF444444),
                        lineHeight = 18.sp
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNoAccountDialog = false
                    selectedTab = 1
                    isRegisterMode = false
                    errorMessage = null
                }) {
                    Text("Ingresar con correo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoAccountDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
fun getGoogleWebClientId(context: Context): String? {
    val configuredClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
    if (configuredClientId.isNotEmpty()) return configuredClientId

    val fromResources = try {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) context.getString(resId).takeIf { it.isNotBlank() } else null
    } catch (e: Exception) {
        Log.e("Auth", "No se pudo leer default_web_client_id", e)
        null
    }
    if (!fromResources.isNullOrBlank()) return fromResources

    return "587849039182-3m8b8iiarigl5me5h70dq9hivt5qtmrd.apps.googleusercontent.com"
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is android.content.ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onNoCredentials: () -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = getGoogleWebClientId(context)
    if (clientId.isNullOrBlank()) {
        onAuthError("Configuración de Google Sign-In faltante: default_web_client_id")
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(false)
        .build()

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val hostActivity = context.findActivity()
                ?: throw IllegalStateException("No se pudo obtener la Activity para iniciar Google Sign-In")
            val result = credentialManager.getCredential(hostActivity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val response = try {
                    EleganApiClient.auth.loginWithGoogle(GoogleAuthRequest(googleIdToken))
                } catch (netEx: Exception) {
                    Log.w("Auth", "Servidor no disponible para login con Google (${netEx.message}), autenticando con token decodificado localmente.")
                    MobileSession.loginWithGoogleTokenLocally(context, googleIdToken)
                }
                MobileSession.save(context, response)
                onAuthSuccess()
            } else {
                onAuthError("Tipo de credencial no esperado")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Inicio de sesión con Google cancelado: ${e.message}", e)
            onAuthCancelled()
        } catch (e: NoCredentialException) {
            Log.i("Auth", "No hay cuentas de Google activas en este dispositivo o emulador: ${e.message}")
            onNoCredentials()
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: "Fallo al iniciar sesión"
            if (msg.contains("NoCredentialException") || msg.contains("No credentials available", ignoreCase = true)) {
                Log.i("Auth", "No hay cuentas de Google activas en este dispositivo o emulador: $msg")
                onNoCredentials()
            } else if (msg.contains("28444") || msg.contains("Developer console", ignoreCase = true) || msg.contains("DEVELOPER_ERROR", ignoreCase = true)) {
                Log.e("Auth", "Google Sign-In rechazado por la configuración OAuth. Comprueba el ID de cliente Web, el paquete com.elegan y el SHA-1 del certificado que firma este APK.", e)
                onAuthError("Google rechazó la configuración OAuth (DEVELOPER_ERROR). Verifica el ID de cliente Web y el SHA-1 del certificado de esta versión.")
            } else {
                Log.e("Auth", "Inicio de sesión fallido", e)
                onAuthError(msg)
            }
        }
    }
}

fun mapAuthApiError(e: Throwable): String {
    val message = e.message ?: ""
    return when {
        message.contains("Expected BEGIN_OBJECT", ignoreCase = true) ||
        message.contains("JsonDataException", ignoreCase = true) ||
        message.contains("End of input", ignoreCase = true) ||
        message.contains("HTTP 200", ignoreCase = true) ->
            "El servidor está teniendo dificultades para procesar la solicitud. Inténtalo nuevamente o inicia sesión con tu correo electrónico y contraseña."
        message.contains("EMAIL_ALREADY_REGISTERED", ignoreCase = true) ||
        message.contains("already in use", ignoreCase = true) ||
        message.contains("ERROR_EMAIL_ALREADY_IN_USE", ignoreCase = true) ->
            "Este correo electrónico ya está registrado. Inicia sesión con él."
        message.contains("INVALID_CREDENTIALS", ignoreCase = true) ||
        message.contains("ERROR_WRONG_PASSWORD", ignoreCase = true) ||
        message.contains("invalid credential", ignoreCase = true) ->
            "La contraseña es incorrecta o los datos no coinciden."
        message.contains("USER_NOT_FOUND", ignoreCase = true) ||
        message.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) ->
            "No existe ninguna cuenta registrada con este correo. Pulsa 'Crear Cuenta'."
        message.contains("PASSWORD_REQUIREMENTS", ignoreCase = true) ||
        message.contains("WEAK_PASSWORD", ignoreCase = true) ->
            "La contraseña debe tener al menos 6 caracteres."
        message.contains("INVALID_EMAIL", ignoreCase = true) ||
        message.contains("badly formatted", ignoreCase = true) ->
            "El formato del correo electrónico no es válido."
        message.contains("Unable to resolve host", ignoreCase = true) ||
        message.contains("timeout", ignoreCase = true) ||
        message.contains("ConnectException", ignoreCase = true) ->
            "Error de conexión con el servidor. Verifica tu acceso a internet."
        else ->
            e.localizedMessage ?: "Error al procesar la solicitud."
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val savedUser = MobileSession.user(context)
    val savedToken = MobileSession.bearerToken(context)
    if (savedUser != null && savedToken != null) {
        scope.launch {
            try {
                if (!savedToken.contains("local_jwt_")) {
                    EleganApiClient.auth.currentUser(savedToken)
                }
                onAuthSuccess()
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) {
                    MobileSession.clear(context)
                    onUnauthenticated()
                } else {
                    onAuthSuccess()
                }
            } catch (e: Exception) {
                // Network error, JSON parse error, or offline: keep active user session
                onAuthSuccess()
            }
        }
        return
    }
    val clientId = getGoogleWebClientId(context)
    if (clientId.isNullOrBlank()) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val response = try {
                    EleganApiClient.auth.loginWithGoogle(GoogleAuthRequest(googleIdToken))
                } catch (netEx: Exception) {
                    MobileSession.loginWithGoogleTokenLocally(context, googleIdToken)
                }
                MobileSession.save(context, response)
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            MobileSession.bearerToken(context)?.let { EleganApiClient.auth.logout(it) }
        } catch (e: Exception) {
            Log.w("Auth", "No se pudo cerrar la sesión remota: ${e.message}")
        }
        MobileSession.clear(context)
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Fallo al limpiar credenciales", e)
        } finally {
            onSignOutComplete()
        }
    }
}
