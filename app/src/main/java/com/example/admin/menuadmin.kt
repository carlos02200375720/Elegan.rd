package com.example.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PostAdd
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory

@Composable
fun MenuAdminScreen(
    onNavigateToPublicar: () -> Unit,
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 2.dp
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ELEGAN • ADMINISTRACIÓN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal,
                                fontFamily = FontFamily.Serif,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Menú Principal y Herramientas",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Surface(
                        color = EleganBurgundy.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF9F7F5)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("admin_menu_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Banner de bienvenida al panel de administración
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF1E1F24), Color(0xFF2C2F36))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(EleganGold.copy(alpha = 0.2f))
                                    .border(1.dp, EleganGold.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.icono),
                                    contentDescription = "Logo Elegan",
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Panel de Control",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Serif
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Gestión integral de catálogo, publicaciones y cuenta",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFD0D0D0),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Título de Sección: Enlaces Principales Solicitados
            item {
                Text(
                    text = "ACCESOS PRINCIPALES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            // 1. ENLACE A PUBLICAR.KT
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigateToPublicar() }
                        .testTag("menuadmin_link_publicar"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EleganBurgundy.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PostAdd,
                                contentDescription = "Publicar",
                                tint = EleganBurgundy,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Publicar Contenido",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = EleganGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "publicar.kt",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Publicar nuevos productos con fotos de galería y crear anuncios promocionales",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ir a publicar",
                            tint = EleganBurgundy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. ENLACE A PUBLICASIONE.KT (Control y administración de todas las publicaciones)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigateToPublicaciones() }
                        .testTag("menuadmin_link_publicasione"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EleganGold.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = "Publicaciones",
                                tint = EleganBurgundy,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Control de Publicaciones",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = EleganBurgundy.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "publicasione.kt",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Controlar y administrar todas las publicaciones existentes (stock, estado y eliminación)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ir a control de publicaciones",
                            tint = EleganBurgundy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 3. ENLACE A PERFIL.KT
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onNavigateToPerfil() }
                        .testTag("menuadmin_link_perfil"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF26292E).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Perfil",
                                tint = Color(0xFF26292E),
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Mi Perfil de Usuario",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFEAEAEA),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "perfil.kt",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Gestionar datos de contacto, dirección de entrega, pedidos e historial",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Ir a perfil",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Título de Sección: Navegación de la Tienda
            item {
                Spacer(modifier = Modifier.height(4.dp))
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

            // Opciones de Navegación Rápida
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AdminMenuNavRow(
                            icon = Icons.Default.Home,
                            title = "Página de Inicio",
                            subtitle = "Volver a la portada principal de la tienda",
                            onClick = onNavigateToInicio,
                            testTag = "menuadmin_nav_inicio"
                        )
                        HorizontalDivider(color = Color(0xFFF0EBE3), modifier = Modifier.padding(horizontal = 16.dp))
                        AdminMenuNavRow(
                            icon = Icons.Default.Storefront,
                            title = "Catálogo de Productos",
                            subtitle = "Ver todos los artículos disponibles y filtros",
                            onClick = onNavigateToCatalogo,
                            testTag = "menuadmin_nav_catalogo"
                        )
                        HorizontalDivider(color = Color(0xFFF0EBE3), modifier = Modifier.padding(horizontal = 16.dp))
                        AdminMenuNavRow(
                            icon = Icons.Default.Checkroom,
                            title = "Menú de Categorías",
                            subtitle = "Explorar camisas, pantalones, gorras y calzado",
                            onClick = onNavigateToCategorias,
                            testTag = "menuadmin_nav_categorias"
                        )
                        HorizontalDivider(color = Color(0xFFF0EBE3), modifier = Modifier.padding(horizontal = 16.dp))
                        AdminMenuNavRow(
                            icon = Icons.Default.ShoppingCart,
                            title = "Bolsa de Compras",
                            subtitle = "Revisar artículos en el carrito de compras",
                            onClick = onNavigateToCart,
                            testTag = "menuadmin_nav_carrito"
                        )
                        HorizontalDivider(color = Color(0xFFF0EBE3), modifier = Modifier.padding(horizontal = 16.dp))
                        AdminMenuNavRow(
                            icon = Icons.Default.SupportAgent,
                            title = "Centro de Mensajes & Soporte",
                            subtitle = "Atender consultas de clientes en tiempo real",
                            onClick = onNavigateToChats,
                            testTag = "menuadmin_nav_chats"
                        )
                    }
                }
            }

            // SECCIÓN INFORMATIVA: FLUJO DEL CONTENIDO MULTIMEDIA Y DONDE SE ALOJA
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ARQUITECTURA DE CONTENIDO MULTIMEDIA",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("menuadmin_info_multimedia"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EleganBurgundy.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = EleganBurgundy,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "¿Dónde se aloja el contenido multimedia?",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Paso 1
                        MultimediaFlowStep(
                            stepNumber = "1",
                            title = "Origen: Galería del Dispositivo",
                            description = "Al hacer clic en seleccionar imagen, Android abre el Photo Picker nativo del sistema y genera un identificador seguro de contenido local ('content://...')."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Paso 2
                        MultimediaFlowStep(
                            stepNumber = "2",
                            title = "Procesamiento y Renderizado Inmediato",
                            description = "La biblioteca Coil y Jetpack Compose cargan la imagen directamente en la app desde el almacenamiento local del dispositivo sin esperas."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Paso 3
                        MultimediaFlowStep(
                            stepNumber = "3",
                            title = "Estado en Memoria y Repositorio",
                            description = "Los nuevos productos y anuncios se incorporan al repositorio activo ('EleganRepository') y StateFlows para verse de inmediato en Inicio y Catálogo."
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Paso 4
                        MultimediaFlowStep(
                            stepNumber = "4",
                            title = "Alojamiento en Nube (Producción)",
                            description = "Para que otros dispositivos y usuarios vean las fotos, los archivos binarios se suben a Firebase Cloud Storage o Google Cloud Storage, guardando sus URLs públicas HTTPS en Firestore."
                        )
                    }
                }
            }

            // Botón de Volver
            item {
                Spacer(modifier = Modifier.height(8.dp))
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
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(EleganBurgundy)
                    )
                ) {
                    Text(
                        text = "Cerrar Panel Administrativo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AdminMenuNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp)
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = EleganCharcoal
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun MultimediaFlowStep(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(EleganBurgundy),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EleganCharcoal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF555555),
                lineHeight = 15.sp
            )
        }
    }
}
