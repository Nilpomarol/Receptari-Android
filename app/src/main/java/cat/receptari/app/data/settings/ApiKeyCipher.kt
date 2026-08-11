package cat.receptari.app.data.settings

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM encryption backed by a key that lives in the Android Keystore.
 *
 * The secret key material never enters the app's process — it is generated inside the
 * Keystore and referenced by alias, so the encrypted API key on disk is worthless to
 * anything that reads the DataStore file without also being this app on this device.
 *
 * No user authentication is required to use the key: the import pipeline has to encrypt
 * and decrypt in the background, and requiring biometrics per API call would make the
 * feature unusable.
 */
@Singleton
class ApiKeyCipher @Inject constructor() {

    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // GCM generates a fresh IV per encryption; it is not secret, but it is required
        // for decryption, so it is stored alongside the ciphertext.
        return encode(cipher.iv) + SEPARATOR + encode(ciphertext)
    }

    /** Returns null when the stored value cannot be decrypted — see [ApiKeyStore]. */
    fun decrypt(stored: String): String? {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 2) return null

        return runCatching {
            val iv = decode(parts[0])
            val ciphertext = decode(parts[1])
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            }
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encode(bytes: ByteArray): String =
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        android.util.Base64.decode(value, android.util.Base64.NO_WRAP)

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "cat.receptari.app.apikey"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val SEPARATOR = ":"
        const val KEY_SIZE_BITS = 256
        const val TAG_LENGTH_BITS = 128
    }
}
