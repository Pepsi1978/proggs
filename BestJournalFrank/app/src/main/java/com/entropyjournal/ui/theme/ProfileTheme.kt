package com.entropyjournal.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
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

    /** Compose-States für die im Design unabhängig auswählbaren App-Schriften. */
    val currentHeadingFont = mutableStateOf(DEFAULT_HEADING_FONT_NAME)
    val currentBodyFont = mutableStateOf(DEFAULT_BODY_FONT_NAME)

    /** Compose-States für die getrennt einstellbaren Schriftgrößen (1f = Auslieferungsgröße). */
    val currentHeadingScale = mutableFloatStateOf(DEFAULT_FONT_SCALE)
    val currentBodyScale = mutableFloatStateOf(DEFAULT_FONT_SCALE)

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
        val storedHeading = prefs.getString(Constants.PREF_HEADING_FONT, null)
        val storedBody = prefs.getString(Constants.PREF_BODY_FONT, null)
        if (storedHeading != null && storedBody != null) {
            currentHeadingFont.value = storedHeading
            currentBodyFont.value = storedBody
        } else {
            val (heading, body) = legacyFonts(prefs.getString(PREF_APP_FONT_PAIR, null))
            currentHeadingFont.value = heading
            currentBodyFont.value = body
            prefs.edit()
                .putString(Constants.PREF_HEADING_FONT, heading)
                .putString(Constants.PREF_BODY_FONT, body)
                .apply()
        }
        currentHeadingScale.floatValue =
            clampFontScale(prefs.getFloat(Constants.PREF_HEADING_FONT_SCALE, DEFAULT_FONT_SCALE))
        currentBodyScale.floatValue =
            clampFontScale(prefs.getFloat(Constants.PREF_BODY_FONT_SCALE, DEFAULT_FONT_SCALE))
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

    fun updateFonts(context: Context, headingFont: String, bodyFont: String) {
        openPrefs(context)
            .edit()
            .putString(Constants.PREF_HEADING_FONT, headingFont)
            .putString(Constants.PREF_BODY_FONT, bodyFont)
            .apply()
        currentHeadingFont.value = headingFont
        currentBodyFont.value = bodyFont
    }

    /**
     * Waehrend des Ziehens am Groessen-Regler: nur der Compose-State, damit die ganze App
     * sofort mitwaechst. Das teure Schreiben in die verschluesselten Prefs macht
     * [persistFontScales] erst beim Loslassen.
     */
    fun setHeadingScale(scale: Float) {
        currentHeadingScale.floatValue = clampFontScale(scale)
    }

    fun setBodyScale(scale: Float) {
        currentBodyScale.floatValue = clampFontScale(scale)
    }

    fun persistFontScales(context: Context) {
        openPrefs(context)
            .edit()
            .putFloat(Constants.PREF_HEADING_FONT_SCALE, currentHeadingScale.floatValue)
            .putFloat(Constants.PREF_BODY_FONT_SCALE, currentBodyScale.floatValue)
            .apply()
    }

    private fun legacyFonts(fontPair: String?): Pair<String, String> =
        when (fontPair) {
            "Lora × Manrope" -> "Lora" to "Manrope"
            "Sora × Source Sans" -> "Sora" to "Source Sans 3"
            "Space Grotesk × IBM Plex" -> "Space Grotesk" to "IBM Plex Sans"
            "Nunito × Nunito Sans" -> "Nunito" to "Nunito Sans"
            "Caveat × Source Sans" -> "Caveat" to "Source Sans 3"
            "Great Vibes × Source Sans" -> "Great Vibes" to "Source Sans 3"
            else -> DEFAULT_HEADING_FONT_NAME to DEFAULT_BODY_FONT_NAME
        }
}
