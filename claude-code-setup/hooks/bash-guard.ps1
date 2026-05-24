# bash-guard.ps1 — Consolidated PreToolUse:Bash guard
# Event: PreToolUse
# Matcher: Bash
# Type: command
# Platform: Windows (PowerShell 7+)
#
# CONSOLIDATION (2026-04-12): Vereint safety-gate.ps1 + silent-corrector.ps1
# Alle Bash-Kommando-Pruefungen in einer Datei:
#   Part 1 (ex safety-gate): Blockiert gefaehrliche Befehle (rm -rf, force push, DROP TABLE)
#   Part 2 (ex silent-corrector): Blockiert Codex-Verzeichnis und sed-auf-JSON
#   Part 3 (ex safety-gate): Shell-Update-Warnung
#   Part 4 (ex safety-gate): settings.json-via-Bash-Warnung
#
# Hook-Protokoll: exit 0 = erlauben, exit 2 + JSON = blockieren
# ROBUSTNESS: Fail-open — bei jedem Fehler exit 0 (nie blockieren bei Hook-Bug)

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }
try { . "$PSScriptRoot/whiteboard-insert.ps1" } catch { }

try {
    $hookInput = [Console]::In.ReadToEnd()
    if (-not $hookInput -or $hookInput.Length -lt 5) { exit 0 }
    $json = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch { exit 0 }

$toolName = $json.tool_name
if ($toolName -ne 'Bash') { exit 0 }

$cmd = $json.tool_input.command
if (-not $cmd) { exit 0 }

# ============================================================
# PART 1: Gefaehrliche Befehle blockieren (ex safety-gate)
# ============================================================

$dangerous = @(
    'rm\s+-rf\s+[/~]',
    'git\s+push\s+--force\s+.*main',
    'git\s+reset\s+--hard',
    'git\s+restore\s+\.',
    'git\s+branch\s+-D',
    '(?i)DROP\s+TABLE',
    '(?i)DROP\s+DATABASE',
    '(?i)TRUNCATE\s+TABLE',
    'format\s+[A-Z]:',
    'del\s+/[sS]\s+/[qQ]\s+C:',
    'Remove-Item\s+-Recurse.*C:\\',
    'git\s+init',
    'gh\s+repo\s+create',
    'git\s+remote\s+add'
)

foreach ($pattern in $dangerous) {
    if ($cmd -match $pattern) {
        Hook-LogError "BLOCKED dangerous: $pattern"
        try {
            $entry = "### $(Get-Date -Format 'yyyy-MM-dd HH:mm') — Hook: bash-guard.ps1 — Befehl blockiert: $pattern"
            Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
        } catch { }
        $msg = "bash-guard: BLOCKIERT — gefaehrlicher Befehl erkannt (Pattern: $pattern). Bitte sichereren Befehl verwenden."
        [Console]::Error.WriteLine($msg)
        @{ error = "BLOCKED: Dangerous command — $pattern" } | ConvertTo-Json -Compress | Write-Output
        exit 2
    }
}

# ------------------------------------------------------------
# PART 1b: 'git add -A' / 'git add .' blockieren (parallele Sessions)
# Wuerde fremde unfertige Dateien anderer Sessions mitcommitten.
# Praezise: blockiert -A/--all/././ auch nach Flags, aber NICHT
# spezifische Pfade ('git add .claude/x', 'git add foo.txt') und
# nicht ueber && hinweg ('... && grep -A 3'). -cmatch = case-sensitiv,
# damit '-A' nicht auch '-a' matcht.
# ------------------------------------------------------------
if ($cmd -cmatch 'git\s+add\s+([^&|;<>]*\s+)?(-A|--all|\.|\./)(\s|$)') {
    try { Hook-LogError "BLOCKED 'git add -A/.': $($cmd.Substring(0, [Math]::Min(100, $cmd.Length)))" } catch { }
    try {
        $entry = "### $(Get-Date -Format 'yyyy-MM-dd HH:mm') — Hook: bash-guard.ps1 — 'git add -A/.' blockiert (parallele Sessions)"
        Insert-WhiteboardEntry -Section "Offene Fehler & Probleme" -Entry $entry
    } catch { }
    [Console]::Error.WriteLine("bash-guard: BLOCKIERT — 'git add -A' / 'git add .' ist verboten. Parallele Sessions wuerden fremde unfertige Dateien mitcommitten. Stage deine eigenen Dateien namentlich: git add pfad/datei1 pfad/datei2")
    @{ error = "BLOCKED: git add -A/. verboten (parallele Sessions). Stage eigene Dateien namentlich: git add pfad/datei" } | ConvertTo-Json -Compress | Write-Output
    exit 2
}

# ============================================================
# PART 2: Codex-Verzeichnis + sed-auf-JSON blockieren (ex silent-corrector)
# ============================================================

$parts = $cmd -split '\s*(?:&&|;|\|\|)\s*'
foreach ($part in $parts) {
    $stripped = $part.Trim()
    if (-not $stripped) { continue }

    # Codex-Verzeichnis blockieren
    if ($stripped -match '^cd\s+.*[~/\\]Codex[/\\]?' -or
        ($stripped -match '^(ls|cat|rm|mv|cp|touch|mkdir|chmod|head|tail|wc|stat|file)\s+' -and $stripped -match '[~/\\]Codex[/\\]')) {
        Write-Output '{"reason":"BLOCKIERT: ~/Codex/ ist gesperrt. Verwende ~/proggs/ statt ~/Codex/."}'
        exit 2
    }

    # sed auf JSON blockieren
    if ($stripped -match '^sed\s+' -and $stripped -match '\.json\b') {
        Write-Output '{"reason":"BLOCKIERT: sed auf JSON verboten. Benutze Edit-Tool oder python3 json.load/dump."}'
        exit 2
    }
}

# ============================================================
# PART 3: Shell-Update-Warnung (ex safety-gate)
# ============================================================

$shellUpdates = @(
    'npm\s+install\s+-g\s+@anthropic',
    'winget\s+upgrade\s+Git\.Git',
    'rustup\s+update'
)
foreach ($pattern in $shellUpdates) {
    if ($cmd -match $pattern) {
        Write-Output "WARNING: Shell-Update erkannt. Laut Regeln muessen Shell-Updates NACH allen Aufgaben erfolgen."
        exit 0
    }
}

# ============================================================
# PART 4: settings.json-via-Bash-Warnung (ex safety-gate)
# ============================================================

if ($cmd -match '>\s*.*settings\.json' -or $cmd -match 'echo.*>.*settings\.json') {
    Write-Output "WARNING: Schreibzugriff auf settings.json per Bash erkannt. config-guard prueft danach."
}

# ============================================================
# PART 5: Hook-Exit0-Guard (ex hook-exit0-guard.ps1, konsolidiert 2026-05-10)
# Loop-3 ESK-4: vermeidet 2. PowerShell-Spawn bei jedem Bash-Call.
# Triggert nur auf 'git commit', prueft staged Hook-Dateien auf exit 0.
# ============================================================

if ($cmd -match 'git commit') {
    try {
        $staged = git -C "$env:USERPROFILE\proggs" diff --cached --name-only 2>$null
        if ($staged) {
            $hookFiles = $staged | Where-Object { $_ -match '\.(ps1|sh)$' -and $_ -match 'hook' }
            if ($hookFiles) {
                $problems = @()
                foreach ($file in $hookFiles) {
                    $fullPath = Join-Path "$env:USERPROFILE\proggs" $file
                    if (-not (Test-Path $fullPath)) { continue }
                    $content = Get-Content $fullPath -Raw -ErrorAction SilentlyContinue
                    if (-not $content) { continue }
                    if ($content -notmatch 'exit\s+0\s*$') { $problems += $file }
                    if ($file -match 'session|auto-sync|invariant') {
                        if ($content -match 'exit\s+1') {
                            $problems += "$file (contains exit 1 — forbidden in SessionStart hooks!)"
                        }
                    }
                }
                if ($problems.Count -gt 0) {
                    $list = $problems -join "`n  - "
                    $msg = "Hook-Exit0-Guard: WARNUNG — folgende Hook-Dateien haben kein 'exit 0' am Ende:`n  - $list`nBitte 'exit 0' am Ende jeder Hook-Datei hinzufuegen!"
                    Write-Output $msg
                    Write-Host $msg
                    try { Hook-LogWarn "Hook files missing exit 0: $($problems -join ', ')" } catch { }
                }
            }
        }
    } catch {
        try { Hook-LogWarn "bash-guard part5 (exit0-check): $_" } catch { }
    }
}

exit 0
