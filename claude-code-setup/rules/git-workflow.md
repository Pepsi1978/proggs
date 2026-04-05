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

**Warum:** Mehrere CLIs (Claude Code, Codex) arbeiten am selben Repo. Ohne fetch+rebase
schlaegt der Push fehl wenn ein anderer Prozess bereits gepusht hat.

**Bei unstaged Changes:** `git stash` → fetch+rebase → `git stash pop` → dann push.

---

## 2. git add: NUR Repo-Pfade (KRITISCH)

**Regel:** `git add` darf nur Pfade innerhalb von `~/proggs/` verwenden, NIEMALS `~/.claude/`-Pfade.

**Verwechslungsgefahr — Zwei .claude/-Verzeichnisse:**
- `~/proggs/.claude/` — liegt IM Repo, darf committed werden
- `~/.claude/` — Home-Verzeichnis, NICHT im Repo

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
| Tampermonkey (JS) | `bunx biome check <file>` |
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
| `~/.claude/settings.json` | Aktive Claude-Settings mit echten Tokens |
| `~/.claude/settings.local.json` | Lokale Overrides |
| `.env`-Dateien | Projekt-Secrets (gitignored) |
| System-Umgebungsvariablen | OS-level Secrets |

### Wo Secrets NIEMALS stehen duerfen (IM Repo)

| Datei | Was stattdessen drinsteht |
|-------|--------------------------|
| `claude-code-setup/settings-reference.json` | `"REDACTED — set locally in ~/.claude/settings.json"` |
| Jede `.json`, `.md`, `.yaml` im Repo | Verweis auf lokale Datei statt echtem Secret |

### Pflicht-Ablauf beim Kopieren von Settings ins Repo

1. Kopieren: `cp ~/.claude/settings.json claude-code-setup/settings-reference.json`
2. **SOFORT danach redaktieren** — BEVOR staged/committed wird:
   ```python
   python3 -c "
   import os, re
   path = os.path.expanduser('~/proggs/claude-code-setup/settings-reference.json')
   with open(path, 'r', encoding='utf-8') as f:
       content = f.read()
   content = re.sub(r'gho_[A-Za-z0-9]{30,}', 'REDACTED — set locally in ~/.claude/settings.json', content)
   content = re.sub(r'ghp_[A-Za-z0-9]{30,}', 'REDACTED — set locally in ~/.claude/settings.json', content)
   content = re.sub(r'sk-[A-Za-z0-9]{20,}', 'REDACTED — set locally in ~/.claude/settings.json', content)
   content = re.sub(r'AIza[A-Za-z0-9_-]{30,}', 'REDACTED — set locally in ~/.claude/settings.json', content)
   with open(path, 'w', encoding='utf-8') as f:
       f.write(content)
   print('Secrets redacted')
   "
   ```
3. Erst DANN: `git add` + `git commit`

---

## 6. Edit + Version-Bump atomar ausfuehren

Bei Tampermonkey-Skripten (und generell bei Batch-Edits ueber mehrere Dateien) muessen
Edit + Version-Bump als zusammengehoeriger Schritt geplant werden.

**Regeln:**
- Pro Datei ALLE Aenderungen in einem Edit zusammenfassen wenn moeglich
- Wenn mehrere Edits pro Datei noetig: Sequentiell pro Datei (Edit 1 → Read → Edit 2),
  nicht alle Edit-1 parallel und dann alle Edit-2 parallel
- Alternativ: Coder-Agent pro Datei — der kann mehrere Edits hintereinander machen

**Vorfall:** 9 Tampermonkey-Skripte parallel editiert, danach Version-Bump fehlgeschlagen
weil alle Dateien nach dem ersten Edit "stale" waren (9 extra Reads noetig).
