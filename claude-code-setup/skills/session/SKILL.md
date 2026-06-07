---
name: session
description: >-
  Sessionuebergreifendes Backup und Restore des Arbeitskontexts. Nutze diesen Skill IMMER wenn
  der Benutzer eine dieser EXAKTEN Drei-Wort-Phrasen sagt: "starte session backup" (auch
  "Session Backup starten", "starte Session-Backup") -> Modus BACKUP; "starte session restore"
  (auch "Session Restore starten", "starte Session-Restore") -> Modus RESTORE. Diese genauen
  drei Woerter REICHEN aus — ein "starte den Skill" davor ist NICHT noetig. Ebenso ausloesen bei:
  "session backup", "starte den Skill Session Backup", "starte den Session Backup Skill", "mach
  ein Session-Backup", "sichere die Session", "sichere den Stand fuer den Neustart", "Backup vor
  Clear", "session restore", "starte den Skill Session Restore", "starte den Session Restore
  Skill", "lade das Session-Backup", "stelle die Session wieder her", oder "mache weiter wo wir
  waren" direkt nach einem /clear. Wird AUSSERDEM automatisch ausgeloest, wenn der Stop-Hook
  session-backup-nudge (Stop) ODER session-backup-midturn (PostToolUse, mitten im Turn) bei
  hohem Kontextverbrauch (ab 80%) anstoesst, ein Backup zu machen.
  backup erstellt eine kuratierte Handoff-Notiz und sichert sie lokal UND im Repo, bevor der
  Benutzer /clear eingibt. restore liest die neueste Notiz und setzt die Arbeit nahtlos fort.
---

# Session — Backup & Restore

## Warum es diesen Skill gibt

Wenn das Kontextfenster voll wird, komprimiert Claude Code automatisch — und verliert dabei
unkontrolliert Details. Besser ist ein **kontrollierter Schnitt**: kurz vor der Grenze eine
saubere, von dir (Claude) kuratierte Uebergabe-Notiz schreiben, dann `/clear`, dann die Notiz
wieder einlesen. So bestimmst DU, was erhalten bleibt, statt es der Automatik zu ueberlassen.

Was ein `/clear` ueberlebt, sind **Dateien** — nicht der Gespraechsverlauf. Genau darauf baut
dieser Skill: Das Backup ist eine Datei, die nach dem `/clear` wieder gelesen wird.

## Unterbefehl erkennen

Der Benutzer (oder der Hook) nennt einen von zwei Modi. Erkenne ihn aus der Anfrage:

| Anfrage enthaelt … | Modus |
|--------------------|-------|
| "backup", "sichern", "sichere den Stand", Hook-Anstoss bei hohem Kontext | **BACKUP** |
| "restore", "wiederherstellen", "lade das Backup", "mache weiter wo wir waren" | **RESTORE** |

Im Zweifel kurz nachfragen, welcher der beiden gemeint ist.

## Pfade (plattformuebergreifend)

```
LOKAL : $HOME/.claude/session-backup.md
REPO  : $HOME/proggs/.claude/session-backup.md
```

`$HOME` loest auf beiden Plattformen korrekt auf (Windows: `C:\Users\barwa`, macOS: `/Users/barwa`).
Es gibt bewusst nur **eine** feste Datei je Ort — sie wird bei jedem Backup ueberschrieben, damit
sich nie mehrere Sessions vermischen.

---

## BACKUP-Workflow

> **TEMPO-REGEL (PFLICHT — Backup soll SCHNELL gehen):** Die Backup-Datei existiert IMMER
> (RESTORE leert sie, loescht sie aber nie). Beim Backup ist sie also leer oder veraltet —
> **NIE vorher mit dem Read-Tool lesen und NIE pruefen ob sie voll ist.** Einfach blind
> ueberschreiben. Das Backup wird genau dafuer gestartet: die Datei zu fuellen. Schreibe die
> Notiz DIREKT per Bash (single-quoted Heredoc), NICHT mit dem Write-Tool — das Write-Tool
> erzwingt bei existierenden Dateien ein vorheriges Read und kostet so 2-3 ueberfluessige
> Tool-Calls. Ziel: Schritt 1-3 in moeglichst wenigen Tool-Calls (idealerweise zwei Bash-Aufrufe:
> einer zum Schreiben beider Dateien, einer zum Committen).

Fuehre diese Schritte der Reihe nach aus. Wenn der Hook dich angestossen hat: Pruefe ZUERST, ob die
aktuelle Aufgabe wirklich abgeschlossen ist (keine offene Rueckfrage / kein Multiple-Choice). Wenn
nicht — erst fertig machen, dann Backup. Wenn der Hook anstoesst und die Aufgabe noch laeuft, dem
Benutzer kurz melden: "Kontext bei 80%+, ich sichere nach dieser Antwort."

### Schritt 1: Handoff-Notiz schreiben

Erstelle den Inhalt nach der Struktur unten (Abschnitt "Handoff-Template"). Der wichtigste
Abschnitt ist **Fehlgeschlagene Ansaetze** — ohne ihn wiederholt die frische Session denselben
Fehler. Schreibe so konkret, dass eine Session OHNE jeden Vorkontext sofort weiterarbeiten kann.

### Schritt 2: An beide Orte schreiben — DIREKT per Bash, kein Read, kein Write-Tool

Schreibe die Notiz mit einem **single-quoted Heredoc** direkt in die lokale Datei und kopiere
sie dann ins Repo. Das ueberschreibt zuverlaessig — egal ob die Datei existiert, leer oder
veraltet ist — in EINEM Bash-Aufruf, ohne vorheriges Read:

```bash
mkdir -p "$HOME/proggs/.claude"
cat <<'SESSION_BACKUP_EOF' > "$HOME/.claude/session-backup.md"
# Session Handoff — <Datum + Uhrzeit>
... (komplette Handoff-Notiz nach dem Template unten) ...
SESSION_BACKUP_EOF
cp "$HOME/.claude/session-backup.md" "$HOME/proggs/.claude/session-backup.md"
```

**Warum so:**
- **Single-quoted Delimiter (`'SESSION_BACKUP_EOF'`)** → der Inhalt wird LITERAL geschrieben:
  keine Shell-Expansion von `$HOME`, `%1$s`, Backslashes (`C:\Users`) oder Backticks. Genau
  das, was die Notiz braucht (sie enthaelt oft Pfade, Platzhalter, Commit-Hashes).
- **Ungewoehnlicher Delimiter** statt `EOF` → verhindert versehentliches fruehes Ende, falls
  die Notiz selbst mal das Wort "EOF" enthaelt.
- **`cp` statt zweitem Heredoc** → eine Quelle, kein Risiko dass die beiden Dateien abweichen.
- **KEIN Write-Tool** → das hat bei existierenden Dateien einen Read-Zwang erzwungen (2-3
  ueberfluessige Tool-Calls). Hier nicht noetig.

### Schritt 3: Repo-Version committen und pushen

Nur die Backup-Datei stagen (namentlich — nie `git add -A`, wegen paralleler Sessions), dann
fetch + rebase + push:

```bash
cd "$HOME/proggs"
git add .claude/session-backup.md
git commit -m "#NNN - session backup: handoff snapshot"
git fetch origin && git rebase origin/main && git push
```

Die fortlaufende Commit-Nummer wie ueblich aus dem letzten Commit ableiten.

**Konflikt auf der Backup-Datei** (eine parallele Session hat sie auch committed): Dein eigenes,
frisches Backup gewinnt, denn es gehoert zu DEINER Session-Fortsetzung. ACHTUNG: bei `git rebase`
sind `--ours`/`--theirs` invertiert. `--theirs` ist dein gerade angewendeter Commit (den willst
du), `--ours` ist origin/main. Bei Konflikt auf der Backup-Datei also:

```bash
git checkout --theirs .claude/session-backup.md
git add .claude/session-backup.md
git rebase --continue && git push
```

### Schritt 4: Disketten-Marker zeigen

Gib EXAKT dieses Format aus (Linien sind je 80 Zeichen `━`):

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💾💾💾 Session-Backup erstellt (lokal + Repo) !!!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

So sieht der Benutzer auf einen Blick: Das Backup wurde wirklich gemacht.

### Schritt 5: Fette "JETZT neue Session"-Warnung

Direkt nach dem Marker, als eigener kraeftiger Block:

```
⚠️ **JETZT NEUE SESSION STARTEN!** Tippe `/clear` und sage danach "session restore".
Nicht weiterarbeiten — sonst drohen Kontextgrenze und ein Kontext, der nicht zum Backup passt.
```

Warum so deutlich: Wenn der Benutzer weiterarbeitet, riskiert er (1) die echte Kontextgrenze bei
langen Aufgaben und (2) einen Arbeitskontext, der vom gespeicherten Backup abweicht. Der Schnitt
muss JETZT passieren, solange Backup und Kontext noch uebereinstimmen.

---

## RESTORE-Workflow

### Schritt 1: Beide Orte pruefen, neuere nicht-leere Version waehlen

Lies beide Dateien. Regeln:
- Datei fehlt oder ist leer (nur Whitespace) → zaehlt als "kein Backup".
- Beide vorhanden und nicht leer → nimm die mit dem **neueren Timestamp** (deckt den Fall
  Windows→macOS ab: das frisch gepullte Repo-Backup gewinnt).
- Nur eine vorhanden → diese nehmen.
- Keine vorhanden → dem Benutzer sagen, dass es kein Backup gibt, und normal weiterarbeiten.

Vor dem Lesen des Repo-Backups einmal `git pull` (bzw. fetch+rebase), damit ein Backup von einem
anderen Rechner wirklich aktuell ist.

Timestamp-Vergleich (Beispiel — Bash-Snippet, auf Windows in Git Bash ausfuehren; `-s`/`-nt` sind POSIX-Test-Operatoren, kein PowerShell):

```bash
LOCAL="$HOME/.claude/session-backup.md"
REPO="$HOME/proggs/.claude/session-backup.md"
newer=""
for f in "$LOCAL" "$REPO"; do
    [ -s "$f" ] || continue                      # -s: existiert und nicht leer
    if [ -z "$newer" ] || [ "$f" -nt "$newer" ]; then newer="$f"; fi
done
[ -n "$newer" ] && echo "Neuestes Backup: $newer" || echo "Kein Backup vorhanden"
```

### Schritt 2: Kontext laden und fortsetzen

Lies die gewaehlte Datei vollstaendig, verinnerliche Ziel, Status, naechste Schritte und
besonders die fehlgeschlagenen Ansaetze. Fasse dem Benutzer in 3-4 Saetzen zusammen, wo ihr
steht, und mach dann mit dem ersten "Naechsten Schritt" weiter.

### Schritt 3: Beide Backups leeren (und Repo-Leerung pushen)

Damit ein spaeterer Restore nie auf zwei verschiedene volle Versionen trifft, werden nach
erfolgreichem Einlesen BEIDE Dateien geleert:

```bash
: > "$HOME/.claude/session-backup.md"
: > "$HOME/proggs/.claude/session-backup.md"
cd "$HOME/proggs"
git add .claude/session-backup.md
git commit -m "#NNN - session restore: clear handoff backup"
git fetch origin && git rebase origin/main && git push
```

So ist der Zustand nach dem Restore sauber: kein aktives Backup, weder lokal noch im Repo.

---

## Handoff-Template

Schreibe die Notiz nach genau dieser Reihenfolge. Was die frische Session zuerst braucht, steht
oben. Halte jeden Abschnitt konkret — keine Floskeln.

```markdown
# Session Handoff — [Datum + Uhrzeit]

## Ziel (1-3 Saetze)
Was soll in dieser Arbeitsphase insgesamt erreicht werden?

## Aktueller Status
- Erledigt: [was fertig ist, mit Commit-Nummer falls relevant]
- In Arbeit: [was begonnen, aber nicht abgeschlossen ist]
- Blockiert: [was wartet und warum]

## Relevante Dateien
- `pfad/zu/datei` — kurze Erklaerung warum relevant
- ...

## Getroffene Entscheidungen
- [Entscheidung] — [Begruendung, damit die naechste Session nicht zurueckrudert]

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- [Ansatz X] ist gescheitert weil [Grund] — NICHT noch einmal versuchen

## Wichtige Recherche-Ergebnisse
- [Kernfakt, der sofort relevant ist, mit Quelle falls vorhanden]

## Naechste Schritte (priorisiert)
1. [Konkrete erste Aktion — so praezise, dass sie sofort ausfuehrbar ist]
2. ...

## Offene Fragen
- [Was noch ungeklaert ist / worauf der Benutzer antworten muss]

## Anker
- Branch: [aktueller Branch]
- Letzte Commits:
[Ausgabe von: git log --oneline -5]
```

Den `git log --oneline -5`-Block tatsaechlich einfuegen — er gibt der frischen Session sofort den
Repo-Stand.

---

## Verhaltensregeln

- **Du schreibst die Notiz, nicht der Benutzer.** Er sagt nur "backup" — die Kuration ist deine
  Aufgabe. Das spart Zeit und verhindert Luecken.
- **Eine Datei je Ort, immer ueberschreiben — niemals anhaengen** (Begruendung siehe Abschnitt "Pfade").
- **restore ist Pflicht-erster-Schritt nach /clear.** Nicht optional behandeln: erst lesen, dann
  arbeiten.
- **Bei parallelen Sessions:** Das Repo-Backup ist eine gemeinsame Datei. Wenn mehrere Sessions
  gleichzeitig sichern wuerden, ueberschreiben sie sich — in der Praxis macht aber nur die Session
  ein Backup, die gerade an die Grenze kommt. Beim Stagen NUR die Backup-Datei namentlich nehmen.
