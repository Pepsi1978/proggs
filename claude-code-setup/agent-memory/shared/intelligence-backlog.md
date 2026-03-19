# Intelligence Backlog — Findings aus R8 Recherchen

> Kumulatives Wissen aus Intelligence-Researcher-Laeufen.
> Status: UMGESETZT / BLOCKIERT / OFFEN / VERWORFEN
> Updated by: intelligence-researcher agent (R8)

## Lauf 2026-03-19 (Erster Lauf — 12 Findings)

### Finding 1: Darwin Goedel Machine (DGM) — Agent-Varianten-Archiv
- **Quelle:** arxiv.org/abs/2505.22954 (Sakana AI)
- **Kernidee:** Agent haelt Archiv von Varianten, selektiert per Evolution
- **Status:** OFFEN — langfristiges Konzept, braucht eigene Design-Session
- **Prioritaet:** Langfristig

### Finding 2: ReVeal — Coder schreibt selbst Smoke-Tests
- **Quelle:** arxiv.org/html/2506.11442v1
- **Status:** VERWORFEN — Challenger: "Autor testet eigene Annahmen"
- **Grund:** Unabhaengiger Tester-Agent existiert, Selbsttests bringen keine neue Perspektive

### Finding 3: Atom of Thoughts (AoT) — Atomares paralleles Reasoning
- **Quelle:** arxiv.org/pdf/2502.12018
- **Kernidee:** Gedankenschritte als unabhaengige Atome parallel skalieren
- **Status:** OFFEN — relevant fuer architect-Agent bei komplexen Entscheidungen
- **Prioritaet:** Mittel

### Finding 4: claude-context-local — Semantische Code-Suche per Embeddings
- **Quelle:** github.com/FarhanAliRaza/claude-context-local
- **Status:** BLOCKIERT — Python noetig + Node 24 inkompatibel fuer Cloud-Version
- **Blocker:** Kein TypeScript/Bun-basierter MCP-Server fuer lokale Embeddings verfuegbar
- **Naechster Schritt:** Eigenen MCP in Bun bauen (Session B1 geplant)
- **Prioritaet:** HOCH — groesster Einzelhebel laut Benchmark

### Finding 5: Graphiti — Temporaler Wissensgraph
- **Quelle:** github.com/getzep/graphiti
- **Status:** OFFEN — MEMORY.md reicht erstmal, aber fuer groessere Projekte relevant
- **Prioritaet:** Niedrig

### Finding 6: Augment Code — Kontext-Engine als Geheimwaffe
- **Quelle:** augmentcode.com/blog/auggie-tops-swe-bench-pro
- **Kernidee:** Gleiches Modell, +17 SWE-bench Probleme nur durch bessere Kontext-Suche
- **Status:** BESTAETIGT Finding 4 — semantische Code-Suche ist der Hebel
- **Prioritaet:** HOCH

### Finding 7: SWE-CI — 75% der Agents brechen Working Code
- **Quelle:** noqta.tn (SWE-CI Benchmark, Maerz 2026)
- **Status:** UMGESETZT — Regression-Check im Tester-Agent hinzugefuegt (#528)
- **Umsetzung:** tester.md → "Regression Check" Sektion (mandatory before every commit)

### Finding 8: Scaffolding > Modell
- **Quelle:** morphllm.com/ai-coding-agent
- **Kernidee:** Architektur entscheidet mehr als Modellgewichte
- **Status:** BESTAETIGT Finding 4 und 6 — Kontext-Aufbereitung ist der Hebel
- **Prioritaet:** Bestätigung, kein eigener Aktionspunkt

### Finding 9: Peer-Review als emergenter Mechanismus
- **Quelle:** sakana.ai/dgm
- **Status:** BEREITS VORHANDEN — quality-gate Agent macht das
- **Prioritaet:** Keine Aktion noetig

### Finding 10: HAWK — Hierarchische 4-Ebenen-Dekomposition
- **Quelle:** arxiv.org/pdf/2507.04067
- **Kernidee:** Problem in Intent → Workflow → Operationen → Atomic Actions zerlegen
- **Status:** OFFEN — relevant fuer architect-Agent
- **Prioritaet:** Mittel

### Finding 11: DSPy GEPA — Automatische Prompt-Evolution
- **Quelle:** dspy.ai, github.com/stanfordnlp/dspy
- **Status:** OFFEN — braucht Python-Backend, aber Python jetzt erlaubt
- **Prioritaet:** Mittel-Lang

### Finding 12: AgentFactory — Subagent-Akkumulation
- **Quelle:** arxiv.org/html/2603.18000
- **Status:** BEREITS VORHANDEN — Skills-System erfuellt diese Funktion
- **Prioritaet:** Keine Aktion noetig
