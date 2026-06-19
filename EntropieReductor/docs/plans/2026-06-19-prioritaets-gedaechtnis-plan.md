# Prioritäts-Gedächtnis — Implementierungsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (empfohlen)
> oder superpowers:executing-plans, um diesen Plan Task-für-Task umzusetzen. Schritte nutzen
> Checkbox-Syntax (`- [ ]`).
>
> **Spec:** `EntropieReductor/docs/specs/2026-06-19-prioritaets-gedaechtnis-design.md`

**Goal:** Manuell gesetzte Aufgaben-Prioritäten werden in einer eigenen Tabelle gemerkt; beim
Einsprechen einer neuen Aufgabe übernimmt Gemini bei spezifischer Ähnlichkeit die gemerkte
Priorität als KI-Vorschlag. Verwaltbar in einem Einstellungs-Bereich mit Liste, Detail-Editor,
einstellbarem Limit (Default 300) und blinkender Limit-Warnung.

**Architecture:** Neue Room-Entity `priority_memory` + DAO + Repository. Lernen passiert
automatisch in `TasksViewModel.setManualPriority`. Der Abgleich läuft im bestehenden
`ProcessEntryUseCase`-Gemini-Aufruf (Gedächtnis als Kontextblock + strenge Match-Regel in
`PRIORITY_DOCTRINE`). An/Aus + Limit liegen in `AppSettings` (DataStore). UI als neuer
Settings-Unterbereich (Liste + Detail). Drive-Backup zieht die neue Tabelle mit (Schema v17).

**Tech Stack:** Kotlin 2.1.0, Jetpack Compose (BOM 2026.03.00, Material3 1.4.0), Room 2.7.x
(KSP), Hilt 2.55, navigation-compose 2.8.7, kotlinx.serialization, Jetpack DataStore.

## Global Constraints

- **Room-Migration NIE destruktiv.** Schema-Bump an DREI Stellen: `version = 30` in `AppDatabase`,
  neues `MIGRATION_29_30`-Objekt, Registrierung in `DatabaseModule.addMigrations(...)`.
- **Room Kotlin-Codegen ist strikt (2.7):** DAO-Rückgaben non-null Collections (leere Liste statt
  null), Listen-Queries als `fun … : Flow<List<…>>`, Einzel-/Schreib-Ops als `suspend fun`.
- **Hilt:** Repository `@Singleton` + `@Inject constructor`; DAO via `@Provides` im `DatabaseModule`.
- **Compose:** Lazy-Listen mit stabilem `key = { it.id }`; kein State-Write in der Composition;
  blinkende Animation via `rememberInfiniteTransition` + `graphicsLayer { alpha = … }`;
  DataStore-Flows stabil über das ViewModel (`stateIn`/`collectAsStateWithLifecycle`), nie roh im
  Composable-Body neu bauen; Scaffold-`innerPadding` immer anwenden.
- **Deutsch + echte Umlaute** in ALLEN UI-Strings (App ist rein deutsch). In Kotlin-Strings
  deutsche Anführungszeichen escapen/vermeiden (`„…"` bricht Strings).
- **BackupPayload Schema:** `version` 16 → **17**; neues Listen-Feld mit `= emptyList()`-Default.
- **Prio-Farbe** immer über `priorityRampColor(score: Double)` aus `presentation/PriorityRamp.kt`.
- **Version-Bump sichtbar:** App-`versionName`/`versionCode` erhöhen (am Ende, ein Build).
- **Git:** Nach JEDER Task nur die eigenen Dateien namentlich stagen, commit (`#NNN`),
  fetch+rebase, push. Commit+Push VOR jedem Gradle-Build.
- **Observability:** Live-Kanal `android.util.Log` mit TAG `"PRIO_MEMORY"` (Tail:
  `adb logcat -s PRIO_MEMORY`). Lern-Sonde + Match-Checkpoint (erwartet vs. tatsächlich).
- **Große Dateien (>500 Z.)** (`TasksScreen.kt`, `TasksViewModel.kt`, `ProcessEntryUseCase.kt`,
  `AppDatabase.kt`, `BackupPayload.kt`, `SyncEntriesUseCase.kt`) NICHT per Subagent editieren —
  gezielt per Grep + Read(Range) + Edit.

## File Structure (neu / geändert)

**Neu**
- `data/local/entities/PriorityMemoryEntity.kt` — Room-Entity.
- `data/local/dao/PriorityMemoryDao.kt` — DAO.
- `data/repository/PriorityMemoryRepository.kt` — Repository + Lern-Logik.
- `domain/usecase/PriorityMemoryLogic.kt` — reine, testbare Funktionen (Format + Dedup-Auswahl).
- `app/src/test/java/.../domain/usecase/PriorityMemoryLogicTest.kt` — Unit-Tests dafür.
- `presentation/settings/prioritymemory/PriorityMemoryScreen.kt` — Listen-Screen.
- `presentation/settings/prioritymemory/PriorityMemoryDetailScreen.kt` — Detail-Editor.
- `presentation/settings/prioritymemory/PriorityMemoryViewModel.kt` — beide ViewModels.

**Geändert**
- `data/local/AppDatabase.kt` (v30, Entity-Liste, DAO-Getter, MIGRATION_29_30).
- `di/DatabaseModule.kt` (Migration + DAO-`@Provides`).
- `data/settings/AppSettings.kt` (An/Aus-Flag + Limit).
- `presentation/dashboard1/TasksViewModel.kt` (`setManualPriority` → Lernen).
- `domain/usecase/ProcessEntryUseCase.kt` (Gedächtnis-Kontext + Checkpoint; neue Dep).
- `domain/usecase/SystemPromptBuilder.kt` (neuer Kontext-Block).
- `presentation/navigation/Routes.kt` + `AppNavGraph.kt` + `presentation/settings/SettingsHomeScreen.kt`.
- `data/remote/drive/BackupPayload.kt` (+ Mappings), `SyncCoordinator.kt`, `SyncEntriesUseCase.kt`,
  `data/prefs/TombstoneStore.kt` (Backup/Sync + Tombstone).
- `app/build.gradle.kts` (Version-Bump).

## Test-Strategie (an die App angepasst)

- **Echtes Unit-TDD** (JUnit4 + Truth) nur für `PriorityMemoryLogic.kt` (reine Funktionen).
- **Room/DataStore/Compose/Gemini:** Verifikation per `./gradlew :app:compileDebugKotlin`
  (Kompilier-Check pro Task) und am Ende voller Build + Gerätetest + Observability
  (`adb logcat -s PRIO_MEMORY`). Keine neuen Test-Frameworks (YAGNI — Infra fehlt bewusst).

---

### Task 1: Datenfundament — Entity, DAO, Migration, DI

**Files:**
- Create: `app/src/main/java/de/frank/entropyreducer/data/local/entities/PriorityMemoryEntity.kt`
- Create: `app/src/main/java/de/frank/entropyreducer/data/local/dao/PriorityMemoryDao.kt`
- Modify: `app/src/main/java/de/frank/entropyreducer/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/de/frank/entropyreducer/di/DatabaseModule.kt`

**Interfaces (Produces):**
- `PriorityMemoryEntity(id: String, title: String, description: String, priority: Double, createdAt: Long, updatedAt: Long, sourceEntryId: String?)`
- `PriorityMemoryDao` mit `getAll(): Flow<List<…>>`, `getNewest(limit: Int): List<…>` (suspend),
  `observeCount(): Flow<Int>`, `getById(id): …?` (suspend), `getBySourceEntryId(id): …?` (suspend),
  `getAllForBackup(): List<…>` (suspend), `upsert(…)`/`update(…)`/`deleteById(id)` (suspend).
- `AppDatabase.priorityMemoryDao(): PriorityMemoryDao`, `AppDatabase.MIGRATION_29_30`.

- [ ] **Step 1: Entity anlegen** — `PriorityMemoryEntity.kt`:

```kotlin
package de.frank.entropyreducer.data.local.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "priority_memory",
    indices = [Index("updatedAt"), Index("sourceEntryId")],
)
data class PriorityMemoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priority: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val sourceEntryId: String? = null,
)
```

- [ ] **Step 2: DAO anlegen** — `PriorityMemoryDao.kt`:

```kotlin
package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriorityMemoryDao {
    @Query("SELECT * FROM priority_memory ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<PriorityMemoryEntity>>

    @Query("SELECT * FROM priority_memory ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getNewest(limit: Int): List<PriorityMemoryEntity>

    @Query("SELECT COUNT(*) FROM priority_memory")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM priority_memory WHERE id = :id")
    suspend fun getById(id: String): PriorityMemoryEntity?

    @Query("SELECT * FROM priority_memory WHERE sourceEntryId = :sourceEntryId LIMIT 1")
    suspend fun getBySourceEntryId(sourceEntryId: String): PriorityMemoryEntity?

    @Query("SELECT * FROM priority_memory")
    suspend fun getAllForBackup(): List<PriorityMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: PriorityMemoryEntity)

    @Update
    suspend fun update(memory: PriorityMemoryEntity)

    @Query("DELETE FROM priority_memory WHERE id = :id")
    suspend fun deleteById(id: String)
}
```

- [ ] **Step 3: AppDatabase erweitern** — drei Edits in `AppDatabase.kt`:
  1. In `@Database(entities = [ … ])` `PriorityMemoryEntity::class` ergänzen (zur Entity-Liste, ~Z.74-100).
  2. `version = 29` → `version = 30`.
  3. DAO-Getter ergänzen (bei den `abstract fun …Dao()`, ~Z.116-167): `abstract fun priorityMemoryDao(): PriorityMemoryDao`.
  4. Im `companion object` (bei den anderen `MIGRATION_x_y`, nach `MIGRATION_28_29`) ergänzen:

```kotlin
val MIGRATION_29_30: Migration =
    object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS priority_memory (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    priority REAL NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    sourceEntryId TEXT
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_priority_memory_updatedAt ON priority_memory(updatedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_priority_memory_sourceEntryId ON priority_memory(sourceEntryId)")
        }
    }
```

  (Imports `Migration`/`SupportSQLiteDatabase` sind in der Datei bereits vorhanden — durch die Bestands-Migrationen.)

- [ ] **Step 4: DI erweitern** — zwei Edits in `di/DatabaseModule.kt`:
  1. In `Room.databaseBuilder(...).addMigrations( … , AppDatabase.MIGRATION_28_29, )` die Zeile
     `AppDatabase.MIGRATION_29_30,` ergänzen.
  2. Bei den DAO-`@Provides` (~Z.75 ff.) ergänzen:

```kotlin
@Provides fun providePriorityMemoryDao(db: AppDatabase) = db.priorityMemoryDao()
```

- [ ] **Step 5: Commit + Push**

```bash
cd ~/proggs
git add EntropieReductor/app/src/main/java/de/frank/entropyreducer/data/local/entities/PriorityMemoryEntity.kt \
        EntropieReductor/app/src/main/java/de/frank/entropyreducer/data/local/dao/PriorityMemoryDao.kt \
        EntropieReductor/app/src/main/java/de/frank/entropyreducer/data/local/AppDatabase.kt \
        EntropieReductor/app/src/main/java/de/frank/entropyreducer/di/DatabaseModule.kt
git commit -m "#NNN - feat(EntropieReductor): priority_memory entity + dao + migration 29->30"
git fetch origin && git rebase origin/main && git push
```

- [ ] **Step 6: Kompilier-Check**

Run: `cd ~/proggs/EntropieReductor && ./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. Bei Room-Fehler „schema changed but forgot version" → version/Migration prüfen (Room-Almanach M3).

---

### Task 2: AppSettings — An/Aus-Schalter + Limit

**Files:**
- Modify: `app/src/main/java/de/frank/entropyreducer/data/settings/AppSettings.kt`

**Interfaces (Produces):**
- `AppSettings.priorityMemoryEnabledFlow: Flow<Boolean>` (Default true)
- `AppSettings.setPriorityMemoryEnabled(value: Boolean)` (suspend)
- `AppSettings.priorityMemoryLimitFlow: Flow<Int>` (Default 300, eingegrenzt 10..2000)
- `AppSettings.setPriorityMemoryLimit(value: Int)` (suspend)

- [ ] **Step 1: Keys ergänzen** — im `companion object` von `AppSettings` (bei den anderen `*PreferencesKey`):

```kotlin
private val KEY_PRIO_MEMORY_ENABLED = booleanPreferencesKey("prio_memory_enabled")
private val KEY_PRIO_MEMORY_LIMIT = intPreferencesKey("prio_memory_limit")
```

- [ ] **Step 2: Flows + Setter ergänzen** — im Klassenrumpf (Muster `widgetOnlyTodayFlow` / `widgetBgAlphaFlow`):

```kotlin
val priorityMemoryEnabledFlow: Flow<Boolean> = ds.data
    .map { it[KEY_PRIO_MEMORY_ENABLED] ?: true }
    .distinctUntilChanged()

suspend fun setPriorityMemoryEnabled(value: Boolean) = ds.edit { it[KEY_PRIO_MEMORY_ENABLED] = value }

val priorityMemoryLimitFlow: Flow<Int> = ds.data
    .map { (it[KEY_PRIO_MEMORY_LIMIT] ?: 300).coerceIn(10, 2000) }
    .distinctUntilChanged()

suspend fun setPriorityMemoryLimit(value: Int) = ds.edit { it[KEY_PRIO_MEMORY_LIMIT] = value.coerceIn(10, 2000) }
```

- [ ] **Step 3: Commit + Push** (nur `AppSettings.kt`), Message:
  `#NNN - feat(EntropieReductor): app settings for priority memory (enabled + limit)`
  danach fetch+rebase+push.

- [ ] **Step 4: Kompilier-Check** — `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.

---

### Task 3: Reine Logik (TDD) — Format + Dedup-Auswahl

**Files:**
- Create: `app/src/main/java/de/frank/entropyreducer/domain/usecase/PriorityMemoryLogic.kt`
- Test: `app/src/test/java/de/frank/entropyreducer/domain/usecase/PriorityMemoryLogicTest.kt`

**Interfaces (Produces):**
- `fun formatPriorityMemoriesForPrompt(memories: List<PriorityMemoryEntity>): String`
- `fun selectMemoryToUpdate(existing: List<PriorityMemoryEntity>, sourceEntryId: String, title: String): PriorityMemoryEntity?`

- [ ] **Step 1: Failing Test schreiben** — `PriorityMemoryLogicTest.kt`:

```kotlin
package de.frank.entropyreducer.domain.usecase

import com.google.common.truth.Truth.assertThat
import de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity
import org.junit.Test

class PriorityMemoryLogicTest {

    private fun mem(id: String, title: String, prio: Double = 50.0, src: String? = null) =
        PriorityMemoryEntity(id = id, title = title, description = "Beschreibung $title",
            priority = prio, createdAt = 0L, updatedAt = 0L, sourceEntryId = src)

    @Test fun `Format enthaelt Titel Beschreibung und Prioritaet`() {
        val out = formatPriorityMemoriesForPrompt(listOf(mem("1", "Laufen", 80.0)))
        assertThat(out).contains("Laufen")
        assertThat(out).contains("Beschreibung Laufen")
        assertThat(out).contains("80")
    }

    @Test fun `Leere Liste ergibt leeren String`() {
        assertThat(formatPriorityMemoriesForPrompt(emptyList())).isEmpty()
    }

    @Test fun `Dedup findet Eintrag per sourceEntryId zuerst`() {
        val list = listOf(mem("a", "X", src = "entry-1"), mem("b", "X", src = "entry-2"))
        val hit = selectMemoryToUpdate(list, sourceEntryId = "entry-2", title = "X")
        assertThat(hit?.id).isEqualTo("b")
    }

    @Test fun `Dedup faellt auf gleichen Titel zurueck wenn keine sourceId passt`() {
        val list = listOf(mem("a", "  Laufen  ", src = "entry-1"))
        val hit = selectMemoryToUpdate(list, sourceEntryId = "entry-99", title = "laufen")
        assertThat(hit?.id).isEqualTo("a")
    }

    @Test fun `Dedup gibt null wenn nichts passt`() {
        val list = listOf(mem("a", "Laufen", src = "entry-1"))
        assertThat(selectMemoryToUpdate(list, "entry-99", "Voellig anderes")).isNull()
    }
}
```

- [ ] **Step 2: Test laufen lassen — muss fehlschlagen**

Run: `cd ~/proggs/EntropieReductor && ./gradlew :app:testDebugUnitTest --tests "de.frank.entropyreducer.domain.usecase.PriorityMemoryLogicTest"`
Expected: FAIL (`formatPriorityMemoriesForPrompt`/`selectMemoryToUpdate` unresolved).

- [ ] **Step 3: Minimale Implementierung** — `PriorityMemoryLogic.kt`:

```kotlin
package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity

/** Formatiert das Prioritaets-Gedaechtnis als Kontextblock fuer den Gemini-Prompt. */
fun formatPriorityMemoriesForPrompt(memories: List<PriorityMemoryEntity>): String =
    memories.joinToString("\n") { m ->
        "- \"${m.title}\" | ${m.description} | Prioritaet ${m.priority.toInt()}"
    }

/**
 * Waehlt den zu aktualisierenden Gedaechtnis-Eintrag (Dedup beim Lernen):
 * zuerst exakt dieselbe Aufgabe (sourceEntryId), sonst gleicher Titel (getrimmt, case-insensitive),
 * sonst null (= neuer Eintrag).
 */
fun selectMemoryToUpdate(
    existing: List<PriorityMemoryEntity>,
    sourceEntryId: String,
    title: String,
): PriorityMemoryEntity? =
    existing.firstOrNull { it.sourceEntryId == sourceEntryId }
        ?: existing.firstOrNull { it.title.trim().equals(title.trim(), ignoreCase = true) }
```

- [ ] **Step 4: Test laufen lassen — muss bestehen**

Run: `./gradlew :app:testDebugUnitTest --tests "de.frank.entropyreducer.domain.usecase.PriorityMemoryLogicTest"`
Expected: PASS (5 Tests grün).

- [ ] **Step 5: Commit + Push** (beide Dateien), Message:
  `#NNN - feat(EntropieReductor): pure priority-memory logic (format + dedup) + unit tests`
  danach fetch+rebase+push.

---

### Task 4: Repository + Lernen in setManualPriority

**Files:**
- Create: `app/src/main/java/de/frank/entropyreducer/data/repository/PriorityMemoryRepository.kt`
- Modify: `app/src/main/java/de/frank/entropyreducer/presentation/dashboard1/TasksViewModel.kt`

**Interfaces (Consumes):** `PriorityMemoryDao` (Task 1), `selectMemoryToUpdate` (Task 3),
`AppSettings.priorityMemoryEnabledFlow` (Task 2).
**Interfaces (Produces):**
- `PriorityMemoryRepository.observeAll(): Flow<List<PriorityMemoryEntity>>`
- `…observeCount(): Flow<Int>`, `…getById(id): …?`, `…getNewest(limit): List<…>`,
  `…getAllForBackup(): List<…>`
- `…learnFromManualPriority(entry: EntropyEntryEntity, priority: Double, now: Long)` (suspend)
- `…updatePriority(id, priority, now)`, `…updateContent(id, title, description, now)`,
  `…delete(id)` (alle suspend)

- [ ] **Step 1: Repository anlegen** — `PriorityMemoryRepository.kt`:

```kotlin
package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.local.dao.PriorityMemoryDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity
import de.frank.entropyreducer.domain.usecase.selectMemoryToUpdate
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriorityMemoryRepository @Inject constructor(
    private val dao: PriorityMemoryDao,
) {
    fun observeAll(): Flow<List<PriorityMemoryEntity>> = dao.getAll()
    fun observeCount(): Flow<Int> = dao.observeCount()
    suspend fun getById(id: String): PriorityMemoryEntity? = dao.getById(id)
    suspend fun getNewest(limit: Int): List<PriorityMemoryEntity> = dao.getNewest(limit)
    suspend fun getAllForBackup(): List<PriorityMemoryEntity> = dao.getAllForBackup()

    /** Legt einen Gedaechtnis-Eintrag an oder aktualisiert einen vorhandenen (Dedup, kein Duplikat). */
    suspend fun learnFromManualPriority(entry: EntropyEntryEntity, priority: Double, now: Long) {
        val existing = selectMemoryToUpdate(dao.getAllForBackup(), entry.id, entry.title)
        if (existing != null) {
            dao.update(
                existing.copy(
                    title = entry.title,
                    description = entry.description,
                    priority = priority,
                    updatedAt = now,
                    sourceEntryId = entry.id,
                )
            )
        } else {
            dao.upsert(
                PriorityMemoryEntity(
                    id = UUID.randomUUID().toString(),
                    title = entry.title,
                    description = entry.description,
                    priority = priority,
                    createdAt = now,
                    updatedAt = now,
                    sourceEntryId = entry.id,
                )
            )
        }
    }

    suspend fun updatePriority(id: String, priority: Double, now: Long) {
        dao.getById(id)?.let { dao.update(it.copy(priority = priority, updatedAt = now)) }
    }

    suspend fun updateContent(id: String, title: String, description: String, now: Long) {
        dao.getById(id)?.let { dao.update(it.copy(title = title, description = description, updatedAt = now)) }
    }

    suspend fun delete(id: String) = dao.deleteById(id)
}
```

- [ ] **Step 2: TasksViewModel — Dependency ergänzen.** In `TasksViewModel.kt` im
  `@Inject constructor(...)` (vor `) : AndroidViewModel(application)`, ~Z.131) eine Zeile ergänzen:

```kotlin
    private val priorityMemoryRepository: de.frank.entropyreducer.data.repository.PriorityMemoryRepository,
```

- [ ] **Step 3: TasksViewModel — Lernen + Sonde einhängen.** In `setManualPriority` direkt NACH dem
  ersten `entries.update(entry.copy(... manualPriorityScore = clamped ...))` (~Z.997, VOR dem
  `val isRec = …`-Block) einfügen:

```kotlin
            if (settings.priorityMemoryEnabledFlow.first()) {
                priorityMemoryRepository.learnFromManualPriority(entry, clamped, now)
                android.util.Log.d("PRIO_MEMORY", "gelernt: '${entry.title}' -> ${clamped.toInt()}")
            }
```

  Hinweise: läuft bereits in `viewModelScope.launch`; `entry`, `clamped`, `now` und `settings`
  (AppSettings) sind dort im Scope. `first()` ist `kotlinx.coroutines.flow.first` — Import
  ggf. ergänzen, falls noch nicht vorhanden.

- [ ] **Step 4: Commit + Push** (Repository + TasksViewModel), Message:
  `#NNN - feat(EntropieReductor): learn manual priority into memory (incl. loop tasks)`
  danach fetch+rebase+push.

- [ ] **Step 5: Kompilier-Check** — `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.
  (Hilt löst `PriorityMemoryRepository` über `@Inject constructor` + den DAO-`@Provides` aus Task 1 auf.)

---

### Task 5: Gemini-Abgleich beim Einsprechen

**Files:**
- Modify: `app/src/main/java/de/frank/entropyreducer/domain/usecase/SystemPromptBuilder.kt`
- Modify: `app/src/main/java/de/frank/entropyreducer/domain/usecase/ProcessEntryUseCase.kt`

**Interfaces (Consumes):** `PriorityMemoryRepository.getNewest(limit)` (Task 4),
`AppSettings.priorityMemoryEnabledFlow`/`priorityMemoryLimitFlow` (Task 2),
`formatPriorityMemoriesForPrompt` (Task 3).

- [ ] **Step 1: SystemPromptBuilder — neuer Parameter.** In `build(...)` (Signatur ~Z.29-42)
  nach `confirmedInsights: List<InsightEntity> = emptyList(),` ergänzen:

```kotlin
    priorityMemories: List<de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity> = emptyList(),
```

- [ ] **Step 2: SystemPromptBuilder — neuer Kontext-Block.** Im `buildString{}` direkt NACH dem
  `confirmedInsights`-Block (~nach Z.69) einfügen:

```kotlin
        if (priorityMemories.isNotEmpty()) {
            appendLine()
            appendLine("## Prioritaets-Gedaechtnis (frueher manuell gesetzte Prioritaeten)")
            appendLine("Vergleiche die neue Aufgabe SPEZIFISCH mit diesen Eintraegen (Details siehe Doktrin unten):")
            priorityMemories.forEach { m ->
                appendLine("- \"${m.title}\" | ${m.description} | Prioritaet ${m.priority.toInt()}")
            }
        }
```

- [ ] **Step 3: PRIORITY_DOCTRINE — strenge Match-Regel.** In `ProcessEntryUseCase.kt` innerhalb der
  `PRIORITY_DOCTRINE`-Konstante, im Abschnitt BESTAETIGTE-METHODEN-HEBEL beim Match-Kriterium
  (~Z.622-625), folgenden Absatz ergänzen (deutsche Anführungszeichen vermeiden):

```
- PRIORITAETS-GEDAECHTNIS-ABGLEICH: Oben kann ein Abschnitt "Prioritaets-Gedaechtnis" stehen.
  Wenn die neue Aufgabe inhaltlich QUASI DASSELBE Vorhaben beschreibt wie ein Eintrag dort
  (z.B. "laufen gehen" entspricht "Lauftraining im Wald"), uebernimm dessen Prioritaet als
  priorityScore und schreibe in priorityReason genau den Marker: aus frueherer aehnlicher
  Aufgabe uebernommen. NUR thematisch verwandt reicht NICHT (Laufen ist NICHT Federball;
  "Sport" ist KEIN gemeinsamer Nenner). Vergleiche hauptsaechlich die Beschreibung, auch den
  Titel. Im Zweifel: KEIN Treffer, normal bewerten.
```

- [ ] **Step 4: ProcessEntryUseCase — Dependency ergänzen.** Im `@Inject constructor(...)` (~Z.29-39)
  ergänzen:

```kotlin
    private val priorityMemoryRepository: de.frank.entropyreducer.data.repository.PriorityMemoryRepository,
```

- [ ] **Step 5: ProcessEntryUseCase — Gedächtnis laden + an build übergeben + Checkpoint.**
  In `invoke(...)`: beim Kontext-Laden (~Z.49-60) ergänzen:

```kotlin
        val prioMemEnabled = settings.priorityMemoryEnabledFlow.first()
        val priorityMemories =
            if (prioMemEnabled) priorityMemoryRepository.getNewest(settings.priorityMemoryLimitFlow.first())
            else emptyList()
```

  Im `systemPromptBuilder.build(...)`-Aufruf (~Z.62-72) das neue Argument anhängen:

```kotlin
            priorityMemories = priorityMemories,
```

  Direkt NACH dem Erzeugen der `entry`-Entity (nach ~Z.146, vor `entries.upsert(entry)`) den
  Intent-Checkpoint loggen (erwartet vs. tatsächlich):

```kotlin
        if (priorityMemories.isNotEmpty()) {
            val hit = entry.priorityReason.contains("aus frueherer aehnlicher Aufgabe", ignoreCase = true)
            android.util.Log.d(
                "PRIO_MEMORY",
                "CHECKPOINT match: neu='${entry.title}' ok=$hit prio=${entry.priorityScore.toInt()} " +
                    (if (hit) "(uebernommen)" else "(kein Treffer)"),
            )
        }
```

  (`settings`/`first()` sind im UseCase vorhanden bzw. wie in Task 4 zu importieren.)

- [ ] **Step 6: Commit + Push** (SystemPromptBuilder + ProcessEntryUseCase), Message:
  `#NNN - feat(EntropieReductor): gemini priority-memory match on new tasks + checkpoint probe`
  danach fetch+rebase+push.

- [ ] **Step 7: Kompilier-Check** — `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.

---

### Task 6: Settings-UI — Listen-Screen (Liste, An/Aus, Limit, blinkende Warnung)

**Files:**
- Create: `app/src/main/java/de/frank/entropyreducer/presentation/settings/prioritymemory/PriorityMemoryViewModel.kt`
- Create: `app/src/main/java/de/frank/entropyreducer/presentation/settings/prioritymemory/PriorityMemoryScreen.kt`
- Modify: `presentation/navigation/Routes.kt`, `presentation/navigation/AppNavGraph.kt`,
  `presentation/settings/SettingsHomeScreen.kt`

**Interfaces (Consumes):** `PriorityMemoryRepository`, `AppSettings` (Tasks 2/4).
**Interfaces (Produces):** Route `Routes.SETTINGS_PRIORITY_MEMORY`,
`PriorityMemoryListViewModel` mit `memories: StateFlow<List<…>>`, `count: StateFlow<Int>`,
`enabled: StateFlow<Boolean>`, `limit: StateFlow<Int>` + Aktionen
`setEnabled`, `setLimit`, `setPriority(id, prio)`, `delete(id)`.

- [ ] **Step 1: ViewModel (Liste).** `PriorityMemoryViewModel.kt` — Datei enthält das Listen-VM
  (Detail-VM kommt in Task 7 in dieselbe Datei):

```kotlin
package de.frank.entropyreducer.presentation.settings.prioritymemory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity
import de.frank.entropyreducer.data.repository.PriorityMemoryRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.util.AppTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PriorityMemoryListViewModel @Inject constructor(
    private val repo: PriorityMemoryRepository,
    private val settings: AppSettings,
) : ViewModel() {

    val memories: StateFlow<List<PriorityMemoryEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val count: StateFlow<Int> =
        repo.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val enabled: StateFlow<Boolean> =
        settings.priorityMemoryEnabledFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val limit: StateFlow<Int> =
        settings.priorityMemoryLimitFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 300)

    fun setEnabled(value: Boolean) = viewModelScope.launch { settings.setPriorityMemoryEnabled(value) }
    fun setLimit(value: Int) = viewModelScope.launch { settings.setPriorityMemoryLimit(value) }
    fun setPriority(id: String, prio: Double) =
        viewModelScope.launch { repo.updatePriority(id, prio, AppTime.now()) }
    fun delete(id: String) = viewModelScope.launch { repo.delete(id) }
}
```

  (Falls `AppTime.now()` nicht existiert: `System.currentTimeMillis()` verwenden — vorher per
  Grep prüfen: `rg "object AppTime" EntropieReductor`.)

- [ ] **Step 2: Listen-Screen.** `PriorityMemoryScreen.kt` nach dem `PromptsScreen`-Muster
  (`CosmosScaffold` + `LazyColumn`, `collectAsStateWithLifecycle`). Kernbestandteile:

```kotlin
@Composable
fun PriorityMemoryScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    vm: PriorityMemoryListViewModel = hiltViewModel(),
) {
    val memories by vm.memories.collectAsStateWithLifecycle()
    val count by vm.count.collectAsStateWithLifecycle()
    val enabled by vm.enabled.collectAsStateWithLifecycle()
    val limit by vm.limit.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current

    CosmosScaffold(
        title = "Prioritaets-Gedaechtnis",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurueck", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { EnabledRow(enabled = enabled, onChange = vm::setEnabled) }
            item { LimitRow(limit = limit, onSave = vm::setLimit) }
            item { LimitWarningBanner(count = count, limit = limit) }
            items(memories, key = { it.id }) { m ->
                PriorityMemoryCard(
                    memory = m,
                    onClick = { onOpenDetail(m.id) },
                    onSetPriority = { vm.setPriority(m.id, it) },
                )
            }
        }
    }
}
```

- [ ] **Step 3: An/Aus-Zeile + Limit-Zeile (im selben File).**

```kotlin
@Composable
private fun EnabledRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Prioritaets-Gedaechtnis nutzen", Modifier.weight(1f),
                color = LocalCosmos.current.textPrimary)
            Switch(checked = enabled, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun LimitRow(limit: Int, onSave: (Int) -> Unit) {
    var text by remember(limit) { mutableStateOf(limit.toString()) }
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Von der KI beruecksichtigte Eintraege", color = LocalCosmos.current.textPrimary)
                Text("Aelteste darueber werden beim Abgleich ignoriert.",
                    style = MaterialTheme.typography.bodySmall, color = LocalCosmos.current.textSecondary)
            }
            OutlinedTextField(
                value = text,
                onValueChange = { s -> text = s.filter { it.isDigit() }.take(4) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(96.dp),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { text.toIntOrNull()?.let(onSave) }) { Text("Speichern") }
        }
    }
}
```

- [ ] **Step 4: Blinkende Warnung (Compose-Almanach-konform: alpha via graphicsLayer, kein State-Write in Composition).**

```kotlin
@Composable
private fun LimitWarningBanner(count: Int, limit: Int) {
    if (count < limit) return
    val transition = rememberInfiniteTransition(label = "limitWarn")
    val alpha by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha",
    )
    val cosmos = LocalCosmos.current
    GlassCard(Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha }) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = cosmos.crit)
            Spacer(Modifier.width(12.dp))
            Text(
                "Limit erreicht ($count von $limit). Aeltere Eintraege werden von der KI nicht mehr " +
                    "beruecksichtigt - loesche Eintraege oder erhoehe das Limit.",
                color = cosmos.crit,
            )
        }
    }
}
```

- [ ] **Step 5: Schlanke Karte (Titel + verkürzte Beschreibung + Prio-Slider).** Optik wie
  `EntropyEntryCard` (Prio-Farbverlauf + Material3-Slider), aber ohne Status/Bucket:

```kotlin
@Composable
private fun PriorityMemoryCard(
    memory: PriorityMemoryEntity,
    onClick: () -> Unit,
    onSetPriority: (Double) -> Unit,
) {
    var sliderActive by remember(memory.id) { mutableStateOf(false) }
    var liveSlider by remember(memory.id) { mutableStateOf<Float?>(null) }
    val effective = liveSlider?.toDouble() ?: memory.priority
    val ramp = priorityRampColor(effective)
    val brush = remember(ramp) { Brush.horizontalGradient(listOf(ramp.copy(alpha = 0.20f), ramp)) }

    GlassCard(Modifier.fillMaxWidth().clickable { onClick() }, tintBrush = brush) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(memory.title, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(memory.description, style = MaterialTheme.typography.bodySmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val label = liveSlider?.let { "Prioritaet ${Math.round(it)}" } ?: "Prioritaet ${memory.priority.toInt()}"
                Text(label, Modifier.weight(1f), color = LocalCosmos.current.textSecondary)
                TextButton(onClick = { sliderActive = !sliderActive }) {
                    Text(if (sliderActive) "Schliessen" else "Aendern")
                }
            }
            if (sliderActive) {
                Slider(
                    value = (liveSlider ?: memory.priority.toFloat()).coerceIn(0f, 100f),
                    onValueChange = { liveSlider = it },
                    onValueChangeFinished = {
                        liveSlider?.let { onSetPriority(it.toDouble()) }
                        sliderActive = false
                    },
                    valueRange = 0f..100f,
                    steps = 19,
                    colors = SliderDefaults.colors(thumbColor = ramp, activeTrackColor = ramp),
                )
            }
        }
    }
}
```

- [ ] **Step 6: Route registrieren.** In `Routes.kt` bei den `SETTINGS_*`-Konstanten:
  `const val SETTINGS_PRIORITY_MEMORY = "settings/priority_memory"`.

- [ ] **Step 7: Section in SettingsHomeScreen.** In `sectionsFor()` ein `SectionDef` ergänzen
  (Icon z.B. `Icons.Outlined.Tune` oder `Icons.Outlined.Insights`, accent `LocalCosmos.current.accent`):

```kotlin
SectionDef(
    icon = Icons.Outlined.Tune,
    accent = LocalCosmos.current.accent,
    title = "Prioritaets-Gedaechtnis",
    subtitle = "Gemerkte Aufgaben-Prioritaeten, die die KI fuer neue Aufgaben nutzt.",
    route = Routes.SETTINGS_PRIORITY_MEMORY,
)
```

- [ ] **Step 8: NavGraph — Listen-Screen verdrahten.** In `AppNavGraph.kt` (bei den Settings-`composable`s):

```kotlin
composable(Routes.SETTINGS_PRIORITY_MEMORY) {
    PriorityMemoryScreen(
        onBack = { nav.popBackStack(); Unit },
        onOpenDetail = { id -> nav.navigate(Routes.priorityMemoryDetail(id)) },
    )
}
```

  (`Routes.priorityMemoryDetail` kommt in Task 7 — bis dahin den `onOpenDetail`-Aufruf erst nach
  Task 7 kompilierbar; daher Task 6 + 7 zusammen kompilieren, siehe Task 7 Step 5.)

- [ ] **Step 9: Commit + Push** (ViewModel, Screen, Routes, SettingsHomeScreen, AppNavGraph),
  Message: `#NNN - feat(EntropieReductor): priority-memory settings list (toggle, limit, blinking warning)`
  danach fetch+rebase+push. (Kompilier-Check gemeinsam mit Task 7.)

---

### Task 7: Detail-Editor (Titel + Beschreibung editierbar, Slider, Löschen)

**Files:**
- Modify: `presentation/settings/prioritymemory/PriorityMemoryViewModel.kt` (Detail-VM ergänzen)
- Create: `presentation/settings/prioritymemory/PriorityMemoryDetailScreen.kt`
- Modify: `presentation/navigation/Routes.kt`, `presentation/navigation/AppNavGraph.kt`

**Interfaces (Produces):** `Routes.PRIORITY_MEMORY_DETAIL_PATTERN` + `Routes.priorityMemoryDetail(id)`,
`PriorityMemoryDetailViewModel` (lädt per id, `save(title, description, priority)`, `delete()`).

- [ ] **Step 1: Detail-VM ergänzen** (in `PriorityMemoryViewModel.kt`):

```kotlin
@HiltViewModel
class PriorityMemoryDetailViewModel @Inject constructor(
    private val repo: PriorityMemoryRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
) : ViewModel() {
    private val id: String = savedStateHandle.get<String>("memoryId").orEmpty()

    private val _state = kotlinx.coroutines.flow.MutableStateFlow<PriorityMemoryEntity?>(null)
    val state: StateFlow<PriorityMemoryEntity?> = _state

    init { viewModelScope.launch { _state.value = repo.getById(id) } }

    fun save(title: String, description: String, priority: Double, onDone: () -> Unit) =
        viewModelScope.launch {
            repo.updateContent(id, title, description, AppTime.now())
            repo.updatePriority(id, priority, AppTime.now())
            onDone()
        }

    fun delete(onDone: () -> Unit) = viewModelScope.launch { repo.delete(id); onDone() }
}
```

- [ ] **Step 2: Detail-Screen** `PriorityMemoryDetailScreen.kt` — voll editierbarer Titel +
  mehrzeilige Beschreibung + Slider + Speichern + Löschen:

```kotlin
@Composable
fun PriorityMemoryDetailScreen(
    onBack: () -> Unit,
    vm: PriorityMemoryDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    val entity = state

    CosmosScaffold(
        title = "Eintrag bearbeiten",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurueck", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        if (entity == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@CosmosScaffold
        }
        var title by remember(entity.id) { mutableStateOf(entity.title) }
        var description by remember(entity.id) { mutableStateOf(entity.description) }
        var prio by remember(entity.id) { mutableStateOf(entity.priority.toFloat()) }
        val ramp = priorityRampColor(prio.toDouble())

        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it },
                label = { Text("Titel") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("Beschreibung") }, minLines = 4, modifier = Modifier.fillMaxWidth())
            Text("Prioritaet ${prio.toInt()}", color = cosmos.textSecondary)
            Slider(value = prio.coerceIn(0f, 100f), onValueChange = { prio = it },
                valueRange = 0f..100f, steps = 19,
                colors = SliderDefaults.colors(thumbColor = ramp, activeTrackColor = ramp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { vm.save(title, description, prio.toDouble(), onBack) },
                    modifier = Modifier.weight(1f)) { Text("Speichern") }
                OutlinedButton(onClick = { vm.delete(onBack) }, modifier = Modifier.weight(1f)) {
                    Text("Loeschen", color = cosmos.crit)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Route mit Argument** in `Routes.kt`:

```kotlin
const val PRIORITY_MEMORY_DETAIL_PATTERN = "settings/priority_memory/{memoryId}"
fun priorityMemoryDetail(memoryId: String): String = "settings/priority_memory/$memoryId"
```

- [ ] **Step 4: NavGraph — Detail verdrahten** (in `AppNavGraph.kt`, Muster wie `LOOP_TEMPLATE_DETAIL_PATTERN`):

```kotlin
composable(
    route = Routes.PRIORITY_MEMORY_DETAIL_PATTERN,
    arguments = listOf(navArgument("memoryId") { type = NavType.StringType }),
) {
    PriorityMemoryDetailScreen(onBack = { nav.popBackStack(); Unit })
}
```

  (`memoryId` wird vom Detail-VM über `SavedStateHandle` gelesen — kein expliziter Parameter nötig.)

- [ ] **Step 5: Commit + Push** (Detail-VM/Screen, Routes, AppNavGraph), Message:
  `#NNN - feat(EntropieReductor): priority-memory detail editor (title/description/priority/delete)`
  danach fetch+rebase+push.

- [ ] **Step 6: Kompilier-Check (Task 6 + 7 zusammen)** — `./gradlew :app:compileDebugKotlin`
  → BUILD SUCCESSFUL. Bei Hilt-Fehler `Cannot create ViewModel` → `@HiltViewModel` + Host
  `@AndroidEntryPoint` + `hiltViewModel()` prüfen (Hilt-Almanach VM1).

---

### Task 8: Drive-Backup & Sync mitziehen (Schema v17 + Tombstone)

**Files:**
- Modify: `data/remote/drive/BackupPayload.kt`
- Modify: `data/remote/drive/SyncCoordinator.kt`
- Modify: `domain/usecase/SyncEntriesUseCase.kt`
- Modify: `data/prefs/TombstoneStore.kt`
- Modify: `data/repository/PriorityMemoryRepository.kt` (Lösch-Tombstone)

**Interfaces (Consumes):** `PriorityMemoryRepository`, `PriorityMemoryEntity`.
**Interfaces (Produces):** `BackupPriorityMemory` + `toBackup()`/`toEntity()`,
`BackupPayload.priorityMemories`, `TombstoneType.PRIORITY_MEMORY`.

- [ ] **Step 1: BackupPriorityMemory + Mappings** in `BackupPayload.kt`. Data class bei den anderen
  `BackupXxx` (z.B. nach `BackupMental`):

```kotlin
@Serializable
data class BackupPriorityMemory(
    val id: String,
    val title: String,
    val description: String = "",
    val priority: Double = 50.0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val sourceEntryId: String? = null,
)
```

  Mappings ans Datei-Ende (Muster `RecurringTemplate`):

```kotlin
fun PriorityMemoryEntity.toBackup(): BackupPriorityMemory =
    BackupPriorityMemory(id, title, description, priority, createdAt, updatedAt, sourceEntryId)

fun BackupPriorityMemory.toEntity(): PriorityMemoryEntity =
    PriorityMemoryEntity(
        id = id, title = title, description = description,
        priority = priority.coerceIn(0.0, 100.0),
        createdAt = createdAt, updatedAt = updatedAt, sourceEntryId = sourceEntryId,
    )
```

- [ ] **Step 2: Payload-Feld + Version.** In `data class BackupPayload` nach `tombstones` ergänzen
  (mit Schema-v17-KDoc im Bestandsstil):

```kotlin
    /** Schema v17 (2026-06-19): Prioritaets-Gedaechtnis. Default emptyList damit aeltere Backups (v1-v16) lesbar bleiben. */
    val priorityMemories: List<BackupPriorityMemory> = emptyList(),
```

  Klassen-KDoc-Historie (~Z.37-49) um eine v17-Zeile ergänzen. Default-`version` (~Z.58) bleibt
  abwärtskompatibel; der reale Upload setzt v17 (nächster Step).

- [ ] **Step 3: SyncCoordinator — Upload.** (a) Lazy-Dependency im Konstruktor (Muster
  `recurringTemplateRepoLazy`, ~Z.116-117): `private val priorityMemoryRepoLazy: dagger.Lazy<PriorityMemoryRepository>`.
  (b) In `performUpload()` (~Z.334) sammeln:
  `val priorityMemoryBackups = priorityMemoryRepoLazy.get().getAllForBackup().map { it.toBackup() }`.
  (c) Im `BackupPayload(...)`-Konstruktor (~Z.390-419) `version = 16` → `version = 17` und
  `priorityMemories = priorityMemoryBackups,` ergänzen.
  (d) In `mergeRemoteAdditiveLists()` (~Z.707-723) `priorityMemories` analog zu den additiven Listen
  per `rescueIfLocalEmpty(...)` mit aufnehmen (verhindert M2-Datenverlust beim Multi-Device-Merge).

- [ ] **Step 4: TombstoneType + Lösch-Tombstone.** (a) In `TombstoneStore.kt` `object TombstoneType`
  ergänzen: `const val PRIORITY_MEMORY = "priority_memory"`. (b) In
  `PriorityMemoryRepository.delete(id)` zusätzlich `markDeleted(context, TombstoneType.PRIORITY_MEMORY, id)`
  aufrufen — dazu braucht das Repo `@ApplicationContext context: Context` als zusätzliche
  Konstruktor-Dependency (Hilt-Almanach M7: nie nackter Context). Import + Konstruktor anpassen.

- [ ] **Step 5: SyncEntriesUseCase — Restore (LWW + Tombstone).** (a) Konstruktor-Dependency
  `private val priorityMemoryRepo: PriorityMemoryRepository` (~Z.77-78). (b) In `restoreFromDrive()`
  nach dem RecurringTemplate-Block (~Z.672) das LWW+Tombstone-Muster (Z.640-672) für
  `payload.priorityMemories` 1:1 nachbauen: Tombstone-Filter auf `TombstoneType.PRIORITY_MEMORY`,
  `existing` per `priorityMemoryRepo.getAllForBackup().associateBy { it.id }`, Insert/Update nach
  `updatedAt`, am Ende lokal vorhandene löschen wenn `deletedAt > local.updatedAt`
  (`priorityMemoryRepo.delete(id)`).

- [ ] **Step 6: Commit + Push** (alle in Task 8 geänderten Dateien), Message:
  `#NNN - feat(EntropieReductor): drive backup/sync for priority memory (schema v17 + tombstone)`
  danach fetch+rebase+push.

- [ ] **Step 7: Kompilier-Check** — `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.

---

### Task 9: Version-Bump, voller Build, Gerätetest, Observability

**Files:**
- Modify: `app/build.gradle.kts` (versionName/versionCode)

- [ ] **Step 1: Version-Bump.** In `app/build.gradle.kts` `versionCode` +1 und `versionName`
  erhöhen (MINOR, neues Feature — z.B. `0.16.0` → `0.17.0`; aktuellen Wert per Grep prüfen:
  `rg "versionName|versionCode" EntropieReductor/app/build.gradle.kts`).

- [ ] **Step 2: Commit + Push VOR dem Build** (nur `build.gradle.kts`), Message:
  `#NNN - chore(EntropieReductor): version bump for priority memory feature`
  danach fetch+rebase+push.

- [ ] **Step 3: Voller Debug-Build**

Run: `cd ~/proggs/EntropieReductor && ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Installieren + starten** (Geräte-ID des S23 Ultra: `R5CW206F0ZM`)

Run: `adb -s R5CW206F0ZM install -r app/build/outputs/apk/debug/app-debug.apk` und App starten.

- [ ] **Step 5: Live-Observability + manueller Test der ganzen Kette.**
  Logcat in einem Terminal: `adb -s R5CW206F0ZM logcat -s PRIO_MEMORY`. Dann am Gerät:
  1. Eine Aufgabe per Slider manuell priorisieren → Log `gelernt: '…' -> NN`.
  2. Eine **sehr ähnliche** Aufgabe einsprechen → Log `CHECKPOINT match: … ok=true (uebernommen)`;
     Aufgabe zeigt „Prioritaet KI" mit übernommenem Wert, Begründung „aus früherer ähnlicher Aufgabe".
  3. Eine **thematisch nur verwandte** Aufgabe (z.B. Federball nach Laufen) → `ok=false (kein Treffer)`.
  4. Einstellungen → „Prioritaets-Gedaechtnis": Liste zeigt Einträge; Slider in der Karte ändert Prio;
     Antippen öffnet Detail; Titel/Beschreibung editieren + speichern; Eintrag löschen.
  5. Limit auf einen Wert ≤ aktueller Anzahl setzen → **blinkende Warnung** erscheint oben.
  6. An/Aus-Schalter aus → erneut ähnliche Aufgabe einsprechen → KEIN `CHECKPOINT`-Log (Lernen/Abgleich aus).

- [ ] **Step 6: Abschluss-Verifikation.** Alle Schritte aus Step 5 grün; keine Crashes im Logcat;
  Versionsanzeige in der App zeigt die neue Version. Bei Abweichung → Root-Cause-Fix (Direktive #3),
  betroffene Sonde erneut prüfen.

---

## Self-Review (gegen die Spec)

- **Spec §2 Entscheidungen:** Abgleich im bestehenden Aufruf (Task 5) ✓ · Dedup/Update (Task 3/4) ✓ ·
  Treffer als KI-Prio + Marker (Task 5) ✓ · schlanke Karte + Detail-Editor (Task 6/7) ✓ ·
  Titel+Beschreibung an Gemini (Task 5 Step 2) ✓ · Loop lernt mit (Task 4, kein Loop-Ausschluss) ✓ ·
  An/Aus Default AN (Task 2) ✓ · nur neue Aufgaben, kein Rescore (Task 5 nur in `invoke`) ✓ ·
  Limit Default 300 + Eingabefeld + blinkende Warnung (Task 2/6) ✓.
- **Spec §4 Entity/DAO/Repo** → Task 1/4 ✓. **§6 Limit/Skalierung** → Task 2/5/6 ✓.
- **§8 Observability** → Lern-Sonde (Task 4) + Match-Checkpoint (Task 5) + Live-Tail (Task 9) ✓.
- **§9 Migration/Backup/Version** → Task 1 (Migration), Task 8 (Backup v17 + Tombstone), Task 9 (Version) ✓.
- **Placeholder-Scan:** keine TBD/TODO; alle Code-Schritte mit echtem Code; offene Live-Lookups
  (`AppTime.now()`, `versionName`) sind als explizite Grep-Checks formuliert, keine Platzhalter.
- **Typ-Konsistenz:** `learnFromManualPriority(entry, priority, now)`, `getNewest(limit)`,
  `observeCount()`, `selectMemoryToUpdate(...)`, `formatPriorityMemoriesForPrompt(...)`,
  `priorityMemories`-Parameter/-Feld durchgängig gleich benannt.
