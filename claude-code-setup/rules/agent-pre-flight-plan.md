# Agent-Pre-Flight-Plan: Grosse Aenderungen vorher ankuendigen (KRITISCH)

> Gilt fuer ALLE arbeitenden Agenten UND den Haupt-Claude.

## Grundregel

Wer plant, **3 oder mehr Dateien** in einem Arbeitsschritt zu aendern (oder neue Dateien zu erstellen),
MUSS VORHER einen kurzen Plan praesentieren — der Benutzer soll wissen was passiert BEVOR es passiert.

## Pflicht-Format

```
Geplante Aenderungen (N Dateien):
1. [Dateiname] — [Was + warum, 1-2 Saetze]
2. [Dateiname] — [Was + warum]
Risiko: [Niedrig/Mittel/Hoch] — [1 Satz warum]
```

## Wann Pflicht

| Situation | Plan? |
|-----------|-------|
| 3+ Dateien gleichzeitig aendern | JA |
| Neue Dateien erstellen | JA |
| 1-2 Dateien (kleine Aenderung) | NEIN |
| Reine Config-Aenderung (1 Datei) | NEIN |
| Build/Test/Commit (keine Code-Aenderung) | NEIN |

## Eingriff

Der Plan wird als TEXT ausgegeben (nicht als Permission-Abfrage) — bei bypassPermissions faehrt der
Agent fort, der Benutzer KANN aber "Stopp"/"Datei X nicht aendern" sagen. Subagenten schreiben den Plan
als ERSTEN Block ihrer Antwort; der Haupt-Claude zeigt ihn dem Benutzer vor der Umsetzung.

## Was NIEMALS passieren darf

- 5+ Dateien aendern ohne vorher einen Plan · Plan der nur "aendere N Dateien" sagt (ohne WAS/WARUM)
- Plan nachtraeglich zeigen (nach den Aenderungen)
