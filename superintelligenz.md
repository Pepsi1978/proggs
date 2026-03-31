# Superintelligenz — Implementierungsliste

> Zentrale Sammlung aller Forschungsergebnisse des Superintelligenz-Agenten.
> Diese Datei wird bei jedem Lauf automatisch aktualisiert und dient als:
> 1. **Implementierungs-Tracker** — Was wurde vorgeschlagen, was umgesetzt, was verworfen
> 2. **Duplikat-Filter** — Der Agent liest diese Datei VOR jeder Recherche
> 3. **Fortschritts-Messung** — Wie viele Verbesserungen wurden bisher gefunden und umgesetzt

## Status-Uebersicht

| Direktive | Offene Vorschlaege | Umgesetzt | Verworfen | Bereits vorhanden |
|-----------|-------------------|-----------|-----------|-------------------|
| Superintelligenz | 1 | 4 | 1 | 1 |
| Selbstbeobachtung | 0 | 3 | 1 | 1 |
| Resilient Bugfixing | 0 | 4 | 1 | 0 |
| **Gesamt** | **1** | **11** | **3** | **2** |

*Letzte Aktualisierung: 2026-03-31 (Lauf 2: macOS-Fokus)*
*Anzahl Forschungslaeufe: 2*

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
23% relative Verbesserung bei Software-Aufgaben laut ICLR 2025 Benchmark (SWE-bench). Besonders wirksam bei komplexen Aenderungen die mehrere Dateien betreffen — genau das, wofuer Claude Code taeglich eingesetzt wird.

**Aufwand:** 1 Tag

**Umsetzung in Claude Code:**
- **Typ:** Agent (neuer `mcts-planner` Agent)
- **Datei(en):** `~/.claude/agents/mcts-planner.md`
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
Wenn ein Agent viele Werkzeuge hat (Bash, Read, Grep, Edit, Write usw.) waehlt er bisher das naechste Werkzeug spontan — ohne vorher zu denken ob die Reihenfolge optimal ist. ToolTree loest das: Vor jedem Werkzeug-Einsatz schaetzt der Agent schnell ein ob dieses Werkzeug sinnvoll ist (Pre-Check). Nach dem Einsatz bewertet er das Ergebnis (Post-Check). Nutzt dabei das gleiche MCTS-Prinzip wie Finding 1. Ergebnis: 10% bessere Performance auf 4 Benchmarks, ohne zusaetzliches Training.

**Was bringt es uns konkret?:**
Weniger "Trial and Error" beim Werkzeug-Einsatz. Seltener das falsche Tool waehlen und dann nochmal von vorne anfangen muessen. Direkte Zeitersparnis bei komplexen Multi-Tool-Aufgaben.

**Aufwand:** 1 Stunde

**Umsetzung in Claude Code:**
- **Typ:** Rule (neue Regel in `~/.claude/rules/tool-planning.md`)
- **Datei(en):** `~/.claude/rules/tool-planning.md`
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
Windsurf (ein Konkurrenz-Tool) hat ein Feature das Claude Code noch nicht hat: Hooks die DIREKT an Agent-Aktionen gebunden sind — nicht nur an "pre/post Tool" sondern an "pre_write_code", "pre_run_command", "post_cascade_response" usw. Das Besondere: Ein Hook kann mit Exit-Code 2 die gesamte Aktion BLOCKIEREN bevor sie ausgefuehrt wird. In Claude Code gibt es zwar Hooks, aber kein Aktion-spezifisches Blockieren. Windsurf benutzt das fuer automatische Code-Formatierung nach jedem Schreib-Vorgang.

**Was bringt es uns konkret?:**
Konkurrenz-Analyse: Windsurf hat hier einen klaren Vorsprung. Wir koennen das teilweise nachahmen mit den bestehenden Claude Code PreToolUse-Hooks — aber die Spezifitaet fehlt. Umsetzen wuerde heissen: Unsere Hooks werden granularer und koennen Code-Standards automatisch durchsetzen.

**Aufwand:** 1 Tag

**Umsetzung in Claude Code:**
- **Typ:** Hook-Erweiterung
- **Datei(en):** `~/.claude/hooks/post-edit-format.ps1` + `~/.claude/hooks/post-edit-format.sh`
- **Schritte:**
  1. PostToolUse-Hook fuer `Write` und `Edit` Tools erstellen
  2. Hook prueft Datei-Extension und fuehrt passenden Formatter aus (Prettier fuer JS/TS, Black fuer Python, ktlint fuer Kotlin)
  3. Hook loggt Formatter-Ergebnis ins Whiteboard
  4. Bei Formatter-Fehler: Fehler ins Whiteboard loggen (nicht stumm verschlucken)

**Kreativitaets-Bonus:** Der Kotlin/Android-Fokus dieses Projekts macht ktlint besonders wertvoll. Jede Kotlin-Datei wird nach jedem Edit automatisch lint-geprueft — kein manuelles "vergiss den lint nicht" mehr.
**Abhaengigkeiten:** Keine
**Risiko:** Langsamer bei grossen Dateien. Formatter muss installiert sein (ktlint, prettier).
**Empfehlung:** Bald
**Status:** UMGESETZT (2026-03-31)

---

### 🧠 Finding 4: Swarm-Muster fuer unabhaengige Teilaufgaben
**Direktive:** 1: Superintelligenz
**Quelle(n):** [Frontiers: MAS + LLM Swarm Intelligence 2025](https://www.frontiersin.org/journals/artificial-intelligence/articles/10.3389/frai.2025.1593017/full)
**Entdeckt in:** Welle 1
**Was ist das? (fuer Nicht-Programmierer):**
Ameisenkolonien legen Pheromone auf den kuerzesten Weg zum Futter — je mehr Ameisen, desto staerker das Signal, desto haeufiger wird der Weg gewaehlt. Uebertraegen auf Agenten: Wenn Agent A einen guten Loesungsweg findet, kann er das in einem geteilten Speicher markieren. Agent B liest den Speicher und bevorzugt bereits "bewaehrte" Pfade. Das ermoeglicht echtes kollektives Lernen ohne dass die Agenten direkt miteinander reden muessen.

**Was bringt es uns konkret?:**
Unser bestehendes MEMORY.md Whiteboard ist bereits ein einfaches "Pheromon"-System. Wenn wir Erfolgs-Muster explizit markieren ("dieser Loesungsweg hat bei aehnlichen Aufgaben funktioniert") koennen parallele Agents schneller zu guten Loesungen konvergieren.

**Aufwand:** 30 Min

**Umsetzung in Claude Code:**
- **Typ:** Rule + Memory-Erweiterung
- **Datei(en):** `~/.claude/rules/swarm-success-tracking.md`, `.claude/agent-memory/shared/MEMORY.md`
- **Schritte:**
  1. Neue Sektion in MEMORY.md: "Bewaehrte Loesungsmuster" (Pheromon-Tabelle)
  2. Regel: Nach erfolgreicher komplexer Aufgabe (>5 Tool-Calls) kurzes Pattern in der Tabelle eintragen: Aufgabentyp + verwendeter Ansatz + Erfolgs-Score
  3. Regel: Vor komplexen Aufgaben die Pheromon-Tabelle lesen und aehnliche Patterns bevorzugen
  4. Tabelle wird automatisch bei /self-improve bereinigt (veraltete Patterns entfernen)

**Kreativitaets-Bonus:** Verbindung zu Trae Ensemble-Reasoning — die Pheromon-Tabelle kann als Vorauswahl fuer den 3-Pfade-Vergleich dienen.
**Abhaengigkeiten:** Keine
**Risiko:** Tabelle kann veralten. Braucht regelmaessige Bereinigung durch /self-improve.
**Empfehlung:** Sofort
**Status:** UMGESETZT (2026-03-31)

---

---

### 🍎 Finding 11: SwiftLens MCP — Compiler-genaue Swift-Code-Analyse fuer AI-Agenten
**Direktive:** 1: Superintelligenz
**Quelle(n):** [SwiftLens GitHub](https://github.com/swiftlens/swiftlens) | [SwiftLens auf LobeHub](https://lobehub.com/mcp/swiftlens-swiftlens) | [PulseMCP Eintrag](https://www.pulsemcp.com/servers/swift-sourcekit-lsp)
**Entdeckt in:** Welle 1 (macOS-Fokus)
**Was ist das? (fuer Nicht-Programmierer):**
Wenn man Claude Code bittet, eine Swift-Datei zu aendern, sieht es den Code wie ein Texteditor: als Buchstaben und Woerter. SwiftLens gibt Claude eine viel tiefere Sicht — wie Xcode selbst. Es verbindet sich mit Apples eigenem Analyse-Werkzeug (SourceKit-LSP) und versteht Swift-Code auf Compiler-Ebene: wo wird eine Funktion aufgerufen, welche Typen sind kompatibel, wo gibt es Fehler. Damit kann Claude nicht nur Code schreiben sondern ihn wirklich VERSTEHEN.

**Was bringt es uns konkret?:**
Fuer das BestJournal-Projekt (Android in Kotlin) gibt es kein direktes Aequivalent — aber fuer jeden Swift/SwiftUI-Code auf macOS wird Claude zum Experten. Statt zu raten ob ein Refactoring funktioniert, weiss Claude durch SourceKit-LSP-Rueckmeldungen sofort ob Typen stimmen, ob Referenzen korrekt sind, und wo andere Stellen im Code angepasst werden muessen. Das eliminiert eine ganze Klasse von "das compile ich mal und schaue ob es klappt" Fehlern.

**Aufwand:** 30 Min

**Umsetzung in Claude Code:**
- **Typ:** MCP-Server (Installation)
- **Datei(en):** `~/proggs/.mcp.json` (macOS-Eintrag hinzufuegen)
- **Schritte:**
  1. SwiftLens installieren: `pip install swiftlens`
  2. Swift-Projekte mit Index-Store bauen: `swift build -Xswiftc -index-store-path -Xswiftc .build/index/store`
  3. MCP-Eintrag in `~/proggs/.mcp.json` hinzufuegen fuer macOS (Windows-Eintrag NICHT erstellen — macOS-only)
  4. In `.mcp.json` unter einem macOS-Conditional-Kommentar dokumentieren warum nur macOS
  5. Testen: Claude bitten eine Swift-Funktion zu finden, die eine bestimmte andere Funktion aufruft

**Kreativitaets-Bonus:** SwiftLens laeuft komplett lokal — keine Cloud, keine Datenexfiltration. Das ist eine macOS-native Defense-in-Depth fuer Swift-Code: Fehler werden durch semantisches Verstaendnis verhindert, nicht nur durch Testen.
**Abhaengigkeiten:** Xcode muss installiert sein (SourceKit-LSP ist im Swift-Toolchain enthalten)
**Risiko:** Nur fuer Swift-Code nuetzlich. Index muss nach Builds aktualisiert werden. macOS-only — keine Windows-Entsprechung moeglich.
**Empfehlung:** Bald (wenn naechstes Swift-Projekt aktiv)
**Status:** BEREITS VORHANDEN (swift-lsp@claude-plugins-official Plugin ist in settings.json aktiviert)

---

### 🔨 Finding 12: Hammerspoon — macOS Automation als Claude-Code-Superkraft
**Direktive:** 1: Superintelligenz
**Quelle(n):** [Hammerspoon GitHub](https://github.com/Hammerspoon/hammerspoon) | [Hammerspoon Skill auf MCPMarket](https://mcpmarket.com/tools/skills/hammerspoon-macos-automation) | [AI + Hammerspoon Medium](https://medium.com/@business_37716/revolutionizing-workflow-automation-with-ai-and-hammerspoon-a834b8cf54f6)
**Entdeckt in:** Welle 1 (macOS-Fokus)
**Was ist das? (fuer Nicht-Programmierer):**
Hammerspoon ist ein kostenloses macOS-Werkzeug das Lua-Skripte direkt mit dem Betriebssystem verbindet — Fensterpositionen, Tastenkuerzel, laufende Programme, Klemmbrett, Bildschirm-Layouts, Mikrofon und mehr. Es gibt bereits einen fertigen Claude-Code-Skill dafuer. Wenn Claude Code diesen Skill kennt, kann es auf Anfrage komplexe macOS-Automatisierungen bauen — zum Beispiel "positioniere das Terminal-Fenster links oben und den Browser rechts" oder "wenn ich Control+Shift+C druecke, oeffne Claude Code im Hintergrund".

**Was bringt es uns konkret?:**
Claude Code kann nicht nur Code schreiben sondern auch die macOS-Arbeitsumgebung des Benutzers intelligent gestalten. Aufgaben wie "richte meinen Entwickler-Desktop automatisch ein" oder "erstelle Tastenkuerzel fuer meine haeufigsten Claude-Aktionen" werden in Minuten erledigt statt in Stunden.

**Aufwand:** 30 Min

**Umsetzung in Claude Code:**
- **Typ:** Skill (Installation eines bestehenden Skills)
- **Datei(en):** `~/.claude/` (Skill wird ueber Claude Code Marketplace installiert)
- **Schritte:**
  1. Hammerspoon App installieren: `brew install --cask hammerspoon`
  2. Skill installieren via Claude Code: `/install-skill plinde/claude-plugins/hammerspoon`
  3. Skill testen: Claude bitten ein einfaches Window-Layout Skript zu erstellen
  4. Hammerspoon Spoon fuer Development-Layout erstellen: Terminal links, Browser rechts, Claude Code Mitte

**Kreativitaets-Bonus:** Hammerspoon kann auf ANDERE macOS-Events reagieren: wenn ein bestimmter Prozess crashed, wenn ein Build fertig ist, wenn das Netzwerk sich veraendert. Das macht es zu einem natuerlichen Begleiter fuer Claude Code Hooks — Hammerspoon auf dem Mac-Level, Claude Hooks auf dem Agent-Level.
**Abhaengigkeiten:** Hammerspoon muss installiert sein (kostenlos, Open Source)
**Risiko:** Lua-Skripte koennen die macOS-Umgebung stark veraendern. Jeden Hammerspoon-Befehl vor Ausfuehren mit dem Benutzer abklaren.
**Empfehlung:** VERWORFEN — macOS Sequoia hat eingebautes Window Tiling, Lua-Konfiguration fuer Nicht-Programmierer unzugaenglich
**Status:** VERWORFEN (2026-03-31) — Alltagstauglichkeits-Pruefung: dauerhaft RAM-Verbrauch, Accessibility-Permission, macOS hat die Kernfunktion schon eingebaut

---

### 🏗️ Finding 13: Apple Foundation Models Framework — Kostenloser lokaler 3B-LLM fuer macOS
**Direktive:** 1: Superintelligenz
**Quelle(n):** [Apple Newsroom: Foundation Models Framework](https://www.apple.com/newsroom/2025/09/apples-foundation-models-framework-unlocks-new-intelligent-app-experiences/) | [Apple Developer Docs](https://developer.apple.com/documentation/FoundationModels) | [WWDC25 Video](https://developer.apple.com/videos/play/wwdc2025/286/)
**Entdeckt in:** Welle 1 (macOS-Fokus)
**Was ist das? (fuer Nicht-Programmierer):**
Apple hat in macOS 26 ein komplett kostenloses 3-Milliarden-Parameter KI-Modell eingebaut, das direkt auf dem Geraet laeuft — ohne Internet, ohne Kosten, ohne Datenweitergabe. Dieses Modell kann Texte zusammenfassen, strukturierte Daten erzeugen (JSON-Ausgabe mit Swift-Typen), und eigene Werkzeuge aufrufen. Es ist mit wenigen Swift-Codezeilen nutzbar. Fuer Entwickler interessant: Bei Aufgaben die kleine und schnelle KI-Unterstuetzung brauchen (Kategorisieren, Kurzfassungen, einfache Entscheidungen) koennte dieses lokale Modell Claude-API-Anfragen ersetzen.

**Was bringt es uns konkret?:**
Fuer Hooks, Skripte und einfache Automatisierungen auf macOS koennte das Foundation Models Framework einen kostenfreien "kleinen Bruder" von Claude bereitstellen. Wenn ein Hook entscheiden muss ob ein Fehler kritisch oder harmlos ist, muss er dafuer kein API-Guthaben verbrauchen — er fragt das lokale Modell. Das reduziert API-Kosten und erhoht die Autonomie der Programmierumgebung.

**Aufwand:** 1 Tag (Swift-Wrapper bauen)

**Umsetzung in Claude Code:**
- **Typ:** Config + neues Swift-Tool (macOS-only)
- **Datei(en):** Neues Swift-Skript `~/proggs/tools/local-classifier.swift` als ausfuehrbares CLI
- **Schritte:**
  1. Voraussetzung pruefen: macOS 26+ und Apple-Intelligence-kompatibles Geraet (M1+ mit 8GB RAM)
  2. Swift-CLI-Tool erstellen: nimmt einen String als Input, gibt Klassifizierung zurueck (z.B. "harmlos/kritisch/unklar")
  3. In Hooks verwenden: Bei bestimmten Fehlermeldungen zuerst lokales Modell befragen, dann nur bei "kritisch" oder "unklar" eine Whiteboard-Meldung schreiben
  4. Build-Skript erstellen: `swift build -c release && cp .build/release/local-classifier ~/proggs/tools/`
  5. Hook-Integration: PostToolUse-Hook kann lokalen Classifier aufrufen

**Kreativitaets-Bonus:** Das Foundation Models Framework unterstuetzt Tool Calling — das lokale Modell kann selbst bestimmen welche Daten es braucht. Kombiniert mit einem MCP-Server waere ein vollstaendig lokaler, kostenfreier AI-Assistent fuer einfache macOS-Aufgaben moeglich: die KI-Variante eines "Automator"-Workflows.
**Abhaengigkeiten:** macOS 26+ (Sequoia Nachfolger) auf Apple Silicon, Apple Intelligence aktiviert
**Risiko:** Modell ist klein (3B Parameter) — bei komplexen Aufgaben unzuverlaessig. Nur fuer einfache Klassifizierungen und Zusammenfassungen einsetzen, nicht fuer Coding-Aufgaben.
**Empfehlung:** Spaeter (wenn macOS 26 verfuegbar)
**Status:** OFFEN

---

### Umgesetzte Vorschlaege

*Noch keine umgesetzten Vorschlaege. Wenn ein Vorschlag implementiert wird, hierher verschieben
mit Datum der Umsetzung und Link zum Commit.*

### Verworfene Vorschlaege

*Noch keine verworfenen Vorschlaege.*

---

## Direktive 2: Selbstbeobachtung

> Findings die dem System helfen sich SELBST besser zu beobachten und daraus zu lernen —
> Metacognition, Confidence-Tracking, Drift-Detection, Reflexion-Architekturen.

### Offene Vorschlaege

---

### 🔍 Finding 5: Confidence-Tracking — Unsicherheits-Ampel pro Antwort
**Direktive:** 2: Selbstbeobachtung
**Quelle(n):** [Uncertainty Quantification Survey arXiv 2025](https://arxiv.org/abs/2503.15850) | [ICLR 2025: Do LLMs Estimate Uncertainty Well](https://proceedings.iclr.cc/paper_files/paper/2025/file/ef472869c217bf693f2d9bbde66a6b07-Paper-Conference.pdf)
**Entdeckt in:** Welle 2
**Was ist das? (fuer Nicht-Programmierer):**
LLMs wissen oft nicht wie sicher sie sind — sie behaupten Dinge mit dem gleichen Ton egal ob sie sicher oder unsicher sind. Neue Forschung zeigt: Durch Reinforcement Learning kann man Modelle trainieren kalibrierte Unsicherheit zu verbessern. Fuer uns ohne Training: Man kann einen einfachen "Sampling-Test" machen — die gleiche Frage 3x stellen und schauen ob die Antworten konsistent sind. Hohe Varianz = niedrige Sicherheit.

**Was bringt es uns konkret?:**
Wenn ein Agent seine Unsicherheit kennt, kann er automatisch entscheiden: "Ich bin nur 40% sicher — ich sollte nachschlagen statt raten." Das verhindert selbstbewusste Falschaussagen die dann als Bugs enden.

**Aufwand:** 30 Min

**Umsetzung in Claude Code:**
- **Typ:** Rule (Pflicht-Selbst-Check)
- **Datei(en):** `~/.claude/rules/confidence-check.md`
- **Schritte:**
  1. Neue Regel: Bei Aussagen ueber Bibliotheks-Versionen, API-Details, Datei-Pfade IMMER zuerst nachschlagen statt aus dem Gedaechtnis antworten
  2. Ampel-System im eigenen Denken: Gruen = aus frischer Quelle gelesen, Gelb = aus Erinnerung, Rot = Vermutung → Rot immer explizit markieren
  3. Wenn Rot: Sofort WebSearch oder Datei-Read ausfuehren bevor weitergemacht wird
  4. Regel gilt besonders fuer: Versionen, Pfade, API-Signaturen, Kotlin/Android-Spezifika

**Kreativitaets-Bonus:** Dieses System ist ein direkter Schutz gegen die haeufigste Fehlerquelle: selbstbewusstes Falschraten bei technischen Details. Verbindet sich gut mit der "Inspect-before-guessing" Regel die bereits existiert.
**Abhaengigkeiten:** Keine
**Risiko:** Kann zu mehr Tool-Calls fuehren. Nur bei technisch prazisen Details anwenden, nicht bei allgemeinen Konzepten.
**Empfehlung:** Sofort
**Status:** UMGESETZT (2026-03-31)

---

### 📚 Finding 6: Case-Based Reasoning (CBR) — Fehler-Datenbank fuer Agenten
**Direktive:** 2: Selbstbeobachtung
**Quelle(n):** [CBR for LLM Agents Review 2025](https://arxiv.org/abs/2504.06943)
**Entdeckt in:** Welle 3
**Was ist das? (fuer Nicht-Programmierer):**
Ein erfahrener Arzt erinnert sich: "Dieses Symptom hatte ich schon mal — da war die Ursache X." Das nennt sich Case-Based Reasoning — Probleme loesen indem man sich an aehnliche vergangene Faelle erinnert. Fuer Agenten: Jedes Mal wenn ein Bug gefixt wird, wird der Fall gespeichert (Symptom + Ursache + Loesung). Beim naechsten aehnlichen Bug werden die gespeicherten Faelle zuerst durchsucht. Das Ergebnis: Schnellere Diagnose, keine wiederholten Fehler.

**Was bringt es uns konkret?:**
Das Whiteboard hat bereits eine "Offene Fehler" Sektion — aber kein strukturiertes Retrieval. Ein CBR-System wuerde bedeuten: Beim neuen Fehler werden automatisch alle aehnlichen alten Faelle gezeigt. Das transformiert das Whiteboard von einem statischen Log zu einem aktiven Lern-Werkzeug.

**Aufwand:** 1 Tag

**Umsetzung in Claude Code:**
- **Typ:** Agent-Upgrade + Memory-Erweiterung
- **Datei(en):** `.claude/agent-memory/shared/bug-cases.jsonl` (neue Datei), `~/.claude/rules/cbr-search.md`
- **Schritte:**
  1. Neue Datei `bug-cases.jsonl` erstellen: Jeder Eintrag hat Felder: symptom, root_cause, fix, files_changed, tags, date
  2. Regel: Nach jedem Bugfix einen Eintrag in bug-cases.jsonl schreiben
  3. Regel: Beim naechsten Fehler zuerst bug-cases.jsonl nach Symptom-Match durchsuchen (grep-basiert reicht)
  4. Bei Match: Den alten Fall als ersten Loesungsansatz zeigen
  5. /self-improve liest bug-cases.jsonl und aggregiert Muster (z.B. "5 Fehler hatten Root Cause: fehlender UTF-8 Encoding-Parameter")

**Kreativitaets-Bonus:** Die 4 Phasen des CBR (Retrieve → Reuse → Revise → Retain) sind fast identisch mit dem bestehenden resilient-bugfixing-Workflow. CBR formalisiert und automatisiert ihn nur. Minimale Aenderung, maximaler Effekt.
**Abhaengigkeiten:** Keine
**Risiko:** Qualitaet der Eintraege muss gut sein. Schlechte Eintraege fuehren zu falschen Hinweisen.
**Empfehlung:** Bald
**Status:** UMGESETZT (2026-03-31)

---

### 🪞 Finding 7: Metacognitive Self-Monitoring — "Innerer Beobachter" Protokoll
**Direktive:** 2: Selbstbeobachtung
**Quelle(n):** [Metacognitive AI Teaching Your Agent Self-Evaluation](https://blakecrosley.com/blog/metacognitive-ai-programming) | [Language Models Capable of Metacognitive Monitoring](https://arxiv.org/html/2505.13763v2)
**Entdeckt in:** Welle 1
**Was ist das? (fuer Nicht-Programmierer):**
Ein erfahrener Koch schmeckt das Gericht waehrend des Kochens — nicht erst wenn es auf dem Tisch steht. Neueste Forschung zeigt: LLMs koennen sich selbst "introspektieren" — sie koennen ihren eigenen Zustand beobachten wenn man sie dazu auffordert. Ein einfaches Protokoll: Am Anfang komplexer Aufgaben eine kurze "Metaziel-Aussage" formulieren ("was genau versuche ich gerade?") und am Ende pruefen ob das erreicht wurde.

**Was bringt es uns konkret?:**
Unsere Intent-Tracking Regel existiert bereits — sie prueft ob man vom urspruenglichen Ziel abgedriftet ist. Aber der Mechanismus ist passiv (nur wenn die Erinnerungsdatei liest). Ein aktives Metacognition-Protokoll wuerde bedeuten: Der Agent haelt an bestimmten Punkten inne und fragt sich selbst: "Bin ich noch auf dem richtigen Weg?"

**Aufwand:** 30 Min

**Umsetzung in Claude Code:**
- **Typ:** Rule-Erweiterung
- **Datei(en):** Erweiterung der `~/.claude/rules/intent-tracking.md`
- **Schritte:**
  1. Bestehende Intent-Tracking Regel erweitern: Alle 5 Tool-Calls eine kurze Selbst-Pruefung: "Mein aktuelles Ziel war X — befinde ich mich noch auf diesem Weg?"
  2. Wenn Abweichung erkannt: Explizit stoppen und begruenden warum die neue Richtung besser ist (oder zurueckkehren)
  3. Am Ende jeder Aufgabe: 1-Satz-Selbst-Bewertung: "Was habe ich gelernt / was haette ich besser machen koennen?"
  4. Diese Selbst-Bewertungen in MEMORY.md unter "Session-Learnings" loggen

**Kreativitaets-Bonus:** Verbindet Intent-Tracking (verhindert Drift) mit Selbstbeobachtungs-Direktive (lernt aus der Arbeit). Zwei bestehende Systeme werden zu einem kohaerenteren Ganzen.
**Abhaengigkeiten:** Keine (Erweiterung bestehender Regel)
**Risiko:** Kann Aufgaben verlangsamen wenn zu haeufig pausiert wird. Rhythmus sorgfaeltig waehlen.
**Empfehlung:** Sofort
**Status:** UMGESETZT (2026-03-31)

---

---

### 🔔 Finding 14: macOS Notification Hooks — Native System-Alerts fuer Agent-Events
**Direktive:** 2: Selbstbeobachtung
**Quelle(n):** [Claude Code Notifications Blog](https://khromov.se/claude-code-hooks-for-simple-macos-notifications/) | [Claude Code Notifications Tutorial](https://nakamasato.medium.com/claude-code-hooks-automating-macos-notifications-for-task-completion-42d200e751cc) | [macOS Notification MCP](https://github.com/devizor/macos-notification-mcp)
**Entdeckt in:** Welle 1 + Welle 2 (macOS-Fokus)
**Was ist das? (fuer Nicht-Programmierer):**
Wenn Claude Code eine lange Aufgabe beendet, sieht der Benutzer es erst wenn er ins Terminal schaut. Auf macOS koennen Hooks so eingerichtet werden dass bei wichtigen Agent-Events (Aufgabe fertig, Fehler aufgetreten, Input noetig) automatisch eine native macOS-Benachrichtigung aufpoppt — mit Sound und Banner, genau wie eine App-Benachrichtigung. Drei Werkzeuge stehen bereit: `afplay` fuer Sound, `osascript` fuer Benachrichtigungen, oder das macOS Notification MCP fuer eine vollstaendige Integration. Das System beobachtet sich selbst und meldet seinen Status proaktiv.

**Was bringt es uns konkret?:**
Der Benutzer muss nicht mehr staendig das Terminal beobachten. Claude kann bei langen Aufgaben (Build, Recherche, Self-Improve) komplett im Hintergrund laufen und meldet sich nur wenn seine Arbeit erledigt ist oder wenn er eine Entscheidung braucht. Das ist Selbstbeobachtung die nach aussen kommuniziert wird.

**Aufwand:** 30 Min

**Umsetzung in Claude Code:**
- **Typ:** Hook (2 neue Hooks, macOS-only .sh-Dateien)
- **Datei(en):** `~/.claude/hooks/notify-stop.sh`, `~/.claude/hooks/notify-input-needed.sh`
- **Schritte:**
  1. Stop-Hook erstellen: Bei jedem Agent-Stop einen Sound abspielen und Banner zeigen
     ```bash
     #!/bin/bash
     afplay /System/Library/Sounds/Glass.aiff
     osascript -e 'display notification "Claude ist fertig" with title "Claude Code" sound name "Glass"'
     ```
  2. Input-Hook erstellen: Wenn Claude wartet, anderen Sound und andere Meldung
     ```bash
     #!/bin/bash
     afplay /System/Library/Sounds/Ping.aiff
     osascript -e 'display notification "Claude braucht Eingabe" with title "Claude Code" sound name "Ping"'
     ```
  3. Hooks in `~/.claude/settings.json` registrieren (Stop und PreToolUse mit input-type filter)
  4. Optional: macOS Notification MCP installieren fuer Antwort-Funktion (Benutzer antwortet direkt aus der Benachrichtigung)

**Kreativitaets-Bonus:** Unterschiedliche Sounds fuer unterschiedliche Events — Glass fuer Erfolg, Sosumi fuer Fehler, Ping fuer Input-noetig. Das Betriebssystem selbst kommuniziert den Agent-Status. Genau das macht macOS-nativ: Audio-Feedback als Informationskanal, nicht nur visuell.
**Abhaengigkeiten:** Keine (afplay und osascript sind auf jedem Mac vorinstalliert)
**Risiko:** osascript braucht Notification-Berechtigung im System (einmalige macOS-Anfrage). Kein Windows-Gegenstueck noetig (Windows-Hooks bereits vorhanden via .ps1).
**Empfehlung:** Sofort — einfachstes macOS-spezifisches Upgrade in dieser Liste
**Status:** BEREITS VORHANDEN (notify.sh + notify.ps1 existierten schon vor dieser Recherche)

---

### 🍺 Finding 15: Homebrew MCP — Intelligente Paketpflege als Self-Healing-Schicht
**Direktive:** 2: Selbstbeobachtung + 3: Resilient Bugfixing
**Quelle(n):** [Homebrew MCP GitHub](https://github.com/jeannier/homebrew-mcp) | [Homebrew Security Audit](https://brew.sh/2024/07/30/homebrew-security-audit/)
**Entdeckt in:** Welle 2 (macOS-Fokus)
**Was ist das? (fuer Nicht-Programmierer):**
Homebrew ist der Paketmanager von macOS — er installiert und verwaltet Entwicklerwerkzeuge wie Node.js, Python, Git, Go und hunderte andere. Wenn diese Pakete veralten, entstehen Sicherheitsluecken und Kompatibilitaetsprobleme. Der Homebrew MCP Server gibt Claude Code direkten Zugriff auf Homebrew-Befehle: Welche Pakete sind veraltet? Welche haben Sicherheitswarnungen? Das Besondere: Claude kann selbst "brew outdated" aufrufen und erkennen wenn die Entwicklungsumgebung Pflege braucht — ohne dass der Benutzer daran denken muss.

**Was bringt es uns konkret?:**
Selbstbeobachtung auf Umgebungsebene: Claude sieht nicht nur den eigenen Code-Zustand sondern auch den Zustand der macOS-Entwicklungsumgebung. Veraltete Pakete, die Fehler verursachen koennen, werden PROAKTIV gemeldet — nicht erst wenn der Build fehlt.

**Aufwand:** 30 Min

**Umsetzung in Claude Code:**
- **Typ:** MCP-Server (Installation) + Rule-Erweiterung
- **Datei(en):** `~/proggs/.mcp.json` (macOS-Eintrag), neue Regel `~/.claude/rules/homebrew-health.md`
- **Schritte:**
  1. Homebrew MCP installieren: `npm install -g homebrew-mcp` oder via npx
  2. MCP-Eintrag in `~/proggs/.mcp.json` hinzufuegen (macOS-only)
  3. Regel erstellen: Bei Session-Start (oder einmal pro Woche) `brew outdated` pruefen
  4. Wenn kritische Pakete veraltet: Automatisch ins Whiteboard loggen unter "Systemzustand"
  5. Optional: `brew audit` in invariant-check.sh integrieren fuer automatische Sicherheitspruefung

**Kreativitaets-Bonus:** Verbindet sich elegant mit dem bestehenden invariant-check.sh Hook. Homebrew-Zustand wird zur sechsten Invariante die bei jedem Session-Start geprueft wird. "Entwicklungsumgebung ist aktuell" wird zur messbaren Invariante — genau das was das Cursor Invariant Sentinel Pattern anstrebt.
**Abhaengigkeiten:** Homebrew muss installiert sein (ist es bereits laut Systemzustand)
**Risiko:** Homebrew-Upgrades koennen bestehende Funktionen brechen. Nur als Information-Tool nutzen, NIEMALS automatisch upgraden ohne Benutzer-Zustimmung.
**Empfehlung:** VERWORFEN — Claude kann `brew outdated` bereits via Bash, MCP-Server (27 Stars, Python-Dependency) ist Overkill
**Status:** VERWORFEN (2026-03-31) — Alltagstauglichkeits-Pruefung: brew outdated ist im heartbeat.sh integriert, kein separater MCP-Server noetig

---

### Umgesetzte Vorschlaege

*Noch keine umgesetzten Vorschlaege.*

### Verworfene Vorschlaege

*Noch keine verworfenen Vorschlaege.*

---

## Direktive 3: Resilient Bugfixing

> Findings die sicherstellen dass KEIN Fehler jemals zweimal auftritt — Zero-Recurrence-Methoden,
> Self-Healing, automatische Root-Cause-Analyse, praediktive Fehlererkennung.

### Offene Vorschlaege

---

### 🛡️ Finding 8: Poka-Yoke fuer Code — Fehler-unmoeglich-machen statt Fehler-erkennen
**Direktive:** 3: Resilient Bugfixing
**Quelle(n):** [Poka-Yoke in Software Engineering](https://www.softwaretestinghelp.com/poka-yoke/) | [FlowFuse: Poka-Yoke Guide 2026](https://flowfuse.com/blog/2025/09/poka-yoke-mistake-proofing/)
**Entdeckt in:** Welle 2
**Was ist das? (fuer Nicht-Programmierer):**
In japanischen Fabriken gibt es einen Trick: Man formt die Teile so dass man sie gar nicht falsch zusammenbauen KANN — die USB-Stecker-Form ist ein bekanntes Beispiel. Das heisst "Poka-Yoke" (Fehler-unmoeglich-machen). Uebertraegen auf Code-Entwicklung: Statt Fehler zu erkennen und zu fixen — Situationen schaffen wo der Fehler gar nicht auftreten kann. Beispiel: Wenn wir wissen dass "git push ohne fetch" immer Probleme macht, koennen wir einen Hook bauen der jeden Push-Befehl automatisch mit fetch+rebase vorbereitet.

**Was bringt es uns konkret?:**
Die meisten unserer Regeln sind REAKTIV (Fehler erklaeren und fixen). Poka-Yoke macht sie PRAEVENTIV — der Fehler passiert gar nicht erst. Kein Fehler = kein Fixen = keine Zeit-Verschwendung. Das ist die Reinform der "Resilient Bugfixing" Direktive.

**Aufwand:** 1 Stunde

**Umsetzung in Claude Code:**
- **Typ:** Hook-Set (mehrere Hooks)
- **Datei(en):** `~/.claude/hooks/poka-yoke-git.ps1`, `~/.claude/hooks/poka-yoke-git.sh`
- **Schritte:**
  1. Audit: Die 5 haeufigsten Fehler aus MEMORY.md und bug-cases.jsonl identifizieren
  2. Fuer jeden Fehler fragen: "Wie kann ich diesen Fehler UNMOEGLICH machen?" (statt nur erklaerbar)
  3. Beispiel-Implementierung: PreToolUse-Hook fuer Bash der "git push" abfaengt und automatisch "git fetch origin && git rebase origin/main" davor einfuegt
  4. Beispiel: PreToolUse-Hook fuer Write der prueft ob JSON-Dateien nach dem Schreiben valide sind (automatische Validierung)
  5. Jeden Poka-Yoke-Hook dokumentieren: Welcher Fehler wird verhindert, wie oft haette er aufgetreten?

**Kreativitaets-Bonus:** Die Luftfahrt-Industrie hat Poka-Yoke perfektioniert: Piloten koennen das Fahrwerk nicht einfahren solange das Flugzeug am Boden ist (physikalische Sperre). Die gleiche Philosophie: Systeme so bauen dass bestimmte Aktionen in bestimmten Kontexten schlicht nicht moeglich sind.
**Abhaengigkeiten:** Keine
**Risiko:** Hooks koennen legitime Befehle blockieren wenn zu aggressiv. Jeden Hook einzeln testen.
**Empfehlung:** Bald
**Status:** UMGESETZT (2026-03-31)

---

### 🔄 Finding 9: AI-gesteuertes Chaos Engineering fuer Claude-Code-Umgebung
**Direktive:** 3: Resilient Bugfixing
**Quelle(n):** [AI-Driven Chaos Engineering Review](https://journals.stecab.com/jcsp/article/view/846) | [Autonomous SRE mit Chaos Engineering](https://dev.to/vaib/autonomous-sre-revolutionizing-reliability-with-ai-automation-and-chaos-engineering-5c7g)
**Entdeckt in:** Welle 2
**Was ist das? (fuer Nicht-Programmierer):**
Netflix hatte eine geniale Idee: Statt zu warten bis der Server ausraesst, bringen sie ihn ABSICHTLICH zum Absturz — um zu sehen ob das System sich selbst erholt. Das heisst "Chaos Engineering". Fuer Claude Code: Man koennte absichtlich fehlerhafte Zust aende erzeugen — z.B. "Was passiert wenn settings.json leer ist?", "Was passiert wenn git fetch fehlschlaegt?", "Was passiert wenn ein Hook crasht?" — und pruefen ob die Self-Healing-Mechanismen korrekt reagieren.

**Was bringt es uns konkret?:**
28% bessere Fehlererkennungsrate, 35% kuerzere Erholungszeit laut aktueller Forschung (2025). Fuer uns: Wir entdecken Schwachstellen in unserer Umgebung BEVOR sie im echten Einsatz auftreten. Das ist proaktives statt reaktives Bugfixing.

**Aufwand:** 1 Tag

**Umsetzung in Claude Code:**
- **Typ:** Agent (neuer `chaos-tester` Agent, gelegentlich aufgerufen)
- **Datei(en):** `~/.claude/agents/chaos-tester.md`
- **Schritte:**
  1. Liste der kritischen Komponenten erstellen: settings.json, MEMORY.md, Hooks, MCP-Server-Config
  2. Fuer jede Komponente: Definiere was "Fehler" bedeutet (leere Datei, ungueltige JSON, fehlende Datei)
  3. `chaos-tester` Agent simuliert jeden Fehler in einer sicheren Umgebung (Backup → Fehler erzeugen → Verhalten beobachten → Backup wiederherstellen)
  4. Ergebnis: Liste der Komponenten die sich NICHT erholen und die daher Poka-Yoke-Schutz benoetigen
  5. Ergebnisse in MEMORY.md unter "Chaos-Test Ergebnisse" loggen

**Kreativitaets-Bonus:** Chaos Engineering kommt urspruenglich von Netflix (Chaos Monkey). Die gleichnamige Philosophie: "Alles was kaputtgehen kann, wird kaputtgehen." Besser es selbst kaputt machen als darauf warten.
**Abhaengigkeiten:** Finding 8 (Poka-Yoke) — sinnvoll als Vorbereitung
**Risiko:** Kann die echte Umgebung beschaedigen wenn nicht sorgfaeltig implementiert. Immer Backup-Mechanismus zuerst testen.
**Empfehlung:** Spaeter (nach Finding 8)
**Status:** UMGESETZT (2026-03-31)

---

### 🦠 Finding 10: Immunologisches Gedaechtnis — Fehler-Antigen-System
**Direktive:** 3: Resilient Bugfixing
**Quelle(n):** [Artificial Immune System Wikipedia](https://en.wikipedia.org/wiki/Artificial_immune_system) | [Multi-layer defense inspired by immune systems (GitHub)](https://gist.github.com/andreschauer/e0f958c2a279062559ae8306f946b43d)
**Entdeckt in:** Welle 3
**Was ist das? (fuer Nicht-Programmierer):**
Das menschliche Immunsystem hat ein faszinierendes Feature: Wenn es ein Virus (Antigen) zum ersten Mal sieht, braucht es Zeit um zu reagieren. Aber es speichert das Muster — beim zweiten Kontakt reagiert es sofort. Dieses "Immunologische Gedaechtnis" ist der Grund warum Impfungen funktionieren. Uebertraegen auf Code-Fehler: Jeder neue Fehlertyp ist ein "Antigen". Das System lernt das Muster. Beim naechsten Auftreten des gleichen Musters — sofortige Erkennung und automatischer Fix.

**Was bringt es uns konkret?:**
Zero-Recurrence Rate fuer bekannte Fehler. Fehler die einmal aufgetreten sind werden nie wieder "unerwartet" sein — das System erkennt und blockt sie automatisch. Das ist die starkste Form von "Resilient Bugfixing".

**Aufwand:** 1 Tag

**Umsetzung in Claude Code:**
- **Typ:** Hook + Agent-Upgrade
- **Datei(en):** `.claude/agent-memory/shared/error-antigens.jsonl` (neue Datei), `~/.claude/hooks/immune-check.ps1`
- **Schritte:**
  1. Neue Datei `error-antigens.jsonl`: Jeder Eintrag speichert Fehler-Muster (Fehlermeldung + Kontext-Tags + Gegenmassnahme)
  2. PreToolUse-Hook der Bash-Befehle gegen bekannte Fehler-Muster prueft ("wird dieser Befehl einen bekannten Fehler ausloesen?")
  3. Bei Match: Automatisch die gespeicherte Gegenmassnahme anwenden BEVOR der Fehler auftreten kann
  4. Bei neuem Fehler: Automatisch ein neues "Antigen" speichern
  5. /self-improve liest error-antigens.jsonl und konsolidiert aehnliche Muster

**Kreativitaets-Bonus:** Das Biologische Vorbild ist elegant: Negative Selection (Immunologie) erkennt "nicht-selbst" Muster. In Software-Sprache: "Diese Fehlermeldung ist kein normales Verhalten — hier greift das Immunsystem ein." Vollkommen andere Disziplin, perfekte Metapher fuer Zero-Recurrence.
**Abhaengigkeiten:** Finding 6 (CBR Bug-Datenbank) — das Antigen-System baut auf der Bug-Datenbank auf
**Risiko:** Die Antigen-Datei muss sorgfaeltig verwaltet werden. Falsch-positive koennen legitime Befehle blockieren.
**Empfehlung:** Bald
**Status:** UMGESETZT (2026-03-31)

---

---

### 🔑 Finding 16: ServeMyAPI — Keychain-gestuetzte API-Key-Verwaltung fuer macOS
**Direktive:** 3: Resilient Bugfixing
**Quelle(n):** [ServeMyAPI GitHub](https://github.com/Jktfe/serveMyAPI) | [ServeMyAPI auf Playbooks](https://playbooks.com/mcp/jktfe-servemyapi-macos-keychain) | [Dev.to: API Keys falsch gespeichert](https://dev.to/the_seventeen/why-every-mcp-setup-guide-is-teaching-you-to-store-api-keys-wrong-4ghf)
**Entdeckt in:** Welle 2 (macOS-Fokus)
**Was ist das? (fuer Nicht-Programmierer):**
Die meisten Anleitungen sagen: "Trage deinen API-Key in die .env-Datei ein". Das ist so als wuerde man den Haustuerschluessel unter die Fussmatte legen. Auf macOS gibt es einen sicheren Tresor: die Keychain. ServeMyAPI ist ein MCP-Server der alle API-Keys (Anthropic, OpenAI, GitHub, Google usw.) sicher im macOS-Tresor speichert. Claude Code kann dann ueber natuerliche Sprache nach Keys fragen: "Gib mir den Anthropic API-Key." Der Key kommt sicher aus dem Tresor, erscheint nie in einer Textdatei und kann nicht versehentlich in Git committed werden.

**Was bringt es uns konkret?:**
Eliminiert eine ganze Klasse von Sicherheitsfehlern: versehentlich committete API-Keys. Das ist Zero-Recurrence fuer eine der haaufigsten und gefaehrlichsten Entwickler-Fehler. Ausserdem: Ein einziger Key in der Keychain ist sofort in ALLEN Projekten verfuegbar — kein manuelles Kopieren von .env-Dateien mehr.

**Aufwand:** 30 Min

**Umsetzung in Claude Code:**
- **Typ:** MCP-Server (Installation, macOS-only)
- **Datei(en):** `~/proggs/.mcp.json` (macOS-Eintrag hinzufuegen)
- **Schritte:**
  1. ServeMyAPI installieren: `npx -y serveMyAPI` oder via Smithery
  2. MCP-Eintrag in `~/proggs/.mcp.json` fuer macOS hinzufuegen
  3. Bestehende API-Keys migrieren: Jeden Key aus .env-Dateien in die Keychain verschieben via ServeMyAPI-Befehle
  4. Regel erstellen: ".env-Dateien mit API-Keys sind verboten — stets ServeMyAPI verwenden"
  5. Bestehende .env-Dateien auf API-Keys pruefen und bereinigen (git history ebenfalls pruefen)

**Kreativitaets-Bonus:** Claude Code hat laut aktueller Dokumentation bereits eine eingebaute Keychain-Integration fuer Plugin-Credentials. ServeMyAPI erweitert das auf ALLE API-Keys projektuebergreifend. Kombiniert: Vollstaendige Key-Isolation vom Dateisystem. Kein Key landet je in einer Textdatei. Das ist Poka-Yoke (Finding 8) fuer Credentials.
**Abhaengigkeiten:** macOS-only (Windows hat Windows Credential Manager — anderes Tool noetig)
**Risiko:** Keychain-Zugriff braucht Benutzerberechtigung bei erster Verwendung. Bei Keychain-Korruption koennen Keys verloren gehen (Backup-Strategie noetig).
**Empfehlung:** VERWORFEN — Keychain-Popups bei jedem Prozess-Neustart stoeren den bypassPermissions-Workflow
**Status:** VERWORFEN (2026-03-31) — Alltagstauglichkeits-Pruefung: macOS Keychain fragt bei jedem neuen node-Prozess um Erlaubnis

---

### ⚡ Finding 17: launchd Heartbeat — Autonome Gesundheitsueberwachung der Entwicklungsumgebung
**Direktive:** 3: Resilient Bugfixing
**Quelle(n):** [PAIArchitecture GitHub](https://github.com/MaxHarar/PAIArchitecture) | [launchd Tutorial](https://www.launchd.info/) | [Apple: Creating Launch Daemons and Agents](https://developer.apple.com/library/archive/documentation/MacOSX/Conceptual/BPSystemStartup/Chapters/CreatingLaunchdJobs.html)
**Entdeckt in:** Welle 3 (macOS-Fokus — kreative Synthese)
**Was ist das? (fuer Nicht-Programmierer):**
macOS hat ein eingebautes Werkzeug namens launchd — es ist wie ein automatischer Waecker der Aufgaben zu bestimmten Zeiten oder Intervallen startet. Eine Community-Implementierung (PAIArchitecture) nutzt das fuer einen "Heartbeat": Alle N Minuten laueft ein Skript das prueft ob alle wichtigen Dienste der Claude-Code-Umgebung noch laufen (MCP-Server, Hooks, Index). Wenn etwas fehlt, repariert es sich selbst. Das Besondere an macOS: launchd ist robuster als cron und startet auch nach einem System-Crash automatisch neu.

**Was bringt es uns konkret?:**
Unsere Entwicklungsumgebung prueft sich aktuell nur bei Session-Start (invariant-check.sh). Mit einem launchd-Heartbeat wird sie DAUERHAFT und AUTOMATISCH ueberwacht — auch zwischen Sessions. Wenn der semantische Suchindex abstuerzt oder ein MCP-Server stoppt, wird das sofort entdeckt und gemeldet (via macOS Notification), nicht erst beim naechsten Start.

**Aufwand:** 1 Tag

**Umsetzung in Claude Code:**
- **Typ:** macOS launchd Agent + Shell-Skript
- **Datei(en):** `~/Library/LaunchAgents/com.claudecode.heartbeat.plist`, `~/.claude/hooks/heartbeat.sh`
- **Schritte:**
  1. Heartbeat-Skript erstellen (`heartbeat.sh`): Prueft ob claude-mem, semantic search und Whiteboard erreichbar sind
  2. launchd plist erstellen: Alle 15 Minuten heartbeat.sh ausfuehren
  3. plist laden: `launchctl load ~/Library/LaunchAgents/com.claudecode.heartbeat.plist`
  4. Wenn Fehler: macOS Notification (via osascript) + Whiteboard-Eintrag
  5. Self-Healing: Wenn MCP-Server gestoppt, automatisch neu starten (nur sichere Dienste)

**Kreativitaets-Bonus:** Das Herzschlag-Muster kommt aus der Medizin: Ein regelmaessiger Puls ist der sicherste Beweis dass ein System noch lebt. Statt darauf zu warten dass ein Dienst abstuerzt und Fehler wirft, prueft der Heartbeat aktiv ob alles am Leben ist. Kombiniert mit Finding 14 (macOS Notifications): Wenn der Heartbeat einen "toten" Dienst findet, hoert man es sofort.
**Abhaengigkeiten:** Finding 14 (macOS Notifications) empfohlen aber nicht Pflicht; launchd ist auf jedem Mac vorinstalliert
**Risiko:** launchd plist-Fehler koennen zu CPU-Loops fuehren wenn das Skript immer wieder crasht. Plist muss mit StartInterval (nicht StartCalendarInterval) konfiguriert werden und Skript muss immer exit 0 liefern.
**Empfehlung:** Bald (nach Finding 14)
**Status:** UMGESETZT (2026-03-31) — heartbeat.sh + com.frank.claude-heartbeat.plist + invariant-check.sh Integration

---

### Umgesetzte Vorschlaege

*Noch keine umgesetzten Vorschlaege.*

### Verworfene Vorschlaege

*Noch keine verworfenen Vorschlaege.*

---

## Top 5 Empfehlungen (nach Impact sortiert)

> Die 5 wichtigsten Vorschlaege die als NAECHSTES umgesetzt werden sollten.
> Wird nach jedem Forschungslauf aktualisiert.

| Rang | Finding | Status | Anmerkung |
|------|---------|--------|-----------|
| ~~1~~ | ~~Finding 14: macOS Notification Hooks~~ | BEREITS VORHANDEN | notify.sh + notify.ps1 existierten schon |
| ~~2~~ | ~~Finding 16: ServeMyAPI Keychain MCP~~ | VERWORFEN | Keychain-Popups bei jedem Prozess-Neustart |
| ~~3~~ | ~~Finding 15: Homebrew MCP~~ | VERWORFEN | brew outdated in heartbeat.sh integriert |
| ~~4~~ | ~~Finding 11: SwiftLens MCP~~ | BEREITS VORHANDEN | swift-lsp Plugin war schon aktiviert |
| **5** | **Finding 17: launchd Heartbeat** | **UMGESETZT** | heartbeat.sh + launchd plist + invariant-check Integration |

*Hinweis: Findings 1-10 aus Lauf 1 sind bereits umgesetzt. Von Findings 11-17 (macOS-Fokus) waren 2 schon vorhanden, 3 wurden nach Alltagstauglichkeits-Pruefung verworfen, 1 wurde umgesetzt, 1 wartet auf macOS 26.*

**Einziger offener Vorschlag:** Finding 13 (Apple Foundation Models) — wartet auf macOS 26 (Tahoe).

---

## Bereits Recherchiert (Duplikat-Filter)

> Themen die bereits untersucht wurden. Der Agent ueberspringt diese bei zukuenftigen Laeufen.

- [2026-03-31] Monte Carlo Tree Search (MCTS) fuer LLM Agenten — 2 Findings (SWE-Search + ToolTree)
- [2026-03-31] Metacognitive AI / Confidence Tracking — 2 Findings (Confidence Ampel + Metacognitive Monitoring)
- [2026-03-31] Automatische Root Cause Analysis / Zero Recurrence — 1 Finding (indirekt ueber CBR + Immune System)
- [2026-03-31] Graph of Thoughts Reasoning — Kein eigenstaendiges Finding (bekannt, Basis fuer MCTS-Arbeit)
- [2026-03-31] Swarm Intelligence LLM Multi-Agent — 1 Finding (Pheromon-Tabelle)
- [2026-03-31] Reflexion / Self-Refine Architektur — In bestehenden Systemen bereits implementiert (Skill: reflexion:reflect vorhanden)
- [2026-03-31] Cursor vs Windsurf vs Claude Code Konkurrenzanalyse 2026 — 1 Finding (Cascade Hooks Pattern)
- [2026-03-31] Chaos Engineering fuer Self-Healing — 1 Finding (chaos-tester Agent)
- [2026-03-31] Poka-Yoke in Software Engineering — 1 Finding (Fehler-unmoeglich-machen Hooks)
- [2026-03-31] Deliberate Practice / Spaced Repetition fuer AI — Kein Finding (zu weit von Claude Code Kontext entfernt)
- [2026-03-31] Case-Based Reasoning (CBR) fuer LLM Agenten — 1 Finding (Bug-Datenbank + Immunsystem)
- [2026-03-31] Immunologisches Gedaechtnis / Artificial Immune System — 1 Finding (Antigen-System)
- [2026-03-31] macOS CoreML on-device AI / Claude Code — 1 Finding (Apple Foundation Models Framework, Finding 13)
- [2026-03-31] macOS Accessibility API fuer Entwickler — Kein eigenstaendiges Finding (zu spezifisch fuer UI-Automatisierung, nicht fuer Claude Code Kern)
- [2026-03-31] Hammerspoon macOS Automation — 1 Finding (Skill auf MCPMarket bereits vorhanden, Finding 12)
- [2026-03-31] SwiftLens / SourceKit-LSP MCP — 1 Finding (compiler-grade Swift-Analyse, Finding 11)
- [2026-03-31] macOS Notification Center fuer Agent-Status — 1 Finding (afplay+osascript Hooks, Finding 14)
- [2026-03-31] macOS Keychain fuer API-Keys / ServeMyAPI — 1 Finding (Zero-Config Secrets, Finding 16)
- [2026-03-31] Homebrew MCP Server — 1 Finding (Paketpflege als Selbstbeobachtung, Finding 15)
- [2026-03-31] macOS launchd fuer Self-Healing — 1 Finding (Heartbeat-Pattern, Finding 17)
- [2026-03-31] macOS DTrace / Instruments Performance-Profiling — Kein Finding (zu aufwendig fuer Claude Code Context, DTrace erfordert SIP-Deaktivierung)
- [2026-03-31] macOS Quick Look Plugins fuer Code-Preview — Kein Finding (passiv, kein direkter Claude Code Mehrwert)
- [2026-03-31] Xcode 26.3 Claude Agent SDK Integration — Kein separates Finding (ist Xcode-intern, Claude Code CLI profitiert indirekt; kein separates Setup noetig)
- [2026-03-31] macOS oslog / Unified Logging als Agent-Observability — Kein Finding (zu viel macOS-Systemwissen noetig, praktischer Nutzen gering gegenueber bestehenden Whiteboard-Logs)

---

## Meta: Forschungs-Verbesserungen

> Der Agent notiert hier, welche Suchstrategien gut funktioniert haben und welche nicht.

- [2026-03-31] **Funktioniert gut:** Interdisziplinaere Suche (Biologie → Immunsystem, Ingenieurwesen → Poka-Yoke). Die kreativsten und wertvollsten Findings kamen aus Verbindungen zwischen Disziplinen, nicht aus direkten AI-Suchen.
- [2026-03-31] **Funktioniert gut:** arXiv-Paper direkt fetchen fuer technische Details. Die "Entdeckt in Welle 2 + 3" Findings waren deutlich konkreter als reine Suchtreffer.
- [2026-03-31] **Funktioniert gut:** Konkurrenz-Analyse (Windsurf Cascade Hooks). Zeigt klar welche Features Claude Code fehlen und liefert konkrete Implementierungsvorlage.
- [2026-03-31] **Nicht effektiv:** "Deliberate Practice / Spaced Repetition" fuer AI-Agenten — Suche lieferte nur Lern-App-Ergebnisse, kein Claude-Code-relevantes Material.
- [2026-03-31] **Naechstes Mal probieren:** Gezielt nach Anthropic-internen Blog-Posts suchen ("Anthropic blog agent architecture 2026") und nach GitHub-Repos von konkreten Implementierungen suchen (z.B. `site:github.com MCTS claude agent`).
- [2026-03-31] **Naechstes Mal probieren:** Suche nach SWE-bench Leaderboard aktualisieren — welche neuen Techniken haben 2026 Top-Scores erreicht? Das sind bewaehrte Methoden.
- [2026-03-31] **Qualitaets-Beobachtung:** 10 Findings in 3 Wellen. Die ersten 6 Suchen (Welle 1) lieferten breite Orientierung. Welle 2 lieferte 60% der besten Findings. Welle 3 lieferte die kreativsten (Immunsystem, CBR-Details). Optimale Verteilung bestaetigt den 3-Wellen-Ansatz.
- [2026-03-31] **Funktioniert gut (Lauf 2):** Plattform-fokussierter Suchrahmen (macOS-Fokus) lieferte konkrete, sofort installierbare MCP-Server und Hooks statt abstrakte Konzepte. Empfehlung: Naechster Lauf mit Kotlin/Android-Fokus oder TypeScript-Fokus.
- [2026-03-31] **Funktioniert gut (Lauf 2):** GitHub-Suche nach Community-Projekten (PAIArchitecture, SwiftLens, ServeMyAPI) brachte kreative Muster die offizielle Doku nicht hat. GitHub ist Pflichtquelle fuer Implementation-Suchen.
- [2026-03-31] **Nicht effektiv (Lauf 2):** WebFetch auf GitHub wird durch den intercept-github-fetch.sh Hook blockiert — stattdessen WebSearch mit Repo-Name nutzen oder `gh repo view` via Bash. Zeitverschwendung durch Blockierung vermeiden.
- [2026-03-31] **Naechstes Mal probieren:** macOS-Shortcuts App und Automator als Agent-Trigger untersuchen (wurde in diesem Lauf nicht recherchiert — moeglicherweise Low-Code Alternative zu Hammerspoon).
