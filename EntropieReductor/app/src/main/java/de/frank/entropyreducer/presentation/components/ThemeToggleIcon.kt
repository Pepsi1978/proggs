package de.frank.entropyreducer.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import de.frank.entropyreducer.data.settings.ThemeMode
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Hell-/Dunkel-Toggle wie in BestJournalAndroid: zyklisch durch
 * SYSTEM → LIGHT → DARK → SYSTEM. Symbol passt sich an den aktuellen Modus an.
 */
@Composable
fun ThemeToggleIcon(
    current: ThemeMode,
    onCycle: (ThemeMode) -> Unit,
) {
    val cosmos = LocalCosmos.current
    val (icon, label) = when (current) {
        ThemeMode.SYSTEM -> Icons.Outlined.Brightness4 to "Auto (System)"
        ThemeMode.LIGHT -> Icons.Outlined.LightMode to "Hell-Modus"
        ThemeMode.DARK -> Icons.Outlined.DarkMode to "Dunkel-Modus"
    }
    IconButton(onClick = {
        onCycle(
            when (current) {
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
            }
        )
    }) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = cosmos.textPrimary,
        )
    }
}
