# Slash-Commands Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Schnell-Orientierung der Harness-Best-Practices;
> der Volltext darunter ist die Tiefe. Vor Arbeit am jeweiligen Werkzeug lesen.

| # | Thema / Situation | Kernregel (Kurzform) | Abschnitt |
|---|-------------------|----------------------|-----------|
| 1 | Commands = Skills | vereint seit 2.1.3; ein Skill schlaegt gleichnamigen Command | Unified Model |
| 2 | `$ARGUMENTS` | bricht bei mehrzeiligem Input; NIE ungefiltert in `` !`bash` `` (Injection) | Argument-Handling |
| 3 | automatische Ausloesung | ueber die `description` (Trigger front-loaden) | description-Feld |
| 4 | Plugin-Command aufrufen | voll-qualifiziert `/plugin:command` | Namespacing |
| 5 | Tool-Vorfreigabe | `allowed-tools` (Genehmigungs-Bypass); `disallowed-tools` zum Sperren | allowed-tools |
| 6 | viele Skills/Commands | `/doctor` (Beschreibungs-Budget) | Beschreibungs-Budget |
