package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.data.model.Product
import com.example.ui.GalleryMediaItem
import com.example.ui.getDefaultCategoryFallback
import com.example.ui.getProductMediaItems
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

/**
 * Cuadrícula de 3 columnas para mostrar a los clientes todas sus publicaciones guardadas
 * en su perfil. Permite acceder de forma rápida y directa a cualquier publicación guardada.
 */
@Composable
fun SavedPublicationsGrid(
    savedProducts: List<Product>,
    onProductClick: (Product) -> Unit,
    onExploreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("saved_publications_grid_card")
            .padding(0.dp)
    ) {
            // Cabecera de la sección de guardados
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EleganBurgundy.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Guardados",
                        tint = EleganBurgundy,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Publicaciones Guardadas",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal,
                            fontFamily = FontFamily.Serif
                        )
                    )
                    Text(
                        text = "Colección personal • Cuadrícula 3x3",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }

                if (savedProducts.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EleganBurgundyDark
                    ) {
                        Text(
                            text = "${savedProducts.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (savedProducts.isEmpty()) {
                // Estado vacío elegante cuando no hay publicaciones guardadas
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(EleganGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = EleganGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Aún no tienes publicaciones guardadas",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Toca el icono de guardado en el feed de inicio o en los productos para agregarlos a tu colección rápida.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray,
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundyDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("saved_explore_button")
                    ) {
                        Text(
                            text = "Explorar Publicaciones",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                // Cuadrícula exacta de 3 columnas (chunked(3) optimizado para lazy list sin colisiones de scroll)
                val rows = savedProducts.chunked(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rows.forEachIndexed { rowIndex, rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (colIndex in 0 until 3) {
                                val product = rowItems.getOrNull(colIndex)
                                if (product != null) {
                                    SavedPublicationGridItem(
                                        product = product,
                                        onClick = { onProductClick(product) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    // Espacio vacío para balancear la última fila de 3
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
    }
}

/**
 * Item individual de la cuadrícula de 3 columnas.
 * Muestra la portada de la publicación, indicador de video o galería, precio y permite abrirla al toque.
 */
@Composable
fun SavedPublicationGridItem(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaItems = getProductMediaItems(product)
    val hasVideo = product.hasVideo || mediaItems.any { it is GalleryMediaItem.Video }
    val isMultiMedia = mediaItems.size > 1

    val previewUrl = when (val first = mediaItems.firstOrNull()) {
        is GalleryMediaItem.Video -> first.posterUrl.ifBlank { getDefaultCategoryFallback(product.category) }
        is GalleryMediaItem.Image -> first.imageUrl.ifBlank { getDefaultCategoryFallback(product.category) }
        null -> getDefaultCategoryFallback(product.category)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEFEBE8))
            .clickable(onClick = onClick)
            .testTag("saved_post_${product.id}")
    ) {
        // Imagen o portada generada del video (nunca fondo negro)
        AsyncImage(
            model = previewUrl,
            contentDescription = product.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Degradado oscuro inferior para legibilidad del nombre y precio
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Distintivo superior derecho: video o multi-imagen
        if (hasVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = EleganGold,
                    modifier = Modifier.size(13.dp)
                )
            }
        } else if (isMultiMedia) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = "Galería",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        // Título breve y precio en la parte inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = product.name,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$${product.price.toInt()}",
                color = EleganGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
