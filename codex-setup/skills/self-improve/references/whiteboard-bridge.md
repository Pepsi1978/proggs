# Whiteboard Bridge for Codex

Codex has its own whiteboard and its own bridge layer.
This whiteboard is the central nervous system of the Codex setup.
The bridge exists so that skills, agents, scripts, and MCP-backed workflows can contribute
to one shared intelligence layer without falling back to Claude/proggs memory.
Every whiteboard-connected component must read the whiteboard section `## Oberste Direktive`
before acting on the rest of the system state.

## Authoritative Whiteboard

```text
<workspace>/codex-setup/agent-memory/shared/MEMORY.md
```

## Bridge Components

- `codex-setup/scripts/whiteboard-bridge.mjs`
  Shared implementation for memory resolution, integrity checks, directive-token generation,
  section inserts, and sentinel merges.

- `codex-setup/scripts/whiteboard-insert.sh`
- `codex-setup/scripts/whiteboard-insert.ps1`
  Thin wrappers for inserting one entry into one named section.

- `codex-setup/scripts/whiteboard-bridge.mjs print-directive`
  Read the authoritative top directive from the Codex whiteboard for attached tools and workflows.

- `codex-setup/scripts/whiteboard-bridge.mjs directive-token`
  Read the current `## Oberste Direktive` and derive a write token from it.

- `codex-setup/scripts/writeback-enforcer.sh`
- `codex-setup/scripts/writeback-enforcer.ps1`
  Merge `agent-writeback-*.json` sentinel files into the Codex whiteboard.

## Bridge Rules

- Attached components should read `## Oberste Direktive` first, then the rest of the whiteboard as needed.
- Wrappers should pass `--workspace <repo-root>` explicitly instead of relying on the caller's current directory.
- Write operations must use a fresh directive token derived from the current `## Oberste Direktive`.
- Skills write directly only through the insert wrapper.
- Agents and researchers should prefer sentinel JSON write-back.
- The coordinator merges sentinels after agents finish.
- MCP servers do not write to the whiteboard directly; their findings are summarized by the caller.
- Direct append-to-file patterns are forbidden.
- The bridge must fail closed outside a Codex workspace; there is no home-directory fallback whiteboard.
- The bridge validates the whiteboard structure before reads and writes and rejects duplicate, missing, or reordered required sections.

## Sentinel Format

```json
{
  "agent": "env-checker",
  "timestamp": "2026-03-22T10:00:00Z",
  "section": "Systemzustand",
  "findings": "Kurze 1-Zeilen-Zusammenfassung"
}
```

`section` is optional. If omitted, the writeback enforcer maps by `agent`.

## Sections

The bridge expects the standard Codex sections in `MEMORY.md`:
- Oberste Direktive
- Offene Fehler & Probleme
- Systemzustand
- Erkenntnisse aus Code Reviews
- Erkenntnisse aus Tests
- Architektur-Entscheidungen
- Debugging-Muster
- Performance & Optimierung
- UI/UX-Patterns
- Forschung & Intelligence
- Regeln & Konventionen
