package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BankAccountInfo
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
fun TranfereciaScreen(
    total: Double,
    bankAccounts: List<BankAccountInfo>,
    paymentProofUri: Uri?,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onPickPaymentProof: () -> Unit,
    onRemovePaymentProof: () -> Unit,
    onConfirmPayment: () -> Unit
) {
    BackHandler { onBack() }

    val clipboardManager = LocalClipboardManager.current
    var copiedAccountId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tranferecia_screen"),
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
                        onClick = onBack,
                        modifier = Modifier.testTag("tranferecia_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = EleganCharcoal
                        )
                    }

                    Text(
                        text = "Transferencia",
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
                        modifier = Modifier.testTag("tranferecia_header_amount")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Monto a transferir",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF6B6560),
                                    fontSize = 9.sp,
                                    lineHeight = 10.sp
                                )
                            )
                            Text(
                                text = "$${String.format("%.2f", total)} USD",
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
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val isCompactBar = maxWidth < 350.dp
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = if (isCompactBar) 12.dp else 16.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onConfirmPayment,
                            enabled = paymentProofUri != null && !isSubmitting,
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
                                .testTag("confirm_bank_transfer_btn")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Confirmar Pago",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            }
        }
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
                isCompact -> 12.dp
                isWide -> 24.dp
                else -> 20.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad, vertical = 16.dp)
            ) {
            Text(
                text = "Transfiere el monto total a cualquiera de las siguientes cuentas bancarias y sube la imagen del comprobante para confirmar tu pago.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF5F5F5F),
                    lineHeight = 18.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                bankAccounts.forEach { account ->
                    val isCopied = copiedAccountId == account.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFAF8F5),
                        border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bank_account_card_${account.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = EleganBurgundy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = account.bankName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EleganCharcoal
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (account.accountType.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = EleganGold.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = account.accountType,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = EleganBurgundyDark,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 10.sp
                                            ),
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFEAE3DC), RoundedCornerShape(8.dp))
                                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Número de cuenta",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Text(
                                        text = account.accountNumber,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EleganCharcoal,
                                            letterSpacing = 0.6.sp
                                        )
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(account.accountNumber))
                                        copiedAccountId = account.id
                                    },
                                    modifier = Modifier.testTag("copy_account_${account.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = "Copiar número de cuenta",
                                        tint = if (isCopied) Color(0xFF2E7D32) else EleganBurgundy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = "Dueño de la cuenta",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Text(
                                        text = account.holderName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = EleganCharcoal
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No. de documento",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    )
                                    Text(
                                        text = account.holderDocument,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = EleganCharcoal
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Evidencia del Pago (Comprobante) *",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (paymentProofUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = if (isCompact) 114.dp else 126.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFAF8F5))
                        .border(1.5.dp, EleganBurgundy.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .clickable { onPickPaymentProof() }
                        .padding(16.dp)
                        .testTag("upload_payment_proof_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(EleganBurgundy.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Cargar imagen del comprobante",
                                tint = EleganBurgundy,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Cargar imagen de evidencia del pago",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganBurgundy,
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Toca para seleccionar la captura de tu transferencia",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFAF8F5),
                    border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_proof_preview_card")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        AsyncImage(
                            model = paymentProofUri,
                            contentDescription = "Evidencia de pago cargada",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Comprobante cargado",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onPickPaymentProof,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    border = BorderStroke(1.dp, EleganBurgundy.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EleganBurgundy),
                                    modifier = Modifier.testTag("change_payment_proof_btn")
                                ) {
                                    Text("Cambiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(
                                    onClick = onRemovePaymentProof,
                                    modifier = Modifier.testTag("remove_payment_proof_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar comprobante",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (paymentProofUri == null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Adjunta la imagen de evidencia del pago para habilitar el botón de confirmación.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF7A7A7A)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
        }
    }
}
