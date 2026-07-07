# Session Handoff — 07.07.2026, ~16:10 Uhr (Server-Berlin-Zeit; lokale Windows-Uhr geht ~2h nach)

## Ziel (1-3 Saetze)
Cortex (Second-Brain-Server) Agenten-Umbau SCHRITT 2: Router-LLM + Leseagent (als eigene Agenten) abschaffen,
der GPT-5.5-Hauptagent macht alles selbst per Tool-Calling (Werkzeugkasten). Frank-Prioritaet: **Korrektheit vor Tempo.**
Das Fundament (Tool-Loop-Motor + Werkzeugkasten) ist FERTIG+VERIFIZIERT; der Einbau in den echten Chat (_process_turn)
sowie eine features.json-Redaktion stehen noch aus.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine halbfertigen Edits — ALLES ist committed+gepusht+deployed (letzter Commit #47601). Das Backup wurde bewusst wegen
extrem langer Session gemacht, um mit frischem Kontext weiterzumachen. Die ZULETZT von Frank gestartete (noch NICHT
begonnene) Aufgabe ist die **features.json-Redaktion** (siehe Naechste Schritte Punkt 1).

## Aktueller Status
FERTIG + VERIFIZIERT (Commits #47590-#47601, agent 0.63.0 + dashboard 0.57.2, alles live):
- Tool-Loop-Motor `codex_generate_tools()` in agent/app.py — GPT/Codex Function-Calling-Loop, gruen getestet (/toolloop-selftest ok).
- Werkzeugkasten `build_agent_tools()` (agent/app.py) mit 5 Lese-Werkzeugen: durchsuche_gedaechtnis, lade_eintrag,
  web_suche, lies_logbuch, was_kann_cortex. Isoliert getestet via `GET /toolagent-test?q=...`:
  Star-Trek-Fall GRUEN (Hauptagent fand 50 Eintraege, nutzte Franks Notizen -> alter Bug geloest);
  was_kann_cortex GRUEN (liest live die System-Info-Chronik).
- agent.jsonl-Volume-Fix (compose.yaml `- ./agent-logs:/app/logs`, Host-Ordner uid 1000) — forensisches Log persistent.
- features-Chronik-Merge-Bugfix (dashboard 0.57.1): /api/features zeigte neue Deploy-Eintraege nicht (persistente
  dashboard-data-Kopie hatte Vorrang, Seed nur 1x kopiert). _read_features_data merged jetzt fehlende Seed-Eintraege.
  Verifiziert (count 53->55). Bugfix ist im Second Brain dokumentiert (bugfixes/cortex-dashboard).
- was_kann_cortex-Werkzeug (agent 0.63.0): Hauptagent liest live die Feature-Liste vom Dashboard (DASHBOARD_URL/api/features).

NOCH NICHT im echten Chat aktiv: Der laufende Cortex-Chat nutzt weiter den ALTEN Weg (Router->Leseagent->Antwort).
Die Werkzeuge existieren nur im Test-Endpoint /toolagent-test. NICHTS am echten Chat wurde veraendert.

## Relevante Dateien
- `second-brain-server/CORTEX-AGENT-UMBAU-PLAN.md` — Uebergabe-Plan (Stand bis 15:12; enthaelt Tool-Loop-Wissen +
  Werkzeug-Blaupause + Zeilen-Anker). NACH dem Restore ggf. mit dem Nachmittags-Fortschritt aktualisieren.
- `second-brain-server/agent/app.py` (>380 KB, NIE komplett lesen — gezielt greppen): codex_generate_tools,
  build_agent_tools, TOOLAGENT_SYSTEM, /toolagent-test, /toolloop-selftest; _process_turn ab Z.~4999.
- `second-brain-server/dashboard/app.py`: _read_features_data (Merge-Fix), VERSION.
- `second-brain-server/dashboard/features.json` — die 55 "Was kann Cortex"-Eintraege (Ziel der Redaktion, Punkt 1).
- `second-brain-server/DEPLOY.md` — Deploy-Weg (scp + docker compose up -d --build; SSH ~/SK/second-brain/id_ed25519).
- Memory `project_cortex_agent_umbau` (Session-Start geladen).

## Getroffene Entscheidungen
- SICHERER INKREMENTELLER _process_turn-Umbau: NUR die Antwort-Zweige (query/query_internet/internet/smalltalk,
  Z.~5169-5271) durch EINEN codex_generate_tools-Aufruf ersetzen. Router (hauptagent_route Z.5016) UND die komplette
  Speicher-Sicherheit (explicit_save Z.5011, pending/confirm/replace/store_clarify Z.5048-5097, save-Rueckfrage
  Z.5101-5141, projektstand/category-Sonderfaelle Z.5151-5167) BLEIBEN UNANGETASTET. So loest man den Star-Trek-Bug im
  echten Chat OHNE Franks Daten-Sicherheit zu riskieren. Router-LLM ganz entfernen = SPAETERER, separater Schritt.
- Werkzeug-Handler kapseln bestehende Funktionen (nicht neu erfinden). durchsuche_gedaechtnis fuellt hit_cache (Snippets),
  lade_eintrag holt Volltext daraus (Kontext-Schutz = Leseagent-Filter verlagert in den Hauptagenten).
- features-Eintraege sollen GENERELL (Dauer-Regel) in einfachem, leicht verstaendlichem Deutsch (keine Fremdwoerter),
  kurz aber detailliert, mit EINFACHER Ueberschrift sein — fuer Frank UND fuer was_kann_cortex (der Hauptagent).

## Hart erarbeitetes TOOL-LOOP-WISSEN (unbedingt beachten — sonst Fallen erneut)
- Backend `chatgpt.com/backend-api/codex`. `stream:true` PFLICHT (stream:false -> HTTP 400).
- function_calls kommen als STREAM-EVENTS (`response.output_item.done` mit dem vollstaendigen Item), NICHT in
  `completed.output` (das bleibt leer!). Wer nur completed.output liest, sieht KEINE Tools.
- Backend bricht Chunked-Stream sporadisch ab ("incomplete chunked read") -> Streaming-Retry ist eingebaut (noetig).
- Tool-Format: `{"type":"function","name","description","parameters":{...,additionalProperties:false}}`, tool_choice:"auto".
- Tool-Timing: codex_generate_tools loggt je Werkzeug `ms` in tool_calls -> Flaschenhals-Radar (kommt ins Turn-Logbuch,
  sobald der Tool-Loop im _process_turn aktiv ist — Frank-Wunsch).

## Fehlgeschlagene Ansaetze (WICHTIG — nicht wiederholen)
- stream:false am Codex-Backend -> HTTP 400. IMMER streamen.
- function_calls aus completed.output lesen -> leer. Aus den output_item.done-Stream-Events lesen.
- Ersten Fehlbefund "Tools werden nicht aufgerufen" NICHT als Backend-Limit missdeuten — es war ein Parsing-Bug.
- Dashboard-Version-Bump NICHT vergessen: bei JEDEM Deploy dashboard/app.py VERSION + Timestamp bumpen UND dashboard
  mit-deployen (sonst sieht Frank den alten Footer). features.json neues Feature = Eintrag ergaenzen (DEPLOY.md-Pflicht).
- Session-Backup: Heredoc mit langem Text schlug fehl (Quoting) — stattdessen Write-Tool nutzen.

## Naechste Schritte (priorisiert — ALLE offenen Aufgaben, Frank-Wunsch: alle merken)
1. **features.json-REDAKTION (zuletzt von Frank gestartet):** ALLE 55 Eintraege in `dashboard/features.json`:
   (a) `name` (Ueberschrift) EINFACHER, weniger professionell — auf einen Blick erkennbar, was es ist.
   (b) `kurz` + `erklaerung` in einfaches, leicht verstaendliches Deutsch, KEINE Fremdwoerter; nicht zu lang
       (der Hauptagent/was_kann_cortex soll nicht zu viel einlesen), aber trotzdem detailliert.
   (c) ALLE Eintraege auf AKTUALITAET pruefen — viele sind laengst ueberarbeitet -> ggf. LOESCHEN (mit Frank abstimmen,
       welche geloescht werden). (d) Bei reiner Textaenderung die alten `eingebaut`-Timestamps BEHALTEN.
   (e) Diese Sprach-/Kuerze-/Ueberschriften-Regel GENERELL fuer ZUKUENFTIGE Eintraege festhalten (Dauer-Regel; ggf. in
       DEPLOY.md features-Pflicht-Block + features.json "hinweis" ergaenzen). Danach dashboard neu deployen + Version bump.
2. **_process_turn-Umbau (Task #7, der Hauptschritt):** Werkzeugkasten in den echten Chat einhaengen — SICHERER
   INKREMENTELLER ANSATZ (siehe Getroffene Entscheidungen): Antwort-Zweige (query/query_internet/internet/smalltalk)
   durch codex_generate_tools(TOOLAGENT_SYSTEM, user_text, tools/handlers aus build_agent_tools(), on_delta) ersetzen;
   Rueckgabe {reply, action, pending:None, sources aus state["hits"], confidence}; Router + Speicher-Sicherheit UNBERUEHRT.
   Timing ins Turn-Logbuch (Logbuch 1) einhaengen. Vorher/Nachher mit /toolagent-test vergleichen.
3. **Schreib-Werkzeuge (speichere, schreibe_regel)** mit der Preflight-Bestaetigung bauen (Speicher-Sicherheit = harte
   Code-Regel, nie ins LLM-Ermessen).
4. **Regelpool (Task #8):** zweistufig (Kandidat -> Frank bestaetigt -> aktiv), Limit ~30-40, agent-data-Datei,
   lies_regeln/schreibe_regel-Werkzeuge.
5. **Feine Sonden in Logbuch 2 (Task #8)** an den neuen Tool-Grenzen (Stale-Probe-Schutz: erst mit dem Umbau).
6. **Evil-Test (Task #8):** Star-Trek-Regression + "Danke" != speichern in die Eval-Suite.
7. **Router-LLM ganz entfernen (spaeterer, separater Schritt):** Hauptagent entscheidet auch save-vs-frage selbst
   (per speichere-Werkzeug). Riskant (Speicher-Sicherheit) -> mit voller Sorgfalt + frischem Kontext.
8. **Diagnose-Reste aufraeumen:** raw_first_output/seen_events in codex_generate_tools, /toolloop-selftest — beim
   finalen Umbau entfernen ODER als Sonde in Logbuch 2 einhaengen. /toolagent-test kann als Health-Check bleiben.

## Offene Fragen
- features.json: welche der 55 Eintraege sind veraltet und sollen GELOESCHT werden? (mit Frank durchgehen).
- Reasoning-Level des Hauptagenten: aktuell "medium"; Frank wollte "High-Thinking" — stellt NUR Frank ein
  (feedback_reasoning_levels_frank_only). Nicht automatisch aendern.

## Anker
- Branch: main
- Letzte Commits:
#47601 - Cortex agent 0.63.0: was_kann_cortex tool + dashboard 0.57.2 visible bump
#47600 - Cortex dashboard 0.57.1: FIX system-info chronicle ignored new seed entries
#47599 - Cortex dashboard 0.57.0: visible version bump + features.json entry for hauptagent toolkit
#47598 - Cortex agent 0.62.0: hauptagent toolkit + /toolagent-test - not yet in chat
#47597 - Cortex agent rebuild: document step-2 progress + tool-loop learnings + toolkit blueprint
