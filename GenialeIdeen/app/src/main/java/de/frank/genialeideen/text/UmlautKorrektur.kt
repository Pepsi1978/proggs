package de.frank.genialeideen.text

/**
 * Hebt bekannte Ersatzschreibungen wieder auf echte Umlaute (Baustein M.4).
 *
 * Bewusst **keine** pauschale Ersetzung `ae → ä`: Das zerstört richtige Wörter („Michael",
 * „Aerodynamik", „Poesie", „Duell"). Ersetzt wird ausschliesslich, was in [WORTLISTE] steht —
 * als ganzes Wort oder als Teil einer deutschen Zusammensetzung. Steht ein Wort nicht in der
 * Liste, bleibt es unangetastet.
 */
object UmlautKorrektur {

    /** Was ersetzt wurde — Wort vorher, Wort nachher. Damit die Liste wachsen kann. */
    data class Ersetzung(val vorher: String, val nachher: String)

    private val WORTLISTE: Map<String, String> = linkedMapOf(
        "ueber" to "über",
        "fuer" to "für",
        "koennen" to "können",
        "koennte" to "könnte",
        "muessen" to "müssen",
        "muesste" to "müsste",
        "moechte" to "möchte",
        "moeglich" to "möglich",
        "waehlen" to "wählen",
        "waehrend" to "während",
        "aendern" to "ändern",
        "aehnlich" to "ähnlich",
        "loeschen" to "löschen",
        "schliessen" to "schließen",
        "groesse" to "Größe",
        "groesser" to "größer",
        "gruen" to "grün",
        "zurueck" to "zurück",
        "naechste" to "nächste",
        "hoeren" to "hören",
        "oeffnen" to "öffnen",
        "erklaeren" to "erklären",
        "verfuegbar" to "verfügbar",
        "gueltig" to "gültig",
        "strasse" to "Straße",
        "gruss" to "Gruß",
        "massnahme" to "Maßnahme",
        "spaeter" to "später",
        "taeglich" to "täglich",
        "natuerlich" to "natürlich",
        "beduerfnis" to "Bedürfnis",
        "einfuehren" to "einführen",
        "durchfuehren" to "durchführen",
        "unterstuetzen" to "unterstützen",
        "beruecksichtigen" to "berücksichtigen",
        "erhoehen" to "erhöhen",
        "loesung" to "Lösung",
        "uebersicht" to "Übersicht",
        "ueberpruefen" to "überprüfen",
        "ausfuehrlich" to "ausführlich",
        "regelmaessig" to "regelmäßig",
        "gemaess" to "gemäß",
        "schoen" to "schön",
        "froehlich" to "fröhlich",
        "buero" to "Büro",
        "moebel" to "Möbel",
        "geschaeft" to "Geschäft",
        "qualitaet" to "Qualität",
        "aktivitaet" to "Aktivität",
        "universitaet" to "Universität",
        "realitaet" to "Realität",
        "kapazitaet" to "Kapazität",
        "funktionalitaet" to "Funktionalität",
        "haeufig" to "häufig",
        "kuerzlich" to "kürzlich",
        "wuensche" to "Wünsche",
        "wuenschen" to "wünschen",
        "beruehmt" to "berühmt",
        "gefuehl" to "Gefühl",
        "schluessel" to "Schlüssel",
        "vergroessern" to "vergrößern",
        "verkuerzen" to "verkürzen",
        "erloes" to "Erlös",
        "umsaetze" to "Umsätze",
    )

    /** Wortbestandteile, die auch am Wortanfang oder -ende einer Zusammensetzung greifen. */
    private val ZUSAMMENSETZUNG = Regex(
        """(?i)\b\p{L}*(""" + WORTLISTE.keys.joinToString("|") + """)\p{L}*\b""",
    )

    /**
     * Korrigiert [text] und meldet über [onErsetzung], was verändert wurde.
     * Der Text bleibt Zeichen für Zeichen erhalten, wo nichts sicher zuzuordnen war.
     */
    fun korrigiere(text: String, onErsetzung: (Ersetzung) -> Unit = {}): String {
        if (text.isEmpty()) return text
        return ZUSAMMENSETZUNG.replace(text) { treffer ->
            val ganzesWort = treffer.value
            val teil = treffer.groupValues[1]
            val ersatz = WORTLISTE[teil.lowercase()] ?: return@replace ganzesWort
            val start = treffer.range.first
            val innerStart = treffer.groups[1]!!.range.first - start
            val innerEnd = treffer.groups[1]!!.range.last - start + 1
            val vorne = ganzesWort.substring(0, innerStart)
            val hinten = ganzesWort.substring(innerEnd)
            // Mitten im Wort schreibt sich der Ersatz klein; steht er vorn, zieht er die
            // Schreibweise des Originals mit.
            val kern = when {
                vorne.isNotEmpty() -> ersatz.replaceFirstChar(Char::lowercaseChar)
                teil.first().isUpperCase() || ganzesWort.first().isUpperCase() ->
                    ersatz.replaceFirstChar(Char::uppercaseChar)
                else -> ersatz.replaceFirstChar(Char::lowercaseChar)
            }
            val neu = vorne + kern + hinten
            if (neu != ganzesWort) onErsetzung(Ersetzung(ganzesWort, neu))
            neu
        }
    }
}
