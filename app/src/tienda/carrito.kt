package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarritoScreen(
    cartItems: List<CartItem>,
    onUpdateQuantity: (String, Long) -> Unit,
    onRemoveItem: (String) -> Unit,
    onNavigateToVerificasion: (Double, Double, Double, Double) -> Unit,
    onExploreCatalog: () -> Unit,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    var couponCode by remember { mutableStateOf("") }
    var discountPercent by remember { mutableDoubleStateOf(0.0) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val subtotal by remember(cartItems) {
        derivedStateOf {
            cartItems.sumOf { it.price * it.quantity }
        }
    }

    val discountAmount by remember(subtotal, discountPercent) {
        derivedStateOf {
            subtotal * discountPercent
        }
    }

    val shippingCost by remember(subtotal) {
        derivedStateOf {
            if (subtotal >= 100.0 || subtotal == 0.0) 0.0 else 12.0
        }
    }

    val total by remember(subtotal, discountAmount, shippingCost) {
        derivedStateOf {
            (subtotal - discountAmount + shippingCost).coerceAtLeast(0.0)
        }
    }

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
                            .testTag("cart_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = EleganCharcoal
                        )
                    }

                    Text(
                        text = "Mi Carrito",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal,
                            fontFamily = FontFamily.Serif
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 56.dp)
                    )

                    val totalItems = cartItems.sumOf { it.quantity }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (totalItems > 0) EleganBurgundy else Color(0xFFDDD5CE),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                            .testTag("cart_items_indicator")
                    ) {
                        Text(
                            text = "$totalItems",
                            color = if (totalItems > 0) Color.White else EleganCharcoal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
                EleganTopBarShadow()
            }
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    EleganBottomBarShadow()
                    Surface(
                        color = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            val isCompactBar = maxWidth < 350.dp
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 680.dp)
                                    .padding(
                                        horizontal = if (isCompactBar) 12.dp else 16.dp,
                                        vertical = 8.dp
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Total a Pagar",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$${String.format("%.2f", total)}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EleganBurgundyDark,
                                            fontSize = if (isCompactBar) 15.sp else 16.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        onNavigateToVerificasion(subtotal, discountAmount, shippingCost, total)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EleganBurgundy,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = if (isCompactBar) 14.dp else 16.dp,
                                        vertical = 0.dp
                                    ),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .testTag("checkout_continue_btn")
                                ) {
                                    Text(
                                        text = "Continuar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            val isCompact = maxWidth < 350.dp
            val isWide = maxWidth >= 600.dp
            val horizontalPad = when {
                isCompact -> 12.dp
                isWide -> 24.dp
                else -> 16.dp
            }

            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isCompact) 20.dp else 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.widthIn(max = 440.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 86.dp else 100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3ECE4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RemoveShoppingCart,
                                contentDescription = null,
                                tint = EleganBurgundy,
                                modifier = Modifier.size(if (isCompact) 40.dp else 48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Tu Carrito está Vacío",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal,
                                fontFamily = FontFamily.Serif,
                                fontSize = if (isCompact) 20.sp else 22.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Explora nuestro catálogo para descubrir prendas exclusivas, boinas, calzado y fragancias.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = if (isCompact) 8.dp else 24.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onExploreCatalog,
                            colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("explore_catalog_from_empty_cart_btn")
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Explorar Catálogo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .testTag("cart_items_list"),
                    contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    items(cartItems, key = { it.id }) { item ->
                        CartItemRow(
                            item = item,
                            onIncrease = { onUpdateQuantity(item.id, item.quantity + 1) },
                            onDecrease = {
                                if (item.quantity > 1) {
                                    onUpdateQuantity(item.id, item.quantity - 1)
                                } else {
                                    onRemoveItem(item.id)
                                }
                            },
                            onRemove = { onRemoveItem(item.id) }
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(if (isCompact) 12.dp else 16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ConfirmationNumber,
                                        contentDescription = null,
                                        tint = EleganGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cupón de Descuento",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EleganCharcoal
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = couponCode,
                                        onValueChange = { couponCode = it.uppercase() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("coupon_input"),
                                        placeholder = { Text("Ej: ELEGAN10", fontSize = 13.sp, maxLines = 1) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EleganBurgundy,
                                            unfocusedBorderColor = Color(0xFFDDD5CE)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 8.dp))
                                    Button(
                                        onClick = {
                                            if (couponCode == "ELEGAN10" || couponCode == "VIP") {
                                                discountPercent = 0.10
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("¡Cupón aplicado: 10% de descuento!")
                                                }
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Cupón inválido. Prueba con ELEGAN10")
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(
                                            horizontal = if (isCompact) 12.dp else 16.dp,
                                            vertical = 10.dp
                                        ),
                                        modifier = Modifier.testTag("apply_coupon_btn")
                                    ) {
                                        Text("Aplicar", fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }

                                if (discountPercent > 0.0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "✓ 10% de descuento activado",
                                        color = Color(0xFF1E7E34),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(if (isCompact) 12.dp else 16.dp)) {
                            Text(
                                text = "Resumen del Pedido",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            SummaryRow(label = "Subtotal", value = "$${String.format("%.2f", subtotal)}")

                            if (discountAmount > 0.0) {
                                SummaryRow(
                                    label = "Descuento (10%)",
                                    value = "-$${String.format("%.2f", discountAmount)}",
                                    valueColor = Color(0xFF1E7E34)
                                )
                            }

                            SummaryRow(
                                label = "Envío",
                                value = if (shippingCost == 0.0) "GRATIS" else "$${String.format("%.2f", shippingCost)}",
                                valueColor = if (shippingCost == 0.0) Color(0xFF1E7E34) else EleganCharcoal
                            )

                            if (subtotal < 100.0) {
                                Text(
                                    text = "Agrega $${String.format("%.2f", 100.0 - subtotal)} más para envío gratuito.",
                                    color = EleganGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFE5DDD5))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Final",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal
                                    )
                                )
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
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("cart_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompactCard = maxWidth < 330.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isCompactCard) 10.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isCompactCard) 68.dp else 80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3ECE4))
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(if (isCompactCard) 10.dp else 14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EleganCharcoal,
                            fontSize = if (isCompactCard) 13.sp else 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.selectedSize.isNotBlank()) {
                        Text(
                            text = "Talla: ${item.selectedSize}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.selectedColor.isNotBlank()) {
                        Text(
                            text = "Color: ${item.selectedColor}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$${String.format("%.2f", item.price)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganBurgundyDark,
                            fontSize = if (isCompactCard) 15.sp else 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF6EFE9))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = onDecrease,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos", tint = EleganBurgundy, modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "${item.quantity}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = onIncrease,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Más", tint = EleganBurgundy, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(if (isCompactCard) 40.dp else 48.dp)
                        .testTag("remove_item_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFB00020)
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = EleganCharcoal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        )
    }
}
