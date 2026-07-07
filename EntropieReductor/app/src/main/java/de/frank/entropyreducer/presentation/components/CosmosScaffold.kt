package de.frank.entropyreducer.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Ambient-Flag: ist dieser Screen unter einer SubTabRow eingebettet?
 *
 * Frank-Wunsch 2026-06-23: Die SubTabRow uebernimmt selbst das Statusbar-Padding und sitzt ganz
 * oben. Die darunter liegenden Screen-Koepfe (TasksScreen, AnalysisScreen, ...) duerfen das
 * Statusbar-Padding NICHT nochmal anwenden — sonst entsteht ~1 cm Luft zwischen Tab-Leiste und
 * Screen-Kopf. Setzt AppNavGraph pro Pager-Host auf true; Default false laesst alle Detail-Screens
 * (EntryDetail, Settings, …) unveraendert.
 *
 * Wenn true: TopAppBar rendert ohne Statusbar-Insets und in kompakter Hoehe (40dp), damit der
 * Screen-Kopf mit ~2 mm Abstand direkt unter der Tab-Leiste klebt.
 */
val LocalBelowSubTabRow = compositionLocalOf { false }

/**
 * Wrapper-Scaffold mit transparenter Top-Bar — Hintergrund kommt vom Theme.
 *
 * Wichtig (Frank-Reklamation 2026-05-08, "Bildschirm immer noch abgeschnitten"): Die BottomBar wird
 * NICHT mehr im Scaffold-`bottomBar`-Slot gerendert (der den Content-Bereich nach oben drueckt und
 * so eine sichtbare Luft-Zone erzeugt). Stattdessen sitzt sie als transparenter Overlay am unteren
 * Rand der Box — der Content (LazyColumn etc.) hat den vollen Bildschirm zur Verfuegung und scrollt
 * unter der BottomBar durch. Der Aufrufer muss in seinem `contentPadding` einen unteren Wert (mind.
 * 96.dp) reservieren damit das letzte Item nicht von der BottomBar verdeckt wird.
 *
 * `compactHeader` (Frank-Wunsch 2026-05-09 — vierte Praezisierung): Reduziert die TopAppBar-Hoehe
 * von Material-3-Default 64dp auf 44dp, damit der Titel "Entropie Reduktor" deutlich naeher am
 * darunterliegenden Inhalt sitzt. Die ~18dp "unsichtbare Luft" unter dem vertikal zentrierten Titel
 * (Material-3- Spec) waren der eigentliche Uebeltaeter — kein Spacer, sondern die TopAppBar selbst.
 *
 * Frank-Wunsch 2026-06-23: `compactHeader` wurde frueher deklariert aber NICHT angewendet (Bug).
 * Jetzt wird es wirksam: expandedHeight = 44.dp. Zusaetzlich greift [LocalBelowSubTabRow]: ist der
 * Screen unter einer SubTabRow eingebettet, entfaellt das doppelte Statusbar-Padding der TopAppBar
 * und die Hoehe schrumpft auf 40 dp — so sitzt der Screen-Kopf mit ~2 mm Abstand direkt unter der
 * Tab-Leiste (statt ~1 cm Luft vorher).
 */
@Composable
fun CosmosScaffold(
    title: String,
    actions: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    showBottomBar: Boolean = true,
    showTopBar: Boolean = true,
    compactHeader: Boolean = false,
    /**
     * Optionaler Slot direkt rechts NEBEN dem Titel-Text (Frank-Wunsch 2026-05-23). Gedacht fuer
     * Kontext-Knoepfe die nicht in den allgemeinen Actions-Bereich gehoeren — z.B. der
     * Heute-Schnellzugriff im Aufgaben-Tab. Bleibt leer wenn nicht gesetzt — bestehende Caller
     * bleiben unveraendert.
     */
    titleEndContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val cosmos = LocalCosmos.current
    // Unter SubTabRow: kein doppeltes Statusbar-Padding + sehr kompakte Hoehe (~2 mm Abstand zur
    // Tab-Leiste). Sonst: Default-Statusbar-Insets + ggf. compactHeader (44dp).
    val belowSubTab = LocalBelowSubTabRow.current
    val topBarHeight = if (belowSubTab) 40.dp else if (compactHeader) 44.dp else TopAppBarDefaults.TopAppBarExpandedHeight
    val topBarInsets = if (belowSubTab) WindowInsets(0, 0, 0, 0) else TopAppBarDefaults.windowInsets
    val contentInsets =
        if (belowSubTab) WindowInsets(0, 0, 0, 0) else WindowInsets.systemBars
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = contentInsets,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                color = cosmos.textPrimary,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            titleEndContent()
                        }
                    },
                    navigationIcon = navigationIcon,
                    actions = { actions() },
                    expandedHeight = topBarHeight,
                    windowInsets = topBarInsets,
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = cosmos.textPrimary,
                            navigationIconContentColor = cosmos.textPrimary,
                            actionIconContentColor = cosmos.textPrimary,
                        ),
                )
            }
        },
        // bottomBar bleibt LEER damit der Scaffold-Padding-Slot keinen Platz
        // dafuer reserviert. Die BottomBar wird unten als Overlay gerendert.
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            content(padding)
            // BottomBar als Overlay am unteren Rand — nur anzeigen wenn showBottomBar = true
            if (showBottomBar) {
                Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) { bottomBar() }
            }
        }
    }
}
