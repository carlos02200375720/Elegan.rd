package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory

@Composable
fun GraciaScreen(
    orderId: String?,
    onNavigateToInicio: () -> Unit,
    onNavigateToPerfil: () -> Unit,
    onNavigateToChats: () -> Unit
) {
    BackHandler { onNavigateToInicio() }

    val displayOrderId = orderId ?: "ELG-2026-${(10000..99999).random()}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EleganIvory)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("gracia_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EleganGold.copy(alpha = 0.3f),
                            EleganBurgundy.copy(alpha = 0.15f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(EleganBurgundy),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Éxito",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡GRACIAS POR TU COMPRA!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = EleganBurgundyDark,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tu pedido ha sido confirmado y registrado exitosamente en el sistema de Elegan.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF555555),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Código de Pedido:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF6EFEA)
                    ) {
                        Text(
                            text = displayOrderId,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganBurgundyDark
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = EleganBurgundy,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Entrega Estimada",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                        Text(
                            text = "2 a 3 días hábiles (Express)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = EleganGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Embalaje Exclusivo",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                        Text(
                            text = "Caja rígida protegida con sello de lacre",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNavigateToInicio,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("gracia_keep_shopping_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EleganBurgundy,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Seguir Comprando", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateToPerfil,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("gracia_view_orders_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = EleganCharcoal
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFCCC3BA))
            )
        ) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Ver mis Pedidos", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onNavigateToChats,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("gracia_contact_support_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = EleganBurgundy
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(EleganBurgundy)
            )
        ) {
            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Consultar con Elegan Concierge", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
