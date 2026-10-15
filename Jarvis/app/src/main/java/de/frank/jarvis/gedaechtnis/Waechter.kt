package de.frank.jarvis.gedaechtnis

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.CalendarContract
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.faehigkeit.TagebuchFaehigkeit
import de.frank.jarvis.observability.Logbuch
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

private const val AKTION_UPDATE_SIGNAL = "de.frank.updatestation.UPDATE_SIGNAL"
private const val PAKET_UPDATESTATION = "de.frank.updatestation"

/**
 * Der Hintergrund-Agent des Live-Gedächtnisses. Läuft im Dienst von Jarvis, also rund um die Uhr:
 *
 * - **Änderungssignale:** Geniale Aufgaben, Geniale Ideen, Genialer Wecker, Stack-Labor und Entropie Reductor melden jede Änderung
 *   über ihre Jarvis-Brücke (`notifyChange`), der Kalender des Handys ebenso. Der Wächter liest dann genau den
 *   betroffenen Bereich neu, entprellt um 1,5 Sekunden (Kalender-Synchronisierungen kommen in Schüben).
 * - **Sicherheitsnetz:** Er frischt jeden Bereich auf, dessen Abstand abgelaufen ist (die signalfähigen Apps alle
 *   30 Minuten, Lokales alle 10 bis 15). Mehr braucht es nicht: Signale, eigene Schreibaktionen und jede Frage frischen
 *   die Bereiche ohnehin sofort auf. Er schaut jede Minute nach, liest aber nur Fälliges.
 * - **Neuer Tag:** Nach Mitternacht liest er alles neu (heute, morgen und die Rückblicke verschieben sich).
 * - **Lernen:** Ruht ein Gespräch zehn Minuten, lernt er daraus (siehe [Gespraechsgedaechtnis.lerne]).
 */
object Waechter {
    private class Quelle(val name: String, val adresse: Uri, val bereiche: List<Bereich>)

    private val QUELLEN = listOf(
        Quelle("Geniale Aufgaben", Uri.parse("content://de.frank.aufgaben.jarvis"), listOf(Bereich.AUFGABEN)),
        Quelle("Geniale Ideen", Uri.parse("content://de.frank.genialeideen.jarvis"), listOf(Bereich.IDEEN)),
        Quelle("Genialer Wecker", Uri.parse("content://de.frank.genialerwecker.jarvis"), listOf(Bereich.WECKER)),
        Quelle("Stack-Labor", Uri.parse("content://de.frank.stacklabor.werftstudio.jarvis"), listOf(Bereich.STACKS)),
        Quelle("Entropie Reductor", Uri.parse("content://de.frank.entropyreducer.jarvis"), listOf(Bereich.BIODATEN, Bereich.TRAININGS)),
        Quelle("Entropie Reductor", Uri.parse("content://de.frank.entropyreducer.debug.jarvis"), listOf(Bereich.BIODATEN, Bereich.TRAININGS)),
        Quelle("Kalender", CalendarContract.CONTENT_URI, listOf(Bereich.TERMINE, Bereich.RAHMEN)),
    )

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var schleife: Job? = null
    private val beobachter = HashMap<Uri, ContentObserver>()
    private val entprellt = HashMap<String, Job>()
    private val handler by lazy { Handler(HandlerThread("jarvis-waechter").apply { start() }.looper) }

    @Synchronized
    fun starte(context: Context) {
        val app = context.applicationContext
        if (schleife?.isActive == true) return
        Lagebild.lade(app)
        registriere(app)
        schleife = bereich.launch { laufe(app) }
        Logbuch.gedaechtnis("Wächter gestartet", "${beobachter.size} von ${QUELLEN.size} Quellen melden Änderungen")
    }

    @Synchronized
    fun stoppe(context: Context) {
        schleife?.cancel()
        schleife = null
        beobachter.values.forEach { runCatching { context.applicationContext.contentResolver.unregisterContentObserver(it) } }
        beobachter.clear()
    }

    /** Meldet sich bei allen Quellen an, die noch fehlen. Fehlt eine App oder die Kalender-Erlaubnis, folgt der nächste Versuch später. */
    @Synchronized
    private fun registriere(app: Context) {
        QUELLEN.filter { it.adresse !in beobachter }.forEach { q ->
            val b = object : ContentObserver(handler) {
                override fun onChange(selbst: Boolean) = signal(app, q)
                override fun onChange(selbst: Boolean, uri: Uri?) = signal(app, q)
            }
            runCatching { app.contentResolver.registerContentObserver(q.adresse, true, b); beobachter[q.adresse] = b }
        }
    }

    private fun signal(app: Context, q: Quelle) {
        synchronized(entprellt) {
            entprellt[q.name]?.cancel()
            entprellt[q.name] = bereich.launch {
                delay(1_500)
                val beginn = System.currentTimeMillis()
                // Immer neu lesen: Das Entprellen bündelt die Signale schon, und eine gerade endende ältere Lesung darf die Änderung nicht verdecken.
                val neu = runCatching { Lagebild.auffrischen(app, q.bereiche, "Änderung in ${q.name}") }.getOrDefault(false)
                Logbuch.gedaechtnis("Änderungssignal ${q.name}", if (neu) "Gedächtnis aktualisiert" else "ohne inhaltliche Änderung", System.currentTimeMillis() - beginn)
            }
        }
    }

    /** Ein Signal des Servers über die Verbindung. „tagebuch“: In Drive kam ein Eintrag dazu oder wurde geändert. */
    fun serverSignal(context: Context, nachricht: JSONObject) {
        val app = context.applicationContext
        val art = nachricht.optString("signal")
        if (art == "update") {
            // Neue APK im Update-Ordner (apk-update am PC): UpdateStation sofort wecken, sie prüft und lädt vor.
            val wecken = Intent(AKTION_UPDATE_SIGNAL).setPackage(PAKET_UPDATESTATION)
                .putExtra("projekt", nachricht.optString("projekt"))
                .putExtra("paket", nachricht.optString("paket"))
                .putExtra("versionCode", nachricht.optLong("versionCode"))
            runCatching { app.sendBroadcast(wecken) }.onFailure { Logbuch.fehler("Signal Update an UpdateStation", it) }
            Logbuch.netz("Signal Update", "${nachricht.optString("projekt")} vc ${nachricht.optLong("versionCode")} an UpdateStation")
            return
        }
        if (art == "programmieren") {
            // Fortschritt oder Rückfrage einer Programmier-Sitzung am PC (Hook über den Relay): Benachrichtigung und Gespräch.
            runCatching { Register.alle(app).filterIsInstance<de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit>().firstOrNull()?.meldungVomServer(nachricht) }
                .onFailure { Logbuch.fehler("Signal Programmieren", it) }
            return
        }
        if (art != "tagebuch") return
        bereich.launch {
            val beginn = System.currentTimeMillis()
            // Nur die letzten Wochen holen (schnell, löscht nichts); der volle Abgleich bleibt dem regelmäßigen Lauf.
            runCatching { Register.alle(app).filterIsInstance<TagebuchFaehigkeit>().firstOrNull()?.synchronisiere(tage = 40) }
            val neu = runCatching { Lagebild.auffrischen(app, listOf(Bereich.TAGEBUCH), "neuer Tagebucheintrag") }.getOrDefault(false)
            Logbuch.gedaechtnis("Signal Tagebuch", if (neu) "Gedächtnis aktualisiert" else "ohne inhaltliche Änderung", System.currentTimeMillis() - beginn)
        }
    }

    private suspend fun laufe(app: Context) {
        var tag = LocalDate.now()
        var letzteAnmeldung = System.currentTimeMillis()
        var letztesLernen = 0L
        // Kurz warten: Beim Start des Dienstes haben Verbindung und Auswertung Vorrang.
        delay(4_000)
        while (bereich.isActive) {
            val jetzt = System.currentTimeMillis()
            if (jetzt - letzteAnmeldung > 10 * 60_000L) { registriere(app); letzteAnmeldung = jetzt }
            val neuerTag = LocalDate.now() != tag
            val abschnitte = Lagebild.stand.value.abschnitte
            val faellig = if (neuerTag) Bereich.entries.toList() else Bereich.entries.filter { b -> abschnitte[b]?.let { jetzt - it.stand >= b.abstandMs } ?: true }
            if (faellig.isNotEmpty()) {
                val beginn = System.currentTimeMillis()
                runCatching { Lagebild.auffrischen(app, faellig, if (neuerTag) "neuer Tag" else "regelmäßig") }
                    .onFailure { Logbuch.fehler("Wächter: Auffrischen", it) }
                val ms = System.currentTimeMillis() - beginn
                if (ms > 5_000 || neuerTag) Logbuch.gedaechtnis("Wächter-Runde", faellig.joinToString(",") { it.kennung }, ms)
            }
            tag = LocalDate.now()
            if (jetzt - letztesLernen >= 10 * 60_000L && Gespraechsgedaechtnis.lernenFaellig(app)) {
                letztesLernen = jetzt
                runCatching { withTimeoutOrNull(150_000L) { Gespraechsgedaechtnis.lerne(app) } }.onFailure { Logbuch.fehler("Gesprächs-Lernen", it) }
            }
            // Ein fester Minutentakt: Die Runde selbst kostet nichts, wenn nichts fällig ist. Kürzer zu schlafen brächte nichts
            // (die Abstände sind lang) und ließe einen gestörten Bereich, dessen Stand stehen bleibt, im Sekundentakt neu lesen.
            delay(60_000L)
        }
    }
}
