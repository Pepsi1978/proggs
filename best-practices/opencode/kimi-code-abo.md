# Kimi Code (Kimi for Coding) in OpenCode — Best Practices

> **Stand:** 29.09.2026, 12:37 Uhr · **Anker:** Kimi Code Docs, offizieller kimi-code-Client `06ebfc821e30`, OpenCode 1.18.33, Sidebar 1.16.0.
> Engine C, sieben Researcher; offizielle Preis-/Modellseiten und Client-Quellcode. Monatsabfrage zusätzlich mit vorhandenem Coding-Key erfolgreich.
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
- Steuerungen: gleitendes 5-h-Fenster; neue Mitglieder ohne Wochenlimit (Legacy: 7 Tage ab Abodatum); gemeinsamer Monats-Pool. Eine verbindliche Token→Monats-Credit-Formel ist nicht veröffentlicht. Was das 5-h-Fenster misst (Anfragen/Token), ist offiziell nicht spezifiziert.
- Extra Usage: Pay-as-you-go ≈ API-Preise, Min. 25 CNY, umgeht Limits. Plan-Namen: Andante/Moderato/Allegretto/Allegro → Go/Plus/Pro/Max (CNY 49/99/199/699, laut Doku unverändert; USD-Seite widersprüchlich).
- Pay-per-Token-Alternative `kimi-k3` (platform.kimi.ai, 1M Kontext): $3 Input (Miss) / $0,30 Hit / $15 Output pro Mio Token. https://platform.kimi.ai/docs/pricing/chat

## Regeln
- Abo nur für persönliche Entwicklung; gewerblich → Open Platform. User-Agent unverändert lassen. Für stabile Ergebnisse `k3-256k`/`k3` pinnen. Modell-/Thinking-Wechsel können den Cache invalidieren; die aktuelle Doku nennt 256K→1M ausdrücklich als cacheerhaltende Ausnahme. `k3-256k` spart gegenüber K3 (1M) ≈ 50 % Quota, keine exakte Preisgarantie.

## Update 29.09.2026 — offizielle API-Preise (platform.kimi.ai/docs/pricing/chat)
Pro 1M Tokens, USD:
| Modell | Input | Output | Cache-Read | Cache-Write |
|--------|-------|--------|-----------|-------------|
| `kimi-k3` (1M Kontext) | 3,00 | 15,00 | 0,30 | 3,00 (TTL 5min) / 6,00 (TTL 1h) |
| `kimi-k2.7-code` | 0,95 | 4,00 | 0,19 | kein separater Write-Preis |
| `kimi-k2.7-code-highspeed` | 1,90 | 8,00 | 0,38 | kein separater Write-Preis |
| `kimi-k2.6` | 0,95 | 4,00 | 0,16 | kein separater Write-Preis |

- Cache-Hits verlängern kostenlos die ursprüngliche TTL. Chat/Responses schreiben standardmäßig 5min; Messages ohne Top-Level `cache_control` liest nur und schreibt nicht. 5min/1h-Pools sind getrennt. [offiziell] https://platform.kimi.ai/docs/guide/context-caching
- Code-Plan: `k3`/`k3-256k` haben **keinen separat veröffentlichten USD-Coding-Tarif**. `kimi-k3` dient lediglich als API-Vergleich. `kimi-for-coding` = **K2.8 Preview**, kein veröffentlichter passender USD-Tarif; HighSpeed hat K2.7-Code-HighSpeed als Modell-Pendant. [offiziell] https://www.kimi.com/code/docs/en/kimi-code/models.html
- **Korrektur zu Sidebar 1.15.0:** Abo-Nullpreise bedeuten keine fehlende API-Rechnung. 1.16.0 kennzeichnet Vergleichskosten, zeigt den ungefähren ½-Quotafaktor separat und entfernt die falsche K2.7-Zuordnung für K2.8 Preview. Fremdanbieter wie OpenRouter behalten ihre eigenen Tarife.
- **Batch kostet 60 %, nicht 50 %:** nur K2.6 und K2.7 Code belegt. Input/Output 0,57/2,40 USD; Cache-Read K2.6 ausdrücklich 0,10 (nicht rechnerisch 0,096), K2.7 Code 0,114. K3 wird nicht unterstützt. [offiziell] https://platform.kimi.ai/docs/pricing/batch · https://platform.kimi.ai/docs/guide/use-batch-api

## Monatsverbrauch und Reset im TUI

- `GET https://api.kimi.ai/coding/v1/usages` international, `.com` für China; Header `Authorization: Bearer <Coding-Key>` und `Accept: application/json`. Keine generischen Moonshot-/Anthropic-Keys als Fallback einsetzen.
- `usages.limit_month_total.used_ratio × 100` = **verbrauchter gemeinsamer Monatsanteil**, nicht verbleibend. `reset_time` ist ein absoluter Zeitstempel; in lokaler Zeitzone anzeigen. Numerische Strings sind möglich, ungültige/fehlende Werte bleiben unbekannt.
- `limit_month_code` ist der Coding-Anteil, `limit_7d` Woche, `limit_5h` Kurzzeitfenster. Keines ersetzt `limit_month_total`. Lokale Tokenhistorie erfasst andere Geräte und Kimi-Webnutzung nicht.
- Monatlichen Reset nicht aus Monatsanfang, Abodatum oder Zahlungsdatum schätzen. Neue Tarife ohne Wochenlimit, alte mit 7-Tage-Zyklus; beide mit Monatsgesamtlimit. Jahresabos erhalten Monatsleistungen; ungenutzte Monatsleistungen verfallen.
- Extra Usage ist separates, nicht verfallendes RMB-Guthaben. Optionales monatliches Ausgabenlimit und Monats-Abo sind verschiedene Größen. API-Dollarpreise garantieren keine identische Extra-Usage-Abbuchung.
- Polling begrenzen, Anfragen zusammenfassen, Timeout setzen und bei Fehlern `n/v` statt alter Kontodaten anzeigen. API-Key nur an die zur Provider-ID gehörende feste Region senden, ohne Redirects. Die Sidebar aktualisiert minütlich und nach Antworten.

Quellen [offiziell, 29.09.2026]:
- https://github.com/MoonshotAI/kimi-code/blob/06ebfc821e304bd4ca3f828ae739954166463d65/packages/oauth/src/managed-usage.ts
- https://github.com/MoonshotAI/kimi-code/blob/06ebfc821e304bd4ca3f828ae739954166463d65/packages/oauth/test/managed-usage.test.ts
- https://www.kimi.com/code/docs/en/kimi-code/membership.html
- https://www.kimi.com/user/agreement/paidServiceAgreement?version=v2
- https://www.kimi.com/user/agreement/extra-usage-rules-cn

Gegenprobe [extern, Quellcode der Integration]: CodexBar verarbeitet ebenfalls `limit_month_total`;
ältere OpenCode-Plugins, die nur `usage`/`limits[]` lesen, verpassen die Monatsquote.
https://github.com/steipete/CodexBar/blob/25bba9b7fd9ce83c33053958f7366e23b2dc8a82/Tests/CodexBarTests/KimiRatioPoolTests.swift

## Token- und Tarifsemantik

- Messages: gesamter Input = `input_tokens + cache_read_input_tokens + cache_creation_input_tokens`.
- Chat/Responses: `prompt_tokens`/`input_tokens` enthält bereits Cache-Read und -Write; nicht nochmals addieren.
- Reasoning ist eine Teilmenge des Outputs, TTL-Unterzähler eine Teilmenge der Cache-Creation. OpenCodes bereits normalisierte Tokens nicht erneut wie rohe Provider-Usage behandeln.
- Kosten aus disjunkten Mengen: ungecachter Input×3 + Read×0,30 + Write5m×3 + Write1h×6 + gesamter Output×15, jeweils /1M. Kein zusätzlicher Inputpreis auf Write-Tokens.
- Ohne TTL-Telemetrie ist K3-Write nur eine 5min-Schätzung. Coding-/Open-Platform-Parität der rohen Usage-Felder ist nicht umfassend zugesichert.
- Thinking off kann K3 im Coding-Dienst auf K2.8 Preview routen. Angeforderte Modell-ID beweist keinen Backend-Snapshot.

Quellen [offiziell]: https://platform.kimi.ai/docs/api/messages · https://platform.kimi.ai/docs/api/chat · https://platform.kimi.ai/docs/api/responses · https://platform.kimi.ai/docs/guide/use-thinking-models · https://www.kimi.com/code/docs/en/kimi-code/models.html

## OpenCode-Versionsanker dieser Integration

Am 29.09.2026 ist `v1.18.33` (veröffentlicht 28.09.2026, 04:22:46 UTC) das neueste stabile Release.
Tag-Commit `51ef4be1d3c122f18fefb510dca8d778571f4f18`, lokal in `1.18.33-windowsfix.23` enthalten.
Für künftige Prüfungen Tag-Referenz auflösen: `target_commitish` der Release-API ist hier ein anderer
Commit. Das archivierte `opencode-ai/opencode` ist ein anderes Projekt (heute Crush), kein Redirect.
`fix(console)` bezeichnet häufig die Web-Konsole, nicht die TUI.
[offiziell] https://github.com/anomalyco/opencode/releases/tag/v1.18.33 · https://api.github.com/repos/anomalyco/opencode/git/ref/tags/v1.18.33
