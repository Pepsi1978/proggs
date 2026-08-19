package de.frank.denknotiz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import de.frank.denknotiz.audio.MicRecorder
import de.frank.denknotiz.tts.SpeechController
import de.frank.denknotiz.ui.DenknotizApp
import de.frank.denknotiz.ui.DenknotizViewModel

class MainActivity : FragmentActivity() {
    private var afterMicrophonePermission: (() -> Unit)? = null
    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) afterMicrophonePermission?.invoke()
        afterMicrophonePermission = null
    }
    private val notificationsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val createBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::export)
    }
    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::import)
    }
    private val viewModel: DenknotizViewModel by viewModels {
        val app = application as DenknotizApplication
        DenknotizViewModel.Factory(app.container, MicRecorder(applicationContext), SpeechController(applicationContext), cacheDir)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DenknotizApp(
                viewModel = viewModel,
                requestMicrophone = ::withMicrophonePermission,
                requestNotifications = {
                    if (android.os.Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                createBackup = { createBackup.launch("denknotiz-sicherung.json") },
                openBackup = { openBackup.launch(arrayOf("application/json", "text/json", "text/plain")) },
                requestFingerprint = ::withFingerprint,
            )
        }
    }

    /** Fragt den Fingerabdruck ab (ersatzweise die Bildschirmsperre) und ruft danach die Aktion auf. */
    private fun withFingerprint(title: String, onConfirmed: () -> Unit) {
        val manager = BiometricManager.from(this)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("Bitte mit dem Fingerabdruck bestätigen.")
        when {
            manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS ->
                builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK).setNegativeButtonText("Abbrechen")
            android.os.Build.VERSION.SDK_INT >= 30 &&
                manager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS ->
                builder.setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            else -> {
                viewModel.fingerprintUnavailable("Auf diesem Gerät ist kein Fingerabdruck und keine Bildschirmsperre eingerichtet.")
                return
            }
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onConfirmed()
            override fun onAuthenticationError(code: Int, message: CharSequence) {
                if (code != BiometricPrompt.ERROR_USER_CANCELED && code != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    viewModel.fingerprintUnavailable(message.toString())
                }
            }
        })
        prompt.authenticate(builder.build())
    }

    /**
     * Der Schutz schliesst sich wieder, sobald die App aus dem Blick ist. Ohne das gälte ein
     * einziger Fingerabdruck bis zum nächsten Neustart der App.
     */
    override fun onStop() {
        super.onStop()
        viewModel.lockSecured()
    }

    private fun withMicrophonePermission(action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) action()
        else { afterMicrophonePermission = action; microphonePermission.launch(Manifest.permission.RECORD_AUDIO) }
    }
}
