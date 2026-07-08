# Semikolon-Trenner für mehrere Aufgaben in einem Prompt (KRITISCH)

> Gilt AUTOMATISCH in JEDER Session. Die EXAKTE Zeichenfolge ` ; ` (Leerzeichen-Semikolon-Leerzeichen)
> signalisiert MEHRERE eigenständige Aufgaben, nacheinander VOLLSTÄNDIG abgearbeitet. Ursprung: Franks
> Voice-Terminal-Overlay hängt nach jedem Mikrofon-Insert automatisch ` ; ` an.

## Erkennung

- Prompt am ` ; ` splitten. **Anzahl Aufgaben = Anzahl NICHT-LEERER Teile** (ein abschließendes ` ; `
  erzeugt einen leeren Teil und zählt NICHT). Pre-/Post-Prompt-Blöcke aussortieren (keine Aufgaben).
- **Nur `Leerzeichen+Semikolon+Leerzeichen` zählt** — Semikola ohne beidseitige Leerzeichen (Code, SQL,
  `const x = 5;`) sind KEINE Trenner.

| Muster | Anzahl |
|--------|--------|
| Kein ` ; ` | 1 Aufgabe |
| Ein ` ; ` **zwischen** zwei Texten | 2 Aufgaben |
| Ein ` ; ` nur **am Ende** | 1 Aufgabe (leerer Teil zählt nicht) |
| N Trenner zwischen Texten (+ ggf. End-` ; `) | N+1 Aufgaben |

## Die 7-Schritte-Pipeline (feste Reihenfolge, Kette darf nie abreißen)

1. **ERKENNEN** — splitten, leere Teile verwerfen, Pre/Post-Marker aussortieren.
2. **SORTIEREN (Pre-Flight)** — vor der ersten Änderung ~10 Sek prüfen: Gruppieren (Zusammengehöriges
   zusammen), Abhängigkeiten (baut B auf A → A zuerst), optimale statt Einsprech-Reihenfolge. Weicht sie
   ab: in EINEM Satz melden. **Konflikt-Warnung (PFLICHT):** fassen zwei Aufgaben dieselbe Stelle
   GEGENSÄTZLICH an (z.B. "Header blau" vs. "grün") → STOP, Konflikt in 1-2 Sätzen zeigen, nachfragen,
   erst nach Antwort weiter.
3. **ANZEIGEN (ab 2 Aufgaben)** — kurze Übersicht ("N Aufgaben erkannt: 1. … 2. …") UND sichtbare
   **TaskCreate-Liste (PFLICHT)** in sortierter Reihenfolge. Bei 1 Aufgabe: direkt arbeiten.
4. **ABARBEITEN (sichtbar, sequenziell, KEINE Subagents)** — jede Aufgabe live im Hauptchat, Zyklus:
   `in_progress → ansagen → umsetzen → committen+pushen → Commit-Marker → sofort abhaken → nächste`.
   Echtzeit-Abhaken erst NACH committen+pushen (nie stapeln). **Nur EIGENE Dateien** `git add <pfad…>`
   namentlich — NIEMALS `git add -A`/`.`; fremde Dateien liegen lassen, vor Push `git status --short`.
   Reine Frage-/Erklär-Aufgabe ohne Code-Änderung: kein Commit/Marker → direkt abhaken.
5. **BAUEN (nur EINMAL, nach der letzten Aufgabe)** — nicht nach jeder Aufgabe. Nur bei baubarer App.
6. **INSTALLIEREN (nur EINMAL, nach dem Build)** — `adb install` + App starten. Nur bei installierbarer App.
7. **VERIFIZIEREN (End-Check)** — Original-Liste durchgehen, jede der N Aufgaben gegen das Ergebnis
   abgleichen, Untergegangenes JETZT nachholen. Alle auf `completed`? Erst dann Status-Meldung + Abschluss-Boxen.

> Schritt 5+6 nur bei baubarer/installierbarer App; bei Regel-/Doku-/Config-/Web-Aufgaben direkt von
> Schritt 4 zu 7. Schritte 1-4 und 7 laufen IMMER.

## Sichtbarer Commit-Marker pro Aufgabe (PFLICHT-Format)

Direkt nach Commit+Push JEDER committenden Aufgabe — sofort, nie gesammelt. Linie aus genau
**80 × `━`** (U+2501), Marker-Zeile mit Disketten-Symbol, wieder 80 × `━`:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💾 Aufgabe 1: Header-Farbe auf Blau geändert — committed und gepusht
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Pflicht-Bausteine: obere Linie (80×`━`), `💾 Aufgabe N:` (Nummer aus der Liste), Kurzbeschreibung
(1 Zeile), wörtlich `— committed und gepusht`, untere Linie (80×`━`).

## Pre-Prompts und Post-Prompts (Marker-basiert)

Markierte Blöcke sind KEINE Aufgaben, sondern Anweisungen für ALLE Aufgaben:

| Marker | Bedeutung |
|--------|-----------|
| `Pre-Prompt: "<text>"` | Kontext/Setup — gilt VOR den Aufgaben |
| `Post-Prompt: "<text>"` | Constraint — gilt WÄHREND und NACH jeder Aufgabe |

- **Tolerante Erkennung:** `PrePrompt`, `Pre Prompt`, `pre-prompt` etc. gleich; Anführungszeichen
  `"…"`/`„…"`/`"…"` alle gültig — der Marker zählt mehr als die Position. Mehrere erlaubt. Nicht
  passender Post-Prompt (z.B. Android-i18n bei Nicht-Android) schlummert still.
- **PFLICHT-Übersichtstabelle** bei mindestens EINEM Pre/Post-Prompt, ganz am Anfang der Antwort, VOR
  jeder anderen Aktivität. Reihenfolge: erst Pre-Prompts, dann Aufgaben, dann Post-Prompts. Jede Zelle
  **WORTWÖRTLICH 1:1** aus dem Prompt (keine Zusammenfassung, kein Umschreiben). Entfällt nur ohne jeden Marker.

```markdown
| Typ | Inhalt |
|-----|--------|
| Pre-Prompt | <Text wortwörtlich> |
| Aufgabe | <Text wortwörtlich> |
| Post-Prompt | <Text wortwörtlich> |
```

## Was NIEMALS passieren darf

- ❌ Multi-Task-Prompt als eine Aufgabe missverstehen, oder nur die erste/letzte erledigen
- ❌ Eine Aufgabe in der Mitte einer langen Liste überspringen (End-Check muss das fangen)
- ❌ Bei 2+ Aufgaben keine TaskCreate-Liste; oder Subagents für die Abarbeitung nutzen
- ❌ Tasks gesammelt am Ende abhaken statt live; Commit-Marker weglassen oder ohne 💾/Nummer/Rahmen
- ❌ `git add -A`/`.` (fremde Dateien mitcommitten) oder fremde Session-Dateien wegräumen
- ❌ Nach JEDER Aufgabe bauen/installieren statt einmal am Ende; oder bauen vor Commit+Push
- ❌ Semikola in Code/SQL/URL als Trenner werten; Pre/Post-Prompt-Block als Aufgabe behandeln
- ❌ In der Pflicht-Tabelle Zellen zusammenfassen/umschreiben statt wortwörtlich 1:1

> Zusammenspiel: Abschluss-Boxen nach Schritt 7 (`task-completion-summary.md`); Commit+Push pro Aufgabe
> VOR dem Build (`git-workflow.md`); nur eigene Dateien + fetch/rebase (`parallel-sessions-git.md`).
> Diese Datei ist die EINZIGE autoritative Quelle für den Multi-Task-Vorgang.
