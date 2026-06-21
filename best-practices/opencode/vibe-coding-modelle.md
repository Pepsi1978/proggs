# Vibe-Coding mit OpenCode — Modelle, Thinking & Prompts (Best Practices)

> **Worum es geht:** Welche Modelle/Einstellungen in OpenCode fuer "Vibe-Coding" (Features
> beschreiben statt selbst tippen, z. B. Android-App-Aenderungen) am besten sind — und die
> Frage, ob High-Thinking-Modelle trotz schlechterer Benchmarks weniger Fehler machen.
>
> **Stand:** recherchiert **2026-06-21** (11 Researcher: 8× Firecrawl+MiniMax M3 "Engine A",
> 3× OpenRouter web_search "Engine B"). Quellen `extern` (Hersteller-Docs, arXiv, Fachblogs,
> OpenRouter/HuggingFace, Praxisberichte) — klar gelabelt. Rohergebnisse:
> `best-practices/opencode/vibe-coding-rohergebnisse-2026-06-21.md`. Modelle aendern sich
> monatlich → Namen nie hardcoden, Heuristik zaehlt. Schwester: `go-recherche-modelle.md`,
> `plugins-mcp-skills.md`.

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) |
|---|-----------|--------------------------|
| 1 | Modell macht beim Vibe-Coding viele Fehler | **Thinking/Reasoning AKTIVIEREN** — der grosse Sprung ist *aus → an* (Pass-Rate +~14 Punkte) |
| 2 | Welche Thinking-Stufe fuer Code-Editing? | **`medium`** ist der Sweet Spot. `high`/`xhigh` bringen beim Code-Editing fast nichts (+58 % Token, 0 Accuracy) und over-editen. (Mathe ≠ Code: bei Mathe lohnt `high`.) |
| 3 | GLM-5.2 "kann kein Thinking" | **Falsch** — GLM-5.2 hat Default + Max Effort. In `opencode.json` `reasoningEffort` explizit setzen (sonst laeuft es ohne) |
| 4 | Reasoning-Modell aendert zu viel (Over-Editing) | Im Prompt **explizit "minimale Aenderung, bestehenden Code/Signaturen erhalten"** → dann editiert es sogar weniger |
| 5 | Modellwahl per Benchmark | **Nicht verlassen** — Harness/Prompt verschiebt dasselbe Modell um 19 %→73 %; Benchmarks kontaminiert + manipulierbar; "offiziell top" ≠ Praxis |
| 6 | Guenstiger Reasoning-Default fuer Vibe-Coding | **MiMo-V2.5-Pro** (Xiaomi, $1/$3, 1M Kontext, #1 Coding-Volumen) oder **MiniMax M3** |
| 7 | Ultra-Long-Horizon-Coding (Stunden) | **GLM-5.2** (stark, aber token-hungrig ~42k) oder MiMo + "MiMo Code"-Harness |
| 8 | Wie viel Tooling/Harness? | **Wenig generische Tools > viele spezialisierte** (Vercel: 15→2 Tools = 80 %→100 %) — aber nur ueber einem Modell-"Capability Floor" |
| 9 | Prompt fuer wenig Fehler | Persona + praezise Spec (benannte Komponente + Signatur + State-Modell) + Plan/Act-Bestaetigung + schrittweise |
| 10 | Android-KI-Code reviewen | Checkliste: Coroutine-Scope (kein `GlobalScope`), kein `!!`, aktuelle APIs (StateFlow statt LiveData/AsyncTask), Recomposition (stable Lambdas) |

---

## 1. Der Thinking-Effekt: "an" ist der Hebel, nicht "hoch"

**Thinking aktivieren bringt beim Coding klar weniger Fehler** (Engine-A-Researcher 1):
- Pass-Rate ~75 %→82 %, Control-Flow-Fehler **halbiert**, Injection-Luecken 20 %→0 % (Sonar/GPT-5-Studie, 4.400 Java-Tasks). Reasoning-Modelle folgen einem "human-like workflow" (Requirements → Ambiguitaeten klaeren → Alternativen → implementieren → pruefen; arXiv 2509.13758).

**ABER mehr Thinking ist beim Code-Editing nicht besser** (Researcher 8 + Eskalation E2):
- Expert-SWE-Refactor (Digital Applied, 60 Multi-File-Tasks): GPT-5.5 Low 58,7 % → **Medium 73,1 %** → High 71,4 % (High *sinkt*). Der steile Anstieg ist **Low→Medium (+14 Punkte)**, Medium→High ~0 bis negativ.
- OckBench (arXiv 2511.05722): GPT-5.5 high→xhigh = Coding-Accuracy **100 %→100 %**, Token **+58 %**, OckScore **identisch**. **Medium hat den besten OckScore.**
- 23 % der High-Effort-Laeufe = "over-engineered refactors" (unnoetige Abstraktionen, gebrochene Signaturen). Zitat: *"Math reasoning is where the dial pays its rent. Code reasoning is where the dial is misused."*
- Anthropic empfiehlt fuer Opus 4.7 zwar `xhigh` als Coding-Default, warnt aber vor `max` (overthinking). → **Task-spezifisch:** high gewinnt Mathe, **medium gewinnt Code-Refactor**, low gewinnt PR-Review.

**Konsequenz:** Beim Vibe-Coding Thinking AN (sonst der grosse Verlust), aber `medium` als Default; `high`/`xhigh` nur fuer schwere Long-Horizon-Tasks.

## 2. Over-Editing: der wichtigste Prompt-Trick

Reasoning-Modelle **over-editen** (aendern mehr als noetig) im Generic-Setting (Minimal-Editing-Studie nrehiew.github.io: DeepSeek V3, GPT-5/5.4, Gemini 3.1, Qwen 3.6, Kimi 2.5 — alle). **Loesung:** explizit zur Minimalitaet anweisen ("preserve the original code", "minimale Aenderung, keine unbeteiligten Dateien anfassen") → dann editieren Reasoning-Modelle **weniger** als Non-Reasoning. Das ist der billigste Fehler-Reduzierer beim Vibe-Coding.

## 3. Modell-Empfehlungen (Stand 2026-06, Namen nie hardcoden)

| Modell | Profil | Einsatz |
|--------|--------|---------|
| **MiMo-V2.5-Pro** (Xiaomi, NICHT MiniMax!) | $1/$3, 1M Kontext, #1 OpenRouter-Coding-Volumen (21,7 %), eigener "MiMo Code"-Harness (+5 Punkte vs Claude Code), SWE-bench Verified 78,9–82 % | guenstiger Reasoning-Default fuer Vibe-Coding, lange autonome Tasks |
| **GLM-5.2** (Z.ai, MIT) | $2/$6, 1M Kontext, Default+Max Effort, FrontierSWE 74,4 %, Terminal-Bench 2.1 81–82,7, "way fewer mistakes + besseres Instruction-Following" als 5.1 — aber token-hungrig (~42k) | Ultra-Long-Horizon-Coding; **Reasoning explizit einschalten** |
| **MiniMax M3** | 1M Kontext, #2 OpenRouter-Coding-Volumen, MSA-Reasoning-Architektur | guenstige Alternative, gut fuer Tool-Use/Long-Horizon |
| **Claude Opus 4.x / GPT-5.x** | teuer, Spitzenqualitaet, #1 LMSYS Thinking | haerteste/heikelste One-Shot-Tasks |
| Kimi K2.6, DeepSeek V4 Pro/Flash, Qwen3-Coder | tool-stabil / 1M / self-host / guenstig | je nach Bedarf (Schwaerme, lokal, Kontext) |

> **Falle:** Benchmark-Ranking ≠ Praxis-Eignung. Beispiel: Gemini 3 Pro ist offiziell empfohlen,
> liefert laut User-Bericht aber "very subpar code and would often get stuck". MiMo verweigert
> live manchmal grundlos ("Sorry, can't work with that"). Immer selbst am eigenen Stack testen.
>
> **Praxis-Befund (Frank, 2026-06-21):** Eine **`Kimi K2.7 Code`**-Variante (Code-optimiert, aber
> OHNE aktiviertes Thinking) brachte beim Vibe-Coding deutlich **schlechtere** Ergebnisse als ein
> Thinking-Modell — exakt der "Thinking-an"-Hebel aus §1. Lehre: **"Code" im Namen heisst NICHT
> besser fuers Vibe-Coding.** Bei Kimi auf eine **Thinking-/Reasoning-Variante** achten (Moonshot
> fuehrt meist eine non-thinking Instruct/Code-Linie UND eine Thinking-Linie) bzw. ein Modell mit
> aktivierbarem Reasoning waehlen (MiMo-V2.5-Pro / GLM-5.2-mit-`reasoningEffort` / MiniMax M3).

## 4. OpenCode-Konfiguration (Reasoning einschalten)

Built-in Effort-Varianten: **Anthropic** `high` (Default)/`max`; **OpenAI** `none`/`minimal`/`low`/`medium`/`high`/`xhigh`; **Google** `low`/`high`. In `opencode.json` (bzw. `.jsonc`):

```json
// Anthropic (Claude) — Thinking-Budget
"anthropic": { "models": { "claude-sonnet-4-5": { "options": {
  "thinking": { "type": "enabled", "budgetTokens": 16000 } } } } }

// OpenAI (GPT) — Reasoning Effort
"openai": { "models": { "gpt-5": { "options": {
  "reasoningEffort": "medium", "textVerbosity": "low", "reasoningSummary": "auto" } } } }
```

Praxis-Muster: "Deep Thinker" = GPT-5 high + low verbosity; Multi-Model = Plan→GLM-5 (reasoning),
Build→MiniMax (guenstig). Fuer OpenRouter-Modelle (GLM/MiMo/DeepSeek): `reasoningEffort` bzw.
`reasoning.effort` setzen — **sonst laeuft das Modell ohne Thinking** (genau die "GLM ohne
Thinking schlechter"-Falle).

## 5. Prompt-Best-Practices fuer Vibe-Coding

1. **Persona/Rolle** setzen ("You're a senior Android/Kotlin engineer…").
2. **Praezise Spezifikation:** benannte Komponente + Signatur (`StatusIndicator(appState: AppState)`),
   State-Modell explizit (StateFlow/ViewModel), erwartetes Verhalten.
3. **Kontext "primen":** erst relevante Doku/SDK laden lassen, dann Code generieren.
4. **Plan vor Code (Plan/Act):** erst Plan bestaetigen, dann ausfuehren (vermeidet "Doom Loop").
5. **In kleine, testbare "UI-Bricks" zerlegen** statt die ganze App in einem Prompt.
6. **Minimalitaet erzwingen** (siehe §2) + **Verifikation** (Tests/Review) nach jedem Schritt.

## 6. Harness: wenig, aber durchdacht

- **Weniger generische Tools schlagen viele spezialisierte:** Vercel 15→2 Tools = 80 %→100 % Accuracy,
  -37 % Token (generische `bash`/`grep` matchen das Modell-Training; spezialisierte Tools sind
  Constraint-Points). Manus, Anthropic-Leitprinzip bestaetigen "start simple".
- **Aber Harness ist nicht optional:** Mehrheit der Agent-Failures = Orchestrierung, nicht Reasoning
  (UC Berkeley MAST, APEX). LangChain: +13,7 Punkte allein durch Harness bei fixem Modell.
- **Capability Floor:** Ueber einem Modell-Mindestniveau dominiert der Harness; darunter hilft kein
  Harness. Anti-Patterns: "Treadmill" (immer mehr Tools) und "Plateau" (nie aktualisiert).

## 7. Android-spezifisch (Kotlin/Compose/Gradle)

**Prompt:** UI-Baustein + benannte Composable + Signatur + State-Modell (StateFlow/ViewModel);
schrittweise; Doku primen. Tools: **Firebender** (Android-MCP-SDK, Android Studio), JetBrains Junie,
Claude Code. Benchmark: **Kotlin-bench V2** (testet auf echten Android-Studio-Projekten).

**Haeufige KI-Android-Fehler — Review-Checkliste:**
1. **Coroutine-Scope:** kein `GlobalScope`, Cancellation nicht vergessen (sonst Leaks/Akku).
2. **Null-Safety:** kein `!!`-Force-Unwrap (NPE-Gefahr).
3. **Veraltete APIs:** Modelle mit aelterem Cutoff nutzen `LiveData`/`AsyncTask` statt `StateFlow`/Coroutines.
4. **Recomposition:** unstable Lambdas/Parameter → unnoetige Recomposition ("janky UI").

## 8. Benchmarks richtig einordnen (Skepsis ist berechtigt)

- Benchmarks **kollabieren Modell+Harness+Umgebung in eine Zahl** → kein Signal aufs reine Modell.
- **Kontamination:** Frontier-Modelle kennen Gold-Patches auswendig → OpenAI hat SWE-bench-Reporting
  eingestellt. 59 % der Probleme haben fehlerhafte Tests; 87 % Bug-Fixes aus 5 Repos.
- **Manipulierbar:** 10 Zeilen `conftest.py` → "500/500 geloest" (Berkeley RDI, alle 8 Benchmarks exploitbar).
- **METR:** erfahrene Devs waren mit KI **19 % langsamer**. Vendor-Scores liegen 4–8 Punkte ueber
  unabhaengigen Reproduktionen. → Modellwahl am **eigenen Stack** testen, nicht per Benchmark-Ranking.

---

## Quellen (kompakt)

Engine-A-Researcher (Firecrawl+MiniMax M3): Sonar/GPT-5-Coding-Studie, arXiv 2509.13758
(Reasoning-Patterns), SWE-bench-Verified-Kritik (Epoch AI, METR, Veracode, CodeRabbit, Berkeley RDI),
Xiaomi/the-decoder/VentureBeat (MiMo), HuggingFace/Together (GLM), OpenCode-Doku + Cast-AI/DEV.to
(Config), Vercel/Manus/LangChain (Harness), Kilo Code / OpenRouter (Modell-Ranglisten).
Eskalation (OpenRouter web_search): OckBench (arXiv 2511.05722), Anthropic Effort-Docs + Opus-4.7-Launch,
nrehiew.github.io (Minimal-Editing), Digital Applied (Cost-vs-Quality), Firebender Kotlin-bench V2,
Medium/Reddit/Kotlin-Slack (Android-Pitfalls). Alle `extern`, Detail-Links in den Rohergebnissen.
