# Tagebuch-Brücke BestJournal Frank → Entropie Reductor — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Der leere Journal-Reiter in Entropie Reductor zeigt die Tagebucheinträge aus BestJournal Frank read-only an, mit Vorlesen, und gleicht sich bei jedem frischen App-Start ab (volles Abbild).

**Architecture:** BestJournal Frank stellt einen nur-lesenden `ContentProvider` über seine Room-DB bereit. Entropie Reductor liest diesen Provider beim Start, spiegelt Einträge + Nachträge in eine eigene (nicht ins Backup gehende) Room-DB und zeigt sie im Timeline-Look des Entropie-Reiters an. Ein Sync-Status-Kopf zeigt Zeitpunkt + Anzahl neuer Einträge.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt, DataStore, ContentProvider/ContentResolver, Google Cloud TTS (vorhandener `TtsPlayer`).

**Spec:** `docs/superpowers/specs/2026-05-24-journal-bruecke-bestjournalfrank-design.md`

---

## Wichtige Fakten aus der Codebase (vorab gelesen)

- **BestJournal Frank** (`com.entropyjournal`, Debug-Suffix `.debug`): Room-DB `entropy_journal_db`, Singleton `AppDatabase.getDatabase(context)` (siehe `BestJournalFrank/app/src/main/java/com/entropyjournal/data/local/AppDatabase.kt:175`). Tabellen `journal_entries` (Spalten: id, timestamp, rawText, improvedText, isImproved, displayText, audioDurationSeconds, moodTag, entropyScore, adviceCategoryTags, summary, title, followUpText, isSynced) und `entry_follow_ups` (id, entryId, text, createdAt, updatedAt, rawText, improvedText, isImproved). Signiert mit `debug-shared.keystore`.
- **Entropie Reductor** (`de.frank.entropyreducer`, Debug-Suffix `.debug`): Hilt, Room via `di/DatabaseModule.kt`, frischer Start-Sync in `StartupViewModel` (`presentation/MainActivity.kt:212`, läuft einmal pro Prozess über `startupRanThisProcess`). `TtsPlayer.speak(text, ...)` für Vorlesen (`domain/tts/TtsPlayer.kt`). Journal-Slot = `parent == Routes.TASKS && index == 1`, fällt aktuell in den `else`-Zweig (`SubAreaScreen`-Platzhalter) in `presentation/navigation/AppNavGraph.kt:257`. Look-Vorlage: `presentation/tagebuch/TagebuchScreen.kt` (Timeline, `SectionHeader`, `TimelineEntryRow`, `CosmosScaffold`, `GlassCard`, `CosmosBottomBar`). Signiert mit `entropiereductor.debug.keystore` (anderer Key → kein signature-Schutz möglich).

---

## Reihenfolge & Commit-Regeln

- Repo: `Pepsi1978/proggs`. Pro Task: nur die eigenen Dateien namentlich `git add`, dann `git fetch origin && git rebase origin/main && git push`. Commit-Nummern fortlaufend (nächste Nummer per `git log --oneline -1` ermitteln, hier als `#NNN` notiert).
- **Commit+Push VOR jedem Build.** Build der Apps erst NACH Push.
- Beide Apps werden am Ende einmal gebaut + per `adb install -r` installiert, dann gestartet.

---

## Phase A — BestJournal Frank: Daten-Durchreiche

### Task A1: Read-only ContentProvider

**Files:**
- Create: `BestJournalFrank/app/src/main/java/com/entropyjournal/data/provider/JournalExportProvider.kt`
- Modify: `BestJournalFrank/app/src/main/AndroidManifest.xml`
- Modify: `BestJournalFrank/app/build.gradle.kts:48-49` (versionCode/versionName bump)

- [ ] **Step 1: Provider-Klasse anlegen**

`JournalExportProvider.kt`:
```kotlin
package com.entropyjournal.data.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import com.entropyjournal.data.local.AppDatabase

/**
 * Nur-lesende Daten-Durchreiche fuer die Tagebucheintraege (Frank-Wunsch 2026-05-24).
 * Gibt journal_entries + entry_follow_ups an die App "Entropie Reductor" frei.
 * Schreibt/aendert NICHTS. Geschuetzt durch die Berechtigung
 * com.entropyjournal.permission.READ_JOURNAL (Manifest).
 */
class JournalExportProvider : ContentProvider() {

    private val authority by lazy { "${requireContext().packageName}.journalexport" }
    private val matcher by lazy {
        UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(authority, "entries", CODE_ENTRIES)
            addURI(authority, "followups", CODE_FOLLOWUPS)
        }
    }

    private fun requireContext() = context ?: error("ContentProvider ohne Context")

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val db = AppDatabase.getDatabase(requireContext()).openHelper.readableDatabase
        val sql = when (matcher.match(uri)) {
            CODE_ENTRIES ->
                "SELECT id, timestamp, title, displayText, rawText, improvedText, " +
                    "isImproved, summary FROM journal_entries ORDER BY timestamp DESC"
            CODE_FOLLOWUPS ->
                "SELECT id, entryId, createdAt, text, rawText, improvedText, isImproved " +
                    "FROM entry_follow_ups ORDER BY createdAt ASC"
            else -> throw IllegalArgumentException("Unbekannte URI: $uri")
        }
        return db.query(SimpleSQLiteQuery(sql))
    }

    override fun getType(uri: Uri): String = when (matcher.match(uri)) {
        CODE_ENTRIES -> "vnd.android.cursor.dir/vnd.$authority.entries"
        CODE_FOLLOWUPS -> "vnd.android.cursor.dir/vnd.$authority.followups"
        else -> throw IllegalArgumentException("Unbekannte URI: $uri")
    }

    // Read-only: alle Schreiboperationen sind nicht erlaubt.
    override fun insert(uri: Uri, values: ContentValues?): Uri =
        throw UnsupportedOperationException("read-only")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("read-only")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("read-only")

    private companion object {
        const val CODE_ENTRIES = 1
        const val CODE_FOLLOWUPS = 2
    }
}
```

- [ ] **Step 2: Manifest — Permission + Provider deklarieren**

In `BestJournalFrank/app/src/main/AndroidManifest.xml`, innerhalb `<manifest>` vor `<application>` die Permission, und innerhalb `<application>` den Provider ergänzen:
```xml
<permission
    android:name="com.entropyjournal.permission.READ_JOURNAL"
    android:protectionLevel="normal"
    android:label="Tagebuch lesen" />
```
```xml
<provider
    android:name=".data.provider.JournalExportProvider"
    android:authorities="${applicationId}.journalexport"
    android:exported="true"
    android:readPermission="com.entropyjournal.permission.READ_JOURNAL" />
```
> `${applicationId}` ist ein eingebauter Gradle-Manifest-Platzhalter und wird automatisch ersetzt (Debug: `com.entropyjournal.debug.journalexport`).

- [ ] **Step 3: Version-Bump**

In `BestJournalFrank/app/build.gradle.kts`: `versionCode = 143`, `versionName = "0.19.10"`.

- [ ] **Step 4: Commit + Push**

```bash
cd ~/proggs
git add BestJournalFrank/app/src/main/java/com/entropyjournal/data/provider/JournalExportProvider.kt BestJournalFrank/app/src/main/AndroidManifest.xml BestJournalFrank/app/build.gradle.kts
git commit -m "#NNN - BestJournal Frank: add read-only JournalExportProvider for cross-app diary bridge"
git fetch origin && git rebase origin/main && git push
```

- [ ] **Step 5: Build verifizieren**

Run: `cd ~/proggs/BestJournalFrank && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. (Installation passiert gesammelt in Task C5.)

---

## Phase B — Entropie Reductor: Datenschicht & Sync

### Task B1: Room-Entities + DAO + DB für die lokale Kopie

**Files:**
- Create: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/data/local/journalmirror/JournalMirrorEntities.kt`
- Create: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/data/local/journalmirror/JournalMirrorDao.kt`
- Create: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/data/local/journalmirror/JournalMirrorDatabase.kt`

- [ ] **Step 1: Entities**

`JournalMirrorEntities.kt`:
```kotlin
package de.frank.entropyreducer.data.local.journalmirror

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Gespiegelter Tagebucheintrag aus BestJournal Frank. sourceId = id der Quell-DB. */
@Entity(tableName = "journal_mirror_entries")
data class JournalMirrorEntryEntity(
    @PrimaryKey val sourceId: Long,
    val timestamp: Long,
    val title: String?,
    val displayText: String,
    val rawText: String,
    val improvedText: String?,
    val isImproved: Boolean,
    val summary: String?,
)

/** Gespiegelter Nachtrag. sourceId = id aus entry_follow_ups, entryId = zugehoeriger Eintrag. */
@Entity(
    tableName = "journal_mirror_followups",
    indices = [Index("entryId")],
)
data class JournalMirrorFollowupEntity(
    @PrimaryKey val sourceId: Long,
    val entryId: Long,
    val createdAt: Long,
    val text: String,
    val rawText: String,
    val improvedText: String?,
    val isImproved: Boolean,
)
```

- [ ] **Step 2: DAO**

`JournalMirrorDao.kt`:
```kotlin
package de.frank.entropyreducer.data.local.journalmirror

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalMirrorDao {

    @Query("SELECT * FROM journal_mirror_entries ORDER BY timestamp DESC")
    fun observeEntries(): Flow<List<JournalMirrorEntryEntity>>

    @Query("SELECT * FROM journal_mirror_entries WHERE sourceId = :id")
    suspend fun getEntry(id: Long): JournalMirrorEntryEntity?

    @Query("SELECT * FROM journal_mirror_followups WHERE entryId = :entryId ORDER BY createdAt ASC")
    suspend fun followupsFor(entryId: Long): List<JournalMirrorFollowupEntity>

    @Query("SELECT sourceId FROM journal_mirror_entries")
    suspend fun existingEntryIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<JournalMirrorEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFollowups(followups: List<JournalMirrorFollowupEntity>)

    // Loeschen nach explizit uebergebener ID-Liste (Repository ruft bei leerer Quelle
    // stattdessen deleteAll* auf — "NOT IN ()" ist in SQLite ungueltig).
    @Query("DELETE FROM journal_mirror_entries WHERE sourceId NOT IN (:keepIds)")
    suspend fun deleteEntriesNotIn(keepIds: List<Long>)

    @Query("DELETE FROM journal_mirror_followups WHERE sourceId NOT IN (:keepIds)")
    suspend fun deleteFollowupsNotIn(keepIds: List<Long>)

    @Query("DELETE FROM journal_mirror_entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM journal_mirror_followups")
    suspend fun deleteAllFollowups()
}
```

- [ ] **Step 3: Database**

`JournalMirrorDatabase.kt`:
```kotlin
package de.frank.entropyreducer.data.local.journalmirror

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Eigenstaendige, NICHT ins Drive-Backup aufgenommene Spiegel-DB der
 * BestJournal-Frank-Tagebucheintraege. Reine Kopie — destructiveFallback ist
 * unkritisch, da bei jedem App-Start neu synchronisiert wird.
 */
@Database(
    entities = [JournalMirrorEntryEntity::class, JournalMirrorFollowupEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class JournalMirrorDatabase : RoomDatabase() {
    abstract fun journalMirrorDao(): JournalMirrorDao

    companion object {
        const val DB_NAME = "journal_mirror_db"
    }
}
```

- [ ] **Step 4: Commit + Push**

```bash
cd ~/proggs
git add EntropieReductor/app/src/main/java/de/frank/entropyreducer/data/local/journalmirror/
git commit -m "#NNN - Entropie Reductor: add journal mirror Room schema (entries + followups)"
git fetch origin && git rebase origin/main && git push
```

---

### Task B2: Hilt-Provider für die Mirror-DB

**Files:**
- Modify: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/di/DatabaseModule.kt`

- [ ] **Step 1: Provider ergänzen**

Am Ende des `DatabaseModule`-Objekts (vor der schließenden `}`) einfügen, Imports oben ergänzen (`Room`, `Context`, `ApplicationContext`, `Singleton` sind bereits importiert):
```kotlin
    /**
     * Spiegel-DB der BestJournal-Frank-Tagebucheintraege (Frank-Wunsch 2026-05-24).
     * Eigene DB-Datei, NICHT im Drive-Backup. destructiveFallback unkritisch (reine Kopie).
     */
    @Provides
    @Singleton
    fun provideJournalMirrorDatabase(
        @ApplicationContext ctx: Context,
    ): de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDatabase =
        Room.databaseBuilder(
            ctx,
            de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDatabase::class.java,
            de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDatabase.DB_NAME,
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideJournalMirrorDao(
        db: de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDatabase,
    ) = db.journalMirrorDao()
```

- [ ] **Step 2: Commit + Push**

```bash
cd ~/proggs
git add EntropieReductor/app/src/main/java/de/frank/entropyreducer/di/DatabaseModule.kt
git commit -m "#NNN - Entropie Reductor: Hilt providers for journal mirror DB"
git fetch origin && git rebase origin/main && git push
```

---

### Task B3: Sync-Metadaten (DataStore)

**Files:**
- Create: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/data/prefs/JournalSyncMeta.kt`

- [ ] **Step 1: DataStore-Wrapper**

`JournalSyncMeta.kt`:
```kotlin
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

/** Status der Journal-Brücke fuer den Sync-Status-Kopf im Journal-Reiter. */
data class JournalSyncStatus(val lastSyncMs: Long, val lastNewCount: Int)

private val Context.journalSyncStore by preferencesDataStore(name = "journal_sync_meta")

@Singleton
class JournalSyncMeta @Inject constructor(@ApplicationContext private val context: Context) {

    private val keyLastSync = longPreferencesKey("last_sync_ms")
    private val keyNewCount = intPreferencesKey("last_new_count")

    /** lastSyncMs = 0L bedeutet "noch nie synchronisiert". */
    val status: Flow<JournalSyncStatus> = context.journalSyncStore.data.map { p ->
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
```

- [ ] **Step 2: Commit + Push**

```bash
cd ~/proggs
git add EntropieReductor/app/src/main/java/de/frank/entropyreducer/data/prefs/JournalSyncMeta.kt
git commit -m "#NNN - Entropie Reductor: journal sync metadata DataStore"
git fetch origin && git rebase origin/main && git push
```

---

### Task B4: Sync-Repository inkl. Diff-Logik (mit Unit-Test)

**Files:**
- Create: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/data/repository/JournalMirrorRepository.kt`
- Test: `EntropieReductor/app/src/test/java/de/frank/entropyreducer/data/repository/JournalMirrorDiffTest.kt`

- [ ] **Step 1: Failing test für die Neu-Zähl-Logik schreiben**

`JournalMirrorDiffTest.kt`:
```kotlin
package de.frank.entropyreducer.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class JournalMirrorDiffTest {

    @Test
    fun countsOnlyTrulyNewIds() {
        val existing = setOf(1L, 2L, 3L)
        val fetched = listOf(2L, 3L, 4L, 5L) // 4 und 5 sind neu
        assertThat(JournalMirrorDiff.newCount(existing, fetched)).isEqualTo(2)
    }

    @Test
    fun zeroWhenNothingNew() {
        assertThat(JournalMirrorDiff.newCount(setOf(1L, 2L), listOf(1L, 2L))).isEqualTo(0)
    }

    @Test
    fun deletionsDoNotCountAsNew() {
        // Quelle hat weniger als lokal -> 0 neue (Loeschungen zaehlen nicht als neu)
        assertThat(JournalMirrorDiff.newCount(setOf(1L, 2L, 3L), listOf(1L))).isEqualTo(0)
    }
}
```

- [ ] **Step 2: Test ausführen, Fehlschlag verifizieren**

Run: `cd ~/proggs/EntropieReductor && ./gradlew :app:testDebugUnitTest --tests "*JournalMirrorDiffTest*"`
Expected: FAIL ("Unresolved reference: JournalMirrorDiff").

- [ ] **Step 3: Repository + Diff-Objekt implementieren**

`JournalMirrorRepository.kt`:
```kotlin
package de.frank.entropyreducer.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDao
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorEntryEntity
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorFollowupEntity
import de.frank.entropyreducer.data.prefs.JournalSyncMeta
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reine, testbare Zähl-Logik fuer den Sync-Status-Kopf. */
object JournalMirrorDiff {
    fun newCount(existingIds: Set<Long>, fetchedIds: List<Long>): Int =
        fetchedIds.count { it !in existingIds }
}

@Singleton
class JournalMirrorRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: JournalMirrorDao,
    private val syncMeta: JournalSyncMeta,
) {
    /**
     * Liest die Durchreiche von BestJournal Frank und spiegelt alles in die lokale DB.
     * Volles Abbild: neue dazu, geaenderte aktualisieren, in der Quelle geloeschte entfernen.
     * Robust: Provider nicht gefunden / keine Berechtigung -> lokale Kopie bleibt unangetastet.
     */
    suspend fun sync(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val authority = resolveAuthority()
                ?: return@runCatching 0.also {
                    // BestJournal Frank nicht installiert -> kein Sync, Status nicht veraendern.
                }

            val entries = readEntries(authority)
            val followups = readFollowups(authority)

            val fetchedIds = entries.map { it.sourceId }
            val existing = dao.existingEntryIds().toSet()
            val newCount = JournalMirrorDiff.newCount(existing, fetchedIds)

            // Upsert
            if (entries.isNotEmpty()) dao.upsertEntries(entries)
            if (followups.isNotEmpty()) dao.upsertFollowups(followups)

            // Volles Abbild: in der Quelle Geloeschtes lokal entfernen.
            if (fetchedIds.isEmpty()) dao.deleteAllEntries() else dao.deleteEntriesNotIn(fetchedIds)
            val followupIds = followups.map { it.sourceId }
            if (followupIds.isEmpty()) dao.deleteAllFollowups()
            else dao.deleteFollowupsNotIn(followupIds)

            syncMeta.record(System.currentTimeMillis(), newCount)
            newCount
        }.onFailure {
            android.util.Log.w("JournalMirrorRepo", "Journal-Sync fehlgeschlagen", it)
        }
    }

    /** Probiert Debug- dann Release-Authority; gibt die erste auf, die installiert ist. */
    private fun resolveAuthority(): String? {
        val candidates = listOf(
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
                out += JournalMirrorEntryEntity(
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

    private fun readFollowups(authority: String): List<JournalMirrorFollowupEntity> {
        val uri = Uri.parse("content://$authority/followups")
        val out = mutableListOf<JournalMirrorFollowupEntity>()
        // Graceful: followups-Lesefehler darf Eintraege nicht mitreissen.
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val iId = c.getColumnIndexOrThrow("id")
                val iEntry = c.getColumnIndexOrThrow("entryId")
                val iCreated = c.getColumnIndexOrThrow("createdAt")
                val iText = c.getColumnIndexOrThrow("text")
                val iRaw = c.getColumnIndexOrThrow("rawText")
                val iImproved = c.getColumnIndexOrThrow("improvedText")
                val iIsImproved = c.getColumnIndexOrThrow("isImproved")
                while (c.moveToNext()) {
                    out += JournalMirrorFollowupEntity(
                        sourceId = c.getLong(iId),
                        entryId = c.getLong(iEntry),
                        createdAt = c.getLong(iCreated),
                        text = c.getString(iText) ?: "",
                        rawText = c.getString(iRaw) ?: "",
                        improvedText = if (c.isNull(iImproved)) null else c.getString(iImproved),
                        isImproved = c.getInt(iIsImproved) != 0,
                    )
                }
            }
        }
        return out
    }
}
```

- [ ] **Step 4: Test ausführen, Erfolg verifizieren**

Run: `cd ~/proggs/EntropieReductor && ./gradlew :app:testDebugUnitTest --tests "*JournalMirrorDiffTest*"`
Expected: PASS (3 Tests grün).

- [ ] **Step 5: Commit + Push**

```bash
cd ~/proggs
git add EntropieReductor/app/src/main/java/de/frank/entropyreducer/data/repository/JournalMirrorRepository.kt EntropieReductor/app/src/test/java/de/frank/entropyreducer/data/repository/JournalMirrorDiffTest.kt
git commit -m "#NNN - Entropie Reductor: journal mirror sync repository + diff unit test"
git fetch origin && git rebase origin/main && git push
```

---

### Task B5: Sync beim frischen App-Start auslösen + Manifest (queries/permission)

**Files:**
- Modify: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/MainActivity.kt` (StartupViewModel)
- Modify: `EntropieReductor/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Manifest — Berechtigung anfragen + Package-Visibility**

In `EntropieReductor/app/src/main/AndroidManifest.xml`, innerhalb `<manifest>` (vor `<application>`) ergänzen:
```xml
<uses-permission android:name="com.entropyjournal.permission.READ_JOURNAL" />

<queries>
    <provider android:authorities="com.entropyjournal.journalexport" />
    <provider android:authorities="com.entropyjournal.debug.journalexport" />
</queries>
```

- [ ] **Step 2: StartupViewModel — Journal-Sync einhängen**

In `MainActivity.kt`, Konstruktor von `StartupViewModel` (ab Zeile ~212) um die Dependency ergänzen:
```kotlin
    private val journalMirror: de.frank.entropyreducer.data.repository.JournalMirrorRepository,
```
Im `init {}`-Block, innerhalb `if (!startupRanThisProcess) { ... }`, einen eigenen Launch-Block ergänzen (parallel zu den anderen, Dispatchers.IO):
```kotlin
            // Frank-Wunsch 2026-05-24: Tagebuch-Bruecke. Beim frischen App-Start die
            // Eintraege aus BestJournal Frank spiegeln (read-only, volles Abbild).
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { journalMirror.sync() }
                    .onFailure {
                        android.util.Log.w("StartupViewModel", "Journal-Sync fehlgeschlagen", it)
                    }
            }
```

- [ ] **Step 3: Commit + Push**

```bash
cd ~/proggs
git add EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/MainActivity.kt EntropieReductor/app/src/main/AndroidManifest.xml
git commit -m "#NNN - Entropie Reductor: trigger journal mirror sync on fresh start + manifest queries/permission"
git fetch origin && git rebase origin/main && git push
```

---

## Phase C — Entropie Reductor: UI

> Look-Vorlage ist `presentation/tagebuch/TagebuchScreen.kt`. Beim Implementieren diese Datei offen halten und die vorhandenen Bausteine wiederverwenden: `CosmosScaffold`, `GlassCard`, `CosmosBottomBar`, `LocalCosmos`, `Routes`, sowie die Timeline-Logik (`SectionHeader`, `TimelineEntryRow`, `groupEntriesBySection`, `sectionLabelFor`). Die dortigen Timeline-Helfer sind `private` — für den Journal-Reiter werden sie 1:1 kopiert (eigene Datei, eigener `JournalAccent`), nicht aus `tagebuch` importiert (Pakettrennung).

### Task C1: Journal-ViewModel + Journal-Screen mit Sync-Status-Kopf

**Files:**
- Create: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/journal/JournalViewModel.kt`
- Create: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/journal/JournalScreen.kt`

- [ ] **Step 1: ViewModel**

`JournalViewModel.kt`:
```kotlin
package de.frank.entropyreducer.presentation.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDao
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorEntryEntity
import de.frank.entropyreducer.data.prefs.JournalSyncMeta
import de.frank.entropyreducer.data.prefs.JournalSyncStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class JournalViewModel @Inject constructor(
    dao: JournalMirrorDao,
    syncMeta: JournalSyncMeta,
) : ViewModel() {

    val entries: StateFlow<List<JournalMirrorEntryEntity>> =
        dao.observeEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val syncStatus: StateFlow<JournalSyncStatus> =
        syncMeta.status
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalSyncStatus(0L, 0))
}
```

- [ ] **Step 2: Screen mit Sync-Status-Kopf + Timeline**

`JournalScreen.kt` — Grundgerüst (Timeline-Helfer aus `TagebuchScreen.kt` 1:1 kopieren, hier nur Kopf + Liste gezeigt):
```kotlin
package de.frank.entropyreducer.presentation.journal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.prefs.JournalSyncStatus
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val JournalAccent: Color = Color(0xFFEA580C)

@Composable
fun JournalScreen(
    onSwitchSub: (parentTab: String, index: Int) -> Unit,
    onSwitchTab: (route: String) -> Unit,
    onOpenEntry: (sourceId: Long) -> Unit,
    vm: JournalViewModel = hiltViewModel(),
) {
    val cosmos = LocalCosmos.current
    val entries by vm.entries.collectAsState()
    val status by vm.syncStatus.collectAsState()

    CosmosScaffold(
        title = "Journal",
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.TASKS,
                micState = MicState.IDLE,
                onTabSelected = { route -> onSwitchTab(route) },
                onMicClick = {}, // Journal ist read-only — kein Mic
                onSubAreaSelected = { parent, index -> onSwitchSub(parent, index) },
                forcedSubMode = Routes.TASKS,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SyncStatusHeader(status)
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Book, null, tint = JournalAccent, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Noch keine Tagebucheinträge aus BestJournal Frank",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cosmos.textSecondary,
                    )
                }
            } else {
                // TODO beim Implementieren: groupEntriesBySection + TimelineEntryRow aus
                // TagebuchScreen.kt 1:1 kopieren, hier auf JournalMirrorEntryEntity mappen
                // (entry.title ?: "Tagebucheintrag", entry.displayText, entry.timestamp).
                // Tap -> onOpenEntry(entry.sourceId).
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(entries.size, key = { entries[it].sourceId }) { i ->
                        val e = entries[i]
                        GlassCard(modifier = Modifier.fillMaxWidth().clickable { onOpenEntry(e.sourceId) }) {
                            Column {
                                Text(
                                    e.title?.ifBlank { "Tagebucheintrag" } ?: "Tagebucheintrag",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = cosmos.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    formatJournalTimestamp(e.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cosmos.textSecondary,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(e.displayText, style = MaterialTheme.typography.bodyMedium, color = cosmos.textPrimary, maxLines = 5)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusHeader(status: JournalSyncStatus) {
    val cosmos = LocalCosmos.current
    val text = if (status.lastSyncMs == 0L) {
        "Noch nicht synchronisiert"
    } else {
        val ts = SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.GERMANY).format(Date(status.lastSyncMs))
        val n = status.lastNewCount
        val neu = if (n == 1) "1 neuer Eintrag" else "$n Einträge neu"
        "Zuletzt synchronisiert: $ts  ·  $neu"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = JournalAccent,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

internal fun formatJournalTimestamp(ts: Long): String =
    SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.GERMANY).format(Date(ts))
```
> `androidx.compose.foundation.lazy.items` Import ergänzen. Beim finalen Implementieren die Timeline-Optik (Buch-Badge/Sektionen) aus `TagebuchScreen.kt` übernehmen, statt der schlichten Liste oben.
> `CosmosBottomBar`-Parameter zuerst in `TagebuchScreen.kt:184` gegenprüfen (exakte Signatur).

- [ ] **Step 3: Build verifizieren**

Run: `cd ~/proggs/EntropieReductor && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit + Push**

```bash
cd ~/proggs
git add EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/journal/
git commit -m "#NNN - Entropie Reductor: Journal screen with sync-status header + timeline list"
git fetch origin && git rebase origin/main && git push
```

---

### Task C2: Detail-Screen (read-only, Original/KI-Umschalter, Nachträge, Vorlesen)

**Files:**
- Create: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/journal/JournalEntryDetailViewModel.kt`
- Create: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/journal/JournalEntryDetailScreen.kt`

- [ ] **Step 1: Detail-ViewModel (lädt Eintrag + Nachträge, steuert TTS)**

`JournalEntryDetailViewModel.kt`:
```kotlin
package de.frank.entropyreducer.presentation.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorDao
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorEntryEntity
import de.frank.entropyreducer.data.local.journalmirror.JournalMirrorFollowupEntity
import de.frank.entropyreducer.domain.tts.TtsPlayer
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JournalDetailState(
    val entry: JournalMirrorEntryEntity? = null,
    val followups: List<JournalMirrorFollowupEntity> = emptyList(),
    val isSpeaking: Boolean = false,
)

@HiltViewModel
class JournalEntryDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dao: JournalMirrorDao,
    private val tts: TtsPlayer,
) : ViewModel() {

    private val sourceId: Long = savedState.get<String>("sourceId")?.toLongOrNull() ?: -1L
    private val _state = MutableStateFlow(JournalDetailState())
    val state: StateFlow<JournalDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val entry = dao.getEntry(sourceId)
            val followups = dao.followupsFor(sourceId)
            _state.value = JournalDetailState(entry = entry, followups = followups)
        }
    }

    /** Liest Eintrag + alle Nachträge am Stück vor. */
    fun speak() {
        val s = _state.value
        val entry = s.entry ?: return
        val builder = StringBuilder()
        builder.append(entry.title?.takeIf { it.isNotBlank() }?.plus(". ") ?: "")
        builder.append(entry.displayText)
        s.followups.forEach { f ->
            builder.append(". Nachtrag: ")
            builder.append(if (f.isImproved && !f.improvedText.isNullOrBlank()) f.improvedText else f.text)
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSpeaking = true)
            tts.speak(
                text = builder.toString().take(4800), // TTS-Limit ~5000 Zeichen
                onComplete = { _state.value = _state.value.copy(isSpeaking = false) },
                onError = { _state.value = _state.value.copy(isSpeaking = false) },
            )
        }
    }

    fun stopSpeaking() {
        tts.stop()
        _state.value = _state.value.copy(isSpeaking = false)
    }

    override fun onCleared() {
        tts.stop()
        super.onCleared()
    }
}
```

- [ ] **Step 2: Detail-Screen (read-only)**

`JournalEntryDetailScreen.kt`:
```kotlin
package de.frank.entropyreducer.presentation.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.LocalCosmos

@Composable
fun JournalEntryDetailScreen(
    onBack: () -> Unit,
    vm: JournalEntryDetailViewModel = hiltViewModel(),
) {
    val cosmos = LocalCosmos.current
    val state by vm.state.collectAsState()
    var showImproved by remember { mutableStateOf(false) }
    val entry = state.entry

    CosmosScaffold(title = "Tagebucheintrag", onBack = onBack) { padding ->
        if (entry == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Eintrag nicht gefunden", color = cosmos.textSecondary)
            }
            return@CosmosScaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Kopf: Titel + Vorlesen-Knopf
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.title?.ifBlank { "Tagebucheintrag" } ?: "Tagebucheintrag",
                    style = MaterialTheme.typography.titleLarge,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { if (state.isSpeaking) vm.stopSpeaking() else vm.speak() }) {
                    Icon(
                        imageVector = if (state.isSpeaking) Icons.Outlined.Stop
                        else Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = if (state.isSpeaking) "Stop" else "Vorlesen",
                        tint = JournalAccent,
                    )
                }
            }
            Text(formatJournalTimestamp(entry.timestamp), style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
            Spacer(Modifier.height(12.dp))

            // KI-Zusammenfassung (falls vorhanden)
            if (!entry.summary.isNullOrBlank()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Zusammenfassung", style = MaterialTheme.typography.labelMedium, color = JournalAccent, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(entry.summary, style = MaterialTheme.typography.bodyMedium, color = cosmos.textPrimary)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Original / KI-verbessert Umschalter (nur wenn es eine verbesserte Version gibt)
            if (!entry.improvedText.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (showImproved) "KI-verbessert" else "Original", style = MaterialTheme.typography.labelMedium, color = JournalAccent, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { showImproved = !showImproved }) {
                        Text(if (showImproved) "Original zeigen" else "KI-verbessert zeigen")
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            val bodyText = if (showImproved && !entry.improvedText.isNullOrBlank()) entry.improvedText else entry.displayText
            Text(bodyText, style = MaterialTheme.typography.bodyLarge, color = cosmos.textPrimary)

            // Nachträge
            if (state.followups.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Nachträge", style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                state.followups.forEach { f ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column {
                            Text(formatJournalTimestamp(f.createdAt), style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (f.isImproved && !f.improvedText.isNullOrBlank()) f.improvedText else f.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cosmos.textPrimary,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
```
> `CosmosScaffold`-Signatur (ob es einen `onBack`-Parameter gibt) zuerst in `presentation/components/` gegenprüfen; falls abweichend, an die vorhandene Detail-Screen-Vorlage `presentation/tagebuch/TagebuchEntryDetailScreen.kt` angleichen.

- [ ] **Step 3: Build verifizieren**

Run: `cd ~/proggs/EntropieReductor && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit + Push**

```bash
cd ~/proggs
git add EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/journal/JournalEntryDetailViewModel.kt EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/journal/JournalEntryDetailScreen.kt
git commit -m "#NNN - Entropie Reductor: read-only journal detail screen with TTS (entry + followups) and original/improved toggle"
git fetch origin && git rebase origin/main && git push
```

---

### Task C3: Navigation verkabeln (Journal-Slot + Detail-Route)

**Files:**
- Modify: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/navigation/Routes.kt`
- Modify: `EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/navigation/AppNavGraph.kt`

- [ ] **Step 1: Route ergänzen**

In `Routes.kt` (nahe `TAGEBUCH_ENTRY_DETAIL_PATTERN`, Zeile ~44) ergänzen:
```kotlin
    const val JOURNAL_ENTRY_DETAIL_PATTERN = "journal/entry/{sourceId}"
    fun journalEntryDetail(sourceId: Long) = "journal/entry/$sourceId"
```

- [ ] **Step 2: Detail-Composable registrieren**

In `AppNavGraph.kt`, nach dem `TAGEBUCH_ENTRY_DETAIL_PATTERN`-Block (nach Zeile 141) einfügen:
```kotlin
        composable(
            route = Routes.JOURNAL_ENTRY_DETAIL_PATTERN,
            arguments = listOf(navArgument("sourceId") { type = NavType.StringType }),
        ) {
            de.frank.entropyreducer.presentation.journal.JournalEntryDetailScreen(
                onBack = {
                    nav.popBackStack()
                    Unit
                }
            )
        }
```

- [ ] **Step 3: Journal-Slot (TASKS index 1) auf JournalScreen routen**

In `AppNavGraph.kt`, im Sub-Area-Block (nach `val isThesen = ...`, Zeile ~224) eine Journal-Erkennung ergänzen und vor dem `if (isTagebuch)` einbauen:
```kotlin
                val isJournal = parent == Routes.TASKS && index == 1
```
Dann den Verzweigungsbaum erweitern — `isJournal` zuerst behandeln:
```kotlin
                if (isJournal) {
                    de.frank.entropyreducer.presentation.journal.JournalScreen(
                        onSwitchSub = { p, i ->
                            nav.navigate(Routes.subRouteFor(p, i)) {
                                popUpTo(pattern) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onSwitchTab = onSwitchTabFromSub,
                        onOpenEntry = { sourceId ->
                            nav.navigate(Routes.journalEntryDetail(sourceId))
                        },
                    )
                } else if (isTagebuch) {
                    // ... unveraendert ...
```
> Den bestehenden `if (isTagebuch) { ... } else if (isThesen) { ... } else { ... }`-Baum so umbauen, dass `if (isJournal)` ganz vorne steht.

- [ ] **Step 4: Build verifizieren**

Run: `cd ~/proggs/EntropieReductor && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit + Push**

```bash
cd ~/proggs
git add EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/navigation/Routes.kt EntropieReductor/app/src/main/java/de/frank/entropyreducer/presentation/navigation/AppNavGraph.kt
git commit -m "#NNN - Entropie Reductor: wire Journal tab (TASKS slot 1) to JournalScreen + detail route"
git fetch origin && git rebase origin/main && git push
```

---

### Task C4: Version-Bump Entropie Reductor

**Files:**
- Modify: `EntropieReductor/app/build.gradle.kts:75-76`

- [ ] **Step 1:** `versionCode = 171`, `versionName = "0.11.15"`.
- [ ] **Step 2: Commit + Push**

```bash
cd ~/proggs
git add EntropieReductor/app/build.gradle.kts
git commit -m "#NNN - Entropie Reductor: version bump for journal bridge"
git fetch origin && git rebase origin/main && git push
```

---

### Task C5: Beide Apps bauen, installieren, manuell verifizieren

- [ ] **Step 1: Geräte prüfen**

Run: `adb devices`
Expected: S23 Ultra (`R5CW206F0ZM`) verbunden.

- [ ] **Step 2: Beide Debug-APKs bauen**

```bash
cd ~/proggs/BestJournalFrank && ./gradlew :app:assembleDebug
cd ~/proggs/EntropieReductor && ./gradlew :app:assembleDebug
```
Expected: beide BUILD SUCCESSFUL.

- [ ] **Step 3: Installieren (-r, keine Deinstallation → keine Datenverluste)**

```bash
adb install -r ~/proggs/BestJournalFrank/app/build/outputs/apk/debug/app-debug.apk
adb install -r ~/proggs/EntropieReductor/app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p de.frank.entropyreducer.debug -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 4: Manuelle Prüfung (Golden Path + Kanten)**

1. BestJournal Frank öffnen, prüfen dass Tagebucheinträge vorhanden sind.
2. Entropie Reductor frisch starten (App vorher aus dem Recent-Apps wischen → echter Kaltstart). Aufgaben-Tab → Sub-Bereich „Journal" öffnen.
3. Sync-Status-Kopf zeigt aktuellen Zeitpunkt + „N neue Einträge" (beim ersten Mal = Gesamtzahl).
4. Einträge erscheinen in der Timeline. Eintrag tippen → Detail mit Text, ggf. Zusammenfassung, Original/KI-Umschalter, Nachträge.
5. Vorlesen-Knopf → Eintrag + Nachträge werden vorgelesen; Stop-Knopf beendet.
6. In BestJournal Frank einen NEUEN Eintrag anlegen → Entropie Reductor kaltstarten → neuer Eintrag erscheint, Kopf zeigt „1 neuer Eintrag".
7. In BestJournal Frank einen Eintrag löschen → Entropie Reductor kaltstarten → Eintrag verschwindet (volles Abbild).
8. Kantenfall: BestJournal Frank über `adb uninstall com.entropyjournal.debug` entfernen (NUR zum Test, danach neu installieren) → Entropie Reductor kaltstarten → kein Crash, letzte Kopie bleibt sichtbar.

> Falls TTS „Kein TTS-API-Schlüssel hinterlegt" meldet: das ist erwartet ohne hinterlegten Google-TTS-Key und kein Bug der Brücke (gleicher Mechanismus wie im restlichen App-TTS).

- [ ] **Step 5: Qualitätsschleife**

Nach erfolgreicher manueller Prüfung den `quality-gate` Agent für die Entropie-Reductor-Änderungen starten (tester + code-reviewer + optimizer parallel). Erst bei PASS gilt das Feature als fertig.

---

## Self-Review (gegen die Spec)

**Spec-Coverage:**
- Durchreiche/ContentProvider (Spec 3.1) → Task A1. ✓
- Lokale Kopie, getrennte DB, nicht im Backup (3.2) → Task B1/B2 (`exportSchema=false`, eigene DB-Datei, kein Drive-Trigger). ✓
- Abgleich/volles Abbild inkl. Löschen (2, 3.2) → Task B4 (`deleteEntriesNotIn`/`deleteAllEntries`). ✓
- Inhalte Titel/Text/Zusammenfassung/Original+verbessert/Nachträge (2) → A1 (Spalten), B1 (Felder), C2 (Anzeige + Umschalter + Nachträge). ✓
- Vorlesen Eintrag + Nachträge (2) → C2 (`speak()` baut kombinierten Text). ✓
- Keine Fotos (2, 8) → nicht implementiert. ✓
- Sync-Status-Kopf, immer sichtbar, auch bei 0 (Frank-Zusatz) → B3 (Meta) + C1 (`SyncStatusHeader`, außerhalb des `entries.isEmpty()`-Zweigs). ✓
- Start-Auslöser (3.2) → Task B5 (StartupViewModel, einmal pro Prozess). ✓
- Read-only (2) → C2 keine Eingabefelder; A1 wirft bei insert/update/delete. ✓
- Fehlerbehandlung (5) → B4 (runCatching, resolveAuthority null-safe, followups separat). ✓
- Optik wie Entropie-Reiter (2) → C1/C2 reuse von CosmosScaffold/GlassCard/Timeline. ✓
- Navigation Slot 1 + Detail-Route (3.2) → C3. ✓
- Tests (6) → B4 Unit-Test + C5 manuelle Prüfliste. ✓

**Placeholder-Scan:** Im Code keine TBDs außer den explizit markierten Look-Übernahmen (Timeline-Helfer aus `TagebuchScreen.kt` kopieren) — das ist eine bewusste Wiederverwendungs-Anweisung mit klarer Quelle, kein offener Platzhalter.

**Typ-Konsistenz:** `JournalMirrorEntryEntity.sourceId: Long` durchgängig (DAO, Repo, ViewModel, Nav-Route als String→toLong). `JournalMirrorDiff.newCount(Set<Long>, List<Long>)` identisch in Test und Repo. `JournalSyncStatus(lastSyncMs, lastNewCount)` identisch in Meta/ViewModel/Header. Authority-Strings identisch in Repo und Manifest-`<queries>`.

---

## Offene Punkte, die der Ausführende prüfen muss
1. Exakte Signatur von `CosmosScaffold` (ob `onBack`-Parameter existiert) und `CosmosBottomBar` — an `TagebuchScreen.kt` / `TagebuchEntryDetailScreen.kt` angleichen.
2. `Routes.subRouteFor` und `Routes.tabSwitch`/`nav.tabSwitch` existieren bereits (in AppNavGraph genutzt) — übernehmen.
3. Ikon `Icons.AutoMirrored.Outlined.VolumeUp` ist in der Material-Icons-Extended-Lib enthalten (Dependency `compose.material.icons` ist vorhanden) — sonst `Icons.Outlined.VolumeUp`.
