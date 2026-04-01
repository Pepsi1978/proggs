# invariant-check.ps1 — SessionStart Hook
# Proaktive Pruefung von System-Invarianten bei jedem Start
# Inspiriert von Cursor Invariant Sentinel Pattern (R8 Finding 3, 2026-03-31)
# Meldet Verletzungen direkt sichtbar, damit sie nicht tagelang unbemerkt bleiben.

$ErrorActionPreference = "SilentlyContinue"
$violations = @()

# --- Invariant 1: Stale OFFEN-Eintraege (>7 Tage) ---
$whiteboardPath = Join-Path $env:USERPROFILE "proggs\.claude\agent-memory\shared\MEMORY.md"
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
$settingsLocal = Join-Path $env:USERPROFILE ".claude\settings.local.json"
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
$hooksDir = Join-Path $env:USERPROFILE ".claude\hooks"
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

# --- Invariant 5: CLAUDE.md Sync ---
$claudeRepo = Join-Path $env:USERPROFILE "proggs\CLAUDE.md"
$claudeHome = Join-Path $env:USERPROFILE "CLAUDE.md"
if ((Test-Path $claudeRepo) -and (Test-Path $claudeHome)) {
    $hashRepo = (Get-FileHash $claudeRepo -Algorithm SHA256).Hash
    $hashHome = (Get-FileHash $claudeHome -Algorithm SHA256).Hash
    if ($hashRepo -ne $hashHome) {
        $violations += "CLAUDE.MD: Repo-Version und Home-Version sind NICHT synchron!"
    }
}

# --- Invariant 6: Heartbeat-Status ---
$heartbeatStatus = Join-Path $env:USERPROFILE ".claude\heartbeat-status.json"
if (Test-Path $heartbeatStatus) {
    try {
        $hb = Get-Content $heartbeatStatus -Raw | ConvertFrom-Json
        if ($hb.status -eq "CRITICAL") {
            $violations += "HEARTBEAT: KRITISCHE Probleme zwischen Sessions erkannt!"
        }
    } catch {}
}

# --- Invariant 7: Hook type/field consistency (Self-Healing) ---
$settingsMain = Join-Path $env:USERPROFILE ".claude\settings.json"
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
