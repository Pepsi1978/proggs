package de.frank.jarvis.live

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log

/**
 * Fängt Medientasten ab, solange ein Live-Gespräch läuft und einige Sekunden danach.
 *
 * Läuft der Live-Ton über das Freisprechprofil (SCO) des Autoradios, schickt das Toyota Touch nach dem Abbau
 * der Verbindung von sich aus ein AVRCP-Play. Ohne eigene Medien-Sitzung leitet Android es an den zuletzt
 * benutzten Player weiter, und Poweramp startet ungefragt. Mit dieser Sitzung ist Jarvis (zuletzt abgespielt)
 * der Empfänger und verwirft den Befehl. Musik, die vor dem Gespräch lief, setzt ihr Player weiterhin selbst
 * über den zurückgegebenen Tonfokus fort.
 */
object MedienTastenRiegel {
    private const val TAG = "JarvisLiveRouting"
    /** Das Play des Autoradios kam nach höchstens 0,8 s; 5 s lassen Luft, ohne die Lenkradtasten lange zu sperren. */
    private const val NACHLAUF_MS = 5_000L

    private val haupt = Handler(Looper.getMainLooper())
    private var sitzung: MediaSessionCompat? = null
    private val loesen = Runnable { freigebenJetzt() }

    /** Ab Gesprächsbeginn: Sitzung anlegen bzw. eine laufende Freigabe abbrechen. Aus jedem Faden aufrufbar. */
    fun sperren(context: Context) {
        val app = context.applicationContext
        haupt.post {
            haupt.removeCallbacks(loesen)
            if (sitzung != null) return@post
            sitzung = runCatching {
                MediaSessionCompat(app, "JarvisLive").apply {
                    setCallback(object : MediaSessionCompat.Callback() {
                        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                            Log.i(TAG, "Medientaste im Live-Gespräch verworfen")
                            return true
                        }
                        override fun onPlay() {}
                        override fun onPause() {}
                        override fun onStop() {}
                    }, haupt)
                    setPlaybackState(PlaybackStateCompat.Builder().setActions(0)
                        .setState(PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f).build())
                    isActive = true
                }
            }.onFailure { Log.w(TAG, "Medientasten-Riegel nicht angelegt", it) }.getOrNull()
        }
    }

    /** Am Gesprächsende: Sitzung erst nach dem Nachlauf freigeben, damit das Play des Autoradios hier landet. */
    fun freigebenSpaeter() {
        haupt.post {
            haupt.removeCallbacks(loesen)
            haupt.postDelayed(loesen, NACHLAUF_MS)
        }
    }

    private fun freigebenJetzt() {
        val s = sitzung ?: return
        sitzung = null
        runCatching { s.isActive = false; s.release() }
    }
}
