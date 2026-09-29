package com.example.data.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object VaultBackupCrypto {

    private val MAGIC = byteArrayOf('V'.code.toByte(), 'B'.code.toByte(), 'A'.code.toByte(), 'C'.code.toByte())
    private const val VERSION: Byte = 0x01
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val ITERATIONS = 65536
    private const val KEY_LENGTH_BITS = 256

    private val secureRandom = SecureRandom()

    fun encrypt(plainBytes: ByteArray, passphrase: String): ByteArray {
        val salt = ByteArray(SALT_LENGTH).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { secureRandom.nextBytes(it) }

        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plainBytes)

        val output = ByteArray(MAGIC.size + 1 + SALT_LENGTH + IV_LENGTH + ciphertext.size)
        var offset = 0

        System.arraycopy(MAGIC, 0, output, offset, MAGIC.size)
        offset += MAGIC.size

        output[offset] = VERSION
        offset += 1

        System.arraycopy(salt, 0, output, offset, SALT_LENGTH)
        offset += SALT_LENGTH

        System.arraycopy(iv, 0, output, offset, IV_LENGTH)
        offset += IV_LENGTH

        System.arraycopy(ciphertext, 0, output, offset, ciphertext.size)
        return output
    }

    @Throws(Exception::class)
    fun decrypt(encryptedBytes: ByteArray, passphrase: String): ByteArray {
        val headerSize = MAGIC.size + 1 + SALT_LENGTH + IV_LENGTH
        if (encryptedBytes.size < headerSize) {
            throw IllegalArgumentException("Invalid backup file: file is too small")
        }

        // Verify magic bytes
        for (i in MAGIC.indices) {
            if (encryptedBytes[i] != MAGIC[i]) {
                throw IllegalArgumentException("Not a valid encrypted vault backup file")
            }
        }

        var offset = MAGIC.size
        val version = encryptedBytes[offset]
        offset += 1

        if (version != VERSION) {
            throw IllegalArgumentException("Unsupported backup version: $version")
        }

        val salt = ByteArray(SALT_LENGTH)
        System.arraycopy(encryptedBytes, offset, salt, 0, SALT_LENGTH)
        offset += SALT_LENGTH

        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(encryptedBytes, offset, iv, 0, IV_LENGTH)
        offset += IV_LENGTH

        val ciphertextSize = encryptedBytes.size - offset
        val ciphertext = ByteArray(ciphertextSize)
        System.arraycopy(encryptedBytes, offset, ciphertext, 0, ciphertextSize)

        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, "AES")
    }
}
