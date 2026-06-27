# Weitere LLM-APIs Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Cohere RAG/Tools | Native v2-API; `response_format`-Schema; `strict:true` | §1 |
| 2 | Together Traffic | Gleichmaessig senden; 429+503 getrennt mit Backoff; `x-ratelimit-reset` | §2 |
| 3 | Fireworks | Voller Pfad-ID; Structured Output; On-Demand statt Serverless | §3 |
| 4 | Perplexity | `search_*` via `extra_body`; `enable_search_classifier` nutzen | §4 |
| 5 | Bedrock | `Converse`-API; SigV4/IAM; Cross-Region Inference Profiles | §5 |
| 6 | Azure OpenAI | Deployment-Name statt Modell; `api-version` aktuell; AzureOpenAI-Client | §6 |
| 7 | Cerebras/Vertex | Modell-IDs nicht hardcoden; Vertex via ADC/SA, kein Key | §7, §8 |
| 8 | HF Providers | Router-Endpunkt + HF-Token; Provider per Suffix (`:cheapest`) | §9 |
