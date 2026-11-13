package com.example.admin

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Announcement
import com.example.data.model.Product
import com.example.data.model.StoreCategory
import com.example.ui.EleganTopBarShadow
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory
import kotlinx.coroutines.launch

@Composable
fun PublicacionesScreen(
    products: List<Product>,
    announcements: List<Announcement>,
    onDeleteProduct: (String) -> Unit,
    onToggleProductStock: (String, Boolean) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onToggleAnnouncementActive: (String, Boolean) -> Unit,
    onNavigateToPublicar: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Todos, 1 = Productos, 2 = Anuncios
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("todo") }

    // Dialogs for deletion
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var announcementToDelete by remember { mutableStateOf<Announcement?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Filtered lists
    val filteredProducts = remember(products, searchQuery, selectedCategoryFilter) {
        products.filter { prod ->
            val matchesCategory = (selectedCategoryFilter == "todo" || prod.category.equals(selectedCategoryFilter, ignoreCase = true))
            val matchesQuery = searchQuery.isBlank() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.description.contains(searchQuery, ignoreCase = true) ||
                    prod.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val filteredAnnouncements = remember(announcements, searchQuery, selectedCategoryFilter) {
        announcements.filter { ann ->
            val matchesCategory = (selectedCategoryFilter == "todo" || ann.targetCategory.equals(selectedCategoryFilter, ignoreCase = true))
            val matchesQuery = searchQuery.isBlank() ||
                    ann.title.contains(searchQuery, ignoreCase = true) ||
                    ann.message.contains(searchQuery, ignoreCase = true) ||
                    ann.discountCode.contains(searchQuery, ignoreCase = true) ||
                    ann.type.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val totalCount = filteredProducts.size + filteredAnnouncements.size

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isCompactTop = maxWidth < 360.dp
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (isCompactTop) 4.dp else 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("publicasione_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Regresar a Menú Admin",
                                    tint = EleganCharcoal
                                )
                            }

                            Spacer(modifier = Modifier.width(if (isCompactTop) 2.dp else 4.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    text = "Control de Publicaciones",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = if (isCompactTop) 15.sp else 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal,
                                        fontFamily = FontFamily.Serif
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Administrar catálogo y anuncios",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Gray,
                                        fontSize = if (isCompactTop) 10.sp else 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                EleganTopBarShadow()
            }
        },
        containerColor = Color(0xFFF9F7F5)
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isCompact = maxWidth < 360.dp
            val horizontalPad = if (isCompact) 10.dp else 16.dp
            val verticalGap = if (isCompact) 12.dp else 16.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("publicasione_screen_list"),
                contentPadding = PaddingValues(horizontal = horizontalPad, vertical = verticalGap),
                verticalArrangement = Arrangement.spacedBy(verticalGap)
            ) {
                // KPI Summary Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)
                    ) {
                        val kpiPad = if (isCompact) 9.dp else 12.dp
                        val kpiLabelSize = if (isCompact) 9.sp else 10.sp
                        val kpiLetterSpacing = if (isCompact) 0.4.sp else 1.sp
                        val kpiNumberSize = if (isCompact) 17.sp else 20.sp
                        val kpiSubSize = if (isCompact) 10.sp else 11.sp

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(kpiPad)) {
                                Text(
                                    text = "TOTAL",
                                    fontSize = kpiLabelSize,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = kpiLetterSpacing,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${products.size + announcements.size}",
                                    fontSize = kpiNumberSize,
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Publicaciones",
                                    fontSize = kpiSubSize,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(kpiPad)) {
                                Text(
                                    text = "PRODUCTOS",
                                    fontSize = kpiLabelSize,
                                    fontWeight = FontWeight.Bold,
                                    color = EleganBurgundy,
                                    letterSpacing = kpiLetterSpacing,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${products.count { it.inStock }}/${products.size}",
                                    fontSize = kpiNumberSize,
                                    fontWeight = FontWeight.Bold,
                                    color = EleganBurgundy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "En Stock",
                                    fontSize = kpiSubSize,
                                    color = Color(0xFF2E7D32),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(kpiPad)) {
                                Text(
                                    text = "ANUNCIOS",
                                    fontSize = kpiLabelSize,
                                    fontWeight = FontWeight.Bold,
                                    color = EleganGold,
                                    letterSpacing = kpiLetterSpacing,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${announcements.count { it.isActive }}/${announcements.size}",
                                    fontSize = kpiNumberSize,
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Activos",
                                    fontSize = kpiSubSize,
                                    color = Color(0xFF2E7D32),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publicasione_search_input"),
                        placeholder = {
                            Text(
                                text = "Buscar por nombre, código o categoría...",
                                fontSize = if (isCompact) 12.sp else 13.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = Color.Gray
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar búsqueda",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = EleganBurgundy,
                            unfocusedBorderColor = Color(0xFFE2E2E2)
                        )
                    )
                }

                // Tabs Selector
                item {
                    val tabFontSize = if (isCompact) 11.sp else 13.sp
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 1.dp
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.White,
                            contentColor = EleganBurgundy,
                            indicator = { tabPositions ->
                                if (selectedTab < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                        color = EleganBurgundy,
                                        height = 3.dp
                                    )
                                }
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                modifier = Modifier.testTag("publicasione_tab_todo"),
                                text = {
                                    Text(
                                        text = "Todo (${filteredProducts.size + filteredAnnouncements.size})",
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = tabFontSize,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                modifier = Modifier.testTag("publicasione_tab_productos"),
                                text = {
                                    Text(
                                        text = "Productos (${filteredProducts.size})",
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = tabFontSize,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                modifier = Modifier.testTag("publicasione_tab_anuncios"),
                                text = {
                                    Text(
                                        text = "Anuncios (${filteredAnnouncements.size})",
                                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = tabFontSize,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

            // Category Chips (for Todo & Productos)
            if (selectedTab != 2) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "FILTRAR POR CATEGORÍA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(StoreCategory.values()) { cat ->
                                val isSelected = selectedCategoryFilter == cat.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategoryFilter = cat.id },
                                    label = {
                                        Text(
                                            text = cat.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EleganBurgundy,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.White,
                                        labelColor = EleganCharcoal
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.testTag("publicasione_chip_${cat.id}")
                                )
                            }
                        }
                    }
                }
            }

            // Content List based on Selected Tab
            when (selectedTab) {
                0 -> {
                    // TODOS: Show products and announcements
                    if (filteredProducts.isEmpty() && filteredAnnouncements.isEmpty()) {
                        item {
                            EmptyPublicationsView(
                                searchQuery = searchQuery,
                                onClearFilter = {
                                    searchQuery = ""
                                    selectedCategoryFilter = "todo"
                                },
                                onNavigateToPublicar = onNavigateToPublicar
                            )
                        }
                    } else {
                        // Section: Products Header
                        if (filteredProducts.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = EleganBurgundy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PRODUCTOS DEL CATÁLOGO (${filteredProducts.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            items(filteredProducts, key = { "prod_${it.id}" }) { product ->
                                ProductAdminCard(
                                    product = product,
                                    onToggleStock = { inStock ->
                                        onToggleProductStock(product.id, inStock)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Stock de '${product.name}' actualizado a: ${if (inStock) "En Stock" else "Agotado"}"
                                            )
                                        }
                                    },
                                    onDeleteClick = { productToDelete = product }
                                )
                            }
                        }

                        // Section: Announcements Header
                        if (filteredAnnouncements.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = EleganGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ANUNCIOS & CAMPAÑAS (${filteredAnnouncements.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            items(filteredAnnouncements, key = { "ann_${it.id}" }) { announcement ->
                                AnnouncementAdminCard(
                                    announcement = announcement,
                                    onToggleActive = { active ->
                                        onToggleAnnouncementActive(announcement.id, active)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Campaña '${announcement.title}' ${if (active) "activada" else "pausada"}"
                                            )
                                        }
                                    },
                                    onDeleteClick = { announcementToDelete = announcement }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // ONLY PRODUCTS
                    if (filteredProducts.isEmpty()) {
                        item {
                            EmptyPublicationsView(
                                searchQuery = searchQuery,
                                onClearFilter = {
                                    searchQuery = ""
                                    selectedCategoryFilter = "todo"
                                },
                                onNavigateToPublicar = onNavigateToPublicar
                            )
                        }
                    } else {
                        items(filteredProducts, key = { "prod_${it.id}" }) { product ->
                            ProductAdminCard(
                                product = product,
                                onToggleStock = { inStock ->
                                    onToggleProductStock(product.id, inStock)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Stock de '${product.name}' actualizado a: ${if (inStock) "En Stock" else "Agotado"}"
                                        )
                                    }
                                },
                                onDeleteClick = { productToDelete = product }
                            )
                        }
                    }
                }

                2 -> {
                    // ONLY ANNOUNCEMENTS
                    if (filteredAnnouncements.isEmpty()) {
                        item {
                            EmptyPublicationsView(
                                searchQuery = searchQuery,
                                onClearFilter = {
                                    searchQuery = ""
                                    selectedCategoryFilter = "todo"
                                },
                                onNavigateToPublicar = onNavigateToPublicar
                            )
                        }
                    } else {
                        items(filteredAnnouncements, key = { "ann_${it.id}" }) { announcement ->
                            AnnouncementAdminCard(
                                announcement = announcement,
                                onToggleActive = { active ->
                                    onToggleAnnouncementActive(announcement.id, active)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Campaña '${announcement.title}' ${if (active) "activada" else "pausada"}"
                                        )
                                    }
                                },
                                onDeleteClick = { announcementToDelete = announcement }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        }
    }

    // Product Deletion Dialog
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = EleganBurgundy,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Eliminar Publicación",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Estás seguro de que deseas eliminar este producto del catálogo?",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = prod.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = EleganCharcoal
                            )
                            Text(
                                text = "$${String.format("%.2f", prod.price)} • Categoría: ${prod.category}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Text(
                        text = "Esta acción es irreversible y removerá el producto de las vitrinas de los clientes.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = prod.id
                        val name = prod.name
                        productToDelete = null
                        onDeleteProduct(id)
                        scope.launch {
                            snackbarHostState.showSnackbar("Producto '$name' eliminado con éxito.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_delete_product_button")
                ) {
                    Text("Eliminar definitivamente", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { productToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar", color = EleganCharcoal)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Announcement Deletion Dialog
    announcementToDelete?.let { ann ->
        AlertDialog(
            onDismissRequest = { announcementToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = EleganBurgundy,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Eliminar Anuncio",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Deseas retirar esta campaña o anuncio de la aplicación?",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = ann.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = EleganCharcoal
                            )
                            Text(
                                text = "Tipo: ${ann.type.uppercase()} • ${ann.message.take(60)}...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = ann.id
                        val title = ann.title
                        announcementToDelete = null
                        onDeleteAnnouncement(id)
                        scope.launch {
                            snackbarHostState.showSnackbar("Anuncio '$title' eliminado con éxito.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_delete_announcement_button")
                ) {
                    Text("Eliminar Anuncio", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { announcementToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar", color = EleganCharcoal)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ProductAdminCard(
    product: Product,
    onToggleStock: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_admin_card_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 330.dp
            val thumbSize = if (isCompact) 72.dp else 86.dp
            val cardPad = if (isCompact) 11.dp else 14.dp

            Column(modifier = Modifier.padding(cardPad)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(thumbSize)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF0F0F0))
                    ) {
                        AsyncImage(
                            model = product.imageUrl.ifBlank { product.images.firstOrNull() ?: "" },
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 6.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = product.mediaFormat.ifBlank { "1:1" },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        if (product.isVideo) {
                            Surface(
                                color = EleganBurgundy,
                                shape = RoundedCornerShape(topStart = 6.dp),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "VIDEO",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            val extraImagesCount = if (product.images.size > 1) product.images.size else 0
                            if (extraImagesCount > 1) {
                                Surface(
                                    color = EleganBurgundy.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(topStart = 6.dp),
                                    modifier = Modifier.align(Alignment.BottomEnd)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Collections,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "$extraImagesCount",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Surface(
                                    color = EleganBurgundy.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = product.category.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (product.isFeatured) {
                                Surface(
                                    color = EleganGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Destacado",
                                            tint = EleganGold,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "DESTACADO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EleganCharcoal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = if (isCompact) 13.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "$${String.format("%.2f", product.price)} USD",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = if (isCompact) 15.sp else 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EleganBurgundy,
                                fontFamily = FontFamily.Serif
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_stock_product_${product.id}")
                    ) {
                        Switch(
                            checked = product.inStock,
                            onCheckedChange = { onToggleStock(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2E7D32),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.LightGray
                            ),
                            modifier = Modifier.testTag("toggle_stock_product_switch_${product.id}")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (product.inStock) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (product.inStock) "EN STOCK" else "AGOTADO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (product.inStock) Color(0xFF2E7D32) else Color(0xFFC62828),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_product_btn_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar Producto",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementAdminCard(
    announcement: Announcement,
    onToggleActive: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("announcement_admin_card_${announcement.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 330.dp
            val thumbSize = if (isCompact) 72.dp else 86.dp
            val cardPad = if (isCompact) 11.dp else 14.dp

            Column(modifier = Modifier.padding(cardPad)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(thumbSize)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when (announcement.type) {
                                    "descuento" -> Color(0xFFFFF3E0)
                                    "coleccion" -> Color(0xFFEDE7F6)
                                    else -> Color(0xFFE3F2FD)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (announcement.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = announcement.imageUrl,
                                contentDescription = announcement.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = when (announcement.type) {
                                    "descuento" -> Icons.Default.LocalOffer
                                    "coleccion" -> Icons.Default.Collections
                                    else -> Icons.Default.Campaign
                                },
                                contentDescription = null,
                                tint = EleganBurgundy,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        if (announcement.isVideo) {
                            Surface(
                                color = EleganBurgundy,
                                shape = RoundedCornerShape(topStart = 6.dp),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "VIDEO",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = when (announcement.type) {
                                "descuento" -> EleganGold.copy(alpha = 0.25f)
                                "coleccion" -> Color(0xFFE1BEE7)
                                else -> EleganBurgundy.copy(alpha = 0.1f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = announcement.type.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (announcement.type) {
                                    "descuento" -> Color(0xFFB78103)
                                    "coleccion" -> Color(0xFF6A1B9A)
                                    else -> EleganBurgundy
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = announcement.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = if (isCompact) 13.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = announcement.message,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray,
                                fontSize = 12.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (announcement.discountCode.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Discount,
                                    contentDescription = null,
                                    tint = EleganBurgundy,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cupón: ${announcement.discountCode} (-${announcement.discountPercent.toInt()}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EleganBurgundy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_active_announcement_${announcement.id}")
                    ) {
                        Switch(
                            checked = announcement.isActive,
                            onCheckedChange = { onToggleActive(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF2E7D32),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.LightGray
                            ),
                            modifier = Modifier.testTag("toggle_active_announcement_switch_${announcement.id}")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = if (announcement.isActive) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (announcement.isActive) "ACTIVO EN TIENDA" else "PAUSADO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (announcement.isActive) Color(0xFF2E7D32) else Color(0xFFE65100),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("delete_announcement_btn_${announcement.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar Anuncio",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPublicationsView(
    searchQuery: String,
    onClearFilter: () -> Unit,
    onNavigateToPublicar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 330.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isCompact) 18.dp else 28.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (searchQuery.isNotEmpty()) "Sin resultados para tu búsqueda" else "No hay publicaciones en esta sección",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (searchQuery.isNotEmpty())
                        "Intenta buscar con otros términos o limpia el filtro actual."
                    else
                        "Comienza a publicar nuevos productos o campañas para tus clientes.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray,
                        fontSize = 12.sp
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isCompact && searchQuery.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedButton(
                            onClick = onClearFilter,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Limpiar Filtros", color = EleganCharcoal)
                        }
                        Button(
                            onClick = onNavigateToPublicar,
                            colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nueva Publicación", color = Color.White)
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onClearFilter,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Limpiar Filtros", color = EleganCharcoal, maxLines = 1)
                            }
                        }
                        Button(
                            onClick = onNavigateToPublicar,
                            colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nueva Publicación", color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
