# Hook-Einschraenkungen (KRITISCH)
<!-- Adapted from MIRROR-2026-03-25-MAC-017 by import agent on 2026-03-26 -->

## Regel 1: KEINE Prompt-Hooks bei SessionStart

`type: "prompt"` Hooks duerfen NIEMALS im `SessionStart` Event verwendet werden.

**Grund:** Claude Code hat bei SessionStart keinen ToolUseContext. Prompt-Hooks brauchen
diesen Kontext um zu funktionieren. Der Fehler ist:
`Failed to run: ToolUseContext is required for prompt hooks. This is a bug.`

**Wo Prompt-Hooks funktionieren:**
- PreToolUse, PostToolUse, PostToolUseFailure ✅
- PreCompact, PostCompact ✅
- Stop, StopFailure ✅
- SubagentStop ✅
- InstructionsLoaded ✅

**Wo Prompt-Hooks NICHT funktionieren:**
- SessionStart ❌ (kein ToolUseContext)
- SessionEnd ❌ (Session wird beendet, kein Kontext mehr)

**Alternative:** Statt `type: "prompt"` bei SessionStart immer `type: "command"` verwenden
und die Logik in ein Bash/PowerShell-Skript auslagern.

## Regel 2: Plugin-Hook-Skripte brauchen Execute-Permission

Plugins koennen `.sh`-Dateien ohne `+x` Permission installieren.
Der `plugin-health-check.sh` Hook repariert das automatisch bei jedem Start.

Wenn ein neues Plugin installiert wird und "Permission denied" Fehler auftreten:
1. `find ~/.claude/plugins -name "*.sh" ! -perm -u+x` — fehlende Permissions finden
2. `chmod +x` auf alle betroffenen Dateien setzen
3. Der naechste Start repariert es automatisch (Self-Healing)

Auf Windows: `.ps1`-Dateien brauchen keine Execute-Permission. Bei Problemen mit Execution-Policy:
`pwsh -ExecutionPolicy Bypass -File "path-to-script.ps1"`

## Regel 3: Debug-Methode bei Hook-Fehlern

Bei "hook error" in der Startup-Ausgabe SOFORT:

**macOS/Linux:**
```bash
echo "test" | claude -p --debug-file /tmp/hook-debug.txt --system-prompt "Reply with just OK"
grep "Hook.*error" /tmp/hook-debug.txt
```

**Windows:**
```powershell
echo "test" | claude -p --debug-file $env:TEMP\hook-debug.txt --system-prompt "Reply with just OK"
Select-String -Pattern "Hook.*error|hook.*fail|Failed to run" -Path $env:TEMP\hook-debug.txt
```

NIEMALS Hooks einzeln testen — die echte Startup-Umgebung ist anders.
Der `--debug-file` Trick loest Hook-Fehler in 30 Sekunden statt 20 Minuten.
