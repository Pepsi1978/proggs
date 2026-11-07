---
name: claude-desktop-store-pfade
description: Claude Desktop ist die Store-Version; Konfiguration und Protokolle liegen nicht unter %APPDATA%\Claude
metadata:
  node_type: memory
  type: reference
  originSessionId: 68275041-763a-408d-8ec9-f00831bc49cc
  modified: 2026-10-09T17:27:06.157Z
---

Claude Desktop auf diesem PC ist die Store-Version (Paket `Claude_pzs8sxrjxfjjc`, Start über `explorer.exe shell:AppsFolder\Claude_pzs8sxrjxfjjc!Claude`).

- Konfiguration: `%LOCALAPPDATA%\Packages\Claude_pzs8sxrjxfjjc\LocalCache\Roaming\Claude\claude_desktop_config.json` (`%APPDATA%\Claude` gibt es nicht).
- Aktuelle MCP-Protokolle: `%LOCALAPPDATA%\Claude\logs\mcp-server-<name>.log` (der `logs`-Ordner neben der Konfiguration ist veraltet, Stand August 2026).
- Entfernte MCP-Server laufen dort als stdio-Brücke: `cmd /c npx -y mcp-remote <url>`, mit `NODE_OPTIONS=--dns-result-order=ipv4first` wegen [[http-timeouts-fallback-wartezeit]].
- Jarvis ist seit 09.10.2026 so eingetragen, siehe [[jarvis-relay-und-chatgpt-plugin]]. Ändert sich die Relay-URL, muss sie auch hier nachgezogen werden. Sicherungen der Konfiguration liegen in `~/SK/Jarvis/claude-desktop-*.json.bak`.
- Die Konfiguration wird nur beim Start gelesen: nur die `Claude.exe` unter `WindowsApps\Claude_*` beenden, nie die CLI-`claude.exe`.
