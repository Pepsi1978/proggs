# Umgebung

Hier liegt die komplette transportable Programmier-Umgebung: alle selbstgebauten Claude-Code-Plugins, alle selbst erstellten Skills und alle selbst gebauten Hooks. Damit kannst du auf jedem neuen Rechner (macOS, Linux, Windows) und in jeder KI-Umgebung (Claude Code, Codex, andere) deine vertraute Toolbox einspielen.

## Layout

```
Umgebung/
├── README.md                    ← diese Datei
│
├── Plugins/                     ← alle selbst gebauten Plugins
│   ├── finale/                  ← ein Plugin pro Unterordner
│   │   ├── INSTALL.md           ← Schritt-für-Schritt für macOS + Windows
│   │   ├── Plugin/              ← Plugin-Dateien (ohne Symlinks)
│   │   └── Skills/              ← Skills die DIESES Plugin braucht (in-Plugin-Kopie)
│   └── <weitere-plugins>/
│       ├── INSTALL.md
│       ├── Plugin/
│       └── Skills/
│
├── Skills/                      ← ALLE selbst erstellten Skills, plattformneutral
│   ├── app-roentgen/
│   ├── rechtssicherheit/
│   ├── string-extraktor/
│   ├── übersetzung/
│   ├── designer/
│   ├── superintelligenz/
│   ├── selbstbeobachtung/
│   ├── resilient-bugfixing/
│   └── ... (alle weiteren)
│
└── Hooks/                       ← ALLE selbst gebauten Hooks (.ps1 + .sh), plattformneutral
    ├── bug-almanac-index.ps1    ← (+ .sh) SessionStart: Bug-Almanach-Liste einblenden
    ├── bug-almanac-guard.ps1    ← (+ .sh) PreToolUse: an passenden Almanach erinnern
    └── ... (weitere nach Bedarf)
```

## Was alles mitgespiegelt werden muss (PFLICHT bei jeder Aenderung)

Verbindliche Regel: `~/.claude/rules/harness-mirror-on-change.md`. **Jede** Neuerstellung
oder Aktualisierung einer der folgenden Komponenten wird sofort 1:1 in die zustaendigen
Repo-Ordner uebernommen — sonst hat ein neuer Rechner / eine andere Plattform den Stand nicht.

| Komponente | Aktiv (nicht im Repo) | Spiegel hier (`Umgebung/`) | Auch nach `claude-code-setup/` |
|------------|------------------------|----------------------------|--------------------------------|
| **Skills** | `~/.claude/skills/<name>/` | `Umgebung/Skills/<name>/` | `claude-code-setup/skills/` |
| **Hooks** (`.ps1` + `.sh`) | `~/.claude/hooks/` | `Umgebung/Hooks/` | `claude-code-setup/hooks/` |
| **Plugins** (eigene) | Plugin-Quelle | `Umgebung/Plugins/<plugin>/` | `claude-code-setup/Plugins/` |
| **Agents** | `~/.claude/agents/` | — | `claude-code-setup/agents/` |
| **Commands** | `~/.claude/commands/` | — | `claude-code-setup/commands/` |
| **Rules** | `~/.claude/rules/` | — | `claude-code-setup/rules/` |
| **MCP-Server-Config** | `settings.json` / `.mcp.json` | — | `claude-code-setup/mcp-windows.json` + `mcp-macos.json` |
| **Settings** | `~/.claude/settings*.json` | — | 3-Dateien-Regel im Setup-Repo |

`Umgebung/` fuehrt bewusst das transportable Trio **Skills / Hooks / Plugins**; Agents,
Commands, Rules, MCP und Settings leben nur in `claude-code-setup/`. Beim Spiegeln IMMER
`__pycache__`, `*.pyc`, `*.bak`, `*.reset-bak-*` und den leeren Skill-Ordner `learned/`
ausschliessen. Externe (installierte) Plugins werden NICHT gespiegelt — nur eigene.

**Werkzeug:** `bash claude-code-setup/scripts/harness-mirror.sh check` zeigt, was fehlt /
veraltet / extra ist; `… sync` zieht den aktiven Stand additiv in beide Spiegel nach.

## Die zwei Skills-Bereiche — Unterschied

| Ort | Zweck | Wann nutzen |
|---|---|---|
| `Umgebung/Skills/` | **Universelle Skill-Sammlung** — alle Skills die du je gebaut hast. Plattformneutrale Kopien. Cross-CLI: kann auch von Codex und anderen KIs gelesen werden. | Auf einem neuen Rechner alle Skills einmal nach `~/.claude/skills/` kopieren. Oder als Referenz-Bibliothek wenn ein anderes KI-Tool die Skill-Logik braucht. |
| `Umgebung/Plugins/<plugin>/Skills/` | **Plugin-spezifische Skill-Kopie** — nur die Skills die *dieses eine* Plugin braucht. Verdoppelung gegenüber `Umgebung/Skills/` ist gewollt. | Wenn jemand nur ein einzelnes Plugin braucht und nicht die komplette Skill-Sammlung will. Macht das Plugin atomar installierbar. |

**Beispiel:** Auf einem neuen Rechner kannst du wählen:
- **Komplett-Setup:** Alles aus `Umgebung/Skills/` nach `~/.claude/skills/` kopieren → alle deine Skills verfügbar.
- **Minimal-Setup für nur ein Plugin:** Nur `Umgebung/Plugins/finale/Skills/` und `Umgebung/Plugins/finale/Plugin/` einspielen → finale läuft, alle anderen Skills fehlen.

## Aktueller Inhalt

### Plugins

| Plugin | Beschreibung | Pfad |
|---|---|---|
| **finale** | Android-App-Release-Pipeline: Roentgen + Rechtssicherheit + Strings + Übersetzung im Closed Loop, ausschließlich Text-Änderungen | `Plugins/finale/` |

### Skills

24 Skill-Ordner, jeweils mit `SKILL.md` und ggf. `references/`, `scripts/`, `assets/`:

| Skill | Größe | Was er macht |
|---|---|---|
| `agent-briefing` | 5 KB | Kompaktes Situational-Awareness-Briefing für Subagenten |
| `android-audio` | 7 KB | Audio-Integration in Android (Oboe, SoundPool, Media3) |
| `android-dev` | 93 KB | Android-App-Erstellung nach NowInAndroid-Architektur |
| `app-monetizer` | 57 KB | Monetarisierungs-Audit für Android-Apps |
| `app-roentgen` | 430 KB | Android-App-Durchleuchtung: Struktur, Texte, Paywall, Hidden Features |
| `auto-verify-iterate` | 3 KB | Verifikations-Loop nach jeder Coding-Aufgabe |
| `codebase-memory-exploring` | 2 KB | Codebase-Exploration via Memory-MCP |
| `codebase-memory-quality` | 2 KB | Dead-Code-Detection via Memory-MCP |
| `codebase-memory-reference` | 6 KB | Referenz-Doku für codebase-memory-Tools |
| `codebase-memory-tracing` | 4 KB | Execution-Tracing via Memory-MCP |
| `cross-platform` | 6 KB | macOS-Swift ↔ Windows-C#-Parität |
| `designer` | 47 KB | UI/UX-Audit für Android-Apps |
| `direktiven-recherche` | 1 KB | Internet-Recherche zu den drei Hauptdirektiven |
| `hook-forge` | 11 KB | Claude-Code-Hook-Erstellung mit Templates |
| `rechtssicherheit` | 143 KB | Android-App-Rechtsprüfung (HWG, UWG, DSGVO, TTDSG, Play-Policy) |
| `resilient-bugfixing` | 5 KB | Direktive #3: Kein Fehler zweimal |
| `screenshot-loop` | 7 KB | Periodische Android-Screenshots mit Vibration |
| `selbstbeobachtung` | 3 KB | Direktive #2: Beobachten, Erkennen, Lernen |
| `sound-search` | 5 KB | Freesound.org-Suche für Sound-Effekte |
| `string-extraktor` | 213 KB | Hardcodes → strings.xml für Android i18n |
| `superintelligenz` | 3 KB | Direktive #1: Intelligenteste Umgebung der Welt |
| `undo-changes` | 4 KB | Sicheres Revert per `git revert` |
| `übersetzung` | 138 KB | Android-Strings in 30+ Locales übersetzen |

**Hinweis:** Einige Skills (z. B. `codebase-memory-*`, eventuell `android-dev`) stammen evtl. aus installierten Plugins statt selbst erstellt zu sein. Sie sind trotzdem mit drin — eine kleine Datei-Größe schadet nicht und beim Filtern auf der Ziel-Plattform kannst du sie weglassen falls nicht gewünscht.

### Hooks

Selbst gebaute Claude-Code-Hooks, jeweils als `.ps1` (Windows) **und** `.sh` (macOS/Linux):

| Hook | Event | Was er macht |
|---|---|---|
| `bug-almanac-index` | SessionStart | Blendet die Liste der vorhandenen Bug-Almanache (`~/proggs/bugs/`) ein (Schicht 1 des Bug-Almanach-Systems) |
| `bug-almanac-guard` | PreToolUse (Edit/Write) | Erinnert beim Anfassen bereichstypischer Dateien an den passenden Almanach (Schicht 2) |

**Einspielen:** Hooks nach `~/.claude/hooks/` kopieren und in `~/.claude/settings.json` registrieren
(Befehlsformat und macOS-Pfade siehe `claude-code-setup/settings.json`):

```bash
cp -R Umgebung/Hooks/* ~/.claude/hooks/
# danach in settings.json unter SessionStart (bug-almanac-index)
# bzw. PreToolUse mit matcher "Edit|Write" (bug-almanac-guard) eintragen
```

## Einspielen auf einem neuen Rechner

### Variante A — Komplett-Setup

```bash
# Alle Skills nach ~/.claude/skills/ kopieren
cp -R Umgebung/Skills/* ~/.claude/skills/

# Plugins einzeln einspielen (siehe jeweilige INSTALL.md)
# Beispiel finale:
mkdir -p ~/.claude/plugins/cache/local/finale
cp -R Umgebung/Plugins/finale/Plugin ~/.claude/plugins/cache/local/finale/0.1.0
# Dann Symlinks anlegen und installed_plugins.json registrieren —
# Details in Umgebung/Plugins/finale/INSTALL.md
```

### Variante B — Nur ein Plugin

Siehe `Umgebung/Plugins/<plugin>/INSTALL.md` für plugin-spezifische Anleitung — die enthält alles was nötig ist (inklusive der Plugin-spezifischen Skills, weil die ja unter `Plugins/<plugin>/Skills/` auch dort liegen).

## Regeln für neue Plugins

1. **Ein Unterordner pro Plugin** unter `Plugins/`. Name = exakter Plugin-Name (Kleinbuchstaben, Bindestriche, keine Leerzeichen).
2. **`Plugin/` enthält den Plugin-Code** wie er nach `~/.claude/plugins/cache/<marketplace>/<plugin-name>/<version>/` gehört.
   - Symlinks werden NIE mitkopiert, sondern beim Setup auf dem Zielsystem neu angelegt.
   - `Plugin/skills/` ist im Bundle leer (mit Platzhalter-README), wird beim Setup mit Symlinks befüllt.
3. **`Skills/` enthält die abhängigen Skills** als echte Kopien (Verdoppelung zu `Umgebung/Skills/` gewollt).
4. **`INSTALL.md` ist Pflicht** — sie muss alle drei Plattformen abdecken und das Registrieren in `installed_plugins.json` mit Pfad-Templates dokumentieren.

## Bundle-Aktualisierung

Wenn du einen Skill in `~/.claude/skills/<name>/` oder ein Plugin in `~/.claude/plugins/cache/<marketplace>/<name>/<version>/` verbesserst, muss das Bundle nachgezogen werden:

```bash
# Alle Skills synchronisieren (überschreibt was im Bundle drin ist)
for d in ~/.claude/skills/*/; do
  name=$(basename "$d")
  [ -f "$d/SKILL.md" ] || continue
  rsync -a --delete "$d" "$HOME/proggs/Umgebung/Skills/$name/"
done

# Plugin-spezifische Anleitung steht in der jeweiligen Plugins/<plugin>/INSTALL.md
```

Danach committen und pushen — der Stand auf einem anderen Rechner ist dann beim nächsten `git pull` aktuell.
