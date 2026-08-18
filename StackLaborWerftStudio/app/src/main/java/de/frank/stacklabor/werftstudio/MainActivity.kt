package de.frank.stacklabor.werftstudio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.stacklabor.werftstudio.service.tts.TtsConfiguration
import de.frank.stacklabor.werftstudio.service.tts.TtsPlaybackService
import de.frank.stacklabor.werftstudio.ui.StackLaborApp
import de.frank.stacklabor.werftstudio.ui.model.FingerprintPurpose
import de.frank.stacklabor.werftstudio.ui.model.StackLaborEvent
import de.frank.stacklabor.werftstudio.ui.model.StackLaborUiEffect
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    private lateinit var viewModel: StackLaborViewModel
    private var pendingExport: String? = null
    private var pendingTextExport: String? = null
    private var pendingTts: Triple<String, TtsConfiguration, Int>? = null

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: throw IOException("Die Importdatei konnte nicht geöffnet werden.")
                }
            }.onSuccess { viewModel.onEvent(StackLaborEvent.ImportDocument(it)) }
                .onFailure { showMessage(it.message ?: "Import fehlgeschlagen") }
        }
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = pendingExport.also { pendingExport = null } ?: return@registerForActivityResult
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(json) }
                        ?: throw IOException("Die Exportdatei konnte nicht angelegt werden.")
                }
            }.onSuccess { showMessage("Daten exportiert") }
                .onFailure { showMessage(it.message ?: "Export fehlgeschlagen") }
        }
    }

    private val textExportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val text = pendingTextExport.also { pendingTextExport = null } ?: return@registerForActivityResult
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) }
                        ?: throw IOException("Die Exportdatei konnte nicht angelegt werden.")
                }
            }.onSuccess { showMessage("Stack exportiert") }
                .onFailure { showMessage(it.message ?: "Export fehlgeschlagen") }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingTts?.let { (text, configuration, start) -> TtsPlaybackService.play(this, text, configuration, start) }
        pendingTts = null
    }

    private val microphonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onEvent(StackLaborEvent.MicrophonePermissionResult(granted))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as StackLaborApplication).container
        viewModel = ViewModelProvider(this, StackLaborViewModelFactory(container))[StackLaborViewModel::class.java]
        collectEffects()
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle().value
            StackLaborApp(state = state, onEvent = viewModel::onEvent)
        }
    }

    private fun collectEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        StackLaborUiEffect.OpenImportDocument -> importLauncher.launch(arrayOf("application/json", "text/json", "text/plain"))
                        is StackLaborUiEffect.CreateExportDocument -> {
                            pendingExport = effect.json
                            exportLauncher.launch("stacklabor-export.json")
                        }
                        is StackLaborUiEffect.CreateTextDocument -> {
                            pendingTextExport = effect.text
                            textExportLauncher.launch(effect.fileName)
                        }
                        is StackLaborUiEffect.OpenUrl -> openUrl(effect.url)
                        is StackLaborUiEffect.PlayTts -> playTts(effect.text, effect.configuration, effect.startParagraph)
                        StackLaborUiEffect.PauseTts -> TtsPlaybackService.pause(this@MainActivity)
                        StackLaborUiEffect.ResumeTts -> TtsPlaybackService.resume(this@MainActivity)
                        StackLaborUiEffect.StopTts -> TtsPlaybackService.stop(this@MainActivity)
                        StackLaborUiEffect.RequestMicrophonePermission -> requestMicrophonePermission()
                        is StackLaborUiEffect.RequestFingerprint -> requestFingerprint(effect.purpose, effect.title, effect.subtitle)
                        is StackLaborUiEffect.Message -> showMessage(effect.text)
                    }
                }
            }
        }
    }

    private fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { showMessage("Für diese Seite ist keine Browser-App verfügbar.") }
    }

    private fun playTts(text: String, configuration: TtsConfiguration, startParagraph: Int = 0) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingTts = Triple(text, configuration, startParagraph)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            TtsPlaybackService.play(this, text, configuration, startParagraph)
        }
    }

    private fun requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onEvent(StackLaborEvent.MicrophonePermissionResult(true))
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    /**
     * Zeigt die Fingerabdruck-Abfrage des Systems und meldet das Ergebnis zurück.
     *
     * Fehlt dem Gerät ein eingerichteter Fingerabdruck, wird das gesagt statt still zu scheitern.
     */
    private fun requestFingerprint(purpose: FingerprintPurpose, title: String, subtitle: String) {
        val stufe = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        when (BiometricManager.from(this).canAuthenticate(stufe)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                viewModel.onEvent(
                    StackLaborEvent.FingerprintResult(purpose, false, "Auf diesem Gerät ist noch kein Fingerabdruck eingerichtet."),
                )
                return
            }
            else -> {
                viewModel.onEvent(
                    StackLaborEvent.FingerprintResult(purpose, false, "Dieses Gerät kann keinen Fingerabdruck prüfen."),
                )
                return
            }
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.onEvent(StackLaborEvent.FingerprintResult(purpose, true))
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val abgebrochen = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    viewModel.onEvent(
                        StackLaborEvent.FingerprintResult(purpose, false, if (abgebrochen) "" else errString.toString()),
                    )
                }

                override fun onAuthenticationFailed() {
                    // Ein einzelner Fehlversuch bricht nichts ab — das System fragt weiter.
                }
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(stufe)
                .build(),
        )
    }

    private fun showMessage(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
}
