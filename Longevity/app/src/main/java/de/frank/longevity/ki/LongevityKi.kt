package de.frank.longevity.ki

import de.frank.longevity.auth.AuthErrorKind
import de.frank.longevity.auth.ChatTurn
import de.frank.longevity.auth.CodexAuthException
import de.frank.longevity.auth.CodexAuthManager
import de.frank.longevity.auth.CodexModel
import de.frank.longevity.auth.ReasoningEffort
import de.frank.longevity.data.Einstellungen
import de.frank.longevity.data.Evidenz
import de.frank.longevity.data.Faktor
import de.frank.longevity.data.Kategorie
import de.frank.longevity.data.Punkt
import de.frank.longevity.data.Quelle
import de.frank.longevity.data.ordnen
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.roundToInt

/** Ergebnis der Auswertung einer eigenen Idee. */
data class Auswertung(val faktor: Faktor, val rang: Int, val bewertung: String, val duplikatVon: Long?)

/** Ergebnis einer Aktualisierung: neue Reihenfolge (mit Altbestand gemischt) und Vorschläge. */
data class Aktualisierung(
    val liste: List<Faktor>,
    val vorschlaege: List<Faktor>,
    val zusammenfassung: String,
    val veraendert: Int,
    /** Beim Mitdiskutieren: die Einordnung des Nutzer-Beitrags durch die Gutachterin. */
    val einordnung: String = "",
    /** Anzahl der in diesem Lauf neu bewerteten Faktoren. */
    val bewertet: Int = 0,
    /** Anzahl neu geschriebener Texte. */
    val ueberarbeitet: Int = 0,
    /** Anzahl neuer Hinweise des Mediziners (Überschneidungen, Widerlegtes). */
    val hinweise: Int = 0,
    /** Platzwechsel als lesbare Zeilen („„Titel“: Platz 27 → 9“), größte Sprünge zuerst. */
    val wechsel: List<String> = emptyList(),
)

/**
 * Alle KI-Arbeiten der App. Die Hauptarbeit läuft mit dem Modell aus den Einstellungen
 * (Standard GPT-6 Astra, Hoch), die Textkorrektur mit einem eigenen, meist schnelleren Modell.
 */
class LongevityKi(
    private val auth: CodexAuthManager,
    private val e: Einstellungen,
    /** Der Standard-Prompt der Aktualisierung (assets/aktualisierung.md). */
    private val standardVorlage: () -> String,
    /** Zwischenstand des großen Laufs, damit ein Abbruch nicht alles verwirft. */
    val speicher: LaufSpeicher,
) {

    // ================= Textkorrektur =================

    suspend fun korrigiere(text: String, bisher: List<String>): String {
        val anweisung = buildString {
            append(
                "Der Text ist eine Eingabe für eine Langlebigkeits-App – diktiert per Spracherkennung oder schnell getippt – und " +
                    "deshalb unsauber. Erkenne die Absicht und gib genau diesen Inhalt in klarem, sehr gutem Deutsch wieder. " +
                    "Korrigiere Grammatik, Rechtschreibung, Satzbau und Wortwahl, entferne Versprecher, Füllwörter und Wiederholungen. " +
                    "Fachbegriffe (z. B. VO2max, Kalorienrestriktion, Rapamycin, ApoB, Omega-3) richtig schreiben. " +
                    "Füge NICHTS Inhaltliches hinzu und lass nichts weg. Zahlen und Namen exakt behalten. " +
                    "Antworte nur mit dem verbesserten Text, ohne Vorrede und ohne Anführungszeichen.",
            )
            if (bisher.isNotEmpty()) {
                append("\n\nDiese Fassungen gab es schon. Liefere eine deutlich andere Formulierung bei gleichem Inhalt:\n")
                bisher.forEach { append("- ").append(it.take(600)).append('\n') }
            }
        }
        return frage(anweisung, text, e.korrekturModell, e.korrekturDenkstufe).trim().trim('„', '“', '"')
    }

    // ================= Eigene Idee auswerten =================

    suspend fun auswerten(idee: String, liste: List<Faktor>, fortschritt: Fortschritt): Auswertung {
        fortschritt.band(0f, 1f, erwarteteZeichen = 3500, erwarteteSekunden = 45f, schritt = "Die KI prüft deine Idee …")
        val antwort = frage(
            grundanweisung(),
            """
            |AKTUELLE LISTE (id | Rang | Titel | Kategorie | Evidenz | Wahrscheinlichkeit | Jahre; oben Plus-Faktoren, unter der Null-Linie die
            |Lebenszeit-Räuber mit negativen Jahren, der schädlichste ganz unten):
            |${listeKompakt(liste)}
            |
            |NEUE IDEE DES NUTZERS:
            |${idee.trim()}
            |
            |AUFGABE: Prüfe die Idee kritisch und ehrlich nach aktuellem Forschungsstand und nach Logik. Formuliere daraus
            |einen Faktor für die Liste und ordne ihn nach seiner Wichtigkeit (Gewinn an gesunder Lebenszeit) in die Liste ein:
            |"rang" ist die Position, an der er eingefügt wird (1 bis ${liste.size + 1}). Vergleiche dazu direkt mit den
            |Nachbarn darüber und darunter. Beschreibt die Idee ein schädliches Verhalten oder ein Verbot („nicht rauchen“,
            |„weniger Zucker“), formuliere den Faktor als das schädliche Verhalten selbst („Rauchen“, „Viel Zucker essen“) mit
            |NEGATIVEN Jahren – er wird dann automatisch unter der Null-Linie nach verlorenen Jahren einsortiert. Ist die Idee im Kern schon als Faktor vorhanden, setze "duplikatVon" auf dessen id
            |und liefere in "ergaenzung" nur das, was die Idee Neues beiträgt.
            |
            |Antworte NUR mit einem JSON-Objekt, ohne Text davor oder danach:
            |{"bewertung": "2–3 Sätze: Stimmt die Idee? Wie gut belegt? Was ist dran, was nicht?",
            | "duplikatVon": null, "ergaenzung": "",
            | ${FAKTOR_SCHEMA},
            | "rang": 5}
            """.trimMargin(),
            e.modell, e.denkstufe, fortschritt, webSuche = webSuche, label = "Auswertung eigene Idee",
        )
        val o = jsonAus(antwort)
        val f = faktorAus(o).copy(eigen = true, notiz = idee.trim(), neu = true, vertieft = true)
        val dup = o.optLong("duplikatVon", -1L).takeIf { id -> id > 0 && liste.any { it.id == id } }
        val bewertung = o.optString("bewertung").trim()
        val ergaenzung = o.optString("ergaenzung").trim()
        return Auswertung(
            faktor = if (dup != null) f.copy(erklaerung = ergaenzung.ifBlank { f.erklaerung }) else f,
            rang = o.optInt("rang", liste.size + 1).coerceIn(1, liste.size + 1),
            bewertung = bewertung,
            duplikatVon = dup,
        )
    }

    // ================= Einen Faktor vertiefen =================

    suspend fun vertiefen(f: Faktor, liste: List<Faktor>, fortschritt: Fortschritt): Faktor {
        auth.webSucheZuruecksetzen()
        fortschritt.band(0f, 1f, erwarteteZeichen = 5500, erwarteteSekunden = if (webSuche) 120f else 60f, schritt = "Die KI recherchiert „${f.titel}“ …")
        val antwort = frage(
            grundanweisung(),
            """
            |HEUTE: ${heute()}
            |GESAMTLISTE zur Einordnung:
            |${listeKompakt(liste)}
            |
            |ZU VERTIEFEN:
            |${detail(f)}
            |${if (f.notiz.isNotBlank()) "Ursprüngliche Eingabe des Nutzers: ${f.notiz}" else ""}
            |
            |AUFGABE: Vertiefe diesen Faktor sehr gründlich. ${if (webSuche) "Recherchiere dazu im Internet den aktuellen Forschungsstand (neue Metaanalysen, große Kohorten, RCTs, Mendel-Randomisierung, Leitlinien)." else ""}
            |Verbessere den Bestand, statt ihn zu verwerfen: Was stimmt, bleibt sinngemäß erhalten; korrigiere Fehler und Veraltetes und
            |ergänze neue Erkenntnisse. Absätze der Form „Neu (Datum): …“ arbeitest du in einen einzigen, stimmigen Fließtext ein – danach
            |gibt es keine „Neu (…)“-Absätze mehr. Bring es auf den Punkt – 3–5 kurze, verständliche Sätze, je Erkenntnis ein Satz: welche Verhaltensweise gemeint ist, warum sie wirkt (Mechanismus) und was
            |die Forschung zeigt (mit Zahlen). Begründe, warum der Faktor genau auf
            |${if (f.raeuber) "seinem Platz unter der Null-Linie (Lebenszeit-Räuber, ${"%.1f".format(Locale.US, f.jahre)} Jahre)" else "Rang ${f.rang}"} steht.
            |Ist der Faktor ein Lebenszeit-Räuber, beschreibt der Titel das schädliche Verhalten, "jahre" bleibt negativ, und Ziel
            |und Aufgabenplan zeigen, wie man es abstellt. Leite daraus ein klares, messbares persönliches Ziel ab und einen Aufgabenplan mit 5–7 Punkten (je 1 Satz),
            |sortiert nach Wichtigkeit (was man sofort umsetzen sollte, zuerst). Ist der Faktor eine Sammelkategorie (z. B.
            |Supplements, Lebensmittel, Übungen), sind die Punkte die einzelnen Elemente (z. B. die einzelnen Supplements, 8–12 Stück),
            |breit gestreut von gut belegt bis logisch plausibel, jeweils ehrlich eingeordnet, das wichtigste zuerst.
            |Nenne in "quellen" 3–8 tragende Quellen (Studie/Metaanalyse/Leitlinie mit Jahr, Link wenn sicher bekannt) – nur echte, nie erfundene.
            |
            |Antworte NUR mit einem JSON-Objekt:
            |{ ${FAKTOR_SCHEMA} }
            """.trimMargin(),
            e.modell, e.denkstufe, fortschritt, webSuche = webSuche, label = "Vertiefung id ${f.id}",
        )
        val neu = faktorAus(jsonAus(antwort))
        return f.copy(
            titel = neu.titel.ifBlank { f.titel },
            kurz = neu.kurz.ifBlank { f.kurz },
            kategorie = neu.kategorie,
            evidenz = neu.evidenz,
            jahre = if (neu.jahre != 0f) neu.jahre else f.jahre,
            wirkung = neu.wirkung,
            wahrscheinlichkeit = neu.wahrscheinlichkeit ?: f.wahrscheinlichkeit,
            erklaerung = neu.erklaerung.ifBlank { f.erklaerung },
            begruendung = neu.begruendung.ifBlank { f.begruendung },
            ziel = neu.ziel.ifBlank { f.ziel },
            punkteJson = Faktor.punkteAlsJson(punkteMischen(f.punkte, neu.punkte)),
            quellenJson = Faktor.quellenAlsJson(Faktor.quellenMischen(f.quellen, neu.quellen)),
            standVom = System.currentTimeMillis(),
            vertieft = true,
        )
    }

    // ================= Aktualisierung: ein Mediziner, die App orchestriert =================

    /** Stufe des Reglers „sparsam ↔ maximal“ (1–5). */
    private val stufe: Int get() = e.aktualisierungsStufe

    /** Websuche für Auswertung, Vertiefung, Neuheiten und Mitreden – nur Stufe 1 arbeitet ganz ohne Internet. */
    private val webSuche: Boolean get() = stufe >= 2

    /** Websuche auch in der Bewertung der Blöcke (ab Stufe 3). */
    private val webBewertung: Boolean get() = stufe >= 3

    /** Faktoren je Bewertungs-Block: je gründlicher, desto kleiner. */
    private val blockGroesse: Int get() = intArrayOf(12, 10, 8, 6, 4)[stufe - 1]

    /** Die Websuche war eingeschaltet, wurde vom Server aber abgelehnt – der Lauf arbeitete ohne Internet. */
    val webSucheAbgelehnt: Boolean get() = webSuche && !auth.webSucheMoeglich

    /**
     * Ein Kreislauf der Aktualisierung. Die App ist der Orchestrator, der Mediziner der einzige Agent:
     * 1. Neuheiten (1 Aufruf, Websuche): Befunde zu vorhandenen Faktoren und neue Kandidaten (Schenker und Räuber).
     * 2. Bewertung (parallel in kleinen Blöcken): Potenzial, Wahrscheinlichkeit, Jahre und Urteil je Faktor.
     * 3. Rangfolge: rechnet der Code aus den Jahren – kein Aufruf muss mehr die ganze Liste schreiben.
     * 4. Texte (parallel): nur Faktoren mit neuen Erkenntnissen, alter Text + Neues = ein kurzer Text.
     * 5. Neue Faktoren (parallel): die Kandidaten werden ausgearbeitet und als Vorschläge eingeordnet.
     * Scheitert ein einzelner Aufruf, bleibt der alte Stand dieses Teils erhalten und der Lauf geht weiter.
     */
    suspend fun aktualisieren(liste: List<Faktor>, fortschritt: Fortschritt): Aktualisierung {
        auth.webSucheZuruecksetzen()
        val v = vorlage()
        val stand = speicher.beginnen("stufe$stufe", liste.map { it.id })
        val bloecke = liste.chunked(blockGroesse)
        KiLog.info(
            "═══ Aktualisierung startet: ${liste.size} Faktoren in ${bloecke.size} Blöcken · Stufe $stufe · ${e.modell.apiId}/${e.denkstufe.name} · " +
                (if (e.aktualisierungsPrompt.isBlank()) "Standard-Prompt" else "eigener Prompt") + " · ${stand.schritte.size} Schritte schon gesichert",
        )
        fortschritt.beitragFertig(
            ORCHESTRATOR,
            if (stand.schritte.isNotEmpty()) "Fortsetzung: ${stand.schritte.size} fertige Schritte werden übernommen."
            else "Neuer Kreislauf: ${liste.size} Faktoren in ${bloecke.size} Blöcken zu höchstens $blockGroesse, " +
                "Websuche ${if (webBewertung) "in Neuheiten und Bewertung" else if (webSuche) "nur für die Neuheiten" else "aus"}.",
        )
        mitSpeicher = true
        laufKennung = "longevity-lauf-${stand.beginn}"
        try {
            // 1. Neuheiten
            fortschritt.phase(0f, 0.12f, 1, 90f, "Der Mediziner sucht neue Forschung …")
            fortschritt.status(SCOUT, "Sucht neue Studien, Schenker und Räuber …")
            var scoutGespeichert = false
            val neuheiten = try {
                val (text, gespeichert) = gesichert("scout:${if (webSuche) "web" else "ohne"}", pruefe = { jsonAus(it) }) {
                    frage(rolle(v), nachricht(liste, fuelle(v["neuheiten"].orEmpty(), werte())), e.modell, denk("scout"), fortschritt, webSuche = webSuche, still = true, label = "Neuheiten")
                }
                scoutGespeichert = gespeichert
                jsonAus(text)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                KiLog.fehler("Neuheiten gescheitert – der Lauf bewertet ohne sie weiter", t)
                null
            }
            val befunde = befundeAus(neuheiten, liste)
            val kandidaten = kandidatenAus(neuheiten?.optJSONArray("neu"), liste)
            if (!scoutGespeichert) fortschritt.beitragFertig(SCOUT, neuheitenText(befunde, kandidaten, liste, neuheiten == null), liveLeeren = false)
            fortschritt.schrittFertig()

            // 2. Bewertung in Blöcken
            val nachId = liste.associateBy { it.id }
            val urteile = java.util.concurrent.ConcurrentHashMap<Long, Urteil>()
            val gescheitert = AtomicInteger(0)
            fortschritt.phase(0.12f, 0.62f, bloecke.size, 30f, "Bewertung: ${liste.size} Faktoren in ${bloecke.size} Blöcken …")
            fortschritt.status(ORCHESTRATOR, "Bewertet ${bloecke.size} Blöcke, bis zu $PARALLEL gleichzeitig …")
            val grenze = Semaphore(PARALLEL)
            val fertig = AtomicInteger(0)
            coroutineScope {
                bloecke.map { block ->
                    async {
                        grenze.withPermit {
                            val bereich = "Rang ${block.first().rang}–${block.last().rang}"
                            val schluessel = "block:${block.first().id}-${block.last().id}-${block.size}"
                            val befundText = block.flatMap { f -> befunde[f.id].orEmpty().map { "- id ${f.id}: $it" } }.joinToString("\n").ifBlank { "(keine)" }
                            val auftrag = fuelle(
                                v["bewertung"].orEmpty(),
                                werte() + mapOf("BLOCK" to block.joinToString("\n\n") { detail(it, kompakt = true) }, "BEFUNDE" to befundText),
                            )
                            var ausSpeicher = false
                            val zeilen = try {
                                val (text, gespeichert) = gesichert(schluessel, pruefe = { t ->
                                    // Mindestens die Hälfte des eigenen Blocks muss bewertet sein, sonst gilt die Antwort als gescheitert.
                                    val treffer = urteileAus(jsonAus(t), nachId).keys.count { id -> block.any { it.id == id } }
                                    require(treffer * 2 >= block.size) { "nur $treffer von ${block.size} Faktoren bewertet" }
                                }) {
                                    frage(rolle(v), nachricht(liste, auftrag), e.modell, denk("bewertung"), fortschritt, webSuche = webBewertung, still = true, label = "Bewertung $bereich")
                                }
                                ausSpeicher = gespeichert
                                val gelesen = urteileAus(jsonAus(text), nachId).filterKeys { id -> block.any { it.id == id } }
                                urteile.putAll(gelesen)
                                block.map { f -> gelesen[f.id]?.let { u -> urteilZeile(f, anwenden(f, u), u) } ?: "• „${f.titel}“: nicht bewertet – alter Stand bleibt" }
                            } catch (c: CancellationException) {
                                throw c
                            } catch (t: Throwable) {
                                gescheitert.incrementAndGet()
                                KiLog.fehler("Bewertung $bereich gescheitert – alter Stand bleibt", t)
                                listOf("Dieser Block kam nicht zustande (${t.message?.take(120)}). Die Faktoren behalten ihren bisherigen Stand.")
                            }
                            if (!ausSpeicher) fortschritt.beitragFertig("$MEDIZINER · $bereich", zeilen.joinToString("\n"), liveLeeren = false)
                            fortschritt.schrittFertig("Bewertung: ${fertig.incrementAndGet()} von ${bloecke.size} Blöcken fertig …")
                        }
                    }
                }.awaitAll()
            }
            if (urteile.isEmpty() && liste.isNotEmpty()) throw IllegalStateException("Kein Block wurde bewertet – bitte Verbindung und Kontingent prüfen und erneut starten.")

            // 3. Rangfolge
            val jetzt = System.currentTimeMillis()
            val bewertet = liste.map { f -> urteile[f.id]?.let { anwenden(f, it) } ?: f }
            val neueListe = rangfolge(bewertet).map { f -> f.copy(vorherRang = nachId[f.id]?.rang, neu = false, geaendertAm = jetzt) }
            val wechsel = neueListe.filter { it.vorherRang != null && it.rang != it.vorherRang }
                .sortedByDescending { abs(it.rang - (it.vorherRang ?: it.rang)) }
                .map { "„${it.titel}“: Platz ${it.vorherRang} → ${it.rang}" }
            fortschritt.beitragFertig(
                ORCHESTRATOR,
                "Rangfolge nach Erwartungswert berechnet: " + if (wechsel.isEmpty()) "keine Platzwechsel." else "${wechsel.size} Platzwechsel.\n" + wechsel.take(12).joinToString("\n"),
                liveLeeren = false,
            )

            // 4. Texte
            val auftraege = textAuftraege(liste, neueListe, urteile, befunde)
            val liste2 = texte(v, neueListe, auftraege, fortschritt, 0.62f, 0.9f)

            // 5. Neue Faktoren
            val vorschlaege = neueFaktoren(v, liste2, kandidaten, fortschritt, 0.9f, 0.99f)

            val korrigiert = urteile.values.count { it.korrigiert }
            val hinweise = urteile.values.count { it.hinweis.isNotBlank() }
            val zusammenfassung = "${urteile.size} von ${liste.size} Faktoren neu bewertet ($korrigiert korrigiert, ${urteile.size - korrigiert} bestätigt)" +
                (if (gescheitert.get() > 0) ", ${gescheitert.get()} Blöcke ohne Antwort – sie behalten ihren alten Stand" else "") + "."
            KiLog.info("═══ Aktualisierung fertig: $zusammenfassung ${wechsel.size} Platzwechsel, ${auftraege.size} Texte, ${vorschlaege.size} Vorschläge")
            return Aktualisierung(
                liste = liste2, vorschlaege = vorschlaege, zusammenfassung = zusammenfassung, veraendert = wechsel.size,
                bewertet = urteile.size, ueberarbeitet = auftraege.size, hinweise = hinweise, wechsel = wechsel,
            )
        } finally {
            mitSpeicher = false
            laufKennung = null
        }
    }

    /**
     * Der Nutzer redet mit: EIN Aufruf des Mediziners prüft den Beitrag und ändert nur die betroffenen Faktoren;
     * deren Texte werden danach neu geschrieben, neue Faktoren erscheinen als Vorschläge.
     */
    suspend fun einwand(liste: List<Faktor>, text: String, bisher: List<Beitrag>, fortschritt: Fortschritt): Aktualisierung {
        auth.webSucheZuruecksetzen()
        val v = vorlage()
        laufKennung = "longevity-einwand-${System.currentTimeMillis()}"
        try {
            fortschritt.beitragFertig(NUTZER, text)
            fortschritt.phase(0f, 0.5f, 1, 90f, "Der Mediziner prüft deinen Beitrag …")
            fortschritt.status(ANTWORT, "Prüft deinen Beitrag …")
            // Nur die letzten Wortwechsel mit dem Nutzer, gekürzt – nicht der ganze Lauf.
            val gespraech = bisher.filter { it.name == NUTZER || it.name == ANTWORT }.takeLast(4)
                .joinToString("\n") { "- ${it.name}: ${it.text.take(500)}" }
            val auftrag = (if (gespraech.isNotBlank()) "BISHERIGES GESPRÄCH (gekürzt):\n$gespraech\n\n" else "") +
                fuelle(v["mitreden"].orEmpty(), werte() + ("EINWAND" to text))
            val antwort = frage(rolle(v), nachricht(liste, auftrag), e.modell, e.denkstufe, fortschritt, webSuche = webSuche, still = true, label = "Mitreden")
            val o = jsonAus(antwort)
            val nachId = liste.associateBy { it.id }
            val urteile = urteileAus(o, nachId, feld = "aenderungen")
            val einordnung = o.optString("einordnung").trim()
            fortschritt.beitragFertig(ANTWORT, einordnung.ifBlank { "Geprüft – an der Rangliste ändert sich nichts." })
            fortschritt.schrittFertig()
            val jetzt = System.currentTimeMillis()
            val neueListe = rangfolge(liste.map { f -> urteile[f.id]?.let { anwenden(f, it) } ?: f })
                .map { f -> f.copy(vorherRang = nachId[f.id]?.rang, neu = false, geaendertAm = if (f.id in urteile) jetzt else f.geaendertAm) }
            val wechsel = neueListe.filter { it.vorherRang != null && it.rang != it.vorherRang }
                .sortedByDescending { abs(it.rang - (it.vorherRang ?: it.rang)) }
                .map { "„${it.titel}“: Platz ${it.vorherRang} → ${it.rang}" }
            val auftraege = textAuftraege(liste, neueListe, urteile, emptyMap(), mitAltlasten = false)
            val liste2 = texte(v, neueListe, auftraege, fortschritt, 0.5f, 0.85f)
            val vorschlaege = neueFaktoren(v, liste2, kandidatenAus(o.optJSONArray("neu"), liste), fortschritt, 0.85f, 0.99f)
            return Aktualisierung(
                liste = liste2, vorschlaege = vorschlaege, zusammenfassung = einordnung, veraendert = wechsel.size, einordnung = einordnung,
                bewertet = urteile.size, ueberarbeitet = auftraege.size, hinweise = urteile.values.count { it.hinweis.isNotBlank() }, wechsel = wechsel,
            )
        } finally {
            laufKennung = null
        }
    }

    /** Cache-Kennung des laufenden großen Laufs bzw. Mitredens; null = Einzelaufruf mit Priority. */
    @Volatile private var laufKennung: String? = null

    /** Während des großen Laufs: jeder fertige Schritt wird gesichert und bei einem Neustart übernommen. */
    @Volatile private var mitSpeicher = false

    /**
     * Ein Schritt des großen Laufs: kommt aus dem Zwischenstand, falls er schon erledigt ist, sonst wird er erarbeitet,
     * mit [pruefe] geprüft (wirft bei unbrauchbarer Antwort) und sofort gesichert. Das zweite Feld ist true bei „aus dem Speicher“.
     */
    private suspend fun gesichert(schluessel: String, pruefe: (String) -> Unit = {}, block: suspend () -> String): Pair<String, Boolean> {
        if (mitSpeicher) speicher.hole(schluessel)?.let { alt ->
            if (runCatching { pruefe(alt) }.isSuccess) {
                KiLog.info("↺ $schluessel aus dem Zwischenstand übernommen (${alt.length} Zeichen)")
                return alt to true
            }
            speicher.entferne(schluessel)
        }
        val text = block()
        try {
            pruefe(text)
        } catch (t: Throwable) {
            KiLog.fehler("Antwort für $schluessel unbrauchbar (${text.length} Zeichen), Anfang: ${text.take(300)} … Ende: ${text.takeLast(300)}", t)
            throw t
        }
        if (mitSpeicher) speicher.lege(schluessel, text)
        return text to false
    }

    /** Das Urteil des Mediziners zu einem Faktor. */
    private class Urteil(
        val korrigiert: Boolean,
        val jahre: Float?,
        val wahrscheinlichkeit: Int?,
        val evidenz: String?,
        val grund: String,
        val neuSchreiben: Boolean,
        val textgrund: String,
        val titel: String,
        val hinweis: String,
        val zusammenMit: Long?,
    )

    private fun urteileAus(o: JSONObject, nachId: Map<Long, Faktor>, feld: String = "bewertungen"): Map<Long, Urteil> {
        val a = o.optJSONArray(feld) ?: return emptyMap()
        val ergebnis = linkedMapOf<Long, Urteil>()
        for (i in 0 until a.length()) {
            val x = a.optJSONObject(i) ?: continue
            val id = x.optLong("id", -1)
            if (id !in nachId) continue
            val jahre = x.optDouble("jahre", Double.NaN).takeIf { !it.isNaN() && it != 0.0 }?.toFloat()?.coerceIn(-20f, 20f)
            ergebnis[id] = Urteil(
                korrigiert = x.optString("urteil").trim().uppercase(Locale.GERMANY).startsWith("KORR"),
                jahre = jahre,
                wahrscheinlichkeit = x.optInt("wahrscheinlichkeit", -1).takeIf { it in 0..100 },
                evidenz = x.optString("evidenz").trim().takeIf { s -> Evidenz.entries.any { it.name == s } },
                grund = x.optString("grund").trim(),
                neuSchreiben = x.optBoolean("neu_schreiben", false),
                textgrund = x.optString("textgrund").trim(),
                titel = x.optString("titel").trim().trim('.').take(72),
                hinweis = x.optString("hinweis").trim(),
                zusammenMit = x.optLong("zusammenMit", -1).takeIf { it in nachId && it != id },
            )
        }
        return ergebnis
    }

    /**
     * Übernimmt ein Urteil. BESTÄTIGT hält die bisherigen Zahlen fest (keine Zufallsschwankungen zwischen Läufen) –
     * außer der Faktor hatte noch nie eine Wahrscheinlichkeit, dann gelten die neuen Werte.
     */
    private fun anwenden(f: Faktor, u: Urteil): Faktor {
        val zahlenNeu = u.korrigiert || f.wahrscheinlichkeit == null
        val jahre = if (zahlenNeu) u.jahre ?: f.jahre else f.jahre
        val wirkung = if (f.jahre != 0f && jahre != f.jahre) (f.wirkung * abs(jahre / f.jahre)).roundToInt().coerceIn(1, 100) else f.wirkung
        return f.copy(
            titel = u.titel.ifBlank { f.titel },
            jahre = jahre,
            wirkung = wirkung,
            wahrscheinlichkeit = if (zahlenNeu) u.wahrscheinlichkeit ?: f.wahrscheinlichkeit else f.wahrscheinlichkeit,
            evidenz = if (zahlenNeu) u.evidenz ?: f.evidenz else f.evidenz,
            begruendung = u.grund.ifBlank { f.begruendung },
            hinweis = u.hinweis.ifBlank { null } ?: f.hinweis,
            zusammenMit = if (u.hinweis.isNotBlank()) u.zusammenMit else f.zusammenMit,
        )
    }

    /** Eine Zeile fürs Protokoll: „Titel“: +6,0 → +5,5 J · 85 % · korrigiert – Grund. */
    private fun urteilZeile(alt: Faktor, neu: Faktor, u: Urteil): String {
        val zahlen = if (neu.jahre != alt.jahre) "${jahreText(alt.jahre)} → ${jahreText(neu.jahre)} J" else "${jahreText(neu.jahre)} J"
        val w = neu.wahrscheinlichkeit?.let { " · $it %" }.orEmpty()
        val urteil = if (u.korrigiert) "korrigiert" else "bestätigt"
        return "• „${neu.titel}“: $zahlen$w · $urteil" + (if (u.grund.isNotBlank()) " – ${u.grund}" else "") +
            (if (u.neuSchreiben) " ✍️" else "") + (if (u.hinweis.isNotBlank()) " ⚠️ ${u.hinweis}" else "")
    }

    private fun jahreText(j: Float) = "%+.1f".format(Locale.GERMANY, j)

    /** Die Befunde der Neuheiten je Faktor-id. */
    private fun befundeAus(o: JSONObject?, liste: List<Faktor>): Map<Long, List<String>> {
        val a = o?.optJSONArray("befunde") ?: return emptyMap()
        val ids = liste.map { it.id }.toSet()
        val ergebnis = linkedMapOf<Long, MutableList<String>>()
        for (i in 0 until a.length()) {
            val x = a.optJSONObject(i) ?: continue
            val id = x.optLong("id", -1)
            val notiz = x.optString("notiz").trim()
            if (id in ids && notiz.isNotBlank()) ergebnis.getOrPut(id) { mutableListOf() } += notiz.take(400)
        }
        return ergebnis
    }

    /** Ein neuer Kandidat aus den Neuheiten oder dem Mitreden. */
    private class Kandidat(val titel: String, val kategorie: String, val evidenz: String, val wahrscheinlichkeit: Int?, val jahre: Float, val grund: String, val quelle: String)

    /** Neue Kandidaten: ohne Doppelungen mit der Liste, höchstens [NEU_MAX] Schenker und [NEU_MAX] Räuber. */
    private fun kandidatenAus(a: JSONArray?, liste: List<Faktor>): List<Kandidat> {
        if (a == null) return emptyList()
        fun schluessel(t: String) = t.lowercase(Locale.GERMANY).filter { it.isLetterOrDigit() }.take(24)
        val vorhanden = liste.map { schluessel(it.titel) }.toMutableSet()
        val alle = (0 until a.length()).mapNotNull { i ->
            val x = a.optJSONObject(i) ?: return@mapNotNull null
            val titel = x.optString("titel").trim().trim('.').take(72)
            val jahre = x.optDouble("jahre", 0.0).toFloat().coerceIn(-20f, 20f)
            if (titel.isBlank() || jahre == 0f || !vorhanden.add(schluessel(titel))) return@mapNotNull null
            Kandidat(
                titel, Kategorie.von(x.optString("kategorie")).name, Evidenz.von(x.optString("evidenz")).name,
                x.optInt("wahrscheinlichkeit", -1).takeIf { it in 0..100 }, jahre, x.optString("grund").trim(), x.optString("quelle").trim(),
            )
        }
        return alle.filter { it.jahre > 0f }.take(NEU_MAX) + alle.filter { it.jahre < 0f }.take(NEU_MAX)
    }

    private fun neuheitenText(befunde: Map<Long, List<String>>, kandidaten: List<Kandidat>, liste: List<Faktor>, gescheitert: Boolean): String {
        if (gescheitert) return "Die Suche nach Neuheiten kam nicht zustande – die Bewertung läuft trotzdem."
        val titel = liste.associate { it.id to it.titel }
        return buildString {
            if (befunde.isEmpty()) append("Keine neuen Befunde zu vorhandenen Faktoren.")
            else {
                append("Neue Befunde:")
                befunde.forEach { (id, n) -> n.forEach { append("\n• „").append(titel[id]).append("“: ").append(it) } }
            }
            append("\n\n")
            if (kandidaten.isEmpty()) append("Keine neuen Kandidaten.")
            else {
                append("Neue Kandidaten:")
                kandidaten.forEach { k ->
                    append("\n• ").append(if (k.jahre < 0) "Räuber " else "").append("„").append(k.titel).append("“ (")
                        .append(jahreText(k.jahre)).append(" J").append(k.wahrscheinlichkeit?.let { " · $it %" }.orEmpty()).append(") – ").append(k.grund)
                }
            }
        }
    }

    /** Die Plus-Faktoren nach Jahren (Erwartungswert), darunter die Räuber – [ordnen] setzt die Null-Linie durch. */
    private fun rangfolge(liste: List<Faktor>): List<Faktor> {
        val (plus, minus) = liste.partition { !it.raeuber }
        return ordnen(plus.sortedByDescending { it.jahre } + minus)
    }

    /**
     * Welche Texte neu geschrieben werden, mit Grund: Urteile mit „neu schreiben“, Vorzeichenwechsel (Verbot → Räuber)
     * und im großen Lauf alte lange Texte mit „Neu (…)“-Anhängseln. Höchstens [TEXTE_MAX] je Lauf, Korrekturen zuerst;
     * der Rest kommt beim nächsten Lauf dran.
     */
    private fun textAuftraege(
        alt: List<Faktor>,
        neu: List<Faktor>,
        urteile: Map<Long, Urteil>,
        befunde: Map<Long, List<String>>,
        mitAltlasten: Boolean = true,
    ): Map<Long, String> {
        val altNachId = alt.associateBy { it.id }
        val auftraege = linkedMapOf<Long, String>()
        neu.filter { f -> urteile[f.id]?.let { it.neuSchreiben && it.korrigiert } == true }.forEach { f -> auftraege[f.id] = grundFuer(f, urteile[f.id], befunde) }
        neu.filter { f -> urteile[f.id]?.neuSchreiben == true }.forEach { f -> auftraege.getOrPut(f.id) { grundFuer(f, urteile[f.id], befunde) } }
        neu.filter { f -> altNachId[f.id]?.let { (it.jahre < 0) != (f.jahre < 0) } == true }.forEach { f ->
            auftraege.getOrPut(f.id) { "Der Faktor steht jetzt ${if (f.raeuber) "als Lebenszeit-Räuber unter" else "als förderliches Verhalten über"} der Null-Linie – Text, Ziel und Plan passend umschreiben." }
        }
        if (mitAltlasten) neu.filter { "\n\nNeu (" in it.erklaerung || it.erklaerung.startsWith("Neu (") || it.erklaerung.length > LANGER_TEXT }.forEach { f ->
            auftraege.getOrPut(f.id) { "Den Text auf die kurze Form bringen (3–5 Sätze) und angehängte „Neu (…)“-Absätze einarbeiten." + befunde[f.id].orEmpty().joinToString("") { " Neu: $it" } }
        }
        return auftraege.entries.take(TEXTE_MAX).associate { it.key to it.value }
    }

    private fun grundFuer(f: Faktor, u: Urteil?, befunde: Map<Long, List<String>>): String = buildList {
        u?.textgrund?.takeIf { it.isNotBlank() }?.let { add(it) }
        u?.grund?.takeIf { it.isNotBlank() }?.let { add("Bewertung: $it") }
        befunde[f.id]?.forEach { add("Neuer Befund: $it") }
    }.joinToString("\n").ifBlank { "Den Text auf den aktuellen Stand bringen." }

    /** Schreibt die Texte der [auftraege] parallel neu – ohne Websuche, die Erkenntnisse stehen im Grund. */
    private suspend fun texte(v: Map<String, String>, liste: List<Faktor>, auftraege: Map<Long, String>, fortschritt: Fortschritt, von: Float, bis: Float): List<Faktor> {
        if (auftraege.isEmpty()) return liste
        val nachId = liste.associateBy { it.id }
        fortschritt.phase(von, bis, auftraege.size, 25f, "${auftraege.size} Texte werden neu geschrieben …")
        fortschritt.status(ORCHESTRATOR, "Schreibt ${auftraege.size} Texte neu, bis zu $PARALLEL gleichzeitig …")
        val grenze = Semaphore(PARALLEL)
        val fertig = AtomicInteger(0)
        val zeilen = java.util.Collections.synchronizedList(mutableListOf<String>())
        val neu = coroutineScope {
            auftraege.mapNotNull { (id, grund) -> nachId[id]?.let { it to grund } }.map { (f, grund) ->
                async {
                    grenze.withPermit {
                        val auftrag = fuelle(v["text"].orEmpty(), werte() + mapOf("FAKTOR" to detail(f), "GRUND" to grund))
                        val ergebnis = try {
                            val (antwort, _) = gesichert("text:${f.id}", pruefe = { pruefeText(it) }) {
                                frage(rolle(v), nachricht(liste, auftrag), e.modell, denk("text"), fortschritt, still = true, label = "Text id ${f.id} „${f.titel}“")
                            }
                            textUebernehmen(f, faktorAus(jsonAus(antwort))).also { zeilen += "• „${f.titel}“ – ${grund.lineSequence().first().take(160)}" }
                        } catch (c: CancellationException) {
                            throw c
                        } catch (t: Throwable) {
                            KiLog.fehler("Text für id ${f.id} „${f.titel}“ gescheitert – alter Text bleibt", t)
                            zeilen += "• „${f.titel}“: nicht geschafft – alter Text bleibt, kommt beim nächsten Lauf dran"
                            f
                        }
                        fortschritt.schrittFertig("Texte: ${fertig.incrementAndGet()} von ${auftraege.size} neu geschrieben …")
                        ergebnis
                    }
                }
            }.awaitAll()
        }.associateBy { it.id }
        fortschritt.beitragFertig("$MEDIZINER · Texte", "Neu geschrieben:\n" + zeilen.joinToString("\n"), liveLeeren = false)
        return liste.map { neu[it.id] ?: it }
    }

    /** Eine Text-Antwort zählt nur mit Erklärung, Kurztext und Aufgabenplan – sonst wird sie nicht gesichert. */
    private fun pruefeText(antwort: String) {
        val f = faktorAus(jsonAus(antwort))
        require(f.erklaerung.isNotBlank() && f.kurz.isNotBlank() && f.punkte.isNotEmpty()) { "Text unvollständig" }
    }

    /** Der neue Text ersetzt Erklärung, Kurztext, Ziel und Plan; Titel, Jahre, Wahrscheinlichkeit und Evidenz kommen aus der Bewertung. */
    private fun textUebernehmen(f: Faktor, neu: Faktor) = f.copy(
        kurz = neu.kurz.ifBlank { f.kurz },
        erklaerung = neu.erklaerung.ifBlank { f.erklaerung },
        ziel = neu.ziel.ifBlank { f.ziel },
        punkteJson = Faktor.punkteAlsJson(punkteMischen(f.punkte, neu.punkte)),
        quellenJson = Faktor.quellenAlsJson(Faktor.quellenMischen(f.quellen, neu.quellen)),
        standVom = System.currentTimeMillis(),
        vertieft = true,
    )

    /** Arbeitet die Kandidaten parallel zu vollständigen Faktoren aus und ordnet sie nach ihren Jahren ein (als Vorschläge). */
    private suspend fun neueFaktoren(v: Map<String, String>, liste: List<Faktor>, kandidaten: List<Kandidat>, fortschritt: Fortschritt, von: Float, bis: Float): List<Faktor> {
        if (kandidaten.isEmpty()) return emptyList()
        fortschritt.phase(von, bis, kandidaten.size, 25f, "${kandidaten.size} neue Faktoren werden ausgearbeitet …")
        val grenze = Semaphore(PARALLEL)
        val plusAnzahl = liste.count { !it.raeuber }
        val neu = coroutineScope {
            kandidaten.map { k ->
                async {
                    grenze.withPermit {
                        val faktor = buildString {
                            append("### NEUER FAKTOR (noch nicht in der Liste)\n")
                            append("Titel: ").append(k.titel).append("\nKategorie: ").append(k.kategorie).append(" · Evidenz: ").append(k.evidenz)
                                .append(" · Jahre (Erwartungswert): ").append("%.1f".format(Locale.US, k.jahre))
                                .append(" · Wahrscheinlichkeit: ").append(k.wahrscheinlichkeit?.let { "$it %" } ?: "bitte schätzen")
                            append("\nWarum: ").append(k.grund)
                            if (k.quelle.isNotBlank()) append("\nQuelle: ").append(k.quelle)
                        }
                        val auftrag = fuelle(v["text"].orEmpty(), werte() + mapOf("FAKTOR" to faktor, "GRUND" to "Neuer Faktor – vollständig ausarbeiten (Text, Ziel, Plan, Quellen)."))
                        val f = try {
                            val (antwort, _) = gesichert("neu:${k.titel.hashCode()}", pruefe = { pruefeText(it) }) {
                                frage(rolle(v), nachricht(liste, auftrag), e.modell, denk("text"), fortschritt, still = true, label = "Neuer Faktor „${k.titel}“")
                            }
                            faktorAus(jsonAus(antwort))
                        } catch (c: CancellationException) {
                            throw c
                        } catch (t: Throwable) {
                            KiLog.fehler("Neuer Faktor „${k.titel}“ nicht ausgearbeitet – kommt als Kurzfassung", t)
                            Faktor(titel = k.titel, kurz = k.grund, erklaerung = k.grund, quellenJson = Faktor.quellenAlsJson(listOfNotNull(k.quelle.takeIf { it.isNotBlank() }?.let { Quelle(it) })))
                        }
                        // Die Rang-Position ergibt sich aus den Jahren – wie in der Rangliste selbst.
                        val rang = if (k.jahre > 0) liste.count { !it.raeuber && it.jahre >= k.jahre } + 1
                        else plusAnzahl + liste.count { it.raeuber && it.jahre >= k.jahre } + 1
                        fortschritt.schrittFertig()
                        f.copy(
                            titel = k.titel, kategorie = k.kategorie, evidenz = k.evidenz, jahre = k.jahre,
                            wahrscheinlichkeit = k.wahrscheinlichkeit ?: f.wahrscheinlichkeit, rang = rang, vertieft = true, neu = true,
                        )
                    }
                }
            }.awaitAll()
        }
        fortschritt.beitragFertig(
            "$MEDIZINER · Neue Faktoren",
            "Als Vorschläge eingeordnet (du entscheidest):\n" + neu.joinToString("\n") { "• „${it.titel}“ auf Platz ${it.rang} (${jahreText(it.jahre)} J)" },
            liveLeeren = false,
        )
        return neu
    }

    /** Die Systemanweisung: die Rolle des Mediziners – für alle Aufrufe gleich (kommt aus dem Cache). */
    private fun rolle(v: Map<String, String>): String = fuelle(v["rolle"].orEmpty(), werte()).trim()

    /** Die Nachricht: gleicher Anfang für alle Aufrufe eines Laufs (Datum, Rangliste), danach der Auftrag. */
    private fun nachricht(liste: List<Faktor>, auftrag: String): String =
        "HEUTE: ${heute()}\n\nRANGLISTE (id | Rang | Titel | Kategorie | Evidenz | Wahrscheinlichkeit | Jahre; negative Jahre = Lebenszeit-Räuber unter der Null-Linie):\n" +
            listeKompakt(liste) + "\n\n" + auftrag.trim()

    /** Die Platzhalter, die überall gelten. */
    private fun werte(): Map<String, String> = mapOf(
        "DATUM" to heute(), "PROFIL" to profilZeile(), "NEU_MAX" to NEU_MAX.toString(), "FAKTOR_SCHEMA" to FAKTOR_SCHEMA,
    )

    /**
     * Denkstufe je Aufgabe nach dem Regler. Die Denkstufe aus den Einstellungen ist die Obergrenze; bis Stufe 3
     * arbeiten Bewertung und Texte mit mittlerer Stufe, Neuheiten immer mit der vollen.
     */
    private fun denk(aufgabe: String): ReasoningEffort {
        val ziel = when (aufgabe) {
            "scout" -> e.denkstufe
            else -> if (stufe <= 3) ReasoningEffort.MEDIUM else e.denkstufe
        }
        return if (ziel.ordinal < e.denkstufe.ordinal) ziel else e.denkstufe
    }

    /** Die wirksame Vorlage: eigene Abschnitte aus den Einstellungen, fehlende oder leere aus dem Standard. */
    private fun vorlage(): Map<String, String> {
        val standard = abschnitte(standardVorlage())
        val eigen = abschnitte(e.aktualisierungsPrompt)
        return standard.keys.associateWith { k -> eigen[k]?.takeIf { it.isNotBlank() } ?: standard.getValue(k) }
    }

    private fun profilZeile(): String {
        val p = e.profilText()
        return if (p.isBlank()) "" else "PROFIL DES NUTZERS (berücksichtige es bei Rang, Ziel und Aufgaben): $p"
    }

    // ================= Gemeinsames =================

    /** Dieselbe Rolle wie im Aktualisierungslauf: der erfahrene Langlebigkeitsmediziner aus dem Arbeitsauftrag. */
    private fun grundanweisung(): String = rolle(vorlage())

    private fun listeKompakt(liste: List<Faktor>) = liste.joinToString("\n") {
        "${it.id} | ${it.rang} | ${it.titel} | ${it.kat.name} | ${it.ev.name} | ${it.wahrscheinlichkeit?.let { w -> "$w %" } ?: "?"} | ${"%.1f".format(Locale.US, it.jahre)}" + if (it.zielErreicht) (if (it.raeuber) " | trifft beim Nutzer nicht zu bzw. abgestellt" else " | vom Nutzer bereits umgesetzt") else ""
    }

    private suspend fun frage(
        anweisung: String,
        text: String,
        modell: CodexModel,
        stufe: ReasoningEffort,
        fortschritt: Fortschritt? = null,
        webSuche: Boolean = false,
        /** Parallele Aufrufe zählen nur für den Balken und schreiben nicht in den Live-Beitrag. */
        still: Boolean = false,
        /** Name des Schritts fürs Diagnose-Log. */
        label: String = "KI-Aufruf",
    ): String {
        var versuch = 0
        while (true) {
            val start = System.currentTimeMillis()
            var empfangen = 0
            KiLog.info(
                "▶ $label" + (if (versuch > 0) " (Wiederholung $versuch)" else "") +
                    " · ${modell.apiId}/${stufe.name} · Websuche ${if (webSuche && auth.webSucheMoeglich) "an" else "aus"}" +
                    " · System ${anweisung.length} + Nachricht ${text.length} Zeichen",
            )
            try {
                // Hartes Zeitlimit je Aufruf: Bleibt eine Antwort stumm, wird sie einmal wiederholt statt ewig zu warten.
                val antwort = withTimeoutOrNull(ZEITLIMIT_MS) { auth.streamChat(
                    instructions = anweisung,
                    turns = listOf(ChatTurn("user", text.trim())),
                    model = modell,
                    reasoningEffort = stufe,
                    webSuche = webSuche,
                    cacheKey = laufKennung,
                    // Lange Läufe ohne Priority-Verarbeitung: langsamer, verbraucht aber deutlich weniger Kontingent.
                    prioritaet = laufKennung == null,
                    onDelta = { stueck ->
                        empfangen += stueck.length
                        if (still) fortschritt?.zeichenStill(stueck) else fortschritt?.zeichen(stueck)
                    },
                ) } ?: throw IOException("Keine vollständige Antwort nach ${ZEITLIMIT_MS / 60_000} Minuten")
                KiLog.info("✔ $label nach ${(System.currentTimeMillis() - start) / 1000}s · ${antwort.length} Zeichen")
                return antwort
            } catch (c: CancellationException) {
                KiLog.warn("■ $label abgebrochen nach ${(System.currentTimeMillis() - start) / 1000}s · $empfangen Zeichen empfangen")
                throw c
            } catch (t: Throwable) {
                // Abgerissene Verbindungen (z. B. Zeitlimit, WLAN-Wechsel) werden komplett wiederholt – auch mitten im Text.
                val abriss = t is IOException || (t is CodexAuthException && t.kind == AuthErrorKind.NETWORK && (t.retryable || t.cause is IOException || "vor dem Abschluss" in t.message.orEmpty()))
                val nochmal = abriss && versuch < WIEDERHOLUNGEN_MS.size
                KiLog.fehler(
                    "✖ $label nach ${(System.currentTimeMillis() - start) / 1000}s · $empfangen Zeichen empfangen · " +
                        if (nochmal) "neuer Versuch in ${WIEDERHOLUNGEN_MS[versuch] / 1000}s" else "gebe auf",
                    t,
                )
                if (!nochmal) throw t
                if (!still) fortschritt?.liveLeeren()
                delay(WIEDERHOLUNGEN_MS[versuch])
                versuch++
            }
        }
    }

    private fun heute(): String = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date())

    /**
     * Ein Faktor mit allem, was die Agenten zum Prüfen und Verbessern brauchen. [kompakt] (für Debatte und Entscheidung,
     * die alle Faktoren zugleich sehen): Erklärung gekürzt, Aufgabenplan nur mit Titeln, höchstens drei Quellen.
     */
    private fun detail(f: Faktor, kompakt: Boolean = false): String = buildString {
        append("### id ").append(f.id).append(" · Rang ").append(f.rang).append(" · ").append(f.titel).append('\n')
        append("Kategorie: ").append(f.kat.name).append(" · Evidenz: ").append(f.ev.name)
            .append(" · Jahre (Erwartungswert): ").append("%.1f".format(Locale.US, f.jahre)).append(" · Wirkung: ").append(f.wirkung)
            .append(" · Wahrscheinlichkeit: ").append(f.wahrscheinlichkeit?.let { "$it %" } ?: "noch nicht geschätzt")
        if (f.zielErreicht) append(if (f.raeuber) " · beim Nutzer abgestellt" else " · vom Nutzer umgesetzt")
        append("\nKurz: ").append(f.kurz)
        val erklaerung = f.erklaerung.replace("\n\n", " ¶ ")
        append("\nErklärung: ").append(if (kompakt && erklaerung.length > 700) erklaerung.take(700) + " …" else erklaerung)
        append("\nBegründung des Rangs: ").append(f.begruendung)
        append("\nZiel: ").append(f.ziel)
        val p = f.punkte
        if (p.isNotEmpty()) {
            append("\nAufgabenplan:")
            p.forEach {
                append("\n- ").append(it.titel).append(" (").append(it.ev.name).append(")")
                if (!kompakt) append(": ").append(it.text)
                if (it.erledigt) append(" [vom Nutzer erledigt]")
            }
        }
        val q = f.quellen
        if (q.isNotEmpty()) append("\nQuellen: ").append((if (kompakt) q.take(3) else q).joinToString("; ") { listOf(it.titel, it.jahr, it.link).filter(String::isNotBlank).joinToString(", ") })
        append("\nInhalt zuletzt geprüft: ").append(f.standVom?.let { SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(it)) } ?: "noch nie (Altbestand)")
        f.hinweis?.takeIf { it.isNotBlank() }?.let { append("\nOffener Hinweis: ").append(it) }
    }

    companion object {
        /** Der eine Agent des Laufs. */
        const val MEDIZINER = "Mediziner"
        /** Seine Antwort beim Mitreden. */
        const val ANTWORT = "Mediziner · Einordnung"
        /** Statuszeilen der App, die den Lauf steuert. */
        const val ORCHESTRATOR = "Orchestrator"
        const val SCOUT = "Neuheiten"
        const val NUTZER = "Du"

        // Namen aus älteren Läufen – nur noch für die Anzeige alter Diskussionen.
        const val PRO = "Forscherin Vita"
        const val CONTRA = "Skeptiker Kron"
        const val RICHTER = "Gutachterin Aeon"
        const val AUTORIN = "Autorin Lexa"
        const val RAEUBER_JAEGER = "Räuber-Jäger"
        private const val SCOUT_ALT = "Neuheiten-Scout"

        /** Höchstens so viele KI-Aufrufe gleichzeitig – mehr bremst das Codex-Kontingent. */
        const val PARALLEL = 5
        /** Höchstens so viele neue förderliche Faktoren und ebenso viele neue Räuber pro Lauf. */
        const val NEU_MAX = 3
        /** Höchstens so viele neue Texte pro Lauf – der Rest kommt beim nächsten Lauf dran. */
        const val TEXTE_MAX = 12
        /** Erklärungen, die länger sind, werden im großen Lauf auf die kurze Form (3–5 Sätze) gebracht. */
        const val LANGER_TEXT = 1_200
        /** Hartes Zeitlimit je KI-Aufruf. */
        private const val ZEITLIMIT_MS = 8 * 60_000L

        /** Wartezeit, bevor ein abgerissener oder stummer Aufruf einmal wiederholt wird. */
        private val WIEDERHOLUNGEN_MS = longArrayOf(10_000L)
        /** Stand des Standard-Prompts; ein eigener Prompt älteren Stands wird einmalig gesichert und ersetzt. */
        const val PROMPT_VERSION = 3

        /** Gehört ein Protokoll-Beitrag zur Recherche eines älteren Laufs? */
        fun istRecherche(name: String) = name.startsWith("Rechercheur") || name == RAEUBER_JAEGER || name == SCOUT_ALT || name == "Recherche-Schwarm" || name == "Einzelprüfung"

        /** Zerlegt die Vorlage an den Überschriften „## Name“; alles vor der ersten ist Erklärung. */
        fun abschnitte(text: String): Map<String, String> {
            val ergebnis = linkedMapOf<String, String>()
            var name: String? = null
            val puffer = StringBuilder()
            fun ablegen() { name?.let { ergebnis[it] = puffer.toString().trim() } }
            text.lineSequence().forEach { zeile ->
                if (zeile.startsWith("## ")) {
                    ablegen()
                    name = zeile.removePrefix("## ").trim().lowercase()
                    puffer.clear()
                } else if (name != null) {
                    puffer.append(zeile).append('\n')
                }
            }
            ablegen()
            return ergebnis
        }

        /** Ersetzt {{NAME}} in einem Durchgang; unbekannte Platzhalter bleiben stehen. */
        fun fuelle(text: String, werte: Map<String, String>): String =
            Regex("\\{\\{([A-Z_]+)\\}\\}").replace(text) { m -> werte[m.groupValues[1]] ?: m.value }

        private val KATEGORIEN = Kategorie.entries.joinToString("|") { it.name }

        val FAKTOR_SCHEMA = """"titel": "max. 60 Zeichen, beschreibt das Verhalten komplett", "kurz": "1 Satz Kernaussage",
            | "kategorie": "$KATEGORIEN", "evidenz": "BELEGT|WAHRSCHEINLICH|LOGISCH",
            | "wahrscheinlichkeit": 0-100 (Prozent, dass der Effekt beim Menschen real ist),
            | "jahre": "Zahl, Erwartungswert = Potenzial × Wahrscheinlichkeit: positiv (z. B. 2.5) bei förderlichem Verhalten, negativ (z. B. -10) bei schädlichem", "wirkung": 0-100 (Stärke, auch bei schädlichem Verhalten positiv), "erklaerung": "3–5 kurze Sätze: nur die entscheidenden Erkenntnisse, je Erkenntnis ein klarer Satz mit Zahl", "begruendung": "1–2 Sätze, warum genau dieser Rang",
            | "ziel": "konkretes, messbares Ziel", "punkte": [{"titel": "max. 50 Zeichen", "text": "1 Satz, konkret mit Dosis/Häufigkeit", "evidenz": "BELEGT|WAHRSCHEINLICH|LOGISCH"}],
            | "quellen": [{"titel": "Autor et al., Studie/Metaanalyse, Journal", "jahr": "2025", "link": "https://… (nur wenn sicher bekannt, sonst leer)"}]""".trimMargin()

        fun faktorAus(o: JSONObject): Faktor {
            val punkte = o.optJSONArray("punkte") ?: JSONArray()
            val liste = (0 until punkte.length()).mapNotNull { i ->
                val p = punkte.optJSONObject(i) ?: return@mapNotNull null
                Punkt(p.optString("titel").trim().take(80), p.optString("text").trim(), Evidenz.von(p.optString("evidenz")).name)
            }.filter { it.titel.isNotBlank() }
            return Faktor(
                titel = o.optString("titel").trim().trim('.').take(72),
                kurz = o.optString("kurz").trim(),
                kategorie = Kategorie.von(o.optString("kategorie")).name,
                evidenz = Evidenz.von(o.optString("evidenz")).name,
                jahre = o.optDouble("jahre", 0.0).toFloat().coerceIn(-20f, 20f),
                wirkung = o.optInt("wirkung", 50).coerceIn(0, 100),
                wahrscheinlichkeit = o.optInt("wahrscheinlichkeit", -1).takeIf { it in 0..100 },
                erklaerung = o.optString("erklaerung").trim(),
                begruendung = o.optString("begruendung").trim(),
                ziel = o.optString("ziel").trim(),
                punkteJson = Faktor.punkteAlsJson(liste),
                quellenJson = Faktor.quellenAlsJson(quellenAus(o)),
                standVom = System.currentTimeMillis(),
            )
        }

        private fun quellenAus(o: JSONObject): List<Quelle> {
            val a = o.optJSONArray("quellen") ?: return emptyList()
            return (0 until a.length()).mapNotNull { i ->
                val q = a.optJSONObject(i) ?: return@mapNotNull a.optString(i).trim().takeIf { it.isNotBlank() }?.let { Quelle(it) }
                Quelle(q.optString("titel").trim(), q.optString("jahr").trim(), q.optString("link").trim().takeIf { it.startsWith("http") }.orEmpty())
            }.filter { it.titel.isNotBlank() }.take(12)
        }

        /** Neue Punkte übernehmen, Häkchen des Nutzers behalten, erledigte alte Punkte nie verlieren. */
        fun punkteMischen(alt: List<Punkt>, neu: List<Punkt>): List<Punkt> {
            if (neu.isEmpty()) return alt
            fun schluessel(p: Punkt) = p.titel.lowercase().filter { it.isLetterOrDigit() }.take(18)
            val erledigt = alt.filter { it.erledigt }.map(::schluessel).toSet()
            val gemischt = neu.map { if (schluessel(it) in erledigt) it.copy(erledigt = true) else it }
            val fehlend = alt.filter { it.erledigt && gemischt.none { n -> schluessel(n) == schluessel(it) } }
            return gemischt + fehlend
        }
    }
}

/** Zieht das erste vollständige JSON-Objekt aus einer Modellantwort (auch mit Vorrede oder ```-Zaun). */
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
