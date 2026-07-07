<!-- DEPRECATED: 2026-03-25 — Replaced by Universal Mirror Bridge (mirror-ledger.md + export/import agents) -->
<!-- This file is kept as Codex-native audit metadata. -->

# Gemini CLI Delta Bridge (für Codex)

Diese Brücke erlaubt Codex, `gemini-setup/`-Deltas nur lesend zu prüfen und sinnvolle Muster Codex-native zu übernehmen.

Read-only Quellen:

- `gemini-setup/**`
- `gemini-setup/shared/MEMORY.md`
- `gemini-setup/state/implemented-intelligence-suggestions.json`

Codex schreibt niemals in Gemini-Verzeichnisse. Kandidaten werden als `ADD`, `ADAPT` oder `REPLACE` klassifiziert; REPLACE-Fälle brauchen Bestätigung.
