package com.example.vault.data.crypto

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object KeyDerivation {
    private const val ALGORITHM = "PBKDF2WithHmacSHA512"
    private const val KEY_LENGTH = 256 // bits
    private const val ITERATIONS = 120_000

    /**
     * Generate a random salt. 16 bytes is sufficient for PBKDF2.
     */
    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    /**
     * Derive a symmetric key from a user password and salt.
     */
    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
}