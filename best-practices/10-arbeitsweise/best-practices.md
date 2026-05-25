# Arbeitsweise / Verhalten — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

> Quelle: Offizielle Anthropic-Dokumentation (code.claude.com/docs/en/best-practices)  
> Recherche-Datum: 2026-05-25

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
