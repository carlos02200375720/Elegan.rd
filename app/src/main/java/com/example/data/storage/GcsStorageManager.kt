package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Gestor de almacenamiento en la nube para Google Cloud Storage.
 * Sube imágenes y videos directamente al bucket oficial 'bucket-elegan-android'
 * utilizando la cuenta de servicio autenticada en 'elegan-cuenta-servicio.json'.
 */
class GcsStorageManager private constructor(private val appContext: Context) {

    companion object {
        const val BUCKET_NAME = "bucket-elegan-android"
        private const val TAG = "GcsStorageManager"

        @Volatile
        private var INSTANCE: GcsStorageManager? = null

        fun getInstance(context: Context): GcsStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GcsStorageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private var clientEmail: String = ""
    private var privateKey: PrivateKey? = null
    private var cachedAccessToken: String? = null
    private var tokenExpiryEpochSeconds: Long = 0L

    init {
        loadCredentials()
    }

    private fun loadCredentials() {
        try {
            val jsonString = try {
                appContext.assets.open("elegan-cuenta-servicio.json").use { it.bufferedReader().readText() }
            } catch (_: Exception) {
                val f = File(appContext.filesDir, "elegan-cuenta-servicio.json")
                if (f.exists()) f.readText() else ""
            }

            if (jsonString.isNotBlank()) {
                val json = JSONObject(jsonString)
                clientEmail = json.optString("client_email")
                val rawPrivateKey = json.optString("private_key")
                if (rawPrivateKey.isNotBlank()) {
                    privateKey = parsePrivateKey(rawPrivateKey)
                    Log.i(TAG, "Credenciales de cuenta de servicio cargadas con éxito: $clientEmail")
                }
            } else {
                Log.w(TAG, "No se encontró el archivo elegan-cuenta-servicio.json en assets.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar credenciales de cuenta de servicio: ${e.message}", e)
        }
    }

    private fun parsePrivateKey(pemKey: String): PrivateKey? {
        return try {
            val cleanKey = pemKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s+".toRegex(), "")
            val keyBytes = Base64.decode(cleanKey, Base64.DEFAULT)
            val spec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            keyFactory.generatePrivate(spec)
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando clave privada RSA: ${e.message}", e)
            null
        }
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val nowSeconds = System.currentTimeMillis() / 1000L
        if (cachedAccessToken != null && nowSeconds < (tokenExpiryEpochSeconds - 120)) {
            return@withContext cachedAccessToken
        }

        val pk = privateKey ?: return@withContext null
        val email = clientEmail.takeIf { it.isNotBlank() } ?: return@withContext null

        try {
            val header = base64UrlEncode("""{"alg":"RS256","typ":"JWT"}""".toByteArray(StandardCharsets.UTF_8))
            val claimsJson = """{"iss":"$email","scope":"https://www.googleapis.com/auth/devstorage.read_write","aud":"https://oauth2.googleapis.com/token","exp":${nowSeconds + 3600},"iat":$nowSeconds}"""
            val payload = base64UrlEncode(claimsJson.toByteArray(StandardCharsets.UTF_8))
            val signingInput = "$header.$payload"

            val signer = Signature.getInstance("SHA256withRSA")
            signer.initSign(pk)
            signer.update(signingInput.toByteArray(StandardCharsets.UTF_8))
            val signature = base64UrlEncode(signer.sign())

            val jwt = "$signingInput.$signature"

            val url = URL("https://oauth2.googleapis.com/token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val postData = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8") +
                    "&assertion=" + URLEncoder.encode(jwt, "UTF-8")

            conn.outputStream.use { it.write(postData.toByteArray(StandardCharsets.UTF_8)) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val respJson = JSONObject(responseText)
                val token = respJson.getString("access_token")
                val expiresIn = respJson.optLong("expires_in", 3600L)
                cachedAccessToken = token
                tokenExpiryEpochSeconds = nowSeconds + expiresIn
                Log.d(TAG, "Nuevo token OAuth obtenido con éxito para GCS.")
                token
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Log.e(TAG, "Error al obtener token de Google OAuth ($responseCode): $err")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al solicitar token OAuth de cuenta de servicio: ${e.message}", e)
            null
        }
    }

    /**
     * Sube un archivo (imagen o video) a Google Cloud Storage en el bucket 'bucket-elegan-android'.
     * Retorna la URL pública y firmada de acceso al objeto.
     */
    suspend fun uploadFile(
        sourceUriString: String,
        destinationObjectName: String,
        mimeType: String
    ): String? = withContext(Dispatchers.IO) {
        val token = getAccessToken()
        if (token == null) {
            Log.w(TAG, "No se pudo obtener token de autenticación para subir a GCS.")
            return@withContext null
        }

        val sourceUri = Uri.parse(sourceUriString)
        val uploadUrlStr = "https://storage.googleapis.com/upload/storage/v1/b/$BUCKET_NAME/o?uploadType=media&name=" +
                URLEncoder.encode(destinationObjectName, "UTF-8")

        try {
            val url = URL(uploadUrlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 20000
            conn.readTimeout = 30000
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", mimeType)
            conn.setChunkedStreamingMode(64 * 1024)

            openInputStream(sourceUri)?.use { input ->
                conn.outputStream.use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                Log.w(TAG, "No se pudo abrir el stream de lectura para $sourceUriString")
                return@withContext null
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                Log.i(TAG, "Archivo subido exitosamente a GCS: $BUCKET_NAME/$destinationObjectName")
                // Generar URL firmada V4 con validez de 7 días (máximo permitido en GCS)
                val signedUrl = generateV4SignedUrl(destinationObjectName, 7 * 24 * 3600)
                val finalUrl = signedUrl ?: "https://storage.googleapis.com/$BUCKET_NAME/$destinationObjectName"
                Log.i(TAG, "URL pública generada para multimedia en GCS: $finalUrl")
                finalUrl
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Log.e(TAG, "Error al subir archivo a GCS ($responseCode): $err")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al subir archivo a GCS: ${e.message}", e)
            null
        }
    }

    private fun openInputStream(uri: Uri): InputStream? {
        return try {
            when (uri.scheme) {
                "file" -> File(uri.path ?: "").inputStream()
                "content" -> appContext.contentResolver.openInputStream(uri)
                null -> File(uri.path ?: uri.toString()).inputStream()
                else -> appContext.contentResolver.openInputStream(uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error abriendo inputStream para $uri: ${e.message}")
            null
        }
    }

    private fun rfc3986Encode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")
    }

    /**
     * Genera una URL firmada V4 para permitir la lectura y reproducción del objeto
     * alojado en el bucket 'bucket-elegan-android'.
     */
    fun generateV4SignedUrl(objectName: String, expirySeconds: Long = 604800L): String? {
        val pk = privateKey ?: return null
        val email = clientEmail.takeIf { it.isNotBlank() } ?: return null

        return try {
            val now = Date()
            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val timeFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val dateStr = dateFormat.format(now)
            val timeStr = timeFormat.format(now)

            val credential = "$email/$dateStr/auto/storage/goog4_request"
            val canonicalUri = "/$BUCKET_NAME/$objectName"

            val queryParams = sortedMapOf(
                "X-Goog-Algorithm" to "GOOG4-RSA-SHA256",
                "X-Goog-Credential" to credential,
                "X-Goog-Date" to timeStr,
                "X-Goog-Expires" to expirySeconds.toString(),
                "X-Goog-SignedHeaders" to "host"
            )

            val canonicalQuery = queryParams.entries.joinToString("&") { (k, v) ->
                "${rfc3986Encode(k)}=${rfc3986Encode(v)}"
            }

            val canonicalHeaders = "host:storage.googleapis.com\n"
            val signedHeaders = "host"
            val payloadHash = "UNSIGNED-PAYLOAD"

            val canonicalRequest = "GET\n$canonicalUri\n$canonicalQuery\n$canonicalHeaders\n$signedHeaders\n$payloadHash"

            val md = java.security.MessageDigest.getInstance("SHA-256")
            val requestHash = md.digest(canonicalRequest.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

            val stringToSign = "GOOG4-RSA-SHA256\n$timeStr\n$dateStr/auto/storage/goog4_request\n$requestHash"

            val signer = Signature.getInstance("SHA256withRSA")
            signer.initSign(pk)
            signer.update(stringToSign.toByteArray(StandardCharsets.UTF_8))
            val signatureBytes = signer.sign()
            val signatureHex = signatureBytes.joinToString("") { "%02x".format(it) }

            "https://storage.googleapis.com$canonicalUri?$canonicalQuery&X-Goog-Signature=$signatureHex"
        } catch (e: Exception) {
            Log.e(TAG, "Error generando URL firmada V4 para $objectName: ${e.message}", e)
            null
        }
    }
}
