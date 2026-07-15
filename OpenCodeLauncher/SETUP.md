# OpenCode Launcher — Einrichtung auf einem neuen Rechner

> Diese Datei beschreibt, **wie die Profile aktuell laufen** und **was auf einem neuen Rechner
> nötig ist**, damit alles identisch funktioniert. Du kannst Claude Code auf dem neuen Rechner
> einfach sagen: *„Richte den OpenCode Launcher genau nach `OpenCodeLauncher/SETUP.md` ein."*

Stand: v1.17.18 (15.07.2026, 15.02 Uhr)

---

## Kurzantwort: Reicht „nur Release bauen"?

**Nein, nicht ganz.** Mit `git pull` + Release-Build hast du den **Launcher-Code und alle
Profil-Quellen**. Aber **rechnerspezifische Dinge sind NICHT im Repo** und müssen separat
eingerichtet werden:

| Kommt per `git pull` (im Repo) | Muss pro Rechner eingerichtet werden (NICHT im Repo) |
|---|---|
| Launcher-Quellcode (`OpenCodeLauncher/`) | .NET 8 SDK (zum Bauen) |
| Alle Profil-Quellen (siehe unten) | OpenCode-Installation (`npm i -g opencode-ai`) |
| `create_shortcut.ps1`, `update-launcher.ps1` | Claude-Code-Installation + Login |
| | Globale `~/.config/opencode/opencode.jsonc` (Provider, `lsp`, Plugins, MCP) |
| | API-Keys als Umgebungsvariablen (aus `~/SK`) |
| | Claude-Login-Token (`.credentials.json` im Config-Ordner) |

---

## So laufen die Profile (Architektur)

**Grundprinzip:** Jedes Profil ist **genau EINE bearbeitbare Repo-Datei**. Der Launcher schreibt
deren Inhalt **vor jedem Start** in die Datei, die das jeweilige Werkzeug tatsächlich liest
(vorher wird sie geleert). Es gibt kein „Global + Projekt" mehr, keine Snapshots, kein Verstecken.

### OpenCode-Profile
- Quelle je Profil: `OpenCodeLauncher/Profiles/OpenCode/<id>/AGENTS.md` (`id` = `minimal` | `standard` | `strict`).
- Beim Start schreibt der Launcher diese Quelle in **`~/proggs/AGENTS.md`** (das Arbeitsverzeichnis).
  OpenCode liest die `AGENTS.md` im Arbeitsverzeichnis immer — deshalb steuern wir ihren Inhalt.
- Zusätzlich setzt der Launcher `OPENCODE_DISABLE_CLAUDE_CODE_PROMPT=1` für die Session → keine
  `CLAUDE.md` als Prompt-Fallback (die Projekt-`CLAUDE.md` wird ohnehin durch die daneben liegende
  `~/proggs/AGENTS.md` unterdrückt). **Skills (`~/.claude/skills`) und MCP bleiben nutzbar** — bewusst
  NICHT der komplette `OPENCODE_DISABLE_CLAUDE_CODE`, der auch die `.claude`-Skills abschalten würde.
- Die globale `~/.config/opencode/AGENTS.md` wird vom Launcher **leer** gehalten.
- `~/proggs/AGENTS.md` ist eine **Laufzeitdatei** (in `.gitignore`, wird immer neu erzeugt).

### Claude-Code-Profile
- Quelle je Profil: `OpenCodeLauncher/Profiles/ClaudeCode/sources/<id>.md`.
- Alle Profile teilen **einen** Config-Ordner (CLAUDE_CONFIG_DIR):
  `OpenCodeLauncher/Profiles/ClaudeCode/minimal/` — dort liegen Login-Token, `settings.json`,
  `skills/` usw. (ein Login für alle Profile).
- Beim Start schreibt der Launcher die gewählte Quelle in die **aktive `CLAUDE.md`** dieses Ordners.
  Diese aktive `CLAUDE.md` ist **untracked** (Laufzeit); versioniert sind nur die `sources/`.

### Profil-Editor
- Zeigt pro Profil **ein** Textfeld mit **Dateiname** (`AGENTS.md` bzw. `CLAUDE.md`) **+ Pfad**.
- „Speichern" schreibt in die jeweilige Repo-Quelle (`Profiles/...`), nicht in die aktive Datei.

### Versionierte Profil-Quellen (kommen per git)
```
OpenCodeLauncher/Profiles/OpenCode/minimal/AGENTS.md
OpenCodeLauncher/Profiles/OpenCode/standard/AGENTS.md
OpenCodeLauncher/Profiles/OpenCode/strict/AGENTS.md
OpenCodeLauncher/Profiles/ClaudeCode/sources/minimal.md
OpenCodeLauncher/Profiles/ClaudeCode/sources/standard.md
OpenCodeLauncher/Profiles/ClaudeCode/sources/strict.md
```

---

## Einrichtung Schritt für Schritt (neuer Rechner)

1. **Repo holen**
   ```
   git clone <proggs-Remote> ~/proggs      # oder: cd ~/proggs && git pull
   ```

2. **.NET 8 SDK installieren** (für den Build) — https://dotnet.microsoft.com/download

3. **Launcher bauen**
   ```
   dotnet build ~/proggs/OpenCodeLauncher/OpenCodeLauncher.csproj -c Release
   ```
   Ergebnis: `OpenCodeLauncher/bin/Release/net8.0-windows10.0.19041.0/win-x64/OpenCodeLauncher.exe`

4. **Desktop-Verknüpfung anlegen**
   ```
   pwsh ~/proggs/OpenCodeLauncher/create_shortcut.ps1
   ```
   (Updates später: `pwsh ~/proggs/OpenCodeLauncher/update-launcher.ps1` — schließt laufenden
   Launcher, baut Release, startet neu.)

5. **OpenCode installieren**
   ```
   npm install -g opencode-ai
   ```

6. **Globale OpenCode-Config einrichten:** `~/.config/opencode/opencode.jsonc`
   - Enthält deine **Provider/Modelle**, `"lsp": true` (Language-Server global an), Plugins, MCP.
   - **Nicht im Repo** (rechnerspezifisch, kann Keys via `{env:...}` referenzieren). Von einem
     eingerichteten Rechner kopieren oder neu anlegen. Mindestens:
     ```jsonc
     {
       "$schema": "https://opencode.ai/config.json",
       "lsp": true,
       "permission": { "*": "allow" },
       "provider": { /* deine Provider + Modelle */ }
     }
     ```
   - **Wichtig:** `OPENCODE_DISABLE_CLAUDE_CODE_PROMPT` musst du NICHT global setzen — das macht der
     Launcher pro OpenCode-Session automatisch.

7. **API-Keys als Umgebungsvariablen** (z. B. `OPENROUTER_API_KEY`, `OPENAI_API_KEY` …)
   - Quelle: `~/SK` (Secrets liegen dort, **nie im Repo**). Als User-Umgebungsvariablen setzen.

8. **Claude Code installieren + einloggen**
   - Claude Code installieren (siehe Anthropic-Doku).
   - Login läuft im gemeinsamen Config-Ordner. Der Launcher setzt `CLAUDE_CONFIG_DIR` beim Start
     auf `OpenCodeLauncher/Profiles/ClaudeCode/minimal/`. Einmalig dort einloggen (Token landet in
     `.credentials.json`, bleibt lokal/untracked).

9. **Skills** (optional, falls genutzt): `~/.claude/skills` bzw. OpenCode-Skills vom alten Rechner
   übernehmen. Sie werden nicht über dieses Repo verteilt.

---

## Was der Launcher zur Laufzeit selbst erzeugt (nicht sichern)
- `~/proggs/AGENTS.md` (aktives OpenCode-Profil) — ignoriert.
- `OpenCodeLauncher/Profiles/ClaudeCode/minimal/CLAUDE.md` (aktives Claude-Profil) — ignoriert.
- Sessions unter `%LOCALAPPDATA%/OpenCodeLauncher/sessions/` — temporär.

## Profile ändern
- Im Launcher Profil wählen → **Bearbeiten** → Text ändern → **Speichern** (schreibt die Repo-Quelle).
- Danach `git add/commit/push`, damit andere Rechner die Änderung per `git pull` bekommen.
