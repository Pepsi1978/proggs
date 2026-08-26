# Claude-Code-Profile für macOS

Dieser Bereich ist das **macOS-Pendant** zu `Profiles/ClaudeCode/` (Windows). Die macOS-Fassung des
Launchers (`~/proggs/OpenLauncherMac`) setzt einen dieser Ordner beim Start als `CLAUDE_CONFIG_DIR`.

## Warum getrennt von Windows?

Die Windows-Profile enthalten absolute Pfade (`C:\Users\barwa\…`) und PowerShell-Hooks
(`pwsh … .ps1`), die auf macOS nicht funktionieren. Statt einen brüchigen „kleinsten gemeinsamen
Nenner" zu bauen, hat macOS **eigene** Profile mit macOS-Pfaden (`/Users/frank/…`) und
`bash … .sh`-Hooks.

Inhaltlich sind sie **wortgleich** mit den Windows-Profilen: dieselben Regeln, Agents, Commands und
Skills, dieselben Profiltexte. Nur Pfade und Hook-Aufrufe sind übersetzt.

## Struktur (analog Windows)

| Ordner | Inhalt |
|--------|--------|
| `sources/` | Die drei Profiltexte (`minimal.md`, `standard.md`, `strict.md`). Ihr Inhalt landet beim Start in der aktiven `CLAUDE.md` des gewählten Profils. |
| `minimal/` | Regelfrei. Die Skills blendet der Launcher zur Laufzeit per **Symlink** auf `~/.claude/skills` ein (Windows braucht dafür eine Junction). |
| `standard/` | Versionierte, frei bearbeitbare `skills/ rules/ agents/ commands/` + `settings.json`. |
| `strict/` | Wie Standard, zusätzlich mit der vollständigen Hook-Kette aus `claude-code-setup/hooks-macos.json`. |

## Was wo herkommt

- **Profiltexte** (`sources/*.md`) — aus `Profiles/ClaudeCode/sources/`, Pfade auf macOS übersetzt
  (`C:\Users\barwa\SK` → `/Users/frank/SK` usw.).
- **`rules/ agents/ commands/ skills/`** — 1:1 aus dem jeweiligen Windows-Profil.
- **`settings.json`** — alle Einstellungen 1:1 aus Windows; **nur** der `hooks`-Block ist
  ausgetauscht: Strikt bekommt die bash-Hooks aus `claude-code-setup/hooks-macos.json`,
  Minimal und Standard den Emulator-Wächter als `.sh`.
- **Arbeitsmodi** liegen NICHT hier, sondern in `Profiles/WorkModes/` — die sind plattformneutral
  und werden mit Windows **geteilt**.

## Sicherheit

Die `.gitignore` jedes Profils ignoriert erst **alles** und schließt dann gezielt nur das
Versionierte wieder ein. Der Login-Token (`.credentials.json`), `projects/`, `sessions/` und
`cache/` werden dadurch garantiert nie eingecheckt. Die aktive `CLAUDE.md` ist bewusst nicht
eingeschlossen — sie ist eine Laufzeitdatei, die der Launcher bei jedem Start neu aus
`sources/<id>.md` befüllt.
