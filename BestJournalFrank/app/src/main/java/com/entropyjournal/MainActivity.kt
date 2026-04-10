package com.entropyjournal

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.entropyjournal.ui.navigation.AppNavGraph
import com.entropyjournal.ui.theme.EntropyJournalTheme
import com.entropyjournal.util.Constants
import com.entropyjournal.util.SunCalculator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var encryptedPrefs: SharedPreferences

    // Compose-accessible lock state — survives recomposition AND configuration changes
    private val isUnlocked = mutableStateOf(false)
    private var biometricPromptActive = false
    private var backgroundTimestamp = 0L
    private val lockDelayMs = 60_000L // 60 seconds tolerance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Restore unlock state across configuration changes (e.g. screen rotation)
        if (savedInstanceState != null) {
            isUnlocked.value = savedInstanceState.getBoolean(KEY_IS_UNLOCKED, false)
            backgroundTimestamp = savedInstanceState.getLong(KEY_BG_TIMESTAMP, 0L)
        } else if (!encryptedPrefs.getBoolean(Constants.PREF_BIOMETRIC_LOCK, false)) {
            isUnlocked.value = true
        }

        val quickPrefs = getSharedPreferences("entropy_theme_quick", MODE_PRIVATE)

        setContent {
            val followSystem = remember {
                mutableStateOf(encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false))
            }
            val followSun = remember {
                mutableStateOf(encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SUN, false))
            }
            val manualDark = remember {
                mutableStateOf(encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, false))
            }
            val sunDark = remember {
                mutableStateOf(
                    try {
                        val lat = encryptedPrefs.getFloat(Constants.PREF_LATITUDE, 0f).toDouble()
                        val lon = encryptedPrefs.getFloat(Constants.PREF_LONGITUDE, 0f).toDouble()
                        if (lat != 0.0 && lon != 0.0) SunCalculator.isDarkNow(lat, lon) else false
                    } catch (_: Exception) {
                        false
                    }
                )
            }
            val systemDark = isSystemInDarkTheme()

            DisposableEffect(Unit) {
                val encListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        Constants.PREF_DARK_THEME ->
                            manualDark.value =
                                encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, false)
                        Constants.PREF_THEME_FOLLOW_SYSTEM ->
                            followSystem.value =
                                encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false)
                        Constants.PREF_THEME_FOLLOW_SUN -> {
                            followSun.value =
                                encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SUN, false)
                            try {
                                val lat =
                                    encryptedPrefs.getFloat(Constants.PREF_LATITUDE, 0f).toDouble()
                                val lon =
                                    encryptedPrefs.getFloat(Constants.PREF_LONGITUDE, 0f).toDouble()
                                if (lat != 0.0 && lon != 0.0)
                                    sunDark.value = SunCalculator.isDarkNow(lat, lon)
                            } catch (_: Exception) {}
                        }
                    }
                }
                encryptedPrefs.registerOnSharedPreferenceChangeListener(encListener)

                val quickListener =
                    SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                        if (key == "toggle_time") {
                            val newDark = prefs.getBoolean("toggle_dark", false)
                            encryptedPrefs
                                .edit()
                                .putBoolean(Constants.PREF_DARK_THEME, newDark)
                                .putBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false)
                                .putBoolean(Constants.PREF_THEME_FOLLOW_SUN, false)
                                .apply()
                            manualDark.value = newDark
                            followSystem.value = false
                            followSun.value = false
                        }
                    }
                quickPrefs.registerOnSharedPreferenceChangeListener(quickListener)

                onDispose {
                    encryptedPrefs.unregisterOnSharedPreferenceChangeListener(encListener)
                    quickPrefs.unregisterOnSharedPreferenceChangeListener(quickListener)
                }
            }

            val isDark =
                when {
                    followSun.value -> sunDark.value
                    followSystem.value -> systemDark
                    else -> manualDark.value
                }

            val initialTab =
                when {
                    intent?.getIntExtra("open_tab", -1) == 0 -> 0
                    intent?.getBooleanExtra("from_weekly_review", false) == true -> 0
                    else -> 2
                }

            EntropyJournalTheme(darkTheme = isDark) {
                if (isUnlocked.value) {
                    AppNavGraph(initialTab = initialTab)
                } else {
                    // Lock screen
                    Column(
                        modifier =
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Rounded.Fingerprint,
                            "Entsperren",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Best Journal",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Zum Entsperren authentifizieren",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = { showBiometricPrompt { isUnlocked.value = true } }
                        ) {
                            Text("Entsperren")
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_UNLOCKED, isUnlocked.value)
        outState.putLong(KEY_BG_TIMESTAMP, backgroundTimestamp)
    }

    override fun onResume() {
        super.onResume()
        val biometricEnabled = encryptedPrefs.getBoolean(Constants.PREF_BIOMETRIC_LOCK, false)
        if (biometricEnabled && isUnlocked.value && backgroundTimestamp > 0) {
            // Lock only if more than 60 seconds have passed in background
            val elapsed = System.currentTimeMillis() - backgroundTimestamp
            if (elapsed > lockDelayMs) {
                isUnlocked.value = false
            }
        }
        backgroundTimestamp = 0L
        if (biometricEnabled && !isUnlocked.value && !biometricPromptActive) {
            showBiometricPrompt { isUnlocked.value = true }
        }
    }

    override fun onPause() {
        super.onPause()
        // Record when app went to background — actual lock happens in onResume after 60s
        if (encryptedPrefs.getBoolean(Constants.PREF_BIOMETRIC_LOCK, false)) {
            backgroundTimestamp = System.currentTimeMillis()
        }
    }

    fun showBiometricPrompt(onSuccess: () -> Unit) {
        if (biometricPromptActive) return
        biometricPromptActive = true

        val biometricManager = BiometricManager.from(this)
        val canAuth =
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPromptActive = false
            onSuccess()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt =
            BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        biometricPromptActive = false
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        biometricPromptActive = false
                    }

                    override fun onAuthenticationFailed() {
                        // Single attempt failed — prompt stays open for retry
                    }
                },
            )

        val promptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Best Journal")
                .setSubtitle("Entsperre dein Tagebuch")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

        prompt.authenticate(promptInfo)
    }

    companion object {
        private const val KEY_IS_UNLOCKED = "is_unlocked"
        private const val KEY_BG_TIMESTAMP = "background_timestamp"
    }
}
