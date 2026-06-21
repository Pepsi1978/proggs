# OpenCode-Go: Modellauswahl für die Firecrawl-Recherche-Pipeline — Best Practices

> Quellen: `extern` (Modell-Reviews, Artificial Analysis, Vectara-Halluzinations-Leaderboard,
> Hersteller-Blogs) bzw. `offiziell` (opencode.ai/docs/go). **Benchmarks und Preise ändern sich
> häufig — vor produktivem Einsatz erneut prüfen.**
>
> **Quellen-Regel (Lehre 2026-06-20):** Bei Modell-Benchmarks sind die **neutralen OpenRouter/Artificial-Analysis-Werte
> maßgeblich** — Hersteller-/Blog-Indizes weichen nach oben ab (real erlebt: GLM-5.2 „Faktentreue unbelegt" war belegt;
> Qwen3.7-Max Hersteller-Index 56.6 vs. OpenRouter 46.0; DeepSeek V4 Pro Researcher-Index 52 vs. OpenRouter 44.3;
> Abweichung auch nach unten: MiMo-V2.5-Pro Non-Hallu 52 % vs. OpenRouter 75.5 %). Immer gegen OpenRouter/AA gegenchecken.
>
> **Stand:** recherchiert am **2026-06-20** mit 7 parallelen Researchern (Opus 1M) für das
> **OpenCode-Go-Abo** (14 Modelle, Stand Juni 2026). Anker: opencode-go=2026-06.
>
> **Aufgabenprofil, für das hier bewertet wird:** Firecrawl crawlt/scrapt viele Webseiten → ein
> günstiges, starkes Modell filtert daraus die **genau wichtigen Informationen** heraus UND
> **hinterfragt kritisch** (Faktentreue, Widersprüche/Fehler in Quellen erkennen, ehrlich über
> Quellenlücken sein). Einsatz: allgemeine Recherchen, **Bug-Almanach-Recherche**,
> **Best-Practices-Recherche**.
>
> Gegenstück (was schiefgeht): `bugs/opencode/opencode-cli.md` (Go-Endpunkt-Falle, Req-Limits).
> Verwandt: `best-practices/opencode/openrouter.md`, Memory `project_research_pipeline_and_openrouter_go`.

---

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

---

## 1. Was ist OpenCode Go (Stand Juni 2026)

- Abo des **OpenCode-Zen-Gateways**: **$5 erster Monat, dann $10/Monat**, 14 Modelle, Hosting US/EU/Singapur. `offiziell` (opencode.ai/docs/go)
- **Dollarbasierte Quoten über ALLE Modelle gemeinsam:** $12/5h, $30/Woche, $60/Monat. Die erreichbare
  **Request-Zahl variiert massiv je Modell**: DeepSeek V4 Flash ~158.000 Req/Mo, **GLM-5.1 nur ~4.300 Req/Mo**.
- **Auth:** API-Key aus dem OpenCode-Zen-Portal (in `~/SK/OpenCode/go-api-key.txt` abgelegt). Config-Referenz
  `opencode-go/<slug>`, Modell-Discovery `…/zen/go/v1/models`.
- **Zwei API-Schemata (Falle, siehe Almanach):**
  - **OpenAI-kompatibel** `…/zen/go/v1/chat/completions` → `glm-5.2`, `glm-5.1`, `kimi-k2.7-code`, `kimi-k2.6`,
    `mimo-v2.5`, `mimo-v2.5-pro`, `deepseek-v4-pro`, `deepseek-v4-flash`
  - **Anthropic** `…/zen/go/v1/messages` → `minimax-m3`, `minimax-m2.7`, `qwen3.7-max`, `qwen3.7-plus`, `qwen3.6-plus`

---

## 2. Bewertungs-Matrix (gewichtet auf „Docs filtern + kritisch hinterfragen")

Gewichtung: **Faktentreue/Anti-Halluzination (höchste)** > Long-Context-Retrieval > Reasoning > Instruction-Following > Preis/Verfügbarkeit.

| Rang | Modell | Faktentreue | Long-Context (viele Seiten) | Reasoning | Preis je Mio (in/out) | Eignung |
|------|--------|-------------|------------------------------|-----------|----------------------|---------|
| 1 | **MiniMax M3** | **AA-Omniscience Nicht-Hallu 83.9 % (erfindet bei Nichtwissen kaum — Top!)**; Eigenwissen-Acc nur 15 % (RAG-nachrangig) | 1M **MSA (retrieval-optimiert)**, **AA-LCR 74.0 %**; reines MRCR/Needle nicht direkt belegt | **GPQA 92.9 (höchstes!)**, **IFBench 82.9**, Index 44.4 | **$0.30/$1.20 (günstigster, Recherche-Profil ~$0.062)** | **Bevorzugter Standard** — gewinnt Ehrlichkeit + Reasoning + IFBench + Preis. Anthropic-Schema! |
| 2 | **DeepSeek V4 Pro** | **AA-Omn. Acc 43.3 % (bestes Eigenwissen)** / SimpleQA-Verified 57.9 — aber **AA-Omn. Nicht-Hallu nur 6.0 % 🔴: erfindet bei Nichtwissen fast immer** (Abstain-Prompt Pflicht) | **1M echt nutzbar**, **MRCR-1M 83.5 %, NIAH 97 % (belegt — sein Trumpf)**; AA-LCR nur 66.3 % | GPQA 88.8, IFBench 76.5, Index 44.3 (3 Think-Modi) | ~$0.44/$0.87 (Recherche-Profil ~$0.090) | **Starke Alternative** — Stärke: belegtes reines Retrieval + Eigenwissen |
| 3 | Qwen3.7 Plus | Familie stark (Max: Hallu 22.9 %), Einzelwert fehlt | **MRCR-128k 91.7 (bestes belegt)**, 1M Kontext | GPQA 90.3, Index ~52 | $0.40/$1.60 (verbose → Output-Kosten) | starke Alternative |
| 4 | Kimi K2.6 | SimpleQA 43 %, **Vectara-Hallu 10.8 %**, IFEval 89.8 | **nur 256K**, keine Retrieval-Benchmarks belegt | **Index 54 (höchster)**, GPQA 90.5, AIME 96.4 | $0.95/$4.00 | Zweitmeinung (Kontext-Limit!) |
| 5 | GLM-5.2 | AA-Omniscience Index **4** (Acc 25.1 %, Hallu 28.1 %) — mittel, **für RAG weniger kritisch** | AA-LCR **71.3 %** (Reasoning gut); reines Retrieval (MRCR/Needle) unbelegt | **stark: Index 51.1, GPQA 89.5, HLE 40.1, τ²-Telecom 99.1** | $1.20/$4.10, **~4.300 Req/Mo** | **Top-Modell** → exzellenter **Eskalations-/Zweitmeinungs-Kandidat**; NICHT für Masse (Go-Budget) |
| 6 | MiMo-V2.5-Pro | AA-Omn. Acc 22.6 %, **Nicht-Hallu 75.5 %** (ehrlich, ok) | 1M, **AA-LCR 73.3 %** (gut); degradiert >512K | GPQA **86.6 (niedrigstes)**, IFBench 79.9, **Index 42.2 (niedrigstes)**; ⚠ Thinking-Latenz (Minuten) | ~$0.44/$0.87 | solides Mittelfeld — **in nichts führend + langsam** → kein Vorzug vor MiniMax M3 |

**Vorfilter-Spezialist:** **DeepSeek V4 Flash** — $0.14/$0.28, **107 tok/s**, ~158k Req/Mo, 5× Concurrency. Zu schwach für die kritische Endsynthese, ideal zum Vorsieben der Masse.

**Ausgeschlossen:** Qwen3.6 Plus (vom 3.7-Nachfolger überholt, teurer), MiMo-V2.5 non-Pro (256K, 15B aktiv — zu schwach), **Kimi K2.7-Code** (reines Coding-Modell, keine Faktentreue-/Recherche-Nachweise, Thinking erzwungen), GLM-5.1 (nur 202K), MiniMax M2.7 (nur 205K, langsam).

---

## 3. Standard-Auswerter: MiniMax M3 bevorzugt, DeepSeek V4 Pro starke Alternative

Nach Auswertung **aller vier OpenRouter/Artificial-Analysis-Profile** (GLM-5.2, MiniMax M3, Qwen3.7 Max,
DeepSeek V4 Pro — Franks Screenshots 2026-06-20) ist das Bild klarer als gedacht: **MiniMax M3 gewinnt fast alle
aufgabenrelevanten Kategorien.** Der A/B-Test muss nur noch DeepSeeks einen Trumpf (reines Retrieval) prüfen.

**MiniMax M3 — bevorzugter Standard (gewinnt Ehrlichkeit + Reasoning + IFBench + Preis):**
- **AA-Omniscience Nicht-Halluzinationsrate 83.9 %** = erfindet bei Nichtwissen kaum etwas (von Natur aus „ehrlich
  über Quellenlücken" — genau das Kernkriterium, ohne dass man es erst erzwingen muss).
- **GPQA 92.9 (höchstes der Liga)**, **IFBench 82.9** (befolgt „nur aus den Quellen" sehr gut),
  **AA-LCR 74.0 %** (bestes Denken über langen Kontext), 1M **MSA** (retrieval-optimiert).
- **Günstigster:** ~$0.062/Recherche vs. ~$0.090 (DeepSeek) im Input-dominierten Recherche-Profil (~31 % billiger).
- ⚠ **Einziger offener Punkt:** reines MRCR/Needle-Retrieval nicht direkt mit Zahl belegt (nur AA-LCR + MSA-Architektur); Eigenwissen niedrig (15 %, RAG-nachrangig). **Anthropic-Schema** im Go.

**DeepSeek V4 Pro — starke Alternative (Trumpf: belegtes reines Retrieval):**
- **Belegtes reines Retrieval:** MRCR-1M 83.5 %, NIAH 97 % — findet verstreute Fakten im riesigen Kontext nachweislich (sein echter Vorteil; bei MiniMax unbelegt).
- **Bestes Eigenwissen:** AA-Omn. Accuracy 43.3 % / SimpleQA-Verified 57.9 (für RAG nachrangig).
- 🔴 **Gravierende Schwäche für genau diese Aufgabe:** AA-Omniscience Nicht-Hallu nur **6.0 %** — erfindet bei Nichtwissen FAST IMMER (94 %). **Abstain-Prompt Pflicht**, und selbst dann bleibt die Grundtendenz ein Risiko bei Quellenlücken. Außerdem niedrigstes AA-LCR (66.3) + GPQA (88.8) der vier.

**Fazit:** Für RAG (Fakten liegen in den Quellen) wiegen **Ehrlichkeit + Instruction-Following + Reasoning + Preis**
schwerer als Eigenwissen → **MiniMax M3 ist der Startfavorit.** DeepSeek V4 Pro nur dann vorziehen, wenn der A/B-Test
zeigt, dass MiniMax beim *Auffinden* verstreuter Fakten in sehr langen Crawls spürbar schlechter ist (DeepSeek NIAH 97 %).
**A/B-Test fahren, Gewinner als Standard festlegen.** Frühere OpenRouter-Tests (19./20.06.) hatten nur
deepseek-v4-pro geprüft (Opus-nahe Faktentreue, ~2 Cent/Recherche) — MiniMax M3 war damals nicht im Rennen.

---

## 4. Pflicht-Architektur der Recherche-Pipeline

```
Firecrawl (scrape/search, Gratis-Kontingent)
   │  viele rohe Seiten
   ▼
Stufe 1 — Vorfilter (Masse, billig):  DeepSeek V4 Flash
   │  grob relevante Auswahl
   ▼
Stufe 2 — kritische Synthese:         MiniMax M3 (bevorzugt)  ODER  DeepSeek V4 Pro   ← Arbeitspferd (A/B-Test); MiniMax = ehrlicher+klüger+günstiger, DeepSeek = belegtes reines Retrieval
   │  kurze, quellengestützte Antwort (+ explizite Quellenlücken)
   ▼
Stufe 3 — Eskalation (Hard-Cases): Opus / Qwen3.7 Max / GLM-5.2   ← starkes Reasoning, Request-Zahl egal
```

**Eskalations-/Zweitmeinungs-Modelle (wenige, schwierige Fälle — Go-Request-Limit dann egal):**
Hier zählt maximales Reasoning, nicht der Preis. Drei sehr starke Optionen:
- **Qwen3.7 Max** — starkes Reasoning (GPQA 92.3, IFBench 80.5, Intelligenz-Index **46.0** lt. OpenRouter), **bestes Eigenwissen** der chinesischen Modelle (AA-Omniscience Acc 30.1 %, Nicht-Hallu 77.1 %). $2.50/$7.50, **text-only, teuerstes Modell** → nur Eskalation, nie Masse. (Hinweis: Hersteller-Index „56.6" ist überhöht; OpenRouter/AA = 46.0.)
- **GLM-5.2** — Top-Reasoning (GPQA 89.5, Index 51.1, AA-LCR 71.3 %, τ²-Telecom 99.1 %), 1M Kontext. $1.20/$4.10; nur ~4.300 Req/Mo — für Eskalation reicht das locker. Faktentreue (AA-Omniscience Index 4) mittel, bei RAG aber nachrangig.
- **Opus** — für die subtilsten Korrektheits-Fälle.

**Abstain-Prompt-Baustein (Pflicht, gegen die Halluzination-bei-Nichtwissen):**
> „Beantworte AUSSCHLIESSLICH aus den bereitgestellten Quellen. Wenn die Information in den Quellen fehlt
> oder widersprüchlich ist, sage das ausdrücklich ('nicht in den Quellen' / 'Quellen widersprechen sich') —
> erfinde nichts. Denke vor der Antwort nach (Thinking)."

**Zweitmeinung** bei strittigen Fakten: dieselbe Frage zusätzlich an **Kimi K2.6** (höchster Intelligenz-Index,
niedrigste Vectara-Halluzination) — Abweichung = Signal, dann Opus.

---

## 5. Einrichtung in OpenCode (Go-Provider + MiniMax-M3-Thinking + Researcher-Agent)

**1) Provider aktivieren (einmalig):** OpenCode Go ist ein **eingebauter** Provider — in der TUI
`/connect` → „OpenCode Go" → Go-Abo-Key einfügen. Key landet in `~/.local/share/opencode/auth.json`
(`opencode-go`). **Kein** eigener `provider:{...}`-Block mit npm/baseURL/apiKey nötig.
⚠️ **NICHT** als Custom-`@ai-sdk/anthropic`-Provider mit eigener baseURL anlegen — bekannter Bug
verliert den Key zur Laufzeit (anomalyco/opencode #21737). Siehe Almanach §14.

**2) Thinking-Budget setzen (optional):** MiniMax M3 läuft im Go über das **Anthropic-Schema**
(`/zen/go/v1/messages`) und denkt dort **nativ** — Thinking ist automatisch an. Das Budget steuert man
per reinem **MERGE-Block** in `opencode.jsonc` (kein npm/baseURL/key → Key bleibt aus auth.json):
```jsonc
"provider": {
  "opencode-go": {
    "models": {
      "minimax-m3": {
        "options": { "thinking": { "type": "enabled", "budgetTokens": 16000 } }
      }
    }
  }
}
```
(Thinking-Keys stehen in `opencode.ai/docs/models`, **nicht** `docs/config`. OpenAI-Schema-Modelle
nutzen stattdessen `reasoningEffort` — für MiniMax **falsch**.)

**3) Als Researcher-Agent einbauen:** Markdown `~/.config/opencode/agents/researcher.md`
(Dateiname = Agentname `@researcher`), read-only, mit Abstain-Pflicht (passt zu M3s Ehrlichkeit):
```markdown
---
description: Web-Recherche mit MiniMax M3 (Thinking) + Firecrawl; quellentreu, erfindet nichts.
mode: subagent
model: opencode-go/minimax-m3
temperature: 0.2
permission:
  edit: deny
  bash: deny
---
... Arbeitsweise + "nur aus den Quellen, sonst sagen, nichts erfinden" ...
```
Aufruf: `@researcher <Frage>`. Thinking-Blöcke kommen als `type:"thinking"` zurück (TUI: `/thinking` toggelt die Anzeige). `permission` statt `tools` (letzteres deprecated); Firecrawl-MCP-Tools sind per Default erlaubt.

**4) Direkter Go-API-Aufruf — LIVE verifiziert 2026-06-20 (curl):** Man kann das Go-Gateway auch ausserhalb
von OpenCode direkt per HTTP ansprechen (z.B. aus einem Skript/Bash-Tool heraus, das ein laufender Agent aufruft).
- **baseURL:** `https://opencode.ai/zen/go/v1` · Discovery: `GET /models` (Bearer ODER x-api-key, beide OK).
- **MiniMax M3 = Anthropic-Schema** `POST /messages` — ⚠ **braucht `x-api-key: <go-key>`**; `Authorization: Bearer`
  wird hier mit „Missing API key" abgelehnt (anders als bei `/models`/`/chat/completions`). Header zusätzlich
  `anthropic-version: 2023-06-01`, `content-type: application/json`.
- **Thinking** über die API: `"thinking": {"type":"enabled","budget_tokens":N}` (Anthropic-Stil; max Thinking = hohes
  `budget_tokens`, `max_tokens` MUSS größer sein). Antwort enthält `content`-Blöcke `type:"thinking"` + `type:"text"`.
  Live-getestet: budget 24000 → ~2.800–13.600 Zeichen Thinking, sauber zurückgeliefert.
- **A/B-Befund 2026-06-21 (`:online` über OpenRouter):** MiniMax M3 denkt im `:online`-Modus OHNEHIN von
  sich aus (~900 reasoning-Token), egal ob `reasoning:{effort:high}` / `{max_tokens:N}` gesetzt ist oder GAR
  NICHT. Beide A/B-Läufe (gleicher Prompt, einmal max Thinking, einmal ohne) lieferten **gleichwertige
  Qualität** und fast identische reasoning-Token (921 vs. 899). **Konsequenz:** `reasoning:high` schadet
  nicht (bleibt konsistent mit der „A+B max Thinking"-Policy), ist bei `:online`/M3-Recherche aber **KEIN
  Qualitäts-Hebel** — anders als bei Vibe-Coding-Modellen (GLM/Kimi), wo Thinking-an der Hebel ist. M3 hat
  Recherche-Thinking fest eingebaut. (Werkzeug: `or-research.py … :online`; Policy: `research-strategy.md` §4.)
- **Alternativ OpenAI-Schema** `POST /chat/completions` (`Authorization: Bearer`): minimax-m3 geht auch hier,
  Thinking kommt nativ als `<think>…</think>` im `content`; `reasoning_split=true` (extra_body) trennt es in `reasoning_details`.
- Key zentral in `~/SK/OpenCode/go-api-key.txt`. Windows-Falle: Body per **stdin-Pipe** an `curl --data-binary @-`
  und Response per Bash-Redirect (kein `curl -o /tmp/...` — Git-Bash-`/tmp` ≠ native-curl-Pfad); Python-Parsing mit `os.path.expanduser`, NIE `/c/Users/...`.
- **Cloudflare-UA (Python-`urllib`):** Direkte `urllib`-Calls an dieses Gateway (oder OpenRouter) werden von Cloudflare
  mit 403/„error code 1010" geblockt, wenn der Default-UA (`Python-urllib/3.x`) gesendet wird → IMMER
  `User-Agent: curl/8.5.0` setzen (so machen es `mm-research.py`/`or-research.py` in `_post()`). `curl` selbst ist nicht betroffen. Almanach §14.8.

```bash
KEY=$(tr -d '[:space:]' < ~/SK/OpenCode/go-api-key.txt)
python3 -c 'import json,sys; sys.stdout.write(json.dumps({"model":"minimax-m3","max_tokens":30000,"thinking":{"type":"enabled","budget_tokens":24000},"messages":[{"role":"user","content":"<frage + ggf. firecrawl-quellen>"}]}))' \
| curl -s https://opencode.ai/zen/go/v1/messages -H "x-api-key: $KEY" -H "content-type: application/json" -H "anthropic-version: 2023-06-01" --data-binary @- > ~/out.json
```

⚠️ OpenCode-TUI-Bug #31569: in der TUI wird MiniMax-M3-Thinking aktuell evtl. NICHT angezeigt — der **API-Call liefert es trotzdem** (siehe Almanach §14).

---

## 6. Quellen (Stand 2026-06-20)

- DeepSeek V4: aistackchoice.com/deepseek-v4-review-2026, medium.com/@leucopsis/deepseek-v4-review, morphllm.com/deepseek-v4, artificialanalysis.ai/models/deepseek-v4-flash
- Qwen 3.7: codersera.com/blog/qwen-3-7-max-launch-guide-2026, digitalapplied.com/blog/qwen-3-7-plus-…, buildfastwithai.com/blogs/qwen-3-7-max-review-2026
- Kimi K2.6/K2.7-Code: artificialanalysis.ai/models/kimi-k2-6, deepinfra.com/blog/kimi-k2-6-model-overview, nerova.ai/news/moonshot-kimi-k2-7-code-release-june-2026
- GLM-5.x: huggingface.co/blog/zai-org/glm-52-blog, llm-stats.com/models/glm-5.1, openrouter.ai/z-ai/glm-5.2
- MiniMax M3/M2.7: artificialanalysis.ai/models/minimax-m3, marktechpost.com (MSA 2026-06-17), minimax.io/news/minimax-m27-en
- MiMo: artificialanalysis.ai/models/mimo-v2-5-pro, huggingface.co/XiaomiMiMo/MiMo-V2.5-Pro
- Halluzination/Long-Context: github.com/vectara/hallucination-leaderboard, digitalapplied.com/blog/ai-model-hallucination-rate-benchmarks-2026-study
- OpenCode Go: opencode.ai/docs/go, bitdoze.com/opencode-go-plan
