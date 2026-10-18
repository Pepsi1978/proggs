package de.frank.jarvis.ablage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Was beim Setzen eines PDFs herauskam: für die Meldung und für die Prüfung durch den Agenten (pdf_entwurf_pruefen).
 * [eingebunden] = an ihrer Stelle gesetzte Bilder, [fehlend] = Bildverweise ohne passende Datei, [angehaengt] = Bilder ohne
 * Verweis (stehen am Ende unter „Abbildungen“).
 */
data class PdfPruefung(
    val seiten: Int,
    val titel: String,
    val titelbild: Boolean,
    val eingebunden: List<String>,
    val fehlend: List<String>,
    val angehaengt: List<String>,
    val tabellen: Int,
    val hinweise: List<String>,
) {
    fun alsText(): String = buildString {
        append("Entwurf gesetzt: ").append(seiten).append(" Seite(n) inklusive Titelseite, Dokumenttitel „").append(titel).append("“, ")
        append(if (titelbild) "mit Titelbild" else "ohne Titelbild (Titelseite nur farbig)").append(". ")
        append("Abbildungen an ihrer Stelle: ").append(if (eingebunden.isEmpty()) "keine" else eingebunden.joinToString()).append(". ")
        if (tabellen > 0) append("Tabellen: ").append(tabellen).append(". ")
        if (fehlend.isNotEmpty()) append("PROBLEM – Bildverweise ohne Datei (Dateinamen genau aus dem bild_erzeugen-Ergebnis übernehmen): ").append(fehlend.joinToString()).append(". ")
        if (angehaengt.isNotEmpty()) append("Bilder ohne Verweis im Text, sie landen am Ende unter „Abbildungen“: ").append(angehaengt.joinToString()).append(". ")
        hinweise.forEach { append("Hinweis: ").append(it).append(' ') }
        if (fehlend.isEmpty() && angehaengt.isEmpty() && hinweise.isEmpty()) append("Keine Probleme gefunden.")
    }.trim()
}

/**
 * Setzt einen Markdown-Bericht als gestaltetes DIN-A4-Dokument: Titelseite (Titelbild oder Farbfläche, Inhaltsübersicht),
 * Kopf- und Fußzeile, klare typografische Hierarchie, Kurzfassung und Merkkästen („> “) als farbige Kästen, Tabellen mit
 * Kopfzeile, Abbildungen mit Nummer und Unterschrift. Alles bleibt echter Text (auswählbar, durchsuchbar, scharf beim Zoomen);
 * Hintergründe sind bewusst statisch, weil übliche PDF-Betrachter keine Animationen abspielen.
 */
internal object PdfGestaltung {
    private const val B = 595f
    private const val H = 842f
    private const val RAND = 56f
    private const val OBEN = 72f
    private const val UNTEN = 64f
    private val BREITE = (B - 2 * RAND).toInt()

    private const val NAVY = 0xFF1B3A5C.toInt()
    private const val PETROL = 0xFF1F7A80.toInt()
    private const val TEXT = 0xFF23272B.toInt()
    private const val LEISE = 0xFF6B7480.toInt()
    private const val TINT = 0xFFEEF4F7.toInt()
    private const val KASTEN = 0xFFEAF5F5.toInt()
    private const val ZEILE = 0xFFF5F8FA.toInt()
    private const val LINIE = 0xFFD5DDE3.toInt()
    private const val CODE = 0xFFF1F3F5.toInt()
    private const val WEISS = 0xFFFFFFFF.toInt()

    private val SANS: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private val SANS_FETT: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    private val SANS_MITTEL: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val SERIF_FETT: Typeface = Typeface.create("serif", Typeface.BOLD)

    private val BILD_ZEILE = Regex("^\\s*!\\[(.*?)]\\((.+?)\\)\\s*$")
    private val INLINE = Regex(
        "\\*\\*(.+?)\\*\\*|__(.+?)__|`([^`]+)`|\\[([^\\]]+)]\\((https?://[^)\\s]+)\\)|(?<![\\w*])\\*(?![\\s*])(.+?)(?<![\\s*])\\*(?![\\w*])"
    )

    private sealed class Block {
        class Ueberschrift(val stufe: Int, val text: String) : Block()
        class Absatz(val text: String) : Block()
        class Punkt(val text: String, val ebene: Int, val nummer: String?) : Block()
        class Kasten(val text: String) : Block()
        class Code(val text: String) : Block()
        class Tabelle(val zeilen: List<List<String>>) : Block()
        class Bild(val name: String, val unterschrift: String) : Block()
        object Linie : Block()
    }

    fun setze(titel: String, text: String, ziel: File, bilder: Map<String, File>, untertitel: String? = null): PdfPruefung {
        val bloecke = zerlege(text).toMutableList()
        // Dokumenttitel: die erste Hauptüberschrift, sonst der Eintragstitel ohne Agentennamen und Uhrzeit.
        val ersteUeberschrift = bloecke.firstOrNull { it !is Block.Bild } as? Block.Ueberschrift
        val dokTitel = if (ersteUeberschrift != null && ersteUeberschrift.stufe == 1) { bloecke.remove(ersteUeberschrift); ohneZeichen(ersteUeberschrift.text) } else aufgeraeumt(titel)
        // Titelbild: ausdrücklich „Titelbild“ beschriftet, sonst ein Bild ganz am Anfang.
        val titelBlock = bloecke.filterIsInstance<Block.Bild>().firstOrNull { it.unterschrift.trim().startsWith("Titelbild", ignoreCase = true) && bilder[it.name] != null }
            ?: (bloecke.firstOrNull() as? Block.Bild)?.takeIf { bilder[it.name] != null }
        titelBlock?.let { bloecke.remove(it) }
        val titelDatei = titelBlock?.let { bilder[it.name] }

        val eingebunden = mutableListOf<String>()
        val fehlend = mutableListOf<String>()
        val hinweise = mutableListOf<String>()
        var tabellen = 0
        val doc = PdfDocument()
        try {
            val satz = Satz(doc, dokTitel)
            titelseite(satz, dokTitel, untertitel, titelDatei, bloecke.filterIsInstance<Block.Ueberschrift>().filter { it.stufe <= 2 }.map { ohneZeichen(it.text) })
            satz.neueSeite()
            var nummer = 0
            var inKurzfassung = false
            var vorher: Block? = null
            bloecke.forEach { b ->
                when (b) {
                    is Block.Ueberschrift -> { inKurzfassung = b.stufe <= 2 && b.text.trim().startsWith("Kurzfassung", ignoreCase = true); ueberschrift(satz, b, vorher == null) }
                    is Block.Absatz -> if (inKurzfassung) kasten(satz, b.text, TINT, NAVY) else absatz(satz, b.text)
                    is Block.Punkt -> punkt(satz, b, vorher !is Block.Punkt)
                    is Block.Kasten -> kasten(satz, b.text, KASTEN, PETROL)
                    is Block.Code -> code(satz, b.text)
                    is Block.Tabelle -> { tabellen++; if (b.zeilen.firstOrNull().orEmpty().size > 5) hinweise += "Eine Tabelle hat ${b.zeilen.first().size} Spalten und wird sehr eng; besser höchstens fünf."; tabelle(satz, b.zeilen) }
                    is Block.Bild -> {
                        val datei = bilder[b.name]
                        if (datei != null) { eingebunden += b.name; abbildung(satz, datei, b.unterschrift, ++nummer) }
                        else { fehlend += b.name; if (b.unterschrift.isNotBlank()) absatz(satz, b.unterschrift) }
                    }
                    Block.Linie -> trennlinie(satz)
                }
                vorher = b
            }
            val uebrig = bilder.filterKeys { it !in eingebunden && bilder[it] != titelDatei }
            if (uebrig.isNotEmpty()) {
                ueberschrift(satz, Block.Ueberschrift(2, "Abbildungen"), false)
                uebrig.values.forEach { abbildung(satz, it, "", ++nummer) }
            }
            satz.schliesse()
            FileOutputStream(ziel).use { doc.writeTo(it) }
            if (bloecke.none { it is Block.Ueberschrift }) hinweise += "Der Bericht hat keine Zwischenüberschriften (##); mit Kapiteln wird das Dokument übersichtlicher."
            return PdfPruefung(satz.nr, dokTitel, titelDatei != null, eingebunden, fehlend, uebrig.keys.toList(), tabellen, hinweise)
        } finally {
            doc.close()
        }
    }

    // ---- Zerlegen ----

    private fun zerlege(roh: String): List<Block> {
        val l = mutableListOf<Block>()
        val zeilen = roh.replace("\r", "").lines()
        val absatz = StringBuilder()
        fun absatzFertig() { if (absatz.isNotBlank()) l += Block.Absatz(absatz.toString().trim()); absatz.setLength(0) }
        var i = 0
        while (i < zeilen.size) {
            val z = zeilen[i]
            val t = z.trim()
            val einzug = (z.length - z.trimStart().length) / 2
            when {
                t.startsWith("```") -> {
                    absatzFertig()
                    val c = StringBuilder()
                    i++
                    while (i < zeilen.size && !zeilen[i].trim().startsWith("```")) { c.appendLine(zeilen[i]); i++ }
                    l += Block.Code(c.toString().trimEnd())
                }
                t.isEmpty() -> absatzFertig()
                t.startsWith("|") -> {
                    absatzFertig()
                    val tab = mutableListOf<List<String>>()
                    while (i < zeilen.size && zeilen[i].trim().startsWith("|")) {
                        val r = zeilen[i].trim()
                        if (!r.matches(Regex("^\\|?(\\s*:?-{2,}:?\\s*\\|)+\\s*:?-*:?\\s*\\|?$"))) tab += r.trim('|').split("|").map { it.trim() }
                        i++
                    }
                    i--
                    if (tab.isNotEmpty()) l += Block.Tabelle(tab)
                }
                t.startsWith(">") -> {
                    absatzFertig()
                    val k = StringBuilder()
                    while (i < zeilen.size && zeilen[i].trim().startsWith(">")) { k.append(zeilen[i].trim().drop(1).trim()).append('\n'); i++ }
                    i--
                    l += Block.Kasten(k.toString().trim())
                }
                t.matches(Regex("^(-{3,}|\\*{3,}|_{3,})$")) -> { absatzFertig(); l += Block.Linie }
                t.startsWith("#") -> { absatzFertig(); val s = t.takeWhile { it == '#' }.length; l += Block.Ueberschrift(s.coerceIn(1, 4), t.drop(s).trim()) }
                BILD_ZEILE.matches(t) -> {
                    absatzFertig()
                    val m = BILD_ZEILE.find(t)!!
                    l += Block.Bild(m.groupValues[2].trim().substringAfterLast('/').lowercase(), m.groupValues[1])
                }
                Regex("^[-*+•]\\s+.+").matches(t) -> { absatzFertig(); l += Block.Punkt(t.drop(1).trim(), einzug.coerceAtMost(3), null) }
                Regex("^\\d{1,3}[.)]\\s+.+").matches(t) -> { absatzFertig(); val n = t.takeWhile { it.isDigit() }; l += Block.Punkt(t.drop(n.length + 1).trim(), einzug.coerceAtMost(3), "$n.") }
                else -> { if (absatz.isNotEmpty()) absatz.append(' '); absatz.append(t) }
            }
            i++
        }
        absatzFertig()
        return l
    }

    private fun ohneZeichen(t: String) = t.replace("**", "").replace("__", "").replace("`", "").trim()

    /** „Recherche – Thema … – 18.10. 19.30“ → „Thema …“. */
    private fun aufgeraeumt(titel: String): String {
        val teile = titel.split(" – ")
        val ohneZeit = if (teile.size > 1 && teile.last().matches(Regex("\\d{1,2}\\.\\d{1,2}\\. \\d{1,2}\\.\\d{2}"))) teile.dropLast(1) else teile
        return (if (ohneZeit.size > 1) ohneZeit.drop(1) else ohneZeit).joinToString(" – ").trim().ifEmpty { titel }
    }

    /** **fett**, *kursiv*, `Code` und [Text](Adresse) als echte Auszeichnung; die Adresse bleibt klein dahinter lesbar. */
    private fun inline(text: String): CharSequence {
        val sb = SpannableStringBuilder()
        var i = 0
        INLINE.findAll(text).forEach { m ->
            sb.append(text, i, m.range.first)
            val start = sb.length
            val g = m.groupValues
            when {
                g[1].isNotEmpty() || g[2].isNotEmpty() -> { sb.append(g[1].ifEmpty { g[2] }); sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
                g[3].isNotEmpty() -> {
                    sb.append(g[3])
                    sb.setSpan(TypefaceSpan("monospace"), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(BackgroundColorSpan(CODE), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                g[4].isNotEmpty() -> {
                    sb.append(g[4]); sb.setSpan(ForegroundColorSpan(PETROL), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    val a = sb.length
                    sb.append(" (").append(g[5]).append(")")
                    sb.setSpan(ForegroundColorSpan(LEISE), a, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(RelativeSizeSpan(0.82f), a, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                g[6].isNotEmpty() -> { sb.append(g[6]); sb.setSpan(StyleSpan(Typeface.ITALIC), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
            }
            i = m.range.last + 1
        }
        sb.append(text, i, text.length)
        return sb
    }

    // ---- Seiten ----

    private class Satz(val doc: PdfDocument, val kopfTitel: String) {
        var nr = 0
        var seite: PdfDocument.Page? = null
        var y = 0f
        val canvas: Canvas get() = seite!!.canvas
        val obenAufSeite: Boolean get() = y <= OBEN + 0.5f

        fun neueSeite(mitRahmen: Boolean = true) {
            schliesse()
            nr++
            seite = doc.startPage(PdfDocument.PageInfo.Builder(B.toInt(), H.toInt(), nr).create())
            canvas.drawColor(WEISS)
            if (mitRahmen) rahmen()
            y = OBEN
        }

        fun schliesse() {
            seite?.let { s -> if (nr > 1) fusszeile(s.canvas); doc.finishPage(s) }
            seite = null
        }

        /** Mindestens [h] Platz, sonst neue Seite. */
        fun sicher(h: Float) { if (seite == null || y + h > H - UNTEN) neueSeite() }

        private fun rahmen() {
            // Dezenter Hintergrund: zwei sehr helle Kreise oben rechts und ein schmaler Farbstreifen am linken Rand.
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.color = PETROL; p.alpha = 14
            canvas.drawCircle(B + 10f, -10f, 120f, p)
            p.color = NAVY; p.alpha = 10
            canvas.drawCircle(B - 70f, 20f, 50f, p)
            p.color = PETROL; p.alpha = 255
            canvas.drawRect(0f, OBEN - 26f, 5f, H - UNTEN + 26f, p.apply { alpha = 40 })
            val kopf = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = LEISE; typeface = SANS }
            val titel = TextUtils.kuerze(kopfTitel, kopf, BREITE - 60f)
            canvas.drawText(titel, RAND, 40f, kopf)
            kopf.color = PETROL; kopf.typeface = SANS_MITTEL
            canvas.drawText("JARVIS", B - RAND - kopf.measureText("JARVIS"), 40f, kopf)
            canvas.drawRect(RAND, 47f, B - RAND, 47.6f, Paint().apply { color = LINIE })
        }

        private fun fusszeile(c: Canvas) {
            val f = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = LEISE; typeface = SANS }
            val text = "Seite $nr"
            c.drawRect(RAND, H - 36f, RAND + 24f, H - 34.5f, Paint().apply { color = PETROL })
            c.drawText(text, B - RAND - f.measureText(text), H - 32f, f)
        }
    }

    private object TextUtils {
        fun kuerze(t: String, p: Paint, max: Float): String {
            if (p.measureText(t) <= max) return t
            var s = t
            while (s.isNotEmpty() && p.measureText("$s…") > max) s = s.dropLast(1)
            return "$s…"
        }
    }

    private fun layout(text: CharSequence, paint: TextPaint, breite: Int, zeilenAbstand: Float = 1.32f): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, breite.coerceAtLeast(10))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, zeilenAbstand).setIncludePad(false).build()

    private fun farbe(f: Int, groesse: Float, schrift: Typeface = SANS) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = f; textSize = groesse; typeface = schrift }

    /**
     * Setzt Text ab der aktuellen Höhe und bricht ihn sauber über Seitenenden. Jeder Seitenteil wird als eigenes Layout
     * gezeichnet (kein abgeschnittener Text im PDF, der sonst doppelt in Suche und Auswahl auftauchte). [hintergrund]
     * zeichnet je Teil einen Kasten (oben, unten); [ersteZeile] bekommt die Grundlinie der ersten Zeile (für Aufzählungszeichen).
     */
    private fun fliesstext(
        satz: Satz, text: CharSequence, paint: TextPaint, x: Float, breite: Int, polster: Float = 0f,
        zeilenAbstand: Float = 1.32f, hintergrund: ((Float, Float) -> Unit)? = null, ersteZeile: ((Float) -> Unit)? = null,
    ) {
        var rest = text
        var erste = true
        while (rest.isNotEmpty()) {
            val l = layout(rest, paint, breite, zeilenAbstand)
            val ersteHoehe = (l.getLineBottom(0) - l.getLineTop(0)).toFloat()
            satz.sicher(ersteHoehe + 2 * polster)
            val frei = H - UNTEN - satz.y - 2 * polster
            var n = 0
            while (n < l.lineCount && l.getLineBottom(n) <= frei) n++
            if (n == 0) n = 1
            val ende = l.getLineEnd(n - 1)
            val teil = if (n == l.lineCount) l else layout(rest.subSequence(0, ende), paint, breite, zeilenAbstand)
            val h = teil.height.toFloat()
            hintergrund?.invoke(satz.y, satz.y + h + 2 * polster)
            if (erste) ersteZeile?.invoke(satz.y + polster + teil.getLineBaseline(0))
            satz.canvas.save()
            satz.canvas.translate(x, satz.y + polster)
            teil.draw(satz.canvas)
            satz.canvas.restore()
            satz.y += h + 2 * polster
            erste = false
            if (n == l.lineCount) break
            rest = rest.subSequence(ende, rest.length)
            satz.neueSeite()
        }
    }

    private fun titelseite(satz: Satz, titel: String, untertitel: String?, bild: File?, kapitel: List<String>) {
        satz.neueSeite(mitRahmen = false)
        val c = satz.canvas
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val flaeche = 430f
        val bmp: Bitmap? = bild?.let { Medien.bildVerkleinert(it, 2000) }
        if (bmp != null) {
            try {
                // Bildschirmfüllend oben, mittig zugeschnitten.
                val f = maxOf(B / bmp.width, flaeche / bmp.height)
                val w = B / f
                val h = flaeche / f
                val quelle = Rect(((bmp.width - w) / 2).toInt(), ((bmp.height - h) / 2).toInt(), ((bmp.width + w) / 2).toInt(), ((bmp.height + h) / 2).toInt())
                c.drawBitmap(bmp, quelle, RectF(0f, 0f, B, flaeche), p)
            } finally {
                bmp.recycle()
            }
            p.shader = LinearGradient(0f, flaeche * 0.35f, 0f, flaeche, 0x001B3A5C, 0xE61B3A5C.toInt(), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, B, flaeche, p)
            p.shader = null
        } else {
            p.color = NAVY
            c.drawRect(0f, 0f, B, flaeche, p)
            p.color = PETROL; p.alpha = 110
            c.drawCircle(B - 40f, 70f, 170f, p)
            p.color = WEISS; p.alpha = 22
            c.drawCircle(B - 190f, 300f, 80f, p)
            p.alpha = 14
            c.drawCircle(60f, 60f, 120f, p)
            p.alpha = 255
        }
        p.color = PETROL
        c.drawRect(0f, flaeche, B, flaeche + 6f, p)

        // Titel unten auf der Fläche, weiß.
        val kopf = farbe(WEISS, if (titel.length > 60) 22f else 27f, SERIF_FETT)
        val tl = layout(titel, kopf, BREITE, 1.12f)
        val unter = farbe(WEISS, 10.5f, SANS_MITTEL).apply { alpha = 215 }
        val datum = LocalDate.now().format(DateTimeFormatter.ofPattern("d. MMMM yyyy", java.util.Locale.GERMAN))
        val zeile2 = listOfNotNull(untertitel?.takeIf { it.isNotBlank() }, datum).joinToString("  ·  ")
        val ul = layout(zeile2, unter, BREITE)
        val oben = flaeche - 34f - ul.height - 12f - tl.height
        c.save(); c.translate(RAND, oben); tl.draw(c); c.restore()
        c.drawRect(RAND, oben + tl.height + 6f, RAND + 36f, oben + tl.height + 8.5f, Paint().apply { color = 0xFF7FD1D3.toInt() })
        c.save(); c.translate(RAND, oben + tl.height + 14f); ul.draw(c); c.restore()

        // Inhaltsübersicht unter der Fläche.
        var y = flaeche + 44f
        val label = farbe(PETROL, 9f, SANS_FETT).apply { letterSpacing = 0.18f }
        c.drawText("INHALT", RAND, y, label)
        y += 18f
        val eintrag = farbe(TEXT, 11f, SANS)
        val nummer = farbe(PETROL, 11f, SANS_FETT)
        val gezeigt = kapitel.take(12)
        gezeigt.forEachIndexed { i, k ->
            if (y > H - 90f) return@forEachIndexed
            val n = String.format("%02d", i + 1)
            c.drawText(n, RAND, y, nummer)
            c.drawText(TextUtils.kuerze(k, eintrag, BREITE - 34f), RAND + 30f, y, eintrag)
            c.drawRect(RAND + 30f, y + 7f, B - RAND, y + 7.5f, Paint().apply { color = LINIE })
            y += 24f
        }
        if (kapitel.size > gezeigt.size) c.drawText("… und ${kapitel.size - gezeigt.size} weitere", RAND + 30f, y, farbe(LEISE, 10f))
        val fuss = farbe(LEISE, 8.5f)
        c.drawText("Erstellt von Jarvis", RAND, H - 40f, fuss)
        satz.y = H
    }

    private fun ueberschrift(satz: Satz, b: Block.Ueberschrift, amAnfang: Boolean) {
        val (paint, vor, nach) = when (b.stufe) {
            1 -> Triple(farbe(NAVY, 19f, SERIF_FETT), 22f, 10f)
            2 -> Triple(farbe(NAVY, 15f, SANS_FETT), 18f, 7f)
            3 -> Triple(farbe(PETROL, 12f, SANS_FETT), 12f, 4f)
            else -> Triple(farbe(TEXT, 10.5f, SANS_FETT), 8f, 3f)
        }
        val text = inline(b.text)
        val l = layout(text, paint, BREITE, 1.15f)
        // Keine Überschrift allein am Seitenende: sie braucht Platz für sich und gut drei Zeilen danach.
        if (!satz.obenAufSeite && !amAnfang) satz.y += vor
        satz.sicher(l.height + 8f + 48f)
        if (b.stufe <= 2) {
            val farbe = Paint().apply { color = PETROL }
            satz.canvas.drawRect(RAND, satz.y, RAND + 28f, satz.y + 3f, farbe)
            satz.y += 9f
        }
        fliesstext(satz, text, paint, RAND, BREITE, zeilenAbstand = 1.15f)
        satz.y += nach
    }

    private fun absatz(satz: Satz, text: String) {
        fliesstext(satz, inline(text), farbe(TEXT, 10.5f), RAND, BREITE)
        satz.y += 7f
    }

    private fun punkt(satz: Satz, b: Block.Punkt, erster: Boolean) {
        if (erster) satz.y += 1f
        val einzug = RAND + 4f + b.ebene * 14f
        val textX = einzug + if (b.nummer != null) 18f else 13f
        val paint = farbe(TEXT, 10.5f)
        fliesstext(satz, inline(b.text), paint, textX, (B - RAND - textX).toInt(), ersteZeile = { grund ->
            if (b.nummer != null) satz.canvas.drawText(b.nummer, einzug, grund, farbe(PETROL, 10.5f, SANS_FETT))
            else satz.canvas.drawCircle(einzug + 3f, grund - 3.6f, if (b.ebene == 0) 2.3f else 1.8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (b.ebene == 0) PETROL else LEISE })
        })
        satz.y += 4f
    }

    private fun kasten(satz: Satz, text: String, grund: Int, balken: Int) {
        satz.y += 3f
        val polster = 11f
        val x = RAND + 14f
        fliesstext(satz, inline(text), farbe(TEXT, 10.5f), x, (B - RAND - 10f - x).toInt(), polster, hintergrund = { oben, unten ->
            satz.canvas.drawRoundRect(RectF(RAND, oben, B - RAND, unten), 5f, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = grund })
            satz.canvas.drawRect(RAND, oben, RAND + 3.5f, unten, Paint().apply { color = balken })
        })
        satz.y += 10f
    }

    private fun code(satz: Satz, text: String) {
        val polster = 8f
        fliesstext(satz, text, farbe(TEXT, 8.8f, Typeface.MONOSPACE), RAND + 10f, BREITE - 20, polster, 1.2f, hintergrund = { oben, unten ->
            satz.canvas.drawRoundRect(RectF(RAND, oben, B - RAND, unten), 4f, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CODE })
        })
        satz.y += 8f
    }

    private fun trennlinie(satz: Satz) {
        satz.sicher(16f)
        satz.y += 6f
        satz.canvas.drawRect(RAND, satz.y, B - RAND, satz.y + 0.6f, Paint().apply { color = LINIE })
        satz.y += 10f
    }

    private fun tabelle(satz: Satz, roh: List<List<String>>) {
        val spalten = roh.maxOf { it.size }
        val zeilen = roh.map { r -> List(spalten) { r.getOrElse(it) { "" } } }
        // Spaltenbreite nach Inhalt, gedeckelt, damit eine lange Spalte die anderen nicht erdrückt.
        val gewicht = (0 until spalten).map { s -> zeilen.maxOf { it[s].length }.coerceIn(4, 38) + 4f }
        val summe = gewicht.sum()
        val breiten = gewicht.map { it / summe * BREITE }
        val polster = 6f
        val kopf = farbe(WEISS, 9f, SANS_FETT)
        val zelle = farbe(TEXT, 9f)
        fun layouts(r: List<String>, p: TextPaint) = r.mapIndexed { i, t -> layout(inline(t), p, (breiten[i] - 2 * polster).toInt(), 1.2f) }
        fun zeile(ls: List<StaticLayout>, hintergrund: Int?, kopfzeile: Boolean) {
            val h = ls.maxOf { it.height } + 2 * polster
            if (satz.y + h > H - UNTEN) {
                satz.neueSeite()
                if (!kopfzeile) zeile(layouts(zeilen.first(), kopf), NAVY, true)
            }
            hintergrund?.let { satz.canvas.drawRect(RAND, satz.y, B - RAND, satz.y + h, Paint().apply { color = it }) }
            var x = RAND
            ls.forEachIndexed { i, l ->
                satz.canvas.save(); satz.canvas.translate(x + polster, satz.y + polster); l.draw(satz.canvas); satz.canvas.restore()
                x += breiten[i]
            }
            satz.y += h
            satz.canvas.drawRect(RAND, satz.y - 0.5f, B - RAND, satz.y, Paint().apply { color = LINIE })
        }
        satz.y += 4f
        satz.sicher(60f)
        zeile(layouts(zeilen.first(), kopf), NAVY, true)
        zeilen.drop(1).forEachIndexed { i, r -> zeile(layouts(r, zelle), if (i % 2 == 1) ZEILE else null, false) }
        satz.y += 12f
    }

    private fun abbildung(satz: Satz, datei: File, unterschrift: String, nummer: Int) {
        val bmp = Medien.bildVerkleinert(datei, 1800) ?: return
        try {
            val maxH = (H - OBEN - UNTEN) * 0.52f
            val f = minOf(BREITE.toFloat() / bmp.width, maxH / bmp.height)
            val w = bmp.width * f
            val h = bmp.height * f
            val text = SpannableStringBuilder("Abbildung $nummer").apply {
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(ForegroundColorSpan(PETROL), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val u = ohneZeichen(unterschrift)
                if (u.isNotBlank()) append("  ").append(u)
            }
            val ul = layout(text, farbe(LEISE, 8.8f, Typeface.create(SANS, Typeface.ITALIC)), BREITE, 1.2f)
            satz.y += 6f
            satz.sicher(h + ul.height + 14f)
            val links = RAND + (BREITE - w) / 2
            val ziel = RectF(links, satz.y, links + w, satz.y + h)
            val c = satz.canvas
            c.save()
            c.clipPath(Path().apply { addRoundRect(ziel, 6f, 6f, Path.Direction.CW) })
            c.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), ziel, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            c.restore()
            c.drawRoundRect(ziel, 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.7f; color = LINIE })
            satz.y += h + 7f
            c.save(); c.translate(RAND, satz.y); ul.draw(c); c.restore()
            satz.y += ul.height + 12f
        } finally {
            bmp.recycle()
        }
    }
}
