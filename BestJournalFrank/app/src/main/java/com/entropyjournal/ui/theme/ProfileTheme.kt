package com.entropyjournal.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.entropyjournal.util.Constants

/**
 * App-weiter State fuer das aktive KI-Dashboard-Profil. Die ganze App
 * faerbt sich live um wenn dieser Index sich aendert.
 *
 * Quelle der Wahrheit ist die EncryptedSharedPreferences-Datei (PREF_DASHBOARD_SCENARIO).
 * Beim App-Start liest [loadFromPrefs] den Wert in den Compose-State, der von
 * [com.entropyjournal.ui.theme.EntropyJournalTheme] beobachtet wird. Beim Profil-
 * Wechsel im Settings-Screen ruft [update] beide Seiten gemeinsam an.
 */
object ProfileTheme {
    /** Compose-State — alle Theme-Konsumenten lesen direkt diesen Index. */
    val currentProfileIndex = mutableIntStateOf(0)

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
        val saved = openPrefs(context).getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        currentProfileIndex.intValue = saved
    }

    /** Vom Settings-Screen aufgerufen: Prefs + Compose-State gemeinsam aktualisieren. */
    fun update(context: Context, index: Int) {
        openPrefs(context)
            .edit()
            .putInt(Constants.PREF_DASHBOARD_SCENARIO, index)
            .apply()
        currentProfileIndex.intValue = index
    }
}
