# Cortex Agenten-Umbau — Plan & Kontext (Übergabe für neue Session)

> **Zweck:** Diese Datei ist das vollständige Gedächtnis für den geplanten Umbau des Cortex-Agenten.
> Sie wurde am **07.07.2026** erstellt, damit nach einem `/clear` + Session-Restore eine neue Session
> SOFORT alle Probleme, den Plan und die technischen Anker kennt — ohne die Diagnose neu machen zu müssen.
> **Stand:** Schritt 1 (Logbuch) ist FERTIG + deployed. Schritt 2 (Agenten-Umbau) LÄUFT — Teilbausteine fertig (siehe Fortschritt unten).
> Frank-Priorität: **Korrektheit vor Geschwindigkeit.** Hauptagent = **GPT-5.5**, wird auf **High-Thinking** gesetzt.

---

## ⚡ FORTSCHRITT Schritt 2 — Stand 07.07.2026, 15:12 Uhr (agent 0.61.4)

### FERTIG + verifiziert
1. **Tool-Loop-Funktion `codex_generate_tools()` gebaut, deployed, GRÜN getestet** (agent 0.61.x, Commits #47590–#47595).
   Der isolierte Selbsttest `GET /toolloop-selftest` (2 triviale Tools) lief `ok:true`, beide Tools aufgerufen, `turns:2, stopped:done`.
   **→ Die Machbarkeit des ganzen Umbaus (GPT-5.5 macht Tool-Calling selbst) ist damit END-TO-END BEWIESEN.**
2. **`agent.jsonl`-Volume-Fix (§6.8) FERTIG** (Commit #47596): compose.yaml `- ./agent-logs:/app/logs`, Host-Ordner
   `/opt/second-brain/agent-logs` gehört uid 1000. Das forensische Voll-Log überlebt jetzt Rebuilds (verifiziert: Datei liegt persistent auf dem Host).

### HART ERARBEITETES TOOL-LOOP-WISSEN (unbedingt beachten beim Weiterbau — sonst Fallen erneut)
- **Backend = `chatgpt.com/backend-api/codex`** (Subscription-Impersonation, `codex_generate`-Pfad). Function-Calling geht, ABER:
- **`stream:true` ist PFLICHT** — `stream:false` → HTTP 400 „Stream must be set to true". (empirisch bewiesen)
- **function_calls kommen als STREAM-EVENTS, NICHT in `completed.output`** (das bleibt leer!). Das vollständige
  `function_call`-Item (name/call_id/arguments) steckt im **`response.output_item.done`**-Event. Sequenz:
  `response.created → in_progress → output_item.added → function_call_arguments.delta/done → output_item.done → completed`.
  Wer nur `completed.output` liest, sieht KEINE Tools (das war der erste Fehl-Befund „Tools werden nicht aufgerufen").
- **Backend bricht den Chunked-Stream sporadisch ab** („peer closed … incomplete chunked read") → **Streaming-Retry**
  (bis 3x, nur solange kein `response.completed` kam; Backend-Call ist seiteneffektfrei) ist eingebaut und nötig.
- Tool-Definition-Format (funktioniert): `{"type":"function","name":...,"description":...,"parameters":{json-schema, additionalProperties:false}}`, `tool_choice:"auto"`.
- Reasoning `high` möglich (Frank stellt Level ein — `feedback_reasoning_levels_frank_only`).

### NÄCHSTER SCHRITT: Werkzeugkasten (Task #6) — Blaupause / Kern-Bausteine (Zeilen in agent/app.py, Stand 0.61.4)
Jedes Werkzeug ist ein dünner Handler um eine BESTEHENDE Funktion (nichts neu erfinden, nur kapseln + JSON-Schema):
| Werkzeug | Nutzt bestehende Funktion | Zeile | Rückgabe an den Agenten |
|----------|---------------------------|-------|--------------------------|
| `durchsuche_gedaechtnis(query)` | `smart_recall(user_text, query, user_id)` → `(hits, meta)` | 1898 | Liste {id, titel, kategorie, score, **snippet**} — NICHT Volltext (Kontext-Schutz!) |
| `lade_eintrag(id)` | brain-api „Eintrag per id" (Funktion noch lokalisieren; `brain_search` 1381 / brain /get) | — | Volltext EINES Eintrags |
| `web_suche(query)` | `tavily_search(query, response_size)` → dict | 3329 | verdichtetes Web-Ergebnis |
| `speichere(text)` | `_do_store(quote, categories, …)` → **ABER Bestätigung-vor-Speichern bleibt HARTE Code-Regel im Preflight** | 3146 | Bestätigungs-Rückfrage / Ergebnis |
| `lies_logbuch(n, nur_probleme)` | `_read_recent_turns(limit, only_problems)` | 537 | letzte Turns (Logbuch 1) |
| `lies_regeln()` / `schreibe_regel(text)` | Regelpool (Task #8 — zweistufig, Frank bestätigt) | neu | Regeln / Kandidat anlegen |
Die Leseagent-Filterfunktion (`leseagent_select` 3202) wird NICHT mehr als eigener Agent aufgerufen, sondern der Hauptagent
filtert selbst (er sieht Snippets via `durchsuche_gedaechtnis`, holt Volltext gezielt via `lade_eintrag`). Antwort-Formulierung
(`hauptagent_answer` 3291 etc.) und Router (`hauptagent_route`) entfallen im Tool-Modus. intent-Zweige in `_process_turn`: query 5060, query_internet 5024, internet 5099.
**Reihenfolge Task #7:** deterministischen Preflight (confirm_yes/no, explicit_save, eindeutige Kommandos) BEHALTEN, danach den
Tool-Loop statt der intent-Verzweigung. `_process_turn` läuft synchron via `to_thread` (nicht blockieren).

### Diagnose-Reste (beim Aufräumen in Task #7 entfernen)
`codex_generate_tools` hat noch `raw_first_output`/`seen_events` (Diagnose) + den `/toolloop-selftest`-Endpoint. Harmlos, aber
beim finalen Umbau aufräumen (oder als Sonde behalten — dann in Logbuch 2 einhängen).

---

## 0. Cortex-Server-Fakten (für jede Arbeit nötig)

| Was | Wert |
|-----|------|
| VPS | `168.231.83.205` (root), SSH-Key `~/SK/second-brain/id_ed25519` (BatchMode, passwortlos) |
| App-Verzeichnis VPS | `/opt/second-brain` (KEIN git! Deploy = scp + `docker compose up -d --build <dienst>`) |
| Lokale Quelle | `~/proggs/second-brain-server/` |
| Deploy-Anleitung | `second-brain-server/DEPLOY.md` (genau EIN Weg; Windscribe/Full-Tunnel AUS vor SSH) |
| Dienste (Ports an WireGuard 10.8.0.1) | `agent`:8002 · `brain-api`:8000 · `dashboard`:8003 · `librarian`:8004 · `mcp`:8001 · `qdrant`:127.0.0.1:6333 |
| Sichtbare Version | `dashboard/app.py` VERSION (der EINE Gesamt-Zähler; bei JEDEM Deploy bumpen + Timestamp) |
| Server-Uhr | NTP-synchron, Europe/Berlin — **maßgeblich** (lokale Windows-Uhr ging am 07.07. ~2h nach!). Timestamps IMMER per `ssh ... date` holen. |
| Feature-Chronik | `dashboard/features.json` (bei neuem Feature Eintrag ergänzen, neueste zuerst) |
| Almanache VOR Arbeit lesen | `bugs/server/ai-agent-frameworks.md` (Tool-Loop!), `bugs/server/fastapi.md`, `bugs/server/docker.md` + jeweils `best-practices/server/*` |

---

## 1. DIE PROBLEME (was Frank fixen will) — mit harter Log-Evidenz

### 1.1 Der Auslöser: Star-Trek-Fall (06.07.2026, 21:19 Uhr)
Frank fragte "Welche Serie kam nach Star Trek Enterprise?". Cortex antwortete "nichts Treffendes im
Gedächtnis gefunden" — obwohl **viele** Star-Trek-Einträge existieren (wortwörtlich "Star Trek").

**Log-Beweis (aus `docker exec sb-agent` agent.jsonl, Turn um 19:19 UTC = 21:19 Berlin):**
- Turn 1: `route = query_internet` → Gedächtnissuche LIEF. `brain_search` lieferte **50 Treffer**
  (`lese_select "von": 50`). Der **Leseagent wählte 0 von 50** (`gewaehlt: 0`) → `confidence: keine` → "nichts gefunden".
- Turn 2 (Frank hakt nach): dieselbe Suche, `von: 50`, aber **`gewaehlt: 10`, `confidence: hoch`**.

### 1.2 Die WAHRE Root Cause (beide Chat-Diagnosen waren FALSCH)
- ❌ NICHT "nie gesucht" (Cortex' eigene Chat-Vermutung) — es lief query_internet.
- ❌ NICHT "Qdrant/Retrieval fand nichts" — brain_search lieferte 50 Treffer.
- ✅ **Der Leseagent (LLM-Filter zwischen Suche und Antwort) verwarf alle 50 als "nicht passend"**,
  weil die Frage rein faktisch formuliert war ("welche Serie kam *danach raus*") und er die
  persönlichen Star-Trek-Einträge nicht als relevant zur Reihenfolge-Frage einstufte.
- **ChatGPT kam unabhängig zur EXAKT gleichen Root Cause** (0/50 Leseagent) → hohe Konfidenz.
- Kernbegriff (ChatGPT): fehlende Trennung **Antwort-Treffer vs. Kontext-Treffer**.

### 1.3 Sekundäre Probleme
- **Router routet Faktenfragen nicht zu `query`.** Der Router-Prompt trennt strikt: `query` nur bei
  "Was weiß **ich** über X?"; "Welche Serie kam nach X?" fällt in `internet`/`smalltalk` → **gar kein
  Recall**. Bei Star Trek lief Recall nur zufällig durch einen query_internet-Zwang-Bug.
- **Der 0.59.1-Fix (07.07. früh) entfernt genau diesen Zwang** → verschärft den Star-Trek-Fall
  (ohne Zwang wäre gar nicht gesucht worden). Muss im Umbau mitgedacht werden.
- **Router-Latenz:** Flaschenhals-Radar zeigt `router_ms ≈ 8,3 s` selbst für Smalltalk (GPT-5.5 medium)
  — starkes Argument, den Router als eigene Instanz abzuschaffen.
- **agent.jsonl (forensisches Log) war flüchtig** (kein Volume, geht bei jedem Rebuild verloren) —
  ChatGPT-Fund. Für die Turn-Ebene durch das neue Logbuch gelöst (persistent auf Samba).

---

## 2. WAS SCHON GEBAUT IST — Schritt 1: Logbuch (FERTIG, deployed 07.07. 13:09)

**Commit #47586. agent 0.60.0 + dashboard 0.56.0. Live verifiziert.**

- **Logbuch 1** (`LOGBOOK_DIR/Trace/turns.jsonl`, 100 rollierend): 1 lesbare Zeile je Anfrage —
  `user_text`, `router_intent`→`final_intent`, `recall_query`/`web_query`, `hits_total`,
  `selected_count`, `confidence`, `answer_preview`, `problem`/`problem_note` UND
  `timing` = **Flaschenhals-Radar** (`router_ms`, `suche_ms`, `leseagent_ms`, `web_antwort_ms`, `gesamt_ms`).
- **Logbuch 2** (`Trace/trace.jsonl`, 4000 Events): feine Events je Turn, per `turn_id` verknüpft —
  bewusst das **erweiterbare Gerüst** für feine Sonden (kommen mit dem Umbau).
- Persistent auf Samba (LOGBOOK_DIR bereits gemountet, uid 1000), atomar (tmp+os.replace),
  secret-maskiert (`_LOG_SECRET_RE`), **best-effort** (try/except überall — gefährdet den Chat nie).
- **Mechanik:** `checkpoint()` füttert zusätzlich einen `contextvars`-Turn-Trace (`_current_trace`);
  `_trace_start` VOR `asyncio.to_thread(_process_turn)` gesetzt (Worker sieht ihn via copy_context);
  `_trace_finish` nach dem Response-Bau (im Threadpool). Ein Phasen-Marker `recall_search` (Ende
  `smart_recall`) trennt Rohsuche vom Leseagent. Eingehängt in `/chat` UND `/chat/stream`.
- **Problem-Markierung:** deterministisches Regex `_PROBLEM_FEEDBACK_RE` ("das lief schlecht/warum
  ging das nicht") → `_mark_last_turn_problem` markiert den letzten Turn (hängt NICHT am Router → überlebt Umbau).
- **Lesen:** `GET /logbook/turns?limit=N&only_problems=bool` (neueste zuerst).
- Neue Funktionen in `agent/app.py`: `_redact_log`, `_write_rolling`, `_trace_start`, `_trace_event`,
  `_turn_summary`, `_trace_finish`, `_mark_last_turn_problem`, `_read_recent_turns`.

---

## 3. SCHRITT 2 — DER AGENTEN-UMBAU (das ist der nächste Bau)

### 3.1 Franks Vision (aus der Diskussion 07.07.)
> Zu viele Instanzen, die Fehler machen können. Router-Agent UND Leseagent sollen **weg**. Der einzige
> sinnvolle Zusatz-Agent ist der **Schreibagent** (spezialisiert aufs 1:1-Ablegen). Der **Hauptagent**
> (volle Intelligenz, großer Prompt, GPT-5.5) soll **alles selbst machen**: selbst entscheiden
> (routen), selbst im Gedächtnis suchen, selbst Tools ausführen, selbst filtern, selbst ins Web.

### 3.2 Die wichtige Korrektur (auf der Claude bestand — Direktive #3, funktionserhaltend)
**Nicht Funktionen LÖSCHEN, sondern VERLAGERN.** Router-LLM und Leseagent-als-eigener-Agent
verschwinden, aber ihre ZWECKE bleiben — als Werkzeuge + wenige harte Code-Regeln:
- **Kontext-Schutz (Leseagent-Grund):** manche Einträge sind 18k Zeichen; man darf nicht alle 50
  Volltexte in den Hauptagenten kippen. → Werkzeug `durchsuche_gedaechtnis` zeigt erst **Titel +
  Snippet + Score**; `lade_eintrag(id)` holt gezielt Volltexte. So filtert der Hauptagent SELBST mit
  vollem Verständnis, ohne Kontext-Explosion. (= ChatGPTs "Antwort-Treffer vs. Kontext-Treffer",
  aber im Hauptagenten statt im Leseagenten.)
- **Speicher-Sicherheit:** Bestätigung-vor-Speichern bleibt HARTE Code-Regel (nie ins LLM-Ermessen).
- **Deterministische Weichen bleiben als Preflight:** eindeutige Kommandos, "Alles über X"→query,
  explicit_save (Titel/Kategorie gesetzt), confirm_yes/no-Erkennung. (Poka-Yoke, schon im Code.)

### 3.3 Ziel-Architektur: EIN Hauptagent + Werkzeugkasten
- Hauptagent (GPT-5.5, High-Thinking) bekommt Franks Nachricht, entscheidet selbst per Tool-Calling.
- **Start-Werkzeugset (fokussiert — nicht überladen, ai-agent BP #5):**
  `durchsuche_gedaechtnis(query)` · `lade_eintrag(id)` · `web_suche(query)` ·
  `speichere(text)` (→ geht an den **Schreibagenten**, der BLEIBT) · `lies_logbuch(n, nur_probleme)` ·
  `lies_regeln()` · `schreibe_regel(text)`.
- Router-LLM als eigene Instanz **weg**; ein schlanker deterministischer **Preflight** bleibt nur für
  die harten Fälle (Speicher-Bestätigung, eindeutige Kommandos).
- Leseagent als eigener Agent **weg** (Filter-Funktion wird zum Tool-Schritt des Hauptagenten).

### 3.4 Regelpool / Regeldatei (Franks Wunsch) — ✅ FERTIG (#47682–#47688, 08.07.2026)
- Der Hauptagent kann **eigene Regeln** speichern (z.B. "Dank/Feedback ≠ Speicherwunsch").
- **ZWEISTUFIG (Pflicht):** Regel-Kandidat erkennen → **Frank bestätigt einmal** → erst dann aktiv.
  (Sonst vergiftet sich das System mit falschen Regeln — ChatGPT hat das selbst erkannt.)
- **Limit** ~30–40 Regeln (sonst Prompt zu lang → Qualität sinkt / Context Rot). Als echte Datei
  (agent-data), die der Hauptagent lesen (`lies_regeln`) und nach Franks OK schreiben (`schreibe_regel`) kann.

### 3.5 Hauptagent-Zugriff auf die Logbücher
- Werkzeug `lies_logbuch` → Frank kann fragen "was war da gestern um 21 Uhr / warum ging das nicht?"
  und der Hauptagent schaut selbst in Logbuch 1 (+ später 2) nach. (War in Schritt 1 bewusst
  ausgeklammert — kommt HIER, weil der Hauptagent jetzt Tools bekommt.)

### 3.6 Feine Sonden (Logbuch 2 füllen) — GLEICHZEITIG mit dem Umbau
- Frank-Regel (Stale-Probe-Schutz): feine Sonden NICHT vorher in Router/Leseagent bauen (wird eh
  ersetzt), sondern MIT dem Umbau an den dann stabilen Werkzeug-Grenzen (Tool-Sub-Calls, Qdrant,
  GPT-Antwort einzeln getimt). Ziel: alles nachverfolgbar, auch Geschwindigkeits-Flaschenhälse.

---

## 4. MACHBARKEIT / ENTSCHEIDUNGEN (bereits geklärt)
- **GPT-5.5 High-Thinking** ist erstklassig im Tool-Calling → Umbau realistisch, keine Modell-Bremse.
- **Korrektheit > Tempo** (Frank) → mehr Denk-Runden im Tool-Loop sind ok (der einzige Vorbehalt entfällt).
- Aktuell laufen eh 3 LLM-Calls (Router, Leseagent, Hauptagent) — ein guter Tool-Agent kann schneller sein.
- **Tool-Calling-Pfad:** muss über den Codex/GPT-Provider laufen (GPT-5.5 native Tool-Calls). Prüfen,
  wie `llm_generate`/`codex_generate` in app.py Tool-Definitionen + tool_use/tool_result unterstützt.
  ⚠️ ai-agent-Almanach: **Hard-Stop (max Turns/Zeit)** Pflicht; **tool_use ↔ tool_result strikt 1:1**;
  Tool-Fehler als tool_result zurück (nicht crashen); Schreib-Tools idempotent (gibt es schon: request_id-Dedup).

---

## 5. TECHNISCHE ANKER in `agent/app.py` (355 KB — NICHT komplett lesen, gezielt greppen!)

| Bereich | Funktion / Symbol |
|---------|-------------------|
| Zentraler Flow (ein Turn) | `_process_turn` (~Z. 4550 nach Logbuch-Insert; die intent-Zweige query/query_internet/internet/smalltalk) |
| Router (LLM) | `hauptagent_route`, `build_hauptagent_prompt`, `ROUTER_SCHEMA`, Router-Prompt via `load_instructions("haupt")` |
| Leseagent (Filter) | `leseagent_select`, `build_abfrage_prompt`, `ABFRAGE_SCHEMA`, `load_instructions("abfrage")` |
| Schreibagent (BLEIBT) | `speicheragent_decide`, `_do_store`, `_store_final`, `build_speicher_prompt` |
| Retrieval | `smart_recall` (Zeit/Multi-Query/Entity/RRF), `brain_search` (→ brain-api /search) |
| Antwort-Formulierung | `hauptagent_answer`, `hauptagent_answer_internet`, `hauptagent_answer_recall_internet`, `_format_memory_hits_for_prompt` |
| Modell-Weiche | `llm_generate` / `_llm_generate_once` (Gemini · OpenCode/minimax · Codex/GPT); `codex_generate` (GPT), Streaming via on_delta |
| Handler | `/chat` (async, `_process_turn` via to_thread), `/chat/stream` (SSE) |
| Logbuch (Schritt 1) | `_trace_start/_trace_event/_trace_finish`, `checkpoint` (gekoppelt), `TRACE_DIR`, `GET /logbook/turns` |
| Editierbare Prompts | `agent-data/{haupt,speicher,abfrage}-prompt.txt`, `config.json` (Modelle/Router-Modell), `/config` GET/PUT |
| Eval-Suite (für Evil-Test) | `/eval-run`, `_eval_one`, ~Z. 3810–3972 (Kunstwort/BM25/Zeit/Entity-Fälle) |

---

## 6. OFFENE TO-DOs (Reihenfolge-Vorschlag)
1. **(EMPFOHLEN ZUERST)** Umbau planen: Tool-Calling-Fähigkeit des GPT-Pfads in app.py prüfen
   (unterstützt `codex_generate` schon Tools?). Dann Ziel-Architektur (§3.3) als Design festzurren.
2. Werkzeuge bauen (§3.3), Hauptagent-Loop mit Hard-Stop + tool_use/tool_result-Hygiene.
3. Router-LLM entfernen (deterministischer Preflight bleibt), Leseagent entfernen (Filter → Tool-Schritt).
4. ~~Regelpool (§3.4, zweistufig + Limit).~~ **FERTIG** (#47682–#47688, 08.07.2026): `agent/rules.py` (atomare Ablage, Regelblock, Trigger-Erkennung), `rule_confirm`-Chatdialog (Ja/Nein/Bearbeiten), deterministische Regel-vs-Speichern-Unterscheidung (`is_rule_request`), REST `/rules` + Dashboard-Chronik. Ablage EXTERN auf `Z:\Logbuch\Regeln` (nicht agent-data), 40-Regeln-Limit. Spec: `docs/superpowers/specs/2026-07-08-cortex-selbst-regeln-design.md`.
5. Feine Sonden in Logbuch 2 (§3.6).
6. Evil-Test: Star-Trek-Regressionsfall in die Eval-Suite (Rohsuche findet Star-Trek-Einträge →
   Auswahl behält ≥1 → Antwort enthält sichtbaren Gedächtnis-Kontext). Auch: "Danke" ≠ speichern/query_internet.
7. Dashboard-Ansicht fürs Logbuch (Tabelle + Flaschenhals + Problem-Filter).
8. **agent.jsonl persistent mounten — DER FIX, DEN CLAUDES ERSTE ANALYSE ÜBERSAH (nur ChatGPT fand ihn).**
   `/app/logs/agent.jsonl` (das forensische VOLL-Log: alle `_log`-Einträge, Fehler, ALLE checkpoints —
   mehr als die Turn-Ebene) hat **KEIN Volume** → geht bei jedem `docker compose up -d --build agent`
   verloren (deshalb hatte es beim Debuggen nur 274 Zeilen ab dem letzten Rebuild; die Star-Trek-Evidenz
   war nur durch Glück noch da). **Fix:** in `compose.yaml` beim `agent`-Dienst ein Volume ergänzen
   (`- ./agent-logs:/app/logs`), Host-Ordner `/opt/second-brain/agent-logs` `chown 1000:1000` (agent
   läuft als uid 1000, DEPLOY.md §5). Danach überlebt das Detail-Log Rebuilds.
   ⚠️ **MUSS noch gemacht werden** — die Turn-Ebene ist zwar durch das neue Logbuch auf Samba schon
   persistent, aber das darüberhinausgehende forensische Voll-Log (brain-api hat es, der agent bisher nicht)
   ist weiter flüchtig, bis dieses Volume ergänzt ist.

### Zwischenlösung (falls der Umbau dauert, gilt in BEIDER Welt)
Kleines Sicherheitsnetz: Der Leseagent (bzw. der spätere Filter-Schritt) darf **nicht 0** Treffer
wählen, wenn exakte Wort-Treffer (BM25) mit hohem Score oben liegen — mindestens die besten
Kontext-Treffer durchreichen. (Poka-Yoke gegen den Star-Trek-Fehler.)

---

## 7. WICHTIG für die neue Session
- Diese Datei ZUERST lesen (`second-brain-server/CORTEX-AGENT-UMBAU-PLAN.md`).
- Vor Code an app.py: die 3 Almanach-Kurzchecks + Best-Practices lesen (ai-agent, fastapi, docker).
- Timestamps IMMER per Server-Uhr (`ssh ... date`) — lokale Uhr war 2h daneben.
- Deploy: scp + `docker compose up -d --build agent dashboard` + dashboard-VERSION bumpen + features.json.
- Nach jedem Deploy: Health + ein Test-Chat (isolierter user `logbuch-selftest`) + `/logbook/turns` prüfen.
