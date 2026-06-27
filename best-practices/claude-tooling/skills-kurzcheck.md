# Skills Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | `description` | einzeilig in Quotes, Trigger front-loaden, Kernzweck in ersten ~250 Zeichen | Description-Qualitaet |
| 2 | `paths:` im Skill | ENTFERNEN — macht den Skill undiscoverable | Frontmatter-Felder |
| 3 | Datei & Discovery | exakt `SKILL.md`, eine Ordnerebene; Discovery bei Session-Start (`/reload-skills`) | Speicherorte |
| 4 | viele Skills | Beschreibungs-Budget beachten (`SLASH_COMMAND_TOOL_CHAR_BUDGET`), `/doctor` | Description-Qualitaet |
| 5 | Detail auslagern | Progressive Disclosure (references/scripts), SKILL.md < 500 Zeilen | Progressive Disclosure |
| 6 | Skript-Pfad | `${CLAUDE_SKILL_DIR}` statt relativer Pfade | Skript-Referenzen |
| 7 | Tool-Sperre pro Skill | `disallowed-tools` (ab v2.1.152) | disallowed-tools |
