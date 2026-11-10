package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.ui.components.SavedPublicationsGrid
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory
import com.example.data.remote.MobileSession
import kotlinx.coroutines.launch

const val SUPER_ADMIN_EMAIL = "cg0220037@gmail.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    userProfile: UserProfile?,
    orders: List<Order>,
    savedProducts: List<Product> = emptyList(),
    cartItemCount: Long = 0L,
    onNavigateToCart: () -> Unit = {},
    onProductClick: (Product) -> Unit = {},
    onExploreClick: () -> Unit = {},
    onSaveProfile: (phone: String, address: String, city: String) -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToPublicar: () -> Unit = {},
    onNavigateToMenuAdmin: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val user = remember { MobileSession.user(context) }
    val currentEmail = (user?.email ?: userProfile?.email ?: "").trim()
    val isSuperAdmin = user?.role == "admin"

    var phone by remember(userProfile) { mutableStateOf(userProfile?.phone ?: "") }
    var address by remember(userProfile) { mutableStateOf(userProfile?.address ?: "") }
    var city by remember(userProfile) { mutableStateOf(userProfile?.city ?: "") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var selectedProfileSection by remember { mutableStateOf("saved") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .testTag("perfil_screen"),
                state = listState,
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(top = 50.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val photoUrl = user?.photoUrl?.toString() ?: userProfile?.photoUrl
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_avatar_gold),
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = user?.name ?: userProfile?.displayName ?: "Caballero Elegan",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal,
                                    fontFamily = FontFamily.Serif
                                )
                            )

                            Text(
                                text = user?.email ?: userProfile?.email ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSuperAdmin) EleganBurgundyDark else EleganBurgundyDark
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSuperAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Diamond,
                                        contentDescription = null,
                                        tint = EleganGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSuperAdmin) "SUPER ADMINISTRADOR" else "MIEMBRO CLUB ELEGAN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                        }

                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("profile_sections_container"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ProfileMenuItem(
                                    label = "Guardados",
                                    icon = Icons.Default.Bookmark,
                                    testTag = "profile_saved_menu",
                                    onClick = { selectedProfileSection = "saved" },
                                    selected = selectedProfileSection == "saved"
                                )
                                ProfileMenuItem(
                                    label = "Pedidos",
                                    icon = Icons.Default.ReceiptLong,
                                    testTag = "profile_orders_menu",
                                    onClick = { selectedProfileSection = "orders" },
                                    selected = selectedProfileSection == "orders"
                                )
                                ProfileMenuItem(
                                    label = "Entrega",
                                    icon = Icons.Default.LocationOn,
                                    testTag = "profile_delivery_menu",
                                    onClick = { selectedProfileSection = "delivery" },
                                    selected = selectedProfileSection == "delivery"
                                )
                                ProfileMenuItem(
                                    label = "Ajustes",
                                    icon = Icons.Default.Settings,
                                    testTag = "profile_settings_menu",
                                    onClick = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "La configuración estará disponible próximamente"
                                            )
                                        }
                                    }
                                )
                            }
                            HorizontalDivider(color = Color(0xFFEAE5E0))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                when (selectedProfileSection) {
                                    "saved" -> SavedPublicationsGrid(
                                        savedProducts = savedProducts,
                                        onProductClick = onProductClick,
                                        onExploreClick = onExploreClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("profile_saved_section")
                                    )
                                    "delivery" -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = EleganBurgundy)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Mis Datos de Entrega",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = EleganCharcoal
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        OutlinedTextField(
                                            value = phone,
                                            onValueChange = { phone = it },
                                            label = { Text("Teléfono de Contacto") },
                                            modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EleganBurgundy,
                                                unfocusedBorderColor = Color(0xFFDDD5CE)
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = address,
                                            onValueChange = { address = it },
                                            label = { Text("Dirección Habitual") },
                                            modifier = Modifier.fillMaxWidth().testTag("profile_address_input"),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EleganBurgundy,
                                                unfocusedBorderColor = Color(0xFFDDD5CE)
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = city,
                                            onValueChange = { city = it },
                                            label = { Text("Ciudad / Provincia") },
                                            modifier = Modifier.fillMaxWidth().testTag("profile_city_input"),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = EleganBurgundy,
                                                unfocusedBorderColor = Color(0xFFDDD5CE)
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Button(
                                            onClick = {
                                                onSaveProfile(phone, address, city)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Datos guardados correctamente")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("save_profile_btn")
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Guardar Cambios")
                                        }
                                    }
                                    "orders" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = EleganBurgundy)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Mis Pedidos Recientes (${orders.size})",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = EleganCharcoal
                                                )
                                            )
                                        }
                                        if (orders.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Aún no has realizado ningún pedido en Elegan.",
                                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                                                )
                                            }
                                        } else {
                                            orders.forEachIndexed { index, order ->
                                                if (index > 0) HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 12.dp),
                                                    color = Color(0xFFEAE5E0)
                                                )
                                                OrderHistoryItemCard(order = order)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Atajos y herramientas fuera de las pestañas del perfil.
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF26292E)),
                        onClick = onNavigateToChats
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = EleganGold)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Soporte & Asesoría VIP", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Chatea en directo con Elegan Concierge", color = Color(0xFFBBBBBB), style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = EleganGold)
                        }
                    }
                }

                if (isSuperAdmin) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag("profile_admin_publicar_card"),
                            colors = CardDefaults.cardColors(containerColor = EleganBurgundyDark),
                            onClick = onNavigateToPublicar
                        ) {
                            Text(
                                "Publicar Producto o Anuncio",
                                modifier = Modifier.padding(18.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).testTag("profile_admin_menu_card"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            onClick = onNavigateToMenuAdmin
                        ) {
                            Text(
                                "Menú Administrativo",
                                modifier = Modifier.padding(18.dp),
                                color = EleganCharcoal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Sign out button
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(48.dp)
                            .testTag("sign_out_button"),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFB00020)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cerrar Sesión",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }

            // Fondo blanco fijo para la barra de estado
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(Color.White)
            )

            // Cabecera fija con fondo transparente
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(48.dp)
                    .background(Color.Transparent)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSuperAdmin) {
                    IconButton(
                        onClick = onNavigateToMenuAdmin,
                        modifier = Modifier.testTag("perfil_menu_admin_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menú Administración",
                            tint = EleganCharcoal
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Icono del carrito en la esquina superior derecha de la cabecera fija
                IconButton(
                    onClick = onNavigateToCart,
                    modifier = Modifier.testTag("perfil_cart_button")
                ) {
                    if (cartItemCount > 0L) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = EleganBurgundy,
                                    contentColor = Color.White
                                ) {
                                    Text("$cartItemCount", fontSize = 10.sp)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Carrito",
                                tint = EleganCharcoal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Carrito",
                            tint = EleganCharcoal,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun ProfileMenuItem(
    label: String,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) EleganBurgundy else Color(0xFF8A817A),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (selected) EleganBurgundy else EleganCharcoal,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        )
        if (selected) {
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(2.dp)
                    .background(EleganBurgundy, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun OrderHistoryItemCard(order: Order, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pedido #${order.id.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganBurgundyDark
                    )
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = order.status.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Entrega en: ${order.address}, ${order.city}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )

            if (order.itemsSummary.isNotBlank()) {
                Text(
                    text = order.itemsSummary,
                    style = MaterialTheme.typography.bodySmall.copy(color = EleganCharcoal),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Método: ${order.paymentMethod}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
                Text(
                    text = "Total: $${String.format("%.2f", order.total)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganBurgundy
                    )
                )
            }
    }
}
