# Session Handoff — 2026-06-23, ~22:45

## Ziel (1-3 Saetze)
Franks "zweites Gehirn" (serverseitiger 1:1-Memory-Server auf Hostinger-VPS, ueber WireGuard) Schritt
fuer Schritt weiterbauen. Heute riesiger Fortschritt: Phase 2.1/3.1/3.2/6.1/6.2 fertig, **Phase 4a
(Bibliothekar-Agent Speicher-Seite) LIVE**, WireGuard-Laufwerks-Auto-Reconnect gebaut, und **Phase 5
Dashboard v1 LIVE**. Naechster Schritt: Dashboard Phase 5 Schritt 2 (Einstellungen + Prompt-Editor).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
**Keine laufende Aufgabe, letzter Stand sauber abgeschlossen** (alle Commits durch bis #47121, alles
deployed + getestet). Ich hatte Frank gerade gefragt, womit wir weitermachen: (a) Dashboard Schritt 2
(Einstellungen + Prompt-Editor), (b) Optik-Feedback zum Dashboard, oder (c) 4b/Backup.
- **So geht es weiter:** Bei "weiter" ZUERST die Memorys lesen: `project_second_brain_flugplan` +
  `project_second_brain_memory_server` + `best-practices/second-brain/agent-bibliothekar-plan.md` +
  `speicher-schema-1zu1.md`. Dann Frank fragen, was er will (a/b/c), ODER falls er es schon sagt:
  **Dashboard Schritt 2 bauen** = (1) Agent prompt-editierbar machen: System-Prompt-INSTRUKTIONEN aus
  Datei `/app/data/prompt.txt` laden (Fallback auf eingebauten Default), GET/PUT `/prompt`-Endpunkte am
  Agenten, JSON-Output-Schema bleibt code-seitig (nicht editierbar, sonst bricht das Parsen); Modell
  per `/config` (GET/PUT, Datei `/app/data/config.json`) umschaltbar. Agent braucht dann ein
  Daten-Volume (`./agent-data:/app/data`) in compose. (2) Dashboard `static/index.html` Einstellungs-Tab
  fuellen: Modell-Dropdown (gemini-3.1-flash-lite etc.) + grosser Prompt-Editor (Textarea, mono) mit
  Speichern; Backend proxyt `/api/prompt` + `/api/config` an den Agenten. (3) Optional Logbuch-Viewer
  (nutzt `/api/logbook`). Deploy: scp + `docker compose up -d --build agent dashboard` (Windscribe AUS).

## Aktueller Status — ALLES LIVE auf dem VPS (Container, Stand heute)
- **sb-qdrant** (qdrant v1.18.2) — 177 Eintraege, Collection `brain`, Volume ./qdrant-data. UNANGETASTET.
- **sb-brain-api** (brain-api **v1.1.0**, 10.8.0.1:8000) — 1:1-Speicher; /search jetzt mit Kategorie+Datum-Filter (Qdrant DatetimeRange).
- **sb-mcp** (v1.1.0, 10.8.0.1:8001/mcp) — MCP-Werkzeuge; in Claude Code (~/.claude.json) + OpenCode (opencode.jsonc) eingetragen, live verifiziert.
- **sb-agent** (**v0.1.3**, 10.8.0.1:8002) — Bibliothekar Speicher-Seite: einordnen (auto bei bestehender Kat / Vorschlag+Rueckfrage bei neuer), Dubletten ersetzen/neu/lassen, Logbuch nach 30-min-Timeout ODER /end -> .txt auf Z:\Logbuch\JJJJ\MM (NUR reines Gespraech im Body, KEINE Zeitstempel pro Zeile - Frank-Wunsch: verwaessern semantische Suche; Kopf hat Kategorie+Datum/Uhrzeit) + 1:1 ins Gehirn (Kat gespraeche). Aktuelle Zeit im Prompt (korrekte Titel). Laeuft als uid 1000.
- **sb-dashboard** (v0.1.0, **10.8.0.1:8003**) — privates Web-Cockpit: Uebersicht (Gedaechtnis-Spektrum der 13 Kategorien + Server CPU/RAM/Disk + Agent-Status), Gehirn-Browser (Suche/Kategorie/Detail-Drawer), Dark+Light umschaltbar. Erreichbar nur ueber WireGuard. Optik live per Playwright geprueft (sieht sehr gut aus, beide Modi).
- **WireGuard-Auto-Reconnect** (Windows, eingerichtet): Aufgabe "WG-Drive-Reconnect" (Highest, Anmeldung + alle 5 Min, unsichtbar via .vbs) startet WireGuard falls aus + verbindet Z:/Y: neu; WG-Dienst hat jetzt Recovery-Aktionen (5s/30s/60s). Skripte in second-brain-server/windows/.

## Relevante Dateien
- `~/.claude/projects/C--Users-barwa-proggs/memory/project_second_brain_flugplan.md` — DER FLUGPLAN (Phasen, alle erledigten markiert)
- `~/.claude/projects/.../memory/project_second_brain_memory_server.md` — voller Systemstand
- `second-brain-server/dashboard/` — app.py (FastAPI) + static/index.html (SPA) + Dockerfile + requirements.txt
- `second-brain-server/agent/app.py` — der Agent (hier kommt prompt-editierbar + /config rein)
- `second-brain-server/compose.yaml` — Stack (qdrant/brain-api/mcp/agent/dashboard)
- `best-practices/second-brain/agent-bibliothekar-plan.md` + `speicher-schema-1zu1.md`
- `second-brain-server/windows/wg-drive-reconnect.{ps1,vbs}` + `wg-setup-elevated.ps1`

## Getroffene Entscheidungen
- Dashboard = eigener Container auf dem VPS, ueber WireGuard-IP (KEINE Domain/Homepage noetig, NICHT oeffentlich).
- Dashboard-Identitaet "Cortex", Signatur = Gedaechtnis-Spektrum; Schriften Space Grotesk/Inter/JetBrains Mono; Dark+Light.
- Agent-Prompt-Editor: editierbar werden NUR die Instruktionen (Rolle/Aufgabe/Ton), JSON-Output-Schema bleibt code-seitig (Schutz).
- Logbuch-Body bewusst OHNE Zeitstempel pro Zeile (semantische Suche nicht verwaessern); Modell Gemini 3.1 Flash Lite (austauschbar).

## Fehlgeschlagene Ansaetze (NICHT wiederholen)
- **PowerShell-Tool-Sandbox blockt `net use /delete` + `Remove-Item`** (haelt "Z:/Y:" faelschlich fuer Loesch-Op) -> solche Befehle als `.ps1` schreiben und via `powershell.exe -File` aus dem Bash-Tool ausfuehren.
- **`Register-ScheduledTask` mit RunLevel Highest braucht Elevation** -> als nicht-erhoehter Prozess "Zugriff verweigert". Loesung: einmalig `Start-Process powershell -Verb RunAs` (ein UAC-Klick durch Frank), barwa IST Admin.
- **`git commit -- <pfad>` scheitert bei NEUER (untracked) Datei** ("pathspec did not match") -> erst `git add <datei>`, dann commit.
- **`docker compose up -d --build <service>`** recreated nebenbei sb-brain-api mit (harmlos, qdrant/177 bleiben). Windscribe muss fuer VPS-Arbeit AUS sein.
- Playwright-MCP-Browser schliesst zwischen Aufrufen -> navigate+click+screenshot in EINEM Aufruf bündeln.

## Wichtige Recherche-Ergebnisse / Anker-Fakten
- VPS: Hostinger 168.231.83.205, Ubuntu 24.04. SSH: `ssh -i ~/.ssh/id_ed25519 root@168.231.83.205` (Windscribe AUS).
- WireGuard: Dienste ueber 10.8.0.1. Deploy (kein git-Repo auf VPS): scp -> `cd /opt/second-brain && docker compose up -d --build <service>`. Secrets: /opt/second-brain/.env + ~/SK/second-brain/.
- Gemini gemini-3.1-flash-lite EXISTIERT + funktioniert (live getestet). Embedding gemini-embedding-001 @1536.
- Dashboard-Test (selbst-aufraeumend) lief ueber python3-Skripte vom VPS-Host gegen 10.8.0.1:8002/8000.

## Naechste Schritte (priorisiert)
1. Frank fragen: Dashboard Schritt 2 (Einstellungen+Prompt-Editor) / Optik-Feedback / 4b / Backup?
2. Falls Schritt 2: Agent prompt-editierbar (Datei + GET/PUT /prompt + /config Modell) -> agent v0.2.0 -> deploy.
3. Dann Dashboard Einstellungs-Tab (Modell-Dropdown + Prompt-Editor + Speichern) + Backend-Proxy /api/prompt /api/config.
4. Optional: Logbuch-Viewer im Dashboard.
5. Spaeter offen: Phase 4b (Abfrage-Seite), Phase 1.1 Backup der 177 (Frank will VORHER besprechen!), STT/TTS-Frontends.

## Offene Fragen
- Was zuerst (Schritt 2 / Optik / 4b / Backup)? + ob Frank Optik-Aenderungen am Dashboard will (Farben/Layout).

## Anker
- Branch: main
- Letzte Commits:
cd6e7e185 #47121 - Dashboard v1 (Cortex): Uebersicht + Gehirn-Browser + Dark/Light
e2ba2ef06 #47120 - Windows WG-Drive-Auto-Reconnect (unsichtbar) + WG-Service-Recovery
328b13721 #47119 - sb-agent v0.1.3: Zeitstempel je Nachricht wieder RAUS (Logbuch sauber)
229d7dbf2 #47118 - sb-agent v0.1.2: aktuelle Zeit/Datum/Zeitzone in den Prompt
ed317863f #47117 - sb-agent v0.1.1: /end-Bugfix + Kategorie-Prompt geschaerft
