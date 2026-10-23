package de.frank.ziele.tts

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
    private val neuePlayer = NeueStimmenPlayer(appContext)
    /** Rückfallkette und ihre Prüfungen kommen aus den Einstellungen, damit Oberfläche und Vorlesen dieselbe Logik nutzen. */
    private val einstellungen = de.frank.ziele.data.Einstellungen.get(appContext)
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
        /** Meldet, dass statt der gewählten Stimme eine Stufe der Rückfallkette liest. */
        onRueckfall: ((String) -> Unit)? = null,
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
        // Every repetition is synthesised anew, so a little variation keeps it from sounding
        // like a replay of the exact same recording.
        val step = if (varied) ++variationStep else 0
        // Das Grundtempo kommt erst je Stimme dazu (wie in Jarvis): Jede Stufe der Rückfallkette liest in ihrem eigenen Tempo.
        val rateFaktor = if (varied && readFlag(PREF_VARY_SPEECH_RATE)) 1f + RATE_STEPS[step % RATE_STEPS.size] / 100f else 1f
        val pitchPercent = if (varied && readFlag(PREF_VARY_PITCH)) {
            PITCH_STEPS[step % PITCH_STEPS.size]
        } else {
            0
        }
        // Standardmaessig an: Ohne diese Bindung sprechen mehrsprachige Stimmen englisch
        // aussehende Woerter englisch aus.
        val erzwingeDeutsch = preferences?.getBoolean(PREF_IMMER_DEUTSCH, true) ?: true
        val ansage = Ansage(spokenText, step, varied, rateFaktor, pitchPercent, erzwingeDeutsch)
        // Wie in Jarvis: Stufen, die nur wiederholten, was die Hauptstimme schon versucht hat, und Stufen ohne Schlüssel laufen gar nicht erst.
        val kette = einstellungen.aktiveRueckfallStufen(providerId)
        val hauptName = TtsProvider.entries.firstOrNull { it.id == providerId }?.kurz ?: providerId
        var hauptFehler: Exception? = null

        /** [i] = -1 ist die gewählte Stimme, danach die Stufen der Kette der Reihe nach. */
        fun versuche(i: Int) {
            val stufe = kette.getOrNull(i)
            val id = stufe?.anbieter?.id ?: providerId
            activeProviderId = id
            starte(
                ansage = ansage,
                providerId = id,
                schluessel = stufe?.schluessel,
                voiceOverride = if (stufe == null) voiceOverride else null,
                mitAusweg = i < kette.lastIndex,
                onStart = guardedStart,
                onComplete = guardedComplete,
                onError = { fehler ->
                    val erster = hauptFehler ?: fehler.also { hauptFehler = it }
                    if (requestGeneration == generation.get() && !terminal.get() && !started.get() && i < kette.lastIndex) {
                        val naechste = kette[i + 1]
                        logger.warning("${stufe?.id ?: providerId} gescheitert, Rückfall auf ${naechste.id}: ${fehler.message}")
                        // Der volle Grund der gerade gescheiterten Stimme steht im Hinweis: Auch ein Einrichtungsfehler (Schlüssel, Region) bleibt so sichtbar.
                        val grund = fehler.message?.removePrefix("Vorlesen fehlgeschlagen: ")?.takeIf { it.isNotBlank() }
                            ?: fehler.javaClass.simpleName
                        onRueckfall?.invoke("${stufe?.titel ?: hauptName} liest gerade nicht, darum liest ${naechste.titel}. Grund: $grund")
                        versuche(i + 1)
                    } else if (stufe == null) {
                        guardedError(fehler)
                    } else {
                        guardedError(IllegalStateException("${erster.message} Auch die Rückfallstimmen scheiterten, zuletzt ${stufe.titel}: ${fehler.message}"))
                    }
                },
            )
        }
        versuche(-1)
    }

    /** Alles, was jede Stufe der Kette für dieselbe Ansage braucht. */
    private class Ansage(
        val text: String,
        val step: Int,
        val varied: Boolean,
        /** Abwandlung je Wiederholung; das Tempo selbst ist je Stimme eingestellt. */
        val rateFaktor: Float,
        val pitchPercent: Int,
        val erzwingeDeutsch: Boolean,
    )

    /** Startet genau eine Stimme; [schluessel] nur bei Gemini-Stufen der Kette: nur der kostenlose bzw. nur der bezahlte Schlüssel. */
    private fun starte(
        ansage: Ansage,
        providerId: String,
        schluessel: String?,
        voiceOverride: String?,
        mitAusweg: Boolean,
        onStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val step = ansage.step
        val varied = ansage.varied
        val speechRate = (einstellungen.ttsTempo(providerId) * ansage.rateFaktor).coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        when (providerId) {
            TtsProvider.EDGE.id -> edgePlayer.speak(
                text = ansage.text,
                voice = voiceOverride
                    ?: rotatedVoice(TtsCatalog.edgeVoices, step, varied)
                    ?: preferences?.getString(PREF_EDGE_VOICE, TtsCatalog.DEFAULT_EDGE_VOICE)
                    ?: TtsCatalog.DEFAULT_EDGE_VOICE,
                speechRate = speechRate,
                pitchPercent = ansage.pitchPercent,
                erzwingeDeutsch = ansage.erzwingeDeutsch,
                onPlaybackStart = onStart,
                onComplete = onComplete,
                onError = onError,
            )
            TtsProvider.GOOGLE_CLOUD.id -> googlePlayer.speak(
                text = ansage.text,
                apiKey = preferences?.getString(PREF_GOOGLE_API_KEY, "").orEmpty(),
                voiceName = voiceOverride
                    ?: rotatedVoice(TtsCatalog.googleVoices, step, varied)
                    ?: preferences?.getString(
                        PREF_GOOGLE_VOICE,
                        TtsCatalog.DEFAULT_GOOGLE_VOICE,
                    ) ?: TtsCatalog.DEFAULT_GOOGLE_VOICE,
                speechRate = speechRate,
                pitchSemitones = toSemitones(ansage.pitchPercent),
                erzwingeDeutsch = ansage.erzwingeDeutsch,
                onPlaybackStart = onStart,
                onComplete = onComplete,
                onError = onError,
            )
            // The cloned voice varies its own emphasis, so rate and pitch stay untouched here.
            TtsProvider.QWEN_CLONE.id -> qwenPlayer.speak(
                text = ansage.text,
                rawApiKey = preferences?.getString(PREF_QWEN_API_KEY, "").orEmpty(),
                rawVoiceId = voiceOverride
                    ?: preferences?.getString(PREF_QWEN_VOICE_ID, "").orEmpty(),
                onPlaybackStart = onStart,
                onComplete = onComplete,
                onError = onError,
            )
            // Gemini 3.8 Flash / Flash-Lite und Azure Neural: erst als Datei erzeugt, dann abgespielt.
            TtsProvider.GEMINI_FLASH.id, TtsProvider.GEMINI_LITE.id, TtsProvider.AZURE.id -> {
                val provider = TtsProvider.entries.first { it.id == providerId }
                val (katalog, gespeichert) = when (provider) {
                    TtsProvider.GEMINI_FLASH -> TtsCatalog.geminiFlashVoices to
                        (preferences?.getString(PREF_GEMINI_FLASH_VOICE, null) ?: TtsCatalog.DEFAULT_GEMINI_FLASH_VOICE)
                    TtsProvider.GEMINI_LITE -> TtsCatalog.geminiLiteVoices to
                        (preferences?.getString(PREF_GEMINI_LITE_VOICE, null) ?: TtsCatalog.DEFAULT_GEMINI_LITE_VOICE)
                    else -> TtsCatalog.azureVoices to
                        (preferences?.getString(PREF_AZURE_VOICE, null) ?: TtsCatalog.DEFAULT_AZURE_VOICE)
                }
                neuePlayer.speak(
                    text = ansage.text,
                    provider = provider,
                    voiceId = voiceOverride ?: rotatedVoice(katalog, step, varied) ?: gespeichert,
                    speechRate = speechRate,
                    zugang = NeueStimmenPlayer.Zugang(
                        geminiKeyFrei = preferences?.getString(PREF_GEMINI_TTS_KEY_FREI, "").orEmpty(),
                        geminiKeyBezahlt = preferences?.getString(PREF_GEMINI_TTS_KEY_BEZAHLT, "").orEmpty(),
                        schluesselWahl = when (schluessel) {
                            RueckfallStufe.FREI -> NeueStimmenPlayer.GEMINI_TTS_NUR_FREI
                            RueckfallStufe.BEZAHLT -> NeueStimmenPlayer.GEMINI_TTS_NUR_BEZAHLT
                            else -> preferences?.getString(PREF_GEMINI_TTS_SCHLUESSEL_WAHL, null)
                                ?: NeueStimmenPlayer.GEMINI_TTS_FREI_DANN_BEZAHLT
                        },
                        azureKey = preferences?.getString(PREF_AZURE_TTS_KEY, "").orEmpty(),
                        azureRegion = preferences?.getString(PREF_AZURE_TTS_REGION, null) ?: TtsCatalog.DEFAULT_AZURE_REGION,
                        erzwingeDeutsch = ansage.erzwingeDeutsch,
                    ),
                    onPlaybackStart = onStart,
                    onComplete = onComplete,
                    onError = onError,
                    mitAusweg = mitAusweg,
                )
            }
            else -> onError(IllegalStateException("Unbekannter TTS-Anbieter."))
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
        neuePlayer.stop()
    }

    fun pause(): Boolean = when (activeProviderId) {
        TtsProvider.EDGE.id -> edgePlayer.pause()
        TtsProvider.GOOGLE_CLOUD.id -> googlePlayer.pause()
        TtsProvider.QWEN_CLONE.id -> qwenPlayer.pause()
        TtsProvider.GEMINI_FLASH.id, TtsProvider.GEMINI_LITE.id, TtsProvider.AZURE.id -> neuePlayer.pause()
        else -> false
    }

    fun resume(): Boolean = when (activeProviderId) {
        TtsProvider.EDGE.id -> edgePlayer.resume()
        TtsProvider.GOOGLE_CLOUD.id -> googlePlayer.resume()
        TtsProvider.QWEN_CLONE.id -> qwenPlayer.resume()
        TtsProvider.GEMINI_FLASH.id, TtsProvider.GEMINI_LITE.id, TtsProvider.AZURE.id -> neuePlayer.resume()
        else -> false
    }

    fun shutdown() {
        generation.incrementAndGet()
        edgePlayer.shutdown()
        googlePlayer.shutdown()
        qwenPlayer.shutdown()
        neuePlayer.shutdown()
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
        const val PREFS_NAME = "ziele_secure_prefs"
        const val PREF_TTS_PROVIDER = "tts_provider"
        const val PREF_EDGE_VOICE = "edge_tts_voice"
        const val PREF_GOOGLE_API_KEY = "google_tts_api_key"
        const val PREF_GOOGLE_VOICE = "google_tts_voice"
        const val PREF_QWEN_API_KEY = "qwen_tts_api_key"
        const val PREF_QWEN_VOICE_ID = "qwen_tts_voice_id"
        const val PREF_IMMER_DEUTSCH = "immer_deutsch_vorlesen"
        const val PREF_SPEECH_RATE = "tts_speech_rate"
        const val PREF_FAVORITE_VOICES = "favorite_tts_voices"
        const val PREF_GEMINI_FLASH_VOICE = "gemini_flash_tts_voice"
        const val PREF_GEMINI_LITE_VOICE = "gemini_lite_tts_voice"
        const val PREF_AZURE_VOICE = "azure_tts_voice"
        const val PREF_AZURE_TTS_KEY = "azure_tts_key"
        const val PREF_AZURE_TTS_REGION = "azure_tts_region"
        const val PREF_GEMINI_TTS_KEY_FREI = "gemini_tts_key_frei"
        const val PREF_GEMINI_TTS_KEY_BEZAHLT = "gemini_tts_key_bezahlt"
        const val PREF_GEMINI_TTS_SCHLUESSEL_WAHL = "gemini_tts_schluessel_wahl"
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
