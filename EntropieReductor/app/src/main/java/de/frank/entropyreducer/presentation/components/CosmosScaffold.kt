package de.frank.entropyreducer.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Wrapper-Scaffold mit transparenter Top-Bar — Hintergrund kommt vom Theme.
 *
 * Wichtig: Scaffold liefert das `padding` an den Content-Block. Frueher wurde
 * der Content zusaetzlich in einer Box mit `Modifier.padding(padding)` verpackt
 * UND nochmal an den Aufrufer durchgereicht — das Ergebnis war doppeltes Padding
 * (oben Statusbar-Insets + TopAppBar-Hoehe doppelt, unten BottomBar-Hoehe doppelt).
 * Sichtbar als grosse Leerflaeche oben + abgeschnittene BottomBar unten.
 *
 * Fix: Scaffold uebergibt `padding` einmalig an den Content-Lambda, der
 * Aufrufer (z.B. TasksScreen) entscheidet wie er das Padding anwendet — meist
 * via `Modifier.padding(padding)` auf seinem aeusseren Container.
 */
@Composable
fun CosmosScaffold(
    title: String,
    actions: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val cosmos = LocalCosmos.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = cosmos.textPrimary,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = navigationIcon,
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = cosmos.textPrimary,
                    navigationIconContentColor = cosmos.textPrimary,
                    actionIconContentColor = cosmos.textPrimary,
                ),
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        content(padding)
    }
}
