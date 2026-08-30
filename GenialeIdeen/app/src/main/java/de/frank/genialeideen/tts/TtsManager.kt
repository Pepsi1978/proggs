package de.frank.genialeideen.tts

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Logger
import kotlin.math.ln

class TtsManager(context: Context) {
    private val appContext = context.applicationContext
    private val edgePlayer = EdgeTtsPlayer(appContext)
    private val googlePlayer = GoogleCloudTtsPlayer(appContext)
    private val qwenPlayer = QwenTtsPlayer(appContext)
    private val generation = AtomicLong(0)
    private var activeProviderId: String? = null
    private var variationStep = 0

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
        varied: Boolean = false,
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

        val providerId = providerOverride?.id ?: preferences?.getString(
                PREF_TTS_PROVIDER,
                TtsCatalog.DEFAULT_PROVIDER.id,
            ) ?: TtsCatalog.DEFAULT_PROVIDER.id
        activeProviderId = providerId
        val baseRate = preferences?.getFloat(PREF_SPEECH_RATE, DEFAULT_SPEECH_RATE)
            ?.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE) ?: DEFAULT_SPEECH_RATE
        // Every repetition is synthesised anew, so a little variation keeps it from sounding
        // like a replay of the exact same recording.
        val step = if (varied) ++variationStep else 0
        val speechRate = if (varied && readFlag(PREF_VARY_SPEECH_RATE)) {
            (baseRate * (1f + RATE_STEPS[step % RATE_STEPS.size] / 100f))
                .coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        } else {
            baseRate
        }
        val pitchPercent = if (varied && readFlag(PREF_VARY_PITCH)) {
            PITCH_STEPS[step % PITCH_STEPS.size]
        } else {
            0
        }
        // Standardmaessig an: Ohne diese Bindung sprechen mehrsprachige Stimmen englisch
        // aussehende Woerter englisch aus.
        val erzwingeDeutsch = preferences?.getBoolean(PREF_IMMER_DEUTSCH, true) ?: true
        when (providerId) {
            TtsProvider.EDGE.id -> edgePlayer.speak(
                text = spokenText,
                voice = voiceOverride
                    ?: rotatedVoice(TtsCatalog.edgeVoices, step, varied)
                    ?: preferences?.getString(PREF_EDGE_VOICE, TtsCatalog.DEFAULT_EDGE_VOICE)
                    ?: TtsCatalog.DEFAULT_EDGE_VOICE,
                speechRate = speechRate,
                pitchPercent = pitchPercent,
                erzwingeDeutsch = erzwingeDeutsch,
                onPlaybackStart = guardedStart,
                onComplete = guardedComplete,
                onError = guardedError,
            )
            TtsProvider.GOOGLE_CLOUD.id -> googlePlayer.speak(
                text = spokenText,
                apiKey = preferences?.getString(PREF_GOOGLE_API_KEY, "").orEmpty(),
                voiceName = voiceOverride
                    ?: rotatedVoice(TtsCatalog.googleVoices, step, varied)
                    ?: preferences?.getString(
                        PREF_GOOGLE_VOICE,
                        TtsCatalog.DEFAULT_GOOGLE_VOICE,
                    ) ?: TtsCatalog.DEFAULT_GOOGLE_VOICE,
                speechRate = speechRate,
                pitchSemitones = toSemitones(pitchPercent),
                erzwingeDeutsch = erzwingeDeutsch,
                onPlaybackStart = guardedStart,
                onComplete = guardedComplete,
                onError = guardedError,
            )
            // The cloned voice varies its own emphasis, so rate and pitch stay untouched here.
            TtsProvider.QWEN_CLONE.id -> qwenPlayer.speak(
                text = spokenText,
                rawApiKey = preferences?.getString(PREF_QWEN_API_KEY, "").orEmpty(),
                rawVoiceId = voiceOverride
                    ?: preferences?.getString(PREF_QWEN_VOICE_ID, "").orEmpty(),
                onPlaybackStart = guardedStart,
                onComplete = guardedComplete,
                onError = guardedError,
            )
            else -> guardedError(IllegalStateException("Unbekannter TTS-Anbieter."))
        }
    }

    /** Picks the next of the user's favourite voices, or null when there is nothing to rotate. */
    private fun rotatedVoice(catalog: List<TtsVoice>, step: Int, varied: Boolean): String? {
        if (!varied || !readFlag(PREF_VARY_VOICE)) return null
        val favourites = preferences?.getStringSet(PREF_FAVORITE_VOICES, emptySet()).orEmpty()
        val available = catalog.map { it.id }.filter { it in favourites }
        return if (available.size < 2) null else available[step % available.size]
    }

    private fun readFlag(key: String): Boolean = preferences?.getBoolean(key, true) ?: true

    private fun toSemitones(percent: Int): Double =
        if (percent == 0) 0.0 else 12.0 * ln(1.0 + percent / 100.0) / ln(2.0)

    fun stop() {
        generation.incrementAndGet()
        activeProviderId = null
        edgePlayer.stop()
        googlePlayer.stop()
        qwenPlayer.stop()
    }

    fun pause(): Boolean = when (activeProviderId) {
        TtsProvider.EDGE.id -> edgePlayer.pause()
        TtsProvider.GOOGLE_CLOUD.id -> googlePlayer.pause()
        TtsProvider.QWEN_CLONE.id -> qwenPlayer.pause()
        else -> false
    }

    fun resume(): Boolean = when (activeProviderId) {
        TtsProvider.EDGE.id -> edgePlayer.resume()
        TtsProvider.GOOGLE_CLOUD.id -> googlePlayer.resume()
        TtsProvider.QWEN_CLONE.id -> qwenPlayer.resume()
        else -> false
    }

    fun shutdown() {
        generation.incrementAndGet()
        edgePlayer.shutdown()
        googlePlayer.shutdown()
        qwenPlayer.shutdown()
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
        const val PREFS_NAME = "geniale_ideen_secure_prefs"
        const val PREF_TTS_PROVIDER = "tts_provider"
        const val PREF_EDGE_VOICE = "edge_tts_voice"
        const val PREF_GOOGLE_API_KEY = "google_tts_api_key"
        const val PREF_GOOGLE_VOICE = "google_tts_voice"
        const val PREF_QWEN_API_KEY = "qwen_tts_api_key"
        const val PREF_QWEN_VOICE_ID = "qwen_tts_voice_id"
        const val PREF_IMMER_DEUTSCH = "immer_deutsch_vorlesen"
        const val PREF_SPEECH_RATE = "tts_speech_rate"
        const val PREF_FAVORITE_VOICES = "favorite_tts_voices"
        const val PREF_VARY_VOICE = "vary_voice_per_repetition"
        const val PREF_VARY_SPEECH_RATE = "vary_speech_rate"
        const val PREF_VARY_PITCH = "vary_pitch"
        const val DEFAULT_SPEECH_RATE = 1f
        const val MIN_SPEECH_RATE = 0.7f
        const val MAX_SPEECH_RATE = 1.3f

        // Both sequences have different lengths so that pace and pitch never pair up the same
        // way twice in a row. Neighbouring entries always differ.
        val RATE_STEPS = intArrayOf(0, 5, -4, 3, -6)
        val PITCH_STEPS = intArrayOf(2, -3, 4, -2)
        val logger = Logger.getLogger(TtsManager::class.java.name)
    }
}
