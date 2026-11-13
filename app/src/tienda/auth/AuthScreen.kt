package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.remote.EleganApiClient
import com.example.data.remote.LoginRequest
import com.example.data.remote.MobileSession
import com.example.data.remote.RegisterRequest
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 0: Iniciar Sesión, 1: Crear Cuenta
    var selectedTab by remember { mutableIntStateOf(0) }

    // Campos del formulario
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val submitAuth: () -> Unit = {
        val isRegister = selectedTab == 1
        val email = emailInput.trim()
        val password = passwordInput
        val name = nameInput.trim()

        if (isRegister && name.isBlank()) {
            errorMessage = "Por favor ingresa tu nombre completo."
        } else if (email.isBlank() || !email.contains("@")) {
            errorMessage = "Por favor ingresa un correo electrónico válido."
        } else if (password.length < 6) {
            errorMessage = "La contraseña debe tener al menos 6 caracteres."
        } else if (isRegister && password != confirmPasswordInput) {
            errorMessage = "Las contraseñas no coinciden."
        } else {
            isLoading = true
            errorMessage = null

            coroutineScope.launch {
                try {
                    val response = if (isRegister) {
                        val displayName = name.ifBlank { "Cliente Elegan" }
                        try {
                            EleganApiClient.auth.register(RegisterRequest(displayName, email, password))
                        } catch (netEx: CancellationException) {
                            throw netEx
                        } catch (netEx: Exception) {
                            Log.w("Auth", "Servidor no disponible para registro (${netEx.message}), registrando localmente.")
                            MobileSession.registerLocalUser(context, displayName, email, password)
                        }
                    } else {
                        try {
                            EleganApiClient.auth.login(LoginRequest(email, password))
                        } catch (netEx: CancellationException) {
                            throw netEx
                        } catch (netEx: Exception) {
                            Log.w("Auth", "Servidor no disponible para login (${netEx.message}), verificando credenciales locales.")
                            MobileSession.loginLocalUser(context, email, password)
                        }
                    }
                    MobileSession.save(context, response)
                    onAuthSuccess()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("Auth", "Error de autenticación", e)
                    errorMessage = mapAuthApiError(e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Logotipo de Elegan
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_fg),
                    contentDescription = "Logo Elegan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .graphicsLayer(scaleX = 1.35f, scaleY = 1.35f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ELEGAN",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 4.sp,
                    color = EleganBurgundyDark
                )
            )

            Text(
                text = "Moda & Accesorios Masculinos de Lujo",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF777777),
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Selector de pestaña: Iniciar Sesión / Crear Cuenta
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
                            "Iniciar Sesión",
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
                            "Crear Cuenta",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Campos del formulario
            if (selectedTab == 1) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nombre completo") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = EleganBurgundy
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EleganBurgundy,
                        unfocusedBorderColor = Color(0xFFDDD5CE)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))
            }

            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Correo electrónico") },
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

            Spacer(modifier = Modifier.height(14.dp))

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
                    imeAction = if (selectedTab == 1) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (selectedTab == 0) submitAuth() }
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

            if (selectedTab == 1) {
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = confirmPasswordInput,
                    onValueChange = { confirmPasswordInput = it },
                    label = { Text("Confirmar contraseña") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
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
                        onDone = { submitAuth() }
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

            // Mensaje de error si ocurre alguno
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
                    border = BorderStroke(1.dp, Color(0xFFFFCCCC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFD32F2F),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón principal
            Button(
                onClick = { submitAuth() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_submit_button"),
                shape = RoundedCornerShape(10.dp),
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
                        text = if (selectedTab == 1) "Crear Mi Cuenta Elegan" else "Iniciar Sesión",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    val guestUser = MobileSession.loginClientLocally(context)
                    MobileSession.save(context, guestUser)
                    onAuthSuccess()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_guest_button"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.5.dp, EleganBurgundy),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = EleganBurgundy
                )
            ) {
                Text(
                    text = "Ingresar como Invitado",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val savedUser = MobileSession.user(context)
    if (savedUser != null) {
        val remoteToken = MobileSession.remoteBearerToken(context)
        if (remoteToken == null) {
            onAuthSuccess()
            return
        }
        scope.launch {
            try {
                EleganApiClient.auth.currentUser(remoteToken)
                onAuthSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401) {
                    MobileSession.clear(context)
                    onUnauthenticated()
                } else {
                    onAuthSuccess()
                }
            } catch (_: Exception) {
                onAuthSuccess()
            }
        }
    } else {
        onUnauthenticated()
    }
}

fun signOutUser(
    context: Context,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            MobileSession.remoteBearerToken(context)?.let { EleganApiClient.auth.logout(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Auth", "No se pudo cerrar la sesión remota: ${e.message}")
        }
        MobileSession.clear(context)
        onSignOutComplete()
    }
}

fun mapAuthApiError(e: Throwable): String {
    val message = e.message ?: ""
    return when {
        message.contains("Expected BEGIN_OBJECT", ignoreCase = true) ||
        message.contains("JsonDataException", ignoreCase = true) ||
        message.contains("End of input", ignoreCase = true) ||
        message.contains("HTTP 200", ignoreCase = true) ->
            "El servidor está procesando solicitudes. Inténtalo nuevamente o ingresa como Invitado."
        message.contains("EMAIL_ALREADY_REGISTERED", ignoreCase = true) ||
        message.contains("email already exists", ignoreCase = true) ||
        message.contains("ya registrado", ignoreCase = true) ->
            "Este correo electrónico ya está registrado. Selecciona Iniciar Sesión."
        message.contains("INVALID_CREDENTIALS", ignoreCase = true) ||
        message.contains("401", ignoreCase = true) ||
        message.contains("incorrecta", ignoreCase = true) ->
            "Correo electrónico o contraseña incorrectos."
        message.contains("Unable to resolve host", ignoreCase = true) ||
        message.contains("ConnectException", ignoreCase = true) ||
        message.contains("timeout", ignoreCase = true) ->
            "No se pudo conectar con el servidor. Verifica tu conexión a internet o ingresa como Invitado."
        else ->
            e.localizedMessage ?: "Ocurrió un error al iniciar sesión. Inténtalo de nuevo."
    }
}
