#!/bin/bash
# MCP-Server für den Chrome des Servers: Alle Sitzungen teilen den einen Browser, sonst liefe der Speicher voll.
export PATH="$HOME/.local/bin:$HOME/.local/node/bin:$PATH"
python3 "$HOME/proggs/OpenLauncherServer/kern/launcher.py" browser >&2 || exit 1
exec chrome-devtools-mcp --browserUrl http://127.0.0.1:9222
