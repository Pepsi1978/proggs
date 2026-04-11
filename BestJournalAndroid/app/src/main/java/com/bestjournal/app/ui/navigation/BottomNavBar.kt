package com.bestjournal.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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

    val activeColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFE07830)
    val inactiveColor = if (isDark) Color(0xFF555555) else Color(0xFFBBBBBB)
    val shape = RoundedCornerShape(24.dp)

    BoxWithConstraints(
        modifier =
            modifier
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .shadow(elevation = 8.dp, shape = shape)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
    ) {
        val itemWidth = maxWidth / items.size

        // Animated indicator offset
        val indicatorOffset by
            animateDpAsState(
                targetValue = itemWidth * activeIndex,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                label = "indicatorOffset",
            )

        // Items
        Row(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                val isSelected = index == activeIndex
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .clickable { onItemClick(item) }
                            .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(24.dp),
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

        // Sliding orange gradient bar at bottom
        Box(
            modifier =
                Modifier.align(Alignment.BottomStart)
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .height(3.dp)
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFFE07830), Color(0xFFFFB74D)))
                    )
        )
    }
}
