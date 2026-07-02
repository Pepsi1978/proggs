# Weitere LLM-APIs — Best Practices (Stand 2026-07-02)

> Gegenstueck zu `bugs/apis/other-llm-apis.md`. Offiziell empfohlen (Quellen). (Researcher-Recherche 2026-06-09, Re-Recherche 2026-07-02.)
> Ein Abschnitt pro Anbieter — gleiche Anbieter-Liste wie die Bug-Datei.
> Update 2026-07-02: Cerebras `gemma-4-31b` ist als Image-Input-Preview interessant; alte Cerebras-API/Parameter vor 21.07.2026 migrieren.

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

## 1. Cohere (Command, Rerank, Embed)
- Fuer RAG/Citations/Connectors die **native v2-API** nutzen, nicht den OpenAI-Kompat-Layer — `connectors`, `documents`, `citation_options` fehlen dort. Quelle: https://docs.cohere.com/docs/compatibility-api · offiziell
- **Structured Outputs** ueber `response_format` mit JSON-Schema setzen (garantiert Schema-Konformitaet, kein nachgelagertes Validieren noetig). Quelle: https://docs.cohere.com/docs/compatibility-api · offiziell
- Bei **Tool-Calling** `strict: true` setzen — jeder generierte Tool-Call folgt dann exakt dem Schema (wichtig fuer RAG-Pipelines mit festen Funktionssignaturen). Quelle: https://docs.cohere.com/docs/compatibility-api · offiziell
- Nicht unterstuetzte OpenAI-Params (`n`, `logit_bias`, `parallel_tool_calls`, `store`, `modalities`) vorher entfernen; `reasoning_effort` nur `none`/`high`. Quelle: https://docs.cohere.com/docs/compatibility-api · offiziell

## 2. Together AI
- **Gleichmaessigen Traffic** senden (z.B. 1 req/s bei 60 RPM) statt Bursts — die dynamischen Limits wachsen mit stetiger Nutzung und drosseln bei Spikes. Quelle: https://docs.together.ai/docs/rate-limits · offiziell
- **429 und 503 getrennt behandeln:** 429 = dein Limit ueberschritten, 503 = Modell ueberlastet (Request war innerhalb deines Limits). Beide mit **Exponential Backoff** wiederholen. Quelle: https://docs.together.ai/docs/rate-limits · offiziell
- Beim Drosseln den Header **`x-ratelimit-reset`** als empfohlenes Retry-Intervall auslesen statt sofort neu zu versuchen. Quelle: https://docs.together.ai/docs/rate-limits · offiziell
- Fuer feste Limits / strikte SLAs **dedizierte Endpoints** statt Serverless einsetzen. Quelle: https://docs.together.ai/docs/rate-limits · offiziell

## 3. Fireworks AI
- **Vollstaendige Pfad-Modell-IDs** verwenden (`accounts/fireworks/models/<name>`), nicht den Kurznamen. Quelle: https://docs.fireworks.ai/guides/querying-text-models · offiziell
- **Structured Output** ueber JSON-Schema fuer zuverlaessige Datenextraktion; Tool-Calling mit typsicheren Parametern fuer externe Tools/APIs. Quelle: https://docs.fireworks.ai/guides/querying-text-models · offiziell
- **Prompt-Caching** und **Streaming** aktivieren, um Latenz und Kosten zu senken; Token-Verbrauch steht im Response-Body. Quelle: https://docs.fireworks.ai/guides/querying-text-models · offiziell
- Fuer garantierte Kapazitaet/hoeheren Durchsatz **On-Demand-Deployments** statt Serverless (umgeht das ~6k-RPM-Ceiling). Quelle: https://docs.fireworks.ai/guides/querying-text-models · offiziell

## 4. Perplexity (`sonar`-Modelle)
- **Such-Parameter** (`search_domain_filter`, `search_recency_filter`, `search_mode`, `enable_search_classifier`) in Python ueber `extra_body`, in TS als direkte Felder setzen. Quelle: https://docs.perplexity.ai/docs/sonar/openai-compatibility · offiziell
- Die Perplexity-spezifischen Felder **`search_results`** (Titel/URL/Datum) und **`citations`** explizit auslesen — reine OpenAI-Parser ignorieren sie. Quelle: https://docs.perplexity.ai/docs/sonar/openai-compatibility · offiziell
- **`enable_search_classifier`** nutzen, damit das Modell selbst entscheidet, ob eine Websuche noetig ist (spart unnoetige Suchen bei reinen Faktenfragen). Quelle: https://docs.perplexity.ai/docs/sonar/openai-compatibility · offiziell
- `response_format` (JSON-Schema) funktioniert identisch zu OpenAI; `base_url="https://api.perplexity.ai"` setzen. Quelle: https://docs.perplexity.ai/docs/sonar/openai-compatibility · offiziell

## 5. AWS Bedrock (Claude/Llama/Nova/Titan)
- **Cross-Region Inference Profiles** verwenden, um Bursts und On-Demand-Quotas abzufedern; **Geographic** (Praefix `us.`/`eu.`/`apac.`) bei Data-Residency-Pflicht, **Global** fuer max. Durchsatz + ~10% Kostenersparnis. Quelle: https://docs.aws.amazon.com/bedrock/latest/userguide/cross-region-inference.html · offiziell
- Fuer Chat/Tools/Multi-Modell die **`Converse`-API** (modell-agnostisch, ein Code-Pfad) statt `InvokeModel` (modell-spezifisches JSON). Quelle: https://docs.aws.amazon.com/bedrock/latest/APIReference/API_runtime_Converse.html · offiziell
- **Auth ueber SigV4/IAM** (SDK boto3 signiert automatisch) — kein simpler Bearer-Key; bei `ThrottlingException` Backoff + Cross-Region-Profile. Quelle: https://docs.aws.amazon.com/bedrock/latest/userguide/cross-region-inference.html · offiziell
- SCP/Org-Policy passend setzen: Geographic erlaubt alle Ziel-Regionen im Profil, Global braucht `aws:RequestedRegion: unspecified`; Routing wird in CloudTrail (`inferenceRegion`) geloggt. Quelle: https://docs.aws.amazon.com/bedrock/latest/userguide/cross-region-inference.html · offiziell

## 6. Azure OpenAI / Azure AI Foundry
- Nicht den Modellnamen, sondern den **Deployment-Namen** im URL-Pfad/Param verwenden — erst ein Deployment anlegen, dann dessen Namen referenzieren. Quelle: https://learn.microsoft.com/azure/ai-services/openai/reference · offiziell
- Bei Legacy-Endpunkten die **`api-version`-Query** Pflicht und aktuell halten (veraltete Versionen verlieren Features); neue **v1-GA** unter `/openai/v1` braucht keine `api-version`. Quelle: https://learn.microsoft.com/azure/ai-services/openai/reference · offiziell
- Den **`AzureOpenAI`-Client** mit der ressourcen-spezifischen Base-URL (`https://<resource>.openai.azure.com/...`) nutzen; Auth per `api-key`-Header oder Entra-ID-Token. Quelle: https://learn.microsoft.com/azure/ai-services/openai/how-to/switching-endpoints · offiziell

## 7. Cerebras
- Base-URL **`https://api.cerebras.ai/v1`** mit OpenAI-Clients; nicht-standard Params (z.B. `clear_thinking`) ueber `extra_body` (OpenAI-Client) bzw. direkt (Cerebras-SDK). Quelle: https://inference-docs.cerebras.ai/resources/openai · offiziell
- **Modell-IDs nicht hardcoden** + Fallback vorhalten — aggressive Deprecations (z.B. Llama 3.1 8B / Qwen 3 235B Instruct am 27.05.2026). Quelle: https://tokenmix.ai/blog/cerebras-api-key-rate-limits-free-tier-2026 · community
- **Free-Tier nicht fuer Produktion** verlassen (8.192-Token-Context-Cap, 30 RPM, still gesenkte Limits bei Hochlast) — lange Prompts woanders verarbeiten oder Paid. Quelle: https://tokenmix.ai/blog/cerebras-api-key-rate-limits-free-tier-2026 · community
- Gleiche Prompts koennen sich von OpenAI unterscheiden (System/Developer-Rolle wirken anders) — das ist erwartet, Prompts ggf. anpassen. Quelle: https://inference-docs.cerebras.ai/resources/openai · offiziell

## 8. Google Vertex AI (Alternative zur Gemini-API)
- **Auth ueber ADC / Service-Account** (kein API-Key) via Client-Library — refresht das OAuth-Token automatisch (laeuft sonst nach 1 h ab → 401). Quelle: https://docs.cloud.google.com/vertex-ai/docs/authentication · offiziell
- Den OpenAI-kompatiblen Endpunkt mit exakter Base-URL setzen: `https://<LOCATION>-aiplatform.googleapis.com/v1beta1/projects/<PROJECT>/locations/<LOCATION>/endpoints/openapi`. Quelle: https://docs.cloud.google.com/vertex-ai/docs/authentication · offiziell
- Lokal **SA-Impersonation** via `gcloud auth application-default login --impersonate-service-account` statt eingebetteter Keys. Quelle: https://docs.cloud.google.com/vertex-ai/docs/authentication · offiziell

## 9. Hugging Face Inference Providers
- **Drop-in OpenAI-Endpunkt** `https://router.huggingface.co/v1` mit HF-Token als `api_key` — ein Token fuer 15+ Provider, server-seitiges Routing + automatisches Failover. Quelle: https://huggingface.co/docs/inference-providers/en/index · offiziell
- **Provider-Auswahl per Suffix** an der Modell-ID steuern: `:fastest` (Default), `:cheapest`, `:preferred` (eigene Reihenfolge) oder fester Provider (`...:sambanova`). Quelle: https://huggingface.co/docs/inference-providers/en/index · offiziell
- Fuer Nicht-Chat-Tasks (Text-to-Image, Embeddings, Speech) die **HF Inference-Clients** (Python/JS) nutzen — der OpenAI-Kompat-Endpunkt deckt nur Chat-Completions ab. Quelle: https://huggingface.co/docs/inference-providers/en/index · offiziell
- **Fine-grained HF-Token** mit `Make calls to Inference Providers`-Recht erstellen; keine separaten Provider-Keys noetig (kein Markup auf Provider-Preise). Quelle: https://huggingface.co/docs/inference-providers/en/index · offiziell

## 🔗 Bezug zum Bug-Almanach
| Best-Practice (Anbieter) | Bug-Abschnitt (`bugs/apis/other-llm-apis.md`) |
|---|---|
| 1 Cohere | 1. Cohere |
| 2 Together AI | 2. Together AI |
| 3 Fireworks AI | 3. Fireworks AI |
| 4 Perplexity | 4. Perplexity |
| 5 AWS Bedrock | 5. AWS Bedrock |
| 6 Azure OpenAI | 6. Azure OpenAI / Azure AI Foundry |
| 7 Cerebras | 7. Cerebras |
| 8 Vertex AI | 8. Google Vertex AI |
| 9 HF Inference Providers | 9. Optionale Aggregatoren (HF Inference Providers) |
