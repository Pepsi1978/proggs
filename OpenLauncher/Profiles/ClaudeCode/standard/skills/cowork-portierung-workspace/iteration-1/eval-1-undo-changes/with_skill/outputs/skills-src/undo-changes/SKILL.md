---
name: undo-changes
description: "Nimmt den/die letzten Commit(s) sicher per revert zurueck (lokal UND auf GitHub), ohne force-push. Trigger: mach rueckgaengig, undo, revert, Aenderung zuruecknehmen, geh zum vorherigen Stand."
---

# Aenderungen Rueckgaengig Machen (Cowork-Fassung) — sicheres Revert lokal + GitHub

Dieser Skill macht den/die letzten Commit(s) rueckgaengig — bevorzugt per `git revert` (neuer
Revert-Commit, kein force-push), und immer sowohl lokal als auch auf GitHub. In Cowork laeuft Git
NICHT nackt, sondern ueber den Wrapper `cowork-git.sh`. Laeuft in der **Claude-Cowork-Desktop-App**.

---

## 0. ZUERST LESEN — Ablage-Ort & Git in Cowork

Dieser Skill schreibt keine eigenen Ergebnisdateien — er arbeitet auf dem **aktuell verbundenen
Arbeitsordner** (ueblicherweise der gemountete `proggs`-Ordner). Alle Git-Operationen laufen ueber den
Wrapper `~/proggs/cowork-git.sh`, der den richtigen git-dir/work-tree setzt und einen
Datenverlust-Waechter mitbringt.

| Was | Pfad |
|-----|------|
| Arbeitsordner (Repo) | der verbundene Mount-Ordner (relativ) |
| Git-Wrapper | `~/proggs/cowork-git.sh` (liegt bewusst ausserhalb des Mounts, bleibt absolut) |
| Whiteboard (Fehler/Erkenntnisse) | `.claude/agent-memory/shared/MEMORY.md` (relativ im Arbeitsordner) |

**Kein Git-Repo verbunden / Wrapper nicht erreichbar:** dem Benutzer melden und nichts erzwingen —
ein Revert ohne funktionierenden Git-Zugang ist nicht moeglich.

## 0a. Cowork-Umgebung — Schreib- & Git-Fallen (PFLICHT beachten)

> Volltext: `bugs/claude-tooling/cowork.md` + `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.

- **Git NIEMALS nackt:** IMMER ueber `bash ~/proggs/cowork-git.sh ...` — die Mount-Bruecke erlaubt
  kein zuverlaessiges nacktes `git` aus der VM (Locks, „Operation not permitted"). Der Wrapper hat
  einen Datenverlust-Waechter.
- **~45s-Shell-Limit:** Jeder Git-Schritt muss in EINEM Shell-Aufruf durchlaufen; Hintergrundprozesse
  ueberleben den Aufruf-Wechsel nicht. Lesen + Revert + Push deshalb nicht ueber mehrere Aufrufe verteilen.
- **Mount-Schreibfalle:** Falls du doch eine Datei schreibst (z.B. Whiteboard-Eintrag), danach das
  Dateiende pruefen (`tail -1`, `wc -l`) — die Bruecke kann Dateienden abschneiden.

---

## Ablauf

### 1. Analyse — Was soll rueckgaengig gemacht werden?

Zuerst dem Benutzer die letzten Commits zeigen:

```
bash ~/proggs/cowork-git.sh log --oneline -5
```

Dann klaeren, welcher Commit gemeint ist:
- "Welchen Commit moechtest du rueckgaengig machen?"
- Wenn klar ist welcher gemeint ist (z.B. "den letzten"), direkt weiter.

### 2. Sicherheitspruefung

Bevor etwas rueckgaengig gemacht wird:
- Uncommittete Aenderungen pruefen: `bash ~/proggs/cowork-git.sh status`
- Pruefen ob der Commit schon gepusht wurde: `bash ~/proggs/cowork-git.sh log origin/main..HEAD`
- Dem Benutzer mitteilen, was genau passieren wird.

### 3. Rueckgaengig machen

**Variante A: Letzter Commit (noch nicht gepusht)**
```
bash ~/proggs/cowork-git.sh reset --soft HEAD~1
```
- Aenderungen bleiben im Staging-Bereich erhalten; der Benutzer kann sie anpassen und neu committen.

**Variante B: Letzter Commit (bereits gepusht) — STANDARD**
```
bash ~/proggs/cowork-git.sh revert HEAD --no-edit
bash ~/proggs/cowork-git.sh push "#NNN - Revert: [Original-Commit-Message]"
```
- Erstellt einen neuen Revert-Commit (sicherer als force-push) und pusht zu GitHub.
- Commit-Nummer `#NNN` = naechste fortlaufende Nummer.

**Variante C: Bestimmter aelterer Commit**
```
bash ~/proggs/cowork-git.sh revert <commit-hash> --no-edit
bash ~/proggs/cowork-git.sh push "#NNN - Revert: [Original-Commit-Message]"
```
- Macht nur diesen einen Commit rueckgaengig, laesst alle anderen intakt.

**Variante D: Mehrere Commits rueckgaengig machen**

Verlangt EXPLIZITE Benutzer-Bestaetigung — zeigen, welche N Commits revertiert werden, und auf
Freigabe warten.
```
bash ~/proggs/cowork-git.sh revert HEAD~N..HEAD --no-edit
bash ~/proggs/cowork-git.sh push "#NNN - Revert: letzte N Commits"
```
- Macht die letzten N Commits rueckgaengig (ein Revert-Commit je Commit).

> Hinweis Cowork-Git: `cowork-git.sh push "..."` erledigt add + Datenverlust-Waechter + commit + push in
> EINEM Aufruf und holt bei Non-Fast-Forward automatisch frisch nach. Deshalb NICHT zusaetzlich
> `git push origin main` nackt aufrufen.

### 4. Bestaetigung

Nach dem Revert dem Benutzer mitteilen:
- Was genau rueckgaengig gemacht wurde (Commit-Hash und Message).
- Ob der Revert auch auf GitHub gepusht wurde.
- Aktuellen Stand zeigen: `bash ~/proggs/cowork-git.sh log --oneline -3`.

## Wichtige Regeln

- **NIEMALS `git push --force`** — immer `git revert` (nachvollziehbar, kein History-Verlust).
- **NIEMALS ohne Bestaetigung** mehrere Commits auf einmal reverten.
- **IMMER** sowohl lokal als auch auf GitHub rueckgaengig machen (nicht nur lokal).
- **IMMER** die Commit-Nummerierung beibehalten: Revert-Commits bekommen die naechste fortlaufende Nummer.
- Bei Unsicherheit: nachfragen, welcher Commit gemeint ist.
- Uncommittete Aenderungen vorhanden: erst fragen, ob diese gespeichert werden sollen.

## Whiteboard-Integration

**Whiteboard**: `.claude/agent-memory/shared/MEMORY.md` (zentrale Wissensdatei, relativ im Arbeitsordner).

- **Lesen**: Vor dem Revert das Whiteboard lesen — bekannte Git/Push-Probleme, laufende Migrationen,
  Merge-Freezes.
- **Schreiben bei Fehlern**: Schlaegt Revert oder Push fehl, in "Offene Fehler & Probleme" eintragen
  (Quelle: `undo-changes`; Symptom: Fehlermeldung; Commit-Hash; Ursache; Fix-Vorschlag; Status: OFFEN).
- **Schreiben bei Erkenntnissen**: Wenn ein Revert-Muster gut funktioniert hat (Multi-Commit-Revert,
  schwieriger Merge-Konflikt geloest), eine 1-Zeilen-Notiz in "Debugging-Muster" eintragen.

## Beispiel

Benutzer sagt: "Mach die letzte Aenderung rueckgaengig"

1. Zeigt: "Letzter Commit: #050 - Automatisierung und CI/CD komplett eingerichtet"
2. Fragt: "Soll ich Commit #050 rueckgaengig machen? Das wird lokal und auf GitHub revertiert."
3. Nach Bestaetigung: `bash ~/proggs/cowork-git.sh revert HEAD --no-edit`
4. Pusht: `bash ~/proggs/cowork-git.sh push "#051 - Revert: #050 ..."`
5. Meldet: "Commit #050 wurde rueckgaengig gemacht. Revert-Commit #051 erstellt und zu GitHub gepusht."

## Was NIEMALS passieren darf

- `git push --force` / `reset --hard` auf bereits gepushte Commits (zerstoert History / fremde Arbeit).
- Mehrere Commits ohne explizite Benutzer-Bestaetigung reverten.
- Nur lokal reverten, ohne den Revert auf GitHub zu pushen.
- Nacktes `git commit`/`git push` aus Cowork verwenden — IMMER `cowork-git.sh` (Mount-Fallen, Datenverlust-Waechter).
- Nach dem Schreiben einer Datei (z.B. Whiteboard) das Dateiende NICHT pruefen (Mount-Truncation).

## Referenzen

- Git-Wrapper: `~/proggs/cowork-git.sh` (Setup + `push`/`push-files`/beliebige git-Befehle).
- Cowork-Regeln: `bugs/claude-tooling/cowork.md`, `bugs/claude-tooling/cowork-git-push.md` im Arbeitsordner.
- Whiteboard: `.claude/agent-memory/shared/MEMORY.md` im Arbeitsordner.
