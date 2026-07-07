# Agenten-Wissens-/Best-Practices-/Lern-System (Harness-Selbstverbesserung) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Hook injiziert Kontext (SessionStart/SubagentStart/Pre/PostToolUse) | Nested `hookSpecificOutput.{hookEventName,additionalContext}` + PS `-Depth 5`; flaches Schema wird STILL ignoriert | §1 |
| 2 | Hook in `~/.claude/hooks/` geaendert | Sofort in Repo-Spiegelung kopieren UND umgekehrt; Drift bleibt sonst MONATE unbemerkt (aktiv≠repo) | §2 |
| 3 | Lern-DB (experience/trajectories/scores) befuellen | NIE Platzhalter (`success_score:3`, leere `tool_sequence`) schreiben — lieber leer als irrefuehrend; sonst sind alle Metriken Papier | §3 |
| 4 | Wartungs-/Self-Test-Skript bauen | Muss vertrauenswuerdig sein: ein Tool mit Dauer-Fehlalarmen (Format-Drift) wird ignoriert und schuetzt nicht mehr | §4 |
| 5 | Neuer Almanach/Wissens-Bereich | Datei-Trigger allein verdeckt Spezial-Almanache hinter dem Sprach-Almanach + faengt Konzept-Arbeit nicht; zweite (semantische) Trigger-Schicht noetig | §5 |
| 6 | "Einfach RAG/Vector-Store drueber" | Statisches RAG reicht fuer Code-Wissen NICHT (kleine Details kippen das Ergebnis) — aktive Governance/Kuration noetig | §6 |
| 7 | Trigger-Metadaten (description/Stichworte) | Muessen strukturiert/maschinenlesbar sein; unstrukturierte Metadaten werden ignoriert (Cursor: `.md` ohne frontmatter greift nicht) | §7 |
| 8 | Wissen veraltet | Kern-/meistgenutztes Wissen veraltet am schnellsten (Risiko-Asymmetrie); ohne Staleness-Markierung wird blind vertraut | §8 |
| 9 | Bug erlebt | Sowohl Almanach ALS AUCH `bug-cases.jsonl` aktualisieren — auch fuer HARNESS-Bugs (versickern sonst) | §9 |
| 10 | Whiteboard/MEMORY.md schreiben | Auto-Log-Spam (Speicher/Effort) raten-limitieren/deduplizieren, sonst ertrinkt das echte Signal | §10 |
| 11 | Web-Recherche per `WebFetch` auf `github.com` | github.com ist fuer WebFetch blockiert (verlangt `gh`-CLI) → auf `npmjs.com`/`sourcepulse.org`/offizielle Doku/`WebSearch` ausweichen, fuer Repo-Daten `gh`-CLI | §11 |
| 12 | Researcher-Schwarm (parallele Web-Recherche) | Zu viele gleichzeitige Researcher/Fetches → Server-Rate-Limit (429). ~8 Fetches/Researcher, 5-6 gleichzeitig, gestaffelt (Continuous-Spawning); Findings NIE kappen, nur Rate drosseln | §12 |
