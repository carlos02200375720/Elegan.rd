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
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
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
        emit(getInitialSampleProducts().filter { it.id !in deletedIds })
        db.collection(path)
            .snapshots()
            .map { snapshot ->
                val currentDeleted = getDeletedProductIds()
                val list = snapshot.toObjects(Product::class.java).filter { it.id !in currentDeleted }
                val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE)
                val isSeeded = prefs?.getBoolean("products_seeded", false) ?: false
                if (list.isEmpty()) {
                    if (isSeeded) emptyList() else getInitialSampleProducts().filter { it.id !in currentDeleted }
                } else {
                    list
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(getInitialSampleProducts().filter { it.id !in getDeletedProductIds() })
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

    suspend fun addProduct(product: Product): String {
        val docRef = if (product.id.isNotBlank()) {
            db.collection("products").document(product.id)
        } else {
            db.collection("products").document()
        }
        val productWithUploadedMedia = uploadProductMedia(product, docRef.id)
        val data = mapOf(
            "name" to productWithUploadedMedia.name,
            "description" to productWithUploadedMedia.description,
            "category" to productWithUploadedMedia.category,
            "price" to productWithUploadedMedia.price,
            "imageUrl" to productWithUploadedMedia.imageUrl,
            "images" to productWithUploadedMedia.images,
            "rating" to productWithUploadedMedia.rating,
            "reviewCount" to productWithUploadedMedia.reviewCount,
            "sizes" to productWithUploadedMedia.sizes,
            "inStock" to productWithUploadedMedia.inStock,
            "isFeatured" to productWithUploadedMedia.isFeatured,
            "mediaFormat" to productWithUploadedMedia.mediaFormat,
            "videoUrl" to productWithUploadedMedia.videoUrl,
            "isVideo" to productWithUploadedMedia.isVideo,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        return docRef.id
    }

    private suspend fun uploadProductMedia(product: Product, productId: String): Product {
        val appContext = context
            ?: throw IllegalStateException("No hay contexto disponible para subir los archivos del producto.")
        val sourceVideo = product.videoUrl.takeIf { product.isVideo }.orEmpty()
        var generatedThumbnail: File? = null

        try {
            val coverSource = product.imageUrl.ifBlank {
                if (product.isVideo) {
                    val thumbnail = withContext(Dispatchers.IO) {
                        createVideoThumbnail(appContext, sourceVideo)
                    }
                    generatedThumbnail = thumbnail
                    Uri.fromFile(thumbnail).toString()
                } else {
                    product.images.firstOrNull().orEmpty()
                }
            }
            val uploadedVideoUrl = if (product.isVideo) {
                uploadProductFile(sourceVideo, productId, "video")
            } else {
                ""
            }
            val uploadedCoverUrl = coverSource.takeIf { it.isNotBlank() }
                ?.let { uploadProductFile(it, productId, "cover") }
                .orEmpty()
            val uploadedGallery = (product.images + listOfNotNull(product.imageUrl.takeIf { it.isNotBlank() }))
                .distinct()
                .filterNot { it == coverSource }
                .mapIndexed { index, image -> uploadProductFile(image, productId, "gallery_$index") }
            val normalizedImages = (listOf(uploadedCoverUrl) + uploadedGallery)
                .filter { it.isNotBlank() }
                .distinct()

            return product.copy(
                imageUrl = uploadedCoverUrl,
                images = normalizedImages,
                videoUrl = uploadedVideoUrl
            )
        } finally {
            generatedThumbnail?.delete()
        }
    }

    private suspend fun uploadProductFile(source: String, productId: String, label: String): String {
        if (source.isBlank()) {
            throw IllegalArgumentException("Falta seleccionar un archivo multimedia del producto.")
        }

        val uri = Uri.parse(source)
        if (uri.scheme == "https" || uri.scheme == "http") return source
        if (uri.scheme != "content" && uri.scheme != "file") {
            throw IllegalArgumentException("El archivo seleccionado no tiene una ubicación válida.")
        }

        val appContext = context
            ?: throw IllegalStateException("No hay contexto disponible para subir los archivos del producto.")
        val mimeType = appContext.contentResolver.getType(uri)
            ?: if (label == "video") "video/mp4" else "image/jpeg"
        val extension = android.webkit.MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.takeIf { it.isNotBlank() }
            ?: if (label == "video") "mp4" else "jpg"
        val storageRef = FirebaseStorage.getInstance()
            .reference
            .child("product-media/$productId/${label}_${UUID.randomUUID()}.$extension")
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()

        storageRef.putFile(uri, metadata).await()
        return storageRef.downloadUrl.await().toString()
    }

    private fun createVideoThumbnail(context: Context, videoSource: String): File {
        val uri = Uri.parse(videoSource)
        if (uri.scheme != "content" && uri.scheme != "file") {
            throw IllegalArgumentException("Selecciona una foto de portada para este video.")
        }

        val retriever = MediaMetadataRetriever()
        val thumbnail = File.createTempFile("product_video_cover_", ".jpg", context.cacheDir)
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            val frameTimeUs = durationMs.coerceAtLeast(0L) * 100L
            val frame = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: throw IllegalStateException("No se pudo crear la portada del video. Selecciona una foto de portada.")
            try {
                FileOutputStream(thumbnail).use { output ->
                    if (!frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output)) {
                        throw IllegalStateException("No se pudo guardar la portada del video.")
                    }
                }
            } finally {
                frame.recycle()
            }
            return thumbnail
        } catch (exception: Exception) {
            thumbnail.delete()
            Log.e("EleganRepository", "No se pudo generar la portada del video del producto.", exception)
            throw exception
        } finally {
            retriever.release()
        }
    }

    suspend fun addAnnouncement(announcement: Announcement): String {
        val docRef = if (announcement.id.isNotBlank()) {
            db.collection("announcements").document(announcement.id)
        } else {
            db.collection("announcements").document()
        }
        val data = mapOf(
            "title" to announcement.title,
            "message" to announcement.message,
            "type" to announcement.type,
            "imageUrl" to announcement.imageUrl,
            "videoUrl" to announcement.videoUrl,
            "isVideo" to announcement.isVideo,
            "discountCode" to announcement.discountCode,
            "discountPercent" to announcement.discountPercent,
            "ctaText" to announcement.ctaText,
            "targetCategory" to announcement.targetCategory,
            "isActive" to announcement.isActive,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        return docRef.id
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        val path = "announcements"
        val deletedIds = getDeletedAnnouncementIds()
        emit(getInitialSampleAnnouncements().filter { it.id !in deletedIds })
        db.collection(path)
            .snapshots()
            .map { snapshot ->
                val currentDeleted = getDeletedAnnouncementIds()
                val list = snapshot.toObjects(Announcement::class.java).filter { it.id !in currentDeleted }
                val prefs = context?.getSharedPreferences("elegan_admin_prefs", Context.MODE_PRIVATE)
                val isSeeded = prefs?.getBoolean("announcements_seeded", false) ?: false
                if (list.isEmpty()) {
                    if (isSeeded) emptyList() else getInitialSampleAnnouncements().filter { it.id !in currentDeleted }
                } else {
                    list
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(getInitialSampleAnnouncements().filter { it.id !in getDeletedAnnouncementIds() })
            }
            .collect { emit(it) }
    }

    suspend fun deleteProduct(productId: String) {
        val path = "products/$productId"
        markProductDeleted(productId)
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

        try {
            if (isSaved) {
                savedRef.set(
                    mapOf(
                        "userId" to uid,
                        "productId" to productId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                savedRef.delete().await()
            }
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

    suspend fun addToCart(product: Product, selectedSize: String, quantity: Long = 1L) {
        val uid = requireUserId()
        val cartRef = db.collection("users").document(uid).collection("cart")
        val cartItemId = "${product.id}_$selectedSize"
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
