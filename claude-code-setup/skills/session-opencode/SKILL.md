---
name: session-opencode
description: 'OpenCode-Variante des Session-Backup/Restore (PowerShell-portiert fuer OpenCode shell=pwsh), mit EIGENEN Dateien getrennt vom Claude-Code-Backup. Nutze IMMER in OpenCode, wenn der Benutzer sagt: "session opencode backup", "starte session opencode backup", "sichere die opencode session", "opencode session sichern", "backup vor /new", "sichere den opencode stand" -> Modus BACKUP; "session opencode restore", "starte session opencode restore", "opencode session wiederherstellen", "lade das opencode backup", "mache in opencode weiter wo wir waren" (direkt nach /new) -> Modus RESTORE. BACKUP schreibt eine sehr gruendliche Uebergabe-Notiz (Ziel, letzte Aufgaben + Ergebnisse, offene Fragen, Vorabinfos, Plaene, uncommitteter Stand, naechste Schritte) lokal UND ins Repo; RESTORE liest die neueste Notiz, sagt klar wo zuletzt gearbeitet wurde und welche Fortsetzungen sinnvoll sind, dann setzt es nahtlos fort. Speziell fuer OpenCode — NICHT mit dem Claude-Code-"session"-Skill verwechseln (eigene Backup-Dateien).'
---

# Session-OpenCode — Backup & Restore (PowerShell)

## Warum es diesen Skill gibt

Wenn das Kontextfenster einer OpenCode-Session voll wird, geht beim Komprimieren unkontrolliert
Detail verloren. Besser ist ein **kontrollierter Schnitt**: kurz vor der Grenze eine saubere, von
dir (dem Agenten) kuratierte Uebergabe-Notiz schreiben, dann eine **neue Session** (`/new`), dann
die Notiz wieder einlesen. So bestimmst DU, was erhalten bleibt, statt es der Automatik zu ueberlassen.

Was ein `/new` ueberlebt, sind **Dateien** — nicht der Gespraechsverlauf. Genau darauf baut dieser
Skill: Das Backup ist eine Datei, die nach dem `/new` wieder gelesen wird.

**Dies ist die OpenCode-Fassung.** Zwei Unterschiede zum Claude-Code-`session`-Skill, beide wichtig:
1. **Shell = PowerShell (`pwsh`).** OpenCode fuehrt Shell-Befehle auf Windows ueber PowerShell aus.
   Alle Schreib-/Lese-/Pruef-Schritte hier sind PowerShell — keine Bash-Heredocs, keine POSIX-Tests.
2. **Eigene Backup-Dateien** (`session-opencode-backup.md`). So ueberschreiben sich OpenCode- und
   Claude-Code-Backups NIE gegenseitig — jede Welt hat ihren eigenen festen Ablageort.

## Unterbefehl erkennen

Der Benutzer nennt einen von zwei Modi. Erkenne ihn aus der Anfrage:

| Anfrage enthaelt … | Modus |
|--------------------|-------|
| "backup", "sichern", "sichere den stand", "vor /new" | **BACKUP** |
| "restore", "wiederherstellen", "lade das backup", "mache weiter wo wir waren" | **RESTORE** |

Im Zweifel kurz nachfragen, welcher der beiden gemeint ist.

## Pfade (fest, OpenCode-eigen)

```
LOKAL : $HOME/.claude/session-opencode-backup.md
REPO  : $HOME/proggs/.claude/session-opencode-backup.md
```

In PowerShell loest `$HOME` korrekt auf (Windows: `C:\Users\barwa`). Es gibt bewusst nur **eine**
feste Datei je Ort — sie wird bei jedem Backup ueberschrieben, damit sich nie mehrere Sessions
vermischen. Diese Pfade sind ANDERS als beim Claude-Code-Skill (`session-backup.md`) — bewusst getrennt.

---

## BACKUP-Workflow

> **TEMPO-REGEL (Backup soll SCHNELL gehen):** Die Backup-Datei existiert nach einem frueheren
> Restore leer oder veraltet — **NICHT vorher mit dem Read-Tool lesen, nicht pruefen ob sie voll
> ist.** Einfach blind ueberschreiben (genau dafuer wird das Backup gestartet). Schreibe die Notiz
> DIREKT per PowerShell-Here-String, NICHT mit dem Write/Edit-Tool — die Datei-Tools erzwingen bei
> existierenden Dateien ein vorheriges Read und kosten ueberfluessige Schritte.

> **WICHTIG — Backup MITTEN in einer laufenden Aufgabe ist der Normalfall:** Der Benutzer macht das
> Backup oft BEWUSST, waehrend eine lange Aufgabe noch laeuft — er bricht ab (ESC) und sagt dann
> "session opencode backup". Dann NICHT "erst fertig machen". Deine WICHTIGSTE Aufgabe ist, den
> **exakten Unterbrechungspunkt** im Abschnitt "Laufende/unterbrochene Aufgabe" festzuhalten — so
> genau, dass die frische Session ohne jeden Kontext exakt diesen einen Schritt wieder aufnimmt.

> **WICHTIG — Backup muss die spaetere Restore-Einleitung ERMOEGLICHEN:** Nicht nur technische
> Daten sichern. Das Backup muss so geschrieben sein, dass die naechste Session Frank sofort sagen
> kann: "Hier waren wir, das ist wichtig, dort koennen wir weitermachen." Sammle deshalb aktiv auch
> Plaene, Vorab-Informationen, besprochene Richtungen, offene Entscheidungen, Warnungen und sinnvolle
> Anschlussarbeiten aus der ganzen aktuellen Session ein. Wenn diese Informationen im Backup fehlen,
> kann Restore sie spaeter nicht ehrlich rekonstruieren.

### Schritt 1: Arbeitsstand + Orientierungsstoff erfassen (PFLICHT)

Bevor du die Notiz schreibst, EINMAL den nicht-committeten Arbeitsstand abfragen (git-Befehle sind
in PowerShell identisch):

```powershell
git -C "$HOME/proggs" status --short                          # M = geaendert, ?? = NEU (untracked)
git -C "$HOME/proggs" diff -- <eigene-geaenderte-dateien>     # Diff der GETRACKTEN Aenderungen
git -C "$HOME/proggs" diff --shortstat -- <eigene-dateien>    # Groesse messen
# git diff zeigt NEUE (??) Dateien NICHT — ihren Inhalt je Datei separat erfassen:
Get-Content "<neue-datei>"                                     # voller Inhalt der neuen Datei
```

Dieser Output gehoert in den Wiedereinstiegspunkt (Feld "Uncommitteter Arbeitsstand"). NUR die
eigenen/relevanten Dateien, keine fremden Parallel-Session-Dateien. **Keine Secrets** uebernehmen
(API-Keys/Tokens aus halbfertigen Edits rausredaktieren — die Datei wandert ins Repo). Groesse per
`--shortstat`: bei grossem Diff (Richtwert ~120+ Zeilen / mehrere Dateien) NICHT inline, sondern in
`$HOME/.claude/session-opencode-backup.diff` auslagern (Schritt 2b). Kleiner Diff: ruhig inline.

Zusaetzlich VOR dem Schreiben der Notiz die gesamte Session gedanklich kuratieren. Fuelle die
Restore-relevanten Abschnitte bewusst, nicht nebenbei:

- **Letzte Aufgaben & Ergebnisse:** Was wurde wirklich gemacht, in welcher Reihenfolge, mit welchem
  konkreten Ergebnis/Commit/Deploy/Verifikation?
- **Offene & gestellte Fragen:** Was hat Frank gefragt, was wurde beantwortet, was ist noch offen?
- **Vorab-Informationen:** Welche Session-Kontexte, Randbedingungen, Warnungen, Zahlen, Live-Zustaende
  oder Benutzerpraeferenzen muss die naechste Session kennen?
- **Getroffene Entscheidungen:** Welche Richtung wurde gewaehlt und warum, damit die naechste Session
  nicht zurueckrudert?
- **Fehlgeschlagene Ansaetze:** Was darf NICHT nochmal versucht werden?
- **Plaene & moegliche Weiterarbeit:** Welche Ausbaustufen, Anschlussaufgaben oder Ideen wurden
  besprochen, auch wenn sie noch kein hartes To-do sind?
- **Naechste Schritte:** Was ist die erste konkrete Aktion nach Restore, falls Frank weitermachen will?

Wenn es keine laufende Aufgabe gibt, trotzdem die letzten Arbeitsbereiche und sinnvolle Fortsetzungen
eintragen. Ein Backup mit nur "alles fertig" ist zu duenn, wenn im Chat Plaene oder Kontext standen.

### Schritt 2: An beide Orte schreiben — DIREKT per PowerShell-Here-String

Schreibe die Notiz mit einem **single-quoted Here-String** (`@'…'@`) in die lokale Datei und kopiere
sie dann ins Repo. Das ueberschreibt zuverlaessig, egal ob die Datei existiert, leer oder veraltet ist:

```powershell
New-Item -ItemType Directory -Force "$HOME/proggs/.claude" | Out-Null
@'
# Session Handoff (OpenCode) — <Datum + Uhrzeit>
... (komplette Handoff-Notiz nach dem Template unten) ...
'@ | Set-Content -Path "$HOME/.claude/session-opencode-backup.md" -Encoding utf8
Copy-Item "$HOME/.claude/session-opencode-backup.md" "$HOME/proggs/.claude/session-opencode-backup.md" -Force
```

**Warum so:**
- **Single-quoted Here-String (`@'…'@`)** → der Inhalt wird LITERAL geschrieben: keine Expansion von
  `$HOME`, Backslashes (`C:\Users`) oder Backticks. Genau das, was die Notiz braucht (Pfade,
  Platzhalter, Commit-Hashes). Der schliessende `'@` MUSS in **Spalte 0** stehen (kein Zeichen davor),
  sonst endet der String nicht. Enthielte die Notiz selbst eine Zeile, die mit `'@` beginnt, ruecke
  diese eine Zeile um ein Leerzeichen ein (kommt in der Praxis nicht vor).
- **`Set-Content -Encoding utf8`** → schreibt UTF-8 (in pwsh 7 ohne BOM). Wichtig fuer deutsche Umlaute.
- **`Copy-Item` statt zweitem Here-String** → eine Quelle, kein Risiko dass die beiden Dateien abweichen.
- **KEIN Write/Edit-Tool** → das erzwingt bei existierenden Dateien einen Read-Zwang.

**SOFORT nach dem Schreiben verifizieren (PFLICHT — kein stiller Datenverlust):** Ein fehlgeschlagener
Here-String (volle Platte, abgebrochener Schreibvorgang) wuerde ein LEERES/abgeschnittenes Backup
hinterlassen — und nach `/new` waere die Arbeit weg. Darum BEVOR ein Erfolgs-Marker kommt pruefen,
dass BEIDE Dateien plausibel gefuellt sind:

```powershell
foreach ($f in @("$HOME/.claude/session-opencode-backup.md","$HOME/proggs/.claude/session-opencode-backup.md")) {
    if (-not (Test-Path $f) -or ((Get-Item $f).Length -eq 0) -or -not (Select-String -Path $f -Pattern 'Session Handoff' -Quiet)) {
        Write-Output "BACKUP KAPUTT: $f"
    } else {
        Write-Output ("{0}: {1} Zeilen" -f $f, (Get-Content $f).Count)
    }
}
```

Schlaegt die Pruefung an (leer / Ueberschrift "Session Handoff" fehlt / verdaechtig wenige Zeilen):
NICHT den Erstellt-Marker zeigen und NICHT `/new` empfehlen — dem Benutzer melden und das Schreiben
wiederholen. Erst wenn beide Dateien plausibel gefuellt sind, weiter.

### Schritt 2b: Grossen uncommitteten Diff auslagern (NUR wenn der Diff gross ist)

Ist der uncommittete Diff zu gross fuer die Notiz (~120+ Zeilen / mehrere Dateien), schreibe ihn
lossless in eine SEPARATE, FESTE Datei (immer ueberschrieben) und referenziere sie im
Wiedereinstiegspunkt nur per Pfad:

```powershell
# Getrackte Aenderungen in die Diff-Datei schreiben ...
git -C "$HOME/proggs" diff -- <eigene-dateien-der-aufgabe> | Out-File -FilePath "$HOME/.claude/session-opencode-backup.diff" -Encoding utf8
# ... und neue (??) Dateien anhaengen (git diff erfasst sie nicht):
foreach ($f in @(<neue-dateien>)) {
    "=== NEUE DATEI: $f ===" | Out-File -FilePath "$HOME/.claude/session-opencode-backup.diff" -Append -Encoding utf8
    Get-Content $f            | Out-File -FilePath "$HOME/.claude/session-opencode-backup.diff" -Append -Encoding utf8
}
Copy-Item "$HOME/.claude/session-opencode-backup.diff" "$HOME/proggs/.claude/session-opencode-backup.diff" -Force
```

Kleiner Diff → diesen Schritt UEBERSPRINGEN, KEINE Diff-Datei anlegen. Die Diff-Datei wird beim
RESTORE nach dem Einlesen wieder GELOESCHT (siehe RESTORE Schritt 4).

### Schritt 3: Repo-Version committen und pushen

Nur die Backup-Datei stagen (namentlich — nie `git add -A`, wegen paralleler Sessions), dann
fetch + rebase + push:

```powershell
Set-Location "$HOME/proggs"
git add .claude/session-opencode-backup.md
if (Test-Path .claude/session-opencode-backup.diff) { git add .claude/session-opencode-backup.diff }
git commit -m "#NNN - session-opencode backup: handoff snapshot"
git fetch origin
git rebase origin/main
git push
```

Die fortlaufende Commit-Nummer wie ueblich aus dem letzten Commit ableiten.

**Konflikt auf der Backup-Datei** (eine parallele Session hat sie auch committed): Dein eigenes,
frisches Backup gewinnt. ACHTUNG: bei `git rebase` sind `--ours`/`--theirs` invertiert. `--theirs`
ist dein gerade angewendeter Commit (den willst du):

```powershell
git checkout --theirs .claude/session-opencode-backup.md
git add .claude/session-opencode-backup.md
git rebase --continue
git push
```

### Schritt 4: Disketten-Marker zeigen

NUR wenn die Verifikation (Ende Schritt 2) bestanden hat. Gib EXAKT dieses Format aus
(Linien je 80 Zeichen `━`):

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💾💾💾 OpenCode-Session-Backup erstellt (lokal + Repo) !!!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Schritt 5: Fette "JETZT neue Session"-Warnung

Direkt nach dem Marker, als eigener kraeftiger Block (OpenCode beendet/leert die Session mit `/new`,
NICHT `/clear`):

```
⚠️ JETZT NEUE OPENCODE-SESSION STARTEN! Tippe /new und sage danach "session opencode restore".
Nicht weiterarbeiten — sonst drohen Kontextgrenze und ein Kontext, der nicht zum Backup passt.
```

Der Schnitt muss JETZT passieren, solange Backup und Kontext noch uebereinstimmen.

---

## RESTORE-Workflow

> **Gleicher PC, direkt nach `/new` (der Normalfall):** Backup → `/new` → restore laeuft meist sofort
> am SELBEN Rechner. Der uncommittete Arbeitsstand (halbfertige Edits UND neue Dateien) liegt nach
> `/new` UNVERAENDERT noch im Working Tree. Du kannst ihn jederzeit direkt einsehen
> (`git -C "$HOME/proggs" status --short` + `git diff`) statt dich allein auf die Notiz zu verlassen.

### Schritt 1: Beide Orte pruefen, neuere nicht-leere Version waehlen

```powershell
$local = "$HOME/.claude/session-opencode-backup.md"
$repo  = "$HOME/proggs/.claude/session-opencode-backup.md"
$cands = @($local,$repo) | Where-Object { (Test-Path $_) -and ((Get-Item $_).Length -gt 0) }
$newer = $cands | Sort-Object { (Get-Item $_).LastWriteTime } -Descending | Select-Object -First 1
if ($newer) { Write-Output "Neuestes Backup: $newer" } else { Write-Output "Kein Backup vorhanden" }
```

Regeln:
- Datei fehlt oder ist leer → zaehlt als "kein Backup".
- Beide vorhanden → am selben PC i.d.R. identisch (lokal wird ins Repo kopiert); die neuere nehmen.
- Keine vorhanden → dem Benutzer sagen, dass es kein OpenCode-Backup gibt, und normal weiterarbeiten.

### Schritt 2: Drift-Pruefung — passt der Stand noch zum Backup?

Parallele Sessions am selben Repo koennen zwischendurch gepusht oder Dateien geaendert haben. Kurz
pruefen, ob sich seit dem Backup etwas verschoben hat:

- **Genannte Dateien noch da?** Existieren die unter "Relevante Dateien" / im Wiedereinstiegspunkt
  genannten Pfade noch?
- **git-Stand passt?** `git -C "$HOME/proggs" log --oneline -3` und der Branch
  (`git -C "$HOME/proggs" branch --show-current`) mit dem "Anker"-Block im Backup vergleichen.
- **Uncommitteter Stand noch da?** Falls das Backup einen "Uncommitteter Arbeitsstand" enthielt: ist
  der halbfertige Edit noch im Working Tree (`git -C "$HOME/proggs" status --short`)?

Bei erkanntem Drift NICHT einfach weitermachen: dem Benutzer in 1-2 Saetzen melden, was abweicht, und
kurz abstimmen. Passt alles, direkt weiter.

### Schritt 3: Kontext laden und fortsetzen

Lies die gewaehlte Datei vollstaendig (Read-Tool), verinnerliche Ziel, Status, **letzte Aufgaben +
Ergebnisse**, **offene/gestellte Fragen**, **Vorab-Informationen**, **Plaene und moegliche
Weiterarbeit**, naechste Schritte und besonders die fehlgeschlagenen Ansaetze. Verweist das Backup auf eine ausgelagerte Diff-Datei
(`.claude/session-opencode-backup.diff`), lies diese ebenfalls vollstaendig. **Ist der Abschnitt
"Laufende/unterbrochene Aufgabe" gefuellt, hat er VORRANG:** nimm den dort beschriebenen, zuletzt
unterbrochenen Schritt sauber neu auf und fuehre ihn zu Ende, BEVOR du zu den allgemeinen "Naechsten
Schritten" uebergehst.

### Schritt 3b: Wiedereinstiegs-Einleitung fuer Frank (PFLICHT)

Nach dem Einlesen und BEVOR du die Backup-Datei leerst, gib dem Benutzer eine klare Orientierung.
Der Zweck des Restores ist nicht nur technisches Aufraeumen, sondern dass Frank sofort weiss, woran
ihr zuletzt gearbeitet habt und wo er sinnvoll weitermachen kann. Antworte deshalb IMMER mit diesen
vier Teilen, auch wenn laut Backup keine Aufgabe offen ist:

1. **Woran wir zuletzt gearbeitet haben:** 3-6 konkrete Punkte aus "Letzte Aufgaben & Ergebnisse".
2. **Wo wir aufgehoert haben:** laufende/unterbrochene Aufgabe ODER ausdruecklich "nichts offen,
   sauber abgeschlossen"; falls Drift erkannt wurde, kurz dazusagen.
3. **Wichtige Vorab-Informationen:** offene Fragen, Entscheidungen, Recherche-Ergebnisse,
   Warnungen, fehlgeschlagene Ansaetze und Dinge, die NICHT nochmal versucht werden sollen.
4. **Woran wir jetzt weiterarbeiten koennten:** 2-5 konkrete Fortsetzungsoptionen aus den
   Naechsten Schritten/Plaenen; wenn keine Plaene im Backup stehen, aus dem gespeicherten Ziel und
   Status ableiten und klar als Vorschlag kennzeichnen.

Formuliere das als menschliche Einleitung, nicht als reine Datei-/Commit-Meldung. Eine gute erste
Zeile ist z.B.: "Wir waren zuletzt bei <Projekt/Bereich>. Der Stand aus dem Backup ist: ...". Erst
danach fortsetzen oder, wenn nichts offen ist, auf Franks Entscheidung warten.

### Schritt 4: Backups leeren + ausgelagerte Diff-Datei loeschen (Repo-Leerung pushen)

Damit ein spaeterer Restore nie auf zwei verschiedene volle Versionen trifft, nach erfolgreichem
Einlesen beide Backup-Dateien leeren UND die Diff-Datei (falls vorhanden) GANZ loeschen:

```powershell
Clear-Content "$HOME/.claude/session-opencode-backup.md" -ErrorAction SilentlyContinue
Clear-Content "$HOME/proggs/.claude/session-opencode-backup.md" -ErrorAction SilentlyContinue
Remove-Item "$HOME/.claude/session-opencode-backup.diff","$HOME/proggs/.claude/session-opencode-backup.diff" -Force -ErrorAction SilentlyContinue
Set-Location "$HOME/proggs"
git add .claude/session-opencode-backup.md
git add .claude/session-opencode-backup.diff 2>$null
git commit -m "#NNN - session-opencode restore: clear handoff backup"
git fetch origin
git rebase origin/main
git push
```

So ist der Zustand nach dem Restore sauber: kein aktives Backup, keine ausgelagerte Diff-Datei.

---

## Handoff-Template

Schreibe die Notiz nach genau dieser Reihenfolge. Was die frische Session zuerst braucht, steht
oben. Halte jeden Abschnitt konkret — keine Floskeln.

```markdown
# Session Handoff (OpenCode) — [Datum + Uhrzeit]

## Ziel (1-3 Saetze)
Was soll in dieser Arbeitsphase insgesamt erreicht werden?

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt (WICHTIGSTER ABSCHNITT)
> IMMER fuellen, wenn beim Backup noch eine Aufgabe lief oder per ESC unterbrochen wurde. Ziel: Die
> frische Session mit NULL Vorkontext nimmt exakt diesen einen Schritt wieder auf. Lieber zu
> ausfuehrlich als zu knapp.
- **Welche Aufgabe lief gerade:** [die konkrete Aufgabe woertlich]
- **Wo genau unterbrochen — der allerletzte Schritt:** [exakter Schritt: Datei + Zeile/Funktion +
  was genau dort getan wurde]
- **Schon erledigter Teil DIESES Schritts:** [was bereits getan/geschrieben/committed ist]
- **Noch offener Teil DIESES Schritts:** [was an genau diesem Schritt noch fehlt]
- **So geht es EXAKT weiter (allererste Aktion der neuen Session):** [konkrete erste Aktion — welche
  Datei, welche Stelle, welcher Befehl/Edit; so praezise, dass sie ohne Nachdenken ausfuehrbar ist]
- **Uncommitteter Arbeitsstand (halbfertige Edits):** [`git status --short` der eigenen Dateien +
  relevanter `git diff`-Ausschnitt; bei riesigem Diff: in `.claude/session-opencode-backup.diff`
  ausgelagert — hier nur Pfad + entscheidende Stellen nennen]

(Lief beim Backup KEINE Aufgabe: hier nur "Keine laufende Aufgabe, letzter Stand sauber abgeschlossen".)

## Letzte Aufgaben & Ergebnisse (chronologisch, WICHTIG)
> Der zweitwichtigste Abschnitt. Die letzten erledigten Aufgaben dieser Session — NEUESTE ZUERST —
> jeweils mit ihrem konkreten Ergebnis. So weiss die frische Session sofort, was zuletzt passiert ist
> und was dabei herauskam, ohne den Gespraechsverlauf zu kennen.
1. [Letzte Aufgabe] → Ergebnis: [was konkret herauskam, inkl. Commit-Nummer/Datei falls relevant]
2. [Vorletzte Aufgabe] → Ergebnis: [...]
3. [usw. — die letzten ~3-6 Aufgaben dieser Session]

## Offene & gestellte Fragen (WICHTIG)
> Jede Frage festhalten, die in dieser Session offen blieb — egal ob der Benutzer dich oder du den
> Benutzer gefragt hast. Mit Antwort, falls sie schon kam; sonst als offen markieren. So geht keine
> Ruecksprache verloren.
- [Frage] — [Antwort, falls vorhanden / sonst "noch offen, wartet auf Benutzer"]

## Vorab-Informationen fuer die naechste Session (WICHTIG)
> Alles, was Frank beim Wiedereinstieg wissen muss, BEVOR er die naechste Entscheidung trifft:
> Kontext aus der Session, Randbedingungen, Warnungen, relevante Zahlen, Live-Zustaende,
> Benutzerpraeferenzen, Annahmen und Dinge, die im Chat besprochen wurden, aber nicht direkt Code sind.
- [Kontext/Warnung/Info] — [warum sie fuer die Fortsetzung wichtig ist]

## Aktueller Status
- Erledigt: [was fertig ist, mit Commit-Nummer falls relevant]
- In Arbeit: [was begonnen, aber nicht abgeschlossen ist]
- Blockiert: [was wartet und warum]

## Relevante Dateien
- `pfad/zu/datei` — kurze Erklaerung warum relevant

## Getroffene Entscheidungen
- [Entscheidung] — [Begruendung, damit die naechste Session nicht zurueckrudert]

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- [Ansatz X] ist gescheitert weil [Grund] — NICHT noch einmal versuchen

## Wichtige Recherche-Ergebnisse
- [Kernfakt, der sofort relevant ist, mit Quelle falls vorhanden]

## Plaene & moegliche Weiterarbeit (WICHTIG)
> Nicht nur harte To-dos. Auch besprochene Richtungen, spaetere Ausbaustufen, Ideen, verworfene
> Alternativen und sinnvolle Anschlussarbeiten festhalten. Ziel: Nach dem Restore kann Frank sofort
> entscheiden, ob er dort weitermachen will.
1. [Fortsetzungsoption/Plan] — [Nutzen + erster konkreter Schritt]
2. [Weitere Option] — [Nutzen + erster konkreter Schritt]

## Naechste Schritte (priorisiert)
1. [Konkrete erste Aktion — so praezise, dass sie sofort ausfuehrbar ist]
2. ...

## Anker
- Branch: [aktueller Branch]
- Letzte Commits:
[Ausgabe von: git -C "$HOME/proggs" log --oneline -5]
```

Den `git log --oneline -5`-Block tatsaechlich einfuegen — er gibt der frischen Session sofort den
Repo-Stand.

---

## Verhaltensregeln

- **Du schreibst die Notiz, nicht der Benutzer.** Er sagt nur "backup" — die Kuration ist deine
  Aufgabe. Das spart Zeit und verhindert Luecken.
- **Eine Datei je Ort, immer ueberschreiben — niemals anhaengen.**
- **restore ist Pflicht-erster-Schritt nach `/new`.** Erst lesen, dann arbeiten.
- **Bei parallelen Sessions:** Das Repo-Backup ist eine gemeinsame Datei. Beim Stagen NUR die
  Backup-Datei namentlich nehmen, nie `git add -A` (fremde In-Flight-Dateien anderer Sessions).
- **Besondere Gruendlichkeit (Frank-Wunsch):** Die Abschnitte "Letzte Aufgaben & Ergebnisse",
  "Offene & gestellte Fragen", "Vorab-Informationen" und "Plaene & moegliche Weiterarbeit" immer
  sorgfaeltig fuellen. Restore soll Frank nicht nur technisch fortsetzen lassen, sondern ihm sofort
  erklaeren, wo ihr zuletzt wart und welche Richtung als Naechstes sinnvoll ist.
