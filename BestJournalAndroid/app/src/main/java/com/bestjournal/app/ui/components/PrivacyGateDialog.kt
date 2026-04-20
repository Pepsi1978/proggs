package com.bestjournal.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/**
 * Per-service pre-usage consent dialog shown on FIRST activation of Groq,
 * Gemini, or Edge TTS. After the user accepts once, `PrivacyGateHelper`
 * persists that decision and the dialog no longer appears for that service.
 *
 * Caller is responsible for:
 *  - Checking `PrivacyGateHelper.hasConsented(service)` before triggering the action
 *  - Displaying this dialog when consent is missing
 *  - Calling `PrivacyGateHelper.setConsent(service, true)` on [onAccept]
 *  - Skipping or falling back to local alternative on [onDecline]
 */
@Composable
fun PrivacyGateDialog(
    titleRes: Int,
    bodyRes: Int,
    acceptRes: Int,
    declineRes: Int,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text(text = stringResource(titleRes)) },
        text = { Text(text = stringResource(bodyRes), modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = onAccept) { Text(stringResource(acceptRes)) } },
        dismissButton = { TextButton(onClick = onDecline) { Text(stringResource(declineRes)) } },
    )
}
