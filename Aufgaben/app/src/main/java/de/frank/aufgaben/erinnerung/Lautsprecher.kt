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

    /**
     * Hebt die Wecker-Lautstärke an, falls sie auf 0 steht. Liefert, wie sie zurückgesetzt wird
     * (nichts zu tun, wenn sie schon hörbar war).
     */
    fun hoerbarMachen(context: Context): () -> Unit {
        val nichts: () -> Unit = {}
        val audio = context.getSystemService(AudioManager::class.java) ?: return nichts
        return try {
            val vorher = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            if (vorher > 0) return nichts
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val ziel = (max * MINDEST_ANTEIL).toInt().coerceIn(1, max)
            audio.setStreamVolume(AudioManager.STREAM_ALARM, ziel, 0)
            val zurueck: () -> Unit = {
                runCatching {
                    // Nur zurückstellen, wenn niemand die Lautstärke inzwischen selbst verändert hat.
                    if (audio.getStreamVolume(AudioManager.STREAM_ALARM) == ziel) audio.setStreamVolume(AudioManager.STREAM_ALARM, vorher, 0)
                }
            }
            zurueck
        } catch (x: Exception) {
            Log.w(TAG, "Wecker-Lautstärke nicht änderbar: ${x.message}")
            nichts
        }
    }
}
