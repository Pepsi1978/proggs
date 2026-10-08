package de.frank.modellkompass.ui

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import de.frank.modellkompass.data.Bereich
import de.frank.modellkompass.ui.theme.LocalFarben
import de.frank.modellkompass.ui.theme.ModellKompassTheme
import de.frank.modellkompass.ui.theme.antippen
import de.frank.modellkompass.ui.theme.glas
import de.frank.modellkompass.ui.theme.knopf3d
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

fun standText(zeit: Long, muster: String = "dd.MM.yyyy, HH:mm"): String = SimpleDateFormat(muster, Locale.GERMANY).format(Date(zeit))

@Composable
fun ModellKompassApp(vm: AppViewModel, activity: ComponentActivity) {
    val system = isSystemInDarkTheme()
    val dunkel = when (vm.einstellungen.modus) { "hell" -> false; "dunkel" -> true; else -> system }
    ModellKompassTheme(dunkel) {
        val view = LocalView.current
        // Die Symbole der Systemleisten folgen dem Modus der App, nicht dem des Handys.
        SideEffect {
            (view.context as? Activity)?.window?.let { fenster ->
                WindowCompat.getInsetsController(fenster, view).apply {
                    isAppearanceLightStatusBars = !dunkel
                    isAppearanceLightNavigationBars = !dunkel
                }
            }
        }
        // Während einer Recherche bleibt der Bildschirm an, damit Android den Lauf nicht schlafen legt.
        DisposableEffect(vm.laufAktiv) {
            view.keepScreenOn = vm.laufAktiv
            onDispose { view.keepScreenOn = false }
        }
        Box(Modifier.fillMaxSize()) {
            Hintergrund(Modifier.fillMaxSize())
            BackHandler(vm.rueckfrage) { vm.rueckfrageSchliessen() }
            BackHandler(!vm.rueckfrage && vm.bildschirm != Bildschirm.Start) { vm.zurueck() }
            AnimatedContent(
                vm.bildschirm,
                transitionSpec = {
                    val vor = targetState != Bildschirm.Start
                    if (vor) (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 420f)) { it / 3 } + fadeIn(tween(220)) + scaleIn(initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.97f))
                    else (fadeIn(tween(220)) + scaleIn(initialScale = 0.97f))
                        .togetherWith(slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(180)))
                },
                label = "bildschirm",
            ) { b ->
                when (b) {
                    Bildschirm.Start -> StartBildschirm(vm, dunkel)
                    is Bildschirm.Detail -> BereichBildschirm(vm, b.bereich)
                    Bildschirm.Einstellungen -> EinstellungenBildschirm(vm, activity)
                }
            }
            Rueckfrage(vm)
            Meldung(vm, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun Meldung(vm: AppViewModel, modifier: Modifier) {
    val f = LocalFarben.current
    val text = vm.meldung
    LaunchedEffect(text) { if (text != null) { delay(2800); vm.meldungWeg() } }
    AnimatedVisibility(text != null, modifier, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
        Row(
            Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 26.dp).widthIn(max = 520.dp).fillMaxWidth()
                .glas(f, 18.dp, 2f, if (f.dunkel) Color(0xF2241A12) else Color(0xF7FFFFFF)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(vm.meldung.orEmpty(), color = f.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        }
    }
}

/** Die Frage vor dem vollständigen Lauf: abgedunkelter Grund, Glaskarte, Ja und Nein. */
@Composable
private fun Rueckfrage(vm: AppViewModel) {
    val f = LocalFarben.current
    AnimatedVisibility(vm.rueckfrage, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f))
                .clickable(remember { MutableInteractionSource() }, null) { vm.rueckfrageSchliessen() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.padding(24.dp).widthIn(max = 440.dp).animateEnterExit(enter = scaleIn(spring(dampingRatio = 0.7f, stiffness = 380f), initialScale = 0.86f), exit = scaleOut(targetScale = 0.92f))
                    .glas(f, 30.dp, 2.4f, if (f.dunkel) Color(0xF51C140E) else Color(0xFAFFFFFF), toenung = f.primaer)
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(64.dp).knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.TravelExplore, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Text("Vollständiger Aktualisierungslauf", color = f.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
                Text(
                    "Möchten Sie einen vollständigen Aktualisierungslauf machen?",
                    color = f.text, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    "${vm.einstellungen.modell.label} · ${vm.einstellungen.modell.normalizeEffort(vm.einstellungen.denkstufe).label} sucht für alle ${Bereich.entries.size} Bereiche im Internet " +
                        "nach den aktuell besten lokalen Modellen. Das dauert einige Minuten und verbraucht ChatGPT-Kontingent. Der Bildschirm bleibt dabei an.",
                    color = f.textLeise, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
                )
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f).height(52.dp).glas(f, 16.dp, 0.6f, f.flaecheStark).antippen { vm.rueckfrageSchliessen() }, contentAlignment = Alignment.Center) {
                        Text("Nein", color = f.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.weight(1f).height(52.dp).knopf3d(f.primaer, f.sekundaer, 16.dp, f.dunkel).antippen { vm.vollstaendigerLauf() }, contentAlignment = Alignment.Center) {
                        Text("Ja, starten", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StartBildschirm(vm: AppViewModel, dunkel: Boolean) {
    val f = LocalFarben.current
    LazyVerticalGrid(
        GridCells.Adaptive(162.dp),
        Modifier.fillMaxSize().displayCutoutPadding(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "kopf", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text("Modell", color = f.textLeise, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
                        Text("Kompass", color = f.primaer, fontSize = 28.sp, fontWeight = FontWeight.Black, lineHeight = 30.sp)
                    }
                    if (vm.laufAktiv) RundKnopf(Icons.Rounded.Close, "Recherche abbrechen") { vm.abbrechen() }
                    else RundKnopf(Icons.Rounded.Refresh, "Aktualisieren") { vm.rueckfrageZeigen() }
                    RundKnopf(Icons.Rounded.Settings, "Einstellungen") { vm.einstellungenOeffnen() }
                    RundKnopf(if (dunkel) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, if (dunkel) "Heller Modus" else "Dunkler Modus") { vm.modusWechseln(dunkel) }
                }
            }
        }
        item(key = "held", span = { GridItemSpan(maxLineSpan) }) { Held(vm) }
        itemsIndexed(Bereich.entries, key = { _, b -> b.id }) { i, b -> BereichKarte(vm, b, i) }
        item(key = "fuss", span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.navigationBarsPadding().height(20.dp)) }
    }
}

/** Die große Karte oben: Kompass, Überblick und während eines Laufs der Fortschritt. */
@Composable
private fun Held(vm: AppViewModel) {
    val f = LocalFarben.current
    val anzahl = vm.ergebnisse.values.sumOf { it.modelle.size }
    val neuester = vm.ergebnisse.values.maxOfOrNull { it.stand }
    val anteil by animateFloatAsState(if (vm.laufAktiv && vm.laufGesamt > 0) vm.laufFertig / vm.laufGesamt.toFloat() else 0f, tween(500), label = "lauf")
    Row(
        Modifier.fillMaxWidth().einblenden().glas(f, 30.dp, 1.8f, toenung = f.primaer).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Kompass(118.dp, vm.laufAktiv, anteil)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text("Lokale KI für deine Grafikkarte", color = f.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
            Text(
                when {
                    vm.laufAktiv -> "Recherche läuft · ${vm.laufFertig} von ${vm.laufGesamt} fertig"
                    neuester != null -> "$anzahl Modelle in ${vm.ergebnisse.size} von ${Bereich.entries.size} Bereichen"
                    else -> "Tippe oben auf Aktualisieren – dann sucht ChatGPT im Internet nach den besten Modellen."
                },
                color = if (vm.laufAktiv) f.primaer else f.textLeise, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp),
            )
            if (vm.laufAktiv) {
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(10.dp).glas(f, 99.dp, 0f, f.textSchwach.copy(alpha = 0.22f), rand = false)) {
                    Box(Modifier.fillMaxWidth(anteil.coerceIn(0.03f, 1f)).fillMaxHeight().knopf3d(f.primaer, f.sekundaer, 99.dp, f.dunkel))
                }
            } else if (neuester != null) {
                Text("Stand ${standText(neuester)} Uhr", color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Text(
                "${vm.einstellungen.modell.label} · ${vm.einstellungen.modell.normalizeEffort(vm.einstellungen.denkstufe).label}",
                color = f.textSchwach, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * Animierter Kompass: atmender Glaskreis, kreisende Lichtpunkte, gestrichelter Ring. Die Nadel pendelt
 * ruhig um Norden; während einer Recherche dreht sie suchend und ein Ring zeigt den Fortschritt.
 */
@Composable
private fun Kompass(seite: Dp, sucht: Boolean, anteil: Float) {
    val f = LocalFarben.current
    val leben by animateFloatAsState(if (sucht) 1f else 0.35f, tween(900), label = "leben")
    val takt = rememberInfiniteTransition(label = "takt")
    val atem by takt.animateFloat(0f, 1f, infiniteRepeatable(tween(3600), RepeatMode.Reverse), label = "atem")
    val dreh by takt.animateFloat(0f, 360f, infiniteRepeatable(tween(36_000, easing = LinearEasing)), label = "dreh")
    val welle by takt.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "welle")
    val pendel by takt.animateFloat(-16f, 16f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pendel")
    val suche by takt.animateFloat(0f, 360f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "suche")
    Box(Modifier.size(seite), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val m = center
            val r = size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(f.primaer.copy(alpha = 0.12f + 0.2f * leben * atem), Color.Transparent), m, r), r, m)
            for (i in 0 until 3) {
                val p = (welle + i / 3f) % 1f
                drawCircle(f.sekundaer.copy(alpha = (1f - p) * 0.34f * leben), r * (0.62f + 0.38f * p), m, style = Stroke(1.5.dp.toPx()))
            }
            rotate(dreh, m) {
                drawCircle(
                    f.textSchwach.copy(alpha = 0.4f), r * 0.95f, m,
                    style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 8.dp.toPx()))),
                )
            }
            val punktFarben = listOf(f.primaer, f.sekundaer, f.tertiaer)
            for (i in 0 until 12) {
                // Ganzzahlige Umläufe, damit die Bewegung beim Neustart der Drehung nahtlos weiterläuft.
                val umlauf = if (i % 3 == 0) -1 else if (i % 3 == 1) 1 else 2
                val wkl = (i * 30f + dreh * umlauf) * PI.toFloat() / 180f
                val bahn = r * (0.80f + 0.03f * (i % 4))
                val funkeln = 0.5f + 0.5f * sin(wkl * 3f + i)
                drawCircle(
                    punktFarben[i % 3].copy(alpha = (0.25f + 0.55f * funkeln) * (0.45f + 0.55f * leben)),
                    (1.4f + 0.7f * (i % 3)).dp.toPx(),
                    Offset(m.x + cos(wkl) * bahn, m.y + sin(wkl) * bahn),
                )
            }
        }
        Box(Modifier.fillMaxSize(0.70f).glas(f, 999.dp, 2f))
        Canvas(Modifier.fillMaxSize(0.70f)) {
            val m = center
            val r = size.minDimension / 2
            val w = 5.dp.toPx()
            // Himmelsrichtungen als vier Striche am Rand.
            for (i in 0 until 4) rotate(i * 90f, m) {
                drawLine(f.textSchwach.copy(alpha = 0.7f), Offset(m.x, m.y - r + w * 1.6f), Offset(m.x, m.y - r + w * 3.2f), 1.5.dp.toPx(), StrokeCap.Round)
            }
            if (anteil > 0f) rotate(-90f, m) {
                drawArc(Brush.sweepGradient(listOf(f.sekundaer, f.primaer, f.sekundaer), m), 0f, 360f * anteil, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round))
            }
            val lang = r * 0.62f
            val breit = r * 0.15f
            rotate(if (sucht) suche else pendel, m) {
                val nord = Path().apply { moveTo(m.x, m.y - lang); lineTo(m.x + breit, m.y); lineTo(m.x - breit, m.y); close() }
                val sued = Path().apply { moveTo(m.x, m.y + lang); lineTo(m.x + breit, m.y); lineTo(m.x - breit, m.y); close() }
                drawPath(sued, f.textSchwach.copy(alpha = 0.55f))
                drawPath(nord, Brush.verticalGradient(listOf(f.sekundaer, f.primaer, f.tertiaer), m.y - lang, m.y))
                // Lichtkante links auf der Nordspitze: gibt der Nadel Tiefe.
                drawPath(Path().apply { moveTo(m.x, m.y - lang); lineTo(m.x, m.y); lineTo(m.x - breit, m.y); close() }, Color.White.copy(alpha = 0.22f))
            }
            drawCircle(Brush.radialGradient(listOf(Color.White, f.sekundaer), m, breit * 0.9f), breit * 0.7f, m)
        }
    }
}

@Composable
private fun BereichKarte(vm: AppViewModel, b: Bereich, index: Int) {
    val f = LocalFarben.current
    val e = vm.ergebnisse[b.id]
    val status = vm.status[b.id]
    val fehler = vm.fehler[b.id]
    Column(
        Modifier.fillMaxWidth().height(164.dp).einblenden(60 + (index % 6) * 45)
            .glas(f, 24.dp, 1.2f, toenung = if (e != null && e.modelle.isNotEmpty()) f.primaer else null)
            .antippen { vm.oeffne(b) }.padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).knopf3d(f.primaer, f.sekundaer, 14.dp, f.dunkel), contentAlignment = Alignment.Center) { Text(b.emoji, fontSize = 22.sp) }
            Spacer(Modifier.weight(1f))
            when {
                status != null -> CircularProgressIndicator(color = f.primaer, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                fehler != null -> Text("!", color = f.gefahr, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 6.dp))
                e != null && e.modelle.isNotEmpty() -> Box(Modifier.size(28.dp).glas(f, 99.dp, 0.4f, f.primaer.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Text("${e.modelle.size}", color = f.primaer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(b.titel, color = f.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.weight(1f))
        val beste = e?.modelle?.firstOrNull()
        Text(
            when {
                status != null -> status
                fehler != null -> "Fehlgeschlagen – antippen"
                beste != null -> beste.name
                e != null -> "Antwort als Text"
                else -> "Noch nicht recherchiert"
            },
            color = when { fehler != null && status == null -> f.gefahr; status != null || beste != null -> f.primaer; else -> f.textSchwach },
            fontSize = 13.sp, fontWeight = if (beste != null && status == null) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (e != null) Text("Stand ${standText(e.stand, "dd.MM.yyyy")}", color = f.textSchwach, fontSize = 11.sp, maxLines = 1)
    }
}
