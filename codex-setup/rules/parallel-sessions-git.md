# Parallele Sessions — Commit & Push am geteilten main-Branch (KRITISCH)

Diese Regel gilt AUTOMATISCH in JEDER Codex-Session, auf ALLEN Plattformen
(Windows + macOS). Der Benutzer kann parallel auch andere CLIs oder Terminals
am selben Repo offen haben. Alle Sessions pushen auf main.
Diese Regel stellt sicher, dass trotzdem nichts ueberschrieben wird und nichts
kaputtgeht.

## Grundprinzip

Es wird IMMER auf `main` committet und nach `origin/main` gepusht.
Keine Feature-Branches. Keine Worktrees. Kein Merge-Dance. Nur `main` —
aber mit diszipliniertem `fetch + rebase + push` vor jedem Push.

## Einmalige Git-Konfiguration (MUSS gesetzt sein)

Vor der ersten Session auf einem neuen Rechner diese drei Zeilen setzen.
Sie machen 90% der Konflikte unsichtbar:

    git config --global pull.rebase true        # Pull nutzt IMMER rebase, nie merge
    git config --global rebase.autoStash true   # Lokale Aenderungen automatisch stashen+poppen
    git config --global rerere.enabled true     # Konfliktloesungen werden gemerkt

Pflicht beim Session-Start: pruefen ob diese Werte gesetzt sind, falls nicht
einmalig setzen und kurz melden.

## Der Standard-Ablauf nach jeder Aufgabe

Nach JEDER abgeschlossenen Aufgabe wird SOFORT committet und gepusht.
Die Reihenfolge ist IMMER dieselbe — keine Abkuerzungen, keine Umwege:

    # 1. Nur eigene Dateien namentlich stagen — NIE git add -A oder git add .
    git add <datei-1> <datei-2> <datei-3>

    # 2. Commit mit fortlaufender Nummer
    git commit -m "#NNNN - Beschreibung auf Englisch"

    # 3. Vor dem Push IMMER synchronisieren
    git fetch origin
    git rebase origin/main

    # 4. Push
    git push

Die vier Schritte sind atomar. Kein Push ohne vorheriges fetch+rebase.

## Verhalten bei Push-Rejection

Eine andere Session war schneller und hat zwischenzeitlich gepusht. Die Reaktion
ist IMMER dieselbe — ruhig bleiben, nicht force-pushen:

    git fetch origin && git rebase origin/main && git push

Einfach wiederholen. Bei der zweiten Rejection wieder wiederholen.

Bei der dritten Rejection in Folge: Stop. Kurz 5-10 Sekunden warten, damit die
andere Session ihren Push-Burst fertig hat. Dann nochmal versuchen. Wenn das
immer noch fehlschlaegt: dem Benutzer melden, nicht stumm weiterprobieren.

Niemals als Ausweg: `--force`, `--force-with-lease`, `reset --hard`, neues
Repo klonen. Diese zerstoeren Arbeit der anderen Sessions.

## Umgang mit fremden Aenderungen im Working Tree

Beim Session-Start kann der Working Tree uncommittete Dateien von einer
frueheren Session oder einer parallel laufenden Session enthalten. Diese
Aenderungen duerfen NIEMALS mitcommittet werden — sie gehoeren dieser
Session nicht.

### Vor dem Commit: Nur eigene Dateien stagen

    # FALSCH — greift alles was im Working Tree liegt
    git add -A
    git add .

    # RICHTIG — nur die Dateien die DIESE Session geaendert hat
    git add pfad/zu/datei1.kt pfad/zu/datei2.xml

Falls versehentlich fremde Dateien im Index landen:

    git restore --staged <fremde-datei>

### Bei Rebase-Blockade durch fremde unstaged Aenderungen

Fehler: `error: cannot rebase: You have unstaged changes.`

Vorgehen:

    # Fremde Aenderungen kurz beiseite packen
    git stash push -u -m "hold-NNNN"

    # Jetzt laeuft Rebase + Push durch
    git fetch origin && git rebase origin/main && git push

    # Fremde Aenderungen wieder zurueckholen
    git stash pop

Die fremden Aenderungen liegen dann wieder unversioniert im Working Tree und
koennen spaeter von ihrer eigenen Session committet werden.

## Die absoluten Tabus

Diese Aktionen sind in parallelen Session-Setups VERBOTEN. Sie zerstoeren
garantiert die Arbeit anderer Sessions:

- `git push --force` — ueberschreibt Commits anderer Sessions auf origin/main
- `git push --force-with-lease` — schuetzt nur gegen lokale Konflikte, nicht
  gegen parallele Sessions
- `git reset --hard` ohne explizite Benutzer-Freigabe — zerstoert unversionierte
  Arbeit anderer Sessions
- `git add -A` / `git add .` — greift fremde In-Flight-Dateien ab
- `git commit --amend` auf bereits gepushte Commits — zwingt zum Force-Push
- `git rebase -i origin/main` (interaktiver Rebase ueber bereits gepushte
  Commits) — veraendert oeffentliche History
- Warten ohne Grund ("sleep bis der andere fertig ist") — blockiert eigene
  Arbeit ohne Nutzen

## Commit-Granularitaet

Ein Commit = ein abgeschlossener Zweck.

- 1 Datei, 1 Fix                 → 1 Commit
- 1 Feature, mehrere Dateien     → 1 Commit nach Abschluss
- Groesseres Feature (>15 Min)   → Mehrere Commits nach logischen Teilschritten

Faustregel: Lieber 5 kleine Commits als 1 grosser. Jeder kleine Commit ist ein
Rettungspunkt und reduziert die Wahrscheinlichkeit eines Rebase-Konflikts mit
anderen Sessions.

Keine Sammel-Commits mit mehreren unabhaengigen Aufgaben. Das erschwert
Revert und erhoeht Konfliktwahrscheinlichkeit.

## Unsicherheit: Was machen die anderen Sessions gerade?

Wenn unklar ist ob eine andere Session gerade dieselben Dateien bearbeitet,
diese drei Befehle nacheinander laufen lassen:

    # 1. Welche Dateien hat DIESE Session geaendert?
    git status --short

    # 2. Hat eine andere Session schon weiter gepusht?
    git fetch origin && git log HEAD..origin/main --oneline

    # 3. Gibt es neue Remote-Commits die dieselben Dateien anfassen?
    git fetch origin && git diff --name-only HEAD origin/main

Wenn die dritte Ausgabe eine Datei zeigt die auch diese Session geaendert hat:
Besonders klein committen und sofort pushen, damit der Konflikt schnell
aufgeloest wird statt zu wachsen.

## Commit-Nachrichten (Format)

Jede Commit-Nachricht beginnt mit einer fortlaufenden Nummer:

    #NNNN - Beschreibung auf Englisch

Die Nummerierung wird anhand der existierenden Commits im Repo automatisch
ermittelt (letzte Nummer + 1).

## Pre-Push-Check: Keine eigene Datei darf vergessen werden (KRITISCH)

Die Regel "nur eigene Dateien namentlich stagen" schuetzt davor, fremde Arbeit
einer parallelen Session zu klauen — hat aber eine Luecke: sie kann nicht
pruefen ob DEINE eigenen Aenderungen vollstaendig sind. Wenn eine eigene Datei
nicht gestaged wird, bleibt sie nach dem Push als unstaged liegen und geht bei
der naechsten Operation womoeglich verloren.

### Pflicht-Ablauf VOR jedem `git push`

Unmittelbar nach `git commit` und vor `git push` MUSS dieser Check laufen:

    git status --short

Jede Zeile in der Ausgabe MUSS bewusst einer dieser drei Kategorien zugeordnet
werden:

| Symbol / Praefix                         | Typ                                        | Aktion |
|------------------------------------------|--------------------------------------------|--------|
| `??`, `M `, ` M`, `MM`, `A `, `AM` — eigene Aenderung | Datei gehoert zu DIESER Aufgabe    | STOP. Erst `git add <pfad>` + neuer Commit, DANN push |
| `??`, `M ` etc. — fremde Aenderung       | Gehoert zu fremder paralleler Session      | Ignorieren, aber dem Benutzer 1 Zeile melden: "Datei X liegt unstaged, gehoert nicht zu dieser Aufgabe" |
| `??` — lokaler Muell                     | Build-Artefakte, Temp-Dateien, .env, Backups | Ignorieren oder in .gitignore eintragen, niemals committen |
| Ausgabe leer                             | Working Tree sauber                        | Push ist sauber, weitermachen |

### Was "bewusst zuordnen" bedeutet

Pro Zeile gedanklich beantworten:
1. Habe ich diese Datei geaendert? Ja → gehoert zu meiner Aufgabe, muss committed werden
2. Nein, aber sie gehoert zum Projekt? → fremde Session, ignorieren aber melden
3. Nein, und sie gehoert nicht zum Projekt? → Muell, ignorieren

Nicht einfach `git status --short` ansehen und denken "passt schon". Jede Zeile
wird einzeln klassifiziert.

### Pflicht-Meldung an den Benutzer (wenn fremde Dateien vorhanden)

Wenn nach dem Check fremde unstaged Dateien im Working Tree liegen, MUSS in der
Status-Meldung eine kurze Zeile erscheinen, z.B.:

    Hinweis: 2 Dateien liegen noch unstaged (fremde Session?):
      .claude/scheduled_tasks.lock
      some-other-file.md
    Nicht committed, weil nicht zu dieser Aufgabe gehoerend.

Damit weiss der Benutzer dass der Check gelaufen ist und die Dateien bewusst
liegengelassen wurden — nicht aus Vergesslichkeit.

### Was NIEMALS passieren darf

- `git push` ohne vorheriges `git status --short`
- Eigene Dateien unstaged lassen weil "der Push ist schon durch"
- `git status --short` ausfuehren, aber die Ausgabe nicht Zeile fuer Zeile klassifizieren
- Fremde Dateien blind mit `git add -A` mitnehmen "damit der Check sauber ist"
- Die Pflicht-Meldung an den Benutzer weglassen wenn fremde Dateien da sind

### Warum das noetig ist (Poka-Yoke Stufe 2)

Diese Regel macht Vergesslichkeit bei eigenen Dateien unmoeglich, ohne die
Sicherheit gegen parallele Sessions zu opfern. Der Benutzer sieht in jedem
Status-Bericht entweder "Push sauber" oder eine explizite Liste
liegengelassener Dateien — nie mehr "irgendwas fehlt und keiner merkt's".

## Zusammenfassung in einem Satz

Parallele Sessions sind OK solange jede Session vor dem Push
`git status --short` ausfuehrt, nur ihre eigenen Dateien namentlich staged,
`git fetch + git rebase origin/main` macht und bei Rejection einfach
`fetch + rebase + push` wiederholt — statt zu force-pushen oder zu resetten.

## Geltungsbereich

Diese Regel ist aus Codex-Sicht verbindlich. Andere CLIs oder Terminal-Sessions
koennen parallel am selben Git-Repo arbeiten; Codex darf aber nicht annehmen,
dass sie ihren Zustand mitteilen. Nur der sauber disziplinierte Git-Workflow
verhindert Kollisionen.
