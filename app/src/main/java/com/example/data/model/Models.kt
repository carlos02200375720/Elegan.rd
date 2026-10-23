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
    val rating: Double = 5.0,
    val reviewCount: Long = 0L,
    val sizes: List<String> = emptyList(),
    val inStock: Boolean = true,
    val isFeatured: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

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
