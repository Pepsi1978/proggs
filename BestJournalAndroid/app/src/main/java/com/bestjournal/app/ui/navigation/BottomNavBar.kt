package com.bestjournal.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Retrospective :
        BottomNavItem("retrospective", "Rückblick", Icons.Rounded.AutoAwesome)

    data object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Rounded.Analytics)

    data object Journal : BottomNavItem("journal", "Tagebuch", Icons.Rounded.Book)

    data object Settings : BottomNavItem("settings", "Einstellungen", Icons.Rounded.Settings)
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items =
        listOf(
            BottomNavItem.Retrospective,
            BottomNavItem.Dashboard,
            BottomNavItem.Journal,
            BottomNavItem.Settings,
        )

    val activeIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val isDark = isSystemInDarkTheme()

    // Dark mode: orange accent — Light mode: teal accent
    val activeColor = if (isDark) Color(0xFFFFB74D) else Color(0xFF0097A7)
    val inactiveColor = if (isDark) Color(0xFF888888) else Color(0xFF999999)
    val activeBg = if (isDark) Color(0x20FFB74D) else Color(0x1A0097A7)

    val shape = RoundedCornerShape(28.dp)

    // Swipe gesture state
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier =
            modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .shadow(elevation = 8.dp, shape = shape)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> dragAccumulator += delta },
                    onDragStopped = {
                        if (dragAccumulator < -80f && activeIndex < items.lastIndex) {
                            onItemClick(items[activeIndex + 1])
                        } else if (dragAccumulator > 80f && activeIndex > 0) {
                            onItemClick(items[activeIndex - 1])
                        }
                        dragAccumulator = 0f
                    },
                )
    ) {
        val itemWidth = maxWidth / items.size

        // Animated indicator offset
        val indicatorOffset by
            animateDpAsState(
                targetValue = itemWidth * activeIndex,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                label = "indicatorOffset",
            )

        // Sliding active background pill
        Box(
            modifier =
                Modifier.offset(x = indicatorOffset)
                    .width(itemWidth)
                    .height(64.dp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(activeBg)
        )

        // Items
        Row(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            items.forEachIndexed { index, item ->
                val isSelected = index == activeIndex
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .clickable { onItemClick(item) }
                            .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(26.dp),
                        tint = if (isSelected) activeColor else inactiveColor,
                    )
                    AnimatedVisibility(visible = isSelected) {
                        Text(
                            text = item.title,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = activeColor,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
