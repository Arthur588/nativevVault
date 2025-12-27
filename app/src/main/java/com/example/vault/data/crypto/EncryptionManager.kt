package com.example.vault.data.crypto

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    /**
     * Encrypt an input stream into the output stream using AES‑GCM.
     * Returns the randomly generated IV used for encryption.
     */
    fun encryptStream(
        input: InputStream,
        output: OutputStream,
        key: ByteArray
    ): ByteArray {
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            val encrypted = cipher.update(buffer, 0, bytesRead)
            if (encrypted != null) {
                output.write(encrypted)
            }
        }
        val finalBytes = cipher.doFinal()
        if (finalBytes != null) {
            output.write(finalBytes)
        }
        return iv
    }

    /**
     * Decrypt an input stream into an output stream using AES‑GCM.
     */
    fun decryptStream(
        input: InputStream,
        output: OutputStream,
        key: ByteArray,
        iv: ByteArray
    ) {
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            val decrypted = cipher.update(buffer, 0, bytesRead)
            if (decrypted != null) {
                output.write(decrypted)
            }
        }
        val finalBytes = cipher.doFinal()
        if (finalBytes != null) {
            output.write(finalBytes)
        }
    }
}