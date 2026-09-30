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
import de.frank.longevity.data.RechercheTiefe
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject

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
    /** Anzahl erfolgreicher Recherche-Dossiers. */
    val recherchen: Int = 0,
    /** Anzahl neu geschriebener Texte. */
    val ueberarbeitet: Int = 0,
    /** Anzahl neuer Hinweise der Gutachterin (Überschneidungen, Veraltetes). */
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
            |AKTUELLE LISTE (id | Rang | Titel | Kategorie | Evidenz | Jahre; oben Plus-Faktoren, unter der Null-Linie die
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
            |gibt es keine „Neu (…)“-Absätze mehr. Erkläre genau, welche Verhaltensweise gemeint ist, warum sie wirkt (Mechanismus) und was
            |die Forschung zeigt (mit Zahlen). Begründe, warum der Faktor genau auf
            |${if (f.raeuber) "seinem Platz unter der Null-Linie (Lebenszeit-Räuber, ${"%.1f".format(Locale.US, f.jahre)} Jahre)" else "Rang ${f.rang}"} steht.
            |Ist der Faktor ein Lebenszeit-Räuber, beschreibt der Titel das schädliche Verhalten, "jahre" bleibt negativ, und Ziel
            |und Aufgabenplan zeigen, wie man es abstellt. Leite daraus ein klares, messbares persönliches Ziel ab und einen Aufgabenplan mit 5–9 Punkten,
            |sortiert nach Wichtigkeit (was man sofort umsetzen sollte, zuerst). Ist der Faktor eine Sammelkategorie (z. B.
            |Supplements, Lebensmittel, Übungen), sind die Punkte die einzelnen Elemente (z. B. die einzelnen Supplements, 10–16 Stück),
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

    // ================= Aktualisierung: Recherche-Schwarm, Debatte, Gutachterin, Text-Konsens =================

    private val webSuche: Boolean get() = e.rechercheTiefe != RechercheTiefe.SCHNELL

    /** Die Websuche war eingeschaltet, wurde vom Server aber abgelehnt – der Lauf arbeitete ohne Internet. */
    val webSucheAbgelehnt: Boolean get() = webSuche && !auth.webSucheMoeglich

    /**
     * Der große Lauf:
     * 1. Recherche-Schwarm (parallel, mit Websuche) – je Lebensbereich ein Rechercheur, dazu Räuber-Jäger und Neuheiten-Scout.
     * 2. Debatte (nacheinander) auf Basis der Dossiers und der vollständigen Texte.
     * 3. Gutachterin: Rangliste, Korrekturen, neue Faktoren, Hinweise und die Liste der Texte, die neu geschrieben werden.
     * 4. Text-Konsens (parallel): Diese Faktoren bekommen einen neuen Text aus altem Stand und neuen Erkenntnissen.
     */
    suspend fun aktualisieren(liste: List<Faktor>, fortschritt: Fortschritt): Aktualisierung {
        auth.webSucheZuruecksetzen()
        val v = vorlage()
        val stand = speicher.beginnen(e.rechercheTiefe.name, liste.map { it.id })
        KiLog.info(
            "═══ Aktualisierung startet: ${liste.size} Faktoren · Tiefe ${e.rechercheTiefe.name} · ${e.modell.apiId}/${e.denkstufe.name} · " +
                (if (e.aktualisierungsPrompt.isBlank()) "Standard-Prompt" else "eigener Prompt") + " · ${stand.schritte.size} Schritte schon gesichert",
        )
        mitSpeicher = true
        try {
            val mitRecherche = e.rechercheTiefe != RechercheTiefe.SCHNELL
            // Bänder: Recherche | Einzelprüfung | Debatte | Gutachterin | Text-Konsens
            val b = if (mitRecherche) floatArrayOf(0.22f, 0.42f, 0.64f, 0.76f) else floatArrayOf(0f, 0f, 0.42f, 0.62f)
            val (dossiers, anzahlRecherchen) = if (mitRecherche) recherchieren(v, liste, fortschritt, 0f, b[0]) else "" to 0
            // Ein aus einem älteren Protokoll geretteter Lauf hatte noch keine Einzelprüfung – seine Debatte steht schon.
            val pruefung = if (mitRecherche && !stand.ohnePruefung) einzelpruefung(v, liste, dossiers, fortschritt, b[0], b[1]) else ""
            val lauf = Lauf(v, liste, "", StringBuilder(), fortschritt, dossiers, pruefung)
            val d = (b[2] - b[1]) / 4f
            KiLog.info("Phase Debatte")
            lauf.sprich(PRO, "runde 1 forscherin", b[1], b[1] + d * 1.2f, 5000, 90f)
            lauf.sprich(CONTRA, "runde 1 skeptiker", b[1] + d * 1.2f, b[1] + d * 2.4f, 5000, 90f)
            lauf.sprich(PRO, "runde 2 forscherin", b[1] + d * 2.4f, b[1] + d * 3.3f, 3200, 60f)
            lauf.sprich(CONTRA, "runde 2 skeptiker", b[1] + d * 3.3f, b[2], 2600, 50f)
            KiLog.info("Phase Entscheidung")
            val richter = lauf.sprich(RICHTER, "entscheidung", b[2], b[3], 1400 + liste.size * 420, 80f + liste.size * 2f)
            val m = mischen(liste, jsonAus(richter), altlastenEinarbeiten = true)
            KiLog.info("Phase Text-Konsens: ${m.konsens.size} Texte")
            val liste2 = konsens(v, m, dossiers, fortschritt, b[3], 0.99f)
            KiLog.info("═══ Aktualisierung fertig: ${m.ergebnis.veraendert} Plätze geändert, ${m.konsens.size} Texte, ${m.ergebnis.vorschlaege.size} Vorschläge")
            return m.ergebnis.copy(liste = liste2, recherchen = anzahlRecherchen, ueberarbeitet = m.konsens.size)
        } finally {
            mitSpeicher = false
        }
    }

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

    /**
     * Rettet einen Lauf aus dem Diskussionsprotokoll (für Läufe vor dem Zwischenspeicher): Dossiers, Debattenrunden
     * und Entscheidung werden als erledigte Schritte übernommen.
     */
    fun rettungAusProtokoll(beitraege: List<Beitrag>): Map<String, String> {
        val schritte = linkedMapOf<String, String>()
        val runden = ArrayDeque(listOf("runde 1 forscherin", "runde 1 skeptiker", "runde 2 forscherin", "runde 2 skeptiker"))
        for (b in beitraege) {
            when {
                istRecherche(b.name) && b.name != SCHWARM && b.name != PRUEFSTAND && !b.text.startsWith("(Recherche fehlgeschlagen") && b.text.isNotBlank() ->
                    schritte["recherche:${b.name}"] = b.text
                (b.name == PRO || b.name == CONTRA) && runden.isNotEmpty() -> {
                    val soll = runden.first()
                    if ((b.name == PRO) == soll.endsWith("forscherin")) schritte["debatte:${runden.removeFirst()}"] = b.text
                }
                b.name == RICHTER && runCatching { jsonAus(b.text) }.isSuccess -> schritte["debatte:entscheidung"] = b.text
            }
        }
        return schritte
    }

    /**
     * Der Nutzer redet mit: Sein Beitrag läuft durch alle Agenten (Forscherin, Skeptiker, Forscherin, Skeptiker),
     * die Gutachterin bildet daraus einen Konsens, ordnet den Beitrag ein und passt die Rangliste an. Betroffene
     * Texte werden danach neu geschrieben.
     */
    suspend fun einwand(liste: List<Faktor>, text: String, bisher: List<Beitrag>, fortschritt: Fortschritt): Aktualisierung {
        auth.webSucheZuruecksetzen()
        val v = vorlage()
        val verlauf = StringBuilder()
        // Nur die Debatte, deine Beiträge und die Entscheidungen – Dossiers, Einzelprüfungen und Autorinnen-Meldungen
        // sind in den Texten schon eingearbeitet und würden jede Anfrage nur aufblähen.
        bisher.filter { b -> !istRecherche(b.name) && b.name != AUTORIN && " · Prüfung " !in b.name }
            .forEach { b -> verlauf.append("\n### ").append(b.name).append(":\n").append(beitragKurz(b)).append('\n') }
        fortschritt.beitragFertig(NUTZER, text)
        verlauf.append("\n### ").append(NUTZER).append(" (der Nutzer):\n").append(text).append('\n')
        val lauf = Lauf(v, liste, text, verlauf, fortschritt, "", "")
        lauf.sprich(PRO, "einwand forscherin", 0.00f, 0.16f, 3500, 80f)
        lauf.sprich(CONTRA, "einwand skeptiker", 0.16f, 0.32f, 3500, 80f)
        lauf.sprich(PRO, "einwand forscherin antwort", 0.32f, 0.44f, 2400, 50f)
        lauf.sprich(CONTRA, "einwand skeptiker schlusswort", 0.44f, 0.54f, 2000, 45f)
        val richter = lauf.sprich(RICHTER, "einwand entscheidung", 0.54f, 0.72f, 1400 + liste.size * 420, 80f + liste.size * 2f)
        val o = jsonAus(richter)
        val m = mischen(liste, o, altlastenEinarbeiten = false)
        val liste2 = konsens(v, m, "", fortschritt, 0.72f, 0.99f)
        return m.ergebnis.copy(liste = liste2, einordnung = o.optString("einordnung").trim(), ueberarbeitet = m.konsens.size)
    }

    /** Ein Recherche-Auftrag des Schwarms. */
    private class RechercheAuftrag(val name: String, val abschnitt: String, val bereich: String, val faktoren: List<Faktor>)

    private fun rechercheAuftraege(liste: List<Faktor>): List<RechercheAuftrag> {
        val gruppen: List<Triple<String, String, List<Kategorie>>> = when (e.rechercheTiefe) {
            RechercheTiefe.SCHNELL -> emptyList()
            RechercheTiefe.MAXIMAL -> Kategorie.entries.map { Triple("Rechercheur ${it.anzeige}", it.anzeige, listOf(it)) }
            RechercheTiefe.GRUENDLICH -> listOf(
                Triple(
                    "Rechercheur Körper", "Körper: Bewegung & Fitness, Ernährung, Schlaf & Rhythmus, Supplements",
                    listOf(Kategorie.BEWEGUNG, Kategorie.ERNAEHRUNG, Kategorie.SCHLAF, Kategorie.SUPPLEMENTE),
                ),
                Triple(
                    "Rechercheur Geist & Leben", "Geist & Leben: Geist & Stress, Beziehungen, Sinn & Lernen",
                    listOf(Kategorie.GEIST, Kategorie.SOZIAL, Kategorie.SINN),
                ),
                Triple(
                    "Rechercheur Medizin & Umwelt", "Medizin & Umwelt: Vorsorge & Medizin, Genussmittel & Gifte, Umwelt",
                    listOf(Kategorie.VORSORGE, Kategorie.GIFTE, Kategorie.UMWELT),
                ),
            )
        }
        if (gruppen.isEmpty()) return emptyList()
        val bereiche = gruppen.map { (name, bereich, kats) ->
            RechercheAuftrag(name, "recherche bereich", "$bereich (Kategorien: ${kats.joinToString { it.name }})", liste.filter { it.kat in kats })
        }
        return bereiche +
            RechercheAuftrag(RAEUBER_JAEGER, "recherche räuber", "Lebenszeit-Räuber (schädliches Verhalten, alle Lebensbereiche)", liste.filter { it.raeuber }) +
            RechercheAuftrag(SCOUT, "recherche neuheiten", "Neue Forschung der letzten 24 Monate (alle Lebensbereiche)", emptyList())
    }

    /** Phase 1: alle Rechercheure parallel (höchstens [PARALLEL] gleichzeitig). Liefert die Dossiers und ihre Anzahl. */
    private suspend fun recherchieren(v: Map<String, String>, liste: List<Faktor>, fortschritt: Fortschritt, von: Float, bis: Float): Pair<String, Int> {
        val auftraege = rechercheAuftraege(liste)
        if (auftraege.isEmpty()) return "" to 0
        val wellen = (auftraege.size + PARALLEL - 1) / PARALLEL
        fortschritt.band(von, bis, auftraege.size * 6000, 150f * wellen, "${auftraege.size} Rechercheure suchen im Internet …")
        fortschritt.status(SCHWARM, "${auftraege.size} Rechercheure starten, bis zu $PARALLEL gleichzeitig …")
        val grenze = Semaphore(PARALLEL)
        val fertig = AtomicInteger(0)
        val fehler = AtomicInteger(0)
        var ersterFehler: Throwable? = null
        val dossiers = coroutineScope {
            auftraege.map { a ->
                async {
                    grenze.withPermit {
                        val werte = basisWerte(liste, "", "") + mapOf(
                            "BEREICH" to a.bereich,
                            "BEREICH_DETAILS" to a.faktoren.joinToString("\n\n") { detail(it) }
                                .ifBlank { "(in diesem Bereich steht noch kein Faktor in der Rangliste)" },
                        )
                        val (text, gespeichert) = try {
                            gesichert("recherche:${a.name}") {
                                frage(
                                    system(v, werte, "rolle rechercheur"), nachricht(v, "aufbau recherche", werte, a.abschnitt),
                                    e.modell, e.denkstufe, fortschritt, webSuche = true, still = true, label = a.name,
                                ).trim()
                            }
                        } catch (c: CancellationException) {
                            throw c
                        } catch (t: Throwable) {
                            fehler.incrementAndGet()
                            if (ersterFehler == null) ersterFehler = t
                            "(Recherche fehlgeschlagen: ${t.message})" to false
                        }
                        if (!gespeichert) fortschritt.beitragFertig(a.name, text, liveLeeren = false)
                        val n = fertig.incrementAndGet()
                        fortschritt.status(SCHWARM, "$n von ${auftraege.size} Dossiers fertig …")
                        "## DOSSIER ${a.name.uppercase(Locale.GERMANY)}\n$text"
                    }
                }
            }.awaitAll()
        }
        // Ist die ganze Recherche gescheitert (z. B. Kontingent aus), hat der Rest des Laufs keine Grundlage.
        if (fehler.get() == auftraege.size) throw ersterFehler ?: IllegalStateException("Die Recherche ist fehlgeschlagen.")
        fortschritt.status(SCHWARM, "Recherche fertig – die Diskussion beginnt.")
        return dossiers.joinToString("\n\n") to (auftraege.size - fehler.get())
    }

    /**
     * Phase 2: Jeder Faktor wird einzeln hinterfragt. Die Rangliste wird in Blöcke geteilt (Maximal 4, Gründlich 8 Faktoren);
     * je Block prüft die Forscherin jeden Faktor mit Websuche, der Skeptiker hält Punkt für Punkt dagegen. Blöcke laufen parallel.
     */
    private suspend fun einzelpruefung(v: Map<String, String>, liste: List<Faktor>, dossiers: String, fortschritt: Fortschritt, von: Float, bis: Float): String {
        val groesse = if (e.rechercheTiefe == RechercheTiefe.MAXIMAL) 4 else 8
        val bloecke = liste.chunked(groesse)
        if (bloecke.isEmpty()) return ""
        val wellen = (bloecke.size + PARALLEL - 1) / PARALLEL
        fortschritt.band(von, bis, bloecke.size * 7000, 220f * wellen, "Einzelprüfung: ${liste.size} Faktoren in ${bloecke.size} Blöcken …")
        fortschritt.status(PRUEFSTAND, "Jeder Faktor wird einzeln geprüft – ${bloecke.size} Blöcke, bis zu $PARALLEL gleichzeitig …")
        val grenze = Semaphore(PARALLEL)
        val fertig = AtomicInteger(0)
        val ergebnisse = coroutineScope {
            bloecke.map { block ->
                async {
                    grenze.withPermit {
                        val bereich = "Rang ${block.first().rang}–${block.last().rang}"
                        val werte = basisWerte(liste, "", dossiers) + mapOf(
                            "BEREICH" to bereich,
                            "BEREICH_DETAILS" to block.joinToString("\n\n") { detail(it) },
                        )
                        val (pro, proGespeichert) = try {
                            gesichert("pruefung:$bereich:forscherin") {
                                frage(
                                    system(v, werte, "rolle forscherin"),
                                    nachricht(v, "aufbau einzelprüfung", werte + ("DISKUSSION" to "(noch keine – du beginnst)"), "einzelprüfung forscherin"),
                                    e.modell, e.denkstufe, fortschritt, webSuche = true, still = true, label = "Einzelprüfung $bereich Forscherin",
                                ).trim()
                            }
                        } catch (c: CancellationException) { throw c } catch (t: Throwable) { "(Prüfung fehlgeschlagen: ${t.message})" to false }
                        if (!proGespeichert) fortschritt.beitragFertig("$PRO · Prüfung $bereich", pro, liveLeeren = false)
                        val (contra, contraGespeichert) = try {
                            gesichert("pruefung:$bereich:skeptiker") {
                                frage(
                                    system(v, werte, "rolle skeptiker"),
                                    nachricht(v, "aufbau einzelprüfung", werte + ("DISKUSSION" to "### $PRO:\n$pro"), "einzelprüfung skeptiker"),
                                    e.modell, e.denkstufe, fortschritt, webSuche = true, still = true, label = "Einzelprüfung $bereich Skeptiker",
                                ).trim()
                            }
                        } catch (c: CancellationException) { throw c } catch (t: Throwable) { "(Gegenprüfung fehlgeschlagen: ${t.message})" to false }
                        if (!contraGespeichert) fortschritt.beitragFertig("$CONTRA · Prüfung $bereich", contra, liveLeeren = false)
                        val n = fertig.incrementAndGet()
                        fortschritt.status(PRUEFSTAND, "$n von ${bloecke.size} Blöcken geprüft …")
                        "## EINZELPRÜFUNG $bereich\n### $PRO:\n$pro\n### $CONTRA:\n$contra"
                    }
                }
            }.awaitAll()
        }
        return ergebnisse.joinToString("\n\n")
    }

    /** Phase 4: Die von der Gutachterin markierten Faktoren bekommen parallel einen neuen Konsens-Text. */
    private suspend fun konsens(v: Map<String, String>, m: Mischung, dossiers: String, fortschritt: Fortschritt, von: Float, bis: Float): List<Faktor> {
        if (m.konsens.isEmpty()) return m.ergebnis.liste
        val wellen = (m.konsens.size + PARALLEL - 1) / PARALLEL
        fortschritt.band(von, bis, m.konsens.size * 4500, (if (webSuche) 110f else 60f) * wellen, "${m.konsens.size} Texte werden neu geschrieben …")
        fortschritt.status(AUTORIN, "Überarbeitet ${m.konsens.size} Texte, bis zu $PARALLEL gleichzeitig …")
        val nachId = m.ergebnis.liste.associateBy { it.id }
        val grenze = Semaphore(PARALLEL)
        val fertig = AtomicInteger(0)
        val datum = heute()
        val recherche = dossiers.ifBlank { "(in diesem Lauf keine Recherche-Dossiers – nutze die Websuche, falls verfügbar)" }
        val neu = coroutineScope {
            m.konsens.mapNotNull { (id, auftrag) -> nachId[id]?.let { it to auftrag } }.map { (f, auftrag) ->
                async {
                    grenze.withPermit {
                        val grund = auftrag.grund.ifBlank { "Den Text auf den aktuellen Stand bringen und zu einem stimmigen Ganzen zusammenführen." }
                        val werte = basisWerte(m.ergebnis.liste, "", recherche) + mapOf(
                            "FAKTOR" to detail(f),
                            "GRUND" to grund + if (auftrag.ergaenzung.isNotBlank()) "\nNeue Erkenntnis aus der Diskussion: ${auftrag.ergaenzung}" else "",
                            "ZUSAMMENFASSUNG" to m.ergebnis.zusammenfassung,
                        )
                        val ergebnis = try {
                            val (antwort, _) = gesichert("konsens:${f.id}", pruefe = { faktorAus(jsonAus(it)) }) {
                                frage(
                                    system(v, werte, "rolle autorin"), nachricht(v, "aufbau konsens", werte, "text konsens"),
                                    e.modell, e.denkstufe, fortschritt, webSuche = webSuche, still = true, label = "Text-Konsens id ${f.id} „${f.titel}“",
                                )
                            }
                            konsensUebernehmen(f, faktorAus(jsonAus(antwort)))
                        } catch (c: CancellationException) {
                            throw c
                        } catch (t: Throwable) {
                            KiLog.fehler("Text-Konsens für id ${f.id} „${f.titel}“ gescheitert – alter Text bleibt", t)
                            // Scheitert der neue Text, geht die Erkenntnis trotzdem nicht verloren.
                            if (auftrag.ergaenzung.isBlank()) f else f.copy(erklaerung = f.erklaerung.trimEnd() + "\n\nNeu ($datum): " + auftrag.ergaenzung)
                        }
                        val n = fertig.incrementAndGet()
                        fortschritt.status(AUTORIN, "$n von ${m.konsens.size} Texten überarbeitet …")
                        if (ergebnis.standVom != f.standVom) fortschritt.beitragFertig(AUTORIN, "„${ergebnis.titel}“ neu geschrieben – $grund", liveLeeren = false)
                        ergebnis
                    }
                }
            }.awaitAll()
        }.associateBy { it.id }
        return m.ergebnis.liste.map { neu[it.id] ?: it }
    }

    /** Der Konsens-Text ersetzt Erklärung, Kurztext, Ziel und Plan; Rang, Jahre, Evidenz und Titel hat die Gutachterin festgelegt. */
    private fun konsensUebernehmen(f: Faktor, neu: Faktor) = f.copy(
        kurz = neu.kurz.ifBlank { f.kurz },
        erklaerung = neu.erklaerung.ifBlank { f.erklaerung },
        ziel = neu.ziel.ifBlank { f.ziel },
        punkteJson = Faktor.punkteAlsJson(punkteMischen(f.punkte, neu.punkte)),
        quellenJson = Faktor.quellenAlsJson(Faktor.quellenMischen(f.quellen, neu.quellen)),
        standVom = System.currentTimeMillis(),
        vertieft = true,
    )

    /** Ein Diskussionslauf: baut je Agent Systemanweisung und Nachricht aus der Vorlage und sammelt den Verlauf. */
    private inner class Lauf(
        val v: Map<String, String>,
        val liste: List<Faktor>,
        val einwand: String,
        val verlauf: StringBuilder,
        val fortschritt: Fortschritt,
        val recherche: String,
        val pruefung: String,
    ) {
        suspend fun sprich(name: String, auftrag: String, von: Float, bis: Float, zeichen: Int, sekunden: Float): String {
            val werte = basisWerte(liste, einwand, recherche.ifBlank { "(in diesem Lauf keine Recherche-Dossiers)" }) +
                ("DISKUSSION" to verlauf.toString().trim().ifBlank { "(noch keine – du beginnst)" }) +
                ("PRUEFUNG" to pruefung.ifBlank { "(in diesem Lauf keine Einzelprüfungen)" })
            val rolle = when (name) { PRO -> "rolle forscherin"; CONTRA -> "rolle skeptiker"; else -> "rolle gutachterin" }
            // Auch die Gutachterin sucht: Sie soll strittige Quellen selbst prüfen können, statt sie mangels Zugriff zu verwerfen.
            val suche = webSuche
            fortschritt.band(von, bis, zeichen, if (suche) sekunden * 1.6f else sekunden, "$name ${if (name == RICHTER) "wägt ab" else "argumentiert"} …")
            // Die Entscheidung zählt nur als fertig, wenn sie ein lesbares JSON ist.
            val pruefe: (String) -> Unit = if (name == RICHTER) { t -> jsonAus(t) } else { _ -> }
            val (text, gespeichert) = gesichert("debatte:$auftrag", pruefe) {
                fortschritt.beitragBeginnt(name)
                frage(
                    system(v, werte, rolle), nachricht(v, AUFBAU_NACHRICHT, werte, auftrag), e.modell, e.denkstufe, fortschritt,
                    webSuche = suche, label = "$name · $auftrag",
                ).trim()
            }
            if (!gespeichert) fortschritt.beitragFertig(name, text) else fortschritt.status(name, "Aus dem Zwischenstand übernommen.")
            verlauf.append("\n### ").append(name).append(":\n").append(text).append('\n')
            return text
        }
    }

    /** Die Platzhalter, die in jedem Aufruf gelten. */
    private fun basisWerte(liste: List<Faktor>, einwand: String, recherche: String): Map<String, String> = mapOf(
        "PRO" to PRO, "CONTRA" to CONTRA, "RICHTER" to RICHTER, "AUTORIN" to AUTORIN, "EINWAND" to einwand,
        "FAKTOR_SCHEMA" to FAKTOR_SCHEMA, "PROFIL" to profilZeile(), "LISTE" to listeKompakt(liste),
        "DETAILS" to liste.joinToString("\n\n") { detail(it) }, "RECHERCHE" to recherche, "DATUM" to heute(),
        "NEU_MAX" to NEU_MAX.toString(), "PRUEFUNG" to "",
    )

    private fun system(v: Map<String, String>, werte: Map<String, String>, rolle: String): String = fuelle(
        v[AUFBAU_SYSTEM].orEmpty(),
        werte + mapOf("GRUNDANWEISUNG" to fuelle(v["grundanweisung"].orEmpty(), werte), "ROLLE" to fuelle(v[rolle].orEmpty(), werte)),
    ).trim()

    private fun nachricht(v: Map<String, String>, aufbau: String, werte: Map<String, String>, auftrag: String): String =
        fuelle(v[aufbau].orEmpty(), werte + ("AUFTRAG" to fuelle(v[auftrag].orEmpty(), werte))).trim()

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

    /** Für den Verlauf: Die Entscheidungen der Gutachterin nur als Zusammenfassung, nicht als ganzes JSON. */
    private fun beitragKurz(b: Beitrag): String = if (b.name != RICHTER) b.text else runCatching {
        val o = jsonAus(b.text)
        listOf(o.optString("einordnung"), o.optString("zusammenfassung")).filter { it.isNotBlank() }.joinToString("\n")
    }.getOrNull()?.takeIf { it.isNotBlank() }?.let { "Entscheidung: $it" } ?: "Entscheidung gefällt."

    /** Warum ein Text neu geschrieben wird, und eine eventuelle Ergänzung aus der Diskussion. */
    private class KonsensAuftrag(val grund: String, val ergaenzung: String)

    private class Mischung(val ergebnis: Aktualisierung, val konsens: Map<Long, KonsensAuftrag>)

    /**
     * Übernimmt die Entscheidung der Gutachterin, ohne je etwas aus dem Altbestand zu verlieren. Texte werden nicht mehr
     * angehängt: Was neu geschrieben werden soll, landet im Konsens-Auftrag. Mit [altlastenEinarbeiten] kommen zusätzlich
     * alle Faktoren mit alten „Neu (…)“-Absätzen dazu, damit sie einmal zu einem stimmigen Text zusammengeführt werden.
     */
    private fun mischen(liste: List<Faktor>, o: JSONObject, altlastenEinarbeiten: Boolean): Mischung {
        val nachId = liste.associateBy { it.id }
        val konsens = linkedMapOf<Long, KonsensAuftrag>()
        val reihenfolge = o.optJSONArray("reihenfolge") ?: JSONArray()
        val gesehen = mutableSetOf<Long>()
        val neueListe = mutableListOf<Faktor>()
        for (i in 0 until reihenfolge.length()) {
            val r = reihenfolge.optJSONObject(i) ?: continue
            val alt = nachId[r.optLong("id", -1)] ?: continue
            if (!gesehen.add(alt.id)) continue
            // Ältere eigene Prompts liefern noch "ergaenzung": wird jetzt eingearbeitet statt angehängt.
            val ergaenzung = r.optString("ergaenzung").trim()
            if (ergaenzung.isNotBlank()) konsens[alt.id] = KonsensAuftrag("Neue Erkenntnis einarbeiten.", ergaenzung)
            val jahre = r.optDouble("jahre", Double.NaN)
            val wirkung = r.optInt("wirkung", -1)
            val kategorie = r.optString("kategorie").trim().takeIf { k -> Kategorie.entries.any { it.name == k } }
            neueListe += alt.copy(
                titel = r.optString("titel").trim().trim('.').take(72).ifBlank { alt.titel },
                kurz = r.optString("kurz").trim().ifBlank { alt.kurz },
                ziel = r.optString("ziel").trim().ifBlank { alt.ziel },
                begruendung = r.optString("begruendung").trim().ifBlank { alt.begruendung },
                evidenz = r.optString("evidenz").takeIf { s -> Evidenz.entries.any { it.name == s } } ?: alt.evidenz,
                jahre = if (!jahre.isNaN() && jahre != 0.0) jahre.toFloat().coerceIn(-20f, 20f) else alt.jahre,
                wirkung = if (wirkung in 0..100) wirkung else alt.wirkung,
                wahrscheinlichkeit = r.optInt("wahrscheinlichkeit", -1).takeIf { it in 0..100 } ?: alt.wahrscheinlichkeit,
                kategorie = kategorie ?: alt.kategorie,
            )
        }
        // Was der Richter vergessen hat, bleibt erhalten – in seiner bisherigen Reihenfolge dahinter.
        liste.filter { it.id !in gesehen }.forEach { neueListe += it }

        // Texte, die neu geschrieben werden sollen (Fehler, veraltete Zahlen, neue Studienlage).
        o.optJSONArray("neu_schreiben")?.let { a ->
            for (i in 0 until a.length()) {
                val x = a.optJSONObject(i)
                val id = x?.optLong("id", -1) ?: a.optLong(i, -1)
                if (id !in nachId) continue
                val grund = x?.optString("grund")?.trim().orEmpty()
                val bisher = konsens[id]
                konsens[id] = KonsensAuftrag(grund.ifBlank { bisher?.grund.orEmpty() }, bisher?.ergaenzung.orEmpty())
            }
        }
        if (altlastenEinarbeiten) {
            liste.filter { "\n\nNeu (" in it.erklaerung || it.erklaerung.startsWith("Neu (") }.forEach { f ->
                if (f.id !in konsens) konsens[f.id] = KonsensAuftrag("Die angehängten „Neu (…)“-Absätze in einen einzigen, stimmigen Text einarbeiten.", "")
            }
        }

        // Hinweise der Gutachterin: Überschneidungen, veraltete Faktoren – der Nutzer entscheidet.
        val hinweise = mutableMapOf<Long, Pair<String, Long?>>()
        o.optJSONArray("hinweise")?.let { a ->
            for (i in 0 until a.length()) {
                val h = a.optJSONObject(i) ?: continue
                val id = h.optLong("id", -1)
                val text = h.optString("text").trim()
                if (id !in nachId || text.isBlank()) continue
                val mit = h.optLong("zusammenMit", -1).takeIf { it in nachId && it != id }
                // Mehrere Hinweise zum selben Faktor bleiben alle erhalten; zusammengelegt wird mit dem ersten genannten Partner.
                val bisher = hinweise[id]
                hinweise[id] = if (bisher == null) text to mit else (bisher.first + "\n\n" + text) to (bisher.second ?: mit)
            }
        }

        val jetzt = System.currentTimeMillis()
        // Die Null-Linie setzt der Code durch: Plus-Faktoren in der Reihenfolge des Richters, Räuber nach verlorenen Jahren.
        val ergebnis = ordnen(neueListe).map { f ->
            val h = hinweise[f.id]
            f.copy(
                vorherRang = nachId[f.id]?.rang, neu = false, geaendertAm = jetzt,
                hinweis = h?.first ?: f.hinweis, zusammenMit = if (h != null) h.second else f.zusammenMit,
            )
        }
        val veraendert = ergebnis.count { it.rang != it.vorherRang }
        val neu = o.optJSONArray("neu") ?: JSONArray()
        val alle = (0 until neu.length()).mapNotNull { i ->
            val n = neu.optJSONObject(i) ?: return@mapNotNull null
            faktorAus(n).takeIf { it.titel.isNotBlank() }?.copy(rang = n.optInt("rang", ergebnis.size + 1), vertieft = true, neu = true)
        }
        // Förderliche Faktoren und Lebenszeit-Räuber haben getrennte Kontingente.
        val vorschlaege = alle.filter { !it.raeuber }.take(NEU_MAX) + alle.filter { it.raeuber }.take(NEU_MAX)
        KiLog.info(
            "Entscheidung gelesen: ${reihenfolge.length()} Einträge in der Reihenfolge (von ${liste.size}), ${liste.size - gesehen.size} fehlten, " +
                "$veraendert Plätze geändert, ${konsens.size} Texte zum Neuschreiben, ${hinweise.size} Hinweise, " +
                "${alle.size} neue Faktoren geliefert, ${vorschlaege.size} übernommen",
        )
        return Mischung(
            Aktualisierung(
                ergebnis, vorschlaege, o.optString("zusammenfassung").trim(), veraendert, hinweise = hinweise.size,
                wechsel = ergebnis.filter { it.vorherRang != null && it.rang != it.vorherRang }
                    .sortedByDescending { kotlin.math.abs(it.rang - (it.vorherRang ?: it.rang)) }
                    .map { "„${it.titel}“: Platz ${it.vorherRang} → ${it.rang}" },
            ),
            konsens,
        )
    }

    // ================= Gemeinsames =================

    private fun grundanweisung(): String = buildString {
        append(
            "Du bist ein weltweit führender Experte für Langlebigkeitsforschung (Geroscience, Epidemiologie, Sportmedizin, " +
                "Ernährungswissenschaft, Schlafforschung, Psychologie, Präventivmedizin). Du betrachtest den Menschen ganzheitlich " +
                "in allen Lebensbereichen: Bewegung, Fitness, Kraft, Ernährung, Schlaf, Supplements, Stress, Geist, Beziehungen, " +
                "Sinn, Vorsorge, Umwelt, Genussmittel. Du berücksichtigst nicht nur gesicherte Evidenz (RCTs, Metaanalysen, " +
                "Mendel-Randomisierung, große Kohorten), sondern auch sehr wahrscheinliche und logisch gut begründete Faktoren – " +
                "und ordnest ehrlich ein: BELEGT, WAHRSCHEINLICH oder LOGISCH. Die Rangliste hat eine Null-Linie: Oben stehen " +
                "förderliche Verhaltensweisen mit POSITIVEN Jahren – der Gewinn an gesunder Lebenszeit durch konsequente Umsetzung " +
                "gegenüber dem Unterlassen. Unten stehen schädliche Verhaltensweisen (Lebenszeit-Räuber) mit NEGATIVEN Jahren – " +
                "die verlorene Lebenszeit gegenüber dem Unterlassen, der schädlichste ganz unten. Ein Verbot ist nie ein Plus-Faktor: " +
                "Man wird als Nichtraucher geboren, Nichtrauchen schenkt keine Jahre, Rauchen kostet sie. Der Titel nennt deshalb " +
                "das schädliche Verhalten selbst („Rauchen“, nicht „Nicht rauchen“) mit negativen Jahren. Denke sehr " +
                "gründlich, detailliert und durchdacht. Schreibe auf Deutsch, klar und konkret, ohne Heilversprechen.",
        )
        val p = e.profilText()
        if (p.isNotBlank()) append("\n\nPROFIL DES NUTZERS (berücksichtige es bei Rang, Ziel und Aufgaben): ").append(p)
    }

    private fun listeKompakt(liste: List<Faktor>) = liste.joinToString("\n") {
        "${it.id} | ${it.rang} | ${it.titel} | ${it.kat.name} | ${it.ev.name} | ${"%.1f".format(Locale.US, it.jahre)}" + if (it.zielErreicht) (if (it.raeuber) " | trifft beim Nutzer nicht zu bzw. abgestellt" else " | vom Nutzer bereits umgesetzt") else ""
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
                val antwort = auth.streamChat(
                    instructions = anweisung,
                    turns = listOf(ChatTurn("user", text.trim())),
                    model = modell,
                    reasoningEffort = stufe,
                    webSuche = webSuche,
                    onDelta = { stueck ->
                        empfangen += stueck.length
                        if (still) fortschritt?.zeichenStill(stueck) else fortschritt?.zeichen(stueck)
                    },
                )
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

    /** Ein Faktor mit allem, was die Agenten zum Prüfen und Verbessern brauchen. */
    private fun detail(f: Faktor): String = buildString {
        append("### id ").append(f.id).append(" · Rang ").append(f.rang).append(" · ").append(f.titel).append('\n')
        append("Kategorie: ").append(f.kat.name).append(" · Evidenz: ").append(f.ev.name)
            .append(" · Jahre (Erwartungswert): ").append("%.1f".format(Locale.US, f.jahre)).append(" · Wirkung: ").append(f.wirkung)
            .append(" · Wahrscheinlichkeit: ").append(f.wahrscheinlichkeit?.let { "$it %" } ?: "noch nicht geschätzt")
        if (f.zielErreicht) append(if (f.raeuber) " · beim Nutzer abgestellt" else " · vom Nutzer umgesetzt")
        append("\nKurz: ").append(f.kurz)
        append("\nErklärung: ").append(f.erklaerung.replace("\n\n", " ¶ "))
        append("\nBegründung des Rangs: ").append(f.begruendung)
        append("\nZiel: ").append(f.ziel)
        val p = f.punkte
        if (p.isNotEmpty()) {
            append("\nAufgabenplan:")
            p.forEach {
                append("\n- ").append(it.titel).append(" (").append(it.ev.name).append("): ").append(it.text)
                if (it.erledigt) append(" [vom Nutzer erledigt]")
            }
        }
        val q = f.quellen
        if (q.isNotEmpty()) append("\nQuellen: ").append(q.joinToString("; ") { listOf(it.titel, it.jahr, it.link).filter(String::isNotBlank).joinToString(", ") })
        append("\nInhalt zuletzt geprüft: ").append(f.standVom?.let { SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(it)) } ?: "noch nie (Altbestand)")
        f.hinweis?.takeIf { it.isNotBlank() }?.let { append("\nOffener Hinweis: ").append(it) }
    }

    companion object {
        const val PRO = "Forscherin Vita"
        const val CONTRA = "Skeptiker Kron"
        const val RICHTER = "Gutachterin Aeon"
        const val NUTZER = "Du"
        const val AUTORIN = "Autorin Lexa"
        const val SCHWARM = "Recherche-Schwarm"
        const val RAEUBER_JAEGER = "Räuber-Jäger"
        const val SCOUT = "Neuheiten-Scout"
        const val PRUEFSTAND = "Einzelprüfung"

        /** Höchstens so viele KI-Aufrufe gleichzeitig – mehr bremst das Codex-Kontingent. */
        const val PARALLEL = 5
        /** Höchstens so viele neue förderliche Faktoren und ebenso viele neue Räuber pro Lauf. */
        const val NEU_MAX = 5
        /** Wartezeiten, bevor ein abgerissener Aufruf komplett wiederholt wird. */
        private val WIEDERHOLUNGEN_MS = longArrayOf(10_000L, 30_000L)
        /** Stand des Standard-Prompts; ein eigener Prompt älteren Stands wird einmalig gesichert und ersetzt. */
        const val PROMPT_VERSION = 2

        /** Gehört ein Protokoll-Beitrag zum Recherche-Schwarm? */
        fun istRecherche(name: String) = name.startsWith("Rechercheur") || name == RAEUBER_JAEGER || name == SCOUT || name == SCHWARM || name == PRUEFSTAND

        const val AUFBAU_SYSTEM = "aufbau systemanweisung"
        const val AUFBAU_NACHRICHT = "aufbau nachricht"

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
            | "jahre": "Zahl, Erwartungswert = Potenzial × Wahrscheinlichkeit: positiv (z. B. 2.5) bei förderlichem Verhalten, negativ (z. B. -10) bei schädlichem", "wirkung": 0-100 (Stärke, auch bei schädlichem Verhalten positiv), "erklaerung": "4–7 Sätze", "begruendung": "2–3 Sätze, warum genau dieser Rang",
            | "ziel": "konkretes, messbares Ziel", "punkte": [{"titel": "max. 50 Zeichen", "text": "1–2 Sätze, konkret mit Dosis/Häufigkeit", "evidenz": "BELEGT|WAHRSCHEINLICH|LOGISCH"}],
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
