package de.frank.gedankenspeicher.ui.verlauf

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import de.frank.gedankenspeicher.tts.Absaetze
import de.frank.gedankenspeicher.ui.theme.Dauern
import de.frank.gedankenspeicher.ui.theme.Farben
import de.frank.gedankenspeicher.ui.theme.Farbrollen
import de.frank.gedankenspeicher.ui.theme.Kurven
import de.frank.gedankenspeicher.ui.theme.Schriften
import de.frank.gedankenspeicher.ui.theme.dauer

/**
 * **Die Auswertung, so wie die KI sie gebaut hat.**
 *
 * Bis Fassung 0.5.11 war eine Auswertung reiner Fließtext: was die KI an Tabellen oder
 * Aufzählungen schickte, stand als roher Strichsalat auf der Karte. Seitdem hat die
 * Auswertungs-KI freie Hand — Überschriften, Listen, Zitate, Trennlinien, Tabellen,
 * gezeichnete Infografiken als SVG, ganze HTML-Bausteine, in beliebiger Reihenfolge
 * gemischt. Dieser Bauplan zerlegt ihren Text in [Baustein]e und zeichnet jeden so, wie er
 * gemeint war.
 *
 * Das Vorlesen (F-06) bleibt daran gebunden: [Reichtext.vorlesetext] liefert genau die
 * Absätze, die gesprochen werden, in derselben Reihenfolge und in denselben Schnitten, in
 * denen [Absaetze.teile] sie später zerlegt. Deshalb sitzt die Hervorhebung des laufenden
 * Absatzes weiterhin auf dem Block, der gerade gesprochen wird — auch dann, wenn zwischen
 * zwei Absätzen eine Tabelle oder eine Zeichnung steht, die selbst nicht vorgelesen wird.
 */
sealed interface Baustein {
    data class Absatz(val text: String) : Baustein
    data class Ueberschrift(val stufe: Int, val text: String) : Baustein
    data class Punkte(val eintraege: List<String>, val nummeriert: Boolean, val start: Int) : Baustein
    data class Zitat(val text: String) : Baustein
    data class Tabelle(val kopf: List<String>, val zeilen: List<List<String>>) : Baustein

    /** Eine Zeichnung. Das Seitenverhältnis kommt aus der viewBox — sonst wüsste niemand, wie hoch sie wird. */
    data class Grafik(val svg: String, val seitenverhaeltnis: Float) : Baustein

    /** Ein freier HTML-Baustein. Seine Höhe misst erst die Seite selbst, nachdem sie steht. */
    data class Seite(val html: String) : Baustein
    data class Code(val text: String) : Baustein
    data object Trennlinie : Baustein
}

object Reichtext {

    private val trennlinie = Regex("^\\s*([-*_])\\s*\\1\\s*\\1[\\s\\-*_]*$")
    private val ueberschrift = Regex("^(#{1,6})\\s+(.*)$")
    private val punkt = Regex("^\\s*[-*\u2022+]\\s+(.*)$")
    private val nummer = Regex("^\\s*(\\d{1,3})[.)]\\s+(.*)$")
    private val tabellenTrenner = Regex("^\\s*\\|?[\\s:|-]*-[\\s:|-]*\\|?\\s*$")
    private val viewBox = Regex("viewBox\\s*=\\s*[\"']\\s*[-\\d.]+\\s+[-\\d.]+\\s+([\\d.]+)\\s+([\\d.]+)")

    // --- Quellenangaben ---------------------------------------------------------------------

    private val markdownLink = Regex("\\[([^\\]\\n]*)]\\((?:https?://|www\\.)[^)\\s]*\\)")
    private val nackteAdresse = Regex("(?<![\\w\"'(=])(?:https?://|www\\.)\\S+")
    private val klammerRest = Regex("\\(\\s*[,;\u00b7\u2022\\s]*\\)")
    private val fussnote = Regex("\u3010[^\u3011]*\u3011|\\[\\^[^\\]]*]")
    private val quellzeile = Regex("(?im)^\\s*(quellen?|sources?|belege?|referenzen)\\s*:.*$")

    /**
     * **Streicht jede Quellenangabe.**
     *
     * Der Auftrag verbietet sie ausdrücklich — aber ein Modell, das gerade im Web gesucht
     * hat, hängt sie trotzdem an. Deshalb wird hier nachgeräumt: aus einem Link bleibt sein
     * Text, eine nackte Adresse verschwindet ganz, „Quelle: …"-Zeilen fallen weg. Was in
     * einer Zeichnung steht, bleibt unberührt: dort stehen keine Quellen, wohl aber
     * `url(#…)`-Verweise, die die Grafik zerstörten, nähme man sie weg.
     */
    fun ohneQuellen(text: String): String {
        val teile = zerlegeAnGrafiken(text)
        return teile.joinToString("") { (istGrafik, stueck) ->
            if (istGrafik) stueck else saeubere(stueck)
        }.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun saeubere(text: String): String = text
        .replace(fussnote, "")
        .replace(markdownLink) { it.groupValues[1] }
        .replace(nackteAdresse, "")
        .replace(quellzeile, "")
        .replace(klammerRest, "")
        .replace(Regex("[ \t]{2,}"), " ")
        .replace(Regex("[ \t]+([.,;:!?])"), "$1")

    /** Teilt den Text in abwechselnd „außerhalb einer Zeichnung" und „Zeichnung". */
    private fun zerlegeAnGrafiken(text: String): List<Pair<Boolean, String>> {
        val ergebnis = mutableListOf<Pair<Boolean, String>>()
        var rest = text
        while (true) {
            val start = rest.indexOf("<svg", ignoreCase = true)
            if (start < 0) break
            val ende = rest.indexOf("</svg>", start, ignoreCase = true)
            if (ende < 0) break
            ergebnis += false to rest.substring(0, start)
            ergebnis += true to rest.substring(start, ende + 6)
            rest = rest.substring(ende + 6)
        }
        ergebnis += false to rest
        return ergebnis
    }

    // --- Zerlegen ---------------------------------------------------------------------------

    /** Zerlegt den Text der Auswertung in seine Bausteine. */
    fun zerlege(roh: String): List<Baustein> {
        val zeilen = roh.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        val bausteine = mutableListOf<Baustein>()
        var i = 0

        while (i < zeilen.size) {
            val zeile = zeilen[i]
            val kurz = zeile.trim()

            when {
                kurz.isEmpty() -> i++

                kurz.startsWith("```") -> {
                    val sprache = kurz.removePrefix("```").trim().lowercase()
                    val inhalt = StringBuilder()
                    i++
                    while (i < zeilen.size && !zeilen[i].trim().startsWith("```")) {
                        inhalt.appendLine(zeilen[i])
                        i++
                    }
                    if (i < zeilen.size) i++
                    val text = inhalt.toString().trim()
                    if (text.isNotEmpty()) bausteine += umschlossenerBlock(sprache, text)
                }

                kurz.startsWith("<svg", ignoreCase = true) -> {
                    val inhalt = StringBuilder()
                    while (i < zeilen.size) {
                        inhalt.appendLine(zeilen[i])
                        val fertig = zeilen[i].contains("</svg>", ignoreCase = true)
                        i++
                        if (fertig) break
                    }
                    bausteine += alsGrafik(inhalt.toString().trim())
                }

                trennlinie.matches(kurz) -> {
                    bausteine += Baustein.Trennlinie
                    i++
                }

                ueberschrift.matches(kurz) -> {
                    val treffer = ueberschrift.find(kurz)!!
                    val text = treffer.groupValues[2].trim().trimEnd('#').trim()
                    if (text.isNotEmpty()) {
                        bausteine += Baustein.Ueberschrift(treffer.groupValues[1].length, text)
                    }
                    i++
                }

                kurz.startsWith(">") -> {
                    val sammlung = mutableListOf<String>()
                    while (i < zeilen.size && zeilen[i].trim().startsWith(">")) {
                        sammlung += zeilen[i].trim().removePrefix(">").trim()
                        i++
                    }
                    bausteine += Baustein.Zitat(sammlung.joinToString(" ").trim())
                }

                istTabellenkopf(zeilen, i) -> {
                    val kopf = zellen(zeilen[i])
                    i += 2
                    val reihen = mutableListOf<List<String>>()
                    while (i < zeilen.size && zeilen[i].contains('|') && zeilen[i].isNotBlank()) {
                        reihen += zellen(zeilen[i])
                        i++
                    }
                    bausteine += Baustein.Tabelle(kopf, reihen.map { angleichen(it, kopf.size) })
                }

                punkt.matches(zeile) || nummer.matches(zeile) -> {
                    val nummeriert = nummer.matches(zeile)
                    val start = if (nummeriert) nummer.find(zeile)!!.groupValues[1].toIntOrNull() ?: 1 else 1
                    val eintraege = mutableListOf<String>()
                    while (i < zeilen.size) {
                        val z = zeilen[i]
                        val alsPunkt = punkt.find(z)
                        val alsNummer = nummer.find(z)
                        val passt = if (nummeriert) alsNummer != null else alsPunkt != null
                        if (!passt) break
                        eintraege += (if (nummeriert) alsNummer!!.groupValues[2] else alsPunkt!!.groupValues[1]).trim()
                        i++
                        // Eine eingerückte Fortsetzungszeile gehört zum Eintrag davor.
                        while (i < zeilen.size && zeilen[i].startsWith("  ") && zeilen[i].isNotBlank() &&
                            punkt.find(zeilen[i]) == null && nummer.find(zeilen[i]) == null
                        ) {
                            eintraege[eintraege.lastIndex] = eintraege.last() + " " + zeilen[i].trim()
                            i++
                        }
                    }
                    bausteine += Baustein.Punkte(eintraege.filter { it.isNotBlank() }, nummeriert, start)
                }

                else -> {
                    val sammlung = mutableListOf<String>()
                    var erste = true
                    while (i < zeilen.size && zeilen[i].isNotBlank() && (erste || !beginntNeuenBlock(zeilen, i))) {
                        sammlung += zeilen[i].trim()
                        erste = false
                        i++
                    }
                    val text = sammlung.joinToString(" ").trim()
                    if (text.isNotEmpty()) bausteine += Baustein.Absatz(text)
                }
            }
        }
        return bausteine
    }

    private fun umschlossenerBlock(sprache: String, text: String): Baustein = when {
        sprache == "svg" || text.trimStart().startsWith("<svg", ignoreCase = true) -> alsGrafik(text)
        sprache == "html" -> Baustein.Seite(text)
        else -> Baustein.Code(text)
    }

    private fun alsGrafik(svg: String): Baustein {
        val treffer = viewBox.find(svg)
        val breite = treffer?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val hoehe = treffer?.groupValues?.get(2)?.toFloatOrNull() ?: 0f
        // Ohne brauchbare viewBox bleibt nur ein ruhiges Standardmaß — sonst wäre die
        // Zeichnung entweder null Pixel hoch oder unendlich lang.
        val verhaeltnis = if (breite > 0f && hoehe > 0f) breite / hoehe else 16f / 9f
        return Baustein.Grafik(svg, verhaeltnis.coerceIn(0.35f, 4f))
    }

    private fun beginntNeuenBlock(zeilen: List<String>, i: Int): Boolean {
        val kurz = zeilen[i].trim()
        return kurz.startsWith("```") ||
            kurz.startsWith("<svg", ignoreCase = true) ||
            kurz.startsWith(">") ||
            trennlinie.matches(kurz) ||
            ueberschrift.matches(kurz) ||
            punkt.matches(zeilen[i]) ||
            nummer.matches(zeilen[i]) ||
            istTabellenkopf(zeilen, i)
    }

    private fun istTabellenkopf(zeilen: List<String>, i: Int): Boolean =
        zeilen[i].contains('|') &&
            i + 1 < zeilen.size &&
            zeilen[i + 1].contains('-') &&
            zeilen[i + 1].contains('|') &&
            tabellenTrenner.matches(zeilen[i + 1])

    private fun zellen(zeile: String): List<String> =
        zeile.trim().removePrefix("|").removeSuffix("|").split('|').map { it.trim() }

    private fun angleichen(zeile: List<String>, spalten: Int): List<String> =
        if (zeile.size >= spalten) zeile.take(spalten) else zeile + List(spalten - zeile.size) { "" }

    // --- Vorlesen ---------------------------------------------------------------------------

    /**
     * **Der Text, der wirklich gesprochen wird — der Fließtext und sonst nichts.**
     *
     * Vorgelesen wird nur, was als zusammenhängender Text geschrieben ist: Absätze und
     * Zitatblöcke. Überschriften, Aufzählungen, Tabellen, Zeichnungen, Codeblöcke und
     * Trennlinien schweigen. Sie sind zum Ansehen gemacht, nicht zum Anhören — eine
     * vorgelesene Tabelle ist eine Kette aus Wortfetzen, und eine dazwischengesprochene
     * Überschrift reißt den Faden auseinander, statt ihn zu ordnen.
     */
    fun vorlesetext(roh: String): String =
        zerlege(roh).mapNotNull(::gesprochen).joinToString("\n\n")

    /** Was ein Baustein beiträgt, wenn er vorgelesen wird. Null heißt: er schweigt. */
    private fun gesprochen(baustein: Baustein): String? = when (baustein) {
        is Baustein.Absatz -> ohneZeichen(baustein.text)
        is Baustein.Zitat -> ohneZeichen(baustein.text)

        is Baustein.Ueberschrift,
        is Baustein.Punkte,
        is Baustein.Tabelle,
        is Baustein.Grafik,
        is Baustein.Seite,
        is Baustein.Code,
        Baustein.Trennlinie,
        -> null
    }?.takeIf { it.isNotBlank() }

    /** Nimmt die Auszeichnungszeichen weg — vorgelesen würden sie sonst mitgesprochen. */
    fun ohneZeichen(text: String): String = text
        .replace(Regex("\\*\\*|__|~~|`|\\*"), "")
        .replace(Regex("(?<!\\w)_|_(?!\\w)"), "")
        .replace(Regex("\\s{2,}"), " ")
        .trim()

    /**
     * Wie viele Vorlese-Absätze ein Baustein belegt. Ein sehr langer Absatz wird von
     * [Absaetze.teile] noch einmal geteilt — dann sind es mehrere.
     */
    fun vorleseAnzahl(baustein: Baustein): Int =
        gesprochen(baustein)?.let { Absaetze.teile(it).size } ?: 0
}

/**
 * Zeichnet die Bausteine untereinander. [hervorgehobenerAbsatz] ist die Nummer des
 * Absatzes, der gerade vorgelesen wird — gezählt über [Reichtext.vorleseAnzahl], damit die
 * Hervorhebung auch dann sitzt, wenn Tabellen und Zeichnungen dazwischenstehen.
 */
@Composable
fun ReichtextAnsicht(
    text: String,
    hervorgehobenerAbsatz: Int,
    stil: TextStyle = Schriften.kiAntworttext,
    modifier: Modifier = Modifier,
) {
    val bausteine = remember(text) { Reichtext.zerlege(text) }
    if (bausteine.isEmpty()) return

    // Von welcher Absatznummer an ein Baustein spricht — einmal ausgerechnet, nicht bei
    // jedem Wechsel des vorgelesenen Absatzes neu.
    val grenzen = remember(bausteine) {
        var laufend = 0
        bausteine.map { baustein ->
            val anzahl = Reichtext.vorleseAnzahl(baustein)
            val von = laufend
            laufend += anzahl
            von until (von + anzahl)
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        bausteine.forEachIndexed { nr, baustein ->
            BausteinAnsicht(
                baustein = baustein,
                hervor = hervorgehobenerAbsatz in grenzen[nr],
                stil = stil,
            )
        }
    }
}

@Composable
private fun BausteinAnsicht(baustein: Baustein, hervor: Boolean, stil: TextStyle) {
    val farben = Farben
    val schrift = Schriften
    val staerke by animateFloatAsState(
        targetValue = if (hervor) 1f else 0f,
        animationSpec = tween(dauer(Dauern.STANDARD), easing = Kurven.standard),
        label = "baustein",
    )
    val hervorhebung = Modifier
        .fillMaxWidth()
        .background(
            farben.akzentGedeckt.copy(alpha = farben.akzentGedeckt.alpha * staerke),
            RoundedCornerShape(8.dp),
        )
        .padding(
            horizontal = if (staerke > 0f) 6.dp else 0.dp,
            vertical = if (staerke > 0f) 4.dp else 0.dp,
        )

    when (baustein) {
        is Baustein.Absatz -> Text(
            text = inline(baustein.text, farben.akzent),
            style = stil,
            color = farben.textStark,
            modifier = hervorhebung,
        )

        is Baustein.Ueberschrift -> Text(
            text = inline(baustein.text, farben.akzent),
            style = stil.copy(
                fontSize = when (baustein.stufe) {
                    1 -> 21.sp
                    2 -> 18.sp
                    else -> 16.sp
                },
                lineHeight = when (baustein.stufe) {
                    1 -> 28.sp
                    2 -> 24.sp
                    else -> 22.sp
                },
                fontWeight = FontWeight(if (baustein.stufe <= 2) 700 else 600),
            ),
            color = if (baustein.stufe == 1) farben.akzent else farben.textStark,
            modifier = hervorhebung,
        )

        is Baustein.Punkte -> Column(
            modifier = hervorhebung,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            baustein.eintraege.forEachIndexed { nr, eintrag ->
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        text = if (baustein.nummeriert) "${baustein.start + nr}." else "\u2022",
                        style = stil,
                        color = farben.akzent,
                        modifier = Modifier.width(if (baustein.nummeriert) 28.dp else 18.dp),
                    )
                    Text(inline(eintrag, farben.akzent), style = stil, color = farben.textStark)
                }
            }
        }

        is Baustein.Zitat -> Row(modifier = hervorhebung) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(with(LocalDensity.current) { stil.lineHeight.toDp() } * 1.2f)
                    .background(farben.akzent, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                inline(baustein.text, farben.akzent),
                style = stil.copy(fontStyle = FontStyle.Italic),
                color = farben.textMittel,
            )
        }

        Baustein.Trennlinie -> Box(Modifier.fillMaxWidth().height(1.dp).background(farben.rand))

        is Baustein.Tabelle -> Tabellenansicht(baustein, hervorhebung)

        is Baustein.Code -> Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(farben.hintergrundErhoben)
                .border(1.dp, farben.rand, RoundedCornerShape(12.dp))
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            Text(
                baustein.text,
                style = schrift.einstellungErklaerung.copy(fontFamily = FontFamily.Monospace),
                color = farben.textMittel,
            )
        }

        is Baustein.Grafik -> Zeichnung(baustein.svg, baustein.seitenverhaeltnis)

        is Baustein.Seite -> Freiseite(baustein.html)
    }
}

@Composable
private fun Tabellenansicht(tabelle: Baustein.Tabelle, hervorhebung: Modifier) {
    val farben = Farben
    val schrift = Schriften
    val breite = spaltenbreite(tabelle)
    // Breite Tabellen laufen nicht über den Rand, sondern werden seitlich geschoben —
    // ein Umbruch mitten in einer Zahlenspalte wäre unlesbar.
    Box(hervorhebung.horizontalScroll(rememberScrollState())) {
        Column(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, farben.rand, RoundedCornerShape(12.dp)),
        ) {
            Row(Modifier.background(farben.akzentGedeckt)) {
                tabelle.kopf.forEach { zelle ->
                    Text(
                        text = Reichtext.ohneZeichen(zelle),
                        style = schrift.kartenUeberschrift,
                        color = farben.akzent,
                        modifier = Modifier.width(breite).padding(10.dp),
                    )
                }
            }
            tabelle.zeilen.forEachIndexed { nr, zeile ->
                Box(Modifier.fillMaxWidth().height(1.dp).background(farben.rand))
                Row(
                    Modifier.background(
                        if (nr % 2 == 1) farben.hintergrundErhoben.copy(alpha = 0.5f) else Color.Transparent,
                    ),
                ) {
                    zeile.forEach { zelle ->
                        Text(
                            text = inline(zelle, farben.akzent),
                            style = schrift.notiztext.copy(fontSize = 14.sp, lineHeight = 20.sp),
                            color = farben.textStark,
                            modifier = Modifier.width(breite).padding(10.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Je mehr Spalten, desto schmaler — aber nie so schmal, dass ein Wort nicht mehr passt. */
private fun spaltenbreite(tabelle: Baustein.Tabelle) = when (tabelle.kopf.size) {
    1 -> 300.dp
    2 -> 170.dp
    3 -> 130.dp
    else -> 110.dp
}

// --- Zeichnungen und freie Seiten ------------------------------------------------------------

/**
 * **Eine Zeichnung der KI.**
 *
 * Sie liegt als SVG vor und wird in einer winzigen, abgeschotteten Seite gezeigt: kein
 * Zugriff auf Dateien, kein Nachladen von außen. Die Höhe steht vorher fest — sie kommt aus
 * der viewBox —, deshalb braucht es kein Nachmessen und die Karte springt beim Aufbauen nicht.
 */
@Composable
private fun Zeichnung(svg: String, seitenverhaeltnis: Float) {
    val farben = Farben
    val seite = remember(svg, farben.akzent, farben.textStark) { rahmeSvg(svg, farben) }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(seitenverhaeltnis)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        AbgeschotteteSeite(seite, Modifier.fillMaxWidth())
    }
}

/** Ein freier HTML-Baustein. Seine Höhe kennt erst die Seite selbst — sie meldet sie zurück. */
@Composable
private fun Freiseite(html: String) {
    val farben = Farben
    val seite = remember(html, farben.akzent, farben.textStark) { rahmeHtml(html, farben) }
    var hoehe by remember(seite) { mutableFloatStateOf(0f) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (hoehe > 0f) hoehe.dp else 1.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        AbgeschotteteSeite(seite, Modifier.fillMaxWidth(), beiHoehe = { hoehe = it })
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
private fun AbgeschotteteSeite(
    html: String,
    modifier: Modifier = Modifier,
    beiHoehe: ((Float) -> Unit)? = null,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                // Die Karte scrollt, nicht die eingebettete Seite — sonst bliebe der Finger
                // in der Grafik hängen, statt die Auswertung weiterzuschieben.
                setOnTouchListener { _, ereignis ->
                    ereignis.action == android.view.MotionEvent.ACTION_MOVE
                }
                settings.apply {
                    javaScriptEnabled = beiHoehe != null
                    allowFileAccess = false
                    allowContentAccess = false
                    domStorageEnabled = false
                    // Nichts wird nachgeladen: was nicht im Text stand, kommt auch nicht ins Bild.
                    blockNetworkLoads = true
                    blockNetworkImage = true
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse = WebResourceResponse("text/plain", "utf-8", null)

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean = true

                    override fun onPageFinished(view: WebView, url: String?) {
                        val melde = beiHoehe ?: return
                        view.evaluateJavascript("document.body.scrollHeight") { wert ->
                            wert.trim('"').toFloatOrNull()?.let { melde(it.coerceIn(24f, 2000f)) }
                        }
                    }
                }
            }
        },
        update = { it.loadDataWithBaseURL(null, html, "text/html", "utf-8", null) },
    )
}

private fun hex(farbe: Color): String {
    val wert = farbe.toArgb()
    return String.format("#%06X%02X", wert and 0xFFFFFF, (wert ushr 24) and 0xFF)
}

/** Der gemeinsame Kopf: die Farben der App als CSS-Variablen, damit nichts herausfällt. */
private fun stilblatt(farben: Farbrollen): String = """
    :root{
      --akzent:${hex(farben.akzent)};
      --text:${hex(farben.textStark)};
      --schwach:${hex(farben.textSchwach)};
      --mittel:${hex(farben.textMittel)};
      --rand:${hex(farben.rand)};
      --grund:${hex(farben.kiKarte)};
      --gedeckt:${hex(farben.akzentGedeckt)};
    }
    html,body{margin:0;padding:0;background:transparent;color:var(--text);
      font-family:-apple-system,Roboto,sans-serif;font-size:14px;line-height:1.5;
      -webkit-user-select:none;user-select:none;overflow:hidden;}
""".trimIndent()

private fun rahmeSvg(svg: String, farben: Farbrollen): String = """
<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<style>${stilblatt(farben)}
svg{display:block;width:100%;height:100%;}
text{font-family:-apple-system,Roboto,sans-serif;}
</style></head><body>$svg</body></html>
""".trimIndent()

private fun rahmeHtml(html: String, farben: Farbrollen): String = """
<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<style>${stilblatt(farben)}
table{border-collapse:collapse;width:100%;}
th,td{border:1px solid var(--rand);padding:6px 8px;text-align:left;}
th{color:var(--akzent);background:var(--gedeckt);}
svg{max-width:100%;}
a{color:var(--akzent);text-decoration:none;}
</style></head><body>$html</body></html>
""".trimIndent()

// --- Auszeichnungen im Fließtext ---------------------------------------------------------------

/**
 * **fett**, *kursiv*, ~~gestrichen~~ und `Nicht-Text` — mehr braucht ein Absatz nicht.
 *
 * Von Hand gelesen statt mit einem regulären Ausdruck: verschachtelte Auszeichnungen
 * (`**fett und *kursiv* zugleich**`) fielen sonst auseinander.
 */
private fun inline(roh: String, akzent: Color): AnnotatedString = buildAnnotatedString {
    var fett = false
    var kursiv = false
    var gestrichen = false
    var code = false
    var i = 0
    val puffer = StringBuilder()

    fun leere() {
        if (puffer.isEmpty()) return
        withStyle(
            SpanStyle(
                fontWeight = if (fett) FontWeight(700) else null,
                fontStyle = if (kursiv) FontStyle.Italic else null,
                textDecoration = if (gestrichen) TextDecoration.LineThrough else null,
                fontFamily = if (code) FontFamily.Monospace else null,
                color = if (code) akzent else Color.Unspecified,
            ),
        ) { append(puffer.toString()) }
        puffer.clear()
    }

    while (i < roh.length) {
        val rest = roh.length - i
        when {
            roh[i] == '\\' && rest > 1 -> {
                puffer.append(roh[i + 1]); i += 2
            }

            roh.startsWith("**", i) && !code -> {
                leere(); fett = !fett; i += 2
            }

            roh.startsWith("~~", i) && !code -> {
                leere(); gestrichen = !gestrichen; i += 2
            }

            roh[i] == '`' -> {
                leere(); code = !code; i++
            }

            roh[i] == '*' && !code -> {
                leere(); kursiv = !kursiv; i++
            }

            roh[i] == '_' && !code && grenzt(roh, i) -> {
                leere(); kursiv = !kursiv; i++
            }

            else -> {
                puffer.append(roh[i]); i++
            }
        }
    }
    leere()
}

/** Ein Unterstrich mitten im Wort (`datei_name`) zeichnet nichts aus. */
private fun grenzt(text: String, i: Int): Boolean {
    val davor = text.getOrNull(i - 1)
    val danach = text.getOrNull(i + 1)
    return davor?.isLetterOrDigit() != true || danach?.isLetterOrDigit() != true
}
