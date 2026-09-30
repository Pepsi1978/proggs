package de.frank.kompass.update

import de.frank.kompass.observability.KompassLog

/**
 * Liest die Schalter aus dem Menü `/experimental` aus dem Codex-Quelltext.
 *
 * Die Liste steht in `codex-rs/features/src/lib.rs` als `FeatureSpec`-Einträge. Ins Menü kommt
 * nur, was `Stage::Experimental { name: "…", menu_description: "…", … }` trägt. Gesucht wird
 * nach dem Stufen-Namen selbst und nicht nach `stage: Stage::Experimental`, denn manche
 * Einträge stehen in einem `if cfg!(…)`-Zweig (z. B. „Prevent sleep while running“).
 */
object ExperimentParser {

    /** Quelltext zum Release-Tag; Codex-Tags heissen `rust-v0.159.2`. */
    fun adresse(tag: String): String =
        "https://raw.githubusercontent.com/openai/codex/$tag/codex-rs/features/src/lib.rs"

    data class Schalter(val name: String, val beschreibung: String, val schluessel: String)

    private val stufe = Regex("Stage::Experimental\\s*\\{")
    private val nameFeld = Regex("\\bname:\\s*\"([^\"]+)\"")
    private val beschreibungFeld = Regex("\\bmenu_description:\\s*\"([^\"]+)\"")
    private val schluesselFeld = Regex("\\bkey:\\s*\"([a-z0-9_]+)\"")

    fun lese(quelltext: String): List<Schalter> {
        val ergebnis = mutableListOf<Schalter>()
        for (treffer in stufe.findAll(quelltext)) {
            val ende = quelltext.indexOf('}', treffer.range.last)
            if (ende < 0) continue
            val block = quelltext.substring(treffer.range.last, ende)
            val name = nameFeld.find(block)?.groupValues?.get(1) ?: continue
            val beschreibung = beschreibungFeld.find(block)?.groupValues?.get(1).orEmpty()
            // Der Schlüssel steht im umgebenden FeatureSpec vor der Stufe.
            val davor = quelltext.substring(maxOf(0, treffer.range.first - 600), treffer.range.first)
            val schluessel = schluesselFeld.findAll(davor).lastOrNull()?.groupValues?.get(1).orEmpty()
            ergebnis += Schalter(name, beschreibung, schluessel)
        }
        KompassLog.info("ExperimentParser", "lese", "Experimentelle Schalter gelesen", mapOf("anzahl" to ergebnis.size))
        return ergebnis.distinctBy { it.name.lowercase() }
    }
}
