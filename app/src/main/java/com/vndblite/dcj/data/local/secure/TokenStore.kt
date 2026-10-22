package com.vndblite.dcj.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.vndblite.dcj.core.AppLog
import com.vndblite.dcj.core.DefaultDispatcherProvider
import com.vndblite.dcj.core.DispatcherProvider
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.withContext

/**
 * Authenticated encryption primitive used by [EncryptedTokenStore] to encrypt API tokens at rest (S5).
 */
interface TokenCipher {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(payload: ByteArray): ByteArray
}

/**
 * Hardware-backed Android Keystore `AES/GCM/NoPadding` 256-bit implementation of [TokenCipher] (Section 2, S5).
 * Avoids the deprecated `EncryptedSharedPreferences` library while providing authenticated encryption.
 */
class AndroidKeystoreAesGcmCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS
) : TokenCipher {

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
            load(null)
        }
        val existingEntry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) {
            return existingEntry.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        require(iv != null && iv.size in MIN_IV_BYTES..MAX_IV_BYTES) {
            "Unexpected GCM IV length"
        }
        val ciphertext = cipher.doFinal(plaintext)
        return packIvAndCiphertext(iv, ciphertext)
    }

    override fun decrypt(payload: ByteArray): ByteArray {
        val (iv, ciphertext) = unpackIvAndCiphertext(payload)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
        return cipher.doFinal(ciphertext)
    }

    companion object {
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val DEFAULT_KEY_ALIAS = "vndb_kotlin_token_aes_gcm_v1"
        internal const val TRANSFORMATION = "AES/GCM/NoPadding"
        internal const val KEY_SIZE_BITS = 256
        internal const val GCM_TAG_BITS = 128
        internal const val MIN_IV_BYTES = 12
        internal const val MAX_IV_BYTES = 16

        internal fun packIvAndCiphertext(iv: ByteArray, ciphertext: ByteArray): ByteArray {
            val output = ByteArray(1 + iv.size + ciphertext.size)
            output[0] = iv.size.toByte()
            System.arraycopy(iv, 0, output, 1, iv.size)
            System.arraycopy(ciphertext, 0, output, 1 + iv.size, ciphertext.size)
            return output
        }

        internal fun unpackIvAndCiphertext(payload: ByteArray): Pair<ByteArray, ByteArray> {
            require(payload.size > 1 + MIN_IV_BYTES) { "Encrypted token payload is too short" }
            val ivSize = payload[0].toInt() and 0xFF
            require(ivSize in MIN_IV_BYTES..MAX_IV_BYTES && payload.size > 1 + ivSize) {
                "Invalid GCM IV length in encrypted token payload"
            }
            val iv = payload.copyOfRange(1, 1 + ivSize)
            val ciphertext = payload.copyOfRange(1 + ivSize, payload.size)
            return iv to ciphertext
        }
    }
}

/**
 * Pure JVM `AES/GCM/NoPadding` 256-bit implementation of [TokenCipher] for unit tests where
 * the `AndroidKeyStore` hardware provider is not registered.
 */
class JvmAesGcmTokenCipher(
    keyBytes: ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }
) : TokenCipher {

    private val secretKey: SecretKey = SecretKeySpec(keyBytes.copyOf(32), "AES")
    private val secureRandom: SecureRandom = SecureRandom()

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val iv = ByteArray(12).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(AndroidKeystoreAesGcmCipher.TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey,
            GCMParameterSpec(AndroidKeystoreAesGcmCipher.GCM_TAG_BITS, iv)
        )
        val ciphertext = cipher.doFinal(plaintext)
        return AndroidKeystoreAesGcmCipher.packIvAndCiphertext(iv, ciphertext)
    }

    override fun decrypt(payload: ByteArray): ByteArray {
        val (iv, ciphertext) = AndroidKeystoreAesGcmCipher.unpackIvAndCiphertext(payload)
        val cipher = Cipher.getInstance(AndroidKeystoreAesGcmCipher.TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(AndroidKeystoreAesGcmCipher.GCM_TAG_BITS, iv)
        )
        return cipher.doFinal(ciphertext)
    }
}

/**
 * Storage contract for the personal access token (S5, B6).
 * Tokens are encrypted at rest, excluded from Auto Backup / Device Transfer, and never logged.
 */
interface TokenStore {
    suspend fun getToken(accountId: String? = null): String?
    suspend fun saveToken(accountId: String, token: String)
    suspend fun clearToken(accountId: String? = null)
    suspend fun clearAllTokens()
    suspend fun hasToken(accountId: String? = null): Boolean
}

class EncryptedTokenStore(
    private val sharedPreferences: SharedPreferences,
    private val cipher: TokenCipher = AndroidKeystoreAesGcmCipher(),
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider
) : TokenStore {

    override suspend fun getToken(accountId: String?): String? = withContext(dispatchers.io) {
        val key = resolvePrefKey(accountId)
        val encoded = sharedPreferences.getString(key, null)
        if (encoded.isNullOrBlank()) {
            return@withContext null
        }
        try {
            val encryptedBytes = Base64.decode(encoded, Base64.NO_WRAP)
            val decryptedBytes = cipher.decrypt(encryptedBytes)
            decryptedBytes.toString(Charsets.UTF_8).takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed to decrypt stored API token for key=$key; clearing corrupt entry", e)
            sharedPreferences.edit().remove(key).apply()
            null
        }
    }

    override suspend fun saveToken(accountId: String, token: String) = withContext(dispatchers.io) {
        val trimmedAccount = accountId.trim()
        val trimmedToken = token.trim()
        require(trimmedAccount.isNotEmpty()) { "accountId must not be blank" }
        require(trimmedToken.isNotEmpty()) { "token must not be blank" }

        val encryptedBytes = cipher.encrypt(trimmedToken.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        sharedPreferences.edit()
            .putString(accountKey(trimmedAccount), encoded)
            .putString(KEY_ACTIVE_TOKEN, encoded)
            .putString(KEY_ACTIVE_ACCOUNT_ID, trimmedAccount)
            .commit()
        Unit
    }

    override suspend fun clearToken(accountId: String?) = withContext(dispatchers.io) {
        val editor = sharedPreferences.edit()
        val targetAccount = accountId?.trim()?.takeIf { it.isNotEmpty() }
            ?: sharedPreferences.getString(KEY_ACTIVE_ACCOUNT_ID, null)
        if (targetAccount != null) {
            editor.remove(accountKey(targetAccount))
        }
        val currentActiveAccount = sharedPreferences.getString(KEY_ACTIVE_ACCOUNT_ID, null)
        if (accountId == null || accountId.trim() == currentActiveAccount) {
            editor.remove(KEY_ACTIVE_TOKEN)
            editor.remove(KEY_ACTIVE_ACCOUNT_ID)
        }
        editor.commit()
        Unit
    }

    override suspend fun clearAllTokens() = withContext(dispatchers.io) {
        sharedPreferences.edit().clear().commit()
        Unit
    }

    override suspend fun hasToken(accountId: String?): Boolean =
        !getToken(accountId).isNullOrBlank()

    private fun resolvePrefKey(accountId: String?): String {
        val cleaned = accountId?.trim().orEmpty()
        return if (cleaned.isNotEmpty()) accountKey(cleaned) else KEY_ACTIVE_TOKEN
    }

    private fun accountKey(accountId: String): String = "token_account_$accountId"

    companion object {
        private const val TAG = "EncryptedTokenStore"
        /** Matches `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` exclusion (B6, S5). */
        const val PREFS_FILE_NAME = "vndb_secure_token_prefs"
        private const val KEY_ACTIVE_TOKEN = "token_active"
        private const val KEY_ACTIVE_ACCOUNT_ID = "token_active_account_id"

        fun create(
            context: Context,
            cipher: TokenCipher = AndroidKeystoreAesGcmCipher(),
            dispatchers: DispatcherProvider = DefaultDispatcherProvider
        ): EncryptedTokenStore {
            val prefs = context.applicationContext.getSharedPreferences(
                PREFS_FILE_NAME,
                Context.MODE_PRIVATE
            )
            return EncryptedTokenStore(
                sharedPreferences = prefs,
                cipher = cipher,
                dispatchers = dispatchers
            )
        }
    }
}
