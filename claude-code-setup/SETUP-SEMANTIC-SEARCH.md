# Semantische Code-Suche — Plattform-Setup

Dieses Dokument beschreibt die Einrichtung der semantischen Code-Suche auf allen Plattformen.
Claude Code liest dieses Dokument beim Setup und richtet alles automatisch ein.

## Architektur

```
~/proggs/mcp-code-search/     ← MCP-Server (TypeScript, laeuft mit Bun)
~/proggs/.code-search/        ← Vektor-Datenbank (SQLite + sqlite-vec)
  ├── current.txt             ← Pointer auf aktive DB (z.B. "index-3.db")
  ├── index-3.db              ← Aktive Datenbank
  └── .last-index-time        ← Zeitstempel des letzten Index-Laufs
~/.claude/hooks/reindex-codebase.sh (.ps1 auf Windows)  ← SessionStart-Hook
~/proggs/.mcp.json            ← MCP-Server-Registrierung
```

## Voraussetzungen

| Komponente | Wozu | Installation |
|---|---|---|
| **Bun** | Laufzeit fuer den MCP-Server | `curl -fsSL https://bun.sh/install \| bash` |
| **Ollama** | Embedding-Modell lokal ausfuehren | `brew install ollama` (macOS) / ollama.com (Windows) |
| **nomic-embed-text** | 768-dimensionale Embeddings | `ollama pull nomic-embed-text` |

## Setup-Schritte (Claude fuehrt diese automatisch aus)

### 1. Voraussetzungen pruefen
```bash
# Bun vorhanden?
bun --version

# Ollama laeuft?
curl -s http://localhost:11434/api/tags | grep nomic-embed-text
```

### 2. npm-Abhaengigkeiten installieren
```bash
cd ~/proggs/mcp-code-search
bun install
```

### 3. MCP-Server registrieren

**macOS** — In `~/proggs/.mcp.json` eintragen:
```json
{
  "mcpServers": {
    "code-search": {
      "command": "bun",
      "args": ["run", "~/proggs/mcp-code-search/src/index.ts"]
    }
  }
}
```

**Windows** — In `~/proggs/.mcp.json` eintragen:
```json
{
  "mcpServers": {
    "code-search": {
      "command": "bun",
      "args": ["run", "C:/Users/barwa/proggs/mcp-code-search/src/index.ts"]
    }
  }
}
```

WICHTIG: Falls bereits andere MCP-Server in .mcp.json stehen, den `code-search` Eintrag
ins bestehende `mcpServers`-Objekt einfuegen — NICHT die Datei ueberschreiben!

### 4. Reindex-Hook installieren

**macOS:**
```bash
cp ~/proggs/claude-code-setup/hooks/reindex-codebase.sh ~/.claude/hooks/
chmod +x ~/.claude/hooks/reindex-codebase.sh
```
Dann in `~/.claude/settings.json` unter `hooks.SessionStart` hinzufuegen:
```json
{
  "type": "command",
  "command": "bash ~/.claude/hooks/reindex-codebase.sh",
  "timeout": 300,
  "async": true
}
```

**Windows:**
```powershell
Copy-Item ~/proggs/claude-code-setup/hooks/reindex-codebase.ps1 ~/.claude/hooks/
```
Dann in `~/.claude/settings.json` unter `hooks.SessionStart` hinzufuegen:
```json
{
  "type": "command",
  "command": "powershell -ExecutionPolicy Bypass -File %USERPROFILE%\\.claude\\hooks\\reindex-codebase.ps1",
  "timeout": 300,
  "async": true
}
```

### 5. Erster Index-Lauf
```bash
# Manuell ausloesen (oder naechsten Claude-Session-Start abwarten)
cd ~/proggs/mcp-code-search
bun run src/index.ts  # Wird beim naechsten SessionStart automatisch passieren
```

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
| `nomic-embed-text` fehlt | `ollama pull nomic-embed-text` |
| `bun` nicht gefunden | PATH pruefen: `~/.bun/bin/` muss drin sein |
| Reindex-Hook lief nicht | `.last-index-time` loeschen, Session neu starten |
