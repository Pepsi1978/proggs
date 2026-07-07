# Multi-Client-Zugriff (MCP + REST-API) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (das Wichtigste zuerst)

| Signal / Frage | Sofort-Regel |
|----------------|--------------|
| Mehrere Clients, ein Store? | **MCP ist designbedingt client-agnostisch** — ein Server bedient Claude Code, OpenCode, Cursor, ChatGPT … gleichzeitig ("one memory, many clients") |
| Protokoll | MCP = JSON-RPC 2.0, drei Primitive: **Tools** (Aktionen), **Resources** (lesbare Daten), **Prompts** (Templates) |
| Eigene Apps (Voice etc.) | **Separate REST-Schicht** auf demselben Store (recall/store/context) — MCP ist NICHT REST; beide Wege parallel anbieten |
| Auth pragmatisch | jeder MCP-Client reicht Credentials anders durch → drei Wege gleichzeitig unterstützen: URL-Query `?key=`, Custom-Header (`x-brain-key`), Bearer-Token |
| Auth spec-konform | OAuth 2.1 (seit MCP 03/2025): Remote-MCP = OAuth Resource Server, `.well-known`-Metadata, Bearer-Validierung, Step-up für Hochrisiko-Tools |
| Credentials | eng scopen, dedizierte Keys mit Minimal-Rechten, Secrets als Env-Var nicht in Config (→ [[server-infrastruktur]]) |
| Remote-MCP-Falle | Transport wandert schnell (SSE → Streamable HTTP …); Session-State skaliert nicht stateless; **Session-404 nach Server-Neustart** (stateful Session-ID + Restart) → Re-Init nötig |
| Voice-App-Anbindung | STT → REST-`store`/`recall` → TTS-Bestätigung; Schreib-Call async (siehe [[schreibpfad-ingestion]] §5) |
