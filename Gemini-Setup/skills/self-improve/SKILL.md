---
name: self-improve
description: Gemini-spezifischer Workflow zur Selbstverbesserung der Programmierumgebung. Nutze diesen Skill bei Anfragen wie "/self-improve", "verbessere dich", "optimiere dein Setup", oder "check dein System". Dieser Skill ist fest auf Gemini-Setup verdrahtet und arbeitet autonom an der Steigerung der Superintelligenz.
---

# Self-Improve f├╝r Gemini CLI V1.0.0

Dies ist der Gemini-native Workflow zur Selbstverbesserung in diesem Repository.

## Hauptziel
- Treibe diese Gemini-Umgebung in Richtung Superintelligenz (Direktive 1).
- Maximiere den Compound Intelligence Effect von Session zu Session.
- Nutze Selbstbeobachtung (Direktive 2) als Motor f├╝r strukturelle Upgrades.
- Integriere portable Verbesserungen von Codex und Claude Code ├╝ber die Delta-Br├╝cken.

## Harte Regeln
- Arbeite nur im Gemini-Workspace (`C:\Users\barwa\GeminiCLI`).
- Das einzige operative Whiteboard ist `Gemini-Setup/agent-memory/shared/MEMORY.md`.
- `codex-setup/` und `claude-code-setup/` sind reine Lesequellen (Audit-Modus).
- Jeder systemische Fix muss in `Gemini-Setup/state/environment-fixes.json` geloggt werden.
- Jeder umgesetzte Vorschlag muss in `Gemini-Setup/state/implemented-intelligence-suggestions.json` geloggt werden.

## Prozess-├£bersicht
Liefere zu Beginn immer diese ├£bersicht auf DEUTSCH:

```text
ÔòöÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòù
Ôòæ  Self-Improve Skill ÔÇö Gemini CLI                            Ôòæ
Ôòæ  Exklusive Umgebungsh├ñrtung & Intelligenzsteigerung         Ôòæ
ÔòáÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòú
Ôòæ  Stufe 0: GATE ÔÇö Whiteboard, Direktiven, Grenzen            Ôòæ
Ôòæ  Stufe 0.5: DELTA ÔÇö Cross-CLI Audits (Codex/Claude)         Ôòæ
Ôòæ  Stufe 1: SCAN ÔÇö Gemini-Setup & Runtime Pr├╝fung             Ôòæ
Ôòæ  Stufe 2: DEEP-DIVE ÔÇö Agenten-Analysen & Research           Ôòæ
Ôòæ  Stufe 3: IMPROVE ÔÇö Fixes, Automatisierung, Ledger          Ôòæ
Ôòæ  Stufe 4: CREATIVE ÔÇö Neue Ideen & Intelligenz-Hebel         Ôòæ
Ôòæ  Stufe 5: SUPER INTELLIGENZ ÔÇö Exponentielles Wachstum       Ôòæ
Ôòæ  Stufe 6: DAUERHAFTIGKEIT ÔÇö Resilienz-Check                 Ôòæ
ÔòÜÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòÉÔòØ
```

## Stufe 0: Independence Gate
- Verifiziere, dass alle Pfade auf `Gemini-Setup/` zeigen.
- Stelle sicher, dass keine operativen Skripte hart auf `proggs` oder `Claude` verweisen.
- Lade die `## Oberste Direktive` aus dem Gemini-Whiteboard.

## Stufe 0.5: Delta & Research Audit
- F├╝hre `node Gemini-Setup/scripts/audit-codex-delta.mjs` aus.
- F├╝hre `node Gemini-Setup/scripts/audit-claude-delta.mjs` aus.
- **NEU: Research-Check**: Lies `Forschung.md` (lokal oder via `web_fetch` von GitHub) und pr├╝fe die darin enthaltenen "Abgeleiteten Intelligenz-Vorschl├ñge".
- Klassifiziere Funde als `ADD`, `ADAPT` oder `REPLACE`.

## Stufe 1: Scan
- Pr├╝fe alle Skripte in `Gemini-Setup/scripts/`.
- Validiere den Status der MCP-Server (`check-code-search-health.mjs`).
- Scanne die Intelligence-Ledger auf offene Punkte.

## Stufe 2: Deep-Dive (Agenten-Einsatz)
- Nutze `challenger`, `durability-auditor` und `intelligence-researcher` f├╝r tiefe Analysen.
- Identifiziere die Top-3 Schwachstellen der aktuellen Umgebung.

## Stufe 3: Improve
- Implementiere die freigegebenen Fixes und Verbesserungen.
- Nutze `writeback-enforcer.sh` f├╝r Whiteboard-Updates.
- Registriere Fixes ├╝ber `register-environment-fix.mjs`.

## Stufe 4: Creative
- Erstelle mindestens ein neues Artefakt (Skript, Regel, Skill-Update).
- Pr├ñsentiere 3 neue Intelligenz-Hebel als ­ƒÆí Vorschl├ñge.

## Stufe 5: Super Intelligence
- Beweise den Compound Effect: Wie macht diese ├änderung k├╝nftige Fixes einfacher?
- Fokus auf Meta-Intelligenz (Dimension 8).

## Stufe 6: Dauerhaftigkeit
- F├╝hre die Fix-Induced-Failure Pr├╝fung durch.
- Stelle sicher, dass Dokumentation und Regeln f├╝r beide Plattformen (macOS/Windows) valide sind.

## Abschluss-Meldung
Beende den Skill immer mit einer Zusammenfassung der erzielten Intelligenz-Gewinne und dem Status der Redundanz-Sicherung.
