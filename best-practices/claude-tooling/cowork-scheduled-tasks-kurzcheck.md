# Cowork — Geplante & wiederkehrende Aufgaben Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Welches System? | Failure-Cost-Test + 5-Fragen-Routing: Cloud (ohne wachen PC) · Local (lokale Dateien) · `/loop` (nur in Session) | §A1, §A2 |
| 2 | Prompt schreiben | Selbst-enthaltend: Ziel, Schritte, Pfade/URLs/Repos, Connectors **namentlich**, Ausgabeformat, Präferenzen | §B1, §B2 |
| 3 | Catch-up-Verspätung | Zeit-Guardrails: „nur heutige Daten; nach 17 Uhr nur Summary"; feste Fenster statt relativer Bezüge | §B3 |
| 4 | Was soll Claude IGNORIEREN? | Explizit benennen („skip Newsletter/Auto-Mails") — niemand korrigiert live | §B4 |
| 5 | Kadenz | Presets bevorzugen, Cron nur für Sonderfälle; Min: Local 1 Min / Cloud 1 h | §C1, §C2 |
| 6 | Exaktes Timing | Wegen Jitter ungerade Minute wählen (`3 9 * * *` statt `0 9`) | §C3 |
| 7 | Model & Worktree | Model pro Task passend; Worktree-Toggle bei Git-Repos mit uncommitteter Arbeit | §D1, §D3 |
| 8 | Permissions vorab | Nach Anlegen **„Run now"** + pro Tool **„always allow"** (sonst Stall im Ask-Mode) | §D2 |
| 9 | ⭐ Sicherheit: Start | Read-only zuerst (Briefings/Reports); keine sensiblen Daten/Connectors, keine irreversiblen Aktionen | §E1, §E2 |
| 10 | ⭐ Sicherheit: Scope | Dedizierter Ordner + minimaler Mount-Modus (read-only > no-delete > read-write); „do NOT delete/send" im Prompt | §E3, §E4 |
| 11 | Failure-Cost-Tier | Tier 1 unbeaufsichtigt ok · Tier 2 nur mit Review · Tier 3 (Geld/Mail/Löschen) nie unbeaufsichtigt | §E5 |
| 12 | Zuverlässigkeit | „Keep computer awake"; Garantie nur per Cloud-Routine (oder 24/7-Rechner) | §F1, §F2 |
| 13 | Verifikation | „Run now" testen + Transkript lesen — grüner Status ≠ Erfolg; Run-History für Skip-Gründe | §G1, §G2 |
| 14 | Verwalten/Verfeinern | SKILL.md auf Disk editieren; Self-Reschedule per `update_scheduled_task`; Verwaltung per Klartext | §G3, §G4 |
| 15 | Dauerkontext | `claude.md` im Arbeitsordner (Rolle, Projekte, VIPs, Prioritäten) → relevantere, kürzere Prompts | §G5 |
| 16 | Quota | Jeder Lauf = volle Session → leichte Tasks bündeln, Frequenz maßvoll, erste Läufe prüfen | §H1 |

> **Goldene Grundregel:** Erst die *Failure-Cost* bestimmen, dann das System wählen — und den Prompt so
> schreiben, dass er **kalt, unbeaufsichtigt und evtl. zur falschen Uhrzeit** trotzdem das Richtige tut.
