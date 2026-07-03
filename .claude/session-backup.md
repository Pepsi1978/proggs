# Session Handoff — 2026-07-03, ca. 12:40 Uhr

## Ziel (1-3 Saetze)
Das seit langem nervende Problem "Claude-/OpenCode-Verknuepfung oeffnet nach Windows-Neustart nur ein kurz aufblitzendes Fenster, erst nach 5-6 Klicks startet das CLI" wurde nach Direktive #3 tiefgruendig diagnostiziert und robust gefixt (Commit #47423). Frank startet den Rechner jetzt neu, um den Fix im echten Kaltstart-Fall zu testen. Diese Session-Fortsetzung begleitet den Test und schliesst die Doku ab.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt (WICHTIGSTER ABSCHNITT)
Keine unterbrochene Code-Aufgabe — der Fix ist KOMPLETT umgesetzt, getestet (Syntax + Normalfall + simulierter Fehlerfall) und gepusht. Offen ist NUR der echte Kaltstart-Test durch Frank:
- **Welche Aufgabe lief gerade:** Warten auf Franks Neustart-Test des Launcher-Fixes.
- **So geht es EXAKT weiter (allererste Aktion der neuen Session):** Frank sagt nach dem Neustart, ob die Verknuepfung mit EINEM Klick funktioniert hat.
  - **JA, funktioniert:** (1) Bugfix ins Second Brain speichern via second-brain-MCP `remember`: Titel `Bugfix Launcher CLI-Start Kaltstart-Race 2026-07-03 HH:MM` (lokale Uhrzeit), Kategorie `bugfixes/<passende Unterkategorie>` (erst mit `list_memories` pruefen ob eine passt, sonst `bugfixes/windows-launcher` neu). Inhalt nach dem festen Bugfix-Format (Symptom/Root Cause/Fix/Verifikation/Funktionalitaets-Diff/Poka-Yoke). (2) Die 2 offenen Intelligenz-Vorschlaege anbieten: (a) Boot-Zombie-Praeventiv-Waechter als Autostart, (b) `claude-code-setup/launcher/` in die harness-mirror-Regel aufnehmen.
  - **NEIN, geht immer noch nicht:** Stufe D! `bugs/claude-tooling/claude-code-desktop-vs-cli.md` §O1 im VOLLTEXT lesen, dann `C:\Users\barwa\start-claude-wt.log` auswerten — die neuen `[robust]`-Zeilen zeigen pro Versuch: Zombie-Cleanup, andocken (-w 0) vs. -w new, ob/wann das innere pwsh erschien und ob es die 3-s-Stabilitaet ueberlebte, ob der conhost-Fallback griff. Damit ist die naechste Diagnose-Ebene datengetrieben, nicht geraten.
- **Was dafuer alles vorhanden sein muss:** Alles liegt auf Platte + Repo (siehe Relevante Dateien). Claude-Memory-Trigger existiert: `project_wt_kaltstart_launcher_fix.md`.

## Aktueller Status
- Erledigt (#47423, gepusht): Root Cause bewiesen (WT-Monarch-Kaltstart-Race; Log-Beweis: 5 Klicks in 14 s = 5 verschiedene WT-PIDs, kein Crash im Event-Log; `windowingBehavior: useAnyExisting` machte den alten "frisches Fenster"-Fallback wirkungslos; TVO ist entlastet = nur Zeuge). Neue zentrale Robust-Funktion `Start-WtCliRobust` in `C:\Users\barwa\start-wt-common.ps1`; alle 4 Launcher (`start-{claude,opencode,codex,gemini}-wt.ps1`) umgestellt; Repo-Spiegel `claude-code-setup/launcher/` (inkl. README); Almanach §O1 + Kurzcheck-Zeile 16 in `bugs/claude-tooling/claude-code-desktop-vs-cli.md`; bug-cases.jsonl-Eintrag. Memory-Index kompaktiert (198→124 Zeilen, 77 Eintraege verlustfrei nach `MEMORY-ARCHIV.md`).
- In Arbeit: nichts.
- Blockiert: Endverifikation des Fixes — geht nur durch Franks echten Windows-Neustart.

## Relevante Dateien
- `C:\Users\barwa\start-wt-common.ps1` — Kern des Fixes: Zombie-Cleanup + Nach-Start-Verifikation (CIM Win32_Process, CommandLine-Match + CreationDate) + Auto-Retry (ab Versuch 2 `-w new`) + conhost-Fallback
- `C:\Users\barwa\start-{claude,opencode,codex,gemini}-wt.ps1` — die 4 Launcher, dot-sourcen die common-Datei
- `C:\Users\barwa\start-claude-wt.log` (+ je CLI eigenes Log) — Diagnose; `[robust]`-Zeilen = neue Schicht
- `~/proggs/claude-code-setup/launcher/` — Repo-Spiegel aller 5 Skripte + README
- `~/proggs/bugs/claude-tooling/claude-code-desktop-vs-cli.md` — §O1 = volle Fall-Doku
- `~/.claude/projects/C--Users-barwa-proggs/memory/project_wt_kaltstart_launcher_fix.md` — Trigger-Memory mit gleicher Anleitung

## Getroffene Entscheidungen
- Architekturwechsel "pruefen statt raten": Vorab-Heuristiken koennen Monarch-Gesundheit prinzipiell nicht erkennen (2 Fix-Generationen 06/2026 gescheitert) → Verifikation NACH dem Start + Auto-Retry + Fallback.
- Gemeinsame Bibliothek statt 4x kopierter Logik (Poka-Yoke: die Geschwister-Launcher koennen nie wieder auf altem Stand zurueckbleiben).
- WT-Setting `windowingBehavior: useAnyExisting` bewusst NICHT angetastet (Funktionserhalt: Frank will Tabs im bestehenden Fenster); Retries erzwingen Neues-Fenster per `-w new` (ueberschreibt das Setting nur im Retry-Fall).
- Second-Brain-Eintrag erst NACH Franks Bestaetigung (Regel: nie unbestaetigte Bugfixes speichern).

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- Vorab-Check "laeuft ein WT-Prozess?" (Fix 2026-06-11) — gescheitert: Prozess-Existenz sagt nichts ueber Monarch-Gesundheit.
- Vorab-Check "MainWindowHandle != 0?" (Fix 2026-06-24) — gescheitert: sterbender Prozess kann gueltiges Fensterhandle haben; ausserdem dockte der "frisches Fenster"-Zweig (ohne -w) wegen useAnyExisting trotzdem an. NICHT wieder mit Vorab-Heuristiken versuchen.
- TVO als Taeter verdaechtigen — widerlegt: TVO killt beim Systemstart nichts (Kill-Stellen nur in rebuild-/update-Skripten); Mikrofonknopf erscheint/verschwindet nur mit dem Terminal-Fenster.

## Wichtige Recherche-Ergebnisse
- Log-Beweis 2026-07-03 12:11 (`start-claude-wt.log`): WT-PID-Kette 25412→44972→36184→28036→15304 bei 5 Klicks; ab erstem gesundem Fenster (12:11:50) klappt jedes Andocken bis zum naechsten Reboot.
- Franks Beobachtung "schneller Doppelklick hilft" = manueller Retry: Klick 1 reisst den Zombie mit, Klick 2 gewinnt die Monarch-Neuwahl.
- Kein WT-Crash im Windows-Ereignisprotokoll — Fenster schliessen "regulaer" nach gescheitertem Handshake.

## Naechste Schritte (priorisiert)
1. Franks Neustart-Testergebnis entgegennehmen → JA-Pfad oder NEIN-Pfad (siehe Wiedereinstiegspunkt oben).
2. Bei JA: Second-Brain-Eintrag + Frage nach den 2 Intelligenz-Vorschlaegen (Boot-Zombie-Waechter, harness-mirror-Ergaenzung).
3. Unabhaengig davon offen: Platte 99% voll (nur 10,5 GB frei) und Semantic-Search node_modules fehlen (`cd ~/proggs/mcp-code-search && bun install`).

## Offene Fragen
- Hat der Ein-Klick-Start nach dem Neustart funktioniert? (Nur Frank kann das beantworten.)

## Anker
- Branch: main
- Letzte Commits:
bdd1e19ea #47425 - Fix subtab bottom bar build
557fb6c60 #47424 - Refresh mental anchor TTS
8c62f5106 #47423 - Fix WT cold-start race: robust CLI launchers (verify+retry+fallback), almanac O1, bug-case
f889fe5a8 #47412 - CortexAndroid fix: new/empty categories missing from dashboard + category picker (app 0.3.1)
05cf329ca #47422 - Update xAI Grok API Almanach
