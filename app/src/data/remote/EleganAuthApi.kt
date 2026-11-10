package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException

data class RegisterRequest(val name: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class GoogleAuthRequest(val idToken: String)
data class AuthUser(
    val id: String = "",
    val originalId: String? = null,
    val email: String? = null,
    val name: String = "",
    val photoUrl: String = "",
    val avatar: String? = null,
    val role: String = "customer",
    val provider: String = "password",
    val isAdmin: Boolean = false
) {
    val effectivePhotoUrl: String
        get() = photoUrl.ifBlank { avatar.orEmpty() }

    val effectiveId: String
        get() = originalId?.takeIf { it.isNotBlank() } ?: id
}

data class AuthResponse(
    val token: String = "",
    val user: AuthUser = AuthUser(),
    val success: Boolean = true,
    val message: String? = null
)

data class CurrentUserResponse(
    val user: AuthUser = AuthUser()
)

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
    private const val HTTP_LOG_TAG = "ELEGAN_HTTP"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                // Omit query parameters so credentials or private values cannot leak into Logcat.
                val safeUrl = request.url.newBuilder().query(null).build()
                Log.i(HTTP_LOG_TAG, "--> ${request.method} $safeUrl")

                try {
                    val response: Response = chain.proceed(request)
                    Log.i(HTTP_LOG_TAG, "<-- ${response.code} ${request.method} $safeUrl")
                    response
                } catch (error: IOException) {
                    Log.e(
                        HTTP_LOG_TAG,
                        "<-- NETWORK_ERROR ${request.method} $safeUrl (${error.javaClass.simpleName})"
                    )
                    throw error
                }
            })
            .build()
    }

    private fun getSafeBaseUrl(): String {
        val configured = try {
            BuildConfig.ELEGAN_API_BASE_URL?.trim().orEmpty()
        } catch (_: Throwable) { "" }

        return when {
            configured.startsWith("https://") -> configured.trimEnd('/') + "/"
            configured.startsWith("http://") -> configured.trimEnd('/') + "/"
            else -> "https://backend-elegan-rd-587849039182.us-central1.run.app/"
        }
    }

    val store: EleganStoreApi by lazy {
        Retrofit.Builder()
            .baseUrl(getSafeBaseUrl())
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EleganStoreApi::class.java)
    }

    val auth: EleganAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(getSafeBaseUrl())
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EleganAuthApi::class.java)
    }
}
