package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
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
const val FIREBASE_PROJECT_ID = "carlos-garcia-509521"
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
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFAF7F2)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo & Brand Header
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, EleganGold.copy(alpha = 0.5f), CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.icono),
                            contentDescription = "Logo Elegan",
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ELEGAN",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp,
                            color = EleganBurgundyDark,
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
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Tab Selector: Google, Correo, Acceso Rápido
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFFEFE8DE),
                        contentColor = EleganBurgundy,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = EleganBurgundy,
                                    height = 3.dp
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
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
                                    fontSize = 12.5.sp,
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
                                    fontSize = 12.5.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = {
                                selectedTab = 2
                                errorMessage = null
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = EleganGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "Acceso Rápido",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Content according to selected tab
                    when (selectedTab) {
                        0 -> {
                            // GOOGLE SIGN IN TAB
                            Text(
                                text = "Inicia sesión con tu cuenta de Google para sincronizar tus pedidos, catálogo guardado y compras en la tienda.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF555555),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

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
                                    .height(52.dp)
                                    .testTag("google_sign_in_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EleganBurgundy,
                                    contentColor = Color.White
                                ),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
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
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Iniciar sesión con Google",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2D9D2))
                                Text(
                                    text = " O ACCESO RÁPIDO ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7A6B62),
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2D9D2))
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Direct Client Button
                            Button(
                                onClick = {
                                    isLoading = true
                                    errorMessage = null
                                    signInAsClient(
                                        onAuthSuccess = {
                                            isLoading = false
                                            onAuthSuccess()
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        },
                                        scope = coroutineScope,
                                        context = context
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("tab0_quick_client_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EleganGold,
                                    contentColor = Color(0xFF2B050B)
                                ),
                                enabled = !isLoading
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Ingreso Rápido como Cliente",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Direct Admin Button
                            OutlinedButton(
                                onClick = {
                                    isLoading = true
                                    errorMessage = null
                                    signInAsAdmin(
                                        onAuthSuccess = {
                                            isLoading = false
                                            onAuthSuccess()
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        },
                                        scope = coroutineScope
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("tab0_quick_admin_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = EleganBurgundyDark
                                ),
                                border = BorderStroke(1.2.dp, EleganBurgundy),
                                enabled = !isLoading
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = EleganBurgundy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Ingreso Administrador ($DEFAULT_SUPER_ADMIN_EMAIL)",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                        }

                        1 -> {
                            // EMAIL & PASSWORD TAB
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_confirm_password_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EleganBurgundy,
                                        unfocusedBorderColor = Color(0xFFDDD5CE)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    val email = emailInput.trim()
                                    val password = passwordInput
                                    if (email.isBlank() || !email.contains("@")) {
                                        errorMessage = "Por favor ingresa un correo electrónico válido."
                                        return@Button
                                    }
                                    if (password.length < 8) {
                                        errorMessage = "La contraseña debe tener al menos 8 caracteres."
                                        return@Button
                                    }
                                    if (isRegisterMode && password != confirmPasswordInput) {
                                        errorMessage = "Las contraseñas no coinciden."
                                        return@Button
                                    }

                                    isLoading = true
                                    errorMessage = null

                                    coroutineScope.launch {
                                        try {
                                            val response = if (isRegisterMode) {
                                                EleganApiClient.auth.register(
                                                    RegisterRequest(nameInput.trim().ifBlank { "Caballero Elegan" }, email, password)
                                                )
                                            } else {
                                                EleganApiClient.auth.login(LoginRequest(email, password))
                                            }
                                            MobileSession.save(context, response)
                                            isLoading = false
                                            onAuthSuccess()
                                        } catch (e: Exception) {
                                            isLoading = false
                                            Log.e("Auth", "Error al procesar correo/contraseña", e)
                                            errorMessage = mapFirebaseError(e)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("email_submit_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EleganBurgundy,
                                    contentColor = Color.White
                                ),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (isRegisterMode) "Crear Mi Cuenta Elegan" else "Iniciar Sesión",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            // Pre-fill Admin button for convenience
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    emailInput = DEFAULT_SUPER_ADMIN_EMAIL
                                    if (passwordInput.isBlank()) {
                                        passwordInput = "AdminElegan2026!"
                                    }
                                    isRegisterMode = false
                                    errorMessage = null
                                }
                            ) {
                                Text(
                                    "¿Eres el Administrador? Cargar $DEFAULT_SUPER_ADMIN_EMAIL",
                                    fontSize = 11.5.sp,
                                    color = EleganBurgundy
                                )
                            }
                        }

                        2 -> {
                            // FAST ACCESS (1-TAP CLIENT LOGIN)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5EFEB)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlashOn,
                                            contentDescription = null,
                                            tint = EleganGold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Ingreso Inmediato",
                                            fontWeight = FontWeight.Bold,
                                            color = EleganBurgundyDark,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Para que puedas explorar la tienda y guardar publicaciones de inmediato sin complicaciones, pulsa el botón a continuación.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF555555),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            isLoading = true
                                            errorMessage = null
                                            signInAsClient(
                                                onAuthSuccess = {
                                                    isLoading = false
                                                    onAuthSuccess()
                                                },
                                                onError = { err ->
                                                    isLoading = false
                                                    errorMessage = err
                                                },
                                                scope = coroutineScope
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("quick_client_sign_in_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EleganGold,
                                            contentColor = Color(0xFF2B050B)
                                        ),
                                        enabled = !isLoading
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color(0xFF2B050B),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "Ingresar como Cliente Elegan",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
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

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = EleganGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exclusividad, Calidad & Confianza",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF777777)
                            )
                        )
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "El emulador o teléfono actual no tiene ninguna cuenta de Google iniciada en el sistema Android.\n\n" +
                                "Puedes ingresar de inmediato con los botones a continuación, o añadir una cuenta al dispositivo:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF444444),
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            showNoAccountDialog = false
                            try {
                                val intent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                                    putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_SYNC_SETTINGS))
                                } catch (e2: Exception) {
                                    Toast.makeText(context, "Abre Ajustes > Cuentas en Android para añadir tu cuenta", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EleganBurgundy)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Añadir Cuenta Google al Dispositivo", fontSize = 12.5.sp)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showNoAccountDialog = false
                            isLoading = true
                            errorMessage = null
                            signInAsAdmin(
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onError = { err ->
                                    isLoading = false
                                    errorMessage = err
                                },
                                scope = coroutineScope
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy)
                    ) {
                        Text("Entrar Admin")
                    }
                    Button(
                        onClick = {
                            showNoAccountDialog = false
                            isLoading = true
                            errorMessage = null
                            signInAsClient(
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onError = { err ->
                                    isLoading = false
                                    errorMessage = err
                                },
                                scope = coroutineScope
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EleganGold, contentColor = Color(0xFF2B050B))
                    ) {
                        Text("Entrar Cliente")
                    }
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

    return try {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) context.getString(resId).takeIf { it.isNotBlank() } else null
    } catch (e: Exception) {
        Log.e("Auth", "No se pudo leer default_web_client_id", e)
        null
    }
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
                val response = EleganApiClient.auth.loginWithGoogle(GoogleAuthRequest(googleIdToken))
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

fun signInAsAdmin(
    context: Context,
    onAuthSuccess: () -> Unit,
    onError: (String) -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            val response = EleganApiClient.auth.login(
                LoginRequest(DEFAULT_SUPER_ADMIN_EMAIL, "EleganAdmin2026!")
            )
            require(response.user.role == "admin") { "Esta cuenta no tiene permisos de administrador en el servidor." }
            MobileSession.save(context, response)
            onAuthSuccess()
        } catch (e: Exception) {
            Log.e("Auth", "Error en acceso de administrador", e)
            onError(mapFirebaseError(e))
        }
    }
}

fun signInAsClient(
    context: Context,
    onAuthSuccess: () -> Unit,
    onError: (String) -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            val response = EleganApiClient.auth.login(
                LoginRequest("cliente.elegan@gmail.com", "EleganCliente2026!")
            )
            MobileSession.save(context, response)
            onAuthSuccess()
        } catch (e: Exception) {
            Log.e("Auth", "Error en acceso rápido de cliente", e)
            onError(mapFirebaseError(e))
        }
    }
}

fun mapFirebaseError(e: Throwable): String {
    val message = e.message ?: ""
    return when {
        message.contains("ERROR_INVALID_EMAIL", ignoreCase = true) || message.contains("badly formatted", ignoreCase = true) ->
            "El formato del correo electrónico no es válido."
        message.contains("ERROR_WRONG_PASSWORD", ignoreCase = true) || message.contains("invalid credential", ignoreCase = true) ->
            "La contraseña es incorrecta o la cuenta no existe."
        message.contains("ERROR_USER_NOT_FOUND", ignoreCase = true) ->
            "No existe ninguna cuenta registrada con este correo."
        message.contains("ERROR_EMAIL_ALREADY_IN_USE", ignoreCase = true) || message.contains("already in use", ignoreCase = true) ->
            "Este correo electrónico ya está registrado. Inicia sesión con él."
        message.contains("WEAK_PASSWORD", ignoreCase = true) ->
            "La contraseña debe tener al menos 6 caracteres."
        message.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) || message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
            "Verifica que el servicio de autenticación de Elegan esté configurado correctamente."
        message.contains("NETWORK_ERROR", ignoreCase = true) || message.contains("network", ignoreCase = true) ->
            "Error de conexión. Verifica tu acceso a internet."
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
    val savedToken = MobileSession.bearerToken(context)
    if (savedToken != null) {
        scope.launch {
            try {
                EleganApiClient.auth.currentUser(savedToken)
                onAuthSuccess()
            } catch (e: Exception) {
                MobileSession.clear(context)
                onUnauthenticated()
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
                val response = EleganApiClient.auth.loginWithGoogle(GoogleAuthRequest(googleIdToken))
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
