# Umgebung — finale-Plugin transportabel einspielen

Dieses Bundle enthält das `finale`-Plugin plus seine vier abhängigen Skills in einer Form, mit der das ganze Setup auf einem neuen Rechner (macOS oder Windows) eingespielt werden kann.

## Inhalt

```
Umgebung/
└── Plugins/
    └── finale/                       ← Wurzel des finale-Plugin-Bundles
        ├── INSTALL.md                ← diese Datei
        ├── Plugin/                   ← das finale-Plugin (ohne Symlinks)
        │   ├── .claude-plugin/plugin.json
        │   ├── agents/        (4 Agents)
        │   ├── commands/      (5 Slash-Commands)
        │   ├── hooks/         (hooks.json + 3 Bash-Scripts)
        │   ├── scripts/       (verify-skills.sh)
        │   ├── skills/        (LEER — wird beim Setup mit Symlinks befüllt)
        │   ├── README.md
        │   └── …
        └── Skills/                   ← die vier abhängigen Skills
            ├── app-roentgen/
            ├── rechtssicherheit/
            ├── string-extraktor/
            └── übersetzung/
```

## Wichtige Designentscheidung

Das Plugin bindet die vier Skills nicht als Kopien ein, sondern als **Symlinks** auf
`~/.claude/skills/<skill-name>/`. Symlinks zeigen auf konkrete Pfade, die plattformspezifisch sind. Deshalb sind die Symlinks im Bundle bewusst NICHT enthalten — sie werden bei der Installation auf dem Zielsystem neu angelegt, mit den Pfaden des Zielsystems.

Im Bundle liegen die Skills daher zweimal logisch und einmal physisch:
- **physisch** unter `Skills/` (echte Kopien, plattformunabhängig)
- **logisch erwartet** unter `Plugin/skills/` (wird bei Setup als Symlink-Liste eingerichtet)

---

## Installation auf macOS

```bash
# 1. Skills nach ~/.claude/skills/ kopieren
cp -R Umgebung/Plugins/finale/Skills/app-roentgen      ~/.claude/skills/
cp -R Umgebung/Plugins/finale/Skills/rechtssicherheit  ~/.claude/skills/
cp -R Umgebung/Plugins/finale/Skills/string-extraktor  ~/.claude/skills/
cp -R Umgebung/Plugins/finale/Skills/übersetzung       ~/.claude/skills/

# 2. Plugin nach ~/.claude/plugins/cache/local/finale/0.1.0/ kopieren
mkdir -p ~/.claude/plugins/cache/local/finale
cp -R Umgebung/Plugins/finale/Plugin ~/.claude/plugins/cache/local/finale/0.1.0

# 3. Skill-Symlinks im Plugin neu anlegen
cd ~/.claude/plugins/cache/local/finale/0.1.0/skills
rm -f roentgen-skill rechtssicherheits-skill strings-skill uebersetzer-skill README.md
ln -s ~/.claude/skills/app-roentgen      roentgen-skill
ln -s ~/.claude/skills/rechtssicherheit  rechtssicherheits-skill
ln -s ~/.claude/skills/string-extraktor  strings-skill
ln -s ~/.claude/skills/übersetzung       uebersetzer-skill

# 4. Skripte ausführbar machen (cp -R unter macOS bewahrt das nicht zuverlässig)
chmod +x ~/.claude/plugins/cache/local/finale/0.1.0/scripts/*.sh
chmod +x ~/.claude/plugins/cache/local/finale/0.1.0/hooks/*.sh

# 5. Symlinks verifizieren
bash ~/.claude/plugins/cache/local/finale/0.1.0/scripts/verify-skills.sh
# Erwartet: { "ok": true, ... } und Exit-Code 0

# 6. Plugin in installed_plugins.json registrieren (siehe Abschnitt unten)
```

---

## Installation auf Windows (Git Bash)

Voraussetzung: **Windows Developer Mode aktiv** (sonst funktionieren native Symlinks nicht).
→ Einstellungen → Datenschutz & Sicherheit → Für Entwickler → Entwicklermodus aktivieren.

```bash
# 1. Skills nach ~/.claude/skills/ kopieren
cp -R Umgebung/Plugins/finale/Skills/app-roentgen      ~/.claude/skills/
cp -R Umgebung/Plugins/finale/Skills/rechtssicherheit  ~/.claude/skills/
cp -R Umgebung/Plugins/finale/Skills/string-extraktor  ~/.claude/skills/
cp -R Umgebung/Plugins/finale/Skills/übersetzung       ~/.claude/skills/

# 2. Plugin nach ~/.claude/plugins/cache/local/finale/0.1.0/ kopieren
mkdir -p ~/.claude/plugins/cache/local/finale
cp -R Umgebung/Plugins/finale/Plugin ~/.claude/plugins/cache/local/finale/0.1.0

# 3. Skill-Symlinks im Plugin neu anlegen — native Windows-Symlinks!
cd ~/.claude/plugins/cache/local/finale/0.1.0/skills
rm -f roentgen-skill rechtssicherheits-skill strings-skill uebersetzer-skill README.md
export MSYS=winsymlinks:nativestrict
ln -s "$HOME/.claude/skills/app-roentgen"      roentgen-skill
ln -s "$HOME/.claude/skills/rechtssicherheit"  rechtssicherheits-skill
ln -s "$HOME/.claude/skills/string-extraktor"  strings-skill
ln -s "$HOME/.claude/skills/übersetzung"       uebersetzer-skill
ls -la
# Erwartet: lrwxrwxrwx (vier echte Symlinks)

# 4. Skripte ausführbar machen (Git Bash behält +x meist, aber sicher ist sicher)
chmod +x ~/.claude/plugins/cache/local/finale/0.1.0/scripts/*.sh
chmod +x ~/.claude/plugins/cache/local/finale/0.1.0/hooks/*.sh

# 5. Symlinks verifizieren
bash ~/.claude/plugins/cache/local/finale/0.1.0/scripts/verify-skills.sh
# Erwartet: { "ok": true, ... } und Exit-Code 0
```

---

## Plugin in `installed_plugins.json` registrieren

Damit Claude Code das Plugin findet, muss es in `~/.claude/plugins/installed_plugins.json` eingetragen sein. Das Bundle macht das nicht automatisch — du fügst den folgenden Eintrag unter `plugins` ein:

### macOS-Variante

```json
"finale@local": [
  {
    "scope": "user",
    "installPath": "/Users/<DEIN-USER>/.claude/plugins/cache/local/finale/0.1.0",
    "version": "0.1.0",
    "installedAt": "<heutiges-datum-iso>",
    "lastUpdated": "<heutiges-datum-iso>",
    "gitCommitSha": "local-no-git"
  }
]
```

### Windows-Variante

```json
"finale@local": [
  {
    "scope": "user",
    "installPath": "C:\\Users\\<DEIN-USER>\\.claude\\plugins\\cache\\local\\finale\\0.1.0",
    "version": "0.1.0",
    "installedAt": "<heutiges-datum-iso>",
    "lastUpdated": "<heutiges-datum-iso>",
    "gitCommitSha": "local-no-git"
  }
]
```

Wichtig:
- Backup von `installed_plugins.json` machen, bevor du editierst!
- JSON-Syntax muss korrekt sein — Komma zwischen den Plugin-Einträgen nicht vergessen.
- Nach dem Eintrag Claude Code neu starten.

---

## Nach der Installation testen

```bash
# Im Verzeichnis einer beliebigen deinen Android-Apps:
cd ~/proggs/BestJournalAndroid

# Read-only Erstlauf:
/finale:audit-only

# Oder natürlichsprachlich aus einem beliebigen Verzeichnis:
„Starte finale audit über der App Best Journal Android"
```

Wenn die App in `~/proggs/` liegt, kann das Plugin den Pfad automatisch auflösen — du musst nicht selbst `cd`-en.

---

## Bundle-Aktualisierung

Wenn du das Plugin oder einen Skill verbesserst, muss das Bundle aktualisiert werden, damit der Stand auf einem neuen Rechner stimmt. Empfohlener Ablauf:

1. Plugin in `~/.claude/plugins/cache/local/finale/0.1.0/` bearbeiten und testen.
2. Skill(s) in `~/.claude/skills/<name>/` bearbeiten und testen.
3. Bundle re-sync-en:

```bash
# Plugin synchronisieren (ohne Symlinks)
rsync -a --exclude='skills/' --delete \
  ~/.claude/plugins/cache/local/finale/0.1.0/  ~/proggs/Umgebung/Plugins/finale/Plugin/
# (skills/ wird ausgenommen — bleibt mit Platzhalter-README)

# Skills synchronisieren
for s in app-roentgen rechtssicherheit string-extraktor übersetzung; do
  rsync -a --delete ~/.claude/skills/$s/  ~/proggs/Umgebung/Plugins/finale/Skills/$s/
done

# Commit + Push
cd ~/proggs
git add Umgebung/
git commit -m "#NNN - Umgebung-Bundle synchronisiert"
git push
```

Hinweis: `rsync` ist auf macOS Standard und auf Windows via Git Bash + zusätzlicher Installation (`pacman -S rsync` in Git Bash, oder über WSL) verfügbar. Alternativ funktioniert `cp -R` wie oben gezeigt.

---

## Troubleshooting

**„Symlinks erscheinen als reguläre Dateien (`-rw-r--r--` statt `lrwxrwxrwx`)":**
Auf Windows ohne Developer Mode fällt Git Bash auf Kopien zurück. Developer Mode aktivieren, Git Bash neu starten, Symlinks neu anlegen.

**„verify-skills.sh: Skill X target-directory-missing":**
Du hast Schritt 1 vergessen (Skills nach `~/.claude/skills/` kopieren) oder einen Tippfehler im Symlink-Namen. Output des Scripts zeigt genau welches Ziel fehlt.

**„Plugin taucht in Claude Code nicht auf":**
- Eintrag in `installed_plugins.json` fehlt oder hat falschen Pfad.
- Pfad in `installPath` muss exakt dem physischen Plugin-Pfad entsprechen (achte auf macOS-Forward-Slashes vs. Windows-Backslashes).
- Claude Code muss neu gestartet werden.

**„übersetzung-Skill hat Probleme mit Umlaut im Verzeichnisnamen":**
Auf manchen Systemen kann der Umlaut im Verzeichnis-Namen Probleme machen. Falls ja: Symlink mit Sonderfall anlegen (`ln -s ~/.claude/skills/uebersetzung uebersetzer-skill`) und das Quellverzeichnis temporär umbenennen.
