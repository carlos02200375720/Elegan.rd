package com.example.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.EleganTopBarShadow
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.data.remote.MobileSession

const val SUPER_ADMIN_EMAIL = "cg0220037@gmail.com"

@Composable
fun MenuAdminScreen(
    onNavigateToPublicarProducto: () -> Unit,
    onNavigateToPublicarAnuncio: () -> Unit,
    onNavigateToPublicaciones: () -> Unit = {},
    onNavigateToPerfil: () -> Unit,
    onNavigateToInicio: () -> Unit = {},
    onNavigateToCatalogo: () -> Unit = {},
    onNavigateToCategorias: () -> Unit = {},
    onNavigateToChats: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val currentUser = MobileSession.user(LocalContext.current)
    val isSuperAdmin = currentUser?.role == "admin"

    if (!isSuperAdmin) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = Color.White
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Acceso Restringido",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Este panel administrativo está reservado exclusivamente para el usuario Super Administrador ($SUPER_ADMIN_EMAIL).",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundyDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Volver a Mi Perfil", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("admin_menu_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar menú",
                                tint = EleganCharcoal
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = "ELEGAN",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal,
                                fontFamily = FontFamily.Serif,
                                letterSpacing = 2.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            color = EleganBurgundy.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = EleganBurgundy,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ADMIN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EleganBurgundy,
                                    letterSpacing = 0.8.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                EleganTopBarShadow()
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            val horizontalPad = if (maxWidth < 360.dp) 12.dp else 16.dp
            val itemSpacing = if (maxWidth < 360.dp) 10.dp else 12.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .testTag("admin_menu_list"),
                contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                item {
                    Text(
                        text = "ACCESOS PRINCIPALES",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                item {
                    AdminMenuPrimaryLinkCard(
                        icon = Icons.Default.ShoppingBag,
                        iconTint = EleganBurgundy,
                        iconContainerColor = EleganBurgundy.copy(alpha = 0.12f),
                        title = "Publicar Producto",
                        onClick = onNavigateToPublicarProducto,
                        testTag = "menuadmin_link_publicar_producto"
                    )
                }

                item {
                    AdminMenuPrimaryLinkCard(
                        icon = Icons.Default.Campaign,
                        iconTint = EleganBurgundy,
                        iconContainerColor = EleganGold.copy(alpha = 0.18f),
                        title = "Publicar Anuncio",
                        onClick = onNavigateToPublicarAnuncio,
                        testTag = "menuadmin_link_publicar_anuncio"
                    )
                }

                item {
                    AdminMenuPrimaryLinkCard(
                        icon = Icons.Default.Inventory2,
                        iconTint = EleganBurgundy,
                        iconContainerColor = EleganGold.copy(alpha = 0.18f),
                        title = "Control de Publicaciones",
                        onClick = onNavigateToPublicaciones,
                        testTag = "menuadmin_link_publicasione"
                    )
                }

                item {
                    AdminMenuPrimaryLinkCard(
                        icon = Icons.Default.AccountCircle,
                        iconTint = Color(0xFF26292E),
                        iconContainerColor = Color(0xFF26292E).copy(alpha = 0.1f),
                        title = "Mi Perfil de Usuario",
                        onClick = onNavigateToPerfil,
                        testTag = "menuadmin_link_perfil"
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "NAVEGACIÓN RÁPIDA DE TIENDA",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFECE7E0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AdminMenuNavRow(
                                icon = Icons.Default.Home,
                                title = "Página de Inicio",
                                onClick = onNavigateToInicio,
                                testTag = "menuadmin_nav_inicio"
                            )
                            HorizontalDivider(color = Color(0xFFF0EBE3), modifier = Modifier.padding(horizontal = 16.dp))
                            AdminMenuNavRow(
                                icon = Icons.Default.Storefront,
                                title = "Catálogo de Productos",
                                onClick = onNavigateToCatalogo,
                                testTag = "menuadmin_nav_catalogo"
                            )
                            HorizontalDivider(color = Color(0xFFF0EBE3), modifier = Modifier.padding(horizontal = 16.dp))
                            AdminMenuNavRow(
                                icon = Icons.Default.Checkroom,
                                title = "Menú de Categorías",
                                onClick = onNavigateToCategorias,
                                testTag = "menuadmin_nav_categorias"
                            )
                            HorizontalDivider(color = Color(0xFFF0EBE3), modifier = Modifier.padding(horizontal = 16.dp))
                            AdminMenuNavRow(
                                icon = Icons.Default.ShoppingCart,
                                title = "Bolsa de Compras",
                                onClick = onNavigateToCart,
                                testTag = "menuadmin_nav_carrito"
                            )
                            HorizontalDivider(color = Color(0xFFF0EBE3), modifier = Modifier.padding(horizontal = 16.dp))
                            AdminMenuNavRow(
                                icon = Icons.Default.SupportAgent,
                                title = "Centro de Mensajes & Soporte",
                                onClick = onNavigateToChats,
                                testTag = "menuadmin_nav_chats"
                            )
                        }
                    }
                }

                // Botón de Volver
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("menuadmin_close_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = EleganBurgundy
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(EleganBurgundy)
                        )
                    ) {
                        Text(
                            text = "Cerrar Panel Administrativo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AdminMenuPrimaryLinkCard(
    icon: ImageVector,
    iconTint: Color,
    iconContainerColor: Color,
    title: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECE7E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal,
                    fontSize = 15.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = EleganBurgundy,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AdminMenuNavRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F1EC)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EleganCharcoal,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = EleganCharcoal
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(16.dp)
        )
    }
}
