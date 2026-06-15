# Session Handoff — 2026-06-15 (spaet)

## Ziel (1-3 Saetze)
Harness-/Bug-Almanach-/Intelligenz-System verbessern (Welle 3 + Folgeaufgaben). ALLE beauftragten
Aufgaben dieser Session sind ERLEDIGT + committed (#46802-46812). Offen ist NUR der geplante Endpunkt:
die 2 NEUEN Update-Skills (Almanach-Welle + Best-Practices-Welle, Cowork-tauglich) bauen.

## Aktueller Status — ALLES committed auf origin/main (#46802-46812)
- #46802 Aufgabe A: claude-hooks-Almanach + best-practices/01-hooks auf Claude Code v2.1.177 re-recherchiert
  (7 Researcher, Issue-Status HART per gh verifiziert). Restliche 4 Welle-3-Almanache (kotlin/compose/gradle/
  firebase-billing) NICHT manuell gemacht -> umgewidmet zu den Update-Skills.
- #46803 Spec fuer die 2 Update-Skills gesichert: bugs/UPDATE-SKILLS-SPEC.md (komplette Bauanleitung).
- #46804/#46805 Aufgabe B: 6 check-coupling-Luecken geschlossen + Folge-Luecke (guard-coverage cowork-git-push) gefixt.
- #46806 Aufgabe C: BOM-Root-Cause = session-guard.ps1 schrieb settings.json mit [System.Text.Encoding]::UTF8
  (=UTF8Encoding(true)=BOM); alle 4 Stellen + redact-settings-reference.ps1 -> (New-Object System.Text.UTF8Encoding $false).
  Empirisch bewiesen, in claude-hooks-Almanach 12.1 + bug-cases dokumentiert.
- #46807 Aufgabe D: generisches Dedup in whiteboard-insert.{ps1,sh} (timestamp-invarianter Key, KEIN top-level exit, beide getestet).
- #46808 W3-1: check-version-anchor.py (in health.py) + strukturiertes `> **Anker:** <label>=<version>`-Feld in 10 software-Almanachen + SYSTEM.md §7.
- #46809 W3-2: semantischer Prompt-Trigger bug-almanac-hint (UserPromptSubmit, py-Logik + ps1/sh-Wrapper, einmalig/Bereich/Session, passiv). 3-Dateien-Settings + SYSTEM.md-Schicht 1b.
- #46810 W3-3: claude-code-setup/tools/memory-staleness.py (read-only) + Governance-Konvention (meta-memory). KEIN 114-Datei-Umbau.
- #46811 W3-4: Agent-Skills als Digest-Traeger EVALUIERT -> Empfehlung NICHT umbauen (keine Erzwingung, Budget). SYSTEM.md §10.
- #46812 W3-5: bestehende Skills bug-almanach-recherche (Schritt 6.3-6.5) + best-practices (Schritt 5) + known-bugs-Regel (Schicht 1b) + UPDATE-SKILLS-SPEC an das neue W3-System angepasst (Frank's Hinweis!).

## Relevante Dateien
- `bugs/UPDATE-SKILLS-SPEC.md` — DIE Bauanleitung fuer die 2 offenen Skills (inkl. W3-Mechanismen + gh-Pflicht).
- `bugs/health.py` — 4 Checks (coupling, guard-coverage, version-anchor, Stand-Verfall). Fuer MEINE Arbeit gruen.
- `bugs/check-version-anchor.py`, `~/.claude/hooks/bug-almanac-hint.{py,ps1,sh}`, `claude-code-setup/tools/memory-staleness.py`.

## Getroffene Entscheidungen
- Welle-3-Re-Recherche wird kuenftig per Skill in Claude COWORK gemacht (bessere Limits), NICHT manuell im CLI.
- W3-3 schlank (Staleness-Tool) statt 114-Datei-Migration (Frank-Wahl). W3-4 reine Eval.

## Fehlgeschlagene Ansaetze / WICHTIG (nicht wiederholen)
- python-Befehle via Bash mit `$HOME`/`/c/Users/...`-Pfad -> FileNotFoundError. IMMER os.path.expanduser ODER C:/Users/... .
- settings.json NICHT per Edit-Tool (triggert Formatter/BOM) und NICHT per json.dump (reformatiert TAB->Spaces).
  Stattdessen: gezielter Python-STRING-Replace (utf-8-sig lesen, utf-8 ohne BOM schreiben) + json.loads-Validierung.
- PARALLELE CVO-Session arbeitet AKTIV an cowork: hat cowork-scheduled-tasks.md (neuer Almanach) + best-practices-cowork.md
  angefasst -> 2 health-WARNs (cowork-Drift + cowork-scheduled-tasks-Luecke) gehoeren IHR. NICHT anfassen (sie macht
  ihre coupling/guard-Eintraege selbst). README.md/SYSTEM.md wurden von ihr mit-modifiziert.
- claude-config + claude-hooks + python-windows Almanach-VOLLTEXTE in dieser Session schon gelesen (Bereiche frei).

## Naechste Schritte (priorisiert)
1. Die 2 Update-Skills bauen — via skill-creator, Anleitung in bugs/UPDATE-SKILLS-SPEC.md. ZUERST der
   Almanach-Update-Skill (Prioritaet), dann der Best-Practices-Update-Skill. Sehr ausfuehrlich. Danach in
   Cowork-Logik umwandeln (cowork-git.sh push-files, ~45s-Shell-Limit, Mount-Fallen) und an Frank schicken.
2. Optional (Intelligenz-Vorschlag, Frank-Zustimmung offen): MEMORY.md-Groessenwaechter (SessionStart-Check),
   da MEMORY.md mit 25.8 KB ueber dem 24.4-KB-Ladelimit ist (nur teilweise geladen).

## Offene Fragen
- Frank-Antwort offen: Update-Skills in frischer Session (empfohlen) ODER direkt? + MEMORY.md-Waechter ja/nein?

## Anker
- Branch: main
- Letzte Commits:
  #46812 W3-5 align existing skills+rule with new W3 logic
  #46811 W3-4 evaluate Agent Skills as native digest carrier -> NOT convert
  #46810 W3-3 memory-staleness.py + governance convention
  #46809 W3-2 semantic prompt-trigger bug-almanac-hint
  #46808 W3-1 version-anchor check + Anker fields
