# Kimi Code (Kimi for Coding) in OpenCode — Best Practices

> **Stand:** 29.09.2026 · **Anker:** Kimi Code Docs (kimi.com/code/docs), K2.8 Preview = `kimi-for-coding`.
> Engine C, 4 Researcher; Seiten teils nur als WebFetch-Kurzfassung → Zahlen gegenprüfen.
> Fallen: `bugs/opencode/kimi-code-gateway.md`. Kurzcheck: `kimi-code-abo-kurzcheck.md`.

## Einrichtung
- Key in der Kimi-Code-Konsole (`kimi.com/code/console`, international auch über `kimi.ai`), bis 5 Keys. `opencode auth login` / `/connect` → „Kimi For Coding". Endpunkte: `https://api.kimi.com/coding/v1` (China) bzw. `https://api.kimi.ai/coding/` (international). **Kimi Code ≠ Moonshot Open Platform** (getrennte Keys, Platform = pay per token). [offiziell] https://www.kimi.com/code/docs/en/kimi-code/faq.html

## Modelle im Abo
| Modell | Kontext | Quota | Plan |
|--------|---------|-------|------|
| `k3` | 1M | ≈ 2× `k3-256k` | K3 ab Plus, 1M ab Pro |
| `k3-256k` | 256K fest | ≈ 0,5× `k3`, gleiche Ergebnisse innerhalb 256K | ab Plus |
| `kimi-for-coding` | 1M (K2.8 Preview) | nicht beziffert, sparsamer als K3 | ab Plus |
| `kimi-for-coding-highspeed` (= K2.7 Code HighSpeed) | 256K | ≈ 3×, ≈ 5–6× Ausgabetempo | ab Pro (sonst 401) |

Quelle: https://www.kimi.com/code/docs/en/kimi-code/models.html

## Kontingent & Kosten
- Alle Clients (CLI, VS Code, Desktop, **Drittanbieter-Tools per API-Key**) belasten **dasselbe Abo-Kontingent**, keine Pro-Token-Abrechnung; kein belegter Aufpreis gegenüber dem offiziellen CLI. [offiziell] https://www.kimi.com/code/docs/en/kimi-code/membership.html
- Steuerungen: gleitendes 5-h-Fenster; neue Mitglieder ohne Wochenlimit (Legacy: 7 Tage); gemeinsamer Monats-Credit-Pool nach realem Tokenverbrauch. Was das 5-h-Fenster misst (Anfragen/Token), ist offiziell nicht spezifiziert.
- Extra Usage: Pay-as-you-go ≈ API-Preise, Min. 25 CNY, umgeht Limits. Plan-Namen: Andante/Moderato/Allegretto/Allegro → Go/Plus/Pro/Max (CNY 49/99/199/699, laut Doku unverändert; USD-Seite widersprüchlich).
- Pay-per-Token-Alternative `kimi-k3` (platform.kimi.ai, 1M Kontext): $3 Input (Miss) / $0,30 Hit / $15 Output pro Mio Token. https://platform.kimi.ai/docs/pricing/chat

## Regeln
- Abo nur für persönliche Entwicklung; gewerblich → Open Platform. User-Agent unverändert lassen. Für stabile Ergebnisse `k3-256k`/`k3` pinnen; Modellwechsel invalidiert den Cache. `k3-256k` spart ≈ 50 % Quota.

## Update 29.09.2026 — offizielle API-Preise (platform.kimi.ai/docs/pricing/chat)
Pro 1M Tokens, USD:
| Modell | Input | Output | Cache-Read | Cache-Write |
|--------|-------|--------|-----------|-------------|
| `kimi-k3` (1M Kontext) | 3,00 | 15,00 | 0,30 | 3,00 (TTL 5min) / 6,00 (TTL 1h) |
| `kimi-k2.7-code` | 0,95 | 4,00 | 0,19 | kein separater Write-Preis |
| `kimi-k2.7-code-highspeed` | 1,90 | 8,00 | 0,38 | kein separater Write-Preis |
| `kimi-k2.6` | 0,95 | 4,00 | 0,16 | kein separater Write-Preis |

- Cache-Hits verlängern die Lebensdauer automatisch, ohne erneuten Write-Preis. Kein TTL angegeben → 5min-Tarif.
- Code-Plan-Mapping: `k3`/`k3-256k` = kimi-k3-Tarif, `kimi-for-coding` = k2.7-code, `kimi-for-coding-highspeed` = k2.7-code-highspeed.
- **models.dev-Lücke:** die Provider `kimi-code-plan-global`/`-cn` führen alle Modelle mit Nullpreisen (Abo), und bei allen Kimi-Modellen fehlt `cache_write`. Die TUI-Sidebar (token-cost-sidebar v1.15.0) hat diese Tarife deshalb lokal eingebaut (`withKimiPricing` in `dist/pricing.ts`): Nullpreise der Code-Plan-Provider werden als API-Vergleichskosten ersetzt, fehlender Cache-Write wird ergänzt, `:batch` halbiert.
