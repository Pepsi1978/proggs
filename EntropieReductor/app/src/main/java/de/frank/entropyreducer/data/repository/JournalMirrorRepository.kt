package de.frank.entropyreducer.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDao
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDatabase
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorEntryEntity
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorFollowupEntity
import de.frank.entropyreducer.data.prefs.JournalSyncMeta
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reine, testbare Zaehl-Logik fuer den Sync-Status-Kopf. */
object JournalMirrorDiff {
    fun newCount(existingIds: Set<Long>, fetchedIds: List<Long>): Int =
        fetchedIds.count { it !in existingIds }
}

/**
 * Liest die nur-lesende Durchreiche von BestJournal Frank und spiegelt die
 * Tagebucheintraege + Nachtraege in die lokale Room-DB (Frank-Wunsch 2026-05-24).
 *
 * Volles Abbild: neue dazu, geaenderte aktualisieren, in der Quelle geloeschte entfernen.
 * Robust: Provider nicht gefunden / nicht installiert / keine Berechtigung -> lokale
 * Kopie bleibt unangetastet, kein Crash.
 */
@Singleton
class JournalMirrorRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val db: JournalMirrorDatabase,
    private val dao: JournalMirrorDao,
    private val syncMeta: JournalSyncMeta,
) {
    suspend fun sync(): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val authority =
                    resolveAuthority()
                        ?: return@runCatching 0 // BestJournal Frank nicht installiert.

                // Eintraege: ein Lesefehler wirft hier und bricht den ganzen Sync ab,
                // BEVOR irgendetwas geschrieben/geloescht wird -> lokale Kopie bleibt intakt.
                val entries = readEntries(authority)
                // Nachtraege: null = Lesefehler. Dann werden Nachtraege NICHT angefasst
                // (kein Upsert, kein Prune) damit ein transienter Fehler nicht alle
                // lokalen Nachtraege loescht. Leere Liste = echte Leere -> normales Prune.
                val followups = readFollowups(authority)

                val fetchedIds = entries.map { it.sourceId }
                val fetchedSet = fetchedIds.toSet()
                val existing = dao.existingEntryIds().toSet()
                val newCount = JournalMirrorDiff.newCount(existing, fetchedIds)
                // Loeschmenge = lokal vorhanden minus in der Quelle vorhanden.
                val toDeleteEntries = existing.filterNot { it in fetchedSet }

                val followupsToUpsert = followups ?: emptyList()
                val toDeleteFollowups: List<Long> =
                    if (followups == null) {
                        emptyList() // Lesefehler -> Nachtraege unberuehrt lassen.
                    } else {
                        val fetchedFu = followups.mapTo(HashSet()) { it.sourceId }
                        dao.existingFollowupIds().filterNot { it in fetchedFu }
                    }

                // Alles in EINER Transaktion: atomar (kein halber Zustand bei App-Kill)
                // und nur ein WAL-Commit statt vier (weniger Disk-fsync).
                db.withTransaction {
                    if (entries.isNotEmpty()) dao.upsertEntries(entries)
                    if (followupsToUpsert.isNotEmpty()) dao.upsertFollowups(followupsToUpsert)
                    // Chunked (<= 900) gegen das SQLite-Variablenlimit. Leere Mengen
                    // erzeugen keinen Aufruf -> kein ungueltiges "IN ()".
                    toDeleteEntries.chunked(DELETE_CHUNK).forEach { dao.deleteEntriesByIds(it) }
                    toDeleteFollowups.chunked(DELETE_CHUNK).forEach { dao.deleteFollowupsByIds(it) }
                }

                syncMeta.record(System.currentTimeMillis(), newCount)
                newCount
            }
                .onFailure {
                    Diag.w(DiagnosticArea.DATABASE, "JournalMirrorRepo", "Journal-Sync fehlgeschlagen", it)
                }
        }

    /** Probiert Debug- dann Release-Authority; gibt die erste auf, die installiert ist. */
    private fun resolveAuthority(): String? {
        val candidates =
            listOf(
                "com.entropyjournal.debug.journalexport",
                "com.entropyjournal.journalexport",
            )
        val pm = context.packageManager
        return candidates.firstOrNull { pm.resolveContentProvider(it, 0) != null }
    }

    private fun readEntries(authority: String): List<JournalMirrorEntryEntity> {
        val uri = Uri.parse("content://$authority/entries")
        val out = mutableListOf<JournalMirrorEntryEntity>()
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val iId = c.getColumnIndexOrThrow("id")
            val iTs = c.getColumnIndexOrThrow("timestamp")
            val iTitle = c.getColumnIndexOrThrow("title")
            val iDisplay = c.getColumnIndexOrThrow("displayText")
            val iRaw = c.getColumnIndexOrThrow("rawText")
            val iImproved = c.getColumnIndexOrThrow("improvedText")
            val iIsImproved = c.getColumnIndexOrThrow("isImproved")
            val iSummary = c.getColumnIndexOrThrow("summary")
            while (c.moveToNext()) {
                out +=
                    JournalMirrorEntryEntity(
                        sourceId = c.getLong(iId),
                        timestamp = c.getLong(iTs),
                        title = if (c.isNull(iTitle)) null else c.getString(iTitle),
                        displayText = c.getString(iDisplay) ?: "",
                        rawText = c.getString(iRaw) ?: "",
                        improvedText = if (c.isNull(iImproved)) null else c.getString(iImproved),
                        isImproved = c.getInt(iIsImproved) != 0,
                        summary = if (c.isNull(iSummary)) null else c.getString(iSummary),
                    )
            }
        }
        return out
    }

    /**
     * Liest die Nachtraege. Rueckgabe `null` signalisiert einen LESEFEHLER (Exception) —
     * der Aufrufer laesst die lokalen Nachtraege dann unberuehrt (kein Prune), damit ein
     * transienter Fehler nicht alle gespiegelten Nachtraege loescht. Eine leere Liste
     * bedeutet dagegen "Quelle hat wirklich keine Nachtraege" und fuehrt zum Prune.
     */
    private fun readFollowups(authority: String): List<JournalMirrorFollowupEntity>? {
        val uri = Uri.parse("content://$authority/followups")
        return runCatching {
            val out = mutableListOf<JournalMirrorFollowupEntity>()
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val iId = c.getColumnIndexOrThrow("id")
                val iEntry = c.getColumnIndexOrThrow("entryId")
                val iCreated = c.getColumnIndexOrThrow("createdAt")
                val iText = c.getColumnIndexOrThrow("text")
                val iRaw = c.getColumnIndexOrThrow("rawText")
                val iImproved = c.getColumnIndexOrThrow("improvedText")
                val iIsImproved = c.getColumnIndexOrThrow("isImproved")
                while (c.moveToNext()) {
                    out +=
                        JournalMirrorFollowupEntity(
                            sourceId = c.getLong(iId),
                            entryId = c.getLong(iEntry),
                            createdAt = c.getLong(iCreated),
                            text = c.getString(iText) ?: "",
                            rawText = c.getString(iRaw) ?: "",
                            improvedText =
                                if (c.isNull(iImproved)) null else c.getString(iImproved),
                            isImproved = c.getInt(iIsImproved) != 0,
                        )
                }
            }
            out.toList()
        }
            .getOrNull()
    }

    private companion object {
        // Unter dem SQLite-Variablenlimit (999 auf aelteren Android-SQLite-Versionen).
        const val DELETE_CHUNK = 900
    }
}
