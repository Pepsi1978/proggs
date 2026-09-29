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
