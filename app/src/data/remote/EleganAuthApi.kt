package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class RegisterRequest(val name: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class GoogleAuthRequest(val idToken: String)
data class AuthUser(
    val id: String,
    val email: String?,
    val name: String,
    val photoUrl: String,
    val role: String,
    val provider: String
)
data class AuthResponse(val token: String, val user: AuthUser)
data class CurrentUserResponse(val user: AuthUser)

interface EleganAuthApi {
    @POST("api/movil/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/movil/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/movil/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleAuthRequest): AuthResponse

    @GET("api/movil/auth/me")
    suspend fun currentUser(@Header("Authorization") bearerToken: String): CurrentUserResponse

    @POST("api/movil/auth/logout")
    suspend fun logout(@Header("Authorization") bearerToken: String): retrofit2.Response<Unit>
}

object EleganApiClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()


    val store: EleganStoreApi by lazy {
        val configuredUrl = BuildConfig.ELEGAN_API_BASE_URL
        require(configuredUrl.startsWith("https://")) {
            "ELEGAN_API_BASE_URL must be configured with the HTTPS Cloud Run URL"
        }
        Retrofit.Builder()
            .baseUrl(configuredUrl.trimEnd('/') + "/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EleganStoreApi::class.java)
    }

    val auth: EleganAuthApi by lazy {
        val configuredUrl = BuildConfig.ELEGAN_API_BASE_URL
        require(configuredUrl.startsWith("https://")) {
            "ELEGAN_API_BASE_URL must be configured with the HTTPS Cloud Run URL"
        }
        Retrofit.Builder()
            .baseUrl(configuredUrl.trimEnd('/') + "/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EleganAuthApi::class.java)
    }
}
