# Windows User-PATH Referenzliste (nach Shell-Updates pruefen)

> Ausgelagert aus `CLAUDE.md` (2026-07-08, verlustfrei). Pflicht-Kontext: Nach JEDEM Shell-Update
> (PowerShell, Git, Node, npm, Bun, Deno, Python, Claude Code CLI) den Windows User PATH pruefen —
> Shell-Updates koennen ihn zerstoeren. Ablauf: `~/.claude/rules/platform-and-paths.md` §4.

## Pruefbefehl

```powershell
pwsh -NoProfile -Command '[Environment]::GetEnvironmentVariable("PATH", "User")'
```

## Referenzliste — diese Verzeichnisse MUESSEN im User PATH stehen

```
%USERPROFILE%\bin                                          # python/python3 Wrapper
%USERPROFILE%\.local\bin                                   # uvx, pipx
%USERPROFILE%\.bun\bin                                     # bun
%USERPROFILE%\.cargo\bin                                   # rustc, cargo, cargo-audit etc.
%USERPROFILE%\AppData\Roaming\npm                          # biome, globale npm-Pakete
%USERPROFILE%\go\bin                                       # gomobile, gobind
C:\Gradle\gradle-9.4.1\bin                                 # gradle (Version bei Upgrade anpassen!)
C:\Kotlin\kotlinc\bin                                      # kotlinc, kotlin
%LOCALAPPDATA%\Android\Sdk\platform-tools                  # adb, fastboot
%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin        # sdkmanager, avdmanager
%LOCALAPPDATA%\Android\Sdk\emulator                        # emulator
```

## Zusaetzlich pruefen

- Env-Variablen gesetzt: `JAVA_HOME`, `ANDROID_HOME`, `GOPATH`.
- Fehlende Eintraege SOFORT wiederherstellen, nicht den Benutzer fragen.
- MCP-Server-Configs (`.mcp.json`) MUESSEN absolute Pfade verwenden, nie nackte Befehlsnamen
  wie "bun" oder "cargo".
- Reparatur-Werkzeug: `pwsh ~/.claude/hooks/path-verify.ps1 -Fix` (Windows) bzw.
  `bash ~/.claude/hooks/path-verify.sh --fix` (macOS).
