# Superintelligenz — Implementierungsliste

> Zentrale Sammlung aller Forschungsergebnisse des Superintelligenz-Agenten.
> Diese Datei wird bei jedem Lauf automatisch aktualisiert und dient als:
> 1. **Implementierungs-Tracker** — Was wurde vorgeschlagen, was umgesetzt, was verworfen
> 2. **Duplikat-Filter** — Der Agent liest diese Datei VOR jeder Recherche
> 3. **Fortschritts-Messung** — Wie viele Verbesserungen wurden bisher gefunden und umgesetzt

## Status-Uebersicht

| Direktive | Offene Vorschlaege | Umgesetzt | Verworfen |
|-----------|-------------------|-----------|-----------|
| Superintelligenz | 0 | 4 | 0 |
| Selbstbeobachtung | 0 | 3 | 0 |
| Resilient Bugfixing | 0 | 3 | 0 |
| **Gesamt** | **0** | **10** | **0** |

*Letzte Aktualisierung: 2026-03-31*
*Anzahl Forschungslaeufe: 1*

---

## Direktive 1: Superintelligenz

> Findings die das System EXPONENTIELL intelligenter machen — neue Reasoning-Methoden,
> Compound Intelligence Patterns, kreative Ansaetze aus anderen Disziplinen, kompetitive Analyse.

### Offene Vorschlaege

---

### 🌳 Finding 1: SWE-Search — MCTS-gesteuerter Reasoning-Modus fuer Agenten
**Direktive:** 1: Superintelligenz
**Quelle(n):** [SWE-Search arXiv](https://arxiv.org/abs/2410.20285) | [ICLR 2025 Paper](https://proceedings.iclr.cc/paper_files/paper/2025/file/a1e6783e4d739196cad3336f12d402bf-Paper-Conference.pdf)
**Entdeckt in:** Welle 1 + Welle 2
**Was ist das? (fuer Nicht-Programmierer):**
Statt einer einzigen Loesungslinie probiert der Agent mehrere moegliche Wege gleichzeitig aus — wie ein Schachspieler der 5 Zuege im Kopf durchspielt bevor er einen waehlt. Ein zweiter Agent bewertet jeden Zwischenschritt mit Punkten (Zahlen UND erklaerenden Worten). Ein Schiedsrichter-Agent schliesst bei Uneinigkeit aus mehreren Bewertungen den besten Weg. Das Ergebnis ist 23% bessere Performance bei komplexen Repository-Aufgaben — ohne ein groesseres Modell zu benutzen.

**Was bringt es uns konkret?:**
23% relative Verbesserung bei Software-Aufgaben laut ICLR 2025 Benchmark (SWE-bench). Besonders wirksam bei komplexen Aenderungen die mehrere Dateien betreffen — genau das, wofuer Gemini CLI taeglich eingesetzt wird.

**Aufwand:** 1 Tag

**Umsetzung in Gemini CLI:**
- **Typ:** Agent (neuer `mcts-planner` Agent)
- **Datei(en):** `gemini-setup/agents/mcts-planner.md`
- **Schritte:**
  1. Neuen Agent `mcts-planner` erstellen der bei komplexen Aufgaben (>3 Dateien) automatisch 3 alternative Loesungspfade generiert
  2. Jeden Pfad mit einem Score-System bewerten (numerisch 0-10 + verbale Erklaerung)
  3. Discriminator-Phase: Bei Punktegleichstand kurze interne Debatte zwischen Pfad A und B
  4. Den Gewinner-Pfad ausfuehren
  5. Ergebnisse in MEMORY.md loggen fuer spaetere Lernverbesserung

**Kreativitaets-Bonus:** Der Discriminator-Agent kann mit dem bestehenden `code-reviewer` kombiniert werden — er bewertet BEVOR Code geschrieben wird, nicht nur danach. Das verhindert falsche Richtungen fruehzeitig.
**Abhaengigkeiten:** Keine
**Risiko:** Hoehere Token-Kosten pro Aufgabe (~2-3x). Nur fuer Aufgaben mit >3 Dateien aktivieren.
**Empfehlung:** Bald
**Status:** UMGESETZT (2026-03-31)

---

### 🔧 Finding 2: ToolTree — Intelligente Werkzeug-Reihenfolge-Planung
**Direktive:** 1: Superintelligenz
**Quelle(n):** [ToolTree ICLR 2026](https://arxiv.org/abs/2603.12740)
**Entdeckt in:** Welle 2
**Was ist das? (fuer Nicht-Programmierer):**
Wenn ein Agent viele Werkzeuge hat (Bash, Read, Grep, Edit, Write usw.) waehlt er bisher das naechste Werkzeug spontan — ohne vorher zu denken ob die Reihenfolge optimal ist. ToolTree loest das: Vor jedem Werkzeug-Einsatz schaetzt der Agent schnell ein ob dieses Werkzeug sinnvoll ist (Pre-Check). Nach dem Einsatz bewertet er das Ergebnis (Post-Check). Nutzt dabei das gleiche MCTS-prinzip wie Finding 1. Ergebnis: 10% bessere Performance auf 4 Benchmarks, ohne zusaetzliches Training.

**Was bringt es uns konkret?:**
Weniger "Trial and Error" beim Werkzeug-Einsatz. Seltener das falsche Tool waehlen und dann nochmal von vorne anfangen muessen. Direkte Zeitersparnis bei komplexen Multi-Tool-Aufgaben.

**Aufwand:** 1 Stunde

**Umsetzung in Gemini CLI:**
- **Typ:** Rule (neue Regel in `gemini-setup/rules/tool-planning.md`)
- **Datei(en):** `gemini-setup/rules/tool-planning.md`
- **Schritte:**
  1. Regel erstellen: Vor jeder Tool-Sequenz mit >3 Tools einen kurzen Plan schreiben ("ich werde Read → Grep → Edit → Bash benutzen, weil...")
  2. Nach jedem Tool: 1-Satz-Bewertung ob Ergebnis wie erwartet (Pre/Post-Check)
  3. Bei negativem Post-Check: Alternativen-Pruefung bevor naechster Schritt
  4. Ergebnis-Score am Ende der Aufgabe: "Tool-Effizienz: [Anzahl benoetigte Tools] / [Anzahl geplante Tools]"

**Kreativitaets-Bonus:** Diese Daten koennen in `session-scores.jsonl` gespeichert werden um Trend-Analysen zu machen — welche Aufgabentypen haben schlechte Tool-Effizienz?
**Abhaengigkeiten:** Keine
**Risiko:** Overhead durch Planung bei sehr einfachen Aufgaben (1-2 Tools). Nur bei >3 Tools aktivieren.
**Empfehlung:** Sofort
**Status:** UMGESETZT (2026-03-31)

---

### 🐝 Finding 3: Cascade Hooks Pattern — Pre/Post-Trigger fuer Coding-Standards
**Direktive:** 1: Superintelligenz
**Quelle(n):** [Windsurf Cascade Hooks Doku](https://docs.windsurf.com/windsurf/cascade/hooks)
**Entdeckt in:** Welle 2 + Welle 3
**Was ist das? (fuer Nicht-Programmierer):**
Windsurf (ein Konkurrenz-Tool) hat ein Feature das Gemini CLI noch nicht hat: Hooks die DIREKT an Agent-Aktionen gebunden sind — nicht nur an "pre/post Tool" sondern an "pre_write_code", "pre_run_command", "post_cascade_response" usw. Das Besondere: Ein Hook kann mit Exit-Code 2 die gesamte Aktion BLOCKIEREN bevor sie ausgefuehrt wird. In Gemini CLI gibt es zwar Hooks, aber kein Aktion-spezifisches Blockieren. Windsurf benutzt das fuer automatische Code-Formatierung nach jedem Schreib-Vorgang.

**Was bringt es uns konkret?:**
Konkurrenz-Analyse: Windsurf hat hier einen klaren Vorsprung. Wir koennen das teilweise nachahmen mit den bestehenden Gemini CLI PreToolUse-Hooks — aber die Spezifitaet fehlt. Umsetzen wuerde heissen: Unsere Hooks werden granularer und koennen Code-Standards automatisch durchsetzen.
