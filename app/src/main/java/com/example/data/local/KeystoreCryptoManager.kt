package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android Keystore AES-256-GCM encryption manager for storing
 * WireGuard private keys, API session tokens, and active tunnel credentials securely.
 */
class KeystoreCryptoManager(private val context: Context) {

  companion object {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "nova_vpn_master_aes_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
  }

  private val secureDir: File
    get() = File(context.filesDir, "nova_secure_vault").apply { if (!exists()) mkdirs() }

  private fun getOrCreateMasterKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    if (existing != null) return existing

    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    val spec = KeyGenParameterSpec.Builder(
      KEY_ALIAS,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256)
      .setRandomizedEncryptionRequired(true)
      .build()
    generator.init(spec)
    return generator.generateKey()
  }

  fun putEncryptedString(key: String, plaintext: String) {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
    val iv = cipher.iv
    val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    val payload = Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
      Base64.encodeToString(encrypted, Base64.NO_WRAP)
    File(secureDir, "$key.enc").writeText(payload, Charsets.UTF_8)
  }

  fun getDecryptedString(key: String): String? {
    val file = File(secureDir, "$key.enc")
    if (!file.exists()) return null
    return runCatching {
      val parts = file.readText(Charsets.UTF_8).split(":")
      if (parts.size != 2) return null
      val iv = Base64.decode(parts[0], Base64.NO_WRAP)
      val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
      val cipher = Cipher.getInstance(TRANSFORMATION)
      cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
      String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrNull()
  }

  fun removeEncrypted(key: String) {
    val file = File(secureDir, "$key.enc")
    if (file.exists()) file.delete()
  }
}
