package de.frank.entropyreducer.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            // System-Navigation-Bar nicht ueberdecken — windowInsetsPadding sorgt
            // dafuer dass die BottomBar oberhalb der Geraete-Gesten/Navigation-Bar liegt.
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(96.dp),
    ) {
        // Bar mit den 4 Tabs (Mic-Lücke in der Mitte)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(cosmos.glassBg)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .align(Alignment.BottomCenter),
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
            // Luecke fuer den Mic-Button
            Spacer(Modifier.width(72.dp))
            TabItem(
                label = "Wissenschaftler",
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
        // Mic-Button mittig, schwebend (FAB-Style)
        MicButton(
            state = micState,
            onClick = onMicClick,
            size = 64.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp),
        )
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
            )
        }
    }
}
