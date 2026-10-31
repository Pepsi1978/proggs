package de.frank.jarvis.ablage

import android.content.Context
import android.os.PowerManager
import android.os.Process
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.speech.GespraechsFokus
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hält die Ablage dieses Handys mit dem Server gleich, von dem alle Jarvis-Versionen (Handy, Mac OS, später Windows) sie beziehen.
 * Das Handy behält seinen eigenen Speicher ([AblageSpeicher], [Prompts]): Werkzeuge, Agenten und die Oberfläche arbeiten unverändert darauf.
 *
 * Ablauf eines Laufs: erst holen, dann senden. [Zustand.bekannt] merkt sich je Dokument den zuletzt abgeglichenen Inhalt:
 * - Nur der Server hat sich geändert: Seine Fassung gilt.
 * - Nur hier hat sich etwas geändert: Es wird gesendet, die Uhrzeiten spielen keine Rolle.
 * - Beide: Nichts geht verloren. Die Dateien werden vereint, die jüngere Textfassung gilt, die ältere kommt als eigener Eintrag dazu.
 * Gelöscht wird nur auf ausdrückliche Meldung, nie weil etwas auf einer Seite fehlt: Ein noch nicht gesendeter Eintrag darf nicht verschwinden,
 * und eine hier abhanden gekommene Datei ([AblageSpeicher.verloren]) geht nicht als Löschung an die anderen, sondern kommt vom Server zurück.
 *
 * Zwei Spuren, damit Kleines nie auf Großes wartet: Texte, Prompts und kleine Dateien laufen sofort im schnellen Lauf. Einträge mit großen Dateien
 * gehen in eine eigene Warteschlange mit niedriger Priorität; solange sie dort liegen, zeigt [status] das in der Ablage an.
 *
 * Angestoßen wird über das Signal des Servers, jede fremde Änderung hier (kurz gesammelt), das Verbinden, einen Netzwechsel und das Öffnen der App.
 * Alle 15 Minuten zur Sicherheit; der Timer steht im Tiefschlaf, das Öffnen der App holt das nach.
 * Beides steht still, solange ein Gespräch läuft, auch mitten in einer Übertragung (Sprachmodus vor Hintergrundarbeit);
 * Downloads setzen danach an derselben Stelle fort. Über das Datenvolumen wacht er bewusst nicht: Frank hat unbegrenzte mobile Daten.
 */
object AblageSync {
    private const val ZYKLUS_MS = 15 * 60_000L
    private const val NACH_FEHLER_MS = 60_000L
    /** Hängt der Gesprächszustand (nie freigegebener Fokus), darf das den Abgleich nicht für immer anhalten. */
    private const val GESPRAECH_HOECHSTENS_MS = 20 * 60_000L
    /** So oft versucht ein Lauf ein Dokument, das nicht zu holen ist; danach hält es die übrigen nicht mehr auf (bis zum nächsten Prozessstart). */
    private const val VERSUCHE = 5
    /** Ab dieser Datenmenge geht ein Eintrag in die Warteschlange für große Dateien. */
    private const val GROSS_BYTES = 2L * 1024 * 1024
    private const val DATEI = "ablage-sync.json"
    private val json = "application/json".toMediaType()
    private val roh = "application/octet-stream".toMediaType()

    private val anstoss = Channel<Unit>(Channel.CONFLATED)
    private val faden = Executors.newSingleThreadExecutor { r -> Thread(r, "AblageSync") }.asCoroutineDispatcher()
    private val dateiFaden = Executors.newSingleThreadExecutor { r -> Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); r.run() }, "AblageSyncDateien") }.asCoroutineDispatcher()
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** Dokumente: kurze Zeitlimits, damit eine tote Verbindung den Abgleich nicht minutenlang festhält. */
    private val schnell = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()
    private val dateiClient = schnell.newBuilder().readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
    @Volatile private var gestartet = false
    /** Alles vergleichen statt nur das Neue. Das holt nach, was hier verloren oder liegen blieb. */
    @Volatile private var vollabgleich = false
    /** Nach dem Prozessstart zuerst das Neue (schnell), direkt danach einmal alles. */
    private var vollNachStart = true
    /** Was der Server endgültig ablehnt (zu groß, ungültig): Bis zur nächsten Änderung oder zum Neustart nicht wieder senden, sonst liefe es jede Minute neu. */
    private val aufgegeben: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val fehlversuche = ConcurrentHashMap<String, Int>()
    /** Prüfsummen der Texte, solange Größe und Datum gleich bleiben (überleben den Neustart): Jeder Lauf bräuchte sie sonst für alle Einträge neu. */
    private val textSummen = ConcurrentHashMap<String, String>()
    private var gespraechSeit = 0L
    private var echterFehler = false
    private var zustand: Zustand? = null

    /** Eigene Schreibvorgänge wecken den Abgleich nicht noch einmal: ohne das folgte auf jede übernommene Änderung ein leerer zweiter Lauf. */
    private val eigenerStand = AtomicLong(-1)
    private val eigenerPromptStand = AtomicLong(-1)

    // ---- große Dateien ----
    private class DateiArbeit(val marke: String, val titel: String, val groesse: Long, val arbeit: suspend () -> Unit)
    private val warteschlange = Channel<DateiArbeit>(Channel.UNLIMITED)
    private val inArbeit: MutableSet<String> = ConcurrentHashMap.newKeySet()
    /** Der neueste Stand eines Eintrags, dessen große Dateien noch laden; kommt währenddessen ein neuerer, gilt der. */
    private val offeneDocs = ConcurrentHashMap<String, JSONObject>()
    private val geloeschtWaehrendLaden: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** Was die Ablage über den Abgleich anzeigt. */
    data class Status(val hinweis: String? = null, val laeuft: Boolean = false, val zuletzt: Long = 0L, val dateien: List<Pair<String, Long>> = emptyList(), val geladen: Long = 0L)
    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    /** Der Server hat das endgültig abgelehnt; ein neuer Versuch ändert daran nichts. */
    private class DauerFehler(meldung: String) : IOException(meldung)
    /** Kein Fehler: später noch einmal (Gespräch läuft, hier hat sich gerade etwas geändert). */
    private class Spaeter(meldung: String) : IOException(meldung)

    fun anstossen() { anstoss.trySend(Unit) }

    /** Das Netz hat gewechselt: Verbindungen darauf sind tot. Nicht auf ihre Zeitlimits warten. */
    fun netzGewechselt() {
        listOf(schnell, dateiClient).forEach { it.dispatcher.cancelAll(); it.connectionPool.evictAll() }
        anstossen()
    }

    @Synchronized
    fun starte(context: Context) {
        if (gestartet) return
        gestartet = true
        val app = context.applicationContext
        bereich.launch(faden) {
            var warten = 0L
            var fehlerFolge = 0
            while (true) {
                withTimeoutOrNull(warten) { anstoss.receive() }
                _status.update { it.copy(laeuft = true) }
                warten = try {
                    wach(app, "jarvis:ablage", 10 * 60_000L) { lauf(app) }
                    fehlerFolge = 0
                    _status.update { it.copy(laeuft = false, zuletzt = System.currentTimeMillis(), hinweis = hinweisText(null)) }
                    ZYKLUS_MS
                } catch (e: Spaeter) {
                    // Kein Fehler: gleich weiter, sobald das Gespräch vorbei ist (der Lauf wartet selbst darauf).
                    _status.update { it.copy(laeuft = false) }
                    5_000L
                } catch (e: Exception) {
                    Logbuch.netz("Ablage-Abgleich", "unterbrochen: ${e.message}")
                    fehlerFolge++
                    _status.update { it.copy(laeuft = false, hinweis = hinweisText(if (fehlerFolge >= 3) "Die Ablage gleicht sich gerade nicht ab, Jarvis versucht es weiter." else null)) }
                    (NACH_FEHLER_MS shl (fehlerFolge - 1).coerceAtMost(4)).coerceAtMost(ZYKLUS_MS)
                }
            }
        }
        bereich.launch(dateiFaden) { dateienAbarbeiten(app) }
        // Kurz gesammelt, aber ohne dass laufende Änderungen das Senden immer weiter hinausschieben. Eine Recherche legt oft Text und mehrere Dateien nacheinander ab.
        val speicher = AblageZentrale.speicher(app)
        bereich.launch { speicher.stand.drop(1).collect { v -> delay(400); if (v != eigenerStand.get()) anstossen() } }
        bereich.launch { Prompts.stand.drop(1).collect { v -> delay(400); if (v != eigenerPromptStand.get()) anstossen() } }
    }

    private fun hinweisText(netz: String?): String? {
        val offen = aufgegeben.size + fehlversuche.values.count { it >= VERSUCHE }
        return netz ?: if (offen > 0) "$offen Einträge lassen sich nicht abgleichen, Details unter Aktivität." else null
    }

    /** Hält das Handy wach, solange der Abgleich arbeitet: Sonst friert er im Tiefschlaf mitten im Download ein. */
    private suspend fun <T> wach(app: Context, name: String, hoechstens: Long, arbeit: suspend () -> T): T {
        val sperre = app.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, name)
        sperre.acquire(hoechstens)
        try { return arbeit() } finally { if (sperre.isHeld) sperre.release() }
    }

    private fun <T> eigen(speicher: AblageSpeicher, arbeit: () -> T): T {
        val vorher = speicher.stand.value
        val r = arbeit()
        if (speicher.stand.value == vorher + 1) eigenerStand.set(vorher + 1)
        return r
    }

    private fun <T> eigenPrompt(arbeit: () -> T): T {
        val vorher = Prompts.stand.value
        val r = arbeit()
        if (Prompts.stand.value == vorher + 1) eigenerPromptStand.set(vorher + 1)
        return r
    }

    /** Läuft ein Gespräch? Nach der Höchstdauer gilt der Zustand als hängend und hält den Abgleich nicht länger an. */
    @Synchronized
    private fun gespraechLaeuft(): Boolean {
        if (!GespraechsFokus.stilleHinweise) { gespraechSeit = 0; return false }
        val jetzt = System.currentTimeMillis()
        if (gespraechSeit == 0L) gespraechSeit = jetzt
        if (jetzt - gespraechSeit < GESPRAECH_HOECHSTENS_MS) return true
        Logbuch.netz("Ablage-Abgleich", "Gesprächszustand seit über 20 Minuten, gleiche trotzdem ab")
        return false
    }

    /** Solange ein Gespräch läuft, geht nichts über die Leitung. */
    private suspend fun gespraechAbwarten() { while (gespraechLaeuft()) delay(2_000) }

    /** Mitten in einer Übertragung: abbrechen, der nächste Lauf macht nach dem Gespräch weiter. */
    private fun gespraechPruefen() { if (gespraechLaeuft()) throw Spaeter("Übertragung pausiert: Gespräch läuft") }

    private class Zustand(val datei: File) {
        @Volatile var stand = 0L
        @Volatile var instanz = ""
        val bekannt = ConcurrentHashMap<String, String>()
        /** Zeit der zuletzt abgeglichenen Fassung je Dokument. */
        val zeiten = ConcurrentHashMap<String, Long>()
        private var zuletzt = ""
        init {
            runCatching {
                val j = JSONObject(datei.readText())
                stand = j.optLong("stand")
                instanz = j.optString("instanz")
                j.optJSONObject("bekannt")?.let { b -> b.keys().forEach { bekannt[it] = b.optString(it) } }
                j.optJSONObject("zeiten")?.let { b -> b.keys().forEach { zeiten[it] = b.optLong(it) } }
                j.optJSONObject("summen")?.let { b -> b.keys().forEach { textSummen[it] = b.optString(it) } }
            }
        }
        /** Geschrieben wird nur, was sich geändert hat. */
        @Synchronized fun sichere() {
            val text = JSONObject().put("stand", stand).put("instanz", instanz).put("bekannt", JSONObject(bekannt as Map<*, *>)).put("zeiten", JSONObject(zeiten as Map<*, *>)).put("summen", JSONObject(textSummen as Map<*, *>)).toString()
            if (text == zuletzt) return
            AblageSpeicher.schreibeAtomar(datei, text.toByteArray())
            zuletzt = text
        }
        /** Geht diese Uhr nach, gälte eine Änderung sonst als älter als der Stand, auf dem sie aufbaut. */
        fun zeitFuer(schluessel: String, geaendert: Long) = maxOf(geaendert, (zeiten[schluessel] ?: 0L) + 1).coerceAtLeast(1)
        fun merke(schluessel: String, abdruck: String, zeit: Long) { bekannt[schluessel] = abdruck; zeiten[schluessel] = zeit }
        fun vergiss(schluessel: String) { bekannt.remove(schluessel); zeiten.remove(schluessel) }
    }

    /** Was hier gerade liegt; neu gelesen nur, wenn sich seit dem letzten Lesen etwas geändert hat. */
    private class Bestand(val app: Context, val speicher: AblageSpeicher) {
        var eintraege: Map<String, Eintrag> = emptyMap()
        var offen: Map<String, Long> = emptyMap()
        var verloren: Set<String> = emptySet()
        var prompts: Map<Long, Prompt> = emptyMap()
        var promptsOffen: Map<Long, Long> = emptyMap()
        private var marke = -1L
        private var promptMarke = -1L
        fun aktuell() {
            if (marke != speicher.stand.value) {
                marke = speicher.stand.value
                eintraege = speicher.eintraege().associateBy { it.id }
                offen = speicher.offeneLoeschungen()
                verloren = speicher.verloren()
            }
            if (promptMarke != Prompts.stand.value) {
                promptMarke = Prompts.stand.value
                prompts = Prompts.alle(app).associateBy { it.erstellt }
                promptsOffen = Prompts.offeneLoeschungen(app)
            }
        }
    }

    private suspend fun lauf(app: Context) {
        gespraechAbwarten()
        val e = Einstellungen.get(app)
        if (e.serverHost.isBlank() || e.geheimnis.isBlank()) return
        val basis = "https://${e.serverHost}/j/${e.geheimnis}/ablage"
        val speicher = AblageZentrale.speicher(app)
        val z = zustand ?: Zustand(File(app.filesDir, DATEI)).also { zustand = it }
        echterFehler = false
        // Etwas kam hier abhanden: Der Stand-Zeiger steht längst dahinter, also alles vergleichen.
        if (speicher.verloren().isNotEmpty()) vollabgleich = true
        val alleGeholt = holen(app, basis, speicher, z)
        if (alleGeholt) vollabgleich = false
        val allesGesendet = senden(app, basis, speicher, z)
        z.sichere()
        if (!alleGeholt || !allesGesendet) { if (echterFehler) throw IOException("Noch nicht alles abgeglichen"); throw Spaeter("Später weiter") }
        if (vollNachStart) { vollNachStart = false; vollabgleich = true; anstossen() }
    }

    // ---- Holen ----

    private suspend fun holen(app: Context, basis: String, speicher: AblageSpeicher, z: Zustand): Boolean {
        val bestand = Bestand(app, speicher)
        var seit = if (vollabgleich) 0L else z.stand
        val ganz = seit == 0L
        var alleGut = true
        val lebend = HashSet<String>()
        while (true) {
            val antwort = schnell.newCall(Request.Builder().url("$basis/aenderungen?seit=$seit").build()).execute().use { r ->
                if (!r.isSuccessful) throw IOException("Server antwortet mit ${r.code}")
                JSONObject(r.body?.string().orEmpty())
            }
            // Der Server wurde neu aufgesetzt oder aus einem Backup geholt: Der ganze Bestand muss wieder hoch.
            val instanz = antwort.optString("instanz")
            val neuerSpeicher = z.instanz.isNotEmpty() && instanz.isNotEmpty() && instanz != z.instanz
            if (instanz.isNotEmpty()) z.instanz = instanz
            if (neuerSpeicher || antwort.optBoolean("neuaufbau")) {
                Logbuch.netz("Ablage-Abgleich", "Server wurde neu aufgesetzt, sende den ganzen Bestand")
                z.bekannt.clear()
                z.zeiten.clear()
                z.stand = 0
                if (seit != 0L) { seit = 0; continue }
            }
            val neu = antwort.optLong("stand")
            val docs = antwort.getJSONArray("docs")
            for (i in 0 until docs.length()) {
                gespraechAbwarten()
                val d = docs.getJSONObject(i)
                val marke = d.optString("art") + ":" + d.optString("id")
                if (d.optString("art") == "eintrag" && !d.optBoolean("geloescht")) lebend += d.optString("id")
                try {
                    bestand.aktuell()
                    docUebernehmen(app, basis, speicher, z, d, bestand, imHintergrund = false)
                    fehlversuche.remove(marke)
                } catch (f: Spaeter) {
                    alleGut = false
                } catch (f: Exception) {
                    // Das Dokument bleibt offen und wird wiederholt. Lässt es sich auch dann nicht holen, hält es den Rest nicht länger auf.
                    val versuche = (fehlversuche[marke] ?: 0) + 1
                    fehlversuche[marke] = versuche
                    if (versuche < VERSUCHE) { alleGut = false; echterFehler = true }
                    else if (versuche == VERSUCHE) Logbuch.fehler("Ablage-Abgleich: $marke übersprungen", f)
                    Logbuch.netz("Ablage-Abgleich", "$marke: ${f.message}")
                }
            }
            if (alleGut) z.stand = neu
            if (!antwort.optBoolean("mehr")) break
            seit = neu
        }
        // Alles gesehen: Was als verloren gilt, der Server aber nicht (mehr) hat, kann nicht zurückkommen und erzwingt sonst bei jedem Lauf einen Vollabgleich.
        if (ganz && alleGut) speicher.verloren().filter { it !in lebend }.forEach { speicher.verlorenErledigt(it) }
        return alleGut
    }

    private fun docUebernehmen(app: Context, basis: String, speicher: AblageSpeicher, z: Zustand, d: JSONObject, b: Bestand, imHintergrund: Boolean) {
        when (d.optString("art")) {
            "eintrag" -> eintragHolen(app, basis, speicher, z, d, b, imHintergrund)
            "prompt" -> promptHolen(app, z, d, b)
            "ordnung" -> ordnungHolen(app, z, d)
        }
    }

    private fun eintragHolen(app: Context, basis: String, speicher: AblageSpeicher, z: Zustand, d: JSONObject, b: Bestand, imHintergrund: Boolean) {
        val id = sicher(d.getString("id"))
        val schluessel = "eintrag:$id"
        val geaendert = d.optLong("geaendert")
        val offen = b.offen[id]
        val lokal = b.eintraege[id]
        val lokalText = lokal?.let { textSha(speicher, it) }
        val lokalAbdruck = lokal?.let { abdruck(it, lokalText) }
        val bekannt = z.bekannt[schluessel]
        val istVerloren = id in b.verloren
        val hierUnveraendert = lokalAbdruck != null && (bekannt == lokalAbdruck || istVerloren)
        if (d.optBoolean("geloescht")) {
            offeneDocs.remove(id)
            if (id in inArbeit || "hoch:$id" in inArbeit) geloeschtWaehrendLaden += id
            if (offen != null) { speicher.quittiereLoeschung(id); z.vergiss(schluessel); return }
            if (lokal == null) { z.vergiss(schluessel); return }
            // Hier etwas Neues trotz Löschung am anderen Gerät: Es bleibt und geht wieder hoch, verloren geht nichts.
            if (hierUnveraendert || bekannt == null) {
                eigen(speicher) { speicher.loescheEintrag(id, melden = false) }
                z.vergiss(schluessel)
            } else z.zeiten[schluessel] = geaendert
            return
        }
        val w = d.getJSONObject("daten")
        val fern = eintragAus(w, id, geaendert)
        // Auf Android liefert optString bei JSON-null den Text „null“.
        val textFern = if (w.isNull("text_sha")) null else w.optString("text_sha").takeIf { it.isNotBlank() }
        val fernAbdruck = abdruck(fern, textFern)
        // Nur das Echo des schon Bekannten (auch bei nachgehender Uhr): Eine Löschung oder Änderung von hier gilt weiter.
        if (offen != null) { if (bekannt == fernAbdruck || offen >= geaendert) return; speicher.quittiereLoeschung(id) }
        if (lokalAbdruck == fernAbdruck) { z.merke(schluessel, fernAbdruck, geaendert); return }
        if (lokal != null && !istVerloren && bekannt == fernAbdruck) return

        // Große Dateien halten den schnellen Lauf nicht auf: Der Zeiger läuft weiter, die Warteschlange holt sie. Geht der Prozess vorher unter, findet der Vollabgleich sie wieder.
        val fehlendeBytes = fern.anhaenge.filter { a -> lokal?.anhaenge?.none { it.id == a.id && it.sha256 == a.sha256 } != false }.sumOf { it.groesse } +
            (if (textFern != null && textFern != lokalText) w.optLong("text_groesse") else 0L)
        if (!imHintergrund && fehlendeBytes >= GROSS_BYTES) {
            geloeschtWaehrendLaden -= id
            offeneDocs[id] = d
            einreihen(id, fern.titel, fehlendeBytes) {
                while (true) {
                    val neuester = offeneDocs.remove(id) ?: break
                    val frisch = Bestand(app, speicher).apply { aktuell() }
                    eintragHolen(app, basis, speicher, z, neuester, frisch, imHintergrund = true)
                }
                z.sichere()
            }
            return
        }

        if (lokal != null && !hierUnveraendert) { konfliktLoesen(basis, speicher, z, schluessel, fern, textFern, lokal, lokalText); return }

        // Ging das Verzeichnis hier verloren, trägt derselbe Text jetzt eine neue Kennung: Sie darf nicht als Doppel an alle gehen.
        val doppelt = if (lokal == null && textFern != null) b.eintraege.values.firstOrNull { it.titel == fern.titel && it.anhaenge.isEmpty() && z.bekannt["eintrag:${it.id}"] == null && textSha(speicher, it) == textFern } else null
        val textTeil = if (textFern != null && textFern != lokalText) laden(basis, textFern, speicher, w.optLong("text_groesse")) else null
        val dateien = HashMap<String, File>()
        fern.anhaenge.filter { a -> lokal?.anhaenge?.none { it.id == a.id && it.sha256 == a.sha256 } != false }.forEach { a -> dateien[a.id] = laden(basis, a.sha256, speicher, a.groesse) }
        // Während des Ladens anderswo gelöscht: nicht wieder anlegen.
        if (geloeschtWaehrendLaden.remove(id)) return
        // Erst wenn alles da ist: Scheitert ein Download, ist die lokale Kopie nicht schon weg.
        doppelt?.let { x -> eigen(speicher) { speicher.loescheEintrag(x.id, melden = false) } }
        if (!eigen(speicher) { speicher.setzeFern(fern, textFern != null, textTeil, dateien, lokal?.geaendert) }) throw Spaeter("$schluessel hat sich hier gerade geändert")
        z.merke(schluessel, fernAbdruck, geaendert)
    }

    /** Hier und auf dem Server geändert: Die Dateien beider Seiten bleiben, die jüngere Textfassung gilt, die ältere kommt als eigener Eintrag dazu. Das Ergebnis geht wieder an alle. */
    private fun konfliktLoesen(basis: String, speicher: AblageSpeicher, z: Zustand, schluessel: String, fern: Eintrag, textFern: String?, lokal: Eintrag, lokalText: String?) {
        Logbuch.netz("Ablage-Abgleich", "Konflikt bei „${lokal.titel}“: beide Seiten geändert, vereine")
        val fernNeuer = fern.geaendert >= lokal.geaendert
        val nurHier = lokal.anhaenge.filter { x -> fern.anhaenge.none { it.id == x.id } }
        val dateien = HashMap<String, File>()
        fern.anhaenge.filter { a -> lokal.anhaenge.none { it.id == a.id && it.sha256 == a.sha256 } }.forEach { a -> dateien[a.id] = laden(basis, a.sha256, speicher, a.groesse) }
        val textTeil = if (fernNeuer && textFern != null && textFern != lokalText) laden(basis, textFern, speicher, 0) else null
        if (textFern != null && lokalText != null && textFern != lokalText) {
            val aelter = if (fernNeuer) speicher.text(lokal) else laden(basis, textFern, speicher, 0).let { f -> f.readText().also { f.delete() } }
            // Der Titel hängt an der älteren Fassung: Eine Wiederholung ergibt keinen zweiten Eintrag, eine frühere ältere Fassung bleibt.
            val wann = java.text.SimpleDateFormat("dd.MM.yyyy HH.mm", Locale.GERMAN).format(java.util.Date(minOf(fern.geaendert, lokal.geaendert)))
            speicher.schreibeText("${lokal.titel} (ältere Fassung $wann)", aelter)
        }
        val basisEintrag = if (fernNeuer) fern else lokal.copy(anhaenge = emptyList())
        val vereint = basisEintrag.copy(anhaenge = fern.anhaenge + nurHier, geaendert = maxOf(fern.geaendert, lokal.geaendert))
        val mitText = lokalText != null || textFern != null
        // Hatte nur eine Seite Text, bleibt er; die Fassung von hier liegt schon an ihrem Platz.
        val fernOhneLokal = !fernNeuer && lokalText == null && textFern != null
        val nurFernText = if (fernOhneLokal) laden(basis, textFern!!, speicher, 0) else textTeil
        if (!speicher.setzeFern(vereint, mitText, nurFernText, dateien, lokal.geaendert)) throw Spaeter("$schluessel hat sich hier gerade geändert")
        // Nicht als abgeglichen merken: Das Vereinte muss noch an alle, und seine Zeit muss die vom Server schlagen.
        z.bekannt.remove(schluessel)
        z.zeiten[schluessel] = fern.geaendert
    }

    private fun promptHolen(app: Context, z: Zustand, d: JSONObject, b: Bestand) {
        val erstellt = d.getString("id").toLong()
        val schluessel = "prompt:$erstellt"
        val geaendert = d.optLong("geaendert")
        val lokal = b.prompts[erstellt]
        val offen = b.promptsOffen[erstellt]
        val bekannt = z.bekannt[schluessel]
        val hierUnveraendert = lokal != null && bekannt == abdruck(lokal)
        if (d.optBoolean("geloescht")) {
            if (offen != null) { eigenPrompt { Prompts.quittiereLoeschung(app, erstellt) }; z.vergiss(schluessel); return }
            if (lokal == null) { z.vergiss(schluessel); return }
            if (hierUnveraendert || bekannt == null) { eigenPrompt { Prompts.loescheFern(app, erstellt) }; z.vergiss(schluessel) } else z.zeiten[schluessel] = geaendert
            return
        }
        val w = d.getJSONObject("daten")
        val fern = Prompt(lokal?.nummer ?: 1, w.optString("text"), erstellt, w.optString("titel"), w.optString("quelle"), geaendert)
        if (fern.text.isEmpty()) return
        // Nur das Echo des schon Bekannten (auch bei nachgehender Uhr): Eine Löschung oder Änderung von hier gilt weiter.
        if (offen != null) { if (bekannt == abdruck(fern) || offen >= geaendert) return; eigenPrompt { Prompts.quittiereLoeschung(app, erstellt) } }
        if (lokal != null && abdruck(lokal) == abdruck(fern)) { z.merke(schluessel, abdruck(fern), geaendert); return }
        if (lokal != null && bekannt == abdruck(fern)) return
        // Beide geändert: Die jüngere Fassung gilt, die ältere bleibt als eigener Prompt. Eine bloße KI-Überschrift hier ist kein Grund dafür.
        if (lokal != null && !hierUnveraendert && lokal.text != fern.text) {
            if (geaendert < lokal.geaendert) { Prompts.setzeFern(app, fern.copy(erstellt = lokal.erstellt + 1, titel = "", geaendert = geaendert)); return }
            Prompts.setzeFern(app, lokal.copy(erstellt = geaendert + 1, titel = "", nummer = 1))
        }
        if (eigenPrompt { Prompts.setzeFern(app, fern) }) z.merke(schluessel, abdruck(fern), geaendert) else throw IOException("Prompt-Datei nicht schreibbar")
    }

    private fun ordnungHolen(app: Context, z: Zustand, d: JSONObject) {
        val zeit = d.optLong("geaendert")
        val lokal = Prompts.ordnungZeit(app)
        if (zeit == lokal) { z.merke("ordnung:prompts", zeit.toString(), zeit); return }
        if (zeit > lokal) {
            val liste = d.getJSONObject("daten").optJSONArray("reihenfolge") ?: return
            if (eigenPrompt { Prompts.ordneFern(app, (0 until liste.length()).map { liste.optLong(it) }, zeit) }) z.merke("ordnung:prompts", zeit.toString(), zeit)
        }
    }

    /**
     * Lädt eine Datei in den Teil-Ordner und prüft sie; erst danach kommt sie in die Ablage. Der Teil-Name kommt aus der Prüfsumme:
     * Ein abgebrochener Download (Netz, Gespräch) macht beim nächsten Lauf an derselben Stelle weiter statt von vorn.
     */
    private fun laden(basis: String, sha: String, speicher: AblageSpeicher, groesse: Long): File {
        val teil = File(speicher.teilOrdner, sicher(sha) + ".teil")
        val schon = if (teil.isFile) teil.length() else 0L
        // Geprüft wird nur eine vollständige Teildatei: Eine unfertige bei jeder Fortsetzung neu zu lesen, kostete bei großen Dateien Minuten.
        if (schon > 0 && (groesse == 0L || schon == groesse) && AblageSpeicher.sha256(teil) == sha) return teil
        val anfrage = Request.Builder().url("$basis/blob/$sha").apply { if (schon > 0) header("Range", "bytes=$schon-") }.build()
        dateiClient.newCall(anfrage).execute().use { r ->
            if (r.code == 416) { teil.delete(); throw IOException("Teildatei passte nicht, beginne neu") }
            if (!r.isSuccessful) throw IOException("Datei nicht abrufbar (${r.code})")
            FileOutputStream(teil, r.code == 206).use { aus ->
                val ein = r.body!!.byteStream()
                val puffer = ByteArray(64 * 1024)
                var gesamt = if (r.code == 206) schon else 0L
                var gemeldet = 0L
                while (true) {
                    gespraechPruefen()
                    val n = ein.read(puffer)
                    if (n < 0) break
                    aus.write(puffer, 0, n)
                    gesamt += n
                    if (gesamt - gemeldet > 1024 * 1024) { gemeldet = gesamt; _status.update { it.copy(geladen = gesamt) } }
                }
            }
        }
        if (AblageSpeicher.sha256(teil) != sha) { teil.delete(); throw IOException("Prüfsumme passt nicht") }
        return teil
    }

    // ---- große Dateien: eigene Warteschlange ----

    private fun einreihen(marke: String, titel: String, groesse: Long, arbeit: suspend () -> Unit) {
        if (!inArbeit.add(marke)) return
        _status.update { it.copy(dateien = it.dateien + (titel to groesse)) }
        warteschlange.trySend(DateiArbeit(marke, titel, groesse, arbeit))
    }

    private suspend fun dateienAbarbeiten(app: Context) {
        val versuche = HashMap<String, Int>()
        while (true) {
            val auftrag = warteschlange.receive()
            gespraechAbwarten()
            var nochmal = false
            try {
                wach(app, "jarvis:ablage-dateien", 30 * 60_000L) { auftrag.arbeit() }
                versuche.remove(auftrag.marke)
                fehlversuche.remove(auftrag.marke)
            } catch (f: Spaeter) {
                nochmal = true
            } catch (f: Exception) {
                val n = (versuche[auftrag.marke] ?: 0) + 1
                versuche[auftrag.marke] = n
                Logbuch.netz("Ablage-Abgleich", "${auftrag.titel}: ${f.message}")
                if (f is DauerFehler || n >= VERSUCHE) { fehlversuche[auftrag.marke] = VERSUCHE; versuche.remove(auftrag.marke) } else nochmal = true
            }
            if (nochmal) {
                // Hinten anstellen: Eine Datei, die gerade nicht will, hält die anderen nicht auf.
                delay(if (warteschlange.isEmpty) 10_000 else 1_000)
                warteschlange.trySend(auftrag)
            } else {
                inArbeit.remove(auftrag.marke)
                _status.update { s -> s.copy(dateien = s.dateien.toMutableList().apply { remove(auftrag.titel to auftrag.groesse) }, geladen = 0, hinweis = hinweisText(null)) }
                anstossen()
            }
        }
    }

    // ---- Senden ----

    /** Jedes Dokument für sich: Eines, das scheitert, hält die anderen nicht auf. false = etwas blieb offen, der Lauf wird wiederholt. */
    private suspend fun senden(app: Context, basis: String, speicher: AblageSpeicher, z: Zustand): Boolean {
        var allesGut = true
        fun versuch(was: String, marke: String, arbeit: () -> Unit) {
            if (marke in aufgegeben) return
            try {
                arbeit()
            } catch (f: DauerFehler) {
                aufgegeben += marke
                Logbuch.fehler("Ablage-Abgleich: $was abgelehnt, wird nicht wiederholt", f)
            } catch (f: Spaeter) {
                allesGut = false
            } catch (f: Exception) {
                allesGut = false
                echterFehler = true
                Logbuch.netz("Ablage-Abgleich", "$was: ${f.message}")
            }
        }
        /** Der Server hat eine jüngere Fassung und schickt sie gleich mit: Sie wird sofort zusammengeführt, das Ergebnis geht im nächsten Lauf hinaus. */
        fun abgelehnt(ergebnis: PutErgebnis) {
            val d = ergebnis.doc
            if (d == null) { vollabgleich = true; allesGut = false; return }
            docUebernehmen(app, basis, speicher, z, d, Bestand(app, speicher).apply { aktuell() }, imHintergrund = false)
            anstossen()
        }
        // Beim Vollabgleich ist die Lage neu (Geheimnis, Server, Zeitlimit eines Proxys): Abgelehntes bekommt noch eine Chance.
        if (vollabgleich) aufgegeben.clear()

        speicher.offeneLoeschungen().forEach { (id, zeit) ->
            gespraechAbwarten()
            val s = "eintrag:$id"
            versuch(s, "$s:weg") {
                val r = put(basis, "eintrag", id, JSONObject(), z.zeitFuer(s, zeit), geloescht = true)
                if (r.uebernommen) { speicher.quittiereLoeschung(id); z.vergiss(s) } else abgelehnt(r)
            }
        }
        Prompts.offeneLoeschungen(app).forEach { (id, zeit) ->
            gespraechAbwarten()
            val s = "prompt:$id"
            versuch(s, "$s:weg") {
                val r = put(basis, "prompt", id.toString(), JSONObject(), z.zeitFuer(s, zeit), geloescht = true)
                if (r.uebernommen) { eigenPrompt { Prompts.quittiereLoeschung(app, id) }; z.vergiss(s) } else abgelehnt(r)
            }
        }
        // Erst das Kleine: Prompts und ihre Reihenfolge warten nicht auf Einträge.
        val prompts = Prompts.alle(app)
        prompts.forEach { p ->
            val s = "prompt:${p.erstellt}"
            val a = abdruck(p)
            if (z.bekannt[s] == a) return@forEach
            gespraechAbwarten()
            versuch(s, "$s:$a") {
                val daten = JSONObject().put("text", p.text).put("titel", p.titel).put("erstellt", p.erstellt).put("quelle", p.quelleName)
                val t = z.zeitFuer(s, p.geaendert)
                val r = put(basis, "prompt", p.erstellt.toString(), daten, t)
                if (r.uebernommen) z.merke(s, a, t) else abgelehnt(r)
            }
        }
        val zeit = Prompts.ordnungZeit(app).coerceAtLeast(1)
        if (z.bekannt["ordnung:prompts"] != zeit.toString() && prompts.isNotEmpty()) {
            gespraechAbwarten()
            versuch("Reihenfolge", "ordnung:$zeit") {
                val ids = Prompts.alle(app).map { it.erstellt }
                val r = put(basis, "ordnung", "prompts", JSONObject().put("reihenfolge", JSONArray(ids)), zeit)
                if (r.uebernommen) { z.merke("ordnung:prompts", zeit.toString(), zeit); eigenPrompt { Prompts.ordnungGesendet(app, ids) } } else abgelehnt(r)
            }
        }
        val verloren = speicher.verloren()
        speicher.eintraege().forEach { alt ->
            val s = "eintrag:${alt.id}"
            // Abhanden gekommen und auf dem Server bekannt: Das holt der Abgleich zurück, es ist keine Änderung für die anderen.
            if (alt.id in verloren && z.bekannt[s] != null) return@forEach
            val a = abdruck(alt, textSha(speicher, alt))
            if (z.bekannt[s] == a) return@forEach
            if ("hoch:${alt.id}" in inArbeit) return@forEach
            gespraechAbwarten()
            versuch(s, "$s:$a") {
                // Große Dateien gehen in die Warteschlange und halten die übrigen Einträge nicht auf.
                val groesse = alt.anhaenge.sumOf { it.groesse }
                if (groesse >= GROSS_BYTES) {
                    einreihen("hoch:${alt.id}", alt.titel, groesse) { eintragSenden(basis, speicher, z, alt.id)?.let { abgelehnt(it) } ; z.sichere() }
                } else eintragSenden(basis, speicher, z, alt.id)?.let { abgelehnt(it) }
            }
        }
        return allesGut
    }

    /** Sendet den aktuellen Stand eines Eintrags. Zurück kommt die Ablehnung des Servers, wenn er eine jüngere Fassung hat. */
    private fun eintragSenden(basis: String, speicher: AblageSpeicher, z: Zustand, id: String): PutErgebnis? {
        val s = "eintrag:$id"
        // Frisch gelesen: Während eines langen Uploads oder Gesprächs kann der Eintrag sich geändert haben oder gelöscht sein.
        val e = speicher.eintrag(id) ?: return null
        val sha = textSha(speicher, e)
        val a = abdruck(e, sha)
        if (z.bekannt[s] == a) return null
        val inhalte = HashMap<String, File>()
        if (sha != null) speicher.textDatei(e)?.let { inhalte[sha] = it }
        val mitSha = e.anhaenge.map { x -> if (x.sha256.isBlank()) x.copy(sha256 = AblageSpeicher.sha256(speicher.datei(x))) else x }
        mitSha.forEach { x -> inhalte[x.sha256] = speicher.datei(x) }
        val t = z.zeitFuer(s, e.geaendert)
        val r = put(basis, "eintrag", e.id, eintragAlsDaten(speicher, e.copy(anhaenge = mitSha), sha), t, inhalte = inhalte)
        if (!r.uebernommen) return r
        z.merke(s, a, t)
        speicher.verlorenErledigt(e.id)
        return null
    }

    private class PutErgebnis(val uebernommen: Boolean, val doc: JSONObject?)

    private fun putErgebnis(rumpf: String?): PutErgebnis = JSONObject(rumpf.orEmpty()).let { PutErgebnis(it.optBoolean("uebernommen"), it.optJSONObject("doc")) }

    /** Sendet ein Dokument; fehlen dem Server dazu Dateien, kommen sie zuerst hoch. */
    private fun put(basis: String, art: String, id: String, daten: JSONObject, geaendert: Long, geloescht: Boolean = false, inhalte: Map<String, File> = emptyMap()): PutErgebnis {
        val rumpf = JSONObject().put("daten", daten).put("geaendert", geaendert).put("geloescht", geloescht).put("von", AblageQuellen.HANDY).toString()
        fun versuch() = schnell.newCall(Request.Builder().url("$basis/doc/$art/$id").put(rumpf.toRequestBody(json)).build()).execute()
        // Nur was der Inhalt selbst verschuldet, ist endgültig. 404 kann ein laufendes Ausrollen sein, 408 und 429 der Proxy.
        fun ablehnung(code: Int): IOException = if (code == 400 || code == 413) DauerFehler("Server antwortet mit $code") else IOException("Server antwortet mit $code")
        versuch().use { r ->
            if (r.isSuccessful) return putErgebnis(r.body?.string())
            if (r.code != 409) throw ablehnung(r.code)
            val fehlend = JSONObject(r.body?.string().orEmpty()).getJSONArray("fehlend")
            for (i in 0 until fehlend.length()) {
                val sha = fehlend.getString(i)
                val datei = inhalte[sha] ?: throw IOException("Datei zu $sha fehlt hier")
                dateiClient.newCall(Request.Builder().url("$basis/blob/$sha").put(GespraechsBewusst(datei, roh)).build()).execute().use { h -> if (!h.isSuccessful) throw ablehnung(h.code) }
            }
        }
        versuch().use { r -> if (!r.isSuccessful) throw ablehnung(r.code); return putErgebnis(r.body?.string()) }
    }

    /** Sendet eine Datei und bricht ab, sobald ein Gespräch beginnt. */
    private class GespraechsBewusst(private val datei: File, private val typ: MediaType) : RequestBody() {
        override fun contentType() = typ
        override fun contentLength() = datei.length()
        override fun writeTo(sink: BufferedSink) {
            datei.inputStream().use { ein ->
                val puffer = ByteArray(64 * 1024)
                while (true) {
                    gespraechPruefen()
                    val n = ein.read(puffer)
                    if (n < 0) break
                    sink.write(puffer, 0, n)
                }
            }
        }
    }

    // ---- Daten ----

    /** Kennungen vom Server landen in Dateipfaden. */
    private fun sicher(wert: String): String { if (!Regex("[A-Za-z0-9_-]{1,64}").matches(wert)) throw IOException("Ungültige Kennung"); return wert }

    private fun textSha(speicher: AblageSpeicher, e: Eintrag): String? {
        val f = speicher.textDatei(e) ?: return null
        val marke = "${f.length()}|${f.lastModified()}|"
        textSummen[f.name]?.takeIf { it.startsWith(marke) }?.let { return it.removePrefix(marke) }
        return AblageSpeicher.sha256(f).also { textSummen[f.name] = marke + it }
    }

    /** Alles, was zu allen Versionen muss; ohne Änderungszeit und ohne das, was jede Version selbst berechnet. */
    private fun abdruck(e: Eintrag, textSha: String?): String = kurz(listOf(e.titel, textSha.orEmpty(), e.erstellt, e.herkunft, e.quelleName, e.beschreibung).joinToString("\u0001") + "\u0002" +
        e.anhaenge.sortedBy { it.id }.joinToString("\u0002") { listOf(it.id, it.titel, it.originalName, it.endung, it.mime, it.groesse, it.erstellt, it.herkunft, it.sha256, it.beschreibung, it.kennung.orEmpty()).joinToString("\u0001") })

    /** Der Abdruck liegt in der Zustandsdatei: als Prüfsumme statt samt ganzem Prompt-Text. */
    private fun kurz(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }.take(32)

    private fun abdruck(p: Prompt): String = kurz(listOf(p.text, p.titel, p.quelleName).joinToString("\u0001"))

    private fun eintragAlsDaten(speicher: AblageSpeicher, e: Eintrag, textSha: String?): JSONObject {
        val kopf = runCatching { speicher.text(e, 40) }.getOrDefault("")
        return JSONObject().put("id", e.id).put("titel", e.titel).put("text_sha", textSha ?: JSONObject.NULL).put("text_groesse", speicher.textDatei(e)?.length() ?: 0)
            .put("erstellt", e.erstellt).put("geaendert", e.geaendert).put("herkunft", e.herkunft).put("quelle", e.quelleName).put("beschreibung", e.beschreibung)
            .put("recherche", "recherche" in e.titel.lowercase(Locale.GERMAN) || kopf.startsWith("# Recherche"))
            .put("anhaenge", JSONArray(e.anhaenge.map { a ->
                JSONObject().put("id", a.id).put("titel", a.titel).put("name", a.originalName).put("endung", a.endung).put("mime", a.mime).put("groesse", a.groesse)
                    .put("erstellt", a.erstellt).put("herkunft", a.herkunft).put("sha256", a.sha256).put("beschreibung", a.beschreibung).put("kennung", a.kennung ?: JSONObject.NULL)
                    .put("art", a.art.name).put("kategorie", a.art.kategorie.name)
            }))
    }

    private fun eintragAus(w: JSONObject, id: String, geaendert: Long): Eintrag {
        val liste = w.optJSONArray("anhaenge") ?: JSONArray()
        return Eintrag(
            id = id, titel = w.optString("titel"), textDatei = null, erstellt = w.optLong("erstellt"), geaendert = geaendert, herkunft = w.optString("herkunft"),
            beschreibung = w.optString("beschreibung"), quelle = w.optString("quelle"),
            anhaenge = (0 until liste.length()).map { i ->
                val a = liste.getJSONObject(i)
                val endung = a.optString("endung").lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }.take(12).ifEmpty { "bin" }
                Anhang(
                    id = sicher(a.getString("id")), titel = a.optString("titel"), originalName = a.optString("name"), endung = endung, mime = a.optString("mime"),
                    groesse = a.optLong("groesse"), erstellt = a.optLong("erstellt"), herkunft = a.optString("herkunft"), speicher = "", sha256 = a.getString("sha256"),
                    beschreibung = a.optString("beschreibung"), kennung = a.optString("kennung").takeIf { !a.isNull("kennung") && it.isNotEmpty() },
                )
            },
        )
    }
}
