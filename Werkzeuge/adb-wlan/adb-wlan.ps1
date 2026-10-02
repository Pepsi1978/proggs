# Verbindet das Android-Handy per WLAN mit adb – mit Kabel und ohne.
# Reihenfolge: schon verbunden und antwortet? -> mDNS (findet das Handy in jedem Netz, auch nach IP-Wechsel)
# -> zuletzt bekannte IP -> Kabel (schaltet WLAN-Debugging + Port 5555 ein).
# Ohne Kabel wird nach einem Handy-Neustart Port 5555 über "Debugging über WLAN" (TLS) wiederhergestellt.
# Gebunden an die Seriennummer des Handys (gemerkt bei Kabelverbindung): fremde Geräte im selben
# Netz werden nie angesprochen.
# -Leise: für den Wachhund (Aufgabenplanung), schreibt nur ins Log und startet den adb-Server nie neu.
# Einrichtung/Hintergrund: best-practices/android/adb-wlan-debugging.md
param([switch]$Leise)

$Port = 5555
$State = Join-Path $env:USERPROFILE ".adb-wlan-last-ip"
$SerialDatei = Join-Path $env:USERPROFILE ".adb-wlan-serial"
$LogDir = Join-Path $env:LOCALAPPDATA "adb-wlan"
$Log = Join-Path $LogDir "adb-wlan.log"
New-Item -ItemType Directory -Force $LogDir | Out-Null
# Immer dieselbe adb wie Gradle (SDK): verschiedene adb-Versionen beenden sich gegenseitig
# den Server, und dabei gehen alle WLAN-Verbindungen verloren.
$Adb = @($env:ADB, (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"),
    (Get-Command adb -ErrorAction SilentlyContinue).Source) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
if (-not $Adb) { Write-Host "FEHLER: adb nicht gefunden (weder `$env:ADB noch Android-SDK noch PATH)."; exit 1 }

function Melde($text) {
    $zeile = "{0} {1}" -f (Get-Date -Format "dd.MM.yyyy HH:mm:ss"), $text
    try { Add-Content -Path $Log -Value $zeile -Encoding utf8 } catch { }
    if (-not $Leise) { Write-Host $text }
}

# adb-Aufruf mit Zeitlimit: eine tote WLAN-Verbindung darf das Skript nicht aufhängen
function AdbZeit([string]$argumente, [int]$ms = 8000) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo $Adb, $argumente
    $psi.RedirectStandardOutput = $true; $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false; $psi.CreateNoWindow = $true
    $p = [System.Diagnostics.Process]::Start($psi)
    $out = $p.StandardOutput.ReadToEndAsync(); $err = $p.StandardError.ReadToEndAsync()
    if (-not $p.WaitForExit($ms)) { try { $p.Kill() } catch { }; return $null }
    return ($out.Result + " " + $err.Result).Trim()
}

function Geraete {
    # Liefert Objekte { Serial; Zustand } aus "adb devices"
    & $Adb devices 2>$null | Select-Object -Skip 1 | Where-Object { $_ -match "^\S+\s+\S+" } | ForEach-Object {
        $t = $_ -split "\s+"; [pscustomobject]@{ Serial = $t[0]; Zustand = $t[1] }
    }
}

function Ist-Netz($serial) { $serial -match ":\d+$" -or $serial -match "_adb-tls-connect" }
function Usb-Geraet { Geraete | Where-Object { $_.Zustand -eq "device" -and -not (Ist-Netz $_.Serial) -and $_.Serial -notmatch "^emulator-" } | Select-Object -First 1 }
function Bekannte-Serial { if (Test-Path $SerialDatei) { (Get-Content $SerialDatei -Raw).Trim() } }

# $true, wenn das Gerät antwortet UND (falls bekannt) unser Handy ist
function Ist-Unser-Handy($serial) {
    $id = AdbZeit "-s $serial shell getprop ro.serialno"
    if (-not $id) { return $false }
    $soll = Bekannte-Serial
    return (-not $soll -or $id -eq $soll)
}

# Nur Netz-Verbindungen, die antworten und zu unserem Handy gehören
function Netz-Verbunden {
    Geraete | Where-Object { $_.Zustand -eq "device" -and (Ist-Netz $_.Serial) } | Where-Object { Ist-Unser-Handy $_.Serial }
}

function Verbinde($ziel) {
    $out = AdbZeit "connect $ziel" 15000
    Melde "adb connect ${ziel}: $out"
    if ($out -notmatch "connected to") { return $false }
    # "connected" heißt noch nicht freigegeben: auf Autorisierung warten und Identität prüfen
    for ($i = 0; $i -lt 5; $i++) {
        $z = (Geraete | Where-Object { $_.Serial -eq $ziel }).Zustand
        if ($z -eq "device") {
            if (Ist-Unser-Handy $ziel) { return $true }
            Melde "$ziel ist nicht das gemerkte Handy – getrennt"
            & $Adb disconnect $ziel 2>&1 | Out-Null; return $false
        }
        if ($z -eq "unauthorized" -and $i -eq 0) { Melde "HINWEIS: Auf dem Handy 'Debugging zulassen' bestätigen (Haken 'Immer zulassen')." }
        Start-Sleep 2
    }
    return $false
}

# Handy-Seite dauerhaft vorbereiten (über Kabel ODER WLAN, höchstens stündlich):
# Berechtigung für UpdateStation (schaltet WLAN-Debugging nach Neustart ein) und Ausnahme von der
# Doze-Drosselung, damit ihre Aufgabe nach dem Boot nicht verzögert wird. Idempotent.
$PflegeDatei = Join-Path $LogDir "letzte-pflege"
function Pflege($serial, [switch]$Sofort) {
    if (-not $Sofort -and (Test-Path $PflegeDatei) -and ((Get-Date) - (Get-Item $PflegeDatei).LastWriteTime).TotalMinutes -lt 60) { return }
    $pk = "de.frank.updatestation"
    if (-not ((AdbZeit "-s $serial shell pm path $pk") -match "package:")) { return }
    AdbZeit "-s $serial shell pm grant $pk android.permission.WRITE_SECURE_SETTINGS" | Out-Null
    if (-not ((AdbZeit "-s $serial shell cmd deviceidle whitelist") -match $pk)) {
        AdbZeit "-s $serial shell cmd deviceidle whitelist +$pk" | Out-Null
        Melde "UpdateStation von Doze-Drosselung ausgenommen"
    }
    Hotspot-Einrichten $serial
    Set-Content -Path $PflegeDatei -Value (Get-Date -Format o) -Encoding ascii
}

# Sucht unser Handy per mDNS (5555 zuerst, sonst TLS und dann 5555 nachziehen)
function Mdns-Verbinden {
    $soll = Bekannte-Serial
    $dienste = @()
    for ($i = 0; $i -lt 3 -and -not $dienste; $i++) {
        $dienste = @(& $Adb mdns services 2>$null | Where-Object {
            $_ -match "\t_adb(-tls-connect)?\._tcp\.?\t" -and (-not $soll -or $_ -match "^adb-$([regex]::Escape($soll))-")
        })
        if (-not $dienste) { Start-Sleep 2 }
    }
    $legacy = $dienste | Where-Object { $_ -match "\t_adb\._tcp\.?\t(\S+)" } | ForEach-Object { $Matches[1] }
    $tlsZiele = $dienste | Where-Object { $_ -match "\t_adb-tls-connect\._tcp\.?\t(\S+)" } | ForEach-Object { $Matches[1] }
    foreach ($z in $legacy) { if (Verbinde $z) { Merke-Ip ($z -split ":")[0]; return $true } }
    foreach ($z in $tlsZiele) {
        if (Verbinde $z) {
            $ip = ($z -split ":")[0]; Merke-Ip $ip
            Stabilisiere $z $ip | Out-Null
            return $true
        }
    }
    return $false
}

# --- Direktverbindung: Windows-Hotspot des PCs, das Handy verbindet sich als Client ---
# Braucht kein externes WLAN. Das Handy kennt den Hotspot (in Pflege gespeichert, je PC ein eigener)
# und tritt ihm von selbst bei, wenn es in keinem anderen WLAN ist. Nur Windows PowerShell 5.1 (WinRT).
$HotspotMarke = Join-Path $LogDir "hotspot-von-uns"          # nur einen selbst gestarteten Hotspot abschalten
$HotspotEingerichtet = Join-Path $LogDir "hotspot-auf-handy"  # Prüfsumme von SSID+Passwort auf dem Handy
$script:Tm = $null
function Hotspot-Manager {
    if ($script:Tm) { return $script:Tm }
    try {
        Add-Type -AssemblyName System.Runtime.WindowsRuntime
        $null = [Windows.Networking.Connectivity.NetworkInformation, Windows.Networking.Connectivity, ContentType = WindowsRuntime]
        $null = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager, Windows.Networking.NetworkOperators, ContentType = WindowsRuntime]
        $p = [Windows.Networking.Connectivity.NetworkInformation]::GetInternetConnectionProfile()
        if (-not $p) { $p = [Windows.Networking.Connectivity.NetworkInformation]::GetConnectionProfiles() | Select-Object -First 1 }
        if (-not $p) { Melde "Direktverbindung: kein Netzwerkprofil zum Teilen"; return $null }
        $script:Tm = [Windows.Networking.NetworkOperators.NetworkOperatorTetheringManager]::CreateFromConnectionProfile($p)
        return $script:Tm
    } catch { Melde "Direktverbindung nicht verfügbar: $($_.Exception.Message)"; return $null }
}

function Warte-WinRT($op) {
    $m = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
        $_.Name -eq "AsTask" -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' } | Select-Object -First 1
    $t = $m.MakeGenericMethod([Windows.Networking.NetworkOperators.NetworkOperatorTetheringOperationResult]).Invoke($null, @($op))
    if (-not $t.Wait(30000)) { return $null }
    return $t.Result
}

function Hotspot-An {
    $tm = Hotspot-Manager; if (-not $tm) { return $false }
    if ("$($tm.TetheringOperationalState)" -eq "On") { return $true }
    try { $r = Warte-WinRT $tm.StartTetheringAsync() } catch { $r = $null }
    if ($r -and "$($r.Status)" -eq "Success") {
        Set-Content -Path $HotspotMarke -Value (Get-Date -Format o) -Encoding ascii
        Melde "Direktverbindung: PC-Hotspot '$($tm.GetCurrentAccessPointConfiguration().Ssid)' eingeschaltet"
        return $true
    }
    Melde "Direktverbindung: Hotspot ließ sich nicht einschalten ($($r.Status) $($r.AdditionalErrorMessage))"
    return $false
}

function Hotspot-Aus {
    if (-not (Test-Path $HotspotMarke)) { return }
    $tm = Hotspot-Manager
    if ($tm -and "$($tm.TetheringOperationalState)" -eq "On") {
        try { Warte-WinRT $tm.StopTetheringAsync() | Out-Null } catch { }
        Melde "Direktverbindung: PC-Hotspot wieder ausgeschaltet"
    }
    Remove-Item $HotspotMarke -ErrorAction SilentlyContinue
}

# IPv4-Adressen der Geräte im PC-Hotspot
function Hotspot-Clients {
    $ips = @()
    $tm = Hotspot-Manager
    if ($tm) {
        try { $ips = @($tm.GetTetheringClients() | ForEach-Object { $_.HostNames } |
            Where-Object { "$($_.Type)" -eq "Ipv4" } | ForEach-Object { $_.CanonicalName }) } catch { }
    }
    if (-not $ips) {
        $ips = @(Get-NetNeighbor -AddressFamily IPv4 -ErrorAction SilentlyContinue | Where-Object {
            $_.IPAddress -like "192.168.137.*" -and $_.IPAddress -notmatch "\.(1|255)$" -and "$($_.State)" -notin "Unreachable", "Permanent" } |
            ForEach-Object { $_.IPAddress })
    }
    return $ips | Select-Object -Unique
}

# Speichert den PC-Hotspot auf dem Handy (erneut, sobald SSID oder Passwort sich ändern)
function Hotspot-Einrichten($serial) {
    $tm = Hotspot-Manager; if (-not $tm) { return }
    $c = $tm.GetCurrentAccessPointConfiguration()
    if (-not $c.Ssid -or -not $c.Passphrase) { return }
    $sha = [System.Security.Cryptography.SHA256]::Create()
    $summe = [BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes("$serial|$($c.Ssid)|$($c.Passphrase)")))
    if ((Test-Path $HotspotEingerichtet) -and (Get-Content $HotspotEingerichtet -Raw).Trim() -eq $summe) { return }
    $out = AdbZeit "-s $serial shell ""cmd wifi add-network '$($c.Ssid)' wpa2 '$($c.Passphrase)'"""
    # add-network meldet nichts zurück: in der Liste der gespeicherten Netze nachsehen
    if ((AdbZeit "-s $serial shell cmd wifi list-networks") -match "\s$([regex]::Escape($c.Ssid))\s+wpa2") {
        Set-Content -Path $HotspotEingerichtet -Value $summe -Encoding ascii
        Melde "Direktverbindung: PC-Hotspot '$($c.Ssid)' auf dem Handy gespeichert"
    } else { Melde "Direktverbindung: Hotspot auf dem Handy speichern fehlgeschlagen: $out" }
}

function Direktverbindung {
    # Nur auf Zuruf (Installieren, Logcat …), nie aus dem Wachhund: der PC soll nicht unnötig funken
    if ($Leise) { return $false }
    if (-not (Hotspot-An)) { return $false }
    $c = $script:Tm.GetCurrentAccessPointConfiguration()

    # Kabel dran: Handy jetzt in den Hotspot holen. Hängt es in einem offenen WLAN, das Geräte
    # abschottet (öffentliches WLAN), dort Auto-Verbinden ausschalten – sonst landet es beim
    # nächsten Mal wieder dort statt im PC-Hotspot.
    $usb = Usb-Geraet
    if ($usb) {
        $s = $usb.Serial
        Hotspot-Einrichten $s
        $status = AdbZeit "-s $s shell cmd wifi status"
        if ($status -match 'connected to "([^"]+)"' -and $Matches[1] -ne $c.Ssid) {
            $alt = $Matches[1]
            $netze = AdbZeit "-s $s shell cmd wifi list-networks"
            if ($netze -match "(?m)^\d+\s+$([regex]::Escape($alt))\s+open\s*$") {
                AdbZeit "-s $s shell ""cmd wifi add-network '$alt' open -d""" | Out-Null
                Melde "Direktverbindung: Im WLAN '$alt' ist das Handy nicht erreichbar – Auto-Verbinden dort ausgeschaltet"
            }
        }
        $out = AdbZeit "-s $s shell ""cmd wifi connect-network '$($c.Ssid)' wpa2 '$($c.Passphrase)'""" 20000
        Melde "Direktverbindung: Handy per Kabel in den PC-Hotspot geschickt: $out"
    }

    $bis = (Get-Date).AddSeconds(90)
    $neuGestartet = $false
    while ((Get-Date) -lt $bis) {
        $clients = @(Hotspot-Clients)
        foreach ($ip in $clients) {
            if (Verbinde "${ip}:$Port") { Merke-Ip $ip; Melde "Direktverbindung steht: ${ip}:$Port"; return $true }
        }
        # Nach einem Handy-Neustart ist nur "Debugging über WLAN" (TLS) da: per mDNS suchen
        if ($clients -and (Mdns-Verbinden)) { Melde "Direktverbindung steht (über WLAN-Debugging)"; return $true }
        # mDNS des adb-Servers sieht nur Netze, die beim Serverstart schon da waren (getestet) –
        # den Hotspot also erst nach einem Neustart. Nur neu starten, wenn kein Gerät verbunden ist.
        if ($clients -and -not $neuGestartet -and -not @(Geraete)) {
            Melde "Direktverbindung: starte adb-Server neu, damit mDNS den Hotspot sieht"
            & $Adb kill-server 2>&1 | Out-Null; & $Adb start-server 2>&1 | Out-Null
            $neuGestartet = $true; Start-Sleep 5; continue
        }
        Start-Sleep 5
    }
    Melde "Direktverbindung: Handy ist dem PC-Hotspot nicht beigetreten"
    Hotspot-Aus
    return $false
}

function Merke-Ip($ip) { if ($ip) { Set-Content -Path $State -Value $ip -Encoding ascii } }

function Stabilisiere($tlsSerial, $ip) {
    # Nur TLS erreichbar (z. B. nach Handy-Neustart): Port 5555 ohne Kabel zurückholen.
    Melde "Nur WLAN-Debugging (TLS) erreichbar – stelle Port $Port wieder her"
    AdbZeit "-s $tlsSerial tcpip $Port" | Out-Null
    & $Adb disconnect $tlsSerial 2>&1 | Out-Null   # TLS-Port wechselt beim adbd-Neustart ohnehin
    for ($i = 0; $i -lt 6; $i++) {
        Start-Sleep 2
        if (Verbinde "${ip}:$Port") { return $true }
    }
    Melde "Port $Port kam nicht zurück – nächster Lauf findet WLAN-Debugging wieder per mDNS"
    return $false
}

# Parallele Aufrufe (Wachhund + Sitzungen) nicht gegeneinander laufen lassen
$mutex = New-Object System.Threading.Mutex($false, "Global\adb-wlan")
try { if (-not $mutex.WaitOne(90000)) { Melde "Anderer adb-wlan-Lauf aktiv – übersprungen"; exit 0 } }
catch [System.Threading.AbandonedMutexException] { }

try {
    & $Adb start-server 2>&1 | Out-Null

    # Neuer Rechner: Wachhund beim ersten Aufruf selbst einrichten (Skript liegt im Repo, gilt für jeden PC)
    if (-not $Leise -and -not (Get-ScheduledTask -TaskName "adb-wlan-wachhund" -ErrorAction SilentlyContinue)) {
        & (Join-Path $PSScriptRoot "wachhund-einrichten.ps1")
    }

    # Tote oder hängende Netz-Einträge räumen (offline/unauthorized oder "device", das nicht antwortet).
    # Antwortende fremde Geräte bleiben unberührt.
    Geraete | Where-Object { Ist-Netz $_.Serial } | ForEach-Object {
        $tot = $_.Zustand -ne "device" -or -not (AdbZeit "-s $($_.Serial) shell echo ok")
        if ($tot) {
            & $Adb disconnect $_.Serial 2>&1 | Out-Null
            Melde "Toten Eintrag $($_.Serial) ($($_.Zustand)) getrennt"
        }
    }

    # Kabel dran: Handy dauerhaft vorbereiten (idempotent, startet adbd nicht neu)
    $usb = Usb-Geraet
    if ($usb) {
        $s = $usb.Serial
        $id = AdbZeit "-s $s shell getprop ro.serialno"
        if ($id) { Set-Content -Path $SerialDatei -Value $id -Encoding ascii }
        Pflege $s -Sofort
        if ((AdbZeit "-s $s shell settings get global adb_wifi_enabled") -ne "1") {
            AdbZeit "-s $s shell settings put global adb_wifi_enabled 1" | Out-Null
        }
    }
    $soll = Bekannte-Serial

    for ($runde = 1; $runde -le 2; $runde++) {
        # 1. Schon per WLAN verbunden und antwortet?
        $netz = @(Netz-Verbunden)
        if ($netz) {
            if ($netz | Where-Object { $_.Serial -match "^\d+\.\d+\.\d+\.\d+:$Port$" }) { break }
            # Nur TLS verbunden -> 5555 nachziehen, damit die Serial stabil IP:5555 bleibt
            $ip = ($netz[0].Serial -split ":")[0]
            if ($ip -match "^\d+\.\d+\.\d+\.\d+$") { Merke-Ip $ip; Stabilisiere $netz[0].Serial $ip | Out-Null }
            break
        }

        # 2. mDNS: findet das Handy mit aktueller IP in jedem Netz (nur Dienste unseres Handys)
        if (Mdns-Verbinden) { break }

        # 3. Zuletzt bekannte IP
        $letzte = if (Test-Path $State) { (Get-Content $State -Raw).Trim() }
        if ($letzte -and (Verbinde "${letzte}:$Port")) { break }

        # 4. Kabel: WLAN-IP holen, Port 5555 einschalten
        $usb = Usb-Geraet
        if ($usb) {
            $s = $usb.Serial
            AdbZeit "-s $s shell svc wifi enable" | Out-Null
            $ip = $null
            for ($i = 0; $i -lt 15 -and -not $ip; $i++) {
                $line = (AdbZeit "-s $s shell ip -f inet addr show wlan0") | Select-String "inet (\d+\.\d+\.\d+\.\d+)"
                if ($line) { $ip = $line.Matches[0].Groups[1].Value } else { Start-Sleep 2 }
            }
            if (-not $ip) { Melde "FEHLER: Handy hat keine WLAN-IP (WLAN am Handy aus oder nicht verbunden)."; exit 1 }
            Merke-Ip $ip
            AdbZeit "-s $s tcpip $Port" | Out-Null
            Start-Sleep 3
            if (Verbinde "${ip}:$Port") { break }
        }

        # 5. Einmal adb-Server neu starten (hilft bei "No route to host" / verklemmtem Server).
        #    Nie im Wachhund: ein Neustart würde andere Sitzungen und Emulatoren trennen.
        if ($runde -eq 1 -and -not $Leise) {
            Melde "Keine Verbindung – starte adb-Server neu und versuche es erneut"
            & $Adb kill-server 2>&1 | Out-Null; & $Adb start-server 2>&1 | Out-Null; Start-Sleep 3
        } else { break }
    }

    # 6. Direktverbindung über den PC-Hotspot: Handy in keinem WLAN, in einem anderen Netz
    #    oder in einem Netz, das Geräte voneinander abschottet (öffentliches WLAN).
    if (-not @(Netz-Verbunden)) { Direktverbindung | Out-Null }

    $fertig = @(Netz-Verbunden)
    if ($fertig) {
        Pflege $fertig[0].Serial
        if (-not $Leise) { & $Adb devices -l }
        exit 0
    }
    Melde ("FEHLER: Handy per WLAN nicht gefunden. Prüfen: Handy im selben WLAN? 'Debugging über WLAN' an? " +
        "Sonst einmal Kabel anstecken und dieses Skript erneut ausführen.")
    if (-not $Leise) { & $Adb devices -l }
    exit 1
}
finally {
    # Log klein halten
    if ((Test-Path $Log) -and (Get-Item $Log).Length -gt 512KB) {
        Get-Content $Log -Tail 2000 | Set-Content "$Log.tmp" -Encoding utf8; Move-Item -Force "$Log.tmp" $Log
    }
    try { $mutex.ReleaseMutex() } catch { }
}
