# Gemini CLI — Foundational Mandates & Project Intelligence

Diese Datei ist das zentrale Mandat für die Gemini CLI in diesem Projekt. Sie basiert auf dem Universal Mirror Ledger (BASELINE-2026-03-25) und synchronisiert die Intelligenz der gesamten Programmierumgebung.

## 1. Philosophie und Direktiven

Das Ziel: **Die intelligenteste Programmierumgebung der Welt werden.**

Drei Haupt-Direktiven (Priorität absteigend):
1. **Superintelligenz** (siehe `Gemini-Setup/rules/superintelligence.md`) — Exponentielle Intelligenzsteigerung.
2. **Selbstbeobachtung** (siehe `Gemini-Setup/rules/self-observation.md`) — Jede Aktion beobachten, Fehler analysieren, Intelligenz-Vorschläge generieren.
3. **Resilient Bugfixing** (siehe `Gemini-Setup/rules/resilient-bugfixing.md`) — Jeder Bug genau einmal. 5-Schritte-Fix.

## 2. Sprache & Interaktion

- **Sprache:** Alle Interaktionen, Erklärungen und Intelligenz-Vorschläge erfolgen auf **Deutsch**.
- **Code & Commits:** Quellcode, Kommentare und Commit-Messages erfolgen auf **Englisch**.
- **Agenten & Skills:** Alle selbst erstellten Agenten, Skills und Commands müssen **komplett auf Deutsch** verfasst sein (Description, Examples, System-Prompt).
- **Whisper-Trigger:** Deutsche Sprachbefehle (via Whisper) werden gemäß `Gemini-Setup/rules/german-skill-triggers.md` zugeordnet.

## 3. Agenten-System (18 Spezialisten)

Die Agenten befinden sich in `Gemini-Setup/agents/`. Gemini CLI nutzt diese Beschreibungen als Wissensbasis für seine Fähigkeiten.

| Agent | Zweck |
|-------|-------|
| `architect` | Systemarchitektur vor der Implementierung. |
| `challenger` | Hinterfragt Pläne und Architektur (Devil's Advocate). |
| `coder` | Schnelle Implementierung von Teilaufgaben. |
| `debugger` | Systematisches Debugging mit Hypothesen. |
| `export` | Mirror Bridge: Schreibt Session-Änderungen ins Ledger. |
| `import` | Mirror Bridge: Liest Ledger und portiert Änderungen. |
| `nemo` | NVIDIA Nemotron Worker für Massen-Aufgaben. |
| `quality-gate` | Kombiniert Review und Tests für Freigaben. |
| ... | (Alle weiteren Agenten siehe BASELINE-2026-03-25) |

## 4. Mirror Bridge (Universal Sync)

Dieses System synchronisiert Änderungen zwischen macOS/Windows und verschiedenen CLIs.

- **Zentrales Ledger:** `claude-code-setup/mirror-ledger.md`
- **Export-Workflow:** Am Ende jeder Session Änderungen scannen und als Eintrag anhängen.
- **Import-Workflow:** Beim Start prüfen, ob PENDING-Einträge für `gemini` existieren.
- **Trigger:**
  - "exportiere" / "Aenderungen spiegeln" -> `export` Agent
  - "importiere" / "was ist neu" -> `import` Agent

## 5. Regeln & Konventionen

Alle Regeln aus `Gemini-Setup/rules/` sind bindend.
Besonders wichtig für Gemini:
- `german-agents-skills.md`: Pflicht für deutsche Beschreibungen.
- `german-skill-triggers.md`: Zuordnung von Whisper-Phrasen.
- `semantic-search-isolation.md`: Code-Search Indizes niemals plattformübergreifend ändern.
- `hook-constraints.md`: (Neu) Erkenntnisse zu Hook-Einschränkungen und Debug-Methoden.

### 5.1 Wissens-Update (März 2026)

- **OpenClaw Integration:** OpenClaw v2026.3.24 ist nativ auf macOS eingerichtet (Modell: `google/gemini-3-flash-preview`). Gateway auf Port 18789.
- **Hook-Einschränkungen:** Prompt-Hooks (`type: "prompt"`) dürfen niemals bei `SessionStart` oder `SessionEnd` verwendet werden (fehlender ToolUseContext). Stattdessen immer Command-Hooks verwenden.
- **Debug-Methode:** Bei Startup-Fehlern ("startup hook error") sofort `claude -p --debug-file $env:TEMP\hook-debug.txt --system-prompt "Reply with just OK"` nutzen. Einzeltests von Hooks sind unzuverlässig.
- **Mirror-Check Robustheit:** Der `mirror-check` Hook wurde stabilisiert (grep/Select-String null-safe gemacht).

## 6. Benutzer-Profil

- **Sprache:** Deutsch (Whisper), Englisch (Code/Commits).
- **Parallelisierung:** Nutze Sub-Agenten (`generalist`, `codebase_investigator`) parallel für komplexe Aufgaben.
- **Sichtbarkeit:** Immer transparent arbeiten, keine versteckten Hintergrundprozesse ohne Info.
- **Repo:** Einzige Wahrheitsquelle ist `Pepsi1978/proggs`.
- **Commit-Format:** `#NNN - Beschreibung` (Englisch).

---
*Zuletzt aktualisiert: 2026-03-26 (via import Agent von MIRROR-2026-03-25-MAC-018)*
