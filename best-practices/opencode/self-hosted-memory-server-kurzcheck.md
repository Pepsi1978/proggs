# Self-hosted Memory-Server für OpenCode UND Claude Code — Bauplan Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Frage | Antwort |
|-------|---------|
| Geht „ein Server, beide CLIs"? | **Ja.** Beide hängen sich an denselben MCP-Endpunkt (`…/mcp`): Claude Code via `mcpServers`/`claude mcp add`, OpenCode via `mcp { type:"remote" }`. |
| Beste Basis? | **supermemory self-hosted** (MIT, **Single-Binary**, kein Docker/Postgres nötig, lokale WASM-Embeddings, MCP-nativ, Port 6767). |
| Ist supermemory cloud-only? | **NEIN** (verbreiteter Irrtum). Offizieller Self-Host: `npx supermemory local` / `curl …/install`. |
| Bleiben die Daten wirklich lokal? | Embeddings ja (on-device WASM). **ABER** der LLM-Extraktions-/Summary-Step nutzt per Default `gpt-5.1` (Cloud) → für volle Datenhoheit auf **lokales Ollama** umbiegen (`OPENAI_BASE_URL`). |
| Server-Größe? | supermemory-Binary genügsam (RAM-Ceiling default 1 GB). Mit lokalem Ollama-LLM mehr RAM. Komfort: 8-GB-VPS. |
| Wichtigste Sicherheits-Falle | Port **NIE** an `0.0.0.0`; Docker umgeht UFW → an `127.0.0.1` binden + Reverse-Proxy (Caddy, TLS+Bearer) + Cloud-Firewall. |
