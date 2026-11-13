package com.example.ui

import com.example.ui.components.EleganVideoPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import com.example.data.model.Product
import com.example.ui.GalleryMediaItem
import com.example.ui.getProductMediaItems
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

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
    onProductClick: () -> Unit,
    modifier: Modifier = Modifier
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

    val mediaItems = remember(
        product.id,
        product.imageUrl,
        product.images,
        product.videoUrl,
        product.isVideo
    ) {
        getProductMediaItems(product)
    }
    val priceText = remember(product.price) {
        "$${String.format("%.2f", product.price)} USD"
    }
    val categoryLabel = remember(product.category) {
        "Boutique Oficial • ${product.category.replaceFirstChar { it.uppercase() }}"
    }
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .testTag("publication_post_${product.id}")
    ) {
        // 1. Cabecera de la publicación: Logo Elegan y verificado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.icono),
                contentDescription = "Logo Elegan",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )

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
                    text = categoryLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                )
            }
        }

        // Título del producto
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

        // 2. Contenedor multimedia (Galería horizontal con videos y fotos)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RectangleShape)
                .background(Color(0xFFF0EBE3)),
            contentAlignment = Alignment.Center
        ) {
            if (mediaItems.size <= 1) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("post_media_pager_${product.id}")
                ) {
                    when (val item = mediaItems.firstOrNull()) {
                        is GalleryMediaItem.Video -> {
                            EleganVideoPlayer(
                                videoUrl = item.videoUrl,
                                posterUrl = item.posterUrl,
                                aspectRatio = ratio,
                                autoPlay = isFocused,
                                isFocused = isFocused,
                                showControls = true,
                                testTagPrefix = "inicio_video_${product.id}_0",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        is GalleryMediaItem.Image -> {
                            PostImageItem(
                                imageUrl = item.imageUrl,
                                contentDescription = "${product.name} - 1",
                                onClick = onProductClick
                            )
                        }
                        null -> {}
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 0,
                    key = { page -> "${product.id}_$page" },
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
                            PostImageItem(
                                imageUrl = item.imageUrl,
                                contentDescription = "${product.name} - ${page + 1}",
                                onClick = onProductClick
                            )
                        }
                        null -> {}
                    }
                }
            }

            // Indicador de contenido (1/10)
            if (mediaItems.isNotEmpty()) {
                val currentItem = mediaItems.getOrNull(pagerState.currentPage)
                val isCurrentVideo = currentItem is GalleryMediaItem.Video

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("post_content_indicator_${product.id}")
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

            // Badge de precio
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.84f))
                    .testTag("post_price_${product.id}")
            ) {
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF137A3A)
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        // 3. Barra horizontal de interacción
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like
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

            // Comentario
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

            // Guardar
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

            // Compartir
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

        // 4. Detalles y acción de compra
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

        // 5. Separador elegante entre publicaciones
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(Color(0xFFEBE6DF))
        )
    }
}

@Composable
private fun PostImageItem(
    imageUrl: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(imageUrl, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(900, 900)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    )
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
