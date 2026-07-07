# AGENTS.md — Die Verfassung von Harness Forge

> Diese Datei ist die hoechste Autoritaet fuer alle Komponenten von Harness Forge
> — sowohl fuer das Programm selbst als auch fuer jeden Harness, den es erzeugt.
> Sie ist auf Deutsch verfasst, weil die Zielgruppe deutschsprachige Entwickler
> und ambitionierte Nicht-Programmierer sind.

---

## 1. Identitaet & Mission

**Harness Forge** ist ein Meta-Harness-Builder. Du beschreibst in normaler
Alltagssprache eine Aufgabe, fuer die du einen KI-Begleiter haben moechtest.
Harness Forge entscheidet dann selbst — auf Basis nachvollziehbarer Kriterien —
welche Form der Begleiter haben soll, und baut ihn vollstaendig.

Das Ergebnis ist kein Stueck Beispielcode, sondern ein echtes, lauffaehiges
Werkzeug mit eingebauter Selbstbeobachtung, Fehlerabsicherung und Lernfaehigkeit.

### Die fuenf moeglichen Harness-Formen

| Form | Wann sinnvoll |
|------|--------------|
| **Android-App** (Kotlin + Jetpack Compose) | Aufgabe braucht Handy-Sensoren, Offline-Fall, unterwegs-nutzbar |
| **Desktop-App** (Tauri v2: Rust-Kern + Svelte-UI) | Aufgabe laeuft am Rechner, braucht Dateisystem, lokale Modelle |
| **Python-CLI** (Typer + Rich) | Aufgabe ist Terminal-orientiert, Entwickler-Zielgruppe |
| **Reiner System-Prompt** (Markdown-Artefakt) | Aufgabe ist rein textlich, keine Tool-Aufrufe noetig |
| **Claude-Subagent + Skill-Bundle** | Aufgabe soll innerhalb von Claude Code laufen, als Spezialist |

---

## 2. Die sechs Leitprinzipien (nicht verhandelbar)

Diese Prinzipien gelten fuer Harness Forge selbst UND fuer jeden erzeugten Harness.
Sie stammen aus der Harness-Forschung 2025/2026 und sind empirisch validiert.

### 2.1 Build to Delete

Jeder Baustein muss sich in unter einer Stunde entfernen lassen, ohne dass
andere Bausteine brechen. Das bedeutet konkret:

- Dependency Injection statt harte Kopplung
- Protokolle (Swift) statt konkrete Typen in Signaturen
- Keine zirkulaeren Modul-Abhaengigkeiten
- Jedes Backend, jeder Builder, jede Schicht ist ein austauschbares Stueck

### 2.2 Thin Harness, Smart Model

Das KI-Modell ist der Planer. Der Harness liefert atomare Werkzeuge, keine
Kontrollfluss-Logik. Wenn im Code steht "if task_type == X then do Y else Z",
ist das fast immer ein Zeichen dass die Verantwortung beim Modell liegen sollte.

Konkret: Unsere Builder bieten dem Modell kleine Tools wie `generate_file`,
`write_prompt`, `add_dependency` — nicht `build_complete_android_app_step_by_step`.

### 2.3 Silent on Success, Loud on Failure

Verifikations-Hooks und Pre-Flight-Checks duerfen bei Erfolg NICHTS in den
Kontext schreiben. Nur Fehler tauchen auf, und zwar mit Exit-Code ≠ 0 und
einer knappen, handlungsorientierten Fehlermeldung.

Warum: Jedes unnoetige Token im Kontext kostet Geld, verwaessert die Aufmerksamkeit
des Modells und erhoeht die Drift-Wahrscheinlichkeit.

### 2.4 The Harness is the Dataset

Jede Interaktion (Task-Input, Tool-Calls, Outputs, Fehler, Nutzer-Feedback) wird
lokal in einer SQLite-Datenbank persistiert (via SwiftData). Diese Trajektorien
sind spaeter:

- Input fuer den Reflection-Loop ("wie haette ich das besser gemacht?")
- Quelle fuer Skill-Extraktion (welche Tool-Sequenzen wiederholen sich?)
- Potenzielles Fine-Tuning-Dataset (Export im MLX-Format)

### 2.5 Never Make That Mistake Again

Jeder erkannte Fehler wird automatisch zu einer Verifikations-Regel. Diese
Regeln landen in `Rules/learned/*.md` des jeweiligen Harness-Projekts. Beim
naechsten Mal, wenn die gleiche Fehlerklasse droht, schlaegt der Guard bereits
vor der Ausfuehrung an.

### 2.6 Model-Agnostisch

Es gibt keine harte Kopplung an Anthropic, OpenAI oder ein anderes Provider-SDK.
Alle Backends sprechen HTTP-APIs via `URLSession` und implementieren das
gleiche Swift-Protokoll `LLMClient`.

---

## 3. Die 5+1 Schichten (Pflicht-Bestandteile jedes Harnesses)

Jeder Harness, egal welcher Form, bekommt diese Schichten eingebaut.

```
┌─────────────────────────────────────────────────────────┐
│  META (Hyperagent, Skill-Extraktion, Drift-Monitoring)  │
├─────────────────────────────────────────────────────────┤
│  L5 — LIFECYCLE                                         │
│       Loop-Detection, Cost-Tracking, HITL-Gates         │
├─────────────────────────────────────────────────────────┤
│  L4 — VERIFICATION                                      │
│       Silent-Success-Hooks, LLM-as-Judge                │
├─────────────────────────────────────────────────────────┤
│  L3 — EXECUTION                                         │
│       Tool-Registry, MCP-Client, Sandbox                │
├─────────────────────────────────────────────────────────┤
│  L2 — CONTEXT                                           │
│       AGENTS.md-Loader, Progressive Disclosure, RAG     │
├─────────────────────────────────────────────────────────┤
│  L1 — CONSTRAINT                                        │
│       Linter-Regeln, Schema-Validierung                 │
└─────────────────────────────────────────────────────────┘
```

**Merkregel**: L1 ist wie die Statik eines Hauses (kann nicht weg), L5 ist wie
die Hausverwaltung (laeuft drumherum), META ist wie ein Bauinspektor, der
regelmaessig vorbeikommt.

---

## 4. Rollenverteilung

| Rolle | Verantwortlich fuer |
|-------|--------------------|
| `TaskAnalyzer` | Aufgabe zerlegen, Harness-Form empfehlen |
| `Router` | Richtiges KI-Backend fuer die konkrete Anfrage waehlen |
| `LLMClient` | Einheitliche Schnittstelle zum gewaehlten Backend |
| `Builder` | Konkretes Harness-Projekt erzeugen (5 Varianten) |
| `PromptVersioning` | Jede Prompt-Aenderung als Git-Commit |
| `TrajectoryStore` | Alle Interaktionen persistieren |
| `ReflectionLoop` | Periodisch: Was lief gut, was schlecht, was lernen wir? |
| `LessonsDB` | Gelernte Fehlerregeln vorhalten und durchsetzen |

Jede Rolle ist als eigenes Swift-Modul oder eigener Typ implementiert und
separat testbar.

---

## 5. Verifikations-Regeln fuer erzeugte Harnesse (Pflicht-Checkliste)

Bevor ein erzeugter Harness als "fertig" markiert wird, muss er diese
Checkliste bestanden haben. Jeder Punkt ist automatisiert pruefbar.

- [ ] `AGENTS.md` vorhanden, auf Deutsch, Rollen definiert
- [ ] `LLMClient`-Abstraktion mit mindestens 2 austauschbaren Backends
- [ ] Plugin-Verzeichnis `Skills/` mit `registry.json`
- [ ] Prompt-Editor-Ansicht (UI-abhaengig: SwiftUI-View, Compose-Screen, CLI-Route)
- [ ] Trajektorien-Logger (SQLite/SwiftData/Room, je nach Ziel)
- [ ] Reflection-Loop konfiguriert (Default: alle 10 Interaktionen)
- [ ] Loop-Detection und Token-Budget-Cap
- [ ] Mindestens ein Silent-Success-Verifizierungs-Hook
- [ ] README auf Deutsch, 5-Minuten-Quickstart
- [ ] Lizenz (MIT, sofern nicht anders gewuenscht)

---

## 6. Kommunikations-Verfassung

- **Sprache zwischen Werkzeug und Nutzer**: Deutsch. User-facing Strings sind
  zusaetzlich in Englisch (`en-US`) vorhanden, Default ist `de-DE`.
- **Sprache im Code**: Typen und Variablen Englisch (Konvention der Standard-
  Bibliothek). Kommentare und Docstrings **Deutsch**.
- **Sprache in Commits**: Englisch, mit fortlaufender Nummer `#NNN - <Text>`.
- **Transparenz**: Jede Entscheidung des TaskAnalyzers wird als "Decision Record"
  in `Decisions/<slug>/decision.md` abgelegt.
- **Live-Stream**: Waehrend einer Generierung sieht der Nutzer den Fortschritt
  in Echtzeit — nichts laeuft unsichtbar im Hintergrund.

---

## 7. Rules/learned/ — der Ort, an dem das System schlauer wird

Dieser Ordner wird automatisch gepflegt. Ein Beispiel-Eintrag:

```markdown
## Regel 017 — Gemini reagiert mit 400 bei Bildern > 20 MB

**Entdeckt am**: 2026-04-23
**Kontext**: Builder "Reiseplaner", Gemini 2.5 Pro
**Symptom**: HTTP 400 "request size exceeds limit"
**Wurzel**: Gemini-API limitiert Multipart-Requests auf 20 MB
**Abhilfe**: Bilder vor Upload auf max. 15 MB skalieren (JPEG q=85)
**Guard**: `GeminiBackend.validateImageSize(_:)` wirft jetzt vor dem Call
```

Diese Regeln werden beim naechsten Start automatisch in die Verifikations-
Schicht geladen.

---

## 8. Was Harness Forge NICHT tut

- **Keine neuen GitHub-Repos anlegen.** Alles geht in `Pepsi1978/proggs` als
  Unterordner mit sprechendem Namen.
- **Keine Python-GUIs erzeugen.** Python ist fuer CLI-Builds OK, aber nicht
  fuer visuelle Oberflaechen.
- **Keine nackten API-Keys irgendwo ablegen.** Alles ueber macOS Keychain.
- **Keine stillen Erfolgsmeldungen "das habe ich nebenher repariert".** Wenn
  etwas korrigiert wird, erfaehrt der Nutzer es.
- **Keine Magie.** Jede Entscheidung ist nachvollziehbar und begruendet.
