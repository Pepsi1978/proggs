package de.frank.entropyreducer.domain.tts

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.remote.GoogleTtsApi
import de.frank.entropyreducer.data.remote.SynthesizeRequest
import de.frank.entropyreducer.data.remote.TtsAudioConfig
import de.frank.entropyreducer.data.remote.TtsInput
import de.frank.entropyreducer.data.remote.TtsVoice
import de.frank.entropyreducer.data.remote.tts.GoogleTtsVoices
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Wiedergabe-Schicht für Google Cloud TTS Chirp 3 HD.
 *
 * Architektur (analog BestJournalFrank's bewaehrtem Pattern):
 *  1. POST `/v1/text:synthesize` mit API-Key als Query-Parameter
 *  2. Antwort enthaelt `audioContent` (Base64-kodiertes MP3)
 *  3. Decodieren in App-Cache-Datei
 *  4. Mit MediaPlayer abspielen — entry/exit-Callbacks für UI-States
 *
 * Bewusst MediaPlayer statt ExoPlayer (Stufenplan-Empfehlung): Der Tagesbriefing-
 * /Wochenrueckblick-Use-Case spielt 30-90s MP3-Dateien ab, MediaPlayer reicht und
 * ist 3 MB kleiner. Falls später Streaming-Synthese kommt — Tausch ist trivial.
 */
@Singleton
class TtsPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsApi: GoogleTtsApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
) {

    private companion object {
        const val TAG = "TtsPlayer"
        const val CACHE_FILE = "entropy_tts.mp3"
    }

    @Volatile private var mediaPlayer: MediaPlayer? = null

    /**
     * Synthetisiert `text` und spielt das Ergebnis ab.
     *
     * @param text Der vorzulesende Text (max ~5000 Zeichen pro Request).
     * @param voiceNameOverride Optionale Voice-ID. Wenn `null`, wird die
     *        Benutzerauswahl aus AppSettings verwendet (Fallback: Default-Stimme).
     * @return [TtsResult.Success] bei laufender Wiedergabe (Callbacks feuern danach),
     *         [TtsResult.Error] bei Setup-Problemen (kein Key, Netzfehler, leerer Text).
     */
    suspend fun speak(
        text: String,
        voiceNameOverride: String? = null,
        onPlaybackStart: (() -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
    ): TtsResult {
        if (text.isBlank()) return TtsResult.Error("Text ist leer")

        val apiKey = secrets.googleTtsApiKey?.takeIf { it.isNotBlank() }
            ?: return TtsResult.Error("Kein TTS-API-Schlüssel hinterlegt")

        val voiceName = voiceNameOverride
            ?: settings.ttsVoiceFlow.first().takeIf { it.isNotBlank() }
            ?: GoogleTtsVoices.DEFAULT_VOICE_NAME

        return try {
            val audioFile = withContext(Dispatchers.IO) { synthesize(text, apiKey, voiceName) }
            withContext(Dispatchers.Main) {
                playFile(audioFile, onPlaybackStart, onComplete, onError)
            }
            TtsResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "TTS-Synthese fehlgeschlagen: ${e.message}", e)
            onError?.invoke(e)
            TtsResult.Error(e.message ?: "Unbekannter Fehler")
        }
    }

    /** Stoppt die laufende Wiedergabe. Nach `stop()` ist sofort eine neue möglich. */
    fun stop() {
        try {
            mediaPlayer?.takeIf { it.isPlaying }?.stop()
        } catch (_: IllegalStateException) {
            // Player schon released — ignorieren
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private suspend fun synthesize(text: String, apiKey: String, voiceName: String): File {
        Log.d(TAG, "Synthesize ${text.length} chars, voice=$voiceName")
        val response = ttsApi.synthesize(
            apiKey = apiKey,
            request = SynthesizeRequest(
                input = TtsInput(text = text),
                voice = TtsVoice(
                    languageCode = GoogleTtsVoices.DEFAULT_LANGUAGE_CODE,
                    name = voiceName,
                ),
                audioConfig = TtsAudioConfig(audioEncoding = "MP3"),
            ),
        )
        val audioBytes = Base64.decode(response.audioContentBase64, Base64.DEFAULT)
        val file = File(context.cacheDir, CACHE_FILE)
        file.writeBytes(audioBytes)
        return file
    }

    private fun playFile(
        file: File,
        onPlaybackStart: (() -> Unit)?,
        onComplete: (() -> Unit)?,
        onError: ((Throwable) -> Unit)?,
    ) {
        // Vorherige Wiedergabe sauber beenden
        stop()
        // prepareAsync() statt prepare() — vermeidet StrictMode-DiskRead-Violation
        // auf dem Main-Thread und ist laut MediaPlayer-Doku der empfohlene Weg.
        // Quelle: developer.android.com/reference/android/media/MediaPlayer
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener { player ->
                player.start()
                onPlaybackStart?.invoke()
            }
            setOnCompletionListener {
                onComplete?.invoke()
                cleanup(file)
            }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                onError?.invoke(IllegalStateException("MediaPlayer error $what/$extra"))
                cleanup(file)
                true
            }
            prepareAsync()
        }
    }

    private fun cleanup(file: File) {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) { /* ignore */ }
        mediaPlayer = null
        if (file.exists()) file.delete()
    }
}

sealed interface TtsResult {
    data object Success : TtsResult
    data class Error(val message: String) : TtsResult
}
