package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.PublicationComment
import com.example.ui.components.EleganNotificationHost
import com.example.ui.components.EleganNotificationState
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicationCommentsBottomSheet(
    product: Product,
    displayName: String,
    comments: List<PublicationComment>,
    onDismiss: () -> Unit,
    onSubmitComment: (String, String, String, (Result<Unit>) -> Unit) -> Unit
) {
    val commentsNotifier = remember { EleganNotificationState() }
    var newCommentText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    // skipPartiallyExpanded = true garantiza que abra completamente y no a medias
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val submitCurrentComment = {
        val trimmed = newCommentText.trim()
        if (trimmed.isNotEmpty() && !isSubmitting) {
            isSubmitting = true
            newCommentText = ""
            onSubmitComment(product.id, displayName, trimmed) { result ->
                isSubmitting = false
                if (result.isSuccess) {
                    commentsNotifier.show("Comentario publicado")
                } else {
                    commentsNotifier.show("No se pudo publicar el comentario")
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD5CFC7))
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp)
            ) {
            // Cabecera compacta del panel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Comentarios de Clientes",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal,
                            fontFamily = FontFamily.Serif
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = EleganBurgundy,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_comments_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFEEE8E0))
            Spacer(modifier = Modifier.height(6.dp))

            // Lista de comentarios con scroll completo
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(comments, key = { it.id }) { comment ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = comment.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = comment.text,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF333333),
                                lineHeight = 18.sp
                            )
                        )
                    }
                    HorizontalDivider(color = Color(0xFFF5F0EA), thickness = 0.75.dp)
                }
                if (comments.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Aún no hay comentarios.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color.DarkGray
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sé el primero en compartir tu opinión sobre esta prenda.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barra inferior de envío compacta y adaptada al teclado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color(0xFFF9F7F5))
                        .border(1.dp, Color(0xFFD6CDC4), RoundedCornerShape(21.dp))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comment_input_field"),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = EleganCharcoal
                        ),
                        cursorBrush = SolidColor(EleganBurgundy),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = { submitCurrentComment() }
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (newCommentText.isEmpty()) {
                                    Text(
                                        text = "Escribe un comentario...",
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
                IconButton(
                    onClick = { submitCurrentComment() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(EleganBurgundy)
                        .testTag("submit_comment_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Publicar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            }

            EleganNotificationHost(
                state = commentsNotifier,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp)
            )
        }
    }
}
