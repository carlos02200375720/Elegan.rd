package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.SupportMessage
import com.example.ui.components.EleganVideoPlayer
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory

val supportQuestionsSuggestions = listOf(
    "¿Cuánto tarda el envío?",
    "¿Cómo elijo mi talla de camisa o blazer?",
    "¿Tienen algún cupón de descuento activo?",
    "¿Cómo gestiono una devolución o cambio?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    messages: List<SupportMessage>,
    cartItemCount: Long = 0L,
    onNavigateToCart: () -> Unit = {},
    onSendMessage: (text: String, mediaUri: String, mediaType: String) -> Unit,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }
    var pendingMediaType by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            pendingMediaUri = uri
            pendingMediaType = if (mime.startsWith("video/")) "video" else "image"
        }
    }

    LaunchedEffect(messages.size, isKeyboardOpen) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 4.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chats_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = EleganCharcoal
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_elegan_gentleman),
                            contentDescription = "Logo Elegan",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Elegan Concierge VIP",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal,
                                fontFamily = FontFamily.Serif,
                                fontSize = 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Asesoría en línea 24/7",
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Icono del carrito en la esquina superior derecha de la cabecera
                    IconButton(
                        onClick = onNavigateToCart,
                        modifier = Modifier.testTag("chats_cart_button")
                    ) {
                        if (cartItemCount > 0) {
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
                                    tint = EleganCharcoal,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Carrito",
                                tint = EleganCharcoal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
                EleganTopBarShadow()
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                EleganBottomBarShadow()
                Surface(
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 760.dp)
                                .then(
                                    if (!isKeyboardOpen) {
                                        Modifier.navigationBarsPadding()
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                        if (pendingMediaUri != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF3EFEA))
                                        .border(1.dp, Color(0xFFD6CDC4), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = pendingMediaUri,
                                        contentDescription = "Archivo adjunto",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (pendingMediaType == "video") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.35f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Videocam,
                                                contentDescription = "Video",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (pendingMediaType == "video") "Video listo para enviar" else "Imagen lista para enviar",
                                    fontSize = 12.sp,
                                    color = EleganCharcoal,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        pendingMediaUri = null
                                        pendingMediaType = ""
                                    },
                                    modifier = Modifier.testTag("chat_remove_attachment_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar archivo",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF0EAE3))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isKeyboardOpen) {
                                        Modifier
                                            .height(54.dp)
                                            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
                                    } else {
                                        Modifier
                                            .height(62.dp)
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(21.dp))
                                    .background(Color(0xFFF9F7F5))
                                    .border(1.dp, Color(0xFFD6CDC4), RoundedCornerShape(21.dp))
                                    .padding(start = 4.dp, end = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        mediaPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("chat_attach_media_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Adjuntar imagen o video de la galería",
                                        tint = EleganBurgundy,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_input_field"),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        color = EleganCharcoal
                                    ),
                                    cursorBrush = SolidColor(EleganBurgundy),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (inputText.isEmpty()) {
                                                Text(
                                                    text = "Escribe tu consulta aquí...",
                                                    fontSize = 13.sp,
                                                    color = Color.Gray,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(EleganBurgundy)
                                    .clickable {
                                        val media = pendingMediaUri?.toString().orEmpty()
                                        if (inputText.isNotBlank() || media.isNotBlank()) {
                                            onSendMessage(inputText, media, pendingMediaType)
                                            inputText = ""
                                            pendingMediaUri = null
                                            pendingMediaType = ""
                                        }
                                    }
                                    .testTag("chat_send_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        }
                    }
                }
            }
        },
        containerColor = EleganIvory
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
                isCompact -> 10.dp
                isWide -> 24.dp
                else -> 16.dp
            }
            val maxBubbleWidth = (maxWidth * if (isWide) 0.68f else 0.78f).coerceIn(220.dp, 460.dp)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .testTag("chats_screen"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.sender == "user" || msg.sender == "client"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isUser) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCompact) 30.dp else 32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E323A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = EleganGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 8.dp))
                        }

                        Column(
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                            modifier = Modifier.widthIn(max = maxBubbleWidth)
                        ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 14.dp,
                                bottomStart = if (isUser) 14.dp else 2.dp,
                                bottomEnd = if (isUser) 2.dp else 14.dp
                            ),
                            color = if (isUser) EleganBurgundy else Color.White,
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = if (msg.mediaUrl.isNotBlank() && msg.text.isBlank()) 6.dp else 12.dp,
                                    vertical = if (msg.mediaUrl.isNotBlank() && msg.text.isBlank()) 6.dp else 10.dp
                                )
                            ) {
                                if (!isUser) {
                                    Text(
                                        text = msg.senderName.ifBlank { "Concierge Elegan" },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EleganGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }

                                if (msg.mediaUrl.isNotBlank()) {
                                    if (msg.mediaType == "video") {
                                        EleganVideoPlayer(
                                            videoUrl = msg.mediaUrl,
                                            aspectRatio = 1.25f,
                                            autoPlay = false,
                                            isFocused = false,
                                            showControls = true,
                                            testTagPrefix = "chat_video_${msg.id}",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        AsyncImage(
                                            model = msg.mediaUrl,
                                            contentDescription = "Imagen enviada",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 220.dp)
                                                .aspectRatio(1.25f)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    }
                                    if (msg.text.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }

                                if (msg.text.isNotBlank()) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isUser) Color.White else EleganCharcoal,
                                            lineHeight = 20.sp
                                        )
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
}
