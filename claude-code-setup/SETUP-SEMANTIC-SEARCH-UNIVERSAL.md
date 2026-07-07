# Semantische Code-Suche — Universelle Installationsanleitung

**Fuer**: Claude Code, Codex CLI, Gemini CLI und jedes andere AI-CLI
**Plattformen**: macOS und Windows
**Stand**: 2026-03-23

Diese Anleitung ist so geschrieben, dass JEDES AI-CLI sie ohne Session-Kontext
verstehen und ausfuehren kann. Es gibt keine CLI-spezifischen Voraussetzungen.

---

## Was ist das?

Ein lokaler MCP-Server der die gesamte Codebasis semantisch durchsuchbar macht.
Statt exakter Text-Suche (grep) kann man natuerlichsprachliche Fragen stellen:

```
"Wo wird die Bridge-Architektur implementiert?"
"Welche Hooks laufen beim Session-Start?"
"Wie funktioniert das Whiteboard-Insert?"
```

Der Server laeuft vollstaendig lokal — keine Cloud, keine API-Kosten, keine Latenz.

**Technologie-Stack**:
- **Ollama** — lokale LLM-Laufzeit (fuehrt das Embedding-Modell aus)
- **snowflake-arctic-embed2** — Embedding-Modell (1024 Dimensionen, 1.2 GB, 8192 Token Context)
- **SQLite + sqlite-vec** — Vektor-Datenbank (speichert die Embeddings)
- **MCP-Server** — TypeScript-Server der die 3 Tools bereitstellt
- **tsx** — TypeScript-Runner (fuehrt den Server und den Indexer aus)

**Tools die der MCP-Server bereitstellt**:
- `index_codebase` — Indexiert ein Verzeichnis (manuell oder automatisch)
- `search_code` — Natuerlichsprachliche Code-Suche
- `search_status` — Zeigt den Index-Status (Dateien, Chunks, letzte Indexierung)

---

## Architektur

```
~/proggs/                          ← Git-Repository (das wird indexiert)
├── mcp-code-search/               ← MCP-Server Quellcode
│   ├── src/
│   │   ├── index.ts               ← MCP-Server Hauptdatei
│   │   ├── reindex.ts             ← Reindex-Script (standalone aufrufbar)
│   │   ├── reindex-core.ts        ← Reindex-Logik
│   │   ├── store.ts               ← Vektor-Store (SQLite + sqlite-vec)
│   │   ├── indexer.ts             ← Datei-zu-Chunk Konverter
│   │   ├── ollama.ts              ← Ollama-Client (Embedding-API)
│   │   └── db-state.ts            ← Datenbank-Pointer-Verwaltung
│   ├── package.json
│   └── node_modules/
├── .code-search/                   ← Vektor-Datenbank (wird erzeugt)
│   ├── current.txt                 ← Pointer auf aktive DB ("index-14.db")
│   ├── index-14.db                 ← Aktive Datenbank (~70 MB fuer 659 Dateien)
│   ├── .last-index-time            ← Zeitstempel der letzten Indexierung
│   └── .reindex.lock               ← Lock-File gegen parallele Reindex-Laeufe
└── .mcp.json                       ← MCP-Server-Registrierung
```

**Zero-Downtime-Prinzip**: Beim Re-Indexieren wird eine NEUE Datenbank geschrieben
(z.B. `index-15.db`). Der MCP-Server liest weiterhin aus der ALTEN (`index-14.db`).
Erst wenn die neue DB komplett fertig ist, wird `current.txt` umgeschaltet.
Suchen funktionieren IMMER, auch waehrend des Re-Indexierens.

---

## Schritt 1: Voraussetzungen installieren

### Node.js + tsx (TypeScript-Runner)

Der MCP-Server ist in TypeScript geschrieben und braucht Node.js + tsx.

**macOS:**
```bash
# Node.js (falls nicht vorhanden)
brew install node

# tsx (TypeScript-Runner)
npm install -g tsx

# Verifizieren
node --version    # mindestens v18
tsx --version     # mindestens v4
```

**Windows:**
```powershell
# Node.js: https://nodejs.org herunterladen und installieren
# oder via winget:
winget install OpenJS.NodeJS.LTS

# tsx
npm install -g tsx

# Verifizieren
node --version
tsx --version
```

### Ollama (lokale Embedding-Laufzeit)

Ollama fuehrt das Embedding-Modell lokal aus. Keine Cloud, keine API-Keys.

**macOS:**
```bash
brew install ollama

# Ollama starten (laeuft als Hintergrund-Service)
ollama serve &
# ODER: Ollama.app starten (hat GUI + Auto-Start)

# Embedding-Modell herunterladen (einmalig, ~274 MB)
ollama pull snowflake-arctic-embed2

# Verifizieren
curl -s http://localhost:11434/api/tags | grep snowflake-arctic-embed2
```

**Windows:**
```powershell
# Ollama herunterladen: https://ollama.com/download/windows
# Installer ausfuehren — startet automatisch als Dienst

# Embedding-Modell herunterladen
ollama pull snowflake-arctic-embed2

# Verifizieren
curl -s http://localhost:11434/api/tags
```

**WICHTIG**: Ollama MUSS laufen bevor der MCP-Server oder der Reindex startet.
Der Reindex-Hook prueft das automatisch und startet Ollama bei Bedarf, aber
fuer die Erstinstallation muss Ollama einmal manuell gestartet werden.

---

## Schritt 2: MCP-Server Abhaengigkeiten installieren

```bash
cd ~/proggs/mcp-code-search
npm install
```

Das installiert:
- `@modelcontextprotocol/sdk` — MCP-Protokoll
- `better-sqlite3` — SQLite Native-Addon
- `sqlite-vec` — Vektor-Erweiterung fuer SQLite
- `glob` — Datei-Pattern-Matching
- `zod` — Schema-Validierung

**Hinweis**: `better-sqlite3` ist ein Native-Addon das beim `npm install`
kompiliert wird. Dafuer braucht es einen C++-Compiler:
- macOS: Xcode Command Line Tools (`xcode-select --install`)
- Windows: Visual Studio Build Tools (`npm install -g windows-build-tools`)

---

## Schritt 3: MCP-Server registrieren

Der MCP-Server muss in der `.mcp.json` des Projekts registriert werden.
Die `.mcp.json` liegt im Root des Git-Repositories.

**WICHTIG**: Absolute Pfade verwenden! Nackte Befehlsnamen wie `tsx` oder `bun`
funktionieren bei manchen CLI-Tools nicht, weil der PATH nicht immer geladen ist.

### macOS

Datei: `~/proggs/.mcp.json`

```json
{
  "mcpServers": {
    "code-search": {
      "command": "/opt/homebrew/bin/tsx",
      "args": ["/Users/DEIN_BENUTZERNAME/proggs/mcp-code-search/src/index.ts"]
    }
  }
}
```

Ersetze `DEIN_BENUTZERNAME` durch deinen macOS-Benutzernamen.

Pfad zu tsx finden: `which tsx` (typisch: `/opt/homebrew/bin/tsx`)

### Windows

Datei: `~/proggs/.mcp.json`

```json
{
  "mcpServers": {
    "code-search": {
      "command": "C:/Users/DEIN_BENUTZERNAME/.bun/bin/bun.exe",
      "args": ["run", "C:/Users/DEIN_BENUTZERNAME/proggs/mcp-code-search/src/index.ts"]
    }
  }
}
```

**Alternativ mit tsx statt bun:**
```json
{
  "mcpServers": {
    "code-search": {
      "command": "tsx",
      "args": ["C:/Users/DEIN_BENUTZERNAME/proggs/mcp-code-search/src/index.ts"]
    }
  }
}
```

**ACHTUNG**: Die `.mcp.json` liegt im Git-Repo und wird dadurch plattformuebergreifend
synchronisiert. Das ist ein bekanntes Problem — macOS-Pfade funktionieren nicht auf
Windows und umgekehrt. Loesung: Jede Plattform hat eine Referenz-Kopie:
- macOS: `claude-code-setup/mcp-macos.json`
- Windows: `claude-code-setup/mcp-windows.json`

Nach einem `git pull` muss die `.mcp.json` eventuell an die lokale Plattform angepasst werden.

### Codex CLI

Codex nutzt eine eigene Konfiguration in `~/.codex/` (nicht `.mcp.json`):

```bash
# MCP-Server bei Codex registrieren
codex mcp add code-search -- /opt/homebrew/bin/tsx /Users/DEIN_BENUTZERNAME/proggs/mcp-code-search/src/index.ts
```

Oder manuell in der Codex-Config (`~/.codex/config.toml` oder aehnlich) eintragen.

### Gemini CLI

Gemini CLI liest entweder die `.mcp.json` im Projektverzeichnis oder hat eine
eigene MCP-Konfiguration. Pruefe die Gemini-Dokumentation fuer das exakte Format.
Der MCP-Server selbst ist identisch — nur der Registrierungsweg unterscheidet sich.

---

## Schritt 4: Erster Index-Lauf

```bash
# Manuell ausfuehren (einmalig)
cd ~/proggs/mcp-code-search
tsx src/reindex.ts ~/proggs
```

Der erste Lauf dauert je nach Codebase-Groesse 1-5 Minuten. Er:
1. Findet alle Code-Dateien (ts, kt, rs, go, cs, swift, py, js, json, md, yaml, ps1, sh)
2. Zerlegt sie in Chunks (~50 Zeilen pro Chunk)
3. Generiert fuer jeden Chunk ein 768-dimensionales Embedding via Ollama
4. Speichert alles in einer SQLite-Datenbank

**Ergebnis** (fuer dieses Repo):
```
Found 678 code files
Reindex complete: 659 files, 11052 chunks
Database: ~/proggs/.code-search/index-14.db (70 MB)
```

---

## Schritt 5: Automatischer Reindex bei Session-Start (optional aber empfohlen)

### Claude Code

Der Reindex-Hook ist bereits installiert und laeuft async bei jedem SessionStart.
Er prueft ob sich Dateien seit der letzten Indexierung geaendert haben und
reindexiert nur die geaenderten Dateien (inkrementell).

Konfiguration in `~/.claude/settings.json`:
```json
{
  "hooks": {
    "SessionStart": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "bash ~/.claude/hooks/reindex-codebase.sh",
            "timeout": 300,
            "async": true
          }
        ]
      }
    ]
  }
}
```

### Codex CLI

Codex hat keinen nativen SessionStart-Hook. Zwei Alternativen:

**Option A: Wrapper-Script** (empfohlen)
Ein Wrapper unter `~/.local/bin/codex` der vor dem echten Codex den Reindex triggert:
```bash
#!/usr/bin/env bash
# ~/.local/bin/codex — Wrapper fuer automatischen Reindex
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
MCP_DIR="$REPO_ROOT/mcp-code-search"
if [ -f "$MCP_DIR/src/reindex.ts" ]; then
  tsx "$MCP_DIR/src/reindex.ts" "$REPO_ROOT" >/dev/null 2>&1 &
fi
exec /usr/local/bin/codex "$@"  # Pfad zum echten Codex-Binary
```

**Option B: Manuell** bei Bedarf:
```bash
tsx ~/proggs/mcp-code-search/src/reindex.ts ~/proggs
```

### Gemini CLI

Gemini CLI kann Hooks oder Pre-Session-Scripts unterstuetzen — pruefe die
Gemini-Dokumentation. Alternativ manuell reindexieren vor wichtigen Sessions.

---

## Schritt 6: Verifizierung

### Per MCP-Tool (wenn der Server laeuft)

```
search_status(directory: "/Users/DEIN_BENUTZERNAME/proggs")
```

Erwartete Ausgabe:
```
- Files indexed: 659
- Code chunks: 11052
- Database: .../index.db
- Ollama model: snowflake-arctic-embed2 (1024 dimensions, 8192 token context)
- Last successful reindex: 2026-03-23T18:56:44+01:00
- Last mode: incremental
```

### Per Kommandozeile

```bash
# Pruefen ob der Index existiert
cat ~/proggs/.code-search/current.txt
# Sollte z.B. "index-14.db" ausgeben

# Pruefen ob die DB Inhalt hat
ls -lh ~/proggs/.code-search/$(cat ~/proggs/.code-search/current.txt)
# Sollte ~70 MB zeigen

# Pruefen ob Ollama laeuft
curl -s http://localhost:11434/api/tags | grep snowflake-arctic-embed2
```

---

## Plattform-Unterschiede (Referenz)

| Aspekt | macOS | Windows |
|--------|-------|---------|
| tsx-Pfad | `/opt/homebrew/bin/tsx` | `tsx` im PATH oder `%APPDATA%\npm\tsx.cmd` |
| Ollama-Pfad | `/opt/homebrew/bin/ollama` | `C:\Users\<user>\AppData\Local\Programs\Ollama\ollama.exe` |
| Ollama-Start | `ollama serve &` oder Ollama.app | Startet als Windows-Dienst automatisch |
| npm-Pfad | `/opt/homebrew/bin/npm` | `npm` im PATH |
| C++-Compiler | Xcode CLT (`xcode-select --install`) | VS Build Tools |
| Typischer DB-Pfad | `~/proggs/.code-search/index-N.db` | `C:\Users\<user>\proggs\.code-search\index-N.db` |
| Alternative Runtime | tsx (empfohlen) | bun (schneller) oder tsx |
| Reindex-Hook | `.sh` (bash) | `.ps1` (PowerShell) |

---

## Fehlerbehebung

| Problem | Ursache | Loesung |
|---------|---------|---------|
| `search_code` liefert 0 Ergebnisse | Index existiert nicht oder ist leer | `tsx src/reindex.ts ~/proggs` ausfuehren |
| `search_code` liefert veraltete Ergebnisse | Reindex lief nicht | `.last-index-time` loeschen, Reindex ausfuehren |
| "Ollama not reachable" | Ollama laeuft nicht | `ollama serve` starten |
| "snowflake-arctic-embed2 not found" | Modell nicht heruntergeladen | `ollama pull snowflake-arctic-embed2` |
| `npm install` schlaegt fehl (better-sqlite3) | Kein C++-Compiler | macOS: `xcode-select --install`, Windows: VS Build Tools |
| Reindex haengt (>30 Min) | Lock-File blockiert | `.code-search/.reindex.lock` loeschen |
| "SQLITE_ERROR: no such module: vec0" | sqlite-vec nicht installiert | `cd mcp-code-search && npm install` |
| DB waechst unkontrolliert | Alte DBs werden nicht aufgeraeumt | Nur die in `current.txt` referenzierte DB behalten, Rest loeschen |
| `.mcp.json` Pfade stimmen nicht | Plattform gewechselt (macOS↔Windows) | `mcp-macos.json` oder `mcp-windows.json` als Referenz nutzen |

---

## Indexierte Dateitypen

Der Indexer erfasst folgende Dateiendungen:
`.ts`, `.kt`, `.rs`, `.go`, `.cs`, `.swift`, `.py`, `.js`, `.json`, `.md`,
`.yaml`, `.ps1`, `.sh`

Ausgeschlossene Verzeichnisse:
`node_modules`, `.git`, `.gradle`, `build`, `dist`, `target`, `.cache`, `.code-search`

---

## Performance-Richtwerte

| Metrik | Wert (dieses Repo) |
|--------|-------------------|
| Dateien | 659 |
| Code-Chunks | 11.052 |
| DB-Groesse | 70 MB |
| Erster Index (Full) | ~3-5 Minuten |
| Inkrementeller Reindex (21 Dateien) | ~15-30 Sekunden |
| Embedding-Modell Speicher | 274 MB (RAM, einmalig beim ersten Aufruf) |
| Such-Latenz | <500ms pro Query |

---

## Fuer Codex und Gemini: Was ihr NICHT tun muesst

- Ihr muesst den MCP-Server-Code NICHT aendern — er ist CLI-neutral
- Ihr muesst die `.code-search/` Datenbank NICHT synchronisieren — jede Plattform hat ihre eigene
- Ihr muesst den Reindex-Hook NICHT identisch uebernehmen — passt ihn an eure Hook-Architektur an
- Ihr MUESST die `.mcp.json` an eure Plattform anpassen (absolute Pfade!)
- Ihr MUESST Ollama + snowflake-arctic-embed2 installiert haben bevor der Server startet

---

## Dateien im Repository

| Datei | Zweck |
|-------|-------|
| `mcp-code-search/src/index.ts` | MCP-Server (3 Tools: index, search, status) |
| `mcp-code-search/src/reindex.ts` | Standalone Reindex-Script |
| `mcp-code-search/src/reindex-core.ts` | Reindex-Logik (Full + Inkrementell) |
| `mcp-code-search/src/store.ts` | SQLite Vektor-Store |
| `mcp-code-search/src/indexer.ts` | Datei-zu-Chunk Konverter |
| `mcp-code-search/src/ollama.ts` | Ollama Embedding-Client |
| `mcp-code-search/src/db-state.ts` | Datenbank-Pointer (current.txt) Verwaltung |
| `mcp-code-search/package.json` | Abhaengigkeiten |
| `claude-code-setup/mcp-macos.json` | MCP-Config Referenz fuer macOS |
| `claude-code-setup/mcp-windows.json` | MCP-Config Referenz fuer Windows |
| `claude-code-setup/hooks/reindex-codebase.sh` | Reindex-Hook fuer macOS |
| `claude-code-setup/hooks/reindex-codebase.ps1` | Reindex-Hook fuer Windows |
