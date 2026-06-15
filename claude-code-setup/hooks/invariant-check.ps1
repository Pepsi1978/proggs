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

# --- Invariant 5: CLAUDE.md im Home darf NICHT existieren (Geloescht 2026-04-04) ---
# Frueher wurde Sync zwischen ~/proggs/CLAUDE.md und ~/CLAUDE.md geprueft.
# Seit 2026-04-04 gibt es keine ~/CLAUDE.md mehr (Duplikat entfernt fuer Token-Ersparnis).
# Poka-Yoke Stufe 3 (2026-05-30): identisches Duplikat wird AUTOMATISCH geheilt
# (geloescht) statt nur gemeldet. Abweichende Datei wird weiterhin nur gemeldet,
# damit eigener Inhalt nicht verloren geht.
$claudeHome = Join-Path $env:USERPROFILE "CLAUDE.md"
$claudeRepo = Join-Path $env:USERPROFILE "proggs\CLAUDE.md"
if (Test-Path $claudeHome) {
    $isDuplicate = $false
    if (Test-Path $claudeRepo) {
        try {
            $hashHome = (Get-FileHash -Path $claudeHome -Algorithm MD5 -ErrorAction Stop).Hash
            $hashRepo = (Get-FileHash -Path $claudeRepo -Algorithm MD5 -ErrorAction Stop).Hash
            $isDuplicate = ($hashHome -eq $hashRepo)
        } catch {}
    }
    if ($isDuplicate) {
        try {
            Remove-Item -Path $claudeHome -Force -ErrorAction Stop
            $violations += "CLAUDE.MD: ~/CLAUDE.md (identisches Duplikat) automatisch entfernt — Token-Ersparnis wiederhergestellt."
        } catch {
            $violations += "CLAUDE.MD: ~/CLAUDE.md existiert, Auto-Loeschung fehlgeschlagen. Bitte manuell loeschen."
        }
    } else {
        $violations += "CLAUDE.MD: ~/CLAUDE.md existiert UND weicht von der Repo-Version ab — NICHT automatisch geloescht. Bitte pruefen."
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

# --- Invariant 8: Whiteboard-Versions-Drift (P2, 2026-05-10) ---
# Prueft ob "Stand: YYYY-MM-DD" in MEMORY.md aelter als 7 Tage UND ob die dort vermerkte
# Claude-Code-Version mit der lokalen Version uebereinstimmt. Verhindert dass Whiteboard
# 24 Versionen drifftet (wie heute entdeckt: v2.1.114 im Whiteboard, v2.1.138 lokal).
if ($content) {
    # Suche nach Versions-Angabe im Systemzustand-Block
    if ($content -match 'Claude Code\s*\*?\*?\s*v(\d+\.\d+\.\d+)') {
        $whiteboardVersion = $Matches[1]
        try {
            $localVersionRaw = (claude --version 2>$null) -join " "
            if ($localVersionRaw -match '(\d+\.\d+\.\d+)') {
                $localVersion = $Matches[1]
                if ($whiteboardVersion -ne $localVersion) {
                    $violations += "WHITEBOARD-VERSIONS-DRIFT: Stand sagt v$whiteboardVersion, lokal lauft v$localVersion — Systemzustand aktualisieren!"
                }
            }
        } catch {
            # claude --version nicht verfuegbar — silent skip
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

# --- Invariant 10: Hook-Drift aktiv<->repo (inhaltlich, EOL/BOM-normalisiert) (2026-06-15) ---
# Faengt den Fall, dass ein aktiver Hook (~/.claude/hooks/) inhaltlich von der Repo-Spiegelung
# (claude-code-setup/hooks/) abweicht (real 2026-06-15: subagent-context war 1 Monat alt + nutzte
# das flache Schema -> Subagenten erbten das System nicht, monatelang unbemerkt). INHALTS-Vergleich
# (BOM via utf-8-sig + CRLF->LF + trailing-newline normalisiert), damit reiner EOL-Drift KEINEN
# Fehlalarm ausloest — sonst piept der Waechter dauernd und wird ignoriert (agent-knowledge-system.md S4).
$repoHooks = Join-Path $env:USERPROFILE "proggs\claude-code-setup\hooks"
if ((Test-Path $hooksDir) -and (Test-Path $repoHooks)) {
    function Get-NormHook($p) {
        try {
            $t = [System.IO.File]::ReadAllText($p)        # erkennt+entfernt BOM automatisch
            $t = $t.TrimStart([char]0xFEFF)               # Sicherheitsnetz fuer BOM
            $t = $t -replace "`r`n", "`n"                 # CRLF -> LF
            return $t.TrimEnd("`n")                       # trailing newlines egal
        } catch { return $null }
    }
    $driftHooks = @()
    foreach ($rf in (Get-ChildItem "$repoHooks\*.ps1", "$repoHooks\*.sh" -ErrorAction SilentlyContinue)) {
        $active = Join-Path $hooksDir $rf.Name
        if (Test-Path $active) {
            $na = Get-NormHook $active
            $nr = Get-NormHook $rf.FullName
            if ($null -ne $na -and $null -ne $nr -and $na -ne $nr) { $driftHooks += $rf.Name }
        }
    }
    if ($driftHooks.Count -gt 0) {
        $violations += "HOOK-DRIFT: $($driftHooks.Count) Hook(s) weichen aktiv<->repo ab (Inhalt): $($driftHooks -join ', ') — Repo<->aktiv spiegeln!"
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

# Almanach-/Harness-Self-Tests buendeln (bugs/health.py) — nur Hinweise, keine harte Verletzung
try {
    $healthScript = Join-Path $env:USERPROFILE "proggs\bugs\health.py"
    $py = Get-Command python -ErrorAction SilentlyContinue
    if ((Test-Path $healthScript) -and $py) {
        $healthOut = (& $py.Source $healthScript --quiet 2>&1 | Out-String).Trim()
        if ($healthOut) {
            Write-Host ""
            Write-Host "Almanach-Self-Test (bugs/health.py):"
            foreach ($l in ($healthOut -split "`n")) { Write-Host "  $l" }
        }
    }
} catch { }

exit 0
