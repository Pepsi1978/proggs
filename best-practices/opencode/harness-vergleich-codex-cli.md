# Harness-Vergleich: Codex CLI vs. OpenCode beim gleichen OpenAI-Modell — Best Practices (Stand 2026-09-04)

> **Frage, die dieser Text beantwortet:** Wenn dasselbe Modell (z. B. `gpt-6-astra`, `gpt-5.6-*`)
> einmal im offiziellen Codex CLI und einmal in OpenCode laeuft — kostet es unterschiedlich viel
> Tokens, und wird es unterschiedlich "intelligent"? Antwort: **ja, beides** — nicht wegen des
> Modells, sondern wegen des Harness.
> **Anker:** opencode=1.18.28 · Codex CLI 0.1xx-Serie (2026) · Recherche 2026-09-04, Engine B (7 Researcher).
> Quellen: `offiziell` (developers.openai.com, opencode.ai/docs, github.com/openai/codex) bzw. `extern`.

---

## 1. Token-Overhead pro Turn (Grundlast, bevor der Benutzer etwas tippt)

| Harness | Grundlast/Turn | Zusammensetzung |
|---|---|---|
| **Codex CLI** | **~2.000–5.000 Tokens** | eingebauter System-Prompt (hardcodiert im Binary) + AGENTS.md-Schicht `extern` |
| **OpenCode** | **~7.000 Tokens** | ~9.300 Zeichen System-Prompt (1 Block) + 10 Built-in-Tools (~21.000 Zeichen Schemata) `extern` |
| (Vergleich) Claude Code | ~33.000 Tokens | ~27.000 Zeichen Prompt (3 Bloecke) + 27 Tools (~100.000 Zeichen Schemata) `extern` |

- **Ohne Tool-Definitionen** liegt OpenCode bei ~2.000 Tokens reinem Prompt-Anteil. Der Grossteil
  seiner Grundlast sind also die **Tool-Schemata**, nicht der Prompt. `extern`
- **AGENTS.md-Deckel bei Codex:** `project_doc_max_bytes` = **32 KiB** (Default). Darueber hinaus
  werden weitere AGENTS.md-Dateien nicht mehr angehaengt. `offiziell`
- **Die eigenen Regeldateien dominieren beides:** 72 KB Instruction-Files entsprechen **+20.000 Tokens**
  pro Anfrage; 5 MCP-Server **+5.000–7.000 Tokens** pro Anfrage. Das ist um ein Vielfaches groesser
  als der Unterschied zwischen den beiden Harnessen selbst. `extern`

> **Praxis-Schluss:** Der Harness-Unterschied (~2–5k vs. ~7k) faellt gegen eine grosse AGENTS.md/
> CLAUDE.md und aktive MCP-Server kaum ins Gewicht. Wer Tokens sparen will, kuerzt zuerst die
> eigenen Regeln und schaltet MCP-Server ab — nicht den Harness.

---

## 2. Prompt-Caching — der eigentliche Kostenhebel (und Codex' Strukturvorteil)

- Codex CLI legt System-Instructions, Tool-Definitionen und Sandbox-Konfiguration bewusst in ein
  **stabiles Prefix**, damit OpenAIs serverseitiger Prompt-Cache greift. `extern`
- Gemessene Wirkung (Lumer et al., ueber 500 Sessions): **41–80 % Kostenreduktion**, **13–31 %
  schnellerer Time-to-First-Token**. "System-prompt-only caching" ist dabei die **konsistenteste**
  Strategie. `extern`
- **Beim Umzug in einen Fremd-Harness geht das teilweise verloren:** "your system prompt layout
  differs, and every request pays the full input-token price". `extern`
- Fuer OpenCode ist in den Quellen **kein** eigener Prompt-Cache-Layer dokumentiert (Wissensluecke,
  nicht bewiesene Abwesenheit). Anthropic-Modelle brauchen dort `setCacheKey:true`.
- **Cache-Killer:** jede Aenderung am stabilen Prefix (Regeldatei, Tool-Liste, CLI-Update)
  invalidiert ihn. `/usage` in Codex CLI ab 0.55.0 macht Cache-Hits sichtbar. `extern`

> **Wichtiger als die Grundlast:** Ein Turn mit Cache-Hit kostet einen Bruchteil eines Turns ohne.
> Codex CLI ist darauf hin gebaut; OpenCode nicht in gleichem Mass.

---

## 3. Kontext-Management — der groesste architektonische Unterschied

| Aspekt | Codex CLI | OpenCode |
|---|---|---|
| Compaction-Strategie | "summarise and replace": ganze Historie zum LLM, Summary zurueck, **Original physisch geloescht** | "stepped governance": timestamp-basiertes **Verstecken** + 5-Heading-Summary + Replay der letzten Instruktion |
| Datenverlust danach | **ja**, irreversibel (Detail-Tool-Outputs weg) | **nein**, Original bleibt im Storage, wird nur ausgefiltert |
| Trennung History/Kontext | nicht getrennt | **"stored history" ungleich "model context"** — Pruning ohne History-Verlust |
| Ausloeser | dual-trigger (pre-turn + mid-turn), **harter 90-%-Clamp** ab v0.100.0 | beim Ueberschreiten des Modell-Limits (keine Prozentzahl dokumentiert) |
| Manuell | `/compact` (ab v0.117.0 mit Follow-up-Queue) | in den Quellen nicht beschrieben |
| Kontextfenster in der Praxis | GPT-5.5: API 1.050.000, Codex kappt auf **400.000**, davon 128.000 fuer Output reserviert, ~5 % Headroom, also **~258.000 Input nutzbar** | keine harten Zahlen dokumentiert |
| Bekannte Bugs | Issue #19842 (Kontext laeuft voll ohne Compaction), Issue #8481 (Compaction-Loop), "compaction death spiral" bei `xhigh` in v0.112, Issue #27522 (1M-Konfig faellt auf 258k zurueck) | keine vergleichbaren Bugs in den Quellen |

`offiziell`/`extern`

> **Praxis-Schluss:** Bei langen Sitzungen verliert Codex CLI Details unwiederbringlich; OpenCode
> behaelt sie im Storage. Wer stundenlange Sessions faehrt und spaeter zurueckblaettern will, ist
> in OpenCode besser aufgehoben. Wer maximale Modell-Treue pro Turn will, in Codex CLI.

---

## 4. "Intelligenz"-Unterschied: der Harness-Effekt ist real und messbar

- **Gleiches Modell, anderer Harness, anderes Ergebnis** — belegt an GPT-5.5 auf Terminal-Bench:
  **Codex CLI 83,4 % vs. Terminus 2 78,2 %** (5,2 Punkte allein durch den Harness). `extern`
- Endor Labs, zitiert: *"the harness shapes outcomes more than the model alone"*. `extern`
- Extremfall derselben Modellbezeichnung in zwei Scaffolds: GPT-5 mit OpenHands **21 %** (SWE-EVO)
  vs. **65 %** (SWE-bench Verified). `extern`
- **Hersteller-Scaffolding blaeht Zahlen um 4–12 Punkte** gegenueber neutraler Auswertung. `extern`
- Direkter Head-to-Head Codex CLI vs. OpenCode (eine Quelle, Codex-affin): Codex **+2,8 Punkte
  Geschwindigkeit**, **+1,1 Punkte Genauigkeit**. `extern` — Groessenordnung, kein harter Beleg.
- **Wichtige Luecke:** Es existiert **kein** sauberer, oeffentlicher Head-to-Head-Benchmark
  "gleiches GPT-5.x in Codex CLI vs. OpenCode" mit Tool-Call-Erfolgsraten. Das Projekt
  [`harness-bench`](https://github.com/TheLime1/harness-bench) verfolgt genau dieses Ziel, hat aber
  keine indexierten Ergebnistabellen.

### Woher der Unterschied technisch kommt

1. **`apply_patch` / V4A-Diffs — der am staerksten belegte Punkt.** GPT-5.3-Codex (und Nachfolger)
   wurden **auf `apply_patch` post-trained**, inklusive konkreter Tool-Namen (`rg` statt `grep`).
   In einem Fremd-Harness entsteht ein **"tool mismatch — the model emits V4A diffs but your harness
   expects string replacements"**. `apply_patch` ist ein Responses-API-Custom-Grammar-Tool und
   verlangt Codex-eigene Request-Header (`x-client-request-id`, `x-codex-window-id`). `extern`
2. **Modelle sind auf ihren Harness trainiert:** *"Frontier models are post-trained on their
   harnesses."* GPT-5-Codex ist laut OpenAI *"optimized for agentic coding in Codex or Codex-like
   environments"* und **nur ueber die Responses API** verfuegbar. `offiziell`
3. **AGENTS.md-Qualitaet schlaegt durch (ETH-Zuerich-Studie):** von Menschen geschriebene AGENTS.md
   **+4 %**, LLM-generierte **-20 %**. Welche Kontextdatei der Harness einliest, veraendert das
   Ergebnis desselben Modells massiv. `extern`
4. **Reasoning-Effort** ist ein API-Setting, das die Qualitaet verschiebt, ohne dass Modellname oder
   Agent-Code sich aendern — bei Vergleichen muss es fixiert werden. `extern`

---

## 5. Was nur das Codex CLI kann (bzw. nur dort offiziell existiert)

- **Plattformnatives OS-Sandboxing** (macOS/Linux/WSL2/Windows), das **auch auf abgespawnte Prozesse**
  wirkt (`git`, Paketmanager, Test-Runner erben die Grenzen). `offiziell`
- **Approval-Policy als eigene Schicht** neben der Sandbox; fuer Teams
  `approvals_reviewer = 'guardian_subagent'`. `offiziell`/`extern`
- **`/review`** — dedizierter Review gegen Base-Branch / uncommitted Changes / einen Commit, mit
  priorisierten Findings **ohne** den Working Tree anzufassen. `offiziell`
- **Cloud-Tasks**: parallele Cloud-Umgebungen, aus der IDE delegierbar, Diffs lokal anwendbar;
  Oberflaeche unter chatgpt.com/codex. `offiziell`
- **GitHub-Integration**: `@codex` auf Issues/PRs taggen. `offiziell`
- **First-Party-Websuche mit OpenAI-gepflegtem Cache-Index** (`web_search = cached|live|disabled`) —
  geringeres Prompt-Injection-Risiko als Live-Fetch. `offiziell`
- **ChatGPT-Plus/Pro-Auth ohne API-Key** — Nutzung im Abo enthalten (Rate-Limits werden von OpenAI
  bewusst nicht beziffert). `extern`
- **Hardware-Pfade**: GPT-5.3-Codex-Spark auf Cerebras WSE-3, ueber 1.000 Tokens/s, ~15x schneller
  als der Standard-Pfad. `extern`

> **Einschraenkung, die ehrlich dazugehoert:** keine Quelle bezeichnet diese Punkte als "exklusiv".
> Ein Fremd-Harness *koennte* vieles nachbauen — die genannten Oberflaechen existieren aber als
> solche nur im OpenAI-Produkt, und der Abo-Zugang ist vertraglich gebunden.

---

## 6. Was nur OpenCode kann

- **75+ Provider** ueber models.dev in einem Binary, inkl. lokaler Modelle (Ollama, LM Studio);
  Codex CLI *"cannot run a local Llama model"*. `extern`
- **Kein Datenverlust bei Compaction** (siehe §3) — lange Sessions bleiben nachvollziehbar.
- **LSP-Integration**, custom Agents (`general`/`build`/`plan`), Plugins, MCP — und OpenCode kann
  **selbst als MCP-Server** exponiert werden (21 Tools), also als Subagent anderer Tools laufen. `extern`
- **Deutlich reichhaltigeres TUI**: Attention-Notifications mit Sound-Packs pro Event-Typ
  (`question`/`permission`/`error`/`done`/`subagent_done`), Command-Palette. `offiziell`
- **Privacy-first**: keine Telemetrie. `extern`
- **Ein Harness fuer alle Modelle** — Anthropic, OpenAI, Google, lokal, ohne Werkzeugwechsel.

---

## 7. Entscheidungsregel fuer die Praxis

| Situation | Wahl |
|---|---|
| Grosse, praezise Code-Aenderungen mit einem OpenAI-Codex-Modell; Diff-Treue zaehlt | **Codex CLI** (`apply_patch`-Fit + Cache) |
| ChatGPT-Abo statt API-Rechnung nutzen | **Codex CLI** |
| Sandbox/Approval/Review/Cloud/GitHub-Workflow gewuenscht | **Codex CLI** |
| Modelle mischen (Anthropic + OpenAI + lokal) in EINEM Werkzeug | **OpenCode** |
| Lange Sessions, in denen nichts verloren gehen darf | **OpenCode** |
| Eigene Agents/Plugins/LSP/TUI-Komfort, Skills-Oekosystem | **OpenCode** |
| Token sparen | **beides sekundaer** — zuerst AGENTS.md kuerzen und MCP-Server abschalten |

---

## 8. Offen / nicht belegt

- Exakte Token-Zahl des Codex-Built-in-Prompts ohne AGENTS.md — keine Quelle nennt eine Zahl.
- OpenCode-Prompt-Caching: nicht dokumentiert (weder bestaetigt noch widerlegt).
- Ob Codex CLI einen bevorzugten **Service-Tier** (Priority-Routing, hoehere Limits) bekommt, den
  OpenCode ueber dieselbe API nicht bekommt — **nicht belegt**.
- "Encrypted reasoning content" als Codex-Vorteil — in keiner Quelle erwaehnt.
- **`gpt-6-astra` selbst** taucht in keiner oeffentlichen Quelle auf; alle Aussagen hier gelten fuer
  die GPT-5.x-Codex-Generation und sind auf Astra nur uebertragen, nicht belegt.
- Tool-Call-Erfolgsraten Codex CLI vs. OpenCode bei identischem Modell — nirgends beziffert.
- Ein Grossteil der Vergleichszahlen stammt aus **einer** Codex-affinen Quelle
  (`codex.danielvaughan.com`) — Zahlen als Groessenordnung lesen, nicht als Messwert.

---

## Quellen (Auswahl)

- https://developers.openai.com/codex/guides/agents-md/ · https://developers.openai.com/codex/prompting
- https://developers.openai.com/codex/concepts/sandboxing · https://developers.openai.com/codex/cli/features
- https://developers.openai.com/codex/cloud · https://developers.openai.com/codex/ide/features
- https://developers.openai.com/api/docs/models/gpt-5-codex · https://openai.com/index/unrolling-the-codex-agent-loop
- https://github.com/openai/codex/issues/11805 · /19842 · /27522 · https://github.com/openai/codex/releases/tag/v0.117.0
- https://opencode.ai/docs/tools · https://opencode.ai/docs/tui
- https://codex.danielvaughan.com/2026/04/08/codex-cli-performance-optimization/
- https://codex.danielvaughan.com/2026/04/28/codex-models-third-party-harnesses-apply-patch-v4a-portable-agent/
- https://codex.danielvaughan.com/2026/04/14/context-compaction-deep-dive-codex-cli-claude-code-opencode/
- https://codex.danielvaughan.com/2026/06/11/terminal-bench-2-1-june-2026-benchmark-landscape-codex-cli-harness-engineering-model-scores
- https://codex.danielvaughan.com/2026/04/09/opencode-vs-codex-cli
- https://vxlnews.com/a/claude-code-vs-opencode-token-overhead
- https://www.developersdigest.tech/blog/claude-code-token-overhead-opencode-comparison
- https://gist.github.com/migom6/70ccd3485ea4db9dd8039245cd9dde4a
- https://kangwooklee.com/blogs/codex_context_compaction.html
- https://arxiv.org/pdf/2606.17799 · https://github.com/TheLime1/harness-bench

## Lokale Codex-Kostenstatuszeile (05.09.2026)

Codex CLI 0.153.2 bietet für `tui.status_line` fest definierte Felder, keinen
frei programmierbaren Statuszeilen-Befehl. Für eine eigene API-Kostenrechnung
muss die native TUI angepasst werden. Die lokale Erweiterung steht unter
`Statusline-Codex/` und übernimmt ihre Preise aus der bestehenden
OpenCode-Seitenleiste einschließlich des dort konfigurierten 20-%-Cache-Read-Aufschlags.

Codex-Input enthält Cache-Tokens; Codex-Output enthält Reasoning. Vor der separaten
Bepreisung beide Anteile abziehen, damit keine Doppelzählung entsteht. Jeden Aufruf
mit seinem damaligen Modell, Service-Tier und Kontextpreis summieren; keine ganze
Session nachträglich mit dem zuletzt ausgewählten Modell bepreisen. Resume aus dem
Rollout rekonstruieren und doppelte Token-/Quota-Meldungen überspringen. Fehlende
Tarife und Historie als Teilbetrag kennzeichnen. Schätzung ist API-Vergleich, nicht
ChatGPT-Aborechnung. Nach einem nativen Build CLI vollständig neu starten.

Primärquellen, direkt mit Codex gelesen:
- https://developers.openai.com/api/docs/models/gpt-6-astra
- https://developers.openai.com/api/docs/pricing
- https://github.com/openai/codex/blob/rust-v0.153.2/codex-rs/tui/src/chatwidget/status_surfaces.rs
