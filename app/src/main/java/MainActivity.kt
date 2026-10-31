package com.example

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import android.graphics.Color as AndroidColor
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.admin.MenuAdminScreen
import com.example.admin.PublicarScreen
import com.example.admin.PublicacionesScreen
import com.example.data.model.Announcement
import com.example.data.model.Product
import com.example.ui.CarritoScreen
import com.example.ui.CatalogoScreen
import com.example.ui.ChatsScreen
import com.example.ui.GraciaScreen
import com.example.ui.InicioScreen
import com.example.ui.MenuScreen
import com.example.ui.ProductoScreen
import com.example.ui.PerfilScreen
import com.example.ui.VerificasionScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory
import com.example.ui.theme.EleganTheme
import com.example.ui.viewmodel.EleganViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
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
        window.navigationBarColor = AndroidColor.WHITE
        window.statusBarColor = AndroidColor.WHITE
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
        setContent {
            EleganTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                },
                onUnauthenticated = {
                    // Stay on AuthScreen
                },
                scope = coroutineScope
            )
        }
    }

    val activity = context as? Activity
    LaunchedEffect(currentUser) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
            window.statusBarColor = AndroidColor.WHITE
            window.navigationBarColor = AndroidColor.WHITE
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        AppContent(
            onSignOut = {
                signOutUser(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = { currentUser = null },
                    scope = coroutineScope
                )
            }
        )
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

    fun navigateTo(screen: Screen) {
        previousScreen = currentScreen
        currentScreen = screen
    }

    val showBottomBar = currentScreen in listOf(
        Screen.INICIO,
        Screen.CATALOGO,
        Screen.PERFIL
    )

    Scaffold(
        contentWindowInsets = if (currentScreen == Screen.PERFIL) {
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
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
                                .height(60.dp),
                            containerColor = Color.White,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.INICIO,
                            onClick = { navigateTo(Screen.INICIO) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio", modifier = Modifier.size(20.dp)) },
                            label = { Text("Inicio", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EleganBurgundy,
                                selectedTextColor = EleganBurgundy,
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("nav_inicio")
                        )

                        NavigationBarItem(
                            selected = currentScreen == Screen.CATALOGO,
                            onClick = {
                                viewModel.setCategory("todo")
                                navigateTo(Screen.CATALOGO)
                            },
                            icon = { Icon(Icons.Default.GridView, contentDescription = "Catálogo", modifier = Modifier.size(20.dp)) },
                            label = { Text("Catálogo", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EleganBurgundy,
                                selectedTextColor = EleganBurgundy,
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("nav_catalogo")
                        )

                        NavigationBarItem(
                            selected = currentScreen == Screen.CARRITO,
                            onClick = { navigateTo(Screen.CARRITO) },
                            icon = {
                                val totalQty = cartItems.sumOf { it.quantity }
                                if (totalQty > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = EleganBurgundy,
                                                contentColor = Color.White
                                            ) {
                                                Text("$totalQty", fontSize = 9.sp)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ShoppingBag, contentDescription = "Carrito", modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    Icon(Icons.Default.ShoppingBag, contentDescription = "Carrito", modifier = Modifier.size(20.dp))
                                }
                            },
                            label = { Text("Carrito", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EleganBurgundy,
                                selectedTextColor = EleganBurgundy,
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("nav_carrito")
                        )

                        NavigationBarItem(
                            selected = currentScreen == Screen.CHATS,
                            onClick = { navigateTo(Screen.CHATS) },
                            icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Soporte", modifier = Modifier.size(20.dp)) },
                            label = { Text("Soporte", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EleganBurgundy,
                                selectedTextColor = EleganBurgundy,
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("nav_chats")
                        )

                        NavigationBarItem(
                            selected = currentScreen == Screen.PERFIL,
                            onClick = { navigateTo(Screen.PERFIL) },
                            icon = { Icon(Icons.Default.PersonOutline, contentDescription = "Perfil", modifier = Modifier.size(20.dp)) },
                            label = { Text("Perfil", fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EleganBurgundy,
                                selectedTextColor = EleganBurgundy,
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("nav_perfil")
                        )
                        }
                    }
                }
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            when (currentScreen) {
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
                        }
                    )
                }

                Screen.PRODUCTO -> {
                    val prod = selectedProduct ?: products.firstOrNull()
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
                            onBack = { navigateTo(previousScreen) }
                        )
                    } else {
                        navigateTo(Screen.CATALOGO)
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
                        onBack = { navigateTo(previousScreen) }
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
                        onConfirmOrder = { name, email, phone, addr, city, payMethod ->
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
                        onNavigateToInicio = { navigateTo(Screen.INICIO) },
                        onNavigateToPerfil = { navigateTo(Screen.PERFIL) },
                        onNavigateToChats = { navigateTo(Screen.CHATS) }
                    )
                }

                Screen.PERFIL -> {
                    PerfilScreen(
                        userProfile = userProfile,
                        orders = orders,
                        savedProducts = savedProducts,
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

                Screen.CHATS -> {
                    ChatsScreen(
                        messages = messages,
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
            }
        }
    }
}
