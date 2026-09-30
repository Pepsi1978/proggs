---
name: opus5-leitplanken
description: Modellspezifische Leitplanken für Claude Opus 5 liegen in best-practices/claude-tooling/modell-opus5.md — kein Hook blendet sie ein
metadata: 
  node_type: memory
  type: reference
  originSessionId: 191ce696-4343-4556-9dcb-9b7874d37724
  modified: 2026-08-10T20:40:41.264Z
---

Modellspezifisches Wissen zu Claude Opus 5: `~/proggs/best-practices/claude-tooling/modell-opus5.md`
(Volltext) und `modell-opus5-kurzcheck.md` (Stufe A). Recherchiert am 10.08.2026.

**Warum das hier steht:** Der Bereich hat bewusst **keine** Gegenseite unter `bugs/`, und der
`bug-almanac-hint`-Hook blendet nur Bereiche ein, für die eine Datei unter `bugs/` existiert
(Existenzprüfung in `bug-almanac-hint.py`). Das Wissen wird also von keinem Hook automatisch
angeboten — dieser Eintrag ist der einzige Auffindbarkeits-Pfad.

**Wann lesen:** vor Arbeit an Regelwerken, Skills, Prompts oder Agent-Schwärmen; bei
unerwartet langen Ausgaben, ausgeweitetem Aufgaben-Umfang oder zu vielen Subagenten.

Kernsatz: Opus 5 macht zu viel, nicht zu wenig — alte Regeln aus der 4.x-Zeit waren gegen
Nachlässigkeit gebaut, gebraucht wird Begrenzung. Verwandt: [[claude-profile-architektur-launcher]].
