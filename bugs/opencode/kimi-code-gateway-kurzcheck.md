# Kimi Code Gateway Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Trifft ein Punkt zu oder tritt ein Fehler auf → VOLLTEXT
> `kimi-code-gateway.md` lesen. Stand 29.09.2026, Anker: Kimi Code Docs 29.09.2026.

## ⚡ Kurzcheck

| # | Signal | Sofort-Regel | Volltext |
|---|--------|--------------|----------|
| 1 | 403 „only available for Coding Agents" / 429 „engine overloaded" | Gateway prüft Client-Kennung. Nur dokumentierte Clients (Claude Code, OpenCode, Codex, Hermes). **User-Agent nie fälschen** (Verstoß, Vorteile weg). | #1, #2 |
| 2 | 403 trotz gültigem Key | Ist meist ein Kontingent-Limit (5 h / 7 Tage / Monat), kein Auth-Fehler. 402 = Mitgliedschaft nicht verifizierbar. | #3 |
| 3 | 401 bei `kimi-for-coding-highspeed` | Erst ab Pro. K3 ab Plus, 1M-Kontext ab Pro. | #4 |
| 4 | Modell „ändert sich" | `kimi-for-coding` wandert auf neue Modelle (K2.8 Preview). `k3`/`k3-256k` explizit pinnen. | #6 |
| 5 | K3-256K zeigt gleichen USD-Preis | ≈½ betrifft Abo-Kontingent, nicht belegten API-Dollarpreis. Vergleichspreise kennzeichnen. | #7 |
| 6 | Batch pauschal 50 % / OpenRouter mit Moonshot-Tarif | K2-Batch 60 %, K2.6 Cache-read 0,10; Anbieter getrennt halten. | #8 |
| 7 | Monatsanzeige fehlt/falscher Reset | Nur `usages.limit_month_total`, Ratio ×100 verbraucht, Datum aus `reset_time`. | #9 |
| 8 | Token/Cache doppelt gezählt | Messages-Input exklusiv Cache, Chat/Responses inklusiv; Output inklusiv Reasoning. | #10 |
| 9 | TUI-SDK beanstandet `path`/`body` | SDK v2 verlangt flache Parameter (`sessionID`, `directory`, …). | #11 |
| 10 | 5h zeigt 0 %, Webseite positiven Verbrauch | Gleiches aktives 5h-Fenster in `limits[]` mit `used / limit` auswerten, Reset abgleichen. Gilt auch bei vorhandenem Monatsfeld. | #12 |
