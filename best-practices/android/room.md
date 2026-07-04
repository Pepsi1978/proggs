# Room-Persistenz (androidx.room) — Best Practices

> **Zweite Seite der Medaille zum Bug-Almanach** [`bugs/android/room.md`](../../bugs/android/room.md):
> dort steht *was schiefgeht und wie man es umgeht*, hier *wie man Room von vornherein richtig
> aufsetzt, damit der Bug gar nicht erst entsteht*. Vor Arbeit an `@Entity`/`@Dao`/`@Database`/
> `Migration`/`@TypeConverter`/`@Relation`/DB-Backup ZUERST den Almanach-Kurzcheck, DANN diesen
> Kurzcheck lesen.
>
> **Stand:** 2026-06-15 (abgeleitet aus dem Room-Almanach, 7-Researcher-Recherche, offizielle Quellen
> developer.android.com Room-Guides + Release-Notes, Google Issue Tracker, maven.google.com).
>
> | | benutzt (live aus den Projekten) | Hinweis |
> |---|---|---|
> | Room | **2.7.0** (`gradle/libs.versions.toml`) | mind. auf **2.7.2** patchen (drop-in); Flow-/Migrations-Stabilität ideal **2.8.4** |
> | KSP | **2.1.0-1.0.29** | vorderer Teil = Kotlin-Version exakt |
> | Kotlin / AGP | **2.1.0** / **8.7.3** | `room.generateKotlin` ab 2.7.0 Default = ON (KSP) |
> | Plattform-Ziel | Android (BestJournalAndroid; künftig QuizVerse) | Drive-Backup-App → Datenverlust beim Update ist Worst-Case |
>
> **Kern-Anker:** Bei einer App mit eigenem Cloud-Backup ist **Datenverlust bei Schema-Änderungen** das
> größte Risiko. Fast jede Best Practice hier dient dem einen Ziel: Schema-Änderungen migrieren statt
> zerstören, und Backups konsistent (WAL!) sichern.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre (`Read` mit `limit=80`).
> Volltext bei Fehlern im Bereich (Stufe B).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Schema geändert | `version` erhöhen + echte `Migration(N,M)` ODER `@AutoMigration`; NIE `fallbackToDestructiveMigration` in Produktion | §2 |
| 2 | Rename/Delete von Spalte/Tabelle | `AutoMigrationSpec` + `@RenameColumn`/`@DeleteColumn`/… (echtes Rename statt Drop+Create) | §2 |
| 3 | Auto-Migration/Tests | Room-Gradle-Plugin `room { schemaDirectory("$projectDir/schemas") }`, Schema-JSONs **in Git einchecken** | §1 |
| 4 | Migration absichern | Jede Migration mit `MigrationTestHelper` für **mehrere** Startversionen testen | §2 |
| 5 | Auto-Backup vs. eigenes Backup | DB aus Android-Auto-Backup ausschließen (`dataExtractionRules`), eigenes Backup mit Schema-Version taggen | §2 |
| 6 | DB-Backup erstellen | Vor dem Kopieren `PRAGMA wal_checkpoint(TRUNCATE)` ODER `close()`; sonst alle 3 Dateien (`.db`/`-wal`/`-shm`) | §3 |
| 7 | DB-Restore | `close()` → `-wal`/`-shm` löschen → Datei einspielen → Room neu bauen → `PRAGMA integrity_check`; nur gleiche Schema-Version | §3 |
| 8 | DAO-Zugriff | `suspend`/`Flow`/`LiveData`; NIE `allowMainThreadQueries()` in Produktion; kein `withContext(IO)` um suspend-DAO | §4 |
| 9 | Flow-Query | `.distinctUntilChanged()` (Invalidation ist tabellenweit); geteilt via `stateIn`/`shareIn` | §4 |
| 10 | `withTransaction{}` | Kein Dispatcher-Wechsel im Block; keine fremde API mit eigener Transaktion darin | §4 |
| 11 | `@Relation`-DAO | IMMER `@Transaction`; gefiltert/limitiert → eigene `@Query` mit JOIN (kein WHERE/LIMIT über `@Relation`) | §5 |
| 12 | Nicht-Primitiv speichern | `@TypeConverter`-Paar, `@TypeConverters` an die **`@Database`**; `Date`↔`Long`, Enum als **Integer/Code** (Backup-stabil) | §6 |
| 13 | Insert mit Konflikt | `@Upsert` (ab 2.5) statt `@Insert(onConflict=REPLACE)` — REPLACE = DELETE+INSERT → CASCADE-Datenverlust | §7 |
| 14 | Mehrere DB-Zugriffe | Genau EINE `RoomDatabase`-Instanz prozessweit (Singleton, via Hilt `@Singleton`) | §8 |
| 15 | KSP | KSP-Suffix = Kotlin-Version exakt; nur `ksp(room-compiler)`, kein `kapt` für Room | §1 |
| 16 | Sync-/Import-Schleife (viele Writes) | ALLE Writes in EINE `withTransaction{}` (1 Flow-Invalidation statt N → kein UI-Recompose-Sturm); Netzwerk-Fetches VOR die Transaktion sammeln | §4 |

---

## 🔗 Bezugs-Tabelle: Best-Practice ↔ Bug-Almanach

| Best-Practice (diese Datei) | Bug-Gegenpart in `bugs/android/room.md` |
|---|---|
| §1 Build-Setup, KSP & Kotlin-Codegen | V1–V7, BLD1–BLD2 |
| §2 Migration & Schema (migrieren statt zerstören) | M1–M13 |
| §3 Backup/Restore mit WAL-Disziplin | B1–B5 (+ `google-drive-backup.md` für die Upload-Mechanik) |
| §4 Threading, Coroutines, Flow | T1–T10 |
| §5 `@Relation` | R1–R5 |
| §6 TypeConverter & @Embedded | C1–C4 |
| §7 Keys, Insert & Foreign Keys | K1–K2 |
| §8 Connection, Locking & Singleton | X1 |

---

## §1 Build-Setup, KSP & Kotlin-Codegen

- **KSP statt kapt:** Room-Compiler ausschließlich über `ksp("androidx.room:room-compiler:<ver>")` einbinden,
  keinen `kapt`-Eintrag für Room (schneller, Kotlin-2.x-tauglich). KSP-Version ist zweiteilig
  `<Kotlin>-<KSP-Release>` (`2.1.0-1.0.29`) — der vordere Teil MUSS exakt der Kotlin-Version entsprechen;
  bei Kotlin-Bump KSP zentral im Version-Catalog mitziehen.
- **Kotlin-Codegen (ab 2.7.0 Default ON) bewusst nutzen:** DAO-Getter als **Funktionen** (`fun getAll(): List<X>`),
  nicht als `val`-Properties; Collection-Rückgaben **non-null** (leere Liste statt `null`); Typ-Argument-Nullability
  exakt setzen (`Flow<List<X>>`). Der Kotlin-Generator ist strenger als der alte Java-Generator — diese Signaturen
  von Anfang an sauber halten erspart den Upgrade-Stolperstein.
- **Schema-Export immer an + eingecheckt:** Room-Gradle-Plugin `room { schemaDirectory("$projectDir/schemas") }`
  (KSP2-kompatibel), `exportSchema = true` (Default) lassen, Schema-JSONs **in Git einchecken**. Sie sind die
  Grundlage für `@AutoMigration` und für Migrationstests. Für Tests:
  `androidTest.assets.srcDirs += files("$projectDir/schemas")`.
- **Pfade ohne Leerzeichen:** Schema-/Projektpfad ohne Leerzeichen halten (sonst KSP-apoption-Fehler — macOS-Falle).
- **Version aktuell halten:** Auf der 2.7-Reihe mindestens **2.7.2** (drop-in, kein API-Bruch). Für Flow-/
  Migrations-/Driver-Stabilität ist **2.8.4** das Ziel (verlangt minSdk 23 + AGP ≥ 8.4 — erfüllt).

## §2 Migration & Schema — migrieren statt zerstören (Kernregel für Backup-Apps)

- **Jede Schema-Änderung = `version`-Bump + echter Migrationspfad.** Rein additiv → `@AutoMigration(from=N, to=M)`;
  sonst manuelle `Migration(N, M)` mit `.addMigrations(...)`. **Niemals** `fallbackToDestructiveMigration()` in
  Produktion (stiller Totalverlust). Wenn Komfort nötig: gezielt `fallbackToDestructiveMigrationOnDowngrade(...)`
  bzw. `...From(versions)`, damit eine versehentlich fehlende Migration weiter als Exception auffällt.
- **Lückenlose Migrationskette ab der allerersten Version** vorhalten — dann migriert auch eine per Android-Auto-Backup
  restaurierte alte DB sauber hoch (kein Crash, kein Datenverlust). Eine direkte Sprung-Migration (1→4) muss exakt
  dasselbe Endschema wie die Kette erzeugen; Migrationen idempotent schreiben.
- **Rename/Delete:** `AutoMigrationSpec` + `@RenameColumn`/`@DeleteColumn`/`@RenameTable`/`@DeleteTable` statt
  Drop+Create (Daten bleiben erhalten).
- **Manuelle `ALTER TABLE`-Disziplin:** SQLite kann nur Spalten hinzufügen/Tabellen umbenennen — kein DROP COLUMN,
  kein Typwechsel. NOT-NULL-Spalten brauchen `DEFAULT` (`ADD COLUMN c INTEGER NOT NULL DEFAULT 0`). Indizes/FKs aus
  `@Index`/`@ForeignKey` exakt wie im exportierten Ziel-Schema-JSON nachbauen (sonst identityHash-Mismatch).
- **Migrationen testen:** klassenbasierter `MigrationTestHelper`: `createDatabase(name, oldVersion)` → Testdaten →
  `runMigrationsAndValidate(name, newVersion, true, MIGRATION_1_2)`, für **mehrere** Startversionen. Das ist die
  wirksamste Vorbeugung gegen Produktions-Crashes.
- **Android-Auto-Backup gezielt steuern:** versionssensible DB via `android:dataExtractionRules` (API 31+) bzw.
  `android:fullBackupContent` mit `<exclude domain="database" path="meine.db"/>` aus dem System-Backup nehmen
  (statt pauschal `allowBackup=false`). Eigenes Drive-Backup IMMER mit Schema-Version/identityHash taggen und beim
  Restore prüfen, ob die installierte App die DB-Version versteht — sonst Restore **ablehnen** statt crashen
  (Downgrade-Schutz).

## §3 Backup/Restore mit WAL-Disziplin (nur Room/SQLite-Teil)

- **WAL ist Default** (`JournalMode.AUTOMATIC`) → die `.db` allein ist beim Kopieren **unvollständig**. Vor jedem
  Backup `PRAGMA wal_checkpoint(TRUNCATE)` (per `@RawQuery`-DAO) erzwingen und Schreibzugriffe sperren, ODER
  robuster `RoomDatabase.close()` (impliziter Checkpoint), ODER alle drei Dateien (`.db`/`-wal`/`-shm`) als Set
  sichern. Einfachste Single-File-Variante: DB mit `.setJournalMode(JournalMode.TRUNCATE)` bauen (geringere
  Schreib-Nebenläufigkeit, für eine zentrale Journal-DB meist akzeptabel).
- **Nie eine offene DB-Datei mit Fremdwerkzeugen kopieren/überschreiben** (→ „database disk image is malformed").
  **Restore-Ablauf:** `close()` → alte `-wal`/`-shm` löschen → Backup-`.db` einspielen → Room frisch instanziieren →
  `PRAGMA integrity_check`. App beim Restore möglichst neu starten.
- **Schema-Konsistenz beim Restore:** nur Backups mit exakt passender `user_version`/identityHash zurückspielen;
  kein `fallbackToDestructiveMigration()` im Restore-Pfad. Drive-Upload-Mechanik (Auth, appDataFolder, Orphans):
  siehe [`bugs/android/google-drive-backup.md`](../../bugs/android/google-drive-backup.md) +
  `best-practices-google-drive-backup.md`.

## §4 Threading, Coroutines & Flow

- **DAOs asynchron:** `suspend fun` (One-Shot), `Flow<T>` (observable), `LiveData`/RxJava/`ListenableFuture`.
  `allowMainThreadQueries()` ist KEIN Fix — nur Debug/Test, nie Produktion (holt die ANRs zurück).
- **Kein `withContext(Dispatchers.IO)` um suspend-DAOs** — Room-suspend-DAOs sind main-safe (Anti-Pattern,
  falscher Pool, schwerer testbar). Query-Pool via `RoomDatabase.Builder.setQueryExecutor(...)`. Ausnahme:
  manuelles `db.runInTransaction { ... }` mit **blockierenden** DAOs braucht einen eigenen Hintergrund-Dispatcher.
- **Flow:** `.distinctUntilChanged()` ist Pflicht (InvalidationTracker arbeitet tabellenweit → sonst UI-Flackern/
  Endlos-Recomposition); sinnvolles `equals()` (data class) als Voraussetzung. Geteilte Daten via
  `stateIn(scope)`/`shareIn(scope)` im ViewModel cachen (Room-Flow ist cold → sonst pro Collector eine Query).
- **`withTransaction{}`-Disziplin:** alle DB-Operationen im Block belassen, **ohne** Dispatcher-Wechsel; keine
  fremde suspend-API aufrufen, die intern eine eigene Transaktion öffnet (Deadlock). In Tests echten
  Multithread-Executor + `runBlocking` nutzen (kein Single-Thread-Test-Dispatcher für DB-Arbeit).
- **RxJava:** bei optionalem Ergebnis `Maybe<T>` statt `Single<T>` (sonst `EmptyResultSetException`).
- **Driver:** Wer den neuen `AndroidSQLiteDriver` mit suspending Transactions / `setAutoCloseTimeout` nutzt,
  sollte ≥ **2.8.2** einplanen (Deadlock-Fixes).
- **Sync-/Import-Schleifen batchen (2026-07-04, EntropieReductor-Fund):** Schreibt eine Schleife pro Element
  einzeln (`upsert`/`UPDATE`), invalidiert JEDER Write die tabellenweiten Flows → pro Write ein neuer UI-State
  → sichtbarer Recompose-Sturm/Jank während des Syncs. Stattdessen ALLE Writes der Schleife in **EINE**
  `withTransaction { }` (genau 1 Invalidation am Ende). Netzwerk-/Fremd-suspend-Arbeit (API-Fetches, `delay`)
  gehört VOR die Transaktion: erst Ergebnisse in eine Liste sammeln, dann atomar schreiben (Bug-Gegenpart:
  Almanach T11).

## §5 `@Relation`

- **Jede `@Relation`-DAO-Methode mit `@Transaction` annotieren** (Eltern + Kinder atomar; Compiler warnt sonst).
- **Filtern/Sortieren/Limitieren geht nicht über `@Relation`** (lädt immer ALLE Kinder → N+1/OOM). Dafür eine
  eigene `@Query` mit JOIN + WHERE/LIMIT/ORDER (Multimap-Return oder Projektions-POJO); große Listen paginieren
  (Paging 3, Rooms generierte PagingSource nutzen).
- **Verschachtelung flach halten;** tiefe Hierarchien als dedizierte JOIN-Query.
- **N:M nur mit `@Junction`** (Cross-Ref-Tabelle mit Composite-PK, `associateBy = Junction(...)` in beiden Richtungen).
- `@Relation`-Collections sind nie `null` (leere Liste) → Code auf „leere Liste = keine Kinder" auslegen, Rückgaben non-null.

## §6 TypeConverter & @Embedded

- **Converter-Paar** (hin/zurück zu `Long`/`String`) für jeden nicht-primitiven Typ; `@TypeConverters` an die
  **`@Database`**-Klasse hängen (app-weit; an Entity/Dao nur lokal wirksam).
- **`Date` ↔ `Long`** (Timestamp), nicht als locale-abhängiger String. Nullable-Signaturen + `null` durchreichen
  (`fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }`).
- **Enums Backup-stabil als Integer/Code** speichern (nicht über den Default-Namen-Converter ab 2.3) — so brechen
  Enum-Refactorings/Umbenennungen alte Drive-Backups nicht.

## §7 Keys, Insert & Foreign Keys

- **`@Upsert` (ab Room 2.5) statt `@Insert(onConflict = REPLACE)`** — REPLACE ist DELETE+INSERT und feuert
  `ON DELETE CASCADE` → unbemerkter Verlust von Kind-Datensätzen.
- **Foreign Keys** mit passendem `@Index` auf der FK-Spalte (sonst Full-Table-Scan + Compiler-Warnung); `onDelete`/
  `onUpdate` bewusst wählen (CASCADE-Wirkung kennen, siehe Upsert oben).
- **Composite-PK** für Cross-Ref-/Junction-Tabellen.

## §8 Connection, Locking & Singleton

- **Genau EINE `RoomDatabase`-Instanz prozessweit** (Singleton) — über Hilt als `@Singleton` bereitstellen.
  Mehrere Instanzen → `database is locked`/`SQLITE_BUSY` unter Last. DAOs/DB via Hilt injizieren (siehe
  [`bugs/android/hilt-dagger.md`](../../bugs/android/hilt-dagger.md)).
- Multi-Prozess-Zugriff vermeiden (eine DB pro Prozess); wenn unvermeidbar, dedizierte Multi-Prozess-Strategie.

---

## Pflicht-Checkliste vor Room-Arbeit

- [ ] Almanach-Kurzcheck ([`bugs/android/room.md`](../../bugs/android/room.md)) + dieser Kurzcheck gelesen?
- [ ] Schema-Änderung → `version`-Bump + echte Migration + Schema-JSON eingecheckt + Migrationstest?
- [ ] Kein `fallbackToDestructiveMigration()` im Produktions-/Restore-Pfad?
- [ ] Backup mit WAL-Checkpoint/`close()`; Restore mit `integrity_check` + Schema-Versions-Prüfung?
- [ ] DAOs `suspend`/`Flow`, `.distinctUntilChanged()`, kein `allowMainThreadQueries()`?
- [ ] `@Relation` mit `@Transaction`; `@Upsert` statt REPLACE; ein DB-Singleton (Hilt)?
- [ ] Version mindestens 2.7.2 (ideal 2.8.4)?
