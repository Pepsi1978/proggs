package com.bestjournal.app.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bestjournal.app.R

/**
 * Crisis-Intervention-Dialog (H5 — California SB 243, HWG, Branchenstandard fuer Mental-Health-Apps).
 *
 * Zeigt Nothilfe-Ressourcen fuer Nutzer in akuten Krisen. Nicht gesetzlich Pflicht in DE,
 * aber haftungsrelevant und Pflicht fuer Apps die unter SB 243 als "Companion Chatbot"
 * eingestuft werden.
 *
 * UI-Pattern: einfache AlertDialog mit drei Aktionen:
 *  - Telefonseelsorge (0800 111 0 111) via tel:-Intent
 *  - Notruf 112 via tel:-Intent
 *  - findahelpline.com (internationales Verzeichnis) via ACTION_VIEW
 */
@Composable
fun CrisisHelpDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val noDialerMsg = stringResource(R.string.settings_crisis_no_dialer)
    val noBrowserMsg = stringResource(R.string.settings_crisis_no_browser)

    fun dial(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        try {
            context.startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(context, noDialerMsg, Toast.LENGTH_LONG).show()
        }
    }

    fun openWeb(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(context, noBrowserMsg, Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.settings_crisis_dialog_title),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_crisis_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))

                // Notruf 112
                CrisisActionRow(
                    icon = Icons.Rounded.Call,
                    title = stringResource(R.string.settings_crisis_emergency_name),
                    buttonLabel = stringResource(R.string.settings_crisis_call_emergency),
                    onClick = { dial("112") },
                    emphasized = true,
                )
                Spacer(Modifier.height(10.dp))

                // Telefonseelsorge DE
                CrisisActionRow(
                    icon = Icons.Rounded.Call,
                    title = stringResource(R.string.settings_crisis_hotline_de_name),
                    subtitle = stringResource(R.string.settings_crisis_hotline_de_hours),
                    buttonLabel = stringResource(R.string.settings_crisis_call_de),
                    onClick = { dial("08001110111") },
                )
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { dial("08001110222") },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_crisis_call_de_alt),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(10.dp))

                // findahelpline.com
                CrisisActionRow(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.settings_crisis_international_name),
                    buttonLabel = stringResource(R.string.settings_crisis_open_findahelpline),
                    onClick = { openWeb("https://findahelpline.com/") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_crisis_close))
            }
        },
    )
}

@Composable
private fun CrisisActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    buttonLabel: String,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (emphasized) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        val buttonColors =
            if (emphasized) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = buttonColors,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = buttonLabel,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
