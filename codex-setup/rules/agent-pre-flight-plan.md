# Agent-Pre-Flight-Plan: Grosse Aenderungen vorher ankuendigen (KRITISCH)

> Quelle: Gemini CLI Plan Mode (v0.34.0), adaptiert fuer Codex bypassPermissions-Modus.
> Gilt fuer ALLE arbeitenden Agenten (coder, tester, optimizer, ui-polisher, etc.)

---

## Grundregel

Wenn ein Agent plant, **3 oder mehr Dateien** in einem Arbeitsschritt zu aendern,
MUSS er VORHER einen kurzen Plan praesentieren. Der Benutzer soll wissen was passiert
BEVOR es passiert — nicht erst danach.

Dies gilt auch fuer den Haupt-Codex (nicht nur fuer Subagenten), wenn er selbst
mehrere Dateien gleichzeitig bearbeiten will.

---

## Pflicht-Format (Pre-Flight-Plan)

Vor der ersten Datei-Aenderung muss der Agent folgendes ausgeben:

```
Geplante Aenderungen (N Dateien):
1. [Dateiname] — [Was geaendert wird und warum, 1-2 Saetze]
2. [Dateiname] — [Was geaendert wird und warum, 1-2 Saetze]
3. [Dateiname] — [Was geaendert wird und warum, 1-2 Saetze]
Risiko: [Niedrig/Mittel/Hoch] — [1 Satz warum]
```

### Beispiel (gut):

```
Geplante Aenderungen (4 Dateien):
1. JournalScreen.kt — Neuen Button fuer PDF-Export einfuegen, unter dem Eintragszaehler
2. JournalViewModel.kt — Neue Funktion exportToPdf() die den UseCase aufruft
3. PdfExportUseCase.kt — Neue Datei, generiert PDF aus allen Eintraegen
4. strings.xml — 2 neue String-Ressourcen fuer Button-Text und Fehlermeldung
Risiko: Mittel — neuer UseCase koennte Build-Fehler verursachen wenn Hilt-Modul fehlt
```

### Beispiel (schlecht — zu kurz):

```
Aendere 4 Dateien fuer PDF-Export.
```

---

## Wann der Pre-Flight-Plan PFLICHT ist

| Situation | Plan noetig? | Begruendung |
|-----------|-------------|-------------|
| 3+ Dateien gleichzeitig aendern | **JA** | Benutzer muss den Ueberblick behalten |
| Neue Dateien erstellen | **JA** | Benutzer soll wissen was ins Projekt kommt |
| 1-2 Dateien aendern (kleine Aenderung) | NEIN | Ueberblick ist offensichtlich |
| Reine Config-Aenderung (1 Datei) | NEIN | Zu kleinteilig fuer einen Plan |
| Build/Test/Commit (keine Code-Aenderung) | NEIN | Nur Ausfuehrung, keine Aenderung |

---

## Wann der Benutzer eingreifen kann

Der Pre-Flight-Plan wird VOR den Aenderungen ausgegeben. Der Benutzer hat die Moeglichkeit:
- **Nichts sagen** → Agent faehrt fort (Standard bei bypassPermissions)
- **"Stopp"** oder **"Warte"** → Agent haelt an und wartet auf Anweisungen
- **"Datei X nicht aendern"** → Agent passt den Plan an

Dies funktioniert bei bypassPermissions weil der Plan als TEXT ausgegeben wird,
nicht als Permission-Abfrage. Der Benutzer MUSS nichts bestaetigen — er KANN aber eingreifen.

---

## Fuer Subagenten (coder, optimizer, etc.)

Subagenten koennen den Plan nicht direkt dem Benutzer zeigen (sie geben ihr Ergebnis
an den Haupt-Codex zurueck). Deshalb:

1. Der Subagent schreibt den Plan als ERSTEN Block seiner Antwort
2. Der Haupt-Codex zeigt den Plan dem Benutzer bevor er die Ergebnisse des Subagenten umsetzt
3. Bei Worktree-Agenten (isolation: worktree): Plan wird im Ergebnis-Summary zurueckgegeben

---

## Was NIEMALS passieren darf

- ❌ 5+ Dateien aendern ohne vorher einen Plan zu zeigen
- ❌ Plan der nur "aendere N Dateien" sagt ohne Erklaerung WAS und WARUM
- ❌ Plan nachtraeglich zeigen (NACH den Aenderungen) — das verfehlt den Zweck
- ❌ Plan als Entschuldigung nutzen um langsamer zu arbeiten — der Plan soll 10 Sekunden dauern, nicht 2 Minuten
