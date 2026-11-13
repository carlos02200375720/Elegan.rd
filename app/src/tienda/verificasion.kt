package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.data.model.UserProfile
import com.example.data.model.defaultBankAccounts
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.data.remote.MobileSession
import kotlinx.coroutines.launch

@Composable
fun VerificasionScreen(
    cartItems: List<CartItem>,
    subtotal: Double,
    discount: Double,
    shipping: Double,
    total: Double,
    userProfile: UserProfile?,
    onConfirmOrder: (name: String, email: String, phone: String, address: String, city: String, payment: String, paymentProofUri: String) -> Unit,
    onBack: () -> Unit
) {
    val user = MobileSession.user(LocalContext.current)
    var customerName by remember { mutableStateOf(user?.name ?: userProfile?.displayName ?: "") }
    var email by remember { mutableStateOf(user?.email ?: userProfile?.email ?: "") }
    var phone by remember { mutableStateOf(userProfile?.phone ?: "") }
    var address by remember { mutableStateOf(userProfile?.address ?: "") }
    var city by remember { mutableStateOf(userProfile?.city ?: "") }

    val cardPayPalOption = "Tarjeta de Crédito / Débito (Visa / Mastercard)"
    val cashOnDeliveryOption = "Pago Contra Entrega (Efectivo al recibir)"
    val bankTransferOption = "Transferencia Bancaria Inmediata"
    val paymentOptions = listOf(
        cardPayPalOption,
        cashOnDeliveryOption,
        bankTransferOption
    )
    var selectedPayment by remember { mutableStateOf(paymentOptions[0]) }
    var isSubmitting by remember { mutableStateOf(false) }

    var showBankTransferPage by remember { mutableStateOf(false) }
    var showPayPalPage by remember { mutableStateOf(false) }
    var showCashOnDeliveryPage by remember { mutableStateOf(false) }
    var paymentProofUri by remember { mutableStateOf<Uri?>(null) }
    val bankAccounts = remember { defaultBankAccounts() }

    val paymentProofPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            paymentProofUri = uri
        }
    }

    fun isDeliveryFormValid(): Boolean =
        customerName.isNotBlank() && email.isNotBlank() && address.isNotBlank() && city.isNotBlank()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = !showBankTransferPage && !showPayPalPage && !showCashOnDeliveryPage) { onBack() }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .testTag("verificasion_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = EleganCharcoal
                        )
                    }

                    Text(
                        text = "Verificación y Pago",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal,
                            fontFamily = FontFamily.Serif
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
                EleganTopBarShadow()
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                EleganBottomBarShadow()
                Surface(
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            if (!isDeliveryFormValid()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Por favor completa los datos de entrega requeridos.")
                                }
                                return@Button
                            }
                            when (selectedPayment) {
                                cardPayPalOption -> {
                                    showPayPalPage = true
                                }
                                cashOnDeliveryOption -> {
                                    showCashOnDeliveryPage = true
                                }
                                bankTransferOption -> {
                                    showBankTransferPage = true
                                }
                                else -> {
                                    isSubmitting = true
                                    onConfirmOrder(customerName, email, phone, address, city, selectedPayment, "")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .testTag("confirm_order_btn"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EleganBurgundy,
                            contentColor = Color.White
                        ),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Confirmar y Pagar $${String.format("%.2f", total)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
            }
        },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("verificasion_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transacción 100% segura y encriptada por Elegan SSL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = EleganBurgundy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Dirección de Entrega",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Nombre y Apellidos *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EleganBurgundy,
                            unfocusedBorderColor = Color(0xFFDDD5CE)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo Electrónico *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_email_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EleganBurgundy,
                            unfocusedBorderColor = Color(0xFFDDD5CE)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono de Contacto") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_phone_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EleganBurgundy,
                            unfocusedBorderColor = Color(0xFFDDD5CE)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Calle, Número y Apartamento *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_address_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EleganBurgundy,
                            unfocusedBorderColor = Color(0xFFDDD5CE)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Ciudad y Código Postal *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_city_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EleganBurgundy,
                            unfocusedBorderColor = Color(0xFFDDD5CE)
                        )
                    )
                }
            }

            item {
                HorizontalDivider(color = Color(0xFFF0EAE4))
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = EleganBurgundy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. Resumen de Artículos (${cartItems.sumOf { it.quantity }})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    for (item in cartItems) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF3ECE4)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = item.name,
                                        tint = EleganBurgundy.copy(alpha = 0.6f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = EleganCharcoal
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val details = buildList {
                                    add("Cant: ${item.quantity}")
                                    if (item.selectedSize.isNotBlank()) add("Talla: ${item.selectedSize}")
                                    if (item.selectedColor.isNotBlank()) add(item.selectedColor)
                                }.joinToString(" • ")
                                Text(
                                    text = details,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "$${String.format("%.2f", item.price * item.quantity)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganBurgundyDark
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFE5DDD5))
                    Spacer(modifier = Modifier.height(10.dp))

                    SummaryRow(label = "Subtotal", value = "$${String.format("%.2f", subtotal)}")
                    if (discount > 0.0) {
                        SummaryRow(label = "Descuento", value = "-$${String.format("%.2f", discount)}", valueColor = Color(0xFF1E7E34))
                    }
                    SummaryRow(label = "Envío", value = if (shipping == 0.0) "GRATIS" else "$${String.format("%.2f", shipping)}")

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total Final", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "$${String.format("%.2f", total)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = EleganBurgundyDark
                            )
                        )
                    }
                }
            }

            item {
                HorizontalDivider(color = Color(0xFFF0EAE4))
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = EleganBurgundy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3. Método de Pago",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    for (option in paymentOptions) {
                        val isSelected = option == selectedPayment
                        val onSelectOption = {
                            selectedPayment = option
                            when (option) {
                                cardPayPalOption -> {
                                    if (isDeliveryFormValid()) {
                                        showPayPalPage = true
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Completa los datos de entrega para continuar con el pago con tarjeta o PayPal.")
                                        }
                                    }
                                }
                                cashOnDeliveryOption -> {
                                    if (isDeliveryFormValid()) {
                                        showCashOnDeliveryPage = true
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Completa los datos de entrega para ver las instrucciones de pago contra entrega.")
                                        }
                                    }
                                }
                                bankTransferOption -> {
                                    if (isDeliveryFormValid()) {
                                        showBankTransferPage = true
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Completa los datos de entrega para continuar con la transferencia.")
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFF9F3EA) else Color.Transparent)
                                .clickable { onSelectOption() }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectOption() },
                                colors = RadioButtonDefaults.colors(selectedColor = EleganBurgundy)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = EleganCharcoal,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }

                    if (selectedPayment == cardPayPalOption) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFAF6F1),
                            border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isDeliveryFormValid()) {
                                        showPayPalPage = true
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Por favor completa los datos de entrega requeridos.")
                                        }
                                    }
                                }
                                .testTag("open_paypal_page_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = EleganBurgundy,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pagar con Tarjeta o PayPal dentro de la app",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = EleganCharcoal,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    } else if (selectedPayment == cashOnDeliveryOption) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFAF6F1),
                            border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isDeliveryFormValid()) {
                                        showCashOnDeliveryPage = true
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Por favor completa los datos de entrega requeridos.")
                                        }
                                    }
                                }
                                .testTag("open_cash_on_delivery_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = EleganBurgundy,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ver instrucciones y compromiso contra entrega",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = EleganCharcoal,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    } else if (selectedPayment == bankTransferOption) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFAF6F1),
                            border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isDeliveryFormValid()) {
                                        showBankTransferPage = true
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Por favor completa los datos de entrega requeridos.")
                                        }
                                    }
                                }
                                .testTag("open_bank_transfer_sheet_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (paymentProofUri != null) Icons.Default.CheckCircle else Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = if (paymentProofUri != null) Color(0xFF2E7D32) else EleganBurgundy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (paymentProofUri != null) {
                                            "Comprobante adjuntado • Ver cuentas"
                                        } else {
                                            "Ver cuentas bancarias y subir comprobante"
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = EleganCharcoal,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    AnimatedVisibility(
        visible = showBankTransferPage,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 320)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 280)
        )
    ) {
        TranfereciaScreen(
            total = total,
            bankAccounts = bankAccounts,
            paymentProofUri = paymentProofUri,
            isSubmitting = isSubmitting,
            onBack = { showBankTransferPage = false },
            onPickPaymentProof = {
                paymentProofPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onRemovePaymentProof = { paymentProofUri = null },
            onConfirmPayment = {
                if (paymentProofUri != null && !isSubmitting) {
                    isSubmitting = true
                    onConfirmOrder(
                        customerName,
                        email,
                        phone,
                        address,
                        city,
                        selectedPayment,
                        paymentProofUri.toString()
                    )
                }
            }
        )
    }

    AnimatedVisibility(
        visible = showPayPalPage,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 320)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 280)
        )
    ) {
        PaypalScreen(
            total = total,
            customerName = customerName,
            customerEmail = email,
            itemCount = cartItems.sumOf { it.quantity },
            itemsSummary = cartItems.joinToString(", ") { "${it.quantity}x ${it.name}" },
            isSubmittingOrder = isSubmitting,
            onBack = { showPayPalPage = false },
            onPaymentSuccess = { paymentLabel, _ ->
                if (!isSubmitting) {
                    isSubmitting = true
                    onConfirmOrder(
                        customerName,
                        email,
                        phone,
                        address,
                        city,
                        paymentLabel,
                        ""
                    )
                }
            }
        )
    }

    AnimatedVisibility(
        visible = showCashOnDeliveryPage,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 320)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 280)
        )
    ) {
        ContraEntregaScreen(
            total = total,
            customerName = customerName,
            phone = phone,
            address = address,
            city = city,
            itemCount = cartItems.sumOf { it.quantity },
            isSubmitting = isSubmitting,
            onBack = { showCashOnDeliveryPage = false },
            onConfirmOrder = {
                if (!isSubmitting) {
                    isSubmitting = true
                    onConfirmOrder(
                        customerName,
                        email,
                        phone,
                        address,
                        city,
                        cashOnDeliveryOption,
                        ""
                    )
                }
            }
        )
    }
    }
}
