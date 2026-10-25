package com.example.admin

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Announcement
import com.example.data.model.Product
import com.example.data.model.StoreCategory
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory
import kotlinx.coroutines.launch

@Composable
fun PublicarScreen(
    onPublishProduct: (Product) -> Unit = {},
    onPublishAnnouncement: (Announcement) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    // -------------------------------------------------------------------------
    // ESTADO: PUBLICAR PRODUCTO
    // -------------------------------------------------------------------------
    var productName by remember { mutableStateOf("") }
    var productDescription by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("camisa") }
    var productPriceText by remember { mutableStateOf("") }
    var productMainImageUrl by remember { mutableStateOf("") }
    var productMediaFormat by remember { mutableStateOf("1:1") }
    val productGalleryImages = remember { mutableStateListOf<String>() }
    var newGalleryUrlInput by remember { mutableStateOf("") }

    val availableSizeOptions = listOf("S", "M", "L", "XL", "XXL", "Única (Estándar)")
    val selectedSizes = remember { mutableStateListOf("S", "M", "L", "XL") }
    var isProductFeatured by remember { mutableStateOf(true) }
    var isProductInStock by remember { mutableStateOf(true) }
    var isSubmittingProduct by remember { mutableStateOf(false) }

    // -------------------------------------------------------------------------
    // ESTADO: PUBLICAR ANUNCIO
    // -------------------------------------------------------------------------
    var announcementTitle by remember { mutableStateOf("") }
    var announcementMessage by remember { mutableStateOf("") }
    var announcementType by remember { mutableStateOf("banner") } // banner, historia_9_16, descuento, coleccion
    var announcementImageUrl by remember { mutableStateOf("") }
    var announcementDiscountCode by remember { mutableStateOf("") }
    var announcementDiscountPercentText by remember { mutableStateOf("") }
    var announcementCtaText by remember { mutableStateOf("Ver Colección") }
    var announcementTargetCategory by remember { mutableStateOf("todo") }
    var isAnnouncementActive by remember { mutableStateOf(true) }
    var isSubmittingAnnouncement by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        modifier = Modifier.testTag("admin_publicar_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = EleganCharcoal
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    ) {
                        Text(
                            text = "Panel de Publicación",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal,
                                fontFamily = FontFamily.Serif
                            )
                        )
                        Text(
                            text = "ELEGAN Boutique Oficial • Administrador",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EleganGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        },
        containerColor = EleganIvory
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pestañas de Navegación: Producto vs Anuncio
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = EleganBurgundy,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = EleganBurgundy,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = {
                        Text(
                            text = "Publicar Producto",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    selectedContentColor = EleganBurgundy,
                    unselectedContentColor = Color.Gray
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = {
                        Text(
                            text = "Publicar Anuncio",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    selectedContentColor = EleganBurgundy,
                    unselectedContentColor = Color.Gray
                )
            }

            // Contenido dinámico según la pestaña activa
            if (selectedTab == 0) {
                // =============================================================
                // FORMULARIO: PUBLICAR PRODUCTO
                // =============================================================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "1. Información General",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = productName,
                                    onValueChange = { productName = it },
                                    label = { Text("Nombre del Producto") },
                                    placeholder = { Text("Ej. Traje Sartorial Borgoña Tres Piezas") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_product_name_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EleganBurgundy,
                                        focusedLabelColor = EleganBurgundy
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = productPriceText,
                                    onValueChange = { productPriceText = it },
                                    label = { Text("Precio en USD ($)") },
                                    placeholder = { Text("Ej. 185.00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_product_price_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF16A34A),
                                        focusedLabelColor = Color(0xFF16A34A)
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = productDescription,
                                    onValueChange = { productDescription = it },
                                    label = { Text("Descripción y Confección") },
                                    placeholder = { Text("Materiales nobles, detalles de confección, corte, etc.") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .testTag("admin_product_desc_input"),
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EleganBurgundy,
                                        focusedLabelColor = EleganBurgundy
                                    )
                                )
                            }
                        }
                    }

                    // Categoría
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "2. Categoría de Boutique",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val categories = StoreCategory.values().filter { it != StoreCategory.TODO }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(categories) { cat ->
                                        val isSelected = productCategory == cat.id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { productCategory = cat.id },
                                            label = { Text(cat.title, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = EleganBurgundy,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Formato y Multimedia
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "3. Formato y Galería Multimedia",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Formato de Publicación en Inicio y Catálogo:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("1:1" to "Cuadrado (1:1)", "3:4" to "Retrato (3:4)", "4:3" to "Horizontal (4:3)").forEach { (formatCode, label) ->
                                        val isSel = productMediaFormat == formatCode
                                        OutlinedButton(
                                            onClick = { productMediaFormat = formatCode },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isSel) EleganBurgundy else Color.White,
                                                contentColor = if (isSel) Color.White else EleganCharcoal
                                            ),
                                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                                brush = androidx.compose.ui.graphics.SolidColor(
                                                    if (isSel) EleganBurgundy else Color(0xFFD6CEC5)
                                                )
                                            ),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = productMainImageUrl,
                                    onValueChange = { productMainImageUrl = it },
                                    label = { Text("URL de la Imagen Principal") },
                                    placeholder = { Text("https://images.unsplash.com/photo-...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_product_img_input"),
                                    singleLine = true,
                                    trailingIcon = {
                                        if (productMainImageUrl.isNotBlank()) {
                                            IconButton(onClick = { productMainImageUrl = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Limpiar")
                                            }
                                        }
                                    }
                                )

                                // Botón rápido con sugerencias de muestra de calidad
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "O usar imagen curada de muestra:",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            productMainImageUrl = when (productCategory) {
                                                "gorra" -> "https://images.unsplash.com/photo-1576871337622-98d48d1cf531?w=800&auto=format&fit=crop&q=80"
                                                "gafa" -> "https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=800&auto=format&fit=crop&q=80"
                                                "camisa" -> "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800&auto=format&fit=crop&q=80"
                                                "correa" -> "https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=800&auto=format&fit=crop&q=80"
                                                "pantalone" -> "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800&auto=format&fit=crop&q=80"
                                                "zapato" -> "https://images.unsplash.com/photo-1614252235316-8c857d38b5f4?w=800&auto=format&fit=crop&q=80"
                                                "interiore" -> "https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?w=800&auto=format&fit=crop&q=80"
                                                "perfume" -> "https://images.unsplash.com/photo-1594035910387-fea47794261f?w=800&auto=format&fit=crop&q=80"
                                                else -> "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800&auto=format&fit=crop&q=80"
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Cargar Ejemplo", fontSize = 11.sp)
                                    }
                                }

                                // Vista previa en vivo de la imagen principal
                                if (productMainImageUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF0EBE3)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = productMainImageUrl,
                                            contentDescription = "Vista previa",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = "Formato $productMediaFormat",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color(0xFFEEE7DF))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Múltiple contenido (Galería hasta 10 fotos con indicador)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Fotos adicionales de la Galería:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Surface(
                                        color = EleganBurgundy.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "${productGalleryImages.size + if (productMainImageUrl.isNotBlank()) 1 else 0}/10 contenidos",
                                            color = EleganBurgundy,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newGalleryUrlInput,
                                        onValueChange = { newGalleryUrlInput = it },
                                        placeholder = { Text("URL de imagen adicional") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (newGalleryUrlInput.isNotBlank()) {
                                                productGalleryImages.add(newGalleryUrlInput.trim())
                                                newGalleryUrlInput = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Añadir")
                                    }
                                }

                                if (productGalleryImages.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(productGalleryImages) { imgUrl ->
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFEFE8E1))
                                            ) {
                                                AsyncImage(
                                                    model = imgUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                IconButton(
                                                    onClick = { productGalleryImages.remove(imgUrl) },
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .align(Alignment.TopEnd)
                                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Quitar", tint = Color.White, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tallas y Opciones
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "4. Tallas y Disponibilidad",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Tallas disponibles para el cliente:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    availableSizeOptions.forEach { sz ->
                                        val isSelected = selectedSizes.contains(sz)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                if (isSelected) selectedSizes.remove(sz) else selectedSizes.add(sz)
                                            },
                                            label = { Text(sz, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = EleganBurgundy,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color(0xFFEEE7DF))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Destacar en Página de Inicio", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Aparecerá en el feed principal con formato $productMediaFormat", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = isProductFeatured,
                                        onCheckedChange = { isProductFeatured = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = EleganBurgundy)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Disponible en Stock", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Permite compra inmediata en línea", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = isProductInStock,
                                        onCheckedChange = { isProductInStock = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = EleganBurgundy)
                                    )
                                }
                            }
                        }
                    }

                    // Botón para Publicar Producto
                    item {
                        Button(
                            onClick = {
                                val priceValue = productPriceText.toDoubleOrNull()
                                if (productName.isBlank()) {
                                    scope.launch { snackbarHostState.showSnackbar("Por favor ingresa el nombre del producto.") }
                                    return@Button
                                }
                                if (priceValue == null || priceValue <= 0.0) {
                                    scope.launch { snackbarHostState.showSnackbar("Por favor ingresa un precio válido mayor a 0.") }
                                    return@Button
                                }
                                if (productMainImageUrl.isBlank()) {
                                    scope.launch { snackbarHostState.showSnackbar("Por favor añade la URL de la imagen principal.") }
                                    return@Button
                                }

                                isSubmittingProduct = true
                                val newProduct = Product(
                                    name = productName.trim(),
                                    description = productDescription.trim(),
                                    category = productCategory,
                                    price = priceValue,
                                    imageUrl = productMainImageUrl.trim(),
                                    images = if (productGalleryImages.isNotEmpty()) listOf(productMainImageUrl.trim()) + productGalleryImages else emptyList(),
                                    sizes = if (selectedSizes.isNotEmpty()) selectedSizes.toList() else listOf("Estándar"),
                                    inStock = isProductInStock,
                                    isFeatured = isProductFeatured,
                                    mediaFormat = productMediaFormat
                                )

                                onPublishProduct(newProduct)
                                scope.launch {
                                    snackbarHostState.showSnackbar("¡Producto '${newProduct.name}' publicado con éxito!")
                                    productName = ""
                                    productDescription = ""
                                    productPriceText = ""
                                    productMainImageUrl = ""
                                    productGalleryImages.clear()
                                    isSubmittingProduct = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_submit_product_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EleganBurgundy,
                                contentColor = Color.White
                            ),
                            enabled = !isSubmittingProduct
                        ) {
                            if (isSubmittingProduct) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Publicar Producto en ELEGAN",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // =============================================================
                // FORMULARIO: PUBLICAR ANUNCIO / CAMPAÑA
                // =============================================================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "1. Mensaje del Anuncio",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = announcementTitle,
                                    onValueChange = { announcementTitle = it },
                                    label = { Text("Título del Anuncio / Campaña") },
                                    placeholder = { Text("Ej. Gran Venta de Otoño Sartorial") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_announcement_title_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EleganBurgundy,
                                        focusedLabelColor = EleganBurgundy
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = announcementMessage,
                                    onValueChange = { announcementMessage = it },
                                    label = { Text("Mensaje o Subtítulo Promocional") },
                                    placeholder = { Text("Ej. Hasta 25% de descuento en trajes italianos por tiempo limitado.") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .testTag("admin_announcement_msg_input"),
                                    maxLines = 3
                                )
                            }
                        }
                    }

                    // Tipo de Formato de Anuncio
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "2. Tipo y Formato de Anuncio",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val types = listOf(
                                    "banner" to "Banner Principal",
                                    "historia_9_16" to "Historia / Estado 9:16",
                                    "descuento" to "Cupón Especial",
                                    "coleccion" to "Nueva Colección"
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(types) { (typeCode, typeLabel) ->
                                        val isSel = announcementType == typeCode
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { announcementType = typeCode },
                                            label = { Text(typeLabel, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = EleganBurgundy,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Imagen Promocional
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "3. Imagen del Anuncio",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = announcementImageUrl,
                                    onValueChange = { announcementImageUrl = it },
                                    label = { Text("URL de la Imagen Promocional") },
                                    placeholder = { Text("https://images.unsplash.com/photo-...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_announcement_img_input"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("O sugerencia predeterminada:", fontSize = 11.sp, color = Color.Gray)
                                    OutlinedButton(
                                        onClick = {
                                            announcementImageUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800&auto=format&fit=crop&q=80"
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Cargar Ejemplo", fontSize = 11.sp)
                                    }
                                }

                                if (announcementImageUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF0EBE3)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = announcementImageUrl,
                                            contentDescription = "Vista previa del anuncio",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Cupones y CTA
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "4. Código y Botón de Acción",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = announcementDiscountCode,
                                        onValueChange = { announcementDiscountCode = it.uppercase() },
                                        label = { Text("Código de Cupón") },
                                        placeholder = { Text("ELEGAN20") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = announcementDiscountPercentText,
                                        onValueChange = { announcementDiscountPercentText = it },
                                        label = { Text("% Descuento") },
                                        placeholder = { Text("20") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = announcementCtaText,
                                    onValueChange = { announcementCtaText = it },
                                    label = { Text("Texto del Botón de Acción") },
                                    placeholder = { Text("Explorar Colección") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Publicar Inmediatamente", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Visible para todos los clientes en vivo", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Switch(
                                        checked = isAnnouncementActive,
                                        onCheckedChange = { isAnnouncementActive = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = EleganBurgundy)
                                    )
                                }
                            }
                        }
                    }

                    // Botón para Publicar Anuncio
                    item {
                        Button(
                            onClick = {
                                if (announcementTitle.isBlank()) {
                                    scope.launch { snackbarHostState.showSnackbar("Por favor ingresa el título del anuncio.") }
                                    return@Button
                                }
                                val discountNum = announcementDiscountPercentText.toDoubleOrNull() ?: 0.0

                                isSubmittingAnnouncement = true
                                val newAnnouncement = Announcement(
                                    title = announcementTitle.trim(),
                                    message = announcementMessage.trim(),
                                    type = announcementType,
                                    imageUrl = announcementImageUrl.trim(),
                                    discountCode = announcementDiscountCode.trim(),
                                    discountPercent = discountNum / 100.0,
                                    ctaText = announcementCtaText.trim().ifBlank { "Ver Más" },
                                    targetCategory = announcementTargetCategory,
                                    isActive = isAnnouncementActive
                                )

                                onPublishAnnouncement(newAnnouncement)
                                scope.launch {
                                    snackbarHostState.showSnackbar("¡Anuncio '${newAnnouncement.title}' publicado con éxito!")
                                    announcementTitle = ""
                                    announcementMessage = ""
                                    announcementImageUrl = ""
                                    announcementDiscountCode = ""
                                    announcementDiscountPercentText = ""
                                    isSubmittingAnnouncement = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("admin_submit_announcement_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EleganBurgundy,
                                contentColor = Color.White
                            ),
                            enabled = !isSubmittingAnnouncement
                        ) {
                            if (isSubmittingAnnouncement) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Publicar Anuncio Oficial",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
