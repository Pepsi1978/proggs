package com.entropyjournal.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entropyjournal.ui.theme.LocalJournalDesignTokens
import com.entropyjournal.util.rememberHapticAction

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    data object Retrospective :
        BottomNavItem("retrospective", "Rückblick", Icons.Rounded.AutoAwesome)

    data object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Rounded.Analytics)

    data object Journal : BottomNavItem("journal", "Tagebuch", Icons.Rounded.Book)

    data object Settings : BottomNavItem("settings", "Einstellungen", Icons.Rounded.Settings)
}

@Composable
fun BottomNavBar(currentRoute: String?, onItemClick: (BottomNavItem) -> Unit) {
    val doHaptic = rememberHapticAction()
    val tokens = LocalJournalDesignTokens.current
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
    val navShape = RoundedCornerShape(26.dp)
    val navBackground = tokens.navigationBackground
    val gold = MaterialTheme.colorScheme.primary
    val copper = MaterialTheme.colorScheme.secondary
    val muted = tokens.textMuted
    val border = tokens.cardBorder

    Box(
        modifier =
            Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
                .pointerInput(currentIndex) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragAccumulator = 0f
                            swipeHandled = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (swipeHandled) return@detectHorizontalDragGestures
                            dragAccumulator += dragAmount
                            if (dragAccumulator < -100f && currentIndex < items.lastIndex) {
                                swipeHandled = true
                                doHaptic(HapticFeedbackType.TextHandleMove)
                                onItemClick(items[currentIndex + 1])
                            } else if (dragAccumulator > 100f && currentIndex > 0) {
                                swipeHandled = true
                                doHaptic(HapticFeedbackType.TextHandleMove)
                                onItemClick(items[currentIndex - 1])
                            }
                        },
                    )
                }
                .shadow(
                    elevation = 16.dp,
                    shape = navShape,
                    ambientColor = Color.Black.copy(alpha = 0.32f),
                    spotColor = Color.Black.copy(alpha = 0.5f),
                )
                .clip(navShape)
                .background(navBackground)
                .border(1.dp, border, navShape)
                .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val itemShape = RoundedCornerShape(18.dp)
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .clip(itemShape)
                            .then(
                                if (selected) {
                                    Modifier.background(
                                            Brush.linearGradient(
                                                listOf(
                                                    gold.copy(alpha = 0.2f),
                                                    copper.copy(alpha = 0.12f),
                                                )
                                            )
                                        )
                                        .border(1.dp, border, itemShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                doHaptic(HapticFeedbackType.TextHandleMove)
                                onItemClick(item)
                            }
                            .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (selected) gold else muted,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = item.title,
                        color = if (selected) gold else muted,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
