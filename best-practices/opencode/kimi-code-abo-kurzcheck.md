# Kimi Code Abo Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Details im Volltext `kimi-code-abo.md`. Stand 29.09.2026.

- Drittanbieter-Key (OpenCode) belastet dasselbe Abo-Kontingent wie das offizielle CLI, keine Pro-Token-Abrechnung.
- `k3-256k` ≈ halber Quota-Verbrauch von `k3`; `kimi-for-coding-highspeed` ≈ 3× Quota, ab Pro.
- Kimi Code ≠ Moonshot Open Platform (getrennte Keys). Nur persönliche Nutzung; User-Agent nie fälschen.
- Fallen: `bugs/opencode/kimi-code-gateway.md`.
- Monat: `GET /coding/v1/usages`, `usages.limit_month_total.used_ratio × 100` **verbraucht**; `reset_time` übernehmen, nicht schätzen. Coding-Key regionsgebunden verwenden.
- Abo-Quote ≠ API-Dollar. K3-256K ungefähr ½ Abo-Verbrauch, kein veröffentlichter halber API-Tarif. `kimi-for-coding` = K2.8 Preview, nicht K2.7.
- Batch K2.6/K2.7 Code = 60 % Standardpreis, K2.6 Cache-read ausdrücklich 0,10 USD/1M. OpenRouter-Tarife nicht durch Moonshot ersetzen.
- Messages-Input schließt Cache aus, Chat/Responses-Input schließt ihn ein; Reasoning ist Output-Teilmenge. TTL5min/1h unterscheiden.
