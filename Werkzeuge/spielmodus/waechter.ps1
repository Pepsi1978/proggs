# Game-mode watcher (version: see README.md).
# Runs as scheduled task "Spielmodus-Waechter" (user context, highest privileges) from the admin-only
# copy in %ProgramFiles%\Spielmodus (installed by einrichten.ps1).
#
# Lock (while Steam or Call of Duty runs): stops Microsoft Store installs (InstallService, start type
# locked) and Gaming Services (protected package services: stopped cleanly, never killed), ends Store
# and Xbox app processes, blocks Google Drive sync (firewall block rule). Afterwards restores all of it.
#
# Game firewall (only while a real Steam game runs: Steam process + RunningAppID, no Steam tool app):
# outbound default Block, foreign outbound allow rules switched off (Core Networking stays), Wi-Fi
# background scans off. Allowed: the game folder, Steam, anti-cheat, other game launchers, Chrome,
# home network, ping, DNS/NCSI, coding CLIs. State is saved in firewall.json BEFORE anything changes;
# notfall.ps1 (SYSTEM task at boot) restores it if the watcher never comes back.
#
# Toggle via desktop shortcut "Spielmodus an-aus" (aus.flag).
# Cheap while gaming: one process snapshot every 4 s, no blocking service waits in the loop
# (stop waits are capped at 3 s, starts never wait), logging only on state changes.

$ErrorActionPreference = 'Continue'
. (Join-Path $PSScriptRoot 'gemeinsam.ps1')
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
$entprellSek    = 8                                  # trigger must be gone this long before unlocking
$pwsh           = (Get-Process -Id $PID).Path
$fwSicher       = Join-Path $daten 'firewall.json'   # exists only while the game firewall is on
$spieleCache    = Join-Path $daten 'spiele.json'     # per app id: build id + exe names (no rescans)
$fwGruppe       = 'Spielmodus'                       # temporary rules (Drive block)
$fwErlaubt      = 'Spielmodus-Erlaubt'               # persistent allow rules (harmless when outbound is Allow)
$driveRegel     = 'Spielmodus: Google Drive gesperrt'
# Steam apps that are tools, not games: they neither start nor end the game firewall
$keinSpiel      = @(431960, 250820, 629520, 228980, 993090)  # Wallpaper Engine, SteamVR, Soundpad, Redistributables, Lossless Scaling

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

function Get-Schnappschuss {
    # The ONE process snapshot per tick: fills $script:prozNamen, returns running trigger names.
    $script:prozNamen = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    $treffer = foreach ($p in [Diagnostics.Process]::GetProcesses()) {
        $n = $p.ProcessName
        [void]$script:prozNamen.Add($n)
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
    # Clean stop; the wait is capped at 3 s in total so the loop (and the firewall) never stalls.
    $laufend = Get-LaufendeDienste
    foreach ($s in $laufend) { try { Stop-Service $s.Name -Force -NoWait -ErrorAction Stop } catch { } }
    $bis = (Get-Date).AddSeconds(3)
    foreach ($s in $laufend) {
        $rest = ($bis - (Get-Date)).TotalMilliseconds
        if ($rest -gt 0) { try { $s.WaitForStatus('Stopped', [TimeSpan]::FromMilliseconds($rest)) } catch { } }
    }
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
    # Only restart Gaming Services if they were running before the lock - and never wait for it
    # (package services can hang in StartPending; Start-Service would block the loop).
    if ($script:gamingWarAktiv) { foreach ($d in $dienstGestoppt) { & sc.exe start $d *> $null } }
}

# ---------- Cut existing TCP connections (a firewall block only stops NEW connections) ----------
Add-Type -Namespace Spielmodus -Name Tcp -MemberDefinition @'
[StructLayout(LayoutKind.Sequential)] public struct ZEILE { public uint State; public uint LocalAddr; public uint LocalPort; public uint RemoteAddr; public uint RemotePort; }
[DllImport("iphlpapi.dll")] public static extern uint SetTcpEntry(ref ZEILE row);
'@ -ErrorAction SilentlyContinue

function Close-Verbindungen([scriptblock]$trennen) {
    # Resets established IPv4 TCP connections to the internet for which $trennen (gets the connection,
    # returns bool) is true. IPv6 cannot be reset this way; those end with their next reconnect.
    $n = 0
    foreach ($c in @(Get-NetTCPConnection -State Established -ErrorAction SilentlyContinue)) {
        if ($c.RemoteAddress -match '^(127\.|10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.|169\.254\.)' -or $c.RemoteAddress -match ':') { continue }
        if (-not (& $trennen $c)) { continue }
        try {
            $z = New-Object Spielmodus.Tcp+ZEILE
            $z.State = 12   # MIB_TCP_STATE_DELETE_TCB
            $z.LocalAddr = [BitConverter]::ToUInt32([Net.IPAddress]::Parse($c.LocalAddress).GetAddressBytes(), 0)
            $z.RemoteAddr = [BitConverter]::ToUInt32([Net.IPAddress]::Parse($c.RemoteAddress).GetAddressBytes(), 0)
            $z.LocalPort = [uint32]((($c.LocalPort -band 0xFF) -shl 8) -bor ($c.LocalPort -shr 8))
            $z.RemotePort = [uint32]((($c.RemotePort -band 0xFF) -shl 8) -bor ($c.RemotePort -shr 8))
            if ([Spielmodus.Tcp]::SetTcpEntry([ref]$z) -eq 0) { $n++ }
        } catch { }
    }
    return $n
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
    $n = 0
    foreach ($p in Get-DrivePfade) {
        try { New-NetFirewallRule -DisplayName $driveRegel -Group $fwGruppe -Direction Outbound -Action Block -Program $p -Profile Any -ErrorAction Stop | Out-Null; $n++ }
        catch { Write-Log "Drive-Sperre fehlgeschlagen: $($_.Exception.Message)" }
    }
    $script:driveGeprueft = Get-Date
    $drivePids = @(Get-Process GoogleDriveFS -ErrorAction SilentlyContinue | ForEach-Object Id)
    $weg = if ($drivePids.Count) { Close-Verbindungen { param($c) $drivePids -contains $c.OwningProcess } } else { 0 }
    Write-Log $(if ($n -gt 0) { "Google Drive: Abgleich gesperrt ($n Regel(n), $weg offene Verbindung(en) getrennt)" } else { 'Google Drive: nicht gefunden, nichts gesperrt' })
}

function Update-DriveSperre {
    # Drive may auto-update during a long session and restart from a new version folder: once a
    # minute, add a block rule for any GoogleDriveFS.exe that has none yet.
    if (((Get-Date) - $script:driveGeprueft).TotalSeconds -lt 60) { return }
    $script:driveGeprueft = Get-Date
    $haben = @(Get-NetFirewallRule -DisplayName $driveRegel -ErrorAction SilentlyContinue | Get-NetFirewallApplicationFilter | ForEach-Object Program)
    foreach ($p in Get-DrivePfade) {
        if ($haben -notcontains $p) {
            try { New-NetFirewallRule -DisplayName $driveRegel -Group $fwGruppe -Direction Outbound -Action Block -Program $p -Profile Any -ErrorAction Stop | Out-Null; Write-Log "Google Drive: neue Version gesperrt ($p)" } catch { }
        }
    }
}

function Disable-DriveSperre {
    Remove-NetFirewallRule -DisplayName $driveRegel -ErrorAction SilentlyContinue
}

# ---------- Game firewall ----------
function Get-SpielAppId {
    try { return [int](Get-ItemPropertyValue 'HKCU:\Software\Valve\Steam' -Name RunningAppID -ErrorAction Stop) } catch { return 0 }
}

function Get-SteamPfad {
    try { return ((Get-ItemPropertyValue 'HKCU:\Software\Valve\Steam' -Name SteamPath -ErrorAction Stop) -replace '/', '\') } catch { return "${env:ProgramFiles(x86)}\Steam" }
}

function Get-SpielInfo([int]$appId) {
    # Game folder and build id from appmanifest_<id>.acf in any Steam library.
    $steam = Get-SteamPfad
    $libs = @($steam)
    $vdf = Join-Path $steam 'steamapps\libraryfolders.vdf'
    if (Test-Path -LiteralPath $vdf) {
        foreach ($m in [regex]::Matches((Get-Content -LiteralPath $vdf -Raw), '"path"\s+"([^"]+)"')) { $libs += ($m.Groups[1].Value -replace '\\\\', '\') }
    }
    foreach ($lib in ($libs | Sort-Object -Unique)) {
        $acf = Join-Path $lib "steamapps\appmanifest_$appId.acf"
        if (Test-Path -LiteralPath $acf) {
            $txt = Get-Content -LiteralPath $acf -Raw
            $m = [regex]::Match($txt, '"installdir"\s+"([^"]+)"')
            $b = [regex]::Match($txt, '"buildid"\s+"(\d+)"')
            if ($m.Success) { return [pscustomobject]@{ Ordner = (Join-Path $lib "steamapps\common\$($m.Groups[1].Value)"); Build = $(if ($b.Success) { $b.Groups[1].Value } else { '' }) } }
        }
    }
    return $null
}

function Add-Erlaubt([string[]]$programme) {
    if ($null -eq $script:erlaubtSet) {
        # Load existing allow rules once; drop rules whose program no longer exists (uninstalled games).
        $script:erlaubtSet = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
        $weg = 0
        foreach ($r in @(Get-NetFirewallRule -Group $fwErlaubt -ErrorAction SilentlyContinue)) {
            $prog = "$(($r | Get-NetFirewallApplicationFilter).Program)"
            if ($prog -and $prog -ne 'Any' -and -not (Test-Path -LiteralPath $prog)) { Remove-NetFirewallRule -Name $r.Name -ErrorAction SilentlyContinue; $weg++ }
            elseif ($prog) { [void]$script:erlaubtSet.Add($prog) }
        }
        if ($weg -gt 0) { Write-Log "Firewall: $weg veraltete Erlaubnis(se) entfernt" }
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

function Get-ExeIn([string[]]$ordner) {
    @(foreach ($o in $ordner) { if (Test-Path -LiteralPath $o) { Get-ChildItem -LiteralPath $o -Filter *.exe -Recurse -ErrorAction SilentlyContinue | ForEach-Object FullName } })
}

function Add-Regel([string]$name, [hashtable]$p) {
    if (Get-NetFirewallRule -DisplayName $name -ErrorAction SilentlyContinue) { return }
    try { New-NetFirewallRule -DisplayName $name -Group $fwErlaubt -Direction Outbound -Action Allow -Profile Any @p -ErrorAction Stop | Out-Null }
    catch { Write-Log "$name fehlgeschlagen: $($_.Exception.Message)" }
}

function Add-GrundErlaubnis {
    if ($script:grundFertig) { return }   # once per watcher run
    $steam = Get-SteamPfad
    $liste = @(Get-ChildItem -LiteralPath $steam -Filter *.exe -ErrorAction SilentlyContinue | ForEach-Object FullName)
    $liste += Get-ExeIn @((Join-Path $steam 'bin'))
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
    # Launchers and anti-cheat that Steam games need outside their own folder (EA, Ubisoft, Rockstar)
    $liste += Get-ExeIn @("$env:ProgramFiles\Electronic Arts", "$env:ProgramFiles\EA\AC",
        "${env:ProgramFiles(x86)}\Ubisoft\Ubisoft Game Launcher", "$env:ProgramFiles\Rockstar Games\Launcher",
        "$env:ProgramFiles\Rockstar Games\Social Club")
    # Coding CLIs stay online during games (e.g. "check my ping while I play"): Claude Code, Codex,
    # OpenCode, OpenLauncher and the tools they call (git + https/credential helpers, gh, curl, shells, python).
    $cli = @(
        "$env:USERPROFILE\.local\bin\claude.exe",
        "$env:ProgramFiles\nodejs\node.exe",
        "$env:APPDATA\npm\node_modules\opencode-ai\bin\opencode.exe",
        "$env:ProgramFiles\GitHub CLI\gh.exe",
        "$env:WINDIR\System32\curl.exe",
        "$env:ProgramFiles\PowerShell\7\pwsh.exe",
        "$env:WINDIR\System32\WindowsPowerShell\v1.0\powershell.exe",
        "$env:USERPROFILE\proggs\OpenLauncher\bin\Release\net8.0-windows10.0.19041.0\win-x64\OpenLauncher.exe"
    )
    # Git for Windows: newer builds live in ucrt64\, older ones in mingw64\ (seen 18.11.2026: push failed
    # mid-game because only mingw64 was allowed) - take the network-relevant tools from wherever they are.
    $cli += @(Get-ChildItem "$env:ProgramFiles\Git" -Recurse -File -ErrorAction SilentlyContinue -Include 'git.exe', 'git-remote-http*.exe', 'git-credential-manager*.exe', 'curl.exe', 'ssh.exe' | ForEach-Object FullName)
    $cli += @(Get-ChildItem "$env:LOCALAPPDATA\Programs\Python\Python3*\python*.exe" -ErrorAction SilentlyContinue | ForEach-Object FullName)
    $cli += Get-ExeIn @("$env:LOCALAPPDATA\Programs\OpenAI\Codex\bin", "$env:APPDATA\npm\node_modules\@openai\codex", "$env:APPDATA\npm\node_modules\opencode-ai")
    # Claude Code version binaries have no extension (e.g. versions\2.1.327)
    $cli += @(Get-ChildItem "$env:USERPROFILE\.local\share\claude\versions" -File -ErrorAction SilentlyContinue | ForEach-Object FullName)
    Add-Erlaubt ($liste + $cli)
    # Protocol/service rules for every program (tiny traffic): ping v4 + v6, encrypted DNS, NCSI
    Add-Regel 'Spielmodus erlaubt: Ping' @{ Protocol = 'ICMPv4' }
    Add-Regel 'Spielmodus erlaubt: Ping IPv6' @{ Protocol = 'ICMPv6'; IcmpType = '128' }
    Add-Regel 'Spielmodus erlaubt: DNS verschlüsselt' @{ Program = "$env:WINDIR\System32\svchost.exe"; Service = 'Dnscache'; Protocol = 'TCP'; RemotePort = @('443', '853') }
    Add-Regel 'Spielmodus erlaubt: Internetprüfung' @{ Program = "$env:WINDIR\System32\svchost.exe"; Service = 'NlaSvc' }
    # Home network (router page, adb over Wi-Fi) for every program
    Add-Regel 'Spielmodus erlaubt: Heimnetz' @{ RemoteAddress = 'LocalSubnet' }
    $script:grundFertig = $true
}

function Add-SpielErlaubnis([int]$appId) {
    # Allows every exe of the game folder. Cached per build id in spiele.json, so the (huge) folder is
    # scanned only after a game update. Returns the number of known game exes (0 = do not block).
    if ($appId -le 0) { return 0 }
    $info = Get-SpielInfo $appId
    if (-not $info) { Write-Log "Spielordner für App $appId nicht gefunden"; return 0 }
    $cache = @{}
    if (Test-Path -LiteralPath $spieleCache) { try { (Get-Content -LiteralPath $spieleCache -Raw | ConvertFrom-Json).PSObject.Properties | ForEach-Object { $cache[$_.Name] = $_.Value } } catch { } }
    $e = $cache["$appId"]
    if ($e -and $e.Build -eq $info.Build -and @($e.Exe).Count -gt 0) {
        $exe = @($e.Exe)
    } else {
        $exe = Get-ExeIn @($info.Ordner)
        $cache["$appId"] = [pscustomobject]@{ Build = $info.Build; Exe = $exe }
        try { Save-JsonAtomar $cache $spieleCache } catch { }
        Write-Log "Spiel $appId (Build $($info.Build)): $($exe.Count) Programmdatei(en) im Spielordner"
    }
    Add-Erlaubt $exe
    foreach ($x in $exe) { [void]$script:spielNamen.Add([IO.Path]::GetFileNameWithoutExtension($x)) }
    return $exe.Count
}

function Enable-SpielFirewall([int]$appId) {
    # Returns $true when the firewall is on (restore file exists). Refuses when the game itself could
    # not be allowed - blocking then would only take the game offline.
    Add-GrundErlaubnis
    if ((Add-SpielErlaubnis $appId) -eq 0) { Write-Log "Spiel-Firewall NICHT eingeschaltet: keine Programmdatei für App $appId erlaubt"; return $false }
    if (-not (Test-Path -LiteralPath $fwSicher)) {
        # Existing outbound allow rules (Claude Desktop, Teams, Store apps, telemetry, backup ...) would
        # still pass under "block by default": switch them off for the game, keep Core Networking (DNS,
        # DHCP). Everything is saved BEFORE it changes, so a crash can always restore it.
        $fremd = @(Get-NetFirewallRule -Direction Outbound -Enabled True -Action Allow -ErrorAction SilentlyContinue |
            Where-Object { $_.Group -ne $fwErlaubt -and $_.Group -ne '@FirewallAPI.dll,-25000' -and $_.DisplayGroup -notmatch '^(Kernnetzwerk|Core Networking)$' })
        $wlan = Get-WlanName
        $vorher = @{ Deaktiviert = @($fremd | ForEach-Object Name); WlanAuto = $wlan }
        foreach ($pr in Get-NetFirewallProfile) { $vorher[$pr.Name] = "$($pr.DefaultOutboundAction)" }
        try { Save-JsonAtomar $vorher $fwSicher }
        catch { Write-Log "firewall.json nicht schreibbar, Spiel-Firewall bleibt aus: $($_.Exception.Message)"; return $false }
        if ($fremd.Count -gt 0) {
            try { $fremd | Disable-NetFirewallRule -ErrorAction Stop; Write-Log "Firewall: $($fremd.Count) fremde ausgehende Freigaben vorübergehend aus" }
            catch { Write-Log "Fremde Freigaben nicht abschaltbar: $($_.Exception.Message)" }
        }
        # Wi-Fi background scans cause the known AX201 ping spikes (~every 10 min, almanac B3).
        # The connection stays; only scanning/roaming pause until the game ends.
        if ($wlan) { netsh wlan set autoconfig enabled=no interface="$wlan" *> $null; Write-Log "WLAN-Hintergrundsuche aus ($wlan)" }
    }
    try {
        Set-NetFirewallProfile -All -DefaultOutboundAction Block -ErrorAction Stop
        # Programs that are now blocked keep connections opened before the block - cut them, so they
        # cannot keep downloading. Allowed programs (game, Steam, CLIs ...) and system processes stay.
        $pfade = @{}
        # svchost is cut too (Windows Update/BITS/telemetry), except the DNS and NCSI hosts
        $schutz = @(Get-CimInstance Win32_Service -Filter "Name='Dnscache' OR Name='NlaSvc'" -ErrorAction SilentlyContinue | ForEach-Object ProcessId)
        $weg = Close-Verbindungen {
            param($c)
            $id = $c.OwningProcess
            if ($id -le 4 -or $schutz -contains $id) { return $false }
            if (-not $pfade.ContainsKey($id)) { $pfade[$id] = try { (Get-Process -Id $id -ErrorAction Stop).Path } catch { $null } }
            $p = $pfade[$id]
            # a self-updated CLI keeps running as "<name>.old.<n>" - still the allowed program
            $basis = if ($p) { $p -replace '\.old\.[\d.]+$', '' } else { $null }
            return ($p -and -not $script:erlaubtSet.Contains($p) -and -not $script:erlaubtSet.Contains($basis) -and $p -notmatch '\\Windows\\System32\\(lsass|services)\.exe$')
        }
        Write-Log "Spiel-Firewall AN (App $appId), $weg offene Verbindung(en) gesperrter Programme getrennt"
    }
    catch { Write-Log "Spiel-Firewall nicht setzbar: $($_.Exception.Message)" }
    return $true   # restore file exists -> the off path undoes whatever was changed
}

function Disable-SpielFirewall {
    # $true when restored; on failure firewall.json stays and the caller retries (with backoff).
    $r = Restore-SpielFirewall $fwSicher
    if ($r.Meldung) { Write-Log "Rückstellen: $($r.Meldung)" }
    if ($r.Ok) { Write-Log 'Spiel-Firewall AUS: alle Programme haben wieder Internet, WLAN-Suche wieder an' }
    return $r.Ok
}

# ---------- Start ----------
Write-Log "Wächter gestartet (PID $PID)"
Save-Starttypen
$gesperrt = $false
$script:codLaeuft = $false
$script:spielNamen = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
$script:driveGeprueft = [datetime]::MinValue
$zuletztGesehen = [datetime]::MinValue
$laeuftStart = Get-Schnappschuss
$steamLief = $laeuftStart -contains 'steam'
$restMeldung = ''
$fwAn = $false
$fwAppId = -1
$spielZuletzt = [datetime]::MinValue
$fwNaechsterVersuch = [datetime]::MinValue

# Reconcile after a crash/reboot during a lock: InstallService must not stay disabled. Never start
# Gaming Services from here (a restart mid-game would start their shader download).
$script:gamingWarAktiv = $false
if ((Get-Service $dienstGesperrt -ErrorAction SilentlyContinue).StartType -eq 'Disabled') {
    Write-Log 'Altlast: InstallService war noch gesperrt, stelle zurück'
    Disable-Sperre
}
$script:gamingWarAktiv = $true
# Leftover game firewall: keep it if a game is running right now (watcher restarted mid-game),
# otherwise restore. A failed restore is retried by the loop ($fwAn stays true).
if (Test-Path -LiteralPath $fwSicher) {
    $idStart = Get-SpielAppId
    if ($steamLief -and $idStart -gt 0 -and $keinSpiel -notcontains $idStart) {
        Write-Log "Spiel läuft noch (App $idStart): Spiel-Firewall bleibt an"
        Add-GrundErlaubnis   # a new version may allow more programs - apply them right away
        [void](Add-SpielErlaubnis $idStart); $fwAppId = $idStart; $spielZuletzt = Get-Date
    } else {
        Write-Log 'Altlast: Spiel-Firewall war noch an, stelle zurück'
        [void](Disable-SpielFirewall)
    }
    $fwAn = Test-Path -LiteralPath $fwSicher
}
Disable-DriveSperre

while ($true) {
    $aus    = Test-Path $flagAus
    $laeuft = Get-Schnappschuss
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

    # Game firewall FIRST, so releasing it never waits behind service work.
    # A game runs if Steam runs and RunningAppID is a game - or, once the firewall is on, while any exe
    # of that game still runs (a Steam tool started later overwrites RunningAppID).
    $appId = Get-SpielAppId
    $istSpielId = ($appId -gt 0) -and ($keinSpiel -notcontains $appId)
    $spielProz = $fwAn -and @($script:spielNamen | Where-Object { $script:prozNamen.Contains($_) }).Count -gt 0
    $spielJetzt = $steamJetzt -and ($istSpielId -or $spielProz)
    if ($spielJetzt) { $spielZuletzt = Get-Date }
    $fwSoll = (-not $aus) -and ($spielJetzt -or ($fwAn -and ((Get-Date) - $spielZuletzt).TotalSeconds -lt $entprellSek))
    if ($fwSoll -and -not $fwAn) {
        # No status window here: a top-most window could disturb a fullscreen game.
        if ((Get-Date) -ge $fwNaechsterVersuch) {
            if (Enable-SpielFirewall $appId) { $fwAn = $true; $fwAppId = $appId }
            else { $fwNaechsterVersuch = (Get-Date).AddSeconds(60) }
        }
    } elseif ($fwAn -and $fwSoll -and $istSpielId -and $appId -ne $fwAppId) {
        [void](Add-SpielErlaubnis $appId); $fwAppId = $appId
    } elseif ($fwAn -and -not $fwSoll) {
        if ((Get-Date) -ge $fwNaechsterVersuch) {
            if (Disable-SpielFirewall) {
                $fwAn = $false; $fwAppId = -1; $script:spielNamen.Clear(); $fwNaechsterVersuch = [datetime]::MinValue
                if (-not $aus) { Show-Meldung 'Spiel beendet: Alle Programme haben wieder Internet.' }
            } else { $fwNaechsterVersuch = (Get-Date).AddSeconds(30) }
        }
    }

    $soll = (-not $aus) -and (($laeuft.Count -gt 0) -or ($gesperrt -and ((Get-Date) - $zuletztGesehen).TotalSeconds -lt $entprellSek))
    if ($soll -and -not $gesperrt) {
        Write-Log "Sperre AN (Auslöser: $($laeuft -join ', '))"
        Enable-Sperre; $gesperrt = $true
    } elseif (-not $soll -and $gesperrt) {
        Write-Log ('Sperre AUS (' + $(if ($aus) { 'von Hand ausgeschaltet' } else { 'kein Spiel/Steam mehr' }) + ')')
        Disable-Sperre; $gesperrt = $false; $restMeldung = ''
        if (-not $aus) { Show-Meldung 'Spielmodus pausiert (Steam/Spiel beendet). Google Drive, Store und Xbox laufen wieder normal.' }
    }

    if ($gesperrt) {
        $rest = @($stopProz | Where-Object { $script:prozNamen.Contains($_) })
        $rest += @(Get-LaufendeDienste | ForEach-Object { $_.Name })
        if ($rest.Count -gt 0) {
            Stop-Ziele
            $txt = ($rest | Sort-Object -Unique) -join ', '
            if ($txt -ne $restMeldung) { Write-Log "Wieder aufgetaucht, beendet: $txt"; $restMeldung = $txt }
        }
        Update-DriveSperre
    }

    Start-Sleep -Seconds 4
}
