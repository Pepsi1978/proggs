package de.frank.claudekompass.audio

import de.frank.claudekompass.observability.KompassLog

/** Ein Abschnitt der Antwort, samt der Kennzahlen, an denen sich eine Erfindung erkennen lässt. */
data class GroqAbschnitt(
    val von: Double?,
    val bis: Double?,
    val text: String?,
    val keineSpracheWahrscheinlichkeit: Double?,
    val mittlereLogWahrscheinlichkeit: Double?,
    val kompressionsRate: Double?,
)

data class GroqAntwort(
    val text: String,
    val abschnitte: List<GroqAbschnitt>? = null,
)

/** Welche Schichten aktiv sind. Voreinstellung: alle vier (Referenz, Baustein F). */
data class FilterSchalter(
    val schicht1Stille: Boolean = true,
    val schicht2Kennzahlen: Boolean = true,
    val schicht3Zeitstempel: Boolean = true,
    val schicht4Floskeln: Boolean = true,
)

/**
 * Die Schichten 2 bis 4 gegen erfundene Sätze (Referenz, Baustein F, Punkt 6.2).
 *
 * Whisper füllt Stille mit Sätzen, die es aus seinem Training kennt — „Vielen Dank fürs
 * Zuschauen", „Untertitel des ZDF". Dagegen stehen vier Schichten; Schicht 1 (gar nicht erst
 * senden) sitzt vor dem Hochladen, die übrigen drei hier.
 *
 * Die goldene Regel zu Schicht 4: Eine Floskel wird NIEMALS allein wegen ihres Wortlauts
 * verworfen. Nur die Kombination aus Kürze, exaktem Treffer und Stille-Umfeld darf löschen —
 * sonst verschwindet ein bewusst gesprochenes „Vielen Dank" aus dem Diktat.
 */
class HalluzinationsFilter(private val schalter: FilterSchalter = FilterSchalter()) {

    fun filtere(antwort: GroqAntwort, analyse: SprachAnalyse?): String {
        val abschnitte = antwort.abschnitte
        if (abschnitte.isNullOrEmpty()) return sperreFloskel(antwort.text.trim(), analyse)

        // --- Schicht 2: Kennzahlen aus der ausführlichen Antwort ---------------------------
        val sicher = if (!schalter.schicht2Kennzahlen) {
            abschnitte
        } else {
            abschnitte.filter { abschnitt ->
                val verwerfen = istErfunden(abschnitt)
                if (verwerfen) {
                    KompassLog.info(
                        "HalluzinationsFilter",
                        "schicht2",
                        "Abschnitt verworfen",
                        mapOf(
                            "text" to abschnitt.text?.trim()?.take(TEXT_GRENZE),
                            "keineSprache" to abschnitt.keineSpracheWahrscheinlichkeit,
                            "logWahrsch" to abschnitt.mittlereLogWahrscheinlichkeit,
                            "kompression" to abschnitt.kompressionsRate,
                        ),
                    )
                }
                !verwerfen
            }
        }
        if (sicher.isEmpty()) {
            KompassLog.info("HalluzinationsFilter", "schicht2", "Alle Abschnitte verworfen", mapOf("anzahl" to abschnitte.size))
            return ""
        }

        // --- Schicht 3: Zeitstempel gegen die Stille-Erkennung ----------------------------
        val abgeglichen = if (analyse == null || !schalter.schicht3Zeitstempel) {
            sicher
        } else {
            sicher.filter { abschnitt ->
                val von = abschnitt.von ?: return@filter true
                val bis = abschnitt.bis ?: return@filter true
                val hatSprache = analyse.abschnittHatSprache(von, bis)
                if (!hatSprache) {
                    KompassLog.info(
                        "HalluzinationsFilter",
                        "schicht3",
                        "Abschnitt ohne Ton verworfen",
                        mapOf("text" to abschnitt.text?.trim()?.take(TEXT_GRENZE), "von" to von, "bis" to bis),
                    )
                }
                hatSprache
            }
        }

        // Sicherung: Würden ALLE Abschnitte fallen, liegt eher ein Zeitstempel-Versatz vor als
        // eine Halluzination. Dann gilt das Ergebnis von Schicht 2 — lieber etwas zu viel
        // behalten als ein ganzes Diktat verlieren.
        val behalten = if (abgeglichen.isEmpty()) {
            KompassLog.warn(
                "HalluzinationsFilter",
                "schicht3",
                "Alle Abschnitte wären gefallen; behalte Schicht 2 wegen wahrscheinlichem Zeitversatz",
                mapOf("anzahl" to sicher.size),
            )
            sicher
        } else {
            abgeglichen
        }

        val zusammengesetzt = behalten.joinToString(" ") { it.text?.trim().orEmpty() }.trim()
        return sperreFloskel(zusammengesetzt, analyse)
    }

    private fun istErfunden(abschnitt: GroqAbschnitt): Boolean {
        val keineSprache = abschnitt.keineSpracheWahrscheinlichkeit ?: 0.0
        val logWahrscheinlichkeit = abschnitt.mittlereLogWahrscheinlichkeit ?: 0.0
        val kompression = abschnitt.kompressionsRate ?: 0.0
        // Unsicher UND unwahrscheinlich: das typische Muster einer erfundenen Zeile.
        if (keineSprache > KEINE_SPRACHE_SCHWELLE && logWahrscheinlichkeit < LOG_WAHRSCH_SCHWELLE) return true
        // Hohe Kompression heisst: Das Modell wiederholt sich in einer Schleife.
        if (kompression > KOMPRESSION_SCHWELLE) return true
        val dauer = (abschnitt.bis ?: 0.0) - (abschnitt.von ?: 0.0)
        return dauer > 0 && dauer < MINI_GERAEUSCH_SEKUNDEN && keineSprache > KEINE_SPRACHE_SCHWELLE
    }

    // --- Schicht 4: Floskel-Sperrliste ----------------------------------------------------

    private fun sperreFloskel(text: String, analyse: SprachAnalyse?): String {
        if (!schalter.schicht4Floskeln) return text
        if (!istGesperrteFloskel(text, analyse)) return text
        KompassLog.info(
            "HalluzinationsFilter",
            "schicht4",
            "Kurze Floskel im Stille-Umfeld verworfen",
            mapOf("text" to text.take(TEXT_GRENZE)),
        )
        return ""
    }

    private fun istGesperrteFloskel(text: String, analyse: SprachAnalyse?): Boolean {
        // Alle drei Bedingungen müssen gelten. Fehlt die Analyse, fehlt die dritte — dann
        // wird nichts gesperrt.
        if (text.isEmpty() || text.length > FLOSKEL_MAX_ZEICHEN || analyse == null) return false
        val normalisiert = normalisiere(text)
        if (normalisiert.isEmpty()) return false
        if (normalisiert.split(' ').count(String::isNotEmpty) > FLOSKEL_MAX_WOERTER) return false
        if (normalisiert !in SPERRLISTE) return false
        return analyse.gesprochenMs < STILLE_UMFELD_MAX_MS
    }

    private fun normalisiere(text: String): String = buildString(text.length) {
        text.lowercase().forEach { zeichen -> append(if (zeichen.isLetter()) zeichen else ' ') }
    }.split(' ').filter(String::isNotEmpty).joinToString(" ")

    companion object {
        const val KEINE_SPRACHE_SCHWELLE = 0.6
        const val LOG_WAHRSCH_SCHWELLE = -1.0
        const val KOMPRESSION_SCHWELLE = 2.4
        const val MINI_GERAEUSCH_SEKUNDEN = 0.4
        const val FLOSKEL_MAX_WOERTER = 8
        const val FLOSKEL_MAX_ZEICHEN = 64
        const val STILLE_UMFELD_MAX_MS = 600
        private const val TEXT_GRENZE = 60

        val SPERRLISTE = setOf(
            "vielen dank",
            "vielen dank fürs zuschauen",
            "vielen dank fuers zuschauen",
            "vielen dank für eure aufmerksamkeit",
            "vielen dank für ihre aufmerksamkeit",
            "vielen dank für die aufmerksamkeit",
            "bis zum nächsten mal",
            "bis zum nächsten video",
            "untertitel",
            "untertitel des zdf",
            "untertitelung des zdf für funk",
            "untertitel im auftrag des zdf für funk",
            "untertitel von stephanie geiges",
            "untertitel der amara org community",
            "der text ist nicht auf deutsch",
            "thank you",
            "thank you for watching",
            "thanks for watching",
            "please subscribe",
            "subtitles by the amara org community",
        )
    }
}
