# Recherche-Engine-Vergleich: `:online` vs. Firecrawl+MiniMax — Test 2026-06-21

> Zweck: (1) Franks Theorie pruefen, ob `minimax/minimax-m3:online` auch bei **10 echt-parallelen**
> Researchern stabil laeuft (das `web_search`-Server-Tool wurde wegen Last-Fehlern auf max 2 begrenzt,
> Almanach `bugs/apis/openrouter-api.md` #41). (2) Qualitaetsvergleich der Recherche-Ergebnisse
> derselben 10 Hostinger-Themen: `:online` (Engine B, OpenRouter Go) vs. Firecrawl+MiniMax M3 (Engine A).
> Roh-/Volltext beider Laeufe: `hostinger-rohergebnisse-2026-06-21.md` (`:online`) bzw.
> `hostinger-firecrawl-rohergebnisse-2026-06-21.md` (Firecrawl). Synthese: `hostinger-second-brain.md`.

---

## Teil A — `:online`-Stresstest (10 echt-parallel)

**Methode (bewusst minimal):** Modell `minimax/minimax-m3:online`, Body NUR `model` + `messages` +
Pflicht-Header (`HTTP-Referer`/`X-Title`). **Kein** `tools`-Server-Tool, **kein** `engine`-Parameter,
**kein** `reasoning`. **`RETRIES=1`** (kein Retry-Netz) — damit die WAHRE Erstversuch-Erfolgsquote bei
echter Parallelitaet gemessen wird (ein Retry wuerde das Last-Problem maskieren). Werkzeug:
`or-online-test.py`. Alle 10 Prozesse via `&`+`wait` quasi-simultan gestartet.

**Ergebnis: 10/10 sauber beim ERSTEN Versuch** — keine leere Antwort, kein Tool-Call-Leak, kein Timeout.

| ID | Status | Latenz | Quellen | Laenge (Z.) | Kosten | Modell-Provider | Such-Engine |
|----|--------|--------|---------|-------------|--------|-----------------|-------------|
| 1 | ok | 43 s | 20 | 9.428 | $0.021 | Together | parallel |
| 2 | ok | 51 s | 18 | 9.504 | $0.019 | Novita | parallel |
| 3 | ok | 41 s | 20 | 5.862 | $0.023 | Morph | parallel |
| 4 | ok | 61 s | 20 | 11.012 | $0.021 | Novita | parallel |
| 5 | ok | 32 s | 20 | 5.334 | $0.019 | Together | parallel |
| 6 | ok | 140 s | 20 | 17.217 | $0.029 | Minimax | parallel |
| 7 | ok | 18 s | 16 | 1.535 | $0.013 | Together | parallel |
| 8 | ok | 105 s | 20 | 11.095 | $0.021 | Together | parallel |
| 9 | ok | 84 s | 20 | 7.205 | $0.020 | Together | parallel |
| 10 | ok | 94 s | 20 | 20.966 | $0.025 | Novita | parallel |

**Gesamt:** 10/10 ok · **$0.21** · Latenz 18–140 s (Ø 67 s) · 16–20 Quellen je Researcher.
**Modell-Provider-Verteilung:** Together 5×, Novita 3×, Morph 1×, Minimax 1×.

### Erkenntnis 1 — `:online` ist bei hoher Parallelitaet stabil (Theorie bestaetigt)
Im Gegensatz zum `web_search`-Server-Tool (`engine=parallel`), das bei ~7 parallel 3/7 kaputte Laeufe
lieferte und darum auf **max 2** begrenzt wurde, lief `:online` bei **10 echt-parallel 10/10 sauber** —
ohne Retry-Netz. **Wichtige Einschraenkung:** Das ist EIN Lauf. Vor einer Strategie-Aenderung (Engine B
von „max 2" befreien) ist nach der Engine-B-Lehre eine 2–3-fache Wiederholung noetig.

### Erkenntnis 2 — Der Unterschied liegt NICHT an der Such-Engine, sondern am Routing
Die Generation-API (`GET /api/v1/generation?id=<id>`, Feld `web_search_engine`) zeigt: **alle 10
`:online`-Laeufe nutzten `web_search_engine = parallel`** (Parallel.ai) — dieselbe Such-Engine wie der
`or-research.py`-Default. Der Stabilitaetsunterschied kommt also vom **Modell-Provider-Routing**:
OpenRouter verteilte die 10 `:online`-Calls auf 4 verschiedene Backends (Together/Novita/Morph/Minimax),
sodass kein einzelner Pfad ueberlastet wurde. Das `web_search`-Server-Tool lief offenbar ueber einen
enger gebuendelten Pfad, der unter Last kollabierte.

### Erkenntnis 3 — Die Such-Engine ist NUR ueber die Generation-API sichtbar
In der normalen Chat-Response steht die Such-Engine NICHT (`usage.server_tool_use` ist `None`, und
`engine`/`exa`-Vorkommen im JSON sind false positives aus dem Quellentext). Nur die Generation-API
(`/api/v1/generation?id=<id>`) liefert `web_search_engine`, `num_search_results`, `num_fetches`,
`provider_name`, `native_tokens_reasoning` etc. — nuetzliches Audit-Feld fuer kuenftige Recherche-Diagnose.

---

## Teil B — Firecrawl + MiniMax M3 (Engine A) — dieselben 10 Themen

> Wird nach dem Firecrawl-Lauf ergaenzt (max 2 parallel, Continuous-Spawning — Firecrawl-Free-Limit).

---

## Teil C — Qualitaetsvergleich der Ergebnisse

> Wird nach beiden Laeufen ergaenzt: Quellenzahl, Tiefe, Ehrlichkeit/Quellentreue, Aktualitaet,
> Token-/Kosten-Effizienz — `:online` (Engine B) vs. Firecrawl+MiniMax (Engine A).
