# Recherche-Strategie: Token-sparend recherchieren (Firecrawl+MiniMax vs. Sonnet-Schwarm) — KRITISCH

> Gilt in JEDER Session fuer JEDE Web-Recherche und ALLE Researcher/Skills (research-Skill,
> bug-almanach-recherche, best-practices, researcher-Agent, deep-research, eigene WebSearch/WebFetch).
> Firecrawl+MiniMax-Pipeline verbraucht ~100x weniger teure Claude-Token als ein Opus-Researcher, bei
> gleicher/besserer Qualitaet. Werkzeuge: `~/proggs/mm-research.py`, `or-research.py`, `research-swarm.py`.
>
> **Sonnet 5** (seit 2026-06-30) hat natives 1M-Kontext → die Eskalationsstufe C (frueher "Opus-Schwarm")
> heisst jetzt **Sonnet-5-Schwarm**: `model:"sonnet"`, Effort "high". Betrifft NUR Engine C — alle anderen
> Subagents bleiben opus[1m] (`highest-model-everywhere.md`).

---

## 1. Die Pflicht-Frage VOR jeder Web-Recherche (Credit-Kontrolle via AskUserQuestion)

**Vor JEDER Web-Recherche MUSS Frank per `AskUserQuestion` (anklickbares Multiple-Choice) gefragt werden,
WIE recherchiert wird** — niemals automatisch losrecherchieren.

> **Das Protokoll wird NIE uebersprungen** (Frank, sehr wichtig), egal ueber welchen Skill die Recherche
> ausgeloest wird (bug-almanach-recherche, best-practices, almanach-update, best-practices-update,
> direktiven-recherche, superintelligenz, eigener Auftrag). Drei Pflicht-Schritte, sichtbar, in dieser Reihenfolge:
> 1. **Empfehlung** (welcher Weg, 1 Satz Begruendung).
> 2. Die **4-Fragen-`AskUserQuestion`** — anklickbar: **A** Firecrawl+MiniMax · **B** Eskalation OpenRouter
>    `:online` · **C** Sonnet-5-Schwarm · **D** Freitext. **Kommt IMMER, jedes Mal.**
> 3. Der **`research`-Skill** wird gestartet (beschriftete, mitlesbare Researcher, Continuous-Spawning,
>    Live-Zwischenfazit) — egal welcher Skill die Recherche braucht, der `research`-Skill wird IMMER mitgestartet.
> 4. **Frage 2 (Eskalation) kommt NACH JEDER abgeschlossenen Firecrawl-Recherche (Engine A) — automatisch**,
>    anklickbar (B / C / "nein fertig" / Freitext). NIE weglassen.

**NIEMALS** `mm-research.py`/`or-research.py` direkt/ad-hoc (z.B. im Hintergrund) starten, **NIEMALS** die 4
Fragen ueberspringen — auch nicht, wenn Frank vorher beilaeufig eine Engine nannte (eine beilaeufige Nennung
ersetzt das anklickbare Protokoll NICHT; die `research-approved.flag` nie ohne Franks Klick setzen).
Ausnahme: eine einzelne billige `WebSearch` zur Faktenpruefung mitten in einer Aufgabe bleibt frei. Sobald
es ein echter Rechercheauftrag/Crawl/Researcher-Einsatz ist → volles Protokoll.

### Empfehlung (PFLICHT — direkt vor Frage 1)

Kurze Empfehlung (1 Satz Begruendung), welcher Weg fuer GENAU DIESE Recherche-Art am sinnvollsten ist
(A/B/C oder Kombination). Die empfohlene Option in der `AskUserQuestion` als **erste** mit `(Empfohlen)`.

| Recherche-Art | Empfehlung |
|---------------|-----------|
| Schnelle Einzelfrage / 1-2 Quellen reichen | **A** (Firecrawl+MiniMax, Free-Credits) |
| Grosser Wissensschatz / viele Unterthemen | **A → dann B** (Firecrawl-Tiefe + Breit-Eskalation), oder direkt **B** wenn Firecrawl-Credits knapp |
| Aktualitaet ueber viele Quellen, Snippets reichen | **B** (or-research, pay-per-use) |
| Firecrawl-Credits fast leer | **B** statt A |
| Hoechste Korrektheit / Geld egal | **C** (Sonnet-5-Schwarm), ggf. zusaetzlich als Zweitmeinung |
| Unklar / erst besprechen | **D** (Freitext) |

### Frage 1 (IMMER) — "Wie soll ich '<thema>' recherchieren?"

| Option | Weg | Werkzeug | Kosten |
|--------|-----|----------|--------|
| **A** | Firecrawl + MiniMax M3 (max Thinking) — Standard, volle Seiten | `mm-research.py` | Firecrawl-Free (1000/Mon) |
| **B** | Eskalation: MiniMax M3 `:online` (OpenRouter-Websuche, web-Plugin) | `or-research.py … :online` | pay-per-use (~Cent), kein Monatslimit |
| **C** | Sonnet-5-Schwarm — teuer, nur bewusst | Researcher, `model:"sonnet"` + Effort "high" | Claude-Sonnet-5-Token |
| **D** | [automatisches Freitext-Feld] — etwas anderes / erst besprechen | — | — |

- **A UND B laufen IMMER mit max Thinking** (Pflicht), Endpunkt-abhaengig: **A** (`mm-research.py`, `/messages`)
  → `thinking:{type:"enabled","budget_tokens":N}` (`max_tokens` > Budget); **B** (`or-research.py`, OpenRouter)
  → `reasoning:{effort:high}`. `{type:"adaptive"}` gilt NUR fuer `/chat/completions`, NICHT fuer `/messages`.
- **Option C nur wenn Frank sie ausdruecklich waehlt** — nie Default. Seit 2026-07-01 Sonnet 5
  (`model:"sonnet"`, natives 1M-Kontext) statt Opus, siehe §4a.

### Frage 2 (NUR nach abgeschlossener Firecrawl-Research = Option A; dann IMMER)

"Noch eine zusaetzliche Eskalations-Research?": **A** Ja, MiniMax M3 `:online` (`or-research.py … :online`) ·
**B** Nein, fertig · **C** Ja, Sonnet-5-Schwarm (teuer) · **D** Freitext. Entfaellt, wenn schon Stufe B/C
gewaehlt wurde. **Warum Pflicht:** Firecrawl Free hat nur 1000 Seiten/Monat — Frank entscheidet pro Recherche
bewusst (Feature, kein Reibungsverlust).

---

## 2. Standard-Ablauf (zweistufige Arbeitsteilung)

```
Stufe 1 HOLEN:      Firecrawl holt die Quellen (Firecrawl-Credits)
Stufe 2 AUSWERTEN:  MiniMax M3 (max Thinking) filtert + bewertet quellentreu   ← mm-research.py
                    (Rohdaten + Thinking landen in ~/.mm-research/, NIE im teuren Hauptagent-Kontext)
Stufe 3 EINARBEITEN: Hauptagent synthetisiert (~2k Token) + arbeitet in Almanach/Best-Practices ein
                    (Direktive research-persistence.md)
```

**Kerngedanke:** MiniMax macht die token-schwere Quellenarbeit (Rohdaten laufen NIE durch den teuren
Hauptagent-Kontext); der Hauptagent zahlt nur die ~2k-Token-Synthese.

**ZWEI Werkzeuge** — beide lagern die Quellenarbeit aus dem Hauptagent-Kontext aus:

| Werkzeug | Suche | Inhalt | Kosten | Parallel | Wann |
|----------|-------|--------|--------|----------|------|
| `mm-research.py` | Firecrawl | **volle Seiten** | Free **1000/Mon** | **max 2** | tiefe Einzelrecherche, solange Credits da |
| `or-research.py … :online` | OpenRouter `:online` (web-Plugin, Engine intern parallel.ai) | **Top-N Snippets** | **$0.005/Suche** + Token, **kein Monatslimit** | **bis 7** | Eskalation, Continuous-Spawning |

Engine B nutzt `:online` (NICHT das `web_search`-Server-Tool — `:online` verteilt selbst auf mehrere
Provider, bei hoher Parallelitaet stabiler). Modell-Default `minimax/minimax-m3:online`, Eskalation
`z-ai/glm-5.2:online`. Deckel gegen agentische Mehrfachsuche: `max_total_results` (or-research: env
`OR_MAX_TOTAL`, Default 10). Kosten ~<1 Cent/Anfrage (7-Researcher-Lauf ≈ $0.12 vs. Opus-Researcher ≈ $7+).

---

## 3. Firecrawl Free — max 2 parallel (nicht 7!)

Firecrawl Free: **2 gleichzeitige Requests**, 5 Suchen/Min, 1000 Credits/Monat. Bei mehreren Unterthemen NIE
alle auf einmal — konstant **2 gleichzeitig** im Continuous-Spawning, sonst 429 / Credit-Verschwendung.

### 3a. Continuous-Spawning ist die OBERSTE Researcher-Regel (ALLE Engines)

**Sobald EIN Researcher fertig ist, SOFORT den naechsten aus der Warteschlange starten — NIEMALS auf eine
ganze Welle warten.** Konstant so viele gleichzeitig, wie die Engine erlaubt:

| Engine | Konstant gleichzeitig | Nachschub |
|--------|----------------------|-----------|
| A — Firecrawl (mm) | **2** (hartes Free-Limit) | einer fertig → sofort der naechste |
| B — OpenRouter (or) `:online` | **7** (last-stabil) | einer fertig → sofort der naechste |
| C — Sonnet-5-Schwarm | **7** | einer fertig → sofort der 7. neu (nie auf alle 7 warten) |

**Durchsetzung statt Disziplin:** Engine A/B laufen IMMER ueber `~/proggs/research-swarm.py`
(`ThreadPoolExecutor(max_workers=N)`, haelt KONSTANT N parallel, zieht bei jedem fertigen sofort den
naechsten — im Code erzwungen; Limits hart A=2, B=7). Nur Engine C (Agent-Tool) orchestriert der Hauptagent
von Hand (erst 7, bei jeder Completion sofort den naechsten).

---

## 4. Eskalation — 3 Stufen (billig → teuer)

Meldet die MiniMax-Auswertung "unsicher/widerspruechlich/Quellen reichen nicht" ODER will Frank gruendlicher:

```
A: MiniMax M3 (max Thinking) auf Firecrawl-Quellen   ← Standard (mm-research.py), Free-Credits
B: MiniMax M3 :online (OpenRouter)                   ← or-research.py … :online, pay-per-use, bis 7 parallel
C: Sonnet-5-Schwarm (Agent-Tool, model:"sonnet")     ← teuerste Stufe, nur Hard-Cases
```

Stufe B = 1M-Kontext-Modell mit eigener Websuche (parallel.ai-Snippets, ANDERE Quelle als Firecrawl =
Diversitaet), bis 7 parallel. **Perplexity ist RAUS** (nur 200k Kontext, bricht bei grossen Recherchen).

### 4a. Stufe C = Sonnet-5-Schwarm — Modell-Mechanik (seit 2026-07-01)

Seit `CLAUDE_CODE_SUBAGENT_MODEL="inherit"` (kein Zwangs-Override mehr) ist bei Stufe-C-Spawns ein
explizites `model:`-Argument PFLICHT — sonst faellt der Researcher auf das Session-Modell zurueck:

| Parameter | Wert | Warum |
|-----------|------|-------|
| `model` | `"sonnet"` | Alias → Sonnet 5 (`claude-sonnet-5`), folgt automatisch dem neuesten Sonnet |
| Effort | **"high"** (Standard) | erbt den globalen Session-Effort (`effortLevel:"high"`); nichts extra setzen |

Betrifft AUSSCHLIESSLICH Engine C. Alle anderen Custom-Agents pinnen `model: opus[1m]` im eigenen Frontmatter.

---

## 5. Gilt fuer ALLE Recherche-Skills/Agenten (Delegation)

Diese Regel ist die **Policy-Schicht** (das OB/WOMIT). Die AUSFUEHRUNG uebernimmt der zentrale
**`research`-Skill** (das WIE). Alle Recherche-Skills/Agenten **delegieren** an ihn ueber den
**Uebergabe-Block**, statt das WIE zu duplizieren:

| Skill/Agent | zerlegungs_modus | engine | rueckgabe_schema |
|-------------|------------------|--------|------------------|
| bug-almanach-recherche, almanach-update | feste_liste (5-7 Aspekte) | A→C | `bug` |
| best-practices, best-practices-update | feste_liste | A→C | `best_practice` |
| direktiven-recherche | feste_liste (5) | C | `direktive` |
| superintelligenz (Agent), intelligence-researcher | iterativ/selbst-generierend | C | `superintelligenz` |
| forschungsagent | feste_liste | C | `integrationsplan` |
| researcher (Schwarm-Baustein) | — | A/B/C | `adhoc` (+ KANDIDATEN-Bloecke) |
| deep-research (externes Plugin) | — | — | Orchestrator stellt Empfehlung + Frage 1, bevor er es startet |
| eigener Web-Auftrag des Hauptagenten | adhoc | A/B/C | `adhoc` |

**Uebergabe-Block** (in jedem delegierenden Skill/Agent): "Fuer ALLE Web-Recherchen den `research`-Skill
laden und ihm diesen Research-Auftrag uebergeben: [thema, zweck, zerlegungs_modus, unterthemen[],
version_anker, engine, anzahl/wellen/cap, rueckgabe_schema, persistenz_ziel, dup_quelle, nacharbeit_aufrufer]."

> **Falle:** Der `superintelligenz`-**Skill** (≠ Agent) ist NUR Leitbild/Checkliste, KEIN Recherche-Workflow
> → bekommt KEINEN Uebergabe-Block.

Empfehlung + Frage 1 (+ bei A Frage 2) laufen als Policy-Schicht VOR der Delegation.

---

## 6. Was NIEMALS passieren darf

- Eine Firecrawl-/Crawl-Recherche starten OHNE Frank vorher zu fragen (A/B/C-Protokoll)
- Mehr als **2** Firecrawl-Researcher gleichzeitig (Free-Limit → 429/Credit-Verschwendung)
- Auf eine ganze Welle warten statt Continuous-Spawning (§3a — haeufigste + teuerste Schwarm-Suende)
- Firecrawl-Rohdaten ungefiltert in den teuren Hauptagent-Kontext laden — immer erst MiniMax
- Bei unsicherem MiniMax-Ergebnis stillschweigend halluzinieren statt zu eskalieren
- Die Frage 2 (Eskalation nach Firecrawl) weglassen · Test-Crawls ohne Franks Freigabe (Credits knapp)

---

## Zusammenspiel & Autoritaet

Stufe 3 (Einarbeiten) = Persistenz-Pflicht (`research-persistence.md`). Schwarm-Regeln
(`agent-and-researcher-rules.md`). Modell-Mechanismus (`highest-model-everywhere.md`, §4a). Firecrawl-Limits
(`bugs/apis/firecrawl.md`). Diese Datei wird automatisch in jeder Session geladen — KEIN Agent/Skill/Hook/
Prozess darf sie entfernen oder abschwaechen.
