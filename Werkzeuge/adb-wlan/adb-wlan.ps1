# Verbindet das Android-Handy per WLAN mit adb – mit Kabel und ohne.
# Reihenfolge: schon verbunden? -> mDNS (findet das Handy in jedem Netz, auch nach IP-Wechsel)
# -> zuletzt bekannte IP -> Kabel (schaltet WLAN-Debugging + Port 5555 ein).
# Ohne Kabel wird nach einem Handy-Neustart Port 5555 über "Debugging über WLAN" (TLS) wiederhergestellt.
# -Leise: für den Wachhund (Aufgabenplanung), schreibt nur ins Log und startet den adb-Server nie neu.
# Einrichtung/Hintergrund: best-practices/android/adb-wlan-debugging.md
param([switch]$Leise)

$Port = 5555
$State = Join-Path $env:USERPROFILE ".adb-wlan-last-ip"
$LogDir = Join-Path $env:LOCALAPPDATA "adb-wlan"
$Log = Join-Path $LogDir "adb-wlan.log"
New-Item -ItemType Directory -Force $LogDir | Out-Null

function Melde($text) {
    $zeile = "{0} {1}" -f (Get-Date -Format "dd.MM.yyyy HH:mm:ss"), $text
    Add-Content -Path $Log -Value $zeile -Encoding utf8
    if (-not $Leise) { Write-Host $text }
}

function Geraete {
    # Liefert Objekte { Serial; Zustand } aus "adb devices"
    adb devices 2>$null | Select-Object -Skip 1 | Where-Object { $_ -match "^\S+\s+\S+" } | ForEach-Object {
        $t = $_ -split "\s+"; [pscustomobject]@{ Serial = $t[0]; Zustand = $t[1] }
    }
}

function Netz-Verbunden { Geraete | Where-Object { $_.Zustand -eq "device" -and ($_.Serial -match ":\d+$" -or $_.Serial -match "_adb-tls-connect") } }
function Usb-Geraet { Geraete | Where-Object { $_.Zustand -eq "device" -and $_.Serial -notmatch ":\d+$" -and $_.Serial -notmatch "^emulator-|_adb-tls" } | Select-Object -First 1 }

function Verbinde($ziel) {
    $out = (adb connect $ziel 2>&1) -join " "
    Melde "adb connect ${ziel}: $out"
    return ($out -match "connected to|already connected")
}

function Merke-Ip($ip) { if ($ip) { Set-Content -Path $State -Value $ip -Encoding ascii } }

function Stabilisiere($tlsSerial, $ip) {
    # Nur TLS erreichbar (z. B. nach Handy-Neustart): Port 5555 ohne Kabel zurückholen.
    Melde "Nur WLAN-Debugging (TLS) erreichbar – stelle Port $Port wieder her"
    adb -s $tlsSerial tcpip $Port 2>&1 | Out-Null
    Start-Sleep 4
    adb disconnect $tlsSerial 2>&1 | Out-Null
    return (Verbinde "${ip}:$Port")
}

# Parallele Aufrufe (Wachhund + Sitzungen) nicht gegeneinander laufen lassen
$mutex = New-Object System.Threading.Mutex($false, "Global\adb-wlan")
try { if (-not $mutex.WaitOne(90000)) { Melde "Anderer adb-wlan-Lauf aktiv – übersprungen"; exit 0 } }
catch [System.Threading.AbandonedMutexException] { }

try {
    adb start-server 2>&1 | Out-Null

    # Hängende Netz-Einträge (offline/unauthorized) räumen, sonst blockieren sie neue Verbindungen
    Geraete | Where-Object { ($_.Serial -match ":\d+$" -or $_.Serial -match "_adb-tls-connect") -and $_.Zustand -ne "device" } | ForEach-Object {
        adb disconnect $_.Serial 2>&1 | Out-Null
        Melde "Hängenden Eintrag $($_.Serial) ($($_.Zustand)) getrennt"
    }

    # Kabel dran: Handy dauerhaft vorbereiten (idempotent, startet adbd nicht neu)
    $usb = Usb-Geraet
    if ($usb) {
        $s = $usb.Serial
        adb -s $s shell settings put global adb_wifi_enabled 1 2>&1 | Out-Null
        adb -s $s shell pm grant de.frank.updatestation android.permission.WRITE_SECURE_SETTINGS 2>&1 | Out-Null
    }

    for ($runde = 1; $runde -le 2; $runde++) {
        # 1. Schon per WLAN verbunden?
        $netz = Netz-Verbunden
        if ($netz) {
            if ($netz | Where-Object { $_.Serial -match "^\d+\.\d+\.\d+\.\d+:$Port$" }) { break }
            # Nur TLS verbunden -> 5555 nachziehen, damit die Serial stabil IP:5555 bleibt
            $tls = $netz | Select-Object -First 1
            $ip = ($tls.Serial -split ":")[0]
            if ($ip -match "^\d+\.\d+\.\d+\.\d+$") { Merke-Ip $ip; Stabilisiere $tls.Serial $ip | Out-Null }
            break
        }

        # 2. mDNS: findet das Handy mit aktueller IP in jedem Netz
        $dienste = @()
        for ($i = 0; $i -lt 3 -and -not $dienste; $i++) {
            $dienste = @(adb mdns services 2>$null | Where-Object { $_ -match "\t_adb(-tls-connect)?\._tcp\.?\t" })
            if (-not $dienste) { Start-Sleep 2 }
        }
        $legacy = $dienste | Where-Object { $_ -match "\t_adb\._tcp\.?\t(\S+)" } | ForEach-Object { $Matches[1] }
        $tlsZiele = $dienste | Where-Object { $_ -match "\t_adb-tls-connect\._tcp\.?\t(\S+)" } | ForEach-Object { $Matches[1] }
        $ok = $false
        foreach ($z in $legacy) { if (Verbinde $z) { Merke-Ip ($z -split ":")[0]; $ok = $true; break } }
        if (-not $ok) {
            foreach ($z in $tlsZiele) {
                if (Verbinde $z) {
                    $ip = ($z -split ":")[0]; Merke-Ip $ip
                    Start-Sleep 1
                    Stabilisiere $z $ip | Out-Null
                    $ok = $true; break
                }
            }
        }
        if ($ok) { break }

        # 3. Zuletzt bekannte IP
        $letzte = if (Test-Path $State) { (Get-Content $State -Raw).Trim() }
        if ($letzte -and (Verbinde "${letzte}:$Port")) { break }

        # 4. Kabel: WLAN-IP holen, Port 5555 einschalten
        $usb = Usb-Geraet
        if ($usb) {
            $s = $usb.Serial
            adb -s $s shell svc wifi enable 2>&1 | Out-Null
            $ip = $null
            for ($i = 0; $i -lt 15 -and -not $ip; $i++) {
                $line = adb -s $s shell ip -f inet addr show wlan0 2>$null | Select-String "inet (\d+\.\d+\.\d+\.\d+)"
                if ($line) { $ip = $line.Matches[0].Groups[1].Value } else { Start-Sleep 2 }
            }
            if (-not $ip) { Melde "FEHLER: Handy hat keine WLAN-IP (WLAN am Handy aus oder nicht verbunden)."; exit 1 }
            Merke-Ip $ip
            adb -s $s tcpip $Port 2>&1 | Out-Null
            Start-Sleep 3
            if (Verbinde "${ip}:$Port") { break }
        }

        # 5. Einmal adb-Server neu starten (hilft bei "No route to host" / verklemmtem Server).
        #    Nie im Wachhund: ein Neustart würde andere Sitzungen und Emulatoren trennen.
        if ($runde -eq 1 -and -not $Leise) {
            Melde "Keine Verbindung – starte adb-Server neu und versuche es erneut"
            adb kill-server 2>&1 | Out-Null; adb start-server 2>&1 | Out-Null; Start-Sleep 3
        } else { break }
    }

    if (Netz-Verbunden) {
        if (-not $Leise) { adb devices -l }
        exit 0
    }
    Melde ("FEHLER: Handy per WLAN nicht gefunden. Prüfen: Handy im selben WLAN? 'Debugging über WLAN' an? " +
        "Sonst einmal Kabel anstecken und dieses Skript erneut ausführen.")
    if (-not $Leise) { adb devices -l }
    exit 1
}
finally {
    # Log klein halten
    if ((Test-Path $Log) -and (Get-Item $Log).Length -gt 512KB) {
        Get-Content $Log -Tail 2000 | Set-Content "$Log.tmp" -Encoding utf8; Move-Item -Force "$Log.tmp" $Log
    }
    try { $mutex.ReleaseMutex() } catch { }
}
