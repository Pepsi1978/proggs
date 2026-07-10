# OpenCode-Setup — plattformuebergreifende Umgebung (Windows + macOS)

> Zweck: Damit OpenCode auf JEDEM Rechner (Windows wie macOS) **1:1 dieselbe Umgebung** einliest.
> Die hier gespiegelten globalen Dateien liegen im echten Betrieb unter `~/.config/opencode/`
> (NICHT im Repo) — dieser Ordner haelt sie versioniert fest, sodass ein neuer Rechner mit **einem
> Befehl** exakt nachgezogen werden kann. Gesetzt 2026-06-26, Installer 2026-06-27 (Frank).
>
> **Das Ziel in einem Satz:** OpenCode-CLI frisch installieren → `install`-Skript laufen lassen →
> Voraussetzungen (Keys/Tunnel) setzen → fertig, identische Umgebung wie auf dem Hauptrechner.

---

## Was OpenCode beim Start einliest (und was das Setup mitbringt)

| Ebene | Datei (Betrieb) | Im Repo? | Spiegelung hier | Vom Installer kopiert? |
|-------|-----------------|----------|-----------------|------------------------|
| 1. Globale Regeln | `~/.config/opencode/AGENTS.md` | nein (lokal) | **`AGENTS-global.md`** | ja |
| 2. Globale Config | `~/.config/opencode/opencode.jsonc` | nein (lokal) | **`opencode.jsonc`** | ja (shell angepasst) |
| 2b. Globale Agents | `~/.config/opencode/agents/*.md` | nein (lokal) | **`agents/`** (z.B. `researcher.md`) | ja |
| 2c. Globale Plugins | `~/.config/opencode/plugins/*.js` + Plugin-Pakete | nein (lokal) | **`plugins/`** (z.B. `tool-first-guard.js`, `token-cost-sidebar/`) | ja |
| 2c1. TUI-Plugin-Liste | `~/.config/opencode/tui.json` | nein (lokal) | **`tui.json`** | ja |
| 2c2. Globale Skills | `~/.config/opencode/skill/<name>/SKILL.md` | nein (lokal) | **`skill/`** (z.B. `session-opencode`) | ja |
| 2d. Notifier-Sounds | `~/.config/opencode/sounds/*.wav` | nein (lokal) | **`sounds/`** (complete/error/permission) | ja |
| 2e. Notifier-Config | `~/.config/opencode/opencode-notifier.json` | nein (lokal) | — (Installer **generiert** sie mit lokalen Pfaden) | ja (erzeugt) |
| 3. Projekt-Regeln | `~/proggs/AGENTS.md` | **ja** | (liegt schon im Repo) | — |
| 4. Projekt-CLAUDE.md | `~/proggs/CLAUDE.md` | **ja** | (liegt schon im Repo) | — |

**Nicht in einer Datei (kommt zentral vom Server):** Die globale `AGENTS.md` weist OpenCode an, zu
Session-Beginn alle Arbeitsregeln aus dem **zweiten Gehirn** zu laden (`second-brain`-MCP, Kategorie
`Programmierung/Rules`, per Nummer iteriert). Diese Regeln liegen also zentral — auf jedem Rechner
identisch, ohne Kopieren. Dafuer muss der WireGuard-Tunnel stehen (siehe Voraussetzungen).

> **NICHT versioniert** (von OpenCode/Bun selbst erzeugt, in `~/.config/opencode/.gitignore`):
> `node_modules/`, `package.json`, `package-lock.json`, `bun.lock`, `opencode-notifier-state.json`
> (Laufzeit-Zaehler). Diese werden weder gespiegelt noch vom Installer angefasst.

---

## Schnellstart: identische Umgebung auf einem neuen Rechner

### Schritt 1 — OpenCode-CLI installieren (frische Installation)

Das macht der Installer NICHT (Paketmanager/interaktiv) — einmal von Hand:

**macOS:**
```bash
brew install anomalyco/tap/opencode     # empfohlen (immer aktuell)
# oder universell:  curl -fsSL https://opencode.ai/install | bash
opencode --version                      # verifizieren
```

**Windows:**
```powershell
scoop install opencode                  # empfohlen (setzt PATH automatisch)
# oder:  choco install opencode
opencode --version
```
> Offizielle Empfehlung fuer Windows ist eigentlich **WSL** (bester Support). Nativ geht auch — dann
> bleibt `"shell": "pwsh"` in der Config richtig. Details: `best-practices/opencode/grundlagen-installation.md`.

### Schritt 2 — Repo klonen (falls noch nicht da)
```sh
git clone https://github.com/Pepsi1978/proggs ~/proggs
```
Damit sind Projekt-`AGENTS.md` + `CLAUDE.md` (Ebene 3+4) automatisch da.

### Schritt 3 — Das Installer-Skript ausfuehren (DER eine Befehl)

Kopiert Config, Regeln, Agents, Plugins und Sounds an ihren Platz, passt die plattformspezifische
`shell`-Zeile an, erzeugt die Notifier-Config mit korrekten lokalen Pfaden und prueft am Ende die
Voraussetzungen:

**macOS / Linux:**
```bash
bash ~/proggs/opencode-setup/install.sh
```

**Windows:**
```powershell
pwsh ~/proggs/opencode-setup/install.ps1
```

Vorhandene Dateien werden vorher nach `~/.config/opencode/.backup-<zeit>/` gesichert (idempotent,
beliebig oft wiederholbar — auch zum Aktualisieren nach einem `git pull`).

### Schritt 4 — Voraussetzungen setzen (was der Installer NUR meldet, nicht selbst kann)

Der Installer zeigt am Ende eine `OK`/`FEHLT`-Liste. Diese Dinge sind manuell zu erledigen:

- **SK-Keys** (Secrets liegen NIE im Repo, siehe Regel `secrets-in-sk-folder`) — fuer die Recherche-Pipeline:
  - `~/SK/OpenCode/firecrawl-api-key.txt` (Engine A)
  - `~/SK/OpenCode/go-api-key.txt` (OpenCode-Go / MiniMax)
  - `~/SK/ClaudeCodeOpenRouter/openrouter.key` (Engine B)
- **`OPENROUTER_API_KEY`** als User-Umgebungsvariable (fuer den Owl/OpenRouter-Provider).
- **WireGuard-Tunnel aktiv** — der `second-brain`-MCP laeuft auf `http://10.8.0.1:8001/mcp` und ist
  NUR ueber den Tunnel erreichbar. Ohne ihn werden die Gehirn-Regeln nicht geladen. Einrichtung:
  Almanach `bugs/server/wireguard.md`.
- **`opencode auth login`** (bzw. `/connect` in der TUI) — fuer das Go-Abo (opencode-go/MiniMax + Plan).

### Schritt 5 — Start & Selbst-Check
```sh
cd ~/proggs && opencode
```
Beim ersten Prompt MUSS OpenCode melden: **"N Regeln aus dem zweiten Gehirn eingelesen."** — dann ist
die Umgebung komplett. Die npm-Plugins (`@mohak34/opencode-notifier`, `@plannotator/opencode`)
installiert OpenCode beim Start selbst aus der `plugin`-Liste.

---

## Was der Installer Schritt fuer Schritt tut (Transparenz)

1. Prueft, ob `opencode` im PATH ist (nur Hinweis, kein Abbruch).
2. Legt `~/.config/opencode/{agents,plugins,sounds}` an und sichert vorhandene Dateien nach `.backup-<zeit>/`.
3. Kopiert `opencode.jsonc` — auf **macOS/Linux** wird `"shell": "pwsh"` → `"bash"` ersetzt; auf **Windows** bleibt `pwsh`; kopiert außerdem `tui.json` fuer TUI-Plugins.
4. Kopiert `AGENTS-global.md` → `AGENTS.md`, alle `agents/*.md`, alle `plugins/*.js`, Plugin-Paket-Verzeichnisse, installiert die TUI-Plugin-Dependencies per `npm`, alle `sounds/*.wav`, alle `skill/<name>/SKILL.md` (OpenCode-Skills wie `session-opencode`).
5. **Erzeugt** `opencode-notifier.json` neu mit den korrekten lokalen Sound-Pfaden (Windows BOM-frei) —
   die Repo-Variante haette feste Windows-Pfade, die auf macOS brechen.
6. Voraussetzungs-Check (SK-Keys, `OPENROUTER_API_KEY`, WireGuard `10.8.0.1`) + TODO-Liste.

## Lokale Plugins (Detail)

`plugins/*.js` werden vom Installer kopiert und beim Start automatisch geladen (kein Eintrag in
`opencode.jsonc` noetig). Aktuell:
- **`tool-first-guard.js`** — setzt die Anti-Halluzinations-Regel "Tool-first, nicht Memory-first" im
  Code durch: warnt (Log), wenn eine bestehende Datei mit `edit`/`patch` geaendert wird, ohne sie
  vorher mit `read` gelesen zu haben. Mit `OPENCODE_TOOL_FIRST_ENFORCE=1` blockt es hart statt zu
  warnen ("Laws"-Ebene). Hintergrund: `best-practices/agents/anti-halluzination-regeln.md` §1+§7.
- **`token-cost-sidebar/`** — lokales TUI-Plugin fuer die rechte Seitenleiste. Zeigt aktuelles Modell,
  Input-, Output-, optionale Reasoning- und Gesamttokens sowie Kosten in Euro. Ein mit der Maus
  bedienbares Dropdown zeigt beim Navigieren per Maus oder Pfeiltasten sofort eine Theme-Vorschau und
  merkt sich die bestaetigte Auswahl; beim Abbrechen kehrt es zum vorherigen Theme zurueck.
  Direkt darunter schaltet eine kompakte Dunkel/Hell-Auswahl die Variante des gewaehlten Themes um.
  Das Plugin bevorzugt die von
  OpenCode gespeicherte echte `cost`; bei fehlenden oder
  veralteten Nullpreisen laedt es den aktuellen Modellpreis von models.dev und beruecksichtigt
  kontextabhaengige Preisstufen. Geladen wird es ueber `tui.json` (`./plugins/token-cost-sidebar`).

## Manueller Weg (Fallback, falls das Skript nicht passt)
```sh
mkdir -p ~/.config/opencode/agents ~/.config/opencode/plugins ~/.config/opencode/sounds
cp ~/proggs/opencode-setup/opencode.jsonc   ~/.config/opencode/opencode.jsonc   # macOS: shell auf "bash" aendern
cp ~/proggs/opencode-setup/tui.json          ~/.config/opencode/tui.json
cp ~/proggs/opencode-setup/AGENTS-global.md ~/.config/opencode/AGENTS.md
cp ~/proggs/opencode-setup/agents/*.md      ~/.config/opencode/agents/
cp ~/proggs/opencode-setup/plugins/*.js     ~/.config/opencode/plugins/
cp -R ~/proggs/opencode-setup/plugins/token-cost-sidebar ~/.config/opencode/plugins/
npm --prefix ~/.config/opencode install @opencode-ai/plugin@1.17.7 @opentui/core@0.3.4 @opentui/solid@0.4.0 solid-js@1.9.12
cp ~/proggs/opencode-setup/sounds/*.wav     ~/.config/opencode/sounds/
# opencode-notifier.json mit lokalen Pfaden von Hand anlegen (siehe install-Skript als Vorlage)
```

---

## Pflege (wichtig — sonst laeuft es auseinander)

- Aenderst du `~/.config/opencode/` (Config, AGENTS.md, agents, plugins, sounds), **spiegle die
  Aenderung sofort hierher** (`opencode-setup/`) und committe — sonst hat der andere Rechner den
  alten Stand. (Gleiche Idee wie `claude-code-setup/` fuer Claude Code, nur fuer OpenCode.)
- Die eigentlichen **Arbeitsregeln** aenderst du NICHT hier, sondern zentral im **zweiten Gehirn**
  (Kategorie `Programmierung/Rules`) — von dort holt sie jeder Rechner automatisch.
- `opencode.jsonc` enthaelt **keine** Klartext-Secrets (nur `{file:}`/`{env:}`-Referenzen) — daher
  unbedenklich im Repo.
- `rules-opencode/` sind kompakte Regel-Fassungen fuer das Gehirn (Token-Sparen) — sie werden NICHT
  nach `~/.config/opencode/` kopiert, sondern dienen dem Zurueckspeichern in `Programmierung/Rules`.
