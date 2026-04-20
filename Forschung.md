# Forschung — Intelligenz-Steigerung durch wissenschaftliche Erkenntnisse

Zentrale Sammelstelle fuer Paper, Erkenntnisse und daraus abgeleitete Verbesserungen.
Wird von /self-improve Stufe 0b und Stufe 5C gelesen und ausgewertet.

## Neue Forschungsvorschlaege
<!-- Hier kommen Paper und Ideen rein die noch bewertet werden muessen -->
<!-- Format: Titel, Quelle, Kurzbeschreibung, Status (NEU/BEWERTET/VERWORFEN) -->

### [2026-04-12] TraceCoder — Execution-Trace-Debugging (arXiv 2602.06875)
- **Quelle:** arxiv.org/abs/2602.06875
- **Beschreibung:** Multi-Agent: Logging-Sonden einbauen, Laufzeit-Traces aufzeichnen, dann Root Cause analysieren. Hoehere Trefferquote als reines Fehlermeldungs-Debugging.
- **Status:** UMZUSETZEN — Als Erweiterung der Hypothesen-Debugging-Regel (30 Min)

### [2026-04-12] AGENTS.md — Strukturiertes Agent-Context-File (arXiv 2601.20404)
- **Quelle:** arxiv.org/abs/2601.20404
- **Beschreibung:** Empirisch: Agenten mit AGENTS.md brauchen 15-30% weniger Tool-Calls.
- **Status:** UMGESETZT (2026-04-12) — ~/proggs/AGENTS.md erstellt

### [2026-04-12] GitNexus Code-Knowledge-Graph MCP
- **Quelle:** github.com/pebblous-ai/gitnexus + blog.pebblous.ai
- **Beschreibung:** Tree-sitter Code-Graph mit Graph-RAG. Strukturelle Codebase-Navigation statt Text-Suche. Schliesst Luecke zu Windsurf Codemaps.
- **Status:** EVALUIERT — Installation und Test empfohlen (1 Std)

### [2026-04-12] Agent Cognitive Compressor (arXiv 2601.11653)
- **Quelle:** arxiv.org/abs/2601.11653
- **Beschreibung:** Schema-gesteuerter Kontext-Kompressor. Kontext bleibt konstant statt linear zu wachsen. Fundamental fuer lange Sessions.
- **Status:** EVALUIERT — Hoher Aufwand, aber fundamentaler Durchbruch

### [2026-04-12] CVE-2026-35021 — OS-Command-Injection in Claude Code CLI
- **Quelle:** tenable.com/cve/CVE-2026-35021, phoenix.security
- **Beschreibung:** 3er-CVE-Kette (35020/21/22) fuer Credential-Exfiltration via praeparierte Dateipfade. In v2.1.104 gepatcht.
- **Status:** BESTAETIGT — Kein Handlungsbedarf (bereits gepatcht)

### [2026-03-31] Cursor Invariant Sentinel Pattern
- **Quelle:** cursor.com/blog/security-agents
- **Beschreibung:** Taegliche Pruefung aller System-Invarianten gegen definierte Liste. 200+ Vulnerabilities in 3000+ PRs/Woche gefunden.
- **Status:** UMGESETZT (invariant-check.ps1/.sh, 2026-03-31)

### [2026-03-31] claudewatch AgentOps (blackwell-systems)
- **Quelle:** github.com/blackwell-systems/claudewatch
- **Beschreibung:** Echtzeit-Erkennung von Error-Loops und Drift. 29 MCP-Tools fuer mid-session Metriken.
- **Status:** GEPLANT

### [2026-03-31] DebugBase MCP Server
- **Quelle:** github.com/DebugBase/mcp-server
- **Beschreibung:** Kollektive Fehler-Wissensdatenbank. Bei neuem Fehler zuerst nachschlagen ob Loesung bekannt.
- **Status:** GEPLANT

### [2026-03-31] Self-Healing Software Systems (arxiv 2504.20093)
- **Quelle:** arxiv.org/abs/2504.20093
- **Beschreibung:** Biologisches 3-Schichten-Modell: Sensoren → KI-Kern → Heilungs-Agenten.
- **Status:** GEPLANT (Healer-Agent)

### [2026-04-02] ACE — Agentic Context Engineering (ICLR 2026, arxiv 2510.04618)
- **Quelle:** arxiv.org/abs/2510.04618
- **Beschreibung:** Kontext als lebendes "Playbook" das sich selbst verbessert. Generator→Reflector→Curator Loop. +10.6% auf Agenten-Benchmarks, 83.6% weniger Token-Kosten. Direkt auf MEMORY.md-Whiteboard anwendbar.
- **Status:** EVALUIERT — Sofort umsetzbar als Formalisierung des /self-improve Workflows

### [2026-04-02] Code Pathfinder MCP — Semantische Call-Graph-Analyse
- **Quelle:** codepathfinder.dev/mcp + github.com/shivasurya/code-pathfinder
- **Beschreibung:** Open-Source MCP-Server (AGPL-3.0, Anthropic MCP-Registry). Call-Graphs, Datenfluss, Abhaengigkeits-Traces fuer Python. 6 Tools. Setup 5 Min, kostenlos, lokal.
- **Status:** EVALUIERT — Nuetzlich fuer debugger-Agent, aber nur Python-Support

### [2026-04-02] ARIS — Autonome Forschungsschleife (AAAI 2026)
- **Quelle:** github.com/wanshuiyin/Auto-claude-code-research-in-sleep
- **Beschreibung:** Markdown-only Skills fuer autonome ML-Forschung. Cross-Modell Review-Schleifen. Zero Dependencies.
- **Status:** EVALUIERT — Konzept fuer autonome Nacht-Recherchen adaptierbar

### [2026-04-02] Awesome Context Engineering Sammlung
- **Quelle:** github.com/Meirtz/Awesome-Context-Engineering
- **Beschreibung:** 100+ Papers und Implementierungen zu Context Engineering (Write/Select/Compress/Isolate). Nachfolger von Prompt Engineering als Industriestandard 2025-2026.
- **Status:** EVALUIERT — Als Forschungsquelle fuer naechste R8-Laeufe

### [2026-04-04] Cursor Debug Mode 2.2 — Hypothesen-gesteuertes Runtime-Debugging
- **Quelle:** cursor.com/blog/debug-mode + Cursor Changelog 2.2
- **Beschreibung:** Agent formuliert mehrere Hypothesen, instrumentiert Code mit Logging, wartet auf Ausfuehrung, analysiert echte Laufzeitdaten. Gegenteil von Trial-and-Error.
- **Status:** UMZUSETZEN — Als Regel fuer debugger-Agent (30 Min)

### [2026-04-04] Multi-Agent Judging — Automatische Loesungs-Bewertung
- **Quelle:** Cursor 2.2 Changelog + arxiv 2507.21028
- **Beschreibung:** Judge-Agent bewertet parallel erzeugte Loesungen strukturiert nach Korrektheit, Sicherheit, Effizienz. Macht PASS/FAIL auditierbar.
- **Status:** GEPLANT — Quality-Gate um Judge-Phase erweitern (1 Std)

### [2026-04-04] SWE-CI — Dual-Agent CI-Maintenance-Loop (arxiv 2603.03823)
- **Quelle:** arxiv.org/abs/2603.03823
- **Beschreibung:** Architect analysiert CI-Feedback und schreibt Requirements, Programmer implementiert, Schleife bis gruen. Erster Benchmark fuer langfristige Wartbarkeit.
- **Status:** EVALUIERT — Adaptierbar als maintenance-loop Agent

### [2026-04-04] Fault Localization Granularity (arxiv 2604.00167)
- **Quelle:** arxiv.org/abs/2604.00167
- **Beschreibung:** Function-level Fehlerlokalisierung hat hoechste Repair-Rate. File-level zu ungenau, line-level verliert Kontext.
- **Status:** UMGESETZT — In resilient-bugfixing.md als Regel hinzugefuegt (2026-04-04)

### [2026-04-04] SWE-Bench Pro — Long-Horizon Benchmark (arxiv 2509.16941)
- **Quelle:** arxiv.org/abs/2509.16941
- **Beschreibung:** Neues Schwierigkeits-Level ueber SWE-Bench Verified. Testet langfristige Softwareentwicklung. Claude fuehrt mit 45-48%.
- **Status:** EVALUIERT — Als Benchmark-Referenz behalten

### [2026-04-20] KGCompass — Repository-Wissensgraph fuer Bug-Lokalisierung (arXiv 2503.21710)
- **Quelle:** arxiv.org/abs/2503.21710
- **Beschreibung:** Baut Wissensgraph ueber Repository (Issues, PRs, Dateien, Funktionen) und nutzt Multi-Hop-Traversierung fuer Bug-Lokalisierung. 58.3% SWE-bench Lite, 89.7% der Lokalisierungen ohne expliziten Ortshinweis in der Fehlermeldung. Kosten: 0.20 USD pro Reparatur.
- **Status:** UMZUSETZEN — Im debugger-Agent als Pre-Debug-Recherche-Schritt integrieren (1 Tag)

### [2026-04-20] When-To-Verify — Optimale Compute-Aufteilung (arXiv 2504.01005, COLM 2026)
- **Quelle:** arxiv.org/abs/2504.01005
- **Beschreibung:** Bei festem Token-Budget: Mehr Loesungen generieren (Self-Consistency) vs. weniger Loesungen aber intensiver verifizieren. Bei schwierigen Aufgaben zahlt sich Verifikation deutlich mehr aus als weitere Loesungsversuche.
- **Status:** UMZUSETZEN — Heuristik im quality-gate Agent (30 Minuten)

### [2026-04-20] Cursor 3 — Design Mode + Cloud Agents
- **Quelle:** cursor.com/blog/cursor-3 (02.04.2026)
- **Beschreibung:** Design Mode fuer visuelle UI-Inspektion statt Selektoren raten. Cloud Agents via Slack/GitHub. Uebertragbar: Pre-UI-Fix Screenshot + adb uiautomator dump als strukturierte Inspektion im ui-polisher.
- **Status:** GEPLANT — Screenshot-Pre-Flight fuer ui-polisher Agent (1 Std)

### [2026-04-20] Swarm-SuperBrain — Alignment-Schicht fuer Agent-Schwarm (arXiv 2509.00510)
- **Quelle:** arxiv.org/html/2509.00510v1
- **Beschreibung:** Subclass Brains unter Swarm Alignment Layer koordiniert. Emergente Intelligenz aus lokalen Agent-Interaktionen ohne zentrale Kontrolle. Biologisch inspiriert (Ameisen, Bienen).
- **Status:** EVALUIERT — Grundlage fuer den offenen Debate-Loop (Stronger-MAS) im Whiteboard

### [2026-04-20] Fault-Localization-Context > Modellgroesse (arXiv 2604.05481)
- **Quelle:** arxiv.org/abs/2604.05481
- **Beschreibung:** Empirische Studie April 2026: Kontext-Qualitaet hat groesseren Einfluss auf Repair als Modell-Auswahl. Call-Stack + letzter gruener Test + betroffene Funktion als Pflicht-Kontext-Block.
- **Status:** UMZUSETZEN — Prompt-Erweiterung im debugger-Agent (30 Min)

### [2026-04-20] Cursor 3 Cost-Aware 3-Tier Routing
- **Quelle:** R3 Parallel-Orchestration Recherche
- **Beschreibung:** Aufgaben-Klassifikation simple (Haiku) / medium (Sonnet) / hard (Opus). Datenbasiert statt statisch. Laut Forschung: 58% Kostenersparnis bei kaum Qualitaetsverlust.
- **Status:** EVALUIERT — Erweiterung der Speed-Tiers in CLAUDE.md (Prompt-Anpassung in coder-Agent)

## Abgeleitete Intelligenz-Vorschlaege
<!-- Konkrete Aktionen die aus der Forschung abgeleitet wurden -->

### Auto-Redaction Pipeline (abgeleitet aus Push-Protection-Vorfall 2026-03-31)
- **Aktion:** Pre-Commit-Hook der Secrets aus settings-reference.json entfernt
- **Status:** GEPLANT

### Compound-Effect-Reminder (abgeleitet aus Stagnations-Analyse 2026-03-31)
- **Aktion:** SessionEnd-Hook der prueft ob Compound Effects dokumentiert wurden
- **Status:** GEPLANT

## Umgesetzte Intelligenz-Steigerungen
<!-- Paper/Ideen die erfolgreich implementiert wurden -->

### [2026-03-31] Invariant Sentinel Pattern → invariant-check Hook
- **Paper:** Cursor Invariant Sentinel
- **Umsetzung:** 5 System-Invarianten werden bei jedem SessionStart geprueft
- **Ergebnis:** Stale Issues werden sofort sichtbar, "Erkennungs-ohne-Heilung-Muster" adressiert

## Compound Effect Erfolge
<!-- Dokumentation der exponentiellen Verbesserungskette -->

### [2026-03-31] Stagnation → Forschung → Invariant-Check → Fehlerklasse eliminiert
Kette: Evolution-Analyst bemerkt 9-Tage-Stagnation → R8 findet Cursor-Pattern → Hook gebaut → Stale Issues werden nie mehr uebersehen.

### [2026-04-02] Meta-Intelligence-Kollaps → Bug-Fix → Fehlerklasse eliminiert
Kette: Evolution-Analyst findet Meta-Intelligence-Abfall (50%→10%) → hyperagent-stop.sh Code inspiziert → Stale-Goal-Bug gefunden (exit 0 statt goal="") → Fix + Schwellen angepasst → Alle Sessions bekommen metacognitiven Prompt.

### [2026-04-20] SubagentStop-Endlosschleife → Input-Validation-Regel → Fehlerklasse eliminiert
Kette: Evolution-Analyst identifiziert strukturelle Schwaeche "fehlende Hook-Input-Validation" → R7 Focus-Researcher findet exakte Doku zu `agent_id`-Feld → 4 Hooks gefixt (memory-watchdog.ps1/.sh + writeback-enforcer.ps1/.sh mit agent_id Guard) → Neue Regel `hook-input-validation.md` dokumentiert das Muster fuer ALLE zukuenftigen Hooks → Poka-Yoke Stufe 3: Der Phantom-Fire-Fehler kann konzeptionell nicht mehr auftreten wenn die Regel eingehalten wird.
