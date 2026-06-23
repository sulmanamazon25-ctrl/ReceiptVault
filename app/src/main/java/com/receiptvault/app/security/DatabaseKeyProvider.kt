package com.receiptvault.app.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates and protects the passphrase used to encrypt the SQLCipher database.
 *
 * The passphrase itself is a random 256-bit value. It is never stored in clear text: it is
 * sealed with an AES-256-GCM key that lives in the Android Keystore (and therefore never
 * leaves secure hardware where available). The sealed blob and its IV are persisted in
 * private SharedPreferences. On first launch a fresh passphrase is generated; afterwards it
 * is unsealed on demand.
 */
@Singleton
class DatabaseKeyProvider @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val keyStore: KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    @Synchronized
    fun getOrCreatePassphrase(): ByteArray {
        ensureKeystoreKey()
        val storedCipherText = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val storedIv = prefs.getString(KEY_IV, null)

        return if (storedCipherText != null && storedIv != null) {
            decrypt(
                cipherText = Base64.decode(storedCipherText, Base64.NO_WRAP),
                iv = Base64.decode(storedIv, Base64.NO_WRAP)
            )
        } else {
            val passphrase = ByteArray(PASSPHRASE_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
            val (cipherText, iv) = encrypt(passphrase)
            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(cipherText, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
            passphrase
        }
    }

    private fun ensureKeystoreKey() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()
            generator.init(spec)
            generator.generateKey()
        }
    }

    private fun secretKey(): SecretKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    private fun encrypt(plain: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain)
        return cipherText to iv
    }

    private fun decrypt(cipherText: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(cipherText)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "receiptvault_db_key"
        const val PREFS_NAME = "receiptvault_secure_prefs"
        const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_passphrase"
        const val KEY_IV = "passphrase_iv"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val KEY_SIZE_BITS = 256
        const val PASSPHRASE_SIZE_BYTES = 32
    }
}
