package de.frank.wecker

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.roundToInt

/**
 * Der Klingelton der Schlafenszeit-Erinnerung — gewählt aus den Standard-Benachrichtigungstönen des
 * Handys, mit eigener Lautstärke.
 *
 * Der Benachrichtigungskanal selbst ist stumm; der Ton kommt von hier. Nur so ist die Lautstärke
 * unabhängig von der Geräteeinstellung: Gespielt wird über den Alarm-Audiostrom, dessen Pegel für die
 * Dauer des Tons auf den gewählten Anteil gesetzt und danach wiederhergestellt wird — genau wie beim
 * Wecken. Laut- oder Leisemodus des Handys ändern daran nichts; ist dagegen „Nicht stören“ aktiv,
 * bleibt die Erinnerung stumm.
 */
object SchlafTon {
    private const val TAG = "WeckerSchlafTon"
    private const val TON_KEY = "sleep_reminder_tone"
    private const val LAUTSTAERKE_KEY = "sleep_reminder_volume"
    /** Längstens so lange klingt die Erinnerung; lange Töne werden danach sanft beendet. */
    private const val HOECHSTDAUER_MS = 8_000L

    private val main = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var vorherLautstaerke = -1
    private var fertig: (() -> Unit)? = null
    private val stopper = Runnable { stoppen() }

    data class Ton(val titel: String, val uri: String)

    private fun prefs(context: Context) = AlarmStore.get(context).prefs

    fun standardUri(): String = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString().orEmpty()

    fun gewaehlt(context: Context): String = prefs(context).getString(TON_KEY, null) ?: standardUri()
    fun setGewaehlt(context: Context, uri: String) { prefs(context).edit().putString(TON_KEY, uri).apply() }

    /** Lautstärke in Prozent, 5..100. */
    fun lautstaerke(context: Context): Int = prefs(context).getInt(LAUTSTAERKE_KEY, 70).coerceIn(5, 100)
    fun setLautstaerke(context: Context, prozent: Int) { prefs(context).edit().putInt(LAUTSTAERKE_KEY, prozent.coerceIn(5, 100)).apply() }

    /** Alle Standard-Benachrichtigungstöne des Geräts, alphabetisch. */
    fun alle(context: Context): List<Ton> = try {
        val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_NOTIFICATION) }
        val cursor = manager.cursor
        buildList {
            while (cursor.moveToNext()) {
                val titel = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX).orEmpty()
                val uri = manager.getRingtoneUri(cursor.position)?.toString() ?: continue
                add(Ton(titel, uri))
            }
        }.distinctBy { it.uri }.sortedBy { it.titel.lowercase() }
    } catch (e: Exception) { Log.w(TAG, "Töne nicht lesbar", e); emptyList() }

    fun titel(context: Context, uri: String): String = runCatching {
        RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context)
    }.getOrNull() ?: "Standardton"

    /** Ob „Nicht stören“ gerade aktiv ist — dann bleibt die Erinnerung stumm. */
    fun nichtStoerenAktiv(context: Context): Boolean {
        val filter = context.getSystemService(NotificationManager::class.java).currentInterruptionFilter
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    /**
     * Spielt [uri] mit [prozent] Lautstärke. [beiEnde] läuft genau einmal, auch bei Fehlern — der
     * Empfänger beendet damit seinen `goAsync`-Auftrag.
     */
    fun spielen(context: Context, uri: String = gewaehlt(context), prozent: Int = lautstaerke(context), beiEnde: () -> Unit = {}) {
        main.post {
            stoppen()
            fertig = beiEnde
            val app = context.applicationContext
            val audio = app.getSystemService(AudioManager::class.java)
            this.audio = audio
            try {
                vorherLautstaerke = audio.getStreamVolume(AudioManager.STREAM_ALARM)
                val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val ziel = (max * prozent / 100f).roundToInt().coerceIn(1, max)
                audio.setStreamVolume(AudioManager.STREAM_ALARM, ziel, 0)
                val neu = MediaPlayer()
                player = neu
                neu.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                neu.setDataSource(app, Uri.parse(uri.ifBlank { standardUri() }))
                neu.setOnPreparedListener { if (player === it) it.start() }
                neu.setOnCompletionListener { if (player === it) stoppen() }
                neu.setOnErrorListener { mp, _, _ -> if (player === mp) stoppen(); true }
                neu.prepareAsync()
                main.postDelayed(stopper, HOECHSTDAUER_MS)
            } catch (e: Exception) {
                Log.w(TAG, "Ton nicht abspielbar", e)
                stoppen()
            }
        }
    }

    private var audio: AudioManager? = null

    fun stoppen() {
        main.removeCallbacks(stopper)
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
        if (vorherLautstaerke >= 0) {
            runCatching { audio?.setStreamVolume(AudioManager.STREAM_ALARM, vorherLautstaerke, 0) }
            vorherLautstaerke = -1
        }
        fertig?.let { fertig = null; runCatching { it() } }
    }
}
