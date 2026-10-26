package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "todo", // gorra, gafa, camisa, correa, pantalone, zapato, interiore, perfume
    val price: Double = 0.0,
    val imageUrl: String = "",
    val images: List<String> = emptyList(),
    val rating: Double = 5.0,
    val reviewCount: Long = 0L,
    val sizes: List<String> = emptyList(),
    val inStock: Boolean = true,
    val isFeatured: Boolean = false,
    val mediaFormat: String = "1:1", // "1:1", "3:4", "4:3", "9:16", "16:9"
    val videoUrl: String = "",
    val isVideo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

val Product.aspectRatioValue: Float
    get() = when (mediaFormat.trim()) {
        "3:4" -> 3f / 4f
        "4:3" -> 4f / 3f
        "9:16" -> 9f / 16f
        "16:9" -> 16f / 9f
        else -> 1f // default 1:1
    }


@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val selectedSize: String = "",
    val quantity: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val paymentMethod: String = "Tarjeta",
    val total: Double = 0.0,
    val itemCount: Long = 1L,
    val status: String = "confirmado",
    val itemsSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupportMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = "user", // "user" or "support"
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "banner", // "banner", "historia_9_16", "descuento", "coleccion"
    val imageUrl: String = "",
    val discountCode: String = "",
    val discountPercent: Double = 0.0,
    val ctaText: String = "Ver Más",
    val targetCategory: String = "todo",
    val isActive: Boolean = true,
    val videoUrl: String = "",
    val isVideo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class StoreCategory(val id: String, val title: String, val iconName: String) {
    TODO("todo", "Todo", "grid_view"),
    GORRA("gorra", "Gorras & Boinas", "checkroom"),
    GAFA("gafa", "Gafas de Sol", "visibility"),
    CAMISA("camisa", "Camisas & Trajes", "dry_cleaning"),
    CORREA("correa", "Correas de Cuero", "straighten"),
    PANTALONE("pantalone", "Pantalones de Vestir", "styler"),
    ZAPATO("zapato", "Zapatos Oxford", "ice_skating"),
    INTERIORE("interiore", "Ropa Interior", "layers"),
    PERFUME("perfume", "Perfumes & Fragancias", "spa")
}
