package com.bestjournal.app.ui.navigation

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import com.bestjournal.app.util.rememberHapticAction

sealed class BottomNavItem(val route: String, @androidx.annotation.StringRes val titleRes: Int, val icon: ImageVector) {
    data object Retrospective :
        BottomNavItem("retrospective", com.bestjournal.app.R.string.nav_retrospective, Icons.Rounded.AutoAwesome)

    data object Dashboard : BottomNavItem("dashboard", com.bestjournal.app.R.string.nav_dashboard, Icons.Rounded.Analytics)

    data object Journal : BottomNavItem("journal", com.bestjournal.app.R.string.nav_journal, Icons.Rounded.Book)

    data object Settings : BottomNavItem("settings", com.bestjournal.app.R.string.nav_settings, Icons.Rounded.Settings)
}

@Composable
fun BottomNavBar(currentRoute: String?, onItemClick: (BottomNavItem) -> Unit) {
    val doHaptic = rememberHapticAction()
    val items =
        listOf(
            BottomNavItem.Retrospective,
            BottomNavItem.Dashboard,
            BottomNavItem.Journal,
            BottomNavItem.Settings,
        )

    val currentIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var swipeHandled by remember { mutableStateOf(false) }
    val swipeThreshold = 100f

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.pointerInput(currentIndex) {
            detectHorizontalDragGestures(
                onDragStart = {
                    dragAccumulator = 0f
                    swipeHandled = false
                },
                onHorizontalDrag = { _, dragAmount ->
                    if (swipeHandled) return@detectHorizontalDragGestures
                    dragAccumulator += dragAmount
                    if (dragAccumulator < -swipeThreshold && currentIndex < items.lastIndex) {
                        swipeHandled = true
                        doHaptic(HapticFeedbackType.TextHandleMove)
                        onItemClick(items[currentIndex + 1])
                    } else if (dragAccumulator > swipeThreshold && currentIndex > 0) {
                        swipeHandled = true
                        doHaptic(HapticFeedbackType.TextHandleMove)
                        onItemClick(items[currentIndex - 1])
                    }
                },
            )
        },
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    doHaptic(HapticFeedbackType.TextHandleMove)
                    onItemClick(item)
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.titleRes),
                        tint =
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                    )
                },
                label = {
                    // maxLines + Ellipsis is the structural safety net (Poka-Yoke
                    // level 3): even if a future translation is unexpectedly long,
                    // the tab stays one line so the icon never jumps up.
                    Text(
                        text = stringResource(item.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color =
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                    )
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
            )
        }
    }
}
