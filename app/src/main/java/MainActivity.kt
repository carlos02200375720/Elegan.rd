package com.example

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.admin.MenuAdminScreen
import com.example.admin.PublicacionesScreen
import com.example.admin.PublicarScreen
import com.example.data.model.Announcement
import com.example.data.model.Product
import com.example.data.remote.AuthUser
import com.example.data.remote.EleganApiClient
import com.example.data.remote.MobileSession
import com.example.ui.CarritoScreen
import com.example.ui.CatalogoScreen
import com.example.ui.ChatsScreen
import com.example.ui.EleganBottomBarShadow
import com.example.ui.GraciaScreen
import com.example.ui.InicioScreen
import com.example.ui.MenuScreen
import com.example.ui.PerfilScreen
import com.example.ui.ProductoScreen
import com.example.ui.VerificasionScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganTheme
import com.example.ui.viewmodel.EleganViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen {
    INICIO,
    MENU,
    CATALOGO,
    PRODUCTO,
    CARRITO,
    VERIFICASION,
    GRACIA,
    PERFIL,
    CHATS,
    PUBLICAR,
    MENU_ADMIN,
    PUBLICASIONE
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.WHITE,
                AndroidColor.WHITE
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.WHITE,
                AndroidColor.WHITE
            )
        )
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val mediaDir = java.io.File(filesDir, "product_media")
                if (mediaDir.exists()) {
                    mediaDir.deleteRecursively()
                }
                getSharedPreferences("elegan_admin_prefs", android.content.Context.MODE_PRIVATE)
                    .edit().clear().apply()
            } catch (_: Exception) {}
        }

        configureCoilImageLoader()

        setContent {
            EleganTheme {
                MainApp()
            }
        }
    }

    private fun configureCoilImageLoader() {
        if (isCoilConfigured) return
        synchronized(this) {
            if (isCoilConfigured) return
            val appCtx = applicationContext
            Coil.setImageLoader(
                ImageLoader.Builder(appCtx)
                    .memoryCache {
                        MemoryCache.Builder(appCtx)
                            .maxSizePercent(0.30)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(appCtx.cacheDir.resolve("elegan_image_cache"))
                            .maxSizePercent(0.05)
                            .build()
                    }
                    .respectCacheHeaders(false)
                    .allowRgb565(true)
                    .crossfade(false)
                    .build()
            )
            isCoilConfigured = true
        }
    }

    companion object {
        @Volatile
        private var isCoilConfigured = false
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf<AuthUser?>(null) }
    var isSessionReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loadedUser = withContext(Dispatchers.IO) {
            val cachedUser = MobileSession.user(context)
            EleganApiClient.store
            cachedUser
        }
        currentUser = loadedUser
        isSessionReady = true
        attemptAutoSignIn(
            context = context,
            onAuthSuccess = { currentUser = MobileSession.user(context) },
            onUnauthenticated = { currentUser = null },
            scope = coroutineScope
        )
    }

    val activity = context as? Activity
    LaunchedEffect(currentUser) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    when {
        !isSessionReady -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
        currentUser == null -> {
            AuthScreen(
                onAuthSuccess = { currentUser = MobileSession.user(context) }
            )
        }
        else -> {
            AppContent(
                onSignOut = {
                    signOutUser(
                        context = context,
                        onSignOutComplete = { currentUser = null },
                        scope = coroutineScope
                    )
                }
            )
        }
    }
}

@Composable
fun AppContent(
    viewModel: EleganViewModel = viewModel(),
    onSignOut: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.INICIO) }
    var previousScreen by remember { mutableStateOf(Screen.INICIO) }

    // Checkout verification parameters
    var checkoutSubtotal by remember { mutableDoubleStateOf(0.0) }
    var checkoutDiscount by remember { mutableDoubleStateOf(0.0) }
    var checkoutShipping by remember { mutableDoubleStateOf(0.0) }
    var checkoutTotal by remember { mutableDoubleStateOf(0.0) }

    // State flows
    val products by viewModel.products.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val savedProductIds by viewModel.savedProductIds.collectAsState()
    val savedProducts by viewModel.savedProducts.collectAsState()
    val publicationStats by viewModel.publicationStats.collectAsState()
    val likedPublicationIds by viewModel.likedPublicationIds.collectAsState()
    val publicationComments by viewModel.publicationComments.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val lastOrderId by viewModel.lastCreatedOrderId.collectAsState()
    val lastOrderPaymentMethod by viewModel.lastOrderPaymentMethod.collectAsState()
    val lastOrderDetails by viewModel.lastOrderDetails.collectAsState()

    fun navigateTo(screen: Screen) {
        previousScreen = currentScreen
        currentScreen = screen
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                when {
                    targetState == Screen.GRACIA -> {
                        ContentTransform(
                            targetContentEnter = slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 320)
                            ),
                            initialContentExit = fadeOut(
                                animationSpec = tween(durationMillis = 320),
                                targetAlpha = 0.99f
                            ),
                            targetContentZIndex = 1f,
                            sizeTransform = SizeTransform(clip = false)
                        )
                    }
                    initialState == Screen.GRACIA -> {
                        ContentTransform(
                            targetContentEnter = fadeIn(
                                animationSpec = tween(durationMillis = 280),
                                initialAlpha = 0.99f
                            ),
                            initialContentExit = slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 280)
                            ),
                            targetContentZIndex = -1f,
                            sizeTransform = SizeTransform(clip = false)
                        )
                    }
                    targetState == Screen.VERIFICASION -> {
                        ContentTransform(
                            targetContentEnter = slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 320)
                            ),
                            initialContentExit = fadeOut(
                                animationSpec = tween(durationMillis = 320),
                                targetAlpha = 0.99f
                            ),
                            targetContentZIndex = 1f,
                            sizeTransform = SizeTransform(clip = false)
                        )
                    }
                    initialState == Screen.VERIFICASION && targetState == Screen.CARRITO -> {
                        ContentTransform(
                            targetContentEnter = fadeIn(
                                animationSpec = tween(durationMillis = 280),
                                initialAlpha = 0.99f
                            ),
                            initialContentExit = slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 280)
                            ),
                            targetContentZIndex = -1f,
                            sizeTransform = SizeTransform(clip = false)
                        )
                    }
                    targetState == Screen.CARRITO -> {
                        ContentTransform(
                            targetContentEnter = slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 320)
                            ),
                            initialContentExit = fadeOut(
                                animationSpec = tween(durationMillis = 320),
                                targetAlpha = 0.99f
                            ),
                            targetContentZIndex = 1f,
                            sizeTransform = SizeTransform(clip = false)
                        )
                    }
                    initialState == Screen.CARRITO && targetState != Screen.VERIFICASION -> {
                        ContentTransform(
                            targetContentEnter = fadeIn(
                                animationSpec = tween(durationMillis = 280),
                                initialAlpha = 0.99f
                            ),
                            initialContentExit = slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 280)
                            ),
                            targetContentZIndex = -1f,
                            sizeTransform = SizeTransform(clip = false)
                        )
                    }
                    targetState == Screen.PRODUCTO -> {
                        ContentTransform(
                            targetContentEnter = slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 320)
                            ),
                            initialContentExit = fadeOut(
                                animationSpec = tween(durationMillis = 320),
                                targetAlpha = 0.99f
                            ),
                            targetContentZIndex = 1f,
                            sizeTransform = SizeTransform(clip = false)
                        )
                    }
                    initialState == Screen.PRODUCTO && targetState != Screen.CARRITO -> {
                        ContentTransform(
                            targetContentEnter = fadeIn(
                                animationSpec = tween(durationMillis = 280),
                                initialAlpha = 0.99f
                            ),
                            initialContentExit = slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 280)
                            ),
                            targetContentZIndex = -1f,
                            sizeTransform = SizeTransform(clip = false)
                        )
                    }
                    else -> {
                        ContentTransform(
                            targetContentEnter = EnterTransition.None,
                            initialContentExit = ExitTransition.None,
                            sizeTransform = SizeTransform(clip = false)
                        )
                    }
                }
            },
            label = "screen_transition"
        ) { screen ->
            val isTabScreen = screen == Screen.INICIO || screen == Screen.CATALOGO || screen == Screen.PERFIL
            if (isTabScreen) {
                Scaffold(
                    contentWindowInsets = if (screen == Screen.PERFIL) {
                        WindowInsets(0, 0, 0, 0)
                    } else {
                        ScaffoldDefaults.contentWindowInsets
                    },
                    bottomBar = {
                        EleganBottomBar(
                            currentScreen = screen,
                            cartItemCount = cartItems.sumOf { it.quantity },
                            onNavigateToInicio = { navigateTo(Screen.INICIO) },
                            onNavigateToCatalogo = {
                                viewModel.setCategory("todo")
                                navigateTo(Screen.CATALOGO)
                            },
                            onNavigateToCarrito = { navigateTo(Screen.CARRITO) },
                            onNavigateToChats = { navigateTo(Screen.CHATS) },
                            onNavigateToPerfil = { navigateTo(Screen.PERFIL) }
                        )
                    },
                    containerColor = Color.White
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        when (screen) {
                            Screen.INICIO -> {
                                InicioScreen(
                                    featuredProducts = products,
                                    userProfile = userProfile,
                                    cartItemCount = cartItems.sumOf { it.quantity },
                                    savedProductIds = savedProductIds,
                                    publicationStats = publicationStats,
                                    likedPublicationIds = likedPublicationIds,
                                    publicationComments = publicationComments,
                                    onToggleLike = { id, onResult ->
                                        viewModel.togglePublicationLike(id, onResult)
                                    },
                                    onOpenComments = viewModel::openPublicationComments,
                                    onSubmitComment = { productId, displayName, text, onResult ->
                                        viewModel.addPublicationComment(productId, displayName, text, onResult)
                                    },
                                    onRecordShare = { productId, onResult ->
                                        viewModel.recordPublicationShare(productId, onResult)
                                    },
                                    onToggleSaveProduct = { id, onResult ->
                                        viewModel.toggleSaveProduct(id, onResult)
                                    },
                                    onNavigateToCatalogo = { cat ->
                                        viewModel.setCategory(cat)
                                        navigateTo(Screen.CATALOGO)
                                    },
                                    onNavigateToProduct = { prod ->
                                        viewModel.selectProduct(prod)
                                        navigateTo(Screen.PRODUCTO)
                                    },
                                    onNavigateToMenu = { navigateTo(Screen.MENU) },
                                    onNavigateToCategoryMenu = { navigateTo(Screen.MENU) },
                                    onNavigateToChats = { navigateTo(Screen.CHATS) },
                                    onNavigateToCart = { navigateTo(Screen.CARRITO) }
                                )
                            }

                            Screen.CATALOGO -> {
                                CatalogoScreen(
                                    products = products,
                                    selectedCategory = selectedCategory,
                                    onCategoryChange = { viewModel.setCategory(it) },
                                    onNavigateToMenu = { navigateTo(Screen.MENU) },
                                    onProductClick = { prod ->
                                        viewModel.selectProduct(prod)
                                        navigateTo(Screen.PRODUCTO)
                                    },
                                    onQuickAddToCart = { prod ->
                                        val defaultSize = prod.sizes.firstOrNull() ?: "Estándar"
                                        val defaultColor = prod.colors.firstOrNull().orEmpty()
                                        viewModel.addToCart(prod, defaultSize, 1L, defaultColor)
                                    },
                                    cartItemCount = cartItems.sumOf { it.quantity },
                                    onNavigateToCart = { navigateTo(Screen.CARRITO) },
                                    savedProductIds = savedProductIds,
                                    onToggleSaveProduct = { id ->
                                        viewModel.toggleSaveProduct(id)
                                    }
                                )
                            }

                            Screen.PERFIL -> {
                                PerfilScreen(
                                    userProfile = userProfile,
                                    orders = orders,
                                    savedProducts = savedProducts,
                                    cartItemCount = cartItems.sumOf { it.quantity },
                                    onNavigateToCart = { navigateTo(Screen.CARRITO) },
                                    onProductClick = { prod ->
                                        viewModel.selectProduct(prod)
                                        navigateTo(Screen.PRODUCTO)
                                    },
                                    onExploreClick = { navigateTo(Screen.INICIO) },
                                    onSaveProfile = { ph, ad, ct -> viewModel.saveProfile(ph, ad, ct) },
                                    onNavigateToChats = { navigateTo(Screen.CHATS) },
                                    onNavigateToPublicar = { navigateTo(Screen.PUBLICAR) },
                                    onNavigateToMenuAdmin = { navigateTo(Screen.MENU_ADMIN) },
                                    onSignOut = onSignOut
                                )
                            }

                            else -> {}
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
                ) {
                    when (screen) {
                Screen.MENU -> {
                    MenuScreen(
                        products = products,
                        onCategorySelected = { cat ->
                            viewModel.setCategory(cat)
                            navigateTo(Screen.CATALOGO)
                        },
                        onBack = { navigateTo(previousScreen) }
                    )
                }

                Screen.PRODUCTO -> {
                    val prod = selectedProduct
                        ?: cartItems.lastOrNull()?.let { item -> products.find { it.id == item.productId } }
                        ?: products.firstOrNull()
                    if (prod != null) {
                        ProductoScreen(
                            product = prod,
                            cartItemCount = cartItems.sumOf { it.quantity },
                            isSaved = savedProductIds.contains(prod.id),
                            onToggleSave = { onResult ->
                                viewModel.toggleSaveProduct(prod.id, onResult)
                            },
                            onAddToCart = { p, s, color, q ->
                                viewModel.addToCart(p, s, q, color)
                            },
                            onBuyNow = { p, s, color, q ->
                                viewModel.addToCart(p, s, q, color)
                                navigateTo(Screen.CARRITO)
                            },
                            onNavigateToCart = { navigateTo(Screen.CARRITO) },
                            onBack = { navigateTo(Screen.CATALOGO) }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navigateTo(Screen.CATALOGO)
                        }
                    }
                }

                Screen.CARRITO -> {
                    CarritoScreen(
                        cartItems = cartItems,
                        onUpdateQuantity = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                        onRemoveItem = { id -> viewModel.removeFromCart(id) },
                        onNavigateToVerificasion = { sub, disc, ship, tot ->
                            checkoutSubtotal = sub
                            checkoutDiscount = disc
                            checkoutShipping = ship
                            checkoutTotal = tot
                            navigateTo(Screen.VERIFICASION)
                        },
                        onExploreCatalog = {
                            viewModel.setCategory("todo")
                            navigateTo(Screen.CATALOGO)
                        },
                        onBack = { navigateTo(Screen.PRODUCTO) }
                    )
                }

                Screen.VERIFICASION -> {
                    VerificasionScreen(
                        cartItems = cartItems,
                        subtotal = checkoutSubtotal,
                        discount = checkoutDiscount,
                        shipping = checkoutShipping,
                        total = checkoutTotal,
                        userProfile = userProfile,
                        onConfirmOrder = { name, email, phone, addr, city, payMethod, paymentProofUri ->
                            val itemsSummary = cartItems.joinToString(", ") { "${it.quantity}x ${it.name}" }
                            viewModel.placeOrder(
                                customerName = name,
                                email = email,
                                phone = phone,
                                address = addr,
                                city = city,
                                paymentMethod = payMethod,
                                total = checkoutTotal,
                                itemCount = cartItems.sumOf { it.quantity },
                                itemsSummary = itemsSummary,
                                paymentProofUri = paymentProofUri,
                                onSuccess = {
                                    navigateTo(Screen.GRACIA)
                                }
                            )
                        },
                        onBack = { navigateTo(Screen.CARRITO) }
                    )
                }

                Screen.GRACIA -> {
                    GraciaScreen(
                        orderId = lastOrderId,
                        isBankTransfer = lastOrderPaymentMethod.contains("Transferencia", ignoreCase = true),
                        orderDetails = lastOrderDetails,
                        onNavigateToInicio = { navigateTo(Screen.INICIO) },
                        onNavigateToPerfil = { navigateTo(Screen.PERFIL) },
                        onNavigateToChats = { navigateTo(Screen.CHATS) }
                    )
                }

                Screen.CHATS -> {
                    ChatsScreen(
                        messages = messages,
                        cartItemCount = cartItems.sumOf { it.quantity },
                        onNavigateToCart = { navigateTo(Screen.CARRITO) },
                        onSendMessage = { text -> viewModel.sendSupportMessage(text) },
                        onBack = { navigateTo(previousScreen) }
                    )
                }

                Screen.PUBLICAR -> {
                    PublicarScreen(
                        onPublishProduct = { prod: Product, onResult: (Result<Unit>) -> Unit ->
                            viewModel.publishProduct(prod, onResult)
                        },
                        onPublishAnnouncement = { ann: Announcement ->
                            viewModel.publishAnnouncement(ann)
                        },
                        onBack = { navigateTo(previousScreen) }
                    )
                }

                Screen.MENU_ADMIN -> {
                    MenuAdminScreen(
                        onNavigateToPublicar = { navigateTo(Screen.PUBLICAR) },
                        onNavigateToPublicaciones = { navigateTo(Screen.PUBLICASIONE) },
                        onNavigateToPerfil = { navigateTo(Screen.PERFIL) },
                        onNavigateToInicio = { navigateTo(Screen.INICIO) },
                        onNavigateToCatalogo = { navigateTo(Screen.CATALOGO) },
                        onNavigateToCategorias = { navigateTo(Screen.MENU) },
                        onNavigateToChats = { navigateTo(Screen.CHATS) },
                        onNavigateToCart = { navigateTo(Screen.CARRITO) },
                        onBack = { navigateTo(previousScreen) }
                    )
                }

                Screen.PUBLICASIONE -> {
                    PublicacionesScreen(
                        products = products,
                        announcements = announcements,
                        onDeleteProduct = { prodId -> viewModel.deleteProduct(prodId) },
                        onToggleProductStock = { prodId, inStock -> viewModel.toggleProductStock(prodId, inStock) },
                        onDeleteAnnouncement = { annId -> viewModel.deleteAnnouncement(annId) },
                        onToggleAnnouncementActive = { annId, active -> viewModel.toggleAnnouncementActive(annId, active) },
                        onNavigateToPublicar = { navigateTo(Screen.PUBLICAR) },
                        onBack = { navigateTo(Screen.MENU_ADMIN) }
                    )
                }

                else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun EleganBottomBar(
    currentScreen: Screen,
    cartItemCount: Long,
    onNavigateToInicio: () -> Unit,
    onNavigateToCatalogo: () -> Unit,
    onNavigateToCarrito: () -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToPerfil: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Sombra superior idéntica a la de la cabecera (2.5.dp, alpha 0.07f)
        EleganBottomBarShadow()

        Surface(
            color = Color.White,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                containerColor = Color.White,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.INICIO,
                    onClick = onNavigateToInicio,
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Inicio",
                            modifier = Modifier.size(25.dp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EleganBurgundy,
                        unselectedIconColor = Color.Black,
                        indicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("nav_inicio")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.CATALOGO,
                    onClick = onNavigateToCatalogo,
                    icon = {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = "Catálogo",
                            modifier = Modifier.size(25.dp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EleganBurgundy,
                        unselectedIconColor = Color.Black,
                        indicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("nav_catalogo")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.CARRITO,
                    onClick = onNavigateToCarrito,
                    icon = {
                        if (cartItemCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = EleganBurgundy,
                                        contentColor = Color.White
                                    ) {
                                        Text("$cartItemCount", fontSize = 9.sp)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.ShoppingBag,
                                    contentDescription = "Carrito",
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.ShoppingBag,
                                contentDescription = "Carrito",
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EleganBurgundy,
                        unselectedIconColor = Color.Black,
                        indicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("nav_carrito")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.CHATS,
                    onClick = onNavigateToChats,
                    icon = {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "Soporte",
                            modifier = Modifier.size(25.dp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EleganBurgundy,
                        unselectedIconColor = Color.Black,
                        indicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("nav_chats")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.PERFIL,
                    onClick = onNavigateToPerfil,
                    icon = {
                        Icon(
                            Icons.Default.PersonOutline,
                            contentDescription = "Perfil",
                            modifier = Modifier.size(25.dp)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EleganBurgundy,
                        unselectedIconColor = Color.Black,
                        indicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("nav_perfil")
                )
            }
        }
        }
    }
}
