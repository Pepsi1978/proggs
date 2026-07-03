# Codex Launcher with random Windows Terminal tab color.
# Mirrors start-claude-wt.ps1 but invokes start-codex-full-access.ps1 instead.
#
# Codex has no inner /color slash command, so we only set the WT tab color and
# title - the inner session tint is unchanged (this is intentional and gives
# the desired effect: a coloured tab in the WT tab strip).
#
# Logging: every invocation appends to start-codex-wt.log for diagnosis.
#
# Robuster Start (2026-07-03): Die komplette Start-/Verifikations-/Retry-Logik liegt
# zentral in start-wt-common.ps1 (Start-WtCliRobust). Hintergrund: Das Kaltstart-Race
# (Fenster blitzt auf und schließt sofort, 5-6 Klicks nötig) ließ sich NICHT durch
# Vorab-Heuristiken lösen — Beweis und Details siehe Kommentarkopf von start-wt-common.ps1.

$ErrorActionPreference = 'Continue'
$logFile = 'C:\Users\barwa\start-codex-wt.log'

function Write-Log($msg) {
    $stamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    Add-Content -Path $logFile -Value "[$stamp] $msg" -ErrorAction SilentlyContinue
}

Write-Log "=== codex launcher started, PID $PID ==="

# Same palette as start-claude-wt.ps1 so the tab strip stays visually consistent
# across Claude and Codex tabs.
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
    '--tabColor';        $pick.Hex
    '--title';           "Codex-$($pick.Name)"
    '--startingDirectory'; 'C:\Users\barwa'
    'pwsh.exe'
    '-NoExit'
    '-ExecutionPolicy';  'Bypass'
    '-File';             'C:\Users\barwa\start-codex-full-access.ps1'
)
Write-Log "tab args: $($tabArgs -join ' | ')"

# Fallback-Argumente: dasselbe innere Skript direkt in pwsh (klassisches Konsolenfenster)
$fallbackArgs = @(
    '-NoExit'
    '-ExecutionPolicy'; 'Bypass'
    '-File';            'C:\Users\barwa\start-codex-full-access.ps1'
)

$ok = Start-WtCliRobust -LogFile $logFile -WtPath $wtPath -TabArgs $tabArgs `
    -InnerMatch 'start-codex-full-access\.ps1' -FallbackPwshArgs $fallbackArgs `
    -FallbackWorkDir 'C:\Users\barwa'

if (-not $ok) {
    Write-Log "ERROR: Start endgueltig fehlgeschlagen (alle Versuche + Fallback)"
    exit 2
}

Write-Log "codex launcher exiting normally"
