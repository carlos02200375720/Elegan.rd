package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.BuildConfig
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import java.util.Locale
import kotlinx.coroutines.launch
import org.json.JSONObject

private class PayPalJsBridge(
    private val onSdkReadyCallback: () -> Unit,
    private val onSdkErrorCallback: (String) -> Unit,
    private val onContentHeightChangedCallback: (Int) -> Unit,
    private val onPaymentSuccessCallback: (String, String) -> Unit,
    private val onPaymentCancelledCallback: () -> Unit,
    private val onPaymentErrorCallback: (String) -> Unit
) {
    @JavascriptInterface
    fun onSdkReady() {
        onSdkReadyCallback()
    }

    @JavascriptInterface
    fun onSdkError(message: String?) {
        onSdkErrorCallback(message?.takeIf { it.isNotBlank() } ?: "No se pudo cargar la pasarela de PayPal.")
    }

    @JavascriptInterface
    fun onContentHeightChanged(heightDp: Int) {
        if (heightDp > 0) {
            onContentHeightChangedCallback(heightDp)
        }
    }

    @JavascriptInterface
    fun onPaymentSuccess(payloadJson: String?) {
        try {
            val json = JSONObject(payloadJson ?: "{}")
            val orderId = json.optString("orderId", "")
            val fundingSource = json.optString("fundingSource", "paypal").lowercase(Locale.ROOT)
            val methodLabel = when {
                fundingSource.contains("card") ->
                    if (orderId.isNotBlank()) "Tarjeta de Crédito / Débito (PayPal #$orderId)"
                    else "Tarjeta de Crédito / Débito (PayPal)"
                else ->
                    if (orderId.isNotBlank()) "PayPal (#$orderId)"
                    else "PayPal"
            }
            onPaymentSuccessCallback(methodLabel, orderId)
        } catch (_: Exception) {
            onPaymentSuccessCallback("PayPal / Tarjeta", "")
        }
    }

    @JavascriptInterface
    fun onPaymentCancelled() {
        onPaymentCancelledCallback()
    }

    @JavascriptInterface
    fun onPaymentError(message: String?) {
        onPaymentErrorCallback(message?.takeIf { it.isNotBlank() } ?: "Ocurrió un error al procesar el pago.")
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PaypalScreen(
    total: Double,
    customerName: String,
    customerEmail: String,
    itemCount: Long,
    itemsSummary: String,
    isSubmittingOrder: Boolean,
    onBack: () -> Unit,
    onPaymentSuccess: (paymentMethodLabel: String, transactionId: String) -> Unit
) {
    val clientId = remember { BuildConfig.PAYPAL_CLIENT_ID.trim() }
    val isClientIdConfigured = remember(clientId) {
        clientId.isNotBlank() && clientId != "PAYPAL_CLIENT_ID_NOT_SET"
    }

    var isSdkLoading by remember { mutableStateOf(isClientIdConfigured) }
    var sdkErrorMessage by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var webViewHeightDp by remember { mutableIntStateOf(380) }
    var popupWebView by remember { mutableStateOf<WebView?>(null) }
    var mainWebViewRef by remember { mutableStateOf<WebView?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val dismissPopup: () -> Unit = {
        popupWebView?.let { popup ->
            try {
                popup.stopLoading()
                popup.destroy()
            } catch (_: Exception) {}
        }
        popupWebView = null
    }

    BackHandler(enabled = !isSubmittingOrder) {
        if (popupWebView != null) {
            dismissPopup()
        } else {
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            dismissPopup()
            mainWebViewRef?.let { webView ->
                try {
                    webView.stopLoading()
                    webView.destroy()
                } catch (_: Exception) {}
            }
            mainWebViewRef = null
        }
    }

    val amountFormatted = remember(total) {
        String.format(Locale.US, "%.2f", total.coerceAtLeast(0.01))
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("paypal_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            if (!isSubmittingOrder) {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("paypal_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = EleganCharcoal
                        )
                    }

                    Text(
                        text = "Tarjeta o PayPal",
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
                        modifier = Modifier.testTag("paypal_header_amount")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Total a pagar",
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
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSubmittingOrder) "Confirmando pedido..." else "Cifrado SSL por PayPal",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = EleganCharcoal,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onBack,
                        enabled = !isSubmittingOrder,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, EleganBurgundy.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EleganBurgundy),
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("paypal_cancel_bottom_btn")
                    ) {
                        Text(
                            text = "Cambiar método",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Resumen del pedido que se desplaza con el scroll general
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFAF8F5),
                    border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Checkout Seguro Elegan",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal
                                    )
                                )
                            }
                            Text(
                                text = "$itemCount art.",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = EleganBurgundy,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (customerName.isNotBlank() || customerEmail.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = listOf(customerName, customerEmail).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF5F5F5F),
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF003087).copy(alpha = 0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color(0xFF003087),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Cuenta PayPal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF003087)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EleganGold.copy(alpha = 0.16f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = EleganCharcoal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Visa / Mastercard / Débito",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EleganCharcoal
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isClientIdConfigured) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFAF6F1),
                            border = BorderStroke(1.dp, EleganBurgundy.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = EleganBurgundy,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Falta configurar PAYPAL_CLIENT_ID",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Agrega tu variable PAYPAL_CLIENT_ID en el panel de Secretos de AI Studio y recompila la aplicación para habilitar los pagos con tarjeta y PayPal.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF5F5F5F),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 380.dp)
                            .height(webViewHeightDp.dp)
                    ) {
                        val checkoutHtml = remember(clientId, amountFormatted, customerName, customerEmail, itemsSummary, reloadKey) {
                            buildPayPalCheckoutHtml(
                                clientId = clientId,
                                amountFormatted = amountFormatted,
                                customerName = customerName,
                                customerEmail = customerEmail,
                                itemsSummary = itemsSummary
                            )
                        }

                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("paypal_webview"),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                                    isVerticalScrollBarEnabled = false
                                    isHorizontalScrollBarEnabled = false
                                    overScrollMode = View.OVER_SCROLL_NEVER
                                    configurePayPalWebViewSettings(this)

                                    val bridge = PayPalJsBridge(
                                        onSdkReadyCallback = {
                                            post {
                                                isSdkLoading = false
                                                sdkErrorMessage = null
                                            }
                                        },
                                        onSdkErrorCallback = { msg ->
                                            post {
                                                isSdkLoading = false
                                                sdkErrorMessage = msg
                                            }
                                        },
                                        onContentHeightChangedCallback = { measuredHeightDp ->
                                            post {
                                                val bounded = measuredHeightDp.coerceIn(320, 2600)
                                                if (kotlin.math.abs(webViewHeightDp - bounded) > 4) {
                                                    webViewHeightDp = bounded
                                                }
                                            }
                                        },
                                        onPaymentSuccessCallback = { label, txId ->
                                            post {
                                                dismissPopup()
                                                onPaymentSuccess(label, txId)
                                            }
                                        },
                                        onPaymentCancelledCallback = {
                                            post {
                                                dismissPopup()
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Pago cancelado. Puedes intentarlo nuevamente.")
                                                }
                                            }
                                        },
                                        onPaymentErrorCallback = { msg ->
                                            post {
                                                dismissPopup()
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(msg)
                                                }
                                            }
                                        }
                                    )
                                    addJavascriptInterface(bridge, "AndroidPayPal")

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val scheme = request?.url?.scheme?.lowercase(Locale.ROOT).orEmpty()
                                            return scheme != "http" && scheme != "https"
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?
                                        ) {
                                            if (request?.isForMainFrame == true) {
                                                isSdkLoading = false
                                                sdkErrorMessage = "No se pudo conectar con PayPal. Verifica tu conexión e inténtalo nuevamente."
                                            }
                                        }
                                    }

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onCreateWindow(
                                            view: WebView?,
                                            isDialog: Boolean,
                                            isUserGesture: Boolean,
                                            resultMsg: Message?
                                        ): Boolean {
                                            val parentCtx = view?.context ?: return false
                                            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                            val childWebView = WebView(parentCtx).apply {
                                                layoutParams = ViewGroup.LayoutParams(
                                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                                configurePayPalWebViewSettings(this)
                                                webViewClient = object : WebViewClient() {
                                                    override fun shouldOverrideUrlLoading(
                                                        w: WebView?,
                                                        req: WebResourceRequest?
                                                    ): Boolean {
                                                        val scheme = req?.url?.scheme?.lowercase(Locale.ROOT).orEmpty()
                                                        return scheme != "http" && scheme != "https"
                                                    }
                                                }
                                                webChromeClient = object : WebChromeClient() {
                                                    override fun onCloseWindow(window: WebView?) {
                                                        post { dismissPopup() }
                                                    }
                                                }
                                            }
                                            popupWebView = childWebView
                                            transport.webView = childWebView
                                            resultMsg.sendToTarget()
                                            return true
                                        }

                                        override fun onCloseWindow(window: WebView?) {
                                            post { dismissPopup() }
                                        }
                                    }

                                    mainWebViewRef = this
                                    tag = reloadKey
                                    loadDataWithBaseURL(
                                        BuildConfig.ELEGAN_API_BASE_URL,
                                        checkoutHtml,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            },
                            update = { webView ->
                                if (webView.tag != reloadKey) {
                                    webView.tag = reloadKey
                                    isSdkLoading = true
                                    sdkErrorMessage = null
                                    webView.loadDataWithBaseURL(
                                        BuildConfig.ELEGAN_API_BASE_URL,
                                        checkoutHtml,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            }
                        )

                        if (isSdkLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.94f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = EleganBurgundy,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Cargando pasarela segura de PayPal...",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = EleganCharcoal,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }

                        if (sdkErrorMessage != null && !isSdkLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = EleganBurgundy,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = sdkErrorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = EleganCharcoal,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { reloadKey++ },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EleganBurgundy,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("paypal_retry_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Reintentar", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isSubmittingOrder) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = EleganBurgundy,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Pago aprobado. Registrando tu pedido...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            )
                        )
                    }
                }
            }
        }
    }

    val activePopup = popupWebView
    if (activePopup != null) {
        Dialog(
            onDismissRequest = dismissPopup,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val dialogView = LocalView.current
            SideEffect {
                (dialogView.parent as? DialogWindowProvider)?.window?.let { win ->
                    win.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    win.setBackgroundDrawable(ColorDrawable(AndroidColor.WHITE))
                    WindowCompat.getInsetsController(win, dialogView).apply {
                        isAppearanceLightStatusBars = true
                        isAppearanceLightNavigationBars = true
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .imePadding()
                ) {
                    Surface(
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Autorización PayPal",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal
                                    )
                                )
                            }
                            IconButton(
                                onClick = dismissPopup,
                                modifier = Modifier.testTag("paypal_popup_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar ventana de PayPal",
                                    tint = EleganCharcoal
                                )
                            }
                        }
                    }

                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        factory = { activePopup }
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configurePayPalWebViewSettings(webView: WebView) {
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(webView, true)
    }
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)
        loadWithOverviewMode = true
        useWideViewPort = true
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }
}

private fun buildPayPalCheckoutHtml(
    clientId: String,
    amountFormatted: String,
    customerName: String,
    customerEmail: String,
    itemsSummary: String
): String {
    val sdkUrl = "https://www.paypal.com/sdk/js?client-id=${Uri.encode(clientId)}&currency=USD&intent=capture&enable-funding=card&locale=es_ES"
    val safeDescription = JSONObject.quote(itemsSummary.ifBlank { "Compra en Elegan" }.take(120))
    val safeAmount = JSONObject.quote(amountFormatted)
    val safeEmail = JSONObject.quote(customerEmail.trim())
    val nameParts = customerName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val safeGivenName = JSONObject.quote(nameParts.firstOrNull().orEmpty())
    val safeSurname = JSONObject.quote(nameParts.drop(1).joinToString(" "))

    return """
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <style>
    * { box-sizing: border-box; }
    html, body {
      margin: 0;
      padding: 0;
      height: auto;
      overflow-x: hidden;
      background: #FFFFFF;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      color: #1F1F1F;
    }
    .container {
      padding: 4px 16px 28px 16px;
      max-width: 520px;
      margin: 0 auto;
    }
    .hint {
      font-size: 13px;
      color: #5F5F5F;
      margin: 0 0 14px 0;
      line-height: 1.4;
    }
    #paypal-button-container {
      min-height: 120px;
    }
    .status-note {
      margin-top: 12px;
      font-size: 12px;
      color: #7A726B;
      text-align: center;
    }
  </style>
  <script
    src="$sdkUrl"
    onerror="if(window.AndroidPayPal){window.AndroidPayPal.onSdkError('No se pudo cargar el script de PayPal. Revisa tu PAYPAL_CLIENT_ID o tu conexión.');}"
  ></script>
</head>
<body>
  <div id="checkout-root" class="container">
    <p class="hint">
      Elige <strong>PayPal</strong> para pagar con tu cuenta o selecciona <strong>Tarjeta de débito o crédito</strong> para ingresar tu tarjeta directamente aquí.
    </p>
    <div id="paypal-button-container"></div>
    <p class="status-note">El cargo se realizará en dólares estadounidenses ($amountFormatted USD).</p>
  </div>

  <script>
    (function initPayPal() {
      function notifyHeight() {
        if (!window.AndroidPayPal || !window.AndroidPayPal.onContentHeightChanged) return;
        var root = document.getElementById('checkout-root');
        if (!root) return;
        var measured = Math.ceil(root.getBoundingClientRect().height + 12);
        if (measured > 0) {
          window.AndroidPayPal.onContentHeightChanged(measured);
        }
      }

      if (typeof ResizeObserver !== 'undefined') {
        var ro = new ResizeObserver(function() { notifyHeight(); });
        var rootEl = document.getElementById('checkout-root');
        if (rootEl) ro.observe(rootEl);
      }
      if (typeof MutationObserver !== 'undefined') {
        var btnContainer = document.getElementById('paypal-button-container');
        if (btnContainer) {
          var mo = new MutationObserver(function() { setTimeout(notifyHeight, 80); });
          mo.observe(btnContainer, { childList: true, subtree: true, attributes: true });
        }
      }

      if (typeof paypal === 'undefined') {
        if (window.AndroidPayPal) {
          window.AndroidPayPal.onSdkError('No se pudo inicializar PayPal Checkout. Verifica PAYPAL_CLIENT_ID.');
        }
        return;
      }

      var description = $safeDescription;
      var amountValue = $safeAmount;
      var buyerEmail = $safeEmail;
      var givenName = $safeGivenName;
      var surname = $safeSurname;

      paypal.Buttons({
        style: {
          layout: 'vertical',
          shape: 'rect',
          label: 'pay',
          height: 48
        },
        createOrder: function(data, actions) {
          var orderPayload = {
            purchase_units: [{
              description: description,
              amount: {
                currency_code: 'USD',
                value: amountValue
              }
            }]
          };
          var payer = {};
          if (buyerEmail && buyerEmail.indexOf('@') > 0) {
            payer.email_address = buyerEmail;
          }
          if (givenName) {
            payer.name = {
              given_name: givenName,
              surname: surname || givenName
            };
          }
          if (Object.keys(payer).length > 0) {
            orderPayload.payer = payer;
          }
          return actions.order.create(orderPayload);
        },
        onApprove: function(data, actions) {
          return actions.order.capture().then(function(details) {
            if (window.AndroidPayPal) {
              window.AndroidPayPal.onPaymentSuccess(JSON.stringify({
                orderId: (details && details.id) || (data && data.orderID) || '',
                status: (details && details.status) || 'COMPLETED',
                fundingSource: (data && data.fundingSource) || 'paypal',
                payerEmail: (details && details.payer && details.payer.email_address) || ''
              }));
            }
          });
        },
        onCancel: function() {
          if (window.AndroidPayPal) {
            window.AndroidPayPal.onPaymentCancelled();
          }
        },
        onError: function(err) {
          if (window.AndroidPayPal) {
            var msg = (err && err.message) ? err.message : 'Error al iniciar el pago con PayPal.';
            window.AndroidPayPal.onPaymentError(msg);
          }
        }
      }).render('#paypal-button-container').then(function() {
        notifyHeight();
        setTimeout(notifyHeight, 300);
        if (window.AndroidPayPal) {
          window.AndroidPayPal.onSdkReady();
        }
      }).catch(function(err) {
        if (window.AndroidPayPal) {
          var msg = (err && err.message) ? err.message : 'No se pudieron renderizar los botones de PayPal.';
          window.AndroidPayPal.onSdkError(msg);
        }
      });
    })();
  </script>
</body>
</html>
""".trimIndent()
}
