# Agent-Verhalten: Commit/Push-Disziplin gegen eingebaute Vorbehalte Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | ⭐ Agent committet/pusht nicht, obwohl Regel es verlangt | Viele CLI-Tools (auch OpenCode) haben einen **hardcodierten** System-Prompt-Vorbehalt ("nur auf explizite Anweisung"). Eine reine Erwähnung der Pflicht in AGENTS.md reicht NICHT — die Regel muss den eingebauten Vorbehalt aktiv als überschrieben markieren | §1 |
| 2 | Wie den eingebauten Vorbehalt entkräften | Formulierungsmuster: "Diese Regel HIER ist die explizite Anweisung, für JEDE Aufgabe, ohne Wiederholung" — nicht nur "bitte committen", sondern das Autorisierungs-Wort selbst liefern | §2 |
| 3 | Zuverlässigster Mechanismus (Goldstandard) | Code-Ebene statt Prompt-Ebene: Commit als deterministische Nebenwirkung eines Event-Handlers (Aider: nach jedem Edit; Cursor+GitButler: bei Task-Ende), NIE als "Entscheidung" des LLM | §3 |
| 4 | Bei Multi-Session-Setups | `git add -A` in Auto-Commit-Snippets NICHT übernehmen — reisst fremde Dateien anderer Sessions mit. Nur eigene/getrackte Dateien committen | §3, §4 |
| 5 | OpenCode konkret | Plugin bei `session.idle` (Alert-Ebene, sofort umsetzbar) oder `tool.execute.after`-Datei-Tracking + gezielter Commit (Ausbaustufe, datei-genau) | §4 |
