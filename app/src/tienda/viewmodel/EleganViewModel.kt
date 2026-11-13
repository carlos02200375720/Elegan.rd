package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Announcement
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import com.example.data.repository.EleganRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

class EleganViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EleganRepository(application)

    private val _selectedCategory = MutableStateFlow<String>("todo")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _lastCreatedOrderId = MutableStateFlow<String?>(null)
    val lastCreatedOrderId: StateFlow<String?> = _lastCreatedOrderId.asStateFlow()

    private val _lastOrderPaymentMethod = MutableStateFlow("")
    val lastOrderPaymentMethod: StateFlow<String> = _lastOrderPaymentMethod.asStateFlow()

    private val _lastOrderDetails = MutableStateFlow<OrderConfirmationDetails?>(null)
    val lastOrderDetails: StateFlow<OrderConfirmationDetails?> = _lastOrderDetails.asStateFlow()

    private val _searchQuery = MutableStateFlow<String>("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _deletedProductIds = MutableStateFlow<Set<String>>(emptySet())
    private val _deletedAnnouncementIds = MutableStateFlow<Set<String>>(emptySet())

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

    val cartItems: StateFlow<List<CartItem>> = repository.observeCart()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val savedProductIds: StateFlow<Set<String>> = repository.observeSavedProductIds()
        .catch { error ->
            Log.e("EleganViewModel", "No se pudieron cargar las publicaciones guardadas.", error)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet()
        )

    val savedProducts: StateFlow<List<Product>> = combine(
        products,
        savedProductIds
    ) { allProducts, savedIds ->
        allProducts.filter { it.id in savedIds }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val likedPublicationIds: StateFlow<Set<String>> =
        repository.observeLikedPublicationIds()
            .catch { error ->
                Log.e("EleganViewModel", "No se pudieron cargar los likes de publicaciones.", error)
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptySet()
            )

    val publicationStats: StateFlow<Map<String, PublicationStats>> =
        combine(
            repository.observePublicationStats(),
            savedProductIds,
            likedPublicationIds
        ) { statsMap, savedIds, likedIds ->
            if (savedIds.isEmpty() && likedIds.isEmpty()) {
                statsMap
            } else {
                val adjusted = statsMap.toMutableMap()
                savedIds.forEach { productId ->
                    val existing = adjusted[productId] ?: PublicationStats(id = productId)
                    if (existing.savesCount < 1L) {
                        adjusted[productId] = existing.copy(savesCount = 1L)
                    }
                }
                likedIds.forEach { productId ->
                    val existing = adjusted[productId] ?: PublicationStats(id = productId)
                    if (existing.likesCount < 1L) {
                        adjusted[productId] = existing.copy(likesCount = 1L)
                    }
                }
                adjusted
            }
        }
            .catch { error ->
                Log.e("EleganViewModel", "No se pudieron cargar las estadísticas de publicaciones.", error)
                emit(emptyMap())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyMap()
            )

    private val _commentsProductId = MutableStateFlow<String?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val publicationComments: StateFlow<List<PublicationComment>> =
        _commentsProductId
            .flatMapLatest { productId ->
                if (productId == null) flowOf(emptyList())
                else repository.observePublicationComments(productId)
            }
            .catch { error ->
                Log.e("EleganViewModel", "No se pudieron cargar los comentarios de la publicación.", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    val orders: StateFlow<List<Order>> = repository.observeOrders()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val messages: StateFlow<List<SupportMessage>> = repository.observeMessages()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile()
        .catch { emit(null) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            repository.seedProductsIfEmpty()
            repository.seedAnnouncementsIfEmpty()
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

    fun toggleSaveProduct(
        productId: String,
        onResult: (Result<Boolean>) -> Unit = {}
    ) {
        val shouldSave = productId !in savedProductIds.value
        viewModelScope.launch {
            try {
                repository.setProductSaved(productId, shouldSave)
                onResult(Result.success(shouldSave))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudo actualizar la publicación guardada.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun togglePublicationLike(
        productId: String,
        onResult: (Result<Boolean>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                onResult(Result.success(repository.togglePublicationLike(productId)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudo actualizar el like de la publicación.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun openPublicationComments(productId: String?) {
        _commentsProductId.value = productId
    }

    fun addPublicationComment(
        productId: String,
        displayName: String,
        text: String,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.addPublicationComment(productId, displayName, text)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudo publicar el comentario.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun recordPublicationShare(
        productId: String,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.recordPublicationShare(productId)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudo registrar el compartido de la publicación.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun addToCart(product: Product, size: String, quantity: Long = 1L, color: String = "") {
        viewModelScope.launch {
            try {
                repository.addToCart(product, size, quantity, color)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al agregar al carrito: ${e.message}")
            }
        }
    }

    fun updateCartQuantity(cartItemId: String, quantity: Long) {
        viewModelScope.launch {
            try {
                repository.updateCartQuantity(cartItemId, quantity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al actualizar cantidad: ${e.message}")
            }
        }
    }

    fun removeFromCart(cartItemId: String) {
        viewModelScope.launch {
            try {
                repository.removeFromCart(cartItemId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al eliminar del carrito: ${e.message}")
            }
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            try {
                repository.clearCart()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al vaciar el carrito: ${e.message}")
            }
        }
    }

    fun setProductSaved(
        productId: String,
        isSaved: Boolean,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.setProductSaved(productId, isSaved)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudo actualizar la publicación guardada.", e)
                onResult(Result.failure(e))
            }
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
        paymentProofUri: String = "",
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val purchasedItemsSnapshot = cartItems.value.toList()
                val orderId = repository.createOrder(
                    customerName = customerName,
                    email = email,
                    phone = phone,
                    address = address,
                    city = city,
                    paymentMethod = paymentMethod,
                    total = total,
                    itemCount = itemCount,
                    itemsSummary = itemsSummary,
                    paymentProofUri = paymentProofUri
                )
                _lastOrderDetails.value = OrderConfirmationDetails(
                    orderId = orderId,
                    items = purchasedItemsSnapshot,
                    total = total,
                    paymentMethod = paymentMethod,
                    customerName = customerName,
                    phone = phone,
                    address = address,
                    city = city
                )
                _lastOrderPaymentMethod.value = paymentMethod
                _lastCreatedOrderId.value = orderId
                onSuccess(orderId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al procesar la orden: ${e.message}")
            }
        }
    }

    fun sendSupportMessage(text: String, mediaUri: String = "", mediaType: String = "") {
        if (text.isBlank() && mediaUri.isBlank()) return
        viewModelScope.launch {
            try {
                repository.sendSupportMessage(text, mediaUri, mediaType, viewModelScope)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al enviar mensaje: ${e.message}")
            }
        }
    }

    fun saveProfile(phone: String, address: String, city: String) {
        viewModelScope.launch {
            try {
                repository.saveUserProfile(phone, address, city)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al guardar perfil: ${e.message}")
            }
        }
    }

    fun publishProduct(product: Product, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addProduct(product)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudo publicar el producto.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun publishAnnouncement(announcement: Announcement, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addAnnouncement(announcement)
                onResult(true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
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
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun toggleProductStock(productId: String, inStock: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleProductStock(productId, inStock)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun deleteAnnouncement(announcementId: String, onResult: (Boolean) -> Unit = {}) {
        _deletedAnnouncementIds.value = _deletedAnnouncementIds.value + announcementId
        viewModelScope.launch {
            try {
                repository.deleteAnnouncement(announcementId)
                onResult(true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun toggleAnnouncementActive(announcementId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleAnnouncementActive(announcementId, isActive)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }
}

data class OrderConfirmationDetails(
    val orderId: String,
    val items: List<CartItem>,
    val total: Double,
    val paymentMethod: String,
    val customerName: String,
    val phone: String,
    val address: String,
    val city: String
)
