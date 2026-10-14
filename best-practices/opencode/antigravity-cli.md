# Antigravity CLI (agy) — Best Practices

> Stand 14.10.2026 · Anker: agy 1.3.5 (Windows x64), Gemini CLI 0.64.0 zum Vergleich ·
> Kurzcheck: `antigravity-cli-kurzcheck.md` · Bugs: `bugs/opencode/antigravity-gemini-cli.md`
> Markierung: [offiziell] = Google-Doku/Changelog, [extern] = Dritte, [geprüft] = am Binary/Bundle verifiziert.

## 1. Welche CLI für welches Konto
- [offiziell] Google AI Free/Pro/Ultra (privat) → **Antigravity CLI `agy`**. Gemini CLI und Code-Assist-Erweiterungen
  bedienen Privat-Abos seit 18.06.2026 nicht mehr (https://developers.googleblog.com/an-important-update-transitioning-gemini-cli-to-antigravity-cli/).
- [offiziell] Gemini CLI bleibt nutzbar mit bezahltem Gemini-API-Key, Vertex AI oder Code Assist Standard/Enterprise
  (Neuverkauf endet 09.10.2026, https://docs.cloud.google.com/gemini/docs/codeassist/sunset).
- [geprüft] Gemini CLI 0.64.0 lässt sich problemlos per `npm install -g @google/gemini-cli` installieren, scheitert aber
  mit Privatkonto am Login (Bug #1 im Almanach).

## 2. Installation und Update (Windows)
- [offiziell] `irm https://antigravity.google/cli/install.ps1 | iex` — legt `%LOCALAPPDATA%\agy\bin\agy.exe` ab,
  prüft SHA-512 gegen das Manifest, ruft `agy install` (PATH). Optionen `-d/--dir`, `--skip-path`.
  https://antigravity.google/docs/cli/install
- [offiziell] Kein npm-Paket (Go-Binary, closed source). winget „Google.AntigravityCLI“ nur [extern], nicht verwenden.
- [offiziell] Update: `agy update` (Installer aktualisiert nicht; Selbst-Update läuft im Hintergrund).

## 3. Start aus einem Launcher
- [geprüft] `agy --help` 1.3.5: `--model`, `--effort low|medium|high|xhigh|max`, `--mode accept-edits|auto|plan`,
  `--dangerously-skip-permissions`, `-p/--print`, `-i/--prompt-interactive`, `-c/--continue`, `--add-dir`, `--sandbox`.
- [offiziell] Seit 1.3.4 akzeptiert `--model` den Basisnamen ohne Effort-Suffix (`gemini-3.8-flash`, `gemini-4-argon`).
- Empfohlener Startbefehl: `agy --model <basis> --effort <stufe> --dangerously-skip-permissions` in einem eigenen
  Windows-Terminal-Reiter, Vollpfad `%LOCALAPPDATA%\agy\bin\agy.exe` (PATH evtl. noch nicht aktiv).
- `GEMINI_API_KEY` vor dem Start aus der Umgebung entfernen, sonst droht API-Key- statt Abo-Nutzung
  (nur zusammen mit `"modelProvider": "gemini"` wirksam, trotzdem sauberer).
- [offiziell] `--mode auto` ist unter Windows (1.3.5) noch nicht verfügbar.

## 4. Modelle und Denkstufen (https://antigravity.google/docs/models, 14.10.2026)
| Modell | API-/agy-ID | Plan | Stufen |
|--------|-------------|------|--------|
| Gemini 4 Argon | `gemini-4-argon` | nur AI Ultra (Pro kurzzeitig, dann gesperrt) | in agy Medium/High; API low/medium/high |
| Gemini 3.8 Flash | `gemini-3.8-flash` | Free/Plus/Pro/Ultra | low/medium/high (API: `minimal` → Fehler) |
| Gemini 3.1 Pro / 3.6 / 3.7 Flash | — | nur Enterprise | — |
- [offiziell] Gemini-API-Stand: `gemini-4-argon` GA seit 13.10.2026 (1 Mio Token Kontext), `gemini-3-pro-preview`
  abgeschaltet (→ 3.1 Pro), `gemini-3.5-flash`/`3.7-flash` seit 08.10.2026 deprecated. Kein 3.5/3.8 Pro.
- Echtes Angebot des eigenen Kontos: `agy models` (braucht Login).

## 5. Regeln, Konfig, Anmeldung
- [offiziell] Kontextdateien: `AGENTS.md` und `GEMINI.md` im Projekt (plus `.agents/AGENTS.md`, `.agents/rules/*.md`
  mit Pflicht-Frontmatter `trigger`), global `~/.gemini/AGENTS.md`/`GEMINI.md`, `~/.gemini/config/…`.
  https://antigravity.google/docs/rules
- Für Multi-CLI-Setups (Codex, Kimi, agy) genügt die Profil-`AGENTS.md` im Arbeitsordner.
- [offiziell] Konfig `~/.gemini/antigravity-cli/settings.json` (nur Abweichungen), u. a. `toolPermission`
  (`always-proceed` = dauerhaft keine Rückfragen), `model`, `trustedWorkspaces`. https://antigravity.google/docs/settings?tab=cli
- [geprüft] Konfig-Verzeichnis lässt sich nicht per `USERPROFILE`/`HOME` umlenken.
- [offiziell] Anmeldung: Browser-OAuth beim ersten Start, Token im Windows-Anmeldeinformationsspeicher; `/logout`.
  Vorhandene Gemini-CLI-Tokens werden beim ersten Start migriert.

## 6. Gemini CLI (falls doch mit API-Key genutzt)
- [geprüft] Kontextdatei per `context.fileName: ["AGENTS.md","GEMINI.md"]`; Suche in Unterordner mit
  `context.discoveryMaxDirs` begrenzen (Default 200 — in einem Monorepo sonst alle AGENTS.md).
- [geprüft] Denkstufe für 3.x per `modelConfigs.customOverrides` mit `match.model: "chat-base-3"` →
  `generateContentConfig.thinkingConfig.thinkingLevel`.
- [geprüft] Einstellungen in `~/.gemini/settings.json` statt System-Settings in `%TEMP%` (ACL-Prüfung, Almanach #2).
