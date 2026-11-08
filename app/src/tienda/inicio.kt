package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.StoreCategory
import com.example.data.model.UserProfile
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

            // Prioridad 1: el producto que cruza el centro de visión del usuario
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
        // Barra superior fija con menú, nombre y carrito
        InicioTopBar(
            displayName = displayName,
            cartItemCount = cartItemCount,
            onNavigateToCategoryMenu = onNavigateToCategoryMenu,
            onNavigateToCart = onNavigateToCart
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
                InicioHeroBanner(
                    onNavigateToCatalogo = onNavigateToCatalogo,
                    onNavigateToMenu = onNavigateToMenu
                )
            }

            // Carrusel de historias de categorías (formato 9:16)
            item(key = "categories_section") {
                CategoryStoriesSection(
                    onNavigateToCatalogo = onNavigateToCatalogo,
                    onNavigateToMenu = onNavigateToMenu
                )
            }

            // Separador antes del feed de publicaciones
            item(key = "feed_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Feed de publicaciones estilo Instagram / TikTok
            itemsIndexed(
                items = featuredProducts,
                key = { _, prod -> prod.id },
                contentType = { _, _ -> "product_post" }
            ) { index, prod ->
                val isLiked = prod.id in likedPublicationIds
                val isSaved = if (onToggleSaveProduct != null) {
                    savedProductIds.contains(prod.id)
                } else {
                    localSavedProductIds.contains(prod.id)
                }
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

            // Tarjeta Elegan Concierge VIP
            item(key = "concierge_vip_card") {
                ConciergeVipCard(onNavigateToChats = onNavigateToChats)
            }

            // Propuestas de valor y garantías
            item(key = "value_props_section") {
                InicioValuePropsSection()
            }
        }
    }

    // Modal BottomSheet de comentarios
    activeCommentProduct?.let { prod ->
        PublicationCommentsBottomSheet(
            product = prod,
            displayName = displayName,
            comments = publicationComments,
            onDismiss = {
                activeCommentProduct = null
                onOpenComments(null)
            },
            onSubmitComment = onSubmitComment
        )
    }
}
