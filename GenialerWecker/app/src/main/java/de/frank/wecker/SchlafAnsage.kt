package de.frank.wecker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.UserManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.frank.genialeideen.R
import de.frank.genialeideen.data.settings.SecureSettings
import de.frank.genialeideen.speech.SyntheseStimme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * Die laut vorgelesene Schlafenszeit-Ansage: erklärt, wann Frank schlafen gehen sollte und wann er geweckt wird.
 * Der Text steht schon beim Planen fest; er wird vorab als Audiodatei mit der gewählten Stimme vorbereitet
 * (Gemini 3.8 Flash-Lite, Gemini Flash, Google, Edge, Azure, Supertonic, Meine Stimmen) und bei der Erinnerung nur abgespielt.
 * Fehlt die Datei, wird live erzeugt (Rückfall Edge); scheitert auch das, klingt wenigstens der Erinnerungston.
 */
object SchlafAnsage {
    private const val TAG = "WeckerSchlafAnsage"
    const val ART_KEY = "sleep_announce_mode"
    const val STIMME_KEY = "sleep_announce_voice"
    private const val CACHE_KEY = "sleep_announce_cache"
    const val ART_BENACHRICHTIGUNG = "benachrichtigung"
    const val ART_WECKER = "wecker"

    private fun prefs(context: Context) = AlarmStore.get(context).prefs

    fun art(context: Context): String = runCatching { prefs(context).getString(ART_KEY, ART_BENACHRICHTIGUNG) }.getOrNull() ?: ART_BENACHRICHTIGUNG
    fun setArt(context: Context, art: String) { prefs(context).edit().putString(ART_KEY, art).apply() }
    /** "anbieter|stimme" oder "" für die Standardstimme aus den Einstellungen. */
    fun stimmWahl(context: Context): String = runCatching { prefs(context).getString(STIMME_KEY, "") }.getOrNull().orEmpty()
    fun setStimmWahl(context: Context, wahl: String) { prefs(context).edit().putString(STIMME_KEY, wahl).apply(); vorbereitenAnstossen(context) }

    fun stimme(context: Context, settings: SecureSettings): SyntheseStimme {
        val defaults = SyntheseStimme(settings)
        val wahl = stimmWahl(context)
        return if (wahl.isBlank() || !wahl.contains('|')) defaults
            else Alarm(voiceProvider = wahl.substringBefore('|'), voiceId = wahl.substringAfter('|')).resolveVoice(defaults)
    }

    // ---------- Text ----------
    data class Eintrag(val name: String, val wakeAt: Long, val sleepMinutes: Int, val sonne: String?)

    /** „22 Uhr“, „7 Uhr 21“, „0 Uhr 30“ – gut vorlesbar. */
    fun uhr(t: Long, zone: ZoneId): String = Instant.ofEpochMilli(t).atZone(zone).let { if (it.minute == 0) "${it.hour} Uhr" else "${it.hour} Uhr ${it.minute}" }

    private fun tag(t: Long, bezug: Long, zone: ZoneId): String {
        val d = Instant.ofEpochMilli(t).atZone(zone).toLocalDate()
        return when (ChronoUnit.DAYS.between(Instant.ofEpochMilli(bezug).atZone(zone).toLocalDate(), d)) {
            0L -> "heute"
            1L -> "morgen"
            else -> "am " + d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
        }
    }

    private fun dauer(minuten: Int): String {
        val h = minuten / 60; val m = minuten % 60
        val stunden = if (h == 1) "1 Stunde" else "$h Stunden"
        return when { h == 0 -> "$m Minuten"; m == 0 -> stunden; else -> "$stunden und $m Minuten" }
    }

    /** Der Ansagetext zum Zeitpunkt [ausloesung]; alle Angaben relativ dazu, damit Vorbereitung und Abspielen denselben Text ergeben. */
    fun text(bedtime: Long, ausloesung: Long, eintraege: List<Eintrag>, zone: ZoneId = ZoneId.systemDefault()): String {
        val sortiert = eintraege.sortedBy { it.wakeAt }
        val minuten = ((bedtime - ausloesung) / 60_000L).toInt()
        val bett = uhr(bedtime, zone).let { if (tag(bedtime, ausloesung, zone) == "heute") it else "${tag(bedtime, ausloesung, zone)} $it" }
        val erster = sortiert.firstOrNull() ?: return "Schlafenszeit-Erinnerung. Um $bett solltest du schlafen gehen."
        val anfang = if (minuten <= 0) "Schlafenszeit-Erinnerung. Jetzt, um $bett, ist deine Schlafenszeit."
            else "Schlafenszeit-Erinnerung. In $minuten Minuten, um $bett, solltest du schlafen gehen"
        fun weck(e: Eintrag) = "${tag(e.wakeAt, ausloesung, zone)} um ${uhr(e.wakeAt, zone)}" + (e.sonne?.let { ", $it" } ?: "")
        return if (sortiert.size == 1) {
            (if (minuten <= 0) "$anfang So bekommst du ${dauer(erster.sleepMinutes)} Schlaf." else "$anfang, damit du ${dauer(erster.sleepMinutes)} Schlaf bekommst.") +
                " Geweckt wirst du ${weck(erster)}, von deinem Wecker „${erster.name}“."
        } else {
            (if (minuten <= 0) anfang else "$anfang.") + " Geweckt wirst du von " +
                sortiert.joinToString(" und von ") { "„${it.name}“ ${weck(it)}, nach ${dauer(it.sleepMinutes)} Schlaf" } + "."
        }
    }

    /** Text für ein geplantes Vorkommen, mit allen Weckern derselben Schlafenszeit; ohne Android-Zustand außer dem Store. */
    fun textFuer(context: Context, alarmId: String, wakeAt: Long, sleepMinutes: Int): String {
        val alle = AlarmStore.get(context).all()
        val eigener = alle.find { it.id == alarmId }
        val global = SchlafErinnerung.enabled(context)
        val v = SchlafPlan.Vorkommen(alarmId, wakeAt, sleepMinutes)
        val ausloesung = v.trigger(SchlafPlan.leadMs(SchlafPlan.vorlauf(eigener, SchlafErinnerung.leadMinutes(context))))
        val mitglieder = alle.mapNotNull { a -> SchlafPlan.inGruppe(a, v.group, ausloesung, SchlafPlan.an(a, global))?.let { a to it } }
            .ifEmpty { listOfNotNull(eigener?.let { it to v }) }
        return text(v.bedtime, ausloesung, mitglieder.map { (a, o) -> Eintrag(a.name, o.wakeAt, o.sleepMinutes, a.sunMode.takeIf { it.isNotBlank() }?.let { SonnenWecker.gesprochen(a) }) })
    }

    /** Vorschau für den Editor: die Ansage für den nächsten Termin dieses (auch ungespeicherten) Weckers. */
    fun vorschau(alarm: Alarm, vorlaufMinuten: Int, now: Long = System.currentTimeMillis()): String? = runCatching {
        val weck = AlarmTime.next(alarm, Instant.ofEpochMilli(now)).takeIf { it > 0 } ?: return null
        val v = SchlafPlan.Vorkommen(alarm.id, weck, alarm.sleepMinutes)
        text(v.bedtime, v.trigger(SchlafPlan.leadMs(vorlaufMinuten)),
            listOf(Eintrag(alarm.name, weck, alarm.sleepMinutes, alarm.sunMode.takeIf { it.isNotBlank() }?.let { SonnenWecker.gesprochen(alarm) })))
    }.getOrNull()

    // ---------- Audio vorbereiten ----------
    private fun schluessel(stimme: SyntheseStimme, text: String) = SpeechPreparation.hash("$stimme|$text")

    private fun cache(context: Context): JSONObject = runCatching { JSONObject(prefs(context).getString(CACHE_KEY, "{}") ?: "{}") }.getOrDefault(JSONObject())

    /** Die vorbereitete Datei zu diesem Text mit der aktuell gewählten Stimme, sonst null. */
    fun datei(context: Context, settings: SecureSettings, text: String): File? =
        cache(context).optString(schluessel(stimme(context, settings), text)).takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.length() > 44 }

    /** Erzeugt die Datei mit der gewählten Stimme, bei Fehlern mit der Edge-Notfallstimme, und merkt sie. */
    suspend fun rendern(context: Context, settings: SecureSettings, text: String): File {
        datei(context, settings, text)?.let { return it }
        val stimme = stimme(context, settings)
        val file = try { SpeechPreparation(context, settings) { stimme }.audio(text) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Log.w(TAG, "Gewählte Stimme fehlgeschlagen, Edge-Notfallstimme", e)
                SpeechPreparation(context, settings) { stimme.edgeFallback() }.audio(text)
            }
        synchronized(this) {
            val c = cache(context)
            c.put(schluessel(stimme, text), file.absolutePath)
            // Höchstens zehn Einträge; ältere Dateien werden entfernt.
            val keys = c.keys().asSequence().toList()
            if (keys.size > 10) keys.take(keys.size - 10).forEach { k -> c.optString(k).takeIf { it != file.absolutePath }?.let { File(it).delete() }; c.remove(k) }
            prefs(context).edit().putString(CACHE_KEY, c.toString()).apply()
        }
        return file
    }

    /** Bereitet die Ansage für jede geplante Erinnerung vor. Wirft nur bei echten Fehlern (für den Wiederholversuch). */
    suspend fun vorbereiten(context: Context, settings: SecureSettings) {
        if (!SchlafErinnerung.enabled(context)) return
        val alle = AlarmStore.get(context).all()
        val global = SchlafErinnerung.enabled(context)
        val now = System.currentTimeMillis()
        var fehler: Exception? = null
        alle.forEach { a ->
            val v = SchlafPlan.next(a, now, SchlafPlan.an(a, global), leadMinutes = SchlafPlan.vorlauf(a, SchlafErinnerung.leadMinutes(context)),
                andere = alle, anderAn = { SchlafPlan.an(it, global) }) ?: return@forEach
            try { rendern(context, settings, textFuer(context, a.id, v.wakeAt, v.sleepMinutes)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Log.w(TAG, "Ansage nicht vorbereitet für ${a.id}", e); fehler = e }
        }
        fehler?.let { throw it }
    }

    fun vorbereitenAnstossen(context: Context) {
        try {
            if (!context.getSystemService(UserManager::class.java).isUserUnlocked) return
            WorkManager.getInstance(context).enqueueUniqueWork("sleep-announce", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SchlafAnsageWorker>().setInitialDelay(5, TimeUnit.SECONDS).build())
        } catch (e: Exception) { Log.w(TAG, "Vorbereitung nicht angestoßen", e) }
    }

    // ---------- Abspielen ----------
    /**
     * Startet die Ansage für ein zugestelltes Vorkommen. Benachrichtigungs-Art bleibt bei „Nicht stören“ stumm (wie bisher);
     * die Wecker-Art klingt auch dann. Scheitert der Dienst, klingt wenigstens der bisherige Ton.
     */
    fun starten(context: Context, alarmId: String, wakeAt: Long, sleepMinutes: Int, beiEnde: () -> Unit) {
        val wecker = art(context) == ART_WECKER
        if (!wecker && SchlafTon.nichtStoerenAktiv(context)) { beiEnde(); return }
        try {
            val text = textFuer(context, alarmId, wakeAt, sleepMinutes)
            abspielen(context, text, wecker, runden = if (wecker) SchlafAnsageService.WECKER_RUNDEN else 1)
            beiEnde()
        } catch (e: Exception) {
            Log.w(TAG, "Ansage nicht gestartet, nur Ton", e)
            SchlafTon.spielen(context.applicationContext, beiEnde = beiEnde)
        }
    }

    fun abspielen(context: Context, text: String, wecker: Boolean, runden: Int) {
        val intent = Intent(context, SchlafAnsageService::class.java).putExtra("text", text).putExtra("wecker", wecker).putExtra("runden", runden)
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
    }

    /** „Ansage testen“: die echte Ansage des nächsten Termins, sonst ein Beispiel; einmal. */
    fun testen(context: Context) {
        val alle = AlarmStore.get(context).all()
        val global = SchlafErinnerung.enabled(context)
        val now = System.currentTimeMillis()
        val naechste = alle.mapNotNull { a ->
            SchlafPlan.next(a, now, SchlafPlan.an(a, true), leadMinutes = SchlafPlan.vorlauf(a, SchlafErinnerung.leadMinutes(context)),
                andere = alle, anderAn = { SchlafPlan.an(it, global) })
        }.minByOrNull { it.wakeAt }
        val text = naechste?.let { textFuer(context, it.alarmId, it.wakeAt, it.sleepMinutes) }
            ?: "Schlafenszeit-Erinnerung. In 30 Minuten, um 22 Uhr, solltest du schlafen gehen, damit du 9 Stunden Schlaf bekommst. Geweckt wirst du morgen um 7 Uhr."
        abspielen(context, text, art(context) == ART_WECKER, runden = 1)
    }

    fun stoppen(context: Context) {
        runCatching { context.startService(Intent(context, SchlafAnsageService::class.java).setAction(SchlafAnsageService.STOPP)) }
    }
}

class SchlafAnsageWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        SecureSettings(applicationContext).use { SchlafAnsage.vorbereiten(applicationContext, it) }
        Result.success()
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { if (runAttemptCount < 4) Result.retry() else Result.failure() }
}

/**
 * Spielt Erinnerungston und Ansage über den Alarm-Audiostrom in der Lautstärke der Erinnerung, mit kurzzeitigem
 * Audiofokus (ein laufendes Video pausiert). Art „Wecker“: Vollbild, wiederholt bis „Verstanden“ (höchstens [WECKER_RUNDEN]-mal).
 */
class SchlafAnsageService : Service() {
    companion object {
        const val STOPP = "de.frank.wecker.SCHLAF_ANSAGE_STOPP"
        const val WECKER_RUNDEN = 5
        private const val PAUSE_MS = 30_000L
        private const val NOTIFICATION_ID = 6100
        private const val KANAL_LEISE = "sleep_announce_v1"
        private const val KANAL_WECKER = "sleep_announce_alarm_v1"
        /** Laufender Ansagetext; leer = keine Ansage aktiv. Für den Vollbildschirm. */
        val text = MutableStateFlow("")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var player: MediaPlayer? = null
    private var vorherLautstaerke = -1
    private var fokus: AudioFocusRequest? = null
    private var letzterStart: Intent? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOPP || intent == null) { beenden(); return START_NOT_STICKY }
        val ansage = intent.getStringExtra("text").orEmpty()
        val wecker = intent.getBooleanExtra("wecker", false)
        val runden = intent.getIntExtra("runden", 1).coerceIn(1, WECKER_RUNDEN)
        letzterStart = intent
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(ansage, wecker),
                if (Build.VERSION.SDK_INT >= 29) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0)
        } catch (e: Exception) {
            Log.w("WeckerSchlafAnsage", "Vordergrunddienst nicht startbar", e)
            SchlafTon.spielen(applicationContext); stopSelf(); return START_NOT_STICKY
        }
        text.value = ansage
        job?.cancel()
        job = scope.launch {
            try { ablauf(ansage, runden) } finally { beenden() }
        }
        return START_NOT_STICKY
    }

    private suspend fun ablauf(ansage: String, runden: Int) {
        val (datei, tempo) = withContext(Dispatchers.IO) {
            SecureSettings(applicationContext).use { settings ->
                val stimme = SchlafAnsage.stimme(applicationContext, settings)
                (SchlafAnsage.datei(applicationContext, settings, ansage)
                    ?: withTimeoutOrNull(45_000) { runCatching { SchlafAnsage.rendern(applicationContext, settings, ansage) }.getOrNull() }) to stimme.playbackSpeed
            }
        }
        val audio = getSystemService(AudioManager::class.java)
        val attribute = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        if (Build.VERSION.SDK_INT >= 26) {
            fokus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attribute).build()
            runCatching { audio.requestAudioFocus(fokus!!) }
        }
        runCatching {
            vorherLautstaerke = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audio.setStreamVolume(AudioManager.STREAM_ALARM, (max * SchlafTon.lautstaerke(this) / 100f).roundToInt().coerceIn(1, max), 0)
        }
        for (runde in 1..runden) {
            spiele(Uri.parse(SchlafTon.gewaehlt(this).ifBlank { SchlafTon.standardUri() }), 8_000, 1f)
            if (datei != null) { delay(300); spiele(Uri.fromFile(datei), 180_000, tempo) }
            if (runde < runden) delay(PAUSE_MS)
        }
    }

    private suspend fun spiele(uri: Uri, hoechstens: Long, tempo: Float) {
        withTimeoutOrNull(hoechstens) {
            suspendCancellableCoroutine<Unit> { cont ->
                val mp = MediaPlayer()
                player = mp
                fun fertig() { if (cont.isActive) cont.resume(Unit) }
                try {
                    mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    mp.setDataSource(applicationContext, uri)
                    mp.setOnPreparedListener {
                        runCatching { if (tempo != 1f && Build.VERSION.SDK_INT >= 23) it.playbackParams = it.playbackParams.setSpeed(tempo) }
                        it.start()
                    }
                    mp.setOnCompletionListener { fertig() }
                    mp.setOnErrorListener { _, _, _ -> fertig(); true }
                    mp.prepareAsync()
                } catch (e: Exception) { Log.w("WeckerSchlafAnsage", "Nicht abspielbar: $uri", e); fertig() }
                cont.invokeOnCancellation { runCatching { mp.stop() } }
            }
        }
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
    }

    private fun beenden() {
        job?.cancel(); job = null
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
        val audio = getSystemService(AudioManager::class.java)
        if (vorherLautstaerke >= 0) { runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, vorherLautstaerke, 0) }; vorherLautstaerke = -1 }
        if (Build.VERSION.SDK_INT >= 26) fokus?.let { runCatching { audio.abandonAudioFocusRequest(it) } }
        fokus = null
        text.value = ""
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    override fun onDestroy() { beenden(); scope.cancel(); super.onDestroy() }

    private fun notification(ansage: String, wecker: Boolean): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(KANAL_LEISE, "Schlafenszeit-Ansage", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Läuft, solange die Schlafenszeit-Ansage vorgelesen wird."; setSound(null, null); enableVibration(false) })
        manager.createNotificationChannel(NotificationChannel(KANAL_WECKER, "Schlafenszeit-Wecker", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Schlafenszeit-Erinnerung als Wecker: Vollbild, bis du sie bestätigst."; setSound(null, null); enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC })
        val stopp = PendingIntent.getService(this, 1, Intent(this, SchlafAnsageService::class.java).setAction(STOPP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val vollbild = PendingIntent.getActivity(this, 2, Intent(this, SchlafAnsageActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, if (wecker) KANAL_WECKER else KANAL_LEISE).setSmallIcon(R.drawable.ic_wecker)
            .setContentTitle(if (wecker) "Schlafenszeit" else "Schlafenszeit-Ansage").setContentText(ansage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(ansage)).setOngoing(true).setSilent(true)
            .setCategory(if (wecker) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (wecker) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, if (wecker) "Verstanden" else "Stopp", stopp)
            .apply { if (wecker) setFullScreenIntent(vollbild, true).setContentIntent(vollbild) }
            .build()
    }
}
