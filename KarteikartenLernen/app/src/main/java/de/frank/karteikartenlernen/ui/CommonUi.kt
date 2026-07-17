package de.frank.karteikartenlernen.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.karteikartenlernen.model.AppTab
import de.frank.karteikartenlernen.model.MicState
import de.frank.karteikartenlernen.ui.theme.LocalAppPalette
import de.frank.karteikartenlernen.ui.theme.Newsreader

@Composable
fun AuroraBackground() {
    val c = LocalAppPalette.current
    val transition = rememberInfiniteTransition(label = "aurora")
    val driftA by transition.animateFloat(
        initialValue = -24f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "auroraA",
    )
    val driftB by transition.animateFloat(
        initialValue = 25f,
        targetValue = -35f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "auroraB",
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .offset((-70).dp + driftA.dp, (-80).dp + (driftA / 2).dp)
                .size(310.dp)
                .blur(60.dp)
                .background(c.blobA, CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .offset(90.dp + driftB.dp, driftB.dp)
                .size(285.dp)
                .blur(60.dp)
                .background(c.blobB, CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset((-80).dp + (driftB / 2).dp, 70.dp)
                .size(260.dp)
                .blur(60.dp)
                .background(c.blobC, CircleShape),
        )
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    radius: Dp = 18.dp,
    color: Color = LocalAppPalette.current.surface,
    content: @Composable () -> Unit,
) {
    val c = LocalAppPalette.current
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(color)
            .border(1.dp, c.border, RoundedCornerShape(radius)),
    ) { content() }
}

@Composable
fun AppHeader(model: String, reasoning: String, onModelClick: () -> Unit) {
    val c = LocalAppPalette.current
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text("RECHERCHE", color = c.faint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
            Text(
                "Was möchtest du wissen?",
                color = c.text,
                fontFamily = Newsreader,
                fontSize = 24.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        GlassSurface(
            Modifier.clickable(onClick = onModelClick),
            radius = 99.dp,
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(c.accent2, CircleShape))
                Column(Modifier.padding(start = 8.dp)) {
                    Text(model, color = c.text, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, lineHeight = 13.sp)
                    Text("Reasoning · $reasoning", color = c.faint, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, lineHeight = 11.sp)
                }
            }
        }
    }
}

@Composable
fun MicButton(state: MicState, seconds: Int, onClick: () -> Unit) {
    val c = LocalAppPalette.current
    val transition = rememberInfiniteTransition(label = "microphone")
    val breathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2_250, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(172.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(172.dp)
                    .scale(breathe)
                    .blur(14.dp)
                    .background(c.accent.copy(alpha = if (c.dark) 0.47f else 0.35f), CircleShape),
            )
            if (state == MicState.RECORDING) {
                repeat(3) { index -> PulseRing(delay = index * 800) }
            }
            Box(
                Modifier
                    .size(134.dp)
                    .graphicsLayer { scaleX = breathe; scaleY = breathe }
                    .clip(CircleShape)
                    .background(
                        if (state == MicState.RECORDING) {
                            Brush.radialGradient(listOf(Color(0xFFFFB0C2), c.red))
                        } else {
                            Brush.linearGradient(listOf(c.accent.copy(alpha = 0.72f), c.accent))
                        },
                    )
                    .clickable(enabled = state != MicState.TRANSCRIBING, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    MicState.IDLE -> Icon(Icons.Outlined.Mic, null, tint = Color.White, modifier = Modifier.size(46.dp))
                    MicState.RECORDING -> WaveBars()
                    MicState.TRANSCRIBING -> LoadingRing(Color.White, 34.dp)
                }
            }
        }
        val text = when (state) {
            MicState.IDLE -> "Antippen und sprechen"
            MicState.RECORDING -> "Aufnahme läuft · 0:${seconds.toString().padStart(2, '0')} — antippen zum Beenden"
            MicState.TRANSCRIBING -> "Transkribiere mit Whisper …"
        }
        Text(text, color = if (state == MicState.RECORDING) c.red else c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PulseRing(delay: Int) {
    val c = LocalAppPalette.current
    val transition = rememberInfiniteTransition(label = "pulse$delay")
    val scale by transition.animateFloat(
        0.75f,
        2.1f,
        infiniteRepeatable(tween(2400, delayMillis = delay), RepeatMode.Restart),
        label = "scale",
    )
    val alpha by transition.animateFloat(
        0.55f,
        0f,
        infiniteRepeatable(tween(1700, delayMillis = delay), RepeatMode.Restart),
        label = "alpha",
    )
    Box(Modifier.size(134.dp).scale(scale).border(2.dp, c.accent.copy(alpha = alpha), CircleShape))
}

@Composable
private fun WaveBars() {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(9) { index ->
            val transition = rememberInfiniteTransition(label = "wave$index")
            val scale by transition.animateFloat(
                0.28f,
                1f,
                infiniteRepeatable(tween(600 + (index * 70) % 500, delayMillis = (index % 5) * 80), RepeatMode.Reverse),
                label = "height",
            )
            Box(Modifier.size(5.dp, 34.dp).scale(scaleY = scale, scaleX = 1f).background(Color.White, RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
fun LoadingRing(color: Color, size: Dp = 18.dp) {
    val transition = rememberInfiniteTransition(label = "spinner")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(700)), label = "rotation")
    Box(
        Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
            .border(2.dp, color.copy(alpha = 0.35f), CircleShape)
            .border(BorderStroke(2.dp, Brush.sweepGradient(listOf(Color.Transparent, color))), CircleShape),
    )
}

@Composable
fun MainBottomBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    val c = LocalAppPalette.current
    val items = listOf(
        Triple(AppTab.RESEARCH, "Recherche", Icons.Outlined.Mic),
        Triple(AppTab.PROFILES, "Profile", Icons.Outlined.Workspaces),
        Triple(AppTab.LEARN, "Lernen", Icons.Outlined.School),
        Triple(AppTab.SETTINGS, "Mehr", Icons.Outlined.Settings),
    )
    Row(
        Modifier.fillMaxWidth().height(72.dp).background(c.tab).border(1.dp, c.border).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        items.forEach { (tab, label, icon) ->
            val active = selected == tab
            Column(
                Modifier.weight(1f).clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(64.dp, 37.dp).clip(RoundedCornerShape(99.dp)).background(if (active) c.accent.copy(alpha = if (c.dark) 0.16f else 0.12f) else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, label, tint = if (active) c.accent else c.faint, modifier = Modifier.size(27.dp))
                }
                Text(label, color = if (active) c.accent else c.faint, fontSize = 11.5.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ScreenTitle(title: String) {
    val c = LocalAppPalette.current
    Text(
        title,
        color = c.text,
        fontFamily = Newsreader,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
    )
}

@Composable
fun SegmentedControl(options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalAppPalette.current
    Row(modifier.clip(RoundedCornerShape(10.dp)).background(c.field).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { value ->
            Text(
                value,
                color = if (selected == value) c.onAccent else c.muted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected == value) c.accent else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 11.dp, vertical = 6.dp),
            )
        }
    }
}
