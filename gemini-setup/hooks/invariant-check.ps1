# invariant-check.ps1 — SessionStart Hook
# Proaktive Pruefung von System-Invarianten bei jedem Start
# Inspiriert von Cursor Invariant Sentinel Pattern (R8 Finding 3, 2026-03-31)
# Meldet Verletzungen direkt sichtbar, damit sie nicht tagelang unbemerkt bleiben.

$ErrorActionPreference = "SilentlyContinue"
$violations = @()

# --- Invariant 1: Stale OFFEN-Eintraege (>7 Tage) ---
$whiteboardPath = Join-Path $env:USERPROFILE "proggs\.Gemini\agent-memory\shared\MEMORY.md"
$content = $null
if (Test-Path $whiteboardPath) {
    $content = Get-Content $whiteboardPath -Raw -Encoding UTF8
    # BUG FIX 2026-03-31: Datum und Status stehen auf VERSCHIEDENEN Zeilen.
    # Alte Regex konnte nie matchen (. matched keine Zeilenumbrueche).
    # Neuer Ansatz: Splitte nach ### Eintraegen, pruefe jeden Block einzeln.
    $today = Get-Date
    $staleCount = 0
    # (?m) = multiline: ^ matches line starts, not just string start
    $blocks = $content -split '(?m)(?=^### \d{4}-\d{2}-\d{2})' | Where-Object { $_ -match '### \d{4}-\d{2}-\d{2}' }
    foreach ($block in $blocks) {
        # Pattern must match Markdown format: **Status:** OFFEN (but NOT DESIGN-OFFEN)
        if ($block -match 'Status:\*\*\s+OFFEN\b') {
            if ($block -match '^### (\d{4}-\d{2}-\d{2})') {
                try {
                    $entryDate = [DateTime]::ParseExact($Matches[1], "yyyy-MM-dd", $null)
                    $age = ($today - $entryDate).Days
                    if ($age -gt 7) { $staleCount++ }
                } catch {}
            }
        }
    }
    if ($staleCount -gt 0) {
        $violations += "WHITEBOARD: $staleCount OFFEN-Eintraege aelter als 7 Tage — /self-improve starten!"
    }
}

# --- Invariant 2: bypassPermissions aktiv ---
$settingsLocal = Join-Path $env:USERPROFILE ".Gemini\settings.local.json"
if (Test-Path $settingsLocal) {
    try {
        $sl = Get-Content $settingsLocal -Raw | ConvertFrom-Json
        if ($sl.permissions.defaultMode -ne "bypassPermissions") {
            $violations += "PERMISSIONS: bypassPermissions NICHT aktiv in settings.local.json!"
        }
    } catch {
        $violations += "PERMISSIONS: settings.local.json nicht lesbar!"
    }
}

# --- Invariant 3: Hook-Paare (.ps1 ohne .sh oder umgekehrt) ---
$hooksDir = Join-Path $env:USERPROFILE ".Gemini\hooks"
if (Test-Path $hooksDir) {
    $ps1Files = Get-ChildItem "$hooksDir\*.ps1" -ErrorAction SilentlyContinue | ForEach-Object { $_.BaseName }
    $shFiles = Get-ChildItem "$hooksDir\*.sh" -ErrorAction SilentlyContinue | ForEach-Object { $_.BaseName }
    # Exclude known platform-only hooks
    $platformOnly = @('notify', 'mcp-auth-check', 'plugin-health-check', 'subagent-context')
    $missingShCount = 0
    foreach ($ps1 in $ps1Files) {
        if ($ps1 -notin $platformOnly -and $ps1 -notin $shFiles) {
            $missingShCount++
        }
    }
    if ($missingShCount -gt 0) {
        $violations += "HOOKS: $missingShCount .ps1-Hooks ohne .sh-Gegenstueck"
    }
}

# --- Invariant 4: Systemzustand-Alter ---
if ($content -match 'Stand:\s*(\d{4}-\d{2}-\d{2})') {
    try {
        $stateDate = [DateTime]::ParseExact($Matches[1], "yyyy-MM-dd", $null)
        $stateAge = ((Get-Date) - $stateDate).Days
        if ($stateAge -gt 14) {
            $violations += "SYSTEMZUSTAND: Letzte Aktualisierung vor $stateAge Tagen — veraltet!"
        }
    } catch {}
}

# --- Invariant 5: Gemini.md im Home darf NICHT existieren (Geloescht 2026-04-04) ---
# Frueher wurde Sync zwischen ~/proggs/Gemini.md und ~/Gemini.md geprueft.
# Seit 2026-04-04 gibt es keine ~/Gemini.md mehr (Duplikat entfernt fuer Token-Ersparnis).
# Wenn ~/Gemini.md wieder auftaucht: Warnung — wahrscheinlich versehentlich erstellt.
$GeminiHome = Join-Path $env:USERPROFILE "Gemini.md"
if (Test-Path $GeminiHome) {
    $violations += "Gemini.MD: ~/Gemini.md existiert wieder — sollte nicht da sein (geloescht 2026-04-04). Bitte loeschen."
}

# --- Invariant 6: Heartbeat-Status ---
$heartbeatStatus = Join-Path $env:USERPROFILE ".Gemini\heartbeat-status.json"
if (Test-Path $heartbeatStatus) {
    try {
        $hb = Get-Content $heartbeatStatus -Raw | ConvertFrom-Json
        if ($hb.status -eq "CRITICAL") {
            $violations += "HEARTBEAT: KRITISCHE Probleme zwischen Sessions erkannt!"
        }
    } catch {}
}

# --- Invariant 7: Hook type/field consistency (Self-Healing) ---
$settingsMain = Join-Path $env:USERPROFILE ".Gemini\settings.json"
if (Test-Path $settingsMain) {
    try {
        $settingsRaw = Get-Content $settingsMain -Raw -Encoding UTF8
        $settings = $settingsRaw | ConvertFrom-Json
        $fixCount = 0
        foreach ($event in $settings.hooks.PSObject.Properties) {
            foreach ($entry in $event.Value) {
                foreach ($hook in $entry.hooks) {
                    if ($hook.type -eq "prompt" -and $hook.PSObject.Properties["command"] -and -not $hook.PSObject.Properties["prompt"]) {
                        $hook.type = "command"
                        $fixCount++
                    }
                }
            }
        }
        if ($fixCount -gt 0) {
            $settings | ConvertTo-Json -Depth 10 | Set-Content $settingsMain -Encoding UTF8
            $violations += "HOOKS-SCHEMA: $fixCount Hook(s) mit type:prompt+command statt type:command gefunden und AUTO-REPARIERT!"
        }
    } catch {}
}

# --- Invariant 8: Whiteboard-Versions-Drift (P2, 2026-05-10) ---
# Prueft ob "Stand: YYYY-MM-DD" in MEMORY.md aelter als 7 Tage UND ob die dort vermerkte
# gemini-setup-Version mit der lokalen Version uebereinstimmt. Verhindert dass Whiteboard
# 24 Versionen drifftet (wie heute entdeckt: v2.1.114 im Whiteboard, v2.1.138 lokal).
if ($content) {
    # Suche nach Versions-Angabe im Systemzustand-Block
    if ($content -match 'Gemini CLI\s*\*?\*?\s*v(\d+\.\d+\.\d+)') {
        $whiteboardVersion = $Matches[1]
        try {
            $localVersionRaw = (Gemini --version 2>$null) -join " "
            if ($localVersionRaw -match '(\d+\.\d+\.\d+)') {
                $localVersion = $Matches[1]
                if ($whiteboardVersion -ne $localVersion) {
                    $violations += "WHITEBOARD-VERSIONS-DRIFT: Stand sagt v$whiteboardVersion, lokal lauft v$localVersion — Systemzustand aktualisieren!"
                }
            }
        } catch {
            # Gemini --version nicht verfuegbar — silent skip
        }
    }
}

# --- Invariant 9: Merge-Konflikt-Marker in MEMORY.md (P1, 2026-05-10) ---
# Heute entdeckt: Merge-Konflikt-Marker ("<<<<<<< Updated upstream") koennen 17+ Tage
# ungeloest in MEMORY.md liegen und das ganze System verfaelschen. Beim SessionStart
# sofort lautstark warnen — nicht nur per auto-sync.ps1 silent rebase-Versuch.
if ($content) {
    $conflictPatterns = @('<<<<<<< Updated upstream', '<<<<<<< HEAD', '>>>>>>> Stashed changes', '\|\|\|\|\|\|\| Stash base')
    foreach ($pattern in $conflictPatterns) {
        if ($content -match [regex]::Escape($pattern)) {
            $violations += "WHITEBOARD-KONFLIKT: MEMORY.md enthaelt Merge-Konflikt-Marker '$pattern' — SOFORT manuell aufloesen!"
            break  # Eine Meldung reicht — Marker kommen meist im Buendel
        }
    }
}

# --- Output ---
if ($violations.Count -gt 0) {
    Write-Host ""
    Write-Host "Invariant-Check: $($violations.Count) Verletzung(en):"
    foreach ($v in $violations) {
        Write-Host "  - $v"
    }
} else {
    Write-Host "Invariant-Check: Alle Pruefungen bestanden."
}

exit 0

