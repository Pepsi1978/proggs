---
name: claude-desktop-store-pfade
description: Claude Desktop ist die Store-Version; Konfiguration und Protokolle liegen nicht unter %APPDATA%\Claude
metadata:
  node_type: memory
  type: reference
  originSessionId: 68275041-763a-408d-8ec9-f00831bc49cc
  modified: 2026-10-09T17:27:06.157Z
---

Claude Desktop auf diesem PC = Store-Version (Paket `Claude_pzs8sxrjxfjjc`, Start: `explorer.exe shell:AppsFolder\Claude_pzs8sxrjxfjjc!Claude`).

- Konfiguration: `%LOCALAPPDATA%\Packages\Claude_pzs8sxrjxfjjc\LocalCache\Roaming\Claude\claude_desktop_config.json` (`%APPDATA%\Claude` gibt es nicht).
- Aktuelle MCP-Protokolle: `%LOCALAPPDATA%\Claude\logs\mcp-server-<name>.log` (der `logs`-Ordner neben der Konfiguration ist veraltet, Stand August 2026).
- Entfernte MCP-Server als stdio-Brücke: `cmd /c npx -y mcp-remote <url>` mit `NODE_OPTIONS=--dns-result-order=ipv4first` wegen [[http-timeouts-fallback-wartezeit]].
- Jarvis so eingetragen seit 09.10.2026, siehe [[jarvis-relay-und-chatgpt-plugin]]. Ändert sich die Relay-URL, auch hier nachziehen. Sicherungen: `~/SK/Jarvis/claude-desktop-*.json.bak`.
- Konfiguration wird nur beim Start gelesen: nur die `Claude.exe` unter `WindowsApps\Claude_*` beenden, nie die CLI-`claude.exe`.
