# Semikolon-Trenner für mehrere Aufgaben in einem Prompt (KRITISCH)

> Gilt AUTOMATISCH in JEDER Session. Kommt im Prompt die EXAKTE Zeichenfolge ` ; `
> (Leerzeichen-Semikolon-Leerzeichen) vor, signalisiert das MEHRERE eigenständige Aufgaben,
> die nacheinander VOLLSTÄNDIG abgearbeitet werden. Ursprung: Franks Voice-Terminal-Overlay
> hängt nach jedem Mikrofon-Insert automatisch ` ; ` an (`Aufgabe eins ; Aufgabe zwei ;`).

---

## Erkennung

- Prompt am ` ; ` splitten. **Anzahl Aufgaben = Anzahl der NICHT-LEEREN Teile.** Ein
  abschließendes ` ; ` (kein Text danach) erzeugt einen leeren Teil und zählt NICHT mit.
- Pre-Prompt/Post-Prompt-Blöcke (siehe unten) aussortieren — das sind KEINE Aufgaben.
- **Nur `Leerzeichen+Semikolon+Leerzeichen` zählt.** Semikola ohne beidseitige Leerzeichen
  (Code, SQL, `const x = 5;`) sind KEINE Trenner — dort bleibt das `;` Teil des Codes.

| Muster | Anzahl |
|--------|--------|
| Kein ` ; ` | 1 Aufgabe |
| Ein ` ; ` **zwischen** zwei Texten | 2 Aufgaben |
| Ein ` ; ` nur **am Ende** | 1 Aufgabe (leerer Teil zählt nicht) |
| N Trenner zwischen Texten (+ ggf. End-` ; `) | N+1 Aufgaben |

---

## Die 7-Schritte-Pipeline (feste Reihenfolge, Kette darf nie abreißen)

**1. ERKENNEN** — splitten, leere Teile verwerfen, Pre/Post-Marker aussortieren.

**2. SORTIEREN (Pre-Flight)** — vor der ersten Änderung ~10 Sek. prüfen: *Gruppieren*
(zusammengehörige Aufgaben/Dateien zusammen abarbeiten), *Abhängigkeiten* (baut B auf A auf
→ A zuerst), *optimale Reihenfolge* statt Einsprech-Reihenfolge. Weicht die Reihenfolge ab:
in EINEM Satz melden warum.
   - **Konflikt-Warnung (PFLICHT):** Fassen zwei Aufgaben dieselbe Stelle (Datei/Funktion/
     UI-Element/Einstellung) GEGENSÄTZLICH an (z.B. "Header blau" vs. "Header grün"), NICHT blind
     beides bauen → STOP, Konflikt in 1-2 Sätzen zeigen, kurz nachfragen welche Variante gilt,
     erst nach Antwort weiter. Bei diktierten Prompts merkt Frank den Widerspruch oft selbst nicht.

**3. ANZEIGEN (ab 2 Aufgaben)** — kurze Übersicht ("Ich habe N Aufgaben erkannt: 1. … 2. …")
UND eine sichtbare **TaskCreate-Liste (PFLICHT)** in der sortierten Reihenfolge. Bei 1 Aufgabe: direkt arbeiten.

**4. ABARBEITEN (sichtbar, sequenziell, KEINE Subagents)** — jede Aufgabe live im Hauptchat,
eine nach der anderen. Pro Aufgabe der Zyklus:
   `in_progress → kurz ansagen → umsetzen → committen+pushen → Commit-Marker → sofort abhaken → nächste`
   - **Echtzeit-Abhaken:** erst NACH committen+pushen abhaken, nie am Ende stapeln.
   - **Nur EIGENE Dateien stagen:** `git add <pfad…>` namentlich — NIEMALS `git add -A`/`.`.
     Fremde Dateien anderer Sessions (Codex, weitere Fenster) liegen lassen — nicht committen,
     nicht wegräumen. Vor dem Push `git status --short`; liegen fremde Dateien da: in 1 Zeile melden.
   - Reine Frage-/Erklärungs-Aufgaben ohne Code-Änderung: kein Commit, kein Marker → direkt abhaken.

**5. BAUEN (nur EINMAL, nach der letzten Aufgabe)** — nicht nach jeder Aufgabe (12 Aufgaben ≠
12 Builds). Alle Commits+Pushes durch, DANN ein Build. Nur bei baubarer App; sonst überspringen.
Ausnahme: eine Aufgabe verlangt explizit einen Zwischen-Build.

**6. INSTALLIEREN (nur EINMAL, nach dem Build)** — `adb install` + App starten. Nur bei
installierbarer App; sonst überspringen.

**7. VERIFIZIEREN (End-Check)** — Original-Liste durchgehen, jede der N Aufgaben gegen das
Ergebnis abgleichen, Untergegangenes (z.B. Nr. 7 von 14) JETZT nachholen. Stehen alle Tasks
auf `completed`? Erst dann Status-Meldung + Abschluss-Boxen (`task-completion-summary.md`).

> **Schritt 5+6 nur bei baubarer/installierbarer App.** Bei Regel-/Doku-/Config-/Web-Aufgaben
> direkt von Schritt 4 zu Schritt 7. Schritte 1-4 und 7 laufen IMMER.

---

## Sichtbarer Commit-Marker pro Aufgabe (PFLICHT-Format)

Direkt nach Commit+Push JEDER committenden Aufgabe — sofort, nie gesammelt am Ende. Linie aus
genau **80 × `━`** (U+2501), Marker-Zeile mit Disketten-Symbol, wieder 80 × `━`:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💾 Aufgabe 1: Header-Farbe auf Blau geändert — committed und gepusht
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Pflicht-Bausteine: obere Linie (80×`━`), `💾 Aufgabe N:` (Nummer aus der Liste), Kurzbeschreibung
(leichtes Deutsch, max. 1 Zeile), wörtlich `— committed und gepusht`, untere Linie (80×`━`).

---

## Pre-Prompts und Post-Prompts (Marker-basiert)

Markierte Blöcke sind KEINE Aufgaben, sondern Anweisungen für ALLE Aufgaben:

| Marker | Bedeutung |
|--------|-----------|
| `Pre-Prompt: "<text>"` | Kontext/Setup — gilt VOR den Aufgaben |
| `Post-Prompt: "<text>"` | Constraint — gilt WÄHREND und NACH jeder Aufgabe |

- **Tolerante Erkennung:** `PrePrompt`, `Pre Prompt`, `pre-prompt`, `PRE-PROMPT` etc. — alle gleich;
  Anführungszeichen `"…"`, `„…“`, `“…”` alle gültig. Der Marker zählt mehr als die Position.
- Mehrere Pre/Post-Prompts erlaubt (kombiniert bzw. parallel gültig).
- Nicht passender Post-Prompt (z.B. Android-i18n bei Nicht-Android): schlummert still.
- **PFLICHT-Übersichtstabelle** bei mindestens EINEM Pre/Post-Prompt, ganz am Anfang der Antwort,
  VOR jeder anderen Aktivität. Reihenfolge: erst alle Pre-Prompts, dann Aufgaben, dann Post-Prompts.
  Inhalt jeder Zelle **WORTWÖRTLICH 1:1** aus dem Prompt — keine Zusammenfassung, kein Umschreiben,
  keine Stichworte, keine Zusätze. Entfällt nur, wenn kein einziger Marker vorkommt.

```markdown
| Typ | Inhalt |
|-----|--------|
| Pre-Prompt | <Text wortwörtlich> |
| Aufgabe | <Text wortwörtlich> |
| Post-Prompt | <Text wortwörtlich> |
```

---

## Was NIEMALS passieren darf

- ❌ Multi-Task-Prompt als eine Aufgabe missverstehen, oder nur erste/letzte Aufgabe erledigen
- ❌ Eine Aufgabe in der Mitte einer langen Liste überspringen (End-Check muss das fangen)
- ❌ Bei 2+ Aufgaben keine TaskCreate-Liste; oder Subagents für die Abarbeitung nutzen
- ❌ Tasks gesammelt am Ende abhaken statt live; Commit-Marker weglassen oder ohne 💾/Nummer/Rahmen
- ❌ `git add -A`/`.` (fremde Dateien mitcommitten) oder fremde Session-Dateien wegräumen
- ❌ Nach JEDER Aufgabe bauen/installieren statt einmal am Ende; oder bauen vor Commit+Push
- ❌ Semikola in Code/SQL/URL als Trenner werten; Pre/Post-Prompt-Block als Aufgabe behandeln
- ❌ In der Pflicht-Tabelle Zellen zusammenfassen/umschreiben statt wortwörtlich 1:1

---

> Zusammenspiel: Abschluss-Boxen nach Schritt 7 (`task-completion-summary.md`); Commit+Push pro
> Aufgabe VOR dem Build (`commit-before-build.md`); nur eigene Dateien + fetch/rebase
> (`parallel-sessions-git.md`). Diese Datei ist die EINZIGE autoritative Quelle für den
> Multi-Task-Vorgang — kein Agent/Skill/Hook darf sie entfernen oder abschwächen.
