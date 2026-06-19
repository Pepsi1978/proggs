# Session Handoff — 2026-06-19, ca. 12:35

## Ziel (1-3 Saetze)
Entropie Reductor (Android, de.frank.entropyreducer): Das gesamte Drive-Backup+Restore-System
zuverlaessig machen und mit Diagnose-Sonden ausstatten. Frank hatte zuerst einen Sync-Bug bei
Mental/Ideen/Gewohnheit gemeldet (geloest), dann ein vollstaendiges Backup-Audit + "ueberall
Diagnose-Sonden" gewuenscht (umgesetzt in 5 Etappen).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. ALLE 5 Etappen sind committed,
gepusht, gebaut (BUILD SUCCESSFUL) und auf dem Fold 6 live verifiziert.

EINZIGE offene Folge-Aktion (NÖTIGE AUFGABE): Das **S23 Ultra (Serial R5CW206F0ZM)** ist noch
NICHT auf v0.15.0 — aktuell ist nur das Fold 6 (RFCX70KTDFX) auf dem neuen Stand. Sobald Frank
das S23 per USB anschliesst, dort installieren:
  cd "$HOME/proggs/EntropieReductor"
  adb -s R5CW206F0ZM install -r app/build/outputs/apk/debug/app-debug.apk
  adb -s R5CW206F0ZM shell am force-stop de.frank.entropyreducer.debug
  adb -s R5CW206F0ZM shell am start -n de.frank.entropyreducer.debug/de.frank.entropyreducer.presentation.MainActivity
(force-stop noetig, sonst greift der 8h-Throttle und der Start-Sync laeuft nicht.)
Die APK v0.15.0 ist bereits gebaut und liegt unter app/build/outputs/apk/debug/app-debug.apk.

## Aktueller Status
- Erledigt (alle gepusht):
  - #46910 Gewohnheit-Sync (Schema v14) + Diagnose-Sonde im Restore
  - #46912 M2-Read-before-write-Schutz (rescue-if-empty) gegen Multi-Device-Overwrite
  - #46913 bug-case (CBR)
  - #46914 Etappe 1: Trigger-Sonden an ALLEN Backup-Ausloesern (requestSync(reason, debounceMs),
    triggerDriveBackup(context, reason)) — jeder Trigger loggt Bereich+Grund
  - #46915 Etappe 2: KI-Vorschlaege ins Backup (Schema v15: taskSuggestions + gewohnheitSuggestions),
    Helper in data/SuggestionDataStores.kt, jede Vorschlags-Mutation triggert
  - #46916 Etappe 3-5: Biomarker 5s-Debounce (BIOMARKER_DEBOUNCE_MS), Trainings-Workouts-M2-Schutz
    (remoteWorkoutsHasData), Restore-Abschluss-Bilanz-Sonde. App v0.15.0 (versionCode 196).
- Frank's Datenrettung (frueher in dieser Session): 8 Mental, 7 Ideen, 2 Gewohnheiten vom Fold 6
  ins Drive-Backup und aufs S23 wiederhergestellt (war durch M2-Overwrite + Fingerprint-Deadlock
  verloren gegangen). Lokales Sicherheitsnetz: _dbtmp/fold6-datastore-backup-2026-06-19/
- In Arbeit: nichts.
- Blockiert: S23-Update wartet nur darauf, dass Frank das Geraet anschliesst.

## Relevante Dateien (alle committed)
- EntropieReductor/.../data/remote/drive/SyncCoordinator.kt — Upload, requestSync(reason,debounceMs),
  mergeRemoteAdditiveLists (M2-rescue inkl. Vorschlaege), remoteWorkoutsHasData, BIOMARKER_DEBOUNCE_MS
- EntropieReductor/.../data/remote/drive/BackupPayload.kt — Schema v15, BackupTaskSuggestion
- EntropieReductor/.../data/SuggestionDataStores.kt — taskSuggestionsForBackup/restoreTaskSuggestions etc.
- EntropieReductor/.../domain/usecase/SyncEntriesUseCase.kt — restoreFromDrive, Restore-Payload-Sonde + Bilanz
- EntropieReductor/.../data/remote/drive/DriveBackupTrigger.kt — triggerDriveBackup(context, reason)
- EntropieReductor/.../presentation/dashboard4/BiomarkerViewModel.kt — refreshNow/refreshWeight 5s
- ~20 weitere Repos/Screens/ViewModels mit reason-annotierten Triggern (Etappe 1)

## Getroffene Entscheidungen
- M2-Schutz = "rescue-only-if-empty" (NICHT voller Union per ID): nur wenn eine additive DataStore-Liste
  lokal KOMPLETT leer ist, wird sie aus dem Remote gerettet. Grund: kein Deletion-Tracking — ein voller
  Union wuerde bewusst geloeschte Einzeleintraege wiederbeleben. Gilt fuer Mental/Ideen/Gewohnheit/
  Tagebuch/Thesen/Vorschlaege + separat fuer das Workouts-Backup.
- Vorschlaege werden mitgesichert (Frank bestaetigt), Existenz-Strategie beim Restore.
- Biomarker 5s statt 1500ms; dirtyDuringUpload deckt den "danach nochmal"-Re-Run ab.

## Fehlgeschlagene Ansaetze
- Keine echten Sackgassen. Hinweis: Bei Git-Bash auf Windows fuer adb-/sdcard-Pfade IMMER
  `export MSYS_NO_PATHCONV=1` setzen, sonst wird /sdcard/... zu C:/Program Files/Git/... umgeschrieben.
- Beim ersten M2-Entwurf war ein voller Union geplant — verworfen (haette Loeschen kaputtgemacht), siehe Entscheidung oben.

## Wichtige Recherche-Ergebnisse
- Diagnose-Logs der App: /sdcard/Android/data/de.frank.entropyreducer.debug/files/diagnostics/diag-YYYY-MM-DD.jsonl
  (JSON-Lines). Auslesen: `export MSYS_NO_PATHCONV=1; adb -s <serial> shell "cat .../diag-2026-06-19.jsonl" | grep ...`
- DataStore-Dateien: `adb -s <serial> shell run-as de.frank.entropyreducer.debug ls -la files/datastore/`
- Geraete: S23 Ultra = R5CW206F0ZM, Fold 6 = RFCX70KTDFX.
- Almanach/Best-Practices fuer Drive-Backup, Room, Kotlin, Hilt, Compose, Gradle, WorkManager in dieser
  Session bereits gelesen/freigegeben.

## Naechste Schritte (priorisiert)
1. Wenn Frank das S23 anschliesst: v0.15.0 installieren + force-stop + starten (Befehle siehe oben),
   dann per diag-Log Restore-Payload v15 + kein Crash verifizieren.
2. Offen/optional (Frank fragen): Diagnose-Sonden AUCH ueber das Backup hinaus (KI-Aufrufe, einzelne
   API-Syncs, Navigation) — Frank's Wunsch "gesamte App voll mit Sonden". Eigene grosse Etappe.
3. Optional: Best-Practices google-drive-backup.md um das Sonden-Pflichtmuster ergaenzen (Intelligenz-Vorschlag 2).

## Offene Fragen
- Will Frank die Sonden auch ausserhalb des Backup-Flusses (ganze App)? Noch nicht beantwortet.

## Anker
- Branch: main
- Letzte Commits:
f4095b3b6 #46916 - feat(EntropieReductor): Biomarker-5s-Debounce, Trainings-M2-Schutz, Restore-Bilanz-Sonde (Etappe 3-5)
ac98f1fbf #46915 - feat(EntropieReductor): KI-Vorschlaege ins Drive-Backup (Schema v15, Etappe 2)
cbff4d639 #46914 - feat(EntropieReductor): Diagnose-Sonden an ALLEN Backup-Triggern (Etappe 1)
c2426b3a3 #46913 - docs: bug-case Entropie Reductor M2-Multi-Device-Overwrite + Gewohnheit-Sync (CBR)
0ea998a4c #46912 - fix(EntropieReductor): M2-Read-before-write-Schutz gegen Multi-Device-Backup-Overwrite
