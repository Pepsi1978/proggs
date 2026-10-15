package de.frank.epigenetik.ki

import de.frank.epigenetik.auth.AuthErrorKind
import de.frank.epigenetik.auth.ChatTurn
import de.frank.epigenetik.auth.CodexAuthException
import de.frank.epigenetik.auth.CodexAuthManager
import de.frank.epigenetik.auth.CodexModel
import de.frank.epigenetik.auth.ReasoningEffort
import de.frank.epigenetik.data.Einstellungen
import de.frank.epigenetik.data.Kategorie
import de.frank.epigenetik.data.Recherche
import de.frank.epigenetik.data.SchalterTreffer
import de.frank.epigenetik.data.Schalterkatalog
import de.frank.epigenetik.data.Typ
import de.frank.epigenetik.data.Zielkatalog
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/** Was eine laufende Recherche als Kontext mitbekommt. */
data class Kontext(
    /** Fertige Mittel der Bibliothek (Name, Kategorie, Kurzfazit, Schalter). */
    val bibliothek: List<Recherche> = emptyList(),
    /** Offline gefundene Mittel zu den Schaltern einer Wirkung oder eines Ziels. */
    val treffer: List<SchalterTreffer> = emptyList(),
    /** Mittel im Stack mit ihren Schaltern. */
    val stack: List<Pair<Recherche, String>> = emptyList(),
    /** Ziele des Nutzers (Titel). */
    val ziele: List<String> = emptyList(),
)

/**
 * Alle KI-Recherchen. Jede Art hat ein festes JSON-Schema; die Schalter-IDs stammen aus dem
 * [Schalterkatalog], damit sich die Ergebnisse untereinander verknüpfen lassen.
 */
class EpigenetikKi(private val auth: CodexAuthManager, private val e: Einstellungen) {

    suspend fun recherchiere(r: Recherche, k: Kontext, onDelta: (String) -> Unit): String {
        auth.webSucheZuruecksetzen()
        val auftrag = when (r.typEnum) {
            Typ.MITTEL -> mittelAuftrag(r)
            Typ.WIRKUNG -> wirkungAuftrag(r, k)
            Typ.ZIEL -> zielAuftrag(r, k)
            Typ.STACK -> stackAuftrag(k)
            Typ.FRAGE -> frageAuftrag(r, k)
        }
        val antwort = frage(grundanweisung(), auftrag, e.modell, e.denkstufe, webSuche = true, label = "${r.typEnum.anzeige} „${r.name}“", onDelta = onDelta)
        // Prüft, dass ein auswertbares JSON-Objekt drin steckt – sonst gilt die Recherche als gescheitert.
        return jsonAus(antwort).toString()
    }

    /** Kurze Textkorrektur einer diktierten Eingabe (schnelles Modell, ohne Websuche). */
    suspend fun korrigiere(text: String): String = frage(
        "Der Text ist eine diktierte Eingabe für eine Epigenetik-App (Name eines Nahrungsergänzungsmittels, Lebensmittels, " +
            "Verhaltens, einer epigenetischen Wirkung, eines Ziels oder eine Frage). Korrigiere Rechtschreibung und Fachbegriffe " +
            "(z. B. Ashwagandha, Nikotinamid-Ribosid, Sulforaphan, Sirtuine, Resveratrol, Spermidin, Fisetin). Ändere nichts am Inhalt. " +
            "Antworte nur mit dem korrigierten Text.",
        text, CodexModel.TERRA, ReasoningEffort.LOW, label = "Korrektur",
    ).trim().trim('„', '“', '"')

    // ================= Aufträge =================

    private fun grundanweisung(): String = buildString {
        append(
            """
            |Du bist ein äußerst gründlicher Forscher für Epigenetik, Ernährungsmedizin, Nahrungsergänzung und Langlebigkeit.
            |Du recherchierst im Internet den aktuellen Forschungsstand (PubMed, Metaanalysen, RCTs, Humanstudien, Übersichtsarbeiten)
            |und antwortest ausschließlich mit einem einzigen JSON-Objekt, ohne Text davor oder danach. Alle Texte auf Deutsch
            |mit echten Umlauten (ä, ö, ü, ß), klar und verständlich für einen interessierten Laien.
            |
            |REGELN FÜR EHRLICHE EVIDENZ:
            |- Nenne zu jeder Schalter-Wirkung die höchste verfügbare Evidenzstufe: Meta, RCT, Humanstudie, Tier oder Zellkultur.
            |- Formuliere Zellkultur- oder Tierbefunde nie so, als wirkten sie sicher beim Menschen.
            |- Koppele "staerke" (0–100) an Humandaten: ohne Humanstudie höchstens 40, nur Zellkultur höchstens 25.
            |- Dosis immer doppelt denken: Studiendosis gegenüber dem, was per Kapsel oder Ernährung realistisch erreichbar ist.
            |  In-vitro-Konzentrationen, die im Blut nie erreicht werden, als solche kennzeichnen.
            |- Widersprüchliche Studienlage offen nennen, nicht glätten. Tiermodelle, Surrogatmarker und Industrie-Finanzierung kennzeichnen.
            |- Nur epigenetisch relevante Wirkungen (Genregulation, Methylierung, Histone, miRNA, Sirtuine/NAD+, Signalwege, die
            |  Genexpression steuern) gehören in "schalter"; allgemeine Wirkungen nur im Text.
            |- Quellen nur, wenn du sie wirklich gefunden hast (mit Link, wenn sicher). Keine erfundenen Studien – lieber weniger Quellen.
            |- Keine Heilversprechen; das Fazit nennt immer auch die größte Unsicherheit.
            |
            |FESTES SCHALTER-VOKABULAR (verwende für "schalter" NUR diese IDs; alles andere kommt nach "sonstige_schalter"):
            |${Schalterkatalog.fuerKi()}
            |
            |ZIEL-IDS: ${Zielkatalog.alle.joinToString(", ") { "${it.id} (${it.name})" }}
            |
            |HEUTE: ${heute()}
            """.trimMargin(),
        )
        val profil = e.profilText()
        if (profil.isNotBlank()) append("\n\nNUTZERPROFIL (bei Dosierung, Risiken und Empfehlungen berücksichtigen): ").append(profil)
    }

    private fun mittelAuftrag(r: Recherche): String {
        val kat = if (r.kategorie.isBlank()) "unbekannt – bestimme sie selbst" else r.kategorieEnum.anzeige
        return """
            |RECHERCHE EINES MITTELS: „${r.name}“ (Art: $kat)
            |Recherchiere genauestens, welchen Einfluss dieses ${if (r.kategorieEnum == Kategorie.VERHALTEN) "Verhalten" else "Mittel"} auf die Epigenetik hat:
            |Welche Gene, Enzyme und Signalwege werden an- oder abgeschaltet, über welche Mechanismen (DNA-Methylierung, Histon-Modifikation,
            |miRNA, Sirtuin-/NAD+-Achse, Transkriptionsfaktoren), wie stark, wie gut belegt und bei welcher Dosis. Gehe mehrere Quellen durch.
            |Ist der Name falsch geschrieben, korrigiere ihn in "name".
            |
            |JSON-SCHEMA:
            |{"name": "korrekter deutscher Name",
            | "kategorie": "supplement|nahrung|verhalten",
            | "klasse": "z. B. Polyphenol, Vitamin, Adaptogen, Ausdauertraining",
            | "aliase": ["weitere Namen, englisch, Abkürzungen, Wirkstoff"],
            | "kurz": "1 Satz: was es epigenetisch vor allem bewirkt",
            | "potenzial": 0-100 (Gesamtbewertung des epigenetischen Nutzens beim Menschen, Evidenz eingerechnet),
            | "evidenzstufe": "stark|mittel|schwach|präklinisch",
            | "schalter": [{"id": "SIRT1", "richtung": "aktiviert|hemmt|moduliert", "staerke": 0-100, "evidenz": "Meta|RCT|Humanstudie|Tier|Zellkultur",
            |   "mechanismus": "1 Satz, wie", "dosis_bezug": "Studiendosis und ob realistisch erreichbar"}],
            | "sonstige_schalter": [{"name": "Gen/Weg außerhalb des Vokabulars", "richtung": "aktiviert|hemmt", "mechanismus": "1 Satz"}],
            | "mechanismen": ["2–6 Sätze, je ein epigenetischer Mechanismus, z. B. 'DNA-Methylierung: …'"],
            | "ziele": ["ZIEL-IDs, für die das Mittel besonders nützt"],
            | "dosierung": {"studien": "…", "empfohlen": "…", "obergrenze": "…"},
            | "einnahme": {"timing": "morgens|abends|zur Mahlzeit|…", "mit_fett": true, "nuechtern": false, "zyklisch": "z. B. 8 Wochen, dann 2 Wochen Pause, oder leer", "hinweise": "…"},
            | "bioverfuegbarkeit": {"bewertung": "gering|mittel|hoch", "beste_form": "…", "verstaerker": ["…"]},
            | "risiken": ["…"],
            | "wechselwirkungen": [{"mit": "…", "art": "…", "schwere": "leicht|mittel|schwer"}],
            | "synergien": [{"mittel": "Name eines anderen Mittels", "begruendung": "1 Satz"}],
            | "kontroversen": ["widersprüchliche Befunde"],
            | "offene_fragen": ["was noch unklar ist"],
            | "fazit": "genau 2 Sätze inkl. größter Unsicherheit",
            | "quellen": [{"titel": "…", "jahr": 2024, "typ": "Meta|RCT|Humanstudie|Review|Tier|Zellkultur", "url": "…"}]}
            |Liefere 4–14 Schalter, die wichtigsten (stärkste, best belegte) zuerst, 3–10 Quellen.
        """.trimMargin()
    }

    private fun wirkungAuftrag(r: Recherche, k: Kontext): String = """
        |RÜCKWÄRTSSUCHE NACH EINER WIRKUNG: „${r.name}“
        |Finde heraus, welche Nahrungsergänzungsmittel (Hauptfokus), welche Nahrungsmittel (zweitens) und welche Verhaltensweisen
        |(drittens) diese epigenetische Wirkung erzielen. Ordne die Wirkung den passenden Schalter-IDs zu. Sortiere jede Liste nach
        |Stärke × Evidenz beim Menschen, das Beste zuerst.
        |${trefferText(k)}
        |
        |JSON-SCHEMA:
        |{"titel": "klarer Titel der Wirkung", "schalter_ids": ["SIRT1", "NAD"],
        | "erklaerung": "3–4 Sätze: was diese Wirkung ist, warum sie wichtig ist, wie man sie misst",
        | "supplements": [{"name": "…", "staerke": 0-100, "evidenz": "Meta|RCT|Humanstudie|Tier|Zellkultur", "mechanismus": "1 Satz", "dosis_hinweis": "…"}],
        | "nahrung": [gleiches Format], "verhalten": [gleiches Format],
        | "zu_meiden": [{"name": "…", "warum": "1 Satz"}],
        | "kombination": "1–2 Sätze: die sinnvollste Kombination für diese Wirkung",
        | "fazit": "2 Sätze", "quellen": [{"titel": "…", "jahr": 2024, "typ": "…", "url": "…"}]}
        |Liefere 8–12 Supplements, 6–10 Nahrungsmittel, 4–8 Verhaltensweisen.
    """.trimMargin()

    private fun zielAuftrag(r: Recherche, k: Kontext): String {
        val vorlage = Zielkatalog.alle.firstOrNull { de.frank.epigenetik.data.normalisiere(it.name) == r.schluessel || it.id.equals(r.name, true) }
        return """
            |ZIEL DES NUTZERS: „${r.name}“
            |${vorlage?.let { "Kernschalter laut Katalog: ${it.schalter.joinToString()}" } ?: "Ordne das Ziel selbst den passenden Schaltern zu."}
            |Erkläre, welche epigenetischen Schalter für dieses Ziel entscheidend sind, und finde die besten Nahrungsergänzungsmittel
            |(Hauptfokus), Nahrungsmittel und Verhaltensweisen, um sie in die günstige Richtung zu bringen. Schlage einen
            |konkreten, realistischen Stack vor (höchstens 6 Mittel) und nenne messbare Marker zur Erfolgskontrolle.
            |${trefferText(k)}
            |${if (k.stack.isNotEmpty()) "AKTUELLER STACK DES NUTZERS: ${k.stack.joinToString { it.first.name }}" else ""}
            |
            |JSON-SCHEMA:
            |{"titel": "klarer Titel", "emoji": "ein passendes Emoji", "beschreibung": "2–3 Sätze, was das Ziel epigenetisch bedeutet",
            | "schalter": [{"id": "SIRT1", "relevanz": 0-100, "richtung": "aktivieren|hemmen|im Gleichgewicht halten", "warum": "1 Satz"}],
            | "supplements": [{"name": "…", "staerke": 0-100, "evidenz": "…", "mechanismus": "1 Satz", "dosis_hinweis": "…"}],
            | "nahrung": [gleiches Format], "verhalten": [gleiches Format],
            | "stack_vorschlag": [{"name": "…", "dosis": "…", "timing": "…", "warum": "1 Satz"}],
            | "zu_meiden": [{"name": "…", "warum": "1 Satz"}],
            | "messbar": [{"marker": "z. B. hsCRP, HbA1c, epigenetische Uhr, HRV", "warum": "1 Satz"}],
            | "fazit": "2 Sätze", "quellen": [{"titel": "…", "jahr": 2024, "typ": "…", "url": "…"}]}
            |Liefere 4–10 Schalter, 6–10 Supplements, 5–8 Nahrungsmittel, 4–8 Verhaltensweisen.
        """.trimMargin()
    }

    private fun stackAuftrag(k: Kontext): String = """
        |ANALYSE DES STACKS (alle Mittel, die der Nutzer täglich nimmt bzw. umsetzt):
        |${k.stack.joinToString("\n") { (m, schalter) -> "- ${m.name} (${m.kategorieEnum.anzeige}): ${m.kurz} Schalter: $schalter" }}
        |${if (k.ziele.isNotEmpty()) "ZIELE DES NUTZERS: ${k.ziele.joinToString()}" else "ZIELE: Langlebigkeit, Prävention, körperliche und geistige Gesundheit."}
        |
        |Bewerte den Stack epigenetisch: Wo verstärken sich Mittel (Synergien), wo doppeln sie sich unnötig, welche wichtigen
        |Schalter für die Ziele bleiben unabgedeckt (mit konkreten Vorschlägen, Supplements zuerst), welche Risiken oder
        |Wechselwirkungen gibt es und wann am Tag nimmt man was am besten. Recherchiere bei Unklarheiten im Internet.
        |
        |JSON-SCHEMA:
        |{"bewertung": 0-100, "zusammenfassung": "3 Sätze",
        | "synergien": [{"mittel": ["A", "B"], "warum": "1 Satz"}],
        | "doppelungen": [{"mittel": ["A", "B"], "hinweis": "1 Satz"}],
        | "luecken": [{"schalter": "ID aus dem Vokabular", "warum": "1 Satz", "vorschlaege": ["Mittel 1", "Mittel 2"]}],
        | "risiken": [{"mittel": ["A", "B"], "hinweis": "1 Satz", "schwere": "leicht|mittel|schwer"}],
        | "tagesplan": [{"zeit": "Morgens nüchtern|Zum Frühstück|Mittags|Abends|Vor dem Schlafen", "mittel": ["…"], "hinweis": "…"}],
        | "weglassen": [{"name": "…", "warum": "1 Satz"}],
        | "fazit": "2 Sätze"}
    """.trimMargin()

    private fun frageAuftrag(r: Recherche, k: Kontext): String = """
        |FRAGE DES NUTZERS: „${r.name}“
        |Beantworte die Frage gründlich, ehrlich und praxisnah aus Sicht der Epigenetik. Recherchiere im Internet.
        |${if (k.bibliothek.isNotEmpty()) "BEREITS RECHERCHIERTE MITTEL DES NUTZERS (nutze sie, wo passend):\n" + k.bibliothek.take(60).joinToString("\n") { "- ${it.name}: ${it.kurz}" } else ""}
        |${if (k.stack.isNotEmpty()) "SEIN STACK: ${k.stack.joinToString { it.first.name }}" else ""}
        |
        |JSON-SCHEMA:
        |{"titel": "kurzer Titel der Frage", "kurz": "1 Satz Kernantwort",
        | "antwort": "ausführliche Antwort in 3–8 Absätzen, Absätze mit \n\n getrennt",
        | "empfehlungen": [{"name": "Mittel oder Verhalten", "warum": "1 Satz"}],
        | "quellen": [{"titel": "…", "jahr": 2024, "typ": "…", "url": "…"}]}
    """.trimMargin()

    private fun trefferText(k: Kontext): String {
        if (k.treffer.isEmpty()) return ""
        val zeilen = k.treffer.groupBy { it.name }.entries.take(40).joinToString("\n") { (name, t) ->
            "- $name: " + t.joinToString("; ") { "${it.schalter} ${it.richtung} (${it.staerke}, ${it.evidenz})" }
        }
        return "BEREITS RECHERCHIERT (aus der Bibliothek des Nutzers, berücksichtige und ergänze sie):\n$zeilen"
    }

    // ================= Aufruf =================

    private suspend fun frage(
        anweisung: String,
        text: String,
        modell: CodexModel,
        stufe: ReasoningEffort,
        webSuche: Boolean = false,
        label: String = "KI-Aufruf",
        onDelta: (String) -> Unit = {},
    ): String {
        var versuch = 0
        while (true) {
            val start = System.currentTimeMillis()
            var empfangen = 0
            KiLog.info("▶ $label" + (if (versuch > 0) " (Wiederholung $versuch)" else "") + " · ${modell.apiId}/${stufe.name} · Websuche ${if (webSuche) "an" else "aus"}")
            try {
                val antwort = withTimeoutOrNull(ZEITLIMIT_MS) {
                    auth.streamChat(
                        instructions = anweisung,
                        turns = listOf(ChatTurn("user", text.trim())),
                        model = modell,
                        reasoningEffort = stufe,
                        webSuche = webSuche,
                        onDelta = { stueck -> empfangen += stueck.length; onDelta(stueck) },
                    )
                } ?: throw IOException("Keine vollständige Antwort nach ${ZEITLIMIT_MS / 60_000} Minuten")
                KiLog.info("✔ $label nach ${(System.currentTimeMillis() - start) / 1000}s · ${antwort.length} Zeichen")
                return antwort
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                val abriss = t is IOException || (t is CodexAuthException && t.kind == AuthErrorKind.NETWORK && (t.retryable || t.cause is IOException))
                val nochmal = abriss && versuch < WIEDERHOLUNGEN_MS.size
                KiLog.fehler("✖ $label nach ${(System.currentTimeMillis() - start) / 1000}s · $empfangen Zeichen · ${if (nochmal) "neuer Versuch" else "gebe auf"}", t)
                if (!nochmal) throw t
                delay(WIEDERHOLUNGEN_MS[versuch])
                versuch++
            }
        }
    }

    private fun heute(): String = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date())

    companion object {
        private const val ZEITLIMIT_MS = 9 * 60_000L
        private val WIEDERHOLUNGEN_MS = longArrayOf(4_000L, 15_000L)
    }
}

/** Holt das erste vollständige JSON-Objekt aus einer KI-Antwort (auch mit Codeblock oder Text drumherum). */
fun jsonAus(text: String): JSONObject {
    val s = text.replace("```json", "").replace("```", "")
    val start = s.indexOf('{')
    if (start < 0) throw IllegalStateException("Die KI hat keine auswertbare Antwort geliefert.")
    var tiefe = 0
    var inString = false
    var escape = false
    for (i in start until s.length) {
        val c = s[i]
        if (inString) {
            when {
                escape -> escape = false
                c == '\\' -> escape = true
                c == '"' -> inString = false
            }
            continue
        }
        when (c) {
            '"' -> inString = true
            '{' -> tiefe++
            '}' -> if (--tiefe == 0) return JSONObject(s.substring(start, i + 1))
        }
    }
    throw IllegalStateException("Die Antwort der KI war unvollständig.")
}
