package de.frank.perfectmoment.backup

import android.content.Context
import android.net.Uri
import de.frank.perfectmoment.data.local.SkillEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Alle Skill-Texte als reine Textdatei — Wort für Wort so, wie sie im Editor stehen.
 *
 * Anders als die Sicherung (JSON, zum Zurückholen gedacht) ist das Ergebnis zum Lesen und
 * Weiterbearbeiten am Rechner gedacht: jeder Skill bekommt eine große Überschrift mit seiner
 * Nummer, darunter folgt der unveränderte Originaltext.
 */
object SkillTextExport {

    private const val RULE = "════════════════════════════════════════════════════════════"

    fun buildText(skills: List<SkillEntity>): String =
        skills.mapIndexed { index, skill ->
            buildString {
                appendLine(RULE)
                appendLine("SKILL ${index + 1}: ${skill.name}")
                appendLine(RULE)
                appendLine()
                appendLine(skill.text)
            }
        }.joinToString("\n")

    /** Name mit Datum, damit mehrere Ausgaben nebeneinander erkennbar bleiben. */
    fun suggestedName(): String =
        "perfectmoment-skills-${SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())}.txt"

    suspend fun write(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
        // "wt" schneidet die Datei ab; ohne das bliebe bei kürzerem Inhalt alter Text stehen.
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(text.toByteArray())
        } ?: throw IllegalStateException("Die Datei konnte nicht geschrieben werden.")
    }
}
