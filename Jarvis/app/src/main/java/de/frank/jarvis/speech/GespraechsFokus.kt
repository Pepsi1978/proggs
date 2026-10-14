package de.frank.jarvis.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import de.frank.jarvis.observability.IdeenLog

/**
 * Hält den Tonfokus für ein ganzes freihändiges Gespräch („Hey Jarvis“ bis zum Ende des Gesprächsmodus).
 *
 * Der [Vorleser] holt für jede Antwort nur vorübergehenden Fokus und gibt ihn danach frei. Ohne diesen Halter
 * fiele der Fokus dann an Spotify & Co. zurück: Die Musik liefe zwischen den Runden wieder an, käme über das
 * Mikrofon in die nächste Aufnahme und verhinderte das Sprech-Ende. Mit dem Halter liegt der Fokus des
 * Vorlesers oben auf dem Stapel, fällt danach an das Gespräch zurück, und die Musik bleibt bis zum
 * Gesprächsende still. Danach läuft sie von selbst weiter.
 *
 * Aufruf: [halten] zu Beginn des Gesprächs (vor der ersten Aufnahme), [freigeben] im finally des Gesprächs.
 */
object GespraechsFokus {
    @Volatile private var anfrage: AudioFocusRequest? = null
    private var audioManager: AudioManager? = null

    /** Holt den Fokus, falls er nicht schon gehalten wird. Ein mehrfacher Aufruf schadet nicht. */
    fun halten(context: Context) {
        synchronized(this) {
            if (anfrage != null) return
            val am = context.applicationContext.getSystemService(AudioManager::class.java) ?: return
            val neu = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                // Verluste sind hier gleichgültig: Übernimmt der Vorleser, kommt der Fokus danach zurück;
                // nimmt ihn ein Anruf, endet das Gespräch ohnehin über seinen eigenen Weg.
                .setOnAudioFocusChangeListener { }
                .build()
            val ergebnis = runCatching { am.requestAudioFocus(neu) }.getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
            if (ergebnis == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                anfrage = neu
                audioManager = am
            } else {
                IdeenLog.warn("GespraechsFokus", "halten", "Tonfokus fürs Gespräch verweigert")
            }
        }
    }

    /** Gibt den Fokus frei; andere Apps setzen danach selbst fort. */
    fun freigeben() {
        synchronized(this) {
            val alt = anfrage ?: return
            runCatching { audioManager?.abandonAudioFocusRequest(alt) }
            anfrage = null
            audioManager = null
        }
    }
}
