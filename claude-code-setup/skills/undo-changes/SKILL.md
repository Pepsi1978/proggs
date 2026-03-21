---
name: undo-changes
description: Use when the user says "mach rueckgaengig", "undo", "revert", "Aenderung zuruecknehmen" or similar. Reverts the last commit(s) both locally AND on GitHub, with clear explanation of what was undone.
---

# Aenderungen Rueckgaengig Machen

## Wann

Dieses Skill wird aktiviert wenn der Benutzer eine der folgenden Anweisungen gibt:
- "Mach das rueckgaengig"
- "Letzte Aenderung zuruecknehmen"
- "Revert" / "Undo"
- "Das war falsch, mach es rueckgaengig"
- "Geh zurueck zum vorherigen Stand"

## Ablauf

### 1. Analyse — Was soll rueckgaengig gemacht werden?

Zuerst dem Benutzer zeigen, was rueckgaengig gemacht werden kann:

```
git log --oneline -5
```

Dem Benutzer die letzten Commits anzeigen und fragen:
- "Welchen Commit moechtest du rueckgaengig machen?"
- Oder wenn klar ist welcher gemeint ist (z.B. "den letzten"), direkt weiter

### 2. Sicherheitspruefung

Bevor etwas rueckgaengig gemacht wird:
- Pruefen ob es uncommittete Aenderungen gibt (`git status`)
- Pruefen ob der Commit bereits gepusht wurde (`git log origin/main..HEAD`)
- Dem Benutzer mitteilen was genau passieren wird

### 3. Rueckgaengig machen

**Variante A: Letzter Commit (noch nicht gepusht)**
```
git reset --soft HEAD~1
```
- Aenderungen bleiben im Staging-Bereich erhalten
- Der Benutzer kann sie anpassen und neu committen

**Variante B: Letzter Commit (bereits gepusht) — STANDARD**
```
git revert HEAD --no-edit
git push origin main
```
- Erstellt einen neuen Revert-Commit (sicherer als force-push)
- Pushed automatisch zu GitHub
- Commit-Message: `#NNN - Revert: [Original-Commit-Message]`

**Variante C: Bestimmter aelterer Commit**
```
git revert <commit-hash> --no-edit
git push origin main
```
- Macht nur diesen einen Commit rueckgaengig
- Laesst alle anderen Commits intakt

**Variante D: Mehrere Commits rueckgaengig machen**
```
git revert HEAD~N..HEAD --no-edit
git push origin main
```
- Macht die letzten N Commits rueckgaengig
- Erstellt fuer jeden einen Revert-Commit

### 4. Bestaetigung

Nach dem Revert dem Benutzer mitteilen:
- Was genau rueckgaengig gemacht wurde (Commit-Hash und Message)
- Ob der Revert auch auf GitHub gepusht wurde
- Den aktuellen Stand zeigen (`git log --oneline -3`)

## Wichtige Regeln

- **NIEMALS `git push --force`** verwenden — immer `git revert` nutzen (sicherer, nachvollziehbar)
- **NIEMALS ohne Bestaetigung** mehrere Commits auf einmal reverten
- **IMMER** sowohl lokal als auch auf GitHub rueckgaengig machen (nicht nur lokal)
- **IMMER** die Commit-Nummerierung beibehalten: Revert-Commits bekommen die naechste fortlaufende Nummer
- Bei Unsicherheit: Nachfragen welcher Commit gemeint ist
- Wenn uncommittete Aenderungen vorhanden sind: Erst fragen ob diese gespeichert werden sollen

## Shared Knowledge Hub Integration

**Whiteboard**: `.claude/agent-memory/shared/MEMORY.md` (EINZIGE zentrale Wissensdatei)

**Lesen**: Vor dem Revert das Whiteboard lesen fuer bekannte Git/Push-Probleme, laufende Migrationen oder Merge-Freezes.

**Schreiben bei Fehlern**: Wenn Revert oder Push fehlschlaegt, in "Offene Fehler & Probleme" eintragen:
- Quelle: `undo-changes`
- Symptom: [Fehlermeldung des git-Befehls]
- Commit-Hash: [Betroffener Hash]
- Ursache: [Was schiefging, z.B. Merge-Konflikt, kein Push-Recht]
- Fix-Vorschlag: [Manueller Schritt oder Alternative]
- Status: OFFEN

**Schreiben bei Erkenntnissen**: Wenn ein besonderes git-Revert-Muster gut funktioniert hat
(z.B. Multi-Commit-Revert, schwieriger Merge-Konflikt geloest, ungewoehnliche Revert-Situation),
eine 1-Zeilen-Notiz via Sentinel in "Debugging-Muster" in MEMORY.md eintragen:
`{"agent": "undo-changes", "timestamp": "[ISO8601]", "findings": "1-Zeile: was funktioniert hat"}`

## Beispiel

Benutzer sagt: "Mach die letzte Aenderung rueckgaengig"

Claude Code:
1. Zeigt: "Letzter Commit: #050 - Automatisierung und CI/CD komplett eingerichtet"
2. Fragt: "Soll ich Commit #050 rueckgaengig machen? Das wird sowohl lokal als auch auf GitHub revertiert."
3. Nach Bestaetigung: `git revert HEAD --no-edit`
4. Pusht: `git push origin main`
5. Meldet: "Commit #050 wurde rueckgaengig gemacht. Neuer Revert-Commit #051 erstellt und zu GitHub gepusht."
