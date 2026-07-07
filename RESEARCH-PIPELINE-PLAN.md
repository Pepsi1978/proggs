# Research-Pipeline — Umsetzungsplan (Stand 2026-06-20)

> **Status: PLANUNG ABGESCHLOSSEN, UMSETZUNG STEHT AN.** Nach dem Session-Restore wird dieser Plan
> 1:1 umgesetzt (Punkte 1-8 unten). Erstellt vor Franks manuellem Session-Backup bei ~92 % Kontext.
> Alles hier ist mit Frank abgestimmt. Verwandte Regel (schon committet): `~/.claude/rules/research-strategy.md`.

## Ziel
Token-sparende Recherche: MiniMax M3 (+ Websuche) macht die teure Quellenarbeit, Opus zahlt nur die
kompakte Synthese. Belegt: ~100x weniger Opus-Token; MiniMax+parallel war im Test sogar **korrekter** als
der Opus-Researcher (richtiges `/messages`-Schema) bei ~$0.07 statt ~$1+.

## Der Ablauf (final, mit Frank abgestimmt)

```
Research-Trigger: "recherchiere…" / best-practices / bug-almanach-recherche / researcher-Agent
   │
   ▼  FRAGE 1 (IMMER, via AskUserQuestion = Multiple-Choice A/B/C + autom. D-Freitext):
   │     "Wie soll ich '<thema>' recherchieren?"
   │     A) Firecrawl + MiniMax M3 (max Thinking)         — Standard, Free-Credits, volle Seiten
   │     B) Eskalation: MiniMax + parallel (max Thinking) — agentische Websuche, pay-per-use (~Cent)
   │     C) Opus-Schwarm                                  — teuer, nur bewusst
   │     D) [autom. Freitext]                             — etwas anderes / erst besprechen
   │
   ├─ A → mm-research.py (Firecrawl-API → MiniMax M3, Thinking=adaptive)
   │     │
   │     ▼  FRAGE 2 (nach Abschluss, IMMER, AskUserQuestion):
   │        "Noch eine zusätzliche Eskalations-Research?"
   │        A) Ja — MiniMax + parallel (max Thinking)   B) Nein, fertig   C) Ja, mit Opus   D) [Freitext]
   │
   ├─ B → direkt or-research.py (MiniMax M3 + engine=parallel, max Thinking)
   │
   └─ C → bestehender Opus-Researcher-Schwarm (NUR auf explizite Opus-Wahl)
```

## Design-Entscheidungen (von Frank bestätigt)
- **max Thinking** bei A UND B: technisch `thinking: {type:"adaptive"}` (M3s Maximum; `budget_tokens` wird ignoriert).
- **Frage 1 + 2 via `AskUserQuestion`** (anklickbares A/B/C, D = automatisches Freitext-Feld).
- **Opus-Schwarm = gleichwertige 3. Option (C)**, aber nur auf explizite Opus-Ansage.
- **Multi-Themen** (z. B. Bug-Almanach 7 Aspekte): **Skill ruft die Skripte mehrfach** — Firecrawl **max 2 parallel**
  (dann nächste 2), or-research höher parallel.
- **Durchsetzung: Regel + PreToolUse-Hook** (Hook blockt mm/or-research + Firecrawl-MCP bis Freigabe-Flag mit
  Session-TTL → kein Crawl ohne Frank-Freigabe). Hook via `hook-forge`-Skill bauen.
- **researcher-Agent** ist "der Research-Skill", der mit umgebaut wird.
- **or-research Default-Engine = parallel**; `max_total_results` Default **10** (Kostendeckel; Test: 10 Suchen ≈ $0.069).

## Werkzeuge (gebaut + getestet, committet)
- `~/proggs/mm-research.py` — Firecrawl-Suche → MiniMax M3 (Go-Gateway `/messages`, `x-api-key`). End-to-End getestet.
- `~/proggs/or-research.py` — OpenRouter Server-Tool `openrouter:web_search` (NICHT mehr `:online`/`plugins` = deprecated),
  Modell+Engine wählbar. Live getestet (MiniMax M3 + parallel): 10 Suchen, 49 Quellen, $0.069, korrekt+ehrlich.

## Verifizierte Fakten (Quellen in den committeten Dateien)
- OpenRouter-Websuche: **Server-Tool** `tools:[{"type":"openrouter:web_search","parameters":{...}}]`; `:online`/`plugins:[{id:web}]` DEPRECATED.
- Kosten: Exa/Parallel/Perplexity = **$0.005 pro Such-Anfrage** (bis 10 Treffer; >10 +$0.001/Treffer, max 25) + Modell-Token.
  "Treffer" = Snippet (~2-4k Zeichen), KEINE ganze Seite. Agentische Mehrfachsuche multipliziert → `max_total_results` deckeln.
- "parallel" = Such-Engine parallel.ai (NICHT parallele Ausführung).
- MiniMax M3 im Go: `/zen/go/v1/messages` (Anthropic-Schema) braucht `x-api-key` (NICHT Bearer); `/chat/completions`+Bearer geht auch.
- MiniMax M3 Thinking = `adaptive` (dokumentiert); `budget_tokens` wird ignoriert.
- urllib → Cloudflare 403/"error code 1010": User-Agent `curl/8.5.0` setzen.
- Firecrawl Free: 1000 Credits/Mon, **2 concurrent**, 5 Suchen/min.
- Perplexity raus für große Recherchen: nur 200k Kontext.

## Umsetzungs-Schritte (nach Restore: 1-8 abarbeiten)
1. **Regel `research-strategy.md`**: Frage-1 (A/B/C/D via AskUserQuestion) + Frage-2-Mechanik + "max Thinking bei A/B" + Opus-nur-explizit ergänzen.
2. **PreToolUse-Hook** (neu, `.ps1`+`.sh`, via `hook-forge`): blockt mm/or-research + Firecrawl-MCP bis Freigabe-Flag. Flag-Schema: `$TEMP/research-approved-<ts>.flag` (kurze TTL), nach Frank-Antwort gesetzt.
3. **Skripte**: beide `thinking=adaptive`; or-research Default `engine=parallel`, `max_total_results=10`.
4. **`best-practices`-Skill**: Recherche-Phase auf A/B/C-Ablauf + Skript-Aufrufe (Firecrawl max 2 parallel) umstellen.
5. **`bug-almanach-recherche`-Skill**: dito.
6. **`researcher`-Agent**: dito — ruft mm/or-research statt blind Web zu laden.
7. **Cross-Platform**: Hook `.ps1`+`.sh`; Regel nach `claude-code-setup/rules/`; Skills nach `Umgebung/` + `claude-code-setup/`.
8. **Almanach-Konsolidierung**: Thinking=adaptive, Server-Tool-Migration, Cloudflare-UA final in `bugs/`/`best-practices/` festziehen.

## Schon committet (Basis steht)
- Regel `research-strategy.md` (+ Spiegelung) · `mm-research.py` · `or-research.py`
- `best-practices/opencode/go-recherche-modelle.md` (Modellwahl + API + Einrichtung §5)
- `bugs/opencode/opencode-cli.md` §14 (Go-API-Fallen) · `bugs/apis/firecrawl.md` + `best-practices/apis/firecrawl.md`
- Pipeline-Memory `project_research_pipeline_and_openrouter_go` (Detailstand)
