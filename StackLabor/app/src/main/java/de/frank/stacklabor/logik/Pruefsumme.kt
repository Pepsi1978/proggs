package de.frank.stacklabor.logik

import java.security.MessageDigest

/**
 * Die Prüfsumme entscheidet, ob eine gespeicherte Bewertung noch zum Stack passt (F-23).
 *
 * Sie geht **nur** über das, was die KI bewertet hat. Alles, was die App selbst neu rechnen
 * kann, fließt bewusst **nicht** ein — sonst wäre die Bewertung nach jedem Häkchen „veraltet",
 * obwohl sie vollkommen gültig bleibt.
 *
 * | fließt ein (macht veraltet) | fließt NICHT ein |
 * |---|---|
 * | Mittel hinzugefügt/entfernt | Häkchen an/aus (F-05) |
 * | Dosis, Einheit, Stückzahl | Ziel-Priorität (F-10) |
 * | Frequenz, „alterniert mit" | Sortieransicht (F-06) |
 * | Zusatztext für die KI | Einnahme-Reihenfolge (F-07) |
 * | Zielauswahl des Stacks (F-09) | Stack-Name |
 * | Eigene Fragen (F-11) | Hell/Dunkel, Stimme, Modellwahl |
 * | Zeitpunkt, Einnahme-Hinweis | Bewertung eines anderen Stacks |
 * | Darreichungsform, Löslichkeit | |
 * | gewählte Dosis-Variante (F-27) | |
 */
object Pruefsumme {

    /** Was von einem Mittel in die Prüfsumme eingeht. */
    data class MittelAnteil(
        val mittelId: String,
        val stueckzahl: Int,
        val mengeJeStueck: Double?,
        val einheit: String,
        val darreichungsform: String,
        val loeslichkeit: String,
        val frequenzTage: Int,
        val alterniertMit: String,
        val zusatztext: String,
        val dosisVarianteB: Double?,
    )

    /**
     * Bildet die Prüfsumme über einen Stack.
     *
     * Die Mittel werden vor dem Zusammenfügen sortiert — sonst würde bloßes Umsortieren
     * (F-07) die Summe ändern, und genau das soll es nicht.
     */
    fun fuerStack(
        zeitpunkt: String,
        einnahmeHinweis: String,
        mittel: List<MittelAnteil>,
        zielIds: List<String>,
        eigeneFragen: List<String>,
        dosisVariante: String,
    ): String {
        val bau = StringBuilder()
        bau.append("z=").append(zeitpunkt).append('\n')
        bau.append("h=").append(einnahmeHinweis).append('\n')
        bau.append("v=").append(dosisVariante).append('\n')

        mittel.sortedBy { it.mittelId }.forEach { m ->
            bau.append("m|").append(m.mittelId)
                .append('|').append(m.stueckzahl)
                .append('|').append(m.mengeJeStueck ?: "-")
                .append('|').append(m.einheit)
                .append('|').append(m.darreichungsform)
                .append('|').append(m.loeslichkeit)
                .append('|').append(m.frequenzTage)
                .append('|').append(m.alterniertMit)
                .append('|').append(m.zusatztext)
                .append('|').append(m.dosisVarianteB ?: "-")
                .append('\n')
        }
        zielIds.sorted().forEach { bau.append("z|").append(it).append('\n') }
        eigeneFragen.sorted().forEach { bau.append("f|").append(it).append('\n') }

        return hash(bau.toString())
    }

    /** Prüfsumme über alle Stacks — für die Gesamtprüfung (F-13). */
    fun fuerAlleStacks(einzelne: List<String>): String = hash(einzelne.sorted().joinToString("\n"))

    private fun hash(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(32)
}
