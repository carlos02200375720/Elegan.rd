package com.example

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
    CHATS
}

class MainActivity : ComponentActivity() {
    private var isAppReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep native Android splash screen on screen until app has initialized
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (isAppReady) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(0xFFFAF7F2.toInt())
        )
        window.navigationBarColor = 0xFFFAF7F2.toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightNavigationBars = true
        setContent {
            EleganTheme {
                MainApp(onAppReady = { isAppReady = true })
            }
        }
    }
}

@Composable
fun MainApp(onAppReady: () -> Unit) {
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
                    onAppReady()
                },
                onUnauthenticated = {
                    onAppReady()
                },
                scope = coroutineScope
            )
        } else {
            onAppReady()
        }
    }

    val activity = context as? Activity
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
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
    val cartItems by viewModel.cartItems.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
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
        Screen.CARRITO,
        Screen.CHATS,
        Screen.PERFIL
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.background(Color(0xFFFAF7F2)),
                    containerColor = Color(0xFFFAF7F2),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.INICIO,
                        onClick = { navigateTo(Screen.INICIO) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                        label = { Text("Inicio", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EleganBurgundy,
                            selectedTextColor = EleganBurgundy,
                            indicatorColor = Color(0xFFFAF7F2)
                        ),
                        modifier = Modifier.testTag("nav_inicio")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.CATALOGO,
                        onClick = {
                            viewModel.setCategory("todo")
                            navigateTo(Screen.CATALOGO)
                        },
                        icon = { Icon(Icons.Default.GridView, contentDescription = "Catálogo") },
                        label = { Text("Catálogo", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EleganBurgundy,
                            selectedTextColor = EleganBurgundy,
                            indicatorColor = Color(0xFFFAF7F2)
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
                                            Text("$totalQty")
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ShoppingBag, contentDescription = "Carrito")
                                }
                            } else {
                                Icon(Icons.Default.ShoppingBag, contentDescription = "Carrito")
                            }
                        },
                        label = { Text("Carrito", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EleganBurgundy,
                            selectedTextColor = EleganBurgundy,
                            indicatorColor = Color(0xFFFAF7F2)
                        ),
                        modifier = Modifier.testTag("nav_carrito")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.CHATS,
                        onClick = { navigateTo(Screen.CHATS) },
                        icon = { Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Soporte") },
                        label = { Text("Soporte", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EleganBurgundy,
                            selectedTextColor = EleganBurgundy,
                            indicatorColor = Color(0xFFFAF7F2)
                        ),
                        modifier = Modifier.testTag("nav_chats")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.PERFIL,
                        onClick = { navigateTo(Screen.PERFIL) },
                        icon = { Icon(Icons.Default.PersonOutline, contentDescription = "Perfil") },
                        label = { Text("Perfil", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EleganBurgundy,
                            selectedTextColor = EleganBurgundy,
                            indicatorColor = Color(0xFFFAF7F2)
                        ),
                        modifier = Modifier.testTag("nav_perfil")
                    )
                }
            }
        },
        containerColor = EleganIvory
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.INICIO -> {
                    InicioScreen(
                        featuredProducts = products,
                        onNavigateToCatalogo = { cat ->
                            viewModel.setCategory(cat)
                            navigateTo(Screen.CATALOGO)
                        },
                        onNavigateToProduct = { prod ->
                            viewModel.selectProduct(prod)
                            navigateTo(Screen.PRODUCTO)
                        },
                        onNavigateToMenu = { navigateTo(Screen.MENU) },
                        onNavigateToChats = { navigateTo(Screen.CHATS) }
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
                            viewModel.addToCart(prod, defaultSize, 1L)
                        }
                    )
                }

                Screen.PRODUCTO -> {
                    val prod = selectedProduct ?: products.firstOrNull()
                    if (prod != null) {
                        ProductoScreen(
                            product = prod,
                            onAddToCart = { p, s, q ->
                                viewModel.addToCart(p, s, q)
                            },
                            onBuyNow = { p, s, q ->
                                viewModel.addToCart(p, s, q)
                                navigateTo(Screen.CARRITO)
                            },
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
                        }
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
                        onSaveProfile = { ph, ad, ct -> viewModel.saveProfile(ph, ad, ct) },
                        onNavigateToChats = { navigateTo(Screen.CHATS) },
                        onSignOut = onSignOut
                    )
                }

                Screen.CHATS -> {
                    ChatsScreen(
                        messages = messages,
                        onSendMessage = { text -> viewModel.sendSupportMessage(text) }
                    )
                }
            }
        }
    }
}
