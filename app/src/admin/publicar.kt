package com.example.admin

import androidx.activity.compose.BackHandler
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.PhotoLibrary

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
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
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
import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory
import kotlinx.coroutines.launch

private fun persistUriLocallyInApp(context: Context, sourceUri: Uri, prefix: String, isVideo: Boolean): String {
    return try {
        if (sourceUri.scheme == "file") {
            val f = File(sourceUri.path ?: "")
            if (f.exists() && f.length() > 0) return sourceUri.toString()
        }
        val mediaDir = File(context.filesDir, "product_media").apply { if (!exists()) mkdirs() }
        val ext = if (isVideo) "mp4" else "jpg"
        val targetFile = File(mediaDir, "${prefix}_${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        if (targetFile.exists() && targetFile.length() > 0) {
            Uri.fromFile(targetFile).toString()
        } else {
            sourceUri.toString()
        }
    } catch (_: Exception) {
        sourceUri.toString()
    }
}

private fun extractThumbnailLocally(context: Context, videoUriStr: String): String? {
    return try {
        val uri = Uri.parse(videoUriStr)
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: retriever.frameAtTime
        if (frame != null) {
            val mediaDir = File(context.filesDir, "product_media").apply { if (!exists()) mkdirs() }
            val thumbFile = File(mediaDir, "thumb_${System.currentTimeMillis()}.jpg")
            FileOutputStream(thumbFile).use { out ->
                frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            frame.recycle()
            retriever.release()
            if (thumbFile.exists() && thumbFile.length() > 0) {
                Uri.fromFile(thumbFile).toString()
            } else null
        } else {
            retriever.release()
            null
        }
    } catch (_: Exception) {
        null
    }
}

private fun getProductVariantSuggestions(category: String): List<String> = when (category) {
    "camisa", "interiore" -> listOf("S", "M", "L", "XL", "XXL")
    "pantalone" -> listOf("30 x 30", "32 x 30", "34 x 30", "36 x 30")
    "perfume" -> listOf("30 ml", "50 ml", "100 ml")
    "zapato" -> listOf("40", "41", "42", "43", "44", "45")
    "gorra" -> listOf("Única (Estándar)", "S/M", "L/XL")
    "gafa", "correa" -> listOf("Única (Estándar)")
    else -> listOf("Única (Estándar)")
}

@Composable
fun PublicarScreen(
    onPublishProduct: (Product, (Result<Unit>) -> Unit) -> Unit = { _, onResult ->
        onResult(Result.failure(IllegalStateException("No se configuró la publicación de productos.")))
    },
    onPublishAnnouncement: (Announcement) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
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

    // Lanzadores para abrir la galería de fotos del dispositivo
    val mainPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val persisted = persistUriLocallyInApp(context, uri, "main_photo", isVideo = false)
            productMainImageUrl = persisted
        }
    }

    val additionalPhotosPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val persisted = persistUriLocallyInApp(context, uri, "gallery_photo", isVideo = false)
            if (persisted !in productGalleryImages && (productGalleryImages.size + if (productMainImageUrl.isNotBlank()) 1 else 0) < 10) {
                productGalleryImages.add(persisted)
            }
        }
    }

    var productMediaType by remember { mutableStateOf("foto") } // "foto" o "video"
    var productVideoUrl by remember { mutableStateOf("") }

    val productVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val persisted = persistUriLocallyInApp(context, uri, "prod_video", isVideo = true)
            productVideoUrl = persisted
            val thumb = extractThumbnailLocally(context, persisted)
            if (thumb != null) {
                if (productMainImageUrl.isBlank()) {
                    productMainImageUrl = thumb
                }
                if (thumb !in productGalleryImages) {
                    productGalleryImages.add(0, thumb)
                }
            }
        }
    }

    val availableSizeOptions = getProductVariantSuggestions(productCategory)
    val selectedSizes = remember { mutableStateListOf("S", "M", "L", "XL") }
    val selectedColors = remember { mutableStateListOf<String>() }
    var customVariant by remember { mutableStateOf("") }
    var customColor by remember { mutableStateOf("") }
    var isProductFeatured by remember { mutableStateOf(true) }
    var isProductInStock by remember { mutableStateOf(true) }
    var isSubmittingProduct by remember { mutableStateOf(false) }

    // -------------------------------------------------------------------------
    // ESTADO: PUBLICAR ANUNCIO
    // -------------------------------------------------------------------------
    var announcementTitle by remember { mutableStateOf("") }
    var announcementMessage by remember { mutableStateOf("") }
    var announcementType by remember { mutableStateOf("banner") } // banner, historia_9_16, descuento, coleccion
    var announcementMediaType by remember { mutableStateOf("foto") } // "foto" o "video"
    var announcementImageUrl by remember { mutableStateOf("") }
    var announcementVideoUrl by remember { mutableStateOf("") }

    // Lanzador para la imagen del anuncio
    val announcementPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val persisted = persistUriLocallyInApp(context, uri, "ann_photo", isVideo = false)
            announcementImageUrl = persisted
        }
    }

    // Lanzador para el video del anuncio desde la galería
    val announcementVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val persisted = persistUriLocallyInApp(context, uri, "ann_video", isVideo = true)
            announcementVideoUrl = persisted
            if (announcementImageUrl.isBlank()) {
                val thumb = extractThumbnailLocally(context, persisted)
                if (thumb != null) {
                    announcementImageUrl = thumb
                } else {
                    announcementImageUrl = persisted
                }
            }
        }
    }
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
                                            onClick = {
                                                productCategory = cat.id
                                                selectedSizes.clear()
                                                selectedSizes.addAll(getProductVariantSuggestions(cat.id))
                                            },
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
                                    text = "Tipo de Multimedia:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("foto" to "📸 Fotos / Galería", "video" to "🎥 Video del Producto").forEach { (typeCode, typeLabel) ->
                                        val isSel = productMediaType == typeCode
                                        OutlinedButton(
                                            onClick = {
                                                productMediaType = typeCode
                                                if (typeCode == "video" && productMediaFormat !in listOf("9:16", "1:1", "16:9")) {
                                                    productMediaFormat = "9:16"
                                                } else if (typeCode == "foto" && productMediaFormat !in listOf("1:1", "3:4", "4:3")) {
                                                    productMediaFormat = "1:1"
                                                }
                                            },
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
                                            Text(typeLabel, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (productMediaType == "video") {
                                    Text(
                                        text = "Formato del Video:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("9:16" to "Reel (9:16)", "1:1" to "Cuadrado (1:1)", "16:9" to "Paisaje (16:9)").forEach { (formatCode, label) ->
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

                                    Text(
                                        text = "Video del Producto desde Galería:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (productVideoUrl.isBlank()) {
                                        Surface(
                                            onClick = {
                                                productVideoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_product_video_picker"),
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFF9F6F0),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                androidx.compose.ui.graphics.SolidColor(Color(0xFFD6CEC5))
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .background(EleganBurgundy.copy(alpha = 0.12f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Videocam,
                                                        contentDescription = "Abrir galería de videos",
                                                        tint = EleganBurgundy,
                                                        modifier = Modifier.size(30.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "Toca para abrir la galería de videos",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = EleganCharcoal
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Selecciona un video (Reel, demo, MP4) de tu producto",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(
                                                    onClick = {
                                                        productVideoPickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Abrir Galería de Videos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F24))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(46.dp)
                                                            .background(EleganBurgundy.copy(alpha = 0.3f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayCircle,
                                                            contentDescription = "Video",
                                                            tint = EleganGold,
                                                            modifier = Modifier.size(30.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Surface(
                                                            color = EleganGold.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "VIDEO CARGADO • FORMATO $productMediaFormat",
                                                                color = EleganGold,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "Video listo para publicar",
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = productVideoUrl.takeLast(35),
                                                            color = Color.LightGray,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            productVideoPickerLauncher.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                            )
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                                    ) {
                                                        Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Cambiar Video", fontSize = 12.sp)
                                                    }
                                                    OutlinedButton(
                                                        onClick = { productVideoUrl = "" },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Quitar video", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Foto de Portada / Miniatura (Opcional):",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (productMainImageUrl.isNotBlank() && productMainImageUrl != productVideoUrl) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = productMainImageUrl,
                                                contentDescription = "Portada",
                                                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Portada seleccionada", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text("Se mostrará como miniatura del video", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            IconButton(onClick = { productMainImageUrl = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                mainPhotoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Elegir foto de portada desde galería", fontSize = 12.sp)
                                        }
                                    }
                                } else {
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

                                                                Text(
                                    text = "Imagen Principal del Producto:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Campo interactivo para abrir la galería al hacer clic
                                if (productMainImageUrl.isBlank()) {
                                    Surface(
                                        onClick = {
                                            mainPhotoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("admin_product_main_photo_picker"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF9F6F0),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.5.dp,
                                            androidx.compose.ui.graphics.SolidColor(Color(0xFFD6CEC5))
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 22.dp, horizontal = 16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .background(EleganBurgundy.copy(alpha = 0.12f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddPhotoAlternate,
                                                    contentDescription = "Abrir galería",
                                                    tint = EleganBurgundy,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Toca para abrir la galería de fotos",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = EleganCharcoal
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Selecciona la foto principal desde tu dispositivo",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    mainPhotoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Abrir Galería", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    // Vista previa de la imagen principal seleccionada desde la galería
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFFF0EBE3))
                                                .clickable {
                                                    mainPhotoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = productMainImageUrl,
                                                contentDescription = "Foto principal del producto",
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
                                            Surface(
                                                color = EleganBurgundy.copy(alpha = 0.9f),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Foto Principal", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    mainPhotoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EleganBurgundy)
                                            ) {
                                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Cambiar Foto de Galería", fontSize = 12.sp)
                                            }
                                            OutlinedButton(
                                                onClick = { productMainImageUrl = "" },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Quitar foto", tint = Color.Red, modifier = Modifier.size(16.dp))
                                            }
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

                                // Campo interactivo para abrir la galería del dispositivo para fotos adicionales
                                Surface(
                                    onClick = {
                                        additionalPhotosPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_product_additional_photos_picker"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF9F6F0),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        androidx.compose.ui.graphics.SolidColor(Color(0xFFD6CEC5))
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Collections,
                                                contentDescription = "Galería",
                                                tint = EleganBurgundy,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Toca para abrir la galería de fotos",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = EleganCharcoal
                                                )
                                                Text(
                                                    text = "Elige fotos adicionales de tu dispositivo",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                additionalPhotosPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Añadir", fontSize = 12.sp)
                                        }
                                    }
                                }

                                if (productGalleryImages.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(1.dp, EleganBurgundy.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .background(EleganBurgundy.copy(alpha = 0.05f))
                                                    .clickable {
                                                        additionalPhotosPickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.Add, contentDescription = "Añadir más", tint = EleganBurgundy, modifier = Modifier.size(22.dp))
                                                    Text("Más", fontSize = 10.sp, color = EleganBurgundy, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        items(productGalleryImages) { imgUriStr ->
                                            Box(
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFEFE8E1))
                                            ) {
                                                AsyncImage(
                                                    model = imgUriStr,
                                                    contentDescription = "Foto adicional de galería",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                IconButton(
                                                    onClick = { productGalleryImages.remove(imgUriStr) },
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .align(Alignment.TopEnd)
                                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Quitar foto",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(13.dp)
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

                    // Tallas y Opciones
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "4. Variantes, Colores y Disponibilidad",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = when (productCategory) {
                                        "pantalone" -> "Tallas / medidas disponibles (ej. 34 x 30):"
                                        "perfume" -> "Presentaciones disponibles (ej. 50 ml):"
                                        else -> "Tallas o variantes disponibles:"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(availableSizeOptions) { sz ->
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

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customVariant,
                                        onValueChange = { customVariant = it },
                                        label = { Text("Agregar talla o medida") },
                                        placeholder = {
                                            Text(
                                                when (productCategory) {
                                                    "pantalone" -> "Ej. 34 x 30"
                                                    "perfume" -> "Ej. 75 ml"
                                                    else -> "Ej. XXL, Única"
                                                }
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_product_variant_input"),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions.Default,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EleganBurgundy,
                                            focusedLabelColor = EleganBurgundy
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            val variant = customVariant.trim()
                                            if (variant.isNotEmpty() && selectedSizes.none { it.equals(variant, true) }) {
                                                selectedSizes.add(variant)
                                                customVariant = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(EleganBurgundy)
                                            .testTag("admin_add_product_variant")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Agregar variante", tint = Color.White)
                                    }
                                }
                                if (selectedSizes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(selectedSizes.toList()) { variant ->
                                            FilterChip(
                                                selected = true,
                                                onClick = { selectedSizes.remove(variant) },
                                                label = { Text("$variant  ×", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = EleganBurgundy,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color(0xFFEEE7DF))
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Colores disponibles:",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(listOf("Negro", "Blanco", "Azul", "Gris", "Marrón", "Rojo")) { color ->
                                        val isSelected = color in selectedColors
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                if (isSelected) selectedColors.remove(color)
                                                else selectedColors.add(color)
                                            },
                                            label = { Text(color, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = EleganBurgundy,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = customColor,
                                        onValueChange = { customColor = it },
                                        label = { Text("Agregar otro color") },
                                        placeholder = { Text("Ej. Verde oliva") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("admin_product_color_input"),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = EleganBurgundy,
                                            focusedLabelColor = EleganBurgundy
                                        )
                                    )
                                    IconButton(
                                        onClick = {
                                            val color = customColor.trim()
                                            if (color.isNotEmpty() && selectedColors.none { it.equals(color, true) }) {
                                                selectedColors.add(color)
                                                customColor = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(EleganBurgundy)
                                            .testTag("admin_add_product_color")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Agregar color", tint = Color.White)
                                    }
                                }
                                if (selectedColors.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(selectedColors.toList()) { color ->
                                            FilterChip(
                                                selected = true,
                                                onClick = { selectedColors.remove(color) },
                                                label = { Text("$color  ×", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = EleganGold,
                                                    selectedLabelColor = EleganCharcoal
                                                )
                                            )
                                        }
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
                                if (productMediaType == "video") {
                                    if (productVideoUrl.isBlank()) {
                                        scope.launch { snackbarHostState.showSnackbar("Por favor selecciona el video del producto desde la galería.") }
                                        return@Button
                                    }
                                } else {
                                    if (productMainImageUrl.isBlank()) {
                                        scope.launch { snackbarHostState.showSnackbar("Por favor selecciona la imagen principal desde la galería de fotos.") }
                                        return@Button
                                    }
                                }

                                isSubmittingProduct = true
                                val effectiveImageUrl = productMainImageUrl.trim().ifBlank {
                                    if (productMediaType == "video") {
                                        extractThumbnailLocally(context, productVideoUrl) ?: when (productCategory.lowercase().trim()) {
                                            "gorra" -> "https://images.unsplash.com/photo-1588850561407-ed78c282e89b?w=800&auto=format&fit=crop&q=80"
                                            "gafa" -> "https://images.unsplash.com/photo-1508296695146-257a814070b4?w=800&auto=format&fit=crop&q=80"
                                            "camisa" -> "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800&auto=format&fit=crop&q=80"
                                            "correa" -> "https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=800&auto=format&fit=crop&q=80"
                                            "pantalone" -> "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800&auto=format&fit=crop&q=80"
                                            "zapato" -> "https://images.unsplash.com/photo-1614252235316-8c857d38b5f4?w=800&auto=format&fit=crop&q=80"
                                            "interiore" -> "https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?w=800&auto=format&fit=crop&q=80"
                                            "perfume" -> "https://images.unsplash.com/photo-1523293182086-7651a899d37f?w=800&auto=format&fit=crop&q=80"
                                            else -> "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800&auto=format&fit=crop&q=80"
                                        }
                                    } else {
                                        ""
                                    }
                                }
                                val newProduct = Product(
                                    name = productName.trim(),
                                    description = productDescription.trim(),
                                    category = productCategory,
                                    price = priceValue,
                                    imageUrl = effectiveImageUrl,
                                    images = (listOf(effectiveImageUrl).filter { it.isNotBlank() } + productGalleryImages).distinct(),
                                    sizes = if (selectedSizes.isNotEmpty()) selectedSizes.toList() else listOf("Estándar"),
                                    colors = selectedColors.toList(),
                                    inStock = isProductInStock,
                                    isFeatured = isProductFeatured,
                                    mediaFormat = productMediaFormat,
                                    videoUrl = if (productMediaType == "video") productVideoUrl.trim() else "",
                                    isVideo = (productMediaType == "video")
                                )

                                onPublishProduct(newProduct) { result ->
                                    scope.launch {
                                        result.onSuccess {
                                            snackbarHostState.showSnackbar("¡Producto '${newProduct.name}' publicado con éxito!")
                                            productName = ""
                                            productDescription = ""
                                            productPriceText = ""
                                            productMainImageUrl = ""
                                            productVideoUrl = ""
                                            productGalleryImages.clear()
                                            selectedSizes.clear()
                                            selectedSizes.addAll(availableSizeOptions)
                                            selectedColors.clear()
                                            customVariant = ""
                                            customColor = ""
                                        }.onFailure { error ->
                                            snackbarHostState.showSnackbar(
                                                "Error al publicar: ${error.localizedMessage ?: "Verifica la conexión a internet e inténtalo de nuevo."}"
                                            )
                                        }
                                        isSubmittingProduct = false
                                    }
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

                    // Contenido Multimedia Promocional
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "3. Contenido Multimedia del Anuncio",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                // Selector: Imagen vs Video
                                Text(
                                    text = "Tipo de Multimedia:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("image" to "📸 Imagen / Banner", "video" to "🎥 Video Promocional / Spot").forEach { (typeKey, typeLabel) ->
                                        val isSel = announcementMediaType == typeKey
                                        OutlinedButton(
                                            onClick = { announcementMediaType = typeKey },
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
                                            Text(typeLabel, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (announcementMediaType == "video") {
                                    Text(
                                        text = "Video Promocional desde Galería:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (announcementVideoUrl.isBlank()) {
                                        Surface(
                                            onClick = {
                                                announcementVideoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_announcement_video_picker"),
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFF9F6F0),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                androidx.compose.ui.graphics.SolidColor(Color(0xFFD6CEC5))
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(52.dp)
                                                        .background(EleganBurgundy.copy(alpha = 0.12f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Videocam,
                                                        contentDescription = "Abrir galería de videos",
                                                        tint = EleganBurgundy,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "Toca para abrir la galería de videos",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = EleganCharcoal
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Selecciona un video publicitario (Spot, Story, Reel MP4)",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(
                                                    onClick = {
                                                        announcementVideoPickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Abrir Galería de Videos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F24))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(46.dp)
                                                            .background(EleganBurgundy.copy(alpha = 0.3f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayCircle,
                                                            contentDescription = "Video",
                                                            tint = EleganGold,
                                                            modifier = Modifier.size(30.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Surface(
                                                            color = EleganGold.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "VIDEO SPOT CARGADO",
                                                                color = EleganGold,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "Spot publicitario listo para publicar",
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = announcementVideoUrl.takeLast(35),
                                                            color = Color.LightGray,
                                                            fontSize = 10.sp
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            announcementVideoPickerLauncher.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                            )
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                                    ) {
                                                        Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Cambiar Video", fontSize = 12.sp)
                                                    }
                                                    OutlinedButton(
                                                        onClick = { announcementVideoUrl = "" },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Quitar video", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Foto de Portada / Banner (Opcional):",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (announcementImageUrl.isNotBlank() && announcementImageUrl != announcementVideoUrl) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = announcementImageUrl,
                                                contentDescription = "Portada Anuncio",
                                                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Portada seleccionada", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text("Se mostrará como miniatura del video spot", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            IconButton(onClick = { announcementImageUrl = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                announcementPhotoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Elegir imagen de portada desde galería", fontSize = 12.sp)
                                        }
                                    }
                                } else {
                                    // Imagen de Galería
                                    if (announcementImageUrl.isBlank()) {
                                        Surface(
                                            onClick = {
                                                announcementPhotoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_announcement_photo_picker"),
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFF9F6F0),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                androidx.compose.ui.graphics.SolidColor(Color(0xFFD6CEC5))
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 22.dp, horizontal = 16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(EleganBurgundy.copy(alpha = 0.12f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AddPhotoAlternate,
                                                        contentDescription = "Abrir galería",
                                                        tint = EleganBurgundy,
                                                        modifier = Modifier.size(26.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "Toca para abrir la galería de fotos",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = EleganCharcoal
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Selecciona la imagen promocional desde tu dispositivo",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(
                                                    onClick = {
                                                        announcementPhotoPickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Abrir Galería", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    } else {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFF0EBE3))
                                                    .clickable {
                                                        announcementPhotoPickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AsyncImage(
                                                    model = announcementImageUrl,
                                                    contentDescription = "Vista previa del anuncio",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Surface(
                                                    color = EleganBurgundy.copy(alpha = 0.85f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(8.dp)
                                                    ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Imagen Seleccionada", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        announcementPhotoPickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EleganBurgundy)
                                                ) {
                                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Cambiar Foto de Galería", fontSize = 12.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { announcementImageUrl = "" },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Quitar foto", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
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
                                if (announcementMediaType == "video") {
                                    if (announcementVideoUrl.isBlank()) {
                                        scope.launch { snackbarHostState.showSnackbar("Por favor selecciona el video del anuncio desde la galería.") }
                                        return@Button
                                    }
                                } else {
                                    if (announcementImageUrl.isBlank()) {
                                        scope.launch { snackbarHostState.showSnackbar("Por favor selecciona la imagen del anuncio desde la galería.") }
                                        return@Button
                                    }
                                }
                                val discountNum = announcementDiscountPercentText.toDoubleOrNull() ?: 0.0

                                isSubmittingAnnouncement = true
                                val effectiveAnnouncementImage = if (announcementImageUrl.isNotBlank()) {
                                    announcementImageUrl.trim()
                                } else if (announcementMediaType == "video") {
                                    announcementVideoUrl.trim()
                                } else {
                                    ""
                                }
                                val newAnnouncement = Announcement(
                                    title = announcementTitle.trim(),
                                    message = announcementMessage.trim(),
                                    type = announcementType,
                                    imageUrl = effectiveAnnouncementImage,
                                    discountCode = announcementDiscountCode.trim(),
                                    discountPercent = discountNum / 100.0,
                                    ctaText = announcementCtaText.trim().ifBlank { "Ver Más" },
                                    targetCategory = announcementTargetCategory,
                                    isActive = isAnnouncementActive,
                                    videoUrl = if (announcementMediaType == "video") announcementVideoUrl.trim() else "",
                                    isVideo = (announcementMediaType == "video")
                                )

                                onPublishAnnouncement(newAnnouncement)
                                scope.launch {
                                    snackbarHostState.showSnackbar("¡Anuncio '${newAnnouncement.title}' publicado con éxito!")
                                    announcementTitle = ""
                                    announcementMessage = ""
                                    announcementImageUrl = ""
                                    announcementVideoUrl = ""
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
