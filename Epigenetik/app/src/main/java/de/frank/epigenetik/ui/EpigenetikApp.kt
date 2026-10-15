package de.frank.epigenetik.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalLibrary
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.epigenetik.ui.theme.Design
import de.frank.epigenetik.ui.theme.EpigenetikTheme
import de.frank.epigenetik.ui.theme.LocalFarben
import de.frank.epigenetik.ui.theme.antippen
import de.frank.epigenetik.ui.theme.glas
import de.frank.epigenetik.ui.theme.knopf3d
import kotlinx.coroutines.delay

@Composable
fun EpigenetikApp(vm: AppViewModel, activity: ComponentActivity) {
    EpigenetikTheme(vm.einstellungen.design, vm.einstellungen.modus) {
        Box(Modifier.fillMaxSize()) {
            Hintergrund(Modifier.fillMaxSize())
            BackHandler(vm.seite != Seite.Haupt || vm.tab != Tab.START) { if (!vm.zurueck()) vm.zeigeTab(Tab.START) }
            AnimatedContent(
                vm.seite,
                transitionSpec = {
                    val vor = targetState != Seite.Haupt
                    if (vor) (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 420f)) { it / 3 } + fadeIn(tween(220)) + scaleIn(initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(160)) + scaleOut(targetScale = 0.97f))
                    else (fadeIn(tween(220)) + scaleIn(initialScale = 0.97f))
                        .togetherWith(slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(180)))
                },
                label = "seite",
            ) { s ->
                when (s) {
                    Seite.Haupt -> Haupt(vm)
                    is Seite.Detail -> DetailBildschirm(vm, s.id)
                    is Seite.SchalterSeite -> SchalterBildschirm(vm, s.id)
                    Seite.Einstellungen -> EinstellungenBildschirm(vm, activity)
                }
            }
            Meldung(vm, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun Haupt(vm: AppViewModel) {
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(vm.tab, transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(140))) }, label = "tab") { t ->
            when (t) {
                Tab.START -> StartBildschirm(vm)
                Tab.WISSEN -> WissenBildschirm(vm)
                Tab.ATLAS -> AtlasBildschirm(vm)
                Tab.ZIELE -> ZieleBildschirm(vm)
                Tab.STACK -> StackBildschirm(vm)
            }
        }
        TabLeiste(vm, Modifier.align(Alignment.BottomCenter))
    }
}

/** Gemeinsames Gerüst einer Hauptseite: Kopfzeile oben, Inhalt mittig begrenzt (für das aufgeklappte Fold). */
@Composable
fun Seitenliste(vm: AppViewModel, titel: String, untertitel: String, inhalt: LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 760.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "kopf") { Kopf(vm, titel, untertitel) }
            inhalt()
        }
    }
}

@Composable
fun Kopf(vm: AppViewModel, titel: String, untertitel: String) {
    val f = LocalFarben.current
    val e = vm.einstellungen
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(untertitel, color = f.textLeise, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                titel, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1,
                style = TextStyle(brush = Brush.linearGradient(listOf(f.text, f.primaer))),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RundKnopf(Icons.Rounded.Palette, "Design wechseln", 40.dp) {
                val alle = Design.entries
                val neu = alle[(alle.indexOf(Design.von(e.design)) + 1) % alle.size]
                e.design = neu.id
                vm.melde("Design: ${neu.anzeige}")
            }
            val modus = e.modus
            RundKnopf(
                when (modus) { "hell" -> Icons.Rounded.LightMode; "dunkel" -> Icons.Rounded.DarkMode; else -> Icons.Rounded.BrightnessAuto },
                "Hell oder Dunkel", 40.dp,
            ) {
                val neu = when (modus) { "hell" -> "system"; "system" -> "dunkel"; else -> "hell" }
                e.modus = neu
                vm.melde(when (neu) { "hell" -> "Hell"; "dunkel" -> "Dunkel"; else -> "Automatisch wie das System" })
            }
            RundKnopf(Icons.Rounded.Settings, "Einstellungen", 40.dp) { vm.oeffne(Seite.Einstellungen) }
        }
    }
}

@Composable
private fun TabLeiste(vm: AppViewModel, modifier: Modifier) {
    val f = LocalFarben.current
    val laufend = vm.recherchen.collectAsState().value.count { it.offen }
    Row(
        modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp).widthIn(max = 560.dp).fillMaxWidth()
            .glas(f, 30.dp, 1.6f, f.flaecheStark).padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Tab.entries.forEach { t ->
            val aktiv = vm.tab == t
            val icon: ImageVector = when (t) {
                Tab.START -> Icons.Rounded.AutoAwesome
                Tab.WISSEN -> Icons.Rounded.LocalLibrary
                Tab.ATLAS -> Icons.Rounded.Hub
                Tab.ZIELE -> Icons.Rounded.TrackChanges
                Tab.STACK -> Icons.Rounded.Science
            }
            Column(
                Modifier.weight(1f).height(54.dp)
                    .then(if (aktiv) Modifier.knopf3d(f.primaer, f.tertiaer, 24.dp, f.dunkel) else Modifier)
                    .antippen { vm.zeigeTab(t) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Box {
                    Icon(icon, t.titel, tint = if (aktiv) Color.White else f.textLeise, modifier = Modifier.size(22.dp))
                    if (t == Tab.START && laufend > 0) Box(Modifier.align(Alignment.TopEnd).padding(start = 14.dp).size(8.dp).glas(f, 99.dp, 0f, f.sekundaer, rand = false))
                }
                Spacer(Modifier.height(2.dp))
                Text(t.titel, color = if (aktiv) Color.White else f.textLeise, fontSize = 11.sp, fontWeight = if (aktiv) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun Meldung(vm: AppViewModel, modifier: Modifier) {
    val f = LocalFarben.current
    val text = vm.meldung
    LaunchedEffect(text) { if (text != null) { delay(2800); vm.meldungWeg() } }
    AnimatedVisibility(text != null, modifier, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
        Box(Modifier.navigationBarsPadding().padding(bottom = 92.dp, start = 24.dp, end = 24.dp)) {
            Text(
                text.orEmpty(), color = f.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.glas(f, 99.dp, 1.4f, f.flaecheStark, toenung = f.primaer).padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }
}
