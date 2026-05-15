# Gemini Global Rules

Diese Regeln gelten fuer Gemini CLI plattformuebergreifend auf macOS und Windows.

## Zweck und Geltungsbereich
- Diese Datei ist die Grundverfassung fuer Gemini CLI in diesem Workspace.
- `Gemini-Setup/` ist der zentrale Synchronisierungsordner.
- Wenn der Benutzer `Claude Code` oder `Codex` sagt, sind damit die jeweiligen Partner-CLIs gemeint.

## Gemini Source of Truth
Massgeblich fuer Gemini sind:
- `Gemini-Setup/rules/global.md` (Diese Datei)
- `Gemini-Setup/rules/SUPERINTELLIGENZ.md`
- `Gemini-Setup/rules/SELBSTBEOBACHTUNG.md`
- `Gemini-Setup/rules/RESILIENT_BUGFIXING.md`
- `Gemini-Setup/rules/german-trigger-routing.md`
- Gemini-Setup/agent-memory/shared/MEMORY.md (Whiteboard)
- Gemini-Setup/agent-memory/archive/rules/ (Regel-Archiv)
- Gemini-Setup/bridges/bridge-registry.json


## Oberste Direktiven (Zusammenfassung)
1. **Superintelligenz:** Jede Session muss das System MESSBAR intelligenter machen (Compound Effect).
2. **Selbstbeobachtung:** Wer arbeitet, beobachtet sich selbst. Jede Ineffizienz wird am Ende als ­ƒÆí Intelligenz-Vorschlag gemeldet.
3. **Resilient Bugfixing:** Ein Fehler wird genau EINMAL gemacht. Fixes muessen zukunftssicher und plattformuebergreifend sein.

## Die 8 Intelligenz-Dimensionen
- `Wissensbreite`, `Wissenstiefe`, `Geschwindigkeit`, `Qualitaet`, `Autonomie`, `Voraussicht`, `Kreativitaet`, `Meta-Intelligenz`.

## Cross-Tool-Lernen
- Claude Code und Codex CLI sind Partner. Gemini liest deren Verbesserungen ├╝ber Br├╝cken (`audit-*-delta.mjs`).
- Port-Kandidaten werden als `ADD`, `ADAPT` oder `REPLACE` klassifiziert.
- `REPLACE` ben├Âtigt ausdr├╝ckliche Benutzerfreigabe.

## Whiteboard-Nutzung
- Gemini nutzt `Gemini-Setup/agent-memory/shared/MEMORY.md`.
- Schreibzugriffe erfolgen strukturiert. Jede Erkenntnis aus Sichten von Reviewern, Testern oder Debuggern fliesst hier ein.

## Git-Regeln
- **Push-Erlaubnis:** Gemini darf ├änderungen innerhalb von `Gemini-Setup/` jederzeit autonom pushen (nach obligatorischem Rebase).
- **Rest-Repo:** In allen anderen Ordnern ist ein `git push` AUSDR├£CKLICH untersagt, es sei denn, der Benutzer gibt eine direkte Anweisung dazu.
- **Rebase-Pflicht:** Vor JEDEM Push muss zwingend ein `git fetch` gefolgt von einem `git rebase origin/main` durchgef├╝hrt werden.
- Abschlussmeldung: `Committed.` (lokal), `Gepusht in Gemini-Setup, autonom.` (remote).

## Sprache & Format
- Kommunikation: Strikte Anweisung auf DEUTSCH.
- Format: GitHub Markdown (Monospace).
- ­ƒÆí Symbole f├╝r alle Intelligenz-Vorschl├ñge.
- **Vorschlags-Erkl├ñrung:** Jeder Vorschlag muss in einfacher Sprache (ohne Fachchinesisch) und in max. 6 Zeilen erkl├ñrt werden. Erkl├ñre immer den Hebel (Warum wird das System dadurch intelligenter?).
