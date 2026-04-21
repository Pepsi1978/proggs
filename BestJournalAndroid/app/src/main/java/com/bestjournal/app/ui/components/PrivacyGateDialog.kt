package com.bestjournal.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.bestjournal.app.util.PrivacyGateHelper

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

/**
 * State holder for a reusable privacy gate. Use [rememberPrivacyGateState] to obtain
 * an instance, then pair it with a [PrivacyGateHost] composable to render the dialog.
 *
 * Usage:
 *   val gate = rememberPrivacyGateState(PrivacyGateHelper.CloudService.Gemini)
 *   PrivacyGateHost(
 *       state = gate,
 *       titleRes = R.string.privacy_gate_gemini_title,
 *       bodyRes = R.string.privacy_gate_gemini_body,
 *       acceptRes = R.string.privacy_gate_gemini_accept,
 *       declineRes = R.string.privacy_gate_gemini_cancel,
 *   )
 *   // Guard a Gemini call:
 *   Button(onClick = { gate.run { viewModel.updateDashboard() } }) { ... }
 */
class PrivacyGateState internal constructor(
    private val context: Context,
    private val service: PrivacyGateHelper.CloudService,
) {
    var showDialog: Boolean by mutableStateOf(false)
        private set

    private var pending: (() -> Unit)? = null

    /**
     * Run [action] if consent for this service has already been given. Otherwise buffer
     * [action] and show the dialog; it will run on accept and be discarded on decline.
     */
    fun run(action: () -> Unit) {
        if (PrivacyGateHelper.hasConsented(context, service)) {
            action()
        } else {
            pending = action
            showDialog = true
        }
    }

    internal fun accept() {
        PrivacyGateHelper.setConsent(context, service, true)
        showDialog = false
        val toRun = pending
        pending = null
        toRun?.invoke()
    }

    internal fun decline() {
        showDialog = false
        pending = null
    }
}

@Composable
fun rememberPrivacyGateState(service: PrivacyGateHelper.CloudService): PrivacyGateState {
    val context = LocalContext.current.applicationContext
    return remember(context, service) { PrivacyGateState(context, service) }
}

/**
 * Renders the dialog when [state].showDialog is true. Place this anywhere inside the
 * screen composition — it is a no-op while the gate is idle.
 */
@Composable
fun PrivacyGateHost(
    state: PrivacyGateState,
    titleRes: Int,
    bodyRes: Int,
    acceptRes: Int,
    declineRes: Int,
) {
    if (state.showDialog) {
        PrivacyGateDialog(
            titleRes = titleRes,
            bodyRes = bodyRes,
            acceptRes = acceptRes,
            declineRes = declineRes,
            onAccept = { state.accept() },
            onDecline = { state.decline() },
        )
    }
}
