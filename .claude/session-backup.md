# Session Handoff — 2026-06-20 ~18:40

## Ziel (1-3 Saetze)
EntropieReductor (Android, de.frank.entropyreducer.debug) geraeteuebergreifend VOLL synchron machen.
Diese Session hat die ID-Kette + Loesch-Propagierung komplett gehaertet. JETZT offen: den 3 Typen
OHNE Modifikations-Zeitstempel (Tagebuch, Ideen, Thesen) ein `updatedAt` geben, damit auch
BEARBEITUNGEN per Last-Write-Wins (LWW) zwischen Geraeten propagieren (und der Loesch-vs-Edit-Edge
sicher wird). Frank hat "alle 3 jetzt" autorisiert.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt (WICHTIGSTER ABSCHNITT)

> ====================================================================================
> >>> ALLERERSTE AKTION NACH DEM RESTORE (Frank-Anweisung 2026-06-20, woertlich):
> >>> "Wir starten direkt nach dem Session Restore mit PHASE B."
> >>> Also SOFORT mit Phase B beginnen — NICHT nachfragen, NICHT neu planen, NICHT die
> >>> Entscheidung "jetzt vs frisch" nochmal aufmachen (die ist erledigt: JETZT). Direkt
> >>> die erste Aktion ausfuehren: in `TagebuchScreen.kt` `data class TagebuchEntry` (~Z.822)
> >>> um `val updatedAt: Long = 0L` ergaenzen, dann die Phase-B-Liste unten Punkt fuer Punkt.
> ====================================================================================

- **Welche Aufgabe lief gerade:** "updatedAt-Umbau fuer Tagebuch/Ideen/Thesen" (Task #17). Frank hat
  ausdruecklich "alle 3 jetzt umsetzen" gewaehlt. PHASE A (Room-Migration) ist FERTIG + LIVE BEWIESEN
  SICHER. PHASE B (die updatedAt-Verkabelung, ~30 Stellen) ist NOCH NICHT BEGONNEN — das ist der
  Wiedereinstieg. Es gibt KEIN offenes Multiple-Choice mehr: einfach Phase B umsetzen (Frank will es).
- **Wo genau unterbrochen:** Nach Phase-A-Verifikation (Daten-Survival OK) habe ich Frank gefragt
  "Phase B jetzt durchziehen vs frisch fortsetzen" — er antwortete mit "mache ein session backup".
  Also: Phase B startet bei null. Kein halbfertiger Edit.
- **Schon erledigter Teil (Phase A, committed #46998, 0.17.21):**
  - `IdeaEntity.kt`: `val updatedAt: Long? = null` ergaenzt (nullable!).
  - `AppDatabase.kt`: version 34->35; `MIGRATION_34_35` = `ALTER TABLE ideas ADD COLUMN updatedAt INTEGER`
    (additiv, NULLABLE, KEIN NOT NULL/DEFAULT -> keine identityHash-Falle M3).
  - `DatabaseModule.kt`: `AppDatabase.MIGRATION_34_35` in `.addMigrations(...)` registriert.
  - LIVE auf S23 (R5CW206F0ZM) installiert: DB user_version=35, ALLE Daten intakt (ideas=13,
    entropy_entries=70, habits=3, mental_sentences=8, entropy_entry_followups=3), updatedAt-Spalte da.
- **Noch offener Teil = PHASE B (die Nutzung), pro Typ ~10 Stellen:**
  - **TAGEBUCH (DataStore, `presentation/tagebuch/TagebuchScreen.kt`):**
    1. `data class TagebuchEntry` (~Z.822): `val updatedAt: Long = 0L` ergaenzen.
    2. `TagebuchEntry.create(text)` (~Z.848): `updatedAt = System.currentTimeMillis()` setzen (=Erstellzeit).
    3. `updateTagebuchEntry` (~Z.928): im `e.copy(...)` (Z.949) `updatedAt = System.currentTimeMillis()`.
    4. Nachtrag-Funktionen je im `e.copy(...)` des geaenderten Eintrags `updatedAt = now` setzen:
       `setTagebuchFollowupImproved` (~969), `addTagebuchFollowup` (~997), `updateTagebuchFollowup` (~1012),
       `deleteTagebuchFollowup` (~1032).
    5. `jsonToEntry` (~Z.1055): `updatedAt = o.optLong("updatedAt", o.optLong("ts"))` (Bestand -> ts=timestampMs als Baseline).
    6. `serializeEntries` (~Z.1085): `o.put("updatedAt", e.updatedAt)`.
  - **THESEN (DataStore, `presentation/thesen/ThesenScreen.kt`):** GENAU spiegeln wie Tagebuch.
    `ThesenEntry` ~Z.808, `create`, `updateThesenEntry`, die 4 Nachtrag-Funktionen, `jsonToEntry`,
    `serializeEntries`. (Struktur ist 1:1 wie Tagebuch.)
  - **IDEEN (Room, `presentation/ideen/IdeenScreen.kt` — Spalte existiert schon ab Phase A):**
    1. `data class IdeenEntry` (~Z.824, UI-Modell): `val updatedAt: Long? = null`.
    2. `ideenEntriesFlow`: beim Mappen IdeaEntity->IdeenEntry `updatedAt = it.updatedAt` mittragen.
    3. `addIdeenEntry`: die `IdeaEntity` mit `updatedAt = System.currentTimeMillis()` schreiben.
    4. `updateIdeenEntry` (~Z.983): `updatedAt = System.currentTimeMillis()` mitsetzen (copy auf IdeaEntity).
    5. `addIdeenFollowup` / `deleteIdeenFollowup`: Parent-Idee laden, `copy(updatedAt = now)`, `ideaDao.upsert/update`.
  - **BACKUP-DTOs (`data/remote/drive/BackupPayload.kt`):**
    - `BackupTagebuchEntry` (~Z.365): `val updatedAt: Long = 0L`.
    - `BackupThesenEntry` (~Z.350): `val updatedAt: Long = 0L`.
    - `BackupIdeenEntry` (~Z.384): `val updatedAt: Long? = null`.
  - **BACKUP-MAPPING (`data/remote/drive/SyncCoordinator.kt`, INLINE — wie ideenBackups bei ~Z.360-377):**
    In den Mappings fuer tagebuchBackups / thesenBackups / ideenBackups je `updatedAt = ...` mitschreiben.
    (tagebuch/thesen-Mappings dort suchen; ideenBackups baut BackupIdeenEntry inline.)
  - **RESTORE LWW (`domain/usecase/SyncEntriesUseCase.kt` — die 3 Abschnitte, die ich schon fuer
    Tombstones umgebaut habe: Tagebuch ~Z.388, Ideen ~Z.458, Thesen ~Z.756):**
    Aktuell "add if not existing". UMSTELLEN auf LWW:
      - existiert lokal UND `incoming.updatedAt > existing.updatedAt` -> ERSETZEN
        (DataStore: `deleteX(appContext, id, propagate=false)` + `addX(appContext, incoming)`;
         Ideen Room: `ideaDao.upsert(neueEntity)`).
      - Tombstone praezisieren: statt "immer anwenden" jetzt delete-wins-ONLY-IF-NEWER:
        `if (tombstone.ts > existing.updatedAt) loeschen` (Edit gewinnt gegen aeltere Loeschung).
    VORLAGE 1:1: `restoreGewohnheiten` in `GewohnheitBoardScreen.kt` (~Z.206-235) — die hat LWW
    (`if (inc.updatedAt > ex.updatedAt) dao.update(ex.copy(text=inc.text, updatedAt=inc.updatedAt))`)
    UND delete-wins-only-if-newer. Mental (`restoreMentals`) ebenso.
  - **VERSION + BUILD:** build.gradle.kts versionCode 223->224, versionName 0.17.21->0.17.22.
- **So geht es EXAKT weiter (allererste Aktion):** TagebuchScreen.kt oeffnen, `data class TagebuchEntry`
  (~Z.822) um `val updatedAt: Long = 0L` ergaenzen — dann Punkt fuer Punkt obige Tagebuch-Liste,
  danach Thesen (spiegeln), Ideen, Backup-DTOs, SyncCoordinator-Mapping, SyncEntriesUseCase-LWW,
  Version-Bump. EIN Commit (#46999), EIN Build, install auf S23.
- **Was dafuer alles vorhanden sein muss / Technik:**
  - S23 serial **R5CW206F0ZM** (nur dieses Geraet angeschlossen). PATH: `export PATH="$PATH:/c/Users/barwa/AppData/Local/Android/Sdk/platform-tools"`.
  - Build: `cd ~/proggs/EntropieReductor && ./gradlew :app:assembleDebug` (run_in_background, ~30-50s).
  - Install: `adb -s R5CW206F0ZM install -r app/build/outputs/apk/debug/app-debug.apk` + monkey-Launch.
  - DB pruefen: `adb -s R5CW206F0ZM exec-out run-as de.frank.entropyreducer.debug cat databases/entropy_reducer.db(+-wal/-shm) > ~/_dbtmp/x.db` -> python sqlite3 mit `os.path.expanduser` (Git-Bash-/tmp != Python-/tmp!).
  - Checkpoint-Logs: `adb -s R5CW206F0ZM logcat -d -v time | grep -E "GenSuggest|AcceptLineage|Diag/AGENTIC"`.
  - Phase B hat KEINEN weiteren Room-Eingriff -> kein Migrations-Risiko mehr (nur DataStore + Room-Daten unveraendert).
- **Uncommitteter Arbeitsstand:** KEINER (Phase A committed #46998). Fremde untracked Datei
  `EntropieReductor/.../presentation/mental/GewohnheitTtsViewModel.kt` ist NICHT meine (war bei
  Session-Start da) — NICHT anfassen/committen.
- **Danach:** Mit "Naechste Schritte" weiter (Cross-device-Test, Followup-Tombstones, etc.).

## Aktueller Status
- Erledigt diese Session (alle committed+gepusht, auf S23 installiert):
  - #46990 (0.17.17): robuster ID-Ketten-Fix — Kette bricht beim Annehmen nicht mehr (countByRootId
    auf entropy_entries+habits; BackupEntry-Herkunft Schema v19; Gewohnheits-Backup direkt aus habits
    mit Herkunft; processedIds-Guertel). LIVE bewiesen skipAccepted=1.
  - #46991-46995 (0.17.18): 4 Haertungen (KI-Aussetzer-Lineage singleOrNull; Accept-Live-Sonde
    AcceptLineage; Gewohnheits-processedIds-Guertel; Mental-Backup-Herkunft) + Version.
  - #46996 (0.17.19): Vorschlags-Loeschung propagieren (TombstoneType.TASK_SUGGESTION/HABIT_SUGGESTION)
    + chain-bewusster Restore. LIVE bewiesen: OpenCodeGo-Vorschlag raeumte sich selbst weg.
  - #46997 (0.17.20): Tombstone-Vollaudit -> Tombstones fuer Mental/Ideen/Tagebuch/Thesen (deleteX
    bekam propagate-Flag + markDeleted; Restore wendet Tombstones an). Alle user-loeschbaren HAUPT-Typen
    propagieren jetzt Loeschungen.
  - #46998 (0.17.21): Phase A — DB-Migration 34->35 (ideas.updatedAt nullable). VERIFIZIERT SICHER.
- In Arbeit: Phase B (updatedAt-Nutzung, siehe oben).
- Blockiert: nichts.

## Relevante Dateien
- `presentation/tagebuch/TagebuchScreen.kt` — Tagebuch-Modell+Funktionen (DataStore).
- `presentation/thesen/ThesenScreen.kt` — Thesen (1:1 wie Tagebuch).
- `presentation/ideen/IdeenScreen.kt` — Ideen (Room, IdeaEntity).
- `data/local/entities/IdeaEntity.kt` — hat jetzt updatedAt (Phase A).
- `data/local/AppDatabase.kt` — version 35, MIGRATION_34_35. `di/DatabaseModule.kt` — addMigrations + fallbackToDestructive(dropAllTables=true) (NICHT drauf verlassen!).
- `data/remote/drive/BackupPayload.kt` — Backup-DTOs (BackupTagebuchEntry/BackupThesenEntry/BackupIdeenEntry, ~Z.350/365/384).
- `data/remote/drive/SyncCoordinator.kt` — Backup-Mappings (inline, ideenBackups ~Z.360-377).
- `domain/usecase/SyncEntriesUseCase.kt` — Restore (3 Abschnitte schon mit Tombstones; LWW fehlt noch).
- `presentation/mental/GewohnheitBoardScreen.kt` `restoreGewohnheiten` (~Z.206) — LWW-VORLAGE.

## Getroffene Entscheidungen
- Ideen-Spalte NULLABLE (`updatedAt INTEGER`, kein NOT NULL/DEFAULT) — sicherste additive Migration,
  keine identityHash-Falle (Room-Almanach M3). Im Sync `updatedAt ?: timestampMs` als Baseline.
- Sync-Modell bleibt: additiv + LWW(updatedAt) + Tombstones(delete-wins-only-if-newer). KEIN naiver
  Mirror (Single-Backup-Datei -> Overwrite-Gefahr, M2). Frank versteht + will genau das.
- Migration immer registrieren (NIE auf fallbackToDestructive verlassen = stiller Datenverlust).

## Fehlgeschlagene Ansaetze / Fallen (WICHTIG — nicht wiederholen)
- NICHT `updatedAt` als NOT NULL mit ALTER TABLE ADD COLUMN auf Tabelle MIT Zeilen -> SQLite-Fehler;
  und NOT NULL+DEFAULT erzeugt identityHash-Mismatch (M3). Loesung: nullable (gemacht).
- NICHT die Loesch-Funktion (deleteX mit propagate=true) im Restore-Cleanup nutzen -> erzeugt neuen
  Tombstone + Backup-Loop. Im Restore IMMER `propagate=false` (roher Cleanup).
- Linter beruehrt SyncEntriesUseCase.kt zwischendurch -> bei "File modified since read" neu lesen.
- Session-Backup-Heredoc per `bash -c` bricht an Single-Quotes (Bash-Tool wrappt in '...'); stattdessen
  Write-Tool fuer die Backup-Datei nutzen (so gemacht).

## Wichtige Fakten
- ignoreUnknownKeys=true + coerceInputValues=true (NetworkModule.kt:37) + runCatching um decode
  (SyncEntriesUseCase:109) -> aeltere App-Version liest neueres Backup OHNE Crash (cross-version safe).
- Fold6 (RFCX70KTDFX) NICHT angeschlossen. Erst nach Update auf 0.17.21+ propagiert beidseitig.
- Loeschung loest Upload aus, weil Tombstone den payload.hashCode() aendert (SyncCoordinator ~Z.497).
- Frank-Prinzip: finale Loeschung MUSS auf allen Geraeten halten; Restore raeumt mit auf (Tombstone),
  nicht nur additiv. Edits sollen genauso 1:1 sein -> deshalb Phase B.

## Naechste Schritte (priorisiert)
1. PHASE B umsetzen (siehe Wiedereinstiegspunkt) — Tagebuch, Thesen, Ideen, Backup-DTOs,
   SyncCoordinator-Mapping, SyncEntriesUseCase-LWW, Version 0.17.22. Commit #46999, Build, install S23.
2. Cross-device-Test live mit Fold6 (auf 0.17.22 updaten) — Edit auf A -> erscheint auf B.
3. Followup-/Sub-Eintrags-Loeschungen (Tagebuch/Ideen/Thesen-Nachtraege) auf Propagierung pruefen
   (Parent-updatedAt-Bump deckt das ab, sobald Phase B drin ist — verifizieren).
4. Optional: Sync-Status-Anzeige in der App; er-deploy.sh Helfer (~/_dbtmp).

## Offene Fragen
- Keine offene Rueckfrage. Frank hat "alle 3 jetzt" autorisiert -> Phase B direkt umsetzen.

## Anker
- Branch: main
- Letzte Commits:
39c2a3a51 #46998 - feat(EntropieReductor): DB-Migration 34->35 ideas.updatedAt (Phase A); 0.17.21
594d07cf1 #46997 - fix(EntropieReductor): Tombstones fuer Mental/Ideen/Tagebuch/Thesen; 0.17.20
2e4b621d5 #46996 - fix(EntropieReductor): Vorschlags-Loeschung propagieren + chain-bewusster Restore; 0.17.19
0d126195a #46995 - chore(EntropieReductor): version 0.17.18
(Hinweis: #46999-#47003 sind teils aus einer PARALLELEN Session (or-research) — nicht meine Arbeit.)
