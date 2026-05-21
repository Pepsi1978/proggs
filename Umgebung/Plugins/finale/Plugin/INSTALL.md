# Installation — finale Plugin

Diese Anleitung deckt die plattform-spezifischen Schritte fuer die Einrichtung
des `finale`-Plugins ab. Erstellt 2026-05-21 als Loop-2-Hardening (Direktive #3).

---

## Voraussetzungen

| Tool | Linux | macOS | Windows |
|------|-------|-------|---------|
| `bash` 4.0+ | systemweit | systemweit oder via Homebrew | Git Bash (msys2) — empfohlen |
| `pwsh` 7.0+ | optional | optional | systemweit |
| `python3` 3.8+ | **PFLICHT** | **PFLICHT** | **PFLICHT** (Microsoft Store oder python.org) |
| `realpath` ODER `readlink -f` | systemweit | `brew install coreutils` (sonst python3-Fallback) | Git Bash systemweit |
| `gradle` 7.0+ via `./gradlew` | per App-Projekt | per App-Projekt | per App-Projekt |

**Pflicht-Tools sind hart**: ohne `python3` blockieren die Sicherheits-Hooks
absichtlich ALLE Bash-Befehle (Fail-Closed). Das ist Design, nicht Bug.

---

## Schritt 1 — Plugin in Claude Code installieren

Empfohlener Weg: ueber den Claude Code Plugin-Manager (legt `installed_plugins.json`
automatisch an). Manueller Weg (falls Plugin-Manager nicht verfuegbar):

```bash
# Verzeichnis fuer lokale Plugins erstellen
mkdir -p "$HOME/.claude/plugins/local/finale"

# Plugin kopieren (oder Symlink ins finale/Plugin/-Verzeichnis dieses Repos)
cp -r ~/proggs/Umgebung/Plugins/finale/Plugin "$HOME/.claude/plugins/local/finale/Plugin"

# Verifikation
ls "$HOME/.claude/plugins/local/finale/Plugin/.claude-plugin/plugin.json"
```

**Hinweis fuer manuelle Installation**: Wenn `installed_plugins.json` nicht
automatisch erzeugt wird, nutzt der Orchestrator den Fallback `CLAUDE_PLUGIN_ROOT`
env-Variable oder bekannte Standard-Pfade — siehe orchestrator.md Phase 0 Schritt 0.

---

## Schritt 2 — Skill-Symlinks anlegen

Die vier Skills muessen unter `~/.claude/skills/` existieren (separate Bundle-
Installation). Dann im Plugin-Bundle Symlinks anlegen:

### macOS / Linux

```bash
cd "$HOME/.claude/plugins/local/finale/Plugin/skills"

ln -sfn "$HOME/.claude/skills/app-roentgen"      roentgen-skill
ln -sfn "$HOME/.claude/skills/rechtssicherheit"  rechtssicherheits-skill
ln -sfn "$HOME/.claude/skills/string-extraktor"  strings-skill
ln -sfn "$HOME/.claude/skills/übersetzung"       uebersetzer-skill

# Verifikation
bash ../scripts/verify-skills.sh
# Erwartet: {"ok": true, ...} und Exit-Code 0
```

### Windows mit Git Bash (Empfehlung)

```bash
# Vorab: Developer Mode aktivieren
# Settings -> Privacy & Security -> For Developers -> Developer Mode ON

# Git Bash: native Symlinks erzwingen
export MSYS=winsymlinks:nativestrict

cd "$HOME/.claude/plugins/local/finale/Plugin/skills"

ln -sfn "$HOME/.claude/skills/app-roentgen"      roentgen-skill
ln -sfn "$HOME/.claude/skills/rechtssicherheit"  rechtssicherheits-skill
ln -sfn "$HOME/.claude/skills/string-extraktor"  strings-skill
ln -sfn "$HOME/.claude/skills/übersetzung"       uebersetzer-skill

# Verifikation
bash ../scripts/verify-skills.sh
```

### Windows mit nativer PowerShell (ohne Git Bash)

```powershell
# Vorab: Developer Mode aktivieren (Settings -> Privacy -> For Developers)

cd "$env:USERPROFILE\.claude\plugins\local\finale\Plugin\skills"

New-Item -ItemType SymbolicLink -Path "roentgen-skill"          -Target "$env:USERPROFILE\.claude\skills\app-roentgen"      -Force
New-Item -ItemType SymbolicLink -Path "rechtssicherheits-skill" -Target "$env:USERPROFILE\.claude\skills\rechtssicherheit"  -Force
New-Item -ItemType SymbolicLink -Path "strings-skill"           -Target "$env:USERPROFILE\.claude\skills\string-extraktor"  -Force
New-Item -ItemType SymbolicLink -Path "uebersetzer-skill"       -Target "$env:USERPROFILE\.claude\skills\übersetzung"       -Force

# Verifikation (braucht bash — wenn nicht da, dann nur Existenz-Check)
Get-ChildItem .
# Erwartet: 4 Eintraege mit Mode "l" (Symlink)
```

---

## Schritt 3 — Hooks aktivieren

Die `hooks.json` registriert sowohl `.sh`- als auch `.ps1`-Hooks (Dual-Registration).
Auf jeder Plattform laeuft genau eine Variante:

- **Mit Git Bash oder nativem bash**: `.sh`-Hooks laufen, `.ps1`-Hooks pruefen
  am Anfang `Get-Command bash` und beenden sich still mit exit 0.
- **Native Windows ohne bash**: `.sh`-Aufruf schlaegt fehl (Claude Code ignoriert),
  `.ps1`-Hooks laufen normal.

Keine manuelle Konfiguration noetig.

---

## Schritt 4 — Erster Lauf

```bash
# Aus dem proggs-Repo heraus
cd ~/proggs/BestJournalAndroid

# Closed-Loop-Pipeline starten
/finale:run

# Oder mit explizitem App-Pfad
/finale:run ~/proggs/BestJournalAndroid
```

Bei erstem Lauf zeigt Phase 0 das Pre-Flight-Plan-Format:

```
═══════════════════════════════════════════════════════════════════
ANDROID-RELEASE-SHIELD — PRE-FLIGHT-PLAN
═══════════════════════════════════════════════════════════════════

App-Root:        ...
Modus:           default
SKILL-VERSIONEN: ...
[F] Freigeben und Phase 1 starten
[A] Modus wechseln
[X] Abbrechen
```

Mit `[F]` startet die Pipeline. Mit `[A]` kann der Modus auf audit-only, fix-only,
strings-only oder translate-only gewechselt werden.

---

## Schritt 5 — Verifikation

Plugin-Health-Check (sollte alle Komponenten gruen melden):

```bash
# verify-skills.sh ohne App-Root: pruefen nur die Symlinks
bash "$HOME/.claude/plugins/local/finale/Plugin/scripts/verify-skills.sh"

# Mit App-Root: zusaetzlicher Sanity-Check (AndroidManifest.xml/settings.gradle)
bash "$HOME/.claude/plugins/local/finale/Plugin/scripts/verify-skills.sh" "$HOME/proggs/BestJournalAndroid"
```

Erwartete Ausgabe:
```json
{
  "ok": true,
  "appRootSanity": "ok",
  "skills": [
    {"name": "roentgen-skill", "status": "ok", ...},
    {"name": "rechtssicherheits-skill", "status": "ok", ...},
    {"name": "strings-skill", "status": "ok", ...},
    {"name": "uebersetzer-skill", "status": "ok", ...}
  ]
}
```

---

## Troubleshooting

### "FEHLER: FINALE_PLUGIN_ROOT konnte nicht aufgeloest werden"

Plugin wurde nicht via Plugin-Manager installiert. Workaround:

```bash
export FINALE_PLUGIN_ROOT="$HOME/.claude/plugins/local/finale/Plugin"
```

Permanenter Fix: Plugin in der Claude-Code-UI als Plugin registrieren oder
`installed_plugins.json` manuell mit einem `finale`-Eintrag versehen.

### "Skill-Symlink zeigt als regulaere Datei, nicht als 'l'"

Auf Windows: Developer Mode nicht aktiv. Git Bash faellt dann auf Kopien zurueck
(Hardlinks/Junctions) — das verursacht Versions-Drift zwischen Skill-Quelle und
Plugin-Version.

**Loesung**: Developer Mode aktivieren, Git Bash neu starten, `export MSYS=winsymlinks:nativestrict`,
Symlinks neu anlegen.

### "python3 nicht im PATH — Hook kann JSON-Input nicht parsen"

Die blockierenden Sicherheits-Hooks (pretooluse-bash, audit-only-write-guard)
verlangen `python3`. Wenn fehlend: blockieren absichtlich alle Bash-Befehle
(Fail-Closed-Default).

**Loesung**: Python 3.8+ installieren und im PATH verfuegbar machen. Auf
Windows: Microsoft Store -> Python oder python.org Installer mit "Add to PATH"-Option.

### "Phase 0 sagt 'Skill geaendert' obwohl ich nichts angefasst habe"

Der mtime der Skill-Datei hat sich geaendert (z.B. weil ein Formatter durchlief),
waehrend der Inhalts-Hash gleich blieb. In diesem Fall ist `Δ unveraendert` mit
`mtime-only` aussagekraeftig. Nicht beunruhigend.

### Audit-only-Lock haengt nach Crash

Wenn der Orchestrator waehrend eines `/finale:audit-only`-Laufs abbricht (Stromausfall,
Token-Cap), bleibt `.android-shield/.audit-only.lock` liegen und blockiert alle
weiteren Schreibversuche.

**Loesung**: 
```bash
rm "<app-root>/.android-shield/.audit-only.lock"
```

Der Orchestrator erkennt Stale-Locks ab Wave 7 (>30 Min alt) und ignoriert sie
automatisch — bei juengeren Locks ist manuelles Eingreifen noetig.

---

## Naechste Schritte

- Plugin-spezifische Doku: `README.md`
- Orchestrator-Logik: `agents/orchestrator.md`
- Cross-Platform-Hooks-Details: `README.md` Sektion "Cross-Platform-Hooks"
- Skill-Update-Workflow: Skill in `~/.claude/skills/<name>/` aendern -> Phase 0
  erkennt automatisch via SHA-Vergleich.
