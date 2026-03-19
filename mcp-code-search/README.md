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
| **Bun** | Schnelle TypeScript-Runtime |
| **Ollama + nomic-embed-text** | Berechnet Embeddings lokal (768 Dimensionen) |
| **sqlite-vec** | Vektor-Datenbank fuer aehnlichkeitsbasierte Suche |
| **MCP SDK** | Protokoll damit Claude Code die Tools nutzen kann |

## Installation

### Voraussetzungen

1. **Bun** installiert
2. **Ollama** installiert und gestartet
3. Embedding-Modell: `ollama pull nomic-embed-text`

### Setup

```bash
cd ~/proggs/mcp-code-search
bun install
```

## Benutzung (3 MCP-Tools)

- **index_codebase**: Indexiert alle Code-Dateien in einem Verzeichnis
- **search_code**: Semantische Suche per natuerlicher Sprache
- **search_status**: Zeigt Index-Status (Dateien, Chunks, DB-Pfad)

## Unterstuetzte Sprachen

TypeScript, JavaScript, Python, Rust, Go, Kotlin, Java, C#, Swift, C/C++,
Markdown, JSON, YAML, TOML, XML, HTML, CSS, SQL, Shell, PowerShell, Groovy
