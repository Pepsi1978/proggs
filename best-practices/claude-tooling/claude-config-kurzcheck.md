# Claude-Code Konfiguration & Regeln-Integration Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre
> (`Read` mit `limit=80`). Volltext bei Fehlern im Bereich (Stufe B) und vor
> Hochrisiko-Arbeit (Stufe C).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Ich will X durchsetzen | Entscheidungsbaum: muss-immer → Hook/`deny`, sonst CLAUDE.md/Rule | §0 |
| 2 | Sicherheits-/Format-Garantie | NIE nur CLAUDE.md (advisory) — Hook oder `permissions.deny` | §1 |
| 3 | Regel soll besser befolgt werden | Emphasis (YOU MUST), spezifisch, Begruendung, Beispiele | §2 |
| 4 | CLAUDE.md schlank halten | < 200 Zeilen, Volltext in `~/.claude/rules/`; `@import` spart nichts | §3 |
| 5 | Tokens echt sparen | path-scoped Rule (`paths:` quoten) oder Skill (on-demand) | §3 |
| 6 | Rolle/Ton/Format dauerhaft | Output-Style (nicht CLAUDE.md), Projekt-Wissen bleibt in CLAUDE.md | §4 |
| 7 | Regel soll /compact ueberleben | Root-CLAUDE.md + unscoped Rules; NIE path-scopen/nesten | §5 |
| 8 | Subagent braucht die Regel | `SubagentStart`-Hook `additionalContext` (garantiert da) | §5 |
| 9 | Eine Vorstellung hart absichern | Defense-in-Depth: Rule + Skill + Hook + Memory | §6 |
| 10 | Regel wird wiederholt ignoriert | In einen Hook umwandeln statt CLAUDE.md verlaengern | §6 |
| 11 | Aktuelle 2.1.x-Features nutzen | Lean System Prompt, `reloadSkills`, `skillOverrides` | §7 |
