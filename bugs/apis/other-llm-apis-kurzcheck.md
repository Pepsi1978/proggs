# Weitere LLM-APIs (Survey + Integrations-Fallen) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Bedrock/Vertex/Azure | KEIN simpler Bearer-Key: SigV4 / ADC / api-key | §5, §8, §6 |
| 2 | Cohere RAG/Citations | Native v2-API; Kompat-Layer lehnt viele Params ab | §1 |
| 3 | Perplexity `sonar` | `search_*` via `extra_body`; `citations`/`search_results` lesen | §4 |
| 4 | Fireworks Modell-ID | Vollen Pfad `accounts/fireworks/models/...`, nicht Kurzname | §3 |
| 5 | Bedrock neue Modelle | Geo-Präfix-Profil (`us.`/`eu.`/`global`), nicht nackte ID | §5 |
| 6 | Together Rate-Limit | Gleichmaessig ~1 req/s; 429+503 getrennt mit Backoff | §2 |
| 7 | Cerebras Free | 8k-Context-Cap + 30 RPM; nicht fuer Produktion | §7 |
| 8 | Modell-IDs allgemein | Nie hardcoden, Fallback bei Deprecation (Cerebras 27.05.2026) | §7 |
