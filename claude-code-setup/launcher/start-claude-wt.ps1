# Claude Code Launcher with random Windows Terminal tab color (Version A: matched)
# Picks one random color and uses it both for the WT tab AND the inner /color session.
#
# Logging: every invocation appends to start-claude-wt.log for diagnosis.
#
# Robuster Start (2026-07-03): Die komplette Start-/Verifikations-/Retry-Logik liegt
# zentral in start-wt-common.ps1 (Start-WtCliRobust). Hintergrund: Das Kaltstart-Race
# (Fenster blitzt auf und schließt sofort, 5-6 Klicks nötig) ließ sich NICHT durch
# Vorab-Heuristiken lösen — Beweis und Details siehe Kommentarkopf von start-wt-common.ps1.

$ErrorActionPreference = 'Continue'
$logFile = 'C:\Users\barwa\start-claude-wt.log'

function Write-Log($msg) {
    $stamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    Add-Content -Path $logFile -Value "[$stamp] $msg" -ErrorAction SilentlyContinue
}

Write-Log "=== launcher started, PID $PID ==="

# Color palette - name MUST match a value Claude Code's /color slash command accepts.
# Valid set as of 2026-05-15: red, blue, green, yellow, purple, orange, pink, cyan, default.
# We use eight (skip "default" since that defeats the purpose of distinguishing tabs).
# Hex values are vibrant tones that read well on Windows Terminal tab strips.
$colors = @(
    [PSCustomObject]@{ Name = 'red';    Hex = '#FF3B3B' }
    [PSCustomObject]@{ Name = 'orange'; Hex = '#FF8C42' }
    [PSCustomObject]@{ Name = 'yellow'; Hex = '#FFD93D' }
    [PSCustomObject]@{ Name = 'green';  Hex = '#4CAF50' }
    [PSCustomObject]@{ Name = 'cyan';   Hex = '#00BCD4' }
    [PSCustomObject]@{ Name = 'blue';   Hex = '#2196F3' }
    [PSCustomObject]@{ Name = 'purple'; Hex = '#9C27B0' }
    [PSCustomObject]@{ Name = 'pink';   Hex = '#FF1493' }
)

$pick = Get-Random -InputObject $colors
Write-Log "picked color: $($pick.Name) ($($pick.Hex))"

# Resolve wt.exe (Microsoft Store execution alias). Fall back to known paths.
$wtPath = $null
$wtCmd = Get-Command wt.exe -ErrorAction SilentlyContinue
if ($wtCmd) { $wtPath = $wtCmd.Source }
if (-not $wtPath -or -not (Test-Path $wtPath)) {
    foreach ($p in @(
        "$env:LOCALAPPDATA\Microsoft\WindowsApps\wt.exe"
    )) {
        if (Test-Path $p) { $wtPath = $p; break }
    }
}
if (-not $wtPath) {
    Write-Log "ERROR: wt.exe not found"
    exit 1
}
Write-Log "wt.exe: $wtPath"

# Gemeinsame Robust-Start-Funktion laden (Zombie-Cleanup + Verify + Auto-Retry + Fallback)
. 'C:\Users\barwa\start-wt-common.ps1'

# Tab-Argumente (ohne -w — das Fenster-Ziel bestimmt Start-WtCliRobust selbst)
$tabArgs = @(
    'new-tab'
    '--tabColor';  $pick.Hex
    '--title';     "Claude-$($pick.Name)"
    'pwsh.exe'
    '-NoExit'
    '-ExecutionPolicy'; 'Bypass'
    '-File';       'C:\Users\barwa\start-claude.ps1'
    '-ColorName';  $pick.Name
)
Write-Log "tab args: $($tabArgs -join ' | ')"

# Fallback-Argumente: dasselbe innere Skript direkt in pwsh (klassisches Konsolenfenster)
$fallbackArgs = @(
    '-NoExit'
    '-ExecutionPolicy'; 'Bypass'
    '-File';       'C:\Users\barwa\start-claude.ps1'
    '-ColorName';  $pick.Name
)

$ok = Start-WtCliRobust -LogFile $logFile -WtPath $wtPath -TabArgs $tabArgs `
    -InnerMatch 'start-claude\.ps1' -FallbackPwshArgs $fallbackArgs `
    -FallbackWorkDir 'C:\Users\barwa\proggs'

if (-not $ok) {
    Write-Log "ERROR: Start endgueltig fehlgeschlagen (alle Versuche + Fallback)"
    exit 2
}

Write-Log "launcher exiting normally"
