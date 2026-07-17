package de.frank.fisetinbegleiter

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import de.frank.fisetinbegleiter.ui.FisetinApp
import de.frank.fisetinbegleiter.ui.theme.AppThemeMode
import de.frank.fisetinbegleiter.ui.theme.AppThemeVariant
import de.frank.fisetinbegleiter.ui.theme.FisetinTheme
import de.frank.fisetinbegleiter.ui.theme.LocalAppColors

class MainActivity : ComponentActivity() {
    private var externalDestination by mutableStateOf<String?>(null)
    private var themeVariant by mutableStateOf(AppThemeVariant.DEFAULT)
    private var themeMode by mutableStateOf(AppThemeMode.DEFAULT)
    private var appPreferences: SharedPreferences? = null
    private var exportText: String = ""

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(exportText) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalDestination = intent.getStringExtra(EXTRA_DESTINATION)
        restoreThemePreferences()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            FisetinTheme(
                variant = themeVariant,
                mode = themeMode,
            ) {
                val appColors = LocalAppColors.current
                SideEffect {
                    val darkMode = themeMode == AppThemeMode.DARK
                    enableEdgeToEdge(
                        statusBarStyle = if (darkMode) {
                            SystemBarStyle.dark(appColors.bg0.toArgb())
                        } else {
                            SystemBarStyle.light(appColors.bg0.toArgb(), appColors.bg0.toArgb())
                        },
                        navigationBarStyle = if (darkMode) {
                            SystemBarStyle.dark(appColors.bg1.toArgb())
                        } else {
                            SystemBarStyle.light(appColors.bg1.toArgb(), appColors.bg1.toArgb())
                        },
                    )
                }
                FisetinApp(
                    themeVariant = themeVariant,
                    themeMode = themeMode,
                    onThemeVariantChange = ::changeThemeVariant,
                    onThemeModeToggle = ::toggleThemeMode,
                    externalDestination = externalDestination,
                    onDestinationConsumed = { externalDestination = null },
                    onExport = { text ->
                        exportText = text
                        createDocument.launch("fisetin-protokoll.txt")
                    },
                    onOpenExactAlarmSettings = {
                        openSettings((application as FisetinApplication).alarmScheduler.exactAlarmSettingsIntent())
                    },
                    onOpenBatterySettings = {
                        openSettings((application as FisetinApplication).alarmScheduler.batterySettingsIntent())
                    },
                    showFirstStartNotice = !getPreferences(MODE_PRIVATE).getBoolean(KEY_NOTICE_SEEN, false),
                    onNoticeConfirmed = {
                        getPreferences(MODE_PRIVATE).edit { putBoolean(KEY_NOTICE_SEEN, true) }
                    },
                )
            }
        }
    }

    private fun restoreThemePreferences() {
        appPreferences = runCatching {
            applicationContext.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        }.onFailure { error ->
            Log.w(LOG_TAG, "Theme-Einstellungen konnten nicht geöffnet werden", error)
        }.getOrNull()

        val storedVariant = readThemePreference(KEY_THEME_VARIANT)
        val storedMode = readThemePreference(KEY_THEME_MODE)
        themeVariant = AppThemeVariant.fromPersistenceValue(storedVariant)
        themeMode = AppThemeMode.fromPersistenceValue(storedMode)

        if (storedVariant != themeVariant.persistenceValue) {
            persistThemePreference(KEY_THEME_VARIANT, themeVariant.persistenceValue)
        }
        if (storedMode != themeMode.persistenceValue) {
            persistThemePreference(KEY_THEME_MODE, themeMode.persistenceValue)
        }
    }

    private fun readThemePreference(key: String): String? = runCatching {
        appPreferences?.getString(key, null)
    }.onFailure { error ->
        Log.w(LOG_TAG, "Ungültiger persistierter Theme-Wert für $key", error)
    }.getOrNull()

    private fun changeThemeVariant(variant: AppThemeVariant) {
        themeVariant = variant
        persistThemePreference(KEY_THEME_VARIANT, variant.persistenceValue)
    }

    private fun toggleThemeMode() {
        themeMode = if (themeMode == AppThemeMode.LIGHT) AppThemeMode.DARK else AppThemeMode.LIGHT
        persistThemePreference(KEY_THEME_MODE, themeMode.persistenceValue)
    }

    private fun persistThemePreference(key: String, value: String) {
        runCatching {
            appPreferences?.edit { putString(key, value) }
        }.onFailure { error ->
            Log.w(LOG_TAG, "Theme-Einstellung konnte nicht gespeichert werden", error)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        externalDestination = intent.getStringExtra(EXTRA_DESTINATION)
    }

    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            Log.w(LOG_TAG, "Herstellerspezifische Einstellungsseite nicht verfügbar", error)
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
        private const val LOG_TAG = "FisetinBegleiter"
        private const val PREFERENCES_NAME = "fisetin_app_preferences"
        private const val KEY_NOTICE_SEEN = "safety_notice_seen"
        private const val KEY_THEME_VARIANT = "theme_variant"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
