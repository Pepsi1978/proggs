# Recherche-Strategie: Token-sparend recherchieren (Firecrawl+MiniMax vs. Sonnet-Schwarm) — KRITISCH

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-20. Gilt AUTOMATISCH in JEDER Session, fuer
> JEDE Web-Recherche und fuer ALLE Researcher/Skills, die im Web suchen (`research`-Skill,
> `bug-almanach-recherche`, `best-practices`, `researcher`-Agent, `deep-research`, eigene
> WebSearch/WebFetch-Rechercheauftraege). Repo-Spiegelung:
> `~/proggs/claude-code-setup/rules/research-strategy.md` (harness-mirror-Pflicht).
>
> Entstanden aus dem 3-Wege-Token-Test 2026-06-20: Firecrawl+MiniMax-M3-Pipeline verbraucht
> **~100x weniger teure Claude-Token** als ein Opus-Researcher (~2.400 statt ~249.000, damaliger
> Test), bei gleicher oder besserer (ehrlicherer) Qualitaet. Ergaenzt [[research-persistence]]
> (Einarbeiten der Ergebnisse) und nutzt die Pipeline `~/proggs/mm-research.py`.
>
> **⚡ Update 2026-07-01 (Frank):** Am selben Tag ist **Sonnet 5** erschienen — mit **nativem
> 1M-Kontext** (genau die Eigenschaft, die vorher nur Opus 4.8[1m] fuer den Eskalations-Schwarm
> bot). Die dritte Eskalationsstufe (bisher "Opus-Schwarm") heisst ab sofort **Sonnet-5-Schwarm**:
> Web-Researcher der Eskalationsstufe C laufen auf `model: "sonnet"` (loest aktuell zu Sonnet 5
> auf) statt auf Opus, mit **Effort "high"** (Standard-Session-Effort, siehe §4a). Betrifft NUR
> diese eine Eskalationsstufe — alle anderen Subagents (coder, tester, debugger, architect etc.)
> bleiben unveraendert auf Opus[1m], siehe `~/.claude/rules/highest-model-everywhere.md`.

---

## 1. Die Pflicht-Frage VOR jeder Web-Recherche (Credit-Kontrolle via AskUserQuestion)

**Vor JEDER Web-Recherche MUSS Frank per `AskUserQuestion` (anklickbares Multiple-Choice) gefragt
werden, WIE recherchiert wird** — niemals automatisch losrecherchieren.

> ## 🛑 ABSOLUT — DAS PROTOKOLL WIRD NIE ÜBERSPRUNGEN (Frank, 2026-06-22, nach Vorfall)
> Frank-Wortlaut: *„Bei der Research, beim Recherchieren, bitte IMMER das Protokoll einhalten.
> Das ist mir sehr wichtig."* Das Protokoll gilt **ausnahmslos bei JEDER Recherche**, egal über
> welchen übergeordneten Skill sie ausgelöst wird (`bug-almanach-recherche`, `best-practices`,
> `almanach-update`, `best-practices-update`, `direktiven-recherche`, `superintelligenz`, eigener
> Rechercheauftrag …). Die drei Schritte sind **Pflicht UND sichtbar**, in dieser Reihenfolge:
> 1. **Empfehlung** (welcher Weg, 1 Satz Begründung).
> 2. Die **4-Fragen-`AskUserQuestion`** — anklickbar: **A** Firecrawl+MiniMax · **B** Eskalation
>    OpenRouter `:online` · **C** Sonnet-5-Schwarm · **D** Freitext. **Diese 4 Fragen kommen IMMER, jedes Mal.**
> 3. Der **`research`-Skill** wird gestartet — mit beschrifteten, mitlesbaren Researchern
>    (Continuous-Spawning, Live-Zwischenfazit). **Egal welcher Skill die Recherche braucht — der
>    `research`-Skill wird IMMER mitgestartet, nie umgangen.**
> 4. **Frage 2 (Eskalation) kommt NACH JEDER abgeschlossenen Firecrawl-Recherche (Engine A) — IMMER,
>    automatisch.** Anklickbar fragen, ob noch ein Eskalations-Lauf gewuenscht ist (B = OpenRouter
>    `:online`, C = Sonnet-5-Schwarm, „nein, fertig", Freitext). **Diese Eskalations-Frage NIE weglassen.**
>    (Frank, 2026-06-22, **2. Vorfall**: nach der Bug-Almanach-Firecrawl-Recherche fehlte Frage 2 —
>    Frank haette gerne einen Eskalationslauf gehabt. Darf nie wieder fehlen.)
>
> **NIEMALS** `mm-research.py`/`or-research.py` direkt/ad-hoc (z.B. im Hintergrund) starten, **NIEMALS**
> die 4 Fragen überspringen — **auch nicht**, wenn Frank vorher beiläufig eine Engine genannt hat
> (eine beiläufige Nennung wie „nimm Firecrawl" ersetzt das **anklickbare** Protokoll NICHT, und die
> `research-approved.flag` wird NIE eigenmächtig ohne Franks Klick gesetzt). Der Standard ist bewusst
> optisch auf Franks Mitlesbarkeit + Credit-Kontrolle ausgelegt.
> **Vorfall 2026-06-22:** Bei der Samba/mem0-Bug-Almanach-Recherche wurde `mm-research.py` ad-hoc im
> Hintergrund gestartet und sowohl die 4-Fragen-`AskUserQuestion` als auch der `research`-Skill
> übersprungen — Frank war zu Recht verärgert. Das darf **nie wieder** passieren.
> (Abgrenzung: Bei NICHT-Recherche-Arbeiten gilt diese Strenge nicht; eine einzelne billige
> `WebSearch` zur schnellen Faktenprüfung mitten in einer Aufgabe bleibt frei. Sobald es ein echter
> Rechercheauftrag/Crawl/Researcher-Einsatz ist → volles Protokoll.) Memory: `feedback_research_always_via_protocol`.

### Empfehlung (PFLICHT — DIREKT VOR Frage 1)

**Bevor die `AskUserQuestion` kommt, IMMER eine kurze Empfehlung (3-4 Zeilen) geben**, welcher Weg
fuer GENAU DIESE Recherche-Art am sinnvollsten ist — A, B, C oder eine **Kombination** (z.B. „A, und
danach B als Eskalation" bei grossem Wissensschatz). Kurz begruenden (1 Satz, woran es liegt). Die
empfohlene Option dann in der `AskUserQuestion` als **erste** Option mit `(Empfohlen)` im Label.

**Heuristik fuer die Empfehlung:**

| Recherche-Art | Empfehlung |
|---------------|-----------|
| Schnelle Einzelfrage / 1 Thema / 1-2 Quellen reichen | **A** (Firecrawl + MiniMax, Free-Credits) |
| Grosser Wissensschatz / viele Unterthemen / breite Abdeckung | **A → dann B** (Firecrawl-Tiefe + parallele Breit-Eskalation) — oder direkt **B**, wenn Firecrawl-Credits knapp |
| Aktualitaet ueber viele Quellen, Snippets reichen, kein Monatslimit-Risiko | **B** (or-research, pay-per-use) |
| Firecrawl-Credits fast leer (Monatslimit nahe) | **B** statt A |
| Hoechste Korrektheit / subtile Logik / Geld egal | **C** (Sonnet-5-Schwarm) — ggf. zusaetzlich zu A/B als Zweitmeinung |
| Unklar / erst besprechen | **D** (Freitext) |

Die Empfehlung ist ein Vorschlag — Frank entscheidet final ueber die `AskUserQuestion`.

### Frage 1 (IMMER, via `AskUserQuestion`) — „Wie soll ich '<thema>' recherchieren?"

| Option | Weg | Werkzeug | Kosten |
|--------|-----|----------|--------|
| **A** | **Firecrawl + MiniMax M3 (max Thinking)** — Standard, volle Seiten | `mm-research.py` | Firecrawl-Free-Credits (1000/Mon) |
| **B** | **Eskalation: MiniMax M3 `:online` (max Thinking)** — OpenRouter-Websuche (web-Plugin) | `or-research.py … :online` | pay-per-use (~Cent), kein Monatslimit |
| **C** | **Sonnet-5-Schwarm** — teuer, nur bewusst | bestehende Researcher, `model:"sonnet"` + Effort "high" | Claude-Sonnet-5-Token |
| **D** | **[automatisches Freitext-Feld]** — etwas anderes / erst besprechen | — | — |

- **A UND B laufen IMMER mit max Thinking** (Pflicht) — der Schalter ist aber ENDPUNKT-abhaengig:
  **A** (`mm-research.py`, Anthropic-`/messages`) → `thinking:{type:"enabled","budget_tokens":<hoch>}`
  (live-getestet; M3 denkt adaptiv bis zum Budget, `max_tokens` > Budget). **B** (`or-research.py`, OpenRouter)
  → `reasoning:{effort:high}`. `{type:"adaptive"}` gilt NUR fuer den OpenAI-`/chat/completions`-Pfad, NICHT fuer `/messages`.
- **Option C (Sonnet-5-Schwarm) wird NUR genommen, wenn Frank sie ausdruecklich waehlt** — nie als
  Default, nie „weil es gruendlicher ist". Sie ist eine gleichwertige 3. Option, aber bewusst teuer.
  Seit 2026-07-01 laeuft sie auf **Sonnet 5** (`model:"sonnet"`, natives 1M-Kontext) statt Opus —
  siehe §4a fuer die genaue Modell-Mechanik.
- Technik: `AskUserQuestion` liefert A/B/C als Buttons; die automatische „Other"/Freitext-Wahl deckt
  **D** ab (Freitext → etwas anderes machen oder erst besprechen).

### Frage 2 (NUR nach einer abgeschlossenen Firecrawl-Research = Option A; dann IMMER, via `AskUserQuestion`)

Nach Abschluss einer Firecrawl-Research (Stufe A) IMMER nachfragen — „Noch eine zusaetzliche
Eskalations-Research?":

| Option | Bedeutung |
|--------|-----------|
| **A** | Ja — MiniMax M3 `:online` (max Thinking) als zusaetzliche Eskalation (`or-research.py … :online`) |
| **B** | Nein, fertig |
| **C** | Ja, mit Sonnet-5-Schwarm (teuer, nur bewusst) |
| **D** | [Freitext] |

Frage 2 entfaellt, wenn schon Stufe B (Option B) oder C gewaehlt wurde — sie haengt spezifisch an der
guenstigen Firecrawl-Stufe, um bei Bedarf gezielt zu eskalieren.

**Warum die Frage Pflicht ist (KRITISCH):** Firecrawl Free hat nur **1.000 Seiten/Monat**. Bei vielen
Recherchen ist das Kontingent schnell weg. Frank entscheidet pro Recherche bewusst, womit recherchiert
wird — das ist ein **Feature, kein Reibungsverlust**. NIEMALS automatisch losrecherchieren ohne Frage 1.

Ausnahme von der Frage: eine **einzelne, billige `WebSearch`** (kein Firecrawl-Crawl) zur schnellen
Faktenverifikation mitten in einer laufenden Aufgabe ist frei (verbraucht keine Firecrawl-Credits).
Sobald es ein echter Rechercheauftrag / Crawl / Researcher-Einsatz ist → Frage 1 stellen.

---

## 2. Der Standard-Ablauf (zweistufige Arbeitsteilung)

```
Frank waehlt "Firecrawl + MiniMax":
  Stufe 1 — HOLEN:      Firecrawl holt die Quellen (Firecrawl-Credits, Go-Abo-unabhaengig)
                            │
  Stufe 2 — AUSWERTEN:  MiniMax M3 (max Thinking) filtert + bewertet quellentreu   ← mm-research.py
                            │ kompakte, quellentreue Antwort (~2k Token)
                            ▼
  Stufe 3 — EINARBEITEN: Der Hauptagent synthetisiert + arbeitet in Almanach/Best-Practices ein
                                                                        (Direktive [[research-persistence]])
```

**Kerngedanke:** MiniMax macht die **token-schwere** Quellenarbeit (Rohdaten laufen NIE durch den teuren
Hauptagent-Kontext); der Hauptagent zahlt nur fuer die ~2k-Token-Synthese. Werkzeug: `python3 ~/proggs/mm-research.py
"<frage>" [n]` (Firecrawl-Suche → MiniMax-M3-Auswertung; Rohdaten + Thinking landen in `~/.mm-research/`,
nicht im Claude-Kontext). Mechanik/Fallen: `best-practices/opencode/go-recherche-modelle.md` §5,
`bugs/opencode/opencode-cli.md` §14.

**ZWEI Werkzeuge — beide lagern die Quellenarbeit aus dem teuren Hauptagent-Kontext aus:**

| Werkzeug | Suche | Inhalt | Kosten | Parallelitaet | Wann |
|----------|-------|--------|--------|---------------|------|
| `mm-research.py` | Firecrawl | **volle Seiten** (Scrape) | Free **1000/Mon** (knapp) | **max 2** (Free) | tiefe Einzelrecherche, solange Credits da |
| `or-research.py … :online` | OpenRouter `:online` (web-Plugin, Such-Engine intern parallel.ai) | **Top-N Snippets** (~2-4k Zeichen je Treffer, KEINE ganzen Seiten) | **$0.005/Suche** (bis 10 Treffer) + Modell-Token; **kein Monatslimit** | **bis 7** (`:online` last-stabil, A/B-Test 2026-06-21 §3a; Retry faengt Leak §42) | Eskalation, wenn Firecrawl-Credits knapp; Continuous-Spawning bis 7 |

**⚡ Update 2026-06-21 — Engine B nutzt jetzt `:online`, NICHT mehr das `web_search`-Server-Tool.**
Das `:online`-Plugin (`<modell>:online`) gilt laut OpenRouter-Doku zwar als „deprecated", ist im A/B-Test
aber **empirisch STABILER bei hoher Parallelitaet**: 10 echt-parallele `:online`-Researcher liefen sauber,
waehrend das `web_search`-Server-Tool bei ~7 parallel 3/7 zerlegte (Almanach `bugs/apis/openrouter-api.md`
#13/#41). Grund: `:online` verteilt selbst auf mehrere Modell-Provider → kein einzelner Pfad ueberlastet.
Such-Engine intern weiterhin **parallel.ai** (sichtbar nur ueber Generation-API `web_search_engine`).
Frank-Entscheidung: Engine B = `:online`, bis 7 parallel (Retry faengt den intermittenten Leak §42).
Der `web_search`-Server-Tool-Pfad bleibt in `or-research.py` als Fallback erhalten (Modell ohne `:online`-Suffix).

**Kosten Web-Suche — verifiziert 2026-06-20, openrouter.ai/docs:** $0.005 PRO Such-Anfrage (nicht pro Seite,
nicht pro 1000!), inkl. bis 10 Treffer; >10 +$0.001/Treffer (max 25). „Treffer" = Snippet, nicht Volltext.
Modell kann agentisch mehrfach suchen (je $0.005, mit `max_total_results` deckelbar). Grob ~$0.008 pro
Recherche-Anfrage mit MiniMax M3 (≪ 1 Cent). 7-Researcher-Lauf ≈ $0.12 (vs. Opus-Researcher ≈ $7+).

**LIVE-Test 2026-06-20 (MiniMax M3 + engine=parallel):** Das Modell suchte **agentisch 10× selbst** (nicht 1×)
→ real **$0.069**, 49 Quellen, korrekte+ehrliche Antwort (korrekter als der Opus-Researcher, der beim
Schema falsch lag). **Lehre:** agentische Mehrfachsuche treibt die Kosten — `max_total_results`/`max_results`
als Deckel setzen (or-research.py: Default `engine=parallel`, env `OR_MAX_TOTAL` Default 10). **M3-Thinking je
Endpunkt:** `/messages` (mm-research) = `{type:"enabled","budget_tokens":N}` (live-getestet, M3 denkt adaptiv bis N,
`max_tokens`>N); `/chat/completions` = `{type:"adaptive"}`; ueber OpenRouter (or-research) = `reasoning:{effort:high}`
(lief, 3.578 reasoning-Token). Kein numerisches „max"-Level — nur enabled/adaptive vs. disabled.

---

## 3. Firecrawl Free — max 2 Researcher PARALLEL (nicht 7!)

Firecrawl Free erlaubt nur **2 gleichzeitige Requests** (+ 5 Suchen/Minute, 1.000 Credits/Monat —
verifiziert 2026-06-20, docs.firecrawl.dev/rate-limits). Das aendert das Schwarm-Muster:

| | Sonnet-5-Researcher-Schwarm (Eskalation C) | Firecrawl + MiniMax (neu) |
|---|----------------------------------|---------------------------|
| Parallelitaet | bis **7** gleichzeitig (Continuous-Spawning) | **max 2** gleichzeitig |
| Nachschub | sofort den naechsten starten | erst wenn 2 fertig → naechste 2 (Continuous-Spawning mit **2**) |

**Pflicht bei Firecrawl-Recherchen mit mehreren Unterthemen** (z.B. Bug-Almanach mit 7 Aspekten):
NIE 7 Firecrawl-Calls auf einmal — konstant **2 gleichzeitig** im Continuous-Spawning (siehe §3a),
bis alle Unterthemen durch sind. Sonst Rate-Limit (429) / verschwendete Credits.

### 3a. Continuous-Spawning ist die OBERSTE Researcher-Regel (ALLE Engines)

**Sobald EIN Researcher fertig ist, wird SOFORT der naechste aus der Warteschlange gestartet —
NIEMALS auf eine ganze Welle warten.** Es laufen konstant so viele gleichzeitig, wie die Engine
erlaubt. Kein Wellen-Barrier, kein Leerlauf, kein Zeitverlust durch "warten bis alle N fertig sind".
Das gilt fuer JEDE Engine, nicht nur Firecrawl:

| Engine | Konstant gleichzeitig | Nachschub-Regel |
|--------|----------------------|-----------------|
| A — Firecrawl (mm) | **2** (hartes Free-Limit) | einer fertig → sofort der naechste (nie auf beide warten) |
| B — OpenRouter (or), `:online` | **7** (`:online` verteilt selbst auf mehrere Modell-Provider → last-stabil; A/B-Test 2026-06-21: 10 echt-parallel sauber. Das alte `web_search`-Tool zerlegte bei 7 → darum `:online`. Retry faengt Leak §42) | einer fertig → sofort der naechste |
| C — Sonnet-5-Schwarm | **7** | einer fertig → sofort der 7. neu (nie auf alle 7 warten) |

Beispiel Engine C: laufen 7 und einer kommt zurueck → es sind nur noch 6 → sofort einen neuen starten,
damit wieder 7 laufen. Genauso bei OpenRouter `:online` (**7**) und Firecrawl (**2**). Diese Regel ist tief
im Gesamtsystem verankert (auch `agent-and-researcher-rules.md`), nicht nur im `research`-Skill.

**Durchsetzung statt Disziplin (Engine A+B):** Eine Regel ist nur advisory — darum laeuft Engine A/B IMMER
ueber `~/proggs/research-swarm.py`, das per `ThreadPoolExecutor(max_workers=N)` KONSTANT N parallel haelt und
bei jedem fertigen Researcher SOFORT den naechsten aus der Queue zieht (Continuous-Spawning im CODE erzwungen,
deterministisch; Limits hart: **A=2, B=7**, Ueberanforderung wird gedeckelt+gewarnt). So ist „nie in Wellen"
nicht von Claudes Disziplin abhaengig. Nur Engine C (Sonnet-5-Schwarm, Agent-Tool) orchestriert der
Hauptagent von Hand (Pattern im `research`-Skill: erst 7, bei jeder Completion sofort den naechsten).

---

## 4. Eskalation — 3 Stufen (billig → teuer)

Wenn die MiniMax-Auswertung **„unsicher / widerspruechlich / Quellen reichen nicht"** meldet, ODER
Frank ausdruecklich gruendlicher will:

```
Stufe A:  MiniMax M3 (max Thinking) auf Firecrawl-Quellen          ← Standard (mm-research.py); Free-Credits
   ↓ reicht nicht
Stufe B:  MiniMax M3 :online (OpenRouter Go, web-Plugin)          ← or-research.py … :online; pay-per-use, bis 7 parallel
   ↓ reicht nicht
Stufe C:  Sonnet-5-Schwarm (Agent-Tool, model:"sonnet")            ← teuerste Stufe, nur Hard-Cases
```

**Stufe B = `or-research.py … :online`** (OpenRouter `:online`-Plugin): ein **1M-Kontext-Modell** mit eigener
Websuche (parallel.ai-Snippets, ANDERE Suchquelle als Firecrawl = Diversitaet), **bis 7 parallel** (last-stabil).
**Perplexity ist RAUS** — nur 200k Kontext, bricht bei grossen Recherchen (genau der Grund fuer Opus-1M bei
Schwaermen). **Modell-Default:** `minimax/minimax-m3:online` (guenstig, A/B-getestet 2026-06-21);
**Eskalation:** `z-ai/glm-5.2:online` (mehr Denkkraft). `reasoning:high` ist im Werkzeug eingebaut — M3 denkt
bei `:online` ohnehin (~900 Tok), das Setting aendert die Qualitaet kaum (A/B-getestet), bleibt aber konsistent
mit der „A+B max Thinking"-Policy.

---

## 4a. Stufe C = Sonnet-5-Schwarm — Modell-Mechanik (seit 2026-07-01)

**Was sich geaendert hat:** Bis 2026-06-30 lief Stufe C ueber `subagent_type:general-purpose`
ohne explizites `model:` — die Researcher liefen automatisch auf Opus[1m], weil die globale
Umgebungsvariable `CLAUDE_CODE_SUBAGENT_MODEL=opus[1m]` JEDEN Subagent zwang (unabhaengig vom
`model:`-Parameter). Seit Sonnet 5 (erschienen 2026-06-30, natives 1M-Kontext) steht
`CLAUDE_CODE_SUBAGENT_MODEL` auf `inherit` (normale Modell-Aufloesung statt Zwangs-Override,
siehe `~/.claude/rules/highest-model-everywhere.md`). Deshalb ist bei Stufe-C-Spawns jetzt ein
**explizites `model:`-Argument PFLICHT** — ohne es wuerden die Researcher auf das Session-Modell
des Hauptagenten zurueckfallen, nicht zwingend auf Sonnet 5.

**Pflicht-Parameter fuer JEDEN Stufe-C-Agent-Tool-Aufruf:**

| Parameter | Wert | Warum |
|-----------|------|-------|
| `model` | `"sonnet"` | Alias, loest aktuell zu Sonnet 5 auf (`claude-sonnet-5`) — folgt automatisch dem jeweils neuesten Sonnet, genau wie `"opus"` bei anderen Agents dem neuesten Opus folgt |
| Effort | **"high"** (Standard) | Kein eigener Effort-Parameter am Agent-Tool fuer Ad-hoc-`general-purpose`-Aufrufe — der Researcher erbt automatisch den globalen Session-Effort (`effortLevel: "high"` in `settings.json`, CLAUDE.md-Standard). Nichts zusaetzlich zu setzen, solange `effortLevel` global "high" bleibt |

**Wichtig — Geltungsbereich:** Diese Umstellung betrifft AUSSCHLIESSLICH die Web-Research-
Eskalationsstufe C (Engine C in diesem Dokument). Alle anderen Custom-Agents (architect, debugger,
coder, tester, code-reviewer, optimizer, ui-polisher, batch-reviewer, der `researcher`-Agent
selbst fuer Stufe A/B-Vorarbeiten, etc.) pinnen jetzt explizit `model: opus[1m]` in ihrem eigenen
Frontmatter (statt sich auf die globale Umgebungsvariable zu verlassen) — fuer sie aendert sich
dadurch nichts.

---

## 5. Gilt fuer ALLE Recherche-Skills/Agenten

Diese Regel ist die **Policy-Schicht** (das OB/WOMIT). Die AUSFUEHRUNG uebernimmt seit 2026-06-21
der zentrale **`research`-Skill** (`~/.claude/skills/research/`, das WIE). Alle Recherche-Skills/
Agenten **delegieren** an ihn ueber den **Uebergabe-Block** (Research-Auftrag-Schema), statt das
Recherche-"WIE" 8x zu duplizieren. So bleibt jede Einheit genauso gut wie vorher — der research-Skill
bekommt ihr Profil (Modus, Engine, Anzahl, Rueckgabe-Schema, Persistenz-Ziel) als Parameter.

| Skill/Agent | zerlegungs_modus | engine | rueckgabe_schema |
|-------------|------------------|--------|------------------|
| `bug-almanach-recherche` (Skill) | feste_liste (5–7 Aspekte) | A→C | `bug` |
| `almanach-update` (Skill) | feste_liste | A→C | `bug` |
| `best-practices` (Skill) | feste_liste | A→C | `best_practice` |
| `best-practices-update` (Skill) | feste_liste | A→C | `best_practice` |
| `direktiven-recherche` (Skill + Agent) | feste_liste (5 Researcher) | C | `direktive` |
| `superintelligenz` (Agent) | iterativ_wellen | C | `superintelligenz` |
| `intelligence-researcher` (Agent) | selbst_generierend (5 Dim.) | C | `superintelligenz` |
| `forschungsagent` (Agent) | feste_liste | C | `integrationsplan` |
| `researcher` (Agent) | (Schwarm-Baustein) | A/B/C | `adhoc` (+ KANDIDATEN-Bloecke) |
| `deep-research` (externes Plugin) | — | — | Orchestrator stellt Empfehlung + Frage 1, bevor er es startet |
| Eigener Web-Rechercheauftrag des Hauptagenten | adhoc | A/B/C | `adhoc` |

> **Falle:** Der `superintelligenz`-**Skill** (≠ Agent) ist NUR Leitbild/Checkliste, KEIN
> Recherche-Workflow → bekommt KEINEN Uebergabe-Block.

**Der Uebergabe-Block** (steht in jedem delegierenden Skill/Agent): "Fuer ALLE Web-Recherchen den
`research`-Skill laden und ihm diesen Research-Auftrag uebergeben: [thema, zweck, zerlegungs_modus,
unterthemen[], version_anker, engine, anzahl/wellen/cap, rueckgabe_schema, persistenz_ziel,
dup_quelle, nacharbeit_aufrufer]. Mit dem Ergebnis im rueckgabe_schema hier weiterarbeiten."

**Stand 2026-06-21 (UMGESETZT):** Zentraler `research`-Skill gebaut (#47027); §5 auf Delegation
umgestellt; Continuous-Spawning als oberste Regel (§3a). Empfehlung (oben in §1) + Frage 1 + (bei A)
Frage 2 bleiben Pflicht und laufen VOR der Delegation (Policy-Schicht).

---

## 6. Was NIEMALS passieren darf

- ❌ Eine Firecrawl-/Crawl-Recherche starten, OHNE Frank vorher zu fragen (Firecrawl/MiniMax vs. Sonnet-5-Schwarm)
- ❌ Mehr als **2** Firecrawl-Researcher gleichzeitig starten (Free-Limit; Rate-Limit/Credit-Verschwendung)
- ❌ Auf eine ganze Welle warten, statt sofort beim Fertigwerden eines Researchers den naechsten zu spawnen (Continuous-Spawning, §3a — Zeitverlust ist die haeufigste Schwarm-Suende)
- ❌ Firecrawl-Rohdaten (gecrawlte Seiten) ungefiltert in den teuren Hauptagent-Kontext laden — immer erst MiniMax
- ❌ Bei einem unsicheren MiniMax-Ergebnis stillschweigend halluzinieren statt zu eskalieren
- ❌ Test-Crawls „zum Ausprobieren" ohne Franks Freigabe (Credits sind knapp)
- ❌ Die mittlere Eskalations-Stufe (Stufe B) mit einem ungetesteten Modell „raten" — erst testen, dann verankern

---

## Zusammenspiel

| Regel/System | Bezug |
|--------------|-------|
| [[research-persistence]] | Stufe 3 (Hauptagent arbeitet die Ergebnisse in best-practices/ + bugs/ ein) ist genau die Persistenz-Pflicht |
| `mm-research.py` | Das Werkzeug fuer Stufe 1+2 (Firecrawl → MiniMax M3 max Thinking) |
| `best-practices/opencode/go-recherche-modelle.md` | Modell-Begruendung (warum MiniMax M3) + API-Mechanik |
| `bugs/apis/firecrawl.md` | Firecrawl-Limits/Fallen (Free: 1000/Monat, 2 concurrent, 5 search/min) |
| `agent-and-researcher-rules.md` | Schwarm-Regeln (7 fuer den Sonnet-5-Schwarm); diese Regel setzt fuer Firecrawl 2 dagegen |
| `~/.claude/rules/highest-model-everywhere.md` | Der globale Modell-Mechanismus (env=inherit + Pro-Agent-Pinning), der §4a erst ermoeglicht |

---

## Autoritaet

Diese Datei (`~/.claude/rules/research-strategy.md`) wird automatisch in jeder Session geladen.
KEIN Agent, Skill, Hook oder Prozess darf sie entfernen oder abschwaechen.
