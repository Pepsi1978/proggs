package de.frank.longevity.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.longevity.data.Faktor
import de.frank.longevity.data.platz
import de.frank.longevity.ki.Art
import de.frank.longevity.ki.KiArbeit
import de.frank.longevity.ui.theme.Chip
import de.frank.longevity.ui.theme.Design
import de.frank.longevity.ui.theme.LocalFarben
import de.frank.longevity.ui.theme.antippen
import de.frank.longevity.ui.theme.farbenFuer
import de.frank.longevity.ui.theme.glas
import de.frank.longevity.ui.theme.knopf3d
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ListeBildschirm(vm: AppViewModel) {
    val f = LocalFarben.current
    val alle by vm.alle.collectAsState()
    val liste = remember(alle) { alle.filter { !it.vorschlag } }
    val plus = remember(liste) { liste.filter { !it.raeuber } }
    val raeuber = remember(liste) { liste.filter { it.raeuber } }
    val vorschlaege = remember(alle) { alle.filter { it.vorschlag } }
    val zustand = rememberLazyListState()
    var designOffen by rememberSaveable { mutableStateOf(false) }
    val oben = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val gescrollt by remember { derivedStateOf { if (zustand.firstVisibleItemIndex > 0) 1f else (zustand.firstVisibleItemScrollOffset / 200f).coerceIn(0f, 1f) } }

    // Nach dem Einordnen den neuen Faktor in Sicht holen.
    val ziel = vm.hervorgehoben
    LaunchedEffect(ziel, liste.size) {
        val index = liste.indexOfFirst { it.id == ziel }
        if (ziel != null && index >= 0 && !KiArbeit.laeuft) {
            zustand.animateScrollToItem(index + 4, -300)
            kotlinx.coroutines.delay(2600)
            if (vm.hervorgehoben == ziel) vm.hervorgehoben = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = zustand,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = oben + 66.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (vm.einstellungen.szeneZeigen) item(key = "szene") {
                // Parallax: die Szene gleitet langsamer weg und blendet aus.
                LongevitySzene(
                    Modifier.fillMaxWidth().graphicsLayer {
                        val o = if (zustand.firstVisibleItemIndex == 0) zustand.firstVisibleItemScrollOffset.toFloat() else 600f
                        translationY = o * 0.45f
                        alpha = (1f - o / 520f).coerceIn(0f, 1f)
                        val s = 1f - (o / 3000f).coerceIn(0f, 0.08f)
                        scaleX = s; scaleY = s
                    },
                    fortschritt = liste.flatMap { it.punkte }.let { p -> if (p.isEmpty()) 0f else p.count { it.erledigt }.toFloat() / p.size },
                )
            }
            item(key = "ki") { KiKarte(vm, nurFuer = null) }
            item(key = "heute") { HeuteKarte(vm, liste) }
            if (vorschlaege.isNotEmpty()) item(key = "vorschlaege") { VorschlaegeKarte(vm, vorschlaege) }
            item(key = "kopf") {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("Deine Rangliste", color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Oben, was dir Lebenszeit schenkt – unter der Null-Linie, was sie dir raubt.", color = f.textLeise, fontSize = 12.sp)
                    }
                    val stand = vm.einstellungen.letzteAktualisierung
                    Text(
                        if (stand == 0L) "Startstand" else "Geprüft " + SimpleDateFormat("dd.MM.yy", Locale.GERMANY).format(Date(stand)),
                        color = f.textSchwach, fontSize = 11.sp,
                    )
                }
            }
            items(plus, key = { it.id }) { x ->
                FaktorKarte(x, x.platz(liste), hervor = vm.hervorgehoben == x.id, modifier = Modifier.animateItem()) { vm.oeffne(x.id) }
            }
            item(key = "nulllinie") { NullLinie(raeuber.size, Modifier.animateItem()) }
            items(raeuber, key = { it.id }) { x ->
                FaktorKarte(x, x.platz(liste), hervor = vm.hervorgehoben == x.id, modifier = Modifier.animateItem()) { vm.oeffne(x.id) }
            }
            item(key = "ueberblick") { Ueberblick(liste) { vm.oeffne(it) } }
            item(key = "hinweis") {
                Text(
                    "Longevity ersetzt keine ärztliche Beratung. Die Schätzungen beruhen auf Studien und KI-Einordnung und gelten für Durchschnittsmenschen.",
                    color = f.textSchwach, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        // Kopfleiste: wird beim Scrollen zur Glasleiste.
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth()
                    .drawBehind {
                        drawRect(Brush.verticalGradient(listOf(f.hgOben.copy(alpha = 0.92f * gescrollt), f.hgOben.copy(alpha = 0.75f * gescrollt))))
                        drawLine(f.rand.copy(alpha = 0.5f * gescrollt), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    }
                    .statusBarsPadding().padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Longevity", color = f.text, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text("${plus.size} schenken Jahre · ${raeuber.size} rauben Jahre", color = f.textLeise, fontSize = 11.sp)
                }
                AktualisierenKnopf(vm)
                RundKnopf(Icons.Rounded.Palette, "Design wechseln", aktiv = designOffen) { designOffen = !designOffen }
                val system = isSystemInDarkTheme()
                RundKnopf(if (f.dunkel) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, if (f.dunkel) "Hell" else "Dunkel") {
                    vm.einstellungen.modus = if (f.dunkel) "hell" else "dunkel"
                    if ((vm.einstellungen.modus == "dunkel") == system) vm.einstellungen.modus = "system"
                }
                RundKnopf(Icons.Rounded.Settings, "Einstellungen") { vm.zeige(Bildschirm.Einstellungen) }
            }
            AnimatedVisibility(designOffen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                DesignWahl(vm.einstellungen.design) { vm.einstellungen.design = it; designOffen = false }
            }
        }

        // Schwebender Plus-Knopf unten rechts.
        PlusKnopf(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = 22.dp)) { vm.neueIdee() }
    }
}

@Composable
private fun AktualisierenKnopf(vm: AppViewModel) {
    val f = LocalFarben.current
    val laeuft = KiArbeit.laeuft && KiArbeit.art == Art.AKTUALISIEREN
    val drehen = rememberInfiniteTransition(label = "drehen")
    val w by drehen.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "w")
    Box(
        Modifier.padding(3.dp).size(42.dp)
            .then(if (laeuft) Modifier.knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel) else Modifier.glas(f, 99.dp, 0.8f))
            .antippen { vm.aktualisieren() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Refresh, "Rangfolge von der KI prüfen lassen",
            tint = if (laeuft) Color.White else f.text,
            modifier = Modifier.size(22.dp).graphicsLayer { if (laeuft) rotationZ = w },
        )
    }
}

@Composable
private fun DesignWahl(aktuell: String, waehlen: (String) -> Unit) {
    val f = LocalFarben.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).glas(f, 22.dp, 2f, f.flaecheStark).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Design.entries.forEach { d ->
            val v = farbenFuer(d, f.dunkel)
            val aktiv = d.id == aktuell
            Column(
                Modifier.weight(1f).glas(f, 16.dp, if (aktiv) 1.5f else 0.3f, if (aktiv) v.primaer.copy(alpha = 0.18f) else f.flaeche).antippen { waehlen(d.id) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(40.dp).knopf3d(v.primaer, v.sekundaer, 99.dp, f.dunkel), contentAlignment = Alignment.Center) { Text(d.emoji, fontSize = 20.sp) }
                Text(d.anzeige, color = if (aktiv) v.primaer else f.text, fontSize = 12.sp, fontWeight = if (aktiv) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun PlusKnopf(modifier: Modifier, aktion: () -> Unit) {
    val f = LocalFarben.current
    val puls = rememberInfiniteTransition(label = "plus")
    val p by puls.animateFloat(0f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Restart), label = "p")
    Box(
        modifier.size(84.dp).drawBehind {
            drawCircle(f.primaer.copy(alpha = 0.25f * (1f - p)), size.minDimension / 2 * (0.75f + 0.35f * p))
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(64.dp).knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel).antippen(aktion = aktion), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, "Eigenen Punkt einsprechen", tint = Color.White, modifier = Modifier.size(34.dp))
        }
    }
}

/**
 * Platz als plastische Plakette; die ersten drei glänzen golden, silbern, bronzen. Lebenszeit-Räuber ([raeuber])
 * tragen ihren Minus-Platz (−1, −2 …) auf roter Plakette.
 */
@Composable
fun RangAbzeichen(rang: Int, groesse: Int = 44, raeuber: Boolean = false, text: String = "$rang") {
    val f = LocalFarben.current
    val medaille = !raeuber && rang in 1..3
    val (a, b) = when {
        raeuber -> f.gefahr to f.gefahr.copy(red = f.gefahr.red * 0.7f, green = f.gefahr.green * 0.7f, blue = f.gefahr.blue * 0.7f)
        rang == 1 -> Color(0xFFFFD35C) to Color(0xFFE59A12)
        rang == 2 -> Color(0xFFE3E8F0) to Color(0xFF9AA6B8)
        rang == 3 -> Color(0xFFF2B48A) to Color(0xFFB8683A)
        else -> f.primaer to f.sekundaer
    }
    Box(Modifier.size(groesse.dp).knopf3d(a, b, 14.dp, f.dunkel), contentAlignment = Alignment.Center) {
        Text(text, color = if (medaille) Color(0xFF3A2400).copy(alpha = if (rang == 2) 0.75f else 0.85f) else Color.White, fontSize = (groesse * (if (text.length > 2) 0.34f else 0.42f)).sp, fontWeight = FontWeight.Black)
    }
}

/** Die Null-Linie zwischen den Plus-Faktoren und den Lebenszeit-Räubern. */
@Composable
private fun NullLinie(anzahl: Int, modifier: Modifier = Modifier) {
    val f = LocalFarben.current
    Column(modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp, start = 4.dp, end = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.weight(1f).height(2.dp)) { drawRect(f.textSchwach.copy(alpha = 0.5f)) }
            Text("0 Jahre", color = f.text, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp))
            Canvas(Modifier.weight(1f).height(2.dp)) { drawRect(f.textSchwach.copy(alpha = 0.5f)) }
        }
        Text("Lebenszeit-Räuber", color = f.gefahr, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
        Text(
            if (anzahl == 0) "Noch keine – schädliche Gewohnheiten, die Jahre kosten, landen hier."
            else "Schädliche Gewohnheiten, die Jahre kosten – ganz unten die schlimmste. Abstellen holt sie zurück; trifft etwas nicht auf dich zu, hake es ab.",
            color = f.textLeise, fontSize = 12.sp,
        )
    }
}

/** Pfeil seit der letzten Aktualisierung: ↑2, ↓1 oder NEU. */
@Composable
fun RangPfeil(x: Faktor) {
    val f = LocalFarben.current
    val diff = x.vorherRang?.let { it - x.rang } ?: 0
    val (text, farbe) = when {
        x.neu -> "NEU" to f.tertiaer
        diff > 0 -> "↑$diff" to f.erfolg
        diff < 0 -> "↓${-diff}" to f.gefahr
        else -> return
    }
    Text(
        text, color = farbe, fontSize = 11.sp, fontWeight = FontWeight.Black,
        modifier = Modifier.glas(f, 99.dp, 0f, farbe.copy(alpha = 0.15f), rand = false).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
fun FaktorKarte(x: Faktor, platz: String, hervor: Boolean, modifier: Modifier = Modifier, aktion: () -> Unit) {
    val f = LocalFarben.current
    val leuchten by animateFloatAsState(if (hervor) 1f else 0f, tween(600), label = "hervor")
    val punkte = x.punkte
    val anteil = if (punkte.isEmpty()) 0f else punkte.count { it.erledigt }.toFloat() / punkte.size
    val erreicht = x.zielErreicht
    Column(
        modifier.fillMaxWidth().einblenden()
            .graphicsLayer { if (erreicht && !hervor) alpha = 0.5f }
            .drawBehind {
                if (leuchten > 0f) drawRoundRect(
                    f.primaer.copy(alpha = 0.35f * leuchten), topLeft = Offset(-8f, -8f), size = Size(size.width + 16f, size.height + 16f),
                    cornerRadius = CornerRadius(f.radius.toPx() + 8f),
                )
            }
            .glas(f, erhoeht = if (erreicht) 0.3f else 1f + leuchten, toenung = if (erreicht) f.textSchwach else f.evidenzFarbe(x.ev))
            .antippen(aktion = aktion)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (erreicht) Box(Modifier.size(44.dp).glas(f, 14.dp, 0f, f.erfolg.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text("✓", color = f.erfolg, fontSize = 22.sp, fontWeight = FontWeight.Black)
            } else RangAbzeichen(x.rang, raeuber = x.raeuber, text = platz)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                if (erreicht) Text("Platz $platz · " + if (x.raeuber) "abgestellt" else "Ziel erreicht", color = f.erfolg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(x.titel, color = if (erreicht) f.textLeise else f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${x.kat.emoji} ${x.kat.anzeige}", color = f.textLeise, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    EvidenzAbzeichen(x.ev)
                }
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                if (!erreicht) RangPfeil(x)
                Text(jahreText(x.jahre), color = if (x.raeuber) f.gefahr else f.primaer, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
                if (punkte.isNotEmpty()) MiniRing(anteil, Modifier.padding(top = 4.dp).size(18.dp))
            }
        }
        Canvas(Modifier.fillMaxWidth().height(4.dp).padding(top = 0.dp).graphicsLayer { alpha = 0.9f }) {
            drawRoundRect(f.textSchwach.copy(alpha = 0.12f), cornerRadius = CornerRadius(4f))
            val balken = if (x.raeuber) listOf(f.gefahr.copy(alpha = 0.6f), f.gefahr) else listOf(f.primaer, f.sekundaer)
            drawRoundRect(Brush.horizontalGradient(balken), size = Size(size.width * x.wirkung / 100f, size.height), cornerRadius = CornerRadius(4f))
        }
    }
}

/** Die drei wichtigsten nächsten Schritte quer über alle Faktoren. */
@Composable
private fun HeuteKarte(vm: AppViewModel, liste: List<Faktor>) {
    val f = LocalFarben.current
    val schritte = remember(liste) { vm.heute(liste) }
    Column(Modifier.fillMaxWidth().einblenden(40).glas(f, erhoeht = 1.2f, toenung = f.primaer).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("☀️", fontSize = 18.sp)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("Heute für dich", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Die wirksamsten nächsten Schritte", color = f.textLeise, fontSize = 11.sp)
            }
            val alle = liste.flatMap { it.punkte }
            if (alle.isNotEmpty()) Text("${alle.count { it.erledigt }}/${alle.size}", color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        if (schritte.isEmpty()) Text("Alles erledigt – großartig! 🎉", color = f.erfolg, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
        schritte.forEach { s ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp).antippen(haptik = false) { vm.oeffne(s.faktor.id) }, verticalAlignment = Alignment.CenterVertically) {
                Checkkreis(false, f.erfolg, 22) { vm.punktUmschalten(s.faktor, s.index) }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(s.punkt.titel, color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Platz ${s.faktor.platz(liste)} · ${s.faktor.titel}", color = f.textLeise, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun VorschlaegeKarte(vm: AppViewModel, vorschlaege: List<Faktor>) {
    val f = LocalFarben.current
    Column(Modifier.fillMaxWidth().einblenden(60).glas(f, erhoeht = 1.2f, toenung = f.tertiaer).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = f.tertiaer, modifier = Modifier.size(20.dp))
            Text("Die KI schlägt neue Faktoren vor", color = f.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
        vorschlaege.forEach { v ->
            Column(Modifier.fillMaxWidth().glas(f, 16.dp, 0.4f, f.flaecheStark).antippen(haptik = false) { vm.oeffne(v.id) }.padding(12.dp)) {
                Text(v.titel, color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text((if (v.raeuber) "Lebenszeit-Räuber" else "Vorgeschlagen für Platz ${v.rang}") + " · ${jahreText(v.jahre)} · ${v.ev.anzeige}", color = f.textLeise, fontSize = 11.sp)
                if (v.kurz.isNotBlank()) Text(v.kurz, color = f.textLeise, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Aufnehmen", true, icon = Icons.Rounded.Add) { vm.vorschlagAnnehmen(v) }
                    Chip("Verwerfen", false, icon = Icons.Rounded.Close) { vm.vorschlagVerwerfen(v) }
                }
            }
        }
    }
}

/**
 * Zeigt die laufende oder gerade fertige KI-Arbeit: Prozentbalken, aktueller Schritt, Ergebnis.
 * [nurFuer] = Faktor-ID, wenn die Karte nur für die Arbeit an diesem Faktor erscheinen soll.
 */
@Composable
fun KiKarte(vm: AppViewModel, nurFuer: Long?) {
    val f = LocalFarben.current
    val art = KiArbeit.art
    val sichtbar = art != null && (nurFuer == null || KiArbeit.bezugId == nurFuer) &&
        (KiArbeit.laeuft || KiArbeit.ergebnis != null || KiArbeit.fehler != null)
    AnimatedVisibility(sichtbar, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Column(
            Modifier.fillMaxWidth().animateContentSize().glas(f, erhoeht = 1.6f, fuellung = f.flaecheStark, toenung = f.primaer).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val laeuft = KiArbeit.laeuft
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when { laeuft -> "🧠"; KiArbeit.fehler != null -> "⚠️"; else -> "✅" },
                    fontSize = 20.sp,
                )
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        (art?.anzeige ?: "KI") + if (laeuft) " läuft" else if (KiArbeit.fehler != null) " nicht fertig" else " fertig",
                        color = f.text, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    )
                    Text(KiArbeit.titel, color = f.textLeise, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (laeuft) Text("${(KiArbeit.prozent * 100).toInt()} %", color = f.primaer, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            if (laeuft) {
                FortschrittsBalken(KiArbeit.prozent)
                Text(KiArbeit.schritt, color = f.textLeise, fontSize = 12.sp)
            } else {
                Text(KiArbeit.fehler ?: KiArbeit.ergebnis.orEmpty(), color = if (KiArbeit.fehler != null) f.gefahr else f.text, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (art == Art.AKTUALISIEREN && (laeuft || KiArbeit.protokoll.isNotEmpty())) {
                    Chip("Diskussion", false, icon = Icons.Rounded.Forum) { vm.zeige(Bildschirm.Protokoll) }
                }
                if (laeuft) {
                    Chip("Abbrechen", false, icon = Icons.Rounded.Stop) { KiArbeit.abbrechen() }
                } else {
                    val id = KiArbeit.bezugId
                    if (id != null && nurFuer == null && KiArbeit.fehler == null) Chip("Ansehen", true, icon = Icons.Rounded.OpenInNew) { KiArbeit.quittieren(); vm.oeffne(id) }
                    Chip("OK", false) { KiArbeit.quittieren() }
                }
            }
        }
    }
}

/** Fortschrittsbalken mit wanderndem Glanz. */
@Composable
fun FortschrittsBalken(wert: Float) {
    val f = LocalFarben.current
    val glatt by animateFloatAsState(wert, tween(600), label = "fort")
    val glanz = rememberInfiniteTransition(label = "glanz")
    val g by glanz.animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "g")
    Canvas(Modifier.fillMaxWidth().height(12.dp)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(f.textSchwach.copy(alpha = 0.18f), cornerRadius = r)
        val breite = size.width * glatt
        drawRoundRect(Brush.horizontalGradient(listOf(f.primaer, f.sekundaer, f.tertiaer)), size = Size(breite, size.height), cornerRadius = r)
        val x = size.width * g
        if (x < breite) drawRoundRect(
            Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent), startX = x - 60f, endX = x + 60f),
            size = Size(breite, size.height), cornerRadius = r,
        )
    }
}
