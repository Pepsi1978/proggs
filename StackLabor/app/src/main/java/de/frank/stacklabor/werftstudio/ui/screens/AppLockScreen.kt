package de.frank.stacklabor.werftstudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.frank.stacklabor.werftstudio.ui.components.PrimaryAction
import de.frank.stacklabor.werftstudio.ui.components.WerftScreen
import de.frank.stacklabor.werftstudio.ui.model.StackLaborCallbacks
import de.frank.stacklabor.werftstudio.ui.model.StackLaborEvent
import de.frank.stacklabor.werftstudio.ui.theme.StackLaborTheme
import de.frank.stacklabor.werftstudio.ui.theme.metalRim

/**
 * Der Vorhang vor der App: Ohne bestätigten Fingerabdruck ist von den Daten nichts zu sehen.
 */
@Composable
fun AppLockScreen(callbacks: StackLaborCallbacks) {
    val colors = StackLaborTheme.colors
    WerftScreen {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.12f))
                    .border(1.5.dp, metalRim(0.9f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Fingerprint, null, Modifier.size(48.dp), tint = colors.accent)
            }
            Spacer(Modifier.height(24.dp))
            Text("StackLabor ist gesperrt", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Zum Öffnen bitte den Fingerabdruck bestätigen.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            PrimaryAction(
                "Mit Fingerabdruck entsperren",
                { callbacks.onEvent(StackLaborEvent.RetryAppUnlock) },
                Modifier.fillMaxWidth(),
            )
        }
    }
}
