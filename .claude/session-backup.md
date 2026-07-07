# Session Handoff — 07.07.2026, 20:45 Uhr (Server-Berlin-Zeit; lokale Windows-Uhr geht ~2h nach)

## Ziel (1-3 Saetze)
Cortex (Second-Brain-Server) **Agenten-Umbau Schritt 2**: Router-LLM + Leseagent (eigene Hilfs-Agenten)
abschaffen — der GPT-5.5-Hauptagent macht alles selbst per Tool-Calling (Werkzeugkasten). Frank-Prioritaet:
**Korrektheit vor Tempo.** Das Fundament (Tool-Loop-Motor + 5 Lese-Werkzeuge) ist FERTIG+VERIFIZIERT; der
eigentliche Einbau in den echten Chat (_process_turn) steht noch aus — das ist der grosse offene Bereich.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
**Keine laufende/unterbrochene Aufgabe** — die zuletzt bearbeitete Aufgabe (features.json-Redaktion) ist
KOMPLETT fertig, deployed und verifiziert (Commits #47604/#47605, dashboard 0.58.0 live, 32 Eintraege).
Nichts Halbfertiges im Working Tree. Das Backup dient dazu, mit frischem Kontext die NAECHSTEN offenen
Aufgaben anzugehen (siehe unten). Naechster grosser Schritt = **_process_turn-Umbau** (Punkt 2 der Liste).

## Aktueller Status
**HEUTE FERTIG (diese Session, alles live):**
- **features.json-Redaktion KOMPLETT** (dashboard 0.58.0, #47604): Feature-Chronik "Was Cortex kann" von
  55 auf 32 Eintraege ueberarbeitet. Alle Eintraege in leichtes, verstaendliches Deutsch — aber alle
  Fachbegriffe/Namen/Zahlen (BM25, RRF, Qdrant, ENV-Variablen, Endpunkte) ERHALTEN + je kurz erklaert
  (Frank-Korrektur: Technik NICHT wegwerfen, nur Sprache vereinfachen). Aehnliche/redundante zusammengefasst
  (Bibliothekar 11->5, Qdrant-Limits 3->1, Such-Verfahren 3->1, Gespraeche 3->1, Kategorien 3->1). Datei
  ~36% kleiner. Deployed + live geprueft (/api/features == 32, Footer 0.58.0). Gemacht mit 4 Sonnet-5-
  Subagenten (Frank-Wunsch: Sonnet fuer token-schwere Arbeit).
- **DEPLOY.md ergaenzt** (#47605): Schreibweise-Dauerregel fuer Chronik-Eintraege (leichtes Deutsch +
  Technik erhalten + zusammenfassen erlaubt) + Merge-Falle dokumentiert. Gleiche Regel im features.json-
  `hinweis`. Merge-Fall auch im Gehirn (bugfixes/cortex-dashboard).

**FERTIG frueher (Commits #47590-#47601, agent 0.63.0):** Tool-Loop-Motor `codex_generate_tools()`
(grün, /toolloop-selftest ok); Werkzeugkasten `build_agent_tools()` mit 5 Lese-Werkzeugen
(durchsuche_gedaechtnis, lade_eintrag, web_suche, lies_logbuch, was_kann_cortex), isoliert getestet via
`GET /toolagent-test?q=...` (Star-Trek-Fall GRUEN: Hauptagent fand 50 Eintraege).

**NOCH NICHT im echten Chat aktiv:** Der laufende Cortex-Chat nutzt weiter den ALTEN Weg
(Router -> Leseagent -> Antwort). Die 5 Werkzeuge existieren nur im Test-Endpoint /toolagent-test.

## Relevante Dateien
- `second-brain-server/agent/app.py` (>380 KB, NIE komplett lesen — gezielt greppen): codex_generate_tools,
  build_agent_tools, TOOLAGENT_SYSTEM, /toolagent-test, /toolloop-selftest; _process_turn ab Z.~4999.
  Anker fuer den Umbau: hauptagent_route Z.~5016, explicit_save Z.~5011, pending/confirm/replace/store_clarify
  Z.~5048-5097, save-Rueckfrage Z.~5101-5141, projektstand/category Z.~5151-5167, Antwort-Zweige
  (query/query_internet/internet/smalltalk) Z.~5169-5271.
- `second-brain-server/CORTEX-AGENT-UMBAU-PLAN.md` — Volltext-Uebergabeplan (Tool-Loop-Wissen + Werkzeug-
  Blaupause + Zeilen-Anker). Ggf. mit heutigem Stand aktualisieren.
- `second-brain-server/DEPLOY.md` — Deploy-Weg: scp nach /opt/second-brain/<dienst>/ + `docker compose up
  -d --build <dienst>`; VPS 168.231.83.205, SSH-Key ~/SK/second-brain/id_ed25519. KEIN git auf dem VPS.
- `second-brain-server/dashboard/app.py` (VERSION Z.31 = aktive; 0.58.0), `dashboard/features.json` (32).
- `second-brain-server/compose.yaml` — Dienste + Volumes (agent, dashboard, brain-api, mcp, librarian, qdrant, caddy).
- Memory `project_cortex_agent_umbau` (operative Notiz, Session-Start geladen).

## OFFENE AUFGABEN (priorisiert, konkret — Frank-Wunsch: ALLE festhalten)
1. **agent.jsonl-Volume-Fix — NOCH OFFEN (Frank: "der Fix muss noch gemacht werden, den hat man noch nicht
   gemacht").** Das forensische Voll-Logbuch des Agenten (agent.jsonl) soll dauerhaft persistent sein
   (compose.yaml Volume `- ./agent-logs:/app/logs`, Host-Ordner uid 1000). Laut Memory
   `project_cortex_agent_umbau` hat ChatGPT den Fix gefunden, er ist aber noch NICHT umgesetzt/deployed.
   ZUERST klaeren: was genau ist der Fix (ChatGPT-Fund nachlesen), dann umsetzen + deployen + verifizieren
   (agent.jsonl ueberlebt Rebuild). Frank haelt das fuer offen — bestaetigen und erledigen.
2. **_process_turn-Umbau (Task #7, DER Hauptschritt — "der ganze Bereich muss umgebaut werden"):**
   Werkzeugkasten in den echten Chat einhaengen. SICHERER INKREMENTELLER ANSATZ (bewusste Entscheidung):
   NUR die Antwort-Zweige (query/query_internet/internet/smalltalk, Z.~5169-5271) durch EINEN
   `codex_generate_tools(TOOLAGENT_SYSTEM, user_text, tools/handlers aus build_agent_tools(), on_delta)`-Aufruf
   ersetzen. Rueckgabe {reply, action, pending:None, sources aus state["hits"], confidence}. Router
   (hauptagent_route) UND die komplette Speicher-Sicherheit (explicit_save, pending/confirm/replace/
   store_clarify, save-Rueckfrage, projektstand/category) BLEIBEN UNANGETASTET. So loest man den Star-Trek-Bug
   im echten Chat OHNE Franks Daten-Sicherheit zu riskieren. Timing ins Turn-Logbuch (Logbuch 1). Vorher/
   Nachher mit /toolagent-test vergleichen. (Router-LLM ganz entfernen = spaeterer Schritt, siehe 7.)
3. **Schreib-Werkzeuge (speichere, schreibe_regel)** mit Preflight-Bestaetigung bauen — Speicher-Sicherheit
   ist harte CODE-Regel, nie ins LLM-Ermessen legen.
4. **Regelpool (Task #8):** zweistufig (Kandidat -> Frank bestaetigt -> aktiv), Limit ~30-40, agent-data-Datei,
   Werkzeuge lies_regeln/schreibe_regel.
5. **Feine Sonden in Logbuch 2 (Task #8)** an den neuen Tool-Grenzen (Stale-Probe-Schutz: erst MIT dem Umbau
   einbauen, nicht vorher).
6. **Evil-Test (Task #8):** Star-Trek-Regression + "Danke" != speichern in die Eval-Suite aufnehmen.
7. **Router-LLM ganz entfernen (spaeterer, separater Schritt):** Hauptagent entscheidet auch save-vs-frage
   selbst (per speichere-Werkzeug). Riskant fuer die Speicher-Sicherheit -> mit voller Sorgfalt + frischem Kontext.
8. **Diagnose-Reste aufraeumen:** raw_first_output/seen_events in codex_generate_tools, /toolloop-selftest —
   beim finalen Umbau entfernen ODER als Sonde in Logbuch 2 einhaengen. /toolagent-test kann als Health-Check bleiben.

**Zusaetzliche offene Punkte (heute entstanden / Vorschlaege):**
- Deploy-Guard gegen die features.json-Merge-Falle: nach jedem Deploy /api/features-Anzahl gegen den Seed
  vergleichen und warnen (verhindert das "68 statt 32"-Problem kuenftig automatisch).
- "was_kann_cortex" live testen: Hauptagenten "was kann Cortex alles?" fragen, pruefen ob die neue kompakte
  32er-Chronik gut zusammengefasst wird.
- Chronik thematisch gruppieren (Suche/Speicher/Bibliothekar/Sicherheit) statt rein chronologisch (optional).
- features.json Router/Leseagent-Eintraege (agenten-architektur, turn-logbuch, internet-suche, such-verfahren
  nennt Router) nach dem fertigen Umbau anpassen/entfernen — aktuell bewusst mit Umbau-Hinweis behalten.

## Getroffene Entscheidungen
- INKREMENTELLER _process_turn-Umbau (nur Antwort-Zweige ersetzen, Router+Speicher-Sicherheit unberuehrt) —
  Router-LLM ganz entfernen erst spaeter separat.
- Werkzeug-Handler kapseln bestehende Funktionen (nicht neu erfinden). durchsuche_gedaechtnis fuellt hit_cache
  (Snippets), lade_eintrag holt Volltext daraus (Leseagent-Filter -> in den Hauptagenten verlagert).
- features.json-Eintraege GENERELL: leichtes Deutsch, ABER alle Fachbegriffe/Namen/Zahlen behalten + kurz
  erklaeren (nie durch Weglassen vereinfachen); kompakt; aehnliche zusammenfassen; Datum sekundaer. (In
  DEPLOY.md + features.json-hinweis verankert.)

## Hart erarbeitetes TOOL-LOOP-WISSEN (unbedingt beachten — sonst Fallen erneut)
- Backend `chatgpt.com/backend-api/codex`. `stream:true` PFLICHT (stream:false -> HTTP 400).
- function_calls kommen als STREAM-EVENTS (`response.output_item.done` mit vollstaendigem Item), NICHT in
  `completed.output` (das bleibt leer!). Wer nur completed.output liest, sieht KEINE Tools.
- Backend bricht Chunked-Stream sporadisch ab ("incomplete chunked read") -> Streaming-Retry ist eingebaut (noetig).
- Tool-Format: `{"type":"function","name","description","parameters":{...,additionalProperties:false}}`, tool_choice:"auto".

## Fehlgeschlagene Ansaetze (WICHTIG — nicht wiederholen)
- stream:false am Codex-Backend -> HTTP 400. IMMER streamen.
- function_calls aus completed.output lesen -> leer. Aus output_item.done-Stream-Events lesen.
- Dashboard-Version-Bump NICHT vergessen: bei JEDEM Deploy dashboard/app.py VERSION Z.31 + Timestamp bumpen
  UND dashboard mit-deployen (sonst sieht Frank den alten Footer). DEPLOY.md-Pflicht.
- **features.json STRUKTUR-Umbau (nicht nur Anhaengen): /api/features MERGED persistente Kopie
  (dashboard-data/features.json) + Seed per id -> bei Zusammenfassen/id-Aenderung DUPLIKATE (heute: 68 statt
  32). LOESUNG: nach --build-Deploy die persistente Kopie auf dem VPS ueberschreiben (scp nach
  /opt/second-brain/dashboard-data/features.json + chown 10002:10002 + docker compose restart dashboard).**
- Session-Backup: langer Heredoc schlug frueher am Quoting fehl -> Write-Tool nutzen (so wie dieses Backup).

## Naechste Schritte (priorisiert)
1. agent.jsonl-Volume-Fix klaeren (ChatGPT-Fund) und umsetzen — Frank haelt ihn fuer offen (Offene Aufgabe 1).
2. _process_turn-Umbau inkrementell umsetzen (Offene Aufgabe 2) — der grosse Bereich.
3. Danach Offene Aufgaben 3-8 der Reihe nach.

## Offene Fragen
- agent.jsonl-Volume-Fix: was genau ist der von ChatGPT gefundene Fix? (nachlesen/klaeren bevor umsetzen)
- Reasoning-Level des Hauptagenten: aktuell "medium"; Frank wollte "High-Thinking" — stellt NUR Frank ein
  (Memory `feedback_reasoning_levels_frank_only`). NICHT automatisch aendern.
- features.json: sollen Router/Leseagent-Eintraege beim fertigen Umbau ganz raus oder umgeschrieben werden?

## Anker
- Branch: main
- Letzte Commits:
12c25c117 #47605 - Cortex DEPLOY.md: features.json Schreibweise-Dauerregel + Struktur-Umbau-Merge-Falle
80803687f #47604 - Cortex dashboard 0.58.0: Feature-Chronik komplett ueberarbeitet (55->32)
0f4ece77c #47604 - opencode(rules-opencode): commit-before-build.md + git-workflow.md entfernen (fremde Session)
dfd05aed9 #47603 - session restore: clear handoff backup
