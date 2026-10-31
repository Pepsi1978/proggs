#!/bin/bash
# MCP-Server für den einen Chrome des Servers. Er spricht die Konsole an (Port 9221): Sie startet den Browser beim ersten echten Zugriff,
# auch nachdem er wegen Untätigkeit geschlossen wurde. Der Start einer CLI allein kostet so keinen Speicher.
export PATH="$HOME/.local/bin:$HOME/.local/node/bin:$PATH"
exec chrome-devtools-mcp --browserUrl http://127.0.0.1:9221 --no-usage-statistics --no-performance-crux --redact-network-headers
