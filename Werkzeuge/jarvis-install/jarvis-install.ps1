# Installiert eine Jarvis-APK per adb (Tunnel, WLAN, notfalls Kabel), aber erst nach Rückfrage in Jarvis:
# Ein Overlay über jeder App bietet „Sofort“ oder „In 1 bis 5 Minuten“, damit eine laufende Aufnahme nicht abbricht.
# Ohne Antwort wird nach 30 Sekunden installiert. Die APK liegt schon vor der Frage in einer Installationssitzung am
# Handy, damit „Sofort“ wirklich sofort installiert: Nach der Antwort fehlt nur noch das Abschließen der Sitzung.
# Jarvis verlangt seinen Pflicht-Teil (assets/freigabe-teil.apk in der APK): Nacktes adb install lehnt Android deshalb
# mit INSTALL_FAILED_MISSING_SPLIT ab, nur dieses Skript und UpdateStation installieren ihn mit und stellen die Frage.
# Aufruf: jarvis-install.ps1 <apk> [-Ziel handy|tablet] [-Geraet <adb-Adresse>]
# -Ziel wählt das Gerät über seine feste Seriennummer aus Werkzeuge/adb-wlan/geraete.json (Vorgabe handy = Fold 8,
# tablet = Tab S9 „Jarvis Tablet“). Sind beide verbunden, landet die APK nie auf dem falschen Gerät.
# Kann das Overlay nicht erscheinen, wird nicht installiert.
# Letzte Zeile JARVIS_INSTALL_STATUS: installiert oder fehler.
param([Parameter(Mandatory)][string]$Apk, [string]$Geraet = "", [string]$Ziel = "handy")

$paket = "de.frank.jarvis"
$fenster = "$paket/.ui.InstallFrageDienst"
$antwort = "/sdcard/Android/data/$paket/files/install-antwort.txt"
# Am Handy immer nur ein Lauf: Zwischendatei, Antwortdatei und Overlay gibt es dort je nur einmal, auch über mehrere Rechner hinweg.
$sperre = "/data/local/tmp/jarvis-install.sperre"
$sperreFristS = 900
$sperreMein = $false
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

# Feste Seriennummer des Zielgeräts: Ohne sie (Datei fehlt, altes Repo) gilt wie früher das erste verbundene Gerät.
$geraeteDatei = Join-Path (Split-Path $PSScriptRoot) "adb-wlan\geraete.json"
$zielSerie = ""
if (Test-Path $geraeteDatei) {
    $register = Get-Content $geraeteDatei -Raw -Encoding utf8 | ConvertFrom-Json
    if (-not $register.$Ziel) { Ende fehler "Ziel '$Ziel' steht nicht in $geraeteDatei" }
    $zielSerie = "$($register.$Ziel.serial)".Trim()
}
elseif ($Ziel -ne "handy") { Ende fehler "geraete.json fehlt, Ziel '$Ziel' unbekannt" }

function Ist-Ziel([string]$adresse) {
    if (-not $zielSerie) { return $true }
    return ((& $adb -s $adresse shell getprop ro.serialno 2>$null | Out-String).Trim() -eq $zielSerie)
}
# WLAN oder Tunnel (IP:Port) vor Kabel; ohne Verbindung einmal das WLAN-Werkzeug fragen. Nur das Zielgerät zählt.
function Waehle-Geraet {
    $bereit = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" } | ForEach-Object { ($_ -split "\s+")[0] } | Where-Object { Ist-Ziel $_ })
    $netz = $bereit | Where-Object { $_ -match ":\d+$" } | Select-Object -First 1
    if ($netz) { return $netz }
    return $bereit | Select-Object -First 1
}
function Verbinde {
    if ($script:Geraet -and (& $adb -s $script:Geraet get-state 2>$null) -eq "device" -and (Ist-Ziel $script:Geraet)) { return $true }
    $script:Geraet = ""
    $script:Geraet = Waehle-Geraet
    if ($script:Geraet) { return $true }
    & pwsh -NoProfile -File $wlanSkript -Geraet $Ziel *> $null
    $script:Geraet = Waehle-Geraet
    return [bool]$script:Geraet
}
function Handy { (& $adb -s $script:Geraet shell @args 2>&1 | Out-String).Trim() }
function Nimm-Sperre {
    $bis = [DateTimeOffset]::Now.ToUnixTimeSeconds() + $sperreFristS + 60
    $gemeldet = $false
    while (Handy mkdir $sperre) {
        # Ein abgestürzter Lauf hinterlässt die Sperre: Älter als jeder echte Lauf heißt verwaist.
        $alter = 0
        try { $alter = [long](Handy date +%s) - [long](Handy stat -c %Y $sperre) } catch { }
        if ($alter -gt $sperreFristS) { Handy rmdir $sperre | Out-Null; continue }
        if ([DateTimeOffset]::Now.ToUnixTimeSeconds() -ge $bis) { Ende fehler "Eine andere Jarvis-Installation blockiert das Handy" }
        if (-not $gemeldet) { "Eine andere Jarvis-Installation läuft, warte ..." | Write-Host; $gemeldet = $true }
        Start-Sleep -Seconds 3
        if (-not (Verbinde)) { Ende fehler "Handy nicht erreichbar" }
    }
    $script:sperreMein = $true
    Handy rm -f "'/data/local/tmp/jarvis-install*.apk'" | Out-Null
}
function Installiere {
    if (-not (Verbinde)) { Ende fehler "Handy nicht erreichbar" }
    # Nach einem Verbindungsabriss kann die Adresse wechseln (WLAN, Tunnel); die Sitzung gilt aber nur auf diesem Handy.
    if ((Handy getprop ro.serialno) -ne $script:serie) { Ende fehler "anderes Gerät verbunden, Sitzung gehört zu $script:serie" }
    $ausgabe = Handy pm install-commit $script:sitzung
    $ausgabe
    if ($ausgabe -match "(?m)^Success") {
        $script:abgeschlossen = $true
        # Samsung startet Jarvis nach dem Update nicht immer von selbst (war die App vorn, fehlten Dienst und Verbindung bis zum
        # nächsten Öffnen): den Prozess wecken. Ältere Fassungen kennen die Aktion nicht, dann passiert nichts.
        Handy am broadcast -a de.frank.jarvis.AUFWACHEN -p $paket | Out-Null
        Ende installiert $script:Geraet
    }
    Ende fehler "Installation gescheitert: $ausgabe"
}

# Eine ausdrücklich genannte Adresse muss zum Ziel passen: Sonst landete die APK stillschweigend auf dem anderen Gerät.
if ($Geraet -and $zielSerie -and (& $adb -s $Geraet get-state 2>$null) -eq "device" -and -not (Ist-Ziel $Geraet)) {
    Ende fehler "Die Adresse $Geraet gehört laut geraete.json nicht zum Ziel '$Ziel'. Das gemeinte Gerät mit -Ziel handy oder -Ziel tablet nennen."
}

try {
    if (-not (Verbinde)) { Ende fehler "Handy nicht erreichbar" }
    $serie = Handy getprop ro.serialno
    Nimm-Sperre

    # Vor dem minutenlangen Hochladen: Ist am Gerät schon eine neuere Fassung (eine parallele Sitzung hat installiert), lehnt
    # Android am Ende mit INSTALL_FAILED_VERSION_DOWNGRADE ab. Dann sofort mit klarem Hinweis abbrechen.
    $aapt2 = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools\*\aapt2.exe", "$env:ANDROID_HOME\build-tools\*\aapt2.exe" -ErrorAction SilentlyContinue | Sort-Object FullName | Select-Object -Last 1
    if ($aapt2) {
        $apkCode = 0; if ((& $aapt2.FullName dump badging $Apk 2>$null | Select-Object -First 1) -match "versionCode='(\d+)'") { $apkCode = [int]$Matches[1] }
        $amGeraet = 0; if ((Handy dumpsys package $paket) -match "versionCode=(\d+)") { $amGeraet = [int]$Matches[1] }
        if ($apkCode -gt 0 -and $amGeraet -gt $apkCode) {
            Ende fehler "Am Gerät ist schon versionCode $amGeraet, die APK hat erst $apkCode (vermutlich hat eine parallele Sitzung installiert): Stand holen, Version anheben, neu bauen"
        }
    }

    "Lade die APK vorab aufs Handy über $Geraet ..."
    # lz4 spart über den Tunnel rund ein Sechstel der Übertragung; ältere adb ohne -z: ohne Kompression.
    & $adb -s $Geraet push -z lz4 $Apk $zwischenlager *> $null
    if ($LASTEXITCODE -ne 0) { & $adb -s $Geraet push $Apk $zwischenlager *> $null }
    if ($LASTEXITCODE -ne 0) {
        # Hängender Tunnel (10.8.0.7): Das WLAN-Werkzeug verbindet neu, oft direkt im LAN; dann ein zweiter Versuch am selben Handy.
        & pwsh -NoProfile -File $wlanSkript -Geraet $Ziel *> $null
        $script:Geraet = Waehle-Geraet
        if ($Geraet -and (Handy getprop ro.serialno) -eq $serie) { "Neuer Versuch über $Geraet ..."; & $adb -s $Geraet push $Apk $zwischenlager *> $null }
    }
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
        & $adb -s $Geraet push -z lz4 $teilDatei $zwischenlager *> $null
        if ($LASTEXITCODE -ne 0) { & $adb -s $Geraet push $teilDatei $zwischenlager *> $null }
        if ($LASTEXITCODE -ne 0) { Ende fehler "Pflicht-Teil ließ sich nicht aufs Handy laden" }
        $ausgabe = Handy pm install-write -S $teilGroesse $sitzung freigabe.apk $zwischenlager
        Handy rm -f $zwischenlager | Out-Null
        if ($ausgabe -notmatch "(?m)^Success") { Ende fehler "Pflicht-Teil ließ sich nicht in die Sitzung schreiben: $ausgabe" }
    }

    $kennung = "$([DateTimeOffset]::Now.ToUnixTimeSeconds())-$PID"
    Handy rm -f $antwort | Out-Null
    $start = Handy am start-foreground-service -n $fenster --es kennung $kennung
    if ($start -cmatch "Not found") {
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
    # Nach einem Verbindungsabriss erst neu verbinden; auf einem anderen Gerät gehört uns dort nichts.
    if ($sperreMein -and (Verbinde) -and (Handy getprop ro.serialno) -eq $serie) {
        if ($sitzung -and -not $abgeschlossen) { Handy pm install-abandon $sitzung | Out-Null }
        Handy rm -f $zwischenlager $antwort | Out-Null
        Handy rmdir $sperre | Out-Null
    }
}
