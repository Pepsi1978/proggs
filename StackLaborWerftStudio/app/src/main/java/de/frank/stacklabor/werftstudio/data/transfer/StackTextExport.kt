package de.frank.stacklabor.werftstudio.data.transfer

import de.frank.stacklabor.werftstudio.domain.model.Darreichungsform
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import de.frank.stacklabor.werftstudio.domain.model.Einheit
import de.frank.stacklabor.werftstudio.domain.model.FrequenzTyp
import de.frank.stacklabor.werftstudio.domain.model.Loeslichkeit
import de.frank.stacklabor.werftstudio.domain.model.Mittel
import de.frank.stacklabor.werftstudio.domain.model.Stack
import de.frank.stacklabor.werftstudio.domain.model.StackEintrag
import de.frank.stacklabor.werftstudio.domain.model.StackEintragDosis
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Alles, was ein Stack für den Export braucht — bewusst ohne Datenbank-Zugriff. */
data class StackExportDaten(
    val stack: Stack,
    val eintraege: List<StackEintrag>,
    val mittelNachId: Map<String, Mittel>,
    /** Die Ziele dieses Stacks, nach Rang geordnet. */
    val ziele: List<StackZielText>,
)

data class StackZielText(val rang: Int, val text: String)

/**
 * Schreibt einen Stack (oder alle) als lesbare Textdatei heraus — mit allen Angaben, die zu
 * einem Mittel gespeichert sind, damit die Zusammenstellung auch außerhalb der App
 * nachvollziehbar bleibt.
 */
object StackTextExport {
    private const val BREITE = 78
    private val DOPPELLINIE = "═".repeat(BREITE)
    private val LINIE = "─".repeat(BREITE)

    fun einzelnerStack(
        daten: StackExportDaten,
        variante: DosisVariante,
        versionName: String,
        zeitpunktEpochMillis: Long,
    ): String = buildString {
        appendLine(kopf("Stack-Export", versionName, zeitpunktEpochMillis, variante))
        appendLine()
        append(abschnitt(daten, variante))
    }

    fun alleStacks(
        daten: List<StackExportDaten>,
        variante: DosisVariante,
        versionName: String,
        zeitpunktEpochMillis: Long,
    ): String = buildString {
        appendLine(kopf("Gesamt-Export aller Stacks", versionName, zeitpunktEpochMillis, variante))
        appendLine()
        appendLine("ÜBERSICHT")
        appendLine(LINIE)
        daten.forEachIndexed { index, eintrag ->
            val zeitpunkt = eintrag.stack.zeitpunkt.ifBlank { "ohne Zeitangabe" }
            appendLine("${(index + 1).toString().padStart(2)}. ${eintrag.stack.name}  —  $zeitpunkt  —  ${eintrag.eintraege.size} Mittel")
        }
        appendLine(LINIE)
        appendLine()
        daten.forEach { eintrag ->
            append(abschnitt(eintrag, variante))
            appendLine()
        }
        appendLine(DOPPELLINIE)
        appendLine("Ende des Exports — ${daten.size} Stacks, ${daten.sumOf { it.eintraege.size }} Einträge.")
        appendLine(DOPPELLINIE)
    }

    /** Dateiname ohne Zeichen, die auf einem Dateisystem Ärger machen. */
    fun dateiname(name: String, zeitpunktEpochMillis: Long): String {
        val sauber = name.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "-")
            .replace(Regex("\\s+"), "-")
            .trim('-')
            .ifBlank { "stack" }
        val stempel = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", Locale.GERMANY)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(zeitpunktEpochMillis))
        return "$sauber-$stempel.txt"
    }

    private fun kopf(titel: String, versionName: String, zeitpunktEpochMillis: Long, variante: DosisVariante): String {
        val stempel = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMANY)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(zeitpunktEpochMillis))
        return buildString {
            appendLine(DOPPELLINIE)
            appendLine("StackLabor Werft Studio — $titel")
            appendLine("Erstellt am $stempel · App-Version $versionName")
            appendLine("Dosis-Variante: ${variante.anzeige()}")
            append(DOPPELLINIE)
        }
    }

    private fun abschnitt(daten: StackExportDaten, variante: DosisVariante): String = buildString {
        val stack = daten.stack
        appendLine(DOPPELLINIE)
        appendLine(stack.name.uppercase(Locale.GERMANY))
        val untertitel = listOf(stack.zeitpunkt, stack.einnahmeHinweis).filter(String::isNotBlank)
        if (untertitel.isNotEmpty()) appendLine(untertitel.joinToString(" · "))
        appendLine(DOPPELLINIE)
        appendLine()
        val aktive = daten.eintraege.count { it.aktiv }
        appendLine("Mittel im Stack: ${daten.eintraege.size} (davon aktiv: $aktive)")
        appendLine()

        appendLine("ZIELE")
        appendLine(LINIE)
        if (daten.ziele.isEmpty()) {
            appendLine("  Für diesen Stack sind keine Ziele festgelegt.")
        } else {
            daten.ziele.sortedBy { it.rang }.forEach { ziel ->
                appendLine("  ${ziel.rang.toString().padStart(2)}. ${ziel.text}")
            }
        }
        appendLine()

        appendLine("MITTEL")
        appendLine(LINIE)
        if (daten.eintraege.isEmpty()) {
            appendLine("  In diesem Stack ist noch kein Mittel eingetragen.")
        }
        daten.eintraege.sortedBy { it.reihenfolge }.forEachIndexed { index, eintrag ->
            val mittel = daten.mittelNachId[eintrag.mittelId]
            val name = mittel?.name ?: "Unbekanntes Mittel (${eintrag.mittelId})"
            appendLine("${(index + 1).toString().padStart(2)}. $name${if (eintrag.aktiv) "" else "   [ausgeschaltet]"}")
            zeile("Dosis (frei)", dosisText(eintrag.dosen.firstOrNull { it.variante == DosisVariante.FREI }))
            eintrag.dosen.firstOrNull { it.variante == DosisVariante.DIENST }?.let {
                zeile("Dosis (Dienst)", dosisText(it))
            }
            zeile("Einnahme", frequenzText(eintrag))
            zeile("Löslichkeit", mittel?.loeslichkeit?.anzeige())
            zeile("Darreichung", mittel?.darreichungsform?.anzeige())
            zeile("Hersteller", mittel?.hersteller)
            zeile("Durchfallrisiko", if (mittel?.durchfallrisiko == true) "ja" else null)
            zeile("Beistoffe", mittel?.beistoffe)
            if (mittel?.wirkstoffkomponenten?.isNotEmpty() == true) {
                zeile(
                    "Wirkstoffe",
                    mittel.wirkstoffkomponenten.joinToString(", ") { komponente ->
                        listOfNotNull(
                            komponente.name,
                            komponente.menge?.let { "${it.schoen()} ${komponente.einheit?.anzeige().orEmpty()}".trim() },
                        ).joinToString(" ")
                    },
                )
            }
            zeile("Gemeinsam nehmen", if (eintrag.gruppeId != null) "ja — als Gruppe" else null)
            zeile(
                "Wechselt sich ab mit",
                eintrag.alternationsPartnerMittelIds
                    .mapNotNull { daten.mittelNachId[it]?.name ?: it }
                    .joinToString(", ")
                    .ifBlank { null },
            )
            zeile("Notiz", eintrag.zusatztext)
            zeile("Hinweis", eintrag.offenerHinweis)
            appendLine(LINIE)
        }
    }

    private fun StringBuilder.zeile(bezeichnung: String, wert: String?) {
        if (wert.isNullOrBlank()) return
        appendLine("    ${bezeichnung.padEnd(20)}$wert")
    }

    private fun dosisText(dosis: StackEintragDosis?): String? {
        if (dosis == null) return null
        val stueck = dosis.stueckzahl.schoen()
        val menge = dosis.mengeJeStueck
        val einheit = dosis.einheitText ?: dosis.einheit?.anzeige().orEmpty()
        if (menge == null) return "$stueck ${einheit.ifBlank { "Stück" }}"
        val gesamt = menge.multiply(dosis.stueckzahl)
        return "$stueck × ${menge.schoen()} $einheit = ${gesamt.schoen()} $einheit".trim()
    }

    private fun frequenzText(eintrag: StackEintrag): String = when (eintrag.frequenzTyp) {
        FrequenzTyp.TAEGLICH -> "täglich"
        FrequenzTyp.ALLE_N_TAGE -> "alle ${eintrag.alleNTage ?: 2} Tage"
    }

    private fun BigDecimal.schoen(): String = stripTrailingZeros().toPlainString()

    private fun DosisVariante.anzeige(): String = when (this) {
        DosisVariante.FREI -> "Frei"
        DosisVariante.DIENST -> "Dienst"
    }

    private fun Loeslichkeit.anzeige(): String = when (this) {
        Loeslichkeit.WASSER -> "wasserlöslich"
        Loeslichkeit.FETT -> "fettlöslich"
        Loeslichkeit.BEIDES -> "wasser- und fettlöslich"
    }

    private fun Darreichungsform.anzeige(): String = when (this) {
        Darreichungsform.KAPSEL -> "Kapsel"
        Darreichungsform.TABLETTE -> "Tablette"
        Darreichungsform.LOEFFEL -> "Löffel"
        Darreichungsform.TASSE -> "Tasse"
        Darreichungsform.PULVER -> "Pulver"
        Darreichungsform.TROPFEN -> "Tropfen"
        Darreichungsform.SONSTIGE -> "sonstige"
    }

    private fun Einheit.anzeige(): String = when (this) {
        Einheit.MG -> "mg"
        Einheit.UG -> "µg"
        Einheit.G -> "g"
        Einheit.ML -> "ml"
        Einheit.IE -> "IE"
        Einheit.STUECK -> "Stück"
        Einheit.TASSE -> "Tasse"
    }
}
