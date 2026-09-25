package de.frank.newskompass.news

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import de.frank.newskompass.ai.CodexClient
import de.frank.newskompass.ai.CodexFehler
import de.frank.newskompass.ai.CodexFehlerArt
import de.frank.newskompass.data.AusgabenSpeicher
import de.frank.newskompass.data.EinstellungenStore
import de.frank.newskompass.data.model.Ausgabe
import de.frank.newskompass.data.model.BildModus
import de.frank.newskompass.data.model.Block
import de.frank.newskompass.data.model.Meldung
import de.frank.newskompass.data.model.Thema
import de.frank.newskompass.network.awaitAntwort
import de.frank.newskompass.observability.KompassLog
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
 * die übrigen Blöcke erscheinen trotzdem.
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
        val vorige = speicher.ausgaben.value.filter { jetzt - it.erstelltUm < 36 * 3_600_000L }
        val kiBudget = intArrayOf(if (stand.bilderUnterstuetzt) stand.maxKiBilder else 0)
        val bloecke = mutableListOf<Block>()
        val schritte = themen.size * 2

        themen.forEachIndexed { nummer, thema ->
            val kurz = thema.text.take(40).let { if (thema.text.length > 40) "$it …" else it }
            beiFortschritt(LaufFortschritt("Recherchiere Thema ${nummer + 1} von ${themen.size}: $kurz", (nummer * 2f) / schritte))
            val block = try {
                recherchiereThema(thema, stand.modellId, stand.denktiefe, vorige, bloecke.flatMap { b -> b.meldungen.map { it.titel } }, jetzt)
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                KompassLog.error("NewsRecherche", "laufe", "Thema gescheitert", mapOf("thema" to kurz, "grund" to fehler.message))
                if (fehler is CodexFehler && fehler.art != CodexFehlerArt.NETZ) throw fehler
                Block(thema.id, kurz, emptyList(), fehler.message ?: "Die Recherche ist gescheitert.")
            }
            beiFortschritt(LaufFortschritt("Suche Bilder für „${block.titel}“ …", (nummer * 2f + 1) / schritte))
            bloecke += if (stand.bildModus == BildModus.KEINE) block else bebildere(block, stand.bildModus, stand.modellId, kiBudget)
        }

        val ausgabe = Ausgabe(
            id = "a$jetzt",
            erstelltUm = jetzt,
            slot = slotName(jetzt),
            bloecke = bloecke,
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
        val schonDa = speicher.ausgaben.value.firstOrNull()?.bloecke.orEmpty().flatMap { b -> b.meldungen.map { it.titel } }
        beiFortschritt(LaufFortschritt("Recherchiere deine Frage …", 0.05f))
        val antwort = codex.frage(
            anweisung = anweisung(jetzt, sprachFrage = true),
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
        val block = zerlege(thema, antwort.text, antwort.quellen).copy(frage = text)
        beiFortschritt(LaufFortschritt("Suche Bilder für „${block.titel}“ …", 0.6f))
        val kiBudget = intArrayOf(if (stand.bilderUnterstuetzt) stand.maxKiBilder else 0)
        val fertig = if (stand.bildModus == BildModus.KEINE) block else bebildere(block, stand.bildModus, stand.modellId, kiBudget)

        val ausgabe = speicher.haengeAn(fertig, jetzt)
        beiFortschritt(LaufFortschritt("Fertig", 1f))
        ausgabe to fertig
    }

    // --- Recherche ---------------------------------------------------------------------------

    private suspend fun recherchiereThema(
        thema: Thema,
        modellId: String,
        denktiefe: String,
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
            anweisung = anweisung(jetzt),
            eingabe = eingabe,
            modellId = modellId,
            denktiefe = denktiefe,
            werkzeuge = JSONArray().put(JSONObject().put("type", "web_search")),
        )
        return zerlege(thema, antwort.text, antwort.quellen)
    }

    private fun anweisung(jetzt: Long, sprachFrage: Boolean = false): String {
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
                "Ältere Meldungen nur, wenn heute etwas Neues dazu passiert ist."
        }
        val blockTitel = if (sprachFrage) "kurzer deutscher Titel für die Frage" else "kurzer deutscher Titel für dieses Thema"
        val briefing = if (sprachFrage) "das dem Leser vorgelesen wird" else "das zweimal am Tag erscheint und dem Leser vorgelesen wird"
        return """
            Du bist Redakteur eines deutschsprachigen Nachrichtenbriefings, $briefing.
            Heute ist $datum, es ist $uhr Uhr deutscher Zeit.

            $auftrag

            Arbeite wie eine gute Nachrichtenredaktion: Sortiere nach Relevanz und Tragweite, nicht nach der Reihenfolge der Suchtreffer. Das Wichtigste des Tages steht oben. Fasse mehrere Berichte über dasselbe Ereignis zu einer Meldung zusammen. Liefere 4 bis 7 Meldungen; gibt es weniger wirklich Neues, lieber weniger als aufgefüllt. Jede Meldung ist eigenständig und behandelt genau ein Ereignis. Bei widersprüchlichen Angaben nenne, wer was sagt.

            So schreibst du, damit es sich gut vorlesen lässt:
            - Deutsch mit echten Umlauten und ß, niemals ae, oe, ue oder ss als Ersatz.
            - Ganze, klare Sätze in natürlicher Sprechsprache. Keine Aufzählungszeichen, kein Markdown, keine Emojis, keine Internetadressen, keine Quellenangaben in Klammern, keine Fußnoten.
            - Zahlen so, wie man sie spricht: „rund zwei Milliarden Dollar“, „fünf Prozent“, „am Dienstag“. Keine Zeichen wie %, €, $, &, / oder ~ im Text.
            - Abkürzungen nur, wenn man sie gesprochen versteht, etwa KI oder EU; sonst ausschreiben.
            - Jede Meldung hat 2 bis 4 Absätze mit je 2 bis 4 Sätzen, jeder Absatz höchstens 500 Zeichen. Der erste Absatz sagt das Wichtigste, die weiteren erklären Hintergrund und Bedeutung.
            - Die Überschrift ist kurz, höchstens 70 Zeichen, ohne Punkt am Ende.

            Antworte ausschließlich mit einem JSON-Objekt, ohne Text davor oder danach, in genau dieser Form:
            {"blockTitel": "$blockTitel, höchstens drei Wörter",
             "meldungen": [{"titel": "...", "absaetze": ["...", "..."], "quellen": ["https://..."], "wann": "sprechbare Zeitangabe wie heute früh, gestern Abend oder am Mittwoch", "update": false, "bildIdee": "one English sentence describing a fitting editorial illustration, no text, no logos"}]}
            In "quellen" stehen die Adressen der Artikel, auf die sich die Meldung stützt, die beste zuerst. "update" ist true, wenn die Meldung eine Fortsetzung einer bereits bekannten Meldung ist.
        """.trimIndent()
    }

    private fun zerlege(thema: Thema, text: String, suchQuellen: List<String>): Block {
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
            ).also { bildIdeen[it.id] = m.optString("bildIdee").ifBlank { it.titel } }
        }
        val titel = json.optString("blockTitel").trim().ifBlank { thema.text.take(30) }
        return Block(thema.id, titel, meldungen, if (meldungen.isEmpty()) "Zu diesem Thema kam heute nichts Neues." else null)
    }

    // Nebenläufig: Eine gesprochene Frage kann parallel zu einem Lauf aus dem Zeitplan recherchiert werden.
    private val bildIdeen = ConcurrentHashMap<String, String>()

    // --- Bilder ------------------------------------------------------------------------------

    private suspend fun bebildere(block: Block, modus: BildModus, modellId: String, kiBudget: IntArray): Block {
        // Fotos der Quellen parallel holen — das sind nur kleine Seitenköpfe.
        val fotos: Map<String, String?> = if (modus == BildModus.NUR_KI) emptyMap() else coroutineScope {
            block.meldungen.map { m -> async { m.id to holeQuellenFoto(m.quellen) } }.awaitAll().toMap()
        }
        val fertig = block.meldungen.map { m ->
            val foto = fotos[m.id]
            when {
                foto != null -> m.copy(bildDatei = foto, bildIstKi = false)
                modus == BildModus.NUR_FOTO || kiBudget[0] <= 0 -> m
                else -> {
                    val ki = erzeugeKiBild(bildIdeen[m.id] ?: m.titel, block.titel, modellId, kiBudget)
                    if (ki != null) m.copy(bildDatei = ki, bildIstKi = true) else m
                }
            }
        }
        return block.copy(meldungen = fertig)
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
            speichereBild(Base64.decode(roh, Base64.DEFAULT))
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.warn("NewsRecherche", "erzeugeKiBild", "Bilderzeugung gescheitert", mapOf("grund" to fehler.message))
            val meldung = fehler.message.orEmpty()
            // Lehnt der Dienst das Werkzeug grundsätzlich ab, jeden weiteren Versuch sparen.
            if (meldung.contains("Codex-Fehler 400") || meldung.contains("image_generation", ignoreCase = true)) {
                einstellungen.setzeBilderUnterstuetzt(false)
                kiBudget[0] = 0
            }
            null
        }
    }

    /** Holt das Titelbild (og:image) des ersten Artikels, der eins hat. */
    private suspend fun holeQuellenFoto(quellen: List<String>): String? {
        for (adresse in quellen.take(3)) {
            val bild = withTimeoutOrNull(25_000) {
                runCatching { titelBildAdresse(adresse)?.let { ladeBild(it) } }.getOrNull()
            }
            if (bild != null) return bild
        }
        return null
    }

    private suspend fun titelBildAdresse(seite: String): String? {
        val basis = seite.toHttpUrlOrNull() ?: return null
        val anfrage = Request.Builder().url(basis).header("User-Agent", BROWSER).header("Accept", "text/html").build()
        val html = netz.newCall(anfrage).awaitAntwort().use { antwort ->
            if (!antwort.isSuccessful) return null
            val strom = antwort.body?.byteStream() ?: return null
            val puffer = ByteArray(MAX_HTML)
            var gelesen = 0
            while (gelesen < MAX_HTML) {
                val n = strom.read(puffer, gelesen, MAX_HTML - gelesen)
                if (n <= 0) break
                gelesen += n
                if (String(puffer, 0, gelesen, Charsets.UTF_8).contains("</head>", ignoreCase = true)) break
            }
            String(puffer, 0, gelesen, Charsets.UTF_8)
        }
        val treffer = META_REGEX.findAll(html).mapNotNull { tag ->
            val t = tag.value
            val art = ART_REGEX.find(t)?.groupValues?.get(1)?.lowercase() ?: return@mapNotNull null
            val inhalt = INHALT_REGEX.find(t)?.groupValues?.get(1) ?: return@mapNotNull null
            if (art in BILD_ARTEN) art to inhalt else null
        }.toList()
        val bester = BILD_ARTEN.firstNotNullOfOrNull { art -> treffer.firstOrNull { it.first == art }?.second } ?: return null
        val sauber = bester.replace("&amp;", "&").replace("&#x2F;", "/").replace("&#47;", "/").trim()
        return basis.resolve(sauber)?.toString()
    }

    private suspend fun ladeBild(adresse: String): String? {
        val anfrage = Request.Builder().url(adresse).header("User-Agent", BROWSER).build()
        val bytes = netz.newCall(anfrage).awaitAntwort().use { antwort ->
            if (!antwort.isSuccessful) return null
            val typ = antwort.header("Content-Type").orEmpty()
            if (typ.isNotBlank() && !typ.startsWith("image")) return null
            antwort.body?.bytes()
        } ?: return null
        if (bytes.size > MAX_BILD_BYTES) return null
        return speichereBild(bytes, mindestBreite = 480)
    }

    /** Verkleinert auf höchstens 1440 Pixel Breite und legt das Bild als JPEG ab. */
    private fun speichereBild(bytes: ByteArray, mindestBreite: Int = 0): String? {
        val masse = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, masse)
        if (masse.outWidth <= 0 || masse.outWidth < mindestBreite) return null
        var teiler = 1
        while (masse.outWidth / (teiler * 2) >= ZIEL_BREITE) teiler *= 2
        val bild = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = teiler })
            ?: return null
        val skaliert = if (bild.width > ZIEL_BREITE) {
            Bitmap.createScaledBitmap(bild, ZIEL_BREITE, (bild.height * ZIEL_BREITE.toFloat() / bild.width).toInt(), true)
        } else {
            bild
        }
        val name = "${UUID.randomUUID()}.jpg"
        val aus = ByteArrayOutputStream()
        skaliert.compress(Bitmap.CompressFormat.JPEG, 86, aus)
        File(speicher.bilderOrdner, name).writeBytes(aus.toByteArray())
        if (skaliert !== bild) skaliert.recycle()
        bild.recycle()
        return name
    }

    companion object {
        private const val MAX_HTML = 400_000
        private const val MAX_BILD_BYTES = 8_000_000
        private const val ZIEL_BREITE = 1440
        private const val BROWSER =
            "Mozilla/5.0 (Linux; Android 16; SM-F971B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0 Mobile Safari/537.36"
        private val BILD_ARTEN = listOf("og:image:secure_url", "og:image", "og:image:url", "twitter:image", "twitter:image:src")
        private val META_REGEX = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
        private val ART_REGEX = Regex("(?:property|name)\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        private val INHALT_REGEX = Regex("content\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)

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
