package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.IceSkating
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.Product
import com.example.data.model.StoreCategory
import com.example.data.model.UserProfile
import com.example.ui.components.EleganVideoPlayer
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory
import com.example.data.remote.MobileSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    featuredProducts: List<Product>,
    userProfile: UserProfile? = null,
    cartItemCount: Long = 0L,
    savedProductIds: Set<String> = emptySet(),
    onToggleSaveProduct: ((String, (Result<Boolean>) -> Unit) -> Unit)? = null,
    publicationStats: Map<String, PublicationStats>,
    likedPublicationIds: Set<String>,
    publicationComments: List<PublicationComment>,
    onToggleLike: (String, (Result<Boolean>) -> Unit) -> Unit,
    onOpenComments: (String?) -> Unit,
    onSubmitComment: (String, String, String, (Result<Unit>) -> Unit) -> Unit,
    onRecordShare: (String, (Result<Unit>) -> Unit) -> Unit,
    onNavigateToCatalogo: (String) -> Unit,
    onNavigateToProduct: (Product) -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToCategoryMenu: () -> Unit = onNavigateToMenu,
    onNavigateToChats: () -> Unit,
    onNavigateToCart: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val productIds = remember(featuredProducts) { featuredProducts.map { it.id }.toSet() }

    // Reutiliza el conjunto de IDs para no reconstruirlo en cada actualización del scroll.
    val focusedProductId by remember(listState, productIds) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@derivedStateOf null

            val vpStart = layoutInfo.viewportStartOffset
            val vpEnd = layoutInfo.viewportEndOffset
            val vpCenter = (vpStart + vpEnd) / 2
            val visibleProducts = visibleItems.filter { it.key in productIds }
            if (visibleProducts.isEmpty()) return@derivedStateOf null

            // Prioridad 1: el producto que cruza el centro de visión del usuario en la pantalla
            val centerProduct = visibleProducts.find { item ->
                item.offset <= vpCenter && (item.offset + item.size) >= vpCenter
            }

            if (centerProduct != null) {
                val visibleTop = maxOf(centerProduct.offset, vpStart)
                val visibleBottom = minOf(centerProduct.offset + centerProduct.size, vpEnd)
                val visiblePx = (visibleBottom - visibleTop).coerceAtLeast(0)
                if (visiblePx >= 100) {
                    return@derivedStateOf centerProduct.key as? String
                }
            }

            // Prioridad 2: el producto con mayor altura visible en el viewport
            val maxVisible = visibleProducts.maxByOrNull { item ->
                val visibleTop = maxOf(item.offset, vpStart)
                val visibleBottom = minOf(item.offset + item.size, vpEnd)
                (visibleBottom - visibleTop).coerceAtLeast(0)
            }
            if (maxVisible != null) {
                val visibleTop = maxOf(maxVisible.offset, vpStart)
                val visibleBottom = minOf(maxVisible.offset + maxVisible.size, vpEnd)
                val visiblePx = (visibleBottom - visibleTop).coerceAtLeast(0)
                if (visiblePx >= 140) {
                    return@derivedStateOf maxVisible.key as? String
                }
            }
            null
        }
    }

    val context = LocalContext.current
    var localSavedProductIds by remember { mutableStateOf(setOf<String>()) }
    var activeCommentProduct by remember { mutableStateOf<Product?>(null) }

    val currentUser = MobileSession.user(context)
    val rawName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.name?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.substringBefore("@")
        ?: "Bienvenido"
    val displayName = rawName.trim()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Barra superior fija para máxima estabilidad y scroll 100% continuo y fluido sin saltos de layout
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateToCategoryMenu,
                    modifier = Modifier.testTag("inicio_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menú",
                        tint = EleganCharcoal
                    )
                }

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal,
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("inicio_user_name")
                )

                IconButton(
                    onClick = onNavigateToCart,
                    modifier = Modifier.testTag("inicio_cart_button")
                ) {
                    if (cartItemCount > 0) {
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
                                tint = EleganCharcoal
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Carrito",
                            tint = EleganCharcoal
                        )
                    }
                }
            }
        }

        // Sombra únicamente en la parte inferior para mantener continuidad fluida y elegante
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.07f),
                            Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("inicio_screen"),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
        // Hero Luxury Banner
        item(key = "hero_banner") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                EleganBurgundyDark,
                                EleganBurgundy,
                                Color(0xFF380710)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top header in banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.icono),
                                    contentDescription = "Logo Elegan",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ELEGAN",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 4.sp,
                                        fontFamily = FontFamily.Serif
                                    )
                                )
                                Text(
                                    text = "Sartorial & Luxury Menswear",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = EleganGold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onNavigateToMenu,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("menu_button_hero")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Menú Categorías",
                                tint = Color.White
                            )
                        }
                    }

                    // Central banner text
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = EleganGold.copy(alpha = 0.25f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "NUEVA COLECCIÓN 2026",
                                color = EleganGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                letterSpacing = 2.sp
                            )
                        }

                        Text(
                            text = "Distinción &\nCarácter Propio",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 32.sp,
                                fontFamily = FontFamily.Serif
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { onNavigateToCatalogo("todo") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EleganGold,
                                    contentColor = EleganCharcoal
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("explore_catalog_hero_btn")
                            ) {
                                Text(
                                    text = "Ver Catálogo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            OutlinedButton(
                                onClick = onNavigateToMenu,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(listOf(Color.White, EleganGold))
                                )
                            ) {
                                Text(text = "Categorías", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Categories quick slider (Historias / Estados en formato 9:16, 4 visibles)
        item(key = "categories_section") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EleganBurgundy)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Categorías",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal,
                                fontFamily = FontFamily.Serif
                            )
                        )
                    }
                    Text(
                        text = "Ver menú completo",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = EleganBurgundy,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.clickable { onNavigateToMenu() }
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    val horizontalPadding = 12.dp
                    val spacing = 8.dp
                    val cardWidth = ((maxWidth - (horizontalPadding * 2) - (spacing * 3)) / 4).coerceAtLeast(70.dp)

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_stories_row"),
                        contentPadding = PaddingValues(horizontal = horizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        items(StoreCategory.values()) { cat ->
                            CategoryStoryCard(
                                category = cat,
                                cardWidth = cardWidth,
                                onClick = { onNavigateToCatalogo(cat.id) }
                            )
                        }
                    }
                }
            }
        }

        // Publications feed (directo debajo del carrusel de categorías)
        item(key = "feed_spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Single column publications feed con reciclado de alto rendimiento y detección de foco
        itemsIndexed(
            items = featuredProducts,
            key = { _, prod -> prod.id },
            contentType = { _, _ -> "product_post" }
        ) { index, prod ->
            val isLiked = prod.id in likedPublicationIds
            val isSaved = if (onToggleSaveProduct != null) savedProductIds.contains(prod.id) else localSavedProductIds.contains(prod.id)
            val stats = publicationStats[prod.id]
            val currentLikes = stats?.likesCount?.toInt() ?: 0
            val currentCommentCount = stats?.commentsCount?.toInt() ?: 0
            val currentShareCount = stats?.sharesCount?.toInt() ?: 0
            val currentSaveCount = stats?.savesCount?.toInt() ?: 0
            val isPostFocused = (focusedProductId == prod.id)

            ProductPublicationPost(
                product = prod,
                index = index,
                isLiked = isLiked,
                isSaved = isSaved,
                likeCount = currentLikes,
                commentCount = currentCommentCount,
                shareCount = currentShareCount,
                saveCount = currentSaveCount,
                isFocused = isPostFocused,
                onLikeClick = {
                    onToggleLike(prod.id) { result ->
                        Toast.makeText(
                            context,
                            if (result.isSuccess) {
                                if (result.getOrNull() == true) "Te gusta esta publicación"
                                else "Quitaste tu like"
                            } else {
                                "No se pudo actualizar el like"
                            },
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onCommentClick = {
                    activeCommentProduct = prod
                    onOpenComments(prod.id)
                },
                onSaveClick = {
                    val willSave = !isSaved
                    if (onToggleSaveProduct != null) {
                        onToggleSaveProduct(prod.id) { result ->
                            Toast.makeText(
                                context,
                                if (result.isSuccess) {
                                    if (willSave) "Publicación guardada en tu perfil"
                                    else "Publicación eliminada de guardados"
                                } else {
                                    "No se pudo actualizar la publicación guardada"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        localSavedProductIds = if (willSave) localSavedProductIds + prod.id else localSavedProductIds - prod.id
                        Toast.makeText(
                            context,
                            if (willSave) "Publicación guardada en tu perfil" else "Publicación eliminada de guardados",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onShareClick = {
                    onRecordShare(prod.id) { result ->
                        if (result.isSuccess) {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "ELEGAN: Descubre ${prod.name} por $${String.format("%.2f", prod.price)} USD. Pieza exclusiva de la boutique."
                                )
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartir publicación"))
                        } else {
                            Toast.makeText(
                                context,
                                "No se pudo registrar el compartido",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onProductClick = { onNavigateToProduct(prod) }
            )
        }

        // Concierge VIP banner
        item(key = "concierge_vip_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToChats() }
                    .testTag("concierge_card_home"),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2E35)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(EleganGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = EleganGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Elegan Concierge VIP",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "¿Dudas con tu talla o estilo? Chatea en directo con un asesor personal.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCCCCCC)
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = EleganGold
                    )
                }
            }
        }

        // Value props
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ValuePropRow(
                    icon = Icons.Default.LocalShipping,
                    title = "Envío Express de Cortesía",
                    desc = "Gratuito en pedidos superiores a $100 con seguro incluido."
                )
                ValuePropRow(
                    icon = Icons.Default.Diamond,
                    title = "Garantía Sartorial 100%",
                    desc = "Materiales nobles y confección de primera categoría."
                )
                ValuePropRow(
                    icon = Icons.Default.CheckCircle,
                    title = "Cambios & Devoluciones Sin Coste",
                    desc = "30 días para probar tus prendas con recogida a domicilio."
                )
            }
        }
    }
}

    if (activeCommentProduct != null) {
        val prod = activeCommentProduct!!
        var newCommentText by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = {
                activeCommentProduct = null
                onOpenComments(null)
            },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comentarios de Clientes",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal,
                            fontFamily = FontFamily.Serif
                        )
                    )
                    IconButton(onClick = {
                        activeCommentProduct = null
                        onOpenComments(null)
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Text(
                    text = prod.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EleganBurgundy,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(200.dp)
                ) {
                    items(publicationComments, key = { it.id }) { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(EleganBurgundy.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = comment.displayName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = EleganBurgundy,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = comment.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal
                                    )
                                )
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF333333)
                                    )
                                )
                            }
                        }
                    }
                    if (publicationComments.isEmpty()) {
                        item {
                            Text(
                                text = "Aún no hay comentarios. Sé el primero en comentar.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Escribe un comentario como $displayName...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comment_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EleganBurgundy,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                onSubmitComment(prod.id, displayName, newCommentText) { result ->
                                    if (result.isSuccess) {
                                        newCommentText = ""
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "No se pudo publicar el comentario",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(EleganBurgundy)
                            .testTag("submit_comment_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Publicar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun CategoryPill(
    category: StoreCategory,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .testTag("cat_pill_${category.id}"),
        color = Color.White,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(EleganBurgundy)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = EleganCharcoal
                )
            )
        }
    }
}

fun getCategoryStoryImage(category: StoreCategory): String {
    return when (category) {
        StoreCategory.TODO -> "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=500&auto=format&fit=crop&q=80"
        StoreCategory.GORRA -> "https://images.unsplash.com/photo-1588850561407-ed78c282e89b?w=500&auto=format&fit=crop&q=80"
        StoreCategory.GAFA -> "https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=500&auto=format&fit=crop&q=80"
        StoreCategory.CAMISA -> "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=500&auto=format&fit=crop&q=80"
        StoreCategory.CORREA -> "https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=500&auto=format&fit=crop&q=80"
        StoreCategory.PANTALONE -> "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=500&auto=format&fit=crop&q=80"
        StoreCategory.ZAPATO -> "https://images.unsplash.com/photo-1614252235316-8c857d38b5f4?w=500&auto=format&fit=crop&q=80"
        StoreCategory.INTERIORE -> "https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?w=500&auto=format&fit=crop&q=80"
        StoreCategory.PERFUME -> "https://images.unsplash.com/photo-1523293182086-7651a899d37f?w=500&auto=format&fit=crop&q=80"
    }
}

fun getCategoryStoryIcon(category: StoreCategory): ImageVector {
    return when (category) {
        StoreCategory.TODO -> Icons.Default.GridView
        StoreCategory.GORRA -> Icons.Default.Checkroom
        StoreCategory.GAFA -> Icons.Default.Visibility
        StoreCategory.CAMISA -> Icons.Default.DryCleaning
        StoreCategory.CORREA -> Icons.Default.Straighten
        StoreCategory.PANTALONE -> Icons.Default.Accessibility
        StoreCategory.ZAPATO -> Icons.Default.IceSkating
        StoreCategory.INTERIORE -> Icons.Default.Layers
        StoreCategory.PERFUME -> Icons.Default.Spa
    }
}

@Composable
fun CategoryStoryCard(
    category: StoreCategory,
    cardWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(cardWidth)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("cat_story_${category.id}"),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.5.dp,
            Brush.verticalGradient(
                colors = listOf(
                    EleganGold,
                    EleganBurgundy
                )
            )
        ),
        shadowElevation = 3.dp,
        color = EleganCharcoal
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Imagen representativa de la categoría
            AsyncImage(
                model = getCategoryStoryImage(category),
                contentDescription = category.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradiente oscuro de fondo (superior para icono, inferior para texto)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.4f),
                            0.3f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
            )

            // Indicador / Avatar circular superior estilo estado de WhatsApp / historia de Instagram
            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.TopStart)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(EleganBurgundy)
                    .border(1.dp, EleganGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryStoryIcon(category),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }

            // Nombre de la categoría en la parte inferior
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 9.5.sp,
                        lineHeight = 11.sp,
                        fontFamily = FontFamily.Serif
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ProductPublicationPost(
    product: Product,
    index: Int,
    isLiked: Boolean,
    isSaved: Boolean,
    likeCount: Int,
    commentCount: Int,
    shareCount: Int = 0,
    saveCount: Int = 0,
    isFocused: Boolean = false,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onProductClick: () -> Unit
) {
    // Determinar formato de publicación: 1:1, 3:4 o 4:3 con remember para scroll 100% fluido
    val ratio = remember(product.mediaFormat, index) {
        when (product.mediaFormat.trim()) {
            "3:4" -> (3f / 4f)
            "4:3" -> (4f / 3f)
            "1:1" -> 1f
            else -> when (index % 3) {
                0 -> 1f
                1 -> (3f / 4f)
                else -> (4f / 3f)
            }
        }
    }

    val mediaItems = remember(product) { getProductMediaItems(product) }
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .testTag("publication_post_${product.id}")
    ) {
        // 1. PARTE SUPERIOR DE LA PUBLICACIÓN:
        // Logo de la marca y nombre "ELEGAN"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(EleganIvory)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icono),
                    contentDescription = "Logo Elegan",
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ELEGAN",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Serif
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = Color(0xFF1D9BF0),
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "Boutique Oficial • ${product.category.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // DEBAJO DEL LOGO Y NOMBRE: Título del producto
        Text(
            text = product.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = EleganCharcoal,
                fontFamily = FontFamily.Serif
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 8.dp)
                .clickable { onProductClick() }
        )

        // 2. CONTENEDOR MULTIMEDIA CON SCROLL HORIZONTAL (GALERÍA):
        // Se extiende de extremo a extremo (0px laterales en la pantalla)
        // 0px borde redondo en la parte superior y en la parte inferior (borde plano)
        // Formato 1:1, 3:4, 4:3 sin recortar imágenes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RectangleShape)
                .background(Color(0xFFF0EBE3)),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("post_media_pager_${product.id}")
            ) { page ->
                when (val item = mediaItems.getOrNull(page)) {
                    is GalleryMediaItem.Video -> {
                        val isVideoFocused = isFocused && (pagerState.currentPage == page)
                        EleganVideoPlayer(
                            videoUrl = item.videoUrl,
                            posterUrl = item.posterUrl,
                            aspectRatio = ratio,
                            autoPlay = isVideoFocused,
                            isFocused = isVideoFocused,
                            showControls = true,
                            testTagPrefix = "inicio_video_${product.id}_$page",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is GalleryMediaItem.Image -> {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = "${product.name} - ${page + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onProductClick() }
                        )
                    }
                    null -> {}
                }
            }


            // Indicador de contenido en la parte inferior izquierda (ej: 1/10)
            if (mediaItems.isNotEmpty()) {
                val currentItem = mediaItems.getOrNull(pagerState.currentPage)
                val isCurrentVideo = currentItem is GalleryMediaItem.Video

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 12.dp)
                        .testTag("post_content_indicator_${product.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCurrentVideo) Icons.Default.Videocam else Icons.Default.Collections,
                            contentDescription = "Tipo de contenido",
                            tint = if (isCurrentVideo) EleganGold else Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${pagerState.currentPage + 1}/${mediaItems.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.84f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .testTag("post_price_${product.id}")
            ) {
                Text(
                    text = "$${String.format("%.2f", product.price)} USD",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF137A3A)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        // 3. BARRA HORIZONTAL DE INTERACCIÓN:
        // Pegada a 0px del contenedor multimedia en la parte inferior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("post_like_${product.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = if (isLiked) EleganBurgundy else EleganCharcoal,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = likeCount.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = EleganCharcoal,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onCommentClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("post_comment_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comentario",
                        tint = EleganCharcoal,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Text(
                    text = commentCount.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = EleganCharcoal,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("post_save_${product.id}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Guardar",
                        tint = if (isSaved) EleganGold else EleganCharcoal,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = saveCount.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = EleganCharcoal,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("post_share_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Compartir",
                        tint = EleganCharcoal,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Text(
                    text = shareCount.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = EleganCharcoal,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        // 4. DETALLES Y ACCIÓN DE COMPRA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            if (product.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF4A4A4A)
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (commentCount > 0) {
                Text(
                    text = "Ver comentarios",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { onCommentClick() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onProductClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EleganBurgundy,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("post_buy_btn_${product.id}")
                ) {
                    Text(
                        text = "Comprar Producto",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onProductClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EleganBurgundy
                    ),
                    modifier = Modifier.testTag("post_details_btn_${product.id}")
                ) {
                    Text("Ver Detalle", fontSize = 13.sp)
                }
            }
        }

        // 5. SEPARADOR ANTES DE LA SIGUIENTE PUBLICACIÓN
        // "luego de la barra de interasion inferior comiensa la siguiente publicasio"
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(Color(0xFFEBE6DF))
        )
    }
}

@Composable
fun ProductHomeCard(
    product: Product,
    onClick: () -> Unit
) {
    ProductPublicationPost(
        product = product,
        index = 0,
        isLiked = false,
        isSaved = false,
        likeCount = product.reviewCount.toInt().coerceAtLeast(10),
        commentCount = 2,
        onLikeClick = {},
        onCommentClick = {},
        onSaveClick = {},
        onShareClick = {},
        onProductClick = onClick
    )
}

@Composable
fun ValuePropRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF9F3EA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EleganBurgundy,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal
                )
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Gray
                )
            )
        }
    }
}
