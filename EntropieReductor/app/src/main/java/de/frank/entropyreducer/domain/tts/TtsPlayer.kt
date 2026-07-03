package de.frank.entropyreducer.domain.tts

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.remote.GoogleTtsApi
import de.frank.entropyreducer.data.remote.SynthesizeRequest
import de.frank.entropyreducer.data.remote.TtsAudioConfig
import de.frank.entropyreducer.data.remote.TtsInput
import de.frank.entropyreducer.data.remote.TtsVoice
import de.frank.entropyreducer.data.remote.tts.GoogleTtsVoices
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.data.tts.TtsUsageBackup
import de.frank.entropyreducer.data.tts.TtsUsageStore
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
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
    private val usageStore: TtsUsageStore,
    private val usageBackup: TtsUsageBackup,
) {

    private companion object {
        const val TAG = "TtsPlayer"
        const val CACHE_FILE = "entropy_tts.mp3"

        /** Praefix aller Mental-Sequenz-Cache-Dateien (siehe synthesizeToCache/clearSequenceCache). */
        const val SEQUENCE_PREFIX = "mental_seq_"
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
            Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "TTS-Synthese fehlgeschlagen: ${e.message}", e)
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

    private suspend fun synthesize(
        text: String,
        apiKey: String,
        voiceName: String,
        targetFile: File = File(context.cacheDir, CACHE_FILE),
    ): File {
        Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "Synthesize ${text.length} chars, voice=$voiceName")
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
        // Erfolgreicher Google-Call -> Zeichen fuers Monats-Kontingent verbuchen. Google bietet
        // keinen Zeichen-Zaehler, also zaehlt die App selbst mit (Observability). Cache-Treffer
        // gehen gar nicht durch diese Methode und werden korrekt NICHT gezaehlt. TTS rechnet pro
        // Eingabe-Zeichen (inkl. Leerzeichen) ab -> text.length ist die korrekte Groesse.
        // Meldet add() eine neue 50k-Schwelle, wird der Stand fire-and-forget ins Drive gesichert
        // (Frank-Wunsch 2026-07-03) — nicht bei jedem Zeichen, sonst liefe das Backup dauernd.
        if (usageStore.add(text.length)) usageBackup.backupNow()
        val audioBytes = Base64.decode(response.audioContentBase64, Base64.DEFAULT)
        targetFile.writeBytes(audioBytes)
        return targetFile
    }

    /* ============================ Mental-Vorlese-Sequenz ============================ */
    //
    // Eigener Pfad für das Mentalboard-Vorlesen (Frank-Wunsch 2026-06-16): Sequenzen können
    // bewusst frische Dateien erzeugen, damit Wiederholungen nicht exakt dieselbe MP3-Datei mit
    // derselben Betonung abspielen. Bewusst getrennt von speak()/cleanup() (die genau EINE Datei
    // nutzen und nach Wiedergabe löschen) — die Sequenz-Dateien dürfen NICHT gelöscht werden,
    // solange die Sequenz läuft.

    /**
     * Synthetisiert [text] in eine EINDEUTIG benannte Cache-Datei und gibt sie zurueck.
     * Existiert die Datei fuer denselben Text+Stimme bereits (gleicher Hash), wird sie OHNE
     * neuen TTS-Aufruf wiederverwendet (Caching). Bei [forceFresh] wird bewusst eine neue Datei
     * erzeugt, damit Satz-Wiederholungen neu an Google-TTS geschickt werden. Loescht selbst
     * nichts — Aufrufer raeumt am Ende per [clearSequenceCache] auf.
     *
     * @throws IllegalStateException wenn kein TTS-API-Schluessel hinterlegt ist (mit Klartext-
     *         Meldung — der Aufrufer zeigt sie dem Benutzer).
     */
    suspend fun synthesizeToCache(
        text: String,
        voiceNameOverride: String? = null,
        forceFresh: Boolean = false,
    ): File {
        val clean = text.trim()
        require(clean.isNotEmpty()) { "Text ist leer" }

        val apiKey = secrets.googleTtsApiKey?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Kein TTS-API-Schlüssel hinterlegt")

        val voiceName = voiceNameOverride
            ?: settings.ttsVoiceFlow.first().takeIf { it.isNotBlank() }
            ?: GoogleTtsVoices.DEFAULT_VOICE_NAME

        // Stabiler, dateinamen-sicherer Schluessel aus Stimme + Text (+ Laenge als
        // zusaetzliche Kollisions-Absicherung). hashCode() ist in ART deterministisch.
        val key = (voiceName + "\u0000" + clean).hashCode().toLong() and 0xFFFFFFFFL
        val freshSuffix = if (forceFresh) "_${System.nanoTime()}" else ""
        val target = File(context.cacheDir, "$SEQUENCE_PREFIX${key}_${clean.length}$freshSuffix.mp3")
        if (!forceFresh && target.exists() && target.length() > 0) {
            Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "Cache-Treffer fuer Satz (${clean.length} chars)")
            return target
        }
        return withContext(Dispatchers.IO) { synthesize(clean, apiKey, voiceName, target) }
    }

    /**
     * Spielt eine bereits synthetisierte Datei ab und suspendiert bis zum Ende des Clips.
     * Loescht die Datei NICHT (anders als [speak]). Abbruchsicher: bei Cancellation der
     * Coroutine wird der MediaPlayer sofort gestoppt und freigegeben.
     *
     * Threading-Vertrag (Bug-Almanach media3 T1): von einem Main-Thread-Kontext aufrufen —
     * der MentalTtsViewModel ruft die Sequenz in withContext(Dispatchers.Main).
     */
    suspend fun playCachedFileAwait(file: File): Unit = suspendCancellableCoroutine { cont ->
        // Vorherige Wiedergabe sauber beenden (loescht keine Sequenz-Datei).
        stop()
        val mp = MediaPlayer()
        cont.invokeOnCancellation {
            try {
                if (mp.isPlaying) mp.stop()
            } catch (_: IllegalStateException) {
                // Player bereits released — ignorieren
            }
            mp.release()
            if (mediaPlayer === mp) mediaPlayer = null
        }
        try {
            mp.setDataSource(file.absolutePath)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener {
                if (cont.isActive) cont.resume(Unit)
            }
            mp.setOnErrorListener { _, what, extra ->
                Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "MediaPlayer error what=$what extra=$extra")
                if (cont.isActive) {
                    cont.resumeWithException(IllegalStateException("MediaPlayer error $what/$extra"))
                }
                true
            }
            mediaPlayer = mp
            mp.prepareAsync()
        } catch (e: Exception) {
            mp.release()
            if (mediaPlayer === mp) mediaPlayer = null
            if (cont.isActive) cont.resumeWithException(e)
        }
    }

    /** Loescht alle Sequenz-Cache-Dateien. Aufruf nach Ende/Stop des Mental-Vorlesens. */
    fun clearSequenceCache() {
        try {
            context.cacheDir.listFiles { f -> f.name.startsWith(SEQUENCE_PREFIX) }
                ?.forEach { it.delete() }
        } catch (e: Exception) {
            Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "Sequenz-Cache aufraeumen fehlgeschlagen: ${e.message}", e)
        }
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
                Diag.e(DiagnosticArea.GOOGLE_TTS, TAG, "MediaPlayer error what=$what extra=$extra")
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
