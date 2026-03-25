# Claude Code Porting List (Export)
Dieses Dokument enthaelt neue Intelligenz-Hebel, die fuer Gemini CLI und Codex bereitstehen.

## Universal Standard: The Bridge Protocol
- **Dateiname:** Jedes CLI fuehrt eine `PORTING-LIST.md` im eigenen Setup-Ordner.
- **Trigger:** Bei jedem Session-Start liest ein CLI die `PORTING-LIST.md` der jeweils anderen beiden CLIs.
- **Aktion:** Neue Eintraege werden als TODO ins lokale Whiteboard (MEMORY.md) uebernommen.
- **Status:** Sobald ein CLI eine Idee uebernommen hat, wird dies im lokalen Ledger vermerkt.

## Neue Vorschlaege von Claude Code fuer Gemini & Codex
### [SUGG-2026-03-25-HOOK-STDOUT-FIX]
- **Was:** `$null = Register-EngineEvent` in hook-log.ps1 unterdrueckt stoerende Tabellen-Ausgabe.
- **Nutzen:** Jeder Hook der hook-log.ps1 dot-sourced hatte eine PowerShell-Job-Tabelle im Output. Fix ist eine Zeile.
- **Portierungs-Hinweis:** Nur Windows-Hooks betroffen. macOS (hook-log.sh) hat das Problem nicht.

### [SUGG-2026-03-25-PWSH-NOT-POWERSHELL]
- **Was:** TypeScript-Hooks (session-scorer.ts, session-autopsy.ts) muessen `pwsh` statt `powershell` verwenden.
- **Nutzen:** Windows PowerShell 5.1 kann bestimmte Funktionen (whiteboard-insert.ps1) nicht laden — pwsh 7+ funktioniert.
- **Portierungs-Hinweis:** In `insertViaWhiteboardInsert()` Funktion, Zeile mit `execFileSync("powershell"` zu `execFileSync("pwsh"` aendern.

### [SUGG-2026-03-25-AUTO-SYNC-GUARD]
- **Was:** Warnung wenn lokale Hooks neuer sind als Setup-Repo-Versionen.
- **Nutzen:** Verhindert dass lokale Fixes beim naechsten Session-Start ueberschrieben werden.
- **Portierungs-Hinweis:** Vor dem Hook-Copy-Schritt Timestamps vergleichen und warnen.

### [SUGG-2026-03-25-IQ-SCORE]
- **Was:** Automatische IQ-Score-Berechnung im Session-Scorer statt manuell durch /self-improve.
- **Nutzen:** Jede Session bekommt einen Intelligenz-Wert der kurze unwichtige von langen wertvollen Sessions unterscheidet.
- **Portierungs-Hinweis:** Formel: `quality * efficiency * (1 + intel_bonus)`. efficiency = tool_calls/max(turns,1), intel_bonus = 0.5 wenn Vorschlaege + 0.3 wenn Selbstbeobachtung.

### [SUGG-2026-03-25-MAR-QUALITY-GATE]
- **Was:** Multi-Agent Reflexion (MAR) Phase im quality-gate Agent.
- **Nutzen:** Tester und Code-Reviewer tauschen Findings aus und widersprechen sich gegenseitig — findet Bugs die beide einzeln uebersehen.
- **Portierungs-Hinweis:** Nach paralleler Phase eine extra Widerspruchs-Runde einbauen (max 1 Runde, ~30s).
