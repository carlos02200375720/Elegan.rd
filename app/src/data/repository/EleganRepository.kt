package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class EleganRepository(
    private val db: FirebaseFirestore,
    private val context: Context? = null
) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        context.applicationContext
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

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
        val path = "products"
        val deletedIds = getDeletedProductIds()
        val localCustom = getCustomProductsLocally().filter { it.id !in deletedIds }
        val initialSamples = getInitialSampleProducts().filter { it.id !in deletedIds }
        emit((localCustom + initialSamples).distinctBy { it.id })

        db.collection(path)
            .snapshots()
            .map { snapshot ->
                val currentDeleted = getDeletedProductIds()
                val firestoreList = snapshot.toObjects(Product::class.java).filter { it.id !in currentDeleted }
                val currentLocal = getCustomProductsLocally().filter { it.id !in currentDeleted }
                val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE)
                val isSeeded = prefs?.getBoolean("products_seeded", false) ?: false

                val combined = (currentLocal + firestoreList).distinctBy { it.id }
                if (combined.isEmpty()) {
                    if (isSeeded) emptyList() else (currentLocal + getInitialSampleProducts()).filter { it.id !in currentDeleted }.distinctBy { it.id }
                } else {
                    combined
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                val currentDeleted = getDeletedProductIds()
                val currentLocal = getCustomProductsLocally().filter { it.id !in currentDeleted }
                emit((currentLocal + getInitialSampleProducts()).filter { it.id !in currentDeleted }.distinctBy { it.id })
            }
            .collect { emit(it) }
    }

    suspend fun seedProductsIfEmpty() {
        val path = "products"
        try {
            val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE)
            val alreadySeeded = prefs?.getBoolean("products_seeded", false) ?: false
            if (alreadySeeded) return

            val snapshot = db.collection(path).limit(1).get().await()
            if (snapshot.isEmpty) {
                val samples = getInitialSampleProducts()
                val deleted = getDeletedProductIds()
                for (item in samples) {
                    if (item.id in deleted) continue
                    val docRef = db.collection(path).document(item.id)
                    val data = mapOf(
                        "name" to item.name,
                        "description" to item.description,
                        "category" to item.category,
                        "price" to item.price,
                        "imageUrl" to item.imageUrl,
                        "rating" to item.rating,
                        "reviewCount" to item.reviewCount,
                        "sizes" to item.sizes,
                        "inStock" to item.inStock,
                        "isFeatured" to item.isFeatured,
                        "mediaFormat" to item.mediaFormat,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(data).await()
                }
            }
            prefs?.edit()?.putBoolean("products_seeded", true)?.apply()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun seedAnnouncementsIfEmpty() {
        val path = "announcements"
        try {
            val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE)
            val alreadySeeded = prefs?.getBoolean("announcements_seeded", false) ?: false
            if (alreadySeeded) return

            val snapshot = db.collection(path).limit(1).get().await()
            if (snapshot.isEmpty) {
                val samples = getInitialSampleAnnouncements()
                val deleted = getDeletedAnnouncementIds()
                for (item in samples) {
                    if (item.id in deleted) continue
                    val docRef = db.collection(path).document(item.id)
                    val data = mapOf(
                        "title" to item.title,
                        "message" to item.message,
                        "type" to item.type,
                        "imageUrl" to item.imageUrl,
                        "discountCode" to item.discountCode,
                        "discountPercent" to item.discountPercent,
                        "ctaText" to item.ctaText,
                        "targetCategory" to item.targetCategory,
                        "isActive" to item.isActive,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(data).await()
                }
            }
            prefs?.edit()?.putBoolean("announcements_seeded", true)?.apply()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    suspend fun getProductById(productId: String): Product? {
        val path = "products/$productId"
        return try {
            val doc = db.collection("products").document(productId).get().await()
            doc.toObject(Product::class.java) ?: getInitialSampleProducts().find { it.id == productId }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            getInitialSampleProducts().find { it.id == productId }
        }
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
        val docRef = if (product.id.isNotBlank()) {
            db.collection("products").document(product.id)
        } else {
            db.collection("products").document()
        }
        val safeCategory = product.category.ifBlank { "camisa" }
        val productWithUploadedMedia = uploadProductMedia(product.copy(id = docRef.id, category = safeCategory), docRef.id)
        val safeImageUrl = productWithUploadedMedia.imageUrl.ifBlank {
            getFallbackCoverForCategory(productWithUploadedMedia.category)
        }
        val safeVideoUrl = productWithUploadedMedia.videoUrl.ifBlank {
            if (product.hasVideo) product.videoUrl else ""
        }
        val isVideoFlag = productWithUploadedMedia.hasVideo || safeVideoUrl.isNotBlank()

        val finalProduct = productWithUploadedMedia.copy(
            id = docRef.id,
            imageUrl = safeImageUrl,
            category = safeCategory,
            videoUrl = safeVideoUrl,
            isVideo = isVideoFlag
        )

        // Guardar de inmediato en almacenamiento local para que siempre aparezca en la app
        saveCustomProductLocally(finalProduct)

        try {
            val data = mapOf(
                "name" to finalProduct.name,
                "description" to finalProduct.description,
                "category" to finalProduct.category,
                "price" to finalProduct.price,
                "imageUrl" to safeImageUrl,
                "images" to finalProduct.images,
                "colors" to finalProduct.colors,
                "rating" to finalProduct.rating,
                "reviewCount" to finalProduct.reviewCount,
                "sizes" to finalProduct.sizes,
                "inStock" to finalProduct.inStock,
                "isFeatured" to finalProduct.isFeatured,
                "mediaFormat" to finalProduct.mediaFormat,
                "videoUrl" to safeVideoUrl,
                "isVideo" to isVideoFlag,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
        } catch (e: Exception) {
            Log.w("EleganRepository", "Firestore write error (product preserved locally): ${e.message}")
        }
        return docRef.id
    }

    private suspend fun uploadProductMedia(product: Product, productId: String): Product {
        val appContext = context
            ?: throw IllegalStateException("No hay contexto disponible para publicar el producto.")
        val sourceVideo = product.videoUrl.takeIf { product.isVideo }.orEmpty()
        var generatedThumbnail: File? = null

        try {
            val coverSource = if (product.isVideo) {
                if (product.imageUrl.isNotBlank() && product.imageUrl != sourceVideo) {
                    product.imageUrl
                } else {
                    val thumbnail = withContext(Dispatchers.IO) {
                        createVideoThumbnail(appContext, sourceVideo)
                    }
                    if (thumbnail != null && thumbnail.exists() && thumbnail.length() > 0) {
                        generatedThumbnail = thumbnail
                        Uri.fromFile(thumbnail).toString()
                    } else {
                        getFallbackCoverForCategory(product.category)
                    }
                }
            } else {
                product.imageUrl.ifBlank {
                    product.images.firstOrNull() ?: getFallbackCoverForCategory(product.category)
                }
            }

            val uploadedVideoUrl = if (product.isVideo && sourceVideo.isNotBlank()) {
                uploadProductFile(sourceVideo, productId, "video")
            } else {
                ""
            }

            val uploadedCoverUrl = if (coverSource.isNotBlank()) {
                uploadProductFile(coverSource, productId, "cover").ifBlank {
                    getFallbackCoverForCategory(product.category)
                }
            } else {
                getFallbackCoverForCategory(product.category)
            }

            val uploadedGallery = (product.images + listOfNotNull(product.imageUrl.takeIf { it.isNotBlank() }))
                .distinct()
                .filterNot { it == coverSource || it == sourceVideo }
                .mapIndexed { index, image -> uploadProductFile(image, productId, "gallery_$index") }

            val normalizedImages = (listOf(uploadedCoverUrl) + uploadedGallery)
                .filter { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }
                .distinct()

            return product.copy(
                imageUrl = uploadedCoverUrl.ifBlank { getFallbackCoverForCategory(product.category) },
                images = if (normalizedImages.isNotEmpty()) normalizedImages else listOf(uploadedCoverUrl.ifBlank { getFallbackCoverForCategory(product.category) }),
                videoUrl = uploadedVideoUrl.ifBlank { sourceVideo },
                isVideo = (uploadedVideoUrl.isNotBlank() || product.isVideo)
            )
        } finally {
            try {
                generatedThumbnail?.delete()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun uploadProductFile(source: String, productId: String, label: String): String {
        if (source.isBlank()) return ""

        val uri = Uri.parse(source)
        if (uri.scheme == "https" || uri.scheme == "http") return source

        val appContext = context ?: return source
        val isVideo = (label == "video" || label == "announcement_video" || source.endsWith(".mp4", ignoreCase = true))
        val localPersistedUriStr = persistMediaLocally(
            appContext = appContext,
            sourceUri = uri,
            productId = productId,
            label = label,
            isVideo = isVideo
        )

        return try {
            val extension = if (isVideo) "mp4" else "jpg"
            val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
            val safeProductId = productId.ifBlank { "general" }
            val objectName = "media/${safeProductId}/${label}_${System.currentTimeMillis()}.${extension}"

            val gcsUrl = com.example.data.storage.GcsStorageManager.getInstance(appContext)
                .uploadFile(localPersistedUriStr, objectName, mimeType)

            if (!gcsUrl.isNullOrBlank()) {
                Log.i("EleganRepository", "Contenido subido exitosamente a bucket-elegan-android: $gcsUrl")
                gcsUrl
            } else {
                Log.w("EleganRepository", "GCS devolvió nulo, manteniendo respaldo local: $localPersistedUriStr")
                localPersistedUriStr
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "Error subiendo a bucket-elegan-android, usando respaldo local: ${e.message}")
            localPersistedUriStr
        }
    }

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
        val docRef = if (announcement.id.isNotBlank()) {
            db.collection("announcements").document(announcement.id)
        } else {
            db.collection("announcements").document()
        }
        val safeImageUrl = if (announcement.imageUrl.isNotBlank() && (announcement.imageUrl.startsWith("content://") || announcement.imageUrl.startsWith("file://"))) {
            uploadProductFile(announcement.imageUrl, docRef.id, "announcement_img")
        } else {
            announcement.imageUrl.ifBlank { getFallbackCoverForCategory(announcement.targetCategory) }
        }
        val safeVideoUrl = if (announcement.videoUrl.isNotBlank() && (announcement.videoUrl.startsWith("content://") || announcement.videoUrl.startsWith("file://"))) {
            uploadProductFile(announcement.videoUrl, docRef.id, "announcement_video")
        } else {
            announcement.videoUrl
        }
        val isVideoFlag = announcement.hasVideo || safeVideoUrl.isNotBlank()

        val finalAnnouncement = announcement.copy(
            id = docRef.id,
            imageUrl = safeImageUrl,
            videoUrl = safeVideoUrl,
            isVideo = isVideoFlag
        )
        saveCustomAnnouncementLocally(finalAnnouncement)

        try {
            val data = mapOf(
                "title" to finalAnnouncement.title,
                "message" to finalAnnouncement.message,
                "type" to finalAnnouncement.type,
                "imageUrl" to safeImageUrl,
                "videoUrl" to safeVideoUrl,
                "isVideo" to isVideoFlag,
                "discountCode" to finalAnnouncement.discountCode,
                "discountPercent" to finalAnnouncement.discountPercent,
                "ctaText" to finalAnnouncement.ctaText,
                "targetCategory" to finalAnnouncement.targetCategory,
                "isActive" to finalAnnouncement.isActive,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
        } catch (e: Exception) {
            Log.w("EleganRepository", "Firestore announcement write error (preserved locally): ${e.message}")
        }
        return docRef.id
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        val path = "announcements"
        val deletedIds = getDeletedAnnouncementIds()
        val localCustom = getCustomAnnouncementsLocally().filter { it.id !in deletedIds }
        val initialSamples = getInitialSampleAnnouncements().filter { it.id !in deletedIds }
        emit((localCustom + initialSamples).distinctBy { it.id })

        db.collection(path)
            .snapshots()
            .map { snapshot ->
                val currentDeleted = getDeletedAnnouncementIds()
                val firestoreList = snapshot.toObjects(Announcement::class.java).filter { it.id !in currentDeleted }
                val currentLocal = getCustomAnnouncementsLocally().filter { it.id !in currentDeleted }
                val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE)
                val isSeeded = prefs?.getBoolean("announcements_seeded", false) ?: false

                val combined = (currentLocal + firestoreList).distinctBy { it.id }
                if (combined.isEmpty()) {
                    if (isSeeded) emptyList() else (currentLocal + getInitialSampleAnnouncements()).filter { it.id !in currentDeleted }.distinctBy { it.id }
                } else {
                    combined
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                val currentDeleted = getDeletedAnnouncementIds()
                val currentLocal = getCustomAnnouncementsLocally().filter { it.id !in currentDeleted }
                emit((currentLocal + getInitialSampleAnnouncements()).filter { it.id !in currentDeleted }.distinctBy { it.id })
            }
            .collect { emit(it) }
    }

    suspend fun deleteProduct(productId: String) {
        val path = "products/$productId"
        markProductDeleted(productId)
        val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE)
        prefs?.let { p ->
            try {
                val currentJson = p.getString("local_custom_products_json", "[]") ?: "[]"
                val array = JSONArray(currentJson)
                val newArray = JSONArray()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.optString("id") != productId) {
                        newArray.put(obj)
                    }
                }
                p.edit().putString("local_custom_products_json", newArray.toString()).apply()
            } catch (_: Exception) {}
        }
        try {
            db.collection("products").document(productId).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
        }
    }

    suspend fun toggleProductStock(productId: String, inStock: Boolean) {
        val path = "products/$productId"
        try {
            db.collection("products").document(productId)
                .update("inStock", inStock)
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun deleteAnnouncement(announcementId: String) {
        val path = "announcements/$announcementId"
        markAnnouncementDeleted(announcementId)
        val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE)
        prefs?.let { p ->
            try {
                val currentJson = p.getString("local_custom_announcements_json", "[]") ?: "[]"
                val array = JSONArray(currentJson)
                val newArray = JSONArray()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.optString("id") != announcementId) {
                        newArray.put(obj)
                    }
                }
                p.edit().putString("local_custom_announcements_json", newArray.toString()).apply()
            } catch (_: Exception) {}
        }
        try {
            db.collection("announcements").document(announcementId).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
        }
    }

    suspend fun toggleAnnouncementActive(announcementId: String, isActive: Boolean) {
        val path = "announcements/$announcementId"
        try {
            db.collection("announcements").document(announcementId)
                .update("isActive", isActive)
                .await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
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

    fun observeCart(): Flow<List<CartItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/cart"
        db.collection("users").document(uid).collection("cart")
            .snapshots()
            .map { it.toObjects(CartItem::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
            .collect { emit(it) }
    }

    fun observeSavedProductIds(): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/savedPublications"
        db.collection("users").document(uid).collection("savedPublications")
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun setProductSaved(productId: String, isSaved: Boolean) {
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        val uid = requireUserId()
        val savedRef = db.collection("users").document(uid)
            .collection("savedPublications").document(productId)
        val statsRef = db.collection("publicationStats").document(productId)

        try {
            db.runTransaction { transaction ->
                val savedSnapshot = transaction.get(savedRef)
                val statsSnapshot = transaction.get(statsRef)
                val likesCount = statsSnapshot.getLong("likesCount") ?: 0L
                val commentsCount = statsSnapshot.getLong("commentsCount") ?: 0L
                val sharesCount = statsSnapshot.getLong("sharesCount") ?: 0L
                val savesCount = statsSnapshot.getLong("savesCount") ?: 0L
                val shouldSave = isSaved && !savedSnapshot.exists()
                val shouldUnsave = !isSaved && savedSnapshot.exists()

                if (shouldSave) {
                    transaction.set(
                        savedRef,
                        mapOf(
                            "productId" to productId,
                            "userId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else if (shouldUnsave) {
                    transaction.delete(savedRef)
                }

                if (shouldSave || shouldUnsave) {
                    transaction.set(
                        statsRef,
                        mapOf(
                            "likesCount" to likesCount,
                            "commentsCount" to commentsCount,
                            "sharesCount" to sharesCount,
                            "savesCount" to (savesCount + if (shouldSave) 1L else -1L).coerceAtLeast(0L),
                            "lastCommentId" to (statsSnapshot.getString("lastCommentId") ?: "")
                        ),
                        SetOptions.merge()
                    )
                }
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (isSaved) OperationType.WRITE else OperationType.DELETE,
                savedRef.path
            )
            throw e
        }
    }

    fun observePublicationStats(): Flow<Map<String, PublicationStats>> = flow {
        requireUserId()
        db.collection("publicationStats")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { document ->
                    document.toObject(PublicationStats::class.java)?.let { stats ->
                        document.id to stats
                    }
                }.toMap()
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, "publicationStats")
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeLikedPublicationIds(): Flow<Set<String>> = callbackFlow {
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                flowOf(emptySet())
            } else {
                val path = "users/$uid/likedPublications"
                db.collection("users").document(uid).collection("likedPublications")
                    .snapshots()
                    .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
                    .catch { error ->
                        if (error is Exception) {
                            handleFirestoreError(error, OperationType.LIST, path)
                        }
                        throw error
                    }
            }
        }

    suspend fun togglePublicationLike(productId: String): Boolean {
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        val uid = requireUserId()
        val likedRef = db.collection("users").document(uid)
            .collection("likedPublications").document(productId)
        val statsRef = db.collection("publicationStats").document(productId)

        try {
            return db.runTransaction { transaction ->
                val likedSnapshot = transaction.get(likedRef)
                val statsSnapshot = transaction.get(statsRef)
                val likesCount = statsSnapshot.getLong("likesCount") ?: 0L
                val commentsCount = statsSnapshot.getLong("commentsCount") ?: 0L
                val isLiked = !likedSnapshot.exists()

                if (isLiked) {
                    transaction.set(
                        likedRef,
                        mapOf("userId" to uid, "productId" to productId)
                    )
                } else {
                    transaction.delete(likedRef)
                }

                transaction.set(
                    statsRef,
                    mapOf(
                        "likesCount" to (likesCount + if (isLiked) 1L else -1L).coerceAtLeast(0L),
                        "commentsCount" to commentsCount,
                        "sharesCount" to (statsSnapshot.getLong("sharesCount") ?: 0L),
                        "savesCount" to (statsSnapshot.getLong("savesCount") ?: 0L),
                        "lastCommentId" to (statsSnapshot.getString("lastCommentId") ?: "")
                    ),
                    SetOptions.merge()
                )
                isLiked
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, statsRef.path)
            throw e
        }
    }

    fun observePublicationComments(productId: String): Flow<List<PublicationComment>> = flow {
        requireUserId()
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        val path = "products/$productId/comments"
        db.collection("products").document(productId).collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(PublicationComment::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun addPublicationComment(
        productId: String,
        displayName: String,
        text: String
    ) {
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        val cleanText = text.trim()
        require(cleanText.isNotEmpty()) { "Comment cannot be blank." }
        require(cleanText.length <= 1000) { "Comment cannot exceed 1000 characters." }
        val uid = requireUserId()
        val safeDisplayName = displayName.trim().ifBlank { "Cliente" }.take(80)
        val commentRef = db.collection("products").document(productId)
            .collection("comments").document()
        val statsRef = db.collection("publicationStats").document(productId)

        try {
            db.runTransaction { transaction ->
                val statsSnapshot = transaction.get(statsRef)
                val likesCount = statsSnapshot.getLong("likesCount") ?: 0L
                val commentsCount = statsSnapshot.getLong("commentsCount") ?: 0L
                transaction.set(
                    commentRef,
                    mapOf(
                        "productId" to productId,
                        "userId" to uid,
                        "displayName" to safeDisplayName,
                        "text" to cleanText,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.set(
                    statsRef,
                    mapOf(
                        "likesCount" to likesCount,
                        "commentsCount" to commentsCount + 1L,
                        "sharesCount" to (statsSnapshot.getLong("sharesCount") ?: 0L),
                        "savesCount" to (statsSnapshot.getLong("savesCount") ?: 0L),
                        "lastCommentId" to commentRef.id
                    ),
                    SetOptions.merge()
                )
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, commentRef.path)
            throw e
        }
    }

    suspend fun recordPublicationShare(productId: String) {
        require(productId.isNotBlank()) { "Product ID cannot be blank." }
        requireUserId()
        val statsRef = db.collection("publicationStats").document(productId)

        try {
            db.runTransaction { transaction ->
                val statsSnapshot = transaction.get(statsRef)
                transaction.set(
                    statsRef,
                    mapOf(
                        "likesCount" to (statsSnapshot.getLong("likesCount") ?: 0L),
                        "commentsCount" to (statsSnapshot.getLong("commentsCount") ?: 0L),
                        "sharesCount" to ((statsSnapshot.getLong("sharesCount") ?: 0L) + 1L),
                        "savesCount" to (statsSnapshot.getLong("savesCount") ?: 0L),
                        "lastCommentId" to (statsSnapshot.getString("lastCommentId") ?: "")
                    ),
                    SetOptions.merge()
                )
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, statsRef.path)
            throw e
        }
    }

    suspend fun addToCart(
        product: Product,
        selectedSize: String,
        quantity: Long = 1L,
        selectedColor: String = ""
    ) {
        val uid = requireUserId()
        val cartRef = db.collection("users").document(uid).collection("cart")
        val cartItemId = "${product.id}_${selectedSize}_${selectedColor}".replace("/", "-")
        val docRef = cartRef.document(cartItemId)

        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val currentQty = existing.getLong("quantity") ?: 1L
                docRef.update(
                    mapOf(
                        "quantity" to currentQty + quantity,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "productId" to product.id,
                    "name" to product.name,
                    "price" to product.price,
                    "imageUrl" to product.imageUrl,
                    "selectedSize" to selectedSize,
                    "selectedColor" to selectedColor,
                    "quantity" to quantity,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateCartQuantity(cartItemId: String, newQuantity: Long) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("cart").document(cartItemId)
        try {
            if (newQuantity <= 0) {
                docRef.delete().await()
            } else {
                docRef.update(
                    mapOf(
                        "quantity" to newQuantity,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun removeFromCart(cartItemId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("cart").document(cartItemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun clearCart() {
        val uid = requireUserId()
        val cartRef = db.collection("users").document(uid).collection("cart")
        try {
            val items = cartRef.get().await()
            for (doc in items.documents) {
                doc.reference.delete().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, cartRef.path)
            throw e
        }
    }

    // -------------------------------------------------------------
    // ORDERS
    // -------------------------------------------------------------

    fun observeOrders(): Flow<List<Order>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/orders"
        db.collection("users").document(uid).collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(Order::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun createOrder(
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        paymentMethod: String,
        total: Double,
        itemCount: Long,
        itemsSummary: String
    ): String {
        val uid = requireUserId()
        val ordersRef = db.collection("users").document(uid).collection("orders")
        val orderDoc = ordersRef.document()
        val payload = mapOf(
            "userId" to uid,
            "customerName" to customerName,
            "email" to email,
            "phone" to phone,
            "address" to address,
            "city" to city,
            "paymentMethod" to paymentMethod,
            "total" to total,
            "itemCount" to itemCount,
            "status" to "confirmado",
            "itemsSummary" to itemsSummary,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            orderDoc.set(payload).await()
            clearCart()
            return orderDoc.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, orderDoc.path)
            throw e
        }
    }

    // -------------------------------------------------------------
    // SUPPORT CHAT
    // -------------------------------------------------------------

    fun observeMessages(): Flow<List<SupportMessage>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/messages"
        db.collection("users").document(uid).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(SupportMessage::class.java)
                if (list.isEmpty()) {
                    listOf(
                        SupportMessage(
                            id = "welcome",
                            userId = uid,
                            sender = "support",
                            senderName = "Concierge Elegan",
                            text = "¡Bienvenido a Elegan! Soy tu asesor personal de estilo y compras. ¿En qué podemos ayudarte hoy?"
                        )
                    )
                } else {
                    list
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }

    suspend fun sendSupportMessage(text: String, scope: CoroutineScope) {
        val uid = requireUserId()
        val messagesRef = db.collection("users").document(uid).collection("messages")
        val userDoc = messagesRef.document()
        val userPayload = mapOf(
            "userId" to uid,
            "sender" to "user",
            "senderName" to (auth.currentUser?.displayName ?: "Cliente"),
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            userDoc.set(userPayload).await()

            // Automated Concierge reply after 1.5s
            scope.launch(Dispatchers.IO) {
                delay(1500)
                try {
                    val botDoc = messagesRef.document()
                    val botReply = when {
                        text.contains("envio", ignoreCase = true) || text.contains("entrega", ignoreCase = true) ->
                            "Nuestros envíos express tardan de 24 a 48 horas en llegar a tu puerta con embalaje de lujo protegido."
                        text.contains("talla", ignoreCase = true) || text.contains("medida", ignoreCase = true) ->
                            "Nuestras prendas tienen corte europeo estilizado (Slim Fit). Si prefieres un ajuste más holgado, te sugerimos pedir una talla superior."
                        text.contains("devolucion", ignoreCase = true) || text.contains("cambio", ignoreCase = true) ->
                            "Dispones de 30 días para cambios gratuitos con recogida a domicilio sin coste adicional."
                        text.contains("descuento", ignoreCase = true) || text.contains("promocion", ignoreCase = true) ->
                            "Puedes utilizar el cupón 'ELEGAN10' en tu carrito para obtener un 10% de descuento de bienvenida."
                        else ->
                            "Gracias por comunicarte con Elegan Concierge. Hemos recibido tu consulta y un asesor especializado está revisando tu solicitud."
                    }
                    val botPayload = mapOf(
                        "userId" to uid,
                        "sender" to "support",
                        "senderName" to "Elegan Concierge",
                        "text" to botReply,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    botDoc.set(botPayload).await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.CREATE, messagesRef.path)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, userDoc.path)
            throw e
        }
    }

    // -------------------------------------------------------------
    // USER PROFILE
    // -------------------------------------------------------------

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        db.collection("users").document(uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(UserProfile::class.java) ?: UserProfile(
                    userId = uid,
                    displayName = auth.currentUser?.displayName ?: "Caballero Elegan",
                    email = auth.currentUser?.email ?: "",
                    photoUrl = auth.currentUser?.photoUrl?.toString() ?: ""
                )
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
            .collect { emit(it) }
    }

    suspend fun saveUserProfile(phone: String, address: String, city: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "displayName" to (auth.currentUser?.displayName ?: "Caballero"),
            "email" to (auth.currentUser?.email ?: ""),
            "photoUrl" to (auth.currentUser?.photoUrl?.toString() ?: ""),
            "phone" to phone,
            "address" to address,
            "city" to city,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // -------------------------------------------------------------
    // CURATED SAMPLE DATA
    // -------------------------------------------------------------

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
