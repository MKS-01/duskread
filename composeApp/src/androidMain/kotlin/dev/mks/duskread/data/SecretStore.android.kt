package dev.mks.duskread.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM under a hardware-backed key, with the ciphertext parked in its own
 * preferences file.
 *
 * Two decisions worth stating, because both had an obvious-looking
 * alternative:
 *
 * **Not `androidx.security:security-crypto`.** `EncryptedSharedPreferences`
 * is the stock answer and is exactly this, but the library has been
 * deprecated by Jetpack with no replacement. Taking a dependency on something
 * already on its way out, to save forty lines the platform provides directly
 * at minSdk 31, is a worse trade than writing them.
 *
 * **Not the app's own preferences file.** The token lives in
 * `duskread_secrets`, never in `algo_atlas` — that file is read by the
 * home-screen widget from a different process, is rewritten wholesale by
 * [dev.mks.duskread.links.LinkLibrary], and is where someone will one day add
 * a debug dump. A credential should not be in the blast radius of any of that.
 *
 * The key never leaves the keystore; only its handle does. Losing it (a
 * restore to a new device, a factory reset) makes the stored value
 * undecryptable, which [get] treats as "no token" rather than an error — the
 * reader is asked to reconnect, which is the honest outcome anyway.
 */
private class KeystoreSecretStore(private val prefs: SharedPreferences) : SecretStore {
    override fun get(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null

        return runCatching {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            // The IV is written in front of the ciphertext rather than stored
            // beside it: one value to read, and no way for the two halves to
            // drift apart.
            val iv = bytes.copyOfRange(0, IvLength)
            val body = bytes.copyOfRange(IvLength, bytes.size)

            val cipher = Cipher.getInstance(Transformation)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TagBits, iv))
            cipher.doFinal(body).decodeToString()
        }.getOrNull()
    }

    override fun put(key: String, value: String?) {
        if (value == null) {
            prefs.edit().remove(key).apply()
            return
        }

        val cipher = Cipher.getInstance(Transformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val body = cipher.doFinal(value.encodeToByteArray())

        val packed = Base64.encodeToString(cipher.iv + body, Base64.NO_WRAP)
        prefs.edit().putString(key, packed).apply()
    }

    /** The keystore entry, generated on first use and reused after that. */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        (keyStore.getEntry(KeyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
        generator.init(
            KeyGenParameterSpec.Builder(KeyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // Deliberately not setUserAuthenticationRequired: a sync runs
                // from a Settings tap and must not demand a fingerprint to
                // read a token the reader just pasted.
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val AndroidKeyStore = "AndroidKeyStore"
        const val KeyAlias = "duskread.secrets"
        const val Transformation = "AES/GCM/NoPadding"
        const val IvLength = 12
        const val TagBits = 128
    }
}

@Composable
actual fun rememberSecretStore(): SecretStore {
    val context = LocalContext.current
    return remember(context) { secretStore(context) }
}

/**
 * The same store without composition, for the same reason
 * [keyValueStore] has one: a sync can be driven from outside the Compose tree.
 */
fun secretStore(context: Context): SecretStore = KeystoreSecretStore(context.getSharedPreferences("duskread_secrets", Context.MODE_PRIVATE))
