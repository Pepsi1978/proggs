# Game-mode watcher (version: see README.md).
# Runs as scheduled task "Spielmodus-Waechter" in the user context with highest privileges.
# While Steam or Call of Duty runs: stops Microsoft Store installs (InstallService, start type locked)
# and Gaming Services (protected package services: stop only, killed again if they come back),
# ends Store and Xbox app processes. Afterwards restores the previous state.
# Also blocks Google Drive sync (firewall block rule) while the lock is on.
# While a Steam game runs (RunningAppID) or CoD: Windows Firewall blocks ALL outbound traffic except
# the running game folder, Steam, anti-cheat, Google Chrome and the local subnet (router page).
# Toggle via desktop shortcut "Spielmodus an-aus" (aus.flag).
# Designed to cost almost nothing while gaming: one process snapshot every 4 s, no pipelines,
# no blocking service waits, logging only on state changes.

$ErrorActionPreference = 'Continue'
$daten   = Join-Path $env:LOCALAPPDATA 'Spielmodus'
$log     = Join-Path $daten 'waechter.log'
$sicher  = Join-Path $daten 'starttypen.json'
$flagAus = Join-Path $daten 'aus.flag'
New-Item -ItemType Directory -Force $daten | Out-Null

$ausloeserNamen = @('steam', 'cod')                 # exact process names
$ausloeserMuster = '^(cod\d\d-cod|sp\d\d-cod)$'     # older CoD titles
$dienstGesperrt = 'InstallService'                   # start type can be locked
$dienstGestoppt = @('GamingServices', 'GamingServicesNet')
# Only app processes are killed. Killing gamingservices.exe counts as a service crash and the SCM
# recovery restarts it at once (seen 17.11.2026), so services are stopped cleanly instead.
$stopProz       = @('WinStore.App', 'XboxPcApp', 'XboxPcAppFT')
$entprellSek    = 8                                # trigger must be gone this long before unlocking
$pwsh           = (Get-Process -Id $PID).Path
$fwSicher       = Join-Path $daten 'firewall.json'   # exists only while the game firewall is on
$fwGruppe       = 'Spielmodus'                       # temporary rules (Drive block)
$fwErlaubt      = 'Spielmodus-Erlaubt'               # persistent allow rules (harmless when outbound is Allow)
$driveRegel     = 'Spielmodus: Google Drive gesperrt'

function Write-Log([string]$t) {
    try {
        Add-Content -Path $log -Value ('{0:dd.MM.yyyy HH:mm:ss}  {1}' -f (Get-Date), $t) -Encoding utf8
        if ((Get-Item $log).Length -gt 2MB) { Move-Item $log "$log.alt" -Force }
    } catch { }
}

function Show-Meldung([string]$t) {
    $skript = Join-Path $PSScriptRoot 'meldung.ps1'
    try {
        Start-Process "$env:WINDIR\System32\conhost.exe" -WindowStyle Hidden -ArgumentList "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$skript`" -Text `"$t`""
    } catch { Write-Log "Mitteilung fehlgeschlagen: $($_.Exception.Message)" }
}

function Get-Ausloeser {
    # One cheap snapshot; returns the names of running trigger processes.
    $treffer = foreach ($p in [Diagnostics.Process]::GetProcesses()) {
        $n = $p.ProcessName
        if ($ausloeserNamen -contains $n -or $n -match $ausloeserMuster) { $n }
        $p.Dispose()
    }
    return @($treffer | Sort-Object -Unique)
}

function Get-Originaltyp {
    $typ = $null
    if (Test-Path $sicher) { try { $typ = (Get-Content $sicher -Raw | ConvertFrom-Json).$dienstGesperrt } catch { } }
    if (-not $typ -or $typ -eq 'Disabled') { $typ = 'Manual' }   # never treat 'Disabled' as original
    return $typ
}

function Save-Starttypen {
    if (Test-Path $sicher) { return }
    $svc = Get-Service $dienstGesperrt -ErrorAction SilentlyContinue
    $typ = if ($svc -and "$($svc.StartType)" -ne 'Disabled') { "$($svc.StartType)" } else { 'Manual' }
    @{ $dienstGesperrt = $typ } | ConvertTo-Json | Set-Content $sicher -Encoding utf8
    Write-Log "Starttyp gesichert: $dienstGesperrt=$typ"
}

function Get-LaufendeDienste {
    # While CoD runs it restarts Gaming Services itself (measured 17.11.2026: ~30 stop/start rounds
    # per session) - fighting it gains no bandwidth and risks hitches, so they are left alone then.
    $ziele = if ($script:codLaeuft) { @($dienstGesperrt) } else { @($dienstGesperrt) + $dienstGestoppt }
    return @(foreach ($d in $ziele) {
        $s = Get-Service $d -ErrorAction SilentlyContinue
        if ($s -and $s.Status -ne 'Stopped') { $s }
    })
}

function Stop-Ziele {
    # Clean stop with a bounded wait (never hangs on StopPending), then end app processes.
    $laufend = Get-LaufendeDienste
    foreach ($s in $laufend) { try { Stop-Service $s.Name -Force -NoWait -ErrorAction Stop } catch { } }
    foreach ($s in $laufend) { try { $s.WaitForStatus('Stopped', [TimeSpan]::FromSeconds(15)) } catch { Write-Log "$($s.Name) nach 15 s nicht gestoppt" } }
    Stop-Process -Name $stopProz -Force -ErrorAction SilentlyContinue
}

function Enable-Sperre {
    $script:gamingWarAktiv = (Get-Service 'GamingServices' -ErrorAction SilentlyContinue).Status -eq 'Running'
    try { Set-Service $dienstGesperrt -StartupType Disabled -ErrorAction Stop } catch { Write-Log "Starttyp $dienstGesperrt nicht setzbar: $($_.Exception.Message)" }
    Stop-Ziele
    Enable-DriveSperre
}

function Disable-Sperre {
    $typ = Get-Originaltyp
    Disable-DriveSperre
    try { Set-Service $dienstGesperrt -StartupType $typ -ErrorAction Stop } catch { Write-Log "Rückstellen $dienstGesperrt fehlgeschlagen: $($_.Exception.Message)" }
    # Only restart Gaming Services if they were running before the lock.
    if ($script:gamingWarAktiv) { Start-Service $dienstGestoppt -ErrorAction SilentlyContinue }
}

# ---------- Google Drive: block sync via firewall (Drive stays usable locally) ----------
function Get-DrivePfade {
    $pf = @(foreach ($p in [Diagnostics.Process]::GetProcesses()) {
        if ($p.ProcessName -eq 'GoogleDriveFS') { try { $p.MainModule.FileName } catch { } }
        $p.Dispose()
    })
    $pf += @(Get-ChildItem "$env:ProgramFiles\Google\Drive File Stream\*\GoogleDriveFS.exe" -ErrorAction SilentlyContinue | ForEach-Object FullName)
    return @($pf | Where-Object { $_ } | Sort-Object -Unique)
}

function Enable-DriveSperre {
    Remove-NetFirewallRule -DisplayName $driveRegel -ErrorAction SilentlyContinue
    foreach ($p in Get-DrivePfade) {
        try { New-NetFirewallRule -DisplayName $driveRegel -Group $fwGruppe -Direction Outbound -Action Block -Program $p -Profile Any -ErrorAction Stop | Out-Null }
        catch { Write-Log "Drive-Sperre fehlgeschlagen: $($_.Exception.Message)" }
    }
    $n = @(Get-NetFirewallRule -DisplayName $driveRegel -ErrorAction SilentlyContinue).Count
    Write-Log $(if ($n -gt 0) { "Google Drive: Abgleich gesperrt ($n Regel(n))" } else { 'Google Drive: nicht gefunden, nichts gesperrt' })
}

function Disable-DriveSperre {
    Remove-NetFirewallRule -DisplayName $driveRegel -ErrorAction SilentlyContinue
}

# ---------- Game firewall: only the running game, Steam, anti-cheat, Chrome, LAN ----------
function Get-SpielAppId {
    try { return [int](Get-ItemPropertyValue 'HKCU:\Software\Valve\Steam' -Name RunningAppID -ErrorAction Stop) } catch { return 0 }
}

function Get-SteamPfad {
    try { return ((Get-ItemPropertyValue 'HKCU:\Software\Valve\Steam' -Name SteamPath -ErrorAction Stop) -replace '/', '\') } catch { return "${env:ProgramFiles(x86)}\Steam" }
}

function Get-SpielOrdner([int]$appId) {
    $steam = Get-SteamPfad
    $libs = @($steam)
    $vdf = Join-Path $steam 'steamapps\libraryfolders.vdf'
    if (Test-Path $vdf) {
        foreach ($m in [regex]::Matches((Get-Content $vdf -Raw), '"path"\s+"([^"]+)"')) { $libs += ($m.Groups[1].Value -replace '\\\\', '\') }
    }
    foreach ($lib in ($libs | Sort-Object -Unique)) {
        $acf = Join-Path $lib "steamapps\appmanifest_$appId.acf"
        if (Test-Path $acf) {
            $m = [regex]::Match((Get-Content $acf -Raw), '"installdir"\s+"([^"]+)"')
            if ($m.Success) { return (Join-Path $lib "steamapps\common\$($m.Groups[1].Value)") }
        }
    }
    return $null
}

function Add-Erlaubt([string[]]$programme) {
    if ($null -eq $script:erlaubtSet) {
        $script:erlaubtSet = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
        try { Get-NetFirewallRule -Group $fwErlaubt -ErrorAction Stop | Get-NetFirewallApplicationFilter | ForEach-Object { [void]$script:erlaubtSet.Add("$($_.Program)") } } catch { }
    }
    $neu = 0
    foreach ($p in $programme) {
        if (-not $p -or $script:erlaubtSet.Contains($p) -or -not (Test-Path -LiteralPath $p)) { continue }
        try {
            New-NetFirewallRule -DisplayName "Spielmodus erlaubt: $(Split-Path $p -Leaf)" -Group $fwErlaubt -Direction Outbound -Action Allow -Program $p -Profile Any -ErrorAction Stop | Out-Null
            [void]$script:erlaubtSet.Add($p); $neu++
        } catch { Write-Log "Erlaubnis für $p fehlgeschlagen: $($_.Exception.Message)" }
    }
    if ($neu -gt 0) { Write-Log "Firewall: $neu Programm(e) neu erlaubt" }
}

function Add-GrundErlaubnis {
    $steam = Get-SteamPfad
    $liste = @(Get-ChildItem $steam -Filter *.exe -ErrorAction SilentlyContinue | ForEach-Object FullName)
    $liste += @(Get-ChildItem (Join-Path $steam 'bin') -Filter *.exe -Recurse -ErrorAction SilentlyContinue | ForEach-Object FullName)
    $liste += @(
        "${env:ProgramFiles(x86)}\Common Files\Steam\steamservice.exe",
        "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
        "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
        "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe",
        "${env:ProgramFiles(x86)}\EasyAntiCheat_EOS\EasyAntiCheat_EOS.exe",
        "${env:ProgramFiles(x86)}\EasyAntiCheat\EasyAntiCheat.exe",
        "${env:ProgramFiles(x86)}\Common Files\BattlEye\BEService.exe",
        "${env:ProgramFiles(x86)}\Common Files\BattlEye\BEService_x64.exe"
    )
    # Coding CLIs stay online during games (e.g. "check my ping while I play"): Claude Code, Codex,
    # OpenCode (node), OpenLauncher and the tools they call (git incl. https helper, gh, curl, shells, python).
    $cli = @(
        "$env:USERPROFILE\.local\bin\claude.exe",
        "$env:ProgramFiles\nodejs\node.exe",
        "$env:ProgramFiles\Git\cmd\git.exe",
        "$env:ProgramFiles\Git\mingw64\bin\git.exe",
        "$env:ProgramFiles\Git\mingw64\libexec\git-core\git-remote-https.exe",
        "$env:ProgramFiles\Git\mingw64\bin\curl.exe",
        "$env:ProgramFiles\Git\usr\bin\ssh.exe",
        "$env:ProgramFiles\GitHub CLI\gh.exe",
        "$env:WINDIR\System32\curl.exe",
        "$env:ProgramFiles\PowerShell\7\pwsh.exe",
        "$env:WINDIR\System32\WindowsPowerShell\v1.0\powershell.exe",
        "$env:LOCALAPPDATA\Programs\Python\Python313\python.exe",
        "$env:USERPROFILE\proggs\OpenLauncher\bin\Release\net8.0-windows10.0.19041.0\win-x64\OpenLauncher.exe"
    )
    $cli += @(Get-ChildItem "$env:LOCALAPPDATA\Programs\OpenAI\Codex\bin" -Filter *.exe -ErrorAction SilentlyContinue | ForEach-Object FullName)
    $cli += @(Get-ChildItem "$env:USERPROFILE\.local\share\claude\versions" -Filter *.exe -Recurse -ErrorAction SilentlyContinue | ForEach-Object FullName)
    Add-Erlaubt ($liste + $cli)
    # ping (ICMPv4) for every program, tiny traffic, needed for latency checks
    if (-not (Get-NetFirewallRule -DisplayName 'Spielmodus erlaubt: Ping' -ErrorAction SilentlyContinue)) {
        try { New-NetFirewallRule -DisplayName 'Spielmodus erlaubt: Ping' -Group $fwErlaubt -Direction Outbound -Action Allow -Protocol ICMPv4 -Profile Any -ErrorAction Stop | Out-Null }
        catch { Write-Log "Ping-Erlaubnis fehlgeschlagen: $($_.Exception.Message)" }
    }
    # Home network (router page, adb over Wi-Fi) for every program
    if (-not (Get-NetFirewallRule -DisplayName 'Spielmodus erlaubt: Heimnetz' -ErrorAction SilentlyContinue)) {
        try { New-NetFirewallRule -DisplayName 'Spielmodus erlaubt: Heimnetz' -Group $fwErlaubt -Direction Outbound -Action Allow -RemoteAddress LocalSubnet -Profile Any -ErrorAction Stop | Out-Null }
        catch { Write-Log "Heimnetz-Erlaubnis fehlgeschlagen: $($_.Exception.Message)" }
    }
}

function Add-SpielErlaubnis([int]$appId) {
    # Scan each game folder once per watcher run (CoD folders are huge); rules persist anyway.
    if ($appId -le 0 -or $script:gescannt.Contains($appId)) { return }
    $o = Get-SpielOrdner $appId
    if (-not $o) { Write-Log "Spielordner für App $appId nicht gefunden"; return }
    Add-Erlaubt @(Get-ChildItem -LiteralPath $o -Filter *.exe -Recurse -ErrorAction SilentlyContinue | ForEach-Object FullName)
    [void]$script:gescannt.Add($appId)
}

function Enable-SpielFirewall([int]$appId) {
    # Returns $true only when outbound is really blocked (and the restore file exists).
    Add-GrundErlaubnis
    Add-SpielErlaubnis $appId
    if (-not (Test-Path $fwSicher)) {
        # Existing outbound allow rules (Claude Desktop, Teams, Store apps, telemetry, backup ...) would
        # still pass under "block by default" - switch them off for the game, keep only Core Networking
        # (DNS, DHCP). Saved BEFORE disabling so a crash can always restore them.
        $fremd = @(Get-NetFirewallRule -Direction Outbound -Enabled True -Action Allow -ErrorAction SilentlyContinue |
            Where-Object { $_.Group -ne $fwErlaubt -and $_.Group -ne '@FirewallAPI.dll,-25000' -and $_.DisplayGroup -notmatch '^(Kernnetzwerk|Core Networking)$' })
        $vorher = @{ Deaktiviert = @($fremd | ForEach-Object Name) }
        foreach ($pr in Get-NetFirewallProfile) { $vorher[$pr.Name] = "$($pr.DefaultOutboundAction)" }
        try { $vorher | ConvertTo-Json | Set-Content $fwSicher -Encoding utf8 -ErrorAction Stop }
        catch { Write-Log "firewall.json nicht schreibbar, Spiel-Firewall bleibt aus: $($_.Exception.Message)"; return $false }
        if ($fremd.Count -gt 0) {
            try { $fremd | Disable-NetFirewallRule -ErrorAction Stop; Write-Log "Firewall: $($fremd.Count) fremde ausgehende Freigaben vorübergehend aus" }
            catch { Write-Log "Fremde Freigaben nicht abschaltbar: $($_.Exception.Message)" }
        }
    }
    try { Set-NetFirewallProfile -All -DefaultOutboundAction Block -ErrorAction Stop; Write-Log "Spiel-Firewall AN (App $appId): nur Spiel, Steam, Chrome, Heimnetz"; return $true }
    catch { Write-Log "Spiel-Firewall nicht setzbar: $($_.Exception.Message)"; return $true }   # restore file exists -> undo anyway later
}

function Disable-SpielFirewall {
    # Returns $true when everything was restored; on failure firewall.json stays for a retry.
    if (-not (Test-Path $fwSicher)) { return $true }   # never touch a firewall we did not change
    $vorher = $null
    try { $vorher = Get-Content $fwSicher -Raw | ConvertFrom-Json } catch { }
    $ok = $true
    foreach ($pr in @('Domain', 'Private', 'Public')) {
        $wert = if ($vorher -and $vorher.$pr -and $vorher.$pr -ne 'Block') { $vorher.$pr } else { 'NotConfigured' }
        try { Set-NetFirewallProfile -Name $pr -DefaultOutboundAction $wert -ErrorAction Stop } catch { $ok = $false; Write-Log "Firewall $pr zurückstellen fehlgeschlagen: $($_.Exception.Message)" }
    }
    $namen = @($vorher.Deaktiviert | Where-Object { $_ })
    if ($namen.Count -gt 0) {
        try { Enable-NetFirewallRule -Name $namen -ErrorAction Stop } catch { Write-Log "Fremde Freigaben teilweise nicht wieder an: $($_.Exception.Message)" }
    }
    if (-not $ok) { return $false }
    Remove-Item $fwSicher -ErrorAction SilentlyContinue
    Write-Log 'Spiel-Firewall AUS: alle Programme haben wieder Internet'
    return $true
}

Write-Log "Wächter gestartet (PID $PID)"
Save-Starttypen
$gesperrt = $false
$script:gamingWarAktiv = $true
$script:codLaeuft = $false
$zuletztGesehen = [datetime]::MinValue
$steamLief = (Get-Ausloeser) -contains 'steam'
$restMeldung = ''

# Reconcile after a crash/reboot during a lock: InstallService must not stay disabled.
if ((Get-Service $dienstGesperrt -ErrorAction SilentlyContinue).StartType -eq 'Disabled') {
    Write-Log 'Altlast: InstallService war noch gesperrt, stelle zurück'
    Disable-Sperre
}
# Same after a crash during a game: outbound must not stay blocked, Drive must sync again.
if (Test-Path $fwSicher) { Write-Log 'Altlast: Spiel-Firewall war noch an, stelle zurück'; [void](Disable-SpielFirewall) }
Disable-DriveSperre
$fwAn = $false
$fwAppId = -1
$spielZuletzt = [datetime]::MinValue
$script:gescannt = [Collections.Generic.HashSet[int]]::new()
# Steam apps that are tools, not games: never cut the whole PC off for them
$keinSpiel = @(431960, 250820, 629520, 228980)   # Wallpaper Engine, SteamVR, Soundpad, Steamworks Common Redistributables

while ($true) {
    $aus    = Test-Path $flagAus
    $laeuft = Get-Ausloeser
    if ($laeuft.Count -gt 0) { $zuletztGesehen = Get-Date }
    $codJetzt = [bool]($laeuft | Where-Object { $_ -ne 'steam' })
    if ($codJetzt -ne $script:codLaeuft -and $gesperrt) { Write-Log ('CoD ' + $(if ($codJetzt) { 'läuft: Gaming Services bleiben in Ruhe' } else { 'beendet: Gaming Services werden wieder gestoppt' })) }
    $script:codLaeuft = $codJetzt

    $steamJetzt = $laeuft -contains 'steam'
    if ($steamJetzt -and -not $steamLief) {
        if ($aus) { Show-Meldung 'Steam gestartet: Spielmodus ist AUS. Store, Xbox und Google Drive laufen normal.' }
        else      { Show-Meldung 'Steam gestartet: Spielmodus ist AN. Store, Xbox und Google Drive pausieren.' }
        Write-Log ('Steam-Start gemeldet (Spielmodus ' + $(if ($aus) { 'AUS' } else { 'AN' }) + ')')
    }
    $steamLief = $steamJetzt

    $soll = (-not $aus) -and (($laeuft.Count -gt 0) -or ($gesperrt -and ((Get-Date) - $zuletztGesehen).TotalSeconds -lt $entprellSek))

    if ($soll -and -not $gesperrt) {
        Write-Log "Sperre AN (Auslöser: $($laeuft -join ', '))"
        Enable-Sperre; $gesperrt = $true
    } elseif (-not $soll -and $gesperrt) {
        Write-Log ('Sperre AUS (' + $(if ($aus) { 'von Hand ausgeschaltet' } else { 'kein Spiel/Steam mehr' }) + ')')
        Disable-Sperre; $gesperrt = $false; $restMeldung = ''
        if (-not $aus) { Show-Meldung 'Steam beendet: Spielmodus pausiert. Google Drive, Store und Xbox laufen wieder normal.' }
    }

    # Game firewall: only while a real Steam game runs (Steam running AND RunningAppID set, no tool app).
    # A stale RunningAppID after a Steam crash does not count, Steam must be running.
    $appId = Get-SpielAppId
    $spielJetzt = $steamJetzt -and ($appId -gt 0) -and ($keinSpiel -notcontains $appId)
    if ($spielJetzt) { $spielZuletzt = Get-Date }
    $fwSoll = (-not $aus) -and ($spielJetzt -or ($fwAn -and ((Get-Date) - $spielZuletzt).TotalSeconds -lt $entprellSek))
    if ($fwSoll -and -not $fwAn) {
        # No status window here: a top-most window could pull a fullscreen game out of focus.
        if (Enable-SpielFirewall $appId) { $fwAn = $true; $fwAppId = $appId }
    } elseif ($fwAn -and $fwSoll -and $spielJetzt -and $appId -ne $fwAppId) {
        Add-SpielErlaubnis $appId; $fwAppId = $appId
    } elseif ($fwAn -and -not $fwSoll) {
        if (Disable-SpielFirewall) {
            $fwAn = $false; $fwAppId = -1
            if (-not $aus) { Show-Meldung 'Spiel beendet: Alle Programme haben wieder Internet.' }
        }
    }

    if ($gesperrt) {
        $rest = @([Diagnostics.Process]::GetProcesses() | Where-Object { $stopProz -contains $_.ProcessName } | ForEach-Object { $_.ProcessName })
        $rest += @(Get-LaufendeDienste | ForEach-Object { $_.Name })
        if ($rest.Count -gt 0) {
            Stop-Ziele
            $txt = ($rest | Sort-Object -Unique) -join ', '
            if ($txt -ne $restMeldung) { Write-Log "Wieder aufgetaucht, beendet: $txt"; $restMeldung = $txt }
        }
    }

    Start-Sleep -Seconds 4
}
