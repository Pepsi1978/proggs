#Requires -Version 7
# Version 1.4.0 - 30.08.2026, 11:54 Uhr
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
. (Join-Path $repoRoot 'voice-overlay-deploy-guard.ps1')

# --- Overlay-Konfiguration (eine Quelle der Wahrheit) ---
$Overlays = @{
    CVO = @{
        Label  = 'Claude Desktop'
        Name   = 'ClaudeVoiceOverlay'
        Folder = Join-Path $repoRoot 'ClaudeVoiceOverlay-Windows'
        Exe    = 'ClaudeVoiceOverlay.exe'
        Port   = 5724
        Task   = 'Spracheingabe - Claude Desktop'
    }
    TVO = @{
        Label  = 'Windows Terminal'
        Name   = 'TerminalVoiceOverlay'
        Folder = Join-Path $repoRoot 'TerminalVoiceOverlay-Windows'
        Exe    = 'TerminalVoiceOverlay.exe'
        Port   = 5723
        Task   = 'Spracheingabe - Terminal'
    }
}

function Write-Step { param([string]$m) Write-Host "  -> $m" -ForegroundColor Gray }
function Write-Head { param([string]$m) Write-Host "`n=== $m ===" -ForegroundColor Cyan }
function Write-Ok   { param([string]$m) Write-Host "  OK: $m" -ForegroundColor Green }
function Write-Warn { param([string]$m) Write-Host "  ! $m" -ForegroundColor Yellow }
function Write-Err  { param([string]$m) Write-Host "  ! $m" -ForegroundColor Red }

# Beendet ALLE Prozesse eines Overlays. Da sich interner Watchdog und Overlay
# gegenseitig neu starten, wird bis zum stabilen Stillstand wiederholt beendet.
function Stop-Overlay {
    param([hashtable]$O)

    $leaf = Split-Path $O.Folder -Leaf   # z.B. "ClaudeVoiceOverlay-Windows" — eindeutig pro Overlay
    $announced = $false

    # Die Autostart-Aufgaben laufen mit RunLevel=Highest. Aus einem normalen
    # Prozess sind CommandLine/ExecutablePath ihrer wscript.exe daher leer und
    # ein reiner WMI-Pfadfilter kann sie nicht erkennen. Die konkrete Aufgabe
    # zuerst kontrolliert stoppen; danach verbleibende interne Prozesse fangen.
    $scheduledTask = Get-ScheduledTask -TaskName $O.Task -ErrorAction SilentlyContinue
    if ($scheduledTask -and $scheduledTask.State -eq 'Running') {
        Write-Step "Stoppe geplante Aufgabe '$($O.Task)'..."
        Stop-ScheduledTask -TaskName $O.Task -ErrorAction Stop
        $taskDeadline = (Get-Date).AddSeconds(10)
        do {
            Start-Sleep -Milliseconds 200
            $scheduledTask = Get-ScheduledTask -TaskName $O.Task -ErrorAction SilentlyContinue
        } while ($scheduledTask -and $scheduledTask.State -eq 'Running' -and (Get-Date) -lt $taskDeadline)
        if ($scheduledTask -and $scheduledTask.State -eq 'Running') {
            throw "Geplante Aufgabe '$($O.Task)' liess sich nicht stoppen; Build sicher abgebrochen."
        }
    }

    for ($attempt = 1; $attempt -le 20; $attempt++) {
        # Ausschliesslich per Prozessname/WScript-Pfad suchen, damit die
        # ausfuehrende PowerShell niemals als Treffer beendet wird.
        $exeProcs = @(Get-CimInstance Win32_Process -Filter "name='$($O.Exe)'" -ErrorAction SilentlyContinue)
        $watcherProcs = @(
            Get-CimInstance Win32_Process -Filter "name='wscript.exe' OR name='cscript.exe'" -ErrorAction SilentlyContinue |
                Where-Object { $_.CommandLine -and ($_.CommandLine -like "*$leaf\watcher.vbs*") }
        )

        if ($exeProcs.Count -eq 0 -and $watcherProcs.Count -eq 0) {
            if (-not $announced) {
                Write-Step 'Keine laufenden Prozesse gefunden (nichts zu beenden).'
            }
            return
        }

        if (-not $announced) {
            Write-Step "Beende Prozesse bis zum stabilen Stillstand: watcher=$($watcherProcs.Count), exe=$($exeProcs.Count)"
            $announced = $true
        }

        # EXEs zuerst: der externe VBS-Watcher prueft nur alle drei Sekunden.
        # Danach alle VBS-Watcher beenden und frisch erzeugte Prozesse im
        # naechsten Durchlauf mitnehmen.
        if ($exeProcs.Count -gt 0) {
            Stop-Process -Id @($exeProcs.ProcessId) -Force -ErrorAction SilentlyContinue
        }
        if ($watcherProcs.Count -gt 0) {
            Stop-Process -Id @($watcherProcs.ProcessId) -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Milliseconds 250
    }

    $remainingExe = @(Get-CimInstance Win32_Process -Filter "name='$($O.Exe)'" -ErrorAction SilentlyContinue)
    $remainingWatcher = @(
        Get-CimInstance Win32_Process -Filter "name='wscript.exe' OR name='cscript.exe'" -ErrorAction SilentlyContinue |
            Where-Object { $_.CommandLine -and ($_.CommandLine -like "*$leaf\watcher.vbs*") }
    )
    if ($remainingExe.Count -eq 0 -and $remainingWatcher.Count -eq 0) {
        return
    }
    throw "Prozesse von $($O.Name) starten sich trotz wiederholtem Quiesce weiter neu; Build sicher abgebrochen."
}

# Datenverlust-Schutz: Reserviert das Overlay atomar VOR dem Kill. Die laufende App
# antwortet erst im echten Idle-Zustand mit ready=true und blockiert danach neue
# Aufnahmen. Unlesbarer Status bei laufendem Prozess bleibt fail-closed. Nur fuer die
# einmalige Migration alter Binaries wird /recording/status stabil bestaetigt und vor
# dem Kill ein zweites Mal geprueft.
function Wait-OverlayIdle {
    param([hashtable]$O, [int]$TimeoutSeconds = 180)
    return Enter-VoiceOverlayDeploymentWindow -ProcessName $O.Name -Port $O.Port -TimeoutSeconds $TimeoutSeconds
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
        # Ohne Out-Host wuerde die gesamte Kindprozess-Ausgabe Teil des
        # Funktionsrueckgabewerts. Ein Array aus Textzeilen plus abschliessendem
        # $false ist in PowerShell truthy und maskierte bisher Publish-Fehler.
        pwsh -NoProfile -File $publishScript | Out-Host
        $publishExitCode = $LASTEXITCODE
        return ($publishExitCode -eq 0)
    }
    finally { Pop-Location }
}

function Test-BuiltArtifact {
    param([hashtable]$O)

    $projectPath = Join-Path $O.Folder "$($O.Name).csproj"
    $exePath = Join-Path $O.Folder "publish\$($O.Exe)"
    if (-not (Test-Path -LiteralPath $projectPath -PathType Leaf) -or
        -not (Test-Path -LiteralPath $exePath -PathType Leaf)) {
        return @{ Ok = $false; Expected = '<fehlt>'; Actual = '<fehlt>' }
    }

    [xml]$project = Get-Content -LiteralPath $projectPath -Raw
    $expected = [string]$project.Project.PropertyGroup.Version
    $actual = (Get-Item -LiteralPath $exePath).VersionInfo.ProductVersion
    $versionMatches = $actual -eq $expected -or
        $actual.StartsWith("$expected ", [StringComparison]::OrdinalIgnoreCase) -or
        $actual.StartsWith("$expected+", [StringComparison]::OrdinalIgnoreCase)
    return @{ Ok = $versionMatches; Expected = $expected; Actual = $actual }
}

# Startet das Overlay wie beim Boot: ueber watcher.vbs (wscript.exe).
# Faellt auf direkten exe-Start zurueck, falls kein watcher.vbs vorhanden ist.
function Start-Overlay {
    param([hashtable]$O)
    $scheduledTask = Get-ScheduledTask -TaskName $O.Task -ErrorAction SilentlyContinue
    if ($scheduledTask) {
        Start-ScheduledTask -TaskName $O.Task -ErrorAction Stop
        Write-Step "Gestartet ueber geplante Aufgabe '$($O.Task)'."
        return
    }

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
    if ($procs.Count -lt 1) {
        return @{ Ok = $false; Count = 0; Stale = 0; Version = '<keiner>'; Endpoint = $false; PortOpen = $false }
    }
    $threshold = $BuildTime.AddSeconds(-2)
    $stale = @($procs | Where-Object { $_.StartTime -lt $threshold })
    $exePath = Join-Path $O.Folder "publish\$($O.Exe)"
    $ver = try { (Get-Item -LiteralPath $exePath).VersionInfo.ProductVersion } catch { '<unlesbar>' }
    $endpoint = $false
    try {
        $status = Invoke-RestMethod -Method Get -Uri "http://127.0.0.1:$($O.Port)/recording/status" -TimeoutSec 2
        $endpoint = $null -ne $status.busy
    } catch { }
    # Lauscht ueberhaupt jemand? Trennt "Status-Server faehrt noch hoch" von
    # "dieses Overlay bietet gar keinen Status-Server an" — der Status-Server
    # faellt bewusst still aus, wenn der Port belegt ist oder der HttpListener
    # nicht binden darf (siehe AutoEnterStatusServer: die App startet dann
    # trotzdem). Ohne diese Unterscheidung wartet die Verifikation auf etwas,
    # das nie kommt.
    $portOpen = $false
    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $connect = $client.ConnectAsync('127.0.0.1', $O.Port)
        $portOpen = $connect.Wait(500) -and $client.Connected
        $client.Close()
    } catch { }
    return @{ Ok = ($stale.Count -eq 0 -and $endpoint); Count = $procs.Count; Stale = $stale.Count;
              Version = $ver; Endpoint = $endpoint; PortOpen = $portOpen }
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
    $reservationHeld = Wait-OverlayIdle -O $O

    # Schritt 1: alle Prozesse beenden
    Write-Step 'Schritt 1/3: Prozesse beenden (Watcher + exe)...'
    try {
        Stop-Overlay -O $O
    }
    finally {
        Exit-VoiceOverlayDeploymentWindow -ProcessName $O.Name -Port $O.Port -Reserved $reservationHeld
    }

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
    $artifact = Test-BuiltArtifact -O $O
    if (-not $artifact.Ok) {
        Write-Err "Build-Artefakt passt nicht zum Projekt: erwartet $($artifact.Expected), gefunden $($artifact.Actual)."
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
    # WARUM DIE WARTEZEITEN SO KURZ SIND (Vorfall 30.08.2026):
    # Hier standen 10 Minuten pro Versuch, bei zwei Versuchen also bis zu 20
    # Minuten — und zwar STUMM, mit einer einzigen Zeile am Anfang. Bei einem
    # Overlay ohne Status-Server (ein vorgesehener Zustand, siehe
    # AutoEnterStatusServer) lief diese Zeit immer voll ab, ohne dass je etwas
    # kommen konnte. Ein Overlay, das ueberhaupt starten will, ist in Sekunden
    # oben; 90 s sind grosszuegig und melden sich unterwegs.
    $StartupTimeoutSeconds = 90
    # So lange darf ein Overlay ohne offenen Port als "faehrt noch hoch" gelten,
    # bevor auf die Verifikation ohne Endpunkt zurueckgefallen wird.
    $PortGraceSeconds = 20

    $verified = $false
    for ($attempt = 1; $attempt -le 2 -and -not $verified; $attempt++) {
        Start-Overlay -O $O
        $startupStarted = Get-Date
        $startupDeadline = $startupStarted.AddSeconds($StartupTimeoutSeconds)
        $announcedEndpointWait = $false
        $lastStartupProgress = $startupStarted
        $verifiedWithoutEndpoint = $false
        do {
            Start-Sleep -Seconds 2
            $r = Test-FreshVersion -O $O -BuildTime $buildTime
            if ($r.Ok) {
                Write-Ok "$t laeuft wieder ($($r.Count) exe, NEUE Version $($r.Version) VERIFIZIERT, Port $($O.Port))."
                $verified = $true
                break
            }
            if ($r.Stale -gt 0) {
                break
            }

            $waited = ((Get-Date) - $startupStarted).TotalSeconds

            # Prozesse laufen, sind alle frisch, aber es lauscht niemand auf dem
            # Port: dieses Overlay bietet den Status-Server nicht an. Das
            # faelschungssichere Kernsignal bleibt trotzdem gueltig — ein
            # Prozess, der NACH dem Build gestartet wurde, fuehrt zwingend den
            # neuen Code aus. Also verifizieren statt bis zum Timeout warten;
            # die schwaechere Grundlage wird ausdruecklich gemeldet.
            if ($r.Count -gt 0 -and -not $r.PortOpen -and $waited -ge $PortGraceSeconds) {
                Write-Warn "$t bietet keinen Status-Server auf Port $($O.Port) an (niemand lauscht nach $([int]$waited) s)."
                Write-Ok "$t laeuft wieder ($($r.Count) exe, NEUE Version $($r.Version) VERIFIZIERT ueber die Startzeit; der Aufnahme-Status ist bei diesem Overlay nicht abfragbar)."
                $verified = $true
                $verifiedWithoutEndpoint = $true
                break
            }

            if ($r.Count -gt 0 -and -not $r.Endpoint -and -not $announcedEndpointWait) {
                Write-Step "$t initialisiert noch; warte bis zu $StartupTimeoutSeconds s, bis Port $($O.Port) den Aufnahme-Status liefert..."
                $announcedEndpointWait = $true
            }
            if (((Get-Date) - $lastStartupProgress).TotalSeconds -ge 10) {
                Write-Host "  ... ${t}: seit $([int]$waited) s gestartet — exe=$($r.Count), Port offen=$($r.PortOpen), Status lesbar=$($r.Endpoint)" -ForegroundColor DarkGray
                $lastStartupProgress = Get-Date
            }
        } while ((Get-Date) -lt $startupDeadline)

        if (-not $verified) {
            if ($r.Stale -gt 0) {
                Write-Err "${t}: ALTE VERSION laeuft noch ($($r.Stale) Prozess(e) AELTER als der Build)! Toete ALLE + starte erneut (Versuch $attempt/2)."
            }
            elseif ($r.Count -lt 1) {
                Write-Warn "$t scheint NICHT gestartet (0 exe). Versuch $attempt/2."
            }
            else {
                Write-Warn "$t lieferte innerhalb von $StartupTimeoutSeconds s keinen gueltigen Aufnahme-Status auf Port $($O.Port). Versuch $attempt/2."
            }
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
