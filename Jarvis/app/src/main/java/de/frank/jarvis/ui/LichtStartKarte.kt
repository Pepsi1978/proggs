package de.frank.jarvis.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Stand der Licht-Karte außerhalb der Komposition. */
private object LichtStand {
    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    var zustand by mutableStateOf<LedStreifen.Zustand?>(null)
    var modusOffen by mutableStateOf(false)
    var automatikOffen by mutableStateOf(false)
    var reiter by mutableIntStateOf(0)
    var hinweis by mutableStateOf<String?>(null)

    fun tun(c: android.content.Context, arbeit: suspend () -> LedStreifen.Bericht) {
        if (LedStreifen.laeuft) { Toast.makeText(c, "Moment, das Licht wird gerade geschaltet", Toast.LENGTH_SHORT).show(); return }
        bereich.launch { runCatching { arbeit() }; zustand = LedStreifen.zustand(c) }
    }

    fun setzen(c: android.content.Context, z: LedStreifen.Zustand) { zustand = z; tun(c) { LedStreifen.setzen(c, z) } }
}

private fun farbe(rgb: String): Color = runCatching { Color(0xFF000000 or rgb.toLong(16)) }.getOrDefault(Color(0xFFFF6000))

/** Ungefähre Anzeigefarbe eines Zustands (Szenen und Mikrofon als Regenbogen-Andeutung). */
private fun anzeige(z: LedStreifen.Zustand?): Color = when (z?.modus) {
    null -> Color(0xFFFF6000)
    Modus.FARBE -> farbe(z.rgb)
    Modus.SZENE -> Color(0xFFB45CFF)
    Modus.RHYTHMUS -> Color(0xFF39D0FF)
}

/**
 * Startbereich „LED-Licht“: alle Streifen an/aus, Farbe, Helligkeit, eigene Farbe; aufklappbar Szenen (23) und Mikrofon-Modi (4) aus KS Lite,
 * darunter die Automatik (freie Tage, Tagdienst, Dämmerung/Dunkelheit, Ausschaltzeiten, Umkreis). Alle Streifen bekommen immer denselben Zustand.
 */
@Composable
fun LichtStartKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    val s = LichtStand
    if (s.zustand == null) s.zustand = LedStreifen.zustand(c)
    val z = s.zustand
    val b = LedStreifen.letzterBericht
    Karte(modifier = griff) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Leuchtkugel(anzeige(z), LedStreifen.laeuft, Modifier.size(52.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("LED-Licht", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        LedStreifen.laeuft -> "Schaltet die Streifen (Handy und Mac) …"
                        b != null && System.currentTimeMillis() - b.zeit < 30 * 60_000L -> b.text
                        z != null -> z.text()
                        else -> "Alle Streifen · noch kein Zustand gemerkt"
                    },
                    color = when { LedStreifen.laeuft -> f.primaer; b?.fehler == true -> f.gefahr; else -> f.textLeise },
                    fontSize = 13.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (!LedStreifen.erlaubt(c)) Text("Jarvis fehlt das Recht „Geräte in der Nähe“ (App-Infos von Jarvis, Berechtigungen).", color = f.gefahr, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))

        // An und Aus, groß.
        Row(Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LichtKnopf("An", Modifier.weight(1f), toenung = anzeige(z), gross = true) { s.tun(c) { LedStreifen.an(c) } }
            LichtKnopf("Aus", Modifier.weight(1f), gross = true) { s.tun(c) { LedStreifen.aus(c) } }
        }

        // Farben: ein Tipp stellt alle Streifen um.
        val farben = LedStreifen.FARBEN.entries.distinctBy { it.value }
        Text("Farbe", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))
        farben.chunked(8).forEach { reihe ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                reihe.forEach { (name, rgb) ->
                    val aktiv = z?.modus == Modus.FARBE && z.rgb.equals(rgb, true)
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(farbe(rgb))
                            .border(if (aktiv) 3.dp else 1.dp, if (aktiv) f.text else f.textSchwach.copy(alpha = 0.4f), CircleShape)
                            .antippen { s.setzen(c, (z ?: LedStreifen.Zustand()).copy(modus = Modus.FARBE, rgb = rgb)) },
                    )
                }
                repeat(8 - reihe.size) { Spacer(Modifier.size(34.dp)) }
            }
        }

        // Eigene Farbe über den Farbkreis (Farbton).
        var farbton by remember { mutableFloatStateOf(30f) }
        Text("Eigene Farbe", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).padding(horizontal = 10.dp).clip(RoundedCornerShape(5.dp))
            .background(Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) })))
        Slider(value = farbton, onValueChange = { farbton = it }, valueRange = 0f..359f, onValueChangeFinished = {
            val col = Color.hsv(farbton, 1f, 1f)
            val rgb = "%02X%02X%02X".format((col.red * 255).toInt(), (col.green * 255).toInt(), (col.blue * 255).toInt())
            s.setzen(c, (z ?: LedStreifen.Zustand()).copy(modus = Modus.FARBE, rgb = rgb))
        })

        // Helligkeit.
        var hell by remember(z?.hell) { mutableFloatStateOf((z?.hell ?: 100).toFloat()) }
        Text("Helligkeit: ${hell.toInt()} %", color = f.textLeise, fontSize = 13.sp)
        Slider(value = hell, onValueChange = { hell = it }, valueRange = 1f..100f, onValueChangeFinished = {
            val alt = z ?: LedStreifen.Zustand()
            s.setzen(c, alt.copy(modus = if (alt.modus == Modus.RHYTHMUS) Modus.FARBE else alt.modus, hell = hell.toInt()))
        })

        // Szenen und Mikrofon-Modi.
        Aufklapper("Szenen und Mikrofon-Modi", s.modusOffen) { s.modusOffen = !s.modusOffen }
        AnimatedVisibility(s.modusOffen) {
            Column(Modifier.padding(top = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LichtKnopf("Szenen", Modifier.weight(1f), aktiv = s.reiter == 0) { s.reiter = 0 }
                    LichtKnopf("Mikrofon", Modifier.weight(1f), aktiv = s.reiter == 1) { s.reiter = 1 }
                }
                if (s.reiter == 0) {
                    LedStreifen.SZENEN.withIndex().chunked(2).forEach { reihe ->
                        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            reihe.forEach { (i, name) ->
                                LichtKnopf(name.replaceFirstChar { it.uppercase() }, Modifier.weight(1f), aktiv = z?.modus == Modus.SZENE && z.szene == i) {
                                    s.setzen(c, (z ?: LedStreifen.Zustand()).copy(modus = Modus.SZENE, szene = i))
                                }
                            }
                            if (reihe.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    var tempo by remember(z?.tempo) { mutableFloatStateOf((z?.tempo ?: 50).toFloat()) }
                    Text("Tempo: ${tempo.toInt()}", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                    Slider(value = tempo, onValueChange = { tempo = it }, valueRange = 0f..100f, onValueChangeFinished = {
                        val alt = z ?: LedStreifen.Zustand()
                        if (alt.modus == Modus.SZENE) s.setzen(c, alt.copy(tempo = tempo.toInt())) else LedStreifen.merken(c, alt.copy(tempo = tempo.toInt()))
                    })
                } else {
                    Text("Die Streifen reagieren mit ihrem eigenen Mikrofon auf Musik und Geräusche.", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LedStreifen.RHYTHMEN.forEachIndexed { i, name ->
                            LichtKnopf(name, Modifier.weight(1f), aktiv = z?.modus == Modus.RHYTHMUS && z.rhythmus == i + 1) {
                                s.setzen(c, (z ?: LedStreifen.Zustand()).copy(modus = Modus.RHYTHMUS, rhythmus = i + 1))
                            }
                        }
                    }
                    var empf by remember(z?.empfindlichkeit) { mutableFloatStateOf((z?.empfindlichkeit ?: 7).toFloat()) }
                    Text("Empfindlichkeit: ${empf.toInt()}", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                    Slider(value = empf, onValueChange = { empf = it }, valueRange = 1f..7f, steps = 5, onValueChangeFinished = {
                        val alt = z ?: LedStreifen.Zustand()
                        if (alt.modus == Modus.RHYTHMUS) s.setzen(c, alt.copy(empfindlichkeit = empf.toInt())) else LedStreifen.merken(c, alt.copy(empfindlichkeit = empf.toInt()))
                    })
                }
            }
        }

        // Automatik.
        Aufklapper("Automatik", s.automatikOffen) { s.automatikOffen = !s.automatikOffen }
        AnimatedVisibility(s.automatikOffen) { AutomatikTeil() }
    }
}

@Composable
private fun AutomatikTeil() {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    val e = remember { Einstellungen.get(c) }
    var stand by remember { mutableIntStateOf(0) }
    fun neu() { stand++ }
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
                val heute = remember(stand) { runCatching { LichtAutomatik.fenster(c) }.getOrNull() }
                val uhr = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                Text(
                    heute?.let { "Heute (${it.art}): an ab ${it.an.format(uhr)} Uhr, aus um ${it.aus.format(uhr)} Uhr." } ?: "Heute schaltet die Automatik nicht (Nachtdienst, Schalter aus oder es ist erst nach der Ausschaltzeit dunkel).",
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

/** Leuchtende Kugel in der aktuellen Farbe; pulsiert, solange geschaltet wird. */
@Composable
private fun Leuchtkugel(farbe: Color, aktiv: Boolean, modifier: Modifier) {
    val puls by rememberInfiniteTransition(label = "licht").animateFloat(0.55f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "puls")
    val staerke = if (aktiv) puls else 0.85f
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(farbe.copy(alpha = 0.55f * staerke), Color.Transparent), center, r), r, center)
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), farbe, farbe.copy(alpha = 0.8f)), center + Offset(-r * 0.15f, -r * 0.15f), r * 0.55f), r * 0.5f, center)
    }
}
