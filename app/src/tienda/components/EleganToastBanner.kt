package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Stable
class EleganNotificationState {
    var currentMessage by mutableStateOf<String?>(null)
        private set

    var triggerId by mutableLongStateOf(0L)
        private set

    fun show(message: String) {
        currentMessage = message
        triggerId += 1L
    }

    fun dismiss() {
        currentMessage = null
    }
}

val LocalEleganNotifier = compositionLocalOf<EleganNotificationState?> { null }

@Composable
fun EleganNotificationHost(
    state: EleganNotificationState,
    modifier: Modifier = Modifier
) {
    val activeMessage = state.currentMessage
    val activeTriggerId = state.triggerId

    LaunchedEffect(activeTriggerId, activeMessage) {
        if (activeMessage != null) {
            delay(2400L)
            state.dismiss()
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val isCompact = maxWidth < 350.dp
        val outerHorizontalPad = if (isCompact) 14.dp else 20.dp
        val iconContainerSize = if (isCompact) 26.dp else 28.dp
        val messageFontSize = if (isCompact) 12.5.sp else 13.5.sp

        AnimatedVisibility(
            visible = activeMessage != null,
            enter = fadeIn(animationSpec = tween(180)) +
                slideInVertically(animationSpec = tween(200)) { it / 2 },
            exit = fadeOut(animationSpec = tween(180)) +
                slideOutVertically(animationSpec = tween(180)) { it / 2 }
        ) {
            val message = activeMessage.orEmpty()
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF141414),
                shadowElevation = 10.dp,
                border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier
                    .padding(horizontal = outerHorizontalPad)
                    .widthIn(min = 160.dp, max = 420.dp)
                    .testTag("elegan_notification_pill")
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = if (isCompact) 7.dp else 8.dp,
                        end = if (isCompact) 14.dp else 16.dp,
                        top = 7.dp,
                        bottom = 7.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconContainerSize)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_elegan_gentleman),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 10.dp))

                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = messageFontSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
