package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import java.util.Locale

@Composable
fun ContraEntregaScreen(
    total: Double,
    customerName: String,
    phone: String,
    address: String,
    city: String,
    itemCount: Long,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onConfirmOrder: () -> Unit
) {
    BackHandler(enabled = !isSubmitting) { onBack() }

    var hasAcceptedResponsibility by remember { mutableStateOf(false) }
    val amountFormatted = remember(total) {
        String.format(Locale.US, "%.2f", total.coerceAtLeast(0.0))
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contraentrega_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 4.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (!isSubmitting) onBack()
                        },
                        modifier = Modifier.testTag("contraentrega_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = EleganCharcoal
                        )
                    }

                    Text(
                        text = "Contra Entrega",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal,
                            fontFamily = FontFamily.Serif,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF9F3EA),
                        modifier = Modifier.testTag("contraentrega_header_amount")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "A pagar al recibir",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF6B6560),
                                    fontSize = 9.sp,
                                    lineHeight = 10.sp
                                )
                            )
                            Text(
                                text = "$$amountFormatted USD",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganBurgundy,
                                    fontSize = 13.sp,
                                    lineHeight = 15.sp
                                )
                            )
                        }
                    }
                }
            }
                EleganTopBarShadow()
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                EleganBottomBarShadow()
                Surface(
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onConfirmOrder,
                        enabled = hasAcceptedResponsibility && !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EleganBurgundy,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFD6CEC7),
                            disabledContentColor = Color.White.copy(alpha = 0.85f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .testTag("confirm_contraentrega_btn")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confirmar Pedido Contra Entrega",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Encabezado informativo principal
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFAF8F5),
                border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(EleganBurgundy.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = EleganBurgundy,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "¿Cómo funciona el Pago Contra Entrega?",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal,
                                fontFamily = FontFamily.Serif,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Con este servicio ordenas hoy sin pagar por adelantado y cancelas el total de tu compra en efectivo al momento en que el mensajero te entregue tu pedido.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF555555),
                                lineHeight = 19.sp
                            )
                        )
                    }
                }
            }

            // Pasos e instrucciones del servicio
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = EleganBurgundy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Instrucciones del Servicio",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF0EAE4))

                    InstructionStepRow(
                        stepNumber = "1",
                        icon = Icons.Default.LocalShipping,
                        title = "Preparación y despacho inmediato",
                        description = "Al confirmar, reservamos tus prendas, realizamos el control de calidad y preparamos el envío exclusivo hacia tu dirección."
                    )

                    InstructionStepRow(
                        stepNumber = "2",
                        icon = Icons.Default.PhoneInTalk,
                        title = "Coordinación previa por llamada o mensaje",
                        description = "Nuestro mensajero se comunicará contigo antes de llegar para confirmar tu disponibilidad y horario de recepción."
                    )

                    InstructionStepRow(
                        stepNumber = "3",
                        icon = Icons.Default.Payments,
                        title = "Pago en efectivo al recibir",
                        description = "Entrega el monto total de $$amountFormatted USD al mensajero al recibir tu paquete. Te recomendamos tener el efectivo exacto listo."
                    )
                }
            }

            // Aviso importante sobre inversión en producto y logística + responsabilidad del cliente
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFF8EE),
                border = BorderStroke(1.2.dp, EleganGold.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contraentrega_responsibility_notice")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = EleganBurgundyDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Compromiso y Responsabilidad",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganBurgundyDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Para que recibas tu pedido sin adelantos, Elegan invierte dinero por adelantado tanto en el producto reservado como en la logística, embalaje de lujo y transporte hasta tu domicilio.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EleganCharcoal,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Por respeto a ese esfuerzo operativo, te pedimos realizar el pedido con total responsabilidad: verifica que tu dirección y teléfono estén correctos, asegúrate de estar disponible para recibirlo y ten el efectivo listo al momento de la entrega.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF4F4A45),
                            lineHeight = 19.sp
                        )
                    )
                }
            }

            // Resumen de datos de entrega donde irá el mensajero
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFAF8F5),
                border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = EleganBurgundy,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Datos de Entrega Confirmados",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cliente: ${customerName.ifBlank { "No especificado" }}",
                        style = MaterialTheme.typography.bodySmall.copy(color = EleganCharcoal, fontWeight = FontWeight.SemiBold)
                    )
                    if (phone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Teléfono: $phone",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555))
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Dirección: ${listOf(address, city).filter { it.isNotBlank() }.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$itemCount artículo(s) • Total a pagar al recibir: $$amountFormatted USD",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = EleganBurgundy,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Casilla de aceptación responsable
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (hasAcceptedResponsibility) Color(0xFFF9F3EA) else Color.White,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (hasAcceptedResponsibility) EleganBurgundy else Color(0xFFD9D0C7)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSubmitting) {
                        hasAcceptedResponsibility = !hasAcceptedResponsibility
                    }
                    .testTag("contraentrega_accept_checkbox")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasAcceptedResponsibility,
                        onCheckedChange = { if (!isSubmitting) hasAcceptedResponsibility = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = EleganBurgundy,
                            uncheckedColor = Color(0xFF8C8279)
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = if (hasAcceptedResponsibility) EleganBurgundy else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Entiendo cómo funciona el servicio contra entrega, la inversión en producto y logística de Elegan, y confirmo mi compromiso responsable de recibir y pagar el pedido.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = EleganCharcoal,
                                fontWeight = if (hasAcceptedResponsibility) FontWeight.SemiBold else FontWeight.Normal,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            if (!hasAcceptedResponsibility) {
                Text(
                    text = "Marca la casilla de compromiso responsable para habilitar la confirmación del pedido.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF7A7A7A)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InstructionStepRow(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(EleganBurgundy),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EleganBurgundy,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal,
                        fontSize = 13.5.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF5F5F5F),
                    lineHeight = 18.sp
                )
            )
        }
    }
}
