# Room-Persistenz (androidx.room) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
