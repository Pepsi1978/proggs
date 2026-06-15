# Session Handoff — 2026-06-15 (nachmittags)

## Ziel (1-3 Saetze)
Das Bug-Almanach- / Best-Practices- / Aktivierungs- / Intelligenz-System des Harness verbessern:
weniger Bugs im Alltag, das Potenzial der Software maximal in Richtung Best Practices lenken.
Konkret die 3 Wellen aus dem System-Audit abarbeiten (jede committen + verifizieren, Direktive #3).

## Aktueller Status
- Erledigt:
  - System-Audit (5 parallele Agenten) + Web-Recherche als WISSEN persistiert (#46780):
    neu `bugs/claude-tooling/agent-knowledge-system.md` (Fallen) + Gegenstueck-BP
    `best-practices/projekt-code/claude-tooling/best-practices-agent-knowledge-system.md` +
    `bugs/SYSTEM-AUDIT-2026-06-15.md` (Ist-Zustand + 3-Wellen-Backlog) + check-guard-coverage-Allowlist.
  - Davor: 13 Guard-Luecken geschlossen + `bugs/check-guard-coverage.py` Poka-Yoke (#46777).
  - WELLE 1 KOMPLETT:
    - 1a: subagent-context repo->aktiv gespiegelt (lokal, KEIN Commit noetig — Repo war schon korrekt,
      nur die laufende Installation war alt + nutzte das flache Schema). Subagenten erben das System wieder. Verifiziert.
    - 1b: `invariant-check.ps1/.sh` "Invariant 10" = Hook-Drift-Detektor (aktiv<->repo, inhaltsnormalisiert
      utf-8-sig + CRLF->LF + rstrip, KEIN EOL-Fehlalarm). Beide Plattformen, Auto-Heilung aus Inv 5 erhalten (#46785).
    - 1c: `check-coupling.py` Fehlalarm-Fix — has_table prueft jetzt "Bezug"-Ueberschrift ODER Link-Emoji
      statt beides zwingend. 19 -> 5 ECHTE Luecken (#46783).
  - 5 SICHERE Hook-Drifts bereits repo->aktiv gespiegelt (aktiv war reine aeltere Teilmenge):
    bash-guard.ps1/.sh, compound-stagnation-detector.ps1, cross-platform-file-guard.ps1, startup-checks.ps1.
  - BACKUP aller aktiven Hooks (Sicherheitsnetz vor Drift-Aufloesung): `~/.claude/hooks-backup-2026-06-15/` (129 Dateien).
- In Arbeit / OFFEN (naechster Brocken):
  - 27 HEIKLE Hook-Drifts (aktiv<->repo, BEIDE Seiten haben eigene Zeilen -> NICHT blind spiegeln,
    pro Hook einzeln pruefen welche Version richtig ist). Liste unten.
  - WELLE 2 (Kern-Reparatur, noch nicht begonnen): Lernschleife (experience-store/session-scores/Near-Miss)
    reparieren ODER ehrlich abschalten (datentot seit 2026-04-12, nur Platzhalter); session-scorer-Trigger
    pruefen; Harness-Bugs in den Almanach-Kreislauf zwingen; MEMORY.md "Offene Fehler" entruempeln + Auto-Log-Dedup;
    Self-Tests in `bugs/health.py` buendeln; Harness-BP (01-12) erzwingen.
  - WELLE 3 (Strategie/Pflege): Kern-Almanache re-recherchieren (claude-hooks/kotlin/compose/gradle/firebase-billing,
    aelteste); Versions-Anker vereinheitlichen + Staleness-Skript; semantischer "Agent-Requested"-Trigger;
    Memory-Governance (confidence/last_verified/version_anchor); windows-electron-text-injection Guard-Mapping
    (.cs-Content-Probe); check-guard-coverage als SessionStart-Check.
  - 5 ECHTE check-coupling-Luecken (fehlende Bezugstabellen, Welle-3-Pflege): cowork-BP, groq-transkription-BP,
    icon-building (BEIDE Seiten), voice-pipeline-BP, wake-word-BP.
- Blockiert: Reihenfolge-Frage (27 Drifts zuerst vs Welle 2 zuerst) wurde gestellt, vom Benutzer zugunsten
  dieses Backups abgebrochen. Antwort steht noch aus.

## Relevante Dateien
- `bugs/SYSTEM-AUDIT-2026-06-15.md` — ZENTRALE Backlog-Datei (Ist-Zustand + alle 3 Wellen + Befunde). HIER zuerst lesen.
- `bugs/claude-tooling/agent-knowledge-system.md` — Fallen-Almanach (Hook-Schema, Tool-Drift, tote Lernschleife, ...).
- `best-practices/projekt-code/claude-tooling/best-practices-agent-knowledge-system.md` — richtige Bauweise.
- `~/.claude/hooks/invariant-check.ps1` + `.sh` — Drift-Detektor (Invariant 10). Repo-Spiegelung: claude-code-setup/hooks/.
- `bugs/check-coupling.py` + `bugs/check-guard-coverage.py` — die zwei Self-Tests.
- `~/.claude/hooks-backup-2026-06-15/` — Backup ALLER aktiven Hooks vor der Drift-Aufloesung.

## Getroffene Entscheidungen
- Welle-Reihenfolge: 1 (sofort/grosser Hebel) -> 2 (Kern: Lernschleife) -> 3 (Strategie/Pflege).
- subagent-context: Repo-Version war korrekt (nested Schema) -> Richtung repo->aktiv.
- check-coupling: Pruef-Pattern lockern (Ueberschrift "Bezug" ODER Link-Emoji), NICHT alle Tabellen ergaenzen.
- agent-knowledge-system ist Querschnitt -> in check-guard-coverage INTENTIONALLY_UNMAPPED-Allowlist.
- Drift-Detektor: INHALTS-Vergleich (nicht Hash) — sonst EOL/BOM-Fehlalarm.

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- invariant-check aktiv->repo spiegeln waere FALSCH gewesen: die REPO-Version war NEUER (Auto-Heilung Inv 5
  vom 2026-05-30). Loesung: Inv 10 in die REPO-Version einbauen, DANN repo->aktiv. -> Bei Drifts IMMER zuerst
  pruefen welche Seite neuer ist, NIE blind eine Richtung spiegeln.
- Drift-Detektor per reinem sha256/Hash -> Fehlalarm bei jedem Start (CRLF/BOM). Loesung: utf-8-sig + CRLF->LF + rstrip.
- Python-Analyse-Skript das ein Emoji per print ausgibt -> cp1252-Crash auf Windows. Loesung: sys.stdout.reconfigure(encoding="utf-8").
- 27 heikle Drifts NICHT blind repo->aktiv spiegeln (aktiv hat eigene Zeilen -> Funktionsverlust-Risiko, Direktive #3).
- Session-Backup per langem single-quoted Bash-Heredoc -> "unexpected EOF matching quote" (Git Bash, langer Inhalt
  mit vielen Apostrophen). Loesung: Write-Tool statt Heredoc fuer die Notiz, dann nur cp+git per Bash.

## Wichtige Recherche-Ergebnisse (in agent-knowledge-system.md persistiert)
- Lernschleife (experience-store.jsonl/trajectories.jsonl) ist DATENTOT: nur Platzhalter (success_score:3,
  leere tool_sequence), near_miss NIE true; session-scores.jsonl eingefroren seit 2026-04-12 (Scorer feuert nicht).
- Bug-Almanach-Loop dagegen LEBENDIG (6/8 Stichprobe befoerdert). bug-cases.jsonl: 133 Eintraege, alle mit Root-Cause.
- Externe Ideen: Agent Skills 3-Tier Progressive Disclosure; semantischer Agent-Requested-Trigger (Cursor);
  Memory-Governance confidence/last_verified/version_anchor (MemGovern); Staleness-Check version_anchor<->live-Version.

## 27 HEIKLE Hook-Drifts (aktiv<->repo, einzeln pruefen — python3 difflib aktiv vs repo)
config-guard.ps1/.sh (aktiv nur 'sonnet', repo sonnet/opus-Allowlist -> repo PRIO), config-guard-preemptive.ps1/.sh,
bug-almanac-guard.ps1/.sh (repo hat windows-electron-Fix der Parallel-Session), auto-sync.ps1/.sh, heartbeat.ps1,
reindex-codebase.ps1, stopfailure-logger.ps1/.sh, sync-hooks-reference.ps1/.sh, hook-exit0-guard.ps1/.sh,
cascade-format.ps1/.sh, agent-resource-lock.ps1/.sh, poka-yoke-json-validate.ps1/.sh, whiteboard-insert.ps1/.sh,
blunder-scan.ps1, admin-setup.sh, claude-mem-worker-launcher.sh.
Detektor live: pwsh -File ~/.claude/hooks/invariant-check.ps1 | grep HOOK-DRIFT

## Naechste Schritte (priorisiert)
1. Reihenfolge klaeren: 27 Hook-Drifts zuerst (teils kritisch, config-guard) ODER Welle 2 zuerst. (Frage war offen.)
2. Bei Drift-Aufloesung: pro Hook python3 difflib aktiv vs repo, entscheiden welche Version richtig (Tendenz: repo
   neuer, da committed/versioniert; aber aktiv-eigene Zeilen pruefen), gezielt spiegeln, danach Detektor -> 0.
   config-guard prioritaer. Backup unter ~/.claude/hooks-backup-2026-06-15/.
3. Welle 2: zuerst Entscheidung Auto-Log-Pipeline REPARIEREN vs ABSCHALTEN (Benutzer fragen).

## Offene Fragen
- Reihenfolge: 27 Drifts vs Welle 2 zuerst?
- Welle 2: tote Lernschleife reparieren (echte Signale schreiben) oder ehrlich abschalten (leer > irrefuehrend)?

## Anker
- Branch: main
- Letzte Commits:
4cb445137 #46785 - invariant-check: add Invariant 10 hook-drift detector (Welle 1b)
a4cbb8404 #46784 - CVO v2.1.0: Text-Einfuegen in Claude Desktop gefixt (UIA + SendInput-Scancode)
702b8d3de #46783 - check-coupling.py: fix 14 false-positive drift alarms (Welle 1c)
15507e176 #46782 - Cowork git-push: dauerhafte Loesung + Almanach/Best-Practices aus 7-Researcher-Recherche
6958ec070 #46782 - best-practices: add windows-electron-text-injection (C#/WPF -> Claude Desktop)
