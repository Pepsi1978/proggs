package de.frank.entropyreducer.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.components.MicButton
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Bottom-Bar mit 4 Tabs + zentralem Mic-Button (FAB-Style).
 * Mic-Button uebersteht die Tabs und ist global verfuegbar.
 */
@Composable
fun CosmosBottomBar(
    currentTab: String,
    micState: MicState,
    onTabSelected: (String) -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    // BottomBar (Frank-Wunsch 2026-05-09): das graue Bar-Background MUSS bis zum
    // physischen Bildschirm-Rand reichen — vorher war unter den Tabs ein
    // transparenter Streifen wo Googles Geste-Indikator durchschien. Jetzt:
    // Background auf der GANZEN Box (72dp Tabs + bottomInset Geste-Streifen),
    // die innere Tab-Row sitzt nur in den oberen 72dp, der Bereich darunter
    // (System-Geste-Inset) zeigt den gleichen grauen Background — kein
    // optisch abgeschnittenes Ende mehr.
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barBg = if (cosmos.isDark) {
        CosmosColors.BgDarkAccent.copy(alpha = 0.92f)
    } else {
        CosmosColors.BgLightAccent.copy(alpha = 0.92f)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp + bottomInset)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(barBg),
    ) {
        // Innere Box haelt Tabs + Mic-Button in den oberen 72dp — der untere
        // Streifen (Geste-Inset) bleibt nur als grauer Background sichtbar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TabItem(
                    label = "Aufgaben",
                    icon = Icons.Outlined.Checklist,
                    selected = currentTab == Routes.TASKS,
                    onClick = { onTabSelected(Routes.TASKS) },
                )
                TabItem(
                    label = "Analyse",
                    icon = Icons.Outlined.Analytics,
                    selected = currentTab == Routes.ANALYSIS,
                    onClick = { onTabSelected(Routes.ANALYSIS) },
                )
                // Luecke für den Mic-Button
                Spacer(Modifier.width(64.dp))
                TabItem(
                    label = "Forscher",
                    icon = Icons.Outlined.Science,
                    selected = currentTab == Routes.SCIENTIST,
                    onClick = { onTabSelected(Routes.SCIENTIST) },
                )
                TabItem(
                    label = "Biomarker",
                    icon = Icons.Outlined.MonitorHeart,
                    selected = currentTab == Routes.BIOMARKER,
                    onClick = { onTabSelected(Routes.BIOMARKER) },
                )
            }
            MicButton(
                state = micState,
                onClick = onMicClick,
                size = 56.dp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun TabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val tint = if (selected) CosmosColors.AccentPrimary else cosmos.textSecondary
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}
