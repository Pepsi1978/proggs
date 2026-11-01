package de.frank.jarvis.ablage

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.meldung.Meldungen
import de.frank.jarvis.meldung.Meldungsarten
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.speech.GespraechsFokus
import de.frank.jarvis.speech.Synthese
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Lässt fällige Erinnerungen erscheinen. Ein einziger Wecker im AlarmManager steht auf dem nächsten Zeitpunkt.
 *
 * Wo sie erscheint: Der Relay kennt das zuletzt benutzte Gerät (Anwesenheit). Ist das ein anderes, bleibt das Handy still und fragt
 * alle zwei Minuten nach. Wird dort nicht bestätigt und Frank ist weg, ist nach fünf Minuten wieder das Handy dran. Ohne Netz meldet sich das Handy immer.
 * Eine Bestätigung von irgendwo kommt über [AblageSync] herein und beendet Meldung, Stimme und Klingeln hier.
 */
object ErinnerungPlaner {
    const val AKTION_PRUEFEN = "de.frank.jarvis.ERINNERUNG_PRUEFEN"
    const val AKTION_BESTAETIGEN = "de.frank.jarvis.ERINNERUNG_BESTAETIGEN"
    const val AKTION_ZEIGEN = "de.frank.jarvis.ERINNERUNG_ZEIGEN"
    const val EXTRA_QUITTUNG = "quittung"
    const val EXTRA_OEFFNEN = "erinnerung_oeffnen"
    const val KANAL = "jarvis_erinnerung"
    private const val PREFS = "erinnerung_planer"
    private const val NACHFRAGE_MS = 2 * 60_000L

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pruefSperre = Mutex()
    private val netz = OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).callTimeout(4, TimeUnit.SECONDS).build()
    private val stimmen = ConcurrentHashMap<String, Job>()
    @Volatile private var gestartet = false

    /** Gerade klingelnde Auslösungen (Wecker): laufen, bis Frank ausdrücklich bestätigt. */
    private val _klingelt = MutableStateFlow<List<Faellig>>(emptyList())
    val klingelt: StateFlow<List<Faellig>> = _klingelt.asStateFlow()

    @Synchronized
    fun starte(context: Context) {
        if (gestartet) return
        gestartet = true
        val app = context.applicationContext
        kanalAnlegen(app)
        bereich.launch {
            Erinnerungen.stand.collectLatest {
                delay(300)
                runCatching { pruefen(app) }.onFailure { Logbuch.fehler("Erinnerung prüfen", it) }
                // Auch Änderungen anderer Geräte stehen so gleich im Live-Gedächtnis.
                de.frank.jarvis.gedaechtnis.Lagebild.anstossen(app, listOf(de.frank.jarvis.gedaechtnis.Bereich.ERINNERUNGEN), "Änderung an Erinnerungen")
                runCatching { ErinnerungStimme.vorbereiten(app) }.onFailure { Logbuch.fehler("Erinnerung Stimme vorbereiten", it) }
            }
        }
        // Anwesenheit: Solange das Handy entsperrt benutzt wird, meldet es das einmal je Minute.
        bereich.launch { while (true) { delay(60_000L); if (benutzt(app)) ziel(app) } }
        bereich.launch { while (true) { runCatching { ErinnerungStimme.vorbereiten(app) }.onFailure { Logbuch.fehler("Erinnerung Stimme vorbereiten", it) }; delay(10 * 60_000L) } }
    }

    private fun benutzt(app: Context) = app.getSystemService(PowerManager::class.java).isInteractive && !app.getSystemService(KeyguardManager::class.java).isKeyguardLocked

    /** Name des Geräts, das jetzt dran ist. Ohne Server, ohne Netz oder bei einer unlesbaren Antwort: dieses Handy. */
    private fun ziel(app: Context): String = runCatching {
        val e = Einstellungen.get(app)
        val url = "https://${e.serverHost}/j/${e.geheimnis}/ablage/anwesenheit".toHttpUrl().newBuilder().addQueryParameter("geraet", AblageQuellen.HANDY).addQueryParameter("aktiv", if (benutzt(app)) "1" else "0").build()
        netz.newCall(Request.Builder().url(url).build()).execute().use { r -> if (r.isSuccessful) JSONObject(r.body?.string().orEmpty()).optString("ziel").ifBlank { AblageQuellen.HANDY } else AblageQuellen.HANDY }
    }.getOrDefault(AblageQuellen.HANDY)

    private fun gezeigt(app: Context): Set<String> = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("gezeigt", emptySet()).orEmpty()
    private fun setzeGezeigt(app: Context, s: Set<String>) { app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("gezeigt", s).apply() }

    /** Der Kern: Bestätigtes verstummen lassen, Fälliges zeigen, nächsten Wecker stellen. Nach jeder Änderung, jedem Abgleich und jedem Wecker. */
    suspend fun pruefen(context: Context) = pruefSperre.withLock {
        val app = context.applicationContext
        val jetzt = System.currentTimeMillis()
        val faellig = Erinnerungen.faellig(app, jetzt)
        val offen = faellig.map { it.quittung }.toSet()
        // Anderswo bestätigt, gelöscht, erledigt oder der Termin verschoben: hier sofort Schluss.
        var schon = gezeigt(app)
        (schon - offen).forEach { verstummen(app, it) }
        schon = schon intersect offen
        _klingelt.value.filter { it.quittung !in offen }.forEach { verstummen(app, it.quittung) }
        val neu = faellig.filter { it.quittung !in schon }
        var warten = false
        if (neu.isNotEmpty()) {
            val hier = ziel(app) == AblageQuellen.HANDY
            if (hier) {
                neu.forEach { zeige(app, it) }
                schon = schon + neu.map { it.quittung }
            } else warten = true
        }
        setzeGezeigt(app, schon)
        // Ein Neustart beendet das Klingeln, nicht die Pflicht zum Ausschalten.
        faellig.filter { it.erinnerung.wecker && it.quittung in schon && _klingelt.value.none { k -> k.quittung == it.quittung } }.forEach { klingeln(app, it) }
        stelleWecker(app, Erinnerungen.naechste(app, jetzt).firstOrNull(), if (warten) jetzt + NACHFRAGE_MS else null)
    }

    private fun verstummen(app: Context, quittung: String) {
        stimmen.remove(quittung)?.cancel()
        app.getSystemService(NotificationManager::class.java).cancel(meldungsNummer(quittung))
        if (_klingelt.value.any { it.quittung == quittung }) {
            _klingelt.value = _klingelt.value.filterNot { it.quittung == quittung }
            ErinnerungKlingelDienst.abgleichen()
        }
    }

    fun bestaetige(context: Context, f: Faellig) {
        val app = context.applicationContext
        Erinnerungen.bestaetige(app, f)
        verstummen(app, f.quittung)
    }

    /** Aus einer Meldung oder vom Aus-Knopf; [quittung] null = alles, was gerade klingelt. */
    private fun bestaetigeAus(app: Context, quittung: String?) {
        val alle = Erinnerungen.alle(app).flatMap { it.termine() }
        if (quittung == null) _klingelt.value.forEach { bestaetige(app, it) }
        else alle.firstOrNull { it.quittung == quittung }?.let { bestaetige(app, it) } ?: verstummen(app, quittung)
    }

    private fun meldungsNummer(quittung: String) = 20_000 + (quittung.hashCode() and 0xFFFF)

    private fun absicht(app: Context, aktion: String, quittung: String?, code: Int) = PendingIntent.getBroadcast(app, code,
        Intent(app, ErinnerungEmpfaenger::class.java).setAction(aktion).putExtra(EXTRA_QUITTUNG, quittung), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun oeffnenAbsicht(app: Context, id: String, code: Int): PendingIntent = PendingIntent.getActivity(app, code,
        Intent(app, MainActivity::class.java).putExtra(EXTRA_OEFFNEN, id).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun zeige(app: Context, f: Faellig) {
        val e = f.erinnerung
        Logbuch.system("Erinnerung fällig", "${e.zeile} (${e.form})")
        if (e.wecker) { klingeln(app, f); return }
        val nummer = meldungsNummer(f.quittung)
        val ok = absicht(app, AKTION_BESTAETIGEN, f.quittung, nummer)
        val bauer = NotificationCompat.Builder(app, KANAL).setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle(e.zeile).setContentText(e.kurz).setStyle(NotificationCompat.BigTextStyle().bigText(e.kurz))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(oeffnenAbsicht(app, e.id, nummer)).setAutoCancel(false)
            .addAction(R.drawable.ic_stat_jarvis, "Bestätigen", ok)
            // Wegwischen zählt als Bestätigung, sonst käme dieselbe Auslösung auf den anderen Geräten wieder.
            .setDeleteIntent(ok)
        Meldungen.zeige(app, Meldungsarten.ERINNERUNG, nummer, bauer, e.zeile, null)
        stimmen[f.quittung] = bereich.launch { ErinnerungStimme.sprich(app, e, alsWecker = false) }
    }

    private fun klingeln(app: Context, f: Faellig) {
        _klingelt.value = _klingelt.value.filterNot { it.quittung == f.quittung } + f
        runCatching { androidx.core.content.ContextCompat.startForegroundService(app, Intent(app, ErinnerungKlingelDienst::class.java)) }
            .onFailure { Logbuch.fehler("Erinnerung klingeln", it) }
    }

    private fun stelleWecker(app: Context, naechste: Faellig?, nachfrage: Long?) {
        val wecker = app.getSystemService(AlarmManager::class.java)
        val pi = absicht(app, AKTION_PRUEFEN, null, 19_999)
        val zeit = listOfNotNull(naechste?.zeit, nachfrage).minOrNull()
        if (zeit == null) { wecker.cancel(pi); return }
        runCatching {
            val exakt = Build.VERSION.SDK_INT < 31 || wecker.canScheduleExactAlarms()
            when {
                // Nur eine echte Wecker-Erinnerung zeigt das Wecker-Symbol des Handys.
                exakt && naechste?.erinnerung?.wecker == true && naechste.zeit == zeit -> wecker.setAlarmClock(AlarmManager.AlarmClockInfo(zeit, oeffnenAbsicht(app, naechste.erinnerung.id, 19_998)), pi)
                exakt -> wecker.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeit, pi)
                else -> wecker.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, zeit, pi)
            }
        }.onFailure { Logbuch.fehler("Erinnerungs-Wecker stellen", it) }
    }

    private fun kanalAnlegen(app: Context) {
        app.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(KANAL, "Erinnerungen", NotificationManager.IMPORTANCE_HIGH).apply { description = "Fällige Erinnerungen aus dem Reiter Erinnerung der Ablage." })
    }

    fun empfangen(context: Context, intent: Intent, fertig: () -> Unit) {
        val app = context.applicationContext
        bereich.launch {
            try {
                when (intent.action) {
                    AKTION_BESTAETIGEN -> bestaetigeAus(app, intent.getStringExtra(EXTRA_QUITTUNG))
                    AKTION_ZEIGEN -> ErinnerungKlingelDienst.abgleichen()
                    else -> Unit
                }
                pruefen(app)
            } catch (e: Exception) { Logbuch.fehler("Erinnerung empfangen", e) } finally { fertig() }
        }
    }
}

class ErinnerungEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rest = goAsync()
        ErinnerungPlaner.empfangen(context, intent) { rest.finish() }
    }
}

/**
 * Die Stimme der Erinnerungen: Die Kurzfassung wird vorab mit der Standardstimme von Jarvis (oder der unter Einstellungen → Benachrichtigungen gewählten) erzeugt und
 * lokal gehalten; so liest sie auch ohne Netz vor. Fehlt die Datei doch, wird sie jetzt erzeugt; scheitert das, liest die Systemstimme des Handys.
 * Jede Änderung von Text oder Stimme ergibt einen neuen Dateinamen. Die Stimmen selbst bleiben unberührt.
 */
object ErinnerungStimme {
    private val attributeMeldung = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val attributeWecker = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()

    private fun ordner(app: Context) = File(app.filesDir, "erinnerung-stimme").apply { mkdirs() }

    private fun schluessel(app: Context, kurz: String): String {
        val s = SecureSettings(app)
        val p = Meldungen.profil(app, Meldungsarten.ERINNERUNG)
        val roh = listOf(kurz, s.ttsProvider, s.edgeTtsVoice, s.googleTtsVoice, s.azureTtsVoice, s.geminiFlashTtsVoice, s.geminiLiteTtsVoice, s.qwenStandardVoice, p.stimme, p.tempo, s.ttsTempo(s.ttsProvider)).joinToString("|")
        return MessageDigest.getInstance("SHA-256").digest(roh.toByteArray()).joinToString("") { "%02x".format(it) }.take(32)
    }

    private fun datei(app: Context, kurz: String): File? { val k = schluessel(app, kurz); return ordner(app).listFiles()?.firstOrNull { it.name.startsWith("$k.") && it.length() > 0 } }

    /** Liegt die Sprachausgabe für diese Kurzfassung fertig auf dem Handy? */
    fun bereit(context: Context, kurz: String) = datei(context.applicationContext, kurz) != null

    private suspend fun erzeuge(app: Context, kurz: String): File {
        val s = SecureSettings(app)
        val roh = Synthese(app, s).synthetisiere(kurz, Meldungen.profil(app, Meldungsarten.ERINNERUNG).stimmWahl)
        val ziel = File(ordner(app), schluessel(app, kurz) + "." + roh.extension.ifBlank { "audio" })
        roh.copyTo(ziel, overwrite = true)
        return ziel
    }

    /** Erzeugt, was fehlt, und räumt auf, was niemand mehr braucht. Fehlt das Netz, versucht es der nächste Lauf. */
    suspend fun vorbereiten(context: Context) {
        val app = context.applicationContext
        val jetzt = System.currentTimeMillis()
        val quittungen = Erinnerungen.quittungen(app)
        val offen = Erinnerungen.alle(app).filter { e -> e.erledigt == 0L && (e.allgemein || e.termine().any { it.quittung !in quittungen && jetzt - it.zeit < Erinnerungen.VERPASST_NACH_MS }) }
        val gebraucht = offen.map { schluessel(app, it.kurz) }.toSet()
        ordner(app).listFiles()?.filter { it.nameWithoutExtension !in gebraucht }?.forEach { it.delete() }
        for (e in offen) if (datei(app, e.kurz) == null) {
            try { erzeuge(app, e.kurz) } catch (c: kotlinx.coroutines.CancellationException) { throw c } catch (f: Exception) { Logbuch.netz("Erinnerung Stimme", "noch nicht erzeugt: ${f.message}"); return }
        }
    }

    /** Liest die Kurzfassung vor. Eine laufende Aufnahme oder ein Gespräch wartet es ab, höchstens 15 Minuten; ein Wecker spricht ohne zu warten. */
    suspend fun sprich(context: Context, e: Erinnerung, alsWecker: Boolean) {
        val app = context.applicationContext
        if (!alsWecker) {
            withTimeoutOrNull(15 * 60_000L) { while (GespraechsFokus.stilleHinweise || GespraechsFokus.aufnahme) delay(1_000L) } ?: return
        }
        val wach = app.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:erinnerung-stimme").apply { setReferenceCounted(false) }
        runCatching { wach.acquire(3 * 60_000L) }
        try {
            val f = datei(app, e.kurz) ?: withTimeoutOrNull(12_000L) { runCatching { erzeuge(app, e.kurz) }.getOrNull() }
            if (f == null || !spiele(f, if (alsWecker) attributeWecker else attributeMeldung)) systemstimme(app, e.kurz)
        } finally { runCatching { if (wach.isHeld) wach.release() } }
    }

    private suspend fun spiele(f: File, attribute: AudioAttributes): Boolean = suspendCancellableCoroutine { w ->
        val p = MediaPlayer()
        fun los(ok: Boolean) { runCatching { p.release() }; if (w.isActive) w.resume(ok) }
        w.invokeOnCancellation { runCatching { p.release() } }
        runCatching {
            p.setAudioAttributes(attribute)
            p.setDataSource(f.path)
            p.setOnCompletionListener { los(true) }
            p.setOnErrorListener { _, _, _ -> los(false); true }
            p.prepare()
            p.start()
        }.onFailure { los(false) }
    }

    private suspend fun systemstimme(app: Context, text: String) {
        var tts: TextToSpeech? = null
        withTimeoutOrNull(60_000L) {
            suspendCancellableCoroutine<Unit> { w ->
                w.invokeOnCancellation { runCatching { tts?.stop() } }
                tts = TextToSpeech(app) { status ->
                    if (status != TextToSpeech.SUCCESS) { if (w.isActive) w.resume(Unit); return@TextToSpeech }
                    tts?.language = Locale.GERMAN
                    tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(id: String?) {}
                        override fun onDone(id: String?) { if (w.isActive) w.resume(Unit) }
                        @Deprecated("Pflicht der Schnittstelle") override fun onError(id: String?) { if (w.isActive) w.resume(Unit) }
                    })
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "erinnerung")
                }
            }
        }
        runCatching { tts?.shutdown() }
    }
}
