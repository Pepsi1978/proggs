package de.frank.aufgaben.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.antippen
import de.frank.aufgaben.ui.theme.glas
import de.frank.aufgaben.ui.theme.knopf3d
import java.time.LocalTime
import kotlinx.coroutines.delay

private val STUNDE = 42.dp
private val LUECKE = 30.dp
private val OBEN = 20.dp
private val SPALTE = 54.dp
/** Mindesthöhe einer Terminkarte; muss zu [Zeitband] BLOCK_MIN passen (28 dp bei 42 dp pro Stunde = 40 Minuten). */
private val KARTE_MIN = 28.dp

/** Höhe einer Terminkarte. Die Karte steht mittig auf ihrer Startzeit (Kartenmitte = Startzeit auf der Zeitleiste). */
private fun karteHoehe(dauer: Int) = (STUNDE * (maxOf(dauer, 30) / 60f) - 3.dp).coerceAtLeast(KARTE_MIN)

/** Gelb des Startzeit-Strichs (wie die mittlere Priorität); blasser für erledigte und gerade gezogene Termine. */
private fun strichFarbe(dunkel: Boolean, a: Aufgabe, zustand: ZiehZustand): Color {
    val gezogen = zustand.aufgabe?.let { it.id == a.id && it.tag == a.tag } == true
    return (if (dunkel) Color(0xFFFFD233) else Color(0xFFE8B000)).copy(alpha = if (gezogen) 0.25f else if (a.erledigt) 0.45f else 1f)
}

/** Wie weit der gelbe Startzeit-Strich in die Karte hineinreicht; dahinter beginnen Uhrzeit und Titel. */
private val STRICH_IN_KARTE = 13.dp

/**
 * Senkrechter Zeitpfeil. Karten, die darüber gezogen werden, zeigen links die Uhrzeit, die beim Loslassen gilt
 * (in 15-Minuten-Schritten, sie wandert mit dem Finger mit).
 *
 * Ohne Ziehen zeigt sie mit [auto] nur die Stunden um die Termine und rückt mit [luecken] freie Stunden zwischen zwei
 * Terminen zusammen; [ganzerTag] schaltet beides für diesen Tag ab. Beim Ziehen gilt immer die eingestellte Spanne,
 * damit man überall ablegen kann.
 */
@Composable
fun Zeitleiste(
    tag: Long, termine: List<Aufgabe>, istHeute: Boolean, zustand: ZiehZustand, vonEinst: Int, bisEinst: Int, auto: Boolean,
    luecken: Boolean, ganzerTag: Boolean, onGanzerTag: (Boolean) -> Unit,
    onTipp: (Aufgabe) -> Unit, onErledigt: (Aufgabe) -> Unit,
) {
    // Aktuelle Uhrzeit in Minuten; heute bestimmt sie die Spanne mit, damit die rote Jetzt-Linie immer zu sehen ist.
    var jetzt by remember { mutableIntStateOf(LocalTime.now().let { it.hour * 60 + it.minute }) }
    if (istHeute) LaunchedEffect(Unit) {
        while (true) { delay(15_000); jetzt = LocalTime.now().let { it.hour * 60 + it.minute } }
    }
    val jetztHeute = if (istHeute) jetzt else null
    val kompakt = remember(termine, vonEinst, bisEinst, auto, luecken, jetztHeute) { Zeitband.fuer(termine, vonEinst, bisEinst, auto, luecken, jetztHeute) }
    // „Ganzer Tag“ zeigt nie weniger als „Kompakt“ (die Automatik reicht eine Stunde über die Termine hinaus).
    val ganz = remember(termine, vonEinst, bisEinst, kompakt) {
        Zeitband.ganzerTag(termine, vonEinst, bisEinst).let { Zeitband(minOf(it.vonMin, kompakt.vonMin), maxOf(it.bisMin, kompakt.bisMin), dehnungen = it.dehnungen) }
    }
    // Beim Ziehen genau die eingestellte Spanne: Darauf rechnet ZiehZustand (ziehSpanne) den Ausgleich beim Ziehstart.
    val band = when {
        zustand.aufgabe != null -> Zeitband(vonEinst, bisEinst)
        (luecken || auto) && ganzerTag -> ganz
        else -> kompakt
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Sichtbar genau dann, wenn die Einstellung an ist — nie abhängig vom Ziehen: Eine Höhenänderung über der Leiste
        // beim Ziehstart kennt der Ausgleich in ZiehZustand nicht, die Leiste spränge unter dem Finger.
        if (luecken || auto) AnsichtSchalter(ganzerTag, kompakt.ausgeblendet(ganz.vonMin, ganz.bisMin), ganz.vonMin, ganz.bisMin, onGanzerTag)
        Leiste(tag, termine, istHeute, jetzt, zustand, band, if ((luecken || auto) && ganzerTag) ganz else kompakt, if (luecken) { { onGanzerTag(true) } } else null, onTipp, onErledigt)
    }
}

@Composable
private fun Leiste(
    tag: Long, termine: List<Aufgabe>, istHeute: Boolean, jetzt: Int, zustand: ZiehZustand, band: Zeitband, rueckBand: Zeitband, onLuecke: (() -> Unit)?,
    onTipp: (Aufgabe) -> Unit, onErledigt: (Aufgabe) -> Unit,
) {
    val f = LocalFarben.current
    val dichte = LocalDensity.current
    val messer = rememberTextMeasurer()
    val stundePx = with(dichte) { STUNDE.toPx() }
    val lueckePx = with(dichte) { LUECKE.toPx() }
    val vonMin = band.vonMin
    val bisMin = band.bisMin
    // Oberer Rand: Karten stehen mittig auf ihrer Startzeit, eine Karte am Bandanfang (0 Uhr, lange Dauer) ragt sonst über
    // die Leiste. Gerechnet am Band ohne Ziehen ([rueckBand]): Beim Ziehstart darf sich die Höhe über dem Band nicht
    // ändern, den Ausgleich in ZiehZustand gibt es nur für die Bandhöhe selbst.
    val obenDp = remember(termine, rueckBand) {
        termine.maxOfOrNull { a ->
            val min = (a.minuten ?: rueckBand.vonMin).coerceIn(rueckBand.vonMin, rueckBand.bisMin)
            karteHoehe(a.dauer) / 2 - rueckBand.y(min, STUNDE.value, LUECKE.value).dp + 2.dp
        }?.coerceAtLeast(OBEN) ?: OBEN
    }
    // Höhen in dp: dieselbe Rechnung wie in px, nur mit den dp-Werten.
    fun yDp(min: Int) = band.y(min, STUNDE.value, LUECKE.value).dp
    val obenPx = with(dichte) { obenDp.toPx() }
    val hoehe = obenDp + band.hoehe(STUNDE.value, LUECKE.value).dp + 30.dp
    // Höhe der Jetzt-Linie in px ab Oberkante der Leiste. In einer zusammengerückten Lücke läuft sie nie durch den Text
    // „… frei“: Sie steht in der ersten Hälfte der Lücke knapp über der Marke, in der zweiten knapp darunter.
    fun jetztY(): Float? {
        if (!istHeute || jetzt !in vonMin..bisMin) return null
        val luecke = band.luecken.firstOrNull { jetzt > it.von && jetzt < it.bis } ?: return obenPx + band.y(jetzt, stundePx, lueckePx)
        val a = obenPx + band.y(luecke.von, stundePx, lueckePx)
        return if (jetzt - luecke.von < luecke.minuten / 2f) a + with(dichte) { 1.5.dp.toPx() } else a + lueckePx - with(dichte) { 1.5.dp.toPx() }
    }
    // Beim Umschalten (Kompakt ⇄ Ganzer Tag, Termine geändert) blendet die Leiste weich neu ein — nicht beim Ziehen und
    // nicht direkt nach dem Loslassen: Dort muss sie ruhig im selben Frame stehen, in dem ZiehZustand nachscrollt.
    val sichtbar = remember { Animatable(1f) }
    val letztesBand = remember { arrayOf(band) }
    LaunchedEffect(band) {
        val nachAblage = System.currentTimeMillis() - zustand.letztesEnde < 2500
        if (band != letztesBand[0] && zustand.aufgabe == null && !nachAblage) { sichtbar.snapTo(0.25f); sichtbar.animateTo(1f, tween(340)) }
        letztesBand[0] = band
    }
    DisposableEffect(tag) { onDispose { zustand.entferneLeiste(tag) } }
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(hoehe)
            .graphicsLayer { alpha = sichtbar.value; compositingStrategy = CompositingStrategy.ModulateAlpha }
            .onGloballyPositioned { c ->
                // Ungeschnittene Lage (positionInRoot + size), nicht boundsInRoot: Das ist am Scrollrand abgeschnitten, die
                // Nulllinie läge sonst am Bildrand und die Uhrzeit spränge. ZiehZustand rechnet sie in Inhaltslage um.
                val p = c.positionInRoot()
                zustand.registriereLeiste(tag, p.x, p.x + c.size.width, p.y + obenPx, stundePx, lueckePx, band, rueckBand)
            },
    ) {
        val breite = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            val x = SPALTE.toPx() - 8.dp.toPx()
            val y0 = obenPx
            val y1 = obenPx + band.hoehe(stundePx, lueckePx)
            fun yVon(min: Int) = y0 + band.y(min, stundePx, lueckePx)
            // Stundenraster; in einer Lücke nur ihre beiden Randstunden
            for (s in (vonMin + 59) / 60..bisMin / 60) {
                val m = s * 60
                if (band.inLuecke(m)) continue
                val y = yVon(m)
                drawLine(f.textSchwach.copy(alpha = 0.18f), Offset(x + 8.dp.toPx(), y), Offset(size.width, y), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
                drawLine(f.textLeise.copy(alpha = 0.6f), Offset(x - 6.dp.toPx(), y), Offset(x + 4.dp.toPx(), y), 2f, StrokeCap.Round)
                val text = messer.measure("%02d:00".format(s), TextStyle(color = f.textLeise, fontSize = 11.sp, fontWeight = FontWeight.Medium))
                drawText(text, topLeft = Offset(x - 10.dp.toPx() - text.size.width, y - text.size.height / 2f))
                if (m + 30 <= bisMin && !band.inLuecke(m + 30)) {
                    val yh = yVon(m + 30)
                    drawLine(f.textSchwach.copy(alpha = 0.5f), Offset(x - 3.dp.toPx(), yh), Offset(x + 2.dp.toPx(), yh), 1.5f)
                }
            }
            // Der Zeitpfeil: ein durchgehender Verlauf, in jeder Lücke unterbrochen und durch drei Punkte ersetzt
            val pinsel = Brush.verticalGradient(listOf(f.primaer, f.sekundaer), y0, y1)
            val dicke = 3.5.dp.toPx()
            var strecke = y0 - 6.dp.toPx()
            band.luecken.forEach { l ->
                val a = yVon(l.von)
                val mitte = a + lueckePx / 2f
                drawLine(pinsel, Offset(x, strecke), Offset(x, a + 2.dp.toPx()), dicke, StrokeCap.Round)
                for (i in -1..1) drawCircle(pinsel, 2.dp.toPx(), Offset(x, mitte + i * 6.dp.toPx()))
                strecke = a + lueckePx - 2.dp.toPx()
            }
            drawLine(pinsel, Offset(x, strecke), Offset(x, y1 + 12.dp.toPx()), dicke, StrokeCap.Round)
            val spitze = Path().apply {
                moveTo(x - 7.dp.toPx(), y1 + 10.dp.toPx()); lineTo(x, y1 + 22.dp.toPx()); lineTo(x + 7.dp.toPx(), y1 + 10.dp.toPx()); close()
            }
            drawPath(spitze, f.sekundaer)
            if (vonMin % 60 != 0) drawLine(f.textSchwach.copy(alpha = 0.5f), Offset(x - 3.dp.toPx(), y0), Offset(x + 2.dp.toPx(), y0), 1.5f)
            // Jetzt-Linie
            jetztY()?.let { y ->
                drawLine(f.gefahr, Offset(x, y), Offset(size.width, y), 2.dp.toPx(), StrokeCap.Round)
                drawCircle(f.gefahr, 6.dp.toPx(), Offset(x, y))
                drawCircle(Color.White, 2.5.dp.toPx(), Offset(x, y))
            }
        }
        val flaeche = breite - SPALTE - 6.dp
        // Zusammengerückte freie Stunden; ein Tipp zeigt den ganzen Tag
        band.luecken.forEach { l ->
            LueckenMarke(
                l, onLuecke,
                Modifier.offset(x = SPALTE + 2.dp, y = obenDp + yDp(l.von) + 4.dp).width(flaeche - 4.dp).height(LUECKE - 8.dp),
            )
        }
        // Termine: Dicht liegende Startzeiten dehnt das Band (Zeitband.dehnungen), die Karten stehen dann untereinander.
        // Was sich trotzdem noch überdeckt (gleiche Startzeit, beim Ziehen das ungedehnte Band), steht nebeneinander —
        // nur innerhalb dieser Gruppe; alle anderen Karten behalten die volle Breite.
        val sortiert = termine.sortedBy { it.minuten }
        val spalteVon = HashMap<Long, Int>()
        val spaltenVon = HashMap<Long, Int>()
        run {
            fun mitte(a: Aufgabe) = yDp((a.minuten ?: vonMin).coerceIn(vonMin, bisMin)).value
            val nachOben = sortiert.sortedBy { mitte(it) - karteHoehe(it.dauer).value / 2 }
            val spalten = mutableListOf<Float>() // Unterkante der letzten Karte je Spalte in der laufenden Gruppe
            val gruppe = mutableListOf<Long>()
            var gruppeEnde = Float.NEGATIVE_INFINITY
            fun abschliessen() { gruppe.forEach { spaltenVon[it] = spalten.size.coerceAtLeast(1) }; gruppe.clear(); spalten.clear() }
            nachOben.forEach { a ->
                val h = karteHoehe(a.dauer).value
                val oben = mitte(a) - h / 2
                // Nur Rundungsrauschen wird übersehen; jede echte Überdeckung kommt in dieselbe Gruppe.
                if (oben >= gruppeEnde - 0.01f) abschliessen()
                val i = spalten.indexOfFirst { it <= oben + 0.01f }.let { if (it < 0) { spalten.add(0f); spalten.lastIndex } else it }
                spalten[i] = oben + h
                spalteVon[a.id] = i
                gruppe.add(a.id)
                gruppeEnde = maxOf(gruppeEnde, oben + h)
            }
            abschliessen()
        }
        // Gelber Startzeit-Strich, äußerer Teil: vom Zeitpfeil waagerecht bis an die Karte, genau auf Höhe ihrer Mitte
        // (= Startzeit). Unter den Karten gezeichnet, damit er nie quer über eine Karte links daneben läuft; das Stück in
        // der Karte bis zur Uhrzeit zeichnet TerminBlock selbst.
        Canvas(Modifier.fillMaxSize()) {
            val x = SPALTE.toPx() - 8.dp.toPx()
            sortiert.forEach { a ->
                val min = (a.minuten ?: vonMin).coerceIn(vonMin, bisMin)
                val y = obenPx + band.y(min, stundePx, lueckePx)
                val sp = spalteVon[a.id] ?: 0
                val n = spaltenVon[a.id] ?: 1
                val farbe = strichFarbe(f.dunkel, a, zustand)
                drawLine(farbe, Offset(x, y), Offset((SPALTE + flaeche / n * sp).toPx() + 2.dp.toPx(), y), 2.5.dp.toPx(), StrokeCap.Round)
                drawCircle(farbe, 4.5.dp.toPx(), Offset(x, y))
            }
        }
        sortiert.forEach { a ->
            val min = (a.minuten ?: vonMin).coerceIn(vonMin, bisMin)
            // Kartenmitte genau auf der Startzeit: Oberkante eine halbe Kartenhöhe darüber.
            val h = karteHoehe(a.dauer)
            val oben = obenDp + yDp(min) - h / 2
            val sp = spalteVon[a.id] ?: 0
            val n = spaltenVon[a.id] ?: 1
            Box(
                Modifier.offset(x = SPALTE + flaeche / n * sp, y = oben)
                    .width(flaeche / n - 4.dp).height(h)
                    // Gemeldet wird die Kartenmitte: Sie liegt auf der Zeitlinie, an der beim Loslassen abgelegt wurde.
                    .onGloballyPositioned { zustand.meldeTermin(a, it.positionInRoot().y + it.size.height / 2f) },
            ) { TerminBlock(a, zustand, onTipp = { onTipp(a) }, onErledigt = { onErledigt(a) }) }
        }
        // Uhrzeit-Marke mittig auf der Jetzt-Linie, über den Terminen gezeichnet, damit sie nie verdeckt ist
        Canvas(Modifier.fillMaxSize()) {
            val y = jetztY() ?: return@Canvas
            val text = messer.measure("%02d:%02d Uhr".format(jetzt / 60, jetzt % 60), TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            val bw = text.size.width + 16.dp.toPx()
            val bh = text.size.height + 5.dp.toPx()
            val mitte = (SPALTE.toPx() - 8.dp.toPx() + size.width) / 2f
            drawRoundRect(f.gefahr, Offset(mitte - bw / 2f, y - bh / 2f), Size(bw, bh), CornerRadius(bh / 2f))
            drawText(text, topLeft = Offset(mitte - text.size.width / 2f, y - text.size.height / 2f))
        }
    }
}

/** „3 Std. frei“ in einer zusammengerückten Lücke; mit [aktion] zeigt ein Tipp den ganzen Tag. */
@Composable
private fun LueckenMarke(l: Luecke, aktion: (() -> Unit)?, modifier: Modifier) {
    val f = LocalFarben.current
    Row(
        modifier.glas(f, radius = 99.dp, erhoeht = 0f, fuellung = f.primaer.copy(alpha = 0.07f), rand = false)
            .then(if (aktion != null) Modifier.antippen(haptik = true, aktion = aktion) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.UnfoldMore, "Ganzen Tag zeigen", tint = f.primaer.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
        Text(
            "${dauerText(l.minuten)} frei · ${l.von / 60}–${l.bis / 60} Uhr", color = f.textLeise, fontSize = 11.sp,
            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 5.dp),
        )
    }
}

/**
 * Kopfzeile der Zeitleiste mit dem Umschalter „Kompakt“ ⇄ „Ganzer Tag“. Links steht, was die Ansicht gerade ausblendet
 * bzw. welche Spanne zu sehen ist; der farbige Knopf gleitet zur gewählten Seite.
 */
@Composable
private fun AnsichtSchalter(ganzerTag: Boolean, ausgeblendet: Int, von: Int, bis: Int, aendern: (Boolean) -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (ganzerTag) Icons.Rounded.Schedule else Icons.Rounded.UnfoldLess, null, tint = f.textLeise, modifier = Modifier.size(15.dp))
        Text(
            when {
                ganzerTag -> "${Tage.zeit(von)}–${Tage.zeit(bis)} Uhr"
                ausgeblendet > 0 -> "${dauerText(ausgeblendet)} ausgeblendet"
                else -> "Alles im Blick"
            },
            color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 6.dp, end = 8.dp),
        )
        val teil = 100.dp
        val lage by animateDpAsState(if (ganzerTag) teil else 0.dp, spring(dampingRatio = 0.72f, stiffness = 520f), label = "ansicht")
        Box(Modifier.width(teil * 2 + 6.dp).height(36.dp).glas(f, radius = 99.dp, erhoeht = 0.5f, fuellung = f.flaeche).padding(3.dp)) {
            Box(Modifier.offset(x = lage).width(teil).fillMaxHeight().knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel))
            Row(Modifier.fillMaxSize()) {
                AnsichtTeil(Icons.Rounded.UnfoldLess, "Kompakt", !ganzerTag, Modifier.weight(1f)) { aendern(false) }
                AnsichtTeil(Icons.Rounded.UnfoldMore, "Ganzer Tag", ganzerTag, Modifier.weight(1f)) { aendern(true) }
            }
        }
    }
}

@Composable
private fun AnsichtTeil(icon: ImageVector, text: String, gewaehlt: Boolean, modifier: Modifier, aktion: () -> Unit) {
    val f = LocalFarben.current
    val farbe by animateColorAsState(if (gewaehlt) Color.White else f.textLeise, label = "ansichtFarbe")
    Row(
        modifier.fillMaxHeight().antippen(rolle = Role.Tab) { if (!gewaehlt) aktion() }
            .semantics { selected = gewaehlt },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = farbe, modifier = Modifier.size(15.dp))
        Text(text, color = farbe, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
    }
}

/** 180 → „3 Std.“, 150 → „2,5 Std.“, 30 → „30 Min.“ */
private fun dauerText(minuten: Int): String {
    val h = minuten / 60
    val r = minuten % 60
    return when {
        h == 0 -> "$r Min."
        r == 0 -> "$h Std."
        r == 30 -> "$h,5 Std."
        else -> "$h Std. $r Min."
    }
}

/** 1 mm (1 dp = 1/160 Zoll): So weit liegt die Zeitlinie über der gezogenen Karte. */
val LINIE_UEBER_KARTE = (160f / 25.4f).dp

/**
 * Vorschau beim Ziehen über eine Zeitleiste: orange Linie vom Zeitpfeil nach rechts plus Geisterblock in der Dauer der
 * Aufgabe. Liegt in der Ebene der gezogenen Karte (nicht im gescrollten Inhalt) und liest dieselbe Höhe wie die Karte
 * ([ZiehZustand.linieY]) — darum gleitet sie ohne Raster und Feder exakt mit, auch beim Randscrollen.
 */
@Composable
fun ZeitVorschau(zustand: ZiehZustand) {
    val (tag, _) = zustand.hoverZeit ?: return
    val m = zustand.leiste(tag) ?: return
    val f = LocalFarben.current
    val dichte = LocalDensity.current
    val breite = with(dichte) { (m.rechts - m.links).toDp() }
    val flaeche = breite - SPALTE - 6.dp
    val dauer = zustand.aufgabe?.dauer ?: 30
    val h = karteHoehe(dauer)
    // Wie die abgelegte Karte: mittig auf der Zeitlinie.
    Box(
        Modifier.graphicsLayer { translationX = m.links - zustand.ursprung.x; translationY = zustand.linieY() - zustand.ursprung.y - h.toPx() / 2f }.offset(x = SPALTE)
            .width(flaeche).height(h)
            .glas(f, radius = 12.dp, erhoeht = 0f, fuellung = f.primaer.copy(alpha = 0.22f)),
    )
    Canvas(
        Modifier.graphicsLayer { translationX = m.links - zustand.ursprung.x; translationY = zustand.linieY() - zustand.ursprung.y - 6.dp.toPx() }
            .width(breite).height(12.dp),
    ) {
        val x = SPALTE.toPx() - 8.dp.toPx()
        val y = size.height / 2f
        drawLine(Brush.horizontalGradient(listOf(f.primaer, f.sekundaer), x, size.width), Offset(x, y), Offset(size.width - 4.dp.toPx(), y), 2.5.dp.toPx(), StrokeCap.Round)
        drawCircle(f.primaer, 5.dp.toPx(), Offset(x, y))
        drawCircle(Color.White, 2.dp.toPx(), Offset(x, y))
    }
}

/** Uhrzeit-Marke links an der Zeitlinie; über der gezogenen Karte gezeichnet, damit sie nie verdeckt wird. */
@Composable
fun ZeitMarke(zustand: ZiehZustand) {
    val (tag, minuten) = zustand.hoverZeit ?: return
    val m = zustand.leiste(tag) ?: return
    val f = LocalFarben.current
    Box(
        Modifier.graphicsLayer { translationX = m.links - zustand.ursprung.x; translationY = zustand.linieY() - zustand.ursprung.y - 13.dp.toPx() }
            .width(SPALTE - 4.dp).height(26.dp).knopf3d(f.primaer, f.sekundaer, 13.dp, f.dunkel),
        contentAlignment = Alignment.Center,
    ) { Text(Tage.zeit(minuten), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun TerminBlock(a: Aufgabe, zustand: ZiehZustand, onTipp: () -> Unit, onErledigt: () -> Unit) {
    // Erledigt: ganzer Block in der grauen Farbfassung (Fläche, Leiste, Text, Symbole).
    val f = LocalFarben.current.let { if (a.erledigt) it.erledigt() else it }
    val farbe = f.prio(a.prio)
    val gezogen = zustand.aufgabe?.let { it.id == a.id && it.tag == a.tag } == true
    val strich = strichFarbe(LocalFarben.current.dunkel, a, zustand).copy(alpha = if (a.erledigt) 0.45f else 1f)
    val ende = ((a.minuten ?: 0) + a.dauer) % (24 * 60)
    Row(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = if (gezogen) 0.25f else 1f; compositingStrategy = CompositingStrategy.ModulateAlpha }
            .glas(f, radius = 12.dp, erhoeht = 1f, fuellung = f.flaecheStark, toenung = farbe)
            .ziehbar(a, zustand)
            .antippen(haptik = false) { if (zustand.tippErlaubt()) onTipp() }
            // Inneres Stück des gelben Startzeit-Strichs: über der Farbleiste bis direkt vor die Uhrzeit, auf Kartenmitte.
            .drawWithContent {
                drawContent()
                val y = size.height / 2f
                drawLine(strich, Offset(0f, y), Offset(STRICH_IN_KARTE.toPx() - 3.dp.toPx(), y), 2.5.dp.toPx(), StrokeCap.Round)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(5.dp).fillMaxHeight().knopf3d(farbe, farbe.copy(alpha = 0.7f), 3.dp, f.dunkel))
        // Auf Kartenmitte (= Startzeit): direkt hinter dem gelben Strich die gewählte Uhrzeit, dann der Titel.
        Row(Modifier.weight(1f).padding(start = STRICH_IN_KARTE, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${Tage.zeit(a.minuten ?: 0)} Uhr ") }
                    append(a.titel)
                },
                color = f.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (a.erledigt) TextDecoration.LineThrough else null, modifier = Modifier.weight(1f, fill = false),
            )
            // Ab einer Stunde Dauer das Ende dazu: Die mittige Karte zeigt es mit ihrer Unterkante nicht mehr an.
            if (a.dauer >= 60) Text(" · bis ${Tage.zeit(ende)}", color = f.textLeise, fontSize = 11.sp, maxLines = 1, softWrap = false)
            // Als Wecker eingestellt: Weckersymbol direkt neben dem Titel, sonst die Glocke bei Erinnerung.
            if (a.erinnerung && a.alsWecker) Icon(Icons.Rounded.Alarm, "Wecker", tint = f.sekundaer, modifier = Modifier.padding(start = 4.dp).size(14.dp))
            if (a.erinnerung && !a.alsWecker) Icon(Icons.Rounded.NotificationsActive, null, tint = f.sekundaer, modifier = Modifier.padding(start = 4.dp).size(12.dp))
        }
        Box(Modifier.size(34.dp).antippen(haptik = true, aktion = onErledigt), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                drawCircle(farbe, size.minDimension / 2 - 1.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                if (a.erledigt) drawCircle(farbe, size.minDimension / 2 - 4.dp.toPx())
            }
        }
    }
}
