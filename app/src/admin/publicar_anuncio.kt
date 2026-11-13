package com.example.admin

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Announcement
import com.example.ui.EleganTopBarShadow
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory
import kotlinx.coroutines.launch

@Composable
fun PublicarAnuncioScreen(
    onPublishAnnouncement: (Announcement) -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var announcementTitle by remember { mutableStateOf("") }
    var announcementMessage by remember { mutableStateOf("") }
    var announcementType by remember { mutableStateOf("banner") } // banner, historia_9_16, descuento, coleccion
    var announcementMediaType by remember { mutableStateOf("image") } // "image" o "video"
    var announcementImageUrl by remember { mutableStateOf("") }
    var announcementVideoUrl by remember { mutableStateOf("") }

    val announcementPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            announcementImageUrl = uri.toString()
        }
    }

    val announcementVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            announcementVideoUrl = uri.toString()
            if (announcementImageUrl.isBlank()) {
                announcementImageUrl = uri.toString()
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("admin_publicar_anuncio_back_btn")
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
                                text = "Publicar Anuncio",
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
                EleganTopBarShadow()
            }
        },
        containerColor = EleganIvory
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EleganBurgundy,
                                focusedLabelColor = EleganBurgundy
                            )
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
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                        brush = SolidColor(
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
                                    border = BorderStroke(1.5.dp, SolidColor(Color(0xFFD6CEC5)))
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
                                    border = BorderStroke(1.5.dp, SolidColor(Color(0xFFD6CEC5)))
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
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EleganBurgundy,
                                    focusedLabelColor = EleganBurgundy
                                )
                            )

                            OutlinedTextField(
                                value = announcementDiscountPercentText,
                                onValueChange = { announcementDiscountPercentText = it },
                                label = { Text("% Descuento") },
                                placeholder = { Text("20") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EleganBurgundy,
                                    focusedLabelColor = EleganBurgundy
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = announcementCtaText,
                            onValueChange = { announcementCtaText = it },
                            label = { Text("Texto del Botón de Acción") },
                            placeholder = { Text("Explorar Colección") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EleganBurgundy,
                                focusedLabelColor = EleganBurgundy
                            )
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
