package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Announcement
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.StoreCategory
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import com.example.data.repository.EleganRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EleganViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EleganRepository(application)

    // Current navigation or filter state
    private val _selectedCategory = MutableStateFlow<String>("todo")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _lastCreatedOrderId = MutableStateFlow<String?>(null)
    val lastCreatedOrderId: StateFlow<String?> = _lastCreatedOrderId.asStateFlow()

    private val _searchQuery = MutableStateFlow<String>("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _deletedProductIds = MutableStateFlow<Set<String>>(emptySet())
    private val _deletedAnnouncementIds = MutableStateFlow<Set<String>>(emptySet())

    // Products
    val products: StateFlow<List<Product>> = combine(
        repository.observeProducts(),
        _deletedProductIds
    ) { list, deletedIds ->
        list.filter { it.id !in deletedIds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = repository.getInitialSampleProducts()
    )

    // Announcements
    val announcements: StateFlow<List<Announcement>> = combine(
        repository.observeAnnouncements(),
        _deletedAnnouncementIds
    ) { list, deletedIds ->
        list.filter { it.id !in deletedIds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = repository.getInitialSampleAnnouncements()
    )

    // Cart
    val cartItems: StateFlow<List<CartItem>> = repository.observeCart()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Orders
    val orders: StateFlow<List<Order>> = repository.observeOrders()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Support Chat Messages
    val messages: StateFlow<List<SupportMessage>> = repository.observeMessages()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // User Profile
    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile()
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    init {
        // Automatically seed products and announcements to Firestore if empty
        viewModelScope.launch {
            if (Firebase.auth.currentUser != null) {
                repository.seedProductsIfEmpty()
                repository.seedAnnouncementsIfEmpty()
            }
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectProduct(product: Product) {
        _selectedProduct.value = product
    }

    fun addToCart(product: Product, size: String, quantity: Long = 1L) {
        viewModelScope.launch {
            repository.addToCart(product, size, quantity)
        }
    }

    fun updateCartQuantity(cartItemId: String, quantity: Long) {
        viewModelScope.launch {
            repository.updateCartQuantity(cartItemId, quantity)
        }
    }

    fun removeFromCart(cartItemId: String) {
        viewModelScope.launch {
            repository.removeFromCart(cartItemId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    fun placeOrder(
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        paymentMethod: String,
        total: Double,
        itemCount: Long,
        itemsSummary: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val orderId = repository.createOrder(
                    customerName = customerName,
                    email = email,
                    phone = phone,
                    address = address,
                    city = city,
                    paymentMethod = paymentMethod,
                    total = total,
                    itemCount = itemCount,
                    itemsSummary = itemsSummary
                )
                _lastCreatedOrderId.value = orderId
                onSuccess(orderId)
            } catch (e: Exception) {
                // Structured logged in repository
            }
        }
    }

    fun sendSupportMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendSupportMessage(text, viewModelScope)
        }
    }

    fun saveProfile(phone: String, address: String, city: String) {
        viewModelScope.launch {
            repository.saveUserProfile(phone, address, city)
        }
    }

    fun publishProduct(product: Product, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addProduct(product)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun publishAnnouncement(announcement: Announcement, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addAnnouncement(announcement)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun deleteProduct(productId: String, onResult: (Boolean) -> Unit = {}) {
        _deletedProductIds.value = _deletedProductIds.value + productId
        viewModelScope.launch {
            try {
                repository.deleteProduct(productId)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun toggleProductStock(productId: String, inStock: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleProductStock(productId, inStock)
            } catch (e: Exception) {
                // Logged in repository
            }
        }
    }

    fun deleteAnnouncement(announcementId: String, onResult: (Boolean) -> Unit = {}) {
        _deletedAnnouncementIds.value = _deletedAnnouncementIds.value + announcementId
        viewModelScope.launch {
            try {
                repository.deleteAnnouncement(announcementId)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun toggleAnnouncementActive(announcementId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleAnnouncementActive(announcementId, isActive)
            } catch (e: Exception) {
                // Logged in repository
            }
        }
    }
}
