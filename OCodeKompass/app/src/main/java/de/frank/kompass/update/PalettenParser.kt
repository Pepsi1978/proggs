package de.frank.kompass.update

import de.frank.kompass.observability.KompassLog

/**
 * Liest die Befehle der Befehlspalette (Strg+P) aus dem Quelltext der OpenCode-TUI.
 *
 * Die Palette ist nirgends als Tabelle dokumentiert. Ihre Befehle stehen als Objekte im
 * Quelltext, entweder `{ name: "session.list", title: "Switch session", category: "Session", … }`
 * oder in der Sitzungsansicht `{ title: "Compact session", value: "session.compact", category: … }`.
 *
 * Gelesen wird Objekt für Objekt, nicht Zeile für Zeile: Ein Objekt beginnt an der letzten
 * öffnenden Klammer vor `title:` und endet an der passenden schliessenden. Nur Objekte mit
 * `category:` zählen — Auswahl-Dialoge tragen ebenfalls ein `title:`, aber keine Kategorie.
 * Objekte mit `hidden: true` erscheinen nicht in der Palette und bleiben draussen.
 *
 * Wechselt der Titel mit dem Zustand (`mode() === "dark" ? "Switch to light mode" : "Switch
 * to dark mode"`), werden beide Texte mit „ / " verbunden — so steht es auch im Bestand.
 */
object PalettenParser {

    /** Die Quelldateien im Repo, relativ zu `packages/tui/src/`. */
    val DATEIEN = listOf(
        "app.tsx",
        "routes/session/index.tsx",
        "component/prompt/index.tsx",
        "feature-plugins/home/tips.tsx",
        "feature-plugins/system/plugins.tsx",
        "feature-plugins/system/diff-viewer.tsx",
    )

    /** Datei, in der nur Objekte mit ausdrücklichem `namespace: "palette"` in die Palette gehören. */
    private const val NUR_MIT_NAMESPACE = "feature-plugins/system/diff-viewer.tsx"

    fun adresse(tag: String, datei: String): String =
        "https://raw.githubusercontent.com/anomalyco/opencode/$tag/packages/tui/src/$datei"

    data class PalettenBefehl(val titel: String, val kategorie: String, val kennung: String)

    private val titelFeld = Regex("\\btitle:")
    /** Der Titel-Ausdruck endet dort, wo das nächste Feld des Objekts beginnt. */
    private val naechstesFeld = Regex("\\n\\s*[a-zA-Z_]+\\s*:")
    /** Befehle, die im Quelltext wie Palettenbefehle aussehen, aber nur Tasten im Eingabefeld sind. */
    private val keinePalette = listOf("prompt.history.")
    private val kategorieFeld = Regex("\\bcategory:\\s*\"([^\"]+)\"")
    private val kennungFeld = Regex("\\b(?:name|value):\\s*\"([a-z][a-z0-9_.-]*)\"")
    /** Nur Texte mit grossem Anfangsbuchstaben: `kv.get("session_directory_filter_enabled")` ist kein Titel. */
    private val zeichenkette = Regex("\"([A-Z][^\"]{1,79})\"")

    fun lese(datei: String, quelltext: String): List<PalettenBefehl> {
        val ergebnis = mutableListOf<PalettenBefehl>()
        for (treffer in titelFeld.findAll(quelltext)) {
            val beginn = quelltext.lastIndexOf('{', treffer.range.first)
            if (beginn < 0) continue
            val ende = passendeKlammer(quelltext, beginn)
            if (ende < 0) continue
            val objekt = quelltext.substring(beginn, ende + 1)
            val kategorie = kategorieFeld.find(objekt)?.groupValues?.get(1) ?: continue
            if (Regex("\\bhidden:\\s*true").containsMatchIn(objekt)) continue
            if (datei == NUR_MIT_NAMESPACE && !objekt.contains("namespace: \"palette\"")) continue
            val kennung = kennungFeld.find(objekt)?.groupValues?.get(1).orEmpty()
            if (keinePalette.any { kennung.startsWith(it) }) continue
            val ausdruckBeginn = treffer.range.last + 1
            val ausdruckEnde = naechstesFeld.find(quelltext, ausdruckBeginn)?.range?.first
                ?.coerceAtMost(ende) ?: ende
            val titel = zeichenkette.findAll(quelltext.substring(ausdruckBeginn, ausdruckEnde))
                .map { it.groupValues[1] }
                .distinct()
                .joinToString(" / ")
            if (titel.isBlank()) continue
            ergebnis += PalettenBefehl(titel, kategorie, kennung)
        }
        KompassLog.info(
            "PalettenParser",
            "lese",
            "Palettenbefehle gelesen",
            mapOf("datei" to datei, "anzahl" to ergebnis.size),
        )
        return ergebnis.distinctBy { it.titel.lowercase() }
    }

    /** Sucht die zu `{` an [start] passende `}` und überspringt dabei Zeichenketten. */
    private fun passendeKlammer(text: String, start: Int): Int {
        var tiefe = 0
        var i = start
        var inZeichenkette: Char? = null
        while (i < text.length) {
            val c = text[i]
            if (inZeichenkette != null) {
                if (c == '\\') {
                    i += 2
                    continue
                }
                if (c == inZeichenkette) inZeichenkette = null
            } else {
                when (c) {
                    '"', '\'', '`' -> inZeichenkette = c
                    '{' -> tiefe++
                    '}' -> {
                        tiefe--
                        if (tiefe == 0) return i
                    }
                }
            }
            i++
        }
        return -1
    }

    /**
     * true, wenn [titel] schon im Bestand steht — auch als Hälfte eines Umschalters
     * („Disable animations / Enable animations").
     */
    fun istBekannt(titel: String, bestandNamen: Collection<String>): Boolean {
        val teile = bestandNamen.flatMap { name -> name.split(" / ").map { it.trim().lowercase() } }.toSet()
        return titel.split(" / ").any { it.trim().lowercase() in teile }
    }
}
