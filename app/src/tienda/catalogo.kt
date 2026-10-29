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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.example.data.model.StoreCategory
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    products: List<Product>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onNavigateToMenu: () -> Unit,
    onProductClick: (Product) -> Unit,
    onQuickAddToCart: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val categoryListState = rememberLazyListState()

    LaunchedEffect(selectedCategory) {
        val categories = StoreCategory.values()
        val targetIndex = categories.indexOfFirst { it.id.equals(selectedCategory, ignoreCase = true) }
        if (targetIndex >= 0) {
            categoryListState.animateScrollToItem(targetIndex)
        }
    }

    val filteredProducts = remember(products, selectedCategory, searchQuery) {
        products.filter { prod ->
            val matchesCat = selectedCategory == "todo" || prod.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.description.contains(searchQuery, ignoreCase = true) ||
                    prod.category.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateToMenu,
                        modifier = Modifier.testTag("catalogo_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Abrir menú de categorías",
                            tint = EleganCharcoal
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF6F5F2))
                            .border(1.dp, Color(0xFFDDD5CE), CircleShape)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = EleganBurgundy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("catalogo_search_field"),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = EleganCharcoal),
                            cursorBrush = SolidColor(EleganBurgundy),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Buscar prendas, zapatos, perfumes...",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar búsqueda",
                                tint = EleganCharcoal,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }
            }
        },
        containerColor = EleganIvory
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("catalogo_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Column(
                    modifier = Modifier
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

                        if (selectedCategory != "todo") {
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
                                        )
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

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val spacing = 8.dp
                        val cardWidth = ((maxWidth - (spacing * 3)) / 4).coerceAtLeast(70.dp)

                        LazyRow(
                            state = categoryListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("catalogo_category_stories_row"),
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(StoreCategory.values()) { cat ->
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

            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredProducts.size} prendas encontradas",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            if (filteredProducts.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No se encontraron productos",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Intenta con otro término de búsqueda o selecciona otra categoría.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    onCategoryChange("todo")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy)
                            ) {
                                Text("Ver todo el catálogo")
                            }
                        }
                    }
                }
            }

            items(filteredProducts, key = { it.id }) { product ->
                ProductGridCard(
                    product = product,
                    onClick = { onProductClick(product) },
                    onAddToCart = {
                        onQuickAddToCart(product)
                        scope.launch {
                            snackbarHostState.showSnackbar("Añadido al carrito: ${product.name}")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProductGridCard(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("catalog_item_${product.id}"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFFF3EFEA))
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    color = EleganBurgundyDark,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = product.category.uppercase(),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onAddToCart,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .testTag("quick_add_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "Añadir al carrito",
                        tint = EleganBurgundy,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = EleganCharcoal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$${String.format("%.2f", product.price)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganBurgundy
                        )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = EleganGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${product.rating}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogoCategoryStoryCard(
    category: StoreCategory,
    cardWidth: androidx.compose.ui.unit.Dp,
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
