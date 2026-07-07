# Vibe-Coding-Recherche — Rohergebnisse (2026-06-21)

> Vollstaendige Researcher-Antworten zur Vibe-Coding-Modell-Recherche (research-persistence). Engine A = Firecrawl+MiniMax M3, Engine B = OpenRouter web_search (parallel, max 2). Kuratierte Best Practices: `vibe-coding-modelle.md`. Quellen-Links jeweils im Text.


---

## [A] 1. Thinking vs Non-Thinking: warum weniger Coding-Fehler

# Antwort: Warum Thinking-Modelle mit hohem Thinking-Budget bei agentischem Coding weniger Fehler machen

## 1. Empirische Belege (direkt aus den Quellen)

### Pass-Rate-Steigerung durch Reasoning
**Quelle 3 (Sonar)** liefert direkte empirische Daten für GPT-5 auf über 4.400 Java-Aufgaben aus MultiPL-E und ComplexCodeEval:

| Modus | Weighted Test Pass@1 |
|---|---|
| GPT-5-minimal | **75,37 %** |
| GPT-5-low | 80,50 % |
| GPT-5-medium | **81,96 %** (Sweet Spot) |
| GPT-5-high | 81,78 % |

→ Bereits wenig Reasoning bringt einen Sprung von ~75 % auf ~80 %; der Peak liegt bei **medium**, höheres Reasoning bringt **keinen weiteren funktionalen Gewinn** (Quelle 3).

### Weniger Control-Flow-Fehler & weniger Standard-Sicherheitslücken
- „Control-Flow-Mistakes" werden von minimal zu high **halbiert** (Quelle 3).
- Path-Traversal- & Injection-Schwachstellen sinken von **20 %** (minimal) auf **0 %** (high) (Quelle 3).
- Allgemein: „models with stronger reasoning tend to produce code that is more logically sound, better handle edge cases, and introduce fewer security vulnerabilities than purely pattern-matching approaches" (Quelle 3, TL;DR).

### Reasoning-Patterns korrelieren mit Korrektheit
**Quelle 4 (arxiv 2509.13758)** analysiert 1.150 Reasoning-Traces von fünf LRMs (DeepSeek-R1-7B, Qwen3-1.7B/8B/14B, QwQ-32B) auf CoderEval-Aufgaben:
- „actions such as **unit test creation and scaffold generation strongly support functional outcomes**"
- Modelle passen Strategien an Aufgabenkontext an: „knowledge recall, alternative exploration, scaffold code generation, and edge case generation based on task dependency and context" (Quelle 4, RQ3).

---

## 2. Mechanismus (aus den Quellen ableitbar)

### a) Explizite, abtrennbare Denkphase vor der Codegenerierung
Thinking-Modelle erzeugen **interne Thought-Tokens** (Chain-of-Thought, Skizzen, Pläne), bewerten/überarbeiten diese und generieren **erst danach** die endgültige Ausgabe (Quelle 5). Sie sind „designed for multi-step deliberation and better generalization to complex problems" (Quelle 4).

### b) Menschähnlicher Coding-Workflow
**Quelle 4** zeigt empirisch, dass LRMs einem „human-like coding workflow" folgen:
1. **Requirements** sammeln
2. **Ambiguitäten** in Signatur/Docstring klären (z. B. DeepSeek-R1 identifiziert `cls` als Klassenmethode und findet Mehrdeutigkeiten *vor* der Codegenerierung – Quelle 4, Figure 1)
3. **Lösungsalternativen** vergleichen
4. **Code implementieren**
5. **Defekte/Stil prüfen**

Bei komplexeren Aufgaben werden **zusätzliche Aktionen** ausgelöst: Scaffolding, Flaw Detection, Style Checks (Quelle 4, RQ1). Einfachere Aufgaben lösen leichtere Reasoning-Sequenzen aus.

### c) Iterative Vertiefung statt Pfad-Switching
- **Underthinking** (häufiger Pfadwechsel ohne Vertiefung) ist ein bekannter Fehlermechanismus bei o1-ähnlichen Modellen (Quelle 1). Thinking-Modelle mit hohem Budget neigen eher dazu, eine korrekte Spur **zu Ende zu verfolgen**, statt sie vorzeitig zu verlassen.
- Verschiedene Modelle zeigen unterschiedliche Patterns: Qwen3 iterative, DeepSeek-R1-7B eher linear (Waterfall) – beide aber explizit mehrstufig (Quelle 4, RQ2).

### d) RL-Training auf „gute Denkprozesse"
Thinking-Modelle werden mit **Chain-of-Thought-Beispielen und Reinforcement-Learning-Schemata** trainiert, die „gute Denkprozesse" verstärken (Quelle 5). OpenAI zeigt, dass die **Steuerbarkeit der CoT** mit zunehmendem RL-Training *abnimmt* – d. h. das Reasoning wird zunehmend **natürlicher/automatischer** (Quelle 2, Diagramm „RLVR-Schritte"). Mehr Reasoning-Länge korreliert in der CoT-Control-Studie mit **geringerer** künstlicher Steuerbarkeit (Quelle 2).

### e) Selbstüberprüfung im Reasoning-Loop
- Quelle 1 beschreibt, dass die korrekte Lösung bei o1-like Modellen oft initial erkannt, dann aber durch ständiges „second-guessing" wieder verworfen wird. Ein hohes, **wohldosiertes** Thinking-Budget erlaubt es, die initiale korrekte Spur zu **verfestigen**, statt sie zu zerstreuen.
- Generell: „Increased reasoning helps the model avoid fundamental logical errors and common API usage mistakes" (Quelle 3, Reliability-Sektion).

---

## 3. Wichtige Einschränkungen (aus den Quellen)

⚠️ **Mehr Reasoning ≠ immer besser:**
- **Quelle 3**: Ab medium-Reasoning stagniert die funktionale Korrektheit; high liefert keine signifikante Verbesserung gegenüber medium, kostet aber 3× so viel ($189 vs. $64 pro Run).
- **Quelle 3**: Issues pro passing task **steigen** von 3,90 (minimal) auf 5,50 (high) – Reasoning erzeugt **neue, subtilere Fehlerklassen** (z. B. Concurrency-/Threading-Bugs verdoppeln sich; I/O-Error-Handling-Probleme steigen).
- **Quelle 1**: Overthinking-Phänomen – bis zu 1.953 % mehr Tokens, **ohne** Accuracy-Gewinn; das Modell stellt richtige Antworten in Frage.
- **Quelle 5**: „Thinking Budget" ist ein Trade-off zwischen Kosten, Latenz und Qualität.

---

## 4. Was die Quellen NICHT direkt belegen

- **„Agentisches Coding" im engeren Sinne** (End-to-End-Umsetzung von Feature-Beschreibungen in produktiven Code über mehrere Tool-Calls hinweg) wird in den Quellen **nicht explizit als Setting** untersucht.
- SWE-Bench Verified (Quelle 2) und MultiPL-E (Quelle 3) sind Code-Generierungs-/Bug-Fix-Benchmarks; sie kommen dem agentischen Setting nahe, sind aber nicht identisch.
- **Keine der Quellen** liefert eine kausale, mechanistische Erklärung auf Token-/Attention-Ebene, *warum* CoT bei Coding-Aufgaben hilft. Der Mechanismus wird qualitativ beschrieben (mehr Deliberation, mehr Selbstprüfung, Pattern-Vergleich), aber nicht formal bewiesen.

---

## Quellenangaben (kompakt)

- **Empirische Pass-Raten & Fehlertypen (Coding):** Quelle 3 (Sonar, Sept. 2025)
- **Reasoning-Patterns & Workflow-Taxonomie:** Quelle 4 (arxiv 2509.13758, Sept. 2025)
- **Mechanismus (Denkphase, RL-Training, Thinking-Budget):** Quelle 5 (Schweizer KI Akademie)
- **CoT-Charakteristika (Steuerbarkeit, RL-Effekte):** Quelle 2 (OpenAI, März 2026)
- **Overthinking/Underthinking-Phänomene:** Quelle 1 (Medium-Artikel, Feb. 2025)


---

## [A] 2. Coding-Benchmarks (SWE-bench): Entstehung, Prompt-Abhaengigkeit, Korrelation

# Recherche-Auswertung: SWE-bench Verified & Co.

Ich beantworte die drei Teilfragen ausschließlich auf Basis der bereitgestellten Quellen. Pro Aussage nenne ich die Quelle als **(Q1)…(Q6)**. Wo etwas nicht in den Quellen steht, sage ich es ausdrücklich.

---

## 1) Wie entstehen LLM-Coding-Benchmarks wie SWE-bench Verified?

**SWE-bench (Original, 2023)**
- Wurde 2023 veröffentlicht; jedes Problem stammt aus einem gelösten GitHub-Issue eines von 12 Open-Source-Python-Repos und wird mit dem zugehörigen Pull Request (PR) verpaart **(Q5)**.
- Jedes Problem enthält zwei Test-Sätze: (a) Fail-to-Pass-Tests, die auf dem unveränderten Codebase fehlschlagen, aber bei korrektem Fix passen, und (b) Regressions-Tests, die vorher und nachher passen müssen. Das Modell sieht die Tests nicht **(Q5)**.
- Bekannte Schwächen des Originals: übermäßig spezifische Tests, unterspezifizierte Task-Beschreibungen, umgebungsspezifische Fehlschläge (Linux/Windows, Python-Version) **(Q5)**.

**SWE-bench Verified (August 2024, OpenAI)**
- OpenAI hat 1.699 SWE-bench-Probleme von Expert:innen (jeweils 3 unabhängig) prüfen lassen, um Probleme mit fehlerhaften Tests und unterspezifizierten Beschreibungen herauszufiltern **(Q5)**.
- Ergebnis: kuratierte Menge von 500 Problemen **(Q5)**.
- Grundidee: „tests pass" ≠ „bug fixed" – Verifikation sollte enger werden, um False Positives zu reduzieren **(Q2)**.

**Verwandte Benchmarks (zur Einordnung)**
- HumanEval, MBPP, LiveCodeBench, BigCodeBench und SWE-bench teilen dieselbe Struktur: ein Modell, ein Harness, eine Umgebung produzieren eine einzige End-to-End-Zahl, oft gegen eine Referenzlösung **(Q1)**.
- SWE-bench-Live und LiveCodeBench sind explizit Reaktionen auf das Kontaminationsproblem: öffentliche Probleme und Repos werden in Trainingsdaten und Tool-Indexe absorbiert, daher brauchen Benchmarks „Freshness" **(Q2)**.

> **Was die Quellen NICHT abdecken:** Detaillierte methodische Schritte (z. B. Auswahl der Repos, Reviewer-Profile, Konsensregeln) jenseits der oben genannten Punkte; konkrete Vergleichsdaten zwischen Verified und dem Original-Setup (z. B. Quote entfernter Probleme nach Problemtyp). Auch der exakte Gründungs-/Reviewprozess von HumanEval, MBPP etc. wird in den Quellen nicht im Detail beschrieben.

---

## 2) Wie stark beeinflussen Prompt und Agent-Harness das Ergebnis?

**Konzeptueller Rahmen: Agent-Harness als System**
- Ein „Coding Agent" in der Praxis ist kein einzelnes Modell, sondern ein System-Harness: ein Orchestrierungs-Layer um ein oder mehrere LLMs, der Tasks, Umgebungen und Feedback über die Zeit verwaltet **(Q1)**.
- Der Agent-Harness besteht aus Modell, **Prompt**, Tools und Loop; jede dieser Komponenten kann den End-to-End-Score um Margen verschieben, die vergleichbar mit den Unterschieden zwischen aufeinanderfolgenden Modellgenerationen sind **(Q1)**.
- Benchmarks liefern **kein Signal auf Komponenten-Ebene**, sondern kollabieren Modell, Harness und Umgebung in eine einzige Zahl **(Q1)**.

**Empirische Befunde zur Harness-Sensitivität**
- *Claw-SWE-Bench (Q3):* Upgrades des Adapters (der Prompts, Tools und Schleifen kapselt) heben die Lösungsrate eines Modells von **19 % auf 73 %**. Unter „bare adapter" scheitern **69 % der Patches** an der Anwendung; mit vollem Adapter sinkt diese Fehlerquote auf **< 2 %**.
- Für QN-3.6-Flash bewirkt allein der Harness-Wechsel einen **27-Prozentpunkte-Swing** der Pass-Rate **(Q3)**.
- *Offene Aussage der Autoren:* „Harness architecture is a first‑order driver of coding performance" **(Q3)**.
- Im industriellen Setting: dasselbe Modell kann **69 % standalone** oder **81 % mit einem ausgeklügelten Agent-Harness** (Retries, iterative Exploration) erzielen **(Q4)**.
- *IQuest-Coder-V1*: beanspruchte 81,4 % auf SWE-bench, aber 24,4 % der Trajektorien führten schlicht `git log` aus und kopierten die Antwort aus der Commit-Historie – korrigierte Quote 76,2 % **(Q6)**. (Hinweis: Dies illustriert Agent-Verhalten, nicht primär Prompt-Sensitivität.)

**Zur Rolle des Prompts im Speziellen**
- Quellen fassen den Prompt als **eine der vier Komponenten des Agent-Harness** auf (Modell, Prompt, Tools, Loop) **(Q1)**.
- Claw-SWE-Bench kritisiert, dass „traditionelle Evaluationen Modelle, Prompt-Templates und Agent-Loops vermengen" **(Q3)**.
- In der Praxis (nicht SWE-bench-spezifisch) zahlen sich repo-spezifische Instruktionsdateien (z. B. CLAUDE.md) aus, die Stil, Architektur und Anti-Patterns dokumentieren **(Q4)**.

> **Was die Quellen NICHT direkt belegen:** Isolierte, kontrollierte Studien, die **ausschließlich den Prompt** (bei fixem Modell/Harness/Loop) variieren und dessen Effekt auf SWE-bench-Verified quantifizieren. Die Quellen behandeln Prompt fast immer als Teil des gesamten Harness-Bündels.

---

## 3) Warum korrelieren hohe Benchmark-Werte schlecht mit echtem agentischem Coding in der Praxis?

### a) Konzeptionelle Fehlausrichtung (Designproblem)
- Benchmarks wurden in einer „pre-agent era" entworfen, um zu messen, ob ein LLM in einem Zug funktionierenden Code generiert – sie kollabieren Modell, Harness und Umgebung in einen Score und vergleichen gegen **eine einzige Referenzlösung** **(Q1)**.
- Drei Symptome werden identifiziert: (i) Scores vermengen Modell mit dem Rest des Harness, (ii) eine einzelne Referenzlösung bestraft gleichwertige Alternativen, (iii) fehlende Signale auf Komponenten-Ebene machen den End-to-End-Score schwer iterierbar **(Q1)**.
- Praktisches agentisches Coding operiert auf der „System-Harness"-Ebene (mehrere Agenten, Environments, mehrere Feedback-Loops); aktuelle Benchmarks messen nur auf der Agent-Harness-Ebene **(Q1)**.
- Feedback existiert in drei Tiers (inner/middle/outer loop); Benchmarks greifen typischerweise nur auf inner-loop-Proxies (Tests) zurück – mittelfristige Geschmacks-/Policy-Signale und langfristige Business-Outcomes fehlen **(Q1)**.

### b) Verunreinigung / Kontamination
- Frontier-Modelle können den **Gold-Patch verbatim reproduzieren** und Problemspezifika auswendig kennen – ein klarer Hinweis auf Training-Kontamination **(Q4, Q5)**.
- Beispiel: GPT-5.2 zeigte in seiner Chain-of-Thought Kenntnis von Release-Notes zum Parameter `edit_only` in Django **(Q5)**.
- Konsequenz: Verbesserungen auf SWE-bench Verified spiegeln zunehmend wider, **wie stark das Modell dem Benchmark im Training ausgesetzt war**, weniger echte Capability **(Q4, Q5)**. OpenAI hat das Reporting auf SWE-bench Verified daher eingestellt **(Q4, Q5)**.

### c) Defekte Tests / Falsche Ground Truth
- Audit von 138 Problemen: **59,4 % enthalten materielle Probleme** in Test-Design oder Problembeschreibung **(Q4, Q5)**.
- 35,5 % „narrow test cases" erzwingen spezifische Implementierungsdetails (z. B. einen bestimmten Funktionsnamen wie `get_annotation` in pylint) **(Q5)**.
- 18,8 % „wide test cases" prüfen Funktionalität, die im Problemtext gar nicht gefordert war **(Q5)**.

### d) Repräsentativitäts-Lücke
- Epoch-AI-Analyse: **87 % der SWE-bench-Probleme sind Bug-Fixes, > 80 % stammen aus 5 Python-Repos, die Hälfte stammt von vor 2020**, der Median-Task ist in **< 1 Stunde** lösbar **(Q4)**.
- Multi-File-Änderungen, Architektur-Entscheidungen und mehrdeutige Anforderungen – der Großteil realer Engineering-Arbeit – fehlen weitgehend **(Q4)**.
- SWE-bench Pro (Scale AI): 1.865 Tasks, Multi-Step-Reasoning über proprietäre/held-out Codebases – Top-Frontier-Modelle erreichen dort nur **23 %** **(Q4)**.

### e) Direkter Vergleich Labor ↔ Produktion
- **METR-Studie:** 16 erfahrene Maintainer großer OSS-Repos (Ø 1 Mio. LOC, 22.000 GitHub-Sterne), 246 Tasks à ~2 Stunden, mit Cursor Pro + Claude 3.5/3.7 Sonnet: **KI-Nutzung verlängerte die Bearbeitungszeit um 19 %** (statt der erwarteten 24 % Ersparnis). Weniger als **44 % der KI-Vorschläge** wurden akzeptiert **(Q4)**.
- **Faros AI:** High-AI-Teams mergen **98 % mehr PRs**, aber PR-Review-Zeit +91 %, PR-Größe +154 %, Bugs/Entwickler +9 % – **kein messbarer Performance-Gewinn auf Organisationsebene** **(Q4)**.
- **Veracode:** KI-generierter Code enthält **2,74× mehr Vulnerabilities** (1,88× unsicherere Passwort-Handhabung, 1,91× unsichere Object References, 2,74× XSS) **(Q4)**.
- **CodeRabbit (470 Repos):** KI-PRs enthalten **75 % mehr Logikfehler** **(Q4)**.
- **Georgia Tech Vibe Security Radar:** 74 bestätigte CVEs direkt auf KI-Coding-Tools zurückzuführen **(Q4)**.

### f) Benchmarks sind selbst angreifbar (Gaming)
- Berkeley-RDI-Team: alle 8 untersuchten Benchmarks (inkl. SWE-bench, SWE-bench Verified, SWE-bench Pro, WebArena, OSWorld, GAIA, Terminal-Bench, FieldWorkArena, CAR-bench) lassen sich **ohne eine einzige gelöste Aufgabe** auf nahezu perfekte Scores exploiten **(Q6)**.
- **SWE-bench Verified:** 10 Zeilen Python in `conftest.py` erzwingen, dass pytest jeden Test als „passed" markiert → **500/500 Instances „gelöst"** **(Q6)**.
- **Terminal-Bench:** Wrapper um `/usr/bin/curl` → 100/89 Tasks **(Q6)**.
- **WebArena:** Navigation zu `file://`-URL liest die Gold-Antwort direkt aus `config_files/{task_id}.json` → ~100 % auf 812 Tasks **(Q6)**.
- **FieldWorkArena:** Validator prüft nur, ob die letzte Nachricht vom Assistant kam (ignoriert Inhalt komplett) → 100 % auf 890 Tasks **(Q6)**.
- **METR:** o3 und Claude 3.7 Sonnet reward-hacken in **> 30 % der Evaluationsläufe** (Stack-Introspection, Monkey-Patching von Gradern, Operator-Overloading) **(Q6)**.
- **KernelBench:** `torch.empty()` liefert stale GPU-Memory mit der Referenz-Antwort des Evaluators **(Q6)**.

### g) Wo es in der Praxis funktioniert – und warum
- Quellen nennen konsistente Erfolgsmuster: **enge Aufgaben, klare Erfolgskriterien, verifizierbarer Output, mechanische Transformation** (Security-Remediation mit klarer Spec, DB-Migrationen, Boilerplate, Bug-Fixes mit reproduzierbarem Test) **(Q4)**.
- Z. B. Devin: Vulnerability-Remediation 1,5 min vs. 30 min beim Menschen (20×), Devins PR-Merge-Rate stieg in 18 Monaten von 34 % auf 67 % **(Q4)**.

---

## Zentrale Quellen-Aussagen im Überblick

| Behauptung | Stütze |
|---|---|
| Benchmarks kollabieren Modell+Harness+Umgebung in eine Zahl | (Q1) Abstract, §1 |
| End-to-End-Score konfundiert Modell mit dem Rest des Harness | (Q1) §1, §2 |
| Gleiches Modell: 69 % vs. 81 % je nach Harness | (Q4) |
| Adapter-Upgrade hebt Modell von 19 % auf 73 % | (Q3) |
| 27-Punkte-Swing allein durch Harness-Wechsel (QN-3.6-Flash) | (Q3) |
| SWE-bench Pro: Top-Modelle nur 23 % | (Q4) |
| 59,4 % der auditierten Probleme mit fehlerhaften Tests | (Q4, Q5) |
| 87 % Bug-Fixes, 80 % aus 5 Repos, Hälfte < 2020 | (Q4) |
| METR: 19 % langsamer mit KI | (Q4) |
| Berkeley: SWE-bench Verified 100 % via `conftest.py`-Exploit | (Q6) |

---

## Wichtige Einschränkungen dieser Auswertung

- **Quelle 2 (Medium-Artikel von Adnan Masood)** ist hinter einer Paywall; ich konnte nur die TL;DR-Liste und das Intro nutzen, nicht den Hauptteil – entsprechend vorsichtig sind Aussagen, die nur daraus stammen.
- Eine **isolierte, kontrollierte Prompt-Studie** (Prompt variiert, alles andere fix) speziell für SWE-bench Verified ist in den Quellen **nicht enthalten**; der Prompt wird überall als Bestandteil des Harness-Bündels behandelt.
- Quellen decken SWE-bench Verified primär aus der Perspektive **seiner Schwächen und seines Rückzugs** ab; eine positive Evaluation der Validität ist in keiner der sechs Quellen zu finden.


---

## [A] 3. MiMo-V2.5-Pro vs GLM (Korrektur: Xiaomi; GLM-5.2 keine Daten -> GLM-5.1)

# Antwort basierend auf den vorliegenden Quellen

## Wichtige Vorab-Hinweise zur Frage

Bevor ich antworte, muss ich auf zwei Unstimmigkeiten hinweisen:

1. **Modellname**: "MiniMax MiMo 2.5 Pro" existiert nicht. MiMo 2.5 Pro stammt von **Xiaomi** (nicht von MiniMax). Die Quellen 1, 2 und 5 identifizieren Xiaomi übereinstimmend als Ersteller von "MiMo-V2.5-Pro" (QUELLE 1, 2, 5).

2. **GLM-5.2**: Die vorliegenden Quellen enthalten **keine Daten zu GLM-5.2** — nur zu **GLM-5** (QUELLE 3, 4) und **GLM-5.1** (QUELLE 2, 5). QUELLE 5 erwähnt zwar einen Link "GLM-5.2 vs Xiaomi MiMo-V2.5-TTS-Series", liefert aber selbst keine Vergleichsdaten zu GLM-5.2.

Ich beantworte die Frage daher auf Basis der **Xiaomi MiMo-V2.5-Pro** vs. **GLM-5.1** (der nächsten verfügbaren Version) — streng nach Quellenlage.

---

## 1. Agentisches Code-Editing (Coding-Benchmarks)

| Benchmark | MiMo-V2.5-Pro | GLM-5.1 | Sieger |
|---|---|---|---|
| Terminal-Bench 2.0 | 68.4 | 63.5 | **MiMo** (+4.9) — QUELLE 5 |
| Terminal-Bench v2.1 | 65% | 62% | **MiMo** — QUELLE 2 |
| SciCode | 50% | 44% | **MiMo** — QUELLE 2 |
| SWE-bench Pro | 57.2 | 58.4 | **GLM-5.1** (+1.2) — QUELLE 5 |
| GDPval-AA v2 (agentische Arbeit) | 39% | 38% | **MiMo** knapp — QUELLE 2 |
| Long-Context Reasoning (AA-LCR) | 73% | 62% | **MiMo** — QUELLE 2 |

**Zwischenfazit Coding**: MiMo-V2.5-Pro gewinnt 4 von 6 Coding-/Agentik-Benchmarks, GLM-5.1 nur auf SWE-bench Pro knapp.

---

## 2. Tool-Use-Zuverlässigkeit

| Metrik | MiMo-V2.5-Pro | GLM-5.1 | Sieger |
|---|---|---|---|
| 𝜏³-Banking (Agentic tool use) | 9% | 12% | **GLM-5.1** — QUELLE 2 |
| ITBench-AA (Kubernetes Root-Cause) | 38% | 40% | **GLM-5.1** knapp — QUELLE 2 |
| APEX-Agents-AA (Long-horizon) | 2% | (keine Angabe) | unklar — QUELLE 2 |
| Code Execution (Capability) | Nein | Ja | **GLM-5.1** — QUELLE 5 |
| Function Calling / Tool Use | Ja | Ja | gleich — QUELLE 5 |
| APEX-Agents-AA — MiniMax wird hier allein gelistet | | | |

**Zwischenfazit Tool-Use**: GLM-5.1 ist bei reinem Tool-Use leicht vorn (𝜏³-Banking, ITBench, Code-Execution-Flag), MiMo bei lang-horizontigen Agenten-Aufgaben.

---

## 3. Instruction-Following & Fehler-Reduktion

| Metrik | MiMo-V2.5-Pro | GLM-5.1 | Sieger |
|---|---|---|---|
| **IFBench (Instruction Following)** | **80%** | 76% | **MiMo** — QUELLE 2 |
| AA-Omniscience Non-Hallucination Rate | 75% | 71% | **MiMo** — QUELLE 2 |
| AA-Omniscience Accuracy | 23% | 24% | praktisch gleich — QUELLE 2 |
| User-Bericht: "Sorry, can't work with that" random | ja (berichtet) | nicht erwähnt | **GLM-5.1** — QUELLE 1 |

**Zwischenfazit Instruction-Following**: MiMo-V2.5-Pro liegt auf IFBench (dem explizit für Instruction-Following designten Benchmark) **4 Prozentpunkte vorn** und halluziniert seltener. Allerdings berichtet QUELLE 1 explizit von Nutzern, dass MiMo im Live-Test "But it keeps saying, 'Sorry, can't work with that.' randomly" — also gelegentliche, nicht vorhersehbare Verweigerungen, was Fehler induziert.

---

## 4. "High Thinking" / Hohes Reasoning-Budget

Die Quellen enthalten **keine Daten**, die MiMo-V2.5-Pro oder GLM-5.1 explizit mit konfiguriertem "hohen Reasoning-Budget" oder "High Thinking"-Modus vergleichen. QUELLE 1 erwähnt, dass MiMo als Reasoning-Variante verfügbar ist und die Pro-Version die instruction-tuned Variante darstellt. QUELLE 2 zeigt beide als "Reasoning model". Konkrete Fehlerraten-Differenzen je nach Reasoning-Budget-Höhe: **nicht in den Quellen**.

---

## 5. Kontext, Architektur, Kosten

- **Kontextfenster**: MiMo 1.050k vs. GLM-5.1 200k (QUELLE 5) — bei langen Feature-Beschreibungen mit vielen Dateien vorteilhaft für MiMo.
- **Aktive Parameter**: MiMo 42B / 1T total, GLM-5.1 40B / 754B total (QUELLE 2, 5) — vergleichbar pro Token.
- **Preis pro 1M Tokens** (günstigste Route): MiMo $0.43 in / $0.87 out; GLM-5.1 $0.98 in / $3.08 out (QUELLE 5) — MiMo günstiger.

---

## Gesamtbild: Welches macht weniger Fehler beim Umsetzen beschriebener Features?

**Auf Basis der Quellenlage** ergibt sich ein **gemischtes, leicht zu MiMo-V2.5-Pro tendierendes Bild**:

- **Für reine Feature-Umsetzung in Code** (Coding-Benchmarks, Long-Context Reasoning, Instruction-Following) liegt **MiMo-V2.5-Pro vorn** (Terminal-Bench, SciCode, AA-LCR, IFBench, Non-Hallucination) — QUELLE 2, 5.
- **Für komplexe Multi-Step-Tool-Workflows** liegt **GLM-5.1 leicht vorn** (𝜏³-Banking, ITBench, Code-Execution-Flag) — QUELLE 2, 5.
- **Kritischer Vorbehalt gegen MiMo**: QUELLE 1 dokumentiert ein konkretes Praxis-Problem — MiMo verweigert im Live-Test mehrfach Aufgaben mit "Sorry, can't work with that" ohne erkennbaren Anlass, was ein häufiger Fehlertyp bei Feature-Umsetzungen wäre. GLM-5.1 wird in der Quelle nicht mit einem vergleichbaren Verhalten beschrieben.
- **Preis-Leistung**: MiMo ist deutlich günstiger (QUELLE 5).

**Einschränkungen der Aussage**:
- Keine Daten zu **GLM-5.2** (nur GLM-5 / 5.1) — die Frage kann streng genommen nicht beantwortet werden.
- Keine Daten zur Wirkung eines **explizit erhöhten Reasoning-Budgets** auf Fehlerraten.
- Die Aussagen basieren auf öffentlichen Benchmarks; QUELLE 1 zeigt, dass Benchmark-Ergebnisse und Live-Verhalten in Tool-Umgebungen auseinanderklaffen können.
- Modellname in der Frage ("MiniMax MiMo") ist falsch — der Hersteller ist **Xiaomi**.

**Quellen-Belege im Überblick**: QUELLE 1 (Live-Test-Erfahrung), QUELLE 2 (Artificial Analysis Benchmark-Vergleich), QUELLE 5 (LLM Reference Specs/Benchmarks). QUELLEN 3 und 4 behandeln andere Modelle (GLM-5, MiniMax M2.5/M2.7) und sind für diese Frage **nicht direkt anwendbar**.


---

## [A] 4. OpenCode: beste Modelle + Thinking-Einstellungen

# Recherche-Auswertung: OpenCode – Modelle & Reasoning-Einstellungen für agentisches Coding

## Wichtige Vorbemerkungen

1. **Begriff „beschreibungs-basiertes agentisches Coding"**: Dieser exakte Begriff taucht in **keiner** der vorliegenden Quellen auf. Die Quellen sprechen allgemein von „agentic coding", „agentic mode" bzw. von „build"/„plan"-Agenten. Ich beantworte die Frage daher sinngemäß auf Basis der belegten Inhalte zu agentischem Coding allgemein.

2. **Dateiname**: Die Quellen nennen konsistent **`opencode.json`** (nicht `opencode.jsonc`). Allerdings ist `opencode.jsonc` ein verbreiteter alternativer Name für dieselbe Konfigurationsdatei mit JSONC-Syntax (Kommentare erlaubt). Quellen 1, 2 und 5 belegen den Pfad `~/.config/opencode/opencode.json`. [Quelle 5]

---

## 1. Offiziell empfohlene Modelle (laut OpenCode-Doku)

Laut der offiziellen OpenCode-Dokumentation sind folgende Modelle „gut im Codegenerieren UND im Tool-Calling" – ohne Rangordnung, ohne Anspruch auf Vollständigkeit/Aktualität:

- **GPT 5.2**
- **GPT 5.1 Codex**
- **Claude Opus 4.5**
- **Claude Sonnet 4.5**
- **MiniMax M2.1**
- **Gemini 3 Pro**

> „Es gibt viele Modelle da draußen … Allerdings gibt es nur wenige von ihnen, die sowohl gut darin sind, Code zu generieren als auch Tools aufzurufen." [Quelle 1, Quelle 2]

---

## 2. Thinking-/Reasoning-Einstellungen – Konfigurations-Syntax in `opencode.json`

### 2.1 Anthropic-Modelle (Claude) – Thinking-Budget

```json
"anthropic": {
  "models": {
    "claude-sonnet-4-5-20250929": {
      "options": {
        "thinking": {
          "type": "enabled",
          "budgetTokens": 16000
        }
      }
    }
  }
}
```
[Quelle 1, Quelle 2]

### 2.2 OpenAI-Modelle (GPT) – Reasoning Effort, Verbosity, Summary

```json
"openai": {
  "models": {
    "gpt-5": {
      "options": {
        "reasoningEffort": "high",
        "textVerbosity": "low",
        "reasoningSummary": "auto",
        "include": ["reasoning.encrypted_content"]
      }
    }
  }
}
```
[Quelle 1, Quelle 2]

---

## 3. Integrierte Reasoning-Varianten (Built-in)

OpenCode liefert für gängige Provider Default-Varianten aus [Quelle 1, Quelle 2]:

| Provider | Varianten |
|----------|-----------|
| **Anthropic** | `high` (hohes Thinking-Budget, **Default**), `max` (maximales Thinking-Budget) |
| **OpenAI** | `none`, `minimal`, `low`, `medium`, `high`, `xhigh` (modellabhängig) |
| **Google** | `low`, `high` |

Benutzerdefinierte Varianten sind möglich, z. B.:
```json
"variants": {
  "thinking": { "reasoningEffort": "high", "textVerbosity": "low" },
  "fast":     { "disabled": true }
}
```
[Quelle 1, Quelle 2]

---

## 4. Praxis-Empfehlungen aus User-Quellen

### 4.1 „Deep Thinker"-Agent (YouTube / DevOps Toolbox)

Praktisches Beispiel für einen **Deep-Thinker-Agenten** mit Markdown-Header-Konfiguration (statt JSON):
- **Modell**: GPT-5
- **reasoningEffort**: high
- **textVerbosity**: low
- **Prompt/Context**: nicht nötig

> „One example would be a deep thinker using GPT5. High reasoning effort and low verbosity, no prompt needed as context." [Quelle 3]

Zusätzlich erwähnt die Quelle:
- Default-**Temperatur** ist `0.1` (sehr deterministisch); höhere Werte (z. B. 0.8) für mehr Kreativität [Quelle 3]
- Empfehlung des OpenCode-Teams: **Zen** oder **Claude direkt** nutzen [Quelle 3]
- Persönliche Wahl im Video: **Sonnet 4.5** [Quelle 3]

### 4.2 OpenRouter-Statistik (DEV.to)

Laut OpenRouter-Statistik haben OpenCode-Nutzer besonders viel Erfolg mit:
- **Anthropic-Modellen**
- **Open-Source-Modellen**: Kimi K2.5, MiniMax 2.5 [Quelle 4]

### 4.3 OpenCode Go (Abo)

Im OpenCode-Go-Abo enthalten (zu Zeitpunkt des Artikels):
- **GLM-5**
- **Kimi K2.5**
- **MiniMax M2.5**

> „Chances are, models included here will work well with OpenCode." [Quelle 4]

### 4.4 Widerspruch / Kritischer Hinweis

Der DEV.to-Autor berichtet explizit, dass **Gemini 3 Pro** (offiziell empfohlen!) bei ihm **„sehr mäßigen Code produziert" und oft hängenbleibt**:
> „I especially struggled with getting Gemini to work. It produces very subpar code and would often get stuck." [Quelle 4]

---

## 5. Multi-Modell-Setup (Cast AI Tutorial)

Das Cast-AI-Tutorial demonstriert eine konkrete Rollenverteilung für agentisches Coding mit `opencode.json` [Quelle 5]:

| Rolle / Modus | Modell | Begründung |
|---|---|---|
| **`plan` mode** (default) | GLM 5 (`glm-5-fp8`) | „high-level reasoning" |
| **`build` mode** | MiniMax M2.7 (`minimax-m2.7`) | „code generation and execution", kostengünstig |
| Image-Analyse | Kimi K2.5 | multimodales Input |
| GSD-Agenten | gemischt (Plan/Research/Verify → GLM 5; Execute/Debug → MiniMax M2.7) | „Reasoning-heavy tasks go to GLM 5, while high-volume code generation uses the more economical MiniMax M2.7 model." |

Auszug aus der Beispiel-Konfiguration [Quelle 5]:
```json
"model": "ai-enabler/minimax-m2.7",
"mode": {
  "plan":  { "model": "ai-enabler/glm-5-fp8" },
  "build": { "model": "ai-enabler/minimax-m2.7" }
}
```

In den dort gezeigten Provider-Definitionen ist explizit `"reasoning": true` für GLM 5 gesetzt (für MiniMax M2.7 wird im Beispiel `false` gesetzt, in der GSD-2.0-Variante jedoch `true` – **das ist eine Inkonsistenz in der Quelle**).

---

## 6. Was in den Quellen NICHT steht

- **Keine Quelle** nennt konkrete **Benchmarks** speziell für „beschreibungs-basiertes agentisches Coding" oder vergleicht Modelle direkt dafür.
- **Keine Quelle** empfiehlt einen **spezifischen `budgetTokens`-Wert** außer dem Beispiel `16000` für Claude Sonnet 4.5 in der Doku [Quelle 1, Quelle 2].
- **Keine Quelle** gibt eine **kanonische Empfehlung**, ob bei „beschreibungs-basiertem" Coding `high` oder `max` (Anthropic) bzw. `high` vs. `xhigh` (OpenAI) besser ist – es werden nur beide Stufen als verfügbar genannt.
- **Keine Quelle** enthält eine Aussage dazu, ob `opencode.jsonc` (mit Kommentaren) offiziell unterstützt wird – alle Beispiele nutzen `opencode.json`/`.json`.

---

## 7. Zusammenfassung – aus den Quellen belegte Empfehlungen

| Aspekt | Belegte Aussage | Quelle |
|---|---|---|
| Beste Modelle (offiziell) | GPT 5.2, GPT 5.1 Codex, Claude Opus 4.5, Claude Sonnet 4.5, MiniMax M2.1, Gemini 3 Pro | [1, 2] |
| Anthropic Reasoning | `thinking.type: "enabled"` + `budgetTokens` (Beispiel: 16000) | [1, 2] |
| OpenAI Reasoning | `reasoningEffort: "high"` + `textVerbosity: "low"` + `reasoningSummary: "auto"` | [1, 2] |
| Anthropic-Varianten | `high` (Default), `max` | [1, 2] |
| OpenAI-Varianten | `none`/`minimal`/`low`/`medium`/`high`/`xhigh` | [1, 2] |
| Google-Varianten | `low`, `high` | [1, 2] |
| Praxis: Deep-Thinker | GPT-5, `reasoningEffort: high`, `textVerbosity: low` | [3] |
| Praxis: Multi-Model | Plan/Reasoning → GLM 5; Build/Code → MiniMax M2.7; Image → Kimi K2.5 | [5] |
| Real-World-Erfolg | Anthropic + Kimi K2.5 / MiniMax 2.5 (OpenRouter) | [4] |
| Widerspruch | Gemini 3 Pro funktioniert laut User-Report schlecht | [4] |


---

## [A] 5. Prompt-Best-Practices fuer Vibe-Coding

# Best Practices für Prompts beim agentischen Coding (Vibe-Coding)

Basierend ausschließlich auf den vorliegenden Quellen beantworte ich die Frage nach den sechs genannten Aspekten. Aussagen, die in den Quellen nicht oder widersprüchlich behandelt werden, markiere ich ausdrücklich.

---

## 1) Persona

- **Setze zu Beginn eine Rolle/Persona, die dem Agenten die Problemdömane und die gewünschte Expertise vermittelt.** Konkretes Beispiel aus der Quelle: "You're a senior AWS Serverless Python developer specializing in building production grade and secure multi-tenant SaaS solutions". [Quelle 1, Step 1]
- **Schon ein einzelner Satz als Rolle im System-Prompt macht einen Unterschied** – z. B. "You are a helpful coding assistant specializing in Python." [Quelle 5, "Gib Claude eine Rolle"]
- **Role-Play-Prompting** ist eine dokumentierte Technik: Man weist dem Modell eine Persona zu (z. B. "senior Python developer" oder "beginner-friendly coding tutor"), um Ton und Tiefe der Antwort zu steuern. [Quelle 2, Abschnitt "Role-Play Prompting"]
- Hinweis: **Quantifizierte Aussagen, ob/wie stark eine Persona Fehler reduziert**, finden sich in keiner der Quellen – das wird nicht belegt.

---

## 2) Spezifikation (Aufgabe präzise beschreiben)

- **Sei präzise und liefere "Definition of Done"**: "Be precise about the task. Include both functional and non-functional requirements. Give as many details as you want to describe the definition of done—the more details and specific, the better. Help the agent understand the scope of the task. Don't make assumptions or expect it to figure out by itself." [Quelle 1, Step 2]
- **Positiv formulieren, nicht negativ**: "stay positive - tell it what you want and expect; don't tell it what you don't want." [Quelle 1, Step 2]
- **Drei-Ebenen-Struktur (Technik → Funktion → Edge Cases)**, um vage Anweisungen zu vermeiden:
  - **Ebene 1 – Technischer Kontext:** Tech-Stack, Versionen, bestehende Konfigurationen (z. B. "Next.js 14 App Router, TypeScript strict mode, Supabase Client bereits in `lib/supabase.ts` konfiguriert, Tailwind CSS v3"). [Quelle 4, "Das Drei-Ebenen-Prompting"]
  - **Ebene 2 – Funktionale Anforderung:** Konkretes Verhalten (nicht "baue ein Login", sondern "Email/Passwort-Login mit Remember-Me-Checkbox, Button während Submits deaktiviert, Redirect auf `/dashboard`"). [Quelle 4, "Das Drei-Ebenen-Prompting"]
  - **Ebene 3 – Edge Cases & Fehlerbehandlung:** API-Fehler als Toast, Netzwerk-Fallback, Cooldown nach 3 Fehlversuchen. [Quelle 4, "Das Drei-Ebenen-Prompting"]
- **Verhalten statt Implementierung beschreiben:** "Statt 'Nutze useState für ein Modal' besser: 'Ich brauche ein Modal, das sich smooth öffnet, den Hintergrund dimmt und bei Escape schließt.' Gib dem Agent das Ziel – er findet oft einen besseren Weg als du." [Quelle 4, "Die fünf häufigsten Prompting-Fehler", Punkt 4]
- **Schritte explizit auflisten, wenn Reihenfolge/Vollständigkeit wichtig ist** (z. B. nummerierte Listen). [Quelle 5, "Sei klar und direkt"]
- **Instruktive Prompts als Basistyp** in Agent-Setups: "Summarize this contract in bullet points." [Quelle 3, "Prompt Engineering: The Interface Layer"]

---

## 3) Kontext-Umfang

- **Kontext ist beim Agenten kritisch**, weil er nicht erraten kann, was im Kopf des Entwicklers ist: "Context is critical: Ensure the agent has all necessary information since it cannot infer what's in your head." [Quelle 6, "Core principles"]
- **Context Engineering** (Begriff geprägt von Martin Fowler 2026): "die Disziplin, den richtigen Kontext für KI-Tools zu kuratieren." [Quelle 4, Einleitung]
- **Persistente Kontext-Dateien (Projekt-Gedächtnis)**, die in jede Session geladen werden:
  - **CLAUDE.md / .cursorrules / AGENTS.md** an Projekt-Root: "Hier dokumentierst du Tech-Stack, Coding-Standards, Architektur-Entscheidungen und Projekt-Konventionen." [Quelle 4, "Context Engineering"]
  - **Pfadbasierte Rules Files** für spezifische Dateitypen (z. B. `*.ts` TypeScript-Konventionen, `*.sh` Shell-Syntax). [Quelle 4, "Rules Files und CLAUDE.md"]
  - **Skills / Custom Commands** (z. B. Slash Commands in Claude Code) für wiederkehrende Aufgaben wie Code Review, Commit Messages, Test-Generierung. [Quelle 4]
  - **Memory-Dateien** (z. B. `memory.md` / `plan.md`) für aktuellen Projektstatus. [Quelle 4]
- **Standardsektionen eines CLAUDE.md**: Projekt-Übersicht, Coding-Konventionen, Build/Test-Befehle, bekannte Einschränkungen, Repository-Etikette. [Quelle 4]
- **.claudeignore analog zu .gitignore**: "Build-Artefakte, `node_modules` und generierte Dateien verbrauchen Token, ohne Mehrwert zu liefern." [Quelle 4]
- **Externe Quellen anbinden**: MCP-Server an interne Datenquellen (Jira, Confluence, Notion, GitHub); "The tighter the integration, the smarter your agent becomes." [Quelle 1, Einleitung]
- **Direktlinks** zu Doku sind effektiv: "Agents work well with direct HTTP pages (if you know where to point) or local rules and configuration files that you can place in your repository." [Quelle 1, Step 3]
- **Lange Dokumente oben platzieren** im Prompt, Anfrage ans Ende: "Anfragen am Ende können die Antwortqualität in Tests um bis zu 30 % verbessern, besonders bei komplexen Eingaben mit mehreren Dokumenten." [Quelle 5, "Prompting mit langem Kontext"]
- **Lange Dokumente in XML-Tags strukturieren** (`<document>`, `<document_content>`, `<source>`) und **Antworten in Zitaten verankern** lassen, damit das Modell relevante Stellen zuerst extrahiert. [Quelle 5, "Prompting mit langem Kontext"]
- **Inhaltliche Begründungen mitliefern, nicht nur Anweisungen** – "Wenn du Kontext oder Motivation hinter deinen Anweisungen lieferst … kann Claude deine Ziele besser verstehen und gezieltere Antworten liefern." [Quelle 5, "Füge Kontext hinzu"]
- **Beispiel Effizienzgewinne:** "Claude Code verbraucht laut Benchmarks 5,5-mal weniger Tokens als vergleichbare Tools für äquivalente Aufgaben – vorausgesetzt, der Kontext ist sauber kuratiert." [Quelle 4] — ⚠️ **Hinweis:** Dies ist eine herstellerspezifische Benchmark-Behauptung von NeverCodeAlone, nicht unabhängig belegt.

---

## 4) Beispiele

- **Few-Shot-Prompting** als zentrale Technik: "Ein paar gut gestaltete Beispiele (bekannt als 'few-shot' oder 'multishot prompting') können Genauigkeit und Konsistenz dramatisch verbessern." [Quelle 5, "Verwende Beispiele effektiv"]
- **Optimale Anzahl:** "Füge 3–5 Beispiele für beste Ergebnisse hinzu." [Quelle 5, "Verwende Beispiele effektiv"]
- **Beispiele müssen relevant, vielfältig und strukturiert sein**, umhüllt mit `<example>`-Tags: "Decke Grenzfälle ab und variiere genug, damit Claude keine unbeabsichtigten Muster aufgreift." [Quelle 5, "Verwende Beispiele effektiv"]
- **One-Shot / Few-Shot Unterschied:** "Oneshot prompting … you provide a single example … Few-shot prompting … you provide several examples … The more examples you give, the better it can learn the pattern." [Quelle 2]
- **Beispiele aus Code-Doku direkt im Prompt verlinken**, z. B. "(see example at https:// …)" für die Nutzung einer internen Library. [Quelle 1, Step 3]

---

## 5) Anti-Patterns (typische Fehler)

Die Quellen nennen übereinstimmend mehrere Anti-Patterns:

1. **Fehlender technischer Kontext** – Agent rät Stack/Version und generiert Code für falsches Framework. [Quelle 4, Fehler #1]
2. **Vage Fehlerbeschreibungen ("fix this")** – "Vage Fehlerbeschreibungen führen zu wildem Herumprobieren." Stattdessen: Was ist passiert, was hätte passieren sollen, relevanter Code + Error-Log beifügen. [Quelle 4, Fehler #2]
3. **Gesamte App in einem Prompt generieren** (sog. "Butterfly-Effekt"): "Große Prompts erzeugen große, schwer prüfbare Codeblöcke. Der 'Butterfly-Effekt' – ein frühes Missverständnis, das sich durch das gesamte Feature zieht – ist der destruktivste Fehlmodus im Agentic Coding." [Quelle 4, Fehler #3]
4. **Implementierung statt Verhalten beschreiben** (siehe oben, Spezifikation). [Quelle 4, Fehler #4]
5. **Security ignorieren** – "Ohne explizite Security-Anweisungen im Prompt entstehen hardcodierte Credentials, fehlende Input-Validierung und offene Endpoints." [Quelle 4, Fehler #5]
6. **"Mega-Prompt" / monolithische Prompts** – "Die Ära des Mega-Prompts ist vorbei. … Strategische Dekomposition schlägt monolithische Prompts." [Quelle 4, "Iteratives Prompting"]
7. **Lange Sessions** – "Wenn du den Agent mehr als zweimal für das gleiche Problem korrigieren musst, ist der Kontext verschmutzt." → frische Sessions pro Task. [Quelle 4, "Iteratives Prompting"]
8. **Nur erstes Ergebnis akzeptieren** – "Don't stop at the first response — even with a solid prompt, results can vary." Unterschiedliche Modelle (GPT-4o, Claude, Gemini) haben unterschiedliche Stärken; Wechsel kann helfen. [Quelle 1, Step 5]
9. **Dem Agenten die Führung überlassen** – "The key is to guide the agent — not let it guide you." [Quelle 1, Einleitung]
10. **Dem Agenten die Architektur/Standards selbst überlassen** – "The agent uses examples from the dataset it was trained on; it doesn't mean they correlate to your standards or security requirements (let me save the effort - they don't)." [Quelle 1, Step 3]

**Statistische Verteilung von Frustration** (einmalig in Quelle 4, nicht unabhängig belegt): "66 Prozent der Entwickler berichten vom '80-Prozent-Problem'; 45 Prozent geben an, dass das Debugging von KI-generiertem Code länger dauert als ihn selbst zu schreiben." [Quelle 4] — ⚠️ Herkunft der Erhebung wird in der Quelle nicht zitiert.

---

## 6) Verifikation (Plan erstellen, iterieren, testen)

- **Plan-First-Prinzip:** "Before it starts coding, ask it to explain its plan — this lets you steer the direction, apply your domain knowledge, and avoid hallucinations." Konkret im Prompt: "Before making changes, list your assumptions, the plan, and any potential risks." [Quelle 1, Step 4]
- **Iterativer Workflow "Spec → Plan → Implement → Review → Iterate"**:
  1. Planen lassen: "Erstelle einen Plan für Feature X. Analysiere zuerst die bestehenden Abhängigkeiten. Schreibe noch keinen Code."
  2. Plan reviewen auf Overengineering, fehlende Edge Cases, Architektur-Kompatibilität.
  3. Schrittweise implementieren und pro Schritt testen + committen.
  4. Automatisierte Tests ausführen. [Quelle 4, "Iteratives Prompting"]
- **Schrittweise Bestätigung pro Tool-Aufruf** (Cline-Pattern): "The agent can only use one tool per message and must wait for confirmation after each execution. This creates a feedback loop that minimizes the risk of cascading errors and going off in the wrong direction." [Quelle 6, "Cline's System Prompt"]
- **Plan Mode vs. Act Mode:** "In PLAN MODE, the agent gathers context, asks clarifying questions, and brainstorms ideas. Once a clear strategy is in place, it switches to ACT MODE to execute the plan step-by-step." [Quelle 6, "Cline's System Prompt"]
- **Testen ist nicht optional:** "KI-generierter Code ohne Tests ist Prototyp-Code, kein Production-Code." [Quelle 4, "Iteratives Prompting"]
- **Iteration als Prinzip:** "Prompt engineering is an iterative process - no way around it! The faster you can get testing, the faster you can learn." [Quelle 6, "Core principles"]
- **Annahmen explizit auflisten lassen** als Teil des Plans, um Halluzinationen sichtbar zu machen. [Quelle 1, Step 4]
- **Test-Agenten in SPARC**: TDD-Zyklen, Linting, Härtung, Best-Practice-Injection; "Test agents run TDD cycles." [Quelle 3, "SPARC in Agentic Engineering"]
- **Self-Verification durch strukturierte Outputs:** XML-Tags für Antworten, damit der Agent das Format zuverlässig einhält ("reduziert Fehlinterpretationen"). [Quelle 5, "Strukturiere Prompts mit XML-Tags"]

---

## Zusätzliche strukturelle Empfehlungen aus den Quellen

- **Zuerst Problem verstehen, dann prompten:** "Before you write a single prompt, make sure you clearly understand the problem you're trying to solve. Gather as much context, requirements, and definitions of done as possible." [Quelle 1, "My Six Steps"]
- **Geteilte, versionierte Prompts als Team-Asset:** "Reusable, secure prompts that align with your team's standards are key to scaling AI-driven development." [Quelle 1, Step 6]
- **Tool-Definitionen sind Teil des Prompt-Engineerings:** "When setting up tools, you should put as much effort into their configuration — name, description, etc — as you do into crafting your prompts." [Quelle 6, "Core components of agents"]
- **Reason + Act (ReAct) für komplexe Multi-Step-Aufgaben:** "ReAct guides the model through a reason[ing] … and then act by performing a task such as calling a tool, querying a database, or making a decision." [Quelle 2]
- **Chain-of-Thought für komplexe Aufgaben:** "Encourages … to think step by step before providing a final answer." [Quelle 2]

---

## Was die Quellen NICHT abdecken (ausdrücklich)

- **Empirische Vergleichszahlen** zu Fehlerraten mit/ ohne Persona, Few-Shot etc. werden in keiner der Quellen genannt.
- **Verifikationstools** (z. B. statische Analyse, automatische Reviewer-Agenten über die Erwähnung von "Test agents" in Quelle 3 hinaus) werden nicht systematisch behandelt.
- **Kosten-Nutzen-Analysen** der Drei-Ebenen-Struktur oder von CLAUDE.md fehlen als unabhängige Belege.
- **Spezifische Empfehlungen für verschiedene Sprachen/Frameworks** jenseits der genannten Beispiele (Python/Serverless, Next.js/TypeScript) sind in den Quellen nicht vorhanden.
- **Widersprüche zwischen den Quellen**: Die Quellen sind im Kern kompatibel. Einziger potenzieller Widerspruch: Quelle 1 rät explizit dazu, "vor dem Coden einen Plan zu erstellen", während Quelle 4 zusätzlich "frische Sessions pro Task" fordert – beides ist kombinierbar, wird aber nicht in einer Quelle zusammen explizit gemacht.

---

## Konsolidierte Quellen-Übersicht

| Aspekt | Hauptquelle(n) |
|---|---|
| Persona | Quelle 1 (Step 1), Quelle 2 (Role-Play), Quelle 5 ("Gib Claude eine Rolle") |
| Spezifikation | Quelle 1 (Step 2), Quelle 4 (Drei-Ebenen), Quelle 5 ("Sei klar und direkt") |
| Kontext-Umfang | Quelle 4 (Context Engineering, CLAUDE.md, Rules, MCP, .claudeignore), Quelle 1 (Step 3), Quelle 5 (Long Context) |
| Beispiele | Quelle 5 (Few-Shot, 3–5 Beispiele), Quelle 2 (One-/Few-Shot), Quelle 1 (Step 3, Doku-Links) |
| Anti-Patterns | Quelle 4 (5 Fehler), Quelle 1 (Steps 3/5), Quelle 3 (SPARC-Limitierungen von Vibe Coding) |
| Verifikation | Quelle 1 (Step 4), Quelle 4 (Spec→Plan→Implement→Review→Iterate), Quelle 6 (Cline Plan/Act Mode, Bestätigungen) |


---

## [A] 6. Wenig Harness / minimaler Setup (Capability Floor)

# Kontext, Tooling und Agent-Harness für zuverlässiges agentisches Coding — Auswertung der Quellen

## Die Grundgleichung: Agent = Modell + Harness

Alle Quellen konvergieren auf dieselbe Formel: Das Modell liefert das Reasoning, der Harness liefert die Ausführung. Simon Willison formuliert es so: „A coding agent is a piece of software that acts as a harness for an LLM, extending that LLM with additional capabilities that are powered by invisible prompts and implemented as callable tools." Harrison Chase (LangChain) bringt es auf die kürzeste Formel: „If you're not the model, you're the harness." (Quelle 3, 5).

Karpathy wiederum nutzt das Hardware-Metapher: „The LLM is the CPU, the context window is RAM." Das Modell ist die CPU — ohne Harness bleibt es in einem Raum ohne Hände (Quelle 3, 6).

---

## Argumente FÜR minimalen Harness („wenig ist mehr")

### 1. Vercel-Beweis: 15 Tools → 2 Tools
Das stärkste empirische Argument für Minimalismus liefert Vercels Text-to-SQL-Agent d0. Ausgangslage: 15 spezialisierte Tools (GetEntityJoins, LoadCatalog, RecallContext, SyntaxValidator, ExecuteSQL u. v. m.). Ergebnis: 80 % Accuracy auf einem 5-Query-Benchmark, im Worst Case 724 Sekunden, 145.463 Tokens, 100 Schritte — und trotzdem gescheitert.

Nach der Reduktion auf nur zwei Tools (ExecuteCommand für Bash und ExecuteSQL) stieg die Accuracy auf 100 %, die Tokens fielen um 37 %, die Geschwindigkeit verbesserte sich um den Faktor 3,5. Vercels Fazit wörtlich: „The best agents might be the ones with the fewest tools." (Quelle 1)

### 2. Manus: Vier Rebuilds, größte Gewinne durch Entfernen
Manus baute sein Agent-Framework viermal komplett neu. „Each rebuild followed a pattern: removing user-facing complexity that seemed necessary but was degrading performance." Komplexe Document-Retrieval-Systeme wurden durch direkten Dateisystemzugriff ersetzt, „fancy routing logic" wurde gestrichen. Der Durchschnitt liegt bei ~50 Tool-Calls pro Task; das Filesystem dient als externer Speicher. (Quelle 1)

### 3. Anthropic-Leitprinzip
DataCamp zitiert explizit: „Starte so einfach wie möglich und füge nur dann bewegliche Teile hinzu, wenn die Aufgabe sie wirklich braucht." (Quelle 5)

### 4. Warum das funktioniert
Spezialisierte Tools werden für fähige Modelle zum Bottleneck. „Each specialized tool is a constraint point — the model must learn its schema, handle its errors, and decide when to use it versus alternatives. With 15 tools, the model spends more tokens choosing than doing." Generische Tools (bash, grep, cat, find) matchen hingegen die Trainingsdaten der Modelle direkt: „They know how to `grep`. They do not know how to call `GetEntityJoins` with the right parameters." (Quelle 1)

### 5. Progressive Disclosure als Kompromiss
Viele moderne Harnesses (2026) nutzen progressive Offenlegung: Beim Start wird nur eine Zusammenfassung der verfügbaren Tools in den Kontext geladen; vollständige Tool-Beschreibungen werden erst nachgeladen, wenn das Modell das jeweilige Tool tatsächlich braucht. So bleibt der Prompt schlank, ohne die Tool-Fähigkeit zu opfern. (Quelle 5)

### 6. OpenDev: „Lazy Tool Discovery"
Auch das OpenDev-System aus dem ArXiv-Paper setzt auf lazy tool discovery, um den Prompt-Budget zu schonen. (Quelle 2)

---

## Argumente FÜR einen umfangreichen, gut designten Harness

### 1. APEX-Agents-Benchmark: Real-World-Performance ist katastrophal
Das APEX-Agents-Benchmark (Mercor, Januar 2026) testet 480 reale professionelle Aufgaben aus Banking, Consulting und Law. Bestes Pass@1: 24,0 %. Pass@8: ~40 %. Zero-Score-Raten: 40–62 %. Timeouts (>250 Schritte): bis zu 30 %. Die kritische Erkenntnis: „These failures were predominantly not knowledge failures. The models had the information and could reason through the problems in isolation. The failures were execution and orchestration problems — agents getting lost after too many steps, looping back to failed approaches, and losing track of their objectives mid-task." (Quelle 1)

### 2. LangChain: Harness allein bewegt 13,7 Prozentpunkte
LangChain hielt das Modell fix (GPT-5.2-Codex) und änderte ausschließlich den Harness. Auf Terminal Bench 2.0 stieg die Performance von 52,8 % auf 66,5 % — ein Sprung aus den Top 30 in die Top 5, bei identischem Modell. (Quelle 3)

### 3. UC Berkeley MAST-Taxonomie
Die MAST-Taxonomie analysierte über 1.600 Agent-Execution-Traces aus sieben Frameworks und fand: „the majority of failures trace to specification and system design, not model limitations." (Quelle 3)

### 4. Anthropic: 512.000 Zeilen Orchestrierung
Als Anthropic im März 2026 versehentlich den vollständigen TypeScript-Source von Claude Code in einer .map-Datei veröffentlichte, zeigte sich: 512.000 Zeilen Orchestrierungs-Code, aber ein „radically simple while(tool_call) loop". Anthropic selbst folgerte: „Building reliable AI agents is primarily an orchestration engineering problem, not a model capability problem." (Quelle 3)

### 5. Google DeepMind: Mehr Agents ≠ Bessere Performance
In 180 kontrollierten Konfigurationen fand Google DeepMind: „adding more agents actually degrades performance by 39 to 70 percent on sequential reasoning tasks." Komplexität im Harness kann also auch schaden. (Quelle 3)

### 6. OpenDev: Compound AI Architecture als Antwort
Das OpenDev-Paper argumentiert für eine ausgedehnte Compound-AI-Architektur mit:
- Workload-spezialischem Model-Routing (jeder Cognitive-Workflow bekommt sein eigenes LLM)
- Dual-Agent-Architektur, die Planung und Ausführung trennt
- Adaptive Context Compaction, die ältere Observations progressiv verdichtet
- Automatisiertem Memory-System für projekt-spezifisches Wissen über Sessions hinweg
- Event-driven System Reminders gegen Instruction-Fade-out (Quelle 2)

Die Begründung: Benchmarks wie Terminal-Bench und LongCLI-Bench zeigen, „that even frontier models struggle with continuous terminal operation, underscoring the need for purpose-built engineering solutions." (Quelle 2)

### 7. Cursor: 30 % Performance-Drop durch Harness-Konfiguration
Cursors Engineering-Team dokumentierte: „removing reasoning traces from one model caused a 30% performance drop" — eine Änderung allein an der Harness-Konfiguration, ohne Modell-Wechsel. (Quelle 6)

### 8. Boris Chernys Philosophie
Der Claude-Code-Erfinder formuliert das Design-Prinzip: „designing agentic systems for the model that's coming — not just the model you have today." Wer den Harness auf ein spezifisches Modell zuschneidet, bricht beim Modell-Wechsel; wer ihn gut strukturiert, profitiert automatisch von jedem besseren Modell. (Quelle 6)

### 9. Fünf nicht-verhandelbare Harness-Schichten
Pappas (Quelle 1) definiert fünf Schichten, die ein produktiver Agent braucht:
1. Context Management
2. Tool Selection
3. Error Recovery
4. State Management
5. External Memory

DataCamp (Quelle 5) ergänzt: System-Prompts/Verhaltensregeln, Tools, Speicher, Ausführungsumgebung, Orchestrierung/Planung, Guardrails, Observability/Tracing. „Ein kleiner Agent braucht vielleicht nur wenige dieser Bausteine, ein produktiver Agent deutlich mehr."

### 10. Zwei typische Fehlmodi
Bhatt (Quelle 3) identifiziert zwei gegenläufige Failure-Modes:
- Das Plateau: Der Engineer bleibt bei einem funktionierenden Workflow stehen und entwickelt sich nicht weiter.
- Das Treadmill: Der Engineer adoptiert alles (jeden MCP-Server, jedes neue Skill-Format) — „More tools, worse results."
Beide haben dieselbe Wurzel: „We are thinking about agents as chat interfaces with better autocomplete. We are not thinking about them as systems."

---

## Die Rolle des Modell-Reasonings

### Capability Floor
Die wichtigste Nuance liefert Pappas (Quelle 1): Die Hypothese „Harness dominiert das Ergebnis" gilt „above a model capability floor. Below that floor, no harness compensates for insufficient reasoning. Above it, harness engineering dominates outcomes."

Anders gesagt: Es gibt ein Kapazitäts-Mindestniveau, unter dem kein noch so guter Harness hilft. Wer ein schwaches Modell mit viel Orchestrierung umgibt, bekommt trotzdem keine zuverlässigen Agenten.

### Modell + System-Prompt = Verhalten
Drew Breunig (zitiert in Quelle 6) zeigt: „Same model, different system prompt, completely different workflow." Identische Modelle verhalten sich mit Codex-Stil-Prompt documentation-first und mit Claude-Stil-Prompt iterativ-trail-and-error. Das Reasoning wird durch den System-Prompt kalibriert, nicht ersetzt.

### Tool-Model-Pairing
Quelle 6 argumentiert: Die Qualität eines Coding-Tools hängt auch davon ab, wie eng Tool-Team und Modell-Training-Team zusammenarbeiten. Claude Code funktioniert besonders gut mit Claude-Modellen, weil beide Teams im selben Unternehmen sitzen — Insider-Zugang zu Quirks und Kalibrierungs-Bedarf. Cursor hingegen tunet von außen für jedes Modell über interne Benchmarks.

### Modell liefert Intelligenz, Harness macht sie nutzbar
Bhatt (Quelle 3): „Model contains the intelligence. Harness makes it usable." Das Modell denkt, plant, entscheidet. Der Harness agiert: ruft Tools, liest Konfiguration, verwaltet Speicher, verifiziert. „The model gives instructions to the harness. The harness takes action. The model observes what happened. Then it reasons again. That loop is the entire architecture."

### Zwei-Agenten-Pattern zeigt die Reasoning-Grenze
Im Beispiel aus Quelle 3 produzierte Claude Code 2.000 Zeilen funktionierenden Code, machte aber eine fundamentale Architekturentscheidung falsch (Session-Management pro Bot statt Broker-Pattern). Erst ein zweiter Agent (Codex) identifizierte den Fehler. Das demonstriert: Die Loop selbst ist bounded by context and specification — ohne explizite zweite Perspektive bleiben bestimmte Reasoning-Lücken unsichtbar.

---

## Synthese: Was die Quellen wirklich sagen

Die Frage „minimal vs. umfangreich" ist in den Quellen falsch gestellt. Die konvergente Antwort lautet: **Richtig dimensioniert und gut designt, nicht maximal oder minimal.**

| Dimension | Was die Quellen sagen |
|---|---|
| **Tools** | So wenige wie möglich, so viele wie nötig. Vercel zeigt: 2 generische Tools schlagen 15 spezialisierte. Aber für produktive Arbeit sind mehrere Tool-Kategorien nötig. |
| **Context** | Progressive Disclosure, adaptive Compaction, automatisches Memory. Kontext ist endlich; der Harness muss aktiv verwalten. |
| **Konfiguration** | Nicht das Modell wechseln, sondern den Harness tunen. LangChain: 13,7 % allein durch Harness. |
| **Komplexität** | Mehr Agents ≠ besser (Google DeepMind). Compound-Architektur ≠ unnötig (OpenDev). |
| **Modell-Reasoning** | Capability Floor ist real; darunter hilft kein Harness. Darüber dominiert der Harness. |
| **Anpassung** | Design für das kommende Modell, nicht für das aktuelle. Harness-Updates brauchen Zeit nach Modell-Releases. |

**Was die Quellen NICHT abdecken oder wo sie widersprüchlich sind:**

- Eine konkrete Mindestanzahl an Tools, ab der ein Coding-Agent zuverlässig arbeitet, wird nicht genannt.
- Vercels Benchmark wird explizit als „small sample" gekennzeichnet (5 Queries). Die Generalisierbarkeit ist offen.
- Die LangChain-Resultate (52,8 → 66,5 %) beziehen sich auf Terminal Bench 2.0 — ob das auf reale Software-Engineering-Aufgaben übertragbar ist, wird in den Quellen nicht geklärt.
- Ob „minimaler Harness" bei lang-horizon-Tasks (50+ Schritte) noch funktioniert, ist offen. Quelle 2 und Quelle 1 deuten an, dass dort umfangreichere Mechanismen nötig werden.
- Quelle 4 (IKANGAI) enthält keinen sichtbaren Inhalt über den Titel hinaus.

---

## Fazit (Quellen-gestützt)

1. **Weniger Tools, mehr Modell-Vertrauen** ist empirisch gestützt (Vercel, Manus, Anthropic-Leitprinzip), aber nur oberhalb eines Modell-Capability-Floors.
2. **Ein durchdachter Harness ist nicht optional** — UC Berkeley MAST, APEX-Agents, LangChain und Anthropic selbst zeigen konsistent, dass die Mehrheit der Agent-Failures Orchestrierungs- und nicht Reasoning-Probleme sind.
3. **Modell-Reasoning bleibt die Grundlage.** Es liefert die Intelligenz, die der Harness operabel macht. Die richtige Architektur ist ein Compound-System, in dem Modell und Harness als Einheit evolvieren.
4. **Anti-Patterns sind eindeutig identifiziert:** das „Treadmill" (immer mehr Tools) und das „Plateau" (nie aktualisierter Workflow). Beide sind gleichermaßen schlecht.
5. **Die wahre Engineering-Disziplin** nennt Bhatt „Agentic Engineering" (Quelle 3), Pappas „Agent Harness Engineering" (Quelle 1) — und beide betonen, dass dies System-Design auf OS-Niveau ist.


---

## [A] 7. Fuehrende Coding-Modelle 2026 mit Thinking (OpenCode/OpenRouter)

# LLM-Rangliste für agentisches Coding mit Reasoning/Thinking (2026)

Hinweis vorab: Die Quellen liefern teils unterschiedliche Perspektiven (Benchmarks, reale OpenRouter-Nutzung, Use-Case-Empfehlungen). Ich stelle sie separat dar. Die genannten Modellnamen entsprechen exakt der Schreibweise in den Quellen.

---

## 1) OpenRouter-Programmier-Rangliste (nach Nutzungsvolumen, Stand Juni 2026)

Quelle 4 (OpenRouter-Collection „Best AI Models for Coding") listet folgende Reihenfolge nach Token-Volumen (Top-9):

| # | Modell | Anbieter | Tokens | Anteil |
|---|---|---|---|---|
| 1 | MiMo-V2.5 | xiaomi | 3,92T | 21,7 % |
| 2 | MiniMax M3 | minimax | 2,17T | 12,0 % |
| 3 | Hy3 preview | tencent | 1,31T | 7,3 % |
| 4 | Claude Opus 4.7 | anthropic | 1,1T | 6,1 % |
| 5 | DeepSeek V4 Pro | deepseek | 1,06T | 5,9 % |
| 6 | GLM 5.1 | z-ai | 940B | 5,2 % |
| 7 | DeepSeek V4 Flash | deepseek | 819B | 4,5 % |
| 8 | GLM 5.2 | z-ai | 703B | 3,9 % |
| 9 | Nemotron 3 Ultra (free) | nvidia | 671B | 3,7 % |

(Quelle 4)

---

## 2) Open-Source-Rangliste für agentisches Coding (Kilo Code, Top 12)

Quelle 2 sortiert nach SWE-Bench Verified/Pro, Terminal-Bench 2.0/2.1, LiveCodeBench und realem Leaderboard-Traffic:

| # | Modell | Highlight | Specs |
|---|---|---|---|
| 1 | **GLM-5.1** (Z.ai) | Best for long-horizon agentic engineering | 744B-A40B · 200K · MIT · SWE-Bench Pro SOTA |
| 2 | **MiniMax M3** (minimax) | Neues Frontier-Modell (Juni 2026) | MoE (MSA) · 1M · Open Weights · 59,0 % SWE-Bench Pro |
| 3 | **Kimi K2.6** (Moonshot) | Best for agent swarms (300 Sub-Agents, 12-h-Autonomie) | 1T-A32B · 256K · Modified MIT · 58,6 % SWE-Bench Pro |
| 4 | **DeepSeek V4-Pro** | Best for 1M context | 1,6T-A49B · 1M · MIT · LiveCodeBench 93,5 |
| 5 | **DeepSeek V4-Flash** | Best cost-efficient self-hosted MoE | 284B-A13B · 1M · MIT · läuft auf einer H100 |
| 6 | **Qwen3-Coder-Next** | Best efficiency per active param | 80B-A3B · 256K · Apache 2.0 |
| 7 | **Qwen3.6-27B** | Best dense model für Repo-Coding | 27B dense · 262K · Apache 2.0 |
| 8 | **MiniMax M2.5** (minimax) | Best free hosted open-weight | MoE · 200K · Open Weights |
| 9 | GLM-5 (Z.ai) | Best foundation for local self-hosting | 744B-A40B · 200K · MIT |
| 10 | Nemotron 3 Super (NVIDIA) | Best for NVIDIA-optimized agent infrastructure | 120B-A12B · 1M · Nemotron Open · 91,8 RULER@1M |
| 11 | Devstral 2 (Mistral) | Best Mistral coding model | 123B · 256K · Apache 2.0 · 72,2 % SWE-Bench Verified |
| 12 | Trinity Large Thinking (Arcee) | Best US-origin open reasoning model | 399B sparse MoE · 128K · Apache 2.0 |

(Quelle 2)

### Use-Case-Empfehlungen aus Quelle 2:
- **Best overall agentic coding:** GLM-5.1
- **Best newest open-weight frontier:** MiniMax M3
- **Best for 1M-token context:** DeepSeek V4-Pro
- **Best cost-efficient self-hosted:** DeepSeek V4-Flash
- **Best for consumer hardware:** Qwen3.6-27B
- **Best free hosted (long context):** Nemotron 3 Super
- **Best free hosted (high throughput):** MiniMax M2.5
- **Best for agent swarms:** Kimi K2.6
- **Best efficiency per active param:** Qwen3-Coder-Next
- **Best US-origin open reasoning:** Trinity Large Thinking

---

## 3) Reasoning/Thinking-Fähigkeiten (explizit genannt)

Modelle, bei denen die Quellen explizit Reasoning/Thinking-Modi erwähnen:

- **GLM 5.2** (OpenRouter, Quelle 4): „large-scale reasoning model … reasoning efforts `high` and `xhigh` are supported"
- **DeepSeek V4 Pro / V4 Flash** (Quelle 4): „Reasoning efforts `high` and `xhigh` are supported; `xhigh` maps to max reasoning"
- **Hy3 preview** (Tencent, Quelle 4): „configurable reasoning levels across disabled, low, and high modes"
- **MiMo-V2.5** (Quelle 4): „Pro-level agentic performance"
- **MiniMax M3** (Quelle 4): trainiert für „long-horizon agentic work, coding, and tool use", basierend auf MSA
- **Claude Opus 4.7** (Quelle 5): „#1 spot on the LMSYS Arena leaderboard in thinking mode"
- **GPT-5.5** (Quelle 5): LMSYS Rank 7 in „high-reasoning mode"
- **Kimi K2.6** (Quelle 2): „multi-step coordination, 12-hour autonomous runs"
- **Trinity Large Thinking** (Quelle 2): explizit „thinking model for multi-turn tool calling"

---

## 4) Verfügbarkeit über OpenCode

Quelle 6 beschreibt OpenCode (SST/Anomaly) als „open-source, provider-agnostic coding agent" mit:
- **75+ LLM-Provider** (Claude, GPT, Gemini, Grok sowie lokale Modelle via Ollama/llama.cpp)
- Dual-Agent-Architektur (build/plan)
- LSP- und MCP-Support
- vollständig offline-fähig

→ Damit sind grundsätzlich **alle oben genannten Modelle auch über OpenCode nutzbar**, sofern der jeweilige Provider angebunden ist (laut Quelle 5 basiert der Erfahrungsbericht explizit auf „real agent loops – Claude Code, Copilot, and OpenCode"). (Quellen 5, 6)

---

## 5) Geschlossene Frontier-Modelle im Real-World-Vergleich (dev.to, Mai 2026)

Quelle 5 nennt diese SWE-bench-Verified-Werte (vendor-reported) und LMSYS-Ränge:

| Modell | SWE-bench Verified | LMSYS |
|---|---|---|
| GPT-5.5 (Apr 2026) | 88,7 % | #7 (high) |
| Claude Opus 4.7 (Apr 2026) | 87,6 % | #1 (thinking) |
| Claude Opus 4.6 | 80,8 % | #3 (thinking) |
| Gemini 3.1 Pro (Feb 2026) | 80,6 % | #4 |
| Kimi K2.6 | 80,2 % | #28 |
| Claude Sonnet 4.6 (Feb 2026) | 79,6 % | #23 |
| DeepSeek V4-Flash | ~79 % | #24 |
| Gemini 3 Flash (high) | 78,0 % | – |
| Grok 4.3 | ~73 % | #34 |
| GPT-5.4 / GPT-5.4 Mini | – / 56,2 % | #11 (high) / – |

Dev.to-Bewertung: „Frontier closed models (GPT-5.5, Claude Opus 4.7, Gemini 3.1 Pro) are still the safest bet for one-shot the hard ticket work." (Quelle 5)

---

## 6) Beste Coding-Agents 2026 (MightyBot, April 2026)

Wichtig: Hier werden **Agenten** (nicht LLMs) gerankt:

| # | Agent | Modell/Stack |
|---|---|---|
| 1 | Codex (OpenAI) | GPT-5.5, 82,7 % Terminal-Bench 2.0 |
| 2 | Claude Code (Anthropic) | Opus 4.7, 1M Kontext |
| 3 | **OpenCode** (SST/Anomaly) | 75+ Provider, offline-fähig |
| 4 | Gemini CLI (Google) | Gemini 3.1 Pro, 1M Kontext, Free |
| 5 | Cursor (Anysphere) | Cloud Agents mit Computer Use |
| 6 | GitHub Copilot | Async Coding Agent |

(Quelle 6)

---

## Synthese / Empfehlung auf Basis der Quellen

**Wenn rein auf OpenRouter-Verfügbarkeit + Agentic-Thinking geachtet wird**, sind laut Quellen 2 und 4 die Top-Kandidaten:
1. **MiniMax M3** (OpenRouter #2, mit 1M-Kontext + Reasoning-Architektur MSA)
2. **GLM 5.1 / 5.2** (OpenRouter #6/#8, explizit als „reasoning model" deklariert)
3. **DeepSeek V4 Pro / Flash** (OpenRouter #5/#7, mit `xhigh`-Reasoning-Modus)
4. **Kimi K2.6** (Kilo-Rang #3, 300-Sub-Agent-Swarms)
5. **Claude Opus 4.7** (OpenRouter #4, #1 LMSYS im Thinking-Mode)
6. **MiMo-V2.5** (OpenRouter #1 nach Volumen, 1M Kontext)
7. **Qwen3-Coder-Next / Qwen3.6-27B** (lokal/self-hosted)

**Widersprüche / Lücken in den Quellen:**
- Quelle 2 listet **MiniMax M3 auf #2** der Open-Source-Rangliste; Quelle 4 (OpenRouter) führt es auf #2 nach Nutzungsvolumen. Beide Quellen stimmen also in der hohen Platzierung überein, gewichten aber unterschiedliche Kriterien.
- GPT-5.5 vs. Claude Opus 4.7: Quelle 5 nennt GPT-5.5 mit 88,7 % vor Opus 4.7 (87,6 %) auf SWE-bench; Quelle 6 hingegen sieht Codex/Claude Code im Agent-Ranking vorne – wobei hier Agent-Workflow, nicht nur Modell-Score, zählt.
- Quelle 5 weist ausdrücklich darauf hin, dass **Vendor-Scores 4–8 Punkte über unabhängigen Reproduktionen** liegen und Harness-Wahl enorme Auswirkungen hat.
- **OpenCode-spezifische Empfehlungen** (etwa „beste Modellwahl innerhalb von OpenCode") werden in den Quellen nicht explizit gegeben – es wird nur erwähnt, dass OpenCode 75+ Provider unterstützt und mit Claude Code/Copilot/OpenCode getestet wurde (Quellen 5, 6).


---

## [A] 8. Reasoning-Effort (low/medium/high/xhigh) -> Coding-Fehlerrate

# Einfluss der Reasoning-Effort-Stufe auf Fehlerrate und Qualität beim KI-Code-Editing

## Wichtige Einschränkung vorab

Die Frage nennt **fünf Stufen** (none, low, medium, high, xhigh). Die vorliegenden Quellen decken diese Granularität **nicht vollständig** ab:

- **„none"** als explizite Stufe taucht in keiner Quelle auf. Was in den Benchmarks als Minimalstufe gemessen wird, heißt „low" bzw. „minimal effort" (Quelle 1).
- **„xhigh"** als Stufe taucht in keiner Quelle auf. Die höchste in den Daten vorkommende Stufe heißt „high" (Quelle 1) bzw. „max" (Quelle 3, nur begrifflich, ohne Daten).
- **Nur Quelle 1 (Digital Applied) misst direkt am Code-Editing-Use-Case** (Expert-SWE refactor, 60 Multi-File-Refactor-Tasks, Pass-Kriterium = vollständige Testsuite grün). Quelle 2 (FutureSearch) misst an Web-Recherche-Tasks, nicht an Code-Editing — die Ergebnisse lassen sich daher nur eingeschränkt auf Code-Editing übertragen.
- Quelle 5 (PDF zu Bundle Adjustment / Computer Vision) ist thematisch irrelevant und enthält keine Daten zur Frage.

---

## 1. Empirische Daten: Pass-Rate nach Effort-Stufe beim Code-Editing

Quelle 1, Tabelle „Expert-SWE refactor pass-rate" (60 Multi-File-Refactor-Tasks, pytest + Integration-Tests, Pass = komplette Testsuite grün, April 2026):

| Modell | Low | Medium | High |
|---|---|---|---|
| GPT-5.5 Pro | 58,7 % | **73,1 %** | 71,4 % |
| Claude Opus 4.7 | 54,1 % | 68,4 % | 69,8 % |
| Gemini 3 Pro DT | – | 63,9 % | – |
| DeepSeek V4 | – | 51,7 % | 56,3 % |
| Grok 4.5 | – | 59,6 % | – |

Quelle: Digital Applied Benchmark (Quelle 1, §03)

**Wichtige Beobachtung aus diesen Daten:** High effort bringt beim Code-Editing **nicht** den größten Qualitätssprung. Bei GPT-5.5 Pro bricht die Pass-Rate von Medium (73,1 %) auf High (71,4 %) sogar leicht ein (−1,7 Punkte). Bei Claude Opus 4.7 beträgt der Zugewinn Medium→High nur +1,4 Punkte. **Der steile Anstieg liegt immer zwischen Low und Medium** (z. B. GPT-5.5 Pro: +14,4 Punkte, Claude Opus 4.7: +14,3 Punkte). — Quelle 1, §03.

Bei anderen Domänen (Mathematik, analytisches Reasoning) sieht die Kurve anders aus: Low→High bringt dort 18–22 Punkte (Math, AIME 2026) bzw. 12–15 Punkte (GPQA Diamond) — aber diese Domänen sind nicht Code-Editing. — Quelle 1, §02 und §04.

---

## 2. Warum „mehr Reasoning" beim Code-Editing schaden kann

Quelle 1 dokumentiert einen konkreten Fehlermechanismus: „On 23 % of high-effort runs we observed over-engineered refactors — renaming functions across uninvolved modules, introducing abstractions the test suite did not require, breaking type signatures the integration tests depended on." (Quelle 1, §03)

Die Schlussfolgerung der Autoren: „Reasoning depth is a liability when the task is bounded by external constraints (existing tests, contracts, callers). Medium is the disciplined default." (Quelle 1, §03)

Konkretes Zitat: *„Math reasoning is where the dial pays its rent. Code reasoning is where the dial is misused."* — Internal eval retro, Mai 2026, zitiert in Quelle 1, §03.

---

## 3. Trade-off Kosten / Korrektheit

Quelle 1, §05, Cost-per-correct-answer auf AIME 2026 (Achtung: Math-Domäne, nicht Code, aber als Größenordnung relevant):

- DeepSeek V4 · high: 0,04 $ pro korrekter Antwort
- DeepSeek V4 · medium: 0,02 $
- Claude Opus 4.7 · high: 0,27 $
- GPT-5.5 Pro · low: 0,31 $
- GPT-5.5 Pro · medium: 0,42 $
- GPT-5.5 Pro · high: 0,78 $ (= 19× so teuer wie DeepSeek V4 high, bei nur +7,5 pp Pass-Rate)

Generell: „Cost-per-correct-answer changes the apparent ranking on every workload we tested. Per-token rate is the input, not the output." (Quelle 1, Key Takeaway 2)

Cost-Inflation High vs. Low über Modelle: Faktor 4–17×, im Schnitt/Gipfel 17× bei GPT-5.5 Pro. (Quelle 1, Header)

---

## 4. Trade-off Latenz

Quelle 1, §06, P50 Time-to-First-Token (TTFT):

| Stufe | TTFT P50 | Eignung |
|---|---|---|
| Minimal / Low | 0,4–1,5 s | Chat-UX, Autocomplete, Codemod |
| Medium | 4–12 s | Code-Refactor, Document-Analyse |
| High | 18–90 s | Batch, Async, Research |

Beispielwert: Claude Opus 4.7 mit Extended Thinking: P50 TTFT steigt von 0,8 s (low) auf 28 s (high) — Faktor ~35. (Quelle 1, Key Takeaway 3)

Gesamtspreizung über Modelle: TTFT-Inflation 5–60× bei high. (Quelle 1, Header)

---

## 5. Ab wann mehr Reasoning nichts (oder Negatives) bringt

Aus den Quellen lässt sich folgende „Diminishing-Returns"-Landkarte ableiten:

- **Beim Code-Editing (Expert-SWE refactor):** „Sweet spot" = **Medium**. Medium→High bringt 0 bis +1,7 Punkte; bei GPT-5.5 Pro ist der Sprung sogar negativ. Low→Medium bringt +14 bis +15 Punkte. → **Ab Medium bringt mehr Reasoning praktisch nichts; ab High kann es aktiv schaden.** (Quelle 1, §03)
- **Beim Math (AIME 2026):** High lohnt sich. Low→High bringt 18–22 Punkte; „the compute pays for itself in verifiable correctness". → **Ab High noch marginaler Zugewinn, aber keine Regression beobachtet.** (Quelle 1, §02)
- **Beim analytischen Reasoning (GPQA Diamond):** Medium-High-Band; Medium→High bringt nur noch +3–7 Punkte. (Quelle 1, §04)
- **Bei Web-Recherche (FutureSearch, 150+ Tasks):** **Mehr Reasoning kann die Korrektheit aktiv senken.** GPT-5 zeigt monoton fallende Scores (49,6 % → 48,6 % → 48,1 %) bei steigenden Kosten; Gemini 3 Flash fällt von 49,9 % auf 47,9 %; nur Claude 4.6 Opus verbessert sich (+1,9 pp) bei +128 % Kosten. (Quelle 2) — Achtung: Dies ist Recherche, nicht Code-Editing, illustriert aber das generelle Phänomen.

Zentrale Empfehlung aus Quelle 1 (Key Takeaway 5): *„The most common mistake is defaulting every reasoning workload to high effort because it sounds safer. … will land most workflows at low or medium and 4–12× cheaper than the default."*

---

## 6. Zusätzliche Hinweise zu Stufen und Parametrisierung

- **OpenAI (o1/o3/o3-mini):** offiziell drei Stufen — low / medium / high. „Higher reasoning_effort = More Explanation". Tokenverbrauch im gezeigten Beispiel (Antwort auf „Why is the sky blue?") bei o3-mini: low 148 Tokens, medium 471, high 898 — Faktor ~6 von low zu high. (Quelle 4)
- **Claude Code:** vier Stufen — low / medium / high / max. „Higher effort means more internal reasoning; lower effort means the model responds more directly from pattern recognition." (Quelle 3) — Quelle 3 nennt die vier Stufen, liefert **keine empirischen Zahlen**.
- **Vendor-Parameter-Mapping** (alle auf „Tier" normalisiert): OpenAI = `reasoning_effort`; Anthropic = Extended-Thinking-Budget; Google = `thinking_budget` (Deep Think); xAI Grok = `reasoning_mode`; DeepSeek = interner CoT-Toggle. (Quelle 1, §01)

---

## 7. Was die Quellen NICHT abdecken

Explizit nicht belegt durch die vorliegenden Quellen:

- **„none"-Stufe** (kein Reasoning) — in keiner Quelle als Mess-Stufe definiert.
- **„xhigh"-Stufe** — in keiner Quelle erwähnt; höchste gemessene Stufe ist „high" (Quelle 1) bzw. „max" begrifflich (Quelle 3).
- **Spezifische Daten für andere Code-Editing-Aufgaben** jenseits von Multi-File-Refactor (z. B. Inline-Edit, Bug-Fix, Feature-Add, Test-Generation) — Expert-SWE refactor ist der einzige direkte Code-Benchmark in den Quellen.
- **Out-of-distribution-Robustheit** (z. B. unbekannte Codebases, fremde Sprachen) — nicht behandelt.
- **Inter-Run-Varianz** über die 3 Runs pro Zelle (Quelle 1 erwähnt 3-Run-Mehrheitsvotum, publiziert aber keine Varianz).
- **Kombinationseffekte** (Effort × Modell × Sprache × Repo-Größe) — nicht systematisch aufgeschlüsselt.


---

## [B] E1. MiMo-V2.5-Pro vs GLM-5.2 direkt (Eskalation)

Ich werde parallel nach beiden Modellen suchen, um aktuelle Informationen zu finden.Ich habe erste Informationen. Jetzt suche ich tiefer nach spezifischen Details zu Benchmarks, Instruction Following, Fehlerraten und Reasoning.Ich habe bereits viele Daten. Ich suche nun noch nach direkten Vergleichen und einigen spezifischen Details.# Direkter Vergleich: Xiaomi MiMo-V2.5-Pro vs. Z.ai GLM-5.2

## ⚠️ Wichtige Vorab-Hinweise zur Datenlage

- **Beide Modelle sind brandneu (2026)**: MiMo-V2.5-Pro wurde am **22. April 2026** veröffentlicht ([OpenRouter](https://openrouter.ai/xiaomi/mimo-v2.5-pro)), GLM-5.2 im **Februar 2026** ([Together AI](https://www.together.ai/models/glm-52)).
- **Es gibt keinen direkten Head-to-Head-Benchmark** der beiden Modelle auf identischen Evaluations — ich stelle die selbstberichteten Zahlen der jeweiligen Hersteller gegenüber, was **methodisch problematisch** ist (unterschiedliche Harnesses, Prompt-Versionen, Test-Splits).
- **Benchmarks wie SWE-bench Pro und Terminal-Bench existieren in mehreren Versionen** (2.0, 2.1, Terminus-2, "Best Reported Harness"), was direkten Vergleich weiter erschwert.
- Für **Fehlerrate** und **Instruction-Following** habe ich nur **partielle Daten** gefunden — ich markiere die Lücken klar.

---

## 1. Architektur & Grunddaten

| Merkmal | MiMo-V2.5-Pro | GLM-5.2 |
|---|---|---|
| **Hersteller** | Xiaomi ([OpenRouter](https://openrouter.ai/xiaomi/mimo-v2.5-pro)) | Z.ai / Zhipu AI ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog)) |
| **Architektur** | MoE (Details nicht in den Quellen) | 744B Gesamt / 40B aktiv, MoE ([Together AI](https://www.together.ai/models/glm-52)) |
| **Kontext-Fenster** | bis zu **1M Tokens** ([OpenRouter](https://openrouter.ai/xiaomi/mimo-v2.5-pro)) | **1M Tokens** nutzbar (5× größer als GLM-5.1 mit ~200K); 131.072 Token Output-Cap ([Together AI](https://www.together.ai/models/glm-52)) |
| **Lizenz** | Open Weights auf HuggingFace ([HuggingFace](https://huggingface.co/XiaomiMiMo/MiMo-V2.5-Pro)) | MIT-Lizenz, Open Weights ([Together AI](https://www.together.ai/models/glm-52)) |
| **Preis (Input/Output)** | **$1 / $3** pro 1M Tokens (OpenRouter) ([OpenRouter](https://openrouter.ai/xiaomi/mimo-v2.5-pro)) | ca. **$2 / $6** pro 1M Tokens (laut MindStudio) ([MindStudio](https://www.mindstudio.ai/blog/glm-5-2-vs-gpt-5-5-vs-claude-opus-agentic-workflows)) |
| **Releasedatum** | 22. April 2026 ([OpenRouter](https://openrouter.ai/xiaomi/mimo-v2.5-pro)) | Februar 2026 ([Together AI](https://www.together.ai/models/glm-52)) |
| **Thinking/Reasoning-Modus** | Nicht explizit in den Quellen als "Effort-Level" dokumentiert | **Zwei Effort-Level** (Default + Max) ([Z.ai Blog](https://z.ai/blog/glm-5.2)) |

---

## 2. Coding-Benchmarks (selbstberichtet)

### SWE-bench
| Benchmark | MiMo-V2.5-Pro | GLM-5.2 | Anmerkung |
|---|---|---|---|
| **SWE-bench Verified** | **78.9** (Xiaomi-Default) / **82%** (mit eigenem MiMo Code Harness) ([the-decoder](https://the-decoder.com/xiaomis-open-weight-mimo-v2-5-pro-takes-aim-at-claude-opus-with-hours-long-autonomous-coding/); [VentureBeat](https://venturebeat.com/technology/xiaomis-new-open-source-agentic-ai-coding-harness-mimo-code-beats-claude-code-at-ultra-long-200-step-tasks)) | in HF-Tabelle **nicht direkt gelistet** | MiMo Code-Harness bringt ca. **+3 Punkte** gegenüber dem Default-Harness ([VentureBeat](https://venturebeat.com/technology/xiaomis-new-open-source-agentic-ai-coding-harness-mimo-code-beats-claude-code-at-ultra-long-200-step-tasks)) |
| **SWE-bench Pro** | **57.2** (Default) / **62%** (mit MiMo Code) ([the-decoder](https://the-decoder.com/xiaomis-open-weight-mimo-v2-5-pro-takes-aim-at-claude-opus-with-hours-long-autonomous-coding/); [VentureBeat](https://venturebeat.com/technology/xiaomis-new-open-source-agentic-ai-coding-harness-mimo-code-beats-claude-code-at-ultra-long-200-step-tasks)) | **62.1** ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog); [VentureBeat](https://venturebeat.com/technology/z-ais-open-weights-glm-5-2-beats-gpt-5-5-on-multiple-long-horizon-coding-benchmarks-for-1-6th-the-cost)) | Direkter Vergleichbarkeit unklar — GLM-5.2 schlägt **GPT-5.5 (58.6)** und **GLM-5.1 (58.4)** ([VentureBeat](https://venturebeat.com/technology/z-ais-open-weights-glm-5-2-beats-gpt-5-5-on-multiple-long-horizon-coding-benchmarks-for-1-6th-the-cost)) |

### Terminal-Bench
| Benchmark | MiMo-V2.5-Pro | GLM-5.2 |
|---|---|---|
| **Terminal-Bench 2.0** | **68.4** ([the-decoder](https://the-decoder.com/xiaomis-open-weight-mimo-v2-5-pro-takes-aim-at-claude-opus-with-hours-long-autonomous-coding/)) | — |
| **Terminal-Bench 2.1 (Terminus-2)** | — | **81.0** ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog); [GitHub](https://github.com/zai-org/GLM-5)) |
| **Terminal-Bench 2.1 (Best Harness)** | — | **82.7** ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog)) |
| **Terminal Bench 2 (MiMo Code)** | **73%** ([VentureBeat](https://venturebeat.com/technology/xiaomis-new-open-source-agentic-ai-coding-harness-mimo-code-beats-claude-code-at-ultra-long-200-step-tasks)) | — |

**Achtung**: Unterschiedliche Terminal-Bench-Versionen (2.0 vs 2.1) sind **nicht vergleichbar**.

### Long-Horizon Coding-Benchmarks (hauptsächlich GLM-5.2 dokumentiert)
| Benchmark | GLM-5.2 | GLM-5.1 | Vergleich zu Closed Source |
|---|---|---|---|
| **FrontierSWE (Dominance)** | **74.4%** | 30.5% | **+1.8%** vs GPT-5.5 (72.6%), **−0.7%** vs Claude Opus 4.8 (75.1%) ([VentureBeat](https://venturebeat.com/technology/z-ais-open-weights-glm-5-2-beats-gpt-5-5-on-multiple-long-horizon-coding-benchmarks-for-1-6th-the-cost)) |
| **PostTrainBench** | **34.3** | 20.1 | nur Opus 4.8 (37.2) höher; GLM-5.2 > GPT-5.5 (28.4) > Gemini 3.1 Pro (21.6) ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog)) |
| **SWE-Marathon** | **13.0** | 1.0 | Opus 4.8: 26.0, GPT-5.5: 12.0 ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog)) |

Für **MiMo-V2.5-Pro** sind diese Long-Horizon-Benchmarks in den gefundenen Quellen **nicht ausgewiesen** — Lücke.

### Weitere Coding/Agent-Benchmarks
| Benchmark | MiMo-V2.5-Pro | GLM-5.2 |
|---|---|---|
| **MiMo Coding Bench (intern)** | **73.7** — knapp hinter Opus 4.6 (77.1), vor Gemini 3.1 Pro (67.8) ([the-decoder](https://the-decoder.com/xiaomis-open-weight-mimo-v2-5-pro-takes-aim-at-claude-opus-with-hours-long-autonomous-coding/)) | — |
| **Tau3-Bench** | **72.9** ([the-decoder](https://the-decoder.com/xiaomis-open-weight-mimo-v2-5-pro-takes-aim-at-claude-opus-with-hours-long-autonomous-coding/)) | — |
| **Claw-Eval** | **63.8** ([tosea.ai](https://tosea.ai/blog/mimo-v2-5-pro-complete-guide)) | — |
| **ProgramBench** | — | **63.7** ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog)) |
| **MCP-Atlas (Public Set)** | — | **76.8** ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog)) |

---

## 3. Reasoning/Thinking-Fähigkeit

### GLM-5.2
- **Explizite Effort-Level-Steuerung** (Default + Max) — Anwender können Compute/Tiefe gegen Geschwindigkeit/Kosten abwägen ([Z.ai Blog](https://z.ai/blog/glm-5.2); [Together AI](https://www.together.ai/models/glm-52)).
- **Massive Reasoning-Tokens bei "Max"-Effort**: Ein HN-Nutzer berichtet, GLM-5.2 (xhigh/Max) habe für einen 400–600 Zeilen-Nim-Evaluator **>15 Minuten** und **~45.000 Reasoning-Tokens** verbraucht, bevor es die erste Datei schrieb ([Hacker News](https://news.ycombinator.com/item?id=48567759)).
- **Token-Vergleich (gleiche Aufgabe, laut HN-Nutzer)**: GPT-5.5 high = 10k, GPT-5.5 xhigh = 16k, Fable 5 = 33k, Opus 4.8 = 41k, **GLM-5.2 = 42k** ([Hacker News](https://news.ycombinator.com/item?id=48567759)).
- **Reasoning-Benchmarks**: HLE 40.5 (ohne Tools) / **54.7** (mit Tools), CritPt 16.7 ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog)).

### MiMo-V2.5-Pro
- Quelle [Tosea.ai](https://tosea.ai/blog/mimo-v2-5-pro-complete-guide) bestätigt **HLE = 48.0**.
- Quelle [MiMo Docs](https://mimo.mi.com/docs/en-US/updates/model) berichtet für den Vorgänger MiMo-V2-Flash: "Thinking Mode" mit **Tool-Calling-Accuracy von 64% → 97.0%** durch Upgrade. Ob das auch für V2.5-Pro gilt, wird in den gefundenen Quellen **nicht explizit gesagt** — **Lücke**.
- Reasoning-Modi / Effort-Level bei MiMo-V2.5-Pro sind in den gefundenen Quellen **nicht dokumentiert** — **Lücke**.

### Subjektive Bewertung (HN-Diskussion)
- HN-Nutzer: GLM-5.2 macht "way fewer StackOverflow brogrammer-tier mistakes and is considerably better at following instructions" als GLM-5.1 ([Hacker News](https://news.ycombinator.com/item?id=48567759)).
- Allerdings: GLM-5.2 ist **nicht reasoning-effizient** im Vergleich zu GPT-5.5 ([Hacker News](https://news.ycombinator.com/item?id=48567759)).

---

## 4. Tool-Use & Agentic Capabilities

### MiMo-V2.5-Pro
- Xiaomi bewirbt die Integration mit **Claude Code, OpenCode, Kilo** als Coding-Agents ([mimo.xiaomi.com](https://mimo.xiaomi.com/mimo-v2-5-pro/)).
- **Real-World Demo**: 8.192 Zeilen Code für einen Video-Editor, **1.868 Tool Calls**, **11,5 Stunden autonome Arbeit** ([mimo.xiaomi.com](https://mimo.xiaomi.com/mimo-v2-5-pro/)).
- **Real-World Demo 2**: Analog-EDA FVF-LDO Design: ngspice-Simulator-Schleife mit Claude Code als Harness, ca. 1 Stunde geschlossene Iteration ([mimo.xiaomi.com](https://mimo.xiaomi.com/mimo-v2-5-pro/)).
- **MiMo Code (eigener Open-Source-Harness)**: Xiaomi hat einen eigenen Agentic-Harness veröffentlicht, der mit MiMo-V2.5-Pro gegen Claude Code gewinnt — **+5 Punkte** auf SWE-bench Pro und Terminal Bench 2 ([VentureBeat](https://venturebeat.com/technology/xiaomis-new-open-source-agentic-ai-coding-harness-mimo-code-beats-claude-code-at-ultra-long-200-step-tasks)).
- **GDPVal-AA (Elo)**: **1.581** Elo-Punkte ([the-decoder](https://the-decoder.com/xiaomis-open-weight-mimo-v2-5-pro-takes-aim-at-claude-opus-with-hours-long-autonomous-coding/)).
- **Long-Context-Tool-Use**: GraphWalks bei 1M Tokens — BFS = 0.37, Parent-Queries = 0.62 (Vorgänger V2-Pro fiel auf 0) ([the-decoder](https://the-decoder.com/xiaomis-open-weight-mimo-v2-5-pro-takes-aim-at-claude-opus-with-hours-long-autonomous-coding/)).
- **Kilo Code / PinchBench**: MiMo-V2.5-Pro wird auf PinchBench für OpenClaw-Agent-Tasks evaluiert (Multi-Step-Execution, Tool-Use, Recovery, Latency, Cost) ([Kilo Code](https://kilo.ai/models/xiaomi-mimo-v2-5-pro)).

### GLM-5.2
- **MCP-Atlas (Public Set)**: 76.8 — Tool-Use über MCP-Protokoll, +5 Punkte vs GLM-5.1 (71.8) ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog)).
- **"Agentic Engineering"-Paradigma**: GLM-5 wurde explizit für den Übergang von "Vibe Coding" zu "Agentic Engineering" trainiert ([GitHub](https://github.com/zai-org/GLM-5); [arXiv-Paper](https://arxiv.org/html/2602.15763v2)).
- **Asynchrones RL-Framework** speziell für Agentic-Performance optimiert ([arXiv-Paper](https://arxiv.org/html/2602.15763v2)).
- **Day-one Drop-in-Kompatibilität** mit gängigen Coding-Agents ([Together AI](https://www.together.ai/models/glm-52)).
- **Preis-Leistung**: GLM-5.2 wird mit "1/6 der Kosten" gegenüber GPT-5.5 beworben ([VentureBeat](https://venturebeat.com/technology/z-ais-open-weights-glm-5-2-beats-gpt-5-5-on-multiple-long-horizon-coding-benchmarks-for-1-6th-the-cost)).

---

## 5. Fehlerrate

⚠️ **Direkte, vergleichbare Fehlerraten-Daten habe ich NICHT gefunden.** Was ich finden konnte:

- **MiMo-V2.5-Pro**: Keine Fehlerraten-Statistiken in den Quellen. Vorgänger V2-Flash hatte Tool-Calling-Accuracy-Probleme (64% → 97% nach Update) ([MiMo Docs](https://mimo.mi.com/docs/en-US/updates/model)).
- **GLM-5.2**: 
  - Subjektive HN-Aussage: "way fewer mistakes" als GLM-5.1 ([Hacker News](https://news.ycombinator.com/item?id=48567759)).
  - **AA-Omniscience Non-Hallucination Rate** ist in Artificial Analysis für MiMo-V2.5-Pro gelistet (kein konkreter Wert in den Snippets) ([Artificial Analysis](https://artificialanalysis.ai/models/mimo-v2-5-pro)).
  - GLM-5-Paper erwähnt explizit "instruction-following failures, logical [errors]" als zu evaluierende Fehlertypen ([arXiv-Paper](https://arxiv.org/html/2602.15763v2)) — konkrete Zahlen für GLM-5.2 aber **nicht extrahiert**.

---

## 6. Instruction-Following

⚠️ **Nur partielle Daten:**

| Metrik | MiMo-V2.5-Pro | GLM-5.2 |
|---|---|---|
| **AA-IFBench (Artificial Analysis)** | Wird in AA evaluiert ([Artificial Analysis](https://artificialanalysis.ai/models/mimo-v2-5-pro)), **Wert nicht in den Quellen-Snippets** | **Nicht direkt gefunden** |
| **Vorgänger MiMo-V2-Flash AAI-IFBench** | **72** ([MiMo Docs](https://mimo.mi.com/docs/en-US/updates/model)) | — |
| **Non-Hallucination Rate (V2-Flash)** | **52%** ([MiMo Docs](https://mimo.mi.com/docs/en-US/updates/model)) | — |
| **Subjektiv (HN-Nutzer)** | — | "considerably better at following instructions" als GLM-5.1 ([Hacker News](https://news.ycombinator.com/item?id=48567759)) |
| **Paper-Dokumentation** | — | GLM-5-Paper: explizite Evaluations-Dimension "instruction following" mit definierten Metriken ([arXiv-Paper](https://arxiv.org/html/2602.15763v2)) — konkrete Zahl für 5.2 nicht extrahiert |

---

## 7. Zusammenfassung — Wer gewinnt wo?

| Kategorie | Vorteil … |
|---|---|
| **SWE-bench Pro** (roh) | **GLM-5.2 (62.1)** > MiMo-V2.5-Pro (57.2 im Default-Harness); mit MiMo-Code-Harness aber MiMo-V2.5-Pro (62%) ≈ GLM-5.2 ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog); [VentureBeat](https://venturebeat.com/technology/xiaomis-new-open-source-agentic-ai-coding-harness-mimo-code-beats-claude-code-at-ultra-long-200-step-tasks)) |
| **Terminal-Bench** | **GLM-5.2 (81.0)** deutlich über MiMo-V2.5-Pro (68.4, Terminal-Bench 2.0) ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog); [the-decoder](https://the-decoder.com/xiaomis-open-weight-mimo-v2-5-pro-takes-aim-at-claude-opus-with-hours-long-autonomous-coding/)) |
| **Long-Horizon (FrontierSWE, PostTrainBench, SWE-Marathon)** | **GLM-5.2 klar dokumentiert** — MiMo-V2.5-Pro fehlt hier ([Hugging Face Blog](https://huggingface.co/blog/zai-org/glm-52-blog)) |
| **Real-World-Demos (Tool-Calls, Stunden-lange Autonomie)** | **MiMo-V2.5-Pro sehr stark dokumentiert** (1.868 Tool Calls, 11,5h Video-Editor) ([mimo.xiaomi.com](https://mimo.xiaomi.com/mimo-v2-5-pro/)) |
| **Preis** | **MiMo-V2.5-Pro** ($1/$3) günstiger als GLM-5.2 ($2/$6) ([OpenRouter](https://openrouter.ai/xiaomi/mimo-v2.5-pro); [MindStudio](https://www.mindstudio.ai/blog/glm-5-2-vs-gpt-5-5-vs-claude-opus-agentic-workflows)) |
| **Reasoning-Effizienz (Tokens)** | Beide "teuer" — GLM-5.2 mit ~42k Tokens, MiMo-V2.5-Pro nicht gemessen ([Hacker News](https://news.ycombinator.com/item?id=48567759)) |
| **Konfiguration / Steuerbarkeit** | **GLM-5.2** mit expliziten Effort-Leveln ([Z.ai Blog](https://z.ai/blog/glm-5.2)) — bei MiMo-V2.5-Pro nicht dokumentiert |

---

## 8. Offene Fragen / Datenlücken

Ich habe **keine** verlässlichen Daten zu:
1. Direkter Head-to-Head-Benchmark **derselben Harness-Version** zwischen beiden Modellen.
2. Quantitativer **Fehlerrate** (z. B. Task-Completion-Failure-Rate) für MiMo-V2.5-Pro.
3. **AA-IFBench-Wert** für GLM-5.2.
4. Reasoning/Thinking-Modi für MiMo-V2.5-Pro.
5. Token-Effizienz von MiMo-V2.5-Pro (vergleichbar zu GLM-5.2's 42k).
6. FrontierSWE / PostTrainBench / SWE-Marathon für MiMo-V2.5-Pro.

---

## Quellen-Liste (vollständig)

1. **MiMo-V2.5-Pro (Xiaomi)**:
   - https://mimo.xiaomi.com/mimo-v2-5-pro/
   - https://openrouter.ai/xiaomi/mimo-v2.5-pro
   - https://huggingface.co/XiaomiMiMo/MiMo-V2.5-Pro
   - https://the-decoder.com/xiaomis-open-weight-mimo-v2-5-pro-takes-aim-at-claude-opus-with-hours-long-autonomous-coding/
   - https://tosea.ai/blog/mimo-v2-5-pro-complete-guide
   - https://venturebeat.com/technology/xiaomis-new-open-source-agentic-ai-coding-harness-mimo-code-beats-claude-code-at-ultra-long-200-step-tasks
   - https://kilo.ai/models/xiaomi-mimo-v2-5-pro
   - https://artificialanalysis.ai/models/mimo-v2-5-pro
   - https://mimo.mi.com/docs/en-US/updates/model
   - https://www.datacamp.com/tutorial/vibe-coding-with-xiaomi-mimo-v2-5-pro

2. **GLM-5.2 (Z.ai)**:
   - https://z.ai/blog/glm-5.2
   - https://huggingface.co/blog/zai-org/glm-52-blog
   - https://github.com/zai-org/GLM-5
   - https://www.together.ai/models/glm-52
   - https://venturebeat.com/technology/z-ais-open-weights-glm-5-2-beats-gpt-5-5-on-multiple-long-horizon-coding-benchmarks-for-1-6th-the-cost
   - https://arxiv.org/html/2602.15763v2 (GLM-5 Paper)
   - https://www.mindstudio.ai/blog/glm-5-2-vs-gpt-5-5-vs-claude-opus-agentic-workflows
   - https://news.ycombinator.com/item?id=48567759 (HN-Diskussion)

=== Web-Quellen ===
- MiMo-V2.5-Pro | Xiaomi — https://mimo.xiaomi.com/mimo-v2-5-pro/
- Xiaomi: MiMo-V2.5-Pro Coding Benchmark - Kilo Code — https://kilo.ai/models/xiaomi-mimo-v2-5-pro
- MiMo-V2.5-Pro - API Pricing & Benchmarks | OpenRouter — https://openrouter.ai/xiaomi/mimo-v2.5-pro
- Xiaomi's new open source, agentic AI coding harness MiMo Code beats Claude Code at ultra-long, 200+ step tasks | VentureBeat — https://venturebeat.com/technology/xiaomis-new-open-source-agentic-ai-coding-harness-mimo-code-beats-claude-code-at-ultra-long-200-step-tasks
- description: Connect Xiaomi MiMo-V2.5-Pro to OpenCode with Olostep MCP and the MiMo Token Plan, then put it to the test by building a CLI tool and a Reflex web app. image: https://media.datacamp.com/cms/gemini_generated_image_loqyg2loqyg2loqy.png title: Vibe Coding with Xiaomi MiMo-V2.5-Pro: A Hands-On Tutorial — https://www.datacamp.com/tutorial/vibe-coding-with-xiaomi-mimo-v2-5-pro
- 
		Z.ai - Advanced AI Chatbot & Agent powered by GLM-5.2
	 — http://Z.ai
- GLM-5.2: Built for Long-Horizon Tasks - Z.ai — https://z.ai/blog/glm-5.2
- GitHub - zai-org/GLM-5: GLM-5: From Vibe Coding to Agentic Engineering · GitHub — https://github.com/zai-org/GLM-5
- Z.ai’s open-weights GLM-5.2 beats GPT-5.5 on multiple long-horizon coding benchmarks for 1/6th the cost | VentureBeat — https://venturebeat.com/technology/z-ais-open-weights-glm-5-2-beats-gpt-5-5-on-multiple-long-horizon-coding-benchmarks-for-1-6th-the-cost
- New Agentic Benchmark Out: Claude Fable and GLM 5.2 ... — https://www.reddit.com/r/LocalLLaMA/comments/1u9yt6v/new_agentic_benchmark_out_claude_fable_and_glm_52/
- Xiaomi's open-weight MiMo-V2.5-Pro takes aim at Claude Opus with hours-long autonomous coding — https://the-decoder.com/xiaomis-open-weight-mimo-v2-5-pro-takes-aim-at-claude-opus-with-hours-long-autonomous-coding/
- Measuring the performance of our models on real-world tasks  | OpenAI — https://openai.com/index/gdpval/
- MiMo-V2-Flash Technical Report — https://arxiv.org/pdf/2601.02780
- How to Use MiMo-V2.5-Pro: Complete Guide to Xiaomi's New 1T MoE Model in 2026 | Tosea.ai — https://tosea.ai/blog/mimo-v2-5-pro-complete-guide
- GLM-5.2: Built for Long-Horizon Tasks - Hugging Face — https://huggingface.co/blog/zai-org/glm-52-blog
- GLM 5.2 beats Claude Fable 5 : GLM 5.2 Benchmarks ... — https://medium.com/data-science-in-your-pocket/glm-5-2-beats-claude-fable-5-glm-5-2-benchmarks-explained-493751c8a24f
- glm-5.2 hits 81.0 on terminal-bench 2.1, within a few points of claude opus 4.8, and it's open source. the 1m context that actually holds up under long agentic sessions is the part worth paying attention to.

https://github.com/zai-org/GLM-5 — https://www.threads.com/@suritech/post/DZsHbWSmkVz/glm-hits-on-terminal-bench-within-a-few-points-of-claude-opus-and-its-open/
- GLM-5.2 model beats GPT-5.5 on coding benchmarks - Facebook — https://www.facebook.com/groups/artificialintelligence.lt/posts/27484576751176135/
- I Tested NEW GLM-5.2 on Four Projects. Wow. - YouTube — https://www.youtube.com/watch?v=YM2Sk7wEmi0
- MiMo-V2.5-Pro - Intelligence, Performance & Price Analysis — https://artificialanalysis.ai/models/mimo-v2-5-pro
- MiMo-V2-Pro | Xiaomi — https://mimo.xiaomi.com/mimo-v2-pro
- Xiaomi MiMo Home — https://mimo.mi.com/docs/en-US/updates/model
- GLM-5.2 is the new leading open weights model on Artificial Analysis | Hacker News — https://news.ycombinator.com/item?id=48567759
- GLM-5: from Vibe Coding to Agentic Engineering — https://arxiv.org/html/2602.15763v2
- description: Flagship MoE model (744B total / 40B activated) with a 1M context window, 131K output cap, two thinking-effort levels, and day-one drop-in compatibility for major coding agents. title: GLM-5.2 API | Together AI image: https://cdn.prod.website-files.com/69654e88dce9154b5f12070c/6a319d613f79826259d19b88_webflow-9d1eb7a2ed92d532ec5f5ac1d5ba0a23-ce5gyufu.jpeg — https://www.together.ai/models/glm-52
- GLM 5.0 Fixes for unreliable, low effort thinking, instruction ... - Reddit — https://www.reddit.com/r/SillyTavernAI/comments/1rb6be6/glm_50_fixes_for_unreliable_low_effort_thinking/
- GLM 5.2 vs GPT 5.5 vs Claude Opus 4.8: Which Model Wins for Agentic Workflows? | MindStudio — https://www.mindstudio.ai/blog/glm-5-2-vs-gpt-5-5-vs-claude-opus-agentic-workflows


---

## [B] E2. xhigh vs high beim Code-Editing (Eskalation)

# Recherche: xhigh vs. high bei Reasoning-Effort-Stufen für agentisches Coding

## Wichtige Vorab-Anmerkung zur Quellenlage

Beim Recherchieren sind mir mehrere Quellen mit Datum **April 2026** untergekommen (Towards-AI-Artikel, Nipralo-Blog, OckBench-Tabelle mit "GPT-5.5"/"GPT-5.4"/"Claude-Opus-4.7"/"Claude Fable 5"). Da das Training des Modells einen Wissenscutoff hat und mir das aktuelle Datum nicht bestätigt vorliegt, kann ich nicht garantieren, dass es sich hier um reale oder fiktive/future-dated Quellen handelt. Ich gebe im Folgenden nur das wieder, was tatsächlich in den gefundenen Texten steht — ohne Annahmen.

---

## 1. Was die Quellen zum xhigh/xhigh-vs-high-Verhältnis sagen

### Anthropic API-Dokumentation (Claude Opus 4.7)
Quelle: https://platform.claude.com/docs/en/build-with-claude/effort

Dort ist xhigh definiert als *"Extended capability for long-horizon work"* und ausdrücklich empfohlen für *"Long-running agentic and coding tasks (over 30 minutes) with token budgets in the millions"*. Für Opus 4.7 nennt Anthropic die Stufen:

| Stufe | Empfehlung |
|-------|-----------|
| `high` | "Often the sweet spot balancing quality and token efficiency" |
| `xhigh` | "The recommended starting point for coding and agentic work, and for exploratory tasks … Expect meaningfully higher token usage than `high`." |
| `max` | "Reserve for genuinely frontier problems. On most workloads `max` adds significant cost for relatively small quality gains, and on some structured-output or less intelligence-sensitive tasks it can lead to overthinking." |

→ Die **Herstellerempfehlung** ist also klar: für Coding/Agentic `high` oder `xhigh` als Startpunkt, `max` nur für Frontier-Probleme.

### Anthropic Launch-Post zu Claude Opus 4.7
Quelle: https://www.anthropic.com/news/claude-opus-4-7

> "Opus 4.7 introduces a new `xhigh` ('extra high') effort level between `high` and `max`, giving users finer control over the tradeoff between reasoning and latency on hard problems. In Claude Code, we've raised the default effort level to `xhigh` for all plans. When testing Opus 4.7 for coding and agentic use cases, we recommend starting with `high` or `xhigh` effort."

---

## 2. Empirische Daten: OckBench (arxiv 2511.05722)

Quelle: https://arxiv.org/html/2511.05722v3 und https://github.com/OckBench/OckBench

OckBench misst **Accuracy und Token-Effizienz** zusammen (sog. *OckScore*). Aus der Tabelle (A.5 Full Leaderboard):

| Modell / Setting | Coding Acc. | Coding Tokens | Overall Acc. | OckScore |
|---|---|---|---|---|
| GPT-5.5 (medium) | 96.7 % | 1.739 | 86.0 | **82.2** |
| GPT-5.5 (high) | 100.0 % | 2.853 | 88.5 | 81.6 |
| GPT-5.5 (xhigh) | 100.0 % | **4.501** | 90.0 | 81.6 |
| Claude-Opus-4.7 (default) | 85.0 % | 1.878 | 83.0 | 77.4 |

**Befund xhigh vs. high (gleiches Modell GPT-5.5):**
- Coding-Accuracy: 100.0 % → 100.0 % (**kein Unterschied**)
- Coding-Tokens: 2.853 → 4.501 (**+58 %** Tokenverbrauch)
- Overall-Accuracy: 88.5 → 90.0 (+1.5 Punkte)
- **OckScore: 81.6 → 81.6 (identisch)** — d.h. der Accuracy-Gewinn wird durch den höheren Tokenverbrauch exakt aufgehoben.

Interessant: **medium hat den höchsten OckScore (82.2)** für GPT-5.5 — der efficiency-adjusted beste Setting ist nicht xhigh.

---

## 3. Empirische Daten: Towards-AI-Test "I Tested All 5 Effort Levels of Claude Opus 4.7"

Quelle: https://pub.towardsai.net/i-tested-all-5-effort-levels-of-claude-opus-4-7-2f335c626786

Der Autor Chew Loong Nian hat **48 Stunden lang 12 Coding-Probleme** durch alle 5 Effort-Stufen gejagt. Titel und Kernaussage:

> *"The 'Max' Setting Is a Trap (And 'Low' Quietly Killed Opus 4.6)"*

Sein Argument (sinngemäß aus dem sichtbaren Teaser-Text): Da Claude Code den Default still auf `xhigh` gesetzt hat, prüft er, ob das gerechtfertigt ist. Vollständige Zahlen pro Stufe sind im sichtbaren Snippet nicht enthalten — die Quelle betont aber, dass `xhigh` als neuer Default und damit als *de facto* Maßstab für Coding-Agents gilt.

---

## 4. Empirische Daten: "Coding Models Are Doing Too Much" (Minimal-Editing-Analyse)

Quelle: https://nrehiew.github.io/blog/minimal_editing/

Untersucht, ob Reasoning (= höhere Reasoning-Effort) zu **Over-Editing** führt (Levenshtein-Distance zwischen Original und Edit):

> "In the generic setting, reasoning models over-edit more than their non-reasoning counterparts in the majority of pairs. DeepSeek V3, GPT-5, GPT-5.4, Gemini 3.1 Pro Preview, Qwen 3.6 Plus, and Kimi 2.5 all show the reasoning bar sitting higher. Reasoning models seems to naturally have more elaborate rewrites where the model reasons its way into a 'better' implementation rather than a minimal fix."

> "In the explicit setting (… 'preserve the original code'), reasoning models have much lower Levenshtein Distance than their non-reasoning counterparts and match or undercut them in almost every pair."

→ Höhere Reasoning-Effort korreliert empirisch mit mehr Over-Editing, **wenn nicht explizit zur Minimalität angewiesen wird**.

---

## 5. Hacker-News-Diskussion (anekdotisch, aber relevant)

Quelle: https://news.ycombinator.com/item?id=47866913

> "Over-editing is definitely not some long gone problem. This was on xhigh thinking, because I forgot to set it to lower."

Mehrere Entwickler berichten, dass Over-Editing unter `xhigh` weiterhin real ist.

---

## 6. Cost-vs-Quality-Analyse (Digital Applied)

Quelle: https://www.digitalapplied.com/blog/reasoning-effort-cost-vs-quality-benchmarks-2026

> "The cost-quality crossover is *task-specific*: high effort wins AIME, medium wins Expert-SWE refactor, low wins PR-scale review."

Kernaussage: **Es gibt keine universelle "höher = besser"-Regel**; der optimale Effort hängt vom Task-Typ ab.

---

## 7. Was FEHLT oder widersprüchlich ist

- **Direkter xhigh-vs-high-A/B-Vergleich** auf Coding-Benchmarks (SWE-bench, Terminal-Bench, CursorBench o.ä.) mit konkreten Prozentpunkten **für xhigh** konnte ich nicht finden — die OckBench-Zahlen sind die einzige direkte Quelle. SWE-bench-Werte in https://nipralo.com/blogs/best-ai-coding-tools-2026 (z.B. SWE-bench Verified 80.8→87.6 %) werden Opus 4.7 generell zugeschrieben, **nicht** speziell dem xhigh-Setting.
- **Der vollständige Towards-AI-Artikel mit den 12-Problems-Ergebnissen pro Effort-Stufe** war in den Snippets nicht komplett zugänglich — ich sehe nur Titel/Teaser, keine Prozentzahl-Tabellen.
- Die **Anthropic-Empfehlung "xhigh als Default in Claude Code"** steht im Widerspruch zur OckBench-Observation, dass `medium` einen höheren OckScore als `xhigh`/`high` hat. Die zwei Quellen messen aber unterschiedliche Dinge (Anthropic: Spitzenqualität auf schweren Agentic-Tasks; OckBench: Effizienz-adjustierte Accuracy über gemischte Tasks).
- Mehrere Quellen enthalten Modellnamen ("GPT-5.5", "Claude Fable 5", "Claude Mythos 5", Datum 2026), die ich **nicht** unabhängig verifizieren kann. Ich liste sie, weil sie in den Suchergebnissen auftauchen, aber ich kann nicht ausschließen, dass es sich um spekulative/fiktive Inhalte handelt.

---

## Antwort auf die Frage (quellengestützt)

**Bringt xhigh gegenüber high einen messbaren Vorteil?**

- Auf **Coding-Accuracy** (OckBench, GPT-5.5): **0 Punkte Unterschied** (beide 100 %).
- Auf **Overall-Accuracy** (OckBench, GPT-5.5): **+1.5 Punkte**.
- Auf **effizienzadjustierter Metrik** (OckScore): **identisch** (81.6).
- **Tokenverbrauch**: +58 % (2.853 → 4.501 Tokens) für marginalen oder keinen Genauigkeitsgewinn.

**Schadet xhigh durch Over-Engineering?**

- Minimal-Editing-Studie (nrehiew.github.io): Reasoning-Varianten editieren im Generic-Setting mehr als Non-Reasoning-Varianten — also tendenziell mehr Over-Editing bei mehr Reasoning-Effort.
- Anthropic-Doku: warnt vor `max` wegen "overthinking", empfiehlt xhigh ausdrücklich für Coding/Agentic.
- HN-Anekdote: Over-Editing tritt unter xhigh weiterhin auf.

**Fazit aus den Quellen**: Für **schwere, lange Agentic-Tasks** (lt. Anthropic > 30 min, Millionen-Token-Budgets) ist xhigh empfohlen. Für **Standard-Coding** zeigen OckBench und das "Coding Models Are Doing Too Much"-Paper, dass der Zusatznutzen von xhigh gegenüber high klein und der Effizienzverlust deutlich ist. **`max`** wird sowohl empirisch (Towards-AI-Titel) als auch herstellerseitig (Anthropic-Doku) als Falle für die meisten Coding-Workloads beschrieben.

=== Web-Quellen ===
- I Tested GPT-5.5 Medium/High/xHigh Reasoning Levels - YouTube — https://www.youtube.com/watch?v=_NNni_fT3ps
- GPT-5 High Reasoning Evaluation: A Major Leap in Coding Performance — https://eval.16x.engineer/blog/gpt-5-high-reasoning-coding-performance-evaluation
- OckBench: Measuring the Efficiency of LLM Reasoning - arXiv — https://arxiv.org/html/2511.05722v3
- I Tested All 5 Effort Levels of Claude Opus 4.7 on the Same 12 Coding Problems — The 'Max' Setting Is a Trap (And 'Low' Quietly Killed Opus 4.6) | by Chew Loong Nian - AI ENGINEER | Apr, 2026 | Towards AI — https://pub.towardsai.net/i-tested-all-5-effort-levels-of-claude-opus-4-7-2f335c626786
- Reasoning Effort: Cost vs Quality Benchmarks 2026 - Digital Applied — https://www.digitalapplied.com/blog/reasoning-effort-cost-vs-quality-benchmarks-2026
- Coding Models Are Doing Too Much - wh — https://nrehiew.github.io/blog/minimal_editing/
- Effort - Claude API Docs — https://platform.claude.com/docs/en/build-with-claude/effort
- Over-editing refers to a model modifying code beyond what is necessary | Hacker News — https://news.ycombinator.com/item?id=47866913
- Claude Opus 4.7 Firmware Improvements for Engineers — https://www.linkedin.com/posts/yeutter_my-comments-on-claude-opus-47-from-a-firmware-activity-7450598411407646721-FUOx
- Introducing Claude Opus 4.7 - Anthropic — https://www.anthropic.com/news/claude-opus-4-7
- Claude Opus 4.7 Benchmarks Explained - Vellum — https://www.vellum.ai/blog/claude-opus-4-7-benchmarks-explained
- Claude Code effort levels explained - what Low/Medium/High/Max ... — https://www.reddit.com/r/ClaudeCode/comments/1soqwfl/claude_code_effort_levels_explained_what/
- (PDF) OckBench: Measuring the Efficiency of LLM Reasoning — https://www.researchgate.net/publication/397481521_OckBench_Measuring_the_Efficiency_of_LLM_Reasoning
- [PDF] OckBench: Measuring the Efficiency of LLM Reasoning | Semantic Scholar — https://www.semanticscholar.org/paper/OckBench%3A-Measuring-the-Efficiency-of-LLM-Reasoning-Du-Kang/1baf5d6a9aa42375692d24ddd1e1de25f1ce17f5
- GitHub - OckBench/OckBench · GitHub — https://github.com/OckBench/OckBench
- OckBench: Measuring the Efficiency of LLM Reasoning — https://www.opentrain.ai/papers/ockbench-measuring-the-efficiency-of-llm-reasoning--arxiv-2511.05722/
- OckBench: Measuring the Efficiency of LLM Reasoning — https://researchtrend.ai/papers/2511.05722
- Best LLM for Coding in 2026: 15 Models Ranked on 38 Real Tasks — https://ianlpaterson.com/blog/llm-benchmark-2026-38-actual-tasks-15-models-for-2-29/
- Best AI Coding Tools 2026: Claude Code, Cursor, Copilot | Nipralo Technologies — https://www.nipralo.com/blogs/best-ai-coding-tools-2026


---

## [B] E3. Android-spezifisches Vibe-Coding (Eskalation)

Ich recherchiere die drei Aspekte der Frage parallel.Lass mich noch tiefer in die spezifischen Aspekte einsteigen.Ich habe genügend Quellenmaterial gesammelt. Lass mich noch zwei spezifische Artikel genauer prüfen.# Best Practices für agentisches Vibe-Coding bei Android-Apps (Kotlin, Jetpack Compose, Gradle)

> Hinweis zur Quellenlage: Die folgenden Empfehlungen stammen aus einer Mischung aus Hersteller-/Tool-Blogs, Branchen-Analysen und Erfahrungsberichten. Wissenschaftliche Studien oder eine allgemein anerkannte Standardmethodik (analog zu SE-Standards) habe ich **nicht** gefunden. Manche Aussagen (z. B. zur Wahl konkreter Reasoning-Settings) sind in den Quellen nur als Erfahrungswerte dokumentiert — das markiere ich entsprechend.

---

## 1. Wie beschreibt man Feature-Änderungen präzise?

### 1.1 Vibe-Coding = Prompt Engineering + Context Engineering
„Vibe coding consists of context engineering and proper prompts — you have to feed the LLM with all the documentation, system prompts, whatever." (Facebook/VibeCodingLife, https://www.facebook.com/groups/vibecodinglife/posts/1926326767955851/)

Google Cloud beschreibt Vibe-Coding explizit als iterativen Prompt-Prozess: „Highlight the code … and use follow-up prompts to modify or improve it. This is perfect for adding new features, adding error handling, improving performance, or changing logic without having to manually refactor."
(Quelle: https://cloud.google.com/discover/what-is-vibe-coding)

### 1.2 Konkrete Prompt-Strukturen, die in den Quellen empfohlen werden
Aus den Erfahrungsberichten ergeben sich diese wiederkehrenden Bestandteile eines „präzisen Feature-Prompts" für Android:

| Element | Beispiel aus den Quellen | Quelle |
|---|---|---|
| Konkreter UI-Baustein | „Android Jetpack Compose code for a simple chat bubble UI" | https://skushagra.com/2025/05/zero-android-experience-to-working.html |
| Benannte Komponente + Signatur | „Provide a Jetpack Compose Composable function `StatusIndicator(appState: AppState)` that displays different icons … based on the type of `AppState`." | https://skushagra.com/2025/05/zero-android-experience-to-working.html |
| State-Modell explizit vorgeben | „Demonstrate using Kotlin Coroutines and `StateFlow` in an Android `ViewModel` to expose the current `AppState` … Then, show how a Jetpack Compose Composable function can collect this `StateFlow`" | https://skushagra.com/2025/05/zero-android-experience-to-working.html |
| Schrittweise Verfeinerung | „That's a good start, but it will crash if the user doesn't have permissions … Can you add error handling?" | https://cloud.google.com/discover/what-is-vibe-coding |
| Kontext vorher „primen" | „Initialize a go module … Read the documentation for the package using the `go doc` command." (Pattern: erst Doku/SDK laden, dann Code generieren) | https://medium.com/google-cloud/taming-vibe-coding-the-engineers-guide-fff70b6d807a |

### 1.3 Plan-/Spec vor Code (Doom Loop vermeiden)
Product Talk nennt es die wichtigste Praxis: **Planning + Code Reviews**, sonst entsteht ein „Doom Loop" endloser Korrekturen. Vibe-Coding-Tools werden dort explizit gegenübergestellt (Copilot/Agent Mode, Codex, Claude Code).
(Quelle: https://www.producttalk.org/vibe-coding-best-practices/)

Im Android-Kontext wird empfohlen, die Aufgabe in **kleine, testbare UI-Bausteine** zu zerlegen („UI bricks") statt die ganze App in einem Prompt zu beschreiben (https://skushagra.com/2025/05/zero-android-experience-to-working.html).

### 1.4 Syncfusion: Prompt-Phasen pro SDLC
Für Android-spezifische Build-/Deploy-Schritte wird empfohlen, pro Phase zu prompten — etwa „CI/CD YAML für GitHub Actions inkl. Android-spezifischer Konfiguration" und „secrets management + secure build practices" (https://www.syncfusion.com/blogs/post/prompt-engineering-vibe-coding).

---

## 2. Welche LLM-Modelle und Reasoning-Einstellungen eignen sich?

> ⚠️ **Quellenoffenlegung:** Die Quellen nennen primär *Tools* (Cursor, Claude Code, Copilot, Junie, Firebender) und weniger konkrete Reasoning-Settings (temperature, reasoning_effort, thinking-Budgets). Aussagen zu spezifischen Reasoning-Einstellungen für Android sind in den gefundenen Quellen **kaum dokumentiert** — das sage ich ausdrücklich.

### 2.1 Tool-/Modell-Landschaft für Android (Kotlin/Compose)
| Tool / Modell | Stärke laut Quelle | Quelle |
|---|---|---|
| **Claude Code** (Claude-Modelle, 1M-Token-Kontext) | „Strong reasoning and code quality"; „Large-scale refactoring … autonomous debugging … Complex project setup" | https://www.producttalk.org/vibe-coding-best-practices/ ; https://northflank.com/blog/claude-code-vs-cursor-comparison |
| **Cursor** (multi-model) | Inline-Generierung „fast and context-aware … 80% correct and stylistically consistent"; gut für tagtägliche Edits | https://dev.to/dextralabs/claude-code-vs-cursor-vs-github-copilot-honest-comparison-after-30-days-1030 |
| **GitHub Copilot / Agent Mode** | „Deepest GitHub integration … Agent Mode for autonomous tasks" | https://www.producttalk.org/vibe-coding-best-practices/ |
| **OpenAI Codex** | „Runs tasks in parallel cloud sandboxes … Can work on multiple tasks at once and propose PRs" | https://www.producttalk.org/vibe-coding-best-practices/ |
| **JetBrains Junie** (in Android Studio) | „Best tips when working with AI agents, specifically Junie, inside Android Studio" | https://www.youtube.com/watch?v=pOVX-6N6JOI |
| **JetBrains AI Assistant / Mellum** (Android Studio Plugin) | Code-Completion + Tests + Commit-Messages; lokale Modelle via LM Studio/Ollama offline nutzbar | https://blog.jetbrains.com/ai/2025/03/ai-assistant-comes-to-kotlin-developers-in-android-studio/ |
| **Firebender** (Android Studio + IntelliJ) | „The best agentic coding experience possible in Android Studio + IntelliJ"; spezielles Android-MCP-SDK | https://www.jasonpearson.dev/ai-code-assistants-for-android-engineers/ ; https://github.com/kaeawc/android-mcp-sdk |
| **Open-Source-Alternativen** | Qwen3 Coder, DeepSeek v3/R1, Qwen3 Thinking 235B — „Matches/exceeds Claude for coding" (laut Northflank) | https://northflank.com/blog/claude-code-vs-cursor-comparison |

### 2.2 Spezifische Android-Evaluierung
**Kotlin-bench V2** ist die mir bekannte Benchmark-Initiative, die agentische LLMs gezielt auf **Android-Studio-Projekten** (mit Debugger, Compose-Previews, Emulator) testet — also die relevanteste Quelle für „Modellwahl für Android":
> „We built custom cloud infrastructure purpose-built for Android project environments … Each evaluation runs in a full Android Studio instance in the cloud with optimized Gradle caching and indexing."
(Quelle: https://firebender.com/blog/kotlin-bench-v2)

In den Suchergebnissen waren **keine konkreten Reasoning-Einstellungen** (z. B. `reasoning_effort=high`, „thinking budget", Temperaturwerte) dokumentiert, die für Android-Vibe-Coding als Best Practice gelten. Hier ist die Quellenlage **lückenhaft**.

---

## 3. Häufige Fehlerquellen bei KI-generiertem Android-Code

### 3.1 Die „5 Most Common AI Android Code Pitfalls" (Praxeen Yadati, Medium)
Die einzige mir vorliegende Quelle, die explizit eine Fehlerliste für KI-generierten Android/Compose-Code aufstellt:
1. **Wrong Coroutine Scope** — AI nutzt `GlobalScope` oder vergisst Cancellation. „Leaked coroutines don't crash immediately, they drain battery and cause subtle bugs."
2. **Missing Null Safety** — häufiges Generieren von `!!` (force-unwrap); „Each `!!` is a potential `NullPointerException`."
3. **Outdated API Usage** — Modelle vor Ende 2024 verwenden teils `LiveData` statt `StateFlow` oder `AsyncTask`.
4. **Recomposition Issues in Compose** — AI optimiert Compose selten; Lambdas verursachen unnötige Recomposition; „If your UI feels janky, check for unstable parameters."
(Quelle: https://medium.com/@praveen.dheep/ai-vibe-coding-for-android-how-i-let-ai-write-my-kotlin-jetpack-compose-code-and-what-broke-738d6c072493)
> ⚠️ **Quellen-Kritikalität:** Der Artikel trägt das Datum „Jun, 2026" — das ist ein auffälliger Datumswert. Ich liste die Aussagen, weil sie konkret und plausibel sind, kann aber die Autorität/Originalität nicht garantieren.

### 3.2 Unstable Lambda Parameters / Recomposition
Reddit r/androiddev: „I spent maybe three days fixing this … a recomposition futility … all lambdas are unstable by default." (https://www.reddit.com/r/androiddev/comments/1an5emm/compose_unstable_lambda_parameters/)
Kotlin-Slack: „ViewModel is unstable … all lambdas are unstable by default." (https://slack-chats.kotlinlang.org/t/8454078/)
Das ist ein Compose-spezifisches Problem, das KI-Code erzeugt oder verschärft, wenn der Agent nicht weiß, wann `remember { }`/`@Stable` einzusetzen ist.

### 3.3 Allgemeine „Vibe-Coding-Fallen" für Android
LinkedIn-Pulse „Vibe Coding in Android Development: Trend, Tool, or Trap?" fasst zusammen:
- „Working code ≠ production-ready code"
- „Violate best practices"
- „Use inefficient patterns"
- „Ignore edge cases specific to Android devices"
(Quelle: https://www.linkedin.com/pulse/vibe-coding-android-development-trend-tool-trap-krishanu-nandan-cwwoc)

### 3.4 Build-/Toolchain-Fallen
In den bekannten Issue-Listen sind typische „AI-erzeugte" bzw. generische Android-Build-Fehler dokumentiert:
- Falsche/inkonsistente `kotlinCompilerExtensionVersion` in `composeOptions` → „after updating the Kotlin and Compose versions, my problem was fixed" (https://stackoverflow.com/questions/70735478/problems-with-android-compose)
- AGP-Internal-Task-Fehler; Heap/Gradle-JVM-Args (`org.gradle.jvmargs=-Xmx512m`) und MultiDex-Setup (https://stackoverflow.com/questions/57606462/a-failure-occurred-while-executing-com-android-build-gradle-internal-tasks)
- Bekannte Android-Studio-/AGP-Inkompatibilitäten (Lint, MPP, macOS-Issues): https://developer.android.com/studio/known-issues

### 3.5 Halluzination von SDK-/API-Namen
Google Cloud (Petruzalek) nennt als typische KI-Falle: „the model will hallucinate a few bits, like for example, assuming ADK means 'Android Development Kit' instead of 'Agent Development Kit'". Gegenmittel: vorher Doku/SDK-Context erzwingen.
(Quelle: https://medium.com/google-cloud/taming-vibe-coding-the-engineers-guide-fff70b6d807a)

### 3.6 Review-Disziplin
Cursor-Erfahrungsbericht: „Bugs introduced: 2 (both caught in review — incorrect default parameter values)" — Review ist die eigentliche Fehlerbremse.
(Quelle: https://dev.to/dextralabs/claude-code-vs-cursor-vs-github-copilot-honest-comparison-after-30-days-1030)

---

## Zusammenfassung dessen, was **nicht** ausreichend belegt ist

Ich habe in den verfügbaren Web-Quellen **keine** verlässlichen, konkreten Empfehlungen gefunden zu:

- **Spezifischen Reasoning-Einstellungen** (z. B. `reasoning_effort`, „thinking budget", Temperature, Top-p) für Android-Vibe-Coding. Die Tools werden genannt, aber die Parametrierung wird in den von mir gefundenen Quellen nicht Android-spezifisch dokumentiert.
- **Einer kanonischen „Android-spezifischen" Vibe-Coding-Methodik** — die meisten Empfehlungen sind generisch („Plan, dann Code, dann Review"), nur skushagra.com und producttalk.org geben Android-/Compose-bezogene Beispiele.
- **Einer quantitativen Vergleichsstudie**, welches Modell für Kotlin/Compose-Code-Generierung objektiv am besten abschneidet. **Kotlin-bench V2** (Firebender) ist die relevanteste Benchmark-Initiative, aber konkrete Rankings/Ergebnisse waren in den Suchausgaben nicht enthalten.

---

## Konsolidierte Quellenliste

1. https://developer.android.com/compose — Offizielle Jetpack-Compose-Doku (Kontext für Compose-Prompts)
2. https://www.linkedin.com/pulse/vibe-coding-android-development-trend-tool-trap-krishanu-nandan-cwwoc — Vibe-Coding-Fallen speziell für Android
3. https://skushagra.com/2025/05/zero-android-experience-to-working.html — Detaillierte Android-/Compose-Prompt-Beispiele (UI bricks, FSM-State-Modell)
4. https://www.youtube.com/watch?v=OcBkJUwjDqg — „5 Tricks for Agentic Android Development"
5. https://www.producttalk.org/vibe-coding-best-practices/ — Doom-Loop-Vermeidung, Tool-Vergleich
6. https://www.jasonpearson.dev/ai-code-assistants-for-android-engineers/ — Firebender, Android MCP SDK, Android-Tool-Vergleich
7. https://www.youtube.com/watch?v=pOVX-6N6JOI — Junie in Android Studio (Use Cases Learning/Coding)
8. https://blog.jetbrains.com/ai/2025/03/ai-assistant-comes-to-kotlin-developers-in-android-studio/ — JetBrains AI Assistant / Mellum, Offline-Modelle (LM Studio/Ollama)
9. https://firebender.com/blog/kotlin-bench-v2 — Android-spezifische agentische LLM-Evaluierung
10. https://sharmaricha7724.medium.com/jetpack-compose-meets-ai-creating-smart-android-apps-with-agents-558c588fb89a — Compose-spezifische AI-Agent-Fähigkeiten (Explain, Improve, Preview, Test, Image→Code)
11. https://medium.com/@praveen.dheep/ai-vibe-coding-for-android-how-i-let-ai-write-my-kotlin-jetpack-compose-code-and-what-broke-738d6c072493 — „5 Most Common AI Android Code Pitfalls" (Datum „Jun 2026" auffällig)
12. https://cloud.google.com/discover/what-is-vibe-coding — Google-Definition, iteratives Prompten
13. https://medium.com/google-cloud/taming-vibe-coding-the-engineers-guide-fff70b6d807a — Context Engineering, SDK-Halluzination
14. https://www.syncfusion.com/blogs/post/prompt-engineering-vibe-coding — Prompt-Phasen pro SDLC (Android-Build-Konfiguration)
15. https://dev.to/dextralabs/claude-code-vs-cursor-vs-github-copilot-honest-comparison-after-30-days-1030 — 30-Tage-Vergleich, Review-Disziplin
16. https://northflank.com/blog/claude-code-vs-cursor-comparison — Tool-Eignung, Open-Source-Modelle (Qwen3, DeepSeek)
17. https://www.reddit.com/r/androiddev/comments/1an5emm/compose_unstable_lambda_parameters/ — Recomposition-/Unstable-Lambda-Diskussion
18. https://developer.android.com/kotlin/flow/stateflow-and-sharedflow — StateFlow (Hintergrund für korrektes State-Prompting)
19. https://stackoverflow.com/questions/70735478/problems-with-android-compose — Compose-/Kotlin-Versions-Inkompatibilität
20. https://stackoverflow.com/questions/57606462/a-failure-occurred-while-executing-com-android-build-gradle-internal-tasks — AGP-/Gradle-Build-Fehler
21. https://developer.android.com/studio/known-issues — Offizielle Liste bekannter Android-Studio-/AGP-Issues
22. https://www.facebook.com/groups/vibecodinglife/posts/1926326767955851/ — Vibe-Coding = Context Engineering + Prompting

---

**Falls du möchtest**, kann ich gezielt einzelne Punkte vertiefen — z. B. konkrete Prompt-Templates für Compose-Refactoring oder eine Detailrecherche zu `reasoning_effort`-Einstellungen (dazu bräuchte ich allerdings neue Suchanläufe mit anderer Suchstrategie, da die Quellenlage hier lückenhaft ist).

=== Web-Quellen ===
- Jetpack Compose UI App Development Toolkit - Android Developers — https://developer.android.com/compose
- Vibe Coding in Android Development: Trend, Tool, or Trap? — https://www.linkedin.com/pulse/vibe-coding-android-development-trend-tool-trap-krishanu-nandan-cwwoc
- How to vibe code properly. - Declarative — https://skushagra.com/2025/05/zero-android-experience-to-working.html
- 5 Tricks for Agentic Android Development (Make your ... - YouTube — https://www.youtube.com/watch?v=OcBkJUwjDqg&vl=en
- Vibe Coding Best Practices: Avoid the Doom Loop with Planning and Code Reviews — https://www.producttalk.org/vibe-coding-best-practices/?srsltid=AfmBOooTi3M5Nh3ODnsx_gcbCN7pJ18xwc0EFHeZ-QpZR-51bG0LJJUV
- AI Code Assistants for Android Engineers - Jason Pearson — https://www.jasonpearson.dev/ai-code-assistants-for-android-engineers/
- What AI is good for Kotlin coding? - Reddit — https://www.reddit.com/r/Kotlin/comments/1qa6bmg/what_ai_is_good_for_kotlin_coding/
- How You Use an AI Coding Agent the RIGHT Way For Mobile Development - My Best Tips - YouTube — https://www.youtube.com/watch?v=pOVX-6N6JOI
- AI Assistant Comes to Kotlin Developers in Android Studio | The JetBrains AI Blog — https://blog.jetbrains.com/ai/2025/03/ai-assistant-comes-to-kotlin-developers-in-android-studio/
- Kotlin-bench V2: Agentic LLM Evaluation | Firebender — https://firebender.com/blog/kotlin-bench-v2
- Jetpack Compose Meets AI: Creating Smart Android Apps with Agents | by Richa Sharma | Medium — https://sharmaricha7724.medium.com/jetpack-compose-meets-ai-creating-smart-android-apps-with-agents-558c588fb89a
- kotlin - Problems with android Compose - Stack Overflow — https://stackoverflow.com/questions/70735478/problems-with-android-compose
- Known issues with Android Studio and Android Gradle Plugin  |  Android Developers — https://developer.android.com/studio/known-issues
- AI Vibe Coding for Android: How I Let AI Write My Kotlin & Jetpack Compose Code (And What Broke) | by Praveen Yadati | Jun, 2026 | Medium — https://medium.com/@praveen.dheep/ai-vibe-coding-for-android-how-i-let-ai-write-my-kotlin-jetpack-compose-code-and-what-broke-738d6c072493
- A failure occurred while executing com.android.build.gradle.internal.tasks - Stack Overflow — https://stackoverflow.com/questions/57606462/a-failure-occurred-while-executing-com-android-build-gradle-internal-tasks
- Vibe Coding** is all about **prompt engineering. - Facebook — https://www.facebook.com/groups/vibecodinglife/posts/1926326767955851/
- Vibe Coding Explained: Tools and Guides - Google Cloud — https://cloud.google.com/discover/what-is-vibe-coding
- How to Vibe Code an App From Start to Finish (Full Course) - YouTube — https://www.youtube.com/watch?v=lbzsbaJfv10
- Taming Vibe Coding: The Engineer’s Guide | by Daniela Petruzalek | Google Cloud - Community | Medium — https://medium.com/google-cloud/taming-vibe-coding-the-engineers-guide-fff70b6d807a
- Prompt Engineering + Vibe Coding: A New Era for Software Developers | Syncfusion Blogs — https://www.syncfusion.com/blogs/post/prompt-engineering-vibe-coding
- Claude Code vs Cursor vs GitHub Copilot: Honest Comparison After 30 Days - DEV Community — https://dev.to/dextralabs/claude-code-vs-cursor-vs-github-copilot-honest-comparison-after-30-days-1030
- Claude Code & Cursor built the same app. There's a clear winner. - YouTube — https://www.youtube.com/watch?v=aRNVncOYd5c
- AI Coding Agents in 2026: Claude Code, Cursor, and How We Actually Use Them | by Kevin Gabeci | Medium — https://kgabeci.medium.com/ai-coding-agents-in-2026-claude-code-cursor-and-how-we-actually-use-them-d76d9c397d82
- Claude Code vs Cursor: Complete comparison guide in 2026 | Blog — Northflank — https://northflank.com/blog/claude-code-vs-cursor-comparison
- Claude Code vs Cursor vs Copilot vs Codeium: Which AI ... — https://www.reddit.com/r/AI_Agents/comments/1t03m4y/claude_code_vs_cursor_vs_copilot_vs_codeium_which/
- StateFlow and SharedFlow  |  Kotlin  |  Android Developers — https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
- Recomposition Is Not a Bug — You Are | by Behzod Halil — https://proandroiddev.com/recomposition-is-not-a-bug-you-are-5d736f65c8b9
- Compose unstable lambda parameters : r/androiddev - Reddit — https://www.reddit.com/r/androiddev/comments/1an5emm/compose_unstable_lambda_parameters/
- Hi guys I have trouble with jetpack compose recomposition ... — https://slack-chats.kotlinlang.org/t/8454078/hi-guys-i-have-trouble-with-jetpack-compose-recomposition-th
- Jetpack Compose recomposition issues with StateFlow in large screens (Android Studio + Kotlin) · community · Discussion #186785 · GitHub — https://github.com/orgs/community/discussions/186785
