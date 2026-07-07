# Parallele Sessions — Commit & Push am geteilten main-Branch (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-04-23.
> Gilt AUTOMATISCH in JEDER Session, auf ALLEN Plattformen (Windows + macOS).
> Der Benutzer arbeitet oft mit 4–5 gleichzeitig offenen gemini-setup-Sessions am
> selben Repo (`Pepsi1978/proggs`). Alle Sessions pushen auf `main`. Diese Regel
> stellt sicher, dass trotzdem nichts überschrieben wird und nichts kaputtgeht.

---

## Grundprinzip

**Es wird IMMER auf `main` committet und nach `origin/main` gepusht.**
Keine Feature-Branches. Keine Worktrees. Kein Merge-Dance. Nur `main` —
aber mit diszipliniertem `fetch + rebase + push` vor jedem Push.

---

## Einmalige Git-Konfiguration (MUSS gesetzt sein)

Vor der ersten Session auf einem neuen Rechner: diese drei Zeilen ausführen.
Sie machen 90 % der Konflikte unsichtbar:

```bash
git config --global pull.rebase true        # Pull nutzt IMMER rebase, nie merge
git config --global rebase.autoStash true   # Lokale Änderungen werden automatisch gestasht und danach gepoppt
git config --global rerere.enabled true     # Git merkt sich gelöste Konflikte und wendet sie beim nächsten Mal automatisch wieder an
```

**Pflicht für Gemini CLI:** Beim Session-Start prüfen ob diese Werte gesetzt sind.
Falls nicht: einmalig setzen und kurz melden.

---

## Der Standard-Ablauf nach jeder Aufgabe

Nach jeder abgeschlossenen Aufgabe wird SOFORT committet und gepusht.
Die Reihenfolge ist IMMER dieselbe — keine Abkürzungen, keine Umwege:

```bash
# 1. Nur eigene Dateien namentlich stagen — NIE git add -A oder git add .
git add <datei-1> <datei-2> <datei-3>

# 2. Commit mit fortlaufender Nummer
git commit -m "#NNNN - Beschreibung auf Englisch"

# 3. Vor dem Push IMMER synchronisieren
git fetch origin
git rebase origin/main

# 4. Push
git push
```

**Die vier Schritte sind atomar.** Kein Push ohne vorheriges fetch + rebase.

---

## Verhalten bei Push-Rejection

Eine andere Session war schneller und hat zwischenzeitlich gepusht. Die Reaktion
ist IMMER dieselbe — ruhig bleiben, nicht force-pushen:

```bash
git fetch origin && git rebase origin/main && git push
```

Einfach wiederholen. Bei der zweiten Rejection wieder wiederholen.

**Bei der dritten Rejection in Folge:** Stop. Kurz 5–10 Sekunden warten, damit die
andere Session ihren Push-Burst fertig hat. Dann nochmal versuchen. Wenn das
immer noch fehlschlägt: dem Benutzer melden, nicht stumm weiterprobieren.

**Niemals als Ausweg:** `--force`, `--force-with-lease`, `reset --hard`, neues
Repo klonen. Diese zerstören Arbeit der anderen Sessions.

---

## Umgang mit fremden Änderungen im Working Tree

Beim Session-Start kann der Working Tree uncommittete Dateien von einer früheren
Session oder einer parallel laufenden Session enthalten. Diese Änderungen dürfen
**NIEMALS mitcommittet** werden — sie gehören dieser Session nicht.

### Vor dem Commit: Nur eigene Dateien stagen

```bash
# FALSCH — greift alles was im Working Tree liegt
git add -A
git add .

# RICHTIG — nur die Dateien die DIESE Session geändert hat
git add pfad/zu/datei1.kt pfad/zu/datei2.xml
```

Falls versehentlich fremde Dateien im Index landen:
```bash
git restore --staged <fremde-datei>
```

### Bei Rebase-Blockade durch fremde unstaged Änderungen

Fehler: `error: cannot rebase: You have unstaged changes.`

Vorgehen:
```bash
# Fremde Änderungen kurz beiseite packen
git stash push -u -m "hold-NNNN"

# Jetzt läuft der Rebase + Push durch
git fetch origin && git rebase origin/main && git push

# Fremde Änderungen wieder zurückholen
git stash pop
```

Die fremden Änderungen liegen dann wieder unversioniert im Working Tree und
können später von ihrer eigenen Session committet werden.

---

## Die absoluten Tabus

Diese Aktionen sind in parallelen Session-Setups **verboten**. Sie zerstören
garantiert die Arbeit anderer Sessions:

| Verbot | Warum |
|--------|-------|
| `git push --force` | Überschreibt Commits anderer Sessions auf `origin/main` |
| `git push --force-with-lease` | Schützt nur gegen lokale Konflikte, nicht gegen parallele Sessions |
| `git reset --hard` ohne explizite Benutzer-Freigabe | Zerstört unversionierte Arbeit anderer Sessions |
| `git add -A` / `git add .` | Greift fremde In-Flight-Dateien ab |
| `git commit --amend` auf bereits gepushte Commits | Zwingt zum Force-Push |
| `git rebase -i origin/main` (interaktiver Rebase über bereits gepushte Commits) | Verändert öffentliche History |
| Warten ohne Grund ("sleep bis der andere fertig ist") | Blockiert eigene Arbeit ohne Nutzen |

---

## Commit-Granularität

**Regel:** Ein Commit = ein abgeschlossener Zweck.

| Aufgabengröße | Anzahl Commits |
|--------------|---------------|
| 1 Datei, 1 Fix | 1 Commit |
| 1 Feature, mehrere Dateien | 1 Commit nach Abschluss |
| Größeres Feature (>15 Min Arbeit) | Mehrere Commits nach logischen Teilschritten |

**Faustregel:** Lieber 5 kleine Commits als 1 großer. Jeder kleine Commit ist ein
Rettungspunkt — und reduziert die Wahrscheinlichkeit eines Rebase-Konflikts mit
anderen Sessions.

**Keine Sammel-Commits** mit mehreren unabhängigen Aufgaben. Das erschwert
Revert und erhöht Konfliktwahrscheinlichkeit.

---

## Unsicherheit: Was machen die anderen Sessions gerade?

Wenn Gemini CLI unklar ist ob eine andere Session gerade denselben Code
bearbeitet, diese drei Befehle nacheinander laufen lassen:

```bash
# 1. Welche Dateien hat DIESE Session geändert?
git status --short

# 2. Hat eine andere Session schon weiter gepusht?
git fetch origin && git log HEAD..origin/main --oneline

# 3. Gibt es neue Remote-Commits die dieselben Dateien anfassen?
git fetch origin && git diff --name-only HEAD origin/main
```

Wenn die dritte Ausgabe eine Datei zeigt die auch diese Session geändert hat:
**Besonders klein committen und sofort pushen**, damit der Konflikt schnell
aufgelöst wird statt zu wachsen.

---

## Pre-Push-Check: Keine eigene Datei darf vergessen werden (KRITISCH)

Die Regel "nur eigene Dateien namentlich stagen" schützt davor, fremde Arbeit einer
parallelen Session zu klauen — hat aber eine Lücke: sie kann nicht prüfen ob DEINE
eigenen Änderungen vollständig sind. Wenn eine eigene Datei nicht gestaged wird,
bleibt sie nach dem Push als unstaged liegen und geht bei der nächsten Operation
womöglich verloren.

### Pflicht-Ablauf VOR jedem `git push`

Unmittelbar nach `git commit` und vor `git push` MUSS dieser Check laufen:

```bash
git status --short
```

Jede Zeile in der Ausgabe MUSS bewusst einer dieser drei Kategorien zugeordnet werden:

| Symbol / Präfix | Typ | Aktion |
|-----------------|-----|--------|
| `??` (untracked), `M ` / ` M` / `MM` (modified), `A ` / `AM` (added) | Datei gehört zu DIESER Aufgabe | **STOP.** Erst `git add <pfad>` + `git commit --amend` oder neuer Commit, DANN push |
| `??`, `M ` etc. | Datei gehört zu fremder paralleler Session (z.B. Codex-Output, andere Gemini-Session) | Ignorieren — aber dem Benutzer 1 Zeile melden: "Datei X liegt unstaged, gehört nicht zu dieser Aufgabe" |
| `??` | Lokaler Müll (Build-Artefakte, Temp-Dateien, `.env`, Editor-Backups) | Ignorieren oder in `.gitignore` eintragen — niemals committen |
| Ausgabe leer | Working Tree sauber | Push ist sauber, weitermachen |

### Was "bewusst zuordnen" bedeutet

Pro Zeile gedanklich beantworten:
1. **Habe ich diese Datei geändert?** Ja → gehört zu meiner Aufgabe, muss committed werden
2. **Nein, aber sie gehört zum Projekt?** → fremde Session, ignorieren aber melden
3. **Nein, und sie gehört nicht zum Projekt?** → Müll, ignorieren

Nicht einfach `git status --short` ansehen und denken "passt schon". Jede Zeile wird
einzeln klassifiziert.

### Pflicht-Meldung an den Benutzer (wenn fremde Dateien vorhanden)

Wenn nach dem Check fremde unstaged Dateien im Working Tree liegen, MUSS in der
Status-Meldung eine kurze Zeile erscheinen, z.B.:

```
Hinweis: 2 Dateien liegen noch unstaged (fremde Session?):
  .Gemini/scheduled_tasks.lock
  some-other-file.md
Nicht committed, weil nicht zu dieser Aufgabe gehörend.
```

Damit weiß der Benutzer dass der Check gelaufen ist und die Dateien bewusst
liegengelassen wurden — nicht aus Vergesslichkeit.

### Was NIEMALS passieren darf

- ❌ `git push` ohne vorheriges `git status --short`
- ❌ Eigene Dateien unstaged lassen weil "der Push ist schon durch"
- ❌ `git status --short` ausführen, aber die Ausgabe nicht Zeile für Zeile klassifizieren
- ❌ Fremde Dateien blind mit `git add -A` mitnehmen "damit der Check sauber ist"
- ❌ Die Pflicht-Meldung an den Benutzer weglassen wenn fremde Dateien da sind

### Warum das nötig ist (Poka-Yoke Stufe 2)

Diese Regel macht Vergesslichkeit bei eigenen Dateien unmöglich, ohne die Sicherheit
gegen parallele Sessions zu opfern. Der Benutzer sieht in jedem Status-Bericht
entweder "Push sauber" oder eine explizite Liste liegengelassener Dateien — nie
mehr "irgendwas fehlt und keiner merkt's".

---

## Zusammenfassung in einem Satz

> **Parallele Sessions sind OK solange jede Session vor dem Push
> `git status --short` ausführt, nur ihre eigenen Dateien namentlich staged,
> `git fetch + git rebase origin/main` macht und bei Rejection einfach
> `fetch + rebase + push` wiederholt — statt zu force-pushen oder zu resetten.**

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `git-workflow.md` (fetch+rebase-before-push) | Diese Regel ist die erweiterte Version für Multi-Session-Szenarien — Grundlage bleibt dieselbe |
| Gemini.md "Commit + Push nach JEDER Änderung" | Wird von dieser Regel ergänzt um das WIE bei parallelen Sessions |
| `bypass-permissions-permanent.md` | Commits + Pushes brauchen keine Freigabe — diese Regel definiert das saubere Vorgehen dafür |
| Cross-Platform-Status-Meldung | Status-Meldung am Ende bleibt wie gehabt ("Committed, gepusht und plattformübergreifend.") |

