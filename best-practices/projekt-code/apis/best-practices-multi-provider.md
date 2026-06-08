# Provider-agnostische LLM-API-Architektur — Best Practices (Stand 2026-06-08)

> Architektur-Seite (kein direktes Bug-Gegenstück). Wie baut man eine Integration, die mehrere
> LLM-Anbieter sauber unterstützt. Quellen: Vercel AI SDK, AWS Bedrock Converse, LiteLLM (offiziell);
> Engineering-Blogs als extern. (Researcher-Recherche 2026-06-08.)

## A. Provider-Abstraktion
1. **Unified-Interface/Adapter** gegen EIN normalisiertes Interface (Messages/Tools/Streaming anbieterübergreifend). Reife Umsetzungen: Vercel AI SDK (`LanguageModel`-Spec), AWS Bedrock `Converse`, LiteLLM. Anbieterwechsel mit ~2 Zeilen. Quelle: https://ai-sdk.dev/docs/foundations/providers-and-models · offiziell
2. **Eigene Adapter** für Nicht-Standard-Backends (Ollama/self-hosted) gegen die offene Spec, statt Kern verbiegen. · offiziell
3. **OpenAI-kompatibles Interface** als gemeinsamer Nenner wo möglich (niedrige Migrationskosten). · extern

## B. Gateway vs. direkter Call
4. **Schwelle:** Gateway erst ab >1 Provider ODER >einige 100 $/Monat Spend; darunter direkte SDKs. Quelle: https://www.digitalapplied.com/blog/llm-gateway-architecture-2026-engineering-reference · extern
5. **Werkzeugwahl nach Engpass:** „viele Modelle, ein Key" → OpenRouter; „Routing/Logs/Budgets über viele Apps" → Portkey/LiteLLM; „AI-Features auf Vercel" → Vercel AI Gateway. Quelle: https://vercel.com/docs/ai-gateway · offiziell
6. **Managed vs. self-hosted** bewusst: self-hosted (LiteLLM) erst bei sehr hohem Spend/Compliance. · extern

## C. Fallback / Routing
7. **Drei GETRENNTE Fallback-Typen** (meist falsch gemacht): general (429/5xx/Timeout)→nächster Provider+Backoff; content_policy→Provider mit anderen Guardrails; context_window→größeres Modell. Quelle: https://docs.litellm.ai/docs/proxy/reliability · offiziell
8. **Resilienz layern:** num_retries + request_timeout + allowed_fails + cooldown_time (failing Modell temporär aus) + geordnete Fallback-Liste (in-order, `order`-Param). · offiziell
9. **KISS:** simpel starten (primary+1 Fallback), datengetrieben erweitern. · extern
10. **Fallbacks testbar:** `mock_testing_fallbacks=true`. · offiziell

## D. Konfiguration
11. **Modelle/Keys/Endpunkte aus Config/Env**, App referenziert nur logische Namen; Provider per Deployment-Kontext (dev→Ollama, prod→Cloud). · offiziell
12. **Routing-Strategie explizit** wählen (Simple-Shuffle/Latency/Usage/Cost/…), nicht Default. · offiziell
13. **Pre-Call-Context-Checks** (`enable_pre_call_checks`): Requests über Token-Limit VOR dem Call ablehnen. · offiziell

## E. Capability- & Kosten-Routing
14. **Capability-Detection vor Modellwahl** (kann das Modell Tools/JSON/Image?). · offiziell
15. **Kosten-/Latenz-Routing nach Aufgabe** (günstig für einfache Tasks, Reasoning nur wo nötig); Caching (Hit <5 ms vs. 2–5 s). · extern
16. **Streaming + Tool-Use über dieselbe Abstraktion** (Bedrock ConverseStream, Vercel AI SDK 5). · offiziell

**Quellen:** ai-sdk.dev · vercel.com/docs/ai-gateway · docs.litellm.ai/docs/proxy/reliability + /routing + /configs · docs.aws.amazon.com/bedrock Converse · digitalapplied.com (extern) · pinggy.io (extern)
