# claude-code-setup — Quelle der Claude-Code-Umgebung (`~/.claude`)

Seit 10.09.2026 verschlankt: Hier liegt nur noch, was aktiv genutzt wird. Codex- und Gemini-Brücken,
alte Anleitungen, Plugin-Kopien (die liegen in `Umgebung/Plugins`) und die frühere projektweite
`CLAUDE.md` sind entfernt. Regeln für Agenten kommen aus den OpenLauncher-Profilen
(`OpenLauncher/Profiles/ClaudeCode/sources/*.md`), Skills aus `OpenLauncher/Profiles/ClaudeCode/standard/skills`.

## Was hier liegt und wer es nutzt

| Pfad | Zweck | Genutzt von |
|------|-------|-------------|
| `hooks/` | Quelle aller Claude-Hooks (Statuszeile, Emulator-Wächter, Auto-Sync, Guards …) | Claude-Profile im OpenLauncher (`statusline.sh`, `emulator-start-guard`), `~/.claude/hooks` (per Auto-Sync) |
| `launcher/` | Windows-Terminal-Startskripte (`start-wt-common.ps1` …) | OpenLauncher (`OpenLauncherService.ResolveRobustLauncherScript`) |
| `docs/rules/` | Volltexte der Direktiven, u. a. `resilient-bugfixing.md` („Direktive 3“) | Profilregel 8, Skills, Regeln |
| `rules/`, `agents/`, `commands/` | Quelle für `~/.claude/rules`, `agents`, `commands` | Auto-Sync-Hook, Einrichtungsskripte |
| `settings*.json`, `hooks-*.json`, `mcp-*.json` | Referenz-Settings für `~/.claude` (Windows/macOS) | Einrichtung, second-brain-Tests |
| `setup-windows.ps1`, `setup-macos.sh`, `setup.ps1`, `setup.sh`, `manifest.json` | `~/.claude` auf einem neuen Rechner einrichten | manuell |
| `mirror-ledger.md` | Abgleich Windows ↔ Mac (Agenten `export`/`import`) | Spiegelregel `harness-mirror-on-change.md` |
| `environment-fixes.md`, `state/` | Umgebungs-Fixes und umgesetzte Vorschläge | Git-Hook `hooks/post-merge` |
| `scripts/`, `tools/` | Hilfsskripte (Spiegel-Check, Secret-Redaktion, Healthchecks) | Hooks, manuell |
| `launchd/`, `com.parry.daemon.plist` | macOS-Dienste (Heartbeat, Parry) | macOS |

## Neuer Rechner

```powershell
# Windows
cd ~/proggs/claude-code-setup; .\setup-windows.ps1
Copy-Item ~/proggs/claude-code-setup/settings-reference.json ~/.claude/settings.json
```

```bash
# macOS
cd ~/proggs/claude-code-setup && chmod +x setup-macos.sh && ./setup-macos.sh
cp ~/proggs/claude-code-setup/settings.json ~/.claude/settings.json
```

Sitzungen über den OpenLauncher brauchen das nicht: Er bringt je Profil eigene Settings mit.

## Statuszeile

`hooks/statusline.sh` zeigt Modell, Effort, Ordner, 5h-/7d-Limit, Kontext und Uhrzeit. Eingebunden in allen
Claude-Profilen des OpenLauncher (`"statusLine": { "command": "bash ~/proggs/claude-code-setup/hooks/statusline.sh" }`).
