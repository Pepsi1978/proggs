# Session Handoff — 2026-07-05, 15:30 Uhr

## Ziel (1-3 Saetze)
Cortex/Second-Brain-Ausbau. Diese Session hat GRUPPE D ("Mitlernen in Programmier-Sessions",
Plan-Nr. 27-33) KOMPLETT gebaut, deployt und end-to-end verifiziert — plus eine wichtige
Frank-Korrektur zur Versions-Sichtbarkeit (Dashboard-Footer = Gesamt-Versionszaehler).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen. Alles committed (#47511-#47535),
gepusht und auf dem VPS deployt+verifiziert. Es sind nur noch OFFENE FRAGEN an Frank unbeantwortet
(siehe "Naechste Schritte"). Vollstaendiger Projekt-Kontext: Memory project_gruppe_d_session_mitlernen.md
(wird automatisch geladen).

## Aktueller Status
- GRUPPE D komplett live (agent 0.53.0, librarian 0.8.0, dashboard 0.46.0, SessionEnd-Hook):
  - D27/D32: SessionEnd-Hook ~/.claude/hooks/session-brain-summary.py (+.ps1/.sh) sammelt beim
    Session-Ende Prompts+Commits+Dateien (Secrets redaktiert) -> agent POST /session-log ->
    verdichtet per LLM -> Eintrag "Session <CLI> <Projekt> — YYYY-MM-DD HH:MM" [Programmierung/Sessions]
    mit Episoden-Auszug. Fail-open, Log ~/.claude/logs/session-brain-summary.log. Schema-Canary.
  - D28: Kern-Block "Woran Frank gerade baut" [Programmierung/Kern-Bloecke] wird automatisch nachgezogen.
  - D31: Projektstand-Recall ("Woran habe ich zuletzt gearbeitet?") deterministisch erkannt -> aus
    Session-Protokollen + Kern-Block chronologisch beantwortet. LIVE getestet, funktioniert.
  - D29: Regel ~/.claude/rules/session-learning-to-brain.md (Entscheidungs-Rueckfluss, Vorschlags-Prinzip).
  - D30: OpenCode AGENTS.md Regel 9 (advisory, kein Hook).
  - D33: Reibungs-Detektor als 8. Bibliothekar-Nachtaufgabe (librarian _task_friction).
- E2E verifiziert: Hook manuell gefeuert -> echtes Session-Protokoll + Kern-Block liegen im Gehirn;
  "Woran habe ich zuletzt gearbeitet?" kam mit Quellen zurueck.
- VERSIONS-KORREKTUR (Frank-Betonung): Dashboard-Version ist der EINE Gesamt-Zaehler des Servers;
  bei JEDER Dienst-Aenderung hochzaehlen + echter Timestamp. Heute versaeumt (agent/librarian
  deployt, Footer blieb 0.45.0/12.13) -> nachgeholt auf 0.46.0 (15.13 Uhr), DEPLOY.md-Regel
  gehaertet (librarian explizit + Vorfall dokumentiert).
- In Arbeit: nichts (von mir). Blockiert: nichts.

## Relevante Dateien
- second-brain-server/agent/app.py (0.53.0) — POST /session-log, Kern-Block-Pflege, projektstand_question/_projektstand_recall
- second-brain-server/librarian/app.py (0.8.0) — _task_friction (D33), STANDARD_TASKS["friction"]
- second-brain-server/dashboard/app.py (0.46.0) — Footer-Version = Server-Gesamtzaehler
- ~/.claude/hooks/session-brain-summary.{py,ps1,sh} — SessionEnd-Hook (Spiegel: claude-code-setup/hooks + Umgebung/Hooks)
- ~/.claude/rules/session-learning-to-brain.md — D29-Regel (Spiegel claude-code-setup/rules)
- ~/.config/opencode/AGENTS.md Regel 9 — D30 (Spiegel opencode-setup/AGENTS-global.md)
- second-brain-server/DEPLOY.md — Versions-Bump-Regel gehaertet
- second-brain-server/LEVEL2-FEATURES-PLAN.md — Status-Tabelle (B/D/E fertig, A/C/F/G offen)

## Getroffene Entscheidungen
- Session-Verdichtung laeuft ASYNCHRON im Hintergrund (agent antwortet sofort; Session-Ende am PC wartet nie aufs LLM).
- Projektstand-Erkennung DETERMINISTISCH im Code (Poka-Yoke wie 0.51.2), nicht im Router-LLM.
- D32 als verdichtete Episode IM Session-Eintrag (Prompts-Auszug), NICHT als separates Voll-Transkript-Archiv.
- Dashboard-Version = Server-Gesamtzaehler (nicht Einzeldienst-Version).

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- Dashboard-Version NICHT gebumpt beim agent/librarian-Deploy -> Footer blieb 0.45.0, Frank dachte
  nichts sei angekommen. NIE WIEDER: bei JEDEM Dienst-Deploy dashboard-Version hoch + echter Timestamp + dashboard mit-deployen.
- features.json ist ins Dashboard-IMAGE gebacken: scp allein erreicht den Container NICHT ->
  IMMER "compose up -d --build dashboard" (Sonde bewiesen: Host 42 vs Container 40).
- Git-Bash-Pfad (/c/Users/...) im Hook: Python kann ihn nicht oeffnen -> _norm_path normalisiert auf C:/... (python-windows §3.1).
- Timestamps NIE schaetzen: immer date/Get-Date (Frank-Regel).

## Wichtige Recherche-Ergebnisse
- Claude Code SessionEnd feuert bei reasons: clear/resume/logout/prompt_input_exit/
  bypass_permissions_disabled/other (offizielle Doku). ABER: hartes Fenster-Zuklicken/Absturz
  feuert es NICHT zuverlaessig -> diese Sitzung wuerde dann nicht protokolliert.
- OpenCode hat KEIN technisches Session-Ende-Signal -> das Modell muss die AGENTS.md-Regel selbst
  befolgen (advisory, unzuverlaessiger als der Claude-Hook).

## Naechste Schritte (priorisiert) — alles OFFENE FRAGEN an Frank (noch unbeantwortet)
1. OpenCode-Plugin bauen, damit OpenCode genauso zuverlaessig mitschreibt wie der Claude-Hook
   (erst pruefen, ob OpenCode ein Session-Ende-Event fuer ein Plugin bereitstellt).
2. Haertung gegen hartes Fenster-Schliessen: beim NAECHSTEN Session-Start pruefen, ob die vorige
   Sitzung protokolliert wurde -> durchgerutschte automatisch nachholen.
3. GRUPPE A (Plan 1-10, Gedaechtnis-Architektur: Kurzzeit-Schicht + Kern-Bloecke) — das eigentliche
   Fundament, auf dem C/Proaktivitaet aufbaut. Empfohlener naechster grosser Block.
4. Offene Intelligenz-Vorschlaege: (a) SESSION_LOG_SYSTEM-Prompt-Hinweis "Commits koennen parallele
   Arbeit enthalten"; (b) Docker-features.json-Image-Falle in docker-Almanach + DEPLOY.md.
5. Aus Vorsession offen: 15 Bibliothekar-Funde abarbeiten; ersten 04:10-Lauf pruefen (jetzt auch
   erster Reibungs-Detektor-Durchlauf).
6. Plan-Gruppen C (19-26 Proaktivitaet), F (40-46 Frontends), G (47-50 Sprache) — spaeter.

## Offene Fragen
- Frank soll waehlen: OpenCode-Plugin (1) / Fenster-Schliessen-Haertung (2) / Gruppe A (3) als Naechstes?
- Die 2 Intelligenz-Vorschlaege (Schritt 4) hat Frank noch nicht beantwortet.

## Anker
- Branch: main
- Letzte Commits (ACHTUNG: #47536-#47539 sind PARALLELE Session = librarian 0.9.0 AI-Ueberschriften
  fuer gelernte Regeln + dashboard 0.24.3; NICHT meine Gruppe-D-Arbeit. Meine: #47511-#47535):
06144b022 #47539 - Revert superfluous dashboard touch (parallele Session)
60b3c1d83 #47538 - Fill provider status header area (parallele Session)
db565a888 #47537 - dashboard 0.24.3 learned-rule headline (parallele Session)
e08dfaea6 #47535 - dashboard 0.46.0: Footer = Server-Gesamtzaehler (MEINE)
b9bd352ef #47536 - librarian 0.9.0 AI-Ueberschriften (parallele Session)
