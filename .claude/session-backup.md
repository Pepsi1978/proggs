# Session Handoff — 2026-06-20, ~00:30

## Ziel
EntropieReductor (private Android-App, Kotlin/Compose/Room/Hilt/Gemini): app-weite ID-/Herkunfts-Architektur.
ANSATZ 3 (Frank-Entscheidung): ALLE Daten in EINE Room-DB (DataStore-JSON-Listen ziehen um), jeder Eintrag
id + originId + originType + rootId. Dedup NIE blockierend (ID-Kette = exakte Identitaet; Prioritaets-
Gedaechtnis = semantische Aehnlichkeit). Feingranularer Multi-Device-Sync (LWW+Tombstone) bleibt + DB-Snapshot.
SPEC (vollstaendig): EntropieReductor/docs/specs/2026-06-19-id-architektur-design.md
Frank-Wunsch: ALLE Etappen durchziehen, nur commit+push zwischendurch, OHNE Pause.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
- **Erledigt + verifiziert + committed:** Etappe 1, 2a, 2b. App laeuft (Frank bestaetigt), Daten doppelt
  gesichert (Room + JSON-Fallback).
- **Naechster Schritt: Etappe 2c + 2e ZUSAMMEN (NICHT trennen!).**
  KRITISCHE ERKENNTNIS: 2c (Ideen/Vorschlaege-Lese-/Schreibquelle auf Room umstellen) DARF NICHT ohne 2e
  (Drive-Backup/Sync-Schema auf Room) ausgeliefert werden — sonst sichert das Backup weiter die alten
  JSON-Stores und NEUE Room-Ideen landen NICHT im Backup => Datenverlust beim Geraete-Wechsel.
- **ALLERERSTE Aktion neue Session:** Spec lesen, dann 2c+2e als EINEN datenkritischen Block planen.
  Vorher Bug-Almanach-Kurzchecks (Stufe A) erneut lesen — in NEUER Session sind alle Bereiche wieder
  gesperrt. Fuer 2c zusaetzlich **Jetpack-Compose**-Almanach+BP (IdeenScreen.kt ist Compose) + erneut
  Room/Hilt/Kotlin.

## So geht 2c+2e EXAKT (Architektur-Analyse aus dieser Session)
Ideen-Persistenz = 9 freie `internal fun` in presentation/ideen/IdeenScreen.kt (KEIN ViewModel, Compose
ruft direkt): ideenEntriesFlow, ideenEntryFlow, addIdeenEntry, deleteIdeenEntry, updateIdeenEntry,
setIdeenFollowupImproved, addIdeenFollowup, updateIdeenFollowup, deleteIdeenFollowup + Helper
(parseEntries, serializeEntries, jsonToEntry, jsonToFollowups). DataStore "ideen_entries"/Key
"entries_json". Jede ruft triggerDriveBackup(context, ...). Followups verschachtelt im JSON.
- Umstellen auf IdeaDao via Hilt @EntryPoint (freie Context-Funktionen) ODER sauberer: IdeaRepository +
  IdeenViewModel. ideenEntriesFlow braucht Ideen+Followups kombiniert -> IdeaDao um Flow erweitern
  (@Relation IdeaWithFollowups ODER combine(getAllIdeas(), getAllFollowupsFlow())). Mapping
  IdeaEntity<->IdeenEntry, IdeaFollowupEntity<->IdeenFollowup schreiben.
- **FALLE:** IdeaTaskRoomMigrator (2b) nutzt ideenEntriesFlow + taskSuggestionsForBackup zum JSON-Lesen!
  Wird ideenEntriesFlow auf Room umgestellt, MUSS der Migrator auf eine DEDIZIERTE JSON-Lese-Funktion
  umgehaengt werden (sonst liest er Room statt JSON = kaputt). Gleiches fuer taskSuggestionsForBackup.
Vorschlaege: presentation/dashboard1/KiTaskSuggestViewModel.kt (HiltViewModel, kiTaskSuggestionStore,
Key "suggestions_json" + "processed_idea_ids") + presentation/dashboard1/AutoSuggestionViewModel.kt
(NOCH NICHT GELESEN — nutzt TASK_PROCESSED_KEY + HABIT_PROCESSED_KEY, kombiniert beide Sets). Auf
TaskSuggestionDao umstellen.
2e Backup/Sync: data/remote/drive/BackupPayload.kt (BackupTaskSuggestion, BackupMental, BackupPriorityMemory),
data/SuggestionDataStores.kt (taskSuggestionsForBackup/restoreTaskSuggestions/gewohnheit...), SyncCoordinator.kt,
SyncEntriesUseCase.kt (Restore LWW+Tombstone), data/TombstoneStore.kt. Neue Tabellen ideas/idea_followups/
task_suggestions ins Backup-Schema + Sync + Tombstone aufnehmen (Backup-Schema-Version hochzaehlen).

## Danach: Etappe 2d, dann 3, 4, 5
- **2d** (Herkunft + Dedup): GenerateSuggestionsUseCase.generateSuggestions schickt ALLE newIdeas zusammen
  an Gemini, bekommt flache tasks[]/habits[] OHNE Rueckbezug zur Quell-Idee (Prompt sagt "GENAU EINE Idee",
  Code schickt aber alle — Diskrepanz). LOESUNG: Prompt erweitern -> Ideen nummerieren/mit id, JSON-Antwort
  pro task/habit mit sourceId. Dann originId/originType(IDEA)/rootId an Vorschlag setzen. Vorschlag->Aufgabe:
  KiTaskSuggestViewModel.acceptSuggestion baut heute "title. description" als Text und ruft process()
  -> ProcessEntryUseCase erzeugt neue UUID (ID-VERLUST). Kuenftig originId=Vorschlag-id durchreichen
  (ProcessEntryUseCase um origin-Parameter erweitern). processed_idea_ids + habit_processed_idea_ids
  ABLOESEN durch Ketten-Dedup (TaskSuggestionDao.countByOriginId existiert schon) — NIE blockieren
  (Frank: aehnliche neue Ideen immer erlaubt; Aehnlichkeit erkennt das Prioritaets-Gedaechtnis).
- **3** (Gewohnheiten): Tabellen habit_suggestions + habits NOCH NICHT angelegt (2a legte nur
  ideas/idea_followups/task_suggestions an). Analog 2a anlegen + Migrator + UI (MentalBoardScreen/
  GewohnheitBoardScreen, GewohnheitSuggestViewModel) umstellen. Mental-Klasse: presentation/mental/MentalBoardScreen.kt.
- **4** (Mental-Saetze): Tabelle mental_sentences anlegen + umstellen.
- **5** (weitere Ketten): Entropie->Idee, These->Idee (originId durchreichen).

## Aktueller Status
- Etappe 1 (#46961): originId/originType/rootId an entropy_entries, Migration 30->31. Verifiziert.
- Etappe 2a (#46962): Tabellen ideas/idea_followups/task_suggestions + IdeaDao/TaskSuggestionDao,
  Migration 31->32. Schema-JSON gegen Migration abgeglichen (KEINE SQL-DEFAULT bei Kotlin-Default-Feldern!).
- Etappe 2b (#46963): IdeaTaskRoomMigrator (data/local/) — einmalig JSON->Room, idempotent (SharedPrefs-Flag
  id_arch_ideas_tasks_v1_done + leer-Check), Anzahl-Abgleich-Sonden, JSON bleibt Fallback. In
  EntropyReducerApp.onCreate via migrateIfNeeded(). Verifiziert: 10 Ideen + 1 Vorschlag kopiert (ok=true).
- App-Version 0.17.4 (versionCode 206), DB-Version 32.

## Fehlgeschlagene Ansaetze / Fallen
- Bug-Almanach-Guard blockt Edit/Write pro Bereich bis Almanach+BestPractices-Kurzcheck (Read limit=80)
  gelesen. Diese Session freigegeben: Room, Gradle, Hilt, Kotlin, WorkManager. NEUE Session: erneut noetig
  (auch Jetpack-Compose fuer IdeenScreen-Edits).
- Room + fallbackToDestructiveMigration(dropAllTables=true) (DatabaseModule): jede Migration MUSS exakt zum
  Entity-Schema passen, sonst stiller Datenverlust. KEINE SQL-DEFAULT-Klausel fuer Felder mit Kotlin-Default
  (z.B. isImproved=false -> Room-Schema hat KEIN DEFAULT). Vor Install Schema-JSON
  (app/schemas/de.frank.entropyreducer.data.local.AppDatabase/NN.json, GITIGNORED) gegen Migration abgleichen.
- Session-Backup: single-quoted Heredoc (<<'EOF') ueber den Bash-Tool-Wrapper scheitert am Quoting
  ("unexpected EOF") — stattdessen Write-Tool + cp ins Repo (so wurde dieses Backup geschrieben).
- git commit -- <pfad> scheitert bei NOCH-UNTRACKTEN neuen Dateien -> erst git add, dann commit.
- Fremde untrackte Datei GewohnheitTtsViewModel.kt (presentation/mental/) gehoert PARALLELER Session — NICHT anfassen.

## Verifikation pro Etappe (Frank-Vorgabe: Sonden ueberall + Log-Kette)
Sonden via Diag.i/d/w/e(DiagnosticArea.DATABASE bzw. TASKS, TAG, msg) — Fassade data/diagnostics/Diag.kt,
laeuft auch vor Hilt. Verifizieren: adb -s RFCX70KTDFX logcat -d | grep "Diag/DATABASE" bzw. "CHECKPOINT".
Build -> Schema-Abgleich -> commit+push -> install -> logcat. Kein Crash/"Room cannot"/destructive = ok.

## Anker
- Branch: main, Version 0.17.4 (versionCode 206), DB-Version 32
- Geraet: RFCX70KTDFX, Debug-Package de.frank.entropyreducer.debug
  (Start: adb -s RFCX70KTDFX shell monkey -p de.frank.entropyreducer.debug -c android.intent.category.LAUNCHER 1)
- Build: cd ~/proggs/EntropieReductor && ./gradlew :app:assembleDebug
- Letzte Commits:
51aa7ad07 #46963 - ID-architecture stage 2b (migration ideas/tasks JSON->Room)
d9bc2759f #46962 - ID-architecture stage 2a (ideas/idea_followups/task_suggestions tables)
ccf8c2d19 #46961 - ID-architecture stage 1 (origin/root fields on entropy_entries)
5def2e9bc #46960 - design spec for app-wide ID/origin architecture
