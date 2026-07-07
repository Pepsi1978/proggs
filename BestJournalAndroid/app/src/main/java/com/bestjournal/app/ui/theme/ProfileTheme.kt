package com.bestjournal.app.ui.theme

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import com.bestjournal.app.util.Constants
import com.bestjournal.app.util.EncryptedPrefsProvider

/**
 * App-weiter State für das aktive KI-Dashboard-Profil. Die ganze App färbt sich live um wenn dieser
 * Index sich ändert (vorausgesetzt der Themes Manager steht auf "Profilfarbe").
 *
 * Quelle der Wahrheit ist die EncryptedSharedPreferences-Datei (PREF_DASHBOARD_SCENARIO). Beim
 * App-Start liest [loadFromPrefs] den Wert in den Compose-State, den [BestJournalTheme] beobachtet.
 * Beim Profil-Wechsel im Settings-Screen ruft [update] beide Seiten gemeinsam an — dadurch greift
 * die Farbänderung SOFORT, ohne Umweg über den Pref-Listener.
 *
 * 1:1 aus BestJournalFrank übernommen.
 */
object ProfileTheme {
    /** Compose-State — alle Theme-Konsumenten lesen direkt diesen Index. */
    val currentProfileIndex = mutableIntStateOf(0)

    /** Beim App-Start aus Prefs laden, BEVOR das erste Compose-Frame gerendert wird. */
    fun loadFromPrefs(context: Context) {
        val saved = EncryptedPrefsProvider.get(context).getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        currentProfileIndex.intValue = saved
    }

    /** Vom Settings-Screen aufgerufen: Prefs + Compose-State gemeinsam aktualisieren. */
    fun update(context: Context, index: Int) {
        EncryptedPrefsProvider.get(context)
            .edit()
            .putInt(Constants.PREF_DASHBOARD_SCENARIO, index)
            .apply()
        currentProfileIndex.intValue = index
    }
}
