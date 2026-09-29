# Anthropic Claude API (Messages API) — Best Practices (Stand 2026-09-29)

> Gegenstück zu `bugs/apis/anthropic-api.md`. Offiziell (platform.claude.com). (Researcher-Recherche 2026-06-08, Re-Recherche 2026-07-02.)
> Update 2026-07-02: Keine belegten neuen API-Bugs seit 2026-06-08 gefunden; die Regeln zu Adaptive Thinking, Prefill-Verbot, Tool-Use-Paarung, Prompt-Caching und Batch-API bleiben unveraendert gueltig.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Prompt Caching | `cache_control` auf letzten STATISCHEN Block, Mindest-Tokens beachten | Prompt Caching |
| 2 | Tool Use | Detaillierte Descriptions (3–4 Sätze), `strict:true`, Tools namespacen | Tool Use |
| 3 | Tool-Fehler | Via `is_error:true` + lehrreiche Meldung (Claude retryt 2–3×) | Tool Use |
| 4 | Extended Thinking (Opus 4.8/4.7) | Nur Adaptive Thinking + `effort`; thinking-Blöcke 1:1 in History | Extended Thinking |
| 5 | Message-Struktur | Keine `tool`/`function`-Rollen; `system` Top-Level (Array für Caching) | Message-Struktur & Streaming |
| 6 | Streaming bei hohem max_tokens | SDK `.stream()` + `get_final_message()` (vermeidet Timeouts) | Message-Struktur & Streaming |
| 7 | Async / Kostenersparnis | Batch API = 50 %; mit 1h-Cache kombinieren | Batch API & Token-Counting |
| 8 | Modellwahl Sonnet 5.5 vs. Opus 5.5 | Kosten pro Aufgabe vergleichen; Sonnet 5.5 auf high/xhigh, max nur gezielt | Modellwahl & Effort |
| 9 | Migration Sonnet 5 → 5.5 | kein `thinking:disabled`, kein forced `tool_choice`, neues Computer-Toolset | Modellwahl & Effort |

## Prompt Caching
- `cache_control` IMMER auf den letzten STATISCHEN Block (Reihenfolge tools→system→messages), NIE auf wechselnden Inhalt. Mindest-Tokens beachten (Opus 4.8/Sonnet 4.6 = 1024; Opus 4.7/4.6 + Haiku 4.5 = 4096; Haiku 3.5 = 2048) sonst stiller Miss. 5m-TTL (Default, write 1,25×) bei <5-Min-Takt; 1h (write 2,0×) bei Agentic/>5 Min; Read immer 0,1×. Max 4 Breakpoints; Pre-Warming mit `max_tokens:0`; Tool-JSON-Key-Reihenfolge stabil. `usage`-Felder (cache_creation/cache_read_input_tokens) monitoren. Invalidierung: Tool-Defs ändern→ganzer Cache weg. Quelle: https://platform.claude.com/docs/en/build-with-claude/prompt-caching · offiziell

## Tool Use
- Sehr detaillierte Tool-Descriptions (3–4 Sätze: was/wann/Parameter/Limits) — wichtigster Faktor. `strict:true` auf Tool-Defs (+ `tool_choice:{type:"any"}` für garantiert validen Call). Tools konsolidieren + namespacen (`github_list_prs`). `tool_result` MUSS unmittelbar auf `tool_use` folgen, passende `tool_use_id`, im content-Array ZUERST. Fehler via `is_error:true` + lehrreiche Meldung (Claude retryt 2–3×). Untrusted Content in `tool_result` isolieren (Prompt-Injection-Schutz). `tool_choice` bei Extended Thinking nur auto/none. Quelle: https://platform.claude.com/docs/en/agents-and-tools/tool-use/define-tools · offiziell

## Extended Thinking
- Opus 4.8/4.7: nur Adaptive Thinking (`{"thinking":{"type":"adaptive"}}` + `effort` low/medium/high) — manuelles `budget_tokens` → 400. Sonnet 4.6/Opus 4.6: adaptive bevorzugt (manuell deprecated; Leitwerte alt: 5–10k/10–20k/20–32k). thinking-Blöcke (inkl. `signature`) unverändert in History (Opus 4.5+/Sonnet 4.6+ behalten sie). Interleaved Thinking (Opus 4.8/4.7/4.6 automatisch; sonst Beta-Header). `display:"omitted"` für schnelleres TTFT. Quelle: https://platform.claude.com/docs/en/build-with-claude/extended-thinking · offiziell

## Message-Struktur & Streaming
- Keine `tool`/`function`-Rollen — alles in user/assistant mit text/image/tool_use/tool_result-Blöcken; system Top-Level (Array für Caching). SDK `.stream()`-Helfer nutzen (`stream.text_stream` / `.on("text")`); bei hohem `max_tokens` `get_final_message()` (Streaming verlangt, vermeidet Timeouts). Quelle: https://platform.claude.com/docs/en/build-with-claude/streaming · offiziell

## Batch API & Token-Counting
- Batch API für 50 % Ersparnis (async, bis 100k Req/256 MB, 24h-Fenster, Ergebnisse 29 Tage); Batch + 1h-Cache kombinieren; `max_tokens:0`/`stream:true` im Batch nicht erlaubt, `max_tokens>=1` Pflicht. Token-Counting vorab; Tool-Use addiert System-Prompt-Tokens (Opus 4.8: 290 auto/none, 410 any/tool). Quelle: https://platform.claude.com/docs/en/build-with-claude/batch-processing · offiziell

## Modellwahl & Effort (Sonnet 5.5 / Opus 5.5, Stand 2026-09-29)
- **Preise:** Sonnet 5.5 = 2 $/10 $ pro Mio. Token (wie Sonnet 5), Cache-Read 0,20 $, Cache-Write 2,50 $; Opus 5.5 = 4 $/20 $. Beide 1M Kontext, 128K Output (Batch bis 300K mit Beta-Header `output-300k-2026-03-24`). Min. cachebarer Prompt bei Sonnet 5.5 = 512 Token (Sonnet 5: 1.024). Quelle: https://platform.claude.com/docs/en/models/overview · offiziell
- **Effort neu kalibrieren:** Default-Effort Sonnet 5.5 = high; Anthropic empfiehlt für agentisches Coding den Start bei medium. Höher ist nicht immer besser: FrontierCode 1.1 Sonnet 5.5 xhigh 52,1 % vs. max 46,2 %. Quelle: Sonnet 5.5 System Card §8.4 · offiziell
- **Kosten pro Aufgabe statt Tokenpreis vergleichen:** Artificial Analysis: Sonnet 5.5 auf Max braucht ~193k Output-Token pro Task (7,60 $) und ist dort teurer als Opus 5.5 Max (5,98 $); auf High (Index 47, 1,08 $) und Xhigh (Index 52, 2,74 $) deutlich günstiger. → Sonnet 5.5 standardmäßig auf high/xhigh, max nur gezielt. Quelle: https://artificialanalysis.ai/articles/claude-sonnet-5-5 · extern
- **Wann Opus 5.5:** Sonnet 5.5 ist bei Terminal-Bench 4.0 (70,6 vs. 66,4), OSWorld 2.1, GDPval-AA und Toolathlon praktisch gleichauf; Opus 5.5 klar vorn bei langen Coding-Aufgaben (SWE-bench Pro 89,9 vs. 81,3; ProgramBench 91,2 vs. 79,7) und HLE ohne Tools (64,4 vs. 56,9). Quelle: Sonnet 5.5 System Card Tab. 8.1.A · offiziell
- **Migration von Sonnet 5:** `thinking` nicht mehr `disabled` (→ `between_tools`), `tool_choice` nur `auto`/`none`, `computer_toolset_20260801`, Historie append-only. Details + Fehlerbilder: `bugs/apis/anthropic-api.md` §31–§35. Quelle: https://platform.claude.com/docs/en/models/sonnet-5-5/whats-new-sonnet-5-5 · offiziell
- **Cyber-Safeguards:** Sonnet 5.5 blockt mehr Cyber-Anfragen (auch harmlose); bei Block Fallback auf Sonnet 5 (API nur per Opt-in). Quelle: Sonnet 5.5 System Card §3.3 · offiziell

## 🔗 Bezug zum Bug-Almanach
| Best-Practice | Bug-Abschnitt (`bugs/apis/anthropic-api.md`) |
|---|---|
| Prompt Caching | 14–17 |
| Tool Use | 7–8 |
| Extended Thinking | 18–20 |
| Message/Streaming | 3–5, 9–13, 25 |
| Batch/Counting | 28–29 |
| Modellwahl & Effort | 31–36 |
