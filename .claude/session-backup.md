# Session Handoff — 2026-06-19, ca. 14:35

## Ziel (1-3 Saetze)
Entropie Reductor (Android, de.frank.entropyreducer.debug): Multi-Device-Sync ZUVERLAESSIG machen —
jede Aenderung (auch Loeschungen) sofort sichern und 1:1 aufs andere Geraet bringen. Etappe 1 (Aufgaben-
Bereich) ist fertig + verifiziert. Danach kam ein Folge-Bug (Loop-Aufgaben springen auf HEUTE zurueck),
der gefixt + bestaetigt ist. Zuletzt: Prioritaets-Logik analysiert (nur Bericht, nichts geaendert).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, aber eine OFFENE RUECKFRAGE an Frank wartet auf Antwort (siehe Offene Fragen):
Ich habe Frank gefragt, ob ich das **Loop-Prio-Analogon** genauso fixen soll wie den Bucket-Bug.
- Wenn Frank "ja" sagt: In `GenerateRecurringInstancesUseCase.kt` (cleanupAndEnsureSingle) wird bei
  Loop-Aufgaben die manuelle PRIORITAET (manualPriorityScore) mit der Template-Prio ueberschrieben —
  genau wie vorher der Bucket. Zwei Stellen: (a) Intervall-Pfad, mein userMovedManually-Zweig (~Z.320,
  "if keep.manualPriorityScore != prio -> upsert") UND der else-Zweig; (b) taeglicher Pfad (~Z.362-373,
  openForThis.firstOrNull -> synchronisiert manualPriorityScore=template.prio).
  Problem: es gibt KEINEN Zeitstempel fuer manuelle Prio (kein manualPriorityScoreSetAt) wie beim
  Bucket (manualBucketSetAt). Loesungs-Optionen vor dem Fix mit Frank klaeren: (1) neues Feld
  manualPriorityScoreSetAt analog manualBucketSetAt einfuehren + Vergleich > lastGeneratedAt; ODER
  (2) Loop-Pflege synchronisiert die Prio nur bei NEU-Generierung, nicht bei jeder Pflege.

## Aktueller Status
- Erledigt + committed + gepusht + auf S23 Ultra (R5CW206F0ZM) installiert + verifiziert:
  - Sync-Etappe 1 (#46919-46924, App v0.16.0): Timing (Drive sofort/APIs bei Biomarker-Tab+8h),
    Tombstones (Loeschungen, Schema v16, delete-wins-only-if-newer), Loop-LWW, Gewohnheit-LWW,
    Trigger-Luecken (Widget+Nachtraege). Live verifiziert: Gewohnheit geloescht -> tombstones=1 im
    Backup; Aufgabe verschoben -> Upload OK; Backup auf v16 umgestellt.
  - Bucket-Rollback-Bug (#46926, v0.16.2): cleanupAndEnsureSingle (Loop-Pflege) ueberschrieb manuelle
    MORGEN-Verschiebung mit Faelligkeits-Bucket (HEUTE). FIX: userMovedManually (manualBucketSetAt >
    template.lastGeneratedAt) -> Bucket bleibt manuell. Frank hat bestaetigt: funktioniert.
  - (#46925 Diagnose-Sonden in TasksViewModel + SyncEntriesUseCase BEHALTEN als Observability.)
- In Arbeit: nichts.
- Blockiert: warte auf Frank-Antwort zur Loop-Prio (siehe oben).

## Relevante Dateien (alle committed)
- domain/usecase/GenerateRecurringInstancesUseCase.kt — Loop-Pflege; Bucket-Fix drin (~Z.306-345),
  Loop-PRIO-Sync noch ungefixt (Z.~320 + ~362-373). HIER der evtl. naechste Fix.
- domain/usecase/ForegroundSyncManager.kt — Timing (syncDriveNow immer / syncApisNow|syncApisIfStale).
- data/TombstoneStore.kt — Loesch-Protokoll (markDeleted/mergeRemoteTombstones/unionTombstones, TombstoneType).
- domain/usecase/SyncEntriesUseCase.kt — Restore (LWW+Tombstone fuer entries/loop/gewohnheit/followup) + RESTORE-Bucket-Sonde.
- presentation/dashboard1/TasksViewModel.kt — setManualBucket/setManualPriority/refillHeute/rollover (+ Diag-Sonden); init ruft maybeAutoRescoreOnDoctrineChange.
- domain/usecase/ProcessEntryUseCase.kt — KI-Bewertung: rescoreExisting setzt AUSSCHLIESSLICH priorityScore (NIE manualPriorityScore).
- domain/usecase/BalanceBucketsUseCase.kt — Bucket-Verteilung; aendert nur timeBucket, nie manualBucket.

## Getroffene Entscheidungen
- Tombstones OHNE Sicherheitsnetz/Max-Delete (Frank: jede Loeschung sofort+voll propagieren).
- Timing: Drive immer sofort; Fitness-APIs nur bei Biomarker-Tab-Klick (LifecycleEventEffect ON_RESUME, Tab nutzt saveState!) ODER 8h-Automatik; Timer-Reset pro API-Lauf.
- Prio-Befund (NUR Bericht): manuelle Prio (manualPriorityScore) hat bei NORMALEN Aufgaben dauerhaft
  Vorrang (Anzeige/Sortierung "manualPriorityScore ?: priorityScore"; KI-Rescore fasst manualPriorityScore
  nie an). KI legt priorityScore automatisch fest (Erstellung + Aktualisieren-Knopf + Doktrin-Aenderung
  einmalig beim Start). EINZIGE Luecke: bei LOOP-Aufgaben ueberschreibt die Loop-Pflege die manuelle Prio.

## Fehlgeschlagene Ansaetze / wichtige Diagnose-Erkenntnisse
- Beim Bucket-Bug zuerst Restore-Race + rollover + refillHeute verdaechtigt -> per Sonden WIDERLEGT
  (keine dieser Sonden feuerte). Erst Franks Hinweis "KI-Refresh sortiert beim Start" + app-weiter
  manualBucket-Grep fuehrte zur 5. Stelle: GenerateRecurringInstancesUseCase (war NICHT besondet).
  Lehre: bei "wer setzt Feld X" IMMER app-weit nach JEDER Schreibstelle greppen, nicht nur die offensichtlichen.
- GewohnheitTtsViewModel.kt ist UNTRACKED = PARALLELE Session. NICHT anfassen/committen (vorlesen-Haekchen-Sync wartet darauf).

## Wichtige Recherche-Ergebnisse / Fakten
- S23 Ultra = R5CW206F0ZM (war zuletzt angeschlossen). Fold 6 = RFCX70KTDFX (war zwischendurch weg,
  NOCH auf v0.15.0 -> muss noch auf v0.16.2 geupdatet werden fuer echten 2-Geraete-Test).
- Diag-Log: /sdcard/Android/data/de.frank.entropyreducer.debug/files/diagnostics/diag-YYYY-MM-DD.jsonl
  (export MSYS_NO_PATHCONV=1; adb -s <serial> shell "cat <log>" | grep ...).
- DB inspizieren: adb -s <serial> exec-out run-as de.frank.entropyreducer.debug cat databases/entropy_reducer.db (+ -wal -shm) -> lokal python3 sqlite3. Tabelle entropy_entries (Spalten timeBucket, manualBucket, manualBucketSetAt, priorityScore, manualPriorityScore, updatedAt).
- Build/Install: ./gradlew assembleDebug; adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk; force-stop + start de.frank.entropyreducer.debug/.presentation.MainActivity.
- Almanache schon gelesen/freigegeben in dieser Session: kotlin, hilt-dagger, workmanager-notifications, jetpack-compose, android-platform, google-drive-backup, gradle.

## Naechste Schritte (priorisiert)
1. Auf Frank-Antwort warten: Loop-Prio-Analogon fixen? (siehe Wiedereinstiegspunkt). Falls ja: Design
   (manualPriorityScoreSetAt-Feld vs. nur-bei-Neu-Generierung) kurz mit Frank klaeren, dann fixen +
   v0.16.3 + Build + Install S23 + verifizieren.
2. Fold 6 anschliessen -> v0.16.2 installieren -> echter 2-Geraete-Test (Loeschung/Verschiebung auf
   A erscheint 3 Min spaeter auf B; im Fold-6-Log "1 via Tombstone geloescht").
3. Etappe 2 Sync: Mental/Ideen/Tagebuch/Thesen auf LWW+Tombstone; vorlesen-Haekchen (loop) +
   Anker/Folge/Repeat-Zaehler ins Backup (wartet auf parallele GewohnheitTtsViewModel-Session);
   App-Settings (Theme/Widget/Modell/Sprache/Stimme) ins Backup.

## Offene Fragen
- Soll ich die manuelle PRIORITAET bei LOOP-Aufgaben genauso schuetzen wie den Bucket (manuell schlaegt
  KI ueberall konsistent)? Frank wurde gefragt, Antwort steht aus.

## Anker
- Branch: main
- Letzte Commits:
e5b40ac30 #46926 - fix(EntropieReductor): Loop-Cleanup ueberschreibt manuelle Bucket-Verschiebung nicht mehr (manuell schlaegt KI) v0.16.2
ce6ed06c5 #46925 - diag(EntropieReductor): Bucket-Rollback-Sonden v0.16.1
5fd34aad6 #46924 - chore(EntropieReductor): version bump 0.16.0 (197) - Sync-Synchronitaet Etappe 1
23941c3a0 #46923 - feat(EntropieReductor): Sync-Etappe 1.5 Trigger-Luecken Widget-Abhaken + Nachtraege
b419a27d5 #46922 - feat(EntropieReductor): Sync-Etappe 1.4 Gewohnheit Zeitstempel + Last-Write-Wins + Tombstone
