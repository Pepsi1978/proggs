package de.frank.jarvis.bio.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.bio.presentation.theme.LocalCosmos
import de.frank.jarvis.ui.theme.glas

/** Optionaler Hintergrund für alle [GlassCard]s eines Teilbaums (aus Entropie Reductor; in Jarvis ungenutzt = Standard). */
val LocalCardBackgroundOverride = compositionLocalOf<Color?> { null }

/**
 * Karte der Biomarker-Bildschirme. Gleiche Schnittstelle wie in Entropie Reductor, gezeichnet mit Jarvis' Glas ([glas]).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    tintColor: Color? = null,
    tintBrush: Brush? = null,
    backgroundOverride: Color? = null,
    content: @Composable () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val f = cosmos.farben
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val fuellung = backgroundOverride ?: LocalCardBackgroundOverride.current ?: f.flaeche
    val diagonal = remember(tintColor) {
        tintColor?.let { Brush.linearGradient(listOf(Color.Transparent, it), Offset(0f, Float.POSITIVE_INFINITY), Offset(Float.POSITIVE_INFINITY, 0f)) }
    }
    val ton = tintBrush ?: diagonal
    Box(
        modifier
            .glas(f, cornerRadius, erhoeht = 0.7f, fuellung = fuellung)
            .then(if (ton != null) Modifier.background(ton, shape) else Modifier)
            .padding(contentPadding),
    ) { content() }
}

/**
 * Bildschirmrahmen mit schlichter, durchsichtiger Kopfzeile. Die Systemleisten-Abstände setzt der Biomarker-Bereich einmal
 * außen (in JarvisApp), deshalb hier keine Insets. `bottomBar`/`showBottomBar` bleiben nur der Schnittstelle wegen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmosScaffold(
    title: String,
    actions: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") bottomBar: @Composable () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") showBottomBar: Boolean = false,
    showTopBar: Boolean = true,
    compactHeader: Boolean = false,
    titleEndContent: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val cosmos = LocalCosmos.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, color = cosmos.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            titleEndContent()
                        }
                    },
                    navigationIcon = navigationIcon,
                    actions = { actions() },
                    expandedHeight = if (compactHeader) 48.dp else 56.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = cosmos.textPrimary,
                        navigationIconContentColor = cosmos.textPrimary,
                        actionIconContentColor = cosmos.textPrimary,
                    ),
                )
            }
        },
    ) { padding -> Box(Modifier.fillMaxSize()) { content(padding) } }
}

/**
 * Kartenfarben-Wahl aus Entropie Reductor: in Jarvis bewusst entfallen (reine Optik, Jarvis hat ein festes Design).
 * Bleibt als leerer Baustein, damit die übernommenen Bildschirme unverändert bleiben.
 */
@Composable
fun ColorPaletteBar(
    @Suppress("UNUSED_PARAMETER") selectedIndex: Int,
    @Suppress("UNUSED_PARAMETER") onPick: (Int) -> Unit,
    @Suppress("UNUSED_PARAMETER") modifier: Modifier = Modifier,
) = Unit

/** Gegenstück zu [ColorPaletteBar]: liefert immer die Standardfarbe, speichert nichts. */
class KartenFarben {
    fun colorFor(@Suppress("UNUSED_PARAMETER") cardId: String, @Suppress("UNUSED_PARAMETER") isDark: Boolean): Int = 0
    fun setColor(@Suppress("UNUSED_PARAMETER") cardId: String, @Suppress("UNUSED_PARAMETER") index: Int, @Suppress("UNUSED_PARAMETER") isDark: Boolean) = Unit
}

@Composable
fun rememberCardColors(): KartenFarben = remember { KartenFarben() }
