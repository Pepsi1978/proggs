# Settings & Konfig Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | settings.json schreiben | JSON validieren + BOM-frei — ein Fehler/BOM killt ALLE Settings still | Konfigurationshierarchie |
| 2 | Permissions | Sperren nur via `deny` (`allow` ist KEINE Whitelist) | Permission-Modes |
| 3 | `effortLevel` | ueber das `effortLevel`-Setting, NIE `CLAUDE_CODE_EFFORT_LEVEL`-Env | effortLevel |
| 4 | Praezedenz | Managed > CLI > local > project > user; Permission-Regeln MERGEN | Konfigurationshierarchie |
| 5 | `model`/`outputStyle` | nicht live-reloaded — Neustart noetig | /model |
| 6 | user-level rules `paths:` | nie user-level (ignoriert) — nur projektweit | .claude/rules paths: |
| 7 | env-Vars | `NO_COLOR`/`FORCE_COLOR` wirken nur fuer Subprozesse (ab v2.1.143) | Umgebungsvariablen |
