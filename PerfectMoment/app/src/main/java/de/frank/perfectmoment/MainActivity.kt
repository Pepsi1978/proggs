package de.frank.perfectmoment

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.perfectmoment.session.SessionSilence
import de.frank.perfectmoment.ui.AppScreen
import de.frank.perfectmoment.ui.AppViewModel
import de.frank.perfectmoment.ui.PerfectMomentApp
import de.frank.perfectmoment.ui.OrbitRing
import de.frank.perfectmoment.ui.PrimaryButton
import de.frank.perfectmoment.ui.theme.BreathingBackground
import de.frank.perfectmoment.ui.theme.Inter
import de.frank.perfectmoment.ui.theme.LocalPmColors
import de.frank.perfectmoment.ui.theme.Newsreader
import de.frank.perfectmoment.ui.theme.PerfectMomentTheme

class MainActivity : FragmentActivity() {
    private val container get() = (application as PerfectMomentApplication).container
    private val viewModel: AppViewModel by viewModels {
        AppViewModel.factory(this, container)
    }
    private var microphoneGranted by mutableStateOf(false)
    private lateinit var fingerprintPrompt: BiometricPrompt

    private val microphonePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        microphoneGranted = granted
        viewModel.onMicrophonePermissionResult(granted)
    }
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fingerprintPrompt = createFingerprintPrompt()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        microphoneGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        setContent {
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            val locked by container.appLockManager.locked.collectAsStateWithLifecycle()
            val lockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
            PerfectMomentTheme(theme) {
                SystemWindowState(lockEnabled, viewModel.screen == AppScreen.SESSION)
                // Google fragt die Freigabe für Drive in einem eigenen Bildschirm ab.
                val consentLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartIntentSenderForResult(),
                ) { result -> viewModel.consentHandled(result.data) }
                LaunchedEffect(viewModel.backupConsent) {
                    viewModel.backupConsent?.let { sender ->
                        consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }
                }
                LaunchedEffect(viewModel.screen) {
                    if (viewModel.screen == AppScreen.SETTINGS) viewModel.refreshBackupState()
                }
                // Android fragt selbst nach dem Ort — Google Drive erscheint dort als Ziel.
                val chooseTarget = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json"),
                ) { uri -> viewModel.backupTargetChosen(uri) }
                val chooseRestore = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri -> viewModel.restoreFileChosen(uri) }
                LaunchedEffect(viewModel.askForBackupTarget) {
                    if (viewModel.askForBackupTarget) {
                        chooseTarget.launch(container.backupRepository.files.suggestedName())
                    }
                }
                LaunchedEffect(viewModel.askForRestoreFile) {
                    if (viewModel.askForRestoreFile) {
                        chooseRestore.launch(arrayOf("application/json", "*/*"))
                    }
                }
                LaunchedEffect(lockEnabled, locked) {
                    if (!(lockEnabled && locked)) requestSilenceAccessOnce()
                }
                LaunchedEffect(viewModel.screen) {
                    if (viewModel.screen == AppScreen.SESSION && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                Box(
                    Modifier.fillMaxSize().background(LocalPmColors.current.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    if (lockEnabled && locked) {
                        AppLockedScreen(::promptUnlock)
                    } else {
                        PerfectMomentApp(
                            viewModel = viewModel,
                            microphonePermissionGranted = microphoneGranted,
                            requestMicrophonePermission = {
                                microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            connectChatGpt = { viewModel.connectChatGpt(this@MainActivity) },
                            copyDeviceCode = ::copyDeviceCode,
                            openDevicePage = ::openDevicePage,
                            toggleAppLock = ::toggleAppLock,
                        )
                    }
                }
            }
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        promptUnlock()
    }

    // Ohne diese Systemfreigabe darf keine App die Benachrichtigungstöne anderer Apps stummschalten.
    private fun requestSilenceAccessOnce() {
        if (SessionSilence.isAllowed(this) || container.settings.doNotDisturbAccessAsked) return
        container.settings.doNotDisturbAccessAsked = true
        viewModel.showMessage(
            "Damit während einer Session nur deine Fragen zu hören sind, erlaube Perfect Moment " +
                "hier den Zugriff auf \"Nicht stören\". Wecker klingeln weiterhin.",
        )
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        } catch (error: ActivityNotFoundException) {
            viewModel.showMessage("Dieses Gerät bietet keine Freigabe für \"Nicht stören\" an.")
        }
    }

    private fun toggleAppLock(enabled: Boolean) {
        if (!enabled) {
            viewModel.disableAppLock()
            return
        }
        val availability = BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            viewModel.showMessage(biometricUnavailableMessage(availability))
            return
        }
        if (!container.appLockManager.beginPrompt(enabling = true)) return
        fingerprintPrompt.authenticate(promptInfo("Fingerabdruckschutz einschalten"))
    }

    private fun promptUnlock() {
        if (!container.settings.appLockEnabled || !container.appLockManager.locked.value) return
        val availability = BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) return
        if (!container.appLockManager.beginPrompt(enabling = false)) return
        fingerprintPrompt.authenticate(promptInfo("Perfect Moment entsperren"))
    }

    private fun createFingerprintPrompt() = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                container.appLockManager.finishAuthentication(success = true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                container.appLockManager.finishAuthentication(success = false)
            }

            override fun onAuthenticationFailed() {
                // Der Prompt bleibt aktiv; erst Erfolg entsperrt die App.
            }
        },
    )

    private fun promptInfo(title: String) = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle("Bestätige mit einem starken biometrischen Fingerabdruck.")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .setNegativeButtonText("Abbrechen")
        .build()

    private fun biometricUnavailableMessage(code: Int): String = when (code) {
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
            "Es ist kein starker biometrischer Fingerabdruck eingerichtet."
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
            "Dieses Gerät besitzt keinen geeigneten Fingerabdrucksensor."
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
            "Der Fingerabdrucksensor ist derzeit nicht verfügbar."
        else -> "Der starke biometrische Fingerabdruck ist auf diesem Gerät nicht verfügbar."
    }

    private fun copyDeviceCode(code: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(
            ClipData.newPlainText("ChatGPT-Gerätecode", code),
        )
    }

    private fun openDevicePage(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    @Composable
    private fun SystemWindowState(secure: Boolean, keepScreenOn: Boolean) {
        val colors = LocalPmColors.current
        SideEffect {
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !colors.dark
                isAppearanceLightNavigationBars = !colors.dark
            }
            if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        DisposableEffect(keepScreenOn) {
            if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        }
    }
}

@Composable
private fun AppLockedScreen(onUnlock: () -> Unit) {
    val colors = LocalPmColors.current
    val density = LocalDensity.current
    BreathingBackground {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OrbitRing(Modifier.size(96.dp).border(1.5.dp, colors.gold.copy(alpha = 0.36f), CircleShape)) {
                Icon(Icons.Outlined.Fingerprint, "Fingerabdruck", tint = colors.goldHi, modifier = Modifier.size(42.dp))
            }
            Text(
                "Perfect Moment ist gesperrt",
                color = colors.goldHi,
                style = TextStyle(
                    fontFamily = Newsreader,
                    fontWeight = FontWeight.Light,
                    fontSize = 28.sp,
                    shadow = Shadow(
                        colors.gold.copy(alpha = 0.10f),
                        Offset(0f, with(density) { 4.dp.toPx() }),
                        with(density) { 24.dp.toPx() },
                    ),
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
            )
            Text(
                "Entsperre die App mit deinem Fingerabdruck.",
                color = colors.text2,
                fontFamily = Inter,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp, vertical = 8.dp),
            )
            PrimaryButton(
                "Mit Fingerabdruck entsperren",
                onUnlock,
                modifier = Modifier.padding(horizontal = 48.dp, vertical = 20.dp),
            )
        }
    }
}
