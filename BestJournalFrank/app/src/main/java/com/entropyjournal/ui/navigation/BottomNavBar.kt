package com.entropyjournal.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Retrospective :
        BottomNavItem("retrospective", "Rückblick", Icons.Rounded.AutoAwesome)

    data object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Rounded.Analytics)

    data object Journal : BottomNavItem("journal", "Tagebuch", Icons.Rounded.Book)

    data object Settings : BottomNavItem("settings", "Einstellungen", Icons.Rounded.Settings)
}

@Composable
fun BottomNavBar(currentRoute: String?, onItemClick: (BottomNavItem) -> Unit) {
    val items =
        listOf(
            BottomNavItem.Retrospective,
            BottomNavItem.Dashboard,
            BottomNavItem.Journal,
            BottomNavItem.Settings,
        )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint =
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        color =
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                    )
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
            )
        }
    }
}
