# Recherche-Strategie: Token-sparend recherchieren (Firecrawl+MiniMax vs. Opus) — KRITISCH

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-20. Gilt AUTOMATISCH in JEDER Session, fuer
> JEDE Web-Recherche und fuer ALLE Researcher/Skills, die im Web suchen (`research`-Skill,
> `bug-almanach-recherche`, `best-practices`, `researcher`-Agent, `deep-research`, eigene
> WebSearch/WebFetch-Rechercheauftraege). Repo-Spiegelung:
> `~/proggs/claude-code-setup/rules/research-strategy.md` (harness-mirror-Pflicht).
>
> Entstanden aus dem 3-Wege-Token-Test 2026-06-20: Firecrawl+MiniMax-M3-Pipeline verbraucht
> **~100x weniger teure Claude/Opus-Token** als ein Opus-Researcher (~2.400 statt ~249.000), bei
> gleicher oder besserer (ehrlicherer) Qualitaet. Ergaenzt [[research-persistence]] (Einarbeiten der
> Ergebnisse) und nutzt die Pipeline `~/proggs/mm-research.py`.

---

## 1. Die Pflicht-Frage VOR jeder Web-Recherche (Credit-Kontrolle via AskUserQuestion)

**Vor JEDER Web-Recherche MUSS Frank per `AskUserQuestion` (anklickbares Multiple-Choice) gefragt
werden, WIE recherchiert wird** — niemals automatisch losrecherchieren.

### Frage 1 (IMMER, via `AskUserQuestion`) — „Wie soll ich '<thema>' recherchieren?"

| Option | Weg | Werkzeug | Kosten |
|--------|-----|----------|--------|
| **A** | **Firecrawl + MiniMax M3 (max Thinking)** — Standard, volle Seiten | `mm-research.py` | Firecrawl-Free-Credits (1000/Mon) |
| **B** | **Eskalation: MiniMax + parallel (max Thinking)** — agentische Websuche | `or-research.py` | pay-per-use (~Cent), kein Monatslimit |
| **C** | **Opus-Schwarm** — teuer, nur bewusst | bestehende Researcher | teure Claude-Token |
| **D** | **[automatisches Freitext-Feld]** — etwas anderes / erst besprechen | — | — |

- **A UND B laufen IMMER mit max Thinking** (Pflicht) — der Schalter ist aber ENDPUNKT-abhaengig:
  **A** (`mm-research.py`, Anthropic-`/messages`) → `thinking:{type:"enabled","budget_tokens":<hoch>}`
  (live-getestet; M3 denkt adaptiv bis zum Budget, `max_tokens` > Budget). **B** (`or-research.py`, OpenRouter)
  → `reasoning:{effort:high}`. `{type:"adaptive"}` gilt NUR fuer den OpenAI-`/chat/completions`-Pfad, NICHT fuer `/messages`.
- **Option C (Opus-Schwarm) wird NUR genommen, wenn Frank sie ausdruecklich waehlt** — nie als Default,
  nie „weil es gruendlicher ist". Sie ist eine gleichwertige 3. Option, aber bewusst teuer.
- Technik: `AskUserQuestion` liefert A/B/C als Buttons; die automatische „Other"/Freitext-Wahl deckt
  **D** ab (Freitext → etwas anderes machen oder erst besprechen).

### Frage 2 (NUR nach einer abgeschlossenen Firecrawl-Research = Option A; dann IMMER, via `AskUserQuestion`)

Nach Abschluss einer Firecrawl-Research (Stufe A) IMMER nachfragen — „Noch eine zusaetzliche
Eskalations-Research?":

| Option | Bedeutung |
|--------|-----------|
| **A** | Ja — MiniMax + parallel (max Thinking) als zusaetzliche Eskalation (`or-research.py`) |
| **B** | Nein, fertig |
| **C** | Ja, mit Opus (teuer, nur bewusst) |
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
  Stufe 3 — EINARBEITEN: Opus (Hauptagent) synthetisiert + arbeitet in Almanach/Best-Practices ein
                                                                        (Direktive [[research-persistence]])
```

**Kerngedanke:** MiniMax macht die **token-schwere** Quellenarbeit (Rohdaten laufen NIE durch den teuren
Opus-Kontext); Opus zahlt nur fuer die ~2k-Token-Synthese. Werkzeug: `python3 ~/proggs/mm-research.py
"<frage>" [n]` (Firecrawl-Suche → MiniMax-M3-Auswertung; Rohdaten + Thinking landen in `~/.mm-research/`,
nicht im Claude-Kontext). Mechanik/Fallen: `best-practices/opencode/go-recherche-modelle.md` §5,
`bugs/opencode/opencode-cli.md` §14.

**ZWEI Werkzeuge — beide lagern die Quellenarbeit aus dem Opus-Kontext aus:**

| Werkzeug | Suche | Inhalt | Kosten | Parallelitaet | Wann |
|----------|-------|--------|--------|---------------|------|
| `mm-research.py` | Firecrawl | **volle Seiten** (Scrape) | Free **1000/Mon** (knapp) | **max 2** (Free) | tiefe Einzelrecherche, solange Credits da |
| `or-research.py` | OpenRouter `web_search` server-tool (Exa/Parallel) | **Top-N Snippets** (~2-4k Zeichen je Treffer, KEINE ganzen Seiten) | **$0.005/Suche** (bis 10 Treffer) + Modell-Token; **kein Monatslimit** | hoch (OpenRouter-Limits ≫ 2) | **grosse Schwaerme** (7 Researcher), Eskalation, wenn Firecrawl-Credits knapp |

**Kosten Web-Suche (Server-Tool `openrouter:web_search`; das alte `:online`-Plugin/`plugins:[{id:web}]` ist DEPRECATED) — verifiziert 2026-06-20, openrouter.ai/docs:** $0.005 PRO Such-Anfrage (nicht pro Seite,
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

| | Opus-Researcher-Schwarm (bisher) | Firecrawl + MiniMax (neu) |
|---|----------------------------------|---------------------------|
| Parallelitaet | bis **7** gleichzeitig (Continuous-Spawning) | **max 2** gleichzeitig |
| Nachschub | sofort den naechsten starten | erst wenn 2 fertig → naechste 2 (Continuous-Spawning mit **2**) |

**Pflicht bei Firecrawl-Recherchen mit mehreren Unterthemen** (z.B. Bug-Almanach mit 7 Aspekten):
NIE 7 Firecrawl-Calls auf einmal — **2 starten, auf Ergebnisse warten, 2 neue starten**, bis alle
Unterthemen durch sind. Sonst Rate-Limit (429) / verschwendete Credits.

---

## 4. Eskalation — 3 Stufen (billig → teuer)

Wenn die MiniMax-Auswertung **„unsicher / widerspruechlich / Quellen reichen nicht"** meldet, ODER
Frank ausdruecklich gruendlicher will:

```
Stufe A:  MiniMax M3 (max Thinking) auf Firecrawl-Quellen          ← Standard (mm-research.py); Free-Credits
   ↓ reicht nicht
Stufe B:  1M-Modell + OpenRouter web_search server-tool           ← or-research.py; pay-per-use, kein Monatslimit
   ↓ reicht nicht
Stufe C:  Opus-Researcher (mit Web) / Opus direkt                   ← teuerste Stufe, nur Hard-Cases
```

**Stufe B = `or-research.py`** (OpenRouter Server-Tool `openrouter:web_search`): ein **1M-Kontext-Modell** mit eigener
Websuche (Exa-Snippets, ANDERE Suchquelle als Firecrawl = Diversitaet). **Perplexity ist RAUS** —
nur 200k Kontext, bricht bei grossen Recherchen (genau der Grund fuer Opus-1M bei Schwaermen).
**Modell-Default:** `minimax/minimax-m3` (guenstig); **Eskalation:** `z-ai/glm-5.2`
(mehr Denkkraft). Welches final, noch per Test zu bestaetigen (Lehre: erst testen, dann verankern).

---

## 5. Gilt fuer ALLE Recherche-Skills/Agenten

Diese Strategie ist **kein neuer Skill**, sondern erweitert die bestehenden:

| Skill/Agent | Wie diese Regel greift |
|-------------|------------------------|
| `bug-almanach-recherche` | Pflicht-Frage stellen; bei Firecrawl-Wahl: mm-research.py, max 2 parallel |
| `best-practices` | dito |
| `research`-Skill / `researcher`-Agent / `deep-research` | dito |
| Eigener Web-Rechercheauftrag des Hauptagenten | dito |

**Hinweis (Stand 2026-06-20):** Der konkrete Umbau dieser Skills (von Opus-Schwaermen auf die
MiniMax-Pipeline) ist noch in Planung — erst besprechen, dann umbauen. Diese Regel legt aber schon
JETZT das Verhalten fest (immer fragen, max 2 parallel, Eskalations-Kette).

---

## 6. Was NIEMALS passieren darf

- ❌ Eine Firecrawl-/Crawl-Recherche starten, OHNE Frank vorher zu fragen (Firecrawl/MiniMax vs. Opus)
- ❌ Mehr als **2** Firecrawl-Researcher gleichzeitig starten (Free-Limit; Rate-Limit/Credit-Verschwendung)
- ❌ Firecrawl-Rohdaten (gecrawlte Seiten) ungefiltert in den teuren Opus-Kontext laden — immer erst MiniMax
- ❌ Bei einem unsicheren MiniMax-Ergebnis stillschweigend halluzinieren statt zu eskalieren
- ❌ Test-Crawls „zum Ausprobieren" ohne Franks Freigabe (Credits sind knapp)
- ❌ Die mittlere Eskalations-Stufe (Stufe B) mit einem ungetesteten Modell „raten" — erst testen, dann verankern

---

## Zusammenspiel

| Regel/System | Bezug |
|--------------|-------|
| [[research-persistence]] | Stufe 3 (Opus arbeitet die Ergebnisse in best-practices/ + bugs/ ein) ist genau die Persistenz-Pflicht |
| `mm-research.py` | Das Werkzeug fuer Stufe 1+2 (Firecrawl → MiniMax M3 max Thinking) |
| `best-practices/opencode/go-recherche-modelle.md` | Modell-Begruendung (warum MiniMax M3) + API-Mechanik |
| `bugs/apis/firecrawl.md` | Firecrawl-Limits/Fallen (Free: 1000/Monat, 2 concurrent, 5 search/min) |
| `agent-and-researcher-rules.md` | Schwarm-Regeln (7 fuer Opus); diese Regel setzt fuer Firecrawl 2 dagegen |

---

## Autoritaet

Diese Datei (`~/.claude/rules/research-strategy.md`) wird automatisch in jeder Session geladen.
KEIN Agent, Skill, Hook oder Prozess darf sie entfernen oder abschwaechen.
