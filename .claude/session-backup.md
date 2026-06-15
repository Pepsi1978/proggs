# Session Handoff — 2026-06-15 (abends)

## Ziel (1-3 Saetze)
Harness-/Almanach-/Intelligenz-System verbessern. WELLE 1 + WELLE 2 sind KOMPLETT.
Jetzt offen: 4 Folge-Aufgaben (Welle 3 + Pflege), die der Benutzer in dieser Reihenfolge
beauftragt hat: (A) Welle-3-Almanach-Recherche, (B) 5 coupling-Luecken, (C) BOM-Root-Cause, (D) whiteboard-insert-Dedup.

## Aktueller Status
- WELLE 1 KOMPLETT (frueher): 27 Hook-Drifts aufgeloest (#46788) + whiteboard-insert top-level-exit-0-Lib-Bug (claude-hooks 13.7).
- WELLE 2 KOMPLETT (diese Session, alle auf origin):
  - #46792 learning-data: 64 tote Platzhalter (tool_count:0) aus experience-store/trajectories entfernt.
  - #46793 session-scores: experience-logger.py zieht jetzt ECHTE Signale aus dem Transcript
    (tool_count/tool_sequence/error_count/duration/turns/session_id) + schreibt experience-store +
    trajectories + session-scores (ein Logger, drei Outputs). .ps1/.sh sind duenne Wrapper (stdin->python,
    Fallback neuestes Transcript). session-scorer.ts aus SessionEnd DEREGISTRIERT (aktive settings + 4 Repo-Kopien).
  - #46794 harness-bug cycle: poka-yoke-json-validate.{ps1,sh} strippt jetzt BOM aus Claude-Config-JSON
    (settings/.mcp/.claude/claude-code-setup) + fix des '--'-argv-Bugs (python3 -c "code" -- $path -> '--' wird argv[1]).
  - #46795 bugs/health.py: buendelt check-coupling + check-guard-coverage + Stand-Verfall; laeuft im
    SessionStart via invariant-check.{ps1,sh}. Meldet die 5 offenen coupling-Luecken.
  - #46799 whiteboard: 71 Auto-Log-Spam-Zeilen aus 'Offene Fehler' entfernt (80+ -> 9 echte Eintraege).
- Working Tree: fremde untracked/modified Dateien von parallelen Cowork/CVO-Sessions (NICHT anfassen).
  Gerettete fremde Dateien liegen in %LOCALAPPDATA%/Temp/rebase-rescue/ (gehoeren parallelen Sessions).

## Relevante Dateien
- `~/.claude/hooks/experience-logger.py` (+.ps1/.sh Wrapper) — der reparierte Logger (Welle 2).
- `bugs/health.py` — gebuendelter Self-Test; `bugs/check-coupling.py` zeigt die 5 Luecken (Detail).
- `~/.claude/hooks/poka-yoke-json-validate.{ps1,sh}` — BOM-Stripper (Aufgabe C-Schutz greift schon).
- `~/.claude/hooks/whiteboard-insert.{ps1,sh}` — DOT-SOURCED BIBLIOTHEK (KEIN top-level exit!) — Ziel von Aufgabe D.
- `bugs/SYSTEM-AUDIT-2026-06-15.md` — Welle-3-Backlog. `bugs/SYSTEM.md` — Almanach-System.

## Getroffene Entscheidungen
- Lernschleife: Hybrid-Reparatur (Logger zieht selbst aus Transcript) statt Scorer-Reparatur — vom Benutzer gewaehlt.
- BOM-Bug: Root-Cause-unabhaengiges Poka-Yoke (Stripper) statt am ungewissen Einfueger zu raten.

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- python -c via Bash mit `/c/Users/...`-Pfad -> FileNotFoundError (Windows-python versteht Git-Bash-Pfade NICHT).
  IMMER `os.path.expanduser` ODER Windows-Pfad `C:/Users/...` in python-Code. (Mehrfach getappt!)
- MEMORY.md-Rewrite in GETRENNTEN Schritten (edit, spaeter add) -> paralleler whiteboard-insert-Hook ueberschreibt
  dazwischen -> Aenderung VERLOREN. IMMER atomar: `python edit.py && git add MEMORY.md && git commit --only MEMORY.md`
  in EINEM Bash-Befehl, dann diff-stat verifizieren (muss -N zeigen).
- `git commit` OHNE pathspec bei parallelen Sessions -> fremde gestagte Dateien (cowork) rutschen mit rein
  (passierte in #46793). IMMER `git commit -- <eigene pfade>`.
- BOM-Einfueger NICHT isoliert: biome AUSGESCHLOSSEN (Test: biome format --write schreibt BOM-frei + Spaces, nicht Tabs).
  config-guard schreibt settings.json (ConvertTo-Json|Out-File) aber macht Spaces nicht Tabs. Quelle weiter offen.
- Almanach-Recherche NICHT ad hoc selbst machen -> Skill `bug-almanach-recherche` ODER `best-practices` ist der
  vorgeschriebene Weg (sonst fehlen Fix-Status, BP-Abgleich, Hook-Mapping). Frank's OK liegt vor (er hat es beauftragt).
- Rebase-Bloecke durch untracked Dateien paralleler Sessions: untracked Blocker nach TEMP mv, dann rebase.

## Naechste Schritte (priorisiert — 4 Aufgaben, Benutzer-Reihenfolge)
1. AUFGABE A (Welle 3): die 5 aeltesten Kern-Almanache RE-RECHERCHIEREN: claude-hooks (06-01), kotlin,
   compose, gradle, firebase-billing (06-02). Pro Almanach den Skill `bug-almanach-recherche` ODER
   `best-practices` starten (Researcher-Schwarm, 7 parallel; NIE Workflow). Findings in best-practices/ + bugs/
   einarbeiten (Kurzcheck + Volltext, research-persistence-Regel). GROSS -> braucht frischen Kontext.
2. AUFGABE B: 5 echte check-coupling-Luecken schliessen (`python bugs/check-coupling.py` zeigt Details):
   cowork-BP, groq-transkription-BP, icon-building (BEIDE Seiten), voice-pipeline-BP, wake-word-BP.
   Jede braucht die wechselseitige Bezugstabelle (Marker '🔗' + 'Bezug') in Almanach UND Best-Practices-Datei.
3. AUFGABE C: BOM-Formatter-Root-Cause isolieren. Methode: TEMP-Kopie settings.json, einzelne PostToolUse-Edit-Hooks
   (auto-format/config-guard/cross-platform-file-guard) isoliert triggern, BOM nach jedem pruefen. Poka-Yoke greift schon.
4. AUFGABE D: generisches Dedup in whiteboard-insert.{ps1,sh} (identischer Eintrag gleiche Quelle+Symptom
   innerhalb X h -> nicht erneut schreiben). VORSICHT: Bibliothek, KEIN top-level exit, Cross-Platform, Repo-Sync.

## Offene Fragen
- Keine offenen Rueckfragen. Bei Aufgabe A ggf. pro Almanach kurz bestaetigen vor dem Researcher-Schwarm.

## Anker
- Branch: main
- Letzte Commits (Welle 2):
  #46799 whiteboard: declutter 'Offene Fehler' (Welle 2)
  #46795 bugs/health.py: bundle self-tests, run on SessionStart (Welle 2)
  #46794 harness-bug cycle: BOM poka-yoke + fix poka-yoke-json-validate --argv bug (Welle 2)
  #46793 session-scores: write from experience-logger, retire session-scorer.ts (Welle 2)
  #46792 learning-data: purge 64 dead placeholder entries (Welle 2)
