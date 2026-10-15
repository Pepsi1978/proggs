package de.frank.jarvis.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Meldungen eines Programmierauftrags im Chat, mit den Emojis wie geschickt (Franks Wunsch vom 15.10.2026). Fett wird, was
 * Claude selbst mit **…** markiert, dazu die wichtigsten Fakten: Committet/Gepusht/Installiert/APK-Update samt Wert, Bau-
 * und Fehlerzeilen, Versionen, versionCode, Commit-Kennungen und die Dauer. Gespeichert bleibt der Originaltext.
 */
private val WICHTIG = Regex(
    "(?:Committet|Gepusht|Installiert|APK-Update|Gemergt|Codex-Review|Version)\\s*:\\s*[^\\n,;]+" +
        "|Bau (?:erfolgreich|fehlgeschlagen)|(?:Push|Commit|Installation|APK-Update) fehlgeschlagen" +
        "|Auf dem Handy installiert|APK-Update für UpdateStation bereit|Advisor wird befragt|Skill [\\w-]+ geladen" +
        "|Dauer seit Start: [^\\n]+|versionCode \\d+|\\bv?\\d+\\.\\d+\\.\\d+\\b|\\b[0-9a-f]{7,12}\\b(?=\\))",
)
private val FETT_MARKDOWN = Regex("\\*\\*(.+?)\\*\\*")

private fun aufbereiten(roh: String): AnnotatedString {
    // **…** auflösen und die Stellen merken, danach die wichtigen Fakten im bereinigten Text suchen.
    val fett = mutableListOf<IntRange>()
    val rein = StringBuilder()
    var pos = 0
    FETT_MARKDOWN.findAll(roh).forEach { m ->
        rein.append(roh, pos, m.range.first)
        val start = rein.length
        rein.append(m.groupValues[1])
        fett += start until rein.length
        pos = m.range.last + 1
    }
    rein.append(roh, pos, roh.length)
    val text = rein.toString()
    WICHTIG.findAll(text).forEach { fett += it.range }
    return buildAnnotatedString {
        append(text)
        fett.forEach { addStyle(SpanStyle(fontWeight = FontWeight.Bold), it.first, it.last + 1) }
    }
}

@Composable
fun ProgrammierText(text: String, modifier: Modifier = Modifier, farbe: Color, fontWeight: FontWeight? = null) {
    val annotiert = remember(text) { aufbereiten(text) }
    Text(annotiert, modifier, color = farbe, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = fontWeight)
}
