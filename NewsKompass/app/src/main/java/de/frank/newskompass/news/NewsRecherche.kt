package de.frank.newskompass.news

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import de.frank.newskompass.ai.CodexClient
import de.frank.newskompass.ai.CodexFehler
import de.frank.newskompass.ai.CodexFehlerArt
import de.frank.newskompass.data.AusgabenSpeicher
import de.frank.newskompass.data.EinstellungenStand
import de.frank.newskompass.data.EinstellungenStore
import de.frank.newskompass.data.Geschichten
import de.frank.newskompass.data.model.Ausfuehrlichkeit
import de.frank.newskompass.data.model.Ausgabe
import de.frank.newskompass.data.model.BildModus
import de.frank.newskompass.data.model.Block
import de.frank.newskompass.data.model.Meldung
import de.frank.newskompass.data.model.Thema
import de.frank.newskompass.network.awaitAntwort
import de.frank.newskompass.observability.KompassLog
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/** Zwischenstand eines Laufs für die Anzeige. */
data class LaufFortschritt(val text: String, val anteil: Float)

/**
 * Ein Nachrichtenlauf: pro Thema eine Codex-Recherche mit Websuche, danach die Bilder.
 *
 * Die Themen laufen nacheinander, nie gleichzeitig — sonst läuft das Codex-Kontingent in
 * wenigen Sekunden gegen die Wand. Scheitert ein Thema, bekommt sein Block einen Hinweis;
 * die übrigen Blöcke erscheinen trotzdem. Ist das Kontingent erschöpft oder die Anmeldung
 * abgelaufen, bleiben die fertigen Blöcke erhalten und die übrigen Themen bekommen einen
 * Hinweis, statt den ganzen Lauf zu verwerfen.
 */
class NewsRecherche(
    private val codex: CodexClient,
    private val einstellungen: EinstellungenStore,
    private val speicher: AusgabenSpeicher,
) {

    private val netz = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun laufe(beiFortschritt: suspend (LaufFortschritt) -> Unit): Ausgabe = withContext(Dispatchers.IO) {
        val stand = einstellungen.stand.value
        val themen = stand.themen.filter { it.text.isNotBlank() }
        if (themen.isEmpty()) throw IllegalStateException("Es ist kein Nachrichtenthema eingetragen.")
        if (!codex.istVerbunden) throw CodexFehler(CodexFehlerArt.ANMELDUNG, "Bitte zuerst in den Einstellungen bei Codex anmelden.")

        val jetzt = System.currentTimeMillis()
        // Was in den letzten 36 Stunden schon lief, soll nicht noch einmal als neu erscheinen.
        val vorige = speicher.ausgabenSeit(jetzt - 36 * 3_600_000L)
        val bloecke = mutableListOf<Block>()
        val schritte = themen.size + 1

        // Kontingent erschöpft oder Anmeldung abgelaufen: Weitere Anfragen scheitern genauso.
        var harterFehler: CodexFehler? = null

        themen.forEachIndexed { nummer, thema ->
            val kurz = thema.text.take(40).let { if (thema.text.length > 40) "$it …" else it }
            harterFehler?.let { fehler ->
                bloecke += Block(thema.id, kurz, emptyList(), hinweisFuer(fehler))
                return@forEachIndexed
            }
            beiFortschritt(LaufFortschritt("Recherchiere Thema ${nummer + 1} von ${themen.size}: $kurz", nummer.toFloat() / schritte))
            val block = try {
                recherchiereThema(thema, stand.modellId, stand.denktiefe, stand.ausfuehrlichkeit, vorige, bloecke.flatMap { b -> b.meldungen.map { it.titel } }, jetzt)
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                KompassLog.error("NewsRecherche", "laufe", "Thema gescheitert", mapOf("thema" to kurz, "grund" to fehler.message))
                if (fehler is CodexFehler && fehler.art != CodexFehlerArt.NETZ) {
                    einstellungen.merkeHartenFehler(jetzt)
                    // Noch nichts fertig: nichts speichern, damit keine leere Ausgabe eine alte verdrängt.
                    if (bloecke.none { it.meldungen.isNotEmpty() }) throw fehler
                    harterFehler = fehler
                    Block(thema.id, kurz, emptyList(), hinweisFuer(fehler))
                } else {
                    Block(thema.id, kurz, emptyList(), fehler.message ?: "Die Recherche ist gescheitert.")
                }
            }
            bloecke += block
        }

        // Nach Kontingent- oder Anmeldefehler scheitern auch Illustrationen — nur noch Fotos suchen.
        val bildStand = if (harterFehler != null) stand.copy(bilderUnterstuetzt = false) else stand
        val bebildert = bebildere(bloecke, bildStand, vorige) { text -> beiFortschritt(LaufFortschritt(text, themen.size.toFloat() / schritte)) }
        vergiss(bloecke)
        val ausgabe = Ausgabe(
            id = "a$jetzt",
            erstelltUm = jetzt,
            slot = slotName(jetzt),
            bloecke = bebildert,
        )
        speicher.speichere(ausgabe)
        beiFortschritt(LaufFortschritt("Fertig", 1f))
        ausgabe
    }

    /**
     * Beantwortet eine Frage, die über den Mikrofon-Knopf gesprochen wurde.
     *
     * Die Frage wirkt wie ein Thema, das nur für diesen Moment gilt: Sie landet in keiner
     * Themenliste, sondern als eigener Block unten in der aktuellen Ausgabe. Liefert die
     * Ausgabe und den angehängten Block.
     */
    suspend fun beantworteFrage(frage: String, beiFortschritt: suspend (LaufFortschritt) -> Unit): Pair<Ausgabe, Block> = withContext(Dispatchers.IO) {
        val text = frage.trim()
        if (text.isEmpty()) throw IllegalStateException("Die Frage ist leer.")
        if (!codex.istVerbunden) throw CodexFehler(CodexFehlerArt.ANMELDUNG, "Bitte zuerst in den Einstellungen bei Codex anmelden.")

        val stand = einstellungen.stand.value
        val jetzt = System.currentTimeMillis()
        val thema = Thema("frage-$jetzt", text)
        // Was die aktuelle Ausgabe schon erzählt, soll der Frage-Block nicht wiederholen.
        val schonDa = speicher.neueste()?.bloecke.orEmpty().flatMap { b -> b.meldungen.map { it.titel } }
        beiFortschritt(LaufFortschritt("Recherchiere deine Frage …", 0.05f))
        val antwort = codex.frage(
            anweisung = anweisung(jetzt, Thema.FRAGE_MIN, Thema.FRAGE_MAX, stand.ausfuehrlichkeit, sprachFrage = true),
            eingabe = buildString {
                appendLine("Die gesprochene Frage des Nutzers:")
                appendLine(text)
                if (schonDa.isNotEmpty()) {
                    appendLine()
                    appendLine("Diese Meldungen stehen bereits in anderen Blöcken der aktuellen Ausgabe. Nimm sie nur auf, wenn sie die Frage direkt beantworten, und dann mit dem, was für die Frage wichtig ist:")
                    schonDa.forEach { appendLine("- $it") }
                }
            },
            modellId = stand.modellId,
            denktiefe = stand.denktiefe,
            werkzeuge = JSONArray().put(JSONObject().put("type", "web_search")),
        )
        val block = zerlege(thema, antwort.text, antwort.quellen, Thema.FRAGE_MAX).copy(frage = text)
        val vorige = speicher.ausgabenSeit(jetzt - 36 * 3_600_000L)
        val fertig = bebildere(listOf(block), stand, vorige) { text -> beiFortschritt(LaufFortschritt(text, 0.6f)) }.first()
        vergiss(listOf(block))

        val ausgabe = speicher.haengeAn(fertig, jetzt)
        beiFortschritt(LaufFortschritt("Fertig", 1f))
        ausgabe to fertig
    }

    // --- Recherche ---------------------------------------------------------------------------

    private suspend fun recherchiereThema(
        thema: Thema,
        modellId: String,
        denktiefe: String,
        ausfuehrlichkeit: Ausfuehrlichkeit,
        vorige: List<Ausgabe>,
        schonInDieserAusgabe: List<String>,
        jetzt: Long,
    ): Block {
        val bekannt = vorige.flatMap { a -> a.bloecke.filter { it.themaId == thema.id }.flatMap { b -> b.meldungen.map { it.titel } } }.distinct()
        val eingabe = buildString {
            appendLine("Thema, zu dem du die Nachrichten zusammenstellst:")
            appendLine(thema.text.trim())
            if (bekannt.isNotEmpty()) {
                appendLine()
                appendLine("Diese Meldungen standen schon in der letzten Ausgabe. Nimm sie nur wieder auf, wenn es wirklich Neues dazu gibt, und sag dann im Text, was neu ist:")
                bekannt.forEach { appendLine("- $it") }
            }
            if (schonInDieserAusgabe.isNotEmpty()) {
                appendLine()
                appendLine("Diese Meldungen stehen bereits in anderen Blöcken dieser Ausgabe. Wiederhole sie hier nicht:")
                schonInDieserAusgabe.forEach { appendLine("- $it") }
            }
        }
        val antwort = codex.frage(
            anweisung = anweisung(jetzt, thema.minMeldungen, thema.maxMeldungen, ausfuehrlichkeit),
            eingabe = eingabe,
            modellId = modellId,
            denktiefe = denktiefe,
            werkzeuge = JSONArray().put(JSONObject().put("type", "web_search")),
        )
        return zerlege(thema, antwort.text, antwort.quellen, thema.maxMeldungen)
    }

    private fun hinweisFuer(fehler: CodexFehler): String = when (fehler.art) {
        CodexFehlerArt.KONTINGENT -> "Das Codex-Kontingent ist gerade erschöpft. Dieses Thema kommt mit dem nächsten Lauf."
        CodexFehlerArt.ANMELDUNG -> "Die Codex-Anmeldung ist abgelaufen. Bitte in den Einstellungen neu anmelden; dieses Thema kommt dann mit dem nächsten Lauf."
        CodexFehlerArt.NETZ -> fehler.message ?: "Die Recherche ist gescheitert."
    }

    private fun anweisung(
        jetzt: Long,
        minMeldungen: Int,
        maxMeldungen: Int,
        ausfuehrlichkeit: Ausfuehrlichkeit,
        sprachFrage: Boolean = false,
    ): String {
        val datum = SimpleDateFormat("EEEE, d. MMMM yyyy", Locale.GERMANY).format(Date(jetzt))
        val uhr = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(jetzt))
        val auftrag = if (sprachFrage) {
            "Der Nutzer hat dir gerade per Sprache eine Frage gestellt oder ein Thema genannt, zu dem er jetzt sofort das Aktuelle wissen will. " +
                "Die Eingabe stammt aus einer Spracherkennung und kann Füllwörter, Versprecher oder falsch erkannte Wörter enthalten; erschließe, was gemeint ist. " +
                "Recherchiere mit der Websuche gründlich und mehrfach die neuesten Nachrichten dazu, vorrangig aus den letzten 24 bis 48 Stunden. " +
                "Fragt der Nutzer nach einem Stand, einer Entwicklung oder einem Hintergrund, beantworte genau das mit dem neuesten belegten Stand; ältere Fakten nur, wenn sie zum Verständnis nötig sind, und dann mit der Angabe, von wann sie stammen. " +
                "Bevorzuge Primärquellen und seriöse Medien. Nimm nur belegte Fakten auf, keine Gerüchte ohne Kennzeichnung, keine Spekulation."
        } else {
            "Recherchiere mit der Websuche die wichtigsten Neuigkeiten zum Thema des Nutzers aus den letzten 24 Stunden, höchstens aus den letzten 48 Stunden. " +
                "Suche gründlich und mehrfach, bevorzuge Primärquellen und seriöse Medien. Nimm nur belegte Fakten auf, keine Gerüchte ohne Kennzeichnung, keine Spekulation. " +
                "Ältere Meldungen nur, wenn heute etwas Neues dazu passiert ist, oder nach der Regel zum Auffüllen unten."
        }
        val anzahl = when {
            sprachFrage -> "Liefere $minMeldungen bis $maxMeldungen Meldungen, genau so viele, wie die Frage wirklich braucht; niemals mehr als $maxMeldungen."
            else -> {
                val spanne = if (minMeldungen == maxMeldungen) "genau $maxMeldungen Meldungen" else "mindestens $minMeldungen und höchstens $maxMeldungen Meldungen"
                "Liefere $spanne; mehr als $maxMeldungen sind nicht erlaubt. " +
                    "Gibt es aus den letzten 48 Stunden weniger als $minMeldungen wirklich neue, belegte Meldungen, darfst du mit relevanten, belegten Meldungen aus den letzten sieben Tagen auffüllen und nennst dann in \"wann\" ehrlich, wann es passiert ist, etwa „am Montag“ oder „vor fünf Tagen“. " +
                    "Fülle niemals mit Erfundenem, mit Gerüchten, mit Wiederholungen oder mit Meldungen, die unten als bereits bekannt stehen; reicht es auch so nicht, liefere lieber weniger als $minMeldungen."
            }
        }
        val blockTitel = if (sprachFrage) "kurzer deutscher Titel für die Frage" else "kurzer deutscher Titel für dieses Thema"
        val briefing = if (sprachFrage) "das dem Leser vorgelesen wird" else "das zweimal am Tag erscheint und dem Leser vorgelesen wird"
        return """
            Du bist Redakteur eines deutschsprachigen Nachrichtenbriefings, $briefing.
            Heute ist $datum, es ist $uhr Uhr deutscher Zeit.

            $auftrag

            Arbeite wie eine gute Nachrichtenredaktion: Sortiere nach Relevanz und Tragweite, nicht nach der Reihenfolge der Suchtreffer. Das Wichtigste des Tages steht oben. Fasse mehrere Berichte über dasselbe Ereignis zu einer Meldung zusammen. $anzahl Jede Meldung ist eigenständig und behandelt genau ein Ereignis. Bei widersprüchlichen Angaben nenne, wer was sagt.

            So schreibst du, damit es sich gut vorlesen lässt:
            - Deutsch mit echten Umlauten und ß, niemals ae, oe, ue oder ss als Ersatz.
            - Ganze, klare Sätze in natürlicher Sprechsprache. Keine Aufzählungszeichen, kein Markdown, keine Emojis, keine Internetadressen, keine Quellenangaben in Klammern, keine Fußnoten.
            - Zahlen so, wie man sie spricht: „rund zwei Milliarden Dollar“, „fünf Prozent“, „am Dienstag“. Keine Zeichen wie %, €, $, &, / oder ~ im Text.
            - Abkürzungen nur, wenn man sie gesprochen versteht, etwa KI oder EU; sonst ausschreiben.
            - ${ausfuehrlichkeit.regel}
            - Die Überschrift ist kurz, höchstens 70 Zeichen, ohne Punkt am Ende.

            Antworte ausschließlich mit einem JSON-Objekt, ohne Text davor oder danach, in genau dieser Form:
            {"blockTitel": "$blockTitel, höchstens drei Wörter",
             "meldungen": [{"titel": "...", "absaetze": ["...", "..."], "quellen": ["https://..."], "wann": "sprechbare Zeitangabe wie heute früh, gestern Abend oder am Mittwoch", "update": false, "bildIdee": "one English sentence describing a fitting editorial illustration, no text, no logos"}]}
            In "quellen" stehen 2 bis 4 Adressen der Artikel, die du für genau diese Meldung tatsächlich gelesen und genutzt hast, die beste zuerst: konkrete Artikelseiten von Medien oder Primärquellen, keine Startseiten, Übersichts- oder Suchseiten; bei gleichwertigen Quellen zuerst die, die ein Foto zum Ereignis zeigt. "update" ist true, wenn die Meldung eine Fortsetzung einer bereits bekannten Meldung ist.
        """.trimIndent()
    }

    /** [maxMeldungen] ist hart: Liefert Codex mehr, fällt der Rest weg — die wichtigsten stehen oben. */
    private fun zerlege(thema: Thema, text: String, suchQuellen: List<String>, maxMeldungen: Int): Block {
        val json = runCatching { JSONObject(schneideJson(text)) }.getOrNull()
        if (json == null) {
            KompassLog.warn("NewsRecherche", "zerlege", "Antwort war kein JSON, nehme den Text roh", mapOf("zeichen" to text.length))
            val absaetze = text.split(Regex("\n{2,}")).map(String::trim).filter(String::isNotBlank)
            return Block(
                thema.id,
                thema.text.take(30),
                listOf(Meldung(UUID.randomUUID().toString(), "Überblick", absaetze, suchQuellen.take(5), null, false)),
                null,
            )
        }
        val liste = json.optJSONArray("meldungen") ?: JSONArray()
        val meldungen = (0 until liste.length()).mapNotNull { index ->
            val m = liste.optJSONObject(index) ?: return@mapNotNull null
            val absaetze = m.optJSONArray("absaetze")?.let { a -> (0 until a.length()).map { a.optString(it).trim() } }
                ?.filter(String::isNotBlank).orEmpty()
            if (absaetze.isEmpty()) return@mapNotNull null
            val quellen = m.optJSONArray("quellen")?.let { a -> (0 until a.length()).map { a.optString(it).trim() } }
                ?.filter { it.startsWith("http") }.orEmpty()
            Meldung(
                id = UUID.randomUUID().toString(),
                titel = m.optString("titel").trim().trimEnd('.'),
                absaetze = absaetze,
                quellen = quellen.ifEmpty { suchQuellen.take(3) },
                bildDatei = null,
                bildIstKi = false,
                wann = m.optString("wann").trim(),
                istUpdate = m.optBoolean("update"),
            ).let { Triple(it, m.optString("bildIdee"), quellen) }
        }.take(maxMeldungen).map { (meldung, idee, eigene) ->
            bildIdeen[meldung.id] = idee.ifBlank { meldung.titel }
            // Globale Websuch-Zitate sind hier nur Anzeige-Ersatz, nie Bildquelle einer Meldung.
            eigeneQuellen[meldung.id] = eigene
            meldung
        }
        val titel = json.optString("blockTitel").trim().ifBlank { thema.text.take(30) }
        return Block(thema.id, titel, meldungen, if (meldungen.isEmpty()) "Zu diesem Thema kam heute nichts Neues." else null)
    }

    // Nebenläufig: Eine gesprochene Frage kann parallel zu einem Lauf aus dem Zeitplan recherchiert werden.
    private val bildIdeen = ConcurrentHashMap<String, String>()

    /** Nur die Quellen, die Codex genau dieser Meldung zugeordnet hat — nur dort wird nach Fotos gesucht. */
    private val eigeneQuellen = ConcurrentHashMap<String, List<String>>()

    private fun vergiss(bloecke: List<Block>) = bloecke.forEach { b ->
        b.meldungen.forEach {
            bildIdeen.remove(it.id)
            eigeneQuellen.remove(it.id)
        }
    }

    // --- Bilder ------------------------------------------------------------------------------

    /**
     * Bebildert die Blöcke in zwei Durchgängen: erst echte Fotos für alle Meldungen, dann bekommen
     * die übrigen KI-Illustrationen aus dem Budget — reihum nach Rang über alle Themen, damit nicht
     * das erste Thema alles verbraucht. Am Ende steht die Bildbilanz (nur Zählwerte) im Logbuch.
     */
    private suspend fun bebildere(
        bloecke: List<Block>,
        stand: EinstellungenStand,
        vorige: List<Ausgabe>,
        beiFortschritt: suspend (String) -> Unit,
    ): List<Block> {
        val modus = stand.bildModus
        if (modus == BildModus.KEINE || bloecke.none { it.meldungen.isNotEmpty() }) return bloecke
        val statistik = BildStatistik()
        val register = FotoRegister(vorige)
        var ergebnis = bloecke
        if (modus == BildModus.FOTO_SONST_KI || modus == BildModus.NUR_FOTO) {
            ergebnis = ergebnis.map { block ->
                if (block.meldungen.isEmpty()) block else {
                    beiFortschritt("Suche Fotos für „${block.titel}“ …")
                    fotosFuer(block, register, statistik)
                }
            }
        }
        if (modus == BildModus.FOTO_SONST_KI || modus == BildModus.NUR_KI) {
            val kiBudget = intArrayOf(if (stand.bilderUnterstuetzt) stand.maxKiBilder else 0)
            ergebnis = illustriere(ergebnis, stand.modellId, kiBudget, statistik, beiFortschritt)
        } else {
            ergebnis.forEach { b -> b.meldungen.filter { it.bildDatei == null }.forEach { statistik.zaehle("ohne_bild") } }
        }
        KompassLog.info(
            "NewsRecherche",
            "bebildere",
            "Bildbilanz",
            statistik.alsMap() + mapOf("meldungen" to ergebnis.sumOf { it.meldungen.size }, "modus" to modus.id),
        )
        return ergebnis
    }

    /** Sucht für jede Meldung eines Blocks parallel ein Foto aus ihren eigenen Quellen. */
    private suspend fun fotosFuer(block: Block, register: FotoRegister, statistik: BildStatistik): Block = coroutineScope {
        val fotos = block.meldungen.map { m -> async { m.id to holeQuellenFoto(m, register, statistik) } }.awaitAll().toMap()
        block.copy(meldungen = block.meldungen.map { m -> fotos[m.id]?.let { m.copy(bildDatei = it, bildIstKi = false) } ?: m })
    }

    /** Zweiter Durchgang: Meldungen ohne Foto bekommen Illustrationen, erst Rang 1 aller Themen, dann Rang 2 … */
    private suspend fun illustriere(
        bloecke: List<Block>,
        modellId: String,
        kiBudget: IntArray,
        statistik: BildStatistik,
        beiFortschritt: suspend (String) -> Unit,
    ): List<Block> {
        val offen = bloecke.flatMapIndexed { blockNr, b ->
            b.meldungen.mapIndexedNotNull { rang, m -> if (m.bildDatei == null) Triple(rang, blockNr, m) else null }
        }.sortedWith(compareBy({ it.first }, { it.second }))
        val neu = HashMap<String, String>()
        offen.forEach { (_, blockNr, m) ->
            if (kiBudget[0] <= 0) {
                statistik.zaehle("ohne_bild")
                return@forEach
            }
            beiFortschritt("Male eine Illustration für „${bloecke[blockNr].titel}“ …")
            val ki = erzeugeKiBild(bildIdeen[m.id] ?: m.titel, bloecke[blockNr].titel, modellId, kiBudget)
            if (ki != null) {
                neu[m.id] = ki
                statistik.zaehle("ki")
            } else {
                statistik.zaehle("ki_fehler")
            }
        }
        return bloecke.map { b -> b.copy(meldungen = b.meldungen.map { m -> neu[m.id]?.let { m.copy(bildDatei = it, bildIstKi = true) } ?: m }) }
    }

    private suspend fun erzeugeKiBild(idee: String, bereich: String, modellId: String, kiBudget: IntArray): String? {
        kiBudget[0] -= 1
        return try {
            val antwort = codex.frage(
                anweisung = "Du erzeugst Bilder für eine Nachrichten-App. Rufe das Bildwerkzeug genau einmal auf und antworte danach nur mit OK.",
                eingabe = "Create one high quality editorial illustration in 16:9 landscape format for a news story " +
                    "in the section \"$bereich\": $idee. Modern, rich colors, cinematic lighting, clean composition. " +
                    "Absolutely no text, letters, captions, watermarks or logos in the image.",
                modellId = modellId,
                denktiefe = "low",
                werkzeuge = JSONArray().put(JSONObject().put("type", "image_generation").put("output_format", "png")),
            )
            val roh = antwort.bilderBase64.firstOrNull() ?: run {
                KompassLog.warn("NewsRecherche", "erzeugeKiBild", "Kein Bild geliefert")
                return null
            }
            kodiere(Base64.decode(roh, Base64.DEFAULT))?.let(::schreibeBild)
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.warn("NewsRecherche", "erzeugeKiBild", "Bilderzeugung gescheitert", mapOf("grund" to fehler.message))
            // Nur wenn der Dienst das Bildwerkzeug selbst ablehnt, alle weiteren Versuche sparen —
            // ein beliebiger anderer Fehler 400 sperrt die Illustrationen nicht mehr dauerhaft.
            if (werkzeugNichtUnterstuetzt(fehler.message.orEmpty())) {
                einstellungen.setzeBilderUnterstuetzt(false)
                kiBudget[0] = 0
            }
            null
        }
    }

    private enum class Guete { QUER, HOCH }

    private class Kandidat(val jpeg: ByteArray, val hash: String, val guete: Guete)

    /**
     * Probiert die eigenen Quellen der Meldung der Reihe nach, pro Artikelseite mehrere
     * Bildkandidaten. Ein gutes Querformat gewinnt sofort; ein hochauflösendes Hochformat bleibt
     * als Reserve, falls kein Querformat kommt — lieber ein echtes Foto als eine Illustration.
     */
    private suspend fun holeQuellenFoto(m: Meldung, register: FotoRegister, statistik: BildStatistik): String? {
        val quellen = eigeneQuellen[m.id].orEmpty()
        if (quellen.isEmpty()) {
            statistik.zaehle("ohne_eigene_quelle")
            return null
        }
        // Legt einen Kandidaten fest: ins Register eintragen, dann speichern. Scheitert eins davon —
        // eine parallele Meldung war schneller oder das Schreiben klappt nicht —, kommt der nächste dran.
        fun sichere(k: Kandidat): String? {
            if (!register.nimm(k.hash, m)) {
                statistik.zaehle("generisch_doppelt")
                return null
            }
            val name = try {
                schreibeBild(k.jpeg)
            } catch (fehler: Exception) {
                register.gib(k.hash, m)
                statistik.zaehle("schreibfehler")
                KompassLog.warn("NewsRecherche", "holeQuellenFoto", "Foto nicht gespeichert", mapOf("grund" to fehler.javaClass.simpleName))
                return null
            }
            statistik.zaehle(if (k.guete == Guete.QUER) "foto_quer" else "foto_hoch")
            return name
        }

        val reserven = mutableListOf<Kandidat>()
        var versuche = 0
        val gefunden = withTimeoutOrNull(FOTO_ZEIT_MS) {
            for (seite in quellen.take(MAX_SEITEN)) {
                val kandidaten = try {
                    bildKandidaten(seite, statistik)
                } catch (abbruch: CancellationException) {
                    throw abbruch
                } catch (fehler: Exception) {
                    statistik.zaehle("seite_fehler")
                    emptyList()
                }
                for (adresse in kandidaten) {
                    if (versuche++ >= MAX_BILDVERSUCHE) return@withTimeoutOrNull null
                    val k = try {
                        pruefeBild(adresse, statistik)
                    } catch (abbruch: CancellationException) {
                        throw abbruch
                    } catch (fehler: Exception) {
                        statistik.zaehle("bild_fehler")
                        null
                    } ?: continue
                    if (!register.frei(k.hash, m)) {
                        statistik.zaehle("generisch_doppelt")
                        continue
                    }
                    if (k.guete == Guete.QUER) {
                        sichere(k)?.let { return@withTimeoutOrNull it }
                        continue
                    }
                    if (reserven.size < MAX_RESERVEN) reserven += k
                }
            }
            null
        }
        if (gefunden != null) return gefunden
        // Kein Querformat: die Hochformat-Reserven der Reihe nach, bevor die KI zum Zug kommt.
        reserven.forEach { k -> sichere(k)?.let { return it } }
        return null
    }

    /**
     * Bildadressen einer Artikelseite, die besten zuerst: Open Graph, Twitter, itemprop, JSON-LD
     * und image_src. Startseiten zählen nicht — ihr Bild zeigt nie das Ereignis.
     */
    private suspend fun bildKandidaten(seite: String, statistik: BildStatistik): List<String> {
        val basis = seite.toHttpUrlOrNull() ?: return emptyList()
        if (basis.encodedPath.trimEnd('/').isEmpty()) {
            statistik.zaehle("startseite")
            return emptyList()
        }
        val html = ladeSeite(basis) ?: run {
            statistik.zaehle("seite_http")
            return emptyList()
        }
        val meta = META_REGEX.findAll(html).mapNotNull { tag ->
            val t = tag.value
            val art = ART_REGEX.find(t)?.groupValues?.get(1)?.lowercase() ?: return@mapNotNull null
            val inhalt = INHALT_REGEX.find(t)?.groupValues?.get(1) ?: return@mapNotNull null
            if (art in BILD_ARTEN) art to inhalt else null
        }.toList()
        val roh = mutableListOf<String>()
        BILD_ARTEN.forEach { art -> meta.filter { it.first == art }.forEach { roh += it.second } }
        LD_JSON_REGEX.findAll(html).forEach { skript ->
            val inhalt = skript.groupValues[1]
            LD_BILD_REGEX.findAll(inhalt).forEach { roh += it.groupValues[1] }
            LD_BILD_OBJEKT_REGEX.findAll(inhalt).forEach { roh += it.groupValues[1] }
        }
        LINK_REGEX.findAll(html).filter { IMAGE_SRC_REGEX.containsMatchIn(it.value) }.forEach { link ->
            HREF_REGEX.find(link.value)?.groupValues?.get(1)?.let { roh += it }
        }
        val adressen = roh.map(::entschaerfe).mapNotNull { basis.resolve(it)?.toString() }.filter { it.startsWith("http") }.distinct()
        if (adressen.isEmpty()) statistik.zaehle("seite_ohne_bild")
        return adressen.filter { adresse ->
            // Nur der Dateiname zählt: Drupal legt echte Fotos unter /sites/default/files/ ab.
            val datei = adresse.toHttpUrlOrNull()?.pathSegments?.lastOrNull { it.isNotBlank() }.orEmpty()
            val logo = LOGO_REGEX.containsMatchIn(datei)
            if (logo) statistik.zaehle("logo_platzhalter")
            !logo
        }.take(MAX_JE_SEITE)
    }

    /** Liest die Seite bis zum Kopf — reicht der nicht, weiter bis [MAX_HTML] (JSON-LD steht oft im Body). */
    private suspend fun ladeSeite(basis: HttpUrl): String? {
        val anfrage = Request.Builder().url(basis).header("User-Agent", BROWSER).header("Accept", "text/html").build()
        return netz.newCall(anfrage).awaitAntwort().use { antwort ->
            if (!antwort.isSuccessful) return null
            val strom = antwort.body?.byteStream() ?: return null
            val puffer = ByteArray(MAX_HTML)
            var gelesen = 0
            var kopfEnde = -1
            while (gelesen < MAX_HTML) {
                val n = strom.read(puffer, gelesen, MAX_HTML - gelesen)
                if (n <= 0) break
                // Nur das neue Stück (mit etwas Überlappung) nach dem Kopfende durchsuchen.
                val von = maxOf(0, gelesen - 8)
                gelesen += n
                if (kopfEnde < 0) {
                    val stueck = String(puffer, von, gelesen - von, Charsets.ISO_8859_1)
                    val treffer = stueck.indexOf("</head>", ignoreCase = true)
                    if (treffer >= 0) kopfEnde = von + treffer
                }
                if (kopfEnde >= 0) {
                    val kopf = String(puffer, 0, kopfEnde, Charsets.ISO_8859_1)
                    if (kopf.contains("og:image", ignoreCase = true) || kopf.contains("twitter:image", ignoreCase = true)) break
                }
            }
            String(puffer, 0, gelesen, Charsets.UTF_8)
        }
    }

    /**
     * Lädt einen Kandidaten und prüft ihn: echtes Rasterbild (kein SVG, GIF oder Icon), groß genug,
     * sinnvolles Seitenverhältnis. Querformat ab 600 × 300 ist erste Wahl, Hochformat oder
     * quadratisch ab 800 px Höhe Reserve.
     */
    private suspend fun pruefeBild(adresse: String, statistik: BildStatistik): Kandidat? {
        val url = adresse.toHttpUrlOrNull() ?: return null
        val anfrage = Request.Builder().url(url).header("User-Agent", BROWSER)
            .header("Accept", "image/webp,image/jpeg,image/png,image/*;q=0.8").build()
        val bytes = netz.newCall(anfrage).awaitAntwort().use { antwort ->
            if (!antwort.isSuccessful) {
                statistik.zaehle("bild_http")
                return null
            }
            val typ = antwort.header("Content-Type").orEmpty().lowercase()
            if (typ.isNotBlank() && !typ.startsWith("image")) {
                statistik.zaehle("kein_bild")
                return null
            }
            if ("svg" in typ || "gif" in typ || "icon" in typ) {
                statistik.zaehle("format")
                return null
            }
            if ((antwort.body?.contentLength() ?: -1L) > MAX_BILD_BYTES) {
                statistik.zaehle("zu_gross")
                return null
            }
            antwort.body?.bytes()
        } ?: return null
        if (bytes.size > MAX_BILD_BYTES) {
            statistik.zaehle("zu_gross")
            return null
        }
        if (bytes.size >= 4 && String(bytes, 0, 4, Charsets.ISO_8859_1) == "GIF8") {
            statistik.zaehle("format")
            return null
        }
        val masse = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, masse)
        val breite = masse.outWidth
        val hoehe = masse.outHeight
        if (breite <= 0 || hoehe <= 0) {
            statistik.zaehle("format")
            return null
        }
        val verhaeltnis = breite.toFloat() / hoehe
        val guete = when {
            verhaeltnis in 1.2f..2.4f && breite >= 600 && hoehe >= 300 -> Guete.QUER
            verhaeltnis >= 0.5f && verhaeltnis < 1.2f && hoehe >= 800 && breite >= 450 -> Guete.HOCH
            verhaeltnis > 2.4f || verhaeltnis < 0.5f -> {
                statistik.zaehle("seitenverhaeltnis")
                return null
            }
            else -> {
                statistik.zaehle("zu_klein")
                return null
            }
        }
        val jpeg = kodiere(bytes) ?: run {
            statistik.zaehle("format")
            return null
        }
        return Kandidat(jpeg, sha256(jpeg), guete)
    }

    /** Verkleinert auf höchstens 1440 Pixel Breite und liefert das Bild als JPEG. */
    private fun kodiere(bytes: ByteArray): ByteArray? {
        val masse = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, masse)
        if (masse.outWidth <= 0) return null
        var teiler = 1
        while (masse.outWidth / (teiler * 2) >= ZIEL_BREITE) teiler *= 2
        val bild = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = teiler })
            ?: return null
        val skaliert = if (bild.width > ZIEL_BREITE) {
            Bitmap.createScaledBitmap(bild, ZIEL_BREITE, (bild.height * ZIEL_BREITE.toFloat() / bild.width).toInt(), true)
        } else {
            bild
        }
        val aus = ByteArrayOutputStream()
        skaliert.compress(Bitmap.CompressFormat.JPEG, 86, aus)
        if (skaliert !== bild) skaliert.recycle()
        bild.recycle()
        return aus.toByteArray()
    }

    private fun schreibeBild(jpeg: ByteArray): String {
        val name = "${UUID.randomUUID()}.jpg"
        File(speicher.bilderOrdner, name).writeBytes(jpeg)
        return name
    }

    /**
     * Fotos dieses Laufs und der letzten 36 Stunden. Taucht ein byte-gleiches Bild bei einer
     * anderen Geschichte auf, ist es ein generisches Seitenbild (Logo, Standard-Vorschau); bei einer
     * Fortsetzung derselben Geschichte darf es wieder erscheinen.
     */
    private inner class FotoRegister(vorige: List<Ausgabe>) {
        private inner class Spur(val hash: String, val titel: String, val quelle: String?, val update: Boolean)

        private val spuren = mutableListOf<Spur>()

        init {
            vorige.forEach { a ->
                a.bloecke.forEach { b ->
                    b.meldungen.filter { it.bildDatei != null && !it.bildIstKi }.forEach { m ->
                        val datei = speicher.bildDatei(m.bildDatei) ?: return@forEach
                        runCatching { spuren += Spur(sha256(datei.readBytes()), m.titel, m.quellen.firstOrNull(), m.istUpdate) }
                    }
                }
            }
        }

        @Synchronized
        fun frei(hash: String, m: Meldung): Boolean = spuren.none {
            it.hash == hash && !Geschichten.gleich(it.titel, it.quelle, it.update, m.titel, m.quellen.firstOrNull(), m.istUpdate)
        }

        @Synchronized
        fun nimm(hash: String, m: Meldung): Boolean {
            if (!frei(hash, m)) return false
            spuren += Spur(hash, m.titel, m.quellen.firstOrNull(), m.istUpdate)
            return true
        }

        /** Gibt einen Eintrag wieder frei, wenn das Bild doch nicht gespeichert werden konnte. */
        @Synchronized
        fun gib(hash: String, m: Meldung) {
            val stelle = spuren.indexOfLast { it.hash == hash && it.titel == m.titel }
            if (stelle >= 0) spuren.removeAt(stelle)
        }
    }

    /** Zählt, warum Bilder genommen oder verworfen wurden — nur Zahlen, keine Adressen oder Texte. */
    private class BildStatistik {
        private val zaehler = ConcurrentHashMap<String, AtomicInteger>()
        fun zaehle(grund: String) {
            zaehler.computeIfAbsent(grund) { AtomicInteger() }.incrementAndGet()
        }
        fun alsMap(): Map<String, Any?> = zaehler.toSortedMap().mapValues { it.value.get() }
    }

    companion object {
        private const val MAX_HTML = 400_000
        private const val MAX_BILD_BYTES = 8_000_000
        private const val ZIEL_BREITE = 1440
        private const val BROWSER =
            "Mozilla/5.0 (Linux; Android 16; SM-F971B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Mobile Safari/537.36"
        private const val FOTO_ZEIT_MS = 45_000L
        private const val MAX_SEITEN = 4
        private const val MAX_JE_SEITE = 4
        private const val MAX_BILDVERSUCHE = 8
        private const val MAX_RESERVEN = 3
        private val BILD_ARTEN = listOf("og:image:secure_url", "og:image", "og:image:url", "twitter:image", "twitter:image:src", "image")
        private val META_REGEX = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
        private val ART_REGEX = Regex("(?:property|name|itemprop)\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        private val LINK_REGEX = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
        private val IMAGE_SRC_REGEX = Regex("rel\\s*=\\s*[\"']image_src[\"']", RegexOption.IGNORE_CASE)
        private val HREF_REGEX = Regex("href\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        private val LD_JSON_REGEX = Regex("<script[^>]*application/ld\\+json[^>]*>(.*?)</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        private val LD_BILD_REGEX = Regex("\"(?:image|thumbnailUrl)\"\\s*:\\s*\\[?\\s*\"(https?:[^\"]+)\"")
        private val LD_BILD_OBJEKT_REGEX = Regex("\"image\"\\s*:\\s*\\[?\\s*\\{[^{}]*?\"(?:url|contentUrl)\"\\s*:\\s*\"(https?:[^\"]+)\"")
        private val LOGO_REGEX = Regex("(?:^|[/_.-])(?:logo|logos|icon|icons|favicon|placeholder|default|sprite|avatar|blank|spacer)(?:[/_.-]|$)", RegexOption.IGNORE_CASE)
        private val INHALT_REGEX = Regex("content\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)

        private fun entschaerfe(adresse: String): String = adresse
            .replace("\\/", "/").replace("\\u002F", "/").replace("&amp;", "&").replace("&#x2F;", "/").replace("&#47;", "/")
            .replace("&quot;", "").trim()

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        /** Lehnt der Dienst das Bildwerkzeug selbst ab — nicht irgendein anderer Fehler 400? */
        fun werkzeugNichtUnterstuetzt(meldung: String): Boolean =
            meldung.contains("image_generation", ignoreCase = true) &&
                listOf("not supported", "unsupported", "not available", "not enabled", "not allowed", "unknown", "invalid").any { meldung.contains(it, ignoreCase = true) }

        /** Schneidet Code-Zäune und Vorreden weg: vom ersten { bis zur letzten }. */
        fun schneideJson(text: String): String {
            val anfang = text.indexOf('{')
            val ende = text.lastIndexOf('}')
            return if (anfang >= 0 && ende > anfang) text.substring(anfang, ende + 1) else text
        }

        /** Morgen- oder Abendausgabe, nach der Uhrzeit des Laufs. */
        fun slotName(zeit: Long): String {
            val stunde = Calendar.getInstance().apply { timeInMillis = zeit }.get(Calendar.HOUR_OF_DAY)
            return when (stunde) {
                in 3..10 -> "Morgenausgabe"
                in 11..15 -> "Mittagsausgabe"
                else -> "Abendausgabe"
            }
        }
    }
}
