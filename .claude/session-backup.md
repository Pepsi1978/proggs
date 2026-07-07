# Session Handoff — 07.07.2026, ~13:20 Uhr (Server-Berlin-Zeit; lokale Windows-Uhr ging ~2h nach)

## Ziel (1-3 Saetze)
Cortex (Second-Brain-Server) verbessern. Frank hatte einen Chat, bei dem der Agent den Gedaechtnis-
Kontext nicht fand (Star-Trek-Fall). Nach Diagnose + Logbuch (Schritt 1, FERTIG) folgt jetzt
**Schritt 2: der Agenten-Umbau** — Router-LLM und Leseagent als eigene Agenten abschaffen, der
GPT-5.5-Hauptagent macht alles selbst per Tool-Calling. Frank-Priorität: **Korrektheit vor Tempo.**

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen (alles committed + gepusht + deployed).
Das Backup wurde BEWUSST gemacht, um mit frischem Kontext in Schritt 2 (Agenten-Umbau) zu starten.

**Allererste Aktion der neuen Session:** Die Datei `~/proggs/second-brain-server/CORTEX-AGENT-UMBAU-PLAN.md`
VOLLSTAENDIG lesen — sie enthaelt ALLES (alle Probleme mit Log-Beweisen, den kompletten Umbau-Plan,
die Design-Entscheidungen, die technischen Anker in app.py). Danach mit Frank den Umbau starten
(Plan §6 Punkt 1: erst pruefen, ob der GPT/Codex-Pfad in app.py schon Tool-Calling unterstuetzt).

## Aktueller Status
- Erledigt: Schritt 1 = zwei Logbuecher (Turn-Logbuch + Sonden-Trace) gebaut, deployed, live
  verifiziert. Commit #47586 (agent 0.60.0 + dashboard 0.56.0, deployed 07.07. 13:09). Handoff-Plan
  + Memory = Commit #47587.
- In Arbeit: nichts.
- Blockiert: nichts. Schritt 2 (Umbau) ist der naechste, noch nicht begonnen.

## Relevante Dateien
- `second-brain-server/CORTEX-AGENT-UMBAU-PLAN.md` — DAS Uebergabe-Dokument, ZUERST lesen (alle Details).
- `second-brain-server/agent/app.py` (355 KB — NIE komplett lesen, gezielt greppen) — der Agent-Code.
- `second-brain-server/DEPLOY.md` — Deploy-Weg (scp + docker compose up -d --build; SSH-Key ~/SK/second-brain/id_ed25519).
- Memory `project_cortex_agent_umbau` (wird beim Session-Start geladen) — Kurzfassung + Pointer.

## Getroffene Entscheidungen
- Router-LLM + Leseagent (als eigene Agenten) ABSCHAFFEN; Hauptagent (GPT-5.5, High-Thinking) macht
  alles per Tool-Calling. Schreibagent BLEIBT (spezialisiert).
- Funktionen NICHT loeschen, sondern VERLAGERN (Direktive #3): Kontext-Schutz (Leseagent-Grund:
  18k-Eintraege) -> Werkzeug "erst Snippets, dann gezielt Volltext"; Speicher-Sicherheit -> harte
  Code-Regel; deterministische Weichen -> schlanker Preflight.
- Zwei Logbuecher nach GRANULARITAET getrennt (grob-lesbar vs. fein-technisch), verknuepft per turn_id.
- Feine Sonden erst MIT dem Umbau (Stale-Probe-Schutz), nicht vorher in den umzubauenden Code.
- Regelpool zweistufig (Kandidat -> Frank bestaetigt -> aktiv), Limit ~30-40.

## Fehlgeschlagene Ansaetze / WICHTIGE KORREKTUREN
- Die urspruengliche Chat-Diagnose "Gedaechtnissuche lief nie" war FALSCH. Ebenso "Qdrant fand nichts".
  ECHTE Root Cause (per Log bewiesen + von ChatGPT unabhaengig bestaetigt): der **Leseagent waehlte
  0 von 50 gefundenen Treffern** aus. NICHT erneut in die falsche Richtung diagnostizieren.
- Bei der ersten Analyse UEBERSEHEN (nur ChatGPT fand es): `agent.jsonl` (forensisches Voll-Log
  /app/logs/agent.jsonl) hat KEIN Volume -> geht bei jedem Rebuild verloren. MUSS noch gefixt werden
  (compose.yaml agent-Dienst `- ./agent-logs:/app/logs` + chown 1000:1000). Siehe Plan §6 Punkt 8.

## Wichtige Recherche-Ergebnisse
- Star-Trek-Log (06.07. 19:19 UTC): route=query_internet, 50 Rohtreffer, Leseagent 0 gewaehlt,
  confidence=keine. Turn 2: 50 Rohtreffer, 10 gewaehlt, confidence=hoch.
- GPT-5.5 High-Thinking ist erstklassig im Tool-Calling -> Umbau realistisch.
- ai-agent-Almanach fuer den Tool-Loop: Hard-Stop (max Turns/Zeit) Pflicht, tool_use<->tool_result
  strikt 1:1, Tool-Fehler als tool_result zurueck (nicht crashen), Schreib-Tools idempotent.
- Server-Uhr (VPS, NTP) ist maszgeblich; lokale Windows-Uhr ging ~2h nach -> Timestamps per `ssh ... date`.

## Naechste Schritte (priorisiert)
1. `CORTEX-AGENT-UMBAU-PLAN.md` lesen, dann mit Frank Schritt 2 starten.
2. Pruefen, ob GPT/Codex-Pfad in app.py Tool-Calling kann (codex_generate); Ziel-Architektur festzurren.
3. Werkzeuge + Hauptagent-Loop (mit Hard-Stop), Router-LLM + Leseagent entfernen (Funktionen verlagern).
4. Regelpool, Hauptagent-Logbuch-Zugriff, feine Sonden, Evil-Test (Star-Trek-Regression).
5. Offener Fix: agent.jsonl-Volume (compose.yaml).

## Offene Fragen
- Frank fragen: Windows-Zeitsync anstoszen (`w32tm /resync`)? (lokale Uhr 2h daneben — noch offen)
- Vor Code an app.py: Almanach-Kurzchecks lesen (ai-agent, fastapi, docker) + Best-Practices.

## Anker
- Branch: main
- Letzte Commits:
f5c8e09c5 #47587 - Cortex agent rebuild plan + all diagnosed problems (session handoff)
11efe1fb3 #47586 - Cortex agent 0.60.0 + dashboard 0.56.0: Turn-Logbuch + Sonden-Trace with bottleneck timing
1a3cf66c6 #47585 - Fix Cortex context prompt height
fa7efc576 #47584 - Expand Cortex context prompt display
0a4cae21b #47584 - Remove Cortex agent price panel
