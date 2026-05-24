package de.frank.entropyreducer.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Status der Journal-Bruecke fuer den Sync-Status-Kopf im Journal-Reiter. */
data class JournalSyncStatus(val lastSyncMs: Long, val lastNewCount: Int)

private val Context.journalSyncStore by preferencesDataStore(name = "journal_sync_meta")

/**
 * Persistiert Zeitpunkt der letzten Journal-Synchronisierung und Anzahl der dabei
 * neu hinzugekommenen Eintraege (Frank-Wunsch 2026-05-24). Wird vom Sync-Repository
 * geschrieben und vom JournalViewModel als Flow beobachtet.
 */
@Singleton
class JournalSyncMeta @Inject constructor(@ApplicationContext private val context: Context) {

    private val keyLastSync = longPreferencesKey("last_sync_ms")
    private val keyNewCount = intPreferencesKey("last_new_count")

    /** lastSyncMs = 0L bedeutet "noch nie synchronisiert". */
    val status: Flow<JournalSyncStatus> =
        context.journalSyncStore.data.map { p ->
            JournalSyncStatus(
                lastSyncMs = p[keyLastSync] ?: 0L,
                lastNewCount = p[keyNewCount] ?: 0,
            )
        }

    suspend fun record(lastSyncMs: Long, newCount: Int) {
        context.journalSyncStore.edit { p ->
            p[keyLastSync] = lastSyncMs
            p[keyNewCount] = newCount
        }
    }
}
