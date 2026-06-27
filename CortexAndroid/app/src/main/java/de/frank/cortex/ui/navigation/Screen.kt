package de.frank.cortex.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Chat : Screen("chat", "Gespräch", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Filled.Chat)
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Equalizer, Icons.Default.Equalizer)
    data object Settings : Screen("settings", "Einstellungen", Icons.Default.Settings, Icons.Default.Settings)

    companion object {
        val bottomScreens = listOf(Chat, Dashboard, Settings)
    }
}
