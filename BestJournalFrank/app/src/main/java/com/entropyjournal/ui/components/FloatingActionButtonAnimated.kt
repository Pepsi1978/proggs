package com.entropyjournal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun FloatingActionButtonAnimated(
    icon: ImageVector,
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.border(1.dp, Color(0x47E8B547), CircleShape),
        containerColor = Color(0xFF221A13),
        contentColor = Color(0xFFE8B547),
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Icon(imageVector = icon, contentDescription = label)
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally(),
            ) {
                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = label, color = Color(0xFFF2E3C8))
                }
            }
        }
    }
}
