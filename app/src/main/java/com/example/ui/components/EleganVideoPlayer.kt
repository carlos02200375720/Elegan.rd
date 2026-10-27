package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import kotlinx.coroutines.delay

/**
 * Reproductor de video interactivo integrado para Jetpack Compose.
 * Permite reproducir videos directamente dentro de las publicaciones de Inicio
 * y en la galería de Productos, con soporte de pausa, reanudación, silencio y repetición.
 */
@Composable
fun EleganVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    posterUrl: String? = null,
    aspectRatio: Float = 1f,
    autoPlay: Boolean = false,
    showControls: Boolean = true,
    testTagPrefix: String = "elegan_video"
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var isPrepared by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var showOverlayControls by remember { mutableStateOf(true) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    // Ocultar controles superpuestos automáticamente tras 3 segundos de reproducción
    LaunchedEffect(isPlaying, showOverlayControls) {
        if (isPlaying && showOverlayControls) {
            delay(2800)
            showOverlayControls = false
        }
    }

    DisposableEffect(videoUrl) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isPlaying) {
                    showOverlayControls = !showOverlayControls
                } else if (isCompleted) {
                    isCompleted = false
                    videoViewRef?.seekTo(0)
                    videoViewRef?.start()
                    isPlaying = true
                } else {
                    isPlaying = true
                    videoViewRef?.start()
                }
            }
            .testTag("${testTagPrefix}_container"),
        contentAlignment = Alignment.Center
    ) {
        // Reproductor nativo Android VideoView
        if (videoUrl.isNotBlank() && !hasError) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        videoViewRef = this
                        try {
                            setVideoURI(Uri.parse(videoUrl))
                        } catch (e: Exception) {
                            hasError = true
                        }

                        setOnPreparedListener { mp ->
                            mediaPlayerRef = mp
                            isPrepared = true
                            hasError = false
                            mp.isLooping = true
                            if (isMuted) {
                                mp.setVolume(0f, 0f)
                            } else {
                                mp.setVolume(1f, 1f)
                            }
                            if (isPlaying) {
                                start()
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
                },
                update = { vView ->
                    videoViewRef = vView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Portada / Poster mientras el video no ha iniciado o no está preparado
        if (!isPlaying && !isPrepared && posterUrl != null && posterUrl.isNotBlank() && !hasError) {
            AsyncImage(
                model = posterUrl,
                contentDescription = "Portada del video",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Indicador de carga mientras prepara
        if (isPlaying && !isPrepared && !hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = EleganGold,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Etiqueta de "VIDEO OFICIAL" en esquina superior izquierda
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = EleganGold,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "VIDEO",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // Botón de silencio en la esquina superior derecha
        if (isPrepared && showControls) {
            IconButton(
                onClick = {
                    val nextMute = !isMuted
                    isMuted = nextMute
                    mediaPlayerRef?.let { mp ->
                        if (nextMute) mp.setVolume(0f, 0f) else mp.setVolume(1f, 1f)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .testTag("${testTagPrefix}_mute_btn")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Activar sonido" else "Silenciar",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Controles superpuestos (Play, Pausa, Repetir)
        AnimatedVisibility(
            visible = !isPlaying || showOverlayControls || isCompleted,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (isCompleted) {
                // Botón Replay
                Surface(
                    onClick = {
                        isCompleted = false
                        videoViewRef?.seekTo(0)
                        videoViewRef?.start()
                        isPlaying = true
                        showOverlayControls = false
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
                // Botón Play elegante
                Surface(
                    onClick = {
                        isPlaying = true
                        videoViewRef?.start()
                        showOverlayControls = false
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
                // Botón Pausa
                Surface(
                    onClick = {
                        videoViewRef?.pause()
                        isPlaying = false
                        showOverlayControls = true
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

        // Mensaje de error si la reproducción falló
        if (hasError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "El archivo de video local se está procesando.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
