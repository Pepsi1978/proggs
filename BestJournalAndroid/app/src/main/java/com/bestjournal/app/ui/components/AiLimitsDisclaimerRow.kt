package com.bestjournal.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bestjournal.app.R

/**
 * Wiederverwendbares Info-Icon das beim Tippen den zentralen KI-Tageskontingent-Dialog oeffnet
 * (Premium-Stufen, Wartezeiten, Free-Limits, Reset-Zeitpunkt, Burst-Schutz).
 *
 * EINZIGE Quelle der Wahrheit fuer den Inhalt: die Strings `ai_limits_dialog_title`,
 * `ai_limits_dialog_body`, `ai_limits_dialog_close` in `res/values/strings.xml`. Wer den Text
 * aendern will, aendert NUR diese String-Ressourcen — alle drei Info-Icons
 * (Paywall-Disclaimer-Zeile, Paywall-Free-Note, Settings-Premium-Bereich) zeigen automatisch den
 * neuen Text.
 *
 * @param modifier Modifier fuer das Icon (Groesse, Padding, etc.)
 * @param iconSize Optionale explizite Icon-Groesse (Default 20.dp passt zu groesseren Zeilen, fuer
 *   kompakte Free-Note-Zeile 14.dp uebergeben).
 * @param tint Optionale Tint-Farbe (Default = colorScheme.primary; fuer dezente Free-Note
 *   onSurfaceVariant uebergeben).
 */
@Composable
fun AiLimitsInfoIcon(
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    var showDialog by remember { mutableStateOf(false) }
    IconButton(
        onClick = { showDialog = true },
        modifier = modifier.size((iconSize.value + 12f).dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = stringResource(R.string.ai_limits_dialog_title),
            modifier = Modifier.size(iconSize),
            tint = tint,
        )
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.ai_limits_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.ai_limits_dialog_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.ai_limits_dialog_close))
                }
            },
        )
    }
}

/**
 * Disclaimer-Zeile mit Info-Icon: zeigt den `ai_limits_disclaimer`-Text und rechts das gemeinsame
 * [AiLimitsInfoIcon]. Wird in der Paywall und im Settings-Premium-Bereich verwendet, damit der
 * Disclaimer immer maximal einen Tipp entfernt vom Kaufentscheidungs-Bildschirm liegt
 * (UWG-§5a-konform).
 */
@Composable
fun AiLimitsDisclaimerRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.ai_limits_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        AiLimitsInfoIcon()
    }
}
