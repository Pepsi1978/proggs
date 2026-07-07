# Semantische Code-Suche — Plattform-Setup

Dieses Dokument beschreibt die Einrichtung der semantischen Code-Suche auf allen Plattformen.
Gemini CLI liest dieses Dokument beim Setup und richtet alles automatisch ein.

## Architektur

```
~/proggs/mcp-code-search/     ← MCP-Server (TypeScript, laeuft mit tsx/Node.js)
~/proggs/.code-search/        ← Vektor-Datenbank (SQLite + sqlite-vec)
  ├── current.txt             ← Pointer auf aktive DB (z.B. "index-3.db")
  ├── index-3.db              ← Aktive Datenbank
  └── .last-index-time        ← Zeitstempel des letzten Index-Laufs
~/.Gemini/hooks/reindex-codebase.sh (.ps1 auf Windows)  ← SessionStart-Hook
~/proggs/.mcp.json            ← MCP-Server-Registrierung
~/.local/bin/codex           ← Optionaler Codex-Wrapper fuer automatischen Hintergrund-Reindex
```

## Voraussetzungen

| Komponente | Wozu | Installation |
|---|---|---|
| **Node.js** | Laufzeit fuer MCP + Native Addons | nodejs.org |
| **tsx** | TypeScript-Runner fuer Server und Reindex | `npm install -g tsx` |
| **Ollama** | Embedding-Modell lokal ausfuehren | `brew install ollama` (macOS) / ollama.com (Windows) |
| **snowflake-arctic-embed2** | 1024-dimensionale Embeddings | `ollama pull snowflake-arctic-embed2` |

## Setup-Schritte (Gemini fuehrt diese automatisch aus)

### 1. Voraussetzungen pruefen
```bash
# Node/tsx vorhanden?
node --version
tsx --version

# Ollama laeuft?
curl -s http://localhost:11434/api/tags | grep snowflake-arctic-embed2
```

### 2. npm-Abhaengigkeiten installieren
```bash
cd ~/proggs/mcp-code-search
npm install
```

### 3. MCP-Server registrieren

**macOS** — In `~/proggs/.mcp.json` eintragen:
```json
{
  "mcpServers": {
    "code-search": {
      "command": "/opt/homebrew/bin/tsx",
      "args": ["/Users/<user>/proggs/mcp-code-search/src/index.ts"]
    }
  }
}
```

**Windows** — In `~/proggs/.mcp.json` eintragen:
```json
{
  "mcpServers": {
    "code-search": {
      "command": "tsx",
      "args": ["C:/Users/barwa/proggs/mcp-code-search/src/index.ts"]
    }
  }
}
```

WICHTIG: Falls bereits andere MCP-Server in .mcp.json stehen, den `code-search` Eintrag
ins bestehende `mcpServers`-Objekt einfuegen — NICHT die Datei ueberschreiben!

### 4. Reindex-Hook installieren

**macOS:**
```bash
cp ~/proggs/gemini-setup/hooks/reindex-codebase.sh ~/.Gemini/hooks/
chmod +x ~/.Gemini/hooks/reindex-codebase.sh
```
Dann in `~/.Gemini/settings.json` unter `hooks.SessionStart` hinzufuegen:
```json
{
  "type": "command",
  "command": "bash ~/.Gemini/hooks/reindex-codebase.sh",
  "timeout": 300,
  "async": true
}
```

**Windows:**
```powershell
Copy-Item ~/proggs/gemini-setup/hooks/reindex-codebase.ps1 ~/.Gemini/hooks/
```
Dann in `~/.Gemini/settings.json` unter `hooks.SessionStart` hinzufuegen:
```json
{
  "type": "command",
  "command": "powershell -ExecutionPolicy Bypass -File %USERPROFILE%\\.Gemini\\hooks\\reindex-codebase.ps1",
  "timeout": 300,
  "async": true
}
```

### 5. Erster Index-Lauf
```bash
# Manuell ausloesen (oder naechsten Gemini-/Codex-Session-Start abwarten)
cd ~/proggs/mcp-code-search
tsx src/reindex.ts ~/proggs
```

## Codex CLI: automatischer Hintergrund-Reindex beim Start

Codex nutzt eine eigene MCP-Konfiguration in `~/.codex/config.toml`. Der MCP-Server kann so registriert werden:

```bash
codex mcp add code-search -- /opt/homebrew/bin/tsx /Users/<user>/proggs/mcp-code-search/src/index.ts
```

Wenn `codex` bei jedem Start automatisch pruefen soll, ob seit dem letzten Lauf Dateien geaendert wurden, ist ein Wrapper vor dem eigentlichen Binary der einfachste robuste Weg:

1. `~/.local/bin/codex` triggert `mcp-code-search/scripts/codex-auto-reindex.sh`
2. Das Skript bestimmt das Workspace-Root (`git rev-parse --show-toplevel`, sonst aktuelles Verzeichnis)
3. `src/reindex.ts` prueft `.last-index-time`, sperrt parallele Laeufe per `.reindex-lock` und schreibt eine neue `index-N.db`
4. Erst nach erfolgreichem Build wird `current.txt` auf die neue DB umgeschaltet

So bleibt die alte Suche waehrend einer Neuindexierung weiter benutzbar.

## Null-Ausfallzeit (Zero Downtime)

Der Hook und der MCP-Server nutzen ein **Pointer-basiertes System**:
- Beim Re-Indexieren wird eine NEUE Datenbank (`index-N+1.db`) geschrieben
- Waehrenddessen liest der MCP-Server weiterhin aus der ALTEN Datenbank
- Erst wenn die neue DB fertig ist, wird `current.txt` umgeschaltet
- Die alte DB wird danach aufgeraeumt (best-effort, uebersprungen wenn gesperrt)

Das bedeutet: **Suchen funktionieren immer**, auch waehrend des Re-Indexierens.

## Fehlerbehebung

| Problem | Loesung |
|---|---|
| `search_code` liefert keine Ergebnisse | `current.txt` pruefen: Zeigt auf existierende .db? |
| Ollama nicht erreichbar | `ollama serve` starten (macOS: laeuft als App) |
| `snowflake-arctic-embed2` fehlt | `ollama pull snowflake-arctic-embed2` |
| `bun` nicht gefunden | PATH pruefen: `~/.bun/bin/` muss drin sein |
| Reindex-Hook lief nicht | `.last-index-time` loeschen, Session neu starten |

