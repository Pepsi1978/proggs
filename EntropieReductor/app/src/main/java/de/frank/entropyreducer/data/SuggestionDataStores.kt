package de.frank.entropyreducer.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Zentrale DataStore-Delegates fuer die Vorschlag-Speicherung.
 * Jede Datei darf NUR HIER definiert sein — sonst gibt es den
 * "multiple DataStores active for the same file" Crash.
 */
val Context.kiTaskSuggestionStore by preferencesDataStore(name = "ki_task_suggestions")
val Context.gewohnheitSuggestionStore by preferencesDataStore(name = "gewohnheit_suggestions")
