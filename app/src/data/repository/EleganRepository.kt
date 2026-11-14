package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.*
import com.example.data.remote.*
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

class EleganRepository(context: Context) {
    private val appContext = context.applicationContext
    private val api get() = EleganApiClient.store

    private fun optionalUserId(): String? = MobileSession.userId(appContext)

    private fun remoteToken(): String? = MobileSession.remoteBearerToken(appContext)

    private fun requireRemoteToken(): String =
        remoteToken() ?: throw IllegalStateException("Tu sesión remota no está activa. Inicia sesión con tu cuenta para continuar.")

    private val cartPrefs by lazy {
        appContext.getSharedPreferences("elegan_cart_cache", Context.MODE_PRIVATE)
    }
    private val savedPrefs by lazy {
        appContext.getSharedPreferences("elegan_saved_cache", Context.MODE_PRIVATE)
    }
    private val statsPrefs by lazy {
        appContext.getSharedPreferences("elegan_stats_cache", Context.MODE_PRIVATE)
    }
    private val likesPrefs by lazy {
        appContext.getSharedPreferences("elegan_likes_cache", Context.MODE_PRIVATE)
    }
    private val ordersPrefs by lazy {
        appContext.getSharedPreferences("elegan_orders_cache", Context.MODE_PRIVATE)
    }
    private val publicationStatePrefs by lazy {
        appContext.getSharedPreferences("elegan_publication_states", Context.MODE_PRIVATE)
    }
    private val productStockOverridesFlow by lazy {
        MutableStateFlow(loadBooleanOverrideMap("product_stock_overrides"))
    }
    private val announcementActiveOverridesFlow by lazy {
        MutableStateFlow(loadBooleanOverrideMap("announcement_active_overrides"))
    }
    private val cartLoadMutex = Mutex()
    private val savedLoadMutex = Mutex()
    private val statsLoadMutex = Mutex()
    private val likesLoadMutex = Mutex()
    private val ordersLoadMutex = Mutex()
    @Volatile
    private var loadedCartKey: String? = null
    @Volatile
    private var loadedSavedKey: String? = null
    @Volatile
    private var loadedStatsKey: String? = null
    @Volatile
    private var loadedLikesKey: String? = null
    @Volatile
    private var loadedOrdersKey: String? = null

    private val localCommentsCache = java.util.concurrent.ConcurrentHashMap<String, MutableList<PublicationComment>>()
    private val localCommentsUpdated = MutableSharedFlow<String>(extraBufferCapacity = 32)
    private val localStatsMap = java.util.concurrent.ConcurrentHashMap<String, PublicationStats>()
    private val localStatsFlow = MutableStateFlow<Map<String, PublicationStats>>(emptyMap())
    private val localCartItems = java.util.concurrent.CopyOnWriteArrayList<CartItem>()
    private val localCartFlow = MutableStateFlow<List<CartItem>>(emptyList())
    private val localSavedProductIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val localSavedFlow = MutableStateFlow<Set<String>>(emptySet())
    private val pendingSavedOps = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private val localLikedPublicationIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val localLikedFlow = MutableStateFlow<Set<String>>(emptySet())
    private val pendingLikeOps = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private val localOrdersList = java.util.concurrent.CopyOnWriteArrayList<Order>()
    private val localOrdersFlow = MutableStateFlow<List<Order>>(emptyList())
    private val pendingOrderIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val localMessagesList = java.util.concurrent.CopyOnWriteArrayList<SupportMessage>()
    private val localMessagesFlow = MutableStateFlow<List<SupportMessage>>(emptyList())
    private val messagesPrefs by lazy {
        appContext.getSharedPreferences("elegan_support_messages", Context.MODE_PRIVATE)
    }
    private val messagesLoadMutex = Mutex()
    @Volatile
    private var loadedMessagesKey: String? = null
    private val loggedRemoteErrors = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private fun clearRemoteError(key: String) {
        loggedRemoteErrors.remove(key)
        loggedRemoteErrors.remove("${key}_401")
    }

    private fun handleRemoteError(key: String, label: String, e: Exception) {
        if (e is CancellationException) throw e
        if (e is retrofit2.HttpException && e.code() == 401) {
            MobileSession.markRemoteSessionRejected()
            if (loggedRemoteErrors.add("${key}_401")) {
                Log.i("EleganRepository", "$label: sesión remota expirada o local (401), operando en modo local.")
            }
            return
        }
        if (loggedRemoteErrors.add(key)) {
            Log.w("EleganRepository", "$label: ${e.message}")
        }
    }

    private fun userCacheKey(): String {
        val user = MobileSession.user(appContext)
        val email = user?.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        val id = user?.effectiveId?.trim()?.takeIf { it.isNotBlank() }
        return email ?: id ?: "guest"
    }

    private fun cartCacheKey(): String = "cart_items_${userCacheKey()}"
    private fun savedCacheKey(userKey: String = userCacheKey()): String = "saved_ids_$userKey"
    private fun pendingSavedCacheKey(userKey: String = userCacheKey()): String = "pending_saved_$userKey"
    private fun likesCacheKey(userKey: String = userCacheKey()): String = "liked_ids_$userKey"
    private fun pendingLikesCacheKey(userKey: String = userCacheKey()): String = "pending_likes_$userKey"
    private fun ordersCacheKey(userKey: String = userCacheKey()): String = "orders_list_$userKey"
    private fun pendingOrdersCacheKey(userKey: String = userCacheKey()): String = "pending_orders_$userKey"
    private fun statsCacheKey(userKey: String = userCacheKey()): String = "publication_stats_$userKey"

    private suspend fun ensureCartLoaded() {
        val currentKey = userCacheKey()
        if (loadedCartKey == currentKey) return
        cartLoadMutex.withLock {
            if (loadedCartKey == currentKey) return
            val loaded = withContext(Dispatchers.IO) { loadPersistedCart() }
            localCartItems.clear()
            if (loaded.isNotEmpty()) {
                localCartItems.addAll(loaded)
            }
            localCartFlow.value = localCartItems.toList()
            loadedCartKey = currentKey
        }
    }

    private suspend fun ensureSavedLoaded() {
        val currentKey = userCacheKey()
        if (loadedSavedKey == currentKey) return
        savedLoadMutex.withLock {
            if (loadedSavedKey == currentKey) return
            val (loadedIds, loadedPending) = withContext(Dispatchers.IO) {
                loadPersistedSavedState(currentKey)
            }
            localSavedProductIds.clear()
            localSavedProductIds.addAll(loadedIds)
            pendingSavedOps.clear()
            pendingSavedOps.putAll(loadedPending)
            localSavedFlow.value = localSavedProductIds.toSet()
            loadedSavedKey = currentKey
        }
    }

    private fun loadPersistedSavedState(userKey: String): Pair<Set<String>, Map<String, Boolean>> {
        val ids = mutableSetOf<String>()
        val pending = mutableMapOf<String, Boolean>()
        try {
            val rawIds = savedPrefs.getString(savedCacheKey(userKey), null)
            if (!rawIds.isNullOrBlank()) {
                val array = JSONArray(rawIds)
                for (i in 0 until array.length()) {
                    val id = array.optString(i, "").trim()
                    if (id.isNotBlank()) ids.add(id)
                }
            }
            val rawPending = savedPrefs.getString(pendingSavedCacheKey(userKey), null)
            if (!rawPending.isNullOrBlank()) {
                val obj = JSONObject(rawPending)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k.isNotBlank()) pending[k] = obj.optBoolean(k, true)
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los productos guardados localmente: ${e.message}")
        }
        return ids to pending
    }

    private suspend fun savePersistedSavedState(userKey: String = userCacheKey()) {
        val idsSnapshot = localSavedProductIds.toList()
        val pendingSnapshot = pendingSavedOps.toMap()
        withContext(Dispatchers.IO) {
            try {
                val idsArray = JSONArray()
                idsSnapshot.forEach { idsArray.put(it) }
                val pendingObj = JSONObject()
                pendingSnapshot.forEach { (k, v) -> pendingObj.put(k, v) }
                savedPrefs.edit()
                    .putString(savedCacheKey(userKey), idsArray.toString())
                    .putString(pendingSavedCacheKey(userKey), pendingObj.toString())
                    .commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar los productos localmente: ${e.message}")
            }
        }
    }

    private suspend fun ensureLikesLoaded() {
        val currentKey = userCacheKey()
        if (loadedLikesKey == currentKey) return
        likesLoadMutex.withLock {
            if (loadedLikesKey == currentKey) return
            val (loadedIds, loadedPending) = withContext(Dispatchers.IO) {
                loadPersistedLikesState(currentKey)
            }
            localLikedPublicationIds.clear()
            localLikedPublicationIds.addAll(loadedIds)
            pendingLikeOps.clear()
            pendingLikeOps.putAll(loadedPending)
            localLikedFlow.value = localLikedPublicationIds.toSet()
            loadedLikesKey = currentKey
        }
    }

    private fun loadPersistedLikesState(userKey: String): Pair<Set<String>, Map<String, Boolean>> {
        val ids = mutableSetOf<String>()
        val pending = mutableMapOf<String, Boolean>()
        try {
            val rawIds = likesPrefs.getString(likesCacheKey(userKey), null)
            if (!rawIds.isNullOrBlank()) {
                val array = JSONArray(rawIds)
                for (i in 0 until array.length()) {
                    val id = array.optString(i, "").trim()
                    if (id.isNotBlank()) ids.add(id)
                }
            }
            val rawPending = likesPrefs.getString(pendingLikesCacheKey(userKey), null)
            if (!rawPending.isNullOrBlank()) {
                val obj = JSONObject(rawPending)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (k.isNotBlank()) pending[k] = obj.optBoolean(k, true)
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los likes locales: ${e.message}")
        }
        return ids to pending
    }

    private suspend fun savePersistedLikesState(userKey: String = userCacheKey()) {
        val idsSnapshot = localLikedPublicationIds.toList()
        val pendingSnapshot = pendingLikeOps.toMap()
        withContext(Dispatchers.IO) {
            try {
                val idsArray = JSONArray()
                idsSnapshot.forEach { idsArray.put(it) }
                val pendingObj = JSONObject()
                pendingSnapshot.forEach { (k, v) -> pendingObj.put(k, v) }
                likesPrefs.edit()
                    .putString(likesCacheKey(userKey), idsArray.toString())
                    .putString(pendingLikesCacheKey(userKey), pendingObj.toString())
                    .commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar los likes localmente: ${e.message}")
            }
        }
    }

    private suspend fun ensureStatsLoaded() {
        val currentKey = userCacheKey()
        if (loadedStatsKey == currentKey) return
        statsLoadMutex.withLock {
            if (loadedStatsKey == currentKey) return
            val loaded = withContext(Dispatchers.IO) { loadPersistedStats(currentKey) }
            localStatsMap.clear()
            localStatsMap.putAll(loaded)
            reconcileSavedCountsLocked()
            reconcileLikedCountsLocked()
            localStatsFlow.value = localStatsMap.toMap()
            loadedStatsKey = currentKey
        }
    }

    private fun reconcileSavedCountsLocked() {
        localSavedProductIds.forEach { productId ->
            val current = localStatsMap[productId] ?: PublicationStats(id = productId)
            if (current.savesCount < 1L) {
                localStatsMap[productId] = current.copy(savesCount = 1L)
            }
        }
    }

    private fun reconcileLikedCountsLocked() {
        localLikedPublicationIds.forEach { productId ->
            val current = localStatsMap[productId] ?: PublicationStats(id = productId)
            if (current.likesCount < 1L) {
                localStatsMap[productId] = current.copy(likesCount = 1L)
            }
        }
    }

    private fun loadPersistedStats(userKey: String): Map<String, PublicationStats> {
        val raw = statsPrefs.getString(statsCacheKey(userKey), null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, PublicationStats>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val item = obj.optJSONObject(id) ?: continue
                result[id] = PublicationStats(
                    id = id,
                    likesCount = item.optLong("likesCount", 0L).coerceAtLeast(0L),
                    commentsCount = item.optLong("commentsCount", 0L).coerceAtLeast(0L),
                    sharesCount = item.optLong("sharesCount", 0L).coerceAtLeast(0L),
                    savesCount = item.optLong("savesCount", 0L).coerceAtLeast(0L)
                )
            }
            result
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer las estadísticas locales: ${e.message}")
            emptyMap()
        }
    }

    private suspend fun savePersistedStats(
        snapshot: Map<String, PublicationStats>,
        userKey: String = userCacheKey()
    ) {
        withContext(Dispatchers.IO) {
            try {
                val root = JSONObject()
                snapshot.forEach { (id, stat) ->
                    root.put(
                        id,
                        JSONObject().apply {
                            put("likesCount", stat.likesCount)
                            put("commentsCount", stat.commentsCount)
                            put("sharesCount", stat.sharesCount)
                            put("savesCount", stat.savesCount)
                        }
                    )
                }
                statsPrefs.edit().putString(statsCacheKey(userKey), root.toString()).commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar las estadísticas localmente: ${e.message}")
            }
        }
    }

    private suspend fun publishStatsState() {
        val snapshot = localStatsMap.toMap()
        localStatsFlow.value = snapshot
        savePersistedStats(snapshot)
    }

    private fun loadPersistedCart(): List<CartItem> {
        val raw = cartPrefs.getString(cartCacheKey(), null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        CartItem(
                            id = obj.optString("id", ""),
                            userId = obj.optString("userId", ""),
                            productId = obj.optString("productId", ""),
                            name = obj.optString("name", ""),
                            price = obj.optDouble("price", 0.0),
                            imageUrl = obj.optString("imageUrl", ""),
                            selectedSize = obj.optString("selectedSize", ""),
                            selectedColor = obj.optString("selectedColor", ""),
                            quantity = obj.optLong("quantity", 1L).coerceAtLeast(1L)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudo leer el carrito local persistido: ${e.message}")
            emptyList()
        }
    }

    private suspend fun savePersistedCart(items: List<CartItem>) {
        withContext(Dispatchers.IO) {
            try {
                val array = JSONArray()
                for (item in items) {
                    array.put(
                        JSONObject().apply {
                            put("id", item.id)
                            put("userId", item.userId)
                            put("productId", item.productId)
                            put("name", item.name)
                            put("price", item.price)
                            put("imageUrl", item.imageUrl)
                            put("selectedSize", item.selectedSize)
                            put("selectedColor", item.selectedColor)
                            put("quantity", item.quantity)
                        }
                    )
                }
                cartPrefs.edit().putString(cartCacheKey(), array.toString()).apply()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudo guardar el carrito localmente: ${e.message}")
            }
        }
    }

    private suspend fun publishCartState() {
        val snapshot = localCartItems.toList()
        localCartFlow.value = snapshot
        savePersistedCart(snapshot)
    }

    // -------------------------------------------------------------
    // PRODUCTS
    // -------------------------------------------------------------

    private fun loadBooleanOverrideMap(key: String): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        try {
            val raw = publicationStatePrefs.getString(key, null)
            if (!raw.isNullOrBlank()) {
                val obj = JSONObject(raw)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    if (id.isNotBlank()) {
                        result[id] = obj.optBoolean(id, true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los estados locales ($key): ${e.message}")
        }
        return result
    }

    private fun saveBooleanOverrideMap(key: String, map: Map<String, Boolean>) {
        try {
            val obj = JSONObject()
            map.forEach { (id, state) -> obj.put(id, state) }
            publicationStatePrefs.edit().putString(key, obj.toString()).commit()
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron guardar los estados locales ($key): ${e.message}")
        }
    }

    fun observeProducts(): Flow<List<Product>> {
        val remoteFlow = flow {
            val samples = getInitialSampleProducts()
            emit(samples)
            while (true) {
                try {
                    val remote = api.products().items
                    clearRemoteError("products")
                    emit(if (remote.isEmpty()) samples else remote)
                } catch (e: Exception) {
                    handleRemoteError("products", "No se pudo actualizar el catálogo", e)
                }
                delay(15000)
            }
        }.flowOn(Dispatchers.IO)

        return combine(remoteFlow, productStockOverridesFlow) { list, overrides ->
            if (overrides.isEmpty()) list
            else list.map { p ->
                overrides[p.id]?.let { overrideStock -> p.copy(inStock = overrideStock) } ?: p
            }
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
        val response = api.createProduct(requireRemoteToken(), finalProduct)
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
        return api.uploadMedia(requireRemoteToken(), part, name).url
    }

    suspend fun addAnnouncement(announcement: Announcement): String {
        val id = announcement.id.ifBlank { "ann_${java.util.UUID.randomUUID()}" }
        val image = if (announcement.imageUrl.startsWith("content://") || announcement.imageUrl.startsWith("file://"))
            uploadProductFile(announcement.imageUrl, id, "announcement_img")
        else announcement.imageUrl.ifBlank { getFallbackCoverForCategory(announcement.targetCategory) }
        val video = if (announcement.videoUrl.startsWith("content://") || announcement.videoUrl.startsWith("file://"))
            uploadProductFile(announcement.videoUrl, id, "announcement_video")
        else announcement.videoUrl
        val final = announcement.copy(id = id, imageUrl = image, videoUrl = video, isVideo = announcement.hasVideo || video.isNotBlank())
        api.createAnnouncement(requireRemoteToken(), final)
        return id
    }

    fun observeAnnouncements(): Flow<List<Announcement>> {
        val remoteFlow = flow {
            val samples = getInitialSampleAnnouncements()
            emit(samples)
            while (true) {
                try {
                    val remote = api.announcements(includeInactive = true).items
                    clearRemoteError("announcements")
                    emit(if (remote.isEmpty()) samples else remote)
                } catch (e: Exception) {
                    handleRemoteError("announcements", "No se pudieron actualizar los anuncios", e)
                }
                delay(15000)
            }
        }.flowOn(Dispatchers.IO)

        return combine(remoteFlow, announcementActiveOverridesFlow) { list, overrides ->
            if (overrides.isEmpty()) list
            else list.map { ann ->
                overrides[ann.id]?.let { overrideActive -> ann.copy(isActive = overrideActive) } ?: ann
            }
        }
    }

    suspend fun deleteProduct(productId: String) {
        api.deleteProduct(requireRemoteToken(), productId)
    }

    suspend fun toggleProductStock(productId: String, inStock: Boolean) {
        val updatedOverrides = productStockOverridesFlow.value + (productId to inStock)
        productStockOverridesFlow.value = updatedOverrides
        withContext(Dispatchers.IO) {
            saveBooleanOverrideMap("product_stock_overrides", updatedOverrides)
            val token = remoteToken() ?: return@withContext
            try {
                api.updateProduct(token, productId, ProductStockUpdateRequest(inStock = inStock))
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                    Log.i("EleganRepository", "Producto $productId no existe en colección remota (404); estado guardado localmente.")
                } else if (e.code() == 401) {
                    MobileSession.markRemoteSessionRejected()
                } else {
                    Log.w("EleganRepository", "No se pudo sincronizar estado de producto $productId: HTTP ${e.code()}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("EleganRepository", "Error de red al actualizar estado de producto $productId: ${e.message}")
            }
        }
    }

    suspend fun deleteAnnouncement(announcementId: String) {
        api.deleteAnnouncement(requireRemoteToken(), announcementId)
    }

    suspend fun toggleAnnouncementActive(announcementId: String, isActive: Boolean) {
        val updatedOverrides = announcementActiveOverridesFlow.value + (announcementId to isActive)
        announcementActiveOverridesFlow.value = updatedOverrides
        withContext(Dispatchers.IO) {
            saveBooleanOverrideMap("announcement_active_overrides", updatedOverrides)
            val token = remoteToken() ?: return@withContext
            try {
                api.updateAnnouncement(token, announcementId, AnnouncementActiveUpdateRequest(isActive = isActive))
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                    Log.i("EleganRepository", "Anuncio $announcementId no existe en colección remota (404); estado guardado localmente.")
                } else if (e.code() == 401) {
                    MobileSession.markRemoteSessionRejected()
                } else {
                    Log.w("EleganRepository", "No se pudo sincronizar estado de anuncio $announcementId: HTTP ${e.code()}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("EleganRepository", "Error de red al actualizar estado de anuncio $announcementId: ${e.message}")
            }
        }
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

    fun observeCart(): Flow<List<CartItem>> = channelFlow {
        ensureCartLoaded()
        launch {
            while (true) {
                val token = remoteToken()
                if (token != null) {
                    try {
                        val remote = api.cart(token).items
                        clearRemoteError("cart")
                        if (remote.isNotEmpty()) {
                            val localOnly = localCartItems.filter { local ->
                                remote.none { r ->
                                    r.productId == local.productId &&
                                        r.selectedSize == local.selectedSize &&
                                        r.selectedColor == local.selectedColor
                                }
                            }
                            localCartItems.clear()
                            localCartItems.addAll(remote + localOnly)
                            publishCartState()
                        }
                    } catch (e: Exception) {
                        handleRemoteError("cart", "Servidor no disponible para sincronizar carrito", e)
                    }
                }
                delay(10000)
            }
        }
        localCartFlow.collect { send(it) }
    }.flowOn(Dispatchers.IO)

    fun observeSavedProductIds(): Flow<Set<String>> = channelFlow {
        ensureSavedLoaded()
        ensureStatsLoaded()
        launch {
            while (true) {
                ensureSavedLoaded()
                val token = remoteToken()
                if (token != null) {
                    try {
                        if (pendingSavedOps.isNotEmpty()) {
                            val pendingSnapshot = pendingSavedOps.toMap()
                            pendingSnapshot.forEach { (pid, saved) ->
                                api.setSaved(token, pid, SavedRequest(saved))
                                pendingSavedOps.remove(pid, saved)
                            }
                            savePersistedSavedState()
                        }
                        val remoteIds = api.saved(token).ids.toSet()
                        clearRemoteError("saved")
                        val pendingTrue = pendingSavedOps.filterValues { it }.keys
                        val pendingFalse = pendingSavedOps.filterValues { !it }.keys
                        val merged = (remoteIds + pendingTrue) - pendingFalse
                        if (merged != localSavedProductIds) {
                            localSavedProductIds.clear()
                            localSavedProductIds.addAll(merged)
                            localSavedFlow.value = localSavedProductIds.toSet()
                            savePersistedSavedState()
                            ensureStatsLoaded()
                            reconcileSavedCountsLocked()
                            publishStatsState()
                        }
                    } catch (e: Exception) {
                        handleRemoteError("saved", "Servidor no disponible para sincronizar guardados", e)
                    }
                }
                delay(15000)
            }
        }
        localSavedFlow.collect { send(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun setProductSaved(productId: String, isSaved: Boolean) {
        require(productId.isNotBlank())
        ensureSavedLoaded()
        ensureStatsLoaded()

        val wasSaved = productId in localSavedProductIds
        if (isSaved) {
            localSavedProductIds.add(productId)
        } else {
            localSavedProductIds.remove(productId)
        }
        localSavedFlow.value = localSavedProductIds.toSet()
        pendingSavedOps[productId] = isSaved
        savePersistedSavedState()

        if (wasSaved != isSaved) {
            val current = localStatsMap[productId] ?: PublicationStats(id = productId)
            val updatedSaves = if (isSaved) {
                current.savesCount + 1L
            } else {
                (current.savesCount - 1L).coerceAtLeast(0L)
            }
            localStatsMap[productId] = current.copy(savesCount = updatedSaves)
            publishStatsState()
        }

        val token = remoteToken()
        if (token != null) {
            try {
                api.setSaved(token, productId, SavedRequest(isSaved))
                pendingSavedOps.remove(productId, isSaved)
                savePersistedSavedState()
                clearRemoteError("saved_write")
            } catch (e: Exception) {
                handleRemoteError("saved_write", "No se pudo sincronizar guardado remoto", e)
            }
        }
    }

    fun observePublicationStats(): Flow<Map<String, PublicationStats>> = channelFlow {
        ensureSavedLoaded()
        ensureLikesLoaded()
        ensureStatsLoaded()
        launch {
            while (true) {
                ensureSavedLoaded()
                ensureLikesLoaded()
                ensureStatsLoaded()
                try {
                    val stats = api.publicationStats().stats.mapValues { (id, value) -> value.copy(id = id) }
                    clearRemoteError("stats")
                    var changed = false
                    stats.forEach { (id, remoteStat) ->
                        val local = localStatsMap[id] ?: PublicationStats(id = id)
                        val isSavedLocally = id in localSavedProductIds
                        val hasPendingSave = pendingSavedOps.containsKey(id)
                        val mergedSaves = if (hasPendingSave) {
                            local.savesCount
                        } else if (isSavedLocally) {
                            maxOf(remoteStat.savesCount, local.savesCount, 1L)
                        } else {
                            maxOf(remoteStat.savesCount, local.savesCount)
                        }
                        val isLikedLocally = id in localLikedPublicationIds
                        val hasPendingLike = pendingLikeOps.containsKey(id)
                        val mergedLikes = if (hasPendingLike) {
                            local.likesCount
                        } else if (isLikedLocally) {
                            maxOf(remoteStat.likesCount, local.likesCount, 1L)
                        } else {
                            maxOf(remoteStat.likesCount, local.likesCount)
                        }
                        val merged = remoteStat.copy(
                            commentsCount = maxOf(remoteStat.commentsCount, local.commentsCount),
                            likesCount = mergedLikes,
                            sharesCount = maxOf(remoteStat.sharesCount, local.sharesCount),
                            savesCount = mergedSaves
                        )
                        if (localStatsMap[id] != merged) {
                            localStatsMap[id] = merged
                            changed = true
                        }
                    }
                    if (changed) {
                        publishStatsState()
                    }
                } catch (e: Exception) {
                    handleRemoteError("stats", "No se pudieron cargar estadísticas", e)
                }
                delay(10000)
            }
        }
        localStatsFlow.collect { send(it) }
    }.flowOn(Dispatchers.IO)

    fun observeLikedPublicationIds(): Flow<Set<String>> = channelFlow {
        ensureLikesLoaded()
        ensureStatsLoaded()
        launch {
            while (true) {
                ensureLikesLoaded()
                val token = remoteToken()
                if (token != null) {
                    try {
                        var remoteIds = api.liked(token).ids.toSet()
                        if (pendingLikeOps.isNotEmpty()) {
                            val pendingSnapshot = pendingLikeOps.toMap()
                            pendingSnapshot.forEach { (pid, desiredLiked) ->
                                val isRemoteLiked = pid in remoteIds
                                if (isRemoteLiked != desiredLiked) {
                                    val resultLiked = api.toggleLike(token, pid).liked
                                    remoteIds = if (resultLiked) remoteIds + pid else remoteIds - pid
                                }
                                pendingLikeOps.remove(pid, desiredLiked)
                            }
                            savePersistedLikesState()
                        }
                        clearRemoteError("likes")
                        val pendingTrue = pendingLikeOps.filterValues { it }.keys
                        val pendingFalse = pendingLikeOps.filterValues { !it }.keys
                        val merged = (remoteIds + pendingTrue) - pendingFalse
                        if (merged != localLikedPublicationIds) {
                            localLikedPublicationIds.clear()
                            localLikedPublicationIds.addAll(merged)
                            localLikedFlow.value = localLikedPublicationIds.toSet()
                            savePersistedLikesState()
                            ensureStatsLoaded()
                            reconcileLikedCountsLocked()
                            publishStatsState()
                        }
                    } catch (e: Exception) {
                        handleRemoteError("likes", "No se pudieron cargar likes", e)
                    }
                }
                delay(15000)
            }
        }
        localLikedFlow.collect { send(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun togglePublicationLike(productId: String): Boolean {
        require(productId.isNotBlank())
        ensureLikesLoaded()
        ensureStatsLoaded()

        val wasLiked = productId in localLikedPublicationIds
        val targetLiked = !wasLiked
        if (targetLiked) {
            localLikedPublicationIds.add(productId)
        } else {
            localLikedPublicationIds.remove(productId)
        }
        localLikedFlow.value = localLikedPublicationIds.toSet()
        pendingLikeOps[productId] = targetLiked
        savePersistedLikesState()

        val current = localStatsMap[productId] ?: PublicationStats(id = productId)
        val updatedLikes = if (targetLiked) {
            current.likesCount + 1L
        } else {
            (current.likesCount - 1L).coerceAtLeast(0L)
        }
        localStatsMap[productId] = current.copy(likesCount = updatedLikes)
        publishStatsState()

        val token = remoteToken()
        if (token != null) {
            try {
                val remoteLiked = api.toggleLike(token, productId).liked
                clearRemoteError("like_write")
                pendingLikeOps.remove(productId, targetLiked)
                if (remoteLiked != targetLiked) {
                    if (remoteLiked) {
                        localLikedPublicationIds.add(productId)
                    } else {
                        localLikedPublicationIds.remove(productId)
                    }
                    localLikedFlow.value = localLikedPublicationIds.toSet()
                    val latest = localStatsMap[productId] ?: PublicationStats(id = productId)
                    val correctedCount = if (remoteLiked) {
                        maxOf(latest.likesCount, 1L)
                    } else {
                        (latest.likesCount - 1L).coerceAtLeast(0L)
                    }
                    localStatsMap[productId] = latest.copy(likesCount = correctedCount)
                    publishStatsState()
                }
                savePersistedLikesState()
                return remoteLiked
            } catch (e: Exception) {
                handleRemoteError("like_write", "Error en like remoto", e)
            }
        }
        return targetLiked
    }

    fun observePublicationComments(productId: String): Flow<List<PublicationComment>> = flow {
        require(productId.isNotBlank())
        val localList = localCommentsCache.computeIfAbsent(productId) { mutableListOf() }
        emit(synchronized(localList) { localList.toList() })

        var lastSync = 0L
        while (true) {
            val now = System.currentTimeMillis()
            if (now - lastSync >= 10000L) {
                try {
                    val remoteItems = api.comments(productId).items.distinctBy { it.id }
                    clearRemoteError("comments_$productId")
                    synchronized(localList) {
                        val unmatchedRemoteKeys = remoteItems
                            .map { it.displayName.trim() to it.text.trim() }
                            .toMutableList()

                        val pendingLocal = localList.filter { localItem ->
                            if (!localItem.id.startsWith("local_")) {
                                false
                            } else {
                                val key = localItem.displayName.trim() to localItem.text.trim()
                                val matchIdx = unmatchedRemoteKeys.indexOf(key)
                                if (matchIdx >= 0) {
                                    unmatchedRemoteKeys.removeAt(matchIdx)
                                    false
                                } else {
                                    true
                                }
                            }
                        }

                        val merged = (remoteItems + pendingLocal)
                            .distinctBy { it.id }
                            .sortedBy { it.createdAt ?: "" }
                        localList.clear()
                        localList.addAll(merged)
                    }
                    lastSync = now
                } catch (e: Exception) {
                    handleRemoteError("comments_$productId", "No se pudieron sincronizar comentarios remotos", e)
                }
            }
            emit(synchronized(localList) { localList.toList() })
            withTimeoutOrNull(2000L) {
                localCommentsUpdated.first { it == productId }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun addPublicationComment(productId: String, displayName: String, text: String) {
        val clean = text.trim()
        require(productId.isNotBlank() && clean.isNotEmpty() && clean.length <= 1000)

        val userId = optionalUserId() ?: "cliente_${java.util.UUID.randomUUID().toString().take(8)}"
        val finalName = displayName.trim().ifBlank { "Cliente" }.take(80)
        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())

        val tempId = "local_${java.util.UUID.randomUUID()}"
        val newComment = PublicationComment(
            id = tempId,
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
        localCommentsUpdated.tryEmit(productId)

        // 2. Incrementar contador local de comentarios
        ensureStatsLoaded()
        val currentStat = localStatsMap[productId] ?: PublicationStats(id = productId)
        localStatsMap[productId] = currentStat.copy(commentsCount = currentStat.commentsCount + 1)
        publishStatsState()

        // 3. Sincronizar con el servidor en segundo plano y reconciliar ID real para evitar duplicados
        val token = remoteToken() ?: "Bearer guest"
        try {
            val response = api.addComment(token, productId, CommentRequest(finalName, clean))
            val serverId = response.id.trim()
            if (serverId.isNotEmpty()) {
                synchronized(localList) {
                    val alreadySynced = localList.any { it.id == serverId }
                    if (alreadySynced) {
                        localList.removeAll { it.id == tempId }
                    } else {
                        val idx = localList.indexOfFirst { it.id == tempId }
                        if (idx >= 0) {
                            localList[idx] = localList[idx].copy(id = serverId)
                        }
                    }
                }
                localCommentsUpdated.tryEmit(productId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("EleganRepository", "Comentario registrado localmente; servidor reportó: ${e.message}")
        }
    }

    suspend fun recordPublicationShare(productId: String) {
        require(productId.isNotBlank())
        ensureStatsLoaded()
        val currentStat = localStatsMap[productId] ?: PublicationStats(id = productId)
        localStatsMap[productId] = currentStat.copy(sharesCount = currentStat.sharesCount + 1)
        publishStatsState()
        val token = remoteToken() ?: "Bearer guest"
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
        ensureCartLoaded()
        val existingIndex = localCartItems.indexOfFirst {
            it.productId == product.id && it.selectedSize == selectedSize && it.selectedColor == selectedColor
        }
        val targetItem: CartItem
        if (existingIndex >= 0) {
            val existing = localCartItems[existingIndex]
            targetItem = existing.copy(quantity = existing.quantity + quantity)
            localCartItems[existingIndex] = targetItem
        } else {
            targetItem = CartItem(
                id = java.util.UUID.randomUUID().toString(),
                productId = product.id,
                name = product.name,
                price = product.price,
                imageUrl = product.imageUrl,
                selectedSize = selectedSize,
                selectedColor = selectedColor,
                quantity = quantity
            )
            localCartItems.add(targetItem)
        }
        publishCartState()

        val token = remoteToken()
        if (token != null) {
            try {
                val remoteId = api.addCartItem(token, targetItem).id
                clearRemoteError("cart_write")
                if (remoteId.isNotBlank() && remoteId != targetItem.id) {
                    val idx = localCartItems.indexOfFirst { it.id == targetItem.id }
                    if (idx >= 0) {
                        localCartItems[idx] = localCartItems[idx].copy(id = remoteId)
                        publishCartState()
                    }
                }
            } catch (e: Exception) {
                handleRemoteError("cart_write", "No se pudo sincronizar producto en carrito remoto", e)
            }
        }
    }

    suspend fun updateCartQuantity(cartItemId: String, newQuantity: Long) {
        ensureCartLoaded()
        if (newQuantity <= 0) {
            removeFromCart(cartItemId)
            return
        }
        val index = localCartItems.indexOfFirst { it.id == cartItemId }
        if (index >= 0) {
            val updated = localCartItems[index].copy(quantity = newQuantity)
            localCartItems[index] = updated
            publishCartState()
        }
        val token = remoteToken()
        if (token != null) {
            try {
                api.updateCartItem(token, cartItemId, QuantityRequest(newQuantity))
                clearRemoteError("cart_write")
            } catch (e: Exception) {
                handleRemoteError("cart_write", "No se pudo actualizar cantidad en servidor", e)
            }
        }
    }

    suspend fun removeFromCart(cartItemId: String) {
        ensureCartLoaded()
        localCartItems.removeAll { it.id == cartItemId }
        publishCartState()
        val token = remoteToken()
        if (token != null) {
            try {
                api.removeCartItem(token, cartItemId)
                clearRemoteError("cart_write")
            } catch (e: Exception) {
                handleRemoteError("cart_write", "No se pudo eliminar item en servidor", e)
            }
        }
    }

    suspend fun clearCart() {
        ensureCartLoaded()
        localCartItems.clear()
        publishCartState()
        val token = remoteToken()
        if (token != null) {
            try {
                api.clearCart(token)
                clearRemoteError("cart_write")
            } catch (e: Exception) {
                handleRemoteError("cart_write", "No se pudo vaciar carrito en servidor", e)
            }
        }
    }

    private suspend fun ensureOrdersLoaded() {
        val currentKey = userCacheKey()
        if (loadedOrdersKey == currentKey) return
        ordersLoadMutex.withLock {
            if (loadedOrdersKey == currentKey) return
            val (loadedOrders, loadedPending) = withContext(Dispatchers.IO) {
                loadPersistedOrdersState(currentKey)
            }
            localOrdersList.clear()
            if (loadedOrders.isNotEmpty()) {
                localOrdersList.addAll(loadedOrders)
            }
            pendingOrderIds.clear()
            pendingOrderIds.addAll(loadedPending)
            localOrdersFlow.value = localOrdersList.toList()
            loadedOrdersKey = currentKey
        }
    }

    private fun loadPersistedOrdersState(userKey: String): Pair<List<Order>, Set<String>> {
        val orders = mutableListOf<Order>()
        val pending = mutableSetOf<String>()
        try {
            val rawOrders = ordersPrefs.getString(ordersCacheKey(userKey), null)
            if (!rawOrders.isNullOrBlank()) {
                val array = JSONArray(rawOrders)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    orders.add(
                        Order(
                            id = obj.optString("id", ""),
                            userId = obj.optString("userId", ""),
                            customerName = obj.optString("customerName", ""),
                            email = obj.optString("email", ""),
                            phone = obj.optString("phone", ""),
                            address = obj.optString("address", ""),
                            city = obj.optString("city", ""),
                            paymentMethod = obj.optString("paymentMethod", "Tarjeta"),
                            paymentProofUrl = obj.optString("paymentProofUrl", ""),
                            total = obj.optDouble("total", 0.0),
                            itemCount = obj.optLong("itemCount", 1L).coerceAtLeast(1L),
                            status = obj.optString("status", "confirmado"),
                            itemsSummary = obj.optString("itemsSummary", ""),
                            createdAt = obj.optString("createdAt", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
            val rawPending = ordersPrefs.getString(pendingOrdersCacheKey(userKey), null)
            if (!rawPending.isNullOrBlank()) {
                val array = JSONArray(rawPending)
                for (i in 0 until array.length()) {
                    val id = array.optString(i, "").trim()
                    if (id.isNotBlank()) pending.add(id)
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los pedidos locales: ${e.message}")
        }
        return orders to pending
    }

    private suspend fun savePersistedOrdersState(
        ordersSnapshot: List<Order> = localOrdersList.toList(),
        pendingSnapshot: Set<String> = pendingOrderIds.toSet(),
        userKey: String = userCacheKey()
    ) {
        withContext(Dispatchers.IO) {
            try {
                val ordersArray = JSONArray()
                ordersSnapshot.forEach { order ->
                    ordersArray.put(
                        JSONObject().apply {
                            put("id", order.id)
                            put("userId", order.userId)
                            put("customerName", order.customerName)
                            put("email", order.email)
                            put("phone", order.phone)
                            put("address", order.address)
                            put("city", order.city)
                            put("paymentMethod", order.paymentMethod)
                            put("paymentProofUrl", order.paymentProofUrl)
                            put("total", order.total)
                            put("itemCount", order.itemCount)
                            put("status", order.status)
                            put("itemsSummary", order.itemsSummary)
                            put("createdAt", order.createdAt.orEmpty())
                        }
                    )
                }
                val pendingArray = JSONArray()
                pendingSnapshot.forEach { pendingArray.put(it) }
                ordersPrefs.edit()
                    .putString(ordersCacheKey(userKey), ordersArray.toString())
                    .putString(pendingOrdersCacheKey(userKey), pendingArray.toString())
                    .commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar los pedidos localmente: ${e.message}")
            }
        }
    }

    private suspend fun publishOrdersState() {
        val snapshot = localOrdersList.toList()
        localOrdersFlow.value = snapshot
        savePersistedOrdersState(ordersSnapshot = snapshot)
    }

    fun observeOrders(): Flow<List<Order>> = channelFlow {
        ensureOrdersLoaded()
        launch {
            while (true) {
                ensureOrdersLoaded()
                val token = remoteToken()
                if (token != null) {
                    try {
                        if (pendingOrderIds.isNotEmpty()) {
                            val pendingList = localOrdersList.filter { it.id in pendingOrderIds }
                            for (pendingOrder in pendingList) {
                                val createdId = api.createOrder(token, pendingOrder).id
                                clearRemoteError("order_write")
                                pendingOrderIds.remove(pendingOrder.id)
                                if (createdId.isNotBlank() && createdId != pendingOrder.id) {
                                    val idx = localOrdersList.indexOfFirst { it.id == pendingOrder.id }
                                    if (idx >= 0) {
                                        localOrdersList[idx] = localOrdersList[idx].copy(id = createdId)
                                    }
                                }
                            }
                            publishOrdersState()
                        }

                        val remoteItems = api.orders(token).items
                        clearRemoteError("orders")
                        val remoteIds = remoteItems.map { it.id }.toSet()
                        val localOnly = localOrdersList.filter { local ->
                            local.id !in remoteIds && (
                                local.id in pendingOrderIds ||
                                    local.id.startsWith("order_")
                                )
                        }
                        val merged = (localOnly + remoteItems)
                            .distinctBy { it.id }
                            .sortedByDescending { it.createdAt.orEmpty() }
                        if (merged != localOrdersList.toList()) {
                            localOrdersList.clear()
                            localOrdersList.addAll(merged)
                            publishOrdersState()
                        }
                    } catch (e: Exception) {
                        handleRemoteError("orders", "No se pudieron sincronizar pedidos remotos", e)
                    }
                }
                delay(15000)
            }
        }
        localOrdersFlow.collect { send(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun createOrder(
        customerName: String, email: String, phone: String, address: String,
        city: String, paymentMethod: String, total: Double, itemCount: Long, itemsSummary: String,
        paymentProofUri: String = ""
    ): String {
        ensureCartLoaded()
        ensureOrdersLoaded()

        val localId = "order_" + java.util.UUID.randomUUID().toString().take(8)
        val token = remoteToken()
        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())

        val uploadedProofUrl = if (paymentProofUri.isNotBlank() && token != null) {
            try {
                uploadProductFile(paymentProofUri, localId, "payment_proof")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudo subir comprobante remoto (${e.message}), guardando referencia local.")
                paymentProofUri
            }
        } else {
            paymentProofUri
        }

        val initialOrder = Order(
            id = localId,
            userId = optionalUserId().orEmpty(),
            customerName = customerName,
            email = email,
            phone = phone,
            address = address,
            city = city,
            paymentMethod = paymentMethod,
            paymentProofUrl = uploadedProofUrl,
            total = total,
            itemCount = itemCount.coerceAtLeast(1L),
            status = "confirmado",
            itemsSummary = itemsSummary,
            createdAt = nowIso
        )

        val cartSnapshot = localCartItems.toList()
        localOrdersList.add(0, initialOrder)
        pendingOrderIds.add(localId)
        publishOrdersState()

        localCartItems.clear()
        publishCartState()

        var finalOrderId = localId
        if (token != null) {
            try {
                // Garantizar que el carrito del servidor contenga los artículos antes de crear la orden remota
                val remoteCart = try {
                    api.cart(token).items
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    emptyList()
                }
                if (remoteCart.isEmpty() && cartSnapshot.isNotEmpty()) {
                    for (cartItem in cartSnapshot) {
                        try {
                            api.addCartItem(token, cartItem)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                        }
                    }
                }

                val createdId = api.createOrder(token, initialOrder).id
                clearRemoteError("order_write")
                pendingOrderIds.remove(localId)
                if (createdId.isNotBlank()) {
                    finalOrderId = createdId
                    val idx = localOrdersList.indexOfFirst { it.id == localId }
                    if (idx >= 0) {
                        localOrdersList[idx] = localOrdersList[idx].copy(id = createdId)
                    }
                }
                publishOrdersState()
            } catch (e: Exception) {
                handleRemoteError("order_write", "No se pudo crear orden en servidor", e)
            }
        }
        return finalOrderId
    }

    private fun messagesCacheKey(userKey: String = userCacheKey()): String = "support_messages_$userKey"

    private suspend fun ensureMessagesLoaded() {
        val currentKey = userCacheKey()
        if (loadedMessagesKey == currentKey) return
        messagesLoadMutex.withLock {
            if (loadedMessagesKey == currentKey) return
            val currentUserId = optionalUserId().orEmpty()
            val welcome = SupportMessage(
                id = "welcome",
                userId = currentUserId,
                sender = "support",
                senderName = "Concierge Elegan",
                text = "¡Bienvenido a Elegan! Soy tu asesor personal de estilo y compras. ¿En qué podemos ayudarte hoy?"
            )
            val loaded = withContext(Dispatchers.IO) { loadPersistedMessages(currentKey) }
            localMessagesList.clear()
            if (loaded.isNotEmpty()) {
                localMessagesList.addAll(loaded)
            } else {
                localMessagesList.add(welcome)
            }
            localMessagesFlow.value = localMessagesList.toList()
            loadedMessagesKey = currentKey
        }
    }

    private fun loadPersistedMessages(userKey: String): List<SupportMessage> {
        val raw = messagesPrefs.getString(messagesCacheKey(userKey), null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        SupportMessage(
                            id = obj.optString("id", ""),
                            userId = obj.optString("userId", ""),
                            sender = obj.optString("sender", "user"),
                            senderName = obj.optString("senderName", ""),
                            text = obj.optString("text", ""),
                            mediaUrl = obj.optString("mediaUrl", ""),
                            mediaType = obj.optString("mediaType", ""),
                            createdAt = obj.optString("createdAt", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los mensajes locales: ${e.message}")
            emptyList()
        }
    }

    private suspend fun savePersistedMessages(userKey: String = userCacheKey()) {
        val snapshot = localMessagesList.toList()
        withContext(Dispatchers.IO) {
            try {
                val array = JSONArray()
                for (msg in snapshot) {
                    array.put(
                        JSONObject().apply {
                            put("id", msg.id)
                            put("userId", msg.userId)
                            put("sender", msg.sender)
                            put("senderName", msg.senderName)
                            put("text", msg.text)
                            put("mediaUrl", msg.mediaUrl)
                            put("mediaType", msg.mediaType)
                            put("createdAt", msg.createdAt.orEmpty())
                        }
                    )
                }
                messagesPrefs.edit().putString(messagesCacheKey(userKey), array.toString()).commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar los mensajes localmente: ${e.message}")
            }
        }
    }

    private suspend fun publishMessagesState() {
        localMessagesFlow.value = localMessagesList.toList()
        savePersistedMessages()
    }

    private suspend fun persistSupportMediaLocally(source: String, msgId: String, isVideo: Boolean): String {
        if (source.isBlank() || source.startsWith("http://") || source.startsWith("https://")) return source
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(source)
                if (uri.scheme == "file" && uri.path?.let { File(it).exists() } == true) {
                    return@withContext source
                }
                val ext = if (isVideo) "mp4" else "jpg"
                val dir = File(appContext.filesDir, "support_media").apply { if (!exists()) mkdirs() }
                val destFile = File(dir, "${msgId}.$ext")
                val input = when (uri.scheme) {
                    "content" -> appContext.contentResolver.openInputStream(uri)
                    "file" -> File(uri.path ?: "").takeIf { it.exists() }?.inputStream()
                    else -> File(source).takeIf { it.exists() }?.inputStream()
                }
                if (input != null) {
                    input.use { src ->
                        destFile.outputStream().use { dst -> src.copyTo(dst) }
                    }
                    Uri.fromFile(destFile).toString()
                } else {
                    source
                }
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudo copiar medio local del chat: ${e.message}")
                source
            }
        }
    }

    fun observeMessages(): Flow<List<SupportMessage>> = channelFlow {
        ensureMessagesLoaded()
        launch {
            while (true) {
                ensureMessagesLoaded()
                val token = remoteToken()
                if (token != null) {
                    try {
                        val items = api.messages(token).items
                        clearRemoteError("messages")
                        if (items.isNotEmpty()) {
                            val merged = (localMessagesList + items)
                                .distinctBy { it.id }
                                .sortedBy { it.createdAt ?: "" }
                            localMessagesList.clear()
                            localMessagesList.addAll(merged)
                            publishMessagesState()
                        }
                    } catch (e: Exception) {
                        handleRemoteError("messages", "No se pudieron cargar mensajes del servidor", e)
                    }
                }
                delay(10000)
            }
        }
        localMessagesFlow.collect { send(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun sendSupportMessage(
        text: String,
        mediaUri: String = "",
        mediaType: String = "",
        scope: CoroutineScope
    ) {
        ensureMessagesLoaded()
        val clean = text.trim()
        val cleanMedia = mediaUri.trim()
        require((clean.isNotEmpty() && clean.length <= 4000) || cleanMedia.isNotEmpty())
        val msgId = java.util.UUID.randomUUID().toString()
        val resolvedType = if (cleanMedia.isNotEmpty()) {
            if (mediaType.equals("video", ignoreCase = true)) "video" else "image"
        } else ""
        val isVideo = resolvedType == "video"
        val localMediaUrl = if (cleanMedia.isNotEmpty()) {
            persistSupportMediaLocally(cleanMedia, msgId, isVideo)
        } else ""

        val nowIso = java.time.Instant.now().toString()
        val userMsg = SupportMessage(
            id = msgId,
            userId = optionalUserId().orEmpty(),
            sender = "user",
            senderName = MobileSession.user(appContext)?.name ?: "Cliente",
            text = clean,
            mediaUrl = localMediaUrl,
            mediaType = resolvedType,
            createdAt = nowIso
        )
        localMessagesList.add(userMsg)
        publishMessagesState()

        val token = remoteToken()
        var syncedRemote = false
        if (token != null) {
            try {
                val uploadedMediaUrl = if (localMediaUrl.isNotBlank()) {
                    try {
                        uploadProductFile(
                            source = localMediaUrl,
                            productId = msgId,
                            label = if (isVideo) "support_video" else "support_img"
                        )
                    } catch (e: Exception) {
                        handleRemoteError("message_media_upload", "No se pudo subir archivo multimedia del chat", e)
                        localMediaUrl
                    }
                } else ""

                api.sendMessage(
                    token,
                    MessageRequest(
                        text = clean,
                        mediaUrl = uploadedMediaUrl,
                        mediaType = resolvedType
                    )
                )
                clearRemoteError("message_write")
                syncedRemote = true

                // Refrescar de inmediato para mostrar la respuesta del Concierge
                val remoteItems = api.messages(token).items
                if (remoteItems.isNotEmpty()) {
                    val localById = localMessagesList.associateBy { it.id }
                    val merged = (localMessagesList + remoteItems)
                        .distinctBy { it.id }
                        .map { item ->
                            val localFallback = localById[item.id]
                            if (item.mediaUrl.isBlank() && localFallback != null && localFallback.mediaUrl.isNotBlank()) {
                                item.copy(mediaUrl = localFallback.mediaUrl, mediaType = localFallback.mediaType)
                            } else item
                        }
                        .sortedBy { it.createdAt ?: "" }
                    localMessagesList.clear()
                    localMessagesList.addAll(merged)
                    publishMessagesState()
                }
            } catch (e: Exception) {
                handleRemoteError("message_write", "No se pudo sincronizar mensaje remoto", e)
            }
        }

        if (!syncedRemote) {
            val normalized = clean.lowercase()
            val replyText = when {
                clean.isEmpty() && localMediaUrl.isNotEmpty() ->
                    "Hemos recibido tu archivo multimedia. Un asesor de Elegan lo revisará enseguida para ayudarte."
                normalized.contains("envio") || normalized.contains("entrega") ->
                    "Gracias por tu consulta. Nuestro equipo de Elegan te ayudará con la información de entrega de tu pedido."
                normalized.contains("talla") || normalized.contains("medida") ->
                    "Gracias por escribirnos. Nuestro equipo puede orientarte con las tallas y medidas disponibles."
                normalized.contains("devolucion") || normalized.contains("cambio") ->
                    "Recibimos tu consulta sobre cambios o devoluciones. Un asesor de Elegan te dará seguimiento."
                normalized.contains("descuento") || normalized.contains("promocion") ->
                    "Gracias por consultar nuestras promociones. Revisa los anuncios y ofertas vigentes en la tienda."
                else ->
                    "Gracias por comunicarte con Elegan. Hemos recibido tu consulta y nuestro equipo te dará seguimiento."
            }
            localMessagesList.add(
                SupportMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = optionalUserId().orEmpty(),
                    sender = "support",
                    senderName = "Elegan Concierge",
                    text = replyText,
                    createdAt = java.time.Instant.now().plusMillis(1).toString()
                )
            )
            publishMessagesState()
        }
    }

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val cachedUser = MobileSession.user(appContext)
        val initialProfile = cachedUser?.let {
            UserProfile(
                userId = it.id,
                displayName = it.name,
                email = it.email.orEmpty(),
                photoUrl = it.photoUrl
            )
        }
        emit(initialProfile)
        while (true) {
            val token = remoteToken()
            if (token != null) {
                try {
                    val remote = api.profile(token).item
                    clearRemoteError("profile")
                    if (remote != null) emit(remote)
                } catch (e: Exception) {
                    handleRemoteError("profile", "No se pudo cargar perfil remoto", e)
                }
            }
            delay(15000)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun saveUserProfile(phone: String, address: String, city: String) {
        val user = MobileSession.user(appContext)
        val profile = UserProfile(
            userId = optionalUserId().orEmpty(),
            displayName = user?.name.orEmpty(),
            email = user?.email.orEmpty(),
            photoUrl = user?.photoUrl.orEmpty(),
            phone = phone, address = address, city = city
        )
        val token = remoteToken()
        if (token != null) {
            try {
                api.saveProfile(token, profile)
                clearRemoteError("profile_write")
            } catch (e: Exception) {
                handleRemoteError("profile_write", "No se pudo guardar perfil en servidor", e)
            }
        }
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
