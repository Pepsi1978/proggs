package de.frank.voicekey.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.voicekey.ui.theme.VK

/**
 * Berechtigungs-Assistent — 1:1 nach Mockup-Screen 3. Fuehrt durch die vier Freigaben,
 * die VoiceKey zum zuverlaessigen Lauschen und Ausloesen braucht.
 */
@Composable
fun SetupScreen(
    permissions: PermissionsState,
    onBack: () -> Unit,
    onRequestMic: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestBattery: () -> Unit,
    onRequestAssistKill: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 30.dp),
    ) {
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zurück", tint = VK.TextMid)
            }
        }

        Text("Einrichtung", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Damit VoiceKey zuverlässig lauschen, ChatGPT starten und auch wieder beenden kann, braucht die App fünf Freigaben.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(18.dp))

        val granted = permissions.grantedCount
        val total = permissions.total
        Row(Modifier.fillMaxWidth()) {
            Text(
                buildString { append(granted); append(" von "); append(total); append(" erteilt") },
                style = MaterialTheme.typography.bodySmall,
                color = VK.PrimarySoft,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text("${granted * 100 / total} %", style = MaterialTheme.typography.bodySmall, color = VK.TextMid)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(VK.Surface3),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction = (granted.toFloat() / total).coerceAtLeast(0.02f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(VK.OrbDeep, VK.PrimarySoft))),
            )
        }
        Spacer(Modifier.height(22.dp))

        PermCard(
            icon = Icons.Rounded.Mic,
            title = "Mikrofon",
            description = "Hört dauerhaft zu, um deine Wake-Wörter zu erkennen. Alles läuft offline auf dem Gerät.",
            granted = permissions.mic,
            onGrant = onRequestMic,
        )
        PermCard(
            icon = Icons.Rounded.Notifications,
            title = "Benachrichtigungen",
            description = "Zeigt die dauerhafte Dienst-Benachrichtigung und den Tipp nach einem Neustart.",
            granted = permissions.notifications,
            onGrant = onRequestNotifications,
        )
        PermCard(
            icon = Icons.Rounded.Layers,
            title = "Über anderen Apps anzeigen",
            description = "Erlaubt VoiceKey, den Voice-Mode aus dem Hintergrund zu starten, auch wenn gerade eine andere App offen ist.",
            granted = permissions.overlay,
            onGrant = onRequestOverlay,
        )
        PermCard(
            icon = Icons.Rounded.BatterySaver,
            title = "Akku uneingeschränkt",
            description = "Verhindert, dass One UI den Lausch-Dienst im Hintergrund beendet.",
            granted = permissions.battery,
            onGrant = onRequestBattery,
        )
        PermCard(
            icon = Icons.Rounded.StopCircle,
            title = "Not-Aus (Bedienungshilfe)",
            description = "Beendet die ChatGPT-Voice-Session wirklich, wenn du auf „Voice beenden“ tippst. Aktiviere dafür „VoiceKey Not-Aus“ in den Bedienungshilfen.",
            granted = permissions.assistKill,
            onGrant = onRequestAssistKill,
        )

        VersionFooter()
    }
}

@Composable
private fun PermCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = VK.Surface1,
        border = BorderStroke(1.dp, VK.OutlineSoft),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(if (granted) VK.GreenDim else VK.PrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (granted) VK.Green else VK.PrimarySoft,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(3.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = VK.TextMid)
                Spacer(Modifier.height(9.dp))
                if (granted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = VK.Green, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.size(5.dp))
                        Text("Erteilt", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = VK.Green)
                    }
                } else {
                    Button(
                        onClick = onGrant,
                        colors = ButtonDefaults.buttonColors(containerColor = VK.Primary, contentColor = VK.OnPrimary),
                        shape = RoundedCornerShape(100.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("Erteilen", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
