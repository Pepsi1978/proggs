# Session Handoff — 2026-06-20 ~14:40

## Ziel (1-3 Saetze)
EntropieReductor ID-Architektur (Lineage Idee->Vorschlag->Aufgabe/Gewohnheit, Ketten-Dedup ueber
countByOriginId) live mit Frank verifizieren UND haerten. Sonden eingebaut, Multi-Device-Lecke
gefunden+gefixt (Backup verlor Herkunft), Altbestand-Backfill gebaut. Jetzt im FINALEN Roundtrip-
Schritt: beweisen, dass auf dem 2. Geraet (S23) kein Doppelvorschlag entsteht.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt (WICHTIGSTER ABSCHNITT)
- **Welche Aufgabe lief gerade:** Backup-Roundtrip ueber ZWEI Geraete (Fold6 -> Google Drive ->
  S23 Ultra) live beweisen. Der Roundtrip-Kernbeweis ist SCHON ERBRACHT (siehe unten). Es fehlt nur
  noch der kroenende Dedup-Schritt auf dem S23.
- **Wo genau unterbrochen — der allerletzte Schritt:** Ich hatte Frank gebeten, auf dem S23 Ultra
  (aktuell per USB angeschlossen, serial R5CW206F0ZM) im Aufgaben-Reiter den "Aktualisieren"-Button
  zu druecken. Ich wartete auf sein "fertig", um danach den Dedup-CHECKPOINT auf dem S23 auszulesen.
  Dann kam "session backup".
- **Schon erledigter Teil DIESES Schritts:** ROUNDTRIP BEWIESEN. S23 (frisch auf 0.17.16, hatte die
  Daten vorher NICHT) hat nach Update+Sync vom Fold6 ueber Drive bekommen: task_suggestion "OpenCodeGo
  Abo abschliessen" originId=054d2a1a originType=IDEA rootId=054d2a1a; habit_suggestion "Ich giesse
  alle zwei Tage Blumen" originId=1e3ab2fd. Die originIds stimmen EXAKT mit den Fold6-Ideen (054d2a1a=
  "OpenCodeGo Abo", 1e3ab2fd="Blumenpflege planen", beide auf S23 vorhanden). -> Die Herkunft hat
  Backup->Restore ueber Drive UEBERLEBT (vor v18-Fix waere sie NULL gewesen). "Claude Code testen"
  bleibt originId=NULL (KI-Aussetzer ohne sourceIndex, konsistent zum Fold6).
- **Noch offener Teil DIESES Schritts:** Der Dedup-Beweis AUF DEM S23: drueckt Frank "Aktualisieren",
  muessen OpenCodeGo- und Blumen-Idee uebersprungen werden -> idealerweise ueber den NEUEN skipChain
  (countByOriginId>0, weil die Herkunft jetzt gesynct ist), nicht nur skipProcessedList. Das ist der
  ultimative Beweis fuer geraeteuebergreifenden Ketten-Dedup.
- **So geht es EXAKT weiter (allererste Aktion der neuen Session):**
  1. `export PATH="$PATH:/c/Users/barwa/AppData/Local/Android/Sdk/platform-tools"`
  2. Frank fragen/bestaetigen, ob er auf dem S23 schon "Aktualisieren" gedrueckt hat.
  3. S23-Dedup-CHECKPOINT lesen (Dump+grep, NICHT der -s Slash-Tag-Filter):
     `adb -s R5CW206F0ZM logcat -d -v time | grep -E "GenSuggest.*CHECKPOINT" | tail -8`
     Erwartet: `CHECKPOINT Dedup: ... skipChain>=1 ... toProcess=0` (mindestens toProcess=0).
  4. Optional DB-Gegenkontrolle S23 (siehe Technik-Notizen).
- **Was dafuer alles vorhanden sein muss:** S23 Ultra angeschlossen (R5CW206F0ZM). Beide Geraete auf
  0.17.16 (build 218). Package de.frank.entropyreducer.debug. Sonden-Format:
  `Diag.i(DiagnosticArea.AGENTIC,"GenSuggest","CHECKPOINT Dedup: ideasTotal=.. skipProcessedList=.. skipChain=.. toProcess=..")`.
- **Uncommitteter Arbeitsstand:** KEINER (alles committed bis #46984). Nur fremde untracked Datei
  `EntropieReductor/.../presentation/mental/GewohnheitTtsViewModel.kt` im Working Tree — gehoert NICHT
  zu dieser Aufgabe (war schon bei Session-Start da), NICHT anfassen/committen.
- **Danach:** mit "Naechste Schritte" weiter (Tests 2+3, dann Abschluss-Boxen).

## Aktueller Status
- Erledigt (alle committed+gepusht; auf beide Geraete installiert wo relevant):
  - #46978: Checkpoint-Sonden in GenerateSuggestionsUseCase (Dedup-Skip-Gruende + Origin-Tracking), 0.17.14.
    LIVE VERIFIZIERT auf Fold6: Origin Aufgabe (OpenCodeGo) + Gewohnheit (Blumen), Dedup (toProcess=0).
  - #46981: Backup-Lineage-Fix v18 — BackupTaskSuggestion/BackupMental tragen originId/originType/rootId;
    4 Mapper in SuggestionDataStores.kt; Schema 17->18 (SyncCoordinator+BackupPayload); 0.17.15.
  - #46983: LineageBackfillMigrator — self-root Altbestand (entropy_entries 68/habits 2/mental 8) +
    alle Ideen in beide processedIds-Listen; OriginType.MENTAL; 0.17.16. LIVE VERIFIZIERT auf Fold6
    (0 herkunftslose Eintraege; self-rooted originId=id, originType=TASK/HABIT/MENTAL).
  - #46984: observability-first.md §6 Punkt 5 (Vollabdeckungs-Pflicht + Bestandscode nachruesten),
    nach claude-code-setup/rules/ gespiegelt.
- In Arbeit: FINALER Roundtrip-Dedup-Schritt auf S23 (siehe oben).
- Blockiert: nichts.

## Relevante Dateien
- EntropieReductor/app/src/main/java/de/frank/entropyreducer/domain/usecase/GenerateSuggestionsUseCase.kt — Dedup + Origin + Sonden.
- EntropieReductor/.../data/SuggestionDataStores.kt — 4 Backup/Restore-Mapper (jetzt mit Herkunft).
- EntropieReductor/.../data/remote/drive/BackupPayload.kt — BackupTaskSuggestion/BackupMental v18, version=18.
- EntropieReductor/.../data/remote/drive/SyncCoordinator.kt — version=18.
- EntropieReductor/.../data/local/LineageBackfillMigrator.kt — NEU, Backfill.
- EntropieReductor/.../data/local/dao/{EntropyEntryDao,HabitDao,MentalSentenceDao}.kt — backfillSelfRoot().
- EntropieReductor/.../EntropyReducerApp.kt — backfillIfNeeded() nach den Room-Migratoren.
- ~/_dbtmp/inspect_er.py — DB-Inspektionsskript (Fold6 er.db; S23 nach s23.db gepullt).

## Getroffene Entscheidungen
- Backfill = "Beides" (Frank-Wahl): self-rooting der Altbestand-Endpunkte + alle Ideen als verarbeitet markieren.
- Backup-Endpunkt-Lineage (habits/mental_sentences SELBST) bewusst NICHT im Backup mitgesichert
  (nicht Dedup-relevant) — als Folgeaufgabe notiert (Memory project_entropie_reductor_id_followups).
- Sonden ueber die vorhandene Diag-Fassade (Diag/AGENTIC, Diag/DATABASE), nichts neu erfunden.

## Fehlgeschlagene Ansaetze (WICHTIG — nicht wiederholen)
- Python sqlite3 mit Git-Bash-Pfad `/tmp/er.db` -> Python sah size=0 (Windows: Git-Bash /tmp != Python /tmp).
  LOESUNG: DB nach `~/_dbtmp/` kopieren + `os.path.expanduser('~/_dbtmp/er.db')` in Python.
- Diagnose-JSONL `files/diagnostics/` per run-as -> leer (liegt im EXTERNEN getExternalFilesDir,
  Scoped Storage sperrt ADB-Shell). LOESUNG: Logcat + Room-DB nutzen.
- `adb logcat -s "Diag/AGENTIC"`-Slash-Tag-Filter unzuverlaessig -> stattdessen `adb logcat -d | grep CHECKPOINT`.
- WAL-Header-Byte-Patch (Offset 18/19) war UNNOETIG — DB kam schon im WAL-Modus; size=0 war der Pfad-Bug oben.
- Session-Backup-Heredoc mit `bash -c` brach an Single-Quotes im Text -> Write-Tool nutzen (kein Shell-Parsing).

## Wichtige Fakten / Technik-Notizen
- Geraete: Fold6=RFCX70KTDFX (SM-F956B), S23 Ultra=R5CW206F0ZM (SM-S918B). AKTUELL nur S23 angeschlossen.
- Beide auf 0.17.16 (build 218). DB intern via run-as zugaenglich; kein on-device sqlite3.
- DB-Pull: `adb -s <serial> exec-out run-as de.frank.entropyreducer.debug cat databases/entropy_reducer.db(+-wal/-shm) > ~/_dbtmp/<x>.db` dann python sqlite3 (expanduser). Spalten: task_suggestions hat title (nicht text), habit_suggestions hat text (nicht title).
- Aufgaben liegen in Tabelle `entropy_entries` (hat originId), NICHT `tasks`.
- OFFEN/zu pruefen: Backfill-CHECKPOINT (ERELineageBackfill) erschien auf S23 NICHT im Log nach 8s —
  evtl. lief er vor logcat-clear, async-Verzoegerung, oder Flag schon gesetzt. Kurz verifizieren ob
  S23-Backfill lief (S23 DB: entropy_entries/habits/mental ohne Herkunft sollte 0 sein).

## Naechste Schritte (priorisiert)
1. S23-Dedup-CHECKPOINT auslesen (siehe Wiedereinstiegspunkt) -> Roundtrip-Dedup beweisen.
2. Optional: S23-Backfill verifizieren (entropy_entries/habits/mental ohne Herkunft = 0).
3. Tests 2+3 nachholen (Task #7 Nichts-Fall: Idee->tasks=0 habits=0 trotzdem processed; #8 Accept-Lineage
   per DB: entropy_entries.originId = Quell-Idee) — falls Frank will.
4. Abschluss: task-completion-summary (3 Boxen) + max 2 Intelligenz-Vorschlaege.
5. Spaetere Folgeaufgaben (Memory project_entropie_reductor_id_followups): KI-Aussetzer-Fix (Herkunft
   auch ohne sourceIndex wenn eindeutig), Accept-Live-Sonde, Endpunkt-Lineage ins Backup.
6. Fester naechster Auftrag (Memory project_research_pipeline_and_openrouter_go): OpenRouter-Go-Key
   einrichten + komplette Firecrawl-Recherche-Pipeline aufsetzen, sobald Frank den Key gibt.

## Offene Fragen
- Hat Frank auf dem S23 schon "Aktualisieren" gedrueckt? (Zuerst klaeren, dann Logcat lesen.)

## Memory-Stand (schon geschrieben diese Session)
- project_entropie_reductor_id_followups (Backup-Lecke=GEFIXT, Backfill, KI-Aussetzer, Tests).
- feedback_observability_probes_everywhere (Sonden-Vollabdeckung als Regel).

## Anker
- Branch: main
- Letzte Commits:
53774d90e #46984 - docs(rules): observability-first §6 full-coverage mandate + retrofit legacy
c8e8a5be9 #46983 - feat(EntropieReductor): ID-architecture backfill migrator (self-root + mark ideas processed); 0.17.16
81c699092 #46981 - fix(EntropieReductor): backup-lineage gap v18 (origin survives Drive backup/restore); 0.17.15
0f81fe835 #46982 - feat(bug-almanac-guard): coverage-luecken geschlossen
a0faae8f3 #46980 - feat(almanach-trigger-auswertung): coverage-check
