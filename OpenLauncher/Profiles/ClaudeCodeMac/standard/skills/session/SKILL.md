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
  waren" direkt nach einem /clear.
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

`$HOME` loest auf beiden Plattformen korrekt auf (Windows: `/Users/frank`, macOS: `/Users/barwa`).
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
> Tool-Calls. Ziel: die Schreib-Schritte in moeglichst wenigen Tool-Calls (idealerweise: bei
> laufender/unterbrochener Aufgabe einer zum Erfassen des uncommitteten Stands (`status`/`diff`),
> bei grossem Diff einer zum Auslagern (Schritt 2b), einer zum Schreiben beider Dateien, einer zum
> Committen. Ist beim Backup nichts offen, entfaellt das Erfassen/Auslagern).

Fuehre diese Schritte der Reihe nach aus.

> **WICHTIG — Backup MITTEN in einer laufenden Aufgabe ist der Normalfall:** Der Benutzer macht das
> Backup oft BEWUSST, waehrend eine lange Aufgabe noch laeuft — er drueckt `ESC` (die Aufgabe wird
> mit "[Request interrupted]" unterbrochen) und sagt dann "session backup". Dann NICHT "erst fertig
> machen". Stattdessen ist deine WICHTIGSTE Aufgabe, den **exakten Unterbrechungspunkt** im
> Pflicht-Abschnitt "Laufende/unterbrochene Aufgabe" des Handoff-Templates festzuhalten — so genau,
> dass die frische Session ohne jeden Kontext exakt diesen einen Schritt wieder aufnimmt. Nur wenn
> gerade eine echte Rueckfrage an den Benutzer offen ist (Multiple-Choice), diese kurz mitfesthalten
> statt zu raten.

### Schritt 1: Handoff-Notiz schreiben

Erstelle den Inhalt nach der Struktur unten (Abschnitt "Handoff-Template"). Die zwei wichtigsten
Abschnitte sind **Laufende/unterbrochene Aufgabe** (der exakte Wiedereinstiegspunkt — wo zuletzt
gearbeitet wurde) und **Fehlgeschlagene Ansaetze** (ohne ihn wiederholt die frische Session
denselben Fehler). Schreibe so konkret, dass eine Session OHNE jeden Vorkontext sofort an genau der
richtigen Stelle weiterarbeiten kann — an exakt dem Schritt, der zuletzt lief, nicht "ungefaehr da".

**Uncommitteten Stand zuerst erfassen (PFLICHT bei laufender/unterbrochener Aufgabe):** Bevor du die
Notiz schreibst, EINMAL den nicht-committeten Arbeitsstand abfragen:

```bash
git -C "$HOME/proggs" status --short                          # M = geaendert, ?? = NEU (untracked)
git -C "$HOME/proggs" diff -- <eigene-geaenderte-dateien>     # Diff der GETRACKTEN Aenderungen
git -C "$HOME/proggs" diff --shortstat -- <eigene-dateien>    # Groesse messen (X files, Y insertions)
# WICHTIG: `git diff` zeigt NEUE (??) Dateien NICHT — ihren Inhalt je Datei separat erfassen:
git -C "$HOME/proggs" diff --no-index /dev/null <neue-datei>  # zeigt den vollen Inhalt als Diff
```

Dieser Output gehoert in den Wiedereinstiegspunkt (Feld "Uncommitteter Arbeitsstand"), damit die
frische Session den halbfertigen, noch NICHT committeten Edit sieht. **`git diff` allein hat ein
Loch: neue (untracked) Dateien fehlen** — `status --short` zeigt nur ihren Namen (`??`), deshalb je
neue Datei `git diff --no-index /dev/null <datei>` (nicht-invasiv, ruehrt den Index nicht an). NUR
die eigenen/relevanten Dateien, keine fremden Parallel-Session-Dateien. **Keine Secrets** uebernehmen
(API-Keys/Tokens aus einem halbfertigen Edit rausredaktieren — die Datei kann ins Repo wandern).
Groesse per `--shortstat` beurteilen: bei grossem Diff (Richtwert ~120+ geaenderte Zeilen oder
mehrere Dateien) NICHT inline, sondern in `$HOME/.claude/session-backup.diff` auslagern (Schritt 2b)
und im Wiedereinstiegspunkt nur Pfad + entscheidende Stellen referenzieren. Kleiner Diff: ruhig inline.

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

**SOFORT nach dem Schreiben verifizieren (PFLICHT — kein stiller Datenverlust):** Ein
fehlgeschlagenes Heredoc (Delimiter taucht im Text auf, volle Platte, abgebrochener Schreibvorgang)
wuerde ein LEERES oder abgeschnittenes Backup hinterlassen — und nach `/clear` waere die Arbeit
unwiederbringlich weg. Darum BEVOR irgendein Erfolgs-Marker kommt kurz pruefen, dass BEIDE Dateien
plausibel gefuellt sind:

```bash
for f in "$HOME/.claude/session-backup.md" "$HOME/proggs/.claude/session-backup.md"; do
  if [ ! -s "$f" ] || ! grep -q "Session Handoff" "$f"; then echo "BACKUP KAPUTT: $f"; fi
  echo "$f: $(wc -l < "$f") Zeilen"
done
```

Schlaegt die Pruefung an (leer / Ueberschrift "Session Handoff" fehlt / verdaechtig wenige Zeilen):
NICHT den Erstellt-Marker zeigen und NICHT `/clear` empfehlen — dem Benutzer melden und das Schreiben
wiederholen. Erst wenn beide Dateien plausibel gefuellt sind, weiter.

### Schritt 2b: Grossen uncommitteten Diff auslagern (NUR wenn der Diff gross ist)

Ist der uncommittete Diff zu gross fuer die Notiz (Richtwert ~120+ Zeilen / mehrere Dateien),
schreibe ihn lossless in eine SEPARATE, FESTE Datei (immer ueberschrieben, damit sich nie mehrere
ansammeln) und referenziere ihn im Wiedereinstiegspunkt nur per Pfad:

```bash
# Getrackte Aenderungen in die Diff-Datei schreiben ...
git -C "$HOME/proggs" diff -- <eigene-dateien-der-aufgabe> > "$HOME/.claude/session-backup.diff"
# ... und neue (??) Dateien anhaengen (git diff allein erfasst sie nicht):
for f in <neue-dateien>; do
  git -C "$HOME/proggs" diff --no-index /dev/null "$f" >> "$HOME/.claude/session-backup.diff"
done
cp "$HOME/.claude/session-backup.diff" "$HOME/proggs/.claude/session-backup.diff"
```

Kleiner Diff → diesen Schritt UEBERSPRINGEN, KEINE Diff-Datei anlegen. Die Diff-Datei wird beim
RESTORE nach dem Einlesen wieder GELOESCHT (nicht nur geleert) — siehe RESTORE Schritt 4.

### Schritt 3: Repo-Version committen und pushen

Nur die Backup-Datei stagen (namentlich — nie `git add -A`, wegen paralleler Sessions), dann
fetch + rebase + push:

```bash
cd "$HOME/proggs"
git add .claude/session-backup.md
# Falls in Schritt 2b ausgelagert, die Diff-Datei mit-stagen:
[ -f .claude/session-backup.diff ] && git add .claude/session-backup.diff
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

NUR wenn die Verifikation (Ende Schritt 2) bestanden hat — bei kaputtem/leerem Backup stattdessen
warnen und das Schreiben wiederholen, NICHT diesen Marker zeigen. Gib EXAKT dieses Format aus
(Linien sind je 80 Zeichen `━`):

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

> **Gleicher PC, direkt nach `/clear` (der Normalfall):** Backup → `/clear` → restore laeuft in der
> Regel sofort am SELBEN Rechner. Das heisst: der uncommittete Arbeitsstand (halbfertige Edits UND
> neue Dateien) liegt nach `/clear` UNVERAENDERT noch im Working Tree. Du kannst ihn jederzeit direkt
> einsehen (`git -C "$HOME/proggs" status --short` + `git diff`) statt dich allein auf die Notiz zu
> verlassen — der echte Code-Stand ist real auf der Platte. Die Backup-Notiz (+ ggf. ausgelagerte
> `.diff`) ist die kuratierte Zusammenfassung und Absicherung, NICHT die einzige Quelle.

### Schritt 1: Beide Orte pruefen, neuere nicht-leere Version waehlen

Lies beide Dateien. Regeln:
- Datei fehlt oder ist leer (nur Whitespace) → zaehlt als "kein Backup".
- Beide vorhanden und nicht leer → am selben PC sind sie i.d.R. identisch (die lokale wird beim
  Backup ins Repo kopiert); nimm die mit dem **neueren Timestamp** (normalerweise die lokale).
- Nur eine vorhanden → diese nehmen.
- Keine vorhanden → dem Benutzer sagen, dass es kein Backup gibt, und normal weiterarbeiten.

(Ein `git fetch origin && git rebase origin/main` ist wegen paralleler Sessions ohnehin sinnvoll,
bevor in Schritt 4 die Leerung gepusht wird — fuer das Backup-Lesen selbst reicht am selben PC aber
die lokale Datei.)

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

### Schritt 2: Drift-Pruefung — passt der Stand noch zum Backup?

Backup → `/clear` → restore laeuft meist in Sekunden am selben PC — da ist Drift selten. Aber
parallele Claude-Sessions am selben Repo koennen zwischendurch gepusht oder Dateien geaendert haben.
Darum kurz pruefen, ob sich seit dem Backup etwas verschoben hat, bevor du weiterarbeitest:

- **Genannte Dateien noch da?** Existieren die unter "Relevante Dateien" und im Wiedereinstiegspunkt
  genannten Pfade noch? (Fehlt eine, wurde sie evtl. verschoben/geloescht/umbenannt.)
- **git-Stand passt?** `git -C "$HOME/proggs" log --oneline -3` und den aktuellen Branch
  (`git -C "$HOME/proggs" branch --show-current`) mit dem "Anker"-Block im Backup vergleichen. Sind
  seither viele fremde Commits dazugekommen oder ist der Branch ein anderer, kann der
  Wiedereinstiegspunkt veraltet sein.
- **Uncommitteter Stand noch da?** Falls das Backup einen "Uncommitteter Arbeitsstand" enthielt:
  ist der beschriebene halbfertige Edit noch im Working Tree (`git -C "$HOME/proggs" status --short`),
  oder wurde er zwischenzeitlich committed/verworfen?

Bei erkanntem Drift NICHT einfach weitermachen: dem Benutzer in 1-2 Saetzen melden, was abweicht
(z.B. "Datei X fehlt jetzt" / "12 fremde Commits seit dem Backup" / "der halbfertige Edit ist nicht
mehr im Working Tree"), und kurz abstimmen, wie fortgesetzt wird. Passt alles, direkt weiter mit dem
naechsten Schritt.

### Schritt 3: Kontext laden und fortsetzen

Lies die gewaehlte Datei vollstaendig, verinnerliche Ziel, Status, naechste Schritte und
besonders die fehlgeschlagenen Ansaetze. Verweist das Backup auf eine ausgelagerte Diff-Datei
(`.claude/session-backup.diff`), lies diese ebenfalls vollstaendig — sie enthaelt den uncommitteten
Zwischenstand des unterbrochenen Schritts. **Ist der Abschnitt "Laufende/unterbrochene Aufgabe"
gefuellt, hat er VORRANG:** nimm den dort beschriebenen, zuletzt unterbrochenen Schritt sauber neu
auf und fuehre ihn zu Ende, BEVOR du zu den allgemeinen "Naechsten Schritten" uebergehst. Fasse dem
Benutzer in 3-4 Saetzen zusammen, wo ihr steht (inkl. an welchem Schritt zuletzt unterbrochen
wurde), und mach dann genau dort weiter. Ist der Abschnitt leer/nicht vorhanden, beginne mit dem
ersten "Naechsten Schritt".

### Schritt 4: Backups leeren + ausgelagerte Diff-Datei loeschen (Repo-Leerung pushen)

Damit ein spaeterer Restore nie auf zwei verschiedene volle Versionen trifft, werden nach
erfolgreichem Einlesen die beiden Backup-Dateien geleert UND die ausgelagerte Diff-Datei (falls
vorhanden) GANZ geloescht — nur leeren wuerde bei grossen Diffs ueber die Zeit Dateimuell ansammeln:

```bash
: > "$HOME/.claude/session-backup.md"
: > "$HOME/proggs/.claude/session-backup.md"
# Ausgelagerte Diff-Datei (falls vorhanden) NACH dem Einlesen GANZ loeschen, nicht nur leeren:
rm -f "$HOME/.claude/session-backup.diff" "$HOME/proggs/.claude/session-backup.diff"
cd "$HOME/proggs"
git add .claude/session-backup.md
# Loeschung der Diff-Datei mit-stagen, falls sie im Repo getrackt war (sonst harmlos):
git add .claude/session-backup.diff 2>/dev/null || true
git commit -m "#NNN - session restore: clear handoff backup"
git fetch origin && git rebase origin/main && git push
```

So ist der Zustand nach dem Restore sauber: kein aktives Backup und keine ausgelagerte Diff-Datei,
weder lokal noch im Repo.

---

## Handoff-Template

Schreibe die Notiz nach genau dieser Reihenfolge. Was die frische Session zuerst braucht, steht
oben. Halte jeden Abschnitt konkret — keine Floskeln.

```markdown
# Session Handoff — [Datum + Uhrzeit]

## Ziel (1-3 Saetze)
Was soll in dieser Arbeitsphase insgesamt erreicht werden?

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt (WICHTIGSTER ABSCHNITT)
> IMMER fuellen, wenn beim Backup noch eine Aufgabe lief oder per `ESC` unterbrochen wurde
> ("[Request interrupted]"). Ziel: Die frische Session mit NULL Vorkontext nimmt exakt diesen
> einen Schritt wieder auf und macht nahtlos weiter — nicht "ungefaehr da", sondern an genau der
> Stelle, die zuletzt lief. Lieber zu ausfuehrlich als zu knapp.
- **Welche Aufgabe lief gerade:** [die konkrete Aufgabe woertlich — was der Benutzer wollte]
- **Wo genau unterbrochen — der allerletzte Schritt:** [der exakte Schritt, der lief, als `ESC`
  kam bzw. als gesichert wurde. So konkret wie moeglich: Datei + Zeile/Funktion + was genau dort
  getan wurde, z.B. "gerade dabei, in `DashboardScreen.kt` ab Zeile 412 den PDF-Export-Button
  einzufuegen, Edit war noch nicht gespeichert"]
- **Schon erledigter Teil DIESES Schritts:** [was vom letzten Schritt bereits getan/geschrieben/
  committed ist — damit es nicht doppelt gemacht wird]
- **Noch offener Teil DIESES Schritts:** [was an genau diesem Schritt noch fehlt, bis er fertig ist]
- **So geht es EXAKT weiter (allererste Aktion der neuen Session):** [die konkrete erste Aktion —
  welche Datei oeffnen, welche Stelle/Zeile, welcher Befehl, welcher Edit. So praezise, dass sie
  ohne Nachdenken ausfuehrbar ist. Den unterbrochenen Schritt sauber NEU ansetzen und zu Ende fuehren]
- **Was dafuer alles vorhanden sein muss:** [Zwischenstaende, Variablenwerte, Pfade, Befehle,
  Build-/Test-Stand, offene Terminal-Ausgaben — alles, was zum nahtlosen Weitermachen gebraucht
  wird. Die neue Session kennt NICHTS davon, also nichts weglassen]
- **Uncommitteter Arbeitsstand (halbfertige Edits):** [Output von `git status --short` der eigenen
  Dateien + fuer die Datei(en) des unterbrochenen Schritts der relevante `git diff`-Ausschnitt — so
  sieht die frische Session den noch nicht gespeicherten/committeten Zwischenstand, nicht nur den
  letzten Commit. Bei riesigem Diff: in `.claude/session-backup.diff` ausgelagert (Backup Schritt 2b)
  — hier dann nur den Pfad + die entscheidenden Stellen nennen]
- **Danach:** [kurzer Verweis: nach Abschluss dieses Schritts mit "Naechste Schritte" weiter]

(Lief beim Backup KEINE Aufgabe — alles sauber abgeschlossen —, hier nur "Keine laufende Aufgabe,
letzter Stand sauber abgeschlossen" schreiben.)

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
