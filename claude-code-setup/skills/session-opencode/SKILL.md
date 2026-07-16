---
name: session-opencode
description: 'Plattformuebergreifende OpenCode-Variante des Session-Backup/Restore (Windows PowerShell, macOS/Linux Bash), mit EIGENEN Dateien getrennt vom Claude-Code-Backup. Nutze IMMER in OpenCode, wenn der Benutzer sagt: "session opencode backup", "starte session opencode backup", "sichere die opencode session", "opencode session sichern", "backup vor /new", "sichere den opencode stand" -> Modus BACKUP; "session opencode restore", "starte session opencode restore", "opencode session wiederherstellen", "lade das opencode backup", "mache in opencode weiter wo wir waren" (direkt nach /new) -> Modus RESTORE. BACKUP schreibt eine sehr gruendliche Uebergabe-Notiz (Ziel, letzte Aufgaben + Ergebnisse, offene Fragen, Vorabinfos, Plaene, uncommitteter Stand, naechste Schritte) lokal UND ins Repo; RESTORE synchronisiert zuerst das Repo, liest die neueste Notiz, sagt klar wo zuletzt gearbeitet wurde und welche Fortsetzungen sinnvoll sind, dann setzt es nahtlos fort. Speziell fuer OpenCode — NICHT mit dem Claude-Code-"session"-Skill verwechseln (eigene Backup-Dateien).'
---

# Session-OpenCode — Backup & Restore (Windows + macOS/Linux)

Stand: v2.0.0 - 16.07.2026, 14:21 Uhr

## Warum es diesen Skill gibt

Wenn das Kontextfenster einer OpenCode-Session voll wird, geht beim Komprimieren unkontrolliert
Detail verloren. Besser ist ein **kontrollierter Schnitt**: kurz vor der Grenze eine saubere, von
dir (dem Agenten) kuratierte Uebergabe-Notiz schreiben, dann eine **neue Session** (`/new`), dann
die Notiz wieder einlesen. So bestimmst DU, was erhalten bleibt, statt es der Automatik zu ueberlassen.

Was ein `/new` ueberlebt, sind **Dateien** — nicht der Gespraechsverlauf. Genau darauf baut dieser
Skill: Das Backup ist eine Datei, die nach dem `/new` wieder gelesen wird.

**Dies ist die OpenCode-Fassung.** Zwei Unterschiede zum Claude-Code-`session`-Skill, beide wichtig:
1. **Plattformeigene Shell.** Unter Windows gilt PowerShell (`pwsh`), unter macOS/Linux Bash.
   Verwende immer nur den passend beschrifteten Befehlsblock. Der OpenCode-Installer setzt
   `shell=pwsh` auf Windows und `shell=bash` auf macOS/Linux.
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

`$HOME` loest in PowerShell und Bash korrekt auf (Windows z.B. `C:\Users\barwa`). Es gibt bewusst
nur **eine** feste Datei je Ort — sie wird bei jedem Backup ueberschrieben, damit sich nie mehrere
Sessions vermischen. Diese Pfade sind ANDERS als beim Claude-Code-Skill (`session-backup.md`) —
bewusst getrennt.

---

## BACKUP-Workflow

> **TEMPO-REGEL (Backup soll SCHNELL gehen):** Die Backup-Datei existiert nach einem frueheren
> Restore leer oder veraltet — **NICHT vorher mit dem Read-Tool lesen, nicht pruefen ob sie voll
> ist.** Einfach blind ueberschreiben (genau dafuer wird das Backup gestartet). Schreibe die Notiz
> DIREKT mit dem literalen Block der plattformeigenen Shell, NICHT mit dem Write/Edit-Tool — diese
> Datei-Tools erzwingen bei existierenden Dateien ein vorheriges Read und kosten ueberfluessige Schritte.

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

### Schritt 2: An beide Orte schreiben — mit der plattformeigenen Shell

Schreibe die Notiz mit einem literalen Block (`@'…'@` in PowerShell, quoted Heredoc in Bash) in die
lokale Datei und kopiere sie dann ins Repo. Das ueberschreibt zuverlaessig, egal ob die Datei
existiert, leer oder veraltet ist:

**Windows (`pwsh`):**

```powershell
New-Item -ItemType Directory -Force "$HOME/.claude","$HOME/proggs/.claude" | Out-Null
@'
# Session Handoff (OpenCode) — <Datum + Uhrzeit>
... (komplette Handoff-Notiz nach dem Template unten) ...
'@ | Set-Content -Path "$HOME/.claude/session-opencode-backup.md" -Encoding utf8
$repoRoot = "$HOME/proggs"
if ((git -C $repoRoot branch --show-current) -ne 'main') { throw "Session-Backup darf nur vom Branch main nach origin/main schreiben." }
git -C $repoRoot fetch origin
if ($LASTEXITCODE -ne 0) { throw "Fetch fehlgeschlagen; Backup ist nur lokal vorhanden." }
git -C $repoRoot merge --ff-only origin/main
if ($LASTEXITCODE -ne 0) { throw "Kein sicherer Fast-Forward moeglich; Backup ist nur lokal vorhanden." }
Copy-Item "$HOME/.claude/session-opencode-backup.md" "$HOME/proggs/.claude/session-opencode-backup.md" -Force
```

**macOS/Linux (`bash`):**

```bash
mkdir -p "$HOME/.claude" "$HOME/proggs/.claude"
cat > "$HOME/.claude/session-opencode-backup.md" <<'SESSION_OPENCODE_EOF'
# Session Handoff (OpenCode) — <Datum + Uhrzeit>
... (komplette Handoff-Notiz nach dem Template unten) ...
SESSION_OPENCODE_EOF
repo_root="$HOME/proggs"
[ "$(git -C "$repo_root" branch --show-current)" = main ] || { printf 'Session-Backup darf nur vom Branch main nach origin/main schreiben.\n' >&2; exit 1; }
git -C "$repo_root" fetch origin || { printf 'Fetch fehlgeschlagen; Backup ist nur lokal vorhanden.\n' >&2; exit 1; }
git -C "$repo_root" merge --ff-only origin/main || { printf 'Kein sicherer Fast-Forward moeglich; Backup ist nur lokal vorhanden.\n' >&2; exit 1; }
cp "$HOME/.claude/session-opencode-backup.md" "$HOME/proggs/.claude/session-opencode-backup.md"
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

**Windows (`pwsh`):**

```powershell
$backupOk = $true
foreach ($f in @("$HOME/.claude/session-opencode-backup.md","$HOME/proggs/.claude/session-opencode-backup.md")) {
    if (-not (Test-Path $f) -or ((Get-Item $f).Length -eq 0) -or -not (Select-String -Path $f -Pattern 'Session Handoff' -Quiet)) {
        Write-Output "BACKUP KAPUTT: $f"
        $backupOk = $false
    } else {
        Write-Output ("{0}: {1} Zeilen" -f $f, (Get-Content $f).Count)
    }
}
if (-not $backupOk) { throw "OpenCode-Session-Backup ist unvollstaendig." }
```

**macOS/Linux (`bash`):**

```bash
backup_ok=true
for f in "$HOME/.claude/session-opencode-backup.md" "$HOME/proggs/.claude/session-opencode-backup.md"; do
  if [ ! -s "$f" ] || ! grep -q 'Session Handoff' "$f"; then
    printf 'BACKUP KAPUTT: %s\n' "$f"
    backup_ok=false
  else
    printf '%s: %s Zeilen\n' "$f" "$(wc -l < "$f")"
  fi
done
[ "$backup_ok" = true ] || { printf 'OpenCode-Session-Backup ist unvollstaendig.\n' >&2; exit 1; }
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

**macOS/Linux (`bash`):**

```bash
git -C "$HOME/proggs" diff -- <eigene-dateien-der-aufgabe> > "$HOME/.claude/session-opencode-backup.diff"
for f in <neue-dateien>; do
  printf '=== NEUE DATEI: %s ===\n' "$f" >> "$HOME/.claude/session-opencode-backup.diff"
  cat "$f" >> "$HOME/.claude/session-opencode-backup.diff"
done
cp "$HOME/.claude/session-opencode-backup.diff" "$HOME/proggs/.claude/session-opencode-backup.diff"
```

Kleiner Diff → diesen Schritt UEBERSPRINGEN, KEINE Diff-Datei anlegen. Die Diff-Datei wird beim
RESTORE nach dem Einlesen wieder GELOESCHT (siehe RESTORE Schritt 4).

### Schritt 3: Repo-Version isoliert committen und pushen

Der Backup-Commit darf **niemals bereits vorgemerkte fremde Dateien aufnehmen**. Deshalb
`git commit --only` verwenden. Der sichere Fast-Forward fand bereits vor der Repo-Kopie in Schritt 2
statt. Ein Rebase ist hier verboten: Der Skill laeuft absichtlich oft mit halbfertigen,
uncommittierten Aenderungen und ein Rebase wuerde daran scheitern oder fremden Zustand temporaer
veraendern. Vor dem Commit nur noch pruefen, ob seit dem Fast-Forward ein neuer Remote-Stand
entstanden ist. Bei Drift bleibt das lokale Backup erhalten, aber es gibt KEINEN Erfolgsmarker und
KEINE `/new`-Empfehlung.

**Windows (`pwsh`):**

```powershell
$repoRoot = "$HOME/proggs"
if ((git -C $repoRoot branch --show-current) -ne 'main') { throw "Session-Backup darf nur vom Branch main nach origin/main schreiben." }
git -C $repoRoot fetch origin
if ($LASTEXITCODE -ne 0) { throw "Fetch fehlgeschlagen; Backup ist nur lokal vorhanden." }
git -C $repoRoot merge-base --is-ancestor origin/main HEAD
if ($LASTEXITCODE -ne 0) { throw "Remote-Stand hat sich geaendert; Backup ist nur lokal vorhanden." }

$backupPaths = @('.claude/session-opencode-backup.md')
if ((Test-Path "$repoRoot/.claude/session-opencode-backup.diff") -or
    (git -C $repoRoot ls-files --error-unmatch -- .claude/session-opencode-backup.diff 2>$null)) {
    $backupPaths += '.claude/session-opencode-backup.diff'
}
foreach ($path in $backupPaths) {
    git -C $repoRoot ls-files --error-unmatch -- $path 2>$null | Out-Null
    if (($LASTEXITCODE -ne 0) -and (Test-Path "$repoRoot/$path")) { git -C $repoRoot add -N -- $path }
}
git -C $repoRoot commit --only -m "session-opencode: handoff snapshot" -- $backupPaths
if ($LASTEXITCODE -ne 0) { throw "Backup-Commit fehlgeschlagen." }
git -C $repoRoot push origin HEAD:main
if ($LASTEXITCODE -ne 0) { throw "Push fehlgeschlagen; Backup ist nicht auf anderen PCs verfuegbar." }
git -C $repoRoot fetch origin
if ((git -C $repoRoot rev-parse HEAD) -ne (git -C $repoRoot rev-parse origin/main)) {
    throw "Remote-Verifikation fehlgeschlagen; Backup ist nicht sicher synchronisiert."
}
```

**macOS/Linux (`bash`):**

```bash
repo_root="$HOME/proggs"
[ "$(git -C "$repo_root" branch --show-current)" = main ] || { printf 'Session-Backup darf nur vom Branch main nach origin/main schreiben.\n' >&2; exit 1; }
git -C "$repo_root" fetch origin || { printf 'Fetch fehlgeschlagen; Backup ist nur lokal vorhanden.\n' >&2; exit 1; }
git -C "$repo_root" merge-base --is-ancestor origin/main HEAD || { printf 'Remote-Stand hat sich geaendert; Backup ist nur lokal vorhanden.\n' >&2; exit 1; }

backup_paths=(.claude/session-opencode-backup.md)
if [ -e "$repo_root/.claude/session-opencode-backup.diff" ] ||
   git -C "$repo_root" ls-files --error-unmatch -- .claude/session-opencode-backup.diff >/dev/null 2>&1; then
  backup_paths+=(.claude/session-opencode-backup.diff)
fi
for path in "${backup_paths[@]}"; do
  if ! git -C "$repo_root" ls-files --error-unmatch -- "$path" >/dev/null 2>&1 && [ -e "$repo_root/$path" ]; then
    git -C "$repo_root" add -N -- "$path"
  fi
done
git -C "$repo_root" commit --only -m "session-opencode: handoff snapshot" -- "${backup_paths[@]}" || exit 1
git -C "$repo_root" push origin HEAD:main || { printf 'Push fehlgeschlagen; Backup ist nicht auf anderen PCs verfuegbar.\n' >&2; exit 1; }
git -C "$repo_root" fetch origin
[ "$(git -C "$repo_root" rev-parse HEAD)" = "$(git -C "$repo_root" rev-parse origin/main)" ] || {
  printf 'Remote-Verifikation fehlgeschlagen; Backup ist nicht sicher synchronisiert.\n' >&2
  exit 1
}
```

### Schritt 4: Disketten-Marker zeigen

NUR wenn Datei-Verifikation, isolierter Commit, Push und Remote-Hash-Pruefung bestanden haben.
Gib EXAKT dieses Format aus
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

### Schritt 1: Remote-Stand holen und gueltiges Backup waehlen

Vor der Auswahl immer `origin/main` holen und den aktuellen Branch ausschließlich per Fast-Forward
aktualisieren. So steht ein auf einem anderen PC gepushtes Backup lokal zur Verfuegung, ohne einen
Rebase oder Stash auszufuehren. Scheitert der Fast-Forward, kein moeglicherweise veraltetes
Repo-Backup verwenden, sondern den Drift melden.

**Windows (`pwsh`):**

```powershell
$repoRoot = "$HOME/proggs"
if ((git -C $repoRoot branch --show-current) -ne 'main') { throw "Session-Restore erwartet den Branch main." }
git -C $repoRoot fetch origin
if ($LASTEXITCODE -ne 0) { throw "Remote-Backup konnte nicht abgerufen werden." }
git -C $repoRoot merge --ff-only origin/main
if ($LASTEXITCODE -ne 0) { throw "Repo-Drift: Restore erst nach sicherer Branch-Synchronisierung fortsetzen." }

$local = "$HOME/.claude/session-opencode-backup.md"
$repo  = "$HOME/proggs/.claude/session-opencode-backup.md"
function Test-OpenCodeHandoff([string]$path) {
    return (Test-Path $path) -and ((Get-Item $path).Length -gt 0) -and
        (Select-String -Path $path -Pattern 'Session Handoff' -Quiet)
}
$localOk = Test-OpenCodeHandoff $local
$repoOk = Test-OpenCodeHandoff $repo
if ($localOk -and $repoOk -and ((Get-FileHash $local).Hash -ne (Get-FileHash $repo).Hash)) {
    throw "BACKUP-KONFLIKT: Lokale und gepushte Version unterscheiden sich; nicht automatisch fortsetzen."
}
$newer = if ($localOk) { $local } elseif ($repoOk) { $repo } else { $null }
if ($newer) { Write-Output "Zu ladendes Backup: $newer" } else { Write-Output "Kein Backup vorhanden" }
```

**macOS/Linux (`bash`):**

```bash
repo_root="$HOME/proggs"
[ "$(git -C "$repo_root" branch --show-current)" = main ] || { printf 'Session-Restore erwartet den Branch main.\n' >&2; exit 1; }
git -C "$repo_root" fetch origin || { printf 'Remote-Backup konnte nicht abgerufen werden.\n' >&2; exit 1; }
git -C "$repo_root" merge --ff-only origin/main || { printf 'Repo-Drift: Restore erst nach sicherer Branch-Synchronisierung fortsetzen.\n' >&2; exit 1; }

local_backup="$HOME/.claude/session-opencode-backup.md"
repo_backup="$HOME/proggs/.claude/session-opencode-backup.md"
valid_handoff() { [ -s "$1" ] && grep -q 'Session Handoff' "$1"; }
if valid_handoff "$local_backup" && valid_handoff "$repo_backup" && ! cmp -s "$local_backup" "$repo_backup"; then
  printf 'BACKUP-KONFLIKT: Lokale und gepushte Version unterscheiden sich; nicht automatisch fortsetzen.\n' >&2
  exit 1
fi
if valid_handoff "$local_backup"; then
  newer="$local_backup"
elif valid_handoff "$repo_backup"; then
  newer="$repo_backup"
else
  newer=''
fi
[ -n "$newer" ] && printf 'Zu ladendes Backup: %s\n' "$newer" || printf 'Kein Backup vorhanden\n'
```

Datei fehlt, ist leer oder enthaelt keine Ueberschrift `Session Handoff` → zaehlt als kein Backup.
Keine Version vorhanden → dem Benutzer sagen, dass es kein OpenCode-Backup gibt, und normal
weiterarbeiten.

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

**Windows (`pwsh`):**

```powershell
New-Item -ItemType Directory -Force "$HOME/.claude","$HOME/proggs/.claude" | Out-Null
Clear-Content "$HOME/.claude/session-opencode-backup.md" -ErrorAction SilentlyContinue
Clear-Content "$HOME/proggs/.claude/session-opencode-backup.md" -ErrorAction SilentlyContinue
Remove-Item "$HOME/.claude/session-opencode-backup.diff","$HOME/proggs/.claude/session-opencode-backup.diff" -Force -ErrorAction SilentlyContinue
$repoRoot = "$HOME/proggs"
$cleanupPaths = @('.claude/session-opencode-backup.md')
git -C $repoRoot ls-files --error-unmatch -- .claude/session-opencode-backup.diff 2>$null | Out-Null
if ($LASTEXITCODE -eq 0) { $cleanupPaths += '.claude/session-opencode-backup.diff' }
git -C $repoRoot commit --only -m "session-opencode: clear restored handoff" -- $cleanupPaths
if ($LASTEXITCODE -ne 0) { throw "Restore wurde gelesen, aber der Cleanup-Commit ist fehlgeschlagen." }
git -C $repoRoot push origin HEAD:main
if ($LASTEXITCODE -ne 0) { throw "Restore wurde gelesen, aber das Remote-Backup ist noch nicht geleert." }
git -C $repoRoot fetch origin
if ((git -C $repoRoot rev-parse HEAD) -ne (git -C $repoRoot rev-parse origin/main)) {
    throw "Remote-Cleanup konnte nicht verifiziert werden."
}
```

**macOS/Linux (`bash`):**

```bash
mkdir -p "$HOME/.claude" "$HOME/proggs/.claude"
: > "$HOME/.claude/session-opencode-backup.md"
: > "$HOME/proggs/.claude/session-opencode-backup.md"
rm -f "$HOME/.claude/session-opencode-backup.diff" "$HOME/proggs/.claude/session-opencode-backup.diff"
repo_root="$HOME/proggs"
cleanup_paths=(.claude/session-opencode-backup.md)
if git -C "$repo_root" ls-files --error-unmatch -- .claude/session-opencode-backup.diff >/dev/null 2>&1; then
  cleanup_paths+=(.claude/session-opencode-backup.diff)
fi
git -C "$repo_root" commit --only -m "session-opencode: clear restored handoff" -- "${cleanup_paths[@]}" || exit 1
git -C "$repo_root" push origin HEAD:main || { printf 'Restore wurde gelesen, aber das Remote-Backup ist noch nicht geleert.\n' >&2; exit 1; }
git -C "$repo_root" fetch origin
[ "$(git -C "$repo_root" rev-parse HEAD)" = "$(git -C "$repo_root" rev-parse origin/main)" ] || {
  printf 'Remote-Cleanup konnte nicht verifiziert werden.\n' >&2
  exit 1
}
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

## Regressionstests

Nach jeder Aenderung am Skill beide isolierten Workflow-Tests ausfuehren. Sie verwenden nur
Temp-Verzeichnisse und lokale Bare-Repositories, beruehren also weder echte Backups noch Remotes:

```powershell
pwsh -File "$HOME/proggs/opencode-setup/skill/session-opencode/tests/workflow.test.ps1"
```

```bash
bash "$HOME/proggs/opencode-setup/skill/session-opencode/tests/workflow.test.sh"
```

Beide Tests muessen Backup an zwei Orte, Commit-Isolation bei fremdem staged/dirty Zustand,
Verfuegbarkeit in einem zweiten Clone und die gepushte Leerung nach Restore bestaetigen.

---

## Verhaltensregeln

- **Du schreibst die Notiz, nicht der Benutzer.** Er sagt nur "backup" — die Kuration ist deine
  Aufgabe. Das spart Zeit und verhindert Luecken.
- **Eine Datei je Ort, immer ueberschreiben — niemals anhaengen.**
- **restore ist Pflicht-erster-Schritt nach `/new`.** Erst lesen, dann arbeiten.
- **Plattformtreue:** Unter Windows nur die `pwsh`-Bloecke, unter macOS/Linux nur die
  `bash`-Bloecke ausfuehren.
- **Bei parallelen Sessions:** Das Repo-Backup ist eine gemeinsame Datei. Beim Stagen NUR die
  Backup-Datei per `git commit --only` aufnehmen, nie `git add -A` oder einen normalen Commit
  verwenden (fremde staged/In-Flight-Dateien anderer Sessions).
- **Besondere Gruendlichkeit (Frank-Wunsch):** Die Abschnitte "Letzte Aufgaben & Ergebnisse",
  "Offene & gestellte Fragen", "Vorab-Informationen" und "Plaene & moegliche Weiterarbeit" immer
  sorgfaeltig fuellen. Restore soll Frank nicht nur technisch fortsetzen lassen, sondern ihm sofort
  erklaeren, wo ihr zuletzt wart und welche Richtung als Naechstes sinnvoll ist.
