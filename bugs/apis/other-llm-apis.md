# Bekannte Bugs: Weitere LLM-APIs (Survey + Integrations-Fallen)

> PFLICHT-LESEN vor Arbeit an einer Integration eines hier gelisteten Anbieters. Stand: zuletzt
> recherchiert am 2026-06-08. Survey der wichtigen Anbieter, die NICHT eine eigene Datei haben
> (OpenAI/Anthropic/Gemini/Groq/OpenRouter/Grok/Mistral/DeepSeek/lokal: siehe jeweils eigene Datei).
> Zweite Seite: `best-practices/projekt-code/apis/best-practices-other-llm-apis.md`.

## TL;DR — Auswahl & wichtigste Fallen

| Anbieter | Auth | OpenAI-kompat | Wichtigste Falle |
|---|---|---|---|
| **Cohere** | Bearer | Teilweise (`compatibility/v1`) | Viele Params abgelehnt; native v2-API für RAG/Citations |
| **Together AI** | Bearer | Ja | Dynamische Limits (429 + 503), Burst-Drosselung |
| **Fireworks** | Bearer | Ja | ~6k-RPM-Ceiling, pfadartige Modell-IDs |
| **Perplexity** | Bearer | Ja | `search_*` via `extra_body`; `citations`/`search_results`-Felder |
| **AWS Bedrock** | **SigV4/IAM** | Nein | Kein Key; Converse>InvokeModel; Geo-Präfix-Profile Pflicht |
| **Azure OpenAI** | api-key / Entra | Größtenteils | Deployment-Name + `api-version` Pflicht |
| **Cerebras** | Bearer | Ja | 8k-Context-Cap (free); Modell-Deprecation 27.05.2026 |
| **Vertex AI** | **ADC/SA (kein Key)** | Ja | OAuth-Token 1 h; spezielle openapi-Base-URL |
| **HF Inference Providers** | HF-Token | Ja (Router) | Provider-Routing + eigene Provider-Keys |

---

## 1. Cohere (Command, Rerank, Embed)
**Wann:** stark bei RAG (eigenes Rerank-API, gute Multilingual-Embeddings). Auth: Bearer. OpenAI-kompat: teilweise (`https://api.cohere.ai/compatibility/v1`).
- **Viele OpenAI-Params abgelehnt** (`n`, `logit_bias`, `top_logprobs`, `parallel_tool_calls`, `store`, `metadata`, `audio`). FIX: entfernen; Verhalten über native v2-API.
- **`reasoning_effort` nur `none`/`high`** — `medium`/`low` schlagen fehl. FIX: auf `high`/`none` mappen.
- **Native Features fehlen im Kompat-Layer** (`connectors`, `documents`, `citation_options`). FIX: für RAG/Citations native v2-API.
- **Doc-Format v1→v2 geändert** + abweichende Tokenisierung. FIX: Token-Budgets nicht 1:1 von OpenAI.
- Quelle: https://docs.cohere.com/docs/compatibility-api

## 2. Together AI
**Wann:** riesiger Open-Weight-Katalog, günstig, gutes Fallback. Auth: Bearer. OpenAI-kompat: Ja (Drop-in).
- **Dynamische Rate-Limits seit Jan 2026** — keine fixen Per-Modell-Limits; wachsen mit gleichmäßiger Nutzung, drosseln bei Bursts. FIX: ~1 req/s statt Burst, `x-ratelimit-reset` lesen.
- **Zwei Fehlercodes:** `429` (`dynamic_request_limited`/`dynamic_token_limited`) = dein Limit; `503` = Plattform-Überlast. FIX: beide mit Backoff.
- **Modell-Verfügbarkeit schwankt.** FIX: für SLA dedizierte Endpoints/Batch.
- Quelle: https://docs.together.ai/docs/rate-limits

## 3. Fireworks AI
**Wann:** sehr schnelle Inferenz für Open-Modelle, gutes Function-Calling. Auth: Bearer. OpenAI-kompat: Ja.
- **~6.000 RPM Spike-Arrest-Ceiling** (nicht garantiert). FIX: Backoff/dedizierte Deployments.
- **Modell-IDs pfadartig** (`accounts/fireworks/models/...`). FIX: vollständige Pfad-ID, nicht Kurzname.
- Quelle: https://fireworks.ai/blog/best-llm-api-providers

## 4. Perplexity (`sonar`-Modelle)
**Wann:** search-grounded Antworten mit Citations ohne eigenen RAG-Stack. Auth: Bearer (`https://api.perplexity.ai`). OpenAI-kompat: Ja.
- **Search-Parameter nur via `extra_body`** (`search_domain_filter`, `search_recency_filter`, `search_mode`, `return_images`). FIX: in Python `extra_body`, in TS direkte Felder.
- **Zusatz-Felder** `citations` + `search_results` (auch im Streaming). FIX: explizit auslesen; reine OpenAI-Parser ignorieren sie.
- **Domain-Filter wirkt nur bei aktivierter Suche** (`disable_search:true` → `search_*` greifen nicht).
- Quelle: https://docs.perplexity.ai/docs/sonar/openai-compatibility

## 5. AWS Bedrock (Claude/Llama/Nova/Titan)
**Wann:** Enterprise-Gateway, IAM/VPC/Compliance. Auth: **SigV4/IAM** (KEIN simpler Key!). OpenAI-kompat: Nein.
- **SigV4 statt Key** — naive `Bearer`-Calls scheitern. FIX: SDK (boto3) signiert; bei manuellem HTTP SigV4 korrekt.
- **`Converse` vs `InvokeModel`:** Converse = modell-agnostisch (ein Code-Pfad), InvokeModel = modell-spezifisches JSON. FIX: für Chat/Tools/Multi-Modell **Converse**.
- **Cross-Region Inference Profile Pflicht:** neuere Modelle (Claude) nur über Profile mit Geo-Präfix `us.`/`eu.`/`apac.`/`global`; nackte Modell-ID abgelehnt. FIX: Profile-ID mit Präfix.
- **Region/Verfügbarkeit variiert + `ThrottlingException`.** FIX: Cross-Region-Profile, Backoff, ggf. Provisioned Throughput.
- Quelle: https://docs.aws.amazon.com/bedrock/latest/APIReference/API_runtime_Converse.html · https://docs.aws.amazon.com/bedrock/latest/userguide/cross-region-inference.html

## 6. Azure OpenAI / Azure AI Foundry
**Wann:** OpenAI-Modelle mit Azure-Compliance/SLA/Region. Auth: `api-key`-Header oder Entra-ID-Token. OpenAI-kompat: größtenteils.
- **`deployment`-Name statt `model`** (im URL-Pfad/Param). FIX: Deployment anlegen, dessen Namen verwenden.
- **`api-version`-Query Pflicht** (Legacy); veraltete Versionen verlieren Features. FIX: aktuelle Version. Neue v1-GA: `/openai/v1`, keine api-version.
- **Andere Base-URL** (`https://<resource>.openai.azure.com/...`). FIX: AzureOpenAI-Client. (Siehe auch `openai-api.md` J.)

## 7. Cerebras
**Wann:** extrem hoher Token-Durchsatz, kleine Modellauswahl. Auth: Bearer. OpenAI-kompat: Ja.
- **8.192-Token-Context-Cap (Free)** + 30 RPM. FIX: Kontext klein oder Paid; lange Prompts woanders.
- **Aggressive Deprecation:** Llama 3.1 8B + Qwen 3 235B Instruct am 27.05.2026 abgekündigt. FIX: Modell-ID nicht hardcoden, Fallback vorhalten.
- **Free-Tier-Limits werden bei Hochlast still gesenkt.** FIX: nicht für Produktion auf Free verlassen.
- Quelle: https://tokenmix.ai/blog/cerebras-api-key-rate-limits-free-tier-2026

## 8. Google Vertex AI (Alternative zur Gemini-API)
**Wann:** Gemini + Modelle mit GCP-IAM/Region/VPC-SC. Auth: **ADC/Service-Account (kein Key)**. OpenAI-kompat: Ja (eigener Endpunkt).
- **OAuth-Token läuft nach 1 h ab** — naiver Key-Ansatz scheitert (401). FIX: ADC/Client-Library (refresht automatisch).
- **Spezielle Base-URL:** `https://<LOCATION>-aiplatform.googleapis.com/v1beta1/projects/<PROJECT>/locations/<LOCATION>/endpoints/openapi`. FIX: exakt setzen.
- **SA-Impersonation** lokal: `gcloud auth application-default login --impersonate-service-account`.
- Quelle: https://docs.cloud.google.com/vertex-ai/docs/authentication

## 9. Optionale Aggregatoren (kurz)
- **HF Inference Providers:** EIN OpenAI-kompatibler Router für 15+ Provider (Groq, Cerebras, SambaNova, Novita, Hyperbolic, Together, Fireworks, Cohere …). Auth: HF-Token (als `api_key`). Falle: Provider-Auswahl/Routing + eigene Provider-Keys; nicht jeder Provider kann jeden Task. Quelle: https://huggingface.co/docs/inference-providers/en/index
- **SambaNova / Cerebras:** sehr schnell, eigene Hardware, OpenAI-kompat, Bearer.
- **Novita / Hyperbolic / Nebius:** günstige Open-Weight-Serverless, OpenAI-kompat, Bearer — Kosten-Fallback.
- **Replicate:** eher Modell-Hosting (auch Bild/Video), **kein** Drop-in-Chat — eigenes `predictions`-API mit Polling (async, `Prefer: wait` für sync). Falle: Cold-Starts + async-Pattern.

---

## Fix-Status (Stand 2026-06-08)

| Anbieter | Datierter Stichtag |
|---|---|
| Cerebras: Llama 3.1 8B + Qwen 3 235B Instruct | abgekündigt **27.05.2026** |

Sonst per Design / Plattform-Verhalten. **Ehrlichkeits-Hinweis:** Azure-Details aus Vorwissen + Kontext-Treffern markiert, nicht in dieser Recherche frisch gefetcht (gut etabliert). Bedrock-Profile-Präfixe, Together-429/503, Cerebras-Deprecation, Perplexity-Felder sind frisch belegt.

---

## Pflicht-Checkliste vor Integration eines dieser Anbieter

- [ ] Auth-Typ korrekt? (Bedrock=SigV4, Vertex=ADC, Azure=api-key/Entra — KEIN simpler Bearer-Key!)
- [ ] OpenAI-kompat ja/nein geklärt; native Features (Cohere-RAG, Perplexity-citations, Bedrock-Converse) berücksichtigt?
- [ ] Modell-ID-Format korrekt (Fireworks-Pfad, Bedrock-Geo-Präfix-Profil) + nicht hardcoded?
- [ ] Rate-Limit-Eigenheit (Together-dynamisch, Cerebras-8k-free, Fireworks-6k) eingeplant?
- [ ] Token-Ablauf bei Vertex (1 h) über ADC/Library gelöst?

## 🔗 Bezug zu Best Practices

Zweite Seite der Medaille (wie man es richtig macht): `best-practices/projekt-code/apis/best-practices-other-llm-apis.md`. Die dortige Mapping-Tabelle „🔗 Bezug zum Bug-Almanach“ verlinkt jede Best-Practice zurueck auf die hier dokumentierten Bug-Abschnitte (bidirektional, ohne Duplikation).
