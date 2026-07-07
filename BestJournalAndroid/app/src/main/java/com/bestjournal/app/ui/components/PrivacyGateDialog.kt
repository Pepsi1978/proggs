package com.bestjournal.app.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                // Gradient icon badge on top
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(72.dp)
                            .background(
                                brush =
                                    Brush.linearGradient(
                                        colors =
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary,
                                            ),
                                    ),
                                shape = RoundedCornerShape(22.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(declineRes))
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Text(
                            stringResource(acceptRes),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
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
