# mcp-code-search — Lokaler Semantischer Code-Such MCP-Server

Ein MCP-Server (Model Context Protocol) der lokale Codebases semantisch durchsuchbar macht.
Statt nach exakten Woertern zu suchen, versteht er die **Bedeutung** deiner Anfrage.

## Was macht das?

Stell dir vor, du suchst nach "Code der Datenbankverbindungen aufbaut" — aber die Funktion
heisst `createPool()` und enthaelt das Wort "Datenbank" gar nicht. Eine normale Textsuche
(grep) findet das nicht. Dieser Server schon, weil er den **Sinn** versteht.

## Technologie

| Komponente | Was es tut |
|-----------|-----------|
| **Node.js + tsx** | Startet den MCP-Server und die Reindex-CLI stabil mit `better-sqlite3` |
| **Ollama + nomic-embed-text** | Berechnet Embeddings lokal (768 Dimensionen) |
| **sqlite-vec** | Vektor-Datenbank fuer aehnlichkeitsbasierte Suche |
| **MCP SDK** | Protokoll damit Claude Code die Tools nutzen kann |

## Installation

### Voraussetzungen

1. **Node.js** installiert
2. **tsx** installiert (`npm install -g tsx`)
3. **Ollama** installiert und gestartet
4. Embedding-Modell: `ollama pull nomic-embed-text`

### Setup

```bash
cd ~/proggs/mcp-code-search
npm install
```

## Codex-Autostart

Fuer Codex CLI wird `code-search` separat in `~/.codex/config.toml` registriert. Der funktionierende Startbefehl ist:

```bash
codex mcp add code-search -- /opt/homebrew/bin/tsx /Users/frank/Codex/mcp-code-search/src/index.ts
```

Damit die Indexierung bei jedem `codex`-Start automatisch im Hintergrund geprueft wird, gibt es zusaetzlich:

- `src/reindex.ts`: prueft, ob seit `.last-index-time` relevante Dateien geaendert wurden
- inkrementelles Reindexing: nur geaenderte, neue oder geloeschte Dateien werden im Index ersetzt
- bestehende Indexdaten fuer unveraenderte Dateien bleiben erhalten und werden nicht neu eingebettet
- `index_codebase` nutzt denselben inkrementellen Pfad; ein Full Reindex bleibt nur fuer manuelle Wartung via `reindex.ts --full`
- atomarer Pointer-Swap ueber `current.txt`: der aktive Snapshot bleibt waehrend jedes Reindex aktiv, und `search_code`/`search_status` lesen zuerst diesen Pointer
- Startup-Guard gegen stale Runtimes: ein bereits offener Store wird neu gebunden, wenn `current.txt` inzwischen auf einen anderen Snapshot zeigt oder der geladene Pfad nicht mehr aktiv ist
- `state.json`: speichert den zuletzt erfolgreich aktivierten Index inklusive Zeitstempel und Modus
- `scripts/codex-auto-reindex.sh`: startet den Reindex-Check asynchron fuer das aktuelle Workspace-Root
- `~/.local/bin/codex`: triggert dieses Skript automatisch vor dem eigentlichen Codex-Start

Der Hintergrundlauf schreibt sein Protokoll nach `<workspace>/.code-search/reindex.log`.

## Benutzung (3 MCP-Tools)

- **index_codebase**: Indexiert alle Code-Dateien in einem Verzeichnis
- **search_code**: Semantische Suche per natuerlicher Sprache
- **search_status**: Zeigt Index-Status (Dateien, Chunks, DB-Pfad, letzter Laufmodus sowie letzter echter Schreiblauf mit `Last write mode` und `Last write at`)

## Unterstuetzte Sprachen

TypeScript, JavaScript, Python, Rust, Go, Kotlin, Java, C#, Swift, C/C++,
Markdown, JSON, YAML, TOML, XML, HTML, CSS, SQL, Shell, PowerShell, Groovy
