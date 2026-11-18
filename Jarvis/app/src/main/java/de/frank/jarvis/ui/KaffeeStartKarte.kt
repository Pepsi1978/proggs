package de.frank.jarvis.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.kaffee.Kaffeemaschine
import de.frank.jarvis.kaffee.Kaffeemaschine.Bild
import de.frank.jarvis.kaffee.Kaffeemaschine.Getraenk
import de.frank.jarvis.ui.theme.LocalFarben
import de.frank.jarvis.ui.theme.antippen
import de.frank.jarvis.ui.theme.glas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Stand der Kaffee-Karte außerhalb der Komposition, nur für diesen App-Lauf. */
private object KaffeeStand {
    var zustand by mutableStateOf<Kaffeemaschine.Zustand?>(null)
    var fehler by mutableStateOf<String?>(null)
    var laeuft by mutableStateOf(false)
    var schritt by mutableStateOf<String?>(null)
    var auswahl by mutableStateOf(Kaffeemaschine.STANDARD)
    var getraenkeOffen by mutableStateOf(false)
    var einstellungenOffen by mutableStateOf(false)
    var automatischGelesen = false
    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var konfiguriert by mutableStateOf(false)

    /** false = gerade läuft schon etwas, der Tipp wird verworfen. */
    fun tun(nachlesen: Boolean = true, arbeit: suspend () -> String?): Boolean {
        if (laeuft) return false
        laeuft = true
        fehler = null
        schritt = null
        bereich.launch {
            try {
                arbeit()?.let { schritt = it }
                val c = appKontext
                if (nachlesen && c != null) runCatching { Kaffeemaschine.zustand(c) }.getOrNull()?.let { zustand = it }
            } catch (e: Kaffeemaschine.Fehler) {
                fehler = e.message
            } catch (e: Exception) {
                fehler = "Die Kaffeemaschine ist gerade nicht erreichbar."
            } finally { laeuft = false }
        }
        return true
    }

    var appKontext: android.content.Context? = null

    fun lesen(c: android.content.Context) {
        appKontext = c
        tun(nachlesen = false) { zustand = Kaffeemaschine.zustand(c); null }
    }
}

/**
 * Startbereich „Kaffeemaschine“: oben Franks Standard (Long Black) zum Antippen, aufklappbar alle Getränke mit Bild und die Einstellungen
 * der Maschine (Ein/Aus, Stopp, automatisch aus, Wasserhärte). Jarvis weckt die Maschine vor dem Brühen selbst auf (Franks Wunsch vom 18.11.2026).
 */
@Composable
fun KaffeeStartKarte(griff: Modifier = Modifier) {
    val f = LocalFarben.current
    val c = LocalContext.current.applicationContext
    val s = KaffeeStand
    LaunchedEffect(Unit) {
        s.appKontext = c
        s.konfiguriert = Kaffeemaschine.konfiguriert(c)
        if (!s.automatischGelesen && s.konfiguriert) { s.automatischGelesen = true; s.lesen(c) }
    }
    val z = s.zustand
    Karte(modifier = griff) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Kaffeemaschine", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        !s.konfiguriert -> "Anmeldung fehlt: Einstellungen, Konten und APIs, De'Longhi"
                        s.laeuft && s.schritt != null -> s.schritt!!
                        s.laeuft -> "Fragt die Maschine …"
                        z != null -> z.text() + (s.schritt?.let { " · $it" } ?: "")
                        else -> "Noch kein Stand"
                    },
                    color = if (s.laeuft) f.primaer else if (z?.bereit == true) f.erfolg else f.textLeise, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
            AktualisierenKnopf(s.laeuft, "Kaffeemaschine aktualisieren", "Kaffeemaschine wird gelesen") { s.konfiguriert = Kaffeemaschine.konfiguriert(c); s.lesen(c) }
        }
        Kaffeemaschine.bereitMeldung?.takeIf { !s.laeuft }?.let { Text(it, color = f.primaer, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
        s.fehler?.let { Text(it, color = f.gefahr, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
        z?.alarme?.takeIf { it.isNotEmpty() }?.let { Text(it.joinToString(" · "), color = f.gefahr, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)) }

        // Hauptknopf: gewähltes Getränk (Standard Long Black) mit einem Tipp; der Pfeil klappt die Getränkeauswahl auf.
        Row(
            Modifier.padding(top = 10.dp).fillMaxWidth().glas(f, 18.dp, erhoeht = 0.6f, toenung = f.primaer).padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).antippen {
                    val g = s.auswahl
                    if (s.tun { Kaffeemaschine.bruehen(c, g) { s.schritt = it } }) Toast.makeText(c, "${g.name} wird vorbereitet", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(c, "Moment, die Kaffeemaschine ist noch beschäftigt", Toast.LENGTH_SHORT).show()
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GetraenkBild(s.auswahl.bild, Modifier.size(58.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(s.auswahl.name, color = f.text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(if (s.auswahl == Kaffeemaschine.STANDARD) "Dein Kaffee · antippen zum Zubereiten" else "Antippen zum Zubereiten", color = f.textLeise, fontSize = 13.sp)
                }
            }
            Box(Modifier.size(44.dp).antippen { s.getraenkeOffen = !s.getraenkeOffen }, contentAlignment = Alignment.Center) {
                Icon(if (s.getraenkeOffen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, if (s.getraenkeOffen) "Getränke zuklappen" else "Alle Getränke zeigen", tint = f.text, modifier = Modifier.size(30.dp))
            }
        }

        AnimatedVisibility(s.getraenkeOffen) {
            Column(Modifier.padding(top = 10.dp)) {
                Kaffeemaschine.GETRAENKE.chunked(3).forEach { reihe ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        reihe.forEach { g -> GetraenkKachel(g, g == s.auswahl, Modifier.weight(1f)) { s.auswahl = g; s.getraenkeOffen = false } }
                        repeat(3 - reihe.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        // Einstellungen der Maschine, ebenfalls aufklappbar.
        Row(Modifier.padding(top = 10.dp).fillMaxWidth().antippen { s.einstellungenOffen = !s.einstellungenOffen }, verticalAlignment = Alignment.CenterVertically) {
            Text("Einstellungen der Maschine", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(if (s.einstellungenOffen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = f.textLeise, modifier = Modifier.size(26.dp))
        }
        AnimatedVisibility(s.einstellungenOffen) {
            Column(Modifier.padding(top = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KaffeeKnopf("Einschalten", Modifier.weight(1f)) { s.tun { Kaffeemaschine.aufwecken(c) } }
                    KaffeeKnopf("Ausschalten", Modifier.weight(1f)) { s.tun { Kaffeemaschine.ausschalten(c) } }
                    KaffeeKnopf("Stopp", Modifier.weight(1f)) { s.tun { Kaffeemaschine.stoppen(c) } }
                }
                Kaffeemaschine.EINSTELLUNGEN.forEach { e ->
                    Text(e.name, color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        e.werte.forEach { (wert, name) ->
                            KaffeeKnopf(name.substringBefore(" ("), Modifier.weight(1f), aktiv = z?.einstellungen?.get(e.schluessel) == wert) {
                                s.tun { Kaffeemaschine.einstellen(c, e, wert) }
                            }
                        }
                    }
                }
                // Wach halten: Die Soul spült bei jedem Aufwachen; bleibt sie an, kommt der Kaffee ohne Spülwasser.
                val e = remember { de.frank.jarvis.data.Einstellungen.get(c) }
                var wach by remember { mutableStateOf(e.kaffeeWachHalten) }
                var von by remember { mutableStateOf(e.kaffeeWachVon) }
                var bis by remember { mutableStateOf(e.kaffeeWachBis) }
                var nachWecker by remember { mutableStateOf(e.kaffeeWachNachWecker) }
                Text("Wach halten (kein Spülen vor dem Kaffee)", color = f.textLeise, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KaffeeKnopf("Aus", Modifier.weight(1f), aktiv = !wach) { wach = false; e.kaffeeWachHalten = false }
                    KaffeeKnopf("An", Modifier.weight(1f), aktiv = wach) { wach = true; e.kaffeeWachHalten = true }
                    if (!nachWecker) KaffeeKnopf("ab $von Uhr", Modifier.weight(1f)) { von = (von + 1) % 24; e.kaffeeWachVon = von }
                    KaffeeKnopf("bis $bis Uhr", Modifier.weight(1f)) { bis = (bis + 1) % 24; e.kaffeeWachBis = bis }
                }
                // Start mit dem Wecker: Tagschicht-, Nachtschicht- oder sonst der eingeschaltete Wecker von heute.
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KaffeeKnopf("Feste Uhrzeit", Modifier.weight(1f), aktiv = !nachWecker) { nachWecker = false; e.kaffeeWachNachWecker = false }
                    KaffeeKnopf("Ab meinem Wecker", Modifier.weight(1f), aktiv = nachWecker) { nachWecker = true; e.kaffeeWachNachWecker = true }
                }
                var startText by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(nachWecker, von, bis, wach) {
                    startText = runCatching { Kaffeemaschine.wachFenster(c) }.getOrNull()?.let { (start, ende) ->
                        val uhr = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                        "Heute an ab ${start.zeit.format(uhr)} Uhr (${start.grund}) bis ${ende.format(uhr)} Uhr" + (if (ende.toLocalDate() != start.zeit.toLocalDate()) " am nächsten Tag" else "") + "."
                    } ?: if (nachWecker) "Heute klingelt kein eingeschalteter Wecker mehr." else null
                }
                if (wach) startText?.let { Text(it, color = f.primaer, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
                Text(
                    "Länger als 3 Stunden bleibt die Soul allein nicht an. Mit „Wach halten“ schickt Jarvis im Zeitfenster stündlich ein Signal, dann bleibt sie bereit. " +
                        "Schläft sie doch, weckt Jarvis sie bei „Mach mir einen Kaffee“ nur auf und meldet sich, wenn das Spülen vorbei ist. Uhrzeiten: antippen zählt eine Stunde weiter.",
                    color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp),
                )
                z?.profile?.takeIf { it.isNotEmpty() }?.let { p ->
                    Text("Jarvis brüht die Rezepte aus Profil 1 (${p[1] ?: "Profil 1"}).", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun GetraenkKachel(g: Getraenk, aktiv: Boolean, modifier: Modifier, aktion: () -> Unit) {
    val f = LocalFarben.current
    Column(
        modifier.height(104.dp).glas(f, 14.dp, erhoeht = 0.5f, toenung = if (aktiv) f.primaer else null).antippen { aktion() }.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        GetraenkBild(g.bild, Modifier.size(50.dp))
        Text(g.name, color = if (aktiv) f.primaer else f.text, fontSize = 12.sp, lineHeight = 14.sp, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun KaffeeKnopf(text: String, modifier: Modifier = Modifier, aktiv: Boolean = false, aktion: () -> Unit) {
    val f = LocalFarben.current
    Box(
        modifier.height(40.dp).glas(f, 12.dp, erhoeht = 0.5f, toenung = if (aktiv) f.primaer else null).antippen { aktion() }.padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (aktiv) f.primaer else f.text, fontSize = 12.sp, fontWeight = if (aktiv) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp) }
}

private val KAFFEE = Color(0xFF4A2C1A)
private val CREMA = Color(0xFFC48A4F)
private val MILCH = Color(0xFFF3EADC)
private val SCHAUM = Color(0xFFFFFCF6)
private val WASSER = Color(0xFF8FCBF2)
private val TASSE = Color(0xFFF7F7F5)

/** Selbst gezeichnete Getränkebilder: Tasse, Becher oder Glas mit Schichten aus Kaffee, Crema, Milch und Schaum. */
@Composable
fun GetraenkBild(bild: Bild, modifier: Modifier = Modifier) {
    val rand = LocalFarben.current.textLeise
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        when (bild) {
            Bild.ESPRESSO -> tasse(w * 0.22f, h * 0.45f, w * 0.56f, h * 0.36f, listOf(KAFFEE to 0.8f, CREMA to 0.2f), rand)
            Bild.DOPPEL -> {
                tasse(w * 0.02f, h * 0.48f, w * 0.44f, h * 0.32f, listOf(KAFFEE to 0.8f, CREMA to 0.2f), rand, henkel = false)
                tasse(w * 0.50f, h * 0.48f, w * 0.44f, h * 0.32f, listOf(KAFFEE to 0.8f, CREMA to 0.2f), rand)
            }
            Bild.KAFFEE -> tasse(w * 0.16f, h * 0.34f, w * 0.64f, h * 0.48f, listOf(KAFFEE to 0.85f, CREMA to 0.15f), rand)
            Bild.LANG, Bild.AMERICANO -> becher(w * 0.20f, h * 0.18f, w * 0.56f, h * 0.68f, listOf(KAFFEE to 0.9f, CREMA to 0.1f), rand)
            Bild.LONG_BLACK -> {
                becher(w * 0.20f, h * 0.18f, w * 0.56f, h * 0.68f, listOf(KAFFEE to 0.75f, CREMA to 0.25f), rand)
                dampf(w * 0.48f, h * 0.02f, h * 0.14f, rand)
            }
            Bild.CAPPUCCINO -> tasse(w * 0.14f, h * 0.34f, w * 0.68f, h * 0.48f, listOf(KAFFEE to 0.4f, MILCH to 0.25f, SCHAUM to 0.35f), rand, haube = true)
            Bild.LATTE -> glasForm(w * 0.28f, h * 0.06f, w * 0.44f, h * 0.86f, listOf(MILCH to 0.45f, CREMA to 0.22f, SCHAUM to 0.25f), rand)
            Bild.CORTADO -> glasForm(w * 0.28f, h * 0.34f, w * 0.44f, h * 0.56f, listOf(KAFFEE to 0.5f, MILCH to 0.4f), rand)
            Bild.MILCH -> glasForm(w * 0.28f, h * 0.10f, w * 0.44f, h * 0.82f, listOf(MILCH to 0.8f, SCHAUM to 0.12f), rand)
            Bild.WASSER -> glasForm(w * 0.28f, h * 0.10f, w * 0.44f, h * 0.82f, listOf(WASSER.copy(alpha = 0.75f) to 0.8f), rand)
        }
    }
}

/** Schichten von unten nach oben in einer Form, die [form] als Pfad liefert. */
private fun DrawScope.schichten(x: Float, y: Float, b: Float, hoehe: Float, schichten: List<Pair<Color, Float>>, form: Path) {
    var unten = y + hoehe
    clipPath(form) {
        schichten.forEach { (farbe, anteil) ->
            val dicke = hoehe * anteil
            drawRect(farbe, Offset(x, unten - dicke), Size(b, dicke))
            unten -= dicke
        }
    }
}

private fun DrawScope.tasse(x: Float, y: Float, b: Float, hoehe: Float, inhalt: List<Pair<Color, Float>>, rand: Color, henkel: Boolean = true, haube: Boolean = false) {
    val strich = Stroke(width = size.minDimension * 0.045f)
    val form = Path().apply {
        moveTo(x, y); lineTo(x + b, y)
        quadraticTo(x + b, y + hoehe, x + b * 0.5f, y + hoehe)
        quadraticTo(x, y + hoehe, x, y); close()
    }
    drawPath(form, TASSE)
    schichten(x, y + hoehe * 0.12f, b, hoehe * 0.88f, inhalt, form)
    if (haube) drawOval(SCHAUM, Offset(x + b * 0.06f, y - hoehe * 0.16f), Size(b * 0.88f, hoehe * 0.34f))
    drawPath(form, rand, style = strich)
    if (henkel) drawArc(rand, -90f, 180f, false, Offset(x + b * 0.82f, y + hoehe * 0.12f), Size(b * 0.32f, hoehe * 0.46f), style = strich)
    drawLine(rand, Offset(x - b * 0.12f, y + hoehe + strich.width * 1.6f), Offset(x + b * 1.12f, y + hoehe + strich.width * 1.6f), strokeWidth = strich.width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
}

private fun DrawScope.becher(x: Float, y: Float, b: Float, hoehe: Float, inhalt: List<Pair<Color, Float>>, rand: Color) {
    val strich = Stroke(width = size.minDimension * 0.045f)
    val form = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(x, y, x + b, y + hoehe, CornerRadius(b * 0.08f), CornerRadius(b * 0.08f), CornerRadius(b * 0.22f), CornerRadius(b * 0.22f)))
    }
    drawPath(form, TASSE)
    schichten(x, y + hoehe * 0.1f, b, hoehe * 0.9f, inhalt, form)
    drawPath(form, rand, style = strich)
    drawArc(rand, -90f, 180f, false, Offset(x + b * 0.8f, y + hoehe * 0.2f), Size(b * 0.42f, hoehe * 0.44f), style = strich)
}

private fun DrawScope.glasForm(x: Float, y: Float, b: Float, hoehe: Float, inhalt: List<Pair<Color, Float>>, rand: Color) {
    val strich = Stroke(width = size.minDimension * 0.04f)
    val form = Path().apply {
        moveTo(x, y); lineTo(x + b, y); lineTo(x + b * 0.9f, y + hoehe); lineTo(x + b * 0.1f, y + hoehe); close()
    }
    drawPath(form, Color.White.copy(alpha = 0.18f))
    schichten(x, y + hoehe * 0.08f, b, hoehe * 0.92f, inhalt, form)
    drawPath(form, rand, style = strich)
}

private fun DrawScope.dampf(x: Float, y: Float, hoehe: Float, rand: Color) {
    val strich = Stroke(width = size.minDimension * 0.03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    for (dx in listOf(-0.12f, 0f, 0.12f)) {
        val p = Path().apply {
            val sx = x + size.width * dx
            moveTo(sx, y + hoehe); cubicTo(sx - hoehe * 0.3f, y + hoehe * 0.6f, sx + hoehe * 0.3f, y + hoehe * 0.4f, sx, y)
        }
        drawPath(p, rand.copy(alpha = 0.6f), style = strich)
    }
}
