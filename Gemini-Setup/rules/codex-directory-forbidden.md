# Codex-Verzeichnis — VERBOTEN (KRITISCH)

## Regel: ~/Codex ist GESPERRT

Das Verzeichnis `~/Codex/` ist ein separater Klon des gleichen Repos (`Pepsi1978/proggs`), der **ausschliesslich von Codex im Terminal** genutzt wird.

### Plattform-Pfade
- **Windows**: `C:\Users\barwa\Codex\`
- **macOS**: `/Users/barwa/Codex/`

### Was NIEMALS passieren darf
- ❌ Dateien in `~/Codex/` lesen
- ❌ Dateien in `~/Codex/` schreiben oder bearbeiten
- ❌ Befehle mit `cd ~/Codex` ausfuehren
- ❌ Git-Operationen in diesem Verzeichnis ausfuehren
- ❌ Pfade die auf `/Codex/` oder `\Codex\` zeigen in irgendeinem Tool verwenden

### Warum
Der Benutzer arbeitet dort parallel mit Codex. Jeder Zugriff durch Claude Code kann laufende Arbeit ueberschreiben oder Konflikte verursachen.

### Korrektes Arbeitsverzeichnis
Immer `~/proggs/` verwenden — das ist das einzige Arbeitsverzeichnis fuer Claude Code.
- **Windows**: `C:\Users\barwa\proggs\`
- **macOS**: `/Users/barwa/proggs/`
