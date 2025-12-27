package com.example.vault.data.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * KeyManager encapsulates deriving and validating the encryption key from the
 * user's password.  It uses PBKDF2 with a salt stored in encrypted shared
 * preferences.  On first run a new salt is generated and stored; the derived
 * key's SHA‑256 hash is persisted.  On subsequent runs, the password is
 * accepted only if the derived key's hash matches the stored hash.
 */
@Singleton
class KeyManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "vault_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Initialise the key for the given password.  If this is the first time a
     * password is being set, a new salt is generated and the derived key hash
     * is stored.  Otherwise the derived key must match the stored hash.  The
     * derived key is returned to callers for use in encryption/decryption.
     * @throws IllegalStateException if the password is incorrect.
     */
    fun initialise(password: String): ByteArray {
        // Retrieve or generate salt
        val saltBase64 = prefs.getString(KEY_SALT, null)
        val salt: ByteArray = if (saltBase64 == null) {
            val s = EncryptionUtils.generateRandomSalt()
            prefs.edit().putString(KEY_SALT, Base64.encodeToString(s, Base64.NO_WRAP)).apply()
            s
        } else {
            Base64.decode(saltBase64, Base64.NO_WRAP)
        }

        val key = KeyDerivation.deriveKey(password.toCharArray(), salt)
        // Compute hash of key
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(key)
        val storedHashBase64 = prefs.getString(KEY_HASH, null)
        return if (storedHashBase64 == null) {
            // first run: store hash
            prefs.edit().putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP)).apply()
            key
        } else {
            val storedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP)
            if (storedHash.contentEquals(hash)) {
                key
            } else {
                throw IllegalStateException("Invalid password")
            }
        }
    }

    companion object {
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "key_hash"
    }
}

/**
 * Helper object that provides cryptographically secure random salt generation.
 */
private object EncryptionUtils {
    fun generateRandomSalt(size: Int = 16): ByteArray {
        val salt = ByteArray(size)
        java.security.SecureRandom().nextBytes(salt)
        return salt
    }
}