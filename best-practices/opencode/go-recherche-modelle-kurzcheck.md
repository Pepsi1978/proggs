# OpenCode-Go: Modellauswahl für die Firecrawl-Recherche-Pipeline Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Frage / Situation | Sofort-Regel |
|---|-------------------|--------------|
| 1 | ⭐ Welches Go-Modell wertet die gecrawlten Seiten aus? | **MiniMax M3 bevorzugt** (`minimax-m3`, **Anthropic-Schema**) — ehrlich bei Nichtwissen (Nicht-Hallu 83.9 %), GPQA 92.9, IFBench 82.9, günstigster. **DeepSeek V4 Pro** (`deepseek-v4-pro`, OpenAI-Schema) als Alternative — belegtes Retrieval (NIAH 97 %), aber Nicht-Hallu nur 6.0 % 🔴 (erfindet bei Nichtwissen → Abstain-Prompt Pflicht). A/B-Test klärt das Retrieval. |
| 2 | ⭐ Viele Seiten billig vorsieben? | **DeepSeek V4 Flash** (`deepseek-v4-flash`) als Vorfilter: $0.14/$0.28, 107 tok/s, ~158k Req/Mo. Dann V4 Pro für die kritische Synthese. |
| 3 | ⭐ Halluziniert V4 Pro? | Ja, **bei Nichtwissen** (AA-Omniscience 94 % „antwortet trotzdem"). **Abstain-Prompting Pflicht:** „nur aus den Quellen, bei Unsicherheit sagen, nicht erfinden" + Thinking-Modus an. |
| 4 | ⭐ Go-API-Endpunkt? | **Zwei Schemata!** DeepSeek/GLM/Kimi/MiMo = OpenAI (`…/zen/go/v1/chat/completions`); Qwen/MiniMax = Anthropic (`…/zen/go/v1/messages`). DeepSeek → OpenAI-Schema. |
| 5 | Zweitmeinung bei strittigen Fakten? | **Kimi K2.6** (höchster Intelligenz-Index 54, Vectara-Hallu 10.8 %) — aber nur 256K Kontext. Oder gegen **Opus** eskalieren. |
| 6 | Günstigste 1M-Long-Context-Alternative? | **MiniMax M3** ($0.30/$1.20, 1M MSA retrieval-treu) — Anthropic-Schema. Zahlen teils unverified. |
| 7 | NICHT für Massen-Auswertung nehmen | **GLM-5.x** (starkes Modell, aber Go-Tier nur ~4.300 Req/Mo + teuer → top als **Eskalation**, nicht für Masse). **Kimi K2.7-Code** (reines Coding-Modell). **MiMo non-Pro / Qwen3.6 / MiniMax M2.7** (Kontext/Leistung schwächer). |

**Empfohlene Pipeline:** `Firecrawl → V4 Flash (Vorfilter, Masse) → MiniMax M3 (bevorzugt) ODER V4 Pro (kritische Synthese, A/B) → Opus/Qwen3.7 Max/GLM-5.2 (nur Hard-Cases)`.
