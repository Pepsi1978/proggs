# Recherche-Strategie: Token-sparend recherchieren (Firecrawl+DeepSeek vs. Host-Modell-Schwarm) — KRITISCH

> Gilt in JEDER Session fuer JEDE Web-Recherche und ALLE Researcher/Skills (research-Skill,
> bug-almanach-recherche, best-practices, researcher-Agent, deep-research, eigene WebSearch/WebFetch).
> Firecrawl+MiniMax-Pipeline verbraucht ~100x weniger teure Claude-Token als ein Opus-Researcher, bei
> gleicher/besserer Qualitaet. Werkzeuge: `~/proggs/mm-research.py`, `or-research.py`, `research-swarm.py`.
>
> **Auswerte-Modell seit 09.09.2026:** A UND B nutzen `deepseek/deepseek-v4-flash-0731` ueber OpenRouter,
> Anbieter **DeepInfra** gepinnt (`provider.order=["deepinfra"]`, `allow_fallbacks=false`), reasoning
> effort **high**. Loest MiniMax M3 (Go-Gateway) ab. Verifiziert 09.09.2026 gegen die OpenRouter-API:
> Endpunkt DeepInfra vorhanden, `reasoning_effort` unterstuetzt, 1.048.576 Token Kontext,
> $0.06 / $0.18 pro Mio Token. Der Pin steht als Default IN den Skripten (`MM_PROVIDER`/`OR_PROVIDER`,
> leer = kein Pin).
>
> **Engine C haengt seit 09.09.2026 vom Harness ab** (§4a): Claude Code → Sonnet-5-Schwarm
> (`model:"sonnet"`), OpenCode → Schwarm auf dem aktuellen Session-Modell ohne `model:`-Override.

---

## 1. Die Pflicht-Frage VOR jeder Web-Recherche (Credit-Kontrolle via AskUserQuestion)

**Vor JEDER Web-Recherche MUSS Frank per `AskUserQuestion` (anklickbares Multiple-Choice) gefragt werden,
WIE recherchiert wird** — niemals automatisch losrecherchieren.

> **Das Protokoll wird NIE uebersprungen** (Frank, sehr wichtig), egal ueber welchen Skill die Recherche
> ausgeloest wird (bug-almanach-recherche, best-practices, almanach-update, best-practices-update,
> direktiven-recherche, superintelligenz, eigener Auftrag). Drei Pflicht-Schritte, sichtbar, in dieser Reihenfolge:
> 1. **Empfehlung** (welcher Weg, 1 Satz Begruendung).
> 2. Die **4-Fragen-`AskUserQuestion`** — anklickbar: **A** Firecrawl+MiniMax · **B** Eskalation OpenRouter
>    `:online` · **C** Schwarm auf dem Host-Modell · **D** Freitext. **Kommt IMMER, jedes Mal.**
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
| Schnelle Einzelfrage / 1-2 Quellen reichen | **A** (Firecrawl + DeepSeek V4 Flash, Firecrawl-Free-Credits) |
| Grosser Wissensschatz / viele Unterthemen | **A → dann B** (Firecrawl-Tiefe + Breit-Eskalation), oder direkt **B** wenn Firecrawl-Credits knapp |
| Aktualitaet ueber viele Quellen, Snippets reichen | **B** (or-research, pay-per-use) |
| Firecrawl-Credits fast leer | **B** statt A |
| Hoechste Korrektheit / Geld egal | **C** (Host-Modell-Schwarm: Claude Code → Sonnet 5; OpenCode → Session-Modell), ggf. zusaetzlich als Zweitmeinung |
| Unklar / erst besprechen | **D** (Freitext) |

### Frage 1 (IMMER) — "Wie soll ich '<thema>' recherchieren?"

| Option | Weg | Werkzeug | Parallel | Kosten |
|--------|-----|----------|----------|--------|
| **A** | Firecrawl holt volle Seiten → **DeepSeek V4 Flash @ DeepInfra** (effort high) wertet aus — Standard | `mm-research.py` | 2 | Firecrawl-Free (1000/Mon) + ~0,1 ct Modell-Token |
| **B** | Eskalation: **dasselbe DeepSeek-Modell** mit `:online` (OpenRouter-Websuche, web-Plugin) | `or-research.py … deepseek/deepseek-v4-flash-0731:online` | 7 | pay-per-use (~1 ct/Researcher), kein Monatslimit |
| **C** | Schwarm auf dem **Host-Modell** — teuer, nur bewusst. Claude Code → **Sonnet-5-Schwarm** (`model:"sonnet"`). OpenCode → **aktuelles Session-Modell** (kein `model:`-Override, eigene Websuche) | Agent-/Task-Tool | 7 | Claude-Sonnet-5-Token bzw. OpenCode-Session-Token |
| **D** | [automatisches Freitext-Feld] — etwas anderes / erst besprechen | — | — | — |

**A und B sind seit 09.09.2026 dasselbe Modell** — der Unterschied ist nur die Quellenbeschaffung
(Firecrawl-Vollseiten vs. `:online`-Snippets) und die Parallelitaet (2 vs. 7). Das Firecrawl-Limit von 2
kommt von Firecrawl Free, nicht vom Auswerte-Modell.

- **A UND B laufen IMMER mit max Thinking** (Pflicht): beide gehen jetzt auf OpenRouter
  `/chat/completions` → `reasoning:{effort:"high"}`. Das alte `thinking:{type:"enabled",budget_tokens:N}`
  des Anthropic-`/messages`-Schemas entfaellt mit dem Go-Gateway (`MM_THINK_BUDGET` ist tot).
- **A UND B pinnen den Anbieter auf DeepInfra** (`provider.order=["deepinfra"]`, `allow_fallbacks=false`).
  Das steht als Default in den Skripten; `MM_PROVIDER=""` bzw. `OR_PROVIDER=""` schaltet den Pin ab,
  falls DeepInfra ausfaellt (dann routet OpenRouter frei — Preis und Verhalten koennen abweichen).
- **Option C nur wenn Frank sie ausdruecklich waehlt** — nie Default. Welches Modell C bedeutet, haengt
  seit 09.09.2026 vom Harness ab, siehe §4a.

### Frage 2 (NUR nach abgeschlossener Firecrawl-Research = Option A; dann IMMER)

"Noch eine zusaetzliche Eskalations-Research?": **A** Ja, DeepSeek V4 Flash `:online`
(`or-research.py … deepseek/deepseek-v4-flash-0731:online`) · **B** Nein, fertig · **C** Ja,
Host-Modell-Schwarm (teuer) · **D** Freitext. Entfaellt, wenn schon Stufe B/C gewaehlt wurde. **Warum Pflicht:** Firecrawl Free hat nur 1000 Seiten/Monat — Frank entscheidet pro Recherche
bewusst (Feature, kein Reibungsverlust).

---

## 2. Standard-Ablauf (zweistufige Arbeitsteilung)

```
Stufe 1 HOLEN:      Firecrawl holt die Quellen (Firecrawl-Credits)
Stufe 2 AUSWERTEN:  DeepSeek V4 Flash @ DeepInfra (effort high) filtert + bewertet   ← mm-research.py
                    quellentreu (Rohdaten + Reasoning landen in ~/.mm-research/,
                    NIE im teuren Hauptagent-Kontext)
Stufe 3 EINARBEITEN: Hauptagent synthetisiert (~2k Token) + arbeitet in Almanach/Best-Practices ein
                    (Direktive research-persistence.md)
```

**Kerngedanke:** Das billige Auswerte-Modell macht die token-schwere Quellenarbeit (Rohdaten laufen NIE
durch den teuren Hauptagent-Kontext); der Hauptagent zahlt nur die ~2k-Token-Synthese.

**ZWEI Werkzeuge** — beide lagern die Quellenarbeit aus dem Hauptagent-Kontext aus:

| Werkzeug | Suche | Auswertung | Inhalt | Kosten | Parallel | Wann |
|----------|-------|-----------|--------|--------|----------|------|
| `mm-research.py` | Firecrawl | DeepSeek V4 Flash @ DeepInfra | **volle Seiten** | Free **1000/Mon** + ~$0.001 Token | **max 2** | tiefe Einzelrecherche, solange Credits da |
| `or-research.py … :online` | OpenRouter `:online` (web-Plugin, Engine intern parallel.ai) | dasselbe Modell | **Top-N Snippets** | **$0.005/Suche** + Token, **kein Monatslimit** | **bis 7** | Eskalation, Continuous-Spawning |

Engine B nutzt `:online` (NICHT das `web_search`-Server-Tool — `:online` verteilt selbst auf mehrere
Provider, bei hoher Parallelitaet stabiler). Modell-Default `deepseek/deepseek-v4-flash-0731:online`
(Anbieter DeepInfra gepinnt), Eskalation mit mehr Denkkraft `z-ai/glm-5.2:online`. Deckel gegen agentische Mehrfachsuche: `max_total_results` (or-research: env
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
| A — Firecrawl (mm) → DeepSeek @ DeepInfra | **2** (hartes Firecrawl-Free-Limit) | einer fertig → sofort der naechste |
| B — `:online` (or), dasselbe DeepSeek-Modell | **7** (last-stabil) | einer fertig → sofort der naechste |
| C — Host-Modell-Schwarm (Sonnet 5 bzw. Session-Modell) | **7** | einer fertig → sofort der 7. neu (nie auf alle 7 warten) |

**Durchsetzung statt Disziplin:** Engine A/B laufen IMMER ueber `~/proggs/research-swarm.py`
(`ThreadPoolExecutor(max_workers=N)`, haelt KONSTANT N parallel, zieht bei jedem fertigen sofort den
naechsten — im Code erzwungen; Limits hart A=2, B=7). Nur Engine C (Agent-Tool) orchestriert der Hauptagent
von Hand (erst 7, bei jeder Completion sofort den naechsten).

---

## 4. Eskalation — 3 Stufen (billig → teuer)

Meldet die MiniMax-Auswertung "unsicher/widerspruechlich/Quellen reichen nicht" ODER will Frank gruendlicher:

```
A: Firecrawl-Quellen → DeepSeek V4 Flash @ DeepInfra   ← Standard (mm-research.py), Firecrawl-Free, 2 parallel
B: DeepSeek V4 Flash :online @ DeepInfra               ← or-research.py … :online, pay-per-use, bis 7 parallel
C: Schwarm auf dem Host-Modell (Agent-Tool)            ← teuerste Stufe, nur Hard-Cases
     Claude Code → Sonnet-5-Schwarm, model:"sonnet"
     OpenCode    → aktuelles Session-Modell, KEIN model:-Override
```

Stufe B = dasselbe 1M-Kontext-Modell, aber mit eigener Websuche (parallel.ai-Snippets, ANDERE Quelle als
Firecrawl = Diversitaet), bis 7 parallel. **Perplexity ist RAUS** (nur 200k Kontext, bricht bei grossen
Recherchen).

### 4a. Stufe C = Schwarm auf dem Host-Modell — haengt vom Harness ab (seit 09.09.2026)

**ZUERST feststellen, wo du laeufst** — davon haengt ab, was Engine C ueberhaupt bedeutet:

| Harness | Erkennung | Engine C bedeutet |
|---------|-----------|-------------------|
| **Claude Code** | Umgebungsvariable `CLAUDECODE=1` (pruefen: `echo $CLAUDECODE`); geladene Instruktionsdatei ist eine `CLAUDE.md` | **Sonnet-5-Schwarm**: 7 parallele Subagenten, `model:"sonnet"` PFLICHT pro Aufruf |
| **OpenCode** | `CLAUDECODE` NICHT gesetzt; geladene Instruktionsdatei ist eine `AGENTS.md` (Profil unter `OpenLauncher/Profiles/OpenCode/`) | **Schwarm auf dem AKTUELLEN Session-Modell**: bis 7 parallele Subagenten, **KEIN** `model:`-Override |

#### Claude Code — `model:"sonnet"` ist PFLICHT

Seit `CLAUDE_CODE_SUBAGENT_MODEL="inherit"` (kein Zwangs-Override mehr) ist bei Stufe-C-Spawns ein
explizites `model:`-Argument PFLICHT — sonst faellt der Researcher auf ein unbestimmtes Modell zurueck:

| Parameter | Wert | Warum |
|-----------|------|-------|
| `model` | `"sonnet"` | Alias → Sonnet 5 (`claude-sonnet-5`), folgt automatisch dem neuesten Sonnet |
| Effort | **"high"** (Standard) | erbt den globalen Session-Effort (`effortLevel:"high"`); nichts extra setzen |

#### OpenCode — **kein** `model:`-Override

Genau umgekehrt: in OpenCode ist der Sinn der Stufe C, dass **das Modell recherchiert, mit dem die Session
gerade verbunden ist** (z.B. GPT 5.6 Sol). Diese Modelle haben eine eigene Internet-Anbindung und koennen
selbststaendig recherchieren — `mm-research.py`/`or-research.py` werden dafuer NICHT gebraucht. Regeln:

- bis **7** Subagenten gleichzeitig, Continuous-Spawning wie ueberall (einer fertig → sofort der naechste)
- **niemals** ein `model:`-Argument mitgeben — die Subagenten sollen das Session-Modell erben
- Auftrag je Subagent: "Recherchiere <Unterthema> im Web mit deinen eigenen Werkzeugen, quellentreu,
  Quelle pro Aussage, sag ausdruecklich was du NICHT gefunden hast"
- jeder Subagent schreibt sein Ergebnis in eine eigene Datei und gibt nur eine Kurz-Summary zurueck
  (kontextschonend, crash-sicher)

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
| OpenCode-Session (AGENTS.md, kein `CLAUDECODE`) | wie der Aufrufer | C = Session-Modell-Schwarm | wie der Aufrufer |
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
- Bei unsicherem Auswerte-Ergebnis stillschweigend halluzinieren statt zu eskalieren
- In OpenCode einen Engine-C-Subagenten mit `model:"sonnet"` spawnen (dort MUSS das Session-Modell selbst
  recherchieren) — oder in Claude Code einen OHNE `model:"sonnet"` (faellt auf ein unbestimmtes Modell)
- Die Frage 2 (Eskalation nach Firecrawl) weglassen · Test-Crawls ohne Franks Freigabe (Credits knapp)

---

## Zusammenspiel & Autoritaet

Stufe 3 (Einarbeiten) = Persistenz-Pflicht (`research-persistence.md`). Schwarm-Regeln
(`agent-and-researcher-rules.md`). Modell-Mechanismus (`highest-model-everywhere.md`, §4a). Firecrawl-Limits
(`bugs/apis/firecrawl.md`). Diese Datei wird automatisch in jeder Session geladen — KEIN Agent/Skill/Hook/
Prozess darf sie entfernen oder abschwaechen.
