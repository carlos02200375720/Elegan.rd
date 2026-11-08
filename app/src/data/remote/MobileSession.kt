package com.example.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Holds the backend session. The JWT is encrypted at rest and is never stored in source code.
 */
object MobileSession {
    private const val PREFS = "elegan_mobile_session"
    private const val TOKEN = "jwt"
    private const val USER_ID = "user_id"
    private const val EMAIL = "email"
    private const val NAME = "name"
    private const val PHOTO = "photo_url"
    private const val ROLE = "role"

    private fun preferences(context: Context) = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFS,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(context: Context, response: AuthResponse) {
        preferences(context).edit()
            .putString(TOKEN, response.token)
            .putString(USER_ID, response.user.id)
            .putString(EMAIL, response.user.email.orEmpty())
            .putString(NAME, response.user.name)
            .putString(PHOTO, response.user.photoUrl)
            .putString(ROLE, response.user.role)
            .apply()
    }

    fun bearerToken(context: Context): String? =
        preferences(context).getString(TOKEN, null)?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

    fun userId(context: Context): String? =
        preferences(context).getString(USER_ID, null)?.takeIf { it.isNotBlank() }

    fun user(context: Context): AuthUser? {
        val prefs = preferences(context)
        val id = prefs.getString(USER_ID, null) ?: return null
        return AuthUser(
            id = id,
            email = prefs.getString(EMAIL, ""),
            name = prefs.getString(NAME, "").orEmpty(),
            photoUrl = prefs.getString(PHOTO, "").orEmpty(),
            role = prefs.getString(ROLE, "customer").orEmpty(),
            provider = "backend"
        )
    }

    fun clear(context: Context) {
        preferences(context).edit().clear().apply()
    }
}
