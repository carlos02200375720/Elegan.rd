package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.IceSkating
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import com.example.data.model.Product
import com.example.data.model.StoreCategory
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

data class CategoriaItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

val storeCategoriesList = listOf(
    CategoriaItem(
        id = "todo",
        title = "Todo el Catálogo",
        subtitle = "Explora todas las piezas y colecciones",
        icon = Icons.Default.GridView
    ),
    CategoriaItem(
        id = "gorra",
        title = "Gorras & Boinas",
        subtitle = "Boinas Peaky, newsboys y sombreros tweed",
        icon = Icons.Default.Checkroom
    ),
    CategoriaItem(
        id = "gafa",
        title = "Gafas",
        subtitle = "Gafas de sol polarizadas y monturas vintage",
        icon = Icons.Default.Visibility
    ),
    CategoriaItem(
        id = "camisa",
        title = "Camisas & Trajes",
        subtitle = "Camisas algodón egipcio, chalecos y blazers",
        icon = Icons.Default.DryCleaning
    ),
    CategoriaItem(
        id = "correa",
        title = "Correas",
        subtitle = "Cinturones de cuero vacuno y hebillas artesanales",
        icon = Icons.Default.Straighten
    ),
    CategoriaItem(
        id = "pantalone",
        title = "Pantalones",
        subtitle = "Pantalones de vestir sartoriales y chinos",
        icon = Icons.Default.Accessibility
    ),
    CategoriaItem(
        id = "zapato",
        title = "Zapatos",
        subtitle = "Oxfords cap-toe, brogues y mocasines",
        icon = Icons.Default.IceSkating
    ),
    CategoriaItem(
        id = "interiore",
        title = "Ropa Interior",
        subtitle = "Bóxers micro-modal y camisetas confort",
        icon = Icons.Default.Layers
    ),
    CategoriaItem(
        id = "perfume",
        title = "Perfumes",
        subtitle = "Fragancias de autor y extractos exclusivos",
        icon = Icons.Default.Spa
    )
)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriaScreen(
    products: List<Product>,
    onCategorySelected: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.icono),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Menú de Categorías",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal,
                                    fontFamily = FontFamily.Serif
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("menu_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = EleganCharcoal
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
                EleganTopBarShadow()
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
                else -> 20.dp
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .testTag("menu_screen"),
                contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 12.dp)
            ) {
                item {
                    Text(
                        text = "Selecciona una categoría para explorar prendas seleccionadas y accesorios:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray,
                            fontSize = if (isCompact) 13.sp else 14.sp
                        ),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                items(storeCategoriesList, key = { it.id }) { catItem ->
                    val count = if (catItem.id == "todo") {
                        products.size
                    } else {
                        products.count { it.category.equals(catItem.id, ignoreCase = true) }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onCategorySelected(catItem.id) }
                            .testTag("menu_item_${catItem.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFEDE6DF)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isCompact) 12.dp else 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCompact) 42.dp else 48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (catItem.id == "todo") EleganBurgundy else Color(0xFFF6EFE9)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = catItem.icon,
                                    contentDescription = catItem.title,
                                    tint = if (catItem.id == "todo") Color.White else EleganBurgundy,
                                    modifier = Modifier.size(if (isCompact) 21.dp else 24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(if (isCompact) 12.dp else 16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = catItem.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EleganCharcoal,
                                            fontSize = if (isCompact) 15.sp else 16.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF2EBE5)
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EleganBurgundyDark,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = catItem.subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Gray,
                                        fontSize = if (isCompact) 11.5.sp else 12.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Ir a categoría",
                                tint = EleganGold,
                                modifier = Modifier.size(20.dp)
                            )
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
    val context = LocalContext.current
    val imageUrl = remember(category) { getCategoryStoryImage(category) }
    val imageRequest = remember(imageUrl, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(400, 700)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }

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
                model = imageRequest,
                contentDescription = category.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        val isCompact = maxWidth < 350.dp
        val isExpanded = maxWidth >= 600.dp
        val headerHorizontalPad = if (isCompact) 12.dp else 16.dp
        val horizontalPadding = if (isCompact) 10.dp else 12.dp
        val spacing = if (isCompact) 6.dp else 8.dp
        val visibleCards = if (isExpanded) 6f else 4f
        val cardWidth = ((maxWidth - (horizontalPadding * 2) - (spacing * (visibleCards - 1f))) / visibleCards)
            .coerceIn(66.dp, 118.dp)

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = headerHorizontalPad),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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
                            fontSize = if (isCompact) 15.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal,
                            fontFamily = FontFamily.Serif
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ver menú completo",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        color = EleganBurgundy,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onNavigateToMenu() }
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("category_stories_row"),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(
                    items = StoreCategory.entries,
                    key = { it.id }
                ) { cat ->
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

@Composable
fun CatalogoCategoryStoriesSection(
    selectedCategory: String,
    categoryListState: LazyListState,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
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
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selectedCategory != "todo") {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EleganBurgundy.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, EleganBurgundy.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { onCategoryChange("todo") }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ver todo",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EleganBurgundy,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar filtro",
                            tint = EleganBurgundy,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val spacing = if (maxWidth < 350.dp) 6.dp else 8.dp
            val visibleCards = if (maxWidth >= 600.dp) 6f else 4f
            val cardWidth = ((maxWidth - (spacing * (visibleCards - 1f))) / visibleCards)
                .coerceIn(66.dp, 118.dp)

            LazyRow(
                state = categoryListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalogo_category_stories_row"),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(StoreCategory.entries, key = { it.id }) { cat ->
                    val isSelected = selectedCategory.equals(cat.id, ignoreCase = true)
                    CatalogoCategoryStoryCard(
                        category = cat,
                        cardWidth = cardWidth,
                        isSelected = isSelected,
                        onClick = { onCategoryChange(cat.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CatalogoCategoryStoryCard(
    category: StoreCategory,
    cardWidth: Dp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderStroke = if (isSelected) {
        BorderStroke(
            2.5.dp,
            Brush.verticalGradient(
                colors = listOf(
                    EleganGold,
                    EleganBurgundy
                )
            )
        )
    } else {
        BorderStroke(
            1.dp,
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE5DDD5),
                    Color(0xFFC8BFB5)
                )
            )
        )
    }

    Surface(
        modifier = Modifier
            .width(cardWidth)
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("catalogo_cat_story_${category.id}"),
        shape = RoundedCornerShape(10.dp),
        border = borderStroke,
        shadowElevation = if (isSelected) 6.dp else 2.dp,
        color = EleganCharcoal
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = getCategoryStoryImage(category),
                contentDescription = category.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = if (isSelected) 0.5f else 0.4f),
                            0.28f to Color.Transparent,
                            0.52f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.90f)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.TopStart)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) EleganBurgundy else Color.Black.copy(alpha = 0.6f))
                    .border(
                        1.dp,
                        if (isSelected) EleganGold else Color.White.copy(alpha = 0.8f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryStoryIcon(category),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .align(Alignment.TopEnd)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EleganGold)
                        .border(1.dp, Color.White, CircleShape)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EleganBurgundy.copy(alpha = 0.85f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
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
                } else {
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
}
