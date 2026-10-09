package de.frank.jarvis.ui

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.tunnel.Tunnel
import de.frank.jarvis.ui.theme.JarvisTheme
import de.frank.jarvis.ui.theme.LocalBewegung
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import de.frank.jarvis.ui.theme.knopf3d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun JarvisApp(vm: AppViewModel, activity: ComponentActivity) {
    JarvisTheme(vm.einstellungen.modus) {
        val f = LocalFarben.current
        val tunnel by Tunnel.zustand.collectAsState()
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(f.hgOben, f.hgUnten)))) {
            Hintergrund()
            // Auf dem aufgeklappten Fold bleibt der Inhalt eine gut lesbare Spalte in der Mitte.
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
                    .widthIn(max = 640.dp).align(Alignment.TopCenter),
            ) {
                if (vm.gesperrt) {
                    Column(Modifier.weight(1f).fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Kern(120.dp, f.primaer, aktiv = false)
                        Text("Jarvis ist gesperrt", color = f.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
                        Text("Im Hintergrund arbeitet Jarvis weiter und bleibt für ChatGPT erreichbar.", color = f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 22.dp))
                        Knopf("Entsperren") { vm.entsperrenAnfragen() }
                    }
                    return@Column
                }
                vm.merkOffen?.let { liste ->
                    // Vollbild: ohne die Reiter-Leiste, zurück mit dem Pfeil oder der Zurück-Geste.
                    MerkBildschirm(vm, liste, Modifier.weight(1f))
                    return@Column
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(vm.reiter, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "reiter") { reiter ->
                        when (reiter) {
                            Reiter.JARVIS -> StartBildschirm(vm, tunnel, activity)
                            Reiter.ABLAGE -> AblageBildschirm(vm, activity)
                            Reiter.AKTIVITAET -> AktivitaetBildschirm(vm)
                            Reiter.EINSTELLUNGEN -> EinstellungenBildschirm(vm, tunnel, activity)
                        }
                    }
                }
                Leiste(vm)
            }
            Meldung(vm, Modifier.align(Alignment.TopCenter).statusBarsPadding())
        }
    }
}

/** Ruhig treibende Lichtflecken hinter dem Glas. */
@Composable
private fun Hintergrund() {
    val f = LocalFarben.current
    val bewegung = LocalBewegung.current
    val lauf = rememberInfiniteTransition(label = "hg")
    val t by lauf.animateFloat(0f, 1f, infiniteRepeatable(tween(46_000, easing = LinearEasing)), label = "t")
    val phase = if (bewegung) t else 0.2f
    Canvas(Modifier.fillMaxSize()) {
        val w = (phase * 2 * PI).toFloat()
        fun fleck(farbe: Color, x: Float, y: Float, r: Float, alpha: Float) {
            val mitte = Offset(size.width * x, size.height * y)
            drawCircle(Brush.radialGradient(listOf(farbe.copy(alpha = alpha), Color.Transparent), mitte, size.minDimension * r), size.minDimension * r, mitte)
        }
        val staerke = if (f.dunkel) 0.34f else 0.55f
        fleck(f.blob1, 0.18f + 0.06f * cos(w), 0.12f + 0.04f * sin(w), 0.85f, staerke)
        fleck(f.blob2, 0.88f + 0.05f * sin(w), 0.42f + 0.06f * cos(w), 0.70f, staerke * 0.7f)
        fleck(f.blob3, 0.30f + 0.07f * sin(w + 1.3f), 0.92f + 0.03f * cos(w), 0.95f, staerke)
    }
}

@Composable
private fun Leiste(vm: AppViewModel) {
    val f = LocalFarben.current
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth().height(62.dp).glas(f, 31.dp, erhoeht = 1.2f, fuellung = f.flaecheStark).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Reiter.entries.forEach { r ->
            val aktiv = vm.reiter == r
            val icon = when (r) { Reiter.JARVIS -> Icons.Rounded.AutoAwesome; Reiter.ABLAGE -> Icons.Rounded.Folder; Reiter.AKTIVITAET -> Icons.Rounded.History; Reiter.EINSTELLUNGEN -> Icons.Rounded.Tune }
            val grund = if (aktiv) Modifier.knopf3d(f.primaer, f.tertiaer, 25.dp, f.dunkel) else Modifier
            Row(
                Modifier.weight(if (aktiv) 1.9f else 1f).height(50.dp).then(grund).antippen { vm.reiter = r },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = if (aktiv) f.aufPrimaer else f.textLeise, modifier = Modifier.size(20.dp))
                if (aktiv) Text(r.anzeige, Modifier.padding(start = 7.dp), color = f.aufPrimaer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Kurze Rückmeldung oben, verschwindet von selbst. */
@Composable
private fun Meldung(vm: AppViewModel, modifier: Modifier) {
    val f = LocalFarben.current
    val text = vm.meldung
    LaunchedEffect(text) {
        if (text != null) { delay(2_800); if (vm.meldung == text) vm.meldung = null }
    }
    AnimatedVisibility(text != null, modifier, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
        Text(
            text.orEmpty(),
            Modifier.padding(top = 10.dp, start = 24.dp, end = 24.dp).glas(f, 18.dp, erhoeht = 1.4f, fuellung = if (f.dunkel) Color(0xF2162538) else Color(0xF7FFFFFF)).padding(horizontal = 18.dp, vertical = 12.dp),
            color = f.text, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        )
    }
}

// ---------------------------------------------------------------- Bausteine

/** Der Leuchtkern: zwei gegenläufige Ringe um einen pulsierenden Mittelpunkt. [aktiv] = schneller und heller. */
@Composable
fun Kern(groesse: Dp, farbe: Color, aktiv: Boolean, modifier: Modifier = Modifier) {
    val bewegung = LocalBewegung.current
    val lauf = rememberInfiniteTransition(label = "kern")
    val dreh by lauf.animateFloat(0f, 360f, infiniteRepeatable(tween(if (aktiv) 2_600 else 9_000, easing = LinearEasing)), label = "dreh")
    val puls by lauf.animateFloat(0.78f, 1f, infiniteRepeatable(tween(if (aktiv) 620 else 1_900), RepeatMode.Reverse), label = "puls")
    val winkel = if (bewegung) dreh else 30f
    val p = if (bewegung) puls else 0.9f
    Canvas(modifier.size(groesse)) {
        val r = size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(farbe.copy(alpha = 0.34f * p), Color.Transparent), center, r), r)
        val strich = r * 0.085f
        val aussen = r * 0.74f
        drawCircle(farbe.copy(alpha = 0.22f), aussen, style = Stroke(strich * 0.5f))
        for (i in 0 until 3) {
            drawArc(farbe.copy(alpha = 0.95f), winkel + i * 120f, 62f, false, Offset(center.x - aussen, center.y - aussen), androidx.compose.ui.geometry.Size(aussen * 2, aussen * 2), style = Stroke(strich, cap = StrokeCap.Round))
        }
        val innen = r * 0.50f
        for (i in 0 until 6) {
            drawArc(farbe.copy(alpha = 0.6f), -winkel * 1.6f + i * 60f, 26f, false, Offset(center.x - innen, center.y - innen), androidx.compose.ui.geometry.Size(innen * 2, innen * 2), style = Stroke(strich * 0.8f, cap = StrokeCap.Round))
        }
        drawCircle(Brush.radialGradient(listOf(Color.White, farbe, farbe.copy(alpha = 0f)), center, r * 0.34f * p), r * 0.34f * p)
    }
}

@Composable
fun Karte(modifier: Modifier = Modifier, toenung: Color? = null, inhalt: @Composable ColumnScope.() -> Unit) {
    val f = LocalFarben.current
    Column(modifier.fillMaxWidth().glas(f, toenung = toenung).padding(18.dp), content = inhalt)
}

@Composable
fun Abschnitt(titel: String, modifier: Modifier = Modifier) {
    Text(titel.uppercase(), modifier.padding(start = 6.dp, top = 6.dp, bottom = 2.dp), color = LocalFarben.current.textSchwach, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
}

/** Hauptknopf (plastisch, gefüllt) oder Nebenknopf (Glas). */
@Composable
fun Knopf(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, haupt: Boolean = true, farbe: Color? = null, aktion: () -> Unit) {
    val f = LocalFarben.current
    val c = farbe ?: f.primaer
    val grund = if (haupt) Modifier.knopf3d(c, if (farbe == null) f.tertiaer else c.copy(alpha = 0.8f), 16.dp, f.dunkel) else Modifier.glas(f, 16.dp, erhoeht = 0.5f)
    Row(
        modifier.height(48.dp).then(grund).antippen(aktion = aktion).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = if (haupt) f.aufPrimaer else c, modifier = Modifier.padding(end = 8.dp).size(18.dp))
        Text(text, color = if (haupt) f.aufPrimaer else f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Eingabefeld. Mit [geheim] ist der Inhalt verdeckt; das Auge rechts zeigt ihn. */
@Composable
fun Eingabe(
    wert: String,
    beiAenderung: (String) -> Unit,
    platzhalter: String,
    modifier: Modifier = Modifier,
    geheim: Boolean = false,
    einzeilig: Boolean = true,
    senden: (() -> Unit)? = null,
) {
    val f = LocalFarben.current
    var sichtbar by remember { mutableStateOf(false) }
    Row(modifier.glas(f, 16.dp, erhoeht = 0.3f, fuellung = f.flaecheStark).padding(start = 16.dp, end = if (geheim) 6.dp else 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
            if (wert.isEmpty()) Text(platzhalter, color = f.textSchwach, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicTextField(
                wert, beiAenderung, Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = f.text, fontSize = 16.sp),
                cursorBrush = SolidColor(f.primaer),
                singleLine = einzeilig,
                maxLines = if (einzeilig) 1 else 5,
                visualTransformation = if (geheim && !sichtbar && wert.isNotEmpty()) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(imeAction = if (senden != null) ImeAction.Send else ImeAction.Default),
                keyboardActions = KeyboardActions(onSend = { senden?.invoke() }),
            )
        }
        if (geheim) Box(Modifier.size(40.dp).antippen(haptik = false) { sichtbar = !sichtbar }, contentAlignment = Alignment.Center) {
            Icon(if (sichtbar) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (sichtbar) "Verbergen" else "Anzeigen", tint = f.textLeise, modifier = Modifier.size(20.dp))
        }
    }
}

/** Uhrzeit oder Datum eines Protokolleintrags, kurz. */
fun zeitKurz(millis: Long): String {
    val zeit = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault())
    val heute = java.time.LocalDate.now()
    val muster = if (zeit.toLocalDate() == heute) "HH:mm" else "d. MMM, HH:mm"
    return zeit.format(java.time.format.DateTimeFormatter.ofPattern(muster, java.util.Locale.GERMAN))
}
