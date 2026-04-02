# PATH-Verifikation nach Shell-Updates (KRITISCH)

## Regel

Nach JEDEM Shell/Terminal-Update MUSS die PATH-Verifikation laufen.
Shell-Updates koennen den PATH zerstoeren — das ist ein bekanntes,
wiederkehrendes Problem auf BEIDEN Plattformen.

## Wann die Verifikation PFLICHT ist

- Nach `brew upgrade` (besonders node, python, deno, powershell)
- Nach `npm update -g` (besonders claude-code CLI)
- Nach `rustup update`
- Nach `winget upgrade` auf Windows
- Nach jedem manuellen Tool-Update
- Nach jedem OS-Update

## Verifikations-Befehl

```bash
# macOS/Linux — Pruefung:
bash ~/.claude/hooks/path-verify.sh

# macOS/Linux — Pruefung + automatische Reparatur:
bash ~/.claude/hooks/path-verify.sh --fix

# Windows — Pruefung:
pwsh ~/.claude/hooks/path-verify.ps1

# Windows — Pruefung + automatische Reparatur:
pwsh ~/.claude/hooks/path-verify.ps1 -Fix
```

## Was geprueft wird

### Tools (muessen im PATH sein)
git, node, npm, bun, python3, rustc, cargo, go, claude, swift (macOS),
kotlin, gradle, deno, ollama (macOS), claudewatch

### Verzeichnisse (muessen im PATH sein)
**macOS:** /opt/homebrew/bin, ~/.cargo/bin, ~/go/bin, ~/.bun/bin
**Windows:** %USERPROFILE%\bin, %USERPROFILE%\.local\bin, %USERPROFILE%\.bun\bin,
%USERPROFILE%\.cargo\bin, %USERPROFILE%\AppData\Roaming\npm, %USERPROFILE%\go\bin,
C:\Gradle\gradle-*\bin, C:\Kotlin\kotlinc\bin, Android SDK Pfade

### Environment-Variablen
JAVA_HOME, ANDROID_HOME, GOPATH (Windows)

## Ablauf bei Shell-Updates (PFLICHT-Reihenfolge)

1. Alle anderen Aufgaben ZUERST abschliessen
2. Ergebnisse committen und pushen
3. Benutzer warnen ("Terminal-Neustart noetig")
4. Shell-Updates durchfuehren
5. **SOFORT danach: `bash ~/.claude/hooks/path-verify.sh --fix`**
6. Bei Problemen: Fehlende Pfade reparieren
7. Ergebnis dem Benutzer zeigen

## Warum

Shell-Updates (besonders brew upgrade, npm update -g) koennen den PATH
auf beiden Plattformen zerstoeren. Auf Windows ist das besonders haeufig
weil Updates manchmal den User PATH ueberschreiben. Auf macOS passiert es
seltener, aber brew kann Symlinks aendern die dann ins Leere zeigen.

Das path-verify Skript prueft ALLE kritischen Tools und Pfade und kann
fehlende Eintraege automatisch reparieren (--fix / -Fix Flag).
