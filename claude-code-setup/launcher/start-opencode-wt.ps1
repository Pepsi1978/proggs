# OpenCode Launcher mit zufälliger Windows-Terminal-Tabfarbe.
# Spiegel von start-claude-wt.ps1 / start-codex-wt.ps1 / start-gemini-wt.ps1, startet opencode.
#
# Logging: jeder Aufruf schreibt in start-opencode-wt.log zur Diagnose.
#
# Robuster Start (2026-07-03): Die komplette Start-/Verifikations-/Retry-Logik liegt
# zentral in start-wt-common.ps1 (Start-WtCliRobust). Hintergrund: Das Kaltstart-Race
# (Fenster blitzt auf und schließt sofort, 5-6 Klicks nötig) ließ sich NICHT durch
# Vorab-Heuristiken lösen — Beweis und Details siehe Kommentarkopf von start-wt-common.ps1.

$ErrorActionPreference = 'Continue'
$logFile = 'C:\Users\barwa\start-opencode-wt.log'

function Write-Log($msg) {
    $stamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    Add-Content -Path $logFile -Value "[$stamp] $msg" -ErrorAction SilentlyContinue
}

Write-Log "=== opencode launcher started, PID $PID ==="

# Gleiche Palette wie die anderen Launcher für konsistente Tab-Leiste
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

# wt.exe finden (Microsoft Store Execution Alias)
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
    '--title';           "OpenCode-$($pick.Name)"
    '--startingDirectory'; 'C:\Users\barwa\proggs'
    'pwsh.exe'
    '-NoExit'
    '-ExecutionPolicy';  'Bypass'
    '-Command';          'opencode'
)
Write-Log "tab args: $($tabArgs -join ' | ')"

# Fallback-Argumente: opencode direkt in pwsh (klassisches Konsolenfenster)
$fallbackArgs = @(
    '-NoExit'
    '-ExecutionPolicy'; 'Bypass'
    '-Command';         'opencode'
)

$ok = Start-WtCliRobust -LogFile $logFile -WtPath $wtPath -TabArgs $tabArgs `
    -InnerMatch '-Command\s+"?opencode' -FallbackPwshArgs $fallbackArgs `
    -FallbackWorkDir 'C:\Users\barwa\proggs'

if (-not $ok) {
    Write-Log "ERROR: Start endgueltig fehlgeschlagen (alle Versuche + Fallback)"
    exit 2
}

Write-Log "opencode launcher exiting normally"
