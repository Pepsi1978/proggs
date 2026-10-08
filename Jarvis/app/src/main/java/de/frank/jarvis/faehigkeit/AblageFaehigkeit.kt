package de.frank.jarvis.faehigkeit

import android.content.Context
import android.util.Base64
import de.frank.jarvis.ablage.AblageFehler
import de.frank.jarvis.ablage.AblageSpeicher
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.ablage.AgentenKontext
import de.frank.jarvis.ablage.Anhang
import de.frank.jarvis.ablage.Art
import de.frank.jarvis.ablage.Dateityp
import de.frank.jarvis.ablage.DownloadAuftrag
import de.frank.jarvis.ablage.Eintrag
import de.frank.jarvis.ablage.Kategorie
import de.frank.jarvis.ablage.Medien
import de.frank.jarvis.ablage.Uebertragungen
import de.frank.jarvis.ablage.UebertragungsZustand
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Die eigene Ablage von Jarvis, wie die übrigen Teile der App sie kennen: Texte als Markdown-Dateien. Seit der
 * Erweiterung um Dateien liegt dahinter der [AblageSpeicher]; diese Fassade bleibt, damit Mail, Vorlesen und Agenten
 * unverändert weiterarbeiten.
 */
object Ablage {
    fun speicher(context: Context): AblageSpeicher = AblageZentrale.speicher(context)

    /** Alle Textdateien (wie bisher), neueste zuerst. */
    fun alle(context: Context): List<File> = speicher(context).let { s -> s.eintraege().mapNotNull { s.textDatei(it) } }

    /** Die Textdatei eines Eintrags: genauer Titel oder ein eindeutiger Teil davon. */
    fun finde(context: Context, titel: String): File? = speicher(context).let { s -> s.finde(titel)?.let { s.textDatei(it) } }

    /** Legt den Text eines Eintrags an oder ersetzt ihn. Mit [anhaengen] wird der Text ans Ende gesetzt. */
    fun schreibe(context: Context, titel: String, inhalt: String, anhaengen: Boolean = false): File = speicher(context).schreibeText(titel, inhalt, anhaengen).second

    fun datum(datei: File): String = datum(datei.lastModified())
    fun datum(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm", Locale.GERMAN))
}

class AblageFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "ablage"
    override val name = "Jarvis-Ablage"
    override val beschreibung = "Ablage von Jarvis für Texte und Dateien aller Art: Recherchen, Bilder, Infografiken, PDFs, Dokumente, Tabellen, Audio, Video, Code."
    override val hinweise =
        "Die Ablage ist das Gedächtnis von Jarvis für Ergebnisse: Ein Eintrag hat einen Titel, optional einen lesbaren Text und beliebig viele Dateien (zum Beispiel Bericht + PDF + Bilder). " +
            "Texte legst du mit ablage_schreiben ab (als_pdf ergänzt ein PDF). Dateien übernimmst du mit ablage_datei_speichern: Dateien aus dem ChatGPT-Gespräch als chatgpt_dateien, sonst als Text-Inhalt (für Markdown, CSV, JSON, SVG, HTML, Code), " +
            "als kleiner Base64-Inhalt (bis etwa 700 kB) oder über eine https-Adresse, die das Handy selbst laden kann. Ein Dateipfad auf einem anderen Rechner oder in deiner eigenen Umgebung " +
            "(zum Beispiel sandbox:/ oder /mnt/data) ist vom Handy aus NICHT erreichbar – nimm dafür chatgpt_dateien, und geht das nicht, sag ehrlich, dass die Datei nicht übertragen werden kann. " +
            "Bilder und Infografiken erzeugt bild_erzeugen über das Bildwerkzeug von Jarvis' Codex-Anmeldung. Echte Dateien für PDF gibt es nur als Text-PDF oder Bild-PDF; " +
            "DOCX, XLSX, PPTX, Audio und Video kann Jarvis nicht selbst erzeugen, nur übernehmen. " +
            "WICHTIG: Melde etwas erst als gespeichert, wenn das Werkzeug-Ergebnis „Gespeichert“ sagt. Sagt es „läuft noch“, ist die Datei noch NICHT da: sag das so und prüfe später mit ablage_lesen. " +
            "Ist ein Vorhaben erledigt und Frank braucht den Eintrag nicht mehr, lösche ihn nach Rückfrage."

    override fun stoerung(): String? = null

    private val speicher get() = AblageZentrale.speicher(context)
    private val uebertragungen: Uebertragungen get() = AblageZentrale.uebertragungen(context)

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            w("ablage_lesen").als(
                beschreibung = "Jarvis: die Ablage von Jarvis (Recherchen, Ausarbeitungen, Notizen und Dateien wie Bilder, PDFs, Tabellen, Audio, Video). Ohne titel kommt die Liste der Einträge " +
                    "mit Datum, Art (nur Text, Text und Dateien, nur Dateien), Dateitypen und Größen, dazu laufende oder fehlgeschlagene Übertragungen – für „Was hast du recherchiert?“, " +
                    "„Ist das Bild schon gespeichert?“. Mit titel wird dieser Eintrag gelesen: der Text vollständig und die Liste seiner Dateien; ein eindeutiger Teil des Titels genügt.",
                schema = schema(
                    "titel" to text("Titel des Eintrags oder ein eindeutiger Teil davon. Weglassen = Liste aller Einträge."),
                    "suche" to text("Nur für die Liste: Einträge, deren Titel, Text oder Dateinamen dieses Wort enthalten."),
                    "art" to text("Nur für die Liste: nach Dateiart filtern.", listOf("alle") + Kategorie.entries.map { it.name.lowercase(Locale.ROOT) }),
                ),
            ) { a -> (if (a.gesetzt("titel")) w("ablage_lesen") else w("ablage_liste")).ausfuehren(a) },
            w("ablage_schreiben"), w("ablage_datei_speichern"), w("ablage_loeschen"), w("bild_erzeugen"),
        )
    }

    private fun w(name: String): Werkzeug = einzeln.first { it.name == name }

    private fun dateiZeile(a: Anhang): String =
        "${a.originalName} (${a.art.anzeige}, ${a.endung.uppercase(Locale.ROOT)}, ${Dateityp.groesse(a.groesse)}, ${Ablage.datum(a.erstellt)}, Herkunft: ${a.herkunft})" +
            (if (a.beschreibung.isNotBlank()) " – ${a.beschreibung.take(160)}" else "")

    private fun inhaltsArt(e: Eintrag): String = when {
        e.hatText && e.hatDateien -> "Text und ${e.anhaenge.size} Datei(en)"
        e.hatDateien -> "nur Dateien (${e.anhaenge.size})"
        else -> "nur Text"
    }

    private fun laufendText(): String = buildString {
        uebertragungen.liste.value.forEach { u ->
            when (u.zustand) {
                UebertragungsZustand.LAEUFT -> append("- LÄUFT: „${u.name}“ von ${u.host} für „${u.eintragTitel}“" + (u.prozent?.let { ", $it %" } ?: ", ${Dateityp.groesse(u.geladen)} geladen") + " – noch NICHT gespeichert.\n")
                UebertragungsZustand.FEHLER -> append("- FEHLGESCHLAGEN: „${u.name}“ von ${u.host} für „${u.eintragTitel}“: ${u.fehler}. Wiederholen mit ablage_datei_speichern (wiederholen=true).\n")
            }
        }
        AblageZentrale.arbeiten.value.forEach { a ->
            if (a.fehler == null) append("- LÄUFT: ${a.art} für „${a.eintragTitel}“ seit ${(System.currentTimeMillis() - a.seit) / 1000} s – noch NICHT gespeichert.\n")
            else append("- FEHLGESCHLAGEN: ${a.art} für „${a.eintragTitel}“: ${a.fehler}\n")
        }
    }

    private val einzeln: List<Werkzeug> = listOf(
        Werkzeug(
            name = "ablage_liste",
            titel = "Ablage ansehen",
            beschreibung = "Jarvis: listet die Einträge der Ablage von Jarvis mit Datum, Art und Dateien.",
            schema = schema("suche" to text("Optional: nur Einträge, deren Titel, Text oder Dateinamen dieses Wort enthalten.")),
            nurLesen = true,
        ) { a ->
            val s = speicher
            val suche = a.optString("suche").trim().lowercase(Locale.GERMAN)
            val kat = a.optString("art").trim().uppercase(Locale.ROOT).takeIf { it.isNotEmpty() && it != "ALLE" }?.let { k -> Kategorie.entries.firstOrNull { it.name == k } }
            val liste = s.eintraege().filter { e ->
                (kat == null || kat in e.kategorien) &&
                    (suche.isEmpty() || suche in e.titel.lowercase(Locale.GERMAN) || e.anhaenge.any { suche in it.originalName.lowercase(Locale.GERMAN) || suche in it.beschreibung.lowercase(Locale.GERMAN) } ||
                        suche in s.text(e, 400_000).lowercase(Locale.GERMAN))
            }
            val laufend = laufendText()
            val kopf = if (liste.isEmpty()) "Die Ablage ist leer" + (if (suche.isEmpty() && kat == null) "." else " für diese Auswahl.") else "${liste.size} Eintrag/Einträge in der Ablage:"
            Ergebnis(buildString {
                append(kopf).append('\n')
                liste.take(80).forEach { e ->
                    append("- ").append(e.titel).append(" (").append(Ablage.datum(e.geaendert)).append("; ").append(inhaltsArt(e))
                    s.textDatei(e)?.let { append("; Text ").append(Dateityp.groesse(it.length())) }
                    append(")")
                    if (e.hatDateien) append(": ").append(e.anhaenge.joinToString(", ") { "${it.originalName} [${it.art.anzeige}, ${Dateityp.groesse(it.groesse)}]" })
                    append('\n')
                }
                if (liste.size > 80) append("… und ${liste.size - 80} weitere (mit suche eingrenzen).\n")
                if (laufend.isNotEmpty()) append("\nÜbertragungen und Erzeugung:\n").append(laufend)
            }.trim())
        },
        Werkzeug(
            name = "ablage_lesen",
            titel = "Ablage-Eintrag lesen",
            beschreibung = "Jarvis: liest einen Eintrag der Ablage: den Text und die Liste der Dateien. Ein eindeutiger Teil des Titels genügt.",
            schema = schema("titel" to text("Titel des Eintrags oder ein eindeutiger Teil davon."), pflicht = listOf("titel")),
            nurLesen = true,
        ) { a ->
            val s = speicher
            val e = s.finde(a.optString("titel"))
            if (e == null) Ergebnis("Kein eindeutiger Eintrag für „${a.optString("titel")}“. Ohne titel kommt die Liste.", fehler = true)
            else Ergebnis(buildString {
                append("Eintrag „${e.titel}“ (Stand ${Ablage.datum(e.geaendert)}; ${inhaltsArt(e)}):\n\n")
                if (e.hatText) append(s.text(e, 240_000).take(60_000)).append('\n')
                if (e.hatDateien) {
                    append("\nDATEIEN (in der Jarvis-App unter Ablage ansehen, herunterladen oder teilen):\n")
                    e.anhaenge.forEach { an ->
                        append("- ").append(dateiZeile(an)).append('\n')
                        // Kleine Textdateien gleich mitliefern, damit das Modell sie lesen kann.
                        if (an.art.kategorie == Kategorie.TEXTE || an.art == Art.TABELLE_TEXT) {
                            if (an.groesse <= 20_000) append("  Inhalt:\n").append(runCatching { s.datei(an).readText() }.getOrDefault("").prependIndent("  ")).append('\n')
                        }
                    }
                }
            }.trim())
        },
        Werkzeug(
            name = "ablage_schreiben",
            titel = "In die Ablage schreiben",
            beschreibung = "Jarvis: legt einen Text in der Ablage von Jarvis an oder ersetzt ihn (mit anhaengen wird ergänzt). Für Ergebnisse, die Frank später wieder braucht: " +
                "Recherchen, Ausarbeitungen, Pläne, Zusammenfassungen eines Gesprächs. Mit als_pdf kommt zusätzlich ein DIN-A4-PDF des Textes als Datei in denselben Eintrag.",
            schema = schema(
                "titel" to text("Sprechender Titel, zum Beispiel „Recherche Wecker-App Machbarkeit“."),
                "inhalt" to text("Der vollständige Text (Markdown erlaubt)."),
                "anhaengen" to schalter("true = an einen vorhandenen Eintrag gleichen Titels anhängen statt ersetzen."),
                "als_pdf" to schalter("true = zusätzlich ein PDF des (ganzen) Textes als Datei im Eintrag ablegen."),
                pflicht = listOf("titel", "inhalt"),
            ),
            nurLesen = false,
        ) { a ->
            if (a.optString("inhalt").isBlank()) return@Werkzeug Ergebnis("Der Inhalt ist leer.", fehler = true)
            val (eintrag, datei) = speicher.schreibeText(a.optString("titel"), a.optString("inhalt"), a.optBoolean("anhaengen"))
            var text = "Abgelegt als „${datei.nameWithoutExtension}“ (${datei.length() / 1000 + 1} kB)."
            if (a.optBoolean("als_pdf")) {
                val teil = speicher.neueTeilDatei()
                text += try {
                    Medien.textAlsPdf(eintrag.titel, datei.readText(), teil)
                    val u = speicher.uebernimm(eintrag.titel, teil, AblageSpeicher.dateiname(eintrag.titel) + ".pdf", "application/pdf", "Jarvis (PDF aus Text)")
                    AblageZentrale.nachDemSpeichern(context, u)
                    " " + AblageZentrale.meldung(u)
                } catch (e: Exception) {
                    " Das PDF ließ sich nicht erzeugen (${AblageZentrale.fehlerText(e)}); der Text ist gespeichert."
                } finally { teil.delete() }
            }
            Ergebnis(text)
        },
        Werkzeug(
            name = "ablage_datei_speichern",
            titel = "Datei in die Ablage übernehmen",
            beschreibung = "Jarvis: übernimmt eine oder mehrere Dateien dauerhaft in einen Ablage-Eintrag von Jarvis (gleicher Titel = gleicher Eintrag, so entstehen Einträge mit mehreren Dateien). " +
                "Dateien aus dem ChatGPT-Gespräch (hochgeladen oder von ChatGPT erzeugt) übergibst du als chatgpt_dateien. Sonst kommt jede Datei als inhalt_text (Markdown, CSV, JSON, SVG, HTML, Code), als inhalt_base64 (höchstens etwa 700 kB) oder als url: eine https-Adresse, die das Handy selbst " +
                "abrufen kann (auch kurzlebige Ergebnis-Links; Jarvis lädt sie sofort). Dateipfade fremder Rechner sind nicht erreichbar. Größere Downloads laufen im Hintergrund weiter: " +
                "Dann meldet das Werkzeug „läuft noch“ und die Datei ist erst nach der Benachrichtigung da. Mit wiederholen=true werden fehlgeschlagene Übertragungen erneut versucht. " +
                "Gleiche Datei zweimal oder dieselbe kennung legt nichts doppelt an.",
            schema = schema(
                "eintrag" to text("Titel des Ablage-Eintrags. Vorhanden = Datei kommt dazu, sonst neuer Eintrag."),
                "dateiname" to text("Dateiname mit Endung, zum Beispiel „Infografik.png“ oder „Daten.csv“."),
                "url" to text("https-Adresse der Datei."),
                "inhalt_text" to text("Der Inhalt einer Textdatei."),
                "inhalt_base64" to text("Der Inhalt einer kleinen Binärdatei als Base64."),
                "mime" to text("Optional: MIME-Typ, zum Beispiel image/png."),
                "beschreibung" to text("Optional: kurze Beschreibung der Datei."),
                "text" to text("Optional: kurzer Begleittext zur Datei (in der Ablage eingeklappt unter der Datei). Nicht den Auftrag wiederholen; keinen zweiten Eintrag mit ablage_schreiben anlegen."),
                "kennung" to text("Optional: eindeutige Kennung dieses Ergebnisses; wiederholte Zustellung mit derselben Kennung wird erkannt."),
                "dateien" to JSONObject().put("type", "array").put("description", "Mehrere Dateien auf einmal; je Datei dieselben Felder wie oben.")
                    .put("items", JSONObject().put("type", "object").put("additionalProperties", false).put("properties", JSONObject()
                        .put("dateiname", text("Dateiname mit Endung.")).put("url", text("https-Adresse.")).put("inhalt_text", text("Textinhalt."))
                        .put("inhalt_base64", text("Base64-Inhalt.")).put("mime", text("MIME-Typ.")).put("beschreibung", text("Beschreibung.")).put("kennung", text("Kennung.")))),
                "wiederholen" to schalter("true = fehlgeschlagene Übertragungen erneut versuchen (sonst nichts angeben)."),
                CHATGPT_DATEIEN to JSONObject().put("type", "array")
                    .put("description", "Dateien aus diesem ChatGPT-Gespräch (hochgeladen oder erzeugt), die Jarvis ablegen soll. ChatGPT übergibt sie hier als Datei-Verweise; Jarvis lädt sie sofort aufs Handy.")
                    .put("items", DATEI_OBJEKT),
            ),
            nurLesen = false,
            // Offizieller Weg der ChatGPT-Plugins für Dateieingaben (developers.openai.com/plugins/reference, „Define file inputs“).
            meta = JSONObject().put("openai/fileParams", JSONArray().put(CHATGPT_DATEIEN)),
        ) { a -> speichereDateien(a) },
        Werkzeug(
            name = "ablage_loeschen",
            titel = "Ablage-Eintrag löschen",
            beschreibung = "Jarvis: löscht einen Eintrag der Ablage von Jarvis samt Text und Dateien, oder mit datei nur eine Datei daraus. Nur wenn Frank das verlangt oder bestätigt hat.",
            schema = schema(
                "titel" to text("Titel des Eintrags oder ein eindeutiger Teil davon."),
                "datei" to text("Optional: nur diese Datei des Eintrags löschen (Dateiname)."),
                pflicht = listOf("titel"),
            ),
            nurLesen = false,
            loeschend = true,
        ) { a ->
            val e = speicher.finde(a.optString("titel")) ?: return@Werkzeug Ergebnis("Kein eindeutiger Eintrag für „${a.optString("titel")}“.", fehler = true)
            if (a.gesetzt("datei")) {
                val g = a.optString("datei").trim().lowercase(Locale.GERMAN)
                val an = e.anhaenge.firstOrNull { it.originalName.lowercase(Locale.GERMAN) == g } ?: e.anhaenge.filter { g in it.originalName.lowercase(Locale.GERMAN) }.singleOrNull()
                    ?: return@Werkzeug Ergebnis("Keine eindeutige Datei „${a.optString("datei")}“ in „${e.titel}“.", fehler = true)
                speicher.loescheAnhang(e.id, an.id)
                Ergebnis("Gelöscht: „${an.originalName}“ aus „${e.titel}“.")
            } else {
                speicher.loescheEintrag(e.id)
                Ergebnis("Gelöscht: „${e.titel}“" + (if (e.hatDateien) " mit ${e.anhaenge.size} Datei(en)." else "."))
            }
        },
        Werkzeug(
            name = "bild_erzeugen",
            titel = "Bild oder Infografik erzeugen",
            beschreibung = "Jarvis: erzeugt ein Bild oder eine Infografik über das Bildwerkzeug von Jarvis (Codex-Anmeldung von Frank) und speichert es als PNG in der Ablage, auf Wunsch " +
                "zusätzlich als DIN-A4-PDF. Beschreibe Inhalt, Aufbau, Stil und ALLE Texte, die im Bild stehen sollen, vollständig auf Deutsch; Fakten (zum Beispiel Jarvis' Funktionen) " +
                "vorher mit Werkzeugen lesen, nie erfinden. Das Erzeugen dauert meist 30 bis 120 Sekunden: Kommt „läuft noch“, ist das Bild noch NICHT gespeichert; Frank bekommt eine " +
                "Benachrichtigung, prüfen mit ablage_lesen.",
            schema = schema(
                "beschreibung" to text("Vollständige Bildbeschreibung mit allen Texten, die im Bild stehen sollen."),
                "titel" to text("Titel des Ablage-Eintrags, zum Beispiel „Infografik Jarvis-Funktionen“."),
                "format" to text("Format; Vorgabe hoch.", listOf("hoch", "quer", "quadrat", "din_a4", "din_a4_quer")),
                "hohe_qualitaet" to schalter("true = höchste Qualität (dauert länger), empfohlen für Infografiken mit viel Text."),
                "auch_pdf" to schalter("true = zusätzlich ein DIN-A4-PDF mit dem Bild ablegen."),
                "text" to text("Nur wenn Frank ausdrücklich einen Begleittext verlangt. Sonst weglassen: Das Bild steht für sich, Auftrag und Bildbeschreibung gehören nicht hierher."),
                pflicht = listOf("beschreibung", "titel"),
            ),
            nurLesen = false,
        ) { a -> erzeugeBild(a) },
    )

    private suspend fun speichereDateien(a: JSONObject): Ergebnis {
        val warten = AblageZentrale.wartezeit()
        if (a.optBoolean("wiederholen")) {
            val offen = uebertragungen.liste.value.filter { it.zustand == UebertragungsZustand.FEHLER }
            if (offen.isEmpty()) return Ergebnis("Es gibt keine fehlgeschlagene Übertragung.")
            val teile = offen.map { u ->
                runCatching { AblageZentrale.starteDownload(context, u.id, u.name, u.eintragTitel, warten) ?: "„${u.name}“ läuft noch (noch NICHT gespeichert)." }
                    .getOrElse { "„${u.name}“: ${AblageZentrale.fehlerText(it)}" }
            }
            return Ergebnis(teile.joinToString("\n"))
        }
        val eintrag = a.optString("eintrag").trim().ifEmpty { AblageZentrale.agentenEintrag().orEmpty() }
        if (eintrag.isEmpty()) return Ergebnis("Bitte einen Titel für den Ablage-Eintrag angeben (eintrag).", fehler = true)
        val dateien = mutableListOf<JSONObject>()
        a.optJSONArray("dateien")?.let { arr -> for (i in 0 until arr.length()) arr.optJSONObject(i)?.let(dateien::add) }
        if (listOf("url", "inhalt_text", "inhalt_base64").any { a.gesetzt(it) || (it == "inhalt_text" && a.has(it)) }) dateien.add(0, a)
        // ChatGPT-Dateiverweise: download_url ist kurzlebig, file_id bleibt gleich (Schutz vor doppelter Zustellung).
        a.optJSONArray(CHATGPT_DATEIEN)?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                if (!o.gesetzt("download_url")) continue
                dateien += JSONObject().put("url", o.optString("download_url")).put("dateiname", o.optString("file_name")).put("mime", o.optString("mime_type"))
                    .put("kennung", o.optString("file_id").takeIf { it.isNotBlank() }?.let { "chatgpt-$it" } ?: "").put("herkunft", "ChatGPT-Datei")
            }
        }
        if (dateien.isEmpty()) return Ergebnis("Es wurde keine Datei übergeben (chatgpt_dateien, url, inhalt_text oder inhalt_base64).", fehler = true)
        if (a.gesetzt("text")) speicher.schreibeText(eintrag, a.optString("text"))
        val zeilen = mutableListOf<String>()
        var fehler = 0
        var laeuft = 0
        for ((i, d) in dateien.withIndex()) {
            val name = d.optString("dateiname").trim()
            val kennung = d.optString("kennung").trim()
            try {
                when {
                    d.gesetzt("url") -> {
                        val url = d.optString("url").trim()
                        if (url.startsWith("sandbox:") || url.startsWith("/") || url.startsWith("file:")) throw AblageFehler("„${name.ifEmpty { "Datei ${i + 1}" }}“: Das ist ein Pfad auf einem anderen System, kein Download-Link. Vom Handy aus nicht erreichbar; nichts gespeichert.")
                        // Ohne eigene Kennung schützt ein Fingerabdruck von Adresse und Eintrag vor doppelter Zustellung.
                        val k = kennung.ifEmpty { "url-" + kurzHash(url + "|" + eintrag) }
                        val schon = uebertragungen.laeuft(k)
                        val id = schon?.id ?: uebertragungen.anlegen(DownloadAuftrag(url, eintrag, name, d.optString("mime"), d.optString("beschreibung"), k, herkunft = d.optString("herkunft").ifBlank { "Download" }))
                        val r = AblageZentrale.starteDownload(context, id, name, eintrag, warten)
                        if (r == null) {
                            laeuft++
                            val stand = uebertragungen.liste.value.firstOrNull { it.id == id }
                            zeilen += "„${stand?.name ?: name}“ läuft noch" + (stand?.prozent?.let { " ($it %)" } ?: "") + " – noch NICHT gespeichert. Rufe ablage_datei_speichern sofort mit denselben Angaben noch einmal auf (wartet auf diese Übertragung, legt nichts doppelt an) und sag Frank Bescheid, sobald „Gespeichert“ kommt."
                        } else zeilen += r
                    }
                    d.gesetzt("inhalt_base64") -> {
                        val roh = d.optString("inhalt_base64").substringAfter("base64,")
                        if (roh.length > 1_000_000) throw AblageFehler("„$name“ ist für die direkte Übergabe zu groß (höchstens etwa 700 kB). Bitte als https-Adresse übergeben.")
                        val bytes = runCatching { Base64.decode(roh, Base64.DEFAULT) }.getOrElse { throw AblageFehler("„$name“: Der Base64-Inhalt ist ungültig; nichts gespeichert.") }
                        val u = speicher.uebernimmBytes(eintrag, bytes, name.ifEmpty { "Datei" }, d.optString("mime"), herkunft(), d.optString("beschreibung"), kennung.ifEmpty { null })
                        AblageZentrale.nachDemSpeichern(context, u)
                        zeilen += AblageZentrale.meldung(u)
                    }
                    else -> {
                        val inhalt = d.optString("inhalt_text")
                        if (inhalt.isEmpty()) throw AblageFehler("„$name“ ist leer; nichts gespeichert.")
                        val u = speicher.uebernimmBytes(eintrag, inhalt.toByteArray(), name.ifEmpty { "Text.txt" }, d.optString("mime"), herkunft(), d.optString("beschreibung"), kennung.ifEmpty { null })
                        AblageZentrale.nachDemSpeichern(context, u)
                        zeilen += AblageZentrale.meldung(u)
                    }
                }
            } catch (e: Exception) {
                fehler++
                zeilen += "NICHT gespeichert: " + AblageZentrale.fehlerText(e)
            }
        }
        if (dateien.size > 1) zeilen.add(0, "${dateien.size - fehler - laeuft} von ${dateien.size} Datei(en) gespeichert" + (if (laeuft > 0) ", $laeuft laufen noch" else "") + (if (fehler > 0) ", $fehler fehlgeschlagen" else "") + ":")
        return Ergebnis(zeilen.joinToString("\n"), fehler = fehler == dateien.size)
    }

    private suspend fun erzeugeBild(a: JSONObject): Ergebnis {
        val beschreibung = a.optString("beschreibung").trim()
        if (beschreibung.length < 10) return Ergebnis("Bitte das Bild ausführlich beschreiben (Inhalt, Aufbau, alle Texte).", fehler = true)
        val titel = a.optString("titel").trim().ifEmpty { AblageZentrale.agentenEintrag().orEmpty() }.ifEmpty { "Bild" }
        val format = when (a.optString("format").lowercase(Locale.ROOT)) {
            "quer" -> AblageZentrale.Bildformat.QUER
            "quadrat" -> AblageZentrale.Bildformat.QUADRAT
            "din_a4" -> AblageZentrale.Bildformat.DIN_A4
            "din_a4_quer" -> AblageZentrale.Bildformat.DIN_A4_QUER
            else -> AblageZentrale.Bildformat.HOCH
        }
        if (a.gesetzt("text")) speicher.schreibeText(titel, a.optString("text"))
        // Gleicher Auftrag noch einmal (etwa weil ChatGPT nach einer Zeitüberschreitung wiederholt) hängt sich an den laufenden an.
        val kennung = "bild-" + kurzHash(titel + "|" + beschreibung + "|" + format.name + "|" + a.optBoolean("auch_pdf"))
        return try {
            val r = AblageZentrale.imHintergrund(context, kennung, "Bild erzeugen", titel, AblageZentrale.wartezeit()) {
                AblageZentrale.erzeugeBild(context, beschreibung, titel, format, a.optBoolean("hohe_qualitaet"), a.optBoolean("auch_pdf"), kennung)
            }
            Ergebnis(r ?: "Das Bild wird gerade erzeugt und ist noch NICHT gespeichert (meist ein bis zwei Minuten). Rufe bild_erzeugen jetzt sofort mit genau denselben Angaben noch einmal auf: " +
                "Das wartet auf dieses Bild und erzeugt kein zweites. Wiederhole das, bis „Gespeichert“ kommt, und sag Frank dann von dir aus, dass es im Eintrag „$titel“ liegt.")
        } catch (e: Exception) {
            Ergebnis("Kein Bild gespeichert: " + AblageZentrale.fehlerText(e), fehler = true)
        }
    }

    private suspend fun herkunft(): String = if (currentCoroutineContext()[AgentenKontext] != null) "Jarvis" else "ChatGPT"

    private companion object {
        const val CHATGPT_DATEIEN = "chatgpt_dateien"

        /** Dateiobjekt nach der Plugin-Referenz: alle vier Felder deklariert, nur download_url und file_id Pflicht. */
        val DATEI_OBJEKT: JSONObject = JSONObject().put("type", "object").put("properties", JSONObject()
            .put("download_url", JSONObject().put("type", "string").put("description", "Temporäre Download-Adresse der Datei."))
            .put("file_id", JSONObject().put("type", "string").put("description", "Kennung der Datei in ChatGPT."))
            .put("mime_type", JSONObject().put("type", "string").put("description", "MIME-Typ, falls bekannt."))
            .put("file_name", JSONObject().put("type", "string").put("description", "Dateiname, falls bekannt.")))
            .put("required", JSONArray().put("download_url").put("file_id"))
    }

    private fun kurzHash(s: String): String = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).take(8).joinToString("") { "%02x".format(it) }
}
