# Git-Workflow: Alle Regeln fuer Git-Operationen (KRITISCH)

> Konsolidiert aus: fetch-rebase-before-push, git-add-repo-paths-only,
> git-rebase-conflict-safety, lint-before-commit, secrets-never-in-repo,
> edit-then-version-bump

---

## 1. Fetch + Rebase VOR jedem Push (KRITISCH)

**Regel:** VOR JEDEM `git push` IMMER zuerst:
```bash
git fetch origin
git rebase origin/main
```

**Warum:** Mehrere CLIs (Gemini CLI, Codex) arbeiten am selben Repo. Ohne fetch+rebase
schlaegt der Push fehl wenn ein anderer Prozess bereits gepusht hat.

**Bei unstaged Changes:** `git stash` → fetch+rebase → `git stash pop` → dann push.

---

## 2. git add: NUR Repo-Pfade (KRITISCH)

**Regel:** `git add` darf nur Pfade innerhalb von `~/proggs/` verwenden, NIEMALS `~/.Gemini/`-Pfade.

**Verwechslungsgefahr — Zwei .Gemini/-Verzeichnisse:**
- `~/proggs/.Gemini/` — liegt IM Repo, darf committed werden
- `~/.Gemini/` — Home-Verzeichnis, NICHT im Repo

---

## 3. Rebase-Konflikt-Sicherheit (KRITISCH)

**Achtung:** Bei `git rebase` sind `--ours` und `--theirs` INVERTIERT gegenueber `git merge`!

| Befehl | `--ours` bedeutet | `--theirs` bedeutet |
|--------|-------------------|---------------------|
| `git merge` | Aktueller Branch | Eingehender Branch |
| `git rebase` | **Upstream** (main) | **Dein** Branch |

**Pflicht:** Konflikt-Dateien IMMER manuell lesen und verstehen, welche Version was ist.
NIEMALS blind `checkout --ours/--theirs` verwenden.

---

## 4. Lint/Build VOR jedem Commit (KRITISCH)

**Regel:** Den passenden Linter/Build LOKAL ausfuehren BEVOR committed wird.

| Projekttyp | Lint-Befehl |
|------------|-------------|
| C# / .NET | `dotnet build` |
| TypeScript | `tsc --noEmit` |
| Rust | `cargo clippy -- -D warnings` |
| Go | `go vet ./...` |
| Kotlin/Android | `./gradlew lint` |
| Python | `python3 -m py_compile <file>` |

**Vorfall:** 3 ESLint-Fehler-Commits wurden unnoetig gepusht weil nicht lokal geprueft wurde.

---

## 5. Secrets: NIEMALS im Repo (KRITISCH)

> **Poka-Yoke Stufe 3 (Eliminierung): Secrets koennen konzeptionell nicht mehr ins Repo gelangen.**

### Wo Secrets leben duerfen (NICHT im Repo)

| Datei | Zweck |
|-------|-------|
| `~/.Gemini/settings.json` | Aktive Gemini-Settings mit echten Tokens |
| `~/.Gemini/settings.local.json` | Lokale Overrides |
| `.env`-Dateien | Projekt-Secrets (gitignored) |
| System-Umgebungsvariablen | OS-level Secrets |

### Wo Secrets NIEMALS stehen duerfen (IM Repo)

| Datei | Was stattdessen drinsteht |
|-------|--------------------------|
| `gemini-setup/settings-reference.json` | `"REDACTED — set locally in ~/.Gemini/settings.json"` |
| Jede `.json`, `.md`, `.yaml` im Repo | Verweis auf lokale Datei statt echtem Secret |

### Pflicht-Ablauf beim Kopieren von Settings ins Repo

1. Kopieren: `cp ~/.Gemini/settings.json gemini-setup/settings-reference.json`
2. **SOFORT danach redaktieren** — BEVOR staged/committed wird:
   ```python
   python3 -c "
   import os, re
   path = os.path.expanduser('~/proggs/gemini-setup/settings-reference.json')
   with open(path, 'r', encoding='utf-8') as f:
       content = f.read()
   content = re.sub(r'gho_[A-Za-z0-9]{30,}', 'REDACTED — set locally in ~/.Gemini/settings.json', content)
   content = re.sub(r'ghp_[A-Za-z0-9]{30,}', 'REDACTED — set locally in ~/.Gemini/settings.json', content)
   content = re.sub(r'sk-[A-Za-z0-9]{20,}', 'REDACTED — set locally in ~/.Gemini/settings.json', content)
   content = re.sub(r'AIza[A-Za-z0-9_-]{30,}', 'REDACTED — set locally in ~/.Gemini/settings.json', content)
   with open(path, 'w', encoding='utf-8') as f:
       f.write(content)
   print('Secrets redacted')
   "
   ```
3. Erst DANN: `git add` + `git commit`

---

## 6. Edit + Version-Bump atomar ausfuehren

Bei Batch-Edits ueber mehrere gleichartige Dateien muessen
Edit + Version-Bump als zusammengehoeriger Schritt geplant werden.

**Regeln:**
- Pro Datei ALLE Aenderungen in einem Edit zusammenfassen wenn moeglich
- Wenn mehrere Edits pro Datei noetig: Sequentiell pro Datei (Edit 1 → Read → Edit 2),
  nicht alle Edit-1 parallel und dann alle Edit-2 parallel
- Alternativ: Coder-Agent pro Datei — der kann mehrere Edits hintereinander machen

**Vorfall:** 9 gleichartige Skript-Dateien parallel editiert, danach Version-Bump fehlgeschlagen
weil alle Dateien nach dem ersten Edit "stale" waren (9 extra Reads noetig).

---

## 7. Keine untracked Projektdateien liegen lassen (KRITISCH)

**Regel:** Alles was ein Agent im Repository erstellt oder bearbeitet und was zum Projekt
gehoert, MUSS am Ende der Aufgabe committed und nach GitHub gepusht werden. Neue Dateien
duerfen nicht still als `??` / untracked im Working Tree liegen bleiben, weil sonst der
naechste Codex-Start den Auto-Pull ueberspringt und GitHub nicht den echten Arbeitsstand
abbildet.

### Pflicht-Ablauf am Ende jeder Aufgabe

```bash
git status --short
```

Jeder Eintrag muss bewusst eingeordnet werden:

| Typ | Aktion |
|-----|--------|
| Projektdatei, Quellcode, Doku, Regel, Script, Test | Namentlich `git add <pfad>`, committen, fetch+rebase, pushen |
| Lokale Konfiguration, Secret, API-Key, maschinenspezifische Datei | NICHT committen; in `.gitignore` einordnen oder als redaktiertes Template ersetzen |
| Generierte Build-Artefakte, Cache, temporaere Dateien | Loeschen oder `.gitignore` ergaenzen |
| Unklar | Benutzer kurz fragen oder Datei pruefen; niemals still liegen lassen |

### Wichtige Grenze

"Alles nach GitHub" bedeutet NICHT, dass Secrets blind committed werden. Echte Secrets,
lokale Tokens, `.env`-Dateien, private Keys und maschinenspezifische Credentials bleiben
aus dem Repo draussen. In solchen Faellen wird ein sicheres Template oder eine README-
Anweisung committed, damit GitHub trotzdem den reproduzierbaren Projektstand enthaelt.

### Commit-Regel

Nur Dateien stagen, die diese Aufgabe wirklich betrifft:

```bash
git add pfad/zu/neuer-datei pfad/zu/geaenderter-datei
```

Niemals `git add .` oder `git add -A` verwenden, weil parallele Sessions sonst fremde
unfertige Dateien mitcommitted bekommen.

