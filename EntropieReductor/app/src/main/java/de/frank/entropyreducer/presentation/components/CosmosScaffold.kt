package de.frank.entropyreducer.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.LocalCosmos

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
 * Wird aktuell nur vom Aufgaben-Screen genutzt; alle anderen Screens behalten die Default-Hoehe.
 */
@Composable
fun CosmosScaffold(
    title: String,
    actions: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
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
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
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
                expandedHeight =
                    if (compactHeader) 44.dp else TopAppBarDefaults.TopAppBarExpandedHeight,
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = cosmos.textPrimary,
                        navigationIconContentColor = cosmos.textPrimary,
                        actionIconContentColor = cosmos.textPrimary,
                    ),
            )
        },
        // bottomBar bleibt LEER damit der Scaffold-Padding-Slot keinen Platz
        // dafuer reserviert. Die BottomBar wird unten als Overlay gerendert.
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            content(padding)
            // BottomBar als Overlay am unteren Rand — schwebt UEBER dem Content
            // mit transparentem Hintergrund (Glas-Effekt vom GlassCard-Stil).
            // Der Content scrollt darunter weiter, exakt wie Frank es vorgeschlagen hat.
            Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) { bottomBar() }
        }
    }
}
