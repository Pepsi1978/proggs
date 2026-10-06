package de.frank.aufgaben.erinnerung

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log

/**
 * Erinnerungen müssen hörbar sein, egal wie das Handy gerade steht. Deshalb:
 * - Wecker-Kanal statt Benachrichtigungs-Kanal: Der klingt auch im Lautlos- und Vibrationsmodus und
 *   bei „Nicht stören“ (Wecker sind dort standardmäßig erlaubt).
 * - Immer der eingebaute Lautsprecher, auch wenn Bluetooth verbunden ist (z. B. ein ausgeschaltetes
 *   oder auf eine andere Quelle gestelltes Autoradio, das den Ton sonst verschluckt).
 * - Steht die Wecker-Lautstärke auf 0, wird sie für die Erinnerung kurz angehoben und danach zurückgesetzt.
 */
object Lautsprecher {
    private const val TAG = "Lautsprecher"
    /** Mindestanteil der Wecker-Lautstärke, wenn sie ganz unten steht. */
    private const val MINDEST_ANTEIL = 0.6f

    fun attribute(sprache: Boolean): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(if (sprache) AudioAttributes.CONTENT_TYPE_SPEECH else AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** Lenkt den Player auf den eingebauten Lautsprecher (vor start() aufrufen). */
    fun aufGeraet(context: Context, player: MediaPlayer) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        val geraet = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } ?: return
        runCatching { player.setPreferredDevice(geraet) }.onFailure { Log.w(TAG, "Lautsprecher nicht wählbar: ${it.message}") }
    }

    private const val PREFS = "lautsprecher"
    private const val VORHER = "wecker_vorher"
    private const val ZIEL = "wecker_ziel"
    /** Wie viele Wiedergaben gerade die angehobene Lautstärke brauchen (Ton-Probe und Dienst gleichzeitig). */
    private var nutzer = 0

    /**
     * Hebt die Wecker-Lautstärke an, falls sie auf 0 steht, und liefert die Freigabe. Erst wenn die
     * letzte Wiedergabe freigegeben ist, kommt der alte Wert zurück. Er steht vorher in den Einstellungen,
     * damit er auch nach einem Absturz zurückkommt ([zuruecksetzen] beim nächsten App-Start).
     */
    fun hoerbarMachen(context: Context): () -> Unit {
        val app = context.applicationContext
        synchronized(this) {
            if (nutzer == 0) anheben(app)
            nutzer++
        }
        var frei = false
        return {
            synchronized(Lautsprecher) {
                if (!frei) {
                    frei = true
                    nutzer = (nutzer - 1).coerceAtLeast(0)
                    if (nutzer == 0) zuruecksetzen(app)
                }
            }
        }
    }

    private fun anheben(app: Context) {
        try {
            val audio = app.getSystemService(AudioManager::class.java) ?: return
            val vorher = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            if (vorher > 0) return
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val ziel = (max * MINDEST_ANTEIL).toInt().coerceIn(1, max)
            // Erst merken, dann ändern: So geht der alte Wert auch bei einem Absturz nicht verloren.
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(VORHER, vorher).putInt(ZIEL, ziel).commit()
            audio.setStreamVolume(AudioManager.STREAM_ALARM, ziel, 0)
        } catch (x: Exception) {
            Log.w(TAG, "Wecker-Lautstärke nicht änderbar: ${x.message}")
        }
    }

    /**
     * Stellt eine angehobene Wecker-Lautstärke zurück, sofern niemand sie inzwischen selbst verändert hat.
     * Läuft nach der letzten Wiedergabe und beim App-Start (falls der Prozess vorher abgestürzt ist).
     */
    fun zuruecksetzen(context: Context) {
        val app = context.applicationContext
        synchronized(this) {
            if (nutzer > 0) return
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (!prefs.contains(VORHER)) return
            val vorher = prefs.getInt(VORHER, 0)
            val ziel = prefs.getInt(ZIEL, -1)
            runCatching {
                val audio = app.getSystemService(AudioManager::class.java)
                if (audio != null && audio.getStreamVolume(AudioManager.STREAM_ALARM) == ziel) audio.setStreamVolume(AudioManager.STREAM_ALARM, vorher, 0)
            }.onFailure { Log.w(TAG, "Wecker-Lautstärke nicht zurückgestellt: ${it.message}") }
            prefs.edit().remove(VORHER).remove(ZIEL).apply()
        }
    }
}
