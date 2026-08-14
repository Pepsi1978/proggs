package de.frank.stacklabor.werftstudio.service.auth

import android.content.Context
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

class KeystoreCodexTokenStore(context: Context) : CodexTokenStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    override fun read(): CodexTokens? {
        val encoded = preferences.getString(KEY_PAYLOAD, null) ?: return null
        return runCatching {
            val envelope = JSONObject(encoded)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(TAG_BITS, Base64.getDecoder().decode(envelope.getString("iv"))),
            )
            val clearText = cipher.doFinal(Base64.getDecoder().decode(envelope.getString("ciphertext")))
            JSONObject(String(clearText, Charsets.UTF_8)).toTokens()
        }.getOrElse {
            clear()
            null
        }
    }

    @Synchronized
    override fun write(tokens: CodexTokens) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val clearText = tokens.toJson().toString().toByteArray(Charsets.UTF_8)
        val envelope = JSONObject()
            .put("iv", Base64.getEncoder().encodeToString(cipher.iv))
            .put("ciphertext", Base64.getEncoder().encodeToString(cipher.doFinal(clearText)))
        check(preferences.edit().putString(KEY_PAYLOAD, envelope.toString()).commit()) {
            "Codex-Tokens konnten nicht atomar gespeichert werden."
        }
    }

    @Synchronized
    override fun clear() {
        preferences.edit().clear().commit()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun CodexTokens.toJson(): JSONObject = JSONObject()
        .put("access_token", accessToken)
        .put("refresh_token", refreshToken ?: JSONObject.NULL)
        .put("expires_at", expiresAtEpochMillis)
        .put("account_id", accountId ?: JSONObject.NULL)
        .put("email", email ?: JSONObject.NULL)

    private fun JSONObject.toTokens(): CodexTokens = CodexTokens(
        accessToken = getString("access_token"),
        refreshToken = optString("refresh_token").takeIf(String::isNotBlank),
        expiresAtEpochMillis = getLong("expires_at"),
        accountId = optString("account_id").takeIf(String::isNotBlank),
        email = optString("email").takeIf(String::isNotBlank),
    )

    private companion object {
        const val PREFERENCES = "codex_oauth"
        const val KEY_PAYLOAD = "encrypted_tokens"
        const val KEY_ALIAS = "stacklabor_werftstudio_codex_tokens"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}
