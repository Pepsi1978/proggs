---
name: research
description: "Zentraler Recherche-Orchestrator fuer JEDE Web-Recherche: nimmt einen strukturierten Research-Auftrag entgegen und fuehrt ihn mit sichtbaren beschrifteten parallelen Researchern (Continuous-Spawning), gepinnter Engine, Live-Zwischenfazit pro Researcher und ruhiger Auswertung aus. Nutze IMMER wenn der Benutzer 'recherchiere', 'such im Web', 'Web-Recherche', 'finde heraus', 'recherchier das' sagt ODER wenn ein anderer Skill (best-practices, bug-almanach-recherche, almanach-update, best-practices-update, direktiven-recherche, superintelligenz) bzw. Agent (researcher, forschungsagent, intelligence-researcher) Web-Recherche braucht und an diesen Skill delegiert. Erfuellt die Research-Strategie-Regel (Empfehlung + Frage 1 A/B/C/D). Registriert als verbindlichen LETZTEN Schritt jeden neu angelegten oder erweiterten Almanach + Best-Practices-Bereich in allen drei Almanach-Hooks (index/hint/guard)."
---

# research — Zentraler Recherche-Orchestrator

Dieser Skill ist die EINE ausfuehrbare Stelle fuer Web-Recherchen. Er kapselt das komplette
"WIE": Auftrag annehmen, Thema zerlegen, sichtbare parallele Researcher mit Continuous-Spawning
starten, pro Researcher sofort ein Zwischenfazit zeigen, ruhig auswerten und konkrete
Umsetz-Aufgaben ableiten. Alle anderen Research-Skills/Agenten **delegieren** hierher, damit
ihre Recherche-Arbeit konsistent und vollstaendig laeuft — ohne dass das "WIE" 8x dupliziert wird.

**Policy-Schicht (bleibt getrennt):** Die Regel `~/.claude/rules/research-strategy.md` entscheidet
das OB/WOMIT (Empfehlung + Frage 1 A/B/C/D + Eskalations-Frage 2 + Kostenkontrolle). Dieser Skill
ist die Orchestrierungs-Schicht (das WIE). Die Ausfuehrungs-Schicht sind die Skripte
`mm-research.py` / `or-research.py` bzw. der Sonnet-5-Schwarm.

---

## Block 0.0 — Dieser Skill traegt sich selbst (PFLICHT-Lesung) ⭐

**Warum das hier steht:** Die Profile unter `OpenLauncher/Profiles/` sind unterschiedlich ausgestattet
(geprueft 09.09.2026). Der Skill ist das EINZIGE, was in jedem Profil vorhanden ist:

| Profil | `rules/` (u.a. `research-strategy.md`) | `research-approval`-Hook | `research`-Skill |
|--------|---------------------------------------|--------------------------|------------------|
| ClaudeCode/**minimal** | ✗ keine | ✗ (nachgeruestet 09.09.2026) | ✓ |
| ClaudeCode/**standard** | ✓ 36 Regeln | ✗ (nachgeruestet 09.09.2026) | ✓ |
| ClaudeCode/**strict** | ✓ 36 Regeln | ✓ | ✓ |
| ClaudeCodeMac/standard + strict | ✓ | teilweise | ✓ |
| OpenCode/OpenCodeMac (alle) | nur `AGENTS.md` | ✗ (Hooks anders) | ✓ (seit 09.09.2026) |

**Folge:** Verlasse dich NIE darauf, dass `research-strategy.md` oder `research-persistence.md` geladen
sind — in `minimal` sind sie es nicht. Alles Pflichtige steht deshalb HIER im Skill im Wortlaut:
Frage 1 (Block 0.1), Frage 2 (Schritt 6) und die Persistenz-Checkliste (Schritt 8).

**Gilt in JEDEM Arbeitsmodus — Freimodus, Schnellmodus, Normalmodus, Gruendlichkeitsmodus.**
Der Schnellmodus sagt "ohne Nachfragen" und "keine Tests, keine Ueberpruefungen". Das meint den
Umsetzungs-Teil einer Programmieraufgabe. Es hebt **nicht** auf:
- **Frage 1** (Engine-Wahl) — sie ist eine Kosten-Freigabe, keine Rueckfrage zur Arbeitsweise. Eine
  Recherche ohne sie kann Firecrawl-Credits und Geld verbrennen, das Frank nicht ausgeben wollte.
- **die Persistenz** in `best-practices/` + `bugs/` (Schritt 8) und die **Hook-Registrierung** (Schritt 9)
  — ohne sie ist die Recherche wertlos, sobald die Session endet. Das ist der eigentliche Ertrag.
Im Schnellmodus wird beides KURZ gehalten (knappe Frage, knappe Eintraege), aber nie weggelassen.

**Gilt bei JEDEM Modell.** Der Skill ist reine Ablauf-Anweisung — er funktioniert gleich, ob die
Session auf einem Anthropic-Modell, einem OpenAI-Modell oder sonst etwas laeuft. Modellabhaengig ist
NUR, was Engine C bedeutet (Tabelle in Schritt 3).

---

## Block 0.1 — Frage 1 im Wortlaut (PFLICHT, jedes Mal) ⭐

Drei sichtbare Schritte, in dieser Reihenfolge, VOR jeder Web-Recherche:

**1. Empfehlung** — ein Satz, welcher Weg fuer genau diese Recherche-Art sinnvoll ist:

| Recherche-Art | Empfehlung |
|---------------|-----------|
| Schnelle Einzelfrage / 1-2 Quellen reichen | **A** |
| Grosser Wissensschatz / viele Unterthemen | **A → dann B**, oder direkt **B** bei knappen Firecrawl-Credits |
| Aktualitaet ueber viele Quellen, Snippets reichen | **B** |
| Firecrawl-Credits fast leer | **B** |
| Hoechste Korrektheit / Geld egal | **C** |
| Unklar / erst besprechen | **D** |

**2. Frage 1** — "Wie soll ich '<thema>' recherchieren?", die empfohlene Option zuerst mit `(Empfohlen)`:

| Option | Weg | Parallel | Kosten |
|--------|-----|----------|--------|
| **A** | Firecrawl holt volle Seiten (Rueckfall: Tavily) → DeepSeek V4 Flash @ Makora wertet aus | 2 | Firecrawl-Free + ~0,1 ct |
| **B** | Dasselbe Modell mit `:online`-Websuche statt Firecrawl | 7 | ~1 ct je Researcher |
| **C** | Schwarm auf dem Host-Modell (siehe Tabelle Schritt 3) | 7 | teuer — nur wenn ausdruecklich gewaehlt |
| **D** | Freitext — etwas anderes / erst besprechen | — | — |

**3. Danach** erst Schritt 1 dieses Skills starten.

**Wie gefragt wird — harness-abhaengig:**
- **`AskUserQuestion` vorhanden** (Claude Code): anklickbar stellen, Header `Engine`, drei Optionen
  (A/B/C) — das Freitext-Feld "Other" ist das D und kommt automatisch dazu.
- **Kein `AskUserQuestion`** (OpenCode und alles andere ohne dieses Werkzeug): die vier Optionen als
  **nummerierte Klartext-Liste** ausgeben und den **Zug beenden**. Auf Franks Antwort warten.
  NIEMALS die Frage selbst beantworten, sich eine Antwort ausdenken oder die Freigabe-Flag ohne
  Franks Antwort setzen — das umgeht genau die Kostenkontrolle, fuer die es die Frage gibt.

Eine beilaeufige frueher genannte Engine ("nimm B") ersetzt die Frage NICHT. Ausnahme bleibt eine
einzelne billige `WebSearch` zur Faktenpruefung mitten in einer anderen Aufgabe.

---

## Block 0 — Fest eingebettete Pfade (kein Suchen!)

NIEMALS nach den Skripten/Keys suchen — sie liegen fest hier:

| Zweck | Pfad |
|-------|------|
| Firecrawl→MiniMax (Engine A) | `~/proggs/mm-research.py` |
| OpenRouter `:online` (Engine B) | `~/proggs/or-research.py` |
| Continuous-Spawning-Runner (A+B, erzwingt max N parallel) | `~/proggs/research-swarm.py` |
| Approval-Flag (vom Hook erzwungen) | `$TEMP/research-approved.flag` (Windows) bzw. `$TMPDIR/research-approved.flag` |
| Firecrawl-Key | `~/SK/OpenCode/firecrawl-api-key.txt` |
| Tavily-Key (Rueckfall Engine A) | `~/SK/Tavily/tavily-api-key.txt` |
| OpenRouter-Key | `~/SK/ClaudeCodeOpenRouter/openrouter.key` |
| Policy-Regel | `~/.claude/rules/research-strategy.md` |
| Rueckgabe-Schema-Vorlagen | `references/rueckgabe-schemata.md` (in diesem Skill) |

**Das Auswerte-Modell fuer A UND B (seit 09.09.2026):** `deepseek/deepseek-v4-flash-0731`,
Anbieter-Kette **Makora → Relace → DeepInfra** (seit 11.09.2026), `reasoning effort: high`. A und B benutzen jetzt DASSELBE Modell —
der Unterschied liegt nur in der Quellenbeschaffung (A = Firecrawl-Vollseiten, B = `:online`-Websuche)
und in der Parallelitaet (A = 2, B = 7). Verifiziert gegen die OpenRouter-API am 09.09.2026:
1.048.576 Token Kontext, `reasoning_effort` unterstuetzt, $0.09/$0.195 pro Mio Token.
Die Kette sitzt in den Skripten (`MM_PROVIDER`/`OR_PROVIDER`, kommagetrennt, Default
`makora,relace,deepinfra`, `allow_fallbacks=false`, LEER = kein Pin) —
nichts von Hand mitgeben.

Aufruf-Konventionen (immer so, nie raten):
- **Engine A:** `python3 ~/proggs/mm-research.py "<unterthema>" [n]`
  — Firecrawl holt die vollen Seiten, DeepSeek V4 Flash @ Makora wertet aus. Modell/Anbieter/Effort
  stehen als Default im Skript; ueberschreibbar per `MM_MODEL` / `MM_PROVIDER` / `MM_EFFORT`.
  **Firecrawl maximal tief (seit 11.09.2026):** `[n]` weglassen — Default ist das Firecrawl-Maximum
  von **100 Quellen** je Suche (`/v2/search`, jede Seite voll gescrapt, nur Hauptinhalt). Kostet
  ~120 Credits je Suche. Scheitert die 100er-Suche, folgt automatisch ein Versuch mit 30, dann Tavily.
  An den Auswerter gehen je Quelle max. 20.000 Zeichen, insgesamt max. ~2 Mio Zeichen (~500k Token).
  **Tavily-Rueckfall (seit 09.09.2026, automatisch):** Faellt Firecrawl aus (HTTP-Fehler, Timeout) ODER
  liefert es 0 bzw. nur leere Treffer, sucht das Skript von selbst bei **Tavily** nach — mit den
  maximalen Einstellungen, die Tavily hergibt: `search_depth="advanced"`, `max_results=20`,
  `chunks_per_source=3`, Volltext (`include_raw_content`) und `include_answer="advanced"`.
  Jede Quelle traegt ihre Herkunft (`### QUELLE n [Firecrawl]` / `[Tavily]`), die stderr-Fusszeile
  nennt sie ebenfalls — im Zwischenfazit (Schritt 4) mit angeben, woher die Belege kamen.
  Steuerung per `MM_TAVILY`: `fallback` (Default) · `always` (immer beide, zusammengefuehrt) · `off`.
  **Wenn Frank sagt, die Ergebnisse seien inhaltlich duenn** — was das Skript nicht messen kann —
  den Lauf mit `MM_TAVILY=always` wiederholen, bevor auf B eskaliert wird.
- **Engine B:** `python3 ~/proggs/or-research.py "<unterthema>" deepseek/deepseek-v4-flash-0731:online`
  — das Modell-Suffix `:online` laesst OpenRouter selbst eine Websuche dazuschalten (web-Plugin,
  Such-Engine intern = parallel.ai). **KEINE explizite Engine als 3. Argument** (kein `parallel`/
  `exa`/`firecrawl`). `:online` ist bei hoher Parallelitaet stabiler als das alte `web_search`-
  Server-Tool (A/B-getestet 2026-06-21). `reasoning:high` und der Makora-Pin sind im Werkzeug
  eingebaut. Bei mehreren Parallel-Laeufen pro Lauf ein eigenes `OR_OUTDIR` setzen (sonst
  ueberschreiben sich die Ausgaben).
  Eskalations-Modell (mehr Denkkraft): `z-ai/glm-5.2:online`.

**Gezielte Einzel-Nachsuche (Luecken fuellen) — IMMER ueber die Skripte, NIE ueber ein MCP-Tool:**
Auch eine einzelne, gezielte Such-/Scrape-Nachsuche (z.B. eine `site:developer.mozilla.org …`-Query,
um eine Primaerquelle nachzuladen) laeuft ueber Engine A mit einer engen Query —
`python3 ~/proggs/mm-research.py "site:… <gezielte query>" 3` — bzw. zur Eskalation ueber Engine B.
**NIEMALS zu einem `firecrawl_*`-Tool greifen** (`firecrawl_firecrawl_search`/`_scrape`): In
OpenCode gibt es seit 2026-06-26 KEINEN Firecrawl-MCP mehr (alles ueber API), in Claude Code gab es
nie einen. Wer instinktiv ein Firecrawl-Tool aufruft, laeuft ins Leere — die API-Skripte sind der
einzige Web-Weg, fuer Haupt- UND Nachsuche.

---

## Der Research-Auftrag (die verlustfreie Bruecke)

Eine Recherche wird IMMER ueber ein **benanntes Feld-Schema** uebergeben — nie als Fliesstext.
Das verhindert "Stille-Post"-Verlust (A sagt B etwas, B sagt C etwas Verzerrtes). Wenn ein
aufrufender Skill delegiert, fuellt er diese Felder; fehlt eines, hier ERFRAGEN statt raten:

| Feld | Bedeutung | Pflicht |
|------|-----------|---------|
| `thema` | Gesamtthema, 1 Satz | ja |
| `zweck` | wozu (bug / best_practice / direktive / superintelligenz / integrationsplan / adhoc) — bestimmt das Rueckgabe-Schema | ja |
| `zerlegungs_modus` | `feste_liste` \| `selbst_generierend` \| `iterativ_wellen` | ja |
| `unterthemen[]` | exakte Teilbereiche, **je 2-3 Saetze praezise** beschrieben (das Herz gegen Verlust). Bei `selbst_generierend`/`iterativ_wellen` ganz/teilweise leer + Generierungs-Auftrag | ja* |
| `version_anker` | LIVE-Softwareversion(en) + Verweis auf bestehenden Stand | bei bug/best_practice PFLICHT |
| `engine` | `A` (mm/Firecrawl) · `B` (or/OpenRouter `:online`) · `C` (Sonnet-5-Schwarm) — aus Frage 1 | ja |
| `anzahl` · `wellen` · `cap` | Researcher-Zahl, Wellen, Eintrags-Cap (Default **kein Cap**) | ja |
| `rueckgabe_schema` | welches Output-Format (siehe `references/rueckgabe-schemata.md`) | ja |
| `persistenz_ziel` | Zielpfad(e), wohin der Aufrufer das Ergebnis einarbeitet | ja |
| `dup_quelle` | woher Duplikate gefiltert werden (bestehender Almanach, MEMORY, superintelligenz.md …) | nein |
| `nacharbeit_aufrufer` | was der Aufrufer DANACH selbst tun muss (z.B. `gh` OPEN/CLOSED-Pruefung — Researcher haben kein Bash) | nein |

Wird der Skill direkt vom Benutzer aufgerufen ("recherchiere X"), fehlt das meiste — dann
`zweck=adhoc`, `rueckgabe_schema=adhoc`, und das Thema sinnvoll selbst in Unterthemen zerlegen.

---

## Ablauf

### Schritt 1 — Auftrag pruefen + Approval

1. Auftrag gegen das Schema pruefen. Fehlt ein Pflichtfeld (besonders `version_anker` bei
   bug/best_practice), beim Aufrufer/Benutzer ERFRAGEN — nie raten (Versions-Luecke = falsche
   Fix-Stati, "Geister jagen").
2. Die Policy-Regel hat vor diesem Skill bereits Empfehlung + Frage 1 (A/B/C/D) gestellt; die
   gewaehlte Engine steht im `engine`-Feld. Falls der Skill direkt ohne vorherige Frage 1
   gestartet wurde: kurz die Empfehlung + Frage 1 nach `research-strategy.md` nachholen.
3. Approval-Gate: Der Hook verlangt `$TEMP/research-approved.flag`. Ist es gesetzt (vom Frage-1-
   Schritt), weiter. **KEINE Selbsttests** "ob das System geht" — die Pipeline ist verifiziert,
   es wird direkt gearbeitet.

### Schritt 2 — Themen-Zerlegung (zwei Modi)

- **`feste_liste`** (bug-almanach-recherche, best-practices, almanach-/best-practices-update):
  Die `unterthemen[]` sind bereits fest formuliert (5-7 Teilbereiche). 1:1 uebernehmen — nichts
  umschreiben, nichts weglassen. Vollstaendigkeit ist hier wichtiger als Kreativitaet.
- **`selbst_generierend`** (intelligence-researcher u.a.): Aus `thema` selbst praezise
  Unterthemen ableiten (je 2-3 Saetze), bevor gespawnt wird.
- **`iterativ_wellen`** (superintelligenz): Erste Welle Fragen generieren, nach jeder Welle eine
  **Luecken-Analyse** ("was fehlt noch, was war widerspruechlich?") und die naechste Welle
  daraus verbessern. Kreativitaet ist hier wichtiger als starre Vollstaendigkeit.

Jedes Unterthema bekommt eine **Beschriftung** (Kurztitel) fuer die Live-Anzeige.

### Schritt 3 — Sichtbare parallele Researcher + Continuous-Spawning ⭐

**Continuous-Spawning ist die oberste Regel: sobald EIN Researcher fertig ist, SOFORT den
naechsten aus der Warteschlange starten — NIEMALS auf eine ganze Welle warten.** Es laufen
konstant so viele gleichzeitig, wie die Engine erlaubt. Kein Wellen-Barrier, kein Leerlauf.

| Engine | Max gleichzeitig | Aufruf |
|--------|------------------|--------|
| A — Firecrawl (mm) → DeepSeek V4 Flash @ Makora | **2** (hartes Free-Limit) | `mm-research.py` |
| B — OpenRouter (or), `:online`, dasselbe DeepSeek-Modell | **7** (`:online` verteilt selbst auf mehrere Modell-Provider → last-stabil; A/B-Test 2026-06-21: 10 echt-parallel sauber. Der intermittente JSON-Tool-Call-Leak §42 wird vom `or-research.py`-Retry gefangen) | `or-research.py … deepseek/deepseek-v4-flash-0731:online` |
| C — Schwarm auf dem **Host-Modell** (harness-abhaengig, s.u.) | **7** | Agent-/Task-Tool |

Praktische Umsetzung — **Continuous-Spawning ist PFLICHT (Zeit nicht verschwenden):** sobald EIN
Researcher fertig ist, startet SOFORT der naechste, sodass konstant das Engine-Limit gleichzeitig laeuft.
NIE in Wellen warten. Bei mehr Unterthemen als dem Limit → nur Limit-viele starten, Rest nachziehen.

**Engine A + B (Bash-Skripte) → IMMER ueber `~/proggs/research-swarm.py` (erzwungene Durchsetzung):**
Eine Skill-Regel allein ist nur advisory; das Skript haelt per `ThreadPoolExecutor(max_workers=N)` KONSTANT
N parallel und zieht bei jedem fertigen Researcher SOFORT den naechsten aus der Queue — das Continuous-
Spawning ist im CODE erzwungen (deterministisch), nicht von Hand zu orchestrieren und nicht "in Wellen"
verschlechterbar. Engine-Limits hart gedeckelt: **A=2, B=7** (Ueberanforderung wird gedeckelt + gewarnt).
```bash
# Unterthemen je 1 Zeile in eine Datei, dann der Swarm (im Hintergrund starten):
python3 ~/proggs/research-swarm.py B ~/.research-swarm/themen.txt   # B = :online, konstant 7 parallel
python3 ~/proggs/research-swarm.py A ~/.research-swarm/themen.txt   # A = Firecrawl, konstant 2 parallel
```
Roh-Antworten je Researcher in `~/.research-swarm/answer-<i>.txt` (+ eigenes `run-<i>/`, kein Ueberschreiben),
NICHT im Hauptkontext. Wenn `done.flag` da ist, je Researcher ein Zwischenfazit (Schritt 4) zeigen.

**PFLICHT — Vor jedem Lauf raeumt `research-swarm.py` automatisch alte Output-Reste weg** (Poka-Yoke Stufe 3,
Vorfall 2026-06-25): es loescht beim Start seine eigenen `answer-*.txt`/`log-*.txt`/`run-*/`/`done.flag` im
Arbeitsverzeichnis, BEVOR es startet. Grund: `run-<i>/` wird mit `exist_ok=True` angelegt — schlaegt ein
Researcher fehl, bleibt sonst die **alte `run-<i>/answer.json` eines FRUEHEREN Laufs (mit fremdem Thema)**
stehen und schlaegt beim Auslesen als falsches Ergebnis durch (real getroffen: 2 von 5 Slots lieferten
LLM-/Agent-Reste statt der Windows-SMB-Themen). Der Cleanup fasst NUR die eigenen Muster an — nie die
Input-Datei `themen.txt` oder fremde Reste anderer Skills (`bp/`, `esc/`, `qdrant/` …). Kein manuelles
Aufraeumen noetig; fuer Crash-Resume einmalig `RESEARCH_SWARM_RESUME=1` setzen (dann KEIN Cleanup, SKIP-Logik aktiv).

**Engine C — NICHT skriptbar (Agent-Tool-Aufrufe macht der Hauptagent), darum Pattern PFLICHT:**
Continuous-Spawning HIER von Hand, aber genauso strikt: **erst 7 Agent-Tool-Aufrufe gleichzeitig**;
**sobald EINE Completion-Notification kommt, im selben Zug den naechsten wartenden Researcher spawnen**
→ wieder 7 laufend. NIE auf alle 7 warten (Wellen-Barrier = Zeitverlust). Jeder Researcher schreibt sein
Ergebnis in eine eigene Datei + gibt nur eine Kurz-Summary zurueck (kontextschonend, crash-sicher nach
`subagent-crash-proofing`).
> laufen 7, einer kommt zurueck → nur noch 6 → SOFORT den 8. spawnen (wieder 7) → … bis alle durch.

**⭐ PFLICHT seit 09.09.2026 — Engine C haengt vom Harness ab. ZUERST feststellen, wo du laeufst:**

**ZUERST pruefen — zwei Umgebungsvariablen entscheiden:** `echo $CLAUDECODE` und `echo $ANTHROPIC_BASE_URL`

| Fall | Erkennung | Was Engine C bedeutet |
|------|-----------|------------------------|
| **Claude Code auf Anthropic-Modell** | `CLAUDECODE=1` **und** `ANTHROPIC_BASE_URL` leer oder auf Anthropic zeigend | **Sonnet-5-Schwarm**: Agent-Tool, `subagent_type:general-purpose` + Prompt + **`model:"sonnet"`** (PFLICHT), Effort "high", 7 parallel |
| **Claude Code auf einem Fremdmodell** (OpenLauncher-Start ueber `claude-openrouter/Start-ClaudeCode-OpenRouter.ps1`, z.B. ein OpenAI-Modell) | `CLAUDECODE=1`, aber `ANTHROPIC_BASE_URL=https://openrouter.ai/api` | **Schwarm auf dem Session-Modell**, **KEIN `model:"sonnet"`** — der Alias loest hinter der OpenRouter-Basis-URL nicht auf und wuerde den Lauf zerlegen. Sonst wie OpenCode (siehe naechste Zeile) |
| **OpenCode / jeder andere Harness** | `CLAUDECODE` NICHT gesetzt; es ist eine `AGENTS.md` geladen (Profil unter `OpenLauncher/Profiles/OpenCode*/`) | **Schwarm auf dem AKTUELLEN Session-Modell**: bis 7 parallele Subagenten, **KEIN `model:`-Override** — sie erben das Modell, mit dem die Session gerade verbunden ist (z.B. GPT 5.6 Sol). Diese Modelle haben eine EIGENE Internet-Anbindung und recherchieren selbststaendig; **kein** `mm-`/`or-research.py` noetig |

**Merksatz:** `model:"sonnet"` NUR im ersten Fall. In allen anderen Faellen recherchiert das Modell,
mit dem die Session gerade verbunden ist — genau darum geht es bei Stufe C ausserhalb von Claude Code.

**In Claude Code auf einem Anthropic-Modell:** `CLAUDE_CODE_SUBAGENT_MODEL` steht seit der Sonnet-5-Umstellung auf
`inherit` (nicht mehr `opus[1m]`) — ohne den expliziten `model`-Parameter wuerden die Researcher auf ein
unbestimmtes Fallback-Modell laufen statt auf Sonnet 5. **Jeder** Engine-C-Agent-Tool-Aufruf bekommt daher
`model:"sonnet"` (Alias, loest zu Sonnet 5 auf, natives 1M-Kontext). Effort bleibt "high" (globaler
Session-Standard `effortLevel: "high"`).

**Ueberall sonst (OpenCode, Claude Code auf Fremdmodell):** genau UMGEKEHRT — **niemals** ein `model:` mitgeben. Der Sinn ist ja, dass das
Session-Modell selbst recherchiert. Jeder Subagent bekommt den Auftrag "recherchiere <Unterthema> im Web
mit deinen eigenen Werkzeugen, quellentreu, Quelle pro Aussage" und schreibt in eine eigene Datei.
Parallelitaet und Continuous-Spawning bleiben identisch (7 gleichzeitig, sofort nachziehen).
Details/Begruendung: `~/.claude/rules/research-strategy.md` §4a.

**Live-Darstellung — jeder Researcher beschriftet mit Engine/Modus + Thema:**

```
🔬 Research: "<thema>"  ·  Engine: DeepSeek V4 Flash @ Makora/:online  ·  Modus: Eskalation  ·  Deckel: 10 Treffer/Researcher
   Researcher 1 [DeepSeek/:online · Eskalation] — <voller Unterthemen-Satz> … laeuft
   Researcher 2 [DeepSeek/:online · Eskalation] — <voller Unterthemen-Satz> … ✓ fertig (8 Quellen)
   Researcher 3 [DeepSeek/:online · Eskalation] — <voller Unterthemen-Satz> … laeuft
   [aktiv: 7 · fertig: 2/12 · ~0,07 $]
```

Die Engine wird in der Kopfzeile UND an jedem Researcher angezeigt (Soll: sichtbar womit
recherchiert wird). Bei Engine B steht dort immer `:online` — keine explizite Such-Engine.

### Schritt 4 — Zwischenfazit pro Researcher (sofort)

**Sobald ein Researcher zurueckkommt — noch bevor alle fertig sind — sofort ein Kurzfazit
(2-3 Saetze)** ausgeben: was er herausgefunden hat und was daran interessant/umsetzbar ist.
So liest der Benutzer Ergebnisse live mit, statt am Ende auf einen Block zu warten:

```
   ✓ Researcher 2 — Kurzfazit: <2-3 Saetze: Kernfund + warum interessant/umsetzbar>
```

Das Kurzfazit ist eine kompakte Lesefassung der Researcher-Antwort (nicht die Rohdaten).

### Schritt 5 — Auswertung nach jeder Stufe (ruhig, im Rueckgabe-Schema)

Wenn alle Researcher einer Stufe fertig sind: ruhige Auswertung in **festen Bloecken**, **keine
`━`-Linien, keine Farbpunkte** (Research = Neues → verstaendlich erklaeren):

```
## Kurzfassung
2-3 Saetze, was unterm Strich rauskam.

## Das Wichtigste
1. Befund — knapp + verstaendlich erklaert.
2. …

## Fuer deinen Einsatz
Was das konkret fuers Projekt bedeutet (1-3 Punkte).

## Noch offen / unsicher
Was die Quellen NICHT hergaben oder widerspruechlich war.

Quellen: 12 · Engine: DeepSeek V4 Flash @ Makora/:online · Kosten: 0,07 $
```

Zusaetzlich liefert der Skill das Ergebnis im **`rueckgabe_schema`** des Auftrags (siehe
`references/rueckgabe-schemata.md`) zurueck an den Aufrufer — exakt in dessen Format, damit kein
Skill verschlechtert wird. Bei vielen Funden **lossless**: in Datei auslagern + Pfad + kompakte
Summary (Funde nie kappen — der `cap` steuert nur, wie viel inline zurueckkommt). Immer mit dabei:
Quellen+Version pro Finding, der "offen/unsicher"-Block und der `nacharbeit_aufrufer`-Hinweis.

### Schritt 6 — Zwei-Stufen-Eskalation

Nach Stufe 1 (Engine A, Firecrawl) kommt die obige Auswertung, danach **IMMER Frage 2** — auch wenn
die Auswertung gut aussieht, und auch im Schnellmodus. Grund: Firecrawl Free hat nur 1000 Seiten/Monat,
Frank entscheidet pro Recherche bewusst. Wortlaut (anklickbar, sonst als nummerierte Klartext-Liste):

> **"Noch eine zusaetzliche Eskalations-Research?"**
> · **Nein, fertig** — die Ergebnisse reichen
> · **Ja, DeepSeek V4 Flash `:online`** — andere Quellenbasis als Firecrawl, bis 7 parallel
> · **Ja, Host-Modell-Schwarm** — teuerste Stufe
> · **Freitext**

Entfaellt nur, wenn ohnehin schon Stufe B oder C gewaehlt wurde. Stufen:

```
A: Firecrawl-Quellen → DeepSeek V4 Flash @ Makora (mm)  → Standard, Firecrawl-Free-Credits, 2 parallel
B: DeepSeek V4 Flash :online @ Makora (or)              → pay-per-use, bis 7 parallel (last-stabil + Retry)
C: Schwarm auf dem Host-Modell                             → Claude Code: Sonnet-5-Schwarm (teuer, nur bewusst
                                                              gewaehlt) · OpenCode: aktuelles Session-Modell
```

Nach JEDER Stufe wieder dieselbe ruhige Auswertung (Schritt 5) → der Benutzer entscheidet ueber
die naechste Stufe. Eskalation nie automatisch durchlaufen ohne Auswertung + Entscheidung.

### Schritt 7 — Gesamtauswertung → konkrete Umsetz-Aufgaben

Zum Schluss der Gesamtauswertung **konkrete, umsetzbare Aufgaben** ableiten — "was koennten wir
jetzt wie umsetzen, was waere sinnvoll?" — **im Kontext von Franks Projekten, hauptsaechlich dem
aktuell bearbeiteten Projekt** (zur Laufzeit ermitteln: zuletzt bearbeitete App / aktueller
Arbeitskontext). Nur bei `zweck=adhoc`/Direkt-Aufruf; bei Delegation kommt das Ergebnis im
Rueckgabe-Schema zurueck und der Aufrufer macht die fachliche Nacharbeit.

```
## Was wir jetzt umsetzen koennten (Projekt: <aktuelles Projekt>)
1. <konkrete Aufgabe> — warum sinnvoll, grober Aufwand
2. …
```

### Schritt 8 — Persistenz: Erkenntnisse speichern (PFLICHT, nicht ueberspringbar) ⭐

**Das ist der Ertrag der ganzen Recherche.** Ohne diesen Schritt war die Arbeit umsonst, sobald die
Session endet. Er gilt in **jedem** Arbeitsmodus — auch im Schnellmodus, dort nur kuerzer formuliert.
Die Regel `research-persistence.md` ist in manchen Profilen (z.B. `minimal`) gar nicht geladen, darum
steht die Checkliste hier vollstaendig:

**Checkliste — jeden Punkt abhaken, bevor du die Recherche als fertig meldest:**

1. **Tauglichkeit pruefen:** "Ist das ueber diese eine Aufgabe hinaus wiederverwendbar?"
   TAUGLICH: Patterns/APIs/Architektur, bekannte Bugs + Workarounds, Library-Vergleiche,
   Plattform-/Policy-Wissen, Harness-Wissen. NICHT: einmalige Faktenabfrage, rein projektspezifisch.
   **Im Zweifel: einarbeiten.** Ist nichts tauglich, das in EINEM Satz begruenden — nie stillschweigend
   weglassen.
2. **Best Practices** → `~/proggs/best-practices/<kategorie>/<bereich>.md`
   Mit Stand-Datum (echte Systemzeit per Befehl holen!), Versions-Anker, Quellen-URLs, Markierung
   `offiziell`/`extern`. **Kurzcheck (Stufe A) UND Volltext** — nie nur eines von beiden.
3. **Bugs/Fallen** → zusaetzlich `~/proggs/bugs/<kategorie>/<bereich>.md`
   Je Eintrag: Symptom, Ursache, betroffene Versionen, funktionserhaltender Fix, Quelle.
   Ebenfalls Kurzcheck UND Volltext. Neuer Bereich → Datei anlegen + README-Index ergaenzen.
   Ein Bug gehoert in den Almanach, **nicht nur** in die Best Practices.
4. **Committen und pushen** — nur die eigenen Dateien namentlich stagen.
5. **Schritt 9** (Hook-Registrierung) anschliessen, wenn ein Almanach-Bereich neu war.

**Was das Ergebnis liefern muss, damit Schritt 8 ueberhaupt gehen kann:** Jeder Researcher markiert in
seiner Antwort `BEST-PRACTICES-KANDIDATEN:` und `BUG-KANDIDATEN:` (mit URLs + Versionen; wenn nichts:
`KEINE`). Das Einarbeiten macht immer der Hauptagent — nie mehrere Researcher schreiben parallel.

Bei Delegation macht Schritt 8 der aufrufende Skill mit dem zurueckgegebenen Ergebnis; er bekommt die
Checkliste ueber das `persistenz_ziel`-Feld mitgegeben.

### Schritt 9 — Hook-Registrierung (PFLICHT, der allerletzte Schritt) ⭐

**Immer wenn Schritt 8 einen Almanach-Bereich (`bugs/<kategorie>/<bereich>.md`) und/oder seine
Best-Practices-Gegenseite NEU angelegt oder erweitert hat, wird als ALLERLETZTER Schritt der neue
Bereich in allen DREI Almanach-Hooks registriert.** Das ist nicht optional — ohne diesen Schritt
ist die Recherche-Pipeline unvollstaendig: der Almanach laege als totes Wissen im Repo, ohne dass
ein Hook ihn einblendet, im Prompt erkennt oder passende Edits absichert (kein Compound-Effekt).

Kurz die drei Hooks (volle Anleitung + Tests + Spiegelung: **`references/hook-registrierung.md`**):

1. **`bug-almanac-index`** (SessionStart) — listet **rekursiv** alle Almanache → **automatisch**,
   nur verifizieren, dass die Datei in `bugs/<kategorie>/` liegt. Keine Code-Aenderung.
2. **`bug-almanac-hint.py`** (UserPromptSubmit) — kuratiertes `AREAS`-Dict: **Eintrag ergaenzen**
   mit Synonym-Stichwoertern, je in **Leerzeichen- UND Bindestrich-Variante** (Substring-Matching;
   deutsche Eingaben nutzen oft Bindestriche). `py_compile` + Positiv-Tests (beide Schreibweisen) +
   ein leerer Negativ-Test. Kollidierende Stichwoerter fremder Bereiche vermeiden.
3. **`bug-almanac-guard`** (PreToolUse) — nur erweitern, wenn der Bereich ein **klares Datei-Muster**
   hat; Konzept-/Querschnitts-Bereiche (wie `agents/…`) bewusst NICHT erzwingen, nur dokumentieren.

Danach JEDE geaenderte Hook-Datei in **beide** Spiegel-Orte (`claude-code-setup/hooks` UND
`Umgebung/Hooks`) 1:1 spiegeln (harness-mirror-Pflicht; bei `.ps1`/`.sh` beide Varianten; kein
`__pycache__`), nur eigene Dateien namentlich stagen, committen, fetch+rebase, pushen.

**Wer registriert:** wer in Schritt 8 persistiert hat. Bei `adhoc`/Direkt-Aufruf macht es dieser
Skill selbst; bei Delegation der aufrufende Skill mit dem zurueckgegebenen Ergebnis. So oder so ist
die Hook-Registrierung der verbindliche Abschluss der gesamten Recherche→Persistenz-Pipeline.

> Hinweis: Hooks editieren ist Harness-Arbeit — der `bug-almanac-guard` verlangt vorher den
> Hooks-Almanach (Stufe C, Volltext) + Best-Practices. Details in `references/hook-registrierung.md`.

---

## Engine-Wahl-Spickzettel (Detail in der Policy-Regel)

- **A (Firecrawl + DeepSeek V4 Flash @ Makora):** volle Seiten, tiefe Einzelrecherche; Firecrawl-Free-
  Credits; **nur 2 parallel** (das Limit kommt von Firecrawl, nicht vom Auswerte-Modell).
- **B (dasselbe DeepSeek-Modell mit `:online`):** Snippets statt Vollseiten, **bis 7 parallel**
  (Continuous-Spawning; `:online` verteilt selbst auf mehrere Modell-Provider → last-stabil, A/B-Test
  2026-06-21: 10 echt-parallel sauber; `or-research.py`-Retry faengt den intermittenten Leak §42),
  pay-per-use, kein Monatslimit. Modell `deepseek/deepseek-v4-flash-0731:online` — KEINE explizite
  Such-Engine angeben. Eskalation mit mehr Denkkraft: `z-ai/glm-5.2:online`.
- **C (Schwarm auf dem Host-Modell):** nur wenn Frank es ausdruecklich waehlt; 7 parallel,
  Continuous-Spawning. **Claude Code** (`CLAUDECODE=1`) → Sonnet-5-Schwarm, `model:"sonnet"` PFLICHT
  pro Aufruf. **OpenCode** → Schwarm auf dem aktuellen Session-Modell, **kein** `model:`-Override,
  die Modelle recherchieren mit ihrer eigenen Internet-Anbindung (Details §4a in `research-strategy.md`).

---

## Was NIEMALS passieren darf

- ❌ Frage 1 ueberspringen, weil der Schnellmodus/Freimodus aktiv ist — sie ist eine Kosten-Freigabe (Block 0.0)
- ❌ Frage 1 selbst beantworten oder die `research-approved.flag` ohne Franks Antwort setzen
- ❌ Ohne `AskUserQuestion` einfach loslaufen, statt die vier Optionen als Klartext zu stellen und zu warten
- ❌ Schritt 8 (Persistenz) oder Schritt 9 (Hook-Registrierung) weglassen — in KEINEM Arbeitsmodus
- ❌ Sich darauf verlassen, dass `research-strategy.md`/`research-persistence.md` geladen sind (in `minimal` nicht)
- ❌ Recherche-Auftrag als Fliesstext annehmen statt ueber das benannte Feld-Schema (Verlustgefahr)
- ❌ Bei bug/best_practice ohne `version_anker` recherchieren (falsche Fix-Stati)
- ❌ Auf ganze Wellen warten statt Continuous-Spawning (Zeitverlust — die oberste Regel)
- ❌ Mehr als 2 Firecrawl-Researcher gleichzeitig (Free-Limit → 429)
- ❌ Bei Engine B eine explizite Such-Engine (`parallel`/`exa`/`firecrawl`) als 3. Argument angeben — `:online` regelt die Suche selbst (Modell-Suffix, kein `tools`-Block)
- ❌ In OpenCode einen Engine-C-Subagenten mit `model:"sonnet"` spawnen — dort MUSS das Session-Modell selbst recherchieren (kein `model:`-Override)
- ❌ In Claude Code einen Engine-C-Subagenten OHNE `model:"sonnet"` spawnen — er faellt sonst auf ein unbestimmtes Modell zurueck
- ❌ Den Makora-Anbieter-Pin von Hand am Aufruf vorbeimogeln — er steht als Default in den Skripten (`MM_PROVIDER`/`OR_PROVIDER`)
- ❌ Mehrere Engine-B-Parallel-Laeufe ohne eigenes `OR_OUTDIR` je Lauf (sie ueberschreiben sich)
- ❌ Den Auto-Cleanup in `research-swarm.py` entfernen/umgehen — ohne ihn schlagen alte `run-<i>/answer.json` fehlgeschlagener Researcher als FREMDE Themen durch (Vorfall 2026-06-25)
- ❌ Selbsttests "ob das System geht" — die Pipeline ist verifiziert
- ❌ Nach den Skripten/Keys suchen — die Pfade stehen in Block 0
- ❌ Funde an einem Cap abschneiden (lossless: in Datei auslagern); `cap` steuert nur Inline-Menge
- ❌ Auswertung mit Linien-Wirrwarr statt ruhiger Bloecke
- ❌ Das Rueckgabe-Schema des Aufrufers ignorieren und "generische Bullets" liefern
- ❌ Einen neu angelegten/erweiterten Almanach-Bereich persistieren, ohne ihn in den drei Almanach-Hooks zu registrieren (Schritt 9) — totes Wissen im Repo, kein Compound-Effekt
- ❌ Beim `hint`-Eintrag nur Leerzeichen-Schreibweisen aufnehmen (Bindestrich-Varianten fehlen → deutsche Eingaben triggern nicht) oder die Hook-Aenderung nicht in beide Spiegel-Orte spiegeln
