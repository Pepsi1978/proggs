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
import android.media.AudioManager
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
    private val stimmAnstoss = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.CONFLATED)
    /** Erster Abgleich nach einer Neuinstallation: Bestätigungen fehlen noch, also nichts Neues zeigen. */
    @Volatile var abgleichPause = false
    /** Wecker, deren Klingeldienst Android nicht starten ließ: Sie klingeln über die Meldung. */
    private val ersatzKlingeln: MutableSet<String> = ConcurrentHashMap.newKeySet()
    @Volatile private var faelligeErinnerungen: List<Faellig> = emptyList()

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
                stimmAnstoss.trySend(Unit)
            }
        }
        // Anwesenheit: Solange das Handy entsperrt benutzt wird, meldet es das einmal je Minute.
        bereich.launch { while (true) { delay(60_000L); if (benutzt(app)) ziel(app) } }
        // Ein einziger Arbeiter: Eine laufende Sprachausgabe wird nie abgebrochen und nie doppelt bezahlt.
        bereich.launch { while (true) { runCatching { ErinnerungStimme.vorbereiten(app) }.onFailure { Logbuch.fehler("Erinnerung Stimme vorbereiten", it) }; withTimeoutOrNull(10 * 60_000L) { stimmAnstoss.receive() } } }
    }

    fun anstossen(context: Context) { val app = context.applicationContext; bereich.launch { runCatching { pruefen(app) }.onFailure { Logbuch.fehler("Erinnerung prüfen", it) } } }

    private fun benutzt(app: Context) = app.getSystemService(PowerManager::class.java).isInteractive && !app.getSystemService(KeyguardManager::class.java).isKeyguardLocked

    /**
     * Name des Geräts, das jetzt dran ist. Ohne Server oder ohne Netz: dieses Gerät selbst (Handy wie Tablet). Lieber erscheint eine
     * Erinnerung dann auf beiden als auf keinem; nur eine leere Antwort des Servers meint wie bisher das Handy.
     */
    private fun ziel(app: Context): String = runCatching {
        val e = Einstellungen.get(app)
        val url = "https://${e.serverHost}/j/${e.geheimnis}/ablage/anwesenheit".toHttpUrl().newBuilder().addQueryParameter("geraet", AblageQuellen.DIESES).addQueryParameter("aktiv", if (benutzt(app)) "1" else "0").build()
        netz.newCall(Request.Builder().url(url).build()).execute().use { r -> if (r.isSuccessful) JSONObject(r.body?.string().orEmpty()).optString("ziel").ifBlank { AblageQuellen.HANDY } else AblageQuellen.DIESES }
    }.getOrDefault(AblageQuellen.DIESES)

    private fun gezeigt(app: Context): Set<String> = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("gezeigt", emptySet()).orEmpty()
    private fun setzeGezeigt(app: Context, s: Set<String>) { app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("gezeigt", s).apply() }

    /** Der Kern: Bestätigtes verstummen lassen, Fälliges zeigen, nächsten Wecker stellen. Nach jeder Änderung, jedem Abgleich und jedem Wecker. */
    suspend fun pruefen(context: Context) = pruefSperre.withLock {
        val app = context.applicationContext
        // Vorübergehend unlesbar heißt nicht „alles gelöscht“.
        if (Erinnerungen.defekt(app)) return@withLock
        val jetzt = System.currentTimeMillis()
        val faellig = Erinnerungen.faellig(app, jetzt)
        faelligeErinnerungen = faellig
        val offen = faellig.map { it.quittung }.toSet()
        // Anderswo bestätigt, gelöscht, erledigt oder der Termin verschoben: hier sofort Schluss.
        var schon = gezeigt(app)
        (schon - offen).forEach { verstummen(app, it) }
        schon = schon intersect offen
        val alsWecker = faellig.filter { it.erinnerung.wecker }.map { it.quittung }.toSet()
        // Auch wenn die Form inzwischen Benachrichtigung ist: Der Ton hört auf, die Meldung erscheint unten neu.
        _klingelt.value.filter { it.quittung !in alsWecker }.forEach { verstummen(app, it.quittung) }
        (ersatzKlingeln - alsWecker).forEach { verstummen(app, it) }
        // Auch Textänderungen einer schon klingelnden Auslösung erreichen den Vorleseablauf.
        _klingelt.value = _klingelt.value.mapNotNull { alt -> faellig.firstOrNull { it.quittung == alt.quittung && it.erinnerung.wecker } }
        // Der Live-Sprachmodus hat Vorrang: Ein Wecker wartet das Gespräch ab und klingelt danach.
        val live = GespraechsFokus.stilleHinweise
        val neu = faellig.filter { it.quittung !in schon && !(it.erinnerung.wecker && live) }
        var warten = faellig.any { it.quittung !in schon && it.erinnerung.wecker && live }
        val jetztGezeigt = HashSet<String>()
        if (neu.isNotEmpty()) {
            if (!abgleichPause && ziel(app) == AblageQuellen.DIESES) {
                neu.forEach { zeige(app, it); jetztGezeigt += it.quittung }
                schon = schon + jetztGezeigt
            } else warten = true
        }
        setzeGezeigt(app, schon)
        // Ein Neustart beendet Klingeln und Meldung, nicht die Pflicht zu bestätigen. Vorgelesen wird dabei nicht noch einmal.
        val sichtbar = runCatching { app.getSystemService(NotificationManager::class.java).activeNotifications.map { it.id }.toSet() }.getOrDefault(emptySet())
        faellig.filter { it.quittung in schon && it.quittung !in jetztGezeigt }.forEach { f ->
            if (f.erinnerung.wecker) { if (!live && f.quittung !in ersatzKlingeln && _klingelt.value.none { k -> k.quittung == f.quittung }) klingeln(app, f) }
            else if (meldungsNummer(f.quittung) !in sichtbar) zeigeMeldung(app, f, sprechen = false)
        }
        stelleWecker(app, Erinnerungen.naechste(app, jetzt).firstOrNull(), if (warten) jetzt + NACHFRAGE_MS else null)
    }

    private fun verstummen(app: Context, quittung: String) {
        stimmen.remove(quittung)?.cancel()
        val nummer = meldungsNummer(quittung)
        app.getSystemService(NotificationManager::class.java).cancel(nummer)
        de.frank.jarvis.meldung.WeckerTon.stopp(nummer)
        ersatzKlingeln.remove(quittung)
        if (_klingelt.value.any { it.quittung == quittung }) {
            _klingelt.value = _klingelt.value.filterNot { it.quittung == quittung }
            ErinnerungKlingelDienst.abgleichen()
        }
    }

    /** false = nicht gespeichert: Dann klingelt es weiter, statt dass die Bestätigung still verloren geht. */
    fun bestaetige(context: Context, f: Faellig): Boolean {
        val app = context.applicationContext
        val ok = Erinnerungen.bestaetige(app, f)
        if (ok) verstummen(app, f.quittung)
        return ok
    }

    /** Aus einer Meldung oder vom Aus-Knopf; [quittung] null = alles, was gerade klingelt. */
    private fun bestaetigeAus(app: Context, quittung: String?) {
        val alle = Erinnerungen.alle(app).flatMap { it.termine() }
        if (quittung == null) (_klingelt.value + alle.filter { it.quittung in ersatzKlingeln }).forEach { bestaetige(app, it) }
        else alle.firstOrNull { it.quittung == quittung }?.let { bestaetige(app, it) } ?: verstummen(app, quittung)
    }

    /** „Wecker ausschalten“ auf der Wecker-Karte: bestätigt alles, was gerade klingelt. */
    fun ausschalten(context: Context) {
        val app = context.applicationContext
        bereich.launch { runCatching { bestaetigeAus(app, null); pruefen(app) }.onFailure { Logbuch.fehler("Wecker ausschalten", it) } }
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
        zeigeMeldung(app, f, sprechen = true)
    }

    private fun zeigeMeldung(app: Context, f: Faellig, sprechen: Boolean) {
        val e = f.erinnerung
        val nummer = meldungsNummer(f.quittung)
        val ok = absicht(app, AKTION_BESTAETIGEN, f.quittung, nummer)
        val bauer = NotificationCompat.Builder(app, KANAL).setSmallIcon(R.drawable.ic_stat_jarvis)
            .setContentTitle(e.zeile).setContentText(e.kurz).setStyle(NotificationCompat.BigTextStyle().bigText(e.kurz))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(oeffnenAbsicht(app, e.id, nummer)).setAutoCancel(false)
            .addAction(R.drawable.ic_stat_jarvis, "Bestätigen", ok)
            // Wegwischen zählt als Bestätigung, sonst käme dieselbe Auslösung auf den anderen Geräten wieder.
            .setDeleteIntent(ok)
        val art = Meldungen.erinnerungsArt(e.wecker)
        Meldungen.zeige(app, art, nummer, bauer, e.zeile, null, laut = sprechen && !e.wecker)
        if (sprechen) {
            stimmen.remove(f.quittung)?.cancel()
            stimmen[f.quittung] = if (e.wecker) ErinnerungsWeckablauf.starten(app, nummer) {
                if (f.quittung !in ersatzKlingeln) emptyList()
                else faelligeErinnerungen.filter { it.quittung == f.quittung && it.erinnerung.wecker }
            } else bereich.launch {
                if (Meldungen.profil(app, art).ton) delay(1_000L)
                ErinnerungStimme.sprich(app, e, alsWecker = false)
            }
        }
    }

    private fun klingeln(app: Context, f: Faellig) {
        // Die Meldung der Benachrichtigungs-Form gehört nicht neben den Wecker.
        app.getSystemService(NotificationManager::class.java).cancel(meldungsNummer(f.quittung))
        _klingelt.value = _klingelt.value.filterNot { it.quittung == f.quittung } + f
        runCatching { androidx.core.content.ContextCompat.startForegroundService(app, Intent(app, ErinnerungKlingelDienst::class.java)) }
            .onFailure { Logbuch.fehler("Erinnerung klingeln", it); dienstGescheitert(app) }
    }

    /** Android lässt den Klingeldienst nicht zu (etwa direkt nach dem Hochfahren): Weckton über die Meldung, ausgeschaltet wird genauso. */
    fun dienstGescheitert(context: Context) {
        val app = context.applicationContext
        _klingelt.value.forEach { f ->
            ersatzKlingeln += f.quittung
            zeigeMeldung(app, f, sprechen = true)
        }
        _klingelt.value = emptyList()
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

    private fun schluessel(app: Context, kurz: String, alsWecker: Boolean): String {
        val s = SecureSettings(app)
        val p = Meldungen.profil(app, Meldungen.erinnerungsArt(alsWecker))
        val roh = listOf(kurz, s.ttsProvider, s.edgeTtsVoice, s.googleTtsVoice, s.azureTtsVoice, s.geminiFlashTtsVoice, s.geminiLiteTtsVoice, s.qwenStandardVoice, p.stimme, p.tempo, s.ttsTempo(s.ttsProvider)).joinToString("|")
        return MessageDigest.getInstance("SHA-256").digest(roh.toByteArray()).joinToString("") { "%02x".format(it) }.take(32)
    }

    private fun datei(app: Context, kurz: String, alsWecker: Boolean): File? { val k = schluessel(app, kurz, alsWecker); return ordner(app).listFiles()?.firstOrNull { it.name.startsWith("$k.") && it.length() > 0 } }

    /** Liegt die Sprachausgabe für diese Kurzfassung fertig auf dem Handy? */
    fun bereit(context: Context, kurz: String, alsWecker: Boolean = false) = datei(context.applicationContext, kurz, alsWecker) != null

    private val erzeugung = Mutex()

    private suspend fun erzeuge(app: Context, kurz: String, alsWecker: Boolean): File = erzeugung.withLock {
        datei(app, kurz, alsWecker)?.let { return@withLock it }
        val s = SecureSettings(app)
        // Die Kennung muss vor der Netzarbeit feststehen; eine inzwischen andere Stimme darf den alten Ton nicht übernehmen.
        val k = schluessel(app, kurz, alsWecker)
        val roh = Synthese(app, s).synthetisiere(kurz, Meldungen.profil(app, Meldungen.erinnerungsArt(alsWecker)).stimmWahl)
        val ziel = File(ordner(app), k + "." + roh.extension.ifBlank { "audio" })
        // Erst fertig unter dem richtigen Namen: Eine halbe Datei gälte sonst dauerhaft als bereit.
        val teil = File(ordner(app), "teil-$k-" + System.nanoTime())
        roh.copyTo(teil, overwrite = true)
        if (!teil.renameTo(ziel)) { teil.delete(); throw java.io.IOException("Sprachdatei nicht ablegbar") }
        ziel
    }

    /** Erzeugt, was fehlt, und räumt auf, was niemand mehr braucht. Fehlt das Netz, versucht es der nächste Lauf. */
    suspend fun vorbereiten(context: Context) {
        val app = context.applicationContext
        val jetzt = System.currentTimeMillis()
        val quittungen = Erinnerungen.quittungen(app)
        val offen = Erinnerungen.alle(app).filter { e -> e.erledigt == 0L && (e.allgemein || e.termine().any { it.quittung !in quittungen && jetzt - it.zeit < Erinnerungen.VERPASST_NACH_MS }) }
        val gebraucht = offen.map { schluessel(app, it.kurz, it.wecker) }.toSet()
        erzeugung.withLock { ordner(app).listFiles()?.filter { it.nameWithoutExtension !in gebraucht }?.forEach { it.delete() } }
        for (e in offen) if (Meldungen.vorlesenAn(app, Meldungen.erinnerungsArt(e.wecker)) && datei(app, e.kurz, e.wecker) == null) {
            try { erzeuge(app, e.kurz, e.wecker) } catch (c: kotlinx.coroutines.CancellationException) { throw c } catch (f: Exception) { Logbuch.netz("Erinnerung Stimme", "noch nicht erzeugt: ${f.message}"); return }
        }
    }

    /** Liest die Kurzfassung vor. Eine laufende Aufnahme oder ein Gespräch wartet es ab, höchstens 15 Minuten; ein Wecker spricht ohne zu warten. */
    suspend fun sprich(context: Context, e: Erinnerung, alsWecker: Boolean) = sprichText(context, e.kurz, alsWecker)

    /** Keine Erinnerung anlegen: Die Probe verwendet dieselben beiden Stimmenwege wie eine fällige Meldung. */
    suspend fun probe(context: Context, alsWecker: Boolean = false) {
        val art = Meldungen.erinnerungsArt(alsWecker)
        val nummer = if (alsWecker) 4_490 else 4_491
        Meldungen.ereignis(context, art, nummer, "Probe: Erinnerungen", "So meldet Jarvis deine Erinnerungen.")
        if (alsWecker) while (de.frank.jarvis.meldung.WeckerTon.klingelt(nummer)) delay(250L)
        else if (Meldungen.profil(context, art).ton) delay(1_000L)
        sprichText(context, "Probe. So liest Jarvis deine Erinnerungen vor.", alsWecker)
    }

    private suspend fun sprichText(context: Context, kurz: String, alsWecker: Boolean) {
        val app = context.applicationContext
        val art = Meldungen.erinnerungsArt(alsWecker)
        if (!Meldungen.vorlesenAn(app, art)) return
        withTimeoutOrNull(15 * 60_000L) { while (GespraechsFokus.stilleHinweise || GespraechsFokus.aufnahme) delay(500L) } ?: return
        val wach = app.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:erinnerung-stimme").apply { setReferenceCounted(false) }
        runCatching { wach.acquire(3 * 60_000L) }
        try {
            // Mehrere zugleich fällige sprechen nacheinander, nicht übereinander.
            reihe.withLock {
                if (!Meldungen.vorlesenAn(app, art)) return@withLock
                val f = datei(app, kurz, alsWecker) ?: withTimeoutOrNull(12_000L) {
                    try { kotlinx.coroutines.withContext(Dispatchers.IO) { erzeuge(app, kurz, alsWecker) } }
                    catch (c: kotlinx.coroutines.CancellationException) { throw c }
                    catch (e: Exception) { Logbuch.netz("Erinnerung Stimme", "Systemstimme: ${e.message}"); null }
                }
                // Während der Synthese kann Frank Vorlesen ausschalten oder ein Gespräch anfangen.
                withTimeoutOrNull(15 * 60_000L) { while (GespraechsFokus.stilleHinweise || GespraechsFokus.aufnahme) delay(500L) } ?: return@withLock
                if (!Meldungen.vorlesenAn(app, art)) return@withLock
                val attribute = if (alsWecker) attributeWecker else attributeMeldung
                val stream = if (alsWecker) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
                de.frank.jarvis.speech.Ansage.mitLautstaerke(app, Meldungen.profil(app, art).lautstaerke, stream) {
                    if (f == null || !spiele(f, attribute)) {
                        val s = SecureSettings(app)
                        val tempo = Meldungen.profil(app, art).tempo.takeIf { it > 0f } ?: s.ttsTempo(s.ttsProvider)
                        systemstimme(app, kurz, attribute, tempo)
                    }
                }
            }
        } finally { runCatching { if (wach.isHeld) wach.release() } }
    }

    private val reihe = Mutex()

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

    private suspend fun systemstimme(app: Context, text: String, attribute: AudioAttributes, tempo: Float) {
        // Erst warten, bis der Dienst bereit ist: Sein Rückruf kann kommen, bevor der Konstruktor zurückgekehrt ist.
        val bereit = kotlinx.coroutines.CompletableDeferred<Int>()
        val tts = TextToSpeech(app) { bereit.complete(it) }
        try {
            withTimeoutOrNull(60_000L) {
                if (bereit.await() != TextToSpeech.SUCCESS) return@withTimeoutOrNull
                suspendCancellableCoroutine<Unit> { w ->
                    w.invokeOnCancellation { runCatching { tts.stop() } }
                    tts.language = Locale.GERMAN
                    tts.setSpeechRate(tempo.coerceIn(0.5f, 2f))
                    tts.setAudioAttributes(attribute)
                    tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(id: String?) {}
                        override fun onDone(id: String?) { if (w.isActive) w.resume(Unit) }
                        @Deprecated("Pflicht der Schnittstelle") override fun onError(id: String?) { if (w.isActive) w.resume(Unit) }
                    })
                    if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "erinnerung") == TextToSpeech.ERROR && w.isActive) w.resume(Unit)
                }
            }
        } finally { runCatching { tts.shutdown() } }
    }
}
