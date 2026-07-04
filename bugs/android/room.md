# Bekannte Bugs: Room-Persistenz (androidx.room)

> PFLICHT-LESEN vor Arbeit an Room (`@Entity`, `@Dao`, `@Database`, `Migration`, `@TypeConverter`,
> `@Relation`, DB-Backup/Restore) in BestJournalAndroid (und kuenftig QuizVerse).
> Stand: recherchiert am **2026-06-15** mit **7 Researchern parallel** (offizielle Quellen zuerst:
> developer.android.com Room-Guides + Release-Notes, Google Issue Tracker, maven.google.com) + dediziertem
> **Fix-Status-Lauf** (2 Researcher; Versions-Timeline + 12 `b/`-Issues hart gegen die Release-Notes geprueft).
> ~64 Eintraege in 9 Sektionen (inkl. Fix-Status-Tabelle). **Ergaenzt 2026-07-04:** T11
> (Sync-Schleife mit Einzel-Writes → N Invalidationen → UI-Recompose-Sturm; EntropieReductor-Fund).
>
> **Versions-Anker (live aus den Projekten):** Room **2.7.0** · KSP **2.1.0-1.0.29** · Kotlin **2.1.0** ·
> AGP **8.7.3** · `room.generateKotlin` = Default ON (KSP). (BestJournalAndroid `gradle/libs.versions.toml`;
> QuizVerse hat aktuell **kein** Room-Setup — gleicher Anker, sobald es eins bekommt.)
> **Aktuellste Versionen (Stand Recherche):** letzte 2.7.x = **2.7.2** (18.06.2025), hoechste stabile 2.x =
> **2.8.4** (19.11.2025), **Room 3.0 nur Alpha** (3.0.0-alpha01 ab 11.03.2026, kein stable). Es gibt **kein**
> 2.7.3–2.7.7 — nach 2.7.2 kam direkt 2.8.0.
>
> **Abgrenzung (was steht woanders):**
> - **Room-Runtime im Framework-Kontext** (Lifecycle/Permissions/Services drumherum) → [`android-platform.md`](android-platform.md) §5 hat eine kompakte **Uebersicht**; **DIESE Datei ist der dedizierte Tiefen-Almanach** fuer Room.
> - **Google-Drive-Upload-Mechanik** (appDataFolder, Auth, Changes-API, Orphans) → [`google-drive-backup.md`](google-drive-backup.md). Hier steht **nur** der Room/SQLite-Teil des Backups (WAL-Checkpoint, `close()`, Datei-Konsistenz, identityHash beim Restore).
> - **Gradle/AGP/KSP-Plugin allgemein, R8-Keep-Regeln** → [`../android-build/gradle.md`](../android-build/gradle.md) bzw. [`../android-build/r8.md`](../android-build/r8.md). Hier nur Room-spezifische Build-Fallen (Schema-Export, KSP-Pfad-Regex, Kotlin-Codegen).
> - **DI von DAOs/DB ueber Hilt** → [`hilt-dagger.md`](hilt-dagger.md) (DAO als `@Provides`/`@Singleton`).
>
> Zweite Seite (wie macht man es von vornherein richtig): `best-practices/android/android-platform.md` §3 „Saubere Room-Migrationsstrategie & Runtime-Disziplin" + `best-practices-google-drive-backup.md` (WAL/Restore).

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
| 23 | Sync-/Import-Schleife schreibt pro Element einzeln (upsert/UPDATE), UI ruckelt/flackert waehrenddessen | ALLE Writes der Schleife in EINE `withTransaction{}` — N Invalidationen → 1; Netzwerk-Fetches VOR die Transaktion ziehen | T11 |

---

## V) Versionen, 2.7-Umstieg & Build-Setup

### V1. Kotlin-Codegen ist ab Room 2.7.0 (KSP) standardmaessig AN ⭐ HAEUFIG (Upgrade-Stolperstein Nr. 1)
- **Symptom:** Nach dem Sprung 2.6 → 2.7 ploetzlich Compile-Fehler, die unter Java-Codegen nie kamen: (a) abstrakte DAO-Getter als `val`/Property verboten („must be a function"); (b) **nullable Collection-Rueckgaben** verboten (`List<X>?`); (c) Nullability von Typ-Argumenten wird streng geprueft (`Flow<List<X>>` vs. `Flow<List<X>?>`).
- **Ursache:** `room.generateKotlin` wurde in 2.6.0 eingefuehrt (opt-in) und ist **ab 2.7.0 Default = true**, sobald via KSP verarbeitet wird. Der Kotlin-Generator ist strikter als der alte Java-Generator. **Genau eure Konstellation** (Room 2.7.0 + KSP).
- **Versionen:** Restriktionen ab 2.6.0, **scharf ab 2.7.0** (per Design, kein „Fix" geplant).
- **FIX (funktionserhaltend):** DAO-Property-Getter in Funktionen umschreiben (`fun getAll(): List<X>` statt `val all: List<X>`); Collection-Rueckgaben non-null (leere Liste statt `null`); Typ-Argument-Nullability exakt setzen. Notnagel beim Massen-Umstieg: `ksp { arg("room.generateKotlin", "false") }` (zurueck zu Java-Codegen, **nicht** fuer KMP). Kein Feature geht verloren — nur Signatur-Anpassungen.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room (2.6.0 / 2.7.0)

### V2. KSP-Versionssuffix muss exakt zur Kotlin-Version passen
- **Symptom:** `ksp-X.Y.Z is too old for kotlin-A.B.C`; Annotation-Processing laeuft gar nicht, Room-Klassen (`*_Impl`) entstehen nicht.
- **Ursache:** KSP-Version ist zweiteilig `<Kotlin>-<KSP-Release>` (`2.1.0-1.0.29`). Der vordere Teil MUSS exakt der Kotlin-Version entsprechen.
- **Versionen:** alle. **Projekt-Anker korrekt gepaart:** Kotlin 2.1.0 ↔ KSP `2.1.0-1.0.29`.
- **FIX:** KSP-Suffix zentral im Version-Catalog an die Kotlin-Version koppeln; bei Kotlin-Bump KSP mitziehen.
- **Quelle:** https://github.com/google/ksp/releases · https://developer.android.com/build/migrate-to-ksp

### V3. kapt fuer Room veraltet/langsam — Umstieg auf KSP, gemischte kapt+KSP-Falle
- **Symptom:** Lange Builds; oder nach KSP-Einfuehrung wird trotzdem Java-Code generiert / doppelte Generierung / `room.generateKotlin` greift nicht.
- **Ursache:** Room-Compiler noch (auch) ueber `kapt("androidx.room:room-compiler")` eingebunden statt nur `ksp(...)`.
- **Versionen:** durchgehend; in 2.7 verstaerkt (Kotlin-2.0-Target).
- **FIX:** Vollstaendig auf KSP: `ksp("androidx.room:room-compiler:<ver>")`, **keinen** `kapt`-Eintrag fuer den Room-Compiler mehr (kapt darf fuer andere Libs bleiben).
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room · https://developer.android.com/build/migrate-to-ksp

### V4. KSP2 vs. KSP1 — Native/KMP-Crashes, `@Serializable`-Entity-Resolver
- **Symptom:** (a) KSP1 auf Kotlin 2.1.x: `NullPointerException … cannot be cast … XAnnotation` beim Bauen nativer/KMP-Targets. (b) KSP2 + Entity mit `@Serializable`: `[MissingType] … not all of its dependencies could be resolved` / `RoomKspProcessor was unable to process`.
- **Ursache:** (a) KSP1 liefert nicht gesetzte Annotation-Werte als `null` statt Default. (b) Upstream-KSP2-Resolver-Problem („awaiting upstream fix"), nicht Room selbst.
- **Versionen:** (a) gefixt ab 2.7.0-rc01 (b/396607230) — **in eurer 2.7.0 enthalten**. (b) Status **offen/Upstream** (KSP #1896).
- **FIX:** Bei reinem Android: meist unkritisch. Gegen (b): `@Serializable` von der Entity trennen (separate DTO-Klasse), ODER auf KSP1 bleiben, ODER KSP auf **2.1.10-1.0.30+** heben (stabilerer KSP2). KMP empfiehlt KSP2.
- **Quelle:** https://github.com/google/ksp/issues/1896 · https://github.com/google/ksp/issues/1788 · https://issuetracker.google.com/396607230

### V5. `room.expandProjection` wurde entfernt
- **Symptom:** Build-Fehler/Warnung „unbekannte Processor-Option `room.expandProjection`" bei Migration aelterer Setups.
- **Ursache:** Die experimentelle Option wurde im Kotlin-Codegen-Umbau entfernt.
- **Versionen:** entfernt im 2.7-Umfeld.
- **FIX:** Option ersatzlos aus dem KSP-/kapt-Args-Block streichen; Star-Projection-Queries durch explizite `@Query`-Projektion bzw. dedizierte DTO-Klassen abdecken.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room (Processor-Options 2.7.0)

### V6. Schon in 2.7.0-Stable enthaltene rc-Fixes — NICHT als aktive Bugs jagen
- **Hinweis:** Folgende frueher gemeldeten Bugs sind **im 2.7.0-Stable bereits gefixt** (kamen in den rc/alpha-Builds davor) — also bei euch erledigt:
  - Connection-Pool reimplementiert + `busy_timeout` gesetzt (b/380088809, rc01/rc03) → weniger „database is locked".
  - `InterruptedException` bei unterbrochenem Thread in blockierenden DAO-Calls (b/400584611, rc03).
  - Compiler-Crash `IllegalArgumentException: not a valid name` bei `inline`/`value class`-Parametern (b/388299754, beta01).
  - Writer-Connection invalidierte Tabellen am Ende nicht → Flow emittierte nicht (b/340606803, rc02).
  - Auto-Migration: neue Spalte auf **FTS**-Tabelle falsch (b/348227770, rc02); FK-Check zu frueh (b/352085724, alpha06).
  - `room-paging`: ungueltiger Code bei `@Relation` + `PagingSource` (alpha11).
- **FIX:** Nichts zu tun — nur wissen, dass diese in 2.7.0 nicht mehr auftreten.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room (2.7.0-alpha06/alpha11/alpha12/rc01/rc02/rc03)

### V7. 2.7.0 hat noch offene Bugs — mindestens auf 2.7.2 patchen ⭐ HAEUFIG
- **Symptom:** Diverse Compiler-/Schema-/Verhaltensfehler, die erst NACH 2.7.0 gefixt wurden (Details unten in §Fix-Status).
- **Ursache:** 2.7.0 ist der erste Stable der 2.7-Reihe; 2.7.1 und 2.7.2 sind reine Patch-Releases.
- **Versionen:** 2.7.1 (23.04.2025), 2.7.2 (18.06.2025).
- **FIX:** Auf **2.7.2** heben (drop-in, kein API-Bruch) — behebt ProvidedTypeConverter-Crash (2.7.1), `runInTransaction`+Driver (2.7.1), fuehrender SQL-Kommentar (2.7.2), Schema-Export bei nativen KSP-Sources (2.7.2), Connection-Timeout-Logging (2.7.2). Fuer Migrations-/Flow-Stabilitaet idealerweise **2.8.4** (verlangt minSdk 23 + AGP ≥ 8.4 — beides bei euch erfuellt).
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room

---

## M) Migration & Schema (Kern-Fehlerquelle)

### M1. „A migration from X to Y was required but not found" (IllegalStateException) ⭐ HAEUFIG
- **Symptom:** App crasht beim ersten Oeffnen nach einem Update mit `IllegalStateException: A migration from 1 to 2 was required but not found` — **nur bei Bestandsnutzern** (Reinstall-Tester merken nichts).
- **Ursache:** `version` in `@Database` erhoeht (oder Schema geaendert, was den Bump erzwingt), aber kein Migrationspfad registriert.
- **Versionen:** per Design, alle Versionen (Schutzmechanismus, kein Bug).
- **FIX (funktionserhaltend):** `Migration(N, M)` mit konkretem SQL schreiben + `.addMigrations(MIGRATION_1_2)`; bei rein additiven Aenderungen `@AutoMigration(from=1, to=2)` (braucht Schema-Export, M5). **Niemals** als „Fix" `fallbackToDestructiveMigration()` (= Datenverlust, M2). Bei Drive-Backup-Apps ist Datenverlust beim Update das Worst-Case-Szenario.
- **Quelle:** https://developer.android.com/training/data-storage/room/migrating-db-versions

### M2. `fallbackToDestructiveMigration()` = stiller Datenverlust
- **Symptom:** Nach Schema-Update sind **alle** Nutzerdaten weg — kein Crash, keine Fehlermeldung. Greift auch, wenn eine Migration eine Exception wirft.
- **Ursache:** Weist Room an, bei fehlendem/fehlgeschlagenem Migrationspfad die DB zu droppen und neu anzulegen — by design. Wird oft „temporaer zum Ruhigstellen" gesetzt (gegen M1) und vergessen.
- **Versionen:** destruktives Verhalten = per Design, alle Versionen.
- **FIX (funktionserhaltend):** In Produktion **nicht** verwenden (nur reine Cache-DBs). Echten Migrationspfad schreiben (M1). Wenn Komfort gebraucht: gezielt `fallbackToDestructiveMigrationOnDowngrade(...)` (nur beim selteneren Downgrade) oder `fallbackToDestructiveMigrationFrom(..., versions)` (nur aus bestimmten kaputten Alt-Versionen) — so faellt eine unbeabsichtigt fehlende Migration weiter als Exception auf.
- **⚠️ Ehrlichkeits-Hinweis zur Signatur:** Mehrere Sekundaerquellen behaupten, die parameterlose `fallbackToDestructiveMigration()` sei **ab Room 2.7** deprecated zugunsten von `fallbackToDestructiveMigration(dropAllTables: Boolean)`. Das liess sich in den offiziellen Release-Notes **nicht als 2.7-Aenderung belegen** (die 2.7.x/2.8.x-Notes enthalten keinen solchen Eintrag; die Deprecation der parameterlosen Variante ist vermutlich aelter, ~2.4.0). Belegt ist nur: **Room 3.0** (3.0.0-alpha02, b/438041176) gab `dropAllTables` wieder einen Default-Wert. Praxis unabhaengig davon: `dropAllTables` explizit setzen, falls euer Compiler es verlangt. (Anmerkung: `android-platform.md` §5.2 nennt „ab 2.7 Pflichtargument" — das ist nicht hart belegt.)
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room3 (b/438041176) · https://developer.android.com/reference/kotlin/androidx/room/RoomDatabase.Builder

### M3. „Room cannot verify the data integrity … changed schema but forgot to update the version" (identityHash)
- **Symptom:** Crash beim DB-Open: `IllegalStateException: Room cannot verify the data integrity.`
- **Ursache:** Room legt aus dem Schema einen **identityHash** in `room_master_table` ab. Schema geaendert (Spalte/Index/Typ/FK), `version` nicht erhoeht → Mismatch. **Auch:** Migration laeuft, aber das Endschema entspricht nicht **byte-genau** dem erwarteten (Index/FK/`DEFAULT`/`NOT NULL` in der manuellen Migration vergessen → M7).
- **Versionen:** per Design, alle Versionen.
- **FIX (funktionserhaltend):** `version` erhoehen UND passende Migration. Bei Mismatch trotz korrekter Version: exportiertes Schema-JSON der Zielversion mit dem nach Migration erzeugten Schema vergleichen — meist fehlt ein Index/FK/Default. **Nicht** DB loeschen oder destruktiv „fixen".
- **Quelle:** https://developer.android.com/training/data-storage/room/migrating-db-versions

### M4. Auto-Backup-Falle: `allowBackup=true` stellt ALTE DB wieder her → Integrity-/Migrations-Crash ⭐ (kritisch fuer Drive-Backup)
- **Symptom:** Frisch installierte App crasht **sofort** mit M1/M3 — obwohl es eine Neuinstallation ist. Geraeteabhaengig, oft nur in Produktion; Datenloeschen/Reinstall hilft nicht, weil das Backup sofort wieder zurueckgespielt wird.
- **Ursache:** Android Auto-Backup (`android:allowBackup` Default **true**) sichert die DB-Datei in die Cloud und stellt sie bei Reinstall/neuem Geraet **vor** App-Start wieder her. Die wiederhergestellte DB hat altes Schema/alten identityHash, das neue APK erwartet ein neueres → kein Migrationspfad bzw. Hash-Mismatch. **Kann mit eurem eigenen Drive-Backup kollidieren.**
- **Versionen:** per Design (Plattformverhalten + Room-Schutz), alle Versionen.
- **FIX (funktionserhaltend):** (1) Lueckenlose Migrationspfade ab der allerersten Version vorhalten — dann wird eine restaurierte alte DB sauber hochmigriert (kein Datenverlust). (2) Auto-Backup gezielt steuern: `android:dataExtractionRules` (ab API 31) bzw. `android:fullBackupContent` mit `<exclude domain="database" path="meine.db"/>`, statt naiv `allowBackup=false`. So bleiben SharedPrefs etc. im Backup, nur die versionssensible DB nicht. Eigenes Drive-Backup IMMER mit Schema-Version/Hash taggen und beim Restore pruefen.
- **Quelle:** https://imunique-zj.medium.com/lesson-learnt-from-allowbackup-and-changing-room-database-schema-d8ed0a0acddd · https://developer.android.com/training/data-storage/room/migrating-db-versions

### M5. Fehlender Schema-Export bricht Auto-Migration / Migration-Test
- **Symptom (Build):** `Schema export directory is not provided to the annotation processor so we cannot export the schema`. **(Test):** `FileNotFoundException: Cannot find the schema file in the assets folder.`
- **Ursache:** `@AutoMigration` und `MigrationTestHelper` brauchen die exportierten **JSON-Schemas** beider Versionen. Ohne konfiguriertes Verzeichnis exportiert KSP nichts; oder JSONs nicht eingecheckt; oder `exportSchema = false`.
- **Versionen:** per Design. Bug, bei dem Annotation-Werte bei **nativen KSP-Sources** falsch gelesen wurden und Schemas teils ausblieben → gefixt **2.7.2** (b/416549580).
- **FIX (funktionserhaltend):** Room-Gradle-Plugin nutzen: `room { schemaDirectory("$projectDir/schemas") }` (sauberste, KSP2-kompatible Variante). Alternativ `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`. `exportSchema = true` (Default) lassen. Schema-JSONs **in Git einchecken**. Fuer Tests: `androidTest.assets.srcDirs += files("$projectDir/schemas")`.
- **Quelle:** https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schema · https://github.com/robolectric/robolectric/issues/6650

### M6. Auto-Migration scheitert bei Spalten-/Tabellen-Rename oder -Delete → `AutoMigrationSpec`
- **Symptom:** Compile-Fehler beim Annotation-Processing: Room kann Rename nicht von Delete+Add unterscheiden („ambiguous").
- **Ursache:** Auto-Migration erkennt nur additive Aenderungen automatisch; Umbenennungen/Loeschungen brauchen explizite Hinweise.
- **Versionen:** per Design seit Auto-Migrations (2.4.0).
- **FIX (funktionserhaltend):** Klasse implementiert `AutoMigrationSpec` mit `@RenameColumn`/`@DeleteColumn`/`@RenameTable`/`@DeleteTable` und wird via `@AutoMigration(from=N, to=M, spec = MySpec::class)` referenziert (echtes Rename statt Drop+Create → Daten bleiben). Bei komplexen Aenderungen manuelle `Migration`.
- **Quelle:** https://developer.android.com/training/data-storage/room/migrating-db-versions · https://medium.com/androiddevelopers/room-auto-migrations-d5370b0ca6eb

### M7. Manuelle Migration: `ALTER TABLE`-Fallen (NOT NULL ohne Default; Index/FK vergessen)
- **Symptom:** Migration laeuft scheinbar durch, danach trotzdem identityHash-Mismatch (M3); oder `SQLiteException` waehrend der Migration („Cannot add a NOT NULL column with default value NULL").
- **Ursache:** SQLite `ALTER TABLE` kann nur Spalten **hinzufuegen** und Tabellen umbenennen — kein DROP COLUMN, kein Typwechsel, kein Constraint-Change; `ADD COLUMN … NOT NULL` ohne `DEFAULT` ist bei vorhandenen Zeilen verboten. Indizes/ForeignKeys aus `@Index`/`@ForeignKey` werden bei `ALTER TABLE` NICHT automatisch angelegt → Schema weicht ab.
- **Versionen:** per Design (SQLite), alle Versionen.
- **FIX (funktionserhaltend):** NOT-NULL-Spalten mit `DEFAULT`: `ALTER TABLE t ADD COLUMN c INTEGER NOT NULL DEFAULT 0`. Fuer Constraint-/Typaenderung/Spalten-Loeschung das Muster „neue Tabelle mit Zielschema → `INSERT INTO new SELECT … FROM old` → alte droppen → umbenennen". **Alle** Indizes/FKs exakt wie im exportierten Ziel-Schema-JSON anlegen.
- **Quelle:** https://developer.android.com/training/data-storage/room/migrating-db-versions · https://infinum.com/handbook/android/common-android/room-migrations

### M8. Migrationen werden selten getestet → `MigrationTestHelper` (2.7-Konstruktor-/Driver-Aenderung)
- **Symptom:** Migrationsfehler fallen erst in Produktion auf; oder bestehender Migration-Test kompiliert nach Room-Update nicht mehr (Konstruktor deprecated).
- **Ursache:** Ungetestete Migrationen sind die haeufigste Quelle fuer Produktions-Crashes. In der KMP-/Driver-Welt von 2.7 gibt es neue `MigrationTestHelper`-Konstruktoren (Datenbankklasse statt nur Schema-Pfad bzw. `SQLiteDriver`); nur die **klassenbasierte** Variante zieht Auto-Migrationen automatisch mit ein.
- **Versionen:** Konstruktor-Deprecations laufend; KMP-/Driver-Konstruktoren ab 2.7.0.
- **FIX (funktionserhaltend):** Klassenbasierten `MigrationTestHelper`-Konstruktor verwenden. Ablauf: `helper.createDatabase(name, oldVersion)` → Testdaten einfuegen → `helper.runMigrationsAndValidate(name, newVersion, true, MIGRATION_1_2)`. Schema-Export als Test-Asset (M5). Jede Migration vor Release fuer **mehrere** Startversionen testen — faengt M3 und M7 ab.
- **Quelle:** https://developer.android.com/training/data-storage/room/migrating-db-versions#test

### M9. Reihenfolge/Mehrfachausfuehrung bei Versionssprung ueber mehrere Stufen
- **Symptom:** Bei Sprung 1 → 4 schlaegt eine Migration fehl oder das Endschema ist falsch; einzelne Migrationen scheinen uebersprungen/doppelt.
- **Ursache:** Room verkettet registrierte Einzel-Migrationen (1→2, 2→3, 3→4) und waehlt den kuerzesten Pfad. Probleme bei (a) fehlender Zwischenmigration (Luecke → M1), (b) zusaetzlicher direkter „Abkuerzungs"-Migration (1→4), die vom Kettenergebnis abweicht, (c) nicht-idempotenter Migration.
- **Versionen:** per Design, alle Versionen.
- **FIX (funktionserhaltend):** Lueckenlose Einzel-Migrationen pro Schritt bereitstellen. Eine direkte Sprung-Migration muss exakt dasselbe Endschema wie die Kette erzeugen. Migrationen idempotent/defensiv schreiben; mit `runMigrationsAndValidate` fuer mehrere Startversionen testen.
- **Quelle:** https://developer.android.com/training/data-storage/room/migrating-db-versions

### M10. Downgrade-Crash bei Versions-Rollback / aelterem Backup (relevant fuer Drive-Restore)
- **Symptom:** Crash beim Start, wenn eine **aeltere** App-Version (oder ein aelteres/neueres Backup ueber Kreuz) auf eine inkompatible DB-Version trifft — Room kennt keinen Downgrade-Pfad.
- **Ursache:** Room migriert nur aufwaerts. Eine wiederhergestellte neuere DB unter aelterer App = Downgrade ohne Pfad → IllegalStateException.
- **Versionen:** per Design, alle Versionen.
- **FIX (funktionserhaltend):** Wenn Daten beim Downgrade verzichtbar: `fallbackToDestructiveMigrationOnDowngrade(...)` (zerstoert NUR beim Downgrade). Wenn Daten erhalten bleiben muessen: eigenes Drive-Backup mit Schema-Version taggen und beim Restore pruefen, ob die installierte App die DB-Version versteht — sonst Restore **ablehnen** statt crashen.
- **Quelle:** https://developer.android.com/training/data-storage/room/migrating-db-versions

### M11. Pre-packaged DB wird destruktiv migriert TROTZ vorhandenem Migrationspfad → Datenverlust
- **Symptom:** Bei einer vorbefuellten DB (`createFromAsset`/`createFromFile`) wird destruktiv migriert, obwohl ein Migrationspfad existiert → Userdaten weg.
- **Ursache:** Fehlentscheidung in der Migrationslogik fuer Pre-Packaged-DBs. (Zusaetzlich dokumentiert: greift destruktiver Fallback UND liegt eine Prepackaged-DB der Zielversion vor, **ueberschreibt** Room die rekonstruierte DB mit dem Asset-Inhalt — per Design.)
- **Versionen:** vorhanden in **2.7.x**, **gefixt ab 2.8.0** (b/432634197, in 2.8.0-rc02). In 2.7.0 NICHT gefixt.
- **FIX (funktionserhaltend):** Immer echte Migrationspfade definieren (haben Vorrang vor dem destruktiven Prepackaged-Fallback). Wenn ihr `createFromAsset` mit echten Migrationen kombiniert: auf **2.8.x** updaten. Destruktiven Fallback nur als letzte Reissleine.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room (2.8.0-rc02) · https://developer.android.com/training/data-storage/room/prepopulate

### M12. Tabellen-/View-Namen bei destruktiver Migration nicht escaped
- **Symptom:** Fehler bei destruktiver Migration, wenn Tabellen-/View-Namen Sonderzeichen oder SQL-Keywords enthalten.
- **Ursache:** Fehlendes Quoting im Drop-/Recreate-Pfad. (Separat in 2.7.0-alpha12 gefixt: destruktive Migration droppte **Views** nicht mit, b/381518941 — in 2.7.0 enthalten.)
- **Versionen:** Escaping vorhanden inkl. 2.7.0, **gefixt ab 2.8.0** (b/427095319, in 2.8.0-beta01).
- **FIX (funktionserhaltend):** Tabellen-/View-Namen ohne SQL-Keywords/Sonderzeichen waehlen; bei Bedarf auf 2.8.x updaten.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room (2.8.0-beta01, 2.7.0-alpha12) · https://issuetracker.google.com/427095319

### M13. In-Memory-DB unterstuetzt kein Prepopulate
- **Symptom:** `createFromAsset()`/`createFromFile()` werden mit `inMemoryDatabaseBuilder` still ignoriert/schlagen fehl.
- **Ursache:** per Design.
- **Versionen:** alle.
- **FIX (funktionserhaltend):** Prepopulate nur mit file-basiertem `databaseBuilder` nutzen.
- **Quelle:** https://developer.android.com/training/data-storage/room/prepopulate

---

## B) Backup / Restore: WAL · Checkpoint · close (nur Room/SQLite-Teil)

> Drive-Upload-Mechanik (Auth, appDataFolder, Changes-API, Orphans) → [`google-drive-backup.md`](google-drive-backup.md).

### B1. WAL ist Default → nur `.db` kopieren = unvollstaendiges/inkonsistentes Backup ⭐ HAEUFIG
- **Symptom:** Die `.db` wird hochgeladen, aber beim Restore fehlen die letzten Schreibvorgaenge / Backup wirkt veraltet oder ist korrupt.
- **Ursache:** Room nutzt `JournalMode.AUTOMATIC` → auf den meisten Geraeten **WAL (Write-Ahead Logging)**. Noch nicht eingecheckpointete Aenderungen liegen in `<name>.db-wal` (+ `<name>.db-shm`), **nicht** in der `.db`.
- **Versionen:** per Design (WAL default), alle Versionen.
- **FIX (funktionserhaltend):** Vor dem Kopieren Checkpoint erzwingen — per `@RawQuery`-DAO `PRAGMA wal_checkpoint(TRUNCATE)` (schreibt WAL in die `.db` zurueck **und** leert `-wal` → danach reicht die reine `.db`). Schreibzugriffe waehrend des Checkpoints sperren. Alternativ alle drei Dateien (`.db`, `-wal`, `-shm`) als Set sichern.
- **Quelle:** https://www.sqlite.org/wal.html · https://androidexplained.github.io/android/room/2020/10/03/room-backup-restore.html

### B2. `JournalMode.TRUNCATE` als robuste Einzeldatei-Alternative
- **Symptom:** Hantieren mit drei Dateien ist fehleranfaellig; Backup/Restore soll mit einer Datei klappen.
- **Ursache:** WAL bedeutet zwangslaeufig mehrere Dateien.
- **Versionen:** per Design.
- **FIX (funktionserhaltend):** DB mit `.setJournalMode(RoomDatabase.JournalMode.TRUNCATE)` bauen → `.db` ist nach jeder Transaktion selbstkonsistent (idealerweise trotzdem nach Checkpoint/in Ruhephase sichern). Tradeoff: geringere Schreib-Nebenlaeufigkeit — fuer eine zentrale Journal-/Quiz-DB meist akzeptabel und deutlich einfacher.
- **Quelle:** https://androidexplained.github.io/android/room/2020/10/03/room-backup-restore.html

### B3. Checkpoint + Lock sind nicht atomar — ein Schreibvorgang rutscht dazwischen
- **Symptom:** Selten inkonsistentes Backup trotz vorangegangenem Checkpoint.
- **Ursache:** `PRAGMA wal_checkpoint(TRUNCATE)` und das anschliessende Sperren/Kopieren sind zwei getrennte Statements; SQLite garantiert keine Atomaritaet dazwischen (vom SQLite-Maintainer bestaetigt).
- **Versionen:** per Design (SQLite), versionsunabhaengig.
- **FIX (funktionserhaltend):** Entweder alle drei Dateien als Set kopieren, ODER (robuster) vor dem Backup `RoomDatabase.close()` (impliziter Checkpoint, keine offene Verbindung), ODER waehrend des gesamten Backups jeden DB-Schreibzugriff unterbinden.
- **Quelle:** https://sqlite.org/forum/forumpost/2ea989bbe9

### B4. Datei kopieren/ueberschreiben bei offener Verbindung → Korruption („database disk image is malformed")
- **Symptom:** `SQLiteDatabaseCorruptException`, „database disk image is malformed" / „SQLite Error 11" beim Oeffnen der wiederhergestellten DB.
- **Ursache:** Eine DB-Datei darf nicht mit Fremdwerkzeugen kopiert/ueberschrieben werden, solange eine SQLite-Verbindung offen ist und schreibt (halb-geschriebene Seiten; uebrig gebliebenes `-wal` korrumpiert eine neu eingespielte `.db`). POSIX/Android-Falle: das Schliessen *irgendeines* FDs auf die Datei gibt **alle** Prozess-Locks frei.
- **Versionen:** per Design (SQLite/OS), versionsunabhaengig.
- **FIX (funktionserhaltend):** **Backup:** Checkpoint (B1) + Schreibsperre, oder `close()` vor dem Copy. **Restore:** `RoomDatabase.close()` → alte `-wal`/`-shm` loeschen → Backup-`.db` einspielen → Room neu instanziieren (`databaseBuilder` neu) → `PRAGMA integrity_check`. App beim Restore moeglichst neu starten bzw. DB-Instanz frisch bauen.
- **Quelle:** https://sqlite.org/forum/forumpost/2ea989bbe9 · https://learn.microsoft.com/en-us/microsoft-cloud/dev/dev-proxy/how-to/sqlite-error-11-database-disk-image-is-malformed

### B5. Restore: identityHash-/Schema-Mismatch beim Einspielen einer Fremd-DB
- **Symptom:** `Room cannot verify the data integrity … identity hash does not match` bzw. `Pre-packaged database has an invalid schema`; oder die DB wird beim ersten Oeffnen destruktiv neu angelegt (Datenverlust).
- **Ursache:** Eingespielte DB (per `createFromFile`/`createFromAsset` oder Restore) hat ein Schema/`user_version`, das nicht exakt zur im Code annotierten `@Database(version=...)` passt → Integritaetspruefung schlaegt fehl; bei Prepackaged-DBs ist der Default-Fallback destruktiv.
- **Versionen:** per Design, alle Versionen.
- **FIX (funktionserhaltend):** Backup-DB nur aus exakt derselben Schema-Version zurueckspielen (`user_version`/identityHash muessen passen). Beim Restore die Datei direkt an die richtige Stelle kopieren (nicht ueber `createFromFile` mit abweichendem Schema). Fuer aeltere Backups saubere `addMigrations(...)` bereitstellen; **kein** `fallbackToDestructiveMigration()` im Restore-Pfad. Schema-JSONs einchecken (M5), damit Migrationen testbar sind.
- **Quelle:** https://developer.android.com/training/data-storage/room/prepopulate · https://issuetracker.google.com/issues/134610941

---

## T) Threading · Coroutines · Flow · suspend-DAO

### T1. „Cannot access database on the main thread" — `allowMainThreadQueries()` ist der Anti-Fix ⭐ HAEUFIG
- **Symptom:** `IllegalStateException: Cannot access database on the main thread since it may potentially lock the UI for a long period of time.`
- **Ursache:** Synchroner (nicht-`suspend`, nicht-`Flow`/`LiveData`) DAO-Aufruf im UI-Thread. Room blockt das absichtlich (sonst ANR).
- **Versionen:** per Design seit 1.0.
- **FIX (funktionserhaltend):** DAO **asynchron** machen: `suspend fun` (One-Shot), `Flow<T>` (observable), alternativ `LiveData`/RxJava/`ListenableFuture`. Aus `viewModelScope`/IO aufrufen. `allowMainThreadQueries()` entfernt nur das Sicherheitsnetz und holt die ANRs zurueck — nur Debug/Test, **nie** Produktion.
- **Quelle:** https://developer.android.com/training/data-storage/room/async-queries

### T2. `withContext(Dispatchers.IO)` um suspend-DAO ist ueberfluessig (Anti-Pattern)
- **Symptom:** Entwickler wickeln `suspend`-DAO-Calls in `withContext(Dispatchers.IO) { … }` aus Sorge, der Main-Thread werde blockiert.
- **Ursache:** Room-generierte `suspend`-Funktionen sind **main-safe** — Room schaltet intern (`CoroutinesRoom.execute`) auf seinen Query-Executor. Das vorgeschaltete `withContext` ist redundant und kann kontraproduktiv sein (falscher Pool, schwerer testbar).
- **Versionen:** seit 2.1 (Coroutines-Support).
- **FIX (funktionserhaltend):** `suspend`-DAO direkt aus jedem Scope (auch Main) aufrufen; DB-Thread-Pool ueber `RoomDatabase.Builder.setQueryExecutor(...)` konfigurieren. **Ausnahme:** manuelles `db.runInTransaction { … }` mit **blockierenden** DAOs braucht weiter einen eigenen Hintergrund-Dispatcher.
- **Quelle:** https://medium.com/androiddevelopers/threading-models-in-coroutines-and-android-sqlite-api-6cab11f7eb90

### T3. `withTransaction{}` / `@Transaction` + suspend: Dispatcher-Wechsel im Block → Deadlock
- **Symptom:** App/Test haengt bei `db.withTransaction { … }`, wenn darin ein DB-Call auf einem anderen Thread/Dispatcher landet.
- **Ursache:** SQLite-Transaktionen sind **thread-confined** (`begin`/`endTransaction` an einen Thread gebunden). Coroutines sind das nicht — nach einem Suspension-Point kann ein anderer Thread fortsetzen. Room routet die DB-Operationen ueber einen speziellen Single-Thread-Dispatcher + `TransactionElement` + `ThreadContextElement` auf den Transaktions-Thread. Legt man im Block selbst `withContext(otherDispatcher)`/`launch` um einen DB-Call, verlaesst man diesen Thread → Falle schlaegt zu.
- **Versionen:** Mechanik seit 2.1.
- **FIX (funktionserhaltend):** Alle DB-Operationen einer Transaktion im `withTransaction`-Block belassen, **ohne** eigenen Dispatcher-Wechsel. Nicht-DB-Arbeit darf den Dispatcher wechseln. (Verwandte Deadlock-Fixes siehe T10.)
- **Quelle:** https://medium.com/androiddevelopers/threading-models-in-coroutines-and-android-sqlite-api-6cab11f7eb90

### T4. `withTransaction` in Tests: Single-Thread-Dispatcher → Deadlock
- **Symptom:** Ein Test mit suspend-Transaktion haengt fuer immer (Timeout).
- **Ursache:** Fuer eine Transaktion muss Room einen Thread aus dem Dispatcher „uebernehmen". Bei einem Single-Thread-/eingeschraenkten Test-Dispatcher (naives `runBlockingTest`/`StandardTestDispatcher`) bleibt kein zweiter Thread fuer die DAO-Calls innerhalb der Transaktion.
- **Versionen:** verhaltensbedingt, seit 2.1.
- **FIX (funktionserhaltend):** Im Test einen echten Multithread-Executor fuer die DB nutzen (`setQueryExecutor`/`setTransactionExecutor` auf realen Pool), echte In-Memory-DB + `runBlocking` statt Test-Single-Thread-Dispatcher fuer DB-Arbeit.
- **Quelle:** https://medium.com/@domplebump/testing-androidx-room-with-coroutines-deadlocks-when-using-transactions-fd214b46a204

### T5. Verschachteltes `withTransaction` / fremde API mit eigener Transaktion → Deadlock
- **Symptom:** Aufruf einer fremden suspend-API (die intern selbst eine Room-Transaktion oeffnet) innerhalb eines `withTransaction`-Blocks → Deadlock (z. B. Store-Library).
- **Ursache:** Der innere Aufruf laeuft evtl. nicht auf dem Transaktions-Thread des aeusseren Blocks; der haelt den (einzigen) Thread, der innere wartet → Deadlock.
- **Versionen:** verhaltensbedingt, alle Versionen.
- **FIX (funktionserhaltend):** In einem `withTransaction`-Block keine fremden APIs aufrufen, die eine **eigene** Transaktion oeffnen. Echtes Room-Nesting (re-entrant gleiches `withTransaction`/`runInTransaction`) ist ok (wird per `TransactionElement` gezaehlt). Externe DB-Arbeit aus dem Block herausziehen.
- **Quelle:** https://github.com/MobileNativeFoundation/Store/issues/453

### T6. `Flow`-DAO emittiert bei JEDER Tabellen-Aenderung neu → `distinctUntilChanged()` Pflicht ⭐ HAEUFIG
- **Symptom:** Ein `Flow`-Query feuert erneut mit identischem Ergebnis, obwohl nur eine andere, irrelevante Zeile derselben Tabelle geaendert wurde → UI flackert / Endlos-Recompositions in Compose.
- **Ursache:** Der InvalidationTracker arbeitet auf **Tabellen-Ebene** (SQLite-Trigger koennen nur tabellenweit benachrichtigen). Jede Aenderung der referenzierten Tabelle re-triggert ALLE Flows darauf.
- **Versionen:** per Design, alle Versionen.
- **FIX (funktionserhaltend):** DAO-Flow mit `.distinctUntilChanged()` umhuellen (reine Emission-Filterung, kein Datenverlust). Voraussetzung: sinnvolles `equals()` (data class). Gilt analog fuer RxJava/LiveData.
- **Quelle:** https://medium.com/androiddevelopers/room-flow-273acffe5b57

### T7. `Flow`-DAO ist cold → mehrere Collector = mehrere Queries
- **Symptom:** Mehrere Collector desselben DAO-Flows loesen mehrere parallele Re-Queries aus.
- **Ursache:** Der von Room erzeugte Flow ist cold; jeder Collector startet die Query separat + eigener InvalidationTracker-Observer.
- **Versionen:** per Design, alle Versionen.
- **FIX (funktionserhaltend):** Geteilte Daten via `stateIn(scope)`/`shareIn(scope)` im ViewModel cachen; ggf. `.conflate()` gegen Backpressure. Ein vorgeschaltetes `flowOn(IO)` fuer die DB ist unnoetig (Room ist main-safe) — `flowOn` nur fuer nachgelagerte teure Maps.
- **Quelle:** https://medium.com/androiddevelopers/room-flow-273acffe5b57

### T8. RxJava `Single` → `EmptyResultSetException` bei leerem Ergebnis
- **Symptom:** `androidx.room.EmptyResultSetException: Query returned empty result set` bei DAO-Rueckgabe `Single<T>`, wenn keine Zeile passt.
- **Ursache:** `Single<T>` garantiert genau einen Wert; bei 0 Treffern ruft Room `onError`. Per Design.
- **Versionen:** alle (RxJava2 ab 2.1, RxJava3 ab 2.3).
- **FIX (funktionserhaltend):** Wenn „kein Treffer" normal ist: Rueckgabetyp `Maybe<T>` (→ `onComplete` ohne Item). Wenn `Single` bleiben muss: `EmptyResultSetException` abfangen (`onErrorReturn`) und als „leer" interpretieren.
- **Quelle:** https://developer.android.com/training/data-storage/room/async-queries

### T9. PagingSource (Room + Paging 3): Snapshot-Invalidierung & `getRefreshKey`
- **Symptom:** Liste springt nach DB-Aenderung an den Anfang; oder eigene PagingSource aktualisiert nicht.
- **Ursache:** Jede PagingSource ist ein Snapshot; bei DB-Aenderung „invalid" → neue Generation. Rooms **generierte** PagingSource invalidiert automatisch (InvalidationTracker); eigene PagingSources muessen selbst `invalidate()`. `getRefreshKey()` muss um `anchorPosition` rechnen, sonst Sprung an den Anfang.
- **Versionen:** Verhalten per Design. (room-paging-Bug bei `@Relation` + PagingSource gefixt in 2.7.0-alpha11 → in 2.7.0 enthalten.)
- **FIX (funktionserhaltend):** Rooms generierte PagingSource nutzen statt selbst invalidieren; `getRefreshKey` korrekt mit `anchorPosition` implementieren.
- **Quelle:** https://developer.android.com/topic/libraries/architecture/paging/v3-paged-data

### T10. Deadlocks mit dem neuen `AndroidSQLiteDriver` (suspending Transactions / Auto-Close + Flow)
- **Symptom:** Gelegentlicher Deadlock bei suspending Transactions; oder Deadlock, wenn eine auto-geschlossene DB aus einer Flow-Emission wieder geoeffnet wird.
- **Ursache:** Driver-/Connection-Pfad-Bugs.
- **Versionen:** suspending-Transaction-Deadlock **gefixt ab 2.8.0** (b/415006268, in 2.8.0-alpha01); Auto-Close+Flow-Deadlock **gefixt ab 2.8.2** (b/446643789). In **2.7.0 noch vorhanden**.
- **FIX (funktionserhaltend):** Wenn ihr den neuen `AndroidSQLiteDriver` UND suspending Transactions / `setAutoCloseTimeout` nutzt: Upgrade auf **≥ 2.8.2** einplanen. Wer (noch) keinen eigenen Driver setzt, ist von T10 nicht betroffen.
- **Quelle:** https://issuetracker.google.com/issues/415006268 · https://issuetracker.google.com/446643789

### T11. Sync-Schleife mit Einzel-Writes → N Flow-Invalidationen → UI-Recompose-Sturm (Scroll-Jank) ⭐ EIGENER VORFALL 2026-07-04
- **Symptom:** UI ruckelt/flackert waehrend eines Hintergrund-Syncs merklich — besonders beim gleichzeitigen Scrollen direkt nach dem Screen-Oeffnen. Kein Fehler, keine Exception; die App wirkt nur „zaeh", Werte erscheinen troepfelnd.
- **Ursache:** Ein Import/Sync/Backfill schreibt in einer Schleife PRO Element einzeln (`upsert`/`UPDATE`), teils noch mit `delay()` gestreckt. **Jeder einzelne Write invalidiert die tabellenweiten Room-Flows** (T6) → jede Invalidation emittiert einen neuen UI-State → bei unstable State-Objekten (Compose Strong-Skipping `===`-Falle) recomposen ALLE sichtbaren Karten pro Write auf dem Main-Thread. 26 Einzel-Writes = 26 komplette UI-Recompositions, waehrend der Benutzer scrollt. EntropieReductor-Vorfall (#47476): HC-Trainings-Merge (N upserts) + Open-Meteo-Wetter-Backfill (26 UPDATEs mit `delay(150)` dazwischen) liefen genau beim Biomarker-Tab-Oeffnen.
- **Versionen:** per Design (tabellenweite Invalidation), alle Room-Versionen.
- **FIX (funktionserhaltend):** ALLE Writes der Schleife in **EINE** `appDatabase.withTransaction { }` batchen → genau 1 Invalidation am Transaktionsende statt N. Netzwerk-/Suspend-Fremdarbeit (API-Fetches, `delay`) gehoert VOR die Transaktion (T3: kein Dispatcher-Wechsel/keine Fremd-Suspension im Block!): erst Ergebnisse in eine Liste sammeln, dann atomar schreiben. Werte/Verhalten identisch — sie erscheinen atomar statt troepfelnd. Ergaenzend `.distinctUntilChanged()` auf den DAO-Flows (T6) gegen inhaltsgleiche Emissionen.
- **Quelle:** eigener Vorfall EntropieReductor #47476 · https://developer.android.com/training/data-storage/room/async-queries (Flow-Invalidation) · T3/T6 in dieser Datei

---

## R) @Relation

### R1. `@Relation` ohne `@Transaction` → inkonsistente Eltern/Kinder
- **Symptom:** Eltern- und Kinddaten passen nicht zusammen (z. B. parallel geaenderte/geloeschte Kinder).
- **Ursache:** Eine `@Relation`-Abfrage loest intern **mehrere** SQL-Queries aus (Eltern + je Relation). Ohne `@Transaction` laufen sie nicht atomar — zwischen den Queries kann geschrieben werden.
- **Versionen:** per Design, alle Versionen (Compiler **warnt**).
- **FIX (funktionserhaltend):** Jede DAO-Methode mit `@Relation`-Rueckgabetyp zusaetzlich mit `@Transaction` annotieren (gilt fuer suspend/Flow/LiveData).
- **Quelle:** https://developer.android.com/training/data-storage/room/relationships/many-to-many

### R2. `@Relation` laedt IMMER ALLE Kinder (kein WHERE/LIMIT/ORDER) → N+1 & Speicherlast
- **Symptom:** Bei grossen Relationen wird alles in den RAM geladen; UI ruckelt, OOM moeglich.
- **Ursache:** `@Relation` generiert pro Eltern-Batch `SELECT * FROM child WHERE childKey IN (...)` — kein Filtern/Sortieren/Limitieren ueber die Annotation. Klassisches N+1.
- **Versionen:** per Design, alle Versionen.
- **FIX (funktionserhaltend):** Statt `@Relation` eine eigene `@Query` mit JOIN + WHERE/LIMIT/ORDER schreiben (offiziell: Multimap-Return oder manuelles JOIN). Fuer reine Anzeige paginieren (Paging 3). Nur Teilfelder → schlankes Projektions-POJO statt voller Entity.
- **Quelle:** https://developer.android.com/training/data-storage/room/referencing-data

### R3. Verschachtelte `@Relation` multipliziert die Query-Zahl
- **Symptom:** Drei-Ebenen-Modell (Quiz → Kategorien → Fragen → Antworten) wird sehr langsam.
- **Ursache:** Jede `@Relation`-Ebene erzeugt eine eigene Query-Runde; bei N Eltern summieren sich die Queries.
- **Versionen:** per Design.
- **FIX (funktionserhaltend):** Verschachtelung flach halten; tiefe/grosse Hierarchien als dedizierte JOIN-Query mit Projektions-POJO laden. Immer `@Transaction` (R1).
- **Quelle:** https://developer.android.com/training/data-storage/room/relationships/nested

### R4. Many-to-Many nur korrekt mit `@Junction`
- **Symptom:** „Cannot find a relationship" / leere oder falsche Listen bei N:M.
- **Ursache:** Bei N:M gibt es keine direkte FK-Referenz; ohne Cross-Reference-Tabelle + `associateBy = Junction(...)` kann Room die Beziehung nicht aufloesen.
- **Versionen:** per Design (Junction seit 2.2).
- **FIX (funktionserhaltend):** Dritte Entity als Cross-Ref-Tabelle mit Composite-PK aus beiden FK-Spalten; in beiden `@Relation` `associateBy = Junction(CrossRef::class)`.
- **Quelle:** https://developer.android.com/training/data-storage/room/relationships/many-to-many

### R5. `@Relation`-Listen sind nie `null` (leere Liste) — Kotlin-Codegen-Nullability
- **Symptom:** Erwartung von `null` bei „keine Kinder" schlaegt fehl; mit Kotlin-Codegen ggf. Nullability-Compile-Fehler.
- **Ursache:** Room fuellt Collection-Relationen immer (leere Liste). Ab 2.6/2.7 (Kotlin-Codegen) sind nullable Collection-Rueckgaben verboten (V1).
- **Versionen:** leere-Liste-Verhalten per Design; Codegen-Striktheit ab 2.6.0, Default ab 2.7.0.
- **FIX (funktionserhaltend):** Code auf „leere Liste = keine Kinder" auslegen; Rueckgabetypen non-null (`List<Song>` statt `List<Song>?`).
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room (2.6.0/2.7.0)

---

## C) TypeConverter & @Embedded

### C1. „Cannot figure out how to save this field into database"
- **Symptom:** Compile-Fehler bei nicht-primitivem Feld (Date, Instant, List, eigene Klasse).
- **Ursache:** Kein passender `@TypeConverter` im Scope. Room persistiert nur Primitive + `String` direkt.
- **Versionen:** per Design.
- **FIX (funktionserhaltend):** Converter-Paar (hin/zurueck zu Long/String) schreiben und via `@TypeConverters` registrieren. `Date` ↔ `Long` (Timestamp), nicht als Locale-abhaengiger String.
- **Quelle:** https://developer.android.com/training/data-storage/room/referencing-data

### C2. `@TypeConverters`-Scope-Falle (falsch platziert)
- **Symptom:** Converter wird in manchen DAOs/Entities „nicht gefunden", obwohl er existiert.
- **Ursache:** `@TypeConverters` wirkt nur im annotierten Scope: an `@Database` global, an `@Entity`/`@Dao`/Methode/Feld nur lokal.
- **Versionen:** per Design.
- **FIX (funktionserhaltend):** Fuer app-weit genutzte Typen `@TypeConverters` an die **`@Database`**-Klasse haengen.
- **Quelle:** https://developer.android.com/training/data-storage/room/referencing-data

### C3. Null-Handling im Converter
- **Symptom:** NPE oder falsche Werte bei `null`-Spalten.
- **Ursache:** Converter bekommt/liefert `null`, behandelt es aber nicht.
- **Versionen:** per Design.
- **FIX (funktionserhaltend):** Nullable-Signaturen + `null` durchreichen: `fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }` und Rueckrichtung analog (`date?.time`).
- **Quelle:** https://developer.android.com/training/data-storage/room/referencing-data

### C4. Enums: Default-Converter ab 2.3 speichert den NAMEN → Umbenennen bricht alte Backups
- **Symptom:** Doppelte/unnoetige Enum-Converter; oder nach Umbenennen eines Enum-Konstanten lassen sich alte Daten/Backups nicht mehr lesen.
- **Ursache:** Room 2.3+ hat einen Default-Enum-Converter (eigene Converter haben Vorrang); er speichert den **Namen** als String.
- **Versionen:** Default-Enum-Converter ab 2.3.0.
- **FIX (funktionserhaltend):** Eigenen Converter nur bei abweichendem Mapping. **Fuer Drive-Backup-Stabilitaet:** stabilen Integer-/Code-Converter statt Name verwenden, damit Enum-Refactorings alte Backups nicht brechen.
- **Quelle:** https://developer.android.com/training/data-storage/room/referencing-data

### C5. `@ProvidedTypeConverter` mit Konstruktor-Injection (+ 2.7.0-Crash)
- **Symptom:** Converter braucht eine Abhaengigkeit (Moshi/Gson), Room instanziiert ihn aber per Default-Konstruktor → geht nicht. In 2.7.0 zusaetzlich Build-Crash `IndexOutOfBoundsException` bei der Validierung.
- **Ursache:** Room instanziiert Converter normalerweise selbst. Plus Compiler-Bug in 2.7.0.
- **Versionen:** `@ProvidedTypeConverter` seit 2.1; Crash in 2.7.0 **gefixt ab 2.7.1** (b/409804755).
- **FIX (funktionserhaltend):** Converter mit `@ProvidedTypeConverter` annotieren, in `@TypeConverters` deklarieren und die fertige Instanz via `RoomDatabase.Builder.addTypeConverter(instance)` uebergeben. Bei 2.7.0 + ProvidedTypeConverter auf **≥ 2.7.1** gehen.
- **Quelle:** https://developer.android.com/training/data-storage/room/referencing-data · https://issuetracker.google.com/409804755

### C6. `@Embedded`: Praefix-Kollisionen bei doppelten Spaltennamen
- **Symptom:** „conflicting column names", wenn zwei `@Embedded`-Felder desselben Typs dieselben Spalten erzeugen.
- **Ursache:** `@Embedded` legt Unterfelder flach in derselben Tabelle ab; ohne Unterscheidung kollidieren `street`/`city` doppelt. Gilt auch fuer verschachteltes `@Embedded`.
- **Versionen:** per Design.
- **FIX (funktionserhaltend):** `@Embedded(prefix = "...")` auf jeder Ebene mit eindeutigem Praefix; Spaltennamen vorab planen.
- **Quelle:** https://developer.android.com/training/data-storage/room/relationships

---

## K) Keys · Insert/Update/Upsert · ForeignKey

### K1. `OnConflictStrategy.REPLACE` = DELETE+INSERT → CASCADE loescht Kinder + neue rowId ⭐ HAEUFIG (kritisch)
- **Symptom:** Nach `@Insert(onConflict = REPLACE)` auf einen Parent sind ploetzlich alle zugehoerigen Kind-Datensaetze weg. Kein Fehler. (z. B. „Quiz-Set neu speichern" loescht alle Fragen; „Tag aktualisieren" loescht Eintraege.)
- **Ursache:** SQLite `REPLACE` loest den Konflikt durch **Loeschen** der alten Zeile + neues Insert. Haengt an einer FK ein `onDelete = CASCADE`, feuert der Cascade und nimmt die Kinder mit. Bei `autoGenerate` bekommt die „ersetzte" Zeile zudem eine **neue** ID → Referenzen zeigen ins Leere.
- **Versionen:** SQLite-Verhalten per Design, alle Versionen.
- **FIX (funktionserhaltend):** Statt `@Insert(onConflict = REPLACE)` die `@Upsert`-Annotation (Room 2.5+) — echtes UPDATE bei Konflikt, kein Delete, kein Cascade. Alternativ IGNORE-Insert + bei Rueckgabe `-1` gezieltes `@Update`.
- **Eigener Vorfall (EntropieReductor, 2026-07-03, #47442):** Besonders heimtueckisch im **Multi-Device-Sync**: Ein LWW-Merge mit Tombstones verwaltete Kind-Datensaetze (Followups, Tool-Permissions) explizit — aber der Eltern-Update lief ueber `@Insert(REPLACE)` und CASCADE loeschte bei JEDEM Sync-Update die lokalen Kinder, die der Merge gerade erhalten wollte (lokal neue, noch nicht hochgeladene Kinder gingen unwiederbringlich verloren). Erkennungsregel: Vor jedem `onConflict = REPLACE` pruefen, ob eine FK mit `onDelete = CASCADE` auf DIESE Tabelle zeigt — dann `@Upsert`.

### K2. `@Upsert` (ab Room 2.5) statt REPLACE — UNIQUE-Sonderfall
- **Symptom:** `@Upsert` unbekannt in alten Versionen; oder Verhalten bei UNIQUE-Konflikt (nicht-PK) unklar.
- **Ursache:** `@Upsert` (insert-or-update auf PK-Konflikt) ist neu (b/241964353), min-API 16 (b/243039555). Frueher Fehlverhalten bei `2067 SQLITE_CONSTRAINT_UNIQUE` → in 2.6.0-beta01 als korrektes Update gefixt.
- **Versionen:** `@Upsert` ab **2.5.0** (in 2.7 nutzbar); UNIQUE-Fix ab 2.6.0-beta01.
- **FIX (funktionserhaltend):** `@Upsert` fuer „insert-or-update auf PK" nutzen; bei UNIQUE-Constraints (nicht-PK) Verhalten testen, da Upsert primaer auf PK-Konflikt zielt.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room

### K3. Insert-Rueckgabe ist die rowId — `-1` bei IGNORE-Konflikt
- **Symptom:** Code verlaesst sich auf die zurueckgegebene ID, bekommt aber `-1`.
- **Ursache:** `@Insert : Long`/`LongArray` gibt die rowId(s) zurueck. Bei `OnConflictStrategy.IGNORE` + Konflikt wird nichts eingefuegt → `-1` an der Position.
- **Versionen:** per Design.
- **FIX (funktionserhaltend):** Rueckgabewert pruefen; `-1` als „existiert schon / ignoriert" behandeln und ggf. separat `@Update` fahren.
- **Quelle:** https://developer.android.com/training/data-storage/room/accessing-data

### K4. `autoGenerate = true` mit `Long`: Wert `0` = „auto", jeder andere Wert wird uebernommen
- **Symptom:** Insert ueberschreibt nicht wie erwartet / vergibt unerwartet neue IDs; beim Restore stimmen IDs nicht.
- **Ursache:** Bei `@PrimaryKey(autoGenerate = true)` auf `Long` behandelt Room/SQLite `0` als „bitte ID vergeben"; ein expliziter Nicht-0-Wert wird als gewuenschte ID genommen.
- **Versionen:** Room/SQLite-Standardverhalten.
- **FIX (funktionserhaltend):** Neue Datensaetze mit `id = 0` (Default) → Room vergibt ID; bestehende mit echter ID. **Fuer Drive-Restore:** feste IDs explizit setzen, `autoGenerate` nicht blind arbeiten lassen.
- **Quelle:** https://developer.android.com/training/data-storage/room/defining-data

### K5. `autoGenerate` auf `Long`, nicht `Int`
- **Symptom:** Ueberlauf-Risiko/Inkonsistenz bei sehr vielen Inserts.
- **Ursache:** SQLite rowid ist 64-bit; ein `Int`-PK bildet den vollen Bereich nicht ab.
- **Versionen:** Best-Practice-Ableitung (SQLite-Standard).
- **FIX (funktionserhaltend):** Auto-generierte PK-Felder als `Long` deklarieren.
- **Quelle:** https://developer.android.com/training/data-storage/room/defining-data

### K6. Composite Primary Key via `primaryKeys = [...]`
- **Symptom:** Unklar, wie man einen Mehrspalten-PK definiert (typisch fuer Cross-Ref-Tabellen).
- **Ursache/FIX (funktionserhaltend):** `@Entity(primaryKeys = ["firstName","lastName"])` statt `@PrimaryKey` an einem Feld; fuer N:M-Cross-Ref `primaryKeys = ["playlistId","songId"]`.
- **Versionen:** per Design.
- **Quelle:** https://developer.android.com/training/data-storage/room/defining-data

### K7. ForeignKey-Kindspalte ohne Index → Warnung + Full-Table-Scans
- **Symptom:** Compiler-Warnung „column references a foreign key but it is not part of an index"; CASCADE-Deletes/JOINs werden langsam.
- **Ursache:** SQLite indiziert FK-Kindspalten nicht automatisch; ohne Index → Full-Table-Scan bei jeder FK-Pruefung/CASCADE.
- **Versionen:** per Design (Warnung in allen Versionen).
- **FIX (funktionserhaltend):** Index auf der FK-Kindspalte: `@Entity(foreignKeys=[...], indices=[Index("parentId")])`.
- **Quelle:** https://developer.android.com/training/data-storage/room/defining-data

### K8. ForeignKey-Constraint beim Insert (Reihenfolge Eltern→Kinder, besonders beim Restore)
- **Symptom:** `SQLiteConstraintException: FOREIGN KEY constraint failed` beim Einfuegen eines Kindes.
- **Ursache:** Das referenzierte Eltern-Element existiert (noch) nicht — besonders beim Drive-Restore in falscher Reihenfolge.
- **Versionen:** per Design.
- **FIX (funktionserhaltend):** Erst Eltern, dann Kinder einfuegen; Restore-Reihenfolge garantieren und alles in eine `@Transaction` packen.
- **Quelle:** https://developer.android.com/training/data-storage/room/defining-data

### K9. Partielle Updates ohne ganze Entity
- **Symptom:** `@Update` mit voller Entity ueberschreibt ungewollt Felder oder verlangt das komplette Objekt.
- **Ursache:** `@Update` aktualisiert per PK alle Spalten der uebergebenen Entity.
- **Versionen:** per Design.
- **FIX (funktionserhaltend):** Teil-Entity/Partial-POJO mit denselben PK-Feldern + nur den zu aendernden Feldern fuer `@Update`, ODER gezieltes `@Query("UPDATE … SET col=:v WHERE id=:id")`.
- **Quelle:** https://developer.android.com/training/data-storage/room/accessing-data

---

## X) Connection-Pool · Locking · Multi-Prozess

### X1. „database is locked" / `SQLiteDatabaseLockedException` durch mehrere DB-Instanzen ⭐ HAEUFIG
- **Symptom:** Sporadisch `SQLiteDatabaseLockedException: database is locked (code 5 SQLITE_BUSY)`, v. a. bei parallelen Schreibzugriffen.
- **Ursache:** Mehr als eine `RoomDatabase`-Instanz gebaut (kein Singleton) → mehrere Connections konkurrieren; SQLite erlaubt nur **einen** Writer. Oft durch fehlendes `@Singleton`/Scoping oder mehrfaches `Room.databaseBuilder(...)`.
- **Versionen:** per Design / Anwendungsfehler.
- **FIX (funktionserhaltend):** Genau EINE `RoomDatabase`-Instanz prozessweit (Singleton via Hilt `@Singleton` oder object-Holder mit Double-Check). Schreibzugriffe ueber `withTransaction`/`@Transaction` serialisieren. WAL (Default) mildert Reader-vs-Writer, ersetzt aber nicht den Singleton.
- **Quelle:** https://dev.to/jgutierrezgil/understanding-the-singleton-pattern-in-android-development-a-room-database-case-study-2foi · https://issuetracker.google.com/issues/280124659

### X2. „Timed out attempting to acquire a reader connection"
- **Symptom:** `SQLException: Error code: 5, Timed out attempting to acquire a reader connection` bzw. `database is locked` unter Last/Parallelzugriff.
- **Ursache:** Connection-Pool + fehlendes `busy_timeout`.
- **Versionen:** **gefixt in 2.7.0** (Pool reimplementiert + busy_timeout, b/380088809, rc01/rc03 — in eurer 2.7.0 enthalten). Weitere Verbesserung in **2.7.2**: Room wirft bei zu langem Connection-Erwerb keine Exception mehr, sondern loggt nur (b/422448815).
- **FIX (funktionserhaltend):** Auf 2.7.0+ ist der Hauptfix da; bei weiterhin auftretenden Timeouts auf **2.7.2** gehen, Pool-Groesse pruefen, ggf. `BundledSQLiteDriver`.
- **Quelle:** https://issuetracker.google.com/380088809 · https://issuetracker.google.com/422448815

### X3. Multi-Prozess: Invalidation greift nicht / `no such table: room_table_modification_log`
- **Symptom:** Aenderungen in einem Prozess erscheinen nicht in `Flow`/`LiveData` eines anderen Prozesses; oder `SQLiteException: no such table: room_table_modification_log`.
- **Ursache:** Standard-Invalidation ist prozessintern. Bei mehreren Prozessen (z. B. separater Sync-/Backup-Service mit `android:process=`) ist der TEMP-`room_table_modification_log` der Primaerverbindung unter WAL fuer andere nicht sichtbar.
- **Versionen:** per Design; `enableMultiInstanceInvalidation()` seit 2.2.
- **FIX (funktionserhaltend):** `.enableMultiInstanceInvalidation()` im Builder. Einfacher: Backup/Restore im selben Prozess wie die DB durchfuehren.
- **Quelle:** https://commonsware.com/Room/pages/chap-processes-003.html · https://issuetracker.google.com/issues/67757002

---

## BLD) KSP / Build / Schema-Export

> Allgemeine Gradle/AGP/KSP-Plugin-Themen → [`../android-build/gradle.md`](../android-build/gradle.md). Hier nur Room-spezifisch.

### BLD1. „Schema export directory is not provided" / Schemas nicht eingecheckt (Multi-Modul)
- **Symptom:** Build-Warnung pro Modul; Migrationen nicht testbar; in Multi-Modul-Setups fehlen/verirren sich Schemas.
- **Ursache:** `exportSchema` ist Default `true`, aber kein Verzeichnis konfiguriert; JSONs vergessen einzuchecken.
- **Versionen:** per Design; Auto-Test-Ressourcen ab 2.7.0-alpha07 (Room-Gradle-Plugin fuegt Schemas automatisch als Instrumentation-Test-Ressourcen hinzu).
- **FIX (funktionserhaltend):** Pro DB-Modul `room { schemaDirectory("$projectDir/schemas") }`; `schemas/<DB>/<version>.json` in Git einchecken.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room (2.7.0-alpha07)

### BLD2. `KSP apoption does not match \S+=\S+: room.schemaLocation` bei Leerzeichen im Pfad ⚠️ (macOS-Falle)
- **Symptom:** Build bricht ab mit `KSP apoption does not match \S+=\S+: room.schemaLocation`. `exportSchema = false` behebt es **nicht**.
- **Ursache:** KSP parst Processor-Argumente mit Regex `\S+=\S+`. Enthaelt der Projekt-/Schema-Pfad ein **Leerzeichen** (typisch macOS: `…/Application Support/…` oder User-Ordner mit Leerzeichen), schlaegt das Matching fehl.
- **Versionen:** KSP-Bug; tritt mit Room + KSP auf, sobald Leerzeichen im Pfad. Nicht prinzipiell in 2.7 gefixt — Pfad-Hygiene noetig.
- **FIX (funktionserhaltend):** Schema-/Projektpfad **ohne Leerzeichen** waehlen (`schemaDirectory("$projectDir/schemas")` wenn `$projectDir` leerzeichenfrei). Bei Leerzeichen im uebergeordneten Pfad das Projekt in einen leerzeichenfreien Pfad verschieben oder das Room-Gradle-Plugin nutzen (umgeht das rohe arg-Parsing teilweise).
- **Quelle:** https://github.com/android/nowinandroid/issues/604 · https://github.com/google/ksp/issues/2045

### BLD3. `room.schemaLocation` als KSP-Argument, nicht als kapt-Argument
- **Symptom:** „Schema export directory is not provided…" trotz gesetztem Pfad.
- **Ursache:** Bei KSP wird das Argument ueber `ksp { arg(...) }` bzw. das Room-Gradle-Plugin gesetzt; ein im `kapt`/`annotationProcessorOptions`-Block gesetzter Pfad wird von KSP ignoriert.
- **Versionen:** per Design; Room-Gradle-Plugin ab 2.6.0.
- **FIX (funktionserhaltend):** Room-Gradle-Plugin `room { schemaDirectory(...) }` (empfohlen) ODER `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` + `exportSchema = true`.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room

### BLD4. Room-Gradle-Plugin scheitert bei leerem Schema-Verzeichnis
- **Symptom:** Configure des `androidx.room`-Plugins schlaegt fehl, wenn das Schema-Directory leer ist.
- **Ursache:** Plugin vertrug leeres Verzeichnis nicht.
- **Versionen:** bis 2.7.1, **gefixt ab 2.7.2** (b/417823384).
- **FIX (funktionserhaltend):** Auf 2.7.2 heben; Workaround vorher: erstes Schema-JSON committen, damit das Verzeichnis nicht leer ist.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room (2.7.2)

### BLD5. KMP: `@ConstructedBy` + `RoomDatabaseConstructor` ersetzen `instantiateImpl()`
- **Symptom:** (Migration aelterer KMP-Setups) alter Init-Code mit `instantiateImpl()` kompiliert nicht; „Expected object … has no actual declaration".
- **Ursache:** Kotlin-2.0-Kompilationsmodell — das fruehere Verfahren ist nicht mehr tragfaehig. Ersetzt durch neue APIs (2.7.0-alpha06). Echte alpha-Bugs (fehlender `actual`-Modifier) gefixt ab 2.7.0-alpha07; reine IDE-„no actual"-Meldung ist Tooling-Limit, Build laeuft.
- **Versionen:** neue API ab 2.7.0-alpha06, stabil in 2.7.0. (Nur relevant, falls ihr KMP nutzt — BestJournalAndroid ist reines Android.)
- **FIX (funktionserhaltend):** `expect object MyDbCtor : RoomDatabaseConstructor<MyDb>` + `@ConstructedBy(MyDbCtor::class)` an die `@Database`-Klasse; `Room.databaseBuilder<AppDatabase>(name = path)` ohne Factory-Argument. IDE-Fehler ggf. `@Suppress("NO_ACTUAL_FOR_EXPECT")`.
- **Quelle:** https://developer.android.com/jetpack/androidx/releases/room (2.7.0-alpha06/alpha07)

---

## ✅ Fix-Status (was ist schon gefixt? — hart gegen Release-Notes geprueft)

> **Methodik (Ehrlichkeit):** Versions-Timeline + alle `b/`-Issues wurden direkt aus den offiziellen
> Release-Notes (developer.android.com) verifiziert; ein `b/`-Eintrag unter „Bug Fixes" einer Version
> ist in genau dieser Version geschlossen. Issuetracker.google.com ist nicht ueber `gh` (GitHub) pruefbar,
> daher sind die Release-Notes die autoritative Quelle. **Im Zweifel gilt ein Bug als offen.**
>
> **Versions-Timeline (verifiziert):** 2.7.0 (09.04.2025) → 2.7.1 (23.04.2025) → 2.7.2 (18.06.2025, **letzte 2.7.x**)
> → 2.8.0 (10.09.2025) → 2.8.1 (24.09.2025) → 2.8.2 (08.10.2025) → 2.8.3 (22.10.2025) → 2.8.4 (19.11.2025, **hoechste 2.x**).
> **Es gibt KEIN 2.7.3–2.7.7.** Room 3.0 ist nur Alpha (ab 11.03.2026, kein stable).

| `b/`-Issue | Bug | gefixt ab | vs. installierte 2.7.0 |
|------------|-----|-----------|------------------------|
| b/409804755 | `IndexOutOfBoundsException` bei `@ProvidedTypeConverter`-Validierung (C5) | **2.7.1** | offen in 2.7.0 → Update |
| b/408364828 | `runInTransaction()` bricht mit konfiguriertem `SQLiteDriver` | **2.7.1** | offen in 2.7.0 → Update |
| b/413061402 | Fuehrender SQL-Kommentar → Query als Nicht-Read fehlinterpretiert | **2.7.2** | offen in 2.7.0 → Update |
| b/416549580 | Schema-Export faellt bei nativen KSP-Sources aus (M5) | **2.7.2** | offen in 2.7.0 → Update |
| b/417823384 | Room-Gradle-Plugin scheitert bei leerem Schema-Verzeichnis (BLD4) | **2.7.2** | offen in 2.7.0 → Update |
| b/422448815 | Connection-Timeout wird nur geloggt statt geworfen (X2) | **2.7.2** | Verbesserung > 2.7.0 |
| b/415006268 | Deadlock bei suspending Transactions (`AndroidSQLiteDriver`) (T10) | **2.8.0** | offen in 2.7.x → 2.8.x |
| b/432634197 | Pre-packaged DB destruktiv migriert trotz Migrationspfad → Datenverlust (M11) | **2.8.0** | offen in 2.7.x → 2.8.x |
| b/427095319 | Tabellen-/View-Namen bei destruktiver Migration nicht escaped (M12) | **2.8.0** | offen in 2.7.x → 2.8.x |
| b/442220723 | room-compiler-Crash bei DAO-Funktion mit suspend-Lambda | **2.8.1** | offen in 2.7.x → 2.8.x |
| b/446643789 | Deadlock beim Re-Open auto-closed DB aus Flow-Emission (T10) | **2.8.2** | offen in 2.7.x → 2.8.x |
| b/438041176 | `dropAllTables` bekommt wieder einen Default-Wert (M2) | **3.0.0-alpha02** | nur Alpha |

**Bereits in 2.7.0-Stable enthalten** (NICHT mehr jagen, siehe V6): b/380088809 (Connection-Pool/busy_timeout),
b/400584611 (InterruptedException), b/388299754 (value-class-Crash), b/340606803 (Writer-Connection-Invalidation
→ Flow), b/348227770 (FTS-Auto-Migration), b/352085724 (FK-Check-Reihenfolge), room-paging+@Relation (alpha11),
Views bei destruktiver Migration (b/381518941, alpha12).

**Noch NICHT gefixt in 2.7.0 (Workaround bleibt aktiv):** alle `@Design`-Eintraege (M1–M10, B1–B5, T1–T9, R1–R5,
C1–C6, K1–K9, X1/X3, BLD1–BLD3, BLD5) — das sind keine Bugs, sondern dauerhafte Mechanik/Regeln. Plus die
oben gelisteten `b/`-Fixes, die erst in 2.7.1/2.7.2/2.8.x kommen.

**Empfehlung (funktionserhaltend, kein Datenverlust):** Von 2.7.0 mindestens auf **2.7.2** patchen (drop-in,
kein API-Bruch) — behebt 6 reale Bugs. Fuer Migrations-/Flow-/Driver-Stabilitaet auf **2.8.4** (minSdk 23 +
AGP ≥ 8.4 sind bei euch erfuellt). Beim 2.8-Sprung Kotlin-Codegen-Striktheit (V1) im Blick behalten.

**Status unklar (ehrlich markiert):** Der exakte Versionspunkt, ab dem die parameterlose
`fallbackToDestructiveMigration()` deprecated und `dropAllTables: Boolean` zum Pflichtargument wurde, liess sich
NICHT aus den Release-Notes belegen (M2). „Ab 2.7" ist NICHT gestuetzt; vermutlich aelter (~2.4.0). Belegt ist
nur die Default-Wert-Rueckgabe in Room 3.0 (b/438041176).

---

## 📋 Pflicht-Checkliste (vor jedem DB-/Migrations-/Backup-Commit mental durchgehen)

- [ ] **Schema geaendert?** → `@Database(version=…)` erhoeht UND Migration (`Migration(N,M)` oder `@AutoMigration`) bereitgestellt? (M1, M3)
- [ ] **Niemals** `fallbackToDestructiveMigration(...)` in Produktion als „Fix"? (M2)
- [ ] **Schema-Export** aktiv (`room { schemaDirectory(...) }`) und JSONs in Git eingecheckt? Pfad ohne Leerzeichen? (M5, BLD1, BLD2)
- [ ] **Migration getestet** mit `MigrationTestHelper.runMigrationsAndValidate` fuer mehrere Startversionen? (M8)
- [ ] **Auto-Backup** mit `dataExtractionRules`/`fullBackupContent` bewusst gesteuert (Kollision mit Drive-Backup vermeiden)? (M4)
- [ ] **Backup/Restore:** Vor Datei-Copy `wal_checkpoint(TRUNCATE)` oder `close()`? Restore: `close()` → `-wal`/`-shm` weg → einspielen → Room neu → `integrity_check`? Nur gleiche Schema-Version? (B1, B4, B5)
- [ ] **DAO** `suspend`/`Flow`/`LiveData` (kein Main-Thread, kein `allowMainThreadQueries`)? Kein `withContext(IO)` um suspend-DAO? (T1, T2)
- [ ] **`Flow`-DAO** mit `.distinctUntilChanged()`? Geteilt via `stateIn`/`shareIn`? (T6, T7)
- [ ] **`@Relation`-Methode** mit `@Transaction`? Grosse Relationen via JOIN-Query statt `@Relation`? (R1, R2)
- [ ] **Kein `@Insert(onConflict = REPLACE)`** bei FK-CASCADE → `@Upsert` nutzen? (K1, K2)
- [ ] **Genau eine `RoomDatabase`-Instanz** prozessweit (Singleton)? (X1)
- [ ] **ForeignKey-Kindspalten** indiziert (`indices=[Index(...)]`)? (K7)
- [ ] **TypeConverter** an `@Database`, null-sicher, Enums backup-stabil (Code statt Name)? (C2, C3, C4)
- [ ] **KSP statt kapt** fuer den Room-Compiler? KSP-Suffix = Kotlin-Version? (V2, V3)
- [ ] **Version-Check:** Lohnt der Patch auf 2.7.2 / Upgrade auf 2.8.4 fuer einen der gelisteten Fixes? (§Fix-Status)

---

## 🔗 Bezug zu Best-Practices (Praevention, „wie macht man es richtig")

Dedizierte Gegenseite (seit 2026-06-15): [`best-practices/android/room.md`](../../best-practices/android/room.md)
— spiegelgleich abgelegt, damit der `bug-almanac-guard` nach diesem Almanach auch die Best-Practices-Seite
erzwingt (erst Almanach, dann Best Practices). Ergaenzend fuer den Framework-/Backup-Kontext:
`best-practices-android-platform.md` §3 (Runtime-Disziplin) + `best-practices-google-drive-backup.md` (WAL/Restore).

| Bug-Abschnitt (diese Datei) | Best-Practice-Abschnitt in `best-practices-room.md` |
|-----------------------------|------------------------------------------------------|
| V) Versionen / 2.7-Umstieg / Build | §1 Build-Setup, KSP & Kotlin-Codegen |
| M) Migration & Schema | §2 Migration & Schema (migrieren statt zerstören) |
| B) Backup/Restore (WAL) | §3 Backup/Restore mit WAL-Disziplin (+ `best-practices-google-drive-backup.md`) |
| T) Threading / Coroutines / Flow | §4 Threading, Coroutines & Flow |
| R) @Relation | §5 @Relation |
| C) TypeConverter / @Embedded | §6 TypeConverter & @Embedded |
| K) Keys / Insert / FK | §7 Keys, Insert & Foreign Keys |
| X) Connection / Locking | §8 Connection, Locking & Singleton |
