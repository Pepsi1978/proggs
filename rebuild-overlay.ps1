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

# Datenverlust-Schutz (Frank-Regel 2026-06-21): Wartet VOR dem Kill, solange das Overlay
# gerade aufnimmt/transkribiert (Frank spricht ein). Fragt GET /recording/status ab
# ({"busy":true|false}). Der Status haengt am OVERLAY, nicht an einer Claude-Session —
# deshalb wirkt der Schutz auch session-uebergreifend: egal welche (Hintergrund-)Session
# den Rebuild ausloest, gekillt wird erst, wenn Frank seinen Text fertig eingefuegt hat.
# Endpoint nicht erreichbar (altes Overlay ohne Endpoint / nicht gestartet) -> NICHT
# blockieren, normal weiter (Rueckwaertskompatibilitaet).
function Wait-OverlayIdle {
    param([hashtable]$O, [int]$TimeoutSeconds = 600)
    $url = "http://127.0.0.1:$($O.Port)/recording/status"
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $announced = $false
    while ((Get-Date) -lt $deadline) {
        try {
            $resp = Invoke-RestMethod -Uri $url -TimeoutSec 2 -ErrorAction Stop
        }
        catch {
            return   # Endpoint nicht da -> nicht blockieren
        }
        if (-not $resp.busy) {
            # Sicherheits-Nachlauf (Frank-Wunsch 2026-06-22): nach dem letzten Busy
            # noch 5s warten, damit Transkription + Einfuegen + Draft-Speichern
            # garantiert durch sind, BEVOR gekillt wird. Beginnt in den 5s wieder
            # eine Aufnahme, wird normal weiter gewartet.
            Write-Step 'Overlay idle — 5s Sicherheits-Nachlauf, dann Kill...'
            Start-Sleep -Seconds 5
            try {
                if ((Invoke-RestMethod -Uri $url -TimeoutSec 2 -ErrorAction Stop).busy) {
                    $announced = $false
                    continue
                }
            } catch { }
            if ($announced) { Write-Ok 'Overlay im Ruhezustand — Rebuild wird fortgesetzt.' }
            return
        }
        if (-not $announced) {
            Write-Warn 'Overlay nimmt gerade auf / transkribiert — warte mit dem Rebuild, bis dein Text fertig eingefuegt ist...'
            $announced = $true
        }
        Start-Sleep -Milliseconds 500
    }
    Write-Warn "Timeout ($TimeoutSeconds s) beim Warten auf Ruhezustand — fahre trotzdem fort."
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

# Liefert die laufenden exe-Prozesse des Overlays als Process-Objekte (mit StartTime).
function Get-OverlayProcs {
    param([hashtable]$O)
    $procName = [IO.Path]::GetFileNameWithoutExtension($O.Exe)
    return @(Get-Process -Name $procName -ErrorAction SilentlyContinue)
}

# Verifiziert NACH dem Neustart, dass die LAUFENDE Version die FRISCH GEBAUTE ist.
# Kernsignal (faelschungssicher): ein Prozess, der VOR dem neuen Build gestartet wurde,
# kann unmoeglich den neuen Code ausfuehren. MainModule.FileVersionInfo liest die DATEI
# auf der Platte und wuerde einen alten, ueberlebenden Prozess faelschlich als "neu"
# melden — deshalb StartTime gegen die Build-Zeit, NICHT die Datei-Version.
# $true, wenn >=1 Prozess laeuft UND KEINER aelter ist als der Build.
function Test-FreshVersion {
    param([hashtable]$O, [datetime]$BuildTime)
    $procs = Get-OverlayProcs -O $O
    if ($procs.Count -lt 1) { return @{ Ok = $false; Count = 0; Stale = 0; Version = '<keiner>' } }
    $threshold = $BuildTime.AddSeconds(-2)
    $stale = @($procs | Where-Object { $_.StartTime -lt $threshold })
    $ver = '<unlesbar>'
    try { $ver = ($procs | Sort-Object StartTime -Descending | Select-Object -First 1).MainModule.FileVersionInfo.ProductVersion } catch {}
    return @{ Ok = ($stale.Count -eq 0); Count = $procs.Count; Stale = $stale.Count; Version = $ver }
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

    # Schritt 0: Datenverlust-Schutz — warten, falls gerade aufgenommen/transkribiert wird.
    Wait-OverlayIdle -O $O

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

    # Build-Zeitpunkt der frischen exe merken — Referenz fuer "laeuft die NEUE Version?".
    $exePath = Join-Path $O.Folder "publish\$($O.Exe)"
    $buildTime = (Get-Item $exePath).LastWriteTime

    # Schritt 3: neu starten + VERIFIZIEREN dass die NEUE Version laeuft
    if ($NoStart) {
        Write-Step 'Schritt 3/3: -NoStart gesetzt -> kein Neustart.'
        continue
    }
    Write-Step 'Schritt 3/3: Overlay neu starten + verifizieren dass die NEUE Version laeuft...'
    $verified = $false
    for ($attempt = 1; $attempt -le 2 -and -not $verified; $attempt++) {
        Start-Overlay -O $O
        Start-Sleep -Seconds 6
        $r = Test-FreshVersion -O $O -BuildTime $buildTime
        if ($r.Count -lt 1) {
            Write-Warn "$t scheint NICHT gestartet (0 exe). Versuch $attempt/2."
        }
        elseif ($r.Ok) {
            Write-Ok "$t laeuft wieder ($($r.Count) exe, NEUE Version $($r.Version) VERIFIZIERT, Port $($O.Port))."
            $verified = $true
        }
        else {
            # ALTE EXE laeuft noch (haelt vermutlich den Single-Instance-Mutex, sodass die
            # neue exe sich sofort wieder beendet). ALLE killen und erneut starten.
            Write-Err "${t}: ALTE VERSION laeuft noch ($($r.Stale) Prozess(e) AELTER als der Build)! Toete ALLE + starte erneut (Versuch $attempt/2)."
            Stop-Overlay -O $O
            Start-Sleep -Seconds 2
        }
    }
    if (-not $verified) {
        Write-Err "${t}: Konnte NICHT verifizieren dass die NEUE Version laeuft — bitte manuell pruefen (Task-Manager, publish\watcher.log)."
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
