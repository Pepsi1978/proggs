# Semantic Search: Plattform-Isolation (KRITISCH)

## Regel: Semantische Suche ist NIEMALS plattformuebergreifend

Die semantische Suche (code-search MCP-Server, `.mcp.json`, Reindex-Hook) wird auf jedem
System **eigenstaendig** konfiguriert und verwaltet. Der Benutzer bearbeitet sie auf jeder
Plattform selbst.

### Was NIEMALS passieren darf

- **macOS → Windows**: Aenderungen an der semantischen Suche auf macOS duerfen NIEMALS
  die Windows-Konfiguration veraendern, ueberschreiben oder "synchronisieren".
- **Windows → macOS**: Aenderungen an der semantischen Suche auf Windows duerfen NIEMALS
  die macOS-Konfiguration veraendern, ueberschreiben oder "synchronisieren".
- **Keine "plattformuebergreifenden" Fixes**: Wenn die semantische Suche auf einer Plattform
  repariert wird, darf die andere Plattform NICHT angefasst werden.
- **Keine "Cross-Platform"-Optimierungen**: Keine Vereinheitlichung der Pfade, Befehle
  oder Konfigurationen zwischen den Plattformen.

### Betroffene Dateien (HANDS OFF fuer die jeweils andere Plattform)

| Datei | Beschreibung |
|-------|-------------|
| `~/proggs/.mcp.json` | Projekt-MCP-Server-Konfiguration (plattformspezifisch!) |
| `~/proggs/claude-code-setup/mcp-macos.json` | macOS-Referenz — nur auf macOS aendern |
| `~/proggs/claude-code-setup/mcp-windows.json` | Windows-Referenz — nur auf Windows aendern |
| `~/.claude/hooks/reindex-codebase.sh` | macOS-Reindex-Hook — nur auf macOS aendern |
| `~/.claude/hooks/reindex-codebase.ps1` | Windows-Reindex-Hook — nur auf Windows aendern |
| `~/proggs/.code-search/` | Lokale Index-Datenbank (nie synchronisieren) |
| `~/proggs/mcp-code-search/` | MCP-Server-Quellcode (plattformunabhaengig, darf geaendert werden) |

### Spezialfall: `.mcp.json` im Repo

Die `.mcp.json` liegt im Git-Repository und wird dadurch plattformuebergreifend synchronisiert.
Das ist das Kernproblem. Loesung:

- **Beim Commit von `.mcp.json`**: IMMER pruefen ob die Aenderung die andere Plattform
  kaputt machen koennte. Im Zweifel: NICHT committen und den Benutzer fragen.
- **Bei Self-Improve / automatischen Fixes**: `.mcp.json` NIEMALS automatisch aendern.
  Nur der Benutzer darf diese Datei aendern.
- **Bei Auto-Sync**: Die `.mcp.json` wird durch `git pull` automatisch ueberschrieben.
  Das ist ein bekanntes Risiko. Die `mcp-macos.json` und `mcp-windows.json` im Setup-Repo
  dienen als Backup fuer die jeweilige Plattform.

### Warum diese Regel existiert

Am 20./21.03.2026 wurde die `.mcp.json` auf Windows "plattformuebergreifend" gemacht,
indem absolute Windows-Pfade durch nackte Befehle ersetzt wurden. Das hat die semantische
Suche auf macOS kaputt gemacht, weil MCP-Server-Prozesse unter macOS absolute Pfade
brauchen (/opt/homebrew/bin/tsx statt tsx).
