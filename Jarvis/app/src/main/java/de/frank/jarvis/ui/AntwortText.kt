package de.frank.jarvis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.ui.theme.LocalFarben

/** Ein Baustein einer Antwort: Überschrift, Absatz, Stichpunkt (mit Zeichen davor), Trennlinie oder Codeblock. */
private sealed class Block {
    class Ueberschrift(val stufe: Int, val text: String) : Block()
    class Absatz(val text: String) : Block()
    class Punkt(val marker: String, val text: String, val ebene: Int) : Block()
    object Linie : Block()
    class Code(val text: String) : Block()
}

/** Ein Symbol oder Emoji am Zeilenanfang wird zum Marker der Zeile, statt mitten im Text zu stehen. */
private val SYMBOL_VORN = Regex("^([\\p{So}\\p{Sk}\\u2190-\\u21FF\\u2300-\\u27BF\\u2B00-\\u2BFF\\x{1F000}-\\x{1FAFF}][\\uFE0F\\u200D\\x{1F3FB}-\\x{1F3FF}\\p{So}]*)\\s+(.+)$")

private fun zerlegen(roh: String): List<Block> {
    val bloecke = mutableListOf<Block>()
    val absatz = StringBuilder()
    fun absatzFertig() { if (absatz.isNotBlank()) bloecke += Block.Absatz(absatz.toString().trim()); absatz.setLength(0) }
    val zeilen = roh.replace("\r", "").lines()
    var i = 0
    while (i < zeilen.size) {
        val z = zeilen[i]
        val t = z.trim()
        val einzug = (z.length - z.trimStart().length) / 2
        when {
            t.startsWith("```") -> {
                absatzFertig()
                val code = StringBuilder()
                i++
                while (i < zeilen.size && !zeilen[i].trim().startsWith("```")) { code.appendLine(zeilen[i]); i++ }
                bloecke += Block.Code(code.toString().trimEnd())
            }
            t.isEmpty() -> absatzFertig()
            t.matches(Regex("^(-{3,}|\\*{3,}|_{3,}|─{3,})$")) -> { absatzFertig(); bloecke += Block.Linie }
            t.startsWith("#") -> { absatzFertig(); val s = t.takeWhile { it == '#' }.length; bloecke += Block.Ueberschrift(s.coerceIn(1, 4), t.drop(s).trim()) }
            Regex("^[-*+•]\\s+.+").matches(t) -> {
                absatzFertig()
                val inhalt = t.drop(1).trim()
                val sym = SYMBOL_VORN.find(inhalt)
                bloecke += if (sym != null) Block.Punkt(sym.groupValues[1], sym.groupValues[2], einzug) else Block.Punkt("•", inhalt, einzug)
            }
            Regex("^\\d{1,3}[.)]\\s+.+").matches(t) -> {
                absatzFertig()
                val nr = t.takeWhile { it.isDigit() }
                bloecke += Block.Punkt("$nr.", t.drop(nr.length + 1).trim(), einzug)
            }
            // Zeile, die mit einem Symbol beginnt („✅ Genialer Wecker installiert · 1.1.119“): wie ein Stichpunkt mit diesem Zeichen.
            SYMBOL_VORN.matches(t) -> {
                absatzFertig()
                val m = SYMBOL_VORN.find(t)!!
                bloecke += Block.Punkt(m.groupValues[1], m.groupValues[2], einzug)
            }
            else -> { if (absatz.isNotEmpty()) absatz.append('\n'); absatz.append(t) }
        }
        i++
    }
    absatzFertig()
    return bloecke
}

/**
 * Eine Antwort von Jarvis als gut lesbarer Fließtext wie in der ChatGPT-App: kurze Absätze, kleine Überschriften,
 * Stichpunkte mit hängendem Einzug (das Symbol steht vorn, der Text bündig daneben), Trennlinien, Code, **fett** und *kursiv*.
 */
@Composable
fun AntwortText(text: String, modifier: Modifier = Modifier, farbe: Color? = null) {
    val f = LocalFarben.current
    val c = farbe ?: f.text
    val bloecke = remember(text) { zerlegen(text) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bloecke.forEach { b ->
            when (b) {
                is Block.Ueberschrift -> Text(
                    buildAnnotatedString { inline(b.text) }, Modifier.padding(top = 4.dp), color = c,
                    fontSize = when (b.stufe) { 1 -> 20.sp; 2 -> 18.sp; else -> 16.5.sp }, fontWeight = FontWeight.Bold, lineHeight = 24.sp,
                )
                is Block.Absatz -> Text(buildAnnotatedString { inline(b.text) }, color = c, fontSize = 16.sp, lineHeight = 23.sp)
                is Block.Punkt -> Row(Modifier.fillMaxWidth().padding(start = (b.ebene * 18).dp)) {
                    Text(b.marker, Modifier.widthIn(min = 22.dp).padding(end = 6.dp), color = if (b.marker == "•" || b.marker.endsWith(".")) f.primaer else c,
                        fontSize = 16.sp, lineHeight = 23.sp, fontWeight = if (b.marker.endsWith(".")) FontWeight.SemiBold else FontWeight.Normal)
                    Text(buildAnnotatedString { inline(b.text) }, Modifier.weight(1f), color = c, fontSize = 16.sp, lineHeight = 23.sp)
                }
                Block.Linie -> Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(f.textLeise.copy(alpha = 0.25f)))
                is Block.Code -> Text(
                    b.text, Modifier.fillMaxWidth().background(f.flaeche, RoundedCornerShape(10.dp)).padding(10.dp),
                    color = c, fontFamily = FontFamily.Monospace, fontSize = 13.5.sp, lineHeight = 19.sp,
                )
            }
        }
    }
}
