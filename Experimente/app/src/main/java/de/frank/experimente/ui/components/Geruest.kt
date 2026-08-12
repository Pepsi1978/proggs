package de.frank.experimente.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import de.frank.experimente.ui.Ziel
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.LocalEffektstufe
import de.frank.experimente.ui.theme.LocalFarben
import de.frank.experimente.ui.theme.dauer
import de.frank.experimente.ui.theme.filmkorn
import de.frank.experimente.ui.theme.lichtgrund

/**
 * Das Gerüst, das jeder Bildschirm des Entwurfs teilt.
 *
 * Von unten nach oben: der Lichtgrund (`E-01`) mit dem Filmkorn (`E-02`) · der scrollende
 * Inhalt · die Glas-Kopfleiste (`E-03`, 64 dp) · die schwebende untere Leiste ·
 * darüber alles, was der Bildschirm zusätzlich auflegt (Plus-Knopf, Störung, Hinweis,
 * Anlegefläche).
 *
 * Der Inhalt beginnt im Entwurf 16 dp unter der Kopfleiste und endet 120 dp über der Kante,
 * damit die schwebende Leiste nichts verdeckt.
 */
@Composable
fun Bildschirmgeruest(
    kopf: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    kopfLinks: (@Composable RowScope.() -> Unit)? = null,
    kopfInnen: androidx.compose.ui.unit.Dp = 16.dp,
    mitUnterkante: Boolean = false,
    leiste: Ziel? = null,
    beiLeistenwahl: ((Ziel) -> Unit)? = null,
    unterKopf: (@Composable ColumnScope.() -> Unit)? = null,
    ueberlagerung: (@Composable BoxScope.() -> Unit)? = null,
    inhalt: LazyListScope.() -> Unit,
) {
    val farben = LocalFarben.current
    val stufe = LocalEffektstufe.current
    val liste = rememberLazyListState()

    Box(
        modifier
            .fillMaxSize()
            .lichtgrund(farben, stufe)
            .filmkorn(stufe),
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            Kopfzeile(
                innenAbstand = kopfInnen,
                mitUnterkante = mitUnterkante,
                links = kopfLinks,
                inhalt = kopf,
            )
            unterKopf?.let { Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { it() } }
            LazyColumn(
                state = liste,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = if (leiste == null) 40.dp else 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = inhalt,
            )
            if (leiste != null && beiLeistenwahl != null) {
                UntereLeiste(jetzt = leiste, beiWahl = beiLeistenwahl)
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            ueberlagerung?.invoke(this)
        }
    }
}

/**
 * Der schwebende Plus-Knopf sitzt im Entwurf 24 dp von rechts und 100 dp von unten — also
 * genau über der schwebenden Leiste.
 */
@Composable
fun BoxScope.SchwebenderPlusknopf(beschriftung: String, beiKlick: () -> Unit) {
    Box(Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 100.dp)) {
        Plusknopf(beschriftung, beiKlick)
    }
}

/**
 * Die Störungsmeldung liegt im Entwurf direkt unter der Kopfleiste, 16 dp vom Rand.
 * Der Hinweis („Reihenfolge geändert.“) sitzt unten, 92 dp über der Kante.
 */
@Composable
fun BoxScope.Meldungen(
    stoerung: String?,
    beiNochmal: () -> Unit,
    hinweis: String? = null,
) {
    val stufe = LocalEffektstufe.current
    if (stoerung != null) {
        val sichtbar by androidx.compose.animation.core.animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(dauer(220, stufe), easing = Bewegung.ruhig),
            label = "stoerung",
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 68.dp, start = 16.dp, end = 16.dp)
                .alpha(sichtbar),
        ) {
            Stoerung(stoerung, beiNochmal)
        }
    }
    if (hinweis != null) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 92.dp),
        ) {
            Hinweiszeile(hinweis)
        }
    }
}

/**
 * **E-10 · M-95 — gestaffeltes Erscheinen.** Listen bauen sich mit 60 ms Versatz je Eintrag
 * auf; jede Karte kommt in 240 ms mit `cubic-bezier(.2, 0, 0, 1)` von 10 dp unten herein
 * (`@keyframes einfliegen`). Der Entwurf staffelt bis zum fünften Eintrag.
 *
 * Rückfallebene auf der Stufe *Aus*: alles erscheint gleichzeitig.
 */
@Composable
fun Einflug(nr: Int, modifier: Modifier = Modifier, inhalt: @Composable () -> Unit) {
    val stufe = LocalEffektstufe.current
    val versatz = remember(nr) { nr.coerceAtMost(5) * Bewegung.STAFFEL_VERSATZ }
    var angekommen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { angekommen = true }

    val laenge = dauer(Bewegung.LANG, stufe)
    val fortschritt by animateFloatAsState(
        targetValue = if (angekommen) 1f else 0f,
        animationSpec = tween(laenge, delayMillis = if (laenge == 0) 0 else versatz, easing = Bewegung.ruhig),
        label = "einfliegen",
    )
    Box(
        modifier.graphicsLayer {
            alpha = fortschritt
            translationY = (1f - fortschritt) * 10.dp.toPx()
        },
    ) {
        inhalt()
    }
}
