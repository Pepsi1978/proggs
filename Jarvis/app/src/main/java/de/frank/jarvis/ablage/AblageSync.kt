package de.frank.jarvis.ablage

import android.content.Context
import android.os.Process
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.speech.GespraechsFokus
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
 * Angestoßen wird über das Signal des Servers, jede eigene Änderung und das Verbinden; alle 15 Minuten zur Sicherheit.
 * Der Lauf hat niedrige Priorität und steht still, solange ein Gespräch läuft, auch mitten in einer Übertragung (Sprachmodus vor Hintergrundarbeit);
 * Downloads setzen danach an derselben Stelle fort. Über das Datenvolumen wacht er bewusst nicht: Frank hat unbegrenzte mobile Daten.
 */
object AblageSync {
    private const val ZYKLUS_MS = 15 * 60_000L
    private const val NACH_FEHLER_MS = 60_000L
    /** Hängt der Gesprächszustand (nie freigegebener Fokus), darf das den Abgleich nicht für immer anhalten. */
    private const val GESPRAECH_HOECHSTENS_MS = 20 * 60_000L
    /** So oft versucht ein Lauf ein Dokument, das nicht zu holen ist; danach hält es die übrigen nicht mehr auf (bis zum nächsten Prozessstart). */
    private const val VERSUCHE = 5
    private const val DATEI = "ablage-sync.json"
    private val json = "application/json".toMediaType()
    private val roh = "application/octet-stream".toMediaType()

    private val anstoss = Channel<Unit>(Channel.CONFLATED)
    private val faden = Executors.newSingleThreadExecutor { r -> Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); r.run() }, "AblageSync") }.asCoroutineDispatcher()
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(5, TimeUnit.MINUTES).writeTimeout(5, TimeUnit.MINUTES).build()
    @Volatile private var gestartet = false
    /** Alles vergleichen statt nur das Neue: einmal je Prozess und nach einem abgelehnten Senden. Das holt nach, was hier verloren oder liegen blieb. */
    private var vollabgleich = true
    /** Was der Server endgültig ablehnt (zu groß, ungültig): Bis zur nächsten Änderung oder zum Neustart nicht wieder senden, sonst liefe es jede Minute neu. */
    private val aufgegeben = HashSet<String>()
    private val fehlversuche = HashMap<String, Int>()
    /** Prüfsummen der Texte je Datei, solange Größe und Datum gleich bleiben: Jeder Lauf bräuchte sie sonst für alle Einträge neu. */
    private val textSummen = HashMap<String, Triple<Long, Long, String>>()
    private var gespraechSeit = 0L

    private val _hinweis = MutableStateFlow<String?>(null)
    /** Was Frank wissen muss, wenn der Abgleich nicht rundläuft; null = alles in Ordnung. */
    val hinweis: StateFlow<String?> = _hinweis.asStateFlow()

    /** Der Server hat das endgültig abgelehnt; ein neuer Versuch ändert daran nichts. */
    private class DauerFehler(meldung: String) : IOException(meldung)
    /** Kein Fehler: später noch einmal (Gespräch läuft, hier hat sich gerade etwas geändert). */
    private class Spaeter(meldung: String) : IOException(meldung)

    fun anstossen() { anstoss.trySend(Unit) }

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
                warten = try {
                    lauf(app)
                    fehlerFolge = 0
                    hinweisSetzen(null)
                    ZYKLUS_MS
                } catch (e: Exception) {
                    Logbuch.netz("Ablage-Abgleich", "unterbrochen: ${e.message}")
                    fehlerFolge++
                    hinweisSetzen(if (fehlerFolge >= 3) "Die Ablage gleicht sich gerade nicht ab, Jarvis versucht es weiter." else null)
                    (NACH_FEHLER_MS shl (fehlerFolge - 1).coerceAtMost(4)).coerceAtMost(ZYKLUS_MS)
                }
            }
        }
        // Eigene Änderungen kurz sammeln: Eine Recherche legt oft Text und mehrere Dateien nacheinander ab.
        bereich.launch { AblageZentrale.speicher(app).stand.collectLatest { delay(2_000); anstossen() } }
        bereich.launch { Prompts.stand.collectLatest { delay(2_000); anstossen() } }
    }

    private fun hinweisSetzen(netz: String?) {
        val offen = aufgegeben.size + fehlversuche.values.count { it >= VERSUCHE }
        _hinweis.value = netz ?: if (offen > 0) "$offen Einträge lassen sich nicht abgleichen, Details unter Aktivität." else null
    }

    /** Läuft ein Gespräch? Nach der Höchstdauer gilt der Zustand als hängend und hält den Abgleich nicht länger an. */
    private fun gespraechLaeuft(): Boolean {
        if (!GespraechsFokus.stilleHinweise) { gespraechSeit = 0; return false }
        val jetzt = System.currentTimeMillis()
        if (gespraechSeit == 0L) gespraechSeit = jetzt
        if (jetzt - gespraechSeit < GESPRAECH_HOECHSTENS_MS) return true
        Logbuch.netz("Ablage-Abgleich", "Gesprächszustand seit über 20 Minuten, gleiche trotzdem ab")
        return false
    }

    /** Solange ein Gespräch läuft, geht nichts über die Leitung. */
    private suspend fun gespraechAbwarten() { while (gespraechLaeuft()) delay(5_000) }

    /** Mitten in einer Übertragung: abbrechen, der nächste Lauf macht nach dem Gespräch weiter. */
    private fun gespraechPruefen() { if (gespraechLaeuft()) throw Spaeter("Übertragung pausiert: Gespräch läuft") }

    private class Zustand(val datei: File) {
        var stand = 0L
        var instanz = ""
        val bekannt = HashMap<String, String>()
        /** Zeit der zuletzt abgeglichenen Fassung je Dokument. */
        val zeiten = HashMap<String, Long>()
        init {
            runCatching {
                val j = JSONObject(datei.readText())
                stand = j.optLong("stand")
                instanz = j.optString("instanz")
                j.optJSONObject("bekannt")?.let { b -> b.keys().forEach { bekannt[it] = b.optString(it) } }
                j.optJSONObject("zeiten")?.let { b -> b.keys().forEach { zeiten[it] = b.optLong(it) } }
            }
        }
        fun sichere() = AblageSpeicher.schreibeAtomar(datei, JSONObject().put("stand", stand).put("instanz", instanz).put("bekannt", JSONObject(bekannt as Map<*, *>)).put("zeiten", JSONObject(zeiten as Map<*, *>)).toString().toByteArray())
        /** Geht diese Uhr nach, gälte eine Änderung sonst als älter als der Stand, auf dem sie aufbaut. */
        fun zeitFuer(schluessel: String, geaendert: Long) = maxOf(geaendert, (zeiten[schluessel] ?: 0L) + 1).coerceAtLeast(1)
        fun merke(schluessel: String, abdruck: String, zeit: Long) { bekannt[schluessel] = abdruck; zeiten[schluessel] = zeit }
        fun vergiss(schluessel: String) { bekannt.remove(schluessel); zeiten.remove(schluessel) }
    }

    /** Was hier gerade liegt; neu gelesen nur, wenn sich seit dem letzten Lesen etwas geändert hat. So bleibt jede Entscheidung aktuell, ohne je Dokument alles zu lesen. */
    private class Bestand(val app: Context, val speicher: AblageSpeicher) {
        var eintraege = HashMap<String, Eintrag>()
        var offen: Map<String, Long> = emptyMap()
        var verloren: Set<String> = emptySet()
        var prompts = HashMap<Long, Prompt>()
        var promptsOffen: Map<Long, Long> = emptyMap()
        private var marke = -1L
        private var promptMarke = -1L
        fun aktuell() {
            if (marke != speicher.stand.value) {
                marke = speicher.stand.value
                eintraege = HashMap(speicher.eintraege().associateBy { it.id })
                offen = speicher.offeneLoeschungen()
                verloren = speicher.verloren()
            }
            if (promptMarke != Prompts.stand.value) {
                promptMarke = Prompts.stand.value
                prompts = HashMap(Prompts.alle(app).associateBy { it.erstellt })
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
        val z = Zustand(File(app.filesDir, DATEI))
        // Etwas kam hier abhanden: Der Stand-Zeiger steht längst dahinter, also alles vergleichen.
        if (speicher.verloren().isNotEmpty()) vollabgleich = true
        val alleGeholt = holen(app, basis, speicher, z)
        if (alleGeholt) vollabgleich = false
        z.sichere()
        val allesGesendet = senden(app, basis, speicher, z)
        z.sichere()
        if (!alleGeholt || !allesGesendet) throw IOException("Noch nicht alles abgeglichen")
    }

    // ---- Holen ----

    private suspend fun holen(app: Context, basis: String, speicher: AblageSpeicher, z: Zustand): Boolean {
        val bestand = Bestand(app, speicher)
        var seit = if (vollabgleich) 0L else z.stand
        var alleGut = true
        var neu: Long
        while (true) {
            val antwort = http(Request.Builder().url("$basis/aenderungen?seit=$seit").build()).use { r ->
                if (!r.isSuccessful) throw IOException("Server antwortet mit ${r.code}")
                JSONObject(r.body?.string().orEmpty())
            }
            // Ein anderer Speicher als beim letzten Mal: Der Server wurde neu aufgesetzt oder aus einem Backup geholt. Dann muss der ganze Bestand wieder hoch.
            val instanz = antwort.optString("instanz")
            if (instanz.isNotEmpty() && instanz != z.instanz) {
                val gewechselt = z.instanz.isNotEmpty()
                z.instanz = instanz
                if (gewechselt) {
                    Logbuch.netz("Ablage-Abgleich", "Server wurde neu aufgesetzt, sende den ganzen Bestand")
                    z.bekannt.clear()
                    z.zeiten.clear()
                    z.stand = 0
                    if (seit != 0L) { seit = 0; continue }
                }
            }
            neu = antwort.optLong("stand")
            val docs = antwort.getJSONArray("docs")
            for (i in 0 until docs.length()) {
                gespraechAbwarten()
                val d = docs.getJSONObject(i)
                val marke = d.optString("art") + ":" + d.optString("id")
                try {
                    bestand.aktuell()
                    when (d.optString("art")) {
                        "eintrag" -> eintragHolen(basis, speicher, z, d, bestand)
                        "prompt" -> promptHolen(app, z, d, bestand)
                        "ordnung" -> ordnungHolen(app, z, d)
                    }
                    fehlversuche.remove(marke)
                } catch (f: Spaeter) {
                    alleGut = false
                } catch (f: Exception) {
                    // Das Dokument bleibt offen und wird wiederholt. Lässt es sich auch dann nicht holen, hält es den Rest nicht länger auf.
                    val versuche = (fehlversuche[marke] ?: 0) + 1
                    fehlversuche[marke] = versuche
                    if (versuche < VERSUCHE) alleGut = false
                    else if (versuche == VERSUCHE) Logbuch.fehler("Ablage-Abgleich: $marke übersprungen", f)
                    Logbuch.netz("Ablage-Abgleich", "$marke: ${f.message}")
                }
            }
            if (alleGut) z.stand = neu
            if (!antwort.optBoolean("mehr")) break
            seit = neu
        }
        return alleGut
    }

    private fun eintragHolen(basis: String, speicher: AblageSpeicher, z: Zustand, d: JSONObject, b: Bestand) {
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
            if (offen != null) { speicher.quittiereLoeschung(id); z.vergiss(schluessel); return }
            if (lokal == null) { z.vergiss(schluessel); return }
            // Hier etwas Neues trotz Löschung am anderen Gerät: Es bleibt und geht wieder hoch, verloren geht nichts.
            if (hierUnveraendert || bekannt == null) {
                speicher.loescheEintrag(id, melden = false)
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

        if (lokal != null && !hierUnveraendert) { konfliktLoesen(basis, speicher, z, schluessel, fern, textFern, lokal, lokalText); return }

        // Ging das Verzeichnis hier verloren, trägt derselbe Text jetzt eine neue Kennung: Sie darf nicht als Doppel an alle gehen.
        val doppelt = if (lokal == null && textFern != null) b.eintraege.values.firstOrNull { it.titel == fern.titel && it.anhaenge.isEmpty() && z.bekannt["eintrag:${it.id}"] == null && textSha(speicher, it) == textFern } else null
        val textTeil = if (textFern != null && textFern != lokalText) laden(basis, textFern, speicher) else null
        val dateien = HashMap<String, File>()
        fern.anhaenge.filter { a -> lokal?.anhaenge?.none { it.id == a.id && it.sha256 == a.sha256 } != false }.forEach { a -> dateien[a.id] = laden(basis, a.sha256, speicher) }
        // Erst wenn alles da ist: Scheitert ein Download, ist die lokale Kopie nicht schon weg.
        doppelt?.let { speicher.loescheEintrag(it.id, melden = false) }
        if (!speicher.setzeFern(fern, textFern != null, textTeil, dateien, lokal?.geaendert)) throw Spaeter("$schluessel hat sich hier gerade geändert")
        z.merke(schluessel, fernAbdruck, geaendert)
    }

    /** Hier und auf dem Server geändert: Die Dateien beider Seiten bleiben, die jüngere Textfassung gilt, die ältere kommt als eigener Eintrag dazu. Das Ergebnis geht wieder an alle. */
    private fun konfliktLoesen(basis: String, speicher: AblageSpeicher, z: Zustand, schluessel: String, fern: Eintrag, textFern: String?, lokal: Eintrag, lokalText: String?) {
        Logbuch.netz("Ablage-Abgleich", "Konflikt bei „${lokal.titel}“: beide Seiten geändert, vereine")
        val fernNeuer = fern.geaendert >= lokal.geaendert
        val nurHier = lokal.anhaenge.filter { x -> fern.anhaenge.none { it.id == x.id } }
        val dateien = HashMap<String, File>()
        fern.anhaenge.filter { a -> lokal.anhaenge.none { it.id == a.id && it.sha256 == a.sha256 } }.forEach { a -> dateien[a.id] = laden(basis, a.sha256, speicher) }
        val textTeil = if (fernNeuer && textFern != null && textFern != lokalText) laden(basis, textFern, speicher) else null
        if (textFern != null && lokalText != null && textFern != lokalText) {
            val aelter = if (fernNeuer) speicher.text(lokal) else laden(basis, textFern, speicher).let { f -> f.readText().also { f.delete() } }
            // Der Titel hängt an der älteren Fassung: Eine Wiederholung ergibt keinen zweiten Eintrag, eine frühere ältere Fassung bleibt.
            val wann = java.text.SimpleDateFormat("dd.MM.yyyy HH.mm", Locale.GERMAN).format(java.util.Date(minOf(fern.geaendert, lokal.geaendert)))
            speicher.schreibeText("${lokal.titel} (ältere Fassung $wann)", aelter)
        }
        val basisEintrag = if (fernNeuer) fern else lokal.copy(anhaenge = emptyList())
        val vereint = basisEintrag.copy(anhaenge = fern.anhaenge + nurHier, geaendert = maxOf(fern.geaendert, lokal.geaendert))
        val mitText = lokalText != null || textFern != null
        // Hatte nur eine Seite Text, bleibt er; die Fassung von hier liegt schon an ihrem Platz.
        val fernOhneLokal = !fernNeuer && lokalText == null && textFern != null
        val nurFernText = if (fernOhneLokal) laden(basis, textFern!!, speicher) else textTeil
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
            if (offen != null) { Prompts.quittiereLoeschung(app, erstellt); z.vergiss(schluessel); return }
            if (lokal == null) { z.vergiss(schluessel); return }
            if (hierUnveraendert || bekannt == null) { Prompts.loescheFern(app, erstellt); z.vergiss(schluessel) } else z.zeiten[schluessel] = geaendert
            return
        }
        val w = d.getJSONObject("daten")
        val fern = Prompt(lokal?.nummer ?: 1, w.optString("text"), erstellt, w.optString("titel"), w.optString("quelle"), geaendert)
        if (fern.text.isEmpty()) return
        // Nur das Echo des schon Bekannten (auch bei nachgehender Uhr): Eine Löschung oder Änderung von hier gilt weiter.
        if (offen != null) { if (bekannt == abdruck(fern) || offen >= geaendert) return; Prompts.quittiereLoeschung(app, erstellt) }
        if (lokal != null && abdruck(lokal) == abdruck(fern)) { z.merke(schluessel, abdruck(fern), geaendert); return }
        if (lokal != null && bekannt == abdruck(fern)) return
        // Beide geändert: Die jüngere Fassung gilt, die ältere bleibt als eigener Prompt. Eine bloße KI-Überschrift hier ist kein Grund dafür.
        if (lokal != null && !hierUnveraendert && lokal.text != fern.text) {
            if (geaendert < lokal.geaendert) { Prompts.setzeFern(app, fern.copy(erstellt = lokal.erstellt + 1, titel = "", geaendert = geaendert)); return }
            Prompts.setzeFern(app, lokal.copy(erstellt = geaendert + 1, titel = "", nummer = 1))
        }
        if (Prompts.setzeFern(app, fern)) z.merke(schluessel, abdruck(fern), geaendert) else throw IOException("Prompt-Datei nicht schreibbar")
    }

    private fun ordnungHolen(app: Context, z: Zustand, d: JSONObject) {
        val zeit = d.optLong("geaendert")
        val lokal = Prompts.ordnungZeit(app)
        if (zeit == lokal) { z.merke("ordnung:prompts", zeit.toString(), zeit); return }
        if (zeit > lokal) {
            val liste = d.getJSONObject("daten").optJSONArray("reihenfolge") ?: return
            if (Prompts.ordneFern(app, (0 until liste.length()).map { liste.optLong(it) }, zeit)) z.merke("ordnung:prompts", zeit.toString(), zeit)
        }
    }

    /**
     * Lädt eine Datei in den Teil-Ordner und prüft sie; erst danach kommt sie in die Ablage. Der Teil-Name kommt aus der Prüfsumme:
     * Ein abgebrochener Download (Netz, Gespräch) macht beim nächsten Lauf an derselben Stelle weiter statt von vorn.
     */
    private fun laden(basis: String, sha: String, speicher: AblageSpeicher): File {
        val teil = File(speicher.teilOrdner, sicher(sha) + ".teil")
        if (teil.isFile && teil.length() > 0 && AblageSpeicher.sha256(teil) == sha) return teil
        val schon = if (teil.isFile) teil.length() else 0L
        val anfrage = Request.Builder().url("$basis/blob/$sha").apply { if (schon > 0) header("Range", "bytes=$schon-") }.build()
        http(anfrage).use { r ->
            if (r.code == 416) { teil.delete(); throw IOException("Teildatei passte nicht, beginne neu") }
            if (!r.isSuccessful) throw IOException("Datei nicht abrufbar (${r.code})")
            FileOutputStream(teil, r.code == 206).use { aus ->
                val ein = r.body!!.byteStream()
                val puffer = ByteArray(64 * 1024)
                while (true) {
                    gespraechPruefen()
                    val n = ein.read(puffer)
                    if (n < 0) break
                    aus.write(puffer, 0, n)
                }
            }
        }
        if (AblageSpeicher.sha256(teil) != sha) { teil.delete(); throw IOException("Prüfsumme passt nicht") }
        return teil
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
            } catch (f: Exception) {
                allesGut = false
                Logbuch.netz("Ablage-Abgleich", "$was: ${f.message}")
            }
        }
        /** Der Server hat eine jüngere Fassung: Der nächste Lauf holt alles und führt beide zusammen. */
        fun abgelehnt() { vollabgleich = true; allesGut = false }
        // Beim Vollabgleich ist die Lage neu (Geheimnis, Server, Zeitlimit eines Proxys): Abgelehntes bekommt noch eine Chance.
        if (vollabgleich) aufgegeben.clear()

        speicher.offeneLoeschungen().forEach { (id, zeit) ->
            gespraechAbwarten()
            val s = "eintrag:$id"
            versuch(s, "$s:weg") {
                val t = z.zeitFuer(s, zeit)
                if (put(basis, "eintrag", id, JSONObject(), t, geloescht = true)) { speicher.quittiereLoeschung(id); z.vergiss(s) } else abgelehnt()
            }
        }
        Prompts.offeneLoeschungen(app).forEach { (id, zeit) ->
            gespraechAbwarten()
            val s = "prompt:$id"
            versuch(s, "$s:weg") {
                val t = z.zeitFuer(s, zeit)
                if (put(basis, "prompt", id.toString(), JSONObject(), t, geloescht = true)) { Prompts.quittiereLoeschung(app, id); z.vergiss(s) } else abgelehnt()
            }
        }
        val verloren = speicher.verloren()
        speicher.eintraege().forEach { alt ->
            val s = "eintrag:${alt.id}"
            // Abhanden gekommen und auf dem Server bekannt: Das holt der Abgleich zurück, es ist keine Änderung für die anderen.
            if (alt.id in verloren && z.bekannt[s] != null) return@forEach
            if (z.bekannt[s] == abdruck(alt, textSha(speicher, alt))) return@forEach
            gespraechAbwarten()
            // Frisch gelesen: Während eines langen Uploads oder Gesprächs kann der Eintrag sich geändert haben oder gelöscht sein.
            val e = speicher.eintrag(alt.id) ?: return@forEach
            val sha = textSha(speicher, e)
            val a = abdruck(e, sha)
            versuch(s, "$s:$a") {
                val inhalte = HashMap<String, File>()
                if (sha != null) speicher.textDatei(e)?.let { inhalte[sha] = it }
                val mitSha = e.anhaenge.map { x -> if (x.sha256.isBlank()) x.copy(sha256 = AblageSpeicher.sha256(speicher.datei(x))) else x }
                mitSha.forEach { x -> inhalte[x.sha256] = speicher.datei(x) }
                val t = z.zeitFuer(s, e.geaendert)
                if (put(basis, "eintrag", e.id, eintragAlsDaten(speicher, e.copy(anhaenge = mitSha), sha), t, inhalte = inhalte)) { z.merke(s, a, t); speicher.verlorenErledigt(e.id) } else abgelehnt()
            }
            z.sichere()
        }
        val prompts = Prompts.alle(app)
        prompts.forEach { p ->
            val s = "prompt:${p.erstellt}"
            val a = abdruck(p)
            if (z.bekannt[s] == a) return@forEach
            gespraechAbwarten()
            versuch(s, "$s:$a") {
                val daten = JSONObject().put("text", p.text).put("titel", p.titel).put("erstellt", p.erstellt).put("quelle", p.quelleName)
                val t = z.zeitFuer(s, p.geaendert)
                if (put(basis, "prompt", p.erstellt.toString(), daten, t)) z.merke(s, a, t) else abgelehnt()
            }
        }
        val zeit = Prompts.ordnungZeit(app).coerceAtLeast(1)
        if (z.bekannt["ordnung:prompts"] != zeit.toString() && prompts.isNotEmpty()) {
            gespraechAbwarten()
            versuch("Reihenfolge", "ordnung:$zeit") {
                val ids = Prompts.alle(app).map { it.erstellt }
                val daten = JSONObject().put("reihenfolge", JSONArray(ids))
                if (put(basis, "ordnung", "prompts", daten, zeit)) { z.merke("ordnung:prompts", zeit.toString(), zeit); Prompts.ordnungGesendet(app, ids) } else abgelehnt()
            }
        }
        return allesGut
    }

    /** Sendet ein Dokument; fehlen dem Server dazu Dateien, kommen sie zuerst hoch. true = der Server hat diese Fassung übernommen. */
    private fun put(basis: String, art: String, id: String, daten: JSONObject, geaendert: Long, geloescht: Boolean = false, inhalte: Map<String, File> = emptyMap()): Boolean {
        val rumpf = JSONObject().put("daten", daten).put("geaendert", geaendert).put("geloescht", geloescht).put("von", AblageQuellen.HANDY).toString()
        fun versuch() = http(Request.Builder().url("$basis/doc/$art/$id").put(rumpf.toRequestBody(json)).build())
        // Nur was der Inhalt selbst verschuldet, ist endgültig. 404 kann ein laufendes Ausrollen sein, 408 und 429 der Proxy.
        fun ablehnung(code: Int): IOException = if (code == 400 || code == 413) DauerFehler("Server antwortet mit $code") else IOException("Server antwortet mit $code")
        versuch().use { r ->
            if (r.isSuccessful) return JSONObject(r.body?.string().orEmpty()).optBoolean("uebernommen")
            if (r.code != 409) throw ablehnung(r.code)
            val fehlend = JSONObject(r.body?.string().orEmpty()).getJSONArray("fehlend")
            for (i in 0 until fehlend.length()) {
                val sha = fehlend.getString(i)
                val datei = inhalte[sha] ?: throw IOException("Datei zu $sha fehlt hier")
                http(Request.Builder().url("$basis/blob/$sha").put(GespraechsBewusst(datei, roh)).build()).use { h -> if (!h.isSuccessful) throw ablehnung(h.code) }
            }
        }
        versuch().use { r -> if (!r.isSuccessful) throw ablehnung(r.code); return JSONObject(r.body?.string().orEmpty()).optBoolean("uebernommen") }
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

    private fun http(anfrage: Request) = client.newCall(anfrage).execute()

    /** Kennungen vom Server landen in Dateipfaden. */
    private fun sicher(wert: String): String { if (!Regex("[A-Za-z0-9_-]{1,64}").matches(wert)) throw IOException("Ungültige Kennung"); return wert }

    private fun textSha(speicher: AblageSpeicher, e: Eintrag): String? {
        val f = speicher.textDatei(e) ?: return null
        textSummen[f.path]?.takeIf { it.first == f.length() && it.second == f.lastModified() }?.let { return it.third }
        return AblageSpeicher.sha256(f).also { textSummen[f.path] = Triple(f.length(), f.lastModified(), it) }
    }

    /** Alles, was zu allen Versionen muss; ohne Änderungszeit und ohne das, was jede Version selbst berechnet. */
    private fun abdruck(e: Eintrag, textSha: String?): String = listOf(e.titel, textSha.orEmpty(), e.erstellt, e.herkunft, e.quelleName, e.beschreibung).joinToString("\u0001") + "\u0002" +
        e.anhaenge.sortedBy { it.id }.joinToString("\u0002") { listOf(it.id, it.titel, it.originalName, it.endung, it.mime, it.groesse, it.erstellt, it.herkunft, it.sha256, it.beschreibung, it.kennung.orEmpty()).joinToString("\u0001") }

    private fun abdruck(p: Prompt): String = listOf(p.text, p.titel, p.quelleName).joinToString("\u0001")

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
