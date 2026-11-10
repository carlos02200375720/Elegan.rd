package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.model.*
import com.example.data.remote.*
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.launch

class EleganRepository(private val context: Context) {
    private val appContext = context.applicationContext
    private val api get() = EleganApiClient.store

    init {
        // Erradicación definitiva de almacenamiento local de contenido multimedia y datos locales
        try {
            val mediaDir = File(appContext.filesDir, "product_media")
            if (mediaDir.exists()) {
                mediaDir.deleteRecursively()
            }
            context.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply()
        } catch (_: Exception) {}
    }

    private fun requireUserId(): String =
        MobileSession.userId(appContext) ?: throw IllegalStateException("Debes iniciar sesión para realizar esta operación.")

    private fun optionalUserId(): String? = MobileSession.userId(appContext)

    private fun requireToken(): String =
        MobileSession.bearerToken(appContext) ?: throw IllegalStateException("Tu sesión no está activa. Inicia sesión nuevamente.")

    private fun optionalToken(): String? = MobileSession.bearerToken(appContext)

    private val localCommentsCache = java.util.concurrent.ConcurrentHashMap<String, MutableList<PublicationComment>>()
    private val localStatsMap = java.util.concurrent.ConcurrentHashMap<String, PublicationStats>()

    // -------------------------------------------------------------
    // PRODUCTS
    // -------------------------------------------------------------

    fun observeProducts(): Flow<List<Product>> = flow {
        val samples = getInitialSampleProducts()
        emit(samples)
        while (true) {
            try {
                val remote = api.products().items
                emit(if (remote.isEmpty()) samples else remote)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudo actualizar el catálogo: ${e.message}") }
            delay(15000)
        }
    }

    suspend fun seedProductsIfEmpty() {
        // Los productos de muestra son un respaldo; no se insertan automáticamente en MongoDB.
    }

    suspend fun seedAnnouncementsIfEmpty() {
        // Los anuncios de muestra son un respaldo; no se insertan automáticamente en MongoDB.
    }

    suspend fun getProductById(productId: String): Product? = try {
        api.product(productId).item ?: getInitialSampleProducts().find { it.id == productId }
    } catch (e: Exception) {
        getInitialSampleProducts().find { it.id == productId }
    }

    private fun getFallbackCoverForCategory(category: String): String {
        return when (category.lowercase().trim()) {
            "camisa" -> "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800&auto=format&fit=crop&q=80"
            "gorra" -> "https://images.unsplash.com/photo-1588850561407-ed78c282e89b?w=800&auto=format&fit=crop&q=80"
            "gafa" -> "https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=800&auto=format&fit=crop&q=80"
            "correa" -> "https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=800&auto=format&fit=crop&q=80"
            "pantalone" -> "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800&auto=format&fit=crop&q=80"
            "zapato" -> "https://images.unsplash.com/photo-1614252235316-8c857d38b5f4?w=800&auto=format&fit=crop&q=80"
            "interiore" -> "https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?w=800&auto=format&fit=crop&q=80"
            "perfume" -> "https://images.unsplash.com/photo-1523293182086-7651a899d37f?w=800&auto=format&fit=crop&q=80"
            else -> "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800&auto=format&fit=crop&q=80"
        }
    }

    suspend fun addProduct(product: Product): String {
        val productId = product.id.ifBlank { "prod_${java.util.UUID.randomUUID()}" }
        val category = product.category.ifBlank { "camisa" }
        val uploaded = uploadProductMedia(product.copy(id = productId, category = category), productId)
        val finalProduct = uploaded.copy(
            id = productId,
            imageUrl = uploaded.imageUrl.ifBlank { getFallbackCoverForCategory(category) },
            category = category,
            isVideo = uploaded.hasVideo
        )
        val response = api.createProduct(requireToken(), finalProduct)
        return response.id.ifBlank { productId }
    }

    private suspend fun uploadProductMedia(product: Product, productId: String): Product {
        val sourceVideo = product.videoUrl.takeIf { product.isVideo }.orEmpty()
        val coverSource = if (product.isVideo) {
            if (product.imageUrl.isNotBlank() && product.imageUrl != sourceVideo) product.imageUrl
            else getFallbackCoverForCategory(product.category)
        } else product.imageUrl.ifBlank { product.images.firstOrNull() ?: getFallbackCoverForCategory(product.category) }

        val videoUrl = if (product.isVideo && sourceVideo.isNotBlank()) uploadProductFile(sourceVideo, productId, "video") else ""
        val coverUrl = uploadProductFile(coverSource, productId, "cover").ifBlank { getFallbackCoverForCategory(product.category) }
        val gallery = (product.images + listOfNotNull(product.imageUrl.takeIf { it.isNotBlank() }))
            .distinct().filterNot { it == coverSource || it == sourceVideo }
            .map { uploadProductFile(it, productId, "gallery") }.filter { it.isNotBlank() }
        val images = (listOf(coverUrl) + gallery).distinct()
        return product.copy(
            imageUrl = coverUrl,
            images = images,
            videoUrl = videoUrl.ifBlank { if (sourceVideo.startsWith("http")) sourceVideo else "" },
            isVideo = product.isVideo
        )
    }

    private suspend fun uploadProductFile(source: String, productId: String, label: String): String {
        if (source.isBlank()) return ""
        if (source.startsWith("https://") || source.startsWith("http://")) return source
        val uri = Uri.parse(source)
        val isVideo = label.contains("video") || source.endsWith(".mp4", true)
        val ext = if (isVideo) "mp4" else "jpg"
        val mime = if (isVideo) "video/mp4" else "image/jpeg"
        val fileName = "${productId}_${label}.$ext"

        val requestBody = object : RequestBody() {
            override fun contentType() = mime.toMediaTypeOrNull()

            override fun contentLength(): Long {
                return try {
                    if (uri.scheme == "content") {
                        appContext.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
                    } else if (uri.scheme == "file") {
                        File(uri.path ?: "").length()
                    } else -1L
                } catch (_: Exception) { -1L }
            }

            override fun writeTo(sink: okio.BufferedSink) {
                val input = when (uri.scheme) {
                    "content" -> appContext.contentResolver.openInputStream(uri)
                    "file" -> File(uri.path ?: "").inputStream()
                    else -> File(source).takeIf { it.exists() }?.inputStream()
                } ?: throw IllegalArgumentException("No se pudo leer el archivo multimedia seleccionado.")
                input.use { inputStream ->
                    val buffer = ByteArray(16384)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        sink.write(buffer, 0, bytesRead)
                    }
                }
            }
        }

        val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
        val name = "${productId}_${label}".toRequestBody("text/plain".toMediaTypeOrNull())
        return api.uploadMedia(requireToken(), part, name).url
    }

    suspend fun addAnnouncement(announcement: Announcement): String {
        val id = announcement.id.ifBlank { "ann_${java.util.UUID.randomUUID()}" }
        val image = if (announcement.imageUrl.startsWith("content://") || announcement.imageUrl.startsWith("file://"))
            uploadProductFile(announcement.imageUrl, id, "announcement_img")
        else announcement.imageUrl.ifBlank { getFallbackCoverForCategory(announcement.targetCategory) }
        val video = if (announcement.videoUrl.startsWith("content://") || announcement.videoUrl.startsWith("file://"))
            uploadProductFile(announcement.videoUrl, id, "announcement_video")
        else announcement.videoUrl
        val final = announcement.copy(id=id, imageUrl=image, videoUrl=video, isVideo=announcement.hasVideo || video.isNotBlank())
        api.createAnnouncement(requireToken(), final)
        return id
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        val samples = getInitialSampleAnnouncements()
        emit(samples)
        while (true) {
            try {
                val remote = api.announcements().items
                emit(if (remote.isEmpty()) samples else remote)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudieron actualizar los anuncios: ${e.message}") }
            delay(15000)
        }
    }

    suspend fun deleteProduct(productId: String) {
        api.deleteProduct(requireToken(), productId)
    }

    suspend fun toggleProductStock(productId: String, inStock: Boolean) {
        api.updateProduct(requireToken(), productId, mapOf("inStock" to inStock))
    }

    suspend fun deleteAnnouncement(announcementId: String) {
        api.deleteAnnouncement(requireToken(), announcementId)
    }

    suspend fun toggleAnnouncementActive(announcementId: String, isActive: Boolean) {
        api.updateAnnouncement(requireToken(), announcementId, mapOf("isActive" to isActive))
    }

    fun getInitialSampleAnnouncements(): List<Announcement> = listOf(
        Announcement(
            id = "ann_1",
            title = "Gran Venta Sartorial 2026",
            message = "Disfruta de hasta un 25% de descuento en trajes y camisas Oxford seleccionadas por tiempo limitado.",
            type = "descuento",
            imageUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800&auto=format&fit=crop&q=80",
            discountCode = "SARTORIAL25",
            discountPercent = 25.0,
            ctaText = "Ver Colección",
            targetCategory = "camisa",
            isActive = true
        ),
        Announcement(
            id = "ann_2",
            title = "Nueva Colección: Fragancias Noir",
            message = "Descubre nuestras esencias exclusivas destiladas para el caballero contemporáneo.",
            type = "banner",
            imageUrl = "https://images.unsplash.com/photo-1594035910387-fea47794261f?w=800&auto=format&fit=crop&q=80",
            discountCode = "",
            discountPercent = 0.0,
            ctaText = "Explorar Perfumes",
            targetCategory = "perfume",
            isActive = true
        ),
        Announcement(
            id = "ann_3",
            title = "Artesanía & Calzado Goodyear",
            message = "Zapatos hechos a mano con pieles italianas de la más alta distinción.",
            type = "coleccion",
            imageUrl = "https://images.unsplash.com/photo-1614252235316-8c857d38b5f4?w=800&auto=format&fit=crop&q=80",
            discountCode = "",
            discountPercent = 0.0,
            ctaText = "Ver Zapatos",
            targetCategory = "zapato",
            isActive = true
        )
    )

    // -------------------------------------------------------------
    // CART
    // -------------------------------------------------------------

    fun observeCart(): Flow<List<CartItem>> = flow {
        if (optionalToken() == null) { emit(emptyList()); return@flow }
        while (true) {
            try { emit(api.cart(requireToken()).items) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudo actualizar el carrito: ${e.message}") }
            delay(10000)
        }
    }

    fun observeSavedProductIds(): Flow<Set<String>> = flow {
        if (optionalToken() == null) { emit(emptySet()); return@flow }
        while (true) {
            try { emit(api.saved(requireToken()).ids.toSet()) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudieron cargar los guardados: ${e.message}") }
            delay(15000)
        }
    }

    suspend fun setProductSaved(productId: String, isSaved: Boolean) {
        require(productId.isNotBlank())
        api.setSaved(requireToken(), productId, SavedRequest(isSaved))
    }

    fun observePublicationStats(): Flow<Map<String, PublicationStats>> = flow {
        while (true) {
            val combined = localStatsMap.toMutableMap()
            try {
                val stats = api.publicationStats().stats.mapValues { (id, value) -> value.copy(id = id) }
                stats.forEach { (id, remoteStat) ->
                    val local = combined[id] ?: PublicationStats(id = id)
                    combined[id] = remoteStat.copy(
                        commentsCount = maxOf(remoteStat.commentsCount, local.commentsCount),
                        likesCount = maxOf(remoteStat.likesCount, local.likesCount),
                        sharesCount = maxOf(remoteStat.sharesCount, local.sharesCount),
                        savesCount = maxOf(remoteStat.savesCount, local.savesCount)
                    )
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudieron cargar estadísticas: ${e.message}") }
            emit(combined.toMap())
            delay(10000)
        }
    }

    fun observeLikedPublicationIds(): Flow<Set<String>> = flow {
        if (optionalToken() == null) { emit(emptySet()); return@flow }
        while (true) {
            try { emit(api.liked(requireToken()).ids.toSet()) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudieron cargar likes: ${e.message}") }
            delay(15000)
        }
    }

    suspend fun togglePublicationLike(productId: String): Boolean {
        require(productId.isNotBlank())
        val token = optionalToken()
        return if (token != null) {
            try {
                val liked = api.toggleLike(token, productId).liked
                val current = localStatsMap[productId] ?: PublicationStats(id = productId)
                val newLikes = if (liked) current.likesCount + 1 else (current.likesCount - 1).coerceAtLeast(0)
                localStatsMap[productId] = current.copy(likesCount = newLikes)
                liked
            } catch (e: Exception) {
                Log.w("EleganRepository", "Error en like remoto: ${e.message}")
                true
            }
        } else {
            true
        }
    }

    fun observePublicationComments(productId: String): Flow<List<PublicationComment>> = flow {
        require(productId.isNotBlank())
        val localList = localCommentsCache.computeIfAbsent(productId) { mutableListOf() }
        emit(localList.toList())

        var lastSync = 0L
        while (true) {
            val now = System.currentTimeMillis()
            if (now - lastSync >= 10000L) {
                try {
                    val remoteItems = api.comments(productId).items
                    if (remoteItems.isNotEmpty()) {
                        val merged = (localList + remoteItems).distinctBy { it.id }.sortedBy { it.createdAt ?: "" }
                        localList.clear()
                        localList.addAll(merged)
                    }
                    lastSync = now
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("EleganRepository", "No se pudieron sincronizar comentarios remotos: ${e.message}")
                }
            }
            emit(localList.toList())
            delay(2000)
        }
    }

    suspend fun addPublicationComment(productId: String, displayName: String, text: String) {
        val clean = text.trim()
        require(productId.isNotBlank() && clean.isNotEmpty() && clean.length <= 1000)

        val userId = optionalUserId() ?: "cliente_${java.util.UUID.randomUUID().toString().take(8)}"
        val finalName = displayName.trim().ifBlank { "Cliente" }.take(80)
        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())

        val newComment = PublicationComment(
            id = java.util.UUID.randomUUID().toString(),
            productId = productId,
            userId = userId,
            displayName = finalName,
            text = clean,
            createdAt = nowIso
        )

        // 1. Guardar de inmediato en la memoria local para despliegue instantáneo (Optimistic UI)
        val localList = localCommentsCache.computeIfAbsent(productId) { mutableListOf() }
        synchronized(localList) {
            localList.add(newComment)
        }

        // 2. Incrementar contador local de comentarios
        val currentStat = localStatsMap[productId] ?: PublicationStats(id = productId)
        localStatsMap[productId] = currentStat.copy(commentsCount = currentStat.commentsCount + 1)

        // 3. Sincronizar con el servidor en segundo plano
        val token = optionalToken() ?: "Bearer guest"
        try {
            api.addComment(token, productId, CommentRequest(finalName, clean))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("EleganRepository", "Comentario registrado localmente; servidor reportó: ${e.message}")
        }
    }

    suspend fun recordPublicationShare(productId: String) {
        require(productId.isNotBlank())
        val currentStat = localStatsMap[productId] ?: PublicationStats(id = productId)
        localStatsMap[productId] = currentStat.copy(sharesCount = currentStat.sharesCount + 1)
        val token = optionalToken() ?: "Bearer guest"
        try {
            api.share(token, productId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("EleganRepository", "Share registrado localmente; servidor reportó: ${e.message}")
        }
    }

    suspend fun addToCart(product: Product, selectedSize: String, quantity: Long = 1L, selectedColor: String = "") {
        require(product.id.isNotBlank() && quantity > 0)
        api.addCartItem(requireToken(), CartItem(
            productId = product.id, name = product.name, price = product.price,
            imageUrl = product.imageUrl, selectedSize = selectedSize,
            selectedColor = selectedColor, quantity = quantity
        ))
    }

    suspend fun updateCartQuantity(cartItemId: String, newQuantity: Long) {
        if (newQuantity <= 0) removeFromCart(cartItemId)
        else api.updateCartItem(requireToken(), cartItemId, QuantityRequest(newQuantity))
    }

    suspend fun removeFromCart(cartItemId: String) {
        api.removeCartItem(requireToken(), cartItemId)
    }

    suspend fun clearCart() {
        api.clearCart(requireToken())
    }

    fun observeOrders(): Flow<List<Order>> = flow {
        if (optionalToken() == null) { emit(emptyList()); return@flow }
        while (true) {
            try { emit(api.orders(requireToken()).items) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudieron cargar órdenes: ${e.message}") }
            delay(15000)
        }
    }

    suspend fun createOrder(
        customerName: String, email: String, phone: String, address: String,
        city: String, paymentMethod: String, total: Double, itemCount: Long, itemsSummary: String
    ): String {
        val order = Order(
            customerName = customerName, email = email, phone = phone, address = address,
            city = city, paymentMethod = paymentMethod, total = total,
            itemCount = itemCount, itemsSummary = itemsSummary
        )
        return api.createOrder(requireToken(), order).id
    }

    fun observeMessages(): Flow<List<SupportMessage>> = flow {
        if (optionalToken() == null) { emit(emptyList()); return@flow }
        val welcome = SupportMessage(
            id = "welcome", userId = requireUserId(), sender = "support",
            senderName = "Concierge Elegan",
            text = "¡Bienvenido a Elegan! Soy tu asesor personal de estilo y compras. ¿En qué podemos ayudarte hoy?"
        )
        while (true) {
            try {
                val items = api.messages(requireToken()).items
                emit(if (items.isEmpty()) listOf(welcome) else items)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudieron cargar mensajes: ${e.message}") }
            delay(10000)
        }
    }

    suspend fun sendSupportMessage(text: String, scope: CoroutineScope) {
        val clean = text.trim()
        require(clean.isNotEmpty() && clean.length <= 4000)
        api.sendMessage(requireToken(), MessageRequest(clean))
    }

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        if (optionalToken() == null) { emit(null); return@flow }
        while (true) {
            try { emit(api.profile(requireToken()).item) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudo cargar perfil: ${e.message}") }
            delay(15000)
        }
    }

    suspend fun saveUserProfile(phone: String, address: String, city: String) {
        val user = MobileSession.user(appContext)
        api.saveProfile(requireToken(), UserProfile(
            userId = requireUserId(),
            displayName = user?.name.orEmpty(),
            email = user?.email.orEmpty(),
            photoUrl = user?.photoUrl.orEmpty(),
            phone = phone, address = address, city = city
        ))
    }

    fun getInitialSampleProducts(): List<Product> = listOf(
        Product(
            id = "prod_gorra_1",
            name = "Boina Clásica Gatsby Peaky Tweed",
            description = "Confeccionada con lana británica de espiga en tono marrón carbón. Interior forrado en satén sedoso para una sensación inigualable.",
            category = "gorra",
            price = 55.00,
            imageUrl = "https://images.unsplash.com/photo-1576871337622-98d48d1cf531?w=800&auto=format&fit=crop&q=80",
            rating = 4.9,
            reviewCount = 142,
            sizes = listOf("S (56cm)", "M (58cm)", "L (60cm)", "XL (62cm)"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_gorra_2",
            name = "Gorra Newsboy Borgoña Signature",
            description = "Elegante gorra de 8 paneles en pana fina color vino intenso. Un homenaje al corte tradicional inglés con acabados a mano.",
            category = "gorra",
            price = 48.00,
            imageUrl = "https://images.unsplash.com/photo-1588850561407-ed78c282e89b?w=800&auto=format&fit=crop&q=80",
            rating = 4.8,
            reviewCount = 98,
            sizes = listOf("M (58cm)", "L (60cm)"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_gafa_1",
            name = "Gafas de Sol Aviador Gold Master",
            description = "Montura de titanio bañada en oro de 18k con lentes polarizadas verde bosque degradadas. Protección UV400 completa con estuche de cuero.",
            category = "gafa",
            price = 145.00,
            imageUrl = "https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=800&auto=format&fit=crop&q=80",
            rating = 5.0,
            reviewCount = 210,
            sizes = listOf("Única (54-18-145)"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_gafa_2",
            name = "Gafas Clubmaster Carey Vintage",
            description = "Acetato italiano pulido a mano con detalles metálicos plateados. Diseño atemporal para caballeros distinguidos.",
            category = "gafa",
            price = 120.00,
            imageUrl = "https://images.unsplash.com/photo-1508296695146-257a814070b4?w=800&auto=format&fit=crop&q=80",
            rating = 4.7,
            reviewCount = 85,
            sizes = listOf("Única (51-20-145)"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_camisa_1",
            name = "Camisa Oxford Blanca Algodón Egipcio",
            description = "Tejido dos cabos de algodón Giza 100%. Cuello cutaway reforzado y botones de nácar natural. Planchado antiarrugas permanente.",
            category = "camisa",
            price = 89.00,
            imageUrl = "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800&auto=format&fit=crop&q=80",
            rating = 4.9,
            reviewCount = 340,
            sizes = listOf("38 (S)", "40 (M)", "42 (L)", "44 (XL)", "46 (XXL)"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_camisa_2",
            name = "Blazer Sartorial Borgoña Imperial",
            description = "Chaqueta estructurada de sarga de lana peinada con solapa en punta de lanza y botones esmaltados. La insignia indiscutible de Elegan.",
            category = "camisa",
            price = 280.00,
            imageUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=800&auto=format&fit=crop&q=80",
            rating = 5.0,
            reviewCount = 115,
            sizes = listOf("48R", "50R", "52R", "54R"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_correa_1",
            name = "Correa de Cuero Vacuno Hebilla Dorada",
            description = "Piel de curtición vegetal teñida en anilina color coñac intenso. Hebilla de latón macizo cepillado de alta durabilidad.",
            category = "correa",
            price = 65.00,
            imageUrl = "https://images.unsplash.com/photo-1624222247344-550fb60583dc?w=800&auto=format&fit=crop&q=80",
            rating = 4.8,
            reviewCount = 160,
            sizes = listOf("85cm", "90cm", "95cm", "100cm", "105cm"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_correa_2",
            name = "Cinturón Reversible Negro & Borgoña",
            description = "Mecanismo giratorio suizo que permite alternar entre cuero negro caviar texturizado y borgoña liso satinado.",
            category = "correa",
            price = 75.00,
            imageUrl = "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&auto=format&fit=crop&q=80",
            rating = 4.9,
            reviewCount = 130,
            sizes = listOf("90cm", "95cm", "100cm", "105cm"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_pantalone_1",
            name = "Pantalón de Vestir Plisado Gris Carbón",
            description = "Corte sartorial con doble pliegue frontal, ajustadores laterales sin necesidad de cinturón y caída recta impecable.",
            category = "pantalone",
            price = 110.00,
            imageUrl = "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800&auto=format&fit=crop&q=80",
            rating = 4.9,
            reviewCount = 192,
            sizes = listOf("30", "32", "34", "36", "38"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_pantalone_2",
            name = "Pantalón Chino Premium Arena",
            description = "Algodón stretch de 280g con tacto melocotón. Ideal tanto para ocasiones de negocios informales como para fines de semana elegantes.",
            category = "pantalone",
            price = 85.00,
            imageUrl = "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=800&auto=format&fit=crop&q=80",
            rating = 4.7,
            reviewCount = 94,
            sizes = listOf("30", "32", "34", "36"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_zapato_1",
            name = "Zapatos Oxford Cuero Glaseado Cap-Toe",
            description = "Construcción Goodyear welted con suela de cuero apomazado. Piel de becerro de grano fino con acabado espejo en la puntera.",
            category = "zapato",
            price = 240.00,
            imageUrl = "https://images.unsplash.com/photo-1614252235316-8c857d38b5f4?w=800&auto=format&fit=crop&q=80",
            rating = 5.0,
            reviewCount = 280,
            sizes = listOf("40", "41", "42", "43", "44", "45"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_zapato_2",
            name = "Zapatos Derby Brogue Borgoña Antik",
            description = "Perforado brogue wingtip artesanal en tono burdeos envejecido. Plantilla con amortiguación de corcho natural transpirable.",
            category = "zapato",
            price = 220.00,
            imageUrl = "https://images.unsplash.com/photo-1533867617858-e7b97e060509?w=800&auto=format&fit=crop&q=80",
            rating = 4.9,
            reviewCount = 175,
            sizes = listOf("41", "42", "43", "44"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_interiore_1",
            name = "Pack 3 Bóxers Modal Seda Elegan",
            description = "Micro-modal ultra suave con pretina elástica forrada en algodón que no marca ni oprime. Máxima frescura y elegancia interior.",
            category = "interiore",
            price = 45.00,
            imageUrl = "https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?w=800&auto=format&fit=crop&q=80",
            rating = 4.8,
            reviewCount = 150,
            sizes = listOf("S", "M", "L", "XL"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_perfume_1",
            name = "Elegan Noir Eau de Parfum 100ml",
            description = "Notas de salida de bergamota y cardamomo, corazón de cedro del Atlas y fondo profundo de ámbar gris y cuero ahumado.",
            category = "perfume",
            price = 135.00,
            imageUrl = "https://images.unsplash.com/photo-1594035910387-fea47794261f?w=800&auto=format&fit=crop&q=80",
            rating = 5.0,
            reviewCount = 410,
            sizes = listOf("100ml Spray"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_perfume_2",
            name = "Gentleman Reserve Extrait de Parfum",
            description = "Alta concentración con notas de tabaco rubio, vainilla bourbon y maderas nobles. Duración superior a 14 horas en piel.",
            category = "perfume",
            price = 165.00,
            imageUrl = "https://images.unsplash.com/photo-1523293182086-7651a899d37f?w=800&auto=format&fit=crop&q=80",
            rating = 4.9,
            reviewCount = 225,
            sizes = listOf("50ml Extrait"),
            inStock = true,
            isFeatured = true
        )
    )
}
