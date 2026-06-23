# Session Handoff — 2026-06-24, ~00:15

## Ziel (1-3 Saetze)
Franks "zweites Gehirn" (serverseitiger 1:1-Memory-Server auf Hostinger-VPS, ueber WireGuard) Schritt
fuer Schritt weiterbauen. Heute: Designer-Dashboard 1:1 uebernommen + deployed, Einstellungen-Tab
funktional gemacht (Prompt-Editor + Modell-Wahl), und den Speicher-System-Prompt gehaertet (v0.2.1).

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
**Keine laufende Aufgabe — letzter Stand sauber abgeschlossen** (alles committed bis #47128, alles auf
dem VPS deployed + live getestet). Frank hat fuer heute Schluss gemacht.

**>> MORGEN (Frank-Ansage beim Backup): An dem Prompt weiterarbeiten — und zwar die ANTWORT-SEITE
(Phase 4b, der Abruf-/Nachschlage-Teil). <<**

## NAECHSTER SCHRITT (morgen) — Phase 4b: Antwort-Seite, KONZEPT IST BESPROCHEN
Frank will EINE natuerliche Unterhaltung mit dem Agenten — mal etwas ablegen, mal etwas abrufen, im
selben Gespraech. Wichtiger Einwand von Frank (und meine REVIDIERTE Empfehlung): NICHT zwei getrennte
Chats (das wuerde die natuerliche Unterhaltung brechen!), sondern:
- **EIN Gespraechs-Eingang (`/chat`), intern zwei "Koepfe":** ein Ablege-Kopf (1:1 speichern, nie
  umschreiben/erfinden) und ein Nachschlage-Kopf (nur aus echten Treffern antworten, nie erfinden).
- Der Agent entscheidet PRO Nachricht selbst, welcher Kopf dran ist. Frank merkt davon nichts.
- Umsetzung: **vierter Modus `recall`** neben store/ask/smalltalk. Erkennt der Agent eine Wissensfrage
  -> action=recall + Suchbegriff -> der SERVER macht die Vektorsuche im Gehirn (brain-api /search,
  gefiltert/semantisch) -> ZWEITER LLM-Aufruf mit einem GETRENNTEN Abruf-Prompt + den Treffern ->
  Antwort NUR aus den Treffern; passt nichts -> ehrlich "dazu habe ich nichts gefunden".
- Die Trennung ist also bei den PROMPTS/Logik-Pfaden (Ablegen vs. Abrufen getrennt), nicht bei den
  Chat-Endpunkten. Read-only-Sicherheit fuer den Abruf-Pfad beibehalten.
- Der Ehrlichkeitsschutz (heute eingebaut) ist die Bruecke: bis 4b steht, sagt der smalltalk-Zweig
  ehrlich "kann noch nicht abrufen". Mit 4b wird daraus echtes Abrufen.

**So geht es morgen los:** Phase 4b kurz durchplanen (recall-Modus im Speicher-Prompt-Schema ergaenzen
ODER separater Abruf-Prompt; brain-api /search-Anbindung; zweiter LLM-Aufruf; Abruf-Prompt mit "nur aus
Treffern"). Dann in agent/app.py umsetzen, deployen, mit echter Frage live testen (Cleanup: agent
restart verwirft Test-Sessions; brain-Eintragszahl muss 177 bleiben).

## Aktueller Status — ALLES LIVE auf dem VPS (Stand heute)
- **sb-qdrant** (qdrant v1.18.2) — 177 Eintraege, Collection `brain`, Volume ./qdrant-data. UNANGETASTET.
- **sb-brain-api** (v1.1.0, 10.8.0.1:8000) — 1:1-Speicher; /search mit Kategorie+Datum-Filter.
- **sb-mcp** (v1.1.0, 10.8.0.1:8001/mcp) — MCP-Werkzeuge (in Claude Code + OpenCode eingetragen).
- **sb-agent** (**v0.2.1**, 10.8.0.1:8002) — Bibliothekar SPEICHER-Seite, jetzt:
  - System-Prompt editierbar/speicherbar (GET/PUT /prompt) + Modell umschaltbar (GET/PUT /config),
    Datei-Persistenz unter /app/data (compose-Volume ./agent-data, gehoert uid 1000). Modellwechsel
    sofort aktiv ohne Neustart.
  - Prompt GEHAERTET: echte Umlaute (+ Anweisung title/reply mit echten Umlauten), Injection-Schutz,
    Ehrlichkeitsschutz bei Wissensfragen (kein Halluzinieren - LIVE getestet: Wissensfrage -> smalltalk
    + ehrliche Antwort), expliziter Feld-Kontrakt pro Aktion, 6 Few-shot-Beispiele (alle valides JSON),
    Kategorie-Schluessel-Format. JSON-Schema bleibt code-seitig GESCHUETZT (nicht editierbar).
  - Prompt-Aufbau: build_system_prompt() = DEFAULT_INSTRUCTIONS (editierbar, {kategorien}-Marker) +
    Leerzeile + SCHEMA_BLOCK (geschuetzt). load_instructions() liest prompt.txt oder Default.
  - Modi aktuell: store / ask / smalltalk. (Morgen kommt recall dazu.)
- **sb-dashboard** (**v0.2.1**, 10.8.0.1:8003) — "Cortex": Designer-Frontend 1:1 (kosmisches Theme,
  Sternenfeld, animiertes Gedaechtnis-Spektrum). Uebersicht + Gehirn-Browser + Einstellungen-Tab
  FUNKTIONAL (Modell-Dropdown + Speicher-Button, System-Prompt laden/speichern/zuruecksetzen).
  Backend-Proxy /api/prompt + /api/config an den Agenten. Server-Speicher/Disk in MB (Frontend erwartet MB).

## Relevante Dateien
- `second-brain-server/agent/app.py` — der Agent (HIER kommt morgen der recall-Modus/Abruf rein).
  DEFAULT_INSTRUCTIONS + SCHEMA_BLOCK (Prompt), build_system_prompt(), llm_decide() (LLM-Aufruf mit
  cand_txt=Dubletten-Kandidaten + response_mime_type=application/json), /chat-Endpunkt.
- `second-brain-server/dashboard/app.py` + `static/index.html` — Dashboard (Cortex), Einstellungen-Tab.
- `second-brain-server/compose.yaml` — Stack (agent hat Volume ./agent-data:/app/data).
- `second-brain-server/dashboard/Cortex Web-Cockpit/` — Designer-Handoff (README + Screenshot, Referenz).
- Memory: `project_second_brain_flugplan` + `project_second_brain_memory_server` (Systemstand/Anker).

## Getroffene Entscheidungen (heute)
- Designer-Dashboard wird 1:1 uebernommen (Frank-Wunsch). Backend an Frontend-Vertraege angepasst (Byte->MB).
- Einstellungen-Editor: NUR Instruktionen editierbar; JSON-Schema + Kategorienliste geschuetzt (sonst bricht Parsen).
- Prompt-Haertung nach Opus-4.8-Analyse: Beispiele/Feld-Kontrakt/Injection/Umlaute/Ehrlichkeit eingebaut.
  WICHTIG: Zwei Opus-Kritikpunkte waren bei uns SCHON geloest (JSON-Zaeune via response_mime_type;
  Dubletten-Einspielung via cand_txt im user_block) — Opus sah nur den Prompt, nicht den Server-Code.
- **Architektur 4b (revidiert nach Frank-Einwand): ein Gespraechs-Eingang, intern zwei Koepfe (recall-Modus).**

## Fehlgeschlagene Ansaetze / Fallen (NICHT wiederholen)
- **Verschachtelter Python-Heredoc ueber ssh** zerschiesst Backslash-Escapes -> SyntaxError. Loesung:
  Test-Skript lokal schreiben, per scp hochladen, dann python3 /tmp/x.py ausfuehren.
- **Bash-Heredoc mit vielen Apostrophen** ('store'/'recall') kann im Bash-Tool an Quoting scheitern
  ("unexpected EOF") -> fuer grosse Notizen mit Sonderzeichen das Write-Tool nutzen (Datei vorher rm,
  damit kein Read-Zwang). So wurde DIESES Backup geschrieben.
- **Frontend erwartet MB**, psutil liefert Bytes -> ohne Umrechnung falsche Speicheranzeige. (Gefixt.)
- **Playwright-Screenshot-Selektor** .set-section traf 3 Elemente (strict mode) -> Viewport-Screenshot ohne target.
- **Test-/chat erzeugt In-Memory-Session** (-> nach 30min Logbuch). Cleanup: docker restart sb-agent
  verwirft Sessions; bei versehentlichem store: brain-api forget + Eintragszahl 177 pruefen.
- docker compose up -d --build <service> recreated nebenbei sb-brain-api mit (harmlos, qdrant/177 bleibt).
- Playwright-MCP-Browser: navigate/click/screenshot kurz nacheinander, Browser bleibt offen.

## Wichtige Recherche-/Anker-Fakten
- VPS: Hostinger 168.231.83.205, Ubuntu 24.04. SSH: ssh -i ~/.ssh/id_ed25519 root@168.231.83.205
- Dienste ueber WireGuard 10.8.0.1 (PC erreicht sie nur mit aktivem Tunnel; SSH zur oeff. IP geht immer).
- Deploy (kein git-Repo auf VPS): scp Datei -> cd /opt/second-brain && docker compose up -d --build <service>
  Secrets: /opt/second-brain/.env (SB_API_KEY/GEMINI_API_KEY/QDRANT_API_KEY). agent-data gehoert uid 1000.
- Modell Gemini gemini-3.1-flash-lite (austauschbar im Dashboard). Embedding gemini-embedding-001 @1536.
- /chat braucht Bearer SB_API_KEY. Dashboard-Proxies (/api/...) brauchen vom Client KEINEN Bearer.

## Naechste Schritte (priorisiert)
1. **Phase 4b: Antwort-Seite** (recall-Modus, Server-Vektorsuche, zweiter LLM-Aufruf, Abruf-Prompt "nur
   aus Treffern", read-only). EIN Gespraechs-Eingang, intern zwei Koepfe. (Franks Hauptwunsch fuer morgen.)
2. Danach offen: geschuetzten Prompt-Teil read-only im Editor anzeigen; Logbuch-Viewer anbinden;
   Backup der 177 (Phase 1.1, VORHER mit Frank besprechen!); Favicon gegen favicon-404.

## Offene Fragen
- Recall: eigener Abruf-Prompt als zweite editierbare Datei (zweiter Editor im Dashboard) ODER fest?
  (Tendenz: editierbar wie der Speicher-Prompt, damit Frank auch den Antwort-Ton steuern kann.)

## Anker
- Branch: main
- Letzte Commits:
cc46d1a81 #47128 - sb-agent v0.2.1: Prompt-Haertung (Umlaute/Injection/Ehrlichkeit/Feld-Kontrakt/Beispiele)
2a28fef34 #47127 - Dashboard Schritt 2: Einstellungen-Tab funktional (Prompt-Editor + Modell-Wahl)
c2e8e6dfb #47126 - Dashboard (Cortex) v0.2.0: Designer-Frontend 1:1 + Byte->MB
7654901b8 #47125 - Dashboard: kompletter Design-Prompt fuer Designagenten
9d2f46fb7 #47124 - session restore: clear handoff backup
