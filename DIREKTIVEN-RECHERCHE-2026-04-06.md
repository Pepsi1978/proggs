# Direktiven-Recherche 2026-04-06

**Datum:** 2026-04-06
**Methode:** 5 parallele Researcher, ~15 Web-Searches/Fetches
**Basis:** v1 (2026-04-05) und v2 (2026-04-05-v2) — alle dort beschriebenen Punkte gelten als bekannt

---

## Zusammenfassung

- ~25 Quellen analysiert, **8 neue Erkenntnisse**, **5 Sofortmassnahmen**
- Top-3 Sofortmassnahmen:
  1. **updatedInput PreToolUse**: Hooks koennen Claude's Tool-Aufrufe STILL KORRIGIEREN statt nur zu blockieren (seit Claude Code v2.0.10 — noch nicht genutzt)
  2. **Prozedurale Workflow-Memory (LEGOMem-Muster)**: Erfolgreiche Tool-Reihenfolgen als Muster speichern — nicht nur was getan wurde, sondern WIE und in welcher Reihenfolge
  3. **Agent Nervous System**: Heartbeat-Monitoring das erkennt ob das System "lebt und sinnvoll arbeitet" — jenseits von "laeuft der Prozess?"

---

## Bereits implementiert (nicht nochmal vorgeschlagen)

| Punkt | Status |
|-------|--------|
| Bug-Cases Auto-Writer | Aktiv (bug-cases.jsonl) |
| Pattern-Matching gegen bekannte Fehler | Aktiv (error-antigens.jsonl) |
| Poka-Yoke (mehrere Stufen) | Aktiv (config-guard, session-guard, hook-exit0-guard) |
| Defense in Depth (4 Schichten) | Aktiv |
| Session-Scores | Aktiv (session-scorer Hook) |
| ClaudeWatch Monitoring | Aktiv |
| Intent-Tracking (intent-anker Hook) | Aktiv |
| Retry-Zaehler, Drift-Detektor | In Direktive #2 beschrieben |
| Circuit Breaker (retry-state.json) | Laut v2-Recherche: implementiert |
| Prozedurale Memory (Konzept) | Konzeptionell — KEINE technische Umsetzung |
| Experience Store (JSONL) | Aktiv — speichert WAS, nicht WIE |

---

## Teil 1: DIREKTIVE #1 — Superintelligenz

### Erkenntnis 1.1: SICA — Selbstreferentielle Verbesserungsschleife (NEU)
**Quelle:** arXiv 2504.15228 — Self-Improving Coding Agent (SICA)
**Was:** SICA zeigt erstmals, dass ein Coding Agent seine eigene Codebasis modifizieren kann
um sich selbst zu verbessern — von 17% auf 53% auf SWE-Bench Verified.
**Konkrete Mechanismen:**
- Utility-Funktion: Benchmark-Performance (50%) + Kosten (25%) + Geschwindigkeit (25%)
- Ein asynchroner Overseer-Agent ueberwacht die Selbstverbesserung in Echtzeit
- Konkrete Selbst-Verbesserungen: Diffs statt Ueberschreiben, AST-Symbol-Locator, Context-aware Diff-Minimierung
**Was Claude Code davon uebernehmen kann:**
- Eine Utility-Metrik pro Session berechnen (nicht nur Session-Score, sondern messbare Effizienz)
- Die Idee: welche Tools wuerden die Arbeit beschleunigen? Und diese dann VORSCHLAGEN

### Erkenntnis 1.2: Live-SWE-agent — Step-Reflection Mechanism (NEU — direkt umsetzbar!)
**Quelle:** arXiv 2511.13646 — Live-SWE-agent (77.4% auf SWE-bench Verified)
**Was:** Nach JEDEM Tool-Aufruf fragt der Agent sich: "Koennte ich ein Tool/Skript erstellen das
diese Aufgabe schneller macht?" Dieser einfache Schritt verbessert die Ergebnisse signifikant.
**Konkreter Mechanismus:**
Ein einzelner Satz wird nach jedem Tool-Feedback angehaengt:
"Reflect on the previous trajectory and decide if there are any tools you can create to help with the current task."
**Wirkung:** Durchschnittlich 3.28 statt 2.92 nuetzliche Tools pro Aufgabe — 12% mehr Effizienz.
**Umsetzbar als Hook:** Ein PostToolUse-Prompt-Hook der genau diesen Reflektions-Satz injiziert.

### Erkenntnis 1.3: MemRL — Utility-gewichteter Experience Store (NEU)
**Quelle:** arXiv 2601.03192 — MemRL: Self-Evolving Agents via Runtime Reinforcement Learning
**Was:** Erfahrungen werden nicht nur gespeichert, sondern mit Q-Werten (Nuetzlichkeits-Scores)
bewertet. Bei Abruf wird nicht nur Aehnlichkeit, sondern NUETZLICHKEIT maximiert.
**Konkret:** Intent-Experience-Utility Triplets: Was wollte ich? Was habe ich gemacht? Hat es geholfen?
**Fast-Misses haben hohen Wert:** Fehlerhafte Loesungen die "fast funktioniert haben" werden
NICHT geloescht, sondern mit niedrigerem Q-Wert behalten — sie helfen trotzdem.
**Umsetzbar:** experience-store.jsonl um Felder `utility_score` (0-1) und `near_miss: true/false` erweitern.

---

## Teil 2: DIREKTIVE #2 — Selbstbeobachtung

### Erkenntnis 2.1: Agent Drift — 3 Drift-Typen und ASI (NEU)
**Quelle:** arXiv 2601.04170 — Agent Drift: Quantifying Behavioral Degradation
**Was:** Drift ist kein einzelnes Phaenomen. Es gibt drei klar trennbare Typen:
1. **Semantischer Drift**: Claude weicht vom urspruenglichen Intent ab (Intent-Tracker adressiert das)
2. **Koordinations-Drift**: Wenn Subagenten sich widersprechen
3. **Verhaltens-Drift**: Claude entwickelt unbeabsichtigte Strategien (z.B. zu vorsichtig werden)
**Was neu ist:** Verhaltens-Drift ist der am schwersten erkennbare. Der Agent "funktioniert" noch,
aber seine Strategien sind suboptimal geworden. Ein Beispiel: Nach vielen Fehlern wird Claude
zu zaghaft und bittet oefte nach Erlaubnis als noetig.
**Empfohlene Massnahmen:** Episodisches Gedaechtnis-Konsolidierung + Adaptive Verhaltensverankerung

### Erkenntnis 2.2: Metacognitive Co-Regulation Agent — CRDAL (NEU)
**Quelle:** arXiv 2603.24768 — CRDAL (Co-Regulation Design Agentic Loop)
**Was:** Ein SEPARATER Agent beobachtet den arbeitenden Agenten und gibt metacognitives Feedback.
Er analysiert: Verbessert sich das Design? Wo stockt es? Was waere eine andere Strategie?
**Wirkung:** 70.92 Ah vs 49.31 Ah im Batterie-Design-Test — signifikant besser, ohne mehr Rechenaufwand.
**Wichtige Erkenntnis:** Separate Agenten sind besser als Self-Reflection. Der externe Beobachter
sieht Muster die der arbeitende Agent nicht sieht (fresh context window, kein Recency-Bias).
**Umsetzbar:** Ein "Metacognitive Observer" Agent der bei komplexen Sessions (>20 Tool-Calls)
automatisch eine Progress-Trajectory-Summary analysiert und Strategie-Feedback gibt.

### Erkenntnis 2.3: Agent Nervous System — Heartbeat-Monitoring (NEU)
**Quelle:** earezki.com, 2026-04-01 — "LLM Agents Need a Nervous System, Not Just a Brain"
**Was:** Agenten muessen aktiv "I am alive" melden — nicht nur "laeuft der Prozess noch".
Standard-Monitoring sieht nur: laeuft der Prozess? Das reicht nicht.
Ein "nervensystem" misst: logische Konsistenz, Kosten-pro-Heartbeat, Validator-Compliance.
**Vier Kern-Signale:**
1. Validator-Compliance (folgt der Agent seinen Regeln?)
2. Length Drift (werden Antworten unerklaerlich kuerzer/laenger?)
3. Semantic Similarity (weichen die Tool-Calls vom Muster ab?)
4. Regression Detection (verschlechtert sich die Performance ueber Zeit?)

---

## Teil 3: DIREKTIVE #3 — Resilient Bugfixing

### Erkenntnis 3.1: updatedInput in PreToolUse — "Stille Korrektur" (NEU — DIREKT UMSETZBAR!)
**Quelle:** Claude Code Docs, seit v2.0.10
**Was:** PreToolUse-Hooks koennen nicht nur BLOCKIEREN (exit 1) oder ERLAUBEN (exit 0),
sondern sie koennen das Tool-Input STILL KORRIGIEREN und dann erlauben.
**Konkret:** Wenn Claude einen falschen Pfad benutzen wuerde, kann der Hook den Pfad korrigieren
ohne Claude zu stoppen. Keine Fehlermeldung, kein neuer LLM-Turn — einfach korrigiert.
**Vorteile:** Weniger Latenz, weniger Token-Kosten, deterministisch statt probabilistisch.
**Anwendungsfall fuer dieses System:**
- Wenn Claude einen ~/Codex/ Pfad benutzen wuerde → Hook korrigiert zu ~/proggs/
- Wenn ein Git-Befehl ohne fetch+rebase aufgerufen wird → Hook fuegt es still hinzu
- Wenn ein sed-Befehl auf JSON-Dateien angewendet wird → Hook ersetzt durch Python

### Erkenntnis 3.2: LEGOMem — Prozedurale Memory fuer Workflows (NEU — wichtigste Erkenntnis!)
**Quelle:** arXiv 2510.04851, Microsoft Research, AAMAS 2026
**Was:** LEGOMem speichert erfolgreiche Workflows als wiederverwendbare "Memory Units" in zwei Typen:
- **Full-Task Memories**: Kompletter Plan + Reasoning-Trace fuer eine Aufgabe
- **Subtask Memories**: Spezifische Agent-Verhaltensweisen und Tool-Interaktionen
**Wirkung:** +12.61% bis +13.38% bessere Erfolgsrate — sogar kleinere Modelle profitieren stark.
**Was das fuer dieses System bedeutet:** Die experience-store.jsonl speichert heute WAS getan wurde.
LEGOMem-Muster wuerden speichern: Fuer Aufgabe vom Typ X → benutze Tool-Sequenz [A→B→C],
nicht [D→E→A], weil die letztere 3x mehr Retries braucht.

---

## Sofortmassnahmen (Priorisiert)

### Massnahme 1: updatedInput Hook — "Stille Pfad-Korrektur"
**Aufwand:** 2-3 Stunden
**Was bauen:** Einen PreToolUse-Hook der bash-Befehle mit /Codex/ Pfaden still auf /proggs/ korrigiert,
und sed-Befehle auf JSON-Dateien still durch python3 ersetzt.
**Warum:** Statt einer Fehlermeldung und einem neuen LLM-Turn → direkter Fix, weniger Tokens.
**Direktive:** #3 (Poka-Yoke Stufe 3 — Fehler konzeptionell unmoeglich machen)
**URL:** https://code.claude.com/docs/en/hooks-guide

### Massnahme 2: Prozedurale Workflow-Memory (LEGOMem-Muster)
**Aufwand:** 3-4 Stunden
**Was bauen:** experience-store.jsonl um Felder erweitern:
- `tool_sequence: ["Read", "Grep", "Edit", "Bash"]` — die Tool-Reihenfolge
- `task_type: "android_build_fix"` — Aufgaben-Kategorie
- `utility_score: 0.85` — wie gut hat diese Sequenz funktioniert
- `near_miss: false` — war es ein Beinahefehler?
Bei neuen Aufgaben: aehnliche task_types suchen und erfolgreiche tool_sequences als Startpunkt.
**Warum:** System weiss dann nicht nur WAS getan wurde, sondern WIE man aehnliche Aufgaben angeht.
**Direktive:** #1 (Compound Intelligence Effect — jede Session baut auf fruehere auf)
**URL:** https://arxiv.org/abs/2510.04851

### Massnahme 3: Step-Reflection PostToolUse-Hook (direkt umsetzbar!)
**Aufwand:** 1-2 Stunden (minimaler Aufwand, grosser Effekt!)
**Was bauen:** Ein PostToolUse Prompt-Hook der nach jedem Tool-Aufruf den Satz injiziert:
"Gibt es ein Hilfsskript oder eine Regel die du erstellen koenntest um diese Art von Aufgabe
kuenftig schneller zu erledigen? Wenn ja, schlage es am Ende als Intelligenz-Vorschlag vor."
**Warum:** Live-SWE-agent hat mit genau diesem Mechanismus 12% mehr Effizienz erzielt, ohne mehr Aufwand.
**Direktive:** #2 (Selbstbeobachtung — Erkennen von Mustern waehrend der Arbeit)
**URL:** https://arxiv.org/html/2511.13646v3

### Massnahme 4: Verhaltens-Drift-Erkennung
**Aufwand:** 2-3 Stunden
**Was bauen:** Den Drift-Detektor in Direktive #2 um einen vierten Modus erweitern:
"Verhaltensdrift" — prueft ob Claude in den letzten 10 Tool-Calls oefte nach Erlaubnis
gefragt hat als in den ersten 10 (Zeichen von zu viel Vorsicht nach Fehlern).
Ein einfacher Zaehler: `permission_requests` pro 10-Tool-Fenster. Wenn >3 → Alarm.
**Direktive:** #2 (Echtzeit-Tracker erweitern)
**URL:** https://arxiv.org/abs/2601.04170

### Massnahme 5: Heartbeat-Monitoring (Agent Nervous System)
**Aufwand:** 2-3 Stunden
**Was bauen:** Den heartbeat.sh Hook um 4 Kern-Signale erweitern:
1. Validator-Compliance: Wurden die letzten 5 Sessions mit Intelligenz-Vorschlaegen beendet?
2. Length Drift: Werden Antworten kuerzer als der Session-Durchschnitt? (Zeichen von Drift)
3. Semantic Drift: Weichen Tool-Calls vom intent-anker ab?
4. Regression: Ist der aktuelle Session-Score niedriger als der 5-Session-Durchschnitt?
**Direktive:** #2 (Selbstbeobachtung — Echtzeit-Tracker)
**URL:** https://earezki.com/ai-news/2026-04-01-llm-agents-need-a-nervous-system-not-just-a-brain/

---

## Weitere Vorschlaege

### Mittelfristig (1-2 Tage)
| # | Vorschlag | Direktive | Quelle | Impact | Erklaerung |
|---|-----------|-----------|--------|--------|------------|
| 6 | CRDAL Metacognitive Observer | #2 | arXiv 2603.24768 | HOCH | Separater Agent analysiert Progress-Trajectory bei >20 Tool-Calls |
| 7 | Near-Miss Retention in Bug-Cases | #3 | arXiv 2601.03192 | MITTEL | Beinahefehler speichern mit Markierung near_miss:true statt loeschen |
| 8 | Utility-Score fuer Experience Store | #1 | MemRL 2601.03192 | HOCH | Sessions berwerten und Score (0-1) an Eintrag anhaengen |

### Langfristig (1-2 Wochen)
| # | Vorschlag | Direktive | Quelle | Impact | Erklaerung |
|---|-----------|-----------|--------|--------|------------|
| 9 | MemEvolve: Memory-Architektur selbst evolvieren | #1 | arXiv 2512.18746 | SEHR HOCH | Das MEMORY-System selbst optimiert sich — komplexes Meta-Learning |
| 10 | SICA Overseer-Architektur | #2 | arXiv 2504.15228 | HOCH | Asynchroner Overseer der pathologische Verhaltensweisen erkennt |

---

## Verbesserungen fuer bestehende Features

### experience-store.jsonl: Zu flach fuer echte Lernkurve
**Aktuell:** Speichert task_type, strategy, success_score
**Problem:** Kein Zeitbezug, keine Tool-Sequenz, kein Utility-Score — Eintraege altern nicht
**Verbesserung:** Felder hinzufuegen: tool_sequence[], utility_score, near_miss, timestamp_decay
Mit timestamp_decay (Eintraege verlieren Gewicht nach 30 Tagen) vermeidet man veraltete Muster.

### Heartbeat-Hook: Ueberwacht nur ob Prozess laeuft
**Aktuell:** heartbeat.sh prueft ob Services laufen
**Problem:** Kein "logical heartbeat" — der Prozess kann laufen waehrend das Verhalten degeneriert
**Verbesserung:** Vier Kernel-Signale wie oben beschrieben hinzufuegen

### Direktive #2 Echtzeit-Tracker: Nur 3 Tracker, kein Verhaltens-Drift
**Aktuell:** Retry-Zaehler, Korrektur-Zaehler, Drift-Detektor
**Problem:** Verhaltens-Drift (zu vorsichtig werden, zu oft nach Erlaubnis fragen) wird nicht erkannt
**Verbesserung:** 4. Tracker hinzufuegen: Verhaltens-Drift-Detektor (permission_requests Zaehler)

---

## Quellen-Index
1. https://arxiv.org/html/2504.15228v1 — SICA: Self-Improving Coding Agent
2. https://arxiv.org/html/2511.13646v3 — Live-SWE-agent: Step-Reflection Mechanism
3. https://arxiv.org/abs/2601.03192 — MemRL: Runtime Reinforcement Learning on Episodic Memory
4. https://arxiv.org/abs/2601.04170 — Agent Drift: Quantifying Behavioral Degradation
5. https://arxiv.org/html/2603.24768v1 — CRDAL: Metacognitive Co-Regulation Agent
6. https://arxiv.org/abs/2510.04851 — LEGOMem: Modular Procedural Memory
7. https://arxiv.org/abs/2512.18746 — MemEvolve: Meta-Evolution of Agent Memory Systems
8. https://code.claude.com/docs/en/hooks-guide — Claude Code updatedInput Hooks
9. https://www.pixelmojo.io/blogs/claude-code-hooks-production-quality-ci-cd-patterns — Hook Patterns
10. https://earezki.com/ai-news/2026-04-01-llm-agents-need-a-nervous-system-not-just-a-brain/ — Agent Nervous System
11. https://earezki.com/ai-news/2026-04-02-three-ai-agent-failure-modes-that-traditional-monitoring-will-never-catch/ — Failure Modes
