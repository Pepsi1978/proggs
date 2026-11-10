package de.frank.jarvis.abgleich

import android.content.Context
import android.net.Uri
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Hält Jarvis' eigenes Gedächtnis und seine Gespräche auf Franks Geräten gleich (Handy ↔ Tablet): die Merkdateien (Regeln, Ziele,
 * „Über Frank“, Entscheidungen, Symbiose), Langzeitkontext, Tagebuch-Kurzfassungen, Tagesauswertungen und jedes Gespräch als eigene
 * Datei. Ablage, Prompts und Erinnerungen laufen über [de.frank.jarvis.ablage.AblageSync], die Daten der angebundenen Apps über
 * [GeraeteAbgleich]; abgeleitete Stände (Lagebild, Wetter, Kontingente) liest jedes Gerät selbst.
 *
 * Je Datei ein Dokument im Geräteabgleich-Speicher des Servers (Art „gedaechtnis“ bzw. „gespraech“), der Inhalt als Datei nach
 * seiner Prüfsumme. Regeln:
 * - **Je Datei gewinnt die jüngste Fassung.** Hat nur ein Gerät seit dem letzten Abgleich geändert, gilt dessen Fassung; haben
 *   beide geändert, die mit der jüngeren Änderungszeit. Jede Fassung, die der Abgleich hier überschreibt oder löscht, bleibt 30 Tage
 *   unter `abgleich-rueckfall/` liegen (höchstens 500).
 * - **Erster Kontakt:** Kennt ein Gerät eine Datei noch nicht als abgeglichen und der Server hat eine andere Fassung, übernimmt
 *   das Tablet die des Servers, das Handy behält seine und lädt sie hoch (das Handy ist die Erstquelle).
 * - **Löschen** gibt es nur bei Gesprächen: Ein hier gelöschtes Gespräch wird als gelöscht gemeldet und verschwindet auch drüben.
 * - Geschrieben wird erst in eine Nachbardatei, dann umbenannt; Gespräche unter derselben Sperre wie jedes andere Schreiben.
 * - Nie während eines Sprachgesprächs (Sprachmodus vor Hintergrundarbeit).
 */
object GedaechtnisAbgleich {
    /** Eine abgeglichene Stelle unter filesDir: eine Datei oder alle Dateien eines Ordners (nicht rekursiv), die auf [endung] enden. */
    private class Quelle(val art: String, val pfad: String, val ordner: Boolean = false, val endung: String = "", val loeschbar: Boolean = false)

    /**
     * Bewusst nicht dabei (geprüft am 10.11.2026): die Dateien des Lernlaufs (`lernberichte.json`, `lern_pruefungen.json`,
     * `gespraeche_gelernt.json`; der Lernlauf hält sie über Minuten im Speicher und schriebe einen alten Stand zurück), die
     * Funktionsübersicht (`funktionen/`, an die installierte Version gebunden), `vorgaenge.json` (laufende Aufträge dieses
     * Prozesses), die Protokolle (`merk_verlauf.jsonl`, `lern_aenderungen.jsonl`, `stil/`: nur Anhängen, ein Ersetzen verlöre Zeilen),
     * `auftragstexte/` (gehören zu den Programmier-Gesprächen) und `langzeitkontext-vorfassung.txt` (Sicherung je Gerät).
     */
    private val QUELLEN: List<Quelle> by lazy {
        de.frank.jarvis.faehigkeit.merkdateien().map { Quelle("gedaechtnis", it.name) } + listOf(
        Quelle("gedaechtnis", "langzeitkontext.txt"),
        Quelle("gedaechtnis", "gespraechsnotizen.json"),
        Quelle("gedaechtnis", "projekte.json"),
        Quelle("gedaechtnis", "tagebuch_kurz", ordner = true, endung = ".json"),
        Quelle("gedaechtnis", "tagebuch_verdichtet", ordner = true),
        // Das Handy behält nur die letzten 100 Fassungen: Was dort wegfällt, fällt auch auf dem Tablet weg.
        Quelle("gedaechtnis", "tagesauswertung", ordner = true, endung = ".json", loeschbar = true),
        Quelle("gedaechtnis", "tagesauswertung_archiv", ordner = true, endung = ".txt"),
        Quelle("gedaechtnis", "agenten", ordner = true, endung = ".json", loeschbar = true),
        Quelle("gespraech", "gespraeche", ordner = true, endung = ".json", loeschbar = true),
        // Bilder und Dateien in Gesprächen: Die Nachrichten nennen sie mit ihrem Pfad, der auf beiden Geräten gleich lautet.
        // Löschbar: Mit einem Gespräch verschwinden auch seine Anhänge, sonst blieben ihre Dateien für immer am Server.
        Quelle("gedaechtnis", "gespraeche/anhaenge", ordner = true, loeschbar = true),
        )
    }

    /** Setzt die Oberfläche: Der Abgleich hat Gespräche oder Projekte eingespielt, die Seitenleiste soll neu lesen. */
    @Volatile var beiGespraechen: (() -> Unit)? = null
    private const val ABSTAND_MS = 60_000L
    private const val RUECKFALL = "abgleich-rueckfall"
    /** Sicherungen und halb geschriebene Dateien der Speicher (AtomicFile, eigene .bak/.defekt-Kopien): nie abgleichen. */
    private val NEBENDATEI = listOf(".teil", ".bak", ".new", ".neu", ".tmp", ".abgleich", ".defekt", ".versuche", ".kaputt")

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val anstoss = Channel<Unit>(Channel.CONFLATED)
    private var schleife: Job? = null
    private val netz = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
    private val JSON = "application/json".toMediaType()

    fun anstossen() { anstoss.trySend(Unit) }

    @Synchronized
    fun starte(context: Context) {
        val app = context.applicationContext
        if (schleife?.isActive == true) return
        schleife = bereich.launch {
            delay(15_000)   // Beim Start des Dienstes haben Verbindung, Gedächtnis und App-Abgleich Vorrang.
            while (isActive) {
                runCatching { runde(app) }.onFailure { Logbuch.fehler("Gedächtnis-Abgleich", it) }
                withTimeoutOrNull(ABSTAND_MS) { anstoss.receive() }
                delay(2_000)
            }
        }
    }

    // ---------------------------------------------------------------- Zustand

    private fun zustandDatei(app: Context) = File(app.filesDir, "gedaechtnis-abgleich.json")
    private fun lade(app: Context): JSONObject = runCatching { JSONObject(zustandDatei(app).readText()) }.getOrElse { JSONObject() }
    private fun sichere(app: Context, z: JSONObject) {
        val teil = File(app.filesDir, "gedaechtnis-abgleich.json.teil")
        teil.writeText(z.toString())
        if (!teil.renameTo(zustandDatei(app))) { zustandDatei(app).delete(); teil.renameTo(zustandDatei(app)) }
    }
    private fun JSONObject.obj(name: String): JSONObject = optJSONObject(name) ?: JSONObject().also { put(name, it) }

    /** Kennung des Dokuments am Server (nur Buchstaben, Ziffern, _ und -); der echte Pfad steht im Dokument. */
    private fun kennung(pfad: String): String {
        val glatt = pfad.replace(Regex("[^A-Za-z0-9_-]"), "_")
        // Namen mit Leerzeichen oder Umlauten (Anhänge) bekommen ein Stück Prüfsumme dazu: „bild 1.jpg“ und „bild_1.jpg“ fielen sonst
        // auf dieselbe Kennung, und nur eine der beiden Dateien käme drüben an.
        val eindeutig = pfad.all { it.isLetterOrDigit() && it.code < 128 || it in "_-./" }
        return if (eindeutig && glatt.length <= 64) glatt else glatt.take(40) + "_" + sha256(pfad.toByteArray()).take(23)
    }

    /** Die Quelle, zu der ein Pfad gehört, oder null: Nur Pfade abgeglichener Stellen werden je geschrieben oder gelöscht. */
    private fun quelleZu(pfad: String): Quelle? {
        if (pfad.contains("..") || pfad.startsWith("/") || pfad.contains('\\')) return null
        return QUELLEN.firstOrNull { q ->
            if (!q.ordner) q.pfad == pfad
            else pfad.startsWith(q.pfad + "/") && !pfad.removePrefix(q.pfad + "/").contains('/') && pfad.endsWith(q.endung) && NEBENDATEI.none { pfad.endsWith(it) || pfad.contains("$it-") }
        }
    }

    /** Alle abgeglichenen Dateien dieses Geräts. [unlesbar] sammelt Ordner, die sich gerade nicht auflisten ließen: Dort gilt nichts als gelöscht. */
    private fun lokaleDateien(app: Context, unlesbar: MutableSet<String>): Map<String, File> {
        val alle = LinkedHashMap<String, File>()
        for (q in QUELLEN) {
            val f = File(app.filesDir, q.pfad)
            if (!q.ordner) { if (f.isFile) alle[q.pfad] = f; continue }
            val liste = f.listFiles { d -> d.isFile }
            if (liste == null) { unlesbar += q.pfad; continue }
            liste.sortedBy { it.name }.forEach { d -> "${q.pfad}/${d.name}".let { p -> if (quelleZu(p) != null) alle[p] = d } }
        }
        return alle
    }

    // ---------------------------------------------------------------- Runde

    private fun runde(app: Context) {
        val e = Einstellungen.get(app)
        if (!e.geraeteAbgleichAn || e.serverHost.isBlank() || e.serverToken.isBlank()) return
        if (de.frank.jarvis.speech.GespraechsFokus.sprachmodus) return
        val basis = "https://${e.serverHost}/j/${e.geheimnis}/abgleich"
        val z = lade(app)
        val dateien = z.obj("dateien")
        val eingespielt = ArrayList<String>()
        var hochgeladen = 0
        try {
            // 1. Was liegt am Server Neues?
            hole(basis, z, dateien)
            // 2. Fremde Fassungen einspielen (oder behalten, wenn die eigene jünger ist). Alle offenen, nicht nur die eben gemeldeten:
            //    Eine Runde, die mittendrin am Netz scheiterte, holt hier nach, ebenso ein Gespräch, das vorhin noch offen war.
            for (pfad in dateien.keys().asSequence().toList()) {
                val s = dateien.optJSONObject(pfad) ?: continue
                val fern = s.optJSONObject("fern") ?: continue
                val offen = if (fern.optBoolean("weg")) !s.optBoolean("weg") else fern.optString("sha") != s.optString("basis")
                if (!offen || istOffen(app, pfad)) continue
                if (pause()) return
                jeDatei(pfad) { if (uebernimm(app, basis, pfad, s)) eingespielt += pfad }
            }
            // 3. Eigene Änderungen und Löschungen hochladen. Zwischendurch sichern: Der erste Lauf lädt ein paar hundert Dateien.
            val unlesbar = HashSet<String>()
            val lokal = lokaleDateien(app, unlesbar)
            for ((pfad, datei) in lokal) {
                if (pause()) return
                jeDatei(pfad) { if (ladeHoch(basis, pfad, datei, dateien.obj(pfad)) && ++hochgeladen % 25 == 0) sichere(app, z) }
            }
            // Hier gelöscht: abgeglichen gewesen, jetzt nicht mehr da. Nie aus einem Ordner, der sich nicht lesen ließ.
            val geloescht = dateien.keys().asSequence().toList().filter { pfad ->
                val s = dateien.optJSONObject(pfad)
                val q = quelleZu(pfad)
                s != null && q != null && q.loeschbar && q.pfad !in unlesbar && pfad !in lokal && s.optString("basis").isNotEmpty() && !s.optBoolean("weg")
            }
            // Schutz vor einem Massenlöschen durch einen Fehler (leer gelesener Ordner, zurückgesetzte App-Daten): Verschwindet auf einen
            // Schlag der größere Teil eines Ordners, wird nichts als gelöscht gemeldet. Frank löscht Gespräche einzeln.
            for ((ordner, pfade) in geloescht.groupBy { quelleZu(it)!!.pfad }) {
                val bekannt = dateien.keys().asSequence().count { quelleZu(it)?.pfad == ordner && dateien.optJSONObject(it)?.optString("basis").orEmpty().isNotEmpty() }
                if (pfade.size > 20 && pfade.size * 2 > bekannt) {
                    val text = "$ordner: ${pfade.size} von $bekannt Dateien fehlen plötzlich. Nichts als gelöscht gemeldet; auf dem anderen Gerät bleiben sie stehen."
                    if (gemeldet.put("massen:$ordner", text) != text) Logbuch.fehler("Gedächtnis-Abgleich", null, text)
                    continue
                }
                for (pfad in pfade) jeDatei(pfad) { meldeGeloescht(basis, pfad, dateien.getJSONObject(pfad)); hochgeladen++ }
            }
        } catch (x: IOException) {
            // Netz weg: Der Rest folgt in der nächsten Runde.
        } finally {
            sichere(app, z)
            raeumeRueckfall(app)
            if (eingespielt.size + hochgeladen > 0) Logbuch.system("Gedächtnis-Abgleich", "${eingespielt.size} übernommen, $hochgeladen hochgeladen")
            if (eingespielt.isNotEmpty()) runCatching { nachEinspielen(app, eingespielt) }
        }
    }

    /** Der Server hat eine einzelne Datei abgelehnt (kein Netzfehler): Die Runde geht mit den übrigen weiter. */
    private class Abgelehnt(text: String) : Exception(text)

    /** Im Sprachgespräch ruht der Abgleich sofort, auch mitten im ersten großen Lauf (Sprachmodus vor Hintergrundarbeit). */
    private fun pause() = de.frank.jarvis.speech.GespraechsFokus.sprachmodus

    /** Eine Datei nach der anderen: Scheitert eine am Server oder an der Platte, kommen die übrigen trotzdem dran. Ein Netzfehler beendet die Runde. */
    private inline fun jeDatei(pfad: String, block: () -> Unit) {
        try {
            block()
            gemeldet.remove(pfad)
        } catch (x: IOException) {
            throw x
        } catch (x: Exception) {
            val text = x.message ?: x.javaClass.simpleName
            if (gemeldet.put(pfad, text) != text) Logbuch.fehler("Gedächtnis-Abgleich", null, "$pfad: $text")
        }
    }
    private val gemeldet = HashMap<String, String>()

    /** Liest die Änderungen seit dem letzten Stand und merkt je Datei die Fassung des Servers (`fern`). */
    private fun hole(basis: String, z: JSONObject, dateien: JSONObject) {
        while (true) {
            val url = "$basis/aenderungen?seit=${z.optLong("stand")}&geraet=${Uri.encode(Geraet.ablageName)}"
            val antwort = netz.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) throw IOException("Server ${r.code}")
                JSONObject(r.body?.string().orEmpty())
            }
            val neuaufbau = antwort.optBoolean("neuaufbau")
            val andereInstanz = z.optString("instanz").isNotEmpty() && z.optString("instanz") != antwort.optString("instanz")
            if (neuaufbau || andereInstanz) {
                // Der Speicher am Server wurde neu aufgesetzt: Was dort lag, ist weg. Die gemerkten Server-Fassungen verwerfen, dann lädt
                // jedes Gerät seinen abgeglichenen Stand wieder hoch (ladeHoch: am Server unbekannt). Der eigene Stand bleibt gültig.
                for (k in dateien.keys()) dateien.optJSONObject(k)?.let { it.remove("fern"); it.remove("marke"); it.remove("entschieden") }
            }
            if (!neuaufbau && andereInstanz && z.optLong("stand") != 0L) {
                z.put("stand", 0).put("instanz", antwort.optString("instanz"))
                continue
            }
            z.put("instanz", antwort.optString("instanz"))
            val docs = antwort.optJSONArray("docs")
            for (i in 0 until (docs?.length() ?: 0)) {
                val d = docs!!.getJSONObject(i)
                if (d.optString("art") != "gedaechtnis" && d.optString("art") != "gespraech") continue
                val daten = d.optJSONObject("daten") ?: JSONObject()
                // Ein Grabstein trägt keine Daten mehr: Der Pfad steht dann nur im eigenen Zustand (über die Kennung gefunden).
                val pfad = daten.optString("pfad").ifEmpty { dateien.keys().asSequence().firstOrNull { kennung(it) == d.optString("id") }.orEmpty() }
                val quelle = quelleZu(pfad) ?: continue
                if (quelle.art != d.optString("art")) continue
                dateien.obj(pfad).put("fern", JSONObject().put("sha", daten.optString("text_sha")).put("geaendert", d.optLong("geaendert"))
                    .put("weg", d.optBoolean("geloescht")).put("von", d.optString("von")))
            }
            z.put("stand", antwort.optLong("stand"))
            if (!antwort.optBoolean("mehr")) return
        }
    }

    /** Gleicht eine Datei mit der Fassung des Servers ab. True, wenn hier etwas geschrieben oder gelöscht wurde. */
    private fun uebernimm(app: Context, basis: String, pfad: String, s: JSONObject): Boolean {
        val fern = s.optJSONObject("fern") ?: return false
        val quelle = quelleZu(pfad) ?: return false
        val datei = File(app.filesDir, pfad)
        val gemerkt = s.optString("basis")
        // Ein Gespräch, das hier inzwischen zu einem Programmierauftrag gehört, bleibt diesem Gerät: weder überschreiben noch löschen
        // (ladeHoch lädt es ab da auch nicht mehr hoch).
        if (quelle.art == "gespraech" && datei.isFile && de.frank.jarvis.data.Gespraeche.istProgrammierDatei(datei)) return false
        val lokal = if (datei.isFile) sha256(datei) else ""
        val lokalGeaendert = lokal != gemerkt
        // Bewertet und bewusst behalten: Erst dann darf ladeHoch die fremde Fassung ablösen (siehe dort).
        fun behalten(): Boolean { s.put("entschieden", fernMarke(fern)); return false }
        if (fern.optBoolean("weg")) {
            if (!quelle.loeschbar || !datei.isFile) { s.put("basis", "").put("weg", true); return false }
            // Hier seither geändert und jünger als das Löschen drüben: behalten, die nächste Runde lädt es wieder hoch.
            if (lokalGeaendert && gemerkt.isNotEmpty() && datei.lastModified() > fern.optLong("geaendert")) return behalten()
            if (gemerkt.isEmpty() && !Geraet.istTablet) return behalten()
            rueckfall(app, pfad, datei)
            loesche(app, quelle, datei)
            s.put("basis", "").put("weg", true)
            return true
        }
        val fernSha = fern.optString("sha")
        if (fernSha.isEmpty()) return false
        if (lokal == fernSha) { s.put("basis", fernSha).put("weg", false); return false }
        val nimmFern = when {
            !datei.isFile -> true
            !lokalGeaendert -> true                                   // nur drüben geändert
            gemerkt.isEmpty() -> Geraet.istTablet                     // erster Kontakt: Das Handy ist die Erstquelle
            else -> datei.lastModified() <= fern.optLong("geaendert")  // beide geändert: die jüngere Änderung gewinnt
        }
        if (!nimmFern) return behalten()
        val neu = File(app.cacheDir, "gedaechtnis-fern.teil")
        try {
            netz.newCall(Request.Builder().url("$basis/blob/$fernSha").build()).execute().use { r ->
                if (!r.isSuccessful) throw Abgelehnt("Datei holen: Server ${r.code}")
                neu.outputStream().use { aus -> r.body!!.byteStream().copyTo(aus) }
            }
            if (sha256(neu) != fernSha) throw Abgelehnt("Datei beschädigt angekommen")
            // Vor jedem fremden Überschreiben bleibt die hiesige Fassung als Rückfall liegen, nicht nur im Konflikt: Käme vom anderen
            // Gerät eine beschädigte oder leer neu begonnene Datei mit jüngerer Zeit, wäre der gute Stand sonst ersatzlos weg.
            val konflikt = datei.isFile && lokalGeaendert && gemerkt.isNotEmpty()
            if (datei.isFile) rueckfall(app, pfad, datei, immer = konflikt)
            if (konflikt) {
                Logbuch.system("Gedächtnis-Abgleich", "$pfad: beide Geräte haben geändert, die Fassung von ${fern.optString("von")} ist jünger und gilt; die hiesige liegt unter $RUECKFALL")
            }
            schreibe(app, quelle, datei, neu, fern.optLong("geaendert"))
            s.put("basis", fernSha).put("weg", false)
            return true
        } finally {
            neu.delete()
        }
    }

    /** Lädt eine hier geänderte Datei hoch. True, wenn hochgeladen. */
    private fun ladeHoch(basis: String, pfad: String, datei: File, s: JSONObject): Boolean {
        // Unverändert seit dem letzten Blick (Größe und Zeit): ohne Prüfsumme weiter.
        val marke = "${datei.length()}:${datei.lastModified()}"
        if (s.optString("marke") == marke) return false
        val quelle = quelleZu(pfad) ?: return false
        // Programmier-Gespräche bleiben je Gerät: Sie hängen am Auftragsbuch dieses Geräts (Stand, Rückfragen, Nachträge), und am
        // Rechner gestartete Sitzungen legt ohnehin jedes Gerät selbst an; abgeglichen erschienen sie doppelt und ohne ihre Knöpfe.
        if (quelle.art == "gespraech" && de.frank.jarvis.data.Gespraeche.istProgrammierDatei(datei)) {
            s.put("marke", marke)
            return false
        }
        val sha = sha256(datei)
        val fern = s.optJSONObject("fern")
        // Abgeglichen und am Server bekannt: nichts zu tun. Kennt der Server die Datei nicht (mehr), etwa nach einem neu aufgesetzten
        // Speicher, geht sie unten wieder hoch.
        if (sha == s.optString("basis") && fern != null) { s.put("marke", marke); return false }
        if (fern != null && !fern.optBoolean("weg") && fern.optString("sha") == sha) { s.put("basis", sha).put("marke", marke).put("weg", false); return false }
        // Eine fremde Fassung wird nur abgelöst, wenn [uebernimm] sie bewertet und bewusst nicht genommen hat (die hiesige ist jünger,
        // oder dies ist das Handy beim ersten Kontakt). Steht die Bewertung noch aus (das Gespräch ist gerade offen, der Download
        // scheiterte, die Fassung kam eben erst), wartet der Upload: Sonst gewönne eine ältere Fassung nur, weil sie zuerst hochlud.
        val ausstehend = fern != null && (if (fern.optBoolean("weg")) !s.optBoolean("weg") else fern.optString("sha") != s.optString("basis"))
        if (ausstehend && s.optString("entschieden") != fernMarke(fern!!)) return false
        netz.newCall(Request.Builder().url("$basis/blob/$sha").put(datei.asRequestBody("application/octet-stream".toMediaType())).build()).execute().use { r ->
            if (!r.isSuccessful) throw Abgelehnt("Datei hochladen: Server ${r.code}")
        }
        // Die Datei kann sich während des Hochladens geändert haben: Dann passt die Prüfsumme am Server nicht, die nächste Runde lädt neu.
        val geaendert = maxOf(datei.lastModified(), (fern?.optLong("geaendert") ?: 0L) + 1)
        val daten = JSONObject().put("pfad", pfad).put("text_sha", sha).put("groesse", datei.length())
        val rumpf = JSONObject().put("daten", daten).put("geaendert", geaendert).put("geloescht", false).put("von", Geraet.ablageName).toString()
        val antwort = netz.newCall(Request.Builder().url("$basis/doc/${quelle.art}/${kennung(pfad)}").put(rumpf.toRequestBody(JSON)).build()).execute().use { r ->
            if (r.code == 409) return false   // Datei am Server (noch) nicht vollständig: nächste Runde
            if (!r.isSuccessful) throw Abgelehnt("Fassung melden: Server ${r.code}")
            JSONObject(r.body?.string().orEmpty())
        }
        if (!antwort.optBoolean("uebernommen")) {
            // Der Server hat eine jüngere Fassung und gibt sie mit (auch einen Grabstein, den hole() keinem Pfad zuordnen konnte): als
            // fremde Fassung merken. Die nächste reguläre Runde bewertet sie; kein sofortiges Wiederholen im Sekundentakt.
            val d = antwort.optJSONObject("doc") ?: return false
            s.put("fern", JSONObject().put("sha", d.optJSONObject("daten")?.optString("text_sha").orEmpty()).put("geaendert", d.optLong("geaendert"))
                .put("weg", d.optBoolean("geloescht")).put("von", "dem anderen Gerät"))
            s.remove("entschieden")
            return false
        }
        s.put("basis", sha).put("marke", marke).put("weg", false)
        s.put("fern", JSONObject().put("sha", sha).put("geaendert", geaendert).put("weg", false).put("von", Geraet.ablageName))
        return true
    }

    /** Woran [uebernimm] und [ladeHoch] eine bestimmte fremde Fassung wiedererkennen. */
    private fun fernMarke(fern: JSONObject) = if (fern.optBoolean("weg")) "weg:${fern.optLong("geaendert")}" else fern.optString("sha")

    private fun meldeGeloescht(basis: String, pfad: String, s: JSONObject) {
        val quelle = quelleZu(pfad) ?: return
        val geaendert = maxOf(System.currentTimeMillis(), (s.optJSONObject("fern")?.optLong("geaendert") ?: 0L) + 1)
        val rumpf = JSONObject().put("daten", JSONObject()).put("geaendert", geaendert).put("geloescht", true).put("von", Geraet.ablageName).toString()
        netz.newCall(Request.Builder().url("$basis/doc/${quelle.art}/${kennung(pfad)}").put(rumpf.toRequestBody(JSON)).build()).execute().use { r ->
            if (!r.isSuccessful) throw Abgelehnt("Löschen melden: Server ${r.code}")
        }
        s.put("basis", "").put("weg", true).put("fern", JSONObject().put("sha", "").put("geaendert", geaendert).put("weg", true).put("von", Geraet.ablageName))
    }

    // ---------------------------------------------------------------- Schreiben

    private fun schreibe(app: Context, quelle: Quelle, ziel: File, neu: File, zeit: Long) {
        val schreiben: () -> Unit = {
            ziel.parentFile?.mkdirs()
            // Nie „<name>.new“: Dorthin schreibt die App selbst (AtomicFile), und beim Lesen löscht sie die Datei.
            val teil = File(ziel.path + ".abgleich")
            neu.copyTo(teil, overwrite = true)
            // Eine liegen gebliebene Sicherung spielte die App beim nächsten Lesen über die neue Fassung zurück.
            File(ziel.path + ".bak").delete()
            if (!teil.renameTo(ziel)) { ziel.delete(); check(teil.renameTo(ziel)) { "${ziel.name} ließ sich nicht ersetzen" } }
            if (zeit > 0) ziel.setLastModified(zeit)
        }
        // Unter derselben Sperre wie das eigene Lesen-Ändern-Schreiben des jeweiligen Speichers.
        val merk = if (quelle.ordner) null else de.frank.jarvis.faehigkeit.merkdateien().firstOrNull { it.name == quelle.pfad }
        when {
            merk != null -> merk.vonAussen(schreiben)
            quelle.art == "gespraech" -> de.frank.jarvis.data.Gespraeche.vonAussen(schreiben)
            quelle.pfad == "projekte.json" -> de.frank.jarvis.data.Projekte.vonAussen(schreiben)
            quelle.pfad == "langzeitkontext.txt" -> synchronized(de.frank.jarvis.gedaechtnis.Langzeitkontext) { schreiben() }
            quelle.pfad == "gespraechsnotizen.json" -> synchronized(de.frank.jarvis.gedaechtnis.Gespraechsgedaechtnis) { schreiben() }
            else -> schreiben()
        }
    }

    /** Das gerade offene Gespräch hält die Oberfläche im Speicher und schriebe es über eine eingespielte Fassung zurück: erst nach dem Wechsel übernehmen. */
    private fun istOffen(app: Context, pfad: String): Boolean =
        pfad == "gespraeche/" + app.getSharedPreferences("jarvis_offenes_gespraech", Context.MODE_PRIVATE).getString("id", "").orEmpty() + ".json"

    /** Nach dem Einspielen: Gedächtnis und Oberfläche lesen die betroffenen Bereiche neu. */
    private fun nachEinspielen(app: Context, pfade: List<String>) {
        val bereiche = LinkedHashSet<de.frank.jarvis.gedaechtnis.Bereich>()
        if (pfade.any { !it.contains('/') }) bereiche += de.frank.jarvis.gedaechtnis.Bereich.GEMERKT
        if (pfade.any { it.startsWith("gespraeche/") || it == "gespraechsnotizen.json" }) bereiche += de.frank.jarvis.gedaechtnis.Bereich.GESPRAECHE
        if (pfade.any { it.startsWith("tagebuch_") }) bereiche += de.frank.jarvis.gedaechtnis.Bereich.TAGEBUCH
        if (pfade.any { it.startsWith("tagesauswertung") }) {
            runCatching { de.frank.jarvis.auswertung.Tagesauswertung.neuEinlesen(app) }
            bereiche += de.frank.jarvis.gedaechtnis.Bereich.AUSWERTUNG
        }
        if (bereiche.isNotEmpty()) runCatching { de.frank.jarvis.gedaechtnis.Lagebild.anstossen(app, bereiche, "Geräteabgleich") }
        if (pfade.any { it.startsWith("gespraeche/") || it == "projekte.json" }) runCatching { beiGespraechen?.invoke() }
    }

    private fun loesche(app: Context, quelle: Quelle, datei: File) {
        if (quelle.art == "gespraech") de.frank.jarvis.data.Gespraeche.loeschen(app, datei.name.removeSuffix(".json")) else datei.delete()
    }

    /** Wann je Datei zuletzt eine Rückfallkopie entstand (nur im Speicher): Ein langes Gespräch am anderen Gerät kopierte sonst bei jeder Nachricht. */
    private val rueckfallZuletzt = HashMap<String, Long>()

    /**
     * Legt die hiesige Fassung beiseite, bevor der Abgleich sie überschreibt oder löscht. [immer] = Konflikt oder Löschung: dann stets;
     * sonst je Datei höchstens alle zehn Minuten. Nicht für Dateien, die nie geändert, nur angelegt und entfernt werden (Anhänge,
     * Auswertungen, Tagebuch-Fassungen): Dort gibt es keinen eigenen Stand zu verlieren.
     */
    private fun rueckfall(app: Context, pfad: String, datei: File, immer: Boolean = true) {
        if (pfad.startsWith("gespraeche/anhaenge/") || pfad.startsWith("tagesauswertung") || pfad.startsWith("tagebuch_")) return
        val jetzt = System.currentTimeMillis()
        if (!immer && jetzt - (rueckfallZuletzt[pfad] ?: 0L) < 10 * 60_000L) return
        runCatching {
            val ordner = File(app.filesDir, RUECKFALL).apply { mkdirs() }
            datei.copyTo(File(ordner, "$jetzt-${pfad.replace('/', '_')}"), overwrite = true)
            rueckfallZuletzt[pfad] = jetzt
        }
    }

    /** Einmal je Runde: höchstens 500 Rückfallkopien, nach 30 Tagen fällt Altes weg. */
    private fun raeumeRueckfall(app: Context) {
        runCatching {
            val liste = File(app.filesDir, RUECKFALL).listFiles() ?: return
            if (liste.isEmpty()) return
            val grenze = System.currentTimeMillis() - 30 * 86_400_000L
            liste.sortedByDescending { it.name }.forEachIndexed { i, f -> if (i >= 500 || f.lastModified() < grenze) f.delete() }
        }
    }

    private fun sha256(datei: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        datei.inputStream().use { ein -> val puffer = ByteArray(65536); while (true) { val n = ein.read(puffer); if (n < 0) break; md.update(puffer, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
