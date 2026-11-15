package de.frank.jarvis.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.frank.jarvis.atem.AtemFarben
import de.frank.jarvis.atem.AtemPhase
import de.frank.jarvis.atem.AtemSitzung
import de.frank.jarvis.atem.dauerText
import de.frank.jarvis.ui.theme.antippen
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val P_EIN = Color(AtemFarben.EIN)
private val P_HALT = Color(AtemFarben.HALTEN)
private val P_AUS = Color(AtemFarben.AUS)
private val P_RUHE = Color(0xFF7FB8FF)
/** Kleinste Blase nach dem Ausatmen: fast verschwunden, damit Ein- und Ausatmen deutlich zu sehen sind. */
private const val KLEIN = 0.07f

private fun phasenFarbe(p: AtemPhase?): Color = when (p) {
    AtemPhase.EIN -> P_EIN
    AtemPhase.HALT_OBEN, AtemPhase.HALT_UNTEN -> P_HALT
    AtemPhase.AUS -> P_AUS
    else -> P_RUHE
}

private fun weich(x: Float): Float = (0.5f - 0.5f * cos(PI.toFloat() * x.coerceIn(0f, 1f)))

/** Teilchen um die Blase: fester Zufall, damit sie nicht springen. */
private class Teilchen(val winkel: Float, val abstand: Float, val tempo: Float, val groesse: Float, val phase: Float)
private val TEILCHEN = Random(7).let { r -> List(54) { Teilchen(r.nextFloat() * 6.283f, 1.12f + r.nextFloat() * 0.95f, (r.nextFloat() - 0.5f) * 0.00022f, 1.2f + r.nextFloat() * 2.6f, r.nextFloat() * 6.283f) } }

/**
 * Vollbild der laufenden Atemübung: eine Blase, die beim Einatmen (grün) wächst, beim Halten (gelb) steht und leise bebt
 * und beim Ausatmen (orange) wieder schrumpft; Blütenblätter, Lichtkranz, Teilchen und ein Fortschrittsring je Phase.
 * Alles hängt an der einen Übungsuhr; gelesen wird sie erst beim Zeichnen.
 */
@Composable
fun AtemSitzungBildschirm(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val profil = AtemSitzung.profil ?: return
    val akzent = Color(profil.farbe)
    var t by remember { mutableLongStateOf(0L) }
    var rahmen by remember { mutableLongStateOf(0L) }
    // Bilder nur, solange sich etwas bewegt: In der Pause und nach dem Ende genügt ein langsamer Takt.
    LaunchedEffect(Unit) {
        while (true) {
            val ruhig = AtemSitzung.pausiert || (AtemSitzung.gestartet && AtemSitzung.plan.isNotEmpty() && AtemSitzung.zeit() >= AtemSitzung.gesamt)
            if (ruhig) { kotlinx.coroutines.delay(250); t = AtemSitzung.zeit() } else withFrameMillis { rahmen = it; t = AtemSitzung.zeit() }
        }
    }
    var endeFrage by remember { mutableStateOf(false) }
    // War die Übung vor der Frage schon pausiert (von Hand oder durch einen Anruf), bleibt sie es danach auch.
    var warPausiert by remember { mutableStateOf(false) }
    fun frageOeffnen() { warPausiert = AtemSitzung.pausiert; AtemSitzung.pause(); endeFrage = true }
    fun frageSchliessen() { endeFrage = false; if (!warPausiert) AtemSitzung.weiter() }

    // Bildschirm bleibt an, Leisten verschwinden: echtes Vollbild.
    DisposableEffect(Unit) {
        val vorher = view.keepScreenOn
        view.keepScreenOn = true
        val fenster = (context as? Activity)?.window
        val steuer = fenster?.let { WindowCompat.getInsetsController(it, view) }
        steuer?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        steuer?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { view.keepScreenOn = vorher; steuer?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    val gestartet = AtemSitzung.gestartet
    // Den Zustand im derivedStateOf selbst lesen, nicht die Momentaufnahme der ersten Zusammensetzung.
    val ende by remember { derivedStateOf { AtemSitzung.gestartet && AtemSitzung.plan.isNotEmpty() && t >= AtemSitzung.gesamt } }
    // Nach dem Ende darf der Bildschirm wieder ausgehen.
    // Nach dem Ende darf der Bildschirm ausgehen; „Nochmal“ schaltet die Wachhaltung wieder ein.
    LaunchedEffect(ende) { view.keepScreenOn = !ende }
    BackHandler { if (!gestartet || ende) AtemSitzung.beenden() else frageOeffnen() }

    if (endeFrage) AlertDialog(
        onDismissRequest = { frageSchliessen() },
        title = { Text("Übung beenden?") },
        text = { Text("Die Atemübung ist pausiert.") },
        confirmButton = { TextButton({ endeFrage = false; AtemSitzung.beenden() }) { Text("Beenden") } },
        dismissButton = { TextButton({ frageSchliessen() }) { Text(if (warPausiert) "Zurück" else "Weiter atmen") } },
    )

    Box(modifier.background(Brush.verticalGradient(listOf(Color(0xFF060912), Color(0xFF0A1220), Color(0xFF070B14))))) {
        // Kopf, Animation und Bedienung untereinander: Die Animation bekommt nur den Platz dazwischen,
        // so liegen Schalter und Knöpfe nie im Kreis.
        Column(Modifier.fillMaxSize()) {
            // ------------------------------------------------ Kopf
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Rund(Icons.Rounded.Close, "Übung beenden") { if (!gestartet || ende) AtemSitzung.beenden() else frageOeffnen() }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(profil.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(akzent))
                        Text(" ${profil.zyklen} Zyklen · ${dauerText(profil.dauerMs)} min", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                // ------------------------------------------------ Animation
                Canvas(Modifier.fillMaxSize().graphicsLayer()) { zeichne(t, rahmen, akzent) }
                // ------------------------------------------------ Mitte: Phase und Sekunden
                val anzeige by remember { derivedStateOf { mitteText(t, AtemSitzung.gestartet) } }
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(anzeige.first, color = Color.White, fontSize = if (anzeige.first.length > 12) 24.sp else 34.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
                    if (anzeige.second.isNotEmpty()) Text(anzeige.second, color = Color.White.copy(alpha = 0.78f), fontSize = 22.sp, fontWeight = FontWeight.Light)
                }
            }
            // ------------------------------------------------ Unten: Einstellungen, Satz, Fortschritt, Pause
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Schalter direkt unter der Animation, mit dem Daumen gut erreichbar.
                Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Haken("Vorlesen", Icons.Rounded.RecordVoiceOver, AtemSitzung.vorlesen) { AtemSitzung.vorlesenUmschalten() }
                    Haken("Atem", Icons.Rounded.Air, AtemSitzung.geraeusch) { AtemSitzung.geraeuschUmschalten() }
                }
                // Lautstärke des Atemgeräuschs: links leise, rechts laut; gilt sofort und bleibt gespeichert.
                Row(Modifier.fillMaxWidth().widthIn(max = 420.dp).padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeDown, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    androidx.compose.material3.Slider(
                        AtemSitzung.pegel, { AtemSitzung.pegelSetzen(it, speichern = false) },
                        Modifier.weight(1f).padding(horizontal = 8.dp).semantics { contentDescription = "Lautstärke des Atemgeräuschs" },
                        onValueChangeFinished = { AtemSitzung.pegelSetzen(AtemSitzung.pegel) },
                        colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = akzent, inactiveTrackColor = Color.White.copy(alpha = 0.18f)),
                    )
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
                val satz = AtemSitzung.satz
                Box(Modifier.heightIn(min = 64.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    var letzter by remember { mutableStateOf("") }
                    if (satz != null) letzter = satz
                    val sicht by androidx.compose.animation.core.animateFloatAsState(if (satz != null) 1f else 0f, tween(if (satz != null) 700 else 900), label = "satz")
                    if (letzter.isNotEmpty()) Text("„$letzter“", Modifier.graphicsLayer { alpha = sicht }, color = Color.White.copy(alpha = 0.9f), fontSize = 19.sp, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center, lineHeight = 26.sp)
                }
                AtemSitzung.hinweis?.let { Text(it, color = P_HALT.copy(alpha = 0.85f), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp)) }
                val vorb = AtemSitzung.vorbereitung
                when {
                    vorb != null -> {
                        Text("Stimme wird vorbereitet … ${vorb.first} von ${vorb.second}", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        Fortschritt(if (vorb.second > 0) vorb.first / vorb.second.toFloat() else 0f, akzent, Modifier.padding(top = 10.dp))
                    }
                    ende -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Knopf("Nochmal", icon = Icons.Rounded.Refresh, haupt = false) { AtemSitzung.starten(context, profil) }
                        Knopf("Schließen", farbe = akzent) { AtemSitzung.beenden() }
                    }
                    else -> {
                        val info by remember { derivedStateOf { untenText(t) } }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(info.first, Modifier.weight(1f), color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                            Text(info.second, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        }
                        val anteil by remember { derivedStateOf { gesamtAnteil(t) } }
                        Fortschritt(anteil, akzent, Modifier.padding(top = 8.dp, bottom = 16.dp))
                        val pausiert = AtemSitzung.pausiert
                        Box(
                            Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                                .antippen { if (pausiert) AtemSitzung.weiter() else AtemSitzung.pause() }.semantics { contentDescription = if (pausiert) "Weiter" else "Pause" },
                            contentAlignment = Alignment.Center,
                        ) { Icon(if (pausiert) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, tint = Color.White, modifier = Modifier.size(32.dp)) }
                    }
                }
            }
        }
    }
}

private fun mitteText(t: Long, gestartet: Boolean): Pair<String, String> {
    if (!gestartet) return "Gleich geht es los" to ""
    val a = AtemSitzung.abschnitt(t) ?: return "Gut gemacht" to ""
    return when (a.phase) {
        AtemPhase.INTRO -> "Mach es dir bequem" to ""
        AtemPhase.OUTRO -> "Nachspüren" to ""
        else -> a.phase.anzeige to ceil((a.ende - t) / 1000.0).toInt().coerceAtLeast(1).toString()
    }
}

private fun untenText(t: Long): Pair<String, String> {
    val a = AtemSitzung.abschnitt(t)
    val z = AtemSitzung.profil?.zyklen ?: 0
    val links = when {
        a == null -> ""
        a.zyklus > 0 -> "Zyklus ${a.zyklus} von $z"
        a.phase == AtemPhase.INTRO -> "Ankommen"
        else -> "Ausklang"
    }
    val rest = (AtemSitzung.uebungEnde - t).coerceAtLeast(0)
    return links to "noch ${dauerText(rest)}"
}

private fun gesamtAnteil(t: Long): Float {
    val s = AtemSitzung.uebungStart; val e = AtemSitzung.uebungEnde
    return if (e <= s) 0f else ((t - s).toFloat() / (e - s)).coerceIn(0f, 1f)
}

/** Größe der Blase (Anteil vom Höchstmaß) und Phasenfarbe mit weichem Übergang. */
private fun zustand(t: Long): Triple<Float, Color, Float> {
    val plan = AtemSitzung.plan
    val i = AtemSitzung.abschnittIndex(t)
    val a = plan.getOrNull(i)
    if (!AtemSitzung.gestartet || a == null) {
        val ende = AtemSitzung.gestartet && plan.isNotEmpty()
        return Triple(if (ende) 0.6f else 0.5f, P_RUHE, 0f)
    }
    val x = ((t - a.start).toFloat() / a.dauer.coerceAtLeast(1)).coerceIn(0f, 1f)
    val vorher = plan.getOrNull(i - 1)?.phase
    val farbe = lerp(phasenFarbe(vorher ?: a.phase), phasenFarbe(a.phase), weich(((t - a.start) / 800f).coerceIn(0f, 1f)))
    val beben = (sin(t * 0.0459f) * 0.6f + sin(t * 0.0697f) * 0.4f) * 0.011f
    val groesse = when (a.phase) {
        AtemPhase.EIN -> KLEIN + (1f - KLEIN) * weich(x)
        AtemPhase.HALT_OBEN -> 1f + beben
        AtemPhase.AUS -> 1f - (1f - KLEIN) * weich(x)
        AtemPhase.HALT_UNTEN -> KLEIN + beben * 0.3f
        AtemPhase.INTRO -> 0.5f + 0.035f * sin(t / 1400f)
        AtemPhase.OUTRO -> {
            // Vom letzten Stand sanft in eine ruhige Mitte.
            val von = when (vorher) { AtemPhase.EIN, AtemPhase.HALT_OBEN -> 1f; else -> KLEIN }
            val ziel = 0.6f + 0.03f * sin(t / 1600f)
            von + (ziel - von) * weich((t - a.start) / 3000f)
        }
        AtemPhase.ENDE -> 0.6f
    }
    val fortschritt = if (a.phase == AtemPhase.INTRO || a.phase == AtemPhase.OUTRO) 0f else x
    return Triple(groesse, farbe, fortschritt)
}

private fun DrawScope.zeichne(t: Long, rahmen: Long, akzent: Color) {
    val (s, farbe, fortschritt) = zustand(t)
    val mitte = Offset(size.width / 2f, size.height * 0.5f)
    // Ring (1,18 × Höchstmaß) passt mit Rand in die Fläche zwischen Kopf und Bedienung.
    val maxR = size.minDimension * 0.38f
    val r = maxR * s
    val drehung = rahmen * 0.000045f + s * 0.9f

    // Lichthof
    drawCircle(Brush.radialGradient(listOf(farbe.copy(alpha = 0.34f), farbe.copy(alpha = 0.10f), Color.Transparent), mitte, r * 2.4f), r * 2.4f, mitte)

    // Teilchen: kreisen langsam, folgen der Atmung mit Abstand.
    TEILCHEN.forEach { p ->
        val w = p.winkel + rahmen * p.tempo
        val d = maxR * 0.12f + r * p.abstand + maxR * 0.06f * sin(rahmen / 2400f + p.phase)
        val pos = Offset(mitte.x + cos(w) * d, mitte.y + sin(w) * d)
        val funkeln = 0.25f + 0.35f * (0.5f + 0.5f * sin(rahmen / 900f + p.phase * 3f))
        drawCircle(lerp(Color.White, farbe, 0.4f).copy(alpha = funkeln), p.groesse * density * 0.9f, pos)
    }

    // Blütenblätter: öffnen sich mit dem Einatmen.
    val blaetter = 7
    for (j in 0 until blaetter) {
        val w = drehung + j * (2 * PI.toFloat() / blaetter)
        val c = Offset(mitte.x + cos(w) * r * 0.42f, mitte.y + sin(w) * r * 0.42f)
        val br = r * 0.62f
        drawCircle(Brush.radialGradient(listOf(lerp(farbe, Color.White, 0.25f).copy(alpha = 0.30f), farbe.copy(alpha = 0.10f), Color.Transparent), c, br), br, c, blendMode = BlendMode.Plus)
    }

    // Kern mit Glanz oben links
    val kernR = r * 0.80f
    drawCircle(
        Brush.radialGradient(listOf(lerp(farbe, Color.White, 0.55f), farbe, lerp(farbe, Color(0xFF0A1220), 0.45f)), Offset(mitte.x - kernR * 0.28f, mitte.y - kernR * 0.32f), kernR * 1.35f),
        kernR, mitte, alpha = 0.92f,
    )
    drawCircle(Color.White.copy(alpha = 0.22f), kernR, mitte, style = Stroke(1.2f * density))
    drawCircle(akzent.copy(alpha = 0.18f), kernR * 1.04f, mitte, style = Stroke(2f * density))

    // Fortschrittsring der Phase
    val ringR = maxR * 1.18f
    val dick = 3.5f * density
    drawCircle(Color.White.copy(alpha = 0.07f), ringR, mitte, style = Stroke(dick))
    if (fortschritt > 0f) drawArc(
        farbe, -90f, 360f * fortschritt, false,
        topLeft = Offset(mitte.x - ringR, mitte.y - ringR), size = androidx.compose.ui.geometry.Size(ringR * 2, ringR * 2),
        style = Stroke(dick, cap = StrokeCap.Round),
    )
}

@Composable
private fun Fortschritt(anteil: Float, farbe: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.10f))) {
        Box(Modifier.fillMaxWidth(anteil.coerceIn(0f, 1f)).fillMaxHeight().background(farbe))
    }
}

@Composable
private fun Rund(icon: ImageVector, beschreibung: String, aktion: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).antippen(aktion = aktion).semantics { contentDescription = beschreibung }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/** Häkchen zum schnellen Umschalten während der Übung. */
@Composable
private fun Haken(titel: String, icon: ImageVector, an: Boolean, aktion: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = if (an) 0.14f else 0.06f)).antippen(aktion = aktion)
            .semantics { contentDescription = "$titel ${if (an) "an" else "aus"}" }.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (an) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank, null, tint = if (an) P_EIN else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Icon(icon, null, tint = Color.White.copy(alpha = if (an) 0.9f else 0.5f), modifier = Modifier.padding(start = 4.dp).size(16.dp))
        Text(titel, Modifier.padding(start = 4.dp), color = Color.White.copy(alpha = if (an) 0.9f else 0.55f), fontSize = 12.sp)
    }
}
