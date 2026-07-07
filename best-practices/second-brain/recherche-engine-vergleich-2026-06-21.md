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

**Methode:** `mm-research.py` — Firecrawl `/v1/search` holt je **5 VOLLE Seiten** (gescraptes Markdown),
MiniMax M3 (max Thinking, `budget_tokens=24000`, OpenRouter Go) wertet quellentreu aus. **Max 2 parallel**
(Firecrawl-Free erlaubt nur 2 concurrent), Continuous-Spawning via `firecrawl-test-run.py`. Jeder Lauf
eigenes `MM_OUTDIR` (kein Ueberschreiben mehr).

**Ergebnis: 10/10 sauber.**

| ID | Quellen (volle Seiten) | MM-Token in | MM-Token out | Thinking (Z.) | Antwort (Z.) |
|----|------------------------|-------------|--------------|---------------|--------------|
| 1 | 5 | 16.802 | 3.378 | 4.179 | 6.239 |
| 2 | 5 | 17.595 | 5.450 | 6.672 | 8.965 |
| 3 | 4 | 12.367 | 1.965 | 3.283 | 4.015 |
| 4 | 4 | 11.366 | 3.105 | 3.189 | 8.573 |
| 5 | 4 | 11.479 | 1.909 | 2.337 | 4.730 |
| 6 | 4 | 10.237 | 4.185 | 3.311 | 11.382 |
| 7 | 4 | 11.092 | 3.299 | 5.233 | 6.918 |
| 8 | 5 | 12.850 | 5.714 | 5.100 | 15.323 |
| 9 | 4 | 12.627 | 1.611 | 3.267 | 3.404 |
| 10 | 4 | 12.756 | 3.246 | 3.229 | 8.449 |

**Gesamt:** 10/10 ok · 4–5 Quellen je Researcher · MiniMax-Token ueber das **Go-Abo** (flat, nicht
pro Call abgerechnet) · ~**50 Firecrawl-Credits** verbraucht (von 1.000/Monat gratis).

### Kosten-/Limit-Gegenueberstellung
| | Engine B (`:online`) | Engine A (Firecrawl+MiniMax) |
|---|---|---|
| Quellen je Researcher | 16–20 Snippets | 4–5 volle Seiten |
| Parallelitaet | 10+ (echt-parallel ok) | **max 2** (Firecrawl-Free) |
| Direkte Kosten | **$0.21** (pay-per-use, sichtbar) | Go-Abo-Token (flat) + ~50/1000 Firecrawl-Credits/Mon |
| Monatslimit-Risiko | keins (pay-per-use) | **ja** (1.000 Firecrawl-Credits/Mon) |
| Latenz Ø | 67 s | aehnlich (max 2 parallel → laenger gesamt) |

---

## Teil C — Qualitaetsvergleich der Ergebnisse (adversarisch geprueft)

> Ein separater Agent las BEIDE Roh-Dateien vollstaendig und verglich alle 10 Themenpaare am Material.

### Korrektur zum `:online`-Ergebnis: 9/10 echte Antworten, NICHT 10/10
Der inhaltliche Vergleich deckte einen Fehler im ersten Test auf: **Researcher 7 (`:online`, Memory-Stacks)
lieferte KEINE echte Antwort** — nur eine Einleitung + rohe JSON-Tool-Calls (`{"name": "web_search",
"input": {"query": "..."}}`). Das Modell wollte agentisch weitersuchen; im `:online`-Modus werden
Folge-Tool-Calls aber nicht ausgefuehrt → sie leakten als Text. Der erste Leak-Detektor
(`or-online-test.py`) kannte nur XML-Marker (`<tool_call>` etc.) und wertete R7 faelschlich als „ok".
**Korrigierte Bilanz: 9/10 saubere Antworten + 1 Tool-Call-Leak.** Fix: Detektor um den JSON-Tool-Call-
Shape erweitert (`or-online-test.py` + `or-research.py`, #47046); neuer Almanach-Eintrag
`bugs/apis/openrouter-api.md` §42. **Wichtig fuer die Theorie:** `:online` bleibt stabil gegen die
LAST-Crashes, die das `web_search`-Server-Tool bei 7 parallel zerlegten (3/7 kaputt) — der R7-Leak ist
ein ANDERER Fehlertyp (agentischer Folge-Tool-Call), kein Last-Crash.

### Pro-Thema-Wertung (Gewinner)
| # | Thema | Gewinner | Begruendung |
|---|-------|----------|-------------|
| 1 | Produktpalette | **B** | B deckt alle 4 Produkttypen + Agent-Eignung ab; A hatte zu Shared/Dedicated keine Quellen |
| 2 | KVM-Specs | **A** | A genauer/aktueller (CPU-Modell, Benchmark-Note) + erkennt VERALTETE NVMe-Werte, die B uebernahm |
| 3 | 1-Klick-Templates | Gleichstand | A belegt Coolify (B verneint es falsch); B belegt Open WebUI — je ein blinder Fleck |
| 4 | OS-Auswahl | **B** | B mit konkretem Hermes-Leitfaden (Ubuntu 24.04 clean, AI-Template meiden); A generischer |
| 5 | Agent-Frameworks | **B** | B mit Preisen/Auth/mehr Quellen; A widerspricht sich bei der Kernfrage |
| 6 | Vektor-DBs | **B** | B mit harten Zahlen (qps/Recall/Kosten pro 1M) + 20 Quellen; A qualitativ, 4 Quellen |
| 7 | Memory-Stacks | **A** | A wertet 4 volle Seiten + markiert befangene supermemory-Quelle; **B-R7 fiel aus (Leak)** |
| 8 | MCP/API absichern | **A** | A mit kopierbaren Nginx/Caddy-Configs aus echten Doku-Seiten; B breiter, aber flacher |
| 9 | Mehrere Dienste | **B** | B trifft den KI-Kern (Compose-Beispiele, deploy-Limit-Falle); A verfehlt das Thema |
| 10 | Best Practices | **B** | B mit weit mehr konkreten Befunden (Zahlen, RLS-Fallen); A duenn + teils off-topic |

**Endstand: `:online` (B) 6 · Firecrawl (A) 3 · Gleichstand 1.**

### Gesamtbild
- **`:online` (B) gewinnt bei Breite & Long-tail:** konstant 16–20 Quellen, harte Zahlen genau dort, wo
  die Firecrawl-5-Seiten-Auswahl das Thema verfehlt (Vektor-DB-Kosten, Best-Practices, Multi-Service).
- **Firecrawl (A) gewinnt bei Praezision auf 1–2 bekannten Seiten:** exakte Specs, Benchmark-Noten,
  kopierbare Config-Bloecke; erkennt aktiv veraltete Werte. Schwaeche: die schmale 4–5-Seiten-Basis
  verfehlt bei breiten Fragen 3× das Thema komplett (R1/R9/R10).
- **Beide aehnlich EHRLICH bei Luecken** — keine systematische Halluzination auf beiden Seiten; beide
  markieren befangene Marketing-Quellen.
- **Aktualitaet:** beide 2026er-Daten; A bei harten Preis-/Spec-Werten minimal frischer/sauberer.

### Kernfrage: Wie gut sind die `:online`-Eskalationsdaten?
**Belastbar genug als Grundlage fuer den Second-Brain-Bauplan — fuer GENAU diesen Zweck sogar leicht
besser als Firecrawl**, weil die bauplan-relevanten Themen (Vektor-DB-Wahl, Multi-Service, Best-Practices)
bei `:online` konkreter und breiter abgestuetzt sind. Zwei Einschraenkungen: (1) **Thema 7 (Memory-Stacks)
aus dem Firecrawl-Lauf nehmen** (B-R7 ist leer). (2) **Harte KVM-Specs vor Kauf live verifizieren**
(Firecrawl erkannte veraltete Werte, die `:online` uebernahm). Keine Halluzination, keine Schoenfaerberei.

### Empfehlung (welche Engine wofuer)
- **`:online` (Engine B)** = Standard fuer **breite, mehrdimensionale** Landschafts-/Vergleichs-/
  Best-Practices-Recherchen (viele Quellen, viele Datenpunkte, hohe Parallelitaet, sichtbare Kosten).
- **Firecrawl (Engine A)** = gezielt fuer **enge, faktenkritische** Fragen, deren Antwort auf 1–2 bekannten
  Seiten in voller Tiefe steht (offizielle Specs/Preise, kopierbare Configs) — oder als Verifikations-Zweitlauf.
- **Optimal kombiniert:** `:online` fuer die Breite, Firecrawl gezielt fuer die 2–3 spec-kritischen Themen.

---

## Teil D — DREI-WEGE-Vergleich (mit Opus-Eskalation, Engine C)

Dieselben 10 Themen zusaetzlich von 10 **Opus-4.8-Researchern** (WebSearch+WebFetch, 7 konstant parallel,
Continuous-Spawning) — die teuerste Eskalationsstufe. Ein separater Agent las ALLE 30 Antworten
(3 Engines × 10 Themen) und bewertete jedes Thema dreifach.

### Token / Kosten / Output je Engine
| Engine | Input-Tok | Output-Tok | Direkte Kosten | Output gesamt | Quellen/Researcher |
|--------|-----------|------------|----------------|---------------|--------------------|
| B `:online` (Parallel.ai) | 49.696 | 43.102 | **$0.22** (pay-per-use) | 106k Zeichen | 16–20 Snippets |
| A Firecrawl + MiniMax M3 | 129.171 | 33.862 | ~gratis (Go-Abo + ~50/1000 Credits) | 78k Zeichen | 4–5 volle Seiten |
| C Opus 4.8 | — (nicht exakt messbar) | — | **~70–100× B** (Claude-Opus-Token) | 146k Zeichen | 8–26 (WebSearch+Fetch) |

### Pro-Thema-Wertung (3 Engines)
| # | Thema | 1. Platz | 2. | 3. |
|---|-------|----------|-----|-----|
| 1 | Produktpalette | **Firecrawl** | Opus | :online |
| 2 | KVM-Specs/Benchmarks | **Opus** | Firecrawl | :online |
| 3 | 1-Klick-Templates | **Opus** | :online | Firecrawl |
| 4 | OS-Auswahl | **Opus** | :online | Firecrawl |
| 5 | Agent-Frameworks | **Opus** | :online | Firecrawl |
| 6 | Vektor-DBs | **Opus** | :online | Firecrawl |
| 7 | Memory-Stacks | **Opus** | :online | Firecrawl |
| 8 | MCP/API-Security | **Opus** | Firecrawl | :online |
| 9 | Multi-Service | **Opus** | :online | Firecrawl |
| 10 | Best-Practices/Fallen | **Opus** | :online | Firecrawl |

**Endstand: Opus 8× Platz 1 · Firecrawl 1× · `:online` 1× (7× Platz 2) · Firecrawl 7× Platz 3.**

### Wo Opus die zwei billigen Engines WIRKLICH schlaegt (rechtfertigt ~70–100× Preis)
- **Projekt-Synthese statt Themen-Referat:** verknuepft jedes Thema mit dem Second-Brain-Vorhaben
  (Plan-pro-Use-Case, konkrete Architektur-Vorschlaege). B/Firecrawl referieren nur Quellen.
- **Tiefere Eigenrecherche + einzigartige harte Zahlen:** Self-Host-QPS/Latenz pro Vektor-DB,
  Letta-Port/RAM-Detail, MCP-OAuth-2.1-Spec, 47.000-USD-Loop-Mechanik, 93,4 %-Agent-Server-Scan.
- **Strukturierte Wiederverwertbarkeit:** liefert pro Researcher fertige `BEST-PRACTICES-` +
  `BUG-KANDIDATEN`-Bloecke — direkt einarbeitbar (Research-Persistenz).
- **Beste Widerspruchs-Behandlung:** erklaert Konflikte statt sie nur zu listen (skalenabhaengige QPS).

### Wo eine billige Engine Opus einholt/schlaegt
- **Firecrawl gewinnt Thema 1 + ist bei harten Produkt-Specs aktueller:** volle Seiten fangen die
  Live-2026-NVMe-Werte (KVM 1 = 30 GB, KVM 8 = 240 GB), die Opus UND `:online` ueber Snippets verpassten.
- **Firecrawl Thema 8 praktischer:** komplette copy-paste-fertige MCP-Reverse-Proxy-Configs.
- **`:online` durchgehend zweiter, nie schwach, $0.22:** bei Thema 7 sogar ehrlicher (entlarvt
  Mem0-Marketing-Benchmark 94,4 % als reale 49 %); bei Thema 6 beste Kosten-/TCO-Zahlen.
- **Firecrawl-Schwaeche:** nur 4–5 Quellen → bricht bei breiten Themen ein (Thema 9 unbeantwortet).

### Kosten-Nutzen-Urteil
Opus-Eskalation lohnt **nur fuer die analytisch/sicherheitskritischen Themen** (5–10: Architektur,
Vektor-DBs, Memory-Stacks, Security, Multi-Service, Fallen) — dort tragen Synthese-Tiefe, Quellenbreite
und die `KANDIDATEN`-Bloecke den ~70–100×-Preis, gerade weil Fehlentscheidungen teuer werden
(47k-Loop, TLS-Strip, falsche DB-Wahl). Fuer **reine Faktenabfragen mit aktuellen Produkt-Specs**
(Themen 1–4) lohnt Opus NICHT — da war Firecrawl aktueller und `:online` fuer $0.22 voellig ausreichend.
**Faustregel:** Opus zahlt sich aus, wenn das Ergebnis ENTSCHEIDUNGEN traegt, nicht wenn es ZAHLEN nachschlaegt.

### Finale Drei-Stufen-Empfehlung (Second-Brain-Vorhaben)
1. **`:online` ($0.22) = Standard** fuer den Alltag und breite Recherche (bestes Preis-Leistungs-Verhaeltnis).
2. **Firecrawl = Verifikations-/Aktualitaets-Schicht** gezielt bei harten, frischen Produkt-Specs/Preisen.
3. **Opus = Eskalation NUR fuer entscheidungstragende, sicherheits-/architekturkritische Fragen**
   (Memory-Stack-Wahl, MCP-Security-Hardening, Multi-Service-Dimensionierung, Kosten-Fallen).
