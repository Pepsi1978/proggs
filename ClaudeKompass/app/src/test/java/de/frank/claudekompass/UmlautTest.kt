package de.frank.claudekompass

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wacht darüber, dass in der Wissensbasis echte Umlaute stehen.
 *
 * Der Hintergrund: Die Texte entstehen in Python-Quellen, die aus Gründen der Robustheit in
 * reinem ASCII geschrieben sind. Beim Erzeugen der Beigaben werden sie umgesetzt. Geht dabei
 * etwas schief — eine neue Wortform, die die Regel nicht kennt, oder ein Text, der am
 * Erzeuger vorbei eingetragen wurde —, sähe man das erst auf dem Gerät.
 *
 * Der Test prüft beide Richtungen:
 *  1. keine übrig gebliebene Ersatzschreibung („fuer", „Gedaechtnis"),
 *  2. kein falsch gebildeter Umlaut („aktülle", „Baün") — der Schaden, den eine zu grobe
 *     Ersetzungsregel anrichtet.
 */
class UmlautTest {

    private val beigaben = listOf(
        "slash_befehle.json",
        "config_einstellungen.json",
        "best_practices.json",
    )

    /** Nur die deutschen Felder; `name` und `englisch` bleiben ausdrücklich unberührt. */
    private val deutscheFelder = listOf("kurz", "erklaerung", "kategorie", "ersatz")

    private val wortMuster = Regex("[A-Za-zÄÖÜäöüß]+")

    /**
     * Woerter, in denen `ae`, `oe` oder `ue` sprachlich richtig sind.
     *
     * Zwei Gruppen: deutsche Woerter, in denen die Buchstaben zu verschiedenen Silben gehoeren
     * (neu-e, Ste-u-erung, ak-tu-ell), und englische Begriffe, die unuebersetzt bleiben.
     */
    private val erlaubt = setOf(
        "neue", "neuen", "neuer", "neues", "neueste", "neuesten", "neuere",
        "erneuern", "erneuert", "erneuerung", "steuerung", "steuert", "steuern",
        "steuerst", "gesteuert", "zeitsteuerung", "fernsteuerung", "dauer", "dauert",
        "dauern", "dauerhaft", "dauerhafte", "dauerhaftem", "gedauert", "genaue",
        "genauer", "ungenauer", "teuer", "bauen", "einzubauen", "nachbauen",
        "bauergebnisse", "bequem", "bequemer", "bequemlichkeit", "quelle", "quellen",
        "quellenangabe", "quellenangaben", "bezugsquellen", "einstellungsquellen",
        "paketquellen", "schauen",
        "aktuell", "aktuelle", "aktuellen", "aktueller", "aktuelles", "zuerst",
        "individuell", "manuell", "virtuell", "eventuell",
        "request", "requests", "response", "true", "false", "value", "values",
        "continue", "queue", "source", "sources", "issue", "issues",
        "askuserquestiontimeout",
    )

    /**
     * Schreibweisen, die ein Eszett brauchen — als Liste, nicht als Regel.
     *
     * Bei `ss` laesst sich sprachlich nicht allgemein entscheiden, ob ein Eszett hingehoert:
     * „muss" und „dass" sind richtig, „schliessen" und „Schutzmassnahme" nicht. Deshalb
     * haelt dieser Test genau die Faelle fest, die schon einmal falsch waren — der
     * wertvollste Testtyp, weil er verhindert, dass ein bekannter Fehler wiederkehrt.
     */
    private val brauchtEszett = setOf(
        "heisst", "heissen", "schliesst", "schliessen", "schliessende", "schliessenden",
        "ausschliesslich", "ausschliesst", "ausschliessen", "anschliessen",
        "anschliessend", "anschliessende", "schliesslich", "einschliesslich",
        "gross", "grosse", "grossen", "grosser", "grosses", "groesser", "groessere",
        "groesste", "groessten", "groesse", "grossteil", "grosszuegig",
        "ausserhalb", "ausser", "aussen", "ausserdem", "aeusserst", "draussen",
        "weiss", "weisst", "weisse", "weiterweisst",
        "massstab", "massgeblich", "massnahme", "massnahmen", "schutzmassnahme",
        "schutzmassnahmen", "gemaess", "erfahrungsgemaess", "sinngemaess",
        "standardmaessig", "planmaessig", "einigermassen", "gleichermassen",
        "fliessend", "fliessende", "fliesst", "fliessen",
        "strasse", "strassen", "fuss", "stossen", "angestossen", "gestossen",
        "reisst", "beisst", "giesst", "spass", "verstoss", "verstoesst",
        "regelmaessig", "unregelmaessig", "zuverlaessig", "zuverlaessige",
        "dreissig", "liess", "liessen", "uebermaessig",
    )

    /** Formen, die nur an einer Morphemgrenze entstehen und deshalb richtig sind. */
    private val gewollteMorphemgrenzen = setOf(
        "dateiänderung", "dateiänderungen", "geändert", "geänderte", "geänderten",
        "geöffnet", "geöffnete", "mitgeänderte",
    )

    private fun ladeTexte(): List<Triple<String, String, String>> {
        val ordner = File("src/main/assets")
        assertTrue(
            "Die Beigaben müssen unter ${ordner.absolutePath} liegen",
            ordner.isDirectory,
        )
        val ergebnis = mutableListOf<Triple<String, String, String>>()
        for (datei in beigaben) {
            val json = JSONObject(File(ordner, datei).readText(Charsets.UTF_8))
            val feld = json.getJSONArray("eintraege")
            for (index in 0 until feld.length()) {
                val eintrag = feld.getJSONObject(index)
                val name = eintrag.optString("name")
                for (schluessel in deutscheFelder) {
                    val text = eintrag.optString(schluessel)
                    if (text.isNotBlank()) ergebnis += Triple(datei, name, text)
                }
            }
        }
        assertTrue("Es müssen Texte gefunden werden", ergebnis.size > 500)
        return ergebnis
    }

    @Test
    fun keineErsatzschreibungStattUmlaut() {
        val funde = mutableListOf<String>()
        for ((datei, name, text) in ladeTexte()) {
            for (wort in wortMuster.findAll(text).map { it.value }) {
                val klein = wort.lowercase()
                if (klein in erlaubt) continue
                if (!Regex("ae|oe|ue").containsMatchIn(klein)) continue
                funde += "$datei / $name: $wort"
            }
        }
        assertTrue(bericht("tragen eine Ersatzschreibung statt echter Umlaute", funde), funde.isEmpty())
    }

    @Test
    fun keinFehlendesEszett() {
        val funde = mutableListOf<String>()
        for ((datei, name, text) in ladeTexte()) {
            for (wort in wortMuster.findAll(text).map { it.value }) {
                if (wort.lowercase() in brauchtEszett) funde += "$datei / $name: $wort"
            }
        }
        assertTrue(bericht("brauchen ein ß statt ss", funde), funde.isEmpty())
    }

    /** Baut die Fehlermeldung: was falsch ist, wie oft, und die ersten Fundstellen. */
    private fun bericht(was: String, funde: List<String>): String =
        "Diese Wörter $was (${funde.size} Stück):\n" + funde.take(30).joinToString("\n")

    @Test
    fun keineFalschGebildetenUmlaute() {
        // „aktülle", „Baün", „Qülle": das Ergebnis einer zu groben Ersetzungsregel. Ein Umlaut
        // steht im Deutschen nie direkt hinter einem anderen Vokal oder hinter einem q.
        val funde = mutableListOf<String>()
        for ((datei, name, text) in ladeTexte()) {
            for (wort in wortMuster.findAll(text).map { it.value }) {
                if (wort.lowercase() in gewollteMorphemgrenzen) continue
                if (!Regex("[aeiouAEIOUqQ][äöüÄÖÜ]").containsMatchIn(wort)) continue
                funde += "$datei / $name: $wort"
            }
        }
        assertTrue(
            "Diese Wörter tragen einen Umlaut hinter einem Vokal — das ist das Ergebnis einer " +
                "zu groben Ersetzung (${funde.size} Stück):\n" + funde.take(30).joinToString("\n"),
            funde.isEmpty(),
        )
    }

    @Test
    fun dieEnglischenOriginaleBleibenUnangetastet() {
        // Die offiziellen Beschreibungen sind der Beleg. Ein Umlaut darin wäre eine Verfälschung.
        val ordner = File("src/main/assets")
        val funde = mutableListOf<String>()
        for (datei in beigaben) {
            val json = JSONObject(File(ordner, datei).readText(Charsets.UTF_8))
            val feld = json.getJSONArray("eintraege")
            for (index in 0 until feld.length()) {
                val eintrag = feld.getJSONObject(index)
                val englisch = eintrag.optString("englisch")
                if (Regex("[äöüÄÖÜß]").containsMatchIn(englisch)) {
                    funde += "$datei / ${eintrag.optString("name")}: $englisch"
                }
            }
        }
        assertTrue(
            "In diesen englischen Originalen steht ein Umlaut:\n" + funde.joinToString("\n"),
            funde.isEmpty(),
        )
    }
}
