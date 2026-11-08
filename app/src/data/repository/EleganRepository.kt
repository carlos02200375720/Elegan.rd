package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.model.*
import com.example.data.remote.*
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
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

    private fun requireUserId(): String =
        MobileSession.userId(appContext) ?: throw IllegalStateException("Debes iniciar sesión para realizar esta operación.")

    private fun requireToken(): String =
        MobileSession.bearerToken(appContext) ?: throw IllegalStateException("Tu sesión no está activa. Inicia sesión nuevamente.")

    private fun optionalToken(): String? = MobileSession.bearerToken(appContext)

    private fun getDeletedAnnouncementIds(): Set<String> {
        val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE)
        return prefs?.getStringSet("deleted_announcements", emptySet()) ?: emptySet()
    }

    private fun markAnnouncementDeleted(id: String) {
        val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE) ?: return
        val current = prefs.getStringSet("deleted_announcements", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(id)
        prefs.edit().putStringSet("deleted_announcements", current).apply()
    }

    private fun getDeletedProductIds(): Set<String> {
        val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE)
        return prefs?.getStringSet("deleted_products", emptySet()) ?: emptySet()
    }

    private fun markProductDeleted(id: String) {
        val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE) ?: return
        val current = prefs.getStringSet("deleted_products", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(id)
        prefs.edit().putStringSet("deleted_products", current).apply()
    }

    // -------------------------------------------------------------
    // PRODUCTS
    // -------------------------------------------------------------

    fun observeProducts(): Flow<List<Product>> = flow {
        val deleted = getDeletedProductIds()
        val local = getCustomProductsLocally().filterNot { it.id in deleted }
        val samples = getInitialSampleProducts().filterNot { it.id in deleted }
        emit((local + samples).distinctBy { it.id })
        while (true) {
            try {
                val remote = api.products().items.filterNot { it.id in getDeletedProductIds() }
                val currentLocal = getCustomProductsLocally().filterNot { it.id in getDeletedProductIds() }
                val combined = (currentLocal + remote).distinctBy { it.id }
                emit(if (combined.isEmpty()) samples else combined)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudo actualizar el catálogo: ${e.message}") }
            delay(15000)
        }
    }

    suspend fun seedProductsIfEmpty() {
        // Los productos de muestra son un respaldo local; no se insertan automáticamente en MongoDB.
    }

    suspend fun seedAnnouncementsIfEmpty() {
        // Los anuncios de muestra son un respaldo local; no se insertan automáticamente en MongoDB.
    }

    suspend fun getProductById(productId: String): Product? = try {
        api.product(productId).item ?: getInitialSampleProducts().find { it.id == productId }
    } catch (e: Exception) {
        getCustomProductsLocally().find { it.id == productId }
            ?: getInitialSampleProducts().find { it.id == productId }
    }

    private fun getFallbackCoverForCategory
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

    private fun persistMediaLocally(
        appContext: Context,
        sourceUri: Uri,
        productId: String,
        label: String,
        isVideo: Boolean
    ): String {
        return try {
            if (sourceUri.scheme == "file") {
                val f = File(sourceUri.path ?: "")
                if (f.exists() && f.length() > 0) return sourceUri.toString()
            }
            val mediaDir = File(appContext.filesDir, "product_media").apply { if (!exists()) mkdirs() }
            val mimeType = appContext.contentResolver.getType(sourceUri)
                ?: if (isVideo) "video/mp4" else "image/jpeg"
            val extension = android.webkit.MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?.takeIf { it.isNotBlank() }
                ?: if (isVideo) "mp4" else "jpg"
            val targetFile = File(mediaDir, "${productId}_${label}_${System.currentTimeMillis()}.$extension")

            appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0) {
                Uri.fromFile(targetFile).toString()
            } else {
                sourceUri.toString()
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudo copiar el archivo a almacenamiento local permanente: ${e.message}")
            sourceUri.toString()
        }
    }

    private fun saveCustomProductLocally(product: Product) {
        val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE) ?: return
        val currentJson = prefs.getString("local_custom_products_json", "[]") ?: "[]"
        try {
            val array = JSONArray(currentJson)
            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("id") != product.id) {
                    newArray.put(obj)
                }
            }
            val productObj = JSONObject().apply {
                put("id", product.id)
                put("name", product.name)
                put("description", product.description)
                put("category", product.category)
                put("price", product.price)
                put("imageUrl", product.imageUrl)
                put("images", JSONArray(product.images))
                put("colors", JSONArray(product.colors))
                put("rating", product.rating)
                put("reviewCount", product.reviewCount)
                put("sizes", JSONArray(product.sizes))
                put("inStock", product.inStock)
                put("isFeatured", product.isFeatured)
                put("mediaFormat", product.mediaFormat)
                put("videoUrl", product.videoUrl)
                put("isVideo", product.hasVideo)
            }
            newArray.put(productObj)
            prefs.edit().putString("local_custom_products_json", newArray.toString()).apply()
        } catch (e: Exception) {
            Log.w("EleganRepository", "Error saving product locally: ${e.message}")
        }
    }

    private fun getCustomProductsLocally(): List<Product> {
        val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE) ?: return emptyList()
        val currentJson = prefs.getString("local_custom_products_json", "[]") ?: "[]"
        val list = mutableListOf<Product>()
        try {
            val array = JSONArray(currentJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val imagesArray = obj.optJSONArray("images")
                val imagesList = mutableListOf<String>()
                if (imagesArray != null) {
                    for (j in 0 until imagesArray.length()) {
                        imagesList.add(imagesArray.optString(j))
                    }
                }
                val sizesArray = obj.optJSONArray("sizes")
                val sizesList = mutableListOf<String>()
                if (sizesArray != null) {
                    for (j in 0 until sizesArray.length()) {
                        sizesList.add(sizesArray.optString(j))
                    }
                }
                val colorsArray = obj.optJSONArray("colors")
                val colorsList = mutableListOf<String>()
                if (colorsArray != null) {
                    for (j in 0 until colorsArray.length()) {
                        colorsList.add(colorsArray.optString(j))
                    }
                }
                list.add(
                    Product(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        description = obj.optString("description"),
                        category = obj.optString("category"),
                        price = obj.optDouble("price", 0.0),
                        imageUrl = obj.optString("imageUrl"),
                        images = imagesList,
                        colors = colorsList,
                        rating = obj.optDouble("rating", 5.0),
                        reviewCount = obj.optLong("reviewCount", 0L),
                        sizes = sizesList,
                        inStock = obj.optBoolean("inStock", true),
                        isFeatured = obj.optBoolean("isFeatured", false),
                        mediaFormat = obj.optString("mediaFormat", "1:1"),
                        videoUrl = obj.optString("videoUrl"),
                        isVideo = obj.optBoolean("isVideo", false)
                    )
                )
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "Error reading local products: ${e.message}")
        }
        return list
    }

    private fun saveCustomAnnouncementLocally(announcement: Announcement) {
        val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE) ?: return
        val currentJson = prefs.getString("local_custom_announcements_json", "[]") ?: "[]"
        try {
            val array = JSONArray(currentJson)
            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("id") != announcement.id) {
                    newArray.put(obj)
                }
            }
            val annObj = JSONObject().apply {
                put("id", announcement.id)
                put("title", announcement.title)
                put("message", announcement.message)
                put("type", announcement.type)
                put("imageUrl", announcement.imageUrl)
                put("discountCode", announcement.discountCode)
                put("discountPercent", announcement.discountPercent)
                put("ctaText", announcement.ctaText)
                put("targetCategory", announcement.targetCategory)
                put("isActive", announcement.isActive)
                put("videoUrl", announcement.videoUrl)
                put("isVideo", announcement.hasVideo)
            }
            newArray.put(annObj)
            prefs.edit().putString("local_custom_announcements_json", newArray.toString()).apply()
        } catch (e: Exception) {
            Log.w("EleganRepository", "Error saving announcement locally: ${e.message}")
        }
    }

    private fun getCustomAnnouncementsLocally(): List<Announcement> {
        val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE) ?: return emptyList()
        val currentJson = prefs.getString("local_custom_announcements_json", "[]") ?: "[]"
        val list = mutableListOf<Announcement>()
        try {
            val array = JSONArray(currentJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Announcement(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        message = obj.optString("message"),
                        type = obj.optString("type", "banner"),
                        imageUrl = obj.optString("imageUrl"),
                        discountCode = obj.optString("discountCode"),
                        discountPercent = obj.optDouble("discountPercent", 0.0),
                        ctaText = obj.optString("ctaText", "Ver Más"),
                        targetCategory = obj.optString("targetCategory", "todo"),
                        isActive = obj.optBoolean("isActive", true),
                        videoUrl = obj.optString("videoUrl"),
                        isVideo = obj.optBoolean("isVideo", false)
                    )
                )
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "Error reading local announcements: ${e.message}")
        }
        return list
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
        saveCustomProductLocally(finalProduct)
        return response.id.ifBlank { productId }
    }

    private suspend fun uploadProductMedia(product: Product, productId: String): Product {
        val sourceVideo = product.videoUrl.takeIf { product.isVideo }.orEmpty()
        var generatedThumbnail: File? = null
        try {
            val coverSource = if (product.isVideo) {
                if (product.imageUrl.isNotBlank() && product.imageUrl != sourceVideo) product.imageUrl
                else {
                    val thumbnail = withContext(Dispatchers.IO) { createVideoThumbnail(appContext, sourceVideo) }
                    if (thumbnail != null && thumbnail.exists() && thumbnail.length() > 0) {
                        generatedThumbnail = thumbnail
                        Uri.fromFile(thumbnail).toString()
                    } else getFallbackCoverForCategory(product.category)
                }
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
        } finally { generatedThumbnail?.delete() }
    }

    private suspend fun uploadProductFile(source: String, productId: String, label: String): String {
        if (source.isBlank()) return ""
        if (source.startsWith("https://") || source.startsWith("http://")) return source
        val uri = Uri.parse(source)
        val temp = withContext(Dispatchers.IO) {
            val file = File.createTempFile("elegan_upload_", if (label.contains("video") || source.endsWith(".mp4", true)) ".mp4" else ".jpg", appContext.cacheDir)
            val input = when (uri.scheme) {
                "content" -> appContext.contentResolver.openInputStream(uri)
                "file" -> File(uri.path ?: "").inputStream()
                else -> File(source).takeIf { it.exists() }?.inputStream()
            } ?: throw IllegalArgumentException("No se pudo leer el archivo multimedia seleccionado.")
            input.use { inputStream -> file.outputStream().use { output -> inputStream.copyTo(output) } }
            file
        }
        try {
            val isVideo = label.contains("video") || source.endsWith(".mp4", true)
            val mime = if (isVideo) "video/mp4" else "image/jpeg"
            val part = MultipartBody.Part.createFormData("file", temp.name, temp.asRequestBody(mime.toMediaTypeOrNull()))
            val name = "${productId}_${label}".toRequestBody("text/plain".toMediaTypeOrNull())
            return api.uploadMedia(requireToken(), part, name).url
        } finally { temp.delete() }
    }

    private fun createVideoThumbnail
    private fun createVideoThumbnail(context: Context, videoSource: String): File? {
        if (videoSource.isBlank()) return null
        val uri = Uri.parse(videoSource)
        if (uri.scheme != "content" && uri.scheme != "file") return null

        val retriever = MediaMetadataRetriever()
        val thumbnail = File.createTempFile("product_video_cover_", ".jpg", context.cacheDir)
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val frameTimeUs = (durationMs.coerceAtLeast(0L) / 2) * 1000L
            val frame = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime

            if (frame != null) {
                try {
                    FileOutputStream(thumbnail).use { output ->
                        frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output)
                    }
                    thumbnail
                } finally {
                    frame.recycle()
                }
            } else {
                thumbnail.delete()
                null
            }
        } catch (exception: Exception) {
            thumbnail.delete()
            Log.w("EleganRepository", "No se pudo extraer miniatura del video: ${exception.message}")
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
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
        saveCustomAnnouncementLocally(final)
        return id
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        val deleted = getDeletedAnnouncementIds()
        val local = getCustomAnnouncementsLocally().filterNot { it.id in deleted }
        val samples = getInitialSampleAnnouncements().filterNot { it.id in deleted }
        emit((local + samples).distinctBy { it.id })
        while (true) {
            try {
                val remote = api.announcements().items.filterNot { it.id in getDeletedAnnouncementIds() }
                val currentLocal = getCustomAnnouncementsLocally().filterNot { it.id in getDeletedAnnouncementIds() }
                val combined = (currentLocal + remote).distinctBy { it.id }
                emit(if (combined.isEmpty()) samples else combined)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudieron actualizar los anuncios: ${e.message}") }
            delay(15000)
        }
    }

    suspend fun deleteProduct(productId: String) {
        markProductDeleted(productId)
        api.deleteProduct(requireToken(), productId)
    }

    suspend fun toggleProductStock(productId: String, inStock: Boolean) {
        api.updateProduct(requireToken(), productId, mapOf("inStock" to inStock))
    }

    suspend fun deleteAnnouncement(announcementId: String) {
        markAnnouncementDeleted(announcementId)
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
            try {
                val stats = api.publicationStats().stats.mapValues { (id, value) -> value.copy(id = id) }
                emit(stats)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudieron cargar estadísticas: ${e.message}") }
            delay(15000)
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
        return api.toggleLike(requireToken(), productId).liked
    }

    fun observePublicationComments(productId: String): Flow<List<PublicationComment>> = flow {
        require(productId.isNotBlank())
        while (true) {
            try { emit(api.comments(productId).items) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w("EleganRepository", "No se pudieron cargar comentarios: ${e.message}") }
            delay(12000)
        }
    }

    suspend fun addPublicationComment(productId: String, displayName: String, text: String) {
        val clean = text.trim()
        require(productId.isNotBlank() && clean.isNotEmpty() && clean.length <= 1000)
        api.addComment(requireToken(), productId, CommentRequest(displayName.trim().ifBlank { "Cliente" }.take(80), clean))
    }

    suspend fun recordPublicationShare(productId: String) {
        require(productId.isNotBlank())
        api.share(requireToken(), productId)
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
