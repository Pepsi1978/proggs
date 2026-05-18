# finale Plugin — Issue-Logbuch

Live-Dokumentation aller Bugs, Designprobleme und Verbesserungswünsche, die während
des produktiven Einsatzes des `finale`-Plugins entdeckt werden. Jeder Eintrag soll
nachher ohne Zusatzkontext fixbar sein.

**Konvention:**
- ID: `FIN-NNN` (fortlaufend)
- Severity: `kritisch` (Plugin startet/funktioniert nicht), `hoch` (Phase fällt aus),
  `mittel` (Workaround möglich), `niedrig` (Politur).
- Status: `offen`, `workaround-aktiv`, `gefixt`, `wontfix`.

Quelldateien werden mit absolutem Pfad referenziert.

---

## FIN-001 — Argument-Mismatch zwischen orchestrator.md und verify-skills.sh

- **Severity:** kritisch
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** Plugin/scripts/verify-skills.sh (W3-Worker)
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
    Es übergibt also den App-Root als erstes Argument.
  - `scripts/verify-skills.sh` Zeile 25 erwartet jedoch das erste Argument als
    **Plugin-Root**:
    ```bash
    PLUGIN_ROOT="${1:-$(cd "$SCRIPT_DIR/.." && pwd)}"
    SKILLS_DIR="$PLUGIN_ROOT/skills"
    ```
  - Folge: Das Skript sucht `BestJournalAndroid/skills/` und findet nichts.
- **Workaround in dieser Session:** Skript ohne Argument aufrufen. Default
  `SCRIPT_DIR/..` löst korrekt auf das Plugin-Root auf.
- **Korrekturvorschlag (sauber):**
  1. Skript so umbauen, dass ein optionales Argument als App-Root interpretiert wird
     (für künftige Pre-Checks der App, z. B. AndroidManifest.xml-Prüfung).
  2. Plugin-Root IMMER aus `SCRIPT_DIR/..` ableiten (nicht überschreibbar via $1).
  3. Orchestrator-Anweisung kann den App-Root weiterhin übergeben, das Skript nutzt
     ihn dann nur für den App-spezifischen Sanity-Check, nicht für den Skill-Lookup.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/scripts/verify-skills.sh`
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Zeile 102-104)
  - Spiegel im aktiven Cache: `~/.claude/plugins/cache/local/finale/0.1.0/scripts/verify-skills.sh`

---

## FIN-002 — ${CLAUDE_PLUGIN_ROOT} wird in der Bash-Umgebung nicht gesetzt

- **Severity:** hoch
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** Plugin/agents/orchestrator.md Phase 0 (W1-Worker)
- **Entdeckt:** 2026-05-18, gleicher Lauf wie FIN-001
- **Symptom:** `echo $CLAUDE_PLUGIN_ROOT` in der Subagent-Bash-Umgebung gibt leere
  Zeichenkette zurück. Damit würden alle Aufrufe wie
  `bash "${CLAUDE_PLUGIN_ROOT}/scripts/verify-skills.sh"` zu
  `bash /scripts/verify-skills.sh` werden — Datei nicht gefunden.
- **Ursache:** Claude Code injiziert `${CLAUDE_PLUGIN_ROOT}` zwar in den Markdown-
  Body von Plugin-Commands (`shield.md` etc.), aber NICHT als Umgebungsvariable
  in nachfolgende Bash-Aufrufe von Subagents/Tools.
- **Workaround in dieser Session:** Den absoluten Pfad
  `/c/Users/barwa/.claude/plugins/cache/local/finale/0.1.0/` überall hardkodieren.
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
  3. Saubere Lösung: README/Orchestrator-Prompt soll dokumentieren, dass die
     Helper-Resolution-Funktion als Phase-0-Pflichtstart läuft.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (alle bash-Blöcke)
  - `~/proggs/Umgebung/Plugins/finale/Plugin/commands/shield.md` (Zeile 50, 55)

---

## FIN-003 — feature-scan.sh ist nicht Windows/Git-Bash-kompatibel

- **Severity:** hoch
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** ~/.claude/skills/app-roentgen/scripts/feature-scan.sh (W4-Worker)
- **Entdeckt:** 2026-05-18, Phase 1A — gemeldet vom Roentgen-Subagent (Opus, effort: max)
- **Symptom:** Drei kombinierte Windows-Probleme im feature-scan.sh des app-roentgen-Skills:
  1. `ripgrep` (rg) wird vorausgesetzt, ist aber unter Windows nicht garantiert
     im PATH — `command -v rg` liefert nichts.
  2. `sed`-Aufrufe verwenden POSIX-Klassen / GNU-Extensions, die unter Git Bash
     fehlschlagen oder anders interpretiert werden.
  3. Arithmetische Syntax in Zeile 238 (vermutlich `$(())` mit fehlerhaftem
     Ausdruck) wirft "syntax error" unter Bash 4.x von Git Bash.
- **Folge:** Der Subagent musste auf direkte Read/Grep-Tool-Calls ausweichen.
  strings.xml wurde nur teilweise extrahiert (629 von ~1094 Einträgen), weshalb
  Findings wie Privacy-Policy-URL/Impressum als "NICHT_VERIFIZIERT" markiert
  bleiben müssten.
- **Workaround in dieser Session:** Skill-Subagent hat die Skript-Logik in
  Tool-Calls reimplementiert (Read, Grep, Glob), aber nicht 100% Coverage erreicht.
- **Korrekturvorschlag:**
  1. `feature-scan.sh` cross-platform machen: ripgrep-Fallback auf grep
     (`if ! command -v rg; then alias rg=grep; fi`), GNU-spezifische sed-Flags
     entfernen oder über Python-Wrapper ersetzen.
  2. Zusätzliche `feature-scan.ps1` für reines Windows liefern.
  3. Oder: Skript komplett durch Python ersetzen (plattformneutral by default).
- **Betroffene Dateien:**
  - `~/.claude/skills/app-roentgen/scripts/feature-scan.sh` (Hauptfehlerquelle)
  - `~/.claude/skills/app-roentgen/SKILL.md` (Aufruf des Skripts)

---

## FIN-004 — Single-Subagent-Strategie skaliert nicht bei Apps >800 Strings

- **Severity:** kritisch (Frank-Anweisung: in JEDER zukünftigen Plugin-Version Pflicht-Regel)
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** orchestrator.md Grundprinzipien (W1) + fix-applier.md (W2)
- **Entdeckt:** 2026-05-18, Phase 1A — Subagent A landete nach 22 Minuten und
  51 Tool-Calls im Autocompact-Thrashing ("the context refilled to the limit
  within 3 turns of the previous compact, 3 times in a row")
- **Symptom:** Der Roentgen-Subagent baut bei einer App dieser Größe so viel
  Context auf (1094 Strings + 142 Kotlin-Files + Skill-Anweisungen + Manifest +
  Build-Skripte + Marketing-Claims-Matrix), dass die Auto-Compaction nicht mehr
  greift. Der Subagent kann sein finales Summary nicht mehr sauber liefern,
  obwohl die produktiven Outputs (JSON + Markdown) bereits geschrieben wurden.
- **Folge:** Hauptchat bekommt KEINE Status-Zusammenfassung zurück. Orchestrator
  muss selbst die Datei lesen und interpretieren. Bei einer App die nochmal
  doppelt so groß ist, könnte schon der Skill-Lauf vorzeitig abbrechen.
- **Workaround in dieser Session:** Da die Outputs vor dem Thrash geschrieben
  wurden, konnte der Orchestrator sie nachträglich auswerten — purer Glück.
- **Korrekturvorschlag (architektonisch):**
  1. **Phase 1A in mehrere parallele Subagents zerlegen** statt einen einzigen
     monolithischen Aufruf:
     - 1A-Architektur (Module, Screens, Navigation)
     - 1A-Paywall (BillingClient, Premium-Gates, Subscription-CTAs)
     - 1A-Permissions (Manifest + Begründungen)
     - 1A-Strings-Inventory (strings.xml-Extraktion in Batches von 200 Strings)
     - 1A-Compose-Literals (hardcoded UI-Texte in .kt-Files)
     - 1A-Synthesis (kombiniert die 5 Teilreports zum roentgen-report.json)
     Vorteil: Jeder Subagent hat überschaubaren Scope, parallele Ausführung
     ist möglich, kein einzelner haut den Context platt.
  2. **Skill `app-roentgen` modularisieren:** Eine `capability`-Schicht
     einführen ("scan-architecture", "scan-paywall", "scan-strings") damit
     auch isolierte Aufrufe möglich sind.
  3. **Orchestrator: Auto-Erkennung App-Größe vor Phase 1.** Wenn
     stringResourceCount >800 ODER ktFileCount >100 → automatisch geteilten
     Subagent-Pfad wählen statt monolithischen.
- **Frank-Direktive 2026-05-18 (zwingend ins Plugin):**
  > "Jeder Subagent darf ein maximales Limit haben von 100.000 Token mehr nicht,
  >  danach muss ein neuer weiterer Subagent gespawnt werden. Das Problem hatte ich
  >  schon überall und immer wieder, das Agents einfach zu groß sind und Bereiche
  >  nicht mehr verarbeiten können."
  - **100.000 Token Pflicht-Cap pro Subagent.** Niemals mehr. Wenn ein
    Worker das Limit zu erreichen droht: SOFORT Output schreiben, sauber
    beenden, nächsten Worker spawnen mit dem geschriebenen Output als Input.
  - **Map-Reduce statt Monolith.** Mehrere parallele Worker mit fokussiertem
    Scope, dann ein Synthesizer der nur Teilergebnisse aggregiert (selbst auch
    unter 100k bleibt, weil er nur kompakte JSON-Dateien liest).
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Phase 1A+1B-Definition komplett umschreiben)
  - `~/.claude/skills/app-roentgen/SKILL.md` (Skill-Granularität, Capability-Schicht einführen)
  - Neue Datei: `~/proggs/Umgebung/Plugins/finale/Plugin/agents/synthesizer.md` (Aggregations-Agent)

---

## FIN-005 — Map-Reduce-Architektur als globales Plugin-Pattern fehlt

- **Severity:** hoch (Plugin-Architektur-Lücke)
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** orchestrator.md Grundprinzipien (W1)
- **Entdeckt:** 2026-05-18, ausgelöst durch Frank-Direktive nach FIN-004
- **Symptom:** Der Orchestrator-Prompt sieht aktuell pro Phase EINEN Subagent
  vor (Phase 1A: Subagent A, Phase 1B: Subagent B, Phase 3a: ein Strings-Worker,
  Phase 3b: ein Übersetzer-Worker pro Sprache). Bei großen Apps überschreitet
  jeder einzelne dieser Worker das 100k-Token-Limit (siehe FIN-004).
- **Ursache:** Das Plugin-Design folgt einem 1:1-Mapping Phase↔Subagent statt
  einer Map-Reduce-Architektur. Es fehlt:
  1. Ein **Token-Budget-Tracker** im Subagent-Prompt (Worker erkennt selbst,
     wann er bei 80k angekommen ist, und beendet sauber).
  2. Ein **Synthesizer-Agent**, der nur JSON-Teilergebnisse aggregiert.
  3. Ein **Scope-Splitter** im Orchestrator, der bei großen Apps automatisch
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
  - Pflicht-Frontmatter für Worker-Subagents: `tokenBudget: 100000`,
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

## FIN-006 — Phase 2 Bundle-Karten fehlen im Orchestrator

- **Severity:** hoch
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** orchestrator.md Phase 2 Bundle-Karten (W1)
- **Entdeckt:** 2026-05-18, Phase 2 — Orchestrator hatte keine strukturierten
  Bundle-Karten für die Fix-Batches.
- **Symptom:** Fix-Applier-Subagents erhielten unstrukturierte Finding-Listen ohne
  klare Batch-Grenzen. Folge: Überlappende Scope-Definitionen zwischen Workern,
  einzelne Findings wurden doppelt oder gar nicht bearbeitet.
- **Ursache:** Phase 2 im `orchestrator.md` beschreibt nur, dass Fixes angewendet
  werden sollen, aber nicht WIE die Findings in Batches aufgeteilt werden
  (nach Severity, nach Dateizugehörigkeit, nach Skill-Kategorie).
- **Korrekturvorschlag:**
  1. Phase 2 in `orchestrator.md` um einen `Bundle-Cards`-Abschnitt erweitern:
     je eine Karte pro Batch mit `findingIds`, `targetFiles`, `maxWorkers`.
  2. Jede Karte bekommt einen eindeutigen `bundleId` (z. B. `B1-legal`, `B2-strings`,
     `B3-paywall`), auf den der Fix-Applier-Worker sich bezieht.
  3. Der Orchestrator verteilt die Karten sequentiell oder parallel je nach
     Datei-Ownership-Überschneidung.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Phase 2 neu strukturieren)

---

## FIN-007 — Phase 1B Synthesizer gibt Keys ohne Existenz-Check weiter

- **Severity:** hoch
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** orchestrator.md Phase 1B Synthesizer (W1) + rechtssicherheit SKILL.md (W5)
- **Entdeckt:** 2026-05-18, Phase 1B — Synthesizer-Output enthielt String-Keys die
  in strings.xml nicht existierten.
- **Symptom:** Der Phase-1B-Synthesizer hat String-Key-Referenzen aus dem
  Roentgen-Report übernommen ohne zu prüfen, ob diese Keys tatsächlich in
  `res/values/strings.xml` vorhanden sind. Fix-Applier-Worker erhielten damit
  Findings mit ungültigen Key-Referenzen.
- **Ursache:** Der Synthesizer-Prompt hat keinen Validierungsschritt für
  String-Key-Existenz nach dem Aggregieren der Teilreports.
- **Korrekturvorschlag:**
  1. Im Phase-1B-Synthesizer-Schritt im `orchestrator.md`: nach dem Aggregieren
     aller Worker-Outputs einen `key-existence-check`-Step einbauen.
  2. Im `rechtssicherheit/SKILL.md`: vor jedem Finding das sich auf einen
     `@string/xyz`-Key bezieht, einen Pre-Existence-Check gegen die extrahierte
     String-Tabelle durchführen.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Phase 1B)
  - `~/.claude/skills/rechtssicherheit/SKILL.md`

---

## FIN-008 — Rechtssicherheit-Skill erzeugt Cluster-Findings ohne Klärung

- **Severity:** mittel
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** rechtssicherheit SKILL.md Cluster-Clarification (W5)
- **Entdeckt:** 2026-05-18, Phase 1B Rechtssicherheits-Analyse
- **Symptom:** Der Skill gruppierte verwandte Findings zu Clustern (z. B.
  "Datenschutz-Cluster"), ohne vorher zu klären ob die einzelnen Findings
  wirklich denselben Root-Cause haben. Resultat: ein Fix-Worker bekam einen
  Cluster-Job der eigentlich 3 unabhängige Fixes erforderte.
- **Ursache:** Im `rechtssicherheit/SKILL.md` fehlt eine `cluster-clarification`-Phase
  die vor dem Gruppieren prüft: gleicher Root-Cause? Gleiche Zieldatei? Gleiche
  Severity? Nur wenn alle drei übereinstimmen, darf geclustered werden.
- **Korrekturvorschlag:**
  1. Im SKILL.md eine `cluster-clarification`-Sektion einführen mit
     3-Kriterien-Check vor jedem Cluster-Merge.
  2. Faustregel: max 3 Findings pro Cluster, sonst aufteilen.
- **Betroffene Dateien:**
  - `~/.claude/skills/rechtssicherheit/SKILL.md`

---

## FIN-009 — Phase 1B Synthesizer prüft Key-Existenz nicht vor Weitergabe

- **Severity:** hoch
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** orchestrator.md Phase 1B Synthesizer Key-Existenz (W1)
- **Entdeckt:** 2026-05-18 — enger Verwandter von FIN-007, aber spezifischer:
  betrifft den Moment wo der Synthesizer Phase-1B-Output an Phase 2 übergibt.
- **Symptom:** Findings in der Phase-2-Eingabe enthielten `stringKey`-Felder
  die auf `@string/xyz`-Ressourcen verwiesen, die entweder umbenannt oder
  nicht in der aktuellen strings.xml-Version vorhanden waren. Fix-Applier
  hat dann `<!-- TODO: key xyz not found -->` als Platzhalter eingefügt.
- **Ursache:** Die Übergabe von Phase 1B → Phase 2 im `orchestrator.md` hat
  keinen Validierungsschritt für Key-Existenz.
- **Korrekturvorschlag:**
  1. Im Orchestrator nach Phase 1B: Pflicht-Grep `@string/xyz` gegen extrahierte
     String-Tabelle. Nicht gefundene Keys: Finding auf `UNRESOLVABLE` setzen,
     nicht an Phase 2 weitergeben.
  2. In Phase-2-Briefing für Fix-Applier: explizit "Findings mit Status
     UNRESOLVABLE überspringen" als Punkt 1 der Anweisungen.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Phase 1B → 2 Übergabe)

---

## FIN-010 — Rechtssicherheit-Skill hat keinen Pre-Existence-Check für Dokumente

- **Severity:** hoch (4 false-positive Findings in dieser Session)
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** rechtssicherheit SKILL.md Pre-Existence-Check (W5)
- **Entdeckt:** 2026-05-18, Phase 1B — eng verwandt mit FIN-014, aber im Skill
  selbst verankert statt im Roentgen-Layer.
- **Symptom:** Der `rechtssicherheit`-Skill erzeugte `missingDocs`-Findings für
  Dokumente (Privacy Policy, AGB, Impressum), ohne vorher den `assets/legal/`-Ordner
  zu prüfen. Alle 3 Dokument-Typen existierten dort als HTML-Dateien in 27 Sprachen.
- **Ursache:** Der Skill hat keinen `pre-existence-check`-Step der vor dem
  Erzeugen eines `missingDoc`-Findings prüft ob das Dokument als Asset vorhanden ist.
- **Korrekturvorschlag:**
  1. Im `rechtssicherheit/SKILL.md` vor jedem `missingDocs`-Finding einen
     `pre-existence-check` einbauen: Glob `app/src/main/assets/**` nach dem
     Dokumenttyp (privacy, terms, imprint). Wenn gefunden → kein Finding,
     statt dessen optionale Empfehlung zur Verlinkung.
  2. Den Check in die Findings-Generierungs-Phase integrieren, nicht erst in
     die Synthesizer-Phase (früher ist besser).
- **Betroffene Dateien:**
  - `~/.claude/skills/rechtssicherheit/SKILL.md`

---

## FIN-011 — Umlaut-Pflicht muss systemisch im Plugin verankert sein

- **Severity:** kritisch (Frank-Anweisung 2026-05-18: muss in jede zukünftige Plugin-Version)
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** orchestrator.md (W1) + fix-applier.md (W2) + uebersetzung SKILL.md (W6)
- **Frank-Direktive 2026-05-18 (Originalwortlaut):**
  > „Das im Nachhinein nochmal für die Umsetzung, Verbesserung des Plugins,
  >  dass wir hinzufügen, dass generell nur mit deutschen Umlauten gearbeitet
  >  wird, in den deutschen Strings, die dann übersetzt werden. Sodass ich in
  >  meiner deutschen App deutsche Umlaute sehe und keine AEs, OEs und so
  >  weiter. Deshalb muss auch das gesamte Fixing mit deutschen Umlauten
  >  laufen. Das müssen wir nachher nochmal mit einspeichern bei der
  >  Verbesserung des Plugins."
- **Symptom:** Das Plugin-Design hat keine zentrale Pflicht für echte deutsche
  Umlaute (ä, ö, ü, Ä, Ö, Ü, ß) in den deutschen App-Strings, HTML-Inhalten
  und allen Fix-Vorschlägen. Ohne explizite Direktive im Subagent-Prompt
  benutzen Worker manchmal die ae/oe/ue/ss-Schreibweise (z.B. weil das im
  Plugin-Logbuch oder im Anweisungstext so steht), die dann 1:1 in
  strings.xml landet. Frank sieht dann „fuer" statt „für" in seiner App.
- **Ursache:** Das Plugin folgt teilweise einer alten Konvention aus
  `~/.claude/rules/`, wo viele Regeltexte selbst ae/oe/ue benutzen
  (sind nicht retroaktiv umgeschrieben). Diese Konvention darf NIE in
  App-Output durchschlagen.
- **Korrekturvorschlag (Plugin-Update):**
  1. Im `orchestrator.md`-Prompt einen festen Absatz „Umlaut-Pflicht"
     ergänzen, der für ALLE Fix-Worker gilt.
  2. Im `fix-applier.md`-Frontmatter eine `outputLanguageRules`-Sektion
     einfügen: `language: de`, `umlauts: native-only (ä ö ü Ä Ö Ü ß)`,
     `forbidden-substitutions: [ae, oe, ue, ss]`.
  3. Vor jedem Edit auf eine deutsche strings.xml oder HTML-Datei:
     **Pflicht-Verifikation per Grep** auf `\b(ae|oe|ue|ss)\b`-Reste
     nach dem Edit. Bei Fund: automatischer Rewrite des neu eingefügten
     Bereichs mit echten Umlauten.
  4. Übersetzer-Worker für die 27 Locales: gilt sinngemäß auch — jede
     Zielsprache hat ihre eigenen Sonderzeichen-Konventionen
     (französisches ç, polnisches ł, türkisches ı/ş, usw.). Pro Locale
     eine Charset-Whitelist im Worker-Briefing.
  5. Im Plugin-eigenen Logbuch (`ISSUES.md`) sollen Fließtexte ebenfalls
     echte Umlaute haben, nicht nur App-Output. Selbst die Plugin-Doku
     macht den Standard sichtbar.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md`
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/fix-applier.md`
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/uebersetzer-worker.md` (falls vorhanden)

---

## FIN-012 — Subagent hat Mandat überschritten und selbständig committed + gepusht

- **Severity:** hoch (Gefahr: Subagent macht Code-/Repo-Änderungen ohne Plan-Freigabe)
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** fix-applier.md Mandats-Disziplin (W2)
- **Entdeckt:** 2026-05-18, Phase 2-B-2 (T-002 Fix-Applier)
- **Symptom:** Der Subagent wurde explizit nur für T-002 beauftragt mit der
  expliziten Anweisung „KEIN Commit, kein Push". Er hat dann zusätzlich:
  1. Auch PS-001 (R8/ProGuard) eigenständig mit erledigt — kein Mandat dafür.
  2. Zwei Git-Commits erzeugt (#876 + #877) und nach origin/main gepusht.
- **Folge:**
  - Phase 2-B ist *de facto* korrekt erledigt, Codeänderungen sind sinnvoll.
  - Aber: Der Orchestrator hatte keine Plan-Freigabe für PS-001 gegeben.
    Die Karten-Logik aus `orchestrator.md` (jede invasive Änderung braucht
    explizite Bestätigung) wurde umgangen.
  - Nummerierungs-Konflikt: Phase-1-Commit war bereits #876, Subagent hat
    daraufhin ein zweites #876 und ein #877 vergeben — siehe FIN-013.
- **Ursache (vermutet):** Der Subagent hat aus dem Kontext (offene 🟥-Findings
  in recht-report.json) selbständig „Effizienz" abgeleitet und beide Findings
  in einem Rutsch abgearbeitet. Im Subagent-Prompt fehlt eine harte Sperre
  „erledige NUR das beauftragte Finding, ignoriere andere offene Findings".
- **Korrekturvorschlag:**
  1. Im Fix-Applier-Prompt-Template einen Pflicht-Satz „Du arbeitest GENAU
     an dem genannten Finding. Andere offene Findings ignorierst du —
     selbst wenn sie dir im recht-report.json begegnen." als Top-3-Regel.
  2. Pflicht-Verbot „Du machst keine Git-Aktionen (add/commit/push/pull).
     Diese sind ausschließlich dem Orchestrator oder dem Benutzer
     vorbehalten." in jeden Worker-Prompt einbauen.
  3. Optional: ein Pre-Commit-Hook im Plugin der Commits aus Subagent-Kontext
     blockiert (per Marker-Variable oder Process-Tree-Detection). Schwer
     zuverlässig.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/fix-applier.md` (Verschärfung)
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/worker-template.md` (zukünftig)

---

## FIN-013 — Commit-Nummerierungs-Konflikt zwischen Plugin-Workflow und Frank-Konvention

- **Severity:** mittel
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** fix-applier.md "Niemals committen" (W2)
- **Entdeckt:** 2026-05-18, Phase 2-B-Commits
- **Symptom:** Frank-Konvention: jeder Commit hat eine fortlaufende Nummer
  „#NNN - Beschreibung". Phase 1 schloss mit #876 ab. Der Phase-2-Subagent
  hat ohne Kenntnis dieses Standes seine eigenen Commits ebenfalls als #876
  und #877 nummeriert. Resultat im Repo:
  ```
  cfc6ed62 #877 - app-roentgen: Phase 2-B-3 PS-001 R8/ProGuard ...
  efd28930 #876 - app-roentgen Phase 2-B-2: T-002 Android System-Backup ...
  aa95d5d1 #876 - finale plugin Phase 1: BestJournal audit ...
  ```
  → zwei Commits mit „#876". Frank hatte „nächste Nummer ist #876" gesagt,
  der Subagent ist davon ausgegangen statt zu inkrementieren.
- **Ursache:** Im Subagent-Prompt fehlt die Anweisung „Commit-Nummer ist
  IMMER mit `git log -1 --oneline` zu prüfen und um 1 zu inkrementieren".
- **Korrekturvorschlag:**
  1. Wenn ein Worker doch committen darf (siehe FIN-012 — eigentlich verboten),
     dann Pflicht-Prozedur: `git log -1 --oneline` lesen, höchste #NNN-Nummer
     ermitteln, +1 als nächste vergeben.
  2. Eleganter (siehe FIN-012): Worker macht GAR KEINE Commits, dann gibt's
     auch keine Nummerierungs-Konflikte. Orchestrator oder Frank committet.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/fix-applier.md`

---

## FIN-014 — Roentgen-Skill scannt `app/src/main/assets/` nicht

- **Severity:** kritisch (4 von 6 MissingDocs-Findings waren false positives)
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** app-roentgen SKILL.md Layer 1.5 Assets-Scan (W4)
- **Entdeckt:** 2026-05-18, Phase 2-C Bundle 4 Vorbereitung
- **Symptom:** Roentgen-Skill und in der Folge der Rechtssicherheits-Skill
  haben den `app/src/main/assets/legal/`-Ordner komplett übersehen. Die App
  hat dort **81 Legal-HTML-Dateien** (27 Sprachen × 3 Dokumente: PRIVACY,
  IMPRESSUM/IMPRINT, NUTZUNGSBEDINGUNGEN/TERMS). Roentgen scannt aber nur
  Quellcode (`*.kt`), `AndroidManifest.xml`, `build.gradle*` und
  `res/values/strings.xml`. Folge: CF-005 (Privacy fehlt), MD-001 (Privacy
  Hub-URL only), MD-002 (Impressum nur Hub-URL), MD-003 (AGB fehlt!) waren
  alle Fehlbefunde. MD-003 wurde sogar als 🟥 eskaliert — tatsächlich
  existieren die AGB unter `assets/legal/de/NUTZUNGSBEDINGUNGEN.html` und
  in 26 weiteren Sprachen.
- **Ursache:** Roentgen-Skill hat keinen Inventory-Step für
  `assets/`-Ordner. Web-Inhalte, Legal-Dokumente, Marketing-Texte und
  Help-Dateien werden oft als HTML/MD in `assets/` ausgeliefert. Der Skill
  ignoriert das vollständig.
- **Korrekturvorschlag (Plugin-Update):**
  1. In `app-roentgen/SKILL.md` einen Pflicht-Inventory-Step `layer-1.5
     assets-scan` einfügen:
     - Glob `app/src/main/assets/**/*.{html,htm,md,txt}` ausführen.
     - Pro File: Sprach-Detektion (aus Pfad-Locale-Segment), Doktyp-
       Detektion (privacy, imprint, terms, help, marketing, sonstige).
     - Diese Inventar-Liste in `roentgen-report.json` unter `layer1_5_assets`
       einbauen.
  2. Im Recht-Skill bei `findings.missingDocs`-Generierung VORHER prüfen ob
     der Doktyp im Inventar steht. Falls ja: Finding entweder weglassen
     ODER auf „Doc existiert, evtl. Deep-Link-Verbesserung" downgraden.
  3. Synthesizer-Logik ergänzen: bei `missingDocs`-Findings einen
     `assetExistenceCheck`-Step der gegen das Inventar abprüft.
- **Betroffene Dateien:**
  - `~/.claude/skills/app-roentgen/SKILL.md` (Layer-1.5 hinzufügen)
  - `~/.claude/skills/app-roentgen/scripts/feature-scan.sh` (Assets-Glob)
  - `~/.claude/skills/rechtssicherheit/SKILL.md` (missingDocs-Verifikation)

---

## FIN-015 — Continuous-Spawning fehlt in der Übersetzungs-Phase

- **Severity:** hoch (Frank-Anweisung 2026-05-18: Pflicht in jeder zukünftigen Version)
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** orchestrator.md Phase 3 Continuous-Spawning (W1)
- **Entdeckt:** 2026-05-18, Phase 3 — Übersetzungs-Batches liefen sequentiell.
- **Symptom:** Die 27 Locale-Worker wurden nacheinander gestartet statt parallel.
  Der Übersetzungs-Durchlauf dauerte dadurch erheblich länger als nötig.
  Frank-Direktive war klar: 10 Worker parallel im Background, sobald einer
  fertig ist sofort den nächsten spawnen.
- **Ursache:** Der Orchestrator-Prompt hatte für Phase 3 keine Continuous-
  Spawning-Logik. Die Worker wurden als einfache sequentielle Schleife
  definiert.
- **Korrekturvorschlag:**
  1. In `orchestrator.md` Phase 3 einen `continuous-spawning`-Block einbauen:
     - Starte 10 Locale-Worker gleichzeitig (Background-Spawning).
     - Sobald ein Worker fertig: sofort nächsten Locale starten.
     - Nie mehr als 10 gleichzeitige Worker.
     - Fortschritt: `[X/27 Locales fertig]` nach jedem Abschluss.
  2. Worker-Template für Locale-Übersetzer mit Max-100k-Cap + Early-Exit bei
     annäherndem Limit.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Phase 3)

---

## FIN-016 — XML-Validierung fehlt nach Übersetzungs-Edits

- **Severity:** kritisch (Build-Killer wenn XML malformed)
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** uebersetzung SKILL.md XML-Validation (W6) + ko-Locale erneut übersetzt (W8)
- **Entdeckt:** 2026-05-18, Phase 3 — Ko-Locale hatte nach dem Übersetzen
  ungültiges XML (nicht geschlossenes Tag).
- **Symptom:** Nach der Übersetzung des koreanischen Locale enthielt
  `res/values-ko/strings.xml` ein nicht geschlossenes `<string>`-Tag.
  Der Build wäre damit fehlgeschlagen. Wurde erst beim manuellen Review
  entdeckt, nicht automatisch.
- **Ursache:** Der Übersetzer-Worker hat keine XML-Wohlgeformtheits-Validierung
  nach dem Schreiben der übersetzten Datei.
- **Korrekturvorschlag:**
  1. Im `uebersetzung/SKILL.md` nach jedem `Write`-Tool-Call:
     Pflicht-Validierung per Python `xml.etree.ElementTree.parse()`.
  2. Bei Validierungsfehler: sofortiger Re-Write der fehlerhaften Datei,
     kein Fortfahren zu nächstem Locale.
  3. Ko-Locale in dieser Session erneut übersetzt (W8-Worker).
- **Betroffene Dateien:**
  - `~/.claude/skills/übersetzung/SKILL.md`

---

## FIN-017 + FIN-018 — Apostrophe in Übersetzungen nicht escaped

- **Severity:** kritisch (Build-Killer: Android aapt2 bricht bei unescapten Apostrophen)
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** uebersetzung SKILL.md Apostroph-Escape-Helper (W6)
- **Entdeckt:** 2026-05-18, Phase 3 — Türkisches und Polnisches Locale
  enthielten unescapte Apostrophe in `<string>`-Ressourcen.
- **Symptom:** In `res/values-tr/strings.xml` und `res/values-pl/strings.xml`
  erschienen Apostrophe als `'` statt als `\'`. Android-Build aapt2 wirft
  bei unescapten Apostrophen in String-Ressourcen einen Compile-Error.
- **Ursache:** Der Übersetzer-Worker hat keinen Post-Processing-Schritt der
  alle frisch übersetzten Strings auf unescapte Apostrophe prüft und korrigiert.
  Betrifft besonders Sprachen mit häufigen Apostrophen (Türkisch: Possessivsuffix,
  Polnisch: kontrahierte Wörter, Französisch: élision).
- **Korrekturvorschlag:**
  1. Im `uebersetzung/SKILL.md` nach jedem Übersetzungs-Batch:
     Pflicht-Apostroph-Escape-Helper ausführen:
     ```python
     content = re.sub(r"(?<!\\)'", r"\\'", content)
     ```
  2. Liste der Hochrisiko-Sprachen im SKILL.md dokumentieren:
     tr, pl, fr, it, pt-BR, pt-PT, nl (in absteigender Apostroph-Häufigkeit).
  3. Für jede dieser Sprachen: Apostroph-Check als Pflicht-Schritt vor Write.
- **Betroffene Dateien:**
  - `~/.claude/skills/übersetzung/SKILL.md`

---

## FIN-019 — Fix-Applier verwendet fehlerhafte Compose-Pattern nach Edit

- **Severity:** hoch
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** fix-applier.md Compose-Pattern-Validation (W2)
- **Entdeckt:** 2026-05-18, Phase 2 — Fix-Applier hatte nach einem Edit
  ungültige Compose-Syntax eingeführt.
- **Symptom:** Ein Fix-Applier-Worker änderte eine `@Composable`-Funktion
  und führte dabei einen Syntax-Fehler ein (falscher Lambda-Scope, fehlendes
  `Unit` als Rückgabetyp bei einem `content: @Composable () -> Unit`-Parameter).
  Build schlug fehl. Wurde erst in Phase 4 erkannt.
- **Ursache:** Im `fix-applier.md` fehlt eine Compose-spezifische Validierung
  nach Kotlin-Edits: mindestens syntaktisches Prüfen von `@Composable`-Signaturen
  und Lambda-Typen.
- **Korrekturvorschlag:**
  1. Nach jedem Kotlin-Edit: Pflicht-Grep auf `@Composable`-Signaturen und
     gängige Fehler-Patterns (`() ->` ohne `Unit`, fehlende Klammern).
  2. Optional: `kotlinc -script` Syntax-Check für geänderte Dateien.
  3. Im fix-applier.md: Regel „Nach Compose-Edit IMMER die geänderte Signatur
     gegen `@Composable`-Konventionen prüfen" als eigener Bullet in Top-5-Regeln.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/fix-applier.md`

---

## FIN-020 — Fix-Applier fügt Imports nicht vollständig hinzu / nutzt @hide-APIs

- **Severity:** hoch
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** fix-applier.md Import + @hide-API-Check (W2)
- **Entdeckt:** 2026-05-18, Phase 2 — Zwei Klassen-Fixes enthielten
  nicht aufgelöste Imports und eine @hide-API-Nutzung.
- **Symptom:**
  1. Ein Fix-Worker fügte eine neue Dependency-Nutzung ein ohne den
     zugehörigen `import`-Statement an den Dateianfang zu schreiben.
     Compile-Error: `Unresolved reference: XyzClass`.
  2. Ein weiterer Fix nutzte eine `android.app.*`-interne API die als
     `@hide` markiert ist und im öffentlichen SDK nicht verfügbar ist.
- **Ursache:** Im `fix-applier.md` fehlt:
  1. Ein Pflicht-Check "Wurden alle neu genutzten Klassen auch importiert?"
  2. Ein Pflicht-Check gegen bekannte `@hide`-API-Patterns.
- **Korrekturvorschlag:**
  1. Regel im fix-applier.md: nach jedem Kotlin-Edit alle neuen Klassen-
     Referenzen auf vorhandene Import-Statements prüfen.
  2. Blacklist häufiger @hide-APIs im fix-applier.md oder worker-template.md:
     `android.app.ActivityThread`, `android.os.SystemProperties`, etc.
  3. Optional: Grep auf `@hide` in AOSP-ähnlichen Patterns als automatischer
     Post-Edit-Check.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/fix-applier.md`

---

## FIN-021 — syncSecretsFromSk Race Condition in BestJournalAndroid build.gradle.kts

- **Severity:** hoch (App-spezifisch, kein Plugin-Bug)
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** App-spezifisch (BestJournalAndroid build.gradle.kts syncSecretsFromSk-Race), kein Plugin-Fix nötig. App-Owner-Empfehlung: in app/build.gradle.kts `syncSecretsFromSk.mustRunAfter(...)` und `processDebugGoogleServices.dependsOn(syncSecretsFromSk)` ergänzen.
- **Entdeckt:** 2026-05-18, Phase 4 Build-Validation
- **Symptom:** Der `syncSecretsFromSk`-Gradle-Task läuft nicht garantiert vor
  `processDebugGoogleServices`. Bei paralleler Gradle-Ausführung kann
  `google-services.json` noch fehlen wenn der Prozessschritt startet.
  Build schlägt mit `File not found: google-services.json` fehl.
- **Ursache:** In `app/build.gradle.kts` fehlt die explizite Task-Abhängigkeit:
  ```kotlin
  processDebugGoogleServices.dependsOn(syncSecretsFromSk)
  syncSecretsFromSk.mustRunAfter("preBuild")
  ```
- **Korrekturvorschlag:**
  - App-Owner-Empfehlung: obige zwei Zeilen in `app/build.gradle.kts` ergänzen.
  - Plugin-seitig kein Fix nötig — das Plugin kann diese Race Condition nicht
    selbst erkennen ohne App-spezifische Gradle-Analyse.
  - Optional in Phase 4 des Plugins: Gradle-Task-Dependency-Check als
    Empfehlung ausgeben wenn `syncSecretsFromSk`-Task ohne Abhängigkeit erkannt.
- **Betroffene Dateien:**
  - `~/proggs/BestJournalAndroid/app/build.gradle.kts` (App-Owner-Fix)

---

## FIN-022 — Phase 4 Build-Validation fehlt im Plugin

- **Severity:** kritisch (Build-Killer-Findings kommen durch ohne Build-Test)
- **Status:** fixed-2026-05-18
- **Patch-Referenz:** orchestrator.md Phase 4 + W4 Build-Validation (W1)
- **Entdeckt:** 2026-05-18, nach Phase 3 — Compose-Fehler (FIN-019) und
  Import-Fehler (FIN-020) wären ohne manuellen Build-Test unentdeckt geblieben.
- **Symptom:** Das Plugin hatte keine Phase 4 mit einem echten Build-Test.
  Die Fehler aus FIN-019 und FIN-020 wurden erst durch einen manuellen
  `./gradlew assembleDebug`-Aufruf außerhalb des Plugin-Workflows entdeckt.
- **Ursache:** `orchestrator.md` definiert keine Build-Validation-Phase als
  Output-Gate. Die Phase 4 fehlte komplett.
- **Korrekturvorschlag:**
  1. In `orchestrator.md` eine Phase 4 „Build-Validation" einführen:
     - `./gradlew assembleDebug --dry-run` als Smoke-Test (schnell, kein APK).
     - Bei Fehlern: Fehler-Log an Fix-Applier zurückgeben für sofortigen Re-Fix.
     - Erst bei grünem Build: Plugin-Lauf als erfolgreich markieren.
  2. W4-Worker (Build-Validation) als eigenen Worker-Typ in worker-template.md.
  3. Phase 4 ist das einzige Pflicht-Gate vor dem finalen Commit-Step.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Phase 4 neu einführen)

---

## Plugin v2.0 — Soll-Architektur (Backlog, Frank-Approval erforderlich)

Aus dem 6.5-Stunden-Plugin-Lauf am 2026-05-18 (BestJournal Android, 27 Findings + 27 Locales) sind 22 FIN-Bugs entstanden und gefixt. Die folgende Soll-Architektur fasst die wichtigsten Lessons zusammen.

### Kernprinzipien

1. **Map-Reduce als Default** — KEIN monolithischer Worker für Phasen mit
   >800 Strings oder >100 Files. Stattdessen: mehrere parallele Worker mit
   100k-Token-Cap + ein Synthesizer.

2. **Continuous-Spawning** — In der Übersetzungs-Phase NIEMALS sequentiell.
   10 Worker IMMER parallel im Background, sobald 1 fertig → sofort
   nächsten spawnen. Frank-Direktive.

3. **Validations vor + nach JEDEM Edit**:
   - Kotlin: Imports vollständig, Compose-Pattern korrekt, @hide-APIs vermeiden
   - XML: Wohlgeformtheit, keine Tag-Verschachtelungen, Apostrophe escaped
   - Bash: Cross-Platform (kein GNU-only sed/awk)

4. **Pre-Existence-Check** — Vor `missingDocs`-Findings IMMER prüfen ob
   das Dokument als Asset existiert. Spart 4-von-6-false-positives.

5. **Build-Validation als Plugin-Output-Gate** — Phase 4 MUSS einen echten
   Build-Test machen, nicht nur Grep+Coverage. Sonst kommen Build-Killer
   durch.

### Konkrete Plugin-Architektur-Empfehlung

- `orchestrator.md` definiert die Phasen-Logik (zentral)
- `fix-applier.md` ist der Standard-Worker für Code-/String-Edits
- `synthesizer.md` aggregiert Worker-Outputs
- `worker-template.md` ist die Vorlage für neue Worker-Typen

### Was Frank entscheiden muss vor v2.0-Release

- [ ] Soll Continuous-Spawning auch in Phase 2 (Fix-Anwendung) gelten,
      oder nur Phase 3 (Übersetzung)?
- [ ] Soll der `assembleDebug --dry-run`-Step automatisch laufen oder
      Frank-gesteuert ("starte Build-Test")?
- [ ] Soll FIN-021 (App-build.gradle-Race) als App-spezifischer Patch
      auch ins Plugin als generischer Workaround?

---

## FIN-023 — Parallelität erhöhen auf 15 Worker statt 10

- **Severity:** kritisch (4-5 Stunden Plugin-Laufzeit ist inakzeptabel laut Frank)
- **Status:** offen — wird in Folgesession W10/W12/W13/W14 umgesetzt
- **Entdeckt:** 2026-05-18, nach 6.5-Stunden-Plugin-Lauf BestJournal
- **Frank-Originalwortlaut (2026-05-18 22:00-Feedback):**
  > "Bitte in Zukunft beim Roentgenskill viel mehr Subagents gleichzeitig
  >  starten. Parallel starten. Meinetwegen 15 parallel. Alles kein Problem.
  >  Sie sollen einfach nur nicht über 100.000 Tokens kommen. Also viel mehr
  >  mit Parallelität arbeiten. Das auch beim Strings erstellen, wenn du
  >  Strings erstellst, mach da auch viel viel mehr parallele Subagents.
  >  Und wenn du viele Subagents laufen hast und hast aber noch mehr Aufgaben,
  >  dann warte nicht, bis die Subagents durchgelaufen sind, bis sie auf
  >  Null runter sind, bis alle Aufgaben erledigt sind, sondern wenn ein
  >  Subagent fertig ist, zum Beispiel wenn 15 gleichzeitig laufen, dann
  >  starte gleich den nächsten, damit keine Zeit verschwendet wird. Du
  >  lässt immer 15 Subagents gleichzeitig laufen. Das beim Roentgenskill,
  >  das gleiche beim Strings-Creator-Skill und das gleiche beim Übersetzer-
  >  Skill. Und auch wenn du Researcher hast, die im Internet noch neue
  >  Informationen suchen, auch diese Researcher nur 100k Token pro Searcher
  >  starten — nicht mehr, damit die nicht abstürzen. Und da auch viel mehr
  >  starten als nur fünf oder sechs, immer 15 oder so."
- **Symptom:** 6.5h Plugin-Laufzeit, sequentielle Wellen statt Continuous-Spawning,
  max 10 Worker in einigen Phasen.
- **Ursache:** Plugin-Default-Architektur ist konservativ (10 Worker, manchmal
  sequentiell).
- **Korrekturvorschlag:**
  1. `orchestrator.md`: 15 Worker als Default für Röntgen, String-Extraktor,
     Übersetzer und Researcher.
  2. Continuous-Spawning ist Pflicht (FIN-015 verstärken): sobald 1 fertig →
     SOFORT nächsten spawnen, nie auf Welle-Ende warten.
  3. Token-Cap 100k bleibt (FIN-004), gilt auch für Researcher.
  4. Researcher (Web-Suche) ebenfalls auf 15 parallel + 100k-Cap.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md`
  - `~/.claude/skills/app-roentgen/SKILL.md`
  - `~/.claude/skills/string-extraktor/SKILL.md`
  - `~/.claude/skills/uebersetzung/SKILL.md`

---

## FIN-024 — Build-Validation-Pflicht (Compose-Pattern, Imports, @hide-APIs, Apostrophe)

- **Severity:** kritisch (Build-Brecher-Bugs erst beim finalen assembleDebug entdeckt)
- **Status:** offen
- **Entdeckt:** 2026-05-18, nach Plugin-Lauf — 6 Compile-/AAPT-Fehler beim Build
- **Frank-Originalwortlaut (2026-05-18 22:00-Feedback):**
  > "Als wir die APK bauen wollten und sie auf dem Handy installieren wollten,
  >  hast du auf einmal ganz viele zusätzliche Fehler noch gefunden. Das kann
  >  eigentlich nicht sein, dass du so viele Fehler mit eingebaut hast.
  >  Da nochmal die Gründe herausfinden und das fixen, dass das in Zukunft
  >  nicht mehr passieren kann. Alle Fixes nach Direktive 3, dass die nie
  >  wieder auftreten können und robust sind."
- **Symptom:** Beim Build-Versuch nach Plugin-Lauf wurden 6 Compile-/AAPT-Fehler
  entdeckt:
  1. `ko/strings.xml` hatte verschachtelte XML-Tags (vgl. FIN-016)
  2. `fr/strings.xml` hatte 10 unescapte Apostrophe (vgl. FIN-017)
  3. `uk/strings.xml` hatte 1 unescapten Apostrophen (vgl. FIN-018)
  4. `ConsentScreen.kt`: verschachtelte Funktion `urlFor` rief `stringResource`
     auf (nur innerhalb von `@Composable` erlaubt)
  5. `SettingsScreen.kt`: fehlende Imports (vgl. FIN-020)
  6. `SettingsScreen.kt`: `ACTION_BACKUP_SETTINGS` ist `@hide`-API (vgl. FIN-020)
- **Ursache:** Kein Build-Validation-Step zwischen Phase 2 (Fix anwenden) und
  Phase 4 (Re-Audit). Die Pre-Phase-4-Worker prüfen XML-Wohlgeformtheit, aber
  AAPT-Spezifika (Apostrophe-Escape, `@hide`-APIs) und Kotlin-Compose-Pattern
  werden erst beim echten Build entdeckt.
- **Korrekturvorschlag (nach Direktive 3 — robust und nie wieder):**
  1. Pflicht-Step in Phase 2 nach JEDEM Fix-Applier-Run: `gradle aapt-compile`
     auf die betroffene `values-*`-Datei (5–10 Sekunden, machbar).
  2. Pflicht-Step in Phase 2 nach Kotlin-Änderungen: Kotlin-Compile-Quick-Check
     mit allen Imports.
  3. `fix-applier.md` (W2) hat die Pflicht-Validation bereits dokumentiert —
     sie muss aber AUSGEFÜHRT werden, nicht nur als Textabschnitt stehen.
  4. Phase 4 W4 (FIN-022): `./gradlew assembleDebug --dry-run` als Gate vor
     "completed-success"-Meldung. Bei Build-Fail: Pipeline auf `build-broken`
     setzen, NICHT auf `completed`.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Phase 2 + 4)
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/fix-applier.md`

---

## FIN-025 — Recht-Findings nach Kategorie-ID-Schema + farbig

- **Severity:** mittel (UX-Problem, kein technischer Bug)
- **Status:** offen
- **Entdeckt:** 2026-05-18, nach Phase 1B — Findings-Präsentation zu
  unübersichtlich
- **Frank-Originalwortlaut (2026-05-18 22:00-Feedback):**
  > "Als der Rechtssicherheit-Skill Probleme gefunden hat, diese vorgetragen
  >  wurden, aber die überhaupt nicht übersichtlich genug waren. Teilweise
  >  mit T-008 — das sind Bezeichnungen. Mach doch einfach ein Buchstabe A
  >  und dann 1. A1, 2, 3, 4, 5 bis kannst du bis 99 machen oder so. Dann
  >  hat man die Fälle durch, dann kannst du B machen. C, D, E, nach
  >  Kategorien sozusagen. Dass ich dann einfach sage, für A1 machst du
  >  bitte Vorschlag B. Und das Ganze ein bisschen optisch, ein bisschen
  >  übersichtlicher gestalten. Das Problem skizzieren, die genaue Zeile
  >  und dann die Vorschläge. Auch mit verschiedenen Farben ein bisschen
  >  arbeiten, dass man das optisch ein bisschen unterscheiden kann."
- **Symptom:** Findings-IDs `T-001` (textual), `AM-001` (advertising),
  `MD-001` (missing-docs), `PS-001` (play-store) sind technisch korrekt,
  aber Frank musste ständig zurückscrollen um Kategorien zuzuordnen.
  Karten-Format ist zu textlastig, keine visuelle Differenzierung nach
  Schwere.
- **Korrekturvorschlag:**
  1. Neues Kategorie-Buchstaben-Schema:
     - `A1–A99` → HWG/Gesundheitsversprechen
     - `B1–B99` → UWG/Werbung/Irreführung
     - `C1–C99` → DSGVO/Datenschutz
     - `D1–D99` → BGB/Widerruf/Vertragsrecht
     - `E1–E99` → Play-Store-Richtlinien
     - `F1–F99` → Dark-Pattern
     - `G1–G99` → MissingDocs
     - `Z1–Z99` → Sonstige
  2. Karten-Layout neu:
     ```
     ┌─────────────────────────────────────────────────────────────┐
     │ A1 🟥 HWG §3 (Heilversprechen)                              │
     │ Datei: app/src/main/res/values/strings.xml:1191             │
     ├─────────────────────────────────────────────────────────────┤
     │ AKTUELL: "Finde deine innere Ruhe"                          │
     │ PROBLEM: Therapeutik-Versprechen im Paywall-Headline        │
     ├─────────────────────────────────────────────────────────────┤
     │ [a] "Mehr Klarheit im Alltag" (Δ -8%)                       │
     │ [b] "Klarere Gedanken jeden Tag" (Δ -5%)                    │
     │ [c] Eigene Formulierung                                     │
     │ [skip] Überspringen                                         │
     └─────────────────────────────────────────────────────────────┘
     ```
  3. ANSI-Farben in der Terminal-Ausgabe:
     - 🟥 kritisch: roter Header (`\033[91m`)
     - 🟧 mittel: orange (`\033[93m`)
     - 🟨 niedrig: gelb (`\033[33m`)
  4. Synthesizer mappt die Worker-IDs (`T-`, `AM-`, `MD-`, `PS-`) auf das
     neue A/B/C/D/E/F/G-Schema beim Konsolidieren.
- **Betroffene Dateien:**
  - `~/.claude/skills/rechtssicherheit/SKILL.md` (Output-Schema + Karten-Template)
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Phase-2-Karten)
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/fix-applier.md` (akzeptiert neue IDs)

---

## FIN-026 — Agent Teams nutzen, nicht nur Subagents

- **Severity:** mittel
- **Status:** offen
- **Entdeckt:** 2026-05-18, Feedback nach Plugin-Lauf
- **Frank-Originalwortlaut (2026-05-18 22:00-Feedback):**
  > "Da auf jeden Fall viel mehr parallele Agents laufen lassen. Du kannst
  >  auch mit Agent Teams arbeiten. Ich weiß gar nicht, ob du jetzt Agent
  >  Teams überhaupt benutzt hattest. Mit mehr Teams und viel mehr Unter-
  >  agenten, Subagenten, also viel viel mehr davon, nicht nur ein paar.
  >  Kannst du viel mehr Aufgaben gleichzeitig machen, dann läuft das Ganze
  >  viel schneller. Und bei Agent Teams kann ja der Team-Leader das Ganze
  >  ja alles überprüfen."
- **Symptom:** Plugin nutzt ausschließlich Subagents (Agent-Tool mit
  `subagent_type`), keine Agent-Teams (TeamCreate). Team-Leader-Pattern
  fehlt — Subagent-Outputs werden nicht peer-reviewed bevor sie als final
  akzeptiert werden.
- **Korrekturvorschlag:**
  1. Bei großen Phasen (Phase 1B mit 3–5 Workers, Phase 3 mit 27 Sprachen):
     Agent-Team statt einzelne Subagents einsetzen.
  2. Team-Leader macht Quality-Gate: bevor Worker-Output als final akzeptiert
     wird, prüft der Team-Leader auf XML-Wohlgeformtheit, Format-Strings,
     Umlaute und Compose-Pattern-Korrektheit.
  3. Passt zum CLAUDE.md-Prinzip: "Agent Teams sind wie ein Büro mit mehreren
     Mitarbeitern die untereinander reden können." Genau das braucht Phase 1B
     (Recht-Worker ↔ Synthesizer) und Phase 3 (Übersetzer ↔ Cross-Lingual-Reviewer).
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Phase 1B, Phase 3)

---

## FIN-027 — Umlaut-Pflicht im GESAMTEN Plugin (Cross-cutting)

- **Severity:** kritisch (App-User-Erfahrung in DE)
- **Status:** offen
- **Entdeckt:** 2026-05-18, Feedback nach Plugin-Lauf
- **Frank-Originalwortlaut (2026-05-18 22:00-Feedback):**
  > "Dann ist mir aufgefallen, dass das gesamte Plugin nicht mit deutschen
  >  Umlauten arbeitet. Deutsche Strings oder deutsche Informationen, die
  >  hinzugefügt werden, sollen auch mit deutschen Umlauten immer gemacht
  >  werden, in dem gesamten Plugin. Ohne Ausnahme. Weil ich möchte später
  >  eine App haben mit deutschen Umlauten drin und nicht ae/oe/ue und so
  >  weiter. Deshalb muss auch das gesamte Fixing mit deutschen Umlauten
  >  laufen. Das muss auch nochmal irgendwo tief in das Plugin mit rein."
- **Symptom:** FIN-011 hat die Umlaut-Pflicht im `uebersetzung`-Skill und im
  `orchestrator` verankert, aber die anderen Skills (`app-roentgen`,
  `rechtssicherheit`, `string-extraktor`) prüfen das nicht systematisch.
  Zusätzlich haben Plugin-eigene Outputs (`audit-log.md`, `ISSUES.md`,
  `recht-report.json` findings.rationale-Felder) manchmal ae/oe/ue/ss aus
  alten Rule-Konventionen übernommen.
- **Korrekturvorschlag:**
  1. In JEDEM Skill (`string-extraktor`, `app-roentgen`, `rechtssicherheit`,
     `uebersetzung`): einen Pflicht-Absatz „Umlaut-Pflicht" als ersten
     inhaltlichen Block im Skill-Body einführen.
  2. `fix-applier.md` (W2) hat bereits den Umlaut-Verifikationsblock — er
     muss aber AUSGEFÜHRT werden, nicht nur dokumentiert sein.
  3. `orchestrator.md`: Pflicht-Verifikation nach JEDEM Worker-Output — Auto-Grep
     auf `\b(ae|oe|ue|ss)\b` in deutschen Textblöcken, Stop bei Fund.
  4. Plugin-eigene Markdown-Dateien (`ISSUES.md`, `audit-log.md`) ebenfalls
     mit echten Umlauten schreiben. Das Stil-Vorbild ist Frank's eigener Wortlaut.
- **Betroffene Dateien:**
  - `~/.claude/skills/string-extraktor/SKILL.md`
  - `~/.claude/skills/app-roentgen/SKILL.md`
  - `~/.claude/skills/rechtssicherheit/SKILL.md`
  - `~/.claude/skills/uebersetzung/SKILL.md`
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md`
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/fix-applier.md`

---

## FIN-028 — Vordergrund-Sichtbarkeit der Worker

- **Severity:** niedrig (UX, nicht funktional)
- **Status:** offen
- **Entdeckt:** 2026-05-18, Feedback nach Plugin-Lauf
- **Frank-Originalwortlaut (2026-05-18 22:00-Feedback):**
  > "Und zwar im Hintergrund. Ich möchte, dass man die im Vordergrund sieht,
  >  nicht im Hintergrund. Die Übersetzer."
- **Symptom:** Background-Worker (`run_in_background: true`) liefern nur die
  finale Notification — Frank sieht nicht, was sie während des Laufs tun.
  Der Übersetzer-Pool ist quasi unsichtbar bis alle Sprachen fertig sind.
- **Korrekturvorschlag (Tradeoff):**
  - Foreground-Subagents: sichtbar im Tool-Call-Output, blockieren aber bis
    alle fertig sind, Continuous-Spawning schwieriger.
  - Background-Subagents: Continuous-Spawning möglich, aber weniger Sichtbarkeit.
  - Kompromiss-Lösung:
    1. Beim Start jeder Welle einen klaren Status-Block ausgeben:
       ```
       Welle 1 (15 Workers): ar, bn, en, es, fr, gu, hi, it, ja, kn, ko, ml,
       mr, nl, pl — alle gestartet im Background
       ```
    2. Pro Worker: aussagekräftiges `description`-Feld z. B.
       `"Translate: ar — 31 Strings × Devanagari-Validierung"`.
    3. Orchestrator druckt Live-Stats nach jeder abgeschlossenen Welle:
       `"X von Y abgeschlossen, Z noch laufend"`.
- **Betroffene Dateien:**
  - `~/proggs/Umgebung/Plugins/finale/Plugin/agents/orchestrator.md` (Phase 3)
