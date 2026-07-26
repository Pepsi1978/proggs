---
name: undo-changes
description: "Macht den/die letzten Commit(s) rueckgaengig — lokal UND auf GitHub — per git revert (nie force-push), mit klarer Erklaerung. Trigger: mach rueckgaengig, undo, revert, Aenderung zuruecknehmen."
---

# Aenderungen Rueckgaengig Machen (Cowork-Fassung) — letzte Commits sicher reverten

Diese Cowork-Fassung nimmt den oder die letzten Commit(s) zurueck — lokal und auf GitHub — und
nutzt dafuer ausschliesslich `git revert` (nie `--force`), damit nichts verloren geht und der
Schritt nachvollziehbar bleibt. Sie laeuft in der **Claude-Cowork-Desktop-App** und ist auf deren
Umgebung zugeschnitten (instabile Mount-Bruecke, ~45s-Shell-Limit, kein nacktes Git).

---

## 0. ZUERST LESEN — Arbeitsordner & Git (Cowork)

Alle Operationen laufen im **aktuell verbundenen Arbeitsordner** (ueblicherweise der gemountete
`proggs`-Ordner) — NICHT in einem fest verdrahteten `~/proggs`-Pfad. Es wird nichts neu angelegt;
dieser Skill veraendert nur die Git-History des verbundenen Repos.

| Was | Wo |
|-----|-----|
| Ziel der Aktion | Git-History des verbundenen Arbeitsordners (lokal + `origin/main`) |
| Commit/Push | IMMER ueber `bash ~/proggs/cowork-git.sh` (siehe §0a + „Sichern") |

Ist **kein Git-Repo verbunden**: dem Benutzer das sagen und nichts reverten (ohne Repo gibt es
keine Commit-History zum Zuruecknehmen).

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

> Volltext: `bugs/claude-tooling/cowork.md` + `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

- **Git NIEMALS nackt:** Aus Cowork erlaubt die Mount-Bruecke kein zuverlaessiges `git commit`/`git
  push` (Locks, Truncation, kein Loeschen aus der VM). Der eigentliche Revert-Schritt (commit + push)
  laeuft deshalb IMMER ueber `bash ~/proggs/cowork-git.sh` — siehe „Sichern". Reine **Lese-Befehle**
  (`git log`, `git status`) sind unkritisch und koennen direkt laufen.
- **~45s-Shell-Limit:** Ein Cowork-Shell-Aufruf laeuft max ~45 Sekunden; Hintergrundprozesse
  ueberleben den Wechsel zwischen Aufrufen NICHT. Jeder Git-Schritt muss in EINEM Aufruf durchlaufen.
- **Mount-Schreibfalle:** Schreibt ein Schritt eine Datei (selten hier), danach das Dateiende pruefen
  (`tail -1`, `wc -l`) — die Bruecke kann Dateienden abschneiden.

---

## Ablauf

### 1. Analyse — Was soll rueckgaengig gemacht werden?

Zuerst dem Benutzer die letzten Commits zeigen (Lese-Befehl, direkt ausfuehrbar):

```
git log --oneline -5
```

Dann klaeren, welcher Commit gemeint ist:
- "Welchen Commit moechtest du rueckgaengig machen?"
- Oder wenn klar ist welcher gemeint ist (z.B. "den letzten"), direkt weiter.

### 2. Sicherheitspruefung

Bevor etwas rueckgaengig gemacht wird:
- Pruefen ob es uncommittete Aenderungen gibt (`git status`). Wenn ja: erst fragen, ob diese
  gespeichert werden sollen — sie duerfen nicht verloren gehen.
- Pruefen ob der Commit bereits gepusht wurde (`git log origin/main..HEAD`).
- Dem Benutzer mitteilen, was genau passieren wird.

### 3. Rueckgaengig machen

In Cowork ist der Push-Weg IMMER `cowork-git.sh` (siehe „Sichern"); deshalb wird der Revert lokal
mit `git revert --no-edit` erzeugt und der Push ueber das Cowork-Skript gemacht — NICHT mit nacktem
`git push origin main`.

**Variante A: Letzter Commit, NOCH NICHT gepusht**
```
git reset --soft HEAD~1
```
- Aenderungen bleiben im Staging-Bereich erhalten; der Benutzer kann sie anpassen und neu committen.
- Kein Push noetig (der Commit war noch nicht auf GitHub).

**Variante B: Letzter Commit, BEREITS gepusht — STANDARD**
```
git revert HEAD --no-edit
```
- Erstellt einen neuen Revert-Commit (sicherer als force-push, nachvollziehbar).
- Anschliessend ueber `cowork-git.sh` pushen (siehe „Sichern").
- Commit-Message-Schema: `#NNN - Revert: [Original-Commit-Message]` (naechste fortlaufende Nummer).

**Variante C: Bestimmter aelterer Commit**
```
git revert [commit-hash] --no-edit
```
- Macht nur diesen einen Commit rueckgaengig, laesst alle anderen intakt.
- Danach pushen ueber `cowork-git.sh`.

**Variante D: Mehrere Commits rueckgaengig machen**

Braucht **explizite Benutzer-Bestaetigung** — zeige welche N Commits revertiert werden und warte
auf die Zustimmung.
```
git revert HEAD~N..HEAD --no-edit
```
- Macht die letzten N Commits rueckgaengig, erstellt fuer jeden einen Revert-Commit.
- Danach pushen ueber `cowork-git.sh`.

### 4. Bestaetigung

Nach dem Revert dem Benutzer mitteilen:
- Was genau rueckgaengig gemacht wurde (Commit-Hash und Message).
- Ob der Revert auch auf GitHub gepusht wurde.
- Den aktuellen Stand zeigen (`git log --oneline -3`).

## Wichtige Regeln

- **NIEMALS `git push --force`** verwenden — immer `git revert` nutzen (sicherer, nachvollziehbar).
- **NIEMALS ohne Bestaetigung** mehrere Commits auf einmal reverten.
- **IMMER** sowohl lokal als auch auf GitHub rueckgaengig machen (nicht nur lokal).
- **IMMER** die Commit-Nummerierung beibehalten: Revert-Commits bekommen die naechste fortlaufende Nummer.
- Bei Unsicherheit: nachfragen, welcher Commit gemeint ist.
- Wenn uncommittete Aenderungen vorhanden sind: erst fragen, ob diese gespeichert werden sollen.

## Sichern (Cowork-Git)

Der Push des Revert-Commits laeuft IMMER ueber das Cowork-Skript (faengt Mount-Fallen +
Datenverlust-Waechter ab) — nie nacktes `git push`:

```bash
bash ~/proggs/cowork-git.sh setup            # warten auf "Push-Zugang OK"
bash ~/proggs/cowork-git.sh push             # pusht den/die erzeugten Revert-Commit(s) nach origin/main
```

Hinweis: Der eigentliche Revert-Commit wird oben mit `git revert --no-edit` lokal erzeugt; `push`
befoerdert ihn nach GitHub. Ist kein Git-Repo verbunden → dem Benutzer sagen und nichts reverten.

## Was NIEMALS passieren darf

- Aus Cowork mit nacktem `git push --force` / `git push origin main` arbeiten — immer `cowork-git.sh`.
- Mehrere Commits ohne explizite Bestaetigung reverten.
- Nur lokal reverten und den Push nach GitHub vergessen (beides gehoert zusammen).
- Uncommittete Aenderungen still wegwerfen — erst fragen, ob sie gesichert werden sollen.
- Mit `git reset --hard` oder force-push „aufraeumen" — zerstoert History und parallele Arbeit.
- Den Skill als fertig melden, ohne dem Benutzer Hash + Message des Reverts und den Push-Status zu nennen.

## Referenzen

- `bugs/claude-tooling/cowork-git-push.md` — die Cowork-Git-/Mount-Regeln (Datenverlust-Waechter, push).
- `bugs/claude-tooling/cowork.md` — die Cowork-Umgebung (Mount-Bruecke, ~45s-Shell-Limit).

## Beispiel

Benutzer sagt: "Mach die letzte Aenderung rueckgaengig"

1. Zeigt: "Letzter Commit: #050 - Automatisierung und CI/CD komplett eingerichtet"
2. Fragt: "Soll ich Commit #050 rueckgaengig machen? Das wird lokal revertiert und ueber
   `cowork-git.sh` nach GitHub gepusht."
3. Nach Bestaetigung: `git revert HEAD --no-edit`
4. Pusht: `bash ~/proggs/cowork-git.sh setup` → `bash ~/proggs/cowork-git.sh push`
5. Meldet: "Commit #050 wurde rueckgaengig gemacht. Neuer Revert-Commit #051 erstellt und nach GitHub gepusht."
