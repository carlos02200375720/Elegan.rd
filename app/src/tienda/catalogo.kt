package com.example.ui

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.data.model.StoreCategory
import com.example.ui.components.LocalEleganNotifier
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    products: List<Product>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onNavigateToMenu: () -> Unit,
    onProductClick: (Product) -> Unit,
    onQuickAddToCart: (Product) -> Unit = {},
    cartItemCount: Long = 0L,
    onNavigateToCart: () -> Unit = {},
    savedProductIds: Set<String> = emptySet(),
    onToggleSaveProduct: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val notifier = LocalEleganNotifier.current
    val categoryListState = rememberLazyListState()

    LaunchedEffect(selectedCategory) {
        val categories = StoreCategory.entries
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
            Column(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isCompactTop = maxWidth < 350.dp
                    val isWideTop = maxWidth >= 600.dp

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactTop) 48.dp else 52.dp),
                        color = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (isWideTop) 12.dp else 4.dp),
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
                            Spacer(modifier = Modifier.width(if (isCompactTop) 2.dp else 4.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 560.dp)
                                        .height(if (isCompactTop) 36.dp else 38.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFDDD5CE), CircleShape)
                                        .padding(horizontal = if (isCompactTop) 10.dp else 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = EleganBurgundy,
                                        modifier = Modifier.size(if (isCompactTop) 18.dp else 20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(if (isCompactTop) 6.dp else 8.dp))
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("catalogo_search_field"),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = EleganCharcoal,
                                            fontSize = if (isCompactTop) 13.sp else 14.sp
                                        ),
                                        cursorBrush = SolidColor(EleganBurgundy),
                                        decorationBox = { innerTextField ->
                                            Box(contentAlignment = Alignment.CenterStart) {
                                                if (searchQuery.isEmpty()) {
                                                    Text(
                                                        text = "Buscar prendas, zapatos, perfumes...",
                                                        fontSize = if (isCompactTop) 13.sp else 14.sp,
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
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .clickable { searchQuery = "" },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Limpiar búsqueda",
                                                tint = EleganCharcoal,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(if (isCompactTop) 2.dp else 4.dp))
                            IconButton(
                                onClick = onNavigateToCart,
                                modifier = Modifier.testTag("catalogo_cart_button")
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
                }
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
            val isMedium = maxWidth >= 600.dp && maxWidth < 840.dp
            val isExpanded = maxWidth >= 840.dp

            val columnsCount = when {
                isExpanded -> 4
                isMedium -> 3
                else -> 2
            }
            val horizontalPad = when {
                isCompact -> 10.dp
                isMedium || isExpanded -> 20.dp
                else -> 16.dp
            }
            val columnGap = if (isCompact) 8.dp else 12.dp
            val rowGap = if (isCompact) 10.dp else 14.dp

            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1120.dp)
                    .testTag("catalogo_screen"),
                contentPadding = PaddingValues(
                    start = horizontalPad,
                    end = horizontalPad,
                    bottom = if (isCompact) 12.dp else 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(columnGap),
                verticalArrangement = Arrangement.spacedBy(rowGap)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CatalogoCategoryStoriesSection(
                        selectedCategory = selectedCategory,
                        categoryListState = categoryListState,
                        onCategoryChange = onCategoryChange
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (isCompact) 2.dp else 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredProducts.size} prendas encontradas",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                fontSize = if (isCompact) 11.5.sp else 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (filteredProducts.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (isCompact) 16.dp else 32.dp, vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No se encontraron productos",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Intenta con otro término de búsqueda o selecciona otra categoría.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Gray
                                    ),
                                    textAlign = TextAlign.Center
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
                    val isSaved = savedProductIds.contains(product.id)
                    ProductGridCard(
                        product = product,
                        isSaved = isSaved,
                        onClick = { onProductClick(product) },
                        onToggleSave = {
                            val willSave = !isSaved
                            onToggleSaveProduct(product.id)
                            val msg = if (willSave) "Guardado en tu colección" else "Eliminado de guardados"
                            if (notifier != null) {
                                notifier.show(msg)
                            } else {
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductGridCard(
    product: Product,
    isSaved: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit
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
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompactCard = maxWidth < 165.dp
            val contentPadding = if (isCompactCard) 8.dp else 10.dp

            Column(modifier = Modifier.fillMaxWidth()) {
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
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .widthIn(max = this@BoxWithConstraints.maxWidth * 0.78f)
                    ) {
                        Text(
                            text = product.category.uppercase(),
                            color = Color.White,
                            fontSize = if (isCompactCard) 7.5.sp else 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(
                                horizontal = if (isCompactCard) 5.dp else 6.dp,
                                vertical = 2.dp
                            )
                        )
                    }
                }

                Column(modifier = Modifier.padding(contentPadding)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EleganCharcoal,
                            fontSize = if (isCompactCard) 13.sp else 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(if (isCompactCard) 2.dp else 3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$${String.format("%.2f", product.price)}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganBurgundy,
                                fontSize = if (isCompactCard) 13.sp else 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onToggleSave,
                            modifier = Modifier
                                .size(if (isCompactCard) 28.dp else 32.dp)
                                .testTag("catalog_save_${product.id}")
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isSaved) "Guardado en colección" else "Guardar en colección guardado",
                                tint = if (isSaved) EleganGold else Color(0xFF8E8E93),
                                modifier = Modifier.size(if (isCompactCard) 20.dp else 22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
