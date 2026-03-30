package com.entropyjournal.ui.components

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.entropyjournal.ui.theme.LocalIsDarkTheme

private val GlowingYellow = Color(0xFFFFD54F)
private val MutedGray = Color(0xFF666666)

@Composable
fun SunMoonToggle(
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current

    val sunSize by animateDpAsState(
        targetValue = if (!isDark) 26.dp else 18.dp,
        animationSpec = tween(300),
        label = "sunSize"
    )
    val moonSize by animateDpAsState(
        targetValue = if (isDark) 26.dp else 18.dp,
        animationSpec = tween(300),
        label = "moonSize"
    )

    Row(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            val themePrefs = context.getSharedPreferences(
                "entropy_theme_quick", Context.MODE_PRIVATE
            )
            themePrefs.edit()
                .putBoolean("toggle_dark", !isDark)
                .putLong("toggle_time", System.currentTimeMillis())
                .apply()
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Sun
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Rounded.LightMode,
                contentDescription = "Tag",
                tint = if (!isDark) GlowingYellow else MutedGray,
                modifier = Modifier.size(sunSize)
            )
        }

        // Divider
        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.height(20.dp).width(1.dp)
        )

        // Moon
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Rounded.DarkMode,
                contentDescription = "Nacht",
                tint = if (isDark) GlowingYellow else MutedGray,
                modifier = Modifier.size(moonSize)
            )
        }
    }
}
