package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.IceSkating
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.StoreCategory
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

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

@Composable
fun CategoryStoryCard(
    category: StoreCategory,
    cardWidth: Dp,
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

            // Indicador circular superior estilo historia
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

            // Nombre de la categoría
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
fun CategoryStoriesSection(
    onNavigateToCatalogo: (String) -> Unit,
    onNavigateToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
