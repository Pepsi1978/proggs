package de.frank.cortex.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.frank.cortex.ui.chat.ChatScreen
import de.frank.cortex.ui.dashboard.DashboardScreen
import de.frank.cortex.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route,
        modifier = modifier
    ) {
        composable(Screen.Chat.route) { ChatScreen() }
        composable(Screen.Dashboard.route) { DashboardScreen() }
        composable(Screen.Settings.route) { SettingsScreen(isDark = isDark, onToggleTheme = onToggleTheme) }
    }
}
