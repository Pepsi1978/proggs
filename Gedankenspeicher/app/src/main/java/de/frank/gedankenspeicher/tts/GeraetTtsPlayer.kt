package de.frank.gedankenspeicher.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

/**
 * Die **Stimme des Geräts** — Androids eingebaute Sprachausgabe.
 *
 * Sie ist der Rückhalt für alles Vorlesen: die drei anderen Wege brauchen Netz, und zwei von
 * ihnen zusätzlich einen Schlüssel. Fehlt eines davon, blieb bisher jeder Lautsprecher stumm
 * und meldete einen technischen Fehler — obwohl auf jedem Android-Telefon eine Sprachausgabe
 * vorhanden ist.
 *
 * Sie klingt schlichter als Chirp 3 oder Edge. Das ist der Punkt: sie ist immer da, sie
 * kostet nichts, und sie funktioniert im Flugmodus.
 *
 * `TextToSpeech` wird erst nach einem asynchronen Rückruf benutzbar. Wer vorher `speak()`
 * ruft, bekommt still ein `ERROR` — deshalb wartet diese Klasse: eine Anfrage, die vor der
 * Bereitschaft eintrifft, wird gemerkt und läuft, sobald es geht.
 */
class GeraetTtsPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val sperre = Any()

    /** `null` = noch nicht bereit, `false` = Einrichtung endgültig fehlgeschlagen. */
    @Volatile
    private var bereit: Boolean? = null

    private var motor: TextToSpeech? = null

    /** Eine Anfrage, die vor der Bereitschaft kam. */
    private var wartend: (() -> Unit)? = null

    private val laeuftGerade = AtomicBoolean(false)

    init {
        motor = TextToSpeech(appContext) { status ->
            val geglueckt = status == TextToSpeech.SUCCESS
            val sprache = if (geglueckt) {
                runCatching { motor?.setLanguage(Locale.GERMAN) }.getOrNull()
            } else {
                null
            }
            // MISSING_DATA/NOT_SUPPORTED heißt: kein Deutsch installiert. Vorgelesen wird
            // trotzdem — mit der Standardsprache klingt es fremd, aber es bleibt hörbar.
            val brauchbar = geglueckt &&
                sprache != TextToSpeech.LANG_MISSING_DATA &&
                sprache != TextToSpeech.LANG_NOT_SUPPORTED
            if (geglueckt && !brauchbar) {
                logger.warning("Geraetestimme: kein Deutsch installiert, nutze die Standardsprache")
            }
            bereit = geglueckt
            val nachholen = synchronized(sperre) { wartend.also { wartend = null } }
            if (geglueckt) {
                nachholen?.invoke()
            } else {
                logger.warning("Geraetestimme liess sich nicht einrichten (status $status)")
                nachholen?.invoke()
            }
        }
    }

    /**
     * Vorlesen. [onError] wird nur gerufen, wenn das Gerät wirklich keine Sprachausgabe hat —
     * dann ist der letzte Weg zu Ende und die Oberfläche muss es sagen dürfen.
     */
    fun speak(
        text: String,
        speechRate: Float = 1f,
        onPlaybackStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        if (text.isBlank()) {
            onComplete()
            return
        }
        val auftrag = {
            val tts = motor
            if (bereit != true || tts == null) {
                onError(TtsPlaybackException("Dieses Gerät hat keine eingerichtete Sprachausgabe."))
            } else {
                starte(tts, text, speechRate, onPlaybackStart, onComplete, onError)
            }
        }
        // Noch in der Einrichtung? Dann wird der Auftrag gemerkt statt verworfen.
        if (bereit == null) {
            val nachtragen = synchronized(sperre) {
                if (bereit == null) {
                    wartend = auftrag
                    true
                } else {
                    false
                }
            }
            if (nachtragen) return
        }
        auftrag()
    }

    private fun starte(
        tts: TextToSpeech,
        text: String,
        speechRate: Float,
        onPlaybackStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val kennung = UUID.randomUUID().toString()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId == kennung) {
                    laeuftGerade.set(true)
                    onPlaybackStart()
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == kennung && laeuftGerade.compareAndSet(true, false)) onComplete()
            }

            @Deprecated("Von der Plattform vorgegeben", ReplaceWith(""))
            override fun onError(utteranceId: String?) {
                if (utteranceId == kennung && laeuftGerade.compareAndSet(true, false)) {
                    onError(TtsPlaybackException("Die Sprachausgabe des Geräts brach ab."))
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId == kennung && laeuftGerade.compareAndSet(true, false)) {
                    onError(TtsPlaybackException("Die Sprachausgabe des Geräts brach ab ($errorCode)."))
                }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                // Ein Abbruch von unserer Seite ist kein Fehler — er meldet nur das Ende.
                if (utteranceId == kennung && laeuftGerade.compareAndSet(true, false)) onComplete()
            }
        })
        tts.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        val ergebnis = tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), kennung)
        if (ergebnis != TextToSpeech.SUCCESS) {
            laeuftGerade.set(false)
            onError(TtsPlaybackException("Die Sprachausgabe des Geräts liess sich nicht starten."))
        }
    }

    fun stop() {
        laeuftGerade.set(false)
        synchronized(sperre) { wartend = null }
        runCatching { motor?.stop() }
    }

    /** Androids Sprachausgabe kennt kein Fortsetzen — Anhalten heisst hier Beenden. */
    fun pause(): Boolean = false

    fun resume(): Boolean = false

    fun shutdown() {
        stop()
        runCatching { motor?.shutdown() }
        motor = null
    }

    private companion object {
        val logger: Logger = Logger.getLogger(GeraetTtsPlayer::class.java.name)
    }
}
