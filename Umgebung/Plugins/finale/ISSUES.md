# finale Plugin — Issue-Logbuch

Live-Dokumentation aller Bugs, Designprobleme und Verbesserungswuensche, die waehrend
des produktiven Einsatzes des `finale`-Plugins entdeckt werden. Jeder Eintrag soll
nachher ohne Zusatzkontext fixbar sein.

**Konvention:**
- ID: `FIN-NNN` (fortlaufend)
- Severity: `kritisch` (Plugin startet/funktioniert nicht), `hoch` (Phase faellt aus),
  `mittel` (Workaround moeglich), `niedrig` (Politur).
- Status: `offen`, `workaround-aktiv`, `gefixt`, `wontfix`.

Quelldateien werden mit absolutem Pfad referenziert.

---

## FIN-001 — Argument-Mismatch zwischen orchestrator.md und verify-skills.sh

- **Severity:** kritisch
- **Status:** workaround-aktiv (Skript wird ohne Argument aufgerufen)
- **Entdeckt:** 2026-05-18, erste produktive Nutzung des Plugins gegen BestJournalAndroid
- **Symptom:**
  ```
  [verify-skills] Plugin-Root: /c/Users/barwa/proggs/BestJournalAndroid
  [verify-skills] Skills-Verzeichnis: /c/Users/barwa/proggs/BestJournalAndroid/skills
  [verify-skills] FEHLER: Skills-Verzeichnis fehlt
  Exit code 1
  ```
- **Ursache:**
  - `agents/orchestrator.md` Zeile 102-104 schreibt vor:
    ```bash
    bash "${CLAUDE_PLUGIN_ROOT}/scripts/verify-skills.sh" "<app-root>"
    ```
    Es uebergibt also den App-Root als erstes Argument.
  - `scripts/verify-skills.sh` Zeile 25 erwartet jedoch das erste Argument als
    **Plugin-Root**:
    ```bash
    PLUGIN_ROOT="${1:-$(cd "$SCRIPT_DIR/.." && pwd)}"
    SKILLS_DIR="$PLUGIN_ROOT/skills"
    ```
  - Folge: Das Skript sucht `BestJournalAndroid/skills/` und findet nichts.
- **Workaround in dieser Session:** Skript ohne Argument aufrufen. Default
  `SCRIPT_DIR/..` loest korrekt auf das Plugin-Root auf.
- **Korrekturvorschlag (sauber):**
  1. Skript so umbauen, dass ein optionales Argument als App-Root interpretiert wird
     (fuer kuenftige Pre-Checks der App, z. B. AndroidManifest.xml-Pruefung).
  2. Plugin-Root IMMER aus `SCRIPT_DIR/..` ableiten (nicht ueberschreibbar via $1).
  3. Orchestrator-Anweisung kann den App-Root weiterhin uebergeben, das Skript nutzt
     ihn dann nur fuer den App-spezifischen Sanity-Check, nicht fuer den Skill-Lookup.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/scripts/verify-skills.sh`
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Zeile 102-104)
  - Spiegel im aktiven Cache: `~/.claude/plugins/cache/local/finale/0.1.0/scripts/verify-skills.sh`

---

## FIN-002 — ${CLAUDE_PLUGIN_ROOT} wird in der Bash-Umgebung nicht gesetzt

- **Severity:** hoch
- **Status:** offen (zwingt zu absoluten Pfaden ueberall)
- **Entdeckt:** 2026-05-18, gleicher Lauf wie FIN-001
- **Symptom:** `echo $CLAUDE_PLUGIN_ROOT` in der Subagent-Bash-Umgebung gibt leere
  Zeichenkette zurueck. Damit wuerden alle Aufrufe wie
  `bash "${CLAUDE_PLUGIN_ROOT}/scripts/verify-skills.sh"` zu
  `bash /scripts/verify-skills.sh` werden — Datei nicht gefunden.
- **Ursache:** Claude Code injiziert `${CLAUDE_PLUGIN_ROOT}` zwar in den Markdown-
  Body von Plugin-Commands (`shield.md` etc.), aber NICHT als Umgebungsvariable
  in nachfolgende Bash-Aufrufe von Subagents/Tools.
- **Workaround in dieser Session:** Den absoluten Pfad
  `/c/Users/barwa/.claude/plugins/cache/local/finale/0.1.0/` ueberall hardkodieren.
- **Korrekturvorschlag (sauber):**
  1. Im Orchestrator-Prompt klarstellen, dass die Bash-Aufrufe entweder
     (a) absolute Pfade benutzen oder
     (b) zuerst eine Resolution-Funktion laufen lassen, die das Plugin-Root
         aus `~/.claude/plugins/installed_plugins.json` ermittelt:
         ```bash
         FINALE_PLUGIN_ROOT="$(python3 -c "import json,os; p=json.load(open(os.path.expanduser('~/.claude/plugins/installed_plugins.json'))); print(p['plugins']['local/finale']['cacheDir'])")"
         ```
  2. Alternativ: Ein kleines `bin/finale-env.sh`, das beim Source-en
     `FINALE_PLUGIN_ROOT` setzt — und der Orchestrator macht als erstes ein
     `source "$(dirname "$0")/../bin/finale-env.sh"` (geht aber im Markdown nicht).
  3. Saubere Loesung: README/Orchestrator-Prompt soll dokumentieren, dass die
     Helper-Resolution-Funktion als Phase-0-Pflichtstart laeuft.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (alle bash-Bloecke)
  - `~/proggs/Umgebung/Plugins/finale/Plugin/commands/shield.md` (Zeile 50, 55)

---

## FIN-003 — feature-scan.sh ist nicht Windows/Git-Bash-kompatibel

- **Severity:** hoch
- **Status:** offen (Roentgen-Subagent hat manuell um das Skript herumgearbeitet)
- **Entdeckt:** 2026-05-18, Phase 1A — gemeldet vom Roentgen-Subagent (Opus, effort: max)
- **Symptom:** Drei kombinierte Windows-Probleme im feature-scan.sh des app-roentgen-Skills:
  1. `ripgrep` (rg) wird vorausgesetzt, ist aber unter Windows nicht garantiert
     im PATH — `command -v rg` liefert nichts.
  2. `sed`-Aufrufe verwenden POSIX-Klassen / GNU-Extensions, die unter Git Bash
     fehlschlagen oder anders interpretiert werden.
  3. Arithmetische Syntax in Zeile 238 (vermutlich `$(())` mit fehlerhaftem
     Ausdruck) wirft "syntax error" unter Bash 4.x von Git Bash.
- **Folge:** Der Subagent musste auf direkte Read/Grep-Tool-Calls ausweichen.
  strings.xml wurde nur teilweise extrahiert (629 von ~1094 Eintraegen), weshalb
  Findings wie Privacy-Policy-URL/Impressum als "NICHT_VERIFIZIERT" markiert
  bleiben muessten.
- **Workaround in dieser Session:** Skill-Subagent hat die Skript-Logik in
  Tool-Calls reimplementiert (Read, Grep, Glob), aber nicht 100% Coverage erreicht.
- **Korrekturvorschlag:**
  1. `feature-scan.sh` cross-platform machen: ripgrep-Fallback auf grep
     (`if ! command -v rg; then alias rg=grep; fi`), GNU-spezifische sed-Flags
     entfernen oder ueber Python-Wrapper ersetzen.
  2. Zusaetzliche `feature-scan.ps1` fuer reines Windows liefern.
  3. Oder: Skript komplett durch Python ersetzen (plattformneutral by default).
- **Betroffene Dateien:**
  - `~/.claude/skills/app-roentgen/scripts/feature-scan.sh` (Hauptfehlerquelle)
  - `~/.claude/skills/app-roentgen/SKILL.md` (Aufruf des Skripts)

---

## FIN-004 — Single-Subagent-Strategie skaliert nicht bei Apps >800 Strings

- **Severity:** kritisch (Frank-Anweisung: in JEDER zukuenftigen Plugin-Version Pflicht-Regel)
- **Status:** offen (in dieser Session funktionsfaehig durch Lucky Save vor dem Thrash)
- **Entdeckt:** 2026-05-18, Phase 1A — Subagent A landete nach 22 Minuten und
  51 Tool-Calls im Autocompact-Thrashing ("the context refilled to the limit
  within 3 turns of the previous compact, 3 times in a row")
- **Symptom:** Der Roentgen-Subagent baut bei einer App dieser Groesse so viel
  Context auf (1094 Strings + 142 Kotlin-Files + Skill-Anweisungen + Manifest +
  Build-Skripte + Marketing-Claims-Matrix), dass die Auto-Compaction nicht mehr
  greift. Der Subagent kann sein finales Summary nicht mehr sauber liefern,
  obwohl die produktiven Outputs (JSON + Markdown) bereits geschrieben wurden.
- **Folge:** Hauptchat bekommt KEINE Status-Zusammenfassung zurueck. Orchestrator
  muss selbst die Datei lesen und interpretieren. Bei einer App die nochmal
  doppelt so gross ist, koennte schon der Skill-Lauf vorzeitig abbrechen.
- **Workaround in dieser Session:** Da die Outputs vor dem Thrash geschrieben
  wurden, konnte der Orchestrator sie nachtraeglich auswerten — purer Glueck.
- **Korrekturvorschlag (architektonisch):**
  1. **Phase 1A in mehrere parallele Subagents zerlegen** statt einen einzigen
     monolithischen Aufruf:
     - 1A-Architektur (Module, Screens, Navigation)
     - 1A-Paywall (BillingClient, Premium-Gates, Subscription-CTAs)
     - 1A-Permissions (Manifest + Begruendungen)
     - 1A-Strings-Inventory (strings.xml-Extraktion in Batches von 200 Strings)
     - 1A-Compose-Literals (hardcoded UI-Texte in .kt-Files)
     - 1A-Synthesis (kombiniert die 5 Teilreports zum roentgen-report.json)
     Vorteil: Jeder Subagent hat ueberschaubaren Scope, parallele Ausfuehrung
     ist moeglich, kein einzelner haut den Context platt.
  2. **Skill `app-roentgen` modularisieren:** Eine `capability`-Schicht
     einfuehren ("scan-architecture", "scan-paywall", "scan-strings") damit
     auch isolierte Aufrufe moeglich sind.
  3. **Orchestrator: Auto-Erkennung App-Groesse vor Phase 1.** Wenn
     stringResourceCount >800 ODER ktFileCount >100 → automatisch geteilten
     Subagent-Pfad waehlen statt monolithischen.
- **Frank-Direktive 2026-05-18 (zwingend ins Plugin):**
  > "Jeder Subagent darf ein maximales Limit haben von 100.000 Token mehr nicht,
  >  danach muss ein neuer weiterer Subagent gespawnt werden. Das Problem hatte ich
  >  schon ueberall und immer wieder, das Agents einfach zu gross sind und Bereiche
  >  nicht mehr verarbeiten koennen."
  - **100.000 Token Pflicht-Cap pro Subagent.** Niemals mehr. Wenn ein
    Worker das Limit zu erreichen droht: SOFORT Output schreiben, sauber
    beenden, naechsten Worker spawnen mit dem geschriebenen Output als Input.
  - **Map-Reduce statt Monolith.** Mehrere parallele Worker mit fokussiertem
    Scope, dann ein Synthesizer der nur Teilergebnisse aggregiert (selbst auch
    unter 100k bleibt, weil er nur kompakte JSON-Dateien liest).
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Phase 1A+1B-Definition komplett umschreiben)
  - `~/.claude/skills/app-roentgen/SKILL.md` (Skill-Granularitaet, Capability-Schicht einfuehren)
  - Neue Datei: `~/proggs/Umgebung/Plugins/finale/Plugin/agents/synthesizer.md` (Aggregations-Agent)

---

## FIN-005 — Map-Reduce-Architektur als globales Plugin-Pattern fehlt

- **Severity:** hoch (Plugin-Architektur-Lücke)
- **Status:** offen
- **Entdeckt:** 2026-05-18, ausgeloest durch Frank-Direktive nach FIN-004
- **Symptom:** Der Orchestrator-Prompt sieht aktuell pro Phase EINEN Subagent
  vor (Phase 1A: Subagent A, Phase 1B: Subagent B, Phase 3a: ein Strings-Worker,
  Phase 3b: ein Uebersetzer-Worker pro Sprache). Bei grossen Apps ueberschreitet
  jeder einzelne dieser Worker das 100k-Token-Limit (siehe FIN-004).
- **Ursache:** Das Plugin-Design folgt einem 1:1-Mapping Phase↔Subagent statt
  einer Map-Reduce-Architektur. Es fehlt:
  1. Ein **Token-Budget-Tracker** im Subagent-Prompt (Worker erkennt selbst,
     wann er bei 80k angekommen ist, und beendet sauber).
  2. Ein **Synthesizer-Agent**, der nur JSON-Teilergebnisse aggregiert.
  3. Ein **Scope-Splitter** im Orchestrator, der bei grossen Apps automatisch
     in Worker-Bereiche zerlegt (z. B. strings.xml in Batches von 300 Strings,
     .kt-Files in Module-Buckets, Findings nach Kategorie).
- **Korrekturvorschlag (Plugin-Architektur-Update):**
  - Standard-Map-Reduce-Pattern in `orchestrator.md` festschreiben:
    ```
    Phase X:
      Worker 1 (scope A, max 100k) ─┐
      Worker 2 (scope B, max 100k) ─┤
      Worker 3 (scope C, max 100k) ─┼─→ Synthesizer (max 100k) → finales JSON
      Worker N (scope ..., max 100k)─┘
    ```
  - Pflicht-Frontmatter fuer Worker-Subagents: `tokenBudget: 100000`,
    `outputFile: <pfad>`, `summaryToOrchestrator: max-20-zeilen`.
  - Synthesizer-Agent als separates Frontmatter-Profil
    (`type: synthesizer`, `inputs: [array of jsons]`, `output: <pfad>`).
  - Im Orchestrator pro Phase ein **Scope-Decision-Block** vor dem Spawn:
    "Worker oder Map-Reduce? Entscheidung anhand stringCount/ktFileCount/findingCount."
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (alle Phasen)
  - Neu: `~/proggs/Umgebung/Plugins/finale/Plugin/agents/synthesizer.md`
  - Neu: `~/proggs/Umgebung/Plugins/finale/Plugin/agents/worker-template.md`

---

<!-- Weitere Issues werden hier waehrend des Laufs angehaengt. -->
