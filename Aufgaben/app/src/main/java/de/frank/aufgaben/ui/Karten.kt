package de.frank.aufgaben.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.Prioritaet
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.data.Wiederholung
import de.frank.aufgaben.ui.theme.LocalBewegung
import de.frank.aufgaben.ui.theme.Farben
import de.frank.aufgaben.ui.theme.LocalFarben
import de.frank.aufgaben.ui.theme.antippen
import de.frank.aufgaben.ui.theme.glas
import de.frank.aufgaben.ui.theme.knopf3d
import kotlinx.coroutines.delay

/** Einblenden beim ersten Erscheinen: kippt leicht aus der Tiefe nach vorn. */
fun Modifier.einblenden(verzoegerung: Int = 0): Modifier = composed {
    val bewegung = LocalBewegung.current
    val a = remember { Animatable(if (bewegung) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (bewegung) { kotlinx.coroutines.delay(verzoegerung.toLong()); a.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 180f)) }
    }
    graphicsLayer {
        val v = a.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 60f
        rotationX = (1f - v) * 18f
        cameraDistance = 16f * density
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}

/** Dunkle Schrift und Symbole auf hellen Farben (auch das helle Gelb mit Helligkeit ~0,48), sonst weiße. */
fun aufFarbe(farbe: Color): Color = if (farbe.luminance() > 0.4f) Color.Black.copy(alpha = 0.85f) else Color.White

@Composable
fun Checkkreis(erledigt: Boolean, farbe: Color, groesse: Int = 26, aktion: () -> Unit) {
    val f = LocalFarben.current
    val haptik = LocalHapticFeedback.current
    val fort by animateFloatAsState(if (erledigt) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "haken")
    Box(
        Modifier.size((groesse + 14).dp).antippen(haptik = false) {
            haptik.performHapticFeedback(HapticFeedbackType.LongPress)
            aktion()
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(groesse.dp).graphicsLayer { scaleX = 1f + 0.15f * fort * (1f - fort) * 4f; scaleY = scaleX }) {
            val r = size.minDimension / 2
            drawCircle(farbe.copy(alpha = 0.12f + 0.88f * fort), r)
            drawCircle(farbe, r - 1.5.dp.toPx(), style = Stroke(2.dp.toPx()))
            if (fort > 0f) {
                val p = Path().apply {
                    moveTo(r * 0.55f, r * 1.02f)
                    lineTo(r * 0.88f, r * 1.35f)
                    lineTo(r * (0.88f + 0.62f * fort), r * (1.35f - 0.72f * fort))
                }
                drawPath(p, if (farbe.luminance() > 0.4f) aufFarbe(farbe) else if (f.dunkel) f.aufPrimaer else Color.White, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun Merkmal(icon: ImageVector?, text: String, farbe: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (icon != null) Icon(icon, null, tint = farbe, modifier = Modifier.size(13.dp))
        Text(text, color = farbe, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** Eine Aufgabe als Glaskarte. */
@Composable
fun AufgabeKarte(
    a: Aufgabe,
    heute: Long,
    zustand: ZiehZustand?,
    zeigeDatum: Boolean = false,
    schwebend: Boolean = false,
    hervorheben: Boolean = false,
    onTipp: () -> Unit,
    onErledigt: () -> Unit,
) {
    val f = LocalFarben.current
    val prio = f.prio(a.prio)
    val gezogen = zustand?.aufgabe?.let { it.id == a.id && it.tag == a.tag } == true && !schwebend
    val glow by animateFloatAsState(if (hervorheben) 1f else 0f, tween(600), label = "glow")
    var basis = Modifier
        .fillMaxWidth()
        .graphicsLayer { alpha = if (gezogen) 0.25f else if (a.erledigt) 0.62f else 1f; compositingStrategy = CompositingStrategy.ModulateAlpha }
        .glas(f, radius = f.radius * 0.72f, erhoeht = if (schwebend) 2.6f else 0.9f, fuellung = if (schwebend) f.flaecheStark else f.flaeche, toenung = if (glow > 0f) f.primaer else prio)
    if (zustand != null && !schwebend) basis = basis.ziehbar(a, zustand)
    Row(
        basis.antippen(haptik = false) { if (zustand == null || zustand.tippErlaubt()) onTipp() }
            .padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkkreis(a.erledigt, prio, aktion = onErledigt)
        Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(
                a.titel,
                color = f.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (a.erledigt) TextDecoration.LineThrough else null,
            )
            val schritte = a.schritte
            val ueberfaellig = a.ueberfaellig(heute)
            val zeigtMerkmale = a.minuten != null || a.erinnerung || a.wdh != Wiederholung.KEINE || schritte.isNotEmpty() || ueberfaellig || zeigeDatum
            if (a.text.isNotBlank() && a.text != a.titel) {
                Text(a.text, color = f.textLeise, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            if (zeigtMerkmale) Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ueberfaellig) Merkmal(null, "überfällig · ${Tage.datum(a.tag!!)}", f.gefahr)
                else if (zeigeDatum && a.tag != null) Merkmal(null, Tage.datum(a.tag), f.primaer)
                a.minuten?.let { Merkmal(Icons.Rounded.Schedule, Tage.zeit(it), f.primaer) }
                if (a.erinnerung && a.minuten != null) Merkmal(Icons.Rounded.NotificationsActive, if (a.vorlauf > 0) "−${a.vorlauf}′" else "", f.sekundaer)
                if (a.wdh != Wiederholung.KEINE) Merkmal(Icons.Rounded.Repeat, a.wdh.anzeige, f.tertiaer)
                if (schritte.isNotEmpty()) Merkmal(Icons.Rounded.Checklist, "${schritte.count { it.erledigt }}/${schritte.size}", f.textLeise)
            }
        }
        if (a.prio != Prioritaet.SPAETER && a.tag != null) {
            Box(Modifier.padding(start = 6.dp).size(width = 6.dp, height = 26.dp).knopf3d(prio, prio.copy(alpha = 0.7f), 3.dp, f.dunkel))
        }
        if (zustand != null && !schwebend) Icon(Icons.Rounded.DragIndicator, "Lang drücken und ziehen", tint = f.textSchwach, modifier = Modifier.padding(start = 6.dp).size(20.dp))
    }
}

/** Ein Bereich (Heute, Morgen, Hoch …) als große Glasfläche mit Kopf. */
@Composable
fun Sektion(
    schluessel: String,
    titel: String,
    icon: ImageVector,
    farbe: Color,
    anzahl: Int,
    zustand: ZiehZustand,
    ziel: Ziel,
    untertitel: String? = null,
    /** Ohne Treffer: Während der Suche bleibt der Bereich dann zugeklappt; sonst sind alle Bereiche offen, auch leere (außer [startZu]). */
    leer: Boolean = false,
    /** Aus während der Suche: Ein leeres Suchergebnis soll die eigene Auf-/Zuklapp-Wahl nicht verwerfen. */
    automatik: Boolean = true,
    verzoegerung: Int = 0,
    /** Sattere Farbe für Kopfband, Rahmen und Leiste (Prioritätsbereiche). */
    kraeftig: Boolean = false,
    /** Ganze Fläche leuchtet als etwas Besonderes hervorgehoben (Bereich Heute). */
    leuchten: Boolean = false,
    /** Bereich ist standardmäßig zugeklappt (Erledigt); während der Suche klappt er bei Treffern auf. */
    startZu: Boolean = false,
    /** Auf-/Zuklapp-Wahl von außen, damit sie erhalten bleibt, wenn der Bereich zeitweise nicht angezeigt wird. */
    wahlZustand: MutableIntState? = null,
    aktionen: @Composable RowScope.() -> Unit = {},
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    val f = LocalFarben.current
    // Leuchten (Heute): ruhig atmender Lichthof um die ganze Fläche, wanderndes Licht darin und ein umlaufender Lichtrand.
    // Nur mit eingeschalteter Bewegung (Einstellung „Bewegung“) und laufenden Systemanimationen; sonst leuchtet der Bereich still.
    val kontext = LocalContext.current
    val systemAnimationen = remember {
        android.provider.Settings.Global.getFloat(kontext.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val bewegt = leuchten && LocalBewegung.current && systemAnimationen
    val lichtLauf = if (bewegt) rememberInfiniteTransition(label = "heuteLicht") else null
    val drehung = lichtLauf?.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "lichtDreh")
    val atem = lichtLauf?.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), label = "lichtAtem")
    val drehLeser: () -> Float = { drehung?.value ?: 35f }
    val atemLeser: () -> Float = { atem?.value ?: 0.25f }
    val aufFarbe = aufFarbe(farbe)
    // 0 = automatisch (beim Start sind alle Bereiche offen, auch leere, damit man überall hineinziehen kann; mit [startZu]
    // zugeklappt; während der Suche nur die mit Treffern), 1 = selbst aufgeklappt, 2 = selbst zugeklappt.
    val eigeneWahl = rememberSaveable(schluessel) { mutableIntStateOf(0) }
    var wahl by (wahlZustand ?: eigeneWahl)
    // Startet die Suche, gilt wieder die Automatik: Auch selbst zugeklappte Bereiche zeigen ihre Treffer.
    // Ein selbst aufgeklappter Bereich, der von sich aus zu wäre ([startZu]), bleibt dabei offen.
    LaunchedEffect(automatik) { if (!automatik && !(startZu && wahl == 1)) wahl = 0 }
    val offen = when (wahl) { 1 -> true; 2 -> false; else -> if (startZu) !automatik && !leer else automatik || !leer }
    // Kopfband-Ecken und Trennlinie folgen dem Auf-/Zuklappen des Inhalts, statt sofort umzuspringen.
    val offenAnteil = animateFloatAsState(if (offen) 1f else 0f, label = "kopfOffen")
    val zielSchluessel = "sek_$schluessel"
    val schwebt = zustand.hoverZiel == zielSchluessel
    // Schwebt eine gezogene Aufgabe zwei Sekunden ruhig über dem geschlossenen Bereich, klappt er auf und bleibt offen.
    // Danach lässt sie sich darin ablegen, bei Tagen mit Zeitleiste auch auf eine Uhrzeit. Solange die Liste am Rand
    // automatisch scrollt, zählt die Wartezeit nicht (sonst klappte am Listenende der letzte Bereich ungewollt auf).
    val haptikAuf = LocalHapticFeedback.current
    LaunchedEffect(schwebt, offen) {
        if (!schwebt || offen || zustand.aufgabe == null) return@LaunchedEffect
        var ruhigSeit = System.currentTimeMillis()
        while (zustand.aufgabe != null && zustand.hoverZiel == zielSchluessel) {
            val nun = System.currentTimeMillis()
            if (zustand.randScrollt) ruhigSeit = nun
            if (nun - ruhigSeit >= 2000) {
                wahl = 1
                haptikAuf.performHapticFeedback(HapticFeedbackType.LongPress)
                break
            }
            delay(50)
        }
    }
    val rand by animateColorAsState(if (schwebt) farbe else Color.Transparent, label = "rand")
    val hub by animateFloatAsState(if (schwebt) 1.015f else 1f, spring(dampingRatio = 0.6f), label = "hub")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .einblenden(verzoegerung)
            .graphicsLayer { scaleX = hub; scaleY = hub }
            .ablageZiel(zustand, zielSchluessel, ziel)
            .then(if (leuchten) Modifier.lichtHof(f, farbe, atemLeser) else Modifier)
            // Farbiger Rahmen in der Bereichsfarbe grenzt die Bereiche klar voneinander ab.
            .glas(f, erhoeht = 1.3f, fuellung = f.flaeche.copy(alpha = f.flaeche.alpha * 0.8f), toenung = farbe, randFarbe = farbe.copy(alpha = if (kraeftig) 0.9f else if (f.dunkel) 0.55f else 0.45f))
            .then(if (leuchten) Modifier.lichtGlanz(f, farbe, drehLeser, atemLeser) else Modifier)
            .then(if (schwebt) Modifier.glas(f, erhoeht = 0f, fuellung = rand.copy(alpha = 0.12f)) else Modifier)
            // Beim Ziehen ohne Größenanimation: ZiehZustand gleicht den Spannenwechsel der Zeitleiste im selben Frame per
            // Scroll aus; eine nachfedernde Höhe ließe den Inhalt erst springen und dann zurückdriften.
            .then(if (zustand.aufgabe == null) Modifier.animateContentSize(spring(dampingRatio = 0.85f, stiffness = 380f)) else Modifier),
    ) {
        Row(
            Modifier.fillMaxWidth().kopfBand(f, farbe, kraeftig) { offenAnteil.value }.antippen(haptik = false) { wahl = if (offen) 2 else 1 }.padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(38.dp).knopf3d(farbe.copy(alpha = 0.95f), farbe.copy(alpha = 0.6f), 13.dp, f.dunkel), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = aufFarbe, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(titel, color = f.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    if (anzahl > 0) Box(
                        Modifier.padding(start = 8.dp).glas(f, 99.dp, 0f, farbe.copy(alpha = 0.92f), rand = false).padding(horizontal = 8.dp, vertical = 1.dp),
                    ) { Text("$anzahl", color = aufFarbe(farbe), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
                if (untertitel != null) Text(untertitel, color = f.text.copy(alpha = 0.78f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            aktionen()
            val dreh by animateFloatAsState(if (offen) 0f else -90f, label = "pfeil")
            Icon(Icons.Rounded.ExpandMore, if (offen) "Zuklappen" else "Aufklappen", tint = f.text, modifier = Modifier.graphicsLayer { rotationZ = dreh })
        }
        AnimatedVisibility(offen, enter = expandVertically(clip = false) + fadeIn(), exit = shrinkVertically(clip = false) + fadeOut()) {
            Column(
                Modifier.fillMaxWidth()
                    // Farbige Leiste links verbindet den Inhalt sichtbar mit seiner Überschrift.
                    .drawBehind {
                        drawRoundRect(
                            farbe.copy(alpha = if (kraeftig) 0.95f else 0.75f), topLeft = Offset(4.dp.toPx(), 10.dp.toPx()),
                            size = Size(3.5.dp.toPx(), (size.height - 22.dp.toPx()).coerceAtLeast(0f)), cornerRadius = CornerRadius(2.dp.toPx()),
                        )
                    }
                    .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                inhalt()
            }
        }
    }
}

/**
 * Kräftig gefärbtes Kopfband eines Bereichs mit diagonalem Streifenmuster in der Bereichsfarbe.
 * Aufgeklappt ([offen] = 1) sind nur die oberen Ecken rund und eine farbige Linie trennt den Kopf vom Inhalt.
 * Pfade und Verläufe liegen im Zeichen-Cache und werden nur bei neuer Größe oder während des Klappens neu gebaut.
 */
private fun Modifier.kopfBand(f: Farben, farbe: Color, kraeftig: Boolean, offen: () -> Float): Modifier = drawWithCache {
    val anteil = offen()
    val r = f.radius.toPx().coerceAtMost(size.minDimension / 2)
    val unten = CornerRadius(r * (1f - anteil))
    val form = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r), CornerRadius(r), unten, unten))
    }
    // Kräftig (Prioritäten): deutlich sattere Fläche und Streifen, damit Rot, Gelb und Grün klar unterscheidbar sind.
    val band = Brush.horizontalGradient(
        if (kraeftig) listOf(farbe.copy(alpha = if (f.dunkel) 0.62f else 0.55f), farbe.copy(alpha = if (f.dunkel) 0.36f else 0.30f))
        else listOf(farbe.copy(alpha = if (f.dunkel) 0.46f else 0.34f), farbe.copy(alpha = if (f.dunkel) 0.20f else 0.14f)),
    )
    val streifenFarbe = farbe.copy(alpha = if (kraeftig) (if (f.dunkel) 0.34f else 0.30f) else if (f.dunkel) 0.16f else 0.13f)
    val abstand = 16.dp.toPx()
    val breite = 5.dp.toPx()
    val h = size.height
    val streifen = Path().apply {
        var x = -h
        while (x < size.width) {
            moveTo(x, h); lineTo(x + breite, h); lineTo(x + breite + h, 0f); lineTo(x + h, 0f); close()
            x += abstand
        }
    }
    val linie = farbe.copy(alpha = 0.7f * anteil)
    val linieDicke = 2.dp.toPx()
    onDrawBehind {
        drawPath(form, band)
        clipPath(form) { drawPath(streifen, streifenFarbe) }
        if (anteil > 0f) drawLine(linie, Offset(0f, h), Offset(size.width, h), linieDicke)
    }
}

/**
 * Lichthof um die ganze Fläche (Bereich Heute): weich verwischtes Leuchten in der Bereichsfarbe, nur außerhalb der Form,
 * damit die Glasfläche darunter klar bleibt. [atem] läuft 0..1 und lässt das Leuchten langsam an- und abschwellen.
 * Gelesen wird [atem] nur beim Zeichnen: kein Neuaufbau der Oberfläche, nur neues Zeichnen.
 */
private fun Modifier.lichtHof(f: Farben, farbe: Color, atem: () -> Float): Modifier = drawWithCache {
    val r = f.radius.toPx().coerceAtMost(size.minDimension / 2)
    val form = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }
    val weite = 13.dp.toPx()
    val stark = if (f.dunkel) 0.75f else 0.55f
    val unscharf = android.os.Build.VERSION.SDK_INT >= 28
    val pinsel = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
        if (unscharf) maskFilter = android.graphics.BlurMaskFilter(weite, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    // Ältere Android-Versionen verwischen nicht hardwarebeschleunigt: dort gestufte Ringe statt Unschärfe.
    val ringe = 9
    val ringBreite = weite / ringe
    onDrawBehind {
        val welle = 0.5f - 0.5f * kotlin.math.cos(atem() * 2f * Math.PI.toFloat())
        val a = stark * (0.55f + 0.45f * welle)
        clipPath(form, ClipOp.Difference) {
            if (unscharf) {
                pinsel.color = farbe.copy(alpha = a).toArgb()
                val aus = weite * 0.15f
                drawIntoCanvas { it.nativeCanvas.drawRoundRect(-aus, -aus, size.width + aus, size.height + aus, r + aus, r + aus, pinsel) }
            } else {
                for (i in 0 until ringe) {
                    val d = ringBreite * (i + 0.5f)
                    drawRoundRect(
                        farbe.copy(alpha = a * 0.35f * (1f - i / ringe.toFloat())), Offset(-d, -d), Size(size.width + 2 * d, size.height + 2 * d),
                        CornerRadius(r + d), style = Stroke(ringBreite),
                    )
                }
            }
        }
    }
}

/**
 * Leuchten innerhalb der Fläche (Bereich Heute), über dem Glas und unter dem Inhalt: zwei weiche Lichtflecken wandern
 * langsam über die ganze Fläche, dazu ein innerer Schimmer am Rand und ein Lichtrand, dessen hellste Stelle ruhig umläuft.
 */
private fun Modifier.lichtGlanz(f: Farben, farbe: Color, drehung: () -> Float, atem: () -> Float): Modifier = drawWithCache {
    val r = f.radius.toPx().coerceAtMost(size.minDimension / 2)
    val form = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }
    val randBreite = 2.5.dp.toPx()
    val innen = Path().apply {
        addRoundRect(RoundRect(randBreite, randBreite, size.width - randBreite, size.height - randBreite, CornerRadius((r - randBreite).coerceAtLeast(0f))))
    }
    val gross = kotlin.math.max(size.width, size.height)
    val diagonale = kotlin.math.sqrt(size.width * size.width + size.height * size.height)
    val hell = lerp(farbe, Color.White, 0.55f)
    val fleckA = Brush.radialGradient(listOf(farbe.copy(alpha = if (f.dunkel) 0.30f else 0.22f), Color.Transparent), Offset.Zero, gross * 0.7f)
    val fleckB = Brush.radialGradient(listOf(hell.copy(alpha = if (f.dunkel) 0.16f else 0.30f), Color.Transparent), Offset.Zero, gross * 0.5f)
    val schimmer = Brush.verticalGradient(listOf(farbe.copy(alpha = if (f.dunkel) 0.28f else 0.20f), farbe.copy(alpha = 0.06f), farbe.copy(alpha = if (f.dunkel) 0.20f else 0.14f)))
    val schimmerStrich = Stroke(width = 16.dp.toPx())
    val lauf = Brush.sweepGradient(
        0f to farbe.copy(alpha = 0.25f), 0.18f to farbe.copy(alpha = 0.55f), 0.25f to hell, 0.32f to farbe.copy(alpha = 0.55f),
        0.5f to farbe.copy(alpha = 0.25f), 0.68f to farbe.copy(alpha = 0.45f), 0.75f to hell.copy(alpha = 0.8f), 0.82f to farbe.copy(alpha = 0.45f),
        1f to farbe.copy(alpha = 0.25f),
        center = Offset.Zero,
    )
    val mitte = Offset(size.width / 2, size.height / 2)
    onDrawBehind {
        val t = atem() * 2f * Math.PI.toFloat()
        clipPath(form) {
            // Wanderndes Licht: zwei Flecken auf gegenläufigen Ellipsenbahnen, so dass die ganze Fläche lebendig leuchtet.
            translate(size.width * (0.5f + 0.38f * kotlin.math.cos(t)), size.height * (0.5f + 0.42f * kotlin.math.sin(t))) {
                drawCircle(fleckA, gross * 0.7f, Offset.Zero)
            }
            translate(size.width * (0.5f - 0.40f * kotlin.math.cos(t + 1.2f)), size.height * (0.5f - 0.38f * kotlin.math.sin(t + 1.2f))) {
                drawCircle(fleckB, gross * 0.5f, Offset.Zero)
            }
            // Innerer Schimmer entlang des Rands (nur die innere Hälfte des Strichs ist sichtbar).
            drawPath(form, schimmer, style = schimmerStrich)
            // Umlaufender Lichtrand: Ring zwischen Form und eingerückter Form, darin ein drehender Verlauf.
            clipPath(innen, ClipOp.Difference) {
                translate(mitte.x, mitte.y) { rotate(drehung(), Offset.Zero) { drawCircle(lauf, diagonale, Offset.Zero) } }
            }
        }
    }
}

@Composable
fun LeerHinweis(icon: ImageVector, text: String) {
    val f = LocalFarben.current
    Row(
        Modifier.fillMaxWidth().glas(f, radius = f.radius * 0.6f, erhoeht = 0f, fuellung = f.flaeche.copy(alpha = f.flaeche.alpha * 0.5f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = f.textSchwach, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
fun Abstand(h: Int) = Spacer(Modifier.height(h.dp))
