package de.frank.entropyreducer.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Permission-State für den Mic-Workflow.
 *
 * Quelle: https://developer.android.com/training/permissions/requesting (offizielle Doku, 2025).
 * Ersetzt das deprecated accompanist-permissions durch die native ActivityResult-API.
 *
 * Ablauf:
 * 1. UI ruft [check] auf — wenn true, sofort weiter mit der Aufnahme.
 * 2. Wenn false, ruft UI [request] auf. Das oeffnet den System-Dialog.
 * 3. Bei Granted feuert [onAllGranted] und der Caller startet die Aufnahme.
 * 4. Bei Denied feuert [onDenied] mit der Liste verweigerter Permissions.
 *
 * Permissions:
 * - [Manifest.permission.RECORD_AUDIO] (immer): für MediaRecorder.
 * - [Manifest.permission.POST_NOTIFICATIONS] (Android 13 = API 33+): für den
 *   Foreground-Service-Notification-Channel "Aufnahme".
 */
class MicPermissionState internal constructor(
    val check: () -> Boolean,
    val request: () -> Unit,
)

@Composable
fun rememberMicPermissionState(
    onAllGranted: () -> Unit,
    onDenied: (List<String>) -> Unit = {},
): MicPermissionState {
    val ctx = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filterValues { !it }.keys.toList()
        if (denied.isEmpty()) onAllGranted() else onDenied(denied)
    }

    return remember(ctx) {
        MicPermissionState(
            check = {
                val recordOk = ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                val notifOk = if (Build.VERSION.SDK_INT >= 33) {
                    ContextCompat.checkSelfPermission(
                        ctx, Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true  // Vor Android 13 wird die Notification implizit erlaubt.
                }
                recordOk && notifOk
            },
            request = {
                val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= 33) {
                    perms += Manifest.permission.POST_NOTIFICATIONS
                }
                launcher.launch(perms.toTypedArray())
            },
        )
    }
}
