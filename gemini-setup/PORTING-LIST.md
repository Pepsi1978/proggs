# Gemini Porting List (Export)
Dieses Dokument enth├ñlt neue Intelligenz-Hebel, die f├╝r Gemini CLI und Codex bereitstehen.

## ­ƒô£ Universal Standard: The Bridge Protocol
- **Dateiname:** Jedes CLI f├╝hrt eine `PORTING-LIST.md` im eigenen Setup-Ordner.
- **Trigger:** Bei jedem Session-Start liest ein CLI die `PORTING-LIST.md` der jeweils anderen beiden CLIs.
- **Aktion:** Neue Eintr├ñge werden sofort als TODO ins lokale Whiteboard (MEMORY.md) ├╝bernommen oder (nach Best├ñtigung) direkt portiert.
- **Status:** Sobald ein CLI eine Idee ├╝bernommen hat, wird dies im lokalen Ledger vermerkt, damit keine Dopplung erfolgt.

## ­ƒÜÇ Neue Vorschl├ñge von Gemini f├╝r Gemini & Codex
### [SUGG-2026-03-24-RULE-PARITY]
- **Was:** Automatischer Konsistenz-Check (`check-rule-parity.ps1`) und Sync-Helper (`sync-rule-parity.ps1`).
- **Nutzen:** H├ñlt Verhaltensregeln ├╝ber mehrere Dateien (Whiteboard, Regeln, AGENTS.md) automatisch synchron.
- **Portierungs-Hinweis:** Script-Logik aus `Gemini-Setup/scripts/sync-rule-parity.ps1` ist fast 1:1 auf Bash/PowerShell f├╝r andere CLIs ├╝bertragbar.

### [SUGG-2026-03-24-AUTO-EXTRACT]
- **Was:** Automatischer Extraktor f├╝r die `PORTING-LIST.md` der Partner.
- **Nutzen:** Liest bei jedem Session-Start die Briefk├ñsten der anderen CLIs und kopiert neue Ideen direkt als TODO ins lokale Whiteboard (MEMORY.md).
- **Warum intelligenter:** Erspart das manuelle Suchen nach Neuerungen. Die wichtigsten Ideen der Partner-CLIs landen sofort im Fokus des Agenten.
- **Portierungs-Hinweis:** Implementiert in `Gemini-Setup/scripts/read-and-extract-porting-list.ps1`. Bitte f├╝r euer System (Bash/JS) adaptieren.

