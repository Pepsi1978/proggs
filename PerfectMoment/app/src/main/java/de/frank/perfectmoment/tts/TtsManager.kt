package de.frank.perfectmoment.tts

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Logger

class TtsManager(context: Context) {
    private val appContext = context.applicationContext
    private val edgePlayer = EdgeTtsPlayer(appContext)
    private val googlePlayer = GoogleCloudTtsPlayer(appContext)
    private val generation = AtomicLong(0)

    private val preferences: SharedPreferences? by lazy {
        try {
            val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKey,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (error: Exception) {
            logger.warning("Encrypted TTS preferences are unavailable: ${error.message}")
            null
        }
    }

    fun speak(
        text: String,
        onStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
        providerOverride: TtsProvider? = null,
        voiceOverride: String? = null,
    ) {
        stop()
        val requestGeneration = generation.incrementAndGet()
        val terminal = AtomicBoolean(false)
        val started = AtomicBoolean(false)
        val spokenText = removeEmojis(text)

        val guardedStart = {
            if (requestGeneration == generation.get() &&
                !terminal.get() &&
                started.compareAndSet(false, true)
            ) {
                onStart()
            }
        }
        val guardedComplete = {
            if (requestGeneration == generation.get() && terminal.compareAndSet(false, true)) {
                onComplete()
            }
        }
        val guardedError = { error: Exception ->
            if (requestGeneration == generation.get() && terminal.compareAndSet(false, true)) {
                onError(error)
            }
        }

        if (spokenText.isBlank()) {
            guardedComplete()
            return
        }

        when (
            providerOverride?.id ?: preferences?.getString(
                PREF_TTS_PROVIDER,
                TtsCatalog.DEFAULT_PROVIDER.id,
            ) ?: TtsCatalog.DEFAULT_PROVIDER.id
        ) {
            TtsProvider.EDGE.id -> edgePlayer.speak(
                text = spokenText,
                voice = voiceOverride
                    ?: preferences?.getString(PREF_EDGE_VOICE, TtsCatalog.DEFAULT_EDGE_VOICE)
                    ?: TtsCatalog.DEFAULT_EDGE_VOICE,
                onPlaybackStart = guardedStart,
                onComplete = guardedComplete,
                onError = guardedError,
            )
            TtsProvider.GOOGLE_CLOUD.id -> googlePlayer.speak(
                text = spokenText,
                apiKey = preferences?.getString(PREF_GOOGLE_API_KEY, "").orEmpty(),
                voiceName = voiceOverride
                    ?: preferences?.getString(
                        PREF_GOOGLE_VOICE,
                        TtsCatalog.DEFAULT_GOOGLE_VOICE,
                    ) ?: TtsCatalog.DEFAULT_GOOGLE_VOICE,
                onPlaybackStart = guardedStart,
                onComplete = guardedComplete,
                onError = guardedError,
            )
            else -> guardedError(IllegalStateException("Unbekannter TTS-Anbieter."))
        }
    }

    fun stop() {
        generation.incrementAndGet()
        edgePlayer.stop()
        googlePlayer.stop()
    }

    fun shutdown() {
        generation.incrementAndGet()
        edgePlayer.shutdown()
        googlePlayer.shutdown()
    }

    private fun removeEmojis(text: String): String {
        val result = StringBuilder(text.length)
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            if (isEmojiCodePoint(codePoint) || isKeycapSequenceAt(text, offset, codePoint)) {
                if (result.isNotEmpty() && !result.last().isWhitespace()) result.append(' ')
            } else {
                result.appendCodePoint(codePoint)
            }
            offset += Character.charCount(codePoint)
        }
        return result.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun isKeycapSequenceAt(text: String, offset: Int, codePoint: Int): Boolean {
        if (codePoint != '#'.code && codePoint != '*'.code && codePoint !in '0'.code..'9'.code) {
            return false
        }
        var nextOffset = offset + Character.charCount(codePoint)
        if (nextOffset < text.length && text.codePointAt(nextOffset) == 0xFE0F) {
            nextOffset += Character.charCount(0xFE0F)
        }
        return nextOffset < text.length && text.codePointAt(nextOffset) == 0x20E3
    }

    private fun isEmojiCodePoint(codePoint: Int): Boolean = when (codePoint) {
        in 0x1F000..0x1FAFF,
        in 0x2600..0x27BF,
        in 0x2300..0x23FF,
        in 0x2B00..0x2BFF,
        in 0x1F1E6..0x1F1FF,
        in 0x1F3FB..0x1F3FF,
        in 0xE0020..0xE007F,
        0x00A9,
        0x00AE,
        0x203C,
        0x2049,
        0x200D,
        0x20E3,
        0x2122,
        0x2139,
        0x25AA,
        0x25AB,
        0x25B6,
        0x25C0,
        in 0x25FB..0x25FE,
        0x3030,
        0x303D,
        0x3297,
        0x3299,
        0xFE0E,
        0xFE0F,
        -> true
        else -> false
    }

    private companion object {
        const val PREFS_NAME = "perfect_moment_secure_prefs"
        const val PREF_TTS_PROVIDER = "tts_provider"
        const val PREF_EDGE_VOICE = "edge_tts_voice"
        const val PREF_GOOGLE_API_KEY = "google_tts_api_key"
        const val PREF_GOOGLE_VOICE = "google_tts_voice"
        val logger = Logger.getLogger(TtsManager::class.java.name)
    }
}
