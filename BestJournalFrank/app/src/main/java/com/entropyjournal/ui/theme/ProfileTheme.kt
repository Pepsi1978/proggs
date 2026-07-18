package com.entropyjournal.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.entropyjournal.util.Constants

/**
 * App-weiter State fuer das aktive KI-Dashboard-Profil UND das aktive App-Theme. Die ganze App
 * faerbt sich live um wenn einer dieser Werte sich aendert.
 *
 * Quelle der Wahrheit sind zwei EncryptedSharedPreferences-Keys:
 * - [Constants.PREF_DASHBOARD_SCENARIO] — Profil-Index (0-3 + Custom)
 * - [Constants.PREF_APP_THEME] — Theme-Storage-Key (siehe [AppTheme])
 *
 * Beim App-Start liest [loadFromPrefs] beide Werte in den Compose-State, der von
 * [com.entropyjournal.ui.theme.EntropyJournalTheme] beobachtet wird. Beim Wechsel im Settings-
 * Screen ruft [update] bzw. [updateTheme] beide Seiten gemeinsam an.
 */
object ProfileTheme {
    private const val PREF_APP_FONT_PAIR = "app_font_pair"

    /** Compose-State — alle Theme-Konsumenten lesen direkt diesen Index. */
    val currentProfileIndex = mutableIntStateOf(0)

    /** Compose-State — vom Themes Manager Dropdown gelesen. */
    val currentAppTheme = mutableStateOf(AppTheme.GoldenThread)

    /** Compose-State für das im Design auswählbare app-weite Schriftpaar. */
    val currentFontPair = mutableStateOf(DEFAULT_FONT_PAIR_LABEL)

    private fun openPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            Constants.ENCRYPTED_PREFS_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    /** Beim App-Start aus Prefs laden, BEVOR das erste Compose-Frame gerendert wird. */
    fun loadFromPrefs(context: Context) {
        val prefs = openPrefs(context)
        currentProfileIndex.intValue = prefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        currentAppTheme.value = AppTheme.fromKey(prefs.getString(Constants.PREF_APP_THEME, null))
        currentFontPair.value = prefs.getString(PREF_APP_FONT_PAIR, DEFAULT_FONT_PAIR_LABEL)
            ?: DEFAULT_FONT_PAIR_LABEL
    }

    /** Vom Settings-Screen aufgerufen: Prefs + Compose-State gemeinsam aktualisieren. */
    fun update(context: Context, index: Int) {
        openPrefs(context)
            .edit()
            .putInt(Constants.PREF_DASHBOARD_SCENARIO, index)
            .apply()
        currentProfileIndex.intValue = index
    }

    /** Themes-Manager-Dropdown ruft das auf wenn der Benutzer ein anderes Theme waehlt. */
    fun updateTheme(context: Context, theme: AppTheme) {
        openPrefs(context)
            .edit()
            .putString(Constants.PREF_APP_THEME, theme.storageKey)
            .apply()
        currentAppTheme.value = theme
    }

    fun updateFontPair(context: Context, fontPairLabel: String) {
        openPrefs(context)
            .edit()
            .putString(PREF_APP_FONT_PAIR, fontPairLabel)
            .apply()
        currentFontPair.value = fontPairLabel
    }
}
