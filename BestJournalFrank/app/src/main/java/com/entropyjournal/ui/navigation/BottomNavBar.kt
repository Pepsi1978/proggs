package com.entropyjournal.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

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

    val isDark = isSystemInDarkTheme()

    // Metallic gradient brushes for icon tinting
    val metallicSelectedBrush =
        Brush.verticalGradient(
            colors =
                if (isDark) {
                    listOf(Color(0xFFCCCCCC), Color(0xFFEEEEEE), Color(0xFFAAAAAA))
                } else {
                    listOf(Color(0xFF2A2A2A), Color(0xFF585858), Color(0xFF1A1A1A))
                }
        )
    val metallicUnselectedBrush =
        Brush.verticalGradient(
            colors =
                if (isDark) {
                    listOf(Color(0xFF666666), Color(0xFF888888), Color(0xFF555555))
                } else {
                    listOf(Color(0xFF9A9A9A), Color(0xFFBBBBBB), Color(0xFF888888))
                }
        )
    val metallicSelectedColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF1C1C1E)
    val metallicUnselectedColor = if (isDark) Color(0xFF777777) else Color(0xFF9E9E9E)

    // Brushed metal background colors
    val metalBaseGradient =
        if (isDark) {
            listOf(Color(0xFF3A3A3C), Color(0xFF2C2C2E), Color(0xFF38383A))
        } else {
            listOf(Color(0xFFD6D6DA), Color(0xFFC8C8CC), Color(0xFFD2D2D6))
        }
    val brushLineColor = if (isDark) Color.White else Color.White
    val brushLineAlphaBase = if (isDark) 0.04f else 0.25f
    val brushLineDarkColor = if (isDark) Color.Black else Color.Black
    val brushLineDarkAlpha = if (isDark) 0.08f else 0.04f

    val shape = RoundedCornerShape(28.dp)
    NavigationBar(
        modifier =
            modifier
                .padding(horizontal = 24.dp, vertical = 10.dp)
                .shadow(elevation = 8.dp, shape = shape)
                .clip(shape)
                .drawBehind {
                    // Base metallic gradient
                    drawRect(brush = Brush.verticalGradient(metalBaseGradient))

                    // Brushed horizontal lines for texture
                    val lineSpacing = 2f
                    val lineCount = (size.height / lineSpacing).toInt()
                    for (i in 0..lineCount) {
                        val y = i * lineSpacing
                        if (i % 2 == 0) {
                            drawLine(
                                color = brushLineColor.copy(alpha = brushLineAlphaBase),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 0.5f,
                            )
                        } else {
                            drawLine(
                                color = brushLineDarkColor.copy(alpha = brushLineDarkAlpha),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 0.5f,
                            )
                        }
                    }

                    // Top highlight reflection
                    drawRect(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.White.copy(alpha = if (isDark) 0.08f else 0.35f),
                                        Color.Transparent,
                                    ),
                                endY = size.height * 0.35f,
                            )
                    )
                },
        containerColor = Color.Transparent,
        contentColor = metallicSelectedColor,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val brush = if (isSelected) metallicSelectedBrush else metallicUnselectedBrush
            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(item) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier =
                            Modifier.graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
                                },
                        tint = Color.White,
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        color = if (isSelected) metallicSelectedColor else metallicUnselectedColor,
                    )
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        indicatorColor = metallicSelectedColor.copy(alpha = 0.08f)
                    ),
            )
        }
    }
}
