# PowerShell Hook Template

Dieses Template ist die Pflicht-Grundlage fuer JEDEN neuen PowerShell-Hook.
Kopiere es, passe die markierten Stellen an, und entferne die Kommentar-Markierungen.

```powershell
# [HOOK-NAME]: [Kurzbeschreibung was der Hook tut]
# Runs as [EVENT] hook (z.B. SessionStart, PostToolUse, Stop)
# stdout -> AI context (system-reminder), stderr -> user terminal
# Platform: Windows (PowerShell 7+)

# --- Standard-Imports (PFLICHT) ---
. "$PSScriptRoot/hook-log.ps1"
# Falls Whiteboard-Zugriff noetig:
# . "$PSScriptRoot/whiteboard-insert.ps1"

# --- Error Handling Setup (PFLICHT) ---
$ErrorActionPreference = "Stop"

try {

    # ============================================================
    # HOOK-LOGIK HIER EINFUEGEN
    # ============================================================

    # Beispiel: Status an AI-Kontext und User-Terminal schreiben
    # Write-Output "[HookName]: Alles OK."
    # [Console]::Error.WriteLine("[HookName]: Alles OK.")

    # Beispiel: Fehler ins Log schreiben
    # Hook-Log "[HookName]: Operation erfolgreich"

    # ============================================================

} catch {
    # Fehler loggen aber NIEMALS propagieren
    Hook-LogError "[HOOK-NAME] failed: $_"

    # Bei schweren Fehlern: Whiteboard-Eintrag (optional)
    # $entry = "### $(Get-Date -Format 'yyyy-MM-dd HH:mm') -- Hook: [HOOK-NAME] -- $_ -- Status: OFFEN"
    # Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
}

# PFLICHT: Jeder Hook MUSS mit exit 0 enden — ausnahmslos.
# PowerShell gibt sonst den Exit-Code des letzten Befehls zurueck,
# was bei internen Fehlern zu sichtbaren Hook-Errors fuehrt.
exit 0
```

## Varianten

### Minimal (fuer einfache Hooks ohne Whiteboard)

```powershell
. "$PSScriptRoot/hook-log.ps1"
$ErrorActionPreference = "Stop"

try {
    # Logik hier
} catch {
    Hook-LogWarn "[HOOK-NAME]: $_"
}

exit 0
```

### Mit stdin-Parsing (fuer PreToolUse/PostToolUse)

```powershell
. "$PSScriptRoot/hook-log.ps1"
$ErrorActionPreference = "Stop"

try {
    $input_data = $input | Out-String | ConvertFrom-Json
    $toolName = $input_data.tool_name
    $toolInput = $input_data.tool_input

    # Logik basierend auf $toolName und $toolInput
} catch {
    Hook-LogWarn "[HOOK-NAME]: $_"
}

exit 0
```

### Async (fuer lang-laufende Operationen)

```powershell
. "$PSScriptRoot/hook-log.ps1"

try {
    $scriptPath = Join-Path $PSScriptRoot "[WORKER-SCRIPT].ps1"
    Start-Process pwsh -ArgumentList "-NoProfile", "-File", $scriptPath -WindowStyle Hidden
    Hook-Log "[HOOK-NAME]: Worker gestartet (async)"
} catch {
    Hook-LogWarn "[HOOK-NAME]: Worker-Start fehlgeschlagen: $_"
}

exit 0
```

## Wichtige Regeln

1. **IMMER `exit 0`** — am Ende des Skripts UND im catch-Block (implizit durch Durchlauf)
2. **NIEMALS `exit 1`** — Fehler werden geloggt, nicht propagiert
3. **IMMER `hook-log.ps1` importieren** — zentrale Protokollierung
4. **IMMER `$ErrorActionPreference = "Stop"`** — damit try/catch greift
5. **NIEMALS interaktive Befehle** — kein Read-Host, kein Warten auf User-Input
6. **Pfade mit `$PSScriptRoot` oder `$env:USERPROFILE`** — nie hardcoded
