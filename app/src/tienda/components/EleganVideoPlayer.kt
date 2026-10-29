package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganGold
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Extrae y almacena en caché local la portada (primer fotograma) del video
 * para que sirva de imagen de portada y acompañe el contenido de la galería.
 */
fun extractVideoPosterLocally(context: Context, videoUriStr: String): String? {
    if (videoUriStr.isBlank()) return null
    return try {
        val uri = Uri.parse(videoUriStr)
        val retriever = MediaMetadataRetriever()
        if (videoUriStr.startsWith("http://") || videoUriStr.startsWith("https://")) {
            retriever.setDataSource(videoUriStr, HashMap<String, String>())
        } else {
            retriever.setDataSource(context, uri)
        }
        val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.frameAtTime
        if (frame != null) {
            val mediaDir = File(context.filesDir, "product_media").apply { if (!exists()) mkdirs() }
            val hash = videoUriStr.hashCode().let { if (it < 0) -it else it }
            val thumbFile = File(mediaDir, "thumb_$hash.jpg")
            if (!thumbFile.exists() || thumbFile.length() == 0L) {
                FileOutputStream(thumbFile).use { out ->
                    frame.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
            }
            frame.recycle()
            retriever.release()
            Uri.fromFile(thumbFile).toString()
        } else {
            retriever.release()
            null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Reproductor de video optimizado para la galería de Elegan.
 * Escala el video con Matrix (Center-Crop) para ocupar el 100% del ancho y alto
 * del contenedor sin dejar espacios vacíos, y mantiene la portada visible hasta
 * que el primer fotograma real del video sea dibujado para evitar fondos negros al inicio.
 */
@Composable
fun EleganVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    posterUrl: String? = null,
    aspectRatio: Float = 1f,
    autoPlay: Boolean = false,
    isFocused: Boolean = false,
    showControls: Boolean = true,
    testTagPrefix: String = "elegan_video"
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(autoPlay || isFocused) }
    var isPrepared by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var showOverlayControls by remember { mutableStateOf(true) }
    var isFirstFrameRendered by remember { mutableStateOf(false) }
    var shouldLoadPlayer by remember(videoUrl) { mutableStateOf(false) }

    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }

    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var surfaceRef by remember { mutableStateOf<Surface?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var extractedPosterUrl by remember { mutableStateOf<String?>(null) }

    // Control automático inteligente de reproducción según el foco en pantalla del usuario:
    // Se reproduce automáticamente al entrar en foco, y se pausa de inmediato al salir del foco.
    LaunchedEffect(isFocused) {
        if (isFocused) {
            shouldLoadPlayer = true
            isPlaying = true
            showOverlayControls = false
            mediaPlayerRef?.let { mp ->
                try {
                    if (isPrepared) {
                        if (isCompleted) {
                            mp.seekTo(0)
                            isCompleted = false
                        }
                        if (!mp.isPlaying) {
                            mp.start()
                        }
                    }
                } catch (_: Exception) {}
            }
        } else {
            isPlaying = false
            mediaPlayerRef?.let { mp ->
                try {
                    if (isPrepared && mp.isPlaying) {
                        mp.pause()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Generar la portada a partir del video si posterUrl no viene provisto
    LaunchedEffect(videoUrl, posterUrl, shouldLoadPlayer) {
        isFirstFrameRendered = false
        if (shouldLoadPlayer && posterUrl.isNullOrBlank() && videoUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val thumb = extractVideoPosterLocally(context, videoUrl)
                if (thumb != null) {
                    extractedPosterUrl = thumb
                }
            }
        }
    }

    val effectivePoster = posterUrl?.takeIf { it.isNotBlank() } ?: extractedPosterUrl

    fun adjustVideoTransform(
        textureView: TextureView?,
        vWidth: Int,
        vHeight: Int
    ) {
        if (textureView == null) return
        val viewWidth = textureView.width.toFloat()
        val viewHeight = textureView.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f || vWidth <= 0 || vHeight <= 0) return

        val viewRatio = viewWidth / viewHeight
        val videoRatio = vWidth.toFloat() / vHeight.toFloat()

        val scaleX: Float
        val scaleY: Float

        // Escala completa (Center Crop): ocupa todo el ancho y alto del contenedor
        // sin dejar espacios vacíos en ningún borde.
        if (videoRatio > viewRatio) {
            scaleX = videoRatio / viewRatio
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = viewRatio / videoRatio
        }

        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
        textureView.setTransform(matrix)
    }

    LaunchedEffect(isPlaying, showOverlayControls) {
        if (isPlaying && showOverlayControls) {
            delay(2800)
            showOverlayControls = false
        }
    }

    DisposableEffect(videoUrl, shouldLoadPlayer) {
        if (!shouldLoadPlayer) {
            onDispose {}
        } else {
            isPrepared = false
            isCompleted = false
            hasError = false
            isFirstFrameRendered = false
            videoWidth = 0
            videoHeight = 0

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                isLooping = true
                setOnPreparedListener { player ->
                    isPrepared = true
                    hasError = false
                    val w = player.videoWidth
                    val h = player.videoHeight
                    if (w > 0 && h > 0) {
                        videoWidth = w
                        videoHeight = h
                        adjustVideoTransform(textureViewRef, w, h)
                    }
                    if (isMuted) {
                        player.setVolume(0f, 0f)
                    } else {
                        player.setVolume(1f, 1f)
                    }
                    if (isPlaying || isFocused) {
                        isPlaying = true
                        player.start()
                    }
                }
                setOnVideoSizeChangedListener { _, w, h ->
                    if (w > 0 && h > 0) {
                        videoWidth = w
                        videoHeight = h
                        adjustVideoTransform(textureViewRef, w, h)
                    }
                }
                setOnCompletionListener {
                    isCompleted = true
                    isPlaying = false
                    showOverlayControls = true
                }
                setOnErrorListener { _, _, _ ->
                    hasError = true
                    isPlaying = false
                    true
                }
            }
            mediaPlayerRef = mp

            if (videoUrl.isNotBlank()) {
                try {
                    mp.setDataSource(context, Uri.parse(videoUrl))
                    mp.prepareAsync()
                } catch (_: Exception) {
                    hasError = true
                }
            }

            surfaceRef?.let { surf ->
                try {
                    mp.setSurface(surf)
                } catch (_: Exception) {}
            }

            onDispose {
                try {
                    mp.stop()
                } catch (_: Exception) {}
                try {
                    mp.reset()
                    mp.release()
                } catch (_: Exception) {}
                if (mediaPlayerRef == mp) {
                    mediaPlayerRef = null
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RectangleShape)
            .background(Color(0xFFF0EBE3))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isPlaying) {
                    showOverlayControls = !showOverlayControls
                } else if (isCompleted) {
                    isCompleted = false
                    isPlaying = true
                    showOverlayControls = false
                    try {
                        mediaPlayerRef?.seekTo(0)
                        mediaPlayerRef?.start()
                    } catch (_: Exception) {}
                } else {
                    shouldLoadPlayer = true
                    isPlaying = true
                    showOverlayControls = false
                    try {
                        mediaPlayerRef?.start()
                    } catch (_: Exception) {}
                }
            }
            .testTag("${testTagPrefix}_container"),
        contentAlignment = Alignment.Center
    ) {
        if (shouldLoadPlayer && videoUrl.isNotBlank() && !hasError) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        textureViewRef = this

                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                                surfaceRef?.release()
                                val s = Surface(st)
                                surfaceRef = s
                                try {
                                    mediaPlayerRef?.setSurface(s)
                                    if ((isPlaying || isFocused) && isPrepared) {
                                        mediaPlayerRef?.start()
                                    }
                                } catch (_: Exception) {}
                                adjustVideoTransform(this@apply, videoWidth, videoHeight)
                            }

                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                                adjustVideoTransform(this@apply, videoWidth, videoHeight)
                            }

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                try {
                                    mediaPlayerRef?.pause()
                                    mediaPlayerRef?.setSurface(null)
                                } catch (_: Exception) {}
                                surfaceRef?.release()
                                surfaceRef = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {
                                // En el momento que se dibuja el primer fotograma real del video,
                                // se oculta la portada para evitar cualquier fondo negro
                                if (!isFirstFrameRendered) {
                                    isFirstFrameRendered = true
                                }
                            }
                        }

                        addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                            val w = right - left
                            val h = bottom - top
                            if (w > 0 && h > 0 && (w != oldRight - oldLeft || h != oldBottom - oldTop)) {
                                adjustVideoTransform(this, videoWidth, videoHeight)
                            }
                        }

                        if (isAvailable && surfaceTexture != null) {
                            val s = Surface(surfaceTexture)
                            surfaceRef = s
                            try {
                                mediaPlayerRef?.setSurface(s)
                            } catch (_: Exception) {}
                            adjustVideoTransform(this, videoWidth, videoHeight)
                        }
                    }
                },
                update = { tv ->
                    textureViewRef = tv
                    adjustVideoTransform(tv, videoWidth, videoHeight)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Portada: Permanece visible hasta que el primer fotograma real del video se haya renderizado.
        // Esto garantiza que en NINGÚN momento se vea un fondo negro al inicio.
        if (!isFirstFrameRendered && !hasError && !effectivePoster.isNullOrBlank()) {
            AsyncImage(
                model = effectivePoster,
                contentDescription = "Portada del video",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isPlaying && !isPrepared && !hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = EleganGold,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }


        AnimatedVisibility(
            visible = !isPlaying || showOverlayControls || isCompleted,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (isCompleted) {
                Surface(
                    onClick = {
                        isCompleted = false
                        shouldLoadPlayer = true
                        isPlaying = true
                        showOverlayControls = false
                        try {
                            mediaPlayerRef?.seekTo(0)
                            mediaPlayerRef?.start()
                        } catch (_: Exception) {}
                    },
                    shape = CircleShape,
                    color = EleganBurgundy.copy(alpha = 0.9f),
                    border = BorderStroke(2.dp, EleganGold),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("${testTagPrefix}_replay_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Repetir video",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else if (!isPlaying) {
                Surface(
                    onClick = {
                        shouldLoadPlayer = true
                        isPlaying = true
                        showOverlayControls = false
                        try {
                            mediaPlayerRef?.start()
                        } catch (_: Exception) {}
                    },
                    shape = CircleShape,
                    color = EleganBurgundy.copy(alpha = 0.92f),
                    border = BorderStroke(2.dp, EleganGold),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("${testTagPrefix}_play_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir video",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else if (showOverlayControls) {
                Surface(
                    onClick = {
                        isPlaying = false
                        showOverlayControls = true
                        try {
                            mediaPlayerRef?.pause()
                        } catch (_: Exception) {}
                    },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.7f),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("${testTagPrefix}_pause_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pausar video",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        if (hasError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF0EBE3))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = EleganGold,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = "Video no disponible",
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "El archivo de video local se está procesando.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
