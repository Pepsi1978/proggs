package de.frank.cortex.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.cortex.ui.theme.*
import de.frank.cortex.vpn.TunnelState

@Composable
fun CortexTopBar(
    isDark: Boolean,
    themeMode: String,
    onSetThemeMode: (String) -> Unit,
    vpnState: TunnelState,
    onVpnToggle: (Boolean) -> Unit,
    showNewChat: Boolean = false,
    onNewChat: () -> Unit = {},
    onOpenSessions: () -> Unit = {}
) {
    val colors = if (isDark) Triple(DarkSurface, DarkBorder, DarkMuted)
    else Triple(LightSurface, LightBorder, LightMuted)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.first)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Speicher-Icon Container (38x38, radius 12, gradient + glow)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Iris.copy(alpha = 0.30f),
                            Iris.copy(alpha = 0.12f)
                        )
                    )
                )
                .border(1.dp, DarkUserBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenSessions),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = "Sessions öffnen",
                tint = Iris,
                modifier = Modifier.size(22.dp)
            )
        }

        if (showNewChat) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Iris.copy(alpha = 0.30f),
                                Iris.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(1.dp, DarkUserBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onNewChat),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddComment,
                    contentDescription = "Neuer Chat",
                    tint = Iris,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Title block
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Cortex",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
                letterSpacing = 0.2.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "ZWEITES GEHIRN",
                fontFamily = JetBrainsMono,
                fontSize = 8.5.sp,
                letterSpacing = 2.5.sp,
                color = colors.third
            )
        }

        // VPN Pill
        VpnPill(state = vpnState, onClick = {
            onVpnToggle(vpnState != TunnelState.CONNECTED)
        })

        Spacer(Modifier.width(10.dp))

        // Theme Toggle (34x34, radius 999)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isDark) DarkChip else LightChip)
                .border(1.dp, if (isDark) DarkChipBorder else LightChipBorder, CircleShape)
                .clickable {
                    val next = when (themeMode) {
                        "dark" -> "light"
                        "light" -> "system"
                        else -> "dark"
                    }
                    onSetThemeMode(next)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (themeMode) {
                    "dark" -> Icons.Default.DarkMode
                    "light" -> Icons.Default.LightMode
                    else -> Icons.Default.BrightnessAuto
                },
                contentDescription = "Theme",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun VpnPill(state: TunnelState, onClick: () -> Unit) {
    val (label, dotColor, fgColor, bgColor, borderColor) = when (state) {
        TunnelState.CONNECTED -> VpnStyle(
            "Verbunden", Mint, Mint,
            Mint.copy(alpha = 0.14f), Mint.copy(alpha = 0.40f)
        )
        TunnelState.CONNECTING -> VpnStyle(
            "Verbinde\u2026", Amber, Amber,
            Amber.copy(alpha = 0.14f), Amber.copy(alpha = 0.40f)
        )
        TunnelState.DISCONNECTED -> VpnStyle(
            "Getrennt", DarkMuted, DarkMuted,
            DarkChip, DarkChipBorder
        )
        TunnelState.ERROR -> VpnStyle(
            "Fehler", Rose, Rose,
            Rose.copy(alpha = 0.14f), Rose.copy(alpha = 0.40f)
        )
    }

    // Pulse animation for CONNECTING
    val infiniteTransition = rememberInfiniteTransition(label = "vpn_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ), label = "pulse_alpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ), label = "pulse_scale"
    )

    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // Dot with optional pulse
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            if (state == TunnelState.CONNECTING) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = pulseAlpha))
                        .scale(pulseScale)
                )
            }
        }
        Text(
            text = label,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            color = fgColor
        )
    }
}

private data class VpnStyle(
    val label: String,
    val dot: Color,
    val fg: Color,
    val bg: Color,
    val border: Color
)
