package de.frank.epigenetik.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.epigenetik.data.Schalterkatalog
import de.frank.epigenetik.ui.theme.LocalBewegung
import de.frank.epigenetik.ui.theme.LocalFarben
import de.frank.epigenetik.ui.theme.antippen
import de.frank.epigenetik.ui.theme.glas
import de.frank.epigenetik.ui.theme.knopf3d

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

/** Glaskarte mit Überschrift. */
@Composable
fun Block(titel: String, modifier: Modifier = Modifier, icon: ImageVector? = null, rechts: (@Composable () -> Unit)? = null, inhalt: @Composable () -> Unit) {
    val f = LocalFarben.current
    Column(modifier.fillMaxWidth().einblenden(60).glas(f, erhoeht = 1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (titel.isNotBlank() || rechts != null) Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = f.primaer, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
            Text(titel.uppercase(), color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, modifier = Modifier.weight(1f))
            rechts?.invoke()
        }
        inhalt()
    }
}

@Composable
fun ChipReihe(inhalt: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { inhalt() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFluss(inhalt: @Composable () -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { inhalt() }
}

/** Runder Glas-Knopf. */
@Composable
fun RundKnopf(icon: ImageVector, beschreibung: String, groesse: Dp = 44.dp, aktiv: Boolean = false, aktion: () -> Unit) {
    val f = LocalFarben.current
    val grund = if (aktiv) Modifier.knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel) else Modifier.glas(f, 99.dp, 1.1f)
    Box(Modifier.size(groesse).then(grund).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
        Icon(icon, beschreibung, tint = if (aktiv) Color.White else f.text, modifier = Modifier.size(groesse * 0.48f))
    }
}

/** Großer plastischer Hauptknopf. */
@Composable
fun HauptKnopf(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier, farbe: Color? = null, aktion: () -> Unit) {
    val f = LocalFarben.current
    val c = farbe ?: f.primaer
    Row(
        modifier.height(50.dp).knopf3d(c, if (farbe == null) f.tertiaer else c.copy(alpha = 0.8f), 99.dp, f.dunkel).antippen(aktion = aktion).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Kleiner Glas-Knopf mit Symbol und Text. */
@Composable
fun GlasKnopf(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier, aktiv: Boolean = false, farbe: Color? = null, aktion: () -> Unit) {
    val f = LocalFarben.current
    val c = farbe ?: f.primaer
    val grund = if (aktiv) Modifier.knopf3d(c, c.copy(alpha = 0.8f), 99.dp, f.dunkel) else Modifier.glas(f, 99.dp, 0.7f, f.flaecheStark)
    Row(modifier.height(40.dp).then(grund).antippen(aktion = aktion).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Icon(icon, null, tint = if (aktiv) Color.White else c, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, color = if (aktiv) Color.White else f.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Ring mit dem epigenetischen Potenzial 0–100. */
@Composable
fun PotenzialRing(wert: Int, groesse: Dp = 52.dp, staerke: Dp = 5.dp) {
    val f = LocalFarben.current
    val ziel by animateFloatAsState(wert / 100f, tween(900), label = "ring")
    Box(Modifier.size(groesse), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(groesse)) {
            val s = staerke.toPx()
            val r = Size(size.width - s, size.height - s)
            val o = Offset(s / 2, s / 2)
            drawArc(f.textSchwach.copy(alpha = 0.22f), 0f, 360f, false, o, r, style = Stroke(s))
            drawArc(Brush.sweepGradient(listOf(f.tertiaer, f.primaer, f.sekundaer, f.tertiaer)), -90f, 360f * ziel, false, o, r, style = Stroke(s, cap = StrokeCap.Round))
        }
        Text(wert.toString(), color = f.text, fontSize = (groesse.value * 0.32f).sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Waagrechter Stärkebalken 0–100. */
@Composable
fun StaerkeBalken(wert: Int, farbe: Color, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val ziel by animateFloatAsState(wert / 100f, tween(700), label = "balken")
    Canvas(modifier.height(6.dp)) {
        val r = size.height / 2
        drawRoundRect(f.textSchwach.copy(alpha = 0.2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
        drawRoundRect(Brush.horizontalGradient(listOf(farbe.copy(alpha = 0.6f), farbe)), size = Size(size.width * ziel, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
    }
}

/** Unbestimmter, fließender Fortschritt für laufende Recherchen. */
@Composable
fun LaufBalken(modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    val t = rememberInfiniteTransition(label = "lauf")
    val x by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "x")
    Canvas(modifier.fillMaxWidth().height(5.dp)) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        drawRoundRect(f.textSchwach.copy(alpha = 0.18f), cornerRadius = r)
        val breite = size.width * 0.35f
        val start = (size.width + breite) * x - breite
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color.Transparent, f.primaer, f.sekundaer, Color.Transparent), startX = start, endX = start + breite),
            topLeft = Offset(start.coerceAtLeast(0f), 0f), size = Size(breite.coerceAtMost(size.width - start.coerceAtLeast(0f)), size.height), cornerRadius = r,
        )
    }
}

/** Pfeil für eine Wirkungsrichtung, grün wenn günstig, rot wenn ungünstig. */
@Composable
fun RichtungsPfeil(schalterId: String, richtung: String, groesse: Dp = 18.dp) {
    val f = LocalFarben.current
    val s = Schalterkatalog.zu(schalterId)
    val guenstig = s?.let { Schalterkatalog.guenstig(it, richtung) }
    val farbe = when (guenstig) { true -> f.erfolg; false -> f.gefahr; null -> f.textLeise }
    val r = richtung.lowercase()
    val icon = when {
        r.startsWith("aktiv") || r.startsWith("steig") || r.startsWith("erh") -> Icons.Rounded.ArrowUpward
        r.startsWith("hemm") || r.startsWith("senk") -> Icons.Rounded.ArrowDownward
        else -> Icons.Rounded.SwapVert
    }
    Box(Modifier.size(groesse + 10.dp).glas(f, 99.dp, 0f, farbe.copy(alpha = 0.16f), rand = false), contentAlignment = Alignment.Center) {
        Icon(icon, richtung, tint = farbe, modifier = Modifier.size(groesse))
    }
}

/** Kleines Etikett (Evidenz, Kategorie). */
@Composable
fun Etikett(text: String, farbe: Color? = null) {
    val f = LocalFarben.current
    val c = farbe ?: f.textLeise
    Text(
        text, color = c, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        modifier = Modifier.glas(f, 99.dp, 0f, c.copy(alpha = 0.13f), rand = false).padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

@Composable
fun Ueberschrift(text: String, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    Text(text, color = f.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = modifier.padding(start = 4.dp, top = 6.dp))
}

@Composable
fun Absatz(text: String, leise: Boolean = false, groesse: Int = 15) {
    val f = LocalFarben.current
    Text(text, color = if (leise) f.textLeise else f.text, fontSize = groesse.sp, lineHeight = (groesse * 1.42f).sp)
}

/** Aufzählungspunkt. */
@Composable
fun Punkt(text: String, farbe: Color? = null) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.padding(top = 8.dp, end = 10.dp).size(6.dp).glas(f, 99.dp, 0f, farbe ?: f.primaer, rand = false))
        Text(text, color = f.text, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun LeerHinweis(emoji: String, titel: String, text: String) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().einblenden(80).glas(f, erhoeht = 0.6f).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(emoji, fontSize = 34.sp)
        Text(titel, color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text, color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun Schalter(text: String, an: Boolean, unter: String = "", aendern: (Boolean) -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().antippen(haptik = false) { aendern(!an) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(text, color = f.text, fontSize = 14.sp)
            if (unter.isNotBlank()) Text(unter, color = f.textLeise, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(an, aendern, colors = SwitchDefaults.colors(checkedTrackColor = f.primaer))
    }
}

@Composable
fun Feld(titel: String, wert: String, geheim: Boolean = false, einzeilig: Boolean = true, hinweis: String = "", aendern: (String) -> Unit) {
    val f = LocalFarben.current
    var text by remember { mutableStateOf(wert) }
    var zeigen by remember { mutableStateOf(!geheim) }
    Column {
        if (titel.isNotBlank()) Text(titel, color = f.textLeise, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().padding(top = 4.dp).glas(f, 12.dp, 0f, f.flaecheStark).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (text.isEmpty() && hinweis.isNotBlank()) Text(hinweis, color = f.textSchwach, fontSize = 15.sp)
                BasicTextField(
                    text, { text = it; aendern(it) }, singleLine = einzeilig,
                    textStyle = TextStyle(color = f.text, fontSize = 15.sp, fontFamily = if (geheim) FontFamily.Monospace else FontFamily.Default),
                    cursorBrush = SolidColor(f.primaer),
                    visualTransformation = if (zeigen) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (geheim) Text(if (zeigen) "verbergen" else "zeigen", color = f.primaer, fontSize = 12.sp, modifier = Modifier.antippen(haptik = false) { zeigen = !zeigen }.padding(start = 8.dp))
        }
    }
}

/** Kompakte Zeile eines Mittels in Listen. */
@Composable
fun MittelZeile(
    emoji: String,
    name: String,
    unter: String,
    potenzial: Int?,
    modifier: Modifier = Modifier,
    rechts: (@Composable () -> Unit)? = null,
    aktion: () -> Unit,
) {
    val f = LocalFarben.current
    Row(
        modifier.fillMaxWidth().glas(f, 18.dp, 0.6f, f.flaecheStark).antippen(aktion = aktion).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 22.sp)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(name, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (unter.isNotBlank()) Text(unter, color = f.textLeise, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (potenzial != null && potenzial > 0) PotenzialRing(potenzial, 38.dp, 3.5.dp)
        rechts?.invoke()
    }
}
