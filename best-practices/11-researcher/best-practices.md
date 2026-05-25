# Researcher & Internet-Recherche — Best Practices (Stand 2026-05-25, Claude Code 2.1.150)

> Neue Kategorie (11), angelegt 2026-05-25. Fokus: robuster Einsatz von Researcher-Subagenten /
> Internet-Recherche — Parallelitaet, Token-/Fetch-Limits, Absturz-Vermeidung, Checkpointing,
> gute Prompts. Eigener Fokus neben Kategorie 3 (Agents), weil Internet-Researcher sehr oft genutzt
> werden und dabei oft abstuerzen.

> TODO: Beim naechsten `/best-practices`-Lauf von einem eigenen Researcher fuellen lassen
> (offizielle Anthropic-Quellen zuerst: code.claude.com/docs zu Subagents / Agent-Tool /
> Parallelitaet, plus bewaehrte Muster gegen Researcher-Abstuerze).

## Schon bekannte Leitplanken (intern, eigene Erfahrung)

## Parallelitaet & Scope
- **Was:** Mehrere Internet-Researcher gleichzeitig sind moeglich; enger Scope verhindert Abstuerze.
- **Best Practice:** 1 Researcher pro abgegrenztem Bereich, alle gleichzeitig starten (bis ~20).
  Lieber viele kleine als wenige grosse — ein enger Scope haelt jeden unter dem Token-Ziel.
- **Quelle:** intern (Erstlauf-Erfahrung 2026-05-25)
- **Stand:** 2026-05-25

## Limits gegen Absturz
- **Was:** Researcher stuerzen ab, wenn sie zu viel sammeln; sie messen ihren Verbrauch nicht selbst.
- **Best Practice:** Token-Ziel ~130k pro Researcher, max 50 Ergebnisse / 15 Web-Fetches / 10 Min.
  Durchsetzung ueber engen Scope, nicht ueber Selbstmessung.
- **Quelle:** intern + ~/.claude/rules/agent-and-researcher-rules.md
- **Stand:** 2026-05-25

## Checkpointing & Continuation
- **Was:** Ein abgestuerzter Researcher verliert seinen gesamten Output.
- **Best Practice:** Inkrementell in die Zieldatei schreiben + Checkpoint-Marker setzen; bei Limit
  einen Continuation-Researcher am Checkpoint starten. Rueckgabe knapp halten (kurze Zusammenfassung
  statt Rohdaten), damit der Hauptkontext nicht ueberlaeuft.
- **Quelle:** intern (Erstlauf-Erfahrung 2026-05-25)
- **Stand:** 2026-05-25
