package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
fun InicioHeroBanner(
    onNavigateToCatalogo: (String) -> Unit,
    onNavigateToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        EleganBurgundyDark,
                        EleganBurgundy,
                        Color(0xFF380710)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val isCompact = maxWidth < 350.dp
        val isExpanded = maxWidth >= 600.dp
        val minBannerHeight = when {
            isExpanded -> 312.dp
            isCompact -> 252.dp
            maxWidth >= 420.dp -> 296.dp
            else -> 280.dp
        }
        val horizontalPad = when {
            isExpanded -> 28.dp
            isCompact -> 16.dp
            else -> 24.dp
        }
        val verticalPad = if (isCompact) 18.dp else 24.dp
        val logoCircleSize = if (isCompact) 40.dp else 44.dp
        val headlineFontSize = when {
            isExpanded -> 31.sp
            isCompact -> 23.sp
            else -> 28.sp
        }
        val headlineLineHeight = when {
            isExpanded -> 36.sp
            isCompact -> 28.sp
            else -> 32.sp
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .heightIn(min = minBannerHeight)
                .padding(horizontal = horizontalPad, vertical = verticalPad),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Cabecera superior dentro del banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(logoCircleSize)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_elegan_gentleman),
                            contentDescription = "Logo Elegan",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ELEGAN",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontSize = if (isCompact) 19.sp else 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = if (isCompact) 3.sp else 4.sp,
                                fontFamily = FontFamily.Serif
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Sartorial & Luxury Menswear",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EleganGold,
                                fontSize = if (isCompact) 10.sp else 11.sp,
                                letterSpacing = if (isCompact) 0.5.sp else 1.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onNavigateToMenu,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .testTag("menu_button_hero")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Menú Categorías",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 22.dp))

            // Texto central y botones de acción
            Column(modifier = Modifier.padding(bottom = if (isCompact) 4.dp else 8.dp)) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EleganGold.copy(alpha = 0.25f),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "NUEVA COLECCIÓN 2026",
                        color = EleganGold,
                        fontSize = if (isCompact) 10.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            horizontal = if (isCompact) 8.dp else 10.dp,
                            vertical = 4.dp
                        ),
                        letterSpacing = if (isCompact) 1.2.sp else 2.sp
                    )
                }

                Text(
                    text = "Distinción &\nCarácter Propio",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontSize = headlineFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = headlineLineHeight,
                        fontFamily = FontFamily.Serif
                    )
                )

                Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)) {
                    Button(
                        onClick = { onNavigateToCatalogo("todo") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EleganGold,
                            contentColor = EleganCharcoal
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(
                            horizontal = if (isCompact) 12.dp else 18.dp,
                            vertical = 10.dp
                        ),
                        modifier = Modifier.testTag("explore_catalog_hero_btn")
                    ) {
                        Text(
                            text = "Ver Catálogo",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompact) 12.sp else 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onNavigateToMenu,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(
                            horizontal = if (isCompact) 12.dp else 18.dp,
                            vertical = 10.dp
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = Brush.horizontalGradient(listOf(Color.White, EleganGold))
                        )
                    ) {
                        Text(
                            text = "Categorías",
                            fontSize = if (isCompact) 12.sp else 13.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
