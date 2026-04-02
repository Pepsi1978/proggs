# Forschung — Intelligenz-Steigerung durch wissenschaftliche Erkenntnisse

Zentrale Sammelstelle fuer Paper, Erkenntnisse und daraus abgeleitete Verbesserungen.
Wird von /self-improve Stufe 0b und Stufe 5C gelesen und ausgewertet.

## Neue Forschungsvorschlaege
<!-- Hier kommen Paper und Ideen rein die noch bewertet werden muessen -->
<!-- Format: Titel, Quelle, Kurzbeschreibung, Status (NEU/BEWERTET/VERWORFEN) -->

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
