#Requires -Version 7
<#
.SYNOPSIS
    Baut ein Voice-Overlay (CVO/TVO) sauber neu und startet es neu — in EINEM Schritt.

.DESCRIPTION
    Loest die "Respawn-Falle": ClaudeVoiceOverlay (CVO) und TerminalVoiceOverlay (TVO)
    laufen je als self-contained publish/.exe mit Watchdog — ein wscript.exe das
    watcher.vbs ausfuehrt PLUS ein interner Selbst-Watchdog (zwei sich gegenseitig
    ueberwachende .exe-Prozesse). Wird nur EIN Prozess gekillt, startet der Watchdog
    ihn sofort neu und lockt die publish/.exe -> publish.ps1 scheitert mit
    UnauthorizedAccessException.

    Dieses Skript killt darum IMMER zuerst ALLE Waechter UND .exe-Prozesse des Ziels
    in einem Rutsch (per NAMENS-Filter, damit die ausfuehrende pwsh nie getroffen wird),
    baut dann die publish/.exe und startet das Overlay sauber wieder ueber watcher.vbs.

    Filtert die Prozesse strikt per Win32_Process-Name (NIE nur per CommandLine —
    ein CommandLine-Filter wuerde die eigene pwsh-Shell matchen und sie killen, Exit 255).

.PARAMETER Target
    Welches Overlay neu gebaut werden soll: CVO, TVO oder Both (beide nacheinander).

.PARAMETER NoStart
    Nur killen + bauen, NICHT neu starten (z.B. wenn man danach manuell etwas pruefen will).

.EXAMPLE
    pwsh -File rebuild-overlay.ps1 CVO
.EXAMPLE
    pwsh -File rebuild-overlay.ps1 TVO
.EXAMPLE
    pwsh -File rebuild-overlay.ps1 Both
.EXAMPLE
    pwsh -File rebuild-overlay.ps1 CVO -NoStart
#>
param(
    [Parameter(Mandatory, Position = 0)]
    [ValidateSet('CVO', 'TVO', 'Both')]
    [string]$Target,

    [switch]$NoStart
)

$ErrorActionPreference = 'Stop'
$repoRoot = $PSScriptRoot

# --- Overlay-Konfiguration (eine Quelle der Wahrheit) ---
$Overlays = @{
    CVO = @{
        Label  = 'Claude Desktop'
        Name   = 'ClaudeVoiceOverlay'
        Folder = Join-Path $repoRoot 'ClaudeVoiceOverlay-Windows'
        Exe    = 'ClaudeVoiceOverlay.exe'
        Port   = 5724
    }
    TVO = @{
        Label  = 'Windows Terminal'
        Name   = 'TerminalVoiceOverlay'
        Folder = Join-Path $repoRoot 'TerminalVoiceOverlay-Windows'
        Exe    = 'TerminalVoiceOverlay.exe'
        Port   = 5723
    }
}

function Write-Step { param([string]$m) Write-Host "  -> $m" -ForegroundColor Gray }
function Write-Head { param([string]$m) Write-Host "`n=== $m ===" -ForegroundColor Cyan }
function Write-Ok   { param([string]$m) Write-Host "  OK: $m" -ForegroundColor Green }
function Write-Warn { param([string]$m) Write-Host "  ! $m" -ForegroundColor Yellow }
function Write-Err  { param([string]$m) Write-Host "  ! $m" -ForegroundColor Red }

# Beendet ALLE Prozesse eines Overlays: zuerst der watcher.vbs (wscript), dann die .exe.
# Watcher zuerst, damit er die exe waehrend des Kills nicht erneut respawnt.
function Stop-Overlay {
    param([hashtable]$O)

    $leaf = Split-Path $O.Folder -Leaf   # z.B. "ClaudeVoiceOverlay-Windows" — eindeutig pro Overlay

    # exe-Prozesse — NAMENS-Filter (CommandLine-Filter wuerde die eigene pwsh treffen!)
    $exeProcs = @(Get-CimInstance Win32_Process -Filter "name='$($O.Exe)'" -ErrorAction SilentlyContinue)

    # watcher: nur wscript.exe (Namens-Filter), die GENAU dieses Overlays watcher.vbs ausfuehren.
    # Der Ordnername im Pfad ist eindeutig -> kein Cross-Match zwischen CVO und TVO.
    $watcherProcs = @(
        Get-CimInstance Win32_Process -Filter "name='wscript.exe'" -ErrorAction SilentlyContinue |
            Where-Object { $_.CommandLine -and ($_.CommandLine -like "*$leaf\watcher.vbs*") }
    )

    $pidsToKill = @()
    $pidsToKill += $watcherProcs.ProcessId   # Watcher ZUERST
    $pidsToKill += $exeProcs.ProcessId
    $pidsToKill = $pidsToKill | Where-Object { $_ } | Select-Object -Unique

    if ($pidsToKill.Count -eq 0) {
        Write-Step 'Keine laufenden Prozesse gefunden (nichts zu beenden).'
        return
    }

    Write-Step "Beende $($pidsToKill.Count) Prozess(e): watcher=$($watcherProcs.Count), exe=$($exeProcs.Count)"
    Stop-Process -Id $pidsToKill -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 2000
}

# Prueft, ob die publish/.exe schreibbar (= nicht gelockt) ist.
function Test-ExeFree {
    param([hashtable]$O)
    $exePath = Join-Path $O.Folder "publish\$($O.Exe)"
    if (-not (Test-Path $exePath)) { return $true }   # noch kein Build -> nichts gelockt
    try {
        $fs = [System.IO.File]::Open($exePath, 'Open', 'ReadWrite', 'None')
        $fs.Close(); $fs.Dispose()
        return $true
    }
    catch { return $false }
}

# Ruft die vorhandene publish.ps1 des Overlays auf (self-contained single-file .exe).
function Invoke-Publish {
    param([hashtable]$O)
    $publishScript = Join-Path $O.Folder 'publish.ps1'
    if (-not (Test-Path $publishScript)) {
        Write-Err "publish.ps1 nicht gefunden: $publishScript"
        return $false
    }
    Push-Location $O.Folder
    try {
        pwsh -NoProfile -File $publishScript
        return ($LASTEXITCODE -eq 0)
    }
    finally { Pop-Location }
}

# Startet das Overlay wie beim Boot: ueber watcher.vbs (wscript.exe).
# Faellt auf direkten exe-Start zurueck, falls kein watcher.vbs vorhanden ist.
function Start-Overlay {
    param([hashtable]$O)
    $watcherPath = Join-Path $O.Folder 'watcher.vbs'
    if (Test-Path $watcherPath) {
        Start-Process wscript.exe -ArgumentList "`"$watcherPath`""
        Write-Step "Gestartet ueber watcher.vbs (wscript.exe)."
    }
    else {
        $exePath = Join-Path $O.Folder "publish\$($O.Exe)"
        Start-Process $exePath
        Write-Step "watcher.vbs fehlt -> exe direkt gestartet."
    }
}

# Wartet kurz und zaehlt die laufenden exe-Prozesse (Boot bringt 2 hoch: Overlay + Watchdog).
function Get-RunningExeCount {
    param([hashtable]$O)
    Start-Sleep -Seconds 6
    return @(Get-CimInstance Win32_Process -Filter "name='$($O.Exe)'" -ErrorAction SilentlyContinue).Count
}

# --- Hauptablauf ---
$targets = if ($Target -eq 'Both') { @('CVO', 'TVO') } else { @($Target) }
$failed = @()

foreach ($t in $targets) {
    $O = $Overlays[$t]
    Write-Head "$t  ($($O.Name) -> $($O.Label))"

    if (-not (Test-Path $O.Folder)) {
        Write-Err "Ordner fehlt: $($O.Folder)"
        $failed += $t
        continue
    }

    # Schritt 1: alle Prozesse beenden
    Write-Step 'Schritt 1/3: Prozesse beenden (Watcher + exe)...'
    Stop-Overlay -O $O

    if (-not (Test-ExeFree -O $O)) {
        Write-Step 'publish/.exe noch gelockt — warte zusaetzlich 2s...'
        Start-Sleep -Seconds 2
    }

    # Schritt 2: bauen
    Write-Step 'Schritt 2/3: publish bauen (dauert ~1-3 Min)...'
    if (-not (Invoke-Publish -O $O)) {
        Write-Err "Build FEHLGESCHLAGEN fuer $t — Neustart uebersprungen."
        $failed += $t
        continue
    }
    Write-Ok 'Build erfolgreich.'

    # Schritt 3: neu starten + verifizieren
    if ($NoStart) {
        Write-Step 'Schritt 3/3: -NoStart gesetzt -> kein Neustart.'
        continue
    }
    Write-Step 'Schritt 3/3: Overlay neu starten...'
    Start-Overlay -O $O
    $running = Get-RunningExeCount -O $O
    if ($running -ge 1) {
        Write-Ok "$t laeuft wieder ($running exe-Prozess(e), AutoEnter-Port $($O.Port))."
    }
    else {
        Write-Warn "$t scheint NICHT gestartet ($running exe-Prozesse). publish\watcher.log pruefen."
        $failed += $t
    }
}

Write-Host ''
if ($failed.Count -eq 0) {
    Write-Host "Fertig — alle Ziele ($($targets -join ', ')) erfolgreich." -ForegroundColor Cyan
    exit 0
}
else {
    Write-Host "Fertig mit Problemen bei: $($failed -join ', ')" -ForegroundColor Red
    exit 1
}
