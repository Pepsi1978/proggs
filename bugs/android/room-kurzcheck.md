# Room-Persistenz (androidx.room) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> Digest-Modell: Kurzcheck = Vorab-Pflicht (`Read` mit `limit=80`). Volltext = Pflicht bei JEDEM Fehler.
> Sektionen: **V** Versionen/2.7-Umstieg · **M** Migration/Schema · **B** Backup/Restore (WAL) ·
> **T** Threading/Coroutines/Flow · **R** @Relation · **C** TypeConverter/@Embedded · **K** Keys/Insert/FK ·
> **X** Connection/Locking/Multi-Prozess · **BLD** KSP/Build/Schema-Export.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Nach Update Crash: `A migration from N to M was required but not found` | `Migration(N,M)` + `.addMigrations(...)` ODER `@AutoMigration`; **nie** destruktiv „fixen" | M1 |
| 2 | Nach Update alle Daten weg, kein Crash | `fallbackToDestructiveMigration(...)` entfernen → echte Migration | M2 |
| 3 | `Room cannot verify the data integrity … changed schema but forgot to update the version` | `version` erhoehen + Migration; Endschema muss exakt zum Schema-JSON passen | M3 |
| 4 | Frische Installation crasht sofort mit Migrations-/Integrity-Fehler | Auto-Backup stellt ALTE DB wieder her → `dataExtractionRules`/`fullBackupContent` DB ausschliessen ODER lueckenlose Migrationen | M4 |
| 5 | `@AutoMigration` / Migration-Test findet Schema nicht | Schema-Export via Room-Gradle-Plugin `schemaDirectory(...)` + JSONs in Git einchecken | M5, BLD1 |
| 6 | Auto-Migration scheitert bei Spalten-/Tabellen-Rename oder -Delete | `AutoMigrationSpec` + `@RenameColumn`/`@DeleteColumn`/`@RenameTable`/`@DeleteTable` | M6 |
| 7 | Backup kopiert nur `.db` → beim Restore fehlen letzte Daten / korrupt | Vor dem Kopieren `PRAGMA wal_checkpoint(TRUNCATE)` (oder `close()`); sonst alle 3 Dateien | B1 |
| 8 | `database disk image is malformed` nach Backup/Restore | Keine DB-Datei kopieren/ueberschreiben bei offener Verbindung; Restore: `close()` → `-wal`/`-shm` loeschen → Datei einspielen → Room neu bauen → `integrity_check` | B4 |
| 9 | `Cannot access database on the main thread` | DAO `suspend`/`Flow`/`LiveData`; **niemals** `allowMainThreadQueries()` in Produktion | T1 |
| 10 | `withContext(Dispatchers.IO){ dao.x() }` um suspend-DAO | Weglassen — Room-suspend-DAOs sind main-safe (Anti-Pattern) | T2 |
| 11 | `withTransaction{}` haengt/Deadlock | IM Block KEINEN Dispatcher wechseln; in Tests Multi-Thread-Executor | T3, T4 |
| 12 | `Flow` feuert bei jeder Tabellen-Aenderung neu / UI flackert | `.distinctUntilChanged()` auf den DAO-Flow (Invalidation ist tabellenweit) | T6 |
| 13 | RxJava `Single` → `EmptyResultSetException` bei leerem Ergebnis | Rueckgabetyp auf `Maybe<T>` aendern | T8 |
| 14 | `@Relation`-Methode liefert inkonsistente Eltern/Kinder | DAO-Methode mit `@Transaction` annotieren | R1 |
| 15 | `@Relation` ist langsam / OOM bei vielen Kindern | Kein Filter/LIMIT moeglich → eigene `@Query` mit JOIN + WHERE/LIMIT | R2 |
| 16 | `Cannot figure out how to save this field` | `@TypeConverter`-Paar schreiben, `@TypeConverters` an **@Database** | C1, C2 |
| 17 | `@Insert(onConflict = REPLACE)` loescht ploetzlich Kind-Datensaetze | REPLACE = DELETE+INSERT → CASCADE feuert; `@Upsert` (ab 2.5) statt REPLACE | K1, K2 |
| 18 | `database is locked` / `SQLITE_BUSY` unter Last | Genau EINE `RoomDatabase`-Instanz prozessweit (Singleton) | X1 |
| 19 | Build: `KSP apoption does not match \S+=\S+: room.schemaLocation` | Leerzeichen im Pfad! Schema-/Projektpfad ohne Leerzeichen (macOS-Falle) | BLD2 |
| 20 | Nach 2.6→2.7-Upgrade neue Compile-Fehler (Nullability, abstrakte DAO-`val`) | Kotlin-Codegen ist ab 2.7.0 Default: DAO-Properties → Funktionen, Collections non-null | V1 |
| 21 | KSP laeuft nicht / `is too old for kotlin` | KSP-Praefix = Kotlin-Version exakt (2.1.0 ↔ `2.1.0-1.0.29`) | V2 |
| 22 | Du jagst einen Bug, der evtl. schon gefixt ist | Fix-Status-Tabelle unten lesen: 2.7.0 → mind. **2.7.2** patchen; viele Fixes erst in **2.8.x** | §Fix-Status |
