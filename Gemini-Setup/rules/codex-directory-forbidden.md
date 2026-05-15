# Codex-Verzeichnis ÔÇö VERBOTEN (KRITISCH)

## Regel: ~/Codex ist GESPERRT

Das Verzeichnis `~/Codex/` ist ein separater Klon des gleichen Repos (`Pepsi1978/proggs`), der **ausschliesslich von Codex im Terminal** genutzt wird.

### Plattform-Pfade
- **Windows**: `C:\Users\barwa\Codex\`
- **macOS**: `/Users/barwa/Codex/`

### Was NIEMALS passieren darf
- ÔØî Dateien in `~/Codex/` lesen
- ÔØî Dateien in `~/Codex/` schreiben oder bearbeiten
- ÔØî Befehle mit `cd ~/Codex` ausfuehren
- ÔØî Git-Operationen in diesem Verzeichnis ausfuehren
- ÔØî Pfade die auf `/Codex/` oder `\Codex\` zeigen in irgendeinem Tool verwenden

### Warum
Der Benutzer arbeitet dort parallel mit Codex. Jeder Zugriff durch Claude Code kann laufende Arbeit ueberschreiben oder Konflikte verursachen.

### Korrektes Arbeitsverzeichnis
Immer `~/proggs/` verwenden ÔÇö das ist das einzige Arbeitsverzeichnis fuer Claude Code.
- **Windows**: `C:\Users\barwa\proggs\`
- **macOS**: `/Users/barwa/proggs/`
