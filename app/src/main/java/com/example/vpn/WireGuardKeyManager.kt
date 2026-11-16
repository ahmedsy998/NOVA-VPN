package com.example.vpn

import com.example.data.local.KeystoreCryptoManager
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair

/**
 * Generates and manages WireGuard Curve25519 key pairs directly on the Android device.
 * Private keys are encrypted via Android Keystore AES-256-GCM and NEVER transmitted to the backend.
 * Only the Base64 public key is shared during VPN session provisioning.
 */
class WireGuardKeyManager(private val cryptoManager: KeystoreCryptoManager) {

  companion object {
    private const val PRIVATE_KEY_ALIAS = "wg_client_private_key"
  }

  @Synchronized
  fun getOrCreateKeyPair(): KeyPair {
    val savedPrivateKeyB64 = cryptoManager.getDecryptedString(PRIVATE_KEY_ALIAS)
    if (!savedPrivateKeyB64.isNullOrBlank()) {
      val parsed = runCatching { KeyPair(Key.fromBase64(savedPrivateKeyB64)) }.getOrNull()
      if (parsed != null) return parsed
    }
    val fresh = KeyPair()
    cryptoManager.putEncryptedString(PRIVATE_KEY_ALIAS, fresh.privateKey.toBase64())
    return fresh
  }

  @Synchronized
  fun rotateKeyPair(): KeyPair {
    val fresh = KeyPair()
    cryptoManager.putEncryptedString(PRIVATE_KEY_ALIAS, fresh.privateKey.toBase64())
    return fresh
  }

  fun getPublicKeyBase64(): String = getOrCreateKeyPair().publicKey.toBase64()
}
