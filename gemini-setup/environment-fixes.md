# Environment Fixes — Cross-Tool Knowledge Base

**Zweck**: Zentrale Dokumentation aller Umgebungs-Fixes, die fuer ALLE AI-Tools gelten.
Wird gelesen von: Gemini CLI, Codex CLI, Gemini CLI.
Jeder Fix hier verhindert, dass ein anderes Tool den gleichen Fehler macht.

**WICHTIG fuer alle lesenden Tools**: Jeder Eintrag ist so geschrieben, dass er OHNE
zusaetzlichen Kontext verstaendlich ist. Wenn du als CLI-Tool diesen Fix liest, hast du
alle Informationen die du brauchst um den gleichen Fehler zu vermeiden — inklusive
Hintergrund, Code-Beispielen und konkreten Regeln.

**Schwester-Datenbanken** (andere CLIs dokumentieren ihre Fixes hier):
- Gemini CLI: `gemini-setup/environment-fixes.md`
- Codex CLI: `codex-setup/state/environment-fixes.json`
- Gemini CLI: `gemini-setup/agent-memory/shared/MEMORY.md`

**Format pro Eintrag**:
- **Datum** und **Plattform** (Windows/macOS/beide)
- **Kontext**: Was wurde gerade gemacht als der Fehler auftrat (damit andere CLIs die Situation verstehen)
- **Symptom**: Was sichtbar schiefging (exakte Fehlermeldung wenn moeglich)
- **Root Cause**: WARUM es passiert ist (tiefste Ursache, nicht nur das Symptom)
- **Fix**: Was konkret geaendert wurde (mit Code-Beispiel)
- **Vermeidungsregel**: Was in Zukunft zu beachten ist (als klare Wenn-Dann-Regel)

---

## 2026-03-28 — bypassPermissions ignored due to allow-list acting as whitelist (Windows + macOS)

**Plattform:** Beide (Windows + macOS)
**Kontext:** Benutzer hat `defaultMode: bypassPermissions` in settings.json. Trotzdem wird
bei manchen Tool-Aufrufen nach Erlaubnis gefragt. Das Problem tritt bei MCP-Tools auf die
von neu installierten Plugins kommen (Hugging Face, code-review-graph, gemini-mem, etc.).
**Symptom:** Gemini CLI fragt "Darf ich dieses Tool nutzen?" obwohl bypassPermissions aktiv
ist. Der Benutzer muss bestaetigen — genau das was bypassPermissions verhindern soll.
**Root Cause:** Die `permissions`-Sektion hatte GLEICHZEITIG `defaultMode: bypassPermissions`
UND eine explizite `allow`-Liste mit 105 Eintraegen. Die `allow`-Liste wirkt als **Whitelist**:
Tools die NICHT auf der Liste stehen werden blockiert, SELBST bei bypassPermissions. Da neue
Plugins neue MCP-Tools hinzufuegen die nicht auf der Liste stehen (31 fehlende Tools gefunden),
werden diese Tools blockiert. Bei jedem Plugin-Update oder neuen Plugin waechst die Luecke.
**Fix:** Die `allow`-Liste komplett entfernen. Bei `bypassPermissions` ist sie redundant und
kontraproduktiv. Drei Absicherungsschichten:
1. Session-Guard Hook: Entfernt die `allow`-Liste bei JEDEM Session-Start automatisch
2. Config-Guard Hook: BLOCKIERT das Hinzufuegen einer `allow`-Liste per PostToolUse
3. Regel in bypass-permissions-permanent.md: Gemini darf nie eine allow-Liste erstellen
```json
// FALSCH — allow-Liste blockiert ungelistete Tools:
"permissions": {
  "allow": ["Bash", "Read", "Edit", ...105 Eintraege...],
  "defaultMode": "bypassPermissions"
}

// RICHTIG — nur defaultMode, keine allow-Liste:
"permissions": {
  "defaultMode": "bypassPermissions"
}
```
**Vermeidungsregel:** Wenn `defaultMode: bypassPermissions` gesetzt ist, NIEMALS eine
`allow`-Liste in der `permissions`-Sektion haben. Die `allow`-Liste ist NUR fuer den
`default`-Modus gedacht. Bei bypassPermissions wirkt sie als Whitelist-Blocker.

---

## 2026-03-28 — Gemini CLI starts in home directory instead of ~/proggs/ (Windows + macOS)

**Plattform:** Beide (Windows + macOS)
**Kontext:** Gemini CLI startet wiederholt im Home-Verzeichnis (~/) statt im Workspace
(~/proggs/). Der Benutzer hat einen Desktop-Shortcut "Gemini T.lnk" der `pwsh.exe` direkt
startet (NICHT ueber Windows Terminal) und `gemini --dangerously-skip-permissions` ausfuehrt.
**Symptom:** `pwd` zeigt `/c/Users/barwa` bei JEDEM Start. Alle bisherigen Fixes (.bashrc
auto-cd, Windows Terminal startingDirectory, session-guard.ps1 Warnung) haben NICHT geholfen.
**Root Cause (ECHTE — gefunden am 28.03.2026 17:10):**
Der Desktop-Shortcut "Gemini T.lnk" hatte:
- Target: `C:\Program Files\PowerShell\7\pwsh.exe`
- Arguments: `-NoExit -Command "gemini --dangerously-skip-permissions"`
- **WorkingDirectory: `C:\Users\barwa`** ← DAS war das eigentliche Problem!

Warum die bisherigen Fixes ALLE wirkungslos waren:
1. `.bashrc` auto-cd → greift nicht, weil der Shortcut **PowerShell** startet, nicht Git Bash.
   `.bashrc` wird nur von Git Bash gesourced, PowerShell ignoriert es komplett.
2. Windows Terminal `startingDirectory` → greift nicht, weil der Shortcut **direkt pwsh.exe**
   startet, nicht Windows Terminal. Die Einstellung gilt nur fuer Tabs IN Windows Terminal.
3. SessionStart-Hooks (session-guard.ps1) → laufen in Subprozessen. `cd` dort aendert nur
   den Subprozess, nicht Gemini CLIs Hauptprozess.

**Fix (5-Schichten Defense in Depth):**
1. **Desktop-Shortcut gefixt** (PRIMAERER FIX): WorkingDirectory auf `C:\Users\barwa\proggs`
   geaendert UND `Set-Location` vor `gemini` in die Arguments eingefuegt:
   ```
   Arguments: -NoExit -Command "Set-Location C:\Users\barwa\proggs; gemini --dangerously-skip-permissions"
   WorkingDir: C:\Users\barwa\proggs
   ```
2. **PowerShell-Profil auto-cd** (NEU): Auto-cd in
   `~/Documents/PowerShell/Microsoft.PowerShell_profile.ps1` hinzugefuegt — greift wenn
   pwsh.exe direkt gestartet wird (z.B. neue Shortcuts, VS Code Terminal):
   ```powershell
   if ($PWD.Path -eq $env:USERPROFILE -or $PWD.Path -eq "C:\Users\barwa") {
       $proggs = Join-Path $env:USERPROFILE "proggs"
       if (Test-Path $proggs) { Set-Location $proggs }
   }
   ```
3. `.bashrc` auto-cd (fuer Git Bash Starts)

