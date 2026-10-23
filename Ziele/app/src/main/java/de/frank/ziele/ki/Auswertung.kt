package de.frank.ziele.ki

import android.content.Context
import android.util.Log
import de.frank.ziele.auth.ChatTurn
import de.frank.ziele.auth.CodexAuthManager
import de.frank.ziele.bruecke.Jarvis
import de.frank.ziele.data.Aufwand
import de.frank.ziele.data.Einstellungen
import de.frank.ziele.data.Kategorie
import de.frank.ziele.data.Kontext
import de.frank.ziele.data.Vorschlag
import de.frank.ziele.data.Ziel
import de.frank.ziele.data.ZieleRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Der Aufgaben-Agent: recherchiert zuerst, wie ein Ziel am besten erreicht wird, und schlägt darauf aufbauend
 * notwendige und unterstützende Aufgaben vor, zugeschnitten auf Frank (Kontextdatei + Jarvis' Live-Gedächtnis).
 *
 * Läuft im Prozess-Bereich, nicht im ViewModel: Verlässt Frank den Bildschirm, rechnet die Auswertung weiter
 * und das Ergebnis landet trotzdem in der Datenbank. Auch Jarvis stößt sie über die Brücke an.
 */
object Auswertung {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val laeufe = mutableMapOf<Long, Job>()

    /** Ziel-id → Schritt, der gerade läuft (für die Fortschrittsanzeige). */
    private val _stand = MutableStateFlow<Map<Long, String>>(emptyMap())
    val stand: StateFlow<Map<Long, String>> = _stand.asStateFlow()

    /** Ziel-id → letzte Fehlermeldung (verschwindet beim nächsten Start). */
    private val _fehler = MutableStateFlow<Map<Long, String>>(emptyMap())
    val fehler: StateFlow<Map<Long, String>> = _fehler.asStateFlow()

    fun laeuft(zielId: Long): Boolean = _stand.value.containsKey(zielId)

    /** Startet die Auswertung; läuft schon eine für dieses Ziel, passiert nichts. [fertig] meldet Erfolg oder Fehlertext. */
    @Synchronized
    fun starte(context: Context, zielId: Long, wunsch: String = "", fertig: (String?) -> Unit = {}): Boolean {
        if (laeufe[zielId]?.isActive == true) return false
        val app = context.applicationContext
        _fehler.update { it - zielId }
        schritt(zielId, "Bereite vor …")
        laeufe[zielId] = bereich.launch {
            val ergebnis = try {
                rechne(app, zielId, wunsch)
                null
            } catch (c: CancellationException) {
                "Abgebrochen."
            } catch (e: Throwable) {
                Log.w("ZieleAuswertung", "Auswertung $zielId", e)
                (e.message ?: e.javaClass.simpleName).also { m -> _fehler.update { it + (zielId to m) } }
            } finally {
                _stand.update { it - zielId }
                synchronized(this@Auswertung) { laeufe.remove(zielId) }
            }
            fertig(ergebnis)
        }
        return true
    }

    fun abbrechen(zielId: Long) { synchronized(this) { laeufe[zielId] }?.cancel() }

    private fun schritt(zielId: Long, text: String) = _stand.update { it + (zielId to text) }

    private suspend fun rechne(app: Context, zielId: Long, wunsch: String) {
        val repo = ZieleRepository.get(app)
        val e = Einstellungen.get(app)
        val auth = CodexAuthManager(app)
        val modell = e.modell
        if (!auth.kannDenken(modell)) {
            throw IllegalStateException(if (modell.istAnthropic) "Für ${modell.label} fehlt der Anthropic-API-Schlüssel (Einstellungen → KI)." else "Bitte zuerst in den Einstellungen mit ChatGPT verbinden.")
        }
        val ziel = repo.ziel(zielId) ?: throw IllegalStateException("Das Ziel gibt es nicht mehr.")
        val bisher = repo.vorschlaege(zielId)

        val persoenlich = e.kontextNutzen
        if (persoenlich) schritt(zielId, if (e.jarvisKontext) "Lese deinen Kontext und Jarvis' Live-Gedächtnis …" else "Lese deinen Kontext …")
        val kontextDatei = if (persoenlich) Kontext.lies(app).trim() else ""
        val gedaechtnis = if (persoenlich && e.jarvisKontext) runCatching { Jarvis.gedaechtnis(app) }.getOrNull()?.take(24_000) else null

        val ueberFrank = buildString {
            append("HEUTE: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN))).append("\n\n")
            if (!persoenlich) append(
                "OHNE PERSÖNLICHEN KONTEXT: Frank möchte diesmal allgemeine Empfehlungen, die für jeden Menschen mit diesem Ziel gelten. " +
                    "Stimme nichts auf seine persönliche Lage, seinen Alltag oder seine Daten ab.\n\n",
            ) else if (kontextDatei.isNotEmpty()) append("KONTEXTDATEI (von Frank selbst eingesprochen):\n").append(kontextDatei.takeLast(12_000)).append("\n\n")
            if (gedaechtnis != null) append("JARVIS-LIVE-GEDÄCHTNIS (Franks persönlicher Assistent, aus allen seinen Apps):\n").append(gedaechtnis).append("\n\n")
            if (persoenlich && kontextDatei.isEmpty() && gedaechtnis == null) append("(Kein weiterer Kontext über Frank vorhanden.)\n\n")
        }
        val zielText = zielBeschreibung(ziel)

        // ---- Schritt 1: Recherche ----
        schritt(zielId, if (e.recherche) "Recherchiere, wie man das Ziel am besten erreicht …" else "Denke über den besten Weg nach …")
        val bericht = auth.streamChat(
            instructions = RECHERCHE_ANWEISUNG + if (e.recherche) RECHERCHE_MIT_WEB else RECHERCHE_OHNE_WEB,
            turns = listOf(ChatTurn("user", "$zielText\n\n$ueberFrank" + if (wunsch.isNotBlank()) "BESONDERER WUNSCH FÜR DIESE AUSWERTUNG: $wunsch\n" else "")),
            model = modell,
            reasoningEffort = e.denkstufe,
            websuche = e.recherche,
        ).trim()
        if (bericht.isBlank()) throw IllegalStateException("Die Recherche hat keinen Bericht geliefert.")

        // ---- Schritt 2: Aufgaben ableiten ----
        schritt(zielId, "Leite die wichtigsten Aufgaben ab …")
        val behalten = bisher.filter { it.aufgabeId != null || it.verworfen }
        val menge = when (e.vorschlagMenge) { "knapp" -> "5 bis 8"; "ausfuehrlich" -> "14 bis 22"; else -> "8 bis 14" }
        val planAnfrage = buildString {
            append(zielText).append("\n\n")
            append("RECHERCHE-BERICHT:\n").append(bericht.take(30_000)).append("\n\n")
            append(ueberFrank)
            if (behalten.isNotEmpty()) {
                append("SCHON VORHANDEN (nicht noch einmal vorschlagen; du darfst sie als Voraussetzung über ihren Schlüssel nennen):\n")
                behalten.forEach { v ->
                    append("- [").append(v.schluessel).append("] ").append(v.titel)
                    append(when { v.verworfen -> " (von Frank verworfen, nichts Ähnliches mehr vorschlagen)"; v.erledigt -> " (erledigt)"; else -> " (übernommen, in Arbeit)" })
                    append('\n')
                }
                append('\n')
            }
            if (wunsch.isNotBlank()) append("BESONDERER WUNSCH: ").append(wunsch).append("\n\n")
            append("Liefere etwa $menge neue Vorschläge.")
        }
        val antwort = auth.streamChat(
            instructions = PLAN_ANWEISUNG,
            turns = listOf(ChatTurn("user", planAnfrage)),
            model = modell,
            reasoningEffort = e.denkstufe,
        )
        val plan = jsonAus(antwort) ?: throw IllegalStateException("Die KI hat keinen lesbaren Aufgabenplan geliefert. Bitte noch einmal auswerten.")

        schritt(zielId, "Speichere die Vorschläge …")
        val belegt = behalten.map { it.schluessel }.toMutableSet()
        val roh = plan.optJSONArray("vorschlaege") ?: JSONArray()
        val objekte = (0 until roh.length()).mapNotNull { roh.optJSONObject(it) }.filter { it.optString("titel").isNotBlank() }
        // Vergibt die KI einen schon belegten Schlüssel neu, wird er umbenannt; Voraussetzungen unter den neuen Vorschlägen
        // meinen dann den neuen Vorschlag und ziehen mit (der erste Vorschlag mit diesem Schlüssel gewinnt).
        val umbenannt = mutableMapOf<String, String>()
        val schluesselJe = objekte.map { o ->
            val gewuenscht = o.optString("schluessel").trim().ifEmpty { "v${belegt.size + 1}" }
            var schluessel = gewuenscht
            while (schluessel in belegt) schluessel += "b"
            belegt += schluessel
            if (schluessel != gewuenscht) umbenannt.putIfAbsent(gewuenscht, schluessel)
            schluessel
        }
        val neu = objekte.mapIndexed { i, o ->
            // Sicherheitsnetz: „Kläre du deinen …“ → „Kläre deinen …“ (Imperativ mit überflüssigem „du“).
            val titel = o.optString("titel").trim().trim('„', '“', '"').replace(DU_NACH_VERB, "$1 ").trimEnd('.')
            val schluessel = schluesselJe[i]
            val vor = o.optJSONArray("voraussetzungen")?.let { a -> (0 until a.length()).map { a.optString(it).trim() }.filter { it.isNotEmpty() } }.orEmpty()
                .map { umbenannt[it] ?: it }.filter { it != schluessel }
            Vorschlag(
                zielId = zielId,
                schluessel = schluessel,
                titel = titel.take(80),
                text = o.optString("text").trim(),
                begruendung = o.optString("begruendung").trim(),
                kategorie = Kategorie.von(o.optString("kategorie")).name,
                wichtigkeit = o.optInt("wichtigkeit", 3).coerceIn(1, 5),
                aufwand = Aufwand.von(o.optString("aufwand")).name,
                minuten = o.optInt("minuten", 60).coerceIn(5, 100 * 60),
                voraussetzungenJson = Vorschlag.liste(vor),
                phase = o.optInt("phase", 2).coerceIn(1, 3),
                erfolg = o.optString("erfolg").trim(),
                wiederkehrend = o.optBoolean("wiederkehrend", false),
            )
        }
        if (neu.isEmpty()) throw IllegalStateException("Die KI hat keine Aufgaben vorgeschlagen. Bitte noch einmal auswerten.")
        // Unter der Sperre des Übernehmens; ist das Ziel inzwischen gelöscht, nichts schreiben (sonst verwaiste Vorschläge).
        val frisch = Abgleich.gesperrt {
            repo.ziel(zielId)?.also { repo.vorschlaegeErsetzen(zielId, neu) }
        } ?: return
        repo.speichere(
            frisch.copy(
                bericht = bericht,
                strategie = plan.optString("strategie").trim().ifEmpty { frisch.strategie },
                symbol = plan.optString("symbol").trim().takeIf { it.isNotEmpty() && it.length <= 8 && frisch.symbol == "🎯" } ?: frisch.symbol,
                ausgewertetAm = System.currentTimeMillis(),
                auswertungModell = modell.label,
            ),
        )
    }

    fun zielBeschreibung(z: Ziel): String = buildString {
        append("ZIEL: ").append(z.titel).append('\n')
        if (z.beschreibung.isNotBlank()) append("GENAUER: ").append(z.beschreibung.trim()).append('\n')
        if (z.warum.isNotBlank()) append("WARUM ES FRANK WICHTIG IST: ").append(z.warum.trim()).append('\n')
        z.frist?.let { append("ZIELDATUM: ").append(LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN))).append('\n') }
    }

    /** Sucht das JSON-Objekt in der Antwort, auch wenn das Modell Codeblöcke oder Text drumherum schreibt. */
    internal fun jsonAus(antwort: String): JSONObject? {
        val ohneZaun = antwort.replace(Regex("```(?:json)?"), "")
        val anfang = ohneZaun.indexOf('{')
        val ende = ohneZaun.lastIndexOf('}')
        if (anfang < 0 || ende <= anfang) return null
        return runCatching { JSONObject(ohneZaun.substring(anfang, ende + 1)) }.getOrNull()
    }

    private val DU_NACH_VERB = Regex("^(\\p{L}+)\\s+du\\s+", RegexOption.IGNORE_CASE)

    private const val RECHERCHE_ANWEISUNG =
        "Du bist ein erstklassiger Coach und Rechercheur. Frank hat ein persönliches Ziel. Finde gründlich heraus, wie man genau dieses Ziel " +
            "am besten erreicht, und schneide alles auf Frank zu (Kontext unten: sein Alltag, seine Arbeit im Schichtdienst, seine Gesundheit, " +
            "seine anderen Ziele und Gewohnheiten). Schreibe auf Deutsch, du sprichst Frank mit „du“ an.\n\n" +
            "Gliedere den Bericht so (Überschriften als eigene Zeile, keine Markdown-Zeichen wie # oder **):\n" +
            "KERN: zwei bis vier Sätze, worauf es bei diesem Ziel wirklich ankommt.\n" +
            "WAS NACHWEISLICH WIRKT: die wirksamsten Hebel, möglichst belegt (Studien, Fachleute, bewährte Methoden), je mit einem Satz warum.\n" +
            "DER WEG: sinnvolle Etappen und Meilensteine bis zum Ziel, mit realistischen Zeiträumen.\n" +
            "FÜR DICH ZUGESCHNITTEN: was in Franks Lage besonders zählt, was ihm leichtfällt, was ihn bremsen könnte.\n" +
            "STOLPERSTEINE: typische Fehler und wie man sie vermeidet.\n" +
            "WORAN DU FORTSCHRITT MERKST: messbare Zeichen.\n\n" +
            "Erfinde nichts. Trenne Belegtes von Erfahrungswissen. Bleib konkret und praktisch; keine Floskeln."

    private const val RECHERCHE_MIT_WEB =
        "\n\nRecherchiere mit der Websuche, gern mehrfach und aus verschiedenen Blickwinkeln, bevor du schreibst. " +
            "Nenne die wichtigsten Quellen am Ende unter QUELLEN (Titel und Adresse, höchstens acht)."

    private const val RECHERCHE_OHNE_WEB = "\n\nStütze dich auf dein Fachwissen; eine Websuche steht dafür nicht zur Verfügung."

    private const val PLAN_ANWEISUNG =
        "Du bist Franks Aufgaben-Agent. Aus dem Recherche-Bericht und dem Kontext über Frank leitest du konkrete Aufgaben ab, " +
            "die ihn seinem Ziel nachweislich näherbringen. Jede Aufgabe muss in jedem Fall zur Zielerreichung beitragen; nichts Allgemeines, nichts Beliebiges. " +
            "Zuschnitt auf Frank: berücksichtige seinen Kontext (Dienstplan, Gesundheit, Vorlieben, vorhandene Ziele), damit die Aufgaben in sein Leben passen.\n\n" +
            "Zwei Gruppen:\n" +
            "- NOTWENDIG: wirklich wichtig, ohne diese Aufgaben wird das Ziel nicht erreicht.\n" +
            "- UNTERSTUETZEND: nicht unbedingt nötig, aber hilfreich (leichter, schneller, sicherer, angenehmer).\n\n" +
            "SPRACHE (sehr wichtig, Frank liest und hört jeden Vorschlag): einwandfreies, natürliches Deutsch, wie es ein guter Muttersprachler schreibt, " +
            "grammatisch korrekt und auf Anhieb verständlich, ohne Fachjargon und ohne Wortneuschöpfungen.\n" +
            "- Titel: eine klare, erledigbare Handlung als Aufforderung in der Du-Form, aber OHNE das Wort „du“ (Imperativ: „Kläre“, „Übe“, „Lies“, „Melde dich“). " +
            "Etwa drei bis acht Wörter, höchstens 60 Zeichen, ohne Punkt am Ende; lieber ein Wort mehr als unverständlich. " +
            "Der Titel muss für sich allein verständlich sein: Was genau ist zu tun?\n" +
            "  Gut: „Melde dich beim Lauftreff Köpenick an“, „Lies täglich zehn Minuten laut vor“, „Lass beim Hausarzt eine Logopädie verordnen“, „Nimm dich einmal pro Woche beim Erzählen auf“.\n" +
            "  Falsch: „Kläre du deinen Sprachverlauf“ (überflüssiges „du“, unklar), „Übe du acht Minuten klar sprechen“ (falsche Wortstellung), " +
            "„Ziehe du etwas Sprachbilanzen“ (kein Deutsch), „Kläre den logopädischen Bedarf“ (Amtsdeutsch, unklar, was zu tun ist).\n" +
            "- text, begruendung und erfolg: ganze, vollständige Sätze in der Du-Form, gut vorlesbar.\n" +
            "Prüfe jeden Titel und Satz vor der Ausgabe: Würde ein Deutschlehrer ihn so stehen lassen? Wenn nicht, formuliere ihn neu.\n\n" +
            "Wiederkehrende Gewohnheiten (z. B. „Laufe dreimal pro Woche 30 Minuten“) sind erlaubt und als wiederkehrend markiert.\n\n" +
            "Antworte NUR mit einem JSON-Objekt, ohne Text davor oder danach, genau in dieser Form:\n" +
            "{\n" +
            "  \"symbol\": \"ein passendes Emoji für das Ziel\",\n" +
            "  \"strategie\": \"ein bis drei Sätze: der beste Weg zum Ziel, für Frank zugeschnitten, gut vorlesbar\",\n" +
            "  \"vorschlaege\": [\n" +
            "    {\n" +
            "      \"schluessel\": \"v1\",\n" +
            "      \"titel\": \"Lies täglich zehn Minuten laut vor\",\n" +
            "      \"text\": \"ein bis drei Sätze: was genau zu tun ist, konkret\",\n" +
            "      \"begruendung\": \"ein Satz: warum das auf das Ziel einzahlt\",\n" +
            "      \"kategorie\": \"NOTWENDIG oder UNTERSTUETZEND\",\n" +
            "      \"wichtigkeit\": 1 bis 5 (5 = entscheidend für das Ziel),\n" +
            "      \"aufwand\": \"KLEIN, MITTEL oder GROSS\",\n" +
            "      \"minuten\": geschätzte Minuten für die Aufgabe (bei wiederkehrenden: pro Durchgang),\n" +
            "      \"phase\": 1 (sofort), 2 (bald) oder 3 (später),\n" +
            "      \"voraussetzungen\": [\"Schlüssel der Aufgaben, die vorher erledigt sein müssen\"],\n" +
            "      \"erfolg\": \"woran Frank merkt, dass die Aufgabe erledigt ist\",\n" +
            "      \"wiederkehrend\": true oder false\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "Schlüssel fortlaufend v1, v2, … (außer sie sind unter SCHON VORHANDEN belegt). Voraussetzungen ohne Kreise. " +
            "Etwa zwei Drittel notwendig, ein Drittel unterstützend. Sortiere nichts, das macht die App."
}
