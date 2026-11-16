package com.example.data.remote

import com.example.data.local.KeystoreCryptoManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * TLS-hardened network provider for NOVA VPN backend and speedtest probes.
 * Rejects insecure cleartext remote URLs (only permitting HTTPS or local loopback dev hosts).
 */
class NetworkModule(private val cryptoManager: KeystoreCryptoManager) {

  companion object {
    private const val TOKEN_KEY = "nova_auth_jwt"
  }

  private val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(20, TimeUnit.SECONDS)
    .addInterceptor { chain ->
      val original = chain.request()
      val token = cryptoManager.getDecryptedString(TOKEN_KEY)
      val builder = original.newBuilder()
        .header("Accept", "application/json")
        .header("User-Agent", "NovaVPN-Android/1.0.0")
      if (!token.isNullOrBlank()) {
        builder.header("Authorization", "Bearer $token")
      }
      chain.proceed(builder.build())
    }
    .build()

  fun isValidSecureBaseUrl(rawUrl: String): Boolean {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return false
    return trimmed.startsWith("https://", ignoreCase = true) ||
      trimmed.startsWith("http://127.0.0.1", ignoreCase = true) ||
      trimmed.startsWith("http://10.0.2.2", ignoreCase = true) ||
      trimmed.startsWith("http://localhost", ignoreCase = true)
  }

  fun createApiService(baseUrl: String): NovaApiService? {
    if (!isValidSecureBaseUrl(baseUrl)) return null
    val normalized = baseUrl.trim().trimEnd('/') + "/"
    return runCatching {
      Retrofit.Builder()
        .baseUrl(normalized)
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(NovaApiService::class.java)
    }.getOrNull()
  }

  fun saveAuthToken(token: String) {
    cryptoManager.putEncryptedString(TOKEN_KEY, token)
  }

  fun clearAuthToken() {
    cryptoManager.removeEncrypted(TOKEN_KEY)
  }

  fun hasAuthToken(): Boolean = !cryptoManager.getDecryptedString(TOKEN_KEY).isNullOrBlank()
}
