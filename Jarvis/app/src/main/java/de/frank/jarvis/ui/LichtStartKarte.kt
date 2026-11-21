package de.frank.jarvis.ui

import android.widget.Toast
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.licht.LedStreifen
import de.frank.jarvis.licht.LedStreifen.Modus
import de.frank.jarvis.licht.LichtAutomatik
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Stand der Licht-Karte außerhalb der Komposition. */
/** Zuletzt an den Reglern gewählt (auch ohne gemerkten Zustand), gilt beim Antippen einer Szene bzw. eines Mikrofon-Modus. */
private object Wahl { @Volatile var tempo = 50; @Volatile var empf = 7 }

private object LichtStand {
    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var zustand: LedStreifen.Zustand?
        get() = LedStreifen.aktuell
        set(v) { LedStreifen.aktuell = v }
    var offen by mutableStateOf(false)
    var farbeOffen by mutableStateOf(true)
    var szenenOffen by mutableStateOf(false)
    var mikroOffen by mutableStateOf(false)
    var automatikOffen by mutableStateOf(false)
    var plaetze by mutableStateOf<List<String>>(emptyList())

    fun tun(c: android.content.Context, arbeit: suspend () -> LedStreifen.Bericht) {
        if (LedStreifen.laeuft) { Toast.makeText(c, "Moment, das Licht wird gerade geschaltet", Toast.LENGTH_SHORT).show(); return }
        bereich.launch { runCatching { arbeit() }; zustand = LedStreifen.zustand(c) }
    }

    fun setzen(c: android.content.Context, z: LedStreifen.Zustand) {
        if (LedStreifen.laeuft) { Toast.makeText(c, "Moment, das Licht wird gerade geschaltet", Toast.LENGTH_SHORT).show(); return }
        zustand = z; tun(c) { LedStreifen.setzen(c, z) }
    }

    /** Während des Ziehens: sofort anzeigen und live an die verbundenen Streifen. */
    fun live(c: android.content.Context, z: LedStreifen.Zustand) { if (LedStreifen.laeuft) return; LedStreifen.liveStart(c); LedStreifen.live(z) }

    /** Loslassen: fest übernehmen (alle Streifen, Handy und Mac) und merken. */
    fun los(c: android.content.Context, z: LedStreifen.Zustand) { zustand = z; bereich.launch { runCatching { LedStreifen.liveEnde(c, z) }; zustand = LedStreifen.zustand(c) } }
}

private fun farbe(rgb: String): Color = runCatching { Color(0xFF000000 or rgb.toLong(16)) }.getOrDefault(Color(0xFFFF1400))

private fun hexAus(col: Color) = "%02X%02X%02X".format((col.red * 255).toInt(), (col.green * 255).toInt(), (col.blue * 255).toInt())

/** Ungefähre Anzeigefarbe eines Zustands (Szenen und Mikrofon als Andeutung). */
private fun anzeige(z: LedStreifen.Zustand?): Color = when (z?.modus) {
    null -> Color(0xFFFF1400)
    Modus.FARBE -> farbe(z.rgb)
    Modus.SZENE -> Color(0xFFB45CFF)
    Modus.RHYTHMUS -> Color(0xFF39D0FF)
}

/**
 * Startbereich „LED-Licht“ wie die Kaffee-Karte: zugeklappt nur An und Aus, der Pfeil öffnet die Untermenüs Farbe und Helligkeit (16 Farbplätze:
 * tippen = einstellen, lange drücken = aktuelle Farbe speichern; Farbkreis live; Farbcode), Szenen, Mikrofon-Modi und Automatik.
 * Alle Streifen bekommen immer denselben Zustand.
 */
@Composable
fun LichtStartKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    val s = LichtStand
    androidx.compose.runtime.LaunchedEffect(Unit) {
        LedStreifen.aktuellLaden(c)
        if (s.plaetze.isEmpty()) s.plaetze = LedStreifen.plaetze(c)
        if (LedStreifen.anAnzeige == null) LedStreifen.anAnzeige = LedStreifen.zuletztAn(c)
    }
    // Tablet: Es schaltet die Streifen selten selbst, deshalb zeigt die Karte den gelesenen Zustand (beim Öffnen, dann jede Minute, nur sichtbar).
    if (de.frank.jarvis.data.Geraet.istTablet) {
        val lebenszyklus = androidx.lifecycle.compose.LocalLifecycleOwner.current
        androidx.compose.runtime.LaunchedEffect(lebenszyklus) {
            lebenszyklus.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                while (true) { if (!LedStreifen.laeuft) runCatching { LedStreifen.anzeigeLesen(c) }; kotlinx.coroutines.delay(60_000) }
            }
        }
    }
    var minute by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(60_000); minute++ } }
    val z = s.zustand
    val b = LedStreifen.letzterBericht
    val frischerFehler = minute >= 0 && b?.fehler == true && System.currentTimeMillis() - b.zeit < 30 * 60_000L
    // 13: Nur die Titelzeile ist Griff zum Verschieben der Karte; Farbplätze, Farbkreis und Regler behalten ihre Gesten.
    Karte {
        // Kopf: Titel und Helligkeit, darunter das 3D-LED-Band.
        val an = if (de.frank.jarvis.data.Geraet.istTablet) LedStreifen.anAnzeige == true else LedStreifen.anAnzeige != false
        Row(griff.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("LED-Licht", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                when {
                    LedStreifen.laeuft -> "Schaltet …"
                    de.frank.jarvis.data.Geraet.istTablet && LedStreifen.anAnzeige == null && !frischerFehler -> "Lese Zustand …"
                    frischerFehler -> "Nicht erreicht"
                    !an -> "Aus"
                    z == null -> "An"
                    z.modus == Modus.FARBE -> "An · Helligkeit ${z.hell} %"
                    z.modus == Modus.SZENE -> "An · Szene"
                    else -> "An · Mikrofon"
                },
                color = when { LedStreifen.laeuft -> f.primaer; frischerFehler -> f.gefahr; else -> f.textLeise }, fontSize = 14.sp,
            )
        }
        b?.takeIf { frischerFehler }?.let { Text(it.text, color = f.gefahr, fontSize = 13.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 4.dp)) }
        LedBand3D(z, an, LedStreifen.laeuft, Modifier.padding(top = 4.dp).fillMaxWidth().height(130.dp))
        if (!LedStreifen.erlaubt(c)) Text("Jarvis fehlt das Recht „Geräte in der Nähe“ (App-Infos von Jarvis, Berechtigungen).", color = f.gefahr, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))

        // Hauptzeile: An, Aus und der Pfeil für alles Weitere.
        Row(
            Modifier.padding(top = 10.dp).fillMaxWidth().glas(f, 18.dp, erhoeht = 0.6f, toenung = anzeige(z)).padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LichtKnopf("An", Modifier.weight(1f), gross = true) { s.tun(c) { LedStreifen.an(c) } }
            LichtKnopf("Aus", Modifier.weight(1f), gross = true) { s.tun(c) { LedStreifen.aus(c) } }
            Box(Modifier.size(44.dp).antippen { s.offen = !s.offen }, contentAlignment = Alignment.Center) {
                Icon(if (s.offen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, if (s.offen) "Licht-Einstellungen zuklappen" else "Licht-Einstellungen zeigen", tint = f.text, modifier = Modifier.size(30.dp))
            }
        }

        AnimatedVisibility(s.offen) {
            Column {
                Aufklapper("Farbe und Helligkeit", s.farbeOffen) { s.farbeOffen = !s.farbeOffen }
                AnimatedVisibility(s.farbeOffen) { FarbeTeil() }
                Aufklapper("Szenen", s.szenenOffen) { s.szenenOffen = !s.szenenOffen }
                AnimatedVisibility(s.szenenOffen) { SzenenTeil() }
                Aufklapper("Mikrofon-Modi", s.mikroOffen) { s.mikroOffen = !s.mikroOffen }
                AnimatedVisibility(s.mikroOffen) { MikrofonTeil() }
                Aufklapper("Automatik", s.automatikOffen) { s.automatikOffen = !s.automatikOffen }
                AnimatedVisibility(s.automatikOffen) { AutomatikTeil() }
            }
        }
    }
}

@Composable
private fun FarbeTeil() {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    val s = LichtStand
    val z = s.zustand
    val haptik = LocalHapticFeedback.current
    val basis = z ?: LedStreifen.Zustand(rgb = "FF1400")
    Column(Modifier.padding(top = 6.dp)) {
        Text("Tippen stellt ein · lange drücken speichert die aktuelle Farbe auf dem Platz", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        s.plaetze.withIndex().chunked(8).forEach { reihe ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                reihe.forEach { (i, rgb) ->
                    val aktiv = z?.modus == Modus.FARBE && z.rgb.equals(rgb, true)
                    val aktBasis by androidx.compose.runtime.rememberUpdatedState(basis)
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(farbe(rgb))
                            .border(if (aktiv) 3.dp else 1.dp, if (aktiv) f.text else f.textSchwach.copy(alpha = 0.4f), CircleShape)
                            .semantics {
                                contentDescription = "Farbplatz ${i + 1}, ${LedStreifen.farbName(rgb)}" + if (aktiv) ", eingestellt" else ""
                                role = androidx.compose.ui.semantics.Role.Button
                                customActions = listOf(androidx.compose.ui.semantics.CustomAccessibilityAction("Aktuelle Farbe hier speichern") {
                                    val akt = s.zustand?.takeIf { it.modus == Modus.FARBE }?.rgb ?: return@CustomAccessibilityAction false
                                    LedStreifen.platzSpeichern(c, i, akt); s.plaetze = LedStreifen.plaetze(c); true
                                })
                            }
                            .pointerInput(rgb) {
                                detectTapGestures(
                                    onTap = { s.setzen(c, aktBasis.copy(modus = Modus.FARBE, rgb = rgb)) },
                                    onLongPress = {
                                        val aktuell = s.zustand?.takeIf { it.modus == Modus.FARBE }?.rgb
                                            ?: run { Toast.makeText(c, "Erst eine Farbe wählen, dann lange drücken zum Speichern", Toast.LENGTH_SHORT).show(); return@detectTapGestures }
                                        LedStreifen.platzSpeichern(c, i, aktuell)
                                        s.plaetze = LedStreifen.plaetze(c)
                                        haptik.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(c, "#$aktuell auf Platz ${i + 1} gespeichert", Toast.LENGTH_SHORT).show()
                                    },
                                )
                            },
                    )
                }
            }
        }

        // Farbkreis wie in KS Lite: Daumen ziehen = live, loslassen = übernehmen.
        Farbkreis(
            if (z?.modus == Modus.FARBE) z.rgb else basis.rgb,
            Modifier.padding(top = 4.dp).fillMaxWidth().aspectRatio(1f),
            ziehen = { rgb -> s.live(c, basis.copy(modus = Modus.FARBE, rgb = rgb)) },
            los = { rgb -> s.los(c, basis.copy(modus = Modus.FARBE, rgb = rgb)) },
        )

        // Farbcode direkt eingeben.
        var code by remember(z?.rgb) { mutableStateOf(z?.rgb ?: "") }
        val fokus = LocalFocusManager.current
        fun uebernehmen() {
            val rgb = LedStreifen.farbe(code) ?: run { Toast.makeText(c, "Bitte sechs Zeichen 0–9 und A–F, etwa FF1400", Toast.LENGTH_SHORT).show(); return }
            fokus.clearFocus()
            s.setzen(c, basis.copy(modus = Modus.FARBE, rgb = rgb))
        }
        Row(Modifier.padding(top = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("#", color = f.textLeise, fontSize = 18.sp)
            Box(Modifier.weight(1f).height(44.dp).glas(f, 12.dp, erhoeht = 0.4f).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    code, { code = it.uppercase().filter { ch -> ch in "0123456789ABCDEF" }.take(6) }, singleLine = true,
                    textStyle = TextStyle(color = f.text, fontSize = 17.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(f.primaer),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { uebernehmen() }),
                )
                if (code.isEmpty()) Text("Farbcode, z. B. FF1400", color = f.textSchwach, fontSize = 14.sp)
            }
            Box(Modifier.size(28.dp).clip(CircleShape).background(farbe(code.padEnd(6, '0'))))
            LichtKnopf("Übernehmen", Modifier.size(110.dp, 44.dp)) { uebernehmen() }
        }

        // Helligkeit, ebenfalls live.
        var hell by remember(z?.hell) { mutableFloatStateOf((z?.hell ?: 100).toFloat()) }
        Text("Helligkeit: ${hell.toInt()} %", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        Slider(
            value = hell, valueRange = 1f..100f,
            onValueChange = { hell = it; z?.takeIf { bz -> bz.modus != Modus.RHYTHMUS || LedStreifen.farbeGewaehlt(c) }?.let { bz -> s.live(c, bz.copy(modus = if (bz.modus == Modus.RHYTHMUS) Modus.FARBE else bz.modus, hell = it.toInt())) } },
            onValueChangeFinished = {
                val h = hell.toInt()
                if (z != null && z.modus == Modus.RHYTHMUS && !LedStreifen.farbeGewaehlt(c)) Toast.makeText(c, "Bitte zuerst eine Farbe wählen", Toast.LENGTH_SHORT).show()
                else if (z != null) s.los(c, z.copy(modus = if (z.modus == Modus.RHYTHMUS) Modus.FARBE else z.modus, hell = h))
                else s.bereich.launch {
                    // Ohne gemerkten Zustand: erst die Streifen lesen; ist keine Farbe bekannt, nichts Geratenes senden.
                    val b0 = LedStreifen.basis(c)
                    if (b0 == null) Toast.makeText(c, "Bitte zuerst eine Farbe wählen", Toast.LENGTH_SHORT).show()
                    else s.setzen(c, b0.copy(modus = if (b0.modus == Modus.RHYTHMUS) Modus.FARBE else b0.modus, hell = h))
                }
            },
        )
    }
}

@Composable
private fun SzenenTeil() {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    val s = LichtStand
    val z = s.zustand
    Column(Modifier.padding(top = 2.dp)) {
        LedStreifen.SZENEN.withIndex().chunked(2).forEach { reihe ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                reihe.forEach { (i, name) ->
                    LichtKnopf(name.replaceFirstChar { it.uppercase() }, Modifier.weight(1f), aktiv = z?.modus == Modus.SZENE && z.szene == i) {
                        s.setzen(c, (z ?: LedStreifen.Zustand()).copy(modus = Modus.SZENE, szene = i, tempo = Wahl.tempo))
                    }
                }
                if (reihe.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        var tempo by remember(z?.tempo) { mutableFloatStateOf((z?.tempo ?: 50).toFloat()) }
        Text("Tempo: ${tempo.toInt()}", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        Slider(value = tempo, onValueChange = { tempo = it }, valueRange = 0f..100f, onValueChangeFinished = {
            Wahl.tempo = tempo.toInt()
            if (z?.modus == Modus.SZENE) s.setzen(c, z.copy(tempo = tempo.toInt())) else LedStreifen.aendern(c) { it.copy(tempo = tempo.toInt()) }
        })
    }
}

@Composable
private fun MikrofonTeil() {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    val s = LichtStand
    val z = s.zustand
    Column(Modifier.padding(top = 2.dp)) {
        Text("Die Streifen reagieren mit ihrem eigenen Mikrofon auf Musik und Geräusche.", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LedStreifen.RHYTHMEN.forEachIndexed { i, name ->
                LichtKnopf(name, Modifier.weight(1f), aktiv = z?.modus == Modus.RHYTHMUS && z.rhythmus == i + 1) {
                    s.setzen(c, (z ?: LedStreifen.Zustand()).copy(modus = Modus.RHYTHMUS, rhythmus = i + 1, empfindlichkeit = Wahl.empf))
                }
            }
        }
        var empf by remember(z?.empfindlichkeit) { mutableFloatStateOf((z?.empfindlichkeit ?: 7).toFloat()) }
        Text("Empfindlichkeit: ${empf.toInt()}", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        Slider(value = empf, onValueChange = { empf = it }, valueRange = 1f..7f, steps = 5, onValueChangeFinished = {
            Wahl.empf = empf.toInt()
            if (z?.modus == Modus.RHYTHMUS) s.setzen(c, z.copy(empfindlichkeit = empf.toInt())) else LedStreifen.aendern(c) { it.copy(empfindlichkeit = empf.toInt()) }
        })
    }
}

/**
 * Farbkreis in einem Quadrat: Winkel = Farbton, Abstand zur Mitte = Sättigung (Mitte weiß). Der Punkt zeigt genau die Farbe, die gesendet wird.
 * [ziehen] bei jeder Bewegung (live), [los] beim Loslassen. Die Bewegung wird verbraucht, damit die Startseite dabei nicht scrollt.
 */
@Composable
private fun Farbkreis(rgb: String, modifier: Modifier, ziehen: (String) -> Unit, los: (String) -> Unit) {
    val f = LocalFarben.current
    val aktZiehen by androidx.compose.runtime.rememberUpdatedState(ziehen)
    val aktLos by androidx.compose.runtime.rememberUpdatedState(los)
    fun hsvAus(code: String) = FloatArray(3).also { runCatching { android.graphics.Color.colorToHSV(android.graphics.Color.parseColor("#$code"), it) } }
    var farbton by remember { mutableFloatStateOf(hsvAus(rgb)[0]) }
    var saettigung by remember { mutableFloatStateOf(hsvAus(rgb)[1]) }
    var zieht by remember { mutableStateOf(false) }
    // Neue Farbe von außen (Platz, Code, Sprache): Punkt nachführen, aber nie mitten im Ziehen.
    androidx.compose.runtime.LaunchedEffect(rgb) { if (!zieht) hsvAus(rgb).let { farbton = it[0]; saettigung = it[1] } }
    val aktuell = Color.hsv(farbton, saettigung.coerceIn(0f, 1f), 1f)
    Box(modifier.glas(f, 18.dp, erhoeht = 0.4f).padding(14.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f).pointerInput(Unit) {
            fun setze(p: Offset): String {
                val m = Offset(size.width / 2f, size.height / 2f)
                val r = min(size.width, size.height) / 2f
                farbton = ((Math.toDegrees(atan2((p.y - m.y).toDouble(), (p.x - m.x).toDouble())).toFloat() + 360f) % 360f)
                saettigung = (hypot(p.x - m.x, p.y - m.y) / r).coerceIn(0f, 1f)
                return hexAus(Color.hsv(farbton, saettigung, 1f))
            }
            awaitEachGesture {
                val erst = awaitFirstDown()
                erst.consume()
                zieht = true
                var letzte = setze(erst.position)
                aktZiehen(letzte)
                while (true) {
                    val e = awaitPointerEvent()
                    val p = e.changes.firstOrNull() ?: break
                    if (!p.pressed) break
                    p.consume()
                    val neu = setze(p.position)
                    if (neu != letzte) { letzte = neu; aktZiehen(neu) }
                }
                zieht = false
                aktLos(letzte)
            }
        }) {
            val r = min(size.width, size.height) / 2f
            val m = center
            drawCircle(Brush.sweepGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) }, m), r, m)
            drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), m, r), r, m)
            val winkel = Math.toRadians(farbton.toDouble())
            val p = Offset(m.x + (cos(winkel) * saettigung * r).toFloat(), m.y + (sin(winkel) * saettigung * r).toFloat())
            drawCircle(aktuell, r * 0.09f, p)
            drawCircle(Color.White, r * 0.09f, p, style = Stroke(width = r * 0.025f))
            drawCircle(Color.Black.copy(alpha = 0.35f), r * 0.105f, p, style = Stroke(width = r * 0.01f))
        }
    }
}

@Composable
private fun AutomatikTeil() {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    val e = remember { Einstellungen.get(c) }
    var stand by remember { mutableIntStateOf(0) }
    fun neu() { stand++ }
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); stand++ } }
    Column(Modifier.padding(top = 6.dp)) {
        androidx.compose.runtime.key(stand) {
            SchalterReihe("Automatik", "Abends von selbst an, zur Ausschaltzeit aus", e.lichtAutomatik) { e.lichtAutomatik = it; neu() }
            if (e.lichtAutomatik) {
                SchalterReihe("An freien Tagen", "Frei, krank, Urlaub, Freischicht · aus um ${e.lichtAusFrei} Uhr", e.lichtAutoFrei) { e.lichtAutoFrei = it; neu() }
                if (e.lichtAutoFrei) Stunden("Aus an freien Tagen", e.lichtAusFrei) { e.lichtAusFrei = it; neu() }
                SchalterReihe("Bei Tagdienst", "Aus um ${e.lichtAusTag} Uhr · bei Nachtdienst nie", e.lichtAutoTag) { e.lichtAutoTag = it; neu() }
                if (e.lichtAutoTag) Stunden("Aus bei Tagdienst", e.lichtAusTag) { e.lichtAusTag = it; neu() }
                Text("Einschalten (Zeitzone Berlin)", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LichtKnopf("Beginn der Dämmerung", Modifier.weight(1f), aktiv = e.lichtAnBei == "daemmerung") { e.lichtAnBei = "daemmerung"; neu() }
                    LichtKnopf("Wenn es dunkel ist", Modifier.weight(1f), aktiv = e.lichtAnBei == "dunkel") { e.lichtAnBei = "dunkel"; neu() }
                }
                SchalterReihe("Aus, wenn ich weg bin", "Beim Verlassen des Hauses aus, beim Heimkommen im Zeitfenster an", e.lichtNurZuhause) { e.lichtNurZuhause = it; neu() }
                if (e.lichtNurZuhause) {
                    var radius by remember { mutableFloatStateOf(e.lichtRadius.toFloat()) }
                    Text("Umkreis: ${radius.toInt()} m", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    Slider(value = radius, onValueChange = { radius = ((it / 10).toInt() * 10f) }, valueRange = 100f..500f, onValueChangeFinished = { e.lichtRadius = radius.toInt(); neu() })
                    var ort by remember { mutableStateOf<String?>(null) }
                    LichtKnopf("Hier ist zuhause", Modifier.fillMaxWidth()) {
                        LichtStand.bereich.launch { ort = runCatching { de.frank.jarvis.kaffee.Kaffeemaschine.zuhauseHierSetzen(c) }.getOrElse { it.message } }
                    }
                    val ohneStandort = if (!de.frank.jarvis.fahrt.Standort.immerErlaubt(c)) "Dafür braucht Jarvis den Standort „Immer zulassen“ (App-Infos von Jarvis, Berechtigungen, Standort)." else null
                    (ort ?: ohneStandort)?.let { Text(it, color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
                }
                val heute by androidx.compose.runtime.produceState<Pair<LichtAutomatik.Fenster?, String>?>(null, stand) {
                    value = kotlinx.coroutines.withContext(Dispatchers.IO) { runCatching { LichtAutomatik.fenster(c) }.getOrNull().let { it to (if (it == null) LichtAutomatik.grundOhneFenster(c) else "") } }
                }
                val uhr = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                Text(
                    heute?.let { (f0, grund) -> f0?.let { "Heute (${it.art}): an ab ${it.an.format(uhr)} Uhr, aus um ${it.aus.format(uhr)} Uhr." } ?: grund } ?: "Lese den Dienstplan …",
                    color = f.primaer, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun SchalterReihe(titel: String, text: String, an: Boolean, setze: (Boolean) -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().padding(top = 8.dp).antippen { setze(!an) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titel, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text, color = f.textSchwach, fontSize = 12.sp, lineHeight = 15.sp)
        }
        Switch(checked = an, onCheckedChange = setze)
    }
}

@Composable
private fun Stunden(titel: String, wert: Int, setze: (Int) -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(titel, color = f.textLeise, fontSize = 13.sp, modifier = Modifier.weight(1f))
        LichtKnopf("−", Modifier.size(44.dp, 36.dp)) { if (wert > 17) setze(wert - 1) }
        Text("$wert:00", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 6.dp))
        LichtKnopf("+", Modifier.size(44.dp, 36.dp)) { if (wert < 23) setze(wert + 1) }
    }
}

@Composable
private fun Aufklapper(titel: String, offen: Boolean, umschalten: () -> Unit) {
    val f = LocalFarben.current
    Row(Modifier.padding(top = 10.dp).fillMaxWidth().antippen { umschalten() }, verticalAlignment = Alignment.CenterVertically) {
        Text(titel, color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(if (offen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = f.textLeise, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun LichtKnopf(text: String, modifier: Modifier = Modifier, aktiv: Boolean = false, toenung: Color? = null, gross: Boolean = false, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(
        modifier.height(if (gross) 50.dp else 40.dp).glas(f, 14.dp, erhoeht = 0.5f, toenung = toenung ?: if (aktiv) f.primaer else null).antippen { aktion() }.padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (aktiv) f.primaer else f.text, fontSize = if (gross) 17.sp else 12.sp, fontWeight = if (aktiv || gross) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp)
    }
}

/**
 * Welt des LED-Bands: Mittellinie auf dem Boden (x quer, z in die Tiefe), von hinten links in einer S-Kurve nach vorn rechts, am Ende eine Spule.
 * Gleichmäßig nach Länge abgetastet ([M] Punkte), damit die LEDs vorn wie hinten im selben Abstand sitzen. Form abgestimmt per Vorschau am 19.11.2026.
 */
private object BandWelt {
    const val M = 280
    val x = FloatArray(M)
    val z = FloatArray(M)
    val nx = FloatArray(M)
    val nz = FloatArray(M)

    init {
        val pi2 = (Math.PI * 2).toFloat()
        val roh = 600
        val k = (0.82f * roh).toInt()
        val rx = FloatArray(roh + 1); val rz = FloatArray(roh + 1)
        for (i in 0..k) {
            val u = i / k.toFloat()
            rx[i] = -3.5f + 7f * u + 0.55f * sin(pi2 * u * 1.5f)
            rz[i] = 4.4f - 1.7f * u + 0.75f * sin(pi2 * u * 1.5f + 0.6f)
        }
        val a0 = atan2(rz[k] - rz[k - 1], rx[k] - rx[k - 1])
        val r0 = 0.42f
        val cx = rx[k] + r0 * cos(a0 - Math.PI.toFloat() / 2); val cz = rz[k] + r0 * sin(a0 - Math.PI.toFloat() / 2)
        val start = atan2(rz[k] - cz, rx[k] - cx)
        for (i in k + 1..roh) {
            val u = (i - k) / (roh - k).toFloat()
            val r = r0 * (1f - 0.45f * u)
            rx[i] = cx + r * cos(start + pi2 * 1.15f * u); rz[i] = cz + r * sin(start + pi2 * 1.15f * u)
        }
        // Nach Länge gleichmäßig neu abtasten.
        val laenge = FloatArray(roh + 1)
        for (i in 1..roh) laenge[i] = laenge[i - 1] + hypot(rx[i] - rx[i - 1], rz[i] - rz[i - 1])
        var j = 0
        for (m in 0 until M) {
            val ziel = laenge[roh] * m / (M - 1)
            while (j < roh - 1 && laenge[j + 1] < ziel) j++
            val f = ((ziel - laenge[j]) / (laenge[j + 1] - laenge[j]).coerceAtLeast(1e-6f)).coerceIn(0f, 1f)
            x[m] = rx[j] + (rx[j + 1] - rx[j]) * f; z[m] = rz[j] + (rz[j + 1] - rz[j]) * f
        }
        for (m in 0 until M) {
            val a = (m - 1).coerceAtLeast(0); val b = (m + 1).coerceAtMost(M - 1)
            val dx = x[b] - x[a]; val dz = z[b] - z[a]; val l = hypot(dx, dz).coerceAtLeast(1e-4f)
            nx[m] = -dz / l; nz[m] = dx / l
        }
    }
}

/**
 * LED-Band als 3D-Motion-Graphic direkt auf dem Kartenhintergrund (keine eigene Bühne): Das Band liegt ausgerollt mit Spule auf dem Boden, gesehen
 * aus 32° Höhe (echte Perspektive), automatisch in die Fläche eingepasst. Bewegung nur durch Licht und eine langsame Kamerafahrt: Lichtimpuls entlang
 * des Bands, sanftes Atmen; bei „An“ zünden die LEDs nacheinander, bei „Aus“ erlöschen sie der Reihe nach. Je LED weißer Kern, enger Farbhof, weicher
 * Schein und Licht auf dem Boden; im Dunkelmodus additiv wie echtes Licht. Szene: Regenbogen; Mikrofon: Equalizer-Flackern. Fortlaufende Uhr ohne
 * Sprung; ist das Licht aus, ruht die Animation.
 */
@Composable
private fun LedBand3D(z: LedStreifen.Zustand?, an: Boolean, laeuft: Boolean, modifier: Modifier) {
    val f = LocalFarben.current
    val dunkel = (f.text.red + f.text.green + f.text.blue) / 3f > 0.5f
    var sek by remember { androidx.compose.runtime.mutableDoubleStateOf(0.0) }
    var wechsel by remember { androidx.compose.runtime.mutableDoubleStateOf(-100.0) }
    var warAn by remember { mutableStateOf(an) }
    androidx.compose.runtime.LaunchedEffect(an) { if (an != warAn) { wechsel = sek; warAn = an } }
    androidx.compose.runtime.LaunchedEffect(an, laeuft) {
        val bis = if (an || laeuft) Double.MAX_VALUE else sek + 3.5
        val start = androidx.compose.runtime.withFrameNanos { it }
        val basis = sek
        var zuletzt = start
        while (sek < bis) androidx.compose.runtime.withFrameNanos { n ->
            if (n - zuletzt >= 40_000_000L) { zuletzt = n; sek = basis + (n - start) / 1e9 }
        }
    }
    val grund = anzeige(z)
    val hellFaktor = ((z?.hell ?: 100) / 100f).coerceIn(0.2f, 1f)
    val M = BandWelt.M
    val pfad = remember { androidx.compose.ui.graphics.Path() }
    val sx = remember { FloatArray(M) }; val sy = remember { FloatArray(M) }; val sd = remember { FloatArray(M) }
    val ax = remember { FloatArray(M) }; val ay = remember { FloatArray(M) }
    val bx = remember { FloatArray(M) }; val by = remember { FloatArray(M) }
    Canvas(modifier) {
        val sekF = (sek % 3600.0).toFloat()
        val w = size.width; val h = size.height
        val pi2 = (Math.PI * 2).toFloat()
        val t = sekF * (if (laeuft) 3f else 1f)
        val phi = Math.toRadians(32.0).toFloat(); val cp = cos(phi); val spp = sin(phi)
        val kameraHoehe = 2.2f
        val halb = 0.12f
        // Rohprojektion (ohne Maßstab), Kamera schwenkt langsam um wenige Grad.
        fun roh(X: Float, Z: Float, gier: Float, aus: FloatArray) {
            val cg = cos(gier); val sg = sin(gier)
            val rx = X * cg - Z * sg; val rz = X * sg + Z * cg
            val vy = 0.02f - kameraHoehe
            val d = (rz * cp - vy * spp).coerceAtLeast(0.2f)
            aus[0] = rx / d; aus[1] = -(vy * cp + rz * spp) / d; aus[2] = d
        }
        val tmp = FloatArray(3)
        // Einpassung aus der Ruhelage, damit nichts abgeschnitten wird und das Bild beim Schwenken nicht zappelt.
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (i in 0 until M) { roh(BandWelt.x[i], BandWelt.z[i], 0f, tmp); minX = min(minX, tmp[0]); maxX = kotlin.math.max(maxX, tmp[0]); minY = min(minY, tmp[1]); maxY = kotlin.math.max(maxY, tmp[1]) }
        val rand = 0.09f
        val sk = min(w * (1 - 2 * rand) / (maxX - minX), h * (1 - 2 * rand) / (maxY - minY))
        val ox = w / 2 - sk * (maxX + minX) / 2; val oy = h / 2 - sk * (maxY + minY) / 2
        val gier = 0.05f * sin(sekF * 0.13f)
        for (i in 0 until M) {
            roh(BandWelt.x[i], BandWelt.z[i], gier, tmp); sx[i] = ox + sk * tmp[0]; sy[i] = oy + sk * tmp[1]; sd[i] = tmp[2]
            roh(BandWelt.x[i] + BandWelt.nx[i] * halb, BandWelt.z[i] + BandWelt.nz[i] * halb, gier, tmp); ax[i] = ox + sk * tmp[0]; ay[i] = oy + sk * tmp[1]
            roh(BandWelt.x[i] - BandWelt.nx[i] * halb, BandWelt.z[i] - BandWelt.nz[i] * halb, gier, tmp); bx[i] = ox + sk * tmp[0]; by[i] = oy + sk * tmp[1]
        }
        val schritt = 4
        val anzahl = M / schritt
        val modus = if (dunkel) androidx.compose.ui.graphics.BlendMode.Plus else androidx.compose.ui.graphics.BlendMode.SrcOver
        fun licht(k: Int): Pair<Color, Float> {
            val u = k / anzahl.toFloat()
            val seit = (sek - wechsel).toFloat()
            val ordnung = if (an) seit - u * 1.6f else 1f - (seit - (1f - u) * 1.6f)
            val zuend = (ordnung / 0.25f).coerceIn(0f, 1f)
            if (zuend <= 0f) return Color(0xFF3C3E46) to 0f
            val atmen = 0.88f + 0.12f * sin(sekF * 1.4f)
            val p = ((u - (t * 0.2f) % 1.3f + 0.2f) / 0.2f)
            val impuls = if (p in 0f..2f) 1f - kotlin.math.abs(p - 1f) else 0f
            val (farbe, s) = when (z?.modus) {
                Modus.SZENE -> Color.hsv(((u * 300f - t * 60f) % 360f + 360f) % 360f, 1f, 1f) to zuend * 0.95f
                Modus.RHYTHMUS -> {
                    val e = (sin(k * 1.3f + t * 11f) * 0.5f + 0.5f) * (sin(k * 0.23f - t * 6f) * 0.5f + 0.5f)
                    Color.hsv((t * 90f + k * 8f) % 360f, 1f, 1f) to zuend * (0.2f + 0.8f * e)
                }
                else -> grund to zuend * (atmen * 0.6f + 0.55f * impuls * impuls).coerceAtMost(1f)
            }
            return farbe to s * hellFaktor
        }

        // Licht auf dem Boden: weicher Schein unter den LEDs.
        for (k in 0 until anzahl) {
            val i = k * schritt + schritt / 2
            val (farbe, s) = licht(k)
            if (s <= 0.02f) continue
            val r = sk * 0.55f / sd[i]
            val m = Offset(sx[i], sy[i] + r * 0.08f)
            drawOval(Brush.radialGradient(listOf(farbe.copy(alpha = (if (dunkel) 0.22f else 0.16f) * s), Color.Transparent), m, r),
                Offset(m.x - r, m.y - r * 0.38f), androidx.compose.ui.geometry.Size(2 * r, r * 0.76f), blendMode = modus)
        }

        // Bandkörper von hinten nach vorn (die Tiefe fällt entlang des Bands, die Spule vorn zuletzt).
        for (i in 0 until M - 1) {
            val nah = (1f - (sd[i] - 2f) / 4f).coerceIn(0f, 1f)
            val (farbe, s) = licht((i / schritt).coerceAtMost(anzahl - 1))
            // Dicke: dunkle Unterkante.
            val dicke = (sk * 0.018f / sd[i]).coerceAtLeast(1f)
            pfad.reset()
            pfad.moveTo(ax[i], ay[i]); pfad.lineTo(ax[i + 1], ay[i + 1]); pfad.lineTo(ax[i + 1], ay[i + 1] + dicke); pfad.lineTo(ax[i], ay[i] + dicke); pfad.close()
            drawPath(pfad, Color(0xFF0B0C0F))
            pfad.reset()
            pfad.moveTo(ax[i], ay[i]); pfad.lineTo(ax[i + 1], ay[i + 1]); pfad.lineTo(bx[i + 1], by[i + 1]); pfad.lineTo(bx[i], by[i]); pfad.close()
            val hell = 0.55f + 0.45f * nah
            drawPath(pfad, Color(
                (0.13f * hell + farbe.red * 0.08f * s).coerceAtMost(1f),
                (0.14f * hell + farbe.green * 0.08f * s).coerceAtMost(1f),
                (0.16f * hell + farbe.blue * 0.08f * s).coerceAtMost(1f),
            ))
            // Kupferbahnen an beiden Rändern.
            val bahn = (sk * 0.004f / sd[i]).coerceAtLeast(0.7f)
            drawLine(Color(0xFFC08A45).copy(alpha = 0.35f + 0.4f * nah), Offset(ax[i], ay[i]), Offset(ax[i + 1], ay[i + 1]), strokeWidth = bahn)
            drawLine(Color(0xFFC08A45).copy(alpha = 0.25f + 0.3f * nah), Offset(bx[i], by[i]), Offset(bx[i + 1], by[i + 1]), strokeWidth = bahn)
        }

        // LEDs: SMD-Gehäuse, weicher Schein, enger Hof, weißer Kern.
        for (k in 0 until anzahl) {
            val i = k * schritt + schritt / 2
            val (farbe, s) = licht(k)
            val gr = sk * 0.085f / sd[i]
            val m = Offset(sx[i], sy[i])
            if (s > 0.02f) {
                drawCircle(Brush.radialGradient(listOf(farbe.copy(alpha = 0.6f * s), farbe.copy(alpha = 0.2f * s), Color.Transparent), m, gr * 2.8f), gr * 2.8f, m, blendMode = modus)
                val kern = gr * (0.45f + 0.25f * s)
                drawOval(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0.9f), farbe, farbe.copy(alpha = 0f)), m, kern),
                    Offset(m.x - kern, m.y - kern * 0.62f), androidx.compose.ui.geometry.Size(2 * kern, kern * 1.24f))
            } else {
                drawOval(Color(0xFFE8E2D2), Offset(m.x - gr * 0.5f, m.y - gr * 0.28f), androidx.compose.ui.geometry.Size(gr * 1.0f, gr * 0.56f))
                drawOval(Color(0xFFC9C1A8), Offset(m.x - gr * 0.28f, m.y - gr * 0.15f), androidx.compose.ui.geometry.Size(gr * 0.56f, gr * 0.3f))
            }
        }
    }
}
