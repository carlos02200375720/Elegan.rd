package com.example.data.remote

import com.example.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

data class ApiItems<T>(val items: List<T> = emptyList())
data class ApiItem<T>(val item: T? = null)
data class ApiIds(val ids: List<String> = emptyList())
data class ApiStats(val stats: Map<String, PublicationStats> = emptyMap())
data class ApiId(val id: String = "")
data class ApiSuccess(val success: Boolean = false)
data class SavedRequest(val saved: Boolean)
data class QuantityRequest(val quantity: Long)
data class CommentRequest(val displayName: String, val text: String)
data class MessageRequest(val text: String)
data class ShareResponse(val success: Boolean = false)
data class LikeResponse(val liked: Boolean = false)
data class SaveResponse(val saved: Boolean = false)
data class MediaResponse(val url: String, val objectName: String, val contentType: String, val size: Long)

interface EleganStoreApi {
    @GET("api/movil/products")
    suspend fun products(): ApiItems<Product>

    @GET("api/movil/products/{id}")
    suspend fun product(@Path("id") id: String): ApiItem<Product>

    @POST("api/movil/products")
    suspend fun createProduct(@Header("Authorization") token: String, @Body product: Product): ApiId

    @PATCH("api/movil/products/{id}")
    suspend fun updateProduct(@Header("Authorization") token: String, @Path("id") id: String, @Body product: Map<String, Any?>): ApiSuccess

    @DELETE("api/movil/products/{id}")
    suspend fun deleteProduct(@Header("Authorization") token: String, @Path("id") id: String): ApiSuccess

    @GET("api/movil/announcements")
    suspend fun announcements(): ApiItems<Announcement>

    @POST("api/movil/announcements")
    suspend fun createAnnouncement(@Header("Authorization") token: String, @Body announcement: Announcement): ApiId

    @PATCH("api/movil/announcements/{id}")
    suspend fun updateAnnouncement(@Header("Authorization") token: String, @Path("id") id: String, @Body announcement: Map<String, Any?>): ApiSuccess

    @DELETE("api/movil/announcements/{id}")
    suspend fun deleteAnnouncement(@Header("Authorization") token: String, @Path("id") id: String): ApiSuccess

    @GET("api/movil/cart")
    suspend fun cart(@Header("Authorization") token: String): ApiItems<CartItem>

    @POST("api/movil/cart")
    suspend fun addCartItem(@Header("Authorization") token: String, @Body item: CartItem): ApiId

    @PATCH("api/movil/cart/{id}")
    suspend fun updateCartItem(@Header("Authorization") token: String, @Path("id") id: String, @Body body: QuantityRequest): ApiSuccess

    @DELETE("api/movil/cart/{id}")
    suspend fun removeCartItem(@Header("Authorization") token: String, @Path("id") id: String): ApiSuccess

    @DELETE("api/movil/cart")
    suspend fun clearCart(@Header("Authorization") token: String): ApiSuccess

    @GET("api/movil/saved")
    suspend fun saved(@Header("Authorization") token: String): ApiIds

    @PUT("api/movil/saved/{productId}")
    suspend fun setSaved(@Header("Authorization") token: String, @Path("productId") productId: String, @Body body: SavedRequest): SaveResponse

    @GET("api/movil/publications/stats")
    suspend fun publicationStats(): ApiStats

    @GET("api/movil/publications/likes")
    suspend fun liked(@Header("Authorization") token: String): ApiIds

    @PUT("api/movil/publications/{productId}/like")
    suspend fun toggleLike(@Header("Authorization") token: String, @Path("productId") productId: String): LikeResponse

    @GET("api/movil/publications/{productId}/comments")
    suspend fun comments(@Path("productId") productId: String): ApiItems<PublicationComment>

    @POST("api/movil/publications/{productId}/comments")
    suspend fun addComment(@Header("Authorization") token: String, @Path("productId") productId: String, @Body body: CommentRequest): ApiId

    @POST("api/movil/publications/{productId}/share")
    suspend fun share(@Header("Authorization") token: String, @Path("productId") productId: String): ShareResponse

    @GET("api/movil/orders")
    suspend fun orders(@Header("Authorization") token: String): ApiItems<Order>

    @POST("api/movil/orders")
    suspend fun createOrder(@Header("Authorization") token: String, @Body order: Order): ApiId

    @GET("api/movil/messages")
    suspend fun messages(@Header("Authorization") token: String): ApiItems<SupportMessage>

    @POST("api/movil/messages")
    suspend fun sendMessage(@Header("Authorization") token: String, @Body body: MessageRequest): ApiId

    @GET("api/movil/profile")
    suspend fun profile(@Header("Authorization") token: String): ApiItem<UserProfile>

    @PUT("api/movil/profile")
    suspend fun saveProfile(@Header("Authorization") token: String, @Body profile: UserProfile): ApiSuccess

    @Multipart
    @POST("api/movil/media")
    suspend fun uploadMedia(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("name") name: RequestBody
    ): MediaResponse
}
