# Arbeitsweise / Verhalten — Best Practices (Stand 2026-06-05, Claude Code 2.1.165)

> Quelle: Offizielle Anthropic-Dokumentation (code.claude.com/docs/en/best-practices)  
> Recherche-Datum: 2026-05-28 / 2026-05-30

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | Grund-Workflow | Explore → Plan → Implement → Commit (EPIC) | EPIC-Workflow |
| 2 | hoechster Hebel | Verifizierung einbauen — kein „fertig“ ohne frischen Beweis | Verifizierung einbauen |
| 3 | staerkstes Einzelmuster | Test-Driven Development (Test zuerst) | Test-Driven Development |
| 4 | Kontext-Hygiene | `/clear` gezielt zwischen unabhaengigen Aufgaben | /clear-Hygiene |
| 5 | Qualitaet | Writer/Reviewer-Trennung (separate Sessions) | Writer/Reviewer-Trennung |
| 6 | viele Dateien | Fan-out — parallel arbeiten | Fan-out |
| 7 | Pflicht-Verhalten | Hooks statt CLAUDE.md (deterministisch erzwungen) | Hooks fuer deterministische Regeln |

---

## Kernprinzip: Kontextfenster ist die wichtigste Ressource

- **Was:** Claudes Kontextfenster füllt sich schnell mit Nachrichten, gelesenen Dateien und Befehlsausgaben. Wenn es voll wird, degradiert die Qualität messbar — Claude „vergisst" frühere Anweisungen oder macht mehr Fehler.
- **Best Practice:** Kontext als die wertvollste Ressource behandeln. Jede Entscheidung über Workflow, Planung, Subagenten und `/clear` folgt aus diesem Prinzip. Qualität degradiert spürbar ab 60–80% des Kontextfensters.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Explore → Plan → Implement → Commit (EPIC-Workflow)

- **Was:** Der empfohlene 4-Phasen-Workflow trennt Erkundung, Planung, Implementierung und Commit strikt voneinander. Direkt zum Coden zu springen löst häufig das falsche Problem.
- **Best Practice:**
  1. **Explore (Plan-Modus):** Claude liest Dateien, beantwortet Fragen — macht keine Änderungen. Beispiel-Prompt: `"Lese /src/auth und erkläre wie Sessions funktionieren. Schreibe noch keinen Code."`
  2. **Plan (Plan-Modus):** Claude erstellt einen detaillierten Implementierungsplan. Mit `Ctrl+G` kann der Plan im Editor direkt bearbeitet werden, bevor Claude loslegt.
  3. **Implement (normaler Modus):** Claude setzt den Plan um, schreibt Tests, führt sie aus.
  4. **Commit:** Claude committet mit aussagekräftiger Nachricht, öffnet optional einen PR.

  **Wann Planung überspringen:** Bei eindeutigem Scope und kleinem Fix (Tippfehler, Log-Zeile, Variable umbenennen). Wenn der Diff in einem Satz beschreibbar ist, ist Planning Overhead.

  **Wann Planung Pflicht:** Unsicherer Ansatz, Änderung betrifft mehrere Dateien, unbekannter Code-Bereich.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Verifizierung einbauen — der höchste Hebel

- **Was:** Claude arbeitet dramatisch besser, wenn es seine Arbeit selbst überprüfen kann (Tests ausführen, Screenshots vergleichen, Outputs validieren). Ohne klare Erfolgskriterien ist man selbst der einzige Feedback-Loop.
- **Best Practice:**
  - Immer Verifikationskriterien mitgeben: Tests, erwartete Outputs, Screenshots, Lint-Befehle.
  - Beispiel: Statt `"implementiere eine E-Mail-Validierungsfunktion"` lieber: `"schreibe validateEmail, Testfälle: user@example.com=true, invalid=false, user@.com=false. Führe Tests danach aus."`
  - UI-Änderungen: Screenshot mitschicken, Claude vergleicht das Ergebnis mit dem Original.
  - Root Cause adressieren, nicht Symptom unterdrücken: `"Behebe den Build-Fehler [Fehlertext]. Verifiziere dass Build danach grün ist. Unterdrücke den Fehler nicht."`
  - Die Verifizierung kann auch ein Test-Suite, ein Linter oder ein Bash-Befehl sein, der Output prüft. In die Verifikation investieren.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Test-Driven Development (TDD) — das stärkste Einzelmuster

- **Was:** TDD ist das wirkungsvollste Pattern beim Arbeiten mit agentic Coding-Tools. Tests liefern Claude unzweideutige Erfolgskriterien und ermöglichen selbstständige Iteration ohne menschliche Intervention bei jedem Schritt.
- **Best Practice:**
  1. **Tests zuerst schreiben** — Claude schreibt von Natur aus erst Implementation, dann Tests. TDD muss explizit angewiesen werden.
  2. **Fehlschlag bestätigen** — Sicherstellen dass der neue Test tatsächlich rot ist.
  3. **Fehlschlagende Tests committen** — Checkpoint vor der Implementation. Wenn Claude die Tests ändert statt die Implementation, zeigt der Diff genau was sich verändert hat und man kann revertieren.
  4. **Implementieren bis grün** — Claude darf Tests nicht ändern, nur die Implementation.

  **Warum TDD gegen Kontext-Degradation hilft:** Tests sind „externe Orakel" — Claude kann seine Arbeit unabhängig vom Kontext-Zustand verifizieren.

  **Wichtige Anmerkung (extern bestätigt):** Ohne explizite Anweisung schreibt Claude zuerst Implementation, dann Tests. TDD erfordert im Prompt: `"Schreibe zuerst einen fehlschlagenden Test, bestätige dass er rot ist, dann implementiere."`
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell); https://medium.com/@moradikor296/the-tdd-paradigm-shift-why-test-driven-development-is-claude-codes-killer-discipline-9be9616d79f6 (extern)
- **Stand:** 2026-05-25

---

## /clear-Hygiene — Kontext gezielt zurücksetzen

- **Was:** Lange Sessions mit irrelevantem Kontext reduzieren die Qualität. Wenn Claude zu viele fehlgeschlagene Korrekturen sieht, ist der Kontext mit schlechten Ansätzen vergiftet.
- **Best Practice:**
  - `/clear` zwischen **unabhängigen Aufgaben** ausführen — nicht zwischen Schritten derselben Aufgabe.
  - **„Kitchen-Sink-Session" vermeiden:** Eine Aufgabe, dann etwas Unverwandtes, dann zurück — Kontext ist voll mit irrelevanten Informationen. Fix: `/clear` zwischen Aufgaben.
  - **Korrektur-Loop erkennen:** Wenn Claude dasselbe Problem mehr als zweimal falsch macht, `/clear` und mit einem besseren, spezifischeren Prompt neu starten. Eine frische Session mit besserem Prompt schlägt fast immer eine lange Session mit angesammelten Korrekturen.
  - `/compact <Anweisung>` für selektive Verdichtung: z.B. `/compact Focus on the API changes`.
  - `/btw` für Nebenfragen: Antwort erscheint als Overlay, landet nie im Kontext.
  - In CLAUDE.md können Compaction-Anweisungen hinterlegt werden: `"When compacting, always preserve the full list of modified files and any test commands."`
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Writer/Reviewer-Trennung (Separate Sessions)

- **Was:** Frischer Kontext verbessert Code-Reviews, weil Claude nicht durch eigenen gerade geschriebenen Code voreingenommen ist. Ein Claude schreibt, ein anderer überprüft.
- **Best Practice:**

  | Session A (Writer) | Session B (Reviewer) |
  |---|---|
  | `"Implementiere einen Rate-Limiter für unsere API-Endpunkte"` | |
  | | `"Überprüfe die Rate-Limiter-Implementation in @src/middleware/rateLimiter.ts. Suche nach Edge Cases, Race Conditions und Konsistenz mit bestehenden Middleware-Patterns."` |
  | `"Hier ist das Review-Feedback: [Session-B-Output]. Behebe diese Punkte."` | |

  **Analoges Muster mit Tests:** Eine Session schreibt Tests, eine andere schreibt Code zum Bestehen.

  **Warum es funktioniert:** Claude, das Code reviewt den es nicht geschrieben hat, ist unvoreingenommen und erkennt Edge Cases die beim Schreiben übersehen wurden.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Spezifischer Kontext im Prompt

- **Was:** Claude kann Absichten ableiten, aber keine Gedanken lesen. Unspezifische Prompts produzieren unspezifische Ergebnisse.
- **Best Practice:**
  - **Scope eingrenzen:** Datei, Szenario, Test-Präferenzen konkret angeben.
  - **Quellen zeigen:** `"Schau dir die Git-History von ExecutionFactory an und erkläre wie die API so wurde"` statt `"Warum hat ExecutionFactory so eine seltsame API?"`
  - **Bestehende Patterns referenzieren:** `"Sieh dir HotDogWidget.php als Beispiel an, folge diesem Pattern für ein neues Kalender-Widget"`.
  - **Symptom beschreiben:** Symptom + wahrscheinlicher Ort + Definition von „gefixt".
  - **Rich Content nutzen:** `@Datei` statt Dateipfade zu beschreiben, Screenshots einfügen (Copy/Paste oder Drag & Drop), `cat error.log | claude` für direkten Daten-Input.

  **Wann vage Prompts sinnvoll sind:** Bei explorativer Arbeit, wenn man sehen will wie Claude ein Problem interpretiert, bevor man es einschränkt.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Subagenten für Recherche und Verifikation

- **Was:** Subagenten erkunden in separaten Kontextfenstern und berichten zurück — ohne den Hauptkontext zu belasten. Da Kontext die fundamentale Einschränkung ist, sind Subagenten eines der mächtigsten verfügbaren Werkzeuge.
- **Best Practice:**
  - Für Recherche: `"Nutze Subagenten um zu untersuchen wie unser Authentifizierungssystem Token-Refresh handhabt und ob es bestehende OAuth-Utilities gibt."`
  - Für Verifikation nach Implementation: `"Nutze einen Subagenten um diesen Code auf Edge Cases zu prüfen."`
  - Für parallele Arbeit an verschiedenen Dateien: Jeder Subagent bekommt eigene Dateien (Datei-Ownership strikt trennen).
  - Bei Exploration ohne Scope-Eingrenzung Subagenten verwenden — sonst füllt die Erkundung den Hauptkontext.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Claude interviewen lassen (für größere Features)

- **Was:** Für größere Features Claude zuerst Interview-Fragen stellen lassen statt direkt zu implementieren. Claude fragt nach Dingen die man noch nicht bedacht hat: technische Implementation, UI/UX, Edge Cases, Tradeoffs.
- **Best Practice:**
  ```
  Ich möchte [kurze Beschreibung] bauen. Interviewe mich detailliert mit dem AskUserQuestion-Tool.
  Frage nach technischer Implementation, UI/UX, Edge Cases, Bedenken und Tradeoffs.
  Stelle keine offensichtlichen Fragen, geh in die schwierigen Teile.
  Schreibe danach eine vollständige Spec in SPEC.md.
  ```
  Nach dem Interview: **Neue Session starten** für die Implementation. Neue Session hat sauberen Kontext der sich voll auf die Umsetzung konzentriert, und man hat eine schriftliche Spec als Referenz.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Häufige Anti-Patterns (Vermeiden)

- **Was:** Typische Fehler die Zeit kosten und Qualität reduzieren — offiziell von Anthropic dokumentiert.
- **Best Practice (Vermeiden):**

  | Anti-Pattern | Fix |
  |---|---|
  | **Kitchen-Sink-Session:** Eine Aufgabe, dann Unverwandtes, dann zurück — Kontext voller irrelevanter Info | `/clear` zwischen unabhängigen Aufgaben |
  | **Korrektur-Loop:** Claude macht etwas falsch, korrigieren, immer noch falsch, wieder korrigieren | Nach 2 Fehlkorrekturen: `/clear` und besseren Prompt schreiben |
  | **Überspezifizierte CLAUDE.md:** Zu lange Datei, Claude ignoriert die Hälfte, wichtige Regeln gehen unter | Gnadenlos kürzen. Wenn Claude etwas ohne die Anweisung richtig macht: raus damit oder als Hook |
  | **Trust-then-verify-Gap:** Plausibel aussehende Implementation die Edge Cases nicht behandelt | Immer Verifikation bereitstellen. Nicht shippen ohne Verifizierung |
  | **Infinite Exploration:** „Untersuche X" ohne Eingrenzung — Claude liest hunderte Dateien, füllt Kontext | Scope eng eingrenzen oder Subagenten nutzen |
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Checkpoints und Rewind

- **Was:** Jeder gesendete Prompt erstellt einen Checkpoint. Man kann Konversation, Code oder beides auf jeden früheren Checkpoint zurücksetzen.
- **Best Practice:**
  - `Esc` stopp, `Esc+Esc` oder `/rewind` zum Öffnen des Rewind-Menüs.
  - Optionen: nur Konversation zurücksetzen, nur Code zurücksetzen, beides, oder von einem Checkpoint aus zusammenfassen.
  - Riskante Ansätze einfach ausprobieren — wenn sie nicht funktionieren, zurückspulen und einen anderen Weg versuchen.
  - Checkpoints bleiben über Sessions hinweg erhalten (Terminal schließen und später trotzdem rewind).
  - **Achtung:** Checkpoints tracken nur Änderungen *von Claude*, nicht von externen Prozessen. Kein Ersatz für git.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Sessions benennen und fortführen

- **Was:** Claude Code speichert Konversationen lokal. Sessions können benannt, fortgeführt und wie Branches behandelt werden.
- **Best Practice:**
  - `claude --continue` für die letzte Session.
  - `claude --resume` für Auswahl aus einer Liste.
  - `/rename` für aussagekräftige Session-Namen wie `oauth-migration`.
  - Jeder Workstream bekommt seine eigene persistente Session.
  - Nie die letzte Session als einzige Aufzeichnung von Entscheidungen verwenden — häufig committen, Fortschritt in Dateien dumpen, jede Session als disposable behandeln.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Hooks für deterministische Regeln

- **Was:** CLAUDE.md-Anweisungen sind advisory (ca. 70% Befolgungsrate laut externen Quellen). Hooks sind deterministisch und garantieren dass eine Aktion passiert — keine Ausnahmen.
- **Best Practice:**
  - Hooks für Aktionen nutzen die bei null Ausnahmen passieren müssen.
  - Claude kann Hooks selbst schreiben: `"Schreibe einen Hook der eslint nach jedem Datei-Edit ausführt"`, `"Schreibe einen Hook der Writes in den Migrations-Ordner blockiert"`.
  - Hooks in `.claude/settings.json` konfigurieren, mit `/hooks` durchsuchen.
  - Wenn Claude trotz CLAUDE.md-Regel etwas Unerwünschtes tut: Als Hook umsetzen statt Regel zu verlängern.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell); https://www.datacamp.com/tutorial/claude-code-best-practices (extern)
- **Stand:** 2026-05-25

---

## Effektive CLAUDE.md — kurz und präzise

- **Was:** CLAUDE.md ist die Datei die Claude bei jedem Gesprächsstart liest. Sie enthält persistenten Kontext den Claude nicht aus dem Code ableiten kann. Zu lange Dateien führen dazu dass Claude Regeln ignoriert.
- **Best Practice:**
  - **`/init`** ausführen um eine Basis zu generieren, dann iterativ verfeinern.
  - Für jede Zeile fragen: *„Würde das Entfernen dazu führen dass Claude Fehler macht?"* Wenn nein: raus.
  - Was hinein gehört: Bash-Befehle die Claude nicht erraten kann, Code-Style-Regeln die von Defaults abweichen, Test-Anweisungen, Repo-Etikette, Architektur-Entscheidungen, häufige Fallstricke, nicht-offensichtliches Verhalten.
  - Was nicht hinein gehört: Standard-Sprachkonventionen die Claude kennt, API-Dokumentation (besser verlinken), Dinge die sich häufig ändern, datei-für-datei Codebase-Beschreibungen, selbstverständliche Praktiken wie „schreibe sauberen Code".
  - Mit `@path/to/import` können weitere Dateien eingebunden werden.
  - CLAUDE.md in git einchecken damit das ganze Team beitragen kann — der Wert wächst über Zeit.
  - Betonung mit „IMPORTANT" oder „YOU MUST" verbessert die Befolgungsrate.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Fan-out — paralleles Arbeiten an vielen Dateien

- **Was:** Für große Migrationen oder Analysen kann Arbeit über viele parallele Claude-Instanzen verteilt werden — bis zu 10x schneller als sequenziell.
- **Best Practice:**
  1. Claude listet alle zu migrierenden Dateien.
  2. Script loopt durch die Liste mit `claude -p` für jede Datei.
  3. An 2-3 Dateien testen, Prompt verfeinern, dann auf gesamtem Set ausführen.

  ```bash
  for file in $(cat files.txt); do
    claude -p "Migriere $file von React zu Vue. Antworte OK oder FAIL." \
      --allowedTools "Edit,Bash(git commit *)"
  done
  ```

  `--allowedTools` einschränken wenn Claude unbeaufsichtigt läuft. `--verbose` für Debugging in der Entwicklung, in Produktion ausschalten.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## CLI-Tools nutzen für externe Services

- **Was:** CLI-Tools wie `gh`, `aws`, `gcloud`, `sentry-cli` sind der token-effizienteste Weg mit externen Services zu interagieren. Claude kennt diese Tools und kann sie effektiv nutzen.
- **Best Practice:**
  - GitHub CLI (`gh`) installieren: Claude nutzt es für Issues, Pull Requests, Kommentare lesen. Ohne `gh` fällt Claude auf die GitHub API zurück, die oft Rate-Limits trifft.
  - Claude kann auch unbekannte CLI-Tools lernen: `"Nutze 'foo-cli-tool --help' um das Tool zu verstehen, dann löse damit A, B, C."`
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## Intuition entwickeln — wann welches Pattern

- **Was:** Die Patterns sind keine festen Regeln sondern Startpunkte. Manchmal ist es richtig Kontext akkumulieren zu lassen, manchmal Planning zu überspringen, manchmal einen vagen Prompt zu verwenden.
- **Best Practice:**
  - Beachten was bei guten Outputs anders war: Prompt-Struktur, bereitgestellter Kontext, verwendeter Modus.
  - Bei schlechten Outputs analysieren warum: Kontext zu laut? Prompt zu vage? Aufgabe zu groß für einen Pass?
  - **Spezifisch sein:** Wenn Korrekturen vorhersehbar sind.
  - **Offen sein:** Wenn man erkunden will wie Claude ein Problem interpretiert.
  - **Planen:** Bei Unklarheit über den Ansatz oder mehrere betroffene Dateien.
  - **Direkt umsetzen:** Bei klarem, kleinem Scope.
- **Quelle:** https://code.claude.com/docs/en/best-practices (offiziell)
- **Stand:** 2026-05-25

---

## NEU (2.1.152): Auto Mode — kein Opt-in mehr erforderlich

- **Was:** Auto Mode ist ein KI-gestützter Sicherheitsmodus zwischen manuellem Genehmigen und vollständigem Bypass. Ein Klassifizierer evaluiert Aktionen automatisch: er blockiert potenziell gefährliche Operationen (Scope-Eskalation, nicht vertrauenswürdige Infrastruktur, Prompt-Injection) und lässt sichere Aktionen ohne Benutzer-Prompt durch. Bis 2.1.151 war dafür ein explizites Opt-in-Consent erforderlich. Ab 2.1.152 entfällt dieser Schritt.
- **Best Practice:**
  - Aktivierung: `--permission-mode auto` im CLI oder Modusauswahl in VS Code / Desktop-App.
  - Nur auf Max, Team und Enterprise verfügbar. Pro, Bedrock, Vertex und Foundry **nicht unterstützt**.
  - Benötigt: Sonnet 4.6, Opus 4.6 oder Opus 4.7 (Haiku nicht unterstützt).
  - Team- und Enterprise-Admins müssen Auto Mode in den Organization-Settings zuerst aktivieren.
  - **Praktische Bedeutung:** Wer bisher Auto Mode nicht nutzte weil der Consent-Dialog störte, kann ihn jetzt friktionslos aktivieren — kein extra Schritt mehr.
  - **Hinweis:** Der Klassifizierer läuft in zwei Phasen (schneller Filter → Chain-of-Thought bei Verdacht). Lokale Dateivorgänge und vordefinierte Erlaubnisregeln werden sofort genehmigt.
  - **NEU (2.1.154):** Anthropic empfiehlt explizit Auto Mode zu aktivieren wenn Dynamic Workflows genutzt werden — ein Workflow der mit hunderten Subagenten läuft und bei jeder Genehmigung pausiert ist nicht wirklich parallel.
- **Quelle:** https://claudefa.st/blog/guide/development/auto-mode (extern, Zusammenfassung aus Changelog); https://releasebot.io/updates/anthropic/claude-code (extern); offizielles Changelog verbatim: „Auto mode no longer requires opt-in consent" (v2.1.152)
- **Unsicherheit:** Die offizielle Anthropic-Dokumentationsseite zu Auto Mode konnte nicht direkt abgerufen werden. Beschreibung basiert auf einem Community-Summary-Artikel. Als `offiziell` markiert nur das Changelog-Zitat selbst; die technischen Details sind `extern`.
- **Stand:** 2026-05-28

---

## NEU (2.1.152/154): /simplify vs /code-review — klare Trennung

- **Was:** `/code-review --fix` und `/simplify` wurden in 2.1.152/154 neu voneinander abgegrenzt. Sie haben jetzt klar unterschiedliche Scopes und Ziele.
- **Best Practice:**

  | Befehl | Scope | Wann nutzen |
  |---|---|---|
  | `/code-review` | Vollständiges Bug-Hunting: Fehler, Edge Cases, Sicherheitsprobleme, Logikfehler — **liest nur, ändert nichts** | Wenn man wissen will ob Code korrekt ist |
  | `/code-review --fix` | Wie oben, aber **wendet die Befunde direkt an** | Wenn man Befunde automatisch angewendet haben will (git-Stand vorher sichern!) |
  | `/simplify` | **Cleanup-only**: Wiederverwendung, Vereinfachung, Effizienz, Abstraktionsniveau — kein Bug-Hunting | Wenn Code funktioniert, aber zu komplex/redundant ist |

  **Wichtige Nuance (NEU ab 2.1.154):** `/simplify` ist seit 2.1.154 ein eigenständiger Cleanup-Review — **kein** Alias mehr für `/code-review --fix`. Es führt keine Bug-Hunting-Analyse durch. Wer Security-Lücken oder Logic-Errors sucht: `/code-review` nutzen, nicht `/simplify`.

  **Workflow-Empfehlung:**
  1. Vor Einsatz von `--fix`: `git commit` oder `git stash` — der Befehl ändert Dateien ohne Bestätigung.
  2. Cleanup nach Feature-Implementierung: `/simplify` — schnell, kein Overhead durch Bug-Hunting.
  3. Vor einem PR: `/code-review` für gründliche Analyse.
  4. Effort kombinierbar: `/code-review high` für tiefere Analyse.
- **Quelle:** https://dev.classmethod.jp/en/articles/20260529-claude-code-updates-v2-1-154/ (extern); offizielles Changelog verbatim: „/simplify now runs a cleanup-only review (reuse, simplification, efficiency, altitude) and applies the fixes, instead of running the full /code-review --fix bug-hunting review" (v2.1.154)
- **Stand:** 2026-05-30

---

## NEU (2.1.153): /model speichert Auswahl dauerhaft als Default für neue Sessions

- **Was:** Bis v2.1.152 galt eine per `/model` gewählte Modell-Auswahl nur für die aktuelle Session. Ab v2.1.153 speichert `/model` die Wahl dauerhaft in den User-Settings (`model`-Feld) und gilt damit als Default für alle neuen Sessions — konsistent mit dem Verhalten in den IDE-Integrationen (VS Code, JetBrains).
- **Best Practice:**
  - **Default ändern:** `/model <name>` eingeben oder `Enter` im Picker drücken → dauerhaft gespeichert.
  - **Nur aktuelle Session wechseln:** Im Picker `s` drücken statt `Enter` → kein Schreiben in Settings.
  - **Breaking Change für Keybindings:** Wer `modelPicker:setAsDefault` in `keybindings.json` konfiguriert hatte, muss es in `modelPicker:thisSessionOnly` umbenennen (die alte `d`-Taste wurde durch `s` ersetzt).
  - **Resumed Sessions:** Beim Fortsetzen einer alten Session (`--resume`, `--continue`) wird das damals verwendete Modell beibehalten — das neue Default greift nicht rückwirkend.
  - **Projekt/Managed-Settings haben Vorrang:** Wenn eine `.claude/settings.json` im Projekt ein Modell vorschreibt, überschreibt das den User-Default bei jedem Start.
  - **Praktische Bedeutung:** Wer dauerhaft auf Opus wechseln will, muss nicht mehr jede Session mit `--model opus` starten oder die `model`-Zeile in Settings manuell editieren — `/model opus` + Enter genügt.
- **Quelle:** https://code.claude.com/docs/en/model-config (offiziell); https://dev.classmethod.jp/en/articles/20260528-claude-code-updates-v2-1-153/ (extern); offizielles Changelog verbatim: „/model now saves your selection as the default for new sessions (matching the IDE). Press s in the picker to switch models for the current session only." (v2.1.153)
- **Stand:** 2026-05-28

---

## NEU (2.1.154): Zurückhaltendes Nachfrage-Verhalten — Claude fragt weniger

- **Was:** Claude Code stellte bisher Mehrfachauswahl-Fragen auch dann, wenn bereits genug Kontext vorhanden war um selbst zu entscheiden. Ab 2.1.154 reserviert Claude diesen Prompt-Typ für Entscheidungen die es wirklich nicht alleine treffen kann.
- **Best Practice:**
  - **Was das bedeutet:** Claude handelt jetzt autonomer. Bei unvollständigem Kontext wird es dennoch weitermachen — mit einer Annahme, die es nennt. Wenn die Annahme falsch ist, kann man korrigieren.
  - **Praktische Auswirkung auf Prompts:** Weniger Rückfragen bedeutet mehr Flow — aber auch: wenn Claude etwas Wichtiges wissen muss, muss man es in den Prompt schreiben, statt auf eine Frage zu warten.
  - **Was trotzdem gefragt wird:** Destruktive Operationen (Datei löschen, DB löschen, force-push), echte Ambiguität zwischen zwei gleichwertigen Optionen, fehlende externe Credentials.
  - **Empfehlung:** Bei mehrdeutigen Aufgaben direkt im Prompt die relevanten Einschränkungen angeben. `"Falls X dann Y, falls Z dann W"` im Prompt verhindert stilles Raten.
  - **Zusammenspiel mit lean system prompt:** Der schlanke System-Prompt verstärkt dieses Verhalten — Claude erhält weniger „frage lieber nach"-Anweisungen und ist dadurch proaktiver.
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell); https://dev.classmethod.jp/en/articles/20260529-claude-code-updates-v2-1-154/ (extern); offizielles Changelog verbatim: „Claude now reserves the multiple-choice question prompt for decisions it genuinely cannot make itself, instead of asking when it already has enough context to proceed" (v2.1.154)
- **Stand:** 2026-05-30

---

## NEU (2.1.154): Lean System Prompt — weniger Token-Overhead, mehr Kontext für echte Arbeit

- **Was:** Der „lean system prompt" ist ab v2.1.154 der Default für alle Modelle außer Haiku, Sonnet und Opus 4.7 (und älter). Er enthält kompaktere Anweisungen — der volle Standard-Prompt war deutlich länger und fraß Token die besser für Nutzer-Kontext genutzt werden könnten.
- **Best Practice:**
  - **Für wen relevant:** Alle die Opus 4.8 oder neuere Modelle nutzen. Haiku/Sonnet/Opus 4.7 bleiben beim bisherigen Prompt.
  - **Was sich ändert:** Claude verhält sich autonomer und fragt weniger nach (siehe Eintrag oben). Das ist eine direkte Folge des lean prompts — weniger „frage lieber nach"-Instruktionen.
  - **Eigene System-Prompt-Ergänzungen:** Wer mit `--system-prompt` oder `systemPrompt` in Settings eigene Anweisungen hinterlegt hat, muss prüfen ob diese noch konsistent mit dem lean-Default sind. Anthropic stellt System-Prompt-Inhalte transparent über https://github.com/Piebald-AI/claude-code-system-prompts bereit.
  - **Für `bypassPermissions`-Nutzer (Frank's Setup):** Der lean prompt kombiniert mit autonomem Verhalten ist ideal — Claude handelt direkter ohne unnötige Rückfragen.
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell); https://dev.classmethod.jp/en/articles/20260529-claude-code-updates-v2-1-154/ (extern); https://github.com/Piebald-AI/claude-code-system-prompts (offiziell/community-tracked)
- **Stand:** 2026-05-30

---

## NEU (2.1.154): Dynamic Workflows — Orchestrierung von Dutzenden bis Hunderten Agenten

- **Was:** Dynamic Workflows sind ein neues Orchestrierungsparadigma (Research Preview ab 2.1.154). Claude schreibt ein JavaScript-Skript für eine Aufgabe, das Runtime führt es im Hintergrund aus und koordiniert bis zu 1.000 Subagenten — während die Session responsiv bleibt.
- **Wann Workflows vs. Subagenten vs. Skills:**

  | | Subagenten | Skills | Workflows |
  |---|---|---|---|
  | Wer entscheidet was als nächstes läuft | Claude, Turn für Turn | Claude, nach Prompt | Das Skript |
  | Wo Zwischenergebnisse landen | Claudes Kontext | Claudes Kontext | Skript-Variablen |
  | Was wiederholbar ist | Worker-Definition | Anweisungen | Die Orchestrierung selbst |
  | Skala | Wenige Tasks pro Turn | Wie Subagenten | Dutzende bis Hunderte Agenten pro Run |
  | Bei Unterbrechung | Turn neu starten | Turn neu starten | Fortsetzbarer Run |

  **Workflows nutzen wenn:**
  - Aufgabe braucht mehr Agenten als eine Konversation koordinieren kann (große Codebase-Audits, 500-Datei-Migrationen, Cross-checked Research)
  - Die Orchestrierung selbst wiederholt werden soll (Review-Prozess bei jedem Branch)
  - Adversariale Review: unabhängige Agenten prüfen gegenseitig ihre Befunde

  **Workflows NICHT nutzen wenn:**
  - Kleine, klar umgrenzte Aufgaben die ein einzelner Agent-Pass erledigt
  - Vorhersagbares Token-Budget wichtig ist (Workflows können deutlich mehr kosten)
  - Routine-Arbeit — nach dem schweren Task: `/effort high` zurückschalten

- **Best Practice:**
  - **Trigger:** Das Wort `workflow` irgendwo im Prompt schreiben — Claude hebt es hervor und erstellt ein Workflow-Skript statt sequenziell zu arbeiten.
  - **`/deep-research <frage>`:** Eingebauter Workflow — durchsucht Web aus mehreren Winkeln, verifiziert Quellen gegenseitig, liefert einen zitierten Bericht. Beste Einstiegsübung.
  - **`/workflows`:** Monitoring-Ansicht während des Runs — Phasen, Agenten-Anzahl, Token-Summen, Laufzeit. Navigation: Pfeiltasten, `p` pausieren/fortsetzen, `r` Agenten neustarten, `s` als Command speichern.
  - **`/effort ultracode`:** Claude entscheidet selbst bei jeder substantiellen Aufgabe ob ein Workflow nötig ist. Kostet deutlich mehr Tokens — nach dem schweren Task `/effort high` zurückschalten.
  - **Auto Mode empfohlen:** Anthropic empfiehlt explizit Auto Mode beim Einsatz von Workflows — ein Workflow der bei jeder Permission-Abfrage pausiert ist nicht wirklich parallel.
  - **Limits:** Max 16 parallele Agenten gleichzeitig, max 1.000 Agenten pro Run gesamt.
  - **Speichern:** Nach einem erfolgreichen Run: `/workflows` → Run auswählen → `s` → als wiederverwendbaren Command speichern (projekt-weit in `.claude/workflows/` oder persönlich in `~/.claude/workflows/`).

- **Quelle:** https://code.claude.com/docs/en/workflows (offiziell); https://claude.com/blog/introducing-dynamic-workflows-in-claude-code (offiziell); https://agentpedia.codes/blog/claude-opus-4-8-claude-code-workflows (extern)
- **Stand:** 2026-05-30

---

## NEU (2.1.158): Workflow-Keyword-Trigger abschaltbar — `/config`-Einstellung

- **Was:** Das Wort `workflow` im Prompt triggert ab 2.1.154 automatisch die Workflow-Erstellung. Das ist nützlich wenn man einen Workflow starten will — störend wenn man nur über Workflows reden will. Ab 2.1.158 gibt es eine `/config`-Einstellung um diesen automatischen Trigger zu deaktivieren.
- **Best Practice:**
  - **Problem deaktivieren:** In `/config` → „Workflow keyword trigger" ausschalten. Persistiert über Sessions.
  - **Alternativ pro Prompt:** `alt+w` drücken wenn das Wort hervorgehoben wird um es für diesen Prompt zu ignorieren. Oder Backspace direkt nach dem hervorgehobenen Wort drücken.
  - **Für Frank's Setup:** Falls in Prompts oft das Wort „Workflow" vorkommt (z.B. beim Besprechen von CI/CD-Workflows oder App-Workflows), den Trigger in `/config` deaktivieren und Workflows explizit per `/deep-research` oder vollständiger Phrase `"erstelle einen Claude-Workflow für..."` auslösen.
  - **Technisch:** Vollständiges Deaktivieren auch über `"disableWorkflows": true` in `~/.claude/settings.json` oder `CLAUDE_CODE_DISABLE_WORKFLOWS=1` (deaktiviert Workflows komplett, nicht nur den Keyword-Trigger).
- **Quelle:** https://code.claude.com/docs/en/changelog (offiziell); https://releasebot.io/updates/anthropic/claude-code (extern); offizielles Changelog verbatim: „Workflow keyword trigger Setting to stop the word 'workflow' from triggering a workflow added to /config" (v2.1.158)
- **Stand:** 2026-05-30

---

<!-- CHECKPOINT: fertig — alle 4 Changelog-Punkte (2.1.154 multiple-choice, /simplify vs /code-review, dynamic workflows, lean system prompt, 2.1.158 workflow keyword trigger) recherchiert und eingebaut. Nächste relevante Updates ab 2.1.159+ beobachten. -->

---

### Update 2026-06-05 (Claude Code 2.1.165) — Arbeitsweise & Effizienz

**1. Grep ersetzt separates Read vor Edit (2.1.160)**
- **Was:** Single-file `grep`/`egrep`/`fgrep`-Commands erfuellen jetzt den read-before-edit-Check. Ein separates `Read` nach einem Grep ist nicht mehr noetig.
- **Best Practice:** Flow `grep -> Read(Ausschnitt) -> Edit` kuerzen auf `grep -> Edit`. Gilt NUR fuer single-file grep (eine Datei/Aufruf). Spart pro Edit-Zyklus einen Read (~500-3000 Token). Werkzeugwahl bleibt: Grep wenn Name bekannt, semantische Suche wenn nur Konzept.
- **Quelle:** github.com/anthropics/claude-code/releases/tag/v2.1.160 `[offiziell]`

**2. Parallele Bash-Calls: Fehler isoliert (2.1.161)**
- **Was:** Scheitert ein Bash-Command in einem parallelen Tool-Batch, liefern die anderen Calls trotzdem ihr Ergebnis (kein Batch-Abbruch mehr).
- **Best Practice:** Mehrere unabhaengige Bash-Kommandos ruhig in EINEN Antwortblock buendeln — aggressivere Parallelisierung ist jetzt risikoaermer und Standard.
- **Quelle:** github.com/anthropics/claude-code/releases/tag/v2.1.161 `[offiziell]`

**3. `/effort` bestaetigt Persistierung (2.1.162)**
- **Was:** `/effort` zeigt jetzt an, wenn das Level als Default fuer neue Sessions persistiert. Nur mehr Transparenz. Regel bleibt: Effort nie per `CLAUDE_CODE_EFFORT_LEVEL`-Env setzen (blockiert `/effort`).
- **Quelle:** code.claude.com/docs/en/changelog `[offiziell]`

**Betrifft eigene Werkzeuge:** Punkt 1 rechtfertigt eine Anpassung von `search-and-agent-scope.md` und `debugging-and-verification.md` — der bisherige Hinweis "Read(Ausschnitt) nach Grep" ist veraltet; der Grep-Schritt IST jetzt das Read. Der Grep-Reflex selbst bleibt unveraendert.
