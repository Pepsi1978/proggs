package de.frank.jarvis.ablage

import android.content.Context
import android.os.Process
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.speech.GespraechsFokus
import java.io.File
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
 * Ablauf eines Laufs: erst holen, dann senden. Es gewinnt die jüngere Änderung; [Zustand.bekannt] merkt sich je Dokument den zuletzt
 * abgeglichenen Inhalt, daran ist zu erkennen, ob sich hier etwas geändert hat. Gelöscht wird nur auf ausdrückliche Meldung, nie
 * weil etwas auf einer Seite fehlt: Ein noch nicht gesendeter Eintrag darf nicht verschwinden.
 *
 * Angestoßen wird über das Signal des Servers, jede eigene Änderung und das Verbinden; alle 15 Minuten zur Sicherheit.
 * Der Lauf hat niedrige Priorität und steht still, solange ein Gespräch läuft, auch mitten in einer Übertragung (Sprachmodus vor Hintergrundarbeit).
 * Über das Datenvolumen wacht er bewusst nicht: Frank hat unbegrenzte mobile Daten.
 */
object AblageSync {
    private const val ZYKLUS_MS = 15 * 60_000L
    private const val NACH_FEHLER_MS = 60_000L
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
    /** Was der Server dauerhaft ablehnt (zu groß, ungültig): Bis zur nächsten Änderung oder zum Neustart nicht wieder senden, sonst liefe es jede Minute neu. */
    private val aufgegeben = HashSet<String>()
    /** Prüfsummen der Texte je Datei, solange Größe und Datum gleich bleiben: Jeder Lauf bräuchte sie sonst für alle Einträge neu. */
    private val textSummen = HashMap<String, Triple<Long, Long, String>>()

    /** Der Server hat das endgültig abgelehnt; ein neuer Versuch ändert daran nichts. */
    private class DauerFehler(meldung: String) : IOException(meldung)

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
                    ZYKLUS_MS
                } catch (e: Exception) {
                    Logbuch.netz("Ablage-Abgleich", "unterbrochen: ${e.message}")
                    fehlerFolge++
                    (NACH_FEHLER_MS shl (fehlerFolge - 1).coerceAtMost(4)).coerceAtMost(ZYKLUS_MS)
                }
            }
        }
        // Eigene Änderungen kurz sammeln: Eine Recherche legt oft Text und mehrere Dateien nacheinander ab.
        bereich.launch { AblageZentrale.speicher(app).stand.collectLatest { delay(2_000); anstossen() } }
        bereich.launch { Prompts.stand.collectLatest { delay(2_000); anstossen() } }
    }

    /** Solange ein Gespräch läuft, geht nichts über die Leitung. */
    private suspend fun gespraechAbwarten() { while (GespraechsFokus.stilleHinweise) delay(5_000) }

    /** Mitten in einer Übertragung: abbrechen, der nächste Lauf holt sie nach dem Gespräch nach. */
    private fun gespraechPruefen() { if (GespraechsFokus.stilleHinweise) throw IOException("Übertragung pausiert: Gespräch läuft") }

    private class Zustand(val datei: File) {
        var stand = 0L
        val bekannt = HashMap<String, String>()
        /** Zeit der zuletzt abgeglichenen Fassung je Dokument. */
        val zeiten = HashMap<String, Long>()
        init {
            runCatching {
                val j = JSONObject(datei.readText())
                stand = j.optLong("stand")
                j.optJSONObject("bekannt")?.let { b -> b.keys().forEach { bekannt[it] = b.optString(it) } }
                j.optJSONObject("zeiten")?.let { b -> b.keys().forEach { zeiten[it] = b.optLong(it) } }
            }
        }
        fun sichere() = AblageSpeicher.schreibeAtomar(datei, JSONObject().put("stand", stand).put("bekannt", JSONObject(bekannt as Map<*, *>)).put("zeiten", JSONObject(zeiten as Map<*, *>)).toString().toByteArray())
        /** Geht diese Uhr nach, gälte eine Änderung sonst als älter als der Stand, auf dem sie aufbaut. */
        fun zeitFuer(schluessel: String, geaendert: Long) = maxOf(geaendert, (zeiten[schluessel] ?: 0L) + 1).coerceAtLeast(1)
        fun merke(schluessel: String, abdruck: String, zeit: Long) { bekannt[schluessel] = abdruck; zeiten[schluessel] = zeit }
        fun vergiss(schluessel: String) { bekannt.remove(schluessel); zeiten.remove(schluessel) }
    }

    private suspend fun lauf(app: Context) {
        gespraechAbwarten()
        val e = Einstellungen.get(app)
        if (e.serverHost.isBlank() || e.geheimnis.isBlank()) return
        val basis = "https://${e.serverHost}/j/${e.geheimnis}/ablage"
        val speicher = AblageZentrale.speicher(app)
        val z = Zustand(File(app.filesDir, DATEI))
        val alleGeholt = holen(app, basis, speicher, z)
        if (alleGeholt) vollabgleich = false
        z.sichere()
        val allesGesendet = senden(app, basis, speicher, z)
        z.sichere()
        if (!alleGeholt || !allesGesendet) throw IOException("Noch nicht alles abgeglichen")
    }

    // ---- Holen ----

    private suspend fun holen(app: Context, basis: String, speicher: AblageSpeicher, z: Zustand): Boolean {
        val seit = if (vollabgleich) 0L else z.stand
        val antwort = http(Request.Builder().url("$basis/aenderungen?seit=$seit").build()).use { r ->
            if (!r.isSuccessful) throw IOException("Server antwortet mit ${r.code}")
            JSONObject(r.body?.string().orEmpty())
        }
        // Der Server hat weniger als wir je von ihm gesehen haben: Er wurde neu aufgesetzt. Dann muss der ganze Bestand wieder hoch.
        val neu = antwort.optLong("stand")
        if (neu < z.stand || antwort.optBoolean("neuaufbau")) {
            Logbuch.netz("Ablage-Abgleich", "Server wurde neu aufgesetzt, sende den ganzen Bestand")
            z.bekannt.clear()
            z.zeiten.clear()
            z.stand = 0
        }
        val docs = antwort.getJSONArray("docs")
        var alleGut = true
        val lokal = HashMap(speicher.eintraege().associateBy { it.id })
        for (i in 0 until docs.length()) {
            gespraechAbwarten()
            val d = docs.getJSONObject(i)
            try {
                when (d.optString("art")) {
                    "eintrag" -> eintragHolen(basis, speicher, z, d, lokal)
                    "prompt" -> promptHolen(app, z, d)
                    "ordnung" -> ordnungHolen(app, z, d)
                }
            } catch (f: Exception) {
                // Das Dokument bleibt offen: Der Stand rückt nicht vor, der nächste Lauf versucht es erneut.
                alleGut = false
                Logbuch.netz("Ablage-Abgleich", "${d.optString("art")} ${d.optString("id")}: ${f.message}")
            }
        }
        if (alleGut) z.stand = neu
        return alleGut
    }

    /** Darf der Stand vom Server den von hier ersetzen? Ja, wenn sich hier seit dem letzten Abgleich nichts getan hat oder er nicht älter ist. */
    private fun fernGewinnt(hierUnveraendert: Boolean, fernZeit: Long, lokalZeit: Long) = hierUnveraendert || fernZeit >= lokalZeit

    private fun eintragHolen(basis: String, speicher: AblageSpeicher, z: Zustand, d: JSONObject, karte: MutableMap<String, Eintrag>) {
        val id = sicher(d.getString("id"))
        val schluessel = "eintrag:$id"
        val geaendert = d.optLong("geaendert")
        val offen = speicher.offeneLoeschungen()[id]
        val lokal = karte[id]
        val lokalText = lokal?.let { textSha(speicher, it) }
        val lokalAbdruck = lokal?.let { abdruck(it, lokalText) }
        val hierUnveraendert = lokalAbdruck != null && z.bekannt[schluessel] == lokalAbdruck
        if (d.optBoolean("geloescht")) {
            if (offen != null) { if (offen <= geaendert) speicher.quittiereLoeschung(id); return }
            if (lokal == null) { z.vergiss(schluessel); return }
            if (fernGewinnt(hierUnveraendert, geaendert, lokal.geaendert)) {
                speicher.loescheEintrag(id, melden = false)
                karte.remove(id)
                z.vergiss(schluessel)
            }
            return
        }
        if (offen != null) { if (offen >= geaendert) return; speicher.quittiereLoeschung(id) }
        val w = d.getJSONObject("daten")
        val fern = eintragAus(w, id, geaendert)
        // Auf Android liefert optString bei JSON-null den Text „null“.
        val textFern = if (w.isNull("text_sha")) null else w.optString("text_sha").takeIf { it.isNotBlank() }
        val fernAbdruck = abdruck(fern, textFern)
        if (lokalAbdruck == fernAbdruck) { z.merke(schluessel, fernAbdruck, geaendert); return }
        if (lokal != null && !fernGewinnt(hierUnveraendert, geaendert, lokal.geaendert)) return

        // Ging das Verzeichnis hier verloren, trägt derselbe Text jetzt eine neue Kennung: Sie darf nicht als Doppel an alle gehen.
        if (lokal == null && textFern != null) {
            karte.values.firstOrNull { it.titel == fern.titel && it.anhaenge.isEmpty() && z.bekannt["eintrag:${it.id}"] == null && textSha(speicher, it) == textFern }?.let { doppelt ->
                speicher.loescheEintrag(doppelt.id, melden = false)
                karte.remove(doppelt.id)
            }
        }
        val textTeil = if (textFern != null && textFern != lokalText) laden(basis, textFern, speicher) else null
        val dateien = HashMap<String, File>()
        try {
            fern.anhaenge.filter { a -> lokal?.anhaenge?.none { it.id == a.id && it.sha256 == a.sha256 } != false }.forEach { a -> dateien[a.id] = laden(basis, a.sha256, speicher) }
            speicher.setzeFern(fern, textFern != null, textTeil, dateien)
        } finally {
            textTeil?.delete()
            dateien.values.forEach { it.delete() }
        }
        speicher.eintrag(id)?.let { karte[id] = it }
        z.merke(schluessel, fernAbdruck, geaendert)
    }

    private fun promptHolen(app: Context, z: Zustand, d: JSONObject) {
        val erstellt = d.getString("id").toLong()
        val schluessel = "prompt:$erstellt"
        val geaendert = d.optLong("geaendert")
        val lokal = Prompts.alle(app).firstOrNull { it.erstellt == erstellt }
        val offen = Prompts.offeneLoeschungen(app)[erstellt]
        val hierUnveraendert = lokal != null && z.bekannt[schluessel] == abdruck(lokal)
        if (d.optBoolean("geloescht")) {
            if (offen != null) { if (offen <= geaendert) Prompts.quittiereLoeschung(app, erstellt); return }
            if (lokal == null) { z.vergiss(schluessel); return }
            if (fernGewinnt(hierUnveraendert, geaendert, lokal.geaendert)) { Prompts.loescheFern(app, erstellt); z.vergiss(schluessel) }
            return
        }
        if (offen != null) { if (offen >= geaendert) return; Prompts.quittiereLoeschung(app, erstellt) }
        val w = d.getJSONObject("daten")
        val fern = Prompt(lokal?.nummer ?: 1, w.optString("text"), erstellt, w.optString("titel"), w.optString("quelle"), geaendert)
        if (fern.text.isEmpty()) return
        if (lokal != null && abdruck(lokal) == abdruck(fern)) { z.merke(schluessel, abdruck(fern), geaendert); return }
        if (lokal != null && !fernGewinnt(hierUnveraendert, geaendert, lokal.geaendert)) return
        if (Prompts.setzeFern(app, fern)) z.merke(schluessel, abdruck(fern), geaendert) else throw IOException("Prompt-Datei nicht schreibbar")
    }

    private fun ordnungHolen(app: Context, z: Zustand, d: JSONObject) {
        val zeit = d.optLong("geaendert")
        if (zeit > Prompts.ordnungZeit(app)) {
            val liste = d.getJSONObject("daten").optJSONArray("reihenfolge") ?: return
            if (Prompts.ordneFern(app, (0 until liste.length()).map { liste.optLong(it) }, zeit)) z.merke("ordnung:prompts", zeit.toString(), zeit)
        }
    }

    /** Lädt eine Datei in den Teil-Ordner und prüft sie; erst danach kommt sie in die Ablage. */
    private fun laden(basis: String, sha: String, speicher: AblageSpeicher): File {
        val teil = speicher.neueTeilDatei()
        try {
            http(Request.Builder().url("$basis/blob/${sicher(sha)}").build()).use { r ->
                if (!r.isSuccessful) throw IOException("Datei nicht abrufbar (${r.code})")
                teil.outputStream().use { aus ->
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
            if (AblageSpeicher.sha256(teil) != sha) throw IOException("Prüfsumme passt nicht")
            return teil
        } catch (e: Exception) {
            teil.delete()
            throw e
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
                Logbuch.netz("Ablage-Abgleich", "$was abgelehnt, wird nicht wiederholt: ${f.message}")
            } catch (f: Exception) {
                allesGut = false
                Logbuch.netz("Ablage-Abgleich", "$was: ${f.message}")
            }
        }
        /** Der Server hat eine gleich alte oder jüngere Fassung: Der nächste Lauf holt alles und klärt das nach der Regel „Jüngeres gewinnt“. */
        fun abgelehnt() { vollabgleich = true; allesGut = false }

        speicher.offeneLoeschungen().forEach { (id, zeit) ->
            val s = "eintrag:$id"
            versuch(s, "$s:weg") {
                val t = z.zeitFuer(s, zeit)
                if (put(basis, "eintrag", id, JSONObject(), t, geloescht = true)) { speicher.quittiereLoeschung(id); z.vergiss(s) } else abgelehnt()
            }
        }
        Prompts.offeneLoeschungen(app).forEach { (id, zeit) ->
            val s = "prompt:$id"
            versuch(s, "$s:weg") {
                val t = z.zeitFuer(s, zeit)
                if (put(basis, "prompt", id.toString(), JSONObject(), t, geloescht = true)) { Prompts.quittiereLoeschung(app, id); z.vergiss(s) } else abgelehnt()
            }
        }
        speicher.eintraege().forEach { e ->
            gespraechAbwarten()
            val s = "eintrag:${e.id}"
            val sha = textSha(speicher, e)
            val a = abdruck(e, sha)
            if (z.bekannt[s] == a) return@forEach
            versuch(s, "$s:$a") {
                val inhalte = HashMap<String, File>()
                if (sha != null) speicher.textDatei(e)?.let { inhalte[sha] = it }
                val mitSha = e.anhaenge.map { x -> if (x.sha256.isBlank()) x.copy(sha256 = AblageSpeicher.sha256(speicher.datei(x))) else x }
                mitSha.forEach { x -> inhalte[x.sha256] = speicher.datei(x) }
                val t = z.zeitFuer(s, e.geaendert)
                if (put(basis, "eintrag", e.id, eintragAlsDaten(speicher, e.copy(anhaenge = mitSha), sha), t, inhalte = inhalte)) z.merke(s, a, t) else abgelehnt()
            }
            z.sichere()
        }
        val prompts = Prompts.alle(app)
        prompts.forEach { p ->
            val s = "prompt:${p.erstellt}"
            val a = abdruck(p)
            if (z.bekannt[s] == a) return@forEach
            versuch(s, "$s:$a") {
                val daten = JSONObject().put("text", p.text).put("titel", p.titel).put("erstellt", p.erstellt).put("quelle", p.quelleName)
                val t = z.zeitFuer(s, p.geaendert)
                if (put(basis, "prompt", p.erstellt.toString(), daten, t)) z.merke(s, a, t) else abgelehnt()
            }
        }
        val zeit = Prompts.ordnungZeit(app).coerceAtLeast(1)
        if (z.bekannt["ordnung:prompts"] != zeit.toString() && prompts.isNotEmpty()) {
            versuch("Reihenfolge", "ordnung:$zeit") {
                val daten = JSONObject().put("reihenfolge", JSONArray(prompts.map { it.erstellt }))
                if (put(basis, "ordnung", "prompts", daten, zeit)) z.merke("ordnung:prompts", zeit.toString(), zeit) else abgelehnt()
            }
        }
        return allesGut
    }

    /** Sendet ein Dokument; fehlen dem Server dazu Dateien, kommen sie zuerst hoch. true = der Server hat diese Fassung übernommen. */
    private fun put(basis: String, art: String, id: String, daten: JSONObject, geaendert: Long, geloescht: Boolean = false, inhalte: Map<String, File> = emptyMap()): Boolean {
        val rumpf = JSONObject().put("daten", daten).put("geaendert", geaendert).put("geloescht", geloescht).put("von", AblageQuellen.HANDY).toString()
        fun versuch() = http(Request.Builder().url("$basis/doc/$art/$id").put(rumpf.toRequestBody(json)).build())
        fun ablehnung(code: Int): IOException = if (code in 400..499) DauerFehler("Server antwortet mit $code") else IOException("Server antwortet mit $code")
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
        val kopf = runCatching { speicher.textDatei(e)?.inputStream()?.use { String(it.readNBytes(40), Charsets.UTF_8) } }.getOrNull().orEmpty()
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
