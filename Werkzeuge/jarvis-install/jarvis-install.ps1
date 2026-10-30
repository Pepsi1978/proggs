# Installiert eine Jarvis-APK per adb (Tunnel, WLAN, notfalls Kabel), aber erst nach Rückfrage in Jarvis:
# Ein Overlay über jeder App bietet „Sofort“ oder „In 1 bis 5 Minuten“, damit eine laufende Aufnahme nicht abbricht.
# Ohne Antwort wird nach 30 Sekunden installiert. Die APK liegt schon vor der Frage in einer Installationssitzung am
# Handy, damit „Sofort“ wirklich sofort installiert: Nach der Antwort fehlt nur noch das Abschließen der Sitzung.
# Jarvis verlangt seinen Pflicht-Teil (assets/freigabe-teil.apk in der APK): Nacktes adb install lehnt Android deshalb
# mit INSTALL_FAILED_MISSING_SPLIT ab, nur dieses Skript und UpdateStation installieren ihn mit und stellen die Frage.
# Aufruf: jarvis-install.ps1 <apk> [-Geraet <gerät>]
# Kann das Overlay nicht erscheinen, wird nicht installiert.
# Letzte Zeile JARVIS_INSTALL_STATUS: installiert oder fehler.
param([Parameter(Mandatory)][string]$Apk, [string]$Geraet = "")

$paket = "de.frank.jarvis"
$fenster = "$paket/.ui.InstallFrageDienst"
$antwort = "/sdcard/Android/data/$paket/files/install-antwort.txt"
$zwischenlager = "/data/local/tmp/jarvis-install.apk"
$teilImApk = "assets/freigabe-teil.apk"
$teilDatei = ""
# Frist des Overlays plus Reserve; sie zählt ab seiner Meldung „offen“, damit jeder Tipp im sichtbaren Fenster noch gilt.
$warteS = 33
$meldefristS = 20
$sitzung = ""
$abgeschlossen = $false
$serie = ""
$wlanSkript = Join-Path (Split-Path $PSScriptRoot) "adb-wlan\adb-wlan.ps1"

function Ende([string]$status, [string]$text = "") {
    if ($text) { "JARVIS_INSTALL_STATUS=$status ($text)" } else { "JARVIS_INSTALL_STATUS=$status" }
    if ($status -eq "fehler") { exit 1 }
    exit 0
}

if (-not (Test-Path $Apk)) { Ende fehler "APK nicht gefunden: $Apk" }
$adb = @($env:ADB, "$env:ANDROID_HOME\platform-tools\adb.exe", "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe", (Get-Command adb -ErrorAction SilentlyContinue).Source) |
    Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
if (-not $adb) { Ende fehler "adb nicht gefunden" }

# WLAN oder Tunnel (IP:Port) vor Kabel; ohne Verbindung einmal das WLAN-Werkzeug fragen.
function Waehle-Geraet {
    $bereit = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" } | ForEach-Object { ($_ -split "\s+")[0] }
    $netz = $bereit | Where-Object { $_ -match ":\d+$" } | Select-Object -First 1
    if ($netz) { return $netz }
    return $bereit | Select-Object -First 1
}
function Verbinde {
    if ($script:Geraet -and (& $adb -s $script:Geraet get-state 2>$null) -eq "device") { return $true }
    $script:Geraet = ""
    $script:Geraet = Waehle-Geraet
    if ($script:Geraet) { return $true }
    & pwsh -NoProfile -File $wlanSkript *> $null
    $script:Geraet = Waehle-Geraet
    return [bool]$script:Geraet
}
function Handy { (& $adb -s $script:Geraet shell @args 2>&1 | Out-String).Trim() }
function Installiere {
    if (-not (Verbinde)) { Ende fehler "Handy nicht erreichbar" }
    # Nach einem Verbindungsabriss kann die Adresse wechseln (WLAN, Tunnel); die Sitzung gilt aber nur auf diesem Handy.
    if ((Handy getprop ro.serialno) -ne $script:serie) { Ende fehler "anderes Gerät verbunden, Sitzung gehört zu $script:serie" }
    $ausgabe = Handy pm install-commit $script:sitzung
    $ausgabe
    if ($ausgabe -match "(?m)^Success") {
        $script:abgeschlossen = $true
        Ende installiert $script:Geraet
    }
    Ende fehler "Installation gescheitert: $ausgabe"
}

try {
    if (-not (Verbinde)) { Ende fehler "Handy nicht erreichbar" }
    $serie = Handy getprop ro.serialno

    "Lade die APK vorab aufs Handy über $Geraet ..."
    & $adb -s $Geraet push $Apk $zwischenlager *> $null
    if ($LASTEXITCODE -ne 0) { Ende fehler "APK ließ sich nicht aufs Handy laden" }
    $groesse = (Get-Item $Apk).Length
    # Ältere Jarvis-APKs haben den Teil noch nicht und brauchen ihn auch nicht.
    $teilGroesse = 0
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $Apk).Path)
    try {
        $eintrag = $zip.GetEntry($teilImApk)
        if ($eintrag) {
            $teilDatei = [System.IO.Path]::GetTempFileName()
            [System.IO.Compression.ZipFileExtensions]::ExtractToFile($eintrag, $teilDatei, $true)
            $teilGroesse = (Get-Item $teilDatei).Length
        }
    }
    finally { $zip.Dispose() }
    $ausgabe = Handy pm install-create -r -S ($groesse + $teilGroesse)
    if ($ausgabe -notmatch "\[(\d+)\]") { Ende fehler "Installationssitzung ließ sich nicht anlegen: $ausgabe" }
    $sitzung = $Matches[1]
    $ausgabe = Handy pm install-write -S $groesse $sitzung base.apk $zwischenlager
    Handy rm -f $zwischenlager | Out-Null
    if ($ausgabe -notmatch "(?m)^Success") { Ende fehler "APK ließ sich nicht in die Sitzung schreiben: $ausgabe" }
    if ($teilDatei) {
        & $adb -s $Geraet push $teilDatei $zwischenlager *> $null
        if ($LASTEXITCODE -ne 0) { Ende fehler "Pflicht-Teil ließ sich nicht aufs Handy laden" }
        $ausgabe = Handy pm install-write -S $teilGroesse $sitzung freigabe.apk $zwischenlager
        Handy rm -f $zwischenlager | Out-Null
        if ($ausgabe -notmatch "(?m)^Success") { Ende fehler "Pflicht-Teil ließ sich nicht in die Sitzung schreiben: $ausgabe" }
    }

    $kennung = "$([DateTimeOffset]::Now.ToUnixTimeSeconds())-$PID"
    Handy rm -f $antwort | Out-Null
    $start = Handy am start-foreground-service -n $fenster --es kennung $kennung
    if ($start -match "Not found") {
        # Die installierte Version kennt das Overlay noch nicht: nur dann ohne Rückfrage.
        "Installierte Jarvis-Version hat das Overlay noch nicht, installiere ohne Rückfrage."
        Installiere
    }
    if ($start -match "(?m)^Error.*") { Ende fehler "Overlay ließ sich nicht öffnen: $($Matches[0])" }

    $meldungBis = (Get-Date).AddSeconds($meldefristS)
    $bis = $null
    $wahl = ""
    while ($true) {
        $jetzt = Get-Date
        if ($bis -and $jetzt -ge $bis) { break }
        if (-not $bis -and $jetzt -ge $meldungBis) { Ende fehler "Overlay hat sich nicht gemeldet, nicht installiert" }
        $zeile = Handy cat $antwort
        if ($zeile -eq "$kennung offen") {
            if (-not $bis) {
                $bis = $jetzt.AddSeconds($warteS)
                "Overlay geöffnet, warte auf Antwort ..."
            }
        }
        elseif ($zeile -eq "$kennung fehler") { Ende fehler "Overlay konnte nicht erscheinen, nicht installiert" }
        elseif ($zeile.StartsWith("$kennung ")) { $wahl = $zeile.Substring($kennung.Length + 1); break }
        Start-Sleep -Milliseconds 200
    }

    switch -Regex ($wahl) {
        "^$" { "Keine Antwort: installiere jetzt."; break }
        "^0$" { "Gewählt: Sofort"; break }
        "^[1-5]$" { "Gewählt: in $wahl Minute(n), Installation um $((Get-Date).AddMinutes([int]$wahl).ToString('HH:mm')) Uhr"; Start-Sleep -Seconds ([int]$wahl * 60); break }
        default { Ende fehler "unbekannte Antwort: $wahl" }
    }
    Installiere
}
finally {
    if ($teilDatei) { Remove-Item $teilDatei -Force -ErrorAction SilentlyContinue }
    # Auch bei Abbruch: Die Sitzung hält sonst die ganze APK am Handy, bis Android sie Tage später entsorgt.
    if ($Geraet) {
        if ($sitzung -and -not $abgeschlossen) { Handy pm install-abandon $sitzung | Out-Null }
        Handy rm -f $zwischenlager $antwort | Out-Null
    }
}
