package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
fun ConciergeVipCard(
    onNavigateToChats: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isCompact = maxWidth < 350.dp
        val outerPad = if (isCompact) 14.dp else 20.dp
        val innerPad = if (isCompact) 14.dp else 20.dp
        val iconCircleSize = if (isCompact) 42.dp else 50.dp

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = outerPad, vertical = if (isCompact) 12.dp else 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onNavigateToChats() }
                .testTag("concierge_card_home"),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A2E35)
            )
        ) {
            Row(
                modifier = Modifier.padding(innerPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(iconCircleSize)
                        .clip(CircleShape)
                        .background(EleganGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = EleganGold,
                        modifier = Modifier.size(if (isCompact) 24.dp else 28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(if (isCompact) 12.dp else 16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Elegan Concierge VIP",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = if (isCompact) 15.sp else 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "¿Dudas con tu talla o estilo? Chatea en directo con un asesor personal.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            color = Color(0xFFCCCCCC)
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = EleganGold
                )
            }
        }
    }
}

@Composable
fun ValuePropRow(
    icon: ImageVector,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF9F3EA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EleganBurgundy,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal
                )
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Gray
                )
            )
        }
    }
}

@Composable
fun InicioValuePropsSection(
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val horizontalPad = if (maxWidth < 350.dp) 14.dp else 20.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPad, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ValuePropRow(
                icon = Icons.Default.LocalShipping,
                title = "Envío Express de Cortesía",
                desc = "Gratuito en pedidos superiores a $100 con seguro incluido."
            )
            ValuePropRow(
                icon = Icons.Default.Diamond,
                title = "Garantía Sartorial 100%",
                desc = "Materiales nobles y confección de primera categoría."
            )
            ValuePropRow(
                icon = Icons.Default.CheckCircle,
                title = "Cambios & Devoluciones Sin Coste",
                desc = "30 días para probar tus prendas con recogida a domicilio."
            )
        }
    }
}
