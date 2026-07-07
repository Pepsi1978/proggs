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

**Warum:** Mehrere parallele Sessions koennen am selben Repo arbeiten. Ohne fetch+rebase
schlaegt der Push fehl wenn ein anderer Prozess bereits gepusht hat.

**Bei unstaged Changes:** `git stash` → fetch+rebase → `git stash pop` → dann push.

---

## 2. git add: NUR Repo-Pfade (KRITISCH)

**Regel:** `git add` darf nur Repo-Pfade innerhalb von `C:\Users\barwa\Codex CLI\` verwenden, NIEMALS `~/.codex/`-Pfade.

**Verwechslungsgefahr — Zwei .codex/-Verzeichnisse:**
- `C:\Users\barwa\Codex CLI\.codex\` — falls vorhanden im Repo, darf nur bewusst committed werden
- `~/.codex/` — Home-Verzeichnis, NICHT im Repo

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

Alle Projekt-Secrets leben zentral in `$HOME/SK/<projekt-name>/`.
Vor Secrets-bezogenen Aenderungen immer `$HOME/SK/README.md` lesen.

| Pfad | Zweck |
|------|-------|
| `$HOME/SK/BestJournalAndroid/` | Firebase-Configs, Keystores, `keystore.properties` |
| `$HOME/SK/BestJournalFrank/` | Shared Debug-Keystore |
| `$HOME/SK/VoiceOverlays/` | `.env` fuer Voice-Overlay-Projekte |
| `$HOME/SK/<neues-projekt>/` | Secrets fuer neue Projekte |

### Wo Secrets NIEMALS stehen duerfen (IM Repo)

| Datei | Was stattdessen drinsteht |
|-------|--------------------------|
| `.env` | `.env.example` mit `REDACTED`-Werten |
| `google-services.json` / `google-services-*.json` | `google-services.json.template` mit `REDACTED`-Werten |
| `credentials.json` | Template oder Doku mit SK-Pfad |
| `*.keystore`, `*.jks`, `*.p12`, `*.pem` | Nur Verweis auf `$HOME/SK/<projekt>/` |
| `keystore.properties` | `keystore.properties.template` mit `REDACTED`-Werten |

### Keine .gitignore-Ausnahmen fuer Secrets

NIEMALS `.gitignore`-Ausnahmen wie diese erstellen oder belassen:

```gitignore
!app/src/debug/google-services.json
!app/src/release/google-services.json
!*.keystore
!keystore.properties
!.env
```

Solche Ausnahmen umgehen die Ignore-Regel und waren die Ursache frueherer Leaks.

### Pflichtmuster pro Stack

| Stack | Pflichtmuster |
|-------|---------------|
| Android / Gradle | `syncSecretsFromSk` kopiert beim Build aus `$HOME/SK/<projekt>/` |
| C# / .NET | `Config.cs` sucht SK als erste Prioritaet |
| Swift / macOS | `Config.swift` sucht SK als erste Prioritaet |
| Python / Node | `$HOME/SK/<projekt>/.env` ist erste Prioritaet |

Release-Keystores sind unwiederbringbar. `$HOME/SK/BestJournalAndroid/release.keystore`
muss extra extern oder verschluesselt gesichert werden.

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
